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
package com.viaversion.viabackwards.api.entities.storage;

import com.viaversion.viabackwards.api.rewriters.EntityRewriterBase;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.util.ProtocolLogger;
import java.util.function.Supplier;

public class EntityPositionHandler {

    public static final double RELATIVE_MOVE_FACTOR = 32 * 128;
    private final EntityRewriterBase<?, ?> entityRewriter;
    private final Class<? extends EntityPositionStorage> storageClass;
    private final Supplier<? extends EntityPositionStorage> storageSupplier;
    private boolean warnedForMissingEntity;

    public EntityPositionHandler(EntityRewriterBase<?, ?> entityRewriter,
                                 Class<? extends EntityPositionStorage> storageClass, Supplier<? extends EntityPositionStorage> storageSupplier) {
        this.entityRewriter = entityRewriter;
        this.storageClass = storageClass;
        this.storageSupplier = storageSupplier;
    }

    public void cacheEntityPosition(PacketWrapper wrapper, boolean create, boolean relative) {
        cacheEntityPosition(wrapper,
            wrapper.get(Types.DOUBLE, 0), wrapper.get(Types.DOUBLE, 1), wrapper.get(Types.DOUBLE, 2), create, relative);
    }

    public void cacheEntityPosition(PacketWrapper wrapper, double x, double y, double z, boolean create, boolean relative) {
        int entityId = wrapper.get(Types.VAR_INT, 0);
        StoredEntityData storedEntity = entityRewriter.tracker(wrapper.user()).entityData(entityId);
        if (storedEntity == null) {
            if (Via.getManager().isDebug()) { // There is too many plugins violating this out there, and reading seems to be hard! :>
                ProtocolLogger logger = entityRewriter.protocol().getLogger();
                logger.warning("Stored entity with id " + entityId + " missing at position: " + x + " - " + y + " - " + z + " in " + storageClass.getSimpleName());
                if (entityId == -1 && x == 0 && y == 0 && z == 0) {
                    logger.warning("DO NOT REPORT THIS TO VIA, THIS IS A PLUGIN ISSUE");
                } else if (!warnedForMissingEntity) {
                    warnedForMissingEntity = true;
                    logger.warning("This is very likely caused by a plugin sending a teleport packet for an entity outside of the player's range.");
                }
            }
            return;
        }

        EntityPositionStorage positionStorage;
        if (create) {
            positionStorage = storageSupplier.get();
            storedEntity.put(positionStorage);
        } else {
            positionStorage = storedEntity.get(storageClass);
            if (positionStorage == null) {
                entityRewriter.protocol().getLogger().warning("Stored entity with id " + entityId + " missing " + storageClass.getSimpleName());
                return;
            }
        }

        if (relative) {
            positionStorage.addRelativePosition(x, y, z);
        } else {
            positionStorage.setPosition(x, y, z);
        }
    }

    public EntityPositionStorage getStorage(UserConnection user, int entityId) {
        StoredEntityData storedEntity = entityRewriter.tracker(user).entityData(entityId);
        EntityPositionStorage entityStorage;
        if (storedEntity == null || (entityStorage = storedEntity.get(EntityPositionStorage.class)) == null) {
            entityRewriter.protocol().getLogger().warning("Untracked entity with id " + entityId + " in " + storageClass.getSimpleName());
            return null;
        }
        return entityStorage;
    }

    public static void writeFacingAngles(PacketWrapper wrapper, double x, double y, double z, double targetX, double targetY, double targetZ) {
        double dX = targetX - x;
        double dY = targetY - y;
        double dZ = targetZ - z;
        double r = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        double yaw = -Math.atan2(dX, dZ) / Math.PI * 180;
        if (yaw < 0) {
            yaw = 360 + yaw;
        }
        double pitch = -Math.asin(dY / r) / Math.PI * 180;

        wrapper.write(Types.BYTE, (byte) (yaw * 256f / 360f));
        wrapper.write(Types.BYTE, (byte) (pitch * 256f / 360f));
    }

    public static void writeFacingDegrees(PacketWrapper wrapper, double x, double y, double z, double targetX, double targetY, double targetZ) {
        double dX = targetX - x;
        double dY = targetY - y;
        double dZ = targetZ - z;
        double r = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        double yaw = -Math.atan2(dX, dZ) / Math.PI * 180;
        if (yaw < 0) {
            yaw = 360 + yaw;
        }
        double pitch = -Math.asin(dY / r) / Math.PI * 180;

        wrapper.write(Types.FLOAT, (float) yaw);
        wrapper.write(Types.FLOAT, (float) pitch);
    }
}
