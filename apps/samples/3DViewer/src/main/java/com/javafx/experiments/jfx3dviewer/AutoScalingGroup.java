/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
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
package com.javafx.experiments.jfx3dviewer;

import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * A Group that auto scales its self to fit its content in a given size box.
 */
public class AutoScalingGroup extends Group {
    private double size;
    private double twoSize;
    private boolean autoScale = false;
    private Translate translate = new Translate(0,0,0);
    private Scale scale = new Scale(1,1,1,0,0,0);
    private SimpleBooleanProperty enabled = new SimpleBooleanProperty(false) {
        @Override protected void invalidated() {
            if (get()) {
                getTransforms().setAll(scale, translate);
            } else {
                getTransforms().clear();
            }
        }
    };

    /**
     * Create AutoScalingGroup
     *
     * @param size half of width/height/depth of box to fit content into
     */
    public AutoScalingGroup(double size) {
        this.size = size;
        this.twoSize = size * 2;
        getTransforms().addAll(scale,translate);
    }

    /**
     * Get is auto scaling enabled
     *
     * @return true if auto scaling is enabled
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Get enabled property
     *
     * @return enabled property
     */
    public SimpleBooleanProperty enabledProperty() {
        return enabled;
    }

    /**
     * Set is auto scaling enabled
     *
     * @param enabled true if auto scaling is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override protected void layoutChildren() {
        if (autoScale) {
            List<Node> children = getChildren();
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;
            boolean first = true;
            for (int i=0, max=children.size(); i<max; i++) {
                final Node node = children.get(i);
                if (node.isVisible()) {
                    Bounds bounds = node.getBoundsInLocal();
                    // if the bounds of the child are invalid, we don't want
                    // to use those in the remaining computations.
                    if (bounds.isEmpty()) continue;
                    if (first) {
                        minX = bounds.getMinX();
                        minY = bounds.getMinY();
                        minZ = bounds.getMinZ();
                        maxX = bounds.getMaxX();
                        maxY = bounds.getMaxY();
                        maxZ = bounds.getMaxZ();
                        first = false;
                    } else {
                        minX = Math.min(bounds.getMinX(), minX);
                        minY = Math.min(bounds.getMinY(), minY);
                        minZ = Math.min(bounds.getMinZ(), minZ);
                        maxX = Math.max(bounds.getMaxX(), maxX);
                        maxY = Math.max(bounds.getMaxY(), maxY);
                        maxZ = Math.max(bounds.getMaxZ(), maxZ);
                    }
                }
            }

            final double w = maxX-minX;
            final double h = maxY-minY;
            final double d = maxZ-minZ;

            final double centerX = minX + (w/2);
            final double centerY = minY + (h/2);
            final double centerZ = minZ + (d/2);

            double scaleX = twoSize/w;
            double scaleY = twoSize/h;
            double scaleZ = twoSize/d;

            double scale = Math.min(scaleX, Math.min(scaleY,scaleZ));
            this.scale.setX(scale);
            this.scale.setY(scale);
            this.scale.setZ(scale);

            this.translate.setX(-centerX);
            this.translate.setY(-centerY);
        }
    }
}
