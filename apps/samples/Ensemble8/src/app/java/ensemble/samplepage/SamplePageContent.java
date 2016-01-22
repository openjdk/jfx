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

import ensemble.PlatformFeatures;
import ensemble.SampleInfo;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.util.Callback;

import static ensemble.samplepage.SamplePage.INDENT;

/**
 * The content for Sample Page
 */
class SamplePageContent extends Region {

    private Node playground;
    private Description description;
    private SampleContainer sampleContainer;
    private boolean needsPlayground;
    final SamplePage samplePage;

    SamplePageContent(final SamplePage samplePage) {
        this.samplePage = samplePage;
        playground = new PlaygroundNode(samplePage);
        description = new Description(samplePage);
        samplePage.registerSampleInfoUpdater((SampleInfo sampleInfo) -> {
            update(sampleInfo);
            return null;
        });
    }

    @Override protected void layoutChildren() {
        super.layoutChildren();

        double maxWidth = getWidth() - 2 * INDENT;
        double maxHeight = getHeight() - 2 * INDENT;

        boolean landscape = getWidth() >= getHeight();
        boolean wide = getWidth() >= getHeight() * 1.5;
        if (wide) {
            // Sample on right, everything else on left
            double x = Math.round(getWidth() / 2 + INDENT / 2);
            double w = getWidth() - INDENT - x;
            sampleContainer.resizeRelocate(x, INDENT, (getWidth() - 3 * INDENT) / 2, maxHeight);
            if (needsPlayground) {
                double h = (getHeight() - INDENT * 3) / 2;
                description.resizeRelocate(INDENT, INDENT, w, h);
                playground.resizeRelocate(INDENT, Math.round(INDENT * 2 + h), w, h);
            } else {
                description.resizeRelocate(INDENT, INDENT, w, maxHeight);
            }
        } else {
            // Sample on top, everything else on bottom
            sampleContainer.resizeRelocate(INDENT, INDENT, maxWidth, (getHeight() - 3 * INDENT) / 2);
            double y = Math.round(getHeight() / 2 + INDENT / 2);
            if (landscape) {
                double h = getHeight() - INDENT - y;
                if (needsPlayground) {
                    double w = (getWidth() - INDENT * 3) / 2;
                    playground.resizeRelocate(INDENT, y, w, h);
                    description.resizeRelocate(Math.round(INDENT * 2 + w), y, w, h);
                } else {
                    description.resizeRelocate(INDENT, y, maxWidth, h);
                }
            } else {
                double w = getWidth() - INDENT * 2;
                if (needsPlayground) {
                    double h = (getHeight() - INDENT * 2 - y) / 2;
                    playground.resizeRelocate(INDENT, y, w, h);
                    description.resizeRelocate(INDENT, Math.round(y + h + INDENT), w, h);
                } else {
                    double h = getHeight() - INDENT - y;
                    description.resizeRelocate(INDENT, y, w, h);
                }
            }
        }
    }

    static Text title(String text) {
        Text title = new Text(text);
        title.getStyleClass().add("sample-page-box-title");
        return title;
    }

    private void update(SampleInfo sampleInfo) {
        sampleContainer = new SampleContainer(samplePage.sampleRuntimeInfoProperty.get().getSampleNode());
        sampleContainer.getStyleClass().add("sample-page-sample-node");
        if (!PlatformFeatures.DISPLAY_PLAYGROUND) {
            needsPlayground = false;
        } else {
            needsPlayground = sampleInfo.needsPlayground();
        }
        if (needsPlayground) {
            getChildren().setAll(sampleContainer, playground, description);
        } else {
            getChildren().setAll(sampleContainer, description);
        }
    }
}
