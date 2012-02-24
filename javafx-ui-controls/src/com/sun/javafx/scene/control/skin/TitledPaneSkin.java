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

import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import com.sun.javafx.scene.control.behavior.TitledPaneBehavior;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.scene.traversal.TraverseListener;

public class TitledPaneSkin extends LabeledSkinBase<TitledPane, TitledPaneBehavior>  {

    public static final int MIN_HEADER_HEIGHT = 22;
    public static final Duration TRANSITION_DURATION = new Duration(350.0);

    private final TitleRegion titleRegion;
    private final Content contentRegion;
    private Timeline timeline;
    private double transitionStartValue;
    private Rectangle clipRect;

    public TitledPaneSkin(final TitledPane titledPane) {
        super(titledPane, new TitledPaneBehavior(titledPane));

        clipRect = new Rectangle();
        setClip(clipRect);

        transitionStartValue = 0;
        titleRegion = new TitleRegion();

        contentRegion = new Content(getSkinnable().getContent());

        if (titledPane.isExpanded()) {
            setExpanded(titledPane.isExpanded());
        } else {
            setTransition(0.0f);
        }

        getChildren().setAll(contentRegion, titleRegion);

        registerChangeListener(titledPane.contentProperty(), "CONTENT");
        registerChangeListener(titledPane.expandedProperty(), "EXPANDED");
        registerChangeListener(titledPane.collapsibleProperty(), "COLLAPSIBLE");
    }

    public StackPane getContentRegion() {
        return contentRegion;
    }

    @Override protected void setWidth(double value) {
        super.setWidth(value);
        clipRect.setWidth(value);
    }

    @Override protected void setHeight(double value) {
        super.setHeight(value);
        clipRect.setHeight(value);
    }

    @Override
    protected void handleControlPropertyChanged(String property) {
        super.handleControlPropertyChanged(property);
        if (property == "CONTENT") {
            contentRegion.setContent(getSkinnable().getContent());
        } else if (property == "EXPANDED") {
            setExpanded(getSkinnable().isExpanded());
        } else if (property == "COLLAPSIBLE") {
            titleRegion.update();
        }
    }

    // Override LabeledSkinBase updateChildren because
    // it removes all the children.  The update() in TitleRegion
    // will replace this method.
    @Override protected void updateChildren() {
        if (titleRegion != null) {
            titleRegion.update();
        }
    }

    private void setExpanded(boolean expanded) {
        if (! getSkinnable().isCollapsible()) {
            setTransition(1.0f);
            return;
        }

        // we need to perform the transition between expanded / hidden
        if (getSkinnable().isAnimated()) {
            transitionStartValue = getTransition();
            doAnimationTransition();
        } else {
            if (expanded) {
                setTransition(1.0f);
            } else {
                setTransition(0.0f);
            }
            contentRegion.setVisible(expanded);
            requestLayout();
        }
    }

    private DoubleProperty transition;
    private void setTransition(double value) { transitionProperty().set(value); }
    private double getTransition() { return transition == null ? 0.0 : transition.get(); }
    private DoubleProperty transitionProperty() {
        if (transition == null) {
            transition = new DoublePropertyBase() {
                @Override protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return TitledPaneSkin.this;
                }

                @Override
                public String getName() {
                    return "transition";
                }
            };
        }
        return transition;
    }

    @Override protected void layoutChildren() {
        double w = snapSize(getWidth()) - (snapSpace(getInsets().getLeft()) + snapSpace(getInsets().getRight()));
        double h = snapSize(getHeight()) - (snapSpace(getInsets().getTop()) + snapSpace(getInsets().getBottom()));

        // header
        double headerHeight = Math.max(MIN_HEADER_HEIGHT, snapSize(titleRegion.prefHeight(-1)));

        titleRegion.resize(w, headerHeight);
        positionInArea(titleRegion, snapSpace(getInsets().getLeft()), snapSpace(getInsets().getTop()),
            w, headerHeight, 0, HPos.LEFT, VPos.CENTER);

        // content
        double contentWidth = w;
        double contentHeight = h - headerHeight;
        if (getSkinnable().getParent() != null && getSkinnable().getParent() instanceof AccordionSkin) {
            if (prefHeightFromAccordion != 0) {
                contentHeight = prefHeightFromAccordion - headerHeight;
            }
        }

        contentRegion.resize(contentWidth, contentHeight);
        positionInArea(contentRegion, snapSpace(getInsets().getLeft()), snapSpace(headerHeight),
            w, contentHeight, /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
    }

    @Override protected double computeMinWidth(double height) {
        return computePrefWidth(height);
    }

    @Override protected double computeMinHeight(double width) {
        return Math.max(MIN_HEADER_HEIGHT, snapSize(titleRegion.prefHeight(-1)));
    }

    @Override protected double computePrefWidth(double height) {
        double titleWidth = snapSize(titleRegion.prefWidth(height));
        double contentWidth = snapSize(contentRegion.prefWidth(height));

        return Math.max(titleWidth, contentWidth) + snapSpace(getInsets().getLeft()) + snapSpace(getInsets().getRight());
    }

    @Override protected double computePrefHeight(double width) {
        double headerHeight = Math.max(MIN_HEADER_HEIGHT, snapSize(titleRegion.prefHeight(-1)));
        double contentHeight = 0;
        if (getSkinnable().getParent() != null && getSkinnable().getParent() instanceof AccordionSkin) {
            contentHeight = contentRegion.prefHeight(-1);
        } else {
            contentHeight = contentRegion.prefHeight(-1) * getTransition();
        }
        return headerHeight + snapSize(contentHeight) + snapSpace(getInsets().getTop()) + snapSpace(getInsets().getBottom());
    }

    private double prefHeightFromAccordion = 0;
    void setMaxTitledPaneHeightForAccordion(double height) {
        this.prefHeightFromAccordion = height;
    }

    double getTitledPaneHeightForAccordion() {
        double headerHeight = Math.max(MIN_HEADER_HEIGHT, snapSize(titleRegion.prefHeight(-1)));
        double contentHeight = (prefHeightFromAccordion - headerHeight) * getTransition();
        return headerHeight + snapSize(contentHeight) + snapSpace(getInsets().getTop()) + snapSpace(getInsets().getBottom());
    }

    private void doAnimationTransition() {
        Duration duration;

        if (contentRegion.getContent() == null) {
            return;
        }

        if (timeline != null && (timeline.getStatus() != Status.STOPPED)) {
            duration = timeline.getCurrentTime();
            timeline.stop();
        } else {
                duration = TRANSITION_DURATION;
        }

        timeline = new Timeline();
        timeline.setCycleCount(1);

        KeyFrame k1, k2;

        if (getSkinnable().isExpanded()) {
            k1 = new KeyFrame(
                Duration.ZERO,
                new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent event) {
                        // start expand
                        contentRegion.getContent().setCache(true);
                        contentRegion.getContent().setVisible(true);
                    }
                },
                new KeyValue(transitionProperty(), transitionStartValue)
            );

            k2 = new KeyFrame(
                duration,
                    new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent event) {
                        // end expand
                        contentRegion.getContent().setCache(false);
                    }
                },
                new KeyValue(transitionProperty(), 1, Interpolator.EASE_OUT)

            );
        } else {
            k1 = new KeyFrame(
                Duration.ZERO,
                new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent event) {
                        // Start collapse
                        contentRegion.getContent().setCache(true);
                    }
                },
                new KeyValue(transitionProperty(), transitionStartValue)
            );

            k2 = new KeyFrame(
                duration,
                new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent event) {
                        // end collapse
                        contentRegion.getContent().setVisible(false);
                        contentRegion.getContent().setCache(false);
                    }
                },
                new KeyValue(transitionProperty(), 0, Interpolator.EASE_IN)
            );
        }

        timeline.getKeyFrames().setAll(k1, k2);
        timeline.play();
    }

    class TitleRegion extends StackPane {
        private final StackPane arrowRegion;

        public TitleRegion() {
            getStyleClass().setAll("title");
            arrowRegion = new StackPane();
            arrowRegion.setId("arrowRegion");
            arrowRegion.getStyleClass().setAll("arrow-button");

            StackPane arrow = new StackPane();
            arrow.setId("arrow");
            arrow.getStyleClass().setAll("arrow");
            arrowRegion.getChildren().setAll(arrow);

            setAlignment(Pos.CENTER_LEFT);

            setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent e) {
                    getBehavior().toggle();
                 }
            });

            // title region consists of the title and the arrow regions
            update();
        }

        private void update() {
            getChildren().clear();
            final TitledPane titledPane = getSkinnable();

            if (titledPane.isCollapsible()) {
                getChildren().add(arrowRegion);
            }

            // Only in some situations do we want to have the graphicPropertyChangedListener
            // installed. Since updateChildren() is not called much, we'll just remove it always
            // and reinstall it later if it is necessary to do so.
            if (graphic != null) {
                graphic.layoutBoundsProperty().removeListener(graphicPropertyChangedListener);
            }
            // Now update the graphic (since it may have changed)
            graphic = titledPane.getGraphic();
            // Now update the children (and add the graphicPropertyChangedListener as necessary)
            if (isIgnoreGraphic()) {
                if (titledPane.getContentDisplay() == ContentDisplay.GRAPHIC_ONLY) {
                    getChildren().clear();
                    getChildren().add(arrowRegion);
                } else {
                    getChildren().add(text);
                }
            } else {
                graphic.layoutBoundsProperty().addListener(graphicPropertyChangedListener);
                if (isIgnoreText()) {
                    getChildren().add(graphic);
                } else {
                    getChildren().addAll(graphic, text);
                }
            }
            setCursor(getSkinnable().isCollapsible() ? Cursor.HAND : Cursor.DEFAULT);
        }

        @Override protected double computePrefWidth(double height) {
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());
            double arrowWidth = 0;
            // We want to use the label's pref width computed by LabeledSkinBase.
            double labelPrefWidth = TitledPaneSkin.super.computePrefWidth(height);

            if (arrowRegion != null) {
                arrowWidth = snapSize(arrowRegion.prefWidth(height));
            }

            return left + arrowWidth + labelPrefWidth + right;
        }

        @Override protected double computePrefHeight(double width) {
            double top = snapSpace(getInsets().getTop());
            double bottom = snapSpace(getInsets().getBottom());
            double arrowHeight = 0;
            // We want to use the label's pref height computed by LabeledSkinBase.
            double labelPrefHeight = TitledPaneSkin.super.computePrefHeight(width);

            if (arrowRegion != null) {
                arrowHeight = snapSize(arrowRegion.prefHeight(width));
            }

            return top + Math.max(arrowHeight, labelPrefHeight) + bottom;
        }

        @Override protected void layoutChildren() {
            double top = snapSpace(getInsets().getTop());
            double bottom = snapSpace(getInsets().getBottom());
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());
            double width = getWidth() - (left + right);
            double height = getHeight() - (top + bottom);
            double arrowWidth = snapSize(arrowRegion.prefWidth(-1));
            double arrowHeight = snapSize(arrowRegion.prefHeight(-1));
            double labelWidth = snapSize(TitledPaneSkin.super.computePrefWidth(-1));
            double labelHeight = snapSize(TitledPaneSkin.super.computePrefHeight(-1));

            HPos hpos = getAlignment().getHpos();
            VPos vpos = getAlignment().getVpos();
            double x = left + arrowWidth + Utils.computeXOffset(width - arrowWidth, labelWidth, hpos);
            double y = top + Utils.computeYOffset(height, Math.max(arrowHeight, labelHeight), vpos);

            arrowRegion.resize(arrowWidth, arrowHeight);
            positionInArea(arrowRegion, left, top, arrowWidth, height,
                    /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);

            layoutLabelInArea(x, y, labelWidth, height, getAlignment());
        }
    }

    class Content extends StackPane implements TraverseListener {
        private Node content;
        private Rectangle clipRect;
        private TraversalEngine engine;
        private Direction direction;

        public Content(Node n) {
            getStyleClass().setAll("content");
            this.clipRect = new Rectangle();
            setClip(clipRect);
            setContent(n);

            engine = new TraversalEngine(this, false) {
                @Override public void trav(Node owner, Direction dir) {
                    direction = dir;
                    super.trav(owner, dir);
                }
            };
            engine.addTraverseListener(this);
            setImpl_traversalEngine(engine);
        }

        public final void setContent(Node n) {
            this.content = n;
            getChildren().clear();
            if (n != null) {
                getChildren().setAll(n);
            }
        }

        public final Node getContent() {
            return content;
        }

        @Override protected void setWidth(double value) {
            super.setWidth(value);
            clipRect.setWidth(value);
        }

        @Override protected void setHeight(double value) {
            super.setHeight(value);
            clipRect.setHeight(value);
        }

        @Override
        public void onTraverse(Node node, Bounds bounds) {
            int index = engine.registeredNodes.indexOf(node);

            if (index == -1 && direction.equals(Direction.PREVIOUS)) {
                getSkinnable().requestFocus();
            }
            if (index == -1 && direction.equals(Direction.NEXT)) {
                // If the parent is an accordion we want to focus to go outside of the
                // accordion and to the next focusable control.
                if (getSkinnable().getParent() != null && getSkinnable().getParent() instanceof AccordionSkin) {
                    new TraversalEngine(getSkinnable(), false).trav(getSkinnable().getParent(), Direction.NEXT);
                }
            }
        }
    }
}
