package net.liopyu.neotale.tests;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.liopyu.neotale.api.eventbus.EventBusSubscriber;
import net.liopyu.neotale.api.events.SubscribeEvent;
import net.liopyu.neotale.api.system.SubscribeSystem;
import net.liopyu.neotale.api.system.SystemStore;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicLong;


//@EventBusSubscriber
public final class Tests {

    private static final AtomicLong BREAK_COUNT_SYSTEM = new AtomicLong();
    private static final AtomicLong BREAK_COUNT_EVENT_A = new AtomicLong();
    private static final AtomicLong BREAK_COUNT_EVENT_B = new AtomicLong();
    private static final AtomicLong BREAK_COUNT_EVENT_C = new AtomicLong();

    private static final AtomicLong CHUNK_ADD = new AtomicLong();
    private static final AtomicLong CHUNK_REMOVE = new AtomicLong();

    private static volatile long lastPrintNs = System.nanoTime();

    @SubscribeSystem(store = SystemStore.ENTITY, priority = 0)
    public static ISystem<EntityStore> entitySystemA() {
        return BreakBlockSystemA.INSTANCE;
    }

    public static final class BreakBlockSystemA extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        public static final BreakBlockSystemA INSTANCE = new BreakBlockSystemA();

        private BreakBlockSystemA() {
            super(BreakBlockEvent.class);
        }

        @Override
        public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent breakBlockEvent) {
            long n = BREAK_COUNT_SYSTEM.incrementAndGet();
            maybePrint(n, BREAK_COUNT_EVENT_A.get(), BREAK_COUNT_EVENT_B.get(), BREAK_COUNT_EVENT_C.get());
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }
    }

    @SubscribeSystem(store = SystemStore.ENTITY, priority = 1)
    public static ISystem<EntityStore> entitySystemB() {
        return BreakBlockSystemB.INSTANCE;
    }

    public static final class BreakBlockSystemB extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        public static final BreakBlockSystemB INSTANCE = new BreakBlockSystemB();

        private BreakBlockSystemB() {
            super(BreakBlockEvent.class);
        }

        @Override
        public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent breakBlockEvent) {
            BREAK_COUNT_SYSTEM.incrementAndGet();
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }
    }

    @SubscribeSystem(store = SystemStore.CHUNK)
    public static ISystem<ChunkStore> chunkSystemA() {
        return ChunkWatchSystemA.INSTANCE;
    }

    public static final class ChunkWatchSystemA extends RefSystem<ChunkStore> {
        public static final ChunkWatchSystemA INSTANCE = new ChunkWatchSystemA();

        private ChunkWatchSystemA() {
        }

        @Override
        public Query<ChunkStore> getQuery() {
            return Query.any();
        }

        @Override
        public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason addReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
            CHUNK_ADD.incrementAndGet();
        }

        @Override
        public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
            CHUNK_REMOVE.incrementAndGet();
        }
    }

    @SubscribeEvent
    public static void onBreakA(BreakBlockEvent e) {
        long n = BREAK_COUNT_EVENT_A.incrementAndGet();
        maybePrint(BREAK_COUNT_SYSTEM.get(), n, BREAK_COUNT_EVENT_B.get(), BREAK_COUNT_EVENT_C.get());
    }

    @SubscribeEvent
    public static void onBreakB(BreakBlockEvent e) {
        long n = BREAK_COUNT_EVENT_B.incrementAndGet();
        maybePrint(BREAK_COUNT_SYSTEM.get(), BREAK_COUNT_EVENT_A.get(), n, BREAK_COUNT_EVENT_C.get());
    }

    @SubscribeEvent
    public static void onBreakC(BreakBlockEvent e) {
        long n = BREAK_COUNT_EVENT_C.incrementAndGet();
        maybePrint(BREAK_COUNT_SYSTEM.get(), BREAK_COUNT_EVENT_A.get(), BREAK_COUNT_EVENT_B.get(), n);
    }

    private static volatile long lastSys;
    private static volatile long lastA;
    private static volatile long lastB;
    private static volatile long lastC;
    private static volatile long lastAdd;
    private static volatile long lastRemove;

    private static void maybePrint(long sys, long a, long b, long c) {
        long now = System.nanoTime();
        long last = lastPrintNs;
        if (now - last < 1_000_000_000L) return;
        lastPrintNs = now;

        long dSys = sys - lastSys;
        long dA = a - lastA;
        long dB = b - lastB;
        long dC = c - lastC;
        long add = CHUNK_ADD.get();
        long rem = CHUNK_REMOVE.get();
        long dAdd = add - lastAdd;
        long dRem = rem - lastRemove;

        lastSys = sys;
        lastA = a;
        lastB = b;
        lastC = c;
        lastAdd = add;
        lastRemove = rem;

        System.out.println(
                "[NeoTaleTest2] +1s dSystem=" + dSys +
                        " dEventA=" + dA +
                        " dEventB=" + dB +
                        " dEventC=" + dC +
                        " dChunkAdd=" + dAdd +
                        " dChunkRemove=" + dRem +
                        " totals system=" + sys +
                        " A=" + a +
                        " B=" + b +
                        " C=" + c
        );
    }

    @SubscribeSystem(store = SystemStore.CHUNK)
    public static ISystem<ChunkStore> myChunkSystem() {
        return new RefSystem<ChunkStore>() {

            @Override
            public Query<ChunkStore> getQuery() {
                return Query.any();
            }

            @Override
            public void onEntityAdded(Ref<ChunkStore> ref, AddReason reason, Store<ChunkStore> store, CommandBuffer<ChunkStore> buffer) {

            }

            @Override
            public void onEntityRemove(Ref<ChunkStore> ref, RemoveReason reason, Store<ChunkStore> store, CommandBuffer<ChunkStore> buffer) {

            }
        };
    }

}
