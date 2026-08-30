package com.epatay.digitalwallet.util

import android.text.Spanned
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FakeSpanned(private val content: String) : Spanned, CharSequence by content {
    override fun <T : Any?> getSpans(start: Int, end: Int, type: Class<T>?): Array<T> = emptyArray<Any>() as Array<T>
    override fun getSpanStart(tag: Any?): Int = -1
    override fun getSpanEnd(tag: Any?): Int = -1
    override fun getSpanFlags(tag: Any?): Int = 0
    override fun nextSpanTransition(start: Int, limit: Int, type: Class<*>?): Int = limit
    override fun toString(): String = content
}

class InputValidationUtilsTest {

    @Test
    fun `MoneyInputFilter allows valid numbers`() {
        val filter = MoneyInputFilter(maxIntegerDigits = 9, maxDecimalPlaces = 2)
        val dest = FakeSpanned("123")
        
        val result = filter.filter("4", 0, 1, dest, 3, 3)
        assertNull(result) // null means allow as is
    }

    @Test
    fun `MoneyInputFilter blocks invalid symbols`() {
        val filter = MoneyInputFilter(maxIntegerDigits = 9, maxDecimalPlaces = 2)
        val dest = FakeSpanned("100")
        
        assertEquals("", filter.filter("$", 0, 1, dest, 3, 3))
        assertEquals("", filter.filter("#", 0, 1, dest, 3, 3))
        assertEquals("", filter.filter("!", 0, 1, dest, 3, 3))
        assertEquals("", filter.filter("a", 0, 1, dest, 3, 3))
        assertEquals("", filter.filter("%", 0, 1, dest, 3, 3))
    }

    @Test
    fun `MoneyInputFilter allows single decimal separator and blocks second`() {
        val filter = MoneyInputFilter(maxIntegerDigits = 9, maxDecimalPlaces = 2)
        val dest1 = FakeSpanned("123")
        assertNull(filter.filter(",", 0, 1, dest1, 3, 3))

        val dest2 = FakeSpanned("123,45")
        assertEquals("", filter.filter(".", 0, 1, dest2, 6, 6))
        assertEquals("", filter.filter(",", 0, 1, dest2, 6, 6))
    }

    @Test
    fun `MoneyInputFilter blocks exceeding decimal places`() {
        val filter = MoneyInputFilter(maxIntegerDigits = 9, maxDecimalPlaces = 2)
        val dest = FakeSpanned("123,45")
        assertEquals("", filter.filter("6", 0, 1, dest, 6, 6))
    }

    @Test
    fun `SafeTextInputFilter allows Turkish and Latin characters, numbers and basic punctuation`() {
        val filter = SafeTextInputFilter(maxLength = 50, allowPunctuation = true)
        val dest = FakeSpanned("Market Alisverisi")
        val input = " - Sok / Bim (2026)"
        val result = filter.filter(input, 0, input.length, dest, dest.length, dest.length)
        assertNull(result)
    }

    @Test
    fun `SafeTextInputFilter blocks dangerous code and special symbols`() {
        val filter = SafeTextInputFilter(maxLength = 50, allowPunctuation = true)
        val dest = FakeSpanned("Test")
        val input = "<script>alert('xss');</script>"
        val result = filter.filter(input, 0, input.length, dest, dest.length, dest.length)
        assertEquals("scriptalert(xss)/script", result.toString())

        val symbols = "$ # % { } [ ] \\ ~ ` @ ^"
        val symbolsResult = filter.filter(symbols, 0, symbols.length, dest, dest.length, dest.length)
        assertEquals("           ", symbolsResult.toString()) // only the spaces remain
    }

    @Test
    fun `IntegerRangeInputFilter validates integer range`() {
        val filter = IntegerRangeInputFilter(min = 1, max = 31)
        val dest = FakeSpanned("")
        assertNull(filter.filter("15", 0, 2, dest, 0, 0))
        assertEquals("", filter.filter("32", 0, 2, dest, 0, 0))
        assertEquals("", filter.filter("abc", 0, 3, dest, 0, 0))
    }

    @Test
    fun `parseMoneyValue parses Turkish and standard formats`() {
        assertEquals(1234.56, parseMoneyValue("1234,56")!!, 0.001)
        assertEquals(1234.56, parseMoneyValue("1234.56")!!, 0.001)
        assertEquals(500.0, parseMoneyValue("₺ 500")!!, 0.001)
        assertEquals(750.25, parseMoneyValue("750,25 TL")!!, 0.001)
        assertNull(parseMoneyValue(""))
        assertNull(parseMoneyValue("abc"))
    }
}