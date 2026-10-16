score += 25
            reasons.add("✅ روند صعودی (EMA20 > EMA50)")
        } else if (price < ema20 && ema20 < ema50) {
            score -= 25
            reasons.add("🔻 روند نزولی (EMA20 < EMA50)")
        } else {
            reasons.add("⚠️ روند نامشخص")
        }

        // 2) RSI
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

        // 3) مومنتوم (۵ کندل)
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

        // 4) EMA200 (روند بلندمدت)
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

        // ---- فیلتر قدرت ----
        var direction = rawDirection
        if ((rawDirection == "BUY" || rawDirection == "SELL") &&
            confidence < MIN_CONFIDENCE
        ) {
            direction = "WAIT"
            reasons.clear()
            reasons.add("⚠️ قدرت سیگنال زیر $MIN_CONFIDENCE%")
            reasons.add("منتظر سیگنال قوی‌تر باشید")
        }

        // ---- محاسبه سطوح بر اساس ATR ----
        var entryLow = 0.0
        var entryHigh = 0.0
        var sl = 0.0
        var tp1 = 0.0
        var tp2 = 0.0
        var rr = 0.0

        if (direction == "BUY" || direction == "SELL") {
            val atrSL = atrVal * 1.5      // SL بر اساس ATR
            val atrTP1 = atrVal * 2.5     // TP1
            val atrTP2 = atrVal * 4.0     // TP2

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

            // ---- فیلتر R:R ----
            if (rr < MIN_RR) {
                direction = "WAIT"
                reasons.clear()
                reasons.add(String.format("⚠️ R:R پایین (1:%.2f)", rr))
                reasons.add("حداقل مورد نیاز: 1:%.1f".format(MIN_RR))
                reasons.add("منتظر ستاپ بهتر باشید")
                rr = 0.0
            }
        }
