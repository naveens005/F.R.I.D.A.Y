"""
Comprehensive Pytest Test Suite for F.R.I.D.A.Y. Backend
Verifies Security Blocklist, Math Engine, Model Router, Browser Integration, and Endpoints directly.
"""

import asyncio
import pytest
import sys
import os

# Ensure backend root is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from main import health_check, read_root
from routers.assistant import process_chat, AssistantRequest
from routers.math_solver import compute_derivative, compute_integral, solve_equation, compute_statistics, convert_units, DerivativeRequest, IntegralRequest, EquationRequest, StatisticsRequest, ConversionRequest
from services.math_engine import math_engine
from services.model_router import model_router, SECURITY_REFUSAL_MESSAGE


def test_health_endpoint():
    data = health_check()
    assert data["status"] == "healthy"
    assert data["engines"]["security_blocklist"] == "ENFORCED"
    assert "SymPy" in data["engines"]["math"]


def test_root_endpoint():
    data = read_root()
    assert data["assistant"] == "F.R.I.D.A.Y."
    assert "PERMANENT_PAYMENT_EXCLUSION_ACTIVE" in data["security_policy"]


# ============================================================================
# SECURITY BLOCKLIST TESTS (STRICT VERIFICATION)
# ============================================================================

def test_security_blocklist_google_pay():
    query = "Open Google Pay and transfer money"
    result = model_router.check_security_blocklist(query)
    assert result == SECURITY_REFUSAL_MESSAGE


def test_security_blocklist_phonepe():
    query = "Friday, open PhonePe to pay electricity bill"
    result = model_router.check_security_blocklist(query)
    assert result == SECURITY_REFUSAL_MESSAGE


def test_security_blocklist_paytm():
    query = "Open Paytm and check wallet balance"
    result = model_router.check_security_blocklist(query)
    assert result == SECURITY_REFUSAL_MESSAGE


def test_security_blocklist_upi_pin():
    query = "Enter UPI PIN 1234"
    result = model_router.check_security_blocklist(query)
    assert result == SECURITY_REFUSAL_MESSAGE


def test_security_blocklist_blocked_package():
    blocked_pkg = "com.google.android.apps.nbu.paisa.user"
    result = model_router.check_security_blocklist("Launch application", package_name=blocked_pkg)
    assert result == SECURITY_REFUSAL_MESSAGE


def test_security_blocklist_banking():
    query = "Open YONO SBI net banking"
    result = model_router.check_security_blocklist(query)
    assert result == SECURITY_REFUSAL_MESSAGE


def test_assistant_api_security_interception():
    req = AssistantRequest(query="Open Google Pay")
    res = asyncio.run(process_chat(req))
    assert res.is_security_blocked is True
    assert res.intent == "SECURITY_BLOCKED"
    assert res.speech_response == SECURITY_REFUSAL_MESSAGE


# ============================================================================
# DETERMINISTIC MATHEMATICS ENGINE TESTS
# ============================================================================

def test_math_derivative():
    # Derivative of x^2 + 5x should be 2x + 5
    res = math_engine.calculate_derivative("x^2 + 5*x", variable="x")
    assert res["success"] is True
    assert res["result"] == "2*x + 5"
    assert len(res["steps"]) > 0

    # API Endpoint check
    req = DerivativeRequest(expression="x^2 + 5*x", variable="x", order=1)
    api_res = compute_derivative(req)
    assert api_res["result"] == "2*x + 5"


def test_math_integral():
    # Indefinite integral of 2x should be x^2
    res = math_engine.calculate_integral("2*x", variable="x")
    assert res["success"] is True
    assert "x**2" in res["result"] or "x^2" in res["result"]

    # Definite integral of 2x from 0 to 3 should be 3^2 - 0 = 9
    res_def = math_engine.calculate_integral("2*x", variable="x", lower=0, upper=3)
    assert res_def["success"] is True
    assert res_def["result"] == "9"


def test_math_solve_quadratic_equation():
    # Solve x^2 - 5x + 6 = 0 -> roots are 2, 3
    res = math_engine.solve_equation("x^2 - 5*x + 6 = 0")
    assert res["success"] is True
    assert "2" in res["solutions"]
    assert "3" in res["solutions"]


def test_math_statistics():
    dataset = [10.0, 20.0, 30.0, 40.0, 50.0]
    res = math_engine.calculate_statistics(dataset)
    assert res["success"] is True
    assert res["mean"] == 30.0
    assert res["count"] == 5
    assert res["median"] == 30.0


def test_math_unit_conversion():
    res = math_engine.convert_units(25.0, "km", "meters")
    assert res["success"] is True
    assert res["result"] == 25000.0


# ============================================================================
# APP INTENT & CHAT TESTS
# ============================================================================

def test_app_control_whatsapp():
    result = asyncio.run(model_router.route_and_execute("Open WhatsApp"))
    assert result["success"] is True
    assert result["intent"] == "APP_CONTROL"
    assert result["action"]["package_name"] == "com.whatsapp"
    assert result["action"]["app_name"] == "WhatsApp"


def test_app_control_chrome_search():
    result = asyncio.run(model_router.route_and_execute("Open Chrome and search for machine learning courses"))
    assert result["success"] is True
    assert result["intent"] == "APP_CONTROL"
    assert result["action"]["package_name"] == "com.android.chrome"
    assert "machine learning courses" in result["action"]["query"]


def test_assistant_chat_qa():
    req = AssistantRequest(query="Friday, what is artificial intelligence?")
    res = asyncio.run(process_chat(req))
    assert res.success is True
    assert "artificial intelligence" in res.speech_response.lower()


def test_assistant_followup_examples():
    req = AssistantRequest(query="Give me three real-world examples")
    res = asyncio.run(process_chat(req))
    assert res.success is True
    assert "self-driving" in res.speech_response.lower() or "medical" in res.speech_response.lower()
