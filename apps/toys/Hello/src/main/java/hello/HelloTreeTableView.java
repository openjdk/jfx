/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package helloworld;

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeSortMode;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class HelloTreeTableView extends Application {

    private static NumberFormat nf = NumberFormat.getNumberInstance();
    private static DateFormat df = new SimpleDateFormat("EEE, MMM d, yyyy");

    public static void main(String[] args) {
        Application.launch(HelloTreeTableView.class, args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("TreeTable Samples");
        final Scene scene = new Scene(new Group(), 875, 700);
        scene.setFill(Color.LIGHTGRAY);
        Group root = (Group)scene.getRoot();

        root.getChildren().add(getContent(scene));

        stage.setScene(scene);
        stage.show();
    }

    public Node getContent(Scene scene) {
        // TabPane
        final TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPrefWidth(scene.getWidth());
        tabPane.setPrefHeight(scene.getHeight());

        tabPane.prefWidthProperty().bind(scene.widthProperty());
        tabPane.prefHeightProperty().bind(scene.heightProperty());

        // simple tree table view example
        Tab simpleTab = new Tab("Simple");
        buildSimpleTab(simpleTab);
        tabPane.getTabs().add(simpleTab);

        // big tree table view example
        Tab bigTab = new Tab("Big Tree");
        buildBigTreeTab(bigTab);
        tabPane.getTabs().add(bigTab);

        Tab fileSystemBrowserTab = new Tab("Filesystem Browser");
        buildFileSystemBrowserTab(fileSystemBrowserTab);
        tabPane.getTabs().add(fileSystemBrowserTab);

//        Tab cellSpanningTab = new Tab("Cell Spanning");
//        buildCellSpanningTab(cellSpanningTab);
//        tabPane.getTabs().add(cellSpanningTab);

        // move to file system tab
        tabPane.getSelectionModel().select(fileSystemBrowserTab);

        return tabPane;
    }

    private void buildSimpleTab(Tab tab) {
        GridPane grid = new GridPane();
//        grid.setGridLinesVisible(true);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        final Node graphic1 = new ImageView(new Image("hello/about_16.png"));
        final Node graphic2 = new ImageView(new Image("hello/folder_16.png"));
        final Node graphic3 = new ImageView(new Image("hello/heart_16.png"));

        final TreeItem<String> root = new TreeItem<String>("Root node", graphic1);
        final TreeItem<String> childNode1 = new TreeItem<String>("Child Node 1", graphic2);
        final TreeItem<String> childNode2 = new TreeItem<String>("Child Node 2", graphic3);
        final TreeItem<String> childNode3 = new TreeItem<String>("Child Node 3");

        root.setExpanded(true);
        root.getChildren().setAll(childNode1, childNode2, childNode3);

        TreeTableColumn<String, String> column = new TreeTableColumn<String, String>("Column");
        column.setCellValueFactory(new Callback<CellDataFeatures<String, String>, ObservableValue<String>>() {
            @Override public ObservableValue<String> call(CellDataFeatures<String, String> p) {
                return new ReadOnlyStringWrapper(p.getValue().getValue());
            }
        });
//        column.setCellValueFactory(new TreeItemPropertyValueFactory("value"));

        final TreeTableView<String> treeTableView = new TreeTableView<String>(root);
        treeTableView.getColumns().add(column);

//        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
//        treeView.setMultipleSelectionAllowed(true);
//        treeView.setCellFactory(TextFieldCellFactory.treeView());
//        treeView.setOnEditCommit(new EventHandler<EditEvent>() {
//            @Override public void handle(EditEvent t) {
//                System.out.println("Edit on Tree: " + t + ", TreeItem: " + t.getTreeItem());
//                t.getTreeItem().setValue(t.getNewValue());
//            }
//        });
        treeTableView.setShowRoot(true);
//        treeTableView.setRoot(root);
        grid.add(treeTableView, 0, 0, 1, 5);
        GridPane.setVgrow(treeTableView, Priority.ALWAYS);
        GridPane.setHgrow(treeTableView, Priority.ALWAYS);
        // --- simple treeTableView

        // control buttons
        Button rootToggle = new Button("Remove Root");
        rootToggle.setTooltip(new Tooltip("Adds/removes the root node from the TreeTableView"));
        rootToggle.setOnAction(new EventHandler() {
            public void handle(Event t) {
                if (treeTableView.getRoot() == null) {
                    treeTableView.setRoot(root);
                } else {
                    treeTableView.setRoot(null);
                }
            }
        });
        grid.add(rootToggle, 1, 0);

        Button disabledToggle = new Button("Toggle Disabled");
        disabledToggle.setOnAction(new EventHandler() {
            public void handle(Event t) {
                treeTableView.setDisable(! treeTableView.isDisabled());
            }
        });
        grid.add(disabledToggle, 1, 1);


        Button addNode = new Button("Add child To\nChild Node 2");
        addNode.setOnAction(new EventHandler() {
            public void handle(Event t) {
                childNode2.getChildren().add(new TreeItem<String>("New node 1"));
            }
        });
        grid.add(addNode, 1, 2);

        Button removeNode2 = new Button("Remove child from\nChild Node 2");
        removeNode2.setOnAction(new EventHandler() {
            public void handle(Event t) {
                if (childNode2.getChildren().isEmpty()) return;
                childNode2.getChildren().remove(childNode2.getChildren().size() - 1);
            }
        });
        grid.add(removeNode2, 1, 3);


        // --- control buttons

        tab.setContent(grid);

        //        // observation code for debugging the simple list view multiple selection
        treeTableView.getSelectionModel().selectedIndexProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                System.out.println("SelectedIndex: " + treeTableView.getSelectionModel().getSelectedIndex());
            }
        });
//        treeTableView.getSelectionModel().addChangeListener(SelectionModel.SELECTED_ITEM, this);
        treeTableView.getSelectionModel().getSelectedIndices().addListener(new ListChangeListener<Integer>() {
            public void onChanged(ListChangeListener.Change<? extends Integer> change) {
                while (change.next()) {
                    System.out.println("SelectedIndices: " + change.getList() +
                            ", removed: " + change.getRemoved() +
                            ", addedFrom: " + change.getFrom() +
                            ", addedTo: " + change.getTo());
                }
            }
        });
//        treeTableView.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<TreeItemModel>() {
//            public void onChanged(ObservableList<TreeItemModel> ol, List<TreeItemModel> removed, int addedFrom, int addedTo, boolean permutation) {
//                System.out.println("SelectedItems: " + ol + ", removed: " + removed + ", addedFrom: " + addedFrom + ", addedTo: " + addedTo);
//            }
//        });
    }


    private void buildBigTreeTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        final int numRows = 1000;
        final int numColumns = 50;

        TreeItem<String> root = new TreeItem<String>("Root");
        root.setExpanded(true);

        for (int i = 0; i < numRows; i++) {
            TreeItem<String> item = new TreeItem<String>("Elem " + i);
            root.getChildren().add(item);

            List<TreeItem<String>> newChildren = new ArrayList<TreeItem<String>>();
            for (int j = 0; j < numColumns; j++) {
                String text = "Parent "+ i + " Subelement " + j;
                newChildren.add(new TreeItem<String>(text));
            }
            item.getChildren().addAll(newChildren);
            item.setExpanded(true);
        }

        final TreeTableView<String> treeView = new TreeTableView<String>(root);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        treeView.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                System.out.println("selected index: " + newValue);
            }
        });

        TreeTableColumn<String, String> col = new TreeTableColumn<String, String>("Column");
        col.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<String,String>, ObservableValue<String>>() {
            @Override public ObservableValue<String> call(CellDataFeatures<String, String> param) {
                return new ReadOnlyStringWrapper(param.getValue().getValue());
            }
        });

        treeView.getColumns().add(col);

        treeView.setShowRoot(true);
        grid.add(treeView, 0, 0, 1, 5);
        GridPane.setVgrow(treeView, Priority.ALWAYS);
        GridPane.setHgrow(treeView, Priority.ALWAYS);
        // --- simple treeview

        // --- control buttons
        tab.setContent(grid);
    }


    private void buildFileSystemBrowserTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        final TreeTableView<File> treeTableView = buildFileBrowserTreeTableView();
        treeTableView.setSortMode(TreeSortMode.ONLY_FIRST_LEVEL);
        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeTableView.getSelectionModel().setCellSelectionEnabled(true);

        grid.add(treeTableView, 1, 0);
        GridPane.setVgrow(treeTableView, Priority.ALWAYS);
        GridPane.setHgrow(treeTableView, Priority.ALWAYS);

        // Control Panel
        VBox controlPanel = new VBox(5);
        grid.add(controlPanel, 2, 0);
        GridPane.setVgrow(controlPanel, Priority.ALWAYS);

        // selection mode
        final String SINGLE_ROW = "Single Row";
        final String SINGLE_CELL = "Single Cell";
        final String MULTIPLE_ROW = "Multiple Row";
        final String MULTIPLE_CELL = "Multiple Cell";
        final ChoiceBox<String> selectionModeBox = new ChoiceBox<String>(
                FXCollections.observableArrayList(SINGLE_ROW, SINGLE_CELL,
                                                  MULTIPLE_ROW, MULTIPLE_CELL));
        selectionModeBox.setValue(SINGLE_CELL);
        selectionModeBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue<? extends String> ov, String t, String t1) {
                TableSelectionModel sm = treeTableView.getSelectionModel();
                switch (selectionModeBox.getValue()) {
                    case SINGLE_ROW:    sm.setCellSelectionEnabled(false);
                                        sm.setSelectionMode(SelectionMode.SINGLE);
                                        break;
                    case SINGLE_CELL:   sm.setCellSelectionEnabled(true);
                                        sm.setSelectionMode(SelectionMode.SINGLE);
                                        break;
                    case MULTIPLE_ROW:  sm.setCellSelectionEnabled(false);
                                        sm.setSelectionMode(SelectionMode.MULTIPLE);
                                        break;
                    case MULTIPLE_CELL: sm.setCellSelectionEnabled(true);
                                        sm.setSelectionMode(SelectionMode.MULTIPLE);
                                        break;
                }
            }
        });
        controlPanel.getChildren().addAll(new Label("Selection Mode:"), selectionModeBox);

        // tree column
        final ChoiceBox<TreeTableColumn<File,?>> treeColumnChooser =
                new ChoiceBox<TreeTableColumn<File,?>>(treeTableView.getColumns());
        treeColumnChooser.setConverter(new StringConverter<TreeTableColumn<File,?>>() {
            @Override public String toString(TreeTableColumn<File,?> t) {
                return t.getText();
            }

            @Override public TreeTableColumn fromString(String string) {
                throw new UnsupportedOperationException("Not supported.");
            }
        });
        final SelectionModel<TreeTableColumn<File,?>> sm = treeColumnChooser.getSelectionModel();
        sm.select(treeTableView.getColumns().get(0));
        sm.selectedItemProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable o) {
                treeTableView.setTreeColumn(sm.getSelectedItem());
            }
        });
        controlPanel.getChildren().addAll(new Label("Tree Column:"), treeColumnChooser);

        // show root toggle
        final CheckBox showRootCheckBox = new CheckBox("Show Root");
        treeTableView.showRootProperty().bind(showRootCheckBox.selectedProperty());
        controlPanel.getChildren().addAll(showRootCheckBox);

        // print selection
        final CheckBox debugSelectionCheckBox = new CheckBox("Debug selection");
        final ListChangeListener<TreeTablePosition> cellsListener = new ListChangeListener<TreeTablePosition>() {
            @Override public void onChanged(Change<? extends TreeTablePosition> change) {
                for (TreeTablePosition cell : change.getList()) {
                    System.out.print("Cell [ row: " + cell.getRow() + ", column: " + cell.getColumn() + "] ");
                }
                System.out.println("");
            }
        };
        debugSelectionCheckBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) {
                ObservableList<TreeTablePosition<File,?>> selectedCells = treeTableView.getSelectionModel().getSelectedCells();
                if (debugSelectionCheckBox.isSelected()) {
                    selectedCells.addListener(cellsListener);
                } else {
                    selectedCells.removeListener(cellsListener);
                }
            }
        });
        controlPanel.getChildren().addAll(debugSelectionCheckBox);

        tab.setContent(grid);
    }

//    private void buildCellSpanningTab(Tab tab) {
//        GridPane grid = new GridPane();
//        grid.setPadding(new Insets(5, 5, 5, 5));
//        grid.setHgap(5);
//        grid.setVgap(5);
//
//        TreeTableView treeTableView = buildFileBrowserTreeTableView();
//
//        treeTableView.setSpanModel(new SpanModel() {
//            CellSpan SPAN_TWO_ROWS = new CellSpan(2, 1);
//            @Override public CellSpan getCellSpanAt(int rowIndex, int columnIndex,
//                    Object rowObject, TableColumnBase tableColumn) {
//
//                if (rowIndex % 3 == 0 && columnIndex == 2) return SPAN_TWO_ROWS;
//                else return null;
//            }
//        });
//
//        grid.add(treeTableView, 1, 0);
//        GridPane.setVgrow(treeTableView, Priority.ALWAYS);
//        GridPane.setHgrow(treeTableView, Priority.ALWAYS);
//
//        tab.setContent(grid);
//    }

    private TreeTableView buildFileBrowserTreeTableView() {
        // create a simple String treeview
        TreeItem<File> root = createNode(new File("/"));
        root.setExpanded(true);

        final TreeTableView<File> treeTableView = new TreeTableView<File>();
        treeTableView.setShowRoot(true);
        treeTableView.setRoot(root);

        // --- name column
        TreeTableColumn<File, String> nameColumn = new TreeTableColumn<File, String>("Name");
        nameColumn.setPrefWidth(300);
        nameColumn.setCellValueFactory(new Callback<CellDataFeatures<File, String>, ObservableValue<String>>() {
            @Override public ObservableValue<String> call(CellDataFeatures<File, String> p) {
                File f = p.getValue().getValue();
                String text = f.getParentFile() == null ? "/" : f.getName();
                return new ReadOnlyObjectWrapper<String>(text);
            }
        });
        // temporary cell factory to simulate expand / collapse instructions
//        nameColumn.setCellFactory(new Callback<TreeTableColumn<File, String>, TreeTableCell<File, String>>() {
//            @Override public TreeTableCell<File, String> call(TreeTableColumn<File, String> p) {
//                final TreeTableCell<File, String> cell = new TreeTableCell<File, String>() {
//                    @Override protected void updateItem(String item, boolean empty) {
//                        super.updateItem(item, empty);
//                        if (empty || item == null) {
//                            setText(null);
//                        } else {
//                            setText(item);
//                        }
//                    }
//                };
//                return cell;
//            }
//        });

        // --- size column
        TreeTableColumn<File, File> sizeColumn = new TreeTableColumn<File, File>("Size");
        sizeColumn.setPrefWidth(100);
        sizeColumn.setCellValueFactory(new Callback<CellDataFeatures<File, File>, ObservableValue<File>>() {
            @Override public ObservableValue<File> call(CellDataFeatures<File, File> p) {
                return new ReadOnlyObjectWrapper<File>(p.getValue().getValue());
            }
        });
        sizeColumn.setCellFactory(new Callback<TreeTableColumn<File, File>, TreeTableCell<File, File>>() {
            @Override public TreeTableCell<File, File> call(final TreeTableColumn<File, File> p) {
                return new TreeTableCell<File, File>() {
                    @Override protected void updateItem(File item, boolean empty) {
                        super.updateItem(item, empty);

                        TreeTableView treeTable = p.getTreeTableView();

                        // if the File is a directory, it has no size...
//                        ObservableList<TreeItem<File>> items = p.getTreeTableView().getItems();
                        if (getIndex() >= treeTable.getExpandedItemCount()) {
                            setText(null);
                        } else {
                            TreeItem<File> treeItem = treeTable.getTreeItem(getIndex());
                            if (item == null || empty || treeItem == null ||
                                    treeItem.getValue() == null || treeItem.getValue().isDirectory()) {
                                setText(null);
                            } else {
                                setText(nf.format(item.length()) + " KB");
                            }
                        }
                    }
                };
            }
        });
        sizeColumn.setComparator(new Comparator<File>() {
            @Override public int compare(File f1, File f2) {
                long s1 = f1.isDirectory() ? 0 : f1.length();
                long s2 = f2.isDirectory() ? 0 : f2.length();
                long result = s1 - s2;
                if (result < 0) {
                    return -1;
                } else if (result == 0) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });

        // --- modified column
        TreeTableColumn<File, Date> lastModifiedColumn = new TreeTableColumn<File, Date>("Last Modified");
        lastModifiedColumn.setPrefWidth(130);
        lastModifiedColumn.setCellValueFactory(new Callback<CellDataFeatures<File, Date>, ObservableValue<Date>>() {
            @Override public ObservableValue<Date> call(CellDataFeatures<File, Date> p) {
                return new ReadOnlyObjectWrapper<Date>(new Date(p.getValue().getValue().lastModified()));
            }
        });
        lastModifiedColumn.setCellFactory(new Callback<TreeTableColumn<File, Date>, TreeTableCell<File, Date>>() {
            @Override public TreeTableCell<File, Date> call(TreeTableColumn<File, Date> p) {
                return new TreeTableCell<File, Date>() {
                    @Override protected void updateItem(Date item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item == null || empty) {
                            setText(null);
                        } else {
                            setText(df.format(item));
                        }
                    }
                };
            }
        });

        treeTableView.getColumns().setAll(nameColumn, sizeColumn, lastModifiedColumn);

        return treeTableView;
    }

    private TreeItem<File> createNode(final File f) {
        final TreeItem<File> node = new TreeItem<File>(f) {
            private boolean isLeaf;
            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;

            @Override
            public ObservableList<TreeItem<File>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;
//                    System.out.println("build children: " + f);

                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }

            @Override
            public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    File f = (File) getValue();
//                    System.out.println("is leaf: " + f);
                    isLeaf = f.isFile();
                }

                return isLeaf;
            }
        };
        return node;
    }

    private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
        File f = (File) TreeItem.getValue();
        if (f != null && f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();

                for (File childFile : files) {
                    children.add(createNode(childFile));
                }

                return children;
            }
        }

        return FXCollections.emptyObservableList();
    }
}
