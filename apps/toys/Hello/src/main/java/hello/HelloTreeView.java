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

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeView.EditEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

import com.sun.javafx.PlatformUtil;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.TextFieldTreeCell;

public class HelloTreeView extends Application implements InvalidationListener {

//    private final Node graphic1 = new ImageView(new Image("helloworld/about_16.png"));
//    private final Node graphic2 = new ImageView(new Image("helloworld/folder_16.png"));
//    private final Node graphic3 = new ImageView(new Image("helloworld/heart_16.png"));

    // String TreeItems
    private final TreeItem<String> root = new TreeItem<String>("Root node");

    private TreeItem<String> childNode1 = new TreeItem<String>("Child Node 1"/*, graphic1*/);
    private final TreeItem<String> childNode2 = new TreeItem<String>("Child Node 2"/*, graphic2*/);
    private TreeItem<String> childNode3 = new TreeItem<String>("Child Node 3"/*, graphic3*/);

    private TreeItem<String> childNode4 = new TreeItem<String>("Child Node 4");
    private TreeItem<String> childNode5 = new TreeItem<String>("Child Node 5");
    private TreeItem<String> childNode6 = new TreeItem<String>("Child Node 6");
    private TreeItem<String> childNode7 = new TreeItem<String>("Child Node 7");
    private TreeItem<String> childNode8 = new TreeItem<String>("Child Node 8");
    private TreeItem<String> childNode9 = new TreeItem<String>("Child Node 9");
    private TreeItem<String> childNode10 = new TreeItem<String>("Child Node 10");
    private TreeItem<String> childNode11 = new TreeItem<String>("Child Node 11");
    private TreeItem<String> childNode12 = new TreeItem<String>("Child Node 12");
    private TreeItem<String> childNode13 = new TreeItem<String>("Child Node 13");
    private TreeItem<String> childNode14 = new TreeItem<String>("Child Node 14");
    private TreeItem<String> childNode15 = new TreeItem<String>("Child Node 15");
    private TreeItem<String> childNode16 = new TreeItem<String>("Child Node 16");
    private TreeItem<String> childNode17 = new TreeItem<String>("Child Node 17");
    private TreeItem<String> childNode18 = new TreeItem<String>("Child Node 18");
    private TreeItem<String> childNode19 = new TreeItem<String>("Child Node 19");
    private TreeItem<String> childNode20 = new TreeItem<String>("Child Node 20");
    private TreeItem<String> childNode21 = new TreeItem<String>("Child Node 21");


    // Button TreeItems
    private final TreeItem<Button> btnRoot = new TreeItem<Button>(new Button("Root node"));

    private TreeItem<Button> btnChildNode1 = new TreeItem<Button>(new Button("Child Node 1"));
    private final TreeItem<Button> btnChildNode2 = new TreeItem<Button>(new Button("Child Node 2"));
    private TreeItem<Button> btnChildNode3 = new TreeItem<Button>(new Button("Child Node 3"));

    private TreeItem<Button> btnChildNode4 = new TreeItem<Button>(new Button("Child Node 4"));
    private TreeItem<Button> btnChildNode5 = new TreeItem<Button>(new Button("Child Node 5"));
    private TreeItem<Button> btnChildNode6 = new TreeItem<Button>(new Button("Child Node 6"));
    private TreeItem<Button> btnChildNode7 = new TreeItem<Button>(new Button("Child Node 7"));
    private TreeItem<Button> btnChildNode8 = new TreeItem<Button>(new Button("Child Node 8"));
    private TreeItem<Button> btnChildNode9 = new TreeItem<Button>(new Button("Child Node 9"));
    private TreeItem<Button> btnChildNode10 = new TreeItem<Button>(new Button("Child Node 10"));
    private TreeItem<Button> btnChildNode11 = new TreeItem<Button>(new Button("Child Node 11"));
    private TreeItem<Button> btnChildNode12 = new TreeItem<Button>(new Button("Child Node 12"));
    private TreeItem<Button> btnChildNode13 = new TreeItem<Button>(new Button("Child Node 13"));
    private TreeItem<Button> btnChildNode14 = new TreeItem<Button>(new Button("Child Node 14"));
    private TreeItem<Button> btnChildNode15 = new TreeItem<Button>(new Button("Child Node 15"));
    private TreeItem<Button> btnChildNode16 = new TreeItem<Button>(new Button("Child Node 16"));
    private TreeItem<Button> btnChildNode17 = new TreeItem<Button>(new Button("Child Node 17"));
    private TreeItem<Button> btnChildNode18 = new TreeItem<Button>(new Button("Child Node 18"));
    private TreeItem<Button> btnChildNode19 = new TreeItem<Button>(new Button("Child Node 19"));
    private TreeItem<Button> btnChildNode20 = new TreeItem<Button>(new Button("Child Node 20"));
    private TreeItem<Button> btnChildNode21 = new TreeItem<Button>(new Button("Child Node 21"));

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        // finish putting together tree
        root.setExpanded(true);
        childNode3.setExpanded(true);
        root.getChildren().setAll(childNode1, childNode2, childNode3);
        childNode3.getChildren().setAll(childNode4, childNode5, childNode6,
                childNode7, childNode8, childNode9,
                childNode10, childNode11, childNode12,
                childNode13, childNode14, childNode15,
                childNode16, childNode17, childNode18,
                childNode19, childNode20, childNode21);

        btnRoot.setExpanded(true);
        btnChildNode3.setExpanded(true);
        btnRoot.getChildren().setAll(btnChildNode1, btnChildNode2, btnChildNode3);
        btnChildNode3.getChildren().setAll(btnChildNode4, btnChildNode5, btnChildNode6,
                btnChildNode7, btnChildNode8, btnChildNode9,
                btnChildNode10, btnChildNode11, btnChildNode12,
                btnChildNode13, btnChildNode14, btnChildNode15,
                btnChildNode16, btnChildNode17, btnChildNode18,
                btnChildNode19, btnChildNode20, btnChildNode21);

        stage.setTitle("Hello TreeView");
        final Scene scene = new Scene(new Group(), 875, 350);
        scene.setFill(Color.LIGHTGRAY);
        Group root = (Group)scene.getRoot();

        // TabPane
        final TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPrefWidth(scene.getWidth());
        tabPane.setPrefHeight(scene.getHeight());

        InvalidationListener sceneListener = new InvalidationListener() {
            @Override public void invalidated(Observable ov) {
                tabPane.setPrefWidth(scene.getWidth());
                tabPane.setPrefHeight(scene.getHeight());
            }
        };
        scene.widthProperty().addListener(sceneListener);
        scene.heightProperty().addListener(sceneListener);

        // simple tree view example
        Tab simpleTab = new Tab("Simple");
        buildSimpleTab(simpleTab);
        tabPane.getTabs().add(simpleTab);

        // big tree view example
        Tab bigTreeTab = new Tab("Big Tree");
        buildBigTreeTab(bigTreeTab);
        tabPane.getTabs().add(bigTreeTab);

        // Button tree view
        Tab buttonTreeViewTab = new Tab("Button TreeView");
        buildButtonTreeViewTab(buttonTreeViewTab);
        tabPane.getTabs().add(buttonTreeViewTab);

        // checked treeview example
        Tab checkedTreeViewTab = new Tab("Checked");
        buildCheckedTreeViewTab(checkedTreeViewTab);
        tabPane.getTabs().add(checkedTreeViewTab);

        // editable tree view example
        Tab editingTab = new Tab("Editing");
        buildEditingTab(editingTab);
        tabPane.getTabs().add(editingTab);

        // file system Tab
        Tab fileSystemTab = new Tab("File Browser");
        buildFileSystemTab(fileSystemTab);
        tabPane.getTabs().add(fileSystemTab);

        root.getChildren().add(tabPane);

        stage.setScene(scene);
        stage.show();
    }

    public void invalidated(Observable observable) {
        System.out.println("Event: " + observable);
    }

    private void buildSimpleTab(Tab tab) {
        GridPane grid = new GridPane();
//        grid.setGridLinesVisible(true);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        final TreeView treeView = new TreeView(root);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
//        treeView.setMultipleSelectionAllowed(true);
//        treeView.setCellFactory(TextFieldCellFactory.treeView());
//        treeView.setOnEditCommit(new EventHandler<EditEvent>() {
//            @Override public void handle(EditEvent t) {
//                System.out.println("Edit on Tree: " + t + ", TreeItem: " + t.getTreeItem());
//                t.getTreeItem().setValue(t.getNewValue());
//            }
//        });
        treeView.setShowRoot(true);
//        treeView.setRoot(root);
        grid.add(treeView, 0, 0, 1, 5);
        GridPane.setVgrow(treeView, Priority.ALWAYS);
        GridPane.setHgrow(treeView, Priority.ALWAYS);
        // --- simple treeview

        // control buttons
        Button rootToggle = new Button("Toggle Root");
        rootToggle.setTooltip(new Tooltip("Adds/removes the root node from the TreeView"));
        rootToggle.setOnAction(new EventHandler() {
            public void handle(Event t) {
                if (treeView.getRoot() == null) {
                    treeView.setRoot(root);
                } else {
                    treeView.setRoot(null);
                }
            }
        });
        grid.add(rootToggle, 1, 0);

        Button disabledToggle = new Button("Toggle Disabled");
        disabledToggle.setOnAction(new EventHandler() {
            public void handle(Event t) {
                treeView.setDisable(! treeView.isDisabled());
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
        treeView.getSelectionModel().selectedIndexProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                System.out.println("SelectedIndex: " + treeView.getSelectionModel().getSelectedIndex());
            }
        });
//        treeView.getSelectionModel().addChangeListener(SelectionModel.SELECTED_ITEM, this);
        treeView.getSelectionModel().getSelectedIndices().addListener(new ListChangeListener<Integer>() {
            public void onChanged(Change<? extends Integer> change) {
                while (change.next()) {
                    System.out.println("SelectedIndices: " + change.getList() +
                            ", removed: " + change.getRemoved() +
                            ", addedFrom: " + change.getFrom() +
                            ", addedTo: " + change.getTo());
                }
            }
        });
//        treeView.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<TreeItemModel>() {
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

        TreeItem<Object> root = new TreeItem<Object>("Root");
        root.setExpanded(true);

        for (int i = 0; i < numRows; i++) {
            TreeItem<Object> item = new TreeItem<Object>("Elem " + i);
            root.getChildren().add(item);

            List<TreeItem<Object>> newChildren = new ArrayList<TreeItem<Object>>();
            for (int j = 0; j < numColumns; j++) {
                String text = "Parent "+ i + " Subelement " + j;
                newChildren.add(new TreeItem<Object>(text));
            }
            item.getChildren().addAll(newChildren);
            item.setExpanded(true);
        }

        final TreeView treeView = new TreeView(root);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.setShowRoot(true);
        grid.add(treeView, 0, 0, 1, 5);
        GridPane.setVgrow(treeView, Priority.ALWAYS);
        GridPane.setHgrow(treeView, Priority.ALWAYS);
        // --- simple treeview

        // --- control buttons
        tab.setContent(grid);
    }

    private void buildButtonTreeViewTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        final TreeView treeView = new TreeView(btnRoot);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.setShowRoot(true);
        grid.add(treeView, 0, 0, 1, 5);
        GridPane.setVgrow(treeView, Priority.ALWAYS);
        GridPane.setHgrow(treeView, Priority.ALWAYS);

        tab.setContent(grid);
    }

    private void buildEditingTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        final TreeView<String> treeView = new TreeView<String>(root);
        treeView.setEditable(true);
        treeView.setShowRoot(true);
//        treeView.setCellFactory(new Callback<TreeView<String>, TreeCell<String>>() {
//            public TreeCell<String> call(TreeView<String> p) {
//                return new TextFieldTreeCellImpl(new Callback<String, String>() {
//                    public String call(String p) {
//                        return p;
//                    }
//                });
//            }
//        });
        treeView.setCellFactory(TextFieldTreeCell.forTreeView());
        treeView.setOnEditCommit(new EventHandler<TreeView.EditEvent<String>>() {
            public void handle(EditEvent<String> t) {
                System.out.println("edit commit: " + t.getNewValue());
            }
        });

        grid.add(treeView, 0, 0, 1, 5);
        GridPane.setVgrow(treeView, Priority.ALWAYS);
        GridPane.setHgrow(treeView, Priority.ALWAYS);
        // --- simple treeview

        tab.setContent(grid);
    }


    private void buildCheckedTreeViewTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        final Node graphic1 = new ImageView(new Image("hello/about_16.png"));
        final Node graphic2 = new ImageView(new Image("hello/folder_16.png"));
        final Node graphic3 = new ImageView(new Image("hello/heart_16.png"));

        final TreeItem<String> root = new CheckBoxTreeItem<String>("Root node");
        root.setExpanded(true);

        TreeItem<String> childNode1 = new CheckBoxTreeItem<String>("Child Node 1", graphic1);
        final TreeItem<String> childNode2 = new CheckBoxTreeItem<String>("Child Node 2", graphic2);
        TreeItem<String> childNode3 = new CheckBoxTreeItem<String>("Child Node 3", graphic3);
        root.getChildren().setAll(childNode1, childNode2, childNode3);

        TreeItem<String> childNode4 = new CheckBoxTreeItem<String>("Child Node 4");
        TreeItem<String> childNode5 = new CheckBoxTreeItem<String>("Child Node 5");
        TreeItem<String> childNode6 = new CheckBoxTreeItem<String>("Child Node 6");
        TreeItem<String> childNode7 = new CheckBoxTreeItem<String>("Child Node 7");
        TreeItem<String> childNode8 = new CheckBoxTreeItem<String>("Child Node 8");
        TreeItem<String> childNode9 = new CheckBoxTreeItem<String>("Child Node 9");
        TreeItem<String> childNode10 = new CheckBoxTreeItem<String>("Child Node 10");
        TreeItem<String> childNode11 = new CheckBoxTreeItem<String>("Child Node 11");
        TreeItem<String> childNode12 = new CheckBoxTreeItem<String>("Child Node 12");
        TreeItem<String> childNode13 = new CheckBoxTreeItem<String>("Child Node 13");
        TreeItem<String> childNode14 = new CheckBoxTreeItem<String>("Child Node 14");
        TreeItem<String> childNode15 = new CheckBoxTreeItem<String>("Child Node 15");
        TreeItem<String> childNode16 = new CheckBoxTreeItem<String>("Child Node 16");
        TreeItem<String> childNode17 = new CheckBoxTreeItem<String>("Child Node 17");
        TreeItem<String> childNode18 = new CheckBoxTreeItem<String>("Child Node 18");
        TreeItem<String> childNode19 = new CheckBoxTreeItem<String>("Child Node 19");
        TreeItem<String> childNode20 = new CheckBoxTreeItem<String>("Child Node 20");
        TreeItem<String> childNode21 = new CheckBoxTreeItem<String>("Child Node 21");
        childNode3.getChildren().setAll(childNode4, childNode5, childNode6,
                childNode7, childNode8, childNode9,
                childNode10, childNode11, childNode12,
                childNode13, childNode14, childNode15,
                childNode16, childNode17, childNode18,
                childNode19, childNode20, childNode21);

        final TreeView treeView = new TreeView(root);
        treeView.setShowRoot(true);
        treeView.setLayoutX(25);
        treeView.setLayoutY(40);

        treeView.setCellFactory(CheckBoxTreeCell.forTreeView());

        grid.add(treeView, 0, 0);
        GridPane.setVgrow(treeView, Priority.ALWAYS);
        GridPane.setHgrow(treeView, Priority.ALWAYS);
        tab.setContent(grid);
    }


    private void buildFileSystemTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        TreeItem<File> root = null;
        if (PlatformUtil.isWindows()) {
            root = createNode(new File("c:/"));
        } else {
            root = createNode(new File("/"));
        }

        final TreeView treeView = new TreeView();
        treeView.setShowRoot(true);
        treeView.setRoot(root);

        grid.add(treeView, 0, 0);
        GridPane.setVgrow(treeView, Priority.ALWAYS);
        GridPane.setHgrow(treeView, Priority.ALWAYS);
        tab.setContent(grid);
    }

    private TreeItem<File> createNode(final File f) {
        // Warning: Ugly hack!
//        RT-27479 removed impl_* methods for RenderToImage and BufferedImage conversion
//        ImageIcon swingIcon = (ImageIcon) _fileSystemView.getSystemIcon(f);
//        Image image = SwingFXUtils.toFXImage((BufferedImage)swingIcon.getImage(), null);
//        ImageView imageView = new ImageView(image);

//        final TreeItem<File> node = new TreeItem<File>(f, imageView) {
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

    private static final class TextFieldTreeCellImpl<T> extends TreeCell<T> {
        private TextField textBox;

        private final Callback<String, T> onCommit;

        public TextFieldTreeCellImpl(Callback<String, T> onCommit) {
            this.onCommit = onCommit;
        }

        @Override
        public void startEdit() {
            super.startEdit();

            if (textBox == null) {
                createTextBox();
            }
            setText(null);
            setGraphic(textBox);
            textBox.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();

            setText((String)getItem());
            setGraphic(null);
        }

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textBox != null) {
                        textBox.setText(getString());
                    }
                    setText(null);
                    setGraphic(textBox);
                } else {
                    setText(getString());
                    setGraphic(null);
                }
            }
        }

        private void createTextBox() {
            textBox = new TextField(getString());
            textBox.setOnKeyReleased(new EventHandler<KeyEvent>() {
                @Override public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        commitEdit(onCommit.call(textBox.getText()));
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }
    }
}
