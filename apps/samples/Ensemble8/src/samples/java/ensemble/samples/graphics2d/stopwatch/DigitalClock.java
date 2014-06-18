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
package ensemble.samples.graphics2d.stopwatch;

import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.effect.Lighting;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class DigitalClock extends Parent {

    private final HBox hBox = new HBox();
    public final Font FONT = new Font(16);
    private Text[] digits = new Text[8];
    private Group[] digitsGroup = new Group[8];
    private int[] numbers = {0, 1, 3, 4, 6, 7};

    DigitalClock() {
        configureDigits();
        configureDots();
        configureHbox();
        getChildren().addAll(hBox);
    }

    private void configureDigits() {
        for (int i : numbers) {
            digits[i] = new Text("0");
            digits[i].setFont(FONT);
            digits[i].setTextOrigin(VPos.TOP);
            digits[i].setLayoutX(2.3);
            digits[i].setLayoutY(-1);
            Rectangle background;
            if (i < 6) {
                background = createBackground(Color.web("#a39f91"), Color.web("#FFFFFF"));
                digits[i].setFill(Color.web("#000000"));
            } else {
                background = createBackground(Color.web("#bdbeb3"), Color.web("#FF0000"));
                digits[i].setFill(Color.web("#FFFFFF"));
            }
            digitsGroup[i] = new Group(background, digits[i]);
        }
    }

    private void configureDots() {
        digits[2] = createDot(":");
        digitsGroup[2] = new Group(createDotBackground(), digits[2]);
        digits[5] = createDot(".");
        digitsGroup[5] = new Group(createDotBackground(), digits[5]);
    }

    private Rectangle createDotBackground() {
        Rectangle background = new Rectangle(8, 17, Color.TRANSPARENT);
        background.setStroke(Color.TRANSPARENT);
        background.setStrokeWidth(2);
        return background;
    }

    private Text createDot(String string) {
        Text text = new Text(string);
        text.setFill(Color.web("#000000"));
        text.setFont(FONT);
        text.setTextOrigin(VPos.TOP);
        text.setLayoutX(1);
        text.setLayoutY(-4);
        return text;
    }

    private Rectangle createBackground(Color stroke, Color fill) {
        Rectangle background = new Rectangle(14, 17, fill);
        background.setStroke(stroke);
        background.setStrokeWidth(2);
        background.setEffect(new Lighting());
        background.setCache(true);
        return background;
    }

    private void configureHbox() {
        hBox.getChildren().addAll(digitsGroup);
        hBox.setSpacing(1);
    }

    public void refreshDigits(String time) { //expecting time in format "xx:xx:xx"
        for (int i = 0; i < digits.length; i++) {
            digits[i].setText(time.substring(i, i + 1));
        }
    }
}
