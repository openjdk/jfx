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
import ensemble.SampleInfo;
import ensemble.SampleInfo.URL;
import ensemble.generated.Samples;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import static ensemble.samplepage.SamplePageContent.title;
import static ensemble.samplepage.SamplePage.INDENT;

/**
 * Description Section on Sample Page
 */
public class Description extends GridPane {
    private static final Image ORANGE_ARROW = new Image(EnsembleApp.class.getResource("images/orange-arrrow.png").toExternalForm());
    private final SamplePage samplePage;
    private final Label description;
    private final VBox relatedDocumentsList;
    private final VBox relatedSamples;
    
    public Description(final SamplePage samplePage) {
        setVgap(INDENT);
        setHgap(INDENT);
        this.samplePage = samplePage;
        getStyleClass().add("sample-page-box");

        // Setup Columns
        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(50);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(50);
        getColumnConstraints().addAll(leftColumn,rightColumn);

        // Add Description
        Text descriptionTitle = title("DESCRIPTION");
        setConstraints(descriptionTitle,0,0,2,1);
        description = new Label();
        description.setWrapText(true);
        description.setMinHeight(Label.USE_PREF_SIZE);
        description.setPadding(new Insets(8,0,8,0));
        setConstraints(description, 0, 1, 2, 1);

        // Add View Source Hyperlink
        Hyperlink sourceBtn = new Hyperlink("VIEW SOURCE");
        setConstraints(sourceBtn,0,2,2,1);
        sourceBtn.getStyleClass().add("sample-page-box-title");
        sourceBtn.setGraphic(new ImageView(ORANGE_ARROW));
        sourceBtn.setContentDisplay(ContentDisplay.RIGHT);
        sourceBtn.setOnAction(event -> {
            samplePage.pageBrowser.goToPage(samplePage.getUrl().replaceFirst("sample://", "sample-src://"));
        });

        // Add Related Documents
        Text relatedDocumentsTitle = title("RELATED DOCUMENTS");
        setConstraints(relatedDocumentsTitle,0,3);
        relatedDocumentsList = new VBox();
        ScrollPane relatedDocumentsScrollPane = new ScrollPane(relatedDocumentsList);
        setConstraints(relatedDocumentsScrollPane,0,4);
        relatedDocumentsScrollPane.setFitToHeight(true);
        relatedDocumentsScrollPane.setFitToWidth(true);
        relatedDocumentsScrollPane.prefHeightProperty().bind(heightProperty());
        relatedDocumentsScrollPane.getStyleClass().clear();

        // Add Related Samples
        Text relatedSamplesTitle = title("RELATED SAMPLES");
        setConstraints(relatedSamplesTitle,1,3);
        relatedSamples = new VBox();
        setConstraints(relatedSamples,1,4);

        getChildren().addAll(
                descriptionTitle,
                description,
                relatedDocumentsTitle,
                relatedDocumentsScrollPane,
                relatedSamplesTitle,
                relatedSamples
                );
        if (!EnsembleApp.IS_EMBEDDED) getChildren().add(sourceBtn);

        // listen for when sample changes
        samplePage.registerSampleInfoUpdater(sampleInfo -> {
            update(sampleInfo);
            return null;
        });
    }

    private void update(SampleInfo sampleInfo) {
        relatedDocumentsList.getChildren().clear();
        for (final URL docUrl : sampleInfo.getDocURLs()) {
            Hyperlink link = new Hyperlink(docUrl.getName());
            link.setOnAction(t -> {
                samplePage.pageBrowser.goToPage(docUrl.getURL());
            });
            link.setTooltip(new Tooltip(docUrl.getName()));
            relatedDocumentsList.getChildren().add(link);
        }
        for (final String classpath : sampleInfo.apiClasspaths) {
            Hyperlink link = new Hyperlink(classpath.replace('$', '.'));
            link.setOnAction(t -> {
                samplePage.pageBrowser.goToPage(samplePage.apiClassToUrl(classpath));
            });
            relatedDocumentsList.getChildren().add(link);
        }
        relatedSamples.getChildren().clear();
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
