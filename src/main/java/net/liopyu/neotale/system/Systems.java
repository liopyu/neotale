package net.liopyu.neotale.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.chunk.systems.ChunkSystems;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.liopyu.neotale.api.events.EventBusSubscriber;
import net.liopyu.neotale.api.system.SubscribeSystem;
import net.liopyu.neotale.api.system.SystemStore;

import javax.annotation.Nonnull;

@EventBusSubscriber
public final class Systems {

    @SubscribeSystem(store = SystemStore.ENTITY)
    public static ISystem<EntityStore> myEntitySystem() {
        return new EntityEventSystem<>(BreakBlockEvent.class) {
            @Override
            public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent breakBlockEvent) {

            }

            @Override
            public Query<EntityStore> getQuery() {
                return Query.any();
            }
        };
    }

    @SubscribeSystem(store = SystemStore.CHUNK)
    public static ISystem<ChunkStore> myChunkSystem() {
        return new ChunkSystems.OnChunkLoad();
    }

}
