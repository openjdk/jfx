/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.core;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

/**
 * A custom page container like a TabView but with special visuals and animation.
 */
public class PageContainer extends Region {
    private static final int PAGE_GAP = 15;

    private final ObservableList<Page> pages = FXCollections.observableArrayList();
    private final ObservableList<Page> unmod = FXCollections.unmodifiableObservableList(pages);
    private final Group pagesGroup = new Group();
    private final Rectangle pagesClipRect = new Rectangle();
    private final ReadOnlyObjectWrapper<Page> currentPage = new ReadOnlyObjectWrapper<>(this, "currentPage");

    public PageContainer() {
        getStyleClass().setAll("page-container");
        pagesClipRect.setSmooth(false);
        pagesGroup.setAutoSizeChildren(false);
        pagesGroup.setClip(pagesClipRect);
        getChildren().addAll(pagesGroup);
    }

    public final Page getCurrentPage() { return currentPage.get(); }
    public final ReadOnlyObjectProperty<Page> currentPageProperty() { return currentPage.getReadOnlyProperty(); }
    public final ObservableList<Page> getPages() { return unmod; }

    private double computeConstraintOfPage(Callback<Node,Double> f) {
        final Page page = getCurrentPage();
        final Insets insets = getInsets();
        if (page != null) {
            final Node n = page.getUI();
            if (n != null) {
                return insets.getLeft() + f.call(n) + insets.getRight();
            }
        }
        return insets.getLeft() + 200 + insets.getRight();
    }

    @Override protected double computeMinWidth(double height) {
        return computeConstraintOfPage(n -> n.minWidth(-1));
    }

    @Override protected double computeMinHeight(double width) {
        return computeConstraintOfPage(n -> n.minHeight(-1));
    }

    @Override protected double computePrefWidth(double height) {
        return computeConstraintOfPage(n -> n.prefWidth(-1));
    }

    @Override protected double computePrefHeight(double width) {
        final double minHeight = minHeight(-1);
        final double maxHeight = maxHeight(-1);
        double prefHeight;
        final Page page = getCurrentPage();
        if (page != null) {
            final Insets inset = getInsets();
            if (width == -1) {
                width = prefWidth(-1);
            }
            double contentWidth = width - inset.getLeft() - inset.getRight();
            double contentHeight = page.getUI().prefHeight(contentWidth);
            prefHeight = inset.getTop() + contentHeight + inset.getBottom();
        } else {
            prefHeight = minHeight;
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
        return Double.MAX_VALUE;
    }

    @Override protected void layoutChildren() {
        final Insets insets = getInsets();
        final int width = (int)getWidth();
        final int height = (int)getHeight();
        final int top = (int)insets.getTop();
        final int right = (int)insets.getRight();
        final int bottom = (int)insets.getBottom();
        final int left = (int)insets.getLeft();

        int pageWidth = width - left - right;
        int pageHeight = height - top - bottom;

        pagesGroup.resizeRelocate(left, top, pageWidth, pageHeight);
        pagesClipRect.setWidth(pageWidth);
        pagesClipRect.setHeight(pageHeight);

        int pageX = 0;
        for (Node page : pagesGroup.getChildren()) {
            page.resizeRelocate(pageX, 0, pageWidth, pageHeight);
            pageX += pageWidth + PAGE_GAP;
        }
    }

    public final void clearPages() {
        while (!pages.isEmpty()) {
            pages.remove(pages.size() - 1).handleHidden();
        }
        currentPage.set(null);
        pagesGroup.getChildren().clear();
        pagesClipRect.setX(0);
        pagesClipRect.setWidth(400);
        pagesClipRect.setHeight(400);
        pagesGroup.setTranslateX(0);
    }

    public final void popPage() {
        if (pages.isEmpty()) return;
        Page oldPage = pages.remove(pages.size() - 1);
        oldPage.handleHidden();
        oldPage.setPageContainer(null);
        if (pages.size() > 0) {
            currentPage.set(pages.get(pages.size()-1));
            int pageWidth = (int) getWidth();
            final int newPageX = (pageWidth+PAGE_GAP) * (pages.size()-1);
            new Timeline(
                    new KeyFrame(Duration.millis(350),
                            t -> {
                                pagesGroup.setCache(false);
                                pagesGroup.getChildren().remove(pagesGroup.getChildren().size()-1);
                                resizePopoverToNewPage(currentPage.get().getUI());
                            },
                            new KeyValue(pagesGroup.translateXProperty(), -newPageX, Interpolator.EASE_BOTH),
                            new KeyValue(pagesClipRect.xProperty(), newPageX, Interpolator.EASE_BOTH)
                    )
            ).play();
        } else {
            pagesGroup.getChildren().clear();
            currentPage.set(null);
        }
    }

    public final void pushPage(final Page page) {
        final Node pageNode = page.getUI();
        pagesGroup.getChildren().add(pageNode);
        final int pageWidth = (int) getWidth();
        final int newPageX = (pageWidth + PAGE_GAP) * pages.size();

        if (!pages.isEmpty() && isVisible()) {
            final Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(350),
                            t -> {
                                pagesGroup.setCache(false);
                                resizePopoverToNewPage(pageNode);
                            },
                            new KeyValue(pagesGroup.translateXProperty(), -newPageX, Interpolator.EASE_BOTH),
                            new KeyValue(pagesClipRect.xProperty(), newPageX, Interpolator.EASE_BOTH)
                    )
            );
            timeline.play();
        }
        page.setPageContainer(this);
        page.handleShown();
        pages.add(page);
        currentPage.set(page);
    }

    private void resizePopoverToNewPage(final Node newPageNode) {
//        final Insets insets = getInsets();
//        final double width = prefWidth(-1);
//        final double contentWidth = width - insets.getLeft() - insets.getRight();
//        double h = newPageNode.prefHeight(contentWidth);
//        h += insets.getTop() + insets.getBottom();
//        new Timeline(
//                new KeyFrame(Duration.millis(200),
//                        new KeyValue(popoverHeight, h, Interpolator.EASE_BOTH)
//                )
//        ).play();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }

    public static final class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            TestPage red = new TestPage("Red", "", "Green", Color.RED);
            TestPage green = new TestPage("Green", "Red", "Blue", Color.GREEN);
            TestPage blue = new TestPage("Blue", "Green", "", Color.BLUE);
            red.next = green;
            green.next = blue;
            PageContainer container = new PageContainer();
            container.pushPage(red);
            container.setLayoutY(30);
            PageNavigationBar bar = new PageNavigationBar();
            bar.setPageContainer(container);
            bar.setPrefHeight(30);

            VBox root = new VBox(bar, container);
            root.setFillWidth(true);
            VBox.setVgrow(container, Priority.ALWAYS);
            Scene s = new Scene(root, 640, 480);

            s.setOnKeyReleased(ke -> {
                Page p = container.getCurrentPage();
                if (p == null) return;
                if (ke.getCode() == KeyCode.RIGHT) {
                    p.handleRightButton();
                } else if (ke.getCode() == KeyCode.LEFT && p != red) {
                    p.handleLeftButton();
                }
            });
            primaryStage.setScene(s);
            primaryStage.show();
        }
    };

    private static final class TestPage implements Page {
        private final ReadOnlyStringWrapper title = new ReadOnlyStringWrapper(this, "title");
        private final ReadOnlyStringWrapper leftButtonText = new ReadOnlyStringWrapper(this, "leftButtonText");
        private final ReadOnlyStringWrapper rightButtonText = new ReadOnlyStringWrapper(this, "rightButtonText");
        private final Region ui;
        private PageContainer container;
        private Page next;

        public TestPage(String title, String leftButtonText, String rightButtonText, Color c) {
            this.title.set(title);
            this.leftButtonText.set(leftButtonText);
            this.rightButtonText.set(rightButtonText);
            ui = new Region();
            ui.setPrefWidth(200);
            ui.setPrefHeight(200);
            ui.setBackground(new Background(new BackgroundFill(c, null, null)));
        }

        @Override public void setPageContainer(PageContainer container) { this.container = container; }
        @Override public PageContainer getPageContainer() { return container; }
        @Override public Node getUI() { return ui; }
        @Override public String getTitle() { return title.get(); }
        @Override public ReadOnlyStringProperty titleProperty() { return title.getReadOnlyProperty(); }
        @Override public String getLeftButtonText() { return leftButtonText.get(); }
        @Override public ReadOnlyStringProperty leftButtonTextProperty() { return leftButtonText.getReadOnlyProperty(); }

        @Override
        public void handleLeftButton() {
            container.popPage();
        }

        @Override public String getRightButtonText() { return rightButtonText.get(); }
        @Override public ReadOnlyStringProperty rightButtonTextProperty() { return rightButtonText.getReadOnlyProperty(); }

        @Override
        public void handleRightButton() {
            if (next != null) {
                container.pushPage(next);
            }
        }

        @Override
        public void handleShown() {
        }

        @Override
        public void handleHidden() {
        }

        @Override public String toString() {
            return getTitle();
        }
    }
}
