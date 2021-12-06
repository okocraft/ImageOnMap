/*
 * Copyright or © or Copr. QuartzLib contributors (2015 - 2020)
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.zcraft.quartzlib.tools.items;

import fr.zcraft.quartzlib.tools.reflection.NMSException;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import fr.zcraft.quartzlib.tools.runners.RunTask;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//import org.bukkit.Sound;

/**
 * Utility class for dealing with items and inventories.
 */
public abstract class ItemUtils {
    private static String getI18nNameMethodName = null;
    private static Method registryLookupMethod = null;

    private ItemUtils() {
    }

    /**
     * Simulates the player consuming the ItemStack in their main hand, depending on
     * their game mode. This decreases the ItemStack's size by one, and replaces
     * it with air if nothing is left.
     *
     * @param player The player that will consume the stack.
     * @return The updated ItemStack.
     */
    public static ItemStack consumeItemInMainHand(Player player) {
        ItemStack newStack = consumeItem(player, player.getInventory().getItemInMainHand());
        player.getInventory().setItemInMainHand(newStack);
        return newStack;
    }

    /**
     * Simulates the player consuming the ItemStack in their main hand, depending on
     * their game mode. This decreases the ItemStack's size by one, and replaces
     * it with air if nothing is left.
     *
     * @param player The player that will consume the stack.
     * @return The updated ItemStack.
     */
    public static ItemStack consumeItemInOffHand(Player player) {
        ItemStack newStack = consumeItem(player, player.getInventory().getItemInOffHand());
        player.getInventory().setItemInOffHand(newStack);
        return newStack;
    }

    /**
     * Simulates the player consuming the given itemstack, depending on his game
     * mode. This decreases the ItemStack's size by one, and replaces it with
     * air if nothing is left.
     *
     * @param player The player that will consume the stack.
     * @param item   The stack to be consumed.
     * @return The updated stack.
     */
    public static ItemStack consumeItem(Player player, ItemStack item) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return item;
        }

        int amount = item.getAmount();
        if (amount == 1) {
            item.setType(Material.AIR);
        } else {
            item.setAmount(amount - 1);
        }
        return item;
    }

    /**
     * Emulates the behaviour of the /give command.
     *
     * @param player The player to give the item to.
     * @param item   The item to give to the player
     * @return true if the player received the item in its inventory, false if
     *              it had to be totally or partially dropped on the ground.
     */
    public static boolean give(final Player player, final ItemStack item) {
        final Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);

        // Not everything fit.
        if (!leftover.isEmpty()) {
            for (final ItemStack leftOverItem : leftover.values()) {
                drop(player.getLocation(), leftOverItem);
            }

            return false;
        } else {
            //Sounds will be added later
            //player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 0.2f, 1.8f);
            return true;
        }
    }

    /**
     * Shortcut method to set the display name of an item.
     *
     * @param item        The item
     * @param displayName The new display name of the item
     * @return The same item.
     */
    public static ItemStack setDisplayName(ItemStack item, String displayName) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if two item stacks are similar, by checking the type, data and display name (if set).
     *
     * @param first An item stack. Can be {@code null}.
     * @param other Another item stack. Can be {@code null}.
     * @return {@code true} if similar (either both {@code null} or similar).
     */
    public static boolean areSimilar(ItemStack first, ItemStack other) {
        if (first == null || other == null) {
            return first == other;
        }

        return first.getType() == other.getType()
                && first.getData().equals(other.getData())
                && ((!first.hasItemMeta() && !other.hasItemMeta())
                || (!first.getItemMeta().hasDisplayName() && !other.getItemMeta().hasDisplayName())
                || (first.getItemMeta().getDisplayName().equals(other.getItemMeta().getDisplayName())));
    }

    /**
     * Checks if an item stack have the given display name.
     *
     * @param stack       An item stack. Can be {@code null}.
     * @param displayName A display name.
     * @return {@code true} if the item stack have the given display name.
     */
    public static boolean hasDisplayName(ItemStack stack, String displayName) {
        return stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
                && stack.getItemMeta().getDisplayName().equals(displayName);
    }

    /**
     * Emulates the item in the player loosing durability as if the given player was using it.
     * If the player is in creative mode, the item won't be damaged at all.
     * The Unbreaking enchantments are taken into account.
     * The player's inventory is also updated, if needed.
     *
     * @param player The player that is using the item.
     * @param item   The item in the player's hand.
     * @param factor The amount of damage taken.
     * @return `true` if the damaged item was broken, `false` otherwise.
     */
    public static boolean damageItem(@NotNull Player player, @NotNull ItemStack item, int factor) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (!(meta instanceof Damageable)) {
            return false;
        }

        int newDurability = ((Damageable) meta).getDamage();
        newDurability += newDurability(item.getEnchantmentLevel(Enchantment.DURABILITY)) * factor;

        if (newDurability >= item.getType().getMaxDurability()) {
            InventoryUtils.breakItemInHand(player, item);
            player.updateInventory();
            return true;
        } else {
            ((Damageable) meta).setDamage(newDurability);
            item.setItemMeta(meta);
            player.updateInventory();
            return false;
        }
    }

    /**
     * Calculates the new durability, taking into account the unbreaking
     * enchantment.
     *
     * @param unbreakingLevel The Unbreaking level (0 if not enchanted with
     *                        that).
     * @return The durability to add.
     */
    private static short newDurability(int unbreakingLevel) {
        if (new Random().nextInt(100) <= (100 / (unbreakingLevel + 1))) {
            return 1;
        }

        return 0;
    }

    /**
     * Returns the name of the method used to retrieve the I18N key of the name
     * of an item from a NMS ItemStack.
     *
     * @param item An item stack.
     * @return The name of the method. This result is cached.
     * @throws NMSException if the operation cannot be executed.
     */
    private static String getI18nNameMethod(ItemStack item) throws NMSException {
        if (getI18nNameMethodName != null) {
            return getI18nNameMethodName;
        }

        try {
            Class<?> minecraftItemClass = Reflection.getMinecraftClassByName("world.item.Item");
            Class<?> minecraftItemStackClass = Reflection.getMinecraftClassByName("world.item.ItemStack");
            Class<?> craftItemStackClass = Reflection.getBukkitClassByName("inventory.CraftItemStack");
            Object itemStackHandle = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
            Object minecraftItem = Reflection.getFieldValue(itemStackHandle, "item");
            List<Method> allMethods = Reflection.findAllMethods(
                    minecraftItemClass, null, String.class, 0, minecraftItemStackClass);

            for (Method method : allMethods) {
                String result = (String) Reflection.call(minecraftItem, method.getName(), itemStackHandle);
                if (result == null) {
                    continue;
                }
                if (!(result.startsWith("item.") || result.startsWith("tile.") || result.startsWith("potion."))) {
                    continue;
                }

                getI18nNameMethodName = method.getName();
                return getI18nNameMethodName;
            }

            throw new NMSException("Unable to retrieve Minecraft I18n name: no method found");
        } catch (Exception ex) {
            throw new NMSException("Unable to retrieve Minecraft I18n name", ex);
        }
    }

    /**
     * Retrieves the method used to lookup the Minecraft internal item registry,
     * used to get the internal names of the Minecraft items (like {@code minecraft.stone}.
     *
     * @return The method. This result is cached.
     * @throws NMSException if the operation cannot be executed.
     */
    private static Method getRegistryLookupMethod() throws NMSException {
        if (registryLookupMethod != null) {
            return registryLookupMethod;
        }

        try {
            Class minecraftItem = Reflection.getMinecraftClassByName("world.item.Item");
            Object materialsRegistry = Reflection.getFieldValue(minecraftItem, null, "REGISTRY");

            Method getMethod = Reflection.findMethod(materialsRegistry.getClass(), "get", (Type) null);

            if (getMethod == null) {
                throw new NMSException("Method RegistryMaterials.get() not found.");
            }

            registryLookupMethod = Reflection.findMethod(
                    materialsRegistry.getClass(),
                    "!get",
                    getMethod.getGenericParameterTypes()[0],
                    0,
                    getMethod.getGenericReturnType());

            if (registryLookupMethod == null) {
                throw new NMSException("Method RegistryMaterials.lookup() not found.");
            }

            return registryLookupMethod;
        } catch (Exception ex) {
            throw new NMSException("Unable to retreive Minecraft ID", ex);
        }
    }

    /**
     * Returns the Minecraft internal ID of an ItemStack.
     * <p>
     * As example, the ID of a {@link Material#STONE Material.STONE} item is {@code minecraft.stone}.
     * This ID is needed to include items in JSON-formatted messages.
     * </p>
     *
     * @param item An item.
     * @return The Minecraft name of this item, or null if the item's material
     *         is invalid.
     * @throws NMSException if the operation cannot be executed.
     */
    public static String getMinecraftId(ItemStack item) throws NMSException {
        try {
            Object craftItemStack = asNMSCopy(item);
            if (craftItemStack == null) {
                return null;
            }

            Object minecraftItem = Reflection.getFieldValue(craftItemStack, "item");
            Class<?> minecraftItemClass = Reflection.getMinecraftClassByName("world.item.Item");
            Object itemsRegistry = Reflection.getFieldValue(minecraftItemClass, null, "REGISTRY");

            Object minecraftKey = getRegistryLookupMethod().invoke(itemsRegistry, minecraftItem);

            return minecraftKey.toString();
        } catch (Exception ex) {
            throw new NMSException("Unable to retrieve Minecraft ID for an ItemStack", ex);
        }
    }

    /**
     * Retrieves the key to use in the {@code translate} property of a JSON message to translate
     * the name of the given ItemStack.
     *
     * @param item An item.
     * @return The I18N key for this item.
     * @throws NMSException if the operation cannot be executed.
     */
    public static String getI18nName(ItemStack item) throws NMSException {
        if (item.getItemMeta() instanceof PotionMeta) {
            return getI18nPotionName(item);
        }

        try {
            Object craftItemStack = asNMSCopy(item);
            Object minecraftItem = Reflection.getFieldValue(craftItemStack, "item");
            return Reflection.call(minecraftItem, getI18nNameMethod(item), craftItemStack) + ".name";
        } catch (Exception ex) {
            throw new NMSException("Unable to retrieve Minecraft I18n name", ex);
        }
    }

    /**
     * Copies the ItemStack in a new net.minecraft.server.ItemStack.
     *
     * @param item An item.
     * @return A copy of this item as a net.minecraft.server.ItemStack object.
     * @throws NMSException if the operation cannot be executed.
     */
    public static Object asNMSCopy(ItemStack item) throws NMSException {
        try {
            Class<?> craftItemStack = Reflection.getBukkitClassByName("inventory.CraftItemStack");
            return craftItemStack.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
        } catch (Exception ex) {
            throw new NMSException("Unable to retreive NMS copy", ex);
        }
    }

    /**
     * Copies the ItemStack in a new CraftItemStack.
     *
     * @param item An item.
     * @return A copy of this item in a CraftItemStack object.
     * @throws NMSException if the operation cannot be executed.
     */
    public static Object asCraftCopy(ItemStack item) throws NMSException {
        try {
            Class<?> craftItemStack = Reflection.getBukkitClassByName("inventory.CraftItemStack");
            return craftItemStack.getMethod("asCraftCopy", ItemStack.class).invoke(null, item);
        } catch (Exception ex) {
            throw new NMSException("Unable to retreive Craft copy", ex);
        }
    }

    /**
     * Returns a NMS ItemStack for the given item.
     *
     * @param item An item.
     * @return A NMS ItemStack for this item. If the item was a CraftItemStack,
     *         this will be the item's handle directly; in the other cases, a copy in a
     *         NMS ItemStack object.
     * @throws NMSException if the operation cannot be executed.
     */
    public static Object getNMSItemStack(ItemStack item) throws NMSException {
        try {
            Class<?> craftItemStack = Reflection.getBukkitClassByName("inventory.CraftItemStack");
            return craftItemStack.isAssignableFrom(item.getClass())
                    ? Reflection.getFieldValue(craftItemStack, item, "handle")
                    : craftItemStack.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
        } catch (Exception ex) {
            throw new NMSException("Unable to retrieve NMS copy", ex);
        }
    }

    /**
     * Returns a CraftItemStack for the given item.
     *
     * @param item An item.
     * @return A CraftItemStack for this item. If the item was initially a
     *         CraftItemStack, it is returned directly. In the other cases,
     *         a copy in a new CraftItemStack will be returned.
     * @throws NMSException if the operation cannot be executed.
     */
    public static Object getCraftItemStack(ItemStack item) throws NMSException {
        try {
            Class<?> craftItemStack = Reflection.getBukkitClassByName("inventory.CraftItemStack");
            return craftItemStack.isAssignableFrom(item.getClass()) ? craftItemStack.cast(item) : asCraftCopy(item);
        } catch (Exception ex) {
            throw new NMSException("Unable to retrieve CraftItemStack copy", ex);
        }
    }

    private static String getI18nPotionName(ItemStack item) throws NMSException {
        String potionKey = getI18nPotionKey(item);

        try {
            Class<?> potionUtil = Reflection.getMinecraftClassByName("world.item.alchemy.PotionUtil");
            Class<?> potionRegistry = Reflection.getMinecraftClassByName("world.item.alchemy.PotionRegistry");
            Class<?> itemStackClass = Reflection.getMinecraftClassByName("world.item.ItemStack");
            Object registry = Reflection.findMethod(potionUtil, null, potionRegistry, 0, itemStackClass)
                    .invoke(null, asNMSCopy(item));

            return (String) Reflection.findMethod(potionRegistry, null, String.class, 0, String.class)
                    .invoke(registry, potionKey);
        } catch (Exception ex) {
            throw new NMSException("Unable to retrieve Minecraft I18n name", ex);
        }
    }

    private static String getI18nPotionKey(ItemStack item) {
        switch (item.getType()) {
            case SPLASH_POTION:
                return "splash_potion.effect.";
            case LINGERING_POTION:
                return "lingering_potion.effect.";
            case POTION:
            default:
                return "potion.effect";
        }
    }

    /**
     * Drops the item at the given location.
     *
     * @param location The location to drop the item at.
     * @param item     The item to drop.
     */
    public static void drop(Location location, ItemStack item) {
        location.getWorld().dropItem(location, item);
    }

    /**
     * Naturally "drops" the item at the given location.
     *
     * @param location The location to drop the item at.
     * @param item     The item to drop.
     */
    public static void dropNaturally(Location location, ItemStack item) {
        location.getWorld().dropItemNaturally(location, item);
    }

    /**
     * Naturally "drops" the item at the given location, at the next server tick.
     *
     * @param location The location to drop the item at.
     * @param item     The item to drop.
     */
    public static void dropNaturallyLater(Location location, ItemStack item) {
        RunTask.nextTick(() -> dropNaturally(location, item));
    }

    /**
     * Drops the item at the given location, at the next server tick.
     *
     * @param location The location to drop the item at.
     * @param item     The item to drop.
     */
    public static void dropLater(final Location location, final ItemStack item) {
        RunTask.nextTick(() -> drop(location, item));
    }

    /**
     * Converts a chat color to its dye equivalent.
     *
     * <p>The transformation is not perfect as there is no 1:1
     * correspondence between dyes and chat colors.</p>
     *
     * @param color The chat color.
     * @return The corresponding dye, or an empty value if none match (e.g. for formatting codes, of for {@code null}).
     */
    @Contract(pure = true)
    public static Optional<DyeColor> asDye(@Nullable final ChatColor color) {
        if (color == null) {
            return Optional.empty();
        }

        switch (color) {
            case BLACK:
                return Optional.of(DyeColor.BLACK);

            case BLUE:
            case DARK_BLUE:
                return Optional.of(DyeColor.BLUE);

            case DARK_GREEN:
                return Optional.of(DyeColor.GREEN);

            case DARK_AQUA:
                return Optional.of(DyeColor.CYAN);

            case DARK_RED:
                return Optional.of(DyeColor.RED);

            case DARK_PURPLE:
                return Optional.of(DyeColor.PURPLE);

            case GOLD:
            case YELLOW:
                return Optional.of(DyeColor.YELLOW);

            case GRAY:
                return Optional.of(DyeColor.LIGHT_GRAY);

            case DARK_GRAY:
                return Optional.of(DyeColor.GRAY);

            case GREEN:
                return Optional.of(DyeColor.LIME);

            case AQUA:
                return Optional.of(DyeColor.LIGHT_BLUE);

            case RED:
                return Optional.of(DyeColor.ORANGE);

            case LIGHT_PURPLE:
                return Optional.of(DyeColor.PINK);

            case WHITE:
                return Optional.of(DyeColor.WHITE);

            // White, reset & formatting
            default:
                return Optional.empty();
        }
    }

    /**
     * Converts a dye color to a dyeable material.
     *
     * @param material The colorable material to colorize.
     * @param color    The dye color.
     * @return The corresponding material.
     */
    @Contract(pure = true)
    public static Material colorize(@NotNull final ColorableMaterial material, @NotNull final DyeColor color) {
        return Material.valueOf(color.name() + "_" + material.name());
    }

    /**
     * Converts a chat color to a dyeable material.
     *
     * @param material The colorable material to colorize.
     * @param color    The chat color.
     * @return The corresponding material. If the chat color was not convertible to a dye, {@code ChatColor#WHITE} is
     *         used.
     */
    @Contract(pure = true)
    public static Material colorize(@NotNull final ColorableMaterial material, @NotNull final ChatColor color) {
        return colorize(material, asDye(color).orElse(DyeColor.WHITE));
    }
}
