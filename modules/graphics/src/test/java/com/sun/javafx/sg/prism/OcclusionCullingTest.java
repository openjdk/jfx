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

import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class OcclusionCullingTest extends NGTestBase {


    @Test
    public void testRectangleOcclusion() {
        final TestNGRectangle root = createRectangle(0, 0, 50, 50);
        TestNGGroup group = createGroup(
                createRectangle(0, 0, 100, 100), root);
        NodePath rootPath = new NodePath();
        group.getRenderRoot(rootPath, new RectBounds(20, 20, 30, 30), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        TestGraphics g = new TestGraphics();
        g.setRenderRoot(rootPath);
        group.render(g);
        assertRoot(rootPath, root);
        checkRootRendering(group, rootPath);
    }

    @Test
    public void testGroupOcclusion() {
        final TestNGRectangle root = createRectangle(0, 0, 50, 50);
        TestNGGroup group = createGroup(createGroup(
                createRectangle(0, 0, 100, 100)), createGroup(root));
        NodePath rootPath = new NodePath();
        group.getRenderRoot(rootPath, new RectBounds(20, 20, 30, 30), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        TestGraphics g = new TestGraphics();
        g.setRenderRoot(rootPath);
        group.render(g);
        assertRoot(rootPath, root);
        checkRootRendering(group, rootPath);
    }

    @Test
    public void testRegionOcclusion() {
        final TestNGRegion root = createRegion(50, 50);
        TestNGGroup group = createGroup(
                createRegion(100, 100), root);
        NodePath rootPath = new NodePath();
        group.getRenderRoot(rootPath, new RectBounds(20, 20, 30, 30), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        TestGraphics g = new TestGraphics();
        g.setRenderRoot(rootPath);
        group.render(g);
        assertRoot(rootPath, root);
        checkRootRendering(group, rootPath);
    }

    @Test
    public void testPresetRegionOcclusion() {
        final TestNGRegion root = createRegion(100, 100);
        final TestNGRegion other = createRegion(50, 50);
        TestNGGroup group = createGroup(
                root, other);
        other.setOpaqueInsets(30, 30, 0, 0);
        NodePath rootPath = new NodePath();
        group.getRenderRoot(rootPath, new RectBounds(20, 20, 30, 30), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        TestGraphics g = new TestGraphics();
        g.setRenderRoot(rootPath);
        group.render(g);
        assertRoot(rootPath, root);
        checkRootRendering(group, rootPath);
    }

    @Test
    public void test2SameRectanglesOcclusion() {
        final TestNGRectangle root = createRectangle(10, 10, 100, 100);
        TestNGGroup group = createGroup(
                createGroup(createRectangle(10, 10, 100, 100), createRectangle(20, 20, 20, 20)),
                createGroup(root));
        NodePath rootPath = new NodePath();
        group.getRenderRoot(rootPath, new RectBounds(10, 10, 100, 100), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        TestGraphics g = new TestGraphics();
        g.setRenderRoot(rootPath);
        group.render(g);
        assertRoot(rootPath, root);
        checkRootRendering(group, rootPath);
    }

    @Test
    public void test2SameRectanglesOcclusionWithRootNotDirty() {
        final TestNGRectangle root = createRectangle(10, 10, 100, 100);
        final TestNGGroup rootParent = createGroup(root);
        TestNGGroup group = createGroup(
                createGroup(createRectangle(10, 10, 100, 100), createRectangle(20, 20, 20, 20)), rootParent);

        group.dirty =  NGNode.DirtyFlag.CLEAN; // need to clean default dirty flags
        rootParent.dirty = NGNode.DirtyFlag.CLEAN;
        rootParent.childDirty = false;
        root.dirty = NGNode.DirtyFlag.CLEAN;
        root.childDirty = false;
        NodePath rootPath = new NodePath();
        group.getRenderRoot(rootPath, new RectBounds(10, 10, 100, 100), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertTrue(rootPath.isEmpty());

        final TestNGRectangle dirtySibling = createRectangle(0,0,10,10);
        rootParent.add(-1, dirtySibling);
        rootPath = new NodePath();
        group.getRenderRoot(rootPath, new RectBounds(10, 10, 100, 100), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertRoot(rootPath, root);
    }

    @Test
    public void testTransparentRegionWithChildren() {
        final TestNGRectangle root = createRectangle(10, 10, 100, 100);
        final TestNGGroup rootParent = createGroup(root);
        TestNGRegion region = createTransparentRegion(0, 0, 100, 100,
                createGroup(createRectangle(10, 10, 100, 100), createRectangle(20, 20, 20, 20)), rootParent);

        region.dirty =  NGNode.DirtyFlag.CLEAN; // need to clean default dirty flags
        rootParent.dirty = NGNode.DirtyFlag.CLEAN;
        rootParent.childDirty = false;
        root.dirty = NGNode.DirtyFlag.CLEAN;
        root.childDirty = false;
        NodePath rootPath = new NodePath();
        region.getRenderRoot(rootPath, new RectBounds(10, 10, 100, 100), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertTrue(rootPath.isEmpty());

        final TestNGRectangle dirtySibling = createRectangle(0,0,10,10);
        rootParent.add(-1,dirtySibling);
        rootPath = new NodePath();
        region.getRenderRoot(rootPath, new RectBounds(10, 10, 100, 100), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertRoot(rootPath, root);
    }

    @Test
    public void testOpaqueRegion() {
        final TestNGRectangle rect = createRectangle(10, 10, 100, 100);
        TestNGRegion region = createOpaqueRegion(0, 0, 200, 200, rect);
        TestNGGroup root = createGroup(region);

        NodePath rootPath = new NodePath();
        root.getRenderRoot(rootPath, new RectBounds(10, 10, 100, 100), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertRoot(rootPath, rect);

        rootPath.clear();
        root.getRenderRoot(rootPath, new RectBounds(5, 5, 150, 150), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertRoot(rootPath, region);
        TestGraphics g = new TestGraphics();
        g.setRenderRoot(rootPath);
        root.render(g);
        checkRootRendering(root, rootPath);

        rootPath.clear();
        root.getRenderRoot(rootPath, new RectBounds(-5, -5, 150, 150), -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertRoot(rootPath, root);
    }

    private void checkRootRendering(TestNGNode node, NodePath root) {
        assertTrue(node.rendered());
        if (node instanceof TestNGGroup) {
            if (root.hasNext()) {
                boolean foundRoot = false;
                root.next();
                for (NGNode p : ((TestNGGroup)node).getChildren()) {
                    TestNGNode n = (TestNGNode) p;
                    if (n == root.getCurrentNode()) {
                        foundRoot = true;
                        checkRootRendering(n, root);
                        continue;
                    }
                    checkRendered(n, foundRoot);
                }
            } else {
                for (NGNode p : ((TestNGGroup)node).getChildren()) {
                    checkRendered((TestNGNode)p, true);
                }
            }
        }
    }

    private void checkRendered(TestNGNode node, boolean rendered) {
        assertEquals(rendered, node.rendered());
        if (node instanceof TestNGGroup) {
             for (NGNode p : ((TestNGGroup)node).getChildren()) {
                 checkRendered((TestNGNode)p, rendered);
             }
        }
    }

    private void assertRoot(NodePath rootPath, final NGNode root) {
        rootPath.reset();
        while(rootPath.hasNext()) {
            rootPath.next();
        }
        assertSame(root, rootPath.getCurrentNode());
        rootPath.reset();
    }


}
