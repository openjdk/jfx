/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * <p>The JavaFX User Interface Controls (UI Controls or just Controls) are
 * specialized Nodes in the JavaFX Scenegraph especially suited for reuse in
 * many different application contexts. They are designed to be highly
 * customizable visually by designers and developers. They are designed to work
 * well with layout systems. Examples of prominent controls include {@link javafx.scene.control.Button Button},
 * {@link javafx.scene.control.Label Label}, {@link javafx.scene.control.ListView ListView}, and {@link javafx.scene.control.TextField TextField}.</p>
 *
 * <p>Since Controls are {@link javafx.scene.Node Nodes} in the scenegraph,
 * they can be freely mixed with {@link javafx.scene.Group Groups},
 * {@link javafx.scene.image.ImageView Images},
 * {@link javafx.scene.media.MediaView Media},
 * {@link javafx.scene.text.Text Text}, and
 * {@link javafx.scene.shape.Shape basic geometric shapes}. While
 * writing new UI Controls is not trivial, using and styling them
 * is very easy, especially to existing web developers.</p>
 *
 * <p>The remainder of this document will describe the basic architecture of
 * the JavaFX UI Control library, how to style existing controls, write custom
 * skins, and how to use controls to build up more complicated user interfaces.
 * </p>
 *
 * <h2>Architecture</h2>
 *
 * <p>Controls follow the classic MVC design pattern. The {@link javafx.scene.control.Control Control} is
 * the "model". It contains both the state and the functions which manipulate
 * that state. The Control class itself does not know how it is rendered or
 * what the user interaction is. These tasks are delegated to the
 * {@link javafx.scene.control.Skin Skin} ("view"), which may internally separate
 * out the view and controller functionality into separate classes, although
 * at present there is no public API for the "controller" aspect.</p>
 *
 * <p>All Controls extend from the Control class, which is in turn a
 * {@link javafx.scene.Parent Parent} node, and which is a
 * {@link javafx.scene.Node Node}. Every Control has a reference to a single Skin, which
 * is the view implementation for the Control. The Control delegates to the
 * Skin the responsibility of computing the min, max, and pref sizes of the
 * Control, the baseline offset, and hit testing (containment and
 * intersection). It is also the responsibility of the Skin, or a delegate of
 * the Skin, to implement and repond to all relevant key
 * events which occur on the Control when it contains the focus.</p>
 *
 * <h2>Control</h2>
 *
 * <p>Control extends from {@link javafx.scene.Parent Parent}, and as such, is
 * not a leaf node. From the perspective of a developer or designer the Control
 * can be thought of as if it were a leaf node in many cases. For example, the
 * developer or designer can consider a Button as if it were a Rectangle or
 * other simple leaf node.</p>
 *
 * <p>Since a Control is resizable, a Control
 * will be <strong>auto-sized to its preferred size</strong> on each scenegraph
 * pulse. Setting the width and height of the Control does not affect its
 * preferred size. When used in a layout container, the layout constraints
 * imposed upon the Control (or manually specified on the Control) will
 * determine how it is positioned and sized.</p>
 *
 * <p>The Skin of a Control can be changed at any time. Doing so will mark the
 * Control as needing to be laid out since changing the Skin likely has changed
 * the preferred size of the Control. If no Skin is specified at the time that
 * the Control is created, then a default CSS-based skin will be provided for
 * all of the built-in Controls.</p>
 *
 * <p>Each Control may have an optional tooltip specified. The Tooltip is a
 * Control which displays some (usually textual) information about the control
 * to the user when the mouse hovers over the Control from some period of time.
 * It can be styled from CSS the same as with other Controls.</p>
 *
 * <p>{@code focusTraversable} is overridden in Control to be true by default,
 * whereas with Node it is false by default. Controls which should not be
 * focusable by default (such as Label) override this to be false.</p>
 *
 * <p>The getMinWidth, getMinHeight, getPrefWidth, getPrefHeight, getMaxWidth,
 * and getMaxHeight functions are delegated directly to the Skin. The
 * baselineOffset method is delegated to the node of the skin. It is not
 * recommended that subclasses alter these delegations.</p>
 *
 * <h2>Styling Controls</h2>
 *
 * <p>There are two methods for customizing the look of a Control. The most
 * difficult and yet most flexible approach is to write a new Skin for the
 * Control which precisely implements the visuals which you
 * desire for the Control. Consult the Skin documentation for more details.</p>
 *
 * <p>The easiest and yet very powerful method for styling the built in
 * Controls is by using CSS. Please note that in this release the following
 * CSS description applies only to the default Skins provided for the built
 * in Controls. Subsequent releases will make this generally available for
 * any custom third party Controls which desire to take advantage of these
 * CSS capabilities.</p>
 *
 * <p>Each of the default Skins for the built in Controls is comprised of
 * multiple individually styleable areas or regions. This is much like an
 * HTML page which is made up of {@literal <div>'s} and then styled from
 * CSS. Each individual region may be drawn with backgrounds, borders, images,
 * padding, margins, and so on. The JavaFX CSS support includes the ability
 * to have multiple backgrounds and borders, and to derive colors. These
 * capabilities make it extremely easy to alter the look of Controls in
 * JavaFX from CSS.</p>
 *
 * <p>The colors used for drawing the default Skins of the built in Controls
 * are all derived from a base color, an accent color and a background
 * color. Simply by modifying the base color for a Control you can alter the
 * derived gradients and create Buttons or other Controls which visually fit
 * in with the default Skins but visually stand out.</p>
 *
 * <p>As with all other Nodes in the scenegraph, Controls can be styled by
 * using an external stylesheet, or by specifying the style directly on the
 * Control. Although for examples it is easier to express and understand by
 * specifying the style directly on the Node, it is recommended to use an
 * external stylesheet and use either the styleClass or id of the Control,
 * just as you would use the "class" or id of an HTML element with HTML
 * CSS.</p>
 *
 * <p>Each UI Control specifies a styleClass which may be used to
 * style controls from an external stylesheet. For example, the Button
 * control is given the "button" CSS style class. The CSS style class names
 * are hyphen-separated lower case as opposed to camel case, otherwise, they
 * are exactly the same. For example, Button is "button", RadioButton is
 * "radio-button", Tooltip is "tooltip" and so on.</p>
 *
 * <p>The class documentation for each Control defines the default Skin
 * regions which can be styled. For further information regarding the CSS
 * capabilities provided with JavaFX, see the
 * <a href="../../../../javafx.graphics/javafx/scene/doc-files/cssref.html">CSS Reference Guide</a>.</p>
 */
package javafx.scene.control;
