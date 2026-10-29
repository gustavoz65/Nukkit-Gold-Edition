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

Foram adicionados zumbi (32), esqueleto (34) e aranha (35), com perseguição simples, colisões e ataques por eventos; o esqueleto lança flechas. O creeper existente ganhou comportamento de aproximação e explosão cancelável. Os registros e as dimensões podem ser usados pelas spawn eggs já existentes no core.

O spawn natural faz no máximo oito tentativas por mundo a cada 200 ticks, em chunks já carregados, a pelo menos 24 blocos dos jogadores, respeitando luz, dificuldade, `spawn-mobs` e `spawn-limits.monsters`. A primeira implementação usa a superfície dos chunks; não implementa a distribuição completa de cavernas do jogo original.

O spawner persiste EntityId e Delay, aceita os IDs hostis 32 a 35 e verifica jogadores a até 16 blocos uma vez por segundo. Tenta gerar até quatro mobs por rodada, com limite de seis próximos e intervalo de 200 a 800 ticks. `CreatureSpawnEvent` permite cancelar o spawn antes de criar a entidade. Colocar o bloco cria seu block entity; uma spawn egg compatível muda o tipo.

Foram testados criação, atualização sem alvo, NBT, perseguição e ataque de zumbi contra jogador simulado, cancelamento de spawn e limite local do spawner. Aparência, animações, combate com cliente real, flechas em jogo e explosões no cliente permanecem sem validação visual. A IA é direta, sem navegação avançada.

Referências dos identificadores: [zumbi no PocketMine histórico](https://github.com/pmmp/PocketMine-MP/blob/6fc435da0e0ca430558930f73042e1f9130815b7/src/pocketmine/entity/Zombie.php), [esqueleto no Nukkit](https://github.com/CloudburstMC/Nukkit/blob/master/src/main/java/cn/nukkit/entity/mob/EntitySkeleton.java) e [aranha no Nukkit](https://github.com/CloudburstMC/Nukkit/blob/master/src/main/java/cn/nukkit/entity/mob/EntitySpider.java).

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
