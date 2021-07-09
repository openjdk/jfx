/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.value;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

/**
 * An {@code ObservableValue} is an entity that wraps a value and allows to
 * observe the value for changes. In general this interface should not be
 * implemented directly but one of its sub-interfaces
 * ({@code ObservableBooleanValue} etc.).
 * <p>
 * The value of the {@code ObservableValue} can be requested with
 * {@link #getValue()}.
 * <p>
 * An implementation of {@code ObservableValue} may support lazy evaluation,
 * which means that the value is not immediately recomputed after changes, but
 * lazily the next time the value is requested (see note 1 in "Implementation Requirements").
 * <p>
 * An {@code ObservableValue} generates two types of events: change events and
 * invalidation events. A change event indicates that the value has changed
 * (see note 2 in "Implementation Requirements"). An
 * invalidation event is generated if the current value is not valid anymore.
 * This distinction becomes important if the {@code ObservableValue} supports
 * lazy evaluation, because for a lazily evaluated value one does not know if an
 * invalid value really has changed until it is recomputed. For this reason,
 * generating change events requires eager evaluation while invalidation events
 * can be generated for eager and lazy implementations.
 * <p>
 * Implementations of this class should strive to generate as few events as
 * possible to avoid wasting too much time in event handlers. Implementations in
 * this library mark themselves as invalid when the first invalidation event
 * occurs. They do not generate any more invalidation events until their value is
 * recomputed and valid again.
 * <p>
 * Two types of listeners can be attached to an {@code ObservableValue}:
 * {@link InvalidationListener} to listen to invalidation events and
 * {@link ChangeListener} to listen to change events.
 * <p>
 * Important note: attaching a {@code ChangeListener} enforces eager computation
 * even if the implementation of the {@code ObservableValue} supports lazy
 * evaluation.
 *
 * @param <T>
 *            The type of the wrapped value.
 *
 * @implSpec <ol>
 * <li> All bindings and properties in the JavaFX library support lazy evaluation.</li>
 * <li> All implementing classes in the JavaFX library check for a change using reference
 * equality (and not object equality, {@code Object#equals(Object)}) of the value.</li>
 * </ol>
 *
 * @see ObservableBooleanValue
 * @see ObservableDoubleValue
 * @see ObservableFloatValue
 * @see ObservableIntegerValue
 * @see ObservableLongValue
 * @see ObservableNumberValue
 * @see ObservableObjectValue
 * @see ObservableStringValue
 *
 *
 * @since JavaFX 2.0
 */
public interface ObservableValue<T> extends Observable {

    /**
     * Adds a {@link ChangeListener} which will be notified whenever the value
     * of the {@code ObservableValue} changes. If the same listener is added
     * more than once, then it will be notified more than once. That is, no
     * check is made to ensure uniqueness.
     * <p>
     * Note that the same actual {@code ChangeListener} instance may be safely
     * registered for different {@code ObservableValues}.
     * <p>
     * The {@code ObservableValue} stores a strong reference to the listener
     * which will prevent the listener from being garbage collected and may
     * result in a memory leak. It is recommended to either unregister a
     * listener by calling {@link #removeListener(ChangeListener)
     * removeListener} after use or to use an instance of
     * {@link WeakChangeListener} avoid this situation.
     *
     * @see #removeListener(ChangeListener)
     *
     * @param listener
     *            The listener to register
     * @throws NullPointerException
     *             if the listener is null
     */
    void addListener(ChangeListener<? super T> listener);

    /**
     * Removes the given listener from the list of listeners that are notified
     * whenever the value of the {@code ObservableValue} changes.
     * <p>
     * If the given listener has not been previously registered (i.e. it was
     * never added) then this method call is a no-op. If it had been previously
     * added then it will be removed. If it had been added more than once, then
     * only the first occurrence will be removed.
     *
     * @see #addListener(ChangeListener)
     *
     * @param listener
     *            The listener to remove
     * @throws NullPointerException
     *             if the listener is null
     */
    void removeListener(ChangeListener<? super T> listener);

    /**
     * Returns the current value of this {@code ObservableValue}
     *
     * @return The current value
     */
    T getValue();

    /**
     * Returns an {@link ObservableValue} which holds a mapping of the value
     * held by this {@code ObservableValue}, and is {@code null} when this
     * {@code ObservableValue} is {@code null}.
     *
     * @param <U> the type of values held by the resulting {@code ObservableValue}
     * @param mapper a {@link Function} which converts a given value to a new value, cannot be null
     * @return an {@link ObservableValue} which holds a mapping of the value
     *     held by this {@code ObservableValue}, and is {@code null} when this
     *     {@code ObservableValue} is {@code null}, never null
     */
    default <U> ObservableValue<U> map(Function<? super T, ? extends U> mapper) {
        return Bindings.mapping(this, Objects.requireNonNull(mapper));
    }

    /**
     * Returns an {@link ObservableValue} which holds a mapping of the value
     * held by this {@code ObservableValue}, or the value held by
     * {@code alternativeValue} when this {@code ObservableValue} is
     * {@code null}.
     *
     * @param alternativeValue an alternative value to use when the value held
     *     by this {@code ObservableValue} is {@code null}, can be null
     * @return an {@link ObservableValue} which holds a mapping of the value
     *     held by this {@code ObservableValue}, or the value held by {@code
     *     alternativeValue} when this {@code ObservableValue} is {@code null},
     *     never null
     */
    default ObservableValue<T> orElse(T alternativeValue) {
        return orElseGet(() -> alternativeValue);
    }

    /**
     * Returns an {@link ObservableValue} which holds a mapping of the value
     * held by this {@code ObservableValue}, or the value supplied by {@code
     * supplier} when this {@code ObservableValue} is {@code null}.
     *
     * @param supplier a {@link Supplier} to use when the value held by this
     *     {@code ObservableValue} is {@code null}, cannot be null
     * @return an {@link ObservableValue} which holds a mapping of the value
     *     held by this {@code ObservableValue}, or the value supplied by
     *     {@code supplier} when this {@code ObservableValue} is {@code null},
     *     never null
     */
    default ObservableValue<T> orElseGet(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);

        return Bindings.nullableMapping(this, v -> v == null ? supplier.get() : v);
    }

    /**
     * Returns an {@link ObservableValue} which holds the value in the {@code
     * ObservableValue} given by applying {@code mapper} on the value of this
     * {@code ObservableValue}, and is {@code null} when this
     * {@code ObservableValue} is {@code null}.<p>
     *
     * Returning {@code null} from {@code mapper} will result in an
     * {@code ObservableValue} which holds {@code null}.
     *
     * @param <U> the type of values held by the resulting {@code ObservableValue}
     * @param mapper a {@link Function} which converts a given value to an
     *     {@code ObservableValue}, cannot be null
     * @return an {@link ObservableValue} which holds the value in the
     *     {@code ObservableValue} given by applying {@code mapper} on the value
     *     of this {@code ObservableValue}, and is {@code null} when this
     *     {@code ObservableValue} is {@code null}, never null
     */
    default <U> ObservableValue<U> flatMap(Function<? super T, ? extends ObservableValue<? extends U>> mapper) {
        return Bindings.flatMapping(this, Objects.requireNonNull(mapper));
    }

    /**
     * Returns an {@link ObservableValue} which holds the value in this
     * {@code ObservableValue} whenever the given {@code condition} evaluates to
     * {@code true}, otherwise holds the value held by this {@code ObservableValue}
     * when {@code condition} became {@code false}.<p>
     *
     * The returned {@code ObservableValue} only observes this
     * {@code ObservableValue} when the given {@code condition} evaluates to
     * {@code true}. This allows the returned {@code ObservableValue} to be
     * garbage collected if not otherwise strongly referenced when
     * {@code condition} becomes {@code false}.<p>
     *
     * Returning {@code null} from the given {@code condition} is treated the
     * same as returning {@code false}.
     *
     * @param condition a boolean {@code ObservableValue}, cannot be null
     * @return an {@link ObservableValue} which holds the value in this
     *     {@code ObservableValue} whenever the given {@code condition}
     *     evaluates to {@code true}, otherwise holds the value held by this
     *     {@code ObservableValue} when {@code condition} became {@code false},
     *     never null
     */
    default ObservableValue<T> conditionOn(ObservableValue<Boolean> condition) {
        return Bindings.conditional(this, condition);
    }

    /**
     * Creates a {@link Subscription} on this {@link ObservableValue} which
     * immediately provides its current value to the given {@code subscriber},
     * followed by any subsequent changes in value.
     *
     * @param subscriber a {@link Consumer} to supply with the values of this
     *     {@link ObservableValue}, cannot be null
     * @return a {@link Subscription} which can be used to cancel this
     *     subscription, never null
     */
    default Subscription subscribe(Consumer<? super T> subscriber) {
        ChangeListener<T> listener = (obs, old, current) -> subscriber.accept(current);

        subscriber.accept(getValue());  // eagerly send current value
        addListener(listener);

        return () -> removeListener(listener);
    }

    /**
     * Creates a {@link Subscription} on this {@link ObservableValue} which
     * calls the given {@code runnable} whenever this {@code ObservableValue}
     * becomes invalid.
     *
     * @param runnable a {@link Runnable} to call whenever this
     *     {@link ObservableValue} becomes invalid, cannot be null
     * @return a {@link Subscription} which can be used to cancel this
     *     subscription, never null
     */
    default Subscription subscribeInvalidations(Runnable runnable) {
        InvalidationListener listener = obs -> runnable.run();

        addListener(listener);

        return () -> removeListener(listener);
    }
}
