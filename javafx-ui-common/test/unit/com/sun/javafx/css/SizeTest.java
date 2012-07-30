/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistribution of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistribution in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.

 * This software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND
 * ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A
 * RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 * IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT
 * OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed, licensed or intended for
 * use in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package com.sun.javafx.css;

import static org.junit.Assert.assertEquals;
import javafx.scene.text.Font;

import org.junit.Test;


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
        double result = instance.points(font);
        assertEquals("px", expResult, result, 0.01);

        instance = new Size(12.0, SizeUnits.PT);
        expResult = 12.0; // PT is absolute
        result = instance.points(font);
        assertEquals("pt", expResult, result, 0.01);

        instance = new Size(50.0, SizeUnits.PERCENT);
        expResult = 0.5 * pointSize;
        result = instance.points(pointSize, font);
        assertEquals("%", expResult, result, 0.01);

        instance = new Size(2, SizeUnits.EM);
        expResult = 2 * pointSize;
        result = instance.points(font);
        assertEquals("em", expResult, result, 0.01);

        instance = new Size(1.0, SizeUnits.EX);
        expResult = 0.5 * pointSize;
        result = instance.points(font);
        assertEquals("ex", expResult, result, 0.01);


        instance = new Size(1.0, SizeUnits.CM);
        expResult = POINTS_PER_INCH/2.54; // CM is absolute (pts per inch/cm per inch)
        result = instance.points(font);
        assertEquals("cm", expResult, result, 0.01);

        instance = new Size(1.0, SizeUnits.MM);
        expResult = POINTS_PER_INCH/25.4; // MM is absolute (pts per inch/mm per inch)
        result = instance.points(font);
        assertEquals("mm", expResult, result, 0.01);

        instance = new Size(1.0, SizeUnits.IN);
        expResult = POINTS_PER_INCH; // IN is absolute (pts per inch)
        result = instance.points(font);
        assertEquals("in", expResult, result, 0.01);

        instance = new Size(1.0, SizeUnits.PC);
        expResult = 12.0; // PC is absolute (pts per pica)
        result = instance.points(font);
        assertEquals("pc", expResult, result, 0.01);

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
        assertEquals("px", expResult, result, 0.01);

        instance = new Size(12.0, SizeUnits.PT);
        expResult = 12.0 * (DOTS_PER_INCH / POINTS_PER_INCH);
        result = instance.pixels(font);
        assertEquals("pt", expResult, result, 0.01);

        instance = new Size(50.0, SizeUnits.PERCENT);
        expResult = .5 * pixelSize;
        result = instance.pixels(pixelSize, font);
        assertEquals("%", expResult, result, 0.01);

        instance = new Size(2, SizeUnits.EM);
        expResult = 2 * pixelSize;
        result = instance.pixels(font);
        assertEquals("em", expResult, result, 0.01);

        instance = new Size(1.0, SizeUnits.EX);
        expResult = .5 * pixelSize;
        result = instance.pixels(font);
        assertEquals("ex", expResult, result, 0.01);

        instance = new Size(1.0, SizeUnits.CM);
        // 1 cm / cm per inch
        expResult = (1/2.54f) * DOTS_PER_INCH;
        result = instance.pixels(font);
        assertEquals("cm", expResult, result, 0.01);

        instance = new Size(1.0, SizeUnits.MM);
        // 1mm / mm per inch
        expResult = (1/25.4f) * DOTS_PER_INCH;
        result = instance.pixels(font);
        assertEquals("mm", expResult, result, 0.01f);

        instance = new Size(1.0, SizeUnits.IN);
        expResult = DOTS_PER_INCH;
        result = instance.pixels(font);
        assertEquals("in", expResult, result, 0.01f);

        instance = new Size(1.0, SizeUnits.PC);
        // 1pc * 12 pt per pc yields points, then convert points to pixels
        expResult = (1*12.0) * (DOTS_PER_INCH / POINTS_PER_INCH);
        result = instance.pixels(1.0, font);
        assertEquals("pc", expResult, result, 0.01f);

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
        assertEquals("1/2pi rad to deg", expResult, result, 0.01);

        instance = new Size(100, SizeUnits.GRAD);
        result = instance.pixels();
        assertEquals("100grad to deg", expResult, result, 0.01);

        instance = new Size(.25, SizeUnits.TURN);
        result = instance.pixels();
        assertEquals(".25turn to deg", expResult, result, 0.01);
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
