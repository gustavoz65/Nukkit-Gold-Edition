# Nukkit Gold 0.2.0

Compilado em 30/10/2026, no Linux com JDK 21 e alvo Java 8, depois da segunda revisão (docs/alteracoes-core.md).

- nukkit-gold-0.15.10.jar: core 0.2.0 com protocolo 84, base Gold, correcoes de rede e inventario, servicos, isolamento, IA de mobs e spawners.

Novidades da 0.2.0 (30/10/2026):
- Fim do puxao do jogador na borda dos blocos (colisao com margem igual a do cliente).
- Iniciadores (iniciar.cmd e start.sh) reabrem o servidor quando um plugin pede restart (arquivo reiniciar.servidor).
- Instaladores aceitam PLUGINS_DIR; a release 0.2.0 traz os 23 plugins do Pack-Gold-Plugins 0.2.0.

SHA-256 em SHA256SUMS.

Pacotes gerados a partir desta pasta (com Java 25 embutido):
- Windows: bash scripts/build-installer.sh -> build/NukkitGold-Setup-0.1.0.exe
- Linux x64: bash scripts/build-linux.sh -> build/NukkitGold-0.1.0-linux-x64.tar.gz (extrair e rodar ./start.sh)

Para incluir plugins no pacote, informe a pasta com os .jar: PLUGINS_DIR=/caminho bash scripts/build-installer.sh

Cliente MCPE real, Java 8 em execucao e macOS deste build ainda nao foram verificados. Nao distribua o plugin GoldCoreRegression com estes arquivos de uso normal.
