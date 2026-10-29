package cn.nukkit.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Registro de interfaces compartilhadas, sem dependencia obrigatoria entre plugins. */
public final class ServiceManager {
    private final Map<Class<?>, List<RegisteredServiceProvider<?>>> services = new LinkedHashMap<>();

    public <T> void register(Class<T> service, T provider, Plugin plugin) {
        register(service, provider, plugin, ServicePriority.NORMAL);
    }

    public synchronized <T> void register(Class<T> service, T provider, Plugin plugin, ServicePriority priority) {
        Objects.requireNonNull(service, "A interface do servico e obrigatoria.");
        Objects.requireNonNull(provider, "A implementacao do servico e obrigatoria.");
        Objects.requireNonNull(plugin, "O plugin responsavel e obrigatorio.");
        Objects.requireNonNull(priority, "A prioridade e obrigatoria.");
        if (!service.isInstance(provider) || !plugin.isEnabled()) {
            throw new IllegalArgumentException("O servico precisa de uma implementacao compativel e de um plugin ativo.");
        }
        List<RegisteredServiceProvider<?>> entries = this.services.computeIfAbsent(service, key -> new ArrayList<>());
        // Um registro do mesmo plugin substitui a implementacao anterior desse tipo.
        entries.removeIf(entry -> entry.getPlugin() == plugin);
        entries.add(new RegisteredServiceProvider<>(service, provider, plugin, priority));
        entries.sort((left, right) -> right.getPriority().compareTo(left.getPriority()));
    }

    public synchronized <T> RegisteredServiceProvider<T> getRegistration(Class<T> service) {
        List<RegisteredServiceProvider<?>> entries = this.services.get(service);
        if (entries != null) {
            for (RegisteredServiceProvider<?> entry : entries) {
                if (entry.getPlugin().isEnabled()) {
                    @SuppressWarnings("unchecked")
                    RegisteredServiceProvider<T> selected = (RegisteredServiceProvider<T>) entry;
                    return selected;
                }
            }
        }
        return null;
    }

    public <T> T load(Class<T> service) {
        RegisteredServiceProvider<T> entry = getRegistration(service);
        return entry == null ? null : entry.getProvider();
    }

    public synchronized void unregister(Class<?> service, Plugin plugin) {
        List<RegisteredServiceProvider<?>> entries = this.services.get(service);
        if (entries != null) {
            entries.removeIf(entry -> entry.getPlugin() == plugin);
            if (entries.isEmpty()) { this.services.remove(service); }
        }
    }

    public synchronized void unregisterAll(Plugin plugin) {
        for (Class<?> service : new ArrayList<>(this.services.keySet())) { unregister(service, plugin); }
    }

    public synchronized void clear() { this.services.clear(); }

    public synchronized Set<Class<?>> getKnownServices() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(this.services.keySet()));
    }
}
