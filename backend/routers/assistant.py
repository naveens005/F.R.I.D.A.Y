"""
F.R.I.D.A.Y. Assistant API Router
Handles conversational queries, multi-step tasks, and mobile action generation.
"""

from typing import List, Optional, Dict, Any
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from services.model_router import model_router

router = APIRouter(prefix="/api/v1/assistant", tags=["Assistant"])


class MessageItem(BaseModel):
    role: str = Field(..., description="Role: 'user' or 'assistant'")
    content: str = Field(..., description="Text content")


class AssistantRequest(BaseModel):
    query: str = Field(..., description="User's voice or text command")
    conversation_history: Optional[List[MessageItem]] = Field(default=[], description="Recent conversation turns")
    installed_packages: Optional[List[str]] = Field(default=[], description="List of installed Android package names")


class ActionPayload(BaseModel):
    type: str
    package_name: Optional[str] = None
    app_name: Optional[str] = None
    action_type: Optional[str] = None
    query: Optional[str] = None
    url: Optional[str] = None


class AssistantResponse(BaseModel):
    success: bool
    is_security_blocked: bool
    intent: str
    speech_response: str
    display_text: str
    action: Optional[Dict[str, Any]] = None
    math_details: Optional[Dict[str, Any]] = None
    model_used: Optional[str] = None


@router.post("/chat", response_model=AssistantResponse)
async def process_chat(request: AssistantRequest):
    """Processes natural language voice/text input and generates speech response + mobile actions."""
    try:
        history_dicts = [{"role": m.role, "content": m.content} for m in request.conversation_history]
        result = await model_router.route_and_execute(
            query=request.query,
            conversation_history=history_dicts,
            installed_packages=request.installed_packages
        )
        return AssistantResponse(**result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Assistant processing error: {str(e)}")

