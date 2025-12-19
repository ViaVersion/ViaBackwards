/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v26_1to1_21_11;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter.BlockItemPacketRewriter26_1;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter.ComponentRewriter26_1;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter.EntityPacketRewriter26_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_11;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.data.item.ItemHasherBase;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.Protocol1_21_11To26_1;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPacket1_21_11;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPackets1_21_11;
import com.viaversion.viaversion.rewriter.AttributeRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol26_1To1_21_11 extends BackwardsProtocol<ClientboundPacket1_21_11, ClientboundPacket1_21_11, ServerboundPacket1_21_9, ServerboundPacket1_21_9> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("26.1", "1.21.11", Protocol1_21_11To26_1.class);
    private final EntityPacketRewriter26_1 entityRewriter = new EntityPacketRewriter26_1(this);
    private final BlockItemPacketRewriter26_1 itemRewriter = new BlockItemPacketRewriter26_1(this);
    private final ParticleRewriter<ClientboundPacket1_21_11> particleRewriter = new ParticleRewriter<>(this);
    private final NBTComponentRewriter<ClientboundPacket1_21_11> translatableRewriter = new ComponentRewriter26_1(this);
    private final TagRewriter<ClientboundPacket1_21_11> tagRewriter = new TagRewriter<>(this);
    private final RegistryDataRewriter registryDataRewriter = new BackwardsRegistryRewriter(this);

    public Protocol26_1To1_21_11() {
        super(ClientboundPacket1_21_11.class, ClientboundPacket1_21_11.class, ServerboundPacket1_21_9.class, ServerboundPacket1_21_9.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        registerClientbound(ClientboundConfigurationPackets1_21_9.REGISTRY_DATA, registryDataRewriter::handle);

        tagRewriter.registerGeneric(ClientboundPackets1_21_11.UPDATE_TAGS);
        tagRewriter.registerGeneric(ClientboundConfigurationPackets1_21_9.UPDATE_TAGS);

        final SoundRewriter<ClientboundPacket1_21_11> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_11.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_11.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets1_21_11.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_21_11.AWARD_STATS);
        new AttributeRewriter<>(this).register1_21(ClientboundPackets1_21_11.UPDATE_ATTRIBUTES);

        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_21_11.OPEN_SCREEN);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_11.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_11.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_11.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_21_11.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_11.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_21_11.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_21_5(ClientboundPackets1_21_11.SET_PLAYER_TEAM);
        translatableRewriter.registerPlayerCombatKill1_20(ClientboundPackets1_21_11.PLAYER_COMBAT_KILL);
        translatableRewriter.registerPlayerInfoUpdate1_21_4(ClientboundPackets1_21_11.PLAYER_INFO_UPDATE);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_11.SYSTEM_CHAT);
        translatableRewriter.registerDisguisedChat(ClientboundPackets1_21_11.DISGUISED_CHAT);
        translatableRewriter.registerPlayerChat1_21_5(ClientboundPackets1_21_11.PLAYER_CHAT);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_21_4(ClientboundPackets1_21_11.LEVEL_PARTICLES);
        particleRewriter.registerExplode1_21_9(ClientboundPackets1_21_11.EXPLODE);
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection, new EntityTrackerBase(connection, EntityTypes1_21_11.PLAYER));
        addItemHasher(connection, new ItemHasherBase(this, connection));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter26_1 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter26_1 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public RegistryDataRewriter getRegistryDataRewriter() {
        return registryDataRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_21_11> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public NBTComponentRewriter<ClientboundPacket1_21_11> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_21_11> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public VersionedTypesHolder types() {
        return VersionedTypes.V26_1;
    }

    @Override
    public VersionedTypesHolder mappedTypes() {
        return VersionedTypes.V1_21_11;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_21_11, ClientboundPacket1_21_11, ServerboundPacket1_21_9, ServerboundPacket1_21_9> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_21_11.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_11.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_21_6.class, ServerboundConfigurationPackets1_21_9.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_6.class, ServerboundConfigurationPackets1_21_9.class)
        );
    }
}
