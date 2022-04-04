/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class LineGraphBoundsSample extends Application {

    @Override
    public void start(Stage primaryStage) {
        NumberAxis yAxis = new NumberAxis();
        NumberAxis xAxis = new NumberAxis();
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);

        DoubleProperty lowerBound = new SimpleDoubleProperty(0);
        DoubleProperty upperBound = new SimpleDoubleProperty(2);

        final ComboBox<String> axisSelection = new ComboBox<>(FXCollections.observableArrayList("X-Axis", "Y-Axis"));
        axisSelection.getSelectionModel().selectedIndexProperty().addListener((o, ov, nv) -> {
            switch (nv.intValue()) {
                case 0:
                    lineChart.setAxisSortingPolicy(LineChart.SortingPolicy.X_AXIS);
                    lineChart.setData(createData(LineChart.SortingPolicy.X_AXIS));
                    lineChart.getXAxis().setAutoRanging(false);
                    lineChart.getYAxis().setAutoRanging(true);
                    ((NumberAxis) lineChart.getXAxis()).lowerBoundProperty().bind(lowerBound);
                    ((NumberAxis) lineChart.getXAxis()).upperBoundProperty().bind(upperBound);
                    ((NumberAxis) lineChart.getYAxis()).lowerBoundProperty().unbind();
                    ((NumberAxis) lineChart.getYAxis()).upperBoundProperty().unbind();
                    break;
                case 1:
                    lineChart.setAxisSortingPolicy(LineChart.SortingPolicy.Y_AXIS);
                    lineChart.setData(createData(LineChart.SortingPolicy.Y_AXIS));
                    lineChart.getXAxis().setAutoRanging(true);
                    lineChart.getYAxis().setAutoRanging(false);
                    ((NumberAxis) lineChart.getXAxis()).lowerBoundProperty().unbind();
                    ((NumberAxis) lineChart.getXAxis()).upperBoundProperty().unbind();
                    ((NumberAxis) lineChart.getYAxis()).lowerBoundProperty().bind(lowerBound);
                    ((NumberAxis) lineChart.getYAxis()).upperBoundProperty().bind(upperBound);
                    break;
            }
            lowerBound.set(0);
            upperBound.set(2);
        });

        Button decrement = new Button("Decrease Bound");
        decrement.setOnAction(e -> {
            final double CHANGE_VALUE = -0.1;
            lowerBound.set(lowerBound.get() + CHANGE_VALUE);
            upperBound.set(upperBound.get() + CHANGE_VALUE);
        });

        Button increment = new Button("Increase Bound");
        increment.setOnAction(e -> {
            final double CHANGE_VALUE = 0.1;
            lowerBound.set(lowerBound.get() + CHANGE_VALUE);
            upperBound.set(upperBound.get() + CHANGE_VALUE);
        });

        final TextField lowerBoundTextField = new TextField();
        lowerBoundTextField.setEditable(false);
        lowerBoundTextField.textProperty().bind(lowerBound.asString("%.2f"));

        final TextField upperBoundTextField = new TextField();
        upperBoundTextField.setEditable(false);
        upperBoundTextField.textProperty().bind(upperBound.asString("%.2f"));

        final BorderPane root = new BorderPane();
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(5);
        gridPane.setHgap(5);
        gridPane.setPadding(new Insets(10));
        gridPane.add(new Label("Sorting Policy: "), 0, 0);
        gridPane.add(axisSelection, 1, 0);
        gridPane.add(new Label("Lower Bound: "), 2, 0);
        gridPane.add(new Label("Upper Bound: "), 2, 1);
        gridPane.add(lowerBoundTextField, 3, 0);
        gridPane.add(upperBoundTextField, 3, 1);
        final HBox buttons = new HBox(10, decrement, increment);
        buttons.setAlignment(Pos.CENTER);
        GridPane.setHalignment(buttons, HPos.CENTER);
        gridPane.add(buttons, 0, 2, 4,1);
        root.setTop(gridPane);
        root.setCenter(lineChart);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        axisSelection.getSelectionModel().select(0);
    }

    private ObservableList<XYChart.Series<Number,Number>> createData(LineChart.SortingPolicy sortingPolicy) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        List<Point2D> points = new ArrayList<>();
        switch (sortingPolicy) {
            case X_AXIS:
                points.addAll(List.of(
                        new Point2D(0.4, 0.5),
                        new Point2D(0.8, 0.5),
                        new Point2D(0.8, 1.0),
                        new Point2D(1.0, 1.0),
                        new Point2D(1.0, 0.8),
                        new Point2D(1.5, 0.8)
                ));
                break;
            case Y_AXIS:
                points.addAll(List.of(
                        new Point2D(0.5, 1.3),
                        new Point2D(0.5, 0.9),
                        new Point2D(1.0, 0.9),
                        new Point2D(1.0, 0.7),
                        new Point2D(0.8, 0.7),
                        new Point2D(0.8, 0.5)
                ));
                break;
        }
        points.forEach(point -> {
            series.getData().add(new XYChart.Data<>(point.getX(), point.getY()));
        });
        return FXCollections.observableArrayList(series);
    }
}
