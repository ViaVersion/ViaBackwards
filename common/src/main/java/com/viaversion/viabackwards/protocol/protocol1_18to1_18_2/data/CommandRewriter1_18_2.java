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
package com.viaversion.viabackwards.protocol.protocol1_18to1_18_2.data;

import com.viaversion.viabackwards.protocol.protocol1_18to1_18_2.Protocol1_18To1_18_2;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CommandRewriter1_18_2 extends CommandRewriter<ClientboundPackets1_18> {

    public CommandRewriter1_18_2(Protocol1_18To1_18_2 protocol) {
        super(protocol);

        // Uncompletable without the full list
        this.parserHandlers.put("minecraft:resource", wrapper -> {
            wrapper.read(Type.STRING);
            wrapper.write(Type.VAR_INT, 1); // Quotable string
        });
        this.parserHandlers.put("minecraft:resource_or_tag", wrapper -> {
            wrapper.read(Type.STRING);
            wrapper.write(Type.VAR_INT, 1); // Quotable string
        });
    }

    @Override
    public @Nullable String handleArgumentType(String argumentType) {
        if (argumentType.equals("minecraft:resource") || argumentType.equals("minecraft:resource_or_tag")) {
            return "brigadier:string";
        }
        return super.handleArgumentType(argumentType);
    }
}
