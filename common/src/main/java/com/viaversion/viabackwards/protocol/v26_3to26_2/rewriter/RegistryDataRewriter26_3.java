/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
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
package com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.protocol.v26_3to26_2.Protocol26_3To26_2;
import com.viaversion.viaversion.util.Key;

public final class RegistryDataRewriter26_3 extends BackwardsRegistryRewriter {

    public RegistryDataRewriter26_3(final Protocol26_3To26_2 protocol) {
        super(protocol);
    }

    @Override
    public void updateEnchantmentTerm(final CompoundTag term) {
        final String type = term.getString("type");
        if (type != null) {
            term.putString("condition", type);
        }

        if (Key.equals(type, "damage_source_properties")) {
            final CompoundTag predicate = term.getCompoundTag("predicate");
            if (predicate != null) {
                final ListTag<CompoundTag> tags = predicate.getListTag("tags", CompoundTag.class);
                if (tags != null) {
                    updateTagKey(tags);
                }
            }
        } else if (Key.equals(type, "match_block")) {
            // Has to run before super, which would otherwise replace the term by a dummy effect
            updateMatchBlock(term);
        }

        super.updateEnchantmentTerm(term);
    }

    @Override
    public boolean updateBlockStateProvider(final CompoundTag tag) {
        String type = tag.getString("type");
        if (type == null) {
            // Move block state to a block provider
            final CompoundTag stateTag = tag.copy();
            handleFullBlockState(stateTag);
            tag.put("state", stateTag);
            tag.putString("type", "simple_state_provider");
            return true;
        }

        type = Key.stripMinecraftNamespace(type);
        switch (type) {
            case "simple_state_provider", "rotated_block_provider" -> {
                handleBlockState(tag, "state");
            }
            case "weighted_state_provider" -> {
                for (final CompoundTag entry : tag.getListTag("entries", CompoundTag.class)) {
                    handleBlockState(entry, "data");
                }
            }
            case "noise_threshold_provider" -> {
                handleBlockState(tag, "default_state");
            }
        }
        return super.updateBlockStateProvider(tag);
    }

    @Override
    protected void handleParticleData(final CompoundTag particleData) {
        super.handleParticleData(particleData);
        if (particleData.get("block_state") instanceof CompoundTag blockState) {
            handleFullBlockState(blockState);
        }
    }

    private void handleBlockState(final CompoundTag parent, final String key) {
        final Tag blockStateTag = parent.get(key);
        if (blockStateTag instanceof CompoundTag compoundTag) {
            handleFullBlockState(compoundTag);
        } else if (blockStateTag instanceof StringTag stringTag) {
            // Needs to be a compound tag in older versions
            final CompoundTag updatedBlockStateTag = new CompoundTag();
            updatedBlockStateTag.putString("Name", stringTag.getValue());
            parent.put(key, updatedBlockStateTag);
        }
    }

    private void handleFullBlockState(final CompoundTag tag) {
        final Tag id = tag.remove("id");
        tag.put("Name", id);

        final Tag properties = tag.remove("properties");
        if (properties != null) {
            tag.put("Properties", properties);
        }
    }

    // match_block replaced block_state_property, which can only hold a single block id
    private void updateMatchBlock(final CompoundTag term) {
        final Tag blocks = term.remove("blocks");
        if (blocks instanceof final StringTag block && !block.getValue().startsWith("#")) {
            term.putString("condition", "minecraft:block_state_property");
            term.put("block", block);

            final Tag state = term.remove("state");
            if (state != null) {
                term.put("properties", state);
            }
            return;
        }

        // Block tags and lists have no equivalent, replace the term by an always true condition
        term.clear();
        term.putString("condition", "minecraft:all_of");
        term.put("terms", new ListTag<>(CompoundTag.class));
    }

    private void updateTagKey(final ListTag<CompoundTag> tags) {
        for (final CompoundTag tag : tags) {
            final Tag idTag = tag.get("id");
            if (idTag instanceof StringTag tagKey) {
                tagKey.setValue(tagKey.getValue().substring(1));
            } else {
                tag.putString("id", "wool"); // dummy value
            }
        }
    }
}
