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
package com.viaversion.viabackwards.protocol.v1_21_5to1_21_4;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.rewriter.BlockItemPacketRewriter1_21_5;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.rewriter.ComponentRewriter1_21_5;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.rewriter.EntityPacketRewriter1_21_5;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.rewriter.RegistryDataRewriter1_21_5;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.storage.HashedItemConverterStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.data.version.StructuredDataKeys1_21_2;
import com.viaversion.viaversion.api.minecraft.data.version.StructuredDataKeys1_21_5;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_4;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_2;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_21_5;
import com.viaversion.viaversion.api.minecraft.item.data.ChatType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.rewriter.CommandRewriter1_19_4;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPacket1_21_4;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.Limit;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_21_5To1_21_4 extends BackwardsProtocol<ClientboundPacket1_21_5, ClientboundPacket1_21_2, ServerboundPacket1_21_5, ServerboundPacket1_21_4> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.21.5", "1.21.4", Protocol1_21_4To1_21_5.class);
    private final EntityPacketRewriter1_21_5 entityRewriter = new EntityPacketRewriter1_21_5(this);
    private final BlockItemPacketRewriter1_21_5 itemRewriter = new BlockItemPacketRewriter1_21_5(this);
    private final ParticleRewriter<ClientboundPacket1_21_5> particleRewriter = new ParticleRewriter<>(this) {
        @Override
        public void rewriteParticle(final UserConnection connection, final Particle particle) {
            if (particle.id() == MAPPINGS.getParticleMappings().id("tinted_leaves")) {
                particle.getArguments().clear();
            }
            super.rewriteParticle(connection, particle);
        }
    };
    private final ComponentRewriter1_21_5 translatableRewriter = new ComponentRewriter1_21_5(this);
    private final TagRewriter<ClientboundPacket1_21_5> tagRewriter = new TagRewriter<>(this);
    private final RegistryDataRewriter registryDataRewriter = new RegistryDataRewriter1_21_5(this);

    public Protocol1_21_5To1_21_4() {
        super(ClientboundPacket1_21_5.class, ClientboundPacket1_21_2.class, ServerboundPacket1_21_5.class, ServerboundPacket1_21_4.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        registerClientbound(ClientboundConfigurationPackets1_21.REGISTRY_DATA, registryDataRewriter::handle);

        tagRewriter.registerGeneric(ClientboundPackets1_21_5.UPDATE_TAGS);
        tagRewriter.registerGeneric(ClientboundConfigurationPackets1_21.UPDATE_TAGS);

        final SoundRewriter<ClientboundPacket1_21_5> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_5.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_5.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets1_21_5.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_21_5.AWARD_STATS);

        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_21_5.OPEN_SCREEN);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_5.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_5.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_5.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_21_5.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_5.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_21_5.TAB_LIST);
        translatableRewriter.registerPlayerCombatKill1_20(ClientboundPackets1_21_5.PLAYER_COMBAT_KILL);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_5.SYSTEM_CHAT);
        translatableRewriter.registerDisguisedChat(ClientboundPackets1_21_5.DISGUISED_CHAT);
        translatableRewriter.registerPlayerInfoUpdate1_21_4(ClientboundPackets1_21_5.PLAYER_INFO_UPDATE);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_21_4(ClientboundPackets1_21_5.LEVEL_PARTICLES);
        particleRewriter.registerExplode1_21_2(ClientboundPackets1_21_5.EXPLODE);

        new CommandRewriter1_19_4<>(this) {
            @Override
            public void handleArgument(final PacketWrapper wrapper, final String argumentType) {
                if (argumentType.equals("minecraft:resource")) {
                    String resource = wrapper.read(Types.STRING);
                    if (Key.equals(resource, "test_instance")) {
                        resource = "minecraft:item"; // Just give it anything else
                    }
                    wrapper.write(Types.STRING, resource);
                } else if (argumentType.equals("minecraft:resource_selector")) {
                    wrapper.read(Types.STRING); // Selector
                    wrapper.write(Types.VAR_INT, 1); // Quotable string
                } else {
                    super.handleArgument(wrapper, argumentType);
                }
            }
        }.registerDeclareCommands1_19(ClientboundPackets1_21_5.COMMANDS);

        registerClientbound(ClientboundPackets1_21_5.PLAYER_CHAT, wrapper -> {
            wrapper.read(Types.VAR_INT); // Index

            wrapper.passthrough(Types.UUID); // Sender
            wrapper.passthrough(Types.VAR_INT); // Index
            wrapper.passthrough(Types.OPTIONAL_SIGNATURE_BYTES); // Signature
            wrapper.passthrough(Types.STRING); // Plain content
            wrapper.passthrough(Types.LONG); // Timestamp
            wrapper.passthrough(Types.LONG); // Salt

            final int lastSeen = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < lastSeen; i++) {
                final int index = wrapper.passthrough(Types.VAR_INT);
                if (index == 0) {
                    wrapper.passthrough(Types.SIGNATURE_BYTES);
                }
            }

            translatableRewriter.processTag(wrapper.user(), wrapper.passthrough(Types.OPTIONAL_TAG)); // Unsigned content

            final int filterMaskType = wrapper.passthrough(Types.VAR_INT);
            if (filterMaskType == 2) { // Partially filtered
                wrapper.passthrough(Types.LONG_ARRAY_PRIMITIVE); // Mask
            }

            wrapper.passthrough(ChatType.TYPE); // Chat Type
            translatableRewriter.processTag(wrapper.user(), wrapper.passthrough(Types.TAG)); // Name
            translatableRewriter.processTag(wrapper.user(), wrapper.passthrough(Types.OPTIONAL_TAG)); // Target Name
        });
        registerServerbound(ServerboundPackets1_21_4.CHAT_COMMAND_SIGNED, wrapper -> {
            wrapper.passthrough(Types.STRING); // Command
            wrapper.passthrough(Types.LONG); // Timestamp
            wrapper.passthrough(Types.LONG); // Salt
            final int signatures = Limit.max(wrapper.passthrough(Types.VAR_INT), 8);
            for (int i = 0; i < signatures; i++) {
                wrapper.passthrough(Types.STRING); // Argument name
                wrapper.passthrough(Types.SIGNATURE_BYTES); // Signature
            }
            wrapper.passthrough(Types.VAR_INT); // Offset
            wrapper.passthrough(Types.ACKNOWLEDGED_BIT_SET); // Acknowledged
            wrapper.write(Types.BYTE, (byte) 0); // Checksum
        });
        registerServerbound(ServerboundPackets1_21_4.CHAT, wrapper -> {
            wrapper.passthrough(Types.STRING); // Message
            wrapper.passthrough(Types.LONG); // Timestamp
            wrapper.passthrough(Types.LONG); // Salt
            wrapper.passthrough(Types.OPTIONAL_SIGNATURE_BYTES); // Signature
            wrapper.passthrough(Types.VAR_INT); // Offset
            wrapper.passthrough(Types.ACKNOWLEDGED_BIT_SET); // Acknowledged
            wrapper.write(Types.BYTE, (byte) 0); // Checksum
        });

        cancelClientbound(ClientboundPackets1_21_5.TEST_INSTANCE_BLOCK_STATUS);
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_21_4.PLAYER));
        user.put(new HashedItemConverterStorage(this));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_21_5 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_21_5 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public RegistryDataRewriter getRegistryDataRewriter() {
        return registryDataRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_21_5> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public ComponentRewriter1_21_5 getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_21_5> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public Types1_20_5<StructuredDataKeys1_21_5, EntityDataTypes1_21_5> types() {
        return VersionedTypes.V1_21_5;
    }

    @Override
    public Types1_20_5<StructuredDataKeys1_21_2, EntityDataTypes1_21_2> mappedTypes() {
        return VersionedTypes.V1_21_4;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_21_5, ClientboundPacket1_21_2, ServerboundPacket1_21_5, ServerboundPacket1_21_4> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_21_5.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_2.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_21_5.class, ServerboundConfigurationPackets1_20_5.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_4.class, ServerboundConfigurationPackets1_20_5.class)
        );
    }
}
