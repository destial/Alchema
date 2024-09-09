package wtf.choco.alchema.integration.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import wtf.choco.alchema.Alchema;
import wtf.choco.commons.integration.PluginIntegration;

/**
 * Soft integration with MythicMobs.
 *
 * @author Parker Hawke - Choco
 */
public final class PluginIntegrationMythicItems implements PluginIntegration {

    private final MythicBukkit mythicPlugin;

    /**
     * Construct a new {@link PluginIntegrationMythicItems}.
     *
     * @param plugin the MythicMobs plugin instance
     */
    public PluginIntegrationMythicItems(@NotNull Plugin plugin) {
        this.mythicPlugin = (MythicBukkit) plugin;
    }

    @NotNull
    @Override
    public Plugin getIntegratedPlugin() {
        return mythicPlugin;
    }

    @Override
    public void load() {
        Alchema alchema = Alchema.getInstance();

        CauldronIngredientMythicItem.key = new NamespacedKey(mythicPlugin, "item");
        CauldronRecipeResultMythicItem.key = CauldronIngredientMythicItem.key;
        alchema.getRecipeRegistry().registerIngredientType(CauldronIngredientMythicItem.key, CauldronIngredientMythicItem::new);
        alchema.getRecipeRegistry().registerResultType(CauldronIngredientMythicItem.key, CauldronRecipeResultMythicItem::new);
        alchema.getLogger().info("Registered foreign ingredient type: " + CauldronIngredientMythicItem.key);
        alchema.getLogger().info("Registered foreign result type: " + CauldronIngredientMythicItem.key);
    }

    @Override
    public void enable() {
        Bukkit.getPluginManager().registerEvents(new MythicMobsIntegrationListener(mythicPlugin), Alchema.getInstance());
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

}
