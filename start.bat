@echo off
setlocal EnableExtensions DisableDelayedExpansion
chcp 65001 >nul
rem Os dados ficam ao lado do iniciador, mesmo quando chamado de outra pasta.
pushd "%~dp0" || exit /b 1
set "arquivo_jar=%~1"
if defined arquivo_jar goto :verificar
set "arquivo_jar=build\nukkit-gold.jar"
if exist "%arquivo_jar%" goto :verificar
set "arquivo_jar=nukkit-gold.jar"
if exist "%arquivo_jar%" goto :verificar
set "arquivo_jar=builds\nukkit-gold-0.15.10.jar"

:verificar
if not exist "%arquivo_jar%" (
    echo Erro: jar nao encontrado. Baixe o jar ou execute scripts\build.bat. 1>&2
    popd
    exit /b 1
)
rem O instalador traz um Java 25 em runtime\; sem ele, usa o Java do PATH.
set "java_exe=java"
set "java_opcoes="
if not exist "runtime\bin\java.exe" goto :java_do_sistema
set "java_exe=runtime\bin\java.exe"
rem Netty e SQLite usam Unsafe e codigo nativo; sem isso o Java 25 imprime avisos.
set "java_opcoes=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow"
goto :executar

:java_do_sistema
where java >nul 2>&1
if errorlevel 1 (
    echo Erro: instale Java 8 ou superior e configure o PATH. 1>&2
    popd
    exit /b 1
)

:executar
rem JLine 2.14.6 exige o nome da classe; o valor literal unsupported gera erro.
"%java_exe%" -Xmx1G %java_opcoes% -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Djline.terminal=jline.UnsupportedTerminal -jar "%arquivo_jar%" disable-ansi
set "resultado=%errorlevel%"
popd
exit /b %resultado%
