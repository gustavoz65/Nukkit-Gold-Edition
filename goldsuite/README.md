# GoldSuite

Reescrita modular dos 86 plugins PHP em Java para Nukkit-Gold, protocolo 84. Esta entrega implementa as interfaces de `gold-api`, **parte da base comum** de `gold-core` e o primeiro módulo de jogo, [GoldAuth](gold-auth/README.md). Os demais módulos de jogo ainda serão implementados; os arquivos PHAR originais não são executados pelo Nukkit.

## Compilar e verificar

Compile primeiro o core na raiz do repositório com `scripts/build.bat` ou `bash scripts/build.sh`. Depois, nesta pasta:

No Windows, também é possível executar `scripts/build-suite.ps1` a partir da raiz: ele usa temporariamente o JDK do `javac` no PATH, restaura `JAVA_HOME` ao terminar e executa o build e os testes offline.

```powershell
.\gradlew.bat --offline check jar
```

```bash
bash gradlew --offline check jar
```

Use um JDK compatível com Gradle 8.14.3; o **JDK 17 foi testado**. O `JAVA_HOME` também precisa apontar para esse JDK: neste computador ele apontava para Java 27, incompatível com essa versão do Gradle, apesar de `java` e `javac` no PATH serem Java 17. O ajuste usado nos testes foi somente no processo, sem mudar as variáveis permanentes do Windows.

O alvo das classes continua sendo Java 8. O wrapper precisa baixar o Gradle na primeira execução se ele ainda não estiver no cache; a distribuição está fixada por versão e SHA-256. Depois disso, `--offline` utiliza apenas dependências locais. Não há repositórios Maven configurados.

Saídas:

- `gold-api/build/libs/gold-api-0.1.0.jar`: interfaces para compilar os módulos; não instalar como plugin separado.
- `gold-core/build/libs/gold-core-0.1.0.jar`: plugin completo com as interfaces e o driver SQLite embutidos.
- `gold-auth/build/libs/gold-auth-0.1.0.jar`: registro, login e troca de senha; requer GoldCore.

Copie apenas o jar do GoldCore para `plugins/` do servidor e reinicie. O GoldCore cria `plugins/GoldCore/goldsuite.db` e `plugins/GoldCore/assinatura.key`. A chave precisa acompanhar os dados nos backups; trocá-la invalida os itens assinados anteriormente.

## Usar os serviços

Os módulos futuros usarão `depend: [GoldCore]` e `compileOnly project(':gold-api')`. As interfaces são carregadas pelo GoldCore, evitando cópias conflitantes em cada plugin.

```java
CoreService core = getServer().getServiceManager().load(CoreService.class);
core.whenReady()
    .thenCompose(ignorado -> core.getStore().put("meu_modulo", "chave", "valor"))
    .thenCompose(ignorado -> core.getStore().get("meu_modulo", "chave"))
    .thenAccept(valor -> getLogger().info(valor.orElse("Sem registro")));
```

Chame a API a partir da thread principal. O trabalho de disco/banco roda em `AsyncTask.onRun`; as conclusões normais são entregues em `onCompletion` na thread principal. Uma operação recusada por encerramento ou fila cheia retorna um futuro com falha. Trate os erros com `exceptionally` ou `whenComplete` e confira se seu plugin continua ativo antes de usar Player/Level em callbacks.

A fila serial é limitada a 1.024 operações pendentes e processa lotes de até 64 operações por tarefa. Isso limita a fila e permite entregar resultados durante carga contínua. No desligamento, o GoldCore recusa novas operações e aguarda por até cinco segundos as escritas já enfileiradas. Se esse prazo não bastar, ele registra um aviso; a escrita só está confirmada quando seu futuro conclui com sucesso.

## Itens especiais

`getItemSigner().sign(item, "goldcrates:chave")` devolve uma cópia assinada. `verify(item, finalidade)` verifica uma HMAC-SHA256 sobre ID, metadado e NBT canônico. A quantidade fica fora, para que pilhas assinadas possam ser divididas e juntadas; em itens com durabilidade o desgaste também fica fora, senão uma espada assinada deixaria de valer no primeiro golpe. A comparação é constante em tempo para assinaturas do mesmo tamanho; um nome visual sozinho não autoriza efeitos especiais.

Alterar o ID, o metadado de itens sem durabilidade ou o NBT exige gerar uma nova assinatura no servidor. Quem precisa limitar a quantidade (uma chave por jogador, por exemplo) deve conferir o count no próprio plugin. A assinatura impede adulteração dos dados assinados, mas não identifica cada exemplar como um item único nem substitui a conservação de itens do core. O trânsito desses itens pelo cliente MCPE real ainda precisa ser testado.

## Testes realizados

- Gradle `check` executa `gold-core:regression`, um programa Java de testes, sem JUnit ou downloads de frameworks.
- SQLite real: gravação, reabertura, exclusão, UTF-8 e chave com texto semelhante a SQL injection.
- HMAC: finalidade diferente, quantidade alterada, NBT alterado e chave de outro servidor.
- Plugin carregado no servidor: inicialização assíncrona, registro e remoção do serviço, retorno na thread principal e recusa após desativação.
- Escrita enfileirada imediatamente antes de desativar o GoldCore: valor conferido no banco após o processo do servidor encerrar.

Menus virtuais, hologramas, cooldowns, os demais eventos de domínio e os módulos de economia, proteção e outras funcionalidades seguem pendentes no [roadmap](ROADMAP.md). A autenticação inicial tem seu próprio [guia e limites](gold-auth/README.md).

## Dependência incluída

O [SQLite JDBC 3.54.1.0](https://github.com/xerial/sqlite-jdbc/releases/tag/3.54.1.0) vem em `lib/`, com as licenças originais dentro do jar. O SHA-256 conferido contra o arquivo de checksums oficial é `f8dce420a6016278497f4f0560f65602b9db858de141b541cf91a4644ebe4639`. A [licença do driver](https://github.com/xerial/sqlite-jdbc/blob/3.54.1.0/LICENSE) e os avisos incorporados permanecem preservados no plugin empacotado.
