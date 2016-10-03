/*
 *
 *     Copyright (C) 2016 Matsv
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.matsv.viabackwards.protocol.protocol1_9_4to1_10;

import lombok.Getter;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.storage.EntityTracker;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets.BlockItemPackets;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets.ChangedPackets;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets.EntityPackets;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets.SoundPackets;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

@Getter
public class Protocol1_9To1_10 extends BackwardsProtocol {
    private EntityPackets entityPackets; // Required for the item rewriter

    protected void registerPackets() {
        new ChangedPackets().register(this);
        new SoundPackets().register(this);
        (entityPackets = new EntityPackets()).register(this);
        new BlockItemPackets().register(this);
    }

    public void init(UserConnection user) {
        // Register ClientWorld
        if (!user.has(ClientWorld.class))
            user.put(new ClientWorld(user));

        // Register EntityTracker if it doesn't exist yet.
        if (!user.has(EntityTracker.class))
            user.put(new EntityTracker(user));
    }
}
