package wtf.choco.alchema.integration.mythicmobs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wtf.choco.alchema.crafting.CauldronIngredient;
import wtf.choco.alchema.crafting.CauldronRecipeResult;

import java.util.Objects;

/**
 * A {@link CauldronIngredient} implementation wrapped around an {@link MMOItem}.
 * The MMO item must match in type and in id. Neither rarities nor levels are taken
 * into consideration.
 *
 * @author Parker Hawke - Choco
 */
public final class CauldronRecipeResultMythicItem implements CauldronRecipeResult {

    public static NamespacedKey key; // Set by PluginIntegrationMMOItems

    private final MythicItem mythicItem;
    private final ItemStack item;

    /**
     * Construct a new {@link CauldronRecipeResultMythicItem} with a given amount. The {@link ItemStack}
     * will be newly created.
     *
     * @param mythicItem the MythicItem instance
     * @param amount the amount
     */
    public CauldronRecipeResultMythicItem(@NotNull MythicItem mythicItem, int amount) {
        this.mythicItem = mythicItem;
        this.item = BukkitAdapter.adapt(mythicItem.generateItemStack(amount));
    }

    /**
     * Construct a new {@link CauldronRecipeResultMythicItem} deserialized from the
     * provided {@link JsonObject}.
     *
     * @param object the object from which to deserialize
     */
    public CauldronRecipeResultMythicItem(@NotNull JsonObject object) {
        if (!object.has("id")) {
            throw new JsonParseException("Missing element \"id\"");
        }

        this.mythicItem = MythicBukkit.inst().getItemManager().getItem(object.get("id").getAsString()).orElse(null);
        if (mythicItem == null) {
            throw new JsonParseException("MythicItems item id \"" + object.get("id").getAsString() + "\"");
        }

        this.item = BukkitAdapter.adapt(mythicItem.generateItemStack(object.has("amount") ? object.get("amount").getAsInt() : 1));
    }

    @NotNull
    @Override
    public NamespacedKey getKey() {
        return key;
    }

    @Override
    public int getAmount() {
        return item.getAmount();
    }

    @Nullable
    @Override
    public ItemStack asItemStack() {
        return item;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAmount(), mythicItem.getInternalName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof CauldronRecipeResultMythicItem other)) {
            return false;
        }

        return getAmount() == other.getAmount() && mythicItem.getInternalName().equalsIgnoreCase(other.mythicItem.getInternalName());
    }

    @Override
    public String toString() {
        return String.format("CauldronIngredientMythicItem[amount=%s, item=%s, mythicItem=%s]", getAmount(), item, mythicItem);
    }

}
