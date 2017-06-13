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

public class SoundPackets1_11 extends SoundRewriter<Protocol1_10To1_11> {
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
                                wrapper.set(Type.FLOAT, 1, handlePitch(oldId));
                            wrapper.set(Type.VAR_INT, 0, newId);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        // Sound replacements, suggestions are always welcome
        // Automatically generated from PAaaS
        added(85, 121, 0.5f); // block.shulker_box.close -> block.wooden_trapdoor.close
        added(86, 122, 0.5f); // block.shulker_box.open -> block.wooden_trapdoor.open

        added(176, 227); // entity.elder_guardian.flop -> entity.guardian.flop

        removed(196); // entity.experience_orb.touch

        added(197, 402, 1.8f); // entity.evocation_fangs.attack -> entity.zombie.attack_iron_door
        added(198, 370, 0.4f); // entity.evocation_illager.ambient -> entity.villager.ambient
        added(199, 255, 1.3f); // entity.evocation_illager.cast_spell -> entity.irongolem.hurt
        added(200, 418, 1.3f); // entity.evocation_illager.death -> entity.zombie_villager.death
        added(201, 372, 1.3f); // entity.evocation_illager.hurt -> entity.villager.hurt
        added(202, 137, 0.8f); // entity.evocation_illager.prepare_attack -> entity.elder_guardian.curse
        added(203, 78, 2f); // entity.evocation_illager.prepare_summon -> block.portal.trigger
        added(204, 376, 0.6f); // entity.evocation_illager.prepare_wololo -> entity.witch.ambient

        added(279, 230, 1.5f); // entity.llama.ambient -> entity.horse.ambient
        added(280, 231, 1.6f); // entity.llama.angry -> entity.horse.angry
        added(281, 164); // entity.llama.chest -> entity.donkey.chest
        added(282, 165, 1.2f); // entity.llama.death -> entity.donkey.death
        added(283, 235, 1.1f); // entity.llama.eat -> entity.horse.eat
        added(284, 166); // entity.llama.hurt -> entity.donkey.hurt
        added(285, 323, 1.7f); // entity.llama.spit -> entity.shulker.shoot
        added(286, 241, 0.8f); // entity.llama.step -> entity.horse.step
        added(287, 423, 0.5f); // entity.llama.swag -> item.armor.equip_generic
        added(296, 164); // entity.mule.chest -> entity.donkey.chest

        added(390, 233, 0.1f); // entity.vex.ambient -> entity.horse.breathe
        added(391, 168, 2f); // entity.vex.charge -> entity.elder_guardian.ambient
        added(392, 144, 0.5f); // entity.vex.death -> entity.cat.death
        added(393, 146, 2f); // entity.vex.hurt -> entity.cat.hurt

        added(400, 370, 0.7f); // entity.vindication_illager.ambient -> entity.villager.ambient
        added(401, 371, 0.8f); // entity.vindication_illager.death -> entity.villager.death
        added(402, 372, 0.7f); // entity.vindication_illager.hurt -> entity.villager.hurt

        added(450, 423, 1.1f); // item.armor.equip_elytra -> item.armor.equip_generic
        added(455, 427, 1.1f); // item.bottle.empty -> item.bottle.fill
        added(470, 2, 0.5f); // item.totem.use -> block.anvil.destroy
    }
}
