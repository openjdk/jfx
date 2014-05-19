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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;

import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;

/**
 *
 * 
 */
public class ImageViewResizer extends AbstractResizer<ImageView> {

    private final double originalFitWidth;
    private final double originalFitHeight;
    private final PropertyName fitWidthName = new PropertyName("fitWidth"); //NOI18N
    private final PropertyName fitHeightName = new PropertyName("fitHeight"); //NOI18N
    private final List<PropertyName> propertyNames = new ArrayList<>();
    
    public ImageViewResizer(ImageView sceneGraphObject) {
        super(sceneGraphObject);
        originalFitWidth   = sceneGraphObject.getFitWidth();
        originalFitHeight  = sceneGraphObject.getFitHeight();
        propertyNames.add(fitWidthName);
        propertyNames.add(fitHeightName);
//        assert BoundsUtils.equals(sceneGraphObject.getLayoutBounds(), 
//                computeBounds(originalFitWidth, originalFitHeight));
    }

    /*
     * AbstractResizer
     */
    
    @Override
    public final Bounds computeBounds(double width, double height) {
        final double minX = sceneGraphObject.getX();
        final double minY = sceneGraphObject.getY();
        final double actualWidth;
        if (width > 0) {
            actualWidth = width;
        } else {
            actualWidth = 1.0;
        }
        final double actualHeight;
        if (height > 0) {
            actualHeight = height;
        } else {
            actualHeight = 1.0;
        }
        return new BoundingBox(minX, minY, Math.round(actualWidth), Math.round(actualHeight));
    }

    
    @Override
    public Feature getFeature() {
        return Feature.FREE;
    }

    @Override
    public void changeWidth(double width) {
        sceneGraphObject.setFitWidth(Math.round(width));
    }

    @Override
    public void changeHeight(double height) {
        sceneGraphObject.setFitHeight(Math.round(height));
    }

    @Override
    public void revertToOriginalSize() {
        sceneGraphObject.setFitWidth(originalFitWidth);
        sceneGraphObject.setFitHeight(originalFitHeight);
    }

    @Override
    public List<PropertyName> getPropertyNames() {
        return propertyNames;
    }

    @Override
    public Object getValue(PropertyName propertyName) {
        assert propertyName != null;
        assert propertyNames.contains(propertyName);
        
        final Object result;
        if (propertyName.equals(fitWidthName)) {
            result = sceneGraphObject.getFitWidth();
        } else if (propertyName.equals(fitHeightName)) {
            result = sceneGraphObject.getFitHeight();
        } else {
            // Emergency code
            result = null;
        }
        
        return result;
    }

    @Override
    public Map<PropertyName, Object> getChangeMap() {
        final Map<PropertyName, Object> result = new HashMap<>();
        if (MathUtils.equals(sceneGraphObject.getFitWidth(), originalFitWidth) == false) {
            result.put(fitWidthName, sceneGraphObject.getFitWidth());
        }
        if (MathUtils.equals(sceneGraphObject.getFitHeight(), originalFitHeight) == false) {
            result.put(fitHeightName, sceneGraphObject.getFitHeight());
        }
        return result;
    }
}
