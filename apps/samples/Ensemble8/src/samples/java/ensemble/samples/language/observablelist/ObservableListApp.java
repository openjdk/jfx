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
package ensemble.samples.language.observablelist;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 * A sample that demonstrates the ObservableList interface, which extends the
 * java.util.List interface. Click the button to change an integer to a new
 * random number in a random position in the list. Once you add a listener,
 * the index of the changed number is displayed above the buttons.
 *
 * @sampleName ObservableList
 * @preview preview.png
 * @see javafx.beans.value.ChangeListener
 * @see javafx.collections.FXCollections
 * @see javafx.collections.ListChangeListener
 * @see javafx.collections.ObservableList
 * @embedded
 */
public class ObservableListApp extends Application {

    public Parent createContent() {
        //create some list with integers
        final List<Integer> listData = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            listData.add(i);
        }

        //create observable list from this list of integers by static method of FXCollections class
        final ObservableList<Integer> list = FXCollections.<Integer>observableList(listData);

        //create text for showing observable list content
        final Text textList = new Text(0, 0, list.toString());
        textList.setStyle("-fx-font-size: 16;");
        textList.setTextOrigin(VPos.TOP);
        textList.setTextAlignment(TextAlignment.CENTER);

        //create text field for showing  message
        final Text textMessage = new Text(0, 0, "Add a listener");
        textMessage.setStyle("-fx-font-size: 16;");
        textMessage.setTextOrigin(VPos.TOP);
        textMessage.setTextAlignment(TextAlignment.CENTER);

        //create button for adding random integer to random position in observable list
        Button buttonAddNumber = new Button("Replace random integer");
        buttonAddNumber.setPrefSize(190, 45);
        buttonAddNumber.setOnAction((ActionEvent t) -> {
            int randomIndex = (int) (Math.round(Math.random() * (list.size() - 1)));
            int randomNumber = (int) (Math.round(Math.random() * 10));
            list.set(randomIndex, randomNumber);
            //actualise content of the text to see the result
            textList.setText(list.toString());
        });

        //create button for adding listener
        Button buttonAdd = new Button("Add list listener");
        buttonAdd.setPrefSize(190, 45);
        final ListChangeListener<Integer> listener = (ListChangeListener.Change<? extends Integer> c) -> {
            while (c.next()) {
                textMessage.setText("replacement on index " + c.getFrom());
            }
        };

        buttonAdd.setOnAction((ActionEvent t) -> {
            list.addListener(listener);
            textMessage.setText("listener added");
        });

        //create a button for removing the listener
        Button buttonRemove = new Button("Remove list listener");
        buttonRemove.setPrefSize(190, 45);
        buttonRemove.setOnAction((ActionEvent t) -> {
            //remove the listener
            list.removeListener(listener);
            textMessage.setText("listener removed");
        });

        VBox vBoxTop = new VBox(10);
        vBoxTop.setAlignment(Pos.CENTER);

        VBox vBoxBottom = new VBox();
        vBoxBottom.setAlignment(Pos.CENTER);
        vBoxBottom.setSpacing(10);

        VBox outerVBox = new VBox(10);
        outerVBox.setAlignment(Pos.CENTER);

        vBoxTop.getChildren().addAll(textMessage, buttonAdd, buttonRemove);
        vBoxBottom.getChildren().addAll(buttonAddNumber, textList);
        outerVBox.getChildren().addAll(vBoxTop, vBoxBottom);
        return outerVBox;
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
