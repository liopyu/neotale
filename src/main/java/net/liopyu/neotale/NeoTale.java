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
        System.out.println("[NeoTale] setup() start id=" + getIdentifier() + " enabled=" + isEnabled() + " file=" + getFile());

        getEventRegistry().registerGlobal(PluginSetupEvent.class, (evt) -> {
            System.out.println("[NeoTale] PluginSetupEvent fired evtClass=" + evt.getClass().getName());

            PluginBase pb = NeoTalePluginSetupEventAccess.extractPlugin(evt);
            System.out.println("[NeoTale] PluginSetupEvent extractPlugin=" + (pb == null ? "null" : (pb.getIdentifier() + " type=" + pb.getClass().getName())));

            if (pb instanceof JavaPlugin jp) {
                System.out.println("[NeoTale] onPluginSetup -> " + jp.getIdentifier() + " enabled=" + jp.isEnabled() + " file=" + jp.getFile());
                NeoTaleAutoBinder.onPluginSetup(jp);
            } else {
                System.out.println("[NeoTale] extractPlugin was not JavaPlugin");
            }

            NeoTaleAutoBinder.tryFinalizeLatePhase();
        });

        System.out.println("[NeoTale] calling bind(this)");
        NeoTaleAutoBinder.bind(this);

        System.out.println("[NeoTale] setup() end");
    }

}
