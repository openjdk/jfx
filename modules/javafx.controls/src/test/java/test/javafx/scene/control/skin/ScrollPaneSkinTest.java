/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.control.skin.ScrollPaneSkinShim;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class ScrollPaneSkinTest {
    private ScrollPane scrollPane;
    private ScrollPaneSkinMock skin;

    @Before public void setup() {
        scrollPane = new ScrollPane();
        skin = new ScrollPaneSkinMock(scrollPane);
        scrollPane.setSkin(skin);
    }

    /*
    ** RT-16641 : root cause, you shouldn't be able to drag
    ** contents if they don't fill the scrollpane
    */
    @Test public void shouldntDragContentSmallerThanViewport() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        StackPane sp = new StackPane();
        sp.setPrefWidth(80);
        sp.setPrefHeight(80);

        scrollPane.setContent(sp);
        scrollPane.setTranslateX(70);
        scrollPane.setTranslateY(30);
        scrollPane.setPrefWidth(100);
        scrollPane.setPrefHeight(100);
        scrollPane.setPannable(true);

        MouseEventGenerator generator = new MouseEventGenerator();

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        double originalValue = scrollPane.getVvalue();

        Event.fireEvent(sp, generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        Event.fireEvent(sp, generator.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 75, 75));
        Event.fireEvent(sp, generator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 75, 75));

        assertEquals(originalValue, scrollPane.getVvalue(), 0.01);

    }

    /*
     * check we can drag contents that are larger than the scrollpane
     */
    @Test public void shouldDragContentLargerThanViewport() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        StackPane sp = new StackPane();
        sp.setPrefWidth(180);
        sp.setPrefHeight(180);

        scrollPane.setContent(sp);
        scrollPane.setTranslateX(70);
        scrollPane.setTranslateY(30);
        scrollPane.setPrefWidth(100);
        scrollPane.setPrefHeight(100);
        scrollPane.setPannable(true);

        MouseEventGenerator generator = new MouseEventGenerator();

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        double originalValue = scrollPane.getVvalue();

        Event.fireEvent(sp, generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 75, 75));
        Event.fireEvent(sp, generator.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 50, 50));
        Event.fireEvent(sp, generator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50));

        assertTrue(originalValue < scrollPane.getVvalue());
    }


    boolean continueTest;
    class myPane extends Pane {
        public void growH() {
            setHeight(300);
        }
       public void growW() {
            setWidth(300);
        }

    }
    myPane pInner;

    /*
    ** check if scrollPane content vertical position compensates for content size change
    */
    @Test public void checkPositionOnContentSizeChangeHeight() {
        pInner = new myPane();
        pInner.setPrefWidth(200);
        pInner.setPrefHeight(200);

        scrollPane.setContent(pInner);
        scrollPane.setPrefWidth(100);
        scrollPane.setPrefHeight(100);

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        double originalValue = 0.5;
        scrollPane.setVvalue(originalValue);

        continueTest = false;
        scrollPane.vvalueProperty().addListener((observable, oldBounds, newBounds) -> {
            continueTest = true;
        });

        /*
        ** increase the height of the content
        */
        pInner.growH();

        int count = 0;
        while (continueTest == false && count < 10) {
            try {
                Thread.sleep(100);
            }
            catch (Exception e) {}
            count++;
        }

        /*
        ** did it work?
        */
        assertTrue(originalValue > scrollPane.getVvalue() && scrollPane.getVvalue() > 0.0);
    }


    /*
    ** check if scrollPane content Horizontal position compensates for content size change
    */
    @Test public void checkPositionOnContentSizeChangeWidth() {

        pInner = new myPane();
        pInner.setPrefWidth(200);
        pInner.setPrefHeight(200);

        scrollPane.setContent(pInner);
        scrollPane.setPrefWidth(100);
        scrollPane.setPrefHeight(100);

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        double originalValue = 0.5;
        scrollPane.setHvalue(originalValue);

        continueTest = false;
        scrollPane.hvalueProperty().addListener((observable, oldBounds, newBounds) -> {
            continueTest = true;
        });

        /*
        ** increase the width of the content
        */
        pInner.growW();

        int count = 0;
        while (continueTest == false && count < 10) {
            try {
                Thread.sleep(100);
            }
            catch (Exception e) {}
            count++;
        }

        /*
        ** did it work?
        */
        assertTrue(originalValue > scrollPane.getHvalue() && scrollPane.getHvalue() > 0.0);
    }


    private boolean scrolled;
    /*
    ** check if scrollPane content Horizontal position compensates for content size change
    */
    @Test public void checkIfScrollPaneWithinScrollPaneGetsScrollEvents() {

        scrolled = false;

        Rectangle rect = new Rectangle(100, 100, 100, 100);
        rect.setOnScroll(event -> {
            scrolled = true;
        });

        final ScrollPane scrollPaneInner = new ScrollPane();
        scrollPaneInner.setSkin(new ScrollPaneSkin(scrollPaneInner));
        scrollPaneInner.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPaneInner.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPaneInner.setPrefWidth(100);
        scrollPaneInner.setPrefHeight(100);
        scrollPaneInner.setPannable(true);
        scrollPaneInner.setContent(rect);

        Pane pOuter = new Pane();
        pOuter.setPrefWidth(600);
        pOuter.setPrefHeight(600);
        pOuter.getChildren().add(scrollPaneInner);

        final ScrollPane scrollPaneOuter = new ScrollPane();
        scrollPaneOuter.setSkin(new ScrollPaneSkin(scrollPaneOuter));
        scrollPaneOuter.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPaneOuter.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPaneOuter.setPrefWidth(500);
        scrollPaneOuter.setPrefHeight(500);
        scrollPaneOuter.setPannable(true);
        scrollPaneOuter.setOnScroll(event -> {
            scrolled = true;
        });
        scrollPaneOuter.setContent(pOuter);

        Scene scene = new Scene(new Group(), 700, 700);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPaneOuter);
        scrolled = false;

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Event.fireEvent(rect,
              new ScrollEvent(ScrollEvent.SCROLL,
                          50, 50,
                          50, 50,
                          false, false, false, false, true, false,
                          0.0, -50.0, 0.0, -50.0,
                          ScrollEvent.HorizontalTextScrollUnits.NONE, 10.0,
                          ScrollEvent.VerticalTextScrollUnits.NONE, 10.0,
                          0, null));

        /*
        ** did it work?
        */
        assertTrue(scrollPaneInner.getVvalue() > 0.0);
    }


    boolean sceneClicked = false;
    /*
    ** check if unconsumed MouseClicked events on a scrollPane reach it's parent.
    */
    @Test public void checkIfScrollPaneConsumesMouseClickedEvents() {
        ScrollPane scrollPaneInner = new ScrollPane();
        scrollPaneInner.setSkin(new ScrollPaneSkin(scrollPaneInner));
        scrollPaneInner.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPaneInner.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPaneInner.setTranslateX(70);
        scrollPaneInner.setTranslateY(30);
        scrollPaneInner.setPrefWidth(100);
        scrollPaneInner.setPrefHeight(100);
        scrollPaneInner.setPannable(true);

        Scene scene = new Scene(new Group(), 400, 400);
        scene.setOnMouseClicked(me -> {
            sceneClicked = true;
        });

        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPaneInner);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Event.fireEvent(scrollPaneInner,
              new MouseEvent(MouseEvent.MOUSE_CLICKED, 50.0, 50.0, 50.0, 50.0,
                         MouseButton.PRIMARY, 1,
                         false, false, false, false, false,
                         true, false, false, false, false, null
                         ));

        /*
        ** did it work?
        */
        assertTrue(sceneClicked == true);
    }

    /*
    ** check if ScrollPane gets focus on unconsumed mousePressed
    */
    @Test public void checkIfScrollPaneFocusesPressedEvents() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        StackPane sp = new StackPane();
        sp.setPrefWidth(80);
        sp.setPrefHeight(80);

        scrollPane.setContent(sp);
        scrollPane.setTranslateX(70);
        scrollPane.setTranslateY(30);
        scrollPane.setPrefWidth(100);
        scrollPane.setPrefHeight(100);
        scrollPane.setPannable(true);

        MouseEventGenerator generator = new MouseEventGenerator();

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        double originalValue = scrollPane.getVvalue();

        Event.fireEvent(sp, generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        /*
        ** did it work?
        */
        assertTrue(scene.getFocusOwner() == scrollPane);
    }

    /*
    ** check if ScrollPane gets focus on unconsumed mousePressed
    */
    @Test public void checkIfScrollPaneViewportIsRounded() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        StackPane sp = new StackPane();
        sp.setPrefWidth(80);
        sp.setPrefHeight(80);

        scrollPane.setContent(sp);
        scrollPane.setTranslateX(70);
        scrollPane.setTranslateY(30);
        scrollPane.setPrefViewportWidth(100.5);
        scrollPane.setPrefHeight(100);
        scrollPane.setPannable(true);

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        /*
        ** did it work?
        */
        assertTrue(scrollPane.getViewportBounds().getWidth() == Math.ceil(100.5));
    }

    /*
    ** check ScrollBars missing if fitToHeight is true but height is > minHeight
    */
    @Test public void checkNoScrollbarsWhenFitToAndSizeOK() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        StackPane sp = new StackPane();
        sp.setPrefWidth(80);
        sp.setPrefHeight(80);
        sp.setMinWidth(40);
        sp.setMinHeight(40);

        scrollPane.setPrefSize(50, 50);
        scrollPane.setContent(sp);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        /*
        ** did it work?
        */
        assertTrue(!skin.isHSBarVisible() & !skin.isVSBarVisible());
    }

    /*
    ** check if ScrollBars appear if fitToHeight is true but height is < minHeight
    */
    @Test public void checkIfScrollbarsWhenFitToHeightAndHeightLessMin() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        StackPane sp = new StackPane();
        sp.setPrefWidth(80);
        sp.setPrefHeight(80);
        sp.setMinWidth(40);
        sp.setMinHeight(40);

        // see comments in checkIfScrollbarsWhenFitToWidthAndWidthLessMin for why width is 60
        scrollPane.setPrefSize(60, 30);
        scrollPane.setContent(sp);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        /*
        ** did it work?
        */
        assertTrue(!skin.isHSBarVisible() & skin.isVSBarVisible());
    }

    /*
    ** check if ScrollBars appear if fitToWidth is true but width is < minWidth
    */
    @Test public void checkIfScrollbarsWhenFitToWidthAndWidthLessMin() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        StackPane sp = new StackPane();
        sp.setPrefWidth(80);
        sp.setPrefHeight(80);
        sp.setMinWidth(40);
        sp.setMinHeight(40);

        // set width < content minWidth, should get horizontal scroll bar
        // set height > content minHeight, should not get vertical scroll bar
        // Note that the scrollPane's height has to be big enough to fit both the
        // horizontal ScrollBar and the re-sized content. If the ScrollBar's height
        // is 16, then the scrollPane's prefHeight needs to be 16 plus the content's
        // minHeight of 40 plus one pixel (since the content and the ScrollBar don't
        // overlap), all of which adds up to 57. Originally, this test had the height
        // set to 50 which caused the vertical ScrollBar to be visible.
        scrollPane.setPrefSize(30, 60);
        scrollPane.setContent(sp);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        /*
        ** did it work?
        */
        assertTrue(skin.isHSBarVisible() & !skin.isVSBarVisible());
    }

    /*
    ** check if ScrollBars appear if fitToHeight & fitToWidth are true but height is < minHeight & width is < minWidth
    */
    @Test public void checkIfScrollbarsWhenBothFitToAndBothLessMin() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        StackPane sp = new StackPane();
        sp.setPrefWidth(80);
        sp.setPrefHeight(80);
        sp.setMinWidth(40);
        sp.setMinHeight(40);

        scrollPane.setPrefSize(60, 30);
        scrollPane.setContent(sp);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        /*
        ** did it work?
        */
        assertTrue(!skin.isHSBarVisible() & skin.isVSBarVisible());
    }

    /*
    ** check if ScrollBars appear if fitToHeight & fitToWidth are true but height is < minHeight & width is < minWidth
    */
    @Test public void checkWeHandleNullContent() {


        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane);

        Stage stage = new Stage();
        stage.setScene(scene);

        stage.setWidth(600);
        stage.setHeight(600);

        stage.show();

    }


    /*
    ** check if 'reduced-size' scrollbars leave a gap
    ** at the right edge
    */
    @Test public void checkForScrollBarGaps() {

        HBox hbox1 = new HBox(20);
        VBox vbox1a = new VBox(10);
        vbox1a.getChildren().addAll(new Label("one"), new Button("two"), new CheckBox("three"), new RadioButton("four"), new Label("five"));
        VBox vbox1b = new VBox(10);
        vbox1b.getChildren().addAll(new Label("one"), new Button("two"), new CheckBox("three"), new RadioButton("four"), new Label("five"));
        hbox1.getChildren().addAll(vbox1a, vbox1b);
        scrollPane.setContent(hbox1);
        scrollPane.setStyle("-fx-background-color: red;-fx-border-color:green;");
        scrollPane.setFocusTraversable(false);
        scrollPane.setPrefSize(50, 50);


        Scene scene = new Scene(new Group(), 400, 400);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPane);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();


        /*
        ** did it work?
        ** check that the scrollbar is right at the border of the scrollpane, and
        ** not at the padding.
        */
        ScrollPaneSkinMock skin = (ScrollPaneSkinMock) scrollPane.getSkin();

        double skinWidth = scrollPane.getWidth();
        double right = scrollPane.getPadding().getRight();
        // 1 px of padding is on the outside - see RT-21251
        double vsbPosAndWidth = (right >= 1 ? 1 : 0) + skin.getVsbX()+skin.getVsbWidth()+(scrollPane.getInsets().getRight() - right);
        assertEquals(skinWidth,  vsbPosAndWidth, 0.1);

        double skinHeight = scrollPane.getHeight();
        // 1 px of padding is on the outside - see RT-21251
        double bottom = scrollPane.getPadding().getBottom();
        double hsbPosAndHeight = (bottom >= 1 ? 1 : 0) + skin.getHsbY()+skin.getHsbHeight()+(scrollPane.getInsets().getBottom() - bottom);
        assertEquals(skinHeight,  hsbPosAndHeight, 0.1);

    }

    /*
    ** check if scrollPane content Horizontal position compensates for content size change
    ** ignored, as we do swipe through ScrollEvents.
    */
    @Ignore
    @Test public void checkIfSwipeDownEventsChangeAnything() {

        scrolled = false;

        Rectangle rect = new Rectangle(200, 200, 200, 200);

        final ScrollPane scrollPaneInner = new ScrollPane();
        scrollPaneInner.setSkin(new ScrollPaneSkin(scrollPaneInner));
        scrollPaneInner.setPrefWidth(100);
        scrollPaneInner.setPrefHeight(100);
        scrollPaneInner.setPannable(true);
        scrollPaneInner.setContent(rect);
        scrollPaneInner.setOnSwipeUp(event -> {
            scrolled = true;
        });
        scrollPaneInner.setOnSwipeDown(event -> {
            scrolled = true;
        });
        scrollPaneInner.setOnSwipeLeft(event -> {
            scrolled = true;
        });
        scrollPaneInner.setOnSwipeRight(event -> {
            scrolled = true;
        });
        Pane pOuter = new Pane();
        pOuter.setPrefWidth(600);
        pOuter.setPrefHeight(600);

        Scene scene = new Scene(new Group(), 700, 700);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPaneInner);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Event.fireEvent(rect,
         new SwipeEvent(SwipeEvent.SWIPE_DOWN,
            0.0, -50.0,
            0.0, -50.0,
            false,
            false,
            false,
            false,
            false,
            1,
            null
            ));

        /*
        ** did it work?
        */
        assertTrue(scrollPaneInner.getVvalue() > 0.0);
    }

    /*
    ** check if scrollPane content Horizontal position compensates for content size change
    ** ignored, as we do swipe through ScrollEvents.
    */
    @Ignore
    @Test public void checkIfSwipeRightEventsChangeAnything() {

        scrolled = false;

        Rectangle rect = new Rectangle(200, 200, 200, 200);

        final ScrollPane scrollPaneInner = new ScrollPane();
        scrollPaneInner.setSkin(new ScrollPaneSkin(scrollPaneInner));
        scrollPaneInner.setPrefWidth(100);
        scrollPaneInner.setPrefHeight(100);
        scrollPaneInner.setPannable(true);
        scrollPaneInner.setContent(rect);
        scrollPaneInner.setOnSwipeUp(event -> {
            scrolled = true;
        });
        scrollPaneInner.setOnSwipeDown(event -> {
            scrolled = true;
        });
        scrollPaneInner.setOnSwipeLeft(event -> {
            scrolled = true;
        });
        scrollPaneInner.setOnSwipeRight(event -> {
            scrolled = true;
        });
        Pane pOuter = new Pane();
        pOuter.setPrefWidth(600);
        pOuter.setPrefHeight(600);

        Scene scene = new Scene(new Group(), 700, 700);
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPaneInner);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Event.fireEvent(rect,
            new SwipeEvent(SwipeEvent.SWIPE_RIGHT,
            0.0, -50.0,
            0.0, -50.0,
            false,
            false,
            false,
            false,
            false,
            1,
            null
            ));

         /*
         ** did it work?
         */
        assertTrue(scrollPaneInner.getHvalue() > 0.0);
    }

    @Test
    public void testScrollDeltaIsIndependentOfScrollPaneHeight() {
        var content = new Rectangle();
        content.setHeight(200);
        content.setWidth(100);

        var scrollPane = new ScrollPane();
        scrollPane.setSkin(new ScrollPaneSkinMock(scrollPane));
        scrollPane.setContent(content);
        scrollPane.setPrefWidth(100);
        scrollPane.setPrefHeight(100);

        Stage stage = new Stage();
        stage.setScene(new Scene(new Group(scrollPane), 500, 500));
        stage.show();

        Event.fireEvent(content, new ScrollEvent(
            ScrollEvent.SCROLL,
            50, 50,
            50, 50,
            false, false, false, false, true, false,
            0.0, -10.0, 0.0, -10.0,
            ScrollEvent.HorizontalTextScrollUnits.NONE, 0.0,
            ScrollEvent.VerticalTextScrollUnits.NONE, 0.0,
            0, null));

        double firstY = content.getLocalToSceneTransform().transform(0, 0).getY();
        scrollPane.setPrefHeight(150);
        scrollPane.setVvalue(0);

        stage.close();
        stage = new Stage();
        stage.setScene(new Scene(new Group(scrollPane), 500, 500));
        stage.show();

        Event.fireEvent(content, new ScrollEvent(
            ScrollEvent.SCROLL,
            50, 50,
            50, 50,
            false, false, false, false, true, false,
            0.0, -10.0, 0.0, -10.0,
            ScrollEvent.HorizontalTextScrollUnits.NONE, 0.0,
            ScrollEvent.VerticalTextScrollUnits.NONE, 0.0,
            0, null));

        double secondY = content.getLocalToSceneTransform().transform(0, 0).getY();
        stage.close();

        assertEquals(firstY, secondY, 0.001);
    }

    public static final class ScrollPaneSkinMock extends ScrollPaneSkinShim {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ScrollPaneSkinMock(ScrollPane scrollPane) {
            super(scrollPane);
        }

        public void addWatchedProperty(ObservableValue<?> p) {
            p.addListener(o -> {
                propertyChanged = true;
                propertyChangeCount++;
            });
        }

        boolean isHSBarVisible() {
            return get_hsb().isVisible();
        }

        boolean isVSBarVisible() {
            return get_vsb().isVisible();
        }
        double getVsbX() {
            return get_vsb().getLayoutX();
        }
        double getVsbWidth() {
            return get_vsb().getWidth();
        }
        double getHsbY() {
            return get_hsb().getLayoutY();
        }
        double getHsbHeight() {
            return get_hsb().getHeight();
        }
    }

    private static class MouseEventGenerator {
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

            MouseEvent event = new MouseEvent(type, x, y, x, y, button,
                    1, false, false, false, false, false, primaryButtonDown,
                    false, false, false, false, null);

            return event;
        }
    }
}
