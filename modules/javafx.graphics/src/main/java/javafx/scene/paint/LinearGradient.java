/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;
import com.sun.javafx.scene.paint.GradientUtils;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.InterpolationUtils;
import javafx.beans.NamedArg;

/**
 * <p>The {@code LinearGradient} class fills a shape
 * with a linear color gradient pattern. The user may specify two or
 * more gradient colors, and this Paint will provide an interpolation
 * between each color.</p>
 *
 * <p>
 * The application provides an array of {@code Stop}s specifying how to distribute
 * the colors along the gradient. The {@code Stop#offset} variable must be
 * the range 0.0 to 1.0 and act like keyframes along the gradient.
 * The offsets mark where the gradient should be exactly a particular color. </p>
 *
 * <p>If the proportional variable is set to true
 * then the start and end points of the gradient
 * should be specified relative to the unit square (0.0-&gt;1.0) and will
 * be stretched across the shape. If the proportional variable is set
 * to false, then the start and end points should be specified
 * in the local coordinate system of the shape and the gradient will
 * not be stretched at all.</p>
 *
 * <p>
 * The two filled rectangles in the example below will render the same.
 * The one on the left uses proportional coordinates to specify
 * the end points of the gradient.  The one on the right uses absolute
 * coordinates.  Both of them fill the specified rectangle with a
 * horizontal gradient that varies from black to red</p>
 *
<PRE>
// object bounding box relative (proportional = true)
Stop[] stops = new Stop[] { new Stop(0, Color.BLACK), new Stop(1, Color.RED)};
LinearGradient lg1 = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
Rectangle r1 = new Rectangle(0, 0, 100, 100);
r1.setFill(lg1);

// user space relative (proportional: = false)
LinearGradient lg2 = new LinearGradient(125, 0, 225, 0, false, CycleMethod.NO_CYCLE, stops);
Rectangle r2 = new Rectangle(125, 0, 100, 100);
r2.setFill(lg2);
</PRE>
 * @since JavaFX 2.0
 */
public final class LinearGradient extends Paint {
    private double startX;

    /**
     * Defines the X coordinate of the gradient axis start point.
     * If proportional is true (the default), this value specifies a
     * point on a unit square that will be scaled to match the size of
     * the shape that the gradient fills.
     *
     * @return the X coordinate of the gradient axis start point
     * @defaultValue 0.0
     * @interpolationType <a href="../../animation/Interpolatable.html#linear">linear</a>
     *                    if both values are absolute or both values are {@link #isProportional() proportional},
     *                    <a href="../../animation/Interpolatable.html#discrete">discrete</a> otherwise
     */
    public final double getStartX() {
        return startX;
    }

    private double startY;

    /**
     * Defines the Y coordinate of the gradient axis start point.
     * If proportional is true (the default), this value specifies a
     * point on a unit square that will be scaled to match the size of
     * the shape that the gradient fills.
     *
     * @return the Y coordinate of the gradient axis start point
     * @defaultValue 0.0
     * @interpolationType <a href="../../animation/Interpolatable.html#linear">linear</a>
     *                    if both values are absolute or both values are {@link #isProportional() proportional},
     *                    <a href="../../animation/Interpolatable.html#discrete">discrete</a> otherwise
     */
    public final double getStartY() {
        return startY;
    }

    private double endX;

    /**
     * Defines the X coordinate of the gradient axis end point.
     * If proportional is true (the default), this value specifies a
     * point on a unit square that will be scaled to match the size of
     * the shape that the gradient fills.
     *
     * @return the X coordinate of the gradient axis end point
     * @defaultValue 1.0
     * @interpolationType <a href="../../animation/Interpolatable.html#linear">linear</a>
     *                    if both values are absolute or both values are {@link #isProportional() proportional},
     *                    <a href="../../animation/Interpolatable.html#discrete">discrete</a> otherwise
     */
    public final double getEndX() {
        return endX;
    }

    private double endY;

    /**
     * Defines the Y coordinate of the gradient axis end point.
     * If proportional is true (the default), this value specifies a
     * point on a unit square that will be scaled to match the size of
     * the shape that the gradient fills.
     *
     * @return the Y coordinate of the gradient axis end point
     * @defaultValue 1.0
     * @interpolationType <a href="../../animation/Interpolatable.html#linear">linear</a>
     *                    if both values are absolute or both values are {@link #isProportional() proportional},
     *                    <a href="../../animation/Interpolatable.html#discrete">discrete</a> otherwise
     */
    public final double getEndY() {
        return endY;
    }

    private boolean proportional;

    /**
     * Indicates whether start and end locations are proportional or absolute.
     * If this flag is true, the two end points are defined in a coordinate
     * space where coordinates in the range {@code [0..1]} are scaled to map
     * onto the bounds of the shape that the gradient fills.
     * If this flag is false, then the coordinates are specified in the local
     * coordinate system of the node.
     *
     * @return if true start and end locations are proportional, otherwise absolute
     * @defaultValue true
     * @interpolationType <a href="../../animation/Interpolatable.html#discrete">discrete</a>
     */
    public final boolean isProportional() {
        return proportional;
    }

    private CycleMethod cycleMethod;

    /**
     * Defines which of the following cycle method is applied
     * to the {@code LinearGradient}: {@code CycleMethod.NO_CYCLE},
     * {@code CycleMethod.REFLECT}, or {@code CycleMethod.REPEAT}.
     *
     * @return the cycle method applied to this linear gradient
     * @defaultValue NO_CYCLE
     * @interpolationType <a href="../../animation/Interpolatable.html#discrete">discrete</a>
     */
    public final CycleMethod getCycleMethod() {
        return cycleMethod;
    }

    private List<Stop> stops;

    /**
     * A sequence of 2 or more {@code Stop} values specifying how to distribute
     * the colors along the gradient. These values must be in the range
     * 0.0 to 1.0. They act like key frames along the gradient: they mark where
     * the gradient should be exactly a particular color.
     *
     * <p>Each stop in the sequence must have an offset that is greater than the
     * previous stop in the sequence.</p>
     *
     * <p>The list is unmodifiable and will throw
     * {@code UnsupportedOperationException} on each modification attempt.</p>
     *
     * @return the list of stop values
     * @defaultValue empty
     * @interpolationType Stop list interpolation produces smooth transitions of gradient stops by allowing
     *                    the insertion of new stops along the gradient. At most, the intermediate stop list
     *                    has the combined number of gradient stops of both the start list and the target list.
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
     * Creates a new instance of LinearGradient.
     * @param startX the X coordinate of the gradient axis start point
     * @param startY the Y coordinate of the gradient axis start point
     * @param endX the X coordinate of the gradient axis end point
     * @param endY the Y coordinate of the gradient axis end point
     * @param proportional whether the coordinates are proportional
     * to the shape which this gradient fills
     * @param cycleMethod cycle method applied to the gradient
     * @param stops the gradient's color specification
     */
    public LinearGradient(
            @NamedArg("startX") double startX,
            @NamedArg("startY") double startY,
            @NamedArg(value="endX", defaultValue="1") double endX,
            @NamedArg(value="endY", defaultValue="1") double endY,
            @NamedArg(value="proportional", defaultValue="true") boolean proportional,
            @NamedArg("cycleMethod") CycleMethod cycleMethod,
            @NamedArg("stops") Stop... stops) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.proportional = proportional;
        this.cycleMethod = (cycleMethod == null) ? CycleMethod.NO_CYCLE: cycleMethod;
        this.stops = Stop.normalize(stops);
        this.opaque = determineOpacity();
    }

    /**
     * Creates a new instance of LinearGradient.
     * @param startX the X coordinate of the gradient axis start point
     * @param startY the Y coordinate of the gradient axis start point
     * @param endX the X coordinate of the gradient axis end point
     * @param endY the Y coordinate of the gradient axis end point
     * @param proportional whether the coordinates are proportional
     * to the shape which this gradient fills
     * @param cycleMethod cycle method applied to the gradient
     * @param stops the gradient's color specification
     */
    public LinearGradient(
            @NamedArg("startX") double startX,
            @NamedArg("startY") double startY,
            @NamedArg(value="endX", defaultValue="1") double endX,
            @NamedArg(value="endY", defaultValue="1") double endY,
            @NamedArg(value="proportional", defaultValue="true") boolean proportional,
            @NamedArg("cycleMethod") CycleMethod cycleMethod,
            @NamedArg("stops") List<Stop> stops) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.proportional = proportional;
        this.cycleMethod = (cycleMethod == null) ? CycleMethod.NO_CYCLE: cycleMethod;
        this.stops = Stop.normalize(stops);
        this.opaque = determineOpacity();
    }

    /**
     * Private constructor accepting a stop list that is already normalized.
     * This constructor is only called from the {@link #interpolate} method.
     */
    private LinearGradient(
            double startX, double startY, double endX, double endY,
            boolean proportional, CycleMethod cycleMethod, List<Stop> stops,
            int ignored) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.proportional = proportional;
        this.cycleMethod = cycleMethod;
        this.stops = stops;
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
     * Returns an intermediate value between the value of this {@code LinearGradient} and the specified
     * {@code endValue} using the linear interpolation factor {@code t}, ranging from 0 (inclusive)
     * to 1 (inclusive).
     *
     * @param endValue the target value
     * @param t the interpolation factor
     * @throws NullPointerException if {@code endValue} is {@code null}
     * @return the intermediate value
     * @since 24
     */
    public LinearGradient interpolate(LinearGradient endValue, double t) {
        Objects.requireNonNull(endValue, "endValue cannot be null");

        // We don't check equals(endValue) here to prevent unnecessary equality checks,
        // and only check for equality with 'this' or 'endValue' after interpolation.
        if (t <= 0.0) {
            return this;
        }

        if (t >= 1.0) {
            return endValue;
        }

        double newStartX, newStartY, newEndX, newEndY;
        boolean newProportional;

        if (this.proportional == endValue.proportional) {
            newStartX = InterpolationUtils.interpolate(this.startX, endValue.startX, t);
            newStartY = InterpolationUtils.interpolate(this.startY, endValue.startY, t);
            newEndX = InterpolationUtils.interpolate(this.endX, endValue.endX, t);
            newEndY = InterpolationUtils.interpolate(this.endY, endValue.endY, t);
            newProportional = this.proportional;
        } else if (t < 0.5) {
            newStartX = this.startX;
            newStartY = this.startY;
            newEndX = this.endX;
            newEndY = this.endY;
            newProportional = this.proportional;
        } else {
            newStartX = endValue.startX;
            newStartY = endValue.startY;
            newEndX = endValue.endX;
            newEndY = endValue.endY;
            newProportional = endValue.proportional;
        }

        CycleMethod newCycleMethod = InterpolationUtils.interpolateDiscrete(this.cycleMethod, endValue.cycleMethod, t);

        // Optimization: if both lists are equal, we don't compute a new intermediate list.
        List<Stop> newStops = this.stops.equals(endValue.stops) ?
            null : Stop.interpolateLists(this.stops, endValue.stops, t);

        if (isSame(newStartX, newStartY, newEndX, newEndY, newProportional,
                   newCycleMethod, Objects.requireNonNullElse(newStops, this.stops))) {
            return this;
        }

        if (endValue.isSame(newStartX, newStartY, newEndX, newEndY, newProportional,
                            newCycleMethod, Objects.requireNonNullElse(newStops, endValue.stops))) {
            return endValue;
        }

        return new LinearGradient(newStartX, newStartY, newEndX, newEndY,
                                  newProportional, newCycleMethod,
                                  Objects.requireNonNullElse(newStops, this.stops), 0);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @since 24
     */
    @Override
    public Paint interpolate(Paint endValue, double t) {
        return InterpolationUtils.interpolatePaint(this, endValue, t);
    }

    private boolean isSame(double startX, double startY, double endX, double endY,
                           boolean proportional, CycleMethod cycleMethod, List<Stop> stops) {
        return this.startX == startX
            && this.startY == startY
            && this.endX == endX
            && this.endY == endY
            && this.proportional == proportional
            && this.cycleMethod == cycleMethod
            && this.stops == stops;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is equal to the {@code obj} argument; {@code false} otherwise.
     */
    @Override public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof LinearGradient) {
            final LinearGradient other = (LinearGradient) obj;
            if ((startX != other.startX) ||
                (startY != other.startY) ||
                (endX != other.endX) ||
                (endY != other.endY) ||
                (proportional != other.proportional) ||
                (cycleMethod != other.cycleMethod)) return false;
            if (!stops.equals(other.stops)) return false;
            return true;
        } else return false;
    }

    /**
     * Returns a hash code for this {@code LinearGradient} object.
     * @return a hash code for this {@code LinearGradient} object.
     */
    @Override public int hashCode() {
        if (hash == 0) {
            long bits = 17L;
            bits = 37L * bits + Double.doubleToLongBits(startX);
            bits = 37L * bits + Double.doubleToLongBits(startY);
            bits = 37L * bits + Double.doubleToLongBits(endX);
            bits = 37L * bits + Double.doubleToLongBits(endY);
            bits = 37L * bits + ((proportional) ? 1231L : 1237L);
            bits = 37L * bits + cycleMethod.hashCode();
            for (Stop stop: stops) {
                bits = 37L * bits + stop.hashCode();
            }
            hash = (int) (bits ^ (bits >> 32));
        }
        return hash;
    }

    /**
     * Returns a string representation of this {@code LinearGradient} object.
     * @return a string representation of this {@code LinearGradient} object.
     */
    @Override public String toString() {
        final StringBuilder s = new StringBuilder("linear-gradient(from ")
                .append(GradientUtils.lengthToString(startX, proportional))
                .append(" ").append(GradientUtils.lengthToString(startY, proportional))
                .append(" to ").append(GradientUtils.lengthToString(endX, proportional))
                .append(" ").append(GradientUtils.lengthToString(endY, proportional))
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
     * Creates a linear gradient value from a string representation.
     * <p>The format of the string representation is based on
     * JavaFX CSS specification for linear gradient which is
     * <pre>
     * linear-gradient( [ [from &lt;point&gt; to &lt;point&gt;| [ to &lt;side-or-corner&gt;], ]? [ [ repeat | reflect ], ]? &lt;color-stop&gt;[, &lt;color-stop&gt;]+)
     * </pre>
     * where
     * <pre>
     * &lt;side-or-corner&gt; = [left | right] || [top | bottom]
     * &lt;point&gt; = [ [ &lt;length&gt; &lt;length&gt; ] | [ &lt;percentage&gt; | &lt;percentage&gt; ] ]
     * &lt;color-stop&gt; = [ &lt;color&gt; [ &lt;percentage&gt; | &lt;length&gt;]? ]
     * </pre>
     * <p> Currently length can be only specified in px, the specification of unit can be omitted.
     * Format of color representation is the one used in {@link Color#web(String color)}.
     * The linear-gradient keyword can be omitted.
     * For additional information about the format of string representation, see the
     * <a href="../doc-files/cssref.html">CSS Reference Guide</a>.
     * </p>
     *
     * Examples:
     * <pre>{@code
     * LinearGradient g
     *      = LinearGradient.valueOf("linear-gradient(from 0% 0% to 100% 100%, red  0% , blue 30%,  black 100%)");
     * LinearGradient g
     *      = LinearGradient.valueOf("from 0% 0% to 100% 100%, red  0% , blue 30%,  black 100%");
     * LinearGradient g
     *      = LinearGradient.valueOf("linear-gradient(from 0px 0px to 200px 0px, #00ff00 0%, 0xff0000 50%, 0x1122ff40 100%)");
     * LinearGradient g
     *      = LinearGradient.valueOf("from 0px 0px to 200px 0px, #00ff00 0%, 0xff0000 50%, 0x1122ff40 100%");
     * }</pre>
     *
     * @param value the string to convert
     * @throws NullPointerException if the {@code value} is {@code null}
     * @throws IllegalArgumentException if the {@code value} cannot be parsed
     * @return a {@code LinearGradient} object holding the value represented
     * by the string argument.
     * @since JavaFX 2.1
     */
    public static LinearGradient valueOf(String value) {
        if (value == null) {
            throw new NullPointerException("gradient must be specified");
        }

        String start = "linear-gradient(";
        String end = ")";
        if (value.startsWith(start)) {
            if (!value.endsWith(end)) {
                throw new IllegalArgumentException("Invalid gradient specification, "
                        + "must end with \"" + end + '"');
            }
            value = value.substring(start.length(), value.length() - end.length());
        }

        GradientUtils.Parser parser = new GradientUtils.Parser(value);
        if (parser.getSize() < 2) {
            throw new IllegalArgumentException("Invalid gradient specification");
        }

        GradientUtils.Point startX = GradientUtils.Point.MIN;
        GradientUtils.Point startY = GradientUtils.Point.MIN;
        GradientUtils.Point endX = GradientUtils.Point.MIN;
        GradientUtils.Point endY = GradientUtils.Point.MIN;

        String[] tokens = parser.splitCurrentToken();
        if ("from".equals(tokens[0])) {
            GradientUtils.Parser.checkNumberOfArguments(tokens, 5);
            startX = parser.parsePoint(tokens[1]);
            startY = parser.parsePoint(tokens[2]);
            if (!"to".equals(tokens[3])) {
                throw new IllegalArgumentException("Invalid gradient specification, \"to\" expected");
            }
            endX = parser.parsePoint(tokens[4]);
            endY = parser.parsePoint(tokens[5]);
            parser.shift();
        } else if ("to".equals(tokens[0])) {
            int horizontalSet = 0;
            int verticalSet = 0;

            for (int i = 1; i < 3 && i < tokens.length; i++) {
                if ("left".equals(tokens[i])) {
                    startX = GradientUtils.Point.MAX;
                    endX = GradientUtils.Point.MIN;
                    horizontalSet++;
                } else if ("right".equals(tokens[i])) {
                    startX = GradientUtils.Point.MIN;
                    endX = GradientUtils.Point.MAX;
                    horizontalSet++;
                } else if ("top".equals(tokens[i])) {
                    startY = GradientUtils.Point.MAX;
                    endY = GradientUtils.Point.MIN;
                    verticalSet++;
                } else if ("bottom".equals(tokens[i])) {
                    startY = GradientUtils.Point.MIN;
                    endY = GradientUtils.Point.MAX;
                    verticalSet++;
                } else {
                    throw new IllegalArgumentException("Invalid gradient specification,"
                        + " unknown value after 'to'");
                }
            }
            if (verticalSet > 1) {
                throw new IllegalArgumentException("Invalid gradient specification,"
                        + " vertical direction set twice after 'to'");
            }
            if (horizontalSet > 1) {
                throw new IllegalArgumentException("Invalid gradient specification,"
                        + " horizontal direction set twice after 'to'");
            }
            parser.shift();
        } else {
            // default is "to bottom"
            startY = GradientUtils.Point.MIN;
            endY = GradientUtils.Point.MAX;
        }

        // repeat/reflect
        CycleMethod method = CycleMethod.NO_CYCLE;
        String currentToken = parser.getCurrentToken();
        if ("repeat".equals(currentToken)) {
            method = CycleMethod.REPEAT;
            parser.shift();
        } else if ("reflect".equals(currentToken)) {
            method = CycleMethod.REFLECT;
            parser.shift();
        }

        double dist = 0;
        if (!startX.proportional) {
            double dx = endX.value - startX.value;
            double dy = endY.value - startY.value;
            dist = Math.sqrt(dx*dx + dy*dy);
        }

        Stop[] stops = parser.parseStops(startX.proportional, dist);

        return new LinearGradient(startX.value, startY.value, endX.value, endY.value,
                                  startX.proportional, method, stops);
    }
}
