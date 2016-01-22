
/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates.
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
package ensemble.samples.graphics2d.calc;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.paint.Color;


/**
 * A calculator that performs simple math exercises.
 *
 * @sampleName Calculator
 * @preview preview.png
 *
 * @see javafx.application.Application
 * @see javafx.stage.Stage
 * @see javafx.scene.Scene
 * @see javafx.scene.Group
 * @see javafx.scene.Parent
 * @see javafx.scene.paint.Color
 * @see javafx.scene.paint.CycleMethod
 * @see javafx.scene.paint.LinearGradient
 * @see javafx.scene.paint.Stop
 * @see javafx.scene.input.KeyEvent
 * @see javafx.scene.input.MouseEvent
 * @see javafx.scene.shape.Rectangle
 * @see javafx.scene.text.Font
 * @see javafx.scene.text.Text
 * @see javafx.event.EventHandler
 * @see javafx.geometry.VPos
 * @embedded
 */

public class CalculatorApp extends Application {


    public Parent createContent() {
        final Calculator calculator = new Calculator();
        return calculator;
    }

    @Override public void start(Stage primaryStage) throws Exception {
        primaryStage.setResizable(false);
        final Scene scene = new Scene(new Group());
        scene.setRoot(createContent());
        primaryStage.setScene(scene);
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
