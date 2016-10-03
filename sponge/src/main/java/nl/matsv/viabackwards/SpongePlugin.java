package nl.matsv.viabackwards;

import com.google.inject.Inject;
import nl.matsv.viabackwards.sponge.VersionInfo;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import us.myles.ViaVersion.sponge.util.LoggerWrapper;

import java.util.logging.Logger;

@Plugin(id = "viabackwards",
        name = "ViaBackwards",
        version = VersionInfo.VERSION,
        authors = {"Matsv"},
        description = "Allow older Minecraft versions to connect to an newer server version.",
        dependencies = {@Dependency(id = "viaversion")}
)
public class SpongePlugin implements ViaBackwardsPlatform {
    private Logger logger;
    @Inject
    private PluginContainer container;

    @Listener
    public void onServerStart(GameAboutToStartServerEvent e) {
        // Setup Logger
        this.logger = new LoggerWrapper(container.getLogger());
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
