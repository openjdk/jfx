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
package ensemble.samplepage;

import ensemble.Page;
import ensemble.PageBrowser;
import ensemble.SampleInfo;
import static ensemble.SampleInfo.SampleRuntimeInfo;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

/**
 * Page for showing a sample
 */
public class SamplePage extends StackPane implements Page {
    static final double INDENT = 8;
    final ObjectProperty<SampleInfo> sampleInfoProperty = new SimpleObjectProperty<>();
    private final StringProperty titleProperty = new SimpleStringProperty();
    PageBrowser pageBrowser;
    final ObjectProperty<SampleRuntimeInfo> sampleRuntimeInfoProperty = new SimpleObjectProperty<>();

    public SamplePage(SampleInfo sampleInfo, String url, final PageBrowser pageBrowser) {
        sampleInfoProperty.set(sampleInfo);
        this.pageBrowser = pageBrowser;
        getStyleClass().add("sample-page");
        titleProperty.bind(new StringBinding() {
            { bind(sampleInfoProperty); }
            @Override protected String computeValue() {
                SampleInfo sample = SamplePage.this.sampleInfoProperty.get();
                if (sample != null) {
                    return sample.name;
                } else {
                    return null;
                }
            }
        });
        sampleRuntimeInfoProperty.bind(new ObjectBinding<SampleRuntimeInfo>() {
            { bind(sampleInfoProperty); }
            @Override protected SampleRuntimeInfo computeValue() {
                return sampleInfoProperty.get().buildSampleNode();
            }
        });

        SamplePageContent frontPage = new SamplePageContent(this);
        getChildren().setAll(frontPage);
    }

    public void update(SampleInfo sampleInfo, String url) {
        sampleInfoProperty.set(sampleInfo);
    }

    @Override public ReadOnlyStringProperty titleProperty() {
        return titleProperty;
    }

    @Override public String getTitle() {
        return titleProperty.get();
    }

    @Override public String getUrl() {
        return "sample://" + sampleInfoProperty.get().ensemblePath;
    }

    @Override public Node getNode() {
        return this;
    }

    String apiClassToUrl(String classname) {
        String urlEnd = classname.replaceAll("\\.([a-z])", "/$1").replaceFirst("\\.([A-Z])", "/$1");
        if (classname.startsWith("javafx")) {
            return "https://docs.oracle.com/javase/8/javafx/api/"+urlEnd+".html";
        } else {
            return "https://docs.oracle.com/javase/8/docs/api/"+urlEnd+".html";
        }
    }

    /**
     * This method is equivalent to bind(ObjectBinding) as it would invoke
     * updater immediately as well as on any change to SampleInfo
     * @param updater a method that updates content for a given SampleInfo
     */
    void registerSampleInfoUpdater(final Callback<SampleInfo, Void> updater) {
        sampleInfoProperty.addListener((ObservableValue<? extends SampleInfo> ov, SampleInfo t, SampleInfo sampleInfo) -> {
            updater.call(sampleInfo);
        });
        updater.call(sampleInfoProperty.get());
    }
}
