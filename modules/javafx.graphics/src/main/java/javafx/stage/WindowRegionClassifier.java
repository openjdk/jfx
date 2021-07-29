/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

import javafx.scene.Node;

/**
 * Classifier that is used by windows with the {@link StageStyle#UNDECORATED_INTERACTIVE} style
 * to determine which region of a window was interacted with.
 *
 * @since 18
 */
@FunctionalInterface
public interface WindowRegionClassifier {

    /**
     * Determines which window region corresponds to the specified coordinates.
     *
     * @param x x-coordinate, relative to the left window border
     * @param y y-coordinate, relative to the top window border
     * @param node the top-most node at the position indicated by {@code x} and {@code y},
     *             or {@code null} if there is no node at this position
     * @return the window region
     */
    WindowRegion classify(double x, double y, Node node);

}
