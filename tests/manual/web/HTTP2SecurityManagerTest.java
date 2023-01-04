/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

@SuppressWarnings("removal")
public class HTTP2SecurityManagerTest {

    public static class MainWindow extends Application {

        @Override
        public void start(Stage stage) throws Exception {
            VBox instructions =  new VBox(
                new Label(" This test loads a web page with a security manager set,"),
                new Label(" and a Policy that grants AllPermission."),
                new Label(""),
                new Label(" Check the console output for an AccessControllException:"),
                new Label(" Click 'Pass' if there is NO exception"),
                new Label(" Click 'Fail' if an AccessControlException is logged")
            );

            Button passButton = new Button("Pass");
            passButton.setOnAction(e -> {
                Platform.exit();
            });

            Button failButton = new Button("Fail");
            failButton.setOnAction(e -> {
                Platform.exit();
                throw new AssertionError("Unexpected AccessControlException");
            });

            HBox buttonBox = new HBox(20, passButton, failButton);

            WebView webView = new WebView();
            webView.getEngine().load("https://www.oracle.com/java/");
            VBox root = new VBox(10, buttonBox, instructions, webView);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        }

    }

    public static void main(String[] args) {
        Policy.setPolicy(new Policy() {
            @Override
            public PermissionCollection getPermissions(ProtectionDomain domain) {
                Permissions permissions = new Permissions();
                permissions.add(new AllPermission());
                return permissions;
            }
        });
        System.setSecurityManager(new SecurityManager());
        Application.launch(MainWindow.class);
    }
}
