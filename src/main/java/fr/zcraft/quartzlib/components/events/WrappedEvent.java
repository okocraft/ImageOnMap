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

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/**
 * A class used to wrap an event for {@link FutureEventHandler}s.
 */
public class WrappedEvent {
    private final Event wrappedEvent;

    public WrappedEvent(Event event) {
        this.wrappedEvent = event;
    }

    /**
     * The wrapped event. You can use reflection with that to execute methods on
     * it.
     *
     * <p>If your listener is called, the class is available and you can use
     * reflection without risks.</p>
     *
     * @return The wrapped event.
     * @see fr.zcraft.quartzlib.tools.reflection.Reflection
     */
    public Event getEvent() {
        return wrappedEvent;
    }

    /**
     * Checks if the wrapped event is cancellable.
     *
     * @return {@code true} if cancellable.
     */
    public boolean isCancellable() {
        return wrappedEvent instanceof Cancellable;
    }

    /**
     * Checks if the wrapped event is cancelled.
     *
     * @return {@code true} if cancelled.
     * @throws UnsupportedOperationException if the wrapped event is not
     *                                       cancellable.
     */
    public boolean isCancelled() throws UnsupportedOperationException {
        if (wrappedEvent instanceof Cancellable) {
            return ((Cancellable) wrappedEvent).isCancelled();
        } else {
            throw new UnsupportedOperationException(
                    "Cannot retrieve the cancellation state of a non-cancellable event");
        }
    }

    /**
     * Marks the wrapped event as cancelled or not.
     *
     * @param cancelled {@code true} to cancel it.
     * @throws UnsupportedOperationException if the wrapped event is not
     *                                       cancellable.
     */
    public void setCancelled(boolean cancelled) throws UnsupportedOperationException {
        if (wrappedEvent instanceof Cancellable) {
            ((Cancellable) wrappedEvent).setCancelled(cancelled);
        } else {
            throw new UnsupportedOperationException("Cannot set the cancellation state of a non-cancellable event");
        }
    }
}
