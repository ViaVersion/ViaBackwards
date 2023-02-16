/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_14_2to1_14_3;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ServerboundPackets1_14;
import com.viaversion.viaversion.rewriter.RecipeRewriter;

public class Protocol1_14_2To1_14_3 extends BackwardsProtocol<ClientboundPackets1_14, ClientboundPackets1_14, ServerboundPackets1_14, ServerboundPackets1_14> {

    public Protocol1_14_2To1_14_3() {
        super(ClientboundPackets1_14.class, ClientboundPackets1_14.class, ServerboundPackets1_14.class, ServerboundPackets1_14.class);
    }

    @Override
    protected void registerPackets() {
        registerClientbound(ClientboundPackets1_14.TRADE_LIST, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                }
                wrapper.passthrough(Type.BOOLEAN);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.FLOAT);
            }
            wrapper.passthrough(Type.VAR_INT);
            wrapper.passthrough(Type.VAR_INT);

            wrapper.passthrough(Type.BOOLEAN);
            wrapper.read(Type.BOOLEAN);
        });

        RecipeRewriter<ClientboundPackets1_14> recipeHandler = new RecipeRewriter<>(this);
        registerClientbound(ClientboundPackets1_14.DECLARE_RECIPES, wrapper -> {
            int size = wrapper.passthrough(Type.VAR_INT);
            int deleted = 0;
            for (int i = 0; i < size; i++) {
                String fullType = wrapper.read(Type.STRING);
                String type = fullType.replace("minecraft:", "");
                String id = wrapper.read(Type.STRING); // id

                if (type.equals("crafting_special_repairitem")) {
                    deleted++;
                    continue;
                }

                wrapper.write(Type.STRING, fullType);
                wrapper.write(Type.STRING, id);

                recipeHandler.handleRecipeType(wrapper, type);
            }

            wrapper.set(Type.VAR_INT, 0, size - deleted);
        });
    }
}
