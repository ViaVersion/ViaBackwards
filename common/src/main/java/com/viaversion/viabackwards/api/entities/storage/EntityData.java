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

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.util.ComponentUtil;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EntityData {
    private final BackwardsProtocol<?, ?, ?, ?> protocol;
    private final int id;
    private final int replacementId;
    private final String key;
    private ComponentType componentType = ComponentType.NONE;
    private MetaCreator defaultMeta;

    public EntityData(BackwardsProtocol<?, ?, ?, ?> protocol, EntityType type, int replacementId) {
        this(protocol, type.name(), type.getId(), replacementId);
    }

    public EntityData(BackwardsProtocol<?, ?, ?, ?> protocol, String key, int id, int replacementId) {
        this.protocol = protocol;
        this.id = id;
        this.replacementId = replacementId;
        this.key = key.toLowerCase(Locale.ROOT);
    }

    public EntityData jsonName() {
        this.componentType = ComponentType.JSON;
        return this;
    }

    public EntityData tagName() {
        this.componentType = ComponentType.TAG;
        return this;
    }

    public EntityData plainName() {
        this.componentType = ComponentType.PLAIN;
        return this;
    }

    public EntityData spawnMetadata(MetaCreator handler) {
        this.defaultMeta = handler;
        return this;
    }

    public boolean hasBaseMeta() {
        return this.defaultMeta != null;
    }

    public int typeId() {
        return id;
    }

    /**
     * @return custom mobname, can be either a String or a JsonElement
     */
    public @Nullable Object entityName() {
        if (componentType == ComponentType.NONE) {
            return null;
        }

        final String name = protocol.getMappingData().mappedEntityName(key);
        if (name == null) {
            return null;
        }

        if (componentType == ComponentType.JSON) {
            return ComponentUtil.legacyToJson(name);
        } else if (componentType == ComponentType.TAG) {
            return new StringTag(name);
        }
        return name;
    }

    public int replacementId() {
        return replacementId;
    }

    public @Nullable MetaCreator defaultMeta() {
        return defaultMeta;
    }

    public boolean isObjectType() {
        return false;
    }

    public int objectData() {
        return -1;
    }

    @Override
    public String toString() {
        return "EntityData{" +
                "id=" + id +
                ", mobName='" + key + '\'' +
                ", replacementId=" + replacementId +
                ", defaultMeta=" + defaultMeta +
                '}';
    }

    @FunctionalInterface
    public interface MetaCreator {

        void createMeta(WrappedMetadata storage);
    }

    private enum ComponentType {
        PLAIN,
        JSON,
        TAG,
        NONE
    }
}
