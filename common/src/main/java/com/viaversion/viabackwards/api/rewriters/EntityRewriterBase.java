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
import com.viaversion.viabackwards.api.entities.storage.WrappedMetadata;
import com.viaversion.viaversion.api.connection.UserConnection;
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
    private final EntityDataType displayNameMetaType;
    private final EntityDataType displayVisibilityMetaType;
    private final int displayNameIndex;
    private final int displayVisibilityIndex;

    EntityRewriterBase(T protocol, EntityDataType displayNameMetaType, int displayNameIndex,
                       EntityDataType displayVisibilityMetaType, int displayVisibilityIndex) {
        super(protocol, false);
        this.displayNameMetaType = displayNameMetaType;
        this.displayNameIndex = displayNameIndex;
        this.displayVisibilityMetaType = displayVisibilityMetaType;
        this.displayVisibilityIndex = displayVisibilityIndex;
    }

    @Override
    public void handleEntityData(int entityId, List<EntityData> entityDataList, UserConnection connection) {
        final TrackedEntity entity = tracker(connection).entity(entityId);
        final boolean initialMetadata = !(entity != null && entity.hasSentEntityData());

        super.handleEntityData(entityId, entityDataList, connection);

        if (entity == null) {
            return; // Don't handle untracked entities - basically always the fault of a plugin sending virtual entities through concurrency-unsafe handling
        }

        // Set the mapped entity name if there is no custom name set already
        final EntityReplacement entityMapping = entityDataForType(entity.entityType());
        final Object displayNameObject;
        if (entityMapping != null && (displayNameObject = entityMapping.entityName()) != null) {
            final EntityData displayName = getData(displayNameIndex, entityDataList);
            if (initialMetadata) {
                if (displayName == null) {
                    // Add it as new metadata
                    entityDataList.add(new EntityData(displayNameIndex, displayNameMetaType, displayNameObject));
                    addDisplayVisibilityMeta(entityDataList);
                } else if (displayName.getValue() == null || displayName.getValue().toString().isEmpty()) {
                    // Overwrite the existing null/empty display name
                    displayName.setValue(displayNameObject);
                    addDisplayVisibilityMeta(entityDataList);
                }
            } else if (displayName != null && (displayName.getValue() == null || displayName.getValue().toString().isEmpty())) {
                // Overwrite null/empty display name
                displayName.setValue(displayNameObject);
                addDisplayVisibilityMeta(entityDataList);
            }
        }

        // Add any other extra meta for mapped entities
        if (entityMapping != null && entityMapping.hasBaseMeta() && initialMetadata) {
            entityMapping.defaultMeta().createMeta(new WrappedMetadata(entityDataList));
        }
    }

    private void addDisplayVisibilityMeta(List<EntityData> metadataList) {
        if (alwaysShowOriginalMobName()) {
            removeMeta(displayVisibilityIndex, metadataList);
            metadataList.add(new EntityData(displayVisibilityIndex, displayVisibilityMetaType, getDisplayVisibilityMetaValue()));
        }
    }

    protected Object getDisplayVisibilityMetaValue() {
        return true;
    }

    protected boolean alwaysShowOriginalMobName() {
        return ViaBackwards.getConfig().alwaysShowOriginalMobName();
    }

    protected @Nullable EntityData getData(int metaIndex, List<EntityData> metadataList) {
        for (EntityData metadata : metadataList) {
            if (metadata.id() == metaIndex) {
                return metadata;
            }
        }
        return null;
    }

    protected void removeMeta(int metaIndex, List<EntityData> metadataList) {
        metadataList.removeIf(meta -> meta.id() == metaIndex);
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

    public void registerMetaTypeHandler(
        @Nullable EntityDataType itemType,
        @Nullable EntityDataType blockStateType,
        @Nullable EntityDataType optionalBlockStateType,
        @Nullable EntityDataType particleType,
        @Nullable EntityDataType componentType,
        @Nullable EntityDataType optionalComponentType
    ) {
        filter().handler((event, meta) -> {
            EntityDataType type = meta.dataType();
            if (type == itemType) {
                protocol.getItemRewriter().handleItemToClient(event.user(), meta.value());
            } else if (type == blockStateType) {
                int data = meta.value();
                meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
            } else if (type == optionalBlockStateType) {
                int data = meta.value();
                if (data != 0) {
                    meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
                }
            } else if (type == particleType) {
                rewriteParticle(event.user(), meta.value());
            } else if (type == optionalComponentType || type == componentType) {
                JsonElement text = meta.value();
                protocol.getTranslatableRewriter().processText(event.user(), text);
            }
        });
    }

    public void registerMetaTypeHandler1_20_3(
        @Nullable EntityDataType itemType,
        @Nullable EntityDataType blockStateType,
        @Nullable EntityDataType optionalBlockStateType,
        @Nullable EntityDataType particleType,
        @Nullable EntityDataType particlesType,
        @Nullable EntityDataType componentType,
        @Nullable EntityDataType optionalComponentType
    ) {
        filter().handler((event, meta) -> {
            EntityDataType type = meta.dataType();
            if (type == itemType) {
                meta.setValue(protocol.getItemRewriter().handleItemToClient(event.user(), meta.value()));
            } else if (type == blockStateType) {
                int data = meta.value();
                meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
            } else if (type == optionalBlockStateType) {
                int data = meta.value();
                if (data != 0) {
                    meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
                }
            } else if (type == particleType) {
                rewriteParticle(event.user(), meta.value());
            } else if (type == particlesType) {
                Particle[] particles = meta.value();
                for (final Particle particle : particles) {
                    rewriteParticle(event.user(), particle);
                }
            } else if (type == optionalComponentType || type == componentType) {
                protocol.getTranslatableRewriter().processTag(event.user(), meta.value());
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

    protected PacketHandler getTrackerHandler(EntityType entityType, Type<? extends Number> intType) {
        return wrapper -> tracker(wrapper.user()).addEntity((int) wrapper.get(intType, 0), entityType);
    }

    protected PacketHandler getDimensionHandler(int index) {
        return wrapper -> {
            ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
            int dimensionId = wrapper.get(Types.INT, index);
            clientWorld.setEnvironment(dimensionId);
        };
    }
}
