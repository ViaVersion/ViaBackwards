/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_17to1_17_1;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.storage.PlayerLastCursorItem;
import com.viaversion.viabackwards.protocol.protocol1_17to1_17_1.storage.InventoryStateIds;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_17_1to1_17.ClientboundPackets1_17_1;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;

public final class Protocol1_17To1_17_1 extends BackwardsProtocol<ClientboundPackets1_17_1, ClientboundPackets1_17, ServerboundPackets1_17, ServerboundPackets1_17> {

    private static final int MAX_PAGE_LENGTH = 8192;
    private static final int MAX_TITLE_LENGTH = 128;
    private static final int MAX_PAGES = 200; // Actually limited to 100 when handling the read packet, but /shrug

    public Protocol1_17To1_17_1() {
        super(ClientboundPackets1_17_1.class, ClientboundPackets1_17.class, ServerboundPackets1_17.class, ServerboundPackets1_17.class);
    }

    @Override
    protected void registerPackets() {
        registerClientbound(ClientboundPackets1_17_1.REMOVE_ENTITIES, null, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int[] entityIds = wrapper.read(Type.VAR_INT_ARRAY_PRIMITIVE);
                    wrapper.cancel();
                    for (int entityId : entityIds) {
                        // Send individual remove packets
                        PacketWrapper newPacket = wrapper.create(ClientboundPackets1_17.REMOVE_ENTITY);
                        newPacket.write(Type.VAR_INT, entityId);
                        newPacket.send(Protocol1_17To1_17_1.class);
                    }
                });
            }
        });

        registerClientbound(ClientboundPackets1_17_1.CLOSE_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    short containerId = wrapper.passthrough(Type.UNSIGNED_BYTE);
                    wrapper.user().get(InventoryStateIds.class).removeStateId(containerId);
                });
            }
        });
        registerClientbound(ClientboundPackets1_17_1.SET_SLOT, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    short containerId = wrapper.passthrough(Type.UNSIGNED_BYTE);
                    int stateId = wrapper.read(Type.VAR_INT);
                    wrapper.user().get(InventoryStateIds.class).setStateId(containerId, stateId);
                });
            }
        });
        registerClientbound(ClientboundPackets1_17_1.WINDOW_ITEMS, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    short containerId = wrapper.passthrough(Type.UNSIGNED_BYTE);
                    int stateId = wrapper.read(Type.VAR_INT);
                    wrapper.user().get(InventoryStateIds.class).setStateId(containerId, stateId);

                    // Length is encoded as a var int in 1.17.1
                    wrapper.write(Type.FLAT_VAR_INT_ITEM_ARRAY, wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT));

                    // Carried item - should work without adding it to the array above
                    Item carried = wrapper.read(Type.FLAT_VAR_INT_ITEM);

                    PlayerLastCursorItem lastCursorItem = wrapper.user().get(PlayerLastCursorItem.class);
                    if (lastCursorItem != null) {
                        // For click drag ghost item fix -- since the state ID is always wrong,
                        // the server always resends the entire window contents after a drag action,
                        // which is useful since we need to update the carried item in preparation
                        // for a subsequent drag

                        lastCursorItem.setLastCursorItem(carried);
                    }
                });
            }
        });

        registerServerbound(ServerboundPackets1_17.CLOSE_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    short containerId = wrapper.passthrough(Type.UNSIGNED_BYTE);
                    wrapper.user().get(InventoryStateIds.class).removeStateId(containerId);
                });
            }
        });
        registerServerbound(ServerboundPackets1_17.CLICK_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    short containerId = wrapper.passthrough(Type.UNSIGNED_BYTE);
                    int stateId = wrapper.user().get(InventoryStateIds.class).removeStateId(containerId);
                    wrapper.write(Type.VAR_INT, stateId == Integer.MAX_VALUE ? 0 : stateId);
                });
            }
        });

        registerServerbound(ServerboundPackets1_17.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    Item item = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                    boolean signing = wrapper.read(Type.BOOLEAN);
                    wrapper.passthrough(Type.VAR_INT); // Slot comes first

                    CompoundTag tag = item.tag();
                    ListTag pagesTag;
                    StringTag titleTag = null;
                    // Sanity checks
                    if (tag == null || (pagesTag = tag.get("pages")) == null
                            || (signing && (titleTag = tag.get("title")) == null)) {
                        wrapper.write(Type.VAR_INT, 0); // Pages length
                        wrapper.write(Type.BOOLEAN, false); // Optional title
                        return;
                    }

                    // Write pages - limit them first
                    if (pagesTag.size() > MAX_PAGES) {
                        pagesTag = new ListTag(pagesTag.getValue().subList(0, MAX_PAGES));
                    }

                    wrapper.write(Type.VAR_INT, pagesTag.size());
                    for (Tag pageTag : pagesTag) {
                        String page = ((StringTag) pageTag).getValue();
                        // Limit page length
                        if (page.length() > MAX_PAGE_LENGTH) {
                            page = page.substring(0, MAX_PAGE_LENGTH);
                        }

                        wrapper.write(Type.STRING, page);
                    }

                    // Write optional title
                    wrapper.write(Type.BOOLEAN, signing);
                    if (signing) {
                        if (titleTag == null) {
                            titleTag = tag.get("title");
                        }

                        // Limit title length
                        String title = titleTag.getValue();
                        if (title.length() > MAX_TITLE_LENGTH) {
                            title = title.substring(0, MAX_TITLE_LENGTH);
                        }

                        wrapper.write(Type.STRING, title);
                    }
                });
            }
        });
    }

    @Override
    public void init(UserConnection connection) {
        connection.put(new InventoryStateIds());
    }
}
