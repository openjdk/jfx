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
 * <p>
 * Because it is immutable, it cannot track locations in the document which is being edited.
 * For that, use {@link Marker}. 
 */
public final class TextPos implements Comparable<TextPos> {
    public static final TextPos ZERO = new TextPos(0, 0, 0, true);
    private final int index;
    private final int offset;
    private final int charIndex;
    private final boolean leading;

    public TextPos(int index, int offset, int charIndex, boolean leading) {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        } else if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
        } else if (charIndex < 0) {
            throw new IllegalArgumentException("charIndex cannot be negative");
        }
        this.index = index;
        this.offset = offset;
        this.charIndex = charIndex;
        this.leading = leading;
    }

    public TextPos(int index, int offset) {
        this(index, offset, offset, true);
    }

    /** returns the model paragraph index */
    public int index() {
        return index;
    }

    /** returns the offset into the plain text string (insertion index) */
    public int offset() {
        return offset;
    }

    /** returns the character index index */
    public int charIndex() {
        return charIndex;
    }

    /** returns whether the text position is leading or trailing */
    public boolean isLeading() {
        return leading;
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof TextPos p) {
            return
                (index == p.index) &&
                (charIndex == p.charIndex) &&
                (offset == p.offset) &&
                (leading == p.leading);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = TextPos.class.hashCode();
        h = 31 * h + index;
        h = 31 * h + charIndex;
        h = 31 * h + offset;
        h = 31 * h + (leading ? 1 : 0);
        return h;
    }

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
            if(leading != p.leading) {
                return leading ? 1 : -1;
            }
            return 0;
        }
        return d;
    }

    public String toString() {
        return
            "TextPos{" +
            "ix=" + index +
            ", off=" + offset +
            ", cix=" + charIndex +
            (leading ? ", leading" : ", trailing") +
            "}";
    }

    /** returns true if the specified insertion point is the same. */
    public boolean isSameInsertionIndex(TextPos p) {
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
