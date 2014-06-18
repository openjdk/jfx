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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * A controller class to bind up a timeline to play controler buttons
 */
public class TimelineController {
    private final ChangeListener<Number> rateListener = new ChangeListener<Number>() {
        @Override public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newRate) {
            System.out.println("newRate = " + newRate);
            if (newRate.intValue() == 0 && playBtn.isSelected()) {
                playBtn.setSelected(false);
            }
        }
    };
    private final SimpleObjectProperty<Timeline> timeline = new SimpleObjectProperty<Timeline>() {
        private Timeline old;
        @Override protected void invalidated() {
            Timeline t = get();
            if (old != null) {
                old.currentRateProperty().removeListener(rateListener);
            }
            if (t == null) {
                startBtn.setDisable(true);
                rwBtn.setDisable(true);
                playBtn.setDisable(true);
                ffBtn.setDisable(true);
                endBtn.setDisable(true);
                loopBtn.setDisable(true);
            } else {
                startBtn.setDisable(false);
                rwBtn.setDisable(false);
                playBtn.setDisable(false);
                ffBtn.setDisable(false);
                endBtn.setDisable(false);
                loopBtn.setDisable(false);
                playBtn.setSelected(t.getCurrentRate() != 0);
                loopBtn.setSelected(t.getCycleDuration().equals(Timeline.INDEFINITE));
                t.currentRateProperty().addListener(rateListener);
            }
            old = t;
        }
    };
    public Timeline getTimeline() { return timeline.get(); }
    public SimpleObjectProperty<Timeline> timelineProperty() { return timeline; }
    public void setTimeline(Timeline timeline) { this.timeline.set(timeline); }

    private final Button startBtn;
    private final Button rwBtn;
    private final ToggleButton playBtn;
    private final Button ffBtn;
    private final Button endBtn;
    private final ToggleButton loopBtn;

    public TimelineController(Button startBtn, Button rwBtn,final ToggleButton playBtn, Button ffBtn, Button endBtn,final ToggleButton loopBtn) {
        this.startBtn = startBtn;
        this.rwBtn = rwBtn;
        this.playBtn = playBtn;
        this.ffBtn = ffBtn;
        this.endBtn = endBtn;
        this.loopBtn = loopBtn;

        this.startBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                getTimeline().jumpTo(Duration.ZERO);
                getTimeline().pause();
            }
        });
        this.endBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                getTimeline().jumpTo(getTimeline().getTotalDuration());
                getTimeline().pause();
            }
        });
        this.playBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                System.out.println("playBtn.isSelected() = " + playBtn.isSelected());
                if (playBtn.isSelected()) { // currently paused so play
                    getTimeline().play();
                } else { // currently playing so pause
                    getTimeline().pause();
                }
            }
        });
        this.ffBtn.setOnMousePressed(
                new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        getTimeline().setRate(2);
                    }
                });
        this.ffBtn.setOnMouseReleased(
                new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        getTimeline().setRate(1);
                    }
                });
        this.rwBtn.setOnMousePressed(
                new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        getTimeline().setRate(-2);
                    }
                });
        this.rwBtn.setOnMouseReleased(
                new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        getTimeline().setRate(1);
                    }
                });
        this.loopBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                System.out.println("LOOP CHANGE TO "+loopBtn.isSelected()+"  before="+getTimeline().getCycleCount());
                if (loopBtn.isSelected()) {
                    getTimeline().stop();
                    getTimeline().setCycleCount(Timeline.INDEFINITE);
                    getTimeline().play();
                } else {
                    getTimeline().stop();
                    getTimeline().setCycleCount(1);
                    getTimeline().play();
                }
                System.out.println("    after = "+getTimeline().getCycleCount());
            }
        });
    }
}
