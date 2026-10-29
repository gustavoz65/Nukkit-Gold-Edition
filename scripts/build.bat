@echo off
setlocal EnableExtensions EnableDelayedExpansion
pushd "%~dp0.." || exit /b 1

where javac >nul 2>&1 || goto :sem_jdk
where jar >nul 2>&1 || goto :sem_jdk
if not exist "src\main\java\" goto :sem_fontes
if not exist "src\main\resources\" goto :sem_fontes
if not exist "lib\*.jar" goto :sem_dependencias

rem Limpa apenas os intermediarios para nao incluir classes de builds anteriores.
if exist "build\classes" rmdir /s /q "build\classes"
if exist "build\stage" rmdir /s /q "build\stage"
if exist "build\classes" goto :falha
if exist "build\stage" goto :falha
mkdir "build\classes" "build\stage" || goto :falha
rem Barras normais e aspas preservam caminhos com espacos no arquivo do javac.
(for /r "src\main\java" %%F in (*.java) do (
    set "fonte=%%~fF"
    echo "!fonte:\=/!"
)) > "build\sources.txt"
for %%F in ("build\sources.txt") do if %%~zF equ 0 goto :sem_fontes
echo Compilando para Java 8...
javac --release 8 -nowarn -encoding UTF-8 -cp "lib/*" -d "build/classes" @build/sources.txt
if errorlevel 1 goto :falha

pushd "build\stage" || goto :falha
for %%J in ("..\..\lib\*.jar") do (
    jar xf "%%J"
    if errorlevel 1 goto :falha_stage
)
rem O jar final recebe seu proprio manifesto; assinaturas antigas nao se aplicam.
del /q "META-INF\MANIFEST.MF" "META-INF\*.SF" "META-INF\*.RSA" "META-INF\*.DSA" 2>nul
xcopy "..\classes\*" ".\" /e /i /y /q >nul
if errorlevel 1 goto :falha_stage
xcopy "..\..\src\main\resources\*" ".\" /e /i /y /q >nul
if errorlevel 1 goto :falha_stage
> "..\manifest.txt" echo Main-Class: cn.nukkit.Nukkit
>> "..\manifest.txt" echo.
jar cfm "..\nukkit-gold.jar" "..\manifest.txt" .
if errorlevel 1 goto :falha_stage
popd
echo Build concluido: build/nukkit-gold.jar
popd
exit /b 0

:sem_jdk
echo Erro: instale um JDK 11 ou superior e configure javac e jar no PATH. 1>&2
goto :falha
:sem_fontes
echo Erro: fontes Java e recursos em src/ sao obrigatorios. 1>&2
goto :falha
:sem_dependencias
echo Erro: nenhuma dependencia encontrada em lib/. 1>&2
goto :falha
:falha_stage
popd
:falha
echo Build interrompido. Consulte o erro acima. 1>&2
popd
exit /b 1
