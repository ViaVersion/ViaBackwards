/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_20_2to1_20_3.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_2to1_20_3.Protocol1_20_2To1_20_3;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19_4Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.type.types.version.Types1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.Protocol1_20_3To1_20_2;

public final class EntityPacketRewriter1_20_3 extends EntityRewriter<ClientboundPackets1_20_2, Protocol1_20_2To1_20_3> {

    public EntityPacketRewriter1_20_3(final Protocol1_20_2To1_20_3 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        registerSpawnTracker(ClientboundPackets1_20_2.SPAWN_ENTITY);
        registerMetadataRewriter(ClientboundPackets1_20_2.ENTITY_METADATA, Types1_20_3.METADATA_LIST, Types1_20_2.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_20_2.REMOVE_ENTITIES);
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, meta) -> {
            final MetaType type = meta.metaType();
            if (type == Types1_20_3.META_TYPES.componentType) {
                meta.setTypeAndValue(Types1_20_2.META_TYPES.componentType, Protocol1_20_3To1_20_2.tagComponentToJson(meta.value()));
            } else if (type == Types1_20_3.META_TYPES.optionalComponentType) {
                meta.setTypeAndValue(Types1_20_2.META_TYPES.optionalComponentType, Protocol1_20_3To1_20_2.tagComponentToJson(meta.value()));
            } else {
                meta.setMetaType(Types1_20_2.META_TYPES.byId(type.typeId()));
            }
        });

        registerMetaTypeHandler(null, null, null, Types1_20_2.META_TYPES.particleType);
    }

    @Override
    public EntityType typeFromId(final int type) {
        return Entity1_19_4Types.getTypeFromId(type);
    }
}