/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data;

import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.BlockItemPackets1_13;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.Particle;

import java.util.List;

public class ParticleMapping {
    private static final ParticleData[] particles;

    static {
        ParticleHandler blockHandler = new ParticleHandler() {
            @Override
            public int[] rewrite(Protocol1_12_2To1_13 protocol, PacketWrapper wrapper) throws Exception {
                return rewrite(wrapper.read(Type.VAR_INT));
            }

            @Override
            public int[] rewrite(Protocol1_12_2To1_13 protocol, List<Particle.ParticleData> data) {
                return rewrite((int) data.get(0).getValue());
            }

            private int[] rewrite(int newType) {
                int blockType = BlockItemPackets1_13.toOldId(newType);

                int type = blockType >> 4;
                int meta = blockType & 15;
                return new int[]{type + (meta << 12)};
            }

            @Override
            public boolean isBlockHandler() {
                return true;
            }
        };

        particles = new ParticleData[]{
                rewrite(16), // (0->16)  minecraft:ambient_entity_effect -> mobSpellAmbient
                rewrite(20), // (1->20)  minecraft:angry_villager -> angryVillager
                rewrite(35), // (2->35)  minecraft:barrier -> barrier
                rewrite(37, blockHandler),
                // (3->37)  minecraft:block -> blockcrack
                rewrite(4),  // (4->4)   minecraft:bubble -> bubble
                rewrite(29), // (5->29)  minecraft:cloud -> cloud
                rewrite(9),  // (6->9)   minecraft:crit -> crit
                rewrite(44), // (7->44)  minecraft:damage_indicator -> damageIndicator‌
                rewrite(42), // (8->42)  minecraft:dragon_breath -> dragonbreath
                rewrite(19), // (9->19)  minecraft:dripping_lava -> dripLava
                rewrite(18), // (10->18) minecraft:dripping_water -> dripWater
                rewrite(30, new ParticleHandler() {
                    @Override
                    public int[] rewrite(Protocol1_12_2To1_13 protocol, PacketWrapper wrapper) throws Exception {
                        float r = wrapper.read(Type.FLOAT);
                        float g = wrapper.read(Type.FLOAT);
                        float b = wrapper.read(Type.FLOAT);
                        float scale = wrapper.read(Type.FLOAT);

                        wrapper.set(Type.FLOAT, 3, r); // 5 - Offset X index=3
                        wrapper.set(Type.FLOAT, 4, g); // 6 - Offset Y index=4
                        wrapper.set(Type.FLOAT, 5, b); // 7 - Offset Z index=5
                        wrapper.set(Type.FLOAT, 6, scale); // 8 - Particle Data index=6
                        wrapper.set(Type.INT, 1, 0); // 9 - Particle Count index=1 enable rgb particle

                        return null;
                    }

                    @Override
                    public int[] rewrite(Protocol1_12_2To1_13 protocol, List<Particle.ParticleData> data) {
                        return null;
                    }
                }),         // (11->30) minecraft:dust -> reddust
                rewrite(13), // (12->13) minecraft:effect -> spell
                rewrite(41), // (13->41) minecraft:elder_guardian -> mobappearance
                rewrite(10), // (14->10) minecraft:enchanted_hit -> magicCrit‌
                rewrite(25), // (15->25) minecraft:enchant -> enchantmenttable
                rewrite(43), // (16->43) minecraft:end_rod -> endRod
                rewrite(15), // (17->15) minecraft:entity_effect -> mobSpell
                rewrite(2),  // (18->2)  minecraft:explosion_emitter -> hugeexplosion
                rewrite(1),  // (19->1)  minecraft:explosion -> largeexplode
                rewrite(46, blockHandler),
                // (20->46) minecraft:falling_dust -> fallingdust
                rewrite(3),  // (21->3)  minecraft:firework -> fireworksSpark
                rewrite(6),  // (22->6)  minecraft:fishing -> wake
                rewrite(26), // (23->26) minecraft:flame -> flame
                rewrite(21), // (24->21) minecraft:happy_villager -> happyVillager
                rewrite(34), // (25->34) minecraft:heart -> heart
                rewrite(14), // (26->14) minecraft:instant_effect -> instantSpell
                rewrite(36, new ParticleHandler() {
                    @Override
                    public int[] rewrite(Protocol1_12_2To1_13 protocol, PacketWrapper wrapper) throws Exception {
                        return rewrite(protocol, wrapper.read(Type.FLAT_ITEM));
                    }

                    @Override
                    public int[] rewrite(Protocol1_12_2To1_13 protocol, List<Particle.ParticleData> data) {
                        return rewrite(protocol, (Item) data.get(0).getValue());
                    }

                    private int[] rewrite(Protocol1_12_2To1_13 protocol, Item newItem) {
                        Item item = protocol.getBlockItemPackets().handleItemToClient(newItem);
                        return new int[]{item.getIdentifier(), item.getData()};
                    }
                }),          // (27->36) minecraft:item -> iconcrack
                rewrite(33), // (28->33) minecraft:item_slime -> slime
                rewrite(31), // (29->31) minecraft:item_snowball -> snowballpoof
                rewrite(12), // (30->12) minecraft:large_smoke -> largesmoke
                rewrite(27), // (31->27) minecraft:lava -> lava
                rewrite(22), // (32->22) minecraft:mycelium -> townaura
                rewrite(23), // (33->23) minecraft:note -> note
                rewrite(0),  // (34->0)  minecraft:poof -> explode
                rewrite(24), // (35->24) minecraft:portal -> portal
                rewrite(39), // (36->39) minecraft:rain -> droplet
                rewrite(11), // (37->11) minecraft:smoke -> smoke
                rewrite(48), // (38->48) minecraft:spit -> spit
                rewrite(12), // (39->-1) minecraft:squid_ink -> squid_ink -> large_smoke
                rewrite(45), // (40->45) minecraft:sweep_attack -> sweepAttack‌
                rewrite(47), // (41->47) minecraft:totem_of_undying -> totem
                rewrite(7),  // (42->7)  minecraft:underwater -> suspended‌
                rewrite(5),  // (43->5)  minecraft:splash -> splash
                rewrite(17), // (44->17) minecraft:witch -> witchMagic
                rewrite(4),  // (45->4)  minecraft:bubble_pop -> bubble
                rewrite(4),  // (46->4)  minecraft:current_down -> bubble
                rewrite(4),  // (47->4)  minecraft:bubble_column_up -> bubble
                rewrite(18), // (48->-1) minecraft:nautilus -> nautilus -> dripWater
                rewrite(18), // (49->18) minecraft:dolphin -> dripWater
        };
    }

    public static ParticleData getMapping(int id) {
        return particles[id];
    }

    private static ParticleData rewrite(int replacementId) {
        return new ParticleData(replacementId);
    }

    private static ParticleData rewrite(int replacementId, ParticleHandler handler) {
        return new ParticleData(replacementId, handler);
    }

    public interface ParticleHandler {

        int[] rewrite(Protocol1_12_2To1_13 protocol, PacketWrapper wrapper) throws Exception;

        int[] rewrite(Protocol1_12_2To1_13 protocol, List<Particle.ParticleData> data);

        default boolean isBlockHandler() {
            return false;
        }
    }

    public static final class ParticleData {
        private final int historyId;
        private final ParticleHandler handler;

        private ParticleData(int historyId, ParticleHandler handler) {
            this.historyId = historyId;
            this.handler = handler;
        }

        private ParticleData(int historyId) {
            this(historyId, null);
        }

        public int[] rewriteData(Protocol1_12_2To1_13 protocol, PacketWrapper wrapper) throws Exception {
            if (handler == null) return null;
            return handler.rewrite(protocol, wrapper);
        }

        public int[] rewriteMeta(Protocol1_12_2To1_13 protocol, List<Particle.ParticleData> data) {
            if (handler == null) return null;
            return handler.rewrite(protocol, data);
        }

        public int getHistoryId() {
            return historyId;
        }

        public ParticleHandler getHandler() {
            return handler;
        }
    }
}
