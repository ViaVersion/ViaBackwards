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

import nl.matsv.viabackwards.api.rewriters.SoundIdRewriter;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.Protocol1_9To1_10;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueTransformer;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class SoundPackets extends SoundIdRewriter<Protocol1_9To1_10> {
    protected static ValueTransformer<Float, Short> toOldPitch = new ValueTransformer<Float, Short>(Type.UNSIGNED_BYTE) {
        public Short transform(PacketWrapper packetWrapper, Float inputValue) throws Exception {
            return (short) Math.round(inputValue * 63.5F);
        }
    };

    @Override
    protected void registerPackets(Protocol1_9To1_10 protocol) {
        // Named sound effect
        protocol.registerOutgoing(State.PLAY, 0x19, 0x19, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // 0 - Sound name
                map(Type.VAR_INT); // 1 - Sound Category
                map(Type.INT); // 2 - x
                map(Type.INT); // 3 - y
                map(Type.INT); // 4 - z
                map(Type.FLOAT); // 5 - Volume
                map(Type.FLOAT, toOldPitch); // 6 - Pitch
            }
        });

        // Sound effect
        protocol.registerOutgoing(State.PLAY, 0x46, 0x46, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Sound name
                map(Type.VAR_INT); // 1 - Sound Category
                map(Type.INT); // 2 - x
                map(Type.INT); // 3 - y
                map(Type.INT); // 4 - z
                map(Type.FLOAT); // 5 - Volume
                map(Type.FLOAT, toOldPitch); // 6 - Pitch

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int oldId = wrapper.get(Type.VAR_INT, 0);
                        int newId = handleSounds(oldId);
                        if (newId == -1)
                            wrapper.cancel();
                        else {
                            if (hasPitch(oldId))
                                wrapper.set(Type.UNSIGNED_BYTE, 0, (short) Math.round(handlePitch(oldId) * 63.5F));
                            wrapper.set(Type.VAR_INT, 0, newId);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        rewriteSound(24, -1); // Enchantment table sound

        // Husk
        rewriteSound(249, 400); // Husk -> Zombie ambient
        rewriteSound(250, 404); // Husk -> Zombie death
        rewriteSound(251, 405); // Husk -> Zombie hurt
        rewriteSound(252, 407); // Husk -> Zombie step

        // Polar bear
        rewriteSound(301, 400, .6F); // Polar bear ambient
        rewriteSound(302, 400, 1.9F); // Polar baby bear ambient
        rewriteSound(303, 404, .7F); // Polar bear death
        rewriteSound(304, 320, .6F); // Polar bear hurt
        rewriteSound(305, 241, .6F); // Polar bear step
        rewriteSound(306, 393, 1.2F); // Polar bear warning

        // Stray
        rewriteSound(365, 331); // Stray -> Skeleton ambient
        rewriteSound(366, 332); // Stray -> Skeleton death
        rewriteSound(367, 333); // Stray -> Skeleton hurt
        rewriteSound(368, 335); // Stray -> Skeleton step

        // Wither skeleton
        rewriteSound(387, 331); // Wither skeleton -> Skeleton ambient
        rewriteSound(388, 332); // Wither skeleton -> Skeleton death
        rewriteSound(389, 333); // Wither skeleton -> Skeleton hurt
        rewriteSound(390, 335); // Wither skeleton -> Skeleton step
    }
}
