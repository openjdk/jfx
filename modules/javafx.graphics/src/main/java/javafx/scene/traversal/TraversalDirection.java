/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.traversal;

import javafx.geometry.NodeOrientation;

/**
 * Specifies the direction of focus traversal.
 *
 * @since 999 TODO
 */
public enum TraversalDirection {
    /** Moving focus downward. */
    DOWN,
    /** Moving focus left. */
    LEFT,
    /** Moving focus to the next focusable Node. */
    NEXT,
    /** Moving focus to the next in line focusable Node (like NEXT, but does not traverse into the current parent). */
    NEXT_IN_LINE,
    /** Moving focus to the previous focusable Node. */
    PREVIOUS,
    /** Moving focus right. */
    RIGHT,
    /** Moving focus upward. */ 
    UP;

    /**
     * Returns true if the traversal is considered a forward movement.
     * @return true if forward
     */
    public boolean isForward() {
        switch (this) {
        case UP:
        case LEFT:
        case PREVIOUS:
            return false;
        }
        return true;
    }

    /**
     * Returns the direction with respect to the node's orientation. It affect's only arrow keys however, so it's not
     * an error to ignore this call if handling only next/previous traversal.
     *
     * @param orientation the node orientation
     * @return the traverse direction
     */
    public TraversalDirection getDirectionForNodeOrientation(NodeOrientation orientation) {
        if (orientation == NodeOrientation.RIGHT_TO_LEFT) {
            switch (this) {
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            }
        }
        return this;
    }
}
