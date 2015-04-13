/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package industrial;

/*
 Todo: 
 Add emergency button
 Add help text ?
 */
import com.sun.javafx.perf.PerformanceTracker;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Industrial extends Application {

    Model model = new Model();
    Controller controller;
    Timeline controllerTimeline;

    Pump pump;

    Tank tank;

    Slider rateP, rateA, rateB;

    Slider highWater, lowWater;

    Valve valveA, valveB;

    Pipe pipeIn;
    Pipe pipeAIn, pipeAOut;
    Pipe pipeBIn, pipeBOut;

    Text fps;

    Button chartFlowButton;
    Button chartFillButton;

    Group flowChartGroup;
    Group chartFillGroup;
    Group chartFlowGroup;
    Group helpGroup;

    PerformanceTracker tracker;

    static final int layoutSizeX = 1280;
    static final int layoutSizeY = 720;

    static final int chartWidth = 800;
    static final int chartHeight = 500;

    static final int historyPointsToKeep = 60;
    static final int historyInterval = 1;

    Scale rootTransform = new Scale(1.0, 1.0, 0.0, 0.0);

    @Override
    public void start(Stage primaryStage) {

        Label title = new Label("Primary Holding Tank");
        title.setFont(new Font(40));
        title.setTranslateX(430);
        title.setTranslateY(20);

        //****************************
        pipeIn = new Pipe(new Path(
                new MoveTo(0, 550),
                new LineTo(440, 550)));

        pump = new Pump(model);
        Group pumpGroup = pump.getGroup();
        pumpGroup.setTranslateX(110);
        pumpGroup.setTranslateY(440);
        pumpGroup.setScaleX(.5);
        pumpGroup.setScaleY(.5);

        Label nameP = new Label("Feed Pump");
        nameP.setFont(new Font(22));
        nameP.setTranslateX(120);
        nameP.setTranslateY(450);

        Label flowRateP = new Label("Flow Rate:");
        flowRateP.setTranslateX(75);
        flowRateP.setTranslateY(635);

        rateP = new Slider(0.0, 10.0, 3);
        rateP.setShowTickLabels(true);
        rateP.setShowTickMarks(true);
        rateP.setMajorTickUnit(1.0);
        rateP.setBlockIncrement(1.0f);
        rateP.setSnapToTicks(true);
        rateP.setTranslateX(150);
        rateP.setTranslateY(630);

        //****************************
        tank = new Tank(400, 580);
        Group tankGroup = tank.getGroup();
        tankGroup.setTranslateX(440);
        tankGroup.setTranslateY(100);

        //****************************
        pipeAIn = new Pipe(new Path(
                new MoveTo(730, 590),
                new LineTo(1040, 590)));

        pipeAOut = new Pipe(new Path(
                new MoveTo(1135, 590),
                new LineTo(layoutSizeX, 590)));

        valveA = new Valve();
        Group valveAGroup = valveA.getGroup();
        valveAGroup.setScaleX(.5);
        valveAGroup.setScaleY(.5);
        valveAGroup.setTranslateX(1010);
        valveAGroup.setTranslateY(520);

        Label nameA = new Label("Discharge A");
        nameA.setFont(new Font(22));
        nameA.setTranslateX(1030);
        nameA.setTranslateY(500);

        Label flowRateA = new Label("Flow Rate:");
        flowRateA.setTranslateX(950);
        flowRateA.setTranslateY(635);

        rateA = new Slider(0.0, 5.0, 3);
        rateA.setShowTickLabels(true);
        rateA.setShowTickMarks(true);
        rateA.setMajorTickUnit(1.0);
        rateA.setBlockIncrement(1.0f);
        rateA.setSnapToTicks(true);
        rateA.setTranslateX(1025);
        rateA.setTranslateY(630);

        //****************************
        pipeBIn = new Pipe(new Path(
                new MoveTo(730, 390),
                new LineTo(1040, 390)));

        pipeBOut = new Pipe(new Path(
                new MoveTo(1135, 390),
                new LineTo(layoutSizeX, 390)));

        valveB = new Valve();
        Group valveBGroup = valveB.getGroup();
        valveBGroup.setScaleX(.5);
        valveBGroup.setScaleY(.5);
        valveBGroup.setTranslateX(1010);
        valveBGroup.setTranslateY(320);

        Label nameB = new Label("Discharge B");
        nameB.setFont(new Font(22));
        nameB.setTranslateX(1030);
        nameB.setTranslateY(300);

        Label flowRateB = new Label("Flow Rate:");
        flowRateB.setTranslateX(950);
        flowRateB.setTranslateY(435);

        rateB = new Slider(0.0, 5.0, 3);
        rateB.setShowTickLabels(true);
        rateB.setShowTickMarks(true);
        rateB.setMajorTickUnit(1.0);
        rateB.setBlockIncrement(1.0f);
        rateB.setMinorTickCount(4);
        rateP.setSnapToTicks(true);
        rateB.setTranslateX(1025);
        rateB.setTranslateY(430);

        //****************************
        fps = new Text("??? fps");
        fps.setFont(new Font(25));
        fps.setLayoutX(10);
        fps.setLayoutY(40);

        //****************************
        lowWater = new Slider(0, 90, 10);
        lowWater.setShowTickLabels(true);
        lowWater.setShowTickMarks(true);
        lowWater.setMajorTickUnit(10.0);
        lowWater.setBlockIncrement(1.0f);
        lowWater.setMinorTickCount(0);
        lowWater.setSnapToTicks(false);
        lowWater.setOrientation(Orientation.VERTICAL);
        lowWater.setTranslateX(390);
        lowWater.setTranslateY(150);
        lowWater.setPrefHeight(535);

        highWater = new Slider(10, 100, 10);
        highWater.setShowTickLabels(true);
        highWater.setShowTickMarks(true);
        highWater.setMajorTickUnit(10.0);
        highWater.setBlockIncrement(1.0f);
        highWater.setMinorTickCount(0);
        highWater.setSnapToTicks(false);
        highWater.setOrientation(Orientation.VERTICAL);
        highWater.setTranslateX(875);
        highWater.setTranslateY(95);
        highWater.setPrefHeight(535);

        //****************************

        Rectangle chartFlowBG = new Rectangle();
        chartFlowBG.setX(0);
        chartFlowBG.setY(0);
        chartFlowBG.setWidth(chartWidth);
        chartFlowBG.setHeight(chartHeight);
        chartFlowBG.setFill(Color.ANTIQUEWHITE);
        chartFlowBG.setOpacity(0.80);

        final NumberAxis xAxisFlow = new NumberAxis();
        final NumberAxis yAxisFlow = new NumberAxis();

        yAxisFlow.setLabel("Flow Rate");
        yAxisFlow.setAutoRanging(false);
        yAxisFlow.setLowerBound(0);
        yAxisFlow.setUpperBound(10);

        xAxisFlow.setLabel("Time (sec)");
        xAxisFlow.setAutoRanging(false);
        xAxisFlow.setLowerBound(0);
        xAxisFlow.setUpperBound(historyPointsToKeep * historyInterval);

        LineChart lineChartFlow = new LineChart(xAxisFlow, yAxisFlow);
        lineChartFlow.setTitle("Flow History");
        lineChartFlow.setPrefSize(chartWidth, chartHeight);
        lineChartFlow.setAnimated(false);
        lineChartFlow.getData().add(model.getFlowRateHistory(Model.PUMP));
        lineChartFlow.getData().add(model.getFlowRateHistory(Model.VALVE_A));
        lineChartFlow.getData().add(model.getFlowRateHistory(Model.VALVE_B));
        lineChartFlow.setCreateSymbols(false);

        chartFlowGroup = new Group(
                chartFlowBG,
                lineChartFlow
        );

        chartFlowGroup.setVisible(false);
        chartFlowGroup.setTranslateX(200);
        chartFlowGroup.setTranslateY(80);

        chartFlowButton = new Button("Flow History");
        chartFlowButton.setFont(new Font(18));
        chartFlowButton.setTranslateX(30.0);
        chartFlowButton.setTranslateY(80.0);
        chartFlowButton.setOnAction((ActionEvent event) -> {
            boolean visible = !chartFlowGroup.isVisible();
            chartFlowGroup.setVisible(visible);
            if (visible) {
                chartFillGroup.setVisible(false);
            }
        });

        //****************************

        Rectangle chartFillBG = new Rectangle();
        chartFillBG.setX(0);
        chartFillBG.setY(0);
        chartFillBG.setWidth(chartWidth);
        chartFillBG.setHeight(chartHeight);
        chartFillBG.setFill(Color.ANTIQUEWHITE);
        chartFillBG.setOpacity(0.80);

        final NumberAxis xAxisFill = new NumberAxis();
        final NumberAxis yAxisFill = new NumberAxis();

        yAxisFill.setLabel("Fill Percent");
        yAxisFill.setAutoRanging(false);
        yAxisFill.setLowerBound(0);
        yAxisFill.setUpperBound(100);

        xAxisFill.setLabel("Time (sec)");
        xAxisFill.setAutoRanging(false);
        xAxisFill.setLowerBound(0);
        xAxisFill.setUpperBound(historyPointsToKeep * historyInterval);

        LineChart lineChartFill = new LineChart(xAxisFill, yAxisFill);
        lineChartFill.setTitle("Fill History");
        lineChartFill.setPrefSize(chartWidth, chartHeight);
        lineChartFill.setAnimated(false);
        lineChartFill.getData().add(model.getFillPercentHistory());
        lineChartFill.setCreateSymbols(false);

        model.getFillPercentHistory().getData().addListener((ListChangeListener.Change c) -> {
            XYChart.Data get = (XYChart.Data) model.getFillPercentHistory().getData().get(0);
            if (get != null) {
                
                long low = (long) get.getXValue();
                long high = low + historyPointsToKeep * historyInterval;
                xAxisFill.setLowerBound(low);
                xAxisFill.setUpperBound(high);
                
                // a bit of a cheat, but these are in lock step
                // and it saves us a listener
                xAxisFlow.setLowerBound(low);
                xAxisFlow.setUpperBound(high);
            }
        });

        chartFillGroup = new Group(
                chartFillBG,
                lineChartFill
        );

        chartFillGroup.setVisible(false);
        chartFillGroup.setTranslateX(200);
        chartFillGroup.setTranslateY(80);

        chartFillButton = new Button("Fill History");
        chartFillButton.setFont(new Font(18));
        chartFillButton.setTranslateX(30.0);
        chartFillButton.setTranslateY(120.0);
        chartFillButton.setOnAction((ActionEvent event) -> {
            boolean visible = !chartFillGroup.isVisible();
            chartFillGroup.setVisible(visible);
            if (visible) {
                chartFlowGroup.setVisible(false);
            }
        });

        //****************************

        helpGroup = Help.createHelp(chartWidth, chartHeight);
        helpGroup.setVisible(false);
        helpGroup.setTranslateX(200);
        helpGroup.setTranslateY(80);

        Button helpButton = new Button("Help");
        helpButton.setFont(new Font(18));
        helpButton.setTranslateX(layoutSizeX - 100);
        helpButton.setTranslateY(80.0);
        helpButton.setOnAction((ActionEvent event) -> {
            boolean visible = !helpGroup.isVisible();
            helpGroup.setVisible(visible);
        });

        //****************************

        Rectangle backdrop = new Rectangle(0, 0, layoutSizeX, layoutSizeY);
        backdrop.setFill(Color.OLDLACE);

        //****************************
        Group root = new Group(
                backdrop,
                fps,
                pipeIn.getGroup(),
                pumpGroup,
                nameP, flowRateP, rateP,
                pipeAIn.getGroup(),
                pipeBIn.getGroup(),
                tankGroup,
                pipeAOut.getGroup(),
                valveAGroup,
                nameA, flowRateA, rateA,
                pipeBOut.getGroup(),
                valveBGroup,
                nameB, flowRateB, rateB,
                lowWater, highWater,
                chartFlowButton, chartFlowGroup,
                chartFillButton, chartFillGroup,
                helpButton, helpGroup,
                title
        );

        root.getTransforms().add(rootTransform);

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        System.out.println("Screen size is " + primaryScreenBounds);
        Scene scene = new Scene(root, layoutSizeX, layoutSizeY);

        tracker = PerformanceTracker.getSceneTracker(scene);

        controller = new Controller(
                model,
                tracker
        );

        model.getFPSProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            fps.setText("" + newValue.intValue() + " fps");
        });

        rateP.valueProperty().bindBidirectional(model.getValveFlowRateProperty(Model.PUMP));
        rateA.valueProperty().bindBidirectional(model.getValveFlowRateProperty(Model.VALVE_A));
        rateB.valueProperty().bindBidirectional(model.getValveFlowRateProperty(Model.VALVE_B));

        tank.getFillPercentageProperty().bind(model.getTankFillPercentProperty());
        tank.getLowWaterPercentageProperty().bind(model.getTankLowWaterPercentProperty());
        tank.getHighWaterPercentageProperty().bind(model.getTankHighWaterPercentProperty());

        model.getValveFlowRateProperty(Model.PUMP).addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            double lowThreshold = 0.5;
            double oldv = oldValue.doubleValue();
            double newv = newValue.doubleValue();
            boolean active = pump.getPumpActive();
            if (newv < lowThreshold && active) {
                pump.setPumpActive(false);
                pump.setPumpRatePercent(0.0);
                pipeIn.setFlow(false);
            } else {
                double ratePercent = newv / model.getValveFlowRateProperty(Model.PUMP).getMax();
                if (!active) {
                    pump.setPumpActive(true);
                    pipeIn.setFlow(true);
                }
                pump.setPumpRatePercent(ratePercent);
            }
        });

        model.getValveFlowRateProperty(Model.VALVE_A).addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            double lowThreshold = 0.5;
            double oldv = oldValue.doubleValue();
            double newv = newValue.doubleValue();
            if (oldv < lowThreshold && newv > lowThreshold) {
                pipeAOut.setFilled(true);
                pipeAIn.setFlow(true);
            } else if (newv <= lowThreshold && oldv > lowThreshold) {
                pipeAOut.setFilled(false);
                pipeAIn.setFlow(false);
            }
        });

        model.getValveFlowRateProperty(Model.VALVE_B).addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            double lowThreshold = 0.5;
            double oldv = oldValue.doubleValue();
            double newv = newValue.doubleValue();
            if (oldv < lowThreshold && newv > lowThreshold) {
                pipeBOut.setFilled(true);
                pipeBIn.setFlow(true);
            } else if (newv <= lowThreshold && oldv > lowThreshold) {
                pipeBOut.setFilled(false);
                pipeBIn.setFlow(false);
            }
        });

        lowWater.valueProperty().bindBidirectional(model.getTankLowWaterPercentProperty());
        highWater.valueProperty().bindBidirectional(model.getTankHighWaterPercentProperty());

        scene.widthProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) -> {
            double scale = (double) newSceneWidth / layoutSizeX;
            rootTransform.setX(scale);
        });
        scene.heightProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) -> {
            double scale = (double) newSceneHeight / layoutSizeY;
            rootTransform.setY(scale);
        });

        primaryStage.setTitle("Industrial Demo");
        primaryStage.setScene(scene);
        primaryStage.show();

        //this drives the Controller polling.
        final KeyFrame kf = new KeyFrame(Duration.millis(50), controller);
        controllerTimeline = new Timeline(kf);
        controllerTimeline.setCycleCount(Timeline.INDEFINITE);
        controllerTimeline.play();
    }

}
