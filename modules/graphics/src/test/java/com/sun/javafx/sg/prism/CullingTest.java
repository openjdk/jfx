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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import org.junit.Assert;
import org.junit.Test;

public class CullingTest extends NGTestBase {

    @Test
    public void test_setCullBits_intersect() {
        NGNode bn = createRectangle(0, 0, 100, 100);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(50, 50, 150, 150),
                    new RectBounds(70, 70, 170, 170)
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        Assert.assertEquals(1 |(1 << 2), bn.cullingBits);
    }

    @Test
    public void test_setCullBits_disjoint() {
        NGNode bn = createRectangle(0, 0, 100, 100);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(110, 110, 150, 150),
                    new RectBounds(0, 101, 170, 170)
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        Assert.assertEquals(0, bn.cullingBits);
    }

    @Test
    public void test_setCullBits_within() {
        NGNode bn = createRectangle(50, 50, 100, 100);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(40, 40, 170, 170),
                    new RectBounds(0, 0, 200, 200)
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        Assert.assertEquals(2 | (2 << 2), bn.cullingBits);
    }
    
    @Test
    public void test_setCullBits_region_within() {
        NGNode bn = createRectangle(0, 0, 100, 100);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(40, 40, 70, 70),
                    new RectBounds(10, 10, 20, 20)
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        Assert.assertEquals(1 | (1 << 2), bn.cullingBits);
    }

    @Test
    public void test_setCullBits_empty() {
        NGNode bn = createRectangle(50, 50, 100, 100);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(40, 40, 170, 170),
                    new RectBounds().makeEmpty()
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        Assert.assertEquals(2, bn.cullingBits);
    }

    @Test
    public void test_setCullBits_null() {
        NGNode bn = createRectangle(50, 50, 100, 100);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]
                    {
                    new RectBounds(40, 40, 170, 170),
                    null
                    });
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        Assert.assertEquals(2, bn.cullingBits);
    }

    @Test
    public void test_setCullBits_empty_regions() {
        NGNode bn = createRectangle(50, 50, 100, 100);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{});
        bn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        Assert.assertEquals(0, bn.cullingBits);
    }

    @Test
    public void test_group_disjoint() {
        NGNode bn1 = createRectangle(150, 0, 50, 50);
        NGNode bn2 = createRectangle(150, 60, 50, 50);
        NGGroup gbn = createGroup(bn1, bn2);
        translate(gbn, 10, 10);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 100, 100), new RectBounds(0, 110, 100, 210)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        Assert.assertEquals(0, gbn.cullingBits);
        for(NGNode n : gbn.getChildren()) {
            Assert.assertEquals(0, n.cullingBits);
        }
    }

    @Test
    public void test_group_intersect() {
        NGNode bn1 = createRectangle(50, 50, 30, 30);
        NGNode bn2 = createRectangle(50, 120, 30, 30);
        NGGroup gbn = createGroup(bn1, bn2);
        translate(gbn, 10, 10);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 100, 100), new RectBounds(0, 110, 100, 210)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());

        //check group
        Assert.assertEquals(1 | (1 << 2), gbn.cullingBits);
        
        //check children               
        Assert.assertEquals(2, bn1.cullingBits);
        Assert.assertEquals(2 << 2, bn2.cullingBits);
    }
    
    @Test
    public void test_group_within() {
        NGNode bn1 = createRectangle(50, 50, 30, 30);
        NGNode bn2 = createRectangle(50, 10, 30, 30);
        NGGroup gbn = createGroup(bn1, bn2);
        translate(gbn, 10, 10);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 100, 100), new RectBounds(0, 110, 100, 210)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());

        //check group
        Assert.assertEquals(2, gbn.cullingBits);
        
        //check children (as the group is "completely covered", the children should not have been processed)          
        Assert.assertEquals(0, bn1.cullingBits);
        Assert.assertEquals(0, bn2.cullingBits);
    }
    
    @Test
    public void test_region_within_group() {
        NGNode bn1 = createRectangle(50, 10, 100, 100);
        NGNode bn2 = createRectangle(50, 120, 100, 100);
        NGGroup gbn = createGroup(bn1, bn2);
        translate(gbn, 5, 5);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(70, 20, 100, 105), new RectBounds(70, 130, 100, 200)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());

        //check group
        Assert.assertEquals(1 | (1 << 2), gbn.cullingBits);

        //check children
        Assert.assertEquals(1, bn1.cullingBits);
        Assert.assertEquals(1 << 2, bn2.cullingBits);
    }

    @Test
    public void test_region_within_group_group() {
        NGNode bn1 = createRectangle(50, 10, 100, 100);
        NGNode bn2 = createRectangle(50, 120, 100, 100);
        NGNode g1bn = createGroup(bn1, bn2);
        translate(g1bn, 5, 5);

        NGNode bn3 = createRectangle(200, 200, 100, 100);
        NGNode gbn = createGroup(g1bn, bn3);
        translate(gbn, 5, 5);

        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(70, 25, 100, 105), new RectBounds(70, 130, 100, 200)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());

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
    public void test_rectangle_group_full_within_then_partial() {
        NGNode bn1 = createRectangle(50, 50, 100, 100);
        NGNode gbn = createGroup(bn1);

        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 160, 160), new RectBounds(80, 80, 100, 100)});
        gbn.markCullRegions(drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());

        Assert.assertEquals(2 | (1 << 2), gbn.cullingBits);
        Assert.assertEquals(1 << 2, bn1.cullingBits);
    }
}
