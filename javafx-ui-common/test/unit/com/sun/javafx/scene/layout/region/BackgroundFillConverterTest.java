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

package com.sun.javafx.scene.layout.region;

import org.junit.Test;

/**
 * Tests for the BackgroundFillConverter class
 */
public class BackgroundFillConverterTest {

    @Test public void dummy() { }
    /*
        -fx-background-color:
        -fx-background-radius:
        -fx-background-insets:
     */
//    @Test public void scenario1() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(0, fills.size());
//    }
//
//    /*
//        -fx-background-color: blue
//        -fx-background-radius:
//        -fx-background-insets:
//    */
//    @Test public void scenario2() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, new Paint[] { Color.BLUE });
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(1, fills.size());
//
//        BackgroundFill fill = fills.get(0);
//        BackgroundFill expected = new BackgroundFill(Color.BLUE, 0, 0, 0, 0, Insets.EMPTY);
//        assertEquals(expected, fill);
//    }
//
//    /*
//        -fx-background-color: blue
//        -fx-background-radius: 10
//        -fx-background-insets:
//     */
//    @Test public void scenario3() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, new Paint[] { Color.BLUE });
//        map.put(BackgroundFill.BACKGROUND_RADIUS, new Insets[] { new Insets(10) });
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(1, fills.size());
//
//        BackgroundFill fill = fills.get(0);
//        BackgroundFill expected = new BackgroundFill(Color.BLUE, 10, 10, 10, 10, Insets.EMPTY);
//        assertEquals(expected, fill);
//    }
//
//    /*
//        -fx-background-color: blue
//        -fx-background-radius: 1 2 3 4
//        -fx-background-insets:
//     */
//    @Test public void scenario4() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, new Paint[] { Color.BLUE });
//        map.put(BackgroundFill.BACKGROUND_RADIUS, new Insets[] { new Insets(1.0, 2.0, 3.0, 4.0) });
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(1, fills.size());
//
//        BackgroundFill fill = fills.get(0);
//        BackgroundFill expected = new BackgroundFill(Color.BLUE, 1, 2, 3, 4, Insets.EMPTY);
//        assertEquals(expected, fill);
//    }
//
//    /*
//        -fx-background-color: blue
//        -fx-background-radius: 10
//        -fx-background-insets: 1
//     */
//    @Test public void scenario5() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, new Paint[] { Color.BLUE });
//        map.put(BackgroundFill.BACKGROUND_RADIUS, new Insets[] { new Insets(10) });
//        map.put(BackgroundFill.BACKGROUND_INSETS, new Insets[] { new Insets(1) });
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(1, fills.size());
//
//        BackgroundFill fill = fills.get(0);
//        BackgroundFill expected = new BackgroundFill(Color.BLUE, 10, 10, 10, 10, new Insets(1));
//        assertEquals(expected, fill);
//    }
//
//    /*
//        -fx-background-color: blue
//        -fx-background-radius: 10, 20
//        -fx-background-insets: 1
//     */
//    @Test public void scenario6() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, new Paint[] { Color.BLUE });
//        map.put(BackgroundFill.BACKGROUND_RADIUS, new Insets[] { new Insets(10), new Insets(20) });
//        map.put(BackgroundFill.BACKGROUND_INSETS, new Insets[] { new Insets(1) });
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(1, fills.size());
//
//        BackgroundFill fill = fills.get(0);
//        BackgroundFill expected = new BackgroundFill(Color.BLUE, 10, 10, 10, 10, new Insets(1));
//        assertEquals(expected, fill);
//    }
//
//    /*
//        -fx-background-color: blue
//        -fx-background-radius: 10
//        -fx-background-insets: 1, 2
//     */
//    @Test public void scenario7() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, new Paint[] { Color.BLUE });
//        map.put(BackgroundFill.BACKGROUND_RADIUS, new Insets[] { new Insets(10) });
//        map.put(BackgroundFill.BACKGROUND_INSETS, new Insets[] { new Insets(1), new Insets(2) });
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(1, fills.size());
//
//        BackgroundFill fill = fills.get(0);
//        BackgroundFill expected = new BackgroundFill(Color.BLUE, 10, 10, 10, 10, new Insets(1));
//        assertEquals(expected, fill);
//    }
//
//    /*
//        -fx-background-color: blue, green
//        -fx-background-radius: 10
//        -fx-background-insets: 1
//     */
//    @Test public void scenario8() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, new Paint[] { Color.BLUE, Color.GREEN });
//        map.put(BackgroundFill.BACKGROUND_RADIUS, new Insets[] { new Insets(10) });
//        map.put(BackgroundFill.BACKGROUND_INSETS, new Insets[] { new Insets(1) });
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(2, fills.size());
//
//        BackgroundFill fill = fills.get(0);
//        BackgroundFill expected = new BackgroundFill(Color.BLUE, 10, 10, 10, 10, new Insets(1));
//        assertEquals(expected, fill);
//
//        fill = fills.get(1);
//        expected = new BackgroundFill(Color.GREEN, 10, 10, 10, 10, new Insets(1));
//        assertEquals(expected, fill);
//    }
//
//    /*
//        -fx-background-color: blue, green
//        -fx-background-radius: 10, 20
//        -fx-background-insets: 1
//     */
//    @Test public void scenario9() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, new Paint[] { Color.BLUE, Color.GREEN });
//        map.put(BackgroundFill.BACKGROUND_RADIUS, new Insets[] { new Insets(10), new Insets(20) });
//        map.put(BackgroundFill.BACKGROUND_INSETS, new Insets[] { new Insets(1) });
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(2, fills.size());
//
//        BackgroundFill fill = fills.get(0);
//        BackgroundFill expected = new BackgroundFill(Color.BLUE, 10, 10, 10, 10, new Insets(1));
//        assertEquals(expected, fill);
//
//        fill = fills.get(1);
//        expected = new BackgroundFill(Color.GREEN, 20, 20, 20, 20, new Insets(1));
//        assertEquals(expected, fill);
//    }
//
//    /*
//        -fx-background-color: blue, green
//        -fx-background-radius: 10
//        -fx-background-insets: 1, 2
//     */
//    @Test public void scenario10() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, new Paint[] { Color.BLUE, Color.GREEN });
//        map.put(BackgroundFill.BACKGROUND_RADIUS, new Insets[] { new Insets(10) });
//        map.put(BackgroundFill.BACKGROUND_INSETS, new Insets[] { new Insets(1), new Insets(2) });
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(2, fills.size());
//
//        BackgroundFill fill = fills.get(0);
//        BackgroundFill expected = new BackgroundFill(Color.BLUE, 10, 10, 10, 10, new Insets(1));
//        assertEquals(expected, fill);
//
//        fill = fills.get(1);
//        expected = new BackgroundFill(Color.GREEN, 10, 10, 10, 10, new Insets(2));
//        assertEquals(expected, fill);
//    }
//
//    /*
//        -fx-background-color: null
//        -fx-background-radius: null
//        -fx-background-insets: null
//     */
//    @Test public void scenario11() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, null);
//        map.put(BackgroundFill.BACKGROUND_RADIUS, null);
//        map.put(BackgroundFill.BACKGROUND_INSETS, null);
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(0, fills.size());
//    }
//
//    /*
//        -fx-background-color: []
//        -fx-background-radius: []
//        -fx-background-insets: []
//     */
//    @Test public void scenario12() {
//        Map<CSSProperty, Object> map = new HashMap<CSSProperty, Object>();
//        map.put(BackgroundFill.BACKGROUND_COLOR, new Color[0]);
//        map.put(BackgroundFill.BACKGROUND_RADIUS, new Insets[0]);
//        map.put(BackgroundFill.BACKGROUND_INSETS, new Insets[0]);
//        List<BackgroundFill> fills = BackgroundFillConverter.getInstance().convert(map);
//        assertEquals(0, fills.size());
//    }

}
