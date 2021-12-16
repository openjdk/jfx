/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;

public class TabPaneSkinShim {

    public static ContextMenu getTabsMenu(TabPaneSkin tpSkin) {
        return tpSkin.test_getTabsMenu();
    }

    public static void disableAnimations(TabPaneSkin tpSkin) {
        tpSkin.test_disableAnimations();
    }

    public static List<Node> getTabHeaders(TabPane tabPane) {
        StackPane headersRegion = (StackPane) tabPane.lookup(".headers-region");
        return headersRegion.getChildren();
    }

    public static double getHeaderAreaScrollOffset(TabPane tabPane) {
        TabPaneSkin skin = (TabPaneSkin) tabPane.getSkin();
        return skin.test_getHeaderAreaScrollOffset();
    }

    public static void setHeaderAreaScrollOffset(TabPane tabPane, double offset) {
        TabPaneSkin skin = (TabPaneSkin) tabPane.getSkin();
        skin.test_setHeaderAreaScrollOffset(offset);
    }

    public static boolean isTabsFit(TabPane tabPane) {
        TabPaneSkin skin = (TabPaneSkin) tabPane.getSkin();
        return skin.test_isTabsFit();
    }

}
