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

package com.viaversion.viabackwards.protocol.v1_13to1_12_2.provider;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.block_entity_handlers.BannerHandler;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.block_entity_handlers.BedHandler;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.block_entity_handlers.FlowerPotHandler;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.block_entity_handlers.PistonHandler;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.block_entity_handlers.SkullHandler;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.block_entity_handlers.SpawnerHandler;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.BackwardsBlockStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.platform.providers.Provider;
import com.viaversion.viaversion.util.Key;
import java.util.HashMap;
import java.util.Map;

public class BackwardsBlockEntityProvider implements Provider {
    private final Map<String, BackwardsBlockEntityProvider.BackwardsBlockEntityHandler> handlers = new HashMap<>();

    public BackwardsBlockEntityProvider() {
        handlers.put("flower_pot", new FlowerPotHandler()); // TODO requires special treatment, manually send
        handlers.put("bed", new BedHandler());
        handlers.put("banner", new BannerHandler());
        handlers.put("skull", new SkullHandler());
        handlers.put("mob_spawner", new SpawnerHandler());
        handlers.put("piston", new PistonHandler());
    }

    /**
     * Check if a block entity handler is present
     *
     * @param key Id of the NBT data ex: minecraft:bed
     * @return true if present
     */
    public boolean isHandled(String key) {
        return handlers.containsKey(Key.stripMinecraftNamespace(key));
    }

    /**
     * Transform blocks to block entities!
     *
     * @param user     The user
     * @param position The position of the block entity
     * @param tag      The block entity tag
     */
    public CompoundTag transform(UserConnection user, BlockPosition position, CompoundTag tag) {
        final StringTag idTag = tag.getStringTag("id");
        if (idTag == null) {
            return tag;
        }

        String id = idTag.getValue();
        BackwardsBlockEntityHandler handler = handlers.get(Key.stripMinecraftNamespace(id));
        if (handler == null) {
            return tag;
        }

        BackwardsBlockStorage storage = user.get(BackwardsBlockStorage.class);
        Integer blockId = storage.get(position);
        if (blockId == null) {
            return tag;
        }

        return handler.transform(blockId, tag);
    }

    /**
     * Transform blocks to block entities!
     *
     * @param user     The user
     * @param position The position of the block entity
     * @param id       The block entity id
     */
    public CompoundTag transform(UserConnection user, BlockPosition position, String id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putInt("x", Math.toIntExact(position.x()));
        tag.putInt("y", Math.toIntExact(position.y()));
        tag.putInt("z", Math.toIntExact(position.z()));

        return this.transform(user, position, tag);
    }

    @FunctionalInterface
    public interface BackwardsBlockEntityHandler {

        CompoundTag transform(int blockId, CompoundTag tag);
    }
}
