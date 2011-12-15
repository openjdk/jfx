/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.pgstub.StubScene;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;
import static org.junit.Assert.*;

import javafx.scene.input.ScrollEvent;

/**
 * @author mickf
 */
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
    ** check we can drag contents that are larger than the scrollpane
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

        Event.fireEvent(sp, generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));
        Event.fireEvent(sp, generator.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 75, 75));
        Event.fireEvent(sp, generator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 75, 75));

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
        scrollPane.vvalueProperty().addListener(new ChangeListener() {
            @Override public void changed(ObservableValue observable, Object oldBounds, Object newBounds) {
                continueTest = true;
            }
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
        scrollPane.hvalueProperty().addListener(new ChangeListener() {
            @Override public void changed(ObservableValue observable, Object oldBounds, Object newBounds) {
                continueTest = true;
            }
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
        rect.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled = true;
            }
        });
        
        final ScrollPane scrollPaneInner = new ScrollPane();
        scrollPaneInner.setSkin(new com.sun.javafx.scene.control.skin.ScrollPaneSkin(scrollPaneInner));
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
        scrollPaneOuter.setSkin(new com.sun.javafx.scene.control.skin.ScrollPaneSkin(scrollPaneOuter));
        scrollPaneOuter.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPaneOuter.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPaneOuter.setPrefWidth(500);
        scrollPaneOuter.setPrefHeight(500);
        scrollPaneOuter.setPannable(true);
        scrollPaneOuter.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                scrolled = true;
            }
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
              ScrollEvent.impl_scrollEvent(
                          0.0, -50.0,
                          ScrollEvent.HorizontalTextScrollUnits.NONE, 10.0,
                          ScrollEvent.VerticalTextScrollUnits.NONE, 10.0,
                          50, 50,
                          50, 50,
                          false, false, false, false));

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
        scrollPaneInner.setSkin(new com.sun.javafx.scene.control.skin.ScrollPaneSkin(scrollPaneInner));
        scrollPaneInner.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPaneInner.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPaneInner.setTranslateX(70);
        scrollPaneInner.setTranslateY(30);
        scrollPaneInner.setPrefWidth(100);
        scrollPaneInner.setPrefHeight(100);
        scrollPaneInner.setPannable(true);     

        Scene scene = new Scene(new Group(), 400, 400);
        scene.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                sceneClicked = true;
            }
        });
        
        ((Group) scene.getRoot()).getChildren().clear();
        ((Group) scene.getRoot()).getChildren().add(scrollPaneInner);

        Stage stage = new Stage();
        stage.setScene(scene);     
        stage.show();

        Event.fireEvent(scrollPaneInner,
              MouseEvent.impl_mouseEvent(50.0, 50.0, 50.0, 50.0,
                         MouseButton.PRIMARY, 1,
                         false, false, false, false, false,
                         true, false, false,
                         MouseEvent.MOUSE_CLICKED));

        /*
        ** did it work?
        */
        assertTrue(sceneClicked == true);
    }
    
    public static final class ScrollPaneSkinMock extends ScrollPaneSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ScrollPaneSkinMock(ScrollPane scrollPane) {
            super(scrollPane);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
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

            MouseEvent event = MouseEvent.impl_mouseEvent(x, y, x, y, button,
                    1, false, false, false, false, false, primaryButtonDown,
                    false, false, type);

            return event;
        }
    }
   private boolean scrolled;

   /*
   ** check if scrollPane content Horizontal position compensates for content size change
   */
   @Test public void checkIfScrollPaneWithinScrollPaneGetsScrollEvents() {


       scrolled = false;

       Rectangle rect = new Rectangle(100, 100, 100, 100);
       rect.setOnScroll(new EventHandler<ScrollEvent>() {
           @Override public void handle(ScrollEvent event) {
               System.out.println("<> rect got a ScrollEvent : "+event);
               scrolled = true;
           }
       });

       final ScrollPane scrollPaneInner = new ScrollPane();
       //scrollPaneInner.setStyle("-fx-skin: com.sun.javafx.scene.control.skin.ScrollPaneSkin;");


       scrollPaneInner.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
       scrollPaneInner.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
       scrollPaneInner.setPrefWidth(100);
       scrollPaneInner.setPrefHeight(100);
       scrollPaneInner.setPannable(true);
       scrollPaneInner.vvalueProperty().addListener(new ChangeListener() {
           @Override public void changed(ObservableValue observable, Object oldBounds, Object newBounds) {
               System.out.println("<><> scrollPaneInner vValue change : "+scrollPaneInner.getVvalue());
           }
       });
       scrollPaneInner.setOnScroll(new EventHandler<ScrollEvent>() {
           @Override public void handle(ScrollEvent event) {
               System.out.println("<><><> scrollPaneInner got a ScrollEvent : "+event);
               scrolled = true;
           }
       });
       scrollPaneInner.setContent(rect);

       Pane pOuter = new Pane();
       pOuter.setPrefWidth(600);
       pOuter.setPrefHeight(600);
       pOuter.getChildren().add(scrollPaneInner);

       final ScrollPane scrollPaneOuter = new ScrollPane();
       //scrollPaneOuter.setStyle("-fx-skin: com.sun.javafx.scene.control.skin.ScrollPaneSkin;");

       scrollPaneOuter.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
       scrollPaneOuter.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
       scrollPaneOuter.setPrefWidth(500);
       scrollPaneOuter.setPrefHeight(500);
       scrollPaneOuter.setPannable(true);
       scrollPaneOuter.vvalueProperty().addListener(new ChangeListener() {
           @Override public void changed(ObservableValue observable, Object oldBounds, Object newBounds) {
               System.out.println("<><><><> scrollPaneOuter vValue change : "+scrollPaneOuter.getVvalue());
           }
       });
       scrollPaneOuter.setOnScroll(new EventHandler<ScrollEvent>() {
           @Override public void handle(ScrollEvent event) {
               System.out.println("<><><><><> scrollPaneOuter got a ScrollEvent : "+event);
               scrolled = true;
           }
       });
       scrollPaneOuter.setContent(pOuter);



       Scene scene = new Scene(new Group(), 700, 700);
       ((Group) scene.getRoot()).getChildren().clear();
       ((Group) scene.getRoot()).getChildren().add(scrollPaneOuter);
       scrolled = false;

       Stage stage = new Stage();
       stage.setScene(scene);
       stage.show();


       System.out.println(">>> spInner vv! : "+scrollPaneInner.getVvalue());
       System.out.println(">>> spOuter vv! : "+scrollPaneOuter.getVvalue());

       Event.fireEvent(rect, 
             ScrollEvent.impl_scrollEvent(
                         0.0, -50.0,
                         ScrollEvent.HorizontalTextScrollUnits.NONE, 10.0,
                         ScrollEvent.VerticalTextScrollUnits.NONE, 10.0,
                         50, 50,
                         50, 50,
                         false, false, false, false));


       System.out.println("<<< spInner vv! : "+scrollPaneInner.getVvalue());
       System.out.println("<<< spOuter vv! : "+scrollPaneOuter.getVvalue());
       /*
       ** did it work?
       */
       //assertTrue(scrollPaneInner.getVvalue() > 0.0);

   }
}
