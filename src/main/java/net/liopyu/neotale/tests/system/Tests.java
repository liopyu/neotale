package net.liopyu.neotale.tests.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.liopyu.neotale.api.events.EventBusSubscriber;
import net.liopyu.neotale.api.events.SubscribeEvent;
import net.liopyu.neotale.api.system.SubscribeSystem;
import net.liopyu.neotale.api.system.SystemStore;

import javax.annotation.Nonnull;

@EventBusSubscriber
public final class Tests {

    @SubscribeSystem(store = SystemStore.ENTITY)
    public static ISystem<EntityStore> myEntitySystem() {
        return BreakBlockSystem.INSTANCE;
    }

    public static final class BreakBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        public static final BreakBlockSystem INSTANCE = new BreakBlockSystem();

        private BreakBlockSystem() {
            super(BreakBlockEvent.class);
        }

        @Override
        public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent breakBlockEvent) {
            System.out.println("Block break System is registered.");
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }
    }

    @SubscribeSystem(store = SystemStore.CHUNK)
    public static ISystem<ChunkStore> myChunkSystem() {
        return ChunkWatchSystem.INSTANCE;
    }

    public static final class ChunkWatchSystem extends RefSystem<ChunkStore> {
        public static final ChunkWatchSystem INSTANCE = new ChunkWatchSystem();

        private ChunkWatchSystem() {
        }

        @Override
        public Query<ChunkStore> getQuery() {
            return Query.any();
        }

        @Override
        public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason addReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
            System.out.println("Added entity to world because: " + addReason.name());
        }

        @Override
        public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
            System.out.println("Removed entity from world because: " + removeReason.name());
        }
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent e) {
        System.out.println("Block break event registered!");
    }
}
