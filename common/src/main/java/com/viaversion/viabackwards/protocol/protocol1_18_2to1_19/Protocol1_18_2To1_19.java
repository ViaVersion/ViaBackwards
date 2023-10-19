/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.data.BackwardsMappings;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.data.CommandRewriter1_19;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.packets.BlockItemPackets1_19;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.packets.EntityPackets1_19;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.storage.DimensionRegistryStorage;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.Protocol1_19To1_19_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ChatDecorationResult;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.Protocol1_19_1To1_19;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ServerboundPackets1_19;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
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
    private final TranslatableRewriter<ClientboundPackets1_19> translatableRewriter = new TranslatableRewriter<>(this, ComponentRewriter.ReadType.JSON);

    public Protocol1_18_2To1_19() {
        super(ClientboundPackets1_19.class, ClientboundPackets1_18.class, ServerboundPackets1_19.class, ServerboundPackets1_17.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_19.BOSSBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19.TAB_LIST);
        translatableRewriter.registerOpenWindow(ClientboundPackets1_19.OPEN_WINDOW);
        translatableRewriter.registerCombatKill(ClientboundPackets1_19.COMBAT_KILL);
        translatableRewriter.registerPing();

        final SoundRewriter<ClientboundPackets1_19> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerStopSound(ClientboundPackets1_19.STOP_SOUND);
        registerClientbound(ClientboundPackets1_19.SOUND, new PacketHandlers() {
            @Override
            public void register() {
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
        registerClientbound(ClientboundPackets1_19.ENTITY_SOUND, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Sound id
                map(Type.VAR_INT); // Source
                map(Type.VAR_INT); // Entity id
                map(Type.FLOAT); // Volume
                map(Type.FLOAT); // Pitch
                read(Type.LONG); // Seed
                handler(soundRewriter.getSoundHandler());
            }
        });
        registerClientbound(ClientboundPackets1_19.NAMED_SOUND, new PacketHandlers() {
            @Override
            public void register() {
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

        final TagRewriter<ClientboundPackets1_19> tagRewriter = new TagRewriter<>(this);
        tagRewriter.removeTags("minecraft:banner_pattern");
        tagRewriter.removeTags("minecraft:instrument");
        tagRewriter.removeTags("minecraft:cat_variant");
        tagRewriter.removeTags("minecraft:painting_variant");
        tagRewriter.addEmptyTag(RegistryType.BLOCK, "minecraft:polar_bears_spawnable_on_in_frozen_ocean");
        tagRewriter.renameTag(RegistryType.BLOCK, "minecraft:wool_carpets", "minecraft:carpets");
        tagRewriter.renameTag(RegistryType.ITEM, "minecraft:wool_carpets", "minecraft:carpets");
        tagRewriter.addEmptyTag(RegistryType.ITEM, "minecraft:occludes_vibration_signals");
        tagRewriter.registerGeneric(ClientboundPackets1_19.TAGS);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_19.STATISTICS);

        final CommandRewriter<ClientboundPackets1_19> commandRewriter = new CommandRewriter1_19(this);
        registerClientbound(ClientboundPackets1_19.DECLARE_COMMANDS, wrapper -> {
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
                    String argumentType = MAPPINGS.getArgumentTypeMappings().identifier(argumentTypeId);
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

        cancelClientbound(ClientboundPackets1_19.SERVER_DATA);
        cancelClientbound(ClientboundPackets1_19.CHAT_PREVIEW);
        cancelClientbound(ClientboundPackets1_19.SET_DISPLAY_CHAT_PREVIEW);
        registerClientbound(ClientboundPackets1_19.PLAYER_CHAT, ClientboundPackets1_18.CHAT_MESSAGE, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    final JsonElement signedContent = wrapper.read(Type.COMPONENT);
                    final JsonElement unsignedContent = wrapper.read(Type.OPTIONAL_COMPONENT);
                    final int chatTypeId = wrapper.read(Type.VAR_INT);
                    final UUID sender = wrapper.read(Type.UUID);
                    final JsonElement senderName = wrapper.read(Type.COMPONENT);
                    final JsonElement teamName = wrapper.read(Type.OPTIONAL_COMPONENT);

                    final CompoundTag chatType = wrapper.user().get(DimensionRegistryStorage.class).chatType(chatTypeId);
                    final ChatDecorationResult decorationResult = Protocol1_19_1To1_19.decorateChatMessage(chatType, chatTypeId, senderName, teamName, unsignedContent != null ? unsignedContent : signedContent);
                    if (decorationResult == null) {
                        wrapper.cancel();
                        return;
                    }

                    translatableRewriter.processText(decorationResult.content());
                    wrapper.write(Type.COMPONENT, decorationResult.content());
                    wrapper.write(Type.BYTE, decorationResult.overlay() ? (byte) 2 : 1);
                    wrapper.write(Type.UUID, sender);
                });
                read(Type.LONG); // Timestamp
                read(Type.LONG); // Salt
                read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
            }
        });

        registerClientbound(ClientboundPackets1_19.SYSTEM_CHAT, ClientboundPackets1_18.CHAT_MESSAGE, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    final JsonElement content = wrapper.passthrough(Type.COMPONENT);
                    translatableRewriter.processText(content);

                    // Screw everything that isn't a system or game info type (which would only happen on funny 1.19.0 servers)
                    final int typeId = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.BYTE, typeId == Protocol1_19To1_19_1.GAME_INFO_ID ? (byte) 2 : (byte) 0);
                });
                create(Type.UUID, ZERO_UUID); // Sender
            }
        });

        registerServerbound(ServerboundPackets1_17.CHAT_MESSAGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Message
                handler(wrapper -> wrapper.write(Type.LONG, Instant.now().toEpochMilli())); // Timestamp
                create(Type.LONG, 0L); // Salt
                handler(wrapper -> {
                    final String message = wrapper.get(Type.STRING, 0);
                    if (!message.isEmpty() && message.charAt(0) == '/') {
                        wrapper.setPacketType(ServerboundPackets1_19.CHAT_COMMAND);
                        wrapper.set(Type.STRING, 0, message.substring(1));
                        wrapper.write(Type.VAR_INT, 0); // No signatures
                    } else {
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES); // Signature
                    }
                    wrapper.write(Type.BOOLEAN, false); // No signed preview
                });
            }
        });

        // Login changes
        registerClientbound(State.LOGIN, ClientboundLoginPackets.GAME_PROFILE.getId(), ClientboundLoginPackets.GAME_PROFILE.getId(), new PacketHandlers() {
            @Override
            public void register() {
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

        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO.getId(), ServerboundLoginPackets.HELLO.getId(), new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Name
                // Write empty profile key - requires the enforce-secure-profiles option to be disabled on the server
                create(Type.OPTIONAL_PROFILE_KEY, null);
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.ENCRYPTION_KEY.getId(), ServerboundLoginPackets.ENCRYPTION_KEY.getId(), new PacketHandlers() {
            @Override
            public void register() {
                map(Type.BYTE_ARRAY_PRIMITIVE); // Keys
                create(Type.BOOLEAN, true); // Is nonce
            }
        });
    }

    @Override
    public void init(final UserConnection user) {
        user.put(new DimensionRegistryStorage());
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_19.PLAYER));
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public TranslatableRewriter<ClientboundPackets1_19> getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public EntityPackets1_19 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPackets1_19 getItemRewriter() {
        return blockItemPackets;
    }
}
