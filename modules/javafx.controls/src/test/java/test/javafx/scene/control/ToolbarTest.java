/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import javafx.css.CssMetaData;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;

import javafx.scene.AccessibleAttribute;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.pgstub.StubToolkit;
import javafx.scene.control.skin.ToolBarSkin;
import com.sun.javafx.tk.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class ToolbarTest {
    private ToolBar toolBar;//Empty
    private ToolBar toolBarWithItems;//Items

    private Toolkit tk;
    private StackPane root;

    private Node node1;
    private Node node2;

    private static final double TOOLBAR_SIZE = 300.0;
    private static final double EXPANDED_CHILDREN_SIZE = 250.0;
    private static final double ORIGINAL_CHILDREN_SIZE = 100.0;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        toolBar = new ToolBar();
        node1 = new Rectangle();
        node2 = new Rectangle(2.0,4.0);
        toolBarWithItems = new ToolBar(node1,node2);
    }



    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultConstructorShouldSetStyleClassTo_toolbar() {
        assertStyleClassContains(toolBar, "tool-bar");
    }

    @Test public void defaultFocusTraversibleIsFalse() {
        assertFalse(toolBar.isFocusTraversable());
    }
    @Test public void defaultVarArgConstructorShouldSetStyleClassTo_toolbar() {
        assertStyleClassContains(toolBarWithItems, "tool-bar");
    }

    @Test public void defaultVarArgConstructorCheckItems() {
        assertNotNull(toolBarWithItems.getItems());
        assertEquals(toolBarWithItems.getItems().size(), 2.0, 0.0);
        assertSame(toolBarWithItems.getItems().get(0), node1);
        assertSame(toolBarWithItems.getItems().get(1), node2);
    }

    @Test public void defaultOrientation() {
        assertSame(toolBar.getOrientation(), Orientation.HORIZONTAL);
    }


    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void orientationPropertyHasBeanReference() {
        assertSame(toolBar, toolBar.orientationProperty().getBean());
    }

    @Test public void orientationPropertyHasName() {
        assertEquals("orientation", toolBar.orientationProperty().getName());
    }


    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenOrientationIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)toolBar.orientationProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolBar));
        ObjectProperty<Orientation> other = new SimpleObjectProperty<Orientation>(Orientation.VERTICAL);
        toolBar.orientationProperty().bind(other);
        assertFalse(styleable.isSettable(toolBar));
    }

    @Test public void whenOrientationIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)toolBar.orientationProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolBar));
    }

    @Test public void canSpecifyOrientationViaCSS() {
        ((StyleableProperty)toolBar.orientationProperty()).applyStyle(null, Orientation.VERTICAL);
        assertSame(Orientation.VERTICAL, toolBar.getOrientation());
    }

    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/
    @Test public void setOrientationAndSeeValueIsReflectedInModel() {
        toolBar.setOrientation(Orientation.HORIZONTAL);
        assertSame(toolBar.orientationProperty().getValue(), Orientation.HORIZONTAL);
    }

    @Test public void setOrientationAndSeeValue() {
        toolBar.setOrientation(Orientation.VERTICAL);
        assertSame(toolBar.getOrientation(), Orientation.VERTICAL);
    }

    @Test public void rt18501_duplicate_items_are_not_allowed() {
        ToolBarSkin toolbarSkin = new ToolBarSkin(toolBar);
        toolBar.setSkin(toolbarSkin);
        toolBar.getItems().clear();
        node1 = new Rectangle();
        node2 = new Rectangle(2.0,4.0);
        final List<Node> list1 = new ArrayList<Node>();
        list1.add(node1);

        toolBar.getItems().add(node1);

        list1.add(node2);

        Button b3 = new Button("button");
        b3.setOnAction(e -> {
            try {
                toolBar.getItems().setAll(list1);
            } catch (Exception iae) {
                fail("Duplicate items are not allowed " + iae.toString());
            }
        });

        b3.fire();
    }

    @Test public void toolBarFocusTraversalTest() {
        toolBar.getItems().clear();

        toolBar.setFocusTraversable(true);
        assertTrue(toolBar.isFocusTraversable());

        // Create 5 buttons - put 2 of them in a Pane
        Button btn1 = new Button("Btn1");
        Button btn2 = new Button("Btn2");

        Button btn3 = new Button("Btn3");
        Button btn4 = new Button("Btn4");
        Pane p1 = new Pane(btn3, btn4);

        Button btn5 = new Button("Btn5");

        toolBar.getItems().addAll(btn1, btn2, p1, btn5);

        ToolBarSkin toolbarSkin = new ToolBarSkin(toolBar);
        toolBar.setSkin(toolbarSkin);

        Scene scene = new Scene(toolBar);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        tk.firePulse();

        toolBar.getScene().getWindow().requestFocus();
        assertTrue(btn1.isFocused());

        KeyEventFirer keyboard1 = new KeyEventFirer(btn1);
        keyboard1.doKeyPress(KeyCode.TAB);
        tk.firePulse();
        assertTrue(btn2.isFocused());

        KeyEventFirer keyboard2 = new KeyEventFirer(btn2);
        keyboard2.doKeyPress(KeyCode.TAB);
        tk.firePulse();
        assertTrue(btn3.isFocused());

        KeyEventFirer keyboard3 = new KeyEventFirer(btn3);
        keyboard3.doKeyPress(KeyCode.TAB);
        tk.firePulse();
        assertTrue(btn4.isFocused());

        KeyEventFirer keyboard4 = new KeyEventFirer(btn4);
        keyboard4.doKeyPress(KeyCode.TAB);
        tk.firePulse();
        assertTrue(btn5.isFocused());
    }

    @Test public void overflowShownInHorizontalAfterChildrenReziseTest() {
        initializeToolBar();

        toolBar.setOrientation(Orientation.HORIZONTAL);

        testOverflowVisibility();
    }

    @Test public void overflowShownInVerticalAfterChildrenReziseTest() {
        initializeToolBar();

        toolBar.setOrientation(Orientation.VERTICAL);

        testOverflowVisibility();
    }

    private void initializeToolBar() {
        root = new StackPane(toolBar);
        root.setPrefSize(400, 400);

        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);

        toolBar.getItems().addAll(node1, node2);
        setFixSize(toolBar, TOOLBAR_SIZE);
    }

    private void testOverflowVisibility() {
        setFixSize(node1, ORIGINAL_CHILDREN_SIZE);
        setFixSize(node2, ORIGINAL_CHILDREN_SIZE);

        assertOverflowNotShown();

        setFixSize(node1, EXPANDED_CHILDREN_SIZE);

        assertOverflowShown();

        setFixSize(node1, ORIGINAL_CHILDREN_SIZE);

        assertOverflowNotShown();
    }

    private void setFixSize(Node node, double size) {
        if (node instanceof Region) {
            ((Region) node).setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
            ((Region) node).setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
            ((Region) node).setPrefSize(size, size);
        } else if (node instanceof Rectangle) {
            ((Rectangle) node).setHeight(size);
            ((Rectangle) node).setWidth(size);
        }

        root.applyCss();
        root.autosize();
        root.layout();
    }

    private void assertOverflowNotShown() {
        Pane pane = (Pane) toolBar.queryAccessibleAttribute(AccessibleAttribute.OVERFLOW_BUTTON);
        assertNotNull(pane);
        assertFalse(pane.isVisible());
    }

    private void assertOverflowShown() {
        Pane pane = (Pane) toolBar.queryAccessibleAttribute(AccessibleAttribute.OVERFLOW_BUTTON);
        assertNotNull(pane);
        assertTrue(pane.isVisible());
    }
}
