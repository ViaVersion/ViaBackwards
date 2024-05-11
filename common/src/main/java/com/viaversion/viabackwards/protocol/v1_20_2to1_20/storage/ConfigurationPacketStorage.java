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
package com.viaversion.viabackwards.protocol.v1_20_2to1_20.storage;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.Protocol1_20_2To1_20;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.nbt.tag.CompoundTag;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ConfigurationPacketStorage implements StorableObject {

    private final List<QueuedPacket> rawPackets = new ArrayList<>();
    private CompoundTag registry;
    private String[] enabledFeatures;
    private boolean finished;
    private QueuedPacket resourcePack;

    public void setResourcePack(final PacketWrapper wrapper) {
        resourcePack = toQueuedPacket(wrapper, ClientboundPackets1_19_4.RESOURCE_PACK);
    }

    public CompoundTag registry() {
        Preconditions.checkNotNull(registry);
        return registry;
    }

    public void setRegistry(final CompoundTag registry) {
        this.registry = registry;
    }

    public String @Nullable [] enabledFeatures() {
        return enabledFeatures;
    }

    public void setEnabledFeatures(final String[] enabledFeatures) {
        this.enabledFeatures = enabledFeatures;
    }

    public void addRawPacket(final PacketWrapper wrapper, final PacketType type) {
        rawPackets.add(toQueuedPacket(wrapper, type));
    }

    private QueuedPacket toQueuedPacket(final PacketWrapper wrapper, final PacketType type) {
        Preconditions.checkArgument(!wrapper.isCancelled(), "Wrapper should be cancelled AFTER calling toQueuedPacket");

        // It's easier to just copy it to a byte array buffer than to manually read the data
        final ByteBuf buf = Unpooled.buffer();
        //noinspection deprecation
        wrapper.setId(-1); // Don't write the packet id to the buffer
        wrapper.writeToBuffer(buf);
        return new QueuedPacket(buf, type);
    }

    public void sendQueuedPackets(final UserConnection connection) {
        // Send resource pack at the end
        if (resourcePack != null) {
            rawPackets.add(resourcePack);
            resourcePack = null;
        }

        for (final QueuedPacket queuedPacket : rawPackets) {
            try {
                final PacketWrapper packet = PacketWrapper.create(queuedPacket.packetType(), queuedPacket.buf(), connection);
                packet.send(Protocol1_20_2To1_20.class);
            } finally {
                queuedPacket.buf().release();
            }
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(final boolean finished) {
        this.finished = finished;
    }

    public record QueuedPacket(ByteBuf buf, PacketType packetType) {
    }
}
