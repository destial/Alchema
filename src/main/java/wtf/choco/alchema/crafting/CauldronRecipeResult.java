package wtf.choco.alchema.crafting;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Represents a {@link CauldronRecipe} result.
 */
public interface CauldronRecipeResult extends Supplier<ItemStack> {

    /**
     * Get the key for this recipe result type.
     *
     * @return the ingredient key
     */
    @NotNull NamespacedKey getKey();

    /**
     * Get the amount of this result.
     *
     * @return the result amount
     */
    int getAmount();

    /**
     * Get this result represented as an {@link ItemStack}.
     *
     * @return the item stack
     */
    @NotNull ItemStack asItemStack();

    @NotNull
    @Override
    default ItemStack get() {
        return asItemStack();
    }

}
