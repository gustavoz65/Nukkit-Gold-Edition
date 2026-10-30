package br.gold;

import br.goldsuite.api.*;
import br.goldsuite.api.EconomyService.Currency;
import cn.nukkit.Server;
import java.util.concurrent.CompletableFuture;

/** Exercita APIs carregadas pelos jars reais, com SQLite e conclusoes na thread principal. */
final class SuiteRegression {
    static CompletableFuture<Void> run(Server server) {
        boolean economyInstalled = server.getPluginManager().getPlugin("GoldEconomy") != null;
        boolean permsInstalled = server.getPluginManager().getPlugin("GoldPerms") != null;
        if (!economyInstalled && !permsInstalled) { server.getLogger().info("GoldPerms e GoldEconomy ausentes; integracao ignorada."); return CompletableFuture.completedFuture(null); }
        check(economyInstalled && permsInstalled, "Instale os dois modulos para testar a integracao.");
        // Os servicos so sao registrados apos a carga assincrona de cada modulo.
        return awaitServices(server, 200).thenCompose(ignored -> exercise(server));
    }
    private static CompletableFuture<Void> awaitServices(Server server, int ticksLeft) {
        if (server.getServiceManager().load(EconomyService.class) != null && server.getServiceManager().load(PermissionService.class) != null) { return CompletableFuture.completedFuture(null); }
        if (ticksLeft <= 0) { throw new AssertionError("GoldPerms ou GoldEconomy nao registraram o servico."); }
        CompletableFuture<Void> next = new CompletableFuture<>();
        server.getScheduler().scheduleDelayedTask(() -> {
            try { awaitServices(server, ticksLeft - 1).whenComplete((v, e) -> { if (e == null) { next.complete(null); } else { next.completeExceptionally(e); } }); }
            catch (Throwable error) { next.completeExceptionally(error); }
        }, 1);
        return next;
    }
    private static CompletableFuture<Void> exercise(Server server) {
        EconomyService economy = server.getServiceManager().load(EconomyService.class);
        PermissionService permissions = server.getServiceManager().load(PermissionService.class);
        Thread thread = Thread.currentThread();
        CompletableFuture<Void> start = CompletableFuture.completedFuture(null);
        for (String name : new String[] {"EcoTesteA", "EcoTesteB"}) {
            long balance = economy.balance(name, Currency.MONEY);
            if (balance > 0) { start = start.thenCompose(unused -> economy.debit(name, Currency.MONEY, balance)); }
        }
        return start.thenCompose(unused -> permissions.setGroup("EcoTesteA", "padrao", 0))
            .thenCompose(unused -> permissions.setGroup("EcoTesteA", "inexistente", 0).handle((result, error) -> {
                check(error != null && permissions.getGroup("EcoTesteA").equals("padrao"), "Grupo inexistente alterou permissoes."); return null;
            }))
            .thenCompose(unused -> economy.credit("EcoTesteA", Currency.MONEY, 10000))
            .thenCompose(unused -> economy.transfer("EcoTesteA", "EcoTesteB", Currency.MONEY, 4000))
            .thenCompose(unused -> {
                check(Thread.currentThread() == thread, "Conclusao de economia fora da thread principal.");
                check(economy.balance("ECOTESTEA", Currency.MONEY) == 6000 && economy.balance("ecotesteb", Currency.MONEY) == 4000, "Transferencia SQLite incorreta.");
                CompletableFuture<Void> first = economy.transfer("EcoTesteA", "EcoTesteB", Currency.MONEY, 6000);
                CompletableFuture<Void> second = economy.transfer("EcoTesteA", "EcoTesteB", Currency.MONEY, 6000);
                return first.thenCompose(ignored -> second.handle((result, error) -> { check(error != null, "Gasto duplo aceito."); return null; }));
            })
            .thenCompose(unused -> {
                check(economy.balance("EcoTesteA", Currency.MONEY) == 0 && economy.balance("EcoTesteB", Currency.MONEY) == 10000, "Gasto duplo perdeu ou criou moedas.");
                return server.getServiceManager().load(CoreService.class).getStore().get("goldeconomy", "estado");
            })
            .thenAccept(stored -> {
                check(stored.isPresent() && stored.get().contains("ecotesteb"), "Economia nao foi persistida.");
                check(Thread.currentThread() == thread, "Leitura retornou fora da thread principal.");
                server.getLogger().info("GOLDPERMS_E_ECONOMIA_INTEGRADOS_APROVADOS");
            });
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
