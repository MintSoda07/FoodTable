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
  | { sent: 0; reason: "no-token" }
  | { sent: number; failed: number; cleaned: number };

export const sendChat = onCall<SendChatPayload, Promise<SendChatResult>>(
  {
    region: "asia-northeast3",
    timeoutSeconds: 60,
    memory: "512MiB",
  },
  async (request: CallableRequest<SendChatPayload>): Promise<SendChatResult> => {
    // 1. 인증 확인
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
    }

    // 2. 파라미터 검증
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

    // 3. 대상 사용자 토큰 읽기
    const userDoc = await db.collection("user").doc(toUid).get();
    if (!userDoc.exists) {
      throw new HttpsError("not-found", "대상 사용자가 존재하지 않습니다.");
    }

    let tokens: string[] = [];

    // 우선 배열 필드(fcmTokens) 확인
    const tokenArray = userDoc.get("fcmTokens");
    if (Array.isArray(tokenArray)) {
      tokens = tokenArray.filter((t: unknown) => typeof t === "string" && t.trim());
    }

    // 배열이 없거나 비어있으면 단일 필드(fcmToken) 사용
    if (tokens.length === 0) {
      const singleToken = userDoc.get("fcmToken");
      if (typeof singleToken === "string" && singleToken.trim()) {
        tokens = [singleToken];
      }
    }

    if (!tokens.length) return { sent: 0, reason: "no-token" };

    // 4. 메시지 구성
    const message: admin.messaging.MulticastMessage = {
      tokens,
      android: { priority: "high", collapseKey: `chat_${chatUid}` },
      data: {
        type: "chat",
        chatUid,
        messageId: Date.now().toString(), // 중복 방지 위해 추후 Firestore ID로 대체 권장
        title: String(title).slice(0, 50),
        body: String(body).slice(0, 140),
      },
    };

    // 5. 발송
    const res = await messaging.sendEachForMulticast(message);

    // 6. 실패 토큰 정리 (배열 기반 저장소에만 적용)
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

    if (invalid.length && Array.isArray(tokenArray)) {
      await cleanupInvalidTokens(toUid, invalid);
    }

    return {
      sent: res.successCount,
      failed: res.failureCount,
      cleaned: invalid.length,
    };
  }
);
