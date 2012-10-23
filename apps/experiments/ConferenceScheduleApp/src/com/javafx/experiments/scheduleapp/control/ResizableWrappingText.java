/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.text.Text;

/**
 * A text node that resizes its wrapping width
 */
public class ResizableWrappingText extends Text implements ChangeListener{
    private double w = 100;

    private double cachedPrefWidth = -2, cachedPrefHeight = -2;
    private double prefWidthCacheKey = -2, prefHeightCacheKey = -2;

    private double cachedNegativeOnePrefWidth = -1;

    public ResizableWrappingText() {
        init();
    }

    public ResizableWrappingText(String text) {
        super(text);
        init();
    }

    private void init() {
        setTextOrigin(VPos.TOP);
        textProperty().addListener(this);
    }
    
    @Override public void changed(ObservableValue ov, Object t, Object t1) {
        if (ov == textProperty()) {
            final Parent parent = getParent();
            if (parent != null) parent.requestLayout();
        }

        cachedPrefWidth = cachedPrefHeight = prefWidthCacheKey = prefHeightCacheKey = -2;
        cachedNegativeOnePrefWidth = -1;
    }

    @Override public boolean isResizable() {
        return true;
    }

    @Override public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override public void resize(double width, double height) {
        w = width;
        setWrappingWidth(w);
    }

    @Override public double minWidth(double height) {
        return 0;
    }

    @Override public double minHeight(double width) {
        return 0;
    }

    @Override public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override public double prefHeight(double width) {
        if (width < 0) {
            return getLayoutBounds().getHeight();
        }

        if (prefHeightCacheKey == width) {
            return cachedPrefHeight;
        }

        setWrappingWidth(width);
        prefHeightCacheKey = width;
        cachedPrefHeight = getLayoutBounds().getHeight();
        setWrappingWidth(w);
        return cachedPrefHeight;
    }

    @Override public double prefWidth(double height) {
        if (height < 0) {
            if (cachedNegativeOnePrefWidth == -1) {
                setWrappingWidth(0);
                cachedNegativeOnePrefWidth = getLayoutBounds().getWidth();
                setWrappingWidth(w);
            }
            return cachedNegativeOnePrefWidth;
        }

        if (prefWidthCacheKey == height) {
            return cachedPrefWidth;
        }

        setWrappingWidth(0);
        prefWidthCacheKey = height;
        cachedPrefWidth = getLayoutBounds().getWidth();
        setWrappingWidth(w);
        return cachedPrefWidth;
    }
}
