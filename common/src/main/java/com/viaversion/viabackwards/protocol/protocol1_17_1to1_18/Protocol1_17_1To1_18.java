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
package com.viaversion.viabackwards.protocol.protocol1_17_1to1_18;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.data.BackwardsMappings;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.packets.BlockItemPackets1_18;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.packets.EntityPackets1_18;
import com.viaversion.viaversion.protocols.protocol1_17_1to1_17.ClientboundPackets1_17_1;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;

public final class Protocol1_17_1To1_18 extends BackwardsProtocol<ClientboundPackets1_17_1, ClientboundPackets1_17_1, ServerboundPackets1_17, ServerboundPackets1_17> {

    private static final BackwardsMappings MAPPINGS = new BackwardsMappings();
    private final EntityPackets1_18 entityRewriter = new EntityPackets1_18(this);
    private final BlockItemPackets1_18 itemRewriter = new BlockItemPackets1_18(this, null); //TODO translatablerewriter

    public Protocol1_17_1To1_18() {
        super(ClientboundPackets1_17_1.class, ClientboundPackets1_17_1.class, ServerboundPackets1_17.class, ServerboundPackets1_17.class);
    }

    @Override
    protected void registerPackets() {
        entityRewriter.register();
        itemRewriter.register();
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPackets1_18 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPackets1_18 getItemRewriter() {
        return itemRewriter;
    }
}
