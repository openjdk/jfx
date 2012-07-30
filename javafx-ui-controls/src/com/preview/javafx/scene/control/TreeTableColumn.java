package com.preview.javafx.scene.control;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;

/**
 *
 * @author Jonathan
 */
public class TreeTableColumn<S, T> extends TableColumn<TreeItem<S>, T> {

    public TreeTableColumn() {
    }
    
    public TreeTableColumn(String text) {
        super(text);
    }
}
