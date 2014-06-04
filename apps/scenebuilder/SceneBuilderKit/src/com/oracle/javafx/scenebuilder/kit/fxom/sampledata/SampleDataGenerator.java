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

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;

/**
 *
 */
public class SampleDataGenerator {
    
    private final Map<FXOMObject, AbstractSampleData> sampleDataMap = new HashMap<>();
    
    public void assignSampleData(FXOMObject startObject) {
        assert startObject != null;
        
        final Object sceneGraphObject = startObject.getSceneGraphObject();
        final AbstractSampleData currentData = sampleDataMap.get(startObject);
        final AbstractSampleData newData;
        
        if (sceneGraphObject == null) {
            // startObject is unresolved
            newData = null;
        } else {
            final Class<?> sceneGraphClass = sceneGraphObject.getClass();
            if (sceneGraphClass == ChoiceBox.class) {
                final ChoiceBox<?> choiceBox = (ChoiceBox<?>) sceneGraphObject;
                if (choiceBox.getItems().isEmpty()) {
                    if (currentData instanceof ChoiceBoxSampleData) {
                        newData = currentData;
                    } else {
                        newData = new ChoiceBoxSampleData();
                    }
                } else {
                    newData = null;
                }
            } else if (sceneGraphClass == ComboBox.class) {
                final ComboBox<?> comboBox = (ComboBox<?>) sceneGraphObject;
                if (comboBox.getItems().isEmpty()) {
                    if (currentData instanceof ComboBoxSampleData) {
                        newData = currentData;
                    } else {
                        newData = new ComboBoxSampleData();
                    }
                } else {
                    newData = null;
                }
            } else if (sceneGraphClass == ListView.class) {
                final ListView<?> listView = (ListView<?>) sceneGraphObject;
                if (listView.getItems().isEmpty()) {
                    if (currentData instanceof ListViewSampleData) {
                        newData = currentData;
                    } else {
                        newData = new ListViewSampleData();
                    }
                } else {
                    newData = null;
                }
            } else if (sceneGraphClass == TreeView.class) {
                final TreeView<?> treeView = (TreeView<?>) sceneGraphObject;
                if (treeView.getRoot() == null) {
                    if (currentData instanceof TreeViewSampleData) {
                        newData = currentData;
                    } else {
                        newData = new TreeViewSampleData();
                    }
                } else {
                    newData = null;
                }
            } else if (sceneGraphClass == TableView.class) {
                final TableView<?> treeView = (TableView<?>) sceneGraphObject;
                if (TableViewSampleData.canApplyTo(treeView)) {
                    if (currentData instanceof TableViewSampleData) {
                        newData = currentData;
                    } else {
                        newData = new TableViewSampleData();
                    }
                } else {
                    newData = null;
                }
            } else if (sceneGraphClass == TreeTableView.class) {
                final TreeTableView<?> treeTableView = (TreeTableView<?>) sceneGraphObject;
                if (treeTableView.getRoot() == null) {
                    if (currentData instanceof TreeTableViewSampleData) {
                        newData = currentData;
                    } else {
                        newData = new TreeTableViewSampleData();
                    }
                } else {
                    newData = null;
                }
            } else if (sceneGraphClass == PieChart.class) {
                final PieChart pieChart = (PieChart) sceneGraphObject;
                if (pieChart.getData().isEmpty()) {
                    if (currentData instanceof PieChartSampleData) {
                        newData = currentData;
                    } else {
                        newData = new PieChartSampleData();
                    }
                } else {
                    newData = null;
                }
            } else if (XYChartSampleData.isKnownXYChart(sceneGraphObject)) {
                final XYChart<?,?> xyChart = (XYChart<?,?>) sceneGraphObject;
                if (xyChart.getData().isEmpty()) {
                    if (currentData instanceof XYChartSampleData) {
                        newData = currentData;
                    } else {
                        newData = new XYChartSampleData();
                    }
                } else {
                    newData = null;
                }
            } else {
                newData = null;
            }
        }
        
        if (newData == null) {
            if (currentData != null) {
                sampleDataMap.remove(startObject);
            }
        } else {
            newData.applyTo(sceneGraphObject);
            sampleDataMap.put(startObject, newData);
        }
        
        if (startObject instanceof FXOMInstance) {
            final FXOMInstance fxomInstance = (FXOMInstance) startObject;
            for (FXOMProperty p : fxomInstance.getProperties().values()) {
                if (p instanceof FXOMPropertyC) {
                    final FXOMPropertyC pc = (FXOMPropertyC) p;
                    for (FXOMObject v : pc.getValues()) {
                        assignSampleData(v);
                    }
                }
            }
        } else if (startObject instanceof FXOMCollection) {
            final FXOMCollection fxomCollection = (FXOMCollection) startObject;
            for (FXOMObject i : fxomCollection.getItems()) {
                assignSampleData(i);
            }
        } 
    }
    
    
    public void removeSampleData(FXOMObject startObject) {
        final AbstractSampleData currentData = sampleDataMap.get(startObject);
        if (currentData != null) {
            currentData.removeFrom(startObject.getSceneGraphObject());
        }
        
        if (startObject instanceof FXOMInstance) {
            final FXOMInstance fxomInstance = (FXOMInstance) startObject;
            for (FXOMProperty p : fxomInstance.getProperties().values()) {
                if (p instanceof FXOMPropertyC) {
                    final FXOMPropertyC pc = (FXOMPropertyC) p;
                    for (FXOMObject v : pc.getValues()) {
                        removeSampleData(v);
                    }
                }
            }
        } else if (startObject instanceof FXOMCollection) {
            final FXOMCollection fxomCollection = (FXOMCollection) startObject;
            for (FXOMObject i : fxomCollection.getItems()) {
                removeSampleData(i);
            }
        } 
    }
    
    /*
     * Private
     */
//    
//    private AbstractSampleData<?> makeSampleData(FXOMObject fxomObject) {
//        final Object obj = fxomObject.getSceneGraphObject();
//        assert obj == null;
//        
//        if (obj instanceof ListView) {
//            @SuppressWarnings("unchecked")
//            final ListView<Object> xyChart = (ListView)obj;
//            return visitList(xyChart);
//        } else if (obj instanceof TreeView) {
//            @SuppressWarnings("unchecked")
//            final TreeView<Object> xyChart = (TreeView)obj;
//            return visitTree(xyChart);
//        } else if (obj instanceof TableView) {
//            @SuppressWarnings("unchecked")
//            final TableView<Object> tableView = (TableView)obj;
//            return visitTable(tableView);
//        } else if (obj instanceof TableColumn) {
//            @SuppressWarnings("unchecked")
//            final TableColumn<Object,Object> tableColumn = 
//                (TableColumn<Object,Object>)obj;
//            return visitTableColumn(tableColumn);
//        } else if (obj instanceof XYChart && XYChartSeries.isKnownXYChart(obj)) {
//            @SuppressWarnings("unchecked")
//            final XYChart<Object,Object> chart = (XYChart<Object,Object>)obj;
//            return visitXYChart(chart);
//        } else if (obj instanceof PieChart) {
//            final PieChart chart = (PieChart)obj;
//            return visitPieChart(chart);
//        } else {
//            return Visit.DESCEND;
//        }
//    }
}
