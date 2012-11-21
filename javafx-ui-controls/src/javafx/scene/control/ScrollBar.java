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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.WritableValue;
import javafx.geometry.Orientation;

import com.sun.javafx.Utils;
import com.sun.javafx.css.*;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.skin.ScrollBarSkin;


/**
 * Either a horizontal or vertical bar with increment and decrement buttons and
 * a "thumb" with which the user can interact. Typically not used alone but used
 * for building up more complicated controls such as the ScrollPane and ListView.
 * <p>
 * ScrollBar sets focusTraversable to false.
 * </p>
 *
 * <p>
 * This example creates a vertical ScrollBar :
 * <pre><code>
 * import javafx.scene.control.ScrollBar;
 *
 * ScrollBar s1 = new ScrollBar();
 * s1.setOrientation(Orientation.VERTICAL);
 * </code></pre>
 *
 * Implementation of ScrollBar According to JavaFX UI Control API Specification
 */

public class ScrollBar extends Control {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new horizontal (i.e. <code>getOrientation() == Orientation.HORIZONTAL</code>)
     * ScrollBar.
     */
    public ScrollBar() {
        // TODO : we need to ensure we have a width and height
        setWidth(ScrollBarSkin.DEFAULT_WIDTH);
        setHeight(ScrollBarSkin.DEFAULT_LENGTH);
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not 
        // override. Initializing focusTraversable by calling set on the 
        // StyleableProperty ensures that css will be able to override the value.
        final StyleableProperty prop = StyleableProperty.getStyleableProperty(focusTraversableProperty());
        prop.set(this, Boolean.FALSE);            
    }
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * The minimum value represented by this {@code ScrollBar}. This should be a
     * value less than or equal to {@link #maxProperty max}.
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
     * value greater than or equal to {@link #minProperty min}.
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
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_VERTICAL);
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_HORIZONTAL);
                }

                @Override 
                public StyleableProperty getStyleableProperty() {
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
                public StyleableProperty getStyleableProperty() {
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
                public StyleableProperty getStyleableProperty() {
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
     *
     * @since JavaFX 1.3
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

    /***************************************************************************
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
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins or Behaviors. It is not common
     *         for developers or designers to access this function directly.
     */
    public void adjustValue(double position) {
        // figure out the "value" associated with the specified position
        double posValue = ((getMax() - getMin()) * Utils.clamp(0, position, 1))+getMin();
        double newValue;
        if (posValue != getValue()) {
            if (posValue > getValue()) {
                newValue = getValue() + getBlockIncrement();
            }
            else {
                newValue = getValue() - getBlockIncrement();
            }
            
            boolean incrementing = position > ((getValue() - getMin())/(getMax() - getMin()));
            if (incrementing && newValue > posValue) newValue = posValue;
            if (! incrementing && newValue < posValue) newValue = posValue;
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

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new ScrollBarSkin(this);
    }

    /***************************************************************************
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

    /**
     * Pseudoclass indicating this is a vertical ScrollBar.
     */
    private static final String PSEUDO_CLASS_VERTICAL = "vertical";

    /**
     * Pseudoclass indicating this is a horizontal ScrollBar.
     */
    private static final String PSEUDO_CLASS_HORIZONTAL = "horizontal";

    private static class StyleableProperties {
        private static final StyleableProperty<ScrollBar,Orientation> ORIENTATION = 
            new StyleableProperty<ScrollBar,Orientation>("-fx-orientation",
                new EnumConverter<Orientation>(Orientation.class),
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
            public WritableValue<Orientation> getWritableValue(ScrollBar n) {
                return n.orientationProperty();
            }
        };
        
        private static final StyleableProperty<ScrollBar,Number> UNIT_INCREMENT = 
            new StyleableProperty<ScrollBar,Number>("-fx-unit-increment",
                SizeConverter.getInstance(), 1.0) {

            @Override
            public boolean isSettable(ScrollBar n) {
                return n.unitIncrement == null || !n.unitIncrement.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(ScrollBar n) {
                return n.unitIncrementProperty();
            }
                    
        };
        
        private static final StyleableProperty<ScrollBar,Number> BLOCK_INCREMENT = 
            new StyleableProperty<ScrollBar,Number>("-fx-block-increment",
                SizeConverter.getInstance(), 10.0) {

            @Override
            public boolean isSettable(ScrollBar n) {
                return n.blockIncrement == null || !n.blockIncrement.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(ScrollBar n) {
                return n.blockIncrementProperty();
            }
                    
        };
        
        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables = 
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                ORIENTATION,
                UNIT_INCREMENT,
                BLOCK_INCREMENT
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return ScrollBar.StyleableProperties.STYLEABLES;
    }

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    @Override protected List<StyleableProperty> impl_getControlStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }

    private static final long VERTICAL_PSEUDOCLASS_STATE =
            StyleManager.getPseudoclassMask("vertical");
    private static final long HORIZONTAL_PSEUDOCLASS_STATE =
            StyleManager.getPseudoclassMask("horizontal");

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        mask |= (getOrientation() == Orientation.VERTICAL) ?
            VERTICAL_PSEUDOCLASS_STATE : HORIZONTAL_PSEUDOCLASS_STATE;
        return mask;
    }
    
    /**
      * Most Controls return true for focusTraversable, so Control overrides
      * this method to return true, but ScrollBar returns false for
      * focusTraversable's initial value; hence the override of the override. 
      * This method is called from CSS code to get the correct initial value.
      * @treatAsPrivate implementation detail
      */
    @Deprecated @Override
    protected /*do not make final*/ Boolean impl_cssGetFocusTraversableInitialValue() {
        return Boolean.FALSE;
    }
    
}
