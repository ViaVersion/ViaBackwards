/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_17to1_16_4;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.rewriter.BlockItemPacketRewriter1_17;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.rewriter.EntityPacketRewriter1_17;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.storage.PlayerLastCursorItem;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.TagData;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_17;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.fastutil.ints.IntArrayList;
import com.viaversion.viaversion.libs.fastutil.ints.IntList;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.rewriter.IdRewriteFunction;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;
import com.viaversion.viaversion.util.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Protocol1_17To1_16_4 extends BackwardsProtocol<ClientboundPackets1_17, ClientboundPackets1_16_2, ServerboundPackets1_17, ServerboundPackets1_16_2> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.17", "1.16.2", Protocol1_16_4To1_17.class);
    private static final RegistryType[] TAG_REGISTRY_TYPES = {RegistryType.BLOCK, RegistryType.ITEM, RegistryType.FLUID, RegistryType.ENTITY};
    private static final int[] EMPTY_ARRAY = {};
    private final EntityPacketRewriter1_17 entityRewriter = new EntityPacketRewriter1_17(this);
    private final BlockItemPacketRewriter1_17 blockItemPackets = new BlockItemPacketRewriter1_17(this);
    private final ParticleRewriter<ClientboundPackets1_17> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPackets1_17> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
    private final TagRewriter<ClientboundPackets1_17> tagRewriter = new TagRewriter<>(this);

    public Protocol1_17To1_16_4() {
        super(ClientboundPackets1_17.class, ClientboundPackets1_16_2.class, ServerboundPackets1_17.class, ServerboundPackets1_16_2.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerComponentPacket(ClientboundPackets1_17.CHAT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_17.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_17.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_17.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_17.SET_PLAYER_TEAM);
        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_17.OPEN_SCREEN);
        translatableRewriter.registerSetObjective(ClientboundPackets1_17.SET_OBJECTIVE);
        translatableRewriter.registerPing();

        SoundRewriter<ClientboundPackets1_17> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound(ClientboundPackets1_17.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_17.SOUND_ENTITY);
        soundRewriter.registerNamedSound(ClientboundPackets1_17.CUSTOM_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_17.STOP_SOUND);

        registerClientbound(ClientboundPackets1_17.UPDATE_TAGS, wrapper -> {
            Map<String, List<TagData>> tags = new HashMap<>();

            int length = wrapper.read(Types.VAR_INT);
            for (int i = 0; i < length; i++) {
                String resourceKey = Key.stripMinecraftNamespace(wrapper.read(Types.STRING));
                List<TagData> tagList = new ArrayList<>();
                tags.put(resourceKey, tagList);

                int tagLength = wrapper.read(Types.VAR_INT);
                for (int j = 0; j < tagLength; j++) {
                    String identifier = wrapper.read(Types.STRING);
                    int[] entries = wrapper.read(Types.VAR_INT_ARRAY_PRIMITIVE);
                    tagList.add(new TagData(identifier, entries));
                }
            }

            // Put them into the hardcoded order of Vanilla tags (and only those), rewrite ids
            for (RegistryType type : TAG_REGISTRY_TYPES) {
                List<TagData> tagList = tags.get(type.identifier());
                if (tagList == null) {
                    // Higher versions may not send the otherwise expected tags
                    wrapper.write(Types.VAR_INT, 0);
                    continue;
                }

                IdRewriteFunction rewriter = tagRewriter.getRewriter(type);

                wrapper.write(Types.VAR_INT, tagList.size());
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

                    wrapper.write(Types.STRING, tagData.identifier());
                    wrapper.write(Types.VAR_INT_ARRAY_PRIMITIVE, entries);
                }
            }
        });

        new StatisticsRewriter<>(this).register(ClientboundPackets1_17.AWARD_STATS);

        registerClientbound(ClientboundPackets1_17.RESOURCE_PACK, wrapper -> {
            wrapper.passthrough(Types.STRING);
            wrapper.passthrough(Types.STRING);
            wrapper.read(Types.BOOLEAN); // Required
            wrapper.read(Types.OPTIONAL_COMPONENT); // Prompt message
        });

        registerClientbound(ClientboundPackets1_17.EXPLODE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.FLOAT); // X
                map(Types.FLOAT); // Y
                map(Types.FLOAT); // Z
                map(Types.FLOAT); // Strength
                handler(wrapper -> {
                    wrapper.write(Types.INT, wrapper.read(Types.VAR_INT)); // Collection length
                });
            }
        });

        registerClientbound(ClientboundPackets1_17.SET_DEFAULT_SPAWN_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14);
                handler(wrapper -> {
                    // Angle (which Mojang just forgot to write to the buffer, lol)
                    wrapper.read(Types.FLOAT);
                });
            }
        });

        registerClientbound(ClientboundPackets1_17.PING, null, wrapper -> {
            wrapper.cancel();

            int id = wrapper.read(Types.INT);
            short shortId = (short) id;
            if (id == shortId && ViaBackwards.getConfig().handlePingsAsInvAcknowledgements()) {
                // Send inventory acknowledgement to replace ping packet functionality in the unsigned byte range
                PacketWrapper acknowledgementPacket = wrapper.create(ClientboundPackets1_16_2.CONTAINER_ACK);
                acknowledgementPacket.write(Types.UNSIGNED_BYTE, (short) 0); // Inventory id
                acknowledgementPacket.write(Types.SHORT, shortId); // Confirmation id
                acknowledgementPacket.write(Types.BOOLEAN, false); // Accepted
                acknowledgementPacket.send(Protocol1_17To1_16_4.class);
                return;
            }

            // Plugins expecting a real response will have to handle this accordingly themselves
            PacketWrapper pongPacket = wrapper.create(ServerboundPackets1_17.PONG);
            pongPacket.write(Types.INT, id);
            pongPacket.sendToServer(Protocol1_17To1_16_4.class);
        });

        registerServerbound(ServerboundPackets1_16_2.CLIENT_INFORMATION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Locale
                map(Types.BYTE); // View distance
                map(Types.VAR_INT); // Chat mode
                map(Types.BOOLEAN); // Chat colors
                map(Types.UNSIGNED_BYTE); // Chat flags
                map(Types.VAR_INT); // Main hand
                handler(wrapper -> {
                    wrapper.write(Types.BOOLEAN, false); // Text filtering
                });
            }
        });

        rewriteTitlePacket(ClientboundPackets1_17.SET_TITLE_TEXT, 0);
        rewriteTitlePacket(ClientboundPackets1_17.SET_SUBTITLE_TEXT, 1);
        rewriteTitlePacket(ClientboundPackets1_17.SET_ACTION_BAR_TEXT, 2);

        mergePacket(ClientboundPackets1_17.SET_TITLES_ANIMATION, ClientboundPackets1_16_2.SET_TITLES, 3);
        registerClientbound(ClientboundPackets1_17.CLEAR_TITLES, ClientboundPackets1_16_2.SET_TITLES, wrapper -> {
            if (wrapper.read(Types.BOOLEAN)) {
                wrapper.write(Types.VAR_INT, 5); // Reset times
            } else {
                wrapper.write(Types.VAR_INT, 4); // Simple clear
            }
        });

        cancelClientbound(ClientboundPackets1_17.ADD_VIBRATION_SIGNAL);
    }

    @Override
    public void init(UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_17.PLAYER));
        user.put(new PlayerLastCursorItem());
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPackets1_17> getComponentRewriter() {
        return translatableRewriter;
    }

    public void mergePacket(ClientboundPackets1_17 newPacketType, ClientboundPackets1_16_2 oldPacketType, int type) {
        // A few packets that had different handling based on an initially read enum type were split into different ones
        registerClientbound(newPacketType, oldPacketType, wrapper -> wrapper.write(Types.VAR_INT, type));
    }

    private void rewriteTitlePacket(ClientboundPackets1_17 newPacketType, int type) {
        // Also handles translations in the title
        registerClientbound(newPacketType, ClientboundPackets1_16_2.SET_TITLES, wrapper -> {
            wrapper.write(Types.VAR_INT, type);
            translatableRewriter.processText(wrapper.user(), wrapper.passthrough(Types.COMPONENT));
        });
    }

    @Override
    public EntityPacketRewriter1_17 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_17 getItemRewriter() {
        return blockItemPackets;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_17> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_17> getTagRewriter() {
        return tagRewriter;
    }
}
