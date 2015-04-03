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

import com.sun.javafx.perf.PerformanceTracker;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.chart.XYChart;
import javafx.util.Duration;

/*
 This is a very simple "Controller" for our MVC.
 It manipulates the data in our Model on a polled basis.

 Note: The manipulation of our Model data is done on a timeline which is
 run on the JavaFX event thread. 

 This polling could be done on a separate thread, but care would need to be 
 taken with the update of the model elements to ensure that all of the updates 
 end up on the event thread.
 */
public class Controller implements EventHandler<ActionEvent> {

    double tankVolume;

    double[] pumpRates = {
        0.0, // off
        3.0, // low
        5.0, // medium
        10.0, // high
    };

    PerformanceTracker tracker;
    Model model;

    public Controller(
            Model model,
            PerformanceTracker tracker
    ) {

        this.model = model;
        this.tracker = tracker;

        tankVolume = 25.0;
    }

    private long lastTime = 0;
    private long lastDataPoint = 0;
    private long seconds = 0;
    double pumpSpeed = 0;
    double foutA = 0.0;
    double foutB = 0.0;

    @Override
    public void handle(ActionEvent t) {
        long currtime = System.currentTimeMillis();
        double deltaTime = (currtime - lastTime) / 1000.0;
        if (lastTime > 0) {
            double tankChange;
            double frateP, frateA, frateB;

            frateP = model.getValveFlowRateProperty(Model.PUMP).get();
            frateA = model.getValveFlowRateProperty(Model.VALVE_A).get();
            frateB = model.getValveFlowRateProperty(Model.VALVE_B).get();

            tankChange = frateP * deltaTime;

            tankChange -= frateA * deltaTime;

            tankChange -= frateB * deltaTime;

            tankVolume += tankChange;

            if (tankVolume < 5.0) {
                tankVolume = 5.0;
            }
            if (tankVolume > 100.0) {
                tankVolume = 100.0;
            }

            model.getTankFillPercentProperty().set(tankVolume);

            double tankHighWater = model.getTankHighWaterPercentProperty().get();
            double tankLowWater = model.getTankLowWaterPercentProperty().get();

            if (tankVolume > tankHighWater) {
                // stop the pump
                frateP = 0.0;
                model.getValveFlowRateProperty(Model.PUMP).set(frateP);
            } else if (tankVolume < tankLowWater) {
                // below low water go to high
                frateP = 10.0;
                model.getValveFlowRateProperty(Model.PUMP).set(frateP);
            }

            if ((currtime - lastDataPoint) > (Industrial.historyInterval * 1000)) {
                ObservableList pump = model.getFlowRateHistory(Model.PUMP).getData();
                ObservableList va = model.getFlowRateHistory(Model.VALVE_A).getData();
                ObservableList vb = model.getFlowRateHistory(Model.VALVE_B).getData();
                ObservableList fp = model.getFillPercentHistory().getData();

                // our aprox 1 second data snapshot
                if (pump.size() > Industrial.historyPointsToKeep) {
                    pump.remove(0);
                    va.remove(0);
                    vb.remove(0);
                    fp.remove(0);
                }

                pump.add(new XYChart.Data(seconds, frateP));
                va.add(new XYChart.Data(seconds, frateA));
                vb.add(new XYChart.Data(seconds, frateB));
                fp.add(new XYChart.Data(seconds, tankVolume));

                seconds+=Industrial.historyInterval;
                lastDataPoint = currtime;

                int fpsValue = (int) Math.round(tracker.getAverageFPS());
                model.getFPSProperty().set(fpsValue);
            }
        } else {
            lastDataPoint = currtime;
        }
        lastTime = currtime;

    }

}
