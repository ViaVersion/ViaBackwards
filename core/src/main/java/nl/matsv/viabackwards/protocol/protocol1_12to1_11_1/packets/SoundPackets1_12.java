/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12to1_11_1.packets;

import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12to1_11_1.Protocol1_11_1To1_12;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class SoundPackets1_12 extends SoundRewriter<Protocol1_11_1To1_12> {
    @Override
    protected void registerPackets(Protocol1_11_1To1_12 protocol) {
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
        protocol.registerOutgoing(State.PLAY, 0x48, 0x46, new PacketRemapper() {
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
        added(26, -1); // block.end_portal.spawn
        added(27, -1); // block.end_portal_frame.fill

        added(72, -1); // block.note.bell
        added(73, -1); // block.note.chime
        added(74, -1); // block.note.flute
        added(75, -1); // block.note.guitar
        added(80, -1); // block.note.xylophone

        added(150, -1); // entity.boat.paddle_land
        added(151, -1); // entity.boat.paddle_water

        added(152, -1); // entity.bobber.retrieve

        added(195, -1); // entity.endereye.death

        added(274, -1); // entity.illusion_illager.ambient
        added(275, -1); // entity.illusion_illager.cast_spell
        added(276, -1); // entity.illusion_illager.death
        added(277, -1); // entity.illusion_illager.hurt
        added(278, -1); // entity.illusion_illager.mirror_move
        added(279, -1); // entity.illusion_illager.prepare_blindness
        added(280, -1); // entity.illusion_illager.prepare_mirror

        added(319, -1); // entity.parrot.ambient
        added(320, -1); // entity.parrot.death
        added(321, -1); // entity.parrot.eat
        added(322, -1); // entity.parrot.fly
        added(323, -1); // entity.parrot.hurt
        added(324, -1); // entity.parrot.imitate.blaze
        added(325, -1); // entity.parrot.imitate.creeper
        added(326, -1); // entity.parrot.imitate.elder_guardian
        added(327, -1); // entity.parrot.imitate.enderdragon
        added(328, -1); // entity.parrot.imitate.enderman
        added(329, -1); // entity.parrot.imitate.endermite
        added(330, -1); // entity.parrot.imitate.evocation_illager
        added(331, -1); // entity.parrot.imitate.ghast
        added(332, -1); // entity.parrot.imitate.husk
        added(333, -1); // entity.parrot.imitate.illusion_illager
        added(334, -1); // entity.parrot.imitate.magmacube
        added(335, -1); // entity.parrot.imitate.polar_bear
        added(336, -1); // entity.parrot.imitate.shulker
        added(337, -1); // entity.parrot.imitate.silverfish
        added(338, -1); // entity.parrot.imitate.skeleton
        added(339, -1); // entity.parrot.imitate.slime
        added(340, -1); // entity.parrot.imitate.spider
        added(341, -1); // entity.parrot.imitate.stray
        added(342, -1); // entity.parrot.imitate.vex
        added(343, -1); // entity.parrot.imitate.vindication_illager
        added(344, -1); // entity.parrot.imitate.witch
        added(345, -1); // entity.parrot.imitate.wither
        added(346, -1); // entity.parrot.imitate.wither_skeleton
        added(347, -1); // entity.parrot.imitate.wolf
        added(348, -1); // entity.parrot.imitate.zombie
        added(349, -1); // entity.parrot.imitate.zombie_pigman
        added(350, -1); // entity.parrot.imitate.zombie_villager
        added(351, -1); // entity.parrot.step

        added(368, -1); // entity.player.hurt_drown
        added(369, -1); // entity.player.hurt_on_fire

        added(544, -1); // ui.toast.in
        added(545, -1); // ui.toast.out
        added(546, -1); // ui.toast.challenge_complete
    }
}
