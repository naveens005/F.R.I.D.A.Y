@echo off
echo ===================================================
echo Starting F.R.I.D.A.Y. AI Brain Gateway (FastAPI)
echo ===================================================
cd /d %~dp0\backend
set PYTHONPATH=%~dp0\backend
.\venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
pause

