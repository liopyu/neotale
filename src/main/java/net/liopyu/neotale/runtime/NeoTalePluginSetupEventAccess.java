package net.liopyu.neotale.runtime;

import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.event.PluginSetupEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class NeoTalePluginSetupEventAccess {
    public static PluginBase extractPlugin(PluginSetupEvent evt) {
        System.out.println("[NeoTalePluginSetupEventAccess] extractPlugin evtClass=" + evt.getClass().getName());

        try {
            Method m = evt.getClass().getMethod("getPlugin");
            Object v = m.invoke(evt);
            System.out.println("[NeoTalePluginSetupEventAccess] getPlugin() -> " + (v == null ? "null" : v.getClass().getName()));
            if (v instanceof PluginBase pb) return pb;
        } catch (Throwable t) {
            System.out.println("[NeoTalePluginSetupEventAccess] getPlugin() failed: " + t.getClass().getName() + " " + String.valueOf(t.getMessage()));
        }

        try {
            Method m = evt.getClass().getMethod("plugin");
            Object v = m.invoke(evt);
            System.out.println("[NeoTalePluginSetupEventAccess] plugin() -> " + (v == null ? "null" : v.getClass().getName()));
            if (v instanceof PluginBase pb) return pb;
        } catch (Throwable t) {
            System.out.println("[NeoTalePluginSetupEventAccess] plugin() failed: " + t.getClass().getName() + " " + String.valueOf(t.getMessage()));
        }

        Field[] fs = evt.getClass().getDeclaredFields();
        System.out.println("[NeoTalePluginSetupEventAccess] scanning fields count=" + fs.length);

        for (int i = 0; i < fs.length; i++) {
            try {
                Field f = fs[i];
                f.setAccessible(true);
                Object v = f.get(evt);
                System.out.println("[NeoTalePluginSetupEventAccess] field[" + i + "] " + f.getName() + ":" + f.getType().getName() + " -> " + (v == null ? "null" : v.getClass().getName()));
                if (v instanceof PluginBase pb) return pb;
            } catch (Throwable t) {
                System.out.println("[NeoTalePluginSetupEventAccess] field[" + i + "] read failed: " + t.getClass().getName() + " " + String.valueOf(t.getMessage()));
            }
        }

        System.out.println("[NeoTalePluginSetupEventAccess] extractPlugin -> null");
        return null;
    }
}
