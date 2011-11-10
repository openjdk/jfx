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
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;

import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleableProperty;

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
        setFocusTraversable(false);
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
    @Styleable(property="-fx-orientation", initial="vertical")
    private ObjectProperty<Orientation> orientation = new ObjectPropertyBase<Orientation>(Orientation.HORIZONTAL) {
        // We have to ensure that the cssPropertyInvalidated flag is called
        // even when the old value == the new value.
        @Override public void set(Orientation value) {
            super.set(value);
            impl_cssPropertyInvalidated(StyleableProperties.ORIENTATION);
        }

        @Override protected void invalidated() {
            impl_cssPropertyInvalidated(StyleableProperties.ORIENTATION);
            impl_pseudoClassStateChanged(PSEUDO_CLASS_VERTICAL);
            impl_pseudoClassStateChanged(PSEUDO_CLASS_HORIZONTAL);
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
    @Styleable(property="-fx-halignment", initial="center")
    private ObjectProperty<HPos> halignment;

    public final void setHalignment(HPos value) {
        halignmentProperty().set(value);
    }

    public final HPos getHalignment() {
        return halignment == null ? HPos.CENTER : halignment.get();
    }

    public final ObjectProperty<HPos> halignmentProperty() {
        if (halignment == null) {
            halignment = new CSSProperty<HPos>(this, "halignment", HPos.CENTER, StyleableProperties.HPOS);
        }
        return halignment;
    }

    /**
     * For horizontal separators, specifies the vertical alignment of the
     * separator line within the separator control's space. Ignored for
     * vertical separators.
     */
    @Styleable(property="-fx-valignment", initial="center")
    private ObjectProperty<VPos> valignment;
    public final void setValignment(VPos value) {
        valignmentProperty().set(value);
    }

    public final VPos getValignment() {
        return valignment == null ? VPos.CENTER : valignment.get();
    }

    public final ObjectProperty<VPos> valignmentProperty() {
        if (valignment == null) {
            valignment = new CSSProperty<VPos>(this, "valignment", VPos.CENTER, StyleableProperties.VPOS);
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
        private static final StyleableProperty ORIENTATION = new StyleableProperty(Separator.class, "orientation");
        private static final StyleableProperty HPOS = new StyleableProperty(Separator.class, "halignment");
        private static final StyleableProperty VPOS = new StyleableProperty(Separator.class, "valignment");

        private static final List<StyleableProperty> STYLEABLES;
        private static final int[] bitIndices;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                ORIENTATION,
                HPOS,
                VPOS
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
        return Separator.StyleableProperties.bitIndices;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return Separator.StyleableProperties.STYLEABLES;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSet(String property, Object value) {
        if ("-fx-orientation".equals(property)) {
            setOrientation((Orientation) value);
        } else if ("-fx-halignment".equals(property)) {
            setHalignment((HPos) value);
        } else if ("-fx-valignment".equals(property)) {
            setValignment((VPos) value);
        }
        return super.impl_cssSet(property,value);
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSettable(String property) {
        if ("-fx-orientation".equals(property)) {
            return orientation == null || !orientation.isBound();
        } else if ("-fx-halignment".equals(property)) {
            return halignment == null || !halignment.isBound();
        } else if ("-fx-valignment".equals(property)) {
            return valignment == null || !valignment.isBound();

        }

        return super.impl_cssSettable(property);
    }

    private static final long VERTICAL_PSEUDOCLASS_STATE = StyleManager.getInstance().getPseudoclassMask("vertical");
    private static final long HORIZONTAL_PSEUDOCLASS_STATE = StyleManager.getInstance().getPseudoclassMask("horizontal");

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
     * Used to reduce the number of inner classes by having a single property which
     * knows how to invalidate the correct CSS property whenever it is changed.
     * This trades static for dynamic footprint, and is used only in cases where
     * a few extra bytes don't matter (like most control's properties).
     * @param <T>
     */
    private final class CSSProperty<T> extends SimpleObjectProperty<T> {
        private StyleableProperty property;

        private CSSProperty(Object bean, String propertyName, T defaultValue, StyleableProperty p) {
            super(bean, propertyName, defaultValue);
            this.property = p;
        }

        // We have to ensure that the cssPropertyInvalidated flag is called
        // even when the old value == the new value.
        @Override public void set(T value) {
            super.set(value);
            impl_cssPropertyInvalidated(StyleableProperties.ORIENTATION);
        }

        @Override public void invalidated() {
            impl_cssPropertyInvalidated(property);
        }
    }
}
