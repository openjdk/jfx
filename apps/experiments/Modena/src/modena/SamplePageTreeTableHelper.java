/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
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
package modena;

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeSortMode;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.shape.SVGPath;
import javafx.util.Callback;

/**
 * Helper class for creating tree table views for testing
 */
public class SamplePageTreeTableHelper {
    private static final NumberFormat nf = NumberFormat.getNumberInstance();
    private static final DateFormat df = new SimpleDateFormat("EEE, MMM d, yyyy");
    private static final String FOLDER = "M8,2.001V0H0v6.002v1.688V14c0,1.105,0.895,2,2,2h12c"
            + "1.104,0,2-0.895,2-2V8.002v-2V2.001H8z M6,2L6,2v2.001h2h6V6H2V2H6z M14,14H2V8h1"
            + "2v0.002V14z";
    private static final String FILE = "M4.775,1.592h5.836c0.293,0,0.531,0.238,0.531,0.53v11"
            + ".673c0,0.293-0.237,0.53-0.531,0.53H2.122c-0.293,0-0.53-0.237-0.53-0.53v-9.02h3"
            + ".183V1.592z M0,4.245v10.611c0,0.586,0.475,1.061,1.061,1.061h10.611c0.586,0,1.0"
            + "61-0.475,1.061-1.061V1.061C12.733,0.475,12.258,0,11.672,0H4.245L0,4.245z";
    
    private static Node createFOLDER() { 
        SVGPath sp = new SVGPath();
        sp.setContent(FOLDER);
        return sp;
    }
    
    private static Node createFILE() { 
        SVGPath sp = new SVGPath();
        sp.setContent(FILE);
        return sp;
    }
    
    static TreeTableView createTreeTableView(int width) {
        TreeTableView treeTableView = buildFileBrowserTreeTableView();
        treeTableView.setSortMode(TreeSortMode.ONLY_FIRST_LEVEL);
        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeTableView.setPrefSize(width, 300);
        treeTableView.getSelectionModel().selectRange(5, 8);
        return treeTableView;
    }
    
    private static TreeTableView buildFileBrowserTreeTableView() {
        // create a simple String treeview
        TreeItem<File> root = new FileTreeItem(new File("/"));
        root.setExpanded(true);
        
        final TreeTableView<File> treeTableView = new TreeTableView<File>();
        treeTableView.setShowRoot(true);
        treeTableView.setRoot(root);
        
        // --- name column
        TreeTableColumn<File, String> nameColumn = new TreeTableColumn<File, String>("Name");
        nameColumn.setPrefWidth(300);
        nameColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<File, String>, ObservableValue<String>>() {
            @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<File, String> p) {
                File f = p.getValue().getValue();
                String text = f.getParentFile() == null ? "/" : f.getName();
                return new ReadOnlyObjectWrapper<String>(text);
            }
        });

        // --- size column
        TreeTableColumn<File, File> sizeColumn = new TreeTableColumn<File, File>("Size");
        sizeColumn.setPrefWidth(100);
        sizeColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<File, File>, ObservableValue<File>>() {
            @Override public ObservableValue<File> call(TreeTableColumn.CellDataFeatures<File, File> p) {
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
        lastModifiedColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<File, Date>, ObservableValue<Date>>() {
            @Override public ObservableValue<Date> call(TreeTableColumn.CellDataFeatures<File, Date> p) {
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
    
    private static ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
        File f = (File) TreeItem.getValue();
        if (f != null && f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
                for (File childFile : files) {
                    children.add(new FileTreeItem(childFile));
                }
                return children;
            }
        }
        return FXCollections.emptyObservableList();
    }
    
    private static class FileTreeItem extends TreeItem<File> {
        private boolean isLeaf;
        private boolean isFirstTimeChildren = true;
        private boolean isFirstTimeLeaf = true;

        public FileTreeItem(File value) {
            super(value);
            setGraphic(value.isFile() ? createFILE() : createFOLDER());
        }

        @Override public ObservableList<TreeItem<File>> getChildren() {
            if (isFirstTimeChildren) {
                isFirstTimeChildren = false;
                super.getChildren().setAll(buildChildren(this));
            }
            return super.getChildren();
        }

        @Override public boolean isLeaf() {
            if (isFirstTimeLeaf) {
                isFirstTimeLeaf = false;
                File f = (File) getValue();
                isLeaf = f.isFile();
            }
            return isLeaf;
        }
    }
}
