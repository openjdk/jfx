/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import com.sun.javafx.Utils;
import javafx.geometry.Insets;
import javafx.scene.input.KeyEvent;
/**
 *
 * @author paru
 */
public class CustomColorDialog extends StackPane {
    
    private static final int CONTENT_PADDING = 10;
    private static final int RECT_SIZE = 200;
    private static final int CONTROLS_WIDTH = 256;
    private static final int COLORBAR_GAP = 9;
    private static final int LABEL_GAP = 2;
    
    final Stage dialog = new Stage();
    ColorRectPane colorRectPane;
    ControlsPane controlsPane;
    
    Circle colorRectIndicator;
    Rectangle colorRect;
    Rectangle colorRectOverlayOne;
    Rectangle colorRectOverlayTwo;
    Rectangle colorBar;
    Rectangle colorBarIndicator;
    
    private Color currentColor = Color.WHITE;
    ObjectProperty<Color> customColorProperty = new SimpleObjectProperty<Color>(Color.TRANSPARENT);
    boolean saveCustomColor = false;
    boolean useCustomColor = false;
    Button saveButton;
    Button useButton;
    
    private WebColorField webField = null;
    private Scene customScene;
    
    public CustomColorDialog(Window owner) {
        getStyleClass().add("custom-color-dialog");
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Custom Colors..");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        colorRectPane = new ColorRectPane();
        controlsPane = new ControlsPane();
        
        customScene = new Scene(this);
        getChildren().addAll(colorRectPane, controlsPane);
        
        dialog.setScene(customScene);
        dialog.addEventHandler(KeyEvent.ANY, keyEventListener);
    }
    
    private final EventHandler<KeyEvent> keyEventListener = new EventHandler<KeyEvent>() {
        @Override public void handle(KeyEvent e) {
            switch (e.getCode()) {
                case ESCAPE :
                    dialog.setScene(null);
                    dialog.close();
            default:
                break;
            }
        }
    };
    
    public void setCurrentColor(Color currentColor) {
        this.currentColor = currentColor;
        controlsPane.currentColorRect.setFill(currentColor);
    }
    
    public void show(double x, double y) {
        if (x != 0 && y != 0) {
            dialog.setX(x);
            dialog.setY(y);
        }
        if (dialog.getScene() == null) dialog.setScene(customScene);
        colorRectPane.updateValues();
        dialog.show();
    }
    
    @Override public void layoutChildren() {
        double x = getInsets().getLeft();
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
            default:
                return 0;
        }
       
    }
    
    /* ------------------------------------------------------------------------*/
    
    class ColorRectPane extends StackPane {
        
        private boolean changeIsLocal = false;
        DoubleProperty hue = new SimpleDoubleProperty(-1) {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    updateHSBColor();
                    changeIsLocal = false;
                }
            }
        };
        DoubleProperty sat = new SimpleDoubleProperty(-1) {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    updateHSBColor();
                    changeIsLocal = false;
                }
            }
        };
        DoubleProperty bright = new SimpleDoubleProperty(-1) {
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

        IntegerProperty red = new SimpleIntegerProperty(-1) {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    updateRGBColor();
                    changeIsLocal = false;
                }
            }
        };
        
        IntegerProperty green = new SimpleIntegerProperty(-1) {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    updateRGBColor();
                    changeIsLocal = false;
                }
            }
        };
        
        IntegerProperty blue = new SimpleIntegerProperty(-1) {
            @Override protected void invalidated() {
                if (!changeIsLocal) {
                    changeIsLocal = true;
                    updateRGBColor();
                    changeIsLocal = false;
                }
            }
        };
        
        DoubleProperty alpha = new SimpleDoubleProperty(100) {
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
            Color newColor = Color.rgb(red.get(), green.get(), blue.get(), clamp(alpha.get() / 100));
            hue.set(newColor.getHue());
            sat.set(newColor.getSaturation() * 100);
            bright.set(newColor.getBrightness() * 100);
            setColor(newColor);
        }
        
        private void updateHSBColor() {
            Color newColor = Color.hsb(hue.get(), clamp(sat.get() / 100), 
                            clamp(bright.get() / 100), clamp(alpha.get() / 100));
            red.set(doubleToInt(newColor.getRed()));
            green.set(doubleToInt(newColor.getGreen()));
            blue.set(doubleToInt(newColor.getBlue()));
            setColor(newColor);
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
            colorProperty().addListener(new ChangeListener<Color>() {
                @Override
                public void changed(ObservableValue<? extends Color> ov, Color t, Color t1) {
                    colorRect.setFill(Color.hsb(hue.getValue(), 1.0, 1.0, clamp(alpha.get()/100)));
                }
            });
            
            colorRectOverlayOne = new Rectangle(RECT_SIZE, RECT_SIZE);
            colorRectOverlayOne.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, 
                    new Stop(0, Color.rgb(255, 255, 255, 1)), 
                    new Stop(1, Color.rgb(255, 255, 255, 0))));
            colorRectOverlayOne.setStroke(Utils.deriveColor(Color.web("#d0d0d0"), -20/100));
        
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
            colorBar.setStroke(Utils.deriveColor(Color.web("#d0d0d0"), -20/100));

            colorBarIndicator = new Rectangle(24, 10, null);
            colorBarIndicator.setLayoutX(CONTENT_PADDING+colorRect.getWidth()+13);
            colorBarIndicator.setLayoutY((CONTENT_PADDING+(colorBar.getHeight()*(hue.get() / 360))));
            colorBarIndicator.setArcWidth(4);
            colorBarIndicator.setArcHeight(4);
            colorBarIndicator.setStroke(Color.WHITE);
            colorBarIndicator.setEffect(new DropShadow(2, 0, 1, Color.BLACK));
            
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
        
        private void updateValues() {
            changeIsLocal = true;
            //Initialize hue, sat, bright, color, red, green and blue
            hue.set(currentColor.getHue());
            sat.set(currentColor.getSaturation()*100);
            bright.set(currentColor.getBrightness()*100);
            setColor(Color.hsb(hue.get(), clamp(sat.get() / 100), clamp(bright.get() / 100), 
                    clamp(alpha.get()/100)));
            red.set(doubleToInt(getColor().getRed()));
            green.set(doubleToInt(getColor().getGreen()));
            blue.set(doubleToInt(getColor().getBlue()));
            changeIsLocal = false;
        }
        
        @Override public void layoutChildren() {
            double x = getInsets().getLeft();
            double y = getInsets().getTop();
//            double w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
//            double h = getHeight() - (getInsets().getTop() + getInsets().getBottom());
            colorRect.relocate(x, y);
            colorRectOverlayOne.relocate(x, y);
            colorRectOverlayTwo.relocate(x, y);
            
            colorBar.relocate(x+colorRect.prefWidth(-1) + COLORBAR_GAP, y);
        }
        
        @Override public double computePrefWidth(double height) {
            return getInsets().getLeft() + colorRect.prefWidth(-1) + COLORBAR_GAP +
                    colorBar.prefWidth(-1) + (colorBarIndicator.getBoundsInParent().getWidth() - colorBar.prefWidth(-1))
                    + getInsets().getRight();
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
        StackPane currentTransparent; // for opacity
        StackPane newTransparent; // for opacity
        GridPane currentAndNewColor;
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
            currentNewColorBorder.setStroke(Utils.deriveColor(Color.web("#d0d0d0"), -20/100));
            currentNewColorBorder.setStrokeWidth(2);
            
            currentTransparent = new StackPane();
            currentTransparent.setPrefSize(CONTROLS_WIDTH/2, 18);
            currentTransparent.setId("transparent-current");
            
            newTransparent = new StackPane();
            newTransparent.setPrefSize(CONTROLS_WIDTH/2, 18);
            newTransparent.setId("transparent-new");
            
            currentColorRect = new Rectangle(CONTROLS_WIDTH/2, 18);
            currentColorRect.setFill(currentColor);

            newColorRect = new Rectangle(CONTROLS_WIDTH/2, 18);
            
            updateNewColorFill();
            colorRectPane.color.addListener(new ChangeListener<Color>() {
                @Override
                public void changed(ObservableValue<? extends Color> ov, Color t, Color t1) {
                    updateNewColorFill();
                    customColorProperty.set(Color.hsb(colorRectPane.hue.getValue(), 
                        clamp(colorRectPane.sat.getValue()/100), 
                        clamp(colorRectPane.bright.getValue()/100),
                        clamp(colorRectPane.alpha.getValue()/100)));
                }
            });

            currentColorLabel = new Label("Current Color");
            newColorLabel = new Label("New Color");
            Rectangle spacer = new Rectangle(0, 12);
            
            whiteBox = new StackPane();
            whiteBox.getStyleClass().add("customcolor-controls-background");
            
            hsbButton = new ToggleButton("HSB");
            hsbButton.setId("toggle-button-left");
            rgbButton = new ToggleButton("RGB");
            rgbButton.setId("toggle-button-center");
            webButton = new ToggleButton("Web");
            webButton.setId("toggle-button-right");
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
            currentAndNewColor.getStyleClass().add("current-new-color-grid");
            currentAndNewColor.add(currentColorLabel, 0, 0, 2, 1);
            currentAndNewColor.add(newColorLabel, 2, 0, 2, 1);
            Region r = new Region();
            r.setPadding(new Insets(1, 128, 1, 128));
            currentAndNewColor.add(r, 0, 1, 4, 1);
            currentAndNewColor.add(currentTransparent, 0, 2, 2, 1);
            currentAndNewColor.add(currentColorRect, 0, 2, 2, 1);
            currentAndNewColor.add(newTransparent, 2, 2, 2, 1);
            currentAndNewColor.add(newColorRect, 2, 2, 2, 1);
            currentAndNewColor.add(spacer, 0, 3, 4, 1);
            
            // Color settings Grid Pane
            alphaSettings = new GridPane();
            alphaSettings.setHgap(5);
            alphaSettings.setVgap(0);
            alphaSettings.setManaged(false);
            alphaSettings.getStyleClass().add("alpha-settings");
//            alphaSettings.setGridLinesVisible(true);
            
            Rectangle spacer4 = new Rectangle(0, 12);
            alphaSettings.add(spacer4, 0, 0, 3, 1);
            
            Label alphaLabel = new Label("Opacity:");
            alphaLabel.setPrefWidth(68);
            alphaSettings.add(alphaLabel, 0, 1);
            
            Slider alphaSlider = new Slider(0, 100, 50);
            alphaSlider.setPrefWidth(100);
            alphaSettings.add(alphaSlider, 1, 1);
            
            IntegerField alphaField = new IntegerField(100);
            alphaField.setSkin(new IntegerFieldSkin(alphaField));
            alphaField.setPrefColumnCount(3);
            alphaField.setMaxWidth(38);
            alphaSettings.add(alphaField, 2, 1);
            
               
            alphaField.valueProperty().bindBidirectional(colorRectPane.alpha);
            alphaSlider.valueProperty().bindBidirectional(colorRectPane.alpha);
            
            Rectangle spacer5 = new Rectangle(0, 15);
            alphaSettings.add(spacer5, 0, 2, 3, 1);
            
            buttonBox = new HBox(4);
            
            saveButton = new Button("Save");
            saveButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent t) {
                    saveCustomColor = true;
                    if (colorSettingsMode == ColorSettingsMode.WEB) {
                        customColorProperty.set(webField.valueProperty().get());
                    } else {
                        customColorProperty.set(Color.rgb(colorRectPane.red.get(), 
                            colorRectPane.green.get(), colorRectPane.blue.get(), 
                            clamp(colorRectPane.alpha.get() / 100)));
                    }
                    dialog.hide();
                    saveCustomColor = false;
                }
            });
            
            useButton = new Button("Use");
            useButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent t) {
                    useCustomColor = true;
                    customColorProperty.set(Color.rgb(colorRectPane.red.get(), 
                            colorRectPane.green.get(), colorRectPane.blue.get(), 
                            clamp(colorRectPane.alpha.get() / 100)));
                    dialog.hide();
                    useCustomColor = false;
                }
            });
            
            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    customColorProperty.set(currentColor);
                    dialog.hide();
                }
            });
            buttonBox.getChildren().addAll(saveButton, useButton, cancelButton);
            
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
                hueLabel.setMinWidth(68);
                hsbSettings.add(hueLabel, 0, 1);

                Slider hueSlider = new Slider(0, 360, 100);
                hueSlider.setPrefWidth(100);
                hsbSettings.add(hueSlider, 1, 1);

                IntegerField hueField = new IntegerField(360);
                hueField.setSkin(new IntegerFieldSkin(hueField));
                hueField.setPrefColumnCount(3);
                hueField.setMaxWidth(38);
                hsbSettings.add(hueField, 2, 1);
                hueField.valueProperty().bindBidirectional(colorRectPane.hue);
                hueSlider.valueProperty().bindBidirectional(colorRectPane.hue);
                
                // Saturation
                Label saturationLabel = new Label("Saturation:");
                saturationLabel.setMinWidth(68);
                hsbSettings.add(saturationLabel, 0, 2);

                Slider saturationSlider = new Slider(0, 100, 50);
                saturationSlider.setPrefWidth(100);
                hsbSettings.add(saturationSlider, 1, 2);
                
                IntegerField saturationField = new IntegerField(100);
                saturationField.setSkin(new IntegerFieldSkin(saturationField));
                saturationField.setPrefColumnCount(3);
                saturationField.setMaxWidth(38);
                hsbSettings.add(saturationField, 2, 2);
                saturationField.valueProperty().bindBidirectional(colorRectPane.sat);
                saturationSlider.valueProperty().bindBidirectional(colorRectPane.sat);
                
                // Brightness
                Label brightnessLabel = new Label("Brightness:");
                brightnessLabel.setMinWidth(68);
                hsbSettings.add(brightnessLabel, 0, 3);

                Slider brightnessSlider = new Slider(0, 100, 50);
                brightnessSlider.setPrefWidth(100);
                hsbSettings.add(brightnessSlider, 1, 3);

                IntegerField brightnessField = new IntegerField(100);
                brightnessField.setSkin(new IntegerFieldSkin(brightnessField));
                brightnessField.setPrefColumnCount(3);
                brightnessField.setMaxWidth(38);
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

                Label redLabel = new Label("Red:");
                redLabel.setMinWidth(68);
                rgbSettings.add(redLabel, 0, 1);

                // Red ----------------------------------------
                Slider redSlider = new Slider(0, 255, 100);
                redSlider.setPrefWidth(100);
                rgbSettings.add(redSlider, 1, 1);

                IntegerField redField = new IntegerField(255);
                redField.setSkin(new IntegerFieldSkin(redField));
//                redField.setPrefColumnCount(3);
                redField.setMaxWidth(38);
                rgbSettings.add(redField, 2, 1);
                
                redField.valueProperty().bindBidirectional(colorRectPane.red);
                redSlider.valueProperty().bindBidirectional(colorRectPane.red);
                
                // Green ----------------------------------------
                Label greenLabel = new Label("Green:     ");
                greenLabel.setMinWidth(68);
                rgbSettings.add(greenLabel, 0, 2);

                Slider greenSlider = new Slider(0, 255, 100);
                greenSlider.setPrefWidth(100);
                rgbSettings.add(greenSlider, 1, 2);

                IntegerField greenField = new IntegerField(255);
                greenField.setSkin(new IntegerFieldSkin(greenField));
//                greenField.setPrefColumnCount(3);
                greenField.setMaxWidth(38);
                rgbSettings.add(greenField, 2, 2);
                
                greenField.valueProperty().bindBidirectional(colorRectPane.green);
                greenSlider.valueProperty().bindBidirectional(colorRectPane.green);

                // Blue ----------------------------------------
                Label blueLabel = new Label("Blue:      ");
//                blueLabel.setMinWidth(Control.USE_PREF_SIZE);
                blueLabel.setMinWidth(68);
                rgbSettings.add(blueLabel, 0, 3);

                Slider blueSlider = new Slider(0, 255, 100);
                blueSlider.setPrefWidth(100);
                rgbSettings.add(blueSlider, 1, 3);

                IntegerField blueField = new IntegerField(255);
                blueField.setSkin(new IntegerFieldSkin(blueField));
//                blueField.setPrefColumnCount(3);
                blueField.setMaxWidth(38);
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

                webField = new WebColorField();
                webField.setSkin(new WebColorFieldSkin(webField));
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
                    y+controlsPane.currentColorLabel.prefHeight(-1)+LABEL_GAP); 
            double hBoxX = computeXOffset(currentAndNewColor.prefWidth(-1), hBox.prefWidth(-1), HPos.CENTER);
            
            GridPane settingsGrid = (GridPane)settingsPane.getChildren().get(0);
            settingsGrid.resize(CONTROLS_WIDTH-28, settingsGrid.prefHeight(-1));
            
            double settingsHeight = settingsPane.getChildren().get(0).prefHeight(-1);
            
            whiteBox.resizeRelocate(x, y+currentAndNewColor.prefHeight(-1)+hBox.prefHeight(-1)/2, 
                    CONTROLS_WIDTH, settingsHeight+hBox.prefHeight(-1)/2);
            
            hBox.resizeRelocate(x+hBoxX, y+currentAndNewColor.prefHeight(-1), 
                    hBox.prefWidth(-1), hBox.prefHeight(-1));
            
            settingsPane.resizeRelocate(x+10, y+currentAndNewColor.prefHeight(-1)+hBox.prefHeight(-1),
                    CONTROLS_WIDTH-28, settingsHeight);
            
            alphaSettings.resizeRelocate(x+10, 
                    y+currentAndNewColor.prefHeight(-1)+hBox.prefHeight(-1)+settingsHeight,
                    CONTROLS_WIDTH-28, alphaSettings.prefHeight(-1));
             
            double buttonBoxX = computeXOffset(currentAndNewColor.prefWidth(-1), buttonBox.prefWidth(-1), HPos.RIGHT);
            buttonBox.resizeRelocate(x+buttonBoxX, y+currentAndNewColor.prefHeight(-1)+hBox.prefHeight(-1)+
                    settingsHeight+alphaSettings.prefHeight(-1), buttonBox.prefWidth(-1), buttonBox.prefHeight(-1));
        }
        
        @Override public double computePrefHeight(double width) {
            double settingsHeight = settingsPane.getChildren().get(0).prefHeight(-1);
            return getInsets().getTop() + currentAndNewColor.prefHeight(-1) +
                    hBox.prefHeight(-1) + settingsHeight + 
                    alphaSettings.prefHeight(-1) + buttonBox.prefHeight(-1) +
                    getInsets().getBottom();
            
        }
        
        @Override public double computePrefWidth(double height) {
            return getInsets().getLeft() + CONTROLS_WIDTH + getInsets().getRight();
        }
    }
    
    static double clamp(double value) {
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
        // RT-27731 regression : reverting back to this - even though findbugs may complain.
        return new Double(value*255).intValue();
    }
}
