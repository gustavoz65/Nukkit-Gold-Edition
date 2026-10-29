$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
try {
    New-Item -ItemType Directory -Force -Path 'build/test-plugin' | Out-Null
    $fontes = @(Get-ChildItem -LiteralPath 'src/test/plugin' -Filter '*.java' -Recurse)
    $fontes | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' } | Set-Content -LiteralPath 'build/plugin-test-sources.txt' -Encoding ascii
    & javac --release 8 -encoding UTF-8 -cp 'build/nukkit-gold.jar' -d 'build/test-plugin' '@build/plugin-test-sources.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar o plugin de testes.' }
    Copy-Item -LiteralPath 'src/test/plugin/resources/plugin.yml' -Destination 'build/test-plugin/plugin.yml' -Force
    & jar cf build/gold-core-regression.jar -C build/test-plugin .
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao empacotar o plugin de testes.' }
} finally { Pop-Location }
