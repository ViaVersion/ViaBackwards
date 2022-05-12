/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.data;

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CommandRewriter1_19 extends CommandRewriter {

    public CommandRewriter1_19(Protocol protocol) {
        super(protocol);
        //TODO
    }

    @Override
    protected @Nullable String handleArgumentType(String argumentType) {
        if (argumentType.equals("minecraft:resource") || argumentType.equals("minecraft:resource_or_tag")) {
            return "brigadier:string";
        }
        return super.handleArgumentType(argumentType);
    }
}
