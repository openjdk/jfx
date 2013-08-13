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

import ensemble.EnsembleApp;
import ensemble.SampleInfo.URL;
import ensemble.util.Utils;
import javafx.scene.control.ScrollPaneBuilder;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextAreaBuilder;
import javafx.scene.control.TooltipBuilder;
import javafx.scene.image.Image;
import javafx.scene.image.ImageViewBuilder;
import javafx.scene.layout.StackPaneBuilder;
import javafx.scene.web.WebView;

/**
 *
 */
class SourceTab extends Tab {
    private URL sourceURL;
    private final SamplePage samplePage;

    public SourceTab(URL sourceURL, final SamplePage samplePage) {
        super(sourceURL.getName());
        this.samplePage = samplePage;
        this.sourceURL = sourceURL;
        String url = sourceURL.getURL();
        String ext = url.substring(url.lastIndexOf('.')).toLowerCase();
        switch (ext) {
            case ".java":
            case ".css":
            case ".fxml":
                String source = Utils.loadFile(getClass().getResource(url));
                if (EnsembleApp.IS_EMBEDDED || EnsembleApp.IS_IOS) {
                    // TODO: Convert to TextFlow
                    //                    TextFlow textFlow = TextFlowBuilder.create()
                    //                            .build();
                    //
                    //                    Reader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(url)));
                    TextArea textArea = TextAreaBuilder.create().text(source).style("-fx-font-family: 'Courier New';").build();
                    setContent(textArea);
                } else {
                    WebView webView = new WebView();
                    webView.getEngine().loadContent(samplePage.convertToHTML(source));
                    setContent(webView);
                }
                break;
            case ".jpg":
            case ".png":
                setContent(ScrollPaneBuilder.create().fitToHeight(true).fitToWidth(true).content(StackPaneBuilder.create().children(ImageViewBuilder.create().image(new Image(url)).build()).build()).build());
                break;
        }
        setTooltip(TooltipBuilder.create().text(url).build());
    }
    
}
