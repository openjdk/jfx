/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.layout;

import com.sun.javafx.scene.AbstractNode;
import com.sun.javafx.sg.prism.NGNode;
import javafx.scene.Node;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import test.com.sun.javafx.scene.layout.MockNodeHelper;


public class MockNode extends AbstractNode {
    static {
        MockNodeHelper.setMockNodeAccessor(new MockNodeHelper.MockNodeAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((MockNode) node).doCreatePeer();
            }
            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((MockNode) node).doComputeGeomBounds(bounds, tx);
            }
            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((MockNode) node).doComputeContains(localX, localY);
            }
        });
    }

    {
        // To initialize the class helper at the begining each constructor of this class
        MockNodeHelper.initHelper(this);
    }

    public MockNode() {
    }

    private NGNode doCreatePeer() { return null; }
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) { return null; }
    private boolean doComputeContains(double localX, double localY) { return false; }
}
