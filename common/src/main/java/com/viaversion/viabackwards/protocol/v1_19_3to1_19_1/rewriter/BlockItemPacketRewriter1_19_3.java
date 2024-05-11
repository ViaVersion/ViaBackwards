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
package com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.Protocol1_19_3To1_19_1;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.packet.ServerboundPackets1_19_1;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.Key;

public final class BlockItemPacketRewriter1_19_3 extends BackwardsItemRewriter<ClientboundPackets1_19_3, ServerboundPackets1_19_1, Protocol1_19_3To1_19_1> {

    public BlockItemPacketRewriter1_19_3(final Protocol1_19_3To1_19_1 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_ARRAY);
    }

    @Override
    protected void registerPackets() {
        final BlockRewriter<ClientboundPackets1_19_3> blockRewriter = BlockRewriter.for1_14(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_19_3.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_19_3.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate(ClientboundPackets1_19_3.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent(ClientboundPackets1_19_3.LEVEL_EVENT, 1010, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_19_3.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_18::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_19_3.BLOCK_ENTITY_DATA);

        registerCooldown(ClientboundPackets1_19_3.COOLDOWN);
        registerSetContent1_17_1(ClientboundPackets1_19_3.CONTAINER_SET_CONTENT);
        registerSetSlot1_17_1(ClientboundPackets1_19_3.CONTAINER_SET_SLOT);
        registerSetEquipment(ClientboundPackets1_19_3.SET_EQUIPMENT);
        registerAdvancements(ClientboundPackets1_19_3.UPDATE_ADVANCEMENTS);
        registerContainerClick1_17_1(ServerboundPackets1_19_1.CONTAINER_CLICK);
        registerMerchantOffers1_19(ClientboundPackets1_19_3.MERCHANT_OFFERS);
        registerSetCreativeModeSlot(ServerboundPackets1_19_1.SET_CREATIVE_MODE_SLOT);
        registerContainerSetData(ClientboundPackets1_19_3.CONTAINER_SET_DATA);
        registerLevelParticles1_19(ClientboundPackets1_19_3.LEVEL_PARTICLES);

        protocol.registerClientbound(ClientboundPackets1_19_3.EXPLODE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.DOUBLE, Types.FLOAT); // X
                map(Types.DOUBLE, Types.FLOAT); // Y
                map(Types.DOUBLE, Types.FLOAT); // Z
            }
        });

        final RecipeRewriter<ClientboundPackets1_19_3> recipeRewriter = new RecipeRewriter<>(protocol);
        protocol.registerClientbound(ClientboundPackets1_19_3.UPDATE_RECIPES, wrapper -> {
            final int size = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < size; i++) {
                final String type = Key.stripMinecraftNamespace(wrapper.passthrough(Types.STRING));
                wrapper.passthrough(Types.STRING); // Recipe Identifier
                switch (type) {
                    case "crafting_shapeless": {
                        wrapper.passthrough(Types.STRING); // Group
                        wrapper.read(Types.VAR_INT); // Crafting book category
                        final int ingredients = wrapper.passthrough(Types.VAR_INT);
                        for (int j = 0; j < ingredients; j++) {
                            final Item[] items = wrapper.passthrough(Types.ITEM1_13_2_ARRAY); // Ingredients
                            for (final Item item : items) {
                                handleItemToClient(wrapper.user(), item);
                            }
                        }
                        handleItemToClient(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)); // Result
                        break;
                    }
                    case "crafting_shaped": {
                        final int ingredients = wrapper.passthrough(Types.VAR_INT) * wrapper.passthrough(Types.VAR_INT);
                        wrapper.passthrough(Types.STRING); // Group
                        wrapper.read(Types.VAR_INT); // Crafting book category
                        for (int j = 0; j < ingredients; j++) {
                            final Item[] items = wrapper.passthrough(Types.ITEM1_13_2_ARRAY); // Ingredients
                            for (final Item item : items) {
                                handleItemToClient(wrapper.user(), item);
                            }
                        }
                        handleItemToClient(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)); // Result
                        break;
                    }
                    case "smelting":
                    case "campfire_cooking":
                    case "blasting":
                    case "smoking":
                        wrapper.passthrough(Types.STRING); // Group
                        wrapper.read(Types.VAR_INT); // Crafting book category
                        final Item[] items = wrapper.passthrough(Types.ITEM1_13_2_ARRAY); // Ingredients
                        for (final Item item : items) {
                            handleItemToClient(wrapper.user(), item);
                        }
                        handleItemToClient(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)); // Result
                        wrapper.passthrough(Types.FLOAT); // EXP
                        wrapper.passthrough(Types.VAR_INT); // Cooking time
                        break;
                    case "crafting_special_armordye":
                    case "crafting_special_bookcloning":
                    case "crafting_special_mapcloning":
                    case "crafting_special_mapextending":
                    case "crafting_special_firework_rocket":
                    case "crafting_special_firework_star":
                    case "crafting_special_firework_star_fade":
                    case "crafting_special_tippedarrow":
                    case "crafting_special_bannerduplicate":
                    case "crafting_special_shielddecoration":
                    case "crafting_special_shulkerboxcoloring":
                    case "crafting_special_suspiciousstew":
                    case "crafting_special_repairitem":
                        wrapper.read(Types.VAR_INT); // Crafting book category
                        break;
                    default:
                        recipeRewriter.handleRecipeType(wrapper, type);
                        break;
                }
            }
        });
    }
}
