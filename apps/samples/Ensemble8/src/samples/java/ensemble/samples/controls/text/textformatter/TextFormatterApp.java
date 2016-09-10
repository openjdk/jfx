/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
