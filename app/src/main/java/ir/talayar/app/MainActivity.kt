package ir.talayar.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val Gold = Color(0xFFFFD700)
val BgDark = Color(0xFF1A2030)
val CardBg = Color(0xFF293242)
val BuyGreen = Color(0xFF4ADE80)
val SellRed = Color(0xFFFF6B6B)
val WaitAmber = Color(0xFFFFC94D)
val TextGray = Color(0xFFC7CFDC)
val TextLight = Color(0xFFFFFFFF)

const val MIN_CONFIDENCE = 75
const val MIN_RR = 1.5
const val ATR_PERIOD = 14

data class Analysis(
    val direction: String,
    val confidence: Int,
    val price: Double,
    val reason: String,
    val entryLow: Double,
    val entryHigh: Double,
    val sl: Double,
    val tp1: Double,
    val tp2: Double,
    val rr: Double,
    val atr: Double,
    val rsi: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { AppScreen() }
        }
    }
}

@Composable
fun AppScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(loadApiKey(context)) }
    var analysis by remember { mutableStateOf<Analysis?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var tempKey by remember { mutableStateOf(apiKey) }
    var lastUpdate by remember { mutableStateOf("") }

    fun doAnalyze() {
        if (apiKey.isBlank()) {
            showKeyDialog = true
            return
        }
        loading = true; error = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { Analyzer.fetch(apiKey) }
                analysis = result
                lastUpdate = SimpleDateFormat("HH:mm:ss", Locale("fa")).format(Date())
            } catch (e: Exception) {
                error = e.message ?: "خطای ناشناخته"
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) {
        if (apiKey.isNotBlank()) doAnalyze()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("طلا یار", color = Gold, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("تحلیل XAU/USD — تایم فریم ۱۵ دقیقه", color = TextGray, fontSize = 12.sp)

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { doAnalyze() },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = BgDark,
                    modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("🔄  تحلیل مجدد", color = BgDark,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (lastUpdate.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("آخرین به‌روزرسانی: $lastUpdate",
                color = TextGray, fontSize = 11.sp)
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { showKeyDialog = true }) {
            Text("تنظیم API Key", color = TextGray, fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))

        error?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SellRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️ خطا", color = SellRed,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = TextLight, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        analysis?.let { a ->
            val dirColor = when (a.direction) {
                "BUY" -> BuyGreen
                "SELL" -> SellRed
                else -> WaitAmber
            }
            val dirLabel = when (a.direction) {
                "BUY" -> "خرید"
                "SELL" -> "فروش"
                else -> "صبر"
            }
            val dirIcon = when (a.direction) {
                "BUY" -> "🟢"
                "SELL" -> "🔴"
                else -> "🟡"
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("قیمت لحظه‌ای", color = TextGray, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(String.format("%.2f", a.price), color = Gold,
                        fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(dirColor.copy(alpha = 0.18f))
                            .padding(horizontal = 32.dp, vertical = 14.dp)
                    ) {
                        Text("$dirIcon  $dirLabel", color = dirColor,
                            fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    if (a.direction != "WAIT") {
                        Spacer(Modifier.height(10.dp))
                        Text("قدرت سیگنال: ${a.confidence}%",
                            color = dirColor, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (a.direction != "WAIT") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("محدوده معامله", color = TextLight,
                                fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(14.dp))

                        RowLine("Entry",
                            String.format("%.2f تا %.2f", a.entryLow, a.entryHigh),
                            Gold)
                        RowLine("Stop Loss",
                            String.format("%.2f", a.sl), SellRed)
                        RowLine("Take Profit 1",
                            String.format("%.2f", a.tp1), BuyGreen)
                        RowLine("Take Profit 2",
                            String.format("%.2f", a.tp2), BuyGreen)

                        Divider(color = TextGray.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 8.dp))

                        RowLine("Risk : Reward",
                            String.format("1 : %.2f", a.rr),
                            if (a.rr >= 2.0) BuyGreen else WaitAmber)
                        RowLine("ATR (نوسان)",
                            String.format("%.2f دلار", a.atr), TextGray)
                        RowLine("RSI",
                            String.format("%.1f", a.rsi),
                            when {
                                a.rsi > 70 -> SellRed
                                a.rsi < 30 -> BuyGreen
                                else -> TextGray
                            })
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("تحلیل", color = TextLight,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(a.reason, color = TextGray, fontSize = 14.sp,
                        lineHeight = 22.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = WaitAmber.copy(alpha = 0.10f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "⚠️ این پیشنهاد است، نه دستور معامله.\nمسئولیت تصمیم با شماست.",
                color = WaitAmber, fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("تنظیمات API Key") },
            text = {
                Column {
                    Text("کلید API خود را از Twelve Data وارد کنید:",
                        fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    apiKey = tempKey.trim()
                    saveApiKey(context, apiKey)
                    showKeyDialog = false
                    if (apiKey.isNotBlank()) doAnalyze()
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showKeyDialog = false
                    tempKey = apiKey
                }) { Text("لغو") }
            }
        )
    }
}

@Composable
fun RowLine(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextGray, fontSize = 14.sp)
        Text(value, color = color, fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold)
    }
}

fun loadApiKey(context: Context): String {
    val prefs = context.getSharedPreferences("talayar", Context.MODE_PRIVATE)
    return prefs.getString("api_key", "") ?: ""
}

fun saveApiKey(context: Context, key: String) {
    val prefs = context.getSharedPreferences("talayar", Context.MODE_PRIVATE)
    prefs.edit().putString("api_key", key).apply()
}

object Analyzer {

    suspend fun fetch(apiKey: String): Analysis = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()

        val url = "https://api.twelvedata.com/time_series" +
                "?symbol=XAU/USD&interval=15min&outputsize=300&apikey=" + apiKey

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("پاسخ خالی از سرور")

        if (!response.isSuccessful) throw Exception("خطای شبکه: " + response.code)

        val json = JSONObject(body)
        if (json.has("status") && json.getString("status") == "error") {
            throw Exception(json.optString("message", "خطای API"))
        }
        if (!json.has("values")) {
            throw Exception("داده‌ای دریافت نشد. API Key را بررسی کنید")
        }

        val values = json.getJSONArray("values")
        val n = values.length()
        val closes = ArrayList<Double>(n)
        val highs = ArrayList<Double>(n)
        val lows = ArrayList<Double>(n)

        var i = n - 1
        while (i >= 0) {
            val c = values.getJSONObject(i)
            closes.add(c.getString("close").toDouble())
            highs.add(c.getString("high").toDouble())
            lows.add(c.getString("low").toDouble())
            i--
        }

        if (closes.size < 60) throw Exception("داده کافی نیست")

        val price = closes.last()
        val ema20 = ema(closes, 20)
        val ema50 = ema(closes, 50)
        val ema200 = if (closes.size >= 200) ema(closes, 200) else null
        val rsiVal = rsi(closes, 14)
        val atrVal = atr(highs, lows, closes, ATR_PERIOD)

        var score = 0
        val reasons = ArrayList<String>()

        if (price > ema20 && ema20 > ema50) {
            score += 25
            reasons.add("✅ روند صعودی (EMA20 > EMA50)")
        } else if (price < ema20 && ema20 < ema50) {
            score -= 25
            reasons.add("🔻 روند نزولی (EMA20 < EMA50)")
        } else {
            reasons.add("⚠️ روند نامشخص")
        }

        if (rsiVal < 30) {
            score += 15
            reasons.add(String.format("✅ RSI اشباع فروش (%.1f)", rsiVal))
        } else if (rsiVal > 70) {
            score -= 15
            reasons.add(String.format("🔻 RSI اشباع خرید (%.1f)", rsiVal))
        } else if (rsiVal > 55) {
            score += 8
            reasons.add(String.format("✅ RSI صعودی (%.1f)", rsiVal))
        } else if (rsiVal < 45) {
            score -= 8
            reasons.add(String.format("🔻 RSI نزولی (%.1f)", rsiVal))
        } else {
            reasons.add(String.format("➖ RSI خنثی (%.1f)", rsiVal))
        }

        if (closes.size >= 6) {
            val momentum = price - closes[closes.size - 6]
            val momentumRatio = momentum / atrVal
            if (momentumRatio > 0.5) {
                score += 15
                reasons.add("✅ مومنتوم صعودی قوی")
            } else if (momentumRatio < -0.5) {
                score -= 15
                reasons.add("🔻 مومنتوم نزولی قوی")
            }
        }

        if (ema200 != null) {
            if (price > ema200) {
                score += 8
                reasons.add("✅ بالای EMA200 (روند بلندمدت صعودی)")
            } else {
                score -= 8
                reasons.add("🔻 زیر EMA200 (روند بلندمدت نزولی)")
            }
        }

        val rawDirection = when {
            score >= 40 -> "BUY"
            score <= -40 -> "SELL"
            else -> "WAIT"
        }
        val confidence = minOf(95, abs(score) + 40)

        var direction = rawDirection
        if ((rawDirection == "BUY" || rawDirection == "SELL") &&
            confidence < MIN_CONFIDENCE
        ) {
            direction = "WAIT"
            reasons.clear()
            reasons.add("⚠️ قدرت سیگنال زیر $MIN_CONFIDENCE%")
            reasons.add("منتظر سیگنال قوی‌تر باشید")
        }

        var entryLow = 0.0
        var entryHigh = 0.0
        var sl = 0.0
        var tp1 = 0.0
        var tp2 = 0.0
        var rr = 0.0

        if (direction == "BUY" || direction == "SELL") {
            val atrSL = atrVal * 1.5
            val atrTP1 = atrVal * 2.5
            val atrTP2 = atrVal * 4.0

            if (direction == "BUY") {
                entryLow = price - atrVal * 0.15
                entryHigh = price + atrVal * 0.15
                val entryRef = entryHigh
                sl = entryRef - atrSL
                tp1 = entryRef + atrTP1
                tp2 = entryRef + atrTP2
                val risk = entryRef - sl
                val reward = tp1 - entryRef
                rr = if (risk > 0) reward / risk else 0.0
            } else {
                entryLow = price - atrVal * 0.15
                entryHigh = price + atrVal * 0.15
                val entryRef = entryLow
                sl = entryRef + atrSL
                tp1 = entryRef - atrTP1
                tp2 = entryRef - atrTP2
                val risk = sl - entryRef
                val reward = entryRef - tp1
                rr = if (risk > 0) reward / risk else 0.0
            }

            if (rr < MIN_RR) {
                direction = "WAIT"
                reasons.clear()
                reasons.add(String.format("⚠️ R:R پایین (1:%.2f)", rr))
                reasons.add("حداقل مورد نیاز: 1:%.1f".format(MIN_RR))
                reasons.add("منتظر ستاپ بهتر باشید")
                rr = 0.0
            }
        }

        Analysis(
            direction = direction,
            confidence = confidence,
            price = price,
            reason = reasons.joinToString("\n"),
            entryLow = entryLow,
            entryHigh = entryHigh,
            sl = sl,
            tp1 = tp1,
            tp2 = tp2,
            rr = rr,
            atr = atrVal,
            rsi = rsiVal
        )
    }

    private fun ema(values: List<Double>, period: Int): Double {
        if (values.size < period) return values.last()
        val k = 2.0 / (period + 1)
        var e = values.take(period).average()
        for (i in period until values.size) {
            e = values[i] * k + e * (1 - k)
        }
        return e
    }

    private fun rsi(values: List<Double>, period: Int): Double {
        if (values.size < period + 1) return 50.0
        var gains = 0.0
        var losses = 0.0
        for (i in 1..period) {
            val diff = values[i] - values[i - 1]
            if (diff > 0) gains += diff else losses -= diff
        }
        var avgGain = gains / period
        var avgLoss = losses / period
        for (i in period + 1 until values.size) {
            val diff = values[i] - values[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    private fun atr(highs: List<Double>, lows: List<Double>,
                    closes: List<Double>, period: Int): Double {
        if (closes.size < period + 1) return 1.0
        val trs = ArrayList<Double>(closes.size - 1)
        for (i in 1 until closes.size) {
            val tr = max(
                highs[i] - lows[i],
                max(abs(highs[i] - closes[i - 1]),
                    abs(lows[i] - closes[i - 1]))
            )
            trs.add(tr)
        }
        var atrVal = trs.take(period).average()
        for (i in period until trs.size) {
            atrVal = (atrVal * (period - 1) + trs[i]) / period
        }
        return atrVal
    }
}ctivity.kt
