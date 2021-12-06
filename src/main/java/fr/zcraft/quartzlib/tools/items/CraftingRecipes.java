/*
 * Copyright or © or Copr. QuartzLib contributors (2015 - 2020)
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.zcraft.quartzlib.tools.items;

import fr.zcraft.quartzlib.core.QuartzComponent;
import fr.zcraft.quartzlib.core.QuartzLib;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * This class provides various utilities for Crafting Recipes management.
 * Crafting recipes can be registered safely before the plugin/server is loaded.
 * If this is the case, their registration will be done at plugin initialization.
 */
public class CraftingRecipes extends QuartzComponent {
    private static final ArrayList<Recipe> recipesToLoad = new ArrayList<>();
    private static boolean registered = false;
    private static boolean enabled = false;

    private static boolean checkEnabled() {
        if (registered) {
            return enabled;
        }

        QuartzLib.loadComponent(CraftingRecipes.class);
        registered = true;

        return true;
    }

    /**
     * Registers a new recipe.
     *
     * @param recipe The recipe to register.
     */
    public static void add(Recipe recipe) {
        if (recipe == null) {
            return;
        }

        if (checkEnabled()) {
            QuartzLib.getPlugin().getServer().addRecipe(recipe);
        } else {
            recipesToLoad.add(recipe);
        }
    }

    /**
     * Registers several new recipes.
     *
     * @param recipes the new recipes to add.
     */
    public static void add(Recipe... recipes) {
        if (recipes == null) {
            return;
        }

        for (Recipe recipe : recipes) {
            add(recipe);
        }
    }

    /**
     * Registers several new recipes.
     *
     * @param recipes the new recipes to add.
     */
    public static void add(Iterable<Recipe> recipes) {
        if (recipes == null) {
            return;
        }

        for (Recipe recipe : recipes) {
            add(recipe);
        }
    }

    /**
     * Generates a recipe string array from a single string.
     * If the string is shorter than 9 characters, spaces will be appended.
     *
     * @param str The recipe string
     * @return The recipe string array
     */
    public static @NotNull String[] getRecipeShape(@NotNull String str) {
        if (str.length() > 9) {
            throw new IllegalArgumentException("Invalid recipe shape string");
        }

        StringBuilder strBuilder = new StringBuilder(str);
        while (strBuilder.length() < 9) {
            strBuilder.append(" ");
        }
        str = strBuilder.toString();

        return new String[] {
                str.substring(0, 3),
                str.substring(3, 6),
                str.substring(6, 9)
        };
    }

    private static @NotNull String[] generateRecipeShape(int count) {
        StringBuilder shape = new StringBuilder();
        char key = 'A';
        while (count-- > 0) {
            shape.append(key);
            key = (char) (key + 1);
        }
        return getRecipeShape(shape.toString());
    }

    /**
     * Generates a dummy shaped recipe, without any specified shape.
     * Ingredients are automatically indexed, using uppercase letters
     * (A = first ingredient, B = second ingredient, etc.)
     *
     * @param recipeName The name of the new recipe.
     * @param result    The resulting item of the recipe.
     * @param materials The ingredients for the recipe
     * @return The dummy recipe.
     */
    public static @NotNull ShapedRecipe shaped(String recipeName, ItemStack result, Material... materials) {
        RecipeChoice[] choices = Arrays.stream(materials)
                .map(RecipeChoice.MaterialChoice::new)
                .toArray(RecipeChoice[]::new);

        return shaped(recipeName, result, choices);
    }

    /**
     * Generates a dummy shaped recipe, without any specified shape.
     * Ingredients are automatically indexed, using uppercase letters
     * (A = first ingredient, B = second ingredient, etc.)
     *
     * @param recipeName The name of the new recipe.
     * @param result    The resulting item of the recipe.
     * @param materials The ingredients for the recipe
     * @return The dummy recipe.
     */
    public static @NotNull ShapedRecipe shaped(String recipeName, ItemStack result, RecipeChoice... materials) {
        if (materials.length > 9) {
            throw new IllegalArgumentException("Too many materials for a recipe (a maximum of 9 is expected).");
        }

        NamespacedKey namespacedKey = new NamespacedKey(QuartzLib.getPlugin(), recipeName);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result)
                .shape(generateRecipeShape(materials.length));

        char key = 'A';
        for (RecipeChoice material : materials) {
            recipe.setIngredient(key, material);
            key = (char) (key + 1);
        }

        return recipe;
    }

    /**
     * Generates a new shaped recipe.
     * Ingredients are automatically indexed, using uppercase letters
     * (A = first ingredient, B = second ingredient, etc.)
     *
     * @param recipeName The name of the new recipe.
     * @param result    The resulting item of the recipe.
     * @param line1     The first line of the recipe
     * @param line2     The second line of the recipe
     * @param line3     The third line of the recipe
     * @param materials The ingredients for the recipe
     * @return The shaped recipe
     */
    public static ShapedRecipe shaped(String recipeName, ItemStack result, String line1, String line2, String line3,
                                      Material... materials) {
        ShapedRecipe recipe = shaped(recipeName, result, materials);
        recipe.shape(line1, line2, line3);
        return recipe;
    }

    /**
     * Generates a new shaped recipe.
     * Ingredients are automatically indexed, using uppercase letters
     * (A = first ingredient, B = second ingredient, etc.)
     *
     * @param recipeName The name of the new recipe.
     * @param result The resulting item of the recipe.
     * @param line1  The first line of the recipe
     * @param line2  The second line of the recipe
     * @param line3  The third line of the recipe
     * @return The shaped recipe
     */
    public static ShapedRecipe shaped(String recipeName, ItemStack result, String line1, String line2, String line3) {
        return shaped(recipeName, result, line1, line2, line3, new Material[] {});
    }

    /**
     * Generates a new shaped recipe.
     * Ingredients are automatically indexed, using uppercase letters
     * (A = first ingredient, B = second ingredient, etc.)
     *
     * @param recipeName The name of the new recipe.
     * @param result    The resulting item of the recipe.
     * @param line1     The first line of the recipe
     * @param line2     The second line of the recipe
     * @param line3     The third line of the recipe
     * @param materials The ingredients for the recipe
     * @return The shaped recipe
     */
    public static ShapedRecipe shaped(String recipeName, ItemStack result, String line1, String line2, String line3,
                                      RecipeChoice... materials) {
        ShapedRecipe recipe = shaped(recipeName, result, materials);
        recipe.shape(line1, line2, line3);
        return recipe;
    }

    /**
     * Generates a new shaped recipe, using the same ingredients and results as
     * the given recipe.
     * Ingredients are automatically indexed, using uppercase letters
     * (A = first ingredient, B = second ingredient, etc.)
     *
     * @param recipeName The name of the new recipe.
     * @param other The recipe to retrieve the ingredients and result from.
     * @param line1 The first line of the recipe
     * @param line2 The second line of the recipe
     * @param line3 The third line of the recipe
     * @return The shaped recipe
     */
    public static ShapedRecipe shaped(String recipeName, ShapedRecipe other, String line1, String line2, String line3) {
        ShapedRecipe newRecipe = shaped(recipeName, other.getResult(), line1, line2, line3);
        other.getChoiceMap().forEach(newRecipe::setIngredient);

        return newRecipe;
    }

    /**
     * Returns a list of all the possible recipes for two ingredients in a
     * 2x2 shaped, placed diagonally.
     * Example :
     * A B -
     * B A -
     * - - -
     *
     * @param baseRecipeName The base name of the new recipe.
     *                       Individual IDs will be appended to this name for each recipe.
     * @param a      The first material of the recipe.
     * @param b      The second material of the recipe.
     * @param result The resulting item of the recipe.
     * @return All the possible recipes.
     */
    public static List<ShapedRecipe> get2x2DiagonalRecipes(
            String baseRecipeName,
            Material a,
            Material b,
            ItemStack result) {
        return get2x2DiagonalRecipes(
                baseRecipeName,
                new RecipeChoice.MaterialChoice(a),
                new RecipeChoice.MaterialChoice(b),
                result);
    }

    /**
     * Returns a list of all the possible recipes for two ingredients in a
     * 2x2 shaped, placed diagonally.
     * Example :
     * A B -
     * B A -
     * - - -
     *
     * @param baseRecipeName The base name of the new recipe.
     *                       Individual IDs will be appended to this name for each recipe.
     * @param a      The first material of the recipe.
     * @param b      The second material of the recipe.
     * @param result The resulting item of the recipe.
     * @return All the possible recipes.
     */
    public static List<ShapedRecipe> get2x2DiagonalRecipes(
            String baseRecipeName,
            RecipeChoice a,
            RecipeChoice b,
            ItemStack result) {
        ArrayList<ShapedRecipe> recipes = new ArrayList<>();

        recipes.add(shaped(baseRecipeName + '1', result, "AB ", "BA ", "   ", a, b));
        recipes.add(shaped(baseRecipeName + '2', result, "BA ", "AB ", "   ", a, b));
        recipes.add(shaped(baseRecipeName + '3', result, " AB", " BA", "   ", a, b));
        recipes.add(shaped(baseRecipeName + '4', result, " BA", " AB", "   ", a, b));
        recipes.add(shaped(baseRecipeName + '5', result, "   ", "AB ", "BA ", a, b));
        recipes.add(shaped(baseRecipeName + '6', result, "   ", "BA ", "AB ", a, b));
        recipes.add(shaped(baseRecipeName + '7', result, "   ", " AB", " BA", a, b));
        recipes.add(shaped(baseRecipeName + '8', result, "   ", " BA", " AB", a, b));

        return recipes;
    }

    /**
     * Returns a list of all the possible recipes for one ingredient in a
     * 2x2 shape, placed diagonally. The other diagonal is empty.
     * Example :
     * A - -
     * - A -
     * - - -
     *
     * @param baseRecipeName The base name of the new recipe.
     *                       Individual IDs will be appended to this name for each recipe.
     * @param a      The material of the recipe.
     * @param result The resulting item of the recipe.
     * @return All the possible recipes.
     */
    public static List<ShapedRecipe> get2x2DiagonalRecipes(String baseRecipeName, Material a, ItemStack result) {
        return get2x2DiagonalRecipes(baseRecipeName, new RecipeChoice.MaterialChoice(a), result);
    }

    /**
     * Returns a list of all the possible recipes for one ingredient in a
     * 2x2 shape, placed diagonally. The other diagonal is empty.
     * Example :
     * A - -
     * - A -
     * - - -
     *
     * @param baseRecipeName The base name of the new recipe.
     *                       Individual IDs will be appended to this name for each recipe.
     * @param a      The material of the recipe.
     * @param result The resulting item of the recipe.
     * @return All the possible recipes.
     */
    public static List<ShapedRecipe> get2x2DiagonalRecipes(String baseRecipeName, RecipeChoice a, ItemStack result) {
        ArrayList<ShapedRecipe> recipes = new ArrayList<>();

        recipes.add(shaped(baseRecipeName + '1',  result, "A  ", " A ", "   ", a));
        recipes.add(shaped(baseRecipeName + '2', result, " A ", "A  ", "   ", a));
        recipes.add(shaped(baseRecipeName + '3', result, " A ", "  A", "   ", a));
        recipes.add(shaped(baseRecipeName + '4', result, "  A", " A ", "   ", a));
        recipes.add(shaped(baseRecipeName + '5', result, "   ", "A  ", " A ", a));
        recipes.add(shaped(baseRecipeName + '6', result, "   ", " A ", "A  ", a));
        recipes.add(shaped(baseRecipeName + '7', result, "   ", " A ", "  A", a));
        recipes.add(shaped(baseRecipeName + '8', result, "   ", "  A", " A ", a));

        return recipes;
    }

    /**
     * Returns a list of all the possible recipes for one ingredient in a
     * 2x2 shape.
     * Example :
     * A A -
     * A A -
     * - - -
     *
     * @param baseRecipeName The base name of the new recipe.
     *                       Individual IDs will be appended to this name for each recipe.
     * @param a      The material of the recipe.
     * @param result The resulting item of the recipe.
     * @return All the possible recipes.
     */
    public static List<ShapedRecipe> get2x2Recipes(String baseRecipeName, Material a, ItemStack result) {
        return get2x2Recipes(baseRecipeName, new RecipeChoice.MaterialChoice(a), result);
    }

    /**
     * Returns a list of all the possible recipes for one ingredient in a
     * 2x2 shape.
     * Example :
     * A A -
     * A A -
     * - - -
     *
     * @param baseRecipeName The base name of the new recipe.
     *                       Individual IDs will be appended to this name for each recipe.
     * @param a      The material of the recipe.
     * @param result The resulting item of the recipe.
     * @return All the possible recipes.
     */
    public static List<ShapedRecipe> get2x2Recipes(String baseRecipeName, RecipeChoice a, ItemStack result) {
        ArrayList<ShapedRecipe> recipes = new ArrayList<>();

        recipes.add(shaped(baseRecipeName + '1', result, "AA ", "AA ", "   ", a));
        recipes.add(shaped(baseRecipeName + '2', result, " AA", " AA", "   ", a));
        recipes.add(shaped(baseRecipeName + '3', result, "   ", "AA ", "AA ", a));
        recipes.add(shaped(baseRecipeName + '4', result, "   ", " AA", " AA", a));

        return recipes;
    }

    @Override
    protected void onEnable() {
        for (Recipe recipe : recipesToLoad) {
            QuartzLib.getPlugin().getServer().addRecipe(recipe);
        }

        recipesToLoad.clear();
        enabled = true;
    }
}
