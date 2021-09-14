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
package com.viaversion.viabackwards.protocol.protocol1_17_1to1_18;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_17_1to1_17.ClientboundPackets1_17_1;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.types.Chunk1_17Type;

public final class Protocol1_17_1To1_18 extends BackwardsProtocol<ClientboundPackets1_17_1, ClientboundPackets1_17_1, ServerboundPackets1_17, ServerboundPackets1_17> {

    public Protocol1_17_1To1_18() {
        super(ClientboundPackets1_17_1.class, ClientboundPackets1_17_1.class, ServerboundPackets1_17.class, ServerboundPackets1_17.class);
    }

    @Override
    protected void registerPackets() {
        registerClientbound(ClientboundPackets1_17_1.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    final EntityTracker tracker = getEntityRewriter().tracker(wrapper.user());
                    final Chunk chunk = wrapper.passthrough(new Chunk1_17Type(tracker.currentWorldSectionHeight()));
                    /*for (int s = 0; s < chunk.getSections().length; s++) {
                        ChunkSection section = chunk.getSections()[s];
                        if (section == null) continue;
                        for (int i = 0; i < section.getPaletteSize(); i++) {
                            int old = section.getPaletteEntry(i);
                            section.setPaletteEntry(i, protocol.getMappingData().getNewBlockStateId(old));
                        }
                    }*/

                    // Create and send light packet first
                    final PacketWrapper lightPacket = wrapper.create(ClientboundPackets1_17_1.UPDATE_LIGHT);
                    lightPacket.write(Type.BOOLEAN, wrapper.read(Type.BOOLEAN)); // Trust edges
                    lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE)); // Sky light mask
                    lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE)); // Block light mask
                    lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE)); // Empty sky light mask
                    lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE)); // Empty block light mask

                    final int skyLightLength = wrapper.read(Type.VAR_INT);
                    lightPacket.write(Type.VAR_INT, skyLightLength);
                    for (int i = 0; i < skyLightLength; i++) {
                        lightPacket.write(Type.BYTE_ARRAY_PRIMITIVE, wrapper.read(Type.BYTE_ARRAY_PRIMITIVE));
                    }

                    final int blockLightLength = wrapper.read(Type.VAR_INT);
                    lightPacket.write(Type.VAR_INT, blockLightLength);
                    for (int i = 0; i < blockLightLength; i++) {
                        lightPacket.write(Type.BYTE_ARRAY_PRIMITIVE, wrapper.read(Type.BYTE_ARRAY_PRIMITIVE));
                    }

                    lightPacket.send(Protocol1_17_1To1_18.class);
                });
            }
        });
    }
}
