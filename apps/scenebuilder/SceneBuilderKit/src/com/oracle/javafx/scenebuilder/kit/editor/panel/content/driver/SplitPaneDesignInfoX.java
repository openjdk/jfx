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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver;

import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.control.SplitPane;

/**
 * Some additional verbs for managing SplitPane at design time.
 * This could potentially move to SplitPaneDesignInfo some day.
 *
 */
public class SplitPaneDesignInfoX  {


    /**
     * Convert from local coordinates to divider position coordinates (0-1).
     *
     * @param splitPane split pane subject of the conversion
     * @param x x in local coordinates
     * @param y y in local coordinates
     * @param clamp true if resulting divider position must be clamped in [0,1].
     * @param snap true if resulting divider position should be rounded to 2 decimals.
     * @return a divider position (between [0, 1] if clamped == true).
     */
    public double splitPaneLocalToDividerPosition(SplitPane splitPane, double x, double y, boolean clamp, boolean snap) {
        final boolean verticalSplit = splitPane.getOrientation() == Orientation.VERTICAL;
        final Bounds lb = splitPane.getLayoutBounds();
        double result;

        if (verticalSplit) {
            assert lb.getHeight() != 0;
            result = (y - lb.getMinY()) / lb.getHeight();
        } else {
            assert lb.getWidth() != 0;
            result = (x - lb.getMinX()) / lb.getWidth();
        }

        if (clamp) {
            result = Math.max(0, Math.min(1.0, result));
        }

        if (snap) {
            result = Math.round(result * 100.0) / 100.0;
        }

        return result;
    }

    /**
     * Convert from divider position coordinates (0-1) to local coordinates.
     *
     * @param splitPane split pane subject of the conversion
     * @param position a divider position
     * @return
     */
    public double dividerPositionToSplitPaneLocal(SplitPane splitPane, double position) {
        final boolean verticalSplit = splitPane.getOrientation() == Orientation.VERTICAL;
        final Bounds lb = splitPane.getLayoutBounds();
        double result;

        if (verticalSplit) {
            result = lb.getMinY() + position * lb.getHeight();
            //assert splitPaneLocalToDividerPosition(0.0, result, false /* clamp */) == position;
        } else {
            result = lb.getMinX() + position * lb.getWidth();
            //assert splitPaneLocalToDividerPosition(result, 0, false /* clamp */) == position;
        }

        return result;
    }


    /**
     * Computes what would be the divider positions of splitpane if
     * divider at dividerIndex was moved to (sceneX, sceneY).
     *
     * @param splitPane the splitpane to which simulated move applies
     * @param dividerIndex the index of the divider that should be moved
     * @param sceneX x of the target position in scene coordinates
     * @param sceneY y of the target position in scene coordinates
     * @return the array of divider positions after the simulated move
     */

    public double[] simulateDividerMove(SplitPane splitPane, int dividerIndex, double sceneX, double sceneY) {
        final List<SplitPane.Divider> dividers = splitPane.getDividers();
        final double currentPos = dividers.get(dividerIndex).getPosition();
        final Point2D p = splitPane.sceneToLocal(sceneX, sceneY);
        final double claimedPos = splitPaneLocalToDividerPosition(splitPane, p.getX(), p.getY(), true /* clamp */, false /* snap */);
        double minPos, maxPos, newPos;
        final double[] result;

        if (0 <= dividerIndex-1) {
            minPos = dividers.get(dividerIndex-1).getPosition();
            assert minPos <= currentPos;
        } else {
            minPos = 0.0;
        }
        if (dividerIndex+1 < dividers.size()) {
            maxPos = dividers.get(dividerIndex+1).getPosition();
            assert currentPos <= maxPos;
        } else {
            maxPos = 1.0;
        }

        if (claimedPos < minPos) {
            // We need to adjust dividers on the left
            // TODO(elp)
            newPos = minPos;
        } else if (maxPos < claimedPos) {
            // We need to adjust dividers on the right
            // TODO(elp)
            newPos = maxPos;
        } else {
            // No need to adjust sibling dividers
            newPos = claimedPos;
        }

        // Clone dividerPositions and update position at dividerIndex
        result = new double[dividers.size()];
        for (int i = 0; i < dividers.size(); i++) {
            result[i] = dividers.get(i).getPosition();
        }
        result[dividerIndex] = newPos;

        return result;
    }
}
