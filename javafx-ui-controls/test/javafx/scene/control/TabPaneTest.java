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

import static javafx.scene.control.ControlTestUtils.assertPseudoClassDoesNotExist;
import static javafx.scene.control.ControlTestUtils.assertPseudoClassExists;
import static javafx.scene.control.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
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
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import com.sun.javafx.scene.control.infrastructure.MouseEventGenerator;
import com.sun.javafx.scene.control.skin.TabPaneSkin;
import com.sun.javafx.scene.input.KeyCodeMap;
import com.sun.javafx.tk.Toolkit;

/**
 *
 * @author srikalyc
 */
public class TabPaneTest {
    private TabPane tabPane;//Empty string
    private Toolkit tk;
    private TabPane.TabPaneSelectionModel sm;
    private Tab tab1;
    private Tab tab2;
    private Tab tab3;
    private Scene scene;
    private Stage stage;
    private StackPane root;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        tabPane = new TabPane();
        tab1 = new Tab("one");
        tab2 = new Tab("two");
        tab3 = new Tab("three");
        sm = new TabPane.TabPaneSelectionModel(tabPane);
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
        ObjectProperty objPr = new SimpleObjectProperty<Side>(Side.BOTTOM);
        tabPane.sideProperty().bind(objPr);
        assertSame("side cannot be bound", tabPane.sideProperty().getValue(), Side.BOTTOM);
        objPr.setValue(Side.RIGHT);
        assertSame("side cannot be bound", tabPane.sideProperty().getValue(), Side.RIGHT);
    }

    @Test public void checkTabClosingPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<TabPane.TabClosingPolicy>(TabPane.TabClosingPolicy.UNAVAILABLE);
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

    @Test public void whenTabMinWidthIsBound_impl_cssSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMinWidthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
        DoubleProperty other = new SimpleDoubleProperty(30.0);
        tabPane.tabMinWidthProperty().bind(other);
        assertFalse(styleable.isSettable(tabPane));
    }

    @Test public void whenTabMinWidthIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMinWidthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
    }

    @Test public void canSpecifyTabMinWidthViaCSS() {
        ((StyleableProperty)tabPane.tabMinWidthProperty()).applyStyle(null, 34.0);
        assertEquals(34.0, tabPane.getTabMinWidth(), 0.0);
    }

    @Test public void whenTabMaxWidthIsBound_impl_cssSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMaxWidthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
        DoubleProperty other = new SimpleDoubleProperty(30.0);
        tabPane.tabMaxWidthProperty().bind(other);
        assertFalse(styleable.isSettable(tabPane));
    }

    @Test public void whenTabMaxWidthIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMaxWidthProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
    }

    @Test public void canSpecifyTabMaxWidthViaCSS() {
        ((StyleableProperty)tabPane.tabMaxWidthProperty()).applyStyle(null, 34.0);
        assertEquals(34.0, tabPane.getTabMaxWidth(), 0.0);
    }

    @Test public void whenTabMinHeightIsBound_impl_cssSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMinHeightProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
        DoubleProperty other = new SimpleDoubleProperty(30.0);
        tabPane.tabMinHeightProperty().bind(other);
        assertFalse(styleable.isSettable(tabPane));
    }

    @Test public void whenTabMinHeightIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMinHeightProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
    }

    @Test public void canSpecifyTabMinHeightViaCSS() {
        ((StyleableProperty)tabPane.tabMinHeightProperty()).applyStyle(null, 34.0);
        assertEquals(34.0, tabPane.getTabMinHeight(), 0.0);
    }

    @Test public void whenTabMaxHeightIsBound_impl_cssSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)tabPane.tabMaxHeightProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(tabPane));
        DoubleProperty other = new SimpleDoubleProperty(30.0);
        tabPane.tabMaxHeightProperty().bind(other);
        assertFalse(styleable.isSettable(tabPane));
    }

    @Test public void whenTabMaxHeightIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
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
        assertEquals(tabPane.getSelectionModel().getItemCount(), 2.0, 0.0);
    }

    @Test public void addTabsCheckItemInSelectionModel() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        assertSame(tabPane.getSelectionModel().getModelItem(0), tab1);
        assertSame(tabPane.getSelectionModel().getModelItem(1), tab2);
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

        tk.firePulse();
        assertTrue(tabPane.isFocused());
        
        tab2.setDisable(true);
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab1, tabPane.getSelectionModel().getSelectedItem());
        assertTrue(tab2.isDisable());

        KeyEventFirer keyboard = new KeyEventFirer(tabPane);
        keyboard.doRightArrowPress();
        tk.firePulse();

        assertEquals(2, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab3, tabPane.getSelectionModel().getSelectedItem());

        keyboard.doLeftArrowPress();
        tk.firePulse();

        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab1, tabPane.getSelectionModel().getSelectedItem());
    }
    
    @Ignore
    @Test public void mousePressSelectsATab_RT20476() {        
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        tabPane.getTabs().add(tab3);        
        
        tab1.setContent(new Button("TAB1"));
        tab2.setContent(new Button("TAB2"));
        tab3.setContent(new Button("TAB3"));
        
        root.getChildren().add(tabPane);
        show();

        root.impl_reapplyCSS();
        root.resize(300, 300);
        root.layout();
        
        tk.firePulse();        
        assertTrue(tabPane.isFocused());
        
        double xval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinX();
        double yval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinY();
   
        scene.impl_processMouseEvent(
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+75, yval+20));
        tk.firePulse();        
        
        assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());        
    }
    
    private int counter = 0;
    @Ignore
    @Test public void setOnSelectionChangedFiresTwice_RT21089() {
        tabPane.getTabs().add(tab1);
        tabPane.getTabs().add(tab2);
        
        tab1.setContent(new Button("TAB1"));
        tab2.setContent(new Button("TAB2"));
        
        root.getChildren().add(tabPane);
        show();

        root.impl_reapplyCSS();
        root.resize(300, 300);
        root.layout();
        
        tk.firePulse();        
        assertTrue(tabPane.isFocused());

        tab2.setOnSelectionChanged(new EventHandler<Event>() {
            @Override public void handle(Event event) {
                assertEquals(0, counter++);
            }
        });

        double xval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinX();
        double yval = (tabPane.localToScene(tabPane.getLayoutBounds())).getMinY();
   
        scene.impl_processMouseEvent(
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+75, yval+20));
        tk.firePulse();
        
        assertEquals(tab2, tabPane.getSelectionModel().getSelectedItem());
                
        scene.impl_processMouseEvent(
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

        tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            public void changed(ObservableValue<? extends Tab> ov, Tab t, Tab t1) {
                assertEquals(t.getText(), "one");
                assertEquals(t1.getText(), "two");
            }            
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
        final ChangeListener<Boolean> focusListener = new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldVal, Boolean newVal) {
                button1Focused = true;
            }
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
        Platform.runLater(new Runnable() {
            @Override public void run() {
                tabPane.requestFocus();
                Event.fireEvent(tabPane, tabEvent);

            }
        });

        assertTrue(button1Focused);

    }    
}
