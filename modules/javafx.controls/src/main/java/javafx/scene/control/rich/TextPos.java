/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control.rich;

/**
 * An immutable text position.
 * Because it is immutable, it cannot track locations in the document which is being edited.
 * For that, use {@link Marker}. 
 */
public record TextPos(int index, int offset) implements Comparable<TextPos> {

    public static final TextPos ZERO = new TextPos(0, 0);

    @Override
    public int compareTo(TextPos p) {
        int d = index - p.index;
        if (d == 0) {
            int off = offset();
            int poff = p.offset();
            if (off < poff) {
                return -1;
            } else if (off > poff) {
                return 1;
            }
            return 0;
        }
        return d;
    }

    public static TextPos min(TextPos a, TextPos b) {
        int cmp = a.compareTo(b);
        if (cmp <= 0) {
            return a;
        } else {
            return b;
        }
    }
    
    public String toString() {
        return "TextPos{" + index + "," + offset + "}";
    }

    /** returns true if index and offset are the same. */
    public boolean isSameIndexAndOffset(TextPos p) {
        // added this method in case we need to add leading/trailing flag
        // semantics of this test is the insertion points are the same.
        if (p != null) {
            if (index == p.index) {
                return (offset == p.offset);
            }
        }
        return false;
    }
}
