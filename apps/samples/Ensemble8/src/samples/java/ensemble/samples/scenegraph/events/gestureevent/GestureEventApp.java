/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.samples.scenegraph.events.gestureevent;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * A sample that demonstrates various multi-touch gesture events and their usage.
 * Tap, scroll and pan to move the rectangle. Use pinch close and open zoom
 * gestures to resize it. User rotate touch gesture to rotate it.
 * Swipe events are reported but not wired to any other behavior.
 * All events are logged to the console. You can also observe inertia scrolling.
 *
 * @sampleName Gesture Event
 * @preview preview.png
 * @related /Scenegraph/Events/MouseEvent
 * @related /Scenegraph/Events/Multi-Touch
 * @see javafx.scene.input.RotateEvent
 * @see javafx.scene.input.ScrollEvent
 * @see javafx.scene.input.SwipeEvent
 * @see javafx.scene.input.TouchEvent
 * @see javafx.scene.input.ZoomEvent
 * @see javafx.scene.Cursor
 * @see javafx.collections.FXCollections
 * @see javafx.collections.ObservableList
 * @see javafx.event.EventHandler
 * @see javafx.scene.control.ListView
 * @see javafx.scene.layout.VBox
 * @see javafx.scene.paint.Color
 * @see javafx.scene.paint.CycleMethod
 * @see javafx.scene.paint.LinearGradient
 * @see javafx.scene.paint.Stop
 * @see javafx.scene.shape.Rectangle
 * @see javafx.scene.layout.Pane
 * @embedded
 */
public class GestureEventApp extends Application {

    private final static int SAMPLE_SCREEN_WIDTH = 400;
    private final static int SAMPLE_SCREEN_HEIGHT = 380;
    private final static int CONSOLE_WIDTH = 400;
    private final static int CONSOLE_HEIGHT = 80;
    private final static int BORDER_HEIGHT = SAMPLE_SCREEN_HEIGHT - CONSOLE_HEIGHT;

    // some offset minus BORDER_HEIGHT so it's not off in the corner
    private final static int SMALL_REC_Y = 20 - BORDER_HEIGHT;

    //some offset so it's not off in the corner
    private final static int SMALL_REC_X = 30;

    //create a console for logging mouse events
    final ListView<String> console = new ListView<>();

    //create a observableArrayList of logged events that will be listed in console
    final ObservableList<String> consoleObservableList = FXCollections.observableArrayList();

    private String lastEvent = "";
    private int lastEventCount = 0;

    public Parent createContent() {
        //set up the console
        console.setItems(consoleObservableList);
        console.setPrefSize(CONSOLE_WIDTH, CONSOLE_HEIGHT);
        console.setMinSize(ListView.USE_PREF_SIZE, ListView.USE_PREF_SIZE);
        console.setMaxSize(ListView.USE_PREF_SIZE, ListView.USE_PREF_SIZE);
        VBox root = new VBox();
        root.setSpacing(2);
        root.setPrefSize(SAMPLE_SCREEN_WIDTH, SAMPLE_SCREEN_HEIGHT);
        root.setMinSize(SAMPLE_SCREEN_WIDTH, SAMPLE_SCREEN_HEIGHT);
        root.setMaxSize(SAMPLE_SCREEN_WIDTH, SAMPLE_SCREEN_HEIGHT);

        Rectangle border = new Rectangle(400, BORDER_HEIGHT);
        border.setStroke(Color.GRAY);
        border.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop[]{
                    new Stop(1, Color.rgb(156, 216, 255)),
                    new Stop(0, Color.rgb(156, 216, 255, 0.5))
                }));

        final Rectangle smallRec = createRectangle();
        smallRec.setTranslateX(SMALL_REC_X);
        smallRec.setTranslateY(SMALL_REC_Y);
        Pane box = new Pane();
        box.getChildren().addAll(border, smallRec);
        setEventListeners(root, smallRec, "From background--");
        root.getChildren().addAll(console, border, smallRec);

        return root;
    }

    private Rectangle createRectangle() {
        final Rectangle smallRec = new Rectangle(100, 100, 100, 100);
        LinearGradient gradient1 = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop[]{
                    new Stop(0, Color.ANTIQUEWHITE),
                    new Stop(1, Color.CORAL)
                });
        smallRec.setFill(gradient1);
        smallRec.setStroke(Color.BLACK);
        smallRec.setCursor(Cursor.HAND);
        setEventListeners(smallRec, smallRec, "From rectangle--");

        return smallRec;
    }

    private void showOnConsole(String text) {
        if (lastEvent.equals(text)) {
            lastEventCount++;
            consoleObservableList.set(consoleObservableList.size() - 1,
                    text + " (" + lastEventCount + " times)");
        } else {
            if (consoleObservableList.size() == 500) {
                consoleObservableList.remove(0);
            }
            consoleObservableList.add(text);
            console.scrollTo(consoleObservableList.size());
            lastEvent = text;
            lastEventCount = 1;
        }
    }

    private void setEventListeners(final Node listeningNode, final Rectangle rec, final String msgPrefix) {
        listeningNode.setOnSwipeDown((SwipeEvent se) -> {
            showOnConsole(msgPrefix + "SwipeDown event");
            se.consume();
        });

        listeningNode.setOnSwipeUp((SwipeEvent se) -> {
            showOnConsole(msgPrefix + "SwipeUp event");
            se.consume();
        });

        listeningNode.setOnSwipeLeft((SwipeEvent se) -> {
            showOnConsole(msgPrefix + "SwipeLeft event");
            se.consume();
        });

        listeningNode.setOnSwipeRight((SwipeEvent se) -> {
            showOnConsole(msgPrefix + "SwipeRight event");
            se.consume();
        });

        listeningNode.setOnTouchStationary((TouchEvent se) -> {
            showOnConsole(msgPrefix + "TouchStationary event");
            se.consume();
        });

        listeningNode.setOnScroll((ScrollEvent event) -> {
            // scroll amount
            double translateX = event.getDeltaX();
            double translateY = event.getDeltaY();

            if ((rec.getTranslateX() + translateX > 0) && (rec.getTranslateX() + translateX < 300)) {
                rec.setTranslateX(rec.getTranslateX() + translateX);
            }
            if ((rec.getTranslateY() + translateY > SMALL_REC_Y - 20) && (rec.getTranslateY() + translateY < 180 + SMALL_REC_Y)) {
                rec.setTranslateY(rec.getTranslateY() + translateY);
            }
            showOnConsole(msgPrefix + "Scroll event");
            event.consume();
        });

        listeningNode.setOnZoom((ZoomEvent event) -> {
            rec.setScaleX(rec.getScaleX() * event.getZoomFactor());
            rec.setScaleY(rec.getScaleY() * event.getZoomFactor());
            showOnConsole(msgPrefix + "Zoom event");
            event.consume();
        });

        listeningNode.setOnRotate((RotateEvent event) -> {
            rec.setRotate(rec.getRotate() + event.getAngle());
            showOnConsole(msgPrefix + "Rotate event");
            event.consume();
        });

        listeningNode.setOnScrollStarted((ScrollEvent event) -> {
            showOnConsole(msgPrefix + "Scroll started");
            event.consume();
        });

        listeningNode.setOnScrollFinished((ScrollEvent event) -> {
            showOnConsole(msgPrefix + "Scroll finished");
            event.consume();
        });

        listeningNode.setOnZoomStarted((ZoomEvent event) -> {
            showOnConsole(msgPrefix + "Zoom started");
            event.consume();
        });

        listeningNode.setOnZoomFinished((ZoomEvent event) -> {
            showOnConsole(msgPrefix + "Zoom finished");
            event.consume();
        });

        listeningNode.setOnRotationStarted((RotateEvent event) -> {
            showOnConsole(msgPrefix + "Rotation started");
            event.consume();
        });

        listeningNode.setOnRotationFinished((RotateEvent event) -> {
            showOnConsole(msgPrefix + "Rotation finished");
            event.consume();
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
