/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.v1_13to1_12_2.data;

import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ParticleIdMappings1_12_2 {
    private static final ParticleData[] particles;

    static {
        ParticleHandler blockHandler = new ParticleHandler() {
            @Override
            public int[] rewrite(Protocol1_13To1_12_2 protocol, PacketWrapper wrapper) {
                return rewrite(wrapper.read(Types.VAR_INT));
            }

            @Override
            public int[] rewrite(Protocol1_13To1_12_2 protocol, List<Particle.ParticleData<?>> data) {
                return rewrite((int) data.get(0).getValue());
            }

            private int[] rewrite(int newType) {
                int blockType = Protocol1_13To1_12_2.MAPPINGS.getNewBlockStateId(newType);

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
            rewrite(44), // (7->44)  minecraft:damage_indicator -> damageIndicator
            rewrite(42), // (8->42)  minecraft:dragon_breath -> dragonbreath
            rewrite(19), // (9->19)  minecraft:dripping_lava -> dripLava
            rewrite(18), // (10->18) minecraft:dripping_water -> dripWater
            rewrite(30, new ParticleHandler() {
                @Override
                public int[] rewrite(Protocol1_13To1_12_2 protocol, PacketWrapper wrapper) {
                    float r = wrapper.read(Types.FLOAT);
                    float g = wrapper.read(Types.FLOAT);
                    float b = wrapper.read(Types.FLOAT);
                    float scale = wrapper.read(Types.FLOAT);

                    wrapper.set(Types.FLOAT, 3, r); // 5 - Offset X index=3
                    wrapper.set(Types.FLOAT, 4, g); // 6 - Offset Y index=4
                    wrapper.set(Types.FLOAT, 5, b); // 7 - Offset Z index=5
                    wrapper.set(Types.FLOAT, 6, scale); // 8 - Particle Data index=6
                    wrapper.set(Types.INT, 1, 0); // 9 - Particle Count index=1 enable rgb particle

                    return null;
                }

                @Override
                public int[] rewrite(Protocol1_13To1_12_2 protocol, List<Particle.ParticleData<?>> data) {
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
                public int[] rewrite(Protocol1_13To1_12_2 protocol, PacketWrapper wrapper) {
                    return rewrite(protocol, wrapper.read(Types.ITEM1_13));
                }

                @Override
                public int[] rewrite(Protocol1_13To1_12_2 protocol, List<Particle.ParticleData<?>> data) {
                    return rewrite(protocol, (Item) data.get(0).getValue());
                }

                private int[] rewrite(Protocol1_13To1_12_2 protocol, Item newItem) {
                    Item item = protocol.getItemRewriter().handleItemToClient(null, newItem);
                    return new int[]{item.identifier(), item.data()};
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

        int[] rewrite(Protocol1_13To1_12_2 protocol, PacketWrapper wrapper);

        int[] rewrite(Protocol1_13To1_12_2 protocol, List<Particle.ParticleData<?>> data);

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

        public int @Nullable [] rewriteData(Protocol1_13To1_12_2 protocol, PacketWrapper wrapper) {
            if (handler == null) return null;
            return handler.rewrite(protocol, wrapper);
        }

        public int @Nullable [] rewriteMeta(Protocol1_13To1_12_2 protocol, List<Particle.ParticleData<?>> data) {
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
