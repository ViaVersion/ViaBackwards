/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Mostly a lost cause as the server will not send all the necessary data.
// Recipe displays can also be different from the actual recipe - at the end of the same,
// the server will still properly handle inputs, but we can't fully reconstruct the recipe book.
public final class RecipeStorage implements StorableObject {

    // Pairs of open + filtering for: Crafting, furnace, blast furnace, smoker
    public static final int RECIPE_BOOK_SETTINGS = 4 * 2;
    private static final String[] EMPTY_STRINGS = new String[0];
    private final List<Recipe> recipes = new ArrayList<>(); // Recipes, excluding stone cutter recipes, in the order they were received
    private final List<Recipe> tempRecipes = new ArrayList<>(); // Recipes received in the current batch
    private final List<StoneCutterRecipe> stoneCutterRecipes = new ArrayList<>();
    private boolean[] recipeBookSettings = new boolean[RECIPE_BOOK_SETTINGS];
    private final Protocol1_21_2To1_21 protocol;

    public RecipeStorage(final Protocol1_21_2To1_21 protocol) {
        this.protocol = protocol;
    }

    abstract static class Recipe {
        private static final int FOOD_CRAFTING_BOOK_CATEGORY = 0;
        private static final int BLOCKS_CRAFTING_BOOK_CATEGORY = 1;
        private static final int MISC_CRAFTING_BOOK_CATEGORY = 2;
        protected int index;
        private Integer group;
        private int category;
        private boolean highlight;

        abstract void write(PacketWrapper wrapper);

        void writeGroup(final PacketWrapper wrapper) {
            wrapper.write(Types.STRING, group != null ? Integer.toString(group) : "");
        }

        void writeIngredients(final PacketWrapper wrapper, final Item[][] ingredients) {
            wrapper.write(Types.VAR_INT, ingredients.length);
            for (final Item[] ingredient : ingredients) {
                writeIngredient(wrapper, ingredient);
            }
        }

        void writeIngredient(final PacketWrapper wrapper, final Item[] ingredient) {
            final Item[] copy = new Item[ingredient.length];
            for (int i = 0; i < ingredient.length; i++) {
                copy[i] = ingredient[i].copy();
            }
            wrapper.write(VersionedTypes.V1_21_2.itemArray(), copy);
        }

        void writeResult(final PacketWrapper wrapper, final Item result) {
            wrapper.write(VersionedTypes.V1_21_2.item(), result.copy());
        }

        void writeCategory(final PacketWrapper wrapper) {
            // TODO Doesn't translate exactly
            final int craftingBookCategory = switch (category) {
                case 4, 9, 12 -> FOOD_CRAFTING_BOOK_CATEGORY;
                case 0, 5, 7, 10 -> BLOCKS_CRAFTING_BOOK_CATEGORY;
                case 1, 2, 3, 6, 8, 11 -> MISC_CRAFTING_BOOK_CATEGORY;
                default -> MISC_CRAFTING_BOOK_CATEGORY;
            };
            wrapper.write(Types.VAR_INT, craftingBookCategory);
        }

        int category() {
            return category;
        }
    }

    public void sendRecipes(final UserConnection connection) {
        // Fill from temp recipes so we can clear before if needed
        if (!tempRecipes.isEmpty()) {
            this.recipes.addAll(tempRecipes);
            tempRecipes.clear();
        }

        int highestIndex = -1;
        for (final Recipe recipe : recipes) {
            highestIndex = Math.max(highestIndex, recipe.index);
        }

        // Add stonecutter recipes from update_recipes
        final List<Recipe> recipes = new ArrayList<>(this.recipes);
        for (final StoneCutterRecipe recipe : stoneCutterRecipes) {
            recipe.index = ++highestIndex;
            recipes.add(recipe);
        }

        // Sort by id
        recipes.sort(Comparator.comparingInt(a -> a.index));

        // Since the server only sends unlocked recipes, we need to re-send all recipes in UPDATE_RECIPES
        final PacketWrapper updateRecipesPacket = PacketWrapper.create(ClientboundPackets1_21.UPDATE_RECIPES, connection);
        updateRecipesPacket.write(Types.VAR_INT, recipes.size());
        for (final Recipe recipe : recipes) {
            updateRecipesPacket.write(Types.STRING, identifier(recipe.index));
            recipe.write(updateRecipesPacket);
        }
        updateRecipesPacket.send(Protocol1_21_2To1_21.class);

        sendUnlockedRecipes(connection, recipes);
    }

    private static String identifier(final int recipeIndex) {
        // Use index as the recipe identifier, add leading zeros to keept it sorted
        return String.format("%06d", recipeIndex);
    }

    public void lockRecipes(final PacketWrapper wrapper, final int[] ids) {
        for (final int id : ids) {
            recipes.removeIf(recipe -> recipe.index == id);
        }

        wrapper.write(Types.VAR_INT, 2); // Remove recipes
        for (final boolean recipeBookSetting : recipeBookSettings) {
            wrapper.write(Types.BOOLEAN, recipeBookSetting);
        }

        final String[] recipeKeys = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            recipeKeys[i] = identifier(ids[i]);
        }
        wrapper.write(Types.STRING_ARRAY, recipeKeys);
    }

    private void sendUnlockedRecipes(final UserConnection connection, final List<Recipe> recipes) {
        final PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_21.RECIPE, connection);
        wrapper.write(Types.VAR_INT, 0); // Init recipes

        for (final boolean recipeBookSetting : recipeBookSettings) {
            wrapper.write(Types.BOOLEAN, recipeBookSetting);
        }

        // Use index as the recipe identifier. We only know the unlocked ones, so send all
        final String[] recipeKeys = new String[recipes.size()];
        final List<String> highlightRecipes = new ArrayList<>();
        for (int i = 0; i < recipes.size(); i++) {
            recipeKeys[i] = identifier(recipes.get(i).index);
            if (recipes.get(i).highlight) {
                highlightRecipes.add(recipeKeys[i]);
            }
        }
        wrapper.write(Types.STRING_ARRAY, recipeKeys);

        wrapper.write(Types.STRING_ARRAY, highlightRecipes.toArray(EMPTY_STRINGS));
        wrapper.send(Protocol1_21_2To1_21.class);
    }

    public void readRecipe(final PacketWrapper wrapper) {
        final int id = wrapper.read(Types.VAR_INT);
        final int type = wrapper.passthrough(Types.VAR_INT);
        final Recipe recipe = switch (type) {
            case 0 -> readShapeless(wrapper);
            case 1 -> readShaped(wrapper);
            case 2 -> readFurnace(wrapper);
            case 3 -> readStoneCutter(wrapper);
            case 4 -> readSmithing(wrapper);
            default -> null;
        };

        final Integer group = wrapper.read(Types.OPTIONAL_VAR_INT);
        final int category = wrapper.read(Types.VAR_INT);
        if (wrapper.read(Types.BOOLEAN)) {
            final int ingredientsSize = wrapper.read(Types.VAR_INT);
            for (int j = 0; j < ingredientsSize; j++) {
                //handleIngredient(wrapper); // Items //TODO ?
                wrapper.read(Types.HOLDER_SET);
            }
        }
        final byte flags = wrapper.read(Types.BYTE);

        if (recipe != null) {
            recipe.index = id;
            recipe.group = group;
            recipe.category = category;
            recipe.highlight = (flags & 2) != 0;
        }
    }

    private Recipe readShapeless(final PacketWrapper wrapper) {
        final Item[][] ingredients = readSlotDisplayList(wrapper);
        final Item result = readSingleSlotDisplay(wrapper);
        readSlotDisplay(wrapper); // Crafting station
        return add(new ShapelessRecipe(ingredients, result));
    }

    private Recipe readShaped(final PacketWrapper wrapper) {
        final int width = wrapper.passthrough(Types.VAR_INT);
        final int height = wrapper.passthrough(Types.VAR_INT);
        final Item[][] ingredients = readSlotDisplayList(wrapper);
        final Item result = readSingleSlotDisplay(wrapper);
        readSlotDisplay(wrapper); // Crafting station
        return add(new ShapedRecipe(width, height, ingredients, result));
    }

    private Recipe readFurnace(final PacketWrapper wrapper) {
        final Item[] ingredient = readSlotDisplay(wrapper);
        readSlotDisplay(wrapper); // Fuel
        final Item result = readSingleSlotDisplay(wrapper);
        readSlotDisplay(wrapper); // Crafting station
        final int duration = wrapper.read(Types.VAR_INT);
        final float experience = wrapper.read(Types.FLOAT);
        return add(new FurnaceRecipe(ingredient, result, duration, experience));
    }

    private Recipe readStoneCutter(final PacketWrapper wrapper) {
        // Use values from UPDATE_RECIPES instead
        readSlotDisplay(wrapper); // Input
        readSlotDisplay(wrapper); // Result
        readSlotDisplay(wrapper); // Crafting station
        return null;
    }

    private Recipe readSmithing(final PacketWrapper wrapper) {
        // TODO Combine with update_recipes?
        readSlotDisplay(wrapper); // Template
        readSlotDisplay(wrapper); // Base
        readSlotDisplay(wrapper); // Addition
        readSlotDisplay(wrapper); // Result
        readSlotDisplay(wrapper); // Crafting station
        return null;
    }

    private Recipe add(final Recipe recipe) {
        tempRecipes.add(recipe);
        return recipe;
    }

    private Item[][] readSlotDisplayList(final PacketWrapper wrapper) {
        final int size = wrapper.passthrough(Types.VAR_INT);
        final Item[][] ingredients = new Item[size][];
        for (int i = 0; i < size; i++) {
            ingredients[i] = readSlotDisplay(wrapper);
        }
        return ingredients;
    }

    private Item readSingleSlotDisplay(final PacketWrapper wrapper) {
        final Item[] items = readSlotDisplay(wrapper);
        return items.length == 0 ? new StructuredItem(1, 1) : items[0];
    }

    private Item[] readSlotDisplay(final PacketWrapper wrapper) {
        // empty, any_fuel, smithing_trim are empty
        final int type = wrapper.read(Types.VAR_INT);
        return switch (type) {
            case 2 -> {
                final int id = wrapper.read(Types.VAR_INT);
                if (id == 0) {
                    protocol.getLogger().warning("Empty item id in recipe");
                    yield new Item[0];
                }
                yield new Item[]{new StructuredItem(rewriteItemId(id), 1)};
            }
            case 3 -> {
                final Item item = wrapper.read(VersionedTypes.V1_21_2.item());
                protocol.getItemRewriter().handleItemToClient(wrapper.user(), item);
                if (item.isEmpty()) {
                    protocol.getLogger().warning("Empty item in recipe");
                    yield new Item[0];
                }
                yield new Item[]{item};
            }
            case 4 -> {
                wrapper.read(Types.STRING); // Tag key // TODO Probably not even worth the effort
                yield new Item[0];
            }
            case 5 -> {
                readSlotDisplay(wrapper); // Base
                readSlotDisplay(wrapper); // Material
                readSlotDisplay(wrapper); // Pattern
                yield new Item[0];
            }
            case 6 -> {
                readSlotDisplay(wrapper); // Input
                readSlotDisplay(wrapper); // Remainder
                yield new Item[0];
            }
            case 7 -> readSlotDisplayList(wrapper)[0]; // Composite
            default -> new Item[0];
        };
    }

    private int rewriteItemId(final int id) {
        return protocol.getMappingData().getNewItemId(id);
    }

    public void readStoneCutterRecipes(final PacketWrapper wrapper) {
        stoneCutterRecipes.clear();
        final int stonecutterRecipesSize = wrapper.read(Types.VAR_INT);
        for (int i = 0; i < stonecutterRecipesSize; i++) {
            // The ingredients are what's actually used in client prediction, they're the important part
            final Item[] ingredient = readHolderSet(wrapper);
            // TODO Probably not actually the result, might have to combine with update_recipes
            final Item result = readSingleSlotDisplay(wrapper);
            stoneCutterRecipes.add(new StoneCutterRecipe(ingredient, result));
        }
    }

    private Item[] readHolderSet(final PacketWrapper wrapper) {
        final HolderSet holderSet = wrapper.read(Types.HOLDER_SET);
        if (holderSet.hasTagKey()) {
            return new Item[]{new StructuredItem(1, 1)}; // TODO Probably not even worth the effort
        }

        final int[] ids = holderSet.ids();
        for (int i = 0; i < ids.length; i++) {
            ids[i] = rewriteItemId(ids[i]);
        }

        final Item[] ingredient = new Item[ids.length];
        for (int i = 0; i < ingredient.length; i++) {
            ingredient[i] = new StructuredItem(ids[i], 1);
        }
        return ingredient;
    }

    private static final class ShapelessRecipe extends Recipe {
        private static final int SERIALIZER_ID = 1;
        private final Item[][] ingredients;
        private final Item result;

        private ShapelessRecipe(final Item[][] ingredients, final Item result) {
            this.ingredients = ingredients;
            this.result = result;
        }

        @Override
        public void write(final PacketWrapper wrapper) {
            wrapper.write(Types.VAR_INT, SERIALIZER_ID);
            writeGroup(wrapper);
            writeCategory(wrapper);
            writeIngredients(wrapper, ingredients);
            writeResult(wrapper, result);
        }
    }

    private static final class ShapedRecipe extends Recipe {
        private static final int SERIALIZER_ID = 0;
        private final int width;
        private final int height;
        private final Item[][] ingredients;
        private final Item result;

        private ShapedRecipe(final int width, final int height, final Item[][] ingredients, final Item result) {
            this.width = width;
            this.height = height;
            this.ingredients = ingredients;
            this.result = result;
        }

        @Override
        public void write(final PacketWrapper wrapper) {
            wrapper.write(Types.VAR_INT, SERIALIZER_ID);
            writeGroup(wrapper);
            writeCategory(wrapper);
            wrapper.write(Types.VAR_INT, width);
            wrapper.write(Types.VAR_INT, height);
            Preconditions.checkArgument(width * height == ingredients.length, "Invalid shaped recipe");
            // No length prefix
            for (final Item[] ingredient : ingredients) {
                writeIngredient(wrapper, ingredient);
            }
            writeResult(wrapper, result);
            wrapper.write(Types.BOOLEAN, false); // Doesn't matter for the init
        }
    }

    private static final class FurnaceRecipe extends Recipe {
        private final Item[] ingredient;
        private final Item result;
        private final float experience;
        private final int cookingTime;

        private FurnaceRecipe(final Item[] ingredient, final Item result, final int cookingTime, final float experience) {
            this.ingredient = ingredient;
            this.result = result;
            this.experience = experience;
            this.cookingTime = cookingTime;
        }

        @Override
        public void write(final PacketWrapper wrapper) {
            wrapper.write(Types.VAR_INT, serializerId());
            writeGroup(wrapper);
            writeCategory(wrapper);
            writeIngredient(wrapper, ingredient);
            writeResult(wrapper, result);
            wrapper.write(Types.FLOAT, experience);
            wrapper.write(Types.VAR_INT, cookingTime);
        }

        private int serializerId() {
            return switch (category()) {
                case 4, 5, 6 -> 15; // Furnace food, bocks, misc
                case 7, 8 -> 16; // Blast furnace blocks, misc
                case 9 -> 17; // Smoker
                case 12 -> 18; // Campfire
                default -> 15;
            };
        }
    }

    private static final class StoneCutterRecipe extends Recipe {
        private static final int SERIALIZER_ID = 19;
        private final Item[] ingredient;
        private final Item result;

        private StoneCutterRecipe(final Item[] ingredient, final Item result) {
            this.ingredient = ingredient;
            this.result = result;
        }

        @Override
        public void write(final PacketWrapper wrapper) {
            wrapper.write(Types.VAR_INT, SERIALIZER_ID);
            writeGroup(wrapper);
            writeIngredient(wrapper, ingredient);
            writeResult(wrapper, result);
        }
    }

    public void setRecipeBookSettings(final boolean[] recipeBookSettings) {
        this.recipeBookSettings = recipeBookSettings;
    }

    public void clearRecipes() {
        recipes.clear();
    }
}
