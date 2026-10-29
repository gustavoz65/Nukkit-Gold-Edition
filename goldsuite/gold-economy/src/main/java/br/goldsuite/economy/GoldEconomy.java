package br.goldsuite.economy;

import br.goldsuite.api.*;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class GoldEconomy extends PluginBase implements EconomyService {
    private PersistentState<Ledger> data;
    @Override public void onEnable() {
        CoreService core = getServer().getServiceManager().load(CoreService.class);
        data = new PersistentState<>(core.getStore(), "goldeconomy", Ledger.class, Ledger::new, Ledger::validate);
        data.ready().whenComplete((unused, error) -> {
            if (!isEnabled()) { return; }
            if (error != null) { getLogger().error("Falha ao carregar economia.", error); getServer().getPluginManager().disablePlugin(this); }
            else { getServer().getServiceManager().register(EconomyService.class, this, this); }
        });
    }
    @Override public void onDisable() { if (data != null) { data.close(); } }
    @Override public long balance(String player, Currency currency) { return data.snapshot().balance(player, Objects.requireNonNull(currency)); }
    @Override public CompletableFuture<Void> credit(String player, Currency currency, long amount) { Objects.requireNonNull(currency); return data.change(s -> s.credit(player, currency, amount)); }
    @Override public CompletableFuture<Void> debit(String player, Currency currency, long amount) { Objects.requireNonNull(currency); return data.change(s -> s.debit(player, currency, amount)); }
    @Override public CompletableFuture<Void> transfer(String from, String to, Currency currency, long amount) { Objects.requireNonNull(currency); return data.change(s -> s.transfer(from, to, currency, amount)); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        AuthService auth = getServer().getServiceManager().load(AuthService.class);
        if (sender instanceof Player && auth != null && !auth.isAuthenticated((Player) sender)) { sender.sendMessage("Autentique-se primeiro."); return true; }
        try {
            Currency currency = command.getName().equals("cash") ? Currency.CASH : Currency.MONEY;
            CompletableFuture<Void> operation;
            switch (command.getName()) {
                case "money": case "cash":
                    String target = args.length == 0 ? sender.getName() : args[0];
                    sender.sendMessage("Saldo de " + target + ": " + Ledger.format(balance(target, currency), currency) + (currency == Currency.CASH ? " cash" : " moedas")); return true;
                case "topmoney":
                    List<Map.Entry<String, Ledger.Account>> accounts = new ArrayList<>(data.snapshot().accounts.entrySet());
                    accounts.sort(Comparator.<Map.Entry<String, Ledger.Account>>comparingLong(e -> e.getValue().money).reversed().thenComparing(Map.Entry::getKey));
                    sender.sendMessage("Maiores saldos:");
                    for (int i = 0; i < Math.min(10, accounts.size()); i++) { sender.sendMessage((i + 1) + ". " + accounts.get(i).getKey() + ": " + Ledger.format(accounts.get(i).getValue().money, Currency.MONEY)); }
                    return true;
                case "pagar":
                    if (!(sender instanceof Player) || args.length != 2) { return false; }
                    operation = transfer(sender.getName(), args[0], Currency.MONEY, Ledger.parse(args[1], Currency.MONEY)); break;
                case "trocarcash":
                    if (!(sender instanceof Player) || args.length != 1) { return false; }
                    long amount = Ledger.parse(args[0], Currency.CASH);
                    operation = data.change(s -> s.exchange(sender.getName(), amount)); break;
                case "economia":
                    if (!sender.hasPermission("goldeconomy.admin")) { sender.sendMessage("Sem permissao."); return true; }
                    if (args.length != 4 || !Arrays.asList("money", "cash").contains(args[1])) { return false; }
                    Currency selected = args[1].equals("cash") ? Currency.CASH : Currency.MONEY;
                    long value = args[3].equals("0") && args[0].equals("definir") ? 0 : Ledger.parse(args[3], selected);
                    switch (args[0]) {
                        case "adicionar": operation = credit(args[2], selected, value); break;
                        case "retirar": operation = debit(args[2], selected, value); break;
                        case "definir": operation = data.change(s -> s.set(args[2], selected, value)); break;
                        default: return false;
                    }
                    break;
                default: return false;
            }
            operation.whenComplete((unused, error) -> { if (isEnabled()) { sender.sendMessage(error == null ? "Operacao salva com sucesso." : "Operacao recusada: " + message(error)); } });
        } catch (RuntimeException error) { sender.sendMessage("Operacao recusada: " + message(error)); }
        return true;
    }
    private static String message(Throwable error) { while (error.getCause() != null) { error = error.getCause(); } return error.getMessage(); }
}
