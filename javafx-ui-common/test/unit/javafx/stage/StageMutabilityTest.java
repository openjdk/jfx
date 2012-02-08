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

package javafx.stage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test those attributes on Stage that should be immutable after Stage is visible.
 *
 */
public class StageMutabilityTest {

// ========== Tests for style

    /**
     * Tests the default value of style for a Stage
     */
    @Test public void testStyleDefault() {
        Stage stage = new Stage();
        assertEquals(StageStyle.DECORATED, stage.getStyle());
    }

    /**
     * Tests setting the value of style for a Stage
     */
    @Test public void testStyleSet() {
        Stage stage = new Stage();
        assertEquals(StageStyle.DECORATED, stage.getStyle());
        stage.initStyle(StageStyle.UNDECORATED);
        assertEquals(StageStyle.UNDECORATED, stage.getStyle());
        stage.initStyle(StageStyle.DECORATED);
        assertEquals(StageStyle.DECORATED, stage.getStyle());
        stage.initStyle(StageStyle.TRANSPARENT);
        assertEquals(StageStyle.TRANSPARENT, stage.getStyle());
        stage.initStyle(StageStyle.UTILITY);
        assertEquals(StageStyle.UTILITY, stage.getStyle());
    }

    /**
     * Tests setting the value of style for a primary Stage
     */
    @Test public void testStyleSetPrimary() {
        Stage stage = new Stage();
        assertFalse(stage.isPrimary());
        stage.impl_setPrimary(true);
        assertTrue(stage.isPrimary());
        assertEquals(StageStyle.DECORATED, stage.getStyle());
        stage.initStyle(StageStyle.UNDECORATED);
        assertEquals(StageStyle.UNDECORATED, stage.getStyle());
        stage.initStyle(StageStyle.DECORATED);
        assertEquals(StageStyle.DECORATED, stage.getStyle());
        stage.initStyle(StageStyle.TRANSPARENT);
        assertEquals(StageStyle.TRANSPARENT, stage.getStyle());
        stage.initStyle(StageStyle.UTILITY);
        assertEquals(StageStyle.UTILITY, stage.getStyle());
    }

    /**
     * Tests initializing the value of style for a Stage in the constructor
     */
    @Test public void testStyleConstructor() {
        Stage stage = new Stage(StageStyle.UNDECORATED);
        assertEquals(StageStyle.UNDECORATED, stage.getStyle());
        stage.initStyle(StageStyle.DECORATED);
        assertEquals(StageStyle.DECORATED, stage.getStyle());
    }

    /**
     * Tests that setting the value of style on a visible Stage throws an exception
     */
    @Test public void testStyleSetWhileVisible() {
        Stage stage = new Stage();
        assertEquals(StageStyle.DECORATED, stage.getStyle());
        stage.show();
        try {
            stage.initStyle(StageStyle.UNDECORATED);
            // Error if we get here we didn't get the expected exception
            assertTrue(false);
        } catch (IllegalStateException ex) {
        }
        assertEquals(StageStyle.DECORATED, stage.getStyle());
    }

    /**
     * Tests that setting the value of style on a Stage after it is
     * set visible throws an exception even if it is subsequently set non-visible
     */
    @Test public void testStyleSetAfterVisible() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        assertEquals(StageStyle.TRANSPARENT, stage.getStyle());
        stage.initStyle(StageStyle.UNDECORATED);
        assertEquals(StageStyle.UNDECORATED, stage.getStyle());
        stage.show();
        stage.hide();
        try {
            stage.initStyle(StageStyle.DECORATED);
            // Error if we get here we didn't get the expected exception
            assertTrue(false);
        } catch (IllegalStateException ex) {
        }
        assertEquals(StageStyle.UNDECORATED, stage.getStyle());
    }

// ========== Tests for modality

    /**
     * Tests the default value of modality for a Stage
     */
    @Test public void testModalityDefault() {
        Stage stage = new Stage();
        assertEquals(Modality.NONE, stage.getModality());
    }

    /**
     * Tests setting the value of modality for a Stage
     */
    @Test public void testModalitySet() {
        Stage stage = new Stage();
        assertEquals(Modality.NONE, stage.getModality());
        stage.initModality(Modality.WINDOW_MODAL);
        assertEquals(Modality.WINDOW_MODAL, stage.getModality());
        stage.initModality(Modality.APPLICATION_MODAL);
        assertEquals(Modality.APPLICATION_MODAL, stage.getModality());
        stage.initModality(Modality.NONE);
        assertEquals(Modality.NONE, stage.getModality());
    }

    /**
     * Tests setting the value of modality for a primary Stage
     */
    @Test public void testModalitySetPrimary() {
        Stage stage = new Stage();
        assertFalse(stage.isPrimary());
        stage.impl_setPrimary(true);
        assertTrue(stage.isPrimary());
        assertEquals(Modality.NONE, stage.getModality());
        try {
            stage.initModality(Modality.WINDOW_MODAL);
            // Error if we get here we didn't get the expected exception
            assertTrue(false);
        } catch (IllegalStateException ex) {
        }
        assertEquals(Modality.NONE, stage.getModality());
    }

// TODO: Add more Modality test below:
//    /**
//     * Tests that setting the value of style on a visible Stage throws an exception
//     */
//    @Test public void testStyleSetWhileVisible() {
//        Stage stage = new Stage();
//        assertEquals(StageStyle.DECORATED, stage.getStyle());
//        stage.show();
//        try {
//            stage.initStyle(StageStyle.UNDECORATED);
//            // Error if we get here we didn't get the expected exception
//            assertTrue(false);
//        } catch (IllegalStateException ex) {
//        }
//        assertEquals(StageStyle.DECORATED, stage.getStyle());
//    }
//
//    /**
//     * Tests that setting the value of style on a Stage after it is
//     * set visible throws an exception even if it is subsequently set non-visible
//     */
//    @Test public void testStyleSetAfterVisible() {
//        Stage stage = new Stage(StageStyle.TRANSPARENT);
//        assertEquals(StageStyle.TRANSPARENT, stage.getStyle());
//        stage.initStyle(StageStyle.UNDECORATED);
//        assertEquals(StageStyle.UNDECORATED, stage.getStyle());
//        stage.show();
//        stage.hide();
//        try {
//            stage.initStyle(StageStyle.DECORATED);
//            // Error if we get here we didn't get the expected exception
//            assertTrue(false);
//        } catch (IllegalStateException ex) {
//        }
//        assertEquals(StageStyle.UNDECORATED, stage.getStyle());
//    }

}
