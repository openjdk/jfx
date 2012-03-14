/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.scene.control.skin.MenuBarMenuButtonRetriever;
import com.sun.javafx.scene.control.skin.MenuBarSkin;
import com.sun.javafx.tk.Toolkit;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import javafx.event.EventType;
import javafx.scene.Node;

import org.junit.Before;
import org.junit.Test;


/**
 *
 * @author lubermud
 */
public class MenuBarTest {
    private MenuBar menuBar;
    private Toolkit tk;
    private Scene scene;
    private Stage stage;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();
        menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);
    }
    
    protected void startApp(Parent root) {
        scene = new Scene(root,800,600);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
        tk.firePulse();
    }

    @Test public void defaultConstructorHasFalseFocusTraversable() {
        assertFalse(menuBar.isFocusTraversable());
    }

    @Test public void defaultConstructorButSetTrueFocusTraversable() {
            menuBar.setFocusTraversable(true);
        assertTrue(menuBar.isFocusTraversable());
    }

    @Test public void getMenusHasSizeZero() {
        assertEquals(0, menuBar.getMenus().size());
    }

    @Test public void getMenusIsAddable() {
        menuBar.getMenus().add(new Menu(""));
        assertTrue(menuBar.getMenus().size() > 0);
    }

    @Test public void getMenusIsClearable() {
        menuBar.getMenus().add(new Menu(""));
        menuBar.getMenus().clear();
        assertEquals(0, menuBar.getMenus().size());
    }
    
    @Test public void testMenuShowHideWithMenuBarWithXYTranslation() {
        final MouseEventGenerator generator = new MouseEventGenerator();
        AnchorPane root = new AnchorPane();
        Menu menu = new Menu("Menu");
        menu.getItems().add(new MenuItem("MenuItem"));
        menuBar.getMenus().add(menu);
        menuBar.setLayoutX(100);
        menuBar.setLayoutY(100);
        root.getChildren().add(menuBar);
        startApp(root);
        tk.firePulse();
        
        MenuBarSkin skin = (MenuBarSkin)menuBar.getSkin();
        assertTrue(skin != null);
        
        double xval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinX();
        double yval = (menuBar.localToScene(menuBar.getLayoutBounds())).getMinY();
   
        MenuButton mb = MenuBarMenuButtonRetriever.getNodeForMenu(skin, 0);
        mb.getScene().getWindow().requestFocus();
        scene.impl_processMouseEvent(
            generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        scene.impl_processMouseEvent(
            generator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));
        assertTrue(menu.isShowing());
        
    }

    static final class MouseEventGenerator {
        private boolean primaryButtonDown = false;

        public MouseEvent generateMouseEvent(EventType<MouseEvent> type,
                double x, double y) {

            MouseButton button = MouseButton.NONE;
            if (type == MouseEvent.MOUSE_PRESSED ||
                    type == MouseEvent.MOUSE_RELEASED ||
                    type == MouseEvent.MOUSE_DRAGGED) {
                button = MouseButton.PRIMARY;
            }

            if (type == MouseEvent.MOUSE_PRESSED ||
                    type == MouseEvent.MOUSE_DRAGGED) {
                primaryButtonDown = true;
            }

            if (type == MouseEvent.MOUSE_RELEASED) {
                primaryButtonDown = false;
            }

            MouseEvent event = MouseEvent.impl_mouseEvent(x, y, x, y, button,
                    1, false, false, false, false, false, primaryButtonDown,
                    false, false, false, type);

            return event;
        }
    }
    
//    static final class MouseEventTracker {
//        private Node node;
//        
//        public MouseEventTracker(final Node node) {
//            this.node = node;
//            
//            node.setOnMouseClicked(new EventHandler<MouseEvent>() {
//                @Override
//                public void handle(MouseEvent t) {
//                    // println here to check if node received mouse event
//                }
//            });
//        }
//    }

}
