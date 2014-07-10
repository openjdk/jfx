/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.CheckBoxTreeItem.TreeModificationEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CheckBoxTreeItemTest {
    
    private CheckBoxTreeItem<Object> treeItem;
    
    private CheckBoxTreeItem<String> root;
        private CheckBoxTreeItem<String> child1;
        private CheckBoxTreeItem<String> child2;
            private CheckBoxTreeItem<String> subchild1;
            private CheckBoxTreeItem<String> subchild2;
        private CheckBoxTreeItem<String> child3;
    
    @Before public void setup() {
        treeItem = new CheckBoxTreeItem<>();
        
        root = new CheckBoxTreeItem<>("root");
            child1 = new CheckBoxTreeItem<>("child1");
            child2 = new CheckBoxTreeItem<>("child2");
                subchild1 = new CheckBoxTreeItem<>("subchild1");
                subchild2 = new CheckBoxTreeItem<>("subchild2");
            child3 = new CheckBoxTreeItem<>("child3");
            
        child2.getChildren().addAll(subchild1, subchild2);
        root.getChildren().addAll(child1, child2, child3);
    }
    
    @After public void cleanup() {
        treeItem = null;
        root = null;
        child1 = null;
        child2 = null;
        child3 = null;
        subchild1 = null;
        subchild2 = null;
    }
    
    
    /**************************************************************************
     * 
     * Util methods     
     * 
     *************************************************************************/
    
    private void assertIsSelected(CheckBoxTreeItem... items) {
        for (CheckBoxTreeItem item : items) {
            assertTrue(item.isSelected());
        }
    }
    
    private void assertIsNotSelected(CheckBoxTreeItem... items) {
        for (CheckBoxTreeItem item : items) {
            assertFalse(item.isSelected());
        }
    }
    
    private void assertAllChildrenSelected(CheckBoxTreeItem<?> parent) {
        assertIsSelected(parent);
        for (TreeItem child : parent.getChildren()) {
            if (child instanceof CheckBoxTreeItem) {
                assertAllChildrenSelected((CheckBoxTreeItem)child);
            }
        }
    }
    
    private void assertAllChildrenNotSelected(CheckBoxTreeItem<?> parent) {
        assertIsNotSelected(parent);
        for (TreeItem child : parent.getChildren()) {
            if (child instanceof CheckBoxTreeItem) {
                assertAllChildrenNotSelected((CheckBoxTreeItem)child);
            }
        }
    }
    
    private void setAllIndependent(CheckBoxTreeItem<?> parent) {
        parent.setIndependent(true);
        for (TreeItem child : parent.getChildren()) {
            if (child instanceof CheckBoxTreeItem) {
                ((CheckBoxTreeItem)child).setIndependent(true);
            }
        }
    }
    
    
    /**************************************************************************
     * 
     * Constructor tests     
     * 
     *************************************************************************/

    @Test public void testDefaultConstructor_hasNullValue() {
        assertNull(treeItem.getValue());
    }
    
    @Test public void testDefaultConstructor_hasNullGraphic() {
        assertNull(treeItem.getGraphic());
    }
    
    @Test public void testDefaultConstructor_hasNullParent() {
        assertNull(treeItem.getParent());
    }
    
    @Test public void testDefaultConstructor_hasEmptyChildren() {
        assertNotNull(treeItem.getChildren());
        assertTrue(treeItem.getChildren().isEmpty());
    }
    
    @Test public void testDefaultConstructor_isExpandedIsTrue() {
        assertFalse(treeItem.isExpanded());
    }
    
    @Test public void testDefaultConstructor_isIndependentIsFalse() {
        assertFalse(treeItem.isIndependent());
    }
    
    @Test public void testDefaultConstructor_isIndeterminateIsFalse() {
        assertFalse(treeItem.isIndeterminate());
    }
    
    @Test public void testDefaultConstructor_isLeafIsTrue() {
        assertTrue(treeItem.isLeaf());
    }
    
    @Test public void testDefaultConstructor_isSelectedIsFalse() {
        assertFalse(treeItem.isSelected());
    }
    
    @Test public void testValueConstructor_hasNonNullValue() {
        CheckBoxTreeItem<String> cbti = new CheckBoxTreeItem<>("TEST");
        assertEquals("TEST", cbti.getValue());
    }
    
    @Test public void testValueGraphicConstructor_hasNonNullValue() {
        Rectangle graphic = new Rectangle(10, 10, Color.RED);
        CheckBoxTreeItem<String> cbti = new CheckBoxTreeItem<>("TEST", graphic);
        assertEquals("TEST", cbti.getValue());
    }
    
    @Test public void testValueGraphicConstructor_hasNonNullGraphic() {
        Rectangle graphic = new Rectangle(10, 10, Color.RED);
        CheckBoxTreeItem<String> cbti = new CheckBoxTreeItem<>("TEST", graphic);
        assertEquals(graphic, cbti.getGraphic());
    }
    
    @Test public void testValueGraphicSelectedConstructor_isSelected() {
        Rectangle graphic = new Rectangle(10, 10, Color.RED);
        CheckBoxTreeItem<String> cbti = new CheckBoxTreeItem<>("TEST", graphic, true);
        assertTrue(cbti.isSelected());
    }
    
    @Test public void testValueGraphicSelectedIndependentConstructor_isIndependent() {
        Rectangle graphic = new Rectangle(10, 10, Color.RED);
        CheckBoxTreeItem<String> cbti = new CheckBoxTreeItem<>("TEST", graphic, true, true);
        assertTrue(cbti.isIndependent());
    }
    
    
    
    /**************************************************************************
     * 
     * Property tests     
     * 
     *************************************************************************/
    
    @Test public void testSelectedSetter() {
        assertFalse(treeItem.isSelected());
        treeItem.setSelected(true);
        assertTrue(treeItem.isSelected());
        treeItem.setSelected(false);
        assertFalse(treeItem.isSelected());
    }
    
    @Test public void testSelectedPropertySetter() {
        assertFalse(treeItem.selectedProperty().get());
        treeItem.selectedProperty().set(true);
        assertTrue(treeItem.selectedProperty().get());
        treeItem.setSelected(false);
        assertFalse(treeItem.selectedProperty().get());
    }
    
    private int selectedEventCount = 0;
    @Test public void testSelectedPropertyEvent() {
        treeItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
            selectedEventCount++;
        });
        
        // no event when the value is unchanged
        treeItem.setSelected(false);
        assertEquals(0, selectedEventCount);
        
        treeItem.setSelected(true);
        assertEquals(1, selectedEventCount);
        
        treeItem.setSelected(false);
        assertEquals(2, selectedEventCount);
        
        treeItem.setSelected(true);
        assertEquals(3, selectedEventCount);
    }
    
    
    @Test public void testIndeterminateSetter() {
        assertFalse(treeItem.isIndeterminate());
        treeItem.setIndeterminate(true);
        assertTrue(treeItem.isIndeterminate());
        treeItem.setIndeterminate(false);
        assertFalse(treeItem.isIndeterminate());
    }
    
    @Test public void testIndeterminatePropertySetter() {
        assertFalse(treeItem.indeterminateProperty().get());
        treeItem.indeterminateProperty().set(true);
        assertTrue(treeItem.indeterminateProperty().get());
        treeItem.setIndeterminate(false);
        assertFalse(treeItem.indeterminateProperty().get());
    }
    
    private int indeterminateEventCount = 0;
    @Test public void testIndeterminatePropertyEvent() {
        treeItem.indeterminateProperty().addListener((observable, oldValue, newValue) -> {
            indeterminateEventCount++;
        });
        
        // no event when the value is unchanged
        treeItem.setIndeterminate(false);
        assertEquals(0, indeterminateEventCount);
        
        treeItem.setIndeterminate(true);
        assertEquals(1, indeterminateEventCount);
        
        treeItem.setIndeterminate(false);
        assertEquals(2, indeterminateEventCount);
        
        treeItem.setIndeterminate(true);
        assertEquals(3, indeterminateEventCount);
    }
    
    
    @Test public void testIndependentSetter() {
        assertFalse(treeItem.isIndependent());
        treeItem.setIndependent(true);
        assertTrue(treeItem.isIndependent());
        treeItem.setIndependent(false);
        assertFalse(treeItem.isIndependent());
    }
    
    @Test public void testIndependentPropertySetter() {
        assertFalse(treeItem.independentProperty().get());
        treeItem.independentProperty().set(true);
        assertTrue(treeItem.independentProperty().get());
        treeItem.setIndependent(false);
        assertFalse(treeItem.independentProperty().get());
    }
    
    private int independentEventCount = 0;
    @Test public void testIndependentPropertyEvent() {
        treeItem.independentProperty().addListener((observable, oldValue, newValue) -> {
            independentEventCount++;
        });
        
        // no event when the value is unchanged
        treeItem.setIndependent(false);
        assertEquals(0, independentEventCount);
        
        treeItem.setIndependent(true);
        assertEquals(1, independentEventCount);
        
        treeItem.setIndependent(false);
        assertEquals(2, independentEventCount);
        
        treeItem.setIndependent(true);
        assertEquals(3, independentEventCount);
    }
    
    
    /**************************************************************************
     * 
     * Non-independent CheckBoxTreeItem tests      
     * 
     *************************************************************************/
    
    @Test public void testNonIndependent_updateRootSelected() {
        assertAllChildrenNotSelected(root);
        
        root.setSelected(true);
        assertAllChildrenSelected(root);
        assertFalse(root.isIndeterminate());
        
        root.setSelected(false);
        assertAllChildrenNotSelected(root);
        assertFalse(root.isIndeterminate());
    }
    
    @Test public void testNonIndependent_updateChild1Selected() {
        assertAllChildrenNotSelected(root);
        
        child1.setSelected(true);
        assertIsSelected(child1);
        
        assertIsNotSelected(root);
        assertAllChildrenNotSelected(child2);
        assertAllChildrenNotSelected(child3);
        
        assertTrue(root.isIndeterminate());
        assertFalse(child2.isIndeterminate());
        assertFalse(child3.isIndeterminate());
    }
    
    @Test public void testNonIndependent_updateChild2Selected() {
        assertAllChildrenNotSelected(root);
        
        child2.setSelected(true);
        
        assertIsNotSelected(root);
        assertAllChildrenSelected(child2);
        assertAllChildrenNotSelected(child3);
        
        assertTrue(root.isIndeterminate());
        assertFalse(child1.isIndeterminate());
        assertFalse(child2.isIndeterminate());
        assertFalse(child3.isIndeterminate());
    }
    
    
    /**************************************************************************
     * 
     * Independent CheckBoxTreeItem tests      
     * 
     *************************************************************************/
    
    @Test public void testIndependent_updateRootSelected() {
        setAllIndependent(root);
        assertAllChildrenNotSelected(root);
        
        root.setSelected(true);
        assertTrue(root.isSelected());
        assertFalse(child1.isSelected());
        assertFalse(child2.isSelected());
        assertFalse(subchild1.isSelected());
        assertFalse(subchild2.isSelected());
        assertFalse(child3.isSelected());
        assertFalse(root.isIndeterminate());
        
        root.setSelected(false);
        assertAllChildrenNotSelected(root);
        assertFalse(root.isIndeterminate());
    }
    
    @Test public void testIndependent_updateChild1Selected() {
        setAllIndependent(root);
        assertAllChildrenNotSelected(root);
        
        child1.setSelected(true);
        assertIsSelected(child1);
        
        assertIsNotSelected(root);
        assertAllChildrenNotSelected(child2);
        assertAllChildrenNotSelected(child3);
        
        assertFalse(root.isIndeterminate());
        assertFalse(child2.isIndeterminate());
        assertFalse(child3.isIndeterminate());
    }
    
    @Test public void testIndependent_updateChild2Selected() {
        setAllIndependent(root);
        assertAllChildrenNotSelected(root);
        
        child2.setSelected(true);
        
        assertIsNotSelected(root);
        assertTrue(child2.isSelected());
        assertFalse(subchild1.isSelected());
        assertFalse(subchild2.isSelected());
        assertAllChildrenNotSelected(child3);
        
        assertFalse(root.isIndeterminate());
        assertFalse(child1.isIndeterminate());
        assertFalse(child2.isIndeterminate());
        assertFalse(child3.isIndeterminate());
    }
    
    
    /**************************************************************************
     * 
     * Indeterminate CheckBoxTreeItem tests      
     * 
     *************************************************************************/
    
    @Test public void testIndeterminate_onRootNode() {
        assertFalse(child1.isIndeterminate());
        assertFalse(root.isIndependent());
        
        root.setIndeterminate(true);
        assertTrue(root.isIndeterminate());
        assertFalse(child1.isIndeterminate());
        assertFalse(child2.isIndeterminate());
        assertFalse(subchild1.isIndeterminate());
        assertFalse(subchild2.isIndeterminate());
        assertFalse(child3.isIndeterminate());
    }
    
    @Test public void testIndeterminate_onChild1Node() {
        assertFalse(child1.isIndeterminate());
        
        child1.setIndeterminate(true);
        assertTrue(root.isIndeterminate());
        assertTrue(child1.isIndeterminate());
        assertFalse(child2.isIndeterminate());
        assertFalse(subchild1.isIndeterminate());
        assertFalse(subchild2.isIndeterminate());
        assertFalse(child3.isIndeterminate());
    }
    
    
    /**************************************************************************
     * 
     * TreeModificationEvent tests      
     * 
     *************************************************************************/
    
    private int eventCount = 0;
    @Test public void testTreeModificationEvent_child1_onSelectionChanged() {
        assertEquals(0, eventCount);
        child1.addEventHandler(CheckBoxTreeItem.<String>checkBoxSelectionChangedEvent(), event -> {
            eventCount++;

            assertTrue(event.wasSelectionChanged());
            assertFalse(event.wasIndeterminateChanged());
            assertEquals(child1, event.getTreeItem());
        });
        
        child1.setSelected(true);
        assertEquals(1, eventCount);
    }
    
    @Test public void testTreeModificationEvent_child1_onSelectionChangedAgain() {
        assertEquals(0, eventCount);
        child1.addEventHandler(CheckBoxTreeItem.<String>checkBoxSelectionChangedEvent(), event -> {
            eventCount++;

            assertTrue(event.wasSelectionChanged());
            assertFalse(event.wasIndeterminateChanged());
            assertEquals(child1, event.getTreeItem());
        });
        
        child1.setSelected(true);
        assertEquals(1, eventCount);
        
        // shouldn't increment when changed to same value
        child1.setSelected(true);
        assertEquals(1, eventCount);
        
        // shouldn't increment when the root is changed and we are watching the child1
        root.setSelected(true);
        assertEquals(1, eventCount);
    }
    
    @Test public void testTreeModificationEvent_child1_onSelectionChangedOnRootWhenChild1IsSelected() {
        assertEquals(0, eventCount);
        child1.addEventHandler(CheckBoxTreeItem.<String>checkBoxSelectionChangedEvent(), event -> {
            eventCount++;

            assertTrue(event.wasSelectionChanged());
            assertFalse(event.wasIndeterminateChanged());
            assertEquals(child1, event.getTreeItem());
        });
        
        child1.setSelected(true);
        assertEquals(1, eventCount);
        
        // shouldn't increment when the root is changed and we are watching the child1
        root.setSelected(true);
        assertEquals(1, eventCount);
    }
    
    @Test public void testTreeModificationEvent_child1_onSelectionChangedOnRootWhenChild1IsNotSelected() {
        assertEquals(0, eventCount);
        child1.addEventHandler(CheckBoxTreeItem.<String>checkBoxSelectionChangedEvent(), event -> {
            eventCount++;

            assertTrue(event.wasSelectionChanged());
            assertFalse(event.wasIndeterminateChanged());
            assertEquals(child1, event.getTreeItem());
        });
        
        // should increment when the root is changed and the child1 is unselected
        root.setSelected(true);
        assertEquals(1, eventCount);
        
        child1.setSelected(false);
        assertEquals(2, eventCount);
    }
    
    
    
    
    @Test public void testTreeModificationEvent_child1_onIndeterminateChanged() {
        assertEquals(0, eventCount);
        child1.addEventHandler(CheckBoxTreeItem.<String>checkBoxSelectionChangedEvent(), event -> {
            eventCount++;

            assertFalse(event.wasSelectionChanged());
            assertTrue(event.wasIndeterminateChanged());
            assertEquals(child1, event.getTreeItem());
        });
        
        child1.setIndeterminate(true);
        assertEquals(1, eventCount);
    }
    
    @Test public void testTreeModificationEvent_child1_onIndeterminateChangedAgain() {
        assertEquals(0, eventCount);
        child1.addEventHandler(CheckBoxTreeItem.<String>checkBoxSelectionChangedEvent(), event -> {
            eventCount++;

            assertFalse(event.wasSelectionChanged());
            assertTrue(event.wasIndeterminateChanged());
            assertEquals(child1, event.getTreeItem());
        });
        
        child1.setIndeterminate(true);
        assertEquals(1, eventCount);
        
        // shouldn't increment when changed to same value
        child1.setIndeterminate(true);
        assertEquals(1, eventCount);
        
        // shouldn't increment when the root is changed and we are watching the child1
        root.setIndeterminate(true);
        assertEquals(1, eventCount);
    }
    
    @Test public void testTreeModificationEvent_child1_onIndeterminateChangedOnRootWhenChild1IsIndeterminate() {
        assertEquals(0, eventCount);
        child1.addEventHandler(CheckBoxTreeItem.<String>checkBoxSelectionChangedEvent(), event -> {
            eventCount++;

            assertFalse(event.wasSelectionChanged());
            assertTrue(event.wasIndeterminateChanged());
            assertEquals(child1, event.getTreeItem());
        });
        
        child1.setIndeterminate(true);
        assertEquals(1, eventCount);
        
        // shouldn't increment when the root is changed and we are watching the child1
        root.setIndeterminate(true);
        assertEquals(1, eventCount);
    }
    
    @Test public void testTreeModificationEvent_child1_onIndeterminateChangedOnRootWhenChild1IsNotIndeterminate() {
        assertEquals(0, eventCount);
        child1.addEventHandler(CheckBoxTreeItem.<String>checkBoxSelectionChangedEvent(), event -> {
            eventCount++;

            assertFalse(event.wasSelectionChanged());
            assertTrue(event.wasIndeterminateChanged());
            assertEquals(child1, event.getTreeItem());
        });
        
        root.setIndeterminate(true);
        assertEquals(0, eventCount);
        
        child1.setIndeterminate(true);
        assertEquals(1, eventCount);
    }
}
