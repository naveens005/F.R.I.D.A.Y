Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Starting F.R.I.D.A.Y. AI Brain Gateway (FastAPI)" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

$env:PYTHONPATH = "$PSScriptRoot\backend"
Set-Location "$PSScriptRoot\backend"
& ".\venv\Scripts\python.exe" -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload

