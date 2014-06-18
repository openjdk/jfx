/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
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
package com.javafx.experiments.jfx3dviewer;

import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

/**
 * Visual display for timeline play head and length
 */
public class TimelineDisplay extends Region {
    private SimpleDoubleProperty currentTimeAsPercentage = new SimpleDoubleProperty(0) {
        @Override protected void invalidated() {
            requestLayout();
        }
    };
    private final SimpleObjectProperty<Timeline> timeline = new SimpleObjectProperty<Timeline>() {
        private Timeline old;
        @Override protected void invalidated() {
            final Timeline t = get();
            if (old != null) {
                currentTimeAsPercentage.unbind();
                end.textProperty().unbind();
            }
            if (t == null) {
                setVisible(false);
            } else {
                setVisible(true);
                currentTimeAsPercentage.bind(
                        new DoubleBinding() {
                            { bind(t.currentTimeProperty(), t.cycleDurationProperty()); }

                            @Override protected double computeValue() {
                                return t.getCurrentTime().toMillis() / t.getCycleDuration().toMillis();
                            }
                        });
                end.textProperty().bind(
                        new StringBinding() {
                            { bind(t.cycleDurationProperty()); }

                            @Override protected String computeValue() {
                                return String.format("%.2fs", t.getCycleDuration().toSeconds());
                            }
                        });
                current.textProperty().bind(
                        new StringBinding() {
                            { bind(t.currentTimeProperty()); }

                            @Override protected String computeValue() {
                                return String.format("%.2fs", t.getCurrentTime().toSeconds());
                            }
                        });
            }
            old = t;
        }
    };
    public Timeline getTimeline() { return timeline.get(); }
    public SimpleObjectProperty<Timeline> timelineProperty() { return timeline; }
    public void setTimeline(Timeline timeline) { this.timeline.set(timeline); }

    private final Region background = new Region();
    private final Region bar = new Region();
    private final Region progress = new Region();
    private final Text start = new Text("0s");
    private final Text end = new Text();
    private final Text current = new Text();

    public TimelineDisplay() {
        getStyleClass().add("timeline-display");
        background.getStyleClass().add("background");
        background.setCache(true); // cache so we don't have to render shadow every frame
        bar.getStyleClass().add("bar");
        progress.getStyleClass().add("progress");
        getChildren().addAll(background,start,current,end,bar,progress);
    }

    @Override protected double computePrefWidth(double height) {
        return 200;
    }

    @Override protected double computePrefHeight(double width) {
        return 24;
    }

    @Override protected void layoutChildren() {
        final double w = getWidth() - snappedLeftInset() - snappedRightInset();
        background.resizeRelocate(0,0,getWidth(),getHeight());
        bar.resizeRelocate(snappedLeftInset(),snappedTopInset(),w,6);
        progress.resizeRelocate(snappedLeftInset(),snappedTopInset(),w*currentTimeAsPercentage.get(),6);
        start.setLayoutX(snappedLeftInset());
        start.setLayoutY(getHeight() - snappedBottomInset());
        current.setLayoutX((int)((getWidth() - current.getLayoutBounds().getWidth())/2d));
        current.setLayoutY(getHeight() - snappedBottomInset());
        end.setLayoutX(getWidth() - snappedRightInset() - end.getLayoutBounds().getWidth());
        end.setLayoutY(getHeight() - snappedBottomInset());
    }
}
