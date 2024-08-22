/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.SceneHelper;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassDoesNotExist;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassExists;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javafx.scene.control.SelectionModel;
import javafx.scene.input.ScrollEvent;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Test;

import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventGenerator;
import com.sun.javafx.scene.input.KeyCodeMap;
import com.sun.javafx.tk.Toolkit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.SingleSelectionModelShim;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.control.skin.TabPaneSkinShim;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPaneShim;

import java.lang.ref.WeakReference;

public class TabPaneTest {
    private TabPane tabPane;//Empty string
    private Toolkit tk;
    private SingleSelectionModel<Tab> sm;
    private Tab tab1;
    private Tab tab2;
    private Tab tab3;
    private Scene scene;
    private Stage stage;
    private StackPane root;

    @Before public void setup() {
        tk = Toolkit.getToolkit();

        assertTrue(tk instanceof StubToolkit);  // Ensure it's StubToolkit

        tabPane = new TabPane();
        tab1 = new Tab("one");
        tab2 = new Tab("two");
        tab3 = new Tab("three");
        sm = TabPaneShim.getTabPaneSelectionModel(tabPane);
        root = new StackPane();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
    }

    /*********************************************************************
     * Helper methods                                                    *
     ********************************************************************/

    private void show() {
        stage.show();
        stage.requestFocus();
    }

    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultConstructorShouldSetStyleClassTo_tabpane() {
        assertStyleClassContains(tabPane, "tab-pane");
    }

    @Test public void defaultConstructorSelectionModelNotNull() {
        assertNotNull(tabPane.getSelectionModel());
    }

    @Test public void defaultConstructorInitialTabsEmpty() {
        assertNotNull(tabPane.getTabs());
        assertEquals(tabPane.getTabs().size(), 0.0, 0.0);
    }

    @Test public void defaultSideIsTop() {
        assertSame(tabPane.getSide(), Side.TOP);
    }

    @Test public void defaultConstructorTabClosingPolicy() {
        assertNotNull(tabPane.getTabClosingPolicy());
        assertSame(tabPane.getTabClosingPolicy(), TabPane.TabClosingPolicy.SELECTED_TAB);
    }

    @Test public void defaultConstructorRotateGraphic() {
        assertFalse(tabPane.isRotateGraphic());
    }

    @Test public void defaultTabMinWidth() {
        assertEquals(tabPane.getTabMinWidth(), 0.0, 0.0);
    }

    @Test public void defaultTabMaxWidth() {
        assertEquals(tabPane.getTabMaxWidth(), Double.MAX_VALUE, 0.0);
    }

    @Test public void defaultTabMinHeight() {
        assertEquals(tabPane.getTabMinHeight(), 0.0, 0.0);
    }

    @Test public void defaultTabMaxHeight() {
        assertEquals(tabPane.getTabMaxHeight(), Double.MAX_VALUE, 0.0);
    }

    @Test public void defaultSelectionModelEmpty() {
        assertTrue(tabPane.getSelectionModel().isEmpty());
    }

    @Test public void initialBoundsInParentMatchesWidthAndHeight() {
        TabPane testPane = new TabPane();
        testPane.resize(400, 400);
        testPane.setSkin(new TabPaneSkin(testPane));
        Bounds boundsInParent = testPane.getBoundsInParent();
        assertEquals(testPane.getWidth(), boundsInParent.getWidth(), 0.0);
        assertEquals(testPane.getHeight(), boundsInParent.getHeight(), 0.0);
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkSelectionModelPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<SingleSelectionModel<Tab>>(null);
        tabPane.selectionModelProperty().bind(objPr);
        assertNull("selectionModel cannot be bound", tabPane.selectionModelProperty().getValue());
        objPr.setValue(sm);
        assertSame("selectionModel cannot be bound", tabPane.selectionModelProperty().getValue(), sm);
    }

    @Test public void checkSidePropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<>(Side.BOTTOM);
        tabPane.sideProperty().bind(objPr);
        assertSame("side cannot be bound", tabPane.sideProperty().getValue(), Side.BOTTOM);
        objPr.setValue(Side.RIGHT);
        assertSame("side cannot be bound", tabPane.sideProperty().getValue(), Side.RIGHT);
    }

    @Test public void checkTabClosingPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<>(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.tabClosingPolicyProperty().bind(objPr);
        assertSame("side cannot be bound", tabPane.tabClosingPolicyProperty().getValue(), TabPane.TabClosingPolicy.UNAVAILABLE);
        objPr.setValue(TabPane.TabClosingPolicy.ALL_TABS);
        assertSame("side cannot be bound", tabPane.tabClosingPolicyProperty().getValue(), TabPane.TabClosingPolicy.ALL_TABS);
    }

    @Test public void checkRotateGraphicsPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(false);
        tabPane.rotateGraphicProperty().bind(objPr);
        assertFalse("rotateGraphic cannot be bound", tabPane.rotateGraphicProperty().getValue());
        objPr.setValue(true);
        assertTrue("rotateGraphic cannot be bound", tabPane.rotateGraphicProperty().getValue());
    }

    @Test public void checkTabMinWidthPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        tabPane.tabMinWidthProperty().bind(objPr);
        assertEquals("tabMinWidthProperty cannot be bound", tabPane.tabMinWidthProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("tabMinWidthProperty cannot be bound", tabPane.tabMinWidthProperty().getValue(), 5.0, 0.0);
    }

    @Test public void checkTabMaxWidthPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        tabPane.tabMaxWidthProperty().bind(objPr);
        assertEquals("tabMaxWidthProperty cannot be bound", tabPane.tabMaxWidthProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("tabMaxWidthProperty cannot be bound", tabPane.tabMaxWidthProperty().getValue(), 5.0, 0.0);
    }


    @Test public void checkTabMinHeightPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        tabPane.tabMinHeightProperty().bind(objPr);
        assertEquals("tabMinHeightProperty cannot be bound", tabPane.tabMinHeightProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("tabMinHeightProperty cannot be bound", tabPane.tabMinHeightProperty().getValue(), 5.0, 0.0);
    }

    @Test public void checkTabMaxHeightPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(2.0);
        tabPane.tabMaxHeightProperty().bind(objPr);
        assertEquals("tabMaxHeightProperty cannot be bound", tabPane.tabMaxHeightProperty().getValue(), 2.0, 0.0);
        objPr.setValue(5.0);
        assertEquals("tabMaxHeightProperty cannot be bound", tabPane.tabMaxHeightProperty().getValue(), 5.0, 0.0);
    }

    @Test public void selectionModelPropertyHasBeanReference() {
        assertSame(tabPane, tabPane.selectionModelProperty().getBean());
    }

    @Test public void selectionModelPropertyHasName() {
        assertEquals("selectionModel", tabPane.selectionModelProperty().getName());
    }

    @Test public void sidePropertyHasBeanReference() {
        assertSame(tabPane, tabPane.sideProperty().getBean());
    }

    @Test public void sidePropertyHasName() {
        assertEquals("side", tabPane.sideProperty().getName());
    }

    @Test public void tabClosingPolicyPropertyHasBeanReference() {
        assertSame(tabPane, tabPane.tabClosingPolicyProperty().getBean());
    }

    @Test public void tabClosingPolicyPropertyHasName() {
        assertEquals("tabClosingPolicy", tabPane.tabClosingPolicyProperty().getName());
    }

    @Test public void rotateGraphicPropertyHasBeanReference() {
        assertSame(tabPane, tabPane.rotateGraphicProperty().getBean());
    }

    @Test public void rotateGraphicPropertyHasName() {
        assertEquals("rotateGraphic", tabPane.rotateGraphicProperty().getName());
    }

    @Test public void tabMinWidthPropertyHasBeanReference() {
        assertSame(tabPane, tabPane.tabMinWidthProperty().getBean());
    }

    @Test public void tabMinWidthPropertyHasName() {
        assertEquals("tabMinWidth", tabPane.tabMinWidthProperty().getName());
    }

    @Test public void tabMaxWidthPropertyHasBeanReference() {
        assertSame(tabPane, tabPane.tabMaxWidthProperty().getBean());
    }

    @Test public void tabMaxWidthPropertyHasName() {
        assertEquals("tabMaxWidth", tabPane.tabMaxWidthProperty().getName());
    }

    @Test public void tabMinHeightPropertyHasBeanReference() {
        assertSame(tabPane, tabPane.tabMinHeightProperty().getBean());
    }

    @Test public void tabMinHeightPropertyHasName() {
        assertEquals("tabMinHeight", tabPane.tabMinHeightProperty().getName());
    }

    @Test public void tabMaxHeightPropertyHasBeanReference() {
        assertSame(tabPane, tabPane.tabMaxHeightProperty().getBean());
    }

    @Test public void tabMaxHeightPropertyHasName() {
        assertEquals("tabMaxHeight", tabPane.tabMaxHeightProperty().getName());
    }


    /*********************************************************************
     * Check for Pseudo classes                                          *
     ********************************************************************/

    @Test public void settingSideSetsPseudoClass() {
        tabPane.setSide(Side.BOTTOM);
        assertPseudoClassExists(tabPane, "bottom");
        assertPseudoClassDoesNotExist(tabPane, "top");
        assertPseudoClassDoesNotExist(tabPane, "left");
        assertPseudoClassDoesNotExist(tabPane, "right");
    }

    @Test public void clearingSideClearsPseudoClass() {
        tabPane.setSide(Side.BOTTOM);
        tabPane.setSide(Side.LEFT);
        assertPseudoClassExists(tabPane, "left");
        assertPseudoClassDoesNotExist(tabPane, "bottom");
        assertPseudoClassDoesNotExist(tabPane, "top");
        assertPseudoClassDoesNotExist(tabPane, "right");
    }


    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/

    @Test public void whenTabMinWidthIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMinWidthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
        DoubleProperty other = new SimpleDoubleProperty(30.0);
        tabPane.tabMinWidthProperty().bind(other);
        assertFalse(styleable.isSettable(tabPane));
    }

    @Test public void whenTabMinWidthIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMinWidthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
    }

    @Test public void canSpecifyTabMinWidthViaCSS() {
        ((StyleableProperty)tabPane.tabMinWidthProperty()).applyStyle(null, 34.0);
        assertEquals(34.0, tabPane.getTabMinWidth(), 0.0);
    }

    @Test public void whenTabMaxWidthIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMaxWidthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
        DoubleProperty other = new SimpleDoubleProperty(30.0);
        tabPane.tabMaxWidthProperty().bind(other);
        assertFalse(styleable.isSettable(tabPane));
    }

    @Test public void whenTabMaxWidthIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMaxWidthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
    }

    @Test public void canSpecifyTabMaxWidthViaCSS() {
        ((StyleableProperty)tabPane.tabMaxWidthProperty()).applyStyle(null, 34.0);
        assertEquals(34.0, tabPane.getTabMaxWidth(), 0.0);
    }

    @Test public void whenTabMinHeightIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMinHeightProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
        DoubleProperty other = new SimpleDoubleProperty(30.0);
        tabPane.tabMinHeightProperty().bind(other);
        assertFalse(styleable.isSettable(tabPane));
    }

    @Test public void whenTabMinHeightIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMinHeightProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
    }

    @Test public void canSpecifyTabMinHeightViaCSS() {
        ((StyleableProperty)tabPane.tabMinHeightProperty()).applyStyle(null, 34.0);
        assertEquals(34.0, tabPane.getTabMinHeight(), 0.0);
    }

    @Test public void whenTabMaxHeightIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMaxHeightProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
        DoubleProperty other = new SimpleDoubleProperty(30.0);
        tabPane.tabMaxHeightProperty().bind(other);
        assertFalse(styleable.isSettable(tabPane));
    }

    @Test public void whenTabMaxHeightIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMaxHeightProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
    }

    @Test public void canSpecifyTabMaxHeightViaCSS() {
        ((StyleableProperty)tabPane.tabMaxHeightProperty()).applyStyle(null, 34.0);
        assertEquals(34.0, tabPane.getTabMaxHeight(), 0.0);
    }


    /*********************************************************************
     * Miscellaneous Tests                                               *
     ********************************************************************/

    @Test public void setSelectionModelAndSeeValueIsReflectedInModel() {
        tabPane.setSelectionModel(sm);
        assertSame(tabPane.selectionModelProperty().getValue(), sm);
    }

    @Test public void setselectionModelAndSeeValue() {
        tabPane.setSelectionModel(sm);
        assertSame(tabPane.getSelectionModel(), sm);
    }

    @Test public void setSideAndSeeValueIsReflectedInModel() {
        tabPane.setSide(Side.BOTTOM);
        assertSame(tabPane.sideProperty().getValue(), Side.BOTTOM);
    }

    @Test public void setSideAndSeeValue() {
        tabPane.setSide(Side.LEFT);
        assertSame(tabPane.getSide(), Side.LEFT);
    }

    @Test public void setTabClosingPolicyAndSeeValueIsReflectedInModel() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        assertSame(tabPane.tabClosingPolicyProperty().getValue(), TabPane.TabClosingPolicy.ALL_TABS);
    }

    @Test public void setTabClosingPolicyAndSeeValue() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        assertSame(tabPane.getTabClosingPolicy(), TabPane.TabClosingPolicy.UNAVAILABLE);
    }

    @Test public void setTabDragPolicyAndSeeValueIsReflectedInModel() {
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        assertSame(TabPane.TabDragPolicy.REORDER, tabPane.tabDragPolicyProperty().getValue());

        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
        assertSame(TabPane.TabDragPolicy.FIXED, tabPane.tabDragPolicyProperty().getValue());
    }

    @Test public void setTabDragPolicyAndSeeValue() {
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        assertSame(TabPane.TabDragPolicy.REORDER, tabPane.getTabDragPolicy());

        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
        assertSame(TabPane.TabDragPolicy.FIXED, tabPane.getTabDragPolicy());
    }

    @Test public void tabDragPolicyReorderAndAsymmetricMouseEvent() {
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        root.getChildren().add(tabPane);
        show();

        root.layout();

        double xval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinX();
        double yval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinY();

        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+75, yval+20));
        tk.firePulse();

        SceneHelper.processMouseEvent(scene,
                MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, xval+75, yval+20));
        tk.firePulse();
    }

    @Test public void setRotateGraphicAndSeeValueIsReflectedInModel() {
        tabPane.setRotateGraphic(true);
        assertTrue(tabPane.rotateGraphicProperty().getValue());
    }

    @Test public void setRotateGraphicAndSeeValue() {
        tabPane.setRotateGraphic(true);
        assertTrue(tabPane.isRotateGraphic());
    }

    @Test public void setTabMinWidthAndSeeValueIsReflectedInModel() {
        tabPane.setTabMinWidth(30.0);
        assertEquals(tabPane.tabMinWidthProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setTabMinWidthAndSeeValue() {
        tabPane.setTabMinWidth(30.0);
        assertEquals(tabPane.getTabMinWidth(), 30.0, 0.0);
    }

    @Test public void setTabMaxWidthAndSeeValueIsReflectedInModel() {
        tabPane.setTabMaxWidth(30.0);
        assertEquals(tabPane.tabMaxWidthProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setTabMaxWidthAndSeeValue() {
        tabPane.setTabMaxWidth(30.0);
        assertEquals(tabPane.getTabMaxWidth(), 30.0, 0.0);
    }

    @Test public void setTabMinHeightAndSeeValueIsReflectedInModel() {
        tabPane.setTabMinHeight(30.0);
        assertEquals(tabPane.tabMinHeightProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setTabMinHeightAndSeeValue() {
        tabPane.setTabMinHeight(30.0);
        assertEquals(tabPane.getTabMinHeight(), 30.0, 0.0);
    }

    @Test public void setTabMaxHeightAndSeeValueIsReflectedInModel() {
        tabPane.setTabMaxHeight(30.0);
        assertEquals(tabPane.tabMaxHeightProperty().getValue(), 30.0, 0.0);
    }

    @Test public void setTabMaxHeightAndSeeValue() {
        tabPane.setTabMaxHeight(30.0);
        assertEquals(tabPane.getTabMaxHeight(), 30.0, 0.0);
    }

    @Test public void addTabsCheckItemCountInSelectionModel() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        assertEquals(SingleSelectionModelShim.getItemCount(tabPane.getSelectionModel()), 2.0, 0.0);
    }

    @Test public void addTabsCheckItemInSelectionModel() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        assertSame(SingleSelectionModelShim.getModelItem(tabPane.getSelectionModel(), 0), tab1);
        assertSame(SingleSelectionModelShim.getModelItem(tabPane.getSelectionModel(), 1), tab2);
    }

    @Test public void addTabsCheckSelectUsingObjInSelectionModel() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getSelectionModel().select(tab1);
        assertTrue(tabPane.getSelectionModel().isSelected(0));
    }

    @Test public void addTabsCheckSelectUsingIndexInSelectionModel() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getSelectionModel().select(1);
        assertTrue(tabPane.getSelectionModel().isSelected(1));
    }

    @Test public void addTabsCheckClearAndSelectUsingIndexInSelectionModel() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getSelectionModel().clearAndSelect(1);
        assertTrue(tabPane.getSelectionModel().isSelected(1));
    }

    @Test public void addTabsCheckSelectMultipleItemsAndClearOneItemInSelectionModel() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getSelectionModel().select(0);
        tabPane.getSelectionModel().select(1);
        tabPane.getSelectionModel().clearSelection(0);
        assertFalse(tabPane.getSelectionModel().isSelected(0));
        assertTrue(tabPane.getSelectionModel().isSelected(1));
    }

    @Test public void addTabsCheckSelectMultipleItemsAndClearAllSelectionInSelectionModel() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getSelectionModel().select(0);
        tabPane.getSelectionModel().select(1);
        tabPane.getSelectionModel().clearSelection();
        assertFalse(tabPane.getSelectionModel().isSelected(0));
        assertFalse(tabPane.getSelectionModel().isSelected(1));
        assertTrue(tabPane.getSelectionModel().isEmpty());
    }

    @Test public void addTabsCheckSelectUsingIteratorKindOfMethodsInSelectionModel() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getSelectionModel().selectFirst();
        assertTrue(tabPane.getSelectionModel().isSelected(0));
        assertFalse(tabPane.getSelectionModel().isSelected(1));
        tabPane.getSelectionModel().selectNext();
        assertFalse(tabPane.getSelectionModel().isSelected(0));
        assertTrue(tabPane.getSelectionModel().isSelected(1));
        tabPane.getSelectionModel().clearSelection();
        tabPane.getSelectionModel().selectLast();
        assertFalse(tabPane.getSelectionModel().isSelected(0));
        assertTrue(tabPane.getSelectionModel().isSelected(1));
        tabPane.getSelectionModel().selectPrevious();
        assertTrue(tabPane.getSelectionModel().isSelected(0));
        assertFalse(tabPane.getSelectionModel().isSelected(1));
    }

    @Test public void flipTabOrder_RT20156() {
        tabPane.setSkin(new TabPaneSkin(tabPane));
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        assertEquals("one", tabPane.getTabs().get(0).getText());
        assertEquals("two", tabPane.getTabs().get(1).getText());
        assertEquals("three", tabPane.getTabs().get(2).getText());

        final int lastTabIndex = tabPane.getTabs().size()-1;
        final Tab lastTab = tabPane.getTabs().get(lastTabIndex);
        tabPane.getTabs().remove(lastTabIndex);
        tabPane.getTabs().add(0, lastTab);

        assertEquals("three", tabPane.getTabs().get(0).getText());
        assertEquals("one", tabPane.getTabs().get(1).getText());
        assertEquals("two", tabPane.getTabs().get(2).getText());
        assertTrue(tabPane.getSelectionModel().isSelected(1));
    }

    @Test public void disableAllTabs() {
        tabPane.setSkin(new TabPaneSkin(tabPane));
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        tab1.setDisable(true);
        tab2.setDisable(true);
        tab3.setDisable(true);

        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab1, tabPane.getSelectionModel().getSelectedItem());
    }

    @Test public void disableFirstTabs() {
        tabPane.setSkin(new TabPaneSkin(tabPane));
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        tab1.setDisable(true);
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab1, tabPane.getSelectionModel().getSelectedItem());
    }

    @Test public void disableATabAndSelectItByIndex() {
        tabPane.setSkin(new TabPaneSkin(tabPane));
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        tab2.setDisable(true);
        tabPane.getSelectionModel().select(1);
        assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
    }

    @Test public void selectByIndexADisableTab() {
        tabPane.setSkin(new TabPaneSkin(tabPane));
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        tabPane.getSelectionModel().select(1);
        tab2.setDisable(true);
        assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
        assertTrue(tab2.isDisable());
    }

    @Test public void disableATabAndSelectItByTab() {
        tabPane.setSkin(new TabPaneSkin(tabPane));
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        tab2.setDisable(true);
        tabPane.getSelectionModel().select(tab2);
        assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
    }

    @Test public void selectByTabADisableTab() {
        tabPane.setSkin(new TabPaneSkin(tabPane));
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        tabPane.getSelectionModel().select(tab2);
        tab2.setDisable(true);
        assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
        assertTrue(tab2.isDisable());
    }

    @Test public void navigateOverDisableTab() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        root.getChildren().add(tabPane);
        show();
        tabPane.requestFocus();

        assertTrue(tabPane.isFocused());

        tab2.setDisable(true);
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab1, tabPane.getSelectionModel().getSelectedItem());
        assertTrue(tab2.isDisable());

        KeyEventFirer keyboard = new KeyEventFirer(tabPane);
        keyboard.doRightArrowPress();

        assertEquals(2, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab3, tabPane.getSelectionModel().getSelectedItem());

        keyboard.doLeftArrowPress();

        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab1, tabPane.getSelectionModel().getSelectedItem());
    }

    @Test public void mousePressSelectsATab_RT20476() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        tab1.setContent(new Button("TAB1"));
        tab2.setContent(new Button("TAB2"));
        tab3.setContent(new Button("TAB3"));

        root.getChildren().add(tabPane);
        show();

        root.applyCss();
        root.resize(300, 300);
        root.layout();

        tk.firePulse();
        assertTrue(tabPane.isFocused());

        double xval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinX();
        double yval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinY();

        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+75, yval+20));
        tk.firePulse();

        assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
    }

    private int counter = 0;
    @Test public void setOnSelectionChangedFiresTwice_RT21089() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);

        tab1.setContent(new Button("TAB1"));
        tab2.setContent(new Button("TAB2"));

        root.getChildren().add(tabPane);
        show();

        root.applyCss();
        root.resize(300, 300);
        root.layout();

        tk.firePulse();
        assertTrue(tabPane.isFocused());

        tab2.setOnSelectionChanged(event -> {
            assertEquals(0, counter++);
        });

        double xval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinX();
        double yval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinY();

        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+75, yval+20));
        tk.firePulse();

        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());

        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+75, yval+20));
        tk.firePulse();
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
    }

    @Test public void unableToSelectNextTabWhenFirstTabIsClosed_RT22326() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        assertEquals("one", tabPane.getTabs().get(0).getText());
        assertEquals("two", tabPane.getTabs().get(1).getText());
        assertEquals("three", tabPane.getTabs().get(2).getText());

        tabPane.getTabs().remove(tab1);

        assertEquals(2, tabPane.getTabs().size());
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
        tabPane.getSelectionModel().selectNext();
        assertEquals(tab3, tabPane.getSelectionModel().getSelectedItem());
    }

    @Test public void removeFirstTabNextTabShouldBeSelected() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        assertEquals("one", tabPane.getTabs().get(0).getText());
        assertEquals("two", tabPane.getTabs().get(1).getText());
        assertEquals("three", tabPane.getTabs().get(2).getText());

        tabPane.getTabs().remove(tab1);

        assertEquals(2, tabPane.getTabs().size());
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
    }

    @Test public void selectionModelShouldNotBeNullWhenClosingFirstTab_RT22925() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);

        tabPane.getSelectionModel().selectedItemProperty().addListener((ov, t, t1) -> {
            assertEquals(t.getText(), "one");
            assertEquals(t1.getText(), "two");
        });

        assertEquals("one", tabPane.getTabs().get(0).getText());
        assertEquals("two", tabPane.getTabs().get(1).getText());
        assertEquals("three", tabPane.getTabs().get(2).getText());

        tabPane.getTabs().remove(tab1);

        assertEquals(2, tabPane.getTabs().size());
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
    }


    boolean button1Focused = false;
    @Test public void focusTraversalShouldLookInsideEmbeddedEngines() {

        Button b1 = new Button("Button1");
        final ChangeListener<Boolean> focusListener = (observable, oldVal, newVal) -> {
            button1Focused = true;
        };
        b1.focusedProperty().addListener(focusListener);

        final ScrollPane sp = new ScrollPane();
        final VBox vbox1 = new VBox();
        vbox1.setSpacing(10);
        vbox1.setTranslateX(10);
        vbox1.setTranslateY(10);
        vbox1.getChildren().addAll(b1);
        tab1.setContent(vbox1);
        sp.setContent(vbox1);
        tab1.setContent(sp);
        tabPane.getTabs().add(tab1);

        tabPane.getTabs().add(tab2);

        final Scene scene1 = new Scene(new Group(), 400, 400);
        ((Group)scene1.getRoot()).getChildren().add(tabPane);

        stage.setScene(scene1);
        stage.show();
        stage.requestFocus();

        final KeyEvent tabEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCodeMap.valueOf(0x09),
                                                         false, false, false, false);
        Platform.runLater(() -> {
            tabPane.requestFocus();
            Event.fireEvent(tabPane, tabEvent);

        });

        assertTrue(button1Focused);

    }

    @Test public void test_rt_35013() {
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(new Button("Button1"), new Button("Button2"));

        TabPane tabPane = new TabPane();
        Tab emptyTab;
        Tab splitTab = new Tab("SplitPane Tab");
        splitTab.setContent(splitPane);
        tabPane.getTabs().addAll(emptyTab = new Tab("Empty Tab"), splitTab);

        StageLoader sl = new StageLoader(tabPane);

        tabPane.getSelectionModel().select(emptyTab);
        Toolkit.getToolkit().firePulse();
        assertFalse(splitPane.getParent().isVisible());

        tabPane.getSelectionModel().select(splitTab);
        Toolkit.getToolkit().firePulse();
        assertTrue(splitPane.getParent().isVisible());

        sl.dispose();
    }

    @Test public void test_rt_36456_default_selectionMovesBackwardOne() {
        Tab tab0 = new Tab("Tab 0");
        Tab tab1 = new Tab("Tab 1");
        Tab tab2 = new Tab("Tab 2");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(tab0, tab1, tab2);
        tabPane.getSelectionModel().select(tab1);

        StageLoader sl = new StageLoader(tabPane);

        assertEquals(tab1, tabPane.getSelectionModel().getSelectedItem());
        tabPane.getTabs().remove(tab1);
        assertEquals(tab0, tabPane.getSelectionModel().getSelectedItem());

        sl.dispose();
    }

    @Test public void test_rt_36456_selectionMovesBackwardTwoSkippingDisabledTab() {
        Tab tab0 = new Tab("Tab 0");
        Tab tab1 = new Tab("Tab 1");
        tab1.setDisable(true);
        Tab tab2 = new Tab("Tab 2");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(tab0, tab1, tab2);
        tabPane.getSelectionModel().select(tab2);

        StageLoader sl = new StageLoader(tabPane);

        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
        tabPane.getTabs().remove(tab2);

        // selection should jump from tab2 to tab0, as tab1 is disabled
        assertEquals(tab0, tabPane.getSelectionModel().getSelectedItem());

        sl.dispose();
    }

    @Test public void test_rt_36456_selectionMovesForwardOne() {
        Tab tab0 = new Tab("Tab 0");
        tab0.setDisable(true);
        Tab tab1 = new Tab("Tab 1");
        Tab tab2 = new Tab("Tab 2");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(tab0, tab1, tab2);
        tabPane.getSelectionModel().select(tab1);

        StageLoader sl = new StageLoader(tabPane);

        assertEquals(tab1, tabPane.getSelectionModel().getSelectedItem());
        tabPane.getTabs().remove(tab1);

        // selection should move to the next non-disabled tab - in this case tab2
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());

        sl.dispose();
    }

    @Test public void test_rt_36456_selectionMovesForwardTwoSkippingDisabledTab() {
        Tab tab0 = new Tab("Tab 0");
        Tab tab1 = new Tab("Tab 1");
        tab1.setDisable(true);
        Tab tab2 = new Tab("Tab 2");

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(tab0, tab1, tab2);
        tabPane.getSelectionModel().select(tab0);

        StageLoader sl = new StageLoader(tabPane);

        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        assertEquals(tab0, selectedTab);
        tabPane.getTabs().remove(tab0);

        // selection should move to the next non-disabled tab - in this case tab2
        selectedTab = tabPane.getSelectionModel().getSelectedItem();
        assertEquals(tab2.getText() + " != " +  tab2.getText(), tab2, selectedTab);

        sl.dispose();
    }

    @Test public void test_rt_36908() {
        TabPane pane = new TabPane();
        final Tab disabled = new Tab("Disabled");
        disabled.setDisable(true);

        Tab tab1 = new Tab("Tab 1");
        Tab tab2 = new Tab("Tab 2");
        pane.getTabs().addAll(disabled, tab1, tab2);

        assertEquals(1, pane.getSelectionModel().getSelectedIndex());
        assertEquals(tab1, pane.getSelectionModel().getSelectedItem());
    }

    @Test public void test_rt_24658() {
        Button btn = new Button("Button");
        final Tab disabled = new Tab("Disabled");
        disabled.setContent(btn);

        TabPane pane = new TabPane();
        pane.getTabs().addAll(disabled);

        assertEquals(0, pane.getSelectionModel().getSelectedIndex());
        assertEquals(disabled, pane.getSelectionModel().getSelectedItem());
        assertFalse(btn.isDisabled());

        disabled.setDisable(true);
        assertTrue(btn.isDisabled());

        disabled.setDisable(false);
        assertFalse(btn.isDisabled());
    }

    @Test public void test_rt_38382_noAddToTabPane() {
        test_rt_38382(false);
    }

    @Test public void test_rt_38382_addToTabPane() {
        test_rt_38382(true);
    }

    public void test_rt_38382(boolean addToTabPane) {
        final List<String> names = Arrays.asList(
                "Biomass",
                "Exploitable Population Biomass",
                "MSY",
                "Yield",
                "Recruitment",
                "Catch",
                "Effort");
        final Map<Tab, List<String>> fooMap = new HashMap<>();
        final List<Tab> tabList = new LinkedList<>();
        for (String name : names) {
            final Tab tab = new Tab();
            tab.setText(name);
            fooMap.put(tab, new LinkedList<>());
            tabList.add(tab);
        }
        TabPane tabPane = new TabPane();

        if (addToTabPane) {
            tabPane.getTabs().setAll(tabList);
        }

        fooMap.entrySet().forEach(entry -> {
            final Tab tab = entry.getKey();
            assertTrue(tabList.contains(tab));
            assertTrue(fooMap.containsKey(tab));
        });
    }

    // Test for JDK-8189424
    int selectionChangeCount = 0;
    @Test public void testTabClosingEventAndSelectionChange() {
        Tab t1 = new Tab("");
        Tab t2 = new Tab("");
        tabPane.getTabs().add(t1);
        tabPane.getTabs().add(t2);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        tabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            selectionChangeCount++;
        });
        t1.setOnCloseRequest((event) -> {
            event.consume();
            tabPane.getTabs().remove(t1);
        });

        root.getChildren().add(tabPane);
        show();

        root.applyCss();
        root.resize(300, 300);
        root.layout();

        tk.firePulse();
        assertTrue(tabPane.isFocused());

        tabPane.getSelectionModel().select(t1);

        double xval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinX();
        double yval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinY();

        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval + 19, yval + 17));
        tk.firePulse();
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval + 19, yval + 17));
        tk.firePulse();

        assertEquals(1, tabPane.getTabs().size());
        assertEquals(t2, tabPane.getSelectionModel().getSelectedItem());
        assertEquals(1, selectionChangeCount);

        tabPane.getTabs().remove(t2);
    }

    // Test for JDK-8193495
    @Test public void testQuickRemoveAddTab() {
        int tabHeaderMinWidth = 200;
        int tabHeaderMinHeight = 50;
        tabPane.setMaxSize(400, 200);
        tabPane.setTabMinWidth(tabHeaderMinWidth);
        tabPane.setTabMinHeight(tabHeaderMinHeight);
        tabPane.getTabs().addAll(tab1, tab2, tab3);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        root.getChildren().add(tabPane);
        show();
        tabPane.requestFocus();
        tk.firePulse();
        assertTrue(tabPane.isFocused());

        tabPane.getTabs().add(1, tabPane.getTabs().remove(0));
        tk.firePulse();
        tabPane.getSelectionModel().select(tab1);
        tk.firePulse();

        double xval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinX();
        double yval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinY();

        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval + 19, yval + 17));
        tk.firePulse();
        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval + 19, yval + 17));
        tk.firePulse();

        assertEquals("Tabpane should have 3 tabs.", 3, tabPane.getTabs().size());
        assertEquals("tab2 should be at index 0.", tab2, tabPane.getSelectionModel().getSelectedItem());
    }

    // Test for JDK-8154039
    @Test public void testSelectNonChildTab() {
        tabPane.getTabs().addAll(tab1);
        root.getChildren().add(tabPane);
        show();
        tk.firePulse();
        WeakReference<Tab> weakTab = new WeakReference<>(new Tab("NonChildTab"));
        tabPane.getSelectionModel().select(weakTab.get());
        tk.firePulse();
        attemptGC(10, weakTab);
        tk.firePulse();
        assertNull(weakTab.get());
    }

    private void attemptGC(int n, WeakReference<?> weakRef) {
        // Attempt gc n times
        for (int i = 0; i < n; i++) {
            System.gc();

            if (weakRef.get() == null) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                fail("InterruptedException occurred during Thread.sleep()");
            }
        }
    }

    // Test for JDK-8157690
    @Test public void testPopupItemsOnSortingTabs() {
        tabPane.setMaxSize(20, 20);
        root.getChildren().add(tabPane);
        tabPane.getTabs().addAll(tab1, tab2, tab3);
        show();
        tk.firePulse();
        TabPaneSkin tbSkin = (TabPaneSkin) tabPane.getSkin();
        assertNotNull(tbSkin);
        ContextMenu tabsMenu = TabPaneSkinShim.getTabsMenu(tbSkin);
        assertNotNull(tabsMenu);
        assertEquals("ContextMenu should contain 3 items.", 3, tabsMenu.getItems().size());

        tabPane.getTabs().sort((o1, o2) -> sortCompare(o1, o2));
        tk.firePulse();
        assertEquals("ContextMenu should contain 3 items.", 3, tabsMenu.getItems().size());
    }

    private int sortCompare(Tab t1, Tab t2) {
        return t2.getText().compareTo(t1.getText());
    }

    class TabPaneSkin1 extends TabPaneSkin {
        TabPaneSkin1(TabPane tabPane) {
            super(tabPane);
        }
    }

    @Test
    public void testNPEOnSwitchSkinAndChangeSelection() {
        tabPane.getTabs().addAll(tab1, tab2);
        root.getChildren().add(tabPane);
        stage.show();
        tk.firePulse();

        tabPane.setSkin(new TabPaneSkin1(tabPane));
        tk.firePulse();
        tabPane.getSelectionModel().select(1);
        tk.firePulse();
    }

    @Test
    public void testSMLeakOnSwitchSkinAndSM() {
        tabPane.getTabs().addAll(tab1, tab2);
        root.getChildren().add(tabPane);
        stage.show();
        tk.firePulse();

        WeakReference<SelectionModel<Tab>> weakSMRef = new WeakReference<>(tabPane.getSelectionModel());
        tabPane.setSkin(new TabPaneSkin1(tabPane));
        tk.firePulse();
        tabPane.setSelectionModel(TabPaneShim.getTabPaneSelectionModel(tabPane));
        tk.firePulse();
        attemptGC(10, weakSMRef);
        assertNull(weakSMRef.get());
    }

    @Test
    public void testVerticalScrollTopSide() {
        scrollTabPane(Side.TOP, 0, -100);
    }

    @Test
    public void testVerticalScrollRightSide() {
        scrollTabPane(Side.RIGHT, 0, 100);
    }

    @Test
    public void testVerticalScrollBottomSide() {
        scrollTabPane(Side.BOTTOM, 0, -100);
    }

    @Test
    public void testVerticalScrollLeftSide() {
        scrollTabPane(Side.LEFT, 0, 100);
    }

    @Test
    public void testHorizontalScrollTopSide() {
        scrollTabPane(Side.TOP, -100, 0);
    }

    @Test
    public void testHorizontalScrollRightSide() {
        scrollTabPane(Side.RIGHT, 100, 0);
    }

    @Test
    public void testHorizontalScrollBottomSide() {
        scrollTabPane(Side.BOTTOM, -100, 0);
    }

    @Test
    public void testHorizontalScrollLeftSide() {
        scrollTabPane(Side.LEFT, 100, 0);
    }

    private void scrollTabPane(Side side, double deltaX, double deltaY) {
        tabPane.setMaxSize(400, 100);
        tabPane.setSide(side);
        for (int i = 0; i < 40; i++) {
            Tab tab = new Tab("Tab " + (1000 + i));
            tabPane.getTabs().add(tab);
        }
        root.getChildren().add(tabPane);
        stage.show();

        Bounds firstTabBounds = tabPane.lookupAll(".tab-label")
                .stream()
                .findFirst()
                .map(n -> n.localToScene(n.getLayoutBounds()))
                .orElse(null);
        assertNotNull(firstTabBounds);

        Bounds layoutBounds = tabPane.getLayoutBounds();
        double minX = tabPane.localToScene(layoutBounds).getMinX();
        double minY = tabPane.localToScene(layoutBounds).getMinY();
        double minScrX = tabPane.localToScreen(layoutBounds).getMinX();
        double minScrY = tabPane.localToScreen(layoutBounds).getMinY();
        double x = 50;
        double y = 10;

        SceneHelper.processMouseEvent(scene,
                MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_MOVED, minX + x, minY + y));
        tk.firePulse();

        StackPane tabHeaderArea = (StackPane) tabPane.lookup(".tab-header-area");
        assertNotNull(tabHeaderArea);

        Event.fireEvent(tabHeaderArea, new ScrollEvent(
                ScrollEvent.SCROLL,
                minX + x, minY + y,
                minScrX + x, minScrY + y,
                false, false, false, false, true, false,
                deltaX, deltaY, deltaX, deltaY,
                ScrollEvent.HorizontalTextScrollUnits.NONE, 0.0,
                ScrollEvent.VerticalTextScrollUnits.NONE, 0.0,
                0, null));
        tk.firePulse();

        Bounds newFirstTabBounds = tabPane.lookupAll(".tab-label")
                .stream()
                .findFirst()
                .map(n -> n.localToScene(n.getLayoutBounds()))
                .orElse(null);
        assertNotNull(newFirstTabBounds);

        if (side.equals(Side.TOP) || side.equals(Side.BOTTOM)) {
            double delta = Math.abs(deltaY) > Math.abs(deltaX) ? deltaY : deltaX;
            assertEquals(firstTabBounds.getMinX() + delta, newFirstTabBounds.getMinX(), 0);
            assertEquals(firstTabBounds.getMinY(), newFirstTabBounds.getMinY(), 0);
        } else {
            assertEquals(firstTabBounds.getMinX(), newFirstTabBounds.getMinX(), 0);
            assertEquals(firstTabBounds.getMinY() - deltaY, newFirstTabBounds.getMinY(), 0);
        }
    }
}
