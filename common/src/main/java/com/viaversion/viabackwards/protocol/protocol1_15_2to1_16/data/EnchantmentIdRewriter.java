package com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.data;

import com.viaversion.viabackwards.ViaBackwards;

public class EnchantmentIdRewriter {

    /**
     * Get enchantment ID thanks to the given enchant key
     * 
     * @param enchantKey the minecraft key as "minecraft:enchant"
     * @return the id
     */
    public static int getEnchantId(String enchantKey) {
        switch (enchantKey) {
        case "minecraft:protection":
            return 0;
        case "minecraft:fire_protection":
            return 1;
        case "minecraft:feather_falling":
            return 2;
        case "minecraft:blast_protection":
            return 3;
        case "minecraft:projectile_protection":
            return 4;
        case "minecraft:respiration":
            return 5;
        case "minecraft:aqua_affinity":
            return 6;
        case "minecraft:thorns":
            return 7;
        case "minecraft:depth_strider":
            return 8;
        case "minecraft:frost_walker":
            return 9;
        case "minecraft:binding_curse":
            return 10;
        case "minecraft:sharpness":
            return 16;
        case "minecraft:smite":
            return 17;
        case "minecraft:bane_of_arthropods":
            return 18;
        case "minecraft:knockback":
            return 19;
        case "minecraft:fire_aspect":
            return 20;
        case "minecraft:looting":
            return 21;
        case "minecraft:sweeping":
            return 22;
        case "minecraft:efficiency":
            return 32;
        case "minecraft:silk_touch":
            return 33;
        case "minecraft:unbreaking":
            return 34;
        case "minecraft:fortune":
            return 35;
        case "minecraft:power":
            return 48;
        case "minecraft:punch":
            return 49;
        case "minecraft:flame":
            return 50;
        case "minecraft:infinity":
            return 51;
        case "minecraft:luck_of_the_sea":
            return 61;
        case "minecraft:lure":
            return 62;
        case "minecraft:mending":
            return 70;
        case "minecraft:vanishing_curse":
            return 71;
        default:
            ViaBackwards.getPlatform().getLogger()
                    .warning("Unknow key '" + enchantKey + "' for enchant while remapping item.");
        }
        return 0;
    }
}
