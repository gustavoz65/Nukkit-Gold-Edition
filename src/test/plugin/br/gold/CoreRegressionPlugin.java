package br.gold;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.inventory.BaseInventory;
import cn.nukkit.inventory.BaseTransaction;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.inventory.SimpleTransactionGroup;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginLoader;
import cn.nukkit.plugin.PluginDescription;
import cn.nukkit.event.Event;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.HandlerList;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Executado apenas no servidor isolado de testes, nunca no jar de distribuicao. */
public final class CoreRegressionPlugin extends PluginBase implements Listener {
    private boolean cancelar;
    private boolean cancelarSpawn;
    private long mainThreadId;

    @Override
    public void onEnable() {
        this.mainThreadId = Thread.currentThread().getId();
        Plugin auth = getServer().getPluginManager().getPlugin("GoldAuth");
        if (auth != null && auth.isEnabled()) {
            br.goldsuite.api.AuthService api = getServer().getServiceManager().load(br.goldsuite.api.AuthService.class);
            getServer().getPluginManager().disablePlugin(auth);
            exigir(api.register(null, "EstadoInativo123".toCharArray()).isCompletedExceptionally(), "API de autenticacao desativada aceitou uma operacao.");
        }
        getServer().getCommandMap().register("goldteste", new Command("verificar_thread_gold") {
            @Override public boolean execute(CommandSender sender, String label, String[] args) {
                exigir(Thread.currentThread().getId() == mainThreadId, "Console executou comando fora da thread principal.");
                getLogger().info("COMANDO_CONSOLE_THREAD_PRINCIPAL");
                return true;
            }
        });
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleDelayedTask(() -> {
            try {
                verificarInventario();
                verificarServicos();
                verificarIsolamento();
                verificarMobs();
                verificarComportamentoMobs();
                verificarGoldCore().thenCompose(ignored -> verificarAgendador()).thenCompose(ignored -> verificarGoldAuth()).thenRun(() -> verificarDrenagem()).whenComplete((ignored, error) -> {
                    if (error == null) {
                        getLogger().info("VERIFICACOES_INTEGRADAS_APROVADAS");
                    } else {
                        getLogger().error("VERIFICACOES_INTEGRADAS_FALHARAM", error);
                        getServer().shutdown();
                    }
                });
            } catch (Throwable erro) {
                getLogger().error("VERIFICACOES_INTEGRADAS_FALHARAM", erro);
                getServer().shutdown();
            }
        }, 20);
    }

    @EventHandler
    public void aoTransacionar(InventoryTransactionEvent evento) {
        if (this.cancelar) { evento.setCancelled(); }
    }

    @EventHandler
    public void aoGerarMob(cn.nukkit.event.entity.CreatureSpawnEvent evento) {
        if (this.cancelarSpawn) { evento.setCancelled(); }
    }

    private void verificarInventario() {
        InventarioTeste inventario = new InventarioTeste();
        inventario.setItem(0, Item.get(Item.STONE, 0, 16));
        SimpleTransactionGroup grupo = transferencia(inventario, 0, 1, 16);
        exigir(grupo.canExecute() && grupo.canExecute(), "Transferencia valida recusada.");
        inventario.cancelarSlot = 1;
        exigir(!grupo.execute(), "Cancelamento de slot ignorado.");
        exigir(inventario.getItem(0).getCount() == 16 && inventario.getItem(1).getId() == Item.AIR, "Cancelamento aplicou parte da transferencia.");
        inventario.cancelarSlot = -1;
        exigir(grupo.execute(), "Transferencia valida falhou.");
        exigir(inventario.getItem(0).getId() == Item.AIR && inventario.getItem(1).getCount() == 16, "Itens perdidos ou duplicados.");
        exigir(!grupo.execute(), "Transacao executada duas vezes.");
        this.cancelar = true;
        exigir(!transferencia(inventario, 1, 0, 16).execute(), "Cancelamento do evento ignorado.");
        this.cancelar = false;
        exigir(inventario.getItem(1).getCount() == 16 && inventario.getItem(0).getId() == Item.AIR, "Evento cancelado alterou o inventario.");
        exigir(!transferencia(inventario, 1, 0, 17).canExecute(), "Duplicacao de itens aceita.");
        SimpleTransactionGroup invalido = new SimpleTransactionGroup();
        invalido.addTransaction(new BaseTransaction(inventario, 1000, Item.get(0), Item.get(1)));
        exigir(!invalido.canExecute(), "Slot externo ao inventario aceito.");
        getLogger().info("Inventarios: transferencia, cancelamentos, duplicacao, repeticao e slots verificados.");
    }

    private SimpleTransactionGroup transferencia(InventarioTeste inventario, int origem, int destino, int quantidade) {
        SimpleTransactionGroup grupo = new SimpleTransactionGroup();
        grupo.addTransaction(new BaseTransaction(inventario, origem, inventario.getItem(origem), Item.get(Item.AIR, 0, 0)));
        grupo.addTransaction(new BaseTransaction(inventario, destino, inventario.getItem(destino), Item.get(Item.STONE, 0, quantidade)));
        return grupo;
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) { throw new AssertionError(mensagem); }
    }

    private void verificarServicos() {
        AtomicBoolean ativo = new AtomicBoolean(true);
        PluginDescription descricao = new PluginDescription("name: GoldDummy\nmain: br.gold.Dummy\nversion: '1.0.0'\napi: ['1.0.0']\n");
        PluginLoader loader = (PluginLoader) Proxy.newProxyInstance(PluginLoader.class.getClassLoader(), new Class<?>[]{PluginLoader.class}, (proxy, metodo, args) -> {
            if (metodo.getName().equals("disablePlugin")) { ativo.set(false); }
            return null;
        });
        Plugin dummy = (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class}, (proxy, metodo, args) -> {
            switch (metodo.getName()) {
                case "isEnabled": return ativo.get();
                case "isDisabled": return !ativo.get();
                case "getDescription": return descricao;
                case "getPluginLoader": return loader;
                case "getName": return "GoldDummy";
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == args[0];
                default: return null;
            }
        });
        getServer().getServiceManager().register(CharSequence.class, "servico de teste", dummy);
        exigir("servico de teste".equals(getServer().getServiceManager().load(CharSequence.class)), "Registro no servidor falhou.");
        getServer().getPluginManager().disablePlugin(dummy);
        exigir(!ativo.get() && !getServer().getServiceManager().getKnownServices().contains(CharSequence.class), "Desativacao nao removeu o servico.");
        getLogger().info("Servicos: registro no servidor e limpeza ao desativar plugin verificados.");
    }

    private void verificarIsolamento() {
        AtomicInteger atendimentos = new AtomicInteger();
        Listener listener = new Listener() { };
        getServer().getPluginManager().registerEvent(FalhaSimuladaEvent.class, listener, EventPriority.NORMAL,
                (alvo, evento) -> { throw new NoClassDefFoundError("SimuladaEvento"); }, this);
        getServer().getPluginManager().registerEvent(FalhaSimuladaEvent.class, listener, EventPriority.MONITOR,
                (alvo, evento) -> atendimentos.incrementAndGet(), this);
        getServer().getCommandMap().register("goldteste", new Command("falha_gold_teste") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                throw new NoClassDefFoundError("SimuladaComando");
            }
        });
        getLogger().info("INICIO_FALHAS_ESPERADAS");
        getServer().getPluginManager().callEvent(new FalhaSimuladaEvent());
        exigir(atendimentos.get() == 1, "Outro listener deixou de receber o evento.");
        exigir(getServer().dispatchCommand(getServer().getConsoleSender(), "falha_gold_teste SEGREDO_NAO_DEVE_APARECER"), "Comando com falha interrompeu o dispatcher.");
        exigir(getServer().dispatchCommand(getServer().getConsoleSender(), "list"), "Comando posterior a falha nao funcionou.");
        AtomicInteger interfacesEncerradas = new AtomicInteger();
        java.util.List<cn.nukkit.network.SourceInterface> interfacesComFalha = new java.util.ArrayList<>();
        for (int i = 0; i < 2; i++) {
            cn.nukkit.network.SourceInterface interfaceComFalha = (cn.nukkit.network.SourceInterface) Proxy.newProxyInstance(
                    cn.nukkit.network.SourceInterface.class.getClassLoader(), new Class<?>[]{cn.nukkit.network.SourceInterface.class}, (proxy, metodo, args) -> {
                        switch (metodo.getName()) {
                            case "hashCode": return System.identityHashCode(proxy);
                            case "equals": return proxy == args[0];
                            case "process": throw new IllegalStateException("Interface de rede simulada");
                            case "emergencyShutdown": interfacesEncerradas.incrementAndGet(); return null;
                            default: return null;
                        }
                    });
            interfacesComFalha.add(interfaceComFalha);
            getServer().getNetwork().registerInterface(interfaceComFalha);
        }
        getServer().getNetwork().processInterfaces();
        exigir(interfacesEncerradas.get() == 2, "Uma interface com falha impediu o isolamento da seguinte.");
        for (cn.nukkit.network.SourceInterface interfaceComFalha : interfacesComFalha) {
            exigir(!getServer().getNetwork().getInterfaces().contains(interfaceComFalha), "Interface com falha permaneceu registrada.");
        }
        getLogger().info("FIM_FALHAS_ESPERADAS");
        getLogger().info("Isolamento: erros de dependencia em evento e comando contidos, demais chamadas preservadas.");
    }

    public static final class FalhaSimuladaEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        public static HandlerList getHandlers() { return handlers; }
    }

    private void verificarMobs() {
        cn.nukkit.level.Level mundo = getServer().getDefaultLevel();
        cn.nukkit.level.Position spawn = mundo.getSafeSpawn();
        int x = spawn.getFloorX();
        int z = spawn.getFloorZ();
        int y = Math.min(120, spawn.getFloorY() + 3);
        cn.nukkit.level.format.FullChunk chunk = mundo.getChunk(x >> 4, z >> 4, true);
        for (int id : new int[]{32, 33, 34, 35}) {
            cn.nukkit.entity.Entity entidade = cn.nukkit.entity.Entity.createEntity(id, chunk, cn.nukkit.level.MobSpawning.createNBT(x + 0.5, y, z + 0.5));
            exigir(entidade instanceof cn.nukkit.entity.mob.EntityMob && entidade.getNetworkId() == id, "Registro de mob incorreto: " + id);
            exigir(entidade.getWidth() > 0 && entidade.getHeight() > 0, "Mob sem volume de colisao.");
            entidade.onUpdate(getServer().getTick() + 1);
            entidade.saveNBT();
            exigir(!entidade.namedTag.getString("id").isEmpty(), "Mob sem identidade persistente.");
            entidade.close();
        }
        cn.nukkit.math.Vector3 posicao = new cn.nukkit.math.Vector3(x + 2, y, z + 2);
        cn.nukkit.block.Block anterior = mundo.getBlock(posicao);
        mundo.setBlock(posicao, cn.nukkit.block.Block.get(cn.nukkit.block.Block.MONSTER_SPAWNER), true, true);
        cn.nukkit.nbt.tag.CompoundTag nbt = new cn.nukkit.nbt.tag.CompoundTag().putString("id", "MobSpawner")
                .putInt("x", x + 2).putInt("y", y).putInt("z", z + 2).putInt("EntityId", 34).putInt("Delay", 0);
        cn.nukkit.blockentity.BlockEntity bloco = cn.nukkit.blockentity.BlockEntity.createBlockEntity("MobSpawner", mundo.getChunk((x + 2) >> 4, (z + 2) >> 4, true), nbt);
        exigir(bloco instanceof cn.nukkit.blockentity.BlockEntityMobSpawner, "Spawner nao registrado.");
        cn.nukkit.blockentity.BlockEntityMobSpawner spawner = (cn.nukkit.blockentity.BlockEntityMobSpawner) bloco;
        exigir(spawner.getEntityId() == 34 && spawner.setEntityId(35) && !spawner.setEntityId(999), "Identidade do spawner invalida.");
        spawner.saveNBT();
        exigir(spawner.namedTag.getString("id").equals("MobSpawner") && spawner.namedTag.getInt("EntityId") == 35, "Spawner nao preservou os dados.");
        for (int i = 0; i < 40; i++) { exigir(spawner.onUpdate(), "Spawner valido encerrou a atualizacao."); }
        spawner.close();
        mundo.setBlock(posicao, anterior, true, true);
        getLogger().info("Mobs: IDs, dimensoes, atualizacao, NBT e spawner sem jogador verificados.");
    }

    private void verificarComportamentoMobs() {
        cn.nukkit.level.Level mundo = getServer().getDefaultLevel();
        cn.nukkit.level.Position spawn = mundo.getSafeSpawn();
        int x = spawn.getFloorX();
        int z = spawn.getFloorZ();
        int y = 120;
        java.util.Map<cn.nukkit.math.Vector3, cn.nukkit.block.Block> pisoAnterior = new java.util.LinkedHashMap<>();
        JogadorSimulado jogador = new JogadorSimulado();
        jogador.x = x + 6.5;
        jogador.y = y;
        jogador.z = z + 0.5;
        mundo.getPlayers().put(-1L, jogador);
        cn.nukkit.blockentity.BlockEntityMobSpawner spawner = null;
        cn.nukkit.math.Vector3 posSpawner = new cn.nukkit.math.Vector3(x + 4, y, z);
        cn.nukkit.block.Block anteriorSpawner = mundo.getBlock(posSpawner);
        try {
            for (int dx = -4; dx <= 12; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    cn.nukkit.math.Vector3 pos = new cn.nukkit.math.Vector3(x + dx, y - 1, z + dz);
                    pisoAnterior.put(pos, mundo.getBlock(pos));
                    mundo.setBlock(pos, cn.nukkit.block.Block.get(cn.nukkit.block.Block.STONE), true, true);
                }
            }
            cn.nukkit.entity.Entity zumbi = cn.nukkit.entity.Entity.createEntity(32, mundo.getChunk(x >> 4, z >> 4, true), cn.nukkit.level.MobSpawning.createNBT(x + 0.5, y, z + 0.5));
            double distanciaAntes = zumbi.distanceSquared(jogador);
            for (int tick = 1; tick <= 80; tick++) { zumbi.onUpdate(getServer().getTick() + tick); }
            exigir(zumbi.distanceSquared(jogador) < distanciaAntes && jogador.dano > 0, "Zumbi nao perseguiu ou atacou o alvo simulado.");
            zumbi.close();
            verificarIAExtra(mundo, jogador, x, y, z);
            this.cancelarSpawn = true;
            exigir(cn.nukkit.level.MobSpawning.spawn(mundo, 32, x + 0.5, y, z + 0.5, cn.nukkit.event.entity.CreatureSpawnEvent.Reason.SPAWNER) == null, "Spawn cancelado por plugin foi aceito.");
            this.cancelarSpawn = false;
            cn.nukkit.entity.Entity permitido = cn.nukkit.level.MobSpawning.spawn(mundo, 32, x + 0.5, y, z + 0.5, cn.nukkit.event.entity.CreatureSpawnEvent.Reason.SPAWNER);
            exigir(permitido != null, "Spawn valido em piso livre foi recusado.");
            permitido.close();
            mundo.setBlock(posSpawner, cn.nukkit.block.Block.get(cn.nukkit.block.Block.MONSTER_SPAWNER), true, true);
            spawner = new cn.nukkit.blockentity.BlockEntityMobSpawner(mundo.getChunk((x + 4) >> 4, z >> 4, true),
                    new cn.nukkit.nbt.tag.CompoundTag().putString("id", "MobSpawner").putInt("x", x + 4).putInt("y", y).putInt("z", z).putInt("EntityId", 32).putInt("Delay", 0));
            for (int rodada = 0; rodada < 8; rodada++) {
                spawner.namedTag.putInt("Delay", 0);
                for (int tick = 0; tick < 20; tick++) { spawner.onUpdate(); }
            }
            int monstros = 0;
            for (cn.nukkit.entity.Entity entidade : mundo.getEntities()) { if (entidade instanceof cn.nukkit.entity.mob.EntityMob) { ++monstros; } }
            exigir(monstros > 0 && monstros <= 6, "Spawner nao gerou mobs ou excedeu o limite local.");
            getLogger().info("Mobs: perseguicao e ataque contra alvo simulado, cancelamento de spawn e limite do spawner verificados.");
        } finally {
            this.cancelarSpawn = false;
            mundo.getPlayers().remove(-1L);
            if (spawner != null) { spawner.close(); }
            for (cn.nukkit.entity.Entity entidade : mundo.getEntities()) { if (entidade instanceof cn.nukkit.entity.mob.EntityMob) { entidade.close(); } }
            mundo.setBlock(posSpawner, anteriorSpawner, true, true);
            for (java.util.Map.Entry<cn.nukkit.math.Vector3, cn.nukkit.block.Block> entrada : pisoAnterior.entrySet()) {
                mundo.setBlock(entrada.getKey(), entrada.getValue(), true, true);
            }
        }
    }

    private static Object alvo(cn.nukkit.entity.Entity mob) throws Exception {
        java.lang.reflect.Field campo = cn.nukkit.entity.mob.EntityMob.class.getDeclaredField("target");
        campo.setAccessible(true);
        return campo.get(mob);
    }

    private void verificarIAExtra(cn.nukkit.level.Level mundo, JogadorSimulado jogador, int x, int y, int z) {
        try {
            // Ticks consecutivos, como no servidor real (o knockback dura 10 ticks).
            int tick = getServer().getTick();
            // Knockback: o impulso do golpe nao pode ser apagado pela IA no tick seguinte.
            cn.nukkit.entity.Entity zumbi = cn.nukkit.entity.Entity.createEntity(32, mundo.getChunk(x >> 4, z >> 4, true), cn.nukkit.level.MobSpawning.createNBT(x + 3.5, y, z + 0.5));
            double antes = zumbi.distanceSquared(jogador);
            zumbi.attack(new cn.nukkit.event.entity.EntityDamageByEntityEvent(jogador, zumbi, cn.nukkit.event.entity.EntityDamageEvent.CAUSE_ENTITY_ATTACK, 1));
            for (int i = 1; i <= 3; i++) { zumbi.onUpdate(tick + i); }
            exigir(zumbi.distanceSquared(jogador) > antes, "Knockback foi anulado pela IA.");
            zumbi.close();
            // Despawn: a mais de 128 blocos de todos os jogadores o mob some.
            double jx = jogador.x;
            jogador.x = x + 300;
            cn.nukkit.entity.Entity longe = cn.nukkit.entity.Entity.createEntity(32, mundo.getChunk(x >> 4, z >> 4, true), cn.nukkit.level.MobSpawning.createNBT(x + 0.5, y, z + 0.5));
            int t = tick + 100 - ((tick + 100) % 20);
            longe.onUpdate(t);
            exigir(longe.closed, "Mob distante de todos os jogadores nao sumiu.");
            jogador.x = jx;
            // Aranha: neutra de dia e a ceu aberto; agredida, passa a perseguir quem bateu.
            int horario = mundo.getTime();
            mundo.setTime(1000);
            cn.nukkit.entity.Entity aranha = cn.nukkit.entity.Entity.createEntity(35, mundo.getChunk(x >> 4, z >> 4, true), cn.nukkit.level.MobSpawning.createNBT(x + 2.5, y, z + 0.5));
            for (int i = 1; i <= 20; i++) { aranha.onUpdate(tick + i); }
            exigir(alvo(aranha) == null, "Aranha atacou de dia sem ser provocada.");
            aranha.attack(new cn.nukkit.event.entity.EntityDamageByEntityEvent(jogador, aranha, cn.nukkit.event.entity.EntityDamageEvent.CAUSE_ENTITY_ATTACK, 1));
            for (int i = 21; i <= 40; i++) { aranha.onUpdate(tick + i); }
            exigir(alvo(aranha) == jogador, "Aranha agredida nao revidou.");
            aranha.close();
            mundo.setTime(horario);
            getLogger().info("Mobs: knockback preservado, despawn distante e neutralidade da aranha verificados.");
        } catch (RuntimeException erro) {
            throw erro;
        } catch (Exception erro) {
            throw new IllegalStateException(erro);
        }
    }

    private static final class JogadorSimulado extends cn.nukkit.Player {
        private float dano;
        private JogadorSimulado() { this("GoldTeste"); }
        private JogadorSimulado(String name) {
            super((cn.nukkit.network.SourceInterface) Proxy.newProxyInstance(cn.nukkit.network.SourceInterface.class.getClassLoader(), new Class<?>[]{cn.nukkit.network.SourceInterface.class}, (proxy, metodo, args) -> {
                if (metodo.getReturnType() == Integer.class || metodo.getReturnType() == int.class) { return 0; }
                if (metodo.getReturnType() == boolean.class) { return false; }
                return null;
            }), 1L, "127.0.0.1", 19133);
            this.spawned = true;
            this.username = name;
        }
        @Override public boolean isAlive() { return true; }
        @Override public boolean isConnected() { return this.connected; }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return false; }
        @Override public void attack(cn.nukkit.event.entity.EntityDamageEvent evento) {
            getServer().getPluginManager().callEvent(evento);
            if (!evento.isCancelled()) { this.dano += evento.getFinalDamage(); }
        }
        @Override public void close(cn.nukkit.lang.TextContainer mensagem, String motivo, boolean notificar) {
            this.connected = false;
            getServer().getPluginManager().callEvent(new cn.nukkit.event.player.PlayerQuitEvent(this, "", false, motivo));
        }
    }

    private java.util.concurrent.CompletableFuture<Void> verificarGoldCore() {
        br.goldsuite.api.CoreService core = getServer().getServiceManager().load(br.goldsuite.api.CoreService.class);
        if (core == null) { return java.util.concurrent.CompletableFuture.completedFuture(null); }
        return core.whenReady()
                .thenCompose(ignored -> core.getStore().put("integracao", "persistencia", "Proteção persistente"))
                .thenCompose(ignored -> core.getStore().get("integracao", "persistencia"))
                .thenAccept(valor -> {
                    exigir(Thread.currentThread().getId() == this.mainThreadId, "Conclusao do armazenamento ocorreu fora da thread principal.");
                    exigir(valor.orElse("").equals("Proteção persistente"), "Armazenamento assincrono nao preservou o valor.");
                    Item assinado = core.getItemSigner().sign(Item.get(Item.DIAMOND), "teste:chave");
                    exigir(core.getItemSigner().verify(assinado, "teste:chave"), "Assinatura do servico real falhou.");
                    assinado.setCount(64);
                    exigir(core.getItemSigner().verify(assinado, "teste:chave"), "Item assinado deixou de valer ao empilhar.");
                    assinado.setNamedTag(((cn.nukkit.nbt.tag.CompoundTag) assinado.getNamedTag().copy()).putInt("adulterado", 1));
                    exigir(!core.getItemSigner().verify(assinado, "teste:chave"), "Servico real aceitou item adulterado.");
                    getLogger().info("GoldCore: SQLite assincrono, retorno na thread principal e HMAC verificados.");
                });
    }

    private void verificarDrenagem() {
        br.goldsuite.api.CoreService core = getServer().getServiceManager().load(br.goldsuite.api.CoreService.class);
        if (core == null) { return; }
        // A escrita foi enfileirada neste tick: o desligamento precisa drena-la sem outro heartbeat.
        core.getStore().put("integracao", "desligamento", "gravado");
        getServer().getPluginManager().disablePlugin(getServer().getPluginManager().getPlugin("GoldCore"));
        exigir(getServer().getServiceManager().load(br.goldsuite.api.CoreService.class) == null, "GoldCore desativado permaneceu registrado.");
        exigir(core.getStore().get("integracao", "persistencia").isCompletedExceptionally(), "Armazenamento desativado aceitou nova operacao.");
        getLogger().info("GoldCore: drenagem ao desativar verificada.");
    }

    private java.util.concurrent.CompletableFuture<Void> aguardar(java.util.function.BooleanSupplier condicao) {
        java.util.concurrent.CompletableFuture<Void> resultado = new java.util.concurrent.CompletableFuture<>();
        getServer().getScheduler().scheduleRepeatingTask(new cn.nukkit.scheduler.PluginTask<CoreRegressionPlugin>(this) {
            private int ticks;
            @Override public void onRun(int tick) {
                if (condicao.getAsBoolean()) { resultado.complete(null); cancel(); }
                else if (++this.ticks > 200) { resultado.completeExceptionally(new AssertionError("Condicao assincrona nao foi atingida.")); cancel(); }
            }
        }, 1);
        return resultado;
    }

    private java.util.concurrent.CompletableFuture<Void> verificarAgendador() {
        java.util.concurrent.CompletableFuture<Void> concluido = new java.util.concurrent.CompletableFuture<>();
        AtomicInteger chamadas = new AtomicInteger();
        AtomicInteger cancelamentos = new AtomicInteger();
        cn.nukkit.scheduler.PluginTask<CoreRegressionPlugin> tarefa = new cn.nukkit.scheduler.PluginTask<CoreRegressionPlugin>(this) {
            @Override public void onRun(int tick) { if (chamadas.incrementAndGet() == 2) { cancel(); } }
            @Override public void onCancel() { cancelamentos.incrementAndGet(); concluido.complete(null); }
        };
        cn.nukkit.scheduler.TaskHandler handler = getServer().getScheduler().scheduleRepeatingTask(tarefa, 1);
        exigir(tarefa.getHandler() == handler, "Tarefa nao recebeu seu handler.");
        return concluido.thenRun(() -> {
            handler.cancel();
            exigir(chamadas.get() == 2 && cancelamentos.get() == 1, "Cancelamento nao foi idempotente.");
            getLogger().info("Agendador: handler, cancelamento pela tarefa e onCancel unico verificados.");
        });
    }

    private java.util.concurrent.CompletableFuture<Void> verificarGoldAuth() {
        Plugin plugin = getServer().getPluginManager().getPlugin("GoldAuth");
        if (plugin == null) { return java.util.concurrent.CompletableFuture.completedFuture(null); }
        getServer().getPluginManager().enablePlugin(plugin);
        br.goldsuite.api.AuthService auth = getServer().getServiceManager().load(br.goldsuite.api.AuthService.class);
        br.goldsuite.api.CoreService core = getServer().getServiceManager().load(br.goldsuite.api.CoreService.class);
        exigir(auth != null && core != null, "Servicos de autenticacao ausentes.");
        JogadorSimulado primeiro = new JogadorSimulado("GoldTesteAuth");
        JogadorSimulado segundo = new JogadorSimulado("goldtesteauth");
        JogadorSimulado terceiro = new JogadorSimulado("GoldTesteAuth");
        JogadorSimulado quarto = new JogadorSimulado("GoldTesteAuth");
        JogadorSimulado quinto = new JogadorSimulado("GoldTesteAuth");
        AtomicInteger eventosAuth = new AtomicInteger();
        getServer().getPluginManager().registerEvent(br.goldsuite.api.event.PlayerAuthEvent.class, new Listener() { }, EventPriority.MONITOR,
                (listener, event) -> { exigir(Thread.currentThread().getId() == mainThreadId, "Evento de autenticacao fora da thread principal."); eventosAuth.incrementAndGet(); }, this);
        return core.getStore().delete("goldauth", "goldtesteauth").thenCompose(ignored -> {
            getServer().getPluginManager().callEvent(new cn.nukkit.event.player.PlayerJoinEvent(primeiro, ""));
            cn.nukkit.event.player.PlayerMoveEvent movimento = new cn.nukkit.event.player.PlayerMoveEvent(primeiro,
                    new cn.nukkit.level.Location(0, 120, 0), new cn.nukkit.level.Location(1, 120, 0));
            getServer().getPluginManager().callEvent(movimento);
            exigir(movimento.isCancelled(), "Jogador sem login conseguiu se mover.");
            cn.nukkit.event.player.PlayerCommandPreprocessEvent comando = new cn.nukkit.event.player.PlayerCommandPreprocessEvent(primeiro, "/OP invasor");
            getServer().getPluginManager().callEvent(comando);
            exigir(comando.isCancelled(), "Comando administrativo passou antes do login.");
            cn.nukkit.event.server.DataPacketReceiveEvent pacote = new cn.nukkit.event.server.DataPacketReceiveEvent(primeiro, new cn.nukkit.network.protocol.ItemFrameDropItemPacket());
            getServer().getPluginManager().callEvent(pacote);
            exigir(pacote.isCancelled(), "Pacote mutavel passou antes do login.");
            return auth.register(primeiro, "SenhaTesteSegura123".toCharArray());
        }).thenCompose(registrado -> {
            exigir(registrado && auth.isAuthenticated(primeiro), "Registro nao autenticou o jogador.");
            cn.nukkit.event.player.PlayerPreLoginEvent duplicado = new cn.nukkit.event.player.PlayerPreLoginEvent(segundo, "");
            getServer().getPluginManager().callEvent(duplicado);
            exigir(duplicado.isCancelled(), "Uma conexao conseguiu substituir a conta autenticada.");
            getServer().getPluginManager().callEvent(new cn.nukkit.event.player.PlayerQuitEvent(primeiro, "", false));
            exigir(!auth.isAuthenticated(primeiro), "Sessao antiga permaneceu autenticada.");
            getServer().getPluginManager().callEvent(new cn.nukkit.event.player.PlayerJoinEvent(segundo, ""));
            return auth.login(segundo, "SenhaIncorreta123".toCharArray());
        }).thenCompose(aceito -> {
            exigir(!aceito && !auth.isAuthenticated(segundo), "Senha incorreta autenticou a conta.");
            return auth.login(segundo, "SenhaTesteSegura123".toCharArray());
        }).thenCompose(aceito -> {
            exigir(aceito && auth.isAuthenticated(segundo), "Login correto recusado.");
            return auth.changePassword(segundo, "SenhaTesteSegura123".toCharArray(), "NovaSenhaSegura456".toCharArray());
        }).thenCompose(alterado -> {
            exigir(alterado, "Troca de senha falhou.");
            getServer().getPluginManager().callEvent(new cn.nukkit.event.player.PlayerQuitEvent(segundo, "", false));
            getServer().getPluginManager().callEvent(new cn.nukkit.event.player.PlayerJoinEvent(terceiro, ""));
            return auth.login(terceiro, "SenhaTesteSegura123".toCharArray());
        }).thenCompose(aceito -> {
            exigir(!aceito, "Senha antiga continuou valida apos a troca.");
            return auth.login(terceiro, "NovaSenhaSegura456".toCharArray());
        }).thenCompose(aceito -> {
            exigir(aceito, "Senha nova foi recusada.");
            getServer().getPluginManager().callEvent(new cn.nukkit.event.player.PlayerQuitEvent(terceiro, "", false));
            getServer().getPluginManager().callEvent(new cn.nukkit.event.player.PlayerJoinEvent(quarto, ""));
            return auth.register(quarto, "NaoSobrescrever123".toCharArray());
        }).thenCompose(aceito -> {
            exigir(!aceito && !auth.isAuthenticated(quarto), "Registro existente foi sobrescrito.");
            cn.nukkit.event.player.PlayerChatEvent chat = new cn.nukkit.event.player.PlayerChatEvent(quarto, "NovaSenhaSegura456");
            getServer().getPluginManager().callEvent(chat);
            exigir(chat.isCancelled() && chat.getMessage().isEmpty(), "Senha do chat nao foi retirada da mensagem.");
            return aguardar(() -> auth.isAuthenticated(quarto));
        }).thenCompose(ignored -> {
            exigir(eventosAuth.get() == 4, "Evento de autenticacao ausente ou duplicado.");
            getServer().getPluginManager().callEvent(new cn.nukkit.event.player.PlayerQuitEvent(quarto, "", false));
            getServer().getPluginManager().callEvent(new cn.nukkit.event.player.PlayerJoinEvent(quinto, ""));
            java.util.concurrent.CompletableFuture<Boolean> tentativas = java.util.concurrent.CompletableFuture.completedFuture(false);
            for (int tentativa = 0; tentativa < 5; tentativa++) {
                tentativas = tentativas.thenCompose(ignorado -> auth.login(quinto, "SenhaSempreErrada123".toCharArray()));
            }
            return tentativas;
        }).thenCompose(aceito -> {
            exigir(!aceito && !quinto.isConnected() && !auth.isAuthenticated(quinto), "Cinco senhas incorretas nao encerraram a sessao.");
            exigir(getServer().dispatchCommand(getServer().getConsoleSender(), "authreset GoldTesteAuth"), "Reset administrativo nao foi executado.");
            return core.getStore().get("goldauth", "goldtesteauth");
        }).thenAccept(registro -> {
            exigir(!registro.isPresent() && !auth.isAuthenticated(quarto), "Reset manteve registro ou sessao autenticada.");
            getLogger().info("GoldAuth: registro, login incorreto/correto, reconexao, troca, chat privado, bloqueios e reset verificados.");
        });
    }

    private static final class InventarioTeste extends BaseInventory {
        private int cancelarSlot = -1;

        private InventarioTeste() { super(null, InventoryType.CHEST); }

        @Override
        protected Item prepareTransactionSlot(int index, Item item) {
            return index == this.cancelarSlot ? null : super.prepareTransactionSlot(index, item);
        }
    }
}
