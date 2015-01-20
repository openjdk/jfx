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
package modena;

import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Container for samplePage that has scrolling and knows how to navigate to sections
 */
public class SamplePageNavigation extends BorderPane {
    private SamplePage samplePage = new SamplePage();
    private ScrollPane scrollPane = new ScrollPane(samplePage);
    private boolean isLocalChange = false;
    private SamplePage.Section currentSection;

    public SamplePageNavigation() {
        scrollPane.setId("SamplePageScrollPane");
        setCenter(scrollPane);
        ToolBar toolBar = new ToolBar();
        toolBar.setId("SamplePageToolBar");
        toolBar.getStyleClass().add("bottom");
        toolBar.getItems().add(new Label("Go to section:"));
        final ChoiceBox<SamplePage.Section> sectionChoiceBox = new ChoiceBox<>();
        sectionChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> setCurrentSection(newValue));
        List<SamplePage.Section> sections = new ArrayList<>(samplePage.getSections());
        Collections.sort(sections, (o1, o2) -> o1.name.compareTo(o2.name));
        sectionChoiceBox.getItems().addAll(sections);
        toolBar.getItems().add(sectionChoiceBox);
        setBottom(toolBar);
        scrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (!isLocalChange) {
                isLocalChange = true;
                // calc scroll position relative to scroll pane content
                double posPixels = samplePage.getLayoutBounds().getHeight() * newValue.doubleValue();
                // move to top of view port
                posPixels -=  scrollPane.getLayoutBounds().getHeight() * newValue.doubleValue();
                // move to center of view port
                posPixels +=  scrollPane.getLayoutBounds().getHeight() * 0.5;
                // find section that contains view port center
                currentSection = null;
                for (SamplePage.Section section: samplePage.getSections()) {
                    if (section.box.getBoundsInParent().getMaxY() > posPixels ) {
                        currentSection = section;
                        break;
                    }
                }
                sectionChoiceBox.getSelectionModel().select(currentSection);
                isLocalChange = false;
            }

        });
    }

    public SamplePage.Section getCurrentSection() {
        return currentSection;
    }

    public void setCurrentSection(SamplePage.Section currentSection) {
        this.currentSection = currentSection;
        if (!isLocalChange) {
            isLocalChange = true;
            double pos = 0;
            if (currentSection != null) {
                double sectionBoxCenterY = currentSection.box.getBoundsInParent().getMinY()
                        + (currentSection.box.getBoundsInParent().getHeight()/2);
                // move to center of view port
                pos -=  scrollPane.getLayoutBounds().getHeight() * 0.5;
                // move to top of view port
                pos +=  scrollPane.getLayoutBounds().getHeight() * (sectionBoxCenterY / samplePage.getLayoutBounds().getHeight());
                // find relative pos
                pos = sectionBoxCenterY / samplePage.getLayoutBounds().getHeight();
            }
            scrollPane.setVvalue(pos);
            isLocalChange = false;
        }
    }

    public SamplePage getSamplePage() {
        return samplePage;
    }
}
