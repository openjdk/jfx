/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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
final public class Size {

    final private double value;
    final private SizeUnits units;
    public Size(double value, SizeUnits units) {
        this.value = value;
        this.units = (units != null) ? units : SizeUnits.PX;
    }

    /**
     * Return the value
     * @return the value
     */
    public double getValue() {
        return value;
    }

    /**
     * Return the units
     * @return the units
     */
    public SizeUnits getUnits() {
        return units;
    }

    /**
     * Return whether or not this Size is an absolute value or a relative value.
     * @return true if it is absolute, otherwise false
     */
    public boolean isAbsolute() {
        return units.isAbsolute();
    }

    /** Convert this size into Points units, a Point is 1/72 of a inch */
    double points(Font font) {
        return points(1.0, font);
    }

    /**
      * Convert this size into points
      *
      * @param multiplier   The multiplier for PERCENTAGE sizes
      * @param font         The font for EM sizes
      */
    double points(double multiplier, Font font) {
        return units.points(value, multiplier, font);
    }

    /**
      * Convert this size into pixels
      *
      * @param multiplier   The multiplier for PERCENTAGE sizes
      * @param font         The font for EM sizes
      * @return the size in pixels
      */
    public double pixels(double multiplier, Font font) {
        return units.pixels(value, multiplier, font);
    }

    /**
      * If size is not an absolute size, return the product of font size in pixels
      * and value. Otherwise, return the absolute value.
     * @param font the font
     * @return the size of pixels
      */
    public double pixels(Font font) {
        return pixels(1.0f, font);
    }

    /**
      * If size is not an absolute size, return the product of multiplier
      * and value. Otherwise, return the absolute value.
      */
    double pixels(double multiplier) {
        return pixels(multiplier, null);
    }

    /**
      * A convenience method for calling <code>pixels(1)</code>
      * @return the size in pixels
      */
    public double pixels() {
        return pixels(1.0f, null);
    }

    @Override public String toString() {
        return Double.toString(value) + units.toString();
    }

    @Override public int hashCode() {
        long bits = 17L;
        bits = 37L * bits + Double.doubleToLongBits(value);
        bits = 37L * bits + units.hashCode();
        return (int) (bits ^ (bits >> 32));
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final Size other = (Size)obj;

        if (units != other.units) {
            return false;
        }

        if (value == other.value) {
            return true;
        }

        if ((value > 0) ? other.value > 0 : other.value < 0) {
            //
            // double == double is not reliable since a double is kind of
            // a fuzzy value. And Double.compare is too precise.
            // For javafx, most sizes are rounded to the nearest tenth
            // (see SizeUnits.round) so comparing  here to the nearest
            // millionth is more than adequate. In the case of rads and
            // percents, this is also be more than adequate.
            //
            // Math.abs is too slow!
            final double v0 = value > 0 ? value : -value;
            final double v1 = other.value > 0 ? other.value : -other.value;
            final double diff = value  - other.value;
            if (diff < -0.000001 || 0.000001 < diff) {
                return false;
            }

            return true;
        }
        return false;
    }

}

