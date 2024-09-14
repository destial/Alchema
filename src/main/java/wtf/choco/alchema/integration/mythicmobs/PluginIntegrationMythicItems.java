package wtf.choco.alchema.integration.mythicmobs;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import wtf.choco.alchema.Alchema;
import wtf.choco.alchema.crafting.CauldronRecipe;
import wtf.choco.commons.integration.PluginIntegration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Soft integration with MythicMobs.
 *
 * @author Parker Hawke - Choco
 */
public final class PluginIntegrationMythicItems implements PluginIntegration {

    private final MythicBukkit mythicPlugin;
    private final Map<String, Map<Attribute, AttributeModifier>> modifierMap;

    /**
     * Construct a new {@link PluginIntegrationMythicItems}.
     *
     * @param plugin the MythicMobs plugin instance
     */
    public PluginIntegrationMythicItems(@NotNull Plugin plugin) {
        this.mythicPlugin = (MythicBukkit) plugin;
        modifierMap = new HashMap<>();
    }

    @NotNull
    @Override
    public MythicBukkit getIntegratedPlugin() {
        return mythicPlugin;
    }

    @Override
    public void load() {
        Alchema alchema = Alchema.getInstance();

        CauldronIngredientMythicItem.key = new NamespacedKey(mythicPlugin, "item");
        CauldronRecipeResultMythicItem.key = CauldronIngredientMythicItem.key;
        alchema.getRecipeRegistry().registerIngredientType(CauldronIngredientMythicItem.key, CauldronIngredientMythicItem::new);
        alchema.getRecipeRegistry().registerResultType(CauldronRecipeResultMythicItem.key, CauldronRecipeResultMythicItem::new);
        alchema.getLogger().info("Registered foreign ingredient type: " + CauldronIngredientMythicItem.key);
        alchema.getLogger().info("Registered foreign result type: " + CauldronRecipeResultMythicItem.key);
    }

    private void loadUpgradeRecipes(@NotNull Alchema plugin, YamlConfiguration config) {
        List<CauldronRecipe> recipes = new ArrayList<>();
        for (String key : config.getKeys(false)) {
            MythicItem upgradeItem = mythicPlugin.getItemManager().getItem(key).orElse(null);
            if (upgradeItem == null) {
                plugin.getLogger().warning("Unable to find item upgradable: " + key);
                continue;
            }

            Map<Attribute, AttributeModifier> mods = new HashMap<>();
            ConfigurationSection deep = config.getConfigurationSection(key);
            UpgradeType type = null;
            int experience = 0;
            for (String attributeName : deep.getKeys(false)) {
                ConfigurationSection modifier = deep.getConfigurationSection(attributeName);
                double amount = modifier.getDouble("amount", 0);
                if (amount == 0)
                    continue;

                experience = modifier.getInt("experience", experience);

                Attribute attribute;
                try {
                    attribute = Attribute.valueOf("GENERIC_" + attributeName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    Alchema.getInstance().getLogger().warning("Attribute of " + attributeName + " was not found!");
                    continue;
                }

                String op = modifier.getString("operation", "add");
                AttributeModifier.Operation operation = switch (op) {
                    case "multiply", "multi", "scale" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                    default -> AttributeModifier.Operation.ADD_NUMBER;
                };

                type = UpgradeType.valueOf(modifier.getString("type", "ALL"));

                AttributeModifier mod = new AttributeModifier(UUID.randomUUID(), attribute.name(), amount, operation);
                mods.put(attribute, mod);
            }

            modifierMap.put(upgradeItem.getInternalName(), mods);

            for (MythicItem mmItem : mythicPlugin.getItemManager().getItems()) {
                if (mmItem == upgradeItem)
                    continue;

                NamespacedKey namespacedKey = new NamespacedKey(plugin, (mmItem.getInternalName() + "_w_" + upgradeItem.getInternalName()).replace("ø", "o"));
                Material mat = mmItem.getMaterial();
                if (mat == null) {
                    if (mmItem.getCachedBaseItem() != null) {
                        mat = mmItem.getCachedBaseItem().getType();
                    }
                    if (mat == null) {
                        mat = Material.getMaterial(mmItem.getMaterialName().toUpperCase());
                    }
                }
                if ((mat.name().endsWith("_HELMET") ||
                     mat.name().endsWith("_CHESTPLATE") ||
                     mat.name().endsWith("_LEGGINGS") ||
                     mat.name().endsWith("_BOOTS") ||
                     mat.name().endsWith("ELYTRA")) &&
                     (type == UpgradeType.ARMOR || type == UpgradeType.ALL)) {
                    CauldronRecipe.Builder builder = CauldronRecipe.builder(namespacedKey, new CauldronRecipeResultMythicItem(mmItem, 1))
                            .experience(experience)
                            .name(mmItem.getInternalName() + "_w_" + upgradeItem.getInternalName())
                            .addIngredient(new CauldronIngredientMythicItem(mmItem, BukkitAdapter.adapt(mmItem.generateItemStack(1)), 1));

                    CauldronIngredientMythicItem upgrade = new CauldronIngredientMythicItem(upgradeItem, BukkitAdapter.adapt(upgradeItem.generateItemStack(1)), 1);
                    upgrade.setModifiers(mods);

                    recipes.add(builder.addIngredient(upgrade).build());
                }

                if ((mat.name().endsWith("_SWORD") ||
                     mat.name().endsWith("_AXE") ||
                     mat == Material.SHIELD || mat == Material.BLAZE_ROD || mat == Material.STICK || mat == Material.BAMBOO) &&
                     (type == UpgradeType.WEAPON|| type == UpgradeType.ALL)) {
                    CauldronRecipe.Builder builder = CauldronRecipe.builder(namespacedKey, new CauldronRecipeResultMythicItem(mmItem, 1))
                        .experience(experience)
                        .name(mmItem.getInternalName() + "_w_" + upgradeItem.getInternalName())
                        .addIngredient(new CauldronIngredientMythicItem(mmItem, BukkitAdapter.adapt(mmItem.generateItemStack(1)), 1));

                    CauldronIngredientMythicItem upgrade = new CauldronIngredientMythicItem(upgradeItem, BukkitAdapter.adapt(upgradeItem.generateItemStack(1)), 1);
                    upgrade.setModifiers(mods);

                    recipes.add(builder.addIngredient(upgrade).build());
                }
            }
        }

        for (CauldronRecipe recipe : recipes) {
            plugin.getRecipeRegistry().registerCauldronRecipe(recipe);
        }

        CauldronRecipe random = recipes.stream().findAny().get();
        plugin.getLogger().info(random.getIngredients().stream().map(i -> i.toJson().toString()).collect(Collectors.joining(",")));

        plugin.getLogger().info("Registered " + recipes.size() + " recipe upgradeables into items! [" + recipes.stream().map(r -> r.getName().get()).collect(Collectors.joining(",")) + "]");
    }

    public void registerUpgrades() {
        Alchema alchema = Alchema.getInstance();

        File upgradeFile = new File(alchema.getRecipesDirectory(), "upgrades.yml");
        if (upgradeFile.exists()) {
            loadUpgradeRecipes(alchema, YamlConfiguration.loadConfiguration(upgradeFile));
        }
    }

    public Map<String, Map<Attribute, AttributeModifier>> getModifierMap() {
        return modifierMap;
    }

    @Override
    public void enable() {
        mythicPlugin.getServer().getPluginManager().registerEvents(new MythicMobsIntegrationListener(this), Alchema.getInstance());
    }

    @Override
    public void disable() { }

    @Override
    public boolean isSupported() {
        try {
            Class.forName("io.lumine.mythic.core.items.MythicItem");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public enum UpgradeType {
        ARMOR,
        WEAPON,
        ALL
    }

}
