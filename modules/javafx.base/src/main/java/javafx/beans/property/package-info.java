/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>The package {@code javafx.beans.property} defines read-only
 *     properties and writable properties, plus a number of implementations.
 * </p>
 * <h2>Read-only Properties</h2>
 * <p>Read-only properties have two getters, {@code get()} returns the
 *     primitive value, {@code getValue()} returns the boxed value.</p>
 * <p>It is possible to observe read-only properties for changes. They
 *     define methods to add and remove {@link
 *     javafx.beans.InvalidationListener InvalidationListeners} and {@link
 *     javafx.beans.value.ChangeListener ChangeListeners}.</p>
 * <p>To get the context of a read-only property, two methods {@code
 *     getBean()} and {@code getName()} are defined. They return the
 *     containing bean and the name of a property.</p>
 *
 * <h2>Writable Properties</h2>
 * <p>In addition to the functionality defined for read-only
 *     properties, writable properties contain the following methods.</p>
 * <p>A writable property defines two setters in addition to the
 *     getters defined for read-only properties. The setter {@code set()}
 *     takes a primitive value, the second setter {@code setValue()} takes
 *     the boxed value.</p>
 * <p>All properties can be bound to {@link
 *     javafx.beans.value.ObservableValue ObservableValues} of the same type,
 *     which means that the property will always contain the same value as
 *     the bound {@code ObservableValue}. It is also possible to define a
 *     bidirectional binding between two properties, so that both properties
 *     always contain the same value. If one of the properties changes, the
 *     other one will be updated.</p>
 */
package javafx.beans.property;
