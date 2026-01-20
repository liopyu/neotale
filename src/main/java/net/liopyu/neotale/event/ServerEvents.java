package net.liopyu.neotale.event;


import com.hypixel.hytale.server.core.event.events.BootEvent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import net.liopyu.neotale.api.events.EventBusSubscriber;
import net.liopyu.neotale.api.events.SubscribeEvent;

@EventBusSubscriber
public final class ServerEvents {


    @SubscribeEvent
    public static void onBoot(BootEvent e) {
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent e) {
        e.getTargetBlock();
        System.out.println("Test!");
    }
}
