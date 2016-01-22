/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates.
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
package ensemble;

import ensemble.generated.Samples;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

/**
 * Ensmeble page for showing a page of documentation from the web
 */
public class DocsPage extends Region implements ChangeListener<String>, Page{
    private final WebView webView = new WebView();
    private final ScrollPane scrollPane = new ScrollPane();
    private final VBox sideBar = new VBox(10);
    private final Label sideBarTitle = new Label("Related Samples:");
    private final PageBrowser pageBrowser;
    private boolean isLocalChange = false;
    private boolean showSideBar = false;

    public DocsPage(PageBrowser pageBrowser) {
        this.pageBrowser = pageBrowser;
        getChildren().add(webView);
        scrollPane.setContent(sideBar);
        sideBar.setAlignment(Pos.TOP_CENTER);
        sideBar.getChildren().add(sideBarTitle);
        sideBarTitle.getStyleClass().add("sidebar-title");
        scrollPane.setFitToWidth(true);
        sideBar.setPadding(new Insets(10));
        webView.getEngine().locationProperty().addListener(this);
    }

    @Override public ReadOnlyStringProperty titleProperty() {
        return webView.getEngine().titleProperty();
    }

    @Override public String getTitle() {
        return webView.getEngine().getTitle();
    }

    @Override public String getUrl() {
        return webView.getEngine().getLocation();
    }

    @Override public Node getNode() {
        return this;
    }

    @Override public void changed(ObservableValue<? extends String> ov, String oldLocation, String newLocation) {
        if (!isLocalChange) pageBrowser.externalPageChange(newLocation);
        updateSidebar(newLocation);
    }

    public void goToUrl(String url) {
        isLocalChange = true;
        webView.getEngine().load(url);
        isLocalChange = false;
    }

    @Override protected void layoutChildren() {
        final double w = getWidth();
        final double h = getHeight();
        if (showSideBar) {
            final double sideBarWidth = sideBar.prefWidth(-1)+14;
            webView.resize(w - sideBarWidth, h);
            scrollPane.setLayoutX(w - sideBarWidth);
            scrollPane.resize(sideBarWidth, h);
        } else {
            webView.resize(w,h);
        }
    }

    private void updateSidebar(String url) {
        String key = url;
        if (key.startsWith("https://docs.oracle.com/javase/8/javafx/api/")) {
            key = key.substring("https://docs.oracle.com/javase/8/javafx/api/".length(), key.lastIndexOf('.'));
            key = key.replaceAll("/", ".");
        } else if (key.startsWith("https://docs.oracle.com/javase/8/docs/api/")) {
            key = key.substring("https://docs.oracle.com/javase/8/docs/api/".length(), key.lastIndexOf('.'));
            key = key.replaceAll("/", ".");
        }
        SampleInfo[] samples = Samples.getSamplesForDoc(key);
        if (samples == null || samples.length == 0) {
            sideBar.getChildren().clear();
            getChildren().remove(scrollPane);
            showSideBar = false;
        } else {
            sideBar.getChildren().setAll(sideBarTitle);
            for (final SampleInfo sample: samples) {
                Button sampleButton = new Button(sample.name);
                sampleButton.setCache(true);
                sampleButton.getStyleClass().setAll("sample-button");
                sampleButton.setGraphic(sample.getMediumPreview());
                sampleButton.setContentDisplay(ContentDisplay.TOP);
                sampleButton.setOnAction((ActionEvent actionEvent) -> {
                    pageBrowser.goToSample(sample);
                });
                sideBar.getChildren().add(sampleButton);
            }
            if (!showSideBar) getChildren().add(scrollPane);
            showSideBar = true;
        }
    }
}
