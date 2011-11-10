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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.scene.control.behavior.ProgressBarBehavior;


public class ProgressBarSkin extends SkinBase<ProgressBar, ProgressBarBehavior<ProgressBar>> {

    /***************************************************************************
     *                                                                         *
     * UI Subcomponents                                                        *
     *                                                                         *
     **************************************************************************/

    private StackPane bar;
    private StackPane track;
    private Rectangle clipRectangle;

    // clean up progress so we never go out of bounds or update graphics more than twice per pixel
    private double barWidth;

    /** The length of the bouncing progress bar in indeterminate state */
    @Styleable(property="-fx-indeterminate-bar-length", initial="60")
    private double indeterminateBarLength = 60.0;

    /** If the progress bar should escape the ends of the progress bar region in indeterminate state*/
    @Styleable(property="-fx-indeterminate-bar-escape", initial="true")
    private boolean indeterminateBarEscape = true;

    /** If the progress bar should flip when it gets to the ends in indeterminate state*/
    @Styleable(property="-fx-indeterminate-bar-flip", initial="true")
    private boolean indeterminateBarFlip = true;

    /**
     * How many seconds it should take for the indeterminate bar to go from
     * one edge to the other
     */
    @Styleable(property="-fx-indeterminate-bar-animation-time", initial="2")
    private double indeterminateBarAnimationTime = 2;

    private Timeline indeterminateTimeline;
    private boolean timelineNulled = false;

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ProgressBarSkin(ProgressBar control) {
        super(control, new ProgressBarBehavior<ProgressBar>(control));

        InvalidationListener indeterminateListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                initialize();
            }
        };
        control.indeterminateProperty().addListener(indeterminateListener);

        InvalidationListener visibilityListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                if (getSkinnable().isIndeterminate() && timelineNulled && indeterminateTimeline == null) {
                    timelineNulled = false;
                    createIndeterminateTimeline();
                }
                
                if (indeterminateTimeline != null) {
                    if (getSkinnable().isVisible() && getSkinnable().getScene() != null) {
                        indeterminateTimeline.play();
                    }
                    else {
                        indeterminateTimeline.pause();
                        indeterminateTimeline = null;
                        timelineNulled = true;
                    }
                }
            }
        };
        control.visibleProperty().addListener(visibilityListener);
        control.parentProperty().addListener(visibilityListener);

        InvalidationListener sceneListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                if (indeterminateTimeline != null) {
                    if (getSkinnable().getScene() == null) {
                        indeterminateTimeline.pause();
                        indeterminateTimeline = null;
                        timelineNulled = true;
                    }
                }
                else {
                    if (getSkinnable().getScene() != null && getSkinnable().isIndeterminate()) {
                        timelineNulled = false;
                        createIndeterminateTimeline();
                        indeterminateTimeline.play();
                        requestLayout();
                    }
                }
            }
        };
        control.sceneProperty().addListener(sceneListener);


        barWidth = ((int) (control.getWidth() - getInsets().getLeft() - getInsets().getRight()) * 2 * Math.min(1, Math.max(0, control.getProgress()))) / 2.0F;

        InvalidationListener listener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                updateProgress();
            }
        };
        control.widthProperty().addListener(listener);
        control.progressProperty().addListener(listener);

        /**
         * used to prevent an indeterminate bar from drawing outside the skin
         */
        clipRectangle = new Rectangle();
        setClip(clipRectangle);

        initialize();
        requestLayout();
    }

    private void initialize() {
        ProgressBar control = getSkinnable();
        boolean isIndeterminate = control.isIndeterminate();

        track = new StackPane();
        track.getStyleClass().setAll("track");

        bar = new StackPane();
        bar.getStyleClass().setAll("bar");

        getChildren().setAll(track, bar);
    }

    private void createIndeterminateTimeline() {
        if (indeterminateTimeline != null) indeterminateTimeline.stop();

        final double w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
        final double startX = indeterminateBarEscape? -indeterminateBarLength : 0;
        final double endX = indeterminateBarEscape? w : w - indeterminateBarLength;

        // Set up the timeline.  We do not want to reverse if we are not flipping.
        indeterminateTimeline = new Timeline();
        indeterminateTimeline.setAutoReverse(indeterminateBarFlip);
        indeterminateTimeline.setCycleCount(Timeline.INDEFINITE);

        ObservableList<KeyFrame> keyframes = FXCollections.<KeyFrame>observableArrayList();

        // Make sure the shading of the bar points in the right direction.
        if (!indeterminateBarFlip) bar.setScaleX(-1);
        keyframes.add(new KeyFrame(Duration.millis(0), new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                if (indeterminateBarFlip) bar.setScaleX(bar.getScaleX() * -1);
            }
        }, new KeyValue(bar.translateXProperty(), startX)));

        keyframes.add(new KeyFrame(Duration.millis(indeterminateBarAnimationTime * 1000), new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
              if (indeterminateBarFlip) bar.setScaleX(bar.getScaleX() * -1);
            }
        }, new KeyValue(bar.translateXProperty(), endX)));

        indeterminateTimeline.getKeyFrames().addAll(keyframes);
    }

    private void updateProgress() {
        ProgressBar control = getSkinnable();
        barWidth = ((int) (control.getWidth() - getInsets().getLeft() - getInsets().getRight()) * 2 * Math.min(1, Math.max(0, control.getProgress()))) / 2.0F;
        requestLayout();
    }

    @Override public void dispose() {
        super.dispose();
        
        if (indeterminateTimeline != null) {
            indeterminateTimeline.stop();
            indeterminateTimeline.getKeyFrames().clear();
            indeterminateTimeline = null;
        }
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

    @Override protected double computeMaxWidth(double height) {
        return getSkinnable().prefWidth(height);
    }

    @Override protected double computeMaxHeight(double width) {
        return getSkinnable().prefHeight(width);
    }

    @Override protected void layoutChildren() {
        boolean isIndeterminate = getSkinnable().isIndeterminate();

        // compute x,y,w,h of content area
        double x = getInsets().getLeft();
        double y = getInsets().getTop();
        double w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
        double h = getHeight() - (getInsets().getTop() + getInsets().getBottom());

        // Prevent the indeterminate bar from drawing outside the skin
        if (clipRectangle != null) {
            clipRectangle.setWidth(getWidth());
            clipRectangle.setHeight(getHeight());
        }

        track.resizeRelocate(x, y, w, h);

        bar.resizeRelocate(x, y, isIndeterminate? indeterminateBarLength : barWidth,
                   getHeight() - (getInsets().getTop() + getInsets().getBottom()));

        // things should be invisible only when well below minimum length
        track.setVisible(true);

        // width might have changed so recreate our animation if needed
        if (isIndeterminate) {
            createIndeterminateTimeline();
            if (getSkinnable().isVisible()) {
                indeterminateTimeline.play();
            }
        } else if (indeterminateTimeline != null) {
            indeterminateTimeline.stop();
            indeterminateTimeline = null;
        }
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatasprivate implementation detail
      */
     private static class StyleableProperties {
         private static final StyleableProperty INDETERMINATE_BAR_LENGTH =
            new StyleableProperty(ProgressBarSkin.class, "indeterminateBarLength");
         private static final StyleableProperty INDETERMINATE_BAR_ESCAPE =
            new StyleableProperty(ProgressBarSkin.class, "indeterminateBarEscape");
         private static final StyleableProperty INDETERMINATE_BAR_FLIP =
            new StyleableProperty(ProgressBarSkin.class, "indeterminateBarFlip");
         private static final StyleableProperty INDETERMINATE_BAR_ANIMATION_TIME =
            new StyleableProperty(ProgressBarSkin.class, "indeterminateBarAnimationTime");

         private static final List<StyleableProperty> STYLEABLES;
         static {

            final List<StyleableProperty> styleables = 
                new ArrayList<StyleableProperty>(SkinBase.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                INDETERMINATE_BAR_LENGTH,
                INDETERMINATE_BAR_ESCAPE,
                INDETERMINATE_BAR_FLIP,
                INDETERMINATE_BAR_ANIMATION_TIME
            );
            STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return StyleableProperties.STYLEABLES;
    };

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSet(String property, Object value) {
        if ("-fx-indeterminate-bar-length".equals(property) ) {
            indeterminateBarLength = (Double) value;
        } else if ("-fx-indeterminate-bar-escape".equals(property)) {
            indeterminateBarEscape = (Boolean) value;
        } else if ("-fx-indeterminate-bar-flip".equals(property)) {
            indeterminateBarFlip = (Boolean) value;
        } else if ("-fx-indeterminate-bar-animation-time".equals(property)) {
            indeterminateBarAnimationTime = (Double) value;
        } else {
            return super.impl_cssSet(property,value);
        }
        return true;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSettable(String property) {
        if ("-fx-indeterminate-bar-length".equals(property) ) {
            return true;
        } else if ("-fx-indeterminate-bar-escape".equals(property) ) {
            return true;
        } else if ("-fx-indeterminate-bar-flip".equals(property) ) {
            return true;
        }  else if ("-fx-indeterminate-bar-animation-time".equals(property)) {
            return true;
        } else {
            return super.impl_cssSettable(property);
        }
    }
}
