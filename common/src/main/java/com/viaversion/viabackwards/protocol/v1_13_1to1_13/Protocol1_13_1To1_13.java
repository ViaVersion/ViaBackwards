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
package com.viaversion.viabackwards.protocol.v1_13_1to1_13;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_13_1to1_13.rewriter.CommandRewriter1_13_1;
import com.viaversion.viabackwards.protocol.v1_13_1to1_13.rewriter.EntityPacketRewriter1_13_1;
import com.viaversion.viabackwards.protocol.v1_13_1to1_13.rewriter.ItemPacketRewriter1_13_1;
import com.viaversion.viabackwards.protocol.v1_13_1to1_13.rewriter.WorldPacketRewriter1_13_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.remapper.ValueTransformer;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_13to1_13_1.Protocol1_13To1_13_1;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;
import com.viaversion.viaversion.util.ComponentUtil;

public class Protocol1_13_1To1_13 extends BackwardsProtocol<ClientboundPackets1_13, ClientboundPackets1_13, ServerboundPackets1_13, ServerboundPackets1_13> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.13.2", "1.13", Protocol1_13To1_13_1.class);
    private final EntityPacketRewriter1_13_1 entityRewriter = new EntityPacketRewriter1_13_1(this);
    private final ItemPacketRewriter1_13_1 itemRewriter = new ItemPacketRewriter1_13_1(this);
    private final ParticleRewriter<ClientboundPackets1_13> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPackets1_13> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
    private final TagRewriter<ClientboundPackets1_13> tagRewriter = new TagRewriter<>(this);

    public Protocol1_13_1To1_13() {
        super(ClientboundPackets1_13.class, ClientboundPackets1_13.class, ServerboundPackets1_13.class, ServerboundPackets1_13.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        WorldPacketRewriter1_13_1.register(this);

        translatableRewriter.registerComponentPacket(ClientboundPackets1_13.CHAT);
        translatableRewriter.registerPlayerCombat(ClientboundPackets1_13.PLAYER_COMBAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_13.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_13.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_13.SET_PLAYER_TEAM);
        translatableRewriter.registerTitle(ClientboundPackets1_13.SET_TITLES);
        translatableRewriter.registerSetObjective(ClientboundPackets1_13.SET_OBJECTIVE);
        translatableRewriter.registerPing();

        new CommandRewriter1_13_1(this).registerDeclareCommands(ClientboundPackets1_13.COMMANDS);

        particleRewriter.registerLevelParticles1_13(ClientboundPackets1_13.LEVEL_PARTICLES, Types.FLOAT);

        registerServerbound(ServerboundPackets1_13.COMMAND_SUGGESTION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.STRING, new ValueTransformer<>(Types.STRING) {
                    @Override
                    public String transform(PacketWrapper wrapper, String inputValue) {
                        // 1.13 starts sending slash at start, so we remove it for compatibility
                        return !inputValue.startsWith("/") ? "/" + inputValue : inputValue;
                    }
                });
            }
        });

        registerServerbound(ServerboundPackets1_13.EDIT_BOOK, wrapper -> {
            final Item item = itemRewriter.handleItemToServer(wrapper.user(), wrapper.read(Types.ITEM1_13));
            wrapper.write(Types.ITEM1_13, item);
            wrapper.passthrough(Types.BOOLEAN);
            wrapper.write(Types.VAR_INT, 0);
        });

        registerClientbound(ClientboundPackets1_13.OPEN_SCREEN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE); // Id
                map(Types.STRING); // Window Type
                handler(wrapper -> {
                    JsonElement title = wrapper.passthrough(Types.COMPONENT);
                    translatableRewriter.processText(wrapper.user(), title);

                    if (ViaBackwards.getConfig().fix1_13FormattedInventoryTitle()) {
                        if (title.isJsonObject() && title.getAsJsonObject().size() == 1
                            && title.getAsJsonObject().has("translate")) {
                            // Hotfix simple translatable components from being converted to legacy text
                            return;
                        }

                        // https://bugs.mojang.com/browse/MC-124543
                        JsonObject legacyComponent = new JsonObject();
                        legacyComponent.addProperty("text", ComponentUtil.jsonToLegacy(title));
                        wrapper.set(Types.COMPONENT, 0, legacyComponent);
                    }
                });
            }
        });

        registerClientbound(ClientboundPackets1_13.COMMAND_SUGGESTIONS, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Transaction id
                map(Types.VAR_INT); // Start
                map(Types.VAR_INT); // Length
                map(Types.VAR_INT); // Count
                handler(wrapper -> {
                    int start = wrapper.get(Types.VAR_INT, 1);
                    wrapper.set(Types.VAR_INT, 1, start - 1); // Offset by +1 to take into account / at beginning
                    // Passthrough suggestions
                    int count = wrapper.get(Types.VAR_INT, 3);
                    for (int i = 0; i < count; i++) {
                        wrapper.passthrough(Types.STRING);
                        wrapper.passthrough(Types.OPTIONAL_COMPONENT); // Tooltip
                    }
                });
            }
        });

        registerClientbound(ClientboundPackets1_13.BOSS_EVENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UUID);
                map(Types.VAR_INT);
                handler(wrapper -> {
                    int action = wrapper.get(Types.VAR_INT, 0);
                    if (action == 0 || action == 3) {
                        translatableRewriter.processText(wrapper.user(), wrapper.passthrough(Types.COMPONENT));
                        if (action == 0) {
                            wrapper.passthrough(Types.FLOAT);
                            wrapper.passthrough(Types.VAR_INT);
                            wrapper.passthrough(Types.VAR_INT);
                            short flags = wrapper.read(Types.UNSIGNED_BYTE);
                            if ((flags & 0x04) != 0) flags |= 0x02;
                            wrapper.write(Types.UNSIGNED_BYTE, flags);
                        }
                    }
                });
            }
        });

        registerClientbound(ClientboundPackets1_13.UPDATE_ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Types.BOOLEAN); // Reset/clear
            int size = wrapper.passthrough(Types.VAR_INT); // Mapping size

            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Types.STRING); // Identifier
                wrapper.passthrough(Types.OPTIONAL_STRING); // Parent

                // Display data
                if (wrapper.passthrough(Types.BOOLEAN)) {
                    wrapper.passthrough(Types.COMPONENT); // Title
                    wrapper.passthrough(Types.COMPONENT); // Description
                    Item icon = itemRewriter.handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_13));
                    wrapper.write(Types.ITEM1_13, icon);
                    wrapper.passthrough(Types.VAR_INT); // Frame type
                    int flags = wrapper.passthrough(Types.INT); // Flags
                    if ((flags & 1) != 0)
                        wrapper.passthrough(Types.STRING); // Background texture
                    wrapper.passthrough(Types.FLOAT); // X
                    wrapper.passthrough(Types.FLOAT); // Y
                }

                wrapper.passthrough(Types.STRING_ARRAY); // Criteria

                int arrayLength = wrapper.passthrough(Types.VAR_INT);
                for (int array = 0; array < arrayLength; array++) {
                    wrapper.passthrough(Types.STRING_ARRAY); // String array
                }
            }
        });

        tagRewriter.register(ClientboundPackets1_13.UPDATE_TAGS, RegistryType.ITEM);
        new StatisticsRewriter<>(this).

            register(ClientboundPackets1_13.AWARD_STATS);
    }

    @Override
    public void init(UserConnection user) {
        user.addEntityTracker(getClass(), new EntityTrackerBase(user, EntityTypes1_13.EntityType.PLAYER));
        user.addClientWorld(getClass(), new ClientWorld());
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_13_1 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public ItemPacketRewriter1_13_1 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_13> getParticleRewriter() {
        return particleRewriter;
    }

    public JsonNBTComponentRewriter<ClientboundPackets1_13> translatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_13> getTagRewriter() {
        return tagRewriter;
    }
}
