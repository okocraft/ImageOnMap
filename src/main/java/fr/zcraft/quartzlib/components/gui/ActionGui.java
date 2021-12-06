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

package fr.zcraft.quartzlib.components.gui;

import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.items.ItemStackBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * This class implements an action-based GUI.
 * Actions are buttons which trigger an event when getting clicked on by the user.
 * They are represented by (customizable) items, which are immutable by the user.
 * <p>Events handlers are (usually private) methods implemented in the derived
 * class(es). They are named using the pattern 'action_[action name]', and
 * are called when the associated action is triggered. They take an optional
 * argument (add it if you need it): the {@link InventoryClickEvent} triggered.</p>
 *
 * @author ProkopyL (main) and Amaury Carrade
 */
public abstract class ActionGui extends InventoryGui {
    /**
     * The prefix for action handlers.
     */
    private static final String ACTION_HANDLER_NAME = "action_";

    /**
     * The class of this GUI.
     * Useful to retrieve methods from the derived classes.
     */
    private final Class<? extends ActionGui> guiClass = this.getClass();

    /**
     * A map containing all the actions defined by the derived class, indexed by
     * their position in the inventory.
     */
    private final HashMap<Integer, Action> actions = new HashMap<>();


    /* ===== Protected API ===== */

    /**
     * Creates a new action, represented by the given item.
     * The item's metadata is changed to use the given title and lore.
     *
     * @param name      The identifier of the action.
     * @param slot      The slot the action will be placed on.
     * @param material  The material used to represent the action.
     * @param title     The title the item will show.
     * @param loreLines The lore the item will show.
     */
    protected void action(String name, int slot, Material material, String title, String... loreLines) {
        action(name, slot, new ItemStack(material), title, Arrays.asList(loreLines));
    }

    /**
     * Creates a new action, represented by the given item.
     * The item's metadata is changed to use the given title and lore.
     *
     * @param name      The identifier of the action.
     * @param slot      The slot the action will be placed on.
     * @param item      The item used to represent the action.
     * @param title     The title the item will show.
     * @param loreLines The lore the item will show.
     */
    protected void action(String name, int slot, ItemStack item, String title, String... loreLines) {
        action(name, slot, item, title, Arrays.asList(loreLines));
    }

    /**
     * Creates a new action, represented by the given item.
     * The item's metadata is changed to use the given title and lore.
     *
     * @param name      The identifier of the action.
     * @param slot      The slot the action will be placed on.
     * @param item      The item used to represent the action.
     * @param title     The title the item will show.
     * @param loreLines The lore the item will show.
     */
    protected void action(String name, int slot, ItemStack item, String title, List<String> loreLines) {
        action(name, slot, GuiUtils.makeItem(item, title, loreLines));
    }

    /**
     * Creates a new action, represented by the given material.
     *
     * @param name     The identifier of the action.
     * @param slot     The slot the action will be placed on.
     * @param material The material used to represent the action.
     */
    protected void action(String name, int slot, Material material) {
        action(name, slot, GuiUtils.makeItem(material));
    }

    /**
     * Creates a new action, and adds it to the GUI.
     *
     * @param name The identifier of the action.
     * @param slot The slot the action will be placed on.
     * @param item The item used to represent the action.
     */
    protected void action(String name, int slot, ItemStackBuilder item) {
        action(name, slot, item.item());
    }

    /**
     * Creates a new action, represented by no item.
     *
     * @param name The identifier of the action.
     * @param slot The slot the action will be placed on.
     * @return an {@link ItemStackBuilder}
     *     ItemStackBuilder to build the representing item
     */
    protected ItemStackBuilder action(String name, int slot) {
        return action(name, slot, (ItemStack) null);
    }

    /**
     * Creates a new action, and adds it to the GUI.
     *
     * @param name The identifier of the action.
     * @param slot The slot the action will be placed on.
     * @param item The item used to represent the action.
     * @return an {@link ItemStackBuilder} ItemStackBuilder to build the representing
     *     item, or null if it was already specified.
     */
    protected ItemStackBuilder action(String name, int slot, ItemStack item) {
        if (slot > getSize() || slot < 0) {
            throw new IllegalArgumentException("Illegal slot ID");
        }

        Action action = new Action(name, slot, item, getActionHandler(guiClass, name));

        action(action);

        if (item == null) {
            return action.updateItem();
        }
        return null;
    }

    /**
     * Adds an action to the GUI.
     *
     * @param action The {@link Action} to register.
     */
    private void action(Action action) {
        actions.put(action.slot, action);
    }

    /**
     * Updates the action represented by the given name.
     *
     * @param name The name of the action to update.
     * @param item The new item to affect to the action.
     * @throws IllegalArgumentException If no action has the given name.
     */
    protected void updateAction(String name, ItemStack item) {
        getAction(name).item = item;
    }

    /**
     * Retrieves the action represented by the given name.
     *
     * @param name The name of the action to retreive.
     * @return The action represented by the given name.
     * @throws IllegalArgumentException If no action has the given name.
     */
    private Action getAction(String name) throws IllegalArgumentException {
        for (Action action : actions.values()) {
            if (action.name.equals(name)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown action name : " + name);
    }

    /**
     * Raised when the Gui needs to be updated.
     * Use this method to create your actions.
     */
    @Override
    protected abstract void onUpdate();

    /**
     * Raised when an action without any event handler has been triggered.
     *
     * @param name The name of the triggered action.
     * @param slot The slot of the action.
     * @param item The item of the action.
     */
    protected void unknown_action(String name, int slot, ItemStack item) {
    }


    @Override
    public void update() {
        actions.clear();
        super.update();
    }

    @Override
    protected void populate(Inventory inventory) {
        for (Action action : actions.values()) {
            inventory.setItem(action.slot, action.getItem());
        }
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (event.getRawSlot() >= event.getInventory().getSize()) { // The user clicked in their own inventory
            if (!event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                return;
            }
        }

        event.setCancelled(true);

        callAction(actions.get(event.getRawSlot()), event);
    }

    /**
     * Triggers the given action's event handler.
     *
     * @param action The action to trigger.
     */
    private void callAction(Action action, InventoryClickEvent event) {
        if (action == null) {
            return;
        }

        if (action.callback == null) {
            unknown_action(action.name, action.slot, action.item);
            return;
        }

        try {
            if (action.callback.getParameterTypes().length == 1) {
                action.callback.invoke(this, event);
            } else {
                action.callback.invoke(this);
            }
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            PluginLogger.error("Could not invoke GUI action handler", ex);
        } catch (InvocationTargetException ex) {
            PluginLogger.error("Error while invoking action handler {0} of GUI {1}",
                    ex.getCause(), action.name, guiClass.getName());
        }
    }

    /**
     * Returns if the given method is a valid action handler.
     * An action handler is valid only if it is accessible and parameter types matches.
     *
     * @param method The method to test
     * @return true if the given method is valid, false otherwise.
     */
    private boolean isActionHandlerValid(Method method) {
        int parameterCount = method.getParameterTypes().length;

        if (parameterCount >= 2) {
            return false;
        }
        if (parameterCount == 1) {
            if (method.getParameterTypes()[0] != InventoryClickEvent.class) {
                return false;
            }
        }

        try {
            method.setAccessible(true);
        } catch (SecurityException ex) {
            return false;
        }
        return true;
    }

    /**
     * Retrieves the event handler matching the given name from a class (or any of its parents).
     *
     * @param klass The class to retrieve the event handler from.
     * @param name  The name of the action.
     * @return The event handler matching the action name, or null if none was found.
     */
    private Method getActionHandler(Class<?> klass, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        do {
            GuiAction actionAnnotation;
            String methodName;

            for (Method method : klass.getDeclaredMethods()) {
                actionAnnotation = method.getAnnotation(GuiAction.class);
                if (actionAnnotation == null) {
                    continue;
                }
                if (!(actionAnnotation.value() == null || actionAnnotation.value().isEmpty())) {
                    if (actionAnnotation.value().equals(name)) {
                        if (isActionHandlerValid(method)) {
                            return method;
                        }
                    }
                } else {
                    methodName = method.getName();
                    if (methodName.equals(name)) {
                        if (isActionHandlerValid(method)) {
                            return method;
                        }
                    }

                    if (methodName.startsWith(ACTION_HANDLER_NAME)) {
                        if (methodName.substring(ACTION_HANDLER_NAME.length()).equals(name)) {
                            if (isActionHandlerValid(method)) {
                                return method;
                            }
                        }
                    }
                }
            }
            klass = klass.getSuperclass();
        } while (klass != null);

        return null;
    }

    /**
     * Returns if this GUI has any actions defined.
     */
    protected boolean hasActions() {
        return !actions.isEmpty();
    }

    /**
     * This structure represents an action.
     */
    private static class Action {
        /**
         * The name of the action.
         */
        public String name;
        /**
         * The slot the action will be put in.
         */
        public int slot;
        /**
         * The item this action will be represented by.
         */
        public ItemStack item;

        public ItemStackBuilder builder;

        /**
         * The callback this action will call when triggered.
         */
        public Method callback;

        public Action(String name, int slot, ItemStack item, Method callback) {
            this.name = name;
            this.slot = slot;
            this.item = item;
            this.callback = callback;
            this.builder = null;
        }

        public ItemStack getItem() {
            if (item == null) {
                item = builder.item();
            }

            return item;
        }

        public ItemStackBuilder updateItem() {
            if (builder == null) {
                builder = new ItemStackBuilder(item);
            }

            item = null;
            return builder;
        }
    }
}
