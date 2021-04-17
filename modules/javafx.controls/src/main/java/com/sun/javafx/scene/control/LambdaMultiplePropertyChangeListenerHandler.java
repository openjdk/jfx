/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;

/**
 * Handler to manage multiple listeners to multiple observables. It handles
 * adding/removing listeners for common notification events (change,
 * invalidation and list change) on particular instances of observables. The
 * listeners are wrapped into their weak counterparts to minimize the potential of
 * memory leaks.
 * <p>
 * Clients can register consumers to be invoked on receiving notification.
 * Un-/Registration api is separate per notification type and per observable.
 * It is allowed to register multiple consumers per observable. They
 * are executed in the order of registration. Note that unregistration
 * of a given observable stops observing that observable (for the notification
 * type of the unregistration) completely, that is none of the consumers
 * previously registered with this handler will be executed after unregistering.
 * <p>
 * Disposing removes all listeners added by this handler from all registered observables.
 */
public final class LambdaMultiplePropertyChangeListenerHandler {
// FIXME JDK-8265401: name doesn't fit after widening to support more notification event types

    @SuppressWarnings("rawtypes")
    private static final Consumer EMPTY_CONSUMER = e -> {};

    // support change listeners
    private final Map<ObservableValue<?>, Consumer<ObservableValue<?>>> propertyReferenceMap;
    private final ChangeListener<Object> propertyChangedListener;
    private final WeakChangeListener<Object> weakPropertyChangedListener;

    // support invalidation listeners
    private final Map<Observable, Consumer<Observable>> observableReferenceMap;
    private final InvalidationListener invalidationListener;
    private final WeakInvalidationListener weakInvalidationListener;

    // support list change listeners
    private final Map<ObservableList<?>, Consumer<Change<?>>> observableListReferenceMap;
    private final ListChangeListener<Object> listChangeListener;
    private final WeakListChangeListener<Object> weakListChangeListener;

    public LambdaMultiplePropertyChangeListenerHandler() {
        // change listening support
        this.propertyReferenceMap = new HashMap<>();
        this.propertyChangedListener = (observable, oldValue, newValue) -> {
            // because all consumers are chained, this calls each consumer for the given property
            // in turn.
            propertyReferenceMap.getOrDefault(observable, EMPTY_CONSUMER).accept(observable);
        };
        this.weakPropertyChangedListener = new WeakChangeListener<>(propertyChangedListener);

        // invalidation listening support
        this.observableReferenceMap = new HashMap<>();
        this.invalidationListener =  obs -> {
            observableReferenceMap.getOrDefault(obs, EMPTY_CONSUMER).accept(obs);
        };
        this.weakInvalidationListener = new WeakInvalidationListener(invalidationListener);

        // list change listening support
        this.observableListReferenceMap = new IdentityHashMap<>();
        this.listChangeListener = change -> {
            observableListReferenceMap.getOrDefault(change.getList(), EMPTY_CONSUMER).accept(change);
        };
        this.weakListChangeListener = new WeakListChangeListener<>(listChangeListener);
    }

    /**
     * Registers a consumer to be invoked on change notification from the given property. Does nothing
     * if property or consumer is null. Consumers registered to the same property will be executed
     * in the order they have been registered.
     *
     * @param property the property to observe for change notification
     * @param consumer the consumer to be invoked on change notification from the property
     */
    public final void registerChangeListener(ObservableValue<?> property, Consumer<ObservableValue<?>> consumer) {
        if (property == null || consumer == null) return;

        // we only add a listener if the propertyReferenceMap does not contain the property
        // (that is, we've added a consumer to this specific property for the first
        // time).
        if (!propertyReferenceMap.containsKey(property)) {
            property.addListener(weakPropertyChangedListener);
        }

        propertyReferenceMap.merge(property, consumer, Consumer::andThen);
    }

    /**
     * Stops observing the given property for change notification. Returns
     * a single chained consumer consisting of all consumers registered with
     * {@link #registerChangeListener(ObservableValue, Consumer)} in the order they
     * have been registered.
     *
     * @param property the property to stop observing for change notification
     * @return a single chained consumer consisting of all consumers registered for the given property
     *    or null if none has been registered or the property is null
     */
    public final Consumer<ObservableValue<?>> unregisterChangeListeners(ObservableValue<?> property) {
        if (property == null) return null;
        property.removeListener(weakPropertyChangedListener);
        return propertyReferenceMap.remove(property);
    }

    /**
     * Registers a consumer to be invoked on invalidation notification from the given observable.
     * Does nothing if observable or consumer is null. Consumers registered to the same observable will be executed
     * in the order they have been registered.
     *
     * @param observable the observable to observe for invalidation notification
     * @param consumer the consumer to be invoked on invalidation notification from the observable
     *
     */
    public final void registerInvalidationListener(Observable observable, Consumer<Observable> consumer) {
        if (observable == null || consumer == null) return;
        if (!observableReferenceMap.containsKey(observable)) {
            observable.addListener(weakInvalidationListener);
        }
        observableReferenceMap.merge(observable, consumer, Consumer::andThen);
    }

    /**
     * Stops observing the given observable for invalidation notification.
     * Returns a single chained consumer consisting of all consumers registered with
     * {@link #registerInvalidationListener(Observable, Consumer)} in the
     * order they have been registered.
     *
     * @param observable the observable to stop observing for invalidation notification
     * @return a single chained consumer consisting of all consumers registered for given observable
     *    or null if none has been registered or the observable is null
     *
     */
    public final Consumer<Observable> unregisterInvalidationListeners(Observable observable) {
        if (observable == null) return null;
        observable.removeListener(weakInvalidationListener);
        return observableReferenceMap.remove(observable);
    }

    /**
     * Registers a consumer to be invoked on list change notification from the given observable list.
     * Does nothing if list or consumer is null. Consumers registered to the same observable list
     * will be executed in the order they have been registered.
     *
     * @param list the observable list observe for list change notification
     * @param consumer the consumer to be invoked on list change notification from the list
     *
     */
    public final void registerListChangeListener(ObservableList<?> list, Consumer<Change<?>> consumer) {
        if (list == null || consumer == null) return;
        if (!observableListReferenceMap.containsKey(list)) {
            list.addListener(weakListChangeListener);
        }
        observableListReferenceMap.merge(list, consumer, Consumer::andThen);
    }

    /**
     * Stops observing the given observable list for list change notification.
     * Returns a single chained consumer consisting of all consumers registered with
     * {@link #registerListChangeListener(ObservableList, Consumer)} in the order they have been registered.
     *
     * @param list the observable list to stop observing for list change notification
     * @return a single chained consumer consisting of all consumers added for the given list
     *    or null if none has been registered or the list is null
     */
    public final Consumer<Change<?>> unregisterListChangeListeners(ObservableList<?> list) {
        if (list == null) return null;
        list.removeListener(weakListChangeListener);
        return observableListReferenceMap.remove(list);
    }


    /**
     * Stops observing all types of notification from all registered observables.
     * <p>
     * Note: this handler is still usable after calling this method.
     */
    public void dispose() {
        // unhook change listeners
        for (ObservableValue<?> value : propertyReferenceMap.keySet()) {
            value.removeListener(weakPropertyChangedListener);
        }
        propertyReferenceMap.clear();
        // unhook invalidation listeners
        for (Observable value : observableReferenceMap.keySet()) {
            value.removeListener(weakInvalidationListener);
        }
        observableReferenceMap.clear();
        // unhook list change listeners
        for (ObservableList<?> list : observableListReferenceMap.keySet()) {
            list.removeListener(weakListChangeListener);
        }
        observableListReferenceMap.clear();
    }
}
