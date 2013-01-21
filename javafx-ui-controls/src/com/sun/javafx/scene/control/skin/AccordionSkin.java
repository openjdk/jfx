/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Insets;

public class AccordionSkin extends BehaviorSkinBase<Accordion, AccordionBehavior> {

    private TitledPane firstTitledPane;
    private Rectangle clipRect;
    private boolean relocateAllPanes = false;
    private boolean resize = false;
    private double previousHeight = 0;

    public AccordionSkin(final Accordion accordion) {
        super(accordion, new AccordionBehavior(accordion));
        accordion.getPanes().addListener(new ListChangeListener<TitledPane>() {
            @Override public void onChanged(Change<? extends TitledPane> c) {
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
            }
        });

        if (!accordion.getPanes().isEmpty()) {
            firstTitledPane = accordion.getPanes().get(0);
            firstTitledPane.getStyleClass().add("first-titled-pane");
        }

        clipRect = new Rectangle();
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
        }
    }

    @Override protected double computeMinHeight(double width) {
        double h = 0;
        for (Node child: getChildren()) {
            h += snapSize(child.minHeight(width));
        }
        return h;
    }

    @Override protected double computePrefHeight(double width) {
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
        
        Insets insets = getSkinnable().getInsets();
        return h + snapSpace(insets.getTop()) + snapSpace(insets.getBottom());
    }

    @Override protected void layoutChildren(final double x, double y,
            final double w, final double h) {

        if (previousHeight != h) {
            previousHeight = h;
            resize = true;
        } else {
            resize = false;
        }

        // TODO need to replace spacing with margins.
        double spacing = 0;
        double collapsedPanesHeight = 0;

        // Compute height of all the collapsed panes
        for(Node n: getSkinnable().getPanes()) {
            TitledPane tp = ((TitledPane)n);
            if (!tp.equals(expandedPane)) {
                // min height is the TitledPane's title bar height.
                collapsedPanesHeight += snapSize(tp.minHeight(-1));
            }
        }
        double maxTitledPaneHeight = h - collapsedPanesHeight;

        for(Node n: getSkinnable().getPanes()) {
            TitledPane tp = ((TitledPane)n);
            Skin<?> skin = tp.getSkin();
            double ph;
            if (skin instanceof TitledPaneSkin) {
                ((TitledPaneSkin)skin).setMaxTitledPaneHeightForAccordion(maxTitledPaneHeight);
                ph = snapSize(((TitledPaneSkin)skin).getTitledPaneHeightForAccordion());
            } else {
                ph = tp.prefHeight(w);
            }
            tp.resize(w, ph);

            if (!resize && previousPane != null && expandedPane != null) {
                // Current expanded pane is after the previous expanded pane..
                if (getSkinnable().getPanes().indexOf(previousPane) < getSkinnable().getPanes().indexOf(expandedPane)) {
                    // Only move the panes that are less than or equal to the current expanded.
                    if (relocateAllPanes || getSkinnable().getPanes().indexOf(tp) <= getSkinnable().getPanes().indexOf(expandedPane)) {
                        tp.relocate(x, y);
                        y += ph + spacing;
                    }
                // Previous pane is after the current expanded pane.
                } else if (getSkinnable().getPanes().indexOf(previousPane) > getSkinnable().getPanes().indexOf(expandedPane)) {
                    // Only move the panes that are less than or equal to the previous expanded pane.
                    if (relocateAllPanes || getSkinnable().getPanes().indexOf(tp) <= getSkinnable().getPanes().indexOf(previousPane)) {
                        tp.relocate(x, y);
                        y += ph + spacing;
                    }
                // Previous and current expanded pane are the same.
                } else {
                    // Since we are expanding and collapsing the same pane we will need to relocate
                    // all the panes.
                    relocateAllPanes = true;
                    tp.relocate(x, y);
                    y += ph + spacing;
                }
            } else {
                tp.relocate(x, y);
                y += ph + spacing;
            }
        }
        // We have relocated all the pane turn relocateAllPanes off.
        if (expandedPane != null &&
                ((TitledPaneSkin)expandedPane.getSkin()).getTitledPaneHeightForAccordion() == maxTitledPaneHeight) {
            relocateAllPanes = false;
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
        return new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean wasExpanded, Boolean expanded) {
                previousPane = expandedPane;
                if (expanded) {
                    if (getSkinnable().getExpandedPane() != null) {
                        getSkinnable().getExpandedPane().setExpanded(false);
                    }
                    if (tp != null) {
                        getSkinnable().setExpandedPane(tp);
                    }
                    expandedPane = getSkinnable().getExpandedPane();
                } else {
                    expandedPane = getSkinnable().getExpandedPane();
                    getSkinnable().setExpandedPane(null);
                }
            }
        };
    }
}
