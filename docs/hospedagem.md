# Hospedagem: DDoS, Contabo e Pterodactyl

## Anti-DDoS: duas camadas
1. **Fora do servidor (volumétrico).** Floods de UDP que lotam o link não chegam ao Java; só a rede resolve.
   - Hospedagem com filtro UDP para jogos (NeoProtect, Path.net, Cloudflare Spectrum, OVH Game).
   - TCPShield não serve: é só para Minecraft Java (TCP). O MCPE usa UDP.
   - Firewall do sistema (nftables/iptables): só 19132/UDP e SSH abertos, limite de pacotes por IP na 19132.
2. **Dentro do core (aplicação).** Flood de handshake/ping RakNet, bots entrando em massa, excesso de pacotes por sessão e pacotes malformados.
   - Tem que ficar na camada RakNet do core, não em plugin (o plugin só vê o evento depois do custo de decodificar).
   - **Implementado (0.3.1):** `cn.nukkit.raknet.server.Shield`, configurado em `nukkit.yml` > `network.anti-ddos`:
     - pacotes por segundo por IP (padrão 1500; acima disso o IP é bloqueado por `block-seconds`, padrão 300 s);
     - pings da lista de servidores por segundo por IP (padrão 5; o excesso nem é respondido, sem amplificação);
     - conexões abertas por IP (5) e novas conexões por minuto por IP (20);
     - modo ataque: acima de 25 conexões novas por segundo no servidor todo, por 30 s só entram 3 por segundo;
     - bloqueios vencidos agora são liberados corretamente (antes alguns IPs ficavam presos);
     - `/antiddos` (status), `/antiddos lista`, `/antiddos desbloquear <ip>` (só OP/console).
   - Testado com flood local: 100 pings em 1 s receberam só 5 respostas; 40 mil pacotes em 0,3 s bloquearam o IP e o servidor seguiu normal.

## Contabo
- Tem proteção sempre ligada e incluída, mas é genérica: não há filtro documentado para jogos em UDP.
- Em ataques maiores, usuários relatam **nullroute** (o IP da VPS fica fora do ar até o ataque acabar).
- Recomendado:
  1. Firewall do painel da Contabo: só 19132/UDP e SSH (de preferência só do seu IP).
  2. nftables na VPS: `sudo bash hospedagem/firewall-nftables.sh` (limite por IP na 19132, descarta pacotes de tamanho impossível, SSH liberado com limite). `FECHAR_RESTO=1` fecha as outras portas; `PORTAS_EXTRAS="8080"` libera mais portas; `REMOVER=1` desfaz.
  3. Módulo anti-DDoS no core.
  4. Se virar alvo: proxy UDP protegido na frente, escondendo o IP da Contabo.

## Pterodactyl
**Egg pronto:** importe `hospedagem/egg-nukkit-gold.json` em *Admin > Nests > Import Egg*. Ele já usa a frase de "ligado" certa, cria o `nukkit.yml` em português (não pergunta o idioma), ajusta porta e IP no `server.properties` e pode instalar o Pack Gold Plugins (variável `PLUGINS_URL` com o link do zip da release).

Com o egg do Nukkit comum (é um `.jar` Java; compilado para Java 8, roda em imagens Java 8 a 21), os ajustes são:
1. **Frase de "ligado":** o egg procura `)! For help, type` (inglês). Em português o servidor escreve `Finalizado (Xs)! Para ver a ajuda...`. Em *Configuração > Start Configuration* use `{"done": ")! "}`, que funciona nos dois idiomas.
2. **Idioma na primeira execução:** sem `nukkit.yml` o servidor pergunta o idioma. Digite `bra` no console ou suba o `nukkit.yml` do pacote Linux.
3. **Restart automático:** o laço do `start.sh`/`iniciar.cmd` (arquivo `reiniciar.servidor`) não é usado no painel. Quando o servidor desliga sozinho (sem o botão Stop do painel), o Wings trata como queda e religa, desde que a detecção de queda esteja ligada (padrão). Se ela estiver desligada, crie uma tarefa agendada de Start.
- Porta padrão 19132/UDP; plugins em `plugins/`, mapa do HG em `worlds/`.
