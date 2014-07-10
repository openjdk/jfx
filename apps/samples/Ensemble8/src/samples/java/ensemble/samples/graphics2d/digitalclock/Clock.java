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
package ensemble.samples.graphics2d.digitalclock;

import java.util.Calendar;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class Clock extends Parent {

    private Calendar calendar = Calendar.getInstance();
    private Digit[] digits;
    private Timeline delayTimeline, secondTimeline;

    public Clock(Color onColor, Color offColor) {
        // create effect for on LEDs
        Glow onEffect = new Glow(1.7f);
        onEffect.setInput(new InnerShadow());
        // create effect for on dot LEDs
        Glow onDotEffect = new Glow(1.7f);
        onDotEffect.setInput(new InnerShadow(5, Color.BLACK));
        // create effect for off LEDs
        InnerShadow offEffect = new InnerShadow();
        // create digits
        digits = new Digit[7];
        for (int i = 0; i < 6; i++) {
            Digit digit = new Digit(onColor, offColor, onEffect, offEffect);
            digit.setLayoutX(i * 80 + ((i + 1) % 2) * 20);
            digits[i] = digit;
            getChildren().add(digit);
        }
        // create dots
        Group dots = new Group(
                new Circle(80 + 54 + 20, 44, 6, onColor),
                new Circle(80 + 54 + 17, 64, 6, onColor),
                new Circle((80 * 3) + 54 + 20, 44, 6, onColor),
                new Circle((80 * 3) + 54 + 17, 64, 6, onColor));
        dots.setEffect(onDotEffect);
        getChildren().add(dots);
        // update digits to current time and start timer to update every second
        refreshClocks();
    }

    private void refreshClocks() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        digits[0].showNumber(hours / 10);
        digits[1].showNumber(hours % 10);
        digits[2].showNumber(minutes / 10);
        digits[3].showNumber(minutes % 10);
        digits[4].showNumber(seconds / 10);
        digits[5].showNumber(seconds % 10);
    }

    public void play() {
        // wait till start of next second then start a timeline to call refreshClocks() every second
        delayTimeline = new Timeline();
        delayTimeline.getKeyFrames().add(
                new KeyFrame(new Duration(1000 - (System.currentTimeMillis() % 1000)), (ActionEvent event) -> {
                    if (secondTimeline != null) {
                        secondTimeline.stop();
                    }
            secondTimeline = new Timeline();
            secondTimeline.setCycleCount(Timeline.INDEFINITE);
            secondTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), (ActionEvent event1) -> {
                refreshClocks();
            }));
            secondTimeline.play();
        }));
        delayTimeline.play();
    }

    public void stop() {
        delayTimeline.stop();
        if (secondTimeline != null) {
            secondTimeline.stop();
        }
    }
}
