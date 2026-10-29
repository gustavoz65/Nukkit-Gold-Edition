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

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleDelayedTask(() -> {
            try {
                verificarInventario();
                verificarServicos();
                verificarIsolamento();
                getLogger().info("VERIFICACOES_INTEGRADAS_APROVADAS");
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
        getLogger().info("FIM_FALHAS_ESPERADAS");
        getLogger().info("Isolamento: erros de dependencia em evento e comando contidos, demais chamadas preservadas.");
    }

    public static final class FalhaSimuladaEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        public static HandlerList getHandlers() { return handlers; }
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
