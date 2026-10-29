#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

for ferramenta in javac jar; do
    if ! command -v "$ferramenta" >/dev/null 2>&1; then
        printf 'Erro: %s ausente. Instale um JDK 11 ou superior e configure o PATH.\n' "$ferramenta" >&2
        exit 1
    fi
done
if [[ ! -d src/main/java || ! -d src/main/resources || ! -d lib ]]; then
    printf 'Erro: as pastas src/main/java, src/main/resources e lib sao obrigatorias.\n' >&2
    exit 1
fi
shopt -s nullglob
dependencias=(lib/*.jar)
if (( ${#dependencias[@]} == 0 )); then
    printf 'Erro: nenhuma dependencia encontrada em lib/.\n' >&2
    exit 1
fi

# Limpa apenas os intermediarios para nao incluir classes de builds anteriores.
rm -rf -- build/classes build/stage
mkdir -p build/classes build/stage
find src/main/java -name '*.java' -type f | LC_ALL=C sort | sed 's/.*/"&"/' > build/sources.txt
if [[ ! -s build/sources.txt ]]; then
    printf 'Erro: nenhum fonte Java encontrado.\n' >&2
    exit 1
fi
printf 'Compilando para Java 8...\n'
javac --release 8 -nowarn -encoding UTF-8 -cp 'lib/*' -d build/classes @build/sources.txt

(
    cd build/stage
    for dependencia in ../../lib/*.jar; do
        jar xf "$dependencia"
    done
    # O jar final recebe seu proprio manifesto; assinaturas antigas nao se aplicam.
    rm -f -- META-INF/MANIFEST.MF META-INF/*.SF META-INF/*.RSA META-INF/*.DSA
    cp -R ../classes/. .
    cp -R ../../src/main/resources/. .
    printf 'Main-Class: cn.nukkit.Nukkit\n\n' > ../manifest.txt
    jar cfm ../nukkit-gold.jar ../manifest.txt .
)
printf 'Build concluido: build/nukkit-gold.jar\n'
