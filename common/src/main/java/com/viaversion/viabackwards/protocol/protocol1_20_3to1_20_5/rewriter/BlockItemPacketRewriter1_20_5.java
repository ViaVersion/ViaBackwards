/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.rewriter.RecipeRewriter1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.rewriter.StructuredDataConverter;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockItemPacketRewriter1_20_5 extends BackwardsStructuredItemRewriter<ClientboundPacket1_20_5, ServerboundPacket1_20_3, Protocol1_20_3To1_20_5> {

    private static final StructuredDataConverter DATA_CONVERTER = new StructuredDataConverter(true);
    private final Protocol1_20_5To1_20_3 vvProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_20_5To1_20_3.class);

    public BlockItemPacketRewriter1_20_5(final Protocol1_20_3To1_20_5 protocol) {
        super(protocol, Types1_20_5.ITEM, Types1_20_5.ITEM_ARRAY, Type.ITEM1_20_2, Type.ITEM1_20_2_ARRAY);
        enchantmentRewriter.setRewriteIds(false); // Let VV handle it
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_20_5> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockAction(ClientboundPackets1_20_5.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_20_5.BLOCK_CHANGE);
        blockRewriter.registerVarLongMultiBlockChange1_20(ClientboundPackets1_20_5.MULTI_BLOCK_CHANGE);
        blockRewriter.registerEffect(ClientboundPackets1_20_5.EFFECT, 1010, 2001);
        blockRewriter.registerChunkData1_19(ClientboundPackets1_20_5.CHUNK_DATA, ChunkType1_20_2::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_20_5.BLOCK_ENTITY_DATA);

        registerSetCooldown(ClientboundPackets1_20_5.COOLDOWN);
        registerWindowItems1_17_1(ClientboundPackets1_20_5.WINDOW_ITEMS);
        registerSetSlot1_17_1(ClientboundPackets1_20_5.SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_20_5.ADVANCEMENTS);
        registerClickWindow1_17_1(ServerboundPackets1_20_3.CLICK_WINDOW);
        registerCreativeInvAction(ServerboundPackets1_20_3.CREATIVE_INVENTORY_ACTION);
        registerWindowPropertyEnchantmentHandler(ClientboundPackets1_20_5.WINDOW_PROPERTY);

        protocol.registerClientbound(ClientboundPackets1_20_5.SPAWN_PARTICLE, wrapper -> {
            wrapper.write(Type.VAR_INT, 0); // Write dummy value, set later

            wrapper.passthrough(Type.BOOLEAN); // Long Distance
            wrapper.passthrough(Type.DOUBLE); // X
            wrapper.passthrough(Type.DOUBLE); // Y
            wrapper.passthrough(Type.DOUBLE); // Z
            wrapper.passthrough(Type.FLOAT); // Offset X
            wrapper.passthrough(Type.FLOAT); // Offset Y
            wrapper.passthrough(Type.FLOAT); // Offset Z
            wrapper.passthrough(Type.FLOAT); // Particle Data
            wrapper.passthrough(Type.INT); // Particle Count

            // Move it to the beginning, move out arguments here
            final Particle particle = wrapper.read(Types1_20_5.PARTICLE);
            rewriteParticle(particle);
            wrapper.set(Type.VAR_INT, 0, particle.id());
            for (final Particle.ParticleData<?> argument : particle.getArguments()) {
                argument.write(wrapper);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.EXPLOSION, wrapper -> {
            wrapper.passthrough(Type.DOUBLE); // X
            wrapper.passthrough(Type.DOUBLE); // Y
            wrapper.passthrough(Type.DOUBLE); // Z
            wrapper.passthrough(Type.FLOAT); // Power
            final int blocks = wrapper.passthrough(Type.VAR_INT);
            for (int i = 0; i < blocks; i++) {
                wrapper.passthrough(Type.BYTE); // Relative X
                wrapper.passthrough(Type.BYTE); // Relative Y
                wrapper.passthrough(Type.BYTE); // Relative Z
            }
            wrapper.passthrough(Type.FLOAT); // Knockback X
            wrapper.passthrough(Type.FLOAT); // Knockback Y
            wrapper.passthrough(Type.FLOAT); // Knockback Z
            wrapper.passthrough(Type.VAR_INT); // Block interaction type

            protocol.getEntityRewriter().rewriteParticle(wrapper, Types1_20_5.PARTICLE, Types1_20_3.PARTICLE); // Small explosion particle
            protocol.getEntityRewriter().rewriteParticle(wrapper, Types1_20_5.PARTICLE, Types1_20_3.PARTICLE); // Large explosion particle

            int soundId = wrapper.read(Type.VAR_INT) - 1;
            if (soundId == -1) {
                // Already followed by the resource location
                return;
            }

            soundId = protocol.getMappingData().getSoundMappings().getNewId(soundId);
            final String soundKey = protocol.getMappingData().mappedSoundName(soundId);
            wrapper.write(Type.STRING, soundKey != null ? soundKey : "minecraft:entity.generic.explode");
            wrapper.write(Type.OPTIONAL_FLOAT, null); // Fixed range
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.TRADE_LIST, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Container id
            final int size = wrapper.passthrough(Type.VAR_INT);
            for (int i = 0; i < size; i++) {
                final Item input = handleItemToClient(wrapper.read(Types1_20_5.ITEM_COST));
                wrapper.write(Type.ITEM1_20_2, input);
                final Item result = handleItemToClient(wrapper.read(Types1_20_5.ITEM));
                wrapper.write(Type.ITEM1_20_2, result);
                final Item secondInput = handleItemToClient(wrapper.read(Types1_20_5.OPTIONAL_ITEM_COST));
                wrapper.write(Type.ITEM1_20_2, secondInput);

                wrapper.passthrough(Type.BOOLEAN); // Out of stock
                wrapper.passthrough(Type.INT); // Number of trade uses
                wrapper.passthrough(Type.INT); // Maximum number of trade uses
                wrapper.passthrough(Type.INT); // XP
                wrapper.passthrough(Type.INT); // Special price
                wrapper.passthrough(Type.FLOAT); // Price multiplier
                wrapper.passthrough(Type.INT); // Demand
            }
        });

        final RecipeRewriter1_20_3<ClientboundPacket1_20_5> recipeRewriter = new RecipeRewriter1_20_3<>(protocol);
        protocol.registerClientbound(ClientboundPackets1_20_5.DECLARE_RECIPES, wrapper -> {
            final int size = wrapper.passthrough(Type.VAR_INT);
            for (int i = 0; i < size; i++) {
                // Change order and write the type as an int
                final String recipeIdentifier = wrapper.read(Type.STRING);
                final int serializerTypeId = wrapper.read(Type.VAR_INT);
                final String serializerType = protocol.getMappingData().getRecipeSerializerMappings().mappedIdentifier(serializerTypeId);
                wrapper.write(Type.STRING, serializerType);
                wrapper.write(Type.STRING, recipeIdentifier);
                recipeRewriter.handleRecipeType(wrapper, Key.stripMinecraftNamespace(serializerType));
            }
        });
    }

    @Override
    public @Nullable Item handleItemToClient(@Nullable final Item item) {
        if (item == null) return null;

        super.handleItemToClient(item);
        return vvProtocol.getItemRewriter().toOldItem(item, DATA_CONVERTER);
    }

    @Override
    public @Nullable Item handleItemToServer(@Nullable final Item item) {
        if (item == null) return null;

        // Convert to structured item first
        final Item structuredItem = vvProtocol.getItemRewriter().toStructuredItem(item);
        return super.handleItemToServer(structuredItem);
    }
}