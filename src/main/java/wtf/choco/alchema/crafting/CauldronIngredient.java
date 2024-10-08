package wtf.choco.alchema.crafting;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wtf.choco.alchema.api.event.CauldronIngredientAddEvent;
import wtf.choco.alchema.cauldron.AlchemicalCauldron;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents an ingredient usable in an {@link AlchemicalCauldron} defined by a
 * {@link CauldronRecipe}.
 * <p>
 * CauldronIngredient is meant to be extendible. Should a plugin choose to add a new
 * implementation of CauldronIngredient, the {@link CauldronIngredientAddEvent} should
 * be listened to in order to inject custom implementations into a cauldron based on
 * the item being thrown in. All implementations need to be registered with
 * {@link CauldronRecipeRegistry#registerIngredientType(NamespacedKey, Function)}
 * with the {@link #getKey()} matching that of the registered key.
 * <p>
 * It is advised that implementations also implement {@link Object#hashCode()},
 * {@link Object#equals(Object)} and {@link Object#toString()}. While not required,
 * it may improve the performance of hash-based collections.
 *
 * @author Parker Hawke - Choco
 */
public interface CauldronIngredient {

    /**
     * Get the key for this ingredient type.
     *
     * @return the ingredient key
     */
    @NotNull
    NamespacedKey getKey();

    /**
     * Get the amount of this ingredient.
     *
     * @return the ingredient amount
     */
    int getAmount();

    /**
     * Get this ingredient represented as an {@link ItemStack}, if possible.
     *
     * @return the item stack. null if no item stack representation
     */
    @Nullable
    ItemStack asItemStack();

    /**
     * Check whether this ingredient is similar to the provided ingredient. The
     * ingredient amount is not taken into consideration when comparing.
     *
     * @param other the other ingredient against which to compare
     *
     * @return true if similar, false otherwise
     */
    boolean isSimilar(@NotNull CauldronIngredient other);

    /**
     * Merge this ingredient with another ingredient. The result of this method
     * should be a new ingredient with the combined amounts of this ingredient
     * and the one passed.
     *
     * @param other the other ingredient
     *
     * @return the merged ingredient
     */
    @NotNull
    CauldronIngredient merge(@NotNull CauldronIngredient other);

    /**
     * Return a new cauldron ingredient with the amount changed by the specified
     * amount. The amount can be either negative or positive but must not result
     * in a negative or zero amount (i.e. if {@code getAmount() - amount} is 0
     * or negative, an exception will be thrown).
     *
     * @param amount the change in amount to apply
     *
     * @return the new ingredient
     */
    @NotNull
    CauldronIngredient adjustAmountBy(int amount);

    /**
     * Drop this ingredient as one or more {@link Item} from the provided cauldron.
     * <p>
     * Default implementation of this method will, if not null, drop the result of
     * {@link #asItemStack()}.
     *
     * @param cauldron the cauldron from which to drop the ingredients
     * @param world the world in which the cauldron resides
     * @param location the location at which the items should be dropped
     *
     * @return the list of Item entities that were dropped. If none, the returned
     * list should be empty, never null
     */
    @NotNull
    default List<@NotNull Item> drop(@NotNull AlchemicalCauldron cauldron, @NotNull World world, @NotNull Location location) {
        ItemStack itemStack = asItemStack();

        List<Item> droppedItems = new ArrayList<>();
        if (itemStack == null) {
            return droppedItems;
        }

        int maxStackSize = itemStack.getType().getMaxStackSize();
        for (int i = itemStack.getAmount(); i > 0; i -= maxStackSize) {
            itemStack.setAmount(Math.min(i, maxStackSize));
            droppedItems.add(world.dropItem(location, itemStack));
        }

        return droppedItems;
    }

    /**
     * Get the complexity of this ingredient. This will be used to calculate the
     * overall complexity of a {@link CauldronRecipe}.
     * <p>
     * Default implementation of this method will return {@link #getAmount()}.
     *
     * @return the ingredient complexity
     */
    default int getComplexity() {
        return getAmount();
    }

    /**
     * Describe this ingredient as a human-readable string.
     * <p>
     * Default implementation of this method will, if not null, describe the ingredient
     * as {@code (amount)x <item name>}.
     *
     * @return the description string
     */
    @NotNull
    default String describe() {
        ItemStack itemStack = asItemStack();
        return itemStack != null ? getAmount() + "x " + StringUtils.capitalize(itemStack.getType().getKey().getKey().replace('_', ' ')) : "???";
    }

    /**
     * Serialize this ingredient to a {@link JsonObject}.
     *
     * @return the serialized json
     */
    @NotNull
    JsonObject toJson();

    @Nullable
    default Map<Attribute, AttributeModifier> getModifiers() {
        return null;
    }

}
