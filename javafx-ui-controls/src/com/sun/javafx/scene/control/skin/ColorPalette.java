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

import javafx.css.PseudoClass;
import javafx.scene.control.ColorPicker;
import java.util.List;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Window;


public class ColorPalette extends VBox {

    private static final int SQUARE_SIZE = 15;
    private static final int NUM_OF_COLUMNS = 12;
    private static final int NUM_OF_ROWS = 10;
    private static final int LABEL_GAP = 2;
    
    private boolean customColorAdded = false;
    ColorPickerGrid colorPickerGrid;
    ColorPicker colorPicker;
    GridPane customColorGrid = new GridPane();
    Hyperlink customColorLink = new Hyperlink("Custom Color..");
    Separator separator = new Separator();
    Window owner;
    Label customColorLabel = new Label("Custom Colors");
    CustomColorDialog customColorDialog = null;
    private final List<ColorSquare> customSquares = FXCollections.observableArrayList();
 
    private double x;
    private double y;
    private PopupControl popupControl;
    private ColorSquare focusedSquare;
    private ContextMenu contextMenu = null;
    
    private Color mouseDragColor = null;
    private boolean dragDetected = false;
    
    public ColorPalette(Color initPaint, final ColorPicker colorPicker) {
        getStyleClass().add("color-palette");
        this.colorPicker = colorPicker;
        owner = colorPicker.getScene().getWindow();
        colorPickerGrid = new ColorPickerGrid(initPaint);
        colorPickerGrid.requestFocus();
        colorPickerGrid.setFocusTraversable(true);
        customColorLabel.setAlignment(Pos.CENTER_LEFT);
        customColorLink.setPrefWidth(colorPickerGrid.prefWidth(-1));
        customColorLink.setAlignment(Pos.CENTER);
        customColorLink.setFocusTraversable(true);
        customColorLink.setVisited(true); // so that it always appears blue 
        // ColorPalette should hide when save or use button is pressed in CustomColorDialog.
        final EventHandler<ActionEvent> actionListener = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) {
                colorPicker.hide();
            }
        };
        customColorLink.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) {
                if (customColorDialog == null) {
                    customColorDialog = new CustomColorDialog(owner);
                    customColorDialog.customColorProperty().addListener(new ChangeListener<Color>() {
                        @Override public void changed(ObservableValue<? extends Color> ov, 
                                                                Color t, Color t1) {
                            Color customColor = customColorDialog.customColorProperty().get();
                            if (customColorDialog.isSaveCustomColor()) {
                                ColorSquare cs = new ColorSquare(customColor, true);
                                customSquares.add(cs);
                                buildCustomColors();
                                colorPicker.getCustomColors().add(customColor);
                                updateSelection(customColor);
                            }
                            if (customColorDialog.isSaveCustomColor() || customColorDialog.isUseCustomColor()) {
                                Event.fireEvent(colorPicker, new ActionEvent());
                            }             
                            colorPicker.setValue(customColor);
                        }
                    });
                    customColorDialog.getSaveButton().addEventHandler(ActionEvent.ACTION, actionListener);
                    customColorDialog.getUseButton().addEventHandler(ActionEvent.ACTION, actionListener);
                }
                customColorDialog.setCurrentColor(colorPicker.valueProperty().get());
                if (popupControl != null) popupControl.setAutoHide(false);
                customColorDialog.show(x, y);
                if (popupControl != null) popupControl.setAutoHide(true);
            }
        });
        
        initNavigation();
        customColorGrid.getStyleClass().add("color-picker-grid");
        customColorGrid.setVisible(false);
        customColorGrid.setFocusTraversable(true);
        for (Color c : colorPicker.getCustomColors()) {
            customSquares.add(new ColorSquare(c, true));
        }
        buildCustomColors();
        colorPicker.getCustomColors().addListener(new ListChangeListener<Color>() {
            @Override public void onChanged(Change<? extends Color> change) {
                customSquares.clear();
                for (Color c : colorPicker.getCustomColors()) {
                    customSquares.add(new ColorSquare(c, true));
                }
                buildCustomColors();
            }
        });
        getChildren().addAll(colorPickerGrid, customColorLabel, customColorGrid, separator, customColorLink);
    }
    
    private void buildCustomColors() {
        int customColumnIndex = 0; 
        int customRowIndex = 0;
        int remainingSquares = customSquares.size()%NUM_OF_COLUMNS;
        int numEmpty = (remainingSquares == 0) ? 0 : NUM_OF_COLUMNS - remainingSquares;

        customColorGrid.getChildren().clear();
        if (customSquares.isEmpty()) {
            customColorLabel.setVisible(false);
            customColorLabel.setManaged(false);
            customColorGrid.setVisible(false);
            customColorGrid.setManaged(false);
            return;
        } else {
            customColorLabel.setVisible(true);
            customColorLabel.setManaged(true);
            customColorGrid.setVisible(true);
            customColorGrid.setManaged(true);
            if (contextMenu == null) {
                MenuItem item = new MenuItem("Remove Color");
                item.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        ColorSquare square = (ColorSquare)contextMenu.getOwnerNode();
                        colorPicker.getCustomColors().remove(square.rectangle.getFill());
                        customSquares.remove(square);
                        buildCustomColors();
                    }
                });
                contextMenu = new ContextMenu(item);
            }
        }
        for(ColorSquare square : customSquares) {
            customColorGrid.add(square, customColumnIndex, customRowIndex);
                customColumnIndex++;
                if (customColumnIndex == NUM_OF_COLUMNS) {
                    customColumnIndex = 0;
                    customRowIndex++;
                }
        }
        for (int i = 0; i < numEmpty; i++) {            
            ColorSquare emptySquare = new ColorSquare(null);
            customColorGrid.add(emptySquare, customColumnIndex, customRowIndex);
            customColumnIndex++;
        }
        requestLayout();
    }
    
    private void initNavigation() {
        setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent ke) {
                switch (ke.getCode()) {
                    case LEFT:
                        if (getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                            processRightKey(ke);
                        } else {
                            processLeftKey(ke);
                        }
                        break;
                    case RIGHT:
                        if (getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                            processLeftKey(ke);
                        } else {
                            processRightKey(ke);
                        }
                        break;
                    case UP:
                        processUpKey(ke);
                        break;
                    case DOWN:
                        processDownKey(ke);
                        break;
                    case ENTER:
                        processSelectKey(ke);
                        break;
                    default: // no-op
                }
            }
        });
    }
    
    private void processSelectKey(KeyEvent ke) {
        if (focusedSquare != null) focusedSquare.selectColor(ke);
    }
    
    private void processLeftKey(KeyEvent ke) {
        int index;
        for (index = (NUM_OF_ROWS*NUM_OF_COLUMNS)-1; index >= 0; index--)  {
            ColorSquare cs = colorPickerGrid.getSquares().get(index);
            if (cs == focusedSquare) {
                ColorSquare prevSquare = colorPickerGrid.getSquares().get((index != 0) ? 
                                    (index-1) : (NUM_OF_ROWS*NUM_OF_COLUMNS)-1);
                prevSquare.requestFocus();
                focusedSquare = prevSquare;
                return;
            } 
        }
        // check custom colors
        int len = customColorGrid.getChildren().size();
        for (index = len-1; index >= 0; index--) {
            ColorSquare cs = (ColorSquare)customColorGrid.getChildren().get(index);
            if (cs == focusedSquare) {
                ColorSquare prevSquare = (ColorSquare)customColorGrid.getChildren().get((index != 0) ? 
                                    (index-1) : len-1);
                prevSquare.requestFocus();
                focusedSquare = prevSquare;
                return;
            } 
        }
        if (index == -1) {
            ColorSquare cs = colorPickerGrid.getSquares().get((NUM_OF_ROWS*NUM_OF_COLUMNS)-1);
            focusedSquare = cs;
            cs.requestFocus();;
        }
    }
    
     private void processUpKey(KeyEvent ke) {
        int index;
        for (index = (NUM_OF_ROWS*NUM_OF_COLUMNS)-1; index >= 0; index--)  {
            ColorSquare cs = colorPickerGrid.getSquares().get(index);
            if (cs == focusedSquare) {
                ColorSquare prevSquare = colorPickerGrid.getSquares().get((index-12 >= 0)? 
                        (index-12) : ((NUM_OF_ROWS-1)*NUM_OF_COLUMNS)+index);
                prevSquare.requestFocus();
                focusedSquare = prevSquare;
                return;
            } 
        }
        int len = customColorGrid.getChildren().size();
        for (index = len-1; index >= 0; index--) {
            // check custom colors
            ColorSquare cs = (ColorSquare)customColorGrid.getChildren().get(index);
            ColorSquare prevSquare = null;
            if (cs == focusedSquare) {
                if (index -12 >= 0) {
                    prevSquare = (ColorSquare)customColorGrid.getChildren().get(index-12);
                } else {
                    int rowIndex = GridPane.getRowIndex(customColorGrid.getChildren().get(len-1));
                    prevSquare = (ColorSquare)customColorGrid.getChildren().get((rowIndex*NUM_OF_COLUMNS)+index);
                }
                prevSquare.requestFocus();
                focusedSquare = prevSquare;
                return;
            }
        }
        if (index == -1) {
            ColorSquare cs = colorPickerGrid.getSquares().get((NUM_OF_ROWS*NUM_OF_COLUMNS)-1);
            focusedSquare = cs;
            focusedSquare.requestFocus();
        }
    }
     
    private void processRightKey(KeyEvent ke) {
        int index;
        for (index = 0; index < (NUM_OF_ROWS*NUM_OF_COLUMNS); index++)  {
            ColorSquare cs = colorPickerGrid.getSquares().get(index);
            if (cs == focusedSquare) {
                ColorSquare prevSquare = colorPickerGrid.getSquares().get(
                        (index != (NUM_OF_ROWS*NUM_OF_COLUMNS)-1) ? (index+1) : 0);
                prevSquare.requestFocus();
                focusedSquare = prevSquare;
                return;
            } 
        }
        // check custom colors
        int len = customColorGrid.getChildren().size();
        for (index = 0; index < len; index++) {
            ColorSquare cs = (ColorSquare)customColorGrid.getChildren().get(index);
            if (cs == focusedSquare) {
                ColorSquare prevSquare = (ColorSquare)customColorGrid.getChildren().get(
                        (index != len-1) ? (index+1) : 0);
                prevSquare.requestFocus();
                focusedSquare = prevSquare;
                return;
            } 
        }
        if (index == len) {
            ColorSquare cs = colorPickerGrid.getSquares().get(0);
            focusedSquare = cs;
            focusedSquare.requestFocus();
        }
    }
    
     private void processDownKey(KeyEvent ke) {
        int index;
        for (index = 0; index < (NUM_OF_ROWS*NUM_OF_COLUMNS); index++)  {
            ColorSquare cs = colorPickerGrid.getSquares().get(index);
            if (cs == focusedSquare) {
                ColorSquare prevSquare = colorPickerGrid.getSquares().get((index+12 < NUM_OF_ROWS*NUM_OF_COLUMNS)? 
                        (index+12) : index-((NUM_OF_ROWS-1)*NUM_OF_COLUMNS));
                prevSquare.requestFocus();
                focusedSquare = prevSquare;
                return;
            } 
        }
        // check custom colors
        int len = customColorGrid.getChildren().size();
        for (index = 0; index < len; index++) {
            ColorSquare cs = (ColorSquare)customColorGrid.getChildren().get(index);
            ColorSquare prevSquare = null;
            if (cs == focusedSquare) {
                if (index+12 < len) {
                    prevSquare = (ColorSquare)customColorGrid.getChildren().get(index+12);
                } else {
                    int rowIndex = GridPane.getRowIndex(customColorGrid.getChildren().get(len-1));
                    prevSquare = (ColorSquare)customColorGrid.getChildren().get(index-(rowIndex)*NUM_OF_COLUMNS);
                }
                prevSquare.requestFocus();
                focusedSquare = prevSquare;
                return;
            } 
        }
        if (index == len) {
            ColorSquare cs = colorPickerGrid.getSquares().get(0);
            focusedSquare.requestFocus();
            focusedSquare = cs;
        }
    }
    
    public void setPopupControl(PopupControl pc) {
        this.popupControl = pc;
    }
    
    public void setDialogLocation(double xValue, double yValue) {
        x = xValue;
        y = yValue;
    }
    
    public ColorPickerGrid getColorGrid() {
        return colorPickerGrid;
    }
//
//    @Override protected void layoutChildren() {
//        double x = getInsets().getLeft();
//        double y = getInsets().getTop();
////        double popupWidth = cpg.prefWidth(-1) + paddingX+getInsets().getRight();
////        double popupHeight = cpg.prefHeight(-1) + getInsets().getTop() + getInsets().getBottom();
//        colorPickerGrid.relocate(x, y);
//        y = y+colorPickerGrid.prefHeight(-1)+GAP;
//        if (customColorAdded) {
//            if (customColorLabel.isVisible()) {
//                customColorLabel.resizeRelocate(x, y, colorPickerGrid.prefWidth(-1), customColorLabel.prefHeight(y));
//                y = y+customColorLabel.prefHeight(-1)+LABEL_GAP;
//            }
//            customColorGrid.relocate(x, y);
//            y = y+customColorGrid.prefHeight(-1)+GAP;
//        }
//        separator.resizeRelocate(x, y, colorPickerGrid.prefWidth(-1), separator.prefHeight(-1));
//        y = y+separator.prefHeight(-1)+GAP;
//        customColorLink.resizeRelocate(x, y, colorPickerGrid.prefWidth(-1), customColorLink.prefHeight(-1));
//    }
//
//    @Override protected double computePrefWidth(double height) {
//        return getInsets().getLeft() + colorPickerGrid.prefWidth(-1) + getInsets().getRight();
//    }
//
//    @Override protected double computePrefHeight(double width) {
//        double totalHeight = colorPickerGrid.prefHeight(-1) + GAP +
//                ((customColorAdded) ?
//                (customColorGrid.prefHeight(-1)+customColorLabel.prefHeight(-1))+LABEL_GAP+GAP : 0) +
//                separator.prefHeight(-1) + GAP + customColorLink.prefHeight(-1);
//        return getInsets().getTop() + totalHeight + getInsets().getBottom();
//    }
//
    public boolean isCustomColorDialogShowing() {
        if (customColorDialog != null) return customColorDialog.isVisible();
        return false;
    }

    class ColorSquare extends StackPane {
        Rectangle rectangle;
        boolean isCustom = false;
        boolean isEmpty = false;
        public ColorSquare(Color color) {
            this(color, false);
        }
        public ColorSquare(Color color, boolean value) {
            // Add style class to handle selected color square
            getStyleClass().add("color-square");
            setFocusTraversable(true);
            this.isCustom = value;
            rectangle = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
            setFocusTraversable(true);
            if (color == null) {
                rectangle.setFill(Color.WHITE );
                isEmpty = true;
            }
            else {
                rectangle.setFill(color);
            }
//            setFill(color);
            rectangle.setSmooth(false);
//            Utils.setBlocksMouse(this, true);
            
            rectangle.setStrokeType(StrokeType.INSIDE);
            String tooltipStr = ColorPickerSkin.colorValueToWeb(color);
            Tooltip.install(this, new Tooltip((tooltipStr == null) ? "" : tooltipStr));
          
            rectangle.getStyleClass().add("color-rect");

            addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    toFront();
                }
            });
            addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    if (!dragDetected && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                        if (!isEmpty) {
                            Color fill = (Color)rectangle.getFill();
                            colorPicker.setValue(fill);
                            colorPicker.fireEvent(new ActionEvent());
                            updateSelection(fill);
                            event.consume();
                        }
                        colorPicker.hide();
                    } else if (event.getButton() == MouseButton.SECONDARY ||
                            event.getButton() == MouseButton.MIDDLE) {
                        if (isCustom && contextMenu != null) {
                            if (!contextMenu.isShowing()) {
                                contextMenu.show(ColorSquare.this, Side.RIGHT, 0, 0);
                                Utils.addMnemonics(contextMenu, ColorSquare.this.getScene());
                            }
                            else {
                                contextMenu.hide();
                                Utils.removeMnemonics(contextMenu, ColorSquare.this.getScene());
                            }
                        }
                    }
                }
            });
            focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                }
            });
            addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    focusedSquare = ColorSquare.this;
                    focusedSquare.requestFocus();
                }
            });
            getChildren().add(rectangle);
        }
        
        public void selectColor(KeyEvent event) {
            if (rectangle.getFill() != null) {
                if (rectangle.getFill() instanceof Color) {
                    colorPicker.setValue((Color) rectangle.getFill());
                    colorPicker.fireEvent(new ActionEvent());
                }
                event.consume();
            }
            colorPicker.hide();
        }
        
        private ReadOnlyBooleanWrapper selected;
        protected final void setSelected(boolean value) {
            selectedPropertyImpl().set(value);
        }
        public final boolean isSelected() { return selected == null ? false : selected.get(); }

        public ReadOnlyBooleanProperty selectedProperty() {
            return selectedPropertyImpl().getReadOnlyProperty();
        }
        private ReadOnlyBooleanWrapper selectedPropertyImpl() {
            if (selected == null) {
                selected = new ReadOnlyBooleanWrapper() {
                    @Override protected void invalidated() {
                        pseudoClassStateChanged(SELECTED_PSEUDOCLASS_STATE, get());
                    }

                    @Override
                    public Object getBean() {
                        return ColorSquare.this;
                    }

                    @Override
                    public String getName() {
                        return "selected";
                    }
                };
            }
            return selected;
        }
        private final PseudoClass SELECTED_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("selected");
        
    }
    
    public void clearFocus() {
        colorPickerGrid.requestFocus();
    }

    // The skin can update selection if colorpicker value changes..
    public void updateSelection(Color color) {
        for (ColorSquare c : colorPickerGrid.getSquares()) {
             c.setSelected(c.rectangle.getFill().equals(color));
        }
        // check custom colors
        for (ColorSquare cs : customSquares) {
            cs.setSelected(cs.rectangle.getFill().equals(color));
        }
    }
    
    class ColorPickerGrid extends GridPane {

        private final List<ColorSquare> squares;

        public ColorPickerGrid(Color initPaint) {
            getStyleClass().add("color-picker-grid");
            setId("ColorCustomizerColorGrid");
            int columnIndex = 0, rowIndex = 0;
            setFocusTraversable(true);
            squares = FXCollections.observableArrayList();
            int numColors = rawValues.length / 3;
            Color[] colors = new Color[numColors];
            for (int i = 0; i < numColors; i++) {
                colors[i] = new Color(rawValues[(i * 3)] / 255,
                        rawValues[(i * 3) + 1] / 255, rawValues[(i * 3) + 2] / 255,
                        1.0);
                ColorSquare cs = new ColorSquare(colors[i]);
                squares.add(cs);
            }

            for (ColorSquare square : squares) {
                add(square, columnIndex, rowIndex);
                columnIndex++;
                if (columnIndex == NUM_OF_COLUMNS) {
                    columnIndex = 0;
                    rowIndex++;
                }
            }
            setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent t) {
                    if (!dragDetected) {
                        dragDetected = true;
                        mouseDragColor = colorPicker.getValue();
                    }
                    int xIndex = (int)(t.getX()/16.0);
                    int yIndex = (int)(t.getY()/16.0);
                        int index = xIndex + yIndex*12;
                    if (index < NUM_OF_COLUMNS*NUM_OF_ROWS) {
                        colorPicker.setValue((Color) squares.get(index).rectangle.getFill());
                        updateSelection(colorPicker.getValue());
                    }
                }
            });
            addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent t) {
                    if(colorPickerGrid.getBoundsInLocal().contains(t.getX(), t.getY())) {
                        updateSelection(colorPicker.getValue());
                        colorPicker.fireEvent(new ActionEvent());
                        colorPicker.hide();
                    } else {
                        // restore color as mouse release happened outside the grid.
                        if (mouseDragColor != null) {
                            colorPicker.setValue(mouseDragColor);
                            updateSelection(mouseDragColor);
                        }
                    }
                    dragDetected = false;
                }
            });
        }
        
        public List<ColorSquare> getSquares() {
            return squares;
        }

        double[] rawValues = {
            255, 255, 255, // first row
            242, 242, 242,
            230, 230, 230,
            204, 204, 204,
            179, 179, 179,
            153, 153, 153,
            128, 128, 128,
            102, 102, 102,
            77, 77, 77,
            51, 51, 51,
            26, 26, 26,
            0, 0, 0,
            0, 51, 51, // second row
            0, 26, 128,
            26, 0, 104,
            51, 0, 51,
            77, 0, 26,
            153, 0, 0,
            153, 51, 0,
            153, 77, 0,
            153, 102, 0,
            153, 153, 0,
            102, 102, 0,
            0, 51, 0,
            26, 77, 77, // third row
            26, 51, 153,
            51, 26, 128,
            77, 26, 77,
            102, 26, 51,
            179, 26, 26,
            179, 77, 26,
            179, 102, 26,
            179, 128, 26,
            179, 179, 26,
            128, 128, 26,
            26, 77, 26,
            51, 102, 102, // fourth row
            51, 77, 179,
            77, 51, 153,
            102, 51, 102,
            128, 51, 77,
            204, 51, 51,
            204, 102, 51,
            204, 128, 51,
            204, 153, 51,
            204, 204, 51,
            153, 153, 51,
            51, 102, 51,
            77, 128, 128, // fifth row
            77, 102, 204,
            102, 77, 179,
            128, 77, 128,
            153, 77, 102,
            230, 77, 77,
            230, 128, 77,
            230, 153, 77,
            230, 179, 77,
            230, 230, 77,
            179, 179, 77,
            77, 128, 77,
            102, 153, 153, // sixth row
            102, 128, 230,
            128, 102, 204,
            153, 102, 153,
            179, 102, 128,
            255, 102, 102,
            255, 153, 102,
            255, 179, 102,
            255, 204, 102,
            255, 255, 77,
            204, 204, 102,
            102, 153, 102,
            128, 179, 179, // seventh row
            128, 153, 255,
            153, 128, 230,
            179, 128, 179,
            204, 128, 153,
            255, 128, 128,
            255, 153, 128,
            255, 204, 128,
            255, 230, 102,
            255, 255, 102,
            230, 230, 128,
            128, 179, 128,
            153, 204, 204, // eigth row
            153, 179, 255,
            179, 153, 255,
            204, 153, 204,
            230, 153, 179,
            255, 153, 153,
            255, 179, 128,
            255, 204, 153,
            255, 230, 128,
            255, 255, 128,
            230, 230, 153,
            153, 204, 153,
            179, 230, 230, // ninth row
            179, 204, 255,
            204, 179, 255,
            230, 179, 230,
            230, 179, 204,
            255, 179, 179,
            255, 179, 153,
            255, 230, 179,
            255, 230, 153,
            255, 255, 153,
            230, 230, 179,
            179, 230, 179,
            204, 255, 255, // tenth row
            204, 230, 255,
            230, 204, 255,
            255, 204, 255,
            255, 204, 230,
            255, 204, 204,
            255, 204, 179,
            255, 230, 204,
            255, 255, 179,
            255, 255, 204,
            230, 230, 204,
            204, 255, 204
        };
        
        @Override protected double computePrefWidth(double height) {
            return (SQUARE_SIZE + 1)*12;
        }

        @Override protected double computePrefHeight(double width) {
            return (SQUARE_SIZE + 1)*10;
        }
        
    }
    
}
