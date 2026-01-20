package net.liopyu.neotale.runtime;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.PluginClassLoader;
import net.liopyu.neotale.api.events.EventBusSubscriber;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class NeoTaleTargetScanner {

    public static Class<?>[] findSubscriberClasses(JavaPlugin plugin) {
        Path file = plugin.getFile();
        PluginClassLoader cl = plugin.getClassLoader();

        List<Class<?>> out = new ArrayList<>();

        if (file == null) return out.toArray(new Class<?>[0]);

        if (Files.isRegularFile(file) && file.toString().endsWith(".jar")) {
            scanJar(out, cl, file.toFile());
        } else if (Files.isDirectory(file)) {
            scanClassesDir(out, cl, file);
        }

        return out.toArray(new Class<?>[0]);
    }


    private static void scanJar(List<Class<?>> out, PluginClassLoader cl, File jarPath) {
        try (JarFile jf = new JarFile(jarPath)) {
            Enumeration<JarEntry> e = jf.entries();
            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                String name = je.getName();
                if (!name.endsWith(".class")) continue;
                if (name.startsWith("META-INF/")) continue;

                String cn = name.substring(0, name.length() - 6).replace('/', '.');

                Class<?> c = tryLoadLocal(cl, cn);
                if (c == null) continue;

                if (c.getAnnotation(EventBusSubscriber.class) == null) continue;

                out.add(c);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void scanClassesDir(List<Class<?>> out, PluginClassLoader cl, Path root) {
        try {
            Files.walk(root).forEach(p -> {
                String s = p.toString();
                if (!s.endsWith(".class")) return;

                String rel = root.relativize(p).toString();
                String cn = rel.substring(0, rel.length() - 6).replace(File.separatorChar, '.');

                Class<?> c = tryLoadLocal(cl, cn);
                if (c == null) return;

                if (c.getAnnotation(EventBusSubscriber.class) == null) return;

                out.add(c);
            });
        } catch (Throwable ignored) {
        }
    }

    private static Class<?> tryLoadLocal(PluginClassLoader cl, String cn) {
        try {
            return cl.loadLocalClass(cn);
        } catch (Throwable t) {
            return null;
        }
    }
}
