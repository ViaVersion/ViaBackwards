/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.protocol1_18_2to1_19;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.data.BackwardsMappings;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.data.CommandRewriter1_19;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.packets.BlockItemPackets1_19;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.packets.EntityPackets1_19;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.storage.DimensionRegistryStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19Types;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.rewriter.EntityRewriter;
import com.viaversion.viaversion.api.rewriter.ItemRewriter;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.kyori.adventure.text.Component;
import com.viaversion.viaversion.libs.kyori.adventure.text.TextReplacementConfig;
import com.viaversion.viaversion.libs.kyori.adventure.text.format.NamedTextColor;
import com.viaversion.viaversion.libs.kyori.adventure.text.format.TextDecoration;
import com.viaversion.viaversion.libs.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.Protocol1_19To1_18_2;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ServerboundPackets1_19;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

import java.time.Instant;
import java.util.UUID;

public final class Protocol1_18_2To1_19 extends BackwardsProtocol<ClientboundPackets1_19, ClientboundPackets1_18, ServerboundPackets1_19, ServerboundPackets1_17> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings();
    private static final UUID ZERO_UUID = new UUID(0, 0);
    private static final byte[] EMPTY_BYTES = new byte[0];
    private final EntityPackets1_19 entityRewriter = new EntityPackets1_19(this);
    private final BlockItemPackets1_19 blockItemPackets = new BlockItemPackets1_19(this);
    private final TranslatableRewriter translatableRewriter = new TranslatableRewriter(this);

    public Protocol1_18_2To1_19() {
        super(ClientboundPackets1_19.class, ClientboundPackets1_18.class, ServerboundPackets1_19.class, ServerboundPackets1_17.class);
    }

    @Override
    protected void registerPackets() {
        executeAsyncAfterLoaded(Protocol1_19To1_18_2.class, () -> {
            MAPPINGS.load();
            entityRewriter.onMappingDataLoaded();
        });

        //TODO update translation mappings on release
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_19.BOSSBAR);
        translatableRewriter.registerDisconnect(ClientboundPackets1_19.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19.TAB_LIST);
        translatableRewriter.registerOpenWindow(ClientboundPackets1_19.OPEN_WINDOW);
        translatableRewriter.registerCombatKill(ClientboundPackets1_19.COMBAT_KILL);
        translatableRewriter.registerPing();

        blockItemPackets.register();
        entityRewriter.register();

        final SoundRewriter soundRewriter = new SoundRewriter(this);
        soundRewriter.registerStopSound(ClientboundPackets1_19.STOP_SOUND);
        registerClientbound(ClientboundPackets1_19.SOUND, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Sound id
                map(Type.VAR_INT); // Source
                map(Type.INT); // X
                map(Type.INT); // Y
                map(Type.INT); // Z
                map(Type.FLOAT); // Volume
                map(Type.FLOAT); // Pitch
                read(Type.LONG); // Seed
                handler(soundRewriter.getSoundHandler());
            }
        });
        registerClientbound(ClientboundPackets1_19.ENTITY_SOUND, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Sound id
                map(Type.VAR_INT); // Source
                map(Type.VAR_INT); // Entity id
                map(Type.FLOAT); // Volume
                map(Type.FLOAT); // Pitch
                read(Type.LONG); // Seed
                handler(soundRewriter.getSoundHandler());
            }
        });
        registerClientbound(ClientboundPackets1_19.NAMED_SOUND, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Sound name
                map(Type.VAR_INT); // Source
                map(Type.INT); // X
                map(Type.INT); // Y
                map(Type.INT); // Z
                map(Type.FLOAT); // Volume
                map(Type.FLOAT); // Pitch
                read(Type.LONG); // Seed
                handler(soundRewriter.getNamedSoundHandler());
            }
        });

        final TagRewriter tagRewriter = new TagRewriter(this);
        tagRewriter.removeTags("minecraft:banner_pattern");
        tagRewriter.removeTags("minecraft:instrument");
        tagRewriter.removeTags("minecraft:cat_variant");
        tagRewriter.removeTags("minecraft:painting_variant");
        tagRewriter.addEmptyTag(RegistryType.BLOCK, "minecraft:polar_bears_spawnable_on_in_frozen_ocean");
        tagRewriter.renameTag(RegistryType.BLOCK, "minecraft:wool_carpets", "minecraft:carpets");
        tagRewriter.renameTag(RegistryType.ITEM, "minecraft:wool_carpets", "minecraft:carpets");
        tagRewriter.addEmptyTag(RegistryType.ITEM, "minecraft:occludes_vibration_signals");
        tagRewriter.registerGeneric(ClientboundPackets1_19.TAGS);

        new StatisticsRewriter(this).register(ClientboundPackets1_19.STATISTICS);

        final CommandRewriter commandRewriter = new CommandRewriter1_19(this);
        registerClientbound(ClientboundPackets1_19.DECLARE_COMMANDS, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    final int size = wrapper.passthrough(Type.VAR_INT);
                    for (int i = 0; i < size; i++) {
                        final byte flags = wrapper.passthrough(Type.BYTE);
                        wrapper.passthrough(Type.VAR_INT_ARRAY_PRIMITIVE); // Children indices
                        if ((flags & 0x08) != 0) {
                            wrapper.passthrough(Type.VAR_INT); // Redirect node index
                        }

                        final int nodeType = flags & 0x03;
                        if (nodeType == 1 || nodeType == 2) { // Literal/argument node
                            wrapper.passthrough(Type.STRING); // Name
                        }

                        if (nodeType == 2) { // Argument node
                            final int argumentTypeId = wrapper.read(Type.VAR_INT);
                            String argumentType = MAPPINGS.argumentType(argumentTypeId);
                            if (argumentType == null) {
                                ViaBackwards.getPlatform().getLogger().warning("Unknown command argument type id: " + argumentTypeId);
                                argumentType = "minecraft:no";
                            }

                            wrapper.write(Type.STRING, commandRewriter.handleArgumentType(argumentType));
                            commandRewriter.handleArgument(wrapper, argumentType);

                            if ((flags & 0x10) != 0) {
                                wrapper.passthrough(Type.STRING); // Suggestion type
                            }
                        }
                    }

                    wrapper.passthrough(Type.VAR_INT); // Root node index
                });
            }
        });

        cancelClientbound(ClientboundPackets1_19.SERVER_DATA);
        cancelClientbound(ClientboundPackets1_19.CHAT_PREVIEW);
        cancelClientbound(ClientboundPackets1_19.SET_DISPLAY_CHAT_PREVIEW);
        registerClientbound(ClientboundPackets1_19.PLAYER_CHAT, ClientboundPackets1_18.CHAT_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    // Send the unsigned message if present, otherwise the signed message
                    final JsonElement message = wrapper.read(Type.COMPONENT);
                    final JsonElement unsignedMessage = wrapper.read(Type.OPTIONAL_COMPONENT);
                    wrapper.write(Type.COMPONENT, unsignedMessage != null ? unsignedMessage : message);
                });
                handler(wrapper -> {
                    final int chatTypeId = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.BYTE, (byte) 1);
                    wrapper.passthrough(Type.UUID);
                    final JsonElement senderName = wrapper.read(Type.COMPONENT);
                    final JsonElement teamName = wrapper.read(Type.OPTIONAL_COMPONENT);
                    final JsonElement element = wrapper.get(Type.COMPONENT, 0);
                    final JsonElement decoratedMessage = decorateChatMessage(wrapper, chatTypeId, senderName, teamName, element);
                    if (decoratedMessage == null) {
                        wrapper.cancel();
                    } else {
                        wrapper.set(Type.COMPONENT, 0, decoratedMessage);
                    }
                });
                read(Type.LONG); // Timestamp
                read(Type.LONG); // Salt
                read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
            }
        });

        registerClientbound(ClientboundPackets1_19.SYSTEM_CHAT, ClientboundPackets1_18.CHAT_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.COMPONENT); // Message
                read(Type.VAR_INT);
                create(Type.BYTE, (byte) 0);
                create(Type.UUID, ZERO_UUID); // Sender
            }
        });

        registerServerbound(ServerboundPackets1_17.CHAT_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Message
                create(Type.LONG, Instant.now().toEpochMilli()); // Timestamp
                create(Type.LONG, 0L); // Salt
                handler(wrapper -> {
                    final String message = wrapper.get(Type.STRING, 0);
                    if (!message.isEmpty() && message.charAt(0) == '/') {
                        wrapper.setPacketType(ServerboundPackets1_19.CHAT_COMMAND);
                        wrapper.set(Type.STRING, 0, message.substring(1));
                        wrapper.write(Type.VAR_INT, 0); // No signatures
                        wrapper.write(Type.BOOLEAN, false); // No signed preview
                    } else {
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES); // Signature
                        wrapper.write(Type.BOOLEAN, false); // No signed preview
                    }
                });
            }
        });

        // Login changes
        registerClientbound(State.LOGIN, ClientboundLoginPackets.GAME_PROFILE.getId(), ClientboundLoginPackets.GAME_PROFILE.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UUID); // UUID
                map(Type.STRING); // Name
                handler(wrapper -> {
                    final int properties = wrapper.read(Type.VAR_INT);
                    for (int i = 0; i < properties; i++) {
                        wrapper.read(Type.STRING); // Name
                        wrapper.read(Type.STRING); // Value
                        if (wrapper.read(Type.BOOLEAN)) {
                            wrapper.read(Type.STRING); // Signature
                        }
                    }
                });
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO.getId(), ServerboundLoginPackets.HELLO.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Name
                create(Type.OPTIONAL_PROFILE_KEY, null); // No public key - requiring this has to be disabled on the server
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.ENCRYPTION_KEY.getId(), ServerboundLoginPackets.ENCRYPTION_KEY.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE_ARRAY_PRIMITIVE); // Keys
                create(Type.BOOLEAN, true); // Is nonce
            }
        });
    }

    @Override
    public void init(final UserConnection user) {
        user.put(new DimensionRegistryStorage());
        addEntityTracker(user, new EntityTrackerBase(user, Entity1_19Types.PLAYER, true));
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public TranslatableRewriter getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public EntityRewriter getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public ItemRewriter getItemRewriter() {
        return blockItemPackets;
    }

    private TextReplacementConfig replace(final JsonElement replacement) {
        return TextReplacementConfig.builder().matchLiteral("%s").replacement(GsonComponentSerializer.gson().deserializeFromTree(replacement)).once().build();
    }

    private JsonElement decorateChatMessage(final PacketWrapper wrapper, final int chatTypeId, final JsonElement senderName, final JsonElement teamName, final JsonElement message) throws Exception {
        translatableRewriter.processText(message);

        CompoundTag chatType = wrapper.user().get(DimensionRegistryStorage.class).chatType(chatTypeId);
        if (chatType == null) {
            ViaBackwards.getPlatform().getLogger().warning("Chat message has unknown chat type id " + chatTypeId + ". Message: " + message);
            return null;
        }

        chatType = chatType.<CompoundTag> get("element").get("chat");
        if (chatType == null) {
            return null;
        }

        String translationKey = (String) chatType.get("translation_key").getValue();
        String rawTranslation = ViaBackwards.getConfig().chatTypeFormat(translationKey);
        if (rawTranslation == null) {
            ViaBackwards.getPlatform().getLogger().warning("Missing chat type translation for key " + translationKey);
            return null;
        }

        Component component = Component.text(rawTranslation);

        CompoundTag style = chatType.get("style");
        if (style != null) {
            StringTag color = style.get("color");
            if (color != null && NamedTextColor.NAMES.value(color.getValue()) != null) {
                component = component.color(NamedTextColor.NAMES.value(color.getValue()));
            }
            for (String key : TextDecoration.NAMES.keys()) {
                if (style.contains(key) && style.<ByteTag> get(key).asByte() == 1) {
                    component = component.decorate(TextDecoration.NAMES.value(key));
                }
            }
        }

        ListTag parameters = chatType.get("parameters");
        if (parameters != null) for (Tag element : parameters) {
            switch ((String) element.getValue()) {
                case "sender":
                    component = component.replaceText(replace(senderName));
                    break;
                case "content":
                    component = component.replaceText(replace(message));
                    break;
                case "team_name":
                    Preconditions.checkNotNull(teamName, "Team name is null");
                    component = component.replaceText(replace(teamName));
                    break;
                default:
                    ViaBackwards.getPlatform().getLogger().warning("Unknown parameter for chat decoration: " + element.getValue());
            }
        }

        return GsonComponentSerializer.gson().serializeToTree(component);
    }
}
