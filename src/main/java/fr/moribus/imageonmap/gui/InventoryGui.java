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
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import fr.zcraft.quartzlib.tools.runners.RunTask;
import java.lang.reflect.InvocationTargetException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

/**
 * This class provides the basic needs for chest-type GUIs.
 * It allows you to create custom GUIs by simply providing an inventory
 * to fill, as well as rerouting basic events to it.
 */
public abstract class InventoryGui extends GuiBase {
    protected static final int INVENTORY_ROW_SIZE = 9;
    protected static final int MAX_INVENTORY_COLUMN_SIZE = 6;
    protected static final int MAX_INVENTORY_SIZE = INVENTORY_ROW_SIZE * MAX_INVENTORY_COLUMN_SIZE;
    protected static final int MAX_TITLE_LENGTH = 32;

    /**
     * The size of the inventory.
     */
    private int size = 0;

    /**
     * The title of the inventory.
     */
    private String title = "Inventory";

    /**
     * The current Bukkit inventory.
     */
    private Inventory inventory;

    /* ===== Public API ===== */

    /**
     * Returns if the given event affects the GUI's inventory.
     *
     * @param event The event to test
     * @return true if any of the event's slots is in the GUI's inventory, false otherwise.
     */
    protected static boolean affectsGui(final InventoryDragEvent event) {
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns if the given event affects the GUI's inventory.
     *
     * @param event The event to test
     * @return {@code true} if the event's slot is in the GUI's inventory,
     *     {@code false} otherwise.
     */
    protected static boolean affectsGui(final InventoryClickEvent event) {
        return event.getRawSlot() < event.getInventory().getSize();
    }

    /* ===== Protected API ===== */

    /**
     * Asks the GUI to recreate its view.
     * The inventory is cleared, but never regenerated when calling this method.
     */
    public void refresh() {
        inventory.clear();
        populate(inventory);
    }

    @Override
    protected void open(final Player player) {
        super.open(player);
        player.openInventory(inventory);
    }

    /**
     * Closes this inventory.
     */
    @Override
    public void close() {
        if (!isOpen()) {
            return;
        }

        // If close() is called manually, not from InventoryCloseEvent
        // Ran on the next tick because it's unsafe to call Player.closeInventory() from an
        // InventoryEvent.
        RunTask.nextTick(() -> {
            final InventoryView openInventoryView = getPlayer().getOpenInventory();
            if (openInventoryView != null
                    && InventoryUtils.sameInventories(inventory, openInventoryView.getTopInventory())) {
                getPlayer().closeInventory();
            }
        });

        super.close();
    }

    /**
     * Called when the inventory needs to be (re)populated.
     *
     * @param inventory The inventory to populate
     */
    protected abstract void populate(final Inventory inventory);

    /**
     * Raised when an action is performed on an item in the inventory.
     *
     * @param event The click event data.
     */
    protected abstract void onClick(final InventoryClickEvent event);

    /**
     * Raised when an drag is performed on the inventory.
     * The default behaviour is to cancel any event that affects the GUI.
     *
     * @param event The drag event data.
     */
    protected void onDrag(final InventoryDragEvent event) {
        if (affectsGui(event)) {
            event.setCancelled(true);
        }
    }

    /**
     * Asks the GUI to update its data, and refresh its view accordingly.
     * The inventory may be regenerated when calling this method.
     */
    @Override
    public void update() {
        super.update();
        Player player = getPlayer();

        boolean titleIsStillTheSame = false;
        try {
            titleIsStillTheSame = player.getOpenInventory().getTitle().equals(title);
        } catch (final NoSuchMethodError e) {
            try {
                titleIsStillTheSame = Reflection.call(inventory, "getTitle").equals(title);
            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        // If inventory does not need to be regenerated
        if (inventory != null && titleIsStillTheSame && inventory.getSize() == size) {
            refresh();
        } else {
            inventory = Bukkit.createInventory(player, size, title);
            populate(inventory);

            // Reopening the inventory
            // FIXME This probably resets the player' mouse cursor position to the center of the GUI in 1.13.
            if (isOpen()) {
                RunTask.nextTick(() -> {
                    player.closeInventory();
                    RunTask.nextTick(() -> {
                        player.openInventory(inventory);
                    });
                });
            }
        }
    }

    /* ===== Getters & Setters ===== */

    /**
     * Gets the size of the inventory.
     *
     * @return The size of the inventory.
     */
    protected final int getSize() {
        return size;
    }

    /**
     * Sets the new size of the inventory.
     * The given value is raised to be a multiple of the size of an inventory's
     * row, and is capped to the maximal size of an inventory.
     * It will be applied on the next GUI update.
     *
     * @param size The new size of the inventory.
     */
    protected final void setSize(final int size) {
        this.size = Math.min(((int) (Math.ceil((double) size / INVENTORY_ROW_SIZE))) * INVENTORY_ROW_SIZE,
                MAX_INVENTORY_SIZE);
    }

    /**
     * Sets the new title of the inventory.
     * It will be applied on the next GUI update.
     *
     * @param title The new title of the inventory
     */
    protected void setTitle(String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH - 4) + "...";
        }
        this.title = title;
    }

    /**
     * Gets the underlying inventory, or null if the Gui has not been opened yet.
     *
     * @return The underlying inventory, or null if the Gui has not been opened yet.
     */
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    protected Listener getEventListener() {
        return new InventoryGuiListener();
    }

    /**
     * Implements a Bukkit listener for all GUI-related events.
     */
    protected class InventoryGuiListener implements Listener {
        @EventHandler
        public void onInventoryDrag(final InventoryDragEvent event) {
            if (event.getWhoClicked() != getPlayer()) {
                return;
            }
            onDrag(event);
        }

        @EventHandler
        public void onInventoryClick(final InventoryClickEvent event) {
            if (event.getWhoClicked() != getPlayer()) {
                return;
            }

            onClick(event);
        }

        @EventHandler
        public void onInventoryClose(final InventoryCloseEvent event) {
            if (event.getPlayer() != getPlayer()) {
                return;
            }

            if (!event.getInventory().equals(inventory)) {
                return;
            }

            if (isOpen()) {
                if (checkImmune()) {
                    return;
                }
                close();
            }
        }
    }
}
