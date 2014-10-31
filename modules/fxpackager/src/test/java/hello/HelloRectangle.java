/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import javafx.application.Application;
import javafx.application.Preloader;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Map;
import java.util.Optional;

/**
 */
public class HelloRectangle extends Application {

    private static class MessagePreloaderNotification implements Preloader.PreloaderNotification {
        String message;

        private MessagePreloaderNotification(String message) {
            this.message = message;
        }
        
        public String toString() {
            return message;
        }
    }
    
    @Override
    public void init() throws Exception {
        boolean wait;
        try {
            wait = System.getProperty("javafx.preloader") != null;
        } catch (SecurityException e) {
            wait = true;
        }
        if (wait) {
            notifyPreloader(new MessagePreloaderNotification("5..."));
            System.out.println("5...");
            Thread.sleep(1000);
            notifyPreloader(new MessagePreloaderNotification("4..."));
            System.out.println("4...");
            Thread.sleep(1000);
            notifyPreloader(new MessagePreloaderNotification("3..."));
            System.out.println("3...");
            Thread.sleep(1000);
            notifyPreloader(new MessagePreloaderNotification("2..."));
            System.out.println("2...");
            Thread.sleep(1000);
            notifyPreloader(new MessagePreloaderNotification("1..."));
            System.out.println("1...");
            Thread.sleep(1000);
            notifyPreloader(new MessagePreloaderNotification("GO!"));
            System.out.println("GO!");
        }
    }

    @Override public void start(Stage stage) {
        String title = "HelloRectangle";
        try {
            title = Optional.ofNullable(System.getProperty("app.preferences.id")).orElse("Hello Rectangle").replace("/", " " );
        } catch (SecurityException ignored) {}
        
        stage.setTitle(title);

        AnchorPane root = new AnchorPane();
        Scene scene = new Scene(root, 600, 450);

        Rectangle rect = new Rectangle();
        rect.setX(25);
        rect.setY(40);
        rect.setWidth(300);
        rect.setHeight(300);
        rect.setFill(Color.RED);
        AnchorPane.setTopAnchor(rect, 20.0);
        AnchorPane.setLeftAnchor(rect, 20.0);

        Parameters p = getParameters();

        ObservableList<Map.Entry<String, String>> paramsList = FXCollections.observableArrayList(p.getNamed().entrySet());
        
        TableColumn<Map.Entry<String, String>, String> paramsKey = new TableColumn<>("Param Name");
        paramsKey.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getKey()));
        TableColumn<Map.Entry<String, String>, String> paramsValue = new TableColumn<>("Param Value");
        paramsValue.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getValue()));
        
        TableView<Map.Entry<String, String>> paramsTable = new TableView<>(paramsList);
        paramsTable.getColumns().setAll(paramsKey, paramsValue);
        paramsTable.setPrefSize(250, 100);

        ListView<String> argsList = new ListView<>(FXCollections.observableArrayList(p.getUnnamed()));
        argsList.setPrefSize(250, 100);

        ListView<String> rawArgsList = new ListView<>(FXCollections.observableArrayList(p.getRaw()));
        rawArgsList.setPrefSize(250, 100);

        ObservableList<Map.Entry<Object, Object>> propsList;
        try {
            propsList = FXCollections.observableArrayList(System.getProperties().entrySet());
        } catch (SecurityException se) {
            propsList = FXCollections.emptyObservableList();
        }

        TableColumn<Map.Entry<Object, Object>, String> propsKey = new TableColumn<>("Param Name");
        propsKey.setCellValueFactory(e -> new SimpleStringProperty(Optional.ofNullable(e.getValue().getKey()).orElse("").toString()));
        propsKey.setPrefWidth(125);
        TableColumn<Map.Entry<Object, Object>, String> propsValue = new TableColumn<>("Param Value");
        propsValue.setCellValueFactory(e -> new SimpleStringProperty(Optional.ofNullable(e.getValue().getValue()).orElse("").toString()));
        propsValue.setPrefWidth(125);

        TableView<Map.Entry<Object, Object>> propsTable = new TableView<>(propsList);
        Label placeholder = new Label("Security prevents enumeration of system properties.");
        placeholder.setWrapText(true);
        propsTable.setPlaceholder(placeholder);
        propsTable.getColumns().setAll(propsKey, propsValue);
        propsTable.setPrefSize(250, 100);


        Accordion wierdAl = new Accordion(
                new TitledPane("Named Params", paramsTable),
                new TitledPane("Unnamed Params", argsList),
                new TitledPane("Raw Arguments", rawArgsList),
                new TitledPane("System Properties", propsTable)
        );
        wierdAl.setExpandedPane(wierdAl.getPanes().get(0));
        wierdAl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        AnchorPane.setTopAnchor(wierdAl, 0.0);
        AnchorPane.setRightAnchor(wierdAl, 0.0);
        AnchorPane.setBottomAnchor(wierdAl, 0.0);
        AnchorPane.setLeftAnchor(wierdAl, 340.0);

        root.getChildren().addAll(rect, wierdAl);

        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
