# NeoTale

NeoTale is a lightweight, annotation-driven event system for Hytale plugins inspired by NeoForge’s event bus model. It
removes manual registration and boilerplate by automatically discovering and wiring your event handlers at runtime.

---

## Features

- Annotation-based event subscription
- No manual registration required
- Automatic class scanning
- Static method dispatch
- Minimal, clean API
- NeoForge-style workflow

---

## How It Works

On startup, NeoTale scans your plugin and automatically detects:

- Classes annotated with `@EventBusSubscriber`
- `public static` methods inside those classes annotated with `@SubscribeEvent`

Any method matching this pattern is automatically registered to the event bus and invoked when the corresponding event
fires.

---

## Event Example

```java

@EventBusSubscriber
public final class ServerEvents {

    @SubscribeEvent
    public static void onBoot(BootEvent event) {
        System.out.println("Server booted!");
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        System.out.println("Block broken!");
    }
}
````

That’s it. NeoTale handles discovery and binding automatically.

---

## Detection Rules

NeoTale will only register methods that meet all of the following:

* Declared in a class annotated with `@EventBusSubscriber`
* Marked `public static`
* Annotated with `@SubscribeEvent`
* Accept exactly one parameter (the event type)

---

## System Registration

NeoTale can also auto-register ECS systems using the same annotation-first approach.
Simply declare `public static` methods annotated with `@SubscribeSystem` inside an `@EventBusSubscriber` class.
NeoTale will invoke these methods during plugin setup and register the returned system into the correct registry.

---

## System Example

```java

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
```

---

## Gradle Setup

Add the NeoTale Maven repository:

### Gradle (Groovy)

```groovy
repositories {
    maven { url 'https://dl.cloudsmith.io/public/lio/neotale/maven/' }
}

dependencies {
    implementation "net.liopyu:neotale:$neoTaleVersion"
}
```

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://dl.cloudsmith.io/public/lio/neotale/maven/")
}

dependencies {
    implementation("net.liopyu:neotale:${neoTaleVersion}")
}
```

---

## Why NeoTale?

Hytale’s native systems are powerful, but event wiring can be repetitive and confusing.
NeoTale streamlines development by providing a simple, familiar approach for developers coming from NeoForge event bus
subscription.

---

## Usage & Distribution

NeoTale is free to use in any project, including public servers, modpacks, and redistributed builds. You are allowed to
bundle and redistribute NeoTale as-is wherever you need it.

The ARR label only applies to direct ownership of the project’s source/assets; it does **not** restrict normal use or
redistribution of the built mod.

