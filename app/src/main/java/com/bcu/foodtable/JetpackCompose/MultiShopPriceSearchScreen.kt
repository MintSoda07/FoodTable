package com.bcu.foodtable.JetpackCompose

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcu.foodtable.useful.ApiKeyManager
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject

private const val TAG = "MultiShopPriceSearch"
private const val TIMEOUT_SEC = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiShopPriceSearchScreen(intent: Intent) {
    val searchKeyword = intent.getStringExtra("SEARCH_INGREDIENT") ?: ""
    val shopNames = listOf("네이버쇼핑", "쿠팡", "11번가", "G마켓", "옥션", "이마트몰")
    val mergedResults = remember { mutableStateListOf<MergedShopItem>() }
    val isLoading = remember { mutableStateOf(true) }
    val context = LocalContext.current

    // 정렬 옵션 상태
    var sortOption by remember { mutableStateOf(SortOption.PRICE_ASC) }
    val sortOptions = listOf(
        SortOption.PRICE_ASC, SortOption.RATING_DESC, SortOption.SHOP
    )

    // GPT 쿼리 및 병합
    LaunchedEffect(searchKeyword) {
        isLoading.value = true
        val deferreds = shopNames.map { shop ->
            async {
                try {
                    val prompt = """
                        $shop 에서 '$searchKeyword'를 최저가순으로 5개,
                        '| 상품명 | 가격 | 평점 | 링크 |' 열이 포함된 마크다운 표로만 정리해 주세요.
                        광고, 안내, 설명 없이 표만.
                    """.trimIndent()
                    val gptResultString = getGptTableResult(prompt)
                    val parsed = parseMarkdownTableWithHttpFallback(parsedShop = shop, mdTable = gptResultString)
                    parsed
                } catch (e: Exception) {
                    Log.e(TAG, "[$shop] 조회 오류: ${e.message}", e)
                    emptyList()
                }
            }
        }
        delay(TIMEOUT_SEC * 1000L)
        mergedResults.clear()
        deferreds.forEach { deferred ->
            val items = try { deferred.await() } catch (_: Exception) { emptyList<MergedShopItem>() }
            mergedResults.addAll(items)
        }
        isLoading.value = false
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("‘$searchKeyword’ 가격 비교", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { (context as? android.app.Activity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).padding(horizontal = 8.dp)) {
            Spacer(Modifier.height(10.dp))

            // 정렬 옵션 드롭다운
            var expanded by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text("정렬:", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Box {
                    Button(onClick = { expanded = true }, contentPadding = PaddingValues(0.dp)) {
                        Text(sortOption.displayName, fontSize = 14.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    sortOption = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            if (isLoading.value) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("$TIMEOUT_SEC 초만 기다려주세요…", color = Color.Gray)
                }
            } else if (mergedResults.isEmpty()) {
                Text("검색 결과가 없습니다.", color = Color.Gray, modifier = Modifier.padding(32.dp))
            } else {
                PriceCompareTable(
                    mergedResults.sortedSmart(sortOption),
                    context,
                    searchKeyword
                )
            }
        }
    }
}

// 정렬 옵션 Enum
enum class SortOption(val displayName: String) {
    PRICE_ASC("가격↑"),
    RATING_DESC("평점↓"),
    SHOP("쇼핑몰순")
}

// 정렬 방식
fun List<MergedShopItem>.sortedSmart(option: SortOption = SortOption.PRICE_ASC): List<MergedShopItem> {
    fun priceNum(item: MergedShopItem): Int {
        val priceStr = Regex("""[\d,]+""").find(item.price)?.value?.replace(",", "")
        return priceStr?.toIntOrNull() ?: Int.MAX_VALUE
    }
    fun ratingNum(item: MergedShopItem): Float {
        val ratingStr = Regex("""[\d.]+""").find(item.rating)?.value
        return ratingStr?.toFloatOrNull() ?: 0f
    }
    return when (option) {
        SortOption.PRICE_ASC -> this.sortedWith(compareBy<MergedShopItem> { priceNum(it) }.thenByDescending { ratingNum(it) })
        SortOption.RATING_DESC -> this.sortedWith(compareByDescending<MergedShopItem> { ratingNum(it) }.thenBy { priceNum(it) })
        SortOption.SHOP -> this.sortedBy { it.shop }
    }
}

@Composable
fun PriceCompareTable(results: List<MergedShopItem>, context: Context,searchKeyword: String = "") {
    val searchUrls = mapOf(
        "네이버쇼핑" to "https://search.shopping.naver.com/search/all?query=",
        "쿠팡" to "https://www.coupang.com/np/search?component=&q=",
        "11번가" to "https://search.11st.co.kr/Search.tmall?kwd=",
        "G마켓" to "https://browse.gmarket.co.kr/search?keyword=",
        "옥션" to "https://search.auction.co.kr/search/search.aspx?keyword=",
        "이마트몰" to "https://emart.ssg.com/search.ssg?query="
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFE5E7EF))
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("No.", Modifier.width(32.dp), fontWeight = FontWeight.Bold, color = Color(0xFF263465), fontSize = 13.sp)
        Text("쇼핑몰", Modifier.width(64.dp), fontWeight = FontWeight.Bold, color = Color(0xFF263465), fontSize = 13.sp)
        Text("상품명 / 가격 / 평점", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF263465), fontSize = 13.sp)
        Text("상세", Modifier.width(58.dp), fontWeight = FontWeight.Bold, color = Color(0xFF263465), fontSize = 13.sp)
    }
    Divider()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(results) { idx, item ->
            Card(
                Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(5.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    Modifier
                        .padding(vertical = 10.dp, horizontal = 6.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${idx + 1}", Modifier.width(32.dp), fontSize = 13.sp, color = Color(0xFF7C7C7C), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(item.shop, Modifier.width(64.dp), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFF3755C6), maxLines = 2)
                    Column(Modifier.weight(1f).padding(start = 6.dp, end = 4.dp)) {
                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row {
                            if (item.price.isNotBlank()) {
                                Text(item.price, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFE25532))
                                Spacer(Modifier.width(10.dp))
                            }
                            if (item.rating.isNotBlank()) {
                                Text("⭐ ${item.rating}", fontSize = 12.sp, color = Color(0xFF6C6C6C))
                            }
                        }
                    }
                    val searchTarget = item.name.ifBlank { searchKeyword }
                    val encoded = Uri.encode(searchTarget)
                    val searchUrl = searchUrls[item.shop]?.plus(encoded) ?: ""

                    if (item.linkUrl.startsWith("http")) {
                        Text(
                            text = "상세검색",
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .width(68.dp)
                                .clickable {
                                    Log.d(TAG, "상세검색 링크 이동: $searchUrl")
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                                    context.startActivity(intent)
                                }
                                .padding(4.dp),
                            maxLines = 1
                        )
                    } else {
                        Text("없음", Modifier.width(58.dp), color = Color(0xFFB0B0B0), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
// --- 1. 마크다운 파서 & 클린유틸 --- //
val httpRegex = Regex("""https?://[^\s\|)>\]]+""")
val parenUrlRegex = Regex("""\(?https?://[^\s\|)>\]]+\)?""")
val markdownLinkRegex = Regex("""\[(.*?)\]\((https?://[^\s\|)>\]]+)\)""")

fun cleanNameAndExtractUrl(rawName: String): Pair<String, String?> {
    // [이름](링크)
    val markdownMatch = markdownLinkRegex.find(rawName)
    if (markdownMatch != null) {
        val nameOnly = markdownMatch.groupValues[1].trim()
        val link = markdownMatch.groupValues[2].trim()
        return nameOnly to link
    }
    // (링크) 또는 http...
    val parenUrlMatch = parenUrlRegex.find(rawName)
    val url = parenUrlMatch?.value?.replace("(", "")?.replace(")", "")?.trim()
    var cleaned = rawName
        .replace(markdownLinkRegex, "")
        .replace(parenUrlRegex, "")
        .replace(httpRegex, "")
        .replace("[", "")
        .replace("]", "")
        .replace("(", "")
        .replace(")", "")
        .replace("．", "") // 가끔 들어오는 비표준 마침표 등도 정리
        .trim()
        .replace(Regex("""\s{2,}"""), " ") // 여러 칸 공백 하나로
        .replace(Regex("""^[\.,;:]+"""), "") // 맨 앞쪽에 특수문자 정리
        .replace(Regex("""[\.,;:]+$"""), "") // 맨 뒤쪽에 특수문자 정리
    if (url != null && url.startsWith("http")) return cleaned to url
    return cleaned to null
}

// --- 2. 마크다운 테이블 파싱 (상품명 링크 추출 포함) --- //
fun parseMarkdownTableWithHttpFallback(parsedShop: String, mdTable: String): List<MergedShopItem> {
    val lines = mdTable.lines().filter { it.trim().startsWith("|") }
    if (lines.size < 2) return emptyList()
    val headers = lines[0].split("|").map { it.trim() }.filter { it.isNotBlank() }
    val rows = lines.drop(2).mapNotNull { line ->
        val parts = line.split("|").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size >= headers.size) parts else null
    }
    val idxName = headers.indexOfFirst { it.contains("상품명") }
    val idxPrice = headers.indexOfFirst { it.contains("가격") }
    val idxRating = headers.indexOfFirst { it.contains("평점") }
    val idxLink = headers.indexOfFirst { it.contains("링크") }

    return rows.mapIndexedNotNull { rowIdx, row ->
        val rawName = row.getOrNull(idxName) ?: ""
        val price = row.getOrNull(idxPrice) ?: ""
        val rating = row.getOrNull(idxRating) ?: ""
        var linkUrl = row.getOrNull(idxLink) ?: ""

        // 상품명에서 링크 추출
        val (nameClean, urlFromName) = cleanNameAndExtractUrl(rawName)
        val linkUrlFinal = when {
            linkUrl.startsWith("http") -> linkUrl
            !urlFromName.isNullOrBlank() -> urlFromName
            else -> ""
        }

        // 디버그 로그 (상세하게!)
        Log.d(TAG, "[ROW $rowIdx] [$parsedShop] name='$rawName' → clean='$nameClean', price='$price', rating='$rating', link='$linkUrlFinal'")

        // 링크 없거나 필수 데이터 누락시 스킵
        if (nameClean.isBlank() || price.isBlank() || !linkUrlFinal.startsWith("http")) return@mapIndexedNotNull null
        MergedShopItem(parsedShop, nameClean, price, rating, linkUrlFinal)
    }
}

// 데이터 클래스
data class MergedShopItem(
    val shop: String,
    val name: String,
    val price: String,
    val rating: String,
    val linkUrl: String
)

// --- 3. GPT 호출 (로그 강화) --- //
suspend fun getGptTableResult(prompt: String): String = withContext(Dispatchers.IO) {
    val apiKeyObj = ApiKeyManager.getGptApi()
    val gptKey = apiKeyObj?.KEY_VALUE
    require(!gptKey.isNullOrBlank()) { "GPT API Key가 누락되었습니다." }
    val apiUrl = "https://api.openai.com/v1/chat/completions"
    val gson = Gson()
    val reqObj = GptRequest(
        model = "gpt-4o",
        messages = listOf(
            GptMessage("system", "당신은 한국 온라인 쇼핑몰 가격비교 전문가입니다. 반드시 표는 '| 상품명 | 가격 | 평점 | 링크 |' 구조의 마크다운 테이블로 제공하세요."),
            GptMessage("user", prompt)
        )
    )

    Log.d(TAG, "[GPT-REQ] Key exists=${!gptKey.isNullOrBlank()}")
    Log.d(TAG, "[GPT-REQ] Prompt preview=${prompt.take(200)}")
    val reqJson = gson.toJson(reqObj)
    Log.d(TAG, "[GPT-REQ] JSON body: $reqJson")
    val reqBody = RequestBody.create(
        "application/json; charset=utf-8".toMediaTypeOrNull(),
        reqJson
    )

    val client = OkHttpClient.Builder()
        .callTimeout(TIMEOUT_SEC.toLong(), java.util.concurrent.TimeUnit.SECONDS)
        .build()
    val request = Request.Builder()
        .url(apiUrl)
        .addHeader("Authorization", "Bearer $gptKey")
        .addHeader("Content-Type", "application/json")
        .post(reqBody)
        .build()

    Log.d(TAG, "[GPT-REQ] 요청 시작: $apiUrl")
    try {
        client.newCall(request).execute().use { resp ->
            val bodyString = resp.body?.string()
            Log.d(TAG, "[GPT-RESP] HTTP status=${resp.code}")
            Log.d(TAG, "[GPT-RESP] body=\n${bodyString ?: "NULL"}")
            if (!resp.isSuccessful) {
                Log.e(TAG, "[GPT-ERR] 네트워크 실패: code=${resp.code} body=$bodyString")
                throw Exception("GPT API 오류: ${resp.code}\n${bodyString ?: ""}")
            }
            try {
                val jsonObj = JSONObject(bodyString)
                val content = jsonObj
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                Log.d(TAG, "[GPT-RESP] content=\n$content")
                return@withContext content
            } catch (e: Exception) {
                Log.e(TAG, "[GPT-ERR] JSON 파싱 실패: $bodyString", e)
                throw Exception("GPT 응답 파싱 오류: ${e.message}")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "[GPT-ERR] 전체 예외: ${e.message}", e)
        throw e
    }
}
data class GptMessage(
    val role: String,
    val content: String
)
data class GptRequest(
    val model: String,
    val messages: List<GptMessage>,
    val max_tokens: Int = 4096,
    val temperature: Double = 0.2
)
