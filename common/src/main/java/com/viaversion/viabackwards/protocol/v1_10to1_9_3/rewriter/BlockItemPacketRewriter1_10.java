/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.v1_10to1_9_3.rewriter;

import com.viaversion.viabackwards.api.rewriters.LegacyBlockItemRewriter;
import com.viaversion.viabackwards.protocol.v1_10to1_9_3.Protocol1_10To1_9_3;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;

public class BlockItemPacketRewriter1_10 extends LegacyBlockItemRewriter<ClientboundPackets1_9_3, ServerboundPackets1_9_3, Protocol1_10To1_9_3> {

    public BlockItemPacketRewriter1_10(Protocol1_10To1_9_3 protocol) {
        super(protocol, "1.10");
    }

    @Override
    protected void registerPackets() {
        registerBlockChange(ClientboundPackets1_9_3.BLOCK_UPDATE);
        registerMultiBlockChange(ClientboundPackets1_9_3.CHUNK_BLOCKS_UPDATE);

        registerSetSlot(ClientboundPackets1_9_3.CONTAINER_SET_SLOT);
        registerSetContent(ClientboundPackets1_9_3.CONTAINER_SET_CONTENT);

        registerSetEquippedItem(ClientboundPackets1_9_3.SET_EQUIPPED_ITEM);
        registerMerchantOffers(ClientboundPackets1_9_3.CUSTOM_PAYLOAD);

        registerContainerClick(ServerboundPackets1_9_3.CONTAINER_CLICK);
        registerSetCreativeModeSlot(ServerboundPackets1_9_3.SET_CREATIVE_MODE_SLOT);

        protocol.registerClientbound(ClientboundPackets1_9_3.LEVEL_CHUNK, wrapper -> {
            ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

            ChunkType1_9_3 type = ChunkType1_9_3.forEnvironment(clientWorld.getEnvironment());
            Chunk chunk = wrapper.passthrough(type);

            handleChunk(chunk);
        });

        // Rewrite metadata items
        protocol.getEntityRewriter().filter().handler((event, meta) -> {
            if (meta.dataType().type().equals(Types.ITEM1_8)) // Is Item
                meta.setValue(handleItemToClient(event.user(), (Item) meta.getValue()));
        });

        // Particle
        protocol.registerClientbound(ClientboundPackets1_9_3.LEVEL_PARTICLES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT);
                map(Types.BOOLEAN);
                map(Types.FLOAT);
                map(Types.FLOAT);
                map(Types.FLOAT);
                map(Types.FLOAT);
                map(Types.FLOAT);
                map(Types.FLOAT);
                map(Types.FLOAT);
                map(Types.INT);

                handler(wrapper -> {
                    int id = wrapper.get(Types.INT, 0);
                    if (id == 46) { // new falling_dust
                        wrapper.set(Types.INT, 0, 38); // -> block_dust
                    }
                });
            }
        });
    }
}
