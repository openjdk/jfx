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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleableProperty;
import javafx.beans.DefaultProperty;

/**
 * <p>A TitledPane is a panel with a title that can be opened and closed.</p>
 *
 * <p>The panel in a TitledPane can be any {@link Node} such as UI controls or groups
 * of nodes added to a layout container.</p>
 *
 * <p>It is not recommended to set the MinHeight, PrefHeight, or MaxHeight
 * for this control.  Unexpected behavior will occur because the
 * TitledPane's height changes when it is opened or closed.</p>
 *
 * <p>Example:</p>
 * <pre><code>
 *  TitledPane t1 = new TitledPane("T1", new Button("B1"));
 *  t1.setMode(TitledPaneMode.FADE);
 * </code></pre>
 *
 */
@DefaultProperty("content")
public class TitledPane extends Labeled {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new TitledPane with no title or content.
     */
    public TitledPane() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    }

    /**
     * Creates a new TitledPane with a title and content.
     * @param title The title of the TitledPane.
     * @param content The content of the TitledPane.
     */
    public TitledPane(String title, Node content) {        
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setText(title);
        setContent(content);
    }

    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- Content
    private ObjectProperty<Node> content;

    /**
     * <p> The content of the TitlePane which can be any Node
     * such as UI controls or groups of nodes added to a layout container.
     *
     * @param value The content for this TitlePane.
     */
    public final void setContent(Node value) {
        contentProperty().set(value);
    }

    /**
     * The content of the TitledPane.  {@code Null} is returned when
     * if there is no content.
     *
     * @return The content of this TitledPane.
     */
    public final Node getContent() {
        return content == null ? null : content.get();
    }

    /**
     * The content of the TitledPane.
     *
     * @return The content of the TitlePane.
     */
    public final ObjectProperty<Node> contentProperty() {
        if (content == null) {
            content = new SimpleObjectProperty<Node>(this, "content");
        }
        return content;
    }


    // --- Expanded
    private BooleanProperty expanded = new BooleanPropertyBase(true) {
        @Override protected void invalidated() {
            get();
            impl_pseudoClassStateChanged(PSEUDO_CLASS_EXPANDED);
            impl_pseudoClassStateChanged(PSEUDO_CLASS_COLLAPSED);
        }

        @Override
        public Object getBean() {
            return TitledPane.this;
        }

        @Override
        public String getName() {
            return "expanded";
        }
    };

    /**
     * Sets the expanded state of the TitledPane.  The default is {@code true}.
     *
     */
    public final void setExpanded(boolean value) { expandedProperty().set(value); }

    /*
     * Returns the expanded state of the TitledPane.  
     *
     * @return The expanded state of the TitledPane.
     */
    public final boolean isExpanded() { return expanded.get(); }

    /**
     * The expanded state of the TitledPane.
     */
    public final BooleanProperty expandedProperty() { return expanded; }


    // --- Animated
    @Styleable(property="-fx-animated", initial="true")
    private BooleanProperty animated = new SimpleBooleanProperty(this, "animated", true);

    /**
     * Specifies how the TitledPane should open and close.  The panel will be
     * animated out when this value is set to {@code true}.  The default is {@code true}.
     *     
     */
    public final void setAnimated(boolean value) { animatedProperty().set(value); }

    /**
     * Returns the animated state of the TitledPane.
     *
     * @return The animated state of the TitledPane.
     */
    public final boolean isAnimated() { return animated.get(); }

    /**
     *  The animated state of the TitledPane.
     */
    public final BooleanProperty animatedProperty() { return animated; }


    // --- Collapsible
    @Styleable(property="-fx-collapsible", initial="true")
    private BooleanProperty collapsible = new SimpleBooleanProperty(this, "collapsible", true);

    /**
     * Specifies if the TitledPane can be collapsed.  The default is {@code true}.
     *
     */
    public final void setCollapsible(boolean value) { collapsibleProperty().set(value); }

    /**
     * Returns the collapsible state of the TitlePane.
     *
     * @return The collapsible state of the TitledPane.
     */
    public final boolean isCollapsible() { return collapsible.get(); }

    /**
     * The collapsible state of the TitledPane.
     */
    public final BooleanProperty collapsibleProperty() { return collapsible; }



    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "titled-pane";

    private static final String PSEUDO_CLASS_EXPANDED = "expanded";
    private static final String PSEUDO_CLASS_COLLAPSED = "collapsed";

    private static class StyleableProperties {

       private static final StyleableProperty COLLAPSIBLE =
           new StyleableProperty(TitledPane.class, "collapsible");
        private static final StyleableProperty ANIMATED =
           new StyleableProperty(TitledPane.class, "animated");

        private static final List<StyleableProperty> STYLEABLES;
        private static final int[] bitIndices;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                COLLAPSIBLE,
                ANIMATED
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
        return StyleableProperties.bitIndices;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSet(String property, Object value) {
        if ("-fx-collapsible".equals(property)) {
            setCollapsible((Boolean)value);
        } else if ("-fx-animated".equals(property)) {
            setAnimated((Boolean)value);
        }
        return super.impl_cssSet(property,value);
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSettable(String property) {
        if ("-fx-collapsible".equals(property)) {
            return collapsible == null || !collapsible.isBound();
        } else if ("-fx-animated".equals(property)) {
            return animated == null || !animated.isBound();
        }

        return super.impl_cssSettable(property);
    }

    private static final long EXPANDED_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("expanded");
    private static final long COLLAPSED_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("collapsed");

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        mask |= isExpanded() ? EXPANDED_PSEUDOCLASS_STATE : COLLAPSED_PSEUDOCLASS_STATE;
        return mask;
    }
}
