/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors;

import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.PropertyEditor.LayoutFormat;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors.PopupEditor;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.klass.ComponentClassMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.PropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
        
/**
 * Utility class for property editors.
 *
 *
 */
public class EditorUtils {

    static final String[] FXML_RESERVED_KEYWORDS = {"null"}; //NOI18N

    public static void makeWidthStretchable(final Node node) {
        Parent p = node.getParent();
        if (p == null) {
            node.parentProperty().addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable valueModel) {
                    if (node.getParent() != null) {
                        makeWidthStretchable(node, node.getParent());
                        node.parentProperty().removeListener(this);
                    }
                }
            });
        } else {
            makeWidthStretchable(node, p);
        }
    }

    private static void makeWidthStretchable(Node node, Parent p) {
        if (p != null) {
            if (p instanceof HBox) {
                HBox.setHgrow(node, Priority.ALWAYS);
            } else if (p instanceof GridPane) {
                GridPane.setHgrow(node, Priority.ALWAYS);
            }
        }
        if (node instanceof Region) {
            ((Region) node).setMinWidth(0);
            ((Region) node).setMaxWidth(Double.MAX_VALUE);
        }
    }

    public static void makeUnstretchable(final Control node, double width, double height) {
        node.setPrefWidth(width);
        node.setMinWidth(width);
        node.setMaxWidth(width);
        node.setPrefHeight(height);
        node.setMinHeight(height);
        node.setMaxHeight(height);
    }

    // Replace a node (wich is in the scene graph) with another node (which is NOT in the scene graph)
    // and adapt the layout if provided.
    public static void replaceNode(Node node, Node newNode, LayoutFormat layoutFormat) {
        Parent parent = node.getParent();
        boolean hasGridPaneParent = parent instanceof GridPane;
        if (parent instanceof Pane) {
            // Supporting Pane only should be enough for now...
            ObservableList<Node> children = ((Pane) parent).getChildren();

            // Remove node
            int childIndex = children.indexOf(node);
            int columnIndex = -1;
            int rowIndex = -1;
            if (hasGridPaneParent) {
                columnIndex = GridPane.getColumnIndex(node);
                rowIndex = GridPane.getRowIndex(node);
            }
            children.remove(childIndex);
            // Remove row constraints if needed
            if (hasGridPaneParent && (layoutFormat != null)) {
                ((GridPane) parent).getRowConstraints().remove(rowIndex);
            }

            // Add new node
            GridPane.setRowIndex(newNode, rowIndex);
            GridPane.setColumnIndex(newNode, columnIndex);
            children.add(childIndex, newNode);
            // Add new row constraints if needed
            if (hasGridPaneParent && (layoutFormat != null)) {
                RowConstraints rowConstraints = new RowConstraints();
                if (layoutFormat == LayoutFormat.SIMPLE_LINE_CENTERED) {
                    rowConstraints.setValignment(VPos.CENTER);
                } else if (layoutFormat == LayoutFormat.SIMPLE_LINE_TOP) {
                    rowConstraints.setValignment(VPos.TOP);
                } else {
                    throw new UnsupportedOperationException(
                            "replaceNode() - GridPane: layout change in double line not supported yet!");
                }
                ((GridPane) parent).getRowConstraints().add(rowIndex, rowConstraints);
            }
        }
    }

    public static String valAsStr(Object val) {
        if (val == null) {
            return null;
        }
        String str = val.toString();
        if ((val instanceof Double) && str.endsWith(".0")) { //NOI18N
            str = str.substring(0, str.length() - 2);
        }
        return str;
    }

    /**
     * Takes the given name and formats it for display. Given a camel-case name,
     * it will split the name at each of the upper-case letters, unless multiple
     * uppercase letters are in a series, in which case it treats them as a
     * single name. The initial lower-case letter is upper cased. So a name like
     * "translateX" becomes "Translate X" and a name like "halign" becomes
     * "Halign".
     * <p>
     * Numbers are treated the same as if they were capital letters, such that
     * "MyClass3" would become "My Class 3" and "MyClass23" would become "My
     * Class 23".
     * <p>
     * Underscores are converted to spaces, with the first letter following the
     * underscore converted to upper case. Multiple underscores in a row are
     * treated only as a single space, and any leading or trailing underscores
     * are skipped.
     *
     * @param name
     * @return
     */
    public static String toDisplayName(String name) {
        if (name == null) {
            return name;
        }
        // Replace all underscores with empty spaces
        name = name.replace("_", " "); //NOI18N
        // Trim out any leading or trailing space (which also effectively
        // removes any underscores that were leading or trailing, since the
        // above line had converted them all to spaces).
        name = name.trim();
        // If the resulting name is empty, return an empty string
        if (name.length() == 0) {
            return name;
        }
        // There are now potentially spaces already in the name. If, while
        // iterating over all of the characters in the name we encounter a
        // space, then we will simply step past the space and capitalize the
        // following character.
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        StringBuilder builder = new StringBuilder();
        char ch = name.charAt(0);
        builder.append(ch);
        boolean previousWasDigit = Character.isDigit(ch);
        boolean previousWasCapital = !previousWasDigit;
        for (int i = 1; i < name.length(); i++) {
            ch = name.charAt(i);
            if ((Character.isUpperCase(ch) && !previousWasCapital)
                    || (Character.isUpperCase(ch) && previousWasDigit)) {
                builder.append(" "); //NOI18N
                builder.append(ch);
                previousWasCapital = true;
                previousWasDigit = false;
            } else if ((Character.isDigit(ch) && !previousWasDigit)
                    || (Character.isDigit(ch) && previousWasCapital)) {
                builder.append(" "); //NOI18N
                builder.append(ch);
                previousWasCapital = false;
                previousWasDigit = true;
            } else if (Character.isUpperCase(ch) || Character.isDigit(ch)) {
                builder.append(ch);
            } else if (Character.isWhitespace(ch)) {
                builder.append(" "); //NOI18N
                // There might have been multiple underscores in a row, so
                // we might now have multiple whitespace in a row. Search ahead
                // to the first non-whitespace character.
                ch = name.charAt(++i);
                while (Character.isWhitespace(ch)) {
                    // Note that because we trim the String, it should be
                    // impossible to have trailing whitespace, and thus we
                    // don't have to worry about the ArrayIndexOutOfBounds
                    // condition here.
                    ch = name.charAt(++i);
                }
                builder.append(Character.toUpperCase(ch));
                previousWasDigit = Character.isDigit(ch);
                previousWasCapital = !previousWasDigit;
            } else {
                previousWasCapital = false;
                previousWasDigit = false;
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    // Check if this could be moved in the Metadata classes.
    // Get the component class metadata where a property is defined.
    private static ComponentClassMetadata getDefiningClass(Class<?> clazz, PropertyName propName) {
        Metadata metadata = Metadata.getMetadata();
        ComponentClassMetadata classMeta = metadata.queryComponentMetadata(clazz);
        while (clazz != null) {
            for (PropertyMetadata propMeta : classMeta.getProperties()) {
                if (propMeta.getName().compareTo(propName) == 0) {
                    return classMeta;
                }
            }
            // Check the inherited classes
            classMeta = classMeta.getParentMetadata();
        }
        return null;
    }

    private static class NextFrameTimer extends AnimationTimer {

        final AtomicInteger count = new AtomicInteger(0);
        final int order;
        Runnable callback;

        public NextFrameTimer(Runnable callback) {
            this(callback, 1);
        }

        public NextFrameTimer(Runnable callback, int order) {
            assert order >= 0;
            this.callback = callback;
            this.order = order;
        }

        @Override
        public void handle(long now) {
            if (count.getAndIncrement() == this.order) {
                try {
                    callback.run();
                } finally {
                    stop();
                }
            }
        }
    }

    public static AnimationTimer doEndOfFrame(final Runnable callback) {
        AnimationTimer timer = new NextFrameTimer(callback, 0);
        timer.start();
        return timer;
    }

    public static AnimationTimer doNextFrame(final Runnable callback) {
        AnimationTimer timer = new NextFrameTimer(callback);
        timer.start();
        return timer;
    }

    /*
     * Round a double value, number of decimals depends on the roundingFactor.
     * e.g. round(10.1233, 100) returns 10.12
     */
    public static double round(double value, int roundingFactor) {
        double doubleRounded = Math.round(value * roundingFactor);
        return doubleRounded / roundingFactor;
    }

    public static double computeLeftAnchor(Node node) {
        return computeLeftAnchor(node, node.getLayoutBounds());
    }

    public static double computeRightAnchor(Node node) {
        return computeRightAnchor(node, node.getLayoutBounds());
    }

    public static double computeTopAnchor(Node node) {
        return computeTopAnchor(node, node.getLayoutBounds());
    }

    public static double computeBottomAnchor(Node node) {
        return computeBottomAnchor(node, node.getLayoutBounds());
    }

    private static double computeLeftAnchor(Node node, Bounds futureLayoutBounds) {
        return node.getLayoutX() + futureLayoutBounds.getMinX() - node.getParent().getLayoutBounds().getMinX();
    }

    private static double computeRightAnchor(Node node, Bounds futureLayoutBounds) {
        return node.getParent().getLayoutBounds().getMaxX() - node.getLayoutX() - futureLayoutBounds.getMaxX();
    }

    private static double computeTopAnchor(Node node, Bounds futureLayoutBounds) {
        return node.getLayoutY() + futureLayoutBounds.getMinY() - node.getParent().getLayoutBounds().getMinY();
    }

    private static double computeBottomAnchor(Node node, Bounds futureLayoutBounds) {
        return node.getParent().getLayoutBounds().getMaxY() - node.getLayoutY() - futureLayoutBounds.getMaxY();
    }

    public static void handleFading(FadeTransition fadeTransition, Node fadingSource) {
        handleFading(fadeTransition, fadingSource, null);
    }

    /*
     * Fade in / fade out a node from its FadeTransition.
     * Fading is activated from a fadingSource node.
     */
    public static void handleFading(FadeTransition fadeTransition, Node fadingSource, BooleanProperty disableProperty) {
        fadingSource.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent arg0) {
                Node targetNode = fadeTransition.getNode();
                if ((targetNode instanceof MenuButton) && ((MenuButton) targetNode).isShowing()) {
                    return;
                }
                if (disableProperty != null && disableProperty.getValue()) {
                    // Nothing to do if disabled
                    return;
                }
                fadeTo(fadeTransition, 1);
            }
        });
        fadingSource.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent arg0) {
                Node targetNode = fadeTransition.getNode();
                if ((targetNode instanceof MenuButton) && ((MenuButton) targetNode).isShowing()) {
                    return;
                }
                if (disableProperty != null && disableProperty.getValue()) {
                    // Nothing to do if disabled
                    return;
                }
                fadeTo(fadeTransition, 0);
            }
        });
    }

    public static void fadeTo(FadeTransition fadeTransition, double toValue) {
        fadeTransition.stop();
        fadeTransition.setFromValue(fadeTransition.getNode().getOpacity());
        fadeTransition.setToValue(toValue);
        fadeTransition.play();
    }

    protected static void openUrl(Set<Class<?>> selectedClasses, ValuePropertyMetadata propMeta) throws IOException {
        Class<?> clazz = null;
        // In case of static property, we don't care of the selectedClasses
        if (selectedClasses != null) {
            for (Class<?> cl : selectedClasses) {
                clazz = cl;
            }
        }
        PropertyName propertyName = propMeta.getName();
        if (propMeta.isStaticProperty()) {
            clazz = propertyName.getResidenceClass();
        } else {
            clazz = getDefiningClass(clazz, propertyName).getKlass();
        }
        String propNameStr = propertyName.getName();
        // First char in uppercase
        propNameStr = propNameStr.substring(0, 1).toUpperCase(Locale.ENGLISH) + propNameStr.substring(1);
        String methodName;
        if (propMeta.getValueClass() == Boolean.class) {
            methodName = "is" + propNameStr + "--"; //NOI18N
        } else if (propMeta.isStaticProperty()) {
            methodName = "get" + propNameStr + "-" + Node.class.getName() + "-"; //NOI18N
        } else {
            methodName = "get" + propNameStr + "--"; //NOI18N
        }

        String url = EditorPlatform.JAVADOC_HOME + clazz.getName().replaceAll("\\.", "/") + ".html"; //NOI18N
        url += "#" + methodName; //NOI18N
        EditorPlatform.open(url);
    }

    // Specific swap() function for an ObservableList:
    // Collections.swap() directly on the ObservableList generates a "duplicate children added" error
    public static void swap(ObservableList<Node> list, int i, int j) {
        ArrayList<Node> children = new ArrayList<>(list);
        Collections.swap(children, i, j);
        // Workaround for RT-31965: list re-arrangement is not detected...
        // list.setAll(children);
        list.clear();
        list.addAll(children);
    }

    public static Parent loadFxml(String fxmlFileName, Object controller) {
        URL fxmlURL = EditorUtils.class.getResource(fxmlFileName);
        return loadFxml(fxmlURL, controller);
    }

    public static Parent loadPopupFxml(String fxmlFileName, Object controller) {
        URL fxmlURL = PopupEditor.class.getResource(fxmlFileName);
        return loadFxml(fxmlURL, controller);
    }

    // To be used only for inspector editors (which are in the same classpath than this class)
    public static Parent loadFxml(URL fxmlURL, Object controller) {
        final FXMLLoader loader = new FXMLLoader();
        loader.setController(controller);
        // Do we really need this?
//        loader.setClassLoader(controller.getClass().getClassLoader());
        loader.setLocation(fxmlURL);
        loader.setResources(I18N.getBundle());
        Parent root;
        try {
            root = (Parent) loader.load();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load " + fxmlURL.getFile(), ex); //NOI18N
        }
        return root;
    }

    public static String getFileName(String urlStr) {
        URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException ex) {
            System.err.println("Invalid URL: " + urlStr); //NOI18N
            assert false;
            return null;
        }
        String[] urlParts = url.getPath().split("\\/");
        return urlParts[urlParts.length - 1];
    }

    public static boolean areEqual(Object obj1, Object obj2) {
        if ((obj1 == null) || (obj2 == null)) {
            if (obj1 != obj2) {
                return false;
            }
        } else if (!obj1.equals(obj2)) {
            return false;
        }
        return true;
    }

    public static URL getUrl(String suffix, PrefixedValue.Type type, URL fxmlFileLocation) {
        String prefixedString;
        if (suffix.isEmpty()) {
            prefixedString = ""; //NOI18N
        } else {
            prefixedString = (new PrefixedValue(type, suffix)).toString();
        }
        return getUrl(prefixedString, fxmlFileLocation);
    }
    
    // Get the URL corresponding to a PrefixedValue string
    @SuppressWarnings("UseSpecificCatch")
    public static URL getUrl(String prefixedString, URL fxmlFileLocation) {
        PrefixedValue prefixedValue = new PrefixedValue(prefixedString);
        URL url = null;
        if (prefixedValue.getType() == PrefixedValue.Type.DOCUMENT_RELATIVE_PATH) {
            url = prefixedValue.resolveDocumentRelativePath(fxmlFileLocation);
        } else if (prefixedValue.getType() == PrefixedValue.Type.PLAIN_STRING) {
            try {
                url = new URI(prefixedValue.getSuffix()).toURL();
            } catch (Throwable ex) {
                // Catching *all* the exception is done on purpose.
                // May happen. nothing to do.
            }
        }
        return url;
    }

}
