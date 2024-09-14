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
package com.viaversion.viabackwards.api.rewriters;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.entities.storage.EntityReplacement;
import com.viaversion.viabackwards.api.entities.storage.WrappedEntityData;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.rewriter.EntityRewriter;
import com.viaversion.viaversion.rewriter.entitydata.EntityDataHandlerEvent;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Entity rewriter base class.
 *
 * @see com.viaversion.viabackwards.api.rewriters.EntityRewriter
 * @see LegacyEntityRewriter
 */
public abstract class EntityRewriterBase<C extends ClientboundPacketType, T extends BackwardsProtocol<C, ?, ?, ?>> extends EntityRewriter<C, T> {
    private final Int2ObjectMap<EntityReplacement> entityDataMappings = new Int2ObjectOpenHashMap<>();
    private final EntityDataType displayNameDataType;
    private final EntityDataType displayVisibilityDataType;
    private final int displayNameIndex;
    private final int displayVisibilityIndex;

    EntityRewriterBase(T protocol, EntityDataType displayNameDataType, int displayNameIndex,
                       EntityDataType displayVisibilityDataType, int displayVisibilityIndex) {
        super(protocol, false);
        this.displayNameDataType = displayNameDataType;
        this.displayNameIndex = displayNameIndex;
        this.displayVisibilityDataType = displayVisibilityDataType;
        this.displayVisibilityIndex = displayVisibilityIndex;
    }

    @Override
    public void handleEntityData(int entityId, List<EntityData> entityDataList, UserConnection connection) {
        final TrackedEntity entity = tracker(connection).entity(entityId);
        final boolean initialEntityData = !(entity != null && entity.hasSentEntityData());

        super.handleEntityData(entityId, entityDataList, connection);

        if (entity == null) {
            return; // Don't handle untracked entities - basically always the fault of a plugin sending virtual entities through concurrency-unsafe handling
        }

        // Set the mapped entity name if there is no custom name set already
        final EntityReplacement entityMapping = entityDataForType(entity.entityType());
        final Object displayNameObject;
        if (entityMapping != null && (displayNameObject = entityMapping.entityName()) != null) {
            final EntityData displayName = getData(displayNameIndex, entityDataList);
            if (initialEntityData) {
                if (displayName == null) {
                    // Add it as new entity data
                    entityDataList.add(new EntityData(displayNameIndex, displayNameDataType, displayNameObject));
                    addDisplayVisibilityData(entityDataList);
                } else if (displayName.getValue() == null || displayName.getValue().toString().isEmpty()) {
                    // Overwrite the existing null/empty display name
                    displayName.setValue(displayNameObject);
                    addDisplayVisibilityData(entityDataList);
                }
            } else if (displayName != null && (displayName.getValue() == null || displayName.getValue().toString().isEmpty())) {
                // Overwrite null/empty display name
                displayName.setValue(displayNameObject);
                addDisplayVisibilityData(entityDataList);
            }
        }

        // Add any other extra data for mapped entities
        if (entityMapping != null && entityMapping.hasBaseData() && initialEntityData) {
            entityMapping.defaultData().createData(new WrappedEntityData(entityDataList));
        }
    }

    private void addDisplayVisibilityData(List<EntityData> entityDataList) {
        if (alwaysShowOriginalMobName()) {
            removeData(displayVisibilityIndex, entityDataList);
            entityDataList.add(new EntityData(displayVisibilityIndex, displayVisibilityDataType, getDisplayVisibilityDataValue()));
        }
    }

    protected Object getDisplayVisibilityDataValue() {
        return true;
    }

    protected boolean alwaysShowOriginalMobName() {
        return ViaBackwards.getConfig().alwaysShowOriginalMobName();
    }

    protected @Nullable EntityData getData(int dataIndex, List<EntityData> entityDataList) {
        for (EntityData entityData : entityDataList) {
            if (entityData.id() == dataIndex) {
                return entityData;
            }
        }
        return null;
    }

    protected void removeData(int dataIndex, List<EntityData> entityDataList) {
        entityDataList.removeIf(data -> data.id() == dataIndex);
    }

    protected boolean hasData(EntityType type) {
        return entityDataMappings.containsKey(type.getId());
    }

    protected @Nullable EntityReplacement entityDataForType(EntityType type) {
        return entityDataMappings.get(type.getId());
    }

    protected @Nullable StoredEntityData storedEntityData(EntityDataHandlerEvent event) {
        return tracker(event.user()).entityData(event.entityId());
    }

    /**
     * Maps an entity type to another with extra data.
     * Note that both types should be of the same version.
     *
     * @param type       entity type
     * @param mappedType mapped entity type
     * @return created entity data
     * @see #mapEntityType(EntityType, EntityType) for id only rewriting
     */
    protected EntityReplacement mapEntityTypeWithData(EntityType type, EntityType mappedType) {
        Preconditions.checkArgument(type.getClass() == mappedType.getClass(), "Both entity types need to be of the same class");

        // Already rewrite the id here
        int mappedReplacementId = newEntityId(mappedType.getId());
        EntityReplacement data = new EntityReplacement(protocol, type, mappedReplacementId);
        mapEntityType(type.getId(), mappedReplacementId);
        entityDataMappings.put(type.getId(), data);
        return data;
    }

    public void registerEntityDataTypeHandler(
        @Nullable EntityDataType itemType,
        @Nullable EntityDataType blockStateType,
        @Nullable EntityDataType optionalBlockStateType,
        @Nullable EntityDataType particleType,
        @Nullable EntityDataType componentType,
        @Nullable EntityDataType optionalComponentType
    ) {
        filter().handler((event, data) -> {
            EntityDataType type = data.dataType();
            if (type == itemType) {
                protocol.getItemRewriter().handleItemToClient(event.user(), data.value());
            } else if (type == blockStateType) {
                int value = data.value();
                data.setValue(protocol.getMappingData().getNewBlockStateId(value));
            } else if (type == optionalBlockStateType) {
                int value = data.value();
                if (value != 0) {
                    data.setValue(protocol.getMappingData().getNewBlockStateId(value));
                }
            } else if (type == particleType) {
                rewriteParticle(event.user(), data.value());
            } else if (type == optionalComponentType || type == componentType) {
                JsonElement text = data.value();
                protocol.getComponentRewriter().processText(event.user(), text);
            }
        });
    }

    public void registerEntityDataTypeHandler1_20_3(
        @Nullable EntityDataType itemType,
        @Nullable EntityDataType blockStateType,
        @Nullable EntityDataType optionalBlockStateType,
        @Nullable EntityDataType particleType,
        @Nullable EntityDataType particlesType,
        @Nullable EntityDataType componentType,
        @Nullable EntityDataType optionalComponentType
    ) {
        filter().handler((event, data) -> {
            EntityDataType type = data.dataType();
            if (type == itemType) {
                data.setValue(protocol.getItemRewriter().handleItemToClient(event.user(), data.value()));
            } else if (type == blockStateType) {
                int value = data.value();
                data.setValue(protocol.getMappingData().getNewBlockStateId(value));
            } else if (type == optionalBlockStateType) {
                int value = data.value();
                if (value != 0) {
                    data.setValue(protocol.getMappingData().getNewBlockStateId(value));
                }
            } else if (type == particleType) {
                rewriteParticle(event.user(), data.value());
            } else if (type == particlesType) {
                Particle[] particles = data.value();
                for (final Particle particle : particles) {
                    rewriteParticle(event.user(), particle);
                }
            } else if (type == optionalComponentType || type == componentType) {
                protocol.getComponentRewriter().processTag(event.user(), data.value());
            }
        });
    }

    // ONLY TRACKS, DOESN'T REWRITE IDS
    protected PacketHandler getTrackerHandler(Type<? extends Number> intType, int typeIndex) {
        return wrapper -> {
            Number id = wrapper.get(intType, typeIndex);
            tracker(wrapper.user()).addEntity(wrapper.get(Types.VAR_INT, 0), typeFromId(id.intValue()));
        };
    }

    protected PacketHandler getTrackerHandler() {
        return getTrackerHandler(Types.VAR_INT, 1);
    }

    protected PacketHandler getTrackerHandler(EntityType entityType) {
        return wrapper -> tracker(wrapper.user()).addEntity((int) wrapper.get(Types.VAR_INT, 0), entityType);
    }

    protected PacketHandler getPlayerTrackerHandler() {
        return wrapper -> {
            final int entityId = wrapper.get(Types.INT, 0);

            final EntityTracker tracker = tracker(wrapper.user());
            tracker(wrapper.user()).setClientEntityId(entityId);
            tracker.addEntity(entityId, tracker.playerType());
        };
    }

    protected PacketHandler getDimensionHandler() {
        return wrapper -> {
            ClientWorld clientWorld = wrapper.user().getClientWorld(this.protocol.getClass());
            int dimensionId = wrapper.get(Types.INT, 1);
            if (clientWorld.setEnvironment(dimensionId)) {
                onDimensionChange(wrapper.user());
            }
        };
    }
}
