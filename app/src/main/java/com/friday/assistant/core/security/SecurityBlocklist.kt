package com.friday.assistant.core.security

/**
 * F.R.I.D.A.Y. Permanent Security Blocklist
 * Strictly excludes all payment applications, banking apps, UPI gateways, and financial transaction mechanisms.
 * This blocklist is immutable and cannot be bypassed or overridden by user commands or settings.
 */
object SecurityBlocklist {

    const val REFUSAL_MESSAGE =
        "Sorry, I cannot access or automate payment applications. This restriction is part of my security configuration."

    // Permanent Blocked Package Identifiers
    val BLOCKED_PACKAGES: Set<String> = setOf(
        // UPI & Wallet Apps
        "com.google.android.apps.nbu.paisa.user", // Google Pay (Tez)
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "in.org.npci.upiapp",                     // BHIM UPI
        "com.dreamplug.androidapp",               // CRED
        "com.amazon.mShop.android.shopping.pay",  // Amazon Pay
        "com.mobikwik_new",                       // MobiKwik
        "com.freecharge.android",                 // FreeCharge
        "money.jupiter",                          // Jupiter
        "co.fi.money",                            // Fi Money

        // Banking Apps
        "com.sbi.lotusintouch",                   // YONO SBI
        "com.csam.icici.bank.imobile",            // iMobile Pay (ICICI)
        "com.snapwork.hdfc",                      // HDFC Bank MobileBanking
        "com.axis.mobile",                        // Axis Mobile
        "com.msf.kbank.mobile",                   // Kotak Mobile Banking
        "com.bankofbaroda.mconnect",              // BOB World
        "com.unionbank.ecommerce.mobile.android", // Union Bank of India
        "com.canarabank.mobility",                // Canara ai1
        "com.pnb.pnbone",                         // PNB ONE
        "com.idbi.idbi",                          // IDBI GO Mobile+
        "com.indusind.indie",                     // INDIE by IndusInd
        "com.fedmobile",                          // FedMobile (Federal Bank)
        "com.rblbank.mobank",                     // RBL MoBank
        "com.yesbank",                            // YES Mobile

        // International Payment Apps
        "com.paypal.android.p2pmobile",           // PayPal
        "com.squareup.cash",                      // Cash App
        "com.venmo",                              // Venmo
        "com.revolut.revolut",                    // Revolut
        "com.coinbase.android",                   // Coinbase
        "com.binance.dev"                         // Binance
    )

    // Permanent Blocked Keywords & Intent Actions
    val BLOCKED_KEYWORDS: List<String> = listOf(
        "payment", "pay", "upi", "gpay", "google pay", "phonepe", "paytm", "bhim",
        "cred", "bank", "banking", "netbanking", "net banking", "credit card", "debit card",
        "cvv", "otp", "one time password", "upi pin", "atm pin", "pin", "password",
        "transfer money", "send money", "receive money", "pay bill", "wallet balance",
        "withdraw cash", "deposit", "cheque", "account balance", "transaction"
    )

    /**
     * Checks if a target package is on the permanent blocklist.
     */
    fun isPackageBlocked(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val pkg = packageName.trim().lowercase()
        return BLOCKED_PACKAGES.any { blocked ->
            pkg == blocked || pkg.contains(blocked) || blocked.contains(pkg)
        }
    }

    /**
     * Checks if user command text contains prohibited payment/financial operations.
     */
    fun isQueryBlocked(query: String?): Boolean {
        if (query.isNullOrBlank()) return false
        val lower = query.lowercase().trim()
        val words = lower.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }

        // Check against multi-word and single-word blocked patterns
        for (keyword in BLOCKED_KEYWORDS) {
            val kwLower = keyword.lowercase()
            if (kwLower.contains(" ")) {
                if (lower.contains(kwLower)) return true
            } else {
                if (words.contains(kwLower)) return true
            }
        }
        return false
    }

    /**
     * Comprehensive security verification evaluated before ANY app launch or automation action.
     */
    fun verifySecurity(packageName: String?, query: String?): SecurityCheckResult {
        if (isPackageBlocked(packageName)) {
            return SecurityCheckResult(
                isAllowed = false,
                reason = "Blocked financial package: $packageName",
                refusalMessage = REFUSAL_MESSAGE
            )
        }

        if (isQueryBlocked(query)) {
            return SecurityCheckResult(
                isAllowed = false,
                reason = "Blocked financial/payment query keywords",
                refusalMessage = REFUSAL_MESSAGE
            )
        }

        return SecurityCheckResult(
            isAllowed = true,
            reason = "Security clearance passed",
            refusalMessage = ""
        )
    }
}

data class SecurityCheckResult(
    val isAllowed: Boolean,
    val reason: String,
    val refusalMessage: String
)

