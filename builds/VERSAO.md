# Nukkit Gold 0.4.3

Compilado em 31/10/2026, no Linux com JDK 21 e alvo Java 8, depois da segunda revisão (docs/alteracoes-core.md).

- nukkit-gold-0.15.10.jar: core 0.4.3 com protocolo 84, base Gold, correcoes de rede e inventario, servicos, isolamento, IA de mobs e spawners.

Novidades da 0.4.3 (31/10/2026):
- Core sem mudancas de codigo alem da versao.
- Plugins 0.4.3 (29): GoldComandos novo (comandos liberados ou bloqueados por zona de mundos, /help so com o que o jogador pode usar ali, boas-vindas na primeira visita, itens da hotbar que executam comandos, OP so pelo console ou de IP autorizado); /suporte no GoldStaff (triagem pela IA da Groq com a chave no /painel, prompt enxuto e modelo pequeno para gastar poucos tokens, aviso para a equipe, /atender com chat isolado entre jogador e staff, /finalizar, /avaliar e /tickets metricas por membro da equipe); cargos hierarquicos no GoldPerms (dono, gerente, admin, moderador, ajudante, com tag no chat, /cargo so para quem esta abaixo, /cargos e /equipe); ninguem pune cargo igual ou superior; comando bloqueado liberado por cargo; /limparchat corrigido; chaves de API mascaradas no painel.

Novidades da 0.4.2 (31/10/2026):
- Core sem mudancas de codigo alem da versao.
- Plugins 0.4.2 (28): GoldSkyWars (estilo Hypixel, refeito do plugin de luizgeh sem as falhas de permissao e de caminho de arquivo que davam acesso a configs de outros plugins; 19 kits, baus com garantia e refil, modo insano) e GoldBedWars (ate 8 times, forja, diamante e esmeralda, loja em 7 categorias, melhorias, armadilha, bola de fogo, ovo-ponte).
- Mapas prontos (ferramentas/arena-builder): sw_ilhas e bw_quatro, com os comandos de configuracao em pontos.txt.
- Lobby: grama com a cor do bioma gravada no mapa (nao fica mais escura).

Novidades da 0.4.1 (31/10/2026):
- Fim do kick FLYING_DISABLED falso: ao entrar ou teleportar (principalmente para mundos sobre o vazio, como o lobby), o cliente fica parado no ar esperando o mapa; agora isso nao conta como voo (5 s de tolerancia e so conta com o chunk embaixo ja enviado). Quem paira de verdade e puxado de volta ao chao em vez de ser expulso.

Novidades da 0.4.0 (31/10/2026):
- Torre: pular e colocar o bloco embaixo dos pes nao some mais o bloco nem empurra o jogador para o lado (teste verificarTorre).
- Login: dados do aparelho (getLoginData, getDeviceFingerprint) e protocolo do cliente (getProtocol), base do 2FA, da deteccao de plataforma e da multiversao (docs/multiversao.md).
- Plugins 0.4.0 (26): GoldDevice (plataforma, /fps, limite de CPS), GoldKeys (keys com pedidos, pagamento, validade e resgate unico), 2FA no GoldAuth, Nuvem Voadora voando.

Novidades da 0.3.2 (30/10/2026):
- Ponte (SkyWars, bridge): blocos colocados embaixo/atras dos pes na beirada nao somem mais. O servidor recusava porque o bloco ficava "atras" da direcao do olhar, e a previsao de queda batia no bloco novo; o jogador caia e morria. Teste de regressao novo (verificarPonte).
- Console: letreiro do Nukkit Gold com o bloco de ouro, log com hora, nivel em portugues colorido e tag [Plugin] em dourado. O server.log ganhou data e hora completas.
- Clima: /weather off desliga a chuva e a trovoada em todos os mundos para sempre (grava level-settings.weather no nukkit.yml); /weather on religa.
- Plugins 0.3.2: cosmeticos 3D sem cair (chaveiro no ombro direito), menu de cosmeticos mais claro (usar/tirar, Tirar tudo), HUD nao some apos comprar.

Novidades da 0.3.1 (30/10/2026):
- Core: anti-DDoS na camada RakNet (limites por IP, modo ataque, /antiddos) e liberacao correta de IPs bloqueados.
- Hospedagem: hospedagem/firewall-nftables.sh (Contabo/VPS) e hospedagem/egg-nukkit-gold.json (Pterodactyl). Guia em docs/hospedagem.md.
- Plugins 0.3.1: painel de configuracao /painel (cores, prefixos, mensagens, precos, Discord), Nuvem Voadora 3D, pingentes-chaveiro 3D, NPC de criatura sem flutuar.

Novidades da 0.3.0 (30/10/2026):
- Core sem mudancas de codigo alem da versao; a release traz os 24 plugins do Pack-Gold-Plugins 0.3.0 (novo: GoldCosmetics).
- Plugins: ranked com elos estilo Valorant, anti-fraude e perfil na hotbar (GoldPvP), cosmeticos (GoldCosmetics), HG com inicio estilo PC, NPCs com a skin do top 3 (GoldNPC), poder de faccoes com pesos, invasao de borda e mini mapa (GoldFactions).

Novidades da 0.2.0 (30/10/2026):
- Fim do puxao do jogador na borda dos blocos (colisao com margem igual a do cliente).
- Iniciadores (iniciar.cmd e start.sh) reabrem o servidor quando um plugin pede restart (arquivo reiniciar.servidor).
- Instaladores aceitam PLUGINS_DIR; a release 0.2.0 traz os 23 plugins do Pack-Gold-Plugins 0.2.0.

SHA-256 em SHA256SUMS.

Pacotes gerados a partir desta pasta (com Java 25 embutido):
- Windows: bash scripts/build-installer.sh -> build/NukkitGold-Setup-0.4.3.exe
- Linux x64: bash scripts/build-linux.sh -> build/NukkitGold-0.4.3-linux-x64.tar.gz (extrair e rodar ./start.sh)

Para incluir plugins no pacote, informe a pasta com os .jar: PLUGINS_DIR=/caminho bash scripts/build-installer.sh

Cliente MCPE real, Java 8 em execucao e macOS deste build ainda nao foram verificados. Nao distribua o plugin GoldCoreRegression com estes arquivos de uso normal.
