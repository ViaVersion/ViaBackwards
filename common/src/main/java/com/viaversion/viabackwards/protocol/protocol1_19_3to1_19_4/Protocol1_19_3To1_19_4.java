/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_19_3to1_19_4;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.ItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_19_3to1_19_4.packets.EntityPackets1_19_4;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19_4Types;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ServerboundPackets1_19_3;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.Protocol1_19_4To1_19_3;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ServerboundPackets1_19_4;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

public final class Protocol1_19_3To1_19_4 extends BackwardsProtocol<ClientboundPackets1_19_4, ClientboundPackets1_19_3, ServerboundPackets1_19_4, ServerboundPackets1_19_3> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.19.4", "1.19.3", Protocol1_19_4To1_19_3.class);
    private final EntityPackets1_19_4 entityRewriter = new EntityPackets1_19_4(this);
    private final ItemRewriter<ClientboundPackets1_19_4, ServerboundPackets1_19_3, Protocol1_19_3To1_19_4> itemRewriter = new ItemRewriter<>(this);

    public Protocol1_19_3To1_19_4() {
        super(ClientboundPackets1_19_4.class, ClientboundPackets1_19_3.class, ServerboundPackets1_19_4.class, ServerboundPackets1_19_3.class);
    }

    @Override
    protected void registerPackets() {
        // TODO fallback field in components
        executeAsyncAfterLoaded(Protocol1_19_4To1_19_3.class, () -> {
            MAPPINGS.load();
            entityRewriter.onMappingDataLoaded();
        });

        entityRewriter.register();

        final CommandRewriter<ClientboundPackets1_19_4> commandRewriter = new CommandRewriter<ClientboundPackets1_19_4>(this) {
            @Override
            public void handleArgument(final PacketWrapper wrapper, final String argumentType) throws Exception {
                if (argumentType.equals("minecraft:time")) {
                    wrapper.read(Type.INT); // Minimum
                } else if (argumentType.equals("minecraft:resource") || argumentType.equals("minecraft:resource_or_tag")) {
                    final String resource = wrapper.read(Type.STRING);
                    // Replace damage types with... something
                    wrapper.write(Type.STRING, resource.equals("minecraft:damage_type") ? "minecraft:mob_effect" : resource);
                } else {
                    super.handleArgument(wrapper, argumentType);
                }
            }
        };
        commandRewriter.registerDeclareCommands1_19(ClientboundPackets1_19_4.DECLARE_COMMANDS);

        final TagRewriter<ClientboundPackets1_19_4> tagRewriter = new TagRewriter<>(this);
        tagRewriter.removeTags("minecraft:damage_type");
        tagRewriter.registerGeneric(ClientboundPackets1_19_4.TAGS);

        cancelClientbound(ClientboundPackets1_19_4.BUNDLE);

        registerClientbound(ClientboundPackets1_19_4.DAMAGE_EVENT, ClientboundPackets1_19_3.ENTITY_STATUS, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT, Type.INT); // Entity id
                read(Type.VAR_INT); // Damage type
                read(Type.VAR_INT); // Cause entity
                read(Type.VAR_INT); // Direct cause entity
                handler(wrapper -> {
                    // Source position
                    if (wrapper.read(Type.BOOLEAN)) {
                        wrapper.read(Type.DOUBLE);
                        wrapper.read(Type.DOUBLE);
                        wrapper.read(Type.DOUBLE);
                    }
                });
                create(Type.BYTE, (byte) 2); // Generic hurt
            }
        });
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, Entity1_19_4Types.PLAYER));
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public ItemRewriter<ClientboundPackets1_19_4, ServerboundPackets1_19_3, Protocol1_19_3To1_19_4> getItemRewriter() {
        return itemRewriter;
    }
}