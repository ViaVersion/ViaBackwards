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
package com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.packets;

import com.viaversion.viabackwards.api.rewriters.ItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.Protocol1_19_1To1_19_3;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ServerboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ClientboundPackets1_19_3;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.Key;

public final class BlockItemPackets1_19_3 extends ItemRewriter<ClientboundPackets1_19_3, ServerboundPackets1_19_1, Protocol1_19_1To1_19_3> {

    public BlockItemPackets1_19_3(final Protocol1_19_1To1_19_3 protocol) {
        super(protocol, Type.ITEM1_13_2, Type.ITEM1_13_2_ARRAY);
    }

    @Override
    protected void registerPackets() {
        final BlockRewriter<ClientboundPackets1_19_3> blockRewriter = BlockRewriter.for1_14(protocol);
        blockRewriter.registerBlockAction(ClientboundPackets1_19_3.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_19_3.BLOCK_CHANGE);
        blockRewriter.registerVarLongMultiBlockChange(ClientboundPackets1_19_3.MULTI_BLOCK_CHANGE);
        blockRewriter.registerEffect(ClientboundPackets1_19_3.EFFECT, 1010, 2001);
        blockRewriter.registerChunkData1_19(ClientboundPackets1_19_3.CHUNK_DATA, ChunkType1_18::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_19_3.BLOCK_ENTITY_DATA);

        registerSetCooldown(ClientboundPackets1_19_3.COOLDOWN);
        registerWindowItems1_17_1(ClientboundPackets1_19_3.WINDOW_ITEMS);
        registerSetSlot1_17_1(ClientboundPackets1_19_3.SET_SLOT);
        registerEntityEquipmentArray(ClientboundPackets1_19_3.ENTITY_EQUIPMENT);
        registerAdvancements(ClientboundPackets1_19_3.ADVANCEMENTS);
        registerClickWindow1_17_1(ServerboundPackets1_19_1.CLICK_WINDOW);
        registerTradeList1_19(ClientboundPackets1_19_3.TRADE_LIST);
        registerCreativeInvAction(ServerboundPackets1_19_1.CREATIVE_INVENTORY_ACTION);
        registerWindowPropertyEnchantmentHandler(ClientboundPackets1_19_3.WINDOW_PROPERTY);
        registerSpawnParticle1_19(ClientboundPackets1_19_3.SPAWN_PARTICLE);

        protocol.registerClientbound(ClientboundPackets1_19_3.EXPLOSION, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.DOUBLE, Type.FLOAT); // X
                map(Type.DOUBLE, Type.FLOAT); // Y
                map(Type.DOUBLE, Type.FLOAT); // Z
            }
        });

        final RecipeRewriter<ClientboundPackets1_19_3> recipeRewriter = new RecipeRewriter<>(protocol);
        protocol.registerClientbound(ClientboundPackets1_19_3.DECLARE_RECIPES, wrapper -> {
            final int size = wrapper.passthrough(Type.VAR_INT);
            for (int i = 0; i < size; i++) {
                final String type = Key.stripMinecraftNamespace(wrapper.passthrough(Type.STRING));
                wrapper.passthrough(Type.STRING); // Recipe Identifier
                switch (type) {
                    case "crafting_shapeless": {
                        wrapper.passthrough(Type.STRING); // Group
                        wrapper.read(Type.VAR_INT); // Crafting book category
                        final int ingredients = wrapper.passthrough(Type.VAR_INT);
                        for (int j = 0; j < ingredients; j++) {
                            final Item[] items = wrapper.passthrough(Type.ITEM1_13_2_ARRAY); // Ingredients
                            for (final Item item : items) {
                                handleItemToClient(wrapper.user(), item);
                            }
                        }
                        handleItemToClient(wrapper.user(), wrapper.passthrough(Type.ITEM1_13_2)); // Result
                        break;
                    }
                    case "crafting_shaped": {
                        final int ingredients = wrapper.passthrough(Type.VAR_INT) * wrapper.passthrough(Type.VAR_INT);
                        wrapper.passthrough(Type.STRING); // Group
                        wrapper.read(Type.VAR_INT); // Crafting book category
                        for (int j = 0; j < ingredients; j++) {
                            final Item[] items = wrapper.passthrough(Type.ITEM1_13_2_ARRAY); // Ingredients
                            for (final Item item : items) {
                                handleItemToClient(wrapper.user(), item);
                            }
                        }
                        handleItemToClient(wrapper.user(), wrapper.passthrough(Type.ITEM1_13_2)); // Result
                        break;
                    }
                    case "smelting":
                    case "campfire_cooking":
                    case "blasting":
                    case "smoking":
                        wrapper.passthrough(Type.STRING); // Group
                        wrapper.read(Type.VAR_INT); // Crafting book category
                        final Item[] items = wrapper.passthrough(Type.ITEM1_13_2_ARRAY); // Ingredients
                        for (final Item item : items) {
                            handleItemToClient(wrapper.user(), item);
                        }
                        handleItemToClient(wrapper.user(), wrapper.passthrough(Type.ITEM1_13_2)); // Result
                        wrapper.passthrough(Type.FLOAT); // EXP
                        wrapper.passthrough(Type.VAR_INT); // Cooking time
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
                        wrapper.read(Type.VAR_INT); // Crafting book category
                        break;
                    default:
                        recipeRewriter.handleRecipeType(wrapper, type);
                        break;
                }
            }
        });
    }
}
