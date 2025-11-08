// index.js 파일 전체 내용입니다.

const functions = require("firebase-functions");
const {GoogleGenerativeAI} = require("@google/generative-ai");
const util = require("util");
const admin = require('firebase-admin');
// --- genAI 초기화 로직 (안전한 버전 유지) ---
let genAI = null;

const API_KEY = process.env.GEMINI_API_KEY;

if (!API_KEY) {
  console.error(
      "GEMINI_API_KEY 환경 변수를 찾을 수 없습니다. AI 기능이 비활성화될 수 있습니다. Cloud Run 서비스 환경 변수 설정을 확인해주세요.",
  );
} else {
  try {
    genAI = new GoogleGenerativeAI(API_KEY);
    if (genAI && typeof genAI.getGenerativeModel === "function") {
      console.log("GoogleGenerativeAI 클라이언트가 API 키로 성공적으로 초기화되었습니다.");
    } else {
      console.error(
          "API 키는 있었으나 GoogleGenerativeAI 클라이언트 초기화에 실패했습니다. SDK가 유효한 클라이언트 객체를 반환하지 않았거나 API 키가 유효하지 않을 수 있습니다. API 키 값을 확인해주세요.",
      );
      genAI = null;
    }
  } catch (e) {
    console.error(
        "GoogleGenerativeAI 초기화 중 예외(catch 블록) 발생. API 키 값의 유효성, @google/generative-ai 라이브러리 버전, 네트워크 연결 등을 확인해주세요. 오류 상세:",
        e,
    );
    genAI = null;
  }
}
// --- genAI 초기화 로직 종료 ---

exports.evaluateDish = functions.https.onCall(async (data, context) => {
  // 1. AI 클라이언트 초기화 상태 확인 (필수)
  if (!genAI) {
    console.error(
        "evaluateDish 호출 시점: GoogleGenerativeAI 클라이언트가 초기화되지 않았습니다. 서비스 시작 시점의 초기화 관련 로그를 확인해주세요.",
    );
    throw new functions.https.HttpsError(
        "internal",
        "AI 서비스가 현재 설정되지 않았거나 초기화에 실패했습니다. 관리자에게 문의하여 서버 로그를 확인해주세요.",
    );
  }

  console.log("evaluateDish: 함수가 호출되었습니다. (원본 로직 실행)");

  const actualReceivedData = data.data;

  if (!actualReceivedData || typeof actualReceivedData !== "object") {
    console.error("evaluateDish: 클라이언트로부터 유효한 data.data 객체를 받지 못했습니다. 수신된 data:", data);
    throw new functions.https.HttpsError(
        "invalid-argument",
        "요청 데이터 형식이 올바르지 않습니다.",
    );
  }

  console.log("evaluateDish: 수신된 data.data 객체:", actualReceivedData);

  const userImageBase64 = actualReceivedData.userImageBase64;
  const referenceImageBase64 = actualReceivedData.referenceImageBase64;
  const mimeTypeUser = actualReceivedData.mimeTypeUser || "image/jpeg";
  const mimeTypeReference = actualReceivedData.mimeTypeReference || "image/jpeg";

  console.log("evaluateDish: userImageBase64 길이:", userImageBase64 ? String(userImageBase64).length : "N/A");
  console.log("evaluateDish: referenceImageBase64 길이:", referenceImageBase64 ? String(referenceImageBase64).length : "N/A");
  console.log("evaluateDish: mimeTypeUser:", mimeTypeUser);
  console.log("evaluateDish: mimeTypeReference:", mimeTypeReference);

  if (!userImageBase64 || typeof userImageBase64 !== "string" || userImageBase64.trim() === "" ||
      !referenceImageBase64 || typeof referenceImageBase64 !== "string" || referenceImageBase64.trim() === "") {
    console.error(
        "evaluateDish: 백엔드 검사 - 사용자 이미지 또는 원본 레시피 이미지 Base64 문자열이 누락되었거나, 타입이 문자열이 아니거나, 비어있습니다." +
        ` userImageBase64 (타입: ${typeof userImageBase64}, 비었나?: ${typeof userImageBase64 === "string" ? (userImageBase64 || "").trim() === "" : "타입오류"})` +
        ` referenceImageBase64 (타입: ${typeof referenceImageBase64}, 비었나?: ${typeof referenceImageBase64 === "string" ? (referenceImageBase64 || "").trim() === "" : "타입오류"})`,
    );
    throw new functions.https.HttpsError(
        "invalid-argument",
        "사용자 요리 사진과 원본 레시피 사진(Base64 인코딩된 비어있지 않은 문자열)을 모두 올바르게 포함해야 합니다.",
    );
  }

  const comparisonPrompt = (
    "다음은 원본 레시피의 대표 사진과 사용자가 직접 만든 요리 사진입니다. " +
    "원본에 대해 평가하지 말고, 사용자의 요리 사진을 원본 레시피 사진과 비교하여, 사용자의 요리가 잘된 점, 개선이 필요한 점, " +
    "원본과의 유사성, 플레이팅, 색감 등을 포함하여 구체적으로 평가하고 종합적인 피드백을 별점 5개 만점 기준으로 제공해주세요."
  );

  try {
    // --- 모델명 변경 ---
    const model = genAI.getGenerativeModel({model: "gemini-2.5-flash"}); // 최신 모델로 변경
    // --- 모델명 변경 끝 ---

    const userImagePart = {inlineData: {data: userImageBase64, mimeType: mimeTypeUser}};
    const referenceImagePart = {inlineData: {data: referenceImageBase64, mimeType: mimeTypeReference}};

    console.log(`Gemini API에 비교 평가 요청 전송 중... (UserImgLen: ${userImageBase64.length}, RefImgLen: ${referenceImageBase64.length})`);
    const result = await model.generateContent([comparisonPrompt, referenceImagePart, userImagePart]);

    if (!result || !result.response) {
      console.error("Gemini API로부터 유효한 응답 객체(result.response)를 받지 못했습니다.");
      throw new functions.https.HttpsError("internal", "AI 서비스로부터 응답이 없거나 응답 형식이 올바르지 않습니다.");
    }

    const response = result.response;
    const text = typeof response.text === "function" ? response.text() : (response.candidates && response.candidates[0] && response.candidates[0].content && response.candidates[0].content.parts && response.candidates[0].content.parts[0] && response.candidates[0].content.parts[0].text);

    if (typeof text !== "string" || text.trim() === "") {
      console.error("Gemini API로부터 유효한 텍스트 응답을 받지 못했습니다. 응답 내용:", JSON.stringify(response, null, 2));
      throw new functions.https.HttpsError("internal", "AI 서비스로부터 평가 내용을 받지 못했습니다.");
    }

    console.log("Gemini API로부터 비교 평가 결과 수신 완료.");
    return {evaluation: text};
  } catch (error) {
    console.error("Gemini API 비교 평가 중 오류 발생:", error);
    let detailedErrorMessage = "Gemini API를 사용한 비교 평가에 실패했습니다.";
    if (error.message) {
      detailedErrorMessage += ` 세부 정보: ${error.message}`;
    }
    if (error.response && error.response.promptFeedback) {
      console.error("Gemini API Prompt Feedback:", JSON.stringify(error.response.promptFeedback, null, 2));
      detailedErrorMessage += ` API Feedback: ${JSON.stringify(error.response.promptFeedback)}`;
    }
    console.error("서버 상세 오류 메시지:", detailedErrorMessage);
    throw new functions.https.HttpsError("internal", "이미지 비교 평가 중 서버에서 오류가 발생했습니다. 관리자에게 문의하여 서버 로그를 확인해주세요.");
  }
});
if (!admin.apps.length) admin.initializeApp();
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

exports.askRecipe = functions.https.onCall(async (data, context) => {
  if (!genAI) {
    throw new functions.https.HttpsError("internal", "AI 서비스 초기화에 실패했습니다.");
  }

  const clientPayload = data?.data || data; // 혹시 몰라 data.data/data 둘 다 지원
  const userText = clientPayload?.text;

  if (!userText || typeof userText !== "string" || userText.trim() === "") {
    throw new functions.https.HttpsError("invalid-argument", "사용자의 질문이 비어 있거나 문자열이 아닙니다.");
  }
// 1) Firestore에서 recipe 컬렉션의 'name' 필드만 최대 20개 추출
    let recipeNames = [];
    try {
      const snapshot = await admin.firestore()
        .collection("recipe")
        .limit(20)
        .get();
      recipeNames = snapshot.docs
        .map(doc => doc.data()?.name)
        .filter(name => typeof name === "string" && name.trim().length > 0);
    } catch (err) {
      console.error("Firestore recipe 목록 불러오기 실패:", err);
    }

        // 2) Gemini 프롬프트 생성 (더 정교하게)
       const prompt = `
       너는 오직 아래 [앱에 등록된 레시피 목록]에 있는 레시피만 안내할 수 있는 "매우 엄격한" 요리 챗봇이야.

       [중요 규칙]
       - 아래 목록에 없는 음식, 유사 음식, 응용 요리, 관련 없는 주제는 그 어떤 상황에도 안내/추천/설명/예시/부연설명/추측/변형을 해서는 안 된다.
       - 유저 입력과 목록의 레시피명이 정확히 일치하지 않아도, 철자/띄어쓰기/발음/복수단수/부분일치/유사 단어(예: 오타, 비슷한 표기, 영어-한글, 재료 등)라면 가장 관련성 높은 **1개 레시피만** 골라서 안내한다.
       - 여러 음식/재료/키워드가 입력돼도 반드시 하나만 골라서, **앱에 실제 등록된 레시피**여야 한다.
       - 전혀 관련성이 없거나 유사도가 낮은 경우, 반드시 "앱에 없는 레시피입니다."라는 문장만 출력하고, 그 외 불필요한 안내는 절대 하지 마라.

       [출력 규칙]
       - 오직 아래 예시 포맷 중 하나만 사용해라. 절대 다른 형식, 추가 문장, 서론, 인사, 요리 추천, 유사 안내, 잡담, 부연, 변형, 요리사가 알아야 할 팁, 추가 설명 등은 금지!
       - 답변의 첫 번째 문장에 반드시 레시피 이름이 자연스럽게 노출되어야 한다.
       - 요리법 설명은 반드시 5문장 이내로, 주요 재료와 요리 순서를 짧고 쉬운 문장으로 안내한다.
       - 만약, 유저 입력이 앱에 등록된 레시피와 관련성이 애매하거나, 여러 개 중에 확실히 골라낼 수 없는 경우도 반드시 "앱에 없는 레시피입니다."라고만 답한다.
       - 답변 형식/문장 구조/내용이 아래 예시 포맷 중 하나와 100% 동일하지 않으면, "앱에 없는 레시피입니다."라고만 출력한다(실수 방지용).

       [앱에 등록된 레시피 목록]
       ${recipeNames.map((r, i) => (i + 1) + '. ' + r).join('\n')}

       유저 입력:
       ${userText}

       [출력 예시(아래 중 반드시 하나!)]
       1. [요리 이름] 레시피입니다. 주요 재료는 [재료 예시]이고, 만드는 방법은 [간단 순서 설명]입니다.
       2. [요리 이름]은(는) [재료 예시]를 사용해서 [간단 순서]로 만들 수 있습니다.
       3. (앱에 없는 경우) 앱에 없는 레시피입니다.

       (※ 위 예시 이외의 말, 인사, 잡담, 창작, 부연, 안내, 주석, 포맷 일탈, 불필요한 안내, 두 개 이상 안내, 반복, 설명 등은 절대 쓰지 마라. 실수로라도 형식을 벗어날 경우 "앱에 없는 레시피입니다." 한 문장만 남겨라.)
       `;

try {
  const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });
  const result = await model.generateContent(prompt);

  if (!result || !result.response) {
    throw new functions.https.HttpsError("internal", "AI 응답이 없습니다.");
  }
  const response = result.response;
  let replyMessage;
  if (typeof response.text === "function") {
    replyMessage = response.text();
  } else {
    replyMessage = "AI 응답을 처리하는 중 예기치 않은 오류가 발생했습니다.";
  }
  if (replyMessage.trim() === "") {
    replyMessage = "AI가 현재 질문에 대해 답변을 생성하지 못했습니다. 다른 방식으로 질문해주시겠어요?";
  }
  return { reply: replyMessage };
} catch (err) {
  const errorMessage = err.message || "Gemini API 호출 중 알 수 없는 오류가 발생했습니다.";
  throw new functions.https.HttpsError("internal", errorMessage);
}
});

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.onRoomMessage = functions.firestore
  .document('openRooms/{roomId}/messages/{msgId}')
  .onCreate(async (snap, ctx) => {
    const msg = snap.data();
    const roomId = ctx.params.roomId;

    await admin.firestore().doc(`openRooms/${roomId}`).update({
      lastMessage: msg.text || (msg.imageUrl ? '사진' : '') || '메시지',
      lastAt: Date.now()
    });

    const membersSnap = await admin.firestore().collection(`openRooms/${roomId}/members`).get();
    const uids = membersSnap.docs.map(d => d.id).filter(uid => uid !== msg.senderUid);
    if (uids.length === 0) return null;

    const userDocs = await Promise.all(uids.map(uid => admin.firestore().doc(`user/${uid}`).get()));
    const tokens = userDocs.flatMap(d => (d.get('deviceTokens') || [])).filter(Boolean);
    if (tokens.length === 0) return null;

    const payload = {
      notification: {
        title: `새 메시지`,
        body: msg.text ? msg.text.substring(0, 50) : '사진이 도착했어요',
      },
      data: { type: 'openchat', roomId }
    };
    await admin.messaging().sendToDevice(tokens, payload);
    return null;
  });

exports.onMemberCountChange = functions.firestore
  .document('openRooms/{roomId}/members/{uid}')
  .onWrite(async (change, ctx) => {
    const roomRef = admin.firestore().doc(`openRooms/${ctx.params.roomId}`);
    const membersSnap = await roomRef.collection('members').get();
    await roomRef.update({ memberCount: membersSnap.size });
  });
