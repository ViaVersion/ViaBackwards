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
import com.viaversion.viaversion.libs.gson.JsonIOException;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonSyntaxException;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.libs.opennbt.tag.io.NBTIO;
import com.viaversion.viaversion.libs.opennbt.tag.io.TagReader;
import com.viaversion.viaversion.util.GsonUtil;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VBMappingDataLoader {

    private static final TagReader<CompoundTag> TAG_READER = NBTIO.reader(CompoundTag.class).named();

    public static final VBMappingDataLoader INSTANCE = new VBMappingDataLoader();

    public @Nullable CompoundTag loadNBT(final String name) {
        final InputStream resource = getResource(name);
        if (resource == null) {
            return null;
        }

        try (final InputStream stream = resource) {
            return TAG_READER.read(stream);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
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
            final CompoundTag fileData = TAG_READER.read(file.toPath(), false);
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

    public JsonObject loadData(final String name) {
        try (final InputStream stream = getResource(name)) {
            if (stream == null) return null;
            return GsonUtil.getGson().fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonObject loadFromDataDir(final String name) {
        final File file = new File(ViaBackwards.getPlatform().getDataFolder(), name);
        if (!file.exists()) {
            return loadData(name);
        }

        // Load the file from the platform's directory if present
        try (final FileReader reader = new FileReader(file)) {
            return GsonUtil.getGson().fromJson(reader, JsonObject.class);
        } catch (final JsonSyntaxException e) {
            ViaBackwards.getPlatform().getLogger().warning(name + " is badly formatted!");
            e.printStackTrace();
            ViaBackwards.getPlatform().getLogger().warning("Falling back to resource's file!");
            return loadData(name);
        } catch (final IOException | JsonIOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the resource as an input stream.
     * Platforms can override this method to load resources from their own directories by creating a new instance of this class.
     */
    public @Nullable InputStream getResource(final String name) {
        return VBMappingDataLoader.class.getClassLoader().getResourceAsStream("assets/viabackwards/data/" + name);
    }
}
