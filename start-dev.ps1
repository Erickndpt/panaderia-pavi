$ErrorActionPreference = 'Stop'

Set-Location -LiteralPath $PSScriptRoot

docker info | Out-Null
docker compose up -d
docker compose ps

Write-Host ''
Write-Host 'Frontend:  http://localhost:4200' -ForegroundColor Green
Write-Host 'Backend:   http://localhost:8081' -ForegroundColor Green
Write-Host 'Swagger:   http://localhost:8081/swagger-ui.html' -ForegroundColor Green
Write-Host 'Keycloak:  http://localhost:38080' -ForegroundColor Green
