/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import com.sun.javafx.scene.NodeHelper;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.WritableValue;
import javafx.css.CssMetaData;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import javafx.css.converter.BooleanConverter;
import javafx.css.converter.SizeConverter;

import javafx.css.Styleable;

/**
 * Default skin implementation for the {@link ProgressBar} control.
 *
 * @see ProgressBar
 * @since 9
 */
public class ProgressBarSkin extends ProgressIndicatorSkin {

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private StackPane bar;
    private StackPane track;
    private Region clipRegion;

    // clean up progress so we never go out of bounds or update graphics more than twice per pixel
    private double barWidth;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ProgressBarSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ProgressBarSkin(ProgressBar control) {
        super(control);

        barWidth = ((int) (control.getWidth() - snappedLeftInset() - snappedRightInset()) * 2 * Math.min(1, Math.max(0, control.getProgress()))) / 2.0F;

        registerChangeListener(control.widthProperty(), o -> updateProgress());

        initialize();
        getSkinnable().requestLayout();
    }



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The length of the bouncing progress bar in indeterminate state
     */
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

    /**
     * If the progress bar should escape the ends of the progress bar region in indeterminate state
     */
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

    /**
     * If the progress bar should flip when it gets to the ends in indeterminate state
     */
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

    private double getIndeterminateBarAnimationTime() {
        return indeterminateBarAnimationTime == null ? 2.0 : indeterminateBarAnimationTime.get();
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public double computeBaselineOffset(double topInset, double rightInset, double bottomInset, double leftInset) {
        return Node.BASELINE_OFFSET_SAME_AS_HEIGHT;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Math.max(100, leftInset + bar.prefWidth(getSkinnable().getWidth()) + rightInset);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + bar.prefHeight(width) + bottomInset;
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefWidth(height);
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefHeight(width);
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {

        final ProgressIndicator control = getSkinnable();
        boolean isIndeterminate = control.isIndeterminate();

        // resize clip
        clipRegion.resizeRelocate(0, 0, w, h);

        track.resizeRelocate(x, y, w, h);
        bar.resizeRelocate(x, y, isIndeterminate ? getIndeterminateBarLength() : barWidth, h);

        // things should be invisible only when well below minimum length
        track.setVisible(true);

        // width might have changed so recreate our animation if needed
        if (isIndeterminate) {
            createIndeterminateTimeline();
            if (NodeHelper.isTreeShowing(getSkinnable())) {
                indeterminateTransition.play();
            }

            // apply clip
            bar.setClip(clipRegion);
        } else if (indeterminateTransition != null) {
            indeterminateTransition.stop();
            indeterminateTransition = null;

            // remove clip
            bar.setClip(null);
            bar.setScaleX(1);
            bar.setTranslateX(0);
            clipRegion.translateXProperty().unbind();
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override void initialize() {
        track = new StackPane();
        track.getStyleClass().setAll("track");

        bar = new StackPane();
        bar.getStyleClass().setAll("bar");

        getChildren().setAll(track, bar);

        // create a region to use as the clip for skin for animated indeterminate state
        clipRegion = new Region();

        // listen to the backgrounds on the bar and apply them to the clip but making them solid black for 100%
        // solid anywhere the bar draws
        bar.backgroundProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.getFills().isEmpty()) {
                final BackgroundFill[] fills = new BackgroundFill[newValue.getFills().size()];
                for (int i = 0; i < newValue.getFills().size(); i++) {
                    BackgroundFill bf = newValue.getFills().get(i);
                    fills[i] = new BackgroundFill(Color.BLACK,bf.getRadii(),bf.getInsets());
                }
                clipRegion.setBackground(new Background(fills));
            }
        });
    }

    /** {@inheritDoc} */
    @Override void createIndeterminateTimeline() {
        if (indeterminateTransition != null) indeterminateTransition.stop();

        ProgressIndicator control = getSkinnable();
        final double w = control.getWidth() - (snappedLeftInset() + snappedRightInset());
        final double startX = getIndeterminateBarEscape() ? -getIndeterminateBarLength() : 0;
        final double endX = getIndeterminateBarEscape() ? w : w - getIndeterminateBarLength();

        // Set up the timeline.  We do not want to reverse if we are not flipping.
        indeterminateTransition = new IndeterminateTransition(startX, endX, this);
        indeterminateTransition.setCycleCount(Timeline.INDEFINITE);

        clipRegion.translateXProperty().bind(new When(bar.scaleXProperty().isEqualTo(-1.0, 1e-100)).
                then(bar.translateXProperty().subtract(w).add(indeterminateBarLengthProperty())).
                otherwise(bar.translateXProperty().negate()));
    }

    boolean wasIndeterminate = false;

    /** {@inheritDoc} */
    @Override void updateProgress() {
        ProgressIndicator control = getSkinnable();
        // RT-33789: if the ProgressBar was indeterminate and still is indeterminate, don't update the bar width
        final boolean isIndeterminate = control.isIndeterminate();
        if (!(isIndeterminate && wasIndeterminate)) {
            barWidth = ((int) (control.getWidth() - snappedLeftInset() - snappedRightInset()) * 2 * Math.min(1, Math.max(0, control.getProgress()))) / 2.0F;
            getSkinnable().requestLayout();
        }
        wasIndeterminate = isIndeterminate;
    }



    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    /*
     * Super-lazy instantiation pattern from Bill Pugh.
     */
    private static class StyleableProperties {
        private static final CssMetaData<ProgressBar, Number> INDETERMINATE_BAR_LENGTH =
                new CssMetaData<ProgressBar, Number>("-fx-indeterminate-bar-length",
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
                        return (StyleableProperty<Number>) (WritableValue<Number>) skin.indeterminateBarLengthProperty();
                    }
                };

        private static final CssMetaData<ProgressBar, Boolean> INDETERMINATE_BAR_ESCAPE =
                new CssMetaData<ProgressBar, Boolean>("-fx-indeterminate-bar-escape",
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
                        return (StyleableProperty<Boolean>) (WritableValue<Boolean>) skin.indeterminateBarEscapeProperty();
                    }
                };

        private static final CssMetaData<ProgressBar, Boolean> INDETERMINATE_BAR_FLIP =
                new CssMetaData<ProgressBar, Boolean>("-fx-indeterminate-bar-flip",
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
                        return (StyleableProperty<Boolean>) (WritableValue<Boolean>) skin.indeterminateBarFlipProperty();
                    }
                };

        private static final CssMetaData<ProgressBar, Number> INDETERMINATE_BAR_ANIMATION_TIME =
                new CssMetaData<ProgressBar, Number>("-fx-indeterminate-bar-animation-time",
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
                        return (StyleableProperty<Number>) (WritableValue<Number>) skin.indeterminateBarAnimationTimeProperty();
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
     * Returns the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @return the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }



    /* *************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/

    private static class IndeterminateTransition extends Transition {
        private final WeakReference<ProgressBarSkin> skin;
        private final double startX;
        private final double endX;
        private final boolean flip;

        public IndeterminateTransition(double startX, double endX, ProgressBarSkin progressBarSkin) {
            this.startX = startX;
            this.endX = endX;
            this.skin = new WeakReference<>(progressBarSkin);
            this.flip = progressBarSkin.getIndeterminateBarFlip();
            progressBarSkin.getIndeterminateBarEscape();
            setCycleDuration(Duration.seconds(progressBarSkin.getIndeterminateBarAnimationTime() * (flip ? 2 : 1)));
        }

        @Override
        protected void interpolate(double frac) {
            ProgressBarSkin s = skin.get();
            if (s == null) {
                stop();
            } else {
                if (frac <= 0.5 || !flip) {
                    s.bar.setScaleX(-1);
                    s.bar.setTranslateX(startX + (flip ? 2 : 1) * frac * (endX - startX));
                } else {
                    s.bar.setScaleX(1);
                    s.bar.setTranslateX(startX + 2 * (1 - frac) * (endX - startX));
                }
            }
        }
    }
}
