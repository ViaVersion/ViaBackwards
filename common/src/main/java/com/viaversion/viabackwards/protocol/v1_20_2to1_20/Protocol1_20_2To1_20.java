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
package com.viaversion.viabackwards.protocol.v1_20_2to1_20;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.provider.AdvancementCriteriaProvider;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.rewriter.BlockItemPacketRewriter1_20_2;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.rewriter.EntityPacketRewriter1_20_2;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.storage.ConfigurationPacketStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_4;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.packet.Direction;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.rewriter.EntityRewriter;
import com.viaversion.viaversion.api.rewriter.ItemRewriter;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.exception.CancelException;
import com.viaversion.viaversion.exception.InformativeException;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ServerboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.Protocol1_20To1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import java.util.UUID;

public final class Protocol1_20_2To1_20 extends BackwardsProtocol<ClientboundPackets1_20_2, ClientboundPackets1_19_4, ServerboundPackets1_20_2, ServerboundPackets1_19_4> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.20.2", "1.20", Protocol1_20To1_20_2.class);
    private final EntityPacketRewriter1_20_2 entityPacketRewriter = new EntityPacketRewriter1_20_2(this);
    private final BlockItemPacketRewriter1_20_2 itemPacketRewriter = new BlockItemPacketRewriter1_20_2(this);
    private final ParticleRewriter<ClientboundPackets1_20_2> particleRewriter = new ParticleRewriter<>(this);
    private final TagRewriter<ClientboundPackets1_20_2> tagRewriter = new TagRewriter<>(this);

    public Protocol1_20_2To1_20() {
        super(ClientboundPackets1_20_2.class, ClientboundPackets1_19_4.class, ServerboundPackets1_20_2.class, ServerboundPackets1_19_4.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        tagRewriter.registerGeneric(ClientboundPackets1_20_2.UPDATE_TAGS);

        final SoundRewriter<ClientboundPackets1_20_2> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_20_2.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_20_2.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets1_20_2.STOP_SOUND);

        particleRewriter.registerLevelParticles1_19(ClientboundPackets1_20_2.LEVEL_PARTICLES);

        registerClientbound(ClientboundPackets1_20_2.SET_DISPLAY_OBJECTIVE, wrapper -> {
            final int slot = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.BYTE, (byte) slot);
        });

        registerClientbound(State.LOGIN, ClientboundLoginPackets.LOGIN_FINISHED, wrapper -> {
            // We can't set the internal state to configuration here as protocols down the line will expect the state to be play
            // Add this *before* sending the ack since the server might immediately answer
            wrapper.user().put(new ConfigurationPacketStorage());

            // Overwrite what is set by the base protocol
            wrapper.user().getProtocolInfo().setClientState(State.LOGIN);

            // States set to configuration in the base protocol
            wrapper.create(ServerboundLoginPackets.LOGIN_ACKNOWLEDGED).scheduleSendToServer(Protocol1_20_2To1_20.class);
        });

        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.FINISH_CONFIGURATION, wrapper -> {
            wrapper.cancel();
            wrapper.user().getProtocolInfo().setServerState(State.PLAY);
            wrapper.user().get(ConfigurationPacketStorage.class).setFinished(true);

            wrapper.create(ServerboundConfigurationPackets1_20_2.FINISH_CONFIGURATION).sendToServer(Protocol1_20_2To1_20.class);
            wrapper.user().getProtocolInfo().setClientState(State.PLAY);
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO, wrapper -> {
            wrapper.passthrough(Types.STRING); // Name

            // TODO Bad
            final UUID uuid = wrapper.read(Types.OPTIONAL_UUID);
            wrapper.write(Types.UUID, uuid != null ? uuid : new UUID(0, 0));
        });

        registerClientbound(ClientboundPackets1_20_2.START_CONFIGURATION, null, wrapper -> {
            wrapper.cancel();
            wrapper.user().getProtocolInfo().setServerState(State.CONFIGURATION);

            // TODO: Check whether all the necessary data for the join game packet is always expected by the client or if we need to cache it from the initial login
            final PacketWrapper configAcknowledgedPacket = wrapper.create(ServerboundPackets1_20_2.CONFIGURATION_ACKNOWLEDGED);
            configAcknowledgedPacket.sendToServer(Protocol1_20_2To1_20.class);
            wrapper.user().getProtocolInfo().setClientState(State.CONFIGURATION);
            wrapper.user().put(new ConfigurationPacketStorage());
        });
        cancelClientbound(ClientboundPackets1_20_2.PONG_RESPONSE);

        // Some can be directly remapped to play packets, others need to be queued
        // Set the packet type properly so the state on it is changed
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.DISCONNECT.getId(), -1, wrapper -> {
            wrapper.setPacketType(ClientboundPackets1_19_4.DISCONNECT);
        });
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.KEEP_ALIVE.getId(), -1, wrapper -> {
            wrapper.setPacketType(ClientboundPackets1_19_4.KEEP_ALIVE);
        });
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.PING.getId(), -1, wrapper -> {
            wrapper.setPacketType(ClientboundPackets1_19_4.PING);
        });
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.RESOURCE_PACK.getId(), -1, wrapper -> {
            // Send after join. We have to pretend the client accepted, else the server won't continue...
            wrapper.user().get(ConfigurationPacketStorage.class).setResourcePack(wrapper);
            wrapper.cancel();

            final PacketWrapper acceptedResponse = wrapper.create(ServerboundConfigurationPackets1_20_2.RESOURCE_PACK);
            acceptedResponse.write(Types.VAR_INT, 3);
            acceptedResponse.sendToServer(Protocol1_20_2To1_20.class);

            final PacketWrapper downloadedResponse = wrapper.create(ServerboundConfigurationPackets1_20_2.RESOURCE_PACK);
            downloadedResponse.write(Types.VAR_INT, 0);
            downloadedResponse.sendToServer(Protocol1_20_2To1_20.class);
        });
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.REGISTRY_DATA.getId(), -1, wrapper -> {
            wrapper.cancel();

            final CompoundTag registry = wrapper.read(Types.COMPOUND_TAG);
            entityPacketRewriter.trackBiomeSize(wrapper.user(), registry);
            entityPacketRewriter.cacheDimensionData(wrapper.user(), registry);
            wrapper.user().get(ConfigurationPacketStorage.class).setRegistry(registry);
        });
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.UPDATE_ENABLED_FEATURES.getId(), -1, wrapper -> {
            final String[] enabledFeatures = wrapper.read(Types.STRING_ARRAY);
            wrapper.user().get(ConfigurationPacketStorage.class).setEnabledFeatures(enabledFeatures);
            wrapper.cancel();
        });
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.UPDATE_TAGS.getId(), -1, wrapper -> {
            tagRewriter.handleGeneric(wrapper);
            wrapper.user().get(ConfigurationPacketStorage.class).addRawPacket(wrapper, ClientboundPackets1_19_4.UPDATE_TAGS);
            wrapper.cancel();
        });
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.CUSTOM_PAYLOAD.getId(), -1, wrapper -> {
            wrapper.user().get(ConfigurationPacketStorage.class).addRawPacket(wrapper, ClientboundPackets1_19_4.CUSTOM_PAYLOAD);
            wrapper.cancel();
        });
    }

    @Override
    public void register(final ViaProviders providers) {
        providers.register(AdvancementCriteriaProvider.class, new AdvancementCriteriaProvider());
    }

    @Override
    public void transform(final Direction direction, final State state, final PacketWrapper wrapper) throws InformativeException, CancelException {
        final ConfigurationPacketStorage configurationPacketStorage = wrapper.user().get(ConfigurationPacketStorage.class);
        if (configurationPacketStorage == null || configurationPacketStorage.isFinished()) {
            super.transform(direction, state, wrapper);
            return;
        }
        if (direction == Direction.CLIENTBOUND) {
            super.transform(direction, State.CONFIGURATION, wrapper);
            return;
        }

        // Map some of the packets to their configuration counterparts
        final int id = wrapper.getId();
        if (id == ServerboundPackets1_19_4.CLIENT_INFORMATION.getId()) {
            wrapper.setPacketType(ServerboundConfigurationPackets1_20_2.CLIENT_INFORMATION);
        } else if (id == ServerboundPackets1_19_4.CUSTOM_PAYLOAD.getId()) {
            wrapper.setPacketType(ServerboundConfigurationPackets1_20_2.CUSTOM_PAYLOAD);
        } else if (id == ServerboundPackets1_19_4.KEEP_ALIVE.getId()) {
            wrapper.setPacketType(ServerboundConfigurationPackets1_20_2.KEEP_ALIVE);
        } else if (id == ServerboundPackets1_19_4.PONG.getId()) {
            wrapper.setPacketType(ServerboundConfigurationPackets1_20_2.PONG);
        } else if (id == ServerboundPackets1_19_4.RESOURCE_PACK.getId()) {
            wrapper.setPacketType(ServerboundConfigurationPackets1_20_2.RESOURCE_PACK);
        } else {
            // TODO Queue
            throw CancelException.generate();
        }
    }

    @Override
    protected void registerConfigurationChangeHandlers() {
        // Don't register them in the transitioning protocol
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection, new EntityTrackerBase(connection, EntityTypes1_19_4.PLAYER));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityRewriter<Protocol1_20_2To1_20> getEntityRewriter() {
        return entityPacketRewriter;
    }

    @Override
    public ItemRewriter<Protocol1_20_2To1_20> getItemRewriter() {
        return itemPacketRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_20_2> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_20_2> getTagRewriter() {
        return tagRewriter;
    }
}
