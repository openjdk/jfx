/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates.
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
import javafx.scene.layout.Region;


/**
 *
 */
class BackPage extends Region {
    private Sources sources;
    private final SamplePage samplePage;

    BackPage(final SamplePage samplePage) {
        this.samplePage = samplePage;
        getStyleClass().add("sample-back-page");
        sources = new Sources(samplePage);
        getChildren().setAll(sources);
    }

    @Override protected void layoutChildren() {
        super.layoutChildren();
        if (getWidth() > getHeight()) {
            // Landscape layout
            final double RIGHT = 60;
            final double LEFT = 80;
            double w = getWidth() - RIGHT - LEFT;
            layoutInArea(sources, LEFT, SamplePage.INDENT, w, getHeight() - SamplePage.INDENT * 2, 0, HPos.LEFT, VPos.TOP);
        } else {
            // Portrait layout
            final double TOP = 80;
            final double RIGHT = SamplePage.INDENT;
            final double BOTTOM = 70;
            layoutInArea(sources, SamplePage.INDENT, TOP, getWidth() - RIGHT - SamplePage.INDENT, getHeight() - BOTTOM - TOP, 0, HPos.LEFT, VPos.TOP);
        }
    }
}
