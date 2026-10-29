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

Section "Servidor" SecServidor
  SectionIn RO
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

Section "Plugins GoldCore e GoldAuth" SecPlugins
  SetOutPath "$INSTDIR\plugins"
  File "${ORIGEM}\plugins\GoldCore-${VERSAO}.jar"
  File "${ORIGEM}\plugins\GoldAuth-${VERSAO}.jar"
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
  !insertmacro MUI_DESCRIPTION_TEXT ${SecPlugins} "Armazenamento SQLite, assinatura de itens, registro e login."
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
