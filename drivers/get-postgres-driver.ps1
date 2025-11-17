param(
    [string]$Version = '42.2.14',
    [string]$OutDir = "$PSScriptRoot",
    [string]$FileName = ''
)

if (-not (Test-Path -Path $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
}

if ([string]::IsNullOrEmpty($FileName)) {
    $FileName = "postgresql-$Version.jar"
}

$url = "https://repo1.maven.org/maven2/org/postgresql/postgresql/$Version/$FileName"
$outPath = Join-Path -Path $OutDir -ChildPath $FileName

Write-Host "Descargando PostgreSQL JDBC driver $Version desde: $url"

try {
    Invoke-WebRequest -Uri $url -OutFile $outPath -UseBasicParsing -Verbose
    Write-Host "Guardado en: $outPath"
} catch {
    Write-Error "Error descargando el driver: $_. Exception.Message"
    Write-Host "Si la descarga falla, descarga manualmente: $url y copia el JAR a: $outPath"
    exit 1
}

exit 0
