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
package com.viaversion.viabackwards.protocol.v1_16to1_15_2.storage;

import com.viaversion.viabackwards.protocol.v1_16to1_15_2.Protocol1_16To1_15_2;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerAttributesStorage implements StorableObject {

    private final Map<String, Attribute> attributes = new HashMap<>();

    public void sendAttributes(final UserConnection connection, final int entityId) {
        final PacketWrapper updateAttributes = PacketWrapper.create(ClientboundPackets1_15.UPDATE_ATTRIBUTES, connection);

        updateAttributes.write(Types.VAR_INT, entityId);
        updateAttributes.write(Types.INT, attributes.size());
        for (final Map.Entry<String, Attribute> attributeEntry : attributes.entrySet()) {
            final Attribute attribute = attributeEntry.getValue();
            updateAttributes.write(Types.STRING, attributeEntry.getKey());
            updateAttributes.write(Types.DOUBLE, attribute.value());
            updateAttributes.write(Types.VAR_INT, attribute.modifiers().length);

            for (final AttributeModifier modifier : attribute.modifiers()) {
                updateAttributes.write(Types.UUID, modifier.uuid());
                updateAttributes.write(Types.DOUBLE, modifier.amount());
                updateAttributes.write(Types.BYTE, modifier.operation());
            }
        }
        updateAttributes.send(Protocol1_16To1_15_2.class);
    }

    public void clearAttributes() {
        attributes.clear();
    }

    public void addAttribute(final String key, final Attribute attribute) {
        attributes.put(key, attribute);
    }

    public record Attribute(double value, AttributeModifier[] modifiers) {
    }

    public record AttributeModifier(UUID uuid, double amount, byte operation) {
    }
}
