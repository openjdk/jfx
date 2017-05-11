/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.paint;

import java.util.List;

import com.sun.javafx.scene.paint.GradientUtils;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.NamedArg;

/**
 * The {@code RadialGradient} class provides a way to fill a shape
 * with a circular radial color gradient pattern.
 * The user may specify 2 or more gradient colors,
 * and this paint will provide an interpolation between each color.
 * <p>
 * The user must specify the circle controlling the gradient pattern,
 * which is defined by a center point and a radius.
 * The user can also specify a separate focus point within that circle,
 * which controls the location of the first color of the gradient.
 * By default the focus is set to be the center of the circle.
 * <p>
 * The center and radius are specified
 * relative to a unit square, unless the <code>proportional</code>
 * variable is false.  By default proportional is true, and the
 * gradient will be scaled to fill whatever shape it is applied to.
 * The focus point is always specified relative to the center point
 * by an angle and a distance relative to the radius.
 * <p>
 * This paint will map the first color of the gradient to the focus point,
 * and the last color to the perimeter of the circle,
 * interpolating smoothly for any in-between colors specified by the user.
 * Any line drawn from the focus point to the circumference will
 * thus span all of the gradient colors.
 * <p>
 * The focus distance will be clamped to the range {@code (-1, 1)}
 * so that the focus point is always strictly inside the circle.
 * <p>
 * The application provides an array of {@code Stop}s specifying how to distribute
 * the colors along the gradient. The {@code Stop#offset} variable must be
 * the range 0.0 to 1.0 and act like keyframes along the gradient.
 * They mark where the gradient should be exactly a particular color.
 * @since JavaFX 2.0
 */
public final class RadialGradient extends Paint {
    private double focusAngle;

    /**
     * Defines the angle in degrees from the center of the gradient
     * to the focus point to which the first color is mapped.
     * @return the angle in degrees from the center of the gradient
     * to the focus point to which the first color is mapped
     */
    public final double getFocusAngle() {
        return focusAngle;
    }

    private double focusDistance;

    /**
     * Defines the distance from the center of the gradient to the
     * focus point to which the first color is mapped.
     * A distance of 0.0 will be at the center of the gradient circle.
     * A distance of 1.0 will be on the circumference of the gradient circle.
     * @return the distance from the center of the gradient to the
     * focus point to which the first color is mapped
     */
    public final double getFocusDistance() {
        return focusDistance;
    }

    private double centerX;

    /**
     * Defines the X coordinate of the center point of the circle defining the gradient.
     * If proportional is true (the default), this value specifies a
     * point on a unit square that will be scaled to match the size of the
     * the shape that the gradient fills.
     * The last color of the gradient is mapped to the perimeter of this circle.
     *
     * @return the X coordinate of the center point of the circle defining the
     * gradient
     * @defaultValue 0.0
     */
    public final double getCenterX() {
        return centerX;
    }

    private double centerY;

    /**
     * Defines the X coordinate of the center point of the circle defining the gradient.
     * If proportional is true (the default), this value specifies a
     * point on a unit square that will be scaled to match the size of the
     * the shape that the gradient fills.
     * The last color of the gradient is mapped to the perimeter of this circle.
     *
     * @return the X coordinate of the center point of the circle defining the
     * gradient
     * @defaultValue 0.0
     */
    public final double getCenterY() {
        return centerY;
    }

    private double radius;

    /**
     * Specifies the radius of the circle defining the extents of the color gradient.
     * If proportional is true (the default), this value specifies a
     * size relative to  unit square that will be scaled to match the size of the
     * the shape that the gradient fills.
     *
     * @return the radius of the circle defining the extents of the color
     * gradient
     * @defaultValue 1.0
     */
    public final double getRadius() {
        return radius;
    }

    private boolean proportional;

    /**
     * Indicates whether the center and radius values are proportional or
     * absolute.
     * If this flag is true, the center point and radius are defined
     * in a coordinate space where coordinates in the range {@code [0..1]}
     * are scaled to map onto the bounds of the shape that the gradient fills.
     * If this flag is false, then the center coordinates and the radius are
     * specified in the local coordinate system of the node.
     *
     * @return true if the center and radius values are proportional, otherwise
     * absolute
     * @defaultValue true
     */
    public final boolean isProportional() {
        return proportional;
    }

    private CycleMethod cycleMethod;

    /**
     * Defines the cycle method applied
     * to the {@code RadialGradient}. One of: {@code CycleMethod.NO_CYCLE},
     * {@code CycleMethod.REFLECT}, or {@code CycleMethod.REPEAT}.
     *
     * @return the cycle method applied to this radial gradient
     * @defaultValue NO_CYCLE
     */
    public final CycleMethod getCycleMethod() {
        return cycleMethod;
    }

    private List<Stop> stops;

    /**
     * A sequence of 2 or more {@code Stop} values specifying how to distribute
     * the colors along the gradient. These values must be in the range
     * 0.0 to 1.0. They act like keyframes along the gradient: they mark where the
     * gradient should be exactly a particular color.
     *
     * <p>Each stop in the sequence must have an offset that is greater than the previous
     * stop in the sequence.</p>
     *
     * <p>The list is unmodifiable and will throw
     * {@code UnsupportedOperationException} on each modification attempt.</p>
     *
     * @return the list of Stop values
     * @defaultValue empty
     */
    public final List<Stop> getStops() {
        return stops;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override public final boolean isOpaque() {
        return opaque;
    }

    private final boolean opaque;

    /**
     * A cached reference to the platform paint, no point recomputing twice
     */
    private Object platformPaint;

    /**
     * The cached hash code, used to improve performance in situations where
     * we cache gradients, such as in the CSS routines.
     */
    private int hash;

    /**
     * Creates a new instance of RadialGradient.
     * @param focusAngle the angle in degrees from the center of the gradient
     * to the focus point to which the first color is mapped
     * @param focusDistance the distance from the center of the gradient to the
     * focus point to which the first color is mapped
     * @param centerX the X coordinate of the center point of the gradient's circle
     * @param centerY the Y coordinate of the center point of the gradient's circle
     * @param radius the radius of the circle defining the extents of the color gradient
     * @param proportional whether the coordinates and sizes are proportional
     * to the shape which this gradient fills
     * @param cycleMethod cycle method applied to the gradient
     * @param stops the gradient's color specification
     */
    public RadialGradient(
            @NamedArg("focusAngle") double focusAngle,
            @NamedArg("focusDistance") double focusDistance,
            @NamedArg("centerX") double centerX,
            @NamedArg("centerY") double centerY,
            @NamedArg(value="radius", defaultValue="1") double radius,
            @NamedArg(value="proportional", defaultValue="true") boolean proportional,
            @NamedArg("cycleMethod") CycleMethod cycleMethod,
            @NamedArg("stops") Stop... stops) {
        this.focusAngle = focusAngle;
        this.focusDistance = focusDistance;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.proportional = proportional;
        this.cycleMethod = (cycleMethod == null) ? CycleMethod.NO_CYCLE : cycleMethod;
        this.stops = Stop.normalize(stops);
        this.opaque = determineOpacity();
    }

    /**
     * Creates a new instance of RadialGradient.
     * @param focusAngle the angle in degrees from the center of the gradient
     * to the focus point to which the first color is mapped
     * @param focusDistance the distance from the center of the gradient to the
     * focus point to which the first color is mapped
     * @param centerX the X coordinate of the center point of the gradient's circle
     * @param centerY the Y coordinate of the center point of the gradient's circle
     * @param radius the radius of the circle defining the extents of the color gradient
     * @param proportional whether the coordinates and sizes are proportional
     * to the shape which this gradient fills
     * @param cycleMethod cycle method applied to the gradient
     * @param stops the gradient's color specification
     */
    public RadialGradient(
            @NamedArg("focusAngle") double focusAngle,
            @NamedArg("focusDistance") double focusDistance,
            @NamedArg("centerX") double centerX,
            @NamedArg("centerY") double centerY,
            @NamedArg(value="radius", defaultValue="1") double radius,
            @NamedArg(value="proportional", defaultValue="true") boolean proportional,
            @NamedArg("cycleMethod") CycleMethod cycleMethod,
            @NamedArg("stops") List<Stop> stops) {
        this.focusAngle = focusAngle;
        this.focusDistance = focusDistance;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.proportional = proportional;
        this.cycleMethod = (cycleMethod == null) ? CycleMethod.NO_CYCLE : cycleMethod;
        this.stops = Stop.normalize(stops);
        this.opaque = determineOpacity();
    }

    /**
     * Iterate over all the stops. If any one of them has a transparent
     * color, then we return false. If there are no stops, we return false.
     * Otherwise, we return true. Note that this is called AFTER Stop.normalize,
     * which ensures that we always have at least 2 stops.
     *
     * @return Whether this gradient is opaque
     */
    private boolean determineOpacity() {
        final int numStops = this.stops.size();
        for (int i = 0; i < numStops; i++) {
            if (!stops.get(i).getColor().isOpaque()) {
                return false;
            }
        }
        return true;
    }

    @Override
    Object acc_getPlatformPaint() {
        if (platformPaint == null) {
            platformPaint = Toolkit.getToolkit().getPaint(this);
        }
        return platformPaint;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is equal to the {@code obj} argument; {@code false} otherwise.
     */
    @Override public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof RadialGradient) {
            final RadialGradient other = (RadialGradient) obj;
            if ((focusAngle != other.focusAngle) ||
                (focusDistance != other.focusDistance) ||
                (centerX != other.centerX) ||
                (centerY != other.centerY) ||
                (radius != other.radius) ||
                (proportional != other.proportional) ||
                (cycleMethod != other.cycleMethod)) return false;
            if (!stops.equals(other.stops)) return false;
            return true;
        } else return false;
    }

    /**
     * Returns a hash code for this {@code RadialGradient} object.
     * @return a hash code for this {@code RadialGradient} object.
     */
    @Override public int hashCode() {
        // We should be able to just call focusAngle.hashCode(),
        // see http://javafx-jira.kenai.com/browse/JFXC-4247
        if (hash == 0) {
            long bits = 17;
            bits = 37 * bits + Double.doubleToLongBits(focusAngle);
            bits = 37 * bits + Double.doubleToLongBits(focusDistance);
            bits = 37 * bits + Double.doubleToLongBits(centerX);
            bits = 37 * bits + Double.doubleToLongBits(centerY);
            bits = 37 * bits + Double.doubleToLongBits(radius);
            bits = 37 * bits + (proportional ? 1231 : 1237);
            bits = 37 * bits + cycleMethod.hashCode();
            for (Stop stop: stops) {
                bits = 37 * bits + stop.hashCode();
            }
            hash = (int) (bits ^ (bits >> 32));
        }
        return hash;
    }

    /**
     * Returns a string representation of this {@code RadialGradient} object.
     * @return a string representation of this {@code RadialGradient} object.
     */
    @Override public String toString() {
        final StringBuilder s = new StringBuilder("radial-gradient(focus-angle ").append(focusAngle)
                .append("deg, focus-distance ").append(focusDistance * 100)
                .append("% , center ").append(GradientUtils.lengthToString(centerX, proportional))
                .append(" ").append(GradientUtils.lengthToString(centerY, proportional))
                .append(", radius ").append(GradientUtils.lengthToString(radius, proportional))
                .append(", ");

        switch (cycleMethod) {
            case REFLECT:
                s.append("reflect").append(", ");
                break;
            case REPEAT:
                s.append("repeat").append(", ");
                break;
        }

        for (Stop stop : stops) {
            s.append(stop).append(", ");
        }

        s.delete(s.length() - 2, s.length());
        s.append(")");

        return s.toString();
    }

    /**
     * Creates a radial gradient value from a string representation.
     * <p>The format of the string representation is based on
     * JavaFX CSS specification for radial gradient which is
     * <pre>
     * radial-gradient([focus-angle &lt;angle&gt;, ]?
     *                 [focus-distance &lt;percentage&gt;, ]?
     *                 [center &lt;point&gt;, ]?
     *                 radius [&lt;length&gt; | &lt;percentage&gt;],
     *                 [[repeat | reflect],]?
     *                 &lt;color-stop&gt;[, &lt;color-stop&gt;]+)
     * </pre>
     * where
     * <pre>
     * &lt;point&gt; = [ [ &lt;length&gt; &lt;length&gt; ] | [ &lt;percentage&gt; | &lt;percentage&gt; ] ]
     * &lt;color-stop&gt; = [ &lt;color&gt; [ &lt;percentage&gt; | &lt;length&gt;]? ]
     * </pre>
     * <p>Currently length can be only specified in px, the specification of unit can be omited.
     * Format of color representation is the one used in {@link Color#web(String color)}.
     * The radial-gradient keyword can be omited.
     * For additional information about the format of string representation, see the
     * <a href="../doc-files/cssref.html">CSS Reference Guide</a>.
     * </p>
     *
     * Examples:
     * <pre>{@code
     * RadialGradient g
     *      = RadialGradient.valueOf("radial-gradient(center 100px 100px, radius 200px, red  0%, blue 30%, black 100%)");
     * RadialGradient g
     *      = RadialGradient.valueOf("center 100px 100px, radius 200px, red  0%, blue 30%, black 100%");
     * RadialGradient g
     *      = RadialGradient.valueOf("radial-gradient(center 50% 50%, radius 50%,  cyan, violet 75%, magenta)");
     * RadialGradient g
     *      = RadialGradient.valueOf("center 50% 50%, radius 50%,  cyan, violet 75%, magenta");
     * }</pre>
     *
     * @param value the string to convert
     * @throws NullPointerException if the {@code value} is {@code null}
     * @throws IllegalArgumentException if the {@code value} cannot be parsed
     * @return a {@code RadialGradient} object holding the value represented
     * by the string argument.
     * @since JavaFX 2.1
     */
    public static RadialGradient valueOf(String value) {
        if (value == null) {
            throw new NullPointerException("gradient must be specified");
        }

        String start = "radial-gradient(";
        String end = ")";
        if (value.startsWith(start)) {
            if (!value.endsWith(end)) {
                throw new IllegalArgumentException("Invalid gradient specification,"
                        + " must end with \"" + end + '"');
            }
            value = value.substring(start.length(), value.length() - end.length());
        }

        GradientUtils.Parser parser = new GradientUtils.Parser(value);
        if (parser.getSize() < 2) {
            throw new IllegalArgumentException("Invalid gradient specification");
        }

        double angle = 0, distance = 0;
        GradientUtils.Point centerX, centerY, radius;

        String[] tokens = parser.splitCurrentToken();
        if ("focus-angle".equals(tokens[0])) {
            GradientUtils.Parser.checkNumberOfArguments(tokens, 1);
            angle = GradientUtils.Parser.parseAngle(tokens[1]);
            parser.shift();
        }

        tokens = parser.splitCurrentToken();
        if ("focus-distance".equals(tokens[0])) {
            GradientUtils.Parser.checkNumberOfArguments(tokens, 1);
            distance = GradientUtils.Parser.parsePercentage(tokens[1]);

            parser.shift();
        }

        tokens = parser.splitCurrentToken();
        if ("center".equals(tokens[0])) {
            GradientUtils.Parser.checkNumberOfArguments(tokens, 2);
            centerX = parser.parsePoint(tokens[1]);
            centerY = parser.parsePoint(tokens[2]);
            parser.shift();
        } else {
            centerX = GradientUtils.Point.MIN;
            centerY = GradientUtils.Point.MIN;
        }

        tokens = parser.splitCurrentToken();
        if ("radius".equals(tokens[0])) {
            GradientUtils.Parser.checkNumberOfArguments(tokens, 1);
            radius = parser.parsePoint(tokens[1]);
            parser.shift();
        } else {
            throw new IllegalArgumentException("Invalid gradient specification: "
                    + "radius must be specified");
        }

        CycleMethod method = CycleMethod.NO_CYCLE;
        String currentToken = parser.getCurrentToken();
        if ("repeat".equals(currentToken)) {
            method = CycleMethod.REPEAT;
            parser.shift();
        } else if ("reflect".equals(currentToken)) {
            method = CycleMethod.REFLECT;
            parser.shift();
        }

        Stop[] stops = parser.parseStops(radius.proportional, radius.value);

        return new RadialGradient(angle, distance, centerX.value, centerY.value,
                                  radius.value, radius.proportional, method, stops);
    }

}
