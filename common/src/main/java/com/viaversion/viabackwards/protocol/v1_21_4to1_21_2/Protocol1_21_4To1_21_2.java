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
package com.viaversion.viabackwards.protocol.v1_21_4to1_21_2;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.rewriter.BlockItemPacketRewriter1_21_4;
import com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.rewriter.ComponentRewriter1_21_4;
import com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.rewriter.EntityPacketRewriter1_21_4;
import com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.rewriter.ParticleRewriter1_21_4;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_4;
import com.viaversion.viaversion.api.minecraft.item.data.ChatType;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPacket1_21_4;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.AttributeRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.ArrayUtil;
import java.util.BitSet;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_21_4To1_21_2 extends BackwardsProtocol<ClientboundPacket1_21_2, ClientboundPacket1_21_2, ServerboundPacket1_21_4, ServerboundPacket1_21_2> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.21.4", "1.21.2", Protocol1_21_2To1_21_4.class);
    private final EntityPacketRewriter1_21_4 entityRewriter = new EntityPacketRewriter1_21_4(this);
    private final BlockItemPacketRewriter1_21_4 itemRewriter = new BlockItemPacketRewriter1_21_4(this);
    private final ParticleRewriter<ClientboundPacket1_21_2> particleRewriter = new ParticleRewriter1_21_4(this);
    private final JsonNBTComponentRewriter<ClientboundPacket1_21_2> translatableRewriter = new ComponentRewriter1_21_4(this);
    private final TagRewriter<ClientboundPacket1_21_2> tagRewriter = new TagRewriter<>(this);

    public Protocol1_21_4To1_21_2() {
        super(ClientboundPacket1_21_2.class, ClientboundPacket1_21_2.class, ServerboundPacket1_21_4.class, ServerboundPacket1_21_2.class);
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
        new AttributeRewriter<>(this).register1_21(ClientboundPackets1_21_2.UPDATE_ATTRIBUTES);

        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_21_2.OPEN_SCREEN);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_21_2.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.DISCONNECT);
        translatableRewriter.registerComponentPacket(ClientboundConfigurationPackets1_21.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_21_2.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_21_2.SET_PLAYER_TEAM);
        translatableRewriter.registerPlayerCombatKill1_20(ClientboundPackets1_21_2.PLAYER_COMBAT_KILL);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SYSTEM_CHAT);
        translatableRewriter.registerDisguisedChat(ClientboundPackets1_21_2.DISGUISED_CHAT);
        translatableRewriter.registerPlayerChat(ClientboundPackets1_21_2.PLAYER_CHAT, ChatType.TYPE);
        translatableRewriter.registerSetObjective(ClientboundPackets1_21_2.SET_OBJECTIVE);
        translatableRewriter.registerSetScore1_20_3(ClientboundPackets1_21_2.SET_SCORE);
        translatableRewriter.registerPing();

        particleRewriter.registerExplode1_21_2(ClientboundPackets1_21_2.EXPLODE);
        registerClientbound(ClientboundPackets1_21_2.LEVEL_PARTICLES, wrapper -> {
            wrapper.passthrough(Types.BOOLEAN); // Override limiter
            wrapper.read(Types.BOOLEAN); // Always show
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.FLOAT); // Offset X
            wrapper.passthrough(Types.FLOAT); // Offset Y
            wrapper.passthrough(Types.FLOAT); // Offset Z
            wrapper.passthrough(Types.FLOAT); // Particle Data
            wrapper.passthrough(Types.INT); // Particle Count
            final Particle particle = wrapper.passthroughAndMap(VersionedTypes.V1_21_4.particle(), VersionedTypes.V1_21_2.particle());
            particleRewriter.rewriteParticle(wrapper.user(), particle);
        });

        registerClientbound(ClientboundConfigurationPackets1_21.UPDATE_ENABLED_FEATURES, wrapper -> {
            final String[] enabledFeatures = wrapper.read(Types.STRING_ARRAY);
            wrapper.write(Types.STRING_ARRAY, ArrayUtil.add(enabledFeatures, "winter_drop"));
        });

        registerClientbound(ClientboundPackets1_21_2.PLAYER_INFO_UPDATE, wrapper -> {
            final BitSet actions = wrapper.read(Types.PROFILE_ACTIONS_ENUM1_21_4);
            // Remove new action
            final BitSet updatedActions = new BitSet(7);
            for (int i = 0; i < 7; i++) {
                if (actions.get(i)) {
                    updatedActions.set(i);
                }
            }
            wrapper.write(Types.PROFILE_ACTIONS_ENUM1_21_2, updatedActions);

            final int entries = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < entries; i++) {
                wrapper.passthrough(Types.UUID);
                if (actions.get(0)) {
                    wrapper.passthrough(Types.STRING); // Player Name
                    wrapper.passthrough(Types.PROFILE_PROPERTY_ARRAY);
                }
                if (actions.get(1) && wrapper.passthrough(Types.BOOLEAN)) {
                    wrapper.passthrough(Types.UUID); // Session UUID
                    wrapper.passthrough(Types.PROFILE_KEY);
                }
                if (actions.get(2)) {
                    wrapper.passthrough(Types.VAR_INT); // Gamemode
                }
                if (actions.get(3)) {
                    wrapper.passthrough(Types.BOOLEAN); // Listed
                }
                if (actions.get(4)) {
                    wrapper.passthrough(Types.VAR_INT); // Latency
                }
                if (actions.get(5)) {
                    translatableRewriter.processTag(wrapper.user(), wrapper.passthrough(Types.OPTIONAL_TAG));
                }
                if (actions.get(6)) {
                    wrapper.passthrough(Types.VAR_INT); // List order
                }

                // Remove
                if (actions.get(7)) {
                    wrapper.read(Types.BOOLEAN); // Show head
                }
            }
        });
    }

    @Override
    protected void onMappingDataLoaded() {
        super.onMappingDataLoaded();
        tagRewriter.addEmptyTags(RegistryType.ITEM, "minecraft:tall_flowers", "minecraft:flowers");
        tagRewriter.addEmptyTag(RegistryType.BLOCK, "minecraft:tall_flowers");
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_21_4.PLAYER));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_21_4 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_21_4 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_21_2> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPacket1_21_2> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_21_2> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public VersionedTypesHolder types() {
        return VersionedTypes.V1_21_4;
    }

    @Override
    public VersionedTypesHolder mappedTypes() {
        return VersionedTypes.V1_21_2;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_21_2, ClientboundPacket1_21_2, ServerboundPacket1_21_4, ServerboundPacket1_21_2> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_21_2.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_2.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_21_4.class, ServerboundConfigurationPackets1_20_5.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_2.class, ServerboundConfigurationPackets1_20_5.class)
        );
    }
}
