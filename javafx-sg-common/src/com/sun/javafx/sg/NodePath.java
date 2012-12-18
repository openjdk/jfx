/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple a reusable storage for root-to-node path.
 * 
 */
public class NodePath<N extends BaseNode> {
    private List<N> path = new ArrayList<N>();
    private int position;
    
    public NodePath() {
    }
    
    // ITERATION methods
    
    public N getCurrentNode() {
        return path.get(position);
    }

    public boolean hasNext() {
        return position > 0;
    }

    public void next() {
        if (!hasNext()) {
            throw new IllegalStateException();
        }
        position--;
    }
    
    public void reset() {
        position = path.size() - 1;
    }
    
    // MODIFICATION methods
    
    public void clear() {
        position = -1;
        path.clear();
    }
    
    public void add(N n) {
        path.add(n);
        position = path.size() - 1;
    }
    
    public int size() {
        return path.size();
    }
    
    /*
     * Remove root and set to beginning.
     */
    public void removeRoot() {
        path.remove(path.size() - 1);
        position = path.size() - 1;
    }

}
