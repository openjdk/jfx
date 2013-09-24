/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * Displays a "back" and "forward" button, and the current page title, of some
 * PageContainer.
 */
public class PageNavigationBar extends Region {
    private final Button leftButton = new Button("Left");
    private final Button rightButton = new Button("Right");
    private Label title;

    private ChangeListener<Page> pageChangeListener = (container, oldPage, newPage) -> {
        if (newPage == null) {
            leftButton.visibleProperty().unbind();
            leftButton.textProperty().unbind();
            leftButton.setVisible(false);
            rightButton.visibleProperty().unbind();
            rightButton.textProperty().unbind();
            rightButton.setVisible(false);
            title.textProperty().unbind();
            title.setVisible(false);
        } else {
            leftButton.visibleProperty().bind(newPage.leftButtonTextProperty().isNotNull().and(newPage.leftButtonTextProperty().isNotEmpty()));
            leftButton.textProperty().bind(newPage.leftButtonTextProperty());
            rightButton.visibleProperty().bind(newPage.rightButtonTextProperty().isNotNull().and(newPage.rightButtonTextProperty().isNotEmpty()));
            rightButton.textProperty().bind(newPage.rightButtonTextProperty());
            title.textProperty().bind(newPage.titleProperty());
            title.setVisible(true);
        }
    };

    private ObjectProperty<PageContainer> pageContainer = new SimpleObjectProperty<PageContainer>(this, "pageContainer") {
        private PageContainer old;
        @Override protected void invalidated() {
            PageContainer value = get();
            if (old != value) {
                if (old != null) {
                    old.currentPageProperty().removeListener(pageChangeListener);
                }
                if (value != null) {
                    value.currentPageProperty().addListener(pageChangeListener);
                    pageChangeListener.changed(value.currentPageProperty(), null, value.getCurrentPage());
                }
                old = value;
            }
        }
    };
    public final PageContainer getPageContainer() { return pageContainer.get(); }
    public final void setPageContainer(PageContainer value) { pageContainer.set(value); }
    public final ObjectProperty<PageContainer> pageContainerProperty() { return pageContainer; }

    public PageNavigationBar() {
        leftButton.setOnMouseClicked(this::onLeft);
        rightButton.setOnMouseClicked(this::onRight);
        title = new Label();
        title.setAlignment(Pos.CENTER);
        getChildren().addAll(title, leftButton, rightButton);
    }

    private void onLeft(Event e) {
        PageContainer pc = getPageContainer();
        if (pc != null) {
            Page p = pc.getCurrentPage();
            if  (p != null) {
                p.handleLeftButton();
            }
        }
    }

    private void onRight(Event e) {
        PageContainer pc = getPageContainer();
        if (pc != null) {
            Page p = pc.getCurrentPage();
            if  (p != null) {
                p.handleRightButton();
            }
        }
    }

    @Override
    protected void layoutChildren() {
        final Insets insets = getInsets();
        final double top = insets.getTop();
        final double left = insets.getLeft();
        final double right = insets.getRight();
        final double bottom = insets.getBottom();
        final double width = getWidth() - left - right;
        final double height = getHeight() - top - bottom;

        final int leftButtonWidth = (int)(leftButton.prefWidth(-1)+0.5d);
        leftButton.resizeRelocate(left, top, leftButtonWidth, height);

        final int rightButtonWidth = (int)(rightButton.prefWidth(-1)+0.5d);
        rightButton.resizeRelocate(width-right-rightButtonWidth, top, rightButtonWidth, height);

        title.setLayoutX(left);
        title.setLayoutY(top);
        title.resize(width, height);
    }
}
