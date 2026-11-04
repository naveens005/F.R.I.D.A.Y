"""
F.R.I.D.A.Y. Browser Automation & Web Intelligence Router
Provides search, URL extraction, and webpage summarization endpoints.
"""

from typing import Optional, List, Dict, Any
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from services.web_search import web_search_engine
from services.web_extractor import web_extractor
from services.gemini_service import gemini_service

router = APIRouter(prefix="/api/v1/browser", tags=["Browser"])


class SearchRequest(BaseModel):
    query: str = Field(..., description="Search query string")
    max_results: int = Field(default=5, ge=1, le=10)


class SearchResultItem(BaseModel):
    title: str
    url: str
    snippet: str


class SearchResponse(BaseModel):
    success: bool
    query: str
    results: List[SearchResultItem]


class SummarizeRequest(BaseModel):
    url: str = Field(..., description="Target webpage URL to summarize")


class SummarizeResponse(BaseModel):
    success: bool
    url: str
    title: Optional[str] = None
    summary: str
    word_count: Optional[int] = None


@router.post("/search", response_model=SearchResponse)
async def perform_search(request: SearchRequest):
    """Searches the web and returns ranked organic results."""
    try:
        results = await web_search_engine.search(request.query, max_results=request.max_results)
        return SearchResponse(
            success=True,
            query=request.query,
            results=[SearchResultItem(**r) for r in results]
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Web search error: {str(e)}")


@router.post("/summarize", response_model=SummarizeResponse)
async def summarize_page(request: SummarizeRequest):
    """Fetches article content from URL and produces a structured AI executive summary."""
    try:
        ext = await web_extractor.extract_content(request.url)
        if not ext["success"]:
            raise HTTPException(status_code=400, detail=ext.get("error", "Failed to extract content"))

        prompt = f"Provide an executive summary of this article titled '{ext.get('title')}' in 4 clear, informative bullet points:\n\n{ext['text']}"
        ai_res = await gemini_service.generate_response(prompt)

        return SummarizeResponse(
            success=True,
            url=request.url,
            title=ext.get("title"),
            summary=ai_res.get("text", "No summary generated"),
            word_count=ext.get("word_count")
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Summarization failed: {str(e)}")

