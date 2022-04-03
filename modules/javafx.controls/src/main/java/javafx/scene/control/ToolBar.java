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
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.css.StyleableObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;

import javafx.css.converter.EnumConverter;
import javafx.scene.control.skin.ToolBarSkin;

import javafx.css.Styleable;
import javafx.css.StyleableProperty;

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
 * <pre><code> ToolBar toolBar = new ToolBar(
 *     new Button("New"),
 *     new Button("Open"),
 *     new Button("Save"),
 *     new Separator(),
 *     new Button("Clean"),
 *     new Button("Compile"),
 *     new Button("Run"),
 *     new Separator(),
 *     new Button("Debug"),
 *     new Button("Profile")
 * );</code></pre>
 *
 * <img src="doc-files/ToolBar.png" alt="Image of the ToolBar control">
 *
 * @since JavaFX 2.0
 */
@DefaultProperty("items")
public class ToolBar extends Control {

    /* *************************************************************************
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
        setAccessibleRole(AccessibleRole.TOOL_BAR);
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not
        // override. Initializing focusTraversable by calling set on the
        // CssMetaData ensures that css will be able to override the value.
        ((StyleableProperty<Boolean>)(WritableValue<Boolean>)focusTraversableProperty()).applyStyle(null, Boolean.FALSE);

        // initialize css pseudo-class state
        pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, true);

    }

    /* *************************************************************************
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
     * @return the list of items
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
                    final boolean isVertical = (get() == Orientation.VERTICAL);
                    pseudoClassStateChanged(VERTICAL_PSEUDOCLASS_STATE,    isVertical);
                    pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, !isVertical);
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
                public CssMetaData<ToolBar,Orientation> getCssMetaData() {
                    return StyleableProperties.ORIENTATION;
                }
            };
        }
        return orientation;
    }

    /* *************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new ToolBarSkin(this);
    }

    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "tool-bar";

    private static class StyleableProperties {
        private static final CssMetaData<ToolBar,Orientation> ORIENTATION =
                new CssMetaData<ToolBar,Orientation>("-fx-orientation",
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
            public StyleableProperty<Orientation> getStyleableProperty(ToolBar n) {
                return (StyleableProperty<Orientation>)(WritableValue<Orientation>)n.orientationProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Control.getClassCssMetaData());
            styleables.add(ORIENTATION);
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

    private static final PseudoClass VERTICAL_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("vertical");
    private static final PseudoClass HORIZONTAL_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("horizontal");

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
}
