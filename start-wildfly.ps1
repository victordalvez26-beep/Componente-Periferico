# Load environment variables from .env file and start WildFly
# Usage: .\start-wildfly.ps1

Write-Host "Loading environment variables from .env file..." -ForegroundColor Green

# Check if .env file exists
if (-not (Test-Path ".env")) {
    Write-Host "Error: .env file not found!" -ForegroundColor Red
    Write-Host "Please copy .env.example to .env and configure your credentials."
    exit 1
}

# Load environment variables from .env file
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        Set-Item -Path "env:$name" -Value $value
        Write-Host "Set $name=$value" -ForegroundColor Cyan
    }
}

Write-Host ""
Write-Host "Starting WildFly with environment variables..." -ForegroundColor Green
Write-Host "MONGODB_URI=$env:MONGODB_URI" -ForegroundColor Yellow
Write-Host "MONGODB_DB=$env:MONGODB_DB" -ForegroundColor Yellow
Write-Host ""

# Check if WILDFLY_HOME is set
if (-not $env:WILDFLY_HOME) {
    Write-Host "Error: WILDFLY_HOME environment variable is not set!" -ForegroundColor Red
    Write-Host "Please set it to your WildFly installation directory."
    exit 1
}

# Start WildFly
& "$env:WILDFLY_HOME\bin\standalone.bat"
