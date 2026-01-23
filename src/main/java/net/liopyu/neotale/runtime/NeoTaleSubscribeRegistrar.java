package net.liopyu.neotale.runtime;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.event.IAsyncEvent;
import com.hypixel.hytale.event.IBaseEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.liopyu.neotale.api.eventbus.EventBusSubscriber;
import net.liopyu.neotale.api.events.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public final class NeoTaleSubscribeRegistrar {

    private static final ConcurrentHashMap<String, ConcurrentHashMap<Class<?>, java.util.concurrent.CopyOnWriteArrayList<Method>>> ECS_METHODS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<Class<?>, ISystemHolder>> ECS_SYSTEMS = new ConcurrentHashMap<>();
    private static final java.util.Set<String> ECS_REGISTERED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private interface ISystemHolder {
        Object system();
    }


    private static final class EcsDispatchSystem<T extends EcsEvent> extends EntityEventSystem<EntityStore, T> {
        private final java.util.concurrent.CopyOnWriteArrayList<Method> methods;

        private EcsDispatchSystem(Class<T> eventType, java.util.concurrent.CopyOnWriteArrayList<Method> methods) {
            super(eventType);
            this.methods = methods;
        }

        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull T event) {
            if (methods.isEmpty()) return;
            for (int i = 0; i < methods.size(); i++) {
                invokeStatic(methods.get(i), event);
            }
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return Query.any();
        }
    }

    public static void registerAll(@Nonnull JavaPlugin plugin, @Nonnull EventRegistry registry, @Nonnull Class<?>... subscriberClasses) {
        for (int i = 0; i < subscriberClasses.length; i++) {
            registerClass(plugin, registry, subscriberClasses[i]);
        }

        String key = ecsKey(plugin);
        ConcurrentHashMap<Class<?>, ISystemHolder> holders = ECS_SYSTEMS.get(key);
        if (holders == null || holders.isEmpty()) return;

        for (java.util.Map.Entry<Class<?>, ISystemHolder> e : holders.entrySet()) {
            Class<?> eventType = e.getKey();
            String regKey = key + ":" + eventType.getName();
            boolean first = ECS_REGISTERED.add(regKey);
            if (!first) continue;

            try {
                System.out.println("[NeoTaleSubscribeRegistrar] registering ECS dispatch system for " + eventType.getName());
                plugin.getEntityStoreRegistry().registerSystem((com.hypixel.hytale.component.system.ISystem<EntityStore>) e.getValue().system());
                System.out.println("[NeoTaleSubscribeRegistrar] ECS dispatch registered OK for " + eventType.getName());
            } catch (IllegalStateException ex) {
                ECS_REGISTERED.remove(regKey);
                throw ex;
            } catch (IllegalArgumentException ex) {
                ECS_REGISTERED.remove(regKey);
                throw ex;
            }
        }
    }

    private static void registerEcs(@Nonnull JavaPlugin plugin, @Nonnull Method m, @Nonnull Class<?> eventTypeRaw) {
        String key = ecsKey(plugin);

        ConcurrentHashMap<Class<?>, java.util.concurrent.CopyOnWriteArrayList<Method>> perPlugin =
                ECS_METHODS.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        java.util.concurrent.CopyOnWriteArrayList<Method> list =
                perPlugin.computeIfAbsent(eventTypeRaw, k -> new java.util.concurrent.CopyOnWriteArrayList<>());

        list.add(m);

        ConcurrentHashMap<Class<?>, ISystemHolder> perPluginSystems =
                ECS_SYSTEMS.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        perPluginSystems.computeIfAbsent(eventTypeRaw, k -> {
            Class<?> et = eventTypeRaw;
            EcsDispatchSystem<?> sys = new EcsDispatchSystem((Class) et, list);
            return () -> sys;
        });
    }


    private static void registerClass(@Nonnull JavaPlugin plugin, @Nonnull EventRegistry registry, @Nonnull Class<?> cls) {
        boolean sub = cls.getAnnotation(EventBusSubscriber.class) != null;
        if (!sub) return;

        Method[] methods = cls.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            SubscribeEvent ann = m.getAnnotation(SubscribeEvent.class);
            if (ann == null) continue;

            int mod = m.getModifiers();
            boolean ok = Modifier.isPublic(mod) && Modifier.isStatic(mod);
            Class<?>[] params = m.getParameterTypes();

            if (!ok) continue;
            if (m.getReturnType() != void.class) continue;
            if (params.length != 1) continue;

            Class<?> eventType = params[0];

            if (IBaseEvent.class.isAssignableFrom(eventType)) {
                registerBus(registry, ann, m, eventType);
                System.out.println("[NeoTaleSubscribeRegistrar]   registered bus handler for " + eventType.getName());
                continue;
            }

            if (EcsEvent.class.isAssignableFrom(eventType)) {
                registerEcs(plugin, m, eventType);
                System.out.println("[NeoTaleSubscribeRegistrar]   registered ecs hook for " + eventType.getName());
            }
        }
    }


    private static void registerBus(@Nonnull EventRegistry registry, @Nonnull SubscribeEvent ann, @Nonnull Method m, @Nonnull Class<?> eventTypeRaw) {
        short prio = ann.priority();

        if (IAsyncEvent.class.isAssignableFrom(eventTypeRaw)) {
            Class<? super IAsyncEvent<Object>> eventType = (Class<? super IAsyncEvent<Object>>) eventTypeRaw;

            Function<CompletableFuture<IAsyncEvent<Object>>, CompletableFuture<IAsyncEvent<Object>>> fn =
                    (future) -> future.thenApply(evt -> {
                        invokeStatic(m, evt);
                        return evt;
                    });

            if (ann.global()) {
                registry.registerAsyncGlobal(prio, eventType, (Function) fn);
            } else if (ann.unhandled()) {
                registry.registerAsyncUnhandled(prio, eventType, (Function) fn);
            } else {
                registry.registerAsync(prio, eventType, (Function) fn);
            }
        } else {
            Class<? super IBaseEvent<Object>> eventType = (Class<? super IBaseEvent<Object>>) eventTypeRaw;
            Consumer<IBaseEvent<Object>> consumer = (evt) -> invokeStatic(m, evt);

            if (ann.global()) {
                registry.registerGlobal(prio, eventType, (Consumer) consumer);
            } else if (ann.unhandled()) {
                registry.registerUnhandled(prio, eventType, (Consumer) consumer);
            } else {
                registry.register(prio, eventType, (Consumer) consumer);
            }
        }
    }


    private static String ecsKey(@Nonnull JavaPlugin plugin) {
        return plugin.getIdentifier().toString() + ":" + System.identityHashCode(plugin.getClassLoader());
    }

    private static void invokeStatic(@Nonnull Method m, @Nonnull Object evt) {
        try {
            m.invoke(null, evt);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
