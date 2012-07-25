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

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.EnumConverter;

/**
 * A horizontal or vertical separator line. The visual appearance of this
 * separator can be controlled via CSS. A horizontal separator occupies the
 * full horizontal space allocated to it (less padding), and a vertical
 * separator occupies the full vertical space allocated to it (less padding).
 * The {@link #halignment} and {@link #valignment} properties determine how the
 * separator is positioned in the other dimension, for example, how a horizontal
 * separator is positioned vertically within its allocated space.
 * <p>
 * The separator is horizontal (i.e. <code>isVertical() == false</code>) by default.
 * <p>
 * The style-class for this control is "separator".
 * <p>
 * The separator provides two pseudo-classes "horizontal" and "vertical" which
 * are mutually exclusive. The "horizontal" pseudo-class applies if the
 * separator is horizontal, and the "vertical" pseudo-class applies if the
 * separator is vertical.
 *
 * <p>
 * Separator sets focusTraversable to false.
 * </p>
 */
public class Separator extends Control {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new horizontal separator with halignment and valignment set to their
     * respective CENTER values.
     */
    public Separator() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not 
        // override. Initializing focusTraversable by calling set on the 
        // StyleableProperty ensures that css will be able to override the value.
        final StyleableProperty prop = StyleableProperty.getStyleableProperty(focusTraversableProperty());
        prop.set(this, Boolean.FALSE);            
    }

    /**
     * Creates a new separator with halignment and valignment set to their respective CENTER
     * values. The direction of the separator is specified by the vertical property.
     *
     * @param orientation Specifies whether the Separator instance is initially
     *      vertical or horizontal.
     */
    public Separator(Orientation orientation) {
        this();
        setOrientation(orientation);
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The orientation of the {@code Separator} can either be horizontal
     * or vertical.
     */
    private ObjectProperty<Orientation> orientation = 
        new StyleableObjectProperty<Orientation>(Orientation.HORIZONTAL) {

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
                return Separator.this;
            }

            @Override
            public String getName() {
                return "orientation";
            }
        };
    public final void setOrientation(Orientation value) { orientation.set(value); }
    public final Orientation getOrientation() { return orientation.get(); }
    public final ObjectProperty<Orientation> orientationProperty() { return orientation; }

    /**
     * For vertical separators, specifies the horizontal position of the
     * separator line within the separator control's space. Ignored for
     * horizontal separators.
     */
    private ObjectProperty<HPos> halignment;

    public final void setHalignment(HPos value) {
        halignmentProperty().set(value);
    }

    public final HPos getHalignment() {
        return halignment == null ? HPos.CENTER : halignment.get();
    }

    public final ObjectProperty<HPos> halignmentProperty() {
        if (halignment == null) {
            halignment = new StyleableObjectProperty(HPos.CENTER) {

                @Override
                public Object getBean() {
                    return Separator.this;
                }

                @Override
                public String getName() {
                    return "halignment";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.HALIGNMENT;
                }
                
            };
        }
        return halignment;
    }

    /**
     * For horizontal separators, specifies the vertical alignment of the
     * separator line within the separator control's space. Ignored for
     * vertical separators.
     */
    private ObjectProperty<VPos> valignment;
    public final void setValignment(VPos value) {
        valignmentProperty().set(value);
    }

    public final VPos getValignment() {
        return valignment == null ? VPos.CENTER : valignment.get();
    }

    public final ObjectProperty<VPos> valignmentProperty() {
        if (valignment == null) {
            valignment = new StyleableObjectProperty(VPos.CENTER) {

                @Override
                public Object getBean() {
                    return Separator.this;
                }

                @Override
                public String getName() {
                    return "valignment";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.VALIGNMENT;
                }
                
            };
        }
        return valignment;
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "separator";

    private static final String PSEUDO_CLASS_VERTICAL = "vertical";
    private static final String PSEUDO_CLASS_HORIZONTAL = "horizontal";

    private static class StyleableProperties {
        private static final StyleableProperty<Separator,Orientation> ORIENTATION = 
                new StyleableProperty<Separator,Orientation>("-fx-orientation",
                new EnumConverter<Orientation>(Orientation.class),
                Orientation.HORIZONTAL) {

            @Override
            public Orientation getInitialValue(Separator node) {
                // A vertical Separator should remain vertical 
                return node.getOrientation();
            }

            @Override
            public boolean isSettable(Separator n) {
                return n.orientation == null || !n.orientation.isBound();
            }

            @Override
            public WritableValue<Orientation> getWritableValue(Separator n) {
                return n.orientationProperty();
            }
        };
        
        private static final StyleableProperty<Separator,HPos> HALIGNMENT = 
                new StyleableProperty<Separator,HPos>("-fx-halignment",
                new EnumConverter<HPos>(HPos.class),
                HPos.CENTER) {

            @Override
            public boolean isSettable(Separator n) {
                return n.halignment == null || !n.halignment.isBound();
            }

            @Override
            public WritableValue<HPos> getWritableValue(Separator n) {
                return n.halignmentProperty();
            }
        };
        
        private static final StyleableProperty<Separator,VPos> VALIGNMENT = 
                new StyleableProperty<Separator,VPos>("-fx-valignment",
                new EnumConverter<VPos>(VPos.class),
                VPos.CENTER){

            @Override
            public boolean isSettable(Separator n) {
                return n.valignment == null || !n.valignment.isBound();
            }

            @Override
            public WritableValue<VPos> getWritableValue(Separator n) {
                return n.valignmentProperty();
            }
        };

        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                ORIENTATION,
                HALIGNMENT,
                VALIGNMENT
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
        return Separator.StyleableProperties.STYLEABLES;
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

    private static final long VERTICAL_PSEUDOCLASS_STATE = StyleManager.getInstance().getPseudoclassMask("vertical");
    private static final long HORIZONTAL_PSEUDOCLASS_STATE = StyleManager.getInstance().getPseudoclassMask("horizontal");

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
      * this method to return true, but Separator returns false for
      * focusTraversable's initial value; hence the override of the override. 
      * This method is called from CSS code to get the correct initial value.
      * @treatAsPrivate implementation detail
      */
    @Deprecated @Override
    protected /*do not make final*/ Boolean impl_cssGetFocusTraversableInitialValue() {
        return Boolean.FALSE;
    }
    
}
