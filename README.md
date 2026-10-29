# Nukkit-Gold-Edition

Servidor em Java para **Minecraft: Pocket Edition 0.15.10**, protocolo **84**, mantido por **gustavoz65**. Este fork parte do [Nukkit no commit b7378636c4cdea686eea998d53da2f3ffb8eec25](https://github.com/Nukkit/Nukkit/tree/b7378636c4cdea686eea998d53da2f3ffb8eec25), de 2016.

O core já recebeu a base Nukkit-Gold 0.1.0, uma primeira rodada de correções de segurança, registro de serviços, isolamento de falhas de plugins e suporte inicial a mobs hostis e spawners. A reescrita dos 86 plugins PHP começou pela base da suíte modular [GoldSuite](goldsuite/README.md), com SQLite assíncrono e HMAC, e pelo módulo inicial [GoldAuth](goldsuite/gold-auth/README.md), para registro e login. Os demais módulos de jogo ainda estão pendentes.

## Requisitos

- **Para executar:** Java 8 ou superior, com `java` no `PATH`.
- **Para compilar:** JDK 11 ou superior, com `java`, `javac` e `jar` no `PATH`. O alvo é Java 8, usando `javac --release 8`.
- Até **1 GiB de heap** para a JVM, mais memória para o sistema e para a própria JVM. Os iniciadores usam `-Xmx1G`.
- Windows: Prompt de Comando ou PowerShell. Linux/macOS: Bash e ferramentas usuais (`find`, `sort`, `sed`, `cp` e `rm`).

## Baixar e iniciar

1. Baixe o [jar pronto em builds/nukkit-gold-0.15.10.jar](https://github.com/gustavoz65/Nukkit-Gold-Edition/raw/refs/heads/main/builds/nukkit-gold-0.15.10.jar). A pasta [builds/](https://github.com/gustavoz65/Nukkit-Gold-Edition/tree/main/builds) mantém o binário de referência; compilar o fonte gera outro jar em `build/`.
2. Crie uma pasta para o servidor, salve o jar como `nukkit-gold.jar` e copie para essa mesma pasta o [start.bat](https://raw.githubusercontent.com/gustavoz65/Nukkit-Gold-Edition/main/start.bat) ou o [start.sh](https://raw.githubusercontent.com/gustavoz65/Nukkit-Gold-Edition/main/start.sh).
3. Execute o iniciador nessa pasta.

Windows, no PowerShell:

```powershell
.\start.bat
```

Linux/macOS:

```bash
bash start.sh
```

Os iniciadores procuram, nesta ordem, `build/nukkit-gold.jar`, `nukkit-gold.jar` e `builds/nukkit-gold-0.15.10.jar`. Também é possível escolher explicitamente um jar:

```powershell
.\start.bat "C:\servidor\nukkit-gold.jar"
```

```bash
bash start.sh /caminho/nukkit-gold.jar
```

O caminho relativo do jar é resolvido a partir da pasta do iniciador. Os dados do servidor também são criados nessa pasta, independentemente da pasta de onde o comando foi chamado. Para testar sem misturar dados com o fonte, copie apenas o iniciador para uma pasta separada e passe o caminho absoluto do jar.

Na primeira execução, digite **`bra`** e pressione Enter para selecionar português brasileiro. O servidor cria `nukkit.yml`, `server.properties`, mundos, pasta de plugins e logs. Aguarde a mensagem **`Finalizado`** antes de conectar. Digite `stop` no console para salvar e encerrar.

O início com os scripts equivale a:

```bash
java -Xmx1G -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar nukkit-gold.jar
```

No Windows, `start.bat` também seleciona a página de código UTF-8 e usa `-Djline.terminal=jline.UnsupportedTerminal` antes de `-jar`, mais o argumento `disable-ansi` depois do jar. O `start.sh` aplica essas opções de terminal ao detectar Git Bash ou Cygwin. Isso evita a inicialização do terminal nativo de jline/jansi, que pode falhar nesse Nukkit antigo. O valor literal `unsupported`, previsto inicialmente, gera `Invalid terminal type` no JLine 2.14.6 incluído; por isso os iniciadores usam o nome completo da classe.

O core também cuida do UTF-8 e seleciona o console simples no Windows ou em entrada redirecionada, inclusive ao executar diretamente com `java -jar`.

## Conectar pelo MCPE 0.15.10

1. Inicie o servidor e aguarde `Finalizado`.
2. Abra **Minecraft PE 0.15.10** no celular ou emulador e adicione um servidor externo na tela de jogar.
3. Informe o IP do computador do servidor e a porta **19132**. No mesmo computador, use `127.0.0.1`; na mesma rede, use o IPv4 local do computador (consulte `ipconfig` no Windows).
4. Salve e entre no servidor.

A porta padrão é **19132/UDP**, configurável em `server.properties`. Permita tráfego UDP nessa porta no firewall. Para acesso pela internet, configure o encaminhamento UDP no roteador e utilize um endereço público acessível.

Este core exige protocolo 84 no login; use exatamente a versão 0.15.10. A conexão e a jogabilidade com um cliente real ainda precisam de validação.

## Compilar do fonte sem Maven

```bash
git clone https://github.com/gustavoz65/Nukkit-Gold-Edition.git
cd Nukkit-Gold-Edition
```

No Windows, pelo PowerShell:

```powershell
.\scripts\build.bat
.\start.bat
```

No Linux/macOS ou Git Bash:

```bash
bash scripts/build.sh
bash start.sh
```

Os scripts compilam `src/main/java/` com `javac --release 8 -nowarn -encoding UTF-8`, usando o classpath `lib/*`. Depois extraem as dependências, removem o manifesto e as assinaturas dos jars originais, copiam as classes e os recursos de `src/main/resources/` e criam um jar único com `Main-Class: cn.nukkit.Nukkit`.

O resultado é **`build/nukkit-gold.jar`**, com os idiomas incluídos. As pastas intermediárias `build/classes/` e `build/stage/` são recriadas em cada build para evitar resíduos. Não há downloads nem dependência do Maven Central; o `pom.xml` histórico não é usado por esses scripts. `build/` é uma saída local ignorada pelo Git.

### Dependências incluídas em lib/

| Dependência | Versão | Arquivo |
| --- | --- | --- |
| Netty Common | 4.1.48.Final | `netty-common-4.1.48.Final.jar` |
| Netty Buffer | 4.1.48.Final | `netty-buffer-4.1.48.Final.jar` |
| Netty Transport | 4.1.48.Final | `netty-transport-4.1.48.Final.jar` |
| Netty Resolver | 4.1.48.Final | `netty-resolver-4.1.48.Final.jar` |
| Gson | 2.10.1 | `gson-2.10.1.jar` |
| SnakeYAML | 2.0 | `snakeyaml-2.0.jar` |
| JLine | 2.14.6 | `jline-2.14.6.jar` |
| Jansi | 1.18 | `jansi-1.18.jar` |
| LevelDB para MCPE | 0.8 | `leveldb-0.8.jar` |

## Verificação do passo 0

Em 29/10/2026, no Windows com JDK 17, foram verificados o build de referência dos 1.039 fontes, `scripts/build.bat` e `scripts/build.sh` pelo Git Bash. O bytecode do ponto de entrada foi conferido como Java 8 (versão 52).

Os iniciadores `.bat` e `.sh` pelo Git Bash foram testados com seleção de idioma `bra`, chegada a `Finalizado`, ping RakNet anunciando `84;0.15.10`, comando `list` e encerramento por `stop`, sem erro de terminal. O `start.bat` também foi testado sem argumentos com o jar ao lado do iniciador.

Ainda não foram verificados um cliente MCPE 0.15.10 real, execução em Java 8, Linux/macOS nativos ou acesso pela internet. A compilação continua emitindo avisos de operações não verificadas do código original.

## Evolução e testes atuais

O core atual tem 1.052 fontes Java. As [alterações do core](docs/alteracoes-core.md) descrevem o motivo, os limites e a cobertura dos testes de cada grupo de mudanças. Execute `scripts/test.ps1` depois de compilar para rodar os sete programas de regressão sem frameworks externos.

A [GoldSuite](goldsuite/README.md) tem build Gradle independente, com dependências locais. O [GoldCore inicial pronto](builds/plugins/GoldCore-0.1.0.jar) fornece a infraestrutura de armazenamento e assinatura. Instale também o [GoldAuth inicial](builds/plugins/GoldAuth-0.1.0.jar) em `plugins/` para registro e login. O [roadmap da reescrita](goldsuite/ROADMAP.md) distingue o que já foi implementado das funcionalidades pendentes.

Foram testados também o jar direto com codificação padrão diferente de UTF-8, as correções de parser, login e limites, transferências de inventário, isolamento de falhas simuladas, registro e limpeza de serviços, criação e atualização de mobs, ataque de zumbi contra alvo simulado, cancelamento de spawn, limites de spawner e integração do GoldCore com SQLite real. A aparência, a jogabilidade e os itens assinados no cliente MCPE real ainda precisam de verificação.

## Roadmap

O passo 0 prepara a documentação de instalação, os scripts de build e os iniciadores. A sequência de evolução aprovada é:

1. **Base do fork:** UTF-8 no console, identificação própria como Nukkit-Gold e fallback seguro quando jline/jansi falharem no Windows.
2. **Auditoria de segurança do core:** pacotes malformados, transações de inventário e duplicação de itens, flood de chat/comandos, limites de NBT/strings e nomes de jogadores; um commit por correção.
3. **Registro de serviços:** implementar um `ServiceManager` simples para comunicação entre plugins.
4. **Isolamento de plugins:** tratar também falhas como `LinkageError` nas chamadas de eventos e comandos para conter erros dos plugins.
5. **Mobs:** adicionar zumbi, esqueleto, aranha e outros com IDs de rede da 0.15, IA simples, spawn natural e `BlockEntityMobSpawner`.
6. **GoldSuite:** reescrever os 86 plugins PHP em módulos Java, começando pelas interfaces de `gold-api` e pela base comum de `gold-core`, com integrações opcionais isoladas.

## Créditos e licença

O Nukkit original foi desenvolvido pelo **Nukkit Project** e seus colaboradores, incluindo **MagicDroidX**. Este fork preserva o código-base, os créditos e os avisos originais; a manutenção de Nukkit-Gold-Edition é de **gustavoz65**. Os idiomas também vêm do projeto Nukkit.

O [README do Nukkit no commit de origem](https://github.com/Nukkit/Nukkit/blob/b7378636c4cdea686eea998d53da2f3ffb8eec25/README.md) declara **GNU Lesser General Public License, versão 3 ou posterior (LGPL-3.0-or-later)**, sem garantia. O arquivo [LICENSE](LICENSE) preservado neste repositório contém o texto da GPLv3; o texto adicional da LGPLv3 está disponível na [GNU](https://www.gnu.org/licenses/lgpl-3.0.html). As dependências em `lib/` mantêm suas próprias licenças e créditos.
