/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.CacheHint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        CacheFilter cf = new CacheFilter(r, CacheHint.DEFAULT);
        assertFalse(cf.isRotateHint());
        assertFalse(cf.isScaleHint());
    }

    @Test public void settingCacheHintToDefault() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.SPEED);
        cf.setHint(CacheHint.DEFAULT);
        assertFalse(cf.isRotateHint());
        assertFalse(cf.isScaleHint());
    }

    @Test public void settingCacheHintToSpeedInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.SPEED);
        assertTrue(cf.isRotateHint());
        assertTrue(cf.isScaleHint());
    }

    @Test public void settingCacheHintToSpeed() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.DEFAULT);
        cf.setHint(CacheHint.SPEED);
        assertTrue(cf.isRotateHint());
        assertTrue(cf.isScaleHint());
    }

    @Test public void settingCacheHintToQualityInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.QUALITY);
        assertFalse(cf.isRotateHint());
        assertFalse(cf.isScaleHint());
    }

    @Test public void settingCacheHintToQuality() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.SPEED);
        cf.setHint(CacheHint.QUALITY);
        assertFalse(cf.isRotateHint());
        assertFalse(cf.isScaleHint());
    }

    @Test public void settingCacheHintToRotateInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.ROTATE);
        assertTrue(cf.isRotateHint());
        assertFalse(cf.isScaleHint());
    }

    @Test public void settingCacheHintToRotate() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.DEFAULT);
        cf.setHint(CacheHint.ROTATE);
        assertTrue(cf.isRotateHint());
        assertFalse(cf.isScaleHint());
    }

    @Test public void settingCacheHintToScaleInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.SCALE);
        assertFalse(cf.isRotateHint());
        assertTrue(cf.isScaleHint());
    }

    @Test public void settingCacheHintToScale() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.DEFAULT);
        cf.setHint(CacheHint.SCALE);
        assertFalse(cf.isRotateHint());
        assertTrue(cf.isScaleHint());
    }

    @Test public void settingCacheHintToScaleAndRotateInConstructor() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.SCALE_AND_ROTATE);
        assertTrue(cf.isRotateHint());
        assertTrue(cf.isScaleHint());
    }

    @Test public void settingCacheHintToScaleAndRotate() {
        NGRectangle r = new NGRectangle();
        CacheFilter cf = new CacheFilter(r, CacheHint.DEFAULT);
        cf.setHint(CacheHint.SCALE_AND_ROTATE);
        assertTrue(cf.isRotateHint());
        assertTrue(cf.isScaleHint());
    }
}
