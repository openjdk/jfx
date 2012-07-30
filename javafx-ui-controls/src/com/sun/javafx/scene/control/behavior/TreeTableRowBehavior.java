package com.sun.javafx.scene.control.behavior;

import com.preview.javafx.scene.control.TreeTableView;
import com.preview.javafx.scene.control.TreeTableRow;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 *
 * @author Jonathan
 */
public class TreeTableRowBehavior<T> extends CellBehaviorBase<TreeTableRow<T>> {

    public TreeTableRowBehavior(TreeTableRow<T> control) {
        super(control);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        
//        if (e.getClickCount() == 2) {
//            ObservableList<T> items = getControl().getTableView().getItems();
//            TreeItem<File> treeItem = items.get(getControl.getIndex());
//            treeItem.setExpanded(! treeItem.isExpanded());
//        }
        
        TreeTableView<T> tv = getControl().getTreeTableView();
        TreeItem treeItem = getControl().getTreeItem();
        int index = getControl().getIndex();
        MultipleSelectionModel sm = tv.getSelectionModel();
        boolean isAlreadySelected = sm.isSelected(index);

        tv.getSelectionModel().clearAndSelect(index);

        // handle editing, which only occurs with the primary mouse button
        if (e.getButton() == MouseButton.PRIMARY) {
            if (e.getClickCount() == 1 && isAlreadySelected) {
                tv.edit(treeItem);
            } else if (e.getClickCount() == 1) {
                // cancel editing
                tv.edit(null);
            } else if (e.getClickCount() == 2/* && ! getControl().isEditable()*/) {
                if (treeItem.isLeaf()) {
                    // attempt to edit
                    tv.edit(treeItem);
                } else {
                    // try to expand/collapse branch tree item
                    treeItem.setExpanded(! treeItem.isExpanded());
                }
            }
        }
    }
}
