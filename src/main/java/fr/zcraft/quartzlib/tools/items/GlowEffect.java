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

import fr.zcraft.quartzlib.core.QuartzComponent;
import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.PluginLogger;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import io.papermc.paper.enchantments.EnchantmentRarity;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EntityCategory;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities creating a fake enchantment to add a glowing effect on any item.
 *
 * <p><b>NOTE:</b> this component needs to be loaded at startup, otherwise it will still works but items using
 * created using this effect may be used in a grindstone to cheat out experience to players.</p>
 *
 * @author Amaury Carrade
 */
public class GlowEffect extends QuartzComponent {
    private static final String ENCHANTMENT_NAME = "____gloweffect____";
    @Nullable
    private static Enchantment glowEnchantment = null;

    @Override
    protected void onEnable() {
        QuartzLib.registerEvents(new GlowEnchantEventListener());
        getGlow();
    }

    /**
     * Registers, if needed, and returns the fake enchantment to apply on items.
     *
     * @return an instance of the fake enchantment.
     */
    private static Enchantment getGlow() {
        if (glowEnchantment != null) {
            return glowEnchantment;
        }

        glowEnchantment = Enchantment.getByKey(getEnchantmentKey());

        if (glowEnchantment != null) {
            return glowEnchantment;
        }

        try {
            // We change this to force Bukkit to accept a new enchantment.
            // Thanks to Cybermaxke on BukkitDev.
            Field acceptingNewField =  Enchantment.class.getField("acceptingNew");
            acceptingNewField.setAccessible(true);
            acceptingNewField.set(null, true);
            glowEnchantment = new GlowEffectEnchantment();
            Enchantment.registerEnchantment(glowEnchantment);
            Enchantment.stopAcceptingRegistrations();
        } catch (Exception e) {
            PluginLogger.error("Unable to re-enable enchantments registrations", e);
        }


        return glowEnchantment;
    }

    private static NamespacedKey getEnchantmentKey() {
        return new NamespacedKey(QuartzLib.getPlugin(), ENCHANTMENT_NAME);
    }

    /**
     * Adds a glowing effect to the given item stack.
     *
     * @param item The item.
     */
    public static void addGlow(ItemStack item) {
        if (item == null) {
            return;
        }

        final Enchantment glow = getGlow();

        if (glow != null) {
            item.addEnchantment(glow, 1);
        }
    }

    /**
     * Returns if the give item has the glowing effect applied to it.
     *
     * @param item The item.
     * @return if the give item has the glowing effect applied to it.
     */
    public static boolean hasGlow(ItemStack item) {
        if (item == null) {
            return false;
        }

        Enchantment glow = getGlow();
        if (glow != null) {
            // For some reason, item.containsEnchantment() doesn't work, but meta.hasEnchant() does
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                return meta.hasEnchant(glow);
            }
            return false;
        }
        return false;
    }

    private static class GlowEnchantEventListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onInventoryClick(InventoryClickEvent event) {
            if (event.getInventory().getType() != InventoryType.GRINDSTONE) {
                return;
            }

            if (event.getClick().isShiftClick() && hasGlow(event.getCurrentItem())) {
                event.setResult(Event.Result.DENY);
                return;
            }

            Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory == null || clickedInventory.getType() != InventoryType.GRINDSTONE) {
                return;
            }

            if (hasGlow(event.getCursor())) {
                event.setResult(Event.Result.DENY);
                return;
            }

            for (ItemStack item : clickedInventory) {
                if (hasGlow(item)) {
                    event.setResult(Event.Result.DENY);
                    return;
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onInventoryDrag(InventoryDragEvent event) {
            if (event.getInventory().getType() != InventoryType.GRINDSTONE) {
                return;
            }

            for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
                Inventory dragResult = event.getView().getInventory(entry.getKey());
                if (dragResult != null && dragResult.getType() == InventoryType.GRINDSTONE) {
                    if (hasGlow(entry.getValue())) {
                        event.setResult(Event.Result.DENY);
                        return;
                    }
                }
            }
        }
    }

    private static class GlowEffectEnchantment extends Enchantment {
        protected GlowEffectEnchantment() {
            super(getEnchantmentKey());
        }

        @Override
        public boolean canEnchantItem(@NotNull ItemStack item) {
            return true;
        }

        @Override
        public @NotNull Component displayName(int i) {
            return null;
        }

        @Override
        public boolean isTradeable() {
            return false;
        }

        @Override
        public boolean isDiscoverable() {
            return false;
        }

        @Override
        public @NotNull EnchantmentRarity getRarity() {
            return null;
        }

        @Override
        public float getDamageIncrease(int i, @NotNull EntityCategory entityCategory) {
            return 0;
        }

        @Override
        public @NotNull Set<EquipmentSlot> getActiveSlots() {
            return null;
        }

        @Override
        public boolean conflictsWith(@NotNull Enchantment other) {
            return false;
        }

        @Override
        public @NotNull EnchantmentTarget getItemTarget() {
            return EnchantmentTarget.ALL;
        }

        @Override
        public boolean isTreasure() {
            return false;
        }

        @Override
        public boolean isCursed() {
            return false;
        }

        @Override
        public int getMaxLevel() {
            return 1;
        }

        @Override
        public @NotNull String getName() {
            return ENCHANTMENT_NAME;
        }

        @Override
        public int getStartLevel() {
            return 1;
        }

        @Override
        public @NotNull String translationKey() {
            return null;
        }
    }
}
