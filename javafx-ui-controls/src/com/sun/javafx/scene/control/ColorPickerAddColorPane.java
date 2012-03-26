/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CircleBuilder;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 *
 * @author paru
 */
public class ColorPickerAddColorPane extends StackPane {
    
    private static final int CONTENT_PADDING = 10;
    private static final int RECT_SIZE = 200;
    private static final int CONTROLS_WIDTH = 252;
    
    final Stage dialog = new Stage();
    ColorRectPane colorRectPane;
    ControlsPane controlsPane;
    
    Circle colorRectIndicator;
    Rectangle colorRect;
    Rectangle colorRectOverlayOne;
    Rectangle colorRectOverlayTwo;
    Rectangle colorBar;
    Rectangle colorBarIndicator;
    
    public ColorPickerAddColorPane(Window owner) {
        getStyleClass().add("add-color-pane");
        if (owner != null) dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        colorRectPane = new ColorRectPane();
        controlsPane = new ControlsPane();
        
        Scene scene = new Scene(new Group());
        Group dialogRoot = (Group) scene.getRoot();
        getChildren().addAll(colorRectPane, controlsPane);
        dialogRoot.getChildren().addAll(this);
        
        dialog.setScene(scene);
        
//        Button dialogButton = new Button("Dismiss");
//        dialogButton.setLayoutX(CONTENT_PADDING+colorRectGroup.prefWidth(-1)+30);
//        dialogButton.setLayoutY(CONTENT_PADDING+10);
//        dialogButton.setOnAction(new EventHandler<ActionEvent>() {
//            @Override public void handle(ActionEvent e) {
//                dialog.hide();
//            }
//        });
//        dialogRoot.getChildren().addAll(contentGroup, dialogButton);
    }
    
    public void show() {
        dialog.show();
    }
    
    @Override public void layoutChildren() {
         controlsPane.relocate(colorRectPane.prefWidth(-1), 0);
    }
    
    @Override public double computePrefWidth(double height) {
        return getInsets().getLeft() + colorRectPane.prefWidth(height) +
                controlsPane.prefWidth(height) + getInsets().getRight();
    }
    
    @Override public double computePrefHeight(double width) {
        return getInsets().getTop() + Math.max(colorRectPane.prefHeight(width),
                controlsPane.prefHeight(width) + getInsets().getBottom());
    }
    
    static double computeXOffset(double width, double contentWidth, HPos hpos) {
        switch(hpos) {
            case LEFT:
               return 0;
            case CENTER:
               return (width - contentWidth) / 2;
            case RIGHT:
               return width - contentWidth;
        }
        return 0;
    }

    static double computeYOffset(double height, double contentHeight, VPos vpos) {
       switch(vpos) {
            case TOP:
               return 0;
            case CENTER:
               return (height - contentHeight) / 2;
            case BOTTOM:
               return height - contentHeight;
        }
       return 0;
    }
    
    /* ------------------------------------------------------------------------*/
    
    class ColorRectPane extends StackPane {
        
        private boolean changeIsLocal = false;
        private DoubleProperty hue = new SimpleDoubleProperty() {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    color.set(Color.hsb(hue.get(), clamp(sat.get() / 100), clamp(bright.get() / 100)));
                    changeIsLocal = false;
                }
            }
        };
        private DoubleProperty sat = new SimpleDoubleProperty() {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    color.set(Color.hsb(hue.get(), clamp(sat.get() / 100), clamp(bright.get() / 100)));
                    changeIsLocal = false;
                }
            }
        };
        private DoubleProperty bright = new SimpleDoubleProperty() {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    color.set(Color.hsb(hue.get(), clamp(sat.get() / 100), clamp(bright.get() / 100)));
                    changeIsLocal = false;
                }
            }
        };
        private ObjectProperty<Color> color = new SimpleObjectProperty<Color>(Color.RED) {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    final Color c = get();
                    hue.set(c.getHue());
                    sat.set(c.getSaturation() * 100);
                    bright.set(c.getBrightness() * 100);
                    changeIsLocal = false;
                }
            }
        };
        public ObjectProperty<Color> colorProperty() { return color; }
        public Color getColor() { return color.get(); }
        public void setColor(Color newColor) { color.set(newColor); }

        public ColorRectPane() {
            
            getStyleClass().add("color-rect-pane");
            colorRectIndicator = 
                    CircleBuilder.create().centerX(60).centerY(60).radius(5).stroke(Color.WHITE).
                    fill(null).effect(new DropShadow(2, 0, 1, Color.BLACK)).build();
            colorRectIndicator.centerXProperty().bind(new DoubleBinding() {
                { bind(sat); }
                @Override protected double computeValue() {
                    return (CONTENT_PADDING + 10) + (RECT_SIZE * (sat.get() / 100));
                }
            });
        
            colorRectIndicator.centerYProperty().bind(new DoubleBinding() {
                { bind(bright); }
                @Override protected double computeValue() {
                    return (CONTENT_PADDING + 10) + (RECT_SIZE * (1 - (bright.get() / 100)));
                }
            });
        
            colorRect = new Rectangle(RECT_SIZE, RECT_SIZE);
            colorRect.fillProperty().bind(new ObjectBinding<Paint>() {
                { bind(color); }
                @Override protected Paint computeValue() {
                    return Color.hsb(hue.getValue(), 1, 1);
                }
            });
        
            colorRectOverlayOne = new Rectangle(RECT_SIZE, RECT_SIZE);
            colorRectOverlayOne.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, 
                    new Stop(0, Color.rgb(255, 255, 255, 1)), 
                    new Stop(1, Color.rgb(255, 255, 255, 0))));
            colorRectOverlayOne.setStroke(Color.BLACK);
        
            EventHandler<MouseEvent> rectMouseHandler = new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    final double x = event.getX() - colorRect.getX();
                    final double y = event.getY() - colorRect.getY();
                    sat.set(clamp(x / RECT_SIZE) * 100);
                    bright.set(100 - (clamp(y / RECT_SIZE) * 100));
                }
            };
        
            colorRectOverlayTwo = new Rectangle(RECT_SIZE, RECT_SIZE);
            colorRectOverlayTwo.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, 
                        new Stop(0, Color.rgb(0, 0, 0, 0)), new Stop(1, Color.rgb(0, 0, 0, 1))));
            colorRectOverlayTwo.setOnMouseDragged(rectMouseHandler);
            colorRectOverlayTwo.setOnMouseClicked(rectMouseHandler);
            
            colorBar = new Rectangle(20, RECT_SIZE);
            colorBar.setFill(createHueGradient());

            colorBarIndicator = new Rectangle(24, 10, null);
            colorBarIndicator.setArcWidth(4);
            colorBarIndicator.setArcHeight(4);
            colorBarIndicator.setStroke(Color.WHITE);
            colorBarIndicator.setEffect(new DropShadow(2, 0, 1, Color.BLACK));
        
            colorBarIndicator.yProperty().bind(new DoubleBinding() {
                { bind(hue); }
                @Override protected double computeValue() {
                    return (CONTENT_PADDING + 5) + (RECT_SIZE * (hue.get() / 360));
                }
            });
            EventHandler<MouseEvent> barMouseHandler = new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    final double y = event.getY() - colorBar.getY();
                    hue.set(clamp(y / RECT_SIZE) * 360);
                }
            };
            colorBar.setOnMouseDragged(barMouseHandler);
            colorBar.setOnMouseClicked(barMouseHandler);
            
            getChildren().addAll(colorRect, colorRectOverlayOne, colorRectOverlayTwo, 
                    colorBar, colorRectIndicator, colorBarIndicator);
        }
        
        @Override public void layoutChildren() {
            double x = getInsets().getLeft();
            double y = getInsets().getTop();
//            double w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
//            double h = getHeight() - (getInsets().getTop() + getInsets().getBottom());
            colorRect.relocate(x, y);
            colorRectOverlayOne.relocate(x, y);
            colorRectOverlayTwo.relocate(x, y);
            
            colorBar.relocate(x+colorRect.getWidth()+10, y);
            colorBarIndicator.relocate(x+colorRect.getWidth()+8, y+5);
        }
    }
    
    /* ------------------------------------------------------------------------*/
    
    class ControlsPane extends StackPane {
        Label currentColorLabel;
        Label newColorLabel;
        Rectangle currentColorRect;
        Rectangle newColorRect;
        GridPane currentAndNewColor;
        GridPane pillButtons;
        Rectangle currentNewColorBorder;
        ToggleButton hsbButton;
        ToggleButton rgbButton;
        ToggleButton webButton;
        HBox hBox;
        GridPane colorSettings;
        HBox buttonBox;
                
        public ControlsPane() {
            getStyleClass().add("controls-pane");
            
            currentNewColorBorder = new Rectangle(CONTROLS_WIDTH, 18, null);
            currentNewColorBorder.setStroke(Color.BLACK);
            
            currentColorRect = new Rectangle(CONTROLS_WIDTH/2, 18, Color.YELLOW);
            newColorRect = new Rectangle(CONTROLS_WIDTH/2, 18, Color.LIGHTGREEN);
            currentColorLabel = new Label("Current Color");
            newColorLabel = new Label("New Color");
            Rectangle spacer = new Rectangle(0, 18);
            
            hsbButton = new ToggleButton("HSB");
            hsbButton.setId("pill-left");
            rgbButton = new ToggleButton("RGB");
            rgbButton.setId("pill-center");
            webButton = new ToggleButton("Web");
            webButton.setId("pill-right");
            ToggleGroup group = new ToggleGroup();
            hsbButton.setToggleGroup(group);
            rgbButton.setToggleGroup(group);
            webButton.setToggleGroup(group);
            group.selectToggle(hsbButton);
            
            hBox = new HBox();
            hBox.getChildren().addAll(hsbButton, rgbButton, webButton);
            
            currentAndNewColor = new GridPane();
//            colorSettings.setGridLinesVisible(true);
            currentAndNewColor.add(currentColorLabel, 0, 0, 2, 1);
            currentAndNewColor.add(newColorLabel, 2, 0, 2, 1);
            currentAndNewColor.add(currentColorRect, 0, 1, 2, 1);
            currentAndNewColor.add(newColorRect, 2, 1, 2, 1);
            currentAndNewColor.add(spacer, 0, 2, 4, 1);
            
            // Color settings Grid Pane
            colorSettings = new GridPane();
            colorSettings.setHgap(5);
            colorSettings.setVgap(4);
            colorSettings.setManaged(false);
            
            Rectangle spacer2 = new Rectangle(0, 5);
            colorSettings.add(spacer2, 0, 0, 3, 1);
            
            Label hueLabel = new Label("Hue:");
            hueLabel.setMinWidth(Control.USE_PREF_SIZE);
            colorSettings.add(hueLabel, 0, 1);
            
            Slider hueSlider = new Slider(0, 100, 50);
//            hueSlider.valueProperty().bind(colorRectPane.bright);
            colorSettings.add(hueSlider, 1, 1);
            
            TextField hueField = new TextField("50");
            hueField.setPrefColumnCount(6);
            colorSettings.add(hueField, 2, 1);
            
            Label saturationLabel = new Label("Saturation:");
            saturationLabel.setMinWidth(Control.USE_PREF_SIZE);
            colorSettings.add(saturationLabel, 0, 2);
            
            Slider saturationSlider = new Slider(0, 100, 50);
//            saturationSlider.valueProperty().bind(colorRectPane.sat);
            colorSettings.add(saturationSlider, 1, 2);
            
            TextField saturationField = new TextField("50");
            saturationField.setPrefColumnCount(6);
            colorSettings.add(saturationField, 2, 2);
            
            Label brightnessLabel = new Label("Hue:");
            brightnessLabel.setMinWidth(Control.USE_PREF_SIZE);
            colorSettings.add(brightnessLabel, 0, 3);
            
            Slider brightnessSlider = new Slider(0, 100, 50);
//            brightnessSlider.valueProperty().bind(colorRectPane.bright);
            colorSettings.add(brightnessSlider, 1, 3);
            
            TextField brightnessField = new TextField("50");
            brightnessField.setPrefColumnCount(6);
            colorSettings.add(brightnessField, 2, 3);
            
            Rectangle spacer3 = new Rectangle(0, 16);
            colorSettings.add(spacer3, 0, 4, 3, 1);
            
            Label alphaLabel = new Label("Alpha:");
            alphaLabel.setMinWidth(Control.USE_PREF_SIZE);
            colorSettings.add(alphaLabel, 0, 5);
            
            Slider alphaSlider = new Slider(0, 100, 50);
//            alphaSlider.valueProperty().bind(colorRectPane.bright);
            colorSettings.add(alphaSlider, 1, 5);
            
            TextField alphaField = new TextField("50");
            alphaField.setPrefColumnCount(6);
            colorSettings.add(alphaField, 2, 5);
            
            Rectangle spacer4 = new Rectangle(0, 12);
            colorSettings.add(spacer4, 0, 6, 3, 1);
            
            buttonBox = new HBox(4);
            Button addButton = new Button("Add");
            Button cancelButton = new Button("Cancel");
            buttonBox.getChildren().addAll(addButton, cancelButton);
            buttonBox.setStyle("-fx-border-color: red;");
            getChildren().addAll(currentAndNewColor, currentNewColorBorder, hBox, colorSettings, buttonBox);
        }
        
        public Label getCurrentColorLabel() {
            return currentColorLabel;
        }
        
        @Override public void layoutChildren() {
            double x = getInsets().getLeft();
            double y = getInsets().getTop();
//            double w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
//            double h = getHeight() - (getInsets().getTop() + getInsets().getBottom());
            currentAndNewColor.resizeRelocate(x+10,
                    y, CONTROLS_WIDTH, 18);
            currentNewColorBorder.relocate(x+10, 
                    y+controlsPane.currentColorLabel.prefHeight(-1));
            double hBoxX = computeXOffset(currentAndNewColor.prefWidth(-1), hBox.prefWidth(-1), HPos.CENTER);
            
            hBox.resizeRelocate(x+10+hBoxX, y+currentAndNewColor.prefHeight(-1), 
                    hBox.prefWidth(-1), hBox.prefHeight(-1));
            colorSettings.resizeRelocate(x+10, y+currentAndNewColor.prefHeight(-1)+hBox.prefHeight(-1)+5,
                    CONTROLS_WIDTH, colorSettings.prefHeight(-1));
            
            double buttonBoxX = computeXOffset(currentAndNewColor.prefWidth(-1), buttonBox.prefWidth(-1), HPos.RIGHT);
            buttonBox.resizeRelocate(x+10+buttonBoxX, y+currentAndNewColor.prefHeight(-1)+hBox.prefHeight(-1)+5+
                    colorSettings.prefHeight(-1), buttonBox.prefWidth(-1), buttonBox.prefHeight(-1));
            
        }
        
        @Override public double computePrefHeight(double width) {
            System.out.println("Top padding = "+getInsets().getTop()+" Bottom Padding = "+
                    getInsets().getBottom());
            return getInsets().getTop() + currentAndNewColor.prefHeight(width) +
                    currentNewColorBorder.prefHeight(width) + hBox.prefHeight(width) +
                    colorSettings.prefHeight(width) + buttonBox.prefHeight(width) +
                    getInsets().getBottom();
        }
    }
    
    private static double clamp(double value) {
        return value < 0 ? 0 : value > 1 ? 1 : value;
    }
    
    private static LinearGradient createHueGradient() {
        double offset;
        Stop[] stops = new Stop[255];
        for (int y = 0; y < 255; y++) {
            offset = (double)(1 - (1.0 / 255) * y);
            int h = (int)((y / 255.0) * 360);
            stops[y] = new Stop(offset, Color.hsb(h, 1.0, 1.0));
        }
        return new LinearGradient(0f, 1f, 1f, 0f, true, CycleMethod.NO_CYCLE, stops);
    }
    
}
