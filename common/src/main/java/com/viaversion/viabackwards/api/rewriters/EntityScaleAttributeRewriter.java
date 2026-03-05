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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.entities.EntityScaleData;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.rewriter.AttributeRewriter;

/**
 * An extension of AttributeRewriter that intercepts UPDATE_ATTRIBUTES packets
 * and dynamically multiplies the scale value if the entity's tracked state
 * (EntityScaleData) currently dictates it should be shrunk (e.g. a baby mob mapped to an adult).
 * 
 * This protects scaled entities from instantly growing to adult size when the server 
 * issues random attribute updates for other reasons.
 */
public class EntityScaleAttributeRewriter<C extends ClientboundPacketType> extends AttributeRewriter<C> {
    
    private final String scaleAttributeId;
    private final Protocol<C, ?, ?, ?> scaleProtocol;

    public EntityScaleAttributeRewriter(Protocol<C, ?, ?, ?> protocol, String scaleAttributeId) {
        super(protocol);
        this.scaleProtocol = protocol;
        this.scaleAttributeId = scaleAttributeId;
    }

    public EntityScaleAttributeRewriter(Protocol<C, ?, ?, ?> protocol) {
        this(protocol, "minecraft:scale");
    }

    @Override
    public void register1_21(C packetType) {
        scaleProtocol.registerClientbound(packetType, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            
            // Fast lookup for scaling factor, negligible overhead if not present
            float scale = 1.0f;
            try {
                StoredEntityData data = wrapper.user().getEntityTracker(scaleProtocol.getClass()).entityData(entityId);
                if (data != null && data.has(EntityScaleData.class)) {
                    scale = data.get(EntityScaleData.class).getScale();
                }
            } catch (Exception ignored) {
                // If entity tracker is missing or data is malformed, just don't scale it
            }

            final int size = wrapper.passthrough(Types.VAR_INT);
            int newSize = size;
            
            int scaleId = -1;
            if (scaleProtocol.getMappingData().getAttributeMappings() != null) {
                scaleId = scaleProtocol.getMappingData().getAttributeMappings().id(scaleAttributeId);
            }

            for (int i = 0; i < size; i++) {
                final int attributeId = wrapper.read(Types.VAR_INT);
                final int mappedId = scaleProtocol.getMappingData().getNewAttributeId(attributeId);
                if (mappedId == -1) {
                    newSize--;

                    wrapper.read(Types.DOUBLE); // Base
                    final int modifierSize = wrapper.read(Types.VAR_INT);
                    for (int j = 0; j < modifierSize; j++) {
                        wrapper.read(Types.STRING); // ID
                        wrapper.read(Types.DOUBLE); // Amount
                        wrapper.read(Types.BYTE); // Operation
                    }
                    continue;
                }

                wrapper.write(Types.VAR_INT, mappedId);
                double value = wrapper.read(Types.DOUBLE); // Base
                
                // Multiply the server's requested scale by our tracked scale modifier
                if (scale != 1.0f && scaleId != -1 && attributeId == scaleId) {
                    value *= scale;
                }
                wrapper.write(Types.DOUBLE, value);
                
                final int modifierSize = wrapper.passthrough(Types.VAR_INT);
                for (int j = 0; j < modifierSize; j++) {
                    wrapper.passthrough(Types.STRING); // ID
                    wrapper.passthrough(Types.DOUBLE); // Amount
                    wrapper.passthrough(Types.BYTE); // Operation
                }
            }

            if (size != newSize) {
                wrapper.set(Types.VAR_INT, 1, newSize);
            }
        });
    }
}
