package net.liopyu.neotale;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.plugin.event.PluginSetupEvent;
import com.hypixel.hytale.server.worldgen.util.LogUtil;
import net.liopyu.neotale.runtime.NeoTaleAutoBinder;
import net.liopyu.neotale.runtime.NeoTalePluginSetupEventAccess;

import javax.annotation.Nonnull;

public class NeoTale extends JavaPlugin {
    public static final HytaleLogger LOGGER = LogUtil.getLogger().getSubLogger("NeoTale");
    private static final java.util.concurrent.atomic.AtomicBoolean DID_LATE_BIND = new java.util.concurrent.atomic.AtomicBoolean(false);

    public NeoTale(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        System.out.println("Starting NeoTale! - Liopyu");
        getEventRegistry().registerGlobal(PluginSetupEvent.class, (evt) -> {
            PluginBase pb = NeoTalePluginSetupEventAccess.extractPlugin(evt);
            if (pb instanceof JavaPlugin jp) {
                NeoTaleAutoBinder.onPluginSetup(jp);
            } else {
            }

            NeoTaleAutoBinder.tryFinalizeLatePhase();
        });
        NeoTaleAutoBinder.bind(this);
    }

}
