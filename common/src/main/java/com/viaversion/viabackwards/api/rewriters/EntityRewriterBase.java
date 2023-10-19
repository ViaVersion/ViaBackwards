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
package com.viaversion.viabackwards.api.rewriters;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.entities.storage.EntityData;
import com.viaversion.viabackwards.api.entities.storage.WrappedMetadata;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.Int2IntMapMappings;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.rewriter.EntityRewriter;
import com.viaversion.viaversion.rewriter.meta.MetaHandlerEvent;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Entity rewriter base class.
 *
 * @see com.viaversion.viabackwards.api.rewriters.EntityRewriter
 * @see LegacyEntityRewriter
 */
public abstract class EntityRewriterBase<C extends ClientboundPacketType, T extends BackwardsProtocol<C, ?, ?, ?>> extends EntityRewriter<C, T> {
    private final Int2ObjectMap<EntityData> entityDataMappings = new Int2ObjectOpenHashMap<>();
    private final MetaType displayNameMetaType;
    private final MetaType displayVisibilityMetaType;
    private final int displayNameIndex;
    private final int displayVisibilityIndex;

    EntityRewriterBase(T protocol, MetaType displayNameMetaType, int displayNameIndex,
                       MetaType displayVisibilityMetaType, int displayVisibilityIndex) {
        super(protocol, false);
        this.displayNameMetaType = displayNameMetaType;
        this.displayNameIndex = displayNameIndex;
        this.displayVisibilityMetaType = displayVisibilityMetaType;
        this.displayVisibilityIndex = displayVisibilityIndex;
    }

    @Override
    public void handleMetadata(int entityId, List<Metadata> metadataList, UserConnection connection) {
        final TrackedEntity entity = tracker(connection).entity(entityId);
        final boolean initialMetadata = !(entity != null && entity.hasSentMetadata());

        super.handleMetadata(entityId, metadataList, connection);

        if (entity == null) {
            return; // Don't handle untracked entities - basically always the fault of a plugin sending virtual entities through concurrency-unsafe handling
        }

        // Set the mapped entity name if there is no custom name set already
        final EntityData entityData = entityDataForType(entity.entityType());
        if (entityData != null && entityData.mobName() != null) {
            final Metadata displayName = getMeta(displayNameIndex, metadataList);
            if (initialMetadata) {
                if (displayName == null) {
                    // Add it as new metadata
                    metadataList.add(new Metadata(displayNameIndex, displayNameMetaType, entityData.mobName()));
                    addDisplayVisibilityMeta(metadataList);
                } else if (displayName.getValue() == null || displayName.getValue().toString().isEmpty()) {
                    // Overwrite the existing null/empty display name
                    displayName.setValue(entityData.mobName());
                    addDisplayVisibilityMeta(metadataList);
                }
            } else if (displayName != null && (displayName.getValue() == null || displayName.getValue().toString().isEmpty())) {
                // Overwrite null/empty display name
                displayName.setValue(entityData.mobName());
                addDisplayVisibilityMeta(metadataList);
            }
        }

        // Add any other extra meta for mapped entities
        if (entityData != null && entityData.hasBaseMeta() && initialMetadata) {
            entityData.defaultMeta().createMeta(new WrappedMetadata(metadataList));
        }
    }

    private void addDisplayVisibilityMeta(List<Metadata> metadataList) {
        if (ViaBackwards.getConfig().alwaysShowOriginalMobName()) {
            removeMeta(displayVisibilityIndex, metadataList);
            metadataList.add(new Metadata(displayVisibilityIndex, displayVisibilityMetaType, true));
        }
    }

    protected @Nullable Metadata getMeta(int metaIndex, List<Metadata> metadataList) {
        for (Metadata metadata : metadataList) {
            if (metadata.id() == metaIndex) {
                return metadata;
            }
        }
        return null;
    }

    protected void removeMeta(int metaIndex, List<Metadata> metadataList) {
        metadataList.removeIf(meta -> meta.id() == metaIndex);
    }

    protected boolean hasData(EntityType type) {
        return entityDataMappings.containsKey(type.getId());
    }

    protected @Nullable EntityData entityDataForType(EntityType type) {
        return entityDataMappings.get(type.getId());
    }

    protected @Nullable StoredEntityData storedEntityData(MetaHandlerEvent event) {
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
    protected EntityData mapEntityTypeWithData(EntityType type, EntityType mappedType) {
        Preconditions.checkArgument(type.getClass() == mappedType.getClass(), "Both entity types need to be of the same class");

        // Already rewrite the id here
        int mappedReplacementId = newEntityId(mappedType.getId());
        EntityData data = new EntityData(protocol, type, mappedReplacementId);
        mapEntityType(type.getId(), mappedReplacementId);
        entityDataMappings.put(type.getId(), data);
        return data;
    }

    /**
     * Maps entity ids based on the enum constant's names.
     *
     * @param oldTypes     entity types of the higher version
     * @param newTypeClass entity types enum class of the lower version
     * @param <E>          new enum type
     */
    @Override
    public <E extends Enum<E> & EntityType> void mapTypes(EntityType[] oldTypes, Class<E> newTypeClass) {
        if (typeMappings == null) {
            typeMappings = Int2IntMapMappings.of();
        }
        for (EntityType oldType : oldTypes) {
            try {
                E newType = Enum.valueOf(newTypeClass, oldType.name());
                typeMappings.setNewId(oldType.getId(), newType.getId());
            } catch (IllegalArgumentException ignored) {
                // Don't warn
            }
        }
    }

    public void registerMetaTypeHandler(
            @Nullable MetaType itemType,
            @Nullable MetaType blockStateType,
            @Nullable MetaType optionalBlockStateType,
            @Nullable MetaType particleType,
            @Nullable MetaType componentType,
            @Nullable MetaType optionalComponentType
    ) {
        filter().handler((event, meta) -> {
            MetaType type = meta.metaType();
            if (type == itemType) {
                protocol.getItemRewriter().handleItemToClient(meta.value());
            } else if (type == blockStateType) {
                int data = meta.value();
                meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
            } else if (type == optionalBlockStateType) {
                int data = meta.value();
                if (data != 0) {
                    meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
                }
            } else if (type == particleType) {
                rewriteParticle(meta.value());
            } else if (type == optionalComponentType || type == componentType) {
                JsonElement text = meta.value();
                if (text != null) {
                    protocol.getTranslatableRewriter().processText(text);
                }
            }
        });
    }

    // ONLY TRACKS, DOESN'T REWRITE IDS
    protected PacketHandler getTrackerHandler(Type<? extends Number> intType, int typeIndex) {
        return wrapper -> {
            Number id = wrapper.get(intType, typeIndex);
            tracker(wrapper.user()).addEntity(wrapper.get(Type.VAR_INT, 0), typeFromId(id.intValue()));
        };
    }

    protected PacketHandler getTrackerHandler() {
        return getTrackerHandler(Type.VAR_INT, 1);
    }

    protected PacketHandler getTrackerHandler(EntityType entityType, Type<? extends Number> intType) {
        return wrapper -> tracker(wrapper.user()).addEntity((int) wrapper.get(intType, 0), entityType);
    }

    protected PacketHandler getDimensionHandler(int index) {
        return wrapper -> {
            ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
            int dimensionId = wrapper.get(Type.INT, index);
            clientWorld.setEnvironment(dimensionId);
        };
    }
}
