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
package com.oracle.javafx.scenebuilder.kit.editor.job.wrap;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.List;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * Utilities to build wrap jobs.
 */
public class WrapJobUtils {

    /**
     * Returns the property name of the specified container to be used for wrapping jobs.
     * May be either the children or the content property name
     * (for ScrollPane and TitledPane containers).
     *
     * @param container
     * @param children
     * @return
     */
    static PropertyName getContainerPropertyName(
            final FXOMInstance container, final List<FXOMObject> children) {
        final DesignHierarchyMask mask = new DesignHierarchyMask(container);
        final PropertyName result;

        if (container.getSceneGraphObject() instanceof BorderPane) {
            // wrap/unwrap the child of a BorderPane
            assert mask.isAcceptingAccessory(Accessory.TOP);
            assert mask.isAcceptingAccessory(Accessory.LEFT);
            assert mask.isAcceptingAccessory(Accessory.CENTER);
            assert mask.isAcceptingAccessory(Accessory.RIGHT);
            assert mask.isAcceptingAccessory(Accessory.BOTTOM);
            assert children != null && children.size() == 1; // wrap job is executable
            final FXOMObject child = children.iterator().next();

            final FXOMObject top = mask.getAccessory(Accessory.TOP);
            final FXOMObject left = mask.getAccessory(Accessory.LEFT);
            final FXOMObject center = mask.getAccessory(Accessory.CENTER);
            final FXOMObject right = mask.getAccessory(Accessory.RIGHT);
            final FXOMObject bottom = mask.getAccessory(Accessory.BOTTOM);
            // Return same accessory as the child container one
            if (child.equals(top)) {
                result = mask.getPropertyNameForAccessory(Accessory.TOP);
            } else if (child.equals(bottom)) {
                result = mask.getPropertyNameForAccessory(Accessory.BOTTOM);
            } else if (child.equals(center)) {
                result = mask.getPropertyNameForAccessory(Accessory.CENTER);
            } else if (child.equals(left)) {
                result = mask.getPropertyNameForAccessory(Accessory.LEFT);
            } else if (child.equals(right)) {
                result = mask.getPropertyNameForAccessory(Accessory.RIGHT);
            } else {
                assert false;
                result = null;
            }
        } else if (mask.isAcceptingSubComponent()) {
            result = mask.getSubComponentPropertyName();
        } else if (mask.isAcceptingAccessory(Accessory.CONTENT)) {
            result = mask.getPropertyNameForAccessory(Accessory.CONTENT);
        } else if (mask.isAcceptingAccessory(Accessory.GRAPHIC)) {
            result = mask.getPropertyNameForAccessory(Accessory.GRAPHIC);
        } else {
            assert false;
            result = null;
        }
        return result;
    }

    static Bounds getUnionOfBounds(final List<FXOMObject> fxomObjects) {
        assert fxomObjects != null && fxomObjects.isEmpty() == false;
        Bounds result = null;
        for (FXOMObject fxomObject : fxomObjects) {
            final Object scenegraphObject = fxomObject.getSceneGraphObject();
            assert scenegraphObject instanceof Node;
            final Node node = (Node) scenegraphObject;
            if (result == null) {
                result = node.getBoundsInParent();
            } else {
                result = getUnionOfBounds(result, node.getBoundsInParent());
            }
        }
        return result;
    }

    static ModifyObjectJob modifyObjectJob(
            final FXOMInstance instance,
            final Class<?> clazz,
            final String name,
            final Object value,
            final EditorController controller) {
        final PropertyName pn = new PropertyName(name, clazz);
        final ValuePropertyMetadata vpm
                = Metadata.getMetadata().queryValueProperty(instance, pn);
        final ModifyObjectJob job = new ModifyObjectJob(
                instance, vpm, value, controller);
        return job;
    }

    static ModifyObjectJob modifyObjectJob(
            final FXOMInstance instance,
            final String name,
            final Object value,
            final EditorController controller) {
        return modifyObjectJob(instance, null, name, value, controller);
    }

    /**
     * Returns the union of n bounds.
     *
     * @param bounds a series of bounds
     * @return the union of all bounds in the series.
     */
    private static Bounds getUnionOfBounds(Bounds... bounds) {
        if (bounds == null || bounds.length == 0) {
            return new BoundingBox(0, 0, 0, 0);
        }
        if (bounds.length == 1) {
            return bounds[0];
        }
        Bounds b0 = bounds[0];
        for (int i = 1; i < bounds.length; i++) {
            final Bounds bi = bounds[i];
            if (bi == null) {
                continue;
            }
            b0 = union(b0, bi);
        }
        return b0;
    }

    /**
     * Returns the union of two bounds.
     *
     * @param b1 first bounds
     * @param b2 second bounds
     * @return the union of the two bounds.
     */
    private static Bounds union(Bounds b1, Bounds b2) {
        double minX, minY, minZ, maxX, maxY, maxZ;

        minX = Math.min(b1.getMinX(), b2.getMinX());
        minY = Math.min(b1.getMinY(), b2.getMinY());
        minZ = Math.min(b1.getMinZ(), b2.getMinZ());

        maxX = Math.max(b1.getMaxX(), b2.getMaxX());
        maxY = Math.max(b1.getMaxY(), b2.getMaxY());
        maxZ = Math.max(b1.getMaxZ(), b2.getMaxZ());

        return new BoundingBox(minX, minY, minZ,
                maxX - minX, maxY - minY, maxZ - minZ);
    }
}
