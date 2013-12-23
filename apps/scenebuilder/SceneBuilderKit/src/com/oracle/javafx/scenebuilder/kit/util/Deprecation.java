/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Application.EventHandler;
import com.sun.javafx.css.Style;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.scene.control.skin.MenuBarSkin;
import com.sun.javafx.scene.input.PickResultChooser;
import java.net.URL;
import java.util.List;
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
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;

@SuppressWarnings("deprecation")
public class Deprecation {

    private Deprecation() {
        assert false;
    }

    // Deprecated stuff in Node
//    // RT-21247 : Promote impl_getAllParentStylesheets to public API
    public static Group createGroupWithNullParentStylesheets() {
        return new Group() {
            @Override
            @SuppressWarnings("unchecked")
            public List<String> impl_getAllParentStylesheets() {
                return null;
            }
        };
    }

//    // RT-21096 : Promote impl_getStyleMap / impl_setStyleMap to public API
    public static void setStyleMap(Node node, ObservableMap<StyleableProperty<?>, List<com.sun.javafx.css.Style>> map) {
        node.impl_setStyleMap(map);
    }

//    // RT-21096 : Promote impl_getStyleMap / impl_setStyleMap to public API
    public static ObservableMap<StyleableProperty<?>, List<Style>> getStyleMap(Node node) {
        return node.impl_getStyleMap();
    }
    
    public static void reapplyCSS(Node node) {
        node.impl_reapplyCSS();
    }

    // Retrieve the node of the Styleable.
    public static Node getNode(Styleable styleable) {
        // Nodes are styleable treated differently.
        if (styleable instanceof MenuItem) {
            return ((MenuItem) styleable).impl_styleableGetNode();
        } else if (styleable instanceof PopupControl) {
            return ((PopupControl) styleable).impl_styleableGetNode();
        } else if (styleable instanceof TableColumn) {
            return ((TableColumn) styleable).impl_styleableGetNode();
        } else if (styleable instanceof TreeTableColumn) {
            return ((TreeTableColumn) styleable).impl_styleableGetNode();
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public static List<Style> getMatchingStyles(CssMetaData cssMetaData, Styleable styleable) {
        return Node.impl_getMatchingStyles(cssMetaData, styleable);
    }

    // Deprecated stuff in Parent
//    // RT-21209 : Promote setImpl_traversalEngine to public API
//    public static void setTraversalEngine(Parent parent, TraversalEngine engine) {
//        parent.setImpl_traversalEngine(engine);
//    }
    // Deprecated stuff in Image
    // RT-21216 : Promote impl_getUrl to public API
    public static String getUrl(Image image) {
        return image.impl_getUrl();
    }

//    // RT-21217 : Promote impl_fromPlatformImage to public API
//    public static Image fromPlatformImage(Object platformImage) {
//        return Image.impl_fromPlatformImage(platformImage);
//    }
//    // RT-21219 : Promote impl_getPlatformImage to public API
//    public static Object getPlatformImage(Image image) {
//        return image.impl_getPlatformImage();
//    }
    // Deprecated stuff in FXMLLoader
    // RT-21226 : Promote setStaticLoad to public API
    public static void setStaticLoad(FXMLLoader loader, boolean staticLoad) {
        loader.impl_setStaticLoad(staticLoad);
    }

    // RT-21228 : Promote setLoadListener to public API
    public static void setLoadListener(FXMLLoader loader, com.sun.javafx.fxml.LoadListener loadListener) {
        loader.impl_setLoadListener(loadListener);
    }

    // RT-19857 : Keeping menu in the Mac menu bar when there is no more stage
    public static void setDefaultSystemMenuBar(MenuBar menuBar) {
        MenuBarSkin.setDefaultSystemMenuBar(menuBar);
    }

//    // RT-21475 : Promote FXMLLoader.setLoadListener to public API
//    public static ParseTraceElement[] getParseTrace(FXMLLoader loader) {
//        return loader.getParseTrace();
//    }
    // Deprecated stuff in JavaFXBuilderFactory
//    // RT-21230 : Promote JavaFXBuilderFactory(ClassLoader classLoader, boolean alwaysUseBuilders) constructor to public API
//    public static JavaFXBuilderFactory getJavaFXBuilderFactory(boolean alwaysUseBuilders) {
//        return new JavaFXBuilderFactory(Thread.currentThread().getContextClassLoader(), alwaysUseBuilders);
//    }
    public static void setPlatformEventHandler(EventHandler eventHandler) {
        Application.GetApplication().setEventHandler(eventHandler);
    }

    public static EventHandler getPlatformEventHandler() {
        return Application.GetApplication().getEventHandler();
    }

    public static int getGridPaneColumnCount(GridPane gridPane) {
        return gridPane.impl_getColumnCount();
    }

    public static int getGridPaneRowCount(GridPane gridPane) {
        return gridPane.impl_getRowCount();
    }

    public static Bounds getGridPaneCellBounds(GridPane gridPane, int c, int r) {
        return gridPane.impl_getCellBounds(c, r);
    }
    
    // RT-33675 : Promote TableColumn.impl_setReorderable() to public API
    @SuppressWarnings("rawtypes")
    public static void setTableColumnReordable(TableColumn tableColumn, boolean reordable) {
        tableColumn.impl_setReorderable(reordable);
    }
    
    // RT-21247 : Promote impl_getAllParentStylesheets to public API
    public static Group makeStylingIsolationGroup() {
        final Group result = new Group() {
            @Override
            public Styleable getStyleableParent() { return null; }
        };
        
        result.getStyleClass().add("root"); //NOI18N
        result.getStylesheets().add(getModenaStylesheetURL().toString());
        
        return result;
    }
    
    
    public static URL getCaspianStylesheetURL() {
        final String resourceName = "com/sun/javafx/scene/control/skin/caspian/caspian.bss"; //NOI18N
        return ClassLoader.getSystemResource(resourceName);
    }
    
    
    public static URL getCaspianHighContrastStylesheetURL() {
        final String resourceName = "com/sun/javafx/scene/control/skin/caspian/highcontrast.bss"; //NOI18N
        return ClassLoader.getSystemResource(resourceName);
    }
    
    
    public static URL getCaspianVirtualKeyboardStylesheetURL() {
        final String resourceName = "com/sun/javafx/scene/control/skin/caspian/fxvk.bss"; //NOI18N
        return ClassLoader.getSystemResource(resourceName);
    }
    
    
    public static URL getCaspianEmbeddedStylesheetURL() {
        final String resourceName = "com/sun/javafx/scene/control/skin/caspian/embedded.bss"; //NOI18N
        return ClassLoader.getSystemResource(resourceName);
    }
    
    
    public static URL getCaspianEmbeddedQVGAStylesheetURL() {
        final String resourceName = "com/sun/javafx/scene/control/skin/caspian/embedded-qvga.bss"; //NOI18N
        return ClassLoader.getSystemResource(resourceName);
    }
    
    
    public static URL getModenaStylesheetURL() {
        final String resourceName = "com/sun/javafx/scene/control/skin/modena/modena.bss"; //NOI18N
        return ClassLoader.getSystemResource(resourceName);
    }
    
    
    public static URL getModenaTouchStylesheetURL() {
        final String resourceName = "com/sun/javafx/scene/control/skin/modena/touch.bss"; //NOI18N
        return ClassLoader.getSystemResource(resourceName);
    }

}
