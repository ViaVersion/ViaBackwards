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
package com.viaversion.viabackwards.protocol.protocol1_13to1_13_1;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_13to1_13_1.data.CommandRewriter1_13_1;
import com.viaversion.viabackwards.protocol.protocol1_13to1_13_1.packets.EntityPackets1_13_1;
import com.viaversion.viabackwards.protocol.protocol1_13to1_13_1.packets.InventoryPackets1_13_1;
import com.viaversion.viabackwards.protocol.protocol1_13to1_13_1.packets.WorldPackets1_13_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_13Types;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.remapper.ValueTransformer;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.protocol1_13_1to1_13.Protocol1_13_1To1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ChatRewriter;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

public class Protocol1_13To1_13_1 extends BackwardsProtocol<ClientboundPackets1_13, ClientboundPackets1_13, ServerboundPackets1_13, ServerboundPackets1_13> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.13.2", "1.13", Protocol1_13_1To1_13.class);
    private final EntityPackets1_13_1 entityRewriter = new EntityPackets1_13_1(this);
    private final InventoryPackets1_13_1 itemRewriter = new InventoryPackets1_13_1(this);

    public Protocol1_13To1_13_1() {
        super(ClientboundPackets1_13.class, ClientboundPackets1_13.class, ServerboundPackets1_13.class, ServerboundPackets1_13.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        WorldPackets1_13_1.register(this);

        TranslatableRewriter<ClientboundPackets1_13> translatableRewriter = new TranslatableRewriter<>(this);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_13.CHAT_MESSAGE);
        translatableRewriter.registerCombatEvent(ClientboundPackets1_13.COMBAT_EVENT);
        translatableRewriter.registerDisconnect(ClientboundPackets1_13.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_13.TAB_LIST);
        translatableRewriter.registerTitle(ClientboundPackets1_13.TITLE);
        translatableRewriter.registerPing();

        new CommandRewriter1_13_1(this).registerDeclareCommands(ClientboundPackets1_13.DECLARE_COMMANDS);

        registerServerbound(ServerboundPackets1_13.TAB_COMPLETE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.STRING, new ValueTransformer<String, String>(Type.STRING) {
                    @Override
                    public String transform(PacketWrapper wrapper, String inputValue) {
                        // 1.13 starts sending slash at start, so we remove it for compatibility
                        return !inputValue.startsWith("/") ? "/" + inputValue : inputValue;
                    }
                });
            }
        });

        registerServerbound(ServerboundPackets1_13.EDIT_BOOK, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.FLAT_ITEM);
                map(Type.BOOLEAN);
                handler(wrapper -> {
                    itemRewriter.handleItemToServer(wrapper.get(Type.FLAT_ITEM, 0));
                    wrapper.write(Type.VAR_INT, 0);
                });
            }
        });

        registerClientbound(ClientboundPackets1_13.OPEN_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE); // Id
                map(Type.STRING); // Window Type
                handler(wrapper -> {
                    JsonElement title = wrapper.passthrough(Type.COMPONENT);
                    translatableRewriter.processText(title);

                    if (ViaBackwards.getConfig().fix1_13FormattedInventoryTitle()) {
                        if (title.isJsonObject() && title.getAsJsonObject().size() == 1
                                && title.getAsJsonObject().has("translate")) {
                            // Hotfix simple translatable components from being converted to legacy text
                            return;
                        }

                        // https://bugs.mojang.com/browse/MC-124543
                        JsonObject legacyComponent = new JsonObject();
                        legacyComponent.addProperty("text", ChatRewriter.jsonToLegacyText(title.toString()));
                        wrapper.set(Type.COMPONENT, 0, legacyComponent);
                    }
                });
            }
        });

        registerClientbound(ClientboundPackets1_13.TAB_COMPLETE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Transaction id
                map(Type.VAR_INT); // Start
                map(Type.VAR_INT); // Length
                map(Type.VAR_INT); // Count
                handler(wrapper -> {
                    int start = wrapper.get(Type.VAR_INT, 1);
                    wrapper.set(Type.VAR_INT, 1, start - 1); // Offset by +1 to take into account / at beginning
                    // Passthrough suggestions
                    int count = wrapper.get(Type.VAR_INT, 3);
                    for (int i = 0; i < count; i++) {
                        wrapper.passthrough(Type.STRING);
                        boolean hasTooltip = wrapper.passthrough(Type.BOOLEAN);
                        if (hasTooltip) {
                            wrapper.passthrough(Type.STRING); // JSON Tooltip
                        }
                    }
                });
            }
        });

        registerClientbound(ClientboundPackets1_13.BOSSBAR, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UUID);
                map(Type.VAR_INT);
                handler(wrapper -> {
                    int action = wrapper.get(Type.VAR_INT, 0);
                    if (action == 0 || action == 3) {
                        translatableRewriter.processText(wrapper.passthrough(Type.COMPONENT));
                        if (action == 0) {
                            wrapper.passthrough(Type.FLOAT);
                            wrapper.passthrough(Type.VAR_INT);
                            wrapper.passthrough(Type.VAR_INT);
                            short flags = wrapper.read(Type.UNSIGNED_BYTE);
                            if ((flags & 0x04) != 0) flags |= 0x02;
                            wrapper.write(Type.UNSIGNED_BYTE, flags);
                        }
                    }
                });
            }
        });

        registerClientbound(ClientboundPackets1_13.ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Type.BOOLEAN); // Reset/clear
            int size = wrapper.passthrough(Type.VAR_INT); // Mapping size

            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Type.STRING); // Identifier

                // Parent
                if (wrapper.passthrough(Type.BOOLEAN))
                    wrapper.passthrough(Type.STRING);

                // Display data
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    wrapper.passthrough(Type.COMPONENT); // Title
                    wrapper.passthrough(Type.COMPONENT); // Description
                    Item icon = wrapper.passthrough(Type.FLAT_ITEM);
                    itemRewriter.handleItemToClient(icon);
                    wrapper.passthrough(Type.VAR_INT); // Frame type
                    int flags = wrapper.passthrough(Type.INT); // Flags
                    if ((flags & 1) != 0)
                        wrapper.passthrough(Type.STRING); // Background texture
                    wrapper.passthrough(Type.FLOAT); // X
                    wrapper.passthrough(Type.FLOAT); // Y
                }

                wrapper.passthrough(Type.STRING_ARRAY); // Criteria

                int arrayLength = wrapper.passthrough(Type.VAR_INT);
                for (int array = 0; array < arrayLength; array++) {
                    wrapper.passthrough(Type.STRING_ARRAY); // String array
                }
            }
        });

        new TagRewriter<>(this).register(ClientboundPackets1_13.TAGS, RegistryType.ITEM);
        new StatisticsRewriter<>(this).register(ClientboundPackets1_13.STATISTICS);
    }

    @Override
    public void init(UserConnection user) {
        user.addEntityTracker(getClass(), new EntityTrackerBase(user, Entity1_13Types.EntityType.PLAYER));

        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld(user));
        }
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPackets1_13_1 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public InventoryPackets1_13_1 getItemRewriter() {
        return itemRewriter;
    }
}
