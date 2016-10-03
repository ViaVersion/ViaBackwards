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

package nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class ChangedPackets {

    public void register(BackwardsProtocol protocol) {
        /* ServerBound packets */

        // ResourcePack status
        protocol.registerIncoming(State.PLAY, 0x16, 0x16, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING, Type.NOTHING); // 0 - Hash
                map(Type.VAR_INT); // 1 - Result
            }
        });
    }
}
