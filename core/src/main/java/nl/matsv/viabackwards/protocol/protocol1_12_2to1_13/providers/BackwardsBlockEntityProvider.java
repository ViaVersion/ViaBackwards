/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.*;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.platform.providers.Provider;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BackwardsBlockEntityProvider implements Provider {
    private final Map<String, BackwardsBlockEntityProvider.BackwardsBlockEntityHandler> handlers = new ConcurrentHashMap<>();

    public BackwardsBlockEntityProvider() {
        handlers.put("minecraft:flower_pot", new FlowerPotHandler()); // TODO requires special treatment, manually send
        handlers.put("minecraft:bed", new BedHandler());
        handlers.put("minecraft:banner", new BannerHandler());
        handlers.put("minecraft:skull", new SkullHandler());
        handlers.put("minecraft:mob_spawner", new SpawnerHandler());
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
        String id = (String) tag.get("id").getValue();
        BackwardsBlockEntityHandler handler = handlers.get(id);
        if (handler == null) {
            if (Via.getManager().isDebug()) {
                ViaBackwards.getPlatform().getLogger().warning("Unhandled BlockEntity " + id + " full tag: " + tag);
            }
            return tag;
        }

        BackwardsBlockStorage storage = user.get(BackwardsBlockStorage.class);

        if (!storage.contains(position)) {
            if (Via.getManager().isDebug()) {
                ViaBackwards.getPlatform().getLogger().warning("Handled BlockEntity does not have a stored block :( " + id + " full tag: " + tag);
            }
            return tag;
        }

        return handler.transform(user, storage.get(position), tag);
    }

    /**
     * Transform blocks to block entities!
     *
     * @param user     The user
     * @param position The position of the block entity
     * @param id       The block entity id
     */
    public CompoundTag transform(UserConnection user, Position position, String id) throws Exception {
        CompoundTag tag = new CompoundTag("");
        tag.put(new StringTag("id", id));
        tag.put(new IntTag("x", Math.toIntExact(position.getX())));
        tag.put(new IntTag("y", Math.toIntExact(position.getY())));
        tag.put(new IntTag("z", Math.toIntExact(position.getZ())));

        return this.transform(user, position, tag);
    }

    public interface BackwardsBlockEntityHandler {
        CompoundTag transform(UserConnection user, int blockId, CompoundTag tag);
    }
}
