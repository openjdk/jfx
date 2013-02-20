/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.CssMetaData;
import static javafx.scene.control.ControlTestUtils.*;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.scene.control.skin.ToolBarSkin;
import com.sun.javafx.tk.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.StyleableProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
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
    private Node node1;
    private Node node2;
    
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
    @Test public void whenOrientationIsBound_impl_cssSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)toolBar.orientationProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(toolBar));
        ObjectProperty<Orientation> other = new SimpleObjectProperty<Orientation>(Orientation.VERTICAL);
        toolBar.orientationProperty().bind(other);
        assertFalse(styleable.isSettable(toolBar));
    }

    @Test public void whenOrientationIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)toolBar.orientationProperty()).getCssMetaData();
        styleable.set(toolBar, Orientation.VERTICAL, null);
        assertTrue(styleable.isSettable(toolBar));
    }

    @Test public void canSpecifyOrientationViaCSS() {
        CssMetaData styleable = ((StyleableProperty)toolBar.orientationProperty()).getCssMetaData();
        styleable.set(toolBar, Orientation.VERTICAL, null);
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
        b3.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {                
                try {
                    toolBar.getItems().setAll(list1);
                } catch (Exception iae) {                    
                    fail("Duplicate items are not allowed " + iae.toString());
                }
            }
        });

        b3.fire();
    }
    
}
