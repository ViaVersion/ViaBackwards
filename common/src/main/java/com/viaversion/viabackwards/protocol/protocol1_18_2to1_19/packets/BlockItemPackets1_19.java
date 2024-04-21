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
package com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.packets;

import com.viaversion.viabackwards.api.rewriters.ItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.Protocol1_18_2To1_19;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.MathUtil;

public final class BlockItemPackets1_19 extends ItemRewriter<ClientboundPackets1_19, ServerboundPackets1_17, Protocol1_18_2To1_19> {

    public BlockItemPackets1_19(final Protocol1_18_2To1_19 protocol) {
        super(protocol, Type.ITEM1_13_2, Type.ITEM1_13_2_ARRAY);
    }

    @Override
    protected void registerPackets() {
        final BlockRewriter<ClientboundPackets1_19> blockRewriter = BlockRewriter.for1_14(protocol);

        new RecipeRewriter<>(protocol).register(ClientboundPackets1_19.DECLARE_RECIPES);

        registerSetCooldown(ClientboundPackets1_19.COOLDOWN);
        registerWindowItems1_17_1(ClientboundPackets1_19.WINDOW_ITEMS);
        registerSetSlot1_17_1(ClientboundPackets1_19.SET_SLOT);
        registerEntityEquipmentArray(ClientboundPackets1_19.ENTITY_EQUIPMENT);
        registerAdvancements(ClientboundPackets1_19.ADVANCEMENTS);
        registerClickWindow1_17_1(ServerboundPackets1_17.CLICK_WINDOW);

        blockRewriter.registerBlockAction(ClientboundPackets1_19.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_19.BLOCK_CHANGE);
        blockRewriter.registerVarLongMultiBlockChange(ClientboundPackets1_19.MULTI_BLOCK_CHANGE);
        blockRewriter.registerEffect(ClientboundPackets1_19.EFFECT, 1010, 2001);

        registerCreativeInvAction(ServerboundPackets1_17.CREATIVE_INVENTORY_ACTION);

        protocol.registerClientbound(ClientboundPackets1_19.TRADE_LIST, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Container id
                handler(wrapper -> {
                    final int size = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.UNSIGNED_BYTE, (short) size);
                    for (int i = 0; i < size; i++) {
                        handleItemToClient(wrapper.user(), wrapper.passthrough(Type.ITEM1_13_2)); // First item
                        handleItemToClient(wrapper.user(), wrapper.passthrough(Type.ITEM1_13_2)); // Result

                        final Item secondItem = wrapper.read(Type.ITEM1_13_2);
                        if (secondItem != null) {
                            handleItemToClient(wrapper.user(), secondItem);
                            wrapper.write(Type.BOOLEAN, true);
                            wrapper.write(Type.ITEM1_13_2, secondItem);
                        } else {
                            wrapper.write(Type.BOOLEAN, false);
                        }

                        wrapper.passthrough(Type.BOOLEAN); // Out of stock
                        wrapper.passthrough(Type.INT); // Uses
                        wrapper.passthrough(Type.INT); // Max uses
                        wrapper.passthrough(Type.INT); // Xp
                        wrapper.passthrough(Type.INT); // Special price diff
                        wrapper.passthrough(Type.FLOAT); // Price multiplier
                        wrapper.passthrough(Type.INT); //Demand
                    }
                });
            }
        });

        registerWindowPropertyEnchantmentHandler(ClientboundPackets1_19.WINDOW_PROPERTY);

        protocol.registerClientbound(ClientboundPackets1_19.BLOCK_CHANGED_ACK, null, new PacketHandlers() {
            @Override
            public void register() {
                read(Type.VAR_INT); // Sequence
                handler(PacketWrapper::cancel); // This is fine:tm:
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.SPAWN_PARTICLE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT, Type.INT); // Particle id
                map(Type.BOOLEAN); // Override limiter
                map(Type.DOUBLE); // X
                map(Type.DOUBLE); // Y
                map(Type.DOUBLE); // Z
                map(Type.FLOAT); // Offset X
                map(Type.FLOAT); // Offset Y
                map(Type.FLOAT); // Offset Z
                map(Type.FLOAT); // Max speed
                map(Type.INT); // Particle Count
                handler(wrapper -> {
                    final int id = wrapper.get(Type.INT, 0);
                    final ParticleMappings particleMappings = protocol.getMappingData().getParticleMappings();
                    if (id == particleMappings.id("sculk_charge")) {
                        //TODO
                        wrapper.set(Type.INT, 0, -1);
                        wrapper.cancel();
                    } else if (id == particleMappings.id("shriek")) {
                        //TODO
                        wrapper.set(Type.INT, 0, -1);
                        wrapper.cancel();
                    } else if (id == particleMappings.id("vibration")) {
                        // Can't do without the position
                        wrapper.set(Type.INT, 0, -1);
                        wrapper.cancel();
                    }
                });
                handler(getSpawnParticleHandler());
            }
        });


        protocol.registerClientbound(ClientboundPackets1_19.CHUNK_DATA, wrapper -> {
            final EntityTracker tracker = protocol.getEntityRewriter().tracker(wrapper.user());
            final ChunkType1_18 chunkType = new ChunkType1_18(tracker.currentWorldSectionHeight(),
                MathUtil.ceilLog2(protocol.getMappingData().getBlockStateMappings().mappedSize()),
                MathUtil.ceilLog2(tracker.biomesSent()));
            final Chunk chunk = wrapper.passthrough(chunkType);
            for (final ChunkSection section : chunk.getSections()) {
                final DataPalette blockPalette = section.palette(PaletteType.BLOCKS);
                for (int i = 0; i < blockPalette.size(); i++) {
                    final int id = blockPalette.idByIndex(i);
                    blockPalette.setIdByIndex(i, protocol.getMappingData().getNewBlockStateId(id));
                }
            }
        });

        // The server does nothing but track the sequence, so we can just set it as 0
        protocol.registerServerbound(ServerboundPackets1_17.PLAYER_DIGGING, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Action
                map(Type.POSITION1_14); // Block position
                map(Type.UNSIGNED_BYTE); // Direction
                create(Type.VAR_INT, 0); // Sequence
            }
        });
        protocol.registerServerbound(ServerboundPackets1_17.PLAYER_BLOCK_PLACEMENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Hand
                map(Type.POSITION1_14); // Block position
                map(Type.VAR_INT); // Direction
                map(Type.FLOAT); // X
                map(Type.FLOAT); // Y
                map(Type.FLOAT); // Z
                map(Type.BOOLEAN); // Inside
                create(Type.VAR_INT, 0); // Sequence
            }
        });
        protocol.registerServerbound(ServerboundPackets1_17.USE_ITEM, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Hand
                create(Type.VAR_INT, 0); // Sequence
            }
        });

        protocol.registerServerbound(ServerboundPackets1_17.SET_BEACON_EFFECT, wrapper -> {
            final int primaryEffect = wrapper.read(Type.VAR_INT);
            if (primaryEffect != -1) {
                wrapper.write(Type.BOOLEAN, true);
                wrapper.write(Type.VAR_INT, primaryEffect);
            } else {
                wrapper.write(Type.BOOLEAN, false);
            }

            final int secondaryEffect = wrapper.read(Type.VAR_INT);
            if (secondaryEffect != -1) {
                wrapper.write(Type.BOOLEAN, true);
                wrapper.write(Type.VAR_INT, secondaryEffect);
            } else {
                wrapper.write(Type.BOOLEAN, false);
            }
        });
    }
}
