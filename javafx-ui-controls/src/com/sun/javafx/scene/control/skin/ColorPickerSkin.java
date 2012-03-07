/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.scene.control.skin;

import com.sun.javafx.scene.control.behavior.ColorPickerBehavior;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import com.sun.javafx.scene.control.ColorPickerPanel;
import java.util.List;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.RectangleBuilder;

/**
 *
 * @author paru
 */
public class ColorPickerSkin<T> extends ComboBoxPopupControl<T> {

    private StackPane displayNode = new StackPane();
    private Rectangle colorRect; 
    private ColorPickerPanel popup = new ColorPickerPanel(Color.WHITE);
    
    private ObjectProperty<Color> color = new SimpleObjectProperty<Color>(Color.RED);
    public ObjectProperty<Color> colorProperty() { return color; }
    public Color getColor() { return color.get(); }
    public void setColor(Color newColor) { color.set(newColor); }
    
    public ColorPickerSkin(final ColorPicker<T> colorPicker) {
        super(colorPicker, new ColorPickerBehavior<T>(colorPicker));
        initialize();
    }
    
    private void initialize() {
        displayNode.getStyleClass().add("picker-color");
        colorRect = RectangleBuilder.create()
                .width(16).height(16)
                .build();
        popup.colorProperty().addListener(new ChangeListener<Color>() {
            @Override public void changed(ObservableValue<? extends Color> ov, Color t, Color t1) {
                 setColor(t1);
            }
        });
        colorRect.fillProperty().bind(new ObjectBinding<Paint>() {
            { bind(color); }
            @Override protected Paint computeValue() {
                return getColor();
            }
        });          
        displayNode.getChildren().add(colorRect);
        getChildren().setAll(displayNode, arrowButton);
        
        popup.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent t) {
                getSkinnable().hide();
            }
        });
    }
    
    @Override
    protected Node getPopupContent() {
       return popup;
    }
    
    @Override
    public Node getDisplayNode() {
        return displayNode;
    }
    
    @Override protected void layoutChildren() {
        final Insets padding = getInsets();
        final Insets arrowButtonPadding = arrowButton.getInsets();
        
        final double arrowWidth = snapSize(arrow.prefWidth(-1));
        final double arrowButtonWidth = snapSpace(arrowButtonPadding.getLeft()) + 
                                        arrowWidth + 
                                        snapSpace(arrowButtonPadding.getRight());
        
        // x, y, w, h are the content area that will hold the label and arrow */
        final double x = padding.getLeft();
        final double y = padding.getTop();
        final double w = getSkinnable().getWidth() - (padding.getLeft() + padding.getRight()+arrowButtonWidth);
        final double h = getSkinnable().getHeight() - (padding.getTop() + padding.getBottom());
        
        if (displayNode != null) {
            displayNode.resizeRelocate(x, y, w, h);
        }
        
        arrowButton.resize(arrowButtonWidth, getHeight());
        positionInArea(arrowButton, getWidth() - padding.getRight() - arrowButtonWidth, 0, 
                arrowButtonWidth, getHeight(), 0, HPos.CENTER, VPos.CENTER);
    }
    @Override protected double computePrefWidth(double height) {
        final double boxWidth = displayNode.prefWidth(-1)
                + arrowButton.prefWidth(-1);
        return getInsets().getLeft() + boxWidth +
                + getInsets().getRight();
    }
    
}
