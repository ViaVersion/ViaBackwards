/*
 * ViaBackwards - https://github.com/ViaVersion/ViaBackwards
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
package nl.matsv.viabackwards.protocol.protocol1_16_1to1_16_2.data;

import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.rewriters.CommandRewriter;
import us.myles.ViaVersion.api.type.Type;

public class CommandRewriter1_16_2 extends CommandRewriter {

    public CommandRewriter1_16_2(Protocol protocol) {
        super(protocol);

        this.parserHandlers.put("minecraft:angle", wrapper -> {
            wrapper.write(Type.VAR_INT, 0); // Single word
        });
    }

    @Override
    @Nullable
    protected String handleArgumentType(String argumentType) {
        if (argumentType.equals("minecraft:angle")) {
            return "brigadier:string";
        }
        return super.handleArgumentType(argumentType);
    }

}
