/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.geometry.Orientation;
import javafx.util.StringConverter;

import com.sun.javafx.Utils;
import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleableProperty;

/**
 * The Slider Control is used to display a continuous or discrete range of
 * valid numeric choices and allows the user to interact with the control. It is
 * typically represented visually as having a "track" and a "knob" or "thumb"
 * which is dragged within the track. The Slider can optionally show tick marks
 * and labels indicating the different slider position values.
 * <p>
 * The three fundamental variables of the slider are <code>min</code>,
 * <code>max</code>, and <code>value</code>. The <code>value</code> should always
 * be a number within the range defined by <code>min</code> and
 * <code>max</code>. <code>min</code> should always be less than or equal to
 * <code>max</code> (although a slider who's <code>min</code> and
 * <code>max</code> are equal is a degenerate case that makes no sense).
 * <code>min</code> defaults to 0, whereas <code>max</code> defaults to 100.
 * <p>
 * This first example creates a slider who's range, or span, goes from 0 to 1,
 * and who's value defaults to .5:
 *
 * <pre>
 * import javafx.scene.control.Slider;
 * 
 * Slider slider = new Slider(0, 1, 0.5);
 * </pre>
 * 
 * <p>
 * This next example shows a slider with customized tick marks and tick mark
 * labels, which also spans from 0 to 1:
 *
 * <pre>
 * import javafx.scene.control.Slider;
 * 
 * Slider slider = new Slider(0, 1, 0.5);
 * slider.setShowTickMarks(true);
 * slider.setShowTickLabels(true);
 * slider.setMajorTickUnit(0.25f);
 * slider.setBlockIncrement(0.1f);
 * </pre>
 */
public class Slider extends Control {

    public Slider() {
        initialize();
    }

    /**
     * Constructs a Slider control with the specified slider min, max and current value values.
     * @param min Slider minimum value
     * @param max Slider maximum value
     * @param value Slider current value
     */
    public Slider(double min, double max, double value) {
        setMax(max);
        setMin(min);
        setValue(value);
        adjustValues();
        initialize();
    }

    private void initialize() {
        //Initialize the style class to be 'slider'.
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    }
    /**
     * The maximum value represented by this Slider. This must be a
     * value greater than {@link #minProperty() min}.
     */
    private DoubleProperty max;
    public final void setMax(double value) {
        maxProperty().set(value);
    }

    public final double getMax() {
        return max == null ? 100 : max.get();
    }

    public final DoubleProperty maxProperty() {
        if (max == null) {
            max = new DoublePropertyBase(100) {
                @Override protected void invalidated() {
                    if (get() < getMin()) {
                        setMin(get());
                    }
                    adjustValues();
                }

                @Override
                public Object getBean() {
                    return Slider.this;
                }

                @Override
                public String getName() {
                    return "max";
                }
            };
        }
        return max;
    }
    /**
     * The minimum value represented by this Slider. This must be a
     * value less than {@link #maxProperty() max}.
     */
    private DoubleProperty min;
    public final void setMin(double value) {
        minProperty().set(value);
    }

    public final double getMin() {
        return min == null ? 0 : min.get();
    }

    public final DoubleProperty minProperty() {
        if (min == null) {
            min = new DoublePropertyBase(0) {
                @Override protected void invalidated() {
                    if (get() > getMax()) {
                        setMax(get());
                    }
                    adjustValues();
                }

                @Override
                public Object getBean() {
                    return Slider.this;
                }

                @Override
                public String getName() {
                    return "min";
                }
            };
        }
        return min;
    }
    /**
     * The current value represented by this Slider. This value must
     * always be between {@link #minProperty() min} and {@link #maxProperty() max},
     * inclusive. If it is ever out of bounds either due to {@code min} or
     * {@code max} changing or due to itself being changed, then it will
     * be clamped to always remain valid.
     */
    private DoubleProperty value;
    public final void setValue(double value) {
        if (!valueProperty().isBound()) valueProperty().set(value);
    }

    public final double getValue() {
        return value == null ? 0 : value.get();
    }

    public final DoubleProperty valueProperty() {
        if (value == null) {
            value = new DoublePropertyBase(0) {
                @Override protected void invalidated() {
                    adjustValues();
                }

                @Override
                public Object getBean() {
                    return Slider.this;
                }

                @Override
                public String getName() {
                    return "value";
                }
            };
        }
        return value;
    }
    /**
     * When true, indicates the current value of this Slider is changing.
     * It provides notification that the value is changing. Once the value is
     * computed, it is reset back to false.
     */
    private BooleanProperty valueChanging;

    public final void setValueChanging(boolean value) {
        valueChangingProperty().set(value);
    }

    public final boolean isValueChanging() {
        return valueChanging == null ? false : valueChanging.get();
    }

    public final BooleanProperty valueChangingProperty() {
        if (valueChanging == null) {
            valueChanging = new SimpleBooleanProperty(this, "valueChanging", false);
        }
        return valueChanging;
    }
//    /**
//     * The {@code span} is the distance, or quantity, between min and max value.
//     * This will be strictly non-negative, since both {@code min} and
//     * {@code max} are forced to maintain a proper relationship.
//     */
//    //    public def span = bind max - min;
    
    /**
     * The orientation of the {@code Slider} can either be horizontal
     * or vertical.
     */
    @Styleable(property="-fx-orientation", initial="vertical")
    private ObjectProperty<Orientation> orientation;
    public final void setOrientation(Orientation value) {
        orientationProperty().set(value);
    }

    public final Orientation getOrientation() {
        return orientation == null ? Orientation.HORIZONTAL : orientation.get();
    }

    public final ObjectProperty<Orientation> orientationProperty() {
        if (orientation == null) {
            orientation = new ObjectPropertyBase<Orientation>(Orientation.HORIZONTAL) {
                @Override protected void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.ORIENTATION);
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_VERTICAL);
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_HORIZONTAL);
                }

                @Override
                public Object getBean() {
                    return Slider.this;
                }

                @Override
                public String getName() {
                    return "orientation";
                }
            };
        }
        return orientation;
    }
    
    
    /**
     * Indicates that the labels for tick marks should be shown. Typically a
     * {@link Skin} implementation will only show labels if
     * {@link #showTickMarksProperty() showTickMarks} is also true.
     */
    @Styleable(property="-fx-show-tick-labels", initial="false")
    private BooleanProperty showTickLabels;
    public final void setShowTickLabels(boolean value) {
        showTickLabelsProperty().set(value);
    }

    public final boolean isShowTickLabels() {
        return showTickLabels == null ? false : showTickLabels.get();
    }

    public final BooleanProperty showTickLabelsProperty() {
        if (showTickLabels == null) {
            showTickLabels = new BooleanPropertyBase(false) {

                @Override
                public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.SHOW_TICK_LABELS);
                }

                @Override
                public Object getBean() {
                    return Slider.this;
                }

                @Override
                public String getName() {
                    return "showTickLabels";
                }
            };
        }
        return showTickLabels;
    }
    /**
     * Specifies whether the {@link Skin} implementation should show tick marks.
     */
    @Styleable(property="-fx-show-tick-marks", initial="false")
    private BooleanProperty showTickMarks;
    public final void setShowTickMarks(boolean value) {
        showTickMarksProperty().set(value);
    }

    public final boolean isShowTickMarks() {
        return showTickMarks == null ? false : showTickMarks.get();
    }

    public final BooleanProperty showTickMarksProperty() {
        if (showTickMarks == null) {
            showTickMarks = new BooleanPropertyBase(false) {

                @Override
                public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.SHOW_TICK_MARKS);
                }

                @Override
                public Object getBean() {
                    return Slider.this;
                }

                @Override
                public String getName() {
                    return "showTickMarks";
                }
            };
        }
        return showTickMarks;
    }
    /**
     * The unit distance between major tick marks. For example, if
     * the {@link #minProperty() min} is 0 and the {@link #maxProperty() max} is 100 and the
     * {@link #majorTickUnitProperty() majorTickUnit} is 25, then there would be 5 tick marks: one at
     * position 0, one at position 25, one at position 50, one at position
     * 75, and a final one at position 100.
     * <p>
     * This value should be positive and should be a value less than the
     * span. Out of range values are essentially the same as disabling
     * tick marks.
     */
    @Styleable(property="-fx-major-tick-unit", initial="25")
    private DoubleProperty majorTickUnit;
    public final void setMajorTickUnit(double value) {
        if (value <= 0) {
            throw new IllegalArgumentException("MajorTickUnit cannot be less than or equal to 0.");
        }
        majorTickUnitProperty().set(value);
    }

    public final double getMajorTickUnit() {
        return majorTickUnit == null ? 25 : majorTickUnit.get();
    }

    public final DoubleProperty majorTickUnitProperty() {
        if (majorTickUnit == null) {
            majorTickUnit = new DoublePropertyBase(25) {
                @Override
                public void invalidated() {
                    if (get() <= 0) {
                        throw new IllegalArgumentException("MajorTickUnit cannot be less than or equal to 0.");
                    }

                    impl_cssPropertyInvalidated(StyleableProperties.MAJOR_TICK_UNIT);
                }

                @Override
                public Object getBean() {
                    return Slider.this;
                }

                @Override
                public String getName() {
                    return "majorTickUnit";
                }
            };
        }
        return majorTickUnit;
    }
    /**
     * The number of minor ticks to place between any two major ticks. This
     * number should be positive or zero. Out of range values will disable
     * disable minor ticks, as will a value of zero.
     */
    @Styleable(property="-fx-minor-tick-count", initial="3")
    private IntegerProperty minorTickCount;
    public final void setMinorTickCount(int value) {
        minorTickCountProperty().set(value);
    }

    public final int getMinorTickCount() {
        return minorTickCount == null ? 3 : minorTickCount.get();
    }

    public final IntegerProperty minorTickCountProperty() {
        if (minorTickCount == null) {
            minorTickCount = new IntegerPropertyBase(3) {

                @Override
                public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.MINOR_TICK_COUNT);
                }

                @Override
                public Object getBean() {
                    return Slider.this;
                }

                @Override
                public String getName() {
                    return "minorTickCount";
                }
            };
        }
        return minorTickCount;
    }
    /**
     * Indicates whether the {@link #valueProperty() value} of the {@code Slider} should always
     * be aligned with the tick marks. This is honored even if the tick marks
     * are not shown.
     */
    @Styleable(property="-fx-snap-to-ticks", initial="false")
    private BooleanProperty snapToTicks;
    public final void setSnapToTicks(boolean value) {
        snapToTicksProperty().set(value);
    }

    public final boolean isSnapToTicks() {
        return snapToTicks == null ? false : snapToTicks.get();
    }

    public final BooleanProperty snapToTicksProperty() {
        if (snapToTicks == null) {
            snapToTicks = new BooleanPropertyBase(false) {

                @Override
                public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.SNAP_TO_TICKS);
                }

                @Override
                public Object getBean() {
                    return Slider.this;
                }

                @Override
                public String getName() {
                    return "snapToTicks";
                }
            };
        }
        return snapToTicks;
    }
    /**
     * A function for formatting the label for a major tick. The number
     * representing the major tick will be passed to the function. If this
     * function is not specified, then a default function will be used by
     * the {@link Skin} implementation.
     */
    private ObjectProperty<StringConverter<Double>> labelFormatter;

    public final void setLabelFormatter(StringConverter<Double> value) {
        labelFormatterProperty().set(value);
    }

    public final StringConverter<Double> getLabelFormatter() {
        return labelFormatter == null ? null : labelFormatter.get();
    }

    public final ObjectProperty<StringConverter<Double>> labelFormatterProperty() {
        if (labelFormatter == null) {
            labelFormatter = new SimpleObjectProperty<StringConverter<Double>>(this, "labelFormatter");
        }
        return labelFormatter;
    }
    /**
     * The amount by which to adjust the slider if the track of the slider is
     * clicked. This is used when manipulating the slider position using keys. If
     * {@link #snapToTicksProperty() snapToTicks} is true then the nearest tick mark to the adjusted
     * value will be used.
     */
    @Styleable(property="-fx-block-increment", initial="10")
    private DoubleProperty blockIncrement;
    public final void setBlockIncrement(double value) {
        blockIncrementProperty().set(value);
    }

    public final double getBlockIncrement() {
        return blockIncrement == null ? 10 : blockIncrement.get();
    }

    public final DoubleProperty blockIncrementProperty() {
        if (blockIncrement == null) {
            blockIncrement = new DoublePropertyBase(10) {

                @Override
                public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.BLOCK_INCREMENT);
                }

                @Override
                public Object getBean() {
                    return Slider.this;
                }

                @Override
                public String getName() {
                    return "blockIncrement";
                }
            };
        }
        return blockIncrement;
    }

    /**
     * Adjusts {@link #valueProperty() value} to match <code>newValue</code>. The 
     * <code>value</code>is the actual amount between the 
     * {@link #minProperty() min} and {@link #maxProperty() max}. This function
     * also takes into account {@link #snapToTicksProperty() snapToTicks}, which
     * is the main difference between adjustValue and setValue. It also ensures 
     * that the value is some valid number between min and max.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins or Behaviors. It is not common
     *         for developers or designers to access this function directly.
     */
    public void adjustValue(double newValue) {
        // figure out the "value" associated with the specified position
        final double _min = getMin();
        final double _max = getMax();
        if (_max <= _min) return;
        newValue = newValue < _min ? _min : newValue;
        newValue = newValue > _max ? _max : newValue;

        setValue(snapValueToTicks(newValue));
    }

    /**
     * Increments the value by {@link #blockIncrementProperty() blockIncrement}, bounded by max. If the
     * max is less than or equal to the min, then this method does nothing.
     */
    public void increment() {
        adjustValue(getValue() + getBlockIncrement());
    }
    
    /**
     * Decrements the value by {@link #blockIncrementProperty() blockIncrement}, bounded by max. If the
     * max is less than or equal to the min, then this method does nothing.
     */
    public void decrement() {
        adjustValue(getValue() - getBlockIncrement());
    }
    
    /**
     * Ensures that min is always < max, that value is always
     * somewhere between the two, and that if snapToTicks is set then the
     * value will always be set to align with a tick mark.
     */
    private void adjustValues() {
        if ((getValue() < getMin() || getValue() > getMax()) /* &&  !isReadOnly(value)*/)
             setValue(Utils.clamp(getMin(), getValue(), getMax()));
    }

     /**
     * Utility function which, given the specified value, will position it
     * either aligned with a tick, or simply clamp between min & max value,
     * depending on whether snapToTicks is set.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins or Behaviors. It is not common
     *         for developers or designers to access this function directly.
     */
    private double snapValueToTicks(double val) {
        double v = val;
        if (isSnapToTicks()) {
            double tickSpacing = 0;
            // compute the nearest tick to this value
            if (getMinorTickCount() != 0) {
                tickSpacing = getMajorTickUnit() / (Math.max(getMinorTickCount(),0)+1);
            } else {
                tickSpacing = getMajorTickUnit();
            }
            int prevTick = (int)((v - getMin())/ tickSpacing);
            double prevTickValue = (prevTick) * tickSpacing + getMin();
            double nextTickValue = (prevTick + 1) * tickSpacing + getMin();
            v = Utils.nearest(prevTickValue, v, nextTickValue);
        }
        return Utils.clamp(getMin(), v, getMax());
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "slider";
    private static final String PSEUDO_CLASS_VERTICAL = "vertical";
    private static final String PSEUDO_CLASS_HORIZONTAL = "horizontal";

    private static class StyleableProperties {
        private static final StyleableProperty BLOCK_INCREMENT =
            new StyleableProperty(Slider.class, "blockIncrement");
        private static final StyleableProperty SHOW_TICK_LABELS =
            new StyleableProperty(Slider.class, "showTickLabels");
        private static final StyleableProperty SHOW_TICK_MARKS =
            new StyleableProperty(Slider.class, "showTickMarks");
        private static final StyleableProperty SNAP_TO_TICKS =
            new StyleableProperty(Slider.class, "snapToTicks");
        private static final StyleableProperty MAJOR_TICK_UNIT =
            new StyleableProperty(Slider.class, "majorTickUnit");
        private static final StyleableProperty MINOR_TICK_COUNT =
            new StyleableProperty(Slider.class, "minorTickCount");
        private static final StyleableProperty ORIENTATION =
            new StyleableProperty(Slider.class, "orientation");

        private static final List<StyleableProperty> STYLEABLES;
        private static final int[] bitIndices;
        static {
            final List<StyleableProperty> styleables = 
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                BLOCK_INCREMENT,
                SHOW_TICK_LABELS,
                SHOW_TICK_MARKS,
                SNAP_TO_TICKS,
                MAJOR_TICK_UNIT,
                MINOR_TICK_COUNT,
                ORIENTATION
            );
            STYLEABLES = Collections.unmodifiableList(styleables);

            bitIndices = new int[StyleableProperty.getMaxIndex()];
            java.util.Arrays.fill(bitIndices, -1);
            for(int bitIndex=0; bitIndex<STYLEABLES.size(); bitIndex++) {
                bitIndices[STYLEABLES.get(bitIndex).getIndex()] = bitIndex;
            }
        }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected int[] impl_cssStyleablePropertyBitIndices() {
        return Slider.StyleableProperties.bitIndices;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return Slider.StyleableProperties.STYLEABLES;
    }

    private static final long VERTICAL_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("vertical");
    private static final long HORIZONTAL_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("horizontal");

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        mask |= (getOrientation() == Orientation.VERTICAL) ?
            VERTICAL_PSEUDOCLASS_STATE : HORIZONTAL_PSEUDOCLASS_STATE;
        return mask;
    }
    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSet(String property, Object value) {
        if ("-fx-block-increment".equals(property) ) {
            setBlockIncrement((Double)value);
        }  else if ( "-fx-show-tick-labels".equals(property) ) {
            setShowTickLabels((Boolean)value);
        }  else if ( "-fx-show-tick-marks".equals(property) ) {
            setShowTickMarks((Boolean)value);
        }  else if ( "-fx-snap-to-ticks".equals(property) ) {
            setSnapToTicks((Boolean)value);
        }  else if ( "-fx-major-tick-unit".equals(property) ) {
            setMajorTickUnit((Double)value);
        }  else if ( "-fx-minor-tick-count".equals(property) ) {
            setMinorTickCount((int)((Double)value).doubleValue());
        } else if ("-fx-orientation".equals(property)) {
            setOrientation((Orientation) value);
        }
        return super.impl_cssSet(property,value);
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSettable(String property) {
        if ( "-fx-block-increment".equals(property) )
            return blockIncrement == null || !blockIncrement.isBound();
        else if ( "-fx-show-tick-labels".equals(property) )
            return showTickLabels == null || !showTickLabels.isBound();
        else if ( "-fx-show-tick-marks".equals(property) )
            return showTickMarks == null || !showTickMarks.isBound();
        else if ( "-fx-snap-to-ticks".equals(property) )
            return snapToTicks == null || !snapToTicks.isBound();
        else if ( "-fx-major-tick-unit".equals(property) )
            return majorTickUnit == null || !majorTickUnit.isBound();
        else if ( "-fx-minor-tick-count".equals(property) )
            return minorTickCount == null || !minorTickCount.isBound();
        else if ("-fx-orientation".equals(property))
            return orientation == null || !orientation.isBound();
        else
            return super.impl_cssSettable(property);
   }
}
