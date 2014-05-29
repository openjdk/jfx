/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.javafx.scenebuilder.kit.fxom.sampledata;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;

/**
 *
 */
class XYChartSampleData extends AbstractSampleData {
    
    private final List<XYChart.Series<Object,Object>> samples = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private Class<?> sampleXAxisClass;
    private Class<?> sampleYAxisClass;

    public static boolean isKnownXYChart(Object obj) {
        final boolean result;
        
        if (obj instanceof XYChart) {
            final Class<?> objClass = obj.getClass();
            result = (objClass == BarChart.class 
                    || objClass == AreaChart.class
                    || objClass == BubbleChart.class
                    || objClass == LineChart.class
                    || objClass == ScatterChart.class
                    || objClass == StackedBarChart.class
                    || objClass == StackedAreaChart.class);
        } else {
            result = false;
        }
        
        return result;
    }

    /*
     * AbstractSampleData
     */
    
    @Override
    public void applyTo(Object sceneGraphObject) {
        assert sceneGraphObject instanceof XYChart;
        
        @SuppressWarnings("unchecked")        
        final XYChart<Object,Object> xyChart = (XYChart<Object,Object>) sceneGraphObject;
        updateSamples(xyChart);
        xyChart.getData().clear();
        xyChart.getData().addAll(samples);
        if (xyChart.getXAxis().getClass() == CategoryAxis.class) {
            final CategoryAxis axis = (CategoryAxis)(Axis<?>) xyChart.getXAxis();
            axis.getCategories().setAll(categories);
        }
        if (xyChart.getYAxis().getClass() == CategoryAxis.class) {
            final CategoryAxis axis = (CategoryAxis)(Axis<?>) xyChart.getYAxis();
            axis.getCategories().setAll(categories);
        }
    }
    
    @Override
    public void removeFrom(Object sceneGraphObject) {
        assert sceneGraphObject instanceof XYChart;
        
        @SuppressWarnings("unchecked")        
        final XYChart<Object,Object> xyChart = (XYChart<Object,Object>) sceneGraphObject;
        xyChart.getData().clear();
        if (xyChart.getXAxis().getClass() == CategoryAxis.class) {
            final CategoryAxis axis = (CategoryAxis)(Axis<?>) xyChart.getXAxis();
            axis.getCategories().clear();
        }
        if (xyChart.getYAxis().getClass() == CategoryAxis.class) {
            final CategoryAxis axis = (CategoryAxis)(Axis<?>) xyChart.getYAxis();
            axis.getCategories().clear();
        }
    }
    
    
    /*
     * Private
     */
    
    private void updateSamples(XYChart<?,?> xyChart) {
        
        final Class<?> xAxisClass = xyChart.getXAxis().getClass();
        final Class<?> yAxisClass = xyChart.getYAxis().getClass();
        
        if ((xAxisClass != sampleXAxisClass) || (yAxisClass != sampleYAxisClass)) {
            sampleXAxisClass = xAxisClass;
            sampleYAxisClass = yAxisClass;
            
            for (int i = 0; i < 3; i++) {
                final XYChart.Series<Object, Object> serie = new XYChart.Series<>();
                for (int j = 0; j < 10; j++) {
                    final Object xValue = makeValue(sampleXAxisClass, i);
                    final Object yValue = makeValue(sampleYAxisClass, i);
                    final XYChart.Data<Object, Object> data = new XYChart.Data<>(xValue, yValue);
                    serie.getData().add(data);

                }
                samples.add(serie);
            }
            
            categories.clear();
            if ((sampleXAxisClass == CategoryAxis.class) || (sampleYAxisClass == CategoryAxis.class)) {
                for (int j = 0; j < 10; j++) {
                    categories.add(String.valueOf(2000 + j));
                }
            }
        }
    }
    
    private Object makeValue(Class<?> axisClass, int index) {
        final Object result;
        
        if (axisClass == NumberAxis.class) {
            result = Math.random() * 100.0;
        } else if (axisClass == CategoryAxis.class) {
            result = String.valueOf(2000 + index);
        } else {
            assert false : "Unexpected Axis subclass" + axisClass;
            result = String.valueOf(index);
        }
        
        return result;
    }
}
