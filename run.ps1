$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$out = Join-Path $root "out"
New-Item -ItemType Directory -Force -Path $out | Out-Null

$sources = Get-ChildItem -Path (Join-Path $root "src") -Filter *.java | ForEach-Object { $_.FullName }
if (-not $sources) {
    Write-Host "No Java sources found in src\."
    exit 1
}

& javac -encoding UTF-8 -d $out $sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& java -cp $out Main
exit $LASTEXITCODE
