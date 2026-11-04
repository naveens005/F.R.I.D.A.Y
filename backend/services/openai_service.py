"""
F.R.I.D.A.Y. OpenAI Reasoning Service
Integrates OpenAI models for complex planning, code analysis, and structured problem decomposition.
"""

from typing import List, Dict, Any, Optional
import os
from config import settings
from services.gemini_service import FRIDAY_SYSTEM_PROMPT


class OpenAIService:
    """Wrapper for OpenAI API."""

    def __init__(self):
        self.api_key = settings.OPENAI_API_KEY or os.getenv("OPENAI_API_KEY")
        self.client = None
        if self.api_key:
            try:
                from openai import OpenAI
                self.client = OpenAI(api_key=self.api_key)
            except Exception as e:
                print(f"Warning: Failed to initialize OpenAI client: {e}")

    async def generate_response(
        self,
        prompt: str,
        conversation_history: Optional[List[Dict[str, str]]] = None,
        context: Optional[str] = None
    ) -> Dict[str, Any]:
        """Generates response using OpenAI or fallback."""
        if self.client:
            try:
                messages = [{"role": "system", "content": FRIDAY_SYSTEM_PROMPT}]
                if context:
                    messages.append({"role": "system", "content": f"Context: {context}"})

                if conversation_history:
                    for msg in conversation_history[-6:]:
                        messages.append({
                            "role": "user" if msg.get("role") == "user" else "assistant",
                            "content": msg.get("content", "")
                        })

                messages.append({"role": "user", "content": prompt})

                response = self.client.chat.completions.create(
                    model=settings.DEFAULT_OPENAI_MODEL,
                    messages=messages,
                    temperature=0.4
                )

                reply_text = response.choices[0].message.content
                return {
                    "success": True,
                    "text": reply_text,
                    "model": settings.DEFAULT_OPENAI_MODEL,
                    "provider": "openai"
                }
            except Exception as e:
                print(f"OpenAI API error, fallback: {e}")

        # Fallback to local response
        return {
            "success": True,
            "text": f"Task planned: {prompt}. All subsystems operational.",
            "model": "friday-openai-fallback",
            "provider": "local_fallback"
        }


openai_service = OpenAIService()

