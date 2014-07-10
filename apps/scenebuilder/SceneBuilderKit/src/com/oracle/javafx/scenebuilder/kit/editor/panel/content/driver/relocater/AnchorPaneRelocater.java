/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.relocater;

import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

/**
 *
 * 
 */
public class AnchorPaneRelocater extends AbstractRelocater<AnchorPane> {

    private final double originalLayoutX;
    private final double originalLayoutY;
    private final Double originalLeftAnchor;
    private final Double originalRightAnchor;
    private final Double originalTopAnchor;
    private final Double originalBottomAnchor;
    private final PropertyName layoutXName = new PropertyName("layoutX");
    private final PropertyName layoutYName = new PropertyName("layoutY");
    private final PropertyName leftAnchorName = new PropertyName("leftAnchor", AnchorPane.class);
    private final PropertyName rightAnchorName = new PropertyName("rightAnchor", AnchorPane.class);
    private final PropertyName topAnchorName = new PropertyName("topAnchor", AnchorPane.class);
    private final PropertyName bottomAnchorName = new PropertyName("bottomAnchor", AnchorPane.class);
    private final List<PropertyName> propertyNames = new ArrayList<>();
    
    public AnchorPaneRelocater(Node sceneGraphObject) {
        super(sceneGraphObject, AnchorPane.class);
        this.originalLayoutX = sceneGraphObject.getLayoutX();
        this.originalLayoutY = sceneGraphObject.getLayoutY();
        this.originalLeftAnchor = AnchorPane.getLeftAnchor(sceneGraphObject);
        this.originalRightAnchor = AnchorPane.getRightAnchor(sceneGraphObject);
        this.originalTopAnchor = AnchorPane.getTopAnchor(sceneGraphObject);
        this.originalBottomAnchor = AnchorPane.getBottomAnchor(sceneGraphObject);
        
        if ((originalLeftAnchor == null) && (originalRightAnchor == null)) {
            propertyNames.add(layoutXName);
        } else {
            if (originalLeftAnchor != null) {
                propertyNames.add(leftAnchorName);
                propertyNames.add(layoutXName);
            }
            if (originalRightAnchor != null) {
                propertyNames.add(rightAnchorName);
            }
        }
        if ((originalTopAnchor == null) && (originalBottomAnchor == null)) {
            propertyNames.add(layoutYName);
        } else {
            if (originalTopAnchor != null) {
                propertyNames.add(topAnchorName);
                propertyNames.add(layoutYName);
            }
            if (originalBottomAnchor != null) {
                propertyNames.add(bottomAnchorName);
            }
        }
    }
    
    
    /*
     * AbstractRelocater
     */
    @Override
    public void moveToLayoutX(double newLayoutX, Bounds newLayoutBounds) {
        if ((originalLeftAnchor == null) && (originalRightAnchor == null)) {
            sceneGraphObject.setLayoutX(Math.round(newLayoutX));
        } else {
            final Bounds parentLayoutBounds = sceneGraphObject.getParent().getLayoutBounds();
            if (originalLeftAnchor != null) {
                final double leftAnchor = computeLeftAnchor(parentLayoutBounds, newLayoutBounds, newLayoutX);
                AnchorPane.setLeftAnchor(sceneGraphObject, (double)Math.round(leftAnchor));
            }
            if (originalRightAnchor != null) {
                final double rightAnchor = computeRightAnchor(parentLayoutBounds, newLayoutBounds, newLayoutX);
                AnchorPane.setRightAnchor(sceneGraphObject, (double)Math.round(rightAnchor));
            }
        }
    }

    @Override
    public void moveToLayoutY(double newLayoutY, Bounds newLayoutBounds) {
        if ((originalTopAnchor == null) && (originalBottomAnchor == null)) {
            sceneGraphObject.setLayoutY(Math.round(newLayoutY));
        } else {
            final Bounds parentLayoutBounds = sceneGraphObject.getParent().getLayoutBounds();
            if (originalTopAnchor != null) {
                final double topAnchor = computeTopAnchor(parentLayoutBounds, newLayoutBounds, newLayoutY);
                AnchorPane.setTopAnchor(sceneGraphObject, (double)Math.round(topAnchor));
            }
            if (originalBottomAnchor != null) {
                final double bottomAnchor = computeBottomAnchor(parentLayoutBounds, newLayoutBounds, newLayoutY);
                AnchorPane.setBottomAnchor(sceneGraphObject, (double)Math.round(bottomAnchor));
            }
        }
    }

    @Override
    public void revertToOriginalLocation() {
        sceneGraphObject.setLayoutX(originalLayoutX);
        sceneGraphObject.setLayoutY(originalLayoutY);
        AnchorPane.setLeftAnchor(sceneGraphObject, originalLeftAnchor);
        AnchorPane.setRightAnchor(sceneGraphObject, originalRightAnchor);
        AnchorPane.setTopAnchor(sceneGraphObject, originalTopAnchor);
        AnchorPane.setBottomAnchor(sceneGraphObject, originalBottomAnchor);
    }

    @Override
    public List<PropertyName> getPropertyNames() {
        return propertyNames;
    }

    @Override
    public Object getValue(PropertyName propertyName) {
        assert propertyName != null;
        assert propertyNames.contains(propertyName) : "propertyName=" + propertyName;
        
        final Object result;
        if (propertyName.equals(layoutXName)) {
            result = sceneGraphObject.getLayoutX();
        } else if (propertyName.equals(layoutYName)) {
            result = sceneGraphObject.getLayoutY();
        } else if (propertyName.equals(leftAnchorName)) {
            result = AnchorPane.getLeftAnchor(sceneGraphObject);
        } else if (propertyName.equals(rightAnchorName)) {
            result = AnchorPane.getRightAnchor(sceneGraphObject);
        } else if (propertyName.equals(topAnchorName)) {
            result = AnchorPane.getTopAnchor(sceneGraphObject);
        } else if (propertyName.equals(bottomAnchorName)) {
            result = AnchorPane.getBottomAnchor(sceneGraphObject);
        } else {
            // Emergency code
            result = null;
        }
        
        return result;
    }

    @Override
    public Map<PropertyName, Object> getChangeMap() {
        final Map<PropertyName, Object> result = new HashMap<>();
        if ((originalLeftAnchor == null) && (originalRightAnchor == null)) {
            if (MathUtils.equals(sceneGraphObject.getLayoutX(), originalLayoutX) == false) {
                result.put(layoutXName, sceneGraphObject.getLayoutX());
            }
        } else {
            if (Objects.equals(AnchorPane.getLeftAnchor(sceneGraphObject), originalLeftAnchor) == false) {
                result.put(leftAnchorName, AnchorPane.getLeftAnchor(sceneGraphObject));
                result.put(layoutXName, sceneGraphObject.getLayoutX());
            }
            if (Objects.equals(AnchorPane.getRightAnchor(sceneGraphObject), originalRightAnchor) == false) {
                result.put(rightAnchorName, AnchorPane.getRightAnchor(sceneGraphObject));
            }
        }
        if ((originalTopAnchor == null) && (originalBottomAnchor == null)) {
            if (MathUtils.equals(sceneGraphObject.getLayoutY(), originalLayoutY) == false) {
                result.put(layoutYName, sceneGraphObject.getLayoutY());
            }
        } else {
            if (Objects.equals(AnchorPane.getTopAnchor(sceneGraphObject), originalTopAnchor) == false) {
                result.put(topAnchorName, AnchorPane.getTopAnchor(sceneGraphObject));
                result.put(layoutYName, sceneGraphObject.getLayoutY());
            }
            if (Objects.equals(AnchorPane.getBottomAnchor(sceneGraphObject), originalBottomAnchor) == false) {
                result.put(bottomAnchorName, AnchorPane.getBottomAnchor(sceneGraphObject));
            }
        }
        return result;
    }
    
    
    
    /*
     * Public (static)
     */
    
    public static double computeLeftAnchor(Bounds parentBounds, Bounds childBounds, double targetLayoutX) {
        /*
         *       o parent origin
         *                               
         *              +---------------------------------------------------+
         *              |                                                   |
         *              |                  o child origin                   |
         *              |                                                   |
         *              |                       +---------------------+     |
         *              |                       |                     |     |
         *              |                       |                     |     |
         *              |                       |                     |     |
         *              |                       +---------------------+     |
         *              |                                                   |
         *              |                                                   |
         *              +------------------+----+---------------------------+
         *              .                  .    .
         *              .                  .    .
         *              .                  .    .
         *              x0                 .    x1
         *                                 .
         *                           targetLayoutX
         * 
         *  result = x1 - x0
         */
        
        final double x0 = parentBounds.getMinX();
        final double x1 = parentBounds.getMinX() + targetLayoutX + childBounds.getMinX();
        return x1 - x0;
    }
    
    public static double computeRightAnchor(Bounds parentBounds, Bounds childBounds, double targetLayoutX) {
        /*
         *       o parent origin
         *                               
         *              +---------------------------------------------------+
         *              |                                                   |
         *              |                  o child origin                   |
         *              |                                                   |
         *              |                       +---------------------+     |
         *              |                       |                     |     |
         *              |                       |                     |     |
         *              |                       |                     |     |
         *              |                       +---------------------+     |
         *              |                                                   |
         *              |                                                   |
         *              +------------------+--------------------------------+
         *                                 .                          .     .
         *                                 .                          .     .
         *                                 .                          .     .
         *                                 .                          x0    x1
         *                                 .
         *                           targetLayoutX
         * 
         *  result = x1 - x0
         */
        
        final double x0 = parentBounds.getMinX() + targetLayoutX + childBounds.getMaxX();
        final double x1 = parentBounds.getMaxX();
        return x1 - x0;
    }
    
    public static double computeTopAnchor(Bounds parentBounds, Bounds childBounds, double targetLayoutY) {
        final double y0 = parentBounds.getMinY();
        final double y1 = parentBounds.getMinY() + targetLayoutY + childBounds.getMinY();
        return y1 - y0;
    }
    
    public static double computeBottomAnchor(Bounds parentBounds, Bounds childBounds, double targetLayoutY) {
        final double y0 = parentBounds.getMinY() + targetLayoutY + childBounds.getMaxY();
        final double y1 = parentBounds.getMaxY();
        return y1 - y0;
    }
}
