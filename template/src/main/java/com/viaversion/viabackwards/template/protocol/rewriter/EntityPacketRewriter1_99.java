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
package com.viaversion.viabackwards.template.protocol.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.template.protocol.Protocol1_98To1_99;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.util.Key;

// Replace if needed
//  Types1_OLD
//  Types1_20_5
public final class EntityPacketRewriter1_99 extends EntityRewriter<ClientboundPacket1_20_5, Protocol1_98To1_99> {

    public EntityPacketRewriter1_99(final Protocol1_98To1_99 protocol) {
        super(protocol, Types1_20_5.ENTITY_DATA_TYPES.optionalComponentType, Types1_20_5.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_20_5.ADD_ENTITY, EntityTypes1_20_5.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_20_5.SET_ENTITY_DATA, /*Types1_OLD.ENTITY_DATA_LIST, */Types1_20_5.ENTITY_DATA_LIST); // Specify old and new entity data list if changed
        registerRemoveEntities(ClientboundPackets1_20_5.REMOVE_ENTITIES);

        // TODO Item and sound id changes in registries, probably others as well
        protocol.registerClientbound(ClientboundConfigurationPackets1_20_5.REGISTRY_DATA, wrapper -> {
            final String registryKey = Key.stripMinecraftNamespace(wrapper.passthrough(Types.STRING));
            final RegistryEntry[] entries = wrapper.passthrough(Types.REGISTRY_ENTRY_ARRAY);
            handleRegistryData1_20_5(wrapper.user(), registryKey, entries); // Caches dimensions to access data like height later and tracks the amount of biomes sent for chunk data
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity id
                map(Types.BOOLEAN); // Hardcore
                map(Types.STRING_ARRAY); // World List
                map(Types.VAR_INT); // Max players
                map(Types.VAR_INT); // View distance
                map(Types.VAR_INT); // Simulation distance
                map(Types.BOOLEAN); // Reduced debug info
                map(Types.BOOLEAN); // Show death screen
                map(Types.BOOLEAN); // Limited crafting
                map(Types.VAR_INT); // Dimension key
                map(Types.STRING); // World
                handler(worldDataTrackerHandlerByKey1_20_5(3)); // Tracks world height and name for chunk data and entity (un)tracking
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.RESPAWN, wrapper -> {
            final int dimensionId = wrapper.passthrough(Types.VAR_INT);
            final String world = wrapper.passthrough(Types.STRING);
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world); // Tracks world height and name for chunk data and entity (un)tracking
        });
    }

    @Override
    protected void registerRewrites() {
        /*filter().handler((event, data) -> {
            int id = data.dataType().typeId();
            if (id >= ac) {
                return;
            } else if (id >= ab) {
                id--;
            }

            data.setDataType(Types1_20_5.ENTITY_DATA_TYPES.byId(id));
        });*/

        registerEntityDataTypeHandler1_20_3(
            Types1_20_5.ENTITY_DATA_TYPES.itemType,
            Types1_20_5.ENTITY_DATA_TYPES.blockStateType,
            Types1_20_5.ENTITY_DATA_TYPES.optionalBlockStateType,
            Types1_20_5.ENTITY_DATA_TYPES.particleType,
            Types1_20_5.ENTITY_DATA_TYPES.particlesType,
            Types1_20_5.ENTITY_DATA_TYPES.componentType,
            Types1_20_5.ENTITY_DATA_TYPES.optionalComponentType
        );
        registerBlockStateHandler(EntityTypes1_20_5.ABSTRACT_MINECART, 11);

        // Remove entity data of new entity type
        // filter().type(EntityTypes1_20_5.SNIFFER).removeIndex(newIndex);
    }

    @Override
    public void onMappingDataLoaded() {
        // If types changed, uncomment to map them
        // mapTypes();

        // mapEntityTypeWithData(EntityTypes1_20_5.SNIFFER, EntityTypes1_20_5.RAVAGER).tagName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_20_5.getTypeFromId(type);
    }
}