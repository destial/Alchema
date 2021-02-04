package wtf.choco.alchema.crafting;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import wtf.choco.alchema.Alchema;
import wtf.choco.alchema.api.event.CauldronRecipeRegisterEvent;
import wtf.choco.alchema.cauldron.AlchemicalCauldron;
import wtf.choco.alchema.util.AlchemaEventFactory;
import wtf.choco.alchema.util.NamespacedKeyUtil;

/**
 * Represents a registry in which recipes and recipe types may be registered.
 *
 * @author Parker Hawke - Choco
 */
public class CauldronRecipeRegistry {

    private final Map<@NotNull NamespacedKey, CauldronRecipe> recipes = new HashMap<>();
    private final Map<@NotNull NamespacedKey, Function<@NotNull JsonObject, @NotNull ? extends CauldronIngredient>> ingredientTypes = new HashMap<>();

    /**
     * Register a {@link CauldronRecipe} to be used by any {@link AlchemicalCauldron}.
     *
     * @param recipe the recipe to register
     */
    public void registerCauldronRecipe(@NotNull CauldronRecipe recipe) {
        Preconditions.checkNotNull(recipe, "Cannot register null recipe");
        this.recipes.put(recipe.getKey(), recipe);
    }

    /**
     * Unregister a {@link CauldronRecipe}. Upon unregistration, cauldrons will not longer be able
     * to execute its recipe.
     *
     * @param recipe the recipe to unregister
     */
    public void unregisterCauldronRecipe(@NotNull CauldronRecipe recipe) {
        this.recipes.remove(recipe.getKey());
    }

    /**
     * Unregister a {@link CauldronRecipe} associated with the provided ID. Upon unregistration,
     * cauldrons will no longer be able to execute its recipe.
     *
     * @param key the key of the recipe to unregister
     *
     * @return the unregistered recipe. null if none
     */
    @Nullable
    public CauldronRecipe unregisterCauldronRecipe(@NotNull NamespacedKey key) {
        return recipes.remove(key);
    }

    /**
     * Get the {@link CauldronRecipe} that applies given a set of ingredients. If no recipe can consume
     * the ingredients and catalyst, null is returned. If more than one recipe is valid, the first is
     * selected.
     *
     * @param ingredients the available ingredients
     *
     * @return the cauldron recipe that applies. null if none
     */
    @Nullable
    public CauldronRecipe getApplicableRecipe(@NotNull List<@NotNull CauldronIngredient> ingredients) {
        return recipes.values().stream()
                .filter(recipe -> recipe.getYieldFromIngredients(ingredients) >= 1)
                .findFirst().orElse(null);
    }

    /**
     * Get a collection of all registered recipes. Changes made to the returned collection will be
     * reflected internally to this instance.
     *
     * @return the collection of registered recipes
     */
    @NotNull
    public Collection<@NotNull CauldronRecipe> getRecipes() {
        return recipes.values(); // Intentionally mutable
    }

    /**
     * Clear all recipes in the manager.
     */
    public void clearRecipes() {
        this.recipes.clear();
    }

    /**
     * Register a new type of {@link CauldronIngredient}. This registration should be done during
     * the plugin's load phase (i.e. {@link JavaPlugin#onLoad()}).
     *
     * @param key the ingredient key. Should match that of {@link CauldronIngredient#getKey()}
     * @param ingredientProvider the ingredient provider
     */
    public void registerIngredientType(@NotNull NamespacedKey key, @NotNull Function<@NotNull JsonObject, @NotNull ? extends CauldronIngredient> ingredientProvider) {
        Preconditions.checkArgument(key != null, "key must not be null");
        Preconditions.checkArgument(ingredientProvider != null, "ingredientProvider must not be null");

        this.ingredientTypes.put(key, ingredientProvider);
    }

    /**
     * Parse a {@link CauldronIngredient} with the ingredient type matching the provided
     * {@link NamespacedKey} from a {@link JsonObject}.
     *
     * @param key the key of the ingredient type to parse
     * @param object the object from which to parse the ingredient
     *
     * @return the parsed ingredient. null if invalid
     */
    @Nullable
    public CauldronIngredient parseIngredientType(@NotNull NamespacedKey key, @NotNull JsonObject object) {
        Preconditions.checkArgument(key != null, "key must not be null");
        Preconditions.checkArgument(object != null, "object must not be null");

        Function<@NotNull JsonObject, @NotNull ? extends CauldronIngredient> ingredientProvider = ingredientTypes.get(key);
        if (ingredientProvider == null) {
            return null;
        }

        return ingredientProvider.apply(object);
    }

    /**
     * Clear all registered ingredient types.
     */
    public void clearIngredientTypes() {
        this.ingredientTypes.clear();
    }

    /**
     * Asynchronously load all cauldron recipes from Alchema's file system, as well as any
     * recipes from third-party plugins listening to the {@link CauldronRecipeRegisterEvent}.
     * The returned {@link CompletableFuture} instance provides the load result.
     *
     * @param plugin the instance of Alchema (for logging purposes)
     * @param recipesDirectory the directory from which to load recipes
     *
     * @return a completable future where the supplied value is the amount of loaded recipes
     */
    @NotNull
    public CompletableFuture<@NotNull RecipeLoadResult> loadCauldronRecipes(@NotNull Alchema plugin, @NotNull File recipesDirectory) {
        long now = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> loadCauldronRecipesFromDirectory(plugin, new StandardRecipeLoadResult(), recipesDirectory, recipesDirectory))
        .thenCompose(result -> {
            CompletableFuture<@NotNull RecipeLoadResult> registryEventFuture = new CompletableFuture<>();

            /*
             * Events need to be called synchronously.
             *
             * This also forces the event to be called AFTER all plugins have finished enabling and registering their listeners.
             * runTask() is run on the next server tick which is done post-plugin enable.
             */
            Bukkit.getScheduler().runTask(plugin, () -> {
                AlchemaEventFactory.callCauldronRecipeRegisterEvent(this);

                long timeToComplete = System.currentTimeMillis() - now;

                result.setThirdParty(getRecipes().size() - result.getNative());
                result.setTimeToComplete(timeToComplete);

                registryEventFuture.complete(result);
            });

            return registryEventFuture;
        });
    }

    private StandardRecipeLoadResult loadCauldronRecipesFromDirectory(@NotNull Alchema plugin, @NotNull StandardRecipeLoadResult result, @NotNull File recipesDirectory, File subdirectory) throws JsonSyntaxException {
        for (File recipeFile : subdirectory.listFiles(file -> file.isDirectory() || file.getName().endsWith(".json"))) {
            if (recipeFile.isDirectory()) {
                this.loadCauldronRecipesFromDirectory(plugin, result, recipesDirectory, recipeFile);
                continue;
            }

            String fileName = recipeFile.getName();
            fileName = fileName.substring(0, fileName.indexOf(".json"));

            String joinedRecipeKey = null;
            if (recipesDirectory.equals(subdirectory)) {
                joinedRecipeKey = fileName; // The root directory is too short to substring itself + 1
            } else {
                /*
                 * Converts file paths to valid keys. Example:
                 *
                 * Given: File#getAbsolutePath() (C:\Users\foo\bar\baz)
                 * Parsed: bar/baz + / + fileName
                 */
                joinedRecipeKey = subdirectory.getAbsolutePath().substring(recipesDirectory.getAbsolutePath().length() + 1).replace('\\', '/') + "/" + fileName;
            }

            if (!NamespacedKeyUtil.isValidKey(joinedRecipeKey)) {
                plugin.getLogger().warning("Invalid recipe file name, \"" + recipeFile.getName() + "\". Must be alphanumerical, lowercased and separated by underscores.");
                continue;
            }

            NamespacedKey key = new NamespacedKey(plugin, joinedRecipeKey);

            try (BufferedReader reader = Files.newReader(recipeFile, Charset.defaultCharset())) {
                JsonObject recipeObject = Alchema.GSON.fromJson(reader, JsonObject.class);
                CauldronRecipe recipe = CauldronRecipe.fromJson(key, recipeObject, this);

                this.registerCauldronRecipe(recipe);
                result.setNative(result.getNative() + 1);
            } catch (Exception e) {
                result.addFailureInfo(new RecipeLoadFailureReport(key, e));
            }
        }

        return result;
    }


    private class StandardRecipeLoadResult implements RecipeLoadResult {

        private int nativelyRegistered, thirdPartyRegistered;
        private long timeToComplete;

        private final List<@NotNull RecipeLoadFailureReport> failures = new ArrayList<>();

        StandardRecipeLoadResult() { }

        private void setNative(int nativelyRegistered) {
            this.nativelyRegistered = nativelyRegistered;
        }

        @Override
        public int getNative() {
            return nativelyRegistered;
        }

        private void setThirdParty(int thirdPartyRegistered) {
            this.thirdPartyRegistered = thirdPartyRegistered;
        }

        @Override
        public int getThirdParty() {
            return thirdPartyRegistered;
        }

        private void setTimeToComplete(long timeToComplete) {
            this.timeToComplete = timeToComplete;
        }

        @Override
        public long getTimeToComplete() {
            return timeToComplete;
        }

        private void addFailureInfo(@NotNull RecipeLoadFailureReport report) {
            this.failures.add(report);
        }

        @NotNull
        @Override
        public List<@NotNull RecipeLoadFailureReport> getFailures() {
            return Collections.unmodifiableList(failures);
        }

    }

}
