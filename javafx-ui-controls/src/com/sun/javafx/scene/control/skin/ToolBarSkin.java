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

package com.sun.javafx.scene.control.skin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
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

import com.sun.javafx.css.StyleableDoubleProperty;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.StyleablePropertyMetaData;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.behavior.ToolBarBehavior;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.scene.traversal.TraverseListener;

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
                requestLayout();
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
                public StyleablePropertyMetaData getStyleablePropertyMetaData() {
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
                public StyleablePropertyMetaData getStyleablePropertyMetaData() {
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
        return getSkinnable().getOrientation() == Orientation.VERTICAL ?
            computePrefWidth(-1) :
            snapSize(overflowMenu.prefWidth(-1)) + snapSpace(getInsets().getLeft()) + snapSpace(getInsets().getRight());
    }

    @Override protected double computeMinHeight(double width) {
        return getSkinnable().getOrientation() == Orientation.VERTICAL?
            snapSize(overflowMenu.prefHeight(-1)) + snapSpace(getInsets().getTop()) + snapSpace(getInsets().getBottom()) :
            computePrefHeight(-1);
    }

    @Override protected double computePrefWidth(double height) {
        double prefWidth = 0;

        if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
            for (Node node : getSkinnable().getItems()) {
                prefWidth += snapSize(node.prefWidth(-1)) + getSpacing();
            }
            prefWidth -= getSpacing();
        } else {
            for (Node node : getSkinnable().getItems()) {
                prefWidth = Math.max(prefWidth, snapSize(node.prefWidth(-1)));
            }
            if (getSkinnable().getItems().size() > 0) {
                savedPrefWidth = prefWidth;
            } else {
                prefWidth = savedPrefWidth;
            }
        }
        return snapSpace(getInsets().getLeft()) + prefWidth + snapSpace(getInsets().getRight());
    }

    @Override protected double computePrefHeight(double width) {
        double prefHeight = 0;

        if(getSkinnable().getOrientation() == Orientation.VERTICAL) {
            for (Node node: getSkinnable().getItems()) {
                prefHeight += snapSize(node.prefHeight(-1)) + getSpacing();
            }
            prefHeight -= getSpacing();
        } else {
            for (Node node : getSkinnable().getItems()) {
                prefHeight = Math.max(prefHeight, snapSize(node.prefHeight(-1)));
            }
            if (getSkinnable().getItems().size() > 0) {
                savedPrefHeight = prefHeight;
            } else {
                prefHeight = savedPrefHeight;
            }
        }
        return snapSpace(getInsets().getTop()) + prefHeight + snapSpace(getInsets().getBottom());
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

        if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
            if (snapSize(getHeight()) != previousHeight || needsUpdate) {
                ((VBox)box).setSpacing(getSpacing());
                ((VBox)box).setAlignment(getBoxAlignment());
                previousHeight = snapSize(getHeight());
                addNodesToToolBar();
            }
        } else {
            if (snapSize(getWidth()) != previousWidth || needsUpdate) {
                ((HBox)box).setSpacing(getSpacing());
                ((HBox)box).setAlignment(getBoxAlignment());
                previousWidth = snapSize(getWidth());
                addNodesToToolBar();
            }
        }
        needsUpdate = false;

        double toolbarWidth = snapSize(getWidth()) - (snapSpace(getInsets().getLeft()) + snapSpace(getInsets().getRight()));
        double toolbarHeight = snapSize(getHeight()) - (snapSpace(getInsets().getTop()) + snapSpace(getInsets().getBottom()));

        if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
            toolbarHeight -= (overflow ? snapSize(overflowMenu.prefHeight(-1)) : 0);
        } else {
            toolbarWidth -= (overflow ? snapSize(overflowMenu.prefWidth(-1)) : 0);
        }

        box.resize(toolbarWidth, toolbarHeight);
        positionInArea(box, snapSpace(getInsets().getLeft()), snapSpace(getInsets().getTop()),
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
                    x = snapSpace(getInsets().getLeft()) +
                        Math.abs((toolbarWidth - overflowMenuWidth)/2);
                } else if (HPos.RIGHT.equals(pos)) {
                    x = (snapSize(getWidth()) - snapSpace(getInsets().getRight()) - toolbarWidth) +
                        Math.abs((toolbarWidth - overflowMenuWidth)/2);
                } else {
                    x = snapSpace(getInsets().getLeft()) +
                        Math.abs((snapSize(getWidth()) - (snapSpace(getInsets().getLeft()) +
                        snapSpace(getInsets().getRight())) - overflowMenuWidth)/2);
                }
                y = snapSize(getHeight()) - overflowMenuHeight - snapSpace(getInsets().getTop());
            } else {
                // This is to prevent the overflow menu from moving when there
                // are no items in the toolbar.
                if (toolbarHeight == 0) {
                    toolbarHeight = savedPrefHeight;
                }
                VPos pos = ((HBox)box).getAlignment().getVpos();
                if (VPos.TOP.equals(pos)) {
                    y = snapSpace(getInsets().getTop()) +
                        Math.abs((toolbarHeight - overflowMenuHeight)/2);
                } else if (VPos.BOTTOM.equals(pos)) {
                    y = (snapSize(getHeight()) - snapSpace(getInsets().getBottom()) - toolbarHeight) +
                        Math.abs((toolbarHeight - overflowMenuHeight)/2);
                } else {
                    y = snapSpace(getInsets().getTop()) +
                        Math.abs((snapSize(getHeight()) - (snapSpace(getInsets().getTop()) +
                        snapSpace(getInsets().getBottom())) - overflowMenuHeight)/2);
                }
                x = snapSize(getWidth()) - overflowMenuWidth - snapSpace(getInsets().getRight());
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
        requestLayout();
    }

    private void addNodesToToolBar() {
        double length = 0;
        if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
            length = snapSize(getHeight()) - (snapSpace(getInsets().getTop()) + snapSpace(getInsets().getBottom())) + getSpacing();
        } else {
            length = snapSize(getWidth()) - (snapSpace(getInsets().getLeft()) + snapSpace(getInsets().getRight())) + getSpacing();
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
         private static final StyleablePropertyMetaData<ToolBar,Number> SPACING =
             new StyleablePropertyMetaData<ToolBar,Number>("-fx-spacing",
                 SizeConverter.getInstance(), 0.0) {

            @Override
            public boolean isSettable(ToolBar n) {
                final ToolBarSkin skin = (ToolBarSkin) n.getSkin();
                return skin.spacing == null || !skin.spacing.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(ToolBar n) {
                final ToolBarSkin skin = (ToolBarSkin) n.getSkin();
                return skin.spacingProperty();
            }
        };
         
        private static final StyleablePropertyMetaData<ToolBar,Pos>ALIGNMENT =
                new StyleablePropertyMetaData<ToolBar,Pos>("-fx-alignment",
                new EnumConverter<Pos>(Pos.class), Pos.TOP_LEFT ) {

            @Override
            public boolean isSettable(ToolBar n) {
                final ToolBarSkin skin = (ToolBarSkin) n.getSkin();
                return skin.boxAlignment == null || !skin.boxAlignment.isBound();
            }

            @Override
            public WritableValue<Pos> getWritableValue(ToolBar n) {
                final ToolBarSkin skin = (ToolBarSkin) n.getSkin();
                return skin.boxAlignmentProperty();
            }
        };

         
         private static final List<StyleablePropertyMetaData> STYLEABLES;
         static {

            final List<StyleablePropertyMetaData> styleables =
                new ArrayList<StyleablePropertyMetaData>(SkinBase.getClassStyleablePropertyMetaData());
            
            // StackPane also has -fx-alignment. Replace it with 
            // ToolBarSkin's. 
            // TODO: Really should be able to reference StackPane.StyleableProperties.ALIGNMENT
            final String alignmentProperty = ALIGNMENT.getProperty();
            for (int n=0, nMax=styleables.size(); n<nMax; n++) {
                final StyleablePropertyMetaData prop = styleables.get(n);
                if (alignmentProperty.equals(prop.getProperty())) styleables.remove(prop);
            }
            
            Collections.addAll(styleables,
                SPACING, 
                ALIGNMENT
            );
            STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleablePropertyMetaData> getClassStyleablePropertyMetaData() {
        return StyleableProperties.STYLEABLES;
    };

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleablePropertyMetaData> getStyleablePropertyMetaData() {
        return getClassStyleablePropertyMetaData();
    }

}
