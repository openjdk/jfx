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
package com.oracle.javafx.scenebuilder.kit.editor.job;

import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;

/**
 *
 */
public class FitToParentObjectJob extends BatchDocumentJob {

    private final FXOMInstance fxomInstance;
    private final FXOMPropertyC parentProperty;
    private final FXOMInstance parentInstance;

    private enum Sizing {

        HORIZONTAL, VERTICAL
    }

    private enum Anchor {

        TOP, RIGHT, BOTTOM, LEFT
    }

    public FitToParentObjectJob(FXOMInstance fxomInstance, EditorController editorController) {
        super(editorController);

        assert fxomInstance != null;
        this.fxomInstance = fxomInstance;
        this.parentProperty = fxomInstance.getParentProperty();
        this.parentInstance = (parentProperty == null) ? null : parentProperty.getParentInstance();
    }

    @Override
    protected List<Job> makeSubJobs() {

        final List<Job> result = new ArrayList<>();

        // Object cannot be root
        if (parentProperty == null) {
            return result; // subJobs is empty => isExecutable will return false
        }
        // Object must be a node
        final Object childObject = fxomInstance.getSceneGraphObject();
        if ((childObject instanceof Node) == false) {
            return result; // subJobs is empty => isExecutable will return false
        }
        // Preview version : Node must be resizable (as in SB 1.1)
        // TODO : if the object is not resizable, 
        // update its bounds but do not anchor it.
        final Node childNode = (Node) childObject;
        if (childNode.isResizable() == false) {
            return result; // subJobs is empty => isExecutable will return false
        }
        // Preview version : Parent node must be an AnchorPane (as in SB 1.1)
        // TODO : if the object container is a Pane, 
        // update its bounds but do not anchor it.
        final Object parentObject = parentInstance.getSceneGraphObject();
        if ((parentObject instanceof AnchorPane) == false) {
            return result; // subJobs is empty => isExecutable will return false
        }

        final AnchorPane parentNode = (AnchorPane) parentObject;
        final Bounds childBounds = childNode.getLayoutBounds();
        final Bounds parentBounds = parentNode.getLayoutBounds();
        Scale scale = null;
        for (Transform transform : childNode.getTransforms()) {
            if (transform instanceof Scale) {
                scale = (Scale) transform;
            }
        }
        double scaleX = scale == null ? 1.0 : scale.getX();
        double scaleY = scale == null ? 1.0 : scale.getY();
        final Set<Sizing> sizing = getSizingMask(childNode);
        final boolean isResizableX = sizing.contains(Sizing.HORIZONTAL);
        final boolean isResizableY = sizing.contains(Sizing.VERTICAL);
        double leftAnchorValue = isResizableX ? 0
                : (parentBounds.getWidth() - childBounds.getWidth() * scaleX) / 2.0;
        double topAnchorValue = isResizableY ? 0
                : (parentBounds.getHeight() - childBounds.getHeight() * scaleY) / 2.0;
        double prefWidthValue = isResizableX ? parentBounds.getWidth() / scaleY
                : childBounds.getWidth();
        double prefHeightValue = isResizableY ? parentBounds.getHeight() / scaleY
                : childBounds.getHeight();

        // Modify pref size jobs
        //----------------------------------------------------------------------
        final Job prefWidthJob = modifyJob("prefWidth", prefWidthValue);
        if (prefWidthJob.isExecutable()) { // Update if new value differs from old one
            result.add(prefWidthJob);
        }
        final Job prefHeightJob = modifyJob("prefHeight", prefHeightValue);
        if (prefHeightJob.isExecutable()) { // Update if new value differs from old one
            result.add(prefHeightJob);
        }

        // Modify Anchors Jobs
        //----------------------------------------------------------------------
        final Job leftAnchorJob = modifyAnchorJob(Anchor.LEFT, leftAnchorValue);
        if (leftAnchorJob.isExecutable()) { // Update if new value differs from old one
            result.add(leftAnchorJob);
        }
        final Job topAnchorJob = modifyAnchorJob(Anchor.TOP, topAnchorValue);
        if (topAnchorJob.isExecutable()) { // Update if new value differs from old one
            result.add(topAnchorJob);
        }
        if (isResizableX) {
            final Job rightAnchorJob = modifyAnchorJob(Anchor.RIGHT, 0.0);
            if (rightAnchorJob.isExecutable()) { // Update if new value differs from old one
                result.add(rightAnchorJob);
            }
        }
        if (isResizableY) {
            final Job bottomAnchorJob = modifyAnchorJob(Anchor.BOTTOM, 0.0);
            if (bottomAnchorJob.isExecutable()) { // Update if new value differs from old one
                result.add(bottomAnchorJob);
            }
        }
        return result;
    }
    
    @Override
    protected String makeDescription() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Fit to Parent ");
        final Object sceneGraphObject = fxomInstance.getSceneGraphObject();
        assert sceneGraphObject != null;
        sb.append(sceneGraphObject.getClass().getSimpleName());
        return sb.toString();
    }

    private Job modifyJob(final Class<?> clazz, final String name, double value) {
        final PropertyName pn = new PropertyName(name, clazz);
        final ValuePropertyMetadata vpm
                = Metadata.getMetadata().queryValueProperty(fxomInstance, pn);
        final ModifyObjectJob subJob = new ModifyObjectJob(
                fxomInstance, vpm, value, getEditorController());
        return subJob;
    }

    private Job modifyJob(final String name, double value) {
        return modifyJob(null, name, value);
    }

    private Job modifyAnchorJob(final Anchor anchor, double value) {
        final String name = anchor.name().toLowerCase(Locale.ROOT) + "Anchor";
        return modifyJob(AnchorPane.class, name, value);
    }

    private Set<Sizing> getSizingMask(final Node node) {
        Set<Sizing> result;

        // ScrollBar
        if (node instanceof ScrollBar) {
            final ScrollBar scrollBar = (ScrollBar) node;
            result = getSizingMask(scrollBar.getOrientation());
        } //
        // Separator
        else if (node instanceof Separator) {
            final Separator separator = (Separator) node;
            result = getSizingMask(separator.getOrientation());
        } //
        // Slider
        else if (node instanceof Slider) {
            final Slider slider = (Slider) node;
            result = getSizingMask(slider.getOrientation());
        } //
        else {
            result = EnumSet.of(Sizing.HORIZONTAL, Sizing.VERTICAL);
        }
        return result;
    }

    private Set<Sizing> getSizingMask(final Orientation orientation) {
        assert orientation != null;
        final Set<Sizing> result;
        switch (orientation) {
            case HORIZONTAL:
                result = EnumSet.of(Sizing.HORIZONTAL);
                break;
            case VERTICAL:
                result = EnumSet.of(Sizing.VERTICAL);
                break;
            default:
                assert false : "unexpected orientation: " + orientation;
                result = null;
        }
        return result;
    }
}
