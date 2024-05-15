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
package com.viaversion.viabackwards.protocol.v1_14to1_13_2.rewriter;

import com.viaversion.viabackwards.protocol.v1_14to1_13_2.Protocol1_14To1_13_2;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CommandRewriter1_14 extends CommandRewriter<ClientboundPackets1_14> {

    public CommandRewriter1_14(Protocol1_14To1_13_2 protocol) {
        super(protocol);

        this.parserHandlers.put("minecraft:nbt_tag", wrapper -> wrapper.write(Types.VAR_INT, 2)); // Greedy phrase
        this.parserHandlers.put("minecraft:time", wrapper -> {
            wrapper.write(Types.BYTE, (byte) (0x01)); // Flags
            wrapper.write(Types.INT, 0); // Min value
        });
    }

    @Override
    public @Nullable String handleArgumentType(String argumentType) {
        return switch (argumentType) {
            case "minecraft:nbt_compound_tag" -> "minecraft:nbt";
            case "minecraft:nbt_tag" -> "brigadier:string";
            case "minecraft:time" -> "brigadier:integer";
            default -> super.handleArgumentType(argumentType);
        };
    }

}
