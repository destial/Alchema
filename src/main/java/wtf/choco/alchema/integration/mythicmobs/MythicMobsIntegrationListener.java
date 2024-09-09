package wtf.choco.alchema.integration.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import wtf.choco.alchema.api.event.CauldronIngredientAddEvent;
import wtf.choco.alchema.api.event.CauldronItemCraftEvent;
import wtf.choco.alchema.api.event.CauldronRecipeRegisterEvent;
import wtf.choco.alchema.api.event.player.PlayerEssenceCollectEvent;
import wtf.choco.alchema.crafting.CauldronIngredientItemStack;
import wtf.choco.alchema.crafting.CauldronRecipeResultItemStack;

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

        event.setRecipeResult(new CauldronRecipeResultMythicItem(mythicItem, item.getAmount()));
    }
}
