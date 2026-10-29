# Evolução inicial do core Gold

## Console e identidade

O servidor identifica-se como Nukkit-Gold 0.1.0, mantendo API 1.0.0 e protocolo 84. Os streams de entrada e saída do console usam UTF-8 explicitamente; no Windows e em entrada redirecionada, o core usa console simples e evita depender das DLLs de jline/jansi. O iniciador continua aplicando suas opções compatíveis.

## Primeira rodada da auditoria

| Correção | Motivo | Verificação |
| --- | --- | --- |
| Limite na saída de Zlib | O limite anterior atingia a entrada comprimida e permitia expansão excessiva | Carga de 1 MiB comprimida, limite exato, excesso de um byte e truncamento |
| Leituras binárias estritas | Campos incompletos eram aceitos parcialmente | Campos negativos, truncamento e comprimentos excessivos |
| Floats finitos na rede | NaN e infinito invalidam comparações usadas em movimento e alcance | Leitura de NaN e infinito rejeitada |
| Lotes limitados | Comprimentos e quantidades podiam produzir alocações e trabalho excessivos | Lote válido, lote aninhado, tamanho inválido e mais de 256 pacotes |
| Limites de NBT | Arrays podiam alocar o tamanho informado antes da validação; recursão não tinha limite | Arrays malformados, profundidade, tipos inválidos, UTF-8 e duas ordens de bytes |
| Login validado | Nome, UUID e skin eram usados antes de validar sua presença; JSON não tinha limite de profundidade | Login sintético com protocolo 84 e skin válida, nomes reservados e dados truncados |
| Limites de flood | O contador anterior liberava mensagens a cada tick | Rajada, reposição gradual e capacidade máxima, com relógio controlado |
| Inventário em duas fases | Um cancelamento intermediário podia deixar uma transferência parcialmente aplicada | Transferência, cancelamentos, repetição, duplicação e slot inválido no servidor isolado |

Limites atuais: 2 MiB para lote/payload de rede, 256 pacotes por lote, 256 KiB para login descomprimido, oito tokens na cadeia, profundidade 32 no JSON de login, 16 KiB de NBT por item recebido. O NBT geral tem orçamento de 16 MiB, profundidade 64, até 65.536 tags e 1.048.576 elementos por coleção. Nomes aceitos: `[A-Za-z0-9_]{3,16}`, exceto console, rcon e nomes de dispositivos reservados no Windows (CON, PRN, AUX, NUL, COM1–9 e LPT1–9), para preservar os arquivos de jogadores.

Cada jogador tem limite de 400 pacotes por segundo, com rajada de 400; os pacotes internos dos lotes também contam. Chat e comandos compartilham quatro mensagens por segundo, com rajada de quatro, além do contador por tick já existente. Mensagens têm no máximo 1.024 caracteres por pacote e 255 por linha. Pacotes malformados encerram a sessão correspondente e não registram o payload completo no console.

Essas verificações constituem uma primeira rodada de auditoria. Não representam uma certificação contra todos os clientes modificados, todos os caminhos de crafting ou todas as formas de duplicação. Ainda é necessário testar a jogabilidade com o cliente real e ampliar os testes de rede e carga.

## Serviços e isolamento de plugins

`Server.getServiceManager()` registra uma implementação por tipo e plugin, com prioridades de LOWEST a HIGHEST. Registros do mesmo plugin substituem os anteriores; implementações de plugins desativados não são retornadas. Desativar ou limpar plugins remove seus serviços.

Eventos e comandos contêm falhas não fatais de plugins, inclusive `LinkageError`. Os timers encerram em `finally`, e argumentos de comandos não são incluídos no diagnóstico genérico, pois podem conter senhas. Falhas fatais da JVM, como `OutOfMemoryError` e `ThreadDeath`, continuam sendo propagadas, inclusive quando embrulhadas em outra exceção.

O teste de integração lança `NoClassDefFoundError` de propósito em um evento e em um comando. O listener seguinte, um comando posterior e o ping continuam funcionando. Os avisos dessas duas falhas são esperados nesse teste; eles não são emitidos pelo servidor normal sem o plugin de regressão.

`ServerScheduler.executeAsyncTask` oferece início imediato no pool e conclusão por `onCompletion` na thread principal. O GoldCore usa esse caminho para poder terminar uma escrita enfileirada durante o mesmo tick em que recebe o desligamento, sem depender de outro heartbeat para iniciar o trabalho.

Os comandos lidos do console são agendados na thread principal, para que comandos de plugins não alterem Player/Level na thread do leitor. O teste digita um comando que confere a identidade da thread, seguido de `list` e `stop`.

O teste do GoldAuth expôs outro defeito antigo: tarefas não recebiam seu TaskHandler e não podiam cancelar a si mesmas. O agendador agora vincula tarefa e handler antes de publicar o trabalho, preserva onCancel, evita reagendar uma tarefa cancelada e chama o cancelamento apenas uma vez. O teste integrado executa e cancela uma tarefa repetitiva e confere esses invariantes.

## Mobs e spawners

Zumbi (32), esqueleto (34), aranha (35) e creeper (33) compartilham a IA de `EntityMob`. Para comparação: no Genisys 0.15 o `onUpdate` do zumbi retornava antes de executar a IA, e os cores PHP da época deixavam os mobs parados. A IA atual faz:

| Comportamento | Detalhe |
| --- | --- |
| Alvo | Jogador vivo, sobrevivência ou aventura, até 16 blocos e com linha de visão para escolher um alvo novo. Quem ataca o mob vira alvo por 10 segundos, mesmo sem visão |
| Knockback | O mob não sobrescreve a velocidade enquanto está no tempo de dano; fricção 0,6 no chão e 0,91 no ar |
| Movimento | Pulo de 0,42 ao bater num bloco com espaço acima; a aranha escala paredes; mobs flutuam na água e só andam em chunks carregados |
| Desvio | Sem progresso por 1 segundo, tenta um desvio lateral por 1 segundo |
| Vagar | Sem alvo, escolhe um ponto a até 8 blocos por 4 a 8 segundos, ou fica parado de 2 a 6 segundos; evita quedas de mais de 3 blocos, lava e fogo |
| Sol | Zumbi e esqueleto pegam fogo de dia com céu aberto e fora da água |
| Despawn | Some a mais de 128 blocos de todos os jogadores; entre 32 e 128, chance de 1/40 por segundo. Mobs com nome, `PersistenceRequired` ou em mundo vazio ficam |
| Drops | Zumbi: 0–2 carne podre. Esqueleto: 0–2 ossos e 0–2 flechas. Aranha: 0–2 linhas e 1/3 de olho. Creeper: 0–2 pólvora |

O dano corpo a corpo segue a dificuldade (2, 3 ou 4). O esqueleto mira com compensação da queda da flecha e alguma imprecisão, e recua quando o jogador fica a menos de 4 blocos. A aranha só fica hostil no escuro (luz abaixo de 8) ou quando é atacada. O creeper arma a explosão por 1,5 segundo a até 3 blocos; a explosão pode ser cancelada por evento.

O spawn natural faz no máximo oito tentativas por mundo a cada 200 ticks, em chunks já carregados, a pelo menos 24 blocos dos jogadores, respeitando luz, dificuldade, `spawn-mobs` e `spawn-limits.monsters`. Usa a superfície dos chunks; não implementa a distribuição completa de cavernas do jogo original.

O spawner persiste EntityId e Delay, aceita os IDs hostis 32 a 35 e verifica jogadores a até 16 blocos uma vez por segundo. Tenta gerar até quatro mobs por rodada, com limite de seis próximos e intervalo de 200 a 800 ticks. `CreatureSpawnEvent` permite cancelar o spawn antes de criar a entidade. Colocar o bloco cria seu block entity; uma spawn egg compatível muda o tipo. Quebrar o spawner não dropa o bloco, como no jogo original; antes, qualquer picareta permitia coletar e multiplicar spawners.

Testes no servidor isolado: criação, NBT, perseguição e ataque do zumbi contra jogador simulado, knockback, despawn por distância, aranha neutra de dia e revidando após ataque, cancelamento de spawn e limite do spawner. Aparência, animação de inchar do creeper, flechas e explosões no cliente real ainda não foram verificadas.

Referências dos identificadores: [zumbi no PocketMine histórico](https://github.com/pmmp/PocketMine-MP/blob/6fc435da0e0ca430558930f73042e1f9130815b7/src/pocketmine/entity/Zombie.php), [esqueleto no Nukkit](https://github.com/CloudburstMC/Nukkit/blob/master/src/main/java/cn/nukkit/entity/mob/EntitySkeleton.java) e [aranha no Nukkit](https://github.com/CloudburstMC/Nukkit/blob/master/src/main/java/cn/nukkit/entity/mob/EntitySpider.java).

## Segunda revisão

| Correção | Problema anterior |
| --- | --- |
| Quantidade da pilha validada só na transação | O decode de slot expulsava o jogador quando o count passava do `getMaxStackSize` do servidor, que divergia do cliente (cabeças empilham até 64). Agora o pacote só recusa count acima de 64 |
| Criativo isolado | Itens com NBT não podiam ser reorganizados no criativo, e o `force` anterior executava a transação inteira em qualquer janela, o que permitia clonar itens de baús. Agora só o inventário do jogador e a armadura aceitam a troca livre, e apenas quando os dois itens existem no menu criativo |
| Erros de rede registrados | Erros ao tratar um pacote válido eram descartados sem log. Pacote malformado continua gerando só uma linha de debug; erro interno gera log completo e encerra a sessão com mensagem própria |
| `StackOverflowError` contido | Recursão infinita num plugin derrubava o servidor; agora é tratada como falha do plugin. `OutOfMemoryError` e as demais falhas da JVM continuam sendo propagadas |
| Token de login nos dois alfabetos | O decode aceitava só base64url; tokens no alfabeto padrão eram recusados |
| HMAC sem quantidade e desgaste | Itens assinados ficavam inválidos ao dividir a pilha ou ao usar a ferramenta |
| GoldAuth por falhas | O limite contava logins corretos e bloqueava jogadores atrás do mesmo IP; no pico, o semáforo recusava logins em vez de enfileirar |

## Reproduzir os testes

```powershell
.\scripts\build.bat
.\scripts\test.ps1
cd goldsuite
.\gradlew.bat --offline check jar
cd ..
.\scripts\build-test-plugin.ps1
.\scripts\test-integration.ps1
```

Os testes unitários usam programas Java comuns. O plugin `build/gold-core-regression.jar` deve ser instalado apenas num servidor de testes isolado, junto com GoldCore e sua API compilada. Ele altera e restaura blocos do mundo de teste e provoca falhas controladas; não deve ser instalado no servidor de uso normal.
