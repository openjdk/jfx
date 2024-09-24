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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.MenuButtonSkin;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventGenerator;

/**
 *
 * @author lubermud
 */
public class MenuButtonTest {
    private MenuButton menuButton;

    @BeforeEach
    public void setup() {
        menuButton = new MenuButton();
    }

    @Test public void defaultConstructorShouldHaveNoGraphic() {
        assertNull(menuButton.getGraphic());
    }

    @Test public void defaultConstructorShouldHaveNullString() {
        assertEquals("", menuButton.getText());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic1() {
        MenuButton mb2 = new MenuButton(null);
        assertNull(mb2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic2() {
        MenuButton mb2 = new MenuButton("");
        assertNull(mb2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveNoGraphic3() {
        MenuButton mb2 = new MenuButton("Hello");
        assertNull(mb2.getGraphic());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString1() {
        MenuButton mb2 = new MenuButton(null);
        assertEquals("", mb2.getText());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString2() {
        MenuButton mb2 = new MenuButton("");
        assertEquals("", mb2.getText());
    }

    @Test public void oneArgConstructorShouldHaveSpecifiedString3() {
        MenuButton mb2 = new MenuButton("Hello");
        assertEquals("Hello", mb2.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedGraphic1() {
        MenuButton mb2 = new MenuButton(null, null);
        assertNull(mb2.getGraphic());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedGraphic2() {
        Rectangle rect = new Rectangle();
        MenuButton mb2 = new MenuButton("Hello", rect);
        assertSame(rect, mb2.getGraphic());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedString1() {
        MenuButton mb2 = new MenuButton(null, null);
        assertEquals("", mb2.getText());
    }

    @Test public void twoArgConstructorShouldHaveSpecifiedString2() {
        Rectangle rect = new Rectangle();
        MenuButton mb2 = new MenuButton("Hello", rect);
        assertEquals("Hello", mb2.getText());
    }

    @Test public void getItemsDefaultNotNull() {
        assertNotNull(menuButton.getItems());
    }

    @Test public void getItemsDefaultSizeZero() {
        assertEquals(0, menuButton.getItems().size());
    }

    @Test public void getItemsAddable() {
        menuButton.getItems().add(new MenuItem());
        assertTrue(menuButton.getItems().size() > 0);
    }

    @Test public void getItemsClearable() {
        menuButton.getItems().add(new MenuItem());
        menuButton.getItems().clear();
        assertEquals(0, menuButton.getItems().size());
    }

    @Test public void defaultIsShowingFalse() {
        assertFalse(menuButton.isShowing());
    }

    @Test public void showIsShowingTrue() {
        menuButton.show();
        assertTrue(menuButton.isShowing());
    }

    @Test public void hideIsShowingFalse1() {
        menuButton.hide();
        assertFalse(menuButton.isShowing());
    }

    @Test public void hideIsShowingFalse2() {
        menuButton.show();
        menuButton.hide();
        assertFalse(menuButton.isShowing());
    }

    @Test public void getUnspecifiedShowingProperty1() {
        assertNotNull(menuButton.showingProperty());
    }

    @Test public void getUnspecifiedShowingProperty2() {
        MenuButton mb2 = new MenuButton("", null);
        assertNotNull(mb2.showingProperty());
    }

    @Test public void unsetShowingButNotNull() {
        menuButton.showingProperty();
        assertNotNull(menuButton.isShowing());
    }

    @Test public void menuButtonIsFiredIsNoOp() {
        menuButton.fire(); // should throw no exceptions, if it does, the test fails
    }

    @Test public void defaultPopupSide() {
        assertEquals(Side.BOTTOM, menuButton.getPopupSide());
        assertEquals(Side.BOTTOM, menuButton.popupSideProperty().get());
    }

    @Test public void setNullPopupSide() {
        menuButton.setPopupSide(null);
        assertNull(menuButton.getPopupSide());
    }

    @Test public void setSpecifiedPopupSide() {
        Side side = Side.TOP;
        menuButton.setPopupSide(side);
        assertSame(side, menuButton.getPopupSide());
    }

    @Test public void getUnspecifiedPopupSideProperty1() {
        assertNotNull(menuButton.popupSideProperty());
    }

    @Test public void getUnspecifiedPopupSideProperty2() {
        MenuButton mb2 = new MenuButton("", null);
        assertNotNull(mb2.popupSideProperty());
    }

    @Test public void unsetPopupSideButNotNull() {
        menuButton.popupSideProperty();
        assertNotNull(menuButton.getPopupSide());
    }

    @Test public void popupSideCanBeBound() {
        Side side = Side.TOP;
        SimpleObjectProperty<Side> other = new SimpleObjectProperty<>(menuButton, "popupSide", side);
        menuButton.popupSideProperty().bind(other);
        assertSame(side, menuButton.getPopupSide());
    }

    //TODO: test show()/isShowing() for disabled=true
    //TODO: test MenuButton.getPsuedoClassState

    @Test
    public void test_RT_21894() {

        // Bug reproduces by setting opacity on the MenuButton
        // then moving focus on and off the MenuButton
        final MenuButton mb = new MenuButton();
        mb.setText("SomeText");

        MenuButtonSkin mbs = new MenuButtonSkin(mb);
        mb.setSkin(mbs);

        Button other = new Button("other");
        // Doesn't have to be done this way, but this more closely duplicates
        // the example code in the bug report.
        other.setOnAction(t -> {
            mb.setOpacity(.5);
        });

        VBox vbox = new VBox();
        vbox.getChildren().addAll(mb, other);
        Scene scene = new Scene(vbox, 300, 300);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        stage.requestFocus();

        other.requestFocus();
        assertFalse(mb.isFocused());

        // set opacity on MenuButton
        other.fire();

        // focus on MenuButton
        mb.requestFocus();
        assertTrue(mb.isFocused());

        // give css a chance to run
        Toolkit.getToolkit().firePulse();

        // focus off the MenuButton
        other.requestFocus();
        assertFalse(mb.isFocused());

        // give css a chance to run
        Toolkit.getToolkit().firePulse();

        // MenuButton should still be 50%
        assertEquals(.5, mb.getOpacity(), 0.00001);

    }

    @Test public void testSetContentDisplayGraphicOnly() {
        Button btn = new Button("1234");

        MenuButton mb1 = new MenuButton("Sample Text", btn);
        mb1.setStyle("-fx-label-padding:0;");
        mb1.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        MenuButton mb2 = new MenuButton("Sample Text", btn);
        mb2.setStyle("-fx-label-padding:100;");
        mb2.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        Scene scene = new Scene(new Group(mb1, mb2), 400, 400);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        Toolkit.getToolkit().firePulse();

        // label-padding should not affect GRAPHIC_ONLY MenuButton size
        assertEquals(mb1.getWidth(), mb2.getWidth(), 0.00001);
        assertEquals(mb1.getHeight(), mb2.getHeight(), 0.00001);
    }

    int onShowing;
    int onShown;
    int onHiding;
    int onHidden;
    EventType[] onShowingEventTypes = new EventType[5];
    EventType[] onShownEventTypes = new EventType[5];
    EventType[] onHidingEventTypes = new EventType[5];
    EventType[] onHiddenEventTypes = new EventType[5];
    // Test for JDK-8175963
    @Test public void test_addOnShowHideEvents() {
        onShowing = 0;
        onShown = 0;
        onHiding = 0;
        onHidden = 0;
        MenuItem it1 = new MenuItem("1");
        MenuButton mbtn = new MenuButton("MenuButton", null, it1);

        for (int i = 0; i < 5; ++i) {
            final int c = i;
            mbtn.addEventHandler(MenuButton.ON_SHOWING, event -> {
                onShowingEventTypes[c] = event.getEventType();
                onShowing++;
            });
            mbtn.addEventHandler(MenuButton.ON_SHOWN, event -> {
                onShownEventTypes[c] = event.getEventType();
                onShown++;
            });
            mbtn.addEventHandler(MenuButton.ON_HIDING, event -> {
                onHidingEventTypes[c] = event.getEventType();
                onHiding++;
            });
            mbtn.addEventHandler(MenuButton.ON_HIDDEN, event -> {
                onHiddenEventTypes[c] = event.getEventType();
                onHidden++;
            });
        }

        mbtn.show();
        mbtn.hide();

        for (int i = 0; i < 5; ++i) {
            assertEquals(MenuButton.ON_SHOWING, onShowingEventTypes[i], "event " + i + " is not of type MenuButton.ON_SHOWN");
            assertEquals(MenuButton.ON_SHOWN, onShownEventTypes[i], "event " + i + " is not of type MenuButton.ON_SHOWN");
            assertEquals(MenuButton.ON_HIDING, onHidingEventTypes[i], "event " + i + " is not of type MenuButton.ON_HIDING");
            assertEquals(MenuButton.ON_HIDDEN, onHiddenEventTypes[i], "event " + i + " is not of type  MenuButton.ON_HIDDEN");
        }

        assertEquals(5, onShowing, "MenuButton.ON_SHOWING event listener should get called 5 times. ");
        assertEquals(5, onShown, "MenuButton.ON_SHOWN event listener should get called 5 times. ");
        assertEquals(5, onHiding, "MenuButton.ON_HIDING event listener should get called 5 times. ");
        assertEquals(5, onHidden, "MenuButton.ON_HIDDEN event listener should get called 5 times. ");
    }

    EventType onShowingEventType;
    EventType onShownEventType;
    EventType onHidingEventType;
    EventType onHiddenEventType;
    // Test for JDK-8177633
    @Test public void test_setOnShowHideEvents() {
        onShowing = 0;
        onShown = 0;
        onHiding = 0;
        onHidden = 0;
        MenuItem it1 = new MenuItem("1");
        MenuButton mbtn = new MenuButton("MenuButton", null, it1);
        for (int i = 0; i < 5; ++i) {
            mbtn.setOnShowing(event -> {
                onShowingEventType = event.getEventType();
                onShowing++;
            });
            mbtn.setOnShown(event -> {
                onShownEventType = event.getEventType();
                onShown++;
            });
            mbtn.setOnHiding(event -> {
                onHidingEventType = event.getEventType();
                onHiding++;
            });
            mbtn.setOnHidden(event -> {
                onHiddenEventType = event.getEventType();
                onHidden++;
            });
        }

        mbtn.show();
        mbtn.hide();

        assertEquals(MenuButton.ON_SHOWING, onShowingEventType, "event is not of type MenuButton.ON_SHOWING");
        assertEquals(MenuButton.ON_SHOWN, onShownEventType, "event is not of type MenuButton.ON_SHOWN");
        assertEquals(MenuButton.ON_HIDING, onHidingEventType, "event is not of type MenuButton.ON_HIDING");
        assertEquals(MenuButton.ON_HIDDEN, onHiddenEventType, "event is not of type MenuButton.ON_HIDDEN");

        assertEquals(1, onShowing, "MenuButton.ON_SHOWING event listener should get called once.");
        assertEquals(1, onShown, "MenuButton.ON_SHOWN event listener should get called once.");
        assertEquals(1, onHiding, "MenuButton.ON_HIDING event listener should get called once.");
        assertEquals(1, onHidden, "MenuButton.ON_HIDDEN event listener should get called once.");
    }

    // Test for JDK-8177633
    @Test public void test_setOnShowHideAndGet() {
        MenuItem it1 = new MenuItem("1");
        MenuButton mbtn = new MenuButton("MenuButton", null, it1);

        EventHandler[] handlers = new EventHandler[4];
        for (int i = 0 ; i < 4; ++i) {
            handlers[i] = new EventHandler() {
                @Override
                public void handle(Event event) {
                }
            };
        }

        mbtn.setOnShowing(handlers[0]);
        mbtn.setOnShown(handlers[1]);
        mbtn.setOnHiding(handlers[2]);
        mbtn.setOnHidden(handlers[3]);

        assertEquals(handlers[0], mbtn.getOnShowing(), "getOnShowing() should return same handler as set using setOnShowing()");
        assertEquals(handlers[1], mbtn.getOnShown(), "getOnShown() should return same handler as set using setOnShown()");
        assertEquals(handlers[2], mbtn.getOnHiding(), "getOnHiding() should return same handler as set using setOnHiding()");
        assertEquals(handlers[3], mbtn.getOnHidden(), "getOnHidden() should return same handler as set using setOnHidden()");

        assertEquals(handlers[0], mbtn.onShowingProperty().get(), "onShowingProperty().get() should return same handler as set using setOnShowing()");
        assertEquals(handlers[1], mbtn.onShownProperty().get(), "onShownProperty().get() should return same handler as set using setOnShown()");
        assertEquals(handlers[2], mbtn.onHidingProperty().get(), "onHidingProperty().get() should return same handler as set using setOnHiding()");
        assertEquals(handlers[3], mbtn.onHiddenProperty().get(), "onHiddenProperty().get() should return same handler as set using setOnHidden()");

        mbtn.setOnShowing(null);
        mbtn.setOnShown(null);
        mbtn.setOnHiding(null);
        mbtn.setOnHidden(null);

        assertEquals(null, mbtn.getOnShowing(), "getOnShowing() should return same handler as set using setOnShowing()");
        assertEquals(null, mbtn.getOnShown(), "getOnShown() should return same handler as set using setOnShown()");
        assertEquals(null, mbtn.getOnHiding(), "getOnHiding() should return same handler as set using setOnHiding()");
        assertEquals(null, mbtn.getOnHidden(), "getOnHidden() should return same handler as set using setOnHidden()");

        assertEquals(null, mbtn.onShowingProperty().get(), "onShowingProperty().get() should return same handler as set using setOnShowing()");
        assertEquals(null, mbtn.onShownProperty().get(), "onShownProperty().get() should return same handler as set using setOnShown()");
        assertEquals(null, mbtn.onHidingProperty().get(), "onHidingProperty().get() should return same handler as set using setOnHiding()");
        assertEquals(null, mbtn.onHiddenProperty().get(), "onHiddenProperty().get() should return same handler as set using setOnHidden()");
    }

    // Test for JDK-8177633
    @Test public void test_setOnShowHidePropertyAndGet() {
        MenuItem it1 = new MenuItem("1");
        MenuButton mbtn = new MenuButton("MenuButton", null, it1);

        EventHandler[] handlers = new EventHandler[4];
        for (int i = 0 ; i < 4; ++i) {
            handlers[i] = new EventHandler() {
                @Override
                public void handle(Event event) {
                }
            };
        }

        mbtn.onShowingProperty().set(handlers[0]);
        mbtn.onShownProperty().set(handlers[1]);
        mbtn.onHidingProperty().set(handlers[2]);
        mbtn.onHiddenProperty().set(handlers[3]);

        assertEquals(handlers[0], mbtn.getOnShowing(), "getOnShowing() should return same handler as set using onShowingProperty().set()");
        assertEquals(handlers[1], mbtn.getOnShown(), "getOnShown() should return same handler as set using onShownProperty().set()");
        assertEquals(handlers[2], mbtn.getOnHiding(), "getOnHiding() should return same handler as set using onHidingProperty().set()");
        assertEquals(handlers[3], mbtn.getOnHidden(), "getOnHidden() should return same handler as set using onHiddenProperty().set()");

        assertEquals(handlers[0], mbtn.onShowingProperty().get(), "onShowingProperty().get() should return same handler as set using onShowingProperty().set()");
        assertEquals(handlers[1], mbtn.onShownProperty().get(), "onShownProperty().get() should return same handler as set using onShownProperty().set()");
        assertEquals(handlers[2], mbtn.onHidingProperty().get(), "onHidingProperty().get() should return same handler as set using onHidingProperty().set()");
        assertEquals(handlers[3], mbtn.onHiddenProperty().get(), "onHiddenProperty().get() should return same handler as set using onHiddenProperty().set()");

        mbtn.onShowingProperty().set(null);
        mbtn.onShownProperty().set(null);
        mbtn.onHidingProperty().set(null);
        mbtn.onHiddenProperty().set(null);

        assertEquals(null, mbtn.getOnShowing(), "getOnShowing() should return same handler as set using onShowingProperty().set()");
        assertEquals(null, mbtn.getOnShown(), "getOnShown() should return same handler as set using onShownProperty().set()");
        assertEquals(null, mbtn.getOnHiding(), "getOnHiding() should return same handler as set using onHidingProperty().set()");
        assertEquals(null, mbtn.getOnHidden(), "getOnHidden() should return same handler as set using onHiddenProperty().set()");

        assertEquals(null, mbtn.onShowingProperty().get(), "onShowingProperty().get() should return same handler as set using onShowingProperty().set()");
        assertEquals(null, mbtn.onShownProperty().get(), "onShownProperty().get() should return same handler as set using onShownProperty().set()");
        assertEquals(null, mbtn.onHidingProperty().get(), "onHidingProperty().get() should return same handler as set using onHidingProperty().set()");
        assertEquals(null, mbtn.onHiddenProperty().get(), "onHiddenProperty().get() should return same handler as set using onHiddenProperty().set()");
    }

    // Test for JDK-8177633
    boolean onShownOrderTest = false;
    boolean onShowingOrderTest = false;
    @Test public void test_OrderOfShowEvents() {
        onShowing = 0;
        onShown = 0;
        MenuItem it1 = new MenuItem("1");
        MenuButton mbtn = new MenuButton("MenuButton", null, it1);

        mbtn.setOnShowing(event -> {
            onShowing = 1;
            onShowingOrderTest = onShown == 0;
        });
        mbtn.setOnShown(event -> {
            onShown = 1;
            onShownOrderTest = onShowing == 1;
        });

        mbtn.show();
        mbtn.hide();

        assertEquals(1, onShowing, "MenuButton.ON_SHOWING event listener should get called once.");
        assertEquals(1, onShown, "MenuButton.ON_SHOWN event listener should get called once.");
        assertTrue(onShowingOrderTest, "MenuButton.ON_SHOWING event should be received before MenuButton.ON_SHOWN.");
        assertTrue(onShownOrderTest, "MenuButton.ON_SHOWN event should be received after MenuButton.ON_SHOWING.");
    }

    // Test for JDK-8177633
    boolean onHidingOrderTest = false;
    boolean onHiddenOrderTest = false;
    @Test public void test_OrderOfHideEvents() {
        onHiding = 0;
        onHidden = 0;
        MenuItem it1 = new MenuItem("1");
        MenuButton mbtn = new MenuButton("MenuButton", null, it1);

        mbtn.setOnHiding(event -> {
            onHiding = 1;
            onHidingOrderTest = onHidden == 0;
        });
        mbtn.setOnHidden(event -> {
            onHidden = 1;
            onHiddenOrderTest = onHiding == 1;
        });

        mbtn.show();
        mbtn.hide();

        assertEquals(1, onHiding, "MenuButton.ON_HIDING event listener should get called once.");
        assertEquals(1, onHidden, "MenuButton.ON_HIDDEN event listener should get called once.");
        assertTrue(onHidingOrderTest, "MenuButton.ON_HIDING event should be received before MenuButton.ON_HIDDEN.");
        assertTrue(onHiddenOrderTest, "MenuButton.ON_HIDDEN event should be received after MenuButton.ON_HIDING.");
    }

    // Test for JDK-8177633
    @Test public void test_OrderOfShowHideEvents() {
        onShowing = 0;
        onShown = 0;
        onHiding = 0;
        onHidden = 0;
        MenuItem it1 = new MenuItem("1");
        MenuButton mbtn = new MenuButton("MenuButton", null, it1);

        mbtn.setOnShowing(event -> {
            onShowing = 1;
        });
        mbtn.setOnShown(event -> {
            onShown = 1;
        });
        mbtn.setOnHiding(event -> {
            onHiding = 1;
        });
        mbtn.setOnHidden(event -> {
            onHidden = 1;
        });

        mbtn.show();
        assertEquals(1, onShowing, "MenuButton.ON_SHOWING event listener should get called once.");
        assertEquals(1, onShown, "MenuButton.ON_SHOWN event listener should get called once.");
        assertEquals(0, onHiding, "MenuButton.ON_HIDING event should not be received while showing.");
        assertEquals(0, onHidden, "MenuButton.ON_HIDDEN event should not be received while showing.");

        onShown = 0;
        onShowing = 0;
        mbtn.hide();
        assertEquals(1, onHiding, "MenuButton.ON_HIDING event listener should get called once.");
        assertEquals(1, onHidden, "MenuButton.ON_HIDDEN event listener should get called once.");
        assertEquals(0, onShowing, "MenuButton.ON_SHOWING event should not be received while hiding.");
        assertEquals(0, onShown, "MenuButton.ON_SHOWN event should not be received while showing.");
    }

    // Test for JDK-8177633
    @Test public void test_onShowHidePropertyAttribs() {
        MenuButton mbtn = new MenuButton();

        assertEquals("onShowing", mbtn.onShowingProperty().getName(), "MenuButton.onShowing name should be \"onShowing\".");
        assertEquals("onShown", mbtn.onShownProperty().getName(), "MenuButton.onShown name should be \"onShown\".");
        assertEquals("onHiding", mbtn.onHidingProperty().getName(), "MenuButton.onHiding name should be \"onHiding\".");
        assertEquals("onHidden", mbtn.onHiddenProperty().getName(), "MenuButton.onHidden name should be \"onHidden\".");

        assertEquals(mbtn, mbtn.onShowingProperty().getBean(), "MenuButton.onShowing bean should be MenuButton object.");
        assertEquals(mbtn, mbtn.onShownProperty().getBean(), "MenuButton.onShown bean should be MenuButton object.");
        assertEquals(mbtn, mbtn.onHidingProperty().getBean(), "MenuButton.onHiding bean should be MenuButton object.");
        assertEquals(mbtn, mbtn.onHiddenProperty().getBean(), "MenuButton.onHidden bean should be MenuButton object.");
    }

    /**
     * JDK-8295426 menu button popup is not shown on mouse click after skin is
     * replaced.
     */
    @Test
    public void testMenuButtonPopupAfterSkinReplaced() {
        MenuItem mi = new MenuItem("MenuItem1");

        MenuButton b = new MenuButton("Menu Button");
        b.getItems().add(mi);

        VBox root = new VBox();
        root.getChildren().addAll(b);

        Scene scene = new Scene(root, 800, 600);
        Stage stage = new Stage();
        // keep stage at 0,0 so the screen X/Y to be the same as local Node X/Y, for
        // MouseEventGenerator
        stage.setX(0);
        stage.setY(0);
        stage.setScene(scene);
        stage.show();
        stage.requestFocus();

        Toolkit.getToolkit().firePulse();

        MenuButtonSkin skin = (MenuButtonSkin)b.getSkin();
        assertTrue(skin != null);

        double offset = 15;
        double x = (b.localToScene(b.getLayoutBounds())).getMinX() + offset;
        double y = (b.localToScene(b.getLayoutBounds())).getMinY() + offset;

        // click on menu button to show the popup
        SceneHelper.processMouseEvent(scene, MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, x, y));
        SceneHelper.processMouseEvent(scene, MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, x, y));
        assertTrue(b.isShowing());

        // change the skin
        b.setSkin(new MenuButtonSkin(b));
        b.requestFocus();
        Toolkit.getToolkit().firePulse();
        assertTrue(b.isFocused());

        // click once more on menu button to hide the popup
        SceneHelper.processMouseEvent(scene, MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, x, y));
        SceneHelper.processMouseEvent(scene, MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, x, y));
        Toolkit.getToolkit().firePulse();
        assertFalse(b.isShowing()); // fails JDK-8295426

        // click again to show the popup
        SceneHelper.processMouseEvent(scene, MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, x, y));
        SceneHelper.processMouseEvent(scene, MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, x, y));
        Toolkit.getToolkit().firePulse();
        assertTrue(b.isShowing());

        stage.hide();
    }
}
