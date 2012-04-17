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

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
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
    ObjectProperty<Color> currentColorProperty = new SimpleObjectProperty<Color>();
    ObjectProperty<Color> customColorProperty = new SimpleObjectProperty<Color>();
    
    public ColorPickerAddColorPane(Window owner, ObjectProperty<Color> currentColorProperty) {
        getStyleClass().add("add-color-pane");
        this.currentColorProperty.bind(currentColorProperty);
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
    }
    
    public void show() {
        dialog.show();
    }
    
    @Override public void layoutChildren() {
        double x = getInsets().getLeft();
        double y = getInsets().getTop();
        controlsPane.relocate(x+colorRectPane.prefWidth(-1), 0);
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
                    updateHSBColor();
                    changeIsLocal = false;
                }
            }
        };
        private DoubleProperty sat = new SimpleDoubleProperty() {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    updateHSBColor();
                    changeIsLocal = false;
                }
            }
        };
        private DoubleProperty bright = new SimpleDoubleProperty() {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    updateHSBColor();
                    changeIsLocal = false;
                }
            }
        };
        private ObjectProperty<Color> color = new SimpleObjectProperty<Color>(); 
        public ObjectProperty<Color> colorProperty() { return color; }
        public Color getColor() { return color.get(); }
        public void setColor(Color newColor) { color.set(newColor); }

        private IntegerProperty red = new SimpleIntegerProperty() {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    updateRGBColor();
                    changeIsLocal = false;
                }
            }
        };
        
        private IntegerProperty green = new SimpleIntegerProperty() {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    updateRGBColor();
                    changeIsLocal = false;
                }
            }
        };
        
        private IntegerProperty blue = new SimpleIntegerProperty() {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    updateRGBColor();
                    changeIsLocal = false;
                }
            }
        };
        
        private DoubleProperty alpha = new SimpleDoubleProperty(100) {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    switch (controlsPane.colorSettingsMode) {
                        case HSB:
                            updateHSBColor();
                            break;
                        case RGB:
                            updateRGBColor();
                            break;
                        case WEB:
                            break;
                    }
                    changeIsLocal = false;
                }
            }
        };
         
        private void updateRGBColor() {
            setColor(Color.rgb(red.get(), green.get(), blue.get(), clamp(alpha.get() / 100)));
            hue.set(getColor().getHue());
            sat.set(getColor().getSaturation() * 100);
            bright.set(getColor().getBrightness() * 100);
        }
        
        private void updateHSBColor() {
            setColor(Color.hsb(hue.get(), clamp(sat.get() / 100), 
                            clamp(bright.get() / 100), clamp(alpha.get() / 100)));
            red.set(doubleToInt(getColor().getRed()));
            green.set(doubleToInt(getColor().getGreen()));
            blue.set(doubleToInt(getColor().getBlue()));
        }
       
        private void colorChanged() {
            if (!changeIsLocal) {
                changeIsLocal = true;
                hue.set(getColor().getHue());
                sat.set(getColor().getSaturation() * 100);
                bright.set(getColor().getBrightness() * 100);
                red.set(doubleToInt(getColor().getRed()));
                green.set(doubleToInt(getColor().getGreen()));
                blue.set(doubleToInt(getColor().getBlue()));
                changeIsLocal = false;
            }
        }
        
        public ColorRectPane() {
            
            getStyleClass().add("color-rect-pane");
            
            color.addListener(new ChangeListener<Color>() {

                @Override
                public void changed(ObservableValue<? extends Color> ov, Color t, Color t1) {
                    colorChanged();
                }
            });
            
            colorRectIndicator = new Circle(60, 60, 5, null);
            colorRectIndicator.setStroke(Color.WHITE);
            colorRectIndicator.setEffect(new DropShadow(2, 0, 1, Color.BLACK));
        
            colorRect = new Rectangle(RECT_SIZE, RECT_SIZE);
            colorRect.fillProperty().bind(new ObjectBinding<Paint>() {
                { bind(color); }
                @Override protected Paint computeValue() {
                    return Color.hsb(hue.getValue(), 1.0, 1.0, clamp(alpha.get()/100));
                }
            });
        
            colorRectOverlayOne = new Rectangle(RECT_SIZE, RECT_SIZE);
            colorRectOverlayOne.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, 
                    new Stop(0, Color.rgb(255, 255, 255, 1)), 
                    new Stop(1, Color.rgb(255, 255, 255, 0))));
            colorRectOverlayOne.setStroke(Color.BLACK);
        
            EventHandler<MouseEvent> rectMouseHandler = new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    final double x = event.getX();
                    final double y = event.getY();
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
            colorBarIndicator.setLayoutX(CONTENT_PADDING+colorRect.getWidth()+13);
            colorBarIndicator.setLayoutY((CONTENT_PADDING+(colorBar.getHeight()*(hue.get() / 360))));
            colorBarIndicator.setArcWidth(4);
            colorBarIndicator.setArcHeight(4);
            colorBarIndicator.setStroke(Color.WHITE);
            colorBarIndicator.setEffect(new DropShadow(2, 0, 1, Color.BLACK));
            
            changeIsLocal = true;
            //Initialize Hue: (TopInsets-indicatorHeight/2 = 10 in calculation belows
            hue.set((10+colorBar.getHeight()-CONTENT_PADDING - RECT_SIZE)*360);
            //Initialize values sat, bright, color
            sat.set(((colorRectIndicator.getCenterX() - CONTENT_PADDING - 
                                colorRectIndicator.getRadius())*100)/RECT_SIZE);
            bright.set(((1 - (colorRectIndicator.getCenterY() - CONTENT_PADDING - 
                    colorRectIndicator.getRadius())/RECT_SIZE))*100);
            setColor(Color.hsb(hue.get(), clamp(sat.get() / 100), clamp(bright.get() / 100), 
                    clamp(alpha.get()/100)));
            red.set(doubleToInt(getColor().getRed()));
            green.set(doubleToInt(getColor().getGreen()));
            blue.set(doubleToInt(getColor().getBlue()));
            changeIsLocal = false;
            
            // *********************** Listeners ******************************
            hue.addListener(new ChangeListener<Number>() {
                @Override public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                    colorBarIndicator.setLayoutY((CONTENT_PADDING) + (RECT_SIZE * (hue.get() / 360)));
                }
            });
            sat.addListener(new ChangeListener<Number>() {
                @Override public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                    colorRectIndicator.setCenterX((CONTENT_PADDING + 
                            colorRectIndicator.getRadius()) + (RECT_SIZE * (sat.get() / 100)));
                }
            });
            bright.addListener(new ChangeListener<Number>() {
                @Override public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                    colorRectIndicator.setCenterY((CONTENT_PADDING + 
                            colorRectIndicator.getRadius()) + (RECT_SIZE * (1 - bright.get() / 100)));
                }
            });
            alpha.addListener(new ChangeListener<Number>() {
                @Override public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                    
                }
            });
            EventHandler<MouseEvent> barMouseHandler = new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    final double y = event.getY();
                    hue.set(clamp(y / RECT_SIZE) * 360);
                }
            };
            
            colorBar.setOnMouseDragged(barMouseHandler);
            colorBar.setOnMouseClicked(barMouseHandler);
            // create rectangle to capture mouse events to hide
        
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
        }
    }
    
    /* ------------------------------------------------------------------------*/
    
    enum ColorSettingsMode {
        HSB,
        RGB,
        WEB
    }
    
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
        GridPane hsbSettings;
        GridPane rgbSettings;
        GridPane webSettings;
        
        GridPane alphaSettings;
        HBox buttonBox;
        StackPane whiteBox;
        ColorSettingsMode colorSettingsMode = ColorSettingsMode.HSB;
        
        StackPane settingsPane = new StackPane();
        
        public ControlsPane() {
            getStyleClass().add("controls-pane");
            
            currentNewColorBorder = new Rectangle(CONTROLS_WIDTH, 18, null);
            currentNewColorBorder.setStroke(Color.BLACK);
            
            currentColorRect = new Rectangle(CONTROLS_WIDTH/2, 18);
            currentColorRect.setFill(currentColorProperty.get());
            currentColorProperty.addListener(new ChangeListener<Color>() {
                @Override public void changed(ObservableValue<? extends Color> ov, Color t, Color t1) {
                    currentColorRect.setFill(currentColorProperty.get());
                }
            });
            newColorRect = new Rectangle(CONTROLS_WIDTH/2, 18);
           
            updateNewColorFill();
            colorRectPane.color.addListener(new ChangeListener<Color>() {
                @Override
                public void changed(ObservableValue<? extends Color> ov, Color t, Color t1) {
                    updateNewColorFill();
                }
            });

            currentColorLabel = new Label("Current Color");
            newColorLabel = new Label("New Color");
            Rectangle spacer = new Rectangle(0, 18);
            
            whiteBox = new StackPane();
            whiteBox.getStyleClass().add("addcolor-controls-background");
            
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
            
            showHSBSettings(); // Color settings Grid Pane
            
            hsbButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent t) {
                    if (colorSettingsMode != ColorSettingsMode.HSB) {
                        colorSettingsMode = ColorSettingsMode.HSB;
                        showHSBSettings();
                        requestLayout();
                    }
                }
            });
            rgbButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent t) {
                    if (colorSettingsMode != ColorSettingsMode.RGB) {
                        colorSettingsMode = ColorSettingsMode.RGB;
                        showRGBSettings();
                        requestLayout();
                    }
                }
            });
            webButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent t) {
                    if (colorSettingsMode != ColorSettingsMode.WEB) {
                        colorSettingsMode = ColorSettingsMode.WEB;
                        showWebSettings();
                        requestLayout();
                    }
                }
            });
            
            hBox = new HBox();
            hBox.getChildren().addAll(hsbButton, rgbButton, webButton);
            
            currentAndNewColor = new GridPane();
            currentAndNewColor.add(currentColorLabel, 0, 0, 2, 1);
            currentAndNewColor.add(newColorLabel, 2, 0, 2, 1);
            currentAndNewColor.add(currentColorRect, 0, 1, 2, 1);
            currentAndNewColor.add(newColorRect, 2, 1, 2, 1);
            currentAndNewColor.add(spacer, 0, 2, 4, 1);
            
            // Color settings Grid Pane
            alphaSettings = new GridPane();
            alphaSettings.setHgap(5);
            alphaSettings.setVgap(0);
            alphaSettings.setManaged(false);
            alphaSettings.getStyleClass().add("alpha-settings");
//            alphaSettings.setGridLinesVisible(true);
            
            Rectangle spacer4 = new Rectangle(0, 4);
            alphaSettings.add(spacer4, 0, 0, 3, 1);
            
            Label alphaLabel = new Label("Alpha:       ");
            alphaLabel.setMinWidth(Control.USE_PREF_SIZE);
            alphaSettings.add(alphaLabel, 0, 1);
            
            Slider alphaSlider = new Slider(0, 100, 50);
//            alphaSlider.valueProperty().bind(colorRectPane.bright);
            alphaSettings.add(alphaSlider, 1, 1);
            
            IntegerField alphaField = new IntegerField();
            alphaField.setPrefColumnCount(6);
            alphaSettings.add(alphaField, 2, 1);
            
               
            alphaField.valueProperty().bindBidirectional(colorRectPane.alpha);
            alphaSlider.valueProperty().bindBidirectional(colorRectPane.alpha);
            
            Rectangle spacer5 = new Rectangle(0, 12);
            alphaSettings.add(spacer5, 0, 2, 3, 1);
            
            buttonBox = new HBox(4);
            Button addButton = new Button("Add");
            addButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent t) {
                    customColorProperty.set(Color.rgb(colorRectPane.red.get(), 
                            colorRectPane.green.get(), colorRectPane.blue.get(), 
                            clamp(colorRectPane.alpha.get() / 100)));
                    dialog.hide();
                }
            });
            
            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    dialog.hide();
                }
            });
            buttonBox.getChildren().addAll(addButton, cancelButton);
            
            getChildren().addAll(currentAndNewColor, currentNewColorBorder, whiteBox, 
                                            hBox, settingsPane, alphaSettings, buttonBox);
        }
        
        private void updateNewColorFill() {
            newColorRect.setFill(Color.hsb(colorRectPane.hue.getValue(), 
                clamp(colorRectPane.sat.getValue()/100), clamp(colorRectPane.bright.getValue()/100),
                clamp(colorRectPane.alpha.getValue()/100)));
        }
        
        private void showHSBSettings() {
            if (hsbSettings == null) {
                hsbSettings = new GridPane();
                hsbSettings.setHgap(5);
                hsbSettings.setVgap(4);
                hsbSettings.setManaged(false);
                
                Region spacer2 = new Region();
                spacer2.setPrefHeight(3);
                hsbSettings.add(spacer2, 0, 0, 3, 1);

                // Hue
                Label hueLabel = new Label("Hue:");
                hueLabel.setMinWidth(Control.USE_PREF_SIZE);
                hsbSettings.add(hueLabel, 0, 1);

                Slider hueSlider = new Slider(0, 360, 100);
                hsbSettings.add(hueSlider, 1, 1);

                IntegerField hueField = new IntegerField();
                hueField.setPrefColumnCount(6);
                hsbSettings.add(hueField, 2, 1);
                hueField.valueProperty().bindBidirectional(colorRectPane.hue);
                hueSlider.valueProperty().bindBidirectional(colorRectPane.hue);
                
                // Saturation
                Label saturationLabel = new Label("Saturation:");
                saturationLabel.setMinWidth(Control.USE_PREF_SIZE);
                hsbSettings.add(saturationLabel, 0, 2);

                Slider saturationSlider = new Slider(0, 100, 50);
                hsbSettings.add(saturationSlider, 1, 2);
                
                IntegerField saturationField = new IntegerField();
                saturationField.setPrefColumnCount(6);
                hsbSettings.add(saturationField, 2, 2);
                saturationField.valueProperty().bindBidirectional(colorRectPane.sat);
                saturationSlider.valueProperty().bindBidirectional(colorRectPane.sat);
                
                // Brightness
                Label brightnessLabel = new Label("Brightness:");
                brightnessLabel.setMinWidth(Control.USE_PREF_SIZE);
                hsbSettings.add(brightnessLabel, 0, 3);

                Slider brightnessSlider = new Slider(0, 100, 50);
                hsbSettings.add(brightnessSlider, 1, 3);

                IntegerField brightnessField = new IntegerField();
                brightnessField.setPrefColumnCount(6);
                hsbSettings.add(brightnessField, 2, 3);
//                colorRectPane.bright.bindBidirectional(brightnessSlider.valueProperty());
//                colorRectPane.bright.bindBidirectional(brightnessField.valueProperty());
                
                brightnessField.valueProperty().bindBidirectional(colorRectPane.bright);
                brightnessSlider.valueProperty().bindBidirectional(colorRectPane.bright);
                
                Region spacer3 = new Region();
                spacer3.setPrefHeight(4);
                hsbSettings.add(spacer3, 0, 4, 3, 1);
            }
            settingsPane.getChildren().setAll(hsbSettings);
        }
        
        
        private void showRGBSettings() {
            if (rgbSettings == null) {
                rgbSettings = new GridPane();
                rgbSettings.setHgap(5);
                rgbSettings.setVgap(4);
                rgbSettings.setManaged(false);
                
                Region spacer2 = new Region();
                spacer2.setPrefHeight(3);
                rgbSettings.add(spacer2, 0, 0, 3, 1);

                Label redLabel = new Label("Red:     ");
                redLabel.setMinWidth(Control.USE_PREF_SIZE);
                rgbSettings.add(redLabel, 0, 1);

                // Red ----------------------------------------
                Slider redSlider = new Slider(0, 100, 50);
                rgbSettings.add(redSlider, 1, 1);

                IntegerField redField = new IntegerField();
                redField.setPrefColumnCount(6);
                rgbSettings.add(redField, 2, 1);
                
                redField.valueProperty().bindBidirectional(colorRectPane.red);
                redSlider.valueProperty().bindBidirectional(colorRectPane.red);
                
                // Green ----------------------------------------
                Label greenLabel = new Label("Green:     ");
                greenLabel.setMinWidth(Control.USE_PREF_SIZE);
                rgbSettings.add(greenLabel, 0, 2);

                Slider greenSlider = new Slider(0, 100, 50);
                rgbSettings.add(greenSlider, 1, 2);

                IntegerField greenField = new IntegerField();
                greenField.setPrefColumnCount(6);
                rgbSettings.add(greenField, 2, 2);
                
                greenField.valueProperty().bindBidirectional(colorRectPane.green);
                greenSlider.valueProperty().bindBidirectional(colorRectPane.green);

                // Blue ----------------------------------------
                Label blueLabel = new Label("Blue:      ");
                blueLabel.setMinWidth(Control.USE_PREF_SIZE);
                rgbSettings.add(blueLabel, 0, 3);

                Slider blueSlider = new Slider(0, 100, 50);
                rgbSettings.add(blueSlider, 1, 3);

                IntegerField blueField = new IntegerField();
                blueField.setPrefColumnCount(6);
                rgbSettings.add(blueField, 2, 3);

                Region spacer3 = new Region();
                spacer3.setPrefHeight(4);
                rgbSettings.add(spacer3, 0, 4, 3, 1);
                
                blueField.valueProperty().bindBidirectional(colorRectPane.blue);
                blueSlider.valueProperty().bindBidirectional(colorRectPane.blue);
            }
            settingsPane.getChildren().setAll(rgbSettings);
        }
        
        private void showWebSettings() {
            if (webSettings == null) {
                webSettings = new GridPane();
                webSettings.setHgap(5);
                webSettings.setVgap(4);
                webSettings.setManaged(false);
                
                Region spacer2 = new Region();
                spacer2.setPrefHeight(3);
                webSettings.add(spacer2, 0, 0, 3, 1);

                Label webLabel = new Label("Web:        ");
                webLabel.setMinWidth(Control.USE_PREF_SIZE);
                webSettings.add(webLabel, 0, 1);

                WebColorField webField = new WebColorField();
                webField.valueProperty().bindBidirectional(colorRectPane.colorProperty());
                webField.setPrefColumnCount(6);
                webSettings.add(webField, 1, 1);
                
                Region spacer3 = new Region();
                spacer3.setPrefHeight(22);
                webSettings.add(spacer3, 0, 2, 3, 1);

                Region spacer4 = new Region();
                spacer4.setPrefHeight(22);
                webSettings.add(spacer4, 0, 3, 3, 1);

                Region spacer5 = new Region();
                spacer5.setPrefHeight(4);
                webSettings.add(spacer5, 0, 4, 3, 1);
            } 
            settingsPane.getChildren().setAll(webSettings);
        }
        
        public Label getCurrentColorLabel() {
            return currentColorLabel;
        }
        
        @Override public void layoutChildren() {
            double x = getInsets().getLeft();
            double y = getInsets().getTop();
//            double w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
//            double h = getHeight() - (getInsets().getTop() + getInsets().getBottom());
            currentAndNewColor.resizeRelocate(x,
                    y, CONTROLS_WIDTH, 18);
            currentNewColorBorder.relocate(x, 
                    y+controlsPane.currentColorLabel.prefHeight(-1));
            double hBoxX = computeXOffset(currentAndNewColor.prefWidth(-1), hBox.prefWidth(-1), HPos.CENTER);
            
            GridPane settingsGrid = (GridPane)settingsPane.getChildren().get(0);
            settingsGrid.resize(CONTROLS_WIDTH-28, settingsGrid.prefHeight(-1));
            
            double settingsHeight = settingsPane.getChildren().get(0).prefHeight(-1);
            
            whiteBox.resizeRelocate(x, y+currentAndNewColor.prefHeight(-1)+hBox.prefHeight(-1)/2, 
                    CONTROLS_WIDTH, settingsHeight+hBox.prefHeight(-1)/2+6);
            
            hBox.resizeRelocate(x+hBoxX, y+currentAndNewColor.prefHeight(-1), 
                    hBox.prefWidth(-1), hBox.prefHeight(-1));
            
            settingsPane.resizeRelocate(x+10, y+currentAndNewColor.prefHeight(-1)+hBox.prefHeight(-1)+5,
                    CONTROLS_WIDTH-28, settingsHeight);
            
            alphaSettings.resizeRelocate(x+10, 
                    y+currentAndNewColor.prefHeight(-1)+hBox.prefHeight(-1)+5+settingsHeight,
                    CONTROLS_WIDTH-28, alphaSettings.prefHeight(-1));
             
            double buttonBoxX = computeXOffset(currentAndNewColor.prefWidth(-1), buttonBox.prefWidth(-1), HPos.RIGHT);
            buttonBox.resizeRelocate(x+buttonBoxX, y+currentAndNewColor.prefHeight(-1)+hBox.prefHeight(-1)+5+
                    settingsHeight+alphaSettings.prefHeight(-1), buttonBox.prefWidth(-1), buttonBox.prefHeight(-1));
        }
        
        @Override public double computePrefHeight(double width) {
            double settingsHeight = settingsPane.getChildren().get(0).prefHeight(-1);
            return getInsets().getTop() + currentAndNewColor.prefHeight(width) +
                    currentNewColorBorder.prefHeight(width) + hBox.prefHeight(width) +
                    settingsHeight + alphaSettings.prefHeight(width) + 
                    buttonBox.prefHeight(width) + getInsets().getBottom();
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
    
    private static int doubleToInt(double value) {
        return new Double(value*255).intValue();
    }
}
