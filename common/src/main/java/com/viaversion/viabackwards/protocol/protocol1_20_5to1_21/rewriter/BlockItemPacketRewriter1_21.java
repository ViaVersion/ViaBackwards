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
package com.viaversion.viabackwards.protocol.protocol1_20_5to1_21.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.api.rewriters.StructuredEnchantmentRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_5to1_21.Protocol1_20_5To1_21;
import com.viaversion.viabackwards.protocol.protocol1_20_5to1_21.storage.EnchantmentsPaintingsStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.api.type.types.version.Types1_21;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.rewriter.RecipeRewriter1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Enchantments1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.IdRewriteFunction;
import java.util.ArrayList;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockItemPacketRewriter1_21 extends BackwardsStructuredItemRewriter<ClientboundPacket1_20_5, ServerboundPacket1_20_5, Protocol1_20_5To1_21> {

    private final StructuredEnchantmentRewriter enchantmentRewriter = new StructuredEnchantmentRewriter(this);

    public BlockItemPacketRewriter1_21(final Protocol1_20_5To1_21 protocol) {
        super(protocol, Types1_21.ITEM, Types1_21.ITEM_ARRAY, Types1_20_5.ITEM, Types1_20_5.ITEM_ARRAY);
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
        registerEntityEquipmentArray(ClientboundPackets1_20_5.ENTITY_EQUIPMENT);
        registerClickWindow1_17_1(ServerboundPackets1_20_5.CLICK_WINDOW);
        registerTradeList1_20_5(ClientboundPackets1_20_5.TRADE_LIST, Types1_21.ITEM_COST, Types1_20_5.ITEM_COST, Types1_21.OPTIONAL_ITEM_COST, Types1_20_5.OPTIONAL_ITEM_COST);
        registerCreativeInvAction(ServerboundPackets1_20_5.CREATIVE_INVENTORY_ACTION);
        registerWindowPropertyEnchantmentHandler(ClientboundPackets1_20_5.WINDOW_PROPERTY);
        registerSpawnParticle1_20_5(ClientboundPackets1_20_5.SPAWN_PARTICLE, Types1_21.PARTICLE, Types1_20_5.PARTICLE);
        registerExplosion(ClientboundPackets1_20_5.EXPLOSION, Types1_21.PARTICLE, Types1_20_5.PARTICLE);

        new RecipeRewriter1_20_3<>(protocol).register1_20_5(ClientboundPackets1_20_5.DECLARE_RECIPES);
    }

    @Override
    public @Nullable Item handleItemToClient(final UserConnection connection, @Nullable final Item item) {
        if (item == null) return null;

        final StructuredDataContainer data = item.structuredData();
        data.setIdLookup(protocol, true);
        data.replaceKey(StructuredDataKey.FOOD1_21, StructuredDataKey.FOOD1_20_5);

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

        return super.handleItemToClient(connection, item);
    }

    @Override
    public @Nullable Item handleItemToServer(final UserConnection connection, @Nullable final Item item) {
        if (item == null) return null;

        final StructuredDataContainer dataContainer = item.structuredData();
        dataContainer.setIdLookup(protocol, false);

        dataContainer.replaceKey(StructuredDataKey.FOOD1_20_5, StructuredDataKey.FOOD1_21);

        // Rewrite enchantments
        final EnchantmentsPaintingsStorage storage = connection.get(EnchantmentsPaintingsStorage.class);
        rewriteEnchantmentToServer(storage, item, StructuredDataKey.ENCHANTMENTS);
        rewriteEnchantmentToServer(storage, item, StructuredDataKey.STORED_ENCHANTMENTS);

        // Restore originals if present
        enchantmentRewriter.handleToServer(item);

        return super.handleItemToServer(connection, item);
    }

    private void rewriteEnchantmentToServer(final EnchantmentsPaintingsStorage storage, final Item item, final StructuredDataKey<Enchantments> key) {
        final StructuredData<Enchantments> enchantmentsData = item.structuredData().getNonEmpty(key);
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