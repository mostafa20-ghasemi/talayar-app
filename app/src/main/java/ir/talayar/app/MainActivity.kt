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
import java.util.concurrent.TimeUnit


data class Analysis(
 val Gold = Color(0xFFFFD700)
val BgDark = Color(0xFF1A2030)
val CardBg = Color(0xFF293242)
val BuyGreen = Color(0xFF4ADE80)
val SellRed = Color(0xFFFF6B6B)
val WaitAmber = Color(0xFFFFC94D)
val TextGray = Color(0xFFC7CFDC)
val TextLight = Color(0xFFFFFFFF)   val direction: String,
    val confidence: Int,
    val price: Double,
    val reason: String,
    val entry: String,
    val sl: String,
    val tp: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppScreen()
            }
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

    fun doAnalyze() {
        if (apiKey.isBlank()) {
            showKeyDialog = true
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    Analyzer.fetch(apiKey)
                }
                analysis = result
            } catch (e: Exception) {
                error = e.message ?: "خطای ناشناخته"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (apiKey.isNotBlank()) doAnalyze()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("طلا یار", color = Gold, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("تحلیل XAU/USD", color = TextGray, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "تایم فریم: 15 دقیقه",
            color = TextGray, fontSize = 12.sp
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { doAnalyze() },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = BgDark,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("تحلیل مجدد", color = BgDark, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { showKeyDialog = true }) {
            Text("تنظیم API Key", color = TextGray, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))

        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SellRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("خطا", color = SellRed, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(error ?: "", color = TextLight, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        String.format("%.2f", a.price),
                        color = Gold, fontSize = 40.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(dirColor.copy(alpha = 0.15f))
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            dirLabel,
                            color = dirColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (a.direction != "WAIT") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "قدرت سیگنال: ${a.confidence}%",
                            color = dirColor,
                            fontSize = 14.sp
                        )
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
                        Text("محدوده معامله", color = TextLight,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))

                        RowLine("Entry", a.entry, Gold)
                        RowLine("Stop Loss", a.sl, SellRed)
                        RowLine("Take Profit", a.tp, BuyGreen)
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
                    Text("تحلیل", color = TextLight,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(a.reason, color = TextGray, fontSize = 14.sp)
                }
            }
        }

        if (analysis == null && !loading && error == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("دکمه «تحلیل مجدد» را بزنید",
                        color = TextGray, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "این پیشنهاد است، نه دستور معامله.\nمسئولیت تصمیم با شماست.",
            color = TextGray, fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("API Key تنظیمات") },
            text = {
                Column {
                    Text(
                        "کلید API خود از Twelve Data را وارد کنید:",
                        fontSize = 13.sp
                    )
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
                }) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showKeyDialog = false
                    tempKey = apiKey
                }) {
                    Text("لغو")
                }
            }
        )
    }
}

@Composable
fun RowLine(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextGray, fontSize = 14.sp)
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val url = "https://api.twelvedata.com/time_series" +
                "?symbol=XAU/USD&interval=15min&outputsize=200&apikey=" + apiKey

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("پاسخ خالی از سرور")

        if (!response.isSuccessful) {
            throw Exception("خطای شبکه: " + response.code)
        }

        val json = JSONObject(body)
        if (json.has("status") && json.getString("status") == "error") {
            throw Exception(json.optString("message", "خطای API"))
        }
        if (!json.has("values")) {
            throw Exception("داده‌ای دریافت نشد. API Key را بررسی کنید")
        }

        val values = json.getJSONArray("values")
        val closes = ArrayList<Double>()
        var i = values.length() - 1
        while (i >= 0) {
            closes.add(values.getJSONObject(i).getString("close").toDouble())
            i--
        }

        if (closes.size < 50) throw Exception("داده کافی نیست")

        val price = closes.last()
        val ema20 = ema(closes, 20)
        val ema50 = ema(closes, 50)
        val rsiVal = rsi(closes, 14)

        var score = 0
        val reasons = ArrayList<String>()

        if (price > ema20 && ema20 > ema50) {
            score += 40
            reasons.add("روند صعودی (EMA20>EMA50)")
        } else if (price < ema20 && ema20 < ema50) {
            score -= 40
            reasons.add("روند نزولی (EMA20<EMA50)")
        } else {
            reasons.add("روند نامشخص")
        }

        if (rsiVal < 30) {
            score += 20
            reasons.add("RSI اشباع فروش")
        } else if (rsiVal > 70) {
            score -= 20
            reasons.add("RSI اشباع خرید")
        } else if (rsiVal > 55) {
            score += 10
            reasons.add("RSI صعودی")
        } else if (rsiVal < 45) {
            score -= 10
            reasons.add("RSI نزولی")
        } else {
            reasons.add("RSI خنثی")
        }

        val direction = when {
            score >= 40 -> "BUY"
            score <= -40 -> "SELL"
            else -> "WAIT"
        }

        val confidence = minOf(95, kotlin.math.abs(score) + 30)
        val atrVal = 3.0

        var entry = "-"
        var sl = "-"
        var tp = "-"

        if (direction == "BUY") {
            entry = String.format("%.2f تا %.2f", price - 0.3, price + 0.3)
            sl = String.format("%.2f", price - atrVal * 1.5)
            tp = String.format("%.2f", price + atrVal * 3.0)
        } else if (direction == "SELL") {
            entry = String.format("%.2f تا %.2f", price - 0.3, price + 0.3)
            sl = String.format("%.2f", price + atrVal * 1.5)
            tp = String.format("%.2f", price - atrVal * 3.0)
        }

        Analysis(
            direction = direction,
            confidence = confidence,
            price = price,
            reason = reasons.joinToString("\n"),
            entry = entry,
            sl = sl,
            tp = tp
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
        for (i in values.size - period until values.size) {
            val diff = values[i] - values[i - 1]
            if (diff > 0) gains += diff else losses -= diff
        }
        val avgGain = gains / period
        val avgLoss = losses / period
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }
}
