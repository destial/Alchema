package wtf.choco.alchema.integration.mythicmobs;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wtf.choco.alchema.Alchema;
import wtf.choco.alchema.crafting.CauldronIngredient;
import wtf.choco.alchema.util.ItemUtil;

import java.util.Map;
import java.util.Objects;

/**
 * A {@link CauldronIngredient} implementation wrapped around an {@link MMOItem}.
 * The MMO item must match in type and in id. Neither rarities nor levels are taken
 * into consideration.
 *
 * @author Parker Hawke - Choco
 */
public final class CauldronIngredientMythicItem implements CauldronIngredient {

    public static NamespacedKey key; // Set by PluginIntegrationMMOItems

    private final MythicItem mythicItem;
    private final ItemStack item;
    private Map<Attribute, AttributeModifier> modifiers;

    /**
     * Construct a new {@link CauldronIngredientMythicItem} with a given amount and {@link ItemStack}.
     *
     * @param mythicItem the MythicItem instance
     * @param item the ItemStack representation of the MythicItem
     * @param amount the amount
     */
    public CauldronIngredientMythicItem(@NotNull MythicItem mythicItem, @NotNull ItemStack item, int amount) {
        this.mythicItem = mythicItem;
        this.item = item;
        this.item.setAmount(amount);
    }

    /**
     * Construct a new {@link CauldronIngredientMythicItem} deserialized from the
     * provided {@link JsonObject}.
     *
     * @param object the object from which to deserialize
     */
    public CauldronIngredientMythicItem(@NotNull JsonObject object) {
        if (!object.has("id")) {
            throw new JsonParseException("Missing element \"id\"");
        }

        this.mythicItem = MythicBukkit.inst().getItemManager().getItem(object.get("id").getAsString()).orElse(null);
        if (mythicItem == null) {
            throw new JsonParseException("MythicItems item id \"" + object.get("id").getAsString() + "\"");
        }

        this.item = BukkitAdapter.adapt(mythicItem.generateItemStack(object.has("amount") ? object.get("amount").getAsInt() : 1));

        if (object.has("modifiers")) {
            this.modifiers = ItemUtil.parseModifiers(object.getAsJsonObject("modifiers"));
        }
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

    @NotNull
    @Override
    public ItemStack asItemStack() {
        return item;
    }

    @Override
    public boolean isSimilar(@NotNull CauldronIngredient other) {
        if (!(other instanceof CauldronIngredientMythicItem ingredient)) {
            return false;
        }

        return mythicItem.getInternalName().equalsIgnoreCase(ingredient.mythicItem.getInternalName());
    }

    @NotNull
    @Override
    public CauldronIngredient merge(@NotNull CauldronIngredient other) {
        Preconditions.checkArgument(other instanceof CauldronIngredientMythicItem, "Cannot merge %s with %s", getClass().getName(), other.getClass().getName());
        CauldronIngredientMythicItem m = new CauldronIngredientMythicItem(mythicItem, item, getAmount() + other.getAmount());
        m.setModifiers(modifiers);
        return m;
    }

    @NotNull
    @Override
    public CauldronIngredient adjustAmountBy(int amount) {
        Preconditions.checkArgument(amount < getAmount(), "amount must be < getAmount(), %d", getAmount());
        CauldronIngredientMythicItem m = new CauldronIngredientMythicItem(mythicItem, item, getAmount() + amount);
        m.setModifiers(modifiers);
        return m;
    }

    @Override
    @NotNull
    public String describe() {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = BukkitAdapter.adapt(mythicItem.generateItemStack(1)).getItemMeta();
            if (meta == null) { // Last attempt. Just going based on data we have available I suppose
                return getAmount() + "x " + StringUtils.capitalize(mythicItem.getInternalName().replace('_', ' ').toLowerCase());
            }

            return getAmount() + "x " + meta.getDisplayName();
        }

        return getAmount() + "x " + ChatColor.stripColor(meta.getDisplayName());
    }

    @NotNull
    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();

        object.addProperty("id", mythicItem.getInternalName());
        object.addProperty("amount", getAmount());
        if (modifiers != null)
            object.add("modifiers", ItemUtil.toJsonModifiers(modifiers));

        return object;
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

        if (!(obj instanceof CauldronIngredientMythicItem other)) {
            return false;
        }

        return getAmount() == other.getAmount() && mythicItem.getInternalName().equalsIgnoreCase(other.mythicItem.getInternalName());
    }

    @Override
    public String toString() {
        return String.format("CauldronIngredientMythicItem[amount=%s, item=%s, mythicItem=%s, modifiers=%s]", getAmount(), item, mythicItem, modifiers);
    }

    @Nullable
    @Override
    public Map<Attribute, AttributeModifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(Map<Attribute, AttributeModifier> modifiers) {
        this.modifiers = modifiers;
    }
}
