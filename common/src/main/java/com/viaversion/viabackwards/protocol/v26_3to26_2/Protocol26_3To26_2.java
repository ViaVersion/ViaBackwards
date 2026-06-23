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
package com.viaversion.viabackwards.protocol.v26_3to26_2;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter.BlockItemPacketRewriter26_3;
import com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter.ComponentRewriter26_3;
import com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter.EntityPacketRewriter26_3;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType26_1;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.item.ItemHasherBase;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.RecipeDisplayRewriter1_21_5;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v26_2to26_3.Protocol26_2To26_3;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.block.BlockRewriter1_21_5;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol26_3To26_2 extends BackwardsProtocol<ClientboundPacket26_1, ClientboundPacket26_1, ServerboundPacket26_1, ServerboundPacket26_1> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("26.3", "26.2", Protocol26_2To26_3.class);
    private final EntityPacketRewriter26_3 entityRewriter = new EntityPacketRewriter26_3(this);
    private final BlockItemPacketRewriter26_3 itemRewriter = new BlockItemPacketRewriter26_3(this);
    private final ParticleRewriter<ClientboundPacket26_1> particleRewriter = new ParticleRewriter<>(this);
    private final NBTComponentRewriter<ClientboundPacket26_1> translatableRewriter = new ComponentRewriter26_3(this);
    private final TagRewriter<ClientboundPacket26_1> tagRewriter = new TagRewriter<>(this);
    private final BackwardsRegistryRewriter registryDataRewriter = new BackwardsRegistryRewriter(this);
    private final BlockRewriter<ClientboundPacket26_1> blockRewriter = new BlockRewriter1_21_5<>(this, ChunkType26_1::new);
    private final RecipeDisplayRewriter<ClientboundPacket26_1> recipeRewriter = new RecipeDisplayRewriter1_21_5<>(this);

    public Protocol26_3To26_2() {
        super(ClientboundPacket26_1.class, ClientboundPacket26_1.class, ServerboundPacket26_1.class, ServerboundPacket26_1.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        registryDataRewriter.remove("decorated_pot_pattern");

        registryDataRewriter.addHandler("trim_material", (key, tag) -> {
            final Tag assetName = tag.remove("palette_id");
            tag.put("asset_name", assetName);
        });
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection);
        connection.addItemHasher(this.getClass(), new ItemHasherBase(this, connection));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter26_3 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter26_3 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public BlockRewriter<ClientboundPacket26_1> getBlockRewriter() {
        return blockRewriter;
    }

    @Override
    public RecipeDisplayRewriter<ClientboundPacket26_1> getRecipeRewriter() {
        return recipeRewriter;
    }

    @Override
    public BackwardsRegistryRewriter getRegistryDataRewriter() {
        return registryDataRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket26_1> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public NBTComponentRewriter<ClientboundPacket26_1> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket26_1> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public VersionedTypesHolder types() {
        return VersionedTypes.V26_3;
    }

    @Override
    public VersionedTypesHolder mappedTypes() {
        return VersionedTypes.V26_2;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket26_1, ClientboundPacket26_1, ServerboundPacket26_1, ServerboundPacket26_1> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets26_1.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets26_1.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets26_1.class, ServerboundConfigurationPackets1_21_9.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets26_1.class, ServerboundConfigurationPackets1_21_9.class)
        );
    }
}
