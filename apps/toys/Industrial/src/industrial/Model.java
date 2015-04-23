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

import java.util.ArrayList;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.scene.chart.XYChart;

/*
 This is a very simple "Model" for our MVC.
 It contains the data elements that are manipulated by our Contoller and 
 are presented by our View.
 */
public class Model {

    private final IntegerProperty fpsProp = new IntegerPropertyBase(0) {

        @Override
        public Object getBean() {
            return Model.this;
        }

        @Override
        public String getName() {
            return "FPS";
        }
    };

    static final int PUMP = 0;
    static final int VALVE_A = 1;
    static final int VALVE_B = 2;

    // Tank 
    private static final double initialFill = 50.0;
    private static final double initialHighWater = 90.0;
    private static final double initialLowWater = 10.0;

    private DoubleProperty fillPercentProp;
    private DoubleProperty highPercentProp;
    private DoubleProperty lowPercentProp;

    // Valve (A & B)
    //  Flow rate
    private final ArrayList<ValveRateProperty> valveFlowRates
            = new ArrayList<>();

    private final ArrayList<XYChart.Series> valveFlowRatesHistory
            = new ArrayList<>();

    private final XYChart.Series fillPercentHistory;

    public class ValveRateProperty extends DoublePropertyBase {

        double min, max;
        String name;

        public ValveRateProperty(String name, double min, double max, double start) {
            this.name = name;
            this.min = min;
            this.max = max;
            set(start);
        }

        @Override
        public void invalidated() {
            double current = get();
            if (current < min) {
                set(min);
            }
            if (current > max) {
                set(max);
            }
        }

        @Override
        public Object getBean() {
            return Model.this;
        }

        @Override
        public String getName() {
            return "valve" + name;
        }

        double getMin() {
            return min;
        }

        double getMax() {
            return max;
        }

    }

    public Model() {
        valveFlowRates.add(new ValveRateProperty("pump", 0.0, 10.0, 0.0));
        valveFlowRates.add(new ValveRateProperty("valveA", 0.0, 5.0, 5.0));
        valveFlowRates.add(new ValveRateProperty("valveB", 0.0, 5.0, 2.0));

        XYChart.Series p = new XYChart.Series();
        p.setName("pump");
        valveFlowRatesHistory.add(p);

        p = new XYChart.Series();
        p.setName("Valve A");
        valveFlowRatesHistory.add(p);

        p = new XYChart.Series();
        p.setName("Valve B");
        valveFlowRatesHistory.add(p);

        fillPercentHistory = new XYChart.Series();
        fillPercentHistory.setName("Fill Percent");
    }

    public DoubleProperty getTankFillPercentProperty() {
        if (fillPercentProp == null) {
            fillPercentProp = new DoublePropertyBase(initialFill) {

                @Override
                public void invalidated() {
                    if (get() < 0.0) {
                        set(0.0);
                    }
                    if (get() > 100.0) {
                        set(100.0);
                    }
                }

                @Override
                public Object getBean() {
                    return Model.this;
                }

                @Override
                public String getName() {
                    return "tankFillPercent";
                }

            };
        }
        return fillPercentProp;
    }

    public DoubleProperty getTankHighWaterPercentProperty() {
        if (highPercentProp == null) {
            highPercentProp = new DoublePropertyBase(initialHighWater) {

                @Override
                public void invalidated() {
                    double curr = get();
                    if (curr < lowPercentProp.get() + 10.0) {
                        set(lowPercentProp.get() + 10.0);
                    }
                    if (curr > 100.0) {
                        set(100.0);
                    }
                }

                @Override
                public Object getBean() {
                    return Model.this;
                }

                @Override
                public String getName() {
                    return "tankHighWaterPercent";
                }

            };
        }
        return highPercentProp;
    }

    public DoubleProperty getTankLowWaterPercentProperty() {
        if (lowPercentProp == null) {
            lowPercentProp = new DoublePropertyBase(initialLowWater) {

                @Override
                public void invalidated() {
                    double curr = get();
                    if (curr < 0.0) {
                        set(0.0);
                    }
                    if (curr > highPercentProp.get() - 10.0) {
                        set(highPercentProp.get() - 10.0);
                    }
                }

                @Override
                public Object getBean() {
                    return Model.this;
                }

                @Override
                public String getName() {
                    return "tankLowWaterPercent";
                }

            };
        }
        return lowPercentProp;
    }

    public ValveRateProperty getValveFlowRateProperty(int which) {
        if (which > valveFlowRates.size()) {
            return null;
        }
        return valveFlowRates.get(which);
    }

    public IntegerProperty getFPSProperty() {
        return fpsProp;
    }

    public XYChart.Series getFlowRateHistory(int which) {
        if (which > valveFlowRatesHistory.size()) {
            return null;
        }
        return valveFlowRatesHistory.get(which);
    }

    public XYChart.Series getFillPercentHistory() {
        return fillPercentHistory;
    }
}
