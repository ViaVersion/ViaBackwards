/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.viabackwards.api.entities;


import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.data.FullMappings;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.rewriter.EntityRewriter;
import com.viaversion.viaversion.rewriter.entitydata.EntityDataHandlerEvent;

/**
 * A helper for injecting entity scaling on older clients.
 * This helper listens for the baby entity data flag and sends an UPDATE_ATTRIBUTES packet
 * to scale the entity, since older clients might not support the newer entity variants natively.
 */
public class EntityScaleHelper {

    private final ClientboundPacketType updateAttributesPacket;
    private final EntityRewriter<?, ?> rewriter;

    /**
     * Constructs a new entity scale helper.
     *
     * @param rewriter               entity rewriter
     * @param updateAttributesPacket UPDATE_ATTRIBUTES packet of the mapped protocol
     * @param <CM>                   mapped clientbound packet type
     */
    public <CM extends ClientboundPacketType, P extends Protocol<?, CM, ?, ?>> EntityScaleHelper(final EntityRewriter<?, P> rewriter, final CM updateAttributesPacket) {
        this.rewriter = rewriter;
        this.updateAttributesPacket = updateAttributesPacket;
    }

    /**
     * Registers a scaling factor for a specific baby entity type.
     *
     * @param type            entity type that requires scaling when it is a baby
     * @param babyScaleFactor multiplier to apply to the minecraft:scale attribute when it is a baby
     * @param babyIndex       entity data index for the baby property in this specific protocol
     */
    public void addBabyScale(final EntityType type, final float babyScaleFactor, final int babyIndex) {
        rewriter.filter().type(type).index(babyIndex).handler(((event, data) -> trackAndSend(event, data, babyScaleFactor)));
    }

    /**
     * Checks the entity data, tracks the scale state and sends an UPDATE_ATTRIBUTES packet if needed.
     */
    public void trackAndSend(final EntityDataHandlerEvent event, final EntityData data, final float babyScaleFactor) {
        if (event.trackedEntity() == null) {
            return;
        }

        final StoredEntityData storedEntityData = event.trackedEntity().data();
        EntityScaleData scaleData = storedEntityData.get(EntityScaleData.class);
        if (scaleData == null) {
            scaleData = new EntityScaleData();
            storedEntityData.put(scaleData);
        }

        final boolean isBaby = data.value();
        if (scaleData.isBaby() == isBaby) {
            return;
        }

        scaleData.setBaby(isBaby);
        scaleData.setScale(isBaby ? babyScaleFactor : 1.0f);

        final FullMappings attributeMappings = rewriter.protocol().getMappingData().getAttributeMappings();
        int attributeId = attributeMappings.mappedId("scale");
        if (attributeId == -1) {
            attributeId = attributeMappings.mappedId("generic.scale");
            Preconditions.checkArgument(attributeId != -1);
        }

        final PacketWrapper attributesPacket = PacketWrapper.create(updateAttributesPacket, event.user());
        attributesPacket.write(Types.VAR_INT, event.entityId());
        attributesPacket.write(Types.VAR_INT, 1); // 1 attribute
        attributesPacket.write(Types.VAR_INT, attributeId);
        attributesPacket.write(Types.DOUBLE, (double) scaleData.scale());
        attributesPacket.write(Types.VAR_INT, 0); // 0 modifiers
        attributesPacket.send(rewriter.protocol().getClass());
    }
}
