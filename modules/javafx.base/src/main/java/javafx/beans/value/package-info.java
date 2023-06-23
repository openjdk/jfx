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
 * <p>The package {@code javafx.beans.value} contains the two
 *     fundamental interfaces {@link javafx.beans.value.ObservableValue} and {@link
 *     javafx.beans.value.WritableValue} and all of its sub-interfaces.</p>
 *
 * <h2>ObservableValue</h2>
 * An ObservableValue wraps a value that can be read and observed for
 * invalidations and changes. Listeners have to implement either {@link
 * javafx.beans.InvalidationListener} or {@link javafx.beans.value.ChangeListener}. To allow
 * working with primitive types directly a number of sub-interfaces are
 * defined.
 * <table>
 *     <caption>ObservableValue Table</caption>
 *     <tr>
 *         <th scope="col">Type</th>
 *         <th scope="col">Sub-interface of ObservableValue</th>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code boolean}</th>
 *         <td>{@link javafx.beans.value.ObservableBooleanValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code double}</th>
 *         <td>{@link javafx.beans.value.ObservableDoubleValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code float}</th>
 *         <td>{@link javafx.beans.value.ObservableFloatValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code int}</th>
 *         <td>{@link javafx.beans.value.ObservableIntegerValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code long}</th>
 *         <td>{@link javafx.beans.value.ObservableLongValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code double}, {@code float}, {@code int}, {@code long}</th>
 *         <td>{@link javafx.beans.value.ObservableNumberValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code Object}</th>
 *         <td>{@link javafx.beans.value.ObservableObjectValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code String}</th>
 *         <td>{@link javafx.beans.value.ObservableStringValue}</td>
 *     </tr>
 * </table>
 *
 * <h2>WritableValue</h2>
 * A WritableValue wraps a value that can be read and set. As with {@code
 * ObservableValues}, a number of sub-interfaces are defined to work with
 * primitive types directly.
 * <table>
 *     <caption>WritableValue Table</caption>
 *     <tr>
 *         <th scope="col">Type</th>
 *         <th scope="col">Sub-interface of WritableValue</th>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code boolean}</th>
 *         <td>{@link javafx.beans.value.WritableBooleanValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code double}</th>
 *         <td>{@link javafx.beans.value.WritableDoubleValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code float}</th>
 *         <td>{@link javafx.beans.value.WritableFloatValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code int}</th>
 *         <td>{@link javafx.beans.value.WritableIntegerValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code long}</th>
 *         <td>{@link javafx.beans.value.WritableLongValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code double}, {@code float}, {@code int}, {@code long}</th>
 *         <td>{@link javafx.beans.value.WritableNumberValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code Object}</th>
 *         <td>{@link javafx.beans.value.WritableObjectValue}</td>
 *     </tr>
 *     <tr>
 *         <th scope="row">{@code String}</th>
 *         <td>{@link javafx.beans.value.WritableStringValue}</td>
 *     </tr>
 * </table>
 */
package javafx.beans.value;
