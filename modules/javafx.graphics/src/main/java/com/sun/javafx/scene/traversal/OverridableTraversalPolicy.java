/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.traversal.TraversalDirection;
import javafx.scene.traversal.TraversalPolicy;

/**
 * Non-traversable policy which allows for overriding of {@link #isParentTraversable(Parent)}.
 */
public class OverridableTraversalPolicy extends TraversalPolicy {
    private Boolean overridenTraversability;

    public OverridableTraversalPolicy() {
    }

    @Override
    public Node select(Parent root, Node owner, TraversalDirection dir) {
        return null;
    }

    @Override
    public Node selectFirst(Parent root) {
        return null;
    }

    @Override
    public Node selectLast(Parent root) {
        return null;
    }

    /**
     * @param value overridden value or null for default value
     */
    public void setOverriddenFocusTraversability(Boolean value) {
        overridenTraversability = value;
    }

    @Override
    public boolean isParentTraversable(Parent root) {
        // This means the traversability can be overriden only for traversable root.
        // If user explicitly disabled traversability, we don't set it back to true
        return overridenTraversability != null ?
            root.isFocusTraversable() && overridenTraversability :
            root.isFocusTraversable();
    }
}
