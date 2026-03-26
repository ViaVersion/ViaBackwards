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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter.BlockItemPacketRewriter26_1;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter.ComponentRewriter26_1;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter.EntityPacketRewriter26_1;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.storage.DayTimeStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_11;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_21_5;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType26_1;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.data.item.ItemHasherBase;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.Protocol1_21_11To26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.RecipeDisplayRewriter1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPacket1_21_11;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPackets1_21_11;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.block.BlockRewriter1_21_5;
import com.viaversion.viaversion.util.Key;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;
import static com.viaversion.viaversion.util.TagUtil.removeNamespaced;

public final class Protocol26_1To1_21_11 extends BackwardsProtocol<ClientboundPacket26_1, ClientboundPacket1_21_11, ServerboundPacket26_1, ServerboundPacket1_21_9> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("26.1", "1.21.11", Protocol1_21_11To26_1.class);
    private final EntityPacketRewriter26_1 entityRewriter = new EntityPacketRewriter26_1(this);
    private final BlockItemPacketRewriter26_1 itemRewriter = new BlockItemPacketRewriter26_1(this);
    private final ParticleRewriter<ClientboundPacket26_1> particleRewriter = new ParticleRewriter<>(this);
    private final NBTComponentRewriter<ClientboundPacket26_1> translatableRewriter = new ComponentRewriter26_1(this);
    private final TagRewriter<ClientboundPacket26_1> tagRewriter = new TagRewriter<>(this);
    private final BackwardsRegistryRewriter registryDataRewriter = new BackwardsRegistryRewriter(this);
    private final BlockRewriter<ClientboundPacket26_1> blockRewriter = new BlockRewriter1_21_5<>(this, ChunkType26_1::new, ChunkType1_21_5::new);
    private final RecipeDisplayRewriter<ClientboundPacket26_1> recipeRewriter = new RecipeDisplayRewriter1_21_5<>(this) {
        @Override
        protected void handleDyeSlotDisplay(final PacketWrapper wrapper) {
            wrapper.consumeReadsOnly(() -> super.handleDyeSlotDisplay(wrapper));
        }

        @Override
        protected void handleOnlyWithComponentSlotDisplay(final PacketWrapper wrapper) {
            wrapper.consumeReadsOnly(() -> super.handleOnlyWithComponentSlotDisplay(wrapper));
        }

        @Override
        protected void handleWithRemainderSlotDisplay(final PacketWrapper wrapper) {
            wrapper.consumeReadsOnly(() -> super.handleWithRemainderSlotDisplay(wrapper));
        }
    };

    public Protocol26_1To1_21_11() {
        super(ClientboundPacket26_1.class, ClientboundPacket1_21_11.class, ServerboundPacket26_1.class, ServerboundPacket1_21_9.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        // Remove new environment attributes
        registryDataRewriter.addHandler("dimension_type", (key, tag) -> {
            final CompoundTag attributes = tag.getCompoundTag("attributes");
            removeNamespaced(attributes, "visual/block_light_tint");
            removeNamespaced(attributes, "visual/night_vision_color");
            removeNamespaced(attributes, "visual/ambient_light_color");
        });

        // Move around entity variant names and sounds
        registryDataRewriter.addHandler("wolf_sound_variant", (key, tag) -> {
            final CompoundTag sounds = tag.getCompoundTag("adult_sounds");
            tag.remove("baby_sounds");
            tag.putAll(sounds);
        });
        registryDataRewriter.addHandler("frog_variant", (key, tag) -> swapEntityNameAffix("frog", tag));
        registryDataRewriter.addHandler("chicken_variant", (key, tag) -> swapEntityNameAffix("chicken", tag));
        registryDataRewriter.addHandler("cow_variant", (key, tag) -> swapEntityNameAffix("cow", tag));
        registryDataRewriter.addHandler("pig_variant", (key, tag) -> swapEntityNameAffix("pig", tag));
        registryDataRewriter.addHandler("cat_variant", (key, tag) -> removeEntityNamePrefix("cat", tag));
        registryDataRewriter.remove("world_clock");
        registryDataRewriter.remove("cat_sound_variant");
        registryDataRewriter.remove("cow_sound_variant");
        registryDataRewriter.remove("pig_sound_variant");
        registryDataRewriter.remove("chicken_sound_variant");

        tagRewriter.addEmptyTags(RegistryType.BLOCK, "big_dripleaf_placeable", "small_dripleaf_placeable", "mushroom_grow_block", "bamboo_plantable_on");

        cancelClientbound(ClientboundPackets26_1.LOW_DISK_SPACE_WARNING);
        cancelClientbound(ClientboundPackets26_1.GAME_RULE_VALUES);

        registerClientbound(ClientboundPackets26_1.SET_TIME, wrapper -> {
            final long gameTime = wrapper.passthrough(Types.LONG);

            Long dayTime = null;
            boolean advanceTime = true;

            final int count = wrapper.read(Types.VAR_INT);
            for (int i = 0; i < count; i++) {
                final int clockType = wrapper.read(Types.VAR_INT);
                final long totalTicks = wrapper.read(Types.VAR_LONG);
                wrapper.read(Types.FLOAT); // Partial tick
                final float tickRate = wrapper.read(Types.FLOAT);
                if (Key.equals(registryDataRewriter.getMappings("world_clock").idToKey(clockType), "overworld")) {
                    dayTime = totalTicks;
                    advanceTime = tickRate != 0;
                }
            }

            final DayTimeStorage dayTimeStorage = wrapper.user().get(DayTimeStorage.class);
            if (dayTime == null) {
                // Determine from previously sent values based on the current game time
                dayTime = dayTimeStorage.setGameTimeAndUpdateDayTime(gameTime);
                advanceTime = dayTimeStorage.advanceTime();
            } else {
                dayTimeStorage.setGameTime(gameTime);
                dayTimeStorage.setDayTime(dayTime);
                dayTimeStorage.setAdvanceTime(advanceTime);
            }

            wrapper.write(Types.LONG, dayTime);
            wrapper.write(Types.BOOLEAN, advanceTime);
        });
    }

    private void removeEntityNamePrefix(final String key, final CompoundTag tag) {
        final StringTag assetIdTag = tag.getStringTag("asset_id");
        final String assetId = assetIdTag.getValue();
        assetIdTag.setValue(assetId.replace(key + "_", ""));
    }

    private void swapEntityNameAffix(final String key, final CompoundTag tag) {
        final StringTag assetIdTag = tag.getStringTag("asset_id");
        final String assetId = assetIdTag.getValue();
        if (assetId.contains(key + "_")) {
            assetIdTag.setValue(assetId.replace(key + "_", "") + "_" + key);
        }
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection, new EntityTrackerBase(connection, EntityTypes1_21_11.PLAYER));
        addItemHasher(connection, new ItemHasherBase(this, connection));
        connection.put(new DayTimeStorage());
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
    public BlockRewriter<ClientboundPacket26_1> getBlockRewriter() {
        return blockRewriter;
    }

    @Override
    public BackwardsRegistryRewriter getRegistryDataRewriter() {
        return registryDataRewriter;
    }

    @Override
    public RecipeDisplayRewriter<ClientboundPacket26_1> getRecipeRewriter() {
        return recipeRewriter;
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
        return VersionedTypes.V26_1;
    }

    @Override
    public VersionedTypesHolder mappedTypes() {
        return VersionedTypes.V1_21_11;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket26_1, ClientboundPacket1_21_11, ServerboundPacket26_1, ServerboundPacket1_21_9> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets26_1.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_11.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets26_1.class, ServerboundConfigurationPackets1_21_9.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_6.class, ServerboundConfigurationPackets1_21_9.class)
        );
    }
}
