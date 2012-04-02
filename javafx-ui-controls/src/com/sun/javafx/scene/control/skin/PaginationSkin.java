/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.javafx.scene.control.skin;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.control.Pagination;
import com.sun.javafx.scene.control.PaginationCell;
import com.sun.javafx.scene.control.WeakListChangeListener;
import com.sun.javafx.scene.control.behavior.PaginationBehavior;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

public class PaginationSkin<T> extends SkinBase<Pagination<T>, PaginationBehavior<T>>  {

    private Pagination<T> pagination;
    private PaginationListView<T> paginationListView;
    private ObservableList dummyListViewItems;

    private Rectangle clipRect;

    private NavigationControl navigation;
    private int fromIndex;
    private int previousIndex;
    private int currentIndex;
    private int toIndex;
    private int numberOfPages;
    private int numberOfVisiblePages;

    public PaginationSkin(final Pagination<T> pagination) {
        super(pagination, new PaginationBehavior(pagination));

        setManaged(false);
        clipRect = new Rectangle();
        //setClip(clipRect);

        this.pagination = pagination;
        this.paginationListView = new PaginationListView<T>();
        updateListViewItems();
        updateCellFactory();
        resetIndexes(true);

        this.navigation = new NavigationControl();

        getChildren().addAll(paginationListView, navigation);

        pagination.numberOfVisiblePagesProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable o) {
                resetIndexes(false);
                navigation.initializePageIndicators();
                navigation.updatePageIndicators();
            }
        });

        registerChangeListener(pagination.itemsPerPageProperty(), "ITEMS_PER_PAGE");
        registerChangeListener(pagination.numberOfItemsProperty(), "NUMBER_OF_ITEMS");
        registerChangeListener(pagination.pageIndexProperty(), "PAGE_INDEX");
        registerChangeListener(pagination.pageFactoryProperty(), "PAGE_FACTORY");
    }

    private void resetIndexes(boolean usePageIndex) {
        numberOfVisiblePages = getSkinnable().getNumberOfVisiblePages();
        numberOfPages = totalNumberOfPages();
        if (totalNumberOfPages() > numberOfVisiblePages) {
            numberOfPages = numberOfVisiblePages;
        } else {
            // If the number of pages is less than the visible number of pages
            // we want to set it to the new value.
            getSkinnable().setNumberOfVisiblePages(numberOfPages);
        }

        fromIndex = 0;
        previousIndex = 0;
        currentIndex = usePageIndex ? getSkinnable().getPageIndex() : 0;
        toIndex = fromIndex + (numberOfPages - 1);

        paginationListView.reset();
        paginationListView.getSelectionModel().select(currentIndex);
    }

    private void updateListViewItems() {
        if (dummyListViewItems == null) {
            dummyListViewItems = FXCollections.observableArrayList();
        } else {
            dummyListViewItems.clear();
            dummyListViewItems.removeListener(weakListViewItemsListener);
        }

        for (int i = 0; i < totalNumberOfPages(); i++) {
            dummyListViewItems.add(i);
        }
        paginationListView.setItems(dummyListViewItems);

        if (dummyListViewItems != null) {
            dummyListViewItems.addListener(weakListViewItemsListener);
        }
        requestLayout();
    }

    private final ListChangeListener listViewItemsListener = new ListChangeListener() {
        @Override public void onChanged(ListChangeListener.Change c) {
            requestLayout();
        }
    };

    private final WeakListChangeListener weakListViewItemsListener =
            new WeakListChangeListener(listViewItemsListener);

    private void updateCellFactory() {
        Callback<ListView<T>, ListCell<T>> cell = createCellFactory();
        paginationListView.setCellFactory(cell);
    }

    private Callback<ListView<T>, ListCell<T>> createCellFactory() {
        Callback<ListView<T>, ListCell<T>> cell = new Callback<ListView<T>, ListCell<T>>() {
            @Override public ListCell<T> call(ListView<T> listView) {
                return new PaginationCell<T>() {
                    @Override public void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {                            
                            setText(null);
                            setGraphic(pagination.getPageFactory().call(getIndex()));
                        }
                    }
                };
            }
        };
        return cell;
    }

    private int totalNumberOfPages() {
        int totalNumberOfPages = getSkinnable().getNumberOfItems()/getSkinnable().getItemsPerPage();
        if (getSkinnable().getNumberOfItems()%getSkinnable().getItemsPerPage() != 0) {
            // Add the remaining to the next page.
            totalNumberOfPages += 1;
        }
        return totalNumberOfPages;
    }

    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if (p == "ITEMS_PER_PAGE") {
            updateListViewItems();
            resetIndexes(false);
            navigation.initializePageIndicators();
            navigation.updatePageIndicators();
        } else if (p == "NUMBER_OF_ITEMS") {
            updateListViewItems();
            updateCellFactory();
            resetIndexes(false);
            navigation.initializePageIndicators();
            navigation.updatePageIndicators();
        } else if (p == "PAGE_INDEX") {
            paginationListView.getSelectionModel().select(getSkinnable().getPageIndex());
        } else if (p == "PAGE_FACTORY") {
            updateCellFactory();
            resetIndexes(false);
        }
        requestLayout();
    }

    @Override protected void setWidth(double value) {
        super.setWidth(value);
        clipRect.setWidth(value);
    }

    @Override protected void setHeight(double value) {
        super.setHeight(value);
        clipRect.setHeight(value);
    }

    @Override protected double computePrefWidth(double height) {
        double left = snapSpace(getInsets().getLeft());
        double right = snapSpace(getInsets().getRight());
        double navigationWidth = snapSize(navigation.prefWidth(height));
        return left + Math.max(paginationListView.prefWidth(height), navigationWidth) + right;
    }

    @Override protected double computePrefHeight(double width) {
        double top = snapSpace(getInsets().getTop());
        double bottom = snapSpace(getInsets().getBottom());
        double navigationHeight = snapSize(navigation.prefHeight(width));
        return top + paginationListView.prefHeight(width) + navigationHeight + bottom;
    }

    @Override protected void layoutChildren() {
        double left = snapSpace(getInsets().getLeft());
        double right = snapSpace(getInsets().getRight());
        double top = snapSpace(getInsets().getTop());
        double bottom = snapSpace(getInsets().getBottom());
        double width = getWidth() - (left + right);
        double height = getHeight() - (top + bottom);
        double navigationWidth = navigation.prefWidth(-1);
        double navigationHeight = navigation.prefHeight(-1);

        HPos hpos = navigation.getAlignment().getHpos();
        VPos vpos = navigation.getAlignment().getVpos();
        double x = left + Utils.computeXOffset(width, navigationWidth, hpos);
        double y = top + Utils.computeYOffset(height, navigationHeight, vpos);

        layoutInArea(paginationListView, left, top, width, height - navigationHeight, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(navigation, x, y, navigationWidth, navigationHeight, 0, hpos, vpos);
    }

    class NavigationControl extends StackPane {

        private StackPane leftArrowButton;
        private StackPane rightArrowButton;
        private List<IndicatorButton> indicatorButton;

        public NavigationControl() {
            getStyleClass().setAll("pagination-control");
            StackPane leftArrow = new StackPane();
            leftArrow.getStyleClass().add("left-arrow");
            leftArrowButton = new StackPane();
            leftArrowButton.getStyleClass().add("page-navigation");
            leftArrowButton.getChildren().setAll(leftArrow);

            StackPane rightArrow = new StackPane();
            rightArrow.getStyleClass().add("right-arrow");
            rightArrowButton = new StackPane();
            rightArrowButton.getStyleClass().add("page-navigation");
            rightArrowButton.getChildren().setAll(rightArrow);

            indicatorButton = new ArrayList<IndicatorButton>();

            getChildren().addAll(leftArrowButton, rightArrowButton);
            initializeNavigationHandlers();
            initializePageIndicators();
            updatePageIndex();
        }

        private void initializeNavigationHandlers() {
            leftArrowButton.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent arg0) {
                    paginationListView.getSelectionModel().selectPrevious();
                    //System.out.println("LEFT BUTTON " + paginationListView.getSelectionModel().getSelectedIndex());
                    requestLayout();
                }
            });

            rightArrowButton.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent arg0) {
                    paginationListView.getSelectionModel().selectNext();
                    //System.out.println("RIGHT BUTTON " + paginationListView.getSelectionModel().getSelectedIndex() + " TNP " + (totalNumberOfPages - 1));
                    requestLayout();
                }
            });

            paginationListView.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
                    previousIndex = arg1.intValue();
                    currentIndex = arg2.intValue();
                    updatePageIndex();
                }
            });
        }

        private void initializePageIndicators() {
            if (!indicatorButton.isEmpty()) {
                getChildren().removeAll(indicatorButton);
                indicatorButton.clear();
            }

            for (int i = fromIndex; i <= toIndex; i++) {
                indicatorButton.add(new IndicatorButton(i));
            }
            getChildren().addAll(indicatorButton);
        }

        private void updatePageIndicators() {
            for (int i = 0; i < indicatorButton.size(); i++) {
                if (indicatorButton.get(i).getPageNumber() == previousIndex) {
                    indicatorButton.get(i).setSelected(false);
                }
                if (indicatorButton.get(i).getPageNumber() == currentIndex) {
                    indicatorButton.get(i).setSelected(true);
                }
            }
        }

        private void updatePageIndex() {
            //System.out.println("SELECT PROPERTY FROM " + fromIndex + " TO " + toIndex + " PREVIOUS " + previousIndex + " CURRENT "+ currentIndex + " NOP " + numberOfPages + " NOVP " + numberOfVisiblePages);
            if (currentIndex >= 0 && currentIndex <= (totalNumberOfPages() - 1)) {
                if (numberOfPages == numberOfVisiblePages) {
                    if (scroll()) {
                        initializePageIndicators();
                    }
                }
                // Update the current page index
                pagination.setPageIndex(currentIndex);
                updatePageIndicators();
                requestLayout();
            }
        }

        // Only scroll to the next set when the current index is at the start or the end of the set.
        // Return true only if we have scrolled to the next/previous set.
        private boolean scroll() {
            if (previousIndex < currentIndex && currentIndex % numberOfVisiblePages == 0) {
                // Scroll to the right
                fromIndex = currentIndex;
                toIndex = fromIndex + (numberOfVisiblePages - 1);
            } else if (currentIndex < previousIndex && currentIndex % numberOfVisiblePages == numberOfVisiblePages - 1) {
                // Scroll to the left
                toIndex = currentIndex;
                fromIndex = toIndex - (numberOfVisiblePages - 1);
            } else {
                // We need to scroll if the currentIndex is out of range.
                if (currentIndex < fromIndex || currentIndex > toIndex) {
                    fromIndex = currentIndex - (currentIndex % numberOfVisiblePages);
                    toIndex = fromIndex + (numberOfVisiblePages - 1);
                } else {
                    return false;
                }
            }

            // We have gone past the total number of pages
            if (toIndex > totalNumberOfPages() - 1) {
                toIndex = totalNumberOfPages() - 1;
            }

            // We have gone past the starting page
            if (fromIndex < 0) {
                fromIndex = 0;
                toIndex = fromIndex + (numberOfVisiblePages - 1);
            }
            return true;
        }

        @Override protected double computeMinWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }

        @Override protected double computePrefWidth(double height) {
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());
            double leftArrowWidth = snapSize(leftArrowButton.prefWidth(-1));
            double rightArrowWidth = snapSize(rightArrowButton.prefWidth(-1));
            double indicatorWidth = snapSize(indicatorButton.get(0).prefWidth(-1));

            indicatorWidth *= indicatorButton.size();

            return left + leftArrowWidth + indicatorWidth + rightArrowWidth + right;
        }

        @Override protected double computePrefHeight(double width) {
            double top = snapSpace(getInsets().getTop());
            double bottom = snapSpace(getInsets().getBottom());
            double leftArrowHeight = snapSize(leftArrowButton.prefWidth(-1));
            double rightArrowHeight = snapSize(rightArrowButton.prefWidth(-1));
            double indicatorHeight = snapSize(indicatorButton.get(0).prefHeight(-1));

            return top + Math.max(leftArrowHeight, Math.max(rightArrowHeight, indicatorHeight)) + bottom;
        }

        @Override protected double computeMaxWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMaxHeight(double width) {
            return computePrefHeight(width);
        }

        @Override protected void layoutChildren() {
            double top = snapSpace(getInsets().getTop());
            double bottom = snapSpace(getInsets().getBottom());
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());
            double width = snapSize(getWidth()) - (left + right);
            double height = snapSize(getHeight()) - (top + bottom);
            double leftArrowWidth = snapSize(leftArrowButton.prefWidth(-1));
            double leftArrowHeight = snapSize(leftArrowButton.prefHeight(-1));
            double rightArrowWidth = snapSize(rightArrowButton.prefWidth(-1));
            double rightArrowHeight = snapSize(rightArrowButton.prefHeight(-1));
            double indicatorWidth = snapSize(indicatorButton.get(0).prefWidth(-1));
            double indicatorHeight = snapSize(indicatorButton.get(0).prefHeight(-1));
            double arrowButtonY = top + Utils.computeYOffset(height, leftArrowHeight, VPos.CENTER);
            double indicatorButtonY = top + Utils.computeYOffset(height, indicatorHeight, VPos.CENTER);

            leftArrowButton.setVisible(true);
            rightArrowButton.setVisible(true);

            if (currentIndex == 0) {
                // Grey out the left arrow if we are at the beginning.
                leftArrowButton.setVisible(false);
            }
            if (currentIndex == (totalNumberOfPages() - 1)) {
                // Grey out the right arrow if we have reached the end.
                rightArrowButton.setVisible(false);
            }

            if (previousIndex < 0) {
                paginationListView.showOffset(0, paginationListView.getSelectionModel().getSelectedIndex());                
            } else {
                paginationListView.showOffset(currentIndex - previousIndex, paginationListView.getSelectionModel().getSelectedIndex());
            }

            leftArrowButton.resize(leftArrowWidth, leftArrowHeight);

            positionInArea(leftArrowButton, left, arrowButtonY, leftArrowWidth, leftArrowHeight, 0, HPos.CENTER, VPos.CENTER);

            double indicatorX = left + leftArrowWidth;
            for (int i = 0; i < indicatorButton.size(); i++) {
                indicatorButton.get(i).resize(indicatorWidth, indicatorHeight);
                positionInArea(indicatorButton.get(i), indicatorX, indicatorButtonY, indicatorWidth, indicatorHeight, 0, HPos.CENTER, VPos.CENTER);
                indicatorX += indicatorWidth;
            }

            rightArrowButton.resize(rightArrowWidth, rightArrowHeight);
            positionInArea(rightArrowButton, indicatorX, arrowButtonY, rightArrowWidth, rightArrowHeight, 0, HPos.CENTER, VPos.CENTER);
        }
    }

    class IndicatorButton extends StackPane {
        private int pageNumber;
        private StackPane indicator;
        private Label pageIndicator;
        private boolean selected;

        public IndicatorButton(int pageNumber) {
            getStyleClass().add("page-navigation");
            this.selected = false;
            this.pageNumber = pageNumber;
            pageIndicator = new Label(Integer.toString(this.pageNumber + 1));

            indicator = new StackPane();
            indicator.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            indicator.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            indicator.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

            setIndicatorType();
            getChildren().setAll(indicator);

            getSkinnable().getStyleClass().addListener(new ListChangeListener<String>() {
                @Override
                public void onChanged(Change<? extends String> change) {
                    setIndicatorType();
                }
            });

            setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent arg0) {
                    int selected = paginationListView.getSelectionModel().getSelectedIndex();
                    // We do not need to update the selection if it has not changed.
                    if (selected != IndicatorButton.this.pageNumber) {
                        paginationListView.getSelectionModel().select(IndicatorButton.this.pageNumber);
                        requestLayout();
                    }
                }
            });
        }

        private void setIndicatorType() {
            if (getSkinnable().getStyleClass().contains(Pagination.STYLE_CLASS_BULLET)) {
                indicator.getStyleClass().setAll("bullet");
                indicator.getChildren().remove(pageIndicator);
            } else {
                indicator.getStyleClass().setAll("number");
                indicator.getChildren().setAll(pageIndicator);
            }
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            impl_pseudoClassStateChanged("selected");
        }

        public int getPageNumber() {
            return this.pageNumber;
        }

        @Override protected double computePrefWidth(double height) {
            double left = snapSpace(getInsets().getLeft());
            double right = snapSpace(getInsets().getRight());

            return left + indicator.prefWidth(height) + right;
        }

        @Override protected double computePrefHeight(double width) {
            double top = snapSpace(getInsets().getTop());
            double bottom = snapSpace(getInsets().getBottom());

            return top + indicator.prefHeight(width) + bottom;
        }

        @Override public long impl_getPseudoClassState() {
            long mask = super.impl_getPseudoClassState();

            if (selected) {
                mask |= SELECTED_PSEUDOCLASS_STATE;
            }
            return mask;
        }
    }

    private static final long SELECTED_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("selected");

    class PaginationListView<T> extends ListView<T> {
        private int previousIndex;
        public PaginationListView() {
            super();
            setId("list-view");
            setOrientation(Orientation.HORIZONTAL);
            getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            previousIndex = -1;
        }

        public void show(int index) {
            if (previousIndex != index) {
                getProperties().put(VirtualContainerBase.SCROLL_TO_INDEX_TOP, index);
                previousIndex = index;
            }
        }

        public void showOffset(int offset, int index) {
            if (previousIndex != index) {
                getProperties().put(VirtualContainerBase.SCROLL_TO_OFFSET, offset);
                previousIndex = index;
            }
        }
        
        public void reset() {
            getProperties().put(VirtualContainerBase.SCROLL_TO_INDEX_TOP, 0);
            previousIndex = -1;
        }
    }
}
