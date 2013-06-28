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

import com.sun.javafx.scene.control.behavior.ProgressBarBehavior;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class LoginProgressBarSkin extends SkinBase<ProgressBar, ProgressBarBehavior<ProgressBar>> {
    /**
     * The track is rendered by the control itself, the bar though is provided by this skin.
     */
    private Region bar;

    private Timeline widthTimeline = null;
    private DoubleProperty barWidth = new SimpleDoubleProperty(this, "barWidth", 0) {
        @Override protected void invalidated() {
            requestLayout();
        }
    }; // between 0 and 1

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public LoginProgressBarSkin(final ProgressBar control) {
        super(control, new ProgressBarBehavior<ProgressBar>(control));
        InvalidationListener listener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                if (widthTimeline != null) widthTimeline.stop();
                widthTimeline = new Timeline(
                        new KeyFrame(Duration.millis(500),
                                new KeyValue(barWidth, control.getProgress(), Interpolator.EASE_IN)));
                widthTimeline.play();
            }
        };
        control.widthProperty().addListener(listener);
        control.progressProperty().addListener(listener);

        bar = new Region();
        bar.getStyleClass().setAll("bar");
        getChildren().setAll( bar);
        requestLayout();
    }

    @Override
    public double getBaselineOffset() {
        double height = getSkinnable().getHeight();
        return getInsets().getTop() + height;
    }

    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/

    @Override protected double computePrefWidth(double height) {
        return Math.max(100, getInsets().getLeft() + bar.prefWidth(getWidth()) + getInsets().getRight());
    }

    @Override protected double computePrefHeight(double width) {
        return getInsets().getTop() + bar.prefHeight(width) + getInsets().getBottom();
    }

    @Override protected void layoutChildren(final double x, final double y, final double w, final double h) {
        final boolean indeterminate = getSkinnable().isIndeterminate();
        double bw = indeterminate ? 0 : w * barWidth.get();
        if (bw > 0) {
            final Insets barInsets = bar.getInsets();
            bw = Math.max(barInsets.getLeft() + barInsets.getRight(), bw);
        }
        final Insets insets = getInsets();
        bar.resizeRelocate(x, y, bw, getHeight() - (insets.getTop() + insets.getBottom()));
    }
}
