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

package javafx.scene.layout;

import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Simple tests for BackgroundFill
 */
public class BackgroundFillTest {
    @Test public void nullPaintDefaultsToTransparent() {
        BackgroundFill fill = new BackgroundFill(null, new CornerRadii(3), new Insets(4));
        assertEquals(Color.TRANSPARENT, fill.getFill());
    }

    @Test public void nullRadiusDefaultsToEmpty() {
        BackgroundFill fill = new BackgroundFill(Color.ORANGE, null, new Insets(2));
        assertEquals(CornerRadii.EMPTY, fill.getRadii());
    }

    @Test public void nullInsetsDefaultsToEmpty() {
        BackgroundFill fill = new BackgroundFill(Color.ORANGE, new CornerRadii(2), null);
        assertEquals(Insets.EMPTY, fill.getInsets());
    }

    @Test public void equivalentFills() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        assertEquals(a, b);
    }

    @Test public void differentFills() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        BackgroundFill b = new BackgroundFill(Color.RED, new CornerRadii(2), new Insets(3));
        assertFalse(a.equals(b));
    }

    @Test public void differentFills2() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(1), new Insets(3));
        assertFalse(a.equals(b));
    }

    @Test public void differentFills3() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(1));
        assertFalse(a.equals(b));
    }

    @Test public void equalsAgainstNull() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        assertFalse(a.equals(null));
    }

    @Test public void equalsAgainstRandomObject() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        assertFalse(a.equals("Some random object"));
    }

    @Test public void equivalentHaveSameHash() {
        BackgroundFill a = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        BackgroundFill b = new BackgroundFill(Color.ORANGE, new CornerRadii(2), new Insets(3));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void toStringCausesNoError() {
        BackgroundFill f = new BackgroundFill(null, null, null);
        f.toString();
    }
}
