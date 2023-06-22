/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * <p>The package {@code javafx.beans} contains the interfaces that
 *     define the most generic form of observability. All other classes in
 *     the JavaFX library, that are observable, extend the {@link javafx.beans.Observable}
 *     interface.</p>
 * <p>An implementation of {@code Observable} allows to attach an
 *     {@link javafx.beans.InvalidationListener}. The contentBinding gets notified every time
 *     the {@code Observable} may have changed. Typical implementations of
 *     {@code Observable} are all properties, all bindings, {@link
 *     javafx.collections.ObservableList}, and {@link
 *     javafx.collections.ObservableMap}.</p>
 * <p>An {@code InvalidationListener} will get no further information,
 *     e.g. it will not get the old and the new value of a property. If you
 *     need more information consider using a {@link
 *     javafx.beans.value.ChangeListener} for properties and bindings, {@link
 *     javafx.collections.ListChangeListener} for {@code ObservableLists} or
 *     {@link javafx.collections.MapChangeListener} for {@code ObservableMap}
 *     instead.</p>
 */
package javafx.beans;
