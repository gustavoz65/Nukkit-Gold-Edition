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

/** Executado apenas no servidor isolado de testes, nunca no jar de distribuicao. */
public final class CoreRegressionPlugin extends PluginBase implements Listener {
    private boolean cancelar;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleDelayedTask(() -> {
            try {
                verificarInventario();
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

    private static final class InventarioTeste extends BaseInventory {
        private int cancelarSlot = -1;

        private InventarioTeste() { super(null, InventoryType.CHEST); }

        @Override
        protected Item prepareTransactionSlot(int index, Item item) {
            return index == this.cancelarSlot ? null : super.prepareTransactionSlot(index, item);
        }
    }
}
