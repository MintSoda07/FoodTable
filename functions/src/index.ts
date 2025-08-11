// functions/src/index.ts
import * as admin from "firebase-admin";
import { onCall, CallableRequest, HttpsError } from "firebase-functions/v2/https";

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

async function cleanupInvalidTokens(uid: string, invalid: string[]) {
  if (!invalid.length) return;
  await db.collection("user").doc(uid).update({
    fcmTokens: admin.firestore.FieldValue.arrayRemove(...invalid),
  });
}

type SendChatPayload = {
  toUid: string;
  chatUid: string;
  title?: string;
  body?: string;
};

type SendChatResult =
  | { sent: 0; skipped: true }
  | { sent: 0; reason: "no-tokens" }
  | { sent: number; failed: number; cleaned: number };

export const sendChat = onCall<SendChatPayload, Promise<SendChatResult>>(
  {
    region: "asia-northeast3",
    timeoutSeconds: 60,
    memory: "512MiB",
  },
  async (request: CallableRequest<SendChatPayload>): Promise<SendChatResult> => {
    // 인증
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
    }

    // 입력 파싱 및 검증
    const {
      toUid,
      chatUid,
      title = "새 메시지",
      body = "메시지가 도착했습니다.",
    } = request.data ?? {};

    if (!toUid || !chatUid) {
      throw new HttpsError("invalid-argument", "toUid, chatUid가 필요합니다.");
    }
    if (toUid === uid) return { sent: 0, skipped: true };

    // 대상 사용자 토큰 조회
    const userDoc = await db.collection("user").doc(toUid).get();
    const tokens = (userDoc.get("fcmTokens") as string[] | undefined) ?? [];
    if (!tokens.length) return { sent: 0, reason: "no-tokens" };

    // 메시지 구성
    const message: admin.messaging.MulticastMessage = {
      tokens,
      android: { priority: "high", collapseKey: `chat_${chatUid}` },
      data: {
        type: "chat",
        chatUid,
        messageId: Date.now().toString(),
        title: String(title).slice(0, 50),
        body: String(body).slice(0, 140),
      },
    };

    // 전송
    const res = await messaging.sendEachForMulticast(message);

    // 실패/만료 토큰 정리
    const invalid: string[] = [];
    res.responses.forEach((r, i) => {
      const code =
        (r.error as { errorInfo?: { code?: string } })?.errorInfo?.code ??
        (r.error as { code?: string })?.code;
      if (
        code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token"
      ) {
        invalid.push(tokens[i]);
      }
    });

    if (invalid.length) {
      await cleanupInvalidTokens(toUid, invalid);
    }

    return {
      sent: res.successCount,
      failed: res.failureCount,
      cleaned: invalid.length,
    };
  }
);
