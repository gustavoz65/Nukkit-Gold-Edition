#!/usr/bin/env bash
# Firewall do Nukkit Gold para VPS Linux (Contabo e outras), com nftables.
# Segura floods pequenos de UDP antes de chegarem ao Java. Floods que lotam o link precisam de protecao da hospedagem.
#
# Uso (como root):  sudo bash firewall-nftables.sh
# Variaveis opcionais:
#   PORTA_JOGO=19132        porta UDP do servidor
#   PORTA_SSH=22            porta do SSH (sempre liberada; confira antes de usar FECHAR_RESTO=1)
#   PACOTES_POR_SEG=2000    limite de pacotes UDP por segundo por IP na porta do jogo
#   PORTAS_EXTRAS="8080"    outras portas TCP liberadas (painel, site...)
#   FECHAR_RESTO=1          bloqueia todas as outras portas de entrada (padrao: 0, so limita a porta do jogo)
#   REMOVER=1               apaga a tabela do Nukkit Gold e sai
set -euo pipefail

PORTA_JOGO="${PORTA_JOGO:-19132}"
PORTA_SSH="${PORTA_SSH:-22}"
PACOTES_POR_SEG="${PACOTES_POR_SEG:-2000}"
PORTAS_EXTRAS="${PORTAS_EXTRAS:-}"
FECHAR_RESTO="${FECHAR_RESTO:-0}"

[[ $EUID -eq 0 ]] || { echo "Rode como root (sudo)." >&2; exit 1; }
command -v nft >/dev/null || { echo "Instale o nftables: apt install nftables" >&2; exit 1; }
for n in "$PORTA_JOGO" "$PORTA_SSH" "$PACOTES_POR_SEG"; do [[ "$n" =~ ^[0-9]+$ ]] || { echo "Valor invalido: $n" >&2; exit 1; }; done
for p in $PORTAS_EXTRAS; do [[ "$p" =~ ^[0-9]+$ ]] || { echo "Porta extra invalida: $p" >&2; exit 1; }; done

nft delete table inet nukkitgold 2>/dev/null || true
if [[ "${REMOVER:-0}" == "1" ]]; then echo "Firewall do Nukkit Gold removido."; exit 0; fi

politica="accept"
[[ "$FECHAR_RESTO" == "1" ]] && politica="drop"
extras=""
for p in $PORTAS_EXTRAS; do extras+="    tcp dport $p accept"$'\n'; done

nft -f - <<NFT
table inet nukkitgold {
  chain entrada {
    type filter hook input priority 0; policy $politica;
    iif lo accept
    ct state established,related accept
    ct state invalid drop
    # SSH sempre liberado (com limite para dificultar forca bruta)
    tcp dport $PORTA_SSH ct state new limit rate 30/minute accept
    tcp dport $PORTA_SSH drop
$extras
    # Jogo: pacotes vazios ou gigantes nao existem no RakNet
    udp dport $PORTA_JOGO udp length < 9 drop
    udp dport $PORTA_JOGO udp length > 1500 drop
    # Limite por IP: quem passar disso tem os pacotes descartados
    udp dport $PORTA_JOGO meter nukkit_por_ip { ip saddr limit rate over $PACOTES_POR_SEG/second burst 500 packets } drop
    udp dport $PORTA_JOGO accept
    icmp type echo-request limit rate 5/second accept
    icmp type echo-request drop
  }
}
NFT

echo "Firewall ativo: UDP $PORTA_JOGO limitado a $PACOTES_POR_SEG pacotes/s por IP, SSH $PORTA_SSH liberado, politica de entrada: $politica."
echo "Para manter depois de reiniciar a VPS: nft list ruleset > /etc/nftables.conf && systemctl enable nftables"
