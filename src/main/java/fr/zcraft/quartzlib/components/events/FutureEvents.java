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

package fr.zcraft.quartzlib.components.events;

import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.TimedRegisteredListener;


public final class FutureEvents {
    /**
     * Registers the future events in a listener.
     *
     * <p>This method will scan the methods of the given listener.
     * To be registered as future events, the methods must:</p>
     * <ul>
     *     <li>be annotated with {@link FutureEventHandler @FutureEventHandler};</li>
     *     <li>accept only one argument of type {@link WrappedEvent}.</li>
     * </ul>
     *
     * <p>These listeners are unregistered the same way as Bukkit ones,
     * with {@link HandlerList#unregisterAll(Listener)}.</p>
     *
     * @param listener The listener to register.
     */
    public static void registerFutureEvents(final Listener listener) {
        if (!QuartzLib.isInitialized() || !QuartzLib.getPlugin().isEnabled()) {
            throw new IllegalPluginAccessException("Plugin attempted to register the future listener " + listener
                    + " while not enabled or QuartzLib not initialized!");
        }

        final Map<Class<? extends Event>, Set<RegisteredListener>> registeredListeners = new HashMap<>();

        for (final Method method : listener.getClass().getDeclaredMethods()) {
            final FutureEventHandler annotation = method.getAnnotation(FutureEventHandler.class);
            if (annotation == null
                    || method.getParameterTypes().length != 1
                    || !WrappedEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                continue;
            }

            Class<?> eventClass;
            try {
                eventClass = Class.forName(annotation.event());
            } catch (ClassNotFoundException e) {
                try {
                    eventClass = Class.forName("org.bukkit.event." + annotation.event());
                } catch (ClassNotFoundException e1) {
                    // The event class cannot be found: this event is not compatible with this version. Aborting...
                    continue;
                }
            }

            if (!Event.class.isAssignableFrom(eventClass)) {
                PluginLogger.error("Cannot register a future event handler with a non-event class ({0})",
                        eventClass.getName());
                continue;
            }

            final Class<?> finalEventClass = eventClass;

            method.setAccessible(true);

            final EventExecutor executor = new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) throws EventException {
                    try {
                        if (finalEventClass.isAssignableFrom(event.getClass())) {
                            final WrappedEvent wrap = new WrappedEvent(event);
                            method.invoke(listener, wrap);
                        }
                    } catch (InvocationTargetException e) {
                        throw new EventException(e.getCause());
                    } catch (Throwable t) {
                        throw new EventException(t);
                    }
                }
            };

            final RegisteredListener registeredListener;
            if (Bukkit.getServer().getPluginManager().useTimings()) {
                registeredListener =
                        new TimedRegisteredListener(listener, executor, annotation.priority(), QuartzLib.getPlugin(),
                                annotation.ignoreCancelled());
            } else {
                registeredListener =
                        new RegisteredListener(listener, executor, annotation.priority(), QuartzLib.getPlugin(),
                                annotation.ignoreCancelled());
            }

            if (!registeredListeners.containsKey(eventClass)) {
                registeredListeners.put((Class<? extends Event>) eventClass, new HashSet<RegisteredListener>());
            }

            registeredListeners.get(eventClass).add(registeredListener);
        }


        final PluginManager pm = Bukkit.getServer().getPluginManager();

        // Methods cannot be retrieved using the Reflection shortcut because NoSuchMethodExceptions are thrown
        try {
            Method getRegistrationClass =
                    SimplePluginManager.class.getDeclaredMethod("getRegistrationClass", Class.class);
            getRegistrationClass.setAccessible(true);

            Method getEventListeners = SimplePluginManager.class.getDeclaredMethod("getEventListeners", Class.class);
            getEventListeners.setAccessible(true);

            for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : registeredListeners.entrySet()) {
                try {
                    HandlerList handlerList =
                            (HandlerList) getEventListeners.invoke(pm, getRegistrationClass.invoke(pm, entry.getKey()));
                    handlerList.registerAll(entry.getValue());
                } catch (IllegalAccessException e) {
                    PluginLogger.error("Cannot register future event handler, is your Bukkit version supported?", e);
                } catch (InvocationTargetException e) {
                    PluginLogger
                            .error("Error while registering future event handler, is your Bukkit version supported?",
                                    e.getCause());
                }
            }
        } catch (NoSuchMethodException e) {
            PluginLogger.error("Cannot load methods needed to register future event handlers,"
                            + "is your Bukkit version supported?", e);
        }
    }
}
