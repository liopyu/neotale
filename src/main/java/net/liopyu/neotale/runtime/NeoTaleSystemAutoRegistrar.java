package net.liopyu.neotale.runtime;

import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.liopyu.neotale.api.events.EventBusSubscriber;
import net.liopyu.neotale.api.system.SubscribeSystem;
import net.liopyu.neotale.api.system.SystemStore;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NeoTaleSystemAutoRegistrar {

    private static final Set<Class<?>> REGISTERED = new HashSet<>();

    public static void registerSystems(JavaPlugin plugin, Class<?>[] discoveredClasses) {
        List<Method> methods = new ArrayList<>();

        for (Class<?> c : discoveredClasses) {
            if (c.getAnnotation(EventBusSubscriber.class) == null) continue;
            if (!REGISTERED.add(c)) continue;

            for (Method m : c.getDeclaredMethods()) {
                if (m.getAnnotation(SubscribeSystem.class) == null) continue;
                int mod = m.getModifiers();
                if (!Modifier.isPublic(mod) || !Modifier.isStatic(mod)) continue;

                Class<?> rt = m.getReturnType();
                if (rt == Void.TYPE) continue;

                Class<?>[] ps = m.getParameterTypes();
                if (!(ps.length == 0 || (ps.length == 1 && ps[0].isAssignableFrom(plugin.getClass())) || (ps.length == 1 && ps[0].getName().equals("com.hypixel.hytale.server.core.plugin.JavaPlugin")))) {
                    continue;
                }

                methods.add(m);
            }
        }

        methods.sort(Comparator.comparingInt(m -> m.getAnnotation(SubscribeSystem.class).priority()));

        for (Method m : methods) {
            try {
                Object sys = (m.getParameterCount() == 0) ? m.invoke(null) : m.invoke(null, plugin);
                if (sys == null) return;

                if (!(sys instanceof ISystem<?> s)) {
                    continue;
                }

                SubscribeSystem meta = m.getAnnotation(SubscribeSystem.class);
                if (meta.store() == SystemStore.CHUNK) {
                    plugin.getChunkStoreRegistry().registerSystem((ISystem<ChunkStore>) s);
                } else {
                    plugin.getEntityStoreRegistry().registerSystem((ISystem<EntityStore>) s);
                }

            } catch (Throwable ignored) {
            }
        }
    }
}
