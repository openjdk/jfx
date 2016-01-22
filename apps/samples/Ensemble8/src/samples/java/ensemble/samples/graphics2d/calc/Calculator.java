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
package ensemble.samples.graphics2d.calc;

import ensemble.samples.graphics2d.calc.Key.Code;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;


public class Calculator extends Parent {

    private static final double BORDER = 7.0;
    private static final double TR_Y = 44.0;
    private static final double WIDHT = Key.WIDTH * 4 + BORDER * 2;
    private static final double HEIGHT = Key.HEIGHT * 5 + BORDER * 2 + TR_Y;
    private static final Stop[] LINE_STOPS = new Stop[]{new Stop(0, Color.BLACK), new Stop(0.2, Color.GRAY)};
    private static final LinearGradient LINE_FILL = new LinearGradient(0, 0, 0, HEIGHT, false, CycleMethod.NO_CYCLE, LINE_STOPS);
    private static final int MAX_DIGITS = 10;

    private Rectangle background;
    private Rectangle numberBackground;
    private Text numberText;
    private Text operationText;
    private Text memoryText;

    private Key[] keys = new Key[Util.KEY_CODES[0].length * Util.KEY_CODES.length];

    private String number = "0";
    private String operation = "";
    private String memory = "";
    private boolean reset;
    private boolean decimal;
    private boolean opActive;
    private int opCode = -1;
    private double opNum1;
    private double opNum2;
    private double memNum;
    private int selIndex;

    private boolean keyNavigatorActive;

    public Calculator() {
        init();
        getChildren().addAll(background, numberBackground, numberText, operationText, memoryText);
        getChildren().addAll(keys);
    }

    private void init() {
        createBackground();
        createNumberBackground();
        createNumberText();
        createOperationText();
        createMemoryText();
        createKeys();
        addKeyListener();
    }

    private void addKeyListener() {
        setOnKeyPressed((KeyEvent ke) -> {
            processKeyEvent(ke);
        });
        setFocusTraversable(true);
        requestFocus();
    }

    private void createBackground() {
        background = new Rectangle(3, 3, WIDHT, HEIGHT);
        final Stop[] stops = new Stop[]{new Stop(0, Color.GRAY), new Stop(0.2, Color.BLACK)};
        final LinearGradient fill = new LinearGradient(0, 0, 0, HEIGHT, false, CycleMethod.NO_CYCLE, stops);
        background.setFill(fill);
        background.setStroke(LINE_FILL);
        background.setStrokeWidth(3);
        background.setArcHeight(20);
        background.setArcWidth(20);
    }

    private void createNumberBackground() {
        numberBackground = new Rectangle(3 + BORDER, 3 + BORDER, WIDHT - 2 * BORDER, 35);
        final Stop[] stops = new Stop[]{new Stop(0, Color.GRAY), new Stop(0.2, Color.WHITE)};
        final LinearGradient fill = new LinearGradient(0, 0, 0, HEIGHT, false, CycleMethod.NO_CYCLE, stops);
        numberBackground.setFill(fill);
        numberBackground.setStroke(LINE_FILL);
        numberBackground.setStrokeWidth(1);
        numberBackground.setArcHeight(10);
        numberBackground.setArcWidth(10);
    }

    private void createNumberText() {
        numberText = new Text();
        numberText.setTranslateY(8 + BORDER);
        numberText.setFont(Font.font("monospaced", 25));
        numberText.setFill(Color.BLACK);
        numberText.setTextOrigin(VPos.TOP);
        numberText.setText(number);
        onNewNumber();
    }

    private void createOperationText() {
        operationText = new Text();
        operationText.setTranslateX(8 + BORDER);
        operationText.setTranslateY(13 + BORDER);
        operationText.setFont(Font.font("monospaced", 10));
        operationText.setFill(Color.BLACK);
        operationText.setTextOrigin(VPos.TOP);
        onNewOperation();
    }

    private void createMemoryText() {
        memoryText = new Text();
        memoryText.setTranslateX(8 + BORDER);
        memoryText.setTranslateY(24 + BORDER);
        memoryText.setFont(Font.font("monospaced", 10));
        memoryText.setFill(Color.BLACK);
        memoryText.setTextOrigin(VPos.TOP);
        onNewMemoryOperation();
    }

    private void createKeys() {
        for (int i = 0; i < Util.KEY_CODES.length; i++) {
            for (int j = 0; j < Util.KEY_CODES[0].length; j++) {
                final Key key = new Key(Util.KEY_CODES[i][j]);
                key.setTranslateX(background.getX() + (Key.WIDTH + 1) * j + BORDER);
                key.setTranslateY(background.getY() + (Key.HEIGHT + 1) * i + BORDER + TR_Y);
                key.setOnMousePressed((MouseEvent me) -> {
                    onKey(key);
                });
                keys[i * Util.KEY_CODES[0].length + j] = key;

            }
        }
    }

    private void onKey(Key key) {
        final Code code = key.getCode();
        if (code.getValue() >= 0 && code.getValue() <= 9) {
            onDigit(code);
        } else if (code == Code.M_PLUS || code == Code.M_MINUS || code == Code.M) {
            onMemory(code);
        } else if (code == Code.DECIMAL) {
            onDecimal();
        } else if (code == Code.EQUALS) {
            onEquals();
        } else if (code == Code.CLEAR) {
            onClear();
        } else if (code == Code.ADD || code == Code.SUBTRACT || code == Code.MULTIPLY
                || code == Code.DIVIDE) {
            onOperations(code);
        }
        refreshDisplay();
    }

    private void onDigit(Code code) {
        opActive = false;
        checkReset();
        String tempText = number;
        if (tempText.equals("0")) {
            tempText = "";
        }
        number = tempText + code.getValue();
        refreshDisplay();
    }

    private void onMemory(Code code) {
        double tempNum = 0f;
        try {
            tempNum = Double.parseDouble(number);
        } catch (Exception ex) {
        }
        if (code == Code.M) {

            if (opCode == -1) {
                opNum1 = memNum;
            } else {
                opNum2 = memNum;
            }
            number = String.valueOf(memNum);
            return;

        } else if (code == Code.M_PLUS) {
            memory = "M";
            memNum = memNum + tempNum;
        } else if (code == Code.M_MINUS) {
            memory = "M";
            memNum = memNum - tempNum;
        }

        opCode = -1;
        reset = true;
        decimal = false;
        operation = "";
    }

    private void onDecimal() {
        opActive = false;
        checkReset();
        if (!decimal) {
            number += ".";
            decimal = true;
        }
    }

    private void onEquals() {
        performMathsOperation(!operation.isEmpty());
        reset = true;
        decimal = false;
        operation = "";
        opActive = false;
    }

    private void onClear() {
        reset = true;
        decimal = false;
        opCode = -1;
        opNum1 = opNum2 = memNum = 0;
        number = "0";
        operation = "";
        opActive = false;
    }

    private void onOperations(Code code) {
        if(operation.isEmpty()){
            opCode = -1;
        }
        operation = code.getText();
        if(opActive){
            opCode = code.getValue();
            return;
        }
        opActive = true;
        if (opCode == -1) {
            try {
                opNum1 = Double.parseDouble(number);
            } catch (Exception ex) {
            }
            reset = true;
        } else {
            performMathsOperation(true);
        }
        opCode = code.getValue();
        decimal = false;
    }

    private void checkReset() {
        if (reset) {
            number = "";
            reset = false;
        }
    }

    private void refreshDisplay() {
        onNewNumber();
        onNewOperation();
        onNewMemoryOperation();
    }

    /**
     *
     * @param getOpNum2 false, if the last operation should be performed again, true otherwise
     */
    private void performMathsOperation(boolean getOpNum2) {
        reset = true;
        if(getOpNum2){
            try {
                opNum2 = Double.parseDouble(number);
            } catch (Exception ex) {
            }
        }
        if (opCode == Code.DIVIDE.getValue()) {
            opNum1 = opNum1 / opNum2;
        } else if (opCode == Code.MULTIPLY.getValue()) {
            opNum1 = opNum1 * opNum2;
        } else if (opCode == Code.ADD.getValue()) {
            opNum1 = opNum1 + opNum2;
        } else if (opCode == Code.SUBTRACT.getValue()) {
            opNum1 = opNum1 - opNum2;
        }
        number = String.valueOf(opNum1);
    }

    private void onNewOperation() {
        operationText.setText(operation.toString());
    }

    private void onNewNumber() {
        formatNumber();
        numberText.setText(number);
        numberText.setTranslateX(WIDHT - numberText.getLayoutBounds().getWidth() - 20);
    }

    private void onNewMemoryOperation() {
        if (memNum != 0) {
            memory = "M";
        } else {
            memory = "";
        }
        memoryText.setText(memory);
    }

    private void formatNumber() {
        int dotIndex = number.indexOf(".");
        if (dotIndex != -1) {
            int eIndex = number.indexOf("E");
            if (eIndex != -1) {
                String eTemp = number.substring(eIndex);
                if(number.length() > MAX_DIGITS){
                    number = number.substring(0, MAX_DIGITS - eTemp.length()) + eTemp;
                }
            }
        }
        if(number.equals(".")){
            number = "0.";
        }
        if (number.length() > MAX_DIGITS) {
            number = number.substring(0, MAX_DIGITS);
        }
    }

    private void processKeyEvent(KeyEvent e) {
        String text = e.getText();
        if(text.equals("0")){
            onDigit(Code.DIGIT_0);
        } else if(text.equals("1")){
            onDigit(Code.DIGIT_1);
        } else if(text.equals("2")){
            onDigit(Code.DIGIT_2);
        } else if(text.equals("3")){
            onDigit(Code.DIGIT_3);
        } else if(text.equals("4")){
            onDigit(Code.DIGIT_4);
        } else if(text.equals("5")){
            onDigit(Code.DIGIT_5);
        } else if(text.equals("6")){
            onDigit(Code.DIGIT_6);
        } else if(text.equals("7")){
            onDigit(Code.DIGIT_7);
        } else if(text.equals("8")){
            onDigit(Code.DIGIT_8);
        } else if(text.equals("9")){
            onDigit(Code.DIGIT_9);
        } else if(text.equals("+")){
            onOperations(Code.ADD);
        } else if(text.equals("-")){
            onOperations(Code.SUBTRACT);
        } else if(text.equals("*")){
            onOperations(Code.MULTIPLY);
        } else if(text.equals("/")){
            onOperations(Code.DIVIDE);
        } else if(text.equals(".")){
            onDecimal();
        } else if(text.equals("=")){
            onEquals();
        }

        switch (e.getCode()) {
            case DELETE:
                onClear();
                break;
            case LEFT:
                selIndex--;
                if (selIndex < 0) {
                    selIndex = 19;
                }
                setSelKey(selIndex);
                break;
            case RIGHT:
                selIndex++;
                if (selIndex > 19) {
                    selIndex = 0;
                }
                setSelKey(selIndex);
                break;
            case UP:
                selIndex = selIndex - 4;
                if (selIndex < 0) {
                    selIndex = 20 + selIndex;
                }
                setSelKey(selIndex);
                break;
            case DOWN:
                selIndex = selIndex + 4;
                if (selIndex > 19) {
                    selIndex = selIndex - 20;
                }
                setSelKey(selIndex);
                break;
            case ENTER:
                if(keyNavigatorActive){
                    Key key = keys[selIndex];
                    onKey(key);
                } else {
                    onEquals();
                }
                break;
            case ESCAPE:
                clearSelKey();
                break;
        }
        refreshDisplay();
    }

    private void setSelKey(int index) {
        for (Key key : keys) {
            key.setCellStroke(false);
        }
        keys[index].setCellStroke(true);
        keyNavigatorActive = true;
    }

    private void clearSelKey(){
        for(Key key : keys){
            key.setCellStroke(false);
        }
        keyNavigatorActive = false;
    }

}
