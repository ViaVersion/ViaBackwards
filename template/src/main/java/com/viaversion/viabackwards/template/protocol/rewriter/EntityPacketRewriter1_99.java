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
package com.viaversion.viabackwards.template.protocol.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.template.protocol.Protocol1_98To_99;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_4;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundPackets1_20_2;

// Replace if needed
//  Types1_OLD
//  Types1_20_3
public final class EntityPacketRewriter1_99 extends EntityRewriter<ClientboundPackets1_20_2, Protocol1_98To_99> {

    public EntityPacketRewriter1_99(final Protocol1_98To_99 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_20_2.SPAWN_ENTITY, EntityTypes1_19_4.FALLING_BLOCK);
        registerMetadataRewriter(ClientboundPackets1_20_2.ENTITY_METADATA, /*Types1_OLD.METADATA_LIST, */Types1_20_3.METADATA_LIST); // Specify old and new metadata list if changed
        registerRemoveEntities(ClientboundPackets1_20_2.REMOVE_ENTITIES);

        protocol.registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.REGISTRY_DATA, new PacketHandlers() {
            @Override
            protected void register() {
                map(Type.COMPOUND_TAG); // Registry data
                handler(configurationDimensionDataHandler()); // Caches dimensions to access data like height later
                handler(configurationBiomeSizeTracker()); // Tracks the amount of biomes sent for chunk data
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity id
                map(Type.BOOLEAN); // Hardcore
                map(Type.STRING_ARRAY); // World List
                map(Type.VAR_INT); // Max players
                map(Type.VAR_INT); // View distance
                map(Type.VAR_INT); // Simulation distance
                map(Type.BOOLEAN); // Reduced debug info
                map(Type.BOOLEAN); // Show death screen
                map(Type.BOOLEAN); // Limited crafting
                map(Type.STRING); // Dimension key
                map(Type.STRING); // World
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Dimension
                map(Type.STRING); // World
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
            }
        });
    }

    @Override
    protected void registerRewrites() {
        /*filter().handler((event, meta) -> {
            int id = meta.metaType().typeId();
            if (id >= ac) {
                return;
            } else if (id >= ab) {
                id--;
            }

            meta.setMetaType(Types1_20_3.META_TYPES.byId(id));
        });*/

        registerMetaTypeHandler(
                Types1_20_3.META_TYPES.itemType,
                Types1_20_3.META_TYPES.blockStateType,
                Types1_20_3.META_TYPES.optionalBlockStateType,
                Types1_20_3.META_TYPES.particleType,
                Types1_20_3.META_TYPES.componentType,
                Types1_20_3.META_TYPES.optionalComponentType
        );

        filter().filterFamily(EntityTypes1_19_4.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int blockState = meta.value();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(blockState));
        });

        // Remove metadata of new entity type
        // filter().type(Entity1_xTypes.SNIFFER).removeIndex(newIndex);
    }

    @Override
    public void onMappingDataLoaded() {
        // If types changed, uncomment to map them
        // mapTypes();

        // mapEntityTypeWithData(EntityTypes1_19_4.SNIFFER, EntityTypes1_19_4.RAVAGER).jsonName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_19_4.getTypeFromId(type);
    }
}