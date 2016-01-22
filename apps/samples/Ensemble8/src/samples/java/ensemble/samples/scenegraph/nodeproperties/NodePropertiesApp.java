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
package ensemble.samples.scenegraph.nodeproperties;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * A sample that demonstrates some properties of nodes. Use the radio buttons
 * to send any of the rectangles to the front or back. Use the controls to
 * change opacity or horizontal position.
 *
 * @sampleName Node Properties
 * @preview preview.png
 * @see javafx.scene.Node
 * @related /Scenegraph/Custom Node
 * @playground rectA.translateX (name="Rectangle A translate X", min=0, max=50)
 * @playground rectB.translateX (name="Rectangle B translate X", min=0, max=50)
 * @playground rectC.translateX (name="Rectangle C translate X", min=0, max=50)
 * @playground rectA.opacity (name="Rectangle A Opacity", min=0, max=1)
 * @playground rectB.opacity (name="Rectangle B Opacity", min=0, max=1)
 * @playground rectC.opacity (name="Rectangle C Opacity", min=0, max=1)
 */
public class NodePropertiesApp extends Application {

    private Rectangle rectA;
    private Rectangle rectB;
    private Rectangle rectC;

    public Parent createContent() {

        //X position of node = X + LayoutX + TranslateX
        rectA = new Rectangle(50, 50, Color.LIGHTSALMON);
        //set position of node temporary (can be changed after)
        rectA.setTranslateX(10);

        rectB = new Rectangle(50, 50, Color.LIGHTGREEN);
        //set position of node when addinf to some layout
        rectB.setLayoutX(20);
        rectB.setLayoutY(10);

        rectC = new Rectangle(50, 50, Color.DODGERBLUE);
        //last posibility of setting X position of node
        rectC.setX(30);
        rectC.setY(20);
        //opacity of node can be set
        rectC.setOpacity(0.8);

        Pane root = new Pane(rectA, rectB, rectC);
        root.setPrefSize(130, 100);
        root.setMinSize(130, 100);
        root.setMaxSize(130, 100);

        return root;
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
