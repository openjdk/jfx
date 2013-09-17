/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

/**
 * Cache hints for use with {@code Node.cacheHint}
 *
 * @see Node#cacheHintProperty
 * @since JavaFX 2.0
 */
public enum CacheHint {
   /**
    * No additional hint. The system will determine the best use of the bitmap
    * cache.
    */
    DEFAULT,

   /**
    * A hint to tell the bitmap caching mechanism that this node is animating,
    * and should be painted from the bitmap cache whenever possible in order to
    * maintain smooth animation. The trade-off is that this may result in
    * decreased visual quality.
    */
    SPEED,

   /**
    * A hint to tell the bitmap caching mechanism that this node should appear
    * on screen at the highest visual quality. The cached bitmap will only be
    * used when it will not degrade the node's appearance on screen.
    * <p>
    * The trade-off is that animations may cause subtle variations in the way
    * that a node would be rendered, and so a node with a cacheHint of QUALITY
    * may be required to re-render a node even when such subtle variations would
    * not be visible in the midst of an animation.  As such, a node with a
    * cacheHint of QUALITY will often benefit from having its cacheHint
    * replaced with a more permissive value (such as {@code SPEED}) during the
    * period of the animation.
    */
    QUALITY,

   /**
    * A hint to tell the bitmap caching mechanism that if the node is scaled up
    * or down, it is acceptable to paint it by scaling the cached bitmap (rather
    * than re-rendering the node).
    */
    SCALE,

   /**
    * A hint to tell the bitmap caching mechanism that if the node is rotated,
    * it is acceptable to paint it by rotating the cached bitmap (rather
    * than re-rendering the node).
    */
    ROTATE,

   /**
    * A hint to tell the bitmap caching mechanism that if the node is scaled
    * and/or rotated, it is acceptable to paint it by scaling and/or rotating
    * the cached bitmap (rather than re-rendering the node).
    */
    SCALE_AND_ROTATE,
}
