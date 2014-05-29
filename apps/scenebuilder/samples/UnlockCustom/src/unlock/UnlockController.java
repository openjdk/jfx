/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package unlock;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.util.Duration;

/**
 * The controller for our 'Unlock' application, see 'Unlock.fxml'.
 * This class has all the logic to open the theater's doors using JavaFX
 * transitions.
 */
public final class UnlockController {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;
    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;
    @FXML
    private Node root;
    @FXML // fx:id="pad"
    private Keypad pad; // Value injected by FXMLLoader
    @FXML // fx:id="error"
    private Rectangle error; // Value injected by FXMLLoader
    @FXML // fx:id="lock"
    private Button lock; // Value injected by FXMLLoader
    @FXML // fx:id="okleft"
    private Rectangle okleft; // Value injected by FXMLLoader
    @FXML // fx:id="okright"
    private Rectangle okright; // Value injected by FXMLLoader
    @FXML // fx:id="unlockbottom"
    private Rectangle unlockbottom; // Value injected by FXMLLoader
    @FXML // fx:id="unlocktop"
    private Rectangle unlocktop; // Value injected by FXMLLoader
    private boolean open = false;

    private final static class HeightTransition extends Transition {

        final Rectangle node;
        final double height;

        public HeightTransition(Duration duration, Rectangle node) {
            this(duration, node, node.getHeight());
        }

        public HeightTransition(Duration duration, Rectangle node, double height) {
            this.node = node;
            this.height = height;
            this.setCycleDuration(duration);
        }

        public Duration getDuration() {
            return getCycleDuration();
        }

        @Override
        protected void interpolate(double frac) {
            this.node.setHeight((1.0 - frac) * height);
        }
    }

    private final static class HeightAndLayoutYTransition extends Transition {

        final Rectangle node;
        final double height;
        final double layoutY;

        public HeightAndLayoutYTransition(Duration duration, Rectangle node) {
            this(duration, node, node.getHeight(), node.getLayoutY());
        }

        public HeightAndLayoutYTransition(Duration duration, Rectangle node, double height, double layoutY) {
            this.node = node;
            this.height = height;
            this.layoutY = layoutY;
            this.setCycleDuration(duration);
        }

        public Duration getDuration() {
            return getCycleDuration();
        }

        @Override
        protected void interpolate(double frac) {
            this.node.setHeight((1.0 - frac) * height);
            this.node.setLayoutY((1.0 + frac) * layoutY);
        }
    }

    private final static class WidthTransition extends Transition {

        final Rectangle node;
        final double width;

        public WidthTransition(Duration duration, Rectangle node) {
            this(duration, node, node.getWidth());
        }

        public WidthTransition(Duration duration, Rectangle node, double width) {
            this.node = node;
            this.width = width;
            this.setCycleDuration(duration);
        }

        public Duration getDuration() {
            return getCycleDuration();
        }

        @Override
        protected void interpolate(double frac) {
            this.node.setWidth((1.0 - frac) * width);
        }
    }
    
    private final static class WidthAndLayoutXTransition extends Transition {

        final Rectangle node;
        final double width;
        final double layoutX;

        public WidthAndLayoutXTransition(Duration duration, Rectangle node) {
            this(duration, node, node.getWidth(), node.getLayoutX());
        }

        public WidthAndLayoutXTransition(Duration duration, Rectangle node, double width, double layoutX) {
            this.node = node;
            this.width = width;
            this.layoutX = layoutX;
            this.setCycleDuration(duration);
        }

        public Duration getDuration() {
            return getCycleDuration();
        }

        @Override
        protected void interpolate(double frac) {
            this.node.setWidth((1.0 - frac) * width);
            this.node.setLayoutX((1.0 + frac) * layoutX);
        }
    }

    private FadeTransition fadeOut(final Duration duration, final Node node) {
        final FadeTransition fadeOut = new FadeTransition(duration, node);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                node.setVisible(false);
            }
        });
        return fadeOut;
    }

    // Handler for Button[fx:id="lock"] onAction
    @FXML
    void unlockPressed(ActionEvent event) {
        // handle the event here
        lock.setDisable(true);
        root.requestFocus();

        final FadeTransition fadeLockButton = fadeOut(Duration.valueOf("1s"), lock);
        final HeightTransition openLockTop = new HeightTransition(Duration.valueOf("2s"), unlocktop);
        openLockTop.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                unlocktop.setVisible(false);
                unlocktop.setHeight(openLockTop.height);
            }
        });

        final HeightAndLayoutYTransition openLockBottom = new HeightAndLayoutYTransition(Duration.valueOf("2s"), unlockbottom);
        openLockBottom.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                unlockbottom.setVisible(false);
                unlockbottom.setHeight(openLockBottom.height);
                unlockbottom.setLayoutY(openLockBottom.layoutY);
            }
        });
        final ParallelTransition openLock = new ParallelTransition(openLockTop, openLockBottom);
        final SequentialTransition unlock = new SequentialTransition(fadeLockButton, openLock);
        unlock.play();
    }

    private void resetVisibility() {
        pad.setOpacity(1.0);
        lock.setOpacity(1.0);
        pad.setVisible(true);
        lock.setVisible(true);
        lock.setDisable(false);
        okright.setVisible(true);
        okleft.setVisible(true);
        unlocktop.setVisible(true);
        unlockbottom.setVisible(true);
    }

    // Handler for AnchorPane[id="AnchorPane"] onKeyPressed
    private void keyboardKeyPressed(KeyEvent event) {
        if (" ".equals(event.getCharacter())) {
            // When "Hello World" is displayed (the theater is open) - pressing
            // the space bar will reinitialize the application.
            if (open) {
                // Reinitializing the application...
                open = false;
                resetVisibility();
                lock.requestFocus();
            }
        }
    }
    
    private final class ValidateCallback implements Callback<String, Boolean> {
        private ValidateCallback() {    
        }
        @Override
        public Boolean call(String param) {
            final boolean accessGranted = "1234".equals(param);
            if (accessGranted) {
                grantAccess();
            } else {
                rejectAccess();
            }
            return accessGranted;
        }
        
    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert error != null : "fx:id=\"error\" was not injected: check your FXML file 'Unlock.fxml'.";
        assert lock != null : "fx:id=\"lock\" was not injected: check your FXML file 'Unlock.fxml'.";
        assert okleft != null : "fx:id=\"okleft\" was not injected: check your FXML file 'Unlock.fxml'.";
        assert okright != null : "fx:id=\"okright\" was not injected: check your FXML file 'Unlock.fxml'.";
        assert pad != null : "fx:id=\"pad\" was not injected: check your FXML file 'Unlock.fxml'.";
        assert root != null : "fx:id=\"root\" was not injected: check your FXML file 'Unlock.fxml'.";
        assert unlockbottom != null : "fx:id=\"unlockbottom\" was not injected: check your FXML file 'Unlock.fxml'.";
        assert unlocktop != null : "fx:id=\"unlocktop\" was not injected: check your FXML file 'Unlock.fxml'.";

        // Set pin validation for the keypad
        pad.setValidateCallback(new ValidateCallback());
        
        // Reset visibility and opacity of nodes - useful if you left your
        // FXML in a 'bad' state
        resetVisibility();

        // Add event handler to the root - used to handle the space bar key at the
        // end of the application
        root.addEventHandler(KeyEvent.KEY_TYPED, new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                if (event instanceof KeyEvent) {
                    keyboardKeyPressed((KeyEvent) event);
                }
            }
        });
    }

    private void grantAccess() {
        root.requestFocus();
        FadeTransition fadeOutPad = fadeOut(Duration.valueOf("1s"), pad);

        final WidthTransition openOkLeft = 
                new WidthTransition(Duration.valueOf("2s"), okleft);
        openOkLeft.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                okleft.setVisible(false);
                okleft.setWidth(openOkLeft.width);
            }
        });
        
        final WidthAndLayoutXTransition openOkRight = 
                new WidthAndLayoutXTransition(openOkLeft.getDuration(), okright);
        openOkRight.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                okright.setVisible(false);
                okright.setWidth(openOkRight.width);
                okright.setLayoutX(openOkRight.layoutX);
            }
        });

        final ParallelTransition openOk =
                new ParallelTransition(openOkLeft, openOkRight);

        final SequentialTransition okTrans =
                new SequentialTransition(fadeOutPad, openOk);
        okTrans.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                open = true;
                root.requestFocus();
            }
        });
        okTrans.play();
    }

    private void rejectAccess() {
        FadeTransition errorTrans = new FadeTransition(Duration.valueOf("500ms"), error);
        errorTrans.setFromValue(0.0);
        errorTrans.setToValue(1.0);
        errorTrans.setCycleCount(2);
        errorTrans.setAutoReverse(true);
        errorTrans.play();
    }
}
