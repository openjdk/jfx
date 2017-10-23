/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.print;

import com.sun.javafx.print.Units;
import static com.sun.javafx.print.Units.*;

/**
 * A class which encapsulates the size of paper media as used by printers.
 * <p>
 * The paper sizes which are enumerated for a printer is the authoritative
 * source of Paper sizes that may be selected for printing on that printer.
 * <p>
 * However for convenience, this class pre-defines some of the most
 * common paper sizes so that an application may easily set up default
 * parameters from code, eg by referring to <code>Paper.A4</code>
 *
 * @since JavaFX 8.0
 */

public final class Paper {

    private String name;
    private double width, height;
    private Units units;

    Paper(String paperName,
          double paperWidth, double paperHeight, Units units)

        throws IllegalArgumentException {

        if (paperWidth <= 0 || paperHeight <= 0) {
            throw new IllegalArgumentException("Illegal dimension");
        }
        if (paperName == null) {
            throw new IllegalArgumentException("Null name");

        }
        name = paperName;
        width = paperWidth;
        height = paperHeight;
        this.units = units;
    }

     /** Get the paper name.
     * This may not be directly useful for user display as it is not localized.
     * @return the paper name
     */
    public final String getName() {
        return name;
    }

    /**
     * Translate the internally stored dimension into points.
     */
    private double getSizeInPoints(double dim) {
        switch (units) {
        case POINT : return (int)(dim+0.5);
        case INCH  : return (int)((dim * 72) + 0.5);
        case MM    : return (int)(((dim * 72) / 25.4) + 0.5);
        }
        return dim;
    }

    /**
     * Get the width of the paper in points (1/72 inch)
     * @return the width of the paper in points (1/72 inch)
     */
    public final double getWidth() {
        return getSizeInPoints(width);
    }

    /**
     * Get the height of the paper in points (1/72 inch)
     * @return the height of the paper in points (1/72 inch)
     */
    public final double getHeight() {
        return getSizeInPoints(height);
    }

    @Override
    public final int hashCode() {
        return (int)width+((int)height<<16)+units.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        return (o != null &&
                o instanceof Paper &&
                this.name.equals(((Paper)o).name) &&
                this.width == (((Paper)o).width) &&
                this.height == (((Paper)o).height) &&
                this.units == (((Paper)o).units));
    }

    @Override
    public final String toString() {
        return "Paper: " + name+" size="+width+"x"+height+" " + units;
    }

   /**
     * Specifies the ISO A0 size, 841 mm by 1189 mm.
     */
    public static final Paper A0 = new Paper("A0", 841, 1189, MM);

    /**
     * Specifies the ISO A1 size, 594 mm by 841 mm.
     */
    public static final Paper A1 = new Paper("A1", 594, 841, MM);

    /**
     * Specifies the ISO A2 size, 420 mm by 594 mm.
     */

    public static final Paper A2 = new Paper("A2", 420, 594, MM);

    /**
     * Specifies the ISO A3 size, 297 mm by 420 mm.
     */
    public static final Paper A3 = new Paper("A3", 297, 420, MM);

    /**
     * Specifies the ISO A4 size, 210 mm by 297 mm.
     */
    public static final Paper A4 = new Paper("A4", 210, 297, MM);

    /**
     * Specifies the ISO A5 size, 148 mm by 210 mm.
     */
    public static final Paper A5 = new Paper("A5", 148, 210, MM);

    /**
     * Specifies the ISO A6 size, 105 mm by 148 mm.
     */
    public static final Paper A6 = new Paper("A6", 105, 148, MM);

    /**
     * Specifies the ISO Designated Long size, 110 mm by 220 mm.
     */
    public static final Paper
        DESIGNATED_LONG = new Paper("Designated Long", 110, 220, MM);

    /**
     *Specifies the North American letter size, 8.5 inches by 11 inches
     */
    public static final Paper NA_LETTER = new Paper("Letter", 8.5, 11, INCH);

    /**
     * Specifies the North American legal size, 8.5 inches by 14 inches.
     */
    public static final Paper LEGAL = new Paper("Legal", 8.4, 14, INCH);

    /**
     * Specifies the tabloid size, 11 inches by 17 inches.
     */
    public static final Paper TABLOID = new Paper("Tabloid", 11.0, 17.0, INCH);

    /**
     * Specifies the executive size, 7.25 inches by 10.5 inches.
     */
    public static final Paper
        EXECUTIVE = new Paper("Executive", 7.25, 10.5, INCH);

    /**
     * Specifies the North American 8 inch by 10 inch paper.
     */
    public static final Paper NA_8X10 = new Paper("8x10", 8, 10, INCH);

    /**
     * Specifies the Monarch envelope size, 3.87 inch by 7.5 inch.
     */
    public static final Paper
        MONARCH_ENVELOPE = new Paper("Monarch Envelope", 3.87, 7.5, INCH);
    /**
     * Specifies the North American Number 10 business envelope size,
     * 4.125 inches by 9.5 inches.
     */
    public static final Paper
        NA_NUMBER_10_ENVELOPE = new Paper("Number 10 Envelope",
                                          4.125, 9.5, INCH);
     /**
      * Specifies the engineering C size, 17 inch by 22 inch.
      */
    public static final Paper C = new Paper("C", 17.0, 22.0, INCH);

    /**
     * Specifies the JIS B4 size, 257 mm by 364 mm.
     */
    public static final Paper JIS_B4 = new Paper("B4", 257, 364, MM);

    /**
     * Specifies the JIS B5 size, 182 mm by 257 mm.
     */

    public static final Paper JIS_B5 = new Paper("B5", 182, 257, MM);

    /**
     * Specifies the JIS B6 size, 128 mm by 182 mm.
     */
    public static final Paper JIS_B6 = new Paper("B6", 128, 182, MM);

    /**
     * Specifies the Japanese postcard size, 100 mm by 148 mm.
     */
    public static final Paper
        JAPANESE_POSTCARD = new Paper("Japanese Postcard", 100, 148, MM);

}


