/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.packrect;

import com.sun.javafx.geom.Rectangle;

class Level {
    int length;
    int size;
    private int sizeOffset;
    private int lengthOffset;

    Level(int length, int size, int sizeOffset) {
        this.length = length;
        this.size = size;
        this.sizeOffset = sizeOffset;
    }

    /**
     * Tries to add the given rectangle to this level.
     */
    boolean add(Rectangle rect, int x, int y, int requestedLength, int requestedSize, boolean vertical) {
        // See whether we can add at the end
        if (lengthOffset + requestedLength <= length && requestedSize <= size) {
            if (vertical) {
                rect.x = sizeOffset;
                rect.y = lengthOffset;
            } else {
                rect.x = lengthOffset;
                rect.y = sizeOffset;
            }
            lengthOffset += requestedLength;

            // this x,y location are external offsets and should not be flipped
            rect.x += x;
            rect.y += y;
            return true;
        }
        return false;
    }
}
