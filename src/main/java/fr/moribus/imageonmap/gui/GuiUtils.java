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

package fr.moribus.imageonmap.gui;

import fr.zcraft.quartzlib.tools.items.InventoryUtils;
import fr.zcraft.quartzlib.tools.runners.RunTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


/**
 * Various utility methods for GUIs.
 */
public final class GuiUtils {
    /**
     * Stores the ItemStack at the given index of a GUI's inventory. The
     * inventory is only updated the next time the Bukkit Scheduler runs (i.e.
     * next server tick).
     *
     * @param gui  The GUI to update
     * @param slot The slot where to put the ItemStack
     * @param item The ItemStack to set
     */
    public static void setItemLater(InventoryGui gui, int slot, ItemStack item) {
        RunTask.nextTick(() -> {
            Inventory inventory = gui.getInventory();

            inventory.setItem(slot, item);
            for (HumanEntity player : inventory.getViewers()) {
                ((Player) player).updateInventory();
            }
        });
    }

    /**
     * One-liner to construct an {@link ItemStack}.
     *
     * @param material The stack's material.
     * @return The constructed {@link ItemStack}.
     */
    public static ItemStack makeItem(Material material) {
        return makeItem(material, null, (List<String>) null);
    }

    /**
     * One-liner to construct an {@link ItemStack}.
     *
     * @param material  The stack's material.
     * @param title     The stack's title.
     * @param loreLines The stack's lore lines.
     * @return The constructed {@link ItemStack}.
     */
    public static ItemStack makeItem(Material material, String title, String... loreLines) {
        return makeItem(material, title, Arrays.asList(loreLines));
    }

    /**
     * One-liner to construct an {@link ItemStack}.
     *
     * @param material  The stack's material.
     * @param title     The stack's title.
     * @param loreLines The stack's lore lines.
     * @return The constructed {@link ItemStack}.
     */
    public static ItemStack makeItem(Material material, String title, List<String> loreLines) {
        return makeItem(new ItemStack(material), title, loreLines);
    }

    /**
     * One-liner to update an {@link ItemStack}'s {@link ItemMeta}.
     * <p>If the stack is a map, it's attributes will be hidden.</p>
     *
     * @param itemStack The original {@link ItemStack}. This stack will be
     *                  directly modified.
     * @param title     The stack's title.
     * @param loreLines A list containing the stack's lines.
     * @return The same {@link ItemStack}, but with an updated {@link ItemMeta}.
     */
    public static ItemStack makeItem(ItemStack itemStack, String title, List<String> loreLines) {
        ItemMeta meta = Objects.requireNonNull(itemStack.getItemMeta());

        if (title != null) {
            meta.setDisplayName(title);
        }

        if (loreLines != null) {
            meta.setLore(loreLines);
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }


    /**
     * Generates a lore list based on the given text, cutting it into lines of
     * 28 characters or less.
     *
     * @param text The text.
     * @return The lore lines.
     */
    public static List<String> generateLore(String text) {
        return generateLore(text, 28);
    }

    /**
     * Generates a lore list based on the given text, cutting it into lines of
     * {@code lineLength} characters or less.
     *
     * @param text       The text.
     * @param lineLength The maximal length of a line.
     * @return The lore lines.
     */
    public static List<String> generateLore(String text, int lineLength) {
        final List<String> lore = new ArrayList<>();
        final String[] segments = text.split("\n");

        String previousSegmentFormatting = "";

        // We divide by segments of blocks without line break to keep
        // these breaks in the final result
        for (String segment : segments) {
            segment = previousSegmentFormatting + segment;

            if (ChatColor.stripColor(segment).length() <= lineLength) {
                lore.add(segment);
                continue;
            }

            final String[] words = segment.split(" ");
            String currentLine = "";

            for (final String word : words) {
                if (ChatColor.stripColor(currentLine + word).length() > lineLength
                        && ChatColor.stripColor(word).length() <= lineLength) {
                    lore.add(currentLine.trim());
                    currentLine = ChatColor.getLastColors(currentLine) + word
                            + " "; // It's important to preserve the formatting
                } else {
                    currentLine += word + " ";
                }
            }

            // Don't forget the last line...
            if (!ChatColor.stripColor(currentLine.trim()).isEmpty()) {
                lore.add(currentLine.trim());
            }

            previousSegmentFormatting = ChatColor.getLastColors(lore.get(lore.size() - 1));
        }

        return lore;
    }

    /**
     * Generates a text with a fixed length of 50 characters and a prefix.
     *
     * @param text   The original text.
     * @param prefix The prefix to add to each line.
     * @return A prefixed and fixed-length text.
     */
    public static String generatePrefixedFixedLengthString(String prefix, String text) {
        return generatePrefixedFixedLengthString(prefix, text, 55);
    }

    /**
     * Generates a text with a fixed length and a prefix.
     *
     * @param text       The original text.
     * @param prefix     The prefix to add to each line.
     * @param lineLength The maximal length of each line.
     * @return A prefixed and fixed-length text.
     */
    public static String generatePrefixedFixedLengthString(String prefix, String text, int lineLength) {
        final List<String> lines = generateLore(text, lineLength);
        final StringBuilder result = new StringBuilder();

        for (final String line : lines) {
            result.append(prefix).append(line).append('\n');
        }

        return result.toString().trim();
    }


    /**
     * Checks if these inventories are equal.
     *
     * @param inventory1 The first inventory.
     * @param inventory2 The other inventory.
     * @return {@code true} if the two inventories are the same one.
     * @deprecated Use InventoryUtils.sameInventories() instead.
     */
    @Deprecated
    public static boolean sameInventories(Inventory inventory1, Inventory inventory2) {
        return InventoryUtils.sameInventories(inventory1, inventory2);
    }


}
