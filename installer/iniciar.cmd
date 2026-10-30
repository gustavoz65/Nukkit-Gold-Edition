@echo off
rem Usado pelos atalhos do instalador: mantem a janela aberta depois que o servidor para.
rem Se um plugin deixar reiniciar.servidor (ex.: /restart), o servidor abre de novo sozinho.
title Nukkit Gold
:inicio
echo Para desligar o servidor sem perder dados, digite stop e aperte Enter.
echo.
call "%~dp0start.bat"
if exist "%~dp0reiniciar.servidor" (
    del "%~dp0reiniciar.servidor"
    echo.
    echo Reiniciando em 3 segundos...
    timeout /t 3 /nobreak >nul
    goto inicio
)
echo.
echo O servidor foi encerrado.
pause
