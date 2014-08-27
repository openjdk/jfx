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
import com.oracle.javafx.scenebuilder.kit.editor.job.JobUtils;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.FXOMObjectCourseComparator.BidimensionalComparator;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.FXOMObjectCourseComparator.GridCourse;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.FXOMObjectCourseComparator.UnidimensionalComparator;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;

/**
 * Job used to wrap selection in a SplitPane.
 */
public class WrapInSplitPaneJob extends AbstractWrapInSubComponentJob {

    public WrapInSplitPaneJob(EditorController editorController) {
        super(editorController);
        newContainerClass = SplitPane.class;
    }

    @Override
    protected void modifyNewContainer(final List<FXOMObject> children) {
        super.modifyNewContainer(children);

        // Update the SplitPane orientation depending on its children positionning
        final Orientation orientation = getOrientation(children);
        JobUtils.setOrientation(newContainer, SplitPane.class, orientation.name());
    }
    
    @Override
    protected Collection<FXOMObject> sortChildren(List<FXOMObject> children) {
        final List<FXOMObject> sorted = new ArrayList<>(children);
        final Orientation orientation = getOrientation(children);
        Collections.sort(sorted, UnidimensionalComparator.of(orientation));
        return sorted;
    }

    private Orientation getOrientation(final List<FXOMObject> fxomObjects) {
        int cols = computeSizeByCourse(fxomObjects, GridCourse.COL_BY_COL);
        if (cols == fxomObjects.size()) {
            return Orientation.HORIZONTAL;
        }
        int rows = computeSizeByCourse(fxomObjects, GridCourse.ROW_BY_ROW);
        if (rows == fxomObjects.size()) {
            return Orientation.VERTICAL;
        }
        final Orientation orientation = cols >= rows
                ? Orientation.HORIZONTAL : Orientation.VERTICAL;
        return orientation;
    }

    private int computeSizeByCourse(
            final List<FXOMObject> fxomObjects,
            final GridCourse course) {

        final BidimensionalComparator comparator = new BidimensionalComparator(course);
        FXOMObject lastObject = null;
        int rc = 0;
        int max = -1;
        for (FXOMObject currentObject : fxomObjects) {
            if (lastObject != null) {
                if (comparator.compare(lastObject, currentObject) != 0) {
                    final Node lastNode = (Node) lastObject.getSceneGraphObject();
                    final Node currentNode = (Node) currentObject.getSceneGraphObject();
                    final Bounds lastBounds = lastNode.getBoundsInParent();
                    final Bounds currentBounds = currentNode.getBoundsInParent();
                    if (course.getMinY(currentBounds) >= course.getMaxY(lastBounds)) {
                        rc++;
                    }
                }
            }
            max = Math.max(max, rc);
            lastObject = currentObject;
        }
        return max;
    }
}
