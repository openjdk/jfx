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

package com.sun.javafx.pgstub;

import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.sg.PGGroup;
import com.sun.javafx.sg.PGNode;

public class StubGroup extends StubNode implements PGGroup {
    public ArrayList<PGNode> content = new ArrayList<PGNode>();
    public ArrayList<PGNode> removed = new ArrayList<PGNode>();

    @Override
    public List<PGNode> getChildren() {
        return new ArrayList<PGNode>(content);
    }

    @Override
    public void add(int index, PGNode node) {
        StubNode oldParent = ((StubNode)node).getParent();
        if (index == -1) {
            content.add(node);
        } else {
            content.add(index, node);
        }
        ((StubNode)node).setParent(this);
    }

    @Override public void clearFrom(int fromIndex) {
        if (fromIndex < content.size()) {
            content.subList(fromIndex, content.size()).clear();
        }
    }
        
    @Override
    public void remove(PGNode node) {
        content.remove(node);
        ((StubNode)node).setParent(null);
    }

    @Override
    public void remove(int index) {
        if (index >= 0 && index < content.size()) {
            remove(content.get(index));
        }
    }

    @Override
    public void clear() {
        for (PGNode node : content) {
            ((StubNode)node).setParent(null);
        }
        content.clear();
    }

    @Override
    public void setBlendMode(Object blendMode) {
        // ignore
    }

    @Override
    public List<PGNode> getRemovedChildren() {
        return new ArrayList<PGNode>(removed);
    }

    @Override
    public void addToRemoved(PGNode node) {
        removed.add(node);
    }

    @Override
    public void markDirty() {
        // mark whole group dirty
    }
}
