/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.binding;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

/**
 * A {@code Binding} calculates a value that depends on one or more sources. The
 * sources are usually called the dependency of a binding. A binding observes
 * its dependencies for changes and updates its value automatically.
 * <p>
 * While a dependency of a binding can be anything, it is almost always an
 * implementation of {@link javafx.beans.value.ObservableValue}. {@code Binding}
 * implements {@code ObservableValue} allowing to use it in another binding.
 * With that one can assemble very complex bindings from simple bindings.
 * <p>
 * All bindings in the JavaFX runtime are calculated lazily. That means, if
 * a dependency changes, the result of a binding is not immediately
 * recalculated, but it is marked as invalid. Next time the value of an invalid
 * binding is requested, it is recalculated.
 * <p>
 * It is recommended to use one of the base classes defined in this package
 * (e.g. {@link DoubleBinding}) to define a custom binding, because these
 * classes already provide most of the needed functionality. See
 * {@link DoubleBinding} for an example.
 *
 * @param <T> the type of the wrapped value
 * @see DoubleBinding
 *
 * @since JavaFX 2.0
 */
public interface Binding<T> extends ObservableValue<T> {

    /**
     * Checks if a binding is valid.
     *
     * @return {@code true} if the {@code Binding} is valid, {@code false}
     *         otherwise
     */
    boolean isValid();

    /**
     * Mark a binding as invalid. This forces the recalculation of the value of
     * the {@code Binding} next time it is request.
     */
    void invalidate();

    /**
     * Returns the dependencies of a binding in an unmodifiable
     * {@link javafx.collections.ObservableList}. The implementation is
     * optional. The main purpose of this method is to support developers during
     * development. It allows to explore and monitor dependencies of a binding
     * during runtime.
     * <p>
     * Because this method should not be used in production code, it is
     * recommended to implement this functionality as sparse as possible. For
     * example if the dependencies do not change, each call can generate a new
     * {@code ObservableList}, avoiding the necessity to store the result.
     *
     * @return an unmodifiable {@code} ObservableList of the dependencies
     */
    ObservableList<?> getDependencies();

    /**
     * Signals to the {@code Binding} that it will not be used anymore and any
     * references can be removed. A call of this method usually results in the
     * binding stopping to observe its dependencies by unregistering its
     * listener(s). The implementation is optional.
     * <p>
     * All bindings in our implementation use instances of
     * {@link javafx.beans.WeakInvalidationListener}, which means usually
     * a binding does not need to be disposed. But if you plan to use your
     * application in environments that do not support {@code WeakReferences}
     * you have to dispose unused {@code Bindings} to avoid memory leaks.
     */
    void dispose();

}
