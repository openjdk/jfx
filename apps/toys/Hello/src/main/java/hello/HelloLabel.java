/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class HelloLabel extends Application {

    static HBox wrapHBoxAroundLabel(Label label) {
        HBox hbox = new HBox();
        hbox.setId("hbox");
        hbox.getChildren().add(new Rectangle(10,10, Color.BLACK));
        label.setId(label.getText() != null? label.getText() : "no text");
        hbox.getChildren().add(label);
        hbox.getChildren().add(new Rectangle(12,12));
        return hbox;
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello Label");
        VBox vbox = new VBox();
        vbox.setId("vbox");
        vbox.setSpacing(8);
        vbox.getStyleClass().add("hellovlabel-vbox");

        Scene scene = new Scene(vbox, 280,480);
        //scene.getStylesheets().add("hello/hello.css");
        

        Label label = new Label("Text");
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label();
        label.setGraphic(new Circle(12));
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label(" and Text");
        label.setId(label.getText());
        label.setGraphic(new Circle(12));
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label("textWrap == true but shouldn't wrap");
        label.setId(label.getText());
        label.setWrapText(true);
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label("textWrap == true and prefwidth == 50 so should wrap and let's make this really really really long");
        label.setId(label.getText());
        label.setWrapText(true);
        label.setPrefSize(50,70);
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label("textOverrun == CLIP");
        label.setId(label.getText());
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setPrefWidth(60);
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label("textOverrun == CENTER_ELLIPSIS");
        label.setId(label.getText());
        label.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        label.setPrefWidth(60);
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label("textOverrun == CENTER_WORD_ELLIPSIS");
        label.setId(label.getText());
        label.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
        label.setPrefWidth(60);
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label("textOverrun == ELLIPSIS");
        label.setId(label.getText());
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setPrefWidth(60);
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label("textOverrun == LEADING ELLIPSIS");
        label.setId(label.getText());
        label.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        label.setPrefWidth(60);
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label("textOverrun == LEADING WORD ELLIPSIS");
        label.setId(label.getText());
        label.setTextOverrun(OverrunStyle.LEADING_WORD_ELLIPSIS);
        label.setPrefWidth(60);
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label("textOverrun == WORD_ELLIPSIS");
        label.setId(label.getText());
        label.setTextOverrun(OverrunStyle.WORD_ELLIPSIS);
        label.setPrefWidth(60);
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        label = new Label("line one.\nno spacing.");
        vbox.getChildren().add(wrapHBoxAroundLabel(label));
  
        label = new Label("line one.\nspacing please.");
        label.setLineSpacing(10);
        vbox.getChildren().add(wrapHBoxAroundLabel(label));

        scene.setRoot(vbox);
        stage.setScene(scene);
        stage.show();
    }
}
