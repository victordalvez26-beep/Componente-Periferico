$cwd = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $cwd
Write-Host "Posting payload.json via curl.exe..."
& curl.exe -v -X POST -H 'Content-Type: application/json' --data-binary @payload.json http://127.0.0.1:8080/nodo-periferico/api/rabbit
