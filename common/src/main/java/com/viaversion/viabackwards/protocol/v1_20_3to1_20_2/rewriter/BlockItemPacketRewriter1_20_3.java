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
package com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.rewriter;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.Protocol1_20_3To1_20_2;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPacket1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPacket1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.rewriter.RecipeRewriter1_20_3;
import com.viaversion.viaversion.rewriter.BlockRewriter;

public final class BlockItemPacketRewriter1_20_3 extends BackwardsItemRewriter<ClientboundPacket1_20_3, ServerboundPacket1_20_2, Protocol1_20_3To1_20_2> {

    public BlockItemPacketRewriter1_20_3(final Protocol1_20_3To1_20_2 protocol) {
        super(protocol, Types.ITEM1_20_2, Types.ITEM1_20_2_ARRAY);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_20_3> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_20_3.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_20_3.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_20_3.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent(ClientboundPackets1_20_3.LEVEL_EVENT, 1010, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_20_3.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_20_2::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_20_3.BLOCK_ENTITY_DATA);

        registerCooldown(ClientboundPackets1_20_3.COOLDOWN);
        registerSetContent1_17_1(ClientboundPackets1_20_3.CONTAINER_SET_CONTENT);
        registerSetSlot1_17_1(ClientboundPackets1_20_3.CONTAINER_SET_SLOT);
        registerSetEquipment(ClientboundPackets1_20_3.SET_EQUIPMENT);
        registerContainerClick1_17_1(ServerboundPackets1_20_2.CONTAINER_CLICK);
        registerMerchantOffers1_19(ClientboundPackets1_20_3.MERCHANT_OFFERS);
        registerSetCreativeModeSlot(ServerboundPackets1_20_2.SET_CREATIVE_MODE_SLOT);
        registerContainerSetData(ClientboundPackets1_20_3.CONTAINER_SET_DATA);

        protocol.registerClientbound(ClientboundPackets1_20_3.LEVEL_PARTICLES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Particle ID
                map(Types.BOOLEAN); // 1 - Long Distance
                map(Types.DOUBLE); // 2 - X
                map(Types.DOUBLE); // 3 - Y
                map(Types.DOUBLE); // 4 - Z
                map(Types.FLOAT); // 5 - Offset X
                map(Types.FLOAT); // 6 - Offset Y
                map(Types.FLOAT); // 7 - Offset Z
                map(Types.FLOAT); // 8 - Particle Data
                map(Types.INT); // 9 - Particle Count
                handler(wrapper -> {
                    final int id = wrapper.get(Types.VAR_INT, 0);
                    final ParticleMappings particleMappings = protocol.getMappingData().getParticleMappings();
                    if (id == particleMappings.id("vibration")) {
                        final int positionSourceType = wrapper.read(Types.VAR_INT);
                        if (positionSourceType == 0) {
                            wrapper.write(Types.STRING, "minecraft:block");
                        } else if (positionSourceType == 1) {
                            wrapper.write(Types.STRING, "minecraft:entity");
                        } else {
                            protocol.getLogger().warning("Unknown position source type: " + positionSourceType);
                            wrapper.cancel();
                        }
                    }
                });
                handler(levelParticlesHandler(Types.VAR_INT));
            }
        });

        new RecipeRewriter1_20_3<>(protocol) {
            @Override
            public void handleCraftingShaped(final PacketWrapper wrapper) {
                // Move width and height up
                final String group = wrapper.read(Types.STRING);
                final int craftingBookCategory = wrapper.read(Types.VAR_INT);

                final int width = wrapper.passthrough(Types.VAR_INT);
                final int height = wrapper.passthrough(Types.VAR_INT);

                wrapper.write(Types.STRING, group);
                wrapper.write(Types.VAR_INT, craftingBookCategory);

                final int ingredients = height * width;
                for (int i = 0; i < ingredients; i++) {
                    handleIngredient(wrapper);
                }
                rewrite(wrapper.user(), wrapper.passthrough(itemType())); // Result
                wrapper.passthrough(Types.BOOLEAN); // Show notification
            }
        }.register(ClientboundPackets1_20_3.UPDATE_RECIPES);

        protocol.registerClientbound(ClientboundPackets1_20_3.EXPLODE, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.FLOAT); // Power

            final int blocks = wrapper.read(Types.VAR_INT);
            final byte[][] toBlow = new byte[blocks][3];
            for (int i = 0; i < blocks; i++) {
                toBlow[i] = new byte[]{
                    wrapper.read(Types.BYTE), // Relative X
                    wrapper.read(Types.BYTE), // Relative Y
                    wrapper.read(Types.BYTE) // Relative Z
                };
            }

            final float knockbackX = wrapper.read(Types.FLOAT); // Knockback X
            final float knockbackY = wrapper.read(Types.FLOAT); // Knockback Y
            final float knockbackZ = wrapper.read(Types.FLOAT); // Knockback Z

            final int blockInteraction = wrapper.read(Types.VAR_INT); // Block interaction type
            // 0 = keep, 1 = destroy, 2 = destroy_with_decay, 3 = trigger_block
            if (blockInteraction == 1 || blockInteraction == 2) {
                wrapper.write(Types.VAR_INT, blocks);
                for (final byte[] relativeXYZ : toBlow) {
                    wrapper.write(Types.BYTE, relativeXYZ[0]);
                    wrapper.write(Types.BYTE, relativeXYZ[1]);
                    wrapper.write(Types.BYTE, relativeXYZ[2]);
                }
            } else {
                // Explosion doesn't destroy blocks
                wrapper.write(Types.VAR_INT, 0);
            }

            wrapper.write(Types.FLOAT, knockbackX);
            wrapper.write(Types.FLOAT, knockbackY);
            wrapper.write(Types.FLOAT, knockbackZ);

            // TODO Probably needs handling
            wrapper.read(Types1_20_3.PARTICLE); // Small explosion particle
            wrapper.read(Types1_20_3.PARTICLE); // Large explosion particle
            wrapper.read(Types.STRING); // Explosion sound
            wrapper.read(Types.OPTIONAL_FLOAT); // Sound range
        });
    }
}