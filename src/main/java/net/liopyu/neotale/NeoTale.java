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

    public NeoTale(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        System.out.println("Loading NeoTale! - Liopyu");
        for (PluginBase pb : PluginManager.get().getPlugins()) {
            if (pb instanceof JavaPlugin jp && jp != this) {
                NeoTaleAutoBinder.bind(jp);
            }
        }

        getEventRegistry().registerGlobal(PluginSetupEvent.class, (evt) -> {
            PluginBase pb = NeoTalePluginSetupEventAccess.extractPlugin(evt);
            if (pb instanceof JavaPlugin jp) {
                NeoTaleAutoBinder.bind(jp);
            }
        });

        NeoTaleAutoBinder.bind(this);

    }

}
