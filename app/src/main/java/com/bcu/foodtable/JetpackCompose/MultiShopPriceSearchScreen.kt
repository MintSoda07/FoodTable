package com.bcu.foodtable.JetpackCompose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.useful.ApiKeyManager
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

/* =========================
   THEME
   ========================= */

private val WarmLightColorScheme = lightColorScheme(
    primary = Color(0xFFE25532),
    onPrimary = Color.White,

    primaryContainer = Color(0xFFFFE2D6),
    onPrimaryContainer = Color(0xFF5C2B1B),

    secondary = Color(0xFFFFF4ED),
    onSecondary = Color(0xFF4B3C35),

    secondaryContainer = Color(0xFFFDE1D5),
    onSecondaryContainer = Color(0xFF5D4037),

    tertiary = Color(0xFFB9806D),
    onTertiary = Color.White,

    tertiaryContainer = Color(0xFFF3E0DC),
    onTertiaryContainer = Color(0xFF4E342E),

    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF3A2C28),

    surface = Color.White,
    onSurface = Color(0xFF2E2E2E),

    surfaceVariant = Color(0xFFFBE7DF),
    onSurfaceVariant = Color(0xFF5F5F5F),

    outline = Color(0xFFDDC7BD),
    outlineVariant = Color(0xFFF0E0D8),

    inverseSurface = Color(0xFF3A2C28),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFFF8F6B),

    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF8B0000)
)

@Composable
fun FoodTableTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmLightColorScheme,
        typography = Typography(),
        content = content
    )
}

/* =========================
   SCREEN
   ========================= */

private const val TAG = "MultiShopPriceSearch"
private const val TIMEOUT_SEC = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiShopPriceSearchScreen(
    intent: Intent,
    lottieResId: Int = com.bcu.foodtable.R.raw.loading_food // 프로젝트 리소스에 맞게 조정
) {
    FoodTableTheme {
        val searchKeyword = intent.getStringExtra("SEARCH_INGREDIENT") ?: ""
        val shopNames = listOf("네이버쇼핑", "쿠팡", "이마트몰") // 필요시 추가

        val mergedResults = remember { mutableStateListOf<MergedShopItem>() }
        var isLoading by remember { mutableStateOf(true) }
        var errorText by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        // GPT 쿼리 및 병합: 각 쇼핑몰 결과가 끝나는 즉시 리스트에 반영
        LaunchedEffect(searchKeyword) {
            isLoading = true
            errorText = null
            mergedResults.clear()

            val jobs = shopNames.map { shop ->
                launch(Dispatchers.IO) {
                    try {
                        val prompt = """
                            $shop 에서 '$searchKeyword'를 최저가순으로 5개,
                            '| 상품명 | 가격 | 평점 | 링크 |' 열이 포함된 마크다운 표로만 정리해 주세요.
                            광고, 안내, 설명 없이 표만.
                        """.trimIndent()
                        val gptResultString = getGptTableResult(prompt)
                        val parsed = parseMarkdownTableWithHttpFallback(parsedShop = shop, mdTable = gptResultString)

                        // 결과를 즉시 UI에 반영
                        withContext(Dispatchers.Main) {
                            mergedResults.addAll(parsed)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[$shop] 조회 오류: ${e.message}", e)
                    }
                }
            }

            // 모든 잡 완료 후 로딩 해제
            jobs.joinAll()
            isLoading = false

            if (mergedResults.isEmpty()) {
                errorText = "검색 결과가 없습니다."
            }
        }

        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = {
                        Text(
                            "‘$searchKeyword’ 가격 비교",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxSize()
            ) {

                // 실제 정렬 상태는 여기에서 관리 (위 TopControlBar를 진짜 인터랙션용으로 쓰려면 아래로 옮겨 사용)
                var sortOption by remember { mutableStateOf(SortOption.PRICE_ASC) }
                TopControlBar(
                    keyword = searchKeyword,
                    resultCount = mergedResults.size,
                    sortOption = sortOption,
                    onSortChange = { sortOption = it }
                )
                Spacer(Modifier.height(8.dp))

                // ***** 핵심 변경점 *****
                // 1) 첫 결과가 들어오기 전 (빈 목록 & 로딩 중) -> 풀스크린 로티
                // 2) 결과가 하나라도 들어오면 즉시 리스트 렌더
                //    - 로딩이 계속 중이라면 상단에 LinearProgressIndicator만 표시
                val hasResults = mergedResults.isNotEmpty()
                if (!hasResults && isLoading) {
                    LoadingLottieFullScreen(
                        lottieResId = lottieResId,
                        baseMessage = "가격을 비교하고 있어요…"
                    )
                } else {
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                    if (errorText != null && !hasResults) {
                        ErrorBlock(text = errorText!!)
                    } else if (!hasResults) {
                        EmptyBlock()
                    } else {
                        // 정렬은 '현재 mergedResults'를 바로 읽어서 매 번 계산
                        val sorted = mergedResults.sortedSmart(sortOption)
                        PriceCompareList(
                            results = sorted,
                            context = context,
                            searchKeyword = searchKeyword
                        )
                    }
                }
            }
        }
    }
}

/* =========================
   UI BLOCKS
   ========================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopControlBar(
    keyword: String,
    resultCount: Int,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(onClick = {}, label = { Text("키워드: $keyword") })
            Spacer(Modifier.width(8.dp))
            AssistChip(onClick = {}, label = { Text("결과: $resultCount 개") })
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SortOption.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = sortOption == option,
                    onClick = { onSortChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, SortOption.entries.size),
                    label = { Text(option.displayName) }
                )
            }
        }
    }
}

@Composable
private fun LoadingLottieFullScreen(
    lottieResId: Int,
    baseMessage: String
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieResId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )

    // 5초마다 텍스트만 갱신
    var elapsed by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            elapsed += 5
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(baseMessage, color = MaterialTheme.colorScheme.inverseOnSurface, fontSize = 14.sp)
            if (elapsed > 0) {
                Text(
                    "요청이 지연되어 ${elapsed}초 더 기다려보는 중…",
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ErrorBlock(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("문제가 발생했어요", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyBlock() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("검색 결과가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PriceCompareList(
    results: List<MergedShopItem>,
    context: Context,
    searchKeyword: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("No.", Modifier.width(40.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text("쇼핑몰", Modifier.width(70.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text("상품 / 가격 / 평점", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text("링크", Modifier.width(96.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
    Divider()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        itemsIndexed(results) { idx, item ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier
                        .padding(vertical = 12.dp, horizontal = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${idx + 1}",
                        Modifier.width(40.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        item.shop,
                        Modifier.width(70.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2
                    )

                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 8.dp, end = 6.dp)
                    ) {
                        Text(
                            item.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.price.isNotBlank()) {
                                PricePill(item.price)
                                Spacer(Modifier.width(8.dp))
                            }
                            if (item.rating.isNotBlank()) {
                                RatingPill(item.rating)
                            }
                        }
                    }

                    LinkButtonsColumn(
                        item = item,
                        context = context,
                        fallbackKeyword = searchKeyword
                    )
                }
            }
        }
    }
}

@Composable
private fun PricePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun RatingPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("⭐ $text", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun LinkButtonsColumn(
    item: MergedShopItem,
    context: Context,
    fallbackKeyword: String
) {
    val searchUrls = remember {
        mapOf(
            "네이버쇼핑" to "https://search.shopping.naver.com/search/all?query=",
            "쿠팡" to "https://www.coupang.com/np/search?component=&q=",
            "11번가" to "https://search.11st.co.kr/Search.tmall?kwd=",
            "G마켓" to "https://browse.gmarket.co.kr/search?keyword=",
            "옥션" to "https://search.auction.co.kr/search/search.aspx?keyword=",
            "이마트몰" to "https://emart.ssg.com/search.ssg?query="
        )
    }
    val searchTarget = item.name.ifBlank { fallbackKeyword }
    val encoded = Uri.encode(searchTarget)
    val searchUrl = searchUrls[item.shop]?.plus(encoded)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        if (item.linkUrl.startsWith("http")) {
            Text(
                text = "바로가기",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        Log.d(TAG, "상품 바로가기: ${item.linkUrl}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.linkUrl)))
                    }
                    .padding(6.dp)
            )
        } else {
            Text(
                "링크없음",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(6.dp)
            )
        }

        if (!searchUrl.isNullOrBlank()) {
            Text(
                text = "쇼핑몰 검색",
                color = Color(0xFF0B8043),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        Log.d(TAG, "쇼핑몰 검색 링크 이동: $searchUrl")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)))
                    }
                    .padding(6.dp)
            )
        }
    }
}

/* =========================
   SORT / DATA
   ========================= */

enum class SortOption(val displayName: String) {
    PRICE_ASC("가격↑"),
    RATING_DESC("평점↓"),
    SHOP("쇼핑몰순")
}

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

/* =========================
   PARSER / UTIL
   ========================= */

val httpRegex = Regex("""https?://[^\s\|)>\]]+""")
val parenUrlRegex = Regex("""\(?https?://[^\s\|)>\]]+\)?""")
val markdownLinkRegex = Regex("""\[(.*?)\]\((https?://[^\s\|)>\]]+)\)""")

fun cleanNameAndExtractUrl(rawName: String): Pair<String, String?> {
    val markdownMatch = markdownLinkRegex.find(rawName)
    if (markdownMatch != null) {
        val nameOnly = markdownMatch.groupValues[1].trim()
        val link = markdownMatch.groupValues[2].trim()
        return nameOnly to link
    }

    val parenUrlMatch = parenUrlRegex.find(rawName)
    val url = parenUrlMatch?.value?.replace("(", "")?.replace(")", "")?.trim()
    val cleaned = rawName
        .replace(markdownLinkRegex, "")
        .replace(parenUrlRegex, "")
        .replace(httpRegex, "")
        .replace("[", "")
        .replace("]", "")
        .replace("(", "")
        .replace(")", "")
        .replace("．", "")
        .trim()
        .replace(Regex("""\s{2,}"""), " ")
        .replace(Regex("""^[\.,;:]+"""), "")
        .replace(Regex("""[\.,;:]+$"""), "")
    if (url != null && url.startsWith("http")) return cleaned to url
    return cleaned to null
}

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
        val linkUrl = row.getOrNull(idxLink) ?: ""

        val (nameClean, urlFromName) = cleanNameAndExtractUrl(rawName)
        val linkUrlFinal = when {
            linkUrl.startsWith("http") -> linkUrl
            !urlFromName.isNullOrBlank() -> urlFromName
            else -> ""
        }

        Log.d(TAG, "[ROW $rowIdx] [$parsedShop] name='$rawName' → clean='$nameClean', price='$price', rating='$rating', link='$linkUrlFinal'")

        if (nameClean.isBlank() || price.isBlank()) return@mapIndexedNotNull null
        MergedShopItem(parsedShop, nameClean, price, rating, linkUrlFinal)
    }
}

/* =========================
   GPT CALL
   ========================= */

data class MergedShopItem(
    val shop: String,
    val name: String,
    val price: String,
    val rating: String,
    val linkUrl: String
)

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

data class GptMessage(val role: String, val content: String)
data class GptRequest(
    val model: String,
    val messages: List<GptMessage>,
    val max_tokens: Int = 4096,
    val temperature: Double = 0.2
)
