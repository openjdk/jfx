/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.test.manual.util;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TestRunnerApp extends Application {

    /** Default test plain in the project root directory */
    public static final String DEFAULT_TEST_PLAN = "test-plan.txt";

    private TableView<DataRow> table;
    private TextArea log;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final FileChooser.ExtensionFilter TXT = new FileChooser.ExtensionFilter("*.txt", "*.txt");

    public static void main(String args[]) throws Exception {
        Application.launch(TestRunnerApp.class, args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        {
            TableColumn<DataRow, String> c = new TableColumn<>("Name");
            c.setResizable(true);
            c.setCellValueFactory(data -> data.getValue().name);
            c.setPrefWidth(300);
            table.getColumns().add(c);
        }
        {
            TableColumn<DataRow, String> c = new TableColumn<>("Status");
            c.setResizable(true);
            c.setCellValueFactory(data -> data.getValue().status);
            c.setPrefWidth(50);
            table.getColumns().add(c);
        }
        {
            TableColumn<DataRow, String> c = new TableColumn<>("Last Run");
            c.setResizable(true);
            c.setCellValueFactory(data -> data.getValue().lastRun);
            c.setPrefWidth(70);
            table.getColumns().add(c);
        }

        Button runButton = new Button("▶ Run");
        runButton.disableProperty().bind(Bindings.createBooleanBinding(
            () -> {
                return table.getSelectionModel().getSelectedIndices().size() != 1;
            },
            table.getSelectionModel().selectedItemProperty()
        ));
        runButton.setOnAction((_) -> {
            runTest();
        });

        ToolBar tb = new ToolBar();
        tb.getItems().addAll(
            runButton
        );

        log = new TextArea();
        log.setEditable(false);

        MenuBar mb = new MenuBar();
        Menu m;
        MenuItem mi;
        // file
        mb.getMenus().add(m = new Menu("File"));
        m.getItems().add(mi = new MenuItem("Open Test Plan"));
        mi.setOnAction((_) -> openTestPlan());
        // log
        mb.getMenus().add(m = new Menu("Log"));
        m.getItems().add(mi = new MenuItem("Clear"));
        mi.setOnAction((_) -> clearLog());
        // test
        mb.getMenus().add(m = new Menu("Test"));
        m.getItems().add(mi = new MenuItem("Run"));
        mi.setOnAction((_) -> runTest());

        SplitPane split = new SplitPane(table, log);
        split.setOrientation(Orientation.VERTICAL);

        BorderPane bp = new BorderPane(split);
        bp.setTop(new VBox(mb, tb));
        Scene scene = new Scene(bp, 1100, 500);

        stage.setTitle("Manual Test Runner");
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> {
            Map<String, String> args = getParameters().getNamed();
            String plan = args.get("plan");
            if (plan == null) {
                System.out.println("Loading default test plan: " + DEFAULT_TEST_PLAN);
                plan = DEFAULT_TEST_PLAN;
            }
            File f = new File(plan);
            loadTestPlan(f);
        });
    }

    private void loadTestPlan(File f) {
        try {
            List<String> lines = Files.readAllLines(f.toPath());
            table.getItems().setAll(lines.stream().map(DataRow::new).toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openTestPlan() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().setAll(TXT);
        fc.setSelectedExtensionFilter(TXT);
        fc.setInitialDirectory(new File("."));
        File f = fc.showOpenDialog(Utils.parentWindow(table));
        if (f != null) {
            loadTestPlan(f);
        }
    }

    private void clearLog() {
        log.clear();
    }

    private void runTest() {
        DataRow d = table.getSelectionModel().getSelectedItem();
        if (d != null) {
            TestRunner.execute(d.testClass, new TestRunner.Client() {
                @Override
                public void onProcessFinished(int exitCode, Throwable error, LocalDateTime time) {
                    String t = DATE_TIME_FMT.format(time);
                    String result;
                    if (error == null) {
                        result = (exitCode == 0) ? "Pass" : "Fail";
                    } else {
                        result = "Error";
                        error.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        d.status.set(result);
                        d.lastRun.set(t);
                    });
                }

                @Override
                public void onOutput(char ch, boolean stdout) {
                    Platform.runLater(() -> {
                        log.appendText(String.valueOf(ch));
                    });
                }
            });
        }
    }

    private static class DataRow {
        public final String testClass;
        public final StringProperty name = new SimpleStringProperty();
        public final StringProperty status = new SimpleStringProperty();
        public final StringProperty lastRun = new SimpleStringProperty();

        public DataRow(String testClass) {
            this.testClass = testClass;
            name.set(testClass);
        }
    }
}