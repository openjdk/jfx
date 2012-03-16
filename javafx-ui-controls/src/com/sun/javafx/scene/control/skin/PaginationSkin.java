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
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

public class PaginationSkin<T> extends SkinBase<Pagination<T>, PaginationBehavior<T>>  {

    private final static int MAX_PAGES = 10;

    private Pagination<T> pagination;
    private PaginationListView<T> paginationListView;
    private ObservableList<T> pages;
    private final ListChangeListener pageListener = new ListChangeListener() {
        @Override public void onChanged(ListChangeListener.Change c) {
            requestLayout();
        }
    };
    private final WeakListChangeListener weakPageListener =
            new WeakListChangeListener(pageListener);

    private Rectangle clipRect;

    private NavigationControl navigation;
    private int fromIndex;
    private int previousIndex;
    private int currentIndex;
    private int toIndex;
    private int totalNumberOfPages;
    private int numberOfPages;

    public PaginationSkin(final Pagination<T> pagination) {
        super(pagination, new PaginationBehavior(pagination));

        setManaged(false);
        clipRect = new Rectangle();
        setClip(clipRect);

        this.pagination = pagination;
        pagination.getSelectionModel().selectFirst();

        this.paginationListView = createListView();
        updateListViewItems();
        updateCellFactory();
        paginationListView.setOrientation(Orientation.HORIZONTAL);

        totalNumberOfPages = pages.size();
        numberOfPages = totalNumberOfPages;
        if (totalNumberOfPages > MAX_PAGES) {
            numberOfPages = MAX_PAGES;
        }

        fromIndex = 0;
        previousIndex = 0;
        currentIndex = 0;
        toIndex = numberOfPages - 1;
        navigation = new NavigationControl();

        getChildren().addAll(paginationListView, navigation);
    }

    private PaginationListView<T> createListView() {
        return new PaginationListView<T>();
    }

    private void updateCellFactory() {
        Callback<ListView<T>, ListCell<T>> cf = pagination.getCellFactory();
        paginationListView.setCellFactory(cf != null ? cf : getDefaultCellFactory());
    }

    private Callback<ListView<T>, ListCell<T>> getDefaultCellFactory() {
        return new Callback<ListView<T>, ListCell<T>>() {
            @Override public ListCell<T> call(ListView<T> listView) {
                return new PaginationCell<T>() {
                    @Override public void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty) {
                            setText(null);
                            setGraphic(null);
                        } else if (item instanceof Node) {
                            setText(null);
                            Node currentNode = getGraphic();
                            Node newNode = (Node) item;
                            if (currentNode == null || ! currentNode.equals(newNode)) {
                                setGraphic(newNode);
                            }
                        } else {
                            /**
                            * This label is used if the item associated with this cell is to be
                            * represented as a String. While we will lazily instantiate it
                            * we never clear it, being more afraid of object churn than a minor
                            * "leak" (which will not become a "major" leak).
                            */
                            setText(item == null ? "null" : item.toString());
                            setGraphic(null);
                        }
                    }
                };
            }
        };
    }

    private void updateListViewItems() {
        if (pages != null) {
            pages.removeListener(weakPageListener);
        }

        this.pages = getSkinnable().getItems();
        paginationListView.setItems(pages);

        if (pages != null) {
            pages.addListener(weakPageListener);
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
            leftArrowButton.getStyleClass().add("arrow-button");
            leftArrowButton.getChildren().setAll(leftArrow);

            StackPane rightArrow = new StackPane();
            rightArrow.getStyleClass().add("right-arrow");
            rightArrowButton = new StackPane();
            rightArrowButton.getStyleClass().add("arrow-button");
            rightArrowButton.getChildren().setAll(rightArrow);

            indicatorButton = new ArrayList<IndicatorButton>();

            getChildren().addAll(leftArrowButton, rightArrowButton);
            setupPageIndicators();
            setupEventHandlers();
            indicatorButton.get(0).setSelected(true);
            leftArrowButton.setVisible(false);
        }

        private void setupEventHandlers() {
            leftArrowButton.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent arg0) {
                    pagination.getSelectionModel().selectPrevious();
                    //System.out.println("LEFT BUTTON " + pagination.getSelectionModel().getSelectedIndex());
                    paginationListView.show(pagination.getSelectionModel().getSelectedIndex());
                    requestLayout();
                }
            });

            rightArrowButton.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent arg0) {
                    pagination.getSelectionModel().selectNext();
                    //System.out.println("RIGHT BUTTON " + pagination.getSelectionModel().getSelectedIndex() + " TNP " + (totalNumberOfPages - 1));
                    paginationListView.show(pagination.getSelectionModel().getSelectedIndex());
                    requestLayout();
                }
            });

            getSkinnable().getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
                    previousIndex = arg1.intValue();
                    currentIndex = arg2.intValue();

                    //System.out.println("\nSELECT PROPERTY FROM " + fromIndex + " TO " + toIndex + " PREVIOUS " + previousIndex + " CURRENT "+ currentIndex + " SIZE " + pages.size());
                    if (currentIndex == 0) {
                        // Grey out the left arrow we are at the beginning.
                        leftArrowButton.setVisible(false);
                    } else if (currentIndex == (totalNumberOfPages - 1)) {
                        // Grey out the right arrow we reached the end.
                        rightArrowButton.setVisible(false);
                    } else {
                        leftArrowButton.setVisible(true);
                        rightArrowButton.setVisible(true);
                        if (numberOfPages == MAX_PAGES) {
                            scroll();
                        }
                    }
                    // Update the indictor buttons
                    for (int i = 0; i < indicatorButton.size(); i++) {
                        if (indicatorButton.get(i).getPageNumber() == previousIndex) {
                            indicatorButton.get(i).setSelected(false);
                        }
                        if (indicatorButton.get(i).getPageNumber() == currentIndex) {
                            indicatorButton.get(i).setSelected(true);
                        }
                    }
                    requestLayout();
                }
            });
        }

        private void setupPageIndicators() {
            if (!indicatorButton.isEmpty()) {
                getChildren().removeAll(indicatorButton);
                indicatorButton.clear();
            }

            for (int i = fromIndex; i <= toIndex; i++) {
                indicatorButton.add(new IndicatorButton(i));
            }
            getChildren().addAll(indicatorButton);
        }

        private void scroll() {
            if (previousIndex < currentIndex && currentIndex % MAX_PAGES == 0) {
                // Scroll to the right
                fromIndex = currentIndex;
                toIndex = fromIndex + (MAX_PAGES - 1);
            } else if (currentIndex < previousIndex && currentIndex % MAX_PAGES == MAX_PAGES - 1) {
                // Scroll to the left
                toIndex = currentIndex;
                fromIndex = toIndex - (MAX_PAGES - 1);
            } else {
                return;
            }

            // We have gone past the total number of pages
            if (toIndex > totalNumberOfPages - 1) {
                toIndex = totalNumberOfPages - 1;
            }

            // We have gone past the starting page
            if (fromIndex < 0) {
                fromIndex = 0;
                toIndex = fromIndex + (MAX_PAGES - 1);
            }
            //System.out.println("SCROLL from " + fromIndex + " to " + toIndex + " previous " + previousIndex + " current " + currentIndex);
            setupPageIndicators();
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
            getStyleClass().add("arrow-button");
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
                    //System.out.println("INDICATOR BUTTON PRESSED " + pageIndicator.getText() + " NUMBER " + IndicatorButton.this.pageNumber);
                    pagination.getSelectionModel().select(IndicatorButton.this.pageNumber);
                    //System.out.println("INDICATOR BUTTON SELECTED INDEX " + pagination.getSelectionModel().getSelectedIndex());
                    paginationListView.show(pagination.getSelectionModel().getSelectedIndex());
                    requestLayout();
                }
            });
        }

        private final void setIndicatorType() {
            if (getSkinnable().getStyleClass().contains(Pagination.STYLE_CLASS_BULLET)) {
                indicator.getStyleClass().setAll("bullet-indicator");
                indicator.getChildren().remove(pageIndicator);
            } else {
                indicator.getStyleClass().setAll("number-indicator");
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
        public PaginationListView() {
            this(FXCollections.<T>observableArrayList());
        }

        public PaginationListView(ObservableList<T> items) {
            super(items);
            setId("list-view");
        }

        public void show(int index) {
            getProperties().put(VirtualContainerBase.SHOW_INDEX, index);
        }
    }
}
