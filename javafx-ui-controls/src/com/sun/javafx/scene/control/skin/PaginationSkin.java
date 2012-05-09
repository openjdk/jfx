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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.javafx.scene.control.skin;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.control.Pagination;
import com.sun.javafx.scene.control.behavior.PaginationBehavior;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class PaginationSkin extends SkinBase<Pagination, PaginationBehavior>  {

    private static final Duration DURATION = new Duration(125.0);
    private static final double THRESHOLD = 0.30;

    private Pagination pagination;
    private ScrollPane currentScrollPane;
    private ScrollPane nextScrollPane;
    private Timeline timeline;
    private Rectangle clipRect;

    private NavigationControl navigation;
    private int fromIndex;
    private int previousIndex;
    private int currentIndex;
    private int toIndex;
    private int pageCount;
    private int maxPageIndicatorCount;

    private boolean animate = true;

    public PaginationSkin(final Pagination pagination) {
        super(pagination, new PaginationBehavior(pagination));

        setManaged(false);
        clipRect = new Rectangle();
        setClip(clipRect);

        this.pagination = pagination;
        this.currentScrollPane = new ScrollPane();
        currentScrollPane.getStyleClass().add("page");
        currentScrollPane.setFitToWidth(true);
        currentScrollPane.setFitToHeight(true);
        currentScrollPane.setPannable(false);

        this.nextScrollPane = new ScrollPane();
        nextScrollPane.getStyleClass().add("page");
        nextScrollPane.setFitToWidth(true);
        nextScrollPane.setFitToHeight(true);
        nextScrollPane.setPannable(false);
        nextScrollPane.setVisible(false);

        resetIndexes(true);

        this.navigation = new NavigationControl();

        getChildren().addAll(currentScrollPane, nextScrollPane, navigation);

        pagination.maxPageIndicatorCountProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable o) {
                resetIndexes(false);
                navigation.initializePageIndicators();
                navigation.updatePageIndicators();
            }
        });

        registerChangeListener(pagination.pageCountProperty(), "PAGE_COUNT");
        registerChangeListener(pagination.pageFactoryProperty(), "PAGE_FACTORY");

        initializeSwipeAndTouchHandlers();
    }

    public void selectNext() {
        if (pagination.getCurrentPageIndex() < getPageCount() - 1) {
            pagination.setCurrentPageIndex(pagination.getCurrentPageIndex() + 1);
        }
    }

    public void selectPrevious() {
        if (pagination.getCurrentPageIndex() != 0) {
            pagination.setCurrentPageIndex(pagination.getCurrentPageIndex() - 1);
        }
    }

    private double pressPos;
    private boolean touchMoved = false;
    private void initializeSwipeAndTouchHandlers() {
//        setOnSwipeLeft(new EventHandler<SwipeEvent>() {
//            @Override public void handle(SwipeEvent t) {
//                selectNext();
//            }
//        });
//
//        setOnSwipeRight(new EventHandler<SwipeEvent>() {
//            @Override public void handle(SwipeEvent t) {
//                selectPrevious();
//            }
//        });

        setOnTouchPressed(new EventHandler<TouchEvent>() {
            @Override public void handle(TouchEvent e) {
                pressPos = e.getTouchPoint().getSceneX();
                e.consume();
            }
        });

        setOnTouchMoved(new EventHandler<TouchEvent>() {
            @Override public void handle(TouchEvent e) {
                touchMoved = true;
                double delta = e.getTouchPoint().getSceneX() - pressPos;
                double width = getWidth() - (getInsets().getLeft() + getInsets().getRight());
                double currentScrollPaneX;
                double nextScrollPaneX;

                if (delta < 0) {
                    // right to left
                    if (Math.abs(delta) <= width) {
                        currentScrollPaneX = delta;
                        nextScrollPaneX = width + delta;
                    } else {
                        currentScrollPaneX = -width;
                        nextScrollPaneX = 0;
                    }
                    currentScrollPane.setTranslateX(currentScrollPaneX);
                    if (pagination.getCurrentPageIndex() < getPageCount() - 1) {
                        createPage(nextScrollPane, currentIndex + 1);
                        nextScrollPane.setVisible(true);
                        nextScrollPane.setTranslateX(nextScrollPaneX);
                    } else {
                        currentScrollPane.setTranslateX(0);
                    }
                } else {
                    // left to right
                    if (Math.abs(delta) <= width) {
                        currentScrollPaneX = delta;
                        nextScrollPaneX = -width + delta;
                    } else {
                        currentScrollPaneX = width;
                        nextScrollPaneX = 0;
                    }
                    currentScrollPane.setTranslateX(currentScrollPaneX);
                    if (pagination.getCurrentPageIndex() != 0) {
                        createPage(nextScrollPane, currentIndex - 1);
                        nextScrollPane.setVisible(true);
                        nextScrollPane.setTranslateX(nextScrollPaneX);
                    } else {
                        currentScrollPane.setTranslateX(0);
                    }
                }
                e.consume();
            }
        });

        setOnTouchReleased(new EventHandler<TouchEvent>() {
            @Override
            public void handle(TouchEvent e) {
                double delta = Math.abs(e.getTouchPoint().getSceneX() - pressPos);
                double width = getWidth() - (getInsets().getLeft() + getInsets().getRight());
                double threshold = delta/width;
                if (touchMoved) {
                    if (threshold > THRESHOLD) {
                        if (pressPos > e.getTouchPoint().getSceneX()) {
                            selectNext();
                        } else {
                            selectPrevious();
                        }
                    } else {
                        animateClamping(pressPos > e.getTouchPoint().getSceneX());
                    }
                }
                touchMoved = false;
                e.consume();
            }
        });
    }

    private void resetIndexes(boolean usePageIndex) {
        maxPageIndicatorCount = getSkinnable().getMaxPageIndicatorCount();
        pageCount = getPageCount();
        if (pageCount > maxPageIndicatorCount) {
            pageCount = maxPageIndicatorCount;
        }

        fromIndex = 0;
        previousIndex = 0;
        currentIndex = usePageIndex ? getSkinnable().getCurrentPageIndex() : 0;
        toIndex = fromIndex + (pageCount - 1);

        boolean isAnimate = animate;
        if (isAnimate) {
            animate = false;
        }

        pagination.setCurrentPageIndex(currentIndex);
        createPage(currentScrollPane, currentIndex);

        if (isAnimate) {
            animate = true;
        }
    }

    private boolean createPage(ScrollPane pane, int index) {
        if (pagination.getPageFactory() != null) {
            Node content = pagination.getPageFactory().call(index);
            // If the content is null we don't want to switch pages.
            if (content != null) {
                pane.setContent(content);
                return true;
            } else {
                // Disable animation if the new page does not exist.  It is strange to
                // see the same page animated out then in.
                boolean isAnimate = animate;
                if (isAnimate) {
                    animate = false;
                }

                pagination.setCurrentPageIndex(previousIndex);

                if (isAnimate) {
                    animate = true;
                }
                return false;
            }
        }
        return false;
    }

    private int getPageCount() {
        return getSkinnable().getPageCount();
    }

    private static final Interpolator interpolator = Interpolator.SPLINE(0.4829, 0.5709, 0.6803, 0.9928);
    private int currentAnimatedIndex;
    private int previousAnimatedIndex;
    private boolean hasPendingAnimation = false;

    private void animateSwitchPage() {
        if (timeline != null) {
            timeline.setRate(8);
            hasPendingAnimation = true;
            return;
        }

          // Uncomment this code if we want to see the page index
          // selections as we cycle from previous to the current index.
//        previousAnimatedIndex = currentAnimatedIndex;
//        if (currentIndex > previousIndex) {
//            currentAnimatedIndex++;
//        } else {
//            currentAnimatedIndex--;
//        }

        // We are handling a touch event if nextScrollPane's page has already been
        // created and visible == true.
        if (!nextScrollPane.isVisible()) {
            if (!createPage(nextScrollPane, currentAnimatedIndex)) {
                // The page does not exist just return without starting
                // any animation.
                return;
            }
        }
        nextScrollPane.setCache(true);
        currentScrollPane.setCache(true);

        // wait one pulse then animate
        Platform.runLater(new Runnable() {
            @Override public void run() {
                // We are handling a touch event if nextScrollPane's translateX is not 0
                boolean useTranslateX = nextScrollPane.getTranslateX() != 0;
                if (currentAnimatedIndex > previousIndex) {  // animate right to left
                    if (!useTranslateX) {
                        nextScrollPane.setTranslateX(currentScrollPane.getWidth());
                    }
                    nextScrollPane.setVisible(true);
                    timeline = new Timeline();
                    KeyFrame k1 =  new KeyFrame(Duration.millis(0),
                        new KeyValue(currentScrollPane.translateXProperty(),
                            useTranslateX ? currentScrollPane.getTranslateX() : 0,
                            interpolator),
                        new KeyValue(nextScrollPane.translateXProperty(),
                            useTranslateX ?
                                nextScrollPane.getTranslateX() : currentScrollPane.getWidth(), interpolator));
                    KeyFrame k2 = new KeyFrame(DURATION,
                        swipeAnimationEndEventHandler,
                        new KeyValue(currentScrollPane.translateXProperty(), -currentScrollPane.getWidth(), interpolator),
                        new KeyValue(nextScrollPane.translateXProperty(), 0, interpolator));
                    timeline.getKeyFrames().setAll(k1, k2);
                    timeline.play();
                } else { // animate left to right
                    if (!useTranslateX) {
                        nextScrollPane.setTranslateX(-currentScrollPane.getWidth());
                    }
                    nextScrollPane.setVisible(true);
                    timeline = new Timeline();
                    KeyFrame k1 = new KeyFrame(Duration.millis(0),
                        new KeyValue(currentScrollPane.translateXProperty(),
                            useTranslateX ? currentScrollPane.getTranslateX() : 0,
                            interpolator),
                        new KeyValue(nextScrollPane.translateXProperty(),
                            useTranslateX ? nextScrollPane.getTranslateX() : -currentScrollPane.getWidth(),
                            interpolator));
                    KeyFrame k2 = new KeyFrame(DURATION,
                        swipeAnimationEndEventHandler,
                        new KeyValue(currentScrollPane.translateXProperty(), currentScrollPane.getWidth(), interpolator),
                        new KeyValue(nextScrollPane.translateXProperty(), 0, interpolator));
                    timeline.getKeyFrames().setAll(k1, k2);
                    timeline.play();
                }
            }
        });
    }

    private EventHandler<ActionEvent> swipeAnimationEndEventHandler = new EventHandler<ActionEvent>() {
        @Override public void handle(ActionEvent t) {
            ScrollPane temp = currentScrollPane;
            currentScrollPane = nextScrollPane;
            nextScrollPane = temp;

            timeline = null;

            currentScrollPane.setTranslateX(0);
            currentScrollPane.setCache(false);

            nextScrollPane.setTranslateX(0);
            nextScrollPane.setCache(false);
            nextScrollPane.setVisible(false);
            nextScrollPane.setContent(null);

//            // Uncomment this code if we want to see the page index
//            // selections as we cycle from previous to the current index.
//            int savedCurrentIndex = currentIndex;
//            int savedPreviousIndex = previousIndex;
//
//            // We swap out the current and previous index so we can select
//            // and unselect them.
//            previousIndex = previousAnimatedIndex;
//            currentIndex = currentAnimatedIndex;
//            navigation.updatePageIndex();
//            currentIndex = savedCurrentIndex;
//            previousIndex = savedPreviousIndex;
//
//            if (currentAnimatedIndex != currentIndex) {
//                animateSwitchPage();
//            }

            if (hasPendingAnimation) {
                animateSwitchPage();
                hasPendingAnimation = false;
            }
        }
    };

    // If the swipe hasn't reached the THRESHOLD we want to animate the clamping.
    private void animateClamping(boolean rightToLeft) {
        if (rightToLeft) {  // animate right to left
            timeline = new Timeline();
            KeyFrame k1 = new KeyFrame(Duration.millis(0),
                new KeyValue(currentScrollPane.translateXProperty(), currentScrollPane.getTranslateX(), interpolator),
                new KeyValue(nextScrollPane.translateXProperty(), nextScrollPane.getTranslateX(), interpolator));
            KeyFrame k2 = new KeyFrame(DURATION,
                clampAnimationEndEventHandler,
                new KeyValue(currentScrollPane.translateXProperty(), 0, interpolator),
                new KeyValue(nextScrollPane.translateXProperty(), currentScrollPane.getWidth(), interpolator));
            timeline.getKeyFrames().setAll(k1, k2);
            timeline.play();
        } else { // animate left to right
            timeline = new Timeline();
            KeyFrame k1 = new KeyFrame(Duration.millis(0),
                new KeyValue(currentScrollPane.translateXProperty(), currentScrollPane.getTranslateX(), interpolator),
                new KeyValue(nextScrollPane.translateXProperty(), nextScrollPane.getTranslateX(), interpolator));
            KeyFrame k2 = new KeyFrame(DURATION,
                clampAnimationEndEventHandler,
                new KeyValue(currentScrollPane.translateXProperty(), 0, interpolator),
                new KeyValue(nextScrollPane.translateXProperty(), -currentScrollPane.getWidth(), interpolator));
            timeline.getKeyFrames().setAll(k1, k2);
            timeline.play();
        }
    }

    private EventHandler<ActionEvent> clampAnimationEndEventHandler = new EventHandler<ActionEvent>() {
        @Override public void handle(ActionEvent t) {
            currentScrollPane.setTranslateX(0);
            nextScrollPane.setTranslateX(0);
            nextScrollPane.setVisible(false);
            timeline = null;
        }
    };

    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if (p == "PAGE_FACTORY") {
            resetIndexes(false);
            navigation.initializePageIndicators();
            navigation.updatePageIndicators();
        } else if (p == "PAGE_COUNT") {
            resetIndexes(false);
            navigation.initializePageIndicators();
            navigation.updatePageIndicators();
        }
        requestLayout();
    }

    @Override protected void setWidth(double value) {
        super.setWidth(value);
        clipRect.setWidth(value);
    }

    @Override protected void setHeight(double value) {
        super.setHeight(value);
        clipRect.setHeight(value);
    }

    @Override protected double computePrefWidth(double height) {
        double left = snapSpace(getInsets().getLeft());
        double right = snapSpace(getInsets().getRight());
        double navigationWidth = snapSize(navigation.prefWidth(height));
        return left + Math.max(currentScrollPane.prefWidth(height), navigationWidth) + right;
    }

    @Override protected double computePrefHeight(double width) {
        double top = snapSpace(getInsets().getTop());
        double bottom = snapSpace(getInsets().getBottom());
        double navigationHeight = snapSize(navigation.prefHeight(width));
        return top + currentScrollPane.prefHeight(width) + navigationHeight + bottom;
    }

    @Override protected void layoutChildren() {
        double left = snapSpace(getInsets().getLeft());
        double right = snapSpace(getInsets().getRight());
        double top = snapSpace(getInsets().getTop());
        double bottom = snapSpace(getInsets().getBottom());
        double width = snapSize(getWidth() - (left + right));
        double height = snapSize(getHeight() - (top + bottom));
        double navigationHeight = snapSize(navigation.prefHeight(-1));
        double scrollPaneHeight = snapSize(height - navigationHeight);

        layoutInArea(currentScrollPane, left, top, width, scrollPaneHeight, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(nextScrollPane, left, top, width, scrollPaneHeight, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(navigation, left, scrollPaneHeight, width, navigationHeight, 0, HPos.CENTER, VPos.CENTER);
    }

    class NavigationControl extends StackPane {

        private HBox controlBox;
        private Button leftArrowButton;
        private Button rightArrowButton;
        private ToggleGroup indicatorButtons;

        public NavigationControl() {
            getStyleClass().setAll("pagination-control");

            controlBox = new HBox();
            controlBox.getStyleClass().add("control-box");

            leftArrowButton = new Button();
            leftArrowButton.getStyleClass().add("left-arrow-button");
            leftArrowButton.setFocusTraversable(false);

            rightArrowButton = new Button();
            rightArrowButton.getStyleClass().add("right-arrow-button");
            rightArrowButton.setFocusTraversable(false);

            indicatorButtons = new ToggleGroup();

            getChildren().add(controlBox);
            initializeNavigationHandlers();
            initializePageIndicators();
            updatePageIndex();
        }

        private void initializeNavigationHandlers() {
            leftArrowButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent arg0) {
                    selectPrevious();
                    requestLayout();
                }
            });

            rightArrowButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent arg0) {
                    selectNext();
                    requestLayout();
                }
            });

            pagination.currentPageIndexProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
                    previousIndex = arg1.intValue();
                    currentIndex = arg2.intValue();
                    updatePageIndex();
                    if (animate) {
                        // Uncomment this code if we want to see the page index
                        // selections as we cycle from previous to the current index.
                        //currentAnimatedIndex = previousIndex;
                        currentAnimatedIndex = currentIndex;
                        animateSwitchPage();
                    } else {
                        createPage(currentScrollPane, currentIndex);
                    }
                }
            });
        }

        private void initializePageIndicators() {
            if (!indicatorButtons.getToggles().isEmpty()) {
                controlBox.getChildren().clear();
                indicatorButtons.getToggles().clear();
            }

            controlBox.getChildren().add(leftArrowButton);
            for (int i = fromIndex; i <= toIndex; i++) {
                IndicatorButton ib = new IndicatorButton(i);
                ib.setToggleGroup(indicatorButtons);
                controlBox.getChildren().add(ib);

            }
            controlBox.getChildren().add(rightArrowButton);
        }

        private void updatePageIndicators() {
            for (int i = 0; i < indicatorButtons.getToggles().size(); i++) {
                IndicatorButton ib = (IndicatorButton)indicatorButtons.getToggles().get(i);
                if (ib.getPageNumber() == currentIndex) {
                    ib.setSelected(true);
                    break;
                }
            }
        }

        private void updatePageIndex() {
            //System.out.println("SELECT PROPERTY FROM " + fromIndex + " TO " + toIndex + " PREVIOUS " + previousIndex + " CURRENT "+ currentIndex + " PAGE COUNT " + pageCount + " PAGE INDICATOR COUNT " + pageIndicatorCount);
            if (pageCount == maxPageIndicatorCount) {
                if (changePageSet()) {
                    initializePageIndicators();
                }
            }
            updatePageIndicators();
            requestLayout();
        }

        // Only change to the next set when the current index is at the start or the end of the set.
        // Return true only if we have scrolled to the next/previous set.
        private boolean changePageSet() {
            if (previousIndex < currentIndex && currentIndex % maxPageIndicatorCount == 0) {
                // Get the right page set
                fromIndex = currentIndex;
                toIndex = fromIndex + (maxPageIndicatorCount - 1);
            } else if (currentIndex < previousIndex && currentIndex % maxPageIndicatorCount == maxPageIndicatorCount - 1) {
                // Get the left page set
                toIndex = currentIndex;
                fromIndex = toIndex - (maxPageIndicatorCount - 1);
            } else {
                // We need to get the new page set if the currentIndex is out of range.
                // This can happen if setPageIndex() is called programatically.
                if (currentIndex < fromIndex || currentIndex > toIndex) {
                    fromIndex = currentIndex - (currentIndex % maxPageIndicatorCount);
                    toIndex = fromIndex + (maxPageIndicatorCount - 1);
                } else {
                    return false;
                }
            }

            // We have gone past the total number of pages
            if (toIndex > getPageCount() - 1) {
                if (fromIndex > getPageCount() - 1) {
                    return false;
                } else {
                  toIndex = getPageCount() - 1;
                }
            }

            // We have gone past the starting page
            if (fromIndex < 0) {
                fromIndex = 0;
                toIndex = fromIndex + (maxPageIndicatorCount - 1);
            }
            return true;
        }

        @Override protected double computeMinWidth(double height) {
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());
            double leftArrowWidth = snapSize(leftArrowButton.prefWidth(-1));
            double rightArrowWidth = snapSize(rightArrowButton.prefWidth(-1));
            double spacing = snapSize(controlBox.getSpacing());

            return left + leftArrowWidth + spacing + rightArrowWidth + right;
        }

        @Override protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }

        @Override protected double computePrefWidth(double height) {
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());
            double controlBoxWidth = snapSize(controlBox.prefWidth(height));

            return left + controlBoxWidth + right;
        }

        @Override protected double computePrefHeight(double width) {
            double top = snapSpace(getInsets().getTop());
            double bottom = snapSpace(getInsets().getBottom());
            double boxHeight = snapSize(controlBox.prefHeight(width));

            return top + boxHeight + bottom;
        }

        @Override protected void layoutChildren() {
            double top = snapSpace(getInsets().getTop());
            double bottom = snapSpace(getInsets().getBottom());
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());
            double width = snapSize(getWidth()) - (left + right);
            double height = snapSize(getHeight()) - (top + bottom);
            double controlBoxWidth = snapSize(controlBox.prefWidth(-1));
            double controlBoxHeight = snapSize(controlBox.prefHeight(-1));

            leftArrowButton.setDisable(false);
            rightArrowButton.setDisable(false);

            if (currentIndex == 0) {
                // Grey out the left arrow if we are at the beginning.
                leftArrowButton.setDisable(true);
            }
            if (currentIndex == (getPageCount() - 1)) {
                // Grey out the right arrow if we have reached the end.
                rightArrowButton.setDisable(true);
            }

            // Determine the number of indicators we can fit within the pagination width.
            double availableWidth = width - controlBoxWidth;

            double controlBoxX = left + Utils.computeXOffset(width, controlBoxWidth, HPos.CENTER);
            double controlBoxY = top + Utils.computeYOffset(height, controlBoxHeight, VPos.CENTER);

            layoutInArea(controlBox, controlBoxX, controlBoxY, controlBoxWidth, controlBoxHeight, 0, HPos.CENTER, VPos.CENTER);
        }
    }

    class IndicatorButton extends ToggleButton {
        private int pageNumber;

        public IndicatorButton(int pageNumber) {
            this.pageNumber = pageNumber;
            setFocusTraversable(false);
            setIndicatorType();

            getSkinnable().getStyleClass().addListener(new ListChangeListener<String>() {
                @Override
                public void onChanged(Change<? extends String> change) {
                    setIndicatorType();
                }
            });

            setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent arg0) {
                    int selected = pagination.getCurrentPageIndex();
                    // We do not need to update the selection if it has not changed.
                    if (selected != IndicatorButton.this.pageNumber) {
                        pagination.setCurrentPageIndex(IndicatorButton.this.pageNumber);
                        requestLayout();
                    }
                }
            });
        }

        private void setIndicatorType() {
            if (getSkinnable().getStyleClass().contains(Pagination.STYLE_CLASS_BULLET)) {
                getStyleClass().addAll("bullet-button");
            } else {
                getStyleClass().addAll("number-button");
                setText(Integer.toString(this.pageNumber + 1));
            }
        }

        public int getPageNumber() {
            return this.pageNumber;
        }

        @Override public void fire() {
            // we don't toggle from selected to not selected if part of a group
            if (getToggleGroup() == null || !isSelected()) {
                super.fire();
            }
        }
    }
}
