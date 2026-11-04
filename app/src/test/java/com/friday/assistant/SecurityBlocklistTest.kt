package com.friday.assistant

import com.friday.assistant.core.security.SecurityBlocklist
import org.junit.Assert.*
import org.junit.Test

class SecurityBlocklistTest {

    @Test
    fun testGPayPackageBlocked() {
        val pkg = "com.google.android.apps.nbu.paisa.user"
        assertTrue(SecurityBlocklist.isPackageBlocked(pkg))
        val result = SecurityBlocklist.verifySecurity(pkg, null)
        assertFalse(result.isAllowed)
        assertEquals(SecurityBlocklist.REFUSAL_MESSAGE, result.refusalMessage)
    }

    @Test
    fun testPhonePePackageBlocked() {
        val pkg = "com.phonepe.app"
        assertTrue(SecurityBlocklist.isPackageBlocked(pkg))
        val result = SecurityBlocklist.verifySecurity(pkg, null)
        assertFalse(result.isAllowed)
        assertEquals(SecurityBlocklist.REFUSAL_MESSAGE, result.refusalMessage)
    }

    @Test
    fun testPaytmPackageBlocked() {
        val pkg = "net.one97.paytm"
        assertTrue(SecurityBlocklist.isPackageBlocked(pkg))
        val result = SecurityBlocklist.verifySecurity(pkg, null)
        assertFalse(result.isAllowed)
        assertEquals(SecurityBlocklist.REFUSAL_MESSAGE, result.refusalMessage)
    }

    @Test
    fun testBankingPackagesBlocked() {
        assertTrue(SecurityBlocklist.isPackageBlocked("com.sbi.lotusintouch"))
        assertTrue(SecurityBlocklist.isPackageBlocked("com.csam.icici.bank.imobile"))
        assertTrue(SecurityBlocklist.isPackageBlocked("com.snapwork.hdfc"))
        assertTrue(SecurityBlocklist.isPackageBlocked("com.axis.mobile"))
        assertTrue(SecurityBlocklist.isPackageBlocked("in.org.npci.upiapp"))
    }

    @Test
    fun testPaymentKeywordsBlocked() {
        assertTrue(SecurityBlocklist.isQueryBlocked("Open Google Pay"))
        assertTrue(SecurityBlocklist.isQueryBlocked("Transfer money to Alex"))
        assertTrue(SecurityBlocklist.isQueryBlocked("Enter UPI PIN 4421"))
        assertTrue(SecurityBlocklist.isQueryBlocked("Pay electricity bill"))
        assertTrue(SecurityBlocklist.isQueryBlocked("What is my bank account balance?"))
        assertTrue(SecurityBlocklist.isQueryBlocked("Show credit card CVV"))
    }

    @Test
    fun testPermittedAppsAllowed() {
        assertFalse(SecurityBlocklist.isPackageBlocked("com.whatsapp"))
        assertFalse(SecurityBlocklist.isPackageBlocked("com.instagram.android"))
        assertFalse(SecurityBlocklist.isPackageBlocked("com.android.chrome"))
        assertFalse(SecurityBlocklist.isPackageBlocked("com.google.android.youtube"))
        assertFalse(SecurityBlocklist.isPackageBlocked("com.google.android.apps.maps"))
    }

    @Test
    fun testPermittedQueriesAllowed() {
        val query1 = "Friday, open Chrome and search for the latest AI news."
        val result1 = SecurityBlocklist.verifySecurity("com.android.chrome", query1)
        assertTrue(result1.isAllowed)

        val query2 = "Open WhatsApp and message Mom"
        val result2 = SecurityBlocklist.verifySecurity("com.whatsapp", query2)
        assertTrue(result2.isAllowed)

        val query3 = "Calculate derivative of x squared plus 5x"
        val result3 = SecurityBlocklist.verifySecurity(null, query3)
        assertTrue(result3.isAllowed)
    }
}

