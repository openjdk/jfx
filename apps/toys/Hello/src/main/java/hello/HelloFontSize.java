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

import java.util.Set;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class HelloFontSize extends Application {

    @Override public void start(Stage stage) {
        stage.setTitle("Font Size Test");
        Group group = new Group();

        Button button = new Button();
        button.setText("Dummy Button");
        button.setLayoutX(10);
        button.setLayoutY(10);

        Text textInstr = new Text("The following two strings should be of different sizes:");
        textInstr.setFont(new Font(14.0f));
        textInstr.setLayoutX(10);
        textInstr.setLayoutY(100);

        Text text12 = new Text("Tiny string: 10pt Font");
        text12.setFont(new Font(10.0f));
        text12.setLayoutX(10);
        text12.setLayoutY(130);

        Text text72 = new Text("Big string: 72pt Font");
        text72.setFont(new Font(72.0f));
        text72.setLayoutX(10);
        text72.setLayoutY(200);

        group.getChildren().addAll(textInstr, text12, text72, button);

        ToggleGroup tg = new ToggleGroup();
        ToggleButton t1 = new ToggleButton("First");
        t1.setId("t1");
        t1.setToggleGroup(tg);

        ToggleGroup tg2 = new ToggleGroup();
        ToggleButton t2 = new ToggleButton("Second");
        t2.setId("t2");
        t2.setToggleGroup(tg);

        ToggleButton t4 = new ToggleButton("Disable");
        t4.setToggleGroup(tg);
        t4.setDisable(true);

        ToggleButton t5 = new ToggleButton("Long ..... really Long");
        t5.setId("t5");
        t5.setToggleGroup(tg);

        final ToggleButton t6 = new ToggleButton("Big!");
        t6.setId("t6");
        t6.setStyle("-fx-font-size: 1.5em;");
        t6.setToggleGroup(tg);
        t6.fontProperty().addListener(new ChangeListener<Font>(){

            public void changed(ObservableValue<? extends Font> ov, Font t, Font t1) {
                double fsize = t1.getSize();
                fsize = Math.round(fsize*10.0)/10.0;                
                t6.setText(fsize + "px");
            }
        });

        final HBox hbox = new HBox();
        hbox.setId("toggle-buttons");
        hbox.setSpacing(3);
        hbox.getChildren().addAll(t1,t2,t4,t5,t6);

        final Slider slider = new Slider();
        slider.setId("FontSizeSlider");
        slider.setMin(9);
        slider.setMax(28);
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(1);
        slider.setSnapToTicks(true);

        final Label lbl = new Label("Adjust font size here");

        final HBox hbox2 = new HBox(5);
        hbox2.getChildren().addAll(slider, lbl);
        
        final VBox vbox = new VBox(5);
        vbox.getChildren().addAll(
            new Text("Move the slider, click around, hover here and there.\n"
                    +"Make sure the buttons don't magically resize.\n"
                    +"The font size of the last button is 1.5em.\n"
                    +"The text of the last button is the font size rounded to nearest 10th"), hbox, hbox2);
        
        vbox.setLayoutX(10);
        vbox.setLayoutY(300);

        final Group root = new Group();
        root.getChildren().addAll(group, vbox);

        slider.skinProperty().addListener(new ChangeListener<Skin>() {

            public void changed(ObservableValue<? extends Skin> ov, Skin t, Skin t1) {
                if (t1 != null) {
                    slider.setValue(14);
                }
            }
        });

        slider.valueProperty().addListener(new ChangeListener<Number>() {

            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                hbox.setStyle("-fx-font: " + t1.toString() + "px \"Comic Sans MS\";");
            }
        });
        
        hbox.styleProperty().addListener(new ChangeListener<String>() {

            public void changed(ObservableValue<? extends String> ov, String t, String t1) {
                lbl.setText(hbox.getStyle());
            }
        });

        // RT-28635
        final SpecialButton specialButton = new SpecialButton("Button");
        
        final VBox rt28635 = new VBox(5);
        rt28635.getStylesheets().add(HelloFontSize.class.getResource("hello.css").toExternalForm());
        
        rt28635.getChildren().addAll(
            new Text("A mouse-press on this button should cause the font size to be set to 20.\n" +
                "The font size and button size should return to normal on mouse-release."), specialButton);
        
        rt28635.setLayoutX(10);
        rt28635.setLayoutY(500);        
        root.getChildren().add(rt28635);
        
        Scene scene = new Scene(root, 800, 600);  
        stage.setScene(scene);
        stage.show();
    }
    
    private static class SpecialButton extends Button {
        
        public static final String PSEUDO_CLASS_NAME = "mouse_pressed";
        public static final String STYLE_CLASS_NAME = "special-button";

        {
            getStyleClass().add(STYLE_CLASS_NAME);
            
            setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent t) {
                    pseudoClassStateChanged(MOUSE_PRESSED_PSEUDO_CLASS,true);
                }
            });        

            setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent t) {
                    pseudoClassStateChanged(MOUSE_PRESSED_PSEUDO_CLASS,false);
                }
            });      
        }

        public SpecialButton() {
            super();
        }

        public SpecialButton(String str) {
            super(str);
        }

        private static final PseudoClass MOUSE_PRESSED_PSEUDO_CLASS = 
                PseudoClass.getPseudoClass(PSEUDO_CLASS_NAME);
        
        private BooleanProperty pressedPseudoState = new BooleanPropertyBase(false) {
            @Override
            protected void invalidated() {
                pseudoClassStateChanged(MOUSE_PRESSED_PSEUDO_CLASS, get());
            }

            @Override
            public Object getBean() {
                return this;
            }

            @Override
            public String getName() {
                return PSEUDO_CLASS_NAME;
            }
        };
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
