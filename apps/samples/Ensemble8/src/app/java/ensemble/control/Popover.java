/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.control;

import java.util.LinkedList;

import ensemble.EnsembleApp;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * A Popover is a mini-window that pops up and contains some application specific content.
 * It's width is defined by the application, but defaults to a hard-coded pref width.
 * The height will always be between a minimum height (determined by the application, but
 * pre-set with a minimum value) and a maximum height (specified by the application, or
 * based on the height of the scene). The value for the pref height is determined by
 * inspecting the pref height of the current displayed page. At time this value is animated
 * (when switching from page to page).
 */
public class Popover extends Region implements EventHandler<Event>{
    private static final int PAGE_GAP = 15;

    /**
     * The visual frame of the popover is defined as an addition region, rather than simply styling
     * the popover itself as one might expect. The reason for this is that our frame is styled via
     * a border image, and it has an inner shadow associated with it, and we want to be able to ensure
     * that the shadow is on top of whatever content is drawn within the popover. In addition, the inner
     * edge of the frame is rounded, and we want the content to slide under it, only to be clipped beneath
     * the frame. So it works best for the frame to be overlaid on top, even though it is not intuitive.
     */
    private final Region frameBorder = new Region();
    private final Button leftButton = new Button("Left");
    private final Button rightButton = new Button("Right");
    private final LinkedList<Page> pages = new LinkedList<Page>();
    private final Pane pagesPane = new Pane();
    private final Rectangle pagesClipRect = new Rectangle();
    private final Pane titlesPane = new Pane();
    private Text title; // the current title
    private final Rectangle titlesClipRect = new Rectangle();
//    private final EventHandler<ScrollEvent> popoverScrollHandler;
    private final EventHandler<MouseEvent> popoverHideHandler;
    private Runnable onHideCallback = null;
    private int maxPopupHeight = -1;

    private DoubleProperty popoverHeight = new SimpleDoubleProperty(400) {
        @Override protected void invalidated() {
            requestLayout();
        }
    };

    public Popover() {
        // TODO Could pagesPane be a region instead? I need to draw some opaque background. Right now when
        // TODO animating from one page to another you can see the background "shine through" because the
        // TODO group background is transparent. That can't be good for performance either.
        getStyleClass().setAll("popover");
        frameBorder.getStyleClass().setAll("popover-frame");
        frameBorder.setMouseTransparent(true);
        // setup buttons
        leftButton.setOnMouseClicked(this);
        leftButton.getStyleClass().add("popover-left-button");
        leftButton.setMinWidth(USE_PREF_SIZE);
        rightButton.setOnMouseClicked(this);
        rightButton.getStyleClass().add("popover-right-button");
        rightButton.setMinWidth(USE_PREF_SIZE);
        pagesClipRect.setSmooth(false);
        pagesPane.setClip(pagesClipRect);
        titlesClipRect.setSmooth(false);
        titlesPane.setClip(titlesClipRect);
        getChildren().addAll(pagesPane, frameBorder, titlesPane, leftButton, rightButton);
        // always hide to start with
        setVisible(false);
        setOpacity(0);
        setScaleX(.8);
        setScaleY(.8);
        // create handlers for auto hiding
        popoverHideHandler = (MouseEvent t) -> {
            // check if event is outside popup
            Point2D mouseInFilterPane = sceneToLocal(t.getX(), t.getY());
            if (mouseInFilterPane.getX() < 0 || mouseInFilterPane.getX() > (getWidth()) ||
                    mouseInFilterPane.getY() < 0 || mouseInFilterPane.getY() > (getHeight())) {
                hide();
                t.consume();
            }
        };
//        popoverScrollHandler = new EventHandler<ScrollEvent>() {
//            @Override public void handle(ScrollEvent t) {
//                t.consume(); // consume all scroll events
//            }
//        };
    }

    /**
     * Handle mouse clicks on the left and right buttons.
     */
    @Override public void handle(Event event) {
        if (event.getSource() == leftButton) {
            pages.getFirst().handleLeftButton();
        } else if (event.getSource() == rightButton) {
            pages.getFirst().handleRightButton();
        }
    }

    @Override protected double computeMinWidth(double height) {
        Page page = pages.isEmpty() ? null : pages.getFirst();
        if (page != null) {
            Node n = page.getPageNode();
            if (n != null) {
                Insets insets = getInsets();
                return insets.getLeft() + n.minWidth(-1) + insets.getRight();
            }
        }
        return 200;
    }

    @Override protected double computeMinHeight(double width) {
        Insets insets = getInsets();
        return insets.getLeft() + 100 + insets.getRight();
    }

    @Override protected double computePrefWidth(double height) {
        Page page = pages.isEmpty() ? null : pages.getFirst();
        if (page != null) {
            Node n = page.getPageNode();
            if (n != null) {
                Insets insets = getInsets();
                return insets.getLeft() + n.prefWidth(-1) + insets.getRight();
            }
        }
        return 400;
    }

    @Override protected double computePrefHeight(double width) {
        double minHeight = minHeight(-1);
        double maxHeight = maxHeight(-1);
        double prefHeight = popoverHeight.get();
        if (prefHeight == -1) {
            Page page = pages.getFirst();
            if (page != null) {
                Insets inset = getInsets();
                if (width == -1) {
                    width = prefWidth(-1);
                }
                double contentWidth = width - inset.getLeft() - inset.getRight();
                double contentHeight = page.getPageNode().prefHeight(contentWidth);
                prefHeight = inset.getTop() + contentHeight + inset.getBottom();
                popoverHeight.set(prefHeight);
            } else {
                prefHeight = minHeight;
            }
        }
        return boundedSize(minHeight, prefHeight, maxHeight);
    }

    static double boundedSize(double min, double pref, double max) {
        double a = pref >= min ? pref : min;
        double b = min >= max ? min : max;
        return a <= b ? a : b;
    }

    @Override protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override protected double computeMaxHeight(double width) {
        Scene scene = getScene();
        if (scene != null) {
            return scene.getHeight() - 100;
        } else {
            return Double.MAX_VALUE;
        }
    }

    @Override protected void layoutChildren() {
        if (maxPopupHeight == -1) {
            maxPopupHeight = (int)getScene().getHeight()-100;
        }
        final Insets insets = getInsets();
        final int width = (int)getWidth();
        final int height = (int)getHeight();
        final int top = (int)insets.getTop();
        final int right = (int)insets.getRight();
        final int bottom = (int)insets.getBottom();
        final int left = (int)insets.getLeft();

        int pageWidth = width - left - right;
        int pageHeight = height - top - bottom;

        frameBorder.resize(width, height);

        pagesPane.resizeRelocate(left, top, pageWidth, pageHeight);
        pagesClipRect.setWidth(pageWidth);
        pagesClipRect.setHeight(pageHeight);

        int pageX = 0;
        for (Node page : pagesPane.getChildren()) {
            page.resizeRelocate(pageX, 0, pageWidth, pageHeight);
            pageX += pageWidth + PAGE_GAP;
        }

        int buttonHeight = (int)(leftButton.prefHeight(-1));
        if (buttonHeight < 30) buttonHeight = 30;
        final int buttonTop = (int)((top-buttonHeight)/2d);
        final int leftButtonWidth = (int)snapSize(leftButton.prefWidth(-1));
        leftButton.resizeRelocate(left, buttonTop,leftButtonWidth,buttonHeight);
        final int rightButtonWidth = (int)snapSize(rightButton.prefWidth(-1));
        rightButton.resizeRelocate(width-right-rightButtonWidth, buttonTop,rightButtonWidth,buttonHeight);

        final double leftButtonRight = leftButton.isVisible() ? (left + leftButtonWidth) : left;
        final double rightButtonLeft = rightButton.isVisible() ? (right + rightButtonWidth) : right;
        titlesClipRect.setX(leftButtonRight);
        titlesClipRect.setWidth(pageWidth - leftButtonRight - rightButtonLeft);
        titlesClipRect.setHeight(top);

        if (title != null) {
            title.setTranslateY((int) (top / 2d));
        }
    }

    public final void clearPages() {
        while (!pages.isEmpty()) {
            pages.pop().handleHidden();
        }
        pagesPane.getChildren().clear();
        titlesPane.getChildren().clear();
        pagesClipRect.setX(0);
        pagesClipRect.setWidth(400);
        pagesClipRect.setHeight(400);
        popoverHeight.set(400);
        pagesPane.setTranslateX(0);
        titlesPane.setTranslateX(0);
        titlesClipRect.setTranslateX(0);
    }

    public final void popPage() {
        Page oldPage = pages.pop();
        oldPage.handleHidden();
        oldPage.setPopover(null);
        Page page = pages.getFirst();
        leftButton.setVisible(page.leftButtonText() != null);
        leftButton.setText(page.leftButtonText());
        rightButton.setVisible(page.rightButtonText() != null);
        rightButton.setText(page.rightButtonText());
        if (pages.size() > 0) {
            final Insets insets = getInsets();
            final int width = (int)prefWidth(-1);
            final int right = (int)insets.getRight();
            final int left = (int)insets.getLeft();
            int pageWidth = width - left - right;
            final int newPageX = (pageWidth+PAGE_GAP) * (pages.size()-1);
            new Timeline(
                    new KeyFrame(Duration.millis(350), (ActionEvent t) -> {
                        pagesPane.setCache(false);
                        pagesPane.getChildren().remove(pagesPane.getChildren().size()-1);
                        titlesPane.getChildren().remove(titlesPane.getChildren().size()-1);
                        resizePopoverToNewPage(pages.getFirst().getPageNode());
            },
                        new KeyValue(pagesPane.translateXProperty(), -newPageX, Interpolator.EASE_BOTH),
                        new KeyValue(titlesPane.translateXProperty(), -newPageX, Interpolator.EASE_BOTH),
                        new KeyValue(pagesClipRect.xProperty(), newPageX, Interpolator.EASE_BOTH),
                        new KeyValue(titlesClipRect.translateXProperty(), newPageX, Interpolator.EASE_BOTH)
                    )
                ).play();
        } else {
            hide();
        }
    }

    public final void pushPage(final Page page) {
        final Node pageNode = page.getPageNode();
        pageNode.setManaged(false);
        pagesPane.getChildren().add(pageNode);
        final Insets insets = getInsets();
        final int pageWidth = (int)(prefWidth(-1) - insets.getLeft() - insets.getRight());
        final int newPageX = (pageWidth + PAGE_GAP) * pages.size();
        leftButton.setVisible(page.leftButtonText() != null);
        leftButton.setText(page.leftButtonText());
        rightButton.setVisible(page.rightButtonText() != null);
        rightButton.setText(page.rightButtonText());

        title = new Text(page.getPageTitle());
        title.getStyleClass().add("popover-title");
        //debtest title.setFill(Color.WHITE);
        title.setTextOrigin(VPos.CENTER);
        title.setTranslateX(newPageX + (int) ((pageWidth - title.getLayoutBounds().getWidth()) / 2d));
        titlesPane.getChildren().add(title);

        if (!pages.isEmpty() && isVisible()) {
            final Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(350), (ActionEvent t) -> {
                        pagesPane.setCache(false);
                        resizePopoverToNewPage(pageNode);
            },
                        new KeyValue(pagesPane.translateXProperty(), -newPageX, Interpolator.EASE_BOTH),
                        new KeyValue(titlesPane.translateXProperty(), -newPageX, Interpolator.EASE_BOTH),
                        new KeyValue(pagesClipRect.xProperty(), newPageX, Interpolator.EASE_BOTH),
                        new KeyValue(titlesClipRect.translateXProperty(), newPageX, Interpolator.EASE_BOTH)
                    )
                );
            timeline.play();
        }
        page.setPopover(this);
        page.handleShown();
        pages.push(page);
    }

    private void resizePopoverToNewPage(final Node newPageNode) {
        final Insets insets = getInsets();
        final double width = prefWidth(-1);
        final double contentWidth = width - insets.getLeft() - insets.getRight();
        double h = newPageNode.prefHeight(contentWidth);
        h += insets.getTop() + insets.getBottom();
        new Timeline(
            new KeyFrame(Duration.millis(200),
                new KeyValue(popoverHeight, h, Interpolator.EASE_BOTH)
            )
        ).play();
    }

    public void show(){
        show(null);
    }

    private Animation fadeAnimation = null;

    public void show(Runnable onHideCallback){
        if (!isVisible() || fadeAnimation != null) {
            this.onHideCallback = onHideCallback;
            getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, popoverHideHandler);
//            getScene().addEventFilter(ScrollEvent.ANY,popoverScrollHandler);

            if (fadeAnimation != null) {
                fadeAnimation.stop();
                setVisible(true); // for good measure
            } else {
                popoverHeight.set(-1);
                setVisible(true);
            }

            FadeTransition fade = new FadeTransition(Duration.seconds(.1), this);
            fade.setToValue(1.0);
            fade.setOnFinished((ActionEvent event) -> {
                fadeAnimation = null;
            });

            ScaleTransition scale = new ScaleTransition(Duration.seconds(.1), this);
            scale.setToX(1);
            scale.setToY(1);

            ParallelTransition tx = new ParallelTransition(fade, scale);
            fadeAnimation = tx;
            tx.play();
        }
    }

    public void hide(){
        if (isVisible() || fadeAnimation != null) {
            getScene().removeEventFilter(MouseEvent.MOUSE_CLICKED, popoverHideHandler);
//            getScene().removeEventFilter(ScrollEvent.ANY,popoverScrollHandler);

            if (fadeAnimation != null) {
                fadeAnimation.stop();
            }

            FadeTransition fade = new FadeTransition(Duration.seconds(.1), this);
            fade.setToValue(0);
            fade.setOnFinished((ActionEvent event) -> {
                fadeAnimation = null;
                setVisible(false);
                clearPages();
                if (onHideCallback != null) onHideCallback.run();
            });

            ScaleTransition scale = new ScaleTransition(Duration.seconds(.1), this);
            scale.setToX(.8);
            scale.setToY(.8);

            ParallelTransition tx = new ParallelTransition(fade, scale);
            fadeAnimation = tx;
            tx.play();
        }
    }

    /**
     * Represents a page in a popover.
     */
    public static interface Page {
        public void setPopover(Popover popover);
        public Popover getPopover();

        /**
         * Get the node that represents the page.
         *
         * @return the page node.
         */
        public Node getPageNode();

        /**
         * Get the title to display for this page.
         *
         * @return The page title
         */
        public String getPageTitle();

        /**
         * The text for left button, if null then button will be hidden.
         * @return The button text
         */
        public String leftButtonText();

        /**
         * Called on a click of the left button of the popover.
         */
        public void handleLeftButton();

        /**
         * The text for right button, if null then button will be hidden.
         * @return The button text
         */
        public String rightButtonText();

        /**
         * Called on a click of the right button of the popover.
         */
        public void handleRightButton();

        public void handleShown();
        public void handleHidden();
    }
}
