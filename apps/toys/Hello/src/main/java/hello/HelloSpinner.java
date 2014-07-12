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
package hello;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class HelloSpinner extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        final Spinner spinner = new Spinner();

        // debug output to console
        spinner.valueProperty().addListener((o, oldValue, newValue) ->
                System.out.println("value changed: '" + oldValue + "' -> '" + newValue + "'"));
        spinner.getEditor().textProperty().addListener((o, oldValue, newValue) ->
                System.out.println("text changed: '" + oldValue + "' -> '" + newValue + "'"));

        // this lets us switch between the spinner value factories
        ComboBox<String> spinnerValueFactoryOptions =
                new ComboBox<>(FXCollections.observableArrayList("Integer", "Double", "List<String>", "Calendar"));
        spinnerValueFactoryOptions.getSelectionModel().selectedItemProperty().addListener((o, oldValue, newValue) -> {
            switch (newValue) {
                case "Integer": {
                    spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 10));
                    break;
                }

                case "List<String>": {
                    ObservableList<String> items = FXCollections.observableArrayList("Jonathan", "Julia", "Henry");
                    spinner.setValueFactory(new SpinnerValueFactory.ListSpinnerValueFactory<>(items));
                    break;
                }

                case "Calendar": {
                    spinner.setValueFactory(new SpinnerValueFactory.LocalDateSpinnerValueFactory());
                    break;
                }

                case "Double": {
                    spinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1.0, 0.5, 0.05));
                    break;
                }
            }
        });
        spinnerValueFactoryOptions.getSelectionModel().select(0);

        ComboBox<String> spinnerStyleClassOptions =
                new ComboBox<>(FXCollections.observableArrayList(
                        "Default (Arrows on right (Vertical))",
                        "Arrows on right (Horizontal)",
                        "Arrows on left (Vertical)",
                        "Arrows on left (Horizontal)",
                        "Split (Vertical)",
                        "Split (Horizontal)"));
        spinnerStyleClassOptions.getSelectionModel().selectedItemProperty().addListener((o, oldValue, newValue) -> {
            spinner.getStyleClass().removeAll(
                    Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL,
                    Spinner.STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL,
                    Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL,
                    Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL,
                    Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);

            switch (newValue) {
                case "Default (Arrows on right (Vertical))": break;

                case "Arrows on right (Horizontal)": {
                    spinner.getStyleClass().add(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL);
                    break;
                }

                case "Arrows on left (Vertical)": {
                    spinner.getStyleClass().add(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL);
                    break;
                }

                case "Arrows on left (Horizontal)": {
                    spinner.getStyleClass().add(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL);
                    break;
                }

                case "Split (Vertical)": {
                    spinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL);
                    break;
                }

                case "Split (Horizontal)": {
                    spinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
                    break;
                }
            }
        });
        spinnerStyleClassOptions.getSelectionModel().select(0);

        final CheckBox wrapAroundCheckBox = new CheckBox();
        wrapAroundCheckBox.selectedProperty().addListener((o, oldValue, newValue) ->
                spinner.getValueFactory().setWrapAround(newValue));

        final CheckBox editableCheckBox = new CheckBox();
        spinner.editableProperty().bind(editableCheckBox.selectedProperty());

        final CheckBox rtlCheckBox = new CheckBox();
        rtlCheckBox.selectedProperty().addListener((o, oldValue, newValue) ->
                spinner.setNodeOrientation(newValue ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.INHERIT));



        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        int row = 0;

        grid.add(new Label("Value Factory:"), 0, row);
        grid.add(spinnerValueFactoryOptions, 1, row++);

        grid.add(new Label("Style Class:"), 0, row);
        grid.add(spinnerStyleClassOptions, 1, row++);

        grid.add(new Label("Wrap around:"), 0, row);
        grid.add(wrapAroundCheckBox, 1, row++);

        grid.add(new Label("Editable:"), 0, row);
        grid.add(editableCheckBox, 1, row++);

        grid.add(new Label("Right-to-left:"), 0, row);
        grid.add(rtlCheckBox, 1, row++);

        grid.add(new Label("Spinner:"), 0, row);
        grid.add(spinner, 1, row);

        Scene scene = new Scene(grid, 350, 300);

        stage.setTitle("Hello Spinner");
        stage.setScene(scene);
        stage.show();
    }
}