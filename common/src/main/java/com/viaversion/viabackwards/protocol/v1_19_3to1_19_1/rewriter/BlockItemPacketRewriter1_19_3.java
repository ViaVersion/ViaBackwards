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
package com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.Protocol1_19_3To1_19_1;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.packet.ServerboundPackets1_19_1;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.Key;

public final class BlockItemPacketRewriter1_19_3 extends BackwardsItemRewriter<ClientboundPackets1_19_3, ServerboundPackets1_19_1, Protocol1_19_3To1_19_1> {

    public BlockItemPacketRewriter1_19_3(final Protocol1_19_3To1_19_1 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_ARRAY);
    }

    @Override
    protected void registerPackets() {
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
                    case "crafting_shapeless" -> {
                        wrapper.passthrough(Types.STRING); // Group
                        wrapper.read(Types.VAR_INT); // Crafting book category
                        final int ingredients = wrapper.passthrough(Types.VAR_INT);
                        for (int j = 0; j < ingredients; j++) {
                            final Item[] items = wrapper.passthrough(Types.ITEM1_13_2_ARRAY); // Ingredients
                            for (int k = 0; k < items.length; k++) {
                                items[k] = handleItemToClient(wrapper.user(), items[k]);
                            }
                        }
                        passthroughClientboundItem(wrapper); // Result
                    }
                    case "crafting_shaped" -> {
                        final int ingredients = wrapper.passthrough(Types.VAR_INT) * wrapper.passthrough(Types.VAR_INT);
                        wrapper.passthrough(Types.STRING); // Group
                        wrapper.read(Types.VAR_INT); // Crafting book category
                        for (int j = 0; j < ingredients; j++) {
                            final Item[] items = wrapper.passthrough(Types.ITEM1_13_2_ARRAY); // Ingredients
                            for (int k = 0; k < items.length; k++) {
                                items[k] = handleItemToClient(wrapper.user(), items[k]);
                            }
                        }
                        passthroughClientboundItem(wrapper); // Result
                    }
                    case "smelting", "campfire_cooking", "blasting", "smoking" -> {
                        wrapper.passthrough(Types.STRING); // Group
                        wrapper.read(Types.VAR_INT); // Crafting book category
                        final Item[] items = wrapper.passthrough(Types.ITEM1_13_2_ARRAY); // Ingredients
                        for (int j = 0; j < items.length; j++) {
                            items[j] = handleItemToClient(wrapper.user(), items[j]);
                        }
                        passthroughClientboundItem(wrapper); // Result
                        wrapper.passthrough(Types.FLOAT); // EXP
                        wrapper.passthrough(Types.VAR_INT); // Cooking time
                    }
                    case "crafting_special_armordye", "crafting_special_bookcloning", "crafting_special_mapcloning",
                         "crafting_special_mapextending", "crafting_special_firework_rocket",
                         "crafting_special_firework_star",
                         "crafting_special_firework_star_fade", "crafting_special_tippedarrow",
                         "crafting_special_bannerduplicate",
                         "crafting_special_shielddecoration", "crafting_special_shulkerboxcoloring",
                         "crafting_special_suspiciousstew",
                         "crafting_special_repairitem" -> wrapper.read(Types.VAR_INT); // Crafting book category
                    default -> recipeRewriter.handleRecipeType(wrapper, type);
                }
            }
        });
    }
}
