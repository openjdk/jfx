/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

/**
 * Enumeration used to determine the grow (or shrink) priority of a given node's
 * layout area when its region has more (or less) space available and
 * multiple nodes are competing for that space.
 *
 * @since JavaFX 2.0
 */
public enum Priority {
    /**
     * Layout area will always try to grow (or shrink), sharing the increase
     * (or decrease) in space with other layout areas that have a grow
     * (or shrink) of ALWAYS.
     */
    ALWAYS,

    /**
     * If there are no other layout areas with grow (or shrink) set to ALWAYS
     * or those layout areas didn't absorb all of the increased (or decreased) space,
     * then will share the increase (or decrease) in space with other
     * layout area's of SOMETIMES.
     */
    SOMETIMES,

    /**
     * Layout area will never grow (or shrink) when there is an increase (or
     * decrease) in space available in the region.
     */
    NEVER;

    /**
     * Convenience method for returning the higher of two priorities.
     * @param a first priority
     * @param b second priority
     * @return the highest of the two priorities
     */
    public static Priority max(Priority a, Priority b) {
        if (a == ALWAYS || b == ALWAYS) {
            return ALWAYS;
        } else if (a == SOMETIMES || b == SOMETIMES) {
            return SOMETIMES;
        } else {
            return NEVER;
        }
    }

    /**
     * Convenience method for returning the lower of two priorities.
     * @param a first priority
     * @param b second priority
     * @return the lower of the two priorities
     */
    public static Priority min(Priority a, Priority b) {
        if (a == NEVER || b == NEVER) {
            return NEVER;
        } else if (a == SOMETIMES || b == SOMETIMES) {
            return SOMETIMES;
        } else {
            return ALWAYS;
        }
    }
}
