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
package ensemble;

import ensemble.control.Popover;
import ensemble.control.PopoverTreeList;
import ensemble.search.DocumentType;
import ensemble.search.SearchResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Popover page that displays a list of search results.
 */
public class SearchResultPopoverList extends PopoverTreeList<SearchResult> implements Popover.Page {
    private Popover popover;
    private PageBrowser pageBrowser;
    private Rectangle leftLine = new Rectangle(0,0,1,1);
    private IconPane iconPane = new IconPane();
    private final Pane backgroundRectangle = new Pane();

    public SearchResultPopoverList(PageBrowser pageBrowser) {
        this.pageBrowser = pageBrowser;
        leftLine.setFill(Color.web("#dfdfdf"));
        iconPane.setManaged(false);
        setFocusTraversable(false);
        backgroundRectangle.setId("PopoverBackground");
        setPlaceholder(backgroundRectangle);
    }

    @Override protected void layoutChildren() {
        super.layoutChildren();
        if (leftLine.getParent() != this) getChildren().add(leftLine);
        if (iconPane.getParent() != this) getChildren().add(iconPane);
        leftLine.setLayoutX(40);
        leftLine.setLayoutY(0);
        leftLine.setHeight(getHeight());
        iconPane.setLayoutX(0);
        iconPane.setLayoutY(0);
        iconPane.resize(getWidth(), getHeight());
        backgroundRectangle.resize(getWidth(), getHeight());
    }

    @Override public void itemClicked(SearchResult item) {
        popover.hide();
        pageBrowser.goToPage(item.getEnsemblePath());
    }

    @Override public void setPopover(Popover popover) {
        this.popover = popover;
    }

    @Override public Popover getPopover() {
        return popover;
    }

    @Override public Node getPageNode() {
        return this;
    }

    @Override public String getPageTitle() {
        return "Results";
    }

    @Override public String leftButtonText() {
        return null;
    }

    @Override public void handleLeftButton() {}

    @Override public String rightButtonText() {
        return null;
    }

    @Override public void handleRightButton() {
    }

    @Override public void handleShown() { }
    @Override public void handleHidden() { }


    @Override public ListCell<SearchResult> call(ListView<SearchResult> p) {
        return new SearchResultListCell();
    }

    private class IconPane extends Pane {
        private Region samplesIcon = new Region();
        private Label classesIcon = new Label("C");
        private Label propertiesIcon = new Label("P");
        private Label methodsIcon = new Label("M");
        private Label fieldsIcon = new Label("F");
        private Label enumsIcon = new Label("E");
        private Region documentationIcon = new Region();
        private List<SearchResultListCell> allCells = new ArrayList<>();
        private Rectangle classesLine = new Rectangle(0,0,40,1);
        private Rectangle propertiesLine = new Rectangle(0,0,40,1);
        private Rectangle methodsLine = new Rectangle(0,0,40,1);
        private Rectangle fieldsLine = new Rectangle(0,0,40,1);
        private Rectangle enumsLine = new Rectangle(0,0,40,1);
        private Rectangle documentationLine = new Rectangle(0,0,40,1);

        public IconPane() {
            getStyleClass().add("search-icon-pane");
            samplesIcon.getStyleClass().add("samples-icon");
            documentationIcon.getStyleClass().add("documentation-icon");
            classesLine.setFill(Color.web("#dfdfdf"));
            propertiesLine.setFill(Color.web("#dfdfdf"));
            methodsLine.setFill(Color.web("#dfdfdf"));
            fieldsLine.setFill(Color.web("#dfdfdf"));
            enumsLine.setFill(Color.web("#dfdfdf"));
            documentationLine.setFill(Color.web("#dfdfdf"));
            getChildren().addAll(samplesIcon,classesIcon,propertiesIcon,methodsIcon,fieldsIcon,enumsIcon,documentationIcon,
                                classesLine,propertiesLine,methodsLine,fieldsLine,enumsLine,documentationLine);
            setMouseTransparent(true);
        }

        private Node getIconForDocType(DocumentType docType) {
            switch(docType) {
                case SAMPLE:
                    return samplesIcon;
                case CLASS:
                    return classesIcon;
                case FIELD:
                    return fieldsIcon;
                case METHOD:
                    return methodsIcon;
                case PROPERTY:
                    return propertiesIcon;
                case ENUM:
                    return enumsIcon;
                case DOC:
                    return documentationIcon;
            }
            return null;
        }

        private Rectangle getLineForDocType(DocumentType docType) {
            switch(docType) {
                case CLASS:
                    return classesLine;
                case FIELD:
                    return fieldsLine;
                case METHOD:
                    return methodsLine;
                case PROPERTY:
                    return propertiesLine;
                case ENUM:
                    return enumsLine;
                case DOC:
                    return documentationLine;
            }
            return null;
        }

        @Override protected void layoutChildren() {
            List<SearchResultListCell> visibleCells = new ArrayList<>(20);
            for (SearchResultListCell cell: allCells) {
                if (cell.isVisible()) visibleCells.add(cell);
            }
            Collections.sort(visibleCells, (Node o1, Node o2) -> Double.compare(o1.getLayoutY(), o2.getLayoutY()));

            samplesIcon.setLayoutX(8);
            samplesIcon.resize(24, 24);
            classesIcon.setLayoutX(8);
            classesIcon.resize(24, 24);
            propertiesIcon.setLayoutX(8);
            propertiesIcon.resize(24, 24);
            methodsIcon.setLayoutX(8);
            methodsIcon.resize(24, 24);
            fieldsIcon.setLayoutX(8);
            fieldsIcon.resize(24, 24);
            enumsIcon.setLayoutX(8);
            enumsIcon.resize(24, 24);
            documentationIcon.setLayoutX(8);
            documentationIcon.resize(24, 24);


            samplesIcon.setVisible(false);
            classesIcon.setVisible(false);
            propertiesIcon.setVisible(false);
            methodsIcon.setVisible(false);
            fieldsIcon.setVisible(false);
            enumsIcon.setVisible(false);
            documentationIcon.setVisible(false);
            classesLine.setVisible(false);
            propertiesLine.setVisible(false);
            methodsLine.setVisible(false);
            fieldsLine.setVisible(false);
            enumsLine.setVisible(false);
            documentationLine.setVisible(false);

            final int last = visibleCells.size()-1;
            DocumentType lastDocType = null;
            for(int index = 0; index <= last; index ++) {
                SearchResultListCell cell = visibleCells.get(index);
                DocumentType docType = getDocumentTypeForCell(cell);
                if (lastDocType != docType && docType != null) {
                    // this is first of this doc type
                    Node icon = getIconForDocType(docType);
                    icon.setVisible(true);
                    // calculate cell position relative to iconPane
                    Point2D cell00 = cell.localToScene(0, 0);
                    cell00 = sceneToLocal(cell00);
                    // check if next is differnt
                    if (index != last && getDocumentTypeForCell(visibleCells.get(index+1)) != docType) {
                        icon.setLayoutY(cell00.getY()+8);
                    } else {
                        icon.setLayoutY(Math.max(8,cell00.getY()+8));
                    }
                    // update line
                    Rectangle line = getLineForDocType(docType);
                    if (line != null) {
                        line.setVisible(true);
                        line.setLayoutY(cell00.getY());
                    }
                }
                lastDocType = docType;
            }

        }

        private final DocumentType getDocumentTypeForCell(SearchResultListCell cell) {
            SearchResult searchResult = cell.getItem();
            return searchResult == null ? null : searchResult.getDocumentType();
        }
    }


    private class SearchResultListCell extends ListCell<SearchResult> implements Skin<SearchResultListCell>, EventHandler {
        private static final int TEXT_GAP = 6;
        private ImageView arrow = new ImageView(RIGHT_ARROW);
        private Label title = new Label();
        private Label details = new Label();
        private int cellIndex;
        private Rectangle topLine = new Rectangle(0,0,1,1);

        private SearchResultListCell() {
            super();
            //System.out.println("CREATED TimeSlot CELL " + (cellIndex));
            // we don't need any of the labeled functionality of the default cell skin, so we replace skin with our own
            // in this case using this same class as it saves memory. This skin is very simple its just a HBox container
            setSkin(this);
            getStyleClass().setAll("search-result-cell");
            title.getStyleClass().setAll("title");
            details.getStyleClass().setAll("details");
            topLine.setFill(Color.web("#dfdfdf"));
            getChildren().addAll(arrow,title,details,topLine);
            setOnMouseClicked(this);

            // listen to changes of this cell being added and removed from list
            // and when it or its parent is moved. If any of those things change
            // then update the iconPane's layout. requestLayout() will be called
            // many times for any change of cell layout in the list but that
            // dosn't matter as they will all be batched up by layout machanisim
            // and iconPane.layoutChildren() will only be called once per frame.
            final ChangeListener<Bounds> boundsChangeListener = (ObservableValue<? extends Bounds> ov, Bounds t, Bounds t1) -> {
                iconPane.requestLayout();
            };
            parentProperty().addListener((ObservableValue<? extends Parent> ov, Parent oldParent, Parent newParent) -> {
                if(oldParent != null) {
                    oldParent.layoutBoundsProperty().removeListener(boundsChangeListener);
                }
                if (newParent != null && newParent.isVisible()) {
                    iconPane.allCells.add(SearchResultListCell.this);
                    newParent.layoutBoundsProperty().addListener(boundsChangeListener);
                } else {
                    iconPane.allCells.remove(SearchResultListCell.this);
                }
                iconPane.requestLayout();
            });
        }

        @Override protected double computeMinWidth(double height) {
            final Insets insets = getInsets();
            final double h = height = insets.getBottom() - insets.getTop();
            return (int)((insets.getLeft() + title.minWidth(h) + TEXT_GAP + details.minWidth(h) + insets.getRight())+ 0.5d);
        }

        @Override protected double computePrefWidth(double height) {
            final Insets insets = getInsets();
            final double h = height = insets.getBottom() - insets.getTop();
            return (int)((insets.getLeft() + title.prefWidth(h) + TEXT_GAP + details.prefWidth(h) + insets.getRight())+ 0.5d);
        }

        @Override protected double computeMaxWidth(double height) {
            final Insets insets = getInsets();
            final double h = height = insets.getBottom() - insets.getTop();
            return (int)((insets.getLeft() + title.maxWidth(h) + TEXT_GAP + details.maxWidth(h) + insets.getRight())+ 0.5d);
        }

        @Override protected double computeMinHeight(double width) {
            final Insets insets = getInsets();
            final double w = width - insets.getLeft() - insets.getRight();
            return (int)((insets.getTop() + title.minHeight(w) + TEXT_GAP + details.minHeight(w) + insets.getBottom())+ 0.5d);
        }

        @Override protected double computePrefHeight(double width) {
            final Insets insets = getInsets();
            final double w = width - insets.getLeft() - insets.getRight();
            return (int)((insets.getTop() + title.prefHeight(w) + TEXT_GAP + details.prefHeight(w) + insets.getBottom())+ 0.5d);
        }

        @Override protected double computeMaxHeight(double width) {
            final Insets insets = getInsets();
            final double w = width - insets.getLeft() - insets.getRight();
            return (int)((insets.getTop() + title.maxHeight(w) + TEXT_GAP + details.maxHeight(w) + insets.getBottom())+ 0.5d);
        }

        @Override protected void layoutChildren() {
            final Insets insets = getInsets();
            final double left = insets.getLeft();
            final double top = insets.getTop();
            final double w = getWidth() - left - insets.getRight();
            final double h = getHeight() - top - insets.getBottom();
            final double titleHeight = title.prefHeight(w);
            title.setLayoutX(left);
            title.setLayoutY(top);
            title.resize(w, titleHeight);
            final double detailsHeight = details.prefHeight(w);
            details.setLayoutX(left);
            details.setLayoutY(top + titleHeight + TEXT_GAP);
            details.resize(w, detailsHeight);
            final Bounds arrowBounds = arrow.getLayoutBounds();
            arrow.setLayoutX(getWidth() - arrowBounds.getWidth() - 12);
            arrow.setLayoutY((int)((getHeight() - arrowBounds.getHeight())/2d));
            topLine.setLayoutX(left-5);
            topLine.setWidth(getWidth()-left+5);
        }

        // CELL METHODS
        @Override protected void updateItem(SearchResult result, boolean empty) {
            super.updateItem(result,empty);
            if (result == null) { // empty item
                arrow.setVisible(false);
                title.setVisible(false);
                details.setVisible(false);
            } else {
                arrow.setVisible(true);
                title.setVisible(true);
                details.setVisible(true);
                title.setText(result.getName());
                details.setText(result.getShortDescription());
            }
        }

        // SKIN METHODS
        @Override public SearchResultListCell getSkinnable() { return this; }
        @Override public Node getNode() { return null; }
        @Override public void dispose() {}

        @Override public void handle(Event t) {
            itemClicked(getItem());
        }
    }
}