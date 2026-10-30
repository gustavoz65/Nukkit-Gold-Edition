# Build inicial 0.1.0

Fonte validado no commit bd661274e50e606eb066d1e0b63c9caad5b5dc90, em 29/10/2026, no Linux com JDK 21 e alvo Java 8, depois da segunda revisão (docs/alteracoes-core.md).

- nukkit-gold-0.15.10.jar: core com protocolo 84, base Gold, correcoes de rede e inventario, servicos, isolamento, IA de mobs e spawners.

SHA-256 em SHA256SUMS.

Pacotes gerados a partir desta pasta (com Java 25 embutido):
- Windows: bash scripts/build-installer.sh -> build/NukkitGold-Setup-0.1.0.exe
- Linux x64: bash scripts/build-linux.sh -> build/NukkitGold-0.1.0-linux-x64.tar.gz (extrair e rodar ./start.sh)

Para incluir plugins no pacote, informe a pasta com os .jar: PLUGINS_DIR=/caminho bash scripts/build-installer.sh

Cliente MCPE real, Java 8 em execucao e macOS deste build ainda nao foram verificados. Nao distribua o plugin GoldCoreRegression com estes arquivos de uso normal.
