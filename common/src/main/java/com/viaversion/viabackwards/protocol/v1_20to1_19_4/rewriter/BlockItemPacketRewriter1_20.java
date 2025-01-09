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
package com.viaversion.viabackwards.protocol.v1_20to1_19_4.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.v1_20to1_19_4.Protocol1_20To1_19_4;
import com.viaversion.viabackwards.protocol.v1_20to1_19_4.storage.BackSignEditStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ServerboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.rewriter.RecipeRewriter1_19_4;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.Key;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockItemPacketRewriter1_20 extends BackwardsItemRewriter<ClientboundPackets1_19_4, ServerboundPackets1_19_4, Protocol1_20To1_19_4> {

    private static final Set<String> NEW_TRIM_PATTERNS = new HashSet<>(Arrays.asList("host", "raiser", "shaper", "silence", "wayfinder"));

    public BlockItemPacketRewriter1_20(final Protocol1_20To1_19_4 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_ARRAY);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPackets1_19_4> blockRewriter = BlockRewriter.for1_14(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_19_4.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_19_4.BLOCK_UPDATE);
        blockRewriter.registerLevelEvent(ClientboundPackets1_19_4.LEVEL_EVENT, 1010, 2001);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_19_4.BLOCK_ENTITY_DATA, this::handleBlockEntity);

        protocol.registerClientbound(ClientboundPackets1_19_4.LEVEL_CHUNK_WITH_LIGHT, new PacketHandlers() {
            @Override
            protected void register() {
                handler(blockRewriter.chunkHandler1_19(ChunkType1_18::new, (user, blockEntity) -> handleBlockEntity(blockEntity)));
                create(Types.BOOLEAN, true); // Trust edges
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.LIGHT_UPDATE, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // X
            wrapper.passthrough(Types.VAR_INT); // Y
            wrapper.write(Types.BOOLEAN, true); // Trust edges
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.SECTION_BLOCKS_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.LONG); // Chunk position
                create(Types.BOOLEAN, false); // Suppress light updates
                handler(wrapper -> {
                    for (final BlockChangeRecord record : wrapper.passthrough(Types.VAR_LONG_BLOCK_CHANGE_ARRAY)) {
                        record.setBlockId(protocol.getMappingData().getNewBlockStateId(record.getBlockId()));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.OPEN_SCREEN, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Container id

            handleMenuType(wrapper);
            protocol.getComponentRewriter().passthroughAndProcess(wrapper);
        });

        registerCooldown(ClientboundPackets1_19_4.COOLDOWN);
        registerSetContent1_17_1(ClientboundPackets1_19_4.CONTAINER_SET_CONTENT);
        registerSetSlot1_17_1(ClientboundPackets1_19_4.CONTAINER_SET_SLOT);
        registerSetEquipment(ClientboundPackets1_19_4.SET_EQUIPMENT);
        registerContainerClick1_17_1(ServerboundPackets1_19_4.CONTAINER_CLICK);
        registerMerchantOffers1_19(ClientboundPackets1_19_4.MERCHANT_OFFERS);
        registerSetCreativeModeSlot(ServerboundPackets1_19_4.SET_CREATIVE_MODE_SLOT);
        registerContainerSetData(ClientboundPackets1_19_4.CONTAINER_SET_DATA);

        protocol.registerClientbound(ClientboundPackets1_19_4.UPDATE_ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Types.BOOLEAN); // Reset/clear
            int size = wrapper.passthrough(Types.VAR_INT); // Mapping size
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Types.STRING); // Identifier
                wrapper.passthrough(Types.OPTIONAL_STRING); // Parent

                // Display data
                if (wrapper.passthrough(Types.BOOLEAN)) {
                    wrapper.passthrough(Types.COMPONENT); // Title
                    wrapper.passthrough(Types.COMPONENT); // Description
                    handleItemToClient(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)); // Icon
                    wrapper.passthrough(Types.VAR_INT); // Frame type
                    int flags = wrapper.passthrough(Types.INT); // Flags
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Types.STRING); // Background texture
                    }
                    wrapper.passthrough(Types.FLOAT); // X
                    wrapper.passthrough(Types.FLOAT); // Y
                }

                wrapper.passthrough(Types.STRING_ARRAY); // Criteria

                int arrayLength = wrapper.passthrough(Types.VAR_INT);
                for (int array = 0; array < arrayLength; array++) {
                    wrapper.passthrough(Types.STRING_ARRAY); // String array
                }

                wrapper.read(Types.BOOLEAN); // Sends telemetry
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.OPEN_SIGN_EDITOR, wrapper -> {
            final BlockPosition position = wrapper.passthrough(Types.BLOCK_POSITION1_14);
            final boolean frontSide = wrapper.read(Types.BOOLEAN);
            if (frontSide) {
                wrapper.user().remove(BackSignEditStorage.class);
            } else {
                wrapper.user().put(new BackSignEditStorage(position));
            }
        });
        protocol.registerServerbound(ServerboundPackets1_19_4.SIGN_UPDATE, wrapper -> {
            final BlockPosition position = wrapper.passthrough(Types.BLOCK_POSITION1_14);
            final BackSignEditStorage backSignEditStorage = wrapper.user().remove(BackSignEditStorage.class);
            final boolean frontSide = backSignEditStorage == null || !backSignEditStorage.position().equals(position);
            wrapper.write(Types.BOOLEAN, frontSide);
        });

        new RecipeRewriter1_19_4<>(protocol).register(ClientboundPackets1_19_4.UPDATE_RECIPES);
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable final Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToClient(connection, item);

        // Remove new trim tags
        final CompoundTag trimTag;
        final CompoundTag tag = item.tag();
        if (tag != null && (trimTag = tag.getCompoundTag("Trim")) != null) {
            final StringTag patternTag = trimTag.getStringTag("pattern");
            if (patternTag != null) {
                final String pattern = Key.stripMinecraftNamespace(patternTag.getValue());
                if (NEW_TRIM_PATTERNS.contains(pattern)) {
                    tag.remove("Trim");
                    tag.put(nbtTagName("Trim"), trimTag);
                }
            }
        }
        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable final Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToServer(connection, item);

        // Add back original trim tag
        final Tag trimTag;
        final CompoundTag tag = item.tag();
        if (tag != null && (trimTag = tag.remove(nbtTagName("Trim"))) != null) {
            tag.put("Trim", trimTag);
        }
        return item;
    }

    private void handleBlockEntity(final BlockEntity blockEntity) {
        // Check for signs
        if (blockEntity.typeId() != 7 && blockEntity.typeId() != 8) {
            return;
        }

        final CompoundTag tag = blockEntity.tag();
        final Tag frontText = tag.remove("front_text");
        tag.remove("back_text");

        if (frontText instanceof CompoundTag frontTextTag) {
            writeMessages(frontTextTag, tag, false);
            writeMessages(frontTextTag, tag, true);

            final Tag color = frontTextTag.remove("color");
            if (color != null) {
                tag.put("Color", color);
            }

            final Tag glowing = frontTextTag.remove("has_glowing_text");
            if (glowing != null) {
                tag.put("GlowingText", glowing);
            }
        }
    }

    private void writeMessages(final CompoundTag frontText, final CompoundTag tag, final boolean filtered) {
        final ListTag<StringTag> messages = frontText.getListTag(filtered ? "filtered_messages" : "messages", StringTag.class);
        if (messages == null) {
            return;
        }

        int i = 0;
        for (final StringTag message : messages) {
            tag.put((filtered ? "FilteredText" : "Text") + ++i, message);
        }

    }
}
