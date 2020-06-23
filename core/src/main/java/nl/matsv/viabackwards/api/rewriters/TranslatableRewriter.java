package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.VBMappingDataLoader;
import us.myles.ViaVersion.api.protocol.ClientboundPacketType;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.util.GsonUtil;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;
import us.myles.viaversion.libs.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

public class TranslatableRewriter {

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
                handler(wrapper -> wrapper.write(Type.COMPONENT_STRING, processText(wrapper.read(Type.COMPONENT_STRING))));
            }
        });
    }

    public void registerDisconnect(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> wrapper.write(Type.COMPONENT_STRING, processText(wrapper.read(Type.COMPONENT_STRING))));
            }
        });
    }

    public void registerChatMessage(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> wrapper.write(Type.COMPONENT_STRING, processText(wrapper.read(Type.COMPONENT_STRING))));
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
                        wrapper.write(Type.COMPONENT_STRING, processText(wrapper.read(Type.COMPONENT_STRING)));
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
                handler(wrapper -> wrapper.write(Type.COMPONENT_STRING, processText(wrapper.read(Type.COMPONENT_STRING))));
            }
        });
    }

    public void registerOpenWindow(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Id
                map(Type.VAR_INT); // Window Type
                handler(wrapper -> wrapper.write(Type.COMPONENT_STRING, processText(wrapper.read(Type.COMPONENT_STRING))));
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
                        wrapper.write(Type.COMPONENT_STRING, processText(wrapper.read(Type.COMPONENT_STRING)));
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
                        wrapper.write(Type.COMPONENT_STRING, processText(wrapper.read(Type.COMPONENT_STRING)));
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
                    wrapper.write(Type.COMPONENT_STRING, processText(wrapper.read(Type.COMPONENT_STRING)));
                    wrapper.write(Type.COMPONENT_STRING, processText(wrapper.read(Type.COMPONENT_STRING)));
                });
            }
        });
    }

    public String processText(String value) {
        JsonElement root = GsonUtil.getJsonParser().parse(value);
        processText(root);
        return root.toString();
    }

    protected void processText(JsonElement element) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            processAsArray(element);
            return;
        }
        if (element.isJsonPrimitive()) {
            handleText(element.getAsJsonPrimitive());
            return;
        }

        JsonObject object = element.getAsJsonObject();
        JsonPrimitive text = object.getAsJsonPrimitive("text");
        if (text != null) {
            handleText(text);
        }

        JsonElement translate = object.get("translate");
        if (translate != null) {
            handleTranslate(object, translate.getAsString());

            JsonElement with = object.get("with");
            if (with != null) {
                processAsArray(with);
            }
        }

        JsonElement extra = object.get("extra");
        if (extra != null) {
            processAsArray(extra);
        }

        JsonObject hoverEvent = object.getAsJsonObject("hoverEvent");
        if (hoverEvent != null) {
            handleHoverEvent(hoverEvent);
        }
    }

    protected void handleText(JsonPrimitive text) {
        // In case this is needed in the future
    }

    protected void handleTranslate(JsonObject root, String translate) {
        String newTranslate = newTranslatables.get(translate);
        if (newTranslate != null) {
            root.addProperty("translate", newTranslate);
        }
    }

    protected void handleHoverEvent(JsonObject hoverEvent) {
        String action = hoverEvent.getAsJsonPrimitive("action").getAsString();
        if (action.equals("show_text")) {
            JsonElement value = hoverEvent.get("value");
            processText(value != null ? value : hoverEvent.get("contents"));
        } else if (action.equals("show_entity")) {
            JsonObject contents = hoverEvent.getAsJsonObject("contents");
            if (contents != null) {
                processText(contents.get("name"));
            }
        }
    }

    private void processAsArray(JsonElement element) {
        for (JsonElement jsonElement : element.getAsJsonArray()) {
            processText(jsonElement);
        }
    }
}
