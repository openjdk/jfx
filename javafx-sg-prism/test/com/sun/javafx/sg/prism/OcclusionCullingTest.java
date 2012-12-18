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
package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.NodePath;
import com.sun.javafx.sg.PGGroup;
import com.sun.javafx.sg.PGNode;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.sun.javafx.sg.prism.NodeTestUtils.*;

/**
 *
 */
public class OcclusionCullingTest {
    
    
    @Test
    public void testRectangleOcclusion() {
        NodeTestUtils.TestNGGroup group = createGroup(
                createRectangle(0, 0, 100, 100), createRectangle(0, 0, 50, 50));
        NodePath<NGNode> rootPath = new NodePath<NGNode>();
        rootPath = group.getRenderRoot(rootPath, new RectBounds(20, 20, 30, 30), -1, BaseTransform.IDENTITY_TRANSFORM, null);
        TestGraphics g = new TestGraphics();
        g.setRenderRoot(rootPath);
        group.render(g);
//        assertSame(group.getChildren().get(1), rootPath.);
        rootPath.reset();
        checkRootRendering(group, rootPath);
    }
    
    @Test
    public void testGroupOcclusion() {
        NodeTestUtils.TestNGGroup group = createGroup(createGroup(
                createRectangle(0, 0, 100, 100)), createGroup(createRectangle(0, 0, 50, 50)));
        NodePath<NGNode> rootPath = new NodePath<NGNode>();
        rootPath = group.getRenderRoot(rootPath, new RectBounds(20, 20, 30, 30), -1, BaseTransform.IDENTITY_TRANSFORM, null);
        TestGraphics g = new TestGraphics();
        g.setRenderRoot(rootPath);
        group.render(g);
//        assertSame(((NGGroup)group.getChildren().get(1)).getChildren().get(0), root);
        rootPath.reset();
        checkRootRendering(group, rootPath);
    }
    
    @Test
    public void testRegionOcclusion() {
        NodeTestUtils.TestNGGroup group = createGroup(
                createRegion(100, 100), createRegion(50, 50));
        NodePath<NGNode> rootPath = new NodePath<NGNode>();
        rootPath = group.getRenderRoot(rootPath, new RectBounds(20, 20, 30, 30), -1, BaseTransform.IDENTITY_TRANSFORM, null);
        TestGraphics g = new TestGraphics();
        g.setRenderRoot(rootPath);
        group.render(g);
//        assertSame(group.getChildren().get(1), root);
        rootPath.reset();
        checkRootRendering(group, rootPath);
    }
    
    @Test
    public void testPresetRegionOcclusion() {
        NodeTestUtils.TestNGGroup group = createGroup(
                createRegion(100, 100), createRegion(50, 50));
        ((NGRegion)group.getChildren().get(1)).setOpaqueInsets(30, 30, 0, 0);
        NodePath<NGNode> rootPath = new NodePath<NGNode>();
        rootPath = group.getRenderRoot(rootPath, new RectBounds(20, 20, 30, 30), -1, BaseTransform.IDENTITY_TRANSFORM, null);
        TestGraphics g = new TestGraphics();
        g.setRenderRoot(rootPath);
        group.render(g);
//        assertSame(group.getChildren().get(0), root);
        rootPath.reset();
        checkRootRendering(group, rootPath);
    }

    private void checkRootRendering(TestNGNode group, NodePath<NGNode> root) {
        assertTrue(group.rendered());
        if (group instanceof TestNGGroup) {
            boolean foundRoot = false;
            for (PGNode p : ((TestNGGroup)group).getChildren()) {
                TestNGNode n = (TestNGNode) p;
                if (n == root.getCurrentNode()) {
                    foundRoot = true;
                    if (root.hasNext()) {
                        root.next();
                        checkRootRendering(n, root);
                        continue;
                    }
                }
                checkRendered(n, foundRoot);
            }
        }
    }

    private void checkRendered(TestNGNode node, boolean rendered) {
        assertEquals(rendered, node.rendered());
        if (node instanceof TestNGGroup) {
             for (PGNode p : ((TestNGGroup)node).getChildren()) {
                 checkRendered((TestNGNode)p, rendered);
             }
        }
    }
    
    
}
