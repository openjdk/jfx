/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.LayoutFlags;

import java.util.List;
import javafx.collections.ObservableList;

public class ParentShim extends Parent {

    public static final int DIRTY_CHILDREN_THRESHOLD = Parent.DIRTY_CHILDREN_THRESHOLD;

    @Override
    public ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    public static ObservableList<Node> getChildren(Parent p) {
        return p.getChildren();
    }

    public static <E extends Node> List<E> getManagedChildren(Parent p) {
        return p.getManagedChildren();
    }

    public static void setNeedsLayout(Parent p, boolean value) {
        p.setNeedsLayout(value);
    }

    public static LayoutFlags getLayoutFlag(Parent p) {
        return p.layoutFlag;
    }

    public static List<Node> test_getRemoved(Parent p) {
        return p.test_getRemoved();
    }

    public static List<Node> test_getViewOrderChildren(Parent p) {
        return p.test_getViewOrderChildren();
    }
}
