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

import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

public class StopWatchButton extends Parent {

    private final Color colorWeak;
    private final Color colorStrong;
    private final Rectangle rectangleSmall = new Rectangle(14, 7);
    private final Rectangle rectangleBig = new Rectangle(28, 5);
    private final Rectangle rectangleWatch = new Rectangle(24, 14);
    private final Rectangle rectangleVisual = new Rectangle(28, 7 + 5 + 14);

    StopWatchButton(Color colorWeak, Color colorStrong) {
        this.colorStrong = colorStrong;
        this.colorWeak = colorWeak;
        configureDesign();
        setCursor(Cursor.HAND);
        getChildren().addAll(rectangleVisual, rectangleSmall, rectangleBig, rectangleWatch);
    }

    private void configureDesign() {
        rectangleVisual.setLayoutY(0f);
        rectangleVisual.setLayoutX(-14);
        rectangleVisual.setFill(Color.TRANSPARENT);

        rectangleSmall.setLayoutX(-7);
        rectangleSmall.setLayoutY(5);
        rectangleSmall.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop[]{
                    new Stop(0, colorWeak),
                    new Stop(0.5, colorStrong),
                    new Stop(1, colorWeak)}));

        rectangleBig.setLayoutX(-14);
        rectangleBig.setLayoutY(0);
        rectangleBig.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop[]{
                    new Stop(0, colorStrong),
                    new Stop(0.5, colorWeak),
                    new Stop(1, colorStrong)}));

        rectangleWatch.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop[]{
                    new Stop(0, Color.web("#4e605f")),
                    new Stop(0.2, Color.web("#c3d6d5")),
                    new Stop(0.5, Color.web("#f9ffff")),
                    new Stop(0.8, Color.web("#c3d6d5")),
                    new Stop(1, Color.web("#4e605f"))}));
        rectangleWatch.setLayoutX(-12);
        rectangleWatch.setLayoutY(12);
    }

    private void move(double smallRectHeight) {
        rectangleSmall.setHeight(smallRectHeight);
        rectangleSmall.setTranslateY(7 - smallRectHeight);
        rectangleBig.setTranslateY(7 - smallRectHeight);
    }

    public void moveDown() { move(0); }

    public void moveUp() { move(7); }
}
