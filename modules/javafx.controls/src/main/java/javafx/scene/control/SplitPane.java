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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;

import javafx.css.StyleableObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;

import javafx.css.converter.EnumConverter;
import javafx.scene.control.skin.SplitPaneSkin;

import javafx.css.Styleable;
import javafx.css.StyleableProperty;

/**
 * <p>A control that has two or more sides, each separated by a divider, which can be
 * dragged by the user to give more space to one of the sides, resulting in
 * the other side shrinking by an equal amount.</p>
 *
 * <p>{@link Node Nodes} can be positioned horizontally next to each other, or stacked
 * vertically. This can be controlled by setting the {@link #orientationProperty()}.</p>
 *
 * <p> The dividers in a SplitPane have the following behavior
 * <ul>
 * <li>Dividers cannot overlap another divider</li>
 * <li>Dividers cannot overlap a node.</li>
 * <li>Dividers moving to the left/top will stop when the node's min size is reached.</li>
 * <li>Dividers moving to the right/bottom will stop when the node's max size is reached.</li>
 * </ul>
 *
 * <p>Nodes needs to be placed inside a layout container before they are added
 * into the SplitPane.  If the node is not inside a layout container
 * the maximum and minimum position of the divider will be the
 * maximum and minimum size of the content.
 * </p>
 *
 * <p>A divider's position ranges from 0 to 1.0(inclusive).  A position of 0 will place the
 * divider at the left/top most edge of the SplitPane plus the minimum size of the node.  A
 * position of 1.0 will place the divider at the right/bottom most edge of the SplitPane minus the
 * minimum size of the node.  A divider position of 0.5 will place the
 * the divider in the middle of the SplitPane.  Setting the divider position greater
 * than the node's maximum size position will result in the divider being set at the
 * node's maximum size position.  Setting the divider position less than the node's minimum size position
 * will result in the divider being set at the node's minimum size position. Therefore the value set in
 * {@link #setDividerPosition} and {@link #setDividerPositions} may not be the same as the value returned by
 * {@link #getDividerPositions}.
 * </p>
 *
 * <p>If there are more than two nodes in the SplitPane and the divider positions are set in such a
 * way that the dividers cannot fit the nodes the dividers will be automatically adjusted by the SplitPane.
 * <p>For example we have three nodes whose sizes and divider positions are
 * </p>
 * <pre>
 * Node 1: min 25 max 100
 * Node 2: min 100 max 200
 * Node 3: min 25 max 50
 * divider 1: 0.40
 * divider 2: 0.45
 * </pre>
 *
 * <p>The result will be Node 1 size will be its pref size and divider 1 will be positioned 0.40,
 * Node 2 size will be its min size and divider 2 position will be the min size of Node 2 plus
 * divider 1 position, and the remaining space will be given to Node 3.
 * </p>
 *
 * <p>
 * SplitPane sets focusTraversable to false.
 * </p>
 *
 * <p>Example:</p>
 * <pre><code> SplitPane sp = new SplitPane();
 * final StackPane sp1 = new StackPane();
 * sp1.getChildren().add(new Button("Button One"));
 * final StackPane sp2 = new StackPane();
 * sp2.getChildren().add(new Button("Button Two"));
 * final StackPane sp3 = new StackPane();
 * sp3.getChildren().add(new Button("Button Three"));
 * sp.getItems().addAll(sp1, sp2, sp3);
 * sp.setDividerPositions(0.3f, 0.6f, 0.9f);</code></pre>
 *
 * <img src="doc-files/SplitPane.png" alt="Image of the SplitPane control">
 *
 * @since JavaFX 2.0
 */
@DefaultProperty("items")
public class SplitPane extends Control {

    /* ******************************************************************
     *  static methods
     ********************************************************************/
    private static final String RESIZABLE_WITH_PARENT = "resizable-with-parent";

    /**
     * Sets a node in the SplitPane to be resizable or not when the SplitPane is
     * resized.  By default all node are resizable.  Setting value to false will
     * prevent the node from being resized.
     * @param node A node in the SplitPane.
     * @param value true if the node is resizable or false if not resizable.
     * @since JavaFX 2.1
     */
    public static void setResizableWithParent(Node node, Boolean value) {
        if (value == null) {
            node.getProperties().remove(RESIZABLE_WITH_PARENT);
        } else {
            node.getProperties().put(RESIZABLE_WITH_PARENT, value);
        }
    }

    /**
     * Return true if the node is resizable when the parent container is resized false otherwise.
     * @param node A node in the SplitPane.
     * @defaultValue true
     * @return true if the node is resizable false otherwise.
     * @since JavaFX 2.1
     */
    public static Boolean isResizableWithParent(Node node) {
        if (node.hasProperties()) {
            Object value = node.getProperties().get(RESIZABLE_WITH_PARENT);
            if (value != null) {
                return (Boolean)value;
            }
        }
        return true;
    }

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new SplitPane with no content.
     */
    public SplitPane() {
        this((Node[])null);
    }

    /**
     * Creates a new SplitPane with the given items set as the content to split
     * between one or more dividers.
     *
     * @param items The items to place inside the SplitPane.
     * @since JavaFX 8u40
     */
    public SplitPane(Node... items) {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not
        // override. Initializing focusTraversable by calling applyStyle with a
        // null StyleOrigin ensures that css will be able to override the value.
        ((StyleableProperty<Boolean>)focusTraversableProperty()).applyStyle(null, Boolean.FALSE);

        getItems().addListener(new ListChangeListener<Node>() {
            @Override public void onChanged(Change<? extends Node> c) {
                while (c.next()) {
                    int from = c.getFrom();
                    int index = from;
                    for (int i = 0; i < c.getRemovedSize(); i++) {
                        if (index < dividers.size()) {
                            dividerCache.put(index, Double.MAX_VALUE);
                        } else if (index == dividers.size()) {
                            if (!dividers.isEmpty()) {
                                if (c.wasReplaced()) {
                                    dividerCache.put(index - 1, dividers.get(index - 1).getPosition());
                                } else {
                                    dividerCache.put(index - 1, Double.MAX_VALUE);
                                }
                            }
                        }
                        index++;
                    }
                    for (int i = 0; i < dividers.size(); i++) {
                        if (dividerCache.get(i) == null) {
                            dividerCache.put(i, dividers.get(i).getPosition());
                        }
                    }
                }
                dividers.clear();
                for (int i = 0; i < getItems().size() - 1; i++) {
                    if (dividerCache.containsKey(i) && dividerCache.get(i) != Double.MAX_VALUE) {
                        Divider d = new Divider();
                        d.setPosition(dividerCache.get(i));
                        dividers.add(d);
                    } else {
                        dividers.add(new Divider());
                    }
                    dividerCache.remove(i);
                }
            }
        });

        if (items != null) {
            getItems().addAll(items);
        }

        // initialize pseudo-class state
        pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, true);
    }

    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- Vertical
    private ObjectProperty<Orientation> orientation;

    /**
     * <p>This property controls how the SplitPane should be displayed to the
     * user. {@link javafx.geometry.Orientation#HORIZONTAL} will result in
     * two (or more) nodes being placed next to each other horizontally, whilst
     * {@link javafx.geometry.Orientation#VERTICAL} will result in the nodes being
     * stacked vertically.</p>
     *
     * @param value the orientation value
     */
    public final void setOrientation(Orientation value) {
        orientationProperty().set(value);
    }

    /**
     * The orientation for the SplitPane.
     * @return The orientation for the SplitPane.
     */
    public final Orientation getOrientation() {
        return orientation == null ? Orientation.HORIZONTAL : orientation.get();
    }

    /**
     * The orientation for the SplitPane.
     * @return the orientation property for the SplitPane
     */
    public final ObjectProperty<Orientation> orientationProperty() {
        if (orientation == null) {
            orientation = new StyleableObjectProperty<Orientation>(Orientation.HORIZONTAL) {
                @Override public void invalidated() {
                    final boolean isVertical = (get() == Orientation.VERTICAL);
                    pseudoClassStateChanged(VERTICAL_PSEUDOCLASS_STATE,    isVertical);
                    pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, !isVertical);
                }

                @Override public CssMetaData<SplitPane,Orientation> getCssMetaData() {
                    return StyleableProperties.ORIENTATION;
                }

                @Override
                public Object getBean() {
                    return SplitPane.this;
                }

                @Override
                public String getName() {
                    return "orientation";
                }
            };
        }
        return orientation;
    }



    /* *************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    private final ObservableList<Node> items = FXCollections.observableArrayList();

    private final ObservableList<Divider> dividers = FXCollections.observableArrayList();
    private final ObservableList<Divider> unmodifiableDividers = FXCollections.unmodifiableObservableList(dividers);

    // Cache the divider positions if the items have not been created.
    private final WeakHashMap<Integer, Double> dividerCache = new WeakHashMap<>();

    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns an ObservableList which can be use to modify the contents of the SplitPane.
     * The order the nodes are placed into this list will be the same order in the SplitPane.
     *
     * @return the list of items in this SplitPane.
     */
    public ObservableList<Node> getItems() {
        return items;
    }

    /**
     * Returns an unmodifiable list of all the dividers in this SplitPane.
     *
     * @return the list of dividers.
     */
    public ObservableList<Divider> getDividers() {
        return unmodifiableDividers;
    }

    /**
     * Sets the position of the divider at the specified divider index.
     *
     * @param dividerIndex the index of the divider.
     * @param position the divider position, between 0.0 and 1.0 (inclusive).
     */
    public void setDividerPosition(int dividerIndex, double position) {
        if (getDividers().size() <= dividerIndex)  {
            dividerCache.put(dividerIndex, position);
            return;
        }
        if (dividerIndex >= 0) {
            getDividers().get(dividerIndex).setPosition(position);
        }
    }

    /**
     * Sets the position of the divider
     *
     * @param positions the divider position, between 0.0 and 1.0 (inclusive).
     */
    public void setDividerPositions(double... positions) {
        if (dividers.isEmpty()) {
            for (int i = 0; i < positions.length; i++) {
                dividerCache.put(i, positions[i]);
            }
            return;
        }
        for (int i = 0; i < positions.length && i < dividers.size(); i++) {
            dividers.get(i).setPosition(positions[i]);
        }
    }

    /**
     * Returns an array of double containing the position of each divider.
     *
     * @return an array of double containing the position of each divider.
     */
    public double[] getDividerPositions() {
        double[] positions = new double[dividers.size()];
        for (int i = 0; i < dividers.size(); i++) {
            positions[i] = dividers.get(i).getPosition();
        }
        return positions;
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new SplitPaneSkin(this);
    }

    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "split-pane";

    private static class StyleableProperties {
        private static final CssMetaData<SplitPane,Orientation> ORIENTATION =
            new CssMetaData<>("-fx-orientation",
                new EnumConverter<>(Orientation.class),
                Orientation.HORIZONTAL) {

            @Override
            public Orientation getInitialValue(SplitPane node) {
                // A vertical SplitPane should remain vertical
                return node.getOrientation();
            }

            @Override
            public boolean isSettable(SplitPane n) {
                return n.orientation == null || !n.orientation.isBound();
            }

            @Override
            public StyleableProperty<Orientation> getStyleableProperty(SplitPane n) {
                return (StyleableProperty<Orientation>)(WritableValue<Orientation>)n.orientationProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(Control.getClassCssMetaData());
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


    /* *************************************************************************
     *                                                                         *
     * Support Classes                                                         *
     *                                                                         *
     **************************************************************************/

    /**
     * Represents a single divider in the SplitPane.
     * @since JavaFX 2.0
     */
    public static class Divider {

        /**
         * Creates a default Divider instance.
         */
        public Divider() {

        }

        /**
         * <p>Represents the location where the divider should ideally be
         * positioned, between 0.0 and 1.0 (inclusive). 0.0 represents the
         * left- or top-most point, and 1.0 represents the right- or bottom-most
         * point (depending on the horizontal property). The SplitPane will attempt
         * to get the divider to the point requested, but it must take into account
         * the minimum width/height of the nodes contained within it.</p>
         *
         * <p>As the user drags the SplitPane divider around this property will
         * be updated to always represent its current location.</p>
         *
         * @defaultValue 0.5
         */
        private DoubleProperty position;
        public final void setPosition(double value) {
            positionProperty().set(value);
        }

        public final double getPosition() {
            return position == null ? 0.5F : position.get();
        }

        public final DoubleProperty positionProperty() {
            if (position == null) {
                position = new SimpleDoubleProperty(this, "position", 0.5F);// {
//                    @Override protected void invalidated() {
//                        if (get() < 0) {
//                            this.value = value;
//                        } else if (get() > 1) {
//                            this.value = value;
//                        }
//                    }
//                };
            }
            return position;
        }
    }
}
