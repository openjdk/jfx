/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import java.util.List;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

/**
 *
 * 
 */
public class BorderPaneTring extends AbstractNodeTring<BorderPane> {
    
    private final Accessory targetAccessory;
    private final BorderPane borderPane = new BorderPane();
    private final Label topLabel = new Label();
    private final Label bottomLabel = new Label();
    private final Label leftLabel = new Label();
    private final Label rightLabel = new Label();
    private final Label centerLabel = new Label();
    

    public BorderPaneTring(ContentPanelController contentPanelController, 
            FXOMInstance fxomInstance, DesignHierarchyMask.Accessory targetAccessory) {
        super(contentPanelController, fxomInstance, BorderPane.class);
        assert (targetAccessory == Accessory.TOP)
                || (targetAccessory == Accessory.BOTTOM)
                || (targetAccessory == Accessory.LEFT)
                || (targetAccessory == Accessory.RIGHT)
                || (targetAccessory == Accessory.CENTER);
        
        this.targetAccessory = targetAccessory;
        
        topLabel.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        topLabel.setMaxSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        bottomLabel.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        bottomLabel.setMaxSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        leftLabel.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        leftLabel.setMaxSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        rightLabel.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        rightLabel.setMaxSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        centerLabel.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        centerLabel.setMaxSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);

        topLabel.setText(Accessory.TOP.toString());
        bottomLabel.setText(Accessory.BOTTOM.toString());
        leftLabel.setText(Accessory.LEFT.toString());
        rightLabel.setText(Accessory.RIGHT.toString());
        centerLabel.setText(Accessory.CENTER.toString());
        
        topLabel.getStyleClass().add(TARGET_RING_CLASS);
        topLabel.getStyleClass().add(BorderPane.class.getSimpleName());
        bottomLabel.getStyleClass().add(TARGET_RING_CLASS);
        bottomLabel.getStyleClass().add(BorderPane.class.getSimpleName());
        leftLabel.getStyleClass().add(TARGET_RING_CLASS);
        leftLabel.getStyleClass().add(BorderPane.class.getSimpleName());
        rightLabel.getStyleClass().add(TARGET_RING_CLASS);
        rightLabel.getStyleClass().add(BorderPane.class.getSimpleName());
        centerLabel.getStyleClass().add(TARGET_RING_CLASS);
        centerLabel.getStyleClass().add(BorderPane.class.getSimpleName());
        
        borderPane.setTop(topLabel);
        borderPane.setBottom(bottomLabel);
        borderPane.setLeft(leftLabel);
        borderPane.setRight(rightLabel);
        borderPane.setCenter(centerLabel);
        borderPane.setMinWidth(BorderPane.USE_PREF_SIZE);
        borderPane.setMinHeight(BorderPane.USE_PREF_SIZE);
        borderPane.setMaxWidth(BorderPane.USE_PREF_SIZE);
        borderPane.setMaxHeight(BorderPane.USE_PREF_SIZE);
        
        getRootNode().getChildren().add(0, borderPane);
    }

    
    public static Bounds computeCenterBounds(BorderPane sceneGraphObject) {
        final Bounds b = sceneGraphObject.getLayoutBounds();
        
        final double x0 = b.getMinX();
        final double x3 = b.getMaxX();
        final double x1 = x0 + (x3 - x0) * 0.25;
        final double x2 = x0 + (x3 - x0) * 0.75;
        
        final double y0 = b.getMinY();
        final double y3 = b.getMaxY();
        final double y1 = y0 + (y3 - y0) * 0.25;
        final double y2 = y0 + (y3 - y0) * 0.75;

        return new BoundingBox(x1, y1, x2 - x1, y2 - y1);
    }
    

    
    public static Bounds computeAreaBounds(Bounds lb, Bounds cb, Accessory area) {
        assert lb != null;
        assert cb != null;
        
        /*                       
         *      lb.minx                                    lb.maxx
         *              cb.minx                   cb.maxx
         *  lb.miny o----------------------------------------o
         *          |                  Top                   | 
         *  cb.miny o-----o-------------------------o--------o
         *          |     |                         |        |
         *          |     |                         |        |
         *          |     |                         |        |
         *          |Left |           Center        |  Right |
         *          |     |                         |        |
         *          |     |                         |        |
         *          |     |                         |        |
         *  cb.maxy o-----o-------------------------o--------o
         *          |                                        |
         *          |                 Bottom                 | 
         *          |                                        |
         *  lb.maxy o----------------------------------------o
         * 
         */
        
        final double xmin, ymin, xmax, ymax;
        switch(area) {
            case TOP:
                xmin = lb.getMinX();
                ymin = lb.getMinY();
                xmax = lb.getMaxX();
                ymax = cb.getMinY();
                break;
            case BOTTOM:
                xmin = lb.getMinX();
                ymin = cb.getMaxY();
                xmax = lb.getMaxX();
                ymax = lb.getMaxY();
                break;
            case LEFT:
                xmin = lb.getMinX();
                ymin = cb.getMinY();
                xmax = cb.getMinX();
                ymax = cb.getMaxY();
                break;
            case RIGHT:
                xmin = cb.getMaxX();
                ymin = cb.getMinY();
                xmax = lb.getMaxX();
                ymax = cb.getMaxY();
                break;
            case CENTER:
                xmin = cb.getMinX();
                ymin = cb.getMinY();
                xmax = cb.getMaxX();
                ymax = cb.getMaxY();
                break;
            default:
                // Emergency code
                assert false : "Unexpected area " + area; //NOI18N
                xmin = cb.getMinX();
                ymin = cb.getMinY();
                xmax = cb.getMaxX();
                ymax = cb.getMaxY();
                break;
        }
        
        return new BoundingBox(xmin, ymin, xmax - xmin, ymax - ymin);
    }
    
    
    /*
     * AbstractGenericTring
     */
        
    @Override
    protected void layoutDecoration() {
        
        super.layoutDecoration();
        
        final Bounds layoutBounds = getSceneGraphObject().getLayoutBounds();
        borderPane.setPrefWidth(layoutBounds.getWidth());
        borderPane.setPrefHeight(layoutBounds.getHeight());

        
        final Bounds centerBounds = computeCenterBounds(getSceneGraphObject());
        centerLabel.setPrefSize(centerBounds.getWidth(), centerBounds.getHeight());
        
        final Bounds topBounds = computeAreaBounds(layoutBounds, centerBounds, Accessory.TOP);
        topLabel.setPrefSize(topBounds.getWidth(), topBounds.getHeight());
        
        final Bounds bottomBounds = computeAreaBounds(layoutBounds, centerBounds, Accessory.BOTTOM);
        bottomLabel.setPrefSize(bottomBounds.getWidth(), bottomBounds.getHeight());
        
        final Bounds leftBounds = computeAreaBounds(layoutBounds, centerBounds, Accessory.LEFT);
        leftLabel.setPrefSize(leftBounds.getWidth(), leftBounds.getHeight());
        
        final Bounds rightBounds = computeAreaBounds(layoutBounds, centerBounds, Accessory.RIGHT);
        rightLabel.setPrefSize(rightBounds.getWidth(), rightBounds.getHeight());
        
        final Label targetLabel;
        switch(targetAccessory) {
            case TOP:
                targetLabel = topLabel;
                break;
            case BOTTOM:
                targetLabel = bottomLabel;
                break;
            case LEFT:
                targetLabel = leftLabel;
                break;
            case RIGHT:
                targetLabel = rightLabel;
                break;
            case CENTER:
                targetLabel = centerLabel;
                break;
            default:
                // Emergency code
                assert false;
                targetLabel = centerLabel;
                break;
        }
        
        setupSelectedStyleClass(topLabel, topLabel == targetLabel);
        setupSelectedStyleClass(bottomLabel, bottomLabel == targetLabel);
        setupSelectedStyleClass(leftLabel, leftLabel == targetLabel);
        setupSelectedStyleClass(rightLabel, rightLabel == targetLabel);
        setupSelectedStyleClass(centerLabel, centerLabel == targetLabel);
        
        
        // Update (decoration) border pane transform
        borderPane.getTransforms().clear();
        borderPane.getTransforms().add(getSceneGraphObjectToDecorationTransform());
    }

    
    
    /*
     * Private
     */
    
    private static final String SELECTED = "selected"; //NOI18N
    
    private static void setupSelectedStyleClass(Label label, boolean selected) {
        final List<String> styleClass = label.getStyleClass();
        if (selected) {
            if (styleClass.contains(SELECTED) == false) {
                styleClass.add(SELECTED);
            }
        } else {
            if (styleClass.contains(SELECTED)) {
                styleClass.remove(SELECTED);
            }
        }
    }
}
