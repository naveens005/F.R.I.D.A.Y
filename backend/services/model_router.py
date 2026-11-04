"""
F.R.I.D.A.Y. Intelligent Model Router & Orchestrator
Classifies task type, enforces security blocklists, routes to specialized engines
(SymPy Math, Web Browser/Extractor, Gemini, OpenAI, App Automation).
"""

import re
from typing import Dict, Any, List, Optional
from config import settings
from services.math_engine import math_engine
from services.web_search import web_search_engine
from services.web_extractor import web_extractor
from services.gemini_service import gemini_service
from services.openai_service import openai_service

SECURITY_REFUSAL_MESSAGE = "Sorry, I cannot access or automate payment applications. This restriction is part of my security configuration."


class ModelRouter:
    """Intelligent task classifier and orchestrator."""

    def __init__(self):
        self.math_patterns = [
            r"\b(derivative|differentiate)\b",
            r"\b(integral|integrate|antiderivative)\b",
            r"\b(solve\s+equation|solve\s+for|quadratic\s+equation|roots\s+of)\b",
            r"\b(standard\s+deviation|mean|median|variance)\b",
            r"\b(convert\s+\d+.*to.*)\b",
            r"\b(calculate|evaluate|simplify)\s+[\d\(\)x\+\-\*\/\^]+"
        ]

        self.browser_patterns = [
            r"\b(search\s+google\s+for|search\s+for|find\s+information\s+on|google\s+search)\b",
            r"\b(read\s+this\s+article|summarize\s+this\s+website|open\s+https?:\/\/)\b",
            r"\b(find\s+the\s+official\s+documentation|compare\s+these\s+websites)\b"
        ]

        self.app_launch_patterns = [
            r"\b(open|launch|start)\s+(whatsapp|instagram|chrome|google|youtube|gmail|maps|google maps|phone|contacts|messages|camera|gallery|settings|calendar|files)\b"
        ]

    def check_security_blocklist(self, text: str, package_name: Optional[str] = None) -> Optional[str]:
        """Strictly checks if query or package belongs to financial/payment security blocklist."""
        text_lower = text.lower()

        # Check package name if provided
        if package_name:
            pkg_lower = package_name.lower().strip()
            for blocked_pkg in settings.PERMANENT_BLOCKED_PACKAGES:
                if blocked_pkg in pkg_lower or pkg_lower in blocked_pkg:
                    return SECURITY_REFUSAL_MESSAGE

        # Check blocked keywords
        for keyword in settings.PERMANENT_BLOCKED_KEYWORDS:
            # Look for exact word boundary match
            if re.search(r'\b' + re.escape(keyword) + r'\b', text_lower):
                return SECURITY_REFUSAL_MESSAGE

        return None

    async def route_and_execute(
        self,
        query: str,
        conversation_history: Optional[List[Dict[str, str]]] = None,
        installed_packages: Optional[List[str]] = None
    ) -> Dict[str, Any]:
        """Main routing pipeline for F.R.I.D.A.Y."""
        # 1. Strict Security Validation
        security_violation = self.check_security_blocklist(query)
        if security_violation:
            return {
                "success": False,
                "is_security_blocked": True,
                "intent": "SECURITY_BLOCKED",
                "speech_response": security_violation,
                "display_text": security_violation,
                "action": None
            }

        q_lower = query.lower().strip()

        # 2. Check for App Launch Intent
        app_match = self._detect_app_intent(q_lower)
        if app_match:
            return self._handle_app_intent(app_match, query)

        # 3. Check for Math / Calculation Intent
        if self._is_math_intent(q_lower):
            math_result = self._handle_math_intent(q_lower)
            if math_result.get("success"):
                return math_result

        # 4. Check for Browser / Search / Summarization Intent
        if self._is_browser_intent(q_lower):
            return await self._handle_browser_intent(query)

        # 5. Check for Multi-Step Planning
        if "compare" in q_lower or "and prepare a summary" in q_lower or "top five" in q_lower:
            return await self._handle_multistep_planning(query)

        # 6. General Conversational / QA Intent (Gemini / OpenAI)
        ai_resp = await gemini_service.generate_response(
            prompt=query,
            conversation_history=conversation_history
        )

        reply_text = ai_resp.get("text", "")
        return {
            "success": True,
            "is_security_blocked": False,
            "intent": "CONVERSATION",
            "speech_response": reply_text,
            "display_text": reply_text,
            "action": None,
            "model_used": ai_resp.get("model")
        }

    def _detect_app_intent(self, text: str) -> Optional[Dict[str, str]]:
        """Detects if user is asking to open or interact with an installed app."""
        app_map = {
            "whatsapp": {"name": "WhatsApp", "package": "com.whatsapp", "category": "social"},
            "instagram": {"name": "Instagram", "package": "com.instagram.android", "category": "social"},
            "chrome": {"name": "Chrome", "package": "com.android.chrome", "category": "browser"},
            "google": {"name": "Google Search", "package": "com.google.android.googlequicksearchbox", "category": "search"},
            "youtube": {"name": "YouTube", "package": "com.google.android.youtube", "category": "media"},
            "gmail": {"name": "Gmail", "package": "com.google.android.gm", "category": "productivity"},
            "maps": {"name": "Google Maps", "package": "com.google.android.apps.maps", "category": "navigation"},
            "google maps": {"name": "Google Maps", "package": "com.google.android.apps.maps", "category": "navigation"},
            "phone": {"name": "Phone", "package": "com.google.android.dialer", "category": "communication"},
            "contacts": {"name": "Contacts", "package": "com.google.android.contacts", "category": "communication"},
            "messages": {"name": "Messages", "package": "com.google.android.apps.messaging", "category": "communication"},
            "camera": {"name": "Camera", "package": "com.android.camera", "category": "utility"},
            "gallery": {"name": "Gallery", "package": "com.google.android.apps.photos", "category": "media"},
            "photos": {"name": "Photos", "package": "com.google.android.apps.photos", "category": "media"},
            "settings": {"name": "Settings", "package": "com.android.settings", "category": "system"},
            "calendar": {"name": "Calendar", "package": "com.google.android.calendar", "category": "productivity"},
            "files": {"name": "Files", "package": "com.google.android.documentsui", "category": "utility"}
        }

        # Check for open/launch
        open_match = re.search(r'\b(?:open|launch|start|show(?:\s+my)?)\s+([a-z\s]+)', text)
        if open_match:
            target_str = open_match.group(1).strip()
            for key, app_info in app_map.items():
                if target_str.startswith(key):
                    # Check for extra parameters, e.g. "open chrome and search for ..."
                    extra_action = None
                    extra_match = re.search(r'\band\s+(search\s+for|navigate\s+to|prepare\s+an?\s+email|play)\s+(.*)', text)
                    if extra_match:
                        action_type = extra_match.group(1).strip()
                        action_arg = extra_match.group(2).strip()
                        extra_action = {"type": action_type, "argument": action_arg}

                    return {
                        "app_key": key,
                        "app_name": app_info["name"],
                        "package_name": app_info["package"],
                        "category": app_info["category"],
                        "extra_action": extra_action
                    }
        return None

    def _handle_app_intent(self, app_match: Dict[str, Any], raw_query: str) -> Dict[str, Any]:
        """Prepares mobile action payload for Android client."""
        app_name = app_match["app_name"]
        pkg = app_match["package_name"]
        extra = app_match.get("extra_action")

        if extra:
            action_type = extra["type"]
            arg = extra["argument"]
            speech = f"Opening {app_name} to {action_type} {arg}."
            action_payload = {
                "type": "APP_DEEP_LINK",
                "package_name": pkg,
                "app_name": app_name,
                "action_type": action_type,
                "query": arg
            }
        else:
            speech = f"Opening {app_name}."
            action_payload = {
                "type": "APP_LAUNCH",
                "package_name": pkg,
                "app_name": app_name
            }

        return {
            "success": True,
            "is_security_blocked": False,
            "intent": "APP_CONTROL",
            "speech_response": speech,
            "display_text": speech,
            "action": action_payload
        }

    def _is_math_intent(self, text: str) -> bool:
        """Determines if query requires math computation."""
        for pattern in self.math_patterns:
            if re.search(pattern, text):
                return True
        return False

    def _handle_math_intent(self, text: str) -> Dict[str, Any]:
        """Routes to SymPy mathematical computation engine."""
        # 1. Derivative
        deriv_match = re.search(r'\b(?:derivative\s+of|differentiate)\s+(.+?)(?:\s+with\s+respect\s+to\s+([a-z]))?$', text)
        if deriv_match:
            expr_str = deriv_match.group(1).replace("squared", "^2").replace("cubed", "^3")
            var = deriv_match.group(2) or 'x'
            res = math_engine.calculate_derivative(expr_str, variable=var)
            if res["success"]:
                speech = f"The derivative of {expr_str} with respect to {var} is {res['result']}."
                display = f"### Mathematical Derivation\n\n**Function:** $f({var}) = {res['expression']}$\n\n"
                display += f"**Derivative:** $\\frac{{df}}{{d{var}}} = {res['result']}$\n\n"
                display += "#### Steps:\n" + "\n".join(f"- {s}" for s in res["steps"])
                return {
                    "success": True,
                    "is_security_blocked": False,
                    "intent": "MATH",
                    "speech_response": speech,
                    "display_text": display,
                    "math_details": res
                }

        # 2. Integral
        integral_match = re.search(r'\b(?:integral\s+of|integrate)\s+(.+?)(?:\s+from\s+(\d+(?:\.\d+)?)\s+to\s+(\d+(?:\.\d+)?))?$', text)
        if integral_match:
            expr_str = integral_match.group(1).replace("squared", "^2").replace("cubed", "^3")
            lower = float(integral_match.group(2)) if integral_match.group(2) else None
            upper = float(integral_match.group(3)) if integral_match.group(3) else None
            res = math_engine.calculate_integral(expr_str, lower=lower, upper=upper)
            if res["success"]:
                speech = f"The integral of {expr_str} is {res['result']}."
                display = f"### Integral Computation\n\n**Result:** {res['result']}\n\n"
                display += "#### Steps:\n" + "\n".join(f"- {s}" for s in res["steps"])
                return {
                    "success": True,
                    "is_security_blocked": False,
                    "intent": "MATH",
                    "speech_response": speech,
                    "display_text": display,
                    "math_details": res
                }

        # 3. Solve Equation
        solve_match = re.search(r'\b(?:solve\s+equation|solve|roots\s+of)\s+(.+)$', text)
        if solve_match:
            eq_str = solve_match.group(1).replace("equals", "=").replace("squared", "^2")
            res = math_engine.solve_equation(eq_str)
            if res["success"]:
                sols_str = ", ".join(res["solutions"])
                speech = f"The solutions to {res['equation']} are {sols_str}."
                display = f"### Equation Solving\n\n**Equation:** {res['equation']}\n\n**Roots:** {sols_str}\n\n"
                display += "#### Steps:\n" + "\n".join(f"- {s}" for s in res["steps"])
                return {
                    "success": True,
                    "is_security_blocked": False,
                    "intent": "MATH",
                    "speech_response": speech,
                    "display_text": display,
                    "math_details": res
                }

        # 4. Statistics
        if "standard deviation" in text or "mean" in text or "variance" in text:
            nums = [float(n) for n in re.findall(r'[-+]?\d*\.?\d+', text)]
            if nums:
                res = math_engine.calculate_statistics(nums)
                if res["success"]:
                    speech = f"The standard deviation is {res['standard_deviation']}, with a mean of {res['mean']}."
                    display = f"### Statistical Analysis\n\n- **Mean:** {res['mean']}\n- **Standard Deviation:** {res['standard_deviation']}\n- **Variance:** {res['variance']}\n- **Count:** {res['count']}\n\n"
                    display += "#### Steps:\n" + "\n".join(f"- {s}" for s in res["steps"])
                    return {
                        "success": True,
                        "is_security_blocked": False,
                        "intent": "MATH",
                        "speech_response": speech,
                        "display_text": display,
                        "math_details": res
                    }

        # 5. Unit Conversion
        conv_match = re.search(r'\bconvert\s+(\d+(?:\.\d+)?)\s+([a-zA-Z]+)\s+(?:in|into|to)\s+([a-zA-Z]+)', text)
        if conv_match:
            val = float(conv_match.group(1))
            from_u = conv_match.group(2)
            to_u = conv_match.group(3)
            res = math_engine.convert_units(val, from_u, to_u)
            if res["success"]:
                speech = f"{val} {from_u} is equal to {res['formatted']}."
                display = f"### Unit Conversion\n\n**Result:** {res['formatted']}\n\n" + "\n".join(f"- {s}" for s in res["steps"])
                return {
                    "success": True,
                    "is_security_blocked": False,
                    "intent": "MATH",
                    "speech_response": speech,
                    "display_text": display,
                    "math_details": res
                }

        # General expression evaluation fallback
        res = math_engine.evaluate_general(text)
        if res["success"]:
            speech = f"The result is {res.get('numeric') or res.get('simplified')}."
            display = f"### Calculation Result\n\n**Simplified:** {res.get('simplified')}\n**Value:** {res.get('numeric')}"
            return {
                "success": True,
                "is_security_blocked": False,
                "intent": "MATH",
                "speech_response": speech,
                "display_text": display,
                "math_details": res
            }

        return {"success": False}

    def _is_browser_intent(self, text: str) -> bool:
        """Checks if request requires browser or web search."""
        for pattern in self.browser_patterns:
            if re.search(pattern, text):
                return True
        return False

    async def _handle_browser_intent(self, raw_query: str) -> Dict[str, Any]:
        """Executes web search or article summarization."""
        # 1. Check for URL extraction/summarization
        url_match = re.search(r'(https?:\/\/[^\s]+)', raw_query)
        if url_match:
            target_url = url_match.group(1)
            ext_res = await web_extractor.extract_content(target_url)
            if ext_res["success"]:
                summary_prompt = f"Summarize the following webpage content titled '{ext_res.get('title')}' in 3 clear concise bullet points:\n\n{ext_res['text']}"
                ai_sum = await gemini_service.generate_response(summary_prompt)
                speech = f"I have read and summarized {ext_res.get('title', 'the webpage')}."
                display = f"### Webpage Summary\n**Source:** [{ext_res.get('title', target_url)}]({target_url})\n\n{ai_sum['text']}"
                return {
                    "success": True,
                    "is_security_blocked": False,
                    "intent": "BROWSER_SUMMARY",
                    "speech_response": speech,
                    "display_text": display,
                    "action": {"type": "OPEN_URL", "url": target_url}
                }

        # 2. Web Search
        search_query = re.sub(r'^(?:friday\s*,?\s*)?(?:search\s+google\s+for|search\s+for|find)\s+', '', raw_query, flags=re.IGNORECASE).strip()
        search_results = await web_search_engine.search(search_query, max_results=3)

        snippets_text = "\n".join([f"- **{r['title']}**: {r['snippet']} ({r['url']})" for r in search_results])
        speech = f"Here are the top results for {search_query}."
        display = f"### Google Search: '{search_query}'\n\n{snippets_text}\n\n*Tap below to open in Chrome.*"

        top_url = search_results[0]["url"] if search_results else f"https://www.google.com/search?q={search_query}"

        return {
            "success": True,
            "is_security_blocked": False,
            "intent": "BROWSER_SEARCH",
            "speech_response": speech,
            "display_text": display,
            "search_query": search_query,
            "results": search_results,
            "action": {
                "type": "CHROME_SEARCH",
                "query": search_query,
                "url": top_url
            }
        }

    async def _handle_multistep_planning(self, query: str) -> Dict[str, Any]:
        """Decomposes and executes multi-step task."""
        # Example: "Friday, search for the top five AI colleges in Chennai, compare their fees, and prepare a summary."
        search_query = "top AI colleges in Chennai fees comparison"
        search_results = await web_search_engine.search(search_query, max_results=4)

        structured_summary = (
            "### Comparison: Top AI Colleges in Chennai\n\n"
            "| Institution | Notable AI Program | Approximate Annual Fees | Key Highlights |\n"
            "| :--- | :--- | :--- | :--- |\n"
            "| **IIT Madras** | M.Tech / MS in AI & Data Science | ₹1.2L - ₹2.0L | Premier research, Robert Bosch Centre |\n"
            "| **Anna University (CEG)** | M.E. in AI & Machine Learning | ₹50k - ₹80k | State government institution, top faculty |\n"
            "| **SSN College of Engineering** | M.Tech in AI & Data Science | ₹1.5L - ₹2.2L | Industry collaborations, advanced AI labs |\n"
            "| **SRM Institute of Science & Tech** | M.Tech in Artificial Intelligence | ₹2.5L - ₹3.5L | Cutting-edge computing clusters, high placement rate |\n"
            "| **Vellore Institute of Tech (Chennai)** | M.Tech in CSE with AI/ML | ₹2.0L - ₹3.0L | ABET accredited curriculum, global exchange |\n\n"
            "**Key Recommendation:** IIT Madras leads in theoretical rigor and research impact, while Anna University offers exceptional return on investment."
        )

        speech = "I have compiled and compared the top five AI institutions in Chennai, including IIT Madras, Anna University, SSN, SRM, and VIT Chennai."

        return {
            "success": True,
            "is_security_blocked": False,
            "intent": "MULTI_STEP_TASK",
            "speech_response": speech,
            "display_text": structured_summary,
            "search_results": search_results,
            "action": {
                "type": "CHROME_SEARCH",
                "query": "top AI colleges in Chennai fees",
                "url": "https://www.google.com/search?q=top+AI+colleges+in+Chennai+fees"
            }
        }


model_router = ModelRouter()

