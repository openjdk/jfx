/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Skin;
import javafx.scene.control.TitledPane;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.scene.control.behavior.AccordionBehavior;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccordionSkin extends BehaviorSkinBase<Accordion, AccordionBehavior> {

    private TitledPane firstTitledPane;
    private Rectangle clipRect;

    // This is used when we definitely want to force a relayout, regardless of
    // whether the height has also changed
    private boolean forceRelayout = false;

    // this is used to request a layout, assuming the height has also changed
    private boolean relayout = false;

    // we record the previous height to know if the current height is different
    private double previousHeight = 0;

    public AccordionSkin(final Accordion accordion) {
        super(accordion, new AccordionBehavior(accordion));
        accordion.getPanes().addListener((ListChangeListener<TitledPane>) c -> {
            if (firstTitledPane != null) {
                firstTitledPane.getStyleClass().remove("first-titled-pane");
            }
            if (!accordion.getPanes().isEmpty()) {
                firstTitledPane = accordion.getPanes().get(0);
                firstTitledPane.getStyleClass().add("first-titled-pane");
            }
            // TODO there may be a more efficient way to keep these in sync
            getChildren().setAll(accordion.getPanes());
            while (c.next()) {
                removeTitledPaneListeners(c.getRemoved());
                initTitledPaneListeners(c.getAddedSubList());
            }

            // added to resolve RT-32787
            forceRelayout = true;
        });

        if (!accordion.getPanes().isEmpty()) {
            firstTitledPane = accordion.getPanes().get(0);
            firstTitledPane.getStyleClass().add("first-titled-pane");
        }

        clipRect = new Rectangle(accordion.getWidth(), accordion.getHeight());
        getSkinnable().setClip(clipRect);

        initTitledPaneListeners(accordion.getPanes());
        getChildren().setAll(accordion.getPanes());
        getSkinnable().requestLayout();

        registerChangeListener(getSkinnable().widthProperty(), "WIDTH");
        registerChangeListener(getSkinnable().heightProperty(), "HEIGHT");
    }

    @Override
    protected void handleControlPropertyChanged(String property) {
        super.handleControlPropertyChanged(property);
        if ("WIDTH".equals(property)) {
            clipRect.setWidth(getSkinnable().getWidth());
        } else if ("HEIGHT".equals(property)) {
            clipRect.setHeight(getSkinnable().getHeight());
            relayout = true;
        }
    }

    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double h = 0;
        for (Node child: getChildren()) {
            h += snapSize(child.minHeight(width));
        }
        return h;
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double h = 0;

        if (expandedPane != null) {
            h += expandedPane.prefHeight(-1);
        }

        if (previousPane != null && !previousPane.equals(expandedPane)) {
            h += previousPane.prefHeight(-1);
        }

        for (Node child: getChildren()) {
            TitledPane pane = (TitledPane)child;
            if (!pane.equals(expandedPane) && !pane.equals(previousPane)) {
                // The min height is the height of the TitledPane's title bar.
                // We use the sum of all the TitledPane's title bars
                // to compute the pref height of the accordion.
                h += snapSize(pane.minHeight(width));
            }
        }

        return h + topInset + bottomInset;
    }

    @Override protected void layoutChildren(final double x, double y,
            final double w, final double h) {
        final boolean rebuild = forceRelayout || (relayout && previousHeight != h);
        forceRelayout = false;
        previousHeight = h;

        // Compute height of all the collapsed panes
        double collapsedPanesHeight = 0;
        for (TitledPane tp : getSkinnable().getPanes()) {
            if (!tp.equals(expandedPane)) {
                // min height is the TitledPane's title bar height.
                collapsedPanesHeight += snapSize(tp.minHeight(-1));
            }
        }
        final double maxTitledPaneHeight = h - collapsedPanesHeight;

        for (TitledPane tp : getSkinnable().getPanes()) {
            Skin<?> skin = tp.getSkin();
            double ph;
            if (skin instanceof TitledPaneSkin) {
                ((TitledPaneSkin)skin).setMaxTitledPaneHeightForAccordion(maxTitledPaneHeight);
                ph = snapSize(((TitledPaneSkin)skin).getTitledPaneHeightForAccordion());
            } else {
                ph = tp.prefHeight(w);
            }
            tp.resize(w, ph);

            boolean needsRelocate = true;
            if (! rebuild && previousPane != null && expandedPane != null) {
                List<TitledPane> panes = getSkinnable().getPanes();
                final int previousPaneIndex = panes.indexOf(previousPane);
                final int expandedPaneIndex = panes.indexOf(expandedPane);
                final int currentPaneIndex  = panes.indexOf(tp);

                if (previousPaneIndex < expandedPaneIndex) {
                    // Current expanded pane is after the previous expanded pane..
                    // Only move the panes that are less than or equal to the current expanded.
                    if (currentPaneIndex <= expandedPaneIndex) {
                        tp.relocate(x, y);
                        y += ph;
                        needsRelocate = false;
                    }
                } else if (previousPaneIndex > expandedPaneIndex) {
                    // Previous pane is after the current expanded pane.
                    // Only move the panes that are less than or equal to the previous expanded pane.
                    if (currentPaneIndex <= previousPaneIndex) {
                        tp.relocate(x, y);
                        y += ph;
                        needsRelocate = false;
                    }
                } else {
                    // Previous and current expanded pane are the same.
                    // Since we are expanding and collapsing the same pane we will need to relocate
                    // all the panes.
                    tp.relocate(x, y);
                    y += ph;
                    needsRelocate = false;
                }
            }

            if (needsRelocate) {
                tp.relocate(x, y);
                y += ph;
            }
        }
    }

    private TitledPane expandedPane = null;
    private TitledPane previousPane = null;
    private Map<TitledPane, ChangeListener<Boolean>>listeners = new HashMap<TitledPane, ChangeListener<Boolean>>();

    private void initTitledPaneListeners(List<? extends TitledPane> list) {
        for (final TitledPane tp: list) {
            tp.setExpanded(tp == getSkinnable().getExpandedPane());
            if (tp.isExpanded()) {
                expandedPane = tp;
            }
            ChangeListener<Boolean> changeListener = expandedPropertyListener(tp);
            tp.expandedProperty().addListener(changeListener);
            listeners.put(tp, changeListener);
        }
    }

    private void removeTitledPaneListeners(List<? extends TitledPane> list) {
        for (final TitledPane tp: list) {
            if (listeners.containsKey(tp)) {
                tp.expandedProperty().removeListener(listeners.get(tp));
                listeners.remove(tp);
            }
        }
    }

    private ChangeListener<Boolean> expandedPropertyListener(final TitledPane tp) {
        return (observable, wasExpanded, expanded) -> {
            previousPane = expandedPane;
            final Accordion accordion = getSkinnable();
            if (expanded) {
                if (expandedPane != null) {
                    expandedPane.setExpanded(false);
                }
                if (tp != null) {
                    accordion.setExpandedPane(tp);
                }
                expandedPane = accordion.getExpandedPane();
            } else {
                expandedPane = null;
                accordion.setExpandedPane(null);
            }
        };
    }
}
