/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import com.oracle.tools.fx.monkey.pages.DemoPage;
import com.oracle.tools.fx.monkey.settings.FxSettings;
import com.oracle.tools.fx.monkey.tools.ClipboardViewer;
import com.oracle.tools.fx.monkey.tools.CssPlaygroundPane;
import com.oracle.tools.fx.monkey.tools.EmbeddedFxTextArea;
import com.oracle.tools.fx.monkey.tools.EmbeddedJTextAreaWindow;
import com.oracle.tools.fx.monkey.tools.KeyboardEventViewer;
import com.oracle.tools.fx.monkey.tools.Native2AsciiPane;
import com.oracle.tools.fx.monkey.tools.SystemInfoViewer;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.SingleInstance;

/**
 * Monkey Tester Main Window
 */
public class MainWindow extends Stage {
    protected ObservableList<DemoPage> pages = FXCollections.observableArrayList();
    protected ListView<DemoPage> pageSelector;
    protected BorderPane contentPane;
    protected DemoPage currentPage;
    protected Label status;

    public MainWindow() {
        FX.name(this, "MainWindow");

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
        FX.name(pageSelector, "pageSelector");
        pageSelector.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updatePage(c);
        });

        contentPane = new BorderPane();
        FX.name(contentPane, "contentPane");

        SplitPane split = new SplitPane(pageSelector, contentPane);
        FX.name(split, "MainSplit");
        split.setDividerPositions(0.15);
        SplitPane.setResizableWithParent(pageSelector, Boolean.FALSE);
        SplitPane.setResizableWithParent(contentPane, Boolean.TRUE);

        BorderPane bp = new BorderPane();
        bp.setTop(createMenu());
        bp.setCenter(split);
        bp.setBottom(st);

        setScene(new Scene(bp));
        setWidth(1200);
        setHeight(800);

        renderScaleXProperty().addListener((x) -> updateStatus());
        renderScaleYProperty().addListener((x) -> updateStatus());
        updateTitle();
        updateStatus();
    }

    private MenuBar createMenu() {
        CheckMenuItem orientation = new CheckMenuItem("Orientation: RTL");
        orientation.setOnAction((ev) -> {
            NodeOrientation v = (orientation.isSelected()) ?
                NodeOrientation.RIGHT_TO_LEFT :
                NodeOrientation.LEFT_TO_RIGHT;
            getScene().setNodeOrientation(v);
        });

        MenuBar b = new MenuBar();
        // File
        FX.menu(b, "_File");
        FX.item(b, "Quit", Platform::exit);
        // Page
        FX.menu(b, "_Page");
        FX.item(b, "Reload Current Page", this::reloadCurrentPage);
        // Skin
        FX.menu(b, "_Skin");
        FX.item(b, "Set New Skin", this::newSkin);
        FX.item(b, "<null> Skin", this::nullSkin);
        // Menu
        FX.menu(b, "_Menu");
        ToggleGroup g = new ToggleGroup();
        FX.radio(b, "RadioMenuItem 1", KeyCombination.keyCombination("Shortcut+1"), g);
        FX.radio(b, "RadioMenuItem 2", KeyCombination.keyCombination("Shortcut+2"), g);
        FX.radio(b, "RadioMenuItem 3", KeyCombination.keyCombination("Shortcut+3"), g);
        // Tools
        FX.menu(b, "_Tools");
        FX.item(b, "Clipboard Viewer", this::openClipboardViewer);
        FX.item(b, "CSS Playground", this::openCssPlayground);
        FX.item(b, "FX TextArea Embedded in JFXPanel", this::openJFXPanel);
        FX.item(b, "JTextArea/JTextField Embedded in SwingNode", this::openJTextArea);
        FX.item(b, "Keyboard Event Viewer", this::openKeyboardViewer);
        FX.item(b, "Native to ASCII", this::openNative2Ascii);
        FX.item(b, "System Info", this::openSystemInfo);
        // Window
        FX.menu(b, "_Window");
        FX.item(b, orientation);
        FX.separator(b);
        FX.item(b, "Open Modal Window", this::openModalWindow);
        return b;
    }

    private void updatePage(DemoPage p) {
        FxSettings.store(contentPane);
        currentPage = p;
        contentPane.setCenter(p == null ? null : p.createPane());
        updateTitle();
        FxSettings.restore(contentPane);
    }

    private void reloadCurrentPage() {
        updatePage(currentPage);
    }

    private void updateTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append("Monkey Tester");
        if (currentPage != null) {
            sb.append(" - ");
            sb.append(currentPage.toString());
        }
        setTitle(sb.toString());
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("   FX:");
        sb.append(System.getProperty("javafx.runtime.version"));
        sb.append("  JDK:");
        sb.append(System.getProperty("java.version"));

        if (getRenderScaleX() == getRenderScaleY()) {
            sb.append("  scale=");
            sb.append(getRenderScaleX());
        } else {
            sb.append("  scaleX=");
            sb.append(getRenderScaleX());
            sb.append("  scaleY=");
            sb.append(getRenderScaleY());
        }

        sb.append("  LOC:");
        sb.append(new File("").getAbsolutePath());
        status.setText(sb.toString());
    }

    private DemoPage[] createPages() {
        DemoPage[] pages = Pages.create();
        Arrays.sort(pages, new Comparator<DemoPage>() {
            @Override
            public int compare(DemoPage a, DemoPage b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        return pages;
    }

    private void openModalWindow() {
        new ModalWindow(this).show();
     }

    private void openNative2Ascii() {
        SingleInstance.openSingleInstance(
            "Native2AsciiWindow",
            "Native to ASCII",
            Native2AsciiPane::new
        );
    }

    private void openCssPlayground() {
        SingleInstance.openSingleInstance(
            "CSSPlayground",
            "CSS Playground",
            CssPlaygroundPane::new
        );
    }

    private void openClipboardViewer() {
        SingleInstance.openSingleInstance(
            "ClipboardViewer",
            "Clipboard Viewer",
            ClipboardViewer::new
        );
    }

    private void openKeyboardViewer() {
        SingleInstance.openSingleInstance(
            "KeyboardEventViewer",
            "Keyboard / Input Method Event Viewer",
            KeyboardEventViewer::new
        );
    }

    private void openSystemInfo() {
        SingleInstance.openSingleInstance(
            "SystemInfo",
            "System Info",
            SystemInfoViewer::new
        );
    }

    private void openJTextArea() {
        SingleInstance.openSingleInstance(
            "JTextArea",
            "JTextArea/JTextField Embedded in SwingNode",
            EmbeddedJTextAreaWindow::new
        );
    }

    private void openJFXPanel() {
        EmbeddedFxTextArea.start();
    }

    private void nullSkin() {
        Node n = contentPane.getCenter();
        if (n instanceof HasSkinnable s) {
            s.nullSkin();
        }
    }

    private void newSkin() {
        Node n = contentPane.getCenter();
        if (n instanceof HasSkinnable s) {
            s.newSkin();
        }
    }
}
