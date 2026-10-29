# Build inicial 0.1.0

Fonte validado no commit 6980fbc930f2262732e196f62e9b60139224a5f5, em 29/10/2026, no Linux com JDK 21 e alvo Java 8, depois da segunda revisão (docs/alteracoes-core.md).

- nukkit-gold-0.15.10.jar: core com protocolo 84, base Gold, correcoes de rede e inventario, servicos, isolamento, IA de mobs e spawners.
- plugins/GoldCore-0.1.0.jar: SQLite assincrono e assinatura HMAC.
- plugins/GoldAuth-0.1.0.jar: registro, login, troca de senha e protecao da sessao.

SHA-256 dos tres arquivos em SHA256SUMS. Os jars foram copiados dos resultados que passaram nos testes unitarios e de integracao. A reescrita dos 86 plugins ainda esta em andamento; consulte goldsuite/ROADMAP.md no repositorio.

Cliente MCPE real, interface dos plugins no celular, Java 8 em execucao e Windows e macOS deste build ainda nao foram verificados. Testes de autenticacao usaram Player simulado e SQLite real. Nao distribua o plugin GoldCoreRegression com estes arquivos de uso normal.