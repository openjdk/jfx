/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package ensemble.samplepage;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.layout.Region;

/**
 * Container for a Sample, responsible for sizing and centering sample
 */
public class SampleContainer extends Region {

    private final boolean resizable;
    private final Parent sampleNode;

    public SampleContainer(Parent sampleNode) {
        this.sampleNode = sampleNode;
        resizable = sampleNode.isResizable() &&
                (sampleNode.maxWidth(-1) == 0 || sampleNode.maxWidth(-1) > sampleNode.minWidth(-1))
                && (sampleNode.maxHeight(-1) == 0 || sampleNode.maxHeight(-1) > sampleNode.minHeight(-1));
        getChildren().add(sampleNode);
        getStyleClass().add("sample-container");
    }

    @Override protected void layoutChildren() {
        super.layoutChildren();
        double sw = sampleNode.getLayoutBounds().getWidth();
        double sh = sampleNode.getLayoutBounds().getHeight();
        double scale = Math.min(getWidth() / sw, getHeight() / sh);
        if (resizable) {
            sw *= scale;
            sh *= scale;
            if (sampleNode.maxWidth(-1) > 0) {
                sw = Math.min(sw, sampleNode.maxWidth(-1));
            }
            if (sampleNode.maxHeight(-1) > 0) {
                sh = Math.min(sh, sampleNode.maxHeight(-1));
            }
            sampleNode.resizeRelocate(Math.round((getWidth() - sw) / 2), Math.round((getHeight() - sh) / 2), sw, sh);
        } else {
            // We never scale up the sample
            scale = Math.min(1, scale);
            sampleNode.setScaleX(scale);
            sampleNode.setScaleY(scale);
            layoutInArea(sampleNode, 0, 0, getWidth(), getHeight(), 0, HPos.CENTER, VPos.CENTER);
        }
    }

    @Override public double getBaselineOffset() {
        return super.getBaselineOffset();
    }
}
