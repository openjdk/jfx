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

import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;

/**
 *
 * 
 */
public class LazyResizer extends AbstractResizer<Node> {

    private final Bounds originalBounds;
    private final double originalScaleX;
    private final double originalScaleY;
    private final PropertyName scaleXName = new PropertyName("scaleX"); //NOI18N
    private final PropertyName scaleYName = new PropertyName("scaleY"); //NOI18N
    private final List<PropertyName> propertyNames = new ArrayList<>();
    
    public LazyResizer(Node sceneGraphObject) {
        super(sceneGraphObject);
        this.originalBounds = sceneGraphObject.getLayoutBounds();
        this.originalScaleX = sceneGraphObject.getScaleX();
        this.originalScaleY = sceneGraphObject.getScaleY();
        propertyNames.add(scaleXName);
        propertyNames.add(scaleYName);
    }
    
    
    /*
     * AbstractResizer
     */
    
    @Override
    public Bounds computeBounds(double width, double height) {
        final double scaleX, scaleY;
        
        if ((originalBounds.getWidth() != 0) && (width != 0)) {
            scaleX = width / originalBounds.getWidth();
        } else {
            scaleX = 1.0;
        }
        
        if ((originalBounds.getHeight() != 0) && (height != 0)) {
            scaleY = height / originalBounds.getHeight();
        } else {
            scaleY = 1.0;
        }
        
        final double minX, minY, maxX, maxY;
        minX = originalBounds.getMinX() * scaleX;
        minY = originalBounds.getMinY() * scaleY;
        maxX = originalBounds.getMaxX() * scaleX;
        maxY = originalBounds.getMaxY() * scaleY;
        
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    
    @Override
    public Feature getFeature() {
        return Feature.FREE;
    }

    @Override
    public void changeWidth(double width) {
        final double scaleX;
        
        if ((originalBounds.getWidth() != 0) && (width != 0)) {
            scaleX = width / originalBounds.getWidth();
        } else {
            scaleX = 1.0;
        }

        sceneGraphObject.setScaleX(scaleX);
    }

    @Override
    public void changeHeight(double height) {
        final double scaleY;
        
        if ((originalBounds.getHeight() != 0) && (height != 0)) {
            scaleY = height / originalBounds.getHeight();
        } else {
            scaleY = 1.0;
        }
        
        sceneGraphObject.setScaleY(scaleY);
    }

    @Override
    public void revertToOriginalSize() {
        sceneGraphObject.setScaleX(originalScaleX);
        sceneGraphObject.setScaleY(originalScaleY);
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
        if (propertyName.equals(scaleXName)) {
            result = sceneGraphObject.getScaleX();
        } else if (propertyName.equals(scaleYName)) {
            result = sceneGraphObject.getScaleY();
        } else {
            // Emergency code
            result = null;
        }
        
        return result;
    }

    @Override
    public Map<PropertyName, Object> getChangeMap() {
        final Map<PropertyName, Object> result = new HashMap<>();
        if (MathUtils.equals(sceneGraphObject.getScaleX(), originalScaleX) == false) {
            result.put(scaleXName, sceneGraphObject.getScaleX());
        }
        if (MathUtils.equals(sceneGraphObject.getScaleY(), originalScaleY) == false) {
            result.put(scaleYName, sceneGraphObject.getScaleY());
        }
        return result;
    }
    
}
