# Hospedagem: DDoS, Contabo e Pterodactyl

## Anti-DDoS: duas camadas
1. **Fora do servidor (volumétrico).** Floods de UDP que lotam o link não chegam ao Java; só a rede resolve.
   - Hospedagem com filtro UDP para jogos (NeoProtect, Path.net, Cloudflare Spectrum, OVH Game).
   - TCPShield não serve: é só para Minecraft Java (TCP). O MCPE usa UDP.
   - Firewall do sistema (nftables/iptables): só 19132/UDP e SSH abertos, limite de pacotes por IP na 19132.
2. **Dentro do core (aplicação).** Flood de handshake/ping RakNet, bots entrando em massa, excesso de pacotes por sessão e pacotes malformados.
   - Tem que ficar na camada RakNet do core, não em plugin (o plugin só vê o evento depois do custo de decodificar).
   - Plano: limite de conexões e logins por IP, limite de pings, pacotes/s por sessão, bloqueio temporário de IP, fila de entrada em modo ataque, validação de tamanho e partes do RakNet, tudo configurável no `nukkit.yml` e com comando no console.

## Contabo
- Tem proteção sempre ligada e incluída, mas é genérica: não há filtro documentado para jogos em UDP.
- Em ataques maiores, usuários relatam **nullroute** (o IP da VPS fica fora do ar até o ataque acabar).
- Recomendado:
  1. Firewall do painel da Contabo: só 19132/UDP e SSH (de preferência só do seu IP).
  2. nftables na VPS com limite por IP na 19132.
  3. Módulo anti-DDoS no core.
  4. Se virar alvo: proxy UDP protegido na frente, escondendo o IP da Contabo.

## Pterodactyl
Funciona com o egg do Nukkit (é um `.jar` Java comum; compilado para Java 8, roda em imagens Java 8 a 21). Ajustes:
1. **Frase de "ligado":** o egg procura `)! For help, type` (inglês). Em português o servidor escreve `Finalizado (Xs)! Para ver a ajuda...`. Em *Configuração > Start Configuration* use `{"done": ")! "}`, que funciona nos dois idiomas.
2. **Idioma na primeira execução:** sem `nukkit.yml` o servidor pergunta o idioma. Digite `bra` no console ou suba o `nukkit.yml` do pacote Linux.
3. **Restart automático:** o laço do `start.sh`/`iniciar.cmd` (arquivo `reiniciar.servidor`) não é usado no painel; `/restart auto` só desliga. Use uma tarefa agendada de Start no painel, ou faça o plugin encerrar com código de erro para o painel religar.
- Porta padrão 19132/UDP; plugins em `plugins/`, mapa do HG em `worlds/`.
