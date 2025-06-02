// index.js 파일 전체 내용입니다.

const functions = require("firebase-functions");
const {GoogleGenerativeAI} = require("@google/generative-ai");

// --- genAI 초기화 로직 (안전한 버전 유지) ---
let genAI = null;

const API_KEY = process.env.GEMINI_API_KEY;

if (!API_KEY) {
    console.error(
        "GEMINI_API_KEY 환경 변수를 찾을 수 없습니다. AI 기능이 비활성화될 수 있습니다. Cloud Run 서비스 환경 변수 설정을 확인해주세요."
    );
} else {
    try {
        genAI = new GoogleGenerativeAI(API_KEY);
        if (genAI && typeof genAI.getGenerativeModel === 'function') {
            console.log("GoogleGenerativeAI 클라이언트가 API 키로 성공적으로 초기화되었습니다.");
        } else {
            console.error(
                "API 키는 있었으나 GoogleGenerativeAI 클라이언트 초기화에 실패했습니다. SDK가 유효한 클라이언트 객체를 반환하지 않았거나 API 키가 유효하지 않을 수 있습니다. API 키 값을 확인해주세요."
            );
            genAI = null;
        }
    } catch (e) {
        console.error(
            "GoogleGenerativeAI 초기화 중 예외(catch 블록) 발생. API 키 값의 유효성, @google/generative-ai 라이브러리 버전, 네트워크 연결 등을 확인해주세요. 오류 상세:",
            e
        );
        genAI = null;
    }
}
// --- genAI 초기화 로직 종료 ---

exports.evaluateDish = functions.https.onCall(async (data, context) => {
  // 1. AI 클라이언트 초기화 상태 확인 (필수)
  if (!genAI) {
    console.error(
        "evaluateDish 호출 시점: GoogleGenerativeAI 클라이언트가 초기화되지 않았습니다. 서비스 시작 시점의 초기화 관련 로그를 확인해주세요."
    );
    throw new functions.https.HttpsError(
        "internal",
        "AI 서비스가 현재 설정되지 않았거나 초기화에 실패했습니다. 관리자에게 문의하여 서버 로그를 확인해주세요."
    );
  }

  console.log("evaluateDish: 함수가 호출되었습니다. (원본 로직 실행)");

  const actualReceivedData = data.data;

  if (!actualReceivedData || typeof actualReceivedData !== 'object') {
    console.error("evaluateDish: 클라이언트로부터 유효한 data.data 객체를 받지 못했습니다. 수신된 data:", data);
    throw new functions.https.HttpsError(
        "invalid-argument",
        "요청 데이터 형식이 올바르지 않습니다."
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

  if (!userImageBase64 || typeof userImageBase64 !== 'string' || userImageBase64.trim() === "" ||
      !referenceImageBase64 || typeof referenceImageBase64 !== 'string' || referenceImageBase64.trim() === "") {
    console.error(
        "evaluateDish: 백엔드 검사 - 사용자 이미지 또는 원본 레시피 이미지 Base64 문자열이 누락되었거나, 타입이 문자열이 아니거나, 비어있습니다." +
        ` userImageBase64 (타입: ${typeof userImageBase64}, 비었나?: ${typeof userImageBase64 === 'string' ? (userImageBase64 || "").trim() === "" : "타입오류"})` +
        ` referenceImageBase64 (타입: ${typeof referenceImageBase64}, 비었나?: ${typeof referenceImageBase64 === 'string' ? (referenceImageBase64 || "").trim() === "" : "타입오류"})`
    );
    throw new functions.https.HttpsError(
        "invalid-argument",
        "사용자 요리 사진과 원본 레시피 사진(Base64 인코딩된 비어있지 않은 문자열)을 모두 올바르게 포함해야 합니다."
    );
  }

  const comparisonPrompt = (
    "다음은 원본 레시피의 대표 사진과 사용자가 직접 만든 요리 사진입니다. " +
    "원본에 대해 평가하지 말고, 사용자의 요리 사진을 원본 레시피 사진과 비교하여, 사용자의 요리가 잘된 점, 개선이 필요한 점, " +
    "원본과의 유사성, 플레이팅, 색감 등을 포함하여 구체적으로 평가하고 종합적인 피드백을 별점 5개 만점 기준으로 제공해주세요."
  );

  try {
    // --- 모델명 변경 ---
    const model = genAI.getGenerativeModel({model: "gemini-1.5-flash-latest"}); // 최신 모델로 변경
    // --- 모델명 변경 끝 ---

    const userImagePart = { inlineData: { data: userImageBase64, mimeType: mimeTypeUser } };
    const referenceImagePart = { inlineData: { data: referenceImageBase64, mimeType: mimeTypeReference } };

    console.log(`Gemini API에 비교 평가 요청 전송 중... (UserImgLen: ${userImageBase64.length}, RefImgLen: ${referenceImageBase64.length})`);
    const result = await model.generateContent([ comparisonPrompt, referenceImagePart, userImagePart ]);

    if (!result || !result.response) {
      console.error("Gemini API로부터 유효한 응답 객체(result.response)를 받지 못했습니다.");
      throw new functions.https.HttpsError("internal", "AI 서비스로부터 응답이 없거나 응답 형식이 올바르지 않습니다.");
    }

    const response = result.response;
    const text = typeof response.text === 'function' ? response.text() : (response.candidates && response.candidates[0] && response.candidates[0].content && response.candidates[0].content.parts && response.candidates[0].content.parts[0] && response.candidates[0].content.parts[0].text);

    if (typeof text !== 'string' || text.trim() === "") {
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