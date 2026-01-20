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
import net.liopyu.neotale.api.events.EventBusSubscriber;
import net.liopyu.neotale.api.events.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public final class NeoTaleSubscribeRegistrar {

    public static void registerAll(@Nonnull JavaPlugin plugin, @Nonnull EventRegistry registry, @Nonnull Class<?>... subscriberClasses) {
        for (int i = 0; i < subscriberClasses.length; i++) {
            registerClass(plugin, registry, subscriberClasses[i]);
        }
    }

    private static void registerClass(@Nonnull JavaPlugin plugin, @Nonnull EventRegistry registry, @Nonnull Class<?> cls) {
        if (cls.getAnnotation(EventBusSubscriber.class) == null) {
            return;
        }

        Method[] methods = cls.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            SubscribeEvent ann = m.getAnnotation(SubscribeEvent.class);
            if (ann == null) {
                continue;
            }

            if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
                continue;
            }

            if (m.getReturnType() != void.class) {
                continue;
            }

            Class<?>[] params = m.getParameterTypes();
            if (params.length != 1) {
                continue;
            }

            Class<?> eventType = params[0];

            if (IBaseEvent.class.isAssignableFrom(eventType)) {
                registerBus(registry, ann, m, eventType);
                continue;
            }

            if (EcsEvent.class.isAssignableFrom(eventType)) {
                registerEcs(plugin, ann, m, eventType);
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

    private static void registerEcs(@Nonnull JavaPlugin plugin, @Nonnull SubscribeEvent ann, @Nonnull Method m, @Nonnull Class<?> eventTypeRaw) {
        Class<? extends EcsEvent> eventType = (Class<? extends EcsEvent>) eventTypeRaw;

        EntityEventSystem<EntityStore, EcsEvent> sys = new EntityEventSystem<EntityStore, EcsEvent>((Class) eventType) {
            @Override
            public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull EcsEvent event) {
                invokeStatic(m, event);
            }

            @Override
            public @Nullable Query<EntityStore> getQuery() {
                return ann.ecsAny() ? Query.any() : Query.any();
            }
        };

        plugin.getEntityStoreRegistry().registerSystem(sys);
    }

    private static void invokeStatic(@Nonnull Method m, @Nonnull Object evt) {
        try {
            m.invoke(null, evt);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
