package nl.matsv.viabackwards.api.rewriters;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.VBMappingDataLoader;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class TranslatableRewriter {

    private static final Map<String, Map<String, String>> TRANSLATABLES = new HashMap<>();
    private final BackwardsProtocol protocol;
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
        protocol.out(State.LOGIN, 0x00, 0x00, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> wrapper.write(Type.STRING, processTranslate(wrapper.read(Type.STRING))));
            }
        });
    }

    public void registerDisconnect(int oldId, int newId) {
        protocol.out(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> wrapper.write(Type.STRING, processTranslate(wrapper.read(Type.STRING))));
            }
        });
    }

    public void registerChatMessage(int oldId, int newId) {
        protocol.out(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> wrapper.write(Type.STRING, processTranslate(wrapper.read(Type.STRING))));
            }
        });
    }

    public void registerBossBar(int oldId, int newId) {
        protocol.out(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UUID);
                map(Type.VAR_INT);
                handler(wrapper -> {
                    int action = wrapper.get(Type.VAR_INT, 0);
                    if (action == 0 || action == 3) {
                        wrapper.write(Type.STRING, processTranslate(wrapper.read(Type.STRING)));
                    }
                });
            }
        });
    }

    public void registerLegacyOpenWindow(int oldId, int newId) {
        protocol.out(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // Id
                map(Type.STRING); // Window Type
                handler(wrapper -> wrapper.write(Type.STRING, processTranslate(wrapper.read(Type.STRING))));
            }
        });
    }

    public void registerOpenWindow(int oldId, int newId) {
        protocol.out(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Id
                map(Type.VAR_INT); // Window Type
                handler(wrapper -> wrapper.write(Type.STRING, processTranslate(wrapper.read(Type.STRING))));
            }
        });
    }

    public void registerCombatEvent(int oldId, int newId) {
        protocol.out(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    if (wrapper.passthrough(Type.VAR_INT) == 2) {
                        wrapper.passthrough(Type.VAR_INT);
                        wrapper.passthrough(Type.INT);
                        wrapper.write(Type.STRING, processTranslate(wrapper.read(Type.STRING)));
                    }
                });
            }
        });
    }

    public void registerTitle(int oldId, int newId) {
        protocol.out(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int action = wrapper.passthrough(Type.VAR_INT);
                    if (action >= 0 && action <= 2) {
                        wrapper.write(Type.STRING, processTranslate(wrapper.read(Type.STRING)));
                    }
                });
            }
        });
    }

    public void registerPlayerList(int oldId, int newId) {
        protocol.out(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.write(Type.STRING, processTranslate(wrapper.read(Type.STRING)));
                    wrapper.write(Type.STRING, processTranslate(wrapper.read(Type.STRING)));
                });
            }
        });
    }

    public String processTranslate(String value) {
        BaseComponent[] components = ComponentSerializer.parse(value);
        for (BaseComponent component : components) {
            processTranslate(component);
        }
        return components.length == 1 ? ComponentSerializer.toString(components[0]) : ComponentSerializer.toString(components);
    }

    protected void processTranslate(BaseComponent component) {
        if (component == null) return;
        if (component instanceof TranslatableComponent) {
            TranslatableComponent translatableComponent = (TranslatableComponent) component;
            String oldTranslate = translatableComponent.getTranslate();
            String newTranslate = newTranslatables.get(oldTranslate);
            if (newTranslate != null) {
                translatableComponent.setTranslate(newTranslate);
            }
            if (translatableComponent.getWith() != null) {
                for (BaseComponent baseComponent : translatableComponent.getWith()) {
                    processTranslate(baseComponent);
                }
            }
        }
        if (component.getHoverEvent() != null) {
            for (BaseComponent baseComponent : component.getHoverEvent().getValue()) {
                processTranslate(baseComponent);
            }
        }
        if (component.getExtra() != null) {
            for (BaseComponent baseComponent : component.getExtra()) {
                processTranslate(baseComponent);
            }
        }
    }
}
