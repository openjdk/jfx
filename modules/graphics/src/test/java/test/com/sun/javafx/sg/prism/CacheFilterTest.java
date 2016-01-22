/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.transform.Translate2D;
import com.sun.javafx.sg.prism.CacheFilter;
import com.sun.javafx.sg.prism.CacheFilterShim;
import com.sun.javafx.sg.prism.NGNodeShim;
import com.sun.javafx.sg.prism.NGRectangle;
import javafx.scene.CacheHint;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class CacheFilterTest {

    /******************************************************************
     *                                                                *
     *  Tests to make sure that the different cache hints get         *
     *  turned into the right values for rotateHint and scaleHint.    *
     *                                                                *
     *****************************************************************/

    @Test public void settingCacheHintToDefaultInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.DEFAULT);
        assertFalse(CacheFilterShim.isRotateHint(cf));
        assertFalse(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToDefault() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.SPEED);
        cf.setHint(CacheHint.DEFAULT);
        assertFalse(CacheFilterShim.isRotateHint(cf));
        assertFalse(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToSpeedInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.SPEED);
        assertTrue(CacheFilterShim.isRotateHint(cf));
        assertTrue(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToSpeed() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.DEFAULT);
        cf.setHint(CacheHint.SPEED);
        assertTrue(CacheFilterShim.isRotateHint(cf));
        assertTrue(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToQualityInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.QUALITY);
        assertFalse(CacheFilterShim.isRotateHint(cf));
        assertFalse(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToQuality() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.SPEED);
        cf.setHint(CacheHint.QUALITY);
        assertFalse(CacheFilterShim.isRotateHint(cf));
        assertFalse(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToRotateInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.ROTATE);
        assertTrue(CacheFilterShim.isRotateHint(cf));
        assertFalse(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToRotate() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.DEFAULT);
        cf.setHint(CacheHint.ROTATE);
        assertTrue(CacheFilterShim.isRotateHint(cf));
        assertFalse(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToScaleInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.SCALE);
        assertFalse(CacheFilterShim.isRotateHint(cf));
        assertTrue(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToScale() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.DEFAULT);
        cf.setHint(CacheHint.SCALE);
        assertFalse(CacheFilterShim.isRotateHint(cf));
        assertTrue(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToScaleAndRotateInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.SCALE_AND_ROTATE);
        assertTrue(CacheFilterShim.isRotateHint(cf));
        assertTrue(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void settingCacheHintToScaleAndRotate() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.DEFAULT);
        cf.setHint(CacheHint.SCALE_AND_ROTATE);
        assertTrue(CacheFilterShim.isRotateHint(cf));
        assertTrue(CacheFilterShim.isScaleHint(cf));
    }

    @Test public void cacheFilterReturnsCorrectDirtyBounds() {

        NGRectangle r = new NGRectangle();
        r.updateRectangle(0.3f, 0.9f, 100.3f, 119.9f, 0, 0);
        r.setTransformMatrix(BaseTransform.IDENTITY_TRANSFORM);
        r.setTransformedBounds(new RectBounds(0.3f, 0.9f, 100.6f, 120.8f), false);
        CacheFilter cf = CacheFilterShim.getCacheFilter(r, CacheHint.DEFAULT);
        RectBounds result = new RectBounds();
        CacheFilterShim.computeDirtyBounds(cf, result, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertEquals(new RectBounds(0, 0, 101, 121), result);

        NGNodeShim.clearDirty(r);

        final Translate2D translation = new Translate2D(10, 10);
        r.setTransformMatrix(translation);
        r.setTransformedBounds(new RectBounds(10.3f, 10, 110.6f, 130.8f), false);
        CacheFilterShim.computeDirtyBounds(cf, result, BaseTransform.IDENTITY_TRANSFORM, new GeneralTransform3D());
        assertEquals(new RectBounds(0, 0, 111, 131), result);
    }
}
