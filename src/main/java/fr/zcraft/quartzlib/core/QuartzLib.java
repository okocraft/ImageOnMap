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

package fr.zcraft.quartzlib.core;

import com.google.common.collect.ImmutableSet;
import fr.zcraft.quartzlib.components.events.FutureEvents;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class QuartzLib {
    private static final ArrayList<Class<? extends QuartzComponent>> componentsToLoad = new ArrayList<>();
    private static JavaPlugin plugin;
    private static Set<QuartzComponent> loadedComponents;

    private static ZLibListener listener;

    /**
     * Initializes QuartzLib.
     * <p>This needs to be called before anything else; otherwise, you will encounter
     * {@link IllegalStateException}s.</p>
     * <p>This method also initializes the {@link PluginLogger}, used by QuartzLib.</p>
     *
     * @param plugin The plugin currently using this instance of the QuartzLib.
     */
    public static void init(JavaPlugin plugin) {
        QuartzLib.plugin = plugin;
        QuartzLib.loadedComponents = new CopyOnWriteArraySet<>();

        PluginLogger.init();

        for (Class<? extends QuartzComponent> component : componentsToLoad) {
            loadComponent(component);
        }
    }

    /**
     * Loads a QuartzLib component, and store it as loaded to automatically unload it when needed.
     *
     * @param component The component to load.
     * @throws IllegalStateException if QuartzLib was not initialized.
     */
    static <T extends QuartzComponent> T loadComponent(T component) throws IllegalStateException {
        checkInitialized();

        //Make sure any loaded component will be correctly unloaded.
        if (listener == null) {
            QuartzLib.listener = registerEvents(new ZLibListener());
        }

        if (loadedComponents.add(component)) {
            if (component instanceof Listener) {
                registerEvents((Listener) component);
            }
            component.setEnabled(true);
        }

        return component;
    }

    /**
     * Tries to load a given component.
     *
     * @param <T>            The type of the component.
     * @param componentClass The component's class.
     * @return The component instance, or null if instanciation failed.
     */
    public static <T extends QuartzComponent> T loadComponent(Class<T> componentClass) {
        if (!isInitialized()) {
            componentsToLoad.add(componentClass);
            return null;
        }

        try {
            return loadComponent(componentClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            PluginLogger.error("Cannot instantiate QuartzLib component {0}", e, componentClass.getName());
            return null;
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }

    /**
     * Unloads all the registered components and the core tools used.
     * <p>This method is automatically called when the plugin is unloaded.</p>
     *
     * @throws IllegalStateException if QuartzLib was not initialized.
     */
    static void exit() {
        checkInitialized();

        for (QuartzComponent component : loadedComponents) {
            component.setEnabled(false);
        }

        loadedComponents.clear();

        loadedComponents = null;
        listener = null;
    }

    /**
     * Returns the plugin currently using the library.
     *
     * @return The plugin currently using the library.
     * @throws IllegalStateException if QuartzLib was not initialized.
     */
    public static JavaPlugin getPlugin() throws IllegalStateException {
        checkInitialized();
        return plugin;
    }

    /**
     * Check wherever QuartzLib is correctly initialized.
     *
     * @return {@code true} if initialized.
     */
    public static boolean isInitialized() {
        return plugin != null;
    }

    /**
     * Registers an event listener for the plugin currently using the library, and returns it.
     * This method also registers {@link fr.zcraft.quartzlib.components.events.FutureEventHandler future events}.
     *
     * @param <T>      The type of the listener.
     * @param listener The listener to register.
     * @return The registered listener.
     */
    public static <T extends Listener> T registerEvents(T listener) {
        Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
        FutureEvents.registerFutureEvents(listener);
        return listener;
    }

    /**
     * Unregisters the given event listener from all events it is subscribed to.
     *
     * @param listener The listener to unregister.
     */
    public static void unregisterEvents(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    /**
     * Check wherever QuartzLib is correctly initialized.
     *
     * @throws IllegalStateException if QuartzLib is not initialized.
     */
    private static void checkInitialized() throws IllegalStateException {
        if (plugin == null) {
            throw new IllegalStateException(
                    "Assertion failed: QuartzLib is not correctly inizialized. "
                            + "Make sure QuartzLib.init() or QuartzLib.onLoad() is correctly called.");
        }
    }

    private static class ZLibListener implements Listener {
        @EventHandler
        public void onPluginDisable(PluginDisableEvent event) {
            if (plugin == event.getPlugin()) {
                exit();
            }
        }
    }
}
