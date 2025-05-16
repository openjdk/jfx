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

package com.sun.javafx.text;

import com.sun.javafx.scene.text.TabAdvancePolicy;

/**
 * TabAdvancePolicy based on a fixed tab size.
 */
public class FixedTabAdvancePolicy implements TabAdvancePolicy {

    private final float tabAdvance;

    /**
     * Creates a tab advance policy for the given tab size.
     *
     * @param tabSize the tab size
     * @param spaceAdvance the advance of the space character
     */
    public FixedTabAdvancePolicy(int tabSize, float spaceAdvance) {
        this.tabAdvance = Math.max(1, tabSize) * spaceAdvance;
    }

    @Override
    public float nextTabStop(float offset, float position) {
        if (tabAdvance == 0.0) {
            return -1.0f;
        }
        return nextPosition(position, tabAdvance);
    }

    static float nextPosition(float position, float tabAdvance) {
        // there is a weird case (tabAdvance=57.6 and position=172.79999)
        // when the original formula
        // float f = ((int)(position / tabAdvance) + 1) * tabAdvance;
        // returns the same pos=172.79999 next=172.79999
        float n = (position / tabAdvance);
        return ((int)(n + Math.ulp(n)) + 1) * tabAdvance;
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof FixedTabAdvancePolicy p) {
            return tabAdvance == p.tabAdvance;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = FixedTabAdvancePolicy.class.hashCode();
        return h * 31 + Float.floatToIntBits(tabAdvance);
    }

    @Override
    public String toString() {
        return "FixedTabAdvancePolicy{tabAdvance=" + tabAdvance + "}";
    }
}
