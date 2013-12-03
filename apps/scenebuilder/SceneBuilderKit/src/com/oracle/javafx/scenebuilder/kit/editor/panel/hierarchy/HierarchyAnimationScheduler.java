/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.ScrollBar;
import javafx.util.Duration;

/**
 * Used to schedule :
 *
 * - auto scrolling animation when reaching the TOP/BOTTOM of the hierarchy panel
 *
 * p
 * @treatAsPrivate
 */
public class HierarchyAnimationScheduler {

    private Timeline timeline;
    // Rate value used to set the timeline duration.
    // The bigger it is, the slower the animation will be.
    private final double rate = 4.0;

    public void playDecrementAnimation(final ScrollBar scrollBar) {
        assert scrollBar != null;
        final double minValue = scrollBar.getMin();
        assert isTimelineRunning() == false;
        // If the scroll bar is not yet at its min value,
        // we play the scroll bar decrement animation
        if (scrollBar.getValue() > minValue) {
            // The timeline duration value depends on :
            // - the scroll bar height
            // - the scroll bar thumb size (visibleAmount property)
            // - the scroll bar value
            final double scrollBarHeight = scrollBar.getHeight();
            final double scrollBarVisibleAmount = scrollBar.getVisibleAmount();
            final double scrollBarValue = scrollBar.getValue();

            // Height between the scroll bar top and the scroll bar thumb
            final double height = scrollBarHeight * scrollBarValue;
            final double duration = height * rate / scrollBarVisibleAmount; // duration in millis

            getTimeline().getKeyFrames().setAll(new KeyFrame(
                    new Duration(duration),
                    new KeyValue(scrollBar.valueProperty(), minValue)));
            getTimeline().play();
        }
    }

    public void playIncrementAnimation(final ScrollBar scrollBar) {
        assert scrollBar != null;
        final double maxValue = scrollBar.getMax();
        assert isTimelineRunning() == false;
        // If the scroll bar is not yet at its max value,
        // we play the scroll bar increment animation
        if (scrollBar.getValue() < maxValue) {
            // The timeline duration value depends on :
            // - the scroll bar height
            // - the scroll bar thumb size (visibleAmount property)
            // - the scroll bar value
            final double scrollBarHeight = scrollBar.getHeight();
            final double scrollBarVisibleAmount = scrollBar.getVisibleAmount();
            final double scrollBarValue = scrollBar.getValue();

            // Height between the scroll bar thumb and the scroll bar bottom
            final double height = scrollBarHeight * (scrollBar.getMax() - scrollBarValue);
            final double duration = height * rate / scrollBarVisibleAmount; // duration in millis

            getTimeline().getKeyFrames().setAll(new KeyFrame(
                    new Duration(duration),
                    new KeyValue(scrollBar.valueProperty(), maxValue)));
            getTimeline().play();
        }
    }

    public boolean isTimelineRunning() {
        return timeline == null ? false
                : timeline.getStatus() == Timeline.Status.RUNNING;
    }

    public void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private Timeline getTimeline() {
        if (timeline == null) {
            timeline = new Timeline();
        }
        return timeline;
    }
}
