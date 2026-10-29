package cn.nukkit.plugin;

/** Registro imutavel de uma implementacao e do plugin responsavel por ela. */
public final class RegisteredServiceProvider<T> {
    private final Class<T> service;
    private final T provider;
    private final Plugin plugin;
    private final ServicePriority priority;

    RegisteredServiceProvider(Class<T> service, T provider, Plugin plugin, ServicePriority priority) {
        this.service = service;
        this.provider = provider;
        this.plugin = plugin;
        this.priority = priority;
    }

    public Class<T> getService() { return this.service; }
    public T getProvider() { return this.provider; }
    public Plugin getPlugin() { return this.plugin; }
    public ServicePriority getPriority() { return this.priority; }
}
