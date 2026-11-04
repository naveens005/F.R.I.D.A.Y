@echo off
echo ===================================================
echo Running F.R.I.D.A.Y. Backend Verification Tests
echo ===================================================
cd /d %~dp0\backend
set PYTHONPATH=%~dp0\backend
.\venv\Scripts\pytest.exe tests/test_backend.py -v
pause

