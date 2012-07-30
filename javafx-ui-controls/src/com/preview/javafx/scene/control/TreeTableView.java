/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.preview.javafx.scene.control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeView;

/**
 *
 * @author Jonathan
 */
public class TreeTableView<S> extends TreeView<S> {
    
    public TreeTableView() {
        getStyleClass().setAll("tree-table-view");
    }

    // this is the only publicly writable list for columns. This represents the
    // columns as they are given initially by the developer.
    private final ObservableList<TreeTableColumn<S, ?>> columns = FXCollections.observableArrayList();
    
    /**
     * The TableColumns that are part of this TableView. As the user reorders
     * the TableView columns, this list will be updated to reflect the current
     * visual ordering.
     *
     * <p>Note: to display any data in a TableView, there must be at least one
     * TableColumn in this ObservableList.</p>
     */
    public final ObservableList<TreeTableColumn<S, ?>> getColumns() {
        return columns;
    }
}
