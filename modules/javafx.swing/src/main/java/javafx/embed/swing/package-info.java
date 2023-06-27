/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * <p>Defines APIs for the JavaFX / Swing interop support included with the
 *    JavaFX UI toolkit, including {@link javafx.embed.swing.SwingNode} (for
 *    embedding Swing inside a JavaFX application) and
 *    {@link javafx.embed.swing.JFXPanel} (for embedding JavaFX inside a Swing
 *    application). </p>
 * <p>The {@link javafx.embed.swing.JFXPanel JFXPanel} class defines a
 *    lightweight Swing component, which accepts and renders an instance of
 *    {@link javafx.scene.Scene Scene} and forwards all input events from Swing
 *    to the attached JavaFX scene.
 *    The {@link javafx.embed.swing.SwingNode} class is used to embed
 *    a Swing content into a JavaFX application.
 *    The content to be displayed is specified with the {@code SwingNode.setContent} method
 *    that accepts an instance of a Swing {@code JComponent}. The hierarchy of components
 *    contained in the {@code JComponent} instance should not contain any heavyweight
 *    components, otherwise {@code SwingNode} may fail to paint it. The content gets
 *    repainted automatically. All the input and focus events are forwarded to the
 *    {@code JComponent} instance.</p>
 */
package javafx.embed.swing;
