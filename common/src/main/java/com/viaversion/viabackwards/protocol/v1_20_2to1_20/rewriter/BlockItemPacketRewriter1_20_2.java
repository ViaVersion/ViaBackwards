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
package com.viaversion.viabackwards.protocol.v1_20_2to1_20.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.Protocol1_20_2To1_20;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.provider.AdvancementCriteriaProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.ChunkPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ServerboundPackets1_19_4;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.rewriter.RecipeRewriter1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.util.PotionEffects1_20_2;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.MathUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockItemPacketRewriter1_20_2 extends BackwardsItemRewriter<ClientboundPackets1_20_2, ServerboundPackets1_19_4, Protocol1_20_2To1_20> {

    public BlockItemPacketRewriter1_20_2(final Protocol1_20_2To1_20 protocol) {
        super(protocol, Types.ITEM1_20_2, Types.ITEM1_20_2_ARRAY);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPackets1_20_2> blockRewriter = BlockRewriter.for1_14(protocol);
        blockRewriter.registerBlockAction(ClientboundPackets1_20_2.BLOCK_EVENT);
        blockRewriter.registerBlockChange(ClientboundPackets1_20_2.BLOCK_UPDATE);
        blockRewriter.registerVarLongMultiBlockChange1_20(ClientboundPackets1_20_2.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerEffect(ClientboundPackets1_20_2.LEVEL_EVENT, 1010, 2001);

        protocol.cancelClientbound(ClientboundPackets1_20_2.CHUNK_BATCH_START);
        protocol.registerClientbound(ClientboundPackets1_20_2.CHUNK_BATCH_FINISHED, null, wrapper -> {
            wrapper.cancel();

            final PacketWrapper receivedPacket = wrapper.create(ServerboundPackets1_20_2.CHUNK_BATCH_RECEIVED);
            receivedPacket.write(Types.FLOAT, 500F); // Requested next batch size... arbitrary value here
            receivedPacket.sendToServer(Protocol1_20_2To1_20.class);
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.FORGET_LEVEL_CHUNK, wrapper -> {
            final ChunkPosition chunkPosition = wrapper.read(Types.CHUNK_POSITION);
            wrapper.write(Types.INT, chunkPosition.chunkX());
            wrapper.write(Types.INT, chunkPosition.chunkZ());
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.MAP_ITEM_DATA, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Map id
            wrapper.passthrough(Types.BYTE); // Scale
            wrapper.passthrough(Types.BOOLEAN); // Locked
            if (wrapper.passthrough(Types.BOOLEAN)) {
                final int icons = wrapper.passthrough(Types.VAR_INT);
                for (int i = 0; i < icons; i++) {
                    // Map new marker types to red marker
                    final int markerType = wrapper.read(Types.VAR_INT);
                    wrapper.write(Types.VAR_INT, markerType < 27 ? markerType : 2);

                    wrapper.passthrough(Types.BYTE); // X
                    wrapper.passthrough(Types.BYTE); // Y
                    wrapper.passthrough(Types.BYTE); // Rotation
                    wrapper.passthrough(Types.OPTIONAL_COMPONENT); // Display name
                }
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.TAG_QUERY, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Transaction id
            wrapper.write(Types.NAMED_COMPOUND_TAG, wrapper.read(Types.COMPOUND_TAG));
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.BLOCK_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14); // Position
            wrapper.passthrough(Types.VAR_INT); // Type
            wrapper.write(Types.NAMED_COMPOUND_TAG, handleBlockEntity(wrapper.read(Types.COMPOUND_TAG)));
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.LEVEL_CHUNK_WITH_LIGHT, wrapper -> {
            final EntityTracker tracker = protocol.getEntityRewriter().tracker(wrapper.user());
            final Type<Chunk> chunkType = new ChunkType1_20_2(tracker.currentWorldSectionHeight(),
                MathUtil.ceilLog2(protocol.getMappingData().getBlockStateMappings().size()),
                MathUtil.ceilLog2(tracker.biomesSent()));
            final Chunk chunk = wrapper.read(chunkType);

            final Type<Chunk> newChunkType = new ChunkType1_18(tracker.currentWorldSectionHeight(),
                MathUtil.ceilLog2(protocol.getMappingData().getBlockStateMappings().mappedSize()),
                MathUtil.ceilLog2(tracker.biomesSent()));
            wrapper.write(newChunkType, chunk);

            for (final ChunkSection section : chunk.getSections()) {
                final DataPalette blockPalette = section.palette(PaletteType.BLOCKS);
                for (int i = 0; i < blockPalette.size(); i++) {
                    final int id = blockPalette.idByIndex(i);
                    blockPalette.setIdByIndex(i, protocol.getMappingData().getNewBlockStateId(id));
                }
            }

            for (final BlockEntity blockEntity : chunk.blockEntities()) {
                handleBlockEntity(blockEntity.tag());
            }
        });

        protocol.registerServerbound(ServerboundPackets1_19_4.SET_BEACON, wrapper -> {
            // Effects start at 1 before 1.20.2
            if (wrapper.passthrough(Types.BOOLEAN)) { // Primary effect
                wrapper.write(Types.VAR_INT, wrapper.read(Types.VAR_INT) - 1);
            }
            if (wrapper.passthrough(Types.BOOLEAN)) { // Secondary effect
                wrapper.write(Types.VAR_INT, wrapper.read(Types.VAR_INT) - 1);
            }
        });

        // Replace the NBT type everywhere
        protocol.registerClientbound(ClientboundPackets1_20_2.CONTAINER_SET_CONTENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE); // Window id
                map(Types.VAR_INT); // State id
                handler(wrapper -> {
                    final Item[] items = wrapper.read(Types.ITEM1_20_2_ARRAY);
                    for (final Item item : items) {
                        handleItemToClient(wrapper.user(), item);
                    }

                    wrapper.write(Types.ITEM1_13_2_ARRAY, items);
                    wrapper.write(Types.ITEM1_13_2, handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_20_2))); // Carried item
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_20_2.CONTAINER_SET_SLOT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE); // Window id
                map(Types.VAR_INT); // State id
                map(Types.SHORT); // Slot id
                handler(wrapper -> wrapper.write(Types.ITEM1_13_2, handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_20_2))));
            }
        });
        protocol.registerClientbound(ClientboundPackets1_20_2.UPDATE_ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Types.BOOLEAN); // Reset/clear
            final int size = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < size; i++) {
                final String advancement = wrapper.passthrough(Types.STRING);
                wrapper.passthrough(Types.OPTIONAL_STRING); // Parent

                // Display data
                if (wrapper.passthrough(Types.BOOLEAN)) {
                    wrapper.passthrough(Types.COMPONENT); // Title
                    wrapper.passthrough(Types.COMPONENT); // Description
                    wrapper.write(Types.ITEM1_13_2, handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_20_2))); // Icon
                    wrapper.passthrough(Types.VAR_INT); // Frame type
                    final int flags = wrapper.passthrough(Types.INT); // Flags
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Types.STRING); // Background texture
                    }
                    wrapper.passthrough(Types.FLOAT); // X
                    wrapper.passthrough(Types.FLOAT); // Y
                }

                final AdvancementCriteriaProvider criteriaProvider = Via.getManager().getProviders().get(AdvancementCriteriaProvider.class);
                wrapper.write(Types.STRING_ARRAY, criteriaProvider.getCriteria(advancement));

                final int requirements = wrapper.passthrough(Types.VAR_INT);
                for (int array = 0; array < requirements; array++) {
                    wrapper.passthrough(Types.STRING_ARRAY);
                }

                wrapper.passthrough(Types.BOOLEAN); // Send telemetry
            }
        });
        protocol.registerClientbound(ClientboundPackets1_20_2.SET_EQUIPMENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                handler(wrapper -> {
                    byte slot;
                    do {
                        slot = wrapper.passthrough(Types.BYTE);
                        wrapper.write(Types.ITEM1_13_2, handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_20_2)));
                    } while ((slot & 0xFFFFFF80) != 0);
                });
            }
        });
        protocol.registerServerbound(ServerboundPackets1_19_4.CONTAINER_CLICK, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE); // Window Id
                map(Types.VAR_INT); // State id
                map(Types.SHORT); // Slot
                map(Types.BYTE); // Button
                map(Types.VAR_INT); // Mode

                handler(wrapper -> {
                    // Affected items
                    final int length = wrapper.passthrough(Types.VAR_INT);
                    for (int i = 0; i < length; i++) {
                        wrapper.passthrough(Types.SHORT); // Slot
                        wrapper.write(Types.ITEM1_20_2, handleItemToServer(wrapper.user(), wrapper.read(Types.ITEM1_13_2)));
                    }

                    // Carried item
                    wrapper.write(Types.ITEM1_20_2, handleItemToServer(wrapper.user(), wrapper.read(Types.ITEM1_13_2)));
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_20_2.MERCHANT_OFFERS, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Container id
            final int size = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < size; i++) {
                wrapper.write(Types.ITEM1_13_2, handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_20_2))); // Input
                wrapper.write(Types.ITEM1_13_2, handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_20_2))); // Output
                wrapper.write(Types.ITEM1_13_2, handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_20_2))); // Second Item

                wrapper.passthrough(Types.BOOLEAN); // Trade disabled
                wrapper.passthrough(Types.INT); // Number of tools uses
                wrapper.passthrough(Types.INT); // Maximum number of trade uses
                wrapper.passthrough(Types.INT); // XP
                wrapper.passthrough(Types.INT); // Special price
                wrapper.passthrough(Types.FLOAT); // Price multiplier
                wrapper.passthrough(Types.INT); // Demand
            }
        });
        protocol.registerServerbound(ServerboundPackets1_19_4.SET_CREATIVE_MODE_SLOT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.SHORT); // 0 - Slot
                handler(wrapper -> wrapper.write(Types.ITEM1_20_2, handleItemToServer(wrapper.user(), wrapper.read(Types.ITEM1_13_2)))); // 1 - Clicked Item
            }
        });
        protocol.registerClientbound(ClientboundPackets1_20_2.LEVEL_PARTICLES, new PacketHandlers() {
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
                    final ParticleMappings mappings = Protocol1_20_2To1_20.MAPPINGS.getParticleMappings();
                    if (mappings.isBlockParticle(id)) {
                        final int data = wrapper.read(Types.VAR_INT);
                        wrapper.write(Types.VAR_INT, protocol.getMappingData().getNewBlockStateId(data));
                    } else if (mappings.isItemParticle(id)) {
                        wrapper.write(Types.ITEM1_13_2, handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_20_2)));
                    }
                });
            }
        });

        new RecipeRewriter1_20_2<>(protocol) {
            @Override
            public void handleCraftingShapeless(final PacketWrapper wrapper) {
                wrapper.passthrough(Types.STRING); // Group
                wrapper.passthrough(Types.VAR_INT); // Crafting book category
                handleIngredients(wrapper);

                final Item result = wrapper.read(itemType());
                rewrite(wrapper.user(), result);
                wrapper.write(Types.ITEM1_13_2, result);
            }

            @Override
            public void handleSmelting(final PacketWrapper wrapper) {
                wrapper.passthrough(Types.STRING); // Group
                wrapper.passthrough(Types.VAR_INT); // Crafting book category
                handleIngredient(wrapper);

                final Item result = wrapper.read(itemType());
                rewrite(wrapper.user(), result);
                wrapper.write(Types.ITEM1_13_2, result);

                wrapper.passthrough(Types.FLOAT); // EXP
                wrapper.passthrough(Types.VAR_INT); // Cooking time
            }

            @Override
            public void handleCraftingShaped(final PacketWrapper wrapper) {
                final int ingredients = wrapper.passthrough(Types.VAR_INT) * wrapper.passthrough(Types.VAR_INT);
                wrapper.passthrough(Types.STRING); // Group
                wrapper.passthrough(Types.VAR_INT); // Crafting book category
                for (int i = 0; i < ingredients; i++) {
                    handleIngredient(wrapper);
                }

                final Item result = wrapper.read(itemType());
                rewrite(wrapper.user(), result);
                wrapper.write(Types.ITEM1_13_2, result);

                wrapper.passthrough(Types.BOOLEAN); // Show notification
            }

            @Override
            public void handleStonecutting(final PacketWrapper wrapper) {
                wrapper.passthrough(Types.STRING); // Group
                handleIngredient(wrapper);

                final Item result = wrapper.read(itemType());
                rewrite(wrapper.user(), result);
                wrapper.write(Types.ITEM1_13_2, result);
            }

            @Override
            public void handleSmithing(final PacketWrapper wrapper) {
                handleIngredient(wrapper); // Base
                handleIngredient(wrapper); // Addition

                final Item result = wrapper.read(itemType());
                rewrite(wrapper.user(), result);
                wrapper.write(Types.ITEM1_13_2, result);
            }

            @Override
            public void handleSmithingTransform(final PacketWrapper wrapper) {
                handleIngredient(wrapper); // Template
                handleIngredient(wrapper); // Base
                handleIngredient(wrapper); // Additions

                final Item result = wrapper.read(itemType());
                rewrite(wrapper.user(), result);
                wrapper.write(Types.ITEM1_13_2, result);
            }

            @Override
            protected void handleIngredient(final PacketWrapper wrapper) {
                final Item[] items = wrapper.read(itemArrayType());
                wrapper.write(Types.ITEM1_13_2_ARRAY, items);
                for (final Item item : items) {
                    rewrite(wrapper.user(), item);
                }
            }
        }.register(ClientboundPackets1_20_2.UPDATE_RECIPES);
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable final Item item) {
        if (item == null) {
            return null;
        }
        if (item.tag() != null) {
            com.viaversion.viaversion.protocols.v1_20to1_20_2.rewriter.BlockItemPacketRewriter1_20_2.to1_20_1Effects(item);
        }

        return super.handleItemToClient(connection, item);
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable final Item item) {
        if (item == null) {
            return null;
        }
        if (item.tag() != null) {
            com.viaversion.viaversion.protocols.v1_20to1_20_2.rewriter.BlockItemPacketRewriter1_20_2.to1_20_2Effects(item);
        }

        return super.handleItemToServer(connection, item);
    }

    private @Nullable CompoundTag handleBlockEntity(@Nullable final CompoundTag tag) {
        if (tag == null) {
            return null;
        }

        final Tag primaryEffect = tag.remove("primary_effect");
        if (primaryEffect instanceof StringTag) {
            final String effectKey = Key.stripMinecraftNamespace(((StringTag) primaryEffect).getValue());
            tag.putInt("Primary", PotionEffects1_20_2.keyToId(effectKey) + 1); // Empty effect at 0
        }

        final Tag secondaryEffect = tag.remove("secondary_effect");
        if (secondaryEffect instanceof StringTag) {
            final String effectKey = Key.stripMinecraftNamespace(((StringTag) secondaryEffect).getValue());
            tag.putInt("Secondary", PotionEffects1_20_2.keyToId(effectKey) + 1); // Empty effect at 0
        }
        return tag;
    }
}