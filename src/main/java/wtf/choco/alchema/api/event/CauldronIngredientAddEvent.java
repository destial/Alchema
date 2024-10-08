package wtf.choco.alchema.api.event;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wtf.choco.alchema.cauldron.AlchemicalCauldron;
import wtf.choco.alchema.crafting.CauldronIngredient;
import wtf.choco.alchema.crafting.CauldronIngredientItemStack;

import java.util.UUID;

/**
 * Called when an {@link Item} consumed by an {@link AlchemicalCauldron} and an ingredient
 * is added to the cauldron.
 * <p>
 * By default, a {@link CauldronIngredientItemStack} is added. This event should be used
 * to change the implementation inserted into the cauldron for more accurate recipe
 * calculation.
 *
 * @author Parker Hawke - Choco
 */
public class CauldronIngredientAddEvent extends CauldronEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private CauldronIngredient ingredient;

    private final Item item;
    private final ItemStack itemStack;
    private final OfflinePlayer player;
    private final UUID playerUUID;
    private boolean cancelled;

    /**
     * Construct a new {@link CauldronIngredientAddEvent}.
     *
     * @param cauldron the cauldron that caused the craft
     * @param ingredient the ingredient to be added to the cauldron
     * @param item the item thrown into the cauldron
     */
    public CauldronIngredientAddEvent(@NotNull AlchemicalCauldron cauldron, @NotNull CauldronIngredient ingredient, @NotNull Item item) {
        super(cauldron);

        Preconditions.checkArgument(ingredient != null, "ingredient must not be null");

        this.ingredient = ingredient;
        this.item = item;
        this.itemStack = item.getItemStack();

        this.playerUUID = item.getThrower();
        this.player = (playerUUID != null) ? Bukkit.getOfflinePlayer(playerUUID) : null;
    }

    /**
     * Set the {@link CauldronIngredient} implementation to be added to the cauldron.
     * <p>
     * By default, a {@link CauldronIngredientItemStack} is added to an {@link AlchemicalCauldron}
     * when an {@link Item} is dropped inside. This method should be used to change the
     * implementation inserted into the cauldron for more accurate recipe calculation.
     *
     * @param ingredient the ingredient to set
     */
    public void setIngredient(@NotNull CauldronIngredient ingredient) {
        Preconditions.checkArgument(ingredient != null, "ingredient must not be null");

        this.ingredient = ingredient;
    }

    /**
     * Get the {@link CauldronIngredient} implementation to be added to the cauldron.
     *
     * @return the ingredient
     */
    @NotNull
    public CauldronIngredient getIngredient() {
        return ingredient;
    }

    /**
     * Get the {@link Item} that was thrown into the cauldron to cause this event.
     * <p>
     * <strong>NOTE:</strong> The item returned by this method will be destroyed by implementation
     * after all event listeners have been called upon. Therefore it is best not to rely on the
     * validity of the Item entity returned by this method.
     *
     * @return the item
     */
    @NotNull
    public Item getItem() {
        return item;
    }

    /**
     * Get the {@link ItemStack} that was thrown into the cauldron to cause this event. This
     * is a convenience method and is equivalent to {@code getItem().getItemStack()}.
     *
     * @return the item stack
     */
    @NotNull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Get the UUID of the player that threw the {@link Item} into the cauldron to cause
     * this event (if there is one).
     *
     * @return the thrower UUID. null if none
     */
    @Nullable
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * Get the player that threw the {@link Item} into the cauldron to cause this event
     * (if there is one).
     *
     * @return the thrower. null if none
     */
    @Nullable
    public OfflinePlayer getPlayer() {
        return player;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Get the HandlerList instance for this event.
     *
     * @return the handler list
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean caancel) {
        this.cancelled = caancel;
    }
}
