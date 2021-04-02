/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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
package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data;

import org.checkerframework.checker.nullness.qual.Nullable;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.rewriters.CommandRewriter;

public class CommandRewriter1_16 extends CommandRewriter {

    public CommandRewriter1_16(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected @Nullable String handleArgumentType(String argumentType) {
        if (argumentType.equals("minecraft:uuid")) {
            return "minecraft:game_profile";
        }
        return super.handleArgumentType(argumentType);
    }

}
