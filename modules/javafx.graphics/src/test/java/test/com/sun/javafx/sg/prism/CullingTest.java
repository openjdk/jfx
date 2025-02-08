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

package test.com.sun.javafx.sg.prism;

import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGNodeShim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        NGNodeShim.markCullRegions(bn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertEquals(1 |(1 << 2), NGNodeShim.cullingBits(bn));
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
        NGNodeShim.markCullRegions(bn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertEquals(0, NGNodeShim.cullingBits(bn));
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
        NGNodeShim.markCullRegions(bn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertEquals(2 | (2 << 2), NGNodeShim.cullingBits(bn));
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
        NGNodeShim.markCullRegions(bn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertEquals(1 | (1 << 2), NGNodeShim.cullingBits(bn));
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
        NGNodeShim.markCullRegions(bn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertEquals(2, NGNodeShim.cullingBits(bn));
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
        NGNodeShim.markCullRegions(bn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertEquals(2, NGNodeShim.cullingBits(bn));
    }

    @Test
    public void test_setCullBits_empty_regions() {
        NGNode bn = createRectangle(50, 50, 100, 100);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{});
        NGNodeShim.markCullRegions(bn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertEquals(0, NGNodeShim.cullingBits(bn));
    }

    @Test
    public void test_group_disjoint() {
        NGNode bn1 = createRectangle(150, 0, 50, 50);
        NGNode bn2 = createRectangle(150, 60, 50, 50);
        NGGroup gbn = createGroup(bn1, bn2);
        translate(gbn, 10, 10);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 100, 100), new RectBounds(0, 110, 100, 210)});
        NGNodeShim.markCullRegions(gbn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertEquals(0, NGNodeShim.cullingBits(gbn));
        for(NGNode n : gbn.getChildren()) {
            assertEquals(0, NGNodeShim.cullingBits(n));
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
        NGNodeShim.markCullRegions(gbn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());

        //check group
        assertEquals(1 | (1 << 2), NGNodeShim.cullingBits(gbn));

        //check children
        assertEquals(2, NGNodeShim.cullingBits(bn1));
        assertEquals(2 << 2, NGNodeShim.cullingBits(bn2));
    }

    @Test
    public void test_group_within() {
        NGNode bn1 = createRectangle(50, 50, 30, 30);
        NGNode bn2 = createRectangle(50, 10, 30, 30);
        NGGroup gbn = createGroup(bn1, bn2);
        translate(gbn, 10, 10);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 100, 100), new RectBounds(0, 110, 100, 210)});
        NGNodeShim.markCullRegions(gbn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());

        //check group
        assertEquals(2, NGNodeShim.cullingBits(gbn));

        //check children (as the group is "completely covered", the children should not have been processed)
        assertEquals(0, NGNodeShim.cullingBits(bn1));
        assertEquals(0, NGNodeShim.cullingBits(bn2));
    }

    @Test
    public void test_region_within_group() {
        NGNode bn1 = createRectangle(50, 10, 100, 100);
        NGNode bn2 = createRectangle(50, 120, 100, 100);
        NGGroup gbn = createGroup(bn1, bn2);
        translate(gbn, 5, 5);
        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(70, 20, 100, 105), new RectBounds(70, 130, 100, 200)});
        NGNodeShim.markCullRegions(gbn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());

        //check group
        assertEquals(1 | (1 << 2), NGNodeShim.cullingBits(gbn));

        //check children
        assertEquals(1, NGNodeShim.cullingBits(bn1));
        assertEquals(1 << 2, NGNodeShim.cullingBits(bn2));
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
        NGNodeShim.markCullRegions(gbn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());

        //check group
        assertEquals(1 | (1 << 2), NGNodeShim.cullingBits(gbn));

        //check group1
        assertEquals(1 | (1 << 2), NGNodeShim.cullingBits(g1bn));

        //check children
        assertEquals(1, NGNodeShim.cullingBits(bn1));
        assertEquals(1 << 2, NGNodeShim.cullingBits(bn2));
        assertEquals(0, NGNodeShim.cullingBits(bn3));
    }

    @Test
    public void test_rectangle_group_full_within_then_partial() {
        NGNode bn1 = createRectangle(50, 50, 100, 100);
        NGNode gbn = createGroup(bn1);

        DirtyRegionContainer drc = new DirtyRegionContainer(2);
        drc.deriveWithNewRegions(new RectBounds[]{new RectBounds(0, 0, 160, 160), new RectBounds(80, 80, 100, 100)});
        NGNodeShim.markCullRegions(gbn,drc, -1, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());

        assertEquals(2 | (1 << 2), NGNodeShim.cullingBits(gbn));
        assertEquals(1 << 2, NGNodeShim.cullingBits(bn1));
    }
}
