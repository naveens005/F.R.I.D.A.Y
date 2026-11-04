"""
F.R.I.D.A.Y. Advanced Personal AI Mobile Assistant - Backend Gateway
FastAPI main application orchestrating AI Reasoning, Deterministic Math, Web Browsing & Security.
"""

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from config import settings
from routers import assistant, browser, math_solver
import json
import asyncio

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="Backend AI brain for F.R.I.D.A.Y. Personal AI Mobile Assistant"
)

# CORS Middleware (Allows Android emulator 10.0.2.2 and local network devices)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount Routers
app.include_router(assistant.router)
app.include_router(browser.router)
app.include_router(math_solver.router)


@app.get("/")
def read_root():
    return {
        "assistant": "F.R.I.D.A.Y.",
        "status": "ONLINE",
        "version": settings.APP_VERSION,
        "security_policy": "PERMANENT_PAYMENT_EXCLUSION_ACTIVE",
        "features": [
            "Natural Voice STT / TTS",
            "App Control Engine",
            "Deterministic SymPy Mathematics",
            "Live Web Search & Article Extraction",
            "Security Blocklist Interception",
            "Multi-Step Task Planning"
        ]
    }


@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "engines": {
            "math": "SymPy 1.14 (Active)",
            "browser": "DuckDuckGo + BeautifulSoup4 (Active)",
            "gemini": "Active" if settings.GEMINI_API_KEY else "Fallback Reasoner Active",
            "openai": "Active" if settings.OPENAI_API_KEY else "Fallback Reasoner Active",
            "security_blocklist": "ENFORCED"
        }
    }


@app.websocket("/ws/voice-stream")
async def websocket_voice_endpoint(websocket: WebSocket):
    """Real-time bi-directional WebSocket for voice visualizer and streaming responses."""
    await websocket.accept()
    try:
        while True:
            data = await websocket.receive_text()
            payload = json.loads(data)
            action = payload.get("action")

            if action == "ping":
                await websocket.send_text(json.dumps({"type": "pong", "status": "connected"}))
            elif action == "voice_command":
                query = payload.get("text", "")
                from services.model_router import model_router
                result = await model_router.route_and_execute(query)
                await websocket.send_text(json.dumps({
                    "type": "command_result",
                    "data": result
                }))
    except WebSocketDisconnect:
        pass
    except Exception as e:
        await websocket.close()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host=settings.HOST, port=settings.PORT, reload=settings.DEBUG)

