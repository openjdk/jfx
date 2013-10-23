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

import javafx.beans.binding.ObjectBinding;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButtonBuilder;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Region;

/**
 *
 */
public class IPhoneLayout extends Region {
    
    private PlaygroundTabs playground;
    private Parent sampleNode;
    private SampleContainer sample;
    private Description description;
    private Sources sources;
    private Parent iPhoneTabs;
    
    public IPhoneLayout(SamplePage samplePage) {
        sample = new SampleContainer(sampleNode = samplePage.sampleInfo.getSampleNode());
        description = new Description(samplePage);
        sources = new Sources(samplePage);
        if (samplePage.sample.needsPlayground()) {
            playground = new PlaygroundTabs(samplePage);
            getChildren().add(playground);
        }
        iPhoneTabs = buildIPhoneTabs();
        getStyleClass().add("sample-page-iphone");
        getChildren().addAll(sample, description, sources, iPhoneTabs);
    }

    @Override protected void layoutChildren() {
        super.layoutChildren();
        
        double w = getWidth();
        double bh = iPhoneTabs.prefHeight(w);
        double sh = getHeight() - bh;
        
        if (playground != null) {
            playground.resizeRelocate(0, 0, w, sh);
        }
        sample.resizeRelocate(0, 0, w, sh);
        description.resizeRelocate(0, 0, w, sh);
        sources.resizeRelocate(0, 0, w, sh);
        iPhoneTabs.resizeRelocate(0, sh, w, bh);
    }
    
    private Parent buildIPhoneTabs() {
        final ToggleGroup shownPage = new ToggleGroup();
        ObjectBinding<Object> selectedToggleUserData = new ObjectBinding<Object>() {

                    {
                        bind(shownPage.selectedToggleProperty());
                    }

                    @Override
                    protected Object computeValue() {
                        Toggle selectedToggle = shownPage.getSelectedToggle();
                        if (selectedToggle == null) {
                            return null;
                        } else {
                            return selectedToggle.getUserData();
                        }
                    }
                };
        if (playground != null) {
            playground.visibleProperty().bind(selectedToggleUserData.isEqualTo(playground));
        }
        sample.visibleProperty().bind(selectedToggleUserData.isEqualTo(sample));
        description.visibleProperty().bind(selectedToggleUserData.isEqualTo(description));
        sources.visibleProperty().bind(selectedToggleUserData.isEqualTo(sources));
        
        HBox hbox = HBoxBuilder.create()
                .styleClass("sample-page-iphone-bottom-bar")
                .alignment(Pos.CENTER)
                .children(
                    ToggleButtonBuilder.create()
                        .text("Description")
                        .toggleGroup(shownPage)
                        .userData(description)
                        .build(),
                    ToggleButtonBuilder.create()
                        .text("Sample")
                        .toggleGroup(shownPage)
                        .selected(true)
                        .userData(sample)
                        .build()
                ).build();
        if (playground != null) {
            hbox.getChildren().add(
                    ToggleButtonBuilder.create()
                        .text("Playground")
                        .toggleGroup(shownPage)
                        .userData(playground)
                        .build());
        }
        hbox.getChildren().add(
                ToggleButtonBuilder.create()
                    .text("Sources")
                    .toggleGroup(shownPage)
                    .userData(sources)
                    .build());
        return hbox;
    }
}
