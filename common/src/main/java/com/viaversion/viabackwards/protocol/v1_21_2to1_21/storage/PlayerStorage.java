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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage;

import com.viaversion.viabackwards.api.entities.storage.PlayerPositionStorage;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;

public final class PlayerStorage extends PlayerPositionStorage {
    private static final PlayerInput EMPTY = new PlayerInput(false, false, false, false, false, false, false);
    private static final float PLAYER_JUMP_HEIGHT = 0.42F;

    private float yaw;
    private float pitch;

    private boolean playerCommandTrackedSneaking;
    private boolean playerCommandTrackedSprinting;

    private PlayerInput lastInput = EMPTY;
    private double prevX;
    private double prevY;
    private double prevZ;

    public void setPosition(PacketWrapper wrapper) {
        setX(wrapper.get(Types.DOUBLE, 0));
        setY(wrapper.get(Types.DOUBLE, 1));
        setZ(wrapper.get(Types.DOUBLE, 2));
    }

    public void tick(final UserConnection user) {
        final double deltaX = x() - prevX;
        final double deltaY = y() - prevY;
        final double deltaZ = z() - prevZ;

        final double magnitude = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double directionX = magnitude > 0 ? deltaX / magnitude : 0;
        double directionZ = magnitude > 0 ? deltaZ / magnitude : 0;

        directionX = Math.max(-1, Math.min(1, directionX));
        directionZ = Math.max(-1, Math.min(1, directionZ));

        final double angle = Math.toRadians(-yaw);
        final double newDirectionX = directionX * Math.cos(angle) - directionZ * Math.sin(angle);
        final double newDirectionZ = directionX * Math.sin(angle) + directionZ * Math.cos(angle);

        final boolean forward = newDirectionZ >= 0.65F;
        final boolean backwards = newDirectionZ <= -0.65F;
        final boolean left = newDirectionX >= 0.65F;
        final boolean right = newDirectionX <= -0.65F;
        final boolean jump = Math.abs(deltaY - PLAYER_JUMP_HEIGHT) <= 1E-4F;

        final PlayerInput input = new PlayerInput(forward, backwards, left, right, jump, playerCommandTrackedSneaking, playerCommandTrackedSprinting);
        if (!lastInput.equals(input)) {
            final PacketWrapper playerInputPacket = PacketWrapper.create(ServerboundPackets1_21_2.PLAYER_INPUT, user);
            byte flags = 0;
            flags = (byte) (flags | (input.forward() ? 1 : 0));
            flags = (byte) (flags | (input.backward() ? 2 : 0));
            flags = (byte) (flags | (input.left() ? 4 : 0));
            flags = (byte) (flags | (input.right() ? 8 : 0));
            flags = (byte) (flags | (input.jump() ? 16 : 0));
            flags = (byte) (flags | (input.sneak() ? 32 : 0));
            flags = (byte) (flags | (input.sprint() ? 64 : 0));
            playerInputPacket.write(Types.BYTE, flags);

            playerInputPacket.sendToServer(Protocol1_21_2To1_21.class);
            lastInput = input;
        }

        this.prevX = x();
        this.prevY = y();
        this.prevZ = z();
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public void setRotation(final PacketWrapper wrapper) {
        this.yaw = wrapper.get(Types.FLOAT, 0);
        this.pitch = wrapper.get(Types.FLOAT, 1);
    }

    public void setPlayerCommandTrackedSneaking(final boolean playerCommandTrackedSneaking) {
        this.playerCommandTrackedSneaking = playerCommandTrackedSneaking;
    }

    public void setPlayerCommandTrackedSprinting(final boolean playerCommandTrackedSprinting) {
        this.playerCommandTrackedSprinting = playerCommandTrackedSprinting;
    }

    public boolean setSneaking(final boolean sneaking) {
        final boolean changed = this.playerCommandTrackedSneaking != sneaking;
        this.playerCommandTrackedSneaking = sneaking;
        return changed;
    }

    public record PlayerInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump,
                              boolean sneak, boolean sprint) {
    }

}
