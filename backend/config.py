"""
F.R.I.D.A.Y. Assistant Configuration Module
Manages environment variables, API keys, model routing, and security blocklists.
"""

from typing import List, Optional
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # App Settings
    APP_NAME: str = "F.R.I.D.A.Y. AI Brain"
    APP_VERSION: str = "2.0.0"
    DEBUG: bool = False
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # AI API Keys (Optional, fallback to smart offline/mock reasoning if unconfigured)
    GEMINI_API_KEY: Optional[str] = None
    OPENAI_API_KEY: Optional[str] = None

    # AI Models
    DEFAULT_GEMINI_MODEL: str = "gemini-2.5-flash"
    DEFAULT_OPENAI_MODEL: str = "gpt-4o-mini"
    MATH_ENGINE: str = "sympy"

    # Security Blocklist (Permanent: Financial / Banking / Payment Applications)
    PERMANENT_BLOCKED_PACKAGES: List[str] = [
        "com.google.android.apps.nbu.paisa.user",  # Google Pay (Tez)
        "com.phonepe.app",                         # PhonePe
        "net.one97.paytm",                         # Paytm
        "in.org.npci.upiapp",                      # BHIM UPI
        "com.dreamplug.androidapp",                # CRED
        "com.amazon.mShop.android.shopping.pay",   # Amazon Pay
        "com.sbi.lotusintouch",                    # YONO SBI
        "com.msf.kbank.mobile",                    # Kotak Bank
        "com.csam.icici.bank.imobile",             # iMobile ICICI
        "com.snapwork.hdfc",                       # HDFC MobileBanking
        "com.axis.mobile",                         # Axis Mobile
        "com.bankofbaroda.mconnect",               # BOB World
        "com.unionbank.ecommerce.mobile.android",  # Union Bank
        "com.canarabank.mobility",                 # Canara ai1
        "com.pnb.pnbone",                          # PNB ONE
        "com.mobikwik_new",                        # MobiKwik
        "com.freecharge.android"                   # Freecharge
    ]

    PERMANENT_BLOCKED_KEYWORDS: List[str] = [
        "payment", "upi", "gpay", "google pay", "phonepe", "paytm", "bhim",
        "banking", "bank account", "net banking", "credit card", "debit card",
        "cvv", "otp", "upi pin", "atm pin", "transfer money", "send money",
        "pay bill", "wallet balance", "withdraw cash"
    ]

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )


settings = Settings()

