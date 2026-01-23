package net.liopyu.neotale.runtime;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoTaleAutoBinder {
    private static final Set<String> BOUND = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, JavaPlugin> PENDING = new ConcurrentHashMap<>();
    private static volatile boolean LATE_PHASE = false;

    public static void markLatePhase() {
        LATE_PHASE = true;

        for (JavaPlugin p : PENDING.values()) {
            bind(p);
        }
    }

    public static void bind(JavaPlugin plugin) {
        String key = plugin.getIdentifier().toString() + ":" + System.identityHashCode(plugin.getClassLoader());
        boolean already = BOUND.contains(key);

        if (already) return;

        if (!LATE_PHASE) {
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
        if (!LATE_PHASE) {
            PENDING.put(key, plugin);
            return;
        }

        JavaPlugin pending = PENDING.remove(key);
        tryBindNow(pending != null ? pending : plugin, key);
    }

    public static void tryFinalizeLatePhase() {
        if (LATE_PHASE) {
            return;
        }


        int total = 0;
        int javaCount = 0;

        for (com.hypixel.hytale.server.core.plugin.PluginBase pb : com.hypixel.hytale.server.core.plugin.PluginManager.get().getPlugins()) {
            total++;
            if (pb instanceof com.hypixel.hytale.server.core.plugin.JavaPlugin jp) {
                javaCount++;
                boolean en = jp.isEnabled();
                if (!en) {
                    return;
                }
            }
        }

        markLatePhase();

        for (com.hypixel.hytale.server.core.plugin.PluginBase pb : com.hypixel.hytale.server.core.plugin.PluginManager.get().getPlugins()) {
            if (pb instanceof com.hypixel.hytale.server.core.plugin.JavaPlugin jp) {
                bind(jp);
            }
        }
    }

    private static void tryBindNow(JavaPlugin plugin, String key) {
        if (!plugin.isEnabled()) {
            PENDING.put(key, plugin);
            return;
        }

        Class<?>[] subscribers = NeoTaleTargetScanner.findSubscriberClasses(plugin);
        if (subscribers.length == 0) {
            BOUND.add(key);
            return;
        }

        try {
            NeoTaleSystemAutoRegistrar.registerSystems(plugin, subscribers);

            NeoTaleSubscribeRegistrar.registerAll(plugin, plugin.getEventRegistry(), subscribers);

            BOUND.add(key);
        } catch (IllegalStateException e) {
            PENDING.put(key, plugin);
            System.out.println("[NeoTaleAutoBinder] tryBindNow() IllegalStateException -> requeued " + e.getMessage());
        }
    }
}