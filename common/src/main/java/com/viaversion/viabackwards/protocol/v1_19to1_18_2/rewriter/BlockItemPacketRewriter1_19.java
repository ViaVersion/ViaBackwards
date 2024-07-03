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
package com.viaversion.viabackwards.protocol.v1_19to1_18_2.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.Protocol1_19To1_18_2;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.storage.LastDeathPosition;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.GlobalBlockPosition;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ClientboundPackets1_19;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.MathUtil;

public final class BlockItemPacketRewriter1_19 extends BackwardsItemRewriter<ClientboundPackets1_19, ServerboundPackets1_17, Protocol1_19To1_18_2> {

    private final EnchantmentRewriter enchantmentRewriter = new EnchantmentRewriter(this);

    public BlockItemPacketRewriter1_19(final Protocol1_19To1_18_2 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_ARRAY);
    }

    @Override
    protected void registerPackets() {
        final BlockRewriter<ClientboundPackets1_19> blockRewriter = BlockRewriter.for1_14(protocol);

        new RecipeRewriter<>(protocol).register(ClientboundPackets1_19.UPDATE_RECIPES);

        registerCooldown(ClientboundPackets1_19.COOLDOWN);
        registerSetContent1_17_1(ClientboundPackets1_19.CONTAINER_SET_CONTENT);
        registerSetSlot1_17_1(ClientboundPackets1_19.CONTAINER_SET_SLOT);
        registerSetEquipment(ClientboundPackets1_19.SET_EQUIPMENT);
        registerAdvancements(ClientboundPackets1_19.UPDATE_ADVANCEMENTS);
        registerContainerClick1_17_1(ServerboundPackets1_17.CONTAINER_CLICK);

        blockRewriter.registerBlockEvent(ClientboundPackets1_19.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_19.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate(ClientboundPackets1_19.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent(ClientboundPackets1_19.LEVEL_EVENT, 1010, 2001);

        registerSetCreativeModeSlot(ServerboundPackets1_17.SET_CREATIVE_MODE_SLOT);

        protocol.registerClientbound(ClientboundPackets1_19.MERCHANT_OFFERS, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Container id
                handler(wrapper -> {
                    final int size = wrapper.read(Types.VAR_INT);
                    wrapper.write(Types.UNSIGNED_BYTE, (short) size);
                    for (int i = 0; i < size; i++) {
                        handleItemToClient(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)); // First item
                        handleItemToClient(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)); // Result

                        final Item secondItem = wrapper.read(Types.ITEM1_13_2);
                        if (secondItem != null) {
                            handleItemToClient(wrapper.user(), secondItem);
                            wrapper.write(Types.BOOLEAN, true);
                            wrapper.write(Types.ITEM1_13_2, secondItem);
                        } else {
                            wrapper.write(Types.BOOLEAN, false);
                        }

                        wrapper.passthrough(Types.BOOLEAN); // Out of stock
                        wrapper.passthrough(Types.INT); // Uses
                        wrapper.passthrough(Types.INT); // Max uses
                        wrapper.passthrough(Types.INT); // Xp
                        wrapper.passthrough(Types.INT); // Special price diff
                        wrapper.passthrough(Types.FLOAT); // Price multiplier
                        wrapper.passthrough(Types.INT); //Demand
                    }
                });
            }
        });

        registerContainerSetData(ClientboundPackets1_19.CONTAINER_SET_DATA);

        protocol.registerClientbound(ClientboundPackets1_19.BLOCK_CHANGED_ACK, null, new PacketHandlers() {
            @Override
            public void register() {
                read(Types.VAR_INT); // Sequence
                handler(PacketWrapper::cancel); // This is fine:tm:
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.LEVEL_PARTICLES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT, Types.INT); // Particle id
                map(Types.BOOLEAN); // Override limiter
                map(Types.DOUBLE); // X
                map(Types.DOUBLE); // Y
                map(Types.DOUBLE); // Z
                map(Types.FLOAT); // Offset X
                map(Types.FLOAT); // Offset Y
                map(Types.FLOAT); // Offset Z
                map(Types.FLOAT); // Max speed
                map(Types.INT); // Particle Count
                handler(wrapper -> {
                    final int id = wrapper.get(Types.INT, 0);
                    final ParticleMappings particleMappings = protocol.getMappingData().getParticleMappings();
                    if (id == particleMappings.id("sculk_charge")) {
                        //TODO
                        wrapper.set(Types.INT, 0, -1);
                        wrapper.cancel();
                    } else if (id == particleMappings.id("shriek")) {
                        //TODO
                        wrapper.set(Types.INT, 0, -1);
                        wrapper.cancel();
                    } else if (id == particleMappings.id("vibration")) {
                        // Can't do without the position
                        wrapper.set(Types.INT, 0, -1);
                        wrapper.cancel();
                    }
                });
                handler(levelParticlesHandler());
            }
        });


        protocol.registerClientbound(ClientboundPackets1_19.LEVEL_CHUNK_WITH_LIGHT, wrapper -> {
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
        protocol.registerServerbound(ServerboundPackets1_17.PLAYER_ACTION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Action
                map(Types.BLOCK_POSITION1_14); // Block position
                map(Types.UNSIGNED_BYTE); // Direction
                create(Types.VAR_INT, 0); // Sequence
            }
        });
        protocol.registerServerbound(ServerboundPackets1_17.USE_ITEM_ON, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Hand
                map(Types.BLOCK_POSITION1_14); // Block position
                map(Types.VAR_INT); // Direction
                map(Types.FLOAT); // X
                map(Types.FLOAT); // Y
                map(Types.FLOAT); // Z
                map(Types.BOOLEAN); // Inside
                create(Types.VAR_INT, 0); // Sequence
            }
        });
        protocol.registerServerbound(ServerboundPackets1_17.USE_ITEM, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Hand
                create(Types.VAR_INT, 0); // Sequence
            }
        });

        protocol.registerServerbound(ServerboundPackets1_17.SET_BEACON, wrapper -> {
            final int primaryEffect = wrapper.read(Types.VAR_INT);
            if (primaryEffect != -1) {
                wrapper.write(Types.BOOLEAN, true);
                wrapper.write(Types.VAR_INT, primaryEffect);
            } else {
                wrapper.write(Types.BOOLEAN, false);
            }

            final int secondaryEffect = wrapper.read(Types.VAR_INT);
            if (secondaryEffect != -1) {
                wrapper.write(Types.BOOLEAN, true);
                wrapper.write(Types.VAR_INT, secondaryEffect);
            } else {
                wrapper.write(Types.BOOLEAN, false);
            }
        });
    }

    @Override
    protected void registerRewrites() {
        enchantmentRewriter.registerEnchantment("minecraft:swift_sneak", "§7Swift Sneak");
    }

    @Override
    public Item handleItemToClient(final UserConnection connection, final Item item) {
        if (item == null) return null;

        final int identifier = item.identifier();
        super.handleItemToClient(connection, item);

        if (identifier != 834) {
            return item;
        }
        final LastDeathPosition lastDeathPosition = connection.get(LastDeathPosition.class);
        if (lastDeathPosition == null) {
            return item;
        }
        final GlobalBlockPosition position = lastDeathPosition.position();

        final CompoundTag lodestonePosTag = new CompoundTag();
        item.tag().putBoolean(nbtTagName(), true);
        item.tag().put("LodestonePos", lodestonePosTag);
        item.tag().putString("LodestoneDimension", position.dimension());

        lodestonePosTag.putInt("X", position.x());
        lodestonePosTag.putInt("Y", position.y());
        lodestonePosTag.putInt("Z", position.z());

        enchantmentRewriter.handleToClient(item);
        return item;
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, final Item item) {
        if (item == null) return null;

        super.handleItemToServer(connection, item);

        CompoundTag tag = item.tag();
        if (item.identifier() == 834 && tag != null) {
            if (tag.contains(nbtTagName())) {
                tag.remove(nbtTagName());
                tag.remove("LodestonePos");
                tag.remove("LodestoneDimension");
            }
            if (tag.isEmpty()) {
                item.setTag(null);
            }
        }

        enchantmentRewriter.handleToServer(item);
        return item;
    }
}
