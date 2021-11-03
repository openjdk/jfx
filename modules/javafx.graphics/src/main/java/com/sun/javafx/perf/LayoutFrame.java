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

package com.sun.javafx.perf;

import javafx.scene.Parent;
import java.util.ArrayList;
import java.util.List;

public class LayoutFrame {

    private final Parent node;
    private final boolean layoutRoot;
    private final List<LayoutFrame> children;
    private int passes;

    LayoutFrame(Parent node, boolean layoutRoot) {
        this.node = node;
        this.layoutRoot = layoutRoot;
        this.children = new ArrayList<>();
    }

    public Parent getNode() {
        return node;
    }

    public boolean isLayoutRoot() {
        return layoutRoot;
    }

    public List<LayoutFrame> getChildren() {
        return children;
    }

    public int getPasses() {
        return passes;
    }

    public int getCumulativePasses() {
        int total = passes;
        for (LayoutFrame child : children) {
            total += child.getCumulativePasses();
        }

        return total;
    }

    LayoutFrame getFrame(Parent node) {
        for (int i = 0, size = children.size(); i < size; ++i) {
            LayoutFrame frame = children.get(i);
            if (frame.node == node) {
                return frame;
            }
        }

        return null;
    }

    void updatePasses(int passes) {
        this.passes += passes;
    }

}
