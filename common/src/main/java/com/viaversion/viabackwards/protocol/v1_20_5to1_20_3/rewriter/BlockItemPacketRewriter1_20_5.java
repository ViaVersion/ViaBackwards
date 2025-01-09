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
package com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.api.rewriters.StructuredEnchantmentRewriter;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.minecraft.item.data.FireworkExplosion;
import com.viaversion.viaversion.api.minecraft.item.data.Fireworks;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ServerboundPacket1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.rewriter.RecipeRewriter1_20_3;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.data.BannerPatterns1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.rewriter.StructuredDataConverter;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockItemPacketRewriter1_20_5 extends BackwardsStructuredItemRewriter<ClientboundPacket1_20_5, ServerboundPacket1_20_3, Protocol1_20_5To1_20_3> {

    private static final StructuredDataConverter DATA_CONVERTER = new StructuredDataConverter(true);
    private final Protocol1_20_3To1_20_5 vvProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_20_3To1_20_5.class);
    private final StructuredEnchantmentRewriter enchantmentRewriter = new StructuredEnchantmentRewriter(this);

    public BlockItemPacketRewriter1_20_5(final Protocol1_20_5To1_20_3 protocol) {
        super(protocol, Types1_20_5.ITEM, Types1_20_5.ITEM_ARRAY, Types.ITEM1_20_2, Types.ITEM1_20_2_ARRAY);
        enchantmentRewriter.setRewriteIds(false); // Let VV handle it
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_20_5> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_20_5.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_20_5.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_20_5.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent(ClientboundPackets1_20_5.LEVEL_EVENT, 1010, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_20_5.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_20_2::new, (user, blockEntity) -> updateBlockEntityTag(user, blockEntity.tag()));
        protocol.registerClientbound(ClientboundPackets1_20_5.BLOCK_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14); // Position
            wrapper.passthrough(Types.VAR_INT); // Block entity type
            final CompoundTag tag = wrapper.passthrough(Types.COMPOUND_TAG);
            updateBlockEntityTag(wrapper.user(), tag);
        });

        registerCooldown(ClientboundPackets1_20_5.COOLDOWN);
        registerSetContent1_17_1(ClientboundPackets1_20_5.CONTAINER_SET_CONTENT);
        registerSetSlot1_17_1(ClientboundPackets1_20_5.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_20_5.UPDATE_ADVANCEMENTS);
        registerContainerClick1_17_1(ServerboundPackets1_20_3.CONTAINER_CLICK);
        registerContainerSetData(ClientboundPackets1_20_5.CONTAINER_SET_DATA);
        registerSetCreativeModeSlot(ServerboundPackets1_20_3.SET_CREATIVE_MODE_SLOT);
        protocol.registerServerbound(ServerboundPackets1_20_3.CONTAINER_BUTTON_CLICK, wrapper -> {
            final int containerId = wrapper.read(Types.BYTE) & 0xFF;
            final int buttonId = wrapper.read(Types.BYTE) & 0xFF;
            wrapper.write(Types.VAR_INT, containerId);
            wrapper.write(Types.VAR_INT, buttonId);
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.LEVEL_PARTICLES, wrapper -> {
            wrapper.write(Types.VAR_INT, 0); // Write dummy value, set later

            wrapper.passthrough(Types.BOOLEAN); // Long Distance
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.FLOAT); // Offset X
            wrapper.passthrough(Types.FLOAT); // Offset Y
            wrapper.passthrough(Types.FLOAT); // Offset Z
            final float data = wrapper.passthrough(Types.FLOAT);
            wrapper.passthrough(Types.INT); // Particle Count

            // Move it to the beginning, move out arguments here
            final Particle particle = wrapper.read(Types1_20_5.PARTICLE);
            protocol.getParticleRewriter().rewriteParticle(wrapper.user(), particle);
            if (particle.id() == protocol.getMappingData().getParticleMappings().mappedId("entity_effect")) {
                // Remove color argument
                final int color = particle.<Integer>removeArgument(0).getValue();
                if (data == 0) {
                    wrapper.set(Types.FLOAT, 3, (float) color);
                }
            } else if (particle.id() == protocol.getMappingData().getParticleMappings().mappedId("dust_color_transition")) {
                // fromColor, toColor, scale -> fromColor, scale, toColor
                particle.add(3, Types.FLOAT, particle.<Float>removeArgument(6).getValue());
            }

            wrapper.set(Types.VAR_INT, 0, particle.id());
            for (final Particle.ParticleData<?> argument : particle.getArguments()) {
                argument.write(wrapper);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.EXPLODE, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.FLOAT); // Power
            final int blocks = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < blocks; i++) {
                wrapper.passthrough(Types.BYTE); // Relative X
                wrapper.passthrough(Types.BYTE); // Relative Y
                wrapper.passthrough(Types.BYTE); // Relative Z
            }
            wrapper.passthrough(Types.FLOAT); // Knockback X
            wrapper.passthrough(Types.FLOAT); // Knockback Y
            wrapper.passthrough(Types.FLOAT); // Knockback Z
            wrapper.passthrough(Types.VAR_INT); // Block interaction type

            final Particle smallExplosionParticle = wrapper.passthroughAndMap(Types1_20_5.PARTICLE, Types1_20_3.PARTICLE);
            final Particle largeExplosionParticle = wrapper.passthroughAndMap(Types1_20_5.PARTICLE, Types1_20_3.PARTICLE);
            protocol.getParticleRewriter().rewriteParticle(wrapper.user(), smallExplosionParticle);
            protocol.getParticleRewriter().rewriteParticle(wrapper.user(), largeExplosionParticle);

            final Holder<SoundEvent> soundEventHolder = wrapper.read(Types.SOUND_EVENT);
            if (soundEventHolder.isDirect()) {
                final SoundEvent soundEvent = soundEventHolder.value();
                wrapper.write(Types.STRING, soundEvent.identifier());
                wrapper.write(Types.OPTIONAL_FLOAT, soundEvent.fixedRange());
            } else {
                final int soundId = protocol.getMappingData().getSoundMappings().getNewId(soundEventHolder.id());
                final String soundKey = Protocol1_20_3To1_20_5.MAPPINGS.soundName(soundId);
                wrapper.write(Types.STRING, soundKey != null ? soundKey : "minecraft:entity.generic.explode");
                wrapper.write(Types.OPTIONAL_FLOAT, null); // Fixed range
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.MERCHANT_OFFERS, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Container id
            final int size = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < size; i++) {
                final Item input = handleItemToClient(wrapper.user(), wrapper.read(Types1_20_5.ITEM_COST));
                cleanInput(input);
                wrapper.write(Types.ITEM1_20_2, input);

                final Item result = handleItemToClient(wrapper.user(), wrapper.read(Types1_20_5.ITEM));
                wrapper.write(Types.ITEM1_20_2, result);

                Item secondInput = wrapper.read(Types1_20_5.OPTIONAL_ITEM_COST);
                if (secondInput != null) {
                    secondInput = handleItemToClient(wrapper.user(), secondInput);
                    cleanInput(secondInput);
                }
                wrapper.write(Types.ITEM1_20_2, secondInput);

                wrapper.passthrough(Types.BOOLEAN); // Out of stock
                wrapper.passthrough(Types.INT); // Number of trade uses
                wrapper.passthrough(Types.INT); // Maximum number of trade uses
                wrapper.passthrough(Types.INT); // XP
                wrapper.passthrough(Types.INT); // Special price
                wrapper.passthrough(Types.FLOAT); // Price multiplier
                wrapper.passthrough(Types.INT); // Demand
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.MAP_ITEM_DATA, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Map id
            wrapper.passthrough(Types.BYTE); // Scale
            wrapper.passthrough(Types.BOOLEAN); // Locked
            if (wrapper.passthrough(Types.BOOLEAN)) {
                final int icons = wrapper.passthrough(Types.VAR_INT);
                for (int i = 0; i < icons; i++) {
                    final int decorationType = wrapper.read(Types.VAR_INT);
                    wrapper.write(Types.VAR_INT, decorationType == 34 ? 32 : decorationType); // Trial champer to jungle temple
                    wrapper.passthrough(Types.BYTE); // X
                    wrapper.passthrough(Types.BYTE); // Y
                    wrapper.passthrough(Types.BYTE); // Rotation
                    wrapper.passthrough(Types.OPTIONAL_TAG); // Display name
                }
            }
        });

        final RecipeRewriter1_20_3<ClientboundPacket1_20_5> recipeRewriter = new RecipeRewriter1_20_3<>(protocol);
        protocol.registerClientbound(ClientboundPackets1_20_5.UPDATE_RECIPES, wrapper -> {
            final int size = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < size; i++) {
                // Change order and write the type as an int
                final String recipeIdentifier = wrapper.read(Types.STRING);
                final int serializerTypeId = wrapper.read(Types.VAR_INT);
                final String serializerType = protocol.getMappingData().getRecipeSerializerMappings().mappedIdentifier(serializerTypeId);
                wrapper.write(Types.STRING, serializerType);
                wrapper.write(Types.STRING, recipeIdentifier);
                recipeRewriter.handleRecipeType(wrapper, Key.stripMinecraftNamespace(serializerType));
            }
        });
    }

    private void updateBlockEntityTag(final UserConnection connection, final CompoundTag tag) {
        if (tag == null) {
            return;
        }

        final Tag profileTag = tag.remove("profile");
        if (profileTag instanceof StringTag) {
            tag.put("SkullOwner", profileTag);
        } else if (profileTag instanceof CompoundTag) {
            updateProfileTag(tag, (CompoundTag) profileTag);
        }

        final ListTag<CompoundTag> patternsTag = tag.getListTag("patterns", CompoundTag.class);
        if (patternsTag != null) {
            for (final CompoundTag patternTag : patternsTag) {
                final String pattern = patternTag.getString("pattern", "");
                final String color = patternTag.getString("color");
                final String compactIdentifier = BannerPatterns1_20_5.fullIdToCompact(Key.stripMinecraftNamespace(pattern));
                if (compactIdentifier == null || color == null) {
                    continue;
                }

                patternTag.remove("pattern");
                patternTag.remove("color");
                patternTag.putString("Pattern", compactIdentifier);
                patternTag.putInt("Color", colorId(color));
            }

            tag.remove("patterns");
            tag.put("Patterns", patternsTag);
        }
    }

    private void updateProfileTag(final CompoundTag tag, final CompoundTag profileTag) {
        final CompoundTag skullOwnerTag = new CompoundTag();
        tag.put("SkullOwner", skullOwnerTag);

        final String name = profileTag.getString("name");
        if (name != null) {
            skullOwnerTag.putString("Name", name);
        }

        final IntArrayTag idTag = profileTag.getIntArrayTag("id");
        if (idTag != null) {
            skullOwnerTag.put("Id", idTag);
        }

        final ListTag<CompoundTag> propertiesListTag = profileTag.getListTag("properties", CompoundTag.class);
        if (propertiesListTag == null) {
            return;
        }

        final CompoundTag propertiesTag = new CompoundTag();
        for (final CompoundTag propertyTag : propertiesListTag) {
            final String property = propertyTag.getString("name", "");
            final String value = propertyTag.getString("value", "");
            final String signature = propertyTag.getString("signature");

            final ListTag<CompoundTag> list = new ListTag<>(CompoundTag.class);
            final CompoundTag updatedPropertyTag = new CompoundTag();
            updatedPropertyTag.putString("Value", value);
            if (signature != null) {
                updatedPropertyTag.putString("Signature", signature);
            }
            list.add(updatedPropertyTag);
            propertiesTag.put(property, list);
        }
        skullOwnerTag.put("Properties", propertiesTag);
    }

    private void cleanInput(@Nullable final Item item) {
        // Try to maybe hopefully get the tag matching to what the client will try to input by removing default data
        if (item == null || item.tag() == null) {
            return;
        }

        final CompoundTag tag = item.tag();
        StructuredDataConverter.removeBackupTag(tag);

        final CompoundTag display = tag.getCompoundTag("display");
        if (display != null) {
            removeEmptyList(display, "Lore");
            if (display.isEmpty()) {
                tag.remove("display");
            }
        }

        removeEmptyList(tag, "Enchantments");
        removeEmptyList(tag, "AttributeModifiers");

        if (tag.getInt("RepairCost", -1) == 0) {
            tag.remove("RepairCost");
        }

        if (tag.isEmpty()) {
            item.setTag(null);
        }
    }

    private void removeEmptyList(final CompoundTag tag, final String key) {
        final ListTag<?> list = tag.getListTag(key);
        if (list != null && list.isEmpty()) {
            tag.remove(key);
        }
    }

    private static int colorId(final String color) {
        return switch (color) {
            case "orange" -> 1;
            case "magenta" -> 2;
            case "light_blue" -> 3;
            case "yellow" -> 4;
            case "lime" -> 5;
            case "pink" -> 6;
            case "gray" -> 7;
            case "light_gray" -> 8;
            case "cyan" -> 9;
            case "purple" -> 10;
            case "blue" -> 11;
            case "brown" -> 12;
            case "green" -> 13;
            case "red" -> 14;
            case "black" -> 15;
            default -> 0;
        };
    }

    @Override
    public @Nullable Item handleItemToClient(final UserConnection connection, final Item item) {
        if (item.isEmpty()) {
            // Back to null for the older protocols
            return null;
        }

        final StructuredDataContainer data = item.dataContainer();
        item.dataContainer().setIdLookup(protocol, true);
        enchantmentRewriter.handleToClient(item);

        super.handleItemToClient(connection, item);

        // Text components since we skip the usual rewrite method
        updateComponent(connection, item, StructuredDataKey.ITEM_NAME, "item_name");
        updateComponent(connection, item, StructuredDataKey.CUSTOM_NAME, "custom_name");
        final Tag[] lore = data.get(StructuredDataKey.LORE);
        if (lore != null) {
            for (final Tag tag : lore) {
                protocol.getComponentRewriter().processTag(connection, tag);
            }
        }

        // In 1.20.6, some items have default values which are not written into the components
        if (item.identifier() == 1105 && !data.has(StructuredDataKey.FIREWORKS)) {
            data.set(StructuredDataKey.FIREWORKS, new Fireworks(1, new FireworkExplosion[0]));
        }

        final CompoundTag customData = data.get(StructuredDataKey.CUSTOM_DATA);
        final Item oldItem = vvProtocol.getItemRewriter().toOldItem(connection, item, DATA_CONVERTER);

        if (customData != null) {
            // We later don't know which tags are custom data and which are not because the VV conversion
            // keeps converted data, so we backup the original custom data and restore it later
            if (oldItem.tag() == null) {
                oldItem.setTag(new CompoundTag());
            }
            oldItem.tag().put(nbtTagName(), customData.copy());
        } else if (oldItem.tag() != null && oldItem.tag().isEmpty()) {
            // Improve item equality checks by removing empty tags
            oldItem.setTag(null);
        }
        return oldItem;
    }

    @Override
    protected void updateItemDataComponents(final UserConnection connection, final Item item, final boolean clientbound) {
        // Items and data within components are handled in this protocol
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, @Nullable final Item item) {
        if (item == null) {
            // Unify as empty going forward
            return StructuredItem.empty();
        }

        // Convert to structured item first
        final Item structuredItem = vvProtocol.getItemRewriter().toStructuredItem(connection, item);

        if (item.tag() != null && item.tag().get(nbtTagName()) instanceof final CompoundTag tag) {
            // Set original custom data from backup
            structuredItem.dataContainer().set(StructuredDataKey.CUSTOM_DATA, tag);
        }

        structuredItem.dataContainer().setIdLookup(protocol, false);
        enchantmentRewriter.handleToServer(structuredItem);

        return super.handleItemToServer(connection, structuredItem);
    }
}
