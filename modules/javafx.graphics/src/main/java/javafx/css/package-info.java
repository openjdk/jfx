/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * <p>Provides API for making properties styleable via CSS and for supporting
 * pseudo-class state.</p>
 *
 * <p>The JavaFX Scene Graph provides the facility to style nodes using
 * CSS (Cascading Style Sheets).
 * The {@link javafx.scene.Node Node} class contains {@code id}, {@code styleClass}, and
 * {@code style} variables which are used by CSS selectors to find nodes
 * to which styles should be applied. The {@link javafx.scene.Scene Scene} class and
 * {@link javafx.scene.Parent Parent} class contain a
 * the {@code stylesheets} variable which is a list of URLs that
 * reference CSS style sheets that are to be applied to the nodes within
 * that scene or parent.
 * <p>The primary classes in this package are:</p>
 *
 * <dl>
 *
 * <dt>{@link javafx.css.CssMetaData CssMetaData}</dt>
 * <dd>Defines the CSS property and provides a link back to the
 *     {@link javafx.css.StyleableProperty StyleableProperty}.
 *     By convention, classes that have CssMetaData implement a
 *     {@code public static List<CssMetaData<? extends Styleable>> getClassCssMetaData()} method that
 *     allows other classes to include CssMetaData from an inherited class. The
 *     method {@link javafx.scene.Node#getCssMetaData() getCssMetaData()} should
 *     be overridden to return {@code getClassCssMetaData()}. The CSS implementation
 *     frequently calls {@code getCssMetaData()}. It is strongly recommended that
 *     the returned list be a {@code final static}.</dd>
 *
 * <dt>{@link javafx.css.StyleableProperty StyleableProperty}</dt>
 * <dd>Defines the interface that the CSS implementation uses to set values on a
 *     property and provides a link back to the {@code CssMetaData} that
 *     corresponds to the property. The {@link javafx.css.StyleablePropertyFactory StyleablePropertyFactory}
 *     greatly simplifies creating a StyleableProperty and its corresponding CssMetaData.</dd>
 *
 * <dt>{@link javafx.css.PseudoClass PseudoClass}</dt>
 * <dd>Defines a pseudo-class which can be set or cleared via the method
 *     {@link javafx.scene.Node#pseudoClassStateChanged(javafx.css.PseudoClass, boolean)
 *     pseudoClassStateChanged}. </dd>
 *
 * </dl>
 *
 * <p>For further information about CSS, how to apply CSS styles
 * to nodes, and what properties are available for styling, see the
 * <a href="../scene/doc-files/cssref.html">CSS Reference Guide</a>.</p>
 */
package javafx.css;
