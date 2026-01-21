package net.liopyu.neotale.runtime;

import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.liopyu.neotale.api.eventbus.EventBusSubscriber;
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

    private static final Set<String> REGISTERED = new HashSet<>();


    public static void registerSystems(JavaPlugin plugin, Class<?>[] discoveredClasses) {
        String key = plugin.getIdentifier().toString() + ":" + System.identityHashCode(plugin.getClassLoader());

        List<Method> methods = new ArrayList<>();

        for (int i = 0; i < discoveredClasses.length; i++) {
            Class<?> c = discoveredClasses[i];
            if (c.getAnnotation(EventBusSubscriber.class) == null) continue;

            String regKey = key + ":" + c.getName();
            if (!REGISTERED.add(regKey)) continue;

            Method[] declared = c.getDeclaredMethods();
            for (int j = 0; j < declared.length; j++) {
                Method m = declared[j];
                SubscribeSystem sub = m.getAnnotation(SubscribeSystem.class);
                if (sub == null) continue;

                int mod = m.getModifiers();
                if (!Modifier.isPublic(mod) || !Modifier.isStatic(mod)) continue;

                if (m.getReturnType() == Void.TYPE) continue;

                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 0) {
                    methods.add(m);
                    continue;
                }

                if (ps.length == 1 && ps[0].isAssignableFrom(plugin.getClass())) {
                    methods.add(m);
                }
            }
        }

        methods.sort(Comparator.comparingInt(m -> m.getAnnotation(SubscribeSystem.class).priority()));

        for (int i = 0; i < methods.size(); i++) {
            Method m = methods.get(i);

            try {
                Object sys = (m.getParameterCount() == 0) ? m.invoke(null) : m.invoke(null, plugin);
                if (sys == null) continue;

                if (!(sys instanceof ISystem<?>)) continue;

                SubscribeSystem meta = m.getAnnotation(SubscribeSystem.class);

                if (meta.store() == SystemStore.CHUNK) {
                    plugin.getChunkStoreRegistry().registerSystem((ISystem<ChunkStore>) sys);
                } else {
                    plugin.getEntityStoreRegistry().registerSystem((ISystem<EntityStore>) sys);
                }
            } catch (IllegalArgumentException ignored) {
            } catch (IllegalStateException e) {
                throw e;
            } catch (Throwable ignored) {
            }

        }
    }

}
