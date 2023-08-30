/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.beans;

import java.util.Objects;

import javafx.util.Subscription;

/**
 * An {@code Observable} is an entity that wraps content and allows to
 * observe the content for invalidations.
 * <p>
 * An implementation of {@code Observable} may support lazy evaluation,
 * which means that the content is not immediately recomputed after changes, but
 * lazily the next time it is requested. All bindings and properties in
 * this library support lazy evaluation.
 * <p>
 * Implementations of this class should strive to generate as few events as
 * possible to avoid wasting too much time in event handlers. Implementations in
 * this library mark themselves as invalid when the first invalidation event
 * occurs. They do not generate anymore invalidation events until their value is
 * recomputed and valid again.
 *
 * @see javafx.beans.value.ObservableValue
 * @see javafx.collections.ObservableList
 * @see javafx.collections.ObservableMap
 *
 *
 * @since JavaFX 2.0
 */
public interface Observable {

    /**
     * Adds an {@link InvalidationListener} which will be notified whenever the
     * {@code Observable} becomes invalid. If the same
     * listener is added more than once, then it will be notified more than
     * once. That is, no check is made to ensure uniqueness.
     * <p>
     * Note that the same actual {@code InvalidationListener} instance may be
     * safely registered for different {@code Observables}.
     * <p>
     * The {@code Observable} stores a strong reference to the listener
     * which will prevent the listener from being garbage collected and may
     * result in a memory leak. It is recommended to either unregister a
     * listener by calling {@link #removeListener(InvalidationListener)
     * removeListener} after use or to use an instance of
     * {@link WeakInvalidationListener} avoid this situation.
     *
     * @see #removeListener(InvalidationListener)
     *
     * @param listener
     *            The listener to register
     * @throws NullPointerException
     *             if the listener is null
     */
    void addListener(InvalidationListener listener);

    /**
     * Removes the given listener from the list of listeners, that are notified
     * whenever the value of the {@code Observable} becomes invalid.
     * <p>
     * If the given listener has not been previously registered (i.e. it was
     * never added) then this method call is a no-op. If it had been previously
     * added then it will be removed. If it had been added more than once, then
     * only the first occurrence will be removed.
     *
     * @see #addListener(InvalidationListener)
     *
     * @param listener
     *            The listener to remove
     * @throws NullPointerException
     *             if the listener is null
     */
    void removeListener(InvalidationListener listener);

    /**
     * Creates a {@code Subscription} on this {@code Observable} which calls
     * {@code invalidationSubscriber} whenever it becomes invalid. The provided
     * subscriber is akin to an {@code InvalidationListener} without the
     * {@code Observable} parameter. If the same subscriber is subscribed more
     * than once, then it will be notified more than once. That is, no check is
     * made to ensure uniqueness.
     * <p>
     * Note that the same subscriber instance may be safely subscribed for
     * different {@code Observables}.
     * <p>
     * Also note that when subscribing on an {@code Observable} with a longer
     * lifecycle than the subscriber, the subscriber must be unsubscribed
     * when no longer needed as the subscription will otherwise keep the subscriber
     * from being garbage collected.
     *
     * @param invalidationSubscriber a {@code Runnable} to call whenever this
     *     value becomes invalid, cannot be {@code null}
     * @return a {@code Subscription} which can be used to cancel this
     *     subscription, never {@code null}
     * @throws NullPointerException if the subscriber is {@code null}
     * @see #addListener(InvalidationListener)
     * @since 21
     */
    default Subscription subscribe(Runnable invalidationSubscriber) {
        Objects.requireNonNull(invalidationSubscriber, "invalidationSubscriber cannot be null");
        InvalidationListener listener = obs -> invalidationSubscriber.run();

        addListener(listener);

        return () -> removeListener(listener);
    }
}
