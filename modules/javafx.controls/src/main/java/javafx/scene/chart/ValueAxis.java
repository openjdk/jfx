/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.CssMetaData;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableIntegerProperty;

import javafx.css.converter.BooleanConverter;
import javafx.css.converter.SizeConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.*;
import javafx.beans.value.WritableValue;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.geometry.Side;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.StringConverter;


/**
 * An axis whose data is defined as Numbers. It can also draw minor
 * tick-marks between the major ones.
 * @since JavaFX 2.0
 */
public abstract class ValueAxis<T extends Number> extends Axis<T> {

    // -------------- PRIVATE FIELDS -----------------------------------------------------------------------------------

    private final Path minorTickPath  = new Path();

    private double offset;
    /** This is the minimum current data value and it is used while auto ranging.
     *  Package private solely for test purposes */
    double dataMinValue;
    /** This is the maximum current data value and it is used while auto ranging.
     *  Package private solely for test purposes */
    double dataMaxValue;
    /** List of the values at which there are minor ticks */
    private List<T> minorTickMarkValues = null;
    private boolean minorTickMarksDirty = true;
    // -------------- PRIVATE PROPERTIES -------------------------------------------------------------------------------

    /**
     * The current value for the lowerBound of this axis (minimum value).
     * This may be the same as lowerBound or different. It is used by NumberAxis to animate the
     * lowerBound from the old value to the new value.
     */
    protected final DoubleProperty currentLowerBound = new SimpleDoubleProperty(this, "currentLowerBound");

    // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

    /** true if minor tick marks should be displayed */
    private BooleanProperty minorTickVisible = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            minorTickPath.setVisible(get());
            requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return ValueAxis.this;
        }

        @Override
        public String getName() {
            return "minorTickVisible";
        }

        @Override
        public CssMetaData<ValueAxis<? extends Number>,Boolean> getCssMetaData() {
            return StyleableProperties.MINOR_TICK_VISIBLE;
        }
    };
    public final boolean isMinorTickVisible() { return minorTickVisible.get(); }
    public final void setMinorTickVisible(boolean value) { minorTickVisible.set(value); }
    public final BooleanProperty minorTickVisibleProperty() { return minorTickVisible; }


    /** The scale factor from data units to visual units */
    private ReadOnlyDoubleWrapper scale = new ReadOnlyDoubleWrapper(this, "scale", 0) {
        @Override
        protected void invalidated() {
            requestAxisLayout();
            measureInvalid = true;
        }
    };
    public final double getScale() { return scale.get(); }
    protected final void setScale(double scale) { this.scale.set(scale); }
    public final ReadOnlyDoubleProperty scaleProperty() { return scale.getReadOnlyProperty(); }
    ReadOnlyDoubleWrapper scalePropertyImpl() { return scale; }

    /** The value for the upper bound of this axis (maximum value). This is automatically set if auto ranging is on. */
    private DoubleProperty upperBound = new DoublePropertyBase(100) {
        @Override protected void invalidated() {
            if(!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return ValueAxis.this;
        }

        @Override
        public String getName() {
            return "upperBound";
        }
    };
    public final double getUpperBound() { return upperBound.get(); }
    public final void setUpperBound(double value) { upperBound.set(value); }
    public final DoubleProperty upperBoundProperty() { return upperBound; }

    /** The value for the lower bound of this axis (minimum value). This is automatically set if auto ranging is on. */
    private DoubleProperty lowerBound = new DoublePropertyBase(0) {
        @Override protected void invalidated() {
            if(!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return ValueAxis.this;
        }

        @Override
        public String getName() {
            return "lowerBound";
        }
    };
    public final double getLowerBound() { return lowerBound.get(); }
    public final void setLowerBound(double value) { lowerBound.set(value); }
    public final DoubleProperty lowerBoundProperty() { return lowerBound; }

    /** StringConverter used to format tick mark labels. If null a default will be used */
    private final ObjectProperty<StringConverter<T>> tickLabelFormatter = new ObjectPropertyBase<StringConverter<T>>(null){
        @Override protected void invalidated() {
            invalidateRange();
            requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return ValueAxis.this;
        }

        @Override
        public String getName() {
            return "tickLabelFormatter";
        }
    };
    public final StringConverter<T> getTickLabelFormatter() { return tickLabelFormatter.getValue(); }
    public final void setTickLabelFormatter(StringConverter<T> value) { tickLabelFormatter.setValue(value); }
    public final ObjectProperty<StringConverter<T>> tickLabelFormatterProperty() { return tickLabelFormatter; }

    /** The length of minor tick mark lines. Set to 0 to not display minor tick marks. */
    private DoubleProperty minorTickLength = new StyleableDoubleProperty(5) {
        @Override protected void invalidated() {
            requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return ValueAxis.this;
        }

        @Override
        public String getName() {
            return "minorTickLength";
        }

        @Override
        public CssMetaData<ValueAxis<? extends Number>,Number> getCssMetaData() {
            return StyleableProperties.MINOR_TICK_LENGTH;
        }
    };
    public final double getMinorTickLength() { return minorTickLength.get(); }
    public final void setMinorTickLength(double value) { minorTickLength.set(value); }
    public final DoubleProperty minorTickLengthProperty() { return minorTickLength; }

    /**
     * The number of minor tick divisions to be displayed between each major tick mark.
     * The number of actual minor tick marks will be one less than this.
     */
    private IntegerProperty minorTickCount = new StyleableIntegerProperty(5) {
        @Override protected void invalidated() {
            invalidateRange();
            requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return ValueAxis.this;
        }

        @Override
        public String getName() {
            return "minorTickCount";
        }

        @Override
        public CssMetaData<ValueAxis<? extends Number>,Number> getCssMetaData() {
            return StyleableProperties.MINOR_TICK_COUNT;
        }
    };
    public final int getMinorTickCount() { return minorTickCount.get(); }
    public final void setMinorTickCount(int value) { minorTickCount.set(value); }
    public final IntegerProperty minorTickCountProperty() { return minorTickCount; }

    // -------------- CONSTRUCTORS -------------------------------------------------------------------------------------

    /**
     * Creates a auto-ranging ValueAxis.
     */
    public ValueAxis() {
        minorTickPath.getStyleClass().add("axis-minor-tick-mark");
        getChildren().add(minorTickPath);
    }

    /**
     * Creates a non-auto-ranging ValueAxis with the given lower and upper bound.
     *
     * @param lowerBound The lower bound for this axis, i.e. min plottable value
     * @param upperBound The upper bound for this axis, i.e. max plottable value
     */
    public ValueAxis(double lowerBound, double upperBound) {
        this();
        setAutoRanging(false);
        setLowerBound(lowerBound);
        setUpperBound(upperBound);
    }

    // -------------- PROTECTED METHODS --------------------------------------------------------------------------------


    /**
     * This calculates the upper and lower bound based on the data provided to invalidateRange() method. This must not
     * affect the state of the axis. Any results of the auto-ranging should be
     * returned in the range object. This will we passed to setRange() if it has been decided to adopt this range for
     * this axis.
     *
     * @param length The length of the axis in screen coordinates
     * @return Range information, this is implementation dependent
     */
    @Override protected final Object autoRange(double length) {
        // guess a sensible starting size for label size, that is approx 2 lines vertically or 2 charts horizontally
        if (isAutoRanging()) {
            // guess a sensible starting size for label size, that is approx 2 lines vertically or 2 charts horizontally
            double labelSize = getTickLabelFont().getSize() * 2;
            return autoRange(dataMinValue,dataMaxValue,length,labelSize);
        } else {
            return getRange();
        }
    }

    /**
     * Calculates new scale for this axis. This should not affect any properties of this axis.
     *
     * @param length The display length of the axis
     * @param lowerBound The lower bound value
     * @param upperBound The upper bound value
     * @return new scale to fit the range from lower bound to upper bound in the given display length
     */
    protected final double calculateNewScale(double length, double lowerBound, double upperBound) {
        double newScale = 1;
        final Side side = getEffectiveSide();
        if (side.isVertical()) {
            offset = length;
            newScale = ((upperBound-lowerBound) == 0) ? -length : -(length / (upperBound - lowerBound));
        } else { // HORIZONTAL
            offset = 0;
            newScale = ((upperBound-lowerBound) == 0) ? length : length / (upperBound - lowerBound);
        }
        return newScale;
    }

    /**
     * Called to set the upper and lower bound and anything else that needs to be auto-ranged. This must not affect
     * the state of the axis. Any results of the auto-ranging should be returned
     * in the range object. This will we passed to setRange() if it has been decided to adopt this range for this axis.
     *
     * @param minValue The min data value that needs to be plotted on this axis
     * @param maxValue The max data value that needs to be plotted on this axis
     * @param length The length of the axis in display coordinates
     * @param labelSize The approximate average size a label takes along the axis
     * @return The calculated range
     */
    protected Object autoRange(double minValue, double maxValue, double length, double labelSize) {
        return null; // this method should have been abstract as there is no way for it to
        // return anything correct. so just return null.

    }

    /**
     * Calculates a list of the data values for every minor tick mark
     *
     * @return List of data values where to draw minor tick marks
     */
    protected abstract List<T> calculateMinorTickMarks();

    /**
     * Called during layout if the tickmarks have been updated, allowing subclasses to do anything they need to
     * in reaction.
     */
    @Override protected void tickMarksUpdated() {
        super.tickMarksUpdated();
        // recalculate minor tick marks
        minorTickMarkValues = calculateMinorTickMarks();
        minorTickMarksDirty = true;
    }

    /**
     * Invoked during the layout pass to layout this axis and all its content.
     */
    @Override protected void layoutChildren() {
        final Side side = getEffectiveSide();
        final double length = side.isVertical() ? getHeight() :getWidth() ;
        // if we are not auto ranging we need to calculate the new scale
        if(!isAutoRanging()) {
            // calculate new scale
            setScale(calculateNewScale(length, getLowerBound(), getUpperBound()));
            // update current lower bound
            currentLowerBound.set(getLowerBound());
        }
        // we have done all auto calcs, let Axis position major tickmarks
        super.layoutChildren();

        if (minorTickMarksDirty) {
            minorTickMarksDirty = false;
            updateMinorTickPath(side, length);
        }
    }

    private void updateMinorTickPath(Side side, double length) {
        int numMinorTicks = (getTickMarks().size() - 1)*(Math.max(1, getMinorTickCount()) - 1);
        double neededLength = (getTickMarks().size()+numMinorTicks)*2;

        // Update minor tickmarks
        minorTickPath.getElements().clear();
        // Don't draw minor tick marks if there isn't enough space for them!

        double minorTickLength = Math.max(0, getMinorTickLength());
        if (minorTickLength > 0 && length > neededLength) {
            if (Side.LEFT.equals(side)) {
                // snap minorTickPath to pixels
                minorTickPath.setLayoutX(-0.5);
                minorTickPath.setLayoutY(0.5);
                for (T value : minorTickMarkValues) {
                    double y = getDisplayPosition(value);
                    if (y >= 0 && y <= length) {
                        minorTickPath.getElements().addAll(
                                new MoveTo(getWidth() - minorTickLength, y),
                                new LineTo(getWidth() - 1, y));
                    }
                }
            } else if (Side.RIGHT.equals(side)) {
                // snap minorTickPath to pixels
                minorTickPath.setLayoutX(0.5);
                minorTickPath.setLayoutY(0.5);
                for (T value : minorTickMarkValues) {
                    double y = getDisplayPosition(value);
                    if (y >= 0 && y <= length) {
                        minorTickPath.getElements().addAll(
                                new MoveTo(1, y),
                                new LineTo(minorTickLength, y));
                    }
                }
            } else if (Side.TOP.equals(side)) {
                // snap minorTickPath to pixels
                minorTickPath.setLayoutX(0.5);
                minorTickPath.setLayoutY(-0.5);
                for (T value : minorTickMarkValues) {
                    double x = getDisplayPosition(value);
                    if (x >= 0 && x <= length) {
                        minorTickPath.getElements().addAll(
                                new MoveTo(x, getHeight() - 1),
                                new LineTo(x, getHeight() - minorTickLength));
                    }
                }
            } else { // BOTTOM
                // snap minorTickPath to pixels
                minorTickPath.setLayoutX(0.5);
                minorTickPath.setLayoutY(0.5);
                for (T value : minorTickMarkValues) {
                    double x = getDisplayPosition(value);
                    if (x >= 0 && x <= length) {
                        minorTickPath.getElements().addAll(
                                new MoveTo(x, 1.0F),
                                new LineTo(x, minorTickLength));
                    }
                }
            }
        }
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------

    /**
     * Called when the data has changed and the range may not be valid anymore. This is only called by the chart if
     * isAutoRanging() returns true. If we are auto ranging it will cause layout to be requested and auto ranging to
     * happen on next layout pass.
     *
     * @param data The current set of all data that needs to be plotted on this axis
     */
    @Override public void invalidateRange(List<T> data) {
        if (data.isEmpty()) {
            dataMaxValue = getUpperBound();
            dataMinValue = getLowerBound();
        } else {
            dataMinValue = Double.MAX_VALUE;
            // We need to init to the lowest negative double (which is NOT Double.MIN_VALUE)
            // in order to find the maximum (positive or negative)
            dataMaxValue = -Double.MAX_VALUE;
        }
        for(T dataValue: data) {
            dataMinValue = Math.min(dataMinValue, dataValue.doubleValue());
            dataMaxValue = Math.max(dataMaxValue, dataValue.doubleValue());
        }
        super.invalidateRange(data);
    }

    /**
     * Gets the display position along this axis for a given value.
     * If the value is not in the current range, the returned value will be an extrapolation of the display
     * position.
     *
     * @param value The data value to work out display position for
     * @return display position
     */
    @Override public double getDisplayPosition(T value) {
        return offset + ((value.doubleValue() - currentLowerBound.get()) * getScale());
    }

    /**
     * Gets the data value for the given display position on this axis. If the axis
     * is a CategoryAxis this will be the nearest value.
     *
     * @param  displayPosition A pixel position on this axis
     * @return the nearest data value to the given pixel position or
     *         null if not on axis;
     */
    @Override public T getValueForDisplay(double displayPosition) {
        return toRealValue(((displayPosition-offset) / getScale()) + currentLowerBound.get());
    }

    /**
     * Gets the display position of the zero line along this axis.
     *
     * @return display position or Double.NaN if zero is not in current range;
     */
    @Override public double getZeroPosition() {
        if (0 < getLowerBound() || 0 > getUpperBound()) return Double.NaN;
        //noinspection unchecked
        return getDisplayPosition((T)Double.valueOf(0));
    }

    /**
     * Checks if the given value is plottable on this axis
     *
     * @param value The value to check if its on axis
     * @return true if the given value is plottable on this axis
     */
    @Override public boolean isValueOnAxis(T value) {
        final double num = value.doubleValue();
        return num >= getLowerBound() && num <= getUpperBound();
    }

    /**
     * All axis values must be representable by some numeric value. This gets the numeric value for a given data value.
     *
     * @param value The data value to convert
     * @return Numeric value for the given data value
     */
    @Override public double toNumericValue(T value) {
        return (value == null) ? Double.NaN : value.doubleValue();
    }

    /**
     * All axis values must be representable by some numeric value. This gets the data value for a given numeric value.
     *
     * @param value The numeric value to convert
     * @return Data value for given numeric value
     */
    @Override public T toRealValue(double value) {
        //noinspection unchecked
        return (T)Double.valueOf(value);
    }

    // -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------

    private static class StyleableProperties  {
        private  static final CssMetaData<ValueAxis<? extends Number>,Number> MINOR_TICK_LENGTH =
            new CssMetaData<ValueAxis<? extends Number>,Number>("-fx-minor-tick-length",
                SizeConverter.getInstance(), 5.0) {

            @Override
            public boolean isSettable(ValueAxis<? extends Number> n) {
                return n.minorTickLength == null || !n.minorTickLength.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(ValueAxis<? extends Number> n) {
                return (StyleableProperty<Number>)(WritableValue<Number>)n.minorTickLengthProperty();
            }
        };

        private static final CssMetaData<ValueAxis<? extends Number>,Number> MINOR_TICK_COUNT =
            new CssMetaData<ValueAxis<? extends Number>,Number>("-fx-minor-tick-count",
                SizeConverter.getInstance(), 5) {

            @Override
            public boolean isSettable(ValueAxis<? extends Number> n) {
                return n.minorTickCount == null || !n.minorTickCount.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(ValueAxis<? extends Number> n) {
                return (StyleableProperty<Number>)(WritableValue<Number>)n.minorTickCountProperty();
            }
        };

         private static final CssMetaData<ValueAxis<? extends Number>,Boolean> MINOR_TICK_VISIBLE =
            new CssMetaData<ValueAxis<? extends Number>,Boolean>("-fx-minor-tick-visible",
                 BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(ValueAxis<? extends Number> n) {
                return n.minorTickVisible == null || !n.minorTickVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(ValueAxis<? extends Number> n) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)n.minorTickVisibleProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Axis.getClassCssMetaData());
            styleables.add(MINOR_TICK_COUNT);
            styleables.add(MINOR_TICK_LENGTH);
            styleables.add(MINOR_TICK_COUNT);
            styleables.add(MINOR_TICK_VISIBLE);
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
     }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
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

}
