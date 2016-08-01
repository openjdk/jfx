/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple a reusable storage for root-to-node path.
 *
 */
public class NodePath {
    private List<NGNode> path = new ArrayList<>();
    private int position;

    public NGNode last() {
        return path.isEmpty() ? null : path.get(path.size() - 1);
    }

    // ITERATION methods

    public NGNode getCurrentNode() {
        return path.get(position);
    }

    public boolean hasNext() {
        return position < path.size() -1 && !isEmpty();
    }

    public void next() {
        if (!hasNext()) {
            throw new IllegalStateException();
        }
        position++;
    }

    public void reset() {
        position = path.isEmpty() ? -1 : 0;
    }

    // MODIFICATION methods

    public void clear() {
        position = -1;
        path.clear();
    }

    public void add(NGNode n) {
        path.add(0, n);
        if (position == -1) position = 0;
    }

    public int size() {
        return path.size();
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    @Override public String toString() {
        return path.toString();
    }
}
