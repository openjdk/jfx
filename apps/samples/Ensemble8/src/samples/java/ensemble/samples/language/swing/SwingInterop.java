/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates.
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

package ensemble.samples.language.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.DecimalFormat;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * SwingInterop
 *
 * Note using the browser might require setting the properties
 *  - http.proxyHost
 *  - http.proxyPort
 *
 * e.g. -Dhttp.proxyHost=webcache.mydomain.com -Dhttp.proxyPort=8080
 *
 */
public class SwingInterop extends JPanel {

    private static final int PANEL_WIDTH = 675;
    private static final int PANEL_HEIGHT = 400;
    private static final int TABLE_PANEL_HEIGHT = 100;
    private static JFXPanel chartFxPanel;
    private static JFXPanel browserFxPanel;
    private static SampleTableModel tableModel;
    private Chart chart;
    private Pane browser;

    void initUI() {
        tableModel = new SampleTableModel();
        // create javafx panel for charts
        chartFxPanel = new JFXPanel();
        chartFxPanel.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        // create javafx panel for browser
        browserFxPanel = new JFXPanel();

        //create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        //JTable
        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(Color.DARK_GRAY);
        DecimalFormatRenderer renderer = new DecimalFormatRenderer();
        renderer.setHorizontalAlignment(JLabel.RIGHT);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        JScrollPane tablePanel = new JScrollPane(table);
        tablePanel.setPreferredSize(new Dimension(PANEL_WIDTH,
                                                  TABLE_PANEL_HEIGHT));

        JPanel chartTablePanel = new JPanel();
        chartTablePanel.setLayout(new BorderLayout());

        //Split pane that holds both chart and table
        JSplitPane jsplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        jsplitPane.setTopComponent(chartTablePanel);
        jsplitPane.setBottomComponent(tablePanel);
        jsplitPane.setDividerLocation(410);
        chartTablePanel.add(chartFxPanel, BorderLayout.CENTER);

        tabbedPane.addTab("JavaFX Chart and Swing JTable", jsplitPane);
        tabbedPane.addTab("Web Browser", browserFxPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // create JavaFX scene
        Platform.runLater(new Runnable() {
            public void run() {
                createScene();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    final String ui =
                        "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
                    UIManager.setLookAndFeel(ui);
                } catch (Exception e) {}

                JFrame frame = new JFrame("JavaFX in Swing");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                SwingInterop app = new SwingInterop();
                app.initUI();

                frame.setContentPane(app);

                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    private void createScene() {
        chart = createBarChart();
        chartFxPanel.setScene(new Scene(chart));
        browser = createBrowser();
        browserFxPanel.setScene(new Scene(browser));
    }

    private BarChart createBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        ObservableList<String> xCategories =
            FXCollections.observableArrayList(tableModel.getColumnNames());
        xAxis.setCategories(xCategories);
        xAxis.setLabel("Year");

        double tickUnit = tableModel.getTickUnit();

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickUnit(tickUnit);
        yAxis.setLabel("Units Sold");

        final BarChart bChart = new BarChart(xAxis, yAxis,
                                             tableModel.getBarChartData());
        final TableModelListener tableListener = (TableModelEvent e) -> {
                if (e.getType() == TableModelEvent.UPDATE) {
                    final int row = e.getFirstRow();
                    final int column = e.getColumn();
                    final Object value =
                        ((SampleTableModel) e.getSource()).getValueAt(row,
                                                                      column);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            XYChart.Series<String, Number> s =
                                (XYChart.Series<String, Number>)
                                    bChart.getData().get(row);
                            BarChart.Data data = s.getData().get(column);
                            data.setYValue(value);
                        }
                    });
                }
            };
        tableModel.addTableModelListener(tableListener);
        return bChart;
    }

    private Pane createBrowser() {
        Double widthDouble = Double.valueOf(PANEL_WIDTH);
        Double heightDouble = Double.valueOf(PANEL_HEIGHT);
        WebView view = new WebView();
        view.setMinSize(widthDouble, heightDouble);
        view.setPrefSize(widthDouble, heightDouble);
        final WebEngine eng = view.getEngine();
        final String message = "Do you need to specify web proxy information?";
        final Label warningLabel = new Label(message);
        eng.load("http://www.oracle.com/us/index.html");

        final ChangeListener<Number> handler =
            (ObservableValue<? extends Number> observable,
             Number oldValue, Number newValue) -> {
                if (warningLabel.isVisible()) {
                    warningLabel.setVisible(false);
                }
            };
        eng.getLoadWorker().progressProperty().addListener(handler);

        final String url = "http://www.oracle.com/us/index.html";
        final TextField locationField = new TextField(url);
        locationField.setMaxHeight(Double.MAX_VALUE);
        Button goButton = new Button("Go");
        goButton.setMinWidth(Button.USE_PREF_SIZE);
        goButton.setDefaultButton(true);
        EventHandler<ActionEvent> goAction = (ActionEvent event) -> {
                eng.load(locationField.getText().startsWith("http://") ?
                         locationField.getText() :
                         "http://" + locationField.getText());
            };
        goButton.setOnAction(goAction);
        locationField.setOnAction(goAction);
        final ChangeListener<String> webListener =
            (ObservableValue<? extends String> observable,
             String oldValue, String newValue) -> {
                locationField.setText(newValue);
            };
        eng.locationProperty().addListener(webListener);
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5));
        grid.setVgap(5);
        grid.setHgap(5);
        GridPane.setConstraints(locationField, 0, 0, 1, 1,
                                HPos.CENTER, VPos.CENTER,
                                Priority.ALWAYS, Priority.SOMETIMES);
        GridPane.setConstraints(goButton, 1, 0);
        GridPane.setConstraints(view, 0, 1, 2, 1,
                                HPos.CENTER, VPos.CENTER,
                                Priority.ALWAYS, Priority.ALWAYS);
        GridPane.setConstraints(warningLabel, 0, 2, 2, 1,
                                HPos.CENTER, VPos.CENTER,
                                Priority.ALWAYS, Priority.SOMETIMES);
        grid.getColumnConstraints().addAll(
                new ColumnConstraints(widthDouble - 200, widthDouble - 200,
                                      Double.MAX_VALUE, Priority.ALWAYS,
                                      HPos.CENTER, true),
                new ColumnConstraints(40, 40, 40,
                                      Priority.NEVER, HPos.CENTER, true));
        grid.getChildren().addAll(locationField, goButton, warningLabel, view);
        return grid;
    }

    private static class DecimalFormatRenderer extends DefaultTableCellRenderer {
        private static final DecimalFormat formatter = new DecimalFormat("#.0");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {
            value = formatter.format((Number) value);
            return super.getTableCellRendererComponent(table, value, isSelected,
                                                       hasFocus, row, column);
        }
    }
}
