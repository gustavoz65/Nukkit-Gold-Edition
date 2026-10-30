#!/usr/bin/env bash
# Gera o pacote Linux (build/NukkitGold-<versao>-linux-x64.tar.gz) a partir do core validado em builds/.
# Opcional: PLUGINS_DIR=pasta com .jar para incluir na pasta plugins/ do servidor.
# Requer: python3 com pip, unzip e tar. Para usar: extrair e rodar ./start.sh.
set -euo pipefail

raiz="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
versao="${VERSAO:-0.1.0}"
# Java embutido: runtime OpenJDK do pacote jdk4py (PyPI), mesma versao do instalador Windows.
java_pacote="jdk4py==25.0.2.1"
java_hash="383d71d8639d072752d2eaafc84944e0d8de7f1c45d30c28f6dfef8a4a65543c"

for comando in python3 unzip tar; do
    command -v "$comando" >/dev/null || { printf 'Erro: %s nao encontrado.\n' "$comando" >&2; exit 1; }
done

trabalho="$raiz/build/linux"
nome="NukkitGold"
origem="$trabalho/$nome"
rm -rf -- "$trabalho"
mkdir -p "$origem/plugins" "$trabalho/java"

cp "$raiz/builds/nukkit-gold-0.15.10.jar" "$origem/nukkit-gold.jar"
if [[ -n "${PLUGINS_DIR:-}" ]]; then
    cp "$PLUGINS_DIR"/*.jar "$origem/plugins/"
fi
cp "$raiz/start.sh" "$raiz/LICENSE" "$origem/"
cp "$raiz/installer/LEIA-ME-LINUX.txt" "$origem/LEIA-ME.txt"
chmod +x "$origem/start.sh"
# Mesmo arquivo que o servidor gera ao escolher "bra" na primeira execucao.
# Vai como .padrao para que extrair uma atualizacao por cima nao apague a configuracao do usuario;
# o start.sh copia para nukkit.yml so na primeira execucao.
unzip -p "$origem/nukkit-gold.jar" lang/bra/nukkit.yml > "$origem/nukkit.yml.padrao"

printf '%s --hash=sha256:%s\n' "$java_pacote" "$java_hash" > "$trabalho/java/requisitos.txt"
python3 -m pip download --quiet --no-deps --only-binary=:all: --platform manylinux_2_17_x86_64 --python-version 3.12 \
    --require-hashes -r "$trabalho/java/requisitos.txt" -d "$trabalho/java"
unzip -q "$trabalho"/java/jdk4py-*.whl 'jdk4py/java-runtime/*' -d "$trabalho/java"
mv "$trabalho/java/jdk4py/java-runtime" "$origem/runtime"
# Cabecalhos C so servem para compilar codigo nativo.
rm -rf -- "$origem/runtime/include"
# O zip do wheel nao guarda o bit de execucao.
chmod +x "$origem"/runtime/bin/* "$origem"/runtime/lib/jspawnhelper 2>/dev/null || true
[[ -x "$origem/runtime/bin/java" ]] || { printf 'Erro: java ausente no runtime.\n' >&2; exit 1; }

saida="$raiz/build/$nome-$versao-linux-x64.tar.gz"
tar -C "$trabalho" --owner=0 --group=0 --numeric-owner -czf "$saida" "$nome"
(cd "$raiz/build" && sha256sum "$(basename "$saida")" > "$(basename "$saida").sha256")
printf 'Pacote Linux: %s\n' "$saida"
