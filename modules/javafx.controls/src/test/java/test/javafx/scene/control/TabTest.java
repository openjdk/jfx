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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabShim;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.pgstub.StubToolkit;

/**
 *
 * @author srikalyc
 */
public class TabTest {
    private TabShim tab;
    private TabShim tabWithStr;
    private TabPane dummyTabPane;
    EventHandler eh;

    @BeforeEach
    public void setup() {
        assertTrue(Toolkit.getToolkit() instanceof StubToolkit);  // Ensure StubToolkit is loaded

        tab = new TabShim();
        tabWithStr = new TabShim("text");
        dummyTabPane = new TabPane();
        eh = event -> { };
    }



    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultConstructorShouldSetStyleClassTo_tab() {
        assertStyleClassContains(tab, "tab");
    }

    @Test public void oneArgConstructorShouldSetStyleClassTo_tab() {
        assertStyleClassContains(tabWithStr, "tab");
    }

    @Test public void defaultConstructorText() {
        assertNull(tab.getText());
    }

    @Test public void oneConstructorText() {
        assertEquals(tabWithStr.getText(), "text");
    }

    @Test public void defaultId() {
        assertNull(tab.getId());
    }

    @Test public void defaultStyle() {
        assertNull(tab.getStyle());
    }

    @Test public void defaultSelected() {
        assertFalse(tab.isSelected());
    }

    @Test public void defaultTab() {
        assertNull(tab.getTabPane());
    }

    @Test public void checkDefaultGraphic() {
        assertNull(tab.getGraphic());
        assertNull(tabWithStr.getGraphic());
    }

    @Test public void checkDefaultContent() {
        assertNull(tab.getContent());
        assertNull(tabWithStr.getContent());
    }

    @Test public void checkDefaultContextMenu() {
        assertNull(tab.getContextMenu());
        assertNull(tabWithStr.getContextMenu());
    }

    @Test public void defaultClosable() {
        assertTrue(tab.isClosable());
    }

    @Test public void checkDefaultOnSelectetionChanged() {
        assertNull(tab.getOnSelectionChanged());
        assertNull(tabWithStr.getOnSelectionChanged());
    }

    @Test public void checkDefaultOnClosed() {
        assertNull(tab.getOnClosed());
        assertNull(tabWithStr.getOnClosed());
    }

    @Test public void checkDefaultTooltip() {
        assertNull(tab.getTooltip());
        assertNull(tabWithStr.getTooltip());
    }



    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/


    @Test public void checkIdPropertyBind() {
        StringProperty objPr = new SimpleStringProperty("one");
        tab.idProperty().bind(objPr);
        assertEquals(tab.idProperty().getValue(), "one", "idProperty cannot be bound");
        objPr.setValue("another");
        assertEquals(tab.idProperty().getValue(), "another", "idProperty cannot be bound");
    }

    @Test public void checkStylePropertyBind() {
        StringProperty objPr = new SimpleStringProperty("one");
        tab.styleProperty().bind(objPr);
        assertEquals(tab.styleProperty().getValue(), "one", "styleProperty cannot be bound");
        objPr.setValue("another");
        assertEquals(tab.styleProperty().getValue(), "another", "styleProperty cannot be bound");
    }

    @Test public void checkTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        tab.textProperty().bind(strPr);
        assertEquals(tab.textProperty().getValue(), "value", "Text cannot be bound");
        strPr.setValue("newvalue");
        assertEquals(tab.textProperty().getValue(), "newvalue", "Text cannot be bound");
    }

    @Test public void checkGraphicPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Node>(null);
        Rectangle rect = new Rectangle(10, 20);
        tab.graphicProperty().bind(objPr);
        assertNull(tab.graphicProperty().getValue(), "Graphic cannot be bound");
        objPr.setValue(rect);
        assertSame(tab.graphicProperty().getValue(), rect, "Graphic cannot be bound");
    }

    @Test public void checkContentPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Node>(null);
        Rectangle rect = new Rectangle(10, 20);
        tab.contentProperty().bind(objPr);
        assertNull(tab.contentProperty().getValue(), "content cannot be bound");
        objPr.setValue(rect);
        assertSame(tab.contentProperty().getValue(), rect, "content cannot be bound");
    }

    @Test public void checkContextMenuPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<ContextMenu>(null);
        ContextMenu mnu = new ContextMenu();
        tab.contextMenuProperty().bind(objPr);
        assertNull(tab.contextMenuProperty().getValue(), "contextMenu cannot be bound");
        objPr.setValue(mnu);
        assertSame(tab.contextMenuProperty().getValue(), mnu, "contextMenu cannot be bound");
    }

    @Test public void checkClosablePropertyBind() {
        BooleanProperty pr = new SimpleBooleanProperty(true);
        tab.closableProperty().bind(pr);
        assertTrue(tab.closableProperty().getValue(), "closable cannot be bound");
        pr.setValue(false);
        assertFalse(tab.closableProperty().getValue(), "closable cannot be bound");
    }

    @Test public void checkOnSelectionChangedPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<EventHandler<Event>>(null);
        tab.onSelectionChangedProperty().bind(objPr);
        assertNull(tab.onSelectionChangedProperty().getValue(), "onSelectionChanged cannot be bound");
        objPr.setValue(eh);
        assertSame(tab.onSelectionChangedProperty().getValue(), eh, "onSelectionChanged cannot be bound");
    }

    @Test public void checkOnClosedPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<EventHandler<Event>>(null);
        tab.onClosedProperty().bind(objPr);
        assertNull(tab.onClosedProperty().getValue(), "onSelectionChanged cannot be bound");
        objPr.setValue(eh);
        assertSame(tab.onClosedProperty().getValue(), eh, "onSelectionChanged cannot be bound");
    }

    @Test public void checkTooltipPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Tooltip>(null);
        tab.tooltipProperty().bind(objPr);
        assertNull(tab.tooltipProperty().getValue(), "tooltip cannot be bound");
        Tooltip tt = new Tooltip();
        objPr.setValue(tt);
        assertSame(tab.tooltipProperty().getValue(), tt, "tooltip cannot be bound");
    }

    @Test public void textPropertyHasBeanReference() {
        assertSame(tab, tab.textProperty().getBean());
    }

    @Test public void textPropertyHasName() {
        assertEquals("text", tab.textProperty().getName());
    }


    @Test public void graphicPropertyHasBeanReference() {
        assertSame(tab, tab.graphicProperty().getBean());
    }

    @Test public void graphicPropertyHasName() {
        assertEquals("graphic", tab.graphicProperty().getName());
    }

    @Test public void contentPropertyHasBeanReference() {
        assertSame(tab, tab.contentProperty().getBean());
    }

    @Test public void contentPropertyHasName() {
        assertEquals("content", tab.contentProperty().getName());
    }

    @Test public void contextMenuPropertyHasBeanReference() {
        assertSame(tab, tab.contextMenuProperty().getBean());
    }

    @Test public void contextMenuPropertyHasName() {
        assertEquals("contextMenu", tab.contextMenuProperty().getName());
    }

    @Test public void closablePropertyHasBeanReference() {
        assertSame(tab, tab.closableProperty().getBean());
    }

    @Test public void closablePropertyHasName() {
        assertEquals("closable", tab.closableProperty().getName());
    }

    @Test public void onSelectionChangedPropertyHasBeanReference() {
        assertSame(tab, tab.onSelectionChangedProperty().getBean());
    }

    @Test public void onSelectionChangedPropertyHasName() {
        assertEquals("onSelectionChanged", tab.onSelectionChangedProperty().getName());
    }

    @Test public void onClosedPropertyHasBeanReference() {
        assertSame(tab, tab.onClosedProperty().getBean());
    }

    @Test public void onClosedPropertyHasName() {
        assertEquals("onClosed",  tab.onClosedProperty().getName());
    }

    @Test public void tooltipPropertyHasBeanReference() {
        assertSame(tab, tab.tooltipProperty().getBean());
    }

    @Test public void tooltipPropertyHasName() {
        assertEquals("tooltip",  tab.tooltipProperty().getName());
    }



    /*********************************************************************
     * Miscellaneous Tests                                               *
     ********************************************************************/
    @Test public void setIdAndSeeValueIsReflectedInModel() {
        tab.setId("one");
        assertEquals(tab.idProperty().getValue(), "one");
    }

    @Test public void setIdAndSeeValue() {
        tab.setId("one");
        assertEquals(tab.getId(), "one");
    }

    @Test public void setStyleAndSeeValueIsReflectedInModel() {
        tab.setStyle("one");
        assertEquals(tab.styleProperty().getValue(), "one");
    }

    @Test public void setStyleAndSeeValue() {
        tab.setStyle("one");
        assertEquals(tab.getStyle(), "one");
    }

    @Test public void setSelectedAndSeeValueIsReflectedInModel() {
        tab.shim_setSelected(true);
        assertTrue(tab.selectedProperty().getValue());
    }

    @Test public void setSelectedAndSeeValue() {
        tab.shim_setSelected(true);
        assertTrue(tab.isSelected());
    }

    @Test public void setTabpaneAndSeeValueIsReflectedInModel() {
        tab.shim_setTabPane(dummyTabPane);
        assertSame(tab.tabPaneProperty().getValue(), dummyTabPane);
    }

    @Test public void setTabpaneAndSeeValue() {
        tab.shim_setTabPane(dummyTabPane);
        assertSame(tab.getTabPane(), dummyTabPane);
    }

    @Test public void setTextAndSeeValueIsReflectedInModel() {
        tab.setText("tmp");
        assertEquals(tab.textProperty().getValue(), "tmp");
    }

    @Test public void setTextAndSeeValue() {
        tab.setText("tmp");
        assertEquals(tab.getText(), "tmp");
    }

    @Test public void setGraphicAndSeeValueIsReflectedInModel() {
        Rectangle rect = new Rectangle();
        tab.setGraphic(rect);
        assertEquals(tab.graphicProperty().getValue(), rect);
    }

    @Test public void setGraphicAndSeeValue() {
        Rectangle rect = new Rectangle();
        tab.setGraphic(rect);
        assertEquals(tab.getGraphic(), rect);
    }

    @Test public void setContentAndSeeValueIsReflectedInModel() {
        Rectangle rect = new Rectangle();
        tab.setContent(rect);
        assertEquals(tab.contentProperty().getValue(), rect);
    }

    @Test public void setContentAndSeeValue() {
        Rectangle rect = new Rectangle();
        tab.setContent(rect);
        assertEquals(tab.getContent(), rect);
    }

    @Test public void setContextMenuAndSeeValueIsReflectedInModel() {
        ContextMenu mnu = new ContextMenu();
        tab.setContextMenu(mnu);
        assertSame(tab.contextMenuProperty().getValue(), mnu);
    }

    @Test public void setContextMenuAndSeeValue() {
        ContextMenu mnu = new ContextMenu();
        tab.setContextMenu(mnu);
        assertSame(tab.getContextMenu(), mnu);
    }

    @Test public void setClosableAndSeeValueIsReflectedInModel() {
        tab.setClosable(true);
        assertTrue(tab.closableProperty().getValue());
    }

    @Test public void setClosableAndSeeValue() {
        tab.setClosable(true);
        assertTrue(tab.isClosable());
    }

    @Test public void setOnSelectionChangedAndSeeValueIsReflectedInModel() {
        tab.setOnSelectionChanged(eh);
        assertSame(tab.onSelectionChangedProperty().getValue(), eh);
    }

    @Test public void setOnSelectionChangedAndSeeValue() {
        tab.setOnSelectionChanged(eh);
        assertSame(tab.getOnSelectionChanged(), eh);
    }

    @Test public void setOnClosedAndSeeValueIsReflectedInModel() {
        tab.setOnClosed(eh);
        assertSame(tab.onClosedProperty().getValue(), eh);
    }

    @Test public void setOnClosedAndSeeValue() {
        tab.setOnClosed(eh);
        assertSame(tab.getOnClosed(), eh);
    }

    @Test public void setTooltipAndSeeValueIsReflectedInModel() {
        Tooltip tt = new Tooltip();
        tab.setTooltip(tt);
        assertSame(tab.tooltipProperty().getValue(), tt);
    }

    @Test public void setTooltipAndSeeValue() {
        Tooltip tt = new Tooltip();
        tab.setTooltip(tt);
        assertSame(tab.getTooltip(), tt);
    }

    @Test public void setDisableAndSeeValue() {
        tab.setDisable(true);
        assertTrue(tab.isDisable());
    }

    @Test public void setDisableAndSeeDisabledValue() {
        tab.setDisable(true);
        assertTrue(tab.isDisabled());
    }

    @Test public void setDisableOnTabPaneAndSeeValue() {
        dummyTabPane.getTabs().add(tab);
        assertFalse(tab.isDisable());
        assertFalse(tab.isDisabled());

        dummyTabPane.setDisable(true);
        assertFalse(tab.isDisable());
        assertTrue(tab.isDisabled());

        dummyTabPane.setDisable(false);
        assertFalse(tab.isDisable());
        assertFalse(tab.isDisabled());
    }

    @Test public void setDisableOnTabPaneContentAndSeeValue() {
        VBox vBox = new VBox();
        dummyTabPane.getTabs().add(tab);
        tab.setDisable(true);
        tab.setContent(vBox);
        assertTrue(vBox.isDisable());
        assertTrue(vBox.isDisabled());
        tab.setContent(null);
    }

    @Test public void testAddAndRemoveEventHandler() {
        var handler = new TestHandler();
        tab.addEventHandler(ActionEvent.ACTION, handler);
        Event.fireEvent(tab, new ActionEvent());
        assertEquals(1, handler.handled);

        tab.removeEventHandler(ActionEvent.ACTION, handler);
        Event.fireEvent(tab, new ActionEvent());
        assertEquals(1, handler.handled);
    }

    @Test public void testAddAndRemoveEventFilter() {
        var handler = new TestHandler();
        tab.addEventFilter(ActionEvent.ACTION, handler);
        Event.fireEvent(tab, new ActionEvent());
        assertEquals(1, handler.handled);

        tab.removeEventFilter(ActionEvent.ACTION, handler);
        Event.fireEvent(tab, new ActionEvent());
        assertEquals(1, handler.handled);
    }

    private static class TestHandler implements EventHandler<ActionEvent> {
        int handled;

        @Override
        public void handle(ActionEvent event) {
            handled++;
        }
    }
}
