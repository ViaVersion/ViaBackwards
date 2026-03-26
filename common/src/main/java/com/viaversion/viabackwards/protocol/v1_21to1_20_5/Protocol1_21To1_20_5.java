/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21to1_20_5;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.rewriter.BlockItemPacketRewriter1_21;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.rewriter.ComponentRewriter1_21;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.rewriter.EntityPacketRewriter1_21;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage.EnchantmentsPaintingsStorage;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage.OpenScreenStorage;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage.PlayerRotationStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.data.version.StructuredDataKeys1_20_5;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_20_5;
import com.viaversion.viaversion.api.minecraft.item.data.ChatType;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.api.type.types.version.Types1_21;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPacket1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.ArrayUtil;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_21To1_20_5 extends BackwardsProtocol<ClientboundPacket1_21, ClientboundPacket1_20_5, ServerboundPacket1_20_5, ServerboundPacket1_20_5> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.21", "1.20.5", Protocol1_20_5To1_21.class);
    private final EntityPacketRewriter1_21 entityRewriter = new EntityPacketRewriter1_21(this);
    private final BlockItemPacketRewriter1_21 itemRewriter = new BlockItemPacketRewriter1_21(this);
    private final ParticleRewriter<ClientboundPacket1_21> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPacket1_21> translatableRewriter = new ComponentRewriter1_21(this);
    private final TagRewriter<ClientboundPacket1_21> tagRewriter = new TagRewriter<>(this);
    private final BlockRewriter<ClientboundPacket1_21> blockRewriter = BlockRewriter.for1_20_2(this, ChunkType1_20_2::new);
    private final BackwardsRegistryRewriter registryDataRewriter = new BackwardsRegistryRewriter(this);

    public Protocol1_21To1_20_5() {
        super(ClientboundPacket1_21.class, ClientboundPacket1_20_5.class, ServerboundPacket1_20_5.class, ServerboundPacket1_20_5.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        // Format change we can't properly map - all this really results in is a desync one version earlier
        cancelClientbound(ClientboundPackets1_21.PROJECTILE_POWER);

        cancelClientbound(ClientboundPackets1_21.CUSTOM_REPORT_DETAILS);
        cancelClientbound(ClientboundPackets1_21.SERVER_LINKS);
        cancelClientbound(ClientboundConfigurationPackets1_21.CUSTOM_REPORT_DETAILS);
        cancelClientbound(ClientboundConfigurationPackets1_21.SERVER_LINKS);

        replaceClientbound(ClientboundPackets1_21.DISGUISED_CHAT, wrapper -> {
            translatableRewriter.processTag(wrapper.user(), wrapper.passthrough(Types.TRUSTED_TAG)); // Message

            final Holder<ChatType> chatType = wrapper.read(ChatType.TYPE);
            if (chatType.isDirect()) {
                // Oh well
                wrapper.write(Types.VAR_INT, 0);
                return;
            }

            wrapper.write(Types.VAR_INT, chatType.id());
        });
        replaceClientbound(ClientboundPackets1_21.PLAYER_CHAT, wrapper -> {
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

            wrapper.passthrough(Types.TRUSTED_OPTIONAL_TAG); // Unsigned content

            final int filterMaskType = wrapper.passthrough(Types.VAR_INT);
            if (filterMaskType == 2) {
                wrapper.passthrough(Types.LONG_ARRAY_PRIMITIVE); // Mask
            }

            final Holder<ChatType> chatType = wrapper.read(ChatType.TYPE);
            if (chatType.isDirect()) {
                // Oh well
                wrapper.write(Types.VAR_INT, 0);
            } else {
                wrapper.write(Types.VAR_INT, chatType.id());
            }

            translatableRewriter.processTag(wrapper.user(), wrapper.passthrough(Types.TRUSTED_TAG)); // Name
            translatableRewriter.processTag(wrapper.user(), wrapper.passthrough(Types.TRUSTED_OPTIONAL_TAG)); // Target Name
        });

        replaceClientbound(ClientboundPackets1_21.UPDATE_ATTRIBUTES, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID

            final int size = wrapper.passthrough(Types.VAR_INT);
            int newSize = size;
            for (int i = 0; i < size; i++) {
                final int attributeId = wrapper.read(Types.VAR_INT);
                final int mappedId = MAPPINGS.getNewAttributeId(attributeId);
                if (mappedId == -1) {
                    newSize--;

                    wrapper.read(Types.DOUBLE); // Base
                    final int modifierSize = wrapper.read(Types.VAR_INT);
                    for (int j = 0; j < modifierSize; j++) {
                        wrapper.read(Types.STRING); // ID
                        wrapper.read(Types.DOUBLE); // Amount
                        wrapper.read(Types.BYTE); // Operation
                    }
                    continue;
                }

                wrapper.write(Types.VAR_INT, mappedId);
                wrapper.passthrough(Types.DOUBLE); // Base
                final int modifierSize = wrapper.passthrough(Types.VAR_INT);
                for (int j = 0; j < modifierSize; j++) {
                    final String id = wrapper.read(Types.STRING);
                    wrapper.write(Types.UUID, Protocol1_20_5To1_21.mapAttributeId(id));
                    wrapper.passthrough(Types.DOUBLE); // Amount
                    wrapper.passthrough(Types.BYTE); // Operation
                }
            }

            if (size != newSize) {
                wrapper.set(Types.VAR_INT, 1, newSize);
            }
        });

        registerClientbound(ClientboundConfigurationPackets1_21.UPDATE_ENABLED_FEATURES, wrapper -> {
            final String[] enabledFeatures = wrapper.read(Types.STRING_ARRAY);
            wrapper.write(Types.STRING_ARRAY, ArrayUtil.add(enabledFeatures, "minecraft:update_1_21"));
        });
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_20_5.PLAYER));
        user.put(new EnchantmentsPaintingsStorage());
        user.put(new OpenScreenStorage());
        user.put(new PlayerRotationStorage());
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_21 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_21 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public BlockRewriter<ClientboundPacket1_21> getBlockRewriter() {
        return blockRewriter;
    }

    @Override
    public BackwardsRegistryRewriter getRegistryDataRewriter() {
        return registryDataRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_21> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPacket1_21> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_21> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public Types1_21 types() {
        return VersionedTypes.V1_21;
    }

    @Override
    public Types1_20_5<StructuredDataKeys1_20_5, EntityDataTypes1_20_5> mappedTypes() {
        return VersionedTypes.V1_20_5;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_21, ClientboundPacket1_20_5, ServerboundPacket1_20_5, ServerboundPacket1_20_5> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_21.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_20_5.class, ClientboundConfigurationPackets1_20_5.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_20_5.class, ServerboundConfigurationPackets1_20_5.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_20_5.class, ServerboundConfigurationPackets1_20_5.class)
        );
    }
}
