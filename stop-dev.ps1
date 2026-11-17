$ErrorActionPreference = 'Stop'

Set-Location -LiteralPath $PSScriptRoot

docker compose stop
docker compose ps

Write-Host 'Servicios detenidos. Los volumenes y datos se conservaron.' -ForegroundColor Yellow
