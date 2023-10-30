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
package test.javafx.scene.text;

import javafx.application.Platform;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

public class TextFlowCrashTest {

    private boolean exceptionWasThrown;

    @BeforeAll
    public static void initFX() throws Exception {
        CountDownLatch startupLatch = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            startupLatch.countDown();
        });
        assertTrue(startupLatch.await(15, TimeUnit.SECONDS), "Timeout waiting for FX runtime to start");
    }

    @Test
    public void testTextflowCrash() {
        Util.runAndWait(() -> {
            Stage stage = new Stage();
            VBox root = new VBox();
            addBoundsListener(root);
            Platform.runLater(() -> {
                root.getChildren().add(getBuggyNode());
            });
            stage.setScene(new Scene(root,
                    200,
                    200));
            stage.show();
        });

        Util.runAndWait(() -> {
            assertFalse(exceptionWasThrown);
        });
    }

    public ScrollPane getBuggyNode() {
        ListView<String> listView = new ListView();
        listView.getItems().add("AAA");
        listView.setCellFactory((view) -> {
            ListCell cell = new ListCell();
            TextFlow flow = new TextFlow();
            flow.getChildren().add(new Text("a"));
            Text text2 = new Text("b");
            text2.sceneProperty().addListener((p,o,n) -> {
                try {
                    text2.getBoundsInParent();
                } catch (Throwable e) {
                    exceptionWasThrown = true;
                    throw e;
                }
            });
            flow.getChildren().add(text2);
            cell.setGraphic(flow);
            addBoundsListener(cell);
            return cell;
        });
        ScrollPane scrollPane = new ScrollPane(listView);
        addBoundsListener(listView);
        addBoundsListener(scrollPane);
        return scrollPane;
    }

    public void addBoundsListener(Node node) {
        node.boundsInParentProperty().addListener((p,o,n) -> {
        });
    }
}
