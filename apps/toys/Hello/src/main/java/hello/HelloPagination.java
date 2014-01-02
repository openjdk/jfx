/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Pagination;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;


public class HelloPagination extends Application {
    private Pagination pagination;

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    public int itemsPerPage() {
        return 10;
    }

    public VBox createPage(int pageIndex) {
        VBox box = new VBox(5);
        int page = pageIndex * itemsPerPage();
        for (int i = page; i < page + itemsPerPage(); i++) {
            Label l = new Label("PAGE INDEX " + pageIndex);
            box.getChildren().add(l);
        }
        return box;
    }

    public VBox createPage2(int pageIndex) {
        VBox box = new VBox(5);
        int page = pageIndex * itemsPerPage();
        for (int i = page; i < page + itemsPerPage(); i++) {
            Label l = new Label("NEW PAGE INDEX " + pageIndex);
            box.getChildren().add(l);
        }
        return box;
    }

    @Override
    public void start(final Stage stage) throws Exception {
        pagination = new Pagination(28, 0);
        pagination.setStyle("-fx-border-color:red;");
        pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override
            public Node call(Integer pageIndex) {
                return createPage(pageIndex);
            }
        });

        VBox toolbar = new VBox(10);
        Button setMaxPageIndicatorCount = new Button("setMaxPageIndicatorCount = 5");
        setMaxPageIndicatorCount.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                System.out.println("setPageIndicatorCount = 5");
                pagination.setMaxPageIndicatorCount(5);
            }
        });

        Button getMaxPageIndicatorCount = new Button("getMaxPageIndicatorCount");
        getMaxPageIndicatorCount.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                System.out.println("getPageIndicatorCount = " + pagination.getMaxPageIndicatorCount());
            }
        });

        Button setCurrentPageIndex = new Button("setCurrentPageIndex = 19");
        setCurrentPageIndex.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                System.out.println("setCurrentPageIndex = 19");
                pagination.setCurrentPageIndex(19);
            }
        });

        Button setCurrentPageIndex2 = new Button("setCurrentPageIndex = 0");
        setCurrentPageIndex2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                System.out.println("setCurrentPageIndex = 0");
                pagination.setCurrentPageIndex(0);
            }
        });

        Button setCurrentPageIndex3 = new Button("setCurrentPageIndex = 8");
        setCurrentPageIndex3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                System.out.println("setCurrentPageIndex = 8");
                pagination.setCurrentPageIndex(8);
            }
        });

        Button getCurrentPageIndex = new Button("getCurrentPageIndex");
        getCurrentPageIndex.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                System.out.println("getCurrentPageIndex = " + pagination.getCurrentPageIndex());
            }
        });

        Button setOldPageFactory = new Button("set OLD PageFactory");
        setOldPageFactory.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                pagination.setPageFactory(new Callback<Integer, Node>() {
                    @Override
                    public Node call(Integer pageIndex) {
                        return createPage(pageIndex);
                    }
                });
            }
        });

        Button setNewPageFactory = new Button("set NEW PageFactory");
        setNewPageFactory.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                pagination.setPageFactory(new Callback<Integer, Node>() {
                    @Override
                    public Node call(Integer pageIndex) {
                        return createPage2(pageIndex);
                    }
                });
            }
        });

        Button setPageCount = new Button("setPageCount = 8");
        setPageCount.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                System.out.println("setPageCount = 8");
                pagination.setPageCount(8);
            }
        });

        Button setPageCount2 = new Button("setPageCount = 18");
        setPageCount2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                System.out.println("setPageCount = 18");
                pagination.setPageCount(18);
            }
        });

        Button setPageCount3 = new Button("setPageCount = INDETERMINATE");
        setPageCount3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                System.out.println("setPageCount = INDETERMINATE");
                pagination.setPageCount(Pagination.INDETERMINATE);
            }
        });

        toolbar.getChildren().addAll(
                setMaxPageIndicatorCount,
                setCurrentPageIndex, setCurrentPageIndex2, setCurrentPageIndex3,
                setOldPageFactory, setNewPageFactory,
                setPageCount, setPageCount2, setPageCount3,
                getMaxPageIndicatorCount,
                getCurrentPageIndex);

        HBox box = new HBox(10);
        box.getChildren().addAll(toolbar);
        AnchorPane anchor = new AnchorPane();
        AnchorPane.setTopAnchor(box, 10.0);
        AnchorPane.setLeftAnchor(box, 10.0);
        AnchorPane.setTopAnchor(pagination, 10.0);
        AnchorPane.setRightAnchor(pagination, 10.0);
        AnchorPane.setBottomAnchor(pagination, 10.0);
        AnchorPane.setLeftAnchor(pagination, 250.0);
        anchor.getChildren().addAll(box, pagination);
        Scene scene = new Scene(anchor, 800, 600);
        stage.setScene(scene);
        stage.show();
    }
}
