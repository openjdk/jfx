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
package ensemble.samplepage;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import ensemble.samplepage.XYDataVisualizer.XYChartItem;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.WeakListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.ContextMenuEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;


public class XYDataVisualizer<X, Y> extends TreeTableView<XYChartItem<X, Y>> {

    XYChart<X, Y> chart;
    private Class<?> clzX;
    double minY, maxY;

    public XYDataVisualizer(final XYChart<X, Y> chart) {
        this.chart = chart;
        setShowRoot(false);
        XYChartItem<X, Y> root = new XYChartItem<>(chart.getData());
        setRoot(new MyTreeItem(root));
        setMinHeight(100);
        setMinWidth(100);

        parseData();

        if (!getRoot().getChildren().isEmpty()) {
            getRoot().getChildren().get(0).setExpanded(true);
        }

        chart.dataProperty().addListener((ObservableValue<? extends ObservableList<Series<X, Y>>> ov, ObservableList<Series<X, Y>> t, ObservableList<Series<X, Y>> t1) -> {
            setRoot(new MyTreeItem(new XYChartItem<X, Y>(t1)));
        });

        TreeTableColumn<XYChartItem<X, Y>, String> nameColumn = new TreeTableColumn<>("Name");
        nameColumn.setCellValueFactory((CellDataFeatures<XYChartItem<X, Y>, String> p) -> {
            if (p.getValue() != null) {
                return p.getValue().getValue().nameProperty();
            } else {
                return null;
            }
        });
        nameColumn.setEditable(true);
        nameColumn.setSortable(false);
        nameColumn.setMinWidth(70);

        TreeTableColumn<XYChartItem<X, Y>, X> xValueColumn = new TreeTableColumn<>("XValue");
        xValueColumn.setCellValueFactory((CellDataFeatures<XYChartItem<X, Y>, X> p) -> {
            if (p.getValue() != null) {
                return p.getValue().getValue().xValueProperty();
            } else {
                return null;
            }
        });
        xValueColumn.setCellFactory((TreeTableColumn<XYChartItem<X, Y>, X> p) -> new TextFieldTreeTableCell<XYChartItem<X, Y>, X>() {
            {
                setConverter(new StringConverter<X>() {
                    @Override
                    public String toString(X t) {
                        return t == null ? null : t.toString();
                    }

                    @Override
                    public X fromString(String string) {
                        if (string == null) {
                            return null;
                        }
                        try {
                            if (clzX.isAssignableFrom(String.class)) {
                                return (X) string;
                            } else if (clzX.isAssignableFrom(Double.class)) {
                                return (X) new Double(string);
                            } else if (clzX.isAssignableFrom(Integer.class)) {
                                return (X) new Integer(string);
                            }
                        } catch (NumberFormatException ex) {
                            Logger.getLogger(XYDataVisualizer.class.getName()).log(Level.FINE,
                                    "Failed to parse {0} to type {1}", new Object[]{string, clzX});
                            return getItem();
                        }
                        Logger.getLogger(XYDataVisualizer.class.getName()).log(Level.FINE,
                                "This valueX type is not supported: {0}", clzX);
                        return getItem();
                    }
                });
            }
        });
        xValueColumn.setEditable(true);
        xValueColumn.setSortable(false);
        xValueColumn.setMinWidth(50);

        TreeTableColumn<XYChartItem<X, Y>, Y> yValueColumn = new TreeTableColumn<>("YValue");
        yValueColumn.setCellValueFactory((CellDataFeatures<XYChartItem<X, Y>, Y> p) -> {
            if (p.getValue() != null) {
                return p.getValue().getValue().yValueProperty();
            } else {
                return null;
            }
        });
        yValueColumn.setCellFactory((TreeTableColumn<XYChartItem<X, Y>, Y> p) -> new TextFieldTreeTableCell<>(new StringConverter<Y>() {

            @Override
            public String toString(Y t) {
                return t == null ? null : t.toString();
            }

            @Override
            public Y fromString(String string) {
                if (string == null) {
                    return null;
                }
                Y y = (Y) new Double(string);
                return y;
            }
        }));
        yValueColumn.setEditable(true);
        yValueColumn.setSortable(false);
        yValueColumn.setMinWidth(50);

        TreeTableColumn<XYChartItem<X, Y>, Object> extraValueColumn = new TreeTableColumn<>("Extra Value");
        extraValueColumn.setCellValueFactory((CellDataFeatures<XYChartItem<X, Y>, Object> p) -> {
            if (p.getValue() != null) {
                return p.getValue().getValue().extraValueProperty();
            } else {
                return null;
            }
        });
        extraValueColumn.setMinWidth(100);
        extraValueColumn.setSortable(false);

        getColumns().setAll(nameColumn, xValueColumn, yValueColumn, extraValueColumn);

        setOnContextMenuRequested((ContextMenuEvent t) -> {
            Node node = t.getPickResult().getIntersectedNode();
            while (node != null && !(node instanceof TreeTableRow) && !(node instanceof TreeTableCell)) {
                node = node.getParent();
            }
            if (node instanceof TreeTableCell) {
                TreeTableCell tc = (TreeTableCell) node;
                if (tc.getItem() == null) {
                    getSelectionModel().clearSelection();
                } else {
                    getSelectionModel().select(tc.getIndex());
                }
            } else if (node instanceof TreeTableRow) {
                TreeTableRow tr = (TreeTableRow) node;
                if (tr.getItem() == null) {
                    getSelectionModel().clearSelection();
                } else {
                    getSelectionModel().select(tr.getIndex());
                }
            }
        });

        MenuItem insertDataItemMenuItem = new MenuItem("Insert data item");
        insertDataItemMenuItem.setDisable(!isEditable());
        insertDataItemMenuItem.setOnAction((ActionEvent t) -> {
            TreeItem<XYChartItem<X, Y>> selectedItem = getSelectionModel().getSelectedItem();
            if (selectedItem == null || selectedItem.getParent() == null) {
                return;
            }
            Object value = selectedItem.getValue().getValue();
            Object parentValue = selectedItem.getParent().getValue().getValue();
            if (value instanceof Series) {
                Series series = (Series) value;
                insertItem(series.getData());
            } else if (parentValue instanceof Series) {
                Series series = (Series) parentValue;
                insertItem(series.getData().indexOf(value), series.getData());
            }
        });

        MenuItem insertSeriesMenuitem = new MenuItem("Insert Series");
        insertSeriesMenuitem.setDisable(!isEditable());
        insertSeriesMenuitem.setOnAction((ActionEvent t) -> {
            TreeItem<XYChartItem<X, Y>> selectedItem = getSelectionModel().getSelectedItem();
            if (selectedItem == null) {
                return;
            }
            Object value = selectedItem.getValue().getValue();
            if (value instanceof ObservableList) {
                ObservableList parentList = (ObservableList) value;
                insertSeries(parentList.size(), parentList);
            } else {
                Object parentValue = selectedItem.getParent().getValue().getValue();
                if (parentValue instanceof ObservableList) {
                    ObservableList parentList = (ObservableList) parentValue;

                    insertSeries(parentList.indexOf(value), parentList);
                }
            }
        });

        MenuItem deleteItemMenuItem = new MenuItem("Delete item");
        deleteItemMenuItem.setDisable(!isEditable());
        deleteItemMenuItem.setOnAction((ActionEvent t) -> {
            TreeItem<XYChartItem<X, Y>> selectedItem = getSelectionModel().getSelectedItem();
            if (selectedItem == null) {
                return;
            }
            Object value = selectedItem.getValue().getValue();
            Object parentValue = selectedItem.getParent().getValue().getValue();
            if (parentValue instanceof ObservableList) {
                ((ObservableList) parentValue).remove(value);
            } else if (parentValue instanceof Series) {
                ((Series) parentValue).getData().remove(value);
            }
        });

        MenuItem removeAllDataMenuItem = new MenuItem("Remove all data");
        removeAllDataMenuItem.setDisable(!isEditable());
        removeAllDataMenuItem.setOnAction((ActionEvent t) -> {
            chart.getData().clear();
//            chart.setData(null);
        });

        MenuItem setNewDataMenuItem = new MenuItem("Set new data");
        setNewDataMenuItem.setDisable(!isEditable());
        setNewDataMenuItem.setOnAction((ActionEvent t) -> {
            chart.setData(generateData());
        });

        ContextMenu contextMenu = new ContextMenu(
                insertDataItemMenuItem,
                insertSeriesMenuitem,
                deleteItemMenuItem,
                removeAllDataMenuItem,
                setNewDataMenuItem);

        setContextMenu(contextMenu);
    }

    private ObservableList generateData() {
        seriesIndex = 1;
        categoryIndex = 1;
        ObservableList newData = FXCollections.observableArrayList();
        for (int i = 0; i < 3; i++) {
            insertSeries(newData);
        }
        return newData;
    }

    private int seriesIndex = 4;

    private void insertSeries(ObservableList parentList) {
        insertSeries(parentList.size(), parentList);
    }

    private void insertSeries(int index, ObservableList parentList) {
        ObservableList observableArrayList = FXCollections.observableArrayList();

        if (chart.getXAxis() instanceof CategoryAxis) {
            CategoryAxis xAxis = (CategoryAxis) chart.getXAxis();
            if (xAxis.getCategories().isEmpty()) {
                xAxis.getCategories().addAll("New category A", "New category B", "New category C");
            }
            for (String category : xAxis.getCategories()) {
                observableArrayList.add(new XYChart.Data(category, Math.random() * (maxY - minY) + minY));
            }
        } else if (chart.getXAxis() instanceof NumberAxis) {
            NumberAxis xAxis = (NumberAxis) chart.getXAxis();
            double lower = xAxis.getLowerBound();
            double upper = xAxis.getUpperBound();
            double x = lower;
            while (x < upper - xAxis.getTickUnit()) {
                x += Math.random() * xAxis.getTickUnit() * 2;
                observableArrayList.add(new XYChart.Data(x, Math.random() * (maxY - minY) + minY));
            }
        }

        parentList.add(index < 0 ? parentList.size() : index,
                new XYChart.Series<>("Series " + (seriesIndex++),
                observableArrayList));
    }

    private int categoryIndex = 1;

    public Data<Integer, Integer> insertItem(int index, ObservableList<Data> list) {
        Data prev = null, next = null;
        if (index >= 0 && index < list.size()) {
            next = list.get(index);
        }
        if (index > 0) {
            prev = list.get(index - 1);
        }
        if (index == -1) {
            index = list.size();
        }
        if (chart.getXAxis() instanceof NumberAxis) {
            NumberAxis xAxis = (NumberAxis) chart.getXAxis();
            double lower = prev == null
                    ? xAxis.getLowerBound() - 2 * xAxis.getTickUnit()
                    : ((Number) prev.getXValue()).doubleValue();
            double upper = next == null
                    ? xAxis.getUpperBound() + 2 * xAxis.getTickUnit()
                    : ((Number) next.getXValue()).doubleValue();
            Data item = new XYChart.Data<>(
                    Math.random() * (upper - lower) + lower,
                    Math.random() * (maxY - minY) + minY);
            list.add(index, item);
            return item;
        } else if (chart.getXAxis() instanceof CategoryAxis) {
            CategoryAxis xAxis = (CategoryAxis) chart.getXAxis();
            int lower = prev == null
                    ? -1
                    : xAxis.getCategories().indexOf(prev.getXValue());
            int upper = next == null
                    ? xAxis.getCategories().size()
                    : xAxis.getCategories().indexOf(next.getXValue());
            String category;
            if (upper - lower <= 1) {
                category = "New category " + (categoryIndex++);
                xAxis.getCategories().add(upper < 0 ? 0 : upper, category);
            } else {
                category = xAxis.getCategories().get(
                        (int) (Math.random() * (upper - lower - 1) + lower + 1));
            }
            Data item = new XYChart.Data<>(category,
                    Math.random() * (maxY - minY) + minY);
            list.add(index, item);
            return item;
        }
        return null;
    }

    public Data<Integer, Integer> insertItem(ObservableList<Data> list) {
        return insertItem(list.size(), list);
    }

    private void parseData() {
        boolean editable = true;
        for (Series<X, Y> series : chart.getData()) {
            for (XYChart.Data<X, Y> data : series.getData()) {
                X x = data.getXValue();
                if (x != null) {
                    clzX = x.getClass();
                }
                Y y = data.getYValue();
                if (y != null) {
                    if (chart.getYAxis() instanceof NumberAxis) {
                        minY = Math.min(minY, ((Number) y).doubleValue());
                        maxY = Math.max(maxY, ((Number) y).doubleValue());
                    }
                }
                if (data.getExtraValue() != null) {
                    editable = false;
                }
            }
        }
        if (chart.getYAxis() instanceof CategoryAxis) {
            editable = false;
        }
        setEditable(editable);
    }

    private static class MyTreeItem<X, Y> extends TreeItem<XYChartItem<X, Y>> {

        {
            expandedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean t, Boolean expanded) -> {
                if (expanded) {
                    ObservableList children = getValue().getChildren();
                    if (children != null) {
                        ListContentBinding.bind(getChildren(), children, (Object p) -> new MyTreeItem(new XYDataVisualizer.XYChartItem(p), false));
                        if (getChildren().size() == 1) {
                            getChildren().get(0).setExpanded(true);
                        }
                    }
                }
            });
        }

        @Override
        public boolean isLeaf() {
            return getValue().isLeaf();
        }

        public MyTreeItem(XYChartItem t) {
            this(t, true);
        }

        public MyTreeItem(XYChartItem t, boolean expand) {
            super(t);
            setExpanded(expand);
        }
    }

    public static class XYChartItem<X, Y> {

        private boolean leaf = true;
        private ObservableList children;
        private Object value;

        public XYChartItem(Object value) {
            this.value = value;
            if (value == null) {
                return;
            }
            name.set(value.toString());
            if (value instanceof ObservableList) {
                children = (ObservableList) value;
                leaf = false;
            } else if (value instanceof XYChart.Series) {
                XYChart.Series<X, Y> series = (XYChart.Series<X, Y>) value;
                name = series.nameProperty();
                children = series.getData();
                leaf = false;
            } else if (value instanceof XYChart.Data) {
                XYChart.Data<X, Y> data = (XYChart.Data<X, Y>) value;
                name.set("Data");
                xValue = data.XValueProperty();
                yValue = data.YValueProperty();
                extraValue.bindBidirectional(data.extraValueProperty());
            }
        }

        public ObservableList getChildren() {
            return children;
        }

        public boolean isLeaf() {
            return leaf;
        }

        private ObjectProperty<X> xValue = new SimpleObjectProperty<>();

        public ObjectProperty<X> xValueProperty() {
            return xValue;
        }

        private ObjectProperty<Y> yValue = new SimpleObjectProperty<>();

        public ObjectProperty<Y> yValueProperty() {
            return yValue;
        }

        private ObjectProperty<Object> extraValue = new SimpleObjectProperty<>();

        public ObjectProperty<Object> extraValueProperty() {
            return extraValue;
        }

        private StringProperty name = new SimpleStringProperty();

        public StringProperty nameProperty() {
            return name;
        }

        public Object getValue() {
            return value;
        }
    }

    private static class ListContentBinding<EF, ET> implements ListChangeListener<EF>, WeakListener {

        public static <EF, ET> Object bind(List<ET> list1, ObservableList<? extends EF> list2, Callback<EF, ET> converter) {
//            checkParameters(list1, list2);
            final ListContentBinding<EF, ET> contentBinding = new ListContentBinding<>(list1, converter);
            if (list1 instanceof ObservableList) {
                ((ObservableList) list1).setAll(contentBinding.convert(list2));
            } else {
                list1.clear();
                list1.addAll(contentBinding.convert(list2));
            }
            list2.addListener(contentBinding);
            return contentBinding;
        }

        private final WeakReference<List<ET>> listRef;
        private final Callback<EF, ET> converter;

        public ListContentBinding(List<ET> list, Callback<EF, ET> converter) {
            this.listRef = new WeakReference<>(list);
            this.converter = converter;
        }

        @Override
        public void onChanged(ListChangeListener.Change<? extends EF> change) {
            final List<ET> list = listRef.get();
            if (list == null) {
                change.getList().removeListener(this);
            } else {
                while (change.next()) {
                    if (change.wasPermutated()) {
                        list.subList(change.getFrom(), change.getTo()).clear();
                        list.addAll(change.getFrom(), convert(change.getList().subList(change.getFrom(), change.getTo())));
                    } else {
                        if (change.wasRemoved()) {
                            list.subList(change.getFrom(), change.getFrom() + change.getRemovedSize()).clear();
                        }
                        if (change.wasAdded()) {
                            list.addAll(change.getFrom(), convert(change.getAddedSubList()));
                        }
                    }
                }
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return listRef.get() == null;
        }

        @Override
        public int hashCode() {
            final List<ET> list = listRef.get();
            return (list == null)? 0 : list.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            final List<ET> list1 = listRef.get();
            if (list1 == null) {
                return false;
            }

            if (obj instanceof ListContentBinding) {
                final ListContentBinding<?, ?> other = (ListContentBinding<?, ?>) obj;
                final List<?> list2 = other.listRef.get();
                return list1 == list2;
            }
            return false;
        }

        private Collection<? extends ET> convert(List<? extends EF> addedSubList) {
            List<ET> res = new ArrayList<>(addedSubList.size());

            for (EF elem : addedSubList) {
                res.add(converter.call(elem));
            }

            return res;
        }
    }
}
