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

package myapp3;

import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import myapp3.pkg4.MyData;

import static myapp3.Constants.*;

// This logic is copied from AppTreeTableViewExported.

/**
 * Modular test application for testing JavaFX beans.
 * This is launched by ModuleLauncherTest.
 */
public class AppTreeTableViewOpened extends Application {

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

    private Logger logger;
    private Handler logHandler;

    private void initLogger() {
        Locale.setDefault(Locale.US);

        // Initialize Logger
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                final Throwable t = record.getThrown();
                // detect any Throwable:
                if (t != null) {
                    System.err.println("ERROR: unexpected exception was logged: " + record.getMessage());
                    t.printStackTrace();
                    System.exit(ERROR_UNEXPECTED_EXCEPTION);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        logger = Logger.getLogger("javafx.scene.control");
        logger.addHandler(logHandler);
    }

    @Override
    public void start(Stage stage) throws Exception {
        initLogger();

        try {
            StackPane root = new StackPane();
            Scene scene = new Scene(root);
            TreeTableView<MyData> treeTableView = new TreeTableView<>();

            // Name column
            TreeTableColumn<MyData, String> nameCol = new TreeTableColumn<>();
            nameCol.setText("Name");
            nameCol.setPrefWidth(150);
            nameCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));

            // Value column
            TreeTableColumn<MyData, Integer> valueCol = new TreeTableColumn<>();
            valueCol.setText("Value");
            valueCol.setPrefWidth(100);
            valueCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));

            treeTableView.getColumns().addAll(nameCol, valueCol);

            TreeItem<MyData> treeRoot = new TreeItem<>(new MyData("Row A", 1));
            treeRoot.setExpanded(true);
            treeTableView.setRoot(treeRoot);

            TreeItem<MyData> item1 = new TreeItem<>(new MyData("Row B", 2));
            TreeItem<MyData> item2 = new TreeItem<>(new MyData("Row C", 3));
            treeRoot.getChildren().addAll(item1, item2);

            root.getChildren().add(treeTableView);

            stage.setScene(scene);
            stage.show();

            // Hide the stage after the specified amount of time
            KeyFrame kf = new KeyFrame(Duration.millis(SHOWTIME), e -> stage.hide());
            Timeline timeline = new Timeline(kf);
            timeline.play();
        } catch (Error | Exception ex) {
            System.err.println("ERROR: caught unexpected exception: " + ex);
            ex.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    @Override public void stop() {
        System.exit(ERROR_NONE);
    }

}
