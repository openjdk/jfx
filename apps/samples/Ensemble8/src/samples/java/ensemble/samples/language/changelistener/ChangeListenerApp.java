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
package ensemble.samples.language.changelistener;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.Lighting;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 * A sample that demonstrates how to add or remove a change listener on a node
 * (for example, a Rectangle node) for some property (for example,
 * Rectangle.hover). Once you add a listener, the text field  shows the hover
 * property change.
 *
 * @sampleName ChangeListener
 * @preview preview.png
 * @see javafx.beans.value.ChangeListener
 * @see javafx.beans.InvalidationListener
 * @see javafx.beans.value.ObservableValue
 * @embedded
 */
public class ChangeListenerApp extends Application {
    // Simple filled rectangle

    Rectangle rect1 = new Rectangle(25, 25, 40, 40);
    // Stroked rectangle
    Rectangle rect2 = new Rectangle(135, 25, 40, 40);

    public Parent createContent() {
        HBox outerHbox = new HBox();
        outerHbox.setAlignment(Pos.CENTER);
        VBox vbox = new VBox(10);
        vbox.setPrefWidth(200);
        // create rectangle
        final Rectangle rect = new Rectangle(150, 0, 60, 60);
        rect.setFill(Color.DODGERBLUE);
        rect.setEffect(new Lighting());
        //create text field for showing actual message
        final Text text = new Text(0, 0, "Add a hover listener");
        text.setStyle("-fx-font-size: 22;");
        text.setTextOrigin(VPos.TOP);
        text.setTextAlignment(TextAlignment.CENTER);
        // create listener
        final InvalidationListener hoverListener = (Observable ov) -> {
            if (rect.isHover()) {
                text.setText("hovered");
            } else {
                text.setText("not hovered");
            }
        };
        //create button for adding listener
        Button buttonAdd = new Button("Add listener");
        buttonAdd.setPrefSize(140, 18);
        buttonAdd.setOnAction((ActionEvent t) -> {
            //add the listener on property hover
            rect.hoverProperty().addListener(hoverListener);
            text.setText("listener added");
        });
        //create a button for removing the listener
        Button buttonRemove = new Button("Remove listener");
        buttonRemove.setPrefSize(140, 18);
        buttonRemove.setOnAction((ActionEvent t) -> {
            //remove the listener
            rect.hoverProperty().removeListener(hoverListener);
            text.setText("listener removed");
        });
        // show all nodes
        vbox.getChildren().addAll(text, buttonAdd, buttonRemove);
        outerHbox.getChildren().addAll(vbox, rect);
        outerHbox.setPadding(new Insets(5,5,5,5));
        return outerHbox;
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
