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

package fr.zcraft.quartzlib.components.nbt;

import fr.zcraft.quartzlib.tools.items.ItemUtils;
import fr.zcraft.quartzlib.tools.reflection.NMSException;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * This class provides various utilities to manipulate NBT data.
 */
public abstract class NBT {
    static Class<?> MC_ITEM_STACK = null;
    static Class<?> MC_NBT_TAG_COMPOUND = null;


    /* ========== Item utilities ========== */
    static Class<?> CB_CRAFT_ITEM_META = null;

    private NBT() {
    }

    /**
     * Returns the NBT tag for the specified item.
     * The tag is read-write, and any modification applied to it will also be
     * applied to the item's NBT tag.
     *
     * @param item The item to get the tag from.
     * @return the NBT tag for the specified item.
     * @throws NMSException If there was any issue while assigning NBT data.
     */
    public static NBTCompound fromItemStack(ItemStack item) throws NMSException {
        // PluginLogger.info("fromItemStack");
        init();
        // PluginLogger.info("init ok");
        try {
            return new NBTCompound(getMcNBTCompound(item));
        } catch (Exception ex) {
            throw new NMSException("Unable to retrieve NBT data", ex);
        }
    }

    /**
     * Returns an NBT-like representation of the specified item meta.
     * It is useful as a fallback if you need item data as an NBT format, but
     * the actual NBT couldn't be retrieved for some reason.
     *
     * @param meta The item meta to get the data from.
     * @return an NBT-like representation of the specified item meta.
     */
    public static Map<String, Object> fromItemMeta(ItemMeta meta) {
        Map<String, Object> itemData = new HashMap<>();

        if (meta.hasDisplayName()) {
            itemData.put("Name", meta.getDisplayName());
        }

        if (meta.hasLore()) {
            itemData.put("Lore", meta.getLore());
        }

        return itemData;
    }

    /**
     * Returns an NBT-like representation of the specified enchantments.
     * It is useful as a fallback if you need item data as an NBT format, but
     * the actual NBT couldn't be retrieved for some reason.
     *
     * @param enchants the enchantment list to get the data from.
     * @return an NBT-like representation of the specified enchantments.
     */
    public static List<Map<String, Object>> fromEnchantments(Map<Enchantment, Integer> enchants) {
        List<Map<String, Object>> enchantList = new ArrayList<>();

        for (Entry<Enchantment, Integer> enchantment : enchants.entrySet()) {
            final Map<String, Object> enchantmentData = new HashMap<>();


            enchantmentData.put("key", enchantment.getKey());


            enchantmentData.put("lvl", enchantment.getValue());
            enchantList.add(enchantmentData);
        }

        return enchantList;
    }


    /* ========== Internal utilities ========== */

    /**
     * Returns an NBT-like representation of the item flags (HideFlags).
     * It is useful as a fallback if you need item data as an NBT format, but
     * the actual NBT couldn't be retrieved for some reason.
     *
     * @param itemFlags item flag set to get the data from.
     * @return an NBT-like representation of the item flags (HideFlags).
     */
    public static byte fromItemFlags(Set<ItemFlag> itemFlags) {
        byte flags = 0;

        for (ItemFlag flag : itemFlags) {
            switch (flag) {
                case HIDE_ENCHANTS -> flags += 1;
                case HIDE_ATTRIBUTES -> flags += 1 << 1;
                case HIDE_UNBREAKABLE -> flags += 1 << 2;
                case HIDE_DESTROYS -> flags += 1 << 3;
                case HIDE_PLACED_ON -> flags += 1 << 4;
                case HIDE_POTION_EFFECTS -> flags += 1 << 5;
            }
        }

        return flags;
    }

    /**
     * Replaces the tags in the given ItemStack by the given tags.
     * <p>This operation is only possible on a CraftItemStack. As a consequence,
     * this method <strong>returns</strong> an {@link ItemStack}. If the given
     * ItemStack was a CraftItemStack, the same instance will be returned, but
     * in the other cases, it will be a new one (a copy).</p>
     *
     * @param item The ItemStack to change.
     * @param tags The tags to place inside the stack.
     * @return An item stack with the modification applied. It may (if you given
     *         a CraftItemStack) or may not (else) be the same instance as the given one.
     * @throws NMSException if the operation cannot be executed.
     * @see #addToItemStack(ItemStack, Map, boolean) This method is equivalent
     *         to this one with replace = true.
     */
    public static ItemStack addToItemStack(ItemStack item, Map<String, Object> tags) throws NMSException {
        return addToItemStack(item, tags, true);
    }

    /**
     * Adds or replaces the tags in the given ItemStack by the given tags.
     * <p>This operation is only possible on a CraftItemStack. As a consequence,
     * this method <strong>returns</strong> an {@link ItemStack}. If the given
     * ItemStack was a CraftItemStack, the same instance will be returned, but
     * in the other cases, it will be a new one (a copy).</p>
     *
     * @param item    The ItemStack to change.
     * @param tags    The tags to place inside the stack.
     * @param replace {@code true} to replace the whole set of tags. If {@code
     *                false}, tags will be added.
     * @return An item stack with the modification applied. It may (if you given
     *         a CraftItemStack) or may not (else) be the same instance as the given one.
     * @throws NMSException if the operation cannot be executed.
     */
    public static ItemStack addToItemStack(final ItemStack item, final Map<String, Object> tags, final boolean replace)
            throws NMSException {
        init();

        try {
            final ItemStack craftItemStack = (ItemStack) ItemUtils.getCraftItemStack(item);
            final Object mcItemStack = ItemUtils.getNMSItemStack(item);
            final NBTCompound compound = fromItemStack(craftItemStack);

            if (replace) {
                compound.clear();
            }
            compound.putAll(tags);
            Object tag = compound.getNbtTagCompound();

            if (tag != null) {
                final ItemMeta craftItemMeta = (ItemMeta) Reflection
                        .call(craftItemStack.getClass(), null, "getItemMeta", new Object[] {mcItemStack});

                // There's an "applyToItem" method in CraftItemMeta but is doesn't handle well new NBT tags.
                // We try to re-create a whole new instance from the same CraftItemMeta base class instead,
                // using the constructor accepting a NBTTagCompound.
                ItemMeta newCraftItemMeta;
                try {
                    newCraftItemMeta = Reflection.instantiate(craftItemMeta.getClass(), tag);
                } catch (NoSuchMethodException e) {
                    // The CraftMetaBlockState constructor is different (like some Portal's turrets):
                    // he takes the Material as his second argument.
                    newCraftItemMeta = Reflection.instantiate(craftItemMeta.getClass(), tag, craftItemStack.getType());
                }

                craftItemStack.setItemMeta(newCraftItemMeta);
            }

            return craftItemStack;
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException
                | IllegalAccessException | NMSException e) {
            throw new NMSException("Cannot set item stack tags", e);
        }
    }

    static Class<?> getMinecraftClass(String className) throws NMSException {
        try {
            return Reflection.getMinecraftClassByName(className);
        } catch (ClassNotFoundException ex) {
            throw new NMSException("Unable to find class: " + className, ex);
        }
    }

    static Class<?> getCraftBukkitClass(String className) throws NMSException {
        try {
            return Reflection.getBukkitClassByName(className);
        } catch (ClassNotFoundException ex) {
            throw new NMSException("Unable to find class: " + className, ex);
        }
    }

    private static void init() throws NMSException {
        // PluginLogger.info("init");
        if (MC_ITEM_STACK != null) {
            // PluginLogger.info("already initialized");
            return; // Already initialized
        }
        // PluginLogger.info("ItemStack");
        MC_ITEM_STACK = getMinecraftClass("world.item.ItemStack");
        // PluginLogger.info("NBTTagCompound");
        MC_NBT_TAG_COMPOUND = getMinecraftClass("nbt.NBTTagCompound");
        // PluginLogger.info("CraftMetaItem");
        CB_CRAFT_ITEM_META = getCraftBukkitClass("inventory.CraftMetaItem");
        // PluginLogger.info("done");
    }

    /**
     * Extracts the NBT compound in the given ItemStack.
     *
     * <p>If there isn't any NBT tag in the ItemStack, one is created, linked to
     * the ItemStack, and returned, so this will always return a NBT compound tag
     * linked to the ItemStack.</p>
     *
     * @param item The item to extract NBT tags from.
     * @return The NMS NBT tag.
     * @throws NMSException If something goes wrong while extracting the tag.
     */
    private static Object getMcNBTCompound(ItemStack item) throws NMSException {
        Object mcItemStack = ItemUtils.getNMSItemStack(item);
        if (mcItemStack == null) {
            return null;
        }

        try {
            Object tag;
            try {
                // PluginLogger.info("before");
                tag = Reflection.call(mcItemStack, "getTag");
                // PluginLogger.info("got tag");
            } catch (NoSuchMethodException ex) {
                //older than 1.17
                tag = Reflection.getFieldValue(MC_ITEM_STACK, mcItemStack, "tag");
            }
            if (tag == null) {
                tag = Reflection.instantiate(MC_NBT_TAG_COMPOUND);

                try {
                    // PluginLogger.info("setting tag " + tag);
                    Reflection.call(MC_ITEM_STACK, mcItemStack, "setTag", tag);
                } catch (NoSuchMethodException e) {
                    // If the set method change—more resilient,
                    // as the setTag will only update the field without any kind of callback.
                    // PluginLogger.info("NoSuchMethodException ");
                    Reflection.setFieldValue(MC_ITEM_STACK, mcItemStack, "tag", tag);

                }
            }
            // PluginLogger.info("Fin");
            return tag;
        } catch (Exception ex) {
            throw new NMSException("Unable to retrieve NBT tag from item", ex);
        }
    }


    /**
     * Converts a native Java value to a NMS NBT instance.
     *
     * @param value The native Java value to convert.
     * @return The NMS NBT tag instance converted from the native value.
     */
    static Object fromNativeValue(Object value) {
        if (value == null) {
            return null;
        }

        NBTType type = NBTType.fromClass(value.getClass());
        return type.newTag(value);
    }

    /**
     * Converts a NMS NBT tag instance to a native value, effectively
     * unwrapping the value inside to something Java alone can understand.
     * <p>Nested values are also converted, if any, to objects implementing
     * the {@link Map} or {@link List} interface..</p>
     *
     * @param nbtTag The NMS NBT tag instance.
     * @return The corresponding native value.
     */
    static Object toNativeValue(Object nbtTag) {
        if (nbtTag == null) {
            return null;
        }
        NBTType type = NBTType.fromNmsNbtTag(nbtTag);

        return switch (type) {
            case TAG_COMPOUND -> new NBTCompound(nbtTag);
            case TAG_LIST -> new NBTList(nbtTag);
            default -> type.getData(nbtTag);
        };
    }


    /* ========== NBT String Utilities ========== */

    /**
     * Returns the NBT JSON representation of the given object.
     * This method returns a non-strict JSON representation of the object,
     * because minecraft (both client and server) can't deal with strict JSON
     * for some item nbt tags.
     *
     * @param value The value to JSONify.
     * @return the NBT JSON representation of the given object.
     */
    public static String toNBTJSONString(Object value) {
        StringBuilder sb = new StringBuilder();
        toNBTJSONString(sb, value);
        return sb.toString();
    }

    private static void toNBTJSONString(StringBuilder builder, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof List) {
            toNBTJSONString(builder, (List) value);
        } else if (value instanceof Map) {
            toNBTJSONString(builder, (Map) value);
        } else if (value instanceof String) {
            toNBTJSONString(builder, (String) value);
        } else {
            builder.append(value);
        }
    }

    private static void toNBTJSONString(StringBuilder builder, List list) {
        builder.append("[");

        boolean isFirst = true;
        for (Object value : list) {
            if (!isFirst) {
                builder.append(",");
            }
            toNBTJSONString(builder, value);
            isFirst = false;
        }

        builder.append("]");
    }

    private static void toNBTJSONString(StringBuilder builder, Map<Object, Object> map) {
        builder.append("{");

        boolean isFirst = true;
        for (Entry<Object, Object> entry : map.entrySet()) {
            if (!isFirst) {
                builder.append(",");
            }
            builder.append(entry.getKey().toString());
            builder.append(':');
            toNBTJSONString(builder, entry.getValue());
            isFirst = false;
        }

        builder.append("}");
    }

    private static void toNBTJSONString(StringBuilder builder, String value) {
        builder.append('"');
        builder.append(value.replace("\"", "\\\""));
        builder.append('"');
    }
}
