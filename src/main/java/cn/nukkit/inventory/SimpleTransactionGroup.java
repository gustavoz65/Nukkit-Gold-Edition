package cn.nukkit.inventory;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.item.Item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class SimpleTransactionGroup implements TransactionGroup {

    private final long creationTime;
    protected boolean hasExecuted = false;

    protected Player source = null;

    protected final Set<Inventory> inventories = new HashSet<>();

    protected final Set<Transaction> transactions = new HashSet<>();

    public SimpleTransactionGroup() {
        this(null);
    }

    public SimpleTransactionGroup(Player source) {
        this.creationTime = System.currentTimeMillis();
        this.source = source;
    }

    public Player getSource() {
        return source;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public Set<Inventory> getInventories() {
        return inventories;
    }

    @Override
    public Set<Transaction> getTransactions() {
        return transactions;
    }

    @Override
    public void addTransaction(Transaction transaction) {
        if (this.transactions.contains(transaction)) {
            return;
        }

        for (Transaction tx : new HashSet<>(this.transactions)) {
            if (tx.getInventory().equals(transaction.getInventory()) && tx.getSlot() == transaction.getSlot()) {
                if (transaction.getCreationTime() >= tx.getCreationTime()) {
                    this.transactions.remove(tx);
                } else {
                    return;
                }
            }
        }

        this.transactions.add(transaction);
        this.inventories.add(transaction.getInventory());
    }

    protected boolean matchItems(List<Item> needItems, List<Item> haveItems) {
        for (Transaction ts : this.transactions) {
            if (!(ts.getInventory() instanceof BaseInventory) || ts.getSlot() < 0
                    || ts.getSlot() >= ((BaseInventory) ts.getInventory()).size
                    || !validItem(ts.getSourceItem()) || !validItem(ts.getTargetItem())) {
                return false;
            }
            if (ts.getTargetItem().getId() != Item.AIR) {
                needItems.add(ts.getTargetItem());
            }
            Item checkSourceItem = ts.getInventory().getItem(ts.getSlot());
            Item sourceItem = ts.getSourceItem();
            if (!checkSourceItem.deepEquals(sourceItem) || sourceItem.getCount() != checkSourceItem.getCount()) {
                return false;
            }
            if (sourceItem.getId() != Item.AIR) {
                haveItems.add(sourceItem);
            }
        }

        for (Item needItem : new ArrayList<>(needItems)) {
            for (Item haveItem : new ArrayList<>(haveItems)) {
                if (needItem.deepEquals(haveItem)) {
                    int amount = Math.min(haveItem.getCount(), needItem.getCount());
                    needItem.setCount(needItem.getCount() - amount);
                    haveItem.setCount(haveItem.getCount() - amount);
                    if (haveItem.getCount() == 0) {
                        haveItems.remove(haveItem);
                    }
                    if (needItem.getCount() == 0) {
                        needItems.remove(needItem);
                        break;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean canExecute() {
        List<Item> haveItems = new ArrayList<>();
        List<Item> needItems = new ArrayList<>();

        return this.matchItems(needItems, haveItems) && haveItems.isEmpty() && needItems.isEmpty() && !this.transactions.isEmpty();
    }

    @Override
    public boolean execute() {
        return execute(false);
    }

    @Override
    public boolean execute(boolean force) {
        if (this.hasExecuted || !this.matchItems(new ArrayList<>(), new ArrayList<>()) || (!force && !this.canExecute())) {
            return false;
        }

        InventoryTransactionEvent ev = new InventoryTransactionEvent(this);
        Server.getInstance().getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            this.resendInventories();
            return false;
        }

        Map<Transaction, Item> prepared = new LinkedHashMap<>();
        for (Transaction transaction : this.transactions) {
            BaseInventory inventory = (BaseInventory) transaction.getInventory();
            Item item = inventory.prepareTransactionSlot(transaction.getSlot(), transaction.getTargetItem());
            if (item == null || !validItem(item)) {
                this.resendInventories();
                return false;
            }
            prepared.put(transaction, item);
        }
        // Os listeners podem alterar os inventarios durante a preparacao.
        if (!this.matchItems(new ArrayList<>(), new ArrayList<>()) || (!force && !this.canExecute())) {
            this.resendInventories();
            return false;
        }
        // Primeiro grava todos os slots; so depois envia notificacoes aos observadores.
        for (Map.Entry<Transaction, Item> entry : prepared.entrySet()) {
            Transaction transaction = entry.getKey();
            BaseInventory inventory = (BaseInventory) transaction.getInventory();
            Item item = entry.getValue();
            if (item.getId() == Item.AIR || item.getCount() <= 0) {
                inventory.slots.remove(transaction.getSlot());
            } else {
                inventory.slots.put(transaction.getSlot(), item.clone());
            }
        }

        this.hasExecuted = true;
        for (Transaction transaction : prepared.keySet()) {
            transaction.getInventory().onSlotChange(transaction.getSlot(), transaction.getSourceItem());
        }

        return true;
    }

    @Override
    public boolean hasExecuted() {
        return this.hasExecuted;
    }

    private static boolean validItem(Item item) {
        return item != null && item.getCount() >= 0
                && (item.getId() == Item.AIR || (item.getCount() > 0 && item.getCount() <= item.getMaxStackSize()));
    }

    private void resendInventories() {
        if (this.source == null) { return; }
        for (Inventory inventory : this.inventories) {
            if (inventory instanceof PlayerInventory) {
                ((PlayerInventory) inventory).sendArmorContents(this.source);
            }
            inventory.sendContents(this.source);
        }
    }
}
