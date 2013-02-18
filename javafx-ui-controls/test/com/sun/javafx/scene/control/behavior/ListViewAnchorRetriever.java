/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.behavior;

import javafx.scene.control.ListView;

public class ListViewAnchorRetriever {

    // can only access the getAnchor method in ListCellBehavior from this package
    public static int getAnchor(ListView listView) {
        return ListCellBehavior.getAnchor(listView);
    }
}
