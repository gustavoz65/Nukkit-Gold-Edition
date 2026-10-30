#!/usr/bin/env bash
# Gera o instalador Windows (build/NukkitGold-Setup-<versao>.exe) a partir do core validado em builds/.
# Opcional: PLUGINS_DIR=pasta com .jar para incluir na pasta plugins/ do servidor.
# Requer: makensis (pacote nsis), python3 com pip e unzip. Roda em Linux; nao precisa de Windows.
set -euo pipefail

raiz="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
versao="${VERSAO:-0.3.2}"
# Java embutido: runtime OpenJDK do pacote jdk4py (PyPI), fixado por versao e hash.
java_pacote="jdk4py==25.0.2.1"
java_hash="5f0ad2e8f7ad3b40eef8af75062d830afd3feccca62fc03db611b2e9a5fa9824"

for comando in makensis python3 unzip; do
    command -v "$comando" >/dev/null || { printf 'Erro: %s nao encontrado.\n' "$comando" >&2; exit 1; }
done

trabalho="$raiz/build/instalador"
origem="$trabalho/origem"
rm -rf -- "$trabalho"
mkdir -p "$origem/plugins" "$trabalho/java"

cp "$raiz/builds/nukkit-gold-0.15.10.jar" "$origem/nukkit-gold.jar"
if [[ -n "${PLUGINS_DIR:-}" ]]; then
    cp "$PLUGINS_DIR"/*.jar "$origem/plugins/"
fi
cp "$raiz/start.bat" "$raiz/LICENSE" "$raiz/installer/iniciar.cmd" "$raiz/installer/nukkit-gold.ico" "$origem/"
# Mesmo arquivo que o servidor gera ao escolher "bra" na primeira execucao.
unzip -p "$origem/nukkit-gold.jar" lang/bra/nukkit.yml > "$origem/nukkit.yml"

printf '%s --hash=sha256:%s\n' "$java_pacote" "$java_hash" > "$trabalho/java/requisitos.txt"
python3 -m pip download --quiet --no-deps --only-binary=:all: --platform win_amd64 --python-version 3.12 \
    --require-hashes -r "$trabalho/java/requisitos.txt" -d "$trabalho/java"
unzip -q "$trabalho"/java/jdk4py-*.whl 'jdk4py/java-runtime/*' -d "$trabalho/java"
mv "$trabalho/java/jdk4py/java-runtime" "$origem/runtime"
# Cabecalhos C so servem para compilar codigo nativo.
rm -rf -- "$origem/runtime/include"
[[ -f "$origem/runtime/bin/java.exe" ]] || { printf 'Erro: java.exe ausente no runtime.\n' >&2; exit 1; }

saida="$raiz/build/NukkitGold-Setup-$versao.exe"
makensis -V2 -INPUTCHARSET UTF8 -DORIGEM="$origem" -DSAIDA="$saida" -DVERSAO="$versao" "$raiz/installer/nukkit-gold.nsi"
(cd "$raiz/build" && sha256sum "$(basename "$saida")" > "$(basename "$saida").sha256")
printf 'Instalador: %s\n' "$saida"
