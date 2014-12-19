/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import javafx.util.converter.FormatStringConverter;

import java.text.NumberFormat;

public class HelloTextFieldWithTextFormatter extends Application{

    //Formats to format and parse numbers
    private TextFormatter<Number> amountFormatter;
    private TextFormatter<Number> percentFormatter;
    private TextFormatter<Number> periodFormatter;
    private TextFormatter<Number> paymentFormatter;

    @Override
    public void start(Stage stage) throws Exception {
        GridPane root = new GridPane();
        root.setHgap(10);
        root.setVgap(10);

        RowConstraints c = new RowConstraints();
        c.setValignment(VPos.BASELINE);
        root.getRowConstraints().addAll(c, c, c, c);

        Label label = new Label("Loan amount: ");
        root.add(label, 0, 0);
        label = new Label("APR (%): ");
        root.add(label, 0, 1);
        label = new Label("Years: ");
        root.add(label, 0, 2);
        label = new Label("Monthly payment: ");
        root.add(label, 0, 3);

        setUpFormats();


        TextField amountField = new TextField();
        amountField.setTextFormatter(amountFormatter);
        root.add(amountField, 1, 0);

        TextField percentField = new TextField();
        percentField.setTextFormatter(percentFormatter);
        root.add(percentField, 1, 1);

        TextField periodField = new TextField();
        periodField.setTextFormatter(periodFormatter);
        root.add(periodField, 1, 2);

        TextField paymentField = new TextField();
        paymentField.setTextFormatter(paymentFormatter);
        paymentField.setEditable(false);
        root.add(paymentField, 1, 3);

        InvalidationListener valueListener = (o) -> paymentFormatter.setValue(
                computePayment(amountFormatter.getValue().doubleValue(), percentFormatter.getValue().doubleValue(), periodFormatter.getValue().intValue()));

        amountFormatter.valueProperty().addListener(valueListener);
        percentFormatter.valueProperty().addListener(valueListener);
        periodFormatter.valueProperty().addListener(valueListener);

        valueListener.invalidated(amountFormatter.valueProperty());

        stage.setScene(new Scene(root));

        stage.show();
    }

    //Compute the monthly payment based on the loan amount,
    //APR, and length of loan.
    private static double computePayment(double loanAmt, double rate, int numPeriods) {
        double I, partial1, denominator, answer;

        numPeriods *= 12;        //get number of months
        if (rate > 0.01) {
            I = rate / 100.0 / 12.0;         //get monthly rate from annual
            partial1 = Math.pow((1 + I), (0.0 - numPeriods));
            denominator = (1 - partial1) / I;
        } else { //rate ~= 0
            denominator = numPeriods;
        }

        answer = (-1 * loanAmt) / denominator;
        return answer;
    }

    private void setUpFormats() {

        final FormatStringConverter<Number> numberSC = new FormatStringConverter<>(NumberFormat.getNumberInstance());
        amountFormatter = new TextFormatter<>(numberSC);
        amountFormatter.setValue(10000d);


        final NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(3);
        final FormatStringConverter<Number> percentSC = new FormatStringConverter<Number>(format);
        percentFormatter = new TextFormatter<>(percentSC);
        percentFormatter.setValue(7.5);

        periodFormatter = new TextFormatter<>(numberSC);
        periodFormatter.setValue(30);


        final FormatStringConverter<Number> paymentSC = new FormatStringConverter<>(NumberFormat.getCurrencyInstance());
        paymentFormatter = new TextFormatter<>(paymentSC);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
