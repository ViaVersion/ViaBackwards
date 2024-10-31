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
package com.viaversion.viabackwards.protocol.template;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.types.version.Types1_21_2;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

// Placeholders to replace (in the entire package):
//   Protocol1_98To1_99, EntityPacketRewriter1_99, BlockItemPacketRewriter1_99
//   Protocol1_20_5To1_21 (the ViaVersion protocol class the mappings depend on)
//   ClientboundPacket1_21_2
//   ServerboundPacket1_21_2
//   ClientboundConfigurationPackets1_21
//   ServerboundConfigurationPackets1_20_5
//   EntityTypes1_20_5 (UNMAPPED type)
//   Types1_21_2.PARTICLE
//   1.99, 1.98
final class Protocol1_98To1_99 extends BackwardsProtocol<ClientboundPacket1_21_2, ClientboundPacket1_21_2, ServerboundPacket1_21_2, ServerboundPacket1_21_2> {

    // ViaBackwards uses its own mappings and also needs a translatablerewriter for translation mappings
    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.99", "1.98", Protocol1_20_5To1_21.class);
    private final EntityPacketRewriter1_99 entityRewriter = new EntityPacketRewriter1_99(this);
    private final BlockItemPacketRewriter1_99 itemRewriter = new BlockItemPacketRewriter1_99(this);
    private final ParticleRewriter<ClientboundPacket1_21_2> particleRewriter = new ParticleRewriter<>(this, Types1_21_2.PARTICLE/*, Types1_OLD.PARTICLE*/);
    private final ComponentRewriter1_99 translatableRewriter = new ComponentRewriter1_99(this);
    private final TagRewriter<ClientboundPacket1_21_2> tagRewriter = new TagRewriter<>(this);

    public Protocol1_98To1_99() {
        super(ClientboundPacket1_21_2.class, ClientboundPacket1_21_2.class, ServerboundPacket1_21_2.class, ServerboundPacket1_21_2.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        tagRewriter.registerGeneric(ClientboundPackets1_21_2.UPDATE_TAGS);
        tagRewriter.registerGeneric(ClientboundConfigurationPackets1_21.UPDATE_TAGS);

        final SoundRewriter<ClientboundPacket1_21_2> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_2.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_2.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets1_21_2.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_21_2.AWARD_STATS);
        //new AttributeRewriter<>(this).register1_21(ClientboundPackets1_21_2.UPDATE_ATTRIBUTES);

        // Registers translatable mappings (missing a whole bunch still)
        //translatableRewriter.registerOpenScreen(ClientboundPackets1_21_2.OPEN_SCREEN); // Handled by registerOpenScreen in item rewriters
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_21_2.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_21_2.TAB_LIST);
        translatableRewriter.registerPlayerCombatKill1_20(ClientboundPackets1_21_2.PLAYER_COMBAT_KILL);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SYSTEM_CHAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.DISGUISED_CHAT);
        translatableRewriter.registerPlayerInfoUpdate1_21_2(ClientboundPackets1_21_2.PLAYER_INFO_UPDATE);
        translatableRewriter.registerPing();

        // If needed for any particle, item, or block changes. Extend ParticleRewriter for particle serializer changes
        particleRewriter.registerLevelParticles1_20_5(ClientboundPackets1_21_2.LEVEL_PARTICLES);
        particleRewriter.registerExplode1_21_2(ClientboundPackets1_21_2.EXPLODE);
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_20_5.PLAYER));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_99 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_99 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_21_2> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public ComponentRewriter1_99 getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_21_2> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_21_2, ClientboundPacket1_21_2, ServerboundPacket1_21_2, ServerboundPacket1_21_2> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_21_2.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_2.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_21_2.class, ServerboundConfigurationPackets1_20_5.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_2.class, ServerboundConfigurationPackets1_20_5.class)
        );
    }
}
