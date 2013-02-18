/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.behavior;

import javafx.scene.control.TreeView;

public class TreeViewAnchorRetriever {

    // can only access the getAnchor method in TreeCellBehavior from this package
    public static int getAnchor(TreeView treeView) {
        return TreeCellBehavior.getAnchor(treeView);
    }
}
