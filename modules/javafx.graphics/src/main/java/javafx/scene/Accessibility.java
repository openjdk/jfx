/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observes accessibility attribute changes across the scene graph, for example from
 * tooling or tests, regardless of whether an assistive technology is active.
 *
 * @since 28
 */
public final class Accessibility {

    private static final List<AccessibleAttributeObserver> observers = new CopyOnWriteArrayList<>();

    private Accessibility() {
    }

    /**
     * Adds an observer notified whenever any {@link Node} reports an accessibility
     * attribute change.
     *
     * @param observer the observer to add
     */
    public static void addAccessibleAttributeObserver(AccessibleAttributeObserver observer) {
        observers.add(observer);
    }

    /**
     * Removes a previously added observer.
     *
     * @param observer the observer to remove
     */
    public static void removeAccessibleAttributeObserver(AccessibleAttributeObserver observer) {
        observers.remove(observer);
    }

    static void notifyObservers(Node node, AccessibleAttribute attribute) {
        for (AccessibleAttributeObserver observer : observers) {
            observer.attributeChanged(node, attribute);
        }
    }
}
