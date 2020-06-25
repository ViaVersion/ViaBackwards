package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.VBMappingDataLoader;
import us.myles.ViaVersion.api.protocol.ClientboundPacketType;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.ComponentRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class TranslatableRewriter extends ComponentRewriter {

    private static final Map<String, Map<String, String>> TRANSLATABLES = new HashMap<>();
    protected final BackwardsProtocol protocol;
    protected final Map<String, String> newTranslatables;

    public static void loadTranslatables() {
        JsonObject jsonObject = VBMappingDataLoader.loadData("translation-mappings.json");
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            Map<String, String> versionMappings = new HashMap<>();
            TRANSLATABLES.put(entry.getKey(), versionMappings);
            for (Map.Entry<String, JsonElement> translationEntry : entry.getValue().getAsJsonObject().entrySet()) {
                versionMappings.put(translationEntry.getKey(), translationEntry.getValue().getAsString());
            }
        }
    }

    public TranslatableRewriter(BackwardsProtocol protocol) {
        this(protocol, protocol.getClass().getSimpleName().split("To")[1].replace("_", "."));
    }

    public TranslatableRewriter(BackwardsProtocol protocol, String sectionIdentifier) {
        this.protocol = protocol;
        final Map<String, String> newTranslatables = TRANSLATABLES.get(sectionIdentifier);
        if (newTranslatables == null) {
            ViaBackwards.getPlatform().getLogger().warning("Error loading " + sectionIdentifier + " translatables!");
            this.newTranslatables = new HashMap<>();
        } else
            this.newTranslatables = newTranslatables;
    }

    public void registerPing() {
        protocol.registerOutgoing(State.LOGIN, 0x00, 0x00, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerDisconnect(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerChatMessage(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerBossBar(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UUID);
                map(Type.VAR_INT);
                handler(wrapper -> {
                    int action = wrapper.get(Type.VAR_INT, 0);
                    if (action == 0 || action == 3) {
                        processText(wrapper.passthrough(Type.COMPONENT));
                    }
                });
            }
        });
    }

    public void registerLegacyOpenWindow(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // Id
                map(Type.STRING); // Window Type
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerOpenWindow(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Id
                map(Type.VAR_INT); // Window Type
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerCombatEvent(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    if (wrapper.passthrough(Type.VAR_INT) == 2) {
                        wrapper.passthrough(Type.VAR_INT);
                        wrapper.passthrough(Type.INT);
                        processText(wrapper.passthrough(Type.COMPONENT));
                    }
                });
            }
        });
    }

    public void registerTitle(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int action = wrapper.passthrough(Type.VAR_INT);
                    if (action >= 0 && action <= 2) {
                        processText(wrapper.passthrough(Type.COMPONENT));
                    }
                });
            }
        });
    }

    public void registerTabList(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    processText(wrapper.passthrough(Type.COMPONENT));
                    processText(wrapper.passthrough(Type.COMPONENT));
                });
            }
        });
    }

    @Override
    protected void handleTranslate(JsonObject root, String translate) {
        String newTranslate = newTranslatables.get(translate);
        if (newTranslate != null) {
            root.addProperty("translate", newTranslate);
        }
    }
}
