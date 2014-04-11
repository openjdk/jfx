/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package sandbox.app;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import static sandbox.Constants.*;

/**
 *
 * @author kcr
 */
public class Util {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;

    private static final String CSS_FILE_NAME = "test.css";
    private static final String FXML_FILE_NAME = "test.fxml";
    private static final String HTML_FILE_NAME = "test.html";

    // Convert the requested resource name into a URL string, and verify
    // that the resource can be accessed.
    private static URL toURL(String resourceName) throws Exception {
        URL url = FXApp.class.getResource(resourceName);
        URLConnection conn = url.openConnection();
        InputStream stream = conn.getInputStream();
        stream.close();
        return url;
    }

    // Create a JavaFX scene and populate it with content
    public static Scene createScene() throws Exception {
        VBox root = new VBox(10);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setFill(Color.WHITE);

        final String styleSheet = toURL(CSS_FILE_NAME).toExternalForm();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(styleSheet);

        Label label = new Label();
        label.setText("Label");

        final URL fxmlFile = toURL(FXML_FILE_NAME);
        Parent fxmlRoot = (Parent)FXMLLoader.load(fxmlFile);

        final String webURLString = toURL(HTML_FILE_NAME).toExternalForm();
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.load(webURLString);

        root.getChildren().addAll(label, fxmlRoot, webView);
        return scene;
    }

    public static void setupTimeoutThread() {
        // Timeout thread
        Thread th = new Thread(() -> {
            try {
                Thread.sleep(TIMEOUT);
            } catch (InterruptedException ex) {
            }
            System.exit(ERROR_TIMEOUT);
        });
        th.setDaemon(true);
        th.start();
    }

    // No need to ever create an instance of this class
    private Util() {}

}
