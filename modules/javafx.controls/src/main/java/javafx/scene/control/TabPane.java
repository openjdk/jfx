/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sun.javafx.collections.UnmodifiableListSet;
import com.sun.javafx.scene.control.TabObservableList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Side;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.css.StyleableDoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;

import javafx.css.converter.SizeConverter;
import javafx.scene.control.skin.TabPaneSkin;

import javafx.beans.DefaultProperty;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Node;

/**
 * A control that allows switching between a group of {@link Tab Tabs}.  Only one tab
 * is visible at a time. Tabs are added to the TabPane by using the {@link #getTabs}.
 *
 * <p>Tabs in a TabPane can be positioned at any of the four sides by specifying the
 * {@link Side}. </p>
 *
 * <p>A TabPane has two modes floating or recessed.  Applying the styleclass STYLE_CLASS_FLOATING
 * will change the TabPane mode to floating.</p>
 *
 * <p>The tabs width and height can be set to a specific size by
 * setting the min and max for height and width.  TabPane default width will be
 * determined by the largest content width in the TabPane.  This is the same for the height.
 * If a different size is desired the width and height of the TabPane can
 * be overridden by setting the min, pref and max size.</p>
 *
 * <p>When the number of tabs do not fit the TabPane a menu button will appear on the right.
 * The menu button is used to select the tabs that are currently not visible.
 * </p>
 *
 * <p>Example:</p>
 * <pre><code> TabPane tabPane = new TabPane();
 * Tab tab = new Tab();
 * tab.setText("new tab");
 * tab.setContent(new Rectangle(100, 50, Color.LIGHTSTEELBLUE));
 * tabPane.getTabs().add(tab);</code></pre>
 *
 * <img src="doc-files/TabPane.png" alt="Image of the TabPane control">
 *
 * @see Tab
 * @since JavaFX 2.0
 */
@DefaultProperty("tabs")
public class TabPane extends Control {
    private static final double DEFAULT_TAB_MIN_WIDTH = 0;

    private static final double DEFAULT_TAB_MAX_WIDTH = Double.MAX_VALUE;

    private static final double DEFAULT_TAB_MIN_HEIGHT = 0;

    private static final double DEFAULT_TAB_MAX_HEIGHT = Double.MAX_VALUE;

    /**
     * TabPane mode will be changed to floating allowing the TabPane
     * to be placed alongside other control.
     */
    public static final String STYLE_CLASS_FLOATING = "floating";

    /**
     * Constructs a new TabPane.
     */
    public TabPane() {
        this((Tab[])null);
    }

    /**
     * Constructs a new TabPane with the given tabs set to show.
     *
     * @param tabs The {@link Tab tabs} to display inside the TabPane.
     * @since JavaFX 8u40
     */
    public TabPane(Tab... tabs) {
        getStyleClass().setAll("tab-pane");
        setAccessibleRole(AccessibleRole.TAB_PANE);
        setSelectionModel(new TabPaneSelectionModel(this));

        this.tabs.addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                for (Tab tab : c.getRemoved()) {
                    if (tab != null && !getTabs().contains(tab)) {
                        tab.setTabPane(null);
                    }
                }

                for (Tab tab : c.getAddedSubList()) {
                    if (tab != null) {
                        tab.setTabPane(TabPane.this);
                    }
                }
            }
        });

        if (tabs != null) {
            getTabs().addAll(tabs);
        }

        // initialize pseudo-class state
        Side edge = getSide();
        pseudoClassStateChanged(TOP_PSEUDOCLASS_STATE, (edge == Side.TOP));
        pseudoClassStateChanged(RIGHT_PSEUDOCLASS_STATE, (edge == Side.RIGHT));
        pseudoClassStateChanged(BOTTOM_PSEUDOCLASS_STATE, (edge == Side.BOTTOM));
        pseudoClassStateChanged(LEFT_PSEUDOCLASS_STATE, (edge == Side.LEFT));

    }

    private ObservableList<Tab> tabs = new TabObservableList<>(new ArrayList<>());

    /**
     * The tabs to display in this TabPane. Changing this ObservableList will
     * immediately result in the TabPane updating to display the new contents
     * of this ObservableList.
     *
     * <p>If the tabs ObservableList changes, the selected tab will remain the previously
     * selected tab, if it remains within this ObservableList. If the previously
     * selected tab is no longer in the tabs ObservableList, the selected tab will
     * become the first tab in the ObservableList.</p>
     * @return the list of tabs
     */
    public final ObservableList<Tab> getTabs() {
        return tabs;
    }

    /**
     * The selection model used for selecting tabs. Changing the model alters
     * how the tabs are selected and which tabs are first or last.
     */
    private ObjectProperty<SingleSelectionModel<Tab>> selectionModel = new SimpleObjectProperty<>(this, "selectionModel");

    public final void setSelectionModel(SingleSelectionModel<Tab> value) { selectionModel.set(value); }

    public final SingleSelectionModel<Tab> getSelectionModel() { return selectionModel.get(); }

    public final ObjectProperty<SingleSelectionModel<Tab>> selectionModelProperty() { return selectionModel; }

    /**
     * The position to place the tabs in this {@code TabPane}. Changes to the value of this property
     * immediately updates the location of the tabs.
     * @defaultValue {@code Side.Top}
     */
    private ObjectProperty<Side> side;

    public final void setSide(Side value) {
        sideProperty().set(value);
    }

    public final Side getSide() {
        return side == null ? Side.TOP : side.get();
    }

    public final ObjectProperty<Side> sideProperty() {
        if (side == null) {
            side = new ObjectPropertyBase<Side>(Side.TOP) {
                private Side oldSide;
                @Override protected void invalidated() {

                    oldSide = get();

                    pseudoClassStateChanged(TOP_PSEUDOCLASS_STATE, (oldSide == Side.TOP || oldSide == null));
                    pseudoClassStateChanged(RIGHT_PSEUDOCLASS_STATE, (oldSide == Side.RIGHT));
                    pseudoClassStateChanged(BOTTOM_PSEUDOCLASS_STATE, (oldSide == Side.BOTTOM));
                    pseudoClassStateChanged(LEFT_PSEUDOCLASS_STATE, (oldSide == Side.LEFT));
                }

                @Override
                public Object getBean() {
                    return TabPane.this;
                }

                @Override
                public String getName() {
                    return "side";
                }
            };
        }
        return side;
    }

    /**
     * Specifies how the {@code TabPane} handles tab closing from an end-user's
     * perspective.
     *
     * <p>Refer to the {@link TabClosingPolicy} enumeration for further details.</p>
     *
     * @defaultValue {@code TabClosingPolicy.SELECTED_TAB}
     */
    private ObjectProperty<TabClosingPolicy> tabClosingPolicy;

    public final void setTabClosingPolicy(TabClosingPolicy value) {
        tabClosingPolicyProperty().set(value);
    }

    public final TabClosingPolicy getTabClosingPolicy() {
        return tabClosingPolicy == null ? TabClosingPolicy.SELECTED_TAB : tabClosingPolicy.get();
    }

    public final ObjectProperty<TabClosingPolicy> tabClosingPolicyProperty() {
        if (tabClosingPolicy == null) {
            tabClosingPolicy = new SimpleObjectProperty<>(this, "tabClosingPolicy", TabClosingPolicy.SELECTED_TAB);
        }
        return tabClosingPolicy;
    }

    /**
     * Specifies whether the graphic inside a {@code Tab} is rotated or not, such
     * that it is always upright, or rotated in the same way as the {@code Tab} text is.
     *
     * <p>If the value is {@code false}, the graphic isn't rotated, resulting in it always appearing upright.
     * If the value is {@code true}, the graphic is rotated with the {@code Tab} text.</p>
     *
     * @defaultValue {@code false}
     */
    private BooleanProperty rotateGraphic;

    public final void setRotateGraphic(boolean value) {
        rotateGraphicProperty().set(value);
    }

    public final boolean isRotateGraphic() {
        return rotateGraphic == null ? false : rotateGraphic.get();
    }

    public final BooleanProperty rotateGraphicProperty() {
        if (rotateGraphic == null) {
            rotateGraphic = new SimpleBooleanProperty(this, "rotateGraphic", false);
        }
        return rotateGraphic;
    }

    /**
     * The minimum width of a {@code Tab} in the {@code TabPane}.
     * This can be used to limit the length of text in tabs to prevent truncation.
     * Setting the same minimum and maximum widths will fix the width of the {@code Tab}.
     *
     * <p>This value can also be set via CSS using {@code -fx-tab-min-width}.</p>
     *
     * @defaultValue 0
     */
    private DoubleProperty tabMinWidth;

    public final void setTabMinWidth(double value) {
        tabMinWidthProperty().setValue(value);
    }

    public final double getTabMinWidth() {
        return tabMinWidth == null ? DEFAULT_TAB_MIN_WIDTH : tabMinWidth.getValue();
    }

    public final DoubleProperty tabMinWidthProperty() {
        if (tabMinWidth == null) {
            tabMinWidth = new StyleableDoubleProperty(DEFAULT_TAB_MIN_WIDTH) {

                @Override
                public CssMetaData<TabPane,Number> getCssMetaData() {
                    return StyleableProperties.TAB_MIN_WIDTH;
                }

                @Override
                public Object getBean() {
                    return TabPane.this;
                }

                @Override
                public String getName() {
                    return "tabMinWidth";
                }
            };
        }
        return tabMinWidth;
    }

    /**
     * The maximum width of a {@code Tab} in the {@code TabPane}.
     * This can be used to limit the length of text in tabs to prevent truncation.
     * If the {@code Tab} text is longer than the maximum width, the text will be truncated.
     * Setting the same minimum and maximum widths will fix the width of the {@code Tab}.
     *
     * <p>This value can also be set via CSS using {@code -fx-tab-max-width}.</p>
     *
     * @defaultValue {@code Double.MAX_VALUE}
     */
    private DoubleProperty tabMaxWidth;

    public final void setTabMaxWidth(double value) {
        tabMaxWidthProperty().setValue(value);
    }

    public final double getTabMaxWidth() {
        return tabMaxWidth == null ? DEFAULT_TAB_MAX_WIDTH : tabMaxWidth.getValue();
    }

    public final DoubleProperty tabMaxWidthProperty() {
        if (tabMaxWidth == null) {
            tabMaxWidth = new StyleableDoubleProperty(DEFAULT_TAB_MAX_WIDTH) {

                @Override
                public CssMetaData<TabPane,Number> getCssMetaData() {
                    return StyleableProperties.TAB_MAX_WIDTH;
                }

                @Override
                public Object getBean() {
                    return TabPane.this;
                }

                @Override
                public String getName() {
                    return "tabMaxWidth";
                }
            };
        }
        return tabMaxWidth;
    }

    /**
     * The minimum height of a {@code Tab} in the {@code TabPane}.
     * This can be used to limit the height of tabs.
     * Setting the same minimum and maximum heights will fix the height of the {@code Tab}.
     *
     * <p>This value can also be set via CSS using {@code -fx-tab-min-height}.</p>
     *
     * @defaultValue 0
     */
    private DoubleProperty tabMinHeight;

    public final void setTabMinHeight(double value) {
        tabMinHeightProperty().setValue(value);
    }

    public final double getTabMinHeight() {
        return tabMinHeight == null ? DEFAULT_TAB_MIN_HEIGHT : tabMinHeight.getValue();
    }

    public final DoubleProperty tabMinHeightProperty() {
        if (tabMinHeight == null) {
            tabMinHeight = new StyleableDoubleProperty(DEFAULT_TAB_MIN_HEIGHT) {

                @Override
                public CssMetaData<TabPane,Number> getCssMetaData() {
                    return StyleableProperties.TAB_MIN_HEIGHT;
                }

                @Override
                public Object getBean() {
                    return TabPane.this;
                }

                @Override
                public String getName() {
                    return "tabMinHeight";
                }
            };
        }
        return tabMinHeight;
    }

    /**
     * The maximum height of a {@code Tab} in the {@code TabPane}.
     * This can be used to limit the height of tabs.
     * Setting the same minimum and maximum heights will fix the height of the {@code Tab}.
     *
     * <p>This value can also be set via CSS using {@code -fx-tab-max-height}.</p>
     *
     * @defaultValue {@code Double.MAX_VALUE}
     */
    private DoubleProperty tabMaxHeight;

    public final void setTabMaxHeight(double value) {
        tabMaxHeightProperty().setValue(value);
    }

    public final double getTabMaxHeight() {
        return tabMaxHeight == null ? DEFAULT_TAB_MAX_HEIGHT : tabMaxHeight.getValue();
    }

    public final DoubleProperty tabMaxHeightProperty() {
        if (tabMaxHeight == null) {
            tabMaxHeight = new StyleableDoubleProperty(DEFAULT_TAB_MAX_HEIGHT) {

                @Override
                public CssMetaData<TabPane,Number> getCssMetaData() {
                    return StyleableProperties.TAB_MAX_HEIGHT;
                }

                @Override
                public Object getBean() {
                    return TabPane.this;
                }

                @Override
                public String getName() {
                    return "tabMaxHeight";
                }
            };
        }
        return tabMaxHeight;
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TabPaneSkin(this);
    }

    /** {@inheritDoc} */
    @Override public Node lookup(String selector) {
        Node n = super.lookup(selector);
        if (n == null) {
            for(Tab tab : tabs) {
                n = tab.lookup(selector);
                if (n != null) break;
            }
        }
        return n;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Node> lookupAll(String selector) {

        if (selector == null) return null;

        final List<Node> results = new ArrayList<>();

        results.addAll(super.lookupAll(selector));
        for(Tab tab : tabs) {
            results.addAll(tab.lookupAll(selector));
        }

        return new UnmodifiableListSet<>(results);
    }


    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static class StyleableProperties {
        private static final CssMetaData<TabPane,Number> TAB_MIN_WIDTH =
                new CssMetaData<>("-fx-tab-min-width",
                SizeConverter.getInstance(), DEFAULT_TAB_MIN_WIDTH) {

            @Override
            public boolean isSettable(TabPane n) {
                return n.tabMinWidth == null || !n.tabMinWidth.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TabPane n) {
                return (StyleableProperty<Number>)n.tabMinWidthProperty();
            }
        };

        private static final CssMetaData<TabPane,Number> TAB_MAX_WIDTH =
                new CssMetaData<>("-fx-tab-max-width",
                SizeConverter.getInstance(), DEFAULT_TAB_MAX_WIDTH) {

            @Override
            public boolean isSettable(TabPane n) {
                return n.tabMaxWidth == null || !n.tabMaxWidth.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TabPane n) {
                return (StyleableProperty<Number>)n.tabMaxWidthProperty();
            }
        };

        private static final CssMetaData<TabPane,Number> TAB_MIN_HEIGHT =
                new CssMetaData<>("-fx-tab-min-height",
                SizeConverter.getInstance(), DEFAULT_TAB_MIN_HEIGHT) {

            @Override
            public boolean isSettable(TabPane n) {
                return n.tabMinHeight == null || !n.tabMinHeight.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TabPane n) {
                return (StyleableProperty<Number>)n.tabMinHeightProperty();
            }
        };

        private static final CssMetaData<TabPane,Number> TAB_MAX_HEIGHT =
                new CssMetaData<>("-fx-tab-max-height",
                SizeConverter.getInstance(), DEFAULT_TAB_MAX_HEIGHT) {

            @Override
            public boolean isSettable(TabPane n) {
                return n.tabMaxHeight == null || !n.tabMaxHeight.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TabPane n) {
                return (StyleableProperty<Number>)n.tabMaxHeightProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(Control.getClassCssMetaData());
            styleables.add(TAB_MIN_WIDTH);
            styleables.add(TAB_MAX_WIDTH);
            styleables.add(TAB_MIN_HEIGHT);
            styleables.add(TAB_MAX_HEIGHT);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    private static final PseudoClass TOP_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("top");
    private static final PseudoClass BOTTOM_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("bottom");
    private static final PseudoClass LEFT_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("left");
    private static final PseudoClass RIGHT_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("right");

    /* *************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/

    static class TabPaneSelectionModel extends SingleSelectionModel<Tab> {
        private final TabPane tabPane;

        private ListChangeListener<Tab> itemsContentObserver;

        public TabPaneSelectionModel(final TabPane t) {
            if (t == null) {
                throw new NullPointerException("TabPane can not be null");
            }
            this.tabPane = t;

            // watching for changes to the items list content
            itemsContentObserver = c -> {
                while (c.next()) {
                    for (Tab tab : c.getRemoved()) {
                        if (tab != null && !tabPane.getTabs().contains(tab)) {
                            if (tab.isSelected()) {
                                tab.setSelected(false);
                                final int tabIndex = c.getFrom();

                                // we always try to select the nearest, non-disabled
                                // tab from the position of the closed tab.
                                findNearestAvailableTab(tabIndex, true);
                            }
                        }
                    }
                    if (c.wasAdded() || c.wasRemoved()) {
                        // The selected tab index can be out of sync with the list of tab if
                        // we add or remove tabs before the selected tab.
                        if (getSelectedIndex() != tabPane.getTabs().indexOf(getSelectedItem())) {
                            clearAndSelect(tabPane.getTabs().indexOf(getSelectedItem()));
                        }
                    }
                }
                if (getSelectedIndex() == -1 && getSelectedItem() == null && tabPane.getTabs().size() > 0) {
                    // we go looking for the first non-disabled tab, as opposed to
                    // just selecting the first tab (fix for RT-36908)
                    findNearestAvailableTab(0, true);
                } else if (tabPane.getTabs().isEmpty()) {
                    clearSelection();
                }
            };
            if (this.tabPane.getTabs() != null) {
                this.tabPane.getTabs().addListener(new WeakListChangeListener<>(itemsContentObserver));
            }
        }

        // API Implementation
        @Override public void select(int index) {
            if (index < 0 || (getItemCount() > 0 && index >= getItemCount()) ||
                (index == getSelectedIndex() && getModelItem(index).isSelected())) {
                return;
            }

            // Unselect the old tab
            if (getSelectedIndex() >= 0 && getSelectedIndex() < tabPane.getTabs().size()) {
                tabPane.getTabs().get(getSelectedIndex()).setSelected(false);
            }

            setSelectedIndex(index);

            Tab tab = getModelItem(index);
            if (tab != null) {
                setSelectedItem(tab);
            }

            // Select the new tab
            if (getSelectedIndex() >= 0 && getSelectedIndex() < tabPane.getTabs().size()) {
                tabPane.getTabs().get(getSelectedIndex()).setSelected(true);
            }

            /* Does this get all the change events */
            tabPane.notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM);
        }

        @Override public void select(Tab tab) {
            final int itemCount = getItemCount();

            for (int i = 0; i < itemCount; i++) {
                final Tab value = getModelItem(i);
                if (value != null && value.equals(tab)) {
                    select(i);
                    return;
                }
            }
        }

        @Override protected Tab getModelItem(int index) {
            final ObservableList<Tab> items = tabPane.getTabs();
            if (items == null) return null;
            if (index < 0 || index >= items.size()) return null;
            return items.get(index);
        }

        @Override protected int getItemCount() {
            final ObservableList<Tab> items = tabPane.getTabs();
            return items == null ? 0 : items.size();
        }

        private Tab findNearestAvailableTab(int tabIndex, boolean doSelect) {
            // we always try to select the nearest, non-disabled
            // tab from the position of the closed tab.
            final int tabCount = getItemCount();
            int i = 1;
            Tab bestTab = null;
            while (true) {
                // look leftwards
                int downPos = tabIndex - i;
                if (downPos >= 0) {
                    Tab _tab = getModelItem(downPos);
                    if (_tab != null && ! _tab.isDisable()) {
                        bestTab = _tab;
                        break;
                    }
                }

                // look rightwards. We subtract one as we need
                // to take into account that a tab has been removed
                // and if we don't do this we'll miss the tab
                // to the right of the tab (as it has moved into
                // the removed tabs position).
                int upPos = tabIndex + i - 1;
                if (upPos < tabCount) {
                    Tab _tab = getModelItem(upPos);
                    if (_tab != null && ! _tab.isDisable()) {
                        bestTab = _tab;
                        break;
                    }
                }

                if (downPos < 0 && upPos >= tabCount) {
                    break;
                }
                i++;
            }

            if (doSelect && bestTab != null) {
                select(bestTab);
            }

            return bestTab;
        }
    }

    /**
     * Specifies how the {@code TabPane} handles tab closing from an end-user's
     * perspective. The options are:
     *
     * <ul>
     *   <li> {@code TabClosingPolicy.UNAVAILABLE}: Tabs can not be closed by the user
     *   <li> {@code TabClosingPolicy.SELECTED_TAB}: Only the currently selected tab will
     *          have the option to be closed, with a graphic next to the tab
     *          text being shown. The graphic will disappear when a tab is no
     *          longer selected.
     *   <li> {@code TabClosingPolicy.ALL_TABS}: All tabs will have the option to be
     *          closed.
     * </ul>
     * @since JavaFX 2.0
     */
    public enum TabClosingPolicy {

        /**
         * Only the currently selected tab will have the option to be closed, with a
         * graphic next to the tab text being shown. The graphic will disappear when
         * a tab is no longer selected.
         */
        SELECTED_TAB,

        /**
         * All tabs will have the option to be closed.
         */
        ALL_TABS,

        /**
         * Tabs can not be closed by the user.
         */
        UNAVAILABLE
    }


    /**
     * The drag policy for the tabs. It specifies if tabs can be reordered or not.
     *
     * @defaultValue {@code TabDragPolicy.FIXED}
     * @since 10
     */
    private ObjectProperty<TabDragPolicy> tabDragPolicy;

    public final ObjectProperty<TabDragPolicy> tabDragPolicyProperty() {
        if (tabDragPolicy == null) {
            tabDragPolicy = new SimpleObjectProperty<>(this, "tabDragPolicy", TabDragPolicy.FIXED);
        }
        return tabDragPolicy;
    }
    public final void setTabDragPolicy(TabDragPolicy value) {
        tabDragPolicyProperty().set(value);
    }
    public final TabDragPolicy getTabDragPolicy() {
        return tabDragPolicyProperty().get();
    }

    /**
     * This enum specifies drag policies for tabs in a TabPane.
     *
     * @since 10
     */
    public enum TabDragPolicy {
        /**
         * The tabs remain fixed in their positions and cannot be dragged.
         */
        FIXED,

        /**
         * The tabs can be dragged to reorder them within the same TabPane.
         * Users can perform the simple mouse press-drag-release gesture on a
         * tab header to drag it to a new position. A tab can not be detached
         * from its parent TabPane.
         * <p>After a tab is reordered, the {@link #getTabs() tabs} list is
         * permuted to reflect the updated order.
         * A {@link javafx.collections.ListChangeListener.Change permutation
         * change} event is fired to indicate which tabs were reordered. This
         * reordering is done after the mouse button is released. While a tab
         * is being dragged, the list of tabs is unchanged.</p>
         */
        REORDER
    }
}
