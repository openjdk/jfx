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

import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.CssMetaData;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.behavior.ProgressBarBehavior;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Insets;
import javafx.scene.control.SkinBase;


public class ProgressBarSkin extends BehaviorSkinBase<ProgressBar, ProgressBarBehavior<ProgressBar>> {

    /***************************************************************************
     *                                                                         *
     * UI Subcomponents                                                        *
     *                                                                         *
     **************************************************************************/

    private Bar bar;
    private StackPane track;
    private Rectangle clipRectangle;

    // clean up progress so we never go out of bounds or update graphics more than twice per pixel
    private double barWidth;

    /** The length of the bouncing progress bar in indeterminate state */
    private DoubleProperty indeterminateBarLength = null;
    private DoubleProperty indeterminateBarLengthProperty() {
        if (indeterminateBarLength == null) {
            indeterminateBarLength = new StyleableDoubleProperty(60.0) {

                @Override
                public Object getBean() {
                    return ProgressBarSkin.this;
                }

                @Override
                public String getName() {
                    return "indeterminateBarLength";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.INDETERMINATE_BAR_LENGTH;
                }
            
            };
        }
        return indeterminateBarLength;
    }
    
    private Double getIndeterminateBarLength() {
        return indeterminateBarLength == null ? 60.0 : indeterminateBarLength.get();
    }
    
    /** If the progress bar should escape the ends of the progress bar region in indeterminate state*/
    private BooleanProperty indeterminateBarEscape = null;
    private BooleanProperty indeterminateBarEscapeProperty() {
        if (indeterminateBarEscape == null) {
            indeterminateBarEscape = new StyleableBooleanProperty(true) {

                @Override
                public Object getBean() {
                    return ProgressBarSkin.this;
                }

                @Override
                public String getName() {
                    return "indeterminateBarEscape";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.INDETERMINATE_BAR_ESCAPE;
                }
            
            
            };
        }
        return indeterminateBarEscape;
    }

    private Boolean getIndeterminateBarEscape() {
        return indeterminateBarEscape == null ? true : indeterminateBarEscape.get();
    }

    /** If the progress bar should flip when it gets to the ends in indeterminate state*/
    private BooleanProperty indeterminateBarFlip = null;
    private BooleanProperty indeterminateBarFlipProperty() {
        if (indeterminateBarFlip == null) {
            indeterminateBarFlip = new StyleableBooleanProperty(true) {

                @Override
                public Object getBean() {
                    return ProgressBarSkin.this;
                }

                @Override
                public String getName() {
                    return "indeterminateBarFlip";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.INDETERMINATE_BAR_FLIP;
                }
                        
            };
        }
        return indeterminateBarFlip;
    }
    
    private Boolean getIndeterminateBarFlip() {
        return indeterminateBarFlip == null ? true : indeterminateBarFlip.get();
    }

    /**
     * How many seconds it should take for the indeterminate bar to go from
     * one edge to the other
     */
    private DoubleProperty indeterminateBarAnimationTime = null;
    private DoubleProperty indeterminateBarAnimationTimeProperty() {
        if (indeterminateBarAnimationTime == null) {
            indeterminateBarAnimationTime = new StyleableDoubleProperty(2.0) {

                @Override
                public Object getBean() {
                    return ProgressBarSkin.this;
                }

                @Override
                public String getName() {
                    return "indeterminateBarAnimationTime";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.INDETERMINATE_BAR_ANIMATION_TIME;
                }
            
            
            };
        }
        return indeterminateBarAnimationTime;
    }
    
    private Double getIndeterminateBarAnimationTime() {
        return indeterminateBarAnimationTime == null ? 2.0 : indeterminateBarAnimationTime.get();
    };

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
                    if (getSkinnable().impl_isTreeVisible() && getSkinnable().getScene() != null) {
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
                        if (getSkinnable().impl_isTreeVisible()) {
                            indeterminateTimeline.play();
                        }
                        getSkinnable().requestLayout();
                    }
                }
            }
        };
        control.sceneProperty().addListener(sceneListener);


        final Insets padding = getSkinnable().getInsets();
        barWidth = ((int) (control.getWidth() - padding.getLeft() - padding.getRight()) * 2 * Math.min(1, Math.max(0, control.getProgress()))) / 2.0F;

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
        getSkinnable().setClip(clipRectangle);

        initialize();
        getSkinnable().requestLayout();
    }

    private void initialize() {
        ProgressBar control = getSkinnable();
        boolean isIndeterminate = control.isIndeterminate();

        track = new StackPane();
        track.getStyleClass().setAll("track");

        bar = new Bar(this);
        bar.getStyleClass().setAll("bar");

        getChildren().setAll(track, bar);
    }

    void pauseBar(boolean pause) {
        if (indeterminateTimeline != null) {
            if (pause) {
                indeterminateTimeline.pause();
            }
            else {
                indeterminateTimeline.play();
            }
        }
    }

    class Bar extends StackPane {
        ProgressBarSkin pbSkin;
        Bar(ProgressBarSkin pb) {
            super();
            pbSkin = pb;
            InvalidationListener treeVisibilityListener = new InvalidationListener() {
                    @Override public void invalidated(Observable valueModel) {
                        if (getSkinnable().impl_isTreeVisible()) {
                            pbSkin.pauseBar(false);
                        }
                        else {
                            pbSkin.pauseBar(true);
                        }
                    }
                };
            impl_treeVisibleProperty().addListener(treeVisibilityListener);
        }
    }

    private void createIndeterminateTimeline() {
        if (indeterminateTimeline != null) indeterminateTimeline.stop();

        ProgressBar control = getSkinnable();
        Insets padding = control.getInsets();
        final double w = control.getWidth() - (padding.getLeft() + padding.getRight());
        final double startX = getIndeterminateBarEscape()? -getIndeterminateBarLength() : 0;
        final double endX = getIndeterminateBarEscape()? w : w - getIndeterminateBarLength();

        // Set up the timeline.  We do not want to reverse if we are not flipping.
        indeterminateTimeline = new Timeline();
        indeterminateTimeline.setAutoReverse(getIndeterminateBarFlip());
        indeterminateTimeline.setCycleCount(Timeline.INDEFINITE);

        ObservableList<KeyFrame> keyframes = FXCollections.<KeyFrame>observableArrayList();

        // Make sure the shading of the bar points in the right direction.
        if (!getIndeterminateBarFlip()) {
            bar.setScaleX(-1.0);
        }
        else {
            bar.setScaleX(1.0);
        }

        keyframes.add(new KeyFrame(Duration.millis(0), new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                if (getIndeterminateBarFlip()) bar.setScaleX(bar.getScaleX() * -1);
            }
        }, new KeyValue(bar.translateXProperty(), startX)));

        keyframes.add(new KeyFrame(Duration.millis(getIndeterminateBarAnimationTime() * 1000), new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
              if (getIndeterminateBarFlip()) bar.setScaleX(bar.getScaleX() * -1);
            }
        }, new KeyValue(bar.translateXProperty(), endX)));

        indeterminateTimeline.getKeyFrames().addAll(keyframes);
    }

    private void updateProgress() {
        ProgressBar control = getSkinnable();
        Insets padding = control.getInsets();
        barWidth = ((int) (control.getWidth() - padding.getLeft() - padding.getRight()) * 2 * Math.min(1, Math.max(0, control.getProgress()))) / 2.0F;
        getSkinnable().requestLayout();
    }

    @Override
    public double getBaselineOffset() {
        double height = getSkinnable().getHeight();        
        return getSkinnable().getInsets().getTop() + height;
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
        ProgressBar control = getSkinnable();
        Insets padding = control.getInsets();
        return Math.max(100, padding.getLeft() + bar.prefWidth(control.getWidth()) + padding.getRight());
    }

    @Override protected double computePrefHeight(double width) {
        Insets padding = getSkinnable().getInsets();
        return padding.getTop() + bar.prefHeight(width) + padding.getBottom();
    }

    @Override protected double computeMaxWidth(double height) {
        return getSkinnable().prefWidth(height);
    }

    @Override protected double computeMaxHeight(double width) {
        return getSkinnable().prefHeight(width);
    }

    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        
        final ProgressBar control = getSkinnable();
        final Insets padding = control.getInsets();
        boolean isIndeterminate = control.isIndeterminate();

        // Prevent the indeterminate bar from drawing outside the skin
        if (clipRectangle != null) {
            clipRectangle.setWidth(control.getWidth());
            clipRectangle.setHeight(control.getHeight());
        }

        track.resizeRelocate(x, y, w, h);

        bar.resizeRelocate(x, y, isIndeterminate? getIndeterminateBarLength() : barWidth,
                   control.getHeight() - (padding.getTop() + padding.getBottom()));

        // things should be invisible only when well below minimum length
        track.setVisible(true);

        // width might have changed so recreate our animation if needed
        if (isIndeterminate) {
            createIndeterminateTimeline();
            if (getSkinnable().impl_isTreeVisible()) {
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
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {
         private static final CssMetaData<ProgressBar,Number> INDETERMINATE_BAR_LENGTH =
            new CssMetaData<ProgressBar,Number>("-fx-indeterminate-bar-length",
                 SizeConverter.getInstance(), 60.0) {

            @Override
            public boolean isSettable(ProgressBar n) {
                final ProgressBarSkin skin = (ProgressBarSkin) n.getSkin();
                return skin.indeterminateBarLength == null ||
                        !skin.indeterminateBarLength.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(ProgressBar n) {
                final ProgressBarSkin skin = (ProgressBarSkin) n.getSkin();
                return (StyleableProperty)skin.indeterminateBarLengthProperty();
            }
        };
         
         private static final CssMetaData<ProgressBar,Boolean> INDETERMINATE_BAR_ESCAPE =
            new CssMetaData<ProgressBar,Boolean>("-fx-indeterminate-bar-escape",
                 BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(ProgressBar n) {
                final ProgressBarSkin skin = (ProgressBarSkin) n.getSkin();
                return skin.indeterminateBarEscape == null || 
                        !skin.indeterminateBarEscape.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(ProgressBar n) {
                final ProgressBarSkin skin = (ProgressBarSkin) n.getSkin();
                return (StyleableProperty)skin.indeterminateBarEscapeProperty();
            }
        };
         
         private static final CssMetaData<ProgressBar,Boolean> INDETERMINATE_BAR_FLIP =
            new CssMetaData<ProgressBar,Boolean>("-fx-indeterminate-bar-flip",
                 BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(ProgressBar n) {
                final ProgressBarSkin skin = (ProgressBarSkin) n.getSkin();
                return skin.indeterminateBarFlip == null ||
                        !skin.indeterminateBarFlip.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(ProgressBar n) {
                final ProgressBarSkin skin = (ProgressBarSkin) n.getSkin();
                return (StyleableProperty)skin.indeterminateBarFlipProperty();
            }
        }; 
         
         private static final CssMetaData<ProgressBar,Number> INDETERMINATE_BAR_ANIMATION_TIME =
            new CssMetaData<ProgressBar,Number>("-fx-indeterminate-bar-animation-time",
                 SizeConverter.getInstance(), 2.0) {

            @Override
            public boolean isSettable(ProgressBar n) {
                final ProgressBarSkin skin = (ProgressBarSkin) n.getSkin();
                return skin.indeterminateBarAnimationTime == null ||
                        !skin.indeterminateBarAnimationTime.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(ProgressBar n) {
                final ProgressBarSkin skin = (ProgressBarSkin) n.getSkin();
                return (StyleableProperty)skin.indeterminateBarAnimationTimeProperty();
            }
        };

         private static final List<CssMetaData> STYLEABLES;
         static {

            final List<CssMetaData> styleables = 
                new ArrayList<CssMetaData>(SkinBase.getClassCssMetaData());
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
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData> getCssMetaData() {
        return getClassCssMetaData();
    }

}
