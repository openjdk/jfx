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
package ensemble.samples.controls.progressbar;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

/**
 * A sample that demonstrates the ProgressBar control.
 *
 * @sampleName Progress Bar
 * @preview preview.png
 * @see javafx.scene.control.ProgressBar
 * @related /Controls/Progress Indicator
 * @embedded
 */
public class ProgressBarApp extends Application {

    public Parent createContent() {
        double y = 15;
        final double SPACING = 15;
        ProgressBar p1 = new ProgressBar();
        p1.setLayoutY(y);

        y += SPACING;
        ProgressBar p2 = new ProgressBar();
        p2.setPrefWidth(150);
        p2.setLayoutY(y);

        y += SPACING;
        ProgressBar p3 = new ProgressBar();
        p3.setPrefWidth(200);
        p3.setLayoutY(y);

        y = 15;
        ProgressBar p4 = new ProgressBar();
        p4.setLayoutX(215);
        p4.setLayoutY(y);
        p4.setProgress(0.25);

        y += SPACING;
        ProgressBar p5 = new ProgressBar();
        p5.setPrefWidth(150);
        p5.setLayoutX(215);
        p5.setLayoutY(y);
        p5.setProgress(0.50);

        y += SPACING;
        ProgressBar p6 = new ProgressBar();
        p6.setPrefWidth(200);
        p6.setLayoutX(215);
        p6.setLayoutY(y);
        p6.setProgress(1);

        Group group = new Group();
        group.getChildren().addAll(p1,p2,p3,p4,p5,p6);
        return group;
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
