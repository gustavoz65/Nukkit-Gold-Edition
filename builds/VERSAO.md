# Build inicial 0.1.0

Fonte validado no commit bd661274e50e606eb066d1e0b63c9caad5b5dc90, em 29/10/2026, no Linux com JDK 21 e alvo Java 8, depois da segunda revisão (docs/alteracoes-core.md).

- nukkit-gold-0.15.10.jar: core com protocolo 84, base Gold, correcoes de rede e inventario, servicos, isolamento, IA de mobs e spawners.
- plugins/GoldCore-0.1.0.jar: SQLite assincrono e assinatura HMAC.
- plugins/GoldAuth-0.1.0.jar: registro, login, troca de senha e protecao da sessao.
- plugins/GoldPerms-0.1.0.jar: grupos, heranca, negacoes e expiracao de permissoes.
- plugins/GoldEconomy-0.1.0.jar: money, cash, /pagar, /trocarcash, /topmoney e /economia.

Atualizacao de 30/10/2026 (branch feat/goldsuite-modulos): GoldPerms e GoldEconomy adicionados; GoldCore reconstruido porque inclui o PersistentState do gold-api; os quatro plugins foram compilados juntos no Linux com JDK 21 e alvo Java 8 e passaram nos testes de regressao e em 21 rodadas do teste de integracao no servidor real (uma falha isolada no teste de troca de senha do GoldAuth nao se repetiu).

Pacotes gerados a partir desta pasta:
- Windows: bash scripts/build-installer.sh -> build/NukkitGold-Setup-0.1.0.exe
- Linux x64: bash scripts/build-linux.sh -> build/NukkitGold-0.1.0-linux-x64.tar.gz (extrair e rodar ./start.sh)

SHA-256 dos arquivos em SHA256SUMS. Os jars foram copiados dos resultados que passaram nos testes unitarios e de integracao. A reescrita dos 86 plugins ainda esta em andamento; consulte goldsuite/ROADMAP.md no repositorio.

Cliente MCPE real, interface dos plugins no celular, Java 8 em execucao e Windows e macOS deste build ainda nao foram verificados. Testes de autenticacao usaram Player simulado e SQLite real. Nao distribua o plugin GoldCoreRegression com estes arquivos de uso normal.