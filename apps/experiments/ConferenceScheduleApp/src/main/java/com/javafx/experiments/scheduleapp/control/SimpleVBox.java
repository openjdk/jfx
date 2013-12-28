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

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * A much simpler and lightweight version of vbox. It also ignores invisible 
 * items. All items are layed out top to bottom with the pref height and the 
 * full width of this vbox;
 */
public class SimpleVBox extends Pane{
    private final int spacing;

    public SimpleVBox(int spacing) {
        this.spacing = spacing;
        setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setPrefWidth(400);
    }

    @Override protected double computePrefHeight(double width) {
        System.out.println("COMPUTING HEIGHT width = "+width);
        new Exception().printStackTrace(System.out);
        final Insets insets = getInsets();
        final int w = (width==-1)? -1 : (int)(width - insets.getLeft() + insets.getRight());
        System.out.println("    w = " + w);
        int height = (int)insets.getTop() + (int)insets.getBottom();
        System.out.println("    height = " + height);
        final ObservableList<Node> children = getChildren();
        for (int i=0; i<children.size(); i++) {
            Node child = children.get(i);
//            if (child.isManaged() && child.isVisible()) {
                height += child.prefHeight(w)+0.5;
                System.out.println("        adding "+((int)child.prefHeight(w)));
                if (i!=0) {
                    height += spacing;
                    System.out.println("        adding spacing "+spacing);
                }
//            } 
        }
        System.out.println("   ---->  "+height);
        return height;
    }

    @Override protected void layoutChildren() {
        final Insets insets = getInsets();
        final int width = (int)(getWidth() - insets.getLeft() + insets.getRight());
        final int x = (int)insets.getLeft();
        int y = (int)insets.getTop();
        final ObservableList<Node> children = getChildren();
        for (int i=0; i<children.size(); i++) {
            Node child = children.get(i);
            if (child.isManaged() && child.isVisible()) {
                final int h = (int)(child.prefHeight(width)+0.5);
                child.setLayoutX(x);
                child.setLayoutY(y);
                child.resize(width, h);
                y += h;
                if (i!=0) y += spacing;
            } 
        }
    }
}
