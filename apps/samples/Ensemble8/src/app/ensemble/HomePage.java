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

package ensemble;

import ensemble.generated.Samples;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Callback;

/**
 * The home page for ensemble.
 */
public class HomePage extends ListView<HomePage.HomePageRow> implements Callback<ListView<HomePage.HomePageRow>, ListCell<HomePage.HomePageRow>>, ChangeListener<Number>, Page{
    private static final Effect RIBBON_EFFECT = BlendBuilder.create()
            .topInput(
                    InnerShadowBuilder.create()
                            .color(Color.rgb(0, 0, 0, 0.75))
                            .offsetX(1)
                            .offsetY(1)
                            .radius(1)
                            .blurType(BlurType.ONE_PASS_BOX)
                            .build())
            .bottomInput(
                    DropShadowBuilder.create()
                            .color(Color.rgb(255, 255, 255, 0.33))
                            .offsetX(1)
                            .offsetY(1)
                            .radius(0)
                            .blurType(BlurType.ONE_PASS_BOX)
                            .build())
            .build();
    private static final int ITEM_WIDTH = 216;
    private static final int ITEM_HEIGHT = 162;
    private static final int ITEM_GAP = 32;
    private static final int MIN_MARGIN = 20;
    private static enum RowType{Highlights,Title,Samples};
    private int numberOfColumns = -1;
    private final HomePageRow HIGHLIGHTS_ROW = new HomePageRow(RowType.Highlights,null,null);
    private final PageBrowser pageBrowser;
    private final ReadOnlyStringProperty titleProperty = new ReadOnlyStringWrapper("JavaFX Ensemble");

    public HomePage(PageBrowser pageBrowser) {
        this.pageBrowser = pageBrowser;
        getStyleClass().clear();
        setId("HomePage");
        // don't use any of the standard ListView CSS
        //   getStyleClass().clear();
       
        // listen for when the list views width changes and recalculate number of columns
        widthProperty().addListener(this);
        // set our custom cell factory
        setCellFactory(this);
    }

    @Override public ReadOnlyStringProperty titleProperty() {
        return titleProperty;
    }

    @Override public String getTitle() {
        return titleProperty.get();
    }

    @Override public String getUrl() {
        return PageBrowser.HOME_URL;
    }

    @Override public Node getNode() {
        return this;
    }

    /* Called when the ListView's width changes */
    @Override public void changed(ObservableValue<? extends Number> observableValue, Number number, Number newWidth) {
        // calculate new number of columns that will fit
        double width = newWidth.doubleValue();
        width -= MIN_MARGIN + MIN_MARGIN;
        width += ITEM_GAP;
        final int newNumOfColumns = Math.max(1, (int) (width / (ITEM_WIDTH + ITEM_GAP)));
        // our size may have changed, so see if we need to rebuild items list
        if (numberOfColumns != newNumOfColumns) {
            numberOfColumns = newNumOfColumns;
            rebuild();
        }
    }

    @Override public ListCell<HomePageRow> call(ListView<HomePageRow> homePageRowListView) {
        return new HomeListCell();
    }
    
    // Called to rebuild the list's items based on the current number of columns
    private void rebuild() {
        // build new list of titles and samples
        List<HomePageRow> newItems = new ArrayList<>();
        // add Highlights to top
        newItems.add(HIGHLIGHTS_ROW);
        // add any samples directly in root category
        addSampleRows(newItems,Samples.ROOT.samples);
        // add samples for all sub categories
        for(SampleCategory category: Samples.ROOT.subCategories) {
            // add title row
            newItems.add(new HomePageRow(RowType.Title,category.name,null));
            // add samples
            addSampleRows(newItems,category.samplesAll);
        }
        // replace the lists items
        getItems().setAll(newItems);
    }

    /**
     * Add samples rows to the items list for all SampleInfo's in samples array
     *
     * @param items The list of rows to add too
     * @param samples The SampleInfo's to create rows for
     */
    private void addSampleRows(List<HomePageRow> items, SampleInfo[] samples) {
        if(samples == null) return;
        for(int row = 0; row < Math.ceil((double) samples.length / numberOfColumns); row++) {
            int sampleIndex = row*numberOfColumns;
            SampleInfo[] sampleInfos = Arrays.copyOfRange(samples,sampleIndex, Math.min(sampleIndex+numberOfColumns,samples.length));
            items.add(new HomePageRow(RowType.Samples, null, sampleInfos));
        }
    }

    private Reference<Pagination> paginationCache;
    private ImageView highlightRibbon;
    private Map<String, SectionRibbon> ribbonsCache = new WeakHashMap<>();
    private Map<SampleInfo, Button> buttonCache = new WeakHashMap<>();

    private static int cellCount = 0;
    
    private class HomeListCell extends ListCell<HomePageRow> implements Callback<Integer,Node>,  Skin<HomeListCell> {
        private static final double HIGHLIGHTS_HEIGHT = 430;
        private static final double RIBBON_HEIGHT = 58;
        private static final double DEFAULT_HEIGHT = 208;
        private double height = DEFAULT_HEIGHT;
        int cellIndex;
        private RowType oldRowType = null;
        private HBox box = new HBox(ITEM_GAP);
        private HomeListCell() {
            super();
            getStyleClass().clear();
            cellIndex = cellCount++;
            box.getStyleClass().add("home-page-cell");
            // we don't need any of the labeled functionality of the default cell skin, so we replace skin with our own
            // in this case using this same class as it saves memory. This skin is very simple its just a HBox container
            setSkin(this);
        }

        @Override
        protected double computeMaxHeight(double d) {
            return height;
        }

        @Override
        protected double computePrefHeight(double d) {
            return height;
        }

        @Override
        protected double computeMinHeight(double d) {
            return height;
        }
        
        // CELL METHODS
        @Override protected void updateItem(HomePageRow item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null) {
                oldRowType = null;
                box.getChildren().clear();
                height = DEFAULT_HEIGHT;
            } else {
                switch (item.rowType) {
                    case Highlights:
                        if (oldRowType != RowType.Highlights) {
                            height = HIGHLIGHTS_HEIGHT;
                            Pagination pagination = paginationCache == null ? null 
                                    : paginationCache.get();
                            if (pagination == null) {
                                pagination = new Pagination(Samples.HIGHLIGHTS.length);
                                pagination.getStyleClass().add(Pagination.STYLE_CLASS_BULLET);
                                pagination.setMaxWidth(USE_PREF_SIZE);
                                pagination.setPageFactory(this);
                                paginationCache = new WeakReference<>(pagination);
                            }
                            if (highlightRibbon == null) {
                                highlightRibbon = new ImageView(new Image(getClass().getResource("images/highlights-ribbon.png").toExternalForm()));
                                highlightRibbon.setManaged(false);
                                highlightRibbon.layoutXProperty().bind(pagination.layoutXProperty().subtract(4));
                                highlightRibbon.layoutYProperty().bind(pagination.layoutYProperty().subtract(4));
                            }
                            box.setAlignment(Pos.CENTER);
                            box.getChildren().setAll(pagination, highlightRibbon);
                        }
                        break;
                    case Title:
                        height = RIBBON_HEIGHT;
                        SectionRibbon ribbon = ribbonsCache.get(item.title);
                        if (ribbon == null) {
                            ribbon = new SectionRibbon(item.title);
                            ribbonsCache.put(item.title, ribbon);
                        }
                        box.getChildren().setAll(ribbon);
                        box.setAlignment(Pos.CENTER_LEFT);
                        break;
                    case Samples:
                        height = DEFAULT_HEIGHT;
                        box.setAlignment(Pos.CENTER);
                        box.getChildren().clear();
                        for (int i = 0; i < item.samples.length; i++) {
                            final SampleInfo sample = item.samples[i];
                            Button sampleButton = buttonCache.get(sample);
                            if (sampleButton == null) {
                                sampleButton = new Button();
                                sampleButton.setCache(true);
                                sampleButton.getStyleClass().setAll("sample-button");
                                sampleButton.setContentDisplay(ContentDisplay.TOP);
                                sampleButton.setText(sample.name);
                                sampleButton.setGraphic(sample.getMediumPreview());
                                sampleButton.setOnAction(new EventHandler<ActionEvent>() {
                                    
                                    @Override public void handle(ActionEvent actionEvent) {
                                        pageBrowser.goToSample(sample);
                                    }
                                });
                                buttonCache.put(sample, sampleButton);
                            }
                            if (sampleButton.getParent() != null) {
                                ((HBox) sampleButton.getParent()).getChildren().remove(sampleButton);
                            }
                            box.getChildren().add(sampleButton);
                        }
                        break;
                }
                oldRowType = item.rowType;
            }
        }

        // SKIN METHODS
        @Override public HomeListCell getSkinnable() { return this; }
        @Override public Node getNode() { return box; }
        @Override public void dispose() {}
        // CALLBACK METHODS
        @Override public Node call(final Integer highlightIndex) {
            Button sampleButton = new Button();
            sampleButton.setCache(true);
            sampleButton.getStyleClass().setAll("sample-button");
            sampleButton.setGraphic(Samples.HIGHLIGHTS[highlightIndex].getLargePreview());
            sampleButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            sampleButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent actionEvent) {
//                    System.out.println("Clicked " + Samples.HIGHLIGHTS[highlightIndex].name);
                    pageBrowser.goToSample(Samples.HIGHLIGHTS[highlightIndex]);
                }
            });
            return sampleButton;
        }
    }


//    private class HomeListCell extends ListCell<HomePageRow> implements ChangeListener<HomePageRow>, Callback<Integer,Node> {
//        private HomeListCell() {
//            super();
//            // don't use any of the standard CSS
//            getStyleClass().setAll("home-page-cell");
//            itemProperty().addListener(this);
////            setSkin(new Label("DUDE"));
//        }
//
//        @Override protected void updateItem(HomePageRow item, boolean empty) {
//            super.updateItem(item, empty);
//        }
//
//        @Override public void changed(ObservableValue<? extends HomePageRow> observableValue, HomePageRow oldRow, HomePageRow newRow) {
//            setText(null);
//            if (newRow == null) {
//                setGraphic(null);
//            } else {
//                switch(newRow.rowType) {
//                    case Highlights:
////                        Pagination pagination = new Pagination(Samples.HIGHLIGHTS.length);
////                        pagination.setMaxWidth(USE_PREF_SIZE);
////                        pagination.setPageFactory(this);
////                        setGraphic(pagination);
//                        setGraphic(Samples.HIGHLIGHTS[0].getLargePreview());
//                        setAlignment(Pos.CENTER);
//                        break;
//                    case Title:
//                        SectionRibbon sectionRibbon = new SectionRibbon(newRow.title+"  public void changed(Obs");
//                        setAlignment(Pos.CENTER_LEFT);
//                        setGraphic(sectionRibbon);
//                        break;
//                    case Samples:
//                        setAlignment(Pos.CENTER);
//                        HBox hbox = new HBox(ITEM_GAP);
//                        setGraphic(hbox);
//                        hbox.setMaxWidth(USE_PREF_SIZE);
//                        hbox.setAlignment(Pos.CENTER);
//                        for (SampleInfo sample:newRow.samples) {
//                            Label sampleLabel = new Label(sample.name);
//                            sampleLabel.getStyleClass().add("sample-label");
//                            sampleLabel.setGraphic(sample.getMediumPreview());
//                            sampleLabel.setContentDisplay(ContentDisplay.TOP);
//                            hbox.getChildren().add(sampleLabel);
//                        }
//                        break;
//                }
//            }
//        }
//
//        @Override public Node call(Integer integer) {
//            return Samples.HIGHLIGHTS[integer].getLargePreview();
//        }
//    }

    public static class HomePageRow {
        public final RowType rowType;
        public final String title;
        public final SampleInfo[] samples;

        private HomePageRow(RowType rowType, String title, SampleInfo[] samples) {
            this.rowType = rowType;
            this.title = title;
            this.samples = samples;
        }

        @Override
        public String toString() {
            return "HomePageRow{" + "rowType=" + rowType + ", title=" + title + ", samples=" + samples + '}';
        }
    }

    private static class SectionRibbon extends Region {
        private Text textNode = new Text();
        public SectionRibbon(String text) {
            textNode.setText(text);
            getStyleClass().add("section-ribbon");
            textNode.setFill(Color.web("#0059a9"));
            textNode.setStyle("-fx-font-family: 'Bree Serif'; -fx-font-size: 16");
            setPrefHeight(38);
            setMaxWidth(USE_PREF_SIZE);
            textNode.setEffect(RIBBON_EFFECT);
            setCache(true);
            getChildren().add(textNode);
        }

        public void setText(String text) {
            textNode.setText(text);
        }

        @Override protected void layoutChildren() {
            textNode.relocate(30, snapPosition((getHeight() - textNode.getBoundsInParent().getHeight()) / 2) - 3);
        }
    }
}
