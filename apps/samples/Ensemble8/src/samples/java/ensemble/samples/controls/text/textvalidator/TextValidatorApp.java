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
package ensemble.samples.controls.text.textvalidator;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * A sample that demonstrates text validation. If the value in the TextField is
 * a small number, the field becomes yellow. If the value in the TextField is
 * not a number, the field becomes red.
 *
 * @sampleName Text Validator
 * @preview preview.png
 * @see javafx.scene.control.TextField
 * @related /Controls/Text/Text Field
 */
public class TextValidatorApp extends Application {

    public Parent createContent() {
        String validatorCss = TextValidatorApp.class.getResource("Validators.css").toExternalForm();

        TextField dateField = new TextField();
        dateField.setPromptText("Enter a Large Number");
        dateField.setMaxHeight(TextField.USE_PREF_SIZE);

        TextInputValidatorPane<TextField> pane = new TextInputValidatorPane<TextField>();
        pane.setContent(dateField);
        pane.setValidator((TextField control) -> {
            try {
                String text = control.getText();
                if (text == null || text.trim().equals("")) {
                    return null;
                }
                double d = Double.parseDouble(text);
                if (d < 1000) {
                    return new ValidationResult("Should be > 1000", ValidationResult.Type.WARNING);
                }
                return null; // succeeded
            } catch (Exception e) {
                // failed
                return new ValidationResult("Bad number", ValidationResult.Type.ERROR);
            }
        });

        StackPane rootSP = new StackPane();
        rootSP.setPadding(new Insets(12));
        rootSP.getChildren().add(pane);
        pane.getStylesheets().add(validatorCss);
        return rootSP;
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
