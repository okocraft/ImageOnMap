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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;


/**
 * Represents an event handler for a potentially non-existent event, like an
 * event added in 1.9 listened in a plugin for both 1.8 and 1.9.
 *
 * <p>The listener will be called only if the event is available in the
 * server.</p>
 *
 * <p>This can also be used for other plugins events.</p>
 *
 * <p>This annotation goes on methods in {@link Listener
 * Listeners}; these methods MUST accept one argument of the type {@link
 * WrappedEvent}. Listeners with this kind of methods must also be registered
 * using {@link FutureEvents#registerFutureEvents(Listener)}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FutureEventHandler {
    /**
     * The class name of the event to listen to.
     *
     * <p>The class will be loaded from the raw name at first; then if it fails,
     * the class {@code org.bukkit.event.[given name]} will be tried.</p>
     */
    String event();

    /**
     * The event's priority.
     *
     * @see EventHandler#priority()
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * {@code true} if this listener should not be called if the event was
     * cancelled before.
     *
     * @see EventHandler#ignoreCancelled()
     */
    boolean ignoreCancelled() default false;
}
