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
        System.out.println("[NeoTaleSystemAutoRegistrar] registerSystems plugin=" + plugin.getIdentifier() + " discovered=" + discoveredClasses.length);

        List<Method> methods = new ArrayList<>();

        for (int i = 0; i < discoveredClasses.length; i++) {
            Class<?> c = discoveredClasses[i];
            boolean isSub = c.getAnnotation(EventBusSubscriber.class) != null;
            System.out.println("[NeoTaleSystemAutoRegistrar] class=" + c.getName() + " @EventBusSubscriber=" + isSub);
            if (!isSub) continue;

            String regKey = key + ":" + c.getName();
            boolean first = REGISTERED.add(regKey);
            System.out.println("[NeoTaleSystemAutoRegistrar] regKey=" + regKey + " firstTime=" + first);
            if (!first) continue;

            Method[] declared = c.getDeclaredMethods();
            for (int j = 0; j < declared.length; j++) {
                Method m = declared[j];
                SubscribeSystem sub = m.getAnnotation(SubscribeSystem.class);
                if (sub == null) continue;

                int mod = m.getModifiers();
                boolean okMods = Modifier.isPublic(mod) && Modifier.isStatic(mod);
                System.out.println("[NeoTaleSystemAutoRegistrar]  @SubscribeSystem method=" + c.getName() + "#" + m.getName() + " okMods=" + okMods + " return=" + m.getReturnType().getName() + " store=" + sub.store() + " prio=" + sub.priority());

                if (!okMods) continue;
                if (m.getReturnType() == Void.TYPE) continue;

                Class<?>[] ps = m.getParameterTypes();
                System.out.println("[NeoTaleSystemAutoRegistrar]   params=" + ps.length);

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
        System.out.println("[NeoTaleSystemAutoRegistrar] methodsToRegister=" + methods.size());

        for (int i = 0; i < methods.size(); i++) {
            Method m = methods.get(i);
            try {
                System.out.println("[NeoTaleSystemAutoRegistrar] invoking " + m.getDeclaringClass().getName() + "#" + m.getName() + " params=" + m.getParameterCount());
                Object sys = (m.getParameterCount() == 0) ? m.invoke(null) : m.invoke(null, plugin);

                System.out.println("[NeoTaleSystemAutoRegistrar] returned=" + (sys == null ? "null" : sys.getClass().getName()));
                if (sys == null) continue;
                if (!(sys instanceof ISystem<?>)) {
                    System.out.println("[NeoTaleSystemAutoRegistrar] not an ISystem, skipping");
                    continue;
                }

                SubscribeSystem meta = m.getAnnotation(SubscribeSystem.class);

                if (meta.store() == SystemStore.CHUNK) {
                    System.out.println("[NeoTaleSystemAutoRegistrar] register chunk system");
                    plugin.getChunkStoreRegistry().registerSystem((ISystem<ChunkStore>) sys);
                } else {
                    System.out.println("[NeoTaleSystemAutoRegistrar] register entity system");
                    plugin.getEntityStoreRegistry().registerSystem((ISystem<EntityStore>) sys);
                }
            } catch (IllegalArgumentException t) {
                System.out.println("[NeoTaleSystemAutoRegistrar] IllegalArgumentException " + t.getMessage());
            } catch (IllegalStateException t) {
                System.out.println("[NeoTaleSystemAutoRegistrar] IllegalStateException " + t.getMessage());
                throw t;
            } catch (Throwable t) {
                System.out.println("[NeoTaleSystemAutoRegistrar] Throwable " + t.getClass().getName() + " " + String.valueOf(t.getMessage()));
            }
        }
    }

}
