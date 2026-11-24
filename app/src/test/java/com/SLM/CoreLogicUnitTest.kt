package com.SLM

import com.SLM.ui.i18n.BnStrings
import com.SLM.ui.i18n.EnStrings
import com.SLM.ui.i18n.stringsForLanguage
import com.SLM.utils.CurrencyUtils
import com.SLM.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicUnitTest {

    @Test
    fun `currency utils formats amounts and compact values properly`() {
        assertEquals("৳ 1,250.50", CurrencyUtils.formatAmount(1250.5, "৳"))
        assertEquals("$ 999.00", CurrencyUtils.formatAmount(999.0, "$"))
        assertEquals("৳ 2.5k", CurrencyUtils.formatCompact(2500.0, "৳"))
        assertEquals("-৳ 1.5 Lk", CurrencyUtils.formatCompact(-150000.0, "৳"))
        assertEquals("৳ 1.0 Cr", CurrencyUtils.formatCompact(10000000.0, "৳"))
    }

    @Test
    fun `date utils bounds return valid chronological ranges`() {
        val (todayStart, todayEnd) = DateUtils.getDayBounds(0)
        assertTrue(todayStart < todayEnd)
        assertTrue(DateUtils.isToday(System.currentTimeMillis()))

        val (yesterdayStart, yesterdayEnd) = DateUtils.getDayBounds(-1)
        assertTrue(yesterdayStart < yesterdayEnd)
        assertTrue(yesterdayEnd < todayStart)

        val (weekStart, weekEnd) = DateUtils.getWeekBounds()
        assertTrue(weekStart < weekEnd)

        val (monthStart, monthEnd) = DateUtils.getMonthBounds(0)
        assertTrue(monthStart < monthEnd)
        assertTrue(DateUtils.isThisMonth(System.currentTimeMillis()))
    }

    @Test
    fun `dual language strings switch cleanly between bn and en`() {
        val bn = stringsForLanguage("bn")
        val en = stringsForLanguage("en")

        assertEquals(BnStrings, bn)
        assertEquals(EnStrings, en)
        assertEquals("InEx Tracker", bn.appName)
        assertEquals("InEx Tracker", en.appName)
        assertNotEquals(bn.navHome, en.navHome)
        assertNotEquals(bn.privacyPolicyTitle, en.privacyPolicyTitle)
        assertFalse(bn.deleteAccountTitle.isBlank())
        assertFalse(en.deleteAccountTitle.isBlank())
        assertFalse(bn.deleteAccountConfirmBody.isBlank())
        assertFalse(en.deleteAccountConfirmBody.isBlank())
        assertFalse(bn.termsConditionsTitle.isBlank())
        assertFalse(en.termsConditionsTitle.isBlank())
        assertFalse(bn.splashPermTitle.isBlank())
        assertFalse(en.splashPermAllowAllBtn.isBlank())
        assertTrue(bn.pdfSavedSuccess.contains("Download/InExTracker"))
        assertTrue(en.pdfSavedSuccess.contains("Download/InExTracker"))
    }
}
