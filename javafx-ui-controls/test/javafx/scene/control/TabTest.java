/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import com.sun.javafx.event.EventDispatchChainImpl;
import static javafx.scene.control.ControlTestUtils.*;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class TabTest {
    private Tab tab;//Empty string
    private Tab tabWithStr;//
    private TabPane dummyTabPane;
    private Toolkit tk;
        EventHandler eh;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        tab = new Tab();
        tabWithStr = new Tab("text");
        dummyTabPane = new TabPane();
        eh = new EventHandler<Event>(){
            @Override
            public void handle(Event event) { }
        };    
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
        assertEquals("idProperty cannot be bound", tab.idProperty().getValue(), "one");
        objPr.setValue("another");
        assertEquals("idProperty cannot be bound", tab.idProperty().getValue(), "another");
    }
    
    @Test public void checkStylePropertyBind() {
        StringProperty objPr = new SimpleStringProperty("one");
        tab.styleProperty().bind(objPr);
        assertEquals("styleProperty cannot be bound", tab.styleProperty().getValue(), "one");
        objPr.setValue("another");
        assertEquals("styleProperty cannot be bound", tab.styleProperty().getValue(), "another");
    }
    
    @Test public void checkSelectedPropertyReadOnly() {
        assertTrue(tab.selectedProperty() instanceof ReadOnlyBooleanProperty);
    }

    @Test public void checkTabPanePropertyReadOnly() {
        assertTrue(tab.tabPaneProperty() instanceof ReadOnlyObjectProperty);
    }

    @Test public void checkTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        tab.textProperty().bind(strPr);
        assertEquals("Text cannot be bound", tab.textProperty().getValue(), "value");
        strPr.setValue("newvalue");
        assertEquals("Text cannot be bound", tab.textProperty().getValue(), "newvalue");
    }
    
    @Test public void checkGraphicPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Node>(null);
        Rectangle rect = new Rectangle(10, 20);
        tab.graphicProperty().bind(objPr);
        assertNull("Graphic cannot be bound", tab.graphicProperty().getValue());
        objPr.setValue(rect);
        assertSame("Graphic cannot be bound", tab.graphicProperty().getValue(), rect);
    }
    
    @Test public void checkContentPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Node>(null);
        Rectangle rect = new Rectangle(10, 20);
        tab.contentProperty().bind(objPr);
        assertNull("content cannot be bound", tab.contentProperty().getValue());
        objPr.setValue(rect);
        assertSame("content cannot be bound", tab.contentProperty().getValue(), rect);
    }

    @Test public void checkContextMenuPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<ContextMenu>(null);
        ContextMenu mnu = new ContextMenu();
        tab.contextMenuProperty().bind(objPr);
        assertNull("contextMenu cannot be bound", tab.contextMenuProperty().getValue());
        objPr.setValue(mnu);
        assertSame("contextMenu cannot be bound", tab.contextMenuProperty().getValue(), mnu);
    }
    
    @Test public void checkClosablePropertyBind() {
        BooleanProperty pr = new SimpleBooleanProperty(true);
        tab.closableProperty().bind(pr);
        assertTrue("closable cannot be bound", tab.closableProperty().getValue());
        pr.setValue(false);
        assertFalse("closable cannot be bound", tab.closableProperty().getValue());
    }
    
    @Test public void checkOnSelectionChangedPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<EventHandler<Event>>(null);
        tab.onSelectionChangedProperty().bind(objPr);
        assertNull("onSelectionChanged cannot be bound", tab.onSelectionChangedProperty().getValue());
        objPr.setValue(eh);
        assertSame("onSelectionChanged cannot be bound", tab.onSelectionChangedProperty().getValue(), eh);
    }
    
    @Test public void checkOnClosedPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<EventHandler<Event>>(null);
        tab.onClosedProperty().bind(objPr);
        assertNull("onSelectionChanged cannot be bound", tab.onClosedProperty().getValue());
        objPr.setValue(eh);
        assertSame("onSelectionChanged cannot be bound", tab.onClosedProperty().getValue(), eh);
    }
    
    @Test public void checkTooltipPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Tooltip>(null);
        tab.tooltipProperty().bind(objPr);
        assertNull("tooltip cannot be bound", tab.tooltipProperty().getValue());
        Tooltip tt = new Tooltip();
        objPr.setValue(tt);
        assertSame("tooltip cannot be bound", tab.tooltipProperty().getValue(), tt);
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
        tab.setSelected(true);
        assertTrue(tab.selectedProperty().getValue());
    }
    
    @Test public void setSelectedAndSeeValue() {
        tab.setSelected(true);
        assertTrue(tab.isSelected());
    }
    
    @Test public void setTabpaneAndSeeValueIsReflectedInModel() {
        tab.setTabPane(dummyTabPane);
        assertSame(tab.tabPaneProperty().getValue(), dummyTabPane);
    }
    
    @Test public void setTabpaneAndSeeValue() {
        tab.setTabPane(dummyTabPane);
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
    @Ignore("The following test is incomplete with no proper sense.")
    @Test public void checkEventDispatcherChain() {
        EventDispatchChain chain = new EventDispatchChainImpl();
        tab.buildEventDispatchChain(chain);
    }
    
    @Test public void setDisableAndSeeValue() {
        tab.setDisable(true);
        assertTrue(tab.isDisable());
    }
    

}
