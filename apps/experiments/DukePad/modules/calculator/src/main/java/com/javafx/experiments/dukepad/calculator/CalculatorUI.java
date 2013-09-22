/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.calculator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The UI for the calculator.
 */
public class CalculatorUI extends Region {
    private enum Style { BLUE, ORANGE, DARK, LIGHT, EQUALS }
    private enum Operation { INCREASE, DECREASE, MULTIPLY, DIVIDE, DEFAULT }

    // Strings used in the app
    private static final String BUTTON_TEXT_MC = "mc";
    private static final String BUTTON_TEXT_MA = "m+";
    private static final String BUTTON_TEXT_MS = "m-";
    private static final String BUTTON_TEXT_MR = "mr";
    private static final String BUTTON_FREE = "C";
    private static final String BUTTON_EQUALS = "=";
    private static final String BUTTON_POINT = ".";
    private static final String SIGN_INCREASE = "+";
    private static final String SIGN_DECREASE = "-";
    private static final String SIGN_MULTIPLY = "x";
    private static final String SIGN_DIVIDE = "÷";
    private static final String SIGN_REVERT = "±";
    private static final String STRING_EMPTY = "";
    private static final String STRING_ZERO = "0";

    private Operation operationType = Operation.DEFAULT;
    private String firstNumber = STRING_EMPTY;
    private String secondNumber = STRING_EMPTY;
    private String memValue = STRING_EMPTY;
    private String operationSign = STRING_EMPTY;

    private boolean isFirstNumberInput = true;
    private boolean isReset = true;
    private boolean isException = false;

    private final TextField result = new TextField(STRING_ZERO);
    private final Button mcBtn = createButton(BUTTON_TEXT_MC, Style.BLUE);
    private final Button maBtn = createButton(BUTTON_TEXT_MA, Style.BLUE);
    private final Button msBtn = createButton(BUTTON_TEXT_MS, Style.BLUE);
    private final Button mrBtn = createButton(BUTTON_TEXT_MR, Style.BLUE);
    private final Button zeroBtn = createNumberButton("0", Style.LIGHT);
    private final Button oneBtn = createNumberButton("1", Style.LIGHT);
    private final Button twoBtn = createNumberButton("2", Style.LIGHT);
    private final Button threeBtn = createNumberButton("3", Style.LIGHT);
    private final Button fourBtn = createNumberButton("4", Style.LIGHT);
    private final Button fiveBtn = createNumberButton("5", Style.LIGHT);
    private final Button sixBtn = createNumberButton("6", Style.LIGHT);
    private final Button sevenBtn = createNumberButton("7", Style.LIGHT);
    private final Button eightBtn = createNumberButton("8", Style.LIGHT);
    private final Button nineBtn = createNumberButton("9", Style.LIGHT);
    private final Button changeSignBtn = createButton(SIGN_REVERT, Style.DARK);
    private final Button divideBtn = createButton(SIGN_DIVIDE, Style.DARK);
    private final Button multiplyBtn = createButton(SIGN_MULTIPLY, Style.DARK);
    private final Button decreaseBtn = createButton(SIGN_DECREASE, Style.DARK);
    private final Button increaseBtn = createButton(SIGN_INCREASE, Style.DARK);
    private final Button equalsBtn = createButton(BUTTON_EQUALS, Style.EQUALS);
    private final Button pointBtn = createButton(BUTTON_POINT, Style.LIGHT);
    private final Button cBtn = createButton(BUTTON_FREE, Style.ORANGE);

    public CalculatorUI() {
        CalculatorTheme.styleBackground(this);
        setPadding(new Insets(30,30,30,45));

        result.setEditable(false);
        result.setAlignment(Pos.BASELINE_RIGHT);
        result.setFocusTraversable(false);
        result.setEditable(false);
        CalculatorTheme.styleTextField(result);

        //Memory buttons section
        mcBtn.setOnAction(event -> handleMemCleanButtonClicked());
        maBtn.setOnAction(event -> handleMemAddClicked());
        msBtn.setOnAction(event -> handleMemSubtractButtonClicked());
        mrBtn.setOnAction(event -> handleMemRestoreButtonClicked());

        //Operation buttons section
        changeSignBtn.setOnAction(event -> handleSignRevertButtonClicked());
        divideBtn.setOnAction(event -> handleDivideButtonClicked());
        multiplyBtn.setOnAction(event -> handleMultiplyButtonClicked());
        decreaseBtn.setOnAction(event -> handleDecreaseButtonClicked());
        increaseBtn.setOnAction(event -> handleIncreaseButtonClicked());
        equalsBtn.setOnAction(event -> handleEqualsButtonClicked());
        pointBtn.setOnAction(event -> handlePointButtonClicked());

        //Other buttons section
        cBtn.setOnAction(event -> handleMemFreeButtonClicked());

        this.addEventHandler(MouseEvent.ANY, MouseEvent::consume);

        getChildren().addAll(result, mcBtn, maBtn, msBtn, mrBtn, zeroBtn, oneBtn, twoBtn, threeBtn, fourBtn,
                fiveBtn, sixBtn, sevenBtn, eightBtn, nineBtn, changeSignBtn, divideBtn, multiplyBtn,
                decreaseBtn, increaseBtn, equalsBtn, pointBtn, cBtn);
    }

    private Button createButton(String text, Style style) {
        Button button = new Button(text);
        button.setMaxHeight(1.7976931348623157E308);
        button.setMaxWidth(1.7976931348623157E308);
        button.setMnemonicParsing(false);
        switch (style) {
            case BLUE:   CalculatorTheme.styleBlueButton(button);   break;
            case ORANGE: CalculatorTheme.styleOrangeButton(button); break;
            case LIGHT:  CalculatorTheme.styleLightButton(button);  break;
            case DARK:   CalculatorTheme.styleDarkButton(button);   break;
            case EQUALS: CalculatorTheme.styleEqualsButton(button); break;
        }
        return button;
    }

    private Button createNumberButton(String text, Style style) {
        Button button = createButton(text, style);

        button.setOnAction(event -> {
            if(isReset) {
                firstNumber = STRING_EMPTY;
                result.setText(STRING_EMPTY);
                secondNumber = STRING_EMPTY;
                isReset = false;
                isFirstNumberInput = true;
                isException = false;
            }

            String value = button.getText();
            String res;
            if(isFirstNumberInput) {
                firstNumber += value;
                res = firstNumber;
            } else {
                secondNumber += value;
                res = firstNumber + operationSign + secondNumber;
            }
            result.setText(res);
        });

        return button;
    }

    @Override
    protected void layoutChildren() {
        final Insets insets = getInsets();
        final double x = insets.getLeft();
        final double y = insets.getTop();
        final double w = getWidth() - insets.getLeft() - insets.getRight();
        final double h = getHeight() - insets.getTop() - insets.getBottom();

        // There are 7 rows and 4 columns
        final int gap = 20;
        final double rowHeight = (h - (gap * 6)) / 7;
        final double colWidth = (w - (gap * 3)) / 4;
        final double rowIncrement = rowHeight + gap;
        final double colIncrement = colWidth + gap;

        // I'll use these instead of x & y to position stuff
        double row = y;
        //noinspection UnnecessaryLocalVariable
        double col1 = x;
        double col2 = col1 + colIncrement;
        double col3 = col2 + colIncrement;
        double col4 = col3 + colIncrement;

        // Text field first
        layout(result, col1, row, colWidth * 4 + gap * 3, rowHeight);
        row += rowIncrement;

        // Memory button row
        layout(mcBtn, col1, row, colWidth, rowHeight);
        layout(maBtn, col2, row, colWidth, rowHeight);
        layout(msBtn, col3, row, colWidth, rowHeight);
        layout(mrBtn, col4, row, colWidth, rowHeight);
        row += rowIncrement;

        // C, +/-, divide, and multiply row
        layout(cBtn, col1, row, colWidth, rowHeight);
        layout(changeSignBtn, col2, row, colWidth, rowHeight);
        layout(divideBtn, col3, row, colWidth, rowHeight);
        layout(multiplyBtn, col4, row, colWidth, rowHeight);
        row += rowIncrement;

        // 7, 8, 9, - row
        layout(sevenBtn, col1, row, colWidth, rowHeight);
        layout(eightBtn, col2, row, colWidth, rowHeight);
        layout(nineBtn, col3, row, colWidth, rowHeight);
        layout(decreaseBtn, col4, row, colWidth, rowHeight);
        row += rowIncrement;

        // 4, 5, 6, + row
        layout(fourBtn, col1, row, colWidth, rowHeight);
        layout(fiveBtn, col2, row, colWidth, rowHeight);
        layout(sixBtn, col3, row, colWidth, rowHeight);
        layout(increaseBtn, col4, row, colWidth, rowHeight);
        row += rowIncrement;

        // 1, 2, 3 row
        layout(oneBtn, col1, row, colWidth, rowHeight);
        layout(twoBtn, col2, row, colWidth, rowHeight);
        layout(threeBtn, col3, row, colWidth, rowHeight);
        // And the equals which is two rows high
        layout(equalsBtn, col4, row, colWidth, rowHeight + rowIncrement);
        row += rowIncrement;

        // 0 which is two columns wide, and .
        layout(zeroBtn, col1, row, colWidth + colIncrement, rowHeight);
        layout(pointBtn, col3, row, colWidth, rowHeight);
    }

    private void layout(Region r, double x, double y, double w, double h) {
        r.setLayoutX(x);
        r.setLayoutY(y);
        r.resize(w, h);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void impl_processCSS() {
        // No-op, no CSS for you! This is a JavaOne hack to speed up things.
    }

    private void setOperation(Operation type) {
        operationType = type;
        switch(type) {
            case INCREASE:
                operationSign = SIGN_INCREASE;
                break;
            case DECREASE:
                operationSign = SIGN_DECREASE;
                break;
            case DIVIDE:
                operationSign = SIGN_DIVIDE;
                break;
            case MULTIPLY:
                operationSign = SIGN_MULTIPLY;
                break;
            default:
                operationSign = STRING_EMPTY;
                break;
        }
    }

    private void handlePointButtonClicked() {
        if(isFirstNumberInput) {
            if(String.valueOf(firstNumber).contains(".")) {
                return;
            }
            firstNumber += ".";
            isReset = false;
            isFirstNumberInput = true;
        } else {
            if(String.valueOf(secondNumber).contains(".")){
                return;
            }
            secondNumber += ".";
        }
        result.setText(result.getText() + ".");
    }

    private void handleEqualsButtonClicked() {
        if (isException || isFirstNumberInput || secondNumber.equals(STRING_EMPTY)) return;
        String operationRes = STRING_EMPTY;
        try {
            BigDecimal first = BigDecimal.valueOf(Double.valueOf(firstNumber));
            BigDecimal second = BigDecimal.valueOf(Double.valueOf(secondNumber));
            switch (operationType) {
                case INCREASE:
                    operationRes = first.add(second).toPlainString();
                    break;
                case DECREASE:
                    operationRes = first.subtract(second).toPlainString();
                    break;
                case MULTIPLY:
                    operationRes = first.multiply(second).toPlainString();
                    break;
                case DIVIDE:
                    if (STRING_ZERO.equals(secondNumber)) {
                        throw new IllegalArgumentException("Cannot divide by zero");
                    } else {
                        operationRes = first.divide(second, 2, RoundingMode.HALF_DOWN).toPlainString();
                    }
                    break;
            }
            result.setText(operationRes);
            firstNumber = operationRes;
            isFirstNumberInput = true;
        } catch (IllegalArgumentException ex) {
            result.setText(ex.getMessage());
            isException = true;
        } finally {
            secondNumber = STRING_EMPTY;
            isReset = true;
            setOperation(Operation.DEFAULT);
        }
    }

    private void handleIncreaseButtonClicked() {
        if(isException || operationType == Operation.INCREASE) {
            return;
        }
        setOperation(Operation.INCREASE);
        result.setText(firstNumber+operationSign+(secondNumber.equals(STRING_EMPTY) ? STRING_EMPTY : secondNumber));
        isReset = false;
        isFirstNumberInput = false;
    }

    private void handleDecreaseButtonClicked() {
        if(isException || operationType == Operation.DECREASE) {
            return;
        }
        setOperation(Operation.DECREASE);
        result.setText(firstNumber+operationSign+(secondNumber.equals(STRING_EMPTY) ? STRING_EMPTY : secondNumber));
        isReset = false;
        isFirstNumberInput = false;
    }

    private void handleMultiplyButtonClicked() {
        if(isException) {
            return;
        }
        isReset = false;
        setOperation(Operation.MULTIPLY);
        isFirstNumberInput = false;
        result.setText(firstNumber+operationSign+(secondNumber.equals(STRING_EMPTY) ? STRING_EMPTY : secondNumber));
    }

    private void handleDivideButtonClicked() {
        if(isException) {
            return;
        }
        isReset = false;
        setOperation(Operation.DIVIDE);
        isFirstNumberInput = false;
        result.setText(firstNumber+operationSign+(secondNumber.equals(STRING_EMPTY) ? STRING_EMPTY : secondNumber));
    }

    private void handleSignRevertButtonClicked() {
        if(operationType == Operation.MULTIPLY || operationType == Operation.DIVIDE) {
            return;
        }
        if(isFirstNumberInput) {
            if(!STRING_EMPTY.equals(firstNumber)) {
                firstNumber = String.valueOf(-Double.valueOf(firstNumber));
                result.setText(String.valueOf(firstNumber));
            }
        } else {
            if(operationType == Operation.INCREASE) {
                setOperation(Operation.DECREASE);
            }
            if(operationType == Operation.DECREASE) {
                setOperation(Operation.INCREASE);
            }
            result.setText(firstNumber + operationSign + (secondNumber.equals(STRING_EMPTY) ? STRING_EMPTY : secondNumber));
            secondNumber = String.valueOf(-Double.valueOf(secondNumber));
        }
    }

    private void handleMemFreeButtonClicked() {
        isFirstNumberInput = true;
        setOperation(Operation.DEFAULT);
        isReset = true;
        firstNumber = STRING_EMPTY;
        secondNumber = STRING_EMPTY;
        result.setText(STRING_ZERO);
    }

    private void handleMemRestoreButtonClicked() {
        if(operationType == Operation.DEFAULT) {
            firstNumber = memValue;
            result.setText(String.valueOf(memValue));
            return;
        }
        if(isFirstNumberInput) {
            firstNumber = memValue;
        } else {
            secondNumber = memValue;
        }
        result.setText(result.getText() + memValue);
    }

    private void handleMemSubtractButtonClicked() {
        if(STRING_EMPTY.equals(result.getText()) || STRING_EMPTY.equals(memValue) || !isFirstNumberInput) {
            return;
        }

        memValue = BigDecimal.valueOf(Double.valueOf(memValue)).subtract(BigDecimal.valueOf(Double.valueOf(result.getText()))).toPlainString();
    }

    private void handleMemAddClicked() {
        if(STRING_EMPTY.equals(result.getText()) || !isFirstNumberInput) {
            return;
        }

        if(STRING_EMPTY.equals(memValue)) {
            memValue = result.getText();
        } else {
            memValue = BigDecimal.valueOf(Double.valueOf(memValue)).add(BigDecimal.valueOf(Double.valueOf(result.getText()))).toPlainString();
        }
    }

    private void handleMemCleanButtonClicked() {
        memValue = STRING_EMPTY;
    }
}
