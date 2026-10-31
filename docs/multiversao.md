# Multiversão (0.15.10 → 0.16 → 1.0)

Objetivo: um único servidor, um único mundo e uma única base de plugins, com jogadores de versões diferentes jogando juntos.

## Como vai funcionar
- O servidor continua pensando em 0.15.10 (protocolo 84). Cada jogador ganha um **tradutor** pela versão com que entrou: pacotes, ids de blocos/itens, metadados e chunks são convertidos nos dois sentidos, por tabelas montadas uma vez no início (sem custo para quem está no 0.15.10).
- Ordem: **0.16.x**, depois **1.0** (resource pack com texturas próprias, ender pearl e itens novos). Versões abaixo de 0.15.10 ficam de fora (protocolo e mundo muito diferentes).
- Os plugins consultam `Player.getProtocol()` para oferecer recursos por versão: cosmético com textura própria só para 1.0, alternativa 3D para 0.15.10; ender pearl vira bola de neve para quem não tem o item.
- A 0.16 usa a mesma base de plugins da 1.0; não existe uma base por versão.

## O que já existe (0.4.0)
- `Player.getProtocol()`: versão de cada jogador.
- `Player.getLoginData()` e `getDeviceFingerprint()`: dados do cliente no login (base da detecção de plataforma e do 2FA).
- Conexões de outras versões são recusadas com registro no console (protocolo e nick), para medir quem tenta entrar.

## Próximos passos
1. Camada de tradução na rede (RakNetInterface → tradutor por sessão).
2. Tradutor 0.16: login, start game, chunks, inventário, entidades.
3. Tradutor 1.0: subchunks, resource pack, novos itens/blocos.
4. Testes com clientes reais de cada versão antes de liberar.
