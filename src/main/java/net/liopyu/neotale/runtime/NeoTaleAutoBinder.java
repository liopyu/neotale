package net.liopyu.neotale.runtime;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoTaleAutoBinder {
    private static final Set<String> BOUND = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, JavaPlugin> PENDING = new ConcurrentHashMap<>();
    private static volatile boolean LATE_PHASE = false;

    public static void markLatePhase() {
        System.out.println("[NeoTaleAutoBinder] markLatePhase() switching LATE_PHASE false->true pending=" + PENDING.size());
        LATE_PHASE = true;

        for (JavaPlugin p : PENDING.values()) {
            System.out.println("[NeoTaleAutoBinder] markLatePhase bind(pending) -> " + p.getIdentifier() + " enabled=" + p.isEnabled() + " file=" + p.getFile());
            bind(p);
        }
    }

    public static void bind(JavaPlugin plugin) {
        String key = plugin.getIdentifier().toString() + ":" + System.identityHashCode(plugin.getClassLoader());
        boolean already = BOUND.contains(key);

        System.out.println("[NeoTaleAutoBinder] bind() plugin=" + plugin.getIdentifier() + " key=" + key + " enabled=" + plugin.isEnabled() + " LATE_PHASE=" + LATE_PHASE + " alreadyBound=" + already);

        if (already) return;

        if (!LATE_PHASE) {
            PENDING.put(key, plugin);
            System.out.println("[NeoTaleAutoBinder] bind() queued pendingSize=" + PENDING.size());
            return;
        }

        tryBindNow(plugin, key);
    }

    public static void unbind(JavaPlugin plugin) {
        String key = plugin.getIdentifier().toString() + ":" + System.identityHashCode(plugin.getClassLoader());
        System.out.println("[NeoTaleAutoBinder] unbind() plugin=" + plugin.getIdentifier() + " key=" + key);
        BOUND.remove(key);
        PENDING.remove(key);
    }

    public static void onPluginSetup(JavaPlugin plugin) {
        String key = plugin.getIdentifier().toString() + ":" + System.identityHashCode(plugin.getClassLoader());

        System.out.println("[NeoTaleAutoBinder] onPluginSetup() plugin=" + plugin.getIdentifier() + " enabled=" + plugin.isEnabled() + " LATE_PHASE=" + LATE_PHASE);

        BOUND.remove(key);

        if (!LATE_PHASE) {
            PENDING.put(key, plugin);
            System.out.println("[NeoTaleAutoBinder] onPluginSetup() queued (late not started) pendingSize=" + PENDING.size());
            return;
        }

        JavaPlugin pending = PENDING.remove(key);
        System.out.println("[NeoTaleAutoBinder] onPluginSetup() late started pendingWas=" + (pending != null));
        tryBindNow(pending != null ? pending : plugin, key);
    }

    public static void tryFinalizeLatePhase() {
        if (LATE_PHASE) {
            System.out.println("[NeoTaleAutoBinder] tryFinalizeLatePhase() already late");
            return;
        }

        System.out.println("[NeoTaleAutoBinder] tryFinalizeLatePhase() checking all plugins enabled...");

        int total = 0;
        int javaCount = 0;

        for (com.hypixel.hytale.server.core.plugin.PluginBase pb : com.hypixel.hytale.server.core.plugin.PluginManager.get().getPlugins()) {
            total++;
            if (pb instanceof com.hypixel.hytale.server.core.plugin.JavaPlugin jp) {
                javaCount++;
                boolean en = jp.isEnabled();
                System.out.println("[NeoTaleAutoBinder] tryFinalizeLatePhase() plugin=" + jp.getIdentifier() + " enabled=" + en + " file=" + jp.getFile());
                if (!en) {
                    System.out.println("[NeoTaleAutoBinder] tryFinalizeLatePhase() not ready (disabled): " + jp.getIdentifier());
                    return;
                }
            } else {
                System.out.println("[NeoTaleAutoBinder] tryFinalizeLatePhase() nonJavaPlugin=" + pb.getIdentifier() + " type=" + pb.getClass().getName());
            }
        }

        System.out.println("[NeoTaleAutoBinder] tryFinalizeLatePhase() ready: total=" + total + " javaPlugins=" + javaCount + " -> entering late phase");
        markLatePhase();

        for (com.hypixel.hytale.server.core.plugin.PluginBase pb : com.hypixel.hytale.server.core.plugin.PluginManager.get().getPlugins()) {
            if (pb instanceof com.hypixel.hytale.server.core.plugin.JavaPlugin jp) {
                System.out.println("[NeoTaleAutoBinder] tryFinalizeLatePhase() bind(all) -> " + jp.getIdentifier());
                bind(jp);
            }
        }
    }

    private static void tryBindNow(JavaPlugin plugin, String key) {
        System.out.println("[NeoTaleAutoBinder] tryBindNow() plugin=" + plugin.getIdentifier() + " enabled=" + plugin.isEnabled() + " key=" + key);

        if (!plugin.isEnabled()) {
            PENDING.put(key, plugin);
            System.out.println("[NeoTaleAutoBinder] tryBindNow() not enabled -> requeued pendingSize=" + PENDING.size());
            return;
        }

        Class<?>[] subscribers = NeoTaleTargetScanner.findSubscriberClasses(plugin);
        System.out.println("[NeoTaleAutoBinder] tryBindNow() subscribersFound=" + subscribers.length + " for " + plugin.getIdentifier());

        for (int i = 0; i < subscribers.length; i++) {
            System.out.println("[NeoTaleAutoBinder] tryBindNow() subscriber[" + i + "]=" + subscribers[i].getName());
        }

        if (subscribers.length == 0) {
            BOUND.add(key);
            System.out.println("[NeoTaleAutoBinder] tryBindNow() none -> marking bound");
            return;
        }

        try {
            System.out.println("[NeoTaleAutoBinder] tryBindNow() registerSystems...");
            NeoTaleSystemAutoRegistrar.registerSystems(plugin, subscribers);

            System.out.println("[NeoTaleAutoBinder] tryBindNow() registerAll events...");
            NeoTaleSubscribeRegistrar.registerAll(plugin, plugin.getEventRegistry(), subscribers);

            BOUND.add(key);
            System.out.println("[NeoTaleAutoBinder] tryBindNow() SUCCESS bound key=" + key);
        } catch (IllegalStateException e) {
            PENDING.put(key, plugin);
            System.out.println("[NeoTaleAutoBinder] tryBindNow() IllegalStateException -> requeued " + e.getMessage());
        }
    }
}