# Nukkit-Gold-Edition

Servidor em Java para **Minecraft: Pocket Edition 0.15.10**, protocolo **84**, mantido por **gustavoz65**. Este fork parte do [Nukkit no commit b7378636c4cdea686eea998d53da2f3ffb8eec25](https://github.com/Nukkit/Nukkit/tree/b7378636c4cdea686eea998d53da2f3ffb8eec25), de 2016.

O core já recebeu a base Nukkit-Gold 0.1.0, uma primeira rodada de correções de segurança, registro de serviços, isolamento de falhas de plugins e suporte inicial a mobs hostis e spawners. Este repositório contém apenas o core do servidor.

## Instalar no Windows

1. Baixe o **NukkitGold-Setup-0.1.0.exe** na página de [Releases](https://github.com/gustavoz65/Nukkit-Gold-Edition/releases/latest).
2. Abra o arquivo e clique em **Avançar** até o fim. Não precisa de administrador nem de Java instalado.
3. Abra o atalho **Nukkit Gold** na área de trabalho.

O instalador coloca o servidor em `C:\Users\<você>\NukkitGold` (dá para trocar a pasta), já com Java 25 e o idioma em português. Ele cria atalhos na área de trabalho e no menu Iniciar, incluindo **Pasta do servidor** (onde ficam `plugins/`, `worlds/` e `server.properties`), e registra o programa em **Configurações > Aplicativos** para desinstalar.

Para atualizar, rode o instalador de uma versão nova por cima: mundos, jogadores e configurações são mantidos. Se o servidor estiver aberto, o instalador pede para ele salvar e desligar sozinho (criando o arquivo `parar.servidor`, que o core verifica a cada segundo); se não houver resposta em 30 segundos, como numa versão anterior a essa, o processo é encerrado à força e perde só o que mudou desde o último salvamento automático. Ao desinstalar, o padrão é manter esses dados; eles só são apagados se você responder **Sim** na pergunta.

O instalador não tem assinatura digital, então o Windows pode exibir "O Windows protegeu o computador". Clique em **Mais informações > Executar assim mesmo**. Na primeira execução, permita o Java no firewall em redes privadas para jogar na rede local.

Para gerar o instalador a partir do fonte (Linux, com `nsis`, `python3` e `unzip`): `bash scripts/build-installer.sh`. O Java embutido é o runtime OpenJDK 25 (GPLv2 com Classpath Exception) do pacote [jdk4py](https://pypi.org/project/jdk4py/), fixado por versão e hash; as licenças ficam em `runtime/legal`.

## Instalação manual

### Requisitos

- **Para executar:** Java 8 ou superior, com `java` no `PATH`.
- **Para compilar:** JDK 11 ou superior, com `java`, `javac` e `jar` no `PATH`. O alvo é Java 8, usando `javac --release 8`.
- Até **1 GiB de heap** para a JVM, mais memória para o sistema e para a própria JVM. Os iniciadores usam `-Xmx1G`.
- Windows: Prompt de Comando ou PowerShell. Linux/macOS: Bash e ferramentas usuais (`find`, `sort`, `sed`, `cp` e `rm`).

### Baixar e iniciar

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

No Windows, `start.bat` também seleciona a página de código UTF-8 e usa `-Djline.terminal=jline.UnsupportedTerminal` antes de `-jar`, mais o argumento `disable-ansi` depois do jar. O `start.sh` aplica essas opções de terminal ao detectar Git Bash ou Cygwin. Isso evita a inicialização do terminal nativo de jline/jansi, que pode falhar nesse Nukkit antigo. O valor literal `unsupported`, previsto inicialmente, gera `Invalid terminal type` no JLine 2.14.6 incluído; por isso os iniciadores usam o nome completo da classe. Se houver `runtime\bin\java.exe` ao lado do `start.bat`, como na instalação pelo instalador, ele usa esse Java em vez do `PATH` e acrescenta `--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow`, que evitam os avisos do Java 25 sobre o Netty e o SQLite.

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

Foram testados também o jar direto com codificação padrão diferente de UTF-8, as correções de parser, login e limites, transferências de inventário, isolamento de falhas simuladas, registro e limpeza de serviços, criação e atualização de mobs, ataque de zumbi contra alvo simulado, cancelamento de spawn e limites de spawner. A aparência e a jogabilidade no cliente MCPE real ainda precisam de verificação.

## Roadmap

O passo 0 prepara a documentação de instalação, os scripts de build e os iniciadores. A sequência de evolução aprovada é:

1. **Base do fork:** UTF-8 no console, identificação própria como Nukkit-Gold e fallback seguro quando jline/jansi falharem no Windows.
2. **Auditoria de segurança do core:** pacotes malformados, transações de inventário e duplicação de itens, flood de chat/comandos, limites de NBT/strings e nomes de jogadores; um commit por correção.
3. **Registro de serviços:** implementar um `ServiceManager` simples para comunicação entre plugins.
4. **Isolamento de plugins:** tratar também falhas como `LinkageError` nas chamadas de eventos e comandos para conter erros dos plugins.
5. **Mobs:** adicionar zumbi, esqueleto, aranha e outros com IDs de rede da 0.15, IA simples, spawn natural e `BlockEntityMobSpawner`.

## Créditos e licença

O Nukkit original foi desenvolvido pelo **Nukkit Project** e seus colaboradores, incluindo **MagicDroidX**. Este fork preserva o código-base, os créditos e os avisos originais; a manutenção de Nukkit-Gold-Edition é de **gustavoz65**. Os idiomas também vêm do projeto Nukkit.

O [README do Nukkit no commit de origem](https://github.com/Nukkit/Nukkit/blob/b7378636c4cdea686eea998d53da2f3ffb8eec25/README.md) declara **GNU Lesser General Public License, versão 3 ou posterior (LGPL-3.0-or-later)**, sem garantia. O arquivo [LICENSE](LICENSE) preservado neste repositório contém o texto da GPLv3; o texto adicional da LGPLv3 está disponível na [GNU](https://www.gnu.org/licenses/lgpl-3.0.html). As dependências em `lib/` mantêm suas próprias licenças e créditos.
