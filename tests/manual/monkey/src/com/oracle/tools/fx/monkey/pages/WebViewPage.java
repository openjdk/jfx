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
package com.oracle.tools.fx.monkey.pages;

import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * WebView Test Page.
 */
public class WebViewPage extends TestPaneBase {
    private final TextField addressField;
    private final WebView view;
    private final WebEngine engine;

    public WebViewPage() {
        addressField = new TextField();
        addressField.setOnAction((ev) -> {
            handleUrlEntered();
        });

        view = new WebView();

        engine = view.getEngine();
        engine.setOnError((ev) -> {
            System.err.println(ev);
        });
        engine.setOnStatusChanged((ev) -> {
            System.err.println(ev);
        });
        engine.getLoadWorker().stateProperty().addListener((s,p,c) -> {
            System.err.println(c);
        });
        
        OptionPane p = new OptionPane();
        p.label("Data:");
        //p.option(modelSelector);
        setOptions(p);

        BorderPane bp = new BorderPane();
        bp.setTop(addressField);
        bp.setCenter(view);
        setContent(bp);
    }
    
    protected void handleUrlEntered() {
        String url = addressField.getText();
        if(Utils.isBlank(url)) {
            return;
        }
        
        engine.load(url);
    }
}
