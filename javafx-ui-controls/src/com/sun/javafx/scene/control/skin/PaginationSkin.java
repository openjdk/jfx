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

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.css.StyleableBooleanProperty;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.scene.control.behavior.PaginationBehavior;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class PaginationSkin extends SkinBase<Pagination, PaginationBehavior>  {

    private static final Duration DURATION = new Duration(125.0);
    private static final double SWIPE_THRESHOLD = 0.30;
    private static final double TOUCH_THRESHOLD = 15;

    private Pagination pagination;
    private StackPane currentStackPane;
    private StackPane nextStackPane;
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

        this.currentStackPane = new StackPane();
        currentStackPane.getStyleClass().add("page");

        this.nextStackPane = new StackPane();
        nextStackPane.getStyleClass().add("page");
        nextStackPane.setVisible(false);

        resetIndexes(true);

        this.navigation = new NavigationControl();

        getChildren().addAll(currentStackPane, nextStackPane, navigation);

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

    private double startTouchPos;
    private double lastTouchPos;
    private long startTouchTime;
    private long lastTouchTime;
    private double touchVelocity;
    private boolean touchThresholdBroken;
    private int touchEventId = -1;
    private boolean nextPageReached = false;
    private boolean setInitialDirection = false;
    private int direction;

    private void initializeSwipeAndTouchHandlers() {
        if (PlatformUtil.isEmbedded()) {
            setOnTouchPressed(new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent e) {
                    if (touchEventId == -1) {
                        touchEventId = e.getTouchPoint().getId();
                    }
                    if (touchEventId != e.getTouchPoint().getId()) {
                        return;
                    }
                    lastTouchPos = startTouchPos = e.getTouchPoint().getX();
                    lastTouchTime = startTouchTime = System.currentTimeMillis();
                    touchThresholdBroken = false;
                    e.consume();
                }
            });

            setOnTouchMoved(new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent e) {
                    if (touchEventId != e.getTouchPoint().getId()) {
                        return;
                    }

                    double drag = e.getTouchPoint().getX() - lastTouchPos;
                    long time = System.currentTimeMillis() - lastTouchTime;
                    touchVelocity = drag/time;
                    lastTouchPos = e.getTouchPoint().getX();
                    lastTouchTime = System.currentTimeMillis();
                    double delta = e.getTouchPoint().getX() - startTouchPos;

                    if (!touchThresholdBroken && Math.abs(delta) > TOUCH_THRESHOLD) {
                        touchThresholdBroken = true;
                    }

                    if (touchThresholdBroken) {
                        double width = getWidth() - (getInsets().getLeft() + getInsets().getRight());
                        double currentPaneX;
                        double nextPaneX;

                        if (!setInitialDirection) {
                            // Remember the direction travelled so we can
                            // load the next or previous page if the touch is not released.
                            setInitialDirection = true;
                            direction = delta < 0 ? 1 : -1;
                        }
                        if (delta < 0) {
                            if (direction == -1) {
                                nextStackPane.getChildren().clear();
                                direction = 1;
                            }
                            // right to left
                            if (Math.abs(delta) <= width) {
                                currentPaneX = delta;
                                nextPaneX = width + delta;
                                nextPageReached = false;
                            } else {
                                currentPaneX = -width;
                                nextPaneX = 0;
                                nextPageReached = true;
                            }
                            currentStackPane.setTranslateX(currentPaneX);
                            if (pagination.getCurrentPageIndex() < getPageCount() - 1) {
                                createPage(nextStackPane, currentIndex + 1);
                                nextStackPane.setVisible(true);
                                nextStackPane.setTranslateX(nextPaneX);
                            } else {
                                currentStackPane.setTranslateX(0);
                            }
                        } else {
                            // left to right
                            if (direction == 1) {
                                nextStackPane.getChildren().clear();
                                direction = -1;
                            }
                            if (Math.abs(delta) <= width) {
                                currentPaneX = delta;
                                nextPaneX = -width + delta;
                                nextPageReached = false;
                            } else {
                                currentPaneX = width;
                                nextPaneX = 0;
                                nextPageReached = true;
                            }
                            currentStackPane.setTranslateX(currentPaneX);
                            if (pagination.getCurrentPageIndex() != 0) {
                                createPage(nextStackPane, currentIndex - 1);
                                nextStackPane.setVisible(true);
                                nextStackPane.setTranslateX(nextPaneX);
                            } else {
                                currentStackPane.setTranslateX(0);
                            }
                        }
                    }
                    e.consume();
                }
            });

            setOnTouchReleased(new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent e) {
                    if (touchEventId != e.getTouchPoint().getId()) {
                        return;
                    } else {
                        touchEventId = -1;
                        setInitialDirection = false;
                    }

                    if (touchThresholdBroken) {
                        // determin if click or swipe
                        final double drag = e.getTouchPoint().getX() - startTouchPos;
                        // calculate complete time from start to end of drag
                        final long time = System.currentTimeMillis() - startTouchTime;
                        // if time is less than 300ms then considered a quick swipe and whole time is used
                        final boolean quick = time < 300;
                        // calculate velocity
                        final double velocity = quick ? (double)drag / time : touchVelocity; // pixels/ms
                        // calculate distance we would travel at this speed for 500ms of travel
                        final double distance = (velocity * 500);
                        final double width = getWidth() - (getInsets().getLeft() + getInsets().getRight());

                        // The swipe distance travelled.
                        final double threshold = Math.abs(distance/width);
                        // The touch and dragged distance travelled.
                        final double delta = Math.abs(drag/width);
                        if (threshold > SWIPE_THRESHOLD || delta > SWIPE_THRESHOLD) {
                            if (startTouchPos > e.getTouchPoint().getX()) {
                                selectNext();
                            } else {
                                selectPrevious();
                            }
                        } else {
                            animateClamping(startTouchPos > e.getTouchPoint().getSceneX());
                        }
                    }
                    e.consume();
                }
            });
        }
    }

    private void resetIndexes(boolean usePageIndex) {
        maxPageIndicatorCount = getSkinnable().getMaxPageIndicatorCount();
        // Used to indicate that we can change a set of pages.
        pageCount = getPageCount();
        if (pageCount > maxPageIndicatorCount) {
            pageCount = maxPageIndicatorCount;
        }

        fromIndex = 0;
        previousIndex = 0;
        currentIndex = usePageIndex ? getSkinnable().getCurrentPageIndex() : 0;
        toIndex = fromIndex + (pageCount - 1);

        if (pageCount == Pagination.INDETERMINATE && maxPageIndicatorCount == Pagination.INDETERMINATE) {
            // We do not know how many indicators  can fit.  Let the layout pass compute it.
            toIndex = 0;
        }

        boolean isAnimate = animate;
        if (isAnimate) {
            animate = false;
        }

        pagination.setCurrentPageIndex(currentIndex);
        createPage(currentStackPane, currentIndex);

        if (isAnimate) {
            animate = true;
        }
    }

    private boolean createPage(StackPane pane, int index) {
        if (pagination.getPageFactory() != null && pane.getChildren().isEmpty()) {
            Node content = pagination.getPageFactory().call(index);
            // If the content is null we don't want to switch pages.
            if (content != null) {
                pane.getChildren().setAll(content);
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

        // We are handling a touch event if nextPane's page has already been
        // created and visible == true.
        if (!nextStackPane.isVisible()) {
            if (!createPage(nextStackPane, currentAnimatedIndex)) {
                // The page does not exist just return without starting
                // any animation.
                return;
            }
        }
        if (nextPageReached) {
            // No animation is needed when the next page is already showing
            // and in the correct position.  Just swap the panes and return
            swapPanes();
            nextPageReached = false;
            return;
        }

        nextStackPane.setCache(true);
        currentStackPane.setCache(true);

        // wait one pulse then animate
        Platform.runLater(new Runnable() {
            @Override public void run() {
                // We are handling a touch event if nextPane's translateX is not 0
                boolean useTranslateX = nextStackPane.getTranslateX() != 0;
                if (currentAnimatedIndex > previousIndex) {  // animate right to left
                    if (!useTranslateX) {
                        nextStackPane.setTranslateX(currentStackPane.getWidth());
                    }
                    nextStackPane.setVisible(true);
                    timeline = new Timeline();
                    KeyFrame k1 =  new KeyFrame(Duration.millis(0),
                        new KeyValue(currentStackPane.translateXProperty(),
                            useTranslateX ? currentStackPane.getTranslateX() : 0,
                            interpolator),
                        new KeyValue(nextStackPane.translateXProperty(),
                            useTranslateX ?
                                nextStackPane.getTranslateX() : currentStackPane.getWidth(), interpolator));
                    KeyFrame k2 = new KeyFrame(DURATION,
                        swipeAnimationEndEventHandler,
                        new KeyValue(currentStackPane.translateXProperty(), -currentStackPane.getWidth(), interpolator),
                        new KeyValue(nextStackPane.translateXProperty(), 0, interpolator));
                    timeline.getKeyFrames().setAll(k1, k2);
                    timeline.play();
                } else { // animate left to right
                    if (!useTranslateX) {
                        nextStackPane.setTranslateX(-currentStackPane.getWidth());
                    }
                    nextStackPane.setVisible(true);
                    timeline = new Timeline();
                    KeyFrame k1 = new KeyFrame(Duration.millis(0),
                        new KeyValue(currentStackPane.translateXProperty(),
                            useTranslateX ? currentStackPane.getTranslateX() : 0,
                            interpolator),
                        new KeyValue(nextStackPane.translateXProperty(),
                            useTranslateX ? nextStackPane.getTranslateX() : -currentStackPane.getWidth(),
                            interpolator));
                    KeyFrame k2 = new KeyFrame(DURATION,
                        swipeAnimationEndEventHandler,
                        new KeyValue(currentStackPane.translateXProperty(), currentStackPane.getWidth(), interpolator),
                        new KeyValue(nextStackPane.translateXProperty(), 0, interpolator));
                    timeline.getKeyFrames().setAll(k1, k2);
                    timeline.play();
                }
            }
        });
    }

    private EventHandler<ActionEvent> swipeAnimationEndEventHandler = new EventHandler<ActionEvent>() {
        @Override public void handle(ActionEvent t) {
            swapPanes();
            timeline = null;

            if (hasPendingAnimation) {
                animateSwitchPage();
                hasPendingAnimation = false;
            }
        }
    };

    private void swapPanes() {
        StackPane temp = currentStackPane;
        currentStackPane = nextStackPane;
        nextStackPane = temp;

        currentStackPane.setTranslateX(0);
        currentStackPane.setCache(false);

        nextStackPane.setTranslateX(0);
        nextStackPane.setCache(false);
        nextStackPane.setVisible(false);
        nextStackPane.getChildren().clear();
    }

    // If the swipe hasn't reached the THRESHOLD we want to animate the clamping.
    private void animateClamping(boolean rightToLeft) {
        if (rightToLeft) {  // animate right to left
            timeline = new Timeline();
            KeyFrame k1 = new KeyFrame(Duration.millis(0),
                new KeyValue(currentStackPane.translateXProperty(), currentStackPane.getTranslateX(), interpolator),
                new KeyValue(nextStackPane.translateXProperty(), nextStackPane.getTranslateX(), interpolator));
            KeyFrame k2 = new KeyFrame(DURATION,
                clampAnimationEndEventHandler,
                new KeyValue(currentStackPane.translateXProperty(), 0, interpolator),
                new KeyValue(nextStackPane.translateXProperty(), currentStackPane.getWidth(), interpolator));
            timeline.getKeyFrames().setAll(k1, k2);
            timeline.play();
        } else { // animate left to right
            timeline = new Timeline();
            KeyFrame k1 = new KeyFrame(Duration.millis(0),
                new KeyValue(currentStackPane.translateXProperty(), currentStackPane.getTranslateX(), interpolator),
                new KeyValue(nextStackPane.translateXProperty(), nextStackPane.getTranslateX(), interpolator));
            KeyFrame k2 = new KeyFrame(DURATION,
                clampAnimationEndEventHandler,
                new KeyValue(currentStackPane.translateXProperty(), 0, interpolator),
                new KeyValue(nextStackPane.translateXProperty(), -currentStackPane.getWidth(), interpolator));
            timeline.getKeyFrames().setAll(k1, k2);
            timeline.play();
        }
    }

    private EventHandler<ActionEvent> clampAnimationEndEventHandler = new EventHandler<ActionEvent>() {
        @Override public void handle(ActionEvent t) {
            currentStackPane.setTranslateX(0);
            nextStackPane.setTranslateX(0);
            nextStackPane.setVisible(false);
            timeline = null;
        }
    };

    private BooleanProperty arrowsVisible;
    public final void setArrowsVisible(boolean value) { arrowsVisibleProperty().set(value); }
    public final boolean isArrowsVisible() { return arrowsVisible == null ? DEFAULT_ARROW_VISIBLE : arrowsVisible.get(); }
    public final BooleanProperty arrowsVisibleProperty() {
        if (arrowsVisible == null) {
            arrowsVisible = new StyleableBooleanProperty(DEFAULT_ARROW_VISIBLE) {
                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.ARROWS_VISIBLE;
                }

                @Override
                public Object getBean() {
                    return PaginationSkin.this;
                }

                @Override
                public String getName() {
                    return "arrowVisible";
                }
            };
        }
        return arrowsVisible;
    }

    private BooleanProperty pageInformationVisible;
    public final void setPageInformationVisible(boolean value) { pageInformationVisibleProperty().set(value); }
    public final boolean isPageInformationVisible() { return pageInformationVisible == null ? DEFAULT_PAGE_INFORMATION_VISIBLE : pageInformationVisible.get(); }
    public final BooleanProperty pageInformationVisibleProperty() {
        if (pageInformationVisible == null) {
            pageInformationVisible = new StyleableBooleanProperty(DEFAULT_PAGE_INFORMATION_VISIBLE) {
                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.PAGE_INFORMATION_VISIBLE;
                }

                @Override
                public Object getBean() {
                    return PaginationSkin.this;
                }

                @Override
                public String getName() {
                    return "pageInformationVisible";
                }
            };
        }
        return pageInformationVisible;
    }

    private ObjectProperty<Side> pageInformationAlignment;
    public final void setPageInformationAlignment(Side value) { pageInformationAlignmentProperty().set(value); }
    public final Side getPageInformationAlignment() { return pageInformationAlignment == null ? DEFAULT_PAGE_INFORMATION_ALIGNMENT : pageInformationAlignment.get(); }
    public final ObjectProperty<Side> pageInformationAlignmentProperty() {
        if (pageInformationAlignment == null) {
            pageInformationAlignment = new StyleableObjectProperty<Side>(Side.BOTTOM) {
                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.PAGE_INFORMATION_ALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return PaginationSkin.this;
                }

                @Override
                public String getName() {
                    return "pageInformationAlignment";
                }
            };
        }
        return pageInformationAlignment;
    }

    private BooleanProperty tooltipVisible;
    public final void setTooltipVisible(boolean value) { tooltipVisibleProperty().set(value); }
    public final boolean isTooltipVisible() { return tooltipVisible == null ? DEFAULT_TOOLTIP_VISIBLE : tooltipVisible.get(); }
    public final BooleanProperty tooltipVisibleProperty() {
        if (tooltipVisible == null) {
            tooltipVisible = new StyleableBooleanProperty(DEFAULT_TOOLTIP_VISIBLE) {
                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.TOOLTIP_VISIBLE;
                }

                @Override
                public Object getBean() {
                    return PaginationSkin.this;
                }

                @Override
                public String getName() {
                    return "tooltipVisible";
                }
            };
        }
        return tooltipVisible;
    }

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

    @Override protected double computeMinWidth(double height) {
        double left = snapSpace(getInsets().getLeft());
        double right = snapSpace(getInsets().getRight());
        double navigationWidth = navigation.isVisible() ? snapSize(navigation.minWidth(height)) : 0;
        return left + Math.max(currentStackPane.minWidth(height), navigationWidth) + right;
    }

    @Override protected double computeMinHeight(double width) {
        double top = snapSpace(getInsets().getTop());
        double bottom = snapSpace(getInsets().getBottom());
        double navigationHeight = navigation.isVisible() ? snapSize(navigation.minHeight(width)) : 0;
        return top + currentStackPane.minHeight(width) + navigationHeight + bottom;
    }

    @Override protected double computePrefWidth(double height) {
        double left = snapSpace(getInsets().getLeft());
        double right = snapSpace(getInsets().getRight());
        double navigationWidth = navigation.isVisible() ? snapSize(navigation.prefWidth(height)) : 0;
        return left + Math.max(currentStackPane.prefWidth(height), navigationWidth) + right;
    }

    @Override protected double computePrefHeight(double width) {
        double top = snapSpace(getInsets().getTop());
        double bottom = snapSpace(getInsets().getBottom());
        double navigationHeight = navigation.isVisible() ? snapSize(navigation.prefHeight(width)) : 0;
        return top + currentStackPane.prefHeight(width) + navigationHeight + bottom;
    }

    @Override protected void layoutChildren() {
        double left = snapSpace(getInsets().getLeft());
        double right = snapSpace(getInsets().getRight());
        double top = snapSpace(getInsets().getTop());
        double bottom = snapSpace(getInsets().getBottom());
        double width = snapSize(getWidth() - (left + right));
        double height = snapSize(getHeight() - (top + bottom));
        double navigationHeight = navigation.isVisible() ? snapSize(navigation.prefHeight(-1)) : 0;
        double stackPaneHeight = snapSize(height - navigationHeight);

        layoutInArea(currentStackPane, left, top, width, stackPaneHeight, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(nextStackPane, left, top, width, stackPaneHeight, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(navigation, left, stackPaneHeight, width, navigationHeight, 0, HPos.CENTER, VPos.CENTER);
    }

    class NavigationControl extends StackPane {

        private HBox controlBox;
        private Button leftArrowButton;
        private StackPane leftArrow;
        private Button rightArrowButton;
        private StackPane rightArrow;
        private ToggleGroup indicatorButtons;
        private Label pageInformation;
        private double previousWidth = -1;
        private double minButtonSize = -1;

        public NavigationControl() {
            getStyleClass().setAll("pagination-control");

            controlBox = new HBox();
            controlBox.getStyleClass().add("control-box");

            leftArrowButton = new Button();
            minButtonSize = leftArrowButton.getFont().getSize() * 2;
            leftArrowButton.fontProperty().addListener(new ChangeListener<Font>() {
                @Override public void changed(ObservableValue<? extends Font> arg0, Font arg1, Font newFont) {
                    minButtonSize = newFont.getSize() * 2;
                    for(Node child: controlBox.getChildren()) {
                        ((Control)child).setMinSize(minButtonSize, minButtonSize);
                    }
                    // We want to relayout the indicator buttons because the size has changed.
                    requestLayout();
                }
            });
            leftArrowButton.setMinSize(minButtonSize, minButtonSize);
            leftArrowButton.getStyleClass().add("left-arrow-button");
            leftArrowButton.setFocusTraversable(false);
            HBox.setMargin(leftArrowButton, new Insets(0, 4, 0, 0));
            leftArrow = new StackPane();
            leftArrow.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
            leftArrowButton.setGraphic(leftArrow);
            leftArrow.getStyleClass().add("left-arrow");

            rightArrowButton = new Button();
            rightArrowButton.setMinSize(minButtonSize, minButtonSize);
            rightArrowButton.getStyleClass().add("right-arrow-button");
            rightArrowButton.setFocusTraversable(false);
            HBox.setMargin(rightArrowButton, new Insets(0, 0, 0, 4));
            rightArrow = new StackPane();
            rightArrow.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
            rightArrowButton.setGraphic(rightArrow);
            rightArrow.getStyleClass().add("right-arrow");

            indicatorButtons = new ToggleGroup();

            pageInformation = new Label();
            pageInformation.getStyleClass().add("page-information");

            getChildren().addAll(controlBox, pageInformation);
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
                        currentAnimatedIndex = currentIndex;
                        animateSwitchPage();
                    } else {
                        createPage(currentStackPane, currentIndex);
                    }
                }
            });
        }

        // Create the indicators using fromIndex and toIndex.
        private void initializePageIndicators() {
            if (!indicatorButtons.getToggles().isEmpty()) {
                previousIndicatorCount = 0;
                controlBox.getChildren().clear();
                indicatorButtons.getToggles().clear();
            }

            controlBox.getChildren().add(leftArrowButton);
            for (int i = fromIndex; i <= toIndex; i++) {
                IndicatorButton ib = new IndicatorButton(i);
                ib.setMinSize(minButtonSize, minButtonSize);
                ib.setToggleGroup(indicatorButtons);
                controlBox.getChildren().add(ib);
            }
            controlBox.getChildren().add(rightArrowButton);
        }

        // Finds and selects the IndicatorButton using the currentIndex.
        private void updatePageIndicators() {
            for (int i = 0; i < indicatorButtons.getToggles().size(); i++) {
                IndicatorButton ib = (IndicatorButton)indicatorButtons.getToggles().get(i);
                if (ib.getPageNumber() == currentIndex) {
                    ib.setSelected(true);
                    updatePageInformation();
                    break;
                }
            }
        }

        // Update the page index using the currentIndex and updates the page set
        // if necessary.
        private void updatePageIndex() {
            //System.out.println("SELECT PROPERTY FROM " + fromIndex + " TO " + toIndex + " PREVIOUS " + previousIndex + " CURRENT "+ currentIndex + " PAGE COUNT " + pageCount + " MAX PAGE INDICATOR COUNT " + maxPageIndicatorCount);
            if (pageCount == maxPageIndicatorCount) {
                if (changePageSet()) {
                    initializePageIndicators();
                }
            }
            updatePageIndicators();
            requestLayout();
        }

        private void updatePageInformation() {
            String currentPageNumber = Integer.toString(currentIndex + 1);
            String lastPageNumber = getPageCount() == Pagination.INDETERMINATE ? "..." : Integer.toString(getPageCount());
            pageInformation.setText(currentPageNumber + "/" + lastPageNumber);
        }

        private int previousIndicatorCount = 0;
        // Layout the maximum number of page indicators we can fit within the width.
        // And always show the selected indicator.
        private void layoutPageIndicators() {
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());
            double width = snapSize(getWidth()) - (left + right);
            double controlBoxleft = snapSpace(controlBox.getInsets().getLeft());
            double controlBoxRight = snapSpace(controlBox.getInsets().getRight());
            double leftArrowWidth = snapSize(Utils.boundedSize(leftArrowButton.prefWidth(-1), leftArrowButton.minWidth(-1), leftArrowButton.maxWidth(-1)));
            double rightArrowWidth = snapSize(Utils.boundedSize(rightArrowButton.prefWidth(-1), rightArrowButton.minWidth(-1), rightArrowButton.maxWidth(-1)));
            double spacing = snapSize(controlBox.getSpacing());
            double w = width - (controlBoxleft + leftArrowWidth + spacing + rightArrowWidth + controlBoxRight);

            if (isPageInformationVisible() &&
                    (Side.LEFT.equals(getPageInformationAlignment()) ||
                    Side.RIGHT.equals(getPageInformationAlignment()))) {
                w -= snapSize(pageInformation.prefWidth(-1));
            }

            double x = 0;
            int indicatorCount = 0;
            for (int i = 0; i < getSkinnable().getMaxPageIndicatorCount(); i++) {
                int index = i < indicatorButtons.getToggles().size() ? i : indicatorButtons.getToggles().size() - 1;
                IndicatorButton ib = (IndicatorButton)indicatorButtons.getToggles().get(index);
                double iw = snapSize(Utils.boundedSize(ib.prefWidth(-1), ib.minWidth(-1), ib.maxWidth(-1)));
                x += (iw + controlBox.getSpacing());
                if (x >= w) {
                    break;
                }
                indicatorCount++;
            }

            if (indicatorCount != previousIndicatorCount) {
                if (indicatorCount < getSkinnable().getMaxPageIndicatorCount()) {
                    maxPageIndicatorCount = indicatorCount;
                } else if (indicatorCount >= getSkinnable().getMaxPageIndicatorCount()) {
                    maxPageIndicatorCount = getSkinnable().getMaxPageIndicatorCount();
                } else {
                    maxPageIndicatorCount = toIndex - fromIndex;
                }

                int lastIndicatorButtonIndex;
                if (pageCount > maxPageIndicatorCount) {
                    pageCount = maxPageIndicatorCount;
                    lastIndicatorButtonIndex = maxPageIndicatorCount - 1;
                 } else {
                    if (indicatorCount > getPageCount()) {
                        pageCount = getPageCount();
                        lastIndicatorButtonIndex = getPageCount() - 1;
                    } else {
                        pageCount = indicatorCount;
                        lastIndicatorButtonIndex = indicatorCount - 1;
                    }
                }

                if (currentIndex >= toIndex) {
                    // The current index has fallen off the right
                    toIndex = currentIndex;
                    fromIndex = toIndex - lastIndicatorButtonIndex;
                } else if (currentIndex <= fromIndex) {
                    // The current index has fallen off the left
                    fromIndex = currentIndex;
                    toIndex = fromIndex + lastIndicatorButtonIndex;
                } else {
                    toIndex = fromIndex + lastIndicatorButtonIndex;
                }

                if (toIndex > getPageCount() - 1) {
                    toIndex = getPageCount() - 1;
                    //fromIndex = toIndex - lastIndicatorButtonIndex;
                }

                if (fromIndex < 0) {
                    fromIndex = 0;
                    toIndex = fromIndex + lastIndicatorButtonIndex;
                }

                initializePageIndicators();
                updatePageIndicators();
                previousIndicatorCount = indicatorCount;
            }
        }

        // Only change to the next set when the current index is at the start or the end of the set.
        // Return true only if we have scrolled to the next/previous set.
        private boolean changePageSet() {
            int index = indexToIndicatorButtonsIndex(currentIndex);
            int lastIndicatorButtonIndex = maxPageIndicatorCount - 1;
            if (previousIndex < currentIndex && index == 0 && index % lastIndicatorButtonIndex == 0) {
                // Get the right page set
                fromIndex = currentIndex;
                toIndex = fromIndex + lastIndicatorButtonIndex;
            } else if (currentIndex < previousIndex && index == lastIndicatorButtonIndex && index % lastIndicatorButtonIndex == 0) {
                // Get the left page set
                toIndex = currentIndex;
                fromIndex = toIndex - lastIndicatorButtonIndex;
            } else {
                // We need to get the new page set if the currentIndex is out of range.
                // This can happen if setPageIndex() is called programatically.
                if (currentIndex < fromIndex || currentIndex > toIndex) {
                    fromIndex = currentIndex - index;
                    toIndex = fromIndex + lastIndicatorButtonIndex;
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
                  //fromIndex = toIndex - lastIndicatorButtonIndex;
                }
            }

            // We have gone past the starting page
            if (fromIndex < 0) {
                fromIndex = 0;
                toIndex = fromIndex + lastIndicatorButtonIndex;
            }
            return true;
        }

        private int indexToIndicatorButtonsIndex(int index) {
            // This should be in the indicator buttons toggle list.
            if (index >= fromIndex && index <= toIndex) {
                return index - fromIndex;
            }
            // The requested index is not in indicator buttons list we have to predict
            // where the index will be.
            int i = 0;
            int from = fromIndex;
            int to = toIndex;
            if (currentIndex > previousIndex) {
                while(from < getPageCount() && to < getPageCount()) {
                    from += i;
                    to += i;
                    if (index >= from && index <= to) {
                        if (index == from) {
                            return 0;
                        } else if (index == to) {
                            return maxPageIndicatorCount - 1;
                        }
                        return index - from;
                    }
                    i += maxPageIndicatorCount;
                }
            } else {
                while (from > 0 && to > 0) {
                    from -= i;
                    to -= i;
                    if (index >= from && index <= to) {
                        if (index == from) {
                            return 0;
                        } else if (index == to) {
                            return maxPageIndicatorCount - 1;
                        }
                        return index - from;
                    }
                    i += maxPageIndicatorCount;
                }
            }
            // We are on the last page set going back to the previous page set
            return maxPageIndicatorCount - 1;
        }

        private Pos sideToPos(Side s) {
            if (Side.TOP.equals(s)) {
                return Pos.TOP_CENTER;
            } else if (Side.RIGHT.equals(s)) {
                return Pos.CENTER_RIGHT;
            } else if (Side.BOTTOM.equals(s)) {
                return Pos.BOTTOM_CENTER;
            }
            return Pos.CENTER_LEFT;
        }

        @Override protected double computeMinWidth(double height) {
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());
            double leftArrowWidth = snapSize(Utils.boundedSize(leftArrowButton.prefWidth(-1), leftArrowButton.minWidth(-1), leftArrowButton.maxWidth(-1)));
            double rightArrowWidth = snapSize(Utils.boundedSize(rightArrowButton.prefWidth(-1), rightArrowButton.minWidth(-1), rightArrowButton.maxWidth(-1)));
            double spacing = snapSize(controlBox.getSpacing());
            double pageInformationWidth = 0;
            Side side = getPageInformationAlignment();
            if (Side.LEFT.equals(side) || Side.RIGHT.equals(side)) {
                pageInformationWidth = snapSize(pageInformation.prefWidth(-1));
            }

            return left + leftArrowWidth + spacing + rightArrowWidth + right + pageInformationWidth;
        }

        @Override protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }

        @Override protected double computePrefWidth(double height) {
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());
            double controlBoxWidth = snapSize(controlBox.prefWidth(height));
            double pageInformationWidth = 0;
            Side side = getPageInformationAlignment();
            if (Side.LEFT.equals(side) || Side.RIGHT.equals(side)) {
                pageInformationWidth = snapSize(pageInformation.prefWidth(-1));
            }

            return left + controlBoxWidth + right + pageInformationWidth;
        }

        @Override protected double computePrefHeight(double width) {
            double top = snapSpace(getInsets().getTop());
            double bottom = snapSpace(getInsets().getBottom());
            double boxHeight = snapSize(controlBox.prefHeight(width));
            double pageInformationHeight = 0;
            Side side = getPageInformationAlignment();
            if (Side.TOP.equals(side) || Side.BOTTOM.equals(side)) {
                pageInformationHeight = snapSize(pageInformation.prefHeight(-1));
            }

            return top + boxHeight + pageInformationHeight + bottom;
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
            double pageInformationWidth = snapSize(pageInformation.prefWidth(-1));
            double pageInformationHeight = snapSize(pageInformation.prefHeight(-1));

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
            // Reapply CSS so the left and right arrow button's disable state is updated
            // immediately.
            impl_reapplyCSS();

            leftArrowButton.setVisible(isArrowsVisible());
            rightArrowButton.setVisible(isArrowsVisible());
            pageInformation.setVisible(isPageInformationVisible());

            // Determine the number of indicators we can fit within the pagination width.
//            if (snapSize(getWidth()) != previousWidth) {
                layoutPageIndicators();
//            }
            previousWidth = getWidth();

            HPos controlBoxHPos = controlBox.getAlignment().getHpos();
            VPos controlBoxVPos = controlBox.getAlignment().getVpos();
            double controlBoxX = left + Utils.computeXOffset(width, controlBoxWidth, controlBoxHPos);
            double controlBoxY = top + Utils.computeYOffset(height, controlBoxHeight, controlBoxVPos);

            if (isPageInformationVisible()) {
                Pos p = sideToPos(getPageInformationAlignment());
                HPos pageInformationHPos = p.getHpos();
                VPos pageInformationVPos = p.getVpos();
                double pageInformationX = left + Utils.computeXOffset(width, pageInformationWidth, pageInformationHPos);
                double pageInformationY = top + Utils.computeYOffset(height, pageInformationHeight, pageInformationVPos);

                if (Side.TOP.equals(getPageInformationAlignment())) {
                    pageInformationY = top;
                    controlBoxY = top + pageInformationHeight;
                } else if (Side.RIGHT.equals(getPageInformationAlignment())) {
                    pageInformationX = width - right - pageInformationWidth;
                } else if (Side.BOTTOM.equals(getPageInformationAlignment())) {
                    controlBoxY = top;
                    pageInformationY = top + controlBoxHeight;
                } else if (Side.LEFT.equals(getPageInformationAlignment())) {
                    pageInformationX = left;
                }
                layoutInArea(pageInformation, pageInformationX, pageInformationY, pageInformationWidth, pageInformationHeight, 0, pageInformationHPos, pageInformationVPos);
            }

            layoutInArea(controlBox, controlBoxX, controlBoxY, controlBoxWidth, controlBoxHeight, 0, controlBoxHPos, controlBoxVPos);
        }
    }

    class IndicatorButton extends ToggleButton {
        private int pageNumber;

        public IndicatorButton(int pageNumber) {
            this.pageNumber = pageNumber;
            setFocusTraversable(false);
            setIndicatorType();
            setTooltipVisible(isTooltipVisible());

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

            tooltipVisibleProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                    setTooltipVisible(newValue);
                }
            });
        }

        private void setIndicatorType() {
            if (getSkinnable().getStyleClass().contains(Pagination.STYLE_CLASS_BULLET)) {
                getStyleClass().remove("number-button");
                getStyleClass().add("bullet-button");
                setText(null);
            } else {
                getStyleClass().remove("bullet-button");
                getStyleClass().add("number-button");
                setText(Integer.toString(this.pageNumber + 1));
            }
        }

        private void setTooltipVisible(boolean b) {
            if (b) {
                setTooltip(new Tooltip(Integer.toString(IndicatorButton.this.pageNumber + 1)));
            } else {
                setTooltip(null);
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

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static final Boolean DEFAULT_ARROW_VISIBLE = Boolean.FALSE;
    private static final Boolean DEFAULT_PAGE_INFORMATION_VISIBLE = Boolean.FALSE;
    private static final Side DEFAULT_PAGE_INFORMATION_ALIGNMENT = Side.BOTTOM;
    private static final Boolean DEFAULT_TOOLTIP_VISIBLE = Boolean.FALSE;

    private static class StyleableProperties {
        private static final StyleableProperty<PaginationSkin,Boolean> ARROWS_VISIBLE =
            new StyleableProperty<PaginationSkin,Boolean>("-fx-arrows-visible",
                BooleanConverter.getInstance(), DEFAULT_ARROW_VISIBLE) {

            @Override
            public boolean isSettable(PaginationSkin n) {
                return n.arrowsVisible == null || !n.arrowsVisible.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(PaginationSkin n) {
                return n.arrowsVisibleProperty();
            }
        };

        private static final StyleableProperty<PaginationSkin,Boolean> PAGE_INFORMATION_VISIBLE =
            new StyleableProperty<PaginationSkin,Boolean>("-fx-page-information-visible",
                BooleanConverter.getInstance(), DEFAULT_PAGE_INFORMATION_VISIBLE) {

            @Override
            public boolean isSettable(PaginationSkin n) {
                return n.pageInformationVisible == null || !n.pageInformationVisible.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(PaginationSkin n) {
                return n.pageInformationVisibleProperty();
            }
        };

        private static final StyleableProperty<PaginationSkin,Side> PAGE_INFORMATION_ALIGNMENT =
            new StyleableProperty<PaginationSkin,Side>("-fx-page-information-alignment",
                new EnumConverter<Side>(Side.class), DEFAULT_PAGE_INFORMATION_ALIGNMENT) {

            @Override
            public boolean isSettable(PaginationSkin n) {
                return n.pageInformationAlignment == null || !n.pageInformationAlignment.isBound();
            }

            @Override
            public WritableValue<Side> getWritableValue(PaginationSkin n) {
                return n.pageInformationAlignmentProperty();
            }
        };

        private static final StyleableProperty<PaginationSkin,Boolean> TOOLTIP_VISIBLE =
            new StyleableProperty<PaginationSkin,Boolean>("-fx-tooltip-visible",
                BooleanConverter.getInstance(), DEFAULT_TOOLTIP_VISIBLE) {

            @Override
            public boolean isSettable(PaginationSkin n) {
                return n.tooltipVisible == null || !n.tooltipVisible.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(PaginationSkin n) {
                return n.tooltipVisibleProperty();
            }
        };

        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(SkinBase.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                ARROWS_VISIBLE,
                PAGE_INFORMATION_VISIBLE,
                PAGE_INFORMATION_ALIGNMENT,
                TOOLTIP_VISIBLE
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return StyleableProperties.STYLEABLES;
    };

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }
}
