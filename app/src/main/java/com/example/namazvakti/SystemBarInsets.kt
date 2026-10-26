package com.example.namazvakti

import com.firstnamazvakti.R
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Android 15+ (hedef SDK 35 ve üzeri) uygulamaları ekranı kenardan kenara çizer;
 * içerik durum çubuğunun (saat, pil) ve gezinme çubuğunun altına taşar.
 *
 * Bu fonksiyon setContentView(...) çağrısından HEMEN SONRA çağrılır ve:
 *  - içeriğe durum çubuğu / çentik / gezinme çubuğu / klavye kadar boşluk ekler,
 *  - sistem çubuklarının arkasında kalan alanı ekranın arka plan rengiyle boyar,
 *  - koyu arka planda durum çubuğu ikonlarını açık renk yapar.
 */
fun Activity.applySystemBarInsets() {
    val content: ViewGroup = findViewById(android.R.id.content)

    // Arka plan rengi: ilk çocuğun düz rengi varsa o, yoksa uygulamanın lacivert tonu
    val bgColor = (content.getChildAt(0)?.background as? ColorDrawable)?.color
        ?: Color.parseColor("#0F172A")
    window.setBackgroundDrawable(ColorDrawable(bgColor))

    val darkBackground = ColorUtils.calculateLuminance(bgColor) < 0.5
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.isAppearanceLightStatusBars = !darkBackground
    controller.isAppearanceLightNavigationBars = !darkBackground

    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
        )
        view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(content)
}
