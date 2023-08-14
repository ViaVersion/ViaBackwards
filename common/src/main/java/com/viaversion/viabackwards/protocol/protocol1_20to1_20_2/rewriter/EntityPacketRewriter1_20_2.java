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
package com.viaversion.viabackwards.protocol.protocol1_20to1_20_2.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20to1_20_2.Protocol1_20To1_20_2;
import com.viaversion.viabackwards.protocol.protocol1_20to1_20_2.storage.ConfigurationPacketStorage;
import com.viaversion.viaversion.api.minecraft.GlobalPosition;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19_4Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_20;
import com.viaversion.viaversion.api.type.types.version.Types1_20_2;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundPackets1_20_2;

public final class EntityPacketRewriter1_20_2 extends EntityRewriter<ClientboundPackets1_20_2, Protocol1_20To1_20_2> {

    public EntityPacketRewriter1_20_2(final Protocol1_20To1_20_2 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_20_2.SPAWN_ENTITY, Entity1_19_4Types.FALLING_BLOCK);
        registerMetadataRewriter(ClientboundPackets1_20_2.ENTITY_METADATA, Types1_20_2.METADATA_LIST, Types1_20.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_20_2.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_20_2.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    final ConfigurationPacketStorage configurationPacketStorage = wrapper.user().remove(ConfigurationPacketStorage.class);
                    wrapper.passthrough(Type.INT); // Entity id
                    wrapper.passthrough(Type.BOOLEAN); // Hardcore

                    final String[] worlds = wrapper.read(Type.STRING_ARRAY);
                    final int maxPlayers = wrapper.read(Type.VAR_INT);
                    final int viewDistance = wrapper.read(Type.VAR_INT);
                    final int simulationDistance = wrapper.read(Type.VAR_INT);
                    final boolean reducedDebugInfo = wrapper.read(Type.BOOLEAN);
                    final boolean showRespawnScreen = wrapper.read(Type.BOOLEAN);
                    final String dimensionType = wrapper.read(Type.STRING);
                    final String world = wrapper.read(Type.STRING);
                    final long seed = wrapper.read(Type.LONG);

                    wrapper.write(Type.UNSIGNED_BYTE, wrapper.read(Type.BYTE).shortValue()); // Gamemode
                    wrapper.passthrough(Type.BYTE); // Previous gamemode

                    wrapper.write(Type.STRING_ARRAY, worlds);
                    wrapper.write(Type.NBT, configurationPacketStorage.registry());
                    wrapper.write(Type.STRING, dimensionType);
                    wrapper.write(Type.STRING, world);
                    wrapper.write(Type.LONG, seed);
                    wrapper.write(Type.VAR_INT, maxPlayers);
                    wrapper.write(Type.VAR_INT, viewDistance);
                    wrapper.write(Type.VAR_INT, simulationDistance);
                    wrapper.write(Type.BOOLEAN, reducedDebugInfo);
                    wrapper.write(Type.BOOLEAN, showRespawnScreen);

                    wrapper.send(Protocol1_20To1_20_2.class);
                    wrapper.cancel();

                    final PacketWrapper featuresPacket = wrapper.create(ClientboundPackets1_19_4.UPDATE_ENABLED_FEATURES);
                    featuresPacket.write(Type.STRING_ARRAY, configurationPacketStorage.enabledFeatures());
                    featuresPacket.send(Protocol1_20To1_20_2.class);

                    configurationPacketStorage.sendQueuedPackets(wrapper.user());
                });
                handler(worldDataTrackerHandlerByKey());
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    wrapper.passthrough(Type.STRING); // Dimension type
                    wrapper.passthrough(Type.STRING); // World
                    wrapper.passthrough(Type.LONG); // Seed
                    wrapper.write(Type.UNSIGNED_BYTE, wrapper.read(Type.BYTE).shortValue()); // Gamemode
                    wrapper.passthrough(Type.BYTE); // Previous gamemode
                    wrapper.passthrough(Type.BOOLEAN); // Debug
                    wrapper.passthrough(Type.BOOLEAN); // Flat

                    final GlobalPosition lastDeathPosition = wrapper.read(Type.OPTIONAL_GLOBAL_POSITION);
                    final int portalCooldown = wrapper.read(Type.VAR_INT);

                    wrapper.passthrough(Type.BYTE); // Data to keep

                    wrapper.write(Type.OPTIONAL_GLOBAL_POSITION, lastDeathPosition);
                    wrapper.write(Type.VAR_INT, portalCooldown);
                });
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.ENTITY_EFFECT, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Entity id
            wrapper.write(Type.VAR_INT, wrapper.read(Type.VAR_INT) + 1); // Effect id
            wrapper.passthrough(Type.BYTE); // Amplifier
            wrapper.passthrough(Type.VAR_INT); // Duration
            wrapper.passthrough(Type.BYTE); // Flags
            if (wrapper.passthrough(Type.BOOLEAN)) {
                wrapper.write(Type.NBT, wrapper.read(Type.NAMELESS_NBT)); // Factor data
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_2.REMOVE_ENTITY_EFFECT, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Entity id
            wrapper.write(Type.VAR_INT, wrapper.read(Type.VAR_INT) + 1); // Effect id
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, meta) -> meta.setMetaType(Types1_20.META_TYPES.byId(meta.metaType().typeId())));
        registerMetaTypeHandler(null, Types1_20.META_TYPES.blockStateType, Types1_20.META_TYPES.optionalBlockStateType, Types1_20.META_TYPES.particleType, null, null);

        filter().filterFamily(Entity1_19_4Types.DISPLAY).removeIndex(10);

        filter().filterFamily(Entity1_19_4Types.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int blockState = meta.value();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(blockState));
        });
    }

    @Override
    public EntityType typeFromId(final int type) {
        return Entity1_19_4Types.getTypeFromId(type);
    }
}