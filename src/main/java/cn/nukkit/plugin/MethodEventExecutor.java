package cn.nukkit.plugin;

import cn.nukkit.event.Event;
import cn.nukkit.event.Listener;
import cn.nukkit.utils.EventException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class MethodEventExecutor implements EventExecutor {

    private final Method method;

    public MethodEventExecutor(Method method) {
        this.method = method;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(Listener listener, Event event) throws EventException {
        try {
            Class<Event>[] params = (Class<Event>[]) method.getParameterTypes();
            for (Class<Event> param : params) {
                if (param.isAssignableFrom(event.getClass())) {
                    method.invoke(listener, event);
                    break;
                }
            }
        } catch (InvocationTargetException ex) {
            PluginFailure.rethrowIfFatal(ex.getCause());
            throw new EventException(ex.getCause());
        } catch (ClassCastException ex) {
            // We are going to ignore ClassCastException because EntityDamageEvent can't be cast to EntityDamageByEntityEvent
        } catch (Throwable t) {
            PluginFailure.rethrowIfFatal(t);
            throw new EventException(t);
        }
    }

    public Method getMethod() {
        return method;
    }
}
