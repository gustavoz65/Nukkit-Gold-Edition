$ErrorActionPreference = 'Stop'
$javaHomeAnterior = $env:JAVA_HOME
$raiz = Split-Path -Parent $PSScriptRoot
try {
    # Usa o mesmo JDK do javac do build do core, sem alterar o Windows permanentemente.
    $detalhes = (& javac '-J-XshowSettings:properties' -version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel localizar o JDK do javac.' }
    $caminhoJdk = [regex]::Match($detalhes, '(?m)^\s*java\.home\s*=\s*(.+?)\r?$')
    if (!$caminhoJdk.Success) { throw 'A propriedade java.home nao foi encontrada.' }
    $env:JAVA_HOME = $caminhoJdk.Groups[1].Value.Trim()
    Push-Location -LiteralPath (Join-Path $raiz 'goldsuite')
    try {
        & .\gradlew.bat --offline --no-daemon check jar
        if ($LASTEXITCODE -ne 0) { throw 'Build ou testes da GoldSuite falharam. Confira se o javac usa um JDK compativel com Gradle 8.14.3.' }
    } finally { Pop-Location }
} finally { $env:JAVA_HOME = $javaHomeAnterior }
