package wtf.choco.alchema.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import wtf.choco.alchema.Alchema;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A series of utilities pertaining to {@link ItemStack ItemStacks}.
 * <p>
 * <strong>NOTE:</strong>This class it not a part of Alchema's API contract and may
 * be subject to breakages without prior warning. Use of methods in this class should
 * be done at ones own risk.
 *
 * @author Parker Hawke - Choco
 */
@Internal
public final class ItemUtil {

    private ItemUtil() { }

    /**
     * Serialize an {@link ItemStack} as a byte array.
     *
     * @param itemStack the item to serialize
     *
     * @return the serialized item stack
     */
    public static byte[] serialize(@NotNull ItemStack itemStack) {
        try (ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream outputStream = new BukkitObjectOutputStream(byteArrayStream)) {
            outputStream.writeObject(itemStack);
            return byteArrayStream.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * Deserialize an {@link ItemStack} from a byte array.
     *
     * @param bytes the bytes
     *
     * @return the deserialized item stack
     */
    @NotNull
    public static ItemStack deserialize(byte[] bytes) {
        try (ByteArrayInputStream byteArrayStream = new ByteArrayInputStream(bytes);
                BukkitObjectInputStream inputStream = new BukkitObjectInputStream(byteArrayStream)) {
            Object read = inputStream.readObject();

            if (read instanceof ItemStack itemStack) {
                return itemStack;
            }
        } catch (IOException | ClassNotFoundException ignored) { }

        return new ItemStack(Material.AIR);
    }

    /**
     * Parse an {@link ItemStack} from a {@link JsonObject}. The object should contain
     * an entry for "item" which is a /give-formatted item stack string, and an optional
     * "amount" integer.
     *
     * @param object the object from which to deserialize an ItemStack
     *
     * @return the deserialized ItemStack
     */
    @NotNull
    public static ItemStack parseItemStack(@NotNull JsonObject object) {
        if (!object.has("item")) {
            throw new JsonParseException("Could not find \"item\"");
        }

        String resultString = object.get("item").getAsString();
        ItemStack result;

        try {
            result = Bukkit.getItemFactory().createItemStack(resultString);

            if (object.has("amount")) {
                result.setAmount(Math.max(object.get("amount").getAsInt(), 1));
            }
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Malformatted \"item\" input. Got: \"" + resultString + "\"");
        }

        return result;
    }

    public static int getMaxUpgrades(Player player) {
        int highestMultiplier = 1;
        if (player == null)
            return highestMultiplier;

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue())
                continue;

            String perm = info.getPermission();
            if (!perm.startsWith(AlchemaConstants.PERMISSION_UPGRADES))
                continue;

            String multi = perm.replace(AlchemaConstants.PERMISSION_UPGRADES, "");
            int m = NumberUtils.toInt(multi, 0);

            if (m > highestMultiplier) {
                highestMultiplier = m;
            }
        }
        return highestMultiplier;
    }

    public static JsonObject toJsonModifiers(Map<Attribute, AttributeModifier> modifiers) {
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entrySet()) {
            JsonObject mod = new JsonObject();
            mod.addProperty("operation", entry.getValue().getOperation() == AttributeModifier.Operation.ADD_NUMBER ? "add" : "scale");
            mod.addProperty("amount", entry.getValue().getAmount());
            jsonObject.add(entry.getKey().name().replace("GENERIC_", "").toLowerCase(), mod);
        }
        return jsonObject;
    }

    public static Map<Attribute, AttributeModifier> parseModifiers(@NotNull JsonObject object) {
        Map<Attribute, AttributeModifier> mods = new HashMap<>();
        for (String key : object.keySet()) {
            JsonObject modifier = object.getAsJsonObject(key);
            Attribute attribute;
            try {
                attribute = Attribute.valueOf("GENERIC_" + key.toUpperCase());
            } catch (IllegalArgumentException e) {
                Alchema.getInstance().getLogger().warning("Attribute of " + key + " was not found!");
                continue;
            }

            String op = modifier.get("operation").getAsString().toLowerCase();
            AttributeModifier.Operation operation = switch (op) {
                case "multiply", "multi", "scale" -> AttributeModifier.Operation.ADD_SCALAR;
                default -> AttributeModifier.Operation.ADD_NUMBER;
            };

            double amount;
            try {
                amount = modifier.get("amount").getAsDouble();
            } catch (IllegalArgumentException e) {
                Alchema.getInstance().getLogger().warning("Amount from modifier " + key + " was not found!");
                continue;
            }

            AttributeModifier mod = new AttributeModifier(attribute.name(), amount, operation);
            mods.put(attribute, mod);
        }

        return mods;
    }

}
