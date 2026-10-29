# Build inicial 0.1.0

Fonte validado no commit 66f929bf6714e88a43fed848199216a85cdf6cc3, em 29/10/2026, no Windows com JDK 17 e alvo Java 8.

- nukkit-gold-0.15.10.jar: core com protocolo 84, base Gold, correcoes iniciais, servicos, isolamento e mobs/spawners basicos.
- plugins/GoldCore-0.1.0.jar: SQLite assincrono e assinatura HMAC.
- plugins/GoldAuth-0.1.0.jar: registro, login, troca de senha e protecao da sessao.

SHA-256 dos tres arquivos em SHA256SUMS. Os jars foram copiados dos resultados testados em clone limpo. A reescrita dos 86 plugins ainda esta em andamento; consulte goldsuite/ROADMAP.md no repositorio.

Cliente MCPE real, interface dos plugins no celular, Java 8 em execucao e Linux/macOS nativos ainda nao foram verificados. Testes de autenticacao usaram Player simulado e SQLite real. Nao distribua o plugin GoldCoreRegression com estes arquivos de uso normal.