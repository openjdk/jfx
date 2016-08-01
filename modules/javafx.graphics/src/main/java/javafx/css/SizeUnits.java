/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css;

import javafx.scene.text.Font;

/**
 * Represents a size specified in a particular unit, such as 14px or 0.2em.
 *
 * @since 9
 */
public enum SizeUnits {

    PERCENT(false) {

        @Override
        public String toString() { return "%"; }

        @Override
        public double points(double value, double multiplier, Font font_not_used) {
            return (value/100.0) * multiplier;
        }

        @Override
        public double pixels(double value, double multiplier, Font font_not_used) {
            return (value/100.0) * multiplier;
        }

    },
    IN(true) {

        @Override
        public String toString() { return "in"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            return value * POINTS_PER_INCH;
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            return value * DOTS_PER_INCH;
        }

    },
    CM(true) {

        @Override
        public String toString() { return "cm"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            return (value / CM_PER_INCH) * POINTS_PER_INCH;
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            return (value / CM_PER_INCH) * DOTS_PER_INCH;
        }

    },
    MM(true) {

        @Override
        public String toString() { return "mm"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            return (value / MM_PER_INCH) * POINTS_PER_INCH;
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            return (value / MM_PER_INCH) * DOTS_PER_INCH;
        }

    },
    EM(false) {

        @Override
        public String toString() { return "em"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font) {
            return round(value * pointSize(font));
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font) {
            return round(value * pixelSize(font));
        }

    },
    EX(false) {

        @Override
        public String toString() { return "ex"; }

        // In the absence of font metrics, one ex is taken to be half an em
        @Override
        public double points(double value, double multiplier_not_used, Font font) {
            return round(value / 2.0 * pointSize(font));
        }

        // In the absence of font metrics, one ex is taken to be half an em
        @Override
        public double pixels(double value, double multiplier_not_used, Font font) {
            return round(value / 2.0 * pixelSize(font));
        }

    },
    PT(true) {
        @Override
        public String toString() { return "pt"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            return value;
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            return value * (DOTS_PER_INCH / POINTS_PER_INCH);
        }

    },
    PC(true) {
        @Override
        public String toString() { return "pc"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            return value * POINTS_PER_PICA;
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            return (value * POINTS_PER_PICA) * (DOTS_PER_INCH / POINTS_PER_INCH);
        }

    },
    PX(true) {
        @Override
        public String toString() { return "px"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            return value * (POINTS_PER_INCH / DOTS_PER_INCH);
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            return value;
        }

    },

    DEG(true) {
        @Override
        public String toString() { return "deg"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            return round(value);
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            return round(value);
        }

    },

    GRAD(true) {

        @Override
        public String toString() { return "grad"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            // convert to degrees - 360deg = 400grad
            return round(value*9/10);
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            // convert to degrees - 360deg = 400grad
            return round(value*9/10);
        }

    },

    RAD(true) {

        @Override
        public String toString() { return "rad"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            // convert to degrees - 360deg = 2pi rad
            return round(value*180/Math.PI);
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            // convert to degrees - 360deg = 2pi rad
            return round(value*180/Math.PI);
        }

    },

    TURN(true) {

        @Override
        public String toString() { return "turn"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            // convert to degrees - 360deg = 1 turn
            return round(value*360);
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            return round(value*360);
        }

    },


    S(true) {

        @Override
        public String toString() { return "s"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            return value;
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            return value;
        }

    },

    MS(true) {

        @Override
        public String toString() { return "ms"; }

        @Override
        public double points(double value, double multiplier_not_used, Font font_not_used) {
            return value;
        }

        @Override
        public double pixels(double value, double multiplier_not_used, Font font_not_used) {
            return value;
        }

    };

    public abstract double points(double value, double multiplier, Font font);
    public abstract double pixels(double value, double multiplier, Font font);

    private SizeUnits(boolean absolute) {
        this.absolute = absolute;
    }

    private final boolean absolute;
    public boolean isAbsolute() {
        return absolute;
    }

    // RT-14711: The spec says 1px is equal to 0.75pt
    //           72 / 0.75 = 96
    static final private double DOTS_PER_INCH = 96.0;
    static final private double POINTS_PER_INCH = 72.0;
    static final private double CM_PER_INCH = 2.54;
    static final private double MM_PER_INCH = CM_PER_INCH * 10;
    static final private double POINTS_PER_PICA = 12.0;

    /* Get the font size in points */
    private static double pointSize(Font font) {
        return pixelSize(font) * (POINTS_PER_INCH/DOTS_PER_INCH);
    }

    /* Get the font size in pixels to points */
    private static double pixelSize(Font font) {
        return (font != null) ? font.getSize() : Font.getDefault().getSize();
    }

    /* round to nearest 10th */
    private static double round(double d) {

        if (d == 0) return d;

        final double r = (d < 0) ? -0.05 : 0.05;
        return ((long)((d + r) * 10)) / 10.0;
    }


}
