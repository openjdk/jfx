/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates.
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
package com.javafx.experiments.scheduleapp;

import static com.javafx.experiments.scheduleapp.Theme.*;
import com.javafx.experiments.scheduleapp.control.Popover;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.TimelineBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A custom page container like a TabView but with special visuals and animation.
 */
public class PageContainer extends Pane {
    private static final Color TEXT_SELECTED_COLOR = Color.WHITE;
    // model
    private final ObservableList<Page> pages = FXCollections.observableArrayList();
    private Page currentPage = null;
    // ui
    private HBox header = new HBox();
    private Rectangle selectionBox = new Rectangle();
    private Map<Page,Label> titlesMap = new HashMap<Page, Label>();
    private ImageView headerShadow = new ImageView(
                new Image(getClass().getResource("images/header-shadow.png").toExternalForm()));
    private ImageView headerArrow = new ImageView(
                new Image(getClass().getResource("images/header-arrow.png").toExternalForm()));
    private final Popover popover;
    private final AutoLogoutLightBox lightBox;

    public PageContainer(Popover popover, AutoLogoutLightBox lightBox, final Page ... pages) {
        this.pages.addAll(pages);
        this.popover = popover;
        this.lightBox = lightBox;
        // build ui
        header.setId("page-container-header");
        header.setFillHeight(true);
        headerShadow.setMouseTransparent(true);
        headerArrow.setMouseTransparent(true);
//        selectionBox.setFill(new ImagePattern(
//                new Image(getClass().getResource("images/rough_diagonal_blue.jpg").toExternalForm()),
//                0,0,255,255,false));
        selectionBox.setFill( new ImagePattern(
                new Image(getClass().getResource("images/rough_diagonal_blue.jpg").toExternalForm()),
                0,0,255,255,false));
        selectionBox.setManaged(false);
        header.getChildren().add(selectionBox);
        getChildren().addAll(header,headerShadow, headerArrow);
        // add all pages
        getChildren().addAll(pages);
        for (Page page: pages) {
            page.setVisible(false);
        }
        // do first rebuild and listen to changes in available pages
        rebuild();
        this.pages.addListener(new ListChangeListener<Page>() {
            @Override public void onChanged(Change<? extends Page> change) {
                rebuild();
            }
        });
        // goto first page, runLater because we want this to happen after first layout
        Platform.runLater(new Runnable() {
            @Override public void run() {
                gotoPage(pages[0], false);
            }
        });
    }
    
    private void rebuild() {
        if (header.getChildren().size() > 1) {
            header.getChildren().remove(1, header.getChildren().size());
            titlesMap.clear();
        }
        for(final Page page: pages) {
            Label title = new Label(page.getName());
            titlesMap.put(page,title);
            title.setMaxHeight(Double.MAX_VALUE);
            title.getStyleClass().add("page-container-header-title");
            title.setPickOnBounds(true);
            title.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent t) {
                    System.out.println("CLICKED ON PAGE BUTTON FOR "+page.getName());
                    if (currentPage != page) {
                        gotoPage(page, true);
                    }
                    page.pageTabClicked();
                }
            });
            header.getChildren().add(title);
        }
    }
    
    public void gotoPage(Page page, boolean animate) {
        System.out.println("CHANGING TO PAGE --> "+page.getName());
        final Label newTitleLabel = titlesMap.get(page);
        final Bounds newPageTitleBounds = newTitleLabel.getBoundsInParent();
        if (currentPage != null && animate) {
            final Label currentTitlelabel = titlesMap.get(currentPage);
            final Bounds currentPageTitleBounds = currentTitlelabel.getBoundsInParent();
            TimelineBuilder.create()
                .keyFrames(
                    new KeyFrame(Duration.ZERO, 
                        new KeyValue(selectionBox.xProperty(), currentPageTitleBounds.getMinX()),
                        new KeyValue(selectionBox.yProperty(), currentPageTitleBounds.getMinY()),
                        new KeyValue(selectionBox.widthProperty(), currentPageTitleBounds.getWidth()),
                        new KeyValue(selectionBox.heightProperty(), currentPageTitleBounds.getHeight()),
                        new KeyValue(newTitleLabel.textFillProperty(), DARK_GREY),
                        new KeyValue(currentTitlelabel.textFillProperty(), TEXT_SELECTED_COLOR),
                        new KeyValue(headerArrow.layoutXProperty(), headerArrow.getLayoutX())
                    ),
                    new KeyFrame(Duration.seconds(.3), 
                        new KeyValue(selectionBox.xProperty(), newPageTitleBounds.getMinX(), Interpolator.EASE_BOTH),
                        new KeyValue(selectionBox.yProperty(), newPageTitleBounds.getMinY(), Interpolator.EASE_BOTH),
                        new KeyValue(selectionBox.widthProperty(), newPageTitleBounds.getWidth(), Interpolator.EASE_BOTH),
                        new KeyValue(selectionBox.heightProperty(), newPageTitleBounds.getHeight(), Interpolator.EASE_BOTH),
                        new KeyValue(newTitleLabel.textFillProperty(), TEXT_SELECTED_COLOR, Interpolator.EASE_BOTH),
                        new KeyValue(currentTitlelabel.textFillProperty(), DARK_GREY, Interpolator.EASE_BOTH),
                        new KeyValue(headerArrow.layoutXProperty(), newPageTitleBounds.getMinX() + (newPageTitleBounds.getWidth()/2) - 6, Interpolator.EASE_BOTH)
                    )
                )
                .build().play();
            // hide current page
            currentPage.setVisible(false);
        } else {
            selectionBox.setX(newPageTitleBounds.getMinX());
            selectionBox.setY(newPageTitleBounds.getMinY());
            selectionBox.setWidth(newPageTitleBounds.getWidth());
            selectionBox.setHeight(newPageTitleBounds.getHeight());
            headerArrow.setLayoutX(newPageTitleBounds.getMinX() + (newPageTitleBounds.getWidth()/2) - 6);
            if (currentPage != null) {
                // hide current page
                currentPage.setVisible(false);
                // change current pages title back to dark grey
                titlesMap.get(currentPage).setTextFill(DARK_GREY);
            }
            newTitleLabel.setTextFill(TEXT_SELECTED_COLOR);
        }
//        if(getChildren().size() == 3) {
//            getChildren().add(page);
//        } else {
//            getChildren().set(3,page);
//        }
        page.setVisible(true);
        currentPage = page;
    }

    @Override protected void layoutChildren() {
        final double w = getWidth();
        final double h = getHeight();
        header.resize(w, 40);
        headerShadow.setFitWidth(w);
        for(Page page: pages) {
            page.resizeRelocate(0, 40, w, h-40);
        }

        Scene scene = getScene();
        double width = popover.prefWidth(-1);
        popover.setLayoutX((int) ((scene.getWidth() - width) / 2));
        popover.setLayoutY(50);
        if (popover.isVisible()) {
            popover.autosize();
        }
        lightBox.resizeRelocate(0, 0, w, h);
    }
    
    public ObservableList<Page> getPages() {
        return pages;
    }

    public void reset() {
        for (Page page : pages) {
            page.reset();
        }
    }
}
