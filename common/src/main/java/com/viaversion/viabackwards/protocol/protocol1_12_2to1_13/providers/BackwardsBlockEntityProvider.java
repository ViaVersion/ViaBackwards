/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.providers;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.BannerHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.BedHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.FlowerPotHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.PistonHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.SkullHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.SpawnerHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.platform.providers.Provider;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.HashMap;
import java.util.Map;

public class BackwardsBlockEntityProvider implements Provider {
    private final Map<String, BackwardsBlockEntityProvider.BackwardsBlockEntityHandler> handlers = new HashMap<>();

    public BackwardsBlockEntityProvider() {
        handlers.put("minecraft:flower_pot", new FlowerPotHandler()); // TODO requires special treatment, manually send
        handlers.put("minecraft:bed", new BedHandler());
        handlers.put("minecraft:banner", new BannerHandler());
        handlers.put("minecraft:skull", new SkullHandler());
        handlers.put("minecraft:mob_spawner", new SpawnerHandler());
        handlers.put("minecraft:piston", new PistonHandler());
    }

    /**
     * Check if a block entity handler is present
     *
     * @param key Id of the NBT data ex: minecraft:bed
     * @return true if present
     */
    public boolean isHandled(String key) {
        return handlers.containsKey(key);
    }

    /**
     * Transform blocks to block entities!
     *
     * @param user     The user
     * @param position The position of the block entity
     * @param tag      The block entity tag
     */
    public CompoundTag transform(UserConnection user, Position position, CompoundTag tag) throws Exception {
        final Tag idTag = tag.get("id");
        if (!(idTag instanceof StringTag)) {
            return tag;
        }

        String id = (String) idTag.getValue();
        BackwardsBlockEntityHandler handler = handlers.get(id);
        if (handler == null) {
            if (Via.getManager().isDebug()) {
                ViaBackwards.getPlatform().getLogger().warning("Unhandled BlockEntity " + id + " full tag: " + tag);
            }
            return tag;
        }

        BackwardsBlockStorage storage = user.get(BackwardsBlockStorage.class);
        Integer blockId = storage.get(position);
        if (blockId == null) {
            if (Via.getManager().isDebug()) {
                ViaBackwards.getPlatform().getLogger().warning("Handled BlockEntity does not have a stored block :( " + id + " full tag: " + tag);
            }
            return tag;
        }

        return handler.transform(user, blockId, tag);
    }

    /**
     * Transform blocks to block entities!
     *
     * @param user     The user
     * @param position The position of the block entity
     * @param id       The block entity id
     */
    public CompoundTag transform(UserConnection user, Position position, String id) throws Exception {
        CompoundTag tag = new CompoundTag();
        tag.put("id", new StringTag(id));
        tag.put("x", new IntTag(Math.toIntExact(position.getX())));
        tag.put("y", new IntTag(Math.toIntExact(position.getY())));
        tag.put("z", new IntTag(Math.toIntExact(position.getZ())));

        return this.transform(user, position, tag);
    }

    public interface BackwardsBlockEntityHandler {
        CompoundTag transform(UserConnection user, int blockId, CompoundTag tag);
    }
}
