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

import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.EnumConverter;

/**
 * <p>
 * A ToolBar is a control which displays items horizontally
 * or vertically. The most common items to place within a {@code ToolBar}
 * are {@link Button Buttons}, {@link ToggleButton ToggleButtons} and
 * {@link Separator Separators}, but you are not restricted to just these, and
 * can insert any {@link Node} into them.
 * </p>
 *
 * <p>If there are too many items to fit in the ToolBar an overflow button will appear.
 * The overflow button allows you to select items that are not currently visible in the toolbar.
 * </p>
 * <p>
 * ToolBar sets focusTraversable to false.
 * </p>
 *
 * <p>
 * Example of a horizontal ToolBar with eight buttons separated with two vertical separators.
 * </p>
 * <pre><code>
 * ToolBar toolBar = new ToolBar(
 *     new Button("New"),
 *     new Button("Open"),
 *     new Button("Save"),
 *     new Separator(true),
 *     new Button("Clean"),
 *     new Button("Compile"),
 *     new Button("Run"),
 *     new Separator(true),
 *     new Button("Debug"),
 *     new Button("Profile")
 * );
 * </code></pre>
 *
 */
@DefaultProperty("items")
public class ToolBar extends Control {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates an empty tool bar.
     */
    public ToolBar() {
        initialize();
    }

    /**
     * Creates a tool bar populated with the specified nodes. None of the items
     * can be null.
     *
     * @param items the items to add
     */
    public ToolBar(Node... items) {
        initialize();
        this.items.addAll(items);
    }

    private void initialize() {
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
     * The items contained in the {@code ToolBar}. Typical use case for a
     * {@code ToolBar} suggest that the most common items to place within it
     * are {@link Button Buttons}, {@link ToggleButton ToggleButtons}, and  {@link Separator Separators},
     * but you are not restricted to just these, and can insert any {@link Node}.
     * The items added must not be null.
     */
    public final ObservableList<Node> getItems() { return items; }

    private final ObservableList<Node> items = FXCollections.<Node>observableArrayList();

    /**
     * The orientation of the {@code ToolBar} - this can either be horizontal
     * or vertical.
     */
    private ObjectProperty<Orientation> orientation;
    public final void setOrientation(Orientation value) {
        orientationProperty().set(value);
    };
    public final Orientation getOrientation() {
        return orientation == null ? Orientation.HORIZONTAL : orientation.get();
    }
    public final ObjectProperty<Orientation> orientationProperty() {
        if (orientation == null) {
            orientation = new StyleableObjectProperty<Orientation>(Orientation.HORIZONTAL) {
                @Override public void invalidated() {
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_VERTICAL);
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_HORIZONTAL);
                }

                @Override
                public Object getBean() {
                    return ToolBar.this;
                }

                @Override
                public String getName() {
                    return "orientation";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.ORIENTATION;
                }
            };
        }
        return orientation;
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "tool-bar";
    private static final String PSEUDO_CLASS_VERTICAL = "vertical";
    private static final String PSEUDO_CLASS_HORIZONTAL = "horizontal";

    private static class StyleableProperties {
        private static final StyleableProperty<ToolBar,Orientation> ORIENTATION = 
                new StyleableProperty<ToolBar,Orientation>("-fx-orientation",
                new EnumConverter<Orientation>(Orientation.class), 
                Orientation.HORIZONTAL) {

            @Override
            public Orientation getInitialValue(ToolBar node) {
                // A vertical ToolBar should remain vertical 
                return node.getOrientation();
            }
            
            @Override
            public boolean isSettable(ToolBar n) {
                return n.orientation == null || !n.orientation.isBound();
            }

            @Override
            public WritableValue<Orientation> getWritableValue(ToolBar n) {
                return n.orientationProperty();
            }
        };

        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                ORIENTATION
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
        return ToolBar.StyleableProperties.STYLEABLES;
    }

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
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
      * this method to return true, but ToolBar returns false for
      * focusTraversable's initial value; hence the override of the override. 
      * This method is called from CSS code to get the correct initial value.
      * @treatAsPrivate implementation detail
      */
    @Deprecated @Override
    protected /*do not make final*/ Boolean impl_cssGetFocusTraversableInitialValue() {
        return Boolean.FALSE;
    }

}
