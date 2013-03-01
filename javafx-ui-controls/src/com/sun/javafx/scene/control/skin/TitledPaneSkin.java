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
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Labeled;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Font;

public class TitledPaneSkin extends LabeledSkinBase<TitledPane, TitledPaneBehavior>  {

    public static final int MIN_HEADER_HEIGHT = 22;
    public static final Duration TRANSITION_DURATION = new Duration(350.0);

    private final TitleRegion titleRegion;
    private final ContentContainer contentContainer;
    private final Content contentRegion;
    private Timeline timeline;
    private double transitionStartValue;
    private Rectangle clipRect;
    private Pos pos;
    private HPos hpos;
    private VPos vpos;

    public TitledPaneSkin(final TitledPane titledPane) {
        super(titledPane, new TitledPaneBehavior(titledPane));

        clipRect = new Rectangle();
        getSkinnable().setClip(clipRect);

        transitionStartValue = 0;
        titleRegion = new TitleRegion();

        contentRegion = new Content(getSkinnable().getContent());
        contentContainer = new ContentContainer();
        contentContainer.getChildren().setAll(contentRegion);

        if (titledPane.isExpanded()) {
            setExpanded(titledPane.isExpanded());
        } else {
            setTransition(0.0f);
            if (contentRegion.getContent() != null) {
                contentRegion.getContent().setVisible(false);
            }
        }

        getChildren().setAll(contentContainer, titleRegion);

        registerChangeListener(titledPane.contentProperty(), "CONTENT");
        registerChangeListener(titledPane.expandedProperty(), "EXPANDED");
        registerChangeListener(titledPane.collapsibleProperty(), "COLLAPSIBLE");
        registerChangeListener(titledPane.alignmentProperty(), "ALIGNMENT");
        registerChangeListener(titledPane.widthProperty(), "WIDTH");
        registerChangeListener(titledPane.heightProperty(), "HEIGHT");
        registerChangeListener(titleRegion.alignmentProperty(), "TITLE_REGION_ALIGNMENT");

        pos = titledPane.getAlignment();
        hpos = titledPane.getAlignment().getHpos();
        vpos = titledPane.getAlignment().getVpos();
    }

    public StackPane getContentRegion() {
        return contentRegion;
    }

    @Override
    protected void handleControlPropertyChanged(String property) {
        super.handleControlPropertyChanged(property);
        if ("CONTENT".equals(property)) {
            contentRegion.setContent(getSkinnable().getContent());
        } else if ("EXPANDED".equals(property)) {
            setExpanded(getSkinnable().isExpanded());
        } else if ("COLLAPSIBLE".equals(property)) {
            titleRegion.update();
        } else if ("ALIGNMENT".equals(property)) {
            pos = getSkinnable().getAlignment();
            hpos = pos.getHpos();
            vpos = pos.getVpos();
        } else if ("TITLE_REGION_ALIGNMENT".equals(property)) {
            pos = titleRegion.getAlignment();
            hpos = pos.getHpos();
            vpos = pos.getVpos();
        } else if ("WIDTH".equals(property)) {
            clipRect.setWidth(getSkinnable().getWidth());
        } else if ("HEIGHT".equals(property)) {
            clipRect.setHeight(getSkinnable().getHeight());
        } else if ("GRAPHIC_TEXT_GAP".equals(property)) {
            titleRegion.requestLayout();            
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
            if (contentRegion.getContent() != null) {
                contentRegion.getContent().setVisible(expanded);
             }
            getSkinnable().requestLayout();
        }
    }

    private DoubleProperty transition;
    private void setTransition(double value) { transitionProperty().set(value); }
    private double getTransition() { return transition == null ? 0.0 : transition.get(); }
    private DoubleProperty transitionProperty() {
        if (transition == null) {
            transition = new DoublePropertyBase() {
                @Override protected void invalidated() {
                    getSkinnable().requestLayout();
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

    private boolean isInsideAccordion() {
        return getSkinnable().getParent() != null && getSkinnable().getParent() instanceof Accordion;
    }

    @Override protected void layoutChildren(final double x, double y,
            final double w, final double h) {
        final Insets padding = getSkinnable().getInsets();
        
        // header
        double headerHeight = Math.max(MIN_HEADER_HEIGHT, snapSize(titleRegion.prefHeight(-1)));

        titleRegion.resize(w, headerHeight);
        positionInArea(titleRegion, snapSize(padding.getLeft()), snapSize(padding.getTop()),
            w, headerHeight, 0, HPos.LEFT, VPos.CENTER);

        // content
        double contentWidth = w;
        double contentHeight = h - headerHeight;
        if (isInsideAccordion()) {
            if (prefHeightFromAccordion != 0) {
                contentHeight = prefHeightFromAccordion - headerHeight;
            }
        }

        y = snapSize(padding.getTop()) + snapSize(headerHeight) - (contentHeight * (1 - getTransition()));
        double clipY = contentHeight * (1 - getTransition());
        ((Rectangle)contentContainer.getClip()).setY(clipY);

        contentContainer.resize(contentWidth, contentHeight);
        positionInArea(contentContainer, snapSize(padding.getLeft()), y,
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
        double contentWidth = snapSize(contentContainer.prefWidth(height));
        final Insets padding = getSkinnable().getInsets();
        return Math.max(titleWidth, contentWidth) + snapSize(padding.getLeft()) + snapSize(padding.getRight());
    }

    @Override protected double computePrefHeight(double width) {
        double headerHeight = Math.max(MIN_HEADER_HEIGHT, snapSize(titleRegion.prefHeight(-1)));
        double contentHeight = contentContainer.prefHeight(-1) * getTransition();
        final Insets padding = getSkinnable().getInsets();
        return headerHeight + snapSize(contentHeight) + snapSize(padding.getTop()) + snapSize(padding.getBottom());
    }

    @Override protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    private double prefHeightFromAccordion = 0;
    void setMaxTitledPaneHeightForAccordion(double height) {
        this.prefHeightFromAccordion = height;
    }

    double getTitledPaneHeightForAccordion() {
        double headerHeight = Math.max(MIN_HEADER_HEIGHT, snapSize(titleRegion.prefHeight(-1)));
        double contentHeight = (prefHeightFromAccordion - headerHeight) * getTransition();
        final Insets padding = getSkinnable().getInsets();
        return headerHeight + snapSize(contentHeight) + snapSize(padding.getTop()) + snapSize(padding.getBottom());
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
            
            // RT-13294: TitledPane : add animation to the title arrow
            arrow.rotateProperty().bind(new DoubleBinding() {
                { bind(transitionProperty()); }

                @Override protected double computeValue() {
                    return -90 * (1.0 - getTransition());
                }
            });

            setAlignment(Pos.CENTER_LEFT);

            setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent e) {
                    if( e.getButton() != MouseButton.PRIMARY ) return;
                    ContextMenu contextMenu = getSkinnable().getContextMenu() ;
                    if (contextMenu != null) {
                        contextMenu.hide() ;
                    }
                    if (getSkinnable().isCollapsible() && getSkinnable().isFocused()) {
                        getBehavior().toggle();
                    }
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
            final Insets padding = getInsets();
            double left = snapSize(padding.getLeft());
            double right = snapSize(padding.getRight());
            double arrowWidth = 0;
            double labelPrefWidth = labelPrefWidth(height);

            if (arrowRegion != null) {
                arrowWidth = snapSize(arrowRegion.prefWidth(height));
            }

            return left + arrowWidth + labelPrefWidth + right;
        }

        @Override protected double computePrefHeight(double width) {
            final Insets padding = getInsets();
            double top = snapSize(padding.getTop());
            double bottom = snapSize(padding.getBottom());
            double arrowHeight = 0;
            double labelPrefHeight = labelPrefHeight(width);

            if (arrowRegion != null) {
                arrowHeight = snapSize(arrowRegion.prefHeight(width));
            }

            return top + Math.max(arrowHeight, labelPrefHeight) + bottom;
        }

        @Override protected void layoutChildren() {
            final Insets padding = getInsets();
            double top = snapSize(padding.getTop());
            double bottom = snapSize(padding.getBottom());
            double left = snapSize(padding.getLeft());
            double right = snapSize(padding.getRight());
            double width = getWidth() - (left + right);
            double height = getHeight() - (top + bottom);
            double arrowWidth = snapSize(arrowRegion.prefWidth(-1));
            double arrowHeight = snapSize(arrowRegion.prefHeight(-1));
            double labelWidth = snapSize(labelPrefWidth(-1));
            double labelHeight = snapSize(labelPrefHeight(-1));

            double x = left + arrowWidth + Utils.computeXOffset(width - arrowWidth, labelWidth, hpos);
            if (HPos.CENTER == hpos) {
                // We want to center the region based on the entire width of the TitledPane.
                x = left + Utils.computeXOffset(width, labelWidth, hpos);
            }
            double y = top + Utils.computeYOffset(height, Math.max(arrowHeight, labelHeight), vpos);

            arrowRegion.resize(arrowWidth, arrowHeight);
            positionInArea(arrowRegion, left, top, arrowWidth, height,
                    /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);

            layoutLabelInArea(x, y, labelWidth, height, pos);
        }

        // Copied from LabeledSkinBase because the padding from TitledPane was being
        // applied to the Label when it should not be.
        private double labelPrefWidth(double height) {
            // Get the preferred width of the text
            final Labeled labeled = getSkinnable();
            final Font font = text.getFont();
            final String string = labeled.getText();
            boolean emptyText = string == null || string.isEmpty();
            Insets labelPadding = labeled.getLabelPadding();
            double widthPadding = labelPadding.getLeft() + labelPadding.getRight();
            double textWidth = emptyText ? 0 : Utils.computeTextWidth(font, string, 0);

            // Now add on the graphic, gap, and padding as appropriate
            final Node graphic = labeled.getGraphic();
            if (isIgnoreGraphic()) {
                return textWidth + widthPadding;
            } else if (isIgnoreText()) {
                return graphic.prefWidth(-1) + widthPadding;
            } else if (labeled.getContentDisplay() == ContentDisplay.LEFT
                    || labeled.getContentDisplay() == ContentDisplay.RIGHT) {
                return textWidth + labeled.getGraphicTextGap() + graphic.prefWidth(-1) + widthPadding;
            } else {
                return Math.max(textWidth, graphic.prefWidth(-1)) + widthPadding;
            }
        }

        // Copied from LabeledSkinBase because the padding from TitledPane was being
        // applied to the Label when it should not be.
        private double labelPrefHeight(double width) {
            final Labeled labeled = getSkinnable();
            final Font font = text.getFont();
            final ContentDisplay contentDisplay = labeled.getContentDisplay();
            final double gap = labeled.getGraphicTextGap();
            final Insets padding = labeled.getInsets();
            final Insets labelPadding = labeled.getLabelPadding();
            final double widthPadding = padding.getLeft() + padding.getRight() + labelPadding.getLeft() + labelPadding.getRight();

            String str = labeled.getText();
            if (str != null && str.endsWith("\n")) {
                // Strip ending newline so we don't count another row.
                str = str.substring(0, str.length() - 1);
            }

            if (!isIgnoreGraphic() &&
                (contentDisplay == ContentDisplay.LEFT || contentDisplay == ContentDisplay.RIGHT)) {
                width -= (graphic.prefWidth(-1) + gap);
            }

            width -= widthPadding;

            // TODO figure out how to cache this effectively.
            final double textHeight = Utils.computeTextHeight(font, str,
                                                            labeled.isWrapText() ? width : 0, text.getBoundsType());

            // Now we want to add on the graphic if necessary!
            double h = textHeight;
            if (!isIgnoreGraphic()) {
                final Node graphic = labeled.getGraphic();
                if (contentDisplay == ContentDisplay.TOP || contentDisplay == ContentDisplay.BOTTOM) {
                    h = graphic.prefHeight(-1) + gap + textHeight;
                } else {
                    h = Math.max(textHeight, graphic.prefHeight(-1));
                }
            }

            return h + labelPadding.getTop() + labelPadding.getBottom();
        }
    }

    class Content extends StackPane implements TraverseListener {
        private Node content;
        private TraversalEngine engine;
        private Direction direction;

        public Content(Node n) {
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
                content.setVisible(getSkinnable().isExpanded());
                getChildren().setAll(n);
            }
        }

        public final Node getContent() {
            return content;
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
                if (isInsideAccordion()) {
                    new TraversalEngine(getSkinnable(), false).trav(getSkinnable().getParent(), Direction.NEXT);
                }
            }
        }
    }

    static class ContentContainer extends StackPane {
        private Rectangle clipRect;

        public ContentContainer() {
            getStyleClass().setAll("content");
            clipRect = new Rectangle();
            setClip(clipRect);

            // RT-20266: We want to align the content container so the bottom of the content
            // is at the bottom of the title region.  If we do not do this the
            // content will be center aligned.
            setAlignment(Pos.BOTTOM_CENTER);
        }

        @Override protected void setWidth(double value) {
            super.setWidth(value);
            clipRect.setWidth(value);
        }

        @Override protected void setHeight(double value) {
            super.setHeight(value);
            clipRect.setHeight(value);
        }
    }
}
