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

package com.sun.javafx.scene;

/**
 * Flags used for processing the CSS pass in the scenegraph
 */
public enum CssFlags {
    // NOTE: Order is significant as I use a quick ordinal() check for
    // determining whether to continue processing, so these are ranked
    // according to precedence
    /**
     * Indicates that the node is clean from here on down and does not
     * require any work on the next CSS pass
     */
    CLEAN,
    /**
     * DIRTY_BRANCH means that this node is CLEAN but one of its children,
     * or grandchildren, etc is UPDATE or REAPPLY so we need to step into this branch.
     */
    DIRTY_BRANCH,
    /**
     * Indicates that we must update properties for this node and all child
     * nodes. This is typically in response to a pseudoclass state change and
     * is much faster than a REAPPLY.
     */
    UPDATE,
    /**
     * Indicates that we must reapply all the styles from this point downwards
     * in the tree, including figuring out which styles apply to each Node. This
     * is the most expensive CSS operation that can occur, and usually only
     * happens when the CSS tree has changed.
     */
    REAPPLY
}
