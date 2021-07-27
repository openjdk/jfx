/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property;

import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;

/**
 * Generic interface that defines the methods common to all (writable)
 * properties independent of their type.
 *
 *
 * @param <T>
 *            the type of the wrapped value
 * @since JavaFX 2.0
 */
public interface Property<T> extends ReadOnlyProperty<T>, WritableValue<T> {

    /**
     * Establishes a unidirectional binding between this property (the <em>bound property</em>)
     * and an {@link ObservableValue} (the <em>binding source</em>).
     * <p>
     * After establishing the binding, the value of the bound property is synchronized with the value
     * of the binding source: any change to the value of the binding source will immediately result in
     * the value of the bound property being changed accordingly. Furthermore, the bound property becomes
     * effectively read-only: any call to {@link #setValue(Object)} will fail with an exception.
     * When the binding is first established, the value of the bound property is set to the current value
     * of the binding source.
     * <p>
     * The bound property <em>strongly</em> references the binding source; this means that, as long as
     * the bound property is alive, the binding source will not be garbage-collected. As a consequence,
     * a bound property will not unexpectedly be unbound if its binding source would otherwise become
     * unreachable.
     * <p>
     * Conversely, the binding source only <em>weakly</em> references the bound property. In order to be
     * eligible for garbage collection, a bound property need not be unbound from its binding source.
     * <p>
     * If this method is called when the property is already bound, the previous binding is removed
     * as if by calling {@link #unbind()} before establishing the new binding. If this property is
     * already bidirectionally bound, calling this method will fail with an exception.
     *
     * @param source the binding source
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalStateException if this property is bidirectionally bound
     */
    void bind(ObservableValue<? extends T> source);

    /**
     * Removes the unidirectional binding that was established with {@link #bind(ObservableValue)}.
     * <p>
     * The value of this property will remain unchanged.
     * If this property is not bound, calling this method has no effect.
     */
    void unbind();

    /**
     * Returns whether this property is bound by a unidirectional binding that was
     * established by calling {@link Property#bind(ObservableValue)}.
     * <p>
     * Note that this method does not account for bidirectional bindings that were
     * established by calling {@link Property#bindBidirectional(Property)}.
     *
     * @return whether this property is unidirectionally bound
     */
    boolean isBound();

    /**
     * Establishes a bidirectional binding between this property and another {@link Property}.
     * <p>
     * After establishing the binding, the values of both properties are synchronized: any change
     * to the value of one property will immediately result in the value of the other property being
     * changed accordingly. When the binding is first established, the value of the this property
     * is set to the current value of the other property.
     * <p>
     * While it is not possible for a property to be bound by more than one unidirectional binding,
     * it is legal to establish multiple bidirectional bindings for the same property. However,
     * since a bidirectional binding allows for the values of both properties to be changed
     * by calling {@link #setValue(Object)}, {@link #isBound()} will return {@code false} for
     * both properties.
     * <p>
     * Both properties of a bidirectional binding <em>weakly</em> reference their counterparts.
     * This is different from a unidirectional binding, where the target property <em>strongly</em>
     * references its binding source. In practice, this means that if any of the bidirectionally
     * bound properties become unreachable, the binding is eligible for garbage collection.
     * Furthermore, neither of the bidirectionally bound properties will keep its counterpart
     * alive if the counterpart would otherwise become unreachable.
     * <p>
     * Bidirectional bindings and unidirectional bindings are mutually exclusive. If a property is
     * unidirectionally bound, any attempt to establish a bidirectional binding will fail with an
     * exception.
     * <p>
     * If this property is already bidirectionally bound to the other property, the existing binding
     * will be removed as if by calling {@link #unbindBidirectional(Property)}.
     *
     * @param other the other property
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is {@code this}
     * @throws IllegalStateException if this property or the other property is unidirectionally bound
     *
     * @see #bind(ObservableValue)
     */
    void bindBidirectional(Property<T> other);

    /**
     * Removes the bidirectional binding that was established with {@link #bindBidirectional(Property)}.
     * <p>
     * Bidirectional bindings can be removed by calling this method on either of the two properties:
     * <pre>{@code
     * property1.bindBidirectional(property2);
     * property2.unbindBidirectional(property1);
     * }</pre>
     * If the properties are not bidirectionally bound, calling this method has no effect.
     *
     * @param other the other property
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is {@code this}
     */
    void unbindBidirectional(Property<T> other);

}
