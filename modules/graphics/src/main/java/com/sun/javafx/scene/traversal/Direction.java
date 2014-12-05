/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.traversal;

import javafx.geometry.NodeOrientation;

/**
 * Specifies the direction of traversal.
 */
public enum Direction {

    UP(false),
    DOWN(true),
    LEFT(false),
    RIGHT(true),
    NEXT(true),
    NEXT_IN_LINE(true), // Like NEXT, but does not traverse into the current parent
    PREVIOUS(false);
    private final boolean forward;

    Direction(boolean forward) {
        this.forward = forward;
    }

    public boolean isForward() {
        return forward;
    }

    /**
     * Returns the direction with respect to the node's orientation. It affect's only arrow keys however, so it's not
     * an error to ignore this call if handling only next/previous traversal.
     * @param orientation
     * @return
     */
    public Direction getDirectionForNodeOrientation(NodeOrientation orientation) {
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
