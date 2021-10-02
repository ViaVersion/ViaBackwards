/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_16_4to1_17;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.packets.BlockItemPackets1_17;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.packets.EntityPackets1_17;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.storage.PingRequests;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.TagData;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_17Types;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.rewriter.EntityRewriter;
import com.viaversion.viaversion.api.rewriter.ItemRewriter;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.fastutil.ints.IntArrayList;
import com.viaversion.viaversion.libs.fastutil.ints.IntList;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.Protocol1_17To1_16_4;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.rewriter.IdRewriteFunction;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Protocol1_16_4To1_17 extends BackwardsProtocol<ClientboundPackets1_17, ClientboundPackets1_16_2, ServerboundPackets1_17, ServerboundPackets1_16_2> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.17", "1.16.2", Protocol1_17To1_16_4.class, true);
    private static final int[] EMPTY_ARRAY = {};
    private final EntityRewriter entityRewriter = new EntityPackets1_17(this);
    private BlockItemPackets1_17 blockItemPackets;
    private TranslatableRewriter translatableRewriter;

    public Protocol1_16_4To1_17() {
        super(ClientboundPackets1_17.class, ClientboundPackets1_16_2.class, ServerboundPackets1_17.class, ServerboundPackets1_16_2.class);
    }

    @Override
    protected void registerPackets() {
        executeAsyncAfterLoaded(Protocol1_17To1_16_4.class, MAPPINGS::load);

        translatableRewriter = new TranslatableRewriter(this);
        translatableRewriter.registerChatMessage(ClientboundPackets1_17.CHAT_MESSAGE);
        translatableRewriter.registerBossBar(ClientboundPackets1_17.BOSSBAR);
        translatableRewriter.registerDisconnect(ClientboundPackets1_17.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_17.TAB_LIST);
        translatableRewriter.registerOpenWindow(ClientboundPackets1_17.OPEN_WINDOW);
        translatableRewriter.registerPing();

        blockItemPackets = new BlockItemPackets1_17(this, translatableRewriter);
        blockItemPackets.register();

        entityRewriter.register();

        SoundRewriter soundRewriter = new SoundRewriter(this);
        soundRewriter.registerSound(ClientboundPackets1_17.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_17.ENTITY_SOUND);
        soundRewriter.registerNamedSound(ClientboundPackets1_17.NAMED_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_17.STOP_SOUND);

        TagRewriter tagRewriter = new TagRewriter(this);
        registerClientbound(ClientboundPackets1_17.TAGS, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    Map<String, List<TagData>> tags = new HashMap<>();

                    int length = wrapper.read(Type.VAR_INT);
                    for (int i = 0; i < length; i++) {
                        String resourceKey = wrapper.read(Type.STRING);
                        if (resourceKey.startsWith("minecraft:")) {
                            resourceKey = resourceKey.substring(10);
                        }

                        List<TagData> tagList = new ArrayList<>();
                        tags.put(resourceKey, tagList);

                        int tagLength = wrapper.read(Type.VAR_INT);
                        for (int j = 0; j < tagLength; j++) {
                            String identifier = wrapper.read(Type.STRING);
                            int[] entries = wrapper.read(Type.VAR_INT_ARRAY_PRIMITIVE);
                            tagList.add(new TagData(identifier, entries));
                        }
                    }

                    // Put them into the hardcoded order of Vanilla tags (and only those), rewrite ids
                    for (RegistryType type : RegistryType.getValues()) {
                        List<TagData> tagList = tags.get(type.getResourceLocation());
                        IdRewriteFunction rewriter = tagRewriter.getRewriter(type);

                        wrapper.write(Type.VAR_INT, tagList.size());
                        for (TagData tagData : tagList) {
                            int[] entries = tagData.entries();
                            if (rewriter != null) {
                                // Handle id rewriting now
                                IntList idList = new IntArrayList(entries.length);
                                for (int id : entries) {
                                    int mappedId = rewriter.rewrite(id);
                                    if (mappedId != -1) {
                                        idList.add(mappedId);
                                    }
                                }
                                entries = idList.toArray(EMPTY_ARRAY);
                            }

                            wrapper.write(Type.STRING, tagData.identifier());
                            wrapper.write(Type.VAR_INT_ARRAY_PRIMITIVE, entries);
                        }

                        // Stop after the entity types
                        if (type == RegistryType.ENTITY) {
                            break;
                        }
                    }
                });
            }
        });

        new StatisticsRewriter(this).register(ClientboundPackets1_17.STATISTICS);

        registerClientbound(ClientboundPackets1_17.RESOURCE_PACK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.passthrough(Type.STRING);
                    wrapper.passthrough(Type.STRING);
                    wrapper.read(Type.BOOLEAN); // Required
                    wrapper.read(Type.OPTIONAL_COMPONENT); // Prompt message
                });
            }
        });

        registerClientbound(ClientboundPackets1_17.EXPLOSION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.FLOAT); // X
                map(Type.FLOAT); // Y
                map(Type.FLOAT); // Z
                map(Type.FLOAT); // Strength
                handler(wrapper -> {
                    wrapper.write(Type.INT, wrapper.read(Type.VAR_INT)); // Collection length
                });
            }
        });

        registerClientbound(ClientboundPackets1_17.SPAWN_POSITION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14);
                handler(wrapper -> {
                    // Angle (which Mojang just forgot to write to the buffer, lol)
                    wrapper.read(Type.FLOAT);
                });
            }
        });

        registerClientbound(ClientboundPackets1_17.PING, null, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.cancel();

                    int id = wrapper.read(Type.INT);
                    if (ViaBackwards.getConfig().handlePingsAsInvAcknowledgements()) {
                        short confirmationId = wrapper.user().get(PingRequests.class).addId(id);

                        // Send inventory acknowledgement to replace ping packet functionality in the unsigned byte range
                        PacketWrapper acknowledgementPacket = wrapper.create(ClientboundPackets1_16_2.WINDOW_CONFIRMATION);
                        acknowledgementPacket.write(Type.UNSIGNED_BYTE, (short) 0); // Inventory id
                        acknowledgementPacket.write(Type.SHORT, confirmationId); // Confirmation id
                        acknowledgementPacket.write(Type.BOOLEAN, false); // Accepted
                        acknowledgementPacket.send(Protocol1_16_4To1_17.class);
                        return;
                    }

                    // Plugins expecting a real response will have to handle this accordingly themselves
                    PacketWrapper pongPacket = wrapper.create(ServerboundPackets1_17.PONG);
                    pongPacket.write(Type.INT, id);
                    pongPacket.sendToServer(Protocol1_16_4To1_17.class);
                });
            }
        });

        registerServerbound(ServerboundPackets1_16_2.CLIENT_SETTINGS, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Locale
                map(Type.BYTE); // View distance
                map(Type.VAR_INT); // Chat mode
                map(Type.BOOLEAN); // Chat colors
                map(Type.UNSIGNED_BYTE); // Chat flags
                map(Type.VAR_INT); // Main hand
                handler(wrapper -> {
                    wrapper.write(Type.BOOLEAN, false); // Text filtering
                });
            }
        });

        mergePacket(ClientboundPackets1_17.TITLE_TEXT, ClientboundPackets1_16_2.TITLE, 0);
        mergePacket(ClientboundPackets1_17.TITLE_SUBTITLE, ClientboundPackets1_16_2.TITLE, 1);
        mergePacket(ClientboundPackets1_17.ACTIONBAR, ClientboundPackets1_16_2.TITLE, 2);
        mergePacket(ClientboundPackets1_17.TITLE_TIMES, ClientboundPackets1_16_2.TITLE, 3);
        registerClientbound(ClientboundPackets1_17.CLEAR_TITLES, ClientboundPackets1_16_2.TITLE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    if (wrapper.read(Type.BOOLEAN)) {
                        wrapper.write(Type.VAR_INT, 5); // Reset times
                    } else {
                        wrapper.write(Type.VAR_INT, 4); // Simple clear
                    }
                });
            }
        });

        cancelClientbound(ClientboundPackets1_17.ADD_VIBRATION_SIGNAL);
    }

    @Override
    public void init(UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, Entity1_17Types.PLAYER));
        user.put(new PingRequests());
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public TranslatableRewriter getTranslatableRewriter() {
        return translatableRewriter;
    }

    public void mergePacket(ClientboundPackets1_17 newPacketType, ClientboundPackets1_16_2 oldPacketType, int type) {
        // A few packets that had different handling based on an initially read enum type were split into different ones
        registerClientbound(newPacketType, oldPacketType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.write(Type.VAR_INT, type);
                });
            }
        });
    }

    @Override
    public EntityRewriter getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public ItemRewriter getItemRewriter() {
        return blockItemPackets;
    }
}
