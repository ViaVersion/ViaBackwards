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
package com.viaversion.viabackwards.api.data;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsMappingDataLoader extends MappingDataLoader {

    public static final BackwardsMappingDataLoader INSTANCE = new BackwardsMappingDataLoader(BackwardsMappingDataLoader.class, "assets/viabackwards/data/");

    public BackwardsMappingDataLoader(final Class<?> dataLoaderClass, final String dataPath) {
        super(dataLoaderClass, dataPath);
    }

    @Override
    public File getFile(final String name) {
        return new File(ViaBackwards.getPlatform().getDataFolder(), name);
    }

    /**
     * Returns nbt data from the plugin folder or packed assets.
     * If a file with the same name exists in the plugin folder, the data of the original packed tag will be merged with the file's tag.
     *
     * @param name name of the file
     * @return nbt data from the plugin folder or packed assets
     */
    public @Nullable CompoundTag loadNBTFromDir(final String name) {
        final CompoundTag packedData = loadNBT(name);

        final File file = new File(ViaBackwards.getPlatform().getDataFolder(), name);
        if (!file.exists()) {
            return packedData;
        }

        ViaBackwards.getPlatform().getLogger().info("Loading " + name + " from plugin folder");
        try {
            final CompoundTag fileData = MAPPINGS_READER.read(file.toPath(), false);
            return mergeTags(packedData, fileData);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CompoundTag mergeTags(final CompoundTag original, final CompoundTag extra) {
        for (final Map.Entry<String, Tag> entry : extra.entrySet()) {
            if (entry.getValue() instanceof CompoundTag) {
                // For compound tags, don't replace the entire tag
                final CompoundTag originalEntry = original.getCompoundTag(entry.getKey());
                if (originalEntry != null) {
                    mergeTags(originalEntry, (CompoundTag) entry.getValue());
                    continue;
                }
            }

            original.put(entry.getKey(), entry.getValue());
        }
        return original;
    }
}
