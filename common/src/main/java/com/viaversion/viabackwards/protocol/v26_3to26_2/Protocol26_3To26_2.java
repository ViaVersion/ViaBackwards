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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter.BlockItemPacketRewriter26_3;
import com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter.BlockPacketRewriter26_3;
import com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter.ComponentRewriter26_3;
import com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter.EntityPacketRewriter26_3;
import com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter.RegistryDataRewriter26_3;
import com.viaversion.viabackwards.protocol.v26_3to26_2.storage.ProtocolStorables26_3;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.item.data.ChatType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.connection.ProtocolStorablesBase;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.rewriter.CommandRewriter1_19_4;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v26_2to26_3.Protocol26_2To26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundConfigurationPackets26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPacket26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ServerboundPacket26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ServerboundPackets26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.RecipeDisplayRewriter26_3;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

import java.util.BitSet;

import static com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.BlockItemPacketRewriter26_3.trimAssetName;
import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol26_3To26_2 extends BackwardsProtocol<ClientboundPacket26_3, ClientboundPacket26_1, ServerboundPacket26_3, ServerboundPacket26_1> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("26.3", "26.2", Protocol26_2To26_3.class);
    private final EntityPacketRewriter26_3 entityRewriter = new EntityPacketRewriter26_3(this);
    private final BlockItemPacketRewriter26_3 itemRewriter = new BlockItemPacketRewriter26_3(this);
    private final ParticleRewriter<ClientboundPacket26_3> particleRewriter = new ParticleRewriter<>(this);
    private final NBTComponentRewriter<ClientboundPacket26_3> translatableRewriter = new ComponentRewriter26_3(this);
    private final TagRewriter<ClientboundPacket26_3> tagRewriter = new TagRewriter<>(this);
    private final BlockRewriter<ClientboundPacket26_3> blockRewriter = new BlockPacketRewriter26_3(this);
    private final BackwardsRegistryRewriter registryDataRewriter = new RegistryDataRewriter26_3(this);
    private final RecipeDisplayRewriter<ClientboundPacket26_3> recipeRewriter = new RecipeDisplayRewriter26_3<>(this) {
        @Override
        protected void handleTag(final PacketWrapper wrapper) {
            final HolderSet items = wrapper.read(Types.HOLDER_SET);
            if (items.hasTagKey()) {
                wrapper.write(Types.STRING, items.tagKey());
            } else {
                wrapper.write(Types.STRING, "planks"); // dummy
            }
        }
    };

    public Protocol26_3To26_2() {
        super(ClientboundPacket26_3.class, ClientboundPacket26_1.class, ServerboundPacket26_3.class, ServerboundPacket26_1.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        tagRewriter.renameTag(RegistryType.BLOCK, "convertible_to_mud", "convertable_to_mud");

        registryDataRewriter.remove("decorated_pot_pattern");
        registryDataRewriter.remove("block_transformer");
        registryDataRewriter.remove("worldgen/block_state_provider");

        registryDataRewriter.addHandler("trim_material", (key, tag) -> {
            final StringTag paletteId = tag.removeUnchecked("palette_id");
            tag.putString("asset_name", trimAssetName(paletteId.getValue()));
        });
        registryDataRewriter.addHandler("dimension_type", (key, tag) -> handleEnvironmentAttributes(tag));
        registryDataRewriter.addHandler("worldgen/biome", (key, tag) -> handleEnvironmentAttributes(tag));

        final CommandRewriter1_19_4<ClientboundPacket26_3> commandRewriter = new CommandRewriter1_19_4<>(this) {
            @Override
            public void handleArgument(final PacketWrapper wrapper, final String argumentType) {
                if (argumentType.equals("minecraft:feature") || argumentType.equals("minecraft:slot_source")
                    || argumentType.equals("minecraft:swing_animation") || argumentType.equals("minecraft:context_float_provider")
                    || argumentType.equals("minecraft:context_int_provider")) {
                    wrapper.write(Types.VAR_INT, 1); // Quotable string
                } else {
                    super.handleArgument(wrapper, argumentType);
                }
            }
        };
        replaceClientbound(ClientboundPackets26_3.COMMANDS, commandRewriter::handle1_19);

        replaceClientbound(ClientboundPackets26_3.PLAYER_CHAT, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Index
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

            translatableRewriter.processTag(wrapper.user(), wrapper.passthrough(Types.TRUSTED_OPTIONAL_TAG)); // Unsigned content

            final int filterMaskType = wrapper.passthrough(Types.VAR_INT);
            if (filterMaskType == 2) { // Partially filtered
                final BitSet mask = wrapper.read(Types.BIT_SET);
                wrapper.write(Types.LONG_ARRAY_PRIMITIVE, mask.toLongArray());
            }

            wrapper.passthrough(ChatType.TYPE); // Chat Type
            translatableRewriter.processTag(wrapper.user(), wrapper.passthrough(Types.TRUSTED_TAG)); // Name
            translatableRewriter.processTag(wrapper.user(), wrapper.passthrough(Types.TRUSTED_OPTIONAL_TAG)); // Target Name
        });

        cancelClientbound(ClientboundPackets26_3.ADD_TRANSIENT_BLOCK);
        cancelClientbound(ClientboundPackets26_3.POST_EFFECTS);
        cancelClientbound(ClientboundConfigurationPackets26_3.POST_EFFECTS);
    }

    private void handleEnvironmentAttributes(final CompoundTag tag) {
        final CompoundTag attributes = tag.getCompoundTag("attributes");
        if (attributes == null) {
            return;
        }

        for (final Tag value : attributes.values()) {
            if (!(value instanceof CompoundTag compoundTag)) {
                continue;
            }

            final StringTag modifier = compoundTag.getStringTag("modifier");
            if (modifier != null && (modifier.getValue().equals("append") || modifier.getValue().equals("overlay"))) {
                modifier.setValue("override");
            }
        }
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection);
        addItemHasher(connection);
    }

    @Override
    public ProtocolStorablesBase createStorables() {
        return new ProtocolStorables26_3();
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
    public BlockRewriter<ClientboundPacket26_3> getBlockRewriter() {
        return blockRewriter;
    }

    @Override
    public RecipeDisplayRewriter<ClientboundPacket26_3> getRecipeRewriter() {
        return recipeRewriter;
    }

    @Override
    public BackwardsRegistryRewriter getRegistryDataRewriter() {
        return registryDataRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket26_3> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public NBTComponentRewriter<ClientboundPacket26_3> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket26_3> getTagRewriter() {
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
    protected PacketTypesProvider<ClientboundPacket26_3, ClientboundPacket26_1, ServerboundPacket26_3, ServerboundPacket26_1> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets26_3.class, ClientboundConfigurationPackets26_3.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets26_1.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets26_3.class, ServerboundConfigurationPackets1_21_9.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets26_1.class, ServerboundConfigurationPackets1_21_9.class)
        );
    }
}
