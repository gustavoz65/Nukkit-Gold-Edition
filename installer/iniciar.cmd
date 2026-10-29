@echo off
rem Usado pelos atalhos do instalador: mantem a janela aberta depois que o servidor para.
title Nukkit Gold
echo Para desligar o servidor sem perder dados, digite stop e aperte Enter.
echo.
call "%~dp0start.bat"
echo.
echo O servidor foi encerrado.
pause
