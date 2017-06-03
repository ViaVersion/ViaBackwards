/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_10to1_11;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;

/*
    Copied from ViaVersion //TODO implement in EntityTypes?
 */
public class EntityTypeNames {
    private static BiMap<String, String> newToOldNames = HashBiMap.create();

    static {
        add("AreaEffectCloud", "minecraft:area_effect_cloud");
        add("ArmorStand", "minecraft:armor_stand");
        add("Arrow", "minecraft:arrow");
        add("Bat", "minecraft:bat");
        add("Blaze", "minecraft:blaze");
        add("Boat", "minecraft:boat");
        add("CaveSpider", "minecraft:cave_spider");
        add("Chicken", "minecraft:chicken");
        add("Cow", "minecraft:cow");
        add("Creeper", "minecraft:creeper");
        add("Donkey", "minecraft:donkey");
        add("DragonFireball", "minecraft:dragon_fireball");
        add("ElderGuardian", "minecraft:elder_guardian");
        add("EnderCrystal", "minecraft:ender_crystal");
        add("EnderDragon", "minecraft:ender_dragon");
        add("Enderman", "minecraft:enderman");
        add("Endermite", "minecraft:endermite");
        add("EntityHorse", "minecraft:horse");
        add("EyeOfEnderSignal", "minecraft:eye_of_ender_signal");
        add("FallingSand", "minecraft:falling_block");
        add("Fireball", "minecraft:fireball");
        add("FireworksRocketEntity", "minecraft:fireworks_rocket");
        add("Ghast", "minecraft:ghast");
        add("Giant", "minecraft:giant");
        add("Guardian", "minecraft:guardian");
        add("Husk", "minecraft:husk");
        add("Item", "minecraft:item");
        add("ItemFrame", "minecraft:item_frame");
        add("LavaSlime", "minecraft:magma_cube");
        add("LeashKnot", "minecraft:leash_knot");
        add("MinecartChest", "minecraft:chest_minecart");
        add("MinecartCommandBlock", "minecraft:commandblock_minecart");
        add("MinecartFurnace", "minecraft:furnace_minecart");
        add("MinecartHopper", "minecraft:hopper_minecart");
        add("MinecartRideable", "minecraft:minecart");
        add("MinecartSpawner", "minecraft:spawner_minecart");
        add("MinecartTNT", "minecraft:tnt_minecart");
        add("Mule", "minecraft:mule");
        add("MushroomCow", "minecraft:mooshroom");
        add("Ozelot", "minecraft:ocelot");
        add("Painting", "minecraft:painting");
        add("Pig", "minecraft:pig");
        add("PigZombie", "minecraft:zombie_pigman");
        add("PolarBear", "minecraft:polar_bear");
        add("PrimedTnt", "minecraft:tnt");
        add("Rabbit", "minecraft:rabbit");
        add("Sheep", "minecraft:sheep");
        add("Shulker", "minecraft:shulker");
        add("ShulkerBullet", "minecraft:shulker_bullet");
        add("Silverfish", "minecraft:silverfish");
        add("Skeleton", "minecraft:skeleton");
        add("SkeletonHorse", "minecraft:skeleton_horse");
        add("Slime", "minecraft:slime");
        add("SmallFireball", "minecraft:small_fireball");
        add("Snowball", "minecraft:snowball");
        add("SnowMan", "minecraft:snowman");
        add("SpectralArrow", "minecraft:spectral_arrow");
        add("Spider", "minecraft:spider");
        add("Squid", "minecraft:squid");
        add("Stray", "minecraft:stray");
        add("ThrownEgg", "minecraft:egg");
        add("ThrownEnderpearl", "minecraft:ender_pearl");
        add("ThrownExpBottle", "minecraft:xp_bottle");
        add("ThrownPotion", "minecraft:potion");
        add("Villager", "minecraft:villager");
        add("VillagerGolem", "minecraft:villager_golem");
        add("Witch", "minecraft:witch");
        add("WitherBoss", "minecraft:wither");
        add("WitherSkeleton", "minecraft:wither_skeleton");
        add("WitherSkull", "minecraft:wither_skull");
        add("Wolf", "minecraft:wolf");
        add("XPOrb", "minecraft:xp_orb");
        add("Zombie", "minecraft:zombie");
        add("ZombieHorse", "minecraft:zombie_horse");
        add("ZombieVillager", "minecraft:zombie_villager");
    }

    // Other way around (-:
    private static void add(String oldName, String newName) {
        newToOldNames.put(newName, oldName);
    }

    public static void toClient(CompoundTag tag) {
        if (tag.get("id") instanceof StringTag) {
            StringTag id = tag.get("id");
            if (newToOldNames.containsKey(id.getValue())) {
                id.setValue(newToOldNames.get(id.getValue()));
            }
        }
    }

    public static void toClientSpawner(CompoundTag tag) {
        if (tag != null && tag.contains("SpawnData")) {
            CompoundTag spawnData = tag.get("SpawnData");
            if (spawnData != null && spawnData.contains("id"))
                toClient(spawnData);
        }
    }

    public static void toClientItem(Item item) {
        if (hasEntityTag(item)) {
            CompoundTag entityTag = item.getTag().get("EntityTag");
            toClient(entityTag);
        }
    }

    private static boolean hasEntityTag(Item item) {
        if (item != null && item.getId() == 383) { // Monster Egg
            CompoundTag tag = item.getTag();
            if (tag != null && tag.contains("EntityTag") && tag.get("EntityTag") instanceof CompoundTag) {
                if (((CompoundTag) tag.get("EntityTag")).get("id") instanceof StringTag) {
                    return true;
                }
            }
        }
        return false;
    }
}

