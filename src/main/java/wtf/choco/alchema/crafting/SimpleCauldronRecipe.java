package wtf.choco.alchema.crafting;

import com.google.common.base.Preconditions;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wtf.choco.alchema.util.AlchemaConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A simple implementation of {@link CauldronRecipe}.
 *
 * @author Parker Hawke
 */
class SimpleCauldronRecipe implements CauldronRecipe {

    private final NamespacedKey key;
    private final CauldronRecipeResult result;

    private final String name, description, comment;
    private final String craftingPermission;
    private final int experience;

    private final List<CauldronIngredient> ingredients = new ArrayList<>();

    SimpleCauldronRecipe(@NotNull NamespacedKey key, @NotNull CauldronRecipeResult result, @Nullable String name, @Nullable String description, @Nullable String comment, int experience, @NotNull List<@NotNull CauldronIngredient> ingredients) {
        Preconditions.checkArgument(key != null, "key must not be null");
        Preconditions.checkArgument(result != null, "result must not be null");

        this.key = key;
        this.result = result;
        this.experience = experience;

        this.name = name;
        this.description = description;
        this.comment = comment;
        this.craftingPermission = AlchemaConstants.PERMISSION_CRAFT + "." + key.getNamespace() + "." + key.getKey().replace('/', '.'); // e.g. "alchema.craft.alchema.glowstone_dust"

        this.ingredients.addAll(ingredients);
    }

    @NotNull
    @Override
    public NamespacedKey getKey() {
        return key;
    }

    @NotNull
    @Override
    public CauldronRecipeResult getRecipeResult() {
        return result;
    }

    @NotNull
    @Override
    public Optional<@NotNull String> getName() {
        return Optional.ofNullable(name);
    }

    @NotNull
    @Override
    public Optional<@NotNull String> getDescription() {
        return Optional.ofNullable(description);
    }

    @NotNull
    @Override
    public Optional<@NotNull String> getComment() {
        return Optional.ofNullable(comment);
    }

    @NotNull
    @Override
    public String getCraftingPermission() {
        return craftingPermission;
    }

    @Override
    public int getExperience() {
        return experience;
    }

    @Override
    public boolean hasIngredient(@NotNull CauldronIngredient ingredient) {
        Preconditions.checkArgument(ingredient != null, "ingredient must not be null");

        for (CauldronIngredient recipeIngredient : ingredients) {
            if (recipeIngredient.isSimilar(ingredient)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    @Override
    public List<@NotNull CauldronIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    @Override
    public int getComplexity() {
        int complexity = 0;

        for (CauldronIngredient ingredient : ingredients) {
            complexity += ingredient.getComplexity();
        }

        return complexity;
    }

    @Override
    public int getYieldFromIngredients(@NotNull List<@NotNull CauldronIngredient> availableIngredients) {
        int yield = 0;
        boolean initialFind = true;

        // This can probably be done a lot better...
        for (CauldronIngredient requiredIngredient : ingredients) {
            CauldronIngredient availableIngredient = null;

            for (CauldronIngredient localIngredient : availableIngredients) {
                if (!requiredIngredient.isSimilar(localIngredient)) {
                    continue;
                }

                int requiredCount = requiredIngredient.getAmount();
                int availableCount = localIngredient.getAmount();

                if (availableCount >= requiredCount) {
                    availableIngredient = localIngredient;
                    break;
                }
            }

            if (availableIngredient == null) {
                return 0;
            }

            // Compute yield
            int requiredCount = requiredIngredient.getAmount();
            int availableCount = availableIngredient.getAmount();
            yield = initialFind ? availableCount / requiredCount : Math.min(availableCount / requiredCount, yield);

            initialFind = false;
        }

        return yield;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, experience, ingredients, name, description, comment, result);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof SimpleCauldronRecipe other)) {
            return false;
        }

        return experience == other.experience && Objects.equals(key, other.key) && Objects.equals(comment, other.comment)
                && Objects.equals(name, other.name) && Objects.equals(description, other.description)
                && Objects.equals(result, other.result) && Objects.equals(ingredients, other.ingredients);
    }

    @Override
    public String toString() {
        return String.format("SimpleCauldronRecipe[key=%s, comment=%s]", key, comment);
    }

}
