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

package com.sun.javafx.scene.layout.region;

import javafx.geometry.Insets;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the BackgroundFill class
 */
public class BackgroundFillTest {

    @Test public void valuesAreSetCorrectly() {
//        BackgroundFill obj = new BackgroundFill(Color.ORANGE, 1, 2, 3, 4, new Insets(20));
//        assertEquals(Color.ORANGE, obj.getFill());
//        assertEquals(1, obj.getTopLeftCornerRadius(), 0);
//        assertEquals(2, obj.getTopRightCornerRadius(), 0);
//        assertEquals(3, obj.getBottomRightCornerRadius(), 0);
//        assertEquals(4, obj.getBottomLeftCornerRadius(), 0);
//        assertEquals(new Insets(20), obj.getOffsets());
    }

    @Test public void hashingReturnsSameObject() {
//        HashMap<BackgroundFill, String> map = new HashMap<BackgroundFill, String>();
//        BackgroundFill obj = new BackgroundFill(Color.ORANGE, 1, 2, 3, 4, new Insets(20));
//        map.put(obj, "YES");
//        assertEquals("YES", map.get(obj));
//
//        BackgroundFill equivalent = new BackgroundFill(Color.ORANGE, 1, 2, 3, 4, new Insets(20));
//        assertEquals("YES", map.get(equivalent));
//
//        BackgroundFill different = new BackgroundFill(Color.ORANGE, 1, /*different!*/3, 3, 4, new Insets(20));
//        assertFalse(map.containsKey(different));
    }

    @Test public void equality() {
//        BackgroundFill obj = new BackgroundFill(Color.ORANGE, 1, 2, 3, 4, new Insets(20));
//        BackgroundFill equivalent = new BackgroundFill(Color.ORANGE, 1, 2, 3, 4, new Insets(20));
//        assertTrue(obj.equals(equivalent));
//
//        BackgroundFill different = new BackgroundFill(Color.ORANGE, 1, /*different!*/3, 3, 4, new Insets(20));
//        assertFalse(obj.equals(different));
    }
}
