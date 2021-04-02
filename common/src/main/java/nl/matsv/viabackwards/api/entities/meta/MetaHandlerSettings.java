/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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

package nl.matsv.viabackwards.api.entities.meta;

import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import org.checkerframework.checker.nullness.qual.Nullable;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;

public class MetaHandlerSettings {
    private EntityType filterType;
    private boolean filterFamily;
    private int filterIndex = -1;
    private MetaHandler handler;

    public MetaHandlerSettings filter(EntityType type) {
        return filter(type, filterFamily, filterIndex);
    }

    public MetaHandlerSettings filter(EntityType type, boolean filterFamily) {
        return filter(type, filterFamily, filterIndex);
    }

    public MetaHandlerSettings filter(int index) {
        return filter(filterType, filterFamily, index);
    }

    public MetaHandlerSettings filter(EntityType type, int index) {
        return filter(type, filterFamily, index);
    }

    public MetaHandlerSettings filter(EntityType type, boolean filterFamily, int index) {
        this.filterType = type;
        this.filterFamily = filterFamily;
        this.filterIndex = index;
        return this;
    }

    public void handle(@Nullable MetaHandler handler) {
        this.handler = handler;
    }

    public void handleIndexChange(final int newIndex) {
        handle(e -> {
            Metadata data = e.getData();
            data.setId(newIndex);
            return data;
        });
    }

    public void removed() {
        handle(e -> {
            throw RemovedValueException.EX;
        });
    }

    public boolean hasHandler() {
        return handler != null;
    }

    public boolean hasType() {
        return filterType != null;
    }

    public boolean hasIndex() {
        return filterIndex > -1;
    }

    public boolean isFilterFamily() {
        return filterFamily;
    }

    /**
     * Returns true if the metadata should be handled by this object.
     *
     * @param type     entity type
     * @param metadata metadata
     * @return true if gucci
     */
    public boolean isGucci(EntityType type, Metadata metadata) {
        if (!hasHandler()) return false;
        if (hasType() && (filterFamily ? !type.isOrHasParent(filterType) : !filterType.is(type))) {
            return false;
        }
        return !hasIndex() || metadata.getId() == filterIndex;
    }

    public EntityType getFilterType() {
        return filterType;
    }

    public int getFilterIndex() {
        return filterIndex;
    }

    public @Nullable MetaHandler getHandler() {
        return handler;
    }

    @Override
    public String toString() {
        return "MetaHandlerSettings{" +
                "filterType=" + filterType +
                ", filterFamily=" + filterFamily +
                ", filterIndex=" + filterIndex +
                ", handler=" + handler +
                '}';
    }
}
