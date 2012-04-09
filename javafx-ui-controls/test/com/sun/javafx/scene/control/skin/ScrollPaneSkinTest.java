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
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
              ScrollEvent.impl_scrollEvent(ScrollEvent.SCROLL,
                          0.0, -50.0, 0.0, -50.0,
                          ScrollEvent.HorizontalTextScrollUnits.NONE, 10.0,
                          ScrollEvent.VerticalTextScrollUnits.NONE, 10.0,
                          0,
                          50, 50,
                          50, 50,
                          false, false, false, false, true, false));

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
                         true, false, false, false,
                         MouseEvent.MOUSE_CLICKED));

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
        assertTrue(scene.getImpl_focusOwner() == scrollPane);
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

        scrollPane.setPrefSize(30, 50);
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
    ** check if ScrollBars appear if fitToHeight is true but height is < minHeight
    */
    @Test public void checkIfScrollbarsWhenFitToWidthAndWidthLessMin() {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        StackPane sp = new StackPane();
        sp.setPrefWidth(80);
        sp.setPrefHeight(80);
        sp.setMinWidth(40);
        sp.setMinHeight(40);

        scrollPane.setPrefSize(50, 30);
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
        assertTrue(skin.isVSBarVisible() & !skin.isHSBarVisible());
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

        scrollPane.setPrefSize(30, 30);
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
        assertTrue(skin.isVSBarVisible() & skin.isHSBarVisible());
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

        double skinWidth = skin.getWidth();
        double vsbPosAndWidth = skin.getVsbX()+skin.getVsbWidth()+(skin.getInsets().getRight() - skin.getPadding().getRight());
        assertEquals(skinWidth,  vsbPosAndWidth, 0.1);

        double skinHeight = skin.getHeight();
        double hsbPosAndHeight = skin.getHsbY()+skin.getHsbHeight()+(skin.getInsets().getBottom() - skin.getPadding().getBottom());
        assertEquals(skinHeight,  hsbPosAndHeight, 0.1);

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

        boolean isHSBarVisible() {
            return hsb.isVisible();
        }

        boolean isVSBarVisible() {
            return vsb.isVisible();
        }
        double getVsbX() {
            return vsb.getLayoutX();
        }
        double getVsbWidth() {
            return vsb.getWidth();
        }
        double getHsbY() {
            return hsb.getLayoutY();
        }
        double getHsbHeight() {
            return hsb.getHeight();
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
                    false, false, false, type);

            return event;
        }
    }
}
