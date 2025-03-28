/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

package embeddedswing;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;


public class EmbeddedSwing extends Application {
    private volatile SwingNode swingNode;
    private ListView<String> listNode;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        swingNode = new SwingNode();

        createAndShowSwingContent(stage);

        BorderPane pane = new BorderPane();
        pane.setCenter(swingNode);

        listNode = new ListView<String>();
        listNode.setPrefWidth(100);
        listNode.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable,
                                    String oldValue,
                                    final String newValue)
                {
                    SwingUtilities.invokeLater(() -> {
                        MusicRegister.select(newValue);
                    });
                }
            });
        listNode.setTooltip(new Tooltip("ListView"));
        pane.setLeft(listNode);

        stage.setScene(new Scene(pane, 400, 300));
        stage.setTitle("JavaFX/Swing Music Register");
        stage.show();
    }

    private void createAndShowSwingContent(final Stage stage) {
        SwingUtilities.invokeLater(() -> {
            JComponent content = MusicRegister.create();
            content.setToolTipText("SwingNode");
            swingNode.setContent(content);

            Platform.runLater(() -> {
                listNode.setItems(
                    FXCollections.observableArrayList(
                        MusicRegister.items()));
            });
        });
    }
}
