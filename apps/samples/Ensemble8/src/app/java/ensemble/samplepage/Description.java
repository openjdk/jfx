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


import ensemble.SampleInfo;
import ensemble.SampleInfo.URL;
import ensemble.generated.Samples;
import static ensemble.samplepage.FrontPage.*;
import static ensemble.samplepage.SamplePage.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPaneBuilder;
import javafx.scene.layout.ColumnConstraintsBuilder;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.GridPaneBuilder;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.util.Callback;


/**
 *
 */
public class Description extends VBox {
    
    private final SamplePage samplePage;
    private final Label description;
    private final VBox relatedDocumentsList;
    private final VBox relatedSamples;
    
    public Description(final SamplePage samplePage) {
        super(INDENT);
        this.samplePage = samplePage;
        getStyleClass().add("sample-page-box");

        relatedDocumentsList = new VBox();
        ScrollPane relatedDocumentsScrollPane = ScrollPaneBuilder.create()
                .content(relatedDocumentsList)
                .fitToHeight(true)
                .fitToWidth(true)
                .pannable(true)
                .build();
        relatedDocumentsScrollPane.prefHeightProperty().bind(heightProperty());
        relatedDocumentsScrollPane.getStyleClass().clear();
        
        VBox relatedDocuments = VBoxBuilder.create()
                .children(
                    title("RELATED DOCUMENTS"), 
                    relatedDocumentsScrollPane
                )
                .build();
        relatedSamples = VBoxBuilder.create()
                .children(title("RELATED SAMPLES"))
                .build();
        
        GridPane gridPane = GridPaneBuilder.create()
                .columnConstraints(
                    ColumnConstraintsBuilder.create()
                        .percentWidth(50)
                        .build(), 
                    ColumnConstraintsBuilder.create()
                        .percentWidth(50)
                        .build()
                )
                .build();
        gridPane.addRow(0, relatedDocuments, relatedSamples);
        
        description = LabelBuilder.create()
                .wrapText(true)
                .minHeight(Label.USE_PREF_SIZE)
                .build();

        samplePage.registerSampleInfoUpdater(new Callback<SampleInfo, Void>() {

            @Override
            public Void call(SampleInfo sampleInfo) {
                update(sampleInfo);
                return null;
            }
        });

        getChildren().addAll(
                title("DESCRIPTION"), description, 
                gridPane);
    }

    private void update(SampleInfo sampleInfo) {
        relatedDocumentsList.getChildren().clear();
        for (final URL docUrl : sampleInfo.getDocURLs()) {
            Hyperlink link = new Hyperlink(docUrl.getName());
            link.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    samplePage.pageBrowser.goToPage(docUrl.getURL());
                }
            });
            relatedDocumentsList.getChildren().add(link);
        }
        for (final String classpath : sampleInfo.apiClasspaths) {
            Hyperlink link = new Hyperlink(classpath.replace('$', '.'));
            link.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    samplePage.pageBrowser.goToPage(samplePage.apiClassToUrl(classpath));
                }
            });
            relatedDocumentsList.getChildren().add(link);
        }
        relatedSamples.getChildren().setAll(relatedSamples.getChildren().get(0));
        for (final SampleInfo.URL sampleURL : sampleInfo.getRelatedSampleURLs()) {
            if (Samples.ROOT.sampleForPath(sampleURL.getURL()) != null) { //Check if sample exists
                Hyperlink sampleLink = new Hyperlink(sampleURL.getName());
                sampleLink.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent t) {
                        samplePage.pageBrowser.goToPage("sample://" + sampleURL.getURL());
                    }
                });
                sampleLink.setPrefWidth(1000);
                relatedSamples.getChildren().add(sampleLink);
            }
        }
        description.setText(sampleInfo.description);
    }

}
