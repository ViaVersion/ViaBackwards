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

package com.viaversion.viabackwards.api.entities.storage;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ChatRewriter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

public class EntityData {
    private final BackwardsProtocol<?, ?, ?, ?> protocol;
    private final int id;
    private final int replacementId;
    private final String key;
    private NameVisibility nameVisibility = NameVisibility.NONE;
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
        this.nameVisibility = NameVisibility.JSON;
        return this;
    }

    public EntityData plainName() {
        this.nameVisibility = NameVisibility.PLAIN;
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
    public @Nullable Object mobName() {
        if (nameVisibility == NameVisibility.NONE) {
            return null;
        }

        String name = protocol.getMappingData().mappedEntityName(key);
        if (name == null) {
            ViaBackwards.getPlatform().getLogger().warning("Entity name for " + key + " not found in protocol " + protocol.getClass().getSimpleName());
            name = key;
        }
        return nameVisibility == NameVisibility.JSON ? ChatRewriter.legacyTextToJson(name) : name;
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

    private enum NameVisibility {
        PLAIN,
        JSON,
        NONE
    }
}
