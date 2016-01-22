/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control;

public class TreeTableViewShim {

    public static Class get_TreeTableViewArrayListSelectionModel_class() {
        return TreeTableView.TreeTableViewArrayListSelectionModel.class;
    }

    public static boolean instanceof_TreeTableViewArrayListSelectionModel(Object o) {
        return o instanceof TreeTableView.TreeTableViewArrayListSelectionModel;
    }

    public static <S> TableSelectionModel<TreeItem<S>>
            get_TreeTableViewArrayListSelectionModel(TreeTableView<S> treeTableView) {
        return new TreeTableView.TreeTableViewArrayListSelectionModel<>(treeTableView);
    }

    public static <S> void setSelectionModel(TreeTableView<S> treeTableView,
            TableSelectionModel<TreeItem<S>> model)
    {
        treeTableView.setSelectionModel((TreeTableView.TreeTableViewSelectionModel)model);
    }

    public static double get_contentWidth(TreeTableView ttv) {
        return ttv.contentWidth;
    }



}
