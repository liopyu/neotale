package net.liopyu.neotale.runtime;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoTaleAutoBinder {
    private static final Set<String> BOUND = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, JavaPlugin> PENDING = new ConcurrentHashMap<>();

    public static void bind(JavaPlugin plugin) {
        String key = plugin.getIdentifier().toString() + ":" + System.identityHashCode(plugin.getClassLoader());
        if (BOUND.contains(key)) return;

        if (plugin.getFile() == null) {
            PENDING.put(key, plugin);
            return;
        }

        tryBindNow(plugin, key);
    }


    public static void unbind(JavaPlugin plugin) {
        String key = plugin.getIdentifier().toString() + ":" + System.identityHashCode(plugin.getClassLoader());
        BOUND.remove(key);
        PENDING.remove(key);
    }

    public static void onPluginSetup(JavaPlugin plugin) {
        String key = plugin.getIdentifier().toString() + ":" + System.identityHashCode(plugin.getClassLoader());

        BOUND.remove(key);

        JavaPlugin pending = PENDING.remove(key);
        tryBindNow(pending != null ? pending : plugin, key);
    }


    private static void tryBindNow(JavaPlugin plugin, String key) {
        Class<?>[] subscribers = NeoTaleTargetScanner.findSubscriberClasses(plugin);
        if (subscribers.length == 0) {
            if (plugin.getFile() == null) {
                PENDING.put(key, plugin);
                return;
            }
            BOUND.add(key);
            return;
        }

        try {
            NeoTaleSystemAutoRegistrar.registerSystems(plugin, subscribers);
            NeoTaleSubscribeRegistrar.registerAll(plugin, plugin.getEventRegistry(), subscribers);
            BOUND.add(key);
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not enabled")) {
                PENDING.put(key, plugin);
                return;
            }
            throw e;
        }
    }
}
