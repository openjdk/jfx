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

package com.sun.javafx.sg;

import junit.framework.Assert;
import org.junit.Test;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.*;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;

public class EffectDirtyRegionTest {

    DirtyRegionContainer drc;
    DirtyRegionContainer drcExpected;
    DirtyRegionPool drp;
    Effect effect;
    BaseNode g1bn;
    BaseNode gbn;

    @Test
    public void dropShadowTest() {
        setupTest();

        effect = new DropShadow();

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(40, 40, 70, 70)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(30, 30, 90, 90)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void colorAdjustTest() {
        setupTest();

        effect = new ColorAdjust();

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60),
                new RectBounds(70, 70, 80, 80)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void bloomTest() {
        setupTest();

        effect = new Bloom();

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 80, 80)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void blendTest() {
        setupTest();

        effect = new Blend(Blend.Mode.ADD, new DropShadow(), new ColorAdjust());

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(40, 40, 70, 70)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(30, 30, 90, 90)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void boxBlurTest() {
        setupTest();

        effect = new BoxBlur(10, 5, 3);

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(36, 44, 74, 66)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(22, 38, 88, 72),
                new RectBounds(56, 64, 94, 86)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void gausianBlurTest() {
        setupTest();

        effect = new GaussianBlur(5);

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(45, 45, 65, 65)
            });
        compareResult(drcExpected, drc);

        ((GaussianBlur)effect).setRadius(2);
        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(43, 43, 67, 67),
                new RectBounds(68, 68, 82, 82)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void glowTest() {
        setupTest();

        effect = new Glow();

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 80, 80)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void innerShadowTest() {
        setupTest();

        effect = new InnerShadow();

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(40, 40, 70, 70)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(30, 30, 90, 90)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void motionBlurTest() {
        setupTest();

        effect = new MotionBlur();

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(40, 50, 70, 60)
            });
        compareResult(drcExpected, drc);

        ((MotionBlur)effect).setAngle((float) Math.toRadians(90));
        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(39, 40, 71, 70),
                new RectBounds(69, 60, 81, 90)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void sepiaToneTest() {
        setupTest();

        effect = new SepiaTone();

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60),
                new RectBounds(70, 70, 80, 80)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void boxShadowTest() {
        setupTest();

        effect = new BoxShadow();
        ((BoxShadow) effect).setHorizontalSize(10);

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(45, 50, 65, 60)
            });
        compareResult(drcExpected, drc);

        ((BoxShadow) effect).setHorizontalSize(0);
        ((BoxShadow) effect).setVerticalSize(10);
        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(45, 45, 65, 65),
                new RectBounds(70, 65, 80, 85)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void generalShadowTest() {
        setupTest();

        effect = new GeneralShadow();

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(40, 40, 70, 70)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(30, 30, 90, 90)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void cropTest() {
        setupTest();

        effect = new Crop(new DropShadow());

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 80, 80)
            });
        compareResult(drcExpected, drc);
    }

    @Test
    public void offsetTest() {
        setupTest();

        effect = new Offset(10, -10, new DropShadow());

        drc.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 50, 60, 60)
            });
        g1bn.setEffect(effect);
        g1bn.applyEffect(g1bn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 30, 80, 60)
            });
        compareResult(drcExpected, drc);

        drc.addDirtyRegion(new RectBounds(70, 70, 80, 80));
        gbn.setEffect(effect);
        gbn.applyEffect(gbn.getEffectFilter(), drc, drp);
        drcExpected.deriveWithNewRegions(new RectBounds[]
            {
                new RectBounds(50, 10, 100, 80)
            });
        compareResult(drcExpected, drc);
    }

    private void setupTest() {
        drc = new DirtyRegionContainer(6);
        drcExpected = new DirtyRegionContainer(6);
        drp = new DirtyRegionPool(4);

        Node g1 = group(new Rectangle(50, 50, 10, 10));
        g1bn = getBaseNode(g1);
        Node g = group(g1, new Rectangle(70, 70, 10, 10));
        gbn = getBaseNode(g);
    }

    private void compareResult(DirtyRegionContainer expected, DirtyRegionContainer computed) {
        for (int i = 0; i < computed.size(); i++) {
            Assert.assertEquals(expected.getDirtyRegion(i), computed.getDirtyRegion(i));
        }
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
