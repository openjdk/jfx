/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.util.StringConverter;

import com.sun.javafx.util.Utils;

import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableObjectProperty;

import javafx.css.converter.BooleanConverter;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import javafx.scene.control.skin.SliderSkin;

import javafx.css.Styleable;
import javafx.css.StyleableProperty;

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
 * <code>max</code> (although a slider whose <code>min</code> and
 * <code>max</code> are equal is a degenerate case that makes no sense).
 * <code>min</code> defaults to 0, whereas <code>max</code> defaults to 100.
 * <p>
 * This first example creates a slider whose range, or span, goes from 0 to 1,
 * and whose value defaults to .5:
 *
 * <pre> Slider slider = new Slider(0, 1, 0.5);</pre>
 *
 * <p>
 * This next example shows a slider with customized tick marks and tick mark
 * labels, which also spans from 0 to 1:
 *
 * <pre> Slider slider = new Slider(0, 1, 0.5);
 * slider.setShowTickMarks(true);
 * slider.setShowTickLabels(true);
 * slider.setMajorTickUnit(0.25f);
 * slider.setBlockIncrement(0.1f);</pre>
 *
 * <img src="doc-files/Slider.png" alt="Image of the Slider control">
 *
 * @since JavaFX 2.0
 */
public class Slider extends Control {

    /**
     * Creates a default Slider instance.
     */
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
        setAccessibleRole(AccessibleRole.SLIDER);
        pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, true);
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
                    notifyAccessibleAttributeChanged(AccessibleAttribute.MAX_VALUE);
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
                    notifyAccessibleAttributeChanged(AccessibleAttribute.MIN_VALUE);
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
                    notifyAccessibleAttributeChanged(AccessibleAttribute.VALUE);
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
    private ObjectProperty<Orientation> orientation;
    public final void setOrientation(Orientation value) {
        orientationProperty().set(value);
    }

    public final Orientation getOrientation() {
        return orientation == null ? Orientation.HORIZONTAL : orientation.get();
    }

    public final ObjectProperty<Orientation> orientationProperty() {
        if (orientation == null) {
            orientation = new StyleableObjectProperty<Orientation>(Orientation.HORIZONTAL) {
                @Override protected void invalidated() {
                    final boolean vertical = (get() == Orientation.VERTICAL);
                    pseudoClassStateChanged(VERTICAL_PSEUDOCLASS_STATE,    vertical);
                    pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, !vertical);
                }

                @Override
                public CssMetaData<Slider,Orientation> getCssMetaData() {
                    return StyleableProperties.ORIENTATION;
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
    private BooleanProperty showTickLabels;
    public final void setShowTickLabels(boolean value) {
        showTickLabelsProperty().set(value);
    }

    public final boolean isShowTickLabels() {
        return showTickLabels == null ? false : showTickLabels.get();
    }

    public final BooleanProperty showTickLabelsProperty() {
        if (showTickLabels == null) {
            showTickLabels = new StyleableBooleanProperty(false) {


                @Override
                public CssMetaData<Slider,Boolean> getCssMetaData() {
                    return StyleableProperties.SHOW_TICK_LABELS;
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
    private BooleanProperty showTickMarks;
    public final void setShowTickMarks(boolean value) {
        showTickMarksProperty().set(value);
    }

    public final boolean isShowTickMarks() {
        return showTickMarks == null ? false : showTickMarks.get();
    }

    public final BooleanProperty showTickMarksProperty() {
        if (showTickMarks == null) {
            showTickMarks = new StyleableBooleanProperty(false) {


                @Override
                public CssMetaData<Slider,Boolean> getCssMetaData() {
                    return StyleableProperties.SHOW_TICK_MARKS;
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
            majorTickUnit = new StyleableDoubleProperty(25) {
                @Override
                public void invalidated() {
                    if (get() <= 0) {
                        throw new IllegalArgumentException("MajorTickUnit cannot be less than or equal to 0.");
                    }
                }

                @Override
                public CssMetaData<Slider,Number> getCssMetaData() {
                    return StyleableProperties.MAJOR_TICK_UNIT;
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
    private IntegerProperty minorTickCount;
    public final void setMinorTickCount(int value) {
        minorTickCountProperty().set(value);
    }

    public final int getMinorTickCount() {
        return minorTickCount == null ? 3 : minorTickCount.get();
    }

    public final IntegerProperty minorTickCountProperty() {
        if (minorTickCount == null) {
            minorTickCount = new StyleableIntegerProperty(3) {


                @Override
                public CssMetaData<Slider,Number> getCssMetaData() {
                    return StyleableProperties.MINOR_TICK_COUNT;
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
    private BooleanProperty snapToTicks;
    public final void setSnapToTicks(boolean value) {
        snapToTicksProperty().set(value);
    }

    public final boolean isSnapToTicks() {
        return snapToTicks == null ? false : snapToTicks.get();
    }

    public final BooleanProperty snapToTicksProperty() {
        if (snapToTicks == null) {
            snapToTicks = new StyleableBooleanProperty(false) {

                @Override
                public CssMetaData<Slider,Boolean> getCssMetaData() {
                    return StyleableProperties.SNAP_TO_TICKS;
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
    private DoubleProperty blockIncrement;
    public final void setBlockIncrement(double value) {
        blockIncrementProperty().set(value);
    }

    public final double getBlockIncrement() {
        return blockIncrement == null ? 10 : blockIncrement.get();
    }

    public final DoubleProperty blockIncrementProperty() {
        if (blockIncrement == null) {
            blockIncrement = new StyleableDoubleProperty(10) {

                @Override
                public CssMetaData<Slider,Number> getCssMetaData() {
                    return StyleableProperties.BLOCK_INCREMENT;
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
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins or Behaviors. It is not common
     *       for developers or designers to access this function directly.
     * @param newValue the new adjusted value
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
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins or Behaviors. It is not common
     *       for developers or designers to access this function directly.
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

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new SliderSkin(this);
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "slider";

    private static class StyleableProperties {
        private static final CssMetaData<Slider,Number> BLOCK_INCREMENT =
            new CssMetaData<Slider,Number>("-fx-block-increment",
                SizeConverter.getInstance(), 10.0) {

            @Override
            public boolean isSettable(Slider n) {
                return n.blockIncrement == null || !n.blockIncrement.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(Slider n) {
                return (StyleableProperty<Number>)(WritableValue<Number>)n.blockIncrementProperty();
            }
        };

        private static final CssMetaData<Slider,Boolean> SHOW_TICK_LABELS =
            new CssMetaData<Slider,Boolean>("-fx-show-tick-labels",
                BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override
            public boolean isSettable(Slider n) {
                return n.showTickLabels == null || !n.showTickLabels.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(Slider n) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)n.showTickLabelsProperty();
            }
        };

        private static final CssMetaData<Slider,Boolean> SHOW_TICK_MARKS =
            new CssMetaData<Slider,Boolean>("-fx-show-tick-marks",
                BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override
            public boolean isSettable(Slider n) {
                return n.showTickMarks == null || !n.showTickMarks.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(Slider n) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)n.showTickMarksProperty();
            }
        };

        private static final CssMetaData<Slider,Boolean> SNAP_TO_TICKS =
            new CssMetaData<Slider,Boolean>("-fx-snap-to-ticks",
                BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override
            public boolean isSettable(Slider n) {
                return n.snapToTicks == null || !n.snapToTicks.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(Slider n) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)n.snapToTicksProperty();
            }
        };

        private static final CssMetaData<Slider,Number> MAJOR_TICK_UNIT =
            new CssMetaData<Slider,Number>("-fx-major-tick-unit",
                SizeConverter.getInstance(), 25.0) {

            @Override
            public boolean isSettable(Slider n) {
                return n.majorTickUnit == null || !n.majorTickUnit.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(Slider n) {
                return (StyleableProperty<Number>)(WritableValue<Number>)n.majorTickUnitProperty();
            }
        };

        private static final CssMetaData<Slider,Number> MINOR_TICK_COUNT =
            new CssMetaData<Slider,Number>("-fx-minor-tick-count",
                SizeConverter.getInstance(), 3.0) {

            @Override
            public boolean isSettable(Slider n) {
                return n.minorTickCount == null || !n.minorTickCount.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(Slider n) {
                return (StyleableProperty<Number>)(WritableValue<Number>)n.minorTickCountProperty();
            }
        };

        private static final CssMetaData<Slider,Orientation> ORIENTATION =
            new CssMetaData<Slider,Orientation>("-fx-orientation",
                new EnumConverter<Orientation>(Orientation.class),
                Orientation.HORIZONTAL) {

            @Override
            public Orientation getInitialValue(Slider node) {
                // A vertical Slider should remain vertical
                return node.getOrientation();
            }

            @Override
            public boolean isSettable(Slider n) {
                return n.orientation == null || !n.orientation.isBound();
            }

            @Override
            public StyleableProperty<Orientation> getStyleableProperty(Slider n) {
                return (StyleableProperty<Orientation>)(WritableValue<Orientation>)n.orientationProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Control.getClassCssMetaData());
            styleables.add(BLOCK_INCREMENT);
            styleables.add(SHOW_TICK_LABELS);
            styleables.add(SHOW_TICK_MARKS);
            styleables.add(SNAP_TO_TICKS);
            styleables.add(MAJOR_TICK_UNIT);
            styleables.add(MINOR_TICK_COUNT);
            styleables.add(ORIENTATION);

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
    @Override protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    private static final PseudoClass VERTICAL_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("vertical");
    private static final PseudoClass HORIZONTAL_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("horizontal");



    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case VALUE: return getValue();
            case MAX_VALUE: return getMax();
            case MIN_VALUE: return getMin();
            case ORIENTATION: return getOrientation();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case INCREMENT: increment(); break;
            case DECREMENT: decrement(); break;
            case SET_VALUE: {
                Double value = (Double) parameters[0];
                if (value != null) setValue(value);
                break;
            }
            default: super.executeAccessibleAction(action, parameters);
        }
    }
}
