package net.liopyu.neotale.runtime;

import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.event.PluginSetupEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class NeoTalePluginSetupEventAccess {
    public static PluginBase extractPlugin(PluginSetupEvent evt) {
        try {
            Method m = evt.getClass().getMethod("getPlugin");
            Object v = m.invoke(evt);
            if (v instanceof PluginBase pb) return pb;
        } catch (Throwable ignored) {
        }

        try {
            Method m = evt.getClass().getMethod("plugin");
            Object v = m.invoke(evt);
            if (v instanceof PluginBase pb) return pb;
        } catch (Throwable ignored) {
        }

        Field[] fs = evt.getClass().getDeclaredFields();
        for (int i = 0; i < fs.length; i++) {
            try {
                Field f = fs[i];
                f.setAccessible(true);
                Object v = f.get(evt);
                if (v instanceof PluginBase pb) return pb;
            } catch (Throwable ignored) {
            }
        }

        return null;
    }
}
