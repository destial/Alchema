package wtf.choco.alchema.integration.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import wtf.choco.alchema.api.event.CauldronIngredientAddEvent;
import wtf.choco.alchema.api.event.CauldronItemCraftEvent;
import wtf.choco.alchema.api.event.player.PlayerEssenceCollectEvent;
import wtf.choco.alchema.crafting.CauldronIngredient;
import wtf.choco.alchema.crafting.CauldronIngredientItemStack;
import wtf.choco.alchema.crafting.CauldronRecipeResultItemStack;

import java.util.Collection;
import java.util.Map;

/**
 * Event listeners for MMOItems plugin integration.
 *
 * @author Parker Hawke - Choco
 */
public final class MythicMobsIntegrationListener implements Listener {
    private final MythicBukkit mythic;
    MythicMobsIntegrationListener(@NotNull MythicBukkit mythicBukkit) {
        this.mythic = mythicBukkit;
    }

    @EventHandler
    private void onEssenceCollect(PlayerEssenceCollectEvent event) {
        if (mythic.getMobManager().isMythicMob(event.getEntity())) {
            event.setEssenceAmount(0);
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onAddMythicItemIngredient(CauldronIngredientAddEvent event) {
        if (!(event.getIngredient() instanceof CauldronIngredientItemStack ingredient)) {
            return;
        }

        ItemStack item = ingredient.asItemStack();
        if (!mythic.getItemManager().isMythicItem(item))
            return;

        String type = mythic.getItemManager().getMythicTypeFromItem(item);
        MythicItem mythicItem = mythic.getItemManager().getItem(type).orElse(null);
        if (mythicItem == null)
            return;

        event.setIngredient(new CauldronIngredientMythicItem(mythicItem, item, item.getAmount()));
    }

    @EventHandler
    private void onMythicItemCraft(CauldronItemCraftEvent event) {
        if (!(event.getRecipeResult() instanceof CauldronRecipeResultItemStack result)) {
            return;
        }

        ItemStack item = result.asItemStack();
        if (!mythic.getItemManager().isMythicItem(item))
            return;

        String type = mythic.getItemManager().getMythicTypeFromItem(item);
        MythicItem mythicItem = mythic.getItemManager().getItem(type).orElse(null);
        if (mythicItem == null)
            return;

        CauldronRecipeResultMythicItem resultItem = null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean updated = false;
            for (CauldronIngredient ingredient : event.getRecipe().getIngredients()) {
                if (ingredient instanceof CauldronIngredientMythicItem mythicIngredient && mythicIngredient.getModifiers() != null) {
                    for (Map.Entry<Attribute, AttributeModifier> entry : mythicIngredient.getModifiers().entrySet()) {
                        Attribute attribute = entry.getKey();
                        AttributeModifier additiveMod = entry.getValue();
                        Collection<AttributeModifier> current = meta.getAttributeModifiers(attribute);
                        if (current == null || current.isEmpty()) {
                            meta.addAttributeModifier(attribute, additiveMod);
                            continue;
                        }

                        for (AttributeModifier currentMod : current) {
                            meta.removeAttributeModifier(attribute, currentMod);
                            double base = currentMod.getAmount();
                            switch (additiveMod.getOperation()) {
                                case ADD_NUMBER -> base += additiveMod.getAmount();
                                case ADD_SCALAR -> base *= additiveMod.getAmount();
                            }
                            AttributeModifier afterMod = new AttributeModifier(currentMod.getUniqueId(), currentMod.getName(), base, currentMod.getOperation());
                            meta.addAttributeModifier(attribute, afterMod);
                        }
                        updated = true;
                    }
                }
            }
            if (updated) {
                item.setItemMeta(meta);
                resultItem = new CauldronRecipeResultMythicItem(mythicItem, item, item.getAmount());
            }
        }

        if (resultItem == null) {
            resultItem = new CauldronRecipeResultMythicItem(mythicItem, item.getAmount());
        }
        event.setRecipeResult(resultItem);
    }
}
