/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.text;

/**
 * This class encapsulates an immutable single tab stop within the {@link TabStopPolicy}.
 *
 * @since 25
 */
public final class TabStop {
    private final double position;

    /**
     * Constructs a new tab stop with the specified position.
     *
     * @param position the position in pixels
     */
    public TabStop(double position) {
        this.position = position;
    }

    /**
     * Returns the position, in pixels, of the tab.
     * @return the position of the tab
     */
    public final double getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof TabStop p) {
            return position == p.position;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = TabStop.class.hashCode();
        h = 31 * h + Double.hashCode(position);
        return h;
    }
}
