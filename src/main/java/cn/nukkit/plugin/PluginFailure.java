package cn.nukkit.plugin;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Contem erros do plugin sem ocultar falhas fatais da propria JVM. */
public final class PluginFailure {
    private PluginFailure() { }

    public static void rethrowIfFatal(Throwable error) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        for (Throwable current = error; current != null && visited.add(current); current = current.getCause()) {
            if (current instanceof VirtualMachineError) { throw (VirtualMachineError) current; }
            if (current instanceof ThreadDeath) { throw (ThreadDeath) current; }
        }
    }
}
