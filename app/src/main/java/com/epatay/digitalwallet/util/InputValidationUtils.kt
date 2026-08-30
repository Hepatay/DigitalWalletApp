package com.epatay.digitalwallet.util

import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Parasal ve ondalık sayılar için özel InputFilter.
 * - Sadece 0-9 rakamlarına ve maksimum 1 adet ondalık ayraca (. veya ,) izin verir.
 * - Özel karakterleri ($ > # ½ £ { } ! % vb.) anında engeller.
 * - Tam basamak ve ondalık basamak sayılarını kısıtlar.
 */
class MoneyInputFilter(
    private val maxIntegerDigits: Int = 9,
    private val maxDecimalPlaces: Int = 2
) : InputFilter {

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val replacement = source.subSequence(start, end).toString()
        val destString = dest.toString()
        val newString = destString.substring(0, dstart) + replacement + destString.substring(dend)

        if (newString.isEmpty()) return null

        // 1. İzin verilmeyen karakterleri tespit et
        for (i in start until end) {
            val c = source[i]
            if (!c.isDigit() && c != '.' && c != ',') {
                return "" // Blokla
            }
        }

        // 2. Maksimum tek bir ayraç kontrolü
        val separatorCount = newString.count { it == '.' || it == ',' }
        if (separatorCount > 1) {
            return "" // Birden fazla ayraç kabul edilmez
        }

        // 3. Basamak sınırları kontrolü
        val separatorIndex = newString.indexOfAny(charArrayOf('.', ','))
        if (separatorIndex >= 0) {
            val integerPart = newString.substring(0, separatorIndex)
            val decimalPart = newString.substring(separatorIndex + 1)

            if (integerPart.length > maxIntegerDigits || decimalPart.length > maxDecimalPlaces) {
                return ""
            }
        } else {
            if (newString.length > maxIntegerDigits) {
                return ""
            }
        }

        return null // Geçerli, izin ver
    }
}

/**
 * Güvenli metin girişi için InputFilter.
 * - Türkçe ve Latin harfleri, rakamlar, boşluk ve temel noktalama işaretlerine izin verir.
 * - Potansiyel zararlı kod / injection karakterlerini engeller.
 */
class SafeTextInputFilter(
    private val maxLength: Int = 50,
    private val allowPunctuation: Boolean = true
) : InputFilter {

    companion object {
        // İzin verilen temel noktalama işaretleri
        private const val ALLOWED_PUNCTUATION = ".,-_()/ "
        // Türkçe karakter kümesi
        private const val TURKISH_CHARS = "abcçdefgğhıijklmnoöprsştuüvyzABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ"
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val destLength = dest.length
        val keep = maxLength - (destLength - (dend - dstart))

        val sb = StringBuilder()
        for (i in start until end) {
            val c = source[i]
            val isLetter = (c in 'a'..'z') || (c in 'A'..'Z') || (c in TURKISH_CHARS)
            val isDigit = c.isDigit()
            val isAllowedPunctuation = allowPunctuation && (c in ALLOWED_PUNCTUATION)

            if (isLetter || isDigit || isAllowedPunctuation) {
                sb.append(c)
            }
        }

        val filtered = sb.toString()
        if (filtered.length != (end - start)) {
            // Bazı karakterler engellendi
            return if (keep <= 0) "" else filtered.take(keep)
        }

        if (keep <= 0) {
            return ""
        } else if (keep >= end - start) {
            return null // Değişiklik yapmadan kabul et
        } else {
            return source.subSequence(start, start + keep)
        }
    }
}

/**
 * Belirli bir aralıkta tamsayı girişine izin veren InputFilter (Örn: Ayın günü 1-31).
 */
class IntegerRangeInputFilter(
    private val min: Int = 1,
    private val max: Int = 31
) : InputFilter {

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val replacement = source.subSequence(start, end).toString()
        val destString = dest.toString()
        val newString = destString.substring(0, dstart) + replacement + destString.substring(dend)

        if (newString.isEmpty()) return null

        if (newString.any { !it.isDigit() }) return ""

        val value = newString.toIntOrNull() ?: return ""
        if (value in 0..max) {
            return null
        }
        return ""
    }
}

/**
 * Parasal girdi alanını kurar ve bağlar.
 */
fun EditText.setupMoneyInput(
    maxValue: Double = 999_999_999.99,
    maxDecimals: Int = 2,
    maxIntegerDigits: Int = 9,
    layout: TextInputLayout? = null,
    onValidStateChanged: ((Boolean) -> Unit)? = null
) {
    this.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    this.filters = arrayOf(
        MoneyInputFilter(maxIntegerDigits = maxIntegerDigits, maxDecimalPlaces = maxDecimals),
        InputFilter.LengthFilter(maxIntegerDigits + maxDecimals + 2)
    )

    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable?) {
            val text = s?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                layout?.error = null
                onValidStateChanged?.invoke(false)
                return
            }

            val value = parseMoneyValue(text)
            if (value == null) {
                layout?.error = "Geçersiz tutar formatı"
                onValidStateChanged?.invoke(false)
            } else if (value > maxValue) {
                val formattedMax = formatMoneyDisplay(maxValue)
                layout?.error = "Maksimum limiti aştınız (En fazla ₺$formattedMax)"
                onValidStateChanged?.invoke(false)
            } else {
                layout?.error = null
                onValidStateChanged?.invoke(true)
            }
        }
    })
}

/**
 * Güvenli metin girdi alanını kurar ve bağlar.
 */
fun EditText.setupTextInput(
    maxLength: Int = 50,
    allowPunctuation: Boolean = true,
    layout: TextInputLayout? = null
) {
    this.filters = arrayOf(
        SafeTextInputFilter(maxLength = maxLength, allowPunctuation = allowPunctuation),
        InputFilter.LengthFilter(maxLength)
    )

    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (!s.isNullOrBlank()) {
                layout?.error = null
            }
        }
    })
}

/**
 * Tamsayı aralığı girdi alanını kurar ve bağlar (Örn: Ayın günü 1-31).
 */
fun EditText.setupIntegerInput(
    minValue: Int = 1,
    maxValue: Int = 31,
    layout: TextInputLayout? = null
) {
    this.inputType = InputType.TYPE_CLASS_NUMBER
    this.filters = arrayOf(
        IntegerRangeInputFilter(min = minValue, max = maxValue),
        InputFilter.LengthFilter(maxValue.toString().length)
    )

    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable?) {
            val text = s?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                layout?.error = null
                return
            }
            val num = text.toIntOrNull()
            if (num == null || num < minValue || num > maxValue) {
                layout?.error = "$minValue ile $maxValue arasında bir değer girin"
            } else {
                layout?.error = null
            }
        }
    })
}

/**
 * Kullanıcı girdisini güvenli bir şekilde Double değere dönüştürür.
 * Virgül veya nokta ayracını normalize eder.
 */
fun parseMoneyValue(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    val cleaned = raw.trim().replace("₺", "").replace("TL", "").trim()
    val normalized = cleaned.replace(',', '.')
    return normalized.toDoubleOrNull()
}

/**
 * Sayıyı ekranda formatlı göstermek için yardımcı fonksiyon.
 */
fun formatMoneyDisplay(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,##0.00", symbols)
    return formatter.format(amount)
}