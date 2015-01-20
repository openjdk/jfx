/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.Parent;

/**
 * This traversal engine can be used to change algorithm for some specific parent/control that needs different traversal.
 * This can be achieved by setting such engine using {@link Parent#setImpl_traversalEngine(ParentTraversalEngine)}
 * and providing a special Algorithm implementation.
 *
 * Alternatively, the traversal engine can be w/o an algorithm and used just for listening to focus changes inside the specified parent.
 */
public final class ParentTraversalEngine extends TraversalEngine{

    private final Parent root;
    private Boolean overridenTraversability;

    public ParentTraversalEngine(Parent root, Algorithm algorithm) {
        super(algorithm);
        this.root = root;
    }

    public ParentTraversalEngine(Parent root) {
        super();
        this.root = root;
    }

    /**
     * @param value overridden value or null for default value
     */
    public void setOverriddenFocusTraversability(Boolean value) {
        overridenTraversability = value;
    }

    @Override
    protected Parent getRoot() {
        return root;
    }

    public boolean isParentTraversable() {
        // This means the traversability can be overriden only for traversable root.
        // If user explicitly disabled traversability, we don't set it back to true
        return overridenTraversability != null ? root.isFocusTraversable() && overridenTraversability : root.isFocusTraversable();
    }

}
