/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.behavior;

import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;

public class TableViewAnchorRetriever {

    // can only access the getAnchor method in TableCellBehavior from this package
    public static TablePosition getAnchor(TableView tableView) {
        return (TablePosition) TableCellBehaviorBase.getAnchor(tableView, null);
    }
}
