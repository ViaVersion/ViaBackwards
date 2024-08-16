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

import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_21;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPacket1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.rewriter.RecipeRewriter1_21_2;
import com.viaversion.viaversion.rewriter.BlockRewriter;

// To replace if needed:
//   ChunkType1_20_2
//   RecipeRewriter1_20_3
//   Types1_21
final class BlockItemPacketRewriter1_99 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21, ServerboundPacket1_20_5, Protocol1_98To1_99> {

    public BlockItemPacketRewriter1_99(final Protocol1_98To1_99 protocol) {
        super(protocol, Types1_21.ITEM, Types1_21.ITEM_ARRAY);
        /*super(protocol,
            Types1_21.ITEM, Types1_21.ITEM_ARRAY, Types1_OLD.ITEM, Types1_OLD.ITEM_ARRAY,
            Types1_21.ITEM_COST, Types1_21.OPTIONAL_ITEM_COST, Types1_OLD.ITEM_COST, Types1_OLD.OPTIONAL_ITEM_COST,
            Types1_21.PARTICLE, Types1_OLD.PARTICLE
        );*/
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_21> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_21.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_21.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_21.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent1_21(ClientboundPackets1_21.LEVEL_EVENT, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_21.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_20_2::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_21.BLOCK_ENTITY_DATA);

        // registerOpenScreen(ClientboundPackets1_21.OPEN_SCREEN);
        registerCooldown(ClientboundPackets1_21.COOLDOWN);
        registerSetContent1_21_2(ClientboundPackets1_21.CONTAINER_SET_CONTENT);
        registerSetSlot1_21_2(ClientboundPackets1_21.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_21.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_21.SET_EQUIPMENT);
        registerContainerClick1_21_2(ServerboundPackets1_20_5.CONTAINER_CLICK);
        registerMerchantOffers1_20_5(ClientboundPackets1_21.MERCHANT_OFFERS);
        registerSetCreativeModeSlot(ServerboundPackets1_20_5.SET_CREATIVE_MODE_SLOT);
        registerContainerSetData(ClientboundPackets1_21.CONTAINER_SET_DATA);
        registerLevelParticles1_20_5(ClientboundPackets1_21.LEVEL_PARTICLES);
        registerExplosion1_21_2(ClientboundPackets1_21.EXPLODE);

        new RecipeRewriter1_21_2<>(protocol).register1_20_5(ClientboundPackets1_21.UPDATE_RECIPES);
    }
}
