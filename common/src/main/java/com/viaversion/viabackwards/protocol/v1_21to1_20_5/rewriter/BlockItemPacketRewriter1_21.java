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
package com.viaversion.viabackwards.protocol.v1_21to1_20_5.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.api.rewriters.StructuredEnchantmentRewriter;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.Protocol1_21To1_20_5;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage.EnchantmentsPaintingsStorage;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage.PlayerRotationStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.api.type.types.version.Types1_21;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.rewriter.RecipeRewriter1_20_3;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.data.Enchantments1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPacket1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.IdRewriteFunction;
import java.util.ArrayList;
import java.util.Arrays;

import static com.viaversion.viaversion.protocols.v1_20_5to1_21.rewriter.BlockItemPacketRewriter1_21.downgradeItemData;
import static com.viaversion.viaversion.protocols.v1_20_5to1_21.rewriter.BlockItemPacketRewriter1_21.updateItemData;

public final class BlockItemPacketRewriter1_21 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21, ServerboundPacket1_20_5, Protocol1_21To1_20_5> {

    private final StructuredEnchantmentRewriter enchantmentRewriter = new StructuredEnchantmentRewriter(this);

    public BlockItemPacketRewriter1_21(final Protocol1_21To1_20_5 protocol) {
        super(protocol, Types1_21.ITEM, Types1_21.ITEM_ARRAY, Types1_20_5.ITEM, Types1_20_5.ITEM_ARRAY);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_21> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_21.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_21.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_21.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_21.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_20_2::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_21.BLOCK_ENTITY_DATA);

        registerCooldown(ClientboundPackets1_21.COOLDOWN);
        registerSetContent1_17_1(ClientboundPackets1_21.CONTAINER_SET_CONTENT);
        registerSetSlot1_17_1(ClientboundPackets1_21.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_21.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_21.SET_EQUIPMENT);
        registerContainerClick1_17_1(ServerboundPackets1_20_5.CONTAINER_CLICK);
        registerMerchantOffers1_20_5(ClientboundPackets1_21.MERCHANT_OFFERS, Types1_21.ITEM_COST, Types1_20_5.ITEM_COST, Types1_21.OPTIONAL_ITEM_COST, Types1_20_5.OPTIONAL_ITEM_COST);
        registerSetCreativeModeSlot(ServerboundPackets1_20_5.SET_CREATIVE_MODE_SLOT);
        registerContainerSetData(ClientboundPackets1_21.CONTAINER_SET_DATA);
        registerLevelParticles1_20_5(ClientboundPackets1_21.LEVEL_PARTICLES, Types1_21.PARTICLE, Types1_20_5.PARTICLE);
        registerExplosion(ClientboundPackets1_21.EXPLODE, Types1_21.PARTICLE, Types1_20_5.PARTICLE);

        protocol.registerClientbound(ClientboundPackets1_21.HORSE_SCREEN_OPEN, wrapper -> {
            wrapper.passthrough(Types.UNSIGNED_BYTE); // Container id

            // From columns to size
            final int columns = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.VAR_INT, columns * 3 + 1);
        });

        protocol.registerClientbound(ClientboundPackets1_21.LEVEL_EVENT, wrapper -> {
            final int event = wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BLOCK_POSITION1_14);

            final int data = wrapper.read(Types.INT);
            if (event == 1010) {
                final int itemId = wrapper.user().get(EnchantmentsPaintingsStorage.class).jubeboxSongToItem(data);
                if (itemId == -1) {
                    wrapper.cancel();
                    return;
                }

                wrapper.write(Types.INT, itemId);
            } else if (event == 2001) {
                wrapper.write(Types.INT, protocol.getMappingData().getNewBlockStateId(data));
            } else {
                wrapper.write(Types.INT, data);
            }
        });

        protocol.registerServerbound(ServerboundPackets1_20_5.USE_ITEM, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Hand
            wrapper.passthrough(Types.VAR_INT); // Sequence

            final PlayerRotationStorage rotation = wrapper.user().get(PlayerRotationStorage.class);
            wrapper.write(Types.FLOAT, rotation.yaw());
            wrapper.write(Types.FLOAT, rotation.pitch());
        });

        new RecipeRewriter1_20_3<>(protocol).register1_20_5(ClientboundPackets1_21.UPDATE_RECIPES);
    }

    @Override
    public Item handleItemToClient(final UserConnection connection, final Item item) {
        if (item.isEmpty()) {
            return item;
        }

        final StructuredDataContainer data = item.dataContainer();
        data.setIdLookup(protocol, true);

        // Enchantments
        final EnchantmentsPaintingsStorage storage = connection.get(EnchantmentsPaintingsStorage.class);
        final IdRewriteFunction idRewriteFunction = id -> {
            final String key = storage.enchantments().idToKey(id);
            return key != null ? Enchantments1_20_5.keyToId(key) : -1;
        };
        final StructuredEnchantmentRewriter.DescriptionSupplier descriptionSupplier = (id, level) -> {
            final Tag description = storage.enchantmentDescription(id);
            if (description == null) {
                return new StringTag("Unknown enchantment");
            }

            final CompoundTag fullDescription = new CompoundTag();
            fullDescription.putString("translate", "%s " + EnchantmentRewriter.getRomanNumber(level));
            fullDescription.put("with", new ListTag<>(Arrays.asList(description)));
            return fullDescription;
        };
        enchantmentRewriter.rewriteEnchantmentsToClient(data, StructuredDataKey.ENCHANTMENTS, idRewriteFunction, descriptionSupplier, false);
        enchantmentRewriter.rewriteEnchantmentsToClient(data, StructuredDataKey.STORED_ENCHANTMENTS, idRewriteFunction, descriptionSupplier, true);

        // Order is important
        super.handleItemToClient(connection, item);
        downgradeItemData(item);
        return item;
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, final Item item) {
        if (item.isEmpty()) {
            return item;
        }

        final StructuredDataContainer dataContainer = item.dataContainer();
        dataContainer.setIdLookup(protocol, false);

        // Rewrite enchantments
        final EnchantmentsPaintingsStorage storage = connection.get(EnchantmentsPaintingsStorage.class);
        rewriteEnchantmentToServer(storage, item, StructuredDataKey.ENCHANTMENTS);
        rewriteEnchantmentToServer(storage, item, StructuredDataKey.STORED_ENCHANTMENTS);

        // Restore originals if present
        enchantmentRewriter.handleToServer(item);

        // Order is important
        super.handleItemToServer(connection, item);
        updateItemData(item);
        return item;
    }

    private void rewriteEnchantmentToServer(final EnchantmentsPaintingsStorage storage, final Item item, final StructuredDataKey<Enchantments> key) {
        final StructuredData<Enchantments> enchantmentsData = item.dataContainer().getNonEmpty(key);
        if (enchantmentsData == null) {
            return;
        }

        final Enchantments enchantments = enchantmentsData.value();
        for (final Int2IntMap.Entry entry : new ArrayList<>(enchantments.enchantments().int2IntEntrySet())) {
            final int id = entry.getIntKey();
            final String enchantmentKey = Enchantments1_20_5.idToKey(id);
            if (enchantmentKey == null) {
                continue;
            }

            final int mappedId = storage.enchantments().keyToId(enchantmentKey);
            if (id != mappedId) {
                enchantments.enchantments().remove(id);
                enchantments.enchantments().put(mappedId, entry.getIntValue());
            }
        }
    }
}