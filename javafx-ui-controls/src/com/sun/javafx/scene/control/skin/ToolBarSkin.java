/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SkinBase;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.CssMetaData;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.behavior.ToolBarBehavior;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.scene.traversal.TraverseListener;
import javafx.css.Styleable;
import javafx.geometry.Insets;

public class ToolBarSkin extends BehaviorSkinBase<ToolBar, ToolBarBehavior> implements TraverseListener {

    private Pane box;
    private ToolBarOverflowMenu overflowMenu;
    private boolean overflow = false;
    private double previousWidth = 0;
    private double previousHeight = 0;
    private double savedPrefWidth = 0;
    private double savedPrefHeight = 0;
    private ObservableList<MenuItem> overflowMenuItems;
    private boolean needsUpdate = false;
    private TraversalEngine engine;
    private Direction direction;

    public ToolBarSkin(ToolBar toolbar) {
        super(toolbar, new ToolBarBehavior(toolbar));
        overflowMenuItems = FXCollections.observableArrayList();
        initialize();
        registerChangeListener(toolbar.orientationProperty(), "ORIENTATION");

        engine = new TraversalEngine(getSkinnable(), false) {
            @Override public void trav(Node owner, Direction dir) {
                // This allows the right arrow to select the overflow menu
                // without it only the tab key can select the overflow menu.
                if (overflow) {
                    engine.reg(overflowMenu);
                }
                direction = dir;
                super.trav(owner, dir);
                if (overflow) {
                    engine.unreg(overflowMenu);
                }
            }
        };
        engine.addTraverseListener(this);
        getSkinnable().setImpl_traversalEngine(engine);

        toolbar.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    // TODO need to detect the focus direction
                    // to selected the first control in the toolbar when TAB is pressed
                    // or select the last control in the toolbar when SHIFT TAB is pressed.
                    if (!box.getChildren().isEmpty()) {
                        box.getChildren().get(0).requestFocus();
                    } else {
                        overflowMenu.requestFocus();
                    }
                }
            }
        });

        toolbar.getItems().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> c) {
                while (c.next()) {
                    for (Node n: c.getRemoved()) {
                        box.getChildren().remove(n);
                    }
                    box.getChildren().addAll(c.getAddedSubList());
                }
                needsUpdate = true;
                getSkinnable().requestLayout();
            }
        });        
    }

    private DoubleProperty spacing;
    public final void setSpacing(double value) {
        spacingProperty().set(snapSpace(value));
    }

    public final double getSpacing() {
        return spacing == null ? 0.0 : snapSpace(spacing.get());
    }

    public final DoubleProperty spacingProperty() {
        if (spacing == null) {
            spacing = new StyleableDoubleProperty() {

                @Override
                protected void invalidated() {
                    final double value = get();
                    if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
                        ((VBox)box).setSpacing(value);
                    } else {
                        ((HBox)box).setSpacing(value);
                    }
                }

                @Override
                public Object getBean() {
                    return ToolBarSkin.this;
                }

                @Override
                public String getName() {
                    return "spacing";
                }

                @Override
                public CssMetaData<ToolBar,Number> getCssMetaData() {
                    return StyleableProperties.SPACING;
                }
            };
        }
        return spacing;
    }

    private ObjectProperty<Pos> boxAlignment;
    public final void setBoxAlignment(Pos value) {
        boxAlignmentProperty().set(value);
    }

    public final Pos getBoxAlignment() {
        return boxAlignment == null ? Pos.TOP_LEFT : boxAlignment.get();
    }

    public final ObjectProperty<Pos> boxAlignmentProperty() {
        if (boxAlignment == null) {
            boxAlignment = new StyleableObjectProperty<Pos>(Pos.TOP_LEFT) {

                @Override
                public void invalidated() {
                    final Pos value = get();
                    if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
                        ((VBox)box).setAlignment(value);
                    } else {
                        ((HBox)box).setAlignment(value);
                    }
                }

                @Override
                public Object getBean() {
                    return ToolBarSkin.this;
                }

                @Override
                public String getName() {
                    return "boxAlignment";
                }

                @Override
                public CssMetaData<ToolBar,Pos> getCssMetaData() {
                    return StyleableProperties.ALIGNMENT;
                }
            };
        }
        return boxAlignment;
    }

    @Override protected void handleControlPropertyChanged(String property) {
        super.handleControlPropertyChanged(property);
        if ("ORIENTATION".equals(property)) {
            initialize();
        }
    }

    @Override protected double computeMinWidth(double height) {
        final ToolBar toolbar = getSkinnable();
        final Insets padding = toolbar.getInsets();
        return toolbar.getOrientation() == Orientation.VERTICAL ?
            computePrefWidth(-1) :
            snapSize(overflowMenu.prefWidth(-1)) + snapSpace(padding.getLeft()) + snapSpace(padding.getRight());
    }

    @Override protected double computeMinHeight(double width) {
        final ToolBar toolbar = getSkinnable();
        final Insets padding = toolbar.getInsets();
        return toolbar.getOrientation() == Orientation.VERTICAL?
            snapSize(overflowMenu.prefHeight(-1)) + snapSpace(padding.getTop()) + snapSpace(padding.getBottom()) :
            computePrefHeight(-1);
    }

    @Override protected double computePrefWidth(double height) {
        double prefWidth = 0;
        final ToolBar toolbar = getSkinnable();
        final Insets padding = toolbar.getInsets();

        if (toolbar.getOrientation() == Orientation.HORIZONTAL) {
            for (Node node : toolbar.getItems()) {
                prefWidth += snapSize(node.prefWidth(-1)) + getSpacing();
            }
            prefWidth -= getSpacing();
        } else {
            for (Node node : toolbar.getItems()) {
                prefWidth = Math.max(prefWidth, snapSize(node.prefWidth(-1)));
            }
            if (toolbar.getItems().size() > 0) {
                savedPrefWidth = prefWidth;
            } else {
                prefWidth = savedPrefWidth;
            }
        }
        return snapSpace(padding.getLeft()) + prefWidth + snapSpace(padding.getRight());
    }

    @Override protected double computePrefHeight(double width) {
        double prefHeight = 0;
        final ToolBar toolbar = getSkinnable();
        final Insets padding = toolbar.getInsets();
        
        if(toolbar.getOrientation() == Orientation.VERTICAL) {
            for (Node node: toolbar.getItems()) {
                prefHeight += snapSize(node.prefHeight(-1)) + getSpacing();
            }
            prefHeight -= getSpacing();
        } else {
            for (Node node : toolbar.getItems()) {
                prefHeight = Math.max(prefHeight, snapSize(node.prefHeight(-1)));
            }
            if (toolbar.getItems().size() > 0) {
                savedPrefHeight = prefHeight;
            } else {
                prefHeight = savedPrefHeight;
            }
        }
        return snapSpace(padding.getTop()) + prefHeight + snapSpace(padding.getBottom());
    }

    @Override protected double computeMaxWidth(double height) {
        return getSkinnable().getOrientation() == Orientation.VERTICAL ?
                snapSize(getSkinnable().prefWidth(-1)) : Double.MAX_VALUE;
    }

    @Override protected double computeMaxHeight(double width) {
        return getSkinnable().getOrientation() == Orientation.VERTICAL ?
                Double.MAX_VALUE : snapSize(getSkinnable().prefHeight(-1));
    }

    @Override protected void layoutChildren(double x, double y,
            final double w, final double h) {
//        super.layoutChildren();
        final ToolBar toolbar = getSkinnable();
        final Insets padding = toolbar.getInsets();

        if (toolbar.getOrientation() == Orientation.VERTICAL) {
            if (snapSize(toolbar.getHeight()) != previousHeight || needsUpdate) {
                ((VBox)box).setSpacing(getSpacing());
                ((VBox)box).setAlignment(getBoxAlignment());
                previousHeight = snapSize(toolbar.getHeight());
                addNodesToToolBar();
            }
        } else {
            if (snapSize(toolbar.getWidth()) != previousWidth || needsUpdate) {
                ((HBox)box).setSpacing(getSpacing());
                ((HBox)box).setAlignment(getBoxAlignment());
                previousWidth = snapSize(toolbar.getWidth());
                addNodesToToolBar();
            }
        }
        needsUpdate = false;

        double toolbarWidth = snapSize(toolbar.getWidth()) - (snapSpace(padding.getLeft()) + snapSpace(padding.getRight()));
        double toolbarHeight = snapSize(toolbar.getHeight()) - (snapSpace(padding.getTop()) + snapSpace(padding.getBottom()));

        if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
            toolbarHeight -= (overflow ? snapSize(overflowMenu.prefHeight(-1)) : 0);
        } else {
            toolbarWidth -= (overflow ? snapSize(overflowMenu.prefWidth(-1)) : 0);
        }

        box.resize(toolbarWidth, toolbarHeight);
        positionInArea(box, snapSpace(padding.getLeft()), snapSpace(padding.getTop()),
                toolbarWidth, toolbarHeight, /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);

        // If popup menu is not null show the overflowControl
        if (overflow) {
            double overflowMenuWidth = snapSize(overflowMenu.prefWidth(-1));
            double overflowMenuHeight = snapSize(overflowMenu.prefHeight(-1));
            if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
                // This is to prevent the overflow menu from moving when there
                // are no items in the toolbar.
                if (toolbarWidth == 0) {
                    toolbarWidth = savedPrefWidth;
                }
                HPos pos = ((VBox)box).getAlignment().getHpos();
                if (HPos.LEFT.equals(pos)) {
                    x = snapSpace(padding.getLeft()) +
                        Math.abs((toolbarWidth - overflowMenuWidth)/2);
                } else if (HPos.RIGHT.equals(pos)) {
                    x = (snapSize(toolbar.getWidth()) - snapSpace(padding.getRight()) - toolbarWidth) +
                        Math.abs((toolbarWidth - overflowMenuWidth)/2);
                } else {
                    x = snapSpace(padding.getLeft()) +
                        Math.abs((snapSize(toolbar.getWidth()) - (snapSpace(padding.getLeft()) +
                        snapSpace(padding.getRight())) - overflowMenuWidth)/2);
                }
                y = snapSize(toolbar.getHeight()) - overflowMenuHeight - snapSpace(padding.getTop());
            } else {
                // This is to prevent the overflow menu from moving when there
                // are no items in the toolbar.
                if (toolbarHeight == 0) {
                    toolbarHeight = savedPrefHeight;
                }
                VPos pos = ((HBox)box).getAlignment().getVpos();
                if (VPos.TOP.equals(pos)) {
                    y = snapSpace(padding.getTop()) +
                        Math.abs((toolbarHeight - overflowMenuHeight)/2);
                } else if (VPos.BOTTOM.equals(pos)) {
                    y = (snapSize(toolbar.getHeight()) - snapSpace(padding.getBottom()) - toolbarHeight) +
                        Math.abs((toolbarHeight - overflowMenuHeight)/2);
                } else {
                    y = snapSpace(padding.getTop()) +
                        Math.abs((snapSize(toolbar.getHeight()) - (snapSpace(padding.getTop()) +
                        snapSpace(padding.getBottom())) - overflowMenuHeight)/2);
                }
                x = snapSize(toolbar.getWidth()) - overflowMenuWidth - snapSpace(padding.getRight());
            }
            overflowMenu.resize(overflowMenuWidth, overflowMenuHeight);
            positionInArea(overflowMenu, x, y, overflowMenuWidth, overflowMenuHeight, /*baseline ignored*/0,
                    HPos.CENTER, VPos.CENTER);
        }
    }

    private void initialize() {
        if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
            box = new VBox();
        } else {
            box = new HBox();
        }
        box.getStyleClass().add("container");
        box.getChildren().addAll(getSkinnable().getItems());
        overflowMenu = new ToolBarOverflowMenu(overflowMenuItems);
        overflowMenu.setVisible(false);
        overflowMenu.setManaged(false);

        getChildren().clear();
        getChildren().add(box);
        getChildren().add(overflowMenu);

        previousWidth = 0;
        previousHeight = 0;
        savedPrefWidth = 0;
        savedPrefHeight = 0;
        needsUpdate = true;
        getSkinnable().requestLayout();
    }

    private void addNodesToToolBar() {
        final ToolBar toolbar = getSkinnable();
        final Insets padding = toolbar.getInsets();
        double length = 0;
        if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
            length = snapSize(toolbar.getHeight()) - (snapSpace(padding.getTop()) + snapSpace(padding.getBottom())) + getSpacing();
        } else {
            length = snapSize(toolbar.getWidth()) - (snapSpace(padding.getLeft()) + snapSpace(padding.getRight())) + getSpacing();
        }

        // Is there overflow ?
        double x = 0;
        boolean hasOverflow = false;
        for (Node node : getSkinnable().getItems()) {
            if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
                x += snapSize(node.prefHeight(-1)) + getSpacing();
            } else {
                x += snapSize(node.prefWidth(-1)) + getSpacing();
            }
            if (x > length) {
                hasOverflow = true;
                break;
            }
        }

        if (hasOverflow) {
            if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
                length -= snapSize(overflowMenu.prefHeight(-1));
            } else {
                length -= snapSize(overflowMenu.prefWidth(-1));
            }
            length -= getSpacing();
        }

        // Determine which node goes to the toolbar and which goes to the overflow.
        x = 0;
        overflowMenuItems.clear();
        box.getChildren().clear();
        for (Node node : getSkinnable().getItems()) {
            node.getStyleClass().remove("menu-item");
            node.getStyleClass().remove("custom-menu-item");
            if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
                x += snapSize(node.prefHeight(-1)) + getSpacing();
            } else {
                x += snapSize(node.prefWidth(-1)) + getSpacing();
            }
            if (x <= length) {
                box.getChildren().add(node);
            } else {
                if (node.isFocused()) {
                    if (!box.getChildren().isEmpty()) {
                        engine.registeredNodes.get(engine.registeredNodes.size() - 1).requestFocus();
                    } else {
                        overflowMenu.requestFocus();                        
                    }
                }
                if (node instanceof Separator) {
                    overflowMenuItems.add(new SeparatorMenuItem());
                } else {
                    overflowMenuItems.add(new CustomMenuItem(node));
                }
            }
        }

        // Check if we overflowed.
        overflow = overflowMenuItems.size() > 0;
        if (!overflow && overflowMenu.isFocused()) {
            engine.registeredNodes.get(engine.registeredNodes.size() - 1).requestFocus();
        }
        overflowMenu.setVisible(overflow);
        overflowMenu.setManaged(overflow);
    }

    @Override
    public void onTraverse(Node node, Bounds bounds) {
        int index = engine.registeredNodes.indexOf(node);
        if (index == -1 && direction.equals(Direction.NEXT)) {
            if (overflow) {
                overflowMenu.requestFocus();
            }
        }
    }

    class ToolBarOverflowMenu extends StackPane {
        private StackPane downArrow;
        private ContextMenu popup;
        private ObservableList<MenuItem> menuItems;

        public ToolBarOverflowMenu(ObservableList<MenuItem> items) {
            getStyleClass().setAll("tool-bar-overflow-button");
            this.menuItems = items;
            downArrow = new StackPane();
            downArrow.getStyleClass().setAll("arrow");
            downArrow.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent me) {                    
                    if (popup.isShowing()) {
                        popup.hide();
                    } else {
                        popup.getItems().clear();
                        popup.getItems().addAll(menuItems);
                        popup.show(downArrow, Side.BOTTOM, 0, 0);
                    }
                }
            });

            setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override public void handle(KeyEvent ke) {                    
                    if (KeyCode.SPACE.equals(ke.getCode())) {
                        if (!popup.isShowing()) {
                            popup.getItems().clear();
                            popup.getItems().addAll(menuItems);
                            popup.show(downArrow, Side.BOTTOM, 0, 0);
                        }
                    } else if (KeyCode.ESCAPE.equals(ke.getCode())) {
                        if (popup.isShowing()) {
                            popup.hide();
                        }
                    } else if (KeyCode.ENTER.equals(ke.getCode())) {
                        if (popup.isShowing()) {
                            popup.hide();
                        } else {
                            popup.getItems().clear();
                            popup.getItems().addAll(menuItems);
                            popup.show(downArrow, Side.BOTTOM, 0, 0);
                        }
                    } else {
                        boolean isRTL = getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT;
                        boolean leftAction = KeyCode.LEFT.equals(ke.getCode());
                        boolean rightAction = KeyCode.RIGHT.equals(ke.getCode()) || KeyCode.TAB.equals(ke.getCode());
                        if (isRTL) {
                            boolean swap = leftAction;
                            leftAction = rightAction;
                            rightAction = swap;
                        }
                        if (KeyCode.UP.equals(ke.getCode()) || leftAction ||
                            (KeyCode.TAB.equals(ke.getCode()) && ke.isShiftDown())) {
                            if (engine.registeredNodes.isEmpty()) {
                                return;
                            }
                            int index = box.getChildren().indexOf(engine.registeredNodes.get(engine.registeredNodes.size() - 1));
                            if (index != -1) {
                                box.getChildren().get(index).requestFocus();                            
                            } else {
                                if (!box.getChildren().isEmpty()) {
                                    box.getChildren().get(0).requestFocus();
                                } else {
                                    new TraversalEngine(getSkinnable(), false).trav(getSkinnable(), Direction.PREVIOUS);
                                }
                            }
                        } else if (KeyCode.DOWN.equals(ke.getCode()) || rightAction) {                        
                            new TraversalEngine(getSkinnable(), false).trav(getSkinnable(), Direction.NEXT);
                        }
                    }
                    ke.consume();
                }
            });

            focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(
                        ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {                    
                    if (newValue) {
                        if (!popup.isShowing()) {
                            popup.getItems().clear();
                            popup.getItems().addAll(menuItems);
                            popup.show(downArrow, Side.BOTTOM, 0, 0);
                        }
                    } else {
                        if (popup.isShowing()) {
                            popup.hide();
                        } 
                    }
                }
            });

            visibleProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(
                        ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                        if (newValue) {
                            if (box.getChildren().isEmpty()) {
                                setFocusTraversable(true);
                            }
                        }
                }
            });
            popup = new ContextMenu();
            setVisible(false);
            setManaged(false);            
            getChildren().add(downArrow);            
        }

        @Override protected double computePrefWidth(double height) {
            return snapSize(getInsets().getLeft()) + snapSize(getInsets().getRight());
        }

        @Override protected double computePrefHeight(double width) {
            return snapSize(getInsets().getTop()) + snapSize(getInsets().getBottom());
        }

        @Override protected void layoutChildren() {
            double w = snapSize(downArrow.prefWidth(-1));
            double h = snapSize(downArrow.prefHeight(-1));
            double x = (snapSize(getWidth()) - w)/2;
            double y = (snapSize(getHeight()) - h)/2;

            // TODO need to provide support for when the toolbar is on the right
            // or bottom
            if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
                downArrow.setRotate(0);
            }

            downArrow.resize(w, h);
            positionInArea(downArrow, x, y, w, h,
                    /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
        }
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {
         private static final CssMetaData<ToolBar,Number> SPACING =
             new CssMetaData<ToolBar,Number>("-fx-spacing",
                 SizeConverter.getInstance(), 0.0) {

            @Override
            public boolean isSettable(ToolBar n) {
                final ToolBarSkin skin = (ToolBarSkin) n.getSkin();
                return skin.spacing == null || !skin.spacing.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(ToolBar n) {
                final ToolBarSkin skin = (ToolBarSkin) n.getSkin();
                return (StyleableProperty<Number>)skin.spacingProperty();
            }
        };
         
        private static final CssMetaData<ToolBar,Pos>ALIGNMENT =
                new CssMetaData<ToolBar,Pos>("-fx-alignment",
                new EnumConverter<Pos>(Pos.class), Pos.TOP_LEFT ) {

            @Override
            public boolean isSettable(ToolBar n) {
                final ToolBarSkin skin = (ToolBarSkin) n.getSkin();
                return skin.boxAlignment == null || !skin.boxAlignment.isBound();
            }

            @Override
            public StyleableProperty<Pos> getStyleableProperty(ToolBar n) {
                final ToolBarSkin skin = (ToolBarSkin) n.getSkin();
                return (StyleableProperty<Pos>)skin.boxAlignmentProperty();
            }
        };

         
         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(SkinBase.getClassCssMetaData());
            
            // StackPane also has -fx-alignment. Replace it with 
            // ToolBarSkin's. 
            // TODO: Really should be able to reference StackPane.StyleableProperties.ALIGNMENT
            final String alignmentProperty = ALIGNMENT.getProperty();
            for (int n=0, nMax=styleables.size(); n<nMax; n++) {
                final CssMetaData<?,?> prop = styleables.get(n);
                if (alignmentProperty.equals(prop.getProperty())) styleables.remove(prop);
            }
            
            styleables.add(SPACING);
            styleables.add(ALIGNMENT);
            STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

}
