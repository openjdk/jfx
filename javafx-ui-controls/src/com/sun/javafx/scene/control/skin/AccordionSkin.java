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

import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;

import com.sun.javafx.scene.control.behavior.AccordionBehavior;

public class AccordionSkin extends SkinBase<Accordion, AccordionBehavior> {

    private TitledPane firstTitledPane;

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
                // TODO when we have access to the bean we need to remove the listeners
                while (c.next()) {
                    initTitledPaneListeners(c.getAddedSubList());
                }                
            }
        });

        if (!accordion.getPanes().isEmpty()) {
            firstTitledPane = accordion.getPanes().get(0);
            firstTitledPane.getStyleClass().add("first-titled-pane");
        }
        initTitledPaneListeners(accordion.getPanes());
        getChildren().setAll(accordion.getPanes());
        requestLayout();
    }

    @Override protected double computePrefHeight(double width) {
        double h = 0;
        for (Node child: getManagedChildren()) {
            h += snapSize(child.prefHeight(width));
        }
        return h + snapSpace(getInsets().getTop()) + snapSpace(getInsets().getBottom());
    }

    @Override protected void layoutChildren() {
        double w = snapSize(getWidth()) - (snapSpace(getInsets().getLeft()) + snapSpace(getInsets().getRight()));
        double x = snapSpace(getInsets().getLeft());
        double y = snapSpace(getInsets().getTop());

        // TODO need to replace spacing with margins.
        double spacing = 0;

        for(Node n: getSkinnable().getPanes()) {
            double ph = snapSize(n.prefHeight(-1));
            n.resize(w, ph);
            n.relocate(x, y);
            y += ph + spacing;
        }
    }

    private void initTitledPaneListeners(List<? extends TitledPane> list) {
        for (final TitledPane tp: list) {
            tp.setExpanded(tp == getSkinnable().getExpandedPane());
            tp.expandedProperty().addListener(new ChangeListener<Boolean>() {
                @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean wasExpanded, Boolean expanded) {
                    if (expanded) {
                        if (getSkinnable().getExpandedPane() != null) {
                            getSkinnable().getExpandedPane().setExpanded(false);
                        }
                        if (tp != null) {
                            getSkinnable().setExpandedPane(tp);
                        }
                    } else {
                        getSkinnable().setExpandedPane(null);
                    }
                }
            });
        }
    }
}
