/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import com.sun.javafx.scene.control.LambdaMultiplePropertyChangeListenerHandler;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.TabObservableList;
import com.sun.javafx.util.Utils;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javafx.css.converter.EnumConverter;
import com.sun.javafx.scene.control.behavior.TabPaneBehavior;

import static com.sun.javafx.scene.control.skin.resources.ControlResources.getString;

/**
 * Default skin implementation for the {@link TabPane} control.
 *
 * @see TabPane
 * @since 9
 */
public class TabPaneSkin extends SkinBase<TabPane> {

    /***************************************************************************
     *                                                                         *
     * Enums                                                                   *
     *                                                                         *
     **************************************************************************/

    private enum TabAnimation {
        NONE,
        GROW
        // In future we could add FADE, ...
    }

    private enum TabAnimationState {
        SHOWING, HIDING, NONE;
    }



    /***************************************************************************
     *                                                                         *
     * Static fields                                                           *
     *                                                                         *
     **************************************************************************/

    static int CLOSE_BTN_SIZE = 16;



    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private static final double ANIMATION_SPEED = 150;
    private static final int SPACER = 10;

    private TabHeaderArea tabHeaderArea;
    private ObservableList<TabContentRegion> tabContentRegions;
    private Rectangle clipRect;
    private Rectangle tabHeaderAreaClipRect;
    private Tab selectedTab;

    private final TabPaneBehavior behavior;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new TabPaneSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public TabPaneSkin(TabPane control) {
        super(control);

        // install default input map for the TabPane control
        this.behavior = new TabPaneBehavior(control);
//        control.setInputMap(behavior.getInputMap());

        clipRect = new Rectangle(control.getWidth(), control.getHeight());
        getSkinnable().setClip(clipRect);

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
        updateSelectionModel();

        registerChangeListener(control.selectionModelProperty(), e -> updateSelectionModel());
        registerChangeListener(control.sideProperty(), e -> updateTabPosition());
        registerChangeListener(control.widthProperty(), e -> {
            tabHeaderArea.invalidateScrollOffset();
            clipRect.setWidth(getSkinnable().getWidth());
        });
        registerChangeListener(control.heightProperty(), e -> {
            tabHeaderArea.invalidateScrollOffset();
            clipRect.setHeight(getSkinnable().getHeight());
        });

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

        initializeSwipeHandlers();
    }



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    private ObjectProperty<TabAnimation> openTabAnimation = new StyleableObjectProperty<TabAnimation>(TabAnimation.GROW) {
        @Override public CssMetaData<TabPane,TabAnimation> getCssMetaData() {
            return StyleableProperties.OPEN_TAB_ANIMATION;
        }

        @Override public Object getBean() {
            return TabPaneSkin.this;
        }

        @Override public String getName() {
            return "openTabAnimation";
        }
    };

    private ObjectProperty<TabAnimation> closeTabAnimation = new StyleableObjectProperty<TabAnimation>(TabAnimation.GROW) {
        @Override public CssMetaData<TabPane,TabAnimation> getCssMetaData() {
            return StyleableProperties.CLOSE_TAB_ANIMATION;
        }

        @Override public Object getBean() {
            return TabPaneSkin.this;
        }

        @Override public String getName() {
            return "closeTabAnimation";
        }
    };

    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        if (selectionModel != null) {
            selectionModel.selectedItemProperty().removeListener(weakSelectionChangeListener);
            selectionModel = null;
        }

        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        // The TabPane can only be as wide as it widest content width.
        double maxw = 0.0;
        for (TabContentRegion contentRegion: tabContentRegions) {
            maxw = Math.max(maxw, snapSizeX(contentRegion.prefWidth(-1)));
        }

        final boolean isHorizontal = isHorizontal();
        final double tabHeaderAreaSize = isHorizontal
                ? snapSizeX(tabHeaderArea.prefWidth(-1))
                : snapSizeY(tabHeaderArea.prefHeight(-1));

        double prefWidth = isHorizontal ?
                Math.max(maxw, tabHeaderAreaSize) : maxw + tabHeaderAreaSize;
        return snapSizeX(prefWidth) + rightInset + leftInset;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        // The TabPane can only be as high as it highest content height.
        double maxh = 0.0;
        for (TabContentRegion contentRegion: tabContentRegions) {
            maxh = Math.max(maxh, snapSizeY(contentRegion.prefHeight(-1)));
        }

        final boolean isHorizontal = isHorizontal();
        final double tabHeaderAreaSize = isHorizontal
                ? snapSizeY(tabHeaderArea.prefHeight(-1))
                : snapSizeX(tabHeaderArea.prefWidth(-1));

        double prefHeight = isHorizontal ?
                maxh + snapSizeY(tabHeaderAreaSize) : Math.max(maxh, tabHeaderAreaSize);
        return snapSizeY(prefHeight) + topInset + bottomInset;
    }

    /** {@inheritDoc} */
    @Override public double computeBaselineOffset(double topInset, double rightInset, double bottomInset, double leftInset) {
        Side tabPosition = getSkinnable().getSide();
        if (tabPosition == Side.TOP) {
            return tabHeaderArea.getBaselineOffset() + topInset;
        }
        return 0;
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        TabPane tabPane = getSkinnable();
        Side tabPosition = tabPane.getSide();

        double headerHeight = tabPosition.isHorizontal()
                ? snapSizeY(tabHeaderArea.prefHeight(-1))
                : snapSizeX(tabHeaderArea.prefHeight(-1));
        double tabsStartX = tabPosition.equals(Side.RIGHT)? x + w - headerHeight : x;
        double tabsStartY = tabPosition.equals(Side.BOTTOM)? y + h - headerHeight : y;

        final double leftInset = snappedLeftInset();
        final double topInset = snappedTopInset();

        if (tabPosition == Side.TOP) {
            tabHeaderArea.resize(w, headerHeight);
            tabHeaderArea.relocate(tabsStartX, tabsStartY);
            tabHeaderArea.getTransforms().clear();
            tabHeaderArea.getTransforms().add(new Rotate(getRotation(Side.TOP)));
        } else if (tabPosition == Side.BOTTOM) {
            tabHeaderArea.resize(w, headerHeight);
            tabHeaderArea.relocate(w + leftInset, tabsStartY - headerHeight);
            tabHeaderArea.getTransforms().clear();
            tabHeaderArea.getTransforms().add(new Rotate(getRotation(Side.BOTTOM), 0, headerHeight));
        } else if (tabPosition == Side.LEFT) {
            tabHeaderArea.resize(h, headerHeight);
            tabHeaderArea.relocate(tabsStartX + headerHeight, h - headerHeight + topInset);
            tabHeaderArea.getTransforms().clear();
            tabHeaderArea.getTransforms().add(new Rotate(getRotation(Side.LEFT), 0, headerHeight));
        } else if (tabPosition == Side.RIGHT) {
            tabHeaderArea.resize(h, headerHeight);
            tabHeaderArea.relocate(tabsStartX, y - headerHeight);
            tabHeaderArea.getTransforms().clear();
            tabHeaderArea.getTransforms().add(new Rotate(getRotation(Side.RIGHT), 0, headerHeight));
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

        if (tabPosition == Side.TOP) {
            contentStartX = x;
            contentStartY = y + headerHeight;
            if (isFloatingStyleClass()) {
                // This is to hide the top border content
                contentStartY -= 1;
            }
        } else if (tabPosition == Side.BOTTOM) {
            contentStartX = x;
            contentStartY = y + topInset;
            if (isFloatingStyleClass()) {
                // This is to hide the bottom border content
                contentStartY = 1 + topInset;
            }
        } else if (tabPosition == Side.LEFT) {
            contentStartX = x + headerHeight;
            contentStartY = y;
            if (isFloatingStyleClass()) {
                // This is to hide the left border content
                contentStartX -= 1;
            }
        } else if (tabPosition == Side.RIGHT) {
            contentStartX = x + leftInset;
            contentStartY = y;
            if (isFloatingStyleClass()) {
                // This is to hide the right border content
                contentStartX = 1 + leftInset;
            }
        }

        double contentWidth = w - (isHorizontal() ? 0 : headerHeight);
        double contentHeight = h - (isHorizontal() ? headerHeight: 0);

        for (int i = 0, max = tabContentRegions.size(); i < max; i++) {
            TabContentRegion tabContent = tabContentRegions.get(i);

            tabContent.setAlignment(Pos.TOP_LEFT);
            if (tabContent.getClip() != null) {
                ((Rectangle)tabContent.getClip()).setWidth(contentWidth);
                ((Rectangle)tabContent.getClip()).setHeight(contentHeight);
            }

            // we need to size all tabs, even if they aren't visible. For example,
            // see RT-29167
            tabContent.resize(contentWidth, contentHeight);
            tabContent.relocate(contentStartX, contentStartY);
        }
    }



    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private SelectionModel<Tab> selectionModel;
    private InvalidationListener selectionChangeListener = observable -> {
        tabHeaderArea.invalidateScrollOffset();
        selectedTab = getSkinnable().getSelectionModel().getSelectedItem();
        getSkinnable().requestLayout();
    };
    private WeakInvalidationListener weakSelectionChangeListener =
            new WeakInvalidationListener(selectionChangeListener);

    private void updateSelectionModel() {
        if (selectionModel != null) {
            selectionModel.selectedItemProperty().removeListener(weakSelectionChangeListener);
        }
        selectionModel = getSkinnable().getSelectionModel();
        if (selectionModel != null) {
            selectionModel.selectedItemProperty().addListener(weakSelectionChangeListener);
        }
    }

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
            imageview.imageProperty().bind(iv.imageProperty());
            return imageview;
        }
        if (n instanceof Label) {
            Label l = (Label)n;
            Label label = new Label(l.getText(), clone(l.getGraphic()));
            label.textProperty().bind(l.textProperty());
            return label;
        }
        return null;
    }

    private void removeTabs(List<? extends Tab> removedList) {
        for (final Tab tab : removedList) {
            stopCurrentAnimation(tab);
            // Animate the tab removal
            final TabHeaderSkin tabRegion = tabHeaderArea.getTabHeaderSkin(tab);
            if (tabRegion != null) {
                tabRegion.isClosing = true;

                tabRegion.removeListeners(tab);
                removeTabContent(tab);

                EventHandler<ActionEvent> cleanup = ae -> {
                    tabRegion.animationState = TabAnimationState.NONE;

                    tabHeaderArea.removeTab(tab);
                    tabHeaderArea.requestLayout();
                    if (getSkinnable().getTabs().isEmpty()) {
                        tabHeaderArea.setVisible(false);
                    }
                };

                if (closeTabAnimation.get() == TabAnimation.GROW) {
                    tabRegion.animationState = TabAnimationState.HIDING;
                    Timeline closedTabTimeline = tabRegion.currentAnimation =
                            createTimeline(tabRegion, Duration.millis(ANIMATION_SPEED), 0.0F, cleanup);
                    closedTabTimeline.play();
                } else {
                    cleanup.handle(null);
                }
            }
        }
    }

    private void stopCurrentAnimation(Tab tab) {
        final TabHeaderSkin tabRegion = tabHeaderArea.getTabHeaderSkin(tab);
        if (tabRegion != null) {
            // Execute the code immediately, don't wait for the animation to finish.
            Timeline timeline = tabRegion.currentAnimation;
            if (timeline != null && timeline.getStatus() == Animation.Status.RUNNING) {
                timeline.getOnFinished().handle(null);
                timeline.stop();
                tabRegion.currentAnimation = null;
            }
        }
    }

    private void addTabs(List<? extends Tab> addedList, int from) {
        int i = 0;

        // RT-39984: check if any other tabs are animating - they must be completed first.
        List<Node> headers = new ArrayList<>(tabHeaderArea.headersRegion.getChildren());
        for (Node n : headers) {
            TabHeaderSkin header = (TabHeaderSkin) n;
            if (header.animationState == TabAnimationState.HIDING) {
                stopCurrentAnimation(header.tab);
            }
        }
        // end of fix for RT-39984

        for (final Tab tab : addedList) {
            stopCurrentAnimation(tab); // Note that this must happen before addTab() call below
            // A new tab was added - animate it out
            if (!tabHeaderArea.isVisible()) {
                tabHeaderArea.setVisible(true);
            }
            int index = from + i++;
            tabHeaderArea.addTab(tab, index);
            addTabContent(tab);
            final TabHeaderSkin tabRegion = tabHeaderArea.getTabHeaderSkin(tab);
            if (tabRegion != null) {
                if (openTabAnimation.get() == TabAnimation.GROW) {
                    tabRegion.animationState = TabAnimationState.SHOWING;
                    tabRegion.animationTransition.setValue(0.0);
                    tabRegion.setVisible(true);
                    tabRegion.currentAnimation = createTimeline(tabRegion, Duration.millis(ANIMATION_SPEED), 1.0, event -> {
                        tabRegion.animationState = TabAnimationState.NONE;
                        tabRegion.setVisible(true);
                        tabRegion.inner.requestLayout();
                    });
                    tabRegion.currentAnimation.play();
                } else {
                    tabRegion.setVisible(true);
                    tabRegion.inner.requestLayout();
                }
            }
        }
    }

    private void initializeTabListener() {
        getSkinnable().getTabs().addListener((ListChangeListener<Tab>) c -> {
            List<Tab> tabsToRemove = new ArrayList<>();
            List<Tab> tabsToAdd = new ArrayList<>();

            while (c.next()) {
                if (c.wasPermutated()) {
                    if (dragState != DragState.REORDER) {
                        TabPane tabPane = getSkinnable();
                        List<Tab> tabs = tabPane.getTabs();

                        // tabs sorted : create list of permutated tabs.
                        // clear selection, set tab animation to NONE
                        // remove permutated tabs, add them back in correct order.
                        // restore old selection, and old tab animation states.
                        int size = c.getTo() - c.getFrom();
                        Tab selTab = tabPane.getSelectionModel().getSelectedItem();
                        List<Tab> permutatedTabs = new ArrayList<Tab>(size);
                        getSkinnable().getSelectionModel().clearSelection();

                        // save and set tab animation to none - as it is not a good idea
                        // to animate on the same data for open and close.
                        TabAnimation prevOpenAnimation = openTabAnimation.get();
                        TabAnimation prevCloseAnimation = closeTabAnimation.get();
                        openTabAnimation.set(TabAnimation.NONE);
                        closeTabAnimation.set(TabAnimation.NONE);
                        for (int i = c.getFrom(); i < c.getTo(); i++) {
                            permutatedTabs.add(tabs.get(i));
                        }

                        removeTabs(permutatedTabs);
                        addTabs(permutatedTabs, c.getFrom());
                        openTabAnimation.set(prevOpenAnimation);
                        closeTabAnimation.set(prevCloseAnimation);
                        getSkinnable().getSelectionModel().select(selTab);
                    }
                }

                if (c.wasRemoved()) {
                    tabsToRemove.addAll(c.getRemoved());
                }
                if (c.wasAdded()) {
                    tabsToAdd.addAll(c.getAddedSubList());
                }
            }

            // now only remove the tabs that are not in the tabsToAdd list
            tabsToRemove.removeAll(tabsToAdd);
            removeTabs(tabsToRemove);

            // and add in any new tabs (that we don't already have showing)
            List<Pair<Integer, TabHeaderSkin>> headersToMove = new ArrayList();
            if (!tabsToAdd.isEmpty()) {
                for (TabContentRegion tabContentRegion : tabContentRegions) {
                    Tab tab = tabContentRegion.getTab();
                    TabHeaderSkin tabHeader = tabHeaderArea.getTabHeaderSkin(tab);
                    if (!tabHeader.isClosing && tabsToAdd.contains(tabContentRegion.getTab())) {
                        tabsToAdd.remove(tabContentRegion.getTab());

                        // If a tab is removed and added back at the same time,
                        // then we must ensure that the index of tabHeader in
                        // headersRegion is same as index of tab in getTabs().
                        int tabIndex = getSkinnable().getTabs().indexOf(tab);
                        int tabHeaderIndex = tabHeaderArea.headersRegion.getChildren().indexOf(tabHeader);
                        if (tabIndex != tabHeaderIndex) {
                            headersToMove.add(new Pair(tabIndex, tabHeader));
                        }
                    }
                }

                if (!tabsToAdd.isEmpty()) {
                    addTabs(tabsToAdd, getSkinnable().getTabs().indexOf(tabsToAdd.get(0)));
                }
                for (Pair<Integer, TabHeaderSkin> move : headersToMove) {
                    tabHeaderArea.moveTab(move.getKey(), move.getValue());
                }
            }

            // Fix for RT-34692
            getSkinnable().requestLayout();
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

    private void updateTabPosition() {
        tabHeaderArea.invalidateScrollOffset();
        getSkinnable().applyCss();
        getSkinnable().requestLayout();
    }

    private Timeline createTimeline(final TabHeaderSkin tabRegion, final Duration duration, final double endValue, final EventHandler<ActionEvent> func) {
        Timeline timeline = new Timeline();
        timeline.setCycleCount(1);

        KeyValue keyValue = new KeyValue(tabRegion.animationTransition, endValue, Interpolator.LINEAR);
        timeline.getKeyFrames().clear();
        timeline.getKeyFrames().add(new KeyFrame(duration, keyValue));

        timeline.setOnFinished(func);
        return timeline;
    }

    private boolean isHorizontal() {
        Side tabPosition = getSkinnable().getSide();
        return Side.TOP.equals(tabPosition) || Side.BOTTOM.equals(tabPosition);
    }

    private void initializeSwipeHandlers() {
        if (Properties.IS_TOUCH_SUPPORTED) {
            getSkinnable().addEventHandler(SwipeEvent.SWIPE_LEFT, t -> {
                behavior.selectNextTab();
            });

            getSkinnable().addEventHandler(SwipeEvent.SWIPE_RIGHT, t -> {
                behavior.selectPreviousTab();
            });
        }
    }

    //TODO need to cache this.
    private boolean isFloatingStyleClass() {
        return getSkinnable().getStyleClass().contains(TabPane.STYLE_CLASS_FLOATING);
    }



    /***************************************************************************
     *                                                                         *
     * CSS                                                                     *
     *                                                                         *
     **************************************************************************/

   /*
    * Super-lazy instantiation pattern from Bill Pugh.
    */
   private static class StyleableProperties {
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        private final static CssMetaData<TabPane,TabAnimation> OPEN_TAB_ANIMATION =
                new CssMetaData<TabPane, TabPaneSkin.TabAnimation>("-fx-open-tab-animation",
                    new EnumConverter<TabAnimation>(TabAnimation.class), TabAnimation.GROW) {

            @Override public boolean isSettable(TabPane node) {
                return true;
            }

            @Override public StyleableProperty<TabAnimation> getStyleableProperty(TabPane node) {
                TabPaneSkin skin = (TabPaneSkin) node.getSkin();
                return (StyleableProperty<TabAnimation>)(WritableValue<TabAnimation>)skin.openTabAnimation;
            }
        };

        private final static CssMetaData<TabPane,TabAnimation> CLOSE_TAB_ANIMATION =
                new CssMetaData<TabPane, TabPaneSkin.TabAnimation>("-fx-close-tab-animation",
                    new EnumConverter<TabAnimation>(TabAnimation.class), TabAnimation.GROW) {

            @Override public boolean isSettable(TabPane node) {
                return true;
            }

            @Override public StyleableProperty<TabAnimation> getStyleableProperty(TabPane node) {
                TabPaneSkin skin = (TabPaneSkin) node.getSkin();
                return (StyleableProperty<TabAnimation>)(WritableValue<TabAnimation>)skin.closeTabAnimation;
            }
        };

        static {

           final List<CssMetaData<? extends Styleable, ?>> styleables =
               new ArrayList<CssMetaData<? extends Styleable, ?>>(SkinBase.getClassCssMetaData());
           styleables.add(OPEN_TAB_ANIMATION);
           styleables.add(CLOSE_TAB_ANIMATION);
           STYLEABLES = Collections.unmodifiableList(styleables);

        }
    }

    /**
     * Returns the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @return the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }



    /***************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/

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

        private boolean measureClosingTabs = false;

        private double scrollOffset;
        private boolean scrollOffsetDirty = true;

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
                        if (tabHeaderSkin.isVisible() && (measureClosingTabs || ! tabHeaderSkin.isClosing)) {
                            width += tabHeaderSkin.prefWidth(height);
                        }
                    }
                    return snapSize(width) + snappedLeftInset() + snappedRightInset();
                }

                @Override protected double computePrefHeight(double width) {
                    double height = 0.0F;
                    for (Node child : getChildren()) {
                        TabHeaderSkin tabHeaderSkin = (TabHeaderSkin)child;
                        height = Math.max(height, tabHeaderSkin.prefHeight(width));
                    }
                    return snapSize(height) + snappedTopInset() + snappedBottomInset();
                }

                @Override protected void layoutChildren() {
                    if (tabsFit()) {
                        setScrollOffset(0.0);
                    } else {
                        if (scrollOffsetDirty) {
                            ensureSelectedTabIsVisible();
                            scrollOffsetDirty = false;
                        }
                        // ensure there's no gap between last visible tab and trailing edge
                        validateScrollOffset();
                    }

                    Side tabPosition = getSkinnable().getSide();
                    double tabBackgroundHeight = snapSize(prefHeight(-1));
                    double tabX = (tabPosition.equals(Side.LEFT) || tabPosition.equals(Side.BOTTOM)) ?
                        snapSize(getWidth()) - getScrollOffset() : getScrollOffset();

                    updateHeaderClip();
                    for (Node node : getChildren()) {
                        TabHeaderSkin tabHeader = (TabHeaderSkin)node;

                        // size and position the header relative to the other headers
                        double tabHeaderPrefWidth = snapSize(tabHeader.prefWidth(-1) * tabHeader.animationTransition.get());
                        double tabHeaderPrefHeight = snapSize(tabHeader.prefHeight(-1));
                        tabHeader.resize(tabHeaderPrefWidth, tabHeaderPrefHeight);

                        // This ensures that the tabs are located in the correct position
                        // when there are tabs of differing heights.
                        double startY = tabPosition.equals(Side.BOTTOM) ?
                            0 : tabBackgroundHeight - tabHeaderPrefHeight - snappedBottomInset();
                        if (tabPosition.equals(Side.LEFT) || tabPosition.equals(Side.BOTTOM)) {
                            // build from the right
                            tabX -= tabHeaderPrefWidth;
                            if (dragState != DragState.REORDER ||
                                    (tabHeader != dragTabHeader && tabHeader != dropAnimHeader)) {
                                tabHeader.relocate(tabX, startY);
                            }
                        } else {
                            // build from the left
                            if (dragState != DragState.REORDER ||
                                    (tabHeader != dragTabHeader && tabHeader != dropAnimHeader)) {
                                tabHeader.relocate(tabX, startY);
                            }
                            tabX += tabHeaderPrefWidth;
                        }
                    }
                }

            };
            headersRegion.getStyleClass().setAll("headers-region");
            headersRegion.setClip(headerClip);
            setupReordering(headersRegion);

            headerBackground = new StackPane();
            headerBackground.getStyleClass().setAll("tab-header-background");

            int i = 0;
            for (Tab tab: tabPane.getTabs()) {
                addTab(tab, i++);
            }

            controlButtons = new TabControlButtons();
            controlButtons.setVisible(false);
            if (controlButtons.isVisible()) {
                controlButtons.setVisible(true);
            }
            getChildren().addAll(headerBackground, headersRegion, controlButtons);

            // support for mouse scroll of header area (for when the tabs exceed
            // the available space).
            // Scrolling the mouse wheel downwards results in the tabs scrolling left (i.e. exposing the right-most tabs)
            // Scrolling the mouse wheel upwards results in the tabs scrolling right (i.e. exposing th left-most tabs)
            addEventHandler(ScrollEvent.SCROLL, (ScrollEvent e) -> {
                Side side = getSkinnable().getSide();
                side = side == null ? Side.TOP : side;
                switch (side) {
                    default:
                    case TOP:
                    case BOTTOM:
                        setScrollOffset(scrollOffset + e.getDeltaY());
                        break;
                    case LEFT:
                    case RIGHT:
                        setScrollOffset(scrollOffset - e.getDeltaY());
                        break;
                }

            });
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

            measureClosingTabs = true;
            double headersPrefWidth = snapSize(headersRegion.prefWidth(-1));
            measureClosingTabs = false;

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

        private void addTab(Tab tab, int addToIndex) {
            TabHeaderSkin tabHeaderSkin = new TabHeaderSkin(tab);
            headersRegion.getChildren().add(addToIndex, tabHeaderSkin);
            invalidateScrollOffset();
        }

        private void removeTab(Tab tab) {
            TabHeaderSkin tabHeaderSkin = getTabHeaderSkin(tab);
            if (tabHeaderSkin != null) {
                headersRegion.getChildren().remove(tabHeaderSkin);
            }
            invalidateScrollOffset();
        }

        private void moveTab(int moveToIndex, TabHeaderSkin tabHeaderSkin) {
            if (moveToIndex != headersRegion.getChildren().indexOf(tabHeaderSkin)) {
                headersRegion.getChildren().remove(tabHeaderSkin);
                headersRegion.getChildren().add(moveToIndex, tabHeaderSkin);
            }
            invalidateScrollOffset();
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

        private boolean tabsFit() {
            double headerPrefWidth = snapSize(headersRegion.prefWidth(-1));
            double controlTabWidth = snapSize(controlButtons.prefWidth(-1));
            double visibleWidth = headerPrefWidth + controlTabWidth + firstTabIndent() + SPACER;
            return visibleWidth < getWidth();
        }

        private void ensureSelectedTabIsVisible() {
            // work out the visible width of the tab header
            double tabPaneWidth = snapSize(isHorizontal() ? getSkinnable().getWidth() : getSkinnable().getHeight());
            double controlTabWidth = snapSize(controlButtons.getWidth());
            double visibleWidth = tabPaneWidth - controlTabWidth - firstTabIndent() - SPACER;

            // and get where the selected tab is in the header area
            double offset = 0.0;
            double selectedTabOffset = 0.0;
            double selectedTabWidth = 0.0;
            for (Node node : headersRegion.getChildren()) {
                TabHeaderSkin tabHeader = (TabHeaderSkin)node;

                double tabHeaderPrefWidth = snapSize(tabHeader.prefWidth(-1));

                if (selectedTab != null && selectedTab.equals(tabHeader.getTab())) {
                    selectedTabOffset = offset;
                    selectedTabWidth = tabHeaderPrefWidth;
                }
                offset += tabHeaderPrefWidth;
            }

            final double scrollOffset = getScrollOffset();
            final double selectedTabStartX = selectedTabOffset;
            final double selectedTabEndX = selectedTabOffset + selectedTabWidth;

            final double visibleAreaEndX = visibleWidth;

            if (selectedTabStartX < -scrollOffset) {
                setScrollOffset(-selectedTabStartX);
            } else if (selectedTabEndX > (visibleAreaEndX - scrollOffset)) {
                setScrollOffset(visibleAreaEndX - selectedTabEndX);
            }
        }

        public double getScrollOffset() {
            return scrollOffset;
        }

        public void invalidateScrollOffset() {
            scrollOffsetDirty = true;
        }

        private void validateScrollOffset() {
            setScrollOffset(getScrollOffset());
        }

        private void setScrollOffset(double newScrollOffset) {
            // work out the visible width of the tab header
            double tabPaneWidth = snapSize(isHorizontal() ? getSkinnable().getWidth() : getSkinnable().getHeight());
            double controlTabWidth = snapSize(controlButtons.getWidth());
            double visibleWidth = tabPaneWidth - controlTabWidth - firstTabIndent() - SPACER;

            // measure the width of all tabs
            double offset = 0.0;
            for (Node node : headersRegion.getChildren()) {
                TabHeaderSkin tabHeader = (TabHeaderSkin)node;
                double tabHeaderPrefWidth = snapSize(tabHeader.prefWidth(-1));
                offset += tabHeaderPrefWidth;
            }

            double actualNewScrollOffset;

            if ((visibleWidth - newScrollOffset) > offset && newScrollOffset < 0) {
                // need to make sure the right-most tab is attached to the
                // right-hand side of the tab header (e.g. if the tab header area width
                // is expanded), and if it isn't modify the scroll offset to bring
                // it into line. See RT-35194 for a test case.
                actualNewScrollOffset = visibleWidth - offset;
            } else if (newScrollOffset > 0) {
                // need to prevent the left-most tab from becoming detached
                // from the left-hand side of the tab header.
                actualNewScrollOffset = 0;
            } else {
                actualNewScrollOffset = newScrollOffset;
            }

            if (Math.abs(actualNewScrollOffset - scrollOffset) > 0.001) {
                scrollOffset = actualNewScrollOffset;
                headersRegion.requestLayout();
            }
        }

        private double firstTabIndent() {
            switch (getSkinnable().getSide()) {
                case TOP:
                case BOTTOM:
                    return snappedLeftInset();
                case RIGHT:
                case LEFT:
                    return snappedTopInset();
                default:
                    return 0;
            }
        }

        @Override protected double computePrefWidth(double height) {
            double padding = isHorizontal() ?
                snappedLeftInset() + snappedRightInset() :
                snappedTopInset() + snappedBottomInset();
            return snapSize(headersRegion.prefWidth(height)) + controlButtons.prefWidth(height) +
                    firstTabIndent() + SPACER + padding;
        }

        @Override protected double computePrefHeight(double width) {
            double padding = isHorizontal() ?
                snappedTopInset() + snappedBottomInset() :
                snappedLeftInset() + snappedRightInset();
            return snapSize(headersRegion.prefHeight(-1)) + padding;
        }

        @Override public double getBaselineOffset() {
            if (getSkinnable().getSide() == Side.TOP) {
                return headersRegion.getBaselineOffset() + snappedTopInset();
            }
            return 0;
        }

        @Override protected void layoutChildren() {
            final double leftInset = snappedLeftInset();
            final double rightInset = snappedRightInset();
            final double topInset = snappedTopInset();
            final double bottomInset = snappedBottomInset();
            double w = snapSize(getWidth()) - (isHorizontal() ?
                    leftInset + rightInset : topInset + bottomInset);
            double h = snapSize(getHeight()) - (isHorizontal() ?
                    topInset + bottomInset : leftInset + rightInset);
            double tabBackgroundHeight = snapSize(prefHeight(-1));
            double headersPrefWidth = snapSize(headersRegion.prefWidth(-1));
            double headersPrefHeight = snapSize(headersRegion.prefHeight(-1));

            controlButtons.showTabsMenu(! tabsFit());

            updateHeaderClip();
            headersRegion.requestLayout();

            // RESIZE CONTROL BUTTONS
            double btnWidth = snapSize(controlButtons.prefWidth(-1));
            final double btnHeight = controlButtons.prefHeight(btnWidth);
            controlButtons.resize(btnWidth, btnHeight);

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
                startX = leftInset;
                startY = tabBackgroundHeight - headersPrefHeight - bottomInset;
                controlStartX = w - btnWidth + leftInset;
                controlStartY = snapSize(getHeight()) - btnHeight - bottomInset;
            } else if (tabPosition.equals(Side.RIGHT)) {
                startX = topInset;
                startY = tabBackgroundHeight - headersPrefHeight - leftInset;
                controlStartX = w - btnWidth + topInset;
                controlStartY = snapSize(getHeight()) - btnHeight - leftInset;
            } else if (tabPosition.equals(Side.BOTTOM)) {
                startX = snapSize(getWidth()) - headersPrefWidth - leftInset;
                startY = tabBackgroundHeight - headersPrefHeight - topInset;
                controlStartX = rightInset;
                controlStartY = snapSize(getHeight()) - btnHeight - topInset;
            } else if (tabPosition.equals(Side.LEFT)) {
                startX = snapSize(getWidth()) - headersPrefWidth - topInset;
                startY = tabBackgroundHeight - headersPrefHeight - rightInset;
                controlStartX = leftInset;
                controlStartY = snapSize(getHeight()) - btnHeight - rightInset;
            }
            if (headerBackground.isVisible()) {
                positionInArea(headerBackground, 0, 0,
                        snapSize(getWidth()), snapSize(getHeight()), /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
            }
            positionInArea(headersRegion, startX, startY, w, h, /*baseline ignored*/0, HPos.LEFT, VPos.CENTER);
            positionInArea(controlButtons, controlStartX, controlStartY, btnWidth, btnHeight,
                        /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
        }
    } /* End TabHeaderArea */




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
        private Tooltip oldTooltip;
        private Tooltip tooltip;
        private Rectangle clip;

        private boolean isClosing = false;

        private LambdaMultiplePropertyChangeListenerHandler listener = new LambdaMultiplePropertyChangeListenerHandler();

        private final ListChangeListener<String> styleClassListener = new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                getStyleClass().setAll(tab.getStyleClass());
            }
        };

        private final WeakListChangeListener<String> weakStyleClassListener =
                new WeakListChangeListener<>(styleClassListener);

        public TabHeaderSkin(final Tab tab) {
            getStyleClass().setAll(tab.getStyleClass());
            setId(tab.getId());
            setStyle(tab.getStyle());
            setAccessibleRole(AccessibleRole.TAB_ITEM);
            setViewOrder(1);

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
                @Override
                public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
                    switch (action) {
                        case FIRE: {
                            Tab tab = getTab();
                            if (behavior.canCloseTab(tab)) {
                                behavior.closeTab(tab);
                                setOnMousePressed(null);
                            }
                            break;
                        }
                        default: super.executeAccessibleAction(action, parameters);
                    }
                }
            };
            closeBtn.setAccessibleRole(AccessibleRole.BUTTON);
            closeBtn.setAccessibleText(getString("Accessibility.title.TabPane.CloseButton"));
            closeBtn.getStyleClass().setAll("tab-close-button");
            closeBtn.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent me) {
                    Tab tab = getTab();
                    if (me.getButton().equals(MouseButton.PRIMARY) && behavior.canCloseTab(tab)) {
                        behavior.closeTab(tab);
                        setOnMousePressed(null);
                        me.consume();
                    }
                }
            });

            updateGraphicRotation();

            final Region focusIndicator = new Region();
            focusIndicator.setMouseTransparent(true);
            focusIndicator.getStyleClass().add("focus-indicator");

            inner = new StackPane() {
                @Override protected void layoutChildren() {
                    final TabPane skinnable = getSkinnable();

                    final double paddingTop = snappedTopInset();
                    final double paddingRight = snappedRightInset();
                    final double paddingBottom = snappedBottomInset();
                    final double paddingLeft = snappedLeftInset();
                    final double w = getWidth() - (paddingLeft + paddingRight);
                    final double h = getHeight() - (paddingTop + paddingBottom);

                    final double prefLabelWidth = snapSize(label.prefWidth(-1));
                    final double prefLabelHeight = snapSize(label.prefHeight(-1));

                    final double closeBtnWidth = showCloseButton() ? snapSize(closeBtn.prefWidth(-1)) : 0;
                    final double closeBtnHeight = showCloseButton() ? snapSize(closeBtn.prefHeight(-1)) : 0;
                    final double minWidth = snapSize(skinnable.getTabMinWidth());
                    final double maxWidth = snapSize(skinnable.getTabMaxWidth());
                    final double maxHeight = snapSize(skinnable.getTabMaxHeight());

                    double labelAreaWidth = prefLabelWidth;
                    double labelWidth = prefLabelWidth;
                    double labelHeight = prefLabelHeight;

                    final double childrenWidth = labelAreaWidth + closeBtnWidth;
                    final double childrenHeight = Math.max(labelHeight, closeBtnHeight);

                    if (childrenWidth > maxWidth && maxWidth != Double.MAX_VALUE) {
                        labelAreaWidth = maxWidth - closeBtnWidth;
                        labelWidth = maxWidth - closeBtnWidth;
                    } else if (childrenWidth < minWidth) {
                        labelAreaWidth = minWidth - closeBtnWidth;
                    }

                    if (childrenHeight > maxHeight && maxHeight != Double.MAX_VALUE) {
                        labelHeight = maxHeight;
                    }

                    if (animationState != TabAnimationState.NONE) {
//                        if (prefWidth.getValue() < labelAreaWidth) {
//                            labelAreaWidth = prefWidth.getValue();
//                        }
                        labelAreaWidth *= animationTransition.get();
                        closeBtn.setVisible(false);
                    } else {
                        closeBtn.setVisible(showCloseButton());
                    }


                    label.resize(labelWidth, labelHeight);


                    double labelStartX = paddingLeft;

                    // If maxWidth is less than Double.MAX_VALUE, the user has
                    // clamped the max width, but we should
                    // position the close button at the end of the tab,
                    // which may not necessarily be the entire width of the
                    // provided max width.
                    double closeBtnStartX = (maxWidth < Double.MAX_VALUE ? Math.min(w, maxWidth) : w) - paddingRight - closeBtnWidth;

                    positionInArea(label, labelStartX, paddingTop, labelAreaWidth, h,
                            /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);

                    if (closeBtn.isVisible()) {
                        closeBtn.resize(closeBtnWidth, closeBtnHeight);
                        positionInArea(closeBtn, closeBtnStartX, paddingTop, closeBtnWidth, h,
                                /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
                    }

                    // Magic numbers regretfully introduced for RT-28944 (so that
                    // the focus rect appears as expected on Windows and Mac).
                    // In short we use the vPadding to shift the focus rect down
                    // into the content area (whereas previously it was being clipped
                    // on Windows, whilst it still looked fine on Mac). In the
                    // future we may want to improve this code to remove the
                    // magic number. Similarly, the hPadding differs on Mac.
                    final int vPadding = Utils.isMac() ? 2 : 3;
                    final int hPadding = Utils.isMac() ? 2 : 1;
                    focusIndicator.resizeRelocate(
                            paddingLeft - hPadding,
                            paddingTop + vPadding,
                            w + 2 * hPadding,
                            h - 2 * vPadding);
                }
            };
            inner.getStyleClass().add("tab-container");
            inner.setRotate(getSkinnable().getSide().equals(Side.BOTTOM) ? 180.0F : 0.0F);
            inner.getChildren().addAll(label, closeBtn, focusIndicator);

            getChildren().addAll(inner);

            tooltip = tab.getTooltip();
            if (tooltip != null) {
                Tooltip.install(this, tooltip);
                oldTooltip = tooltip;
            }

            listener.registerChangeListener(tab.closableProperty(), e -> {
                inner.requestLayout();
                requestLayout();
            });
            listener.registerChangeListener(tab.selectedProperty(), e -> {
                pseudoClassStateChanged(SELECTED_PSEUDOCLASS_STATE, tab.isSelected());
                // Need to request a layout pass for inner because if the width
                // and height didn't not change the label or close button may have
                // changed.
                inner.requestLayout();
                requestLayout();
            });
            listener.registerChangeListener(tab.textProperty(),e -> label.setText(getTab().getText()));
            listener.registerChangeListener(tab.graphicProperty(), e -> label.setGraphic(getTab().getGraphic()));
            listener.registerChangeListener(tab.tooltipProperty(), e -> {
                // uninstall the old tooltip
                if (oldTooltip != null) {
                    Tooltip.uninstall(this, oldTooltip);
                }
                tooltip = tab.getTooltip();
                if (tooltip != null) {
                    // install new tooltip and save as old tooltip.
                    Tooltip.install(this, tooltip);
                    oldTooltip = tooltip;
                }
            });
            listener.registerChangeListener(tab.disabledProperty(), e -> {
                updateTabDisabledState();
            });
            listener.registerChangeListener(tab.getTabPane().disabledProperty(), e -> {
                updateTabDisabledState();
            });
            listener.registerChangeListener(tab.styleProperty(), e -> setStyle(tab.getStyle()));

            tab.getStyleClass().addListener(weakStyleClassListener);

            listener.registerChangeListener(getSkinnable().tabClosingPolicyProperty(),e -> {
                inner.requestLayout();
                requestLayout();
            });
            listener.registerChangeListener(getSkinnable().sideProperty(),e -> {
                final Side side = getSkinnable().getSide();
                pseudoClassStateChanged(TOP_PSEUDOCLASS_STATE, (side == Side.TOP));
                pseudoClassStateChanged(RIGHT_PSEUDOCLASS_STATE, (side == Side.RIGHT));
                pseudoClassStateChanged(BOTTOM_PSEUDOCLASS_STATE, (side == Side.BOTTOM));
                pseudoClassStateChanged(LEFT_PSEUDOCLASS_STATE, (side == Side.LEFT));
                inner.setRotate(side == Side.BOTTOM ? 180.0F : 0.0F);
                if (getSkinnable().isRotateGraphic()) {
                    updateGraphicRotation();
                }
            });
            listener.registerChangeListener(getSkinnable().rotateGraphicProperty(), e -> updateGraphicRotation());
            listener.registerChangeListener(getSkinnable().tabMinWidthProperty(), e -> {
                requestLayout();
                getSkinnable().requestLayout();
            });
            listener.registerChangeListener(getSkinnable().tabMaxWidthProperty(), e -> {
                requestLayout();
                getSkinnable().requestLayout();
            });
            listener.registerChangeListener(getSkinnable().tabMinHeightProperty(), e -> {
                requestLayout();
                getSkinnable().requestLayout();
            });
            listener.registerChangeListener(getSkinnable().tabMaxHeightProperty(), e -> {
                requestLayout();
                getSkinnable().requestLayout();
            });

            getProperties().put(Tab.class, tab);
            getProperties().put(ContextMenu.class, tab.getContextMenu());

            setOnContextMenuRequested((ContextMenuEvent me) -> {
               if (getTab().getContextMenu() != null) {
                    getTab().getContextMenu().show(inner, me.getScreenX(), me.getScreenY());
                    me.consume();
                }
            });
            setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent me) {
                    Tab tab = getTab();
                    if (tab.isDisable()) {
                        return;
                    }
                    if (me.getButton().equals(MouseButton.MIDDLE)
                        || me.getButton().equals(MouseButton.PRIMARY)) {

                        if (tab.getContextMenu() != null
                            && tab.getContextMenu().isShowing()) {
                            tab.getContextMenu().hide();
                        }
                    }
                    if (me.getButton().equals(MouseButton.MIDDLE)) {
                        if (showCloseButton()) {
                            if (behavior.canCloseTab(tab)) {
                                removeListeners(tab);
                                behavior.closeTab(tab);
                            }
                        }
                    } else if (me.getButton().equals(MouseButton.PRIMARY)) {
                        behavior.selectTab(tab);
                    }
                }
            });

            // initialize pseudo-class state
            pseudoClassStateChanged(SELECTED_PSEUDOCLASS_STATE, tab.isSelected());
            pseudoClassStateChanged(DISABLED_PSEUDOCLASS_STATE, tab.isDisabled());
            final Side side = getSkinnable().getSide();
            pseudoClassStateChanged(TOP_PSEUDOCLASS_STATE, (side == Side.TOP));
            pseudoClassStateChanged(RIGHT_PSEUDOCLASS_STATE, (side == Side.RIGHT));
            pseudoClassStateChanged(BOTTOM_PSEUDOCLASS_STATE, (side == Side.BOTTOM));
            pseudoClassStateChanged(LEFT_PSEUDOCLASS_STATE, (side == Side.LEFT));
        }

        private void updateTabDisabledState() {
            pseudoClassStateChanged(DISABLED_PSEUDOCLASS_STATE, tab.isDisabled());
            inner.requestLayout();
            requestLayout();
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

        private final DoubleProperty animationTransition = new SimpleDoubleProperty(this, "animationTransition", 1.0) {
            @Override protected void invalidated() {
                requestLayout();
            }
        };

        private void removeListeners(Tab tab) {
            listener.dispose();
            inner.getChildren().clear();
            getChildren().clear();
            setOnContextMenuRequested(null);
            setOnMousePressed(null);
        }

        private TabAnimationState animationState = TabAnimationState.NONE;
        private Timeline currentAnimation;

        @Override protected double computePrefWidth(double height) {
//            if (animating) {
//                return prefWidth.getValue();
//            }
            double minWidth = snapSize(getSkinnable().getTabMinWidth());
            double maxWidth = snapSize(getSkinnable().getTabMaxWidth());
            double paddingRight = snappedRightInset();
            double paddingLeft = snappedLeftInset();
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
//            prefWidth.setValue(tmpPrefWidth);
            return tmpPrefWidth;
        }

        @Override protected double computePrefHeight(double width) {
            double minHeight = snapSize(getSkinnable().getTabMinHeight());
            double maxHeight = snapSize(getSkinnable().getTabMaxHeight());
            double paddingTop = snappedTopInset();
            double paddingBottom = snappedBottomInset();
            double tmpPrefHeight = snapSize(label.prefHeight(width));

            if (tmpPrefHeight > maxHeight) {
                tmpPrefHeight = maxHeight;
            } else if (tmpPrefHeight < minHeight) {
                tmpPrefHeight = minHeight;
            }
            tmpPrefHeight += paddingTop + paddingBottom;
            return tmpPrefHeight;
        }

        @Override protected void layoutChildren() {
            double w = (snapSize(getWidth()) - snappedRightInset() - snappedLeftInset()) * animationTransition.getValue();
            inner.resize(w, snapSize(getHeight()) - snappedTopInset() - snappedBottomInset());
            inner.relocate(snappedLeftInset(), snappedTopInset());
        }

        @Override protected void setWidth(double value) {
            super.setWidth(value);
            clip.setWidth(value);
        }

        @Override protected void setHeight(double value) {
            super.setHeight(value);
            clip.setHeight(value);
        }

        /** {@inheritDoc} */
        @Override
        public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
            switch (attribute) {
                case TEXT: return getTab().getText();
                case SELECTED: return selectedTab == getTab();
                default: return super.queryAccessibleAttribute(attribute, parameters);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
            switch (action) {
                case REQUEST_FOCUS:
                    getSkinnable().getSelectionModel().select(getTab());
                    break;
                default: super.executeAccessibleAction(action, parameters);
            }
        }

    } /* End TabHeaderSkin */

    private static final PseudoClass SELECTED_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("selected");
    private static final PseudoClass TOP_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("top");
    private static final PseudoClass BOTTOM_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("bottom");
    private static final PseudoClass LEFT_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("left");
    private static final PseudoClass RIGHT_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("right");
    private static final PseudoClass DISABLED_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("disabled");


    /**************************************************************************
     *
     * TabContentRegion: each tab has one to contain the tab's content node
     *
     **************************************************************************/
    static class TabContentRegion extends StackPane {

        private Tab tab;

        private InvalidationListener tabContentListener = valueModel -> {
            updateContent();
        };
        private InvalidationListener tabSelectedListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                setVisible(tab.isSelected());
            }
        };

        private WeakInvalidationListener weakTabContentListener =
                new WeakInvalidationListener(tabContentListener);
        private WeakInvalidationListener weakTabSelectedListener =
                new WeakInvalidationListener(tabSelectedListener);

        public Tab getTab() {
            return tab;
        }

        public TabContentRegion(Tab tab) {
            getStyleClass().setAll("tab-content-area");
            setManaged(false);
            this.tab = tab;
            updateContent();
            setVisible(tab.isSelected());

            tab.selectedProperty().addListener(weakTabSelectedListener);
            tab.contentProperty().addListener(weakTabContentListener);
        }

        private void updateContent() {
            Node newContent = getTab().getContent();
            if (newContent == null) {
                getChildren().clear();
            } else {
                getChildren().setAll(newContent);
            }
        }

        private void removeListeners(Tab tab) {
            tab.selectedProperty().removeListener(weakTabSelectedListener);
            tab.contentProperty().removeListener(weakTabContentListener);
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
            downArrowBtn.setOnMouseClicked(me -> {
                showPopupMenu();
            });

            setupPopupMenu();

            inner = new StackPane() {
                @Override protected double computePrefWidth(double height) {
                    double pw;
                    double maxArrowWidth = ! isShowTabsMenu() ? 0 : snapSize(downArrow.prefWidth(getHeight())) + snapSize(downArrowBtn.prefWidth(getHeight()));
                    pw = 0.0F;
                    if (isShowTabsMenu()) {
                        pw += maxArrowWidth;
                    }
                    if (pw > 0) {
                        pw += snappedLeftInset() + snappedRightInset();
                    }
                    return pw;
                }

                @Override protected double computePrefHeight(double width) {
                    double height = 0.0F;
                    if (isShowTabsMenu()) {
                        height = Math.max(height, snapSize(downArrowBtn.prefHeight(width)));
                    }
                    if (height > 0) {
                        height += snappedTopInset() + snappedBottomInset();
                    }
                    return height;
                }

                @Override protected void layoutChildren() {
                    if (isShowTabsMenu()) {
                        double x = 0;
                        double y = snappedTopInset();
                        double w = snapSize(getWidth()) - x + snappedLeftInset();
                        double h = snapSize(getHeight()) - y + snappedBottomInset();
                        positionArrow(downArrowBtn, downArrow, x, y, w, h);
                    }
                }

                private void positionArrow(Pane btn, StackPane arrow, double x, double y, double width, double height) {
                    btn.resize(width, height);
                    positionInArea(btn, x, y, width, height, /*baseline ignored*/0,
                            HPos.CENTER, VPos.CENTER);
                    // center arrow region within arrow button
                    double arrowWidth = snapSize(arrow.prefWidth(-1));
                    double arrowHeight = snapSize(arrow.prefHeight(-1));
                    arrow.resize(arrowWidth, arrowHeight);
                    positionInArea(arrow, btn.snappedLeftInset(), btn.snappedTopInset(),
                            width - btn.snappedLeftInset() - btn.snappedRightInset(),
                            height - btn.snappedTopInset() - btn.snappedBottomInset(),
                            /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
                }
            };
            inner.getStyleClass().add("container");
            inner.getChildren().add(downArrowBtn);

            getChildren().add(inner);

            tabPane.sideProperty().addListener(valueModel -> {
                Side tabPosition = getSkinnable().getSide();
                downArrow.setRotate(tabPosition.equals(Side.BOTTOM)? 180.0F : 0.0F);
            });
            tabPane.getTabs().addListener((ListChangeListener<Tab>) c -> setupPopupMenu());
            showControlButtons = false;
            if (isShowTabsMenu()) {
                showControlButtons = true;
                requestLayout();
            }
            getProperties().put(ContextMenu.class, popup);
        }

        private boolean showTabsMenu = false;

        private void showTabsMenu(boolean value) {
            final boolean wasTabsMenuShowing = isShowTabsMenu();
            this.showTabsMenu = value;

            if (showTabsMenu && !wasTabsMenuShowing) {
                downArrowBtn.setVisible(true);
                showControlButtons = true;
                inner.requestLayout();
                tabHeaderArea.requestLayout();
            } else if (!showTabsMenu && wasTabsMenuShowing) {
                hideControlButtons();
            }
        }

        private boolean isShowTabsMenu() {
            return showTabsMenu;
        }

        @Override protected double computePrefWidth(double height) {
            double pw = snapSize(inner.prefWidth(height));
            if (pw > 0) {
                pw += snappedLeftInset() + snappedRightInset();
            }
            return pw;
        }

        @Override protected double computePrefHeight(double width) {
            return Math.max(getSkinnable().getTabMinHeight(), snapSize(inner.prefHeight(width))) +
                    snappedTopInset() + snappedBottomInset();
        }

        @Override protected void layoutChildren() {
            double x = snappedLeftInset();
            double y = snappedTopInset();
            double w = snapSize(getWidth()) - x + snappedRightInset();
            double h = snapSize(getHeight()) - y + snappedBottomInset();

            if (showControlButtons) {
                showControlButtons();
                showControlButtons = false;
            }

            inner.resize(w, h);
            positionInArea(inner, x, y, w, h, /*baseline ignored*/0, HPos.CENTER, VPos.BOTTOM);
        }

        private void showControlButtons() {
            setVisible(true);
            if (popup == null) {
                setupPopupMenu();
            }
        }

        private void hideControlButtons() {
            // If the scroll arrows or tab menu is still visible we don't want
            // to hide it animate it back it.
            if (isShowTabsMenu()) {
                showControlButtons = true;
            } else {
                setVisible(false);
                clearPopupMenu();
                popup = null;
            }

            // This needs to be called when we are in the left tabPosition
            // to allow for the clip offset to move properly (otherwise
            // it jumps too early - before the animation is done).
            requestLayout();
        }

        private void setupPopupMenu() {
            if (popup == null) {
                popup = new ContextMenu();
            }
            clearPopupMenu();
            ToggleGroup group = new ToggleGroup();
            ObservableList<RadioMenuItem> menuitems = FXCollections.<RadioMenuItem>observableArrayList();
            for (final Tab tab : getSkinnable().getTabs()) {
                TabMenuItem item = new TabMenuItem(tab);
                item.setToggleGroup(group);
                item.setOnAction(t -> getSkinnable().getSelectionModel().select(tab));
                menuitems.add(item);
            }
            popup.getItems().addAll(menuitems);
        }

        private void clearPopupMenu() {
            for (MenuItem item : popup.getItems()) {
                ((TabMenuItem) item).dispose();
            }
            popup.getItems().clear();
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

    static class TabMenuItem extends RadioMenuItem {
        Tab tab;

        private InvalidationListener disableListener = new InvalidationListener() {
            @Override public void invalidated(Observable o) {
                setDisable(tab.isDisable());
            }
        };

        private WeakInvalidationListener weakDisableListener =
                new WeakInvalidationListener(disableListener);

        public TabMenuItem(final Tab tab) {
            super(tab.getText(), TabPaneSkin.clone(tab.getGraphic()));
            this.tab = tab;
            setDisable(tab.isDisable());
            tab.disableProperty().addListener(weakDisableListener);
            textProperty().bind(tab.textProperty());
        }

        public Tab getTab() {
            return tab;
        }

        public void dispose() {
            textProperty().unbind();
            tab.disableProperty().removeListener(weakDisableListener);
            tab = null;
        }
    }

    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case FOCUS_ITEM: return tabHeaderArea.getTabHeaderSkin(selectedTab);
            case ITEM_COUNT: return tabHeaderArea.headersRegion.getChildren().size();
            case ITEM_AT_INDEX: {
                Integer index = (Integer)parameters[0];
                if (index == null) return null;
                return tabHeaderArea.headersRegion.getChildren().get(index);
            }
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    // --------------------------
    // Tab Reordering
    // --------------------------
    private enum DragState {
        NONE,
        START,
        REORDER
    }
    private EventHandler<MouseEvent> headerDraggedHandler = this::handleHeaderDragged;
    private EventHandler<MouseEvent> headerMousePressedHandler = this::handleHeaderMousePressed;
    private EventHandler<MouseEvent> headerMouseReleasedHandler = this::handleHeaderMouseReleased;

    private int dragTabHeaderStartIndex;
    private int dragTabHeaderIndex;
    private TabHeaderSkin dragTabHeader;
    private TabHeaderSkin dropTabHeader;
    private StackPane headersRegion;
    private DragState dragState;
    private final int MIN_TO_MAX = 1;
    private final int MAX_TO_MIN = -1;
    private int xLayoutDirection;
    private double dragEventPrevLoc;
    private int prevDragDirection = MIN_TO_MAX;
    private final double DRAG_DIST_THRESHOLD = 0.75;

    // Reordering Animation
    private final double ANIM_DURATION = 120;
    private TabHeaderSkin dropAnimHeader;
    private double dropHeaderSourceX;
    private double dropHeaderTransitionX;
    private final Animation dropHeaderAnim = new Transition() {
        {
            setInterpolator(Interpolator.EASE_BOTH);
            setCycleDuration(Duration.millis(ANIM_DURATION));
            setOnFinished(event -> {
                completeHeaderReordering();
            });
        }
        protected void interpolate(double frac) {
            dropAnimHeader.setLayoutX(dropHeaderSourceX + dropHeaderTransitionX * frac);
        }
    };
    private double dragHeaderDestX;
    private double dragHeaderSourceX;
    private double dragHeaderTransitionX;
    private final Animation dragHeaderAnim = new Transition() {
        {
            setInterpolator(Interpolator.EASE_OUT);
            setCycleDuration(Duration.millis(ANIM_DURATION));
            setOnFinished(event -> {
                reorderTabs();
                resetDrag();
            });
        }
        protected void interpolate(double frac) {
            dragTabHeader.setLayoutX(dragHeaderSourceX + dragHeaderTransitionX * frac);
        }
    };

    // Helper methods for managing the listeners based on TabDragPolicy.
    private void addReorderListeners(Node n) {
        n.addEventHandler(MouseEvent.MOUSE_PRESSED, headerMousePressedHandler);
        n.addEventHandler(MouseEvent.MOUSE_RELEASED, headerMouseReleasedHandler);
        n.addEventHandler(MouseEvent.MOUSE_DRAGGED, headerDraggedHandler);
    }

    private void removeReorderListeners(Node n) {
        n.removeEventHandler(MouseEvent.MOUSE_PRESSED, headerMousePressedHandler);
        n.removeEventHandler(MouseEvent.MOUSE_RELEASED, headerMouseReleasedHandler);
        n.removeEventHandler(MouseEvent.MOUSE_DRAGGED, headerDraggedHandler);
    }

    private ListChangeListener childListener = new ListChangeListener<Node>() {
        public void onChanged(Change<? extends Node> change) {
            while (change.next()) {
                if (change.wasAdded()) {
                    for(Node n : change.getAddedSubList()) {
                        addReorderListeners(n);
                    }
                }
                if (change.wasRemoved()) {
                    for(Node n : change.getRemoved()) {
                        removeReorderListeners(n);
                    }
                }
            }
        }
    };

    private void updateListeners() {
        if (getSkinnable().getTabDragPolicy() == TabDragPolicy.FIXED ||
                getSkinnable().getTabDragPolicy() == null) {
            for (Node n : headersRegion.getChildren()) {
                removeReorderListeners(n);
            }
            headersRegion.getChildren().removeListener(childListener);
        } else if (getSkinnable().getTabDragPolicy() == TabDragPolicy.REORDER) {
            for (Node n : headersRegion.getChildren()) {
                addReorderListeners(n);
            }
            headersRegion.getChildren().addListener(childListener);
        }
    }

    private void setupReordering(StackPane headersRegion) {
        dragState = DragState.NONE;
        this.headersRegion = headersRegion;
        updateListeners();
        getSkinnable().tabDragPolicyProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != newValue) {
                updateListeners();
            }
        });
    }

    private void handleHeaderMousePressed(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY)) {
            ((StackPane) event.getSource()).setMouseTransparent(true);
            startDrag(event);
        }
    }

    private void handleHeaderMouseReleased(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY)) {
            ((StackPane) event.getSource()).setMouseTransparent(false);
            stopDrag();
            event.consume();
        }
    }

    private void handleHeaderDragged(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY)) {
            performDrag(event);
        }
    }

    private double getDragDelta(double curr, double prev) {
        if (getSkinnable().getSide().equals(Side.TOP) ||
                getSkinnable().getSide().equals(Side.RIGHT)) {
            return curr - prev;
        } else {
            return prev - curr;
        }
    }

    private int deriveTabHeaderLayoutXDirection() {
        if (getSkinnable().getSide().equals(Side.TOP) ||
                getSkinnable().getSide().equals(Side.RIGHT)) {
            // TabHeaderSkin are laid out in left to right direction inside headersRegion
            return MIN_TO_MAX;
        }
        // TabHeaderSkin are laid out in right to left direction inside headersRegion
        return MAX_TO_MIN;
    }

    private void performDrag(MouseEvent event) {
        if (dragState == DragState.NONE) {
            return;
        }
        int dragDirection;
        double dragHeaderNewLayoutX;
        Bounds dragHeaderBounds;
        Bounds dropHeaderBounds;
        double draggedDist;
        double mouseCurrentLoc = getHeaderRegionLocalX(event);
        double dragDelta = getDragDelta(mouseCurrentLoc, dragEventPrevLoc);

        if (dragDelta > 0) {
            // Dragging the tab header towards higher indexed tab headers inside headersRegion.
            dragDirection = MIN_TO_MAX;
        } else {
            // Dragging the tab header towards lower indexed tab headers inside headersRegion.
            dragDirection = MAX_TO_MIN;
        }
        // Stop dropHeaderAnim if direction of drag is changed
        if (prevDragDirection != dragDirection) {
            stopAnim(dropHeaderAnim);
            prevDragDirection = dragDirection;
        }

        dragHeaderNewLayoutX = dragTabHeader.getLayoutX() + xLayoutDirection * dragDelta;

        if (dragHeaderNewLayoutX >= 0 &&
                dragHeaderNewLayoutX + dragTabHeader.getWidth() <= headersRegion.getWidth()) {

            dragState = DragState.REORDER;
            dragTabHeader.setLayoutX(dragHeaderNewLayoutX);
            dragHeaderBounds = dragTabHeader.getBoundsInParent();

            if (dragDirection == MIN_TO_MAX) {
                // Dragging the tab header towards higher indexed tab headers
                // Last tab header can not be dragged outside headersRegion.

                // When the mouse is moved too fast, sufficient number of events
                // are not generated. Hence it is required to check all possible
                // headers to be reordered.
                for (int i = dragTabHeaderIndex + 1; i < headersRegion.getChildren().size(); i++) {
                    dropTabHeader = (TabHeaderSkin) headersRegion.getChildren().get(i);

                    // dropTabHeader should not be already reordering.
                    if (dropAnimHeader != dropTabHeader) {
                        dropHeaderBounds = dropTabHeader.getBoundsInParent();

                        if (xLayoutDirection == MIN_TO_MAX) {
                            draggedDist = dragHeaderBounds.getMaxX() - dropHeaderBounds.getMinX();
                        } else {
                            draggedDist = dropHeaderBounds.getMaxX() - dragHeaderBounds.getMinX();
                        }

                        // A tab header is reordered when dragged tab header crosses DRAG_DIST_THRESHOLD% of next tab header's width.
                        if (draggedDist > dropHeaderBounds.getWidth() * DRAG_DIST_THRESHOLD) {
                            stopAnim(dropHeaderAnim);
                            // Distance by which tab header should be animated.
                            dropHeaderTransitionX = xLayoutDirection * -dragHeaderBounds.getWidth();
                            if (xLayoutDirection == MIN_TO_MAX) {
                                dragHeaderDestX = dropHeaderBounds.getMaxX() - dragHeaderBounds.getWidth();
                            } else {
                                dragHeaderDestX = dropHeaderBounds.getMinX();
                            }
                            startHeaderReorderingAnim();
                        } else {
                            break;
                        }
                    }
                }
            } else {
                // dragDirection is MAX_TO_MIN
                // Dragging the tab header towards lower indexed tab headers.
                // First tab header can not be dragged outside headersRegion.

                // When the mouse is moved too fast, sufficient number of events
                // are not generated. Hence it is required to check all possible
                // tab headers to be reordered.
                for (int i = dragTabHeaderIndex - 1; i >= 0; i--) {
                    dropTabHeader = (TabHeaderSkin) headersRegion.getChildren().get(i);

                    // dropTabHeader should not be already reordering.
                    if (dropAnimHeader != dropTabHeader) {
                        dropHeaderBounds = dropTabHeader.getBoundsInParent();

                        if (xLayoutDirection == MIN_TO_MAX) {
                            draggedDist = dropHeaderBounds.getMaxX() - dragHeaderBounds.getMinX();
                        } else {
                            draggedDist = dragHeaderBounds.getMaxX() - dropHeaderBounds.getMinX();
                        }

                        // A tab header is reordered when dragged tab crosses DRAG_DIST_THRESHOLD% of next tab header's width.
                        if (draggedDist > dropHeaderBounds.getWidth() * DRAG_DIST_THRESHOLD) {
                            stopAnim(dropHeaderAnim);
                            // Distance by which tab header should be animated.
                            dropHeaderTransitionX = xLayoutDirection * dragHeaderBounds.getWidth();
                            if (xLayoutDirection == MIN_TO_MAX) {
                                dragHeaderDestX = dropHeaderBounds.getMinX();
                            } else {
                                dragHeaderDestX = dropHeaderBounds.getMaxX() - dragHeaderBounds.getWidth();
                            }
                            startHeaderReorderingAnim();
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        dragEventPrevLoc = mouseCurrentLoc;
        event.consume();
    }

    private void startDrag(MouseEvent event) {
        // Stop the animations if any are running from previous reorder.
        stopAnim(dropHeaderAnim);
        stopAnim(dragHeaderAnim);

        dragTabHeader = (TabHeaderSkin) event.getSource();
        if (dragTabHeader != null) {
            dragState = DragState.START;
            xLayoutDirection = deriveTabHeaderLayoutXDirection();
            dragEventPrevLoc = getHeaderRegionLocalX(event);
            dragTabHeaderIndex = headersRegion.getChildren().indexOf(dragTabHeader);
            dragTabHeaderStartIndex = dragTabHeaderIndex;
            dragTabHeader.setViewOrder(0);
            dragHeaderDestX = dragTabHeader.getLayoutX();
        }
    }

    private double getHeaderRegionLocalX(MouseEvent ev) {
        // The event is converted to tab header's parent i.e. headersRegion's local space.
        // This will provide a value of X co-ordinate with all transformations of TabPane
        // and transformations of all nodes in the TabPane's parent hierarchy.
        Point2D sceneToLocalHR = headersRegion.sceneToLocal(ev.getSceneX(), ev.getSceneY());
        return sceneToLocalHR.getX();
    }

    private void stopDrag() {
        if (dragState == DragState.START) {
            // No drag action was performed.
            resetDrag();
        } else if (dragState == DragState.REORDER) {
            // Animate tab header being dragged to its final position.
            dragHeaderSourceX = dragTabHeader.getLayoutX();
            dragHeaderTransitionX = dragHeaderDestX - dragHeaderSourceX;
            dragHeaderAnim.playFromStart();
        }
        tabHeaderArea.invalidateScrollOffset();
    }

    private void reorderTabs() {
        if (dragTabHeaderIndex != dragTabHeaderStartIndex) {
            ((TabObservableList<Tab>) getSkinnable().getTabs()).reorder(
                    getSkinnable().getTabs().get(dragTabHeaderStartIndex),
                    getSkinnable().getTabs().get(dragTabHeaderIndex));
        }
    }

    private void resetDrag() {
        dragState = DragState.NONE;
        dragTabHeader.setViewOrder(1);
        dragTabHeader = null;
        dropTabHeader = null;
        headersRegion.requestLayout();
    }

    // Animate tab header being dropped-on to its new position.
    private void startHeaderReorderingAnim() {
        dropAnimHeader = dropTabHeader;
        dropHeaderSourceX = dropAnimHeader.getLayoutX();
        dropHeaderAnim.playFromStart();
    }

    // Remove dropAnimHeader and add at the index position of dragTabHeader.
    private void completeHeaderReordering() {
        if (dropAnimHeader != null) {
            headersRegion.getChildren().remove(dropAnimHeader);
            headersRegion.getChildren().add(dragTabHeaderIndex, dropAnimHeader);
            dropAnimHeader = null;
            headersRegion.requestLayout();
            dragTabHeaderIndex = headersRegion.getChildren().indexOf(dragTabHeader);
        }
    }

    // Helper method to stop an animation.
    private void stopAnim(Animation anim) {
        if (anim.getStatus() == Animation.Status.RUNNING) {
            anim.getOnFinished().handle(null);
            anim.stop();
        }
    }

    // For testing purpose.
    ContextMenu test_getTabsMenu() {
        return tabHeaderArea.controlButtons.popup;
    }

    void test_disableAnimations() {
        closeTabAnimation.set(TabAnimation.NONE);
        openTabAnimation.set(TabAnimation.NONE);
    }

    double test_getHeaderAreaScrollOffset() {
        return tabHeaderArea.getScrollOffset();
    }

    void test_setHeaderAreaScrollOffset(double offset) {
        tabHeaderArea.setScrollOffset(offset);
    }

    boolean test_isTabsFit() {
        return tabHeaderArea.tabsFit();
    }
}
