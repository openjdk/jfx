/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates.
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
package ensemble.samples.controls.spinner;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;

import java.util.Arrays;

/**
 * A sample that demonstrates the Spinner control.
 *
 * @sampleName SpinnerApp
 * @preview preview.png
 * @see javafx.scene.control.Spinner
 * @docUrl http://www.oracle.com/pls/topic/lookup?ctx=javase80&id=JFXUI336 Using JavaFX UI Controls
 *
 * @related /Controls/DialogApp
 */
public class SpinnerApp extends Application {

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    public Parent createContent() {

        String[] styles = {
            "spinner",  // defaults to arrows on right stacked vertically
            Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL,
            Spinner.STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL,
            Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL,
            Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL,
            Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL
        };

        TilePane tilePane = new TilePane();
        tilePane.setPrefColumns(6);     //preferred columns
        tilePane.setPrefRows(3);        //preferred rows
        tilePane.setHgap(20);
        tilePane.setVgap(30);

        Pane root = new Pane();
        root.setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        root.setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);

        for (int i = 0; i < styles.length; i++) {
            /* Integer spinners */
            SpinnerValueFactory svf =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99);
            Spinner sp = new Spinner();
            sp.setValueFactory(svf);
            sp.getStyleClass().add(styles[i]);
            sp.setPrefWidth(80);
            tilePane.getChildren().add(sp);
        }

        for (int i = 0; i < styles.length; i++) {
            /* Double spinners */
            SpinnerValueFactory svf =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1.0,
                                                                  0.5, 0.01);
            Spinner sp = new Spinner();
            sp.setValueFactory(svf);
            sp.getStyleClass().add(styles[i]);
            sp.setPrefWidth(90);
            tilePane.getChildren().add(sp);
        }

        for (int i = 0; i < styles.length; i++) {
            /* String spinners */
            ObservableList<String> items =
                FXCollections.observableArrayList("Grace", "Matt", "Katie");
            SpinnerValueFactory svf =
                new SpinnerValueFactory.ListSpinnerValueFactory<>(items);
            Spinner sp = new Spinner();
            sp.setValueFactory(svf);
            sp.setPrefWidth(100);
            sp.getStyleClass().add(styles[i]);
            tilePane.getChildren().add(sp);
        }

        root.getChildren().add(tilePane);
        return root;
    }
}
