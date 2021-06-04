/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates.
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
package ensemble.samples.controls.text.bidi;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * Demonstrates bi-directional text.
 *
 * @sampleName Bidi
 * @preview preview.png
 * @docUrl http://docs.oracle.com/javase/8/javafx/user-interface-tutorial/text.htm#JFXUI734 Using JavaFX Text
 * @playground text1.strikethrough (name="He said... strikethrough")
 * @playground text1.underline (name="He said... underline")
 * @playground text1.fill (name="He said... fill")
 * @playground text1.rotate (name="He said... rotate", min=-180, max=180)
 * @playground text1.translateX (name="He said... translateX")
 * @playground text1.translateY (name="He said... translateY")
 * @playground text2.strikethrough (name="...to me. strikethrough")
 * @playground text2.underline (name="...to me. underline")
 * @playground text2.fill (name="...to me. fill")
 * @playground text2.rotate (name="...to me. rotate", min=-180, max=180)
 * @playground text2.translateX (name="...to me. translateX")
 * @playground text2.translateY (name="...to me. translateY")
 * @see javafx.scene.text.Text
 * @see javafx.scene.text.TextFlow
 * @embedded
 *
 * @related /Controls/Text/Advanced Label
 * @related /Controls/Text/Inset Text
 * @related /Controls/Text/Search Box
 * @related /Controls/Text/Simple Label
 * @related /Controls/Text/Text Field
 * @related /Controls/Text/TextFlow
 * @related /Controls/Text/Text Formatter
 * @related /Controls/Text/Text Validator
 */
public class BidiApp extends Application {

    Text text1;
    Text text2;

    public Parent createContent() {
        Font font = new Font("Tahoma", 48);
        text1 = new Text("He said \u0627\u0644\u0633\u0644\u0627\u0645");
        text1.setFill(Color.RED);
        text1.setFont(font);
        text2 = new Text(" \u0639\u0644\u064a\u0643\u0645 to me.");
        text2.setFill(Color.BLUE);
        text2.setFont(font);

        return new Group(new TextFlow(text1, text2));
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
