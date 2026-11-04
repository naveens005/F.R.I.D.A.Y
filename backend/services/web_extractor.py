"""
F.R.I.D.A.Y. Webpage Extractor
Fetches and extracts clean, readable text from web pages for summarization and analysis.
"""

from typing import Dict, Any
import httpx
from bs4 import BeautifulSoup
import re


class WebExtractor:
    """Extracts article content and metadata from web URLs."""

    def __init__(self):
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        }

    async def extract_content(self, url: str) -> Dict[str, Any]:
        """Fetches URL and returns clean parsed article content."""
        try:
            if not url.startswith("http://") and not url.startswith("https://"):
                url = f"https://{url}"

            async with httpx.AsyncClient(headers=self.headers, timeout=12.0, follow_redirects=True) as client:
                response = await client.get(url)

            if response.status_code != 200:
                return {
                    "success": False,
                    "url": url,
                    "error": f"Failed to fetch webpage (HTTP status {response.status_code})"
                }

            soup = BeautifulSoup(response.text, "html.parser")

            # Remove unwanted elements
            for tag in soup(["script", "style", "nav", "footer", "header", "noscript", "aside", "svg"]):
                tag.decompose()

            # Extract title
            title = ""
            if soup.title and soup.title.string:
                title = soup.title.string.strip()
            elif soup.find("h1"):
                title = soup.find("h1").text.strip()

            # Find main article content if possible
            main_element = soup.find("article") or soup.find("main") or soup.find("body")
            if not main_element:
                return {"success": False, "url": url, "error": "No content body detected"}

            # Collect text
            paragraphs = [p.get_text().strip() for p in main_element.find_all(["p", "h2", "h3", "li"])]
            text_content = "\n\n".join([p for p in paragraphs if len(p) > 20])

            # Limit length to prevent context explosion
            if len(text_content) > 8000:
                text_content = text_content[:8000] + "... [Content truncated for analysis]"

            return {
                "success": True,
                "url": url,
                "title": title,
                "text": text_content,
                "word_count": len(text_content.split())
            }

        except Exception as e:
            return {
                "success": False,
                "url": url,
                "error": f"Extraction error: {str(e)}"
            }


web_extractor = WebExtractor()

