/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.skin;

import com.sun.javafx.PlatformUtil;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import com.sun.javafx.scene.control.behavior.TabPaneBehavior;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.scene.traversal.TraverseListener;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;

import com.sun.javafx.css.StyleManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.input.*;

public class TabPaneSkin extends SkinBase<TabPane, TabPaneBehavior> {

    private static int getRotation(Side pos) {
        switch (pos) {
            case TOP:
                return 0;
            case BOTTOM:
                return 180;
            case LEFT:
                return -90;
            case RIGHT:
                return 90;
            default:
                return 0;
        }
    }

    /**
     * VERY HACKY - this lets us 'duplicate' Label and ImageView nodes to be used in a
     * Tab and the tabs menu at the same time.
     */
    private static Node clone(Node n) {
        if (n == null) {
            return null;
        }
        if (n instanceof ImageView) {
            ImageView iv = (ImageView) n;
            ImageView imageview = new ImageView();
            imageview.setImage(iv.getImage());
            return imageview;
        }
        if (n instanceof Label) {            
            Label l = (Label)n;
            Label label = new Label(l.getText(), l.getGraphic());
            return label;
        }
        return null;
    }
    private static final double ANIMATION_SPEED = 300;
    private static final int SPACER = 10;

    private TabHeaderArea tabHeaderArea;
    private ObservableList<TabContentRegion> tabContentRegions;
    private Rectangle clipRect;
    private Rectangle tabHeaderAreaClipRect;
    boolean focusTraversable = true;
    private Tab selectedTab;
    private Tab previousSelectedTab;
    private boolean isSelectingTab;

    public TabPaneSkin(TabPane tabPane) {
        super(tabPane, new TabPaneBehavior(tabPane));

        clipRect = new Rectangle();
        setClip(clipRect);

        tabContentRegions = FXCollections.<TabContentRegion>observableArrayList();

        for (Tab tab : getSkinnable().getTabs()) {
            addTabContent(tab);
        }

        tabHeaderAreaClipRect = new Rectangle();
        tabHeaderArea = new TabHeaderArea();
        tabHeaderArea.setClip(tabHeaderAreaClipRect);
        getChildren().add(tabHeaderArea);
        if (getSkinnable().getTabs().size() == 0) {
            tabHeaderArea.setVisible(false);
        }

        initializeTabListener();

        registerChangeListener(tabPane.getSelectionModel().selectedItemProperty(), "SELECTED_TAB");
        registerChangeListener(tabPane.sideProperty(), "SIDE");

        previousSelectedTab = null;        
        selectedTab = getSkinnable().getSelectionModel().getSelectedItem();
        // Could not find the selected tab try and get the selected tab using the selected index
        if (selectedTab == null && getSkinnable().getSelectionModel().getSelectedIndex() != -1) {
            getSkinnable().getSelectionModel().select(getSkinnable().getSelectionModel().getSelectedIndex());
            selectedTab = getSkinnable().getSelectionModel().getSelectedItem();        
        } 
        if (selectedTab == null) {
            // getSelectedItem and getSelectedIndex failed select the first.
            getSkinnable().getSelectionModel().selectFirst();
        } 
        selectedTab = getSkinnable().getSelectionModel().getSelectedItem();        
        isSelectingTab = false;
        
        initializeSwipeHandlers();
    }

    public StackPane getSelectedTabContentRegion() {
        for (TabContentRegion contentRegion : tabContentRegions) {
            if (contentRegion.getTab().equals(selectedTab)) {
                return contentRegion;
            }
        }
        return null;
    }

    @Override protected void handleControlPropertyChanged(String property) {
        super.handleControlPropertyChanged(property);
        if (property == "SELECTED_TAB") {            
            isSelectingTab = true;
            previousSelectedTab = selectedTab;
            selectedTab = getSkinnable().getSelectionModel().getSelectedItem();
            requestLayout();
        } else if (property == "SIDE") {
            updateTabPosition();
        }
    }

    private Map<Tab, Timeline> closedTab = new HashMap();

    private void initializeTabListener() {
        getSkinnable().getTabs().addListener(new ListChangeListener<Tab>() {
            @Override public void onChanged(final Change<? extends Tab> c) {      
                while (c.next()) {
                    for (final Tab tab : c.getRemoved()) {
                        // Animate the tab removal
                        final TabHeaderSkin tabRegion = tabHeaderArea.getTabHeaderSkin(tab);
                        Timeline closedTabTimeline = null;
                        if (tabRegion != null) {
                            tabRegion.animating = true;
                            closedTabTimeline = createTimeline(tabRegion, Duration.millis(ANIMATION_SPEED * 1.5F), 0.0F, new EventHandler<ActionEvent>() {

                                @Override
                                public void handle(ActionEvent event) {      
                                    removeTab(tab);
                                    closedTab.remove(tab);
                                    if (getSkinnable().getTabs().isEmpty()) {
                                        tabHeaderArea.setVisible(false);
                                    }
                                }
                            });
                            closedTabTimeline.play();
                        }
                        closedTab.put(tab, closedTabTimeline);
                    }

                    int i = 0;
                    for (final Tab tab : c.getAddedSubList()) {
                        // Handle the case where we are removing and adding the same tab.
                        Timeline closedTabTimeline = closedTab.get(tab);
                        if (closedTabTimeline != null) {
                            closedTabTimeline.stop();
                            Iterator<Tab> keys = closedTab.keySet().iterator();
                            while (keys.hasNext()) {
                                Tab key = keys.next();
                                if (tab.equals(key)) {
                                    removeTab(key);
                                    keys.remove();                                    
                                }
                            }
                        }
                        // A new tab was added - animate it out
                        if (!tabHeaderArea.isVisible()) {
                            tabHeaderArea.setVisible(true);
                        }
                        int index = c.getFrom() + i++;
                        tabHeaderArea.addTab(tab, index, false);
                        addTabContent(tab);
                        final TabHeaderSkin tabRegion = tabHeaderArea.getTabHeaderSkin(tab);
                        if (tabRegion != null) {
                            tabRegion.animateNewTab = new Runnable() {

                                @Override
                                public void run() {
                                    final double w = snapSize(tabRegion.prefWidth(-1));
                                    tabRegion.animating = true;
                                    tabRegion.prefWidth.set(0.0);
                                    tabRegion.setVisible(true);
                                    createTimeline(tabRegion, Duration.millis(ANIMATION_SPEED), w, new EventHandler<ActionEvent>() {

                                        @Override
                                        public void handle(ActionEvent event) {
                                            tabRegion.animating = false;
                                            tabRegion.inner.requestLayout();
                                        }
                                    }).play();
                                }
                            };
                        }
                    }
                }
            }
        });
    }

    private void addTabContent(Tab tab) {
        TabContentRegion tabContentRegion = new TabContentRegion(tab);
        tabContentRegion.setClip(new Rectangle());
        tabContentRegions.add(tabContentRegion);
        // We want the tab content to always sit below the tab headers
        getChildren().add(0, tabContentRegion);
    }

    private void removeTabContent(Tab tab) {
        for (TabContentRegion contentRegion : tabContentRegions) {
            if (contentRegion.getTab().equals(tab)) {
                contentRegion.removeListeners(tab);
                getChildren().remove(contentRegion);
                tabContentRegions.remove(contentRegion);
                break;
            }
        }
    }

    private void removeTab(Tab tab) {
        final TabHeaderSkin tabRegion = tabHeaderArea.getTabHeaderSkin(tab);
        tabHeaderArea.removeTab(tab);
        removeTabContent(tab);
        tabRegion.animating = false;
        tabHeaderArea.requestLayout();
        tab = null;
    }

    private void updateTabPosition() {
        tabHeaderArea.setScrollOffset(0.0F);
        impl_reapplyCSS();
        requestLayout();
    }

    private Timeline createTimeline(final TabHeaderSkin tabRegion, final Duration duration, final double endValue, final EventHandler<ActionEvent> func) {
        Timeline timeline = new Timeline();
        timeline.setCycleCount(1);

        KeyValue keyValue = new KeyValue(tabRegion.prefWidth, endValue, Interpolator.LINEAR);

        timeline.getKeyFrames().clear();
        timeline.getKeyFrames().add(new KeyFrame(duration, func, keyValue));
        return timeline;
    }

    private boolean isHorizontal() {
        Side tabPosition = getSkinnable().getSide();
        return Side.TOP.equals(tabPosition) || Side.BOTTOM.equals(tabPosition);
    }

    private void initializeSwipeHandlers() {
        if (PlatformUtil.isEmbedded()) {
            setOnSwipeLeft(new EventHandler<SwipeEvent>() {
                @Override public void handle(SwipeEvent t) {
                    getBehavior().selectNextTab();
                }
            });

            setOnSwipeRight(new EventHandler<SwipeEvent>() {
                @Override public void handle(SwipeEvent t) {
                    getBehavior().selectPreviousTab();
                }
            });        
        }    
    }
    
    //TODO need to cache this.
    private boolean isFloatingStyleClass() {
        return getSkinnable().getStyleClass().contains(TabPane.STYLE_CLASS_FLOATING);
    }

    @Override protected void setWidth(double value) {
        super.setWidth(value);
        clipRect.setWidth(value);
    }

    @Override protected void setHeight(double value) {
        super.setHeight(value);
        clipRect.setHeight(value);
    }

    private double maxw = 0.0d;
    @Override protected double computePrefWidth(double height) {
        // The TabPane can only be as wide as it widest content width.
        for (TabContentRegion contentRegion: tabContentRegions) {
             maxw = Math.max(maxw, snapSize(contentRegion.prefWidth(-1)));
        }
        double prefwidth = isHorizontal() ?
            maxw : maxw + snapSize(tabHeaderArea.prefHeight(-1));
        return snapSize(prefwidth) + snapSize(getInsets().getRight()) + snapSize(getInsets().getLeft());
    }

    private double maxh = 0.0d;
    @Override protected double computePrefHeight(double width) {
        // The TabPane can only be as high as it highest content height.
        for (TabContentRegion contentRegion: tabContentRegions) {
             maxh = Math.max(maxh, snapSize(contentRegion.prefHeight(-1)));
        }
        double prefheight = isHorizontal()?
            maxh + snapSize(tabHeaderArea.prefHeight(-1)) : maxh;
        return snapSize(prefheight) + snapSize(getInsets().getTop()) + snapSize(getInsets().getBottom());
    }

    @Override public double getBaselineOffset() {
        return tabHeaderArea.getBaselineOffset() + tabHeaderArea.getLayoutY();
    }

    @Override protected void layoutChildren() {
        TabPane tabPane = getSkinnable();
        Side tabPosition = tabPane.getSide();
        Insets padding = getInsets();

        final double w = snapSize(getWidth()) - snapSize(padding.getLeft()) - snapSize(padding.getRight());
        final double h = snapSize(getHeight()) - snapSize(padding.getTop()) - snapSize(padding.getBottom());
        final double x = snapSize(padding.getLeft());
        final double y = snapSize(padding.getTop());

        double headerHeight = snapSize(tabHeaderArea.prefHeight(-1));
        double tabsStartX = tabPosition.equals(Side.RIGHT)? x + w - headerHeight : x;
        double tabsStartY = tabPosition.equals(Side.BOTTOM)? y + h - headerHeight : y;

        if (tabPosition.equals(tabPosition.TOP)) {
            tabHeaderArea.resize(w, headerHeight);
            tabHeaderArea.relocate(tabsStartX, tabsStartY);
            tabHeaderArea.getTransforms().clear();
            tabHeaderArea.getTransforms().add(new Rotate(getRotation(tabPosition.TOP)));
        } else if (tabPosition.equals(tabPosition.BOTTOM)) {
            tabHeaderArea.resize(w, headerHeight);
            tabHeaderArea.relocate(w, tabsStartY - headerHeight);
            tabHeaderArea.getTransforms().clear();
            tabHeaderArea.getTransforms().add(new Rotate(getRotation(tabPosition.BOTTOM), 0, headerHeight));
        } else if (tabPosition.equals(tabPosition.LEFT)) {
            tabHeaderArea.resize(h, headerHeight);
            tabHeaderArea.relocate(tabsStartX + headerHeight, h - headerHeight);
            tabHeaderArea.getTransforms().clear();
            tabHeaderArea.getTransforms().add(new Rotate(getRotation(tabPosition.LEFT), 0, headerHeight));
        } else if (tabPosition.equals(tabPosition.RIGHT)) {
            tabHeaderArea.resize(h, headerHeight);
            tabHeaderArea.relocate(tabsStartX, y - headerHeight);
            tabHeaderArea.getTransforms().clear();
            tabHeaderArea.getTransforms().add(new Rotate(getRotation(tabPosition.RIGHT), 0, headerHeight));
        }

        tabHeaderAreaClipRect.setX(0);
        tabHeaderAreaClipRect.setY(0);
        if (isHorizontal()) {
            tabHeaderAreaClipRect.setWidth(w);
        } else {
            tabHeaderAreaClipRect.setWidth(h);
        }
        tabHeaderAreaClipRect.setHeight(headerHeight);

        // ==================================
        // position the tab content for the selected tab only
        // ==================================
        // if the tabs are on the left, the content needs to be indented
        double contentStartX = 0;
        double contentStartY = 0;

        if (tabPosition.equals(tabPosition.TOP)) {
            contentStartX = x;
            contentStartY = y + headerHeight;
            if (isFloatingStyleClass()) {
                // This is to hide the top border content
                contentStartY -= 1;
            }
        } else if (tabPosition.equals(tabPosition.BOTTOM)) {
            contentStartX = x;
            contentStartY = y;
            if (isFloatingStyleClass()) {
                // This is to hide the bottom border content
                contentStartY = 1;
            }
        } else if (tabPosition.equals(tabPosition.LEFT)) {
            contentStartX = x + headerHeight;
            contentStartY = y;
            if (isFloatingStyleClass()) {
                // This is to hide the left border content
                contentStartX -= 1;
            }
        } else if (tabPosition.equals(tabPosition.RIGHT)) {
            contentStartX = x;
            contentStartY = y;
            if (isFloatingStyleClass()) {
                // This is to hide the right border content
                contentStartX = 1;
            }
        }

        double contentWidth = w - (isHorizontal() ? 0 : headerHeight);
        double contentHeight = h - (isHorizontal() ? headerHeight: 0);
        for (TabContentRegion tabContent : tabContentRegions) {
            if (tabContent.getTab().equals(selectedTab)) {
                tabContent.setAlignment(Pos.TOP_LEFT);
                if (tabContent.getClip() != null) {
                    ((Rectangle)tabContent.getClip()).setWidth(contentWidth);
                    ((Rectangle)tabContent.getClip()).setHeight(contentHeight);
                }
                tabContent.resize(contentWidth, contentHeight);
                tabContent.relocate(contentStartX, contentStartY);
                Node content = tabContent.getTab().getContent();
                if (content != null) content.setVisible(true);
            } else {
                Node content = tabContent.getTab().getContent();
                if (content != null) content.setVisible(false);
            }
        }
    }

    /**************************************************************************
     *
     * TabHeaderArea: Area responsible for painting all tabs
     *
     **************************************************************************/
    class TabHeaderArea extends StackPane {
        private Rectangle headerClip;
        private StackPane headersRegion;
        private StackPane headerBackground;
        private TabControlButtons controlButtons;

        // -- drag support for tabs
        private double lastDragPos;

        // + headersRegion.padding.top + headersRegion.padding.bottom;
        private double scrollOffset;
        public double getScrollOffset() {
            return scrollOffset;
        }
        public void setScrollOffset(double value) {
            scrollOffset = value;
            headersRegion.requestLayout();
        }
        private Point2D dragAnchor;
        public TabHeaderArea() {
            getStyleClass().setAll("tab-header-area");
            setManaged(false);
            final TabPane tabPane = getSkinnable();

            headerClip = new Rectangle();

            headersRegion = new StackPane() {
                @Override protected double computePrefWidth(double height) {
                    double width = 0.0F;
                    for (Node child : getChildren()) {
                        TabHeaderSkin tabHeaderSkin = (TabHeaderSkin)child;
                        if (tabHeaderSkin.isVisible()) {
                            width += tabHeaderSkin.prefWidth(height);
                        }
                    }
                    return snapSize(width) + snapSize(getInsets().getLeft()) + snapSize(getInsets().getRight());
                }

                @Override protected double computePrefHeight(double width) {
                    double height = 0.0F;
                    for (Node child : getChildren()) {
                        TabHeaderSkin tabHeaderSkin = (TabHeaderSkin)child;
                        height = Math.max(height, tabHeaderSkin.prefHeight(width));
                    }
                    return snapSize(height) + snapSize(getInsets().getTop()) + snapSize(getInsets().getBottom());
                }

                @Override protected void layoutChildren() {
                    if (tabsFit()) {
                        controlButtons.showTabsMenu(false);
                        setScrollOffset(0.0);
                    } else {
                        controlButtons.showTabsMenu(true);
                        if (!removeTab.isEmpty()) {                            
                            double offset = 0;
                            double w = tabHeaderArea.getWidth() - snapSize(controlButtons.prefWidth(-1)) - firstTabIndent() - SPACER;
                            Iterator i = getChildren().iterator();
                            while (i.hasNext()) {
                                TabHeaderSkin tabHeader = (TabHeaderSkin)i.next();
                                double tabHeaderPrefWidth = snapSize(tabHeader.prefWidth(-1));
                                if (removeTab.contains(tabHeader)) {                                    
                                    if (offset < w) {
                                        isSelectingTab = true;
                                    }
                                    i.remove();
                                    removeTab.remove(tabHeader);
                                    if (removeTab.isEmpty()) {
                                        break;
                                    }
                                }
                                offset += tabHeaderPrefWidth;                                
                            }
                        } else {
                            isSelectingTab = true;
                        }
                    }

                    if (isSelectingTab) {
                        double offset = 0;
                        double selectedTabOffset = 0;
                        double selectedTabWidth = 0;
                        double previousSelectedTabOffset = 0;
                        double previousSelectedTabWidth = 0;
                        for (Node node: getChildren()) {
                            TabHeaderSkin tabHeader = (TabHeaderSkin)node;
                            // size and position the header relative to the other headers
                            double tabHeaderPrefWidth = snapSize(tabHeader.prefWidth(-1));
                            if (selectedTab != null && selectedTab.equals(tabHeader.getTab())) {
                                selectedTabOffset = offset;
                                selectedTabWidth = tabHeaderPrefWidth;
                            }
                            if (previousSelectedTab != null && previousSelectedTab.equals(tabHeader.getTab())) {
                                previousSelectedTabOffset = offset;
                                previousSelectedTabWidth = tabHeaderPrefWidth;
                            }
                            offset+=tabHeaderPrefWidth;
                        }
                        if (selectedTabOffset > previousSelectedTabOffset) {
                            scrollToSelectedTab(selectedTabOffset + selectedTabWidth, previousSelectedTabOffset);
                        } else {
                            scrollToSelectedTab(selectedTabOffset, previousSelectedTabOffset);
                        }
                        isSelectingTab = false;
                    }

                    Side tabPosition = getSkinnable().getSide();
                    double tabBackgroundHeight = snapSize(prefHeight(-1));
                    double tabX = (tabPosition.equals(Side.LEFT) || tabPosition.equals(Side.BOTTOM)) ?
                        snapSize(getWidth()) - getScrollOffset() : getScrollOffset();

                    updateHeaderClip();
                    for (Node node : getChildren()) {
                        TabHeaderSkin tabHeader = (TabHeaderSkin)node;
                        // size and position the header relative to the other headers
                        double tabHeaderPrefWidth = snapSize(tabHeader.prefWidth(-1));
                        double tabHeaderPrefHeight = snapSize(tabHeader.prefHeight(-1));
                        tabHeader.resize(tabHeaderPrefWidth, tabHeaderPrefHeight);
                        // This ensures that the tabs are located in the correct position
                        // when there are tabs of differing heights.
                        double startY = tabPosition.equals(Side.BOTTOM) ?
                            0 : tabBackgroundHeight - tabHeaderPrefHeight - snapSize(getInsets().getBottom());
                        if (tabPosition.equals(Side.LEFT) || tabPosition.equals(Side.BOTTOM)) {
                            // build from the right
                            tabX -= tabHeaderPrefWidth;
                            tabHeader.relocate(tabX, startY);
                        } else {
                            // build from the left
                            tabHeader.relocate(tabX, startY);
                            tabX += tabHeaderPrefWidth;
                        }
                    }
                }
            };
            headersRegion.getStyleClass().setAll("headers-region");
            headersRegion.setClip(headerClip);

            headerBackground = new StackPane();
            headerBackground.getStyleClass().setAll("tab-header-background");

            int i = 0;
            for (Tab tab: tabPane.getTabs()) {
                addTab(tab, i++, true);
            }

            controlButtons = new TabControlButtons();
            controlButtons.setVisible(false);
            if (controlButtons.isVisible()) {
                controlButtons.setVisible(true);
            }
            getChildren().addAll(headerBackground, headersRegion, controlButtons);

        }

        private void updateHeaderClip() {
            Side tabPosition = getSkinnable().getSide();

            double x = 0;
            double y = 0;
            double clipWidth = 0;
            double clipHeight = 0;
            double maxWidth = 0;
            double shadowRadius = 0;
            double clipOffset = firstTabIndent();
            double controlButtonPrefWidth = snapSize(controlButtons.prefWidth(-1));
            double headersPrefWidth = snapSize(headersRegion.prefWidth(-1));
            double headersPrefHeight = snapSize(headersRegion.prefHeight(-1));

            // Add the spacer if isShowTabsMenu is true.
            if (controlButtonPrefWidth > 0) {
                controlButtonPrefWidth = controlButtonPrefWidth + SPACER;
            }

            if (headersRegion.getEffect() instanceof DropShadow) {
                DropShadow shadow = (DropShadow)headersRegion.getEffect();
                shadowRadius = shadow.getRadius();
            }

            maxWidth = snapSize(getWidth()) - controlButtonPrefWidth - clipOffset;
            if (tabPosition.equals(Side.LEFT) || tabPosition.equals(Side.BOTTOM)) {
                if (headersPrefWidth < maxWidth) {
                    clipWidth = headersPrefWidth + shadowRadius;
                } else {
                    x = headersPrefWidth - maxWidth;
                    clipWidth = maxWidth + shadowRadius;
                }
                clipHeight = headersPrefHeight;
            } else {
                // If x = 0 the header region's drop shadow is clipped.
                x = -shadowRadius;
                clipWidth = (headersPrefWidth < maxWidth ? headersPrefWidth : maxWidth) + shadowRadius;
                clipHeight = headersPrefHeight;
            }

            headerClip.setX(x);
            headerClip.setY(y);
            headerClip.setWidth(clipWidth);
            headerClip.setHeight(clipHeight);
        }

        private void addTab(Tab tab, int addToIndex, boolean visible) {
            TabHeaderSkin tabHeaderSkin = new TabHeaderSkin(tab);
            tabHeaderSkin.setVisible(visible);
            headersRegion.getChildren().add(addToIndex, tabHeaderSkin);
        }

        private List<TabHeaderSkin> removeTab = new ArrayList();
        private void removeTab(Tab tab) {
            TabHeaderSkin tabHeaderSkin = getTabHeaderSkin(tab);
            if (tabHeaderSkin != null) {
                if (tabsFit()) {
                    headersRegion.getChildren().remove(tabHeaderSkin);
                } else {
                    // The tab will be removed during layout because
                    // we need its width to compute the scroll offset.
                    removeTab.add(tabHeaderSkin);
                    tabHeaderSkin.removeListeners(tab);
                }
            }
        }

        private TabHeaderSkin getTabHeaderSkin(Tab tab) {
            for (Node child: headersRegion.getChildren()) {
                TabHeaderSkin tabHeaderSkin = (TabHeaderSkin)child;
                if (tabHeaderSkin.getTab().equals(tab)) {
                    return tabHeaderSkin;
                }
            }
            return null;
        }

        // ----- Code for scrolling the tab header area based on the user clicking
        // the left/right arrows on the control buttons tab
        private Timeline scroller;

        private void createScrollTimeline(final double val) {
            scroll(val);
            scroller = new Timeline();
            scroller.setCycleCount(Timeline.INDEFINITE);
            scroller.getKeyFrames().add(new KeyFrame(Duration.millis(150), new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    scroll(val);
                }
            }));
        }

        // ----- End of control button scrolling support
        private void scroll(double d) {
            if (tabsFit()) {
                return;
            }
            Side tabPosition = getSkinnable().getSide();
            double headerPrefWidth = snapSize(headersRegion.prefWidth(-1));
            double controlTabWidth = snapSize(controlButtons.prefWidth(-1));
            double max = getWidth() - headerPrefWidth - controlTabWidth;
            double delta = tabPosition.equals(Side.LEFT) || tabPosition.equals(Side.BOTTOM) ? -d : d;
            double newOffset = getScrollOffset() + delta;
            setScrollOffset(newOffset >= 0 ? 0.0F : (newOffset <= max ? max : newOffset));
        }

        private boolean tabsFit() {
            double headerPrefWidth = snapSize(headersRegion.prefWidth(-1));
            double controlTabWidth = snapSize(controlButtons.prefWidth(-1));
            double visibleWidth = headerPrefWidth + controlTabWidth + firstTabIndent() + SPACER;
            return visibleWidth < getWidth();
        }

        private void scrollToSelectedTab(double selected, double previous) {
            if (selected > previous) {
                // needs to scroll to the left
                double distance = selected - previous;
                double offset = (previous + getScrollOffset()) + distance;
                double width = snapSize(getWidth()) - snapSize(controlButtons.prefWidth(-1)) - firstTabIndent() - SPACER;
                if (offset > width) {
                    setScrollOffset(getScrollOffset() -(offset - width));
                }
            } else {
                // needs to scroll to the right
                double offset = selected + getScrollOffset();
                if (offset < 0) {
                    setScrollOffset(getScrollOffset() - offset);
                }
            }
        }

        private double firstTabIndent() {
            switch (getSkinnable().getSide()) {
                case TOP:
                case BOTTOM:
                    return snapSize(getInsets().getLeft());
                case RIGHT:
                case LEFT:
                    return snapSize(getInsets().getTop());
                default:
                    return 0;
            }
        }

        @Override protected double computePrefWidth(double height) {
            double padding = isHorizontal() ?
                snapSize(getInsets().getLeft()) + snapSize(getInsets().getRight()) :
                snapSize(getInsets().getTop()) + snapSize(getInsets().getBottom());
            return snapSize(headersRegion.prefWidth(-1)) + padding;
        }

        @Override protected double computePrefHeight(double width) {
            double padding = isHorizontal() ?
                snapSize(getInsets().getTop()) + snapSize(getInsets().getBottom()) :
                snapSize(getInsets().getLeft()) + snapSize(getInsets().getRight());
            return snapSize(headersRegion.prefHeight(-1)) + padding;
        }

        @Override public double getBaselineOffset() {
            return headersRegion.getBaselineOffset() + headersRegion.getLayoutY();
        }

        @Override protected void layoutChildren() {
            TabPane tabPane = getSkinnable();
            Insets padding = getInsets();
            double w = snapSize(getWidth()) - (isHorizontal() ?
                snapSize(padding.getLeft()) + snapSize(padding.getRight()) : snapSize(padding.getTop()) + snapSize(padding.getBottom()));
            double h = snapSize(getHeight()) - (isHorizontal() ?
                snapSize(padding.getTop()) + snapSize(padding.getBottom()) : snapSize(padding.getLeft()) + snapSize(padding.getRight()));
            double tabBackgroundHeight = snapSize(prefHeight(-1));
            double headersPrefWidth = snapSize(headersRegion.prefWidth(-1));
            double headersPrefHeight = snapSize(headersRegion.prefHeight(-1));

            updateHeaderClip();

            // RESIZE CONTROL BUTTONS
            double btnWidth = snapSize(controlButtons.prefWidth(-1));
            controlButtons.resize(btnWidth, controlButtons.getControlTabHeight());
            // POSITION TABS
            headersRegion.resize(headersPrefWidth, headersPrefHeight);

            if (isFloatingStyleClass()) {
                headerBackground.setVisible(false);
            } else {
                headerBackground.resize(snapSize(getWidth()), snapSize(getHeight()));
                headerBackground.setVisible(true);
            }

            double startX = 0;
            double startY = 0;
            double controlStartX = 0;
            double controlStartY = 0;
            Side tabPosition = getSkinnable().getSide();

            if (tabPosition.equals(Side.TOP)) {
                startX = snapSize(padding.getLeft());
                startY = tabBackgroundHeight - headersPrefHeight - snapSize(padding.getBottom());
                controlStartX = w - btnWidth + snapSize(padding.getLeft());
                controlStartY = snapSize(getHeight()) - controlButtons.getControlTabHeight() - snapSize(padding.getBottom());
            } else if (tabPosition.equals(Side.RIGHT)) {
                startX = snapSize(padding.getTop());
                startY = tabBackgroundHeight - headersPrefHeight - snapSize(padding.getLeft());
                controlStartX = w - btnWidth + snapSize(padding.getTop());
                controlStartY = snapSize(getHeight()) - controlButtons.getControlTabHeight() - snapSize(padding.getLeft());
            } else if (tabPosition.equals(Side.BOTTOM)) {
                startX = snapSize(getWidth()) - headersPrefWidth - snapSize(getInsets().getLeft());
                startY = tabBackgroundHeight - headersPrefHeight - snapSize(padding.getTop());
                controlStartX = snapSize(padding.getRight());
                controlStartY = snapSize(getHeight()) - controlButtons.getControlTabHeight() - snapSize(padding.getTop());
            } else if (tabPosition.equals(Side.LEFT)) {
                startX = snapSize(getWidth()) - headersPrefWidth - snapSize(getInsets().getTop());
                startY = tabBackgroundHeight - headersPrefHeight - snapSize(padding.getRight());
                controlStartX = snapSize(padding.getLeft());
                controlStartY = snapSize(getHeight()) - controlButtons.getControlTabHeight() - snapSize(padding.getRight());
            }
            if (headerBackground.isVisible()) {
                positionInArea(headerBackground, 0, 0,
                        snapSize(getWidth()), snapSize(getHeight()), /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
            }
            positionInArea(headersRegion, startX, startY, w, h, /*baseline ignored*/0, HPos.LEFT, VPos.CENTER);
            positionInArea(controlButtons, controlStartX, controlStartY, btnWidth, controlButtons.getControlTabHeight(),
                        /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
        }
    } /* End TabHeaderArea */

    static int CLOSE_BTN_SIZE = 16;

    /**************************************************************************
     *
     * TabHeaderSkin: skin for each tab
     *
     **************************************************************************/

    class TabHeaderSkin extends StackPane {
        private final Tab tab;
        public Tab getTab() {
            return tab;
        }
        private Label label;
        private StackPane closeBtn;
        private StackPane inner;
        private Tooltip tooltip;
        private Rectangle clip;
        private InvalidationListener tabListener;
        private InvalidationListener controlListener;

        public TabHeaderSkin(final Tab tab) {
            getStyleClass().setAll(tab.getStyleClass());
            setId(tab.getId());
            setStyle(tab.getStyle());

            this.tab = tab;
            clip = new Rectangle();
            setClip(clip);

            label = new Label(tab.getText(), tab.getGraphic());
            label.getStyleClass().setAll("tab-label");

            closeBtn = new StackPane() {
                @Override protected double computePrefWidth(double h) {
                    return CLOSE_BTN_SIZE;
                }
                @Override protected double computePrefHeight(double w) {
                    return CLOSE_BTN_SIZE;
                }
            };
            closeBtn.getStyleClass().setAll("tab-close-button");
            closeBtn.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent me) {
                    removeListeners(getTab());
                    getBehavior().closeTab(getTab());
                    setOnMousePressed(null);
                }
            });
            
            updateGraphicRotation();

            inner = new StackPane() {
                @Override protected void layoutChildren() {
                    Side tabPosition = getSkinnable().getSide();
                    double paddingTop = snapSize(getInsets().getTop());
                    double paddingRight = snapSize(getInsets().getRight());
                    double paddingBottom = snapSize(getInsets().getBottom());
                    double paddingLeft = snapSize(getInsets().getLeft());
                    double w = getWidth() - paddingLeft + paddingRight;
                    double h = getHeight() - paddingTop + paddingBottom;

                    double labelWidth = snapSize(label.prefWidth(-1));
                    double labelHeight = snapSize(label.prefHeight(-1));
                    double closeBtnWidth = showCloseButton() ? snapSize(closeBtn.prefWidth(-1)) : 0;
                    double closeBtnHeight = showCloseButton() ? snapSize(closeBtn.prefHeight(-1)) : 0;
                    double minWidth = snapSize(getSkinnable().getTabMinWidth());
                    double maxWidth = snapSize(getSkinnable().getTabMaxWidth());
                    double minHeight = snapSize(getSkinnable().getTabMinHeight());
                    double maxHeight = snapSize(getSkinnable().getTabMaxHeight());

                    double childrenWidth = labelWidth + closeBtnWidth;
                    double childrenHeight = Math.max(labelHeight, closeBtnHeight);

                    if (childrenWidth > maxWidth && maxWidth != Double.MAX_VALUE) {
                        labelWidth = maxWidth - closeBtnWidth;
                    } else if (childrenWidth < minWidth) {
                        labelWidth = minWidth - closeBtnWidth;
                    }

                    if (childrenHeight > maxHeight && maxHeight != Double.MAX_VALUE) {
                        labelHeight = maxHeight;
                    } else if (childrenHeight < minHeight) {
                        labelHeight = minHeight;
                    }

                    if (animating) {
                        if (prefWidth.getValue() < labelWidth) {
                            labelWidth = prefWidth.getValue();
                        }
                        closeBtn.setVisible(false);
                    } else {
                        closeBtn.setVisible(showCloseButton());
                    }
                    
                    label.resize(labelWidth, labelHeight);

                    double labelStartX = paddingLeft;
                    double closeBtnStartX = (maxWidth != Double.MAX_VALUE ? maxWidth : w) - paddingRight - closeBtnWidth;
                    
                    positionInArea(label, labelStartX, paddingTop, labelWidth, h,
                            /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);

                    if (closeBtn.isVisible()) {
                        closeBtn.resize(closeBtnWidth, closeBtnHeight);
                        positionInArea(closeBtn, closeBtnStartX, paddingTop, closeBtnWidth, h,
                                /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
                    }
                }
            };
            inner.setRotate(getSkinnable().getSide().equals(Side.BOTTOM) ? 180.0F : 0.0F);
            inner.getChildren().addAll(label, closeBtn);

            getChildren().addAll(inner);

            tooltip = tab.getTooltip();
            if (tooltip != null) {
                Tooltip.install(this, tooltip);
            }

            tabListener = new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    if (valueModel == tab.selectedProperty()) {
                        impl_pseudoClassStateChanged("selected");
                        // Need to request a layout pass for inner because if the width
                        // and height didn't not change the label or close button may have
                        // changed.
                        inner.requestLayout();
                        requestLayout();
                    } else if (valueModel == tab.textProperty()) {
                        label.setText(getTab().getText());
                    } else if (valueModel == tab.graphicProperty()) {
                        label.setGraphic(getTab().getGraphic());
                    } else if (valueModel == tab.contextMenuProperty()) {
                        // todo
                    } else if (valueModel == tab.tooltipProperty()) {
                        getChildren().remove(tooltip);
                        tooltip = tab.getTooltip();
                        if (tooltip != null) {
//                            getChildren().addAll(tooltip);
                        }
                    } else if (valueModel == tab.styleProperty()) {
                        setStyle(tab.getStyle());
                    } else if (valueModel == tab.disableProperty()) {
                        impl_pseudoClassStateChanged("disabled");
                        inner.requestLayout();
                        requestLayout();
                    } else if (valueModel == tab.closableProperty()) {
                        inner.requestLayout();
                        requestLayout();
                    }
                }
            };
            
            tab.closableProperty().addListener(tabListener);
            tab.selectedProperty().addListener(tabListener);
            tab.textProperty().addListener(tabListener);
            tab.graphicProperty().addListener(tabListener);
            tab.contextMenuProperty().addListener(tabListener);
            tab.tooltipProperty().addListener(tabListener);
            tab.disableProperty().addListener(tabListener);
            tab.styleProperty().addListener(tabListener);
            tab.getStyleClass().addListener(new ListChangeListener<String>() {
                @Override
                public void onChanged(Change<? extends String> c) {
                    getStyleClass().setAll(tab.getStyleClass());
                }
            });

            controlListener = new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    if (valueModel == getSkinnable().tabClosingPolicyProperty()) {
                        inner.requestLayout();
                        requestLayout();
                    } else if (valueModel == getSkinnable().sideProperty()) {
                        inner.setRotate(getSkinnable().getSide().equals(Side.BOTTOM) ? 180.0F : 0.0F);
                        if (getSkinnable().isRotateGraphic()) {
                            updateGraphicRotation();
                        }
                    } else if (valueModel == getSkinnable().rotateGraphicProperty()) {
                        updateGraphicRotation();
                    } else if (valueModel == getSkinnable().tabMinWidthProperty() ||
                            valueModel == getSkinnable().tabMaxWidthProperty() ||
                            valueModel == getSkinnable().tabMinHeightProperty() ||
                            valueModel == getSkinnable().tabMaxHeightProperty()) {
                        requestLayout();
                    }
                }
            };
            getSkinnable().tabClosingPolicyProperty().addListener(controlListener);
            getSkinnable().sideProperty().addListener(controlListener);
            getSkinnable().rotateGraphicProperty().addListener(controlListener);
            getSkinnable().tabMinWidthProperty().addListener(controlListener);
            getSkinnable().tabMaxWidthProperty().addListener(controlListener);
            getSkinnable().tabMinHeightProperty().addListener(controlListener);
            getSkinnable().tabMaxHeightProperty().addListener(controlListener);
            getProperties().put(Tab.class, tab);
            getProperties().put(ContextMenu.class, tab.getContextMenu());

            setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
                @Override public void handle(ContextMenuEvent me) {
                   if (getTab().getContextMenu() != null) {
                        getTab().getContextMenu().show(inner, me.getScreenX(), me.getScreenY());
                        me.consume();
                    }
                }
            });
            setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent me) {
                    if (getTab().isDisable()) {
                        return;
                    }
                    if (me.getButton().equals(MouseButton.MIDDLE)) {
                        if (showCloseButton()) {
                            removeListeners(getTab());
                            getBehavior().closeTab(getTab());
                        }
                    } else if (me.getButton().equals(MouseButton.PRIMARY)) {
                        getBehavior().selectTab(getTab());
                    }
                }
            });            
        }

        private void updateGraphicRotation() {
            if (label.getGraphic() != null) {
                label.getGraphic().setRotate(getSkinnable().isRotateGraphic() ? 0.0F :
                    (getSkinnable().getSide().equals(Side.RIGHT) ? -90.0F :
                        (getSkinnable().getSide().equals(Side.LEFT) ? 90.0F : 0.0F)));
            }
        }

        private boolean showCloseButton() {
            return tab.isClosable() &&
                    (getSkinnable().getTabClosingPolicy().equals(TabClosingPolicy.ALL_TABS) ||
                    getSkinnable().getTabClosingPolicy().equals(TabClosingPolicy.SELECTED_TAB) && tab.isSelected());
        }

        private final DoubleProperty prefWidth = new DoublePropertyBase() {
            @Override
            protected void invalidated() {
                requestLayout();
            }

            @Override
            public Object getBean() {
                return TabHeaderSkin.this;
            }

            @Override
            public String getName() {
                return "prefWidth";
            }
        };

        private void removeListeners(Tab tab) {
            tab.selectedProperty().removeListener(tabListener);
            tab.textProperty().removeListener(tabListener);
            tab.graphicProperty().removeListener(tabListener);
            ContextMenu menu = tab.getContextMenu();
            if (menu != null) {
                menu.getItems().clear();
            }
            tab.contextMenuProperty().removeListener(tabListener);
            tab.tooltipProperty().removeListener(tabListener);
            tab.styleProperty().removeListener(tabListener);
            getSkinnable().tabClosingPolicyProperty().removeListener(controlListener);
            getSkinnable().sideProperty().removeListener(controlListener);
            getSkinnable().rotateGraphicProperty().removeListener(controlListener);
            getSkinnable().tabMinWidthProperty().removeListener(controlListener);
            getSkinnable().tabMaxWidthProperty().removeListener(controlListener);
            getSkinnable().tabMinHeightProperty().removeListener(controlListener);
            getSkinnable().tabMaxHeightProperty().removeListener(controlListener);
            inner.getChildren().clear();
            getChildren().clear();
        }

        private boolean animating = false;

        @Override protected double computePrefWidth(double height) {
            if (animating) {
                return prefWidth.getValue();
            }
            double minWidth = snapSize(getSkinnable().getTabMinWidth());
            double maxWidth = snapSize(getSkinnable().getTabMaxWidth());
            double paddingRight = snapSize(getInsets().getRight());
            double paddingLeft = snapSize(getInsets().getLeft());
            double tmpPrefWidth = snapSize(label.prefWidth(-1));

            // only include the close button width if it is relevant
            if (showCloseButton()) {
                tmpPrefWidth += snapSize(closeBtn.prefWidth(-1));
            }

            if (tmpPrefWidth > maxWidth) {
                tmpPrefWidth = maxWidth;
            } else if (tmpPrefWidth < minWidth) {
                tmpPrefWidth = minWidth;
            }
            tmpPrefWidth += paddingRight + paddingLeft;
            prefWidth.setValue(tmpPrefWidth);
            return tmpPrefWidth;
        }

        @Override protected double computePrefHeight(double width) {
            double minHeight = snapSize(getSkinnable().getTabMinHeight());
            double maxHeight = snapSize(getSkinnable().getTabMaxHeight());
            double paddingTop = snapSize(getInsets().getTop());
            double paddingBottom = snapSize(getInsets().getBottom());
            double tmpPrefHeight = snapSize(label.prefHeight(width));

            if (tmpPrefHeight > maxHeight) {
                tmpPrefHeight = maxHeight;
            } else if (tmpPrefHeight < minHeight) {
                tmpPrefHeight = minHeight;
            }
            tmpPrefHeight += paddingTop + paddingBottom;
            return tmpPrefHeight;
        }

        private Runnable animateNewTab = null;

        @Override protected void layoutChildren() {            
            Insets padding = getInsets();            
            inner.resize(snapSize(getWidth()) - snapSize(padding.getRight()) - snapSize(padding.getLeft()),
                    snapSize(getHeight()) - snapSize(padding.getTop()) - snapSize(padding.getBottom()));
            inner.relocate(snapSize(padding.getLeft()), snapSize(padding.getTop()));

            if (animateNewTab != null) {
                animateNewTab.run();
                animateNewTab = null;
            }
        }

        @Override protected void setWidth(double value) {
            super.setWidth(value);
            clip.setWidth(value);
        }

        @Override protected void setHeight(double value) {
            super.setHeight(value);
            clip.setHeight(value);
        }


        @Override
        public long impl_getPseudoClassState() {
            long mask = super.impl_getPseudoClassState();

            if (getTab().isDisable()) {
                mask |= DISABLED_PSEUDOCLASS_STATE;
            } else if (getTab().isSelected()) {
                mask |= SELECTED_PSEUDOCLASS_STATE;
            }

            switch(getSkinnable().getSide()) {
                case TOP:
                    mask |= TOP_PSEUDOCLASS_STATE;
                    break;
                case RIGHT:
                    mask |= RIGHT_PSEUDOCLASS_STATE;
                    break;
                case BOTTOM:
                    mask |= BOTTOM_PSEUDOCLASS_STATE;
                    break;
                case LEFT:
                    mask |= LEFT_PSEUDOCLASS_STATE;
                    break;
            }

            return mask;
        }

    } /* End TabHeaderSkin */

    private static final long SELECTED_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("selected");
    private static final long TOP_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("top");
    private static final long BOTTOM_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("bottom");
    private static final long LEFT_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("left");
    private static final long RIGHT_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("right");
    private static final long DISABLED_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("disabled");    


   /**************************************************************************
     *
     * TabContentRegion: each tab has one to contain the tab's content node
     *
     **************************************************************************/
    class TabContentRegion extends StackPane implements TraverseListener {

        private TraversalEngine engine;
        private Direction direction;
        private Tab tab;
        private InvalidationListener tabListener;

        public Tab getTab() {
            return tab;
        }

        public TabContentRegion(Tab tab) {
            getStyleClass().setAll("tab-content-area");
            setManaged(false);
            this.tab = tab;
            updateContent();

            tabListener = new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    if (valueModel == getTab().selectedProperty()) {
                        setVisible(getTab().isSelected());
                    } else if (valueModel == getTab().contentProperty()) {
                        getChildren().clear();
                        updateContent();
                    }
                }
            };
            tab.selectedProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    setVisible(getTab().isSelected());
                }
            });
            tab.contentProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    getChildren().clear();
                    updateContent();
                }
            });

            tab.selectedProperty().addListener(tabListener);
            tab.contentProperty().addListener(tabListener);

            engine = new TraversalEngine(this, false) {
                @Override public void trav(Node owner, Direction dir) {
                    direction = dir;
                    super.trav(owner, dir);
                }
            };
            engine.addTraverseListener(this);
            setImpl_traversalEngine(engine);
            setVisible(tab.isSelected());
        }

        private void updateContent() {
            if (getTab().getContent() != null) {
                getChildren().add(getTab().getContent());
            }
        }

        private void removeListeners(Tab tab) {
            tab.selectedProperty().removeListener(tabListener);
            tab.contentProperty().removeListener(tabListener);
            engine.removeTraverseListener(this);
        }

        @Override public void onTraverse(Node node, Bounds bounds) {
            int index = engine.registeredNodes.indexOf(node);

            if (index == -1 && direction.equals(Direction.PREVIOUS)) {
                // Sends the focus back the tab
                getSkinnable().requestFocus();
            }
            if (index == -1 && direction.equals(Direction.NEXT)) {
                // Sends the focus to the next focusable control outside of the TabPane
                new TraversalEngine(getSkinnable(), false).trav(getSkinnable(), Direction.NEXT);
            }
        }
    } /* End TabContentRegion */

    /**************************************************************************
     *
     * TabControlButtons: controls to manipulate tab interaction
     *
     **************************************************************************/
    class TabControlButtons extends StackPane {
        private StackPane inner;
        private StackPane downArrow;
        private Pane downArrowBtn;
        private boolean showControlButtons;
        private ContextMenu popup;

        public TabControlButtons() {            
            getStyleClass().setAll("control-buttons-tab");

            TabPane tabPane = getSkinnable();

            downArrowBtn = new Pane();
            downArrowBtn.getStyleClass().setAll("tab-down-button");
            downArrowBtn.setVisible(isShowTabsMenu());
            downArrow = new StackPane();
            downArrow.setManaged(false);
            downArrow.getStyleClass().setAll("arrow");
            downArrow.setRotate(tabPane.getSide().equals(Side.BOTTOM) ? 180.0F : 0.0F);
            downArrowBtn.getChildren().add(downArrow);
            downArrowBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent me) {
                    showPopupMenu();
                }
            });
            
            setupPopupMenu();

            inner = new StackPane() {
                private double getArrowBtnWidth() {
                    if (animationLock) return maxArrowWidth;
                    if (isShowTabsMenu()) {
                        maxArrowWidth = Math.max(maxArrowWidth, snapSize(downArrow.prefWidth(getHeight())) + snapSize(downArrowBtn.prefWidth(getHeight())));
                    }
                    return maxArrowWidth;
                }

                @Override protected double computePrefWidth(double height) {
                    if (animationLock) return innerPrefWidth;
                    innerPrefWidth = getActualPrefWidth();
                    return innerPrefWidth;
                }

                public double getActualPrefWidth() {
                    double pw;
                    double maxArrowWidth = getArrowBtnWidth();
                    pw = 0.0F;
                    if (isShowTabsMenu()) {
                        pw += maxArrowWidth;
                    }
                    if (pw > 0) {
                        pw += snapSize(getInsets().getLeft()) + snapSize(getInsets().getRight());
                    }
                    return pw;
                }

                @Override protected double computePrefHeight(double width) {
                    double height = 0.0F;
                    if (isShowTabsMenu()) {
                        height = Math.max(height, snapSize(downArrowBtn.prefHeight(width)));
                    }
                    if (height > 0) {
                        height += snapSize(getInsets().getTop()) + snapSize(getInsets().getBottom());
                    }
                    return height;
                }

                @Override protected void layoutChildren() {
                    Side tabPosition = getSkinnable().getSide();
                    double x = 0.0F;
                    //padding.left;
                    double y = snapSize(getInsets().getTop());
                    double h = snapSize(getHeight()) - snapSize(getInsets().getTop()) + snapSize(getInsets().getBottom());
                    // when on the left or bottom, we need to position the tabs controls
                    // button such that it is the furtherest button away from the tabs.
                    if (tabPosition.equals(Side.BOTTOM) || tabPosition.equals(Side.LEFT)) {
                        x += positionTabsMenu(x, y, h, true);
                    } else {
                        x += positionTabsMenu(x, y, h, false);
                    }
                }

                private double positionTabsMenu(double x, double y, double h, boolean showSep) {
                    double newX = x;
                    if (isShowTabsMenu()) {
                        // DOWN ARROW BUTTON
                        positionArrow(downArrowBtn, downArrow, newX, y, maxArrowWidth, h);
                        newX += maxArrowWidth;
                    }
                    return newX;
                }

                private void positionArrow(Pane btn, StackPane arrow, double x, double y, double width, double height) {
                    btn.resize(width, height);
                    positionInArea(btn, x, y, width, height, /*baseline ignored*/0,
                            HPos.CENTER, VPos.CENTER);
                    // center arrow region within arrow button
                    double arrowWidth = snapSize(arrow.prefWidth(-1));
                    double arrowHeight = snapSize(arrow.prefHeight(-1));
                    arrow.resize(arrowWidth, arrowHeight);
                    positionInArea(arrow, snapSize(btn.getInsets().getLeft()), snapSize(btn.getInsets().getTop()),
                            width - snapSize(btn.getInsets().getLeft()) - snapSize(btn.getInsets().getRight()),
                            height - snapSize(btn.getInsets().getTop()) - snapSize(btn.getInsets().getBottom()),
                            /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
                }
            };
            inner.getChildren().add(downArrowBtn);

            getChildren().add(inner);

            tabPane.sideProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    Side tabPosition = getSkinnable().getSide();
                    downArrow.setRotate(tabPosition.equals(Side.BOTTOM)? 180.0F : 0.0F);
                }
            });
            tabPane.getTabs().addListener(new ListChangeListener<Tab>() {
                @Override public void onChanged(Change<? extends Tab> c) {
                    setupPopupMenu();
                }
            });
            showControlButtons = false;
            if (isShowTabsMenu()) {
                showControlButtons = true;
                requestLayout();
            }
            getProperties().put(ContextMenu.class, popup);
        }

        private boolean showTabsMenu = false;

        private void showTabsMenu(boolean value) {
            if (value && !showTabsMenu) {
                downArrowBtn.setVisible(true);
                showControlButtons = true;
                inner.requestLayout();
                tabHeaderArea.requestLayout();
            } else if (!value && showTabsMenu) {
                hideControlButtons();
            }
            this.showTabsMenu = value;
        }

        private boolean isShowTabsMenu() {
            return showTabsMenu;
        }

        private final DoubleProperty controlTabHeight = new SimpleDoubleProperty(this, "controlTabHeight");
        {
            controlTabHeight.addListener(new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    requestLayout();
                }
            });
        }
        // TODO: Maybe the getter and setter can be dropped completely after
        // turning controlTabHeight into a DoubleVariable?
        public double getControlTabHeight() {
            return controlTabHeight.get();
        }
        public void setControlTabHeight(double value) {
            controlTabHeight.set(value);
        }

        private boolean animationLock = false;
        private void setAnimationLock(boolean value) {
            animationLock = value;
            tabHeaderArea.requestLayout();
        }

//        FIXME this should be allowed, but the method is final on Node
//        @Override
//        public boolean isVisible() {
//            return getSkinnable().isShowScrollArrows() || getSkinnable().isShowTabsMenu();
//        }

        double maxArrowWidth;
        double innerPrefWidth;

        private double prefWidth;
        @Override protected double computePrefWidth(double height) {
            if (animationLock) {
                return prefWidth;
            }
            prefWidth = getActualPrefWidth(height);
            return prefWidth;
        }

        private double getActualPrefWidth(double height) {
            double pw = snapSize(inner.prefWidth(height));
            if (pw > 0) {
                pw += snapSize(getInsets().getLeft()) + snapSize(getInsets().getRight());
            }
            return pw;
        }

        @Override protected double computePrefHeight(double width) {
            return Math.max(getSkinnable().getTabMinHeight(), snapSize(inner.prefHeight(width))) +
                    snapSize(getInsets().getTop()) + snapSize(getInsets().getBottom());
        }

        @Override protected void layoutChildren() {
            double x = snapSize(getInsets().getLeft());
            double y = snapSize(getInsets().getTop());
            double w = snapSize(getWidth()) - snapSize(getInsets().getLeft()) + snapSize(getInsets().getRight());
            double h = snapSize(getHeight()) - snapSize(getInsets().getTop()) + snapSize(getInsets().getBottom());

            if (showControlButtons) {
                showControlButtons();
                showControlButtons = false;
            }

            inner.resize(w, h);
            positionInArea(inner, x, y, w, h, /*baseline ignored*/0, HPos.CENTER, VPos.BOTTOM);
        }

        private void showControlButtons() {
            double prefHeight = snapSize(prefHeight(-1));
            Timeline timeline = new Timeline();
            KeyValue keyValue = new KeyValue(controlTabHeight, prefHeight, Interpolator.EASE_OUT);

            setVisible(true);
            timeline.getKeyFrames().clear();
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(ANIMATION_SPEED), new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    if (popup == null) {
                        setupPopupMenu();
                    }
                    requestLayout();
                }
            }, keyValue));
            timeline.play();
        }

        private void hideControlButtons() {
            setAnimationLock(true);
            Timeline timeline = new Timeline();
            KeyValue keyValue = new KeyValue(controlTabHeight, 0.0, Interpolator.EASE_IN);

            timeline.getKeyFrames().clear();
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(ANIMATION_SPEED), new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    if (!isShowTabsMenu()) {
                        downArrowBtn.setVisible(false);
                    }
                    // If the scroll arrows or tab menu is still visible we don't want
                    // to hide it animate it back it.
                    if (isShowTabsMenu()) {
                        showControlButtons = true;
                    } else {
                        setVisible(false);
                        popup.getItems().clear();
                        popup = null;
                    }
                    setAnimationLock(false);
                    // This needs to be called when we are in the left tabPosition
                    // to allow for the clip offset to move properly (otherwise
                    // it jumps too early - before the animation is done).
                    requestLayout();
                }
            }, keyValue));
            timeline.play();
        }

        private void setupPopupMenu() {
            if (popup == null) {
                popup = new ContextMenu();
//                popup.setManaged(false);
            }
            popup.getItems().clear();
            ToggleGroup group = new ToggleGroup();
            ObservableList<RadioMenuItem> menuitems = FXCollections.<RadioMenuItem>observableArrayList();
            for (final Tab tab : getSkinnable().getTabs()) {
                TabMenuItem item = new TabMenuItem(tab);                
                item.setToggleGroup(group);
                item.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent t) {
                        getSkinnable().getSelectionModel().select(tab);
                    }
                });
                menuitems.add(item);
            }
            popup.getItems().addAll(menuitems);
        }
        
        private void showPopupMenu() {
            for (MenuItem mi: popup.getItems()) {
                TabMenuItem tmi = (TabMenuItem)mi;
                if (selectedTab.equals(tmi.getTab())) {
                    tmi.setSelected(true);
                    break;
                }
            }
            popup.show(downArrowBtn, Side.BOTTOM, 0, 0);            
        }
    } /* End TabControlButtons*/

    class TabMenuItem extends RadioMenuItem {
        Tab tab;
        public TabMenuItem(final Tab tab) {
            super(tab.getText(), TabPaneSkin.clone(tab.getGraphic()));                        
            this.tab = tab;
            setDisable(tab.isDisable());
            tab.disableProperty().addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable arg0) {
                    setDisable(tab.isDisable());
                }
            });                   
        }

        public Tab getTab() {
            return tab;
        }
    }
}
