/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;

import javafx.scene.Parent;

import com.sun.javafx.util.WeakReferenceQueue;

/**
 * The base class for defining node-specific layout constraints.  Region
 * classes may create extensions of this class if they need to define their own
 * set of layout constraints.
 *
 * @since JavaFX 2.0
 */
public abstract class ConstraintsBase {

    /**
     * If set as max value indicates that the pref value should be used as the max.
     * This allows an application to constrain a resizable node's size, which normally
     * has an unlimited max value, to its preferred size.
     */
    public static final double CONSTRAIN_TO_PREF = Double.NEGATIVE_INFINITY;

    private WeakReferenceQueue nodes = new WeakReferenceQueue();

    ConstraintsBase() {
    }

    void add(Parent node) {
        nodes.add(node);
    }

    void remove(Parent node) {
        nodes.remove(node);
    }

    /**
     * Calls requestLayout on layout parent associated with this constraint object.
     */
    protected void requestLayout() {
        Iterator<Parent> nodeIter = nodes.iterator();

        while (nodeIter.hasNext()) {
            nodeIter.next().requestLayout();
        }
    }
}
