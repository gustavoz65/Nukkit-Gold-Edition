#!/usr/bin/env bash
set -euo pipefail

# Os dados ficam ao lado do iniciador, mesmo quando chamado de outra pasta.
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if (( $# > 0 )); then
    arquivo_jar="$1"
    shift
elif [[ -f build/nukkit-gold.jar ]]; then
    arquivo_jar=build/nukkit-gold.jar
elif [[ -f nukkit-gold.jar ]]; then
    arquivo_jar=nukkit-gold.jar
else
    arquivo_jar=builds/nukkit-gold-0.15.10.jar
fi
if [[ ! -f "$arquivo_jar" ]]; then
    printf 'Erro: jar nao encontrado: %s. Baixe o jar ou execute bash scripts/build.sh.\n' "$arquivo_jar" >&2
    exit 1
fi
# Configuracao em portugues do pacote Linux, sem sobrescrever a existente.
if [[ ! -f nukkit.yml && -f nukkit.yml.padrao ]]; then
    cp nukkit.yml.padrao nukkit.yml
fi
# O pacote Linux traz um Java 25 em runtime/; sem ele, usa o Java do PATH.
java_opcoes=()
if [[ -x runtime/bin/java ]]; then
    java_exe=runtime/bin/java
    # Netty e SQLite usam Unsafe e codigo nativo; sem isso o Java 25 imprime avisos.
    java_opcoes=(--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow)
elif command -v java >/dev/null 2>&1; then
    java_exe=java
else
    printf 'Erro: instale Java 8 ou superior e configure o PATH.\n' >&2
    exit 1
fi
# Git Bash e Cygwin tambem precisam evitar o terminal nativo no Windows.
opcoes_terminal=()
argumentos_extra=()
case "${OSTYPE:-}" in
    msys*|cygwin*)
        opcoes_terminal=(-Djline.terminal=jline.UnsupportedTerminal)
        argumentos_extra=(disable-ansi)
        ;;
esac
# Se um plugin deixar reiniciar.servidor (ex.: /restart), o servidor abre de novo sozinho.
while true; do
    status=0
    "$java_exe" -Xmx1G ${java_opcoes[@]+"${java_opcoes[@]}"} -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 ${opcoes_terminal[@]+"${opcoes_terminal[@]}"} \
        -jar "$arquivo_jar" ${argumentos_extra[@]+"${argumentos_extra[@]}"} "$@" || status=$?
    if [[ -f reiniciar.servidor ]]; then
        rm -f reiniciar.servidor
        printf 'Reiniciando em 3 segundos...\n'
        sleep 3
        continue
    fi
    exit "$status"
done
