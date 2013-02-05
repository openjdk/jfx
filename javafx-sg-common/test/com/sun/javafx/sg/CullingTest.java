/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import junit.framework.Assert;
import javafx.scene.shape.Rectangle;
import org.junit.Test;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;

public class CullingTest {

    @Test
    public void test_setCullBits_intersect() {
        BaseNode bn = getBaseNode(new Rectangle(0, 0, 100, 100));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(50, 50, 150, 150),
                    new RectBounds(70, 70, 170, 170)
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);
        Assert.assertEquals(1 |(1 << 2), bn.cullingBits);
    }

    @Test
    public void test_setCullBits_disjoint() {
        BaseNode bn = getBaseNode(new Rectangle(0, 0, 100, 100));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(110, 110, 150, 150),
                    new RectBounds(0, 101, 170, 170)
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);
        Assert.assertEquals(0, bn.cullingBits);
    }

    @Test
    public void test_setCullBits_within() {
        BaseNode bn = getBaseNode(new Rectangle(50, 50, 100, 100));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(40, 40, 170, 170),
                    new RectBounds(0, 0, 200, 200)
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);
        Assert.assertEquals(2 | (2 << 2), bn.cullingBits);
    }
    
    @Test
    public void test_setCullBits_region_within() {
        BaseNode bn = getBaseNode(new Rectangle(0, 0, 100, 100));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(40, 40, 70, 70),
                    new RectBounds(10, 10, 20, 20)
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);
        Assert.assertEquals(1 | (1 << 2), bn.cullingBits);
    }

    @Test
    public void test_setCullBits_empty() {
        BaseNode bn = getBaseNode(new Rectangle(50, 50, 100, 100));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(40, 40, 170, 170),
                    new RectBounds().makeEmpty()
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);
        Assert.assertEquals(2, bn.cullingBits);
    }

    @Test
    public void test_setCullBits_null() {
        BaseNode bn = getBaseNode(new Rectangle(50, 50, 100, 100));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(40, 40, 170, 170),
                    null
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);
        Assert.assertEquals(2, bn.cullingBits);
    }

    @Test
    public void test_setCullBits_empty_regions() {
        BaseNode bn = getBaseNode(new Rectangle(50, 50, 100, 100));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{});
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);
        Assert.assertEquals(0, bn.cullingBits);
    }

    @Test
    public void test_group_disjoint() {
        Node g = group(new Rectangle(150, 0, 50, 50), new Rectangle(150, 60, 50, 50));
        g.setTranslateX(10);
        g.setTranslateY(10);
        BaseNode gbn = getBaseNode(g);
        BaseNode bn1 = getBaseNode(((Group)g).getChildren().get(0));
        BaseNode bn2 = getBaseNode(((Group)g).getChildren().get(1));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 100, 100), new RectBounds(0, 110, 100, 210)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);
        Assert.assertEquals(0, gbn.cullingBits);
        for(Node n:((Group)g).getChildren()) {
            Assert.assertEquals(0, getBaseNode(n).cullingBits);
        }
    }

    @Test
    public void test_group_intersect() {
        Node g = group(new Rectangle(50, 50, 30, 30), new Rectangle(50, 120, 30, 30));
        g.setTranslateX(10);
        g.setTranslateY(10);
        BaseNode gbn = getBaseNode(g);
        BaseNode bn1 = getBaseNode(((Group)g).getChildren().get(0));
        BaseNode bn2 = getBaseNode(((Group)g).getChildren().get(1));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 100, 100), new RectBounds(0, 110, 100, 210)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);

        //check group
        Assert.assertEquals(1 | (1 << 2), gbn.cullingBits);
        
        //check children               
        Assert.assertEquals(2, bn1.cullingBits);
        Assert.assertEquals(2 << 2, bn2.cullingBits);
    }
    
    @Test
    public void test_group_within() {
        Node g = group(new Rectangle(50, 50, 30, 30), new Rectangle(50, 10, 30, 30));
        g.setTranslateX(10);
        g.setTranslateY(10);
        BaseNode gbn = getBaseNode(g);
        BaseNode bn1 = getBaseNode(((Group)g).getChildren().get(0));
        BaseNode bn2 = getBaseNode(((Group)g).getChildren().get(1));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 100, 100), new RectBounds(0, 110, 100, 210)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);

        //check group
        Assert.assertEquals(2, gbn.cullingBits);
        
        //check children (as the group is "completely covered", the children should not have been processed)          
        Assert.assertEquals(0, bn1.cullingBits);
        Assert.assertEquals(0, bn2.cullingBits);
    }
    
    @Test
    public void test_region_within_group() {
        Node g = group(new Rectangle(50, 10, 100, 100), new Rectangle(50, 120, 100, 100));
        g.setTranslateX(5);
        g.setTranslateY(5);
        BaseNode gbn = getBaseNode(g);
        BaseNode bn1 = getBaseNode(((Group)g).getChildren().get(0));
        BaseNode bn2 = getBaseNode(((Group)g).getChildren().get(1));
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(70, 20, 100, 105), new RectBounds(70, 130, 100, 200)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);

        //check group
        Assert.assertEquals(1 | (1 << 2), gbn.cullingBits);

        //check children
        Assert.assertEquals(1, bn1.cullingBits);
        Assert.assertEquals(1 << 2, bn2.cullingBits);
    }

    @Test
    public void test_region_within_group_group() {
        Node g1 = group(new Rectangle(50, 10, 100, 100), new Rectangle(50, 120, 100, 100));
        g1.setTranslateX(5);
        g1.setTranslateY(5);
        BaseNode g1bn = getBaseNode(g1);
        BaseNode bn1 = getBaseNode(((Group)g1).getChildren().get(0));
        BaseNode bn2 = getBaseNode(((Group)g1).getChildren().get(1));

        Node g = group(g1, new Rectangle(200, 200, 100, 100));
        g.setTranslateX(5);
        g.setTranslateY(5);
        BaseNode gbn = getBaseNode(g);
        BaseNode bn3 = getBaseNode(((Group)g).getChildren().get(1));

        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(70, 25, 100, 105), new RectBounds(70, 130, 100, 200)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);

        //check group
        Assert.assertEquals(1 | (1 << 2), gbn.cullingBits);

        //check group1
        Assert.assertEquals(1 | (1 << 2), g1bn.cullingBits);

        //check children
        Assert.assertEquals(1, bn1.cullingBits);
        Assert.assertEquals(1 << 2, bn2.cullingBits);
        Assert.assertEquals(0, bn3.cullingBits);
    }

    @Test
    public void test_region_group_full_withing_then_partial() {
        Node g = group(new Rectangle(50, 50, 100, 100));
        BaseNode gbn = getBaseNode(g);
        BaseNode bn1 = getBaseNode(((Group)g).getChildren().get(0));

        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 160, 160), new RectBounds(80, 80, 100, 100)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, null);

        Assert.assertEquals(2 | (1 << 2), gbn.cullingBits);
        Assert.assertEquals(1 << 2, bn1.cullingBits);
    }

    public static BaseNode getBaseNode(Node n) {
        Scene.impl_setAllowPGAccess(true);
        // Eeek, this is gross! I have to use reflection to invoke this
        // method so that bounds are updated...
        try {
            java.lang.reflect.Method method = Node.class.getDeclaredMethod("updateBounds");
            method.setAccessible(true);
            method.invoke(n);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update bounds", e);
        }
        n.impl_updatePG();
        BaseNode bn = (BaseNode)n.impl_getPGNode();        
        Scene.impl_setAllowPGAccess(false);
        return bn;
    }

    public static Node group(Node... n) {
        Group g = new Group(n);
        return g;
    }

    public static BaseBounds getBounds(Node n, BaseTransform tx) {
        PGNode pgn = getBaseNode(n);
        return ((BaseNode) pgn).getContentBounds(new RectBounds(), tx);
    }
}
