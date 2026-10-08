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
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.protocol.v26_3to26_2.Protocol26_3To26_2;
import com.viaversion.viaversion.util.Key;
import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class RegistryDataRewriter26_3 extends BackwardsRegistryRewriter {

    public RegistryDataRewriter26_3(final Protocol26_3To26_2 protocol) {
        super(protocol);
    }

    @Override
    public void updateEnchantmentTerm(final CompoundTag term) {
        // 26.3 uses "type" instead of "condition" - rename it back for 26.2
        final StringTag typeTag = term.removeUnchecked("type");
        if (typeTag != null) {
            term.put("condition", typeTag);
        }
        final String type = typeTag != null ? typeTag.getValue() : null;

        // In 26.3, the "terms" field of all_of/any_of can be an inline term (CompoundTag)
        // instead of always being a ListTag<CompoundTag>. Convert it to a list for 26.2.
        final Tag termsTag = term.get("terms");
        if (termsTag instanceof CompoundTag inlineTerm) {
            final ListTag<CompoundTag> termsList = new ListTag<>(CompoundTag.class);
            termsList.add(inlineTerm);
            term.put("terms", termsList);
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
        } else if (Key.equals(type, "int_value_check") || Key.equals(type, "float_value_check")) {
            term.putString("condition", "minecraft:value_check");
            if (!convert(term, "value", "value", RegistryDataRewriter26_3::toNumberProvider)
                || !convert(term, "test", "range", RegistryDataRewriter26_3::toIntRange)) {
                replaceWithAlwaysTrue(term);
            }
        } else if (Key.equals(type, "time_check")) {
            if (!convert(term, "value", "value", RegistryDataRewriter26_3::toIntRange)) {
                replaceWithAlwaysTrue(term);
            }
        } else if (Key.equals(type, "random_chance")) {
            if (!convert(term, "chance", "chance", RegistryDataRewriter26_3::toNumberProvider)) {
                replaceWithAlwaysTrue(term);
            }
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

        // Block tags and lists have no equivalent
        replaceWithAlwaysTrue(term);
    }

    private static void replaceWithAlwaysTrue(final CompoundTag term) {
        term.clear();
        term.putString("condition", "minecraft:all_of");
        term.put("terms", new ListTag<>(CompoundTag.class));
    }

    // Int and float providers have been merged into a single number provider type
    private static @Nullable Tag toNumberProvider(final Tag tag) {
        if (tag instanceof NumberTag) {
            return tag;
        }
        if (!(tag instanceof CompoundTag provider)) {
            return null; // References to provider registries
        }

        return switch (Key.stripMinecraftNamespace(provider.getString("type", ""))) {
            case "constant", "enchantment_level", "environment_attribute" -> provider;
            case "storage", "score" -> {
                provider.remove("fallback");
                yield provider;
            }
            case "uniform" -> convert(provider, "min", "min", RegistryDataRewriter26_3::toNumberProvider)
                && convert(provider, "max", "max", RegistryDataRewriter26_3::toNumberProvider) ? provider : null;
            case "binomial" -> convert(provider, "n", "n", RegistryDataRewriter26_3::toNumberProvider)
                && convert(provider, "p", "p", RegistryDataRewriter26_3::toNumberProvider) ? provider : null;
            case "add" -> {
                provider.putString("type", "minecraft:sum");
                yield convert(provider, "inputs", "summands", RegistryDataRewriter26_3::toNumberProviderList) ? provider : null;
            }
            // Old int conversion rounds instead of truncating
            case "from_int", "from_float", "round" -> {
                final Tag input = provider.get("input");
                yield input != null ? toNumberProvider(input) : null;
            }
            default -> null;
        };
    }

    private static @Nullable Tag toNumberProviderList(final Tag tag) {
        final ListTag<CompoundTag> list = new ListTag<>(CompoundTag.class);
        for (final Tag element : tag instanceof ListTag<?> listTag ? listTag : List.of(tag)) {
            final Tag provider = toNumberProvider(element);
            if (provider == null) {
                return null;
            }
            if (provider instanceof CompoundTag compoundTag) {
                list.add(compoundTag);
            } else {
                final CompoundTag constant = new CompoundTag();
                constant.putString("type", "minecraft:constant");
                constant.put("value", provider);
                list.add(constant);
            }
        }
        return list;
    }

    private static @Nullable Tag toIntRange(final Tag tag) {
        if (tag instanceof NumberTag numberTag) {
            return tag instanceof IntTag ? tag : new IntTag(Math.round(numberTag.asFloat()));
        }
        if (!(tag instanceof CompoundTag range)) {
            return null;
        }

        if (range.contains("type")) {
            // Single provider, convert to an exact range
            final Tag value = toNumberProvider(range);
            if (value == null) {
                return null;
            }

            final CompoundTag exactRange = new CompoundTag();
            exactRange.put("min", value);
            exactRange.put("max", value.copy());
            return exactRange;
        }
        return convert(range, "min", "min", RegistryDataRewriter26_3::toNumberProvider)
            && convert(range, "max", "max", RegistryDataRewriter26_3::toNumberProvider) ? range : null;
    }

    private static boolean convert(final CompoundTag tag, final String key, final String newKey, final Function<Tag, @Nullable Tag> converter) {
        final Tag value = tag.remove(key);
        if (value == null) {
            return true;
        }

        final Tag converted = converter.apply(value);
        if (converted == null) {
            return false;
        }
        tag.put(newKey, converted);
        return true;
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
