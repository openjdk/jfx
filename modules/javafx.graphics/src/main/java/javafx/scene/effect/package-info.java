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
 * <p>Provides the set of classes for attaching graphical filter effects to JavaFX Scene Graph Nodes.</p>
 * <p>An effect is a graphical algorithm that produces an image, typically
 *    as a modification of a source image.
 *    An effect can be associated with a scene graph {@code Node} by setting the
 *    {@link javafx.scene.Node#effectProperty effect}
 *    attribute.
 *    Some effects change the color properties of the source pixels
 *    (such as {@link javafx.scene.effect.ColorAdjust}),
 *    others combine multiple images together (such as
 *    {@link javafx.scene.effect.Blend Blend}),
 *    while still others warp or move the pixels of the source image around (such as
 *    {@link javafx.scene.effect.DisplacementMap DisplacementMap} or {@link javafx.scene.effect.PerspectiveTransform PerspectiveTransform}).
 *    All effects have at least one input defined and the input can be set
 *    to another effect to chain the effects together and combine their
 *    results, or it can be left unspecified in which case the effect will
 *    operate on a graphical rendering of the node it is attached to.</p>
 */
package javafx.scene.effect;
