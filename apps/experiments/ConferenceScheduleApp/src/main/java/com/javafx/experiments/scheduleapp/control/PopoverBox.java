/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates.
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
package com.javafx.experiments.scheduleapp.control;

import java.util.List;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * Similar to the PopoverTreeList, but represents the content as an HBox of items rather
 * than a ListView of items. This is a reasonable choice when there are a smaller number
 * of items to display and virtualization is not required. Each item in the tree box looks like:
 * <pre>
 *   ---------------------------------------------------
 *  | Item Name         [Optional Name of Next Page]  > |
 *   ---------------------------------------------------
 * </pre>
 */
public class PopoverBox<T> extends Region {
    public final ObservableList<PopoverBoxItem<T>> getItems() {
        return (ObservableList<PopoverBoxItem<T>>) (ObservableList) getChildren();
    }

    public PopoverBox() {
        getStyleClass().setAll("popover-box");
    }

    @Override protected double computePrefHeight(double width) {
        List<Node> children = getChildren();
        double prefHeight = 0;
        for (int i=0; i<children.size(); i++) {
            Node child = children.get(i);
            prefHeight += child.prefHeight(child.prefWidth(-1));
        }

        final Insets insets = getInsets();
        return insets.getTop() + prefHeight + insets.getBottom();
    }

    @Override protected double computePrefWidth(double height) {
        List<Node> children = getChildren();
        double prefWidth = 0;
        for (int i=0; i<children.size(); i++) {
            Node child = children.get(i);
            prefWidth = Math.max(prefWidth, child.prefWidth(-1));
        }

        final Insets insets = getInsets();
        return insets.getLeft() + prefWidth + insets.getRight();
    }

    @Override protected void layoutChildren() {
        final Insets insets = getInsets();
        final double top = insets.getTop();
        final double left = insets.getLeft();
        final double width = getWidth() - left - insets.getRight();

        List<Node> children = getChildren();
        double y = top;
        for (int i=0; i<children.size(); i++) {
            Node child = children.get(i);
            double h = child.prefHeight(width);
            child.resizeRelocate((int)(left + .5), (int)(y + .5), (int)(width + .5), (int)(h + .5));
            y += h;
        }
    }
}
