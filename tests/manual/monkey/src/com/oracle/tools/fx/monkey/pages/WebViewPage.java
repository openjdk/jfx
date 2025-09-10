/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.pages;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.ColorOption;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.sheets.NodePropertySheet;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.EnterTextDialog;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * WebView Test Page.
 */
public class WebViewPage extends TestPaneBase {
    private final TextField addressField;
    private final WebView webView;
    private final WebEngine engine;
    private static final SimpleStringProperty htmlContent = new SimpleStringProperty();

    public WebViewPage() {
        super("WebViewPage");

        webView = new WebView();
        engine = webView.getEngine();
        engine.setOnError((ev) -> {
            System.err.println("onError:" + ev);
        });
        engine.setOnStatusChanged((ev) -> {
            System.err.println("onStatusChanged:" + ev);
        });
        engine.getLoadWorker().stateProperty().addListener((s, p, c) -> {
            System.err.println("state:" + c);
        });

        addressField = new TextField("https://");
        addressField.setOnAction((ev) -> {
            handleUrlEntered();
        });

        Button contentButton = new Button("Edit HTML");
        contentButton.setOnAction((ev) -> {
            openContentEditor();
        });

        OptionPane op = new OptionPane();
        op.section("WebView");
        op.option(new BooleanOption("contextMenuEnabled", "context menu enabled", webView.contextMenuEnabledProperty()));
        op.option("Font Scale:", DoubleOption.of("fontScale", webView.fontScaleProperty(), 0.2, 0.5, 0.75, 1.0, 1.5, 2.0, 4.0));
        op.option("Font Smoothing:", new EnumOption<>("fontSmoothing", FontSmoothingType.class, webView.fontSmoothingTypeProperty()));
        op.option("Max Height", Options.tabPaneConstraints("maxHeight", webView.maxHeightProperty()));
        op.option("Max Width", Options.tabPaneConstraints("maxWidth", webView.maxWidthProperty()));
        op.option("Min Height", Options.tabPaneConstraints("minHeight", webView.minHeightProperty()));
        op.option("Min Width", Options.tabPaneConstraints("minWidth", webView.minWidthProperty()));
        op.option("Page Fill:", new ColorOption("textFill", webView.pageFillProperty()));
        op.option("Pref Height", Options.tabPaneConstraints("prefHeight", webView.prefHeightProperty()));
        op.option("Pref Width", Options.tabPaneConstraints("prefWidth", webView.prefWidthProperty()));
        op.option("Zoom:", DoubleOption.of("zoom", webView.zoomProperty(), 0.2, 0.5, 0.75, 1.0, 1.5, 2.0, 4.0));
        NodePropertySheet.appendTo(op, webView);

        HBox tb = new HBox(
            5,
            addressField,
            contentButton
        );
        HBox.setHgrow(addressField, Priority.ALWAYS);
        tb.setBackground(Background.fill(Color.gray(0.8)));
        tb.setPadding(new Insets(0, 2, 2, 2));

        BorderPane bp = new BorderPane();
        bp.setTop(tb);
        bp.setCenter(webView);

        setOptions(op);
        setContent(bp);
    }

    private void handleUrlEntered() {
        String url = addressField.getText();
        if (!Utils.isBlank(url)) {
            engine.load(url);
        }
    }

    private void openContentEditor() {
        String old = htmlContent.get();
        EnterTextDialog d = new EnterTextDialog(this, old, (html) -> {
            engine.loadContent(html);
            htmlContent.set(html);
        });
        d.setTitle("Edit HTML Content");
        d.show();
    }
}
