# Собирает BerryUp.jar, пакует BerryUp.zip для игроков и обновляет
# launcher/latest-java.json на сайте.
#   powershell -ExecutionPolicy Bypass -File build\publish-java.ps1 -Notes "что нового"
param(
    [string]$Notes = "",
    [switch]$NoCopy,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $root "build\dist"
$site = Join-Path (Split-Path -Parent $root) "packwiz\launcher"

$cfg = Get-Content (Join-Path $root "src\main\java\ru\berrytrip\berryup\Cfg.java") -Raw
$version = [regex]::Match($cfg, 'VERSION\s*=\s*"([0-9.]+)"').Groups[1].Value
if (-not $version) { throw "no VERSION in Cfg.java" }
Write-Host "[*] Version: $version"

if (-not $SkipBuild) {
    $gradle = Join-Path $root ".tools\gradle-8.10.2\bin\gradle.bat"
    if (-not (Test-Path $gradle)) { $gradle = Join-Path $root "gradlew.bat" }
    & $gradle fatJar --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "gradle build failed ($LASTEXITCODE)" }
}

New-Item -ItemType Directory -Force -Path $dist | Out-Null
$jar = Join-Path $dist "BerryUp.jar"
$cmd = Join-Path $dist "BerryUp.cmd"
$readme = Join-Path $dist "README.txt"
$zip = Join-Path $dist "BerryUp.zip"

Copy-Item (Join-Path $root "build\libs\BerryUp.jar") $jar -Force
Copy-Item (Join-Path $root "packaging\BerryUp.cmd") $cmd -Force
Copy-Item (Join-Path $root "packaging\README.txt") $readme -Force

if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path $jar, $cmd, $readme -DestinationPath $zip -CompressionLevel Optimal

$jarHash = (Get-FileHash $jar -Algorithm SHA256).Hash.ToLowerInvariant()
$jarSize = (Get-Item $jar).Length
$zipHash = (Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant()
$zipSize = (Get-Item $zip).Length

$update = [ordered]@{
    version   = $version
    jar       = "https://evilvalue.github.io/berry/launcher/BerryUp.jar"
    sha256    = $jarHash
    size      = $jarSize
    zip       = "https://evilvalue.github.io/berry/launcher/BerryUp.zip"
    zip_sha256 = $zipHash
    zip_size  = $zipSize
    notes     = $Notes
}
$json = $update | ConvertTo-Json
Set-Content -Path (Join-Path $dist "latest-java.json") -Value $json -Encoding UTF8

Write-Host "[+] BerryUp.jar: $jarSize bytes, $($jarHash.Substring(0,16))"
Write-Host "[+] BerryUp.zip: $zipSize bytes, $($zipHash.Substring(0,16))"

if (-not $NoCopy) {
    New-Item -ItemType Directory -Force -Path $site | Out-Null
    Copy-Item $jar (Join-Path $site "BerryUp.jar") -Force
    Copy-Item $zip (Join-Path $site "BerryUp.zip") -Force
    Copy-Item $cmd (Join-Path $site "BerryUp.cmd") -Force
    Copy-Item (Join-Path $dist "latest-java.json") (Join-Path $site "latest-java.json") -Force
    Write-Host "[+] Сайт: $site"
}
Write-Host "[i] Дальше: git add -A; git commit; git push, затем gh release create v$version ..."
