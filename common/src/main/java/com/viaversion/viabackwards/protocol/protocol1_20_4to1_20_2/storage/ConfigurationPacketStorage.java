/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_20_4to1_20_2.storage;

import com.viaversion.viabackwards.protocol.protocol1_20_4to1_20_2.Protocol1_20To1_20_2;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;

public final class ConfigurationPacketStorage implements StorableObject {

    private final List<QueuedPacket> rawPackets = new ArrayList<>();
    private CompoundTag registry;
    private String[] enabledFeatures;

    public CompoundTag registry() {
        return registry;
    }

    public void setRegistry(final CompoundTag registry) {
        this.registry = registry;
    }

    public String[] enabledFeatures() {
        return enabledFeatures;
    }

    public void setEnabledFeatures(final String[] enabledFeatures) {
        this.enabledFeatures = enabledFeatures;
    }

    public List<QueuedPacket> getRawPackets() {
        return rawPackets;
    }

    public void addRawPacket(final PacketWrapper wrapper, final PacketType type) throws Exception {
        // It's easier to just copy it to a byte array buffer than to manually read the data
        final ByteBuf buf = Unpooled.buffer();
        final int id = wrapper.getId();
        //noinspection deprecation
        wrapper.setId(-1); // Don't write the packet id to the buffer
        wrapper.writeToBuffer(buf);
        rawPackets.add(new QueuedPacket(buf, type));
    }

    public void sendQueuedPackets(final UserConnection connection) throws Exception {
        for (final QueuedPacket queuedPacket : rawPackets) {
            try {
                final PacketWrapper packet = PacketWrapper.create(queuedPacket.packetType(), queuedPacket.buf(), connection);
                packet.send(Protocol1_20To1_20_2.class);
            } finally {
                queuedPacket.buf().release();
            }
        }
    }

    public static final class QueuedPacket {
        private final ByteBuf buf;
        private final PacketType packetType;

        public QueuedPacket(final ByteBuf buf, final PacketType packetType) {
            this.buf = buf;
            this.packetType = packetType;
        }

        public ByteBuf buf() {
            return buf;
        }

        public PacketType packetType() {
            return packetType;
        }
    }
}
