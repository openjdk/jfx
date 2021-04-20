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

import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import javafx.scene.shape.Rectangle;


import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 *
 * @author lubermud
 */
public class MenuItemTest {
    public static EventType<Event> eventType;
    private MenuItem menuItem;

    @BeforeClass public static void classSetup() {
        eventType = new EventType<Event>(Event.ANY, "TEST_EVENT");
    }

    @Before public void setup() {
        menuItem = new MenuItem();
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void defaultConstructorShouldHaveNoGraphic() {
        assertNull(menuItem.getGraphic());
    }

    @Test public void defaultConstructorShouldHaveNullString() {
        assertNull(menuItem.getText());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic1() {
        MenuItem mi2 = new MenuItem(null);
        assertNull(mi2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic2() {
        MenuItem mi2 = new MenuItem("");
        assertNull(mi2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic3() {
        MenuItem mi2 = new MenuItem("Hello");
        assertNull(mi2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString1() {
        MenuItem mi2 = new MenuItem(null);
        assertNull(mi2.getText());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString2() {
        MenuItem mi2 = new MenuItem("");
        assertEquals("", mi2.getText());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString3() {
        MenuItem mi2 = new MenuItem("Hello");
        assertEquals("Hello", mi2.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedGraphic1() {
        MenuItem mi2 = new MenuItem(null, null);
        assertNull(mi2.getGraphic());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedGraphic2() {
        Rectangle rect = new Rectangle();
        MenuItem mi2 = new MenuItem("Hello", rect);
        assertSame(rect, mi2.getGraphic());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedString1() {
        MenuItem mi2 = new MenuItem(null, null);
        assertNull(mi2.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedString2() {
        Rectangle rect = new Rectangle();
        MenuItem mi2 = new MenuItem("Hello", rect);
        assertEquals("Hello", mi2.getText());
    }

    @Test public void getUninitializedId() {
        assertNull(menuItem.getId());
    }

    @Test public void setNullId() {
        menuItem.setId(null);
        assertNull(menuItem.getId());
    }

    @Test public void setSpecifiedId1() {
        menuItem.setId("");
        assertEquals("", menuItem.getId());
    }

    @Test public void setSpecifiedId2() {
        menuItem.setId("Hello");
        assertEquals("Hello", menuItem.getId());
    }

    @Test public void getUninitializedStyle() {
        assertNull(menuItem.getStyle());
    }

    @Test public void setNullStyle() {
        menuItem.setStyle(null);
        assertNull(menuItem.getStyle());
    }

    @Test public void setSpecifiedStyle1() {
        menuItem.setStyle("");
        assertEquals("", menuItem.getStyle());
    }

    @Test public void setSpecifiedStyle2() {
        menuItem.setStyle("Hello");
        assertEquals("Hello", menuItem.getStyle());
    }

    @Test public void getUnspecifiedParentMenu() {
        assertNull(menuItem.getParentMenu());
    }

    @Test public void getUnspecifiedParentMenuProperty() {
        assertNotNull(menuItem.parentMenuProperty());
    }

    @Test public void getUnspecifiedParentPopup() {
        assertNull(menuItem.getParentPopup());
    }

    @Test public void getUnspecifiedParentPopupProperty() {
        assertNotNull(menuItem.parentPopupProperty());
    }

    @Test public void resetText1() {
        menuItem.setText("Hello");
        assertEquals("Hello", menuItem.getText());
    }

    @Test public void resetText2() {
        Rectangle rect = new Rectangle();
        MenuItem mi2 = new Menu("Hello", rect);

        mi2.setText("Goodbye");
        assertEquals("Goodbye", mi2.getText());
    }

    @Test public void resetText3() {
        Rectangle rect = new Rectangle();
        MenuItem mi2 = new Menu("Hello", rect);

        mi2.setText("Hello");
        assertEquals("Hello", mi2.getText());
    }

    @Test public void resetText4() {
        Rectangle rect = new Rectangle();
        MenuItem mi2 = new Menu("Hello", rect);

        mi2.setText(null);
        assertEquals(null, mi2.getText());
    }

    @Test public void getUnspecifiedTextProperty1() {
        MenuItem mi2 = new MenuItem();
        assertNotNull(mi2.textProperty());
        assertNull(mi2.getText());
    }

    @Test public void getUnspecifiedTextProperty2() {
        MenuItem mi2 = new MenuItem("");
        assertEquals("", mi2.getText());
    }

    @Test public void textCanBeBound() {
        SimpleStringProperty other = new SimpleStringProperty(menuItem, "text", "Goodbye");
        menuItem.textProperty().bind(other);
        assertEquals("Goodbye", menuItem.getText());
    }

    @Test public void resetGraphic1() {
        Rectangle rect = new Rectangle();
        menuItem.setGraphic(rect);
        assertSame(rect, menuItem.getGraphic());
    }

    @Test public void resetGraphic2() {
        Rectangle rect = new Rectangle();
        MenuItem mi2 = new Menu("Hello", rect);

        Rectangle rect2 = new Rectangle();
        mi2.setGraphic(rect2);
        assertSame(rect2, mi2.getGraphic());
    }

    @Test public void resetGraphic3() {
        Rectangle rect = new Rectangle();
        MenuItem mi2 = new Menu("Hello", rect);

        Rectangle rect2 = null;
        mi2.setGraphic(rect2);
        assertNull(mi2.getGraphic());
    }

    @Test public void getUnspecifiedGraphicProperty1() {
        MenuItem mi2 = new MenuItem();
        assertNotNull(mi2.graphicProperty());
        assertNull(mi2.getGraphic());
    }

    @Test public void getUnspecifiedGraphicProperty2() {
        MenuItem mi2 = new MenuItem("",null);
        assertNotNull(mi2.graphicProperty());
        assertNull(mi2.getGraphic());
    }

    @Test public void graphicCanBeBound() {
        Rectangle rect = new Rectangle();
        SimpleObjectProperty<Node> other = new SimpleObjectProperty<Node>(menuItem, "graphic", rect);
        menuItem.graphicProperty().bind(other);
        assertSame(rect, menuItem.getGraphic());
    }

    @Test public void onActionIsNullByDefault1() {
        assertNull(menuItem.getOnAction());
    }

    @Test public void onActionIsNullByDefault2() {
        assertNull(menuItem.onActionProperty().getValue());
    }

    @Test public void setOnActionNull() {
        MenuItem mi2 = new MenuItem();
        mi2.setOnAction(null);
        assertNull(mi2.getOnAction());
    }

    @Test public void onActionCanBeSet() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        menuItem.setOnAction(handler);
        assertEquals(handler, menuItem.getOnAction());
    }

    @Test public void onActionSetToNonDefaultValueIsReflectedInModel() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        menuItem.setOnAction(handler);
        assertEquals(handler, menuItem.onActionProperty().getValue());
    }

    @Test public void onActionCanBeCleared() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        menuItem.setOnAction(handler);
        menuItem.setOnAction(null);
        assertNull(menuItem.getOnAction());
    }

    @Test public void onActionCanBeBound() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        ObjectProperty<EventHandler<ActionEvent>> other = new SimpleObjectProperty<EventHandler<ActionEvent>>(handler);
        menuItem.onActionProperty().bind(other);
        assertEquals(handler, menuItem.getOnAction());
    }

    @Test public void onActionCalledWhenMenuItemIsFired() {
        final EventHandlerStub handler = new EventHandlerStub();
        menuItem.setOnAction(handler);
        menuItem.fire();
        assertTrue(handler.called);
    }

    @Test public void onActionCalledWhenNullWhenMenuItemIsFiredIsNoOp() {
        menuItem.fire(); // should throw no exceptions, if it does, the test fails
    }

    @Test public void onActionPropertyBeanValue() {
        assertEquals(menuItem, menuItem.onActionProperty().getBean());
    }

    @Test public void onActionPropertyNameValue() {
        assertEquals("onAction", menuItem.onActionProperty().getName());
    }

    public static final class EventHandlerStub implements EventHandler<ActionEvent> {
        boolean called = false;
        @Override public void handle(ActionEvent event) {
            called = true;
        }
    };

    @Test public void getUnspecifiedDisable() {
        assertEquals(false, menuItem.isDisable());
    }

    @Test public void setTrueDisable() {
        menuItem.setDisable(true);
        assertEquals(true, menuItem.isDisable());
    }

    @Test public void setFalseDisable() {
        menuItem.setDisable(false);
        assertEquals(false, menuItem.isDisable());
    }

    @Test public void disableNotSetButNotNull() {
        menuItem.disableProperty();
        assertNotNull(menuItem.isDisable());
    }

    @Test public void disableCanBeBound1() {
        SimpleBooleanProperty other = new SimpleBooleanProperty(menuItem, "disable", false);
        menuItem.disableProperty().bind(other);
        assertEquals(other.get(), menuItem.isDisable());
    }

    @Test public void disableCanBeBound2() {
        SimpleBooleanProperty other = new SimpleBooleanProperty(menuItem, "disable", true);
        menuItem.disableProperty().bind(other);
        assertEquals(other.get(), menuItem.isDisable());
    }

    @Test public void getUnspecifiedVisible() {
        assertEquals(true, menuItem.isVisible());
    }

    @Test public void setTrueVisible() {
        menuItem.setVisible(true);
        assertEquals(true, menuItem.isVisible());
    }

    @Test public void setFalseVisible() {
        menuItem.setVisible(false);
        assertEquals(false, menuItem.isVisible());
    }

    @Test public void visibleNotSetButNotNull() {
        menuItem.visibleProperty();
        assertNotNull(menuItem.isVisible());
    }

    @Test public void visibleCanBeBound() {
        SimpleBooleanProperty other = new SimpleBooleanProperty(menuItem, "visible", true);
        menuItem.visibleProperty().bind(other);
        assertEquals(other.get(), menuItem.isVisible());
    }

    @Test public void setSpecifiedAccelerator1() {
        Modifier[] modifierArray = {};
        KeyCombination kc = new KeyCodeCombination(KeyCode.A, modifierArray);
        menuItem.setAccelerator(kc);
        assertEquals(kc, menuItem.getAccelerator());
    }

    @Test public void setSpecifiedAccelerator2() {
        Modifier[] modifierArray = {};
        KeyCombination kc = new KeyCharacterCombination("A", modifierArray);
        menuItem.setAccelerator(kc);
        assertEquals(kc, menuItem.getAccelerator());
    }

    @Test public void getUnspecifiedAccelerator() {
        assertNull(menuItem.getAccelerator());
    }

    @Test public void setNullAccelerator() {
        menuItem.setAccelerator(null);
        assertNull(menuItem.getAccelerator());
    }

    @Test public void getUnspecifiedAcceleratorProperty() {
        assertNotNull(menuItem.acceleratorProperty());
    }

    @Test public void unsetAcceleratorButNotNull() {
        menuItem.acceleratorProperty();
        assertNotNull(menuItem.acceleratorProperty());
    }

    @Test public void acceleratorCanBeBound() {
        KeyCombination kc = new KeyCharacterCombination("A", KeyCombination.ALT_DOWN);
        SimpleObjectProperty<KeyCombination> other = new SimpleObjectProperty<KeyCombination>(menuItem, "accelerator", kc);
        menuItem.acceleratorProperty().bind(other);
        assertEquals(kc, menuItem.getAccelerator());
    }

    @Test public void getUnspecifiedMnemonicParsing() {
        assertEquals(true, menuItem.isMnemonicParsing());
    }

    @Test public void setTrueMnemonicParsing() {
        menuItem.setMnemonicParsing(true);
        assertEquals(true, menuItem.isMnemonicParsing());
    }

    @Test public void setFalseMnemonicParsing() {
        menuItem.setMnemonicParsing(false);
        assertEquals(false, menuItem.isMnemonicParsing());
    }

    @Test public void mnemonicParsingNotSetButNotNull() {
        menuItem.mnemonicParsingProperty();
        assertNotNull(menuItem.isMnemonicParsing());
    }

    @Test public void mnemonicParsingCanBeBound() {
        SimpleBooleanProperty other = new SimpleBooleanProperty(menuItem, "disable", true);
        menuItem.disableProperty().bind(other);
        assertEquals(other.get(), menuItem.isDisable());
    }

    @Test public void notNullStyleClass() {
        assertNotNull(menuItem.getStyleClass());
    }

    @Test public void greaterThanZeroStyleClass() {
        assertTrue(menuItem.getStyleClass().size() > 0);
    }

    @Test public void clearedStyleClass() {
        menuItem.getStyleClass().clear();
        assertTrue(menuItem.getStyleClass().size() == 0);
    }

    @Test public void addedEventHandler1() {
        NewEventHandlerStub handler = new NewEventHandlerStub();
        Event.fireEvent(menuItem, new Event(eventType));
        menuItem.addEventHandler(eventType, handler);

        assertFalse(handler.called);
    }

    @Test public void addedEventHandler2() {
        NewEventHandlerStub handler = new NewEventHandlerStub();
        menuItem.addEventHandler(eventType, handler);
        Event.fireEvent(menuItem, new Event(eventType));

        assertTrue(handler.called);
    }

    @Test public void addedRemovedEventHandler1() {
        NewEventHandlerStub handler = new NewEventHandlerStub();
        menuItem.removeEventHandler(eventType, handler);
        Event.fireEvent(menuItem, new Event(eventType));

        assertFalse(handler.called);
    }

    @Test public void addedRemovedEventHandler2() {
        NewEventHandlerStub handler = new NewEventHandlerStub();
        Event.fireEvent(menuItem, new Event(eventType));
        menuItem.removeEventHandler(eventType, handler);

        assertFalse(handler.called);
    }

    @Test public void addedRemovedEventHandler3() {
        NewEventHandlerStub handler = new NewEventHandlerStub();
        menuItem.addEventHandler(eventType, handler);
        menuItem.removeEventHandler(eventType, handler);
        Event.fireEvent(menuItem, new Event(eventType));

        assertFalse(handler.called);
    }

    @Test public void addedRemovedEventHandler4() {
        NewEventHandlerStub handler = new NewEventHandlerStub();
        menuItem.addEventHandler(eventType, handler);
        Event.fireEvent(menuItem, new Event(eventType));
        menuItem.removeEventHandler(eventType, handler);

        assertTrue(handler.called);
    }

    public static final class NewEventHandlerStub implements EventHandler<Event> {
        boolean called = false;
        @Override public void handle(Event event) {
            called = true;
        }
    };

    //TODO: test this -> MenuItem.buildEventDispatchChain(EventDispatchChain tail)

    @Test public void getUnspecifiedUserData() {
        assertNull(menuItem.getUserData());
    }

    @Test public void getSpecifiedUserData1() {
        Object obj = new Object();
        menuItem.setUserData(obj);
        assertEquals(obj, menuItem.getUserData());
    }

    @Test public void getSpecifiedUserData2() {
        String str = "Hello";
        menuItem.setUserData(str);
        assertEquals(str, menuItem.getUserData());
    }

    @Test public void notNullGetProperties() {
        assertNotNull(menuItem.getProperties());
    }

    @Test public void zeroSizeGetProperties() {
        assertTrue(menuItem.getProperties().size() == 0);
    }

    @Test public void addableGetProperties() {
        menuItem.getProperties().put(null, null);
        assertTrue(menuItem.getProperties().size() > 0);
    }

    private int eventCounter = 0;
    @Test public void testAcceleratorIsNotFiredWhenMenuItemRemovedFromScene() {
        MenuItem item = new MenuItem("Item 1");
        item.setOnAction(e -> eventCounter++);
        item.setAccelerator(KeyCombination.valueOf("alt+1"));

        MenuButton menuButton = new MenuButton();
        menuButton.getItems().add(item);

        StageLoader s = new StageLoader(menuButton);
        Scene scene = s.getStage().getScene();
        KeyEventFirer keyboard = new KeyEventFirer(item, scene);

        // Invoke MenuItem's action listener twice by using accelerator KeyCombination
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(1, eventCounter);

        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(2, eventCounter);

        // Remove all children from the scene
        Group root = (Group)scene.getRoot();
        root.getChildren().clear();

        // Assert that the MenuItem's action listener is not invoked
        // after MenuItem has been removed from the scene
        keyboard.doKeyPress(KeyCode.DIGIT1, KeyModifier.ALT);
        assertEquals(2, eventCounter);

        // Assert that key combination does not remain in the scene's list of accelerators
        assertFalse(scene.getAccelerators().containsKey(KeyCombination.keyCombination("alt+1")));
    }
}
