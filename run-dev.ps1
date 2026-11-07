Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Test-CommandExists {
  param(
    [Parameter(Mandatory = $true)]
    [string] $Command
  )
  return [bool](Get-Command $Command -ErrorAction SilentlyContinue)
}

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $repoRoot "Backend"
$frontendDir = Join-Path $repoRoot "frontend"

if (-not (Test-Path $backendDir)) { throw "Backend directory not found: $backendDir" }
if (-not (Test-Path $frontendDir)) { throw "frontend directory not found: $frontendDir" }

if (-not (Test-CommandExists "java")) { throw "Java not found. Install Java 21 and ensure 'java' is on PATH." }
if (-not (Test-CommandExists "node")) { throw "Node.js not found. Install Node.js (18+) and ensure 'node' is on PATH." }
if (-not (Test-CommandExists "npm")) { throw "npm not found. Install Node.js (includes npm) and ensure 'npm' is on PATH." }

# Ensure frontend API base URL is configured (Create React App uses REACT_APP_*).
$frontendEnvPath = Join-Path $frontendDir ".env"
if (-not (Test-Path $frontendEnvPath)) {
  "REACT_APP_API_BASE_URL=http://localhost:5050/api" | Out-File -FilePath $frontendEnvPath -Encoding utf8
  Write-Host "Created $frontendEnvPath with REACT_APP_API_BASE_URL=http://localhost:5050/api"
} else {
  Write-Host "Found existing $frontendEnvPath (leaving as-is)"
}

Write-Host ""
Write-Host "Starting backend (Spring Boot) on http://localhost:5050 ..."
Write-Host "Starting frontend (React) on http://localhost:3000 ..."
Write-Host ""

$backendCmd = @"
Set-StrictMode -Version Latest
`$ErrorActionPreference = 'Stop'
cd '$backendDir'
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd spring-boot:run
} else {
  mvn spring-boot:run
}
"@

$frontendCmd = @"
Set-StrictMode -Version Latest
`$ErrorActionPreference = 'Stop'
cd '$frontendDir'
npm install
npm start
"@

Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-Command", $backendCmd | Out-Null
Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-Command", $frontendCmd | Out-Null

Write-Host "Done. Two windows opened (backend + frontend)."
