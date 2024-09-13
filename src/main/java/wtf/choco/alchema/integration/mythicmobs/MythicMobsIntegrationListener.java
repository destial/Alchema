package wtf.choco.alchema.integration.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import wtf.choco.alchema.Alchema;
import wtf.choco.alchema.api.event.CauldronIngredientAddEvent;
import wtf.choco.alchema.api.event.CauldronItemCraftEvent;
import wtf.choco.alchema.api.event.CauldronRecipeRegisterEvent;
import wtf.choco.alchema.api.event.player.PlayerEssenceCollectEvent;
import wtf.choco.alchema.crafting.CauldronIngredient;
import wtf.choco.alchema.crafting.CauldronIngredientItemStack;
import wtf.choco.alchema.util.AlchemaConstants;
import wtf.choco.alchema.util.ItemUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Event listeners for MMOItems plugin integration.
 *
 * @author Parker Hawke - Choco
 */
public final class MythicMobsIntegrationListener implements Listener {
    private final PluginIntegrationMythicItems integration;
    private final MythicBukkit mythic;
    MythicMobsIntegrationListener(@NotNull PluginIntegrationMythicItems integration) {
        this.integration = integration;
        this.mythic = integration.getIntegratedPlugin();
    }

    @EventHandler
    private void onRecipeRegister(CauldronRecipeRegisterEvent event) {
        integration.registerUpgrades();
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
        if (!(event.getRecipe().getRecipeResult() instanceof CauldronRecipeResultMythicItem result)) {
            return;
        }

        ItemStack item = result.asItemStack();
        String type = mythic.getItemManager().getMythicTypeFromItem(item);
        if (type == null)
            return;

        MythicItem mythicItem = mythic.getItemManager().getItem(type).orElse(null);
        if (mythicItem == null)
            return;

        if (event.getRecipe().getIngredients().stream().anyMatch(i -> i.getModifiers() != null)) {
            CauldronIngredient original = event.getCauldron().getIngredients().stream().filter(i -> i.getModifiers() == null).findFirst().orElse(null);
            if (original != null)
                item = original.asItemStack();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean updated = false;
            int upgrades = meta.getPersistentDataContainer().getOrDefault(AlchemaConstants.UPGRADE_KEY, PersistentDataType.INTEGER, 0);
            if (upgrades > ItemUtil.getMaxUpgrades(event.getPlayer())) {
                Alchema.getInstance().getLogger().info("Max upgrades!");
                event.setCancelled(true);
                return;
            }

            for (CauldronIngredient ingredient : event.getRecipe().getIngredients()) {
                if (ingredient instanceof CauldronIngredientMythicItem mythicIngredient && mythicIngredient.getModifiers() != null) {
                    for (Map.Entry<Attribute, AttributeModifier> entry : mythicIngredient.getModifiers().entrySet()) {
                        Attribute attribute = entry.getKey();
                        AttributeModifier additiveMod = entry.getValue();
                        Collection<AttributeModifier> current = meta.getAttributeModifiers(attribute);
                        if (current == null || current.isEmpty()) {
                            List<EquipmentSlot> slots = new ArrayList<>();
                            if (item.getType().name().endsWith("_BOOTS")) {
                                slots.add(EquipmentSlot.FEET);
                            }
                            if (item.getType().name().endsWith("_LEGGINGS")) {
                                slots.add(EquipmentSlot.LEGS);
                            }
                            if (item.getType().name().endsWith("_CHESTPLATE") || item.getType().name().endsWith("ELYTRA")) {
                                slots.add(EquipmentSlot.CHEST);
                            }
                            if (item.getType().name().endsWith("_HELMET") || item.getType() == Material.CARVED_PUMPKIN) {
                                slots.add(EquipmentSlot.HEAD);
                            }

                            if (item.getType().name().endsWith("_SWORD") || item.getType().name().endsWith("_AXE") || item.getType().name().endsWith("BOW")
                                || item.getType() == Material.BLAZE_ROD || item.getType() == Material.STICK || item.getType() == Material.BAMBOO) {
                                slots.add(EquipmentSlot.HAND);
                                slots.add(EquipmentSlot.OFF_HAND);
                            }
                            for (EquipmentSlot slot : slots) {
                                AttributeModifier modifier = new AttributeModifier(additiveMod.getUniqueId(), additiveMod.getName(), additiveMod.getAmount(), additiveMod.getOperation(), slot);
                                meta.addAttributeModifier(attribute, modifier);
                            }
                            updated = true;
                            continue;
                        }

                        for (AttributeModifier currentMod : current) {
                            if (currentMod.getAmount() < 0)
                                continue;
                            meta.removeAttributeModifier(attribute, currentMod);
                            double base = getBase(mythicIngredient, currentMod, additiveMod);
                            upgrades += mythicIngredient.getAmount();
                            meta.getPersistentDataContainer().remove(AlchemaConstants.UPGRADE_KEY);
                            meta.getPersistentDataContainer().set(AlchemaConstants.UPGRADE_KEY, PersistentDataType.INTEGER, upgrades);
                            AttributeModifier afterMod = new AttributeModifier(currentMod.getUniqueId(), currentMod.getName(), base, currentMod.getOperation(), currentMod.getSlot());
                            meta.addAttributeModifier(attribute, afterMod);
                        }
                        updated = true;
                    }
                }
            }
            if (updated) {
                item.setItemMeta(meta);
            }
        }

        event.setRecipeResult(new CauldronRecipeResultMythicItem(mythicItem, item, item.getAmount()));
    }

    private double getBase(CauldronIngredientMythicItem mythicIngredient, AttributeModifier currentMod, AttributeModifier additiveMod) {
        double base = currentMod.getAmount();
        switch (additiveMod.getOperation()) {
            case ADD_NUMBER -> {
                for (int i = 0; i < mythicIngredient.getAmount(); i++) {
                    base += additiveMod.getAmount();
                }
            }
            case ADD_SCALAR -> {
                for (int i = 0; i < mythicIngredient.getAmount(); i++) {
                    base *= additiveMod.getAmount();
                }
            }
        }
        return base;
    }
}
