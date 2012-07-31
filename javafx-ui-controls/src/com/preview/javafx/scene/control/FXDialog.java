/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.preview.javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import com.sun.javafx.css.StyleManager;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javafx.geometry.Pos;

class FXDialog extends Stage {
    
    // Use the CSS supplied with the JavaFX runtime, rather than our local tweak
     private static final URL DIALOGS_CSS_URL = FXDialog.class.getResource("dialogs.css");   
//    private static final URL DIALOGS_CSS_URL = FXAboutDialog.class.getResource("deploydialogs.css");
    
    private BorderPane root;
    private RootPane decoratedRoot;
    private HBox windowBtns;
    private Button minButton;
    private Button maxButton;
    private Rectangle resizeCorner;
    private double mouseDragOffsetX = 0;
    private double mouseDragOffsetY = 0;
    protected Label titleLabel;

    private static final int HEADER_HEIGHT = 28;

    FXDialog(String title) {
        this(title, null, false);
    }

    FXDialog(String title, Window owner, boolean modal) {
        this(title, owner, modal, StageStyle.TRANSPARENT);
    }

    FXDialog(String title, Window owner, boolean modal, StageStyle stageStyle) {
        super(stageStyle);

        setTitle(title);

        if (owner != null) {
            initOwner(owner);
        }

        if (modal) {
            initModality(Modality.WINDOW_MODAL);
        }

        resizableProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                resizeCorner.setVisible(isResizable());
                maxButton.setVisible(isResizable());
                
                if (isResizable()) {
                    windowBtns.getChildren().add(1, maxButton);
                } else {
                    windowBtns.getChildren().remove(maxButton);
                }
            }
        });

        root = new BorderPane();

        Scene scene;

        if (stageStyle == StageStyle.DECORATED) {
            scene = new Scene(root);
            scene.getStylesheets().addAll(DIALOGS_CSS_URL.toExternalForm());
            setScene(scene);
            return;
        }



        // *** The rest is for adding window decorations ***

        decoratedRoot = new RootPane() {
            @Override protected void layoutChildren() {
                super.layoutChildren();
                if (resizeCorner != null) {
                    resizeCorner.relocate(getWidth() - 20, getHeight() - 20);
                }
            }
        };
        decoratedRoot.getChildren().add(root);
        scene = new Scene(decoratedRoot);
        String css = (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override public Object run() {
                return DIALOGS_CSS_URL.toExternalForm();
            }
        });
        scene.getStylesheets().addAll(css);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);

        decoratedRoot.getStyleClass().addAll("dialog", "decorated-root");

        focusedProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                decoratedRoot.pseudoClassStateChanged("active");
            }
        });

        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("window-header");
        toolBar.setPrefHeight(HEADER_HEIGHT);
        toolBar.setMinHeight(HEADER_HEIGHT);
        toolBar.setMaxHeight(HEADER_HEIGHT);

        // add window dragging
        toolBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                mouseDragOffsetX = event.getSceneX();
                mouseDragOffsetY = event.getSceneY();
            }
        });
        toolBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                setX(event.getScreenX() - mouseDragOffsetX);
                setY(event.getScreenY() - mouseDragOffsetY);
            }
        });

        titleLabel = new Label();
        titleLabel.getStyleClass().add("window-title");
        titleLabel.setText(getTitle());

        titleProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                titleLabel.setText(getTitle());
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // add close min max
        Button closeButton = new WindowButton("close");
        closeButton.setOnAction(new EventHandler() {
            @Override public void handle(Event event) {
                FXDialog.this.hide();
            }
        });
        minButton = new WindowButton("minimize");
        minButton.setOnAction(new EventHandler() {
            @Override public void handle(Event event) {
                setIconified(!isIconified());
            }
        });

        maxButton = new WindowButton("maximize");
        maxButton.setOnAction(new EventHandler() {
            private double restoreX;
            private double restoreY;
            private double restoreW;
            private double restoreH;

            @Override public void handle(Event event) {
                Screen screen = Screen.getPrimary(); // todo something more sensible
                double minX = screen.getVisualBounds().getMinX();
                double minY = screen.getVisualBounds().getMinY();
                double maxW = screen.getVisualBounds().getWidth();
                double maxH = screen.getVisualBounds().getHeight();

                if (restoreW == 0 || getX() != minX || getY() != minY || getWidth() != maxW || getHeight() != maxH) {
                    restoreX = getX();
                    restoreY = getY();
                    restoreW = getWidth();
                    restoreH = getHeight();
                    setX(minX);
                    setY(minY);
                    setWidth(maxW);
                    setHeight(maxH);
                } else {
                    setX(restoreX);
                    setY(restoreY);
                    setWidth(restoreW);
                    setHeight(restoreH);
                }
            }
        });

        windowBtns = new HBox(3);
        windowBtns.getStyleClass().add("window-buttons");
        windowBtns.getChildren().addAll(minButton, maxButton, closeButton);

        toolBar.getItems().addAll(titleLabel, spacer, windowBtns);
        root.setTop(toolBar);

        resizeCorner = new Rectangle(10, 10);
        resizeCorner.getStyleClass().add("window-resize-corner");

        // add window resizing
        EventHandler<MouseEvent> resizeHandler = new EventHandler<MouseEvent>() {
            private double width;
            private double height;
            private Point2D dragAnchor;

            @Override public void handle(MouseEvent event) {
                EventType type = event.getEventType();

                if (type == MouseEvent.MOUSE_PRESSED) {
                    width = getWidth();
                    height = getHeight();
                    dragAnchor = new Point2D(event.getSceneX(), event.getSceneY());
                } else if (type == MouseEvent.MOUSE_DRAGGED) {
                    setWidth(Math.max(decoratedRoot.minWidth(-1),   width  + (event.getSceneX() - dragAnchor.getX())));
                    setHeight(Math.max(decoratedRoot.minHeight(-1), height + (event.getSceneY() - dragAnchor.getY())));
                }
            }
        };
        resizeCorner.setOnMousePressed(resizeHandler);
        resizeCorner.setOnMouseDragged(resizeHandler);

        resizeCorner.setManaged(false);
        decoratedRoot.getChildren().add(resizeCorner);
    }

    void setContentPane(Pane pane) {
        if (pane.getId() == null) {
            pane.getStyleClass().add("content-pane");
        }
        root.setCenter(pane);
    }

    public void setIconifiable(boolean iconifiable) {
        minButton.setVisible(iconifiable);
    }

    private static class WindowButton extends Button {
        WindowButton(String name) {
            getStyleClass().setAll("window-button");
            getStyleClass().add("window-"+name+"-button");
            StackPane graphic = new StackPane();
            graphic.getStyleClass().setAll("graphic");
            setGraphic(graphic);
            setMinSize(17, 17);
            setPrefSize(17, 17);
        }
    }

    private static class RootPane extends StackPane {
        /***************************************************************************
         *                                                                         *
         * Stylesheet Handling                                                     *
         *                                                                         *
         **************************************************************************/

        private static final long PSEUDO_CLASS_ACTIVE_MASK = StyleManager.getInstance().getPseudoclassMask("active");

        @Override public long impl_getPseudoClassState() {
            long mask = super.impl_getPseudoClassState();
            if (getScene().getWindow().isFocused()) {
                mask |= PSEUDO_CLASS_ACTIVE_MASK;
            }
            return mask;
        }

        private void pseudoClassStateChanged(String pseudoClass) {
            impl_pseudoClassStateChanged(pseudoClass);
        }
    }
}

