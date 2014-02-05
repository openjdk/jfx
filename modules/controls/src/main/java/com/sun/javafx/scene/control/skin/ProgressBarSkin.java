/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.behavior.ProgressBarBehavior;
import javafx.css.Styleable;


public class ProgressBarSkin extends BehaviorSkinBase<ProgressBar, ProgressBarBehavior<ProgressBar>> {

    /***************************************************************************
     *                                                                         *
     * UI Subcomponents                                                        *
     *                                                                         *
     **************************************************************************/

    private Bar bar;
    private StackPane track;
    private Region clipRegion;

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
                public CssMetaData<ProgressBar,Number> getCssMetaData() {
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
                public CssMetaData<ProgressBar,Boolean> getCssMetaData() {
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
                public CssMetaData<ProgressBar,Boolean> getCssMetaData() {
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
                public CssMetaData<ProgressBar,Number> getCssMetaData() {
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


        barWidth = ((int) (control.getWidth() - snappedLeftInset() - snappedRightInset()) * 2 * Math.min(1, Math.max(0, control.getProgress()))) / 2.0F;

        InvalidationListener listener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                updateProgress();
            }
        };
        control.widthProperty().addListener(listener);
        control.progressProperty().addListener(listener);

        initialize();
        getSkinnable().requestLayout();
    }

    private void initialize() {
        ProgressBar control = getSkinnable();

        track = new StackPane();
        track.getStyleClass().setAll("track");

        bar = new Bar(this);
        bar.getStyleClass().setAll("bar");

        getChildren().setAll(track, bar);

        // create a region to use as the clip for skin for animated indeterminate state
        clipRegion = new Region();
        // listen to the backgrounds on the bar and apply them to the clip but making them solid black for 100%
        // solid anywhere the bar draws
        bar.backgroundProperty().addListener(
                new ChangeListener<Background>() {
                    @Override public void changed(ObservableValue<? extends Background> observable, Background oldValue, Background newValue) {
                        if (newValue != null && !newValue.getFills().isEmpty()) {
                            final BackgroundFill[] fills = new BackgroundFill[newValue.getFills().size()];
                            for (int i = 0; i < newValue.getFills().size(); i++) {
                                BackgroundFill bf = newValue.getFills().get(i);
                                fills[i] = new BackgroundFill(Color.BLACK,bf.getRadii(),bf.getInsets());
                            }
                            clipRegion.setBackground(new Background(fills));
                        }
                    }
                });
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

    private boolean isVisibleInClip() {
        Parent p1 = getSkinnable();

        Bounds ourBounds1 = p1.localToScene(getSkinnable().getLayoutBounds());
        while (p1 != null) {
            Node clip = p1.getClip();
            if (clip != null) {
                Bounds clipBounds1 = p1.localToScene(clip.getLayoutBounds());
                if (!ourBounds1.intersects(clipBounds1)) {
                    return false;
                }
            }
            p1 = p1.getParent();
        }
        return true;
    }

    private boolean isInvisibleOrDisconnected() {
        Scene s = getSkinnable().getScene();
        if (s == null) {
            return true;
        }
        Window w = s.getWindow();
        if (w == null) {
            return true;
        }
        if (w.impl_getPeer() == null) {
            return true;
        }
        if (!getSkinnable().impl_isTreeVisible()) {
            return true;
        }

        return false;
    }

    private boolean stopIfInvisibleOrDisconnected() {
        if (isInvisibleOrDisconnected()) {
            if (indeterminateTimeline != null) {
            indeterminateTimeline.stop();
            indeterminateTimeline = null;
            }
            return true;
        }
        return false;
    }

    static final private Duration CLIPPED_DELAY = new Duration(300);
    static final private Duration UNCLIPPED_DELAY = new Duration(0);

    private void createIndeterminateTimeline() {
        if (indeterminateTimeline != null) indeterminateTimeline.stop();

        ProgressBar control = getSkinnable();
        final double w = control.getWidth() - (snappedLeftInset() + snappedRightInset());
        final double startX = getIndeterminateBarEscape()? -getIndeterminateBarLength() : 0;
        final double endX = getIndeterminateBarEscape()? w : w - getIndeterminateBarLength();

        // Set up the timeline.  We do not want to reverse if we are not flipping.
        indeterminateTimeline = new Timeline();
        indeterminateTimeline.setCycleCount(Timeline.INDEFINITE);
        indeterminateTimeline.setDelay(UNCLIPPED_DELAY);

        if (getIndeterminateBarFlip()) {
            indeterminateTimeline.getKeyFrames().addAll(
                    new KeyFrame(
                            Duration.millis(0),
                            new EventHandler<ActionEvent>() {
                                @Override public void handle(ActionEvent event) {
                                    bar.setScaleX(-1);

                                    /**
                                     * Stop the animation if the ProgressBar is removed
                                     * from a Scene, or is invisible.
                                     * Pause the animation if it's outside of a clipped
                                     * region (e.g. not visible in a ScrollPane)
                                    */
                                    if (indeterminateTimeline != null) {
                                        stopIfInvisibleOrDisconnected();
                                        if (!isVisibleInClip()) {
                                            Platform.runLater(new Runnable() {
                                              @Override public void run() {
                                                  if (indeterminateTimeline != null) {
                                                      if (indeterminateTimeline.getDelay().compareTo(CLIPPED_DELAY) != 0) {
                                                          indeterminateTimeline.setDelay(CLIPPED_DELAY);
                                                      }
                                                      indeterminateTimeline.stop();
                                                      indeterminateTimeline.jumpTo(Duration.ZERO);
                                                      indeterminateTimeline.play();
                                                  }
                                              }
                                            });
                                        }
                                        else {
                                            Platform.runLater(new Runnable() {
                                              @Override public void run() {
                                                  if (indeterminateTimeline != null) {
                                                      if (indeterminateTimeline.getDelay().compareTo(UNCLIPPED_DELAY) != 0) {
                                                          indeterminateTimeline.setDelay(UNCLIPPED_DELAY);
                                                      }
                                                  }
                                              }
                                            });
                                        }
                                    }
                                }
                            },
                            new KeyValue(clipRegion.translateXProperty(), startX-(w - getIndeterminateBarLength())),
                            new KeyValue(bar.translateXProperty(), startX)
                    ),
                    new KeyFrame(
                            Duration.millis(getIndeterminateBarAnimationTime() * 1000),
                            new EventHandler<ActionEvent>() {
                                @Override public void handle(ActionEvent event) {
                                    bar.setScaleX(1);                            }
                            },
                            new KeyValue(clipRegion.translateXProperty(), endX-(w - getIndeterminateBarLength())),
                            new KeyValue(bar.translateXProperty(), endX)
                    ),
                    new KeyFrame(
                            Duration.millis((getIndeterminateBarAnimationTime() * 1000)+1),
                            new KeyValue(clipRegion.translateXProperty(), -endX)
                    ),
                    new KeyFrame(
                            Duration.millis(getIndeterminateBarAnimationTime() * 2000),
                            new KeyValue(clipRegion.translateXProperty(), -startX),
                            new KeyValue(bar.translateXProperty(), startX)
                    )
            );
        } else {
            indeterminateTimeline.getKeyFrames().addAll(
                    new KeyFrame(
                            Duration.millis(0),
                            new EventHandler<ActionEvent>() {
                                @Override public void handle(ActionEvent event) {
                                    /**
                                     * Stop the animation if the ProgressBar is removed
                                     * from a Scene, or is invisible.
                                     * Pause the animation if it's outside of a clipped
                                     * region (e.g. not visible in a ScrollPane)
                                    */
                                    if (indeterminateTimeline != null) {
                                        stopIfInvisibleOrDisconnected();
                                        if (!isVisibleInClip()) {
                                            Platform.runLater(new Runnable() {
                                              @Override public void run() {
                                                  if (indeterminateTimeline != null) {
                                                      if (indeterminateTimeline.getDelay().compareTo(CLIPPED_DELAY) != 0) {
                                                          indeterminateTimeline.setDelay(CLIPPED_DELAY);
                                                      }
                                                      indeterminateTimeline.stop();
                                                      indeterminateTimeline.jumpTo(Duration.ZERO);
                                                      indeterminateTimeline.play();
                                                  }
                                              }
                                            });
                                        }
                                        else {
                                            Platform.runLater(new Runnable() {
                                              @Override public void run() {
                                                  if (indeterminateTimeline != null) {
                                                      if (indeterminateTimeline.getDelay().compareTo(UNCLIPPED_DELAY) != 0) {
                                                          indeterminateTimeline.setDelay(UNCLIPPED_DELAY);
                                                      }
                                                  }
                                              }
                                            });
                                        }
                                    }
                                }
                            },

                            new KeyValue(clipRegion.translateXProperty(), startX-(w - getIndeterminateBarLength())),
                            new KeyValue(bar.translateXProperty(), startX)
                    ),
                    new KeyFrame(
                            Duration.millis(getIndeterminateBarAnimationTime() * 1000*2),
                            new KeyValue(clipRegion.translateXProperty(), endX-(w - getIndeterminateBarLength())),
                            new KeyValue(bar.translateXProperty(), endX)
                    )
            );
        }

    }

    boolean wasIndeterminate = false;
    private void updateProgress() {
        ProgressBar control = getSkinnable();
        // RT-33789: if the ProgressBar was indeterminate and still is indeterminate, don't update the bar width
        final boolean isIndeterminate = control.isIndeterminate();
        if (!(isIndeterminate && wasIndeterminate)) {
            barWidth = ((int) (control.getWidth() - snappedLeftInset() - snappedRightInset()) * 2 * Math.min(1, Math.max(0, control.getProgress()))) / 2.0F;
            getSkinnable().requestLayout();
        }
        wasIndeterminate = isIndeterminate;
    }

    @Override
    public double computeBaselineOffset(double topInset, double rightInset, double bottomInset, double leftInset) {
        return Node.BASELINE_OFFSET_SAME_AS_HEIGHT;
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

    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Math.max(100, leftInset + bar.prefWidth(getSkinnable().getWidth()) + rightInset);
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + bar.prefHeight(width) + bottomInset;
    }

    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefWidth(height);
    }

    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefHeight(width);
    }

    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {

        final ProgressBar control = getSkinnable();
        boolean isIndeterminate = control.isIndeterminate();

        // resize clip
        clipRegion.resizeRelocate(0,0, w, h);

        track.resizeRelocate(x, y, w, h);
        bar.resizeRelocate(x, y, isIndeterminate? getIndeterminateBarLength() : barWidth,h);

        // things should be invisible only when well below minimum length
        track.setVisible(true);

        // width might have changed so recreate our animation if needed
        if (isIndeterminate) {
            createIndeterminateTimeline();
            if (getSkinnable().impl_isTreeVisible()) {
                indeterminateTimeline.play();
            }
            // apply clip
            if (getIndeterminateBarFlip()) {
                bar.setClip(clipRegion);
            } else {
                bar.setClip(null);
            }
        } else if (indeterminateTimeline != null) {
            indeterminateTimeline.stop();
            indeterminateTimeline = null;
            // remove clip
            bar.setClip(null);
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
                return (StyleableProperty<Number>)skin.indeterminateBarLengthProperty();
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
                return (StyleableProperty<Boolean>)skin.indeterminateBarEscapeProperty();
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
                return (StyleableProperty<Boolean>)skin.indeterminateBarFlipProperty();
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
                return (StyleableProperty<Number>)skin.indeterminateBarAnimationTimeProperty();
            }
        };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(SkinBase.getClassCssMetaData());
            styleables.add(INDETERMINATE_BAR_LENGTH);
            styleables.add(INDETERMINATE_BAR_ESCAPE);
            styleables.add(INDETERMINATE_BAR_FLIP);
            styleables.add(INDETERMINATE_BAR_ANIMATION_TIME);
            STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

}
