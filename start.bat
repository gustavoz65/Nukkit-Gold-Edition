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
where java >nul 2>&1
if errorlevel 1 (
    echo Erro: instale Java 8 ou superior e configure o PATH. 1>&2
    popd
    exit /b 1
)
rem JLine 2.14.6 exige o nome da classe; o valor literal unsupported gera erro.
java -Xmx1G -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Djline.terminal=jline.UnsupportedTerminal -jar "%arquivo_jar%" disable-ansi
set "resultado=%errorlevel%"
popd
exit /b %resultado%
