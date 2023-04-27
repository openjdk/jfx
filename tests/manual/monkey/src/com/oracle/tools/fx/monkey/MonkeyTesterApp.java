/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey;

import java.util.Arrays;
import java.util.Comparator;
import com.oracle.tools.fx.monkey.pages.DemoPage;
import com.oracle.tools.fx.monkey.settings.FxSettings;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.Native2AsciiPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Monkey Tester Application.
 *
 * Applications stores its user preferences (window location, etc.) in ~/.MonkeyTester directory.
 * To use a different directory, redefine the "user.home" system property, -Duser.home=<...>.
 * To disable saving, specify -Ddisable.settings=true vm agrument.
 */
public class MonkeyTesterApp extends Application {
    protected Stage stage;
    protected ObservableList<DemoPage> pages = FXCollections.observableArrayList();
    protected ListView<DemoPage> pageSelector;
    protected BorderPane contentPane;
    protected DemoPage currentPage;
    protected Label status;

    public static void main(String[] args) {
        Application.launch(MonkeyTesterApp.class, args);
    }

    @Override
    public void init() {
        if (!Boolean.getBoolean("disable.settings")) {
            FxSettings.useDirectory(".MonkeyTester");
        }
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        status = new Label();
        status.setPadding(new Insets(2, 2, 2, 2));

        Label spacer = new Label();

        Label ver = new Label();

        GridPane st = new GridPane();
        st.add(status, 0, 0);
        st.add(spacer, 1, 0);
        st.add(ver, 2, 0);
        GridPane.setVgrow(status, Priority.ALWAYS);
        GridPane.setHgrow(spacer, Priority.ALWAYS);
        GridPane.setVgrow(ver, Priority.ALWAYS);

        pages.setAll(createPages());

        pageSelector = new ListView(pages);
        pageSelector.setId("pageSelector");
        pageSelector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updatePage(c);
        });

        contentPane = new BorderPane();
        contentPane.setId("contentPane");

        SplitPane split = new SplitPane(pageSelector, contentPane);
        split.setDividerPositions(0.15);
        SplitPane.setResizableWithParent(pageSelector, Boolean.FALSE);
        SplitPane.setResizableWithParent(contentPane, Boolean.TRUE);

        BorderPane bp = new BorderPane();
        bp.setTop(createMenu());
        bp.setCenter(split);
        bp.setBottom(st);

        stage.setScene(new Scene(bp));
        stage.setWidth(1200);
        stage.setHeight(800);

        stage.renderScaleXProperty().addListener((x) -> updateStatus());
        stage.renderScaleYProperty().addListener((x) -> updateStatus());
        updateTitle();
        updateStatus();

        stage.show();
    }

    protected MenuBar createMenu() {
        CheckMenuItem orientation = new CheckMenuItem("Orientation: RTL");
        orientation.setOnAction((ev) -> {
            NodeOrientation v = (orientation.isSelected()) ? NodeOrientation.RIGHT_TO_LEFT
                : NodeOrientation.LEFT_TO_RIGHT;
            stage.getScene().setNodeOrientation(v);
        });

        MenuBar b = new MenuBar();
        // File
        FX.menu(b, "_File");
        FX.item(b, "Quit", Platform::exit);
        // Page
        FX.menu(b, "_Page");
        FX.item(b, "Reload Current Page", this::reloadCurrentPage);
        // Menu
        FX.menu(b, "_Menu");
        ToggleGroup g = new ToggleGroup();
        FX.radio(b, "RadioMenuItem 1", KeyCombination.keyCombination("Shortcut+1"), g);
        FX.radio(b, "RadioMenuItem 2", KeyCombination.keyCombination("Shortcut+2"), g);
        FX.radio(b, "RadioMenuItem 3", KeyCombination.keyCombination("Shortcut+3"), g);
        FX.menu(b, "_Tools");
        FX.item(b, "Native-to-ascii", this::openNative2Ascii);
        // Window
        FX.menu(b, "_Window");
        FX.item(b, orientation);
        FX.separator(b);
        FX.item(b, "Open Modal Window", this::openModalWindow);
        return b;
    }

    protected void updatePage(DemoPage p) {
        FxSettings.store(contentPane);
        currentPage = p;
        contentPane.setCenter(p == null ? null : p.createPane());
        updateTitle();
        FxSettings.restore(contentPane);
    }

    protected void reloadCurrentPage() {
        updatePage(currentPage);
    }

    protected void updateTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append("Monkey Tester");
        if (currentPage != null) {
            sb.append(" - ");
            sb.append(currentPage.toString());
        }
        stage.setTitle(sb.toString());
    }

    protected void updateStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        sb.append(System.getProperty("javafx.runtime.version"));

        if (stage.getRenderScaleX() == stage.getRenderScaleY()) {
            sb.append("  scale=");
            sb.append(stage.getRenderScaleX());
        } else {
            sb.append("  scaleX=");
            sb.append(stage.getRenderScaleX());
            sb.append("  scaleY=");
            sb.append(stage.getRenderScaleY());
        }
        status.setText(sb.toString());
    }

    protected DemoPage[] createPages() {
        DemoPage[] pages = Pages.create();
        Arrays.sort(pages, new Comparator<DemoPage>() {
            @Override
            public int compare(DemoPage a, DemoPage b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        return pages;
    }

    protected void openModalWindow() {
        Button b = new Button("Platform.exit()");
        b.setOnAction((ev) -> Platform.exit());

        Button b2 = new Button("OK");

        HBox bp = new HBox(b, b2);

        BorderPane p = new BorderPane();
        p.setBottom(bp);

        Stage d = new Stage();
        d.setScene(new Scene(p));
        d.initModality(Modality.APPLICATION_MODAL);
        d.initOwner(stage);
        d.setWidth(500);
        d.setHeight(400);
        d.show();

        b2.setOnAction((ev) -> d.hide());
    }

    protected void openNative2Ascii() {
        Stage s = new Stage();
        s.setTitle("Native to ASCII");
        s.setScene(new Scene(new Native2AsciiPane()));
        s.show();
    }
}
