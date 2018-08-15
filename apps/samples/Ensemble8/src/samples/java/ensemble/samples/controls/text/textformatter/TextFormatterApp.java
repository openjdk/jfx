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
package ensemble.samples.controls.text.textformatter;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import javafx.util.converter.FormatStringConverter;

import java.text.NumberFormat;

/**
 * Demonstrates a TextField control with a TextFormatter that filters and formats the content.
 *
 * @sampleName Text Formatter
 * @preview preview.png
 * @docUrl http://docs.oracle.com/javase/8/javafx/user-interface-tutorial/text.htm#JFXUI734 Using JavaFX Text
 * @see javafx.scene.control.TextFormatter
 * @see javafx.scene.control.TextField
 * @see javafx.scene.control.TextInputControl
 * @see javafx.util.converter.FormatStringConverter
 * @playground price (min=0, max=10000)
 * @embedded
 *
 * @related /Controls/Text/Advanced Label
 * @related /Controls/Text/Bidi
 * @related /Controls/Text/Inset Text
 * @related /Controls/Button/Graphic Button
 * @related /Controls/Text/Search Box
 * @related /Controls/Text/Simple Label
 * @related /Controls/Text/Text Field
 * @related /Controls/Text/TextFlow
 * @related /Controls/Text/Text Validator
 */
public class TextFormatterApp extends Application{

    private final DoubleProperty price = new SimpleDoubleProperty(1200.555);
    public final DoubleProperty priceProperty() {
        return price;
    }

    public Parent createContent() {
        final NumberFormat currencyInstance = NumberFormat.getCurrencyInstance();
        String symbol = currencyInstance.getCurrency().getSymbol();
        FormatStringConverter<Number> converter =
            new FormatStringConverter<>(currencyInstance);
        TextFormatter<Number> formatter = new TextFormatter<>(converter);
        formatter.valueProperty().bindBidirectional(price);
        final TextField text = new TextField();
        text.setTextFormatter(formatter);
        text.setMaxSize(140, TextField.USE_COMPUTED_SIZE);
        return text;
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
