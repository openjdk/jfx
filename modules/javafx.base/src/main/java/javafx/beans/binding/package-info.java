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
 * <p>Provides classes that create and operate on a {@link Binding Binding}
 *     that calculates a value that depends on one or more sources.</p>
 * <h2>Characteristics of Bindings</h2>
 * <p>Bindings are assembled from one or more sources, usually called
 *     their dependencies. A binding observes its dependencies for changes
 *     and updates its own value according to changes in the dependencies.</p>
 * <p>Almost all bindings defined in this library require
 *     implementations of {@link javafx.beans.Observable} for their
 *     dependencies. There are two types of implementations already provided,
 *     the properties in the package {@link javafx.beans.property} and the
 *     observable collections ({@link javafx.collections.ObservableList} and
 *     {@link javafx.collections.ObservableMap}). Bindings also implement
 *     {@code Observable} and can again serve as sources for other bindings
 *     allowing to construct very complex bindings from simple ones.</p>
 * <p>Bindings in our implementation are always calculated lazily.
 *     That means, if a dependency changes, the result of a binding is not
 *     immediately recalculated, but it is marked as invalid. Next time the
 *     value of an invalid binding is requested, it is recalculated.</p>
 * <h2>High Level API and Low Level API</h2>
 * <p>The Binding API is roughly divided in two parts, the High Level
 *     Binding API and the Low Level Binding API. The High Level Binding API
 *     allows to construct simple bindings in an easy to use fashion.
 *     Defining a binding with the High Level API should be straightforward,
 *     especially when used in an IDE that provides code completion.
 *     Unfortunately it has its limitation and at that point the Low Level
 *     API comes into play. Experienced Java developers can use the Low Level
 *     API to define bindings, if the functionality of the High Level API is
 *     not sufficient or to improve the performance. The main goals of the
 *     Low Level API are fast execution and small memory footprint.</p>
 * <p>Following is an example of how both APIs can be used. Assuming
 *     we have four instances of {@link
 *     javafx.beans.property.DoubleProperty} {@code a}, {@code b}, {@code
 *     c} , and {@code d}, we can define a binding that calculates {@code a*b
 *     + c*d} with the High Level API for example like this:</p>
 * <p>{@code NumberBinding result = Bindings.add (a.multiply(b),
 *     c.multiply(d)); }</p>
 * <p>Defining the same binding using the Low Level API could be done
 *     like this:</p>
 * <pre>
 * <code>
 * DoubleBinding foo = new DoubleBinding() {
 *
 *     {
 *         super.bind(a, b, c, d);
 *     }
 *
 *     &#x40;Override
 *     protected double computeValue() {
 *         return a.getValue() * b.getValue() + c.getValue() * d.getValue();
 *     }
 * };
 * </code>
 * </pre>
 */
package javafx.beans.binding;
