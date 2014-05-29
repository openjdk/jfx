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
package a11y;


import a11y.HelloSimpleTableView.Item;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloSimpleTreeView extends Application {

    public void start(Stage stage) {
        
        TreeItem<String> root = new TreeItem<>("Root node");
        for (int i = 0; i < 200; i++) {
            TreeItem<String> item = new TreeItem<>("Child node " + i);
            root.getChildren().add(item);
            if ((i % 3) == 0) {
                for (int j = 0; j < 5; j++) {
                    TreeItem<String> sitem = new TreeItem<>("sub item " + i + " " + j);
                    item.getChildren().add(sitem);
                }
                if ((i % 2) == 0) item.setExpanded(true);
            }
        }
        root.setExpanded(true);
        TreeView<String> treeView = new TreeView<>(root);
        Label label = new Label("JFX TreeView");
        label.setLabelFor(treeView);
        Button button = new Button("okay");
        ToggleButton button2 = new ToggleButton("empty");
        button2.setOnAction(t-> {
            if (treeView.getRoot()!=null) {
                treeView.setRoot(null);
            } else {
                treeView.setRoot(root);
            }
        });
        VBox group = new VBox(label, treeView, button, button2);
        stage.setScene(new Scene(group, 800, 600));
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
