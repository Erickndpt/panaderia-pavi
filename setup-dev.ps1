$ErrorActionPreference = 'Stop'

Set-Location -LiteralPath $PSScriptRoot

Write-Host 'Verificando Docker...' -ForegroundColor Cyan
docker info | Out-Null
docker compose version | Out-Null

Write-Host 'Construyendo y levantando Panaderia Pavi...' -ForegroundColor Cyan
docker compose up -d --build

Write-Host ''
docker compose ps
Write-Host ''
Write-Host 'La primera inicializacion de Oracle puede tardar varios minutos.' -ForegroundColor Yellow
Write-Host 'Consulte el estado con: docker compose ps'
Write-Host 'Consulte los logs con: docker compose logs -f'
