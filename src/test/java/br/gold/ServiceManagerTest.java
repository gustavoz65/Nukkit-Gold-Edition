package br.gold;

import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.ServiceManager;
import cn.nukkit.plugin.ServicePriority;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServiceManagerTest {
    private static Plugin plugin(AtomicBoolean ativo) {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class}, (proxy, method, args) -> {
            if (method.getName().equals("isEnabled")) { return ativo.get(); }
            if (method.getName().equals("equals")) { return proxy == args[0]; }
            if (method.getName().equals("hashCode")) { return System.identityHashCode(proxy); }
            return null;
        });
    }

    public static void main(String[] args) {
        ServiceManager manager = new ServiceManager();
        AtomicBoolean ativo = new AtomicBoolean(true);
        Plugin base = plugin(new AtomicBoolean(true));
        Plugin prioritario = plugin(ativo);
        manager.register(CharSequence.class, "base", base);
        manager.register(CharSequence.class, "prioridade", prioritario, ServicePriority.HIGH);
        if (!"prioridade".equals(manager.load(CharSequence.class))) { throw new AssertionError("Prioridade ignorada."); }
        ativo.set(false);
        if (!"base".equals(manager.load(CharSequence.class))) { throw new AssertionError("Plugin desativado continuou visivel."); }
        ativo.set(true);
        manager.unregisterAll(prioritario);
        if (!"base".equals(manager.load(CharSequence.class))) { throw new AssertionError("Limpeza do plugin falhou."); }
        manager.register(CharSequence.class, "substituto", base);
        if (!"substituto".equals(manager.load(CharSequence.class))) { throw new AssertionError("Substituicao do registro falhou."); }
        try {
            manager.getKnownServices().clear();
            throw new AssertionError("Colecao interna exposta para modificacao.");
        } catch (UnsupportedOperationException esperado) { }
        manager.unregisterAll(base);
        if (manager.load(CharSequence.class) != null || !manager.getKnownServices().isEmpty()) {
            throw new AssertionError("Servico removido ainda existe.");
        }
        System.out.println("ServiceManagerTest: prioridade, substituicao, desativacao e limpeza verificados.");
    }
}
