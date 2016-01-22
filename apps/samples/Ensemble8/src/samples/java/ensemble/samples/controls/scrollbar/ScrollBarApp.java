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
package ensemble.samples.controls.scrollbar;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * A sample showing horizontal and vertical scroll bars.
 *
 * @sampleName ScrollBar
 * @preview preview.png
 * @see javafx.scene.control.ScrollBar
 * @embedded
 */
public class ScrollBarApp extends Application {
    private Circle circle;
    private ScrollBar xscrollBar;
    private ScrollBar yscrollBar;
    private double xscrollValue=0;
    private double yscrollValue=15;
    private static final int xBarWidth = 393;
    private static final int xBarHeight = 15;
    private static final int yBarWidth = 15;
    private static final int yBarHeight = 393;
    private static final int circleRadius = 90;

    public Parent createContent() {
        Rectangle bg = new Rectangle(xBarWidth+yBarWidth,xBarHeight+yBarHeight,Color.rgb(90,90,90));
        Rectangle box = new Rectangle (100,100,Color.rgb(150,150,150));
        box.setTranslateX(147);
        box.setTranslateY(147);

        //create moveable circle
        circle = new Circle(45,45, circleRadius,  Color.rgb(90,210,210));
        circle.setOpacity(0.4);
        circle.relocate(0,15);

        //create horizontal scrollbar
        xscrollBar = horizontalScrollBar(-1,-1,xBarWidth,xBarHeight,xBarWidth,xBarHeight);
        xscrollBar.setUnitIncrement(20.0);
        xscrollBar.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            //changes the x position of the circle
            setScrollValueX(xscrollBar.getValue(), circle);
        });

        //create vertical scrollbar
        yscrollBar = verticalScrollBar(-1,-1,yBarWidth,yBarHeight,yBarWidth,yBarHeight);
        yscrollBar.setUnitIncrement(20.0);
        yscrollBar.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            //changes the y position of the circle
            setScrollValueY(yscrollBar.getValue(), circle);
        });

        //shift position of vertical scrollbar to right side of scene
        yscrollBar.setTranslateX(yBarHeight);
        yscrollBar.setTranslateY(yBarWidth);
        yscrollBar.setOrientation(Orientation.VERTICAL);

        Group group = new Group();
        group.getChildren().addAll(bg, box, circle, xscrollBar, yscrollBar);
        return group;
    }

    //Create a ScrollBar with given parameters
    private ScrollBar horizontalScrollBar(double minw, double minh, double prefw, double prefh, double maxw, double maxh) {
        final ScrollBar scrollBar = new ScrollBar();
        scrollBar.setMinSize(minw, minh);
        scrollBar.setPrefSize(prefw, prefh);
        scrollBar.setMaxSize(maxw, maxh);
        scrollBar.setVisibleAmount(50);
        scrollBar.setMax(xBarWidth-(2*circleRadius));
        return scrollBar;
    }

    //Create a ScrollBar with given parameters
    private ScrollBar verticalScrollBar(double minw, double minh, double prefw, double prefh, double maxw, double maxh) {
        final ScrollBar scrollBar = new ScrollBar();
        scrollBar.setMinSize(minw, minh);
        scrollBar.setPrefSize(prefw, prefh);
        scrollBar.setMaxSize(maxw, maxh);
        scrollBar.setVisibleAmount(50);
        scrollBar.setMax(yBarHeight-(2*circleRadius));
        return scrollBar;
    }
    //Updates x values
    private void setScrollValueX(double v, Circle circle) {
        this.xscrollValue = v;
        circle.relocate(xscrollValue, yscrollValue);
    }
    //Updates x values
    private void setScrollValueY(double v, Circle circle) {
        this.yscrollValue = v+xBarHeight;
        circle.relocate(xscrollValue, yscrollValue);
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
