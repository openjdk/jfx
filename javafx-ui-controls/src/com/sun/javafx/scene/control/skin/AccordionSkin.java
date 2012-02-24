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
import javafx.scene.control.TitledPane;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.scene.control.behavior.AccordionBehavior;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccordionSkin extends SkinBase<Accordion, AccordionBehavior> {

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
        setClip(clipRect);

        initTitledPaneListeners(accordion.getPanes());
        getChildren().setAll(accordion.getPanes());
        requestLayout();
    }

    @Override protected double computeMinHeight(double width) {
        double h = 0;
        for (Node child: getManagedChildren()) {
            h += snapSize(child.minHeight(width));
        }
        return h;
    }

    @Override protected double computePrefHeight(double width) {
        double h = 0;
        // We want the current expanded pane or the currently collapsing one (previousPane).
        // This is because getExpandedPane() will be null when the expanded pane
        // is collapsing.
        TitledPane expandedTitledPane = getSkinnable().getExpandedPane() != null ? getSkinnable().getExpandedPane() : previousPane;
        if (expandedTitledPane != null) {
            h = expandedTitledPane.prefHeight(-1);
        }
        for (Node child: getManagedChildren()) {
            TitledPane pane = (TitledPane)child;
            if (!pane.equals(expandedTitledPane)) {
                // The min height is the height of the TitledPane's title bar.
                // We use the sum of all the TitledPane's title bars
                // to compute the pref height of the accordion.
                h += snapSize(pane.minHeight(width));
            }
        }
        return h + snapSpace(getInsets().getTop()) + snapSpace(getInsets().getBottom());
    }

    @Override protected void layoutChildren() {
        double w = snapSize(getWidth()) - (snapSpace(getInsets().getLeft()) + snapSpace(getInsets().getRight()));
        double h = snapSize(getHeight()) - (snapSpace(getInsets().getTop()) + snapSpace(getInsets().getBottom()));
        double x = snapSpace(getInsets().getLeft());
        double y = snapSpace(getInsets().getTop());

        // TODO need to replace spacing with margins.
        double spacing = 0;
        double collapsedPanesHeight = 0;

        // Compute height of all the collapsed panes
        for(Node n: getSkinnable().getPanes()) {
            TitledPane tp = ((TitledPane)n);
            if (!tp.isExpanded()) {
                // min height is the TitledPane's title bar height.
                collapsedPanesHeight += snapSize(tp.minHeight(-1));
            }
        }

        double maxTitledPaneHeight = 0;
        if (previousPane != null && previousPane.equals(expandedPane) && getSkinnable().getExpandedPane() == null) {
            if (getSkinnable().getPanes().size() ==  1) {
                // Open and close same pane.
                maxTitledPaneHeight = h;
            } else {
                // Open and close same pane if there are more than one pane.
                maxTitledPaneHeight = h - collapsedPanesHeight + previousPane.minHeight(-1);
            }
        } else {
            maxTitledPaneHeight = h - collapsedPanesHeight;
        }

        for(Node n: getSkinnable().getPanes()) {
            TitledPane tp = ((TitledPane)n);
            TitledPaneSkin skin = (TitledPaneSkin) tp.getSkin();
            skin.setMaxTitledPaneHeightForAccordion(maxTitledPaneHeight);
            double ph = snapSize(skin.getTitledPaneHeightForAccordion());
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

    @Override protected void setWidth(double value) {
        super.setWidth(value);
        clipRect.setWidth(value);
    }

    @Override protected void setHeight(double value) {
        super.setHeight(value);
        clipRect.setHeight(value);
        if (previousHeight != value) {
            previousHeight = value;
            resize = true;
        } else {
            resize = false;
        }
    }

    private TitledPane expandedPane = null;
    private TitledPane previousPane = null;
    private Map<TitledPane, ChangeListener>listeners = new HashMap();

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
