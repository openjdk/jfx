/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.util;

import javafx.css.Style;
import javafx.scene.control.skin.MenuBarSkin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javafx.collections.ObservableMap;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;

@SuppressWarnings("deprecation")
public class Deprecation {

    private Deprecation() {
        assert false;
    }

    public static final String CASPIAN_STYLESHEET = "com/sun/javafx/scene/control/skin/caspian/caspian.bss"; //NOI18N
    public static final String CASPIAN_HIGHCONTRAST_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/caspian/caspian-highContrast.css"; //NOI18N
    public static final String CASPIAN_EMBEDDED_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/caspian/caspian-embedded.css"; //NOI18N
    public static final String CASPIAN_EMBEDDED_HIGHCONTRAST_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/caspian/caspian-embedded-highContrast.css"; //NOI18N
    public static final String CASPIAN_EMBEDDED_QVGA_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/caspian/caspian-embedded-qvga.css"; //NOI18N
    public static final String CASPIAN_EMBEDDED_QVGA_HIGHCONTRAST_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/caspian/caspian-embedded-qvga-highContrast.css"; //NOI18N
    public static final String MODENA_STYLESHEET = "com/sun/javafx/scene/control/skin/modena/modena.bss"; //NOI18N
    public static final String MODENA_TOUCH_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/modena/modena-touch.css"; //NOI18N
    public static final String MODENA_HIGHCONTRAST_BLACKONWHITE_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/modena/modena-highContrast-blackOnWhite.css"; //NOI18N
    public static final String MODENA_HIGHCONTRAST_WHITEONBLACK_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/modena/modena-highContrast-whiteOnBlack.css"; //NOI18N
    public static final String MODENA_HIGHCONTRAST_YELLOWONBLACK_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/modena/modena-highContrast-yellowOnBlack.css"; //NOI18N
    public static final String MODENA_TOUCH_HIGHCONTRAST_BLACKONWHITE_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/modena/modena-touch-highContrast-blackOnWhite.css"; //NOI18N
    public static final String MODENA_TOUCH_HIGHCONTRAST_WHITEONBLACK_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/modena/modena-touch-highContrast-whiteOnBlack.css"; //NOI18N
    public static final String MODENA_TOUCH_HIGHCONTRAST_YELLOWONBLACK_STYLESHEET = "com/oracle/javafx/scenebuilder/kit/util/css/modena/modena-touch-highContrast-yellowOnBlack.css"; //NOI18N

    // Deprecated stuff in Node
//    // RT-21247 : Promote impl_getAllParentStylesheets to public API
    public static Group createGroupWithNullParentStylesheets() {
        System.err.println("Error: impl_getAllParentStylesheets is no longer publicly accessible");
        return new Group() {
//            @Override
//            public List<String> impl_getAllParentStylesheets() {
//                return null;
//            }
        };
    }

//    // RT-21096 : Promote impl_getStyleMap / impl_setStyleMap to public API
    public static void setStyleMap(Node node, ObservableMap<StyleableProperty<?>, List<javafx.css.Style>> map) {
        // node.impl_setStyleMap(map);
        System.err.println("Error: impl_setStyleMap is no longer publicly accessible");
    }

//    // RT-21096 : Promote impl_getStyleMap / impl_setStyleMap to public API
    public static Map<StyleableProperty<?>, List<Style>> getStyleMap(Node node) {
//        return node.impl_findStyles(null);
        System.err.println("Error: findStyles is no longer publicly accessible");
        return null;
    }

    public static void reapplyCSS(Parent parent, String stylesheetPath) {
        assert parent != null;

        final List<String> stylesheets = parent.getStylesheets();
        for (String s : new LinkedList<>(stylesheets)) {
            if (s.endsWith(stylesheetPath)) {
                final int index = stylesheets.indexOf(s);
                assert index != -1;
                stylesheets.remove(index);
                stylesheets.add(index, s);
                break;
            }
        }

        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Parent) {
                final Parent childParent = (Parent) child;
                reapplyCSS(childParent, stylesheetPath);
            } else if (child instanceof SubScene) {
                final SubScene childSubScene = (SubScene) child;
                reapplyCSS(childSubScene.getRoot(), stylesheetPath);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public static List<Style> getMatchingStyles(CssMetaData cssMetaData, Styleable styleable) {
//        return Node.impl_getMatchingStyles(cssMetaData, styleable);
        System.err.println("Error: impl_getMatchingStyles is no longer publicly accessible");
        return null;
    }

    // Deprecated stuff in Parent

    // Deprecated stuff in FXMLLoader
    // RT-21226 : Promote setStaticLoad to public API
    public static void setStaticLoad(FXMLLoader loader, boolean staticLoad) {
//        loader.impl_setStaticLoad(staticLoad);
        // System.err.println("Error: impl_setStaticLoad is no longer publicly accessible");
    }

    // RT-20184 : FX should provide a Parent.pick() routine
    public static Node pick(Node node, double sceneX, double sceneY) {
        Point2D p = node.sceneToLocal(sceneX, sceneY, true /* rootScene */);

        // check if the given node has the point inside it, or else we drop out
        if (!node.contains(p)) return null;

        // at this point we know that _at least_ the given node is a valid
        // answer to the given point, so we will return that if we don't find
        // a better child option
        if (node instanceof Parent) {
            // we iterate through all children (recursively). We don't stop
            // iteration when we hit the first child that also contains the bounds,
            // as we know that later nodes have a higher z-ordering, so they
            // should be picked before the earlier nodes.
            Node bestMatchingChild = null;
            for (Node child : ((Parent)node).getChildrenUnmodifiable()) {
                p = child.sceneToLocal(sceneX, sceneY, true /* rootScene */);
                if (child.contains(p)) {
                    bestMatchingChild = child;
                }
            }

            if (bestMatchingChild != null) {
                return pick(bestMatchingChild, sceneX, sceneY);
            }
        }

        return node;
    }

    // RT-19857 : Keeping menu in the Mac menu bar when there is no more stage
    public static void setDefaultSystemMenuBar(MenuBar menuBar) {
        MenuBarSkin.setDefaultSystemMenuBar(menuBar);
    }

//    // RT-21475 : Promote FXMLLoader.setLoadListener to public API
//    public static ParseTraceElement[] getParseTrace(FXMLLoader loader) {
//        return loader.getParseTrace();
//    }

    public static int getGridPaneColumnCount(GridPane gridPane) {
        return gridPane.getColumnCount();
    }

    public static int getGridPaneRowCount(GridPane gridPane) {
        return gridPane.getRowCount();
    }

    public static Bounds getGridPaneCellBounds(GridPane gridPane, int c, int r) {
        return gridPane.getCellBounds(c, r);
    }

    // Returns the corresponding text css (.css) from a binary css (.bss)
    public static URL getThemeTextStylesheet(String binaryCssUrlStr) {
        String textCssUrlStr = binaryCssUrlStr.replaceAll(".bss", ".css"); //NOI18N
        try {
            return new URL(textCssUrlStr);
        } catch (MalformedURLException ex) {
            // should never happen
            return null;
        }
    }

    // Deprecated as of FX 8 u20, and replaced by new method getTreeItemLevel:
    // using it would break ability to compile over JDK 8 GA, not an option for now.
    public static int getNodeLevel(TreeItem<?> item) {
        return TreeView.getNodeLevel(item);
    }

    public static Point2D localToLocal(Node source, double sourceX, double sourceY, Node target) {
        final Point2D sceneXY = source.localToScene(sourceX, sourceY, true /* rootScene */);
        return target.sceneToLocal(sceneXY, true /* rootScene */);
    }

    public static Bounds localToLocal(Node source, Bounds sourceBounds, Node target) {
        final Bounds sceneBounds = source.localToScene(sourceBounds, true /* rootScene */);
        return target.sceneToLocal(sceneBounds, true /* rootScene */);
    }
}
