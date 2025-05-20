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
package com.viaversion.viabackwards.protocol.v1_22to1_21_5;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_22to1_21_5.rewriter.BlockItemPacketRewriter1_22;
import com.viaversion.viabackwards.protocol.v1_22to1_21_5.rewriter.ComponentRewriter1_22;
import com.viaversion.viabackwards.protocol.v1_22to1_21_5.rewriter.EntityPacketRewriter1_22;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_22;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.data.item.ItemHasherBase;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.rewriter.CommandRewriter1_19_4;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.Protocol1_21_5To1_22;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ClientboundConfigurationPackets1_22;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ClientboundPacket1_22;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ClientboundPackets1_22;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ServerboundConfigurationPackets1_22;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ServerboundPacket1_22;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ServerboundPackets1_22;
import com.viaversion.viaversion.rewriter.AttributeRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.SerializerVersion;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_22To1_21_5 extends BackwardsProtocol<ClientboundPacket1_22, ClientboundPacket1_21_5, ServerboundPacket1_22, ServerboundPacket1_21_5> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.21.6", "1.21.5", Protocol1_21_5To1_22.class);
    private final EntityPacketRewriter1_22 entityRewriter = new EntityPacketRewriter1_22(this);
    private final BlockItemPacketRewriter1_22 itemRewriter = new BlockItemPacketRewriter1_22(this);
    private final ParticleRewriter<ClientboundPacket1_22> particleRewriter = new ParticleRewriter<>(this);
    private final NBTComponentRewriter<ClientboundPacket1_22> translatableRewriter = new ComponentRewriter1_22(this);
    private final TagRewriter<ClientboundPacket1_22> tagRewriter = new TagRewriter<>(this);

    public Protocol1_22To1_21_5() {
        super(ClientboundPacket1_22.class, ClientboundPacket1_21_5.class, ServerboundPacket1_22.class, ServerboundPacket1_21_5.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        tagRewriter.removeTags("dialog");
        tagRewriter.registerGeneric(ClientboundPackets1_22.UPDATE_TAGS);
        tagRewriter.registerGeneric(ClientboundConfigurationPackets1_22.UPDATE_TAGS);

        final SoundRewriter<ClientboundPacket1_22> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_22.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_22.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets1_22.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_22.AWARD_STATS);
        new AttributeRewriter<>(this).register1_21(ClientboundPackets1_22.UPDATE_ATTRIBUTES);
        new CommandRewriter1_19_4<>(this) {
            @Override
            public void handleArgument(final PacketWrapper wrapper, final String argumentType) {
                if (argumentType.equals("minecraft:hex_color") || argumentType.equals("minecraft:dialog")) {
                    wrapper.write(Types.VAR_INT, 0); // Word
                } else {
                    super.handleArgument(wrapper, argumentType);
                }
            }
        }.registerDeclareCommands1_19(ClientboundPackets1_22.COMMANDS);

        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_22.OPEN_SCREEN);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_22.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_22.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_22.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_22.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_22.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_22.TAB_LIST);
        translatableRewriter.registerPlayerCombatKill1_20(ClientboundPackets1_22.PLAYER_COMBAT_KILL);
        translatableRewriter.registerPlayerInfoUpdate1_21_4(ClientboundPackets1_22.PLAYER_INFO_UPDATE);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_22.SYSTEM_CHAT);
        translatableRewriter.registerDisguisedChat(ClientboundPackets1_22.DISGUISED_CHAT);
        translatableRewriter.registerPlayerChat1_21_5(ClientboundPackets1_22.PLAYER_CHAT);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_21_4(ClientboundPackets1_22.LEVEL_PARTICLES);
        particleRewriter.registerExplode1_21_2(ClientboundPackets1_22.EXPLODE);

        cancelClientbound(ClientboundPackets1_22.TRACKED_WAYPOINT);
        cancelClientbound(ClientboundPackets1_22.CLEAR_DIALOG);
        cancelClientbound(ClientboundPackets1_22.SHOW_DIALOG);
        cancelClientbound(ClientboundConfigurationPackets1_22.CLEAR_DIALOG);
        cancelClientbound(ClientboundConfigurationPackets1_22.SHOW_DIALOG);
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_22.PLAYER));
        addItemHasher(user, new ItemHasherBase(this, user, SerializerVersion.V1_21_5, SerializerVersion.V1_21_5));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_22 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_22 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_22> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public NBTComponentRewriter<ClientboundPacket1_22> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_22> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public VersionedTypesHolder types() {
        return VersionedTypes.V1_22;
    }

    @Override
    public VersionedTypesHolder mappedTypes() {
        return VersionedTypes.V1_21_5;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_22, ClientboundPacket1_21_5, ServerboundPacket1_22, ServerboundPacket1_21_5> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_22.class, ClientboundConfigurationPackets1_22.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_5.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_22.class, ServerboundConfigurationPackets1_22.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_5.class, ServerboundConfigurationPackets1_20_5.class)
        );
    }
}
