/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.scene.control;

import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Window;


public class ColorPickerPanel extends Region {
    
    private static final int ARROW_SIZE = 10;
    private static final int RADIUS = 8;
    private static final int GAP = 15;
    private static final int SQUARE_SIZE = 15;
    private static final int NUM_OF_COLUMNS = 12;
    
    private boolean customColorAdded = false;
    ColorPickerGrid cpg;
    Path path;
    ColorPicker colorPicker;
    GridPane customColorGrid = new GridPane();
    Button addColorButton = new Button("Add Color...");
    Separator separator = new Separator();
    Window owner;
    ColorPickerAddColorPane addColorDialog = null;
    int customColumnIndex = 0, customRowIndex = 0;
    private final List<ColorSquare> customSquares = FXCollections.observableArrayList();
    
    private ObjectProperty<Color> color = new SimpleObjectProperty<Color>(Color.WHITE);
    public ObjectProperty<Color> colorProperty() { return color; }
    public Color getColor() { return color.get(); }
    public void setColor(Color newColor) { color.set(newColor);}
    
    private ObjectProperty<Color> customColor = new SimpleObjectProperty<Color>(Color.WHITE);
    public ObjectProperty<Color> customColorProperty() { return customColor; }
    public Color getcustomColor() { return customColor.get(); }
    public void setCustomColor(Color newColor) { customColor.set(newColor);}
    
    public ColorPickerPanel(Color initPaint) {
        getStyleClass().add("color-panel");
        cpg = new ColorPickerGrid(initPaint);
        colorProperty().bindBidirectional(cpg.colorProperty());
        addColorDialog = new ColorPickerAddColorPane(owner, colorProperty());
        addColorButton.setPrefWidth(cpg.prefWidth(-1));
        addColorButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                addColorDialog.show();
            }
        });
        addColorDialog.customColorProperty.addListener(new ChangeListener<Color>() {
            @Override public void changed(ObservableValue<? extends Color> ov, Color t, Color t1) {
                if (!customColorAdded) {
                    customColorAdded = true;
                    customColorGrid.setGridLinesVisible(true);
                }
                ColorSquare cs = new ColorSquare(addColorDialog.customColorProperty.get());
                customSquares.add(cs);
                customColorGrid.add(cs, customColumnIndex, customRowIndex);
                if (customColumnIndex == 0) { // new row - add empty squares
                    for (int i = 0; i < NUM_OF_COLUMNS - 1; i++) {
                        ColorSquare emptySquare = new ColorSquare(null);
                        customSquares.add(emptySquare);
                        customColorGrid.add(emptySquare, i+1, customRowIndex);
                    }
                }
                customColumnIndex++;
                if (customColumnIndex == NUM_OF_COLUMNS) {
                    customColumnIndex = 0;
                    customRowIndex++;
                } 
                requestLayout();
            }
        });
        
        // create popup path for main shape
        path = new Path();
//        path.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#313131")), new Stop(0.5, Color.web("#5f5f5f")), new Stop(1, Color.web("#313131"))));
        path.setFill(Color.LIGHTGRAY);
        path.setStroke(null);
        path.setEffect(new DropShadow(15, 0, 1, Color.gray(0, 0.6)));
        path.setCache(true);
        getChildren().addAll(path, cpg, customColorGrid, separator, addColorButton);
    }
    public void setOwner(ColorPicker colorPicker) {
        this.colorPicker = colorPicker;
        owner = colorPicker.getScene().getWindow();
        cpg.owner = colorPicker.getScene().getWindow();
    }
    
    @Override protected void layoutChildren() {
        double paddingX = getInsets().getLeft();
        double paddingY = getInsets().getTop();
//        double popupWidth = cpg.prefWidth(-1) + paddingX+getInsets().getRight();
//        double popupHeight = cpg.prefHeight(-1) + getInsets().getTop() + getInsets().getBottom();
        double popupWidth = getWidth();
        double popupHeight = getHeight();
        double arrowX = paddingX+RADIUS;
        path.getElements().addAll(
                new MoveTo(paddingX, getInsets().getTop() + ARROW_SIZE + RADIUS), 
                new ArcTo(RADIUS, RADIUS, 90, paddingX + RADIUS, paddingX + ARROW_SIZE, false, true), 
                new LineTo(paddingX + arrowX - (ARROW_SIZE * 0.8), paddingX + ARROW_SIZE), 
                new LineTo(paddingX + arrowX, paddingX), 
                new LineTo(paddingX + arrowX + (ARROW_SIZE * 0.8), paddingX + ARROW_SIZE), 
                new LineTo(paddingX + popupWidth - RADIUS, paddingX + ARROW_SIZE), 
                new ArcTo(RADIUS, RADIUS, 90, paddingX + popupWidth, paddingX + ARROW_SIZE + RADIUS, false, true), 
                new LineTo(paddingX + popupWidth, paddingX + ARROW_SIZE + popupHeight - RADIUS), 
                new ArcTo(RADIUS, RADIUS, 90, paddingX + popupWidth - RADIUS, paddingX + ARROW_SIZE + popupHeight, false, true), 
                new LineTo(paddingX + RADIUS, paddingX + ARROW_SIZE + popupHeight), 
                new ArcTo(RADIUS, RADIUS, 90, paddingX, paddingX + ARROW_SIZE + popupHeight - RADIUS, false, true), 
                new ClosePath());
        double x = paddingX*2;
        double y = 2*getInsets().getTop()+ARROW_SIZE;
        cpg.relocate(x, y);
        y = y+cpg.prefHeight(-1)+GAP;
        if (customColorAdded) {
            customColorGrid.relocate(x, y);
            y = y+customColorGrid.prefHeight(-1)+GAP;
        }
        separator.resizeRelocate(x, y, cpg.prefWidth(-1), separator.prefHeight(-1));
        y = y+separator.prefHeight(-1)+GAP;
        addColorButton.resizeRelocate(x, y, cpg.prefWidth(-1), addColorButton.prefHeight(-1));
    }
    
    @Override protected double computePrefWidth(double height) {
        return getInsets().getLeft() + cpg.prefWidth(-1) + getInsets().getRight();
    }
    
    @Override protected double computePrefHeight(double width) {
        double totalHeight = cpg.prefHeight(-1) + GAP + customColorGrid.prefHeight(-1) +
                GAP + separator.prefHeight(-1) + GAP + addColorButton.prefHeight(-1);
        return getInsets().getTop() + totalHeight + getInsets().getBottom();
    }
    
    private void setCustomColorAdded(boolean value) {
        this.customColorAdded = value;
    }
    

    class ColorSquare extends Rectangle {
        public ColorSquare(Color color) {
            setFill(color == null  ? Color.WHITE : color);
//            setFill(color);
            setSmooth(false);
//            Utils.setBlocksMouse(this, true);
            setWidth(SQUARE_SIZE);
            setHeight(SQUARE_SIZE);
            setStrokeType(StrokeType.INSIDE);
            // Add style class to handle selected color square
            getStyleClass().add("color-square");
            addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getClickCount() == 1) {
                        if (getFill() != null) {
                            if (getFill() instanceof Color) {
                                setColor((Color) getFill());
                            }
                            event.consume();
                        }
                    }
                }
            });
        }
    }

    class ColorPickerGrid extends GridPane {

        private Color currentColor = null;
        private final List<ColorSquare> squares;
        Window owner;

        public ColorPickerGrid(Color initPaint) {
            getStyleClass().add("color-picker-grid");
            setId("ColorCustomizerColorGrid");
            setGridLinesVisible(true);
            int columnIndex = 0, rowIndex = 0;

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
            setColor(initPaint);
        }

        private ObjectProperty<Color> color = new SimpleObjectProperty<Color>(Color.RED) {
            @Override protected void invalidated() {
                for (ColorSquare cs : squares) {
                if (cs.getFill().equals(get())) {
                    // Check css rule has not been already added
                    if (!cs.getStyleClass().contains("selected")) {
                        cs.getStyleClass().add("selected");
                    }
                } else {
                    cs.getStyleClass().remove("selected");
                }
            }
            currentColor = get(); 
            }
        };
        public ObjectProperty<Color> colorProperty() { return color; }
        public Color getColor() { return color.get(); }
        public void setColor(Color newColor) { color.set(newColor);}

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
    }
}
