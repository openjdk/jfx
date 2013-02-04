/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.behavior;

import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;

public class TreeTableViewAnchorRetriever {

    // can only access the getAnchor method in TableCellBehavior from this package
    public static TreeTablePosition getAnchor(TreeTableView tableView) {
        return (TreeTablePosition) TableCellBehaviorBase.getAnchor(tableView, null);
    }
}
