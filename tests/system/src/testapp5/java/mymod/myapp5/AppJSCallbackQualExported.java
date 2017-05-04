/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package myapp5;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import myapp5.pkg3.MyCallback;

import static myapp5.Constants.*;

/**
 * Modular test application for testing Javascript callback.
 * This is launched by ModuleLauncherTest.
 */
public class AppJSCallbackQualExported extends Application {

    private final MyCallback callback = new MyCallback();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Application.launch(args);
        } catch (Throwable t) {
            System.err.println("ERROR: caught unexpected exception: " + t);
            t.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            final WebView webView = new WebView();
            webView.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                if (n == Worker.State.SUCCEEDED) {
                    try {
                        final JSObject window = (JSObject) webView.getEngine().executeScript("window");
                        Util.assertNotNull(window);
                        window.setMember("javaCallback", callback);
                        webView.getEngine().executeScript("document.getElementById(\"mybtn1\").click()");
                        Util.assertEquals(0, callback.getCount());
                        System.exit(ERROR_NONE);
                    } catch (Throwable t) {
                        t.printStackTrace(System.err);
                        System.exit(ERROR_ASSERTION_FAILURE);
                    }
                }
            });
            webView.getEngine().loadContent(Util.content);
        } catch (Error | Exception ex) {
            System.err.println("ERROR: caught unexpected exception: " + ex);
            ex.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

}
