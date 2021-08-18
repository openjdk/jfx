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

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import javafx.animation.Transition;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.StringConverter;

import com.sun.javafx.scene.control.behavior.SliderBehavior;

/**
 * Default skin implementation for the {@link Slider} control.
 *
 * @see Slider
 * @since 9
 */
public class SliderSkin extends SkinBase<Slider> {

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    /** Track if slider is vertical/horizontal and cause re layout */
//    private boolean horizontal;
    private NumberAxis tickLine = null;
    private double trackToTickGap = 2;

    private boolean showTickMarks;
    private double thumbWidth;
    private double thumbHeight;

    private double trackStart;
    private double trackLength;
    private double thumbTop;
    private double thumbLeft;
    private double preDragThumbPos;
    private Point2D dragStart; // in skin coordinates

    private StackPane thumb;
    private StackPane track;
    private boolean trackClicked = false;
//    private double visibleAmount = 16;

    private final SliderBehavior behavior;

    StringConverter<Number> stringConverterWrapper = new StringConverter<Number>() {
        Slider slider = getSkinnable();
        @Override public String toString(Number object) {
            return(object != null) ? slider.getLabelFormatter().toString(object.doubleValue()) : "";
        }
        @Override public Number fromString(String string) {
            return slider.getLabelFormatter().fromString(string);
        }
    };



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new SliderSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public SliderSkin(Slider control) {
        super(control);

        behavior = new SliderBehavior(control);
//        control.setInputMap(behavior.getInputMap());

        initialize();
        control.requestLayout();
        registerChangeListener(control.minProperty(), e -> {
            if (showTickMarks && tickLine != null) {
                tickLine.setLowerBound(control.getMin());
            }
            getSkinnable().requestLayout();
        });
        registerChangeListener(control.maxProperty(), e -> {
            if (showTickMarks && tickLine != null) {
                tickLine.setUpperBound(control.getMax());
            }
            getSkinnable().requestLayout();
        });
        registerChangeListener(control.valueProperty(), e -> {
            // only animate thumb if the track was clicked - not if the thumb is dragged
            positionThumb(trackClicked);
        });
        registerChangeListener(control.orientationProperty(), e -> {
            if (showTickMarks && tickLine != null) {
                tickLine.setSide(control.getOrientation() == Orientation.VERTICAL ? Side.RIGHT : (control.getOrientation() == null) ? Side.RIGHT: Side.BOTTOM);
            }
            getSkinnable().requestLayout();
        });
        registerChangeListener(control.showTickMarksProperty(), e -> setShowTickMarks(control.isShowTickMarks(), control.isShowTickLabels()));
        registerChangeListener(control.showTickLabelsProperty(), e -> setShowTickMarks(control.isShowTickMarks(), control.isShowTickLabels()));
        registerChangeListener(control.majorTickUnitProperty(), e -> {
            if (tickLine != null) {
                tickLine.setTickUnit(control.getMajorTickUnit());
                getSkinnable().requestLayout();
            }
        });
        registerChangeListener(control.minorTickCountProperty(), e -> {
            if (tickLine != null) {
                tickLine.setMinorTickCount(Math.max(control.getMinorTickCount(), 0) + 1);
                getSkinnable().requestLayout();
            }
        });
        registerChangeListener(control.labelFormatterProperty(), e -> {
            if (tickLine != null) {
                if (control.getLabelFormatter() == null) {
                    tickLine.setTickLabelFormatter(null);
                } else {
                    tickLine.setTickLabelFormatter(stringConverterWrapper);
                    tickLine.requestAxisLayout();
                }
            }
        });
        registerChangeListener(control.snapToTicksProperty(), e -> {
            control.adjustValue(control.getValue());
        });
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        // calculate the available space
        // resize thumb to preferred size
        thumbWidth = snapSizeX(thumb.prefWidth(-1));
        thumbHeight = snapSizeY(thumb.prefHeight(-1));
        thumb.resize(thumbWidth, thumbHeight);
        // we are assuming the is common radius's for all corners on the track
        double trackRadius = track.getBackground() == null ? 0 : track.getBackground().getFills().size() > 0 ?
                track.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius() : 0;

        if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
            double tickLineHeight =  (showTickMarks) ? tickLine.prefHeight(-1) : 0;
            double trackHeight = snapSizeY(track.prefHeight(-1));
            double trackAreaHeight = Math.max(trackHeight,thumbHeight);
            double totalHeightNeeded = trackAreaHeight  + ((showTickMarks) ? trackToTickGap+tickLineHeight : 0);
            double startY = y + ((h - totalHeightNeeded)/2); // center slider in available height vertically
            trackLength = snapSizeX(w - thumbWidth);
            trackStart = snapPositionX(x + (thumbWidth/2));
            double trackTop = (int)(startY + ((trackAreaHeight-trackHeight)/2));
            thumbTop = (int)(startY + ((trackAreaHeight-thumbHeight)/2));

            positionThumb(false);
            // layout track
            track.resizeRelocate((int)(trackStart - trackRadius),
                    trackTop ,
                    (int)(trackLength + trackRadius + trackRadius),
                    trackHeight);
            // layout tick line
            if (showTickMarks) {
                tickLine.setLayoutX(trackStart);
                tickLine.setLayoutY(trackTop+trackHeight+trackToTickGap);
                tickLine.resize(trackLength, tickLineHeight);
                tickLine.requestAxisLayout();
            } else {
                if (tickLine != null) {
                    tickLine.resize(0,0);
                    tickLine.requestAxisLayout();
                }
                tickLine = null;
            }
        } else {
            double tickLineWidth = (showTickMarks) ? tickLine.prefWidth(-1) : 0;
            double trackWidth = snapSizeX(track.prefWidth(-1));
            double trackAreaWidth = Math.max(trackWidth,thumbWidth);
            double totalWidthNeeded = trackAreaWidth  + ((showTickMarks) ? trackToTickGap+tickLineWidth : 0) ;
            double startX = x + ((w - totalWidthNeeded)/2); // center slider in available width horizontally
            trackLength = snapSizeY(h - thumbHeight);
            trackStart = snapPositionY(y + (thumbHeight/2));
            double trackLeft = (int)(startX + ((trackAreaWidth-trackWidth)/2));
            thumbLeft = (int)(startX + ((trackAreaWidth-thumbWidth)/2));

            positionThumb(false);
            // layout track
            track.resizeRelocate(trackLeft,
                    (int)(trackStart - trackRadius),
                    trackWidth,
                    (int)(trackLength + trackRadius + trackRadius));
            // layout tick line
            if (showTickMarks) {
                tickLine.setLayoutX(trackLeft+trackWidth+trackToTickGap);
                tickLine.setLayoutY(trackStart);
                tickLine.resize(tickLineWidth, trackLength);
                tickLine.requestAxisLayout();
            } else {
                if (tickLine != null) {
                    tickLine.resize(0,0);
                    tickLine.requestAxisLayout();
                }
                tickLine = null;
            }
        }
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        final Slider s = getSkinnable();
        if (s.getOrientation() == Orientation.HORIZONTAL) {
            return (leftInset + minTrackLength() + thumb.minWidth(-1) + rightInset);
        } else {
            return(leftInset + thumb.prefWidth(-1) + rightInset);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        final Slider s = getSkinnable();
        if (s.getOrientation() == Orientation.HORIZONTAL) {
            double axisHeight = showTickMarks ? (tickLine.prefHeight(-1) + trackToTickGap) : 0;
            return topInset + thumb.prefHeight(-1) + axisHeight + bottomInset;
        } else {
            return topInset + minTrackLength() + thumb.prefHeight(-1) + bottomInset;
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        final Slider s = getSkinnable();
        if (s.getOrientation() == Orientation.HORIZONTAL) {
            if(showTickMarks) {
                return Math.max(140, tickLine.prefWidth(-1));
            } else {
                return 140;
            }
        } else {
            double axisWidth = showTickMarks ? (tickLine.prefWidth(-1) + trackToTickGap) : 0;
            return leftInset + Math.max(thumb.prefWidth(-1), track.prefWidth(-1)) + axisWidth + rightInset;
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        final Slider s = getSkinnable();
        if (s.getOrientation() == Orientation.HORIZONTAL) {
            return topInset + Math.max(thumb.prefHeight(-1), track.prefHeight(-1)) +
                    ((showTickMarks) ? (trackToTickGap+tickLine.prefHeight(-1)) : 0)  + bottomInset;
        } else {
            if(showTickMarks) {
                return Math.max(140, tickLine.prefHeight(-1));
            } else {
                return 140;
            }
        }
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
            return Double.MAX_VALUE;
        } else {
            return getSkinnable().prefWidth(-1);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
            return getSkinnable().prefHeight(width);
        } else {
            return Double.MAX_VALUE;
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void initialize() {
        thumb = new StackPane() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
                switch (attribute) {
                    case VALUE: return getSkinnable().getValue();
                    default: return super.queryAccessibleAttribute(attribute, parameters);
                }
            }
        };
        thumb.getStyleClass().setAll("thumb");
        thumb.setAccessibleRole(AccessibleRole.THUMB);
        track = new StackPane();
        track.getStyleClass().setAll("track");
//        horizontal = getSkinnable().isVertical();

        getChildren().clear();
        getChildren().addAll(track, thumb);
        setShowTickMarks(getSkinnable().isShowTickMarks(), getSkinnable().isShowTickLabels());
        track.setOnMousePressed(me -> {
            if (!thumb.isPressed()) {
                trackClicked = true;
                if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
                    behavior.trackPress(me, (me.getX() / trackLength));
                } else {
                    behavior.trackPress(me, (me.getY() / trackLength));
                }
                trackClicked = false;
            }
        });

        track.setOnMouseDragged(me -> {
            if (!thumb.isPressed()) {
                if (getSkinnable().getOrientation() == Orientation.HORIZONTAL) {
                    behavior.trackPress(me, (me.getX() / trackLength));
                } else {
                    behavior.trackPress(me, (me.getY() / trackLength));
                }
            }
        });

        thumb.setOnMousePressed(me -> {
            behavior.thumbPressed(me, 0.0f);
            dragStart = thumb.localToParent(me.getX(), me.getY());
            preDragThumbPos = (getSkinnable().getValue() - getSkinnable().getMin()) /
                    (getSkinnable().getMax() - getSkinnable().getMin());
        });

        thumb.setOnMouseReleased(me -> {
            behavior.thumbReleased(me);
        });

        thumb.setOnMouseDragged(me -> {
            Point2D cur = thumb.localToParent(me.getX(), me.getY());
            double dragPos = (getSkinnable().getOrientation() == Orientation.HORIZONTAL) ?
                    cur.getX() - dragStart.getX() : -(cur.getY() - dragStart.getY());
            behavior.thumbDragged(me, preDragThumbPos + dragPos / trackLength);
        });
    }

    private void setShowTickMarks(boolean ticksVisible, boolean labelsVisible) {
        showTickMarks = (ticksVisible || labelsVisible);
        Slider slider = getSkinnable();
        if (showTickMarks) {
            if (tickLine == null) {
                tickLine = new NumberAxis();
                tickLine.setAutoRanging(false);
                tickLine.setSide(slider.getOrientation() == Orientation.VERTICAL ? Side.RIGHT : (slider.getOrientation() == null) ? Side.RIGHT: Side.BOTTOM);
                tickLine.setUpperBound(slider.getMax());
                tickLine.setLowerBound(slider.getMin());
                tickLine.setTickUnit(slider.getMajorTickUnit());
                tickLine.setTickMarkVisible(ticksVisible);
                tickLine.setTickLabelsVisible(labelsVisible);
                tickLine.setMinorTickVisible(ticksVisible);
                // add 1 to the slider minor tick count since the axis draws one
                // less minor ticks than the number given.
                tickLine.setMinorTickCount(Math.max(slider.getMinorTickCount(),0) + 1);
                if (slider.getLabelFormatter() != null) {
                    tickLine.setTickLabelFormatter(stringConverterWrapper);
                }
                getChildren().clear();
                getChildren().addAll(tickLine, track, thumb);
            } else {
                tickLine.setTickLabelsVisible(labelsVisible);
                tickLine.setTickMarkVisible(ticksVisible);
                tickLine.setMinorTickVisible(ticksVisible);
            }
        }
        else  {
            getChildren().clear();
            getChildren().addAll(track, thumb);
//            tickLine = null;
        }

        getSkinnable().requestLayout();
    }

    /**
     * Called when ever either min, max or value changes, so thumb's layoutX, Y is recomputed.
     */
    void positionThumb(final boolean animate) {
        Slider s = getSkinnable();
        if (s.getValue() > s.getMax()) return;// this can happen if we are bound to something
        boolean horizontal = s.getOrientation() == Orientation.HORIZONTAL;
        final double endX = (horizontal) ? trackStart + (((trackLength * ((s.getValue() - s.getMin()) /
                (s.getMax() - s.getMin()))) - thumbWidth/2)) : thumbLeft;
        final double endY = (horizontal) ? thumbTop :
            snappedTopInset() + trackLength - (trackLength * ((s.getValue() - s.getMin()) /
                (s.getMax() - s.getMin()))); //  - thumbHeight/2

        if (animate) {
            // lets animate the thumb transition
            final double startX = thumb.getLayoutX();
            final double startY = thumb.getLayoutY();
            Transition transition = new Transition() {
                {
                    setCycleDuration(Duration.millis(200));
                }

                @Override protected void interpolate(double frac) {
                    if (!Double.isNaN(startX)) {
                        thumb.setLayoutX(startX + frac * (endX - startX));
                    }
                    if (!Double.isNaN(startY)) {
                        thumb.setLayoutY(startY + frac * (endY - startY));
                    }
                }
            };
            transition.play();
        } else {
            thumb.setLayoutX(endX);
            thumb.setLayoutY(endY);
        }
    }

    double minTrackLength() {
        return 2*thumb.prefWidth(-1);
    }
}

