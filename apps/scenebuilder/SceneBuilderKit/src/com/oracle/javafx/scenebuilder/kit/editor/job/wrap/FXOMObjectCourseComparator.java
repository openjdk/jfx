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
package com.oracle.javafx.scenebuilder.kit.editor.job.wrap;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.Comparator;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;

/**
 * Comparator used to sort nodes row by row or column by column.
 */
public class FXOMObjectCourseComparator {

    private static final int B1GREATER = 1;
    private static final int B2GREATER = -1;
    // The "fuzz" here means that the algorithm which dispatches nodes into
    // grid cells will be a little more lenient and allow 2 nodes which 
    // overlap by 1pixel only to be dispatched each in its own cell.
    //
    // So if my two buttons boundsInParent overlap by one pixel - they 
    // will still be assigned to two neighbor cells instead of being 
    // put in the same cell.
    public static final double OVERLAP_FUZZ = 1.0;

    /**
     * A GridCourse defines a means to sort nodes into a grid by following a
     * given course.
     * The ROW_BY_ROW course run through the grid row by row, from left to
     * right and top to bottom.
     * The COL_BY_COL course run through the grid column by column, from top
     * to bottom and left to right.
     */
    static enum GridCourse {

        ROW_BY_ROW, COL_BY_COL;

        public double getMinX(Bounds b) {
            switch (this) {
                case ROW_BY_ROW:
                    return b.getMinX();
                case COL_BY_COL:
                    return b.getMinY();
            }
            throw new IllegalArgumentException(String.valueOf(this));
        }

        public double getMaxX(Bounds b) {
            switch (this) {
                case ROW_BY_ROW:
                    return b.getMaxX();
                case COL_BY_COL:
                    return b.getMaxY();
            }
            throw new IllegalArgumentException(String.valueOf(this));
        }

        public double getMinY(Bounds b) {
            switch (this) {
                case ROW_BY_ROW:
                    return b.getMinY();
                case COL_BY_COL:
                    return b.getMinX();
            }
            throw new IllegalArgumentException(String.valueOf(this));
        }

        public double getMaxY(Bounds b) {
            switch (this) {
                case ROW_BY_ROW:
                    return b.getMaxY();
                case COL_BY_COL:
                    return b.getMaxX();
            }
            throw new IllegalArgumentException(String.valueOf(this));
        }

        public int index() {
            switch (this) {
                case ROW_BY_ROW:
                    return 0;
                case COL_BY_COL:
                    return 1;
            }
            throw new IllegalArgumentException(String.valueOf(this));
        }
    }

    /** *************************************************************************
     *                                                                         *
     * Comparator on row axis AND column axis *
     *                                                                         *
     ************************************************************************* */
    static class BidimensionalComparator implements Comparator<FXOMObject> {

        private final GridCourse course;
        private static final long serialVersionUID = 0;

        public BidimensionalComparator(GridCourse course) {
            this.course = course;
        }

        @Override
        public int compare(FXOMObject o1, FXOMObject o2) {
            assert o1.getSceneGraphObject() != null
                    && o1.getSceneGraphObject() instanceof Node;
            assert o2.getSceneGraphObject() != null
                    && o2.getSceneGraphObject() instanceof Node;
            final Node n1 = (Node) o1.getSceneGraphObject();
            final Node n2 = (Node) o2.getSceneGraphObject();
            final Bounds b1 = n1.getBoundsInParent();
            final Bounds b2 = n2.getBoundsInParent();
            int test1 = compareBounds(b1, b2);
            if (test1 != 0) {
                return test1;
            }
            int test2 = compareBounds(b2, b1);
            if (test2 != 0) {
                return -test2;
            }
            return 0;
        }

        // Used to order elements in natural western reading order:
        // begins at the top, and goes left to right, row by row.
        private int compareBounds(Bounds b1, Bounds b2) {
            if (course.getMaxY(b2) - OVERLAP_FUZZ <= course.getMinY(b1)) {
                return B1GREATER;
            }
            if (course.getMinY(b2) + OVERLAP_FUZZ >= course.getMaxY(b1)) {
                return B2GREATER;
            }
            if (course.getMaxX(b2) - OVERLAP_FUZZ <= course.getMinX(b1)) {
                return B1GREATER;
            }
            if (course.getMinX(b2) + OVERLAP_FUZZ >= course.getMaxX(b1)) {
                return B2GREATER;
            }
            return 0;
        }
    }

    /** *************************************************************************
     *                                                                         *
     * Comparator on row axis OR column axis *
     *                                                                         *
     ************************************************************************* */
    static class UnidimensionalComparator implements Comparator<FXOMObject> {

        private final GridCourse course;
        private static final long serialVersionUID = 0;

        public UnidimensionalComparator(GridCourse course) {
            this.course = course;
        }

        public static UnidimensionalComparator of(Orientation orientation) {
            switch (orientation) {
                case HORIZONTAL:
                    return new UnidimensionalComparator(GridCourse.ROW_BY_ROW);
                case VERTICAL:
                    return new UnidimensionalComparator(GridCourse.COL_BY_COL);
            }
            throw new IllegalArgumentException(String.valueOf(orientation));
        }

        @Override
        public int compare(FXOMObject o1, FXOMObject o2) {
            assert o1.getSceneGraphObject() != null
                    && o1.getSceneGraphObject() instanceof Node;
            assert o2.getSceneGraphObject() != null
                    && o2.getSceneGraphObject() instanceof Node;
            final Node n1 = (Node) o1.getSceneGraphObject();
            final Node n2 = (Node) o2.getSceneGraphObject();
            final Bounds b1 = n1.getBoundsInParent();
            final Bounds b2 = n2.getBoundsInParent();
            return compareBounds(b1, b2);
        }

        // Used to order elements in natural western reading order:
        // begins at the top, and goes left to right, row by row.
        private int compareBounds(Bounds b1, Bounds b2) {
            if (course.getMinX(b2) < course.getMinX(b1)) {
                return B1GREATER;
            }
            if (course.getMinX(b2) > course.getMinX(b1)) {
                return B2GREATER;
            }
            return 0;
        }
    }
}
