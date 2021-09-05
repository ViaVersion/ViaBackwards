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
package com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.packets;

import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.Protocol1_19_4To1_20;
import com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.storage.BackSignEditStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ServerboundPackets1_19_4;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.rewriter.RecipeRewriter1_19_4;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.Key;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockItemPackets1_20 extends BackwardsItemRewriter<ClientboundPackets1_19_4, ServerboundPackets1_19_4, Protocol1_19_4To1_20> {

    private static final Set<String> NEW_TRIM_PATTERNS = new HashSet<>(Arrays.asList("host", "raiser", "shaper", "silence", "wayfinder"));

    public BlockItemPackets1_20(final Protocol1_19_4To1_20 protocol) {
        super(protocol, Type.ITEM1_13_2, Type.ITEM1_13_2_ARRAY);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPackets1_19_4> blockRewriter = BlockRewriter.for1_14(protocol);
        blockRewriter.registerBlockAction(ClientboundPackets1_19_4.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_19_4.BLOCK_CHANGE);
        blockRewriter.registerEffect(ClientboundPackets1_19_4.EFFECT, 1010, 2001);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_19_4.BLOCK_ENTITY_DATA, this::handleBlockEntity);

        protocol.registerClientbound(ClientboundPackets1_19_4.CHUNK_DATA, new PacketHandlers() {
            @Override
            protected void register() {
                handler(blockRewriter.chunkDataHandler1_19(ChunkType1_18::new, (user, blockEntity) -> handleBlockEntity(blockEntity)));
                create(Type.BOOLEAN, true); // Trust edges
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.UPDATE_LIGHT, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // X
            wrapper.passthrough(Type.VAR_INT); // Y
            wrapper.write(Type.BOOLEAN, true); // Trust edges
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.MULTI_BLOCK_CHANGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.LONG); // Chunk position
                create(Type.BOOLEAN, false); // Suppress light updates
                handler(wrapper -> {
                    for (final BlockChangeRecord record : wrapper.passthrough(Type.VAR_LONG_BLOCK_CHANGE_RECORD_ARRAY)) {
                        record.setBlockId(protocol.getMappingData().getNewBlockStateId(record.getBlockId()));
                    }
                });
            }
        });

        registerOpenWindow(ClientboundPackets1_19_4.OPEN_WINDOW);
        registerSetCooldown(ClientboundPackets1_19_4.COOLDOWN);
        registerWindowItems1_17_1(ClientboundPackets1_19_4.WINDOW_ITEMS);
        registerSetSlot1_17_1(ClientboundPackets1_19_4.SET_SLOT);
        registerEntityEquipmentArray(ClientboundPackets1_19_4.ENTITY_EQUIPMENT);
        registerClickWindow1_17_1(ServerboundPackets1_19_4.CLICK_WINDOW);
        registerTradeList1_19(ClientboundPackets1_19_4.TRADE_LIST);
        registerCreativeInvAction(ServerboundPackets1_19_4.CREATIVE_INVENTORY_ACTION);
        registerWindowPropertyEnchantmentHandler(ClientboundPackets1_19_4.WINDOW_PROPERTY);
        registerSpawnParticle1_19(ClientboundPackets1_19_4.SPAWN_PARTICLE);

        protocol.registerClientbound(ClientboundPackets1_19_4.ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Type.BOOLEAN); // Reset/clear
            int size = wrapper.passthrough(Type.VAR_INT); // Mapping size
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Type.STRING); // Identifier
                wrapper.passthrough(Type.OPTIONAL_STRING); // Parent

                // Display data
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    wrapper.passthrough(Type.COMPONENT); // Title
                    wrapper.passthrough(Type.COMPONENT); // Description
                    handleItemToClient(wrapper.user(), wrapper.passthrough(Type.ITEM1_13_2)); // Icon
                    wrapper.passthrough(Type.VAR_INT); // Frame type
                    int flags = wrapper.passthrough(Type.INT); // Flags
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Type.STRING); // Background texture
                    }
                    wrapper.passthrough(Type.FLOAT); // X
                    wrapper.passthrough(Type.FLOAT); // Y
                }

                wrapper.passthrough(Type.STRING_ARRAY); // Criteria

                int arrayLength = wrapper.passthrough(Type.VAR_INT);
                for (int array = 0; array < arrayLength; array++) {
                    wrapper.passthrough(Type.STRING_ARRAY); // String array
                }

                wrapper.read(Type.BOOLEAN); // Sends telemetry
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.OPEN_SIGN_EDITOR, wrapper -> {
            final Position position = wrapper.passthrough(Type.POSITION1_14);
            final boolean frontSide = wrapper.read(Type.BOOLEAN);
            if (frontSide) {
                wrapper.user().remove(BackSignEditStorage.class);
            } else {
                wrapper.user().put(new BackSignEditStorage(position));
            }
        });
        protocol.registerServerbound(ServerboundPackets1_19_4.UPDATE_SIGN, wrapper -> {
            final Position position = wrapper.passthrough(Type.POSITION1_14);
            final BackSignEditStorage backSignEditStorage = wrapper.user().remove(BackSignEditStorage.class);
            final boolean frontSide = backSignEditStorage == null || !backSignEditStorage.position().equals(position);
            wrapper.write(Type.BOOLEAN, frontSide);
        });

        new RecipeRewriter1_19_4<>(protocol).register(ClientboundPackets1_19_4.DECLARE_RECIPES);
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