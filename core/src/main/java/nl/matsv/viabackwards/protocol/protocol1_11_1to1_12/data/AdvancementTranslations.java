/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancementTranslations {
    private static final Map<String, String> advancements = new ConcurrentHashMap<>();

    static {
        add("advancements.nether.get_wither_skull.title", "Spooky Scary Skeleton");
        add("advancements.husbandry.break_diamond_hoe.description", "Completely use up a diamond hoe, and then reevaluate your life choices");
        add("advancements.nether.fast_travel.description", "Use the Nether to travel 7km in the Overworld");
        add("advancements.story.enter_the_end.description", "Enter the End Portal");
        add("advancements.story.follow_ender_eye.title", "Eye Spy");
        add("advancements.toast.task", "Advancement Made!");
        add("advancements.nether.find_fortress.description", "Break your way into a Nether Fortress");
        add("advancements.story.form_obsidian.description", "Form and mine a block of Obsidian");
        add("advancements.adventure.sniper_duel.description", "Kill a skeleton with an arrow from more than 50 meters");
        add("advancements.end.root.description", "Or the beginning?");
        add("advancements.adventure.kill_a_mob.title", "Monster Hunter");
        add("advancements.end.elytra.description", "Find an Elytra");
        add("advancements.end.dragon_breath.description", "Collect dragon's breath in a glass bottle");
        add("advancements.story.enter_the_nether.title", "We Need to Go Deeper");
        add("advancements.story.upgrade_tools.title", "Getting an Upgrade");
        add("advancements.story.lava_bucket.title", "Hot Stuff");
        add("advancements.story.shiny_gear.title", "Cover Me With Diamonds");
        add("advancements.story.smelt_iron.description", "Smelt an iron ingot");
        add("chat.type.advancement.goal", "%s has reached the goal %s");
        add("advancements.husbandry.break_diamond_hoe.title", "Serious Dedication");
        add("advancements.story.iron_tools.description", "Upgrade your pickaxe");
        add("advancements.end.respawn_dragon.title", "The End... Again...");
        add("advancements.husbandry.tame_an_animal.description", "Tame an animal");
        add("advancements.end.levitate.description", "Levitate up 50 blocks from the attacks of a Shulker");
        add("advancements.adventure.shoot_arrow.description", "Shoot something with a bow and arrow");
        add("advancements.nether.root.description", "Bring summer clothes");
        add("advancements.story.enchant_item.description", "Enchant an item at an Enchanting Table");
        add("advancements.adventure.root.title", "Adventure");
        add("advancements.adventure.trade.title", "What a Deal!");
        add("advancements.husbandry.breed_all_animals.title", "Two by Two");
        add("advancements.nether.find_fortress.title", "A Terrible Fortress");
        add("advancements.nether.create_full_beacon.title", "Beaconator");
        add("advancements.story.cure_zombie_villager.description", "Weaken and then cure a zombie villager");
        add("advancements.toast.challenge", "Challenge Complete!");
        add("advancements.nether.create_full_beacon.description", "Bring a beacon to full power");
        add("advancements.story.follow_ender_eye.description", "Follow an Ender Eye");
        add("advancements.end.find_end_city.title", "The City at the End of the Game");
        add("chat.type.advancement.challenge", "%s has completed the challenge %s");
        add("advancements.story.deflect_arrow.title", "Not Today, Thank You");
        add("advancements.adventure.kill_all_mobs.description", "Kill one of every hostile monster");
        add("advancements.story.smelt_iron.title", "Acquire Hardware");
        add("advancements.end.levitate.title", "Great View From Up Here");
        add("advancements.end.kill_dragon.title", "Free the End");
        add("advancements.end.kill_dragon.description", "Good luck");
        add("advancements.nether.brew_potion.title", "Local Brewery");
        add("advancements.story.enchant_item.title", "Enchanter");
        add("advancements.end.respawn_dragon.description", "Respawn the ender dragon");
        add("advancements.nether.uneasy_alliance.title", "Uneasy Alliance");
        add("advancements.nether.root.title", "Nether");
        add("advancements.nether.brew_potion.description", "Brew a potion");
        add("advancements.nether.obtain_blaze_rod.title", "Into Fire");
        add("advancements.nether.summon_wither.title", "Withering Heights");
        add("advancements.story.root.title", "Minecraft");
        add("advancements.husbandry.balanced_diet.description", "Eat everything that is edible, even if it's not good for you");
        add("advancements.nether.uneasy_alliance.description", "Rescue a Ghast from the Nether, bring it safely home to the Overworld... and then kill it.");
        add("advancements.husbandry.breed_all_animals.description", "Breed all the animals!");
        add("advancements.adventure.kill_all_mobs.title", "Monsters Hunted");
        add("advancements.story.cure_zombie_villager.title", "Zombie Doctor");
        add("advancements.husbandry.plant_seed.title", "A Seedy Place");
        add("advancements.end.dragon_breath.title", "You Need a Mint");
        add("advancements.story.mine_stone.title", "Stone Age");
        add("advancements.end.find_end_city.description", "Go on in, what could happen?");
        add("advancements.nether.create_beacon.description", "Construct and place a Beacon");
        add("advancements.adventure.summon_iron_golem.title", "Hired Help");
        add("advancements.end.elytra.title", "Sky's the Limit");
        add("chat.type.advancement.task", "%s has made the advancement %s");
        add("advancements.story.deflect_arrow.description", "Deflect an arrow with a shield");
        add("advancements.nether.all_effects.description", "Have every effect applied at the same time");
        add("advancements.story.enter_the_nether.description", "Build, light and enter a Nether Portal");
        add("advancements.story.mine_diamond.title", "Diamonds!");
        add("advancements.husbandry.balanced_diet.title", "A Balanced Diet");
        add("advancements.husbandry.root.title", "Husbandry");
        add("advancements.story.root.description", "The heart and story of the game");
        add("advancements.story.upgrade_tools.description", "Construct a better pickaxe");
        add("advancements.nether.all_effects.title", "How Did We Get Here?");
        add("advancements.end.enter_end_gateway.title", "Remote Getaway");
        add("advancements.story.mine_stone.description", "Mine stone with your new pickaxe");
        add("advancements.husbandry.root.description", "The world is full of friends and food");
        add("advancements.end.dragon_egg.title", "The Next Generation");
        add("advancements.toast.goal", "Goal Reached!");
        add("advancements.empty", "There doesn't seem to be anything here...");
        add("advancements.husbandry.tame_an_animal.title", "Best Friends Forever");
        add("advancements.end.root.title", "The End");
        add("advancements.husbandry.breed_an_animal.title", "The Parrots and the Bats");
        add("advancements.story.mine_diamond.description", "Acquire diamonds");
        add("advancements.adventure.sleep_in_bed.title", "Sweet dreams");
        add("advancements.nether.return_to_sender.title", "Return to Sender");
        add("advancements.story.obtain_armor.title", "Suit Up");
        add("advancements.adventure.kill_a_mob.description", "Kill any hostile monster");
        add("advancements.nether.all_potions.description", "Have every potion effect applied at the same time");
        add("advancements.story.iron_tools.title", "Isn't It Iron Pick");
        add("advancements.adventure.sleep_in_bed.description", "Change your respawn point");
        add("advancements.husbandry.plant_seed.description", "Plant a seed and watch it grow");
        add("advancements.husbandry.breed_an_animal.description", "Breed two animals together");
        add("advancements.adventure.shoot_arrow.title", "Take Aim");
        add("advancements.adventure.adventuring_time.description", "Discover every biome");
        add("advancements.adventure.adventuring_time.title", "Adventuring Time");
        add("advancements.nether.get_wither_skull.description", "Obtain a wither skeleton's skull");
        add("advancements.adventure.summon_iron_golem.description", "Summon an Iron Golem to help defend a village");
        add("advancements.nether.return_to_sender.description", "Destroy a Ghast with a fireball");
        add("advancements.adventure.trade.description", "Successfully trade with a Villager");
        add("advancements.story.obtain_armor.description", "Protect yourself with a piece of iron armor");
        add("advancements.adventure.root.description", "Adventure, exploration and combat");
        add("advancements.nether.create_beacon.title", "Bring Home the Beacon");
        add("advancements.end.dragon_egg.description", "Hold the Dragon Egg");
        add("advancements.nether.obtain_blaze_rod.description", "Relieve a Blaze of its rod");
        add("advancements.story.lava_bucket.description", "Fill a bucket with lava");
        add("advancements.story.form_obsidian.title", "Ice Bucket Challenge");
        add("advancements.story.enter_the_end.title", "The End?");
        add("advancements.nether.fast_travel.title", "Subspace Bubble");
        add("advancements.end.enter_end_gateway.description", "Escape the island");
        add("advancements.adventure.totem_of_undying.title", "Postmortal");
        add("advancements.nether.all_potions.title", "A Furious Cocktail");
        add("advancements.adventure.sniper_duel.title", "Sniper duel");
        add("advancements.nether.summon_wither.description", "Summon the Wither");
        add("advancements.adventure.totem_of_undying.description", "Use a Totem of Undying to cheat death");
        add("advancements.story.shiny_gear.description", "Diamond armor saves lives");
    }

    private static void add(String key, String value) {
        advancements.put(key, value);
    }

    public static boolean has(String key) {
        return advancements.containsKey(key);
    }

    public static String get(String key) {
        return advancements.get(key);
    }
}
