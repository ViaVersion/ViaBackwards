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
package com.viaversion.viabackwards.protocol.v1_21_9to1_21_7;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.rewriter.BlockItemPacketRewriter1_21_9;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.rewriter.ComponentRewriter1_21_9;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.rewriter.EntityPacketRewriter1_21_9;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.rewriter.ParticleRewriter1_21_9;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.rewriter.RegistryDataRewriter1_21_9;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.storage.DimensionScaleStorage;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.storage.PlayerRotationStorage;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.tracker.EntityTracker1_21_9;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.version.StructuredDataKeys1_21_5;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_9;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_5;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.item.ItemHasherBase;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundConfigurationPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPacket1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundConfigurationPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPacket1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.Protocol1_21_7To1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundPacket1_21_9;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_21_9To1_21_7 extends BackwardsProtocol<ClientboundPacket1_21_9, ClientboundPacket1_21_6, ServerboundPacket1_21_9, ServerboundPacket1_21_6> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.21.9", "1.21.7", Protocol1_21_7To1_21_9.class);
    private final EntityPacketRewriter1_21_9 entityRewriter = new EntityPacketRewriter1_21_9(this);
    private final BlockItemPacketRewriter1_21_9 itemRewriter = new BlockItemPacketRewriter1_21_9(this);
    private final ParticleRewriter<ClientboundPacket1_21_9> particleRewriter = new ParticleRewriter1_21_9(this);
    private final NBTComponentRewriter<ClientboundPacket1_21_9> translatableRewriter = new ComponentRewriter1_21_9(this);
    private final TagRewriter<ClientboundPacket1_21_9> tagRewriter = new TagRewriter<>(this);
    private final RegistryDataRewriter registryDataRewriter = new RegistryDataRewriter1_21_9(this);

    public Protocol1_21_9To1_21_7() {
        super(ClientboundPacket1_21_9.class, ClientboundPacket1_21_6.class, ServerboundPacket1_21_9.class, ServerboundPacket1_21_6.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        registerClientbound(ClientboundConfigurationPackets1_21_9.REGISTRY_DATA, registryDataRewriter::handle);

        tagRewriter.registerGeneric(ClientboundPackets1_21_9.UPDATE_TAGS);
        tagRewriter.registerGeneric(ClientboundConfigurationPackets1_21_9.UPDATE_TAGS);

        final SoundRewriter<ClientboundPacket1_21_9> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_9.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_9.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets1_21_9.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_21_9.AWARD_STATS);

        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_21_9.OPEN_SCREEN);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_9.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_9.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_9.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_21_9.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_9.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_21_9.TAB_LIST);
        translatableRewriter.registerPlayerCombatKill1_20(ClientboundPackets1_21_9.PLAYER_COMBAT_KILL);
        translatableRewriter.registerPlayerInfoUpdate1_21_4(ClientboundPackets1_21_9.PLAYER_INFO_UPDATE);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_9.SYSTEM_CHAT);
        translatableRewriter.registerDisguisedChat(ClientboundPackets1_21_9.DISGUISED_CHAT);
        translatableRewriter.registerPlayerChat1_21_5(ClientboundPackets1_21_9.PLAYER_CHAT);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_21_4(ClientboundPackets1_21_9.LEVEL_PARTICLES);
        registerClientbound(ClientboundPackets1_21_9.EXPLODE, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z

            wrapper.read(Types.FLOAT); // Radius
            wrapper.read(Types.INT); // Affected blocks

            if (wrapper.passthrough(Types.BOOLEAN)) {
                wrapper.passthrough(Types.DOUBLE); // Knockback X
                wrapper.passthrough(Types.DOUBLE); // Knockback Y
                wrapper.passthrough(Types.DOUBLE); // Knockback Z
            }

            particleRewriter.passthroughParticle(wrapper); // Explosion particle
            soundRewriter.soundHolderHandler().handle(wrapper);

            final int blockParticles = wrapper.read(Types.VAR_INT);
            for (int i = 0; i < blockParticles; i++) {
                wrapper.read(particleRewriter.particleType());
                wrapper.read(Types.FLOAT); // Scaling
                wrapper.read(Types.FLOAT); // Speed
                wrapper.read(Types.VAR_INT); // Weight
            }
        });

        registerClientbound(ClientboundConfigurationPackets1_21_9.CODE_OF_CONDUCT, ClientboundConfigurationPackets1_21_6.SHOW_DIALOG, wrapper -> {
            if (wrapper.user().getProtocolInfo().protocolVersion().olderThanOrEqualTo(ProtocolVersion.v1_21_5)) {
                // TODO Remove once we implement dialogs during the configuration state in 1.21.6->1.21.5
                wrapper.cancel();

                final PacketWrapper acceptPacket = wrapper.create(ServerboundConfigurationPackets1_21_9.ACCEPT_CODE_OF_CONDUCT);
                acceptPacket.sendToServer(Protocol1_21_9To1_21_7.class);
                return;
            }

            final String codeOfConduct = wrapper.read(Types.STRING);

            final CompoundTag tag = new CompoundTag();
            tag.putString("type", "minecraft:confirmation");
            tag.putString("title", translatableRewriter.mappedTranslationKey("multiplayer.codeOfConduct.title"));

            final CompoundTag body = new CompoundTag();
            body.putString("type", "minecraft:plain_message");
            body.putString("contents", codeOfConduct);
            tag.put("body", body);

            final CompoundTag yes = new CompoundTag();
            final CompoundTag yesLabel = new CompoundTag();
            yesLabel.putString("translate", "gui.acknowledge");
            yes.put("label", yesLabel);

            final CompoundTag acceptAction = new CompoundTag();
            acceptAction.putString("type", "minecraft:custom");
            acceptAction.putString("id", "viabackwards:ack_code_of_conduct");
            yes.put("action", acceptAction);
            tag.put("yes", yes);

            final CompoundTag no = new CompoundTag();
            final CompoundTag noLabel = new CompoundTag();
            noLabel.putString("translate", "menu.disconnect");
            no.put("label", noLabel);

            final CompoundTag disconnectAction = new CompoundTag();
            disconnectAction.putString("type", "minecraft:custom");
            disconnectAction.putString("id", "viabackwards:disconnect");
            no.put("action", disconnectAction);
            tag.put("no", no);

            wrapper.write(Types.TAG, tag);
        });

        registerServerbound(ServerboundConfigurationPackets1_21_6.CUSTOM_CLICK_ACTION, wrapper -> {
            final String id = wrapper.passthrough(Types.STRING);
            if ("viabackwards:ack_code_of_conduct".equals(id)) {
                wrapper.cancel();
                final PacketWrapper acceptPacket = wrapper.create(ServerboundConfigurationPackets1_21_9.ACCEPT_CODE_OF_CONDUCT);
                acceptPacket.sendToServer(Protocol1_21_9To1_21_7.class);
            } else if ("viabackwards:disconnect".equals(id)) {
                wrapper.cancel();
                wrapper.user().disconnect(translatableRewriter.mappedTranslationKey("multiplayer.disconnect.code_of_conduct"));
            }
        });

        registerServerbound(ServerboundPackets1_21_6.DEBUG_SAMPLE_SUBSCRIPTION, wrapper -> {
            final int sampleType = wrapper.read(Types.VAR_INT);
            if (sampleType == 0) { // TICK_TIME
                wrapper.write(Types.VAR_INT, 1); // Subscription count
                wrapper.write(Types.VAR_INT, 0); // Subscription registry id (DEDICATED_SERVER_TICK_TIME)
            }
        });

        cancelClientbound(ClientboundPackets1_21_9.DEBUG_BLOCK_VALUE);
        cancelClientbound(ClientboundPackets1_21_9.DEBUG_CHUNK_VALUE);
        cancelClientbound(ClientboundPackets1_21_9.DEBUG_ENTITY_VALUE);
        cancelClientbound(ClientboundPackets1_21_9.DEBUG_EVENT);
        cancelClientbound(ClientboundPackets1_21_9.GAME_EVENT_TEST_HIGHLIGHT_POS);
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection, new EntityTracker1_21_9(connection, EntityTypes1_21_9.PLAYER));
        addItemHasher(connection, new ItemHasherBase(this, connection));
        connection.put(new PlayerRotationStorage());
        connection.put(new DimensionScaleStorage());
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_21_9 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_21_9 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public RegistryDataRewriter getRegistryDataRewriter() {
        return registryDataRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_21_9> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public NBTComponentRewriter<ClientboundPacket1_21_9> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_21_9> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public VersionedTypesHolder types() {
        return VersionedTypes.V1_21_9;
    }

    @Override
    public Types1_20_5<StructuredDataKeys1_21_5, EntityDataTypes1_21_5> mappedTypes() {
        return VersionedTypes.V1_21_6;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_21_9, ClientboundPacket1_21_6, ServerboundPacket1_21_9, ServerboundPacket1_21_6> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_21_9.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_6.class, ClientboundConfigurationPackets1_21_6.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_21_6.class, ServerboundConfigurationPackets1_21_9.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_6.class, ServerboundConfigurationPackets1_21_6.class)
        );
    }
}
