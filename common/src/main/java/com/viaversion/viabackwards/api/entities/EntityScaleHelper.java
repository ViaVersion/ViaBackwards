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


import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.FullMappings;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.rewriter.entitydata.EntityDataHandlerEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * A modular helper for injecting entity scaling on older clients via metadata interception.
 * This helper listens for the isBaby metadata flag and injects an UPDATE_ATTRIBUTES packet
 * to scale the entity, since older clients might not support the newer entity variants natively.
 */
public class EntityScaleHelper {

    private static final class BabyScale {
        private final float scaleFactor;
        private final int index;

        private BabyScale(float scaleFactor, int index) {
            this.scaleFactor = scaleFactor;
            this.index = index;
        }
    }

    private final Map<EntityType, BabyScale> babyScales = new HashMap<>();
    private final ClientboundPacketType updateAttributesPacket;

    public EntityScaleHelper(ClientboundPacketType updateAttributesPacket) {
        this.updateAttributesPacket = updateAttributesPacket;
    }

    /**
     * Registers a scaling factor for a specific baby entity type.
     * 
     * @param type The entity type that requires scaling when it is a baby.
     * @param babyScaleFactor The multiplier to apply to the minecraft:scale attribute when it is a baby.
     * @param babyIndex The metadata index for the is_baby property in this specific protocol
     */
    public void addBabyScale(EntityType type, float babyScaleFactor, int babyIndex) {
        babyScales.put(type, new BabyScale(babyScaleFactor, babyIndex));
    }

    public Iterable<EntityType> getRegisteredTypes() {
        return babyScales.keySet();
    }

    /**
     * Checks the metadata, tracks the scale state natively, and injects an UPDATE_ATTRIBUTES
     * packet down the pipeline right as the metadata is processed by the client.
     */
    public void trackAndInject(final EntityDataHandlerEvent event, final EntityData data, final Protocol<?, ?, ?, ?> protocol) {
        final StoredEntityData storedEntityData = event.user().getEntityTracker(protocol.getClass()).entityData(event.entityId());
        if (storedEntityData == null) return;
                  
        final BabyScale babyScale = babyScales.get(storedEntityData.type());
        if (babyScale == null || data.id() != babyScale.index || !(data.value() instanceof Boolean)) {
            return;
        }

        EntityScaleData scaleData = storedEntityData.get(EntityScaleData.class);
        if (scaleData == null) {
            scaleData = new EntityScaleData();
            storedEntityData.put(scaleData);
        }
        
        final boolean isBaby = (Boolean) data.value();
        final float scale = isBaby ? babyScale.scaleFactor : 1.0f;
        
        if (scaleData.isBaby() != isBaby || scaleData.scale() != scale) {
            scaleData.setBaby(isBaby);
            scaleData.setScale(scale);
            
            final FullMappings attributeMappings = protocol.getMappingData().getAttributeMappings();
            if (attributeMappings == null) return;
            
            int unmappedId = attributeMappings.id("minecraft:scale");
            if (unmappedId == -1) {
                unmappedId = attributeMappings.id("minecraft:generic.scale");
                if (unmappedId == -1) {
                    unmappedId = attributeMappings.id("generic.scale");
                }
            }
            
            final int mappedId = unmappedId != -1 ? protocol.getMappingData().getNewAttributeId(unmappedId) : -1;
            
            if (mappedId != -1) {
                final PacketWrapper updatePacket = PacketWrapper.create(updateAttributesPacket, event.user());
                updatePacket.write(Types.VAR_INT, event.entityId());
                updatePacket.write(Types.VAR_INT, 1); // 1 attribute
                updatePacket.write(Types.VAR_INT, mappedId);
                updatePacket.write(Types.DOUBLE, (double) scaleData.scale());
                updatePacket.write(Types.VAR_INT, 0); // 0 modifiers
                updatePacket.scheduleSend(protocol.getClass());
            } else if (Via.getManager().isDebug()) {
                protocol.getLogger().warning("Scale mapping not found for EntityScaleHelper, baby scaling won't be applied. It's harmless unless you're using custom mappings.");
            }
        }
    }
}
