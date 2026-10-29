param([switch]$Compilar)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $raiz
try {
    if ($Compilar) {
        & cmd.exe /d /c scripts\build.bat
        if ($LASTEXITCODE -ne 0) { throw 'Falha no build.' }
    }
    if (!(Test-Path -LiteralPath 'build/nukkit-gold.jar')) { throw 'Compile primeiro com scripts/build.bat.' }
    New-Item -ItemType Directory -Force -Path 'build/test-classes' | Out-Null
    $fontes = @(Get-ChildItem -LiteralPath 'src/test/java/br/gold' -Filter '*.java' -Recurse)
    $fontes | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' } | Set-Content -LiteralPath 'build/test-sources.txt' -Encoding ascii
    & javac --release 8 -encoding UTF-8 -cp 'build/nukkit-gold.jar' -d 'build/test-classes' '@build/test-sources.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar os testes.' }
    foreach ($fonte in ($fontes | Where-Object { $_.Name.EndsWith('Test.java') } | Sort-Object Name)) {
        & java -ea -Xmx256M -cp 'build/nukkit-gold.jar;build/test-classes' ('br.gold.' + $fonte.BaseName)
        if ($LASTEXITCODE -ne 0) { throw "Teste falhou: $($fonte.BaseName)" }
    }
} finally { Pop-Location }
