; Instalador do Nukkit Gold para Windows.
; Gere com: bash scripts/build-installer.sh (prepara ORIGEM e chama o makensis).

Unicode true
!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "FileFunc.nsh"

!ifndef ORIGEM
  !error "Use scripts/build-installer.sh; ele define ORIGEM, SAIDA e VERSAO."
!endif

!define NOME "Nukkit Gold"
!define CHAVE "Software\NukkitGold"
!define CHAVE_REMOVER "Software\Microsoft\Windows\CurrentVersion\Uninstall\NukkitGold"

Name "${NOME} ${VERSAO}"
OutFile "${SAIDA}"
; Pasta do usuario: o servidor grava mundos e configuracoes ao lado do jar,
; o que nao funciona em Arquivos de Programas sem administrador.
InstallDir "$PROFILE\NukkitGold"
InstallDirRegKey HKCU "${CHAVE}" "Pasta"
RequestExecutionLevel user
SetCompressor /SOLID lzma
ManifestDPIAware true
BrandingText "${NOME} ${VERSAO}"

VIProductVersion "${VERSAO}.0"
VIAddVersionKey /LANG=1046 "ProductName" "${NOME}"
VIAddVersionKey /LANG=1046 "ProductVersion" "${VERSAO}"
VIAddVersionKey /LANG=1046 "FileVersion" "${VERSAO}"
VIAddVersionKey /LANG=1046 "FileDescription" "Instalador do ${NOME}"
VIAddVersionKey /LANG=1046 "CompanyName" "gustavoz65"
VIAddVersionKey /LANG=1046 "LegalCopyright" "GPLv3"

!define MUI_ICON "${ORIGEM}\nukkit-gold.ico"
!define MUI_UNICON "${ORIGEM}\nukkit-gold.ico"
!define MUI_ABORTWARNING
!define MUI_COMPONENTSPAGE_SMALLDESC

!define MUI_WELCOMEPAGE_TITLE "Instalar o ${NOME} ${VERSAO}"
!define MUI_WELCOMEPAGE_TEXT "Servidor de MCPE 0.15.10 (protocolo 84).$\r$\n$\r$\nO Java já vem junto; não é preciso instalar mais nada. Os mundos e configurações ficam na pasta escolhida e são preservados em atualizações.$\r$\n$\r$\nClique em Avançar para continuar."
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT "Iniciar o servidor agora"
!define MUI_FINISHPAGE_RUN_FUNCTION IniciarServidor
!define MUI_FINISHPAGE_SHOWREADME
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Abrir a pasta do servidor"
!define MUI_FINISHPAGE_SHOWREADME_FUNCTION AbrirPasta
!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!define MUI_FINISHPAGE_TEXT "O ${NOME} foi instalado em $INSTDIR.$\r$\n$\r$\nNa primeira vez, o Windows pode pedir para liberar o Java no firewall; permita em redes privadas para jogar na rede local."

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${ORIGEM}\LICENSE"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "PortugueseBR"

; Um programa em execucao nao pode ser aberto para escrita no Windows, entao
; abrir java.exe em modo "a" (sem escrever) diz se o servidor esta rodando.
; Se estiver, o instalador pede a parada criando parar.servidor: o core le esse
; arquivo a cada segundo e desliga salvando mundos e jogadores, como o stop.
; Versoes antigas nao conhecem o arquivo; depois de 30 s o processo e encerrado
; a forca (perde so o que mudou desde o ultimo autosave).
!macro EmUso UNICO DESTINO_LIVRE
  IfFileExists "$INSTDIR\runtime\bin\java.exe" 0 ${DESTINO_LIVRE}
  ClearErrors
  FileOpen $0 "$INSTDIR\runtime\bin\java.exe" a
  IfErrors +3
  FileClose $0
  Goto ${DESTINO_LIVRE}
!macroend

!macro FecharServidor UNICO
  StrCpy $1 0
  checar_${UNICO}:
  !insertmacro EmUso ${UNICO} livre_${UNICO}
  ${If} $1 == 0
    DetailPrint "O servidor esta aberto; pedindo para ele salvar e desligar..."
    FileOpen $2 "$INSTDIR\parar.servidor" w
    FileClose $2
  ${EndIf}
  IntOp $1 $1 + 1
  ${If} $1 <= 60
    Sleep 500
    Goto checar_${UNICO}
  ${EndIf}
  ${If} $1 == 61
    DetailPrint "O servidor nao respondeu em 30 s; encerrando o processo."
    ; O caminho vai por variavel de ambiente para nao quebrar aspas no comando.
    System::Call 'kernel32::SetEnvironmentVariable(t "NUKKITGOLD_JAVA", t "$INSTDIR\runtime\")'
    nsExec::Exec `powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Get-Process java,javaw -ErrorAction SilentlyContinue | Where-Object { $$_.Path -and $$_.Path.StartsWith($$env:NUKKITGOLD_JAVA, [StringComparison]::OrdinalIgnoreCase) } | Stop-Process -Force"`
    Pop $2
    Sleep 1000
    Goto checar_${UNICO}
  ${EndIf}
  Delete "$INSTDIR\parar.servidor"
  Abort "Nao foi possivel fechar o servidor. Feche a janela do Nukkit Gold e rode o instalador de novo."
  livre_${UNICO}:
  Delete "$INSTDIR\parar.servidor"
  ${If} $1 > 0
    DetailPrint "Servidor fechado."
    ; O Windows libera os arquivos alguns instantes depois do fim do processo.
    Sleep 1000
  ${EndIf}
!macroend

Section "Servidor" SecServidor
  SectionIn RO
  !insertmacro FecharServidor instalar
  SetOutPath "$INSTDIR"
  File "${ORIGEM}\nukkit-gold.jar"
  File "${ORIGEM}\start.bat"
  File "${ORIGEM}\iniciar.cmd"
  File "${ORIGEM}\nukkit-gold.ico"
  File "${ORIGEM}\LICENSE"
  ; nukkit.yml ja em portugues evita a pergunta de idioma na primeira execucao.
  ; Nao sobrescreve: numa atualizacao, a configuracao do usuario fica.
  SetOverwrite off
  File "${ORIGEM}\nukkit.yml"
  SetOverwrite on
  CreateDirectory "$INSTDIR\plugins"

  WriteUninstaller "$INSTDIR\desinstalar.exe"
  WriteRegStr HKCU "${CHAVE}" "Pasta" "$INSTDIR"
  WriteRegStr HKCU "${CHAVE_REMOVER}" "DisplayName" "${NOME}"
  WriteRegStr HKCU "${CHAVE_REMOVER}" "DisplayVersion" "${VERSAO}"
  WriteRegStr HKCU "${CHAVE_REMOVER}" "Publisher" "gustavoz65"
  WriteRegStr HKCU "${CHAVE_REMOVER}" "DisplayIcon" "$INSTDIR\nukkit-gold.ico"
  WriteRegStr HKCU "${CHAVE_REMOVER}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKCU "${CHAVE_REMOVER}" "UninstallString" '"$INSTDIR\desinstalar.exe"'
  WriteRegDWORD HKCU "${CHAVE_REMOVER}" "NoModify" 1
  WriteRegDWORD HKCU "${CHAVE_REMOVER}" "NoRepair" 1

  CreateDirectory "$SMPROGRAMS\${NOME}"
  CreateShortCut "$SMPROGRAMS\${NOME}\${NOME}.lnk" "$INSTDIR\iniciar.cmd" "" "$INSTDIR\nukkit-gold.ico"
  CreateShortCut "$SMPROGRAMS\${NOME}\Pasta do servidor.lnk" "$INSTDIR"
  CreateShortCut "$SMPROGRAMS\${NOME}\Desinstalar.lnk" "$INSTDIR\desinstalar.exe"
SectionEnd

Section "Java 25 embutido" SecJava
  ; Remove a versao anterior inteira para nao misturar arquivos de dois Javas.
  RMDir /r "$INSTDIR\runtime"
  SetOutPath "$INSTDIR\runtime"
  File /r "${ORIGEM}\runtime\*.*"
SectionEnd

Section "Plugins GoldSuite" SecPlugins
  SetOutPath "$INSTDIR\plugins"
  File "${ORIGEM}\plugins\GoldCore-${VERSAO}.jar"
  File "${ORIGEM}\plugins\GoldAuth-${VERSAO}.jar"
  File "${ORIGEM}\plugins\GoldPerms-${VERSAO}.jar"
  File "${ORIGEM}\plugins\GoldEconomy-${VERSAO}.jar"
SectionEnd

Section "Atalho na área de trabalho" SecAtalho
  SetOutPath "$INSTDIR"
  CreateShortCut "$DESKTOP\${NOME}.lnk" "$INSTDIR\iniciar.cmd" "" "$INSTDIR\nukkit-gold.ico"
SectionEnd

Section "-Tamanho"
  ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
  WriteRegDWORD HKCU "${CHAVE_REMOVER}" "EstimatedSize" $0
SectionEnd

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecServidor} "O core Nukkit Gold e o iniciador."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecJava} "Java 25 só para o servidor. Desmarque apenas se já tiver Java 8 ou superior no PATH."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecPlugins} "GoldCore (SQLite e assinatura de itens), GoldAuth (registro e login), GoldPerms (grupos e permissões) e GoldEconomy (money, cash e /pagar)."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecAtalho} "Cria o atalho Nukkit Gold na área de trabalho."
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Function IniciarServidor
  SetOutPath "$INSTDIR"
  ExecShell "open" "$INSTDIR\iniciar.cmd"
FunctionEnd

Function AbrirPasta
  ExecShell "open" "$INSTDIR"
FunctionEnd

Section "Uninstall"
  !insertmacro FecharServidor remover
  Delete "$DESKTOP\${NOME}.lnk"
  RMDir /r "$SMPROGRAMS\${NOME}"

  Delete "$INSTDIR\nukkit-gold.jar"
  Delete "$INSTDIR\start.bat"
  Delete "$INSTDIR\iniciar.cmd"
  Delete "$INSTDIR\nukkit-gold.ico"
  Delete "$INSTDIR\LICENSE"
  RMDir /r "$INSTDIR\runtime"
  Delete "$INSTDIR\plugins\GoldCore-${VERSAO}.jar"
  Delete "$INSTDIR\plugins\GoldAuth-${VERSAO}.jar"
  Delete "$INSTDIR\plugins\GoldPerms-${VERSAO}.jar"
  Delete "$INSTDIR\plugins\GoldEconomy-${VERSAO}.jar"

  ; So apaga dados que o proprio servidor cria, por nome. Nunca a pasta inteira,
  ; caso o usuario tenha instalado numa pasta com outros arquivos.
  MessageBox MB_YESNO|MB_ICONQUESTION|MB_DEFBUTTON2 "Apagar também mundos, jogadores, plugins e configurações?$\r$\n$\r$\nEscolha Não para manter esses dados em $INSTDIR." /SD IDNO IDNO manter
    RMDir /r "$INSTDIR\worlds"
    RMDir /r "$INSTDIR\players"
    RMDir /r "$INSTDIR\plugins"
    Delete "$INSTDIR\nukkit.yml"
    Delete "$INSTDIR\server.properties"
    Delete "$INSTDIR\server.log"
    Delete "$INSTDIR\ops.txt"
    Delete "$INSTDIR\white-list.txt"
    Delete "$INSTDIR\banned-players.json"
    Delete "$INSTDIR\banned-ips.json"
  manter:

  Delete "$INSTDIR\desinstalar.exe"
  ; RMDir sem /r so remove pastas vazias.
  RMDir "$INSTDIR\plugins"
  RMDir "$INSTDIR"
  DeleteRegKey HKCU "${CHAVE_REMOVER}"
  DeleteRegKey HKCU "${CHAVE}"
SectionEnd
