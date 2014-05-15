/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.chart;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Dimension2D;
import javafx.geometry.Side;
import javafx.util.Duration;
import javafx.util.StringConverter;

import com.sun.javafx.charts.ChartLayoutAnimator;
import javafx.css.StyleableDoubleProperty;
import javafx.css.CssMetaData;
import com.sun.javafx.css.converters.SizeConverter;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;

/**
 * A axis class that plots a range of numbers with major tick marks every "tickUnit". You can use any Number type with
 * this axis, Long, Double, BigDecimal etc.
 * @since JavaFX 2.0
 */
public final class NumberAxis extends ValueAxis<Number> {

    /** We use these for auto ranging to pick a user friendly tick unit. We handle tick units in the range of 1e-10 to 1e+12 */
    private static final double[] TICK_UNIT_DEFAULTS = {
        1.0E-10d, 2.5E-10d, 5.0E-10d, 1.0E-9d, 2.5E-9d, 5.0E-9d, 1.0E-8d, 2.5E-8d, 5.0E-8d, 1.0E-7d, 2.5E-7d, 5.0E-7d,
        1.0E-6d, 2.5E-6d, 5.0E-6d, 1.0E-5d, 2.5E-5d, 5.0E-5d, 1.0E-4d, 2.5E-4d, 5.0E-4d, 0.0010d, 0.0025d, 0.0050d,
        0.01d, 0.025d, 0.05d, 0.1d, 0.25d, 0.5d, 1.0d, 2.5d, 5.0d, 10.0d, 25.0d, 50.0d, 100.0d, 250.0d, 500.0d,
        1000.0d, 2500.0d, 5000.0d, 10000.0d, 25000.0d, 50000.0d, 100000.0d, 250000.0d, 500000.0d, 1000000.0d,
        2500000.0d, 5000000.0d, 1.0E7d, 2.5E7d, 5.0E7d, 1.0E8d, 2.5E8d, 5.0E8d, 1.0E9d, 2.5E9d, 5.0E9d, 1.0E10d,
        2.5E10d, 5.0E10d, 1.0E11d, 2.5E11d, 5.0E11d, 1.0E12d, 2.5E12d, 5.0E12d
    };
    /** These are matching decimal formatter strings */
    private static final String[] TICK_UNIT_FORMATTER_DEFAULTS = {"0.0000000000", "0.00000000000", "0.0000000000",
                                                                  "0.000000000", "0.0000000000", "0.000000000",
                                                                  "0.00000000", "0.000000000", "0.00000000",
                                                                  "0.0000000", "0.00000000", "0.0000000", "0.000000",
                                                                  "0.0000000", "0.000000", "0.00000", "0.000000",
                                                                  "0.00000", "0.0000", "0.00000", "0.0000", "0.000",
                                                                  "0.0000", "0.000", "0.00", "0.000", "0.00", "0.0",
                                                                  "0.00", "0.0", "0", "0.0", "0", "#,##0"};

    private Object currentAnimationID;
    private final ChartLayoutAnimator animator = new ChartLayoutAnimator(this);
    private IntegerProperty currentRangeIndexProperty = new SimpleIntegerProperty(this, "currentRangeIndex", -1);
    private DefaultFormatter defaultFormatter = new DefaultFormatter(this);

    // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

    /** When true zero is always included in the visible range. This only has effect if auto-ranging is on. */
    private BooleanProperty forceZeroInRange = new BooleanPropertyBase(true) {
        @Override protected void invalidated() {
            // This will effect layout if we are auto ranging
            if(isAutoRanging()) requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return NumberAxis.this;
        }

        @Override
        public String getName() {
            return "forceZeroInRange";
        }
    };
    public final boolean isForceZeroInRange() { return forceZeroInRange.getValue(); }
    public final void setForceZeroInRange(boolean value) { forceZeroInRange.setValue(value); }
    public final BooleanProperty forceZeroInRangeProperty() { return forceZeroInRange; }

    /**  The value between each major tick mark in data units. This is automatically set if we are auto-ranging. */
    private DoubleProperty tickUnit = new StyleableDoubleProperty(5) {
        @Override protected void invalidated() {
            if(!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }
        
        @Override
        public CssMetaData<NumberAxis,Number> getCssMetaData() {
            return StyleableProperties.TICK_UNIT;
        }

        @Override
        public Object getBean() {
            return NumberAxis.this;
        }

        @Override
        public String getName() {
            return "tickUnit";
        }
    };
    public final double getTickUnit() { return tickUnit.get(); }
    public final void setTickUnit(double value) { tickUnit.set(value); }
    public final DoubleProperty tickUnitProperty() { return tickUnit; }

    // -------------- CONSTRUCTORS -------------------------------------------------------------------------------------

    /**
     * Create a auto-ranging NumberAxis
     */
    public NumberAxis() {}

    /**
     * Create a non-auto-ranging NumberAxis with the given upper bound, lower bound and tick unit
     *
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     * @param tickUnit The tick unit, ie space between tickmarks
     */
    public NumberAxis(double lowerBound, double upperBound, double tickUnit) {
        super(lowerBound, upperBound);
        setTickUnit(tickUnit);
    }

    /**
     * Create a non-auto-ranging NumberAxis with the given upper bound, lower bound and tick unit
     *
     * @param axisLabel The name to display for this axis
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     * @param tickUnit The tick unit, ie space between tickmarks
     */
    public NumberAxis(String axisLabel, double lowerBound, double upperBound, double tickUnit) {
        super(lowerBound, upperBound);
        setTickUnit(tickUnit);
        setLabel(axisLabel);
    }

    // -------------- PROTECTED METHODS --------------------------------------------------------------------------------

    /**
     * Get the string label name for a tick mark with the given value
     *
     * @param value The value to format into a tick label string
     * @return A formatted string for the given value
     */
    @Override protected String getTickMarkLabel(Number value) {
        StringConverter<Number> formatter = getTickLabelFormatter();
        if (formatter == null) formatter = defaultFormatter;
        return formatter.toString(value);
    }

    /**
     * Called to get the current axis range.
     *
     * @return A range object that can be passed to setRange() and calculateTickValues()
     */
    @Override protected Object getRange() {
        return new double[]{
            getLowerBound(),
            getUpperBound(),
            getTickUnit(),
            getScale(),
            currentRangeIndexProperty.get()
        };
    }

    /**
     * Called to set the current axis range to the given range. If isAnimating() is true then this method should
     * animate the range to the new range.
     *
     * @param range A range object returned from autoRange()
     * @param animate If true animate the change in range
     */
    @Override protected void setRange(Object range, boolean animate) {
        final double[] rangeProps = (double[]) range;
        final double lowerBound = rangeProps[0];
        final double upperBound = rangeProps[1];
        final double tickUnit = rangeProps[2];
        final double scale = rangeProps[3];
        final double rangeIndex = rangeProps[4];
        currentRangeIndexProperty.set((int)rangeIndex);
        final double oldLowerBound = getLowerBound();
        setLowerBound(lowerBound);
        setUpperBound(upperBound);
        setTickUnit(tickUnit);
        if(animate) {
            animator.stop(currentAnimationID);
            currentAnimationID = animator.animate(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(currentLowerBound, oldLowerBound),
                        new KeyValue(scalePropertyImpl(), getScale())
                ),
                new KeyFrame(Duration.millis(700),
                        new KeyValue(currentLowerBound, lowerBound),
                        new KeyValue(scalePropertyImpl(), scale)
                )
            );
        } else {
            currentLowerBound.set(lowerBound);
            setScale(scale);
        }
    }

    /**
     * Calculate a list of all the data values for each tick mark in range
     *
     * @param length The length of the axis in display units
     * @param range A range object returned from autoRange()
     * @return A list of tick marks that fit along the axis if it was the given length
     */
    @Override protected List<Number> calculateTickValues(double length, Object range) {
        final double[] rangeProps = (double[]) range;
        final double lowerBound = rangeProps[0];
        final double upperBound = rangeProps[1];
        final double tickUnit = rangeProps[2];
        List<Number> tickValues = new ArrayList<>();
        if (lowerBound == upperBound) {
            tickValues.add(lowerBound);
        } else if (tickUnit <= 0) {
            tickValues.add(lowerBound);
            tickValues.add(upperBound);
        } else if (tickUnit > 0) {
            tickValues.add(lowerBound);
            if (((upperBound - lowerBound) / tickUnit) > 2000) {
                // This is a ridiculous amount of major tick marks, something has probably gone wrong
                System.err.println("Warning we tried to create more than 2000 major tick marks on a NumberAxis. " +
                        "Lower Bound=" + lowerBound + ", Upper Bound=" + upperBound + ", Tick Unit=" + tickUnit);
            } else {
                if (lowerBound + tickUnit < upperBound) {
                    // If tickUnit is integer, start with the nearest integer
                    double first = Math.rint(tickUnit) == tickUnit ? Math.ceil(lowerBound) : lowerBound + tickUnit;
                    for (double major = first; major < upperBound; major += tickUnit) {
                        tickValues.add(major);
                    }
                }
            }
            tickValues.add(upperBound);
        }
        return tickValues;
    }

    /**
     * Calculate a list of the data values for every minor tick mark
     *
     * @return List of data values where to draw minor tick marks
     */
    protected List<Number> calculateMinorTickMarks() {
        final List<Number> minorTickMarks = new ArrayList<>();
        final double lowerBound = getLowerBound();
        final double upperBound = getUpperBound();
        final double tickUnit = getTickUnit();
        final double minorUnit = tickUnit/Math.max(1, getMinorTickCount());
        if (tickUnit > 0) {
            if(((upperBound - lowerBound) / minorUnit) > 10000) {
                // This is a ridiculous amount of major tick marks, something has probably gone wrong
                System.err.println("Warning we tried to create more than 10000 minor tick marks on a NumberAxis. " +
                        "Lower Bound=" + getLowerBound() + ", Upper Bound=" + getUpperBound() + ", Tick Unit=" + tickUnit);
                return minorTickMarks;
            }
            final boolean tickUnitIsInteger = Math.rint(tickUnit) == tickUnit;
            if (tickUnitIsInteger) {
                for (double minor = Math.floor(lowerBound) + minorUnit; minor < Math.ceil(lowerBound); minor += minorUnit) {
                    if (minor > lowerBound) {
                        minorTickMarks.add(minor);
                    }
                }
            }
            double major = tickUnitIsInteger ? Math.ceil(lowerBound) : lowerBound;
            for (; major < upperBound; major += tickUnit)  {
                final double next = Math.min(major + tickUnit, upperBound);
                for (double minor = major + minorUnit; minor < next; minor += minorUnit) {
                    minorTickMarks.add(minor);
                }
            }
        }
        return minorTickMarks;
    }

    /**
     * Measure the size of the label for given tick mark value. This uses the font that is set for the tick marks
     *
     * @param value tick mark value
     * @param range range to use during calculations
     * @return size of tick mark label for given value
     */
    @Override protected Dimension2D measureTickMarkSize(Number value, Object range) {
        final double[] rangeProps = (double[]) range;
        final double rangeIndex = rangeProps[4];
        return measureTickMarkSize(value, getTickLabelRotation(), (int)rangeIndex);
    }

    /**
     * Measure the size of the label for given tick mark value. This uses the font that is set for the tick marks
     *
     * @param value     tick mark value
     * @param rotation  The text rotation
     * @param rangeIndex The index of the tick unit range
     * @return size of tick mark label for given value
     */
    private Dimension2D measureTickMarkSize(Number value, double rotation, int rangeIndex) {
        String labelText;
        StringConverter<Number> formatter = getTickLabelFormatter();
        if (formatter == null) formatter = defaultFormatter;
        if(formatter instanceof DefaultFormatter) {
            labelText = ((DefaultFormatter)formatter).toString(value, rangeIndex);
        } else {
            labelText = formatter.toString(value);
        }
        return measureTickMarkLabelSize(labelText, rotation);
    }

    /**
     * Called to set the upper and lower bound and anything else that needs to be auto-ranged
     *
     * @param minValue The min data value that needs to be plotted on this axis
     * @param maxValue The max data value that needs to be plotted on this axis
     * @param length The length of the axis in display coordinates
     * @param labelSize The approximate average size a label takes along the axis
     * @return The calculated range
     */
    @Override protected Object autoRange(double minValue, double maxValue, double length, double labelSize) {
        final Side side = getEffectiveSide();
        // check if we need to force zero into range
        if (isForceZeroInRange()) {
            if (maxValue < 0) {
                maxValue = 0;
            } else if (minValue > 0) {
                minValue = 0;
            }
        }
        final double range = maxValue-minValue;
        // pad min and max by 2%, checking if the range is zero
        final double paddedRange = (range==0) ? 2 : Math.abs(range)*1.02;
        final double padding = (paddedRange - range) / 2;
        // if min and max are not zero then add padding to them
        double paddedMin = minValue - padding;
        double paddedMax = maxValue + padding;
        // check padding has not pushed min or max over zero line
        if ((paddedMin < 0 && minValue >= 0) || (paddedMin > 0 && minValue <= 0)) {
            // padding pushed min above or below zero so clamp to 0
            paddedMin = 0;
        }
        if ((paddedMax < 0 && maxValue >= 0) || (paddedMax > 0 && maxValue <= 0)) {
            // padding pushed min above or below zero so clamp to 0
            paddedMax = 0;
        }
        // calculate the number of tick-marks we can fit in the given length
        int numOfTickMarks = (int)Math.floor(Math.abs(length)/labelSize);
        // can never have less than 2 tick marks one for each end
        numOfTickMarks = Math.max(numOfTickMarks, 2);
        // calculate tick unit for the number of ticks can have in the given data range
        double tickUnit = paddedRange/(double)numOfTickMarks;
        // search for the best tick unit that fits
        double tickUnitRounded = 0;
        double minRounded = 0;
        double maxRounded = 0;
        int count = 0;
        double reqLength = Double.MAX_VALUE;
        int rangeIndex = 10;
        // loop till we find a set of ticks that fit length and result in a total of less than 20 tick marks
        while (reqLength > length || count > 20) {
            // find a user friendly match from our default tick units to match calculated tick unit
            for (int i=0; i<TICK_UNIT_DEFAULTS.length; i++) {
                double tickUnitDefault = TICK_UNIT_DEFAULTS[i];
                if (tickUnitDefault > tickUnit) {
                    tickUnitRounded = tickUnitDefault;
                    rangeIndex = i;
                    break;
                }
            }
            // move min and max to nearest tick mark
            minRounded = Math.floor(paddedMin / tickUnitRounded) * tickUnitRounded;
            maxRounded = Math.ceil(paddedMax / tickUnitRounded) * tickUnitRounded;
            // calculate the required length to display the chosen tick marks for real, this will handle if there are
            // huge numbers involved etc or special formatting of the tick mark label text
            double maxReqTickGap = 0;
            double last = 0;
            count = 0;
            for (double major = minRounded; major <= maxRounded; major += tickUnitRounded, count ++)  {
                double size = side.isVertical() ? measureTickMarkSize(major, getTickLabelRotation(), rangeIndex).getHeight() :
                                            measureTickMarkSize(major, getTickLabelRotation(), rangeIndex).getWidth();
                if (major == minRounded) { // first
                    last = size/2;
                } else {
                    maxReqTickGap = Math.max(maxReqTickGap, last + 6 + (size/2) );
                }
            }
            reqLength = (count-1) * maxReqTickGap;
            tickUnit = tickUnitRounded;
            // check if we already found max tick unit
            if (tickUnitRounded == TICK_UNIT_DEFAULTS[TICK_UNIT_DEFAULTS.length-1]) {
                // nothing we can do so just have to use this
                break;
            }

            // fix for RT-35600 where a massive tick unit was being selected
            // unnecessarily. There is probably a better solution, but this works
            // well enough for now.
            if (numOfTickMarks == 2 && reqLength > length) {
                break;
            }
        }
        // calculate new scale
        final double newScale = calculateNewScale(length, minRounded, maxRounded);
        // return new range
        return new double[]{minRounded, maxRounded, tickUnitRounded, newScale, rangeIndex};
    }

    // -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------

     /** @treatAsPrivate implementation detail */
    private static class StyleableProperties {
        private static final CssMetaData<NumberAxis,Number> TICK_UNIT =
            new CssMetaData<NumberAxis,Number>("-fx-tick-unit",
                SizeConverter.getInstance(), 5.0) {

            @Override
            public boolean isSettable(NumberAxis n) {
                return n.tickUnit == null || !n.tickUnit.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(NumberAxis n) {
                return (StyleableProperty<Number>)n.tickUnitProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
           final List<CssMetaData<? extends Styleable, ?>> styleables = 
               new ArrayList<CssMetaData<? extends Styleable, ?>>(ValueAxis.getClassCssMetaData());
           styleables.add(TICK_UNIT);
           STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    // -------------- INNER CLASSES ------------------------------------------------------------------------------------

    /**
     * Default number formatter for NumberAxis, this stays in sync with auto-ranging and formats values appropriately.
     * You can wrap this formatter to add prefixes or suffixes;
     * @since JavaFX 2.0
     */
    public static class DefaultFormatter extends StringConverter<Number> {
        private DecimalFormat formatter;
        private String prefix = null;
        private String suffix = null;

        /** used internally */
        private DefaultFormatter() {}

        /**
         * Construct a DefaultFormatter for the given NumberAxis
         *
         * @param axis The axis to format tick marks for
         */
        public DefaultFormatter(final NumberAxis axis) {
            formatter = getFormatter(axis.isAutoRanging()? axis.currentRangeIndexProperty.get() : -1);
            final ChangeListener<Object> axisListener = (observable, oldValue, newValue) -> {
                formatter = getFormatter(axis.isAutoRanging()? axis.currentRangeIndexProperty.get() : -1);
            };
            axis.currentRangeIndexProperty.addListener(axisListener);
            axis.autoRangingProperty().addListener(axisListener);
        }

        /**
         * Construct a DefaultFormatter for the given NumberAxis with a prefix and/or suffix.
         *
         * @param axis The axis to format tick marks for
         * @param prefix The prefix to append to the start of formatted number, can be null if not needed
         * @param suffix The suffix to append to the end of formatted number, can be null if not needed
         */
        public DefaultFormatter(NumberAxis axis, String prefix, String suffix) {
            this(axis);
            this.prefix = prefix;
            this.suffix = suffix;
        }

        private static DecimalFormat getFormatter(int rangeIndex) {
            if (rangeIndex < 0) {
                return new DecimalFormat();
            } else if(rangeIndex >= TICK_UNIT_FORMATTER_DEFAULTS.length) {
                return new DecimalFormat(TICK_UNIT_FORMATTER_DEFAULTS[TICK_UNIT_FORMATTER_DEFAULTS.length-1]);
            } else {
                return new DecimalFormat(TICK_UNIT_FORMATTER_DEFAULTS[rangeIndex]);
            }
        }

        /**
        * Converts the object provided into its string form.
        * Format of the returned string is defined by this converter.
        * @return a string representation of the object passed in.
        * @see StringConverter#toString
        */
        @Override public String toString(Number object) {
            return toString(object, formatter);
        }

        private String toString(Number object, int rangeIndex) {
            return toString(object, getFormatter(rangeIndex));
        }

        private String toString(Number object, DecimalFormat formatter) {
            if (prefix != null && suffix != null) {
                return prefix + formatter.format(object) + suffix;
            } else if (prefix != null) {
                return prefix + formatter.format(object);
            } else if (suffix != null) {
                return formatter.format(object) + suffix;
            } else {
                return formatter.format(object);
            }
        }

        /**
        * Converts the string provided into a Number defined by the this converter.
        * Format of the string and type of the resulting object is defined by this converter.
        * @return a Number representation of the string passed in.
        * @see StringConverter#toString
        */
        @Override public Number fromString(String string) {
            try {
                int prefixLength = (prefix == null)? 0: prefix.length();
                int suffixLength = (suffix == null)? 0: suffix.length();
                return formatter.parse(string.substring(prefixLength, string.length() - suffixLength));
            } catch (ParseException e) {
                return null;
            }
        }
    }

}

/*
  // Code to generate tick unit defaults

  public static void main(String[] args) {
        List<BigDecimal> values = new ArrayList<BigDecimal>();
        List<String> formats = new ArrayList<String>();
        for(int power=-10; power <= 12; power ++) {
            BigDecimal val = new BigDecimal(10);
            val = val.pow(power, MathContext.DECIMAL32);
            BigDecimal val2 = val.multiply(new BigDecimal(2.5d));
            BigDecimal val5 = val.multiply(new BigDecimal(5d));
            values.add(val);
            values.add(val2);
            values.add(val5);
            System.out.print("["+power+"]  ");
            System.out.print(
                    val.doubleValue() + "d, " +
                            val2.doubleValue() + "d, " +
                            val5.doubleValue() + "d, "
            );
            DecimalFormat df = null;
            DecimalFormat dfTwoHalf = null;
            if (power < 0) {
                String nf = "0.";
                for (int i=0; i<Math.abs(power); i++) nf = nf+"0";
                System.out.print("    ---   nf = " + nf);
                String nf2 = "0.";
                for (int i=0; i<=Math.abs(power); i++) nf2 = nf2+"0";
                System.out.print("    ---   nf2 = " + nf2);
                df = new DecimalFormat(nf);
                dfTwoHalf = new DecimalFormat(nf2);
                formats.add(nf);
                formats.add(nf2);
                formats.add(nf);
            } else if (power == 0) {
                df = new DecimalFormat("0");
                dfTwoHalf = new DecimalFormat("0.0");
                formats.add("0");
                formats.add("0.0");
                formats.add("0");
            } else {
                String nf = "0";
                for (int i=0; i<Math.abs(power); i++) {
                    if((i % 3) == 2) {
                        nf = "#," + nf;
                    } else {
                        nf = "#" + nf;
                    }
                }
                System.out.print("    ---   nf = " + nf);
                formats.add(nf);
                formats.add(nf);
                formats.add(nf);
                dfTwoHalf = df = new DecimalFormat(nf);
            }
            System.out.println("        ---      "+
                    df.format(val.doubleValue())+", "+
                    dfTwoHalf.format(val2.doubleValue())+", "+
                    df.format(val5.doubleValue())+", "
            );
        }
        System.out.print("    private static final double[] TICK_UNIT_DEFAULTS = { ");
        for(BigDecimal val: values) System.out.print(val.doubleValue()+", ");
        System.out.println(" };");
        System.out.print("    private static final String[] TICK_UNIT_FORMATTER_DEFAULTS = { ");
        for(String format: formats) System.out.print("\""+format+"\", ");
        System.out.println(" };");
    }
*/
