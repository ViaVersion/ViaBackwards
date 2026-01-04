/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_18_2to1_18;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.protocol.v1_18_2to1_18.rewriter.CommandRewriter1_18_2;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import com.viaversion.viaversion.util.TagUtil;

public final class Protocol1_18_2To1_18 extends BackwardsProtocol<ClientboundPackets1_18, ClientboundPackets1_18, ServerboundPackets1_17, ServerboundPackets1_17> {

    public Protocol1_18_2To1_18() {
        super(ClientboundPackets1_18.class, ClientboundPackets1_18.class, ServerboundPackets1_17.class, ServerboundPackets1_17.class);
    }

    @Override
    protected void registerPackets() {
        new CommandRewriter1_18_2(this).registerDeclareCommands(ClientboundPackets1_18.COMMANDS);

        final PacketHandler entityEffectIdHandler = wrapper -> {
            final int id = wrapper.read(Types.VAR_INT);
            if ((byte) id != id) {
                if (Via.getConfig().logOtherConversionWarnings()) {
                    getLogger().warning("Cannot send entity effect id " + id + " to old client");
                }
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.BYTE, (byte) id);
        };
        registerClientbound(ClientboundPackets1_18.UPDATE_MOB_EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Entity id
                handler(entityEffectIdHandler);
            }
        });

        registerClientbound(ClientboundPackets1_18.REMOVE_MOB_EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Entity id
                handler(entityEffectIdHandler);
            }
        });

        registerClientbound(ClientboundPackets1_18.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity ID
                map(Types.BOOLEAN); // Hardcore
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous Gamemode
                map(Types.STRING_ARRAY); // World List
                map(Types.NAMED_COMPOUND_TAG); // Registry
                map(Types.NAMED_COMPOUND_TAG); // Current dimension data
                handler(wrapper -> {
                    final CompoundTag registry = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);
                    final ListTag<CompoundTag> dimensions = TagUtil.getRegistryEntries(registry, "dimension_type");
                    for (final CompoundTag dimension : dimensions) {
                        removeTagPrefix(dimension.getCompoundTag("element"));
                    }

                    removeTagPrefix(wrapper.get(Types.NAMED_COMPOUND_TAG, 1));
                });
            }
        });

        registerClientbound(ClientboundPackets1_18.RESPAWN, wrapper -> removeTagPrefix(wrapper.passthrough(Types.NAMED_COMPOUND_TAG)));
    }

    private void removeTagPrefix(CompoundTag tag) {
        final StringTag infiniburnTag = tag.getStringTag("infiniburn");
        if (infiniburnTag != null) {
            infiniburnTag.setValue(infiniburnTag.getValue().substring(1));
        }
    }
}
