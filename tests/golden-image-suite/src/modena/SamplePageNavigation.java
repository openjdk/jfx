/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package modena;

import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;

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
        sectionChoiceBox.getItems().addAll(samplePage.getSections());
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
