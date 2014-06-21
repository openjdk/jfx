/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;
import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import com.sun.javafx.scene.control.behavior.TitledPaneBehavior;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Labeled;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Font;

public class TitledPaneSkin extends LabeledSkinBase<TitledPane, TitledPaneBehavior>  {

    public static final Duration TRANSITION_DURATION = new Duration(350.0);

    // caching results in poorer looking text (it is blurry), so we don't do it
    // unless on a low powered device (admittedly the test below isn't a great
    // indicator of power, but it'll do for now).
    private static final boolean CACHE_ANIMATION = PlatformUtil.isEmbedded();

    private final TitleRegion titleRegion;
    private final StackPane contentContainer;
    private Node content;
    private Timeline timeline;
    private double transitionStartValue;
    private Rectangle clipRect;
    private Pos pos;
    private HPos hpos;
    private VPos vpos;

    public TitledPaneSkin(final TitledPane titledPane) {
        super(titledPane, new TitledPaneBehavior(titledPane));

        clipRect = new Rectangle();

        transitionStartValue = 0;
        titleRegion = new TitleRegion();

        content = getSkinnable().getContent();
        contentContainer = new StackPane() {
            {
                getStyleClass().setAll("content");

                if (content != null) {
                    getChildren().setAll(content);
                }
            }
        };
        contentContainer.setClip(clipRect);

        if (titledPane.isExpanded()) {
            setTransition(1.0f);
            setExpanded(titledPane.isExpanded());
        } else {
            setTransition(0.0f);
            if (content != null) {
                content.setVisible(false);
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
        hpos = pos == null ? HPos.LEFT   : pos.getHpos();
        vpos = pos == null ? VPos.CENTER : pos.getVpos();
    }

    public StackPane getContentContainer() {
        return contentContainer;
    }

    @Override
    protected void handleControlPropertyChanged(String property) {
        super.handleControlPropertyChanged(property);
        if ("CONTENT".equals(property)) {
            content = getSkinnable().getContent();
            if (content == null) {
                contentContainer.getChildren().clear();
            } else {
                contentContainer.getChildren().setAll(content);
            }
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
            clipRect.setHeight(contentContainer.getHeight());
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
            if (content != null) {
                content.setVisible(expanded);
             }
            getSkinnable().requestLayout();
        }
    }

    private DoubleProperty transition;
    private void setTransition(double value) { transitionProperty().set(value); }
    private double getTransition() { return transition == null ? 0.0 : transition.get(); }
    private DoubleProperty transitionProperty() {
        if (transition == null) {
            transition = new SimpleDoubleProperty(this, "transition", 0.0) {
                @Override protected void invalidated() {
                    contentContainer.requestLayout();
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
        
        // header
        double headerHeight = snapSize(titleRegion.prefHeight(-1));

        titleRegion.resize(w, headerHeight);
        positionInArea(titleRegion, x, y,
            w, headerHeight, 0, HPos.LEFT, VPos.CENTER);

        // content
        double contentHeight = (h - headerHeight) * getTransition();
        if (isInsideAccordion()) {
            if (prefHeightFromAccordion != 0) {
                contentHeight = (prefHeightFromAccordion - headerHeight) * getTransition();
            }
        }
        contentHeight = snapSize(contentHeight);

        y += snapSize(headerHeight);
        contentContainer.resize(w, contentHeight);
        clipRect.setHeight(contentHeight);
        positionInArea(contentContainer, x, y,
            w, contentHeight, /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
    }

    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double titleWidth = snapSize(titleRegion.prefWidth(height));
        double contentWidth = snapSize(contentContainer.minWidth(height));
        return Math.max(titleWidth, contentWidth) + leftInset + rightInset;
    }

    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double headerHeight = snapSize(titleRegion.prefHeight(width));
        double contentHeight = contentContainer.minHeight(width) * getTransition();
        return headerHeight + snapSize(contentHeight) + topInset + bottomInset;
    }

    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double titleWidth = snapSize(titleRegion.prefWidth(height));
        double contentWidth = snapSize(contentContainer.prefWidth(height));
        return Math.max(titleWidth, contentWidth) + leftInset + rightInset;
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double headerHeight = snapSize(titleRegion.prefHeight(width));
        double contentHeight = contentContainer.prefHeight(width) * getTransition();
        return headerHeight + snapSize(contentHeight) + topInset + bottomInset;
    }

    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Double.MAX_VALUE;
    }

    double getTitleRegionSize(double width) {
        return snapSize(titleRegion.prefHeight(width)) + snappedTopInset() + snappedBottomInset();
    }

    private double prefHeightFromAccordion = 0;
    void setMaxTitledPaneHeightForAccordion(double height) {
        this.prefHeightFromAccordion = height;
    }

    double getTitledPaneHeightForAccordion() {
        double headerHeight = snapSize(titleRegion.prefHeight(-1));
        double contentHeight = (prefHeightFromAccordion - headerHeight) * getTransition();
        return headerHeight + snapSize(contentHeight) + snappedTopInset() + snappedBottomInset();
    }

    private void doAnimationTransition() {
        if (content == null) {
            return;
        }

        Duration duration;
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
                    event -> {
                        // start expand
                        if (CACHE_ANIMATION) content.setCache(true);
                        content.setVisible(true);
                    },
                new KeyValue(transitionProperty(), transitionStartValue)
            );

            k2 = new KeyFrame(
                duration,
                    event -> {
                        // end expand
                        if (CACHE_ANIMATION) content.setCache(false);
                    },
                new KeyValue(transitionProperty(), 1, Interpolator.LINEAR)

            );
        } else {
            k1 = new KeyFrame(
                Duration.ZERO,
                    event -> {
                        // Start collapse
                        if (CACHE_ANIMATION) content.setCache(true);
                    },
                new KeyValue(transitionProperty(), transitionStartValue)
            );

            k2 = new KeyFrame(
                duration,
                    event -> {
                        // end collapse
                        content.setVisible(false);
                        if (CACHE_ANIMATION) content.setCache(false);
                    },
                new KeyValue(transitionProperty(), 0, Interpolator.LINEAR)
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

            setOnMouseReleased(e -> {
                if( e.getButton() != MouseButton.PRIMARY ) return;
                ContextMenu contextMenu = getSkinnable().getContextMenu() ;
                if (contextMenu != null) {
                    contextMenu.hide() ;
                }
                if (getSkinnable().isCollapsible() && getSkinnable().isFocused()) {
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
            double left = snappedLeftInset();
            double right = snappedRightInset();
            double arrowWidth = 0;
            double labelPrefWidth = labelPrefWidth(height);

            if (arrowRegion != null) {
                arrowWidth = snapSize(arrowRegion.prefWidth(height));
            }

            return left + arrowWidth + labelPrefWidth + right;
        }

        @Override protected double computePrefHeight(double width) {
            double top = snappedTopInset();
            double bottom = snappedBottomInset();
            double arrowHeight = 0;
            double labelPrefHeight = labelPrefHeight(width);

            if (arrowRegion != null) {
                arrowHeight = snapSize(arrowRegion.prefHeight(width));
            }

            return top + Math.max(arrowHeight, labelPrefHeight) + bottom;
        }

        @Override protected void layoutChildren() {
            final double top = snappedTopInset();
            final double bottom = snappedBottomInset();
            final double left = snappedLeftInset();
            final double right = snappedRightInset();
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
            final Insets labelPadding = labeled.getLabelPadding();
            final double widthPadding = snappedLeftInset() + snappedRightInset() + labelPadding.getLeft() + labelPadding.getRight();

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
}
