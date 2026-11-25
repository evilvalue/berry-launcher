# Собирает BerryUp.jar и обновляет BerryUp.jar + launcher/latest-java.json на сайте.
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
Copy-Item (Join-Path $root "build\libs\BerryUp.jar") $jar -Force

$jarHash = (Get-FileHash $jar -Algorithm SHA256).Hash.ToLowerInvariant()
$jarSize = (Get-Item $jar).Length

$update = [ordered]@{
    version    = $version
    jar        = "https://evilvalue.github.io/berry/launcher/BerryUp.jar"
    sha256     = $jarHash
    size       = $jarSize
    notes      = $Notes
}
$json = $update | ConvertTo-Json
Set-Content -Path (Join-Path $dist "latest-java.json") -Value $json -Encoding UTF8

Write-Host "[+] BerryUp.jar: $jarSize bytes, $($jarHash.Substring(0,16))"

if (-not $NoCopy) {
    New-Item -ItemType Directory -Force -Path $site | Out-Null
    Copy-Item $jar (Join-Path $site "BerryUp.jar") -Force
    Copy-Item (Join-Path $dist "latest-java.json") (Join-Path $site "latest-java.json") -Force
    foreach ($stale in @("BerryUp.zip", "BerryUp.cmd")) {
        $path = Join-Path $site $stale
        if (Test-Path $path) { Remove-Item $path -Force; Write-Host "[-] Убран старый $stale" }
    }
    Write-Host "[+] Сайт: $site"
}
Write-Host "[i] Дальше: git add -A; git commit; git push, затем gh release create v$version BerryUp.jar"
