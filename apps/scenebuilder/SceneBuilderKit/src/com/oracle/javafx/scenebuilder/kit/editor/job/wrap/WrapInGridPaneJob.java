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
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.JobUtils;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.FXOMObjectCourseComparator.BidimensionalComparator;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.FXOMObjectCourseComparator.GridCourse;
import static com.oracle.javafx.scenebuilder.kit.editor.job.wrap.FXOMObjectCourseComparator.OVERLAP_FUZZ;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

/**
 * Job used to wrap selection in a GridPane.
 */
public class WrapInGridPaneJob extends AbstractWrapInSubComponentJob {

    private static final double DEFAULT_MIN_WIDTH = 10;
    private static final double DEFAULT_MIN_HEIGHT = 10;
    // Key = FXOM object
    // Value = 2 dimensions integer array for the COLUMN and ROW index
    private final Map<FXOMObject, int[]> indices = new HashMap<>();

    public WrapInGridPaneJob(EditorController editorController) {
        super(editorController);
        newContainerClass = GridPane.class;
    }

    @Override
    protected List<Job> modifyChildrenJobs(final List<FXOMObject> children) {
        final List<Job> jobs = super.modifyChildrenJobs(children);

        for (FXOMObject child : children) {
            int[] childIndices = indices.get(child);

            // Modify child column index
            final ModifyObjectJob modifyColumnIndex = WrapJobUtils.modifyObjectJob(
                    (FXOMInstance) child, GridPane.class, "columnIndex", //NOI18N
                    childIndices[GridCourse.COL_BY_COL.index()],
                    getEditorController());
            jobs.add(modifyColumnIndex);

            // Modify child row index
            final ModifyObjectJob modifyRowIndex = WrapJobUtils.modifyObjectJob(
                    (FXOMInstance) child, GridPane.class, "rowIndex", //NOI18N
                    childIndices[GridCourse.ROW_BY_ROW.index()],
                    getEditorController());
            jobs.add(modifyRowIndex);
        }
        return jobs;
    }

    @Override
    protected void modifyNewContainer(final List<FXOMObject> children) {
        super.modifyNewContainer(children);

        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();

        // Update the GridPane constraints depending on its children positionning
        // Find and set the column index for each element in the indices map.
        final int maxcol = computeIndexByCourse(children, GridCourse.COL_BY_COL, indices);
        // Find and set the row index for each element in the indices map.
        final int maxrow = computeIndexByCourse(children, GridCourse.ROW_BY_ROW, indices);
        final double[] columnWidth = new double[maxcol + 1];
        final double[] rowHeight = new double[maxrow + 1];
        computeSizes(children, indices, columnWidth, rowHeight);

        // COLUMNS
        for (int index = 0; index <= maxcol; index++) {
            final FXOMInstance constraint = makeConstraintsInstance(ColumnConstraints.class);
            JobUtils.setHGrow(constraint, ColumnConstraints.class, Priority.SOMETIMES.name());
            if (columnWidth[index] >= DEFAULT_MIN_WIDTH) {
                JobUtils.setMinWidth(constraint, ColumnConstraints.class, DEFAULT_MIN_WIDTH);
            } else {
                JobUtils.setMinWidth(constraint, ColumnConstraints.class, columnWidth[index]);
            }

            JobUtils.addColumnConstraints(fxomDocument, newContainer, constraint, index);
        }

        // ROWS
        for (int index = 0; index <= maxrow; index++) {
            final FXOMInstance constraint = makeConstraintsInstance(RowConstraints.class);
            JobUtils.setVGrow(constraint, RowConstraints.class, Priority.SOMETIMES.name());
            if (rowHeight[index] >= DEFAULT_MIN_HEIGHT) {
                JobUtils.setMinHeight(constraint, RowConstraints.class, DEFAULT_MIN_HEIGHT);
            } else {
                JobUtils.setMinHeight(constraint, RowConstraints.class, rowHeight[index]);
            }

            JobUtils.addRowConstraints(fxomDocument, newContainer, constraint, index);
        }
    }

    /**
     * This method computes either the ROW index or COLUMN index of each
     * element, by running through a would-be grid according to a given course.
     * For instance, when course==ROW_BY_ROW, this method first order the
     * elements row by row, and then sets their ROW index inside the indices
     * map. When course==COL_BY_COL, this method orders the elements column by
     * column, and then sets their COLUMN index inside the indices map. Note
     * that this method leaves the original elements and children list
     * unchanged. All it does is populating the indices map.
     *
     * @param fxomObjects The children of the would-be grid.
     * @param course The course for which this method runs.
     * @param indices The indices map.
     * @return the greater index.
     */
    private int computeIndexByCourse(
            final List<FXOMObject> fxomObjects,
            final GridCourse course,
            final Map<FXOMObject, int[]> indices) {

        final BidimensionalComparator comparator = new BidimensionalComparator(course);
        final List<FXOMObject> unsorted = new ArrayList<>(fxomObjects);
        Collections.sort(unsorted, comparator);
        FXOMObject lastObject = null;
        int rc = 0;
        int max = -1;
        for (int i = 0; i < unsorted.size(); i++) {
            FXOMObject currentObject = unsorted.get(i);
            int[] ind = indices.get(currentObject);
            if (ind == null) {
                ind = new int[2];
                indices.put(currentObject, ind);
            }
            if (lastObject != null) {
                if (comparator.compare(lastObject, currentObject) != 0) {
                    final Node lastNode = (Node) lastObject.getSceneGraphObject();
                    final Node currentNode = (Node) currentObject.getSceneGraphObject();
                    final Bounds lastBounds = lastNode.getBoundsInParent();
                    final Bounds currentBounds = currentNode.getBoundsInParent();
                    if (course.getMinY(currentBounds) >= course.getMaxY(lastBounds) - OVERLAP_FUZZ) {
                        rc++;
                    }
                }
            }
            ind[course.index()] = rc;
            max = Math.max(max, rc);
            lastObject = currentObject;
        }
        return max;
    }

    private void computeSizes(
            final List<FXOMObject> fxomObjects,
            final Map<FXOMObject, int[]> indices,
            double[] columnWidth, double[] rowHeight) {

        for (FXOMObject fxomObject : fxomObjects) {
            final Node node = (Node) fxomObject.getSceneGraphObject();
            final double width = node.getBoundsInLocal().getWidth();
            final double height = node.getBoundsInLocal().getHeight();
            final int[] ind = indices.get(fxomObject);
            final int col = ind[GridCourse.COL_BY_COL.index()];
            final int row = ind[GridCourse.ROW_BY_ROW.index()];
            columnWidth[col] = Math.max(columnWidth[col], width);
            rowHeight[row] = Math.max(rowHeight[row], height);
        }
    }

    private FXOMInstance makeConstraintsInstance(final Class<?> constraintsClass) {

        // Create new constraints instance
        final FXOMDocument newDocument = new FXOMDocument();
        final FXOMInstance result
                = new FXOMInstance(newDocument, constraintsClass);
        newDocument.setFxomRoot(result);
        result.moveToFxomDocument(getEditorController().getFxomDocument());

        return result;
    }
}
