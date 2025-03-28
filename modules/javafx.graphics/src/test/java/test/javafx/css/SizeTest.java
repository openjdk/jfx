/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.css;

import javafx.css.Size;
import javafx.css.SizeShim;
import javafx.css.SizeUnits;
import javafx.scene.text.Font;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class SizeTest {

    public SizeTest() {
    }

    static final private double DOTS_PER_INCH = 96.0;
    static final private double POINTS_PER_INCH = 72.0;

    /**
     * Test of points method, of class Size.
     */
    @Test
    public void testPoints() {
        final Font font = Font.font("Amble", 16);
        final double pixelSize = font.getSize();
        final double pointSize = pixelSize * (POINTS_PER_INCH / DOTS_PER_INCH);

        Size instance = new Size(12.0, SizeUnits.PX);
        double expResult = 12.0  * (POINTS_PER_INCH/DOTS_PER_INCH);
        double result = SizeShim.points(instance, font);
        assertEquals(expResult, result, 0.01, "px");

        instance = new Size(12.0, SizeUnits.PT);
        expResult = 12.0; // PT is absolute
        result = SizeShim.points(instance, font);
        assertEquals(expResult, result, 0.01, "pt");

        instance = new Size(50.0, SizeUnits.PERCENT);
        expResult = 0.5 * pointSize;
        result = SizeShim.points(instance, pointSize, font);
        assertEquals(expResult, result, 0.01, "%");

        instance = new Size(2, SizeUnits.EM);
        expResult = 2 * pointSize;
        result = SizeShim.points(instance, font);
        assertEquals(expResult, result, 0.01, "em");

        instance = new Size(1.0, SizeUnits.EX);
        expResult = 0.5 * pointSize;
        result = SizeShim.points(instance, font);
        assertEquals(expResult, result, 0.01, "ex");


        instance = new Size(1.0, SizeUnits.CM);
        expResult = POINTS_PER_INCH/2.54; // CM is absolute (pts per inch/cm per inch)
        result = SizeShim.points(instance, font);
        assertEquals(expResult, result, 0.01, "cm");

        instance = new Size(1.0, SizeUnits.MM);
        expResult = POINTS_PER_INCH/25.4; // MM is absolute (pts per inch/mm per inch)
        result = SizeShim.points(instance, font);
        assertEquals(expResult, result, 0.01, "mm");

        instance = new Size(1.0, SizeUnits.IN);
        expResult = POINTS_PER_INCH; // IN is absolute (pts per inch)
        result = SizeShim.points(instance, font);
        assertEquals(expResult, result, 0.01, "in");

        instance = new Size(1.0, SizeUnits.PC);
        expResult = 12.0; // PC is absolute (pts per pica)
        result = SizeShim.points(instance, font);
        assertEquals(expResult, result, 0.01, "pc");

    }

    /**
     * Test of pixels method, of class Size.
     */
    @Test
    public void testPixels() {
        final Font font = Font.font("Amble", 16);
        final double pixelSize = font.getSize();
        final double pointSize = pixelSize * (POINTS_PER_INCH / DOTS_PER_INCH);

        Size instance = new Size(12.0, SizeUnits.PX);
        double expResult = 12.0;
        double result = instance.pixels(font);
        assertEquals(expResult, result, 0.01, "px");

        instance = new Size(12.0, SizeUnits.PT);
        expResult = 12.0 * (DOTS_PER_INCH / POINTS_PER_INCH);
        result = instance.pixels(font);
        assertEquals(expResult, result, 0.01, "pt");

        instance = new Size(50.0, SizeUnits.PERCENT);
        expResult = .5 * pixelSize;
        result = instance.pixels(pixelSize, font);
        assertEquals(expResult, result, 0.01, "%");

        instance = new Size(2, SizeUnits.EM);
        expResult = 2 * pixelSize;
        result = instance.pixels(font);
        assertEquals(expResult, result, 0.01, "em");

        instance = new Size(1.0, SizeUnits.EX);
        expResult = .5 * pixelSize;
        result = instance.pixels(font);
        assertEquals(expResult, result, 0.01, "ex");

        instance = new Size(1.0, SizeUnits.CM);
        // 1 cm / cm per inch
        expResult = (1/2.54f) * DOTS_PER_INCH;
        result = instance.pixels(font);
        assertEquals(expResult, result, 0.01, "cm");

        instance = new Size(1.0, SizeUnits.MM);
        // 1mm / mm per inch
        expResult = (1/25.4f) * DOTS_PER_INCH;
        result = instance.pixels(font);
        assertEquals(expResult, result, 0.01f, "mm");

        instance = new Size(1.0, SizeUnits.IN);
        expResult = DOTS_PER_INCH;
        result = instance.pixels(font);
        assertEquals(expResult, result, 0.01f, "in");

        instance = new Size(1.0, SizeUnits.PC);
        // 1pc * 12 pt per pc yields points, then convert points to pixels
        expResult = (1*12.0) * (DOTS_PER_INCH / POINTS_PER_INCH);
        result = instance.pixels(1.0, font);
        assertEquals(expResult, result, 0.01f, "pc");

    }


    /**
     * Test of sizes with angle units.
     */
    @Test
    public void testAngles() {

        // 360 degrees = 2pi radians
        // 90 degrees as radians
        double expResult = 90;

        Size instance = new Size(0.5*Math.PI, SizeUnits.RAD);
        double result = instance.pixels();
        assertEquals(expResult, result, 0.01, "1/2pi rad to deg");

        instance = new Size(100, SizeUnits.GRAD);
        result = instance.pixels();
        assertEquals(expResult, result, 0.01, "100grad to deg");

        instance = new Size(.25, SizeUnits.TURN);
        result = instance.pixels();
        assertEquals(expResult, result, 0.01, ".25turn to deg");
    }

    /**
     * Test of sizes with time units.
     */
    @Test
    public void testTime() {

        double expResult = 90;

        Size instance = new Size(90, SizeUnits.S);
        double result = instance.pixels();
        assertEquals(expResult, result, 0.01, "90s");

        instance = new Size(90, SizeUnits.MS);
        result = instance.pixels();
        assertEquals(expResult, result, 0.01, "90ms");

    }


    /**
     * Test of equals method, of class Size.
     */
    @Test
    public void testEquals() {
        Object o = new Size(2.0, SizeUnits.PX);
        Size instance = new Size(1.0, SizeUnits.PX);
        boolean expResult = false;
        boolean result = instance.equals(o);
        assertEquals(expResult, result);

        o = new Size(2.0, SizeUnits.PX);
        instance = new Size(2.0, SizeUnits.PX);
        expResult = true;
        result = instance.equals(o);
        assertEquals(expResult, result);

        o = new Size(2.0, SizeUnits.PT);
        instance = new Size(2.0, SizeUnits.EM);
        expResult = false;
        result = instance.equals(o);
        assertEquals(expResult, result);
    }

    /**
     * Test of getValue method, of class Size.
     */
    @Test
    public void testGetValue() {
        Size instance = new Size(0.0, SizeUnits.PX);
        double expResult = 0.0;
        double result = instance.getValue();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getUnits method, of class Size.
     */
    @Test
    public void testGetUnits() {
        Size instance = new Size(0.0, SizeUnits.PX);
        SizeUnits expResult = SizeUnits.PX;
        SizeUnits result = instance.getUnits();
        assertEquals(expResult, result);
    }

    /**
     * Test of isAbsolute method, of class Size.
     */
    @Test
    public void testIsAbsolute() {
        Size instance = new Size(0.0, SizeUnits.EM);
        boolean expResult = false;
        boolean result = instance.isAbsolute();
        assertEquals(expResult, result);

        instance = new Size(0.0, SizeUnits.PX);
        expResult = true;
        result = instance.isAbsolute();
        assertEquals(expResult, result);

    }

}
