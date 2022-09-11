/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Function;

import com.sun.javafx.binding.FlatMappedBinding;
import com.sun.javafx.binding.MappedBinding;
import com.sun.javafx.binding.OrElseBinding;

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
     * Returns an {@code ObservableValue} that holds the result of applying the
     * given mapping function on this value. The result is updated when this
     * {@code ObservableValue} changes. If this value is {@code null}, no
     * mapping is applied and the resulting value is also {@code null}.
     * <p>
     * For example, mapping a string to an upper case string:
     * <pre>{@code
     * var text = new SimpleStringProperty("abcd");
     * ObservableValue<String> upperCase = text.map(String::toUpperCase);
     *
     * upperCase.getValue();  // Returns "ABCD"
     * text.set("xyz");
     * upperCase.getValue();  // Returns "XYZ"
     * text.set(null);
     * upperCase.getValue();  // Returns null
     * }</pre>
     *
     * @param <U> the type of values held by the resulting {@code ObservableValue}
     * @param mapper the mapping function to apply to a value, cannot be {@code null}
     * @return an {@code ObservableValue} that holds the result of applying the given
     *     mapping function on this value, or {@code null} when it
     *     is {@code null}; never returns {@code null}
     * @throws NullPointerException if the mapping function is {@code null}
     * @since 19
     */
    default <U> ObservableValue<U> map(Function<? super T, ? extends U> mapper) {
        return new MappedBinding<>(this, mapper);
    }

    /**
     * Returns an {@code ObservableValue} that holds this value, or the given constant if
     * it is {@code null}. The result is updated when this {@code ObservableValue} changes. This
     * method, when combined with {@link #map(Function)}, allows handling of all values
     * including {@code null} values.
     * <p>
     * For example, mapping a string to an upper case string, but leaving it blank
     * if the input is {@code null}:
     * <pre>{@code
     * var text = new SimpleStringProperty("abcd");
     * ObservableValue<String> upperCase = text.map(String::toUpperCase).orElse("");
     *
     * upperCase.getValue();  // Returns "ABCD"
     * text.set(null);
     * upperCase.getValue();  // Returns ""
     * }</pre>
     *
     * @param constant the value to use when this {@code ObservableValue}
     *     holds {@code null}; can be {@code null}
     * @return an {@code ObservableValue} that holds this value, or the given constant if
     *     it is {@code null}; never returns {@code null}
     * @since 19
     */
    default ObservableValue<T> orElse(T constant) {
        return new OrElseBinding<>(this, constant);
    }

    /**
     * Returns an {@code ObservableValue} that holds the value of an {@code ObservableValue}
     * produced by applying the given mapping function on this value. The result is updated
     * when either this {@code ObservableValue} or the {@code ObservableValue} produced by
     * the mapping changes. If this value is {@code null}, no mapping is applied and the
     * resulting value is {@code null}. If the mapping resulted in {@code null}, then the
     * resulting value is also {@code null}.
     * <p>
     * This method is similar to {@link #map(Function)}, but the mapping function is
     * one whose result is already an {@code ObservableValue}, and if invoked, {@code flatMap} does
     * not wrap it within an additional {@code ObservableValue}.
     * <p>
     * For example, a property that is only {@code true} when a UI element is part of a {@code Scene}
     * that is part of a {@code Window} that is currently shown on screen:
     * <pre>{@code
     * ObservableValue<Boolean> isShowing = listView.sceneProperty()
     *     .flatMap(Scene::windowProperty)
     *     .flatMap(Window::showingProperty)
     *     .orElse(false);
     *
     * // Assuming the listView is currently shown to the user, then:
     *
     * isShowing.getValue();  // Returns true
     *
     * listView.getScene().getWindow().hide();
     * isShowing.getValue();  // Returns false
     *
     * listView.getScene().getWindow().show();
     * isShowing.getValue();  // Returns true
     *
     * listView.getParent().getChildren().remove(listView);
     * isShowing.getValue();  // Returns false
     * }</pre>
     * Changes in any of the values of: the scene of {@code listView}, the window of that scene, or
     * the showing of that window, will update the boolean value {@code isShowing}.
     * <p>
     * This method is preferred over {@link javafx.beans.binding.Bindings#select Bindings} methods
     * since it is type safe.
     *
     * @param <U> the type of values held by the resulting {@code ObservableValue}
     * @param mapper the mapping function to apply to a value, cannot be {@code null}
     * @return an {@code ObservableValue} that holds the value of an {@code ObservableValue}
     *     produced by applying the given mapping function on this value, or
     *     {@code null} when the value is {@code null}; never returns {@code null}
     * @throws NullPointerException if the mapping function is {@code null}
     * @since 19
     */
    default <U> ObservableValue<U> flatMap(Function<? super T, ? extends ObservableValue<? extends U>> mapper) {
        return new FlatMappedBinding<>(this, mapper);
    }
}
