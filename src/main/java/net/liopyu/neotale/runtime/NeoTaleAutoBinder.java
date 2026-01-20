package net.liopyu.neotale.runtime;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoTaleAutoBinder {
    private static final Set<String> BOUND = ConcurrentHashMap.newKeySet();

    public static void bind(JavaPlugin plugin) {
        String key = bindKey(plugin);
        if (!BOUND.add(key)) {
            return;
        }

        Class<?>[] subscribers = NeoTaleTargetScanner.findSubscriberClasses(plugin);
        if (subscribers.length == 0) {
            return;
        }

        NeoTaleSystemAutoRegistrar.registerSystems(plugin, subscribers);
        NeoTaleSubscribeRegistrar.registerAll(plugin, plugin.getEventRegistry(), subscribers);
    }

    public static void unbind(JavaPlugin plugin) {
        BOUND.remove(bindKey(plugin));
    }

    private static String bindKey(JavaPlugin plugin) {
        return plugin.getIdentifier().toString() + ":" + System.identityHashCode(plugin.getClassLoader());
    }
}
