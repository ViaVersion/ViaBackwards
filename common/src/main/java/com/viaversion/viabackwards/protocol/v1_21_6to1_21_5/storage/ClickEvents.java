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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.Protocol1_21_6To1_21_5;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.Dialog;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.provider.DialogViewProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectArrayMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectMap;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.util.Key;
import java.util.UUID;

public final class ClickEvents implements StorableObject {

    private final Object2ObjectMap<String, CompoundTag> clickEvents = new Object2ObjectArrayMap<>();

    public String storeClickEvent(final CompoundTag clickEvent) {
        final String id = "vv_" + UUID.randomUUID();
        if (clickEvents.containsKey(id)) {
            return storeClickEvent(clickEvent); // Ensure unique ID
        }
        clickEvents.put(id, clickEvent);
        return id;
    }

    public boolean handleChatCommand(final UserConnection connection, final String command) {
        final CompoundTag clickEvent = clickEvents.get(command);
        if (clickEvent != null) {
            handleClickEvent(connection, clickEvent);
            return true;
        } else {
            return false;
        }
    }

    public static void handleClickEvent(final UserConnection connection, final CompoundTag clickEvent) {
        final String action = Key.stripMinecraftNamespace(clickEvent.getString("action"));
        if ("show_dialog".equals(action)) {
            if (!ViaBackwards.getConfig().dialogsViaChests()) {
                return;
            }

            final RegistryAndTags registryAndTags = connection.get(RegistryAndTags.class);
            final ServerLinks serverLinks = connection.get(ServerLinks.class);

            CompoundTag dialogTag = clickEvent.getCompoundTag("dialog");
            if (dialogTag == null) {
                final StringTag dialogReferenceTag = clickEvent.getStringTag("dialog");
                if (dialogReferenceTag != null) { // No tags here
                    dialogTag = registryAndTags.fromRegistry(Key.stripMinecraftNamespace(dialogReferenceTag.getValue()));
                }
            }

            final DialogViewProvider provider = Via.getManager().getProviders().get(DialogViewProvider.class);
            provider.openDialog(connection, new Dialog(registryAndTags, serverLinks, dialogTag));
        } else if ("custom".equals(action)) {
            final String id = clickEvent.getString("id");
            final CompoundTag payload = clickEvent.getCompoundTag("payload");

            final PacketWrapper customClickAction = PacketWrapper.create(ServerboundPackets1_21_6.CUSTOM_CLICK_ACTION, connection);
            customClickAction.write(Types.STRING, id);
            customClickAction.write(Types.CUSTOM_CLICK_ACTION_TAG, payload);

            customClickAction.sendToServer(Protocol1_21_6To1_21_5.class);
        }
    }

}
