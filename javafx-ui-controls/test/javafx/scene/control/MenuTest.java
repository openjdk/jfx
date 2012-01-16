/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;


/**
 *
 * @author lubermud
 */
public class MenuTest {
    private Menu menu;

    @Before public void setup() {
        menu = new Menu("Hello");
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    @Test public void oneArgConstructorShouldHaveNoGraphic1() {
        Menu menu2 = new Menu(null);
        assertNull(menu2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic2() {
        Menu menu2 = new Menu("");
        assertNull(menu2.getGraphic());
    }
    
    @Test public void oneArgConstructorShouldHaveNoGraphic3() {
        assertNull(menu.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString1() {
        Menu menu2 = new Menu(null);
        assertNull(menu2.getText());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString2() {
        Menu menu2 = new Menu("");
        assertEquals("", menu2.getText());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString3() {
        assertEquals("Hello", menu.getText());
    }
    
    @Test public void twoArgConstructorShouldHaveSpecifiedGraphic1() {
        Menu menu2 = new Menu(null, null);
        assertNull(menu2.getGraphic());
    }
    
    @Test public void twoArgConstructorShouldHaveSpecifiedGraphic2() {   
        Rectangle rect = new Rectangle();
        Menu menu2 = new Menu("Hello", rect);
        assertEquals(rect, menu2.getGraphic());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedString1() {
        Menu menu2 = new Menu(null, null);
        assertNull(menu2.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedString2() {
        Rectangle rect = new Rectangle();
        Menu menu2 = new Menu("Hello", rect);
        assertEquals("Hello", menu2.getText());
    }

    @Test public void getUnspecifiedShowing() {
        assertEquals(false, menu.isShowing());
    }

    @Test public void showingNotSetButStillFalse() {
        menu.showingProperty();
        assertFalse(menu.isShowing());
    }

    @Test public void showingSetTrue() {
        menu.show();
        assertFalse(menu.isShowing());
    }

    @Test public void showingSetFalse() {
        menu.hide();
        assertFalse(menu.isShowing());
    }

    @Test public void showingSetTrueThenFalse() {
        menu.show();
        menu.hide();
        assertFalse(menu.isShowing());
    }

    @Test public void showingSetTrueButDisabledSoStillFalse() {
        menu.setDisable(true);
        menu.show();
        assertFalse(menu.isShowing());
    }

    @Test public void showingSetTrueThenDisabledSoStillTrue() {
        menu.setDisable(true);
        menu.show();
        assertFalse(menu.isShowing());
    }

    @Test public void shownThenDisabledThenHiddenSoFalse() {
        menu.show();
        menu.setDisable(true);
        menu.hide();
        assertFalse(menu.isShowing());
    }

    @Test public void getItemsNotNull() {
        assertNotNull(menu.getItems());
    }

    @Test public void getItemsSizeZero() {
        assertEquals(0, menu.getItems().size());
    }

    @Test public void getItemsAddable() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        assertTrue(menu.getItems().size() > 0);
    }

    @Test public void getItemsClearable() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        menu.getItems().clear();
        assertEquals(0, menu.getItems().size());
    }

    @Test public void showingSetTrueWithChildren() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        menu.show();
        assertTrue(menu.isShowing());
    }

    @Test public void showingSetFalseWithChildren() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        menu.hide();
        assertFalse(menu.isShowing());
    }

    @Test public void showingSetTrueThenFalseWithChildren() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        menu.show();
        menu.hide();
        assertFalse(menu.isShowing());
    }

    @Test public void showingSetTrueButDisabledSoStillFalseWithChildren() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        menu.setDisable(true);
        menu.show();
        assertFalse(menu.isShowing());
    }

    @Test public void showingSetTrueThenDisabledSoStillTrueWithChildren() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        menu.show();
        menu.setDisable(true);
        assertTrue(menu.isShowing());
    }

    @Test public void shownThenDisabledThenHiddenSoFalseWithChildren() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        menu.show();
        menu.setDisable(true);
        menu.hide();
        assertFalse(menu.isShowing());
    }

    @Test public void shownThenClearedThenHiddenButTrueWithChildren() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        menu.show();
        menu.getItems().clear();
        menu.hide();
        assertFalse(menu.isShowing());
    }

    @Test public void shownHiddenThenClearedThenShownButFalseWithChildren() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        menu.show();
        menu.hide();
        menu.getItems().clear();
        menu.show();
        assertFalse(menu.isShowing());
    }

    @Test public void setShowingTrueWithMenuItemChildren() {
        MenuItem mi2 = new MenuItem("Child");
        menu.getItems().add(mi2);
        menu.show();
        assertTrue(menu.isShowing());
    }

    @Test public void setShowingTrueThenFalseWithMenuItemChildren() {
        MenuItem mi2 = new MenuItem("Child");
        menu.getItems().add(mi2);
        menu.show();
        menu.hide();
        assertFalse(menu.isShowing());
    }

    @Test public void hidingAlsoHidesMenuChildren() {
        Menu mi2 = new Menu("Child1");
        MenuItem mi3 = new MenuItem("Child2");
        
        mi2.getItems().add(mi3);
        menu.getItems().add(mi2);

        mi2.show();
        menu.show();
        menu.hide();
        assertFalse(mi2.isShowing());
    }

    @Test public void hidingDoesNotHideMenuChildrenWithNoSubChildren() {
        Menu mi2 = new Menu("Child1");
        MenuItem mi3 = new MenuItem("Child2");
        mi2.getItems().add(mi3);
        menu.getItems().add(mi2);

        mi2.show();
        mi2.getItems().clear();
        menu.show();
        menu.hide();
        assertFalse(mi2.isShowing());
    }

    @Test public void clearingGetItemsShouldHideMenu() {
        MenuItem mi2 = new Menu("Child");
        menu.getItems().add(mi2);
        menu.show();

        menu.getItems().clear();
        assertFalse(menu.isShowing());
    }

    @Test public void getOnShowingProperty() {
        assertNotNull(menu.onShowingProperty());
    }

    @Test public void getOnShowing() {
        assertNull(menu.getOnShowing());
    }

    @Test public void setNullOnShowing() {
        menu.setOnShowing(null);
        assertNull(menu.getOnShowing());
    }

    @Test public void setOnShowing() {
        EventHandlerStub handler = new EventHandlerStub();
        menu.setOnShowing(handler);
        assertEquals(handler, menu.getOnShowing());
    }

    @Test public void setOnShowingAndShow() {
        Menu mi2 = new Menu("Child");
        menu.getItems().add(mi2);

        EventHandlerStub handler = new EventHandlerStub();
        menu.setOnShowing(handler);
        menu.show();
        assertTrue(handler.called);
    }

    @Test public void onShowingPropertyBeanValue() {
        assertEquals(menu, menu.onShowingProperty().getBean());
    }

    @Test public void onShowingPropertyNameValue() {
        assertEquals("onShowing", menu.onShowingProperty().getName());
    }

    @Test public void getOnShownProperty() {
        assertNotNull(menu.onShownProperty());
    }

    @Test public void getOnShown() {
        assertNull(menu.getOnShown());
    }

    @Test public void setNullOnShown() {
        menu.setOnShown(null);
        assertNull(menu.getOnShown());
    }

    @Test public void setOnShown() {
        EventHandlerStub handler = new EventHandlerStub();
        menu.setOnShown(handler);
        assertEquals(handler, menu.getOnShown());
    }

    @Test public void setOnShownAndShow() {
        Menu mi2 = new Menu("Child");
        menu.getItems().add(mi2);

        EventHandlerStub handler = new EventHandlerStub();
        menu.setOnShown(handler);
        menu.show();
        assertTrue(handler.called);
    }

    @Test public void onShownPropertyBeanValue() {
        assertEquals(menu, menu.onShownProperty().getBean());
    }

    @Test public void onShownPropertyNameValue() {
        assertEquals("onShown", menu.onShownProperty().getName());
    }

    static boolean showingOccured1 = false;
    static boolean shownOccured1 = false;
    @Test public void showingCalledBeforeShown1() {
        Menu mi2 = new Menu("Child");
        menu.getItems().add(mi2);

        menu.setOnShowing(new EventHandler<Event>(){
            @Override public void handle(Event event) {
                showingOccured1 = true;
            }
        });
        menu.setOnShown(new EventHandler<Event>(){
            @Override public void handle(Event event) {
                if (showingOccured1) {
                    shownOccured1 = true;
                }
            }
        });

        menu.show();
        assertTrue(shownOccured1);
    }

    static boolean showingOccured2 = false;
    static boolean shownOccured2 = false;
    @Test public void showingCalledBeforeShown2() {
        Menu mi2 = new Menu("Child");
        menu.getItems().add(mi2);

        menu.setOnShowing(new EventHandler<Event>(){
            @Override public void handle(Event event) {
                if (shownOccured2) {
                    showingOccured2 = true;
                }
            }
        });
        menu.setOnShown(new EventHandler<Event>(){
            @Override public void handle(Event event) {
                shownOccured2 = true;
            }
        });

        menu.show();
        assertFalse(showingOccured2);
    }

    @Test public void getOnHidingProperty() {
        assertNotNull(menu.onHidingProperty());
    }

    @Test public void getOnHiding() {
        assertNull(menu.getOnHiding());
    }

    @Test public void setNullOnHiding() {
        menu.setOnHiding(null);
        assertNull(menu.getOnHiding());
    }

    @Test public void setOnHiding() {
        EventHandlerStub handler = new EventHandlerStub();
        menu.setOnHiding(handler);
        assertEquals(handler, menu.getOnHiding());
    }

    @Test public void setOnHidingAndHide1() {
        Menu mi2 = new Menu("Child");
        menu.getItems().add(mi2);

        EventHandlerStub handler = new EventHandlerStub();
        menu.setOnHiding(handler);
        menu.hide();
        assertFalse(handler.called);
    }

    @Test public void setOnHidingAndHide2() {
        Menu mi2 = new Menu("Child");
        menu.getItems().add(mi2);

        EventHandlerStub handler = new EventHandlerStub();
        menu.setOnHiding(handler);
        menu.show();
        menu.hide();
        assertTrue(handler.called);
    }

    @Test public void onHidingPropertyBeanValue() {
        assertEquals(menu, menu.onHidingProperty().getBean());
    }

    @Test public void onHidingPropertyNameValue() {
        assertEquals("onHiding", menu.onHidingProperty().getName());
    }

    @Test public void getOnHiddenProperty() {
        assertNotNull(menu.onHiddenProperty());
    }

    @Test public void getOnHidden() {
        assertNull(menu.getOnHidden());
    }

    @Test public void setNullOnHidden() {
        menu.setOnHidden(null);
        assertNull(menu.getOnHidden());
    }

    @Test public void setOnHidden() {
        EventHandlerStub handler = new EventHandlerStub();
        menu.setOnHidden(handler);
        assertEquals(handler, menu.getOnHidden());
    }

    @Test public void setOnHiddenAndHide1() {
        Menu mi2 = new Menu("Child");
        menu.getItems().add(mi2);

        EventHandlerStub handler = new EventHandlerStub();
        menu.setOnHidden(handler);
        menu.hide();
        assertFalse(handler.called);
    }

    @Test public void setOnHiddenAndHide2() {
        Menu mi2 = new Menu("Child");
        menu.getItems().add(mi2);

        EventHandlerStub handler = new EventHandlerStub();
        menu.setOnHidden(handler);
        menu.show();
        menu.hide();
        assertTrue(handler.called);
    }

    @Test public void onHiddenPropertyBeanValue() {
        assertEquals(menu, menu.onHiddenProperty().getBean());
    }

    @Test public void onHiddenPropertyNameValue() {
        assertEquals("onHidden", menu.onHiddenProperty().getName());
    }

    static boolean hidingOccured1 = false;
    static boolean hiddenOccured1 = false;
    @Test public void hidingCalledBeforeHidden1() {
        Menu mi2 = new Menu("Child");
        menu.getItems().add(mi2);

        menu.setOnHiding(new EventHandler<Event>(){
            @Override public void handle(Event event) {
                hidingOccured1 = true;
            }
        });
        menu.setOnHidden(new EventHandler<Event>(){
            @Override public void handle(Event event) {
                if (hidingOccured1) {
                    hiddenOccured1 = true;
                }
            }
        });

        menu.show();
        menu.hide();
        assertTrue(hiddenOccured1);
    }

    static boolean hidingOccured2 = false;
    static boolean hiddenOccured2 = false;
    @Test public void hidingCalledBeforeHidden2() {
        Menu mi2 = new Menu("Child");
        menu.getItems().add(mi2);

        menu.setOnHiding(new EventHandler<Event>(){
            @Override public void handle(Event event) {
                if (hiddenOccured2) {
                    hidingOccured2 = true;
                }
            }
        });
        menu.setOnHidden(new EventHandler<Event>(){
            @Override public void handle(Event event) {
                hiddenOccured2 = true;
            }
        });

        menu.show();
        menu.hide();
        assertFalse(hidingOccured2);
    }

    @Test public void addedEventHandler1() {
        EventType<Event> et1 = new EventType<Event>(Event.ANY, "ON_EVENT");
        EventHandlerStub handler = new EventHandlerStub();
        Event.fireEvent(menu, new Event(et1));
        menu.addEventHandler(et1, handler);

        assertFalse(handler.called);
    }

    @Test public void addedEventHandler2() {
        EventType<Event> et1 = new EventType<Event>(Event.ANY, "ON_EVENT");
        EventHandlerStub handler = new EventHandlerStub();
        menu.addEventHandler(et1, handler);
        Event.fireEvent(menu, new Event(et1));

        assertTrue(handler.called);
    }

    @Test public void addedRemovedEventHandler1() {
        EventType<Event> et1 = new EventType<Event>(Event.ANY, "ON_EVENT");
        EventHandlerStub handler = new EventHandlerStub();
        menu.removeEventHandler(et1, handler);
        Event.fireEvent(menu, new Event(et1));

        assertFalse(handler.called);
    }

    @Test public void addedRemovedEventHandler2() {
        EventType<Event> et1 = new EventType<Event>(Event.ANY, "ON_EVENT");
        EventHandlerStub handler = new EventHandlerStub();
        Event.fireEvent(menu, new Event(et1));
        menu.removeEventHandler(et1, handler);

        assertFalse(handler.called);
    }

    @Test public void addedRemovedEventHandler3() {
        EventType<Event> et1 = new EventType<Event>(Event.ANY, "ON_EVENT");
        EventHandlerStub handler = new EventHandlerStub();
        menu.addEventHandler(et1, handler);
        menu.removeEventHandler(et1, handler);
        Event.fireEvent(menu, new Event(et1));

        assertFalse(handler.called);
    }

    @Test public void addedRemovedEventHandler4() {
        EventType<Event> et1 = new EventType<Event>(Event.ANY, "ON_EVENT");
        EventHandlerStub handler = new EventHandlerStub();
        menu.addEventHandler(et1, handler);
        Event.fireEvent(menu, new Event(et1));
        menu.removeEventHandler(et1, handler);

        assertTrue(handler.called);
    }

    public static final class EventHandlerStub implements EventHandler<Event> {
        boolean called = false;
        @Override public void handle(Event event) {
            called = true;
        }
    };


    //TODO: test this -> Menu.buildEventDispatchChain(EventDispatchChain tail)
    
}
