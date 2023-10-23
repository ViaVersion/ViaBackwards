/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.data;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.Protocol1_9To1_8;
import com.viaversion.viaversion.util.Key;

public class ShoulderTracker extends StoredObject {
    private int entityId;
    private String leftShoulder;
    private String rightShoulder;

    public ShoulderTracker(UserConnection user) {
        super(user);
    }

    public void update() {
        PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_12.CHAT_MESSAGE, null, getUser());

        wrapper.write(Type.COMPONENT, Protocol1_9To1_8.fixJson(generateString()));
        wrapper.write(Type.BYTE, (byte) 2);

        try {
            wrapper.scheduleSend(Protocol1_11_1To1_12.class);
        } catch (Exception e) {
            ViaBackwards.getPlatform().getLogger().severe("Failed to send the shoulder indication");
            e.printStackTrace();
        }
    }

    // Does actionbar not support json colors? :(
    private String generateString() {
        StringBuilder builder = new StringBuilder();

        // Empty spaces because the non-json formatting is weird
        builder.append("  ");
        if (leftShoulder == null) {
            builder.append("§4§lNothing");
        } else {
            builder.append("§2§l").append(getName(leftShoulder));
        }

        builder.append("§8§l <- §7§lShoulders§8§l -> ");

        if (rightShoulder == null) {
            builder.append("§4§lNothing");
        } else {
            builder.append("§2§l").append(getName(rightShoulder));
        }

        return builder.toString();
    }

    private String getName(String current) {
        current = Key.stripMinecraftNamespace(current);

        String[] array = current.split("_");
        StringBuilder builder = new StringBuilder();

        for (String s : array) {
            builder.append(s.substring(0, 1).toUpperCase())
                    .append(s.substring(1))
                    .append(" ");
        }

        return builder.toString();
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public String getLeftShoulder() {
        return leftShoulder;
    }

    public void setLeftShoulder(String leftShoulder) {
        this.leftShoulder = leftShoulder;
    }

    public String getRightShoulder() {
        return rightShoulder;
    }

    public void setRightShoulder(String rightShoulder) {
        this.rightShoulder = rightShoulder;
    }

    @Override
    public String toString() {
        return "ShoulderTracker{" + "entityId=" + entityId + ", leftShoulder='" + leftShoulder + '\'' + ", rightShoulder='" + rightShoulder + '\'' + '}';
    }
}
