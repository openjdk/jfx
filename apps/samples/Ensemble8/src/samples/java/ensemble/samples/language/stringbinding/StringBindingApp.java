/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates.
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
package ensemble.samples.language.stringbinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javafx.application.Application;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * A sample that demonstrates how to bind text properties so the
 * value of the bound property is updated automatically when the value
 * of the original property is changed.
 *
 * @sampleName String Binding
 * @preview preview.png
 * @see javafx.beans.binding.StringBinding
 * @see javafx.scene.control.TextField
 * @see javafx.scene.control.Label
 * @embedded
 */
public class StringBindingApp extends Application {

    public Parent createContent() {
        final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        format.setLenient(false);
        final TextField dateField = new TextField();
        dateField.setPromptText("Enter a birth date");
        dateField.setMaxHeight(TextField.USE_PREF_SIZE);
        dateField.setMaxWidth(TextField.USE_PREF_SIZE);

        Label label = new Label();
        label.setWrapText(true);
        label.textProperty().bind(new StringBinding() {
            {
                bind(dateField.textProperty());
            }

            @Override
            protected String computeValue() {
                try {
                    Date date = format.parse(dateField.getText());
                    Calendar c = Calendar.getInstance();
                    c.setTime(date);

                    Date today = new Date();
                    Calendar c2 = Calendar.getInstance();
                    c2.setTime(today);

                    if (c.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) - 1
                            && c.get(Calendar.YEAR) == c2.get(Calendar.YEAR)) {
                        return "You were born yesterday";
                    } else if (c.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
                               && c.get(Calendar.YEAR) == c2.get(Calendar.YEAR)) {
                        return "You were born today";
                    } else {
                        return "You were born " + format.format(date);
                    }
                } catch (Exception e) {
                    return "Enter your valid birth date (mm/dd/yyyy)";
                }
            }
        });

        VBox vBox = new VBox(7);
        vBox.setPadding(new Insets(12));
        vBox.getChildren().addAll(label, dateField);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
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
