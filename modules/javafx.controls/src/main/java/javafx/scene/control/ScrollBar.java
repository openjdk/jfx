/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.util.Utils;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import javafx.scene.control.skin.ScrollBarSkin;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.WritableValue;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Either a horizontal or vertical bar with increment and decrement buttons and
 * a "thumb" with which the user can interact. Typically not used alone but used
 * for building up more complicated controls such as the ScrollPane and ListView.
 * <p>
 * ScrollBar sets focusTraversable to false.
 * </p>
 *
 * <p>
 * This example creates a vertical ScrollBar:
 * <pre><code> ScrollBar s1 = new ScrollBar();
 * s1.setOrientation(Orientation.VERTICAL);</code></pre>
 *
 * <img src="doc-files/ScrollBar.png" alt="Image of the ScrollBar control">
 *
 * @since JavaFX 2.0
 */
public class ScrollBar extends Control {

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new horizontal ScrollBar (ie getOrientation() == Orientation.HORIZONTAL).
     *
     *
     */
    public ScrollBar() {
        // TODO : we need to ensure we have a width and height
        setWidth(Properties.DEFAULT_WIDTH);
        setHeight(Properties.DEFAULT_LENGTH);
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.SCROLL_BAR);
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not
        // override. Initializing focusTraversable by calling applyStyle with null
        // for StyleOrigin ensures that css will be able to override the value.
        ((StyleableProperty<Boolean>)focusTraversableProperty()).applyStyle(null,Boolean.FALSE);

        // set pseudo-class state to horizontal
        pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, true);

    }
    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * The minimum value represented by this {@code ScrollBar}. This should be a
     * value less than or equal to {@link #maxProperty max}. Default value is 0.
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
            min = new SimpleDoubleProperty(this, "min");
        }
        return min;
    }
    /**
     * The maximum value represented by this {@code ScrollBar}. This should be a
     * value greater than or equal to {@link #minProperty min}. Default value is 100.
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
            max = new SimpleDoubleProperty(this, "max", 100);
        }
        return max;
    }
    /**
     * The current value represented by this {@code ScrollBar}. This value should
     * be between {@link #minProperty min} and {@link #maxProperty max}, inclusive.
     */
    private DoubleProperty value;
    public final void setValue(double value) {
        valueProperty().set(value);
    }

    public final double getValue() {
        return value == null ? 0 : value.get();
    }

    public final DoubleProperty valueProperty() {
        if (value == null) {
            value = new SimpleDoubleProperty(this, "value");
        }
        return value;
    }
    /**
     * The orientation of the {@code ScrollBar} can either be {@link javafx.geometry.Orientation#HORIZONTAL HORIZONTAL}
     * or {@link javafx.geometry.Orientation#VERTICAL VERTICAL}.
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
                public CssMetaData<ScrollBar,Orientation> getCssMetaData() {
                    return StyleableProperties.ORIENTATION;
                }

                @Override
                public Object getBean() {
                    return ScrollBar.this;
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
     * The amount by which to adjust the ScrollBar when the {@link #increment() increment} or
     * {@link #decrement() decrement} methods are called.
     */
    private DoubleProperty unitIncrement;
    public final void setUnitIncrement(double value) {
        unitIncrementProperty().set(value);
    }

    public final double getUnitIncrement() {
        return unitIncrement == null ? 1 : unitIncrement.get();
    }

    public final DoubleProperty unitIncrementProperty() {
        if (unitIncrement == null) {
            unitIncrement = new StyleableDoubleProperty(1) {

                @Override
                public CssMetaData<ScrollBar,Number> getCssMetaData() {
                    return StyleableProperties.UNIT_INCREMENT;
                }

                @Override
                public Object getBean() {
                    return ScrollBar.this;
                }

                @Override
                public String getName() {
                    return "unitIncrement";
                }
            };
        }
        return unitIncrement;
    }
    /**
     * The amount by which to adjust the scrollbar if the track of the bar is
     * clicked.
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
                public CssMetaData<ScrollBar,Number> getCssMetaData() {
                    return StyleableProperties.BLOCK_INCREMENT;
                }

                @Override
                public Object getBean() {
                    return ScrollBar.this;
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
     * Visible amount of the scrollbar's range, typically represented by
     * the size of the scroll bar's thumb.
     */
    private DoubleProperty visibleAmount;

    public final void setVisibleAmount(double value) {
        visibleAmountProperty().set(value);
    }

    public final double getVisibleAmount() {
        return visibleAmount == null ? 15 : visibleAmount.get();
    }

    public final DoubleProperty visibleAmountProperty() {
        if (visibleAmount == null) {
            visibleAmount = new SimpleDoubleProperty(this, "visibleAmount");
        }
        return visibleAmount;
    }

    /* *************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Adjusts the {@link #valueProperty() value} property by
     * {@link #blockIncrementProperty() blockIncrement}. The {@code position} is the fractional amount
     * between the {@link #minProperty min} and {@link #maxProperty max}. For
     * example, it might be 50%. If {@code #minProperty min} were 0 and {@code #maxProperty max}
     * were 100 and {@link #valueProperty() value} were 25, then a position of .5 would indicate
     * that we should increment {@link #valueProperty() value} by
     * {@code blockIncrement}. If {@link #valueProperty() value} were 75, then a
     * position of .5 would indicate that we
     * should decrement {@link #valueProperty() value} by {@link #blockIncrementProperty blockIncrement}.
     *
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins or Behaviors. It is not common
     *       for developers or designers to access this function directly.
     * @param position the position
     */
    public void adjustValue(double position) {
        // figure out the "value" associated with the specified position
        double posValue = ((getMax() - getMin()) * Utils.clamp(0, position, 1))+getMin();
        double newValue;
        if (Double.compare(posValue, getValue()) != 0) {
            if (posValue > getValue()) {
                newValue = getValue() + getBlockIncrement();
            }
            else {
                newValue = getValue() - getBlockIncrement();
            }

            setValue(Utils.clamp(getMin(), newValue, getMax()));
        }
    }

    /**
     * Increments the value of the {@code ScrollBar} by the
     * {@link #unitIncrementProperty unitIncrement}
     */
    public void increment() {
        setValue(Utils.clamp(getMin(), getValue() + getUnitIncrement(), getMax()));
    }

    /**
     * Decrements the value of the {@code ScrollBar} by the
     * {@link #unitIncrementProperty unitIncrement}
     */
    public void decrement() {
        setValue(Utils.clamp(getMin(), getValue() - getUnitIncrement(), getMax()));
    }

    private void blockIncrement() {
        adjustValue(getValue() + getBlockIncrement());
    }

    private void blockDecrement() {
        adjustValue(getValue() - getBlockIncrement());
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new ScrollBarSkin(this);
    }

    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    /**
     * Initialize the style class to 'scroll-bar'.
     *
     * This is the selector class from which CSS can be used to style
     * this control.
     */
    private static final String DEFAULT_STYLE_CLASS = "scroll-bar";

    private static class StyleableProperties {
        private static final CssMetaData<ScrollBar,Orientation> ORIENTATION =
            new CssMetaData<>("-fx-orientation",
                new EnumConverter<>(Orientation.class),
                Orientation.HORIZONTAL) {

            @Override
            public Orientation getInitialValue(ScrollBar node) {
                // A vertical ScrollBar should remain vertical
                return node.getOrientation();
            }

            @Override
            public boolean isSettable(ScrollBar n) {
                return n.orientation == null || !n.orientation.isBound();
            }

            @Override
            public StyleableProperty<Orientation> getStyleableProperty(ScrollBar n) {
                return (StyleableProperty<Orientation>)(WritableValue<Orientation>)n.orientationProperty();
            }
        };

        private static final CssMetaData<ScrollBar,Number> UNIT_INCREMENT =
            new CssMetaData<>("-fx-unit-increment",
                SizeConverter.getInstance(), 1.0) {

            @Override
            public boolean isSettable(ScrollBar n) {
                return n.unitIncrement == null || !n.unitIncrement.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(ScrollBar n) {
                return (StyleableProperty<Number>)n.unitIncrementProperty();
            }

        };

        private static final CssMetaData<ScrollBar,Number> BLOCK_INCREMENT =
            new CssMetaData<>("-fx-block-increment",
                SizeConverter.getInstance(), 10.0) {

            @Override
            public boolean isSettable(ScrollBar n) {
                return n.blockIncrement == null || !n.blockIncrement.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(ScrollBar n) {
                return (StyleableProperty<Number>)n.blockIncrementProperty();
            }

        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(Control.getClassCssMetaData());
            styleables.add(ORIENTATION);
            styleables.add(UNIT_INCREMENT);
            styleables.add(BLOCK_INCREMENT);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
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
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    /**
     * Pseud-class indicating this is a vertical ScrollBar.
     */
    private static final PseudoClass VERTICAL_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("vertical");

    /**
     * Pseudo-class indicating this is a horizontal ScrollBar.
     */
    private static final PseudoClass HORIZONTAL_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("horizontal");

    /**
     * Returns the initial focus traversable state of this control, for use
     * by the JavaFX CSS engine to correctly set its initial value. This method
     * is overridden as by default UI controls have focus traversable set to true,
     * but that is not appropriate for this control.
     *
     * @since 9
     */
    @Override protected Boolean getInitialFocusTraversable() {
        return Boolean.FALSE;
    }



    /* *************************************************************************
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
            case BLOCK_INCREMENT: blockIncrement(); break;
            case BLOCK_DECREMENT: blockDecrement(); break;
            case SET_VALUE: {
                Double value = (Double) parameters[0];
                if (value != null) setValue(value);
                break;
            }
            default: super.executeAccessibleAction(action, parameters);
        }
    }
}
