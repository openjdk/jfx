/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HelloProgressIndicator extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello ProgressIndicator");
        Scene scene = new Scene(new Group(), 600, 450);
//        scene.setFill(Color.CHOCOLATE);

        Group root = (Group)scene.getRoot();

        ProgressIndicator pInd1 = new ProgressIndicator();
        pInd1.setPrefSize(50, 50);
        pInd1.setLayoutX(25);
        pInd1.setLayoutY(40);
        pInd1.setVisible(true);
        root.getChildren().add(pInd1);

        ProgressIndicator pInd1a = new ProgressIndicator();
        pInd1a.setStyle("-fx-progress-color: red;");
        pInd1a.setPrefSize(100, 100);
        pInd1a.setLayoutX(75);
        pInd1a.setLayoutY(40);
        pInd1a.setVisible(true);
        root.getChildren().add(pInd1a);

        ProgressIndicator pInd2 = new ProgressIndicator();
        pInd2.setPrefSize(150, 150);
        pInd2.setLayoutX(200);
        pInd2.setLayoutY(20);
        pInd2.setProgress(0.25F);
        pInd2.setVisible(true);
        root.getChildren().add(pInd2);

        ProgressIndicator pInd25 = new ProgressIndicator();
        pInd25.setPrefSize(150, 150);
        pInd25.setLayoutX(300);
        pInd25.setLayoutY(20);
        pInd25.setProgress(1);
        pInd25.setVisible(true);
        root.getChildren().add(pInd25);

        ProgressIndicator pInd3 = new ProgressIndicator();
        pInd3.setPrefSize(100, 100);
        pInd3.setLayoutX(25);
        pInd3.setLayoutY(150);
        pInd3.setProgress(0.5F);
        pInd3.setVisible(true);
        root.getChildren().add(pInd3);

        ProgressIndicator pInd4 = new ProgressIndicator();
        pInd4.setPrefSize(40, 40);
        pInd4.setLayoutX(200);
        pInd4.setLayoutY(150);
        pInd4.setProgress(1.0F);
        pInd4.setVisible(true);
        root.getChildren().add(pInd4);

        ProgressIndicator pInd5 = new ProgressIndicator();
        pInd5.setLayoutX(100);
        pInd5.setLayoutY(250);
        pInd5.setProgress(1.0F);
        root.getChildren().add(pInd5);

        ProgressIndicator pInd6 = new ProgressIndicator();
        pInd6.setLayoutX(200);
        pInd6.setLayoutY(250);
        pInd6.setProgress(0.5);
        root.getChildren().add(pInd6);

        ProgressIndicator pInd7 = new ProgressIndicator();
        pInd7.setLayoutX(300);
        pInd7.setLayoutY(250);
        pInd7.setProgress(-1);
        pInd7.setStyle("-fx-spin-enabled:true;");
        root.getChildren().add(pInd7);

        ProgressIndicator pInd8 = new ProgressIndicator();
        pInd8.setLayoutX(360);
        pInd8.setLayoutY(250);
        pInd8.setProgress(-1);
        pInd8.setStyle("-fx-spin-enabled:false;");
        root.getChildren().add(pInd8);

        ProgressIndicator pInd9 = new ProgressIndicator();
        pInd9.setLayoutX(450);
        pInd9.setPrefSize(100, 100);
        pInd9.setLayoutY(250);
        pInd9.setProgress(-0.1);

        pInd9.progressProperty().addListener((ov, oldVal, newVal) -> {

               final double percent = newVal.doubleValue();
               if (percent < 0) return; // progress bar went indeterminate

               //
               // poor man's gradient for stops: red, yellow 50%, green
               // Based on http://en.wikibooks.org/wiki/Color_Theory/Color_gradient#Linear_RGB_gradient_with_6_segments
               //
               final double m = (2d * percent);
               final int n = (int)m;
               final double f = m - n;
               final int t = (int)(255 * f);
               int r=0, g=0, b=0;
               switch(n) {
                   case 0:
                       r = 255;
                       g = t;
                       b = 0;
                       break;
                   case 1:
                       r = 255 - t;
                       g = 255;
                       b = 0;
                       break;
                   case 2:
                       r = 0;
                       g = 255;
                       b = 0;
                       break;

               }
               final String style = String.format("-fx-progress-color: rgb(%d,%d,%d)", r, g, b);
               pInd9.setStyle(style);
            });
        root.getChildren().add(pInd9);

        final Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(true);
        final KeyValue kv = new KeyValue(pInd9.progressProperty(), 1);
        final KeyFrame kf1 = new KeyFrame(Duration.millis(3000), kv);
        timeline.getKeyFrames().add(kf1);
        timeline.play();


        // Helper code to create CSS for dotted progress indicator.
        Group g2 = new Group();
        root.getChildren().add(0,g2);
        double smallRadius = 1.5;
        double bigRadius = 14-smallRadius;
        String circles = "";
        for(int angle=0, i=0; angle<360; angle += 30, i++) {
            double centerX = bigRadius + smallRadius + Math.cos(Math.toRadians(angle)) * bigRadius;
            double centerY = bigRadius + smallRadius + Math.sin(Math.toRadians(angle)) * bigRadius;
            g2.getChildren().add(new Circle(centerX,centerY,smallRadius,Color.rgb(0,0,0,0.1)));
            circles += writeCircle(centerX,centerY,smallRadius);
            //System.out.println(".progress-indicator:indeterminate .segment"+i+" {\n" +
            //   "    -fx-shape:\""+ writeCircle(centerX,centerY,smallRadius)+"\";\n" +
            //   "}");
        }
        //System.out.println("<path d=\""+circles+"\" />");

        stage.setScene(scene);
        stage.show();
    }


    private String writeCircle(double  centerX,double centerY,double radius) {
        centerX = ((int)(centerX*100))/100d;
        centerY = ((int)(centerY*100))/100d;
        return "M"+(centerX-radius)+" "+centerY+" a"+radius+","+radius+" 0 1,1 0,1 Z";
    }
}
