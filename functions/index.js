// index.js 파일 전체 내용입니다.

const functions = require("firebase-functions");
const {GoogleGenerativeAI} = require("@google/generative-ai");
const util = require("util");
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
    const model = genAI.getGenerativeModel({model: "gemini-1.5-flash-latest"}); // 최신 모델로 변경
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
exports.askRecipe = functions.https.onCall(async (data, context) => {
  if (!genAI) {
    console.error("askRecipe: genAI가 초기화되지 않았습니다.");
    throw new functions.https.HttpsError(
      "internal",
      "AI 서비스 초기화에 실패했습니다.",
    );
  }

  console.log("--- askRecipe V5.1 실행 (Gemini 오류 로깅 강화) ---");
  console.log("수신된 'data' 파라미터 (util.inspect, depth: 3):", util.inspect(data, { depth: 3, colors: false }));

  const clientPayload = data?.data;
  const userText = clientPayload?.text;

  console.log("추출된 clientPayload (data.data):", clientPayload ? JSON.stringify(clientPayload, null, 2) : clientPayload);
  console.log("추출된 userText (data.data.text 사용):", userText);
  console.log("추출된 userText의 타입:", typeof userText);

  if (!userText || typeof userText !== "string" || userText.trim() === "") {
    console.error("userText 유효성 검사 실패. 추출된 userText:", userText);
    throw new functions.https.HttpsError(
      "invalid-argument",
      "사용자의 질문이 비어 있거나 문자열이 아닙니다.",
    );
  }

  // Gemini API 호출 로직
  try {
    console.log("askRecipe: Gemini generateContent 호출 시작. userText:", userText);
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash-latest" });

    const result = await model.generateContent(userText);

    if (!result || !result.response) {
      console.error("askRecipe: Gemini 응답이 없음 또는 형식 오류");
      throw new functions.https.HttpsError("internal", "AI 응답이 없습니다.");
    }

    const response = result.response;
    let replyMessage;

    if (typeof response.text === "function") {
      replyMessage = response.text();
    } else {
      console.error("askRecipe: response.text가 함수가 아닙니다. Gemini API 응답 구조를 확인해야 합니다.");
      replyMessage = "AI 응답을 처리하는 중 예기치 않은 오류가 발생했습니다.";
    }

    if (replyMessage.trim() === "") {
      console.log("askRecipe: Gemini가 빈 응답을 반환했습니다. 사용자 안내 메시지를 설정합니다.");
      replyMessage = "AI가 현재 질문에 대해 답변을 생성하지 못했습니다. 다른 방식으로 질문해주시겠어요?";
    }

    console.log("askRecipe: Gemini 응답 수신 (클라이언트 전달 예정) →", replyMessage);
    return { reply: replyMessage };

  } catch (err) {
    console.error("askRecipe: Gemini API 호출 중 심각한 오류 발생!"); // 로그 메시지 변경
    // ★★★ 강화된 오류 로깅 시작 ★★★
    console.error("askRecipe: 전체 오류 객체 (util.inspect) →", util.inspect(err, { depth: 5, colors: false }));

    if (err.message) {
        console.error("askRecipe: 오류 메시지 (err.message):", err.message);
    }
    // GoogleGenerativeAIFetchError 와 같은 네트워크 기반 오류는 status 나 cause 를 가질 수 있습니다.
    if (err.status) {
        console.error("askRecipe: 오류 상태 (err.status):", err.status);
    }
    if (err.cause) {
        console.error("askRecipe: 오류 원인 (err.cause, util.inspect) →", util.inspect(err.cause, { depth: 3, colors: false }));
    }
    // 기존 HTTP 응답 관련 로깅 시도도 유지 (일부 오류 유형에 유용할 수 있음)
    if (err.response?.status) {
      console.error("askRecipe: 오류 객체 내 HTTP 응답 상태 코드 (err.response.status):", err.response.status);
    }
    if (err.response?.data) {
      try {
        console.error("askRecipe: 오류 객체 내 HTTP 응답 본문 (err.response.data):", JSON.stringify(err.response.data, null, 2));
      } catch (jsonErr) {
        console.error("askRecipe: 응답 본문 stringify 실패:", jsonErr);
      }
    }
    // ★★★ 강화된 오류 로깅 끝 ★★★

    const errorMessage = err.message || "Gemini API 호출 중 알 수 없는 오류가 발생했습니다.";
    throw new functions.https.HttpsError("internal", errorMessage);
  }
});