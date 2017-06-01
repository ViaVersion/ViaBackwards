/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_10to1_11.packets;

import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class SoundPackets extends SoundRewriter<Protocol1_10To1_11> {
    @Override
    protected void registerPackets(Protocol1_10To1_11 protocol) {
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
                map(Type.FLOAT); // 6 - Pitch
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
                map(Type.FLOAT); // 6 - Pitch

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int oldId = wrapper.get(Type.VAR_INT, 0);
                        int newId = handleSounds(oldId);
                        if (newId == -1)
                            wrapper.cancel();
                        else {
                            if (hasPitch(oldId))
                                wrapper.set(Type.FLOAT, 0, handlePitch(oldId));
                            wrapper.set(Type.VAR_INT, 0, newId);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        // TODO find good replacement sounds
        // Automatically generated from PAaaS
        added(85, 121, 0.5f); // block.shulker_box.close -> block.wooden_trapdoor.close
        added(86, 122, 0.5f); // block.shulker_box.open -> block.wooden_trapdoor.open

        added(176, 227); // entity.elder_guardian.flop -> entity.guardian.flop

        removed(196); // entity.experience_orb.touch

        added(197, -1); // entity.evocation_fangs.attack
        added(198, -1); // entity.evocation_illager.ambient
        added(199, -1); // entity.evocation_illager.cast_spell
        added(200, -1); // entity.evocation_illager.death
        added(201, -1); // entity.evocation_illager.hurt
        added(202, -1); // entity.evocation_illager.prepare_attack
        added(203, -1); // entity.evocation_illager.prepare_summon
        added(204, -1); // entity.evocation_illager.prepare_wololo

        added(279, -1); // entity.llama.ambient
        added(280, -1); // entity.llama.angry
        added(281, -1); // entity.llama.chest
        added(282, -1); // entity.llama.death
        added(283, -1); // entity.llama.eat
        added(284, -1); // entity.llama.hurt
        added(285, -1); // entity.llama.spit
        added(286, -1); // entity.llama.step
        added(287, -1); // entity.llama.swag
        added(296, -1); // entity.mule.chest

        added(390, -1); // entity.vex.ambient
        added(391, -1); // entity.vex.charge
        added(392, -1); // entity.vex.death
        added(393, -1); // entity.vex.hurt

        added(400, -1); // entity.vindication_illager.ambient
        added(401, -1); // entity.vindication_illager.death
        added(402, -1); // entity.vindication_illager.hurt

        added(450, -1); // item.armor.equip_elytra
        added(455, -1); // item.bottle.empty
        added(470, -1); // item.totem.use
    }
}
