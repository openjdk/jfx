/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.text;

/**
 * Represents the hit information in a Text node.
 */
public class HitInfo {

    /**
     * The index of the character which this hit information refers to.
     */
    private int charIndex;
    public int getCharIndex() { return charIndex; }
    public void setCharIndex(int charIndex) { this.charIndex = charIndex; }

    /**
     * Indicates whether the hit is on the leading edge of the character.
     * If it is false, it represents the trailing edge.
     */
    private boolean leading;
    public boolean isLeading() { return leading; }
    public void setLeading(boolean leading) { this.leading = leading; }

    /**
     * Returns the index of the insertion position.
     */
    public int getInsertionIndex() {
        return leading ? charIndex : charIndex + 1;
    }

    @Override public String toString() {
        return "charIndex: " + charIndex + ", isLeading: " + leading;
    }
}
