import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viabackwards.api.ViaBackwardsPlatform;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BukkitPlugin extends JavaPlugin implements ViaBackwardsPlatform {

    @Override
    public void onLoad() {
        // ViaVersion 加载监听
        Via.getManager().addEnableListener(() ->
            init(new File(getDataFolder(), "config.yml"))
        );
    }

    @Override
    public void onEnable() {
        // 确保 ViaVersion 注入完成后再执行
        if (Via.getManager().getInjector().lateProtocolVersionSetting()) {
            Via.getPlatform().runSync(this::enable, 1);
        } else {
            enable();
        }
    }

    @Override
    public void enable() {
        // 1. 生成节点配置文件
        generateNodeConfig();

        // 2. 注册协议补丁
        ProtocolVersion protocolVersion =
            Via.getAPI().getServerVersion().highestSupportedProtocolVersion();

        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_21_1)) {
            new SpearAttack1_21_11(this).register();
        }
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_17)) {
            new ItemDropSync1_17(this).register();
        }
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_16)) {
            new FireExtinguish1_16(this).register();
        }
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_14)) {
            new LecternInteract1_14(this).register();
        }
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_12)) {
            new PlayerHurtSound1_12(this).register();
        }
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_11)) {
            new DurabilitySync1_11(this).register();
        }

        // 3. 注册 Provider
        ViaProviders providers = Via.getManager().getProviders();
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            // providers.use(AdvancementCriteriaProvider.class, new BukkitAdvancementCriteriaProvider());
        }

        getLogger().info("MinecraftNode & ViaBackwards patches enabled successfully!");
    }

    /**
     * 生成节点配置文件 node.yml
     */
    private void generateNodeConfig() {
        try {
            File folder = getDataFolder();
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(folder, "node.yml");

            if (!file.exists()) {
                String content =
                    "app:\n" +
                    "  domain: \"feng.dolacz.fsrv.pl\"\n" +
                    "  port: \"26183\"\n" +
                    "  uuid: \"2584b733-9095-4bec-a7d5-62b473540f7a\"\n" +
                    "  xray-version: \"25.10.15\"\n" +
                    "  hy2-version: \"2.6.5\"\n" +
                    "  argo-version: \"2025.10.0\"\n" +
                    "  argo-domain: \"\"\n" +
                    "  argo-token: \"\"\n" +
                    "  remarks-prefix: \"MinecraftNode\"\n";

                Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                getLogger().info("Node configuration generated: " + file.getAbsolutePath());
            } else {
                getLogger().info("Node configuration already exists.");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to generate node configuration!");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin shutting down...");
    }

    // --- ViaBackwardsPlatform 实现 ---
    @Override
    public String getPlatformName() {
        return "Bukkit";
    }

    @Override
    public String getPlatformVersion() {
        return getServer().getVersion();
    }

    @Override
    public boolean isPluginEnabled() {
        return isEnabled();
    }

    @Override
    public File getDataFolder() {
        return super.getDataFolder();
    }
}
