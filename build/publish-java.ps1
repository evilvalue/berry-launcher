# Собирает BerryUp.jar, BerryUp.zip и обновляет их вместе с launcher/latest-java.json на сайте.
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

# Архив для игроков: jar плюс запускатор, который сам находит Java. Так .jar
# не нужно связывать с Java — на чистой Windows двойной щелчок по нему молчит.
$zip = Join-Path $dist "BerryUp.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::Open($zip, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
        $archive, $jar, "BerryUp.jar") | Out-Null
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
        $archive, (Join-Path $root "BerryUp.bat"), "START-BerryUp.bat") | Out-Null
} finally {
    $archive.Dispose()
}
Write-Host "[+] BerryUp.zip: $((Get-Item $zip).Length) bytes"

if (-not $NoCopy) {
    New-Item -ItemType Directory -Force -Path $site | Out-Null
    Copy-Item $jar (Join-Path $site "BerryUp.jar") -Force
    Copy-Item (Join-Path $dist "latest-java.json") (Join-Path $site "latest-java.json") -Force
    Copy-Item $zip (Join-Path $site "BerryUp.zip") -Force
    Copy-Item (Join-Path $root "BerryUp.bat") (Join-Path $site "BerryUp.bat") -Force
    $stale = Join-Path $site "BerryUp.cmd"
    if (Test-Path $stale) { Remove-Item $stale -Force; Write-Host "[-] Убран старый BerryUp.cmd" }
    Write-Host "[+] Сайт: $site"
}
Write-Host "[i] Дальше: git add -A; git commit; git push, затем gh release upload v$version BerryUp.jar BerryUp.zip"
