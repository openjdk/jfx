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

package com.sun.javafx.scene;

public enum InvalidateLayoutOption {
    /**
     * The current node will be scheduled for layout.
     */
    LOCAL_LAYOUT,

    /**
     * The parent node will be scheduled for layout, except if the parent node is currently
     * performing layout. In this case, no further layout pass will be scheduled.
     */
    PARENT_LAYOUT,

    /**
     * The parent node will be scheduled for layout. If the parent node is currently
     * performing layout, a new layout pass will be scheduled.
     */
    FORCE_PARENT_LAYOUT,

    /**
     * All parent nodes (up to the layout root) will be scheduled for layout.
     * If a parent node is currently performing layout, a new layout pass will be scheduled.
     */
    FORCE_ROOT_LAYOUT
}
