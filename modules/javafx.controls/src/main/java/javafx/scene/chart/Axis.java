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

package javafx.scene.chart;

import com.sun.javafx.scene.NodeHelper;
import javafx.css.Styleable;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;

import javafx.css.converter.BooleanConverter;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;

import java.util.*;

import javafx.animation.FadeTransition;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.*;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.FontCssMetaData;
import javafx.css.StyleableProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;


/**
 * Base class for all axes in JavaFX that represents an axis drawn on a chart area.
 * It holds properties for axis auto ranging, ticks and labels along the axis.
 * <p>
 * Some examples of concrete subclasses include {@link NumberAxis} whose axis plots data
 * in numbers and {@link CategoryAxis} whose values / ticks represent string
 * categories along its axis.
 * @since JavaFX 2.0
 */
public abstract class Axis<T> extends Region {

    // -------------- PRIVATE FIELDS -----------------------------------------------------------------------------------

    Text measure = new Text();
    private Orientation effectiveOrientation;
    private double effectiveTickLabelRotation = Double.NaN;
    private Label axisLabel = new Label();
    private final Path tickMarkPath = new Path();
    private double oldLength = 0;
    /** True when the current range invalid and all dependent calculations need to be updated */
    boolean rangeValid = false;
    boolean measureInvalid = false;
    boolean tickLabelsVisibleInvalid = false;

    private BitSet labelsToSkip = new BitSet();

    // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

    private final ObservableList<TickMark<T>> tickMarks = FXCollections.observableArrayList();
    private final ObservableList<TickMark<T>> unmodifiableTickMarks = FXCollections.unmodifiableObservableList(tickMarks);
    /**
     * Unmodifiable observable list of tickmarks, each TickMark directly representing a tickmark on this axis. This is updated
     * whenever the displayed tickmarks changes.
     *
     * @return Unmodifiable observable list of TickMarks on this axis
     */
    public ObservableList<TickMark<T>> getTickMarks() { return unmodifiableTickMarks; }

    /** The side of the plot which this axis is being drawn on */
    private ObjectProperty<Side> side = new StyleableObjectProperty<Side>(){
        @Override protected void invalidated() {
            // cause refreshTickMarks
            Side edge = get();
            pseudoClassStateChanged(TOP_PSEUDOCLASS_STATE, edge == Side.TOP);
            pseudoClassStateChanged(RIGHT_PSEUDOCLASS_STATE, edge == Side.RIGHT);
            pseudoClassStateChanged(BOTTOM_PSEUDOCLASS_STATE, edge == Side.BOTTOM);
            pseudoClassStateChanged(LEFT_PSEUDOCLASS_STATE, edge == Side.LEFT);
            requestAxisLayout();
        }

        @Override
        public CssMetaData<Axis<?>,Side> getCssMetaData() {
            return StyleableProperties.SIDE;
        }

        @Override
        public Object getBean() {
            return Axis.this;
        }

        @Override
        public String getName() {
            return "side";
        }
    };
    public final Side getSide() { return side.get(); }
    public final void setSide(Side value) { side.set(value); }
    public final ObjectProperty<Side> sideProperty() { return side; }

    final void setEffectiveOrientation(Orientation orientation) {
        effectiveOrientation = orientation;
    }

    final Side getEffectiveSide() {
        final Side side = getSide();
        if (side == null || (side.isVertical() && effectiveOrientation == Orientation.HORIZONTAL)
                || side.isHorizontal() && effectiveOrientation == Orientation.VERTICAL) {
            // Means side == null && effectiveOrientation == null produces Side.BOTTOM
            return effectiveOrientation == Orientation.VERTICAL ? Side.LEFT : Side.BOTTOM;
        }
        return side;
    }

    /** The axis label */
    private ObjectProperty<String> label = new ObjectPropertyBase<String>() {
        @Override protected void invalidated() {
            axisLabel.setText(get());
            requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return Axis.this;
        }

        @Override
        public String getName() {
            return "label";
        }
    };
    public final String getLabel() { return label.get(); }
    public final void setLabel(String value) { label.set(value); }
    public final ObjectProperty<String> labelProperty() { return label; }

    /** true if tick marks should be displayed */
    private BooleanProperty tickMarkVisible = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            tickMarkPath.setVisible(get());
            requestAxisLayout();
        }

        @Override
        public CssMetaData<Axis<?>,Boolean> getCssMetaData() {
            return StyleableProperties.TICK_MARK_VISIBLE;
        }
        @Override
        public Object getBean() {
            return Axis.this;
        }

        @Override
        public String getName() {
            return "tickMarkVisible";
        }
    };
    public final boolean isTickMarkVisible() { return tickMarkVisible.get(); }
    public final void setTickMarkVisible(boolean value) { tickMarkVisible.set(value); }
    public final BooleanProperty tickMarkVisibleProperty() { return tickMarkVisible; }

    /** true if tick mark labels should be displayed */
    private BooleanProperty tickLabelsVisible = new StyleableBooleanProperty(true) {
        @Override protected void invalidated() {
            // update textNode visibility for each tick
            for (TickMark<T> tick : tickMarks) {
                tick.setTextVisible(get());
            }
            tickLabelsVisibleInvalid = true;
            requestAxisLayout();
        }

        @Override
        public CssMetaData<Axis<?>,Boolean> getCssMetaData() {
            return StyleableProperties.TICK_LABELS_VISIBLE;
        }

        @Override
        public Object getBean() {
            return Axis.this;
        }

        @Override
        public String getName() {
            return "tickLabelsVisible";
        }
    };
    public final boolean isTickLabelsVisible() { return tickLabelsVisible.get(); }
    public final void setTickLabelsVisible(boolean value) {
        tickLabelsVisible.set(value); }
    public final BooleanProperty tickLabelsVisibleProperty() { return tickLabelsVisible; }

    /** The length of tick mark lines */
    private DoubleProperty tickLength = new StyleableDoubleProperty(8) {
        @Override protected void invalidated() {
            if (tickLength.get() < 0 && !tickLength.isBound()) {
                tickLength.set(0);
            }
            // this effects preferred size so request layout
            requestAxisLayout();
        }

        @Override
        public CssMetaData<Axis<?>,Number> getCssMetaData() {
            return StyleableProperties.TICK_LENGTH;
        }
        @Override
        public Object getBean() {
            return Axis.this;
        }

        @Override
        public String getName() {
            return "tickLength";
        }
    };
    public final double getTickLength() { return tickLength.get(); }
    public final void setTickLength(double value) { tickLength.set(value); }
    public final DoubleProperty tickLengthProperty() { return tickLength; }

    /** This is true when the axis determines its range from the data automatically */
    private BooleanProperty autoRanging = new BooleanPropertyBase(true) {
        @Override protected void invalidated() {
            if(get()) {
                // auto range turned on, so need to auto range now
//                autoRangeValid = false;
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return Axis.this;
        }

        @Override
        public String getName() {
            return "autoRanging";
        }
    };
    public final boolean isAutoRanging() { return autoRanging.get(); }
    public final void setAutoRanging(boolean value) { autoRanging.set(value); }
    public final BooleanProperty autoRangingProperty() { return autoRanging; }

    /** The font for all tick labels */
    private ObjectProperty<Font> tickLabelFont = new StyleableObjectProperty<Font>(Font.font("System",8)) {
        @Override protected void invalidated() {
            Font f = get();
            measure.setFont(f);
            for(TickMark<T> tm : getTickMarks()) {
                tm.textNode.setFont(f);
            }
            measureInvalid = true;
            requestAxisLayout();
        }

        @Override
        public CssMetaData<Axis<?>,Font> getCssMetaData() {
            return StyleableProperties.TICK_LABEL_FONT;
        }

        @Override
        public Object getBean() {
            return Axis.this;
        }

        @Override
        public String getName() {
            return "tickLabelFont";
        }
    };
    public final Font getTickLabelFont() { return tickLabelFont.get(); }
    public final void setTickLabelFont(Font value) { tickLabelFont.set(value); }
    public final ObjectProperty<Font> tickLabelFontProperty() { return tickLabelFont; }

    /** The fill for all tick labels */
    private ObjectProperty<Paint> tickLabelFill = new StyleableObjectProperty<Paint>(Color.BLACK) {
        @Override protected void invalidated() {
            for (TickMark<T> tick : tickMarks) {
                tick.textNode.setFill(getTickLabelFill());
            }
        }

        @Override
        public CssMetaData<Axis<?>,Paint> getCssMetaData() {
            return StyleableProperties.TICK_LABEL_FILL;
        }

        @Override
        public Object getBean() {
            return Axis.this;
        }

        @Override
        public String getName() {
            return "tickLabelFill";
        }
    };
    public final Paint getTickLabelFill() { return tickLabelFill.get(); }
    public final void setTickLabelFill(Paint value) { tickLabelFill.set(value); }
    public final ObjectProperty<Paint> tickLabelFillProperty() { return tickLabelFill; }

    /** The gap between tick labels and the tick mark lines */
    private DoubleProperty tickLabelGap = new StyleableDoubleProperty(3) {
        @Override protected void invalidated() {
           requestAxisLayout();
        }

        @Override
        public CssMetaData<Axis<?>,Number> getCssMetaData() {
            return StyleableProperties.TICK_LABEL_TICK_GAP;
        }

        @Override
        public Object getBean() {
            return Axis.this;
        }

        @Override
        public String getName() {
            return "tickLabelGap";
        }
    };
    public final double getTickLabelGap() { return tickLabelGap.get(); }
    public final void setTickLabelGap(double value) { tickLabelGap.set(value); }
    public final DoubleProperty tickLabelGapProperty() { return tickLabelGap; }

    /**
     * When true any changes to the axis and its range will be animated.
     */
    private BooleanProperty animated = new SimpleBooleanProperty(this, "animated", true);

    /**
     * Indicates whether the changes to axis range will be animated or not.
     *
     * @return true if axis range changes will be animated and false otherwise
     */
    public final boolean getAnimated() { return animated.get(); }
    public final void setAnimated(boolean value) { animated.set(value); }
    public final BooleanProperty animatedProperty() { return animated; }

    /**
     * Rotation in degrees of tick mark labels from their normal horizontal.
     */
    private DoubleProperty tickLabelRotation = new DoublePropertyBase(0) {
        @Override protected void invalidated() {
            if (isAutoRanging()) {
                invalidateRange(); // NumberAxis and CategoryAxis use this property in autorange
            }
            requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return Axis.this;
        }

        @Override
        public String getName() {
            return "tickLabelRotation";
        }
    };
    public final double getTickLabelRotation() { return tickLabelRotation.getValue(); }
    public final void setTickLabelRotation(double value) { tickLabelRotation.setValue(value); }
    public final DoubleProperty tickLabelRotationProperty() { return tickLabelRotation; }

    // -------------- CONSTRUCTOR --------------------------------------------------------------------------------------

    /**
     * Creates and initializes a new instance of the Axis class.
     */
    public Axis() {
        getStyleClass().setAll("axis");
        axisLabel.getStyleClass().add("axis-label");
        axisLabel.setAlignment(Pos.CENTER);
        tickMarkPath.getStyleClass().add("axis-tick-mark");
        getChildren().addAll(axisLabel, tickMarkPath);
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------

    /**
     * See if the current range is valid, if it is not then any range dependent calulcations need to redone on the next layout pass
     *
     * @return true if current range calculations are valid
     */
    protected final boolean isRangeValid() { return rangeValid; }

    /**
     * Mark the current range invalid, this will cause anything that depends on the range to be recalculated on the
     * next layout.
     */
    protected final void invalidateRange() { rangeValid = false; }

    /**
     * This is used to check if any given animation should run. It returns true if animation is enabled and the node
     * is visible and in a scene.
     *
     * @return true if animations should happen
     */
    protected final boolean shouldAnimate(){
        return getAnimated() && NodeHelper.isTreeShowing(this);
    }

    /**
     * We suppress requestLayout() calls here by doing nothing as we don't want changes to our children to cause
     * layout. If you really need to request layout then call requestAxisLayout().
     */
    @Override public void requestLayout() {}

    /**
     * Request that the axis is laid out in the next layout pass. This replaces requestLayout() as it has been
     * overridden to do nothing so that changes to children's bounds etc do not cause a layout. This was done as a
     * optimization as the Axis knows the exact minimal set of changes that really need layout to be updated. So we
     * only want to request layout then, not on any child change.
     */
    public void requestAxisLayout() {
        super.requestLayout();
    }

    /**
     * Called when data has changed and the range may not be valid any more. This is only called by the chart if
     * isAutoRanging() returns true. If we are auto ranging it will cause layout to be requested and auto ranging to
     * happen on next layout pass.
     *
     * @param data The current set of all data that needs to be plotted on this axis
     */
    public void invalidateRange(List<T> data) {
        invalidateRange();
        requestAxisLayout();
    }

    /**
     * This calculates the upper and lower bound based on the data provided to invalidateRange() method. This must not
     * effect the state of the axis, changing any properties of the axis. Any results of the auto-ranging should be
     * returned in the range object. This will we passed to setRange() if it has been decided to adopt this range for
     * this axis.
     *
     * @param length The length of the axis in screen coordinates
     * @return Range information, this is implementation dependent
     */
    protected abstract Object autoRange(double length);

    /**
     * Called to set the current axis range to the given range. If isAnimating() is true then this method should
     * animate the range to the new range.
     *
     * @param range A range object returned from autoRange()
     * @param animate If true animate the change in range
     */
    protected abstract void setRange(Object range, boolean animate);

    /**
     * Called to get the current axis range.
     *
     * @return A range object that can be passed to setRange() and calculateTickValues()
     */
    protected abstract Object getRange();

    /**
     * Get the display position of the zero line along this axis.
     *
     * @return display position or Double.NaN if zero is not in current range;
     */
    public abstract double getZeroPosition();

    /**
     * Get the display position along this axis for a given value.
     * If the value is not in the current range, the returned value will be an extrapolation of the display
     * position.
     *
     * If the value is not valid for this Axis and the axis cannot display such value in any range,
     * Double.NaN is returned
     *
     * @param value The data value to work out display position for
     * @return display position or Double.NaN if value not valid
     */
    public abstract double getDisplayPosition(T value);

    /**
     * Get the data value for the given display position on this axis. If the axis
     * is a CategoryAxis this will be the nearest value.
     *
     * @param  displayPosition A pixel position on this axis
     * @return the nearest data value to the given pixel position or
     *         null if not on axis;
     */
    public abstract T getValueForDisplay(double displayPosition);

    /**
     * Checks if the given value is plottable on this axis
     *
     * @param value The value to check if its on axis
     * @return true if the given value is plottable on this axis
     */
    public abstract boolean isValueOnAxis(T value);

    /**
     * All axis values must be representable by some numeric value. This gets the numeric value for a given data value.
     *
     * @param value The data value to convert
     * @return Numeric value for the given data value
     */
    public abstract double toNumericValue(T value);

    /**
     * All axis values must be representable by some numeric value. This gets the data value for a given numeric value.
     *
     * @param value The numeric value to convert
     * @return Data value for given numeric value
     */
    public abstract T toRealValue(double value);

    /**
     * Calculate a list of all the data values for each tick mark in range
     *
     * @param length The length of the axis in display units
     * @param range A range object returned from autoRange()
     * @return A list of tick marks that fit along the axis if it was the given length
     */
    protected abstract List<T> calculateTickValues(double length, Object range);

    /**
     * Computes the preferred height of this axis for the given width. If axis orientation
     * is horizontal, it takes into account the tick mark length, tick label gap and
     * label height.
     *
     * @return the computed preferred width for this axis
     */
    @Override protected double computePrefHeight(double width) {
        final Side side = getEffectiveSide();
        if (side.isVertical()) {
            // TODO for now we have no hard and fast answer here, I guess it should work
            // TODO out the minimum size needed to display min, max and zero tick mark labels.
            return 100;
        } else { // HORIZONTAL
            // we need to first auto range as this may/will effect tick marks
            Object range = autoRange(width);
            // calculate max tick label height
            double maxLabelHeight = 0;
            // calculate the new tick marks
            if (isTickLabelsVisible()) {
                final List<T> newTickValues = calculateTickValues(width, range);
                for (T value: newTickValues) {
                    maxLabelHeight = Math.max(maxLabelHeight,measureTickMarkSize(value, range).getHeight());
                }
            }
            // calculate tick mark length
            final double tickMarkLength = isTickMarkVisible() ? (getTickLength() > 0) ? getTickLength() : 0 : 0;
            // calculate label height
            final double labelHeight =
                    axisLabel.getText() == null || axisLabel.getText().length() == 0 ?
                    0 : axisLabel.prefHeight(-1);
            return maxLabelHeight + getTickLabelGap() + tickMarkLength + labelHeight;
        }
    }

    /**
     * Computes the preferred width of this axis for the given height. If axis orientation
     * is vertical, it takes into account the tick mark length, tick label gap and
     * label height.
     *
     * @return the computed preferred width for this axis
     */
    @Override protected double computePrefWidth(double height) {
        final Side side = getEffectiveSide();
        if (side.isVertical()) {
            // we need to first auto range as this may/will effect tick marks
            Object range = autoRange(height);
            // calculate max tick label width
            double maxLabelWidth = 0;
            // calculate the new tick marks
            if (isTickLabelsVisible()) {
                final List<T> newTickValues = calculateTickValues(height,range);
                for (T value: newTickValues) {
                    maxLabelWidth = Math.max(maxLabelWidth, measureTickMarkSize(value, range).getWidth());
                }
            }
            // calculate tick mark length
            final double tickMarkLength = isTickMarkVisible() ? (getTickLength() > 0) ? getTickLength() : 0 : 0;
            // calculate label height
            final double labelHeight =
                    axisLabel.getText() == null || axisLabel.getText().length() == 0 ?
                    0 : axisLabel.prefHeight(-1);
            return maxLabelWidth + getTickLabelGap() + tickMarkLength + labelHeight;
        } else  { // HORIZONTAL
            // TODO for now we have no hard and fast answer here, I guess it should work
            // TODO out the minimum size needed to display min, max and zero tick mark labels.
            return 100;
        }
    }

    /**
     * Called during layout if the tickmarks have been updated, allowing subclasses to do anything they need to
     * in reaction.
     */
    protected void tickMarksUpdated(){}

    /**
     * Invoked during the layout pass to layout this axis and all its content.
     */
    @Override protected void layoutChildren() {
        final boolean isFirstPass = oldLength == 0;
        // auto range if it is not valid
        final Side side = getEffectiveSide();
        final double length = side.isVertical() ? getHeight() : getWidth();
        boolean rangeInvalid = !isRangeValid();
        boolean lengthDiffers = oldLength != length;
        if (lengthDiffers || rangeInvalid) {
            // get range
            Object range;
            if(isAutoRanging()) {
                // auto range
                range = autoRange(length);
                // set current range to new range
                setRange(range, getAnimated() && !isFirstPass && NodeHelper.isTreeShowing(this) && rangeInvalid);
            } else {
                range = getRange();
            }
            // calculate new tick marks
            List<T> newTickValues = calculateTickValues(length, range);

            // remove everything
            Iterator<TickMark<T>> tickMarkIterator = tickMarks.iterator();
            while (tickMarkIterator.hasNext()) {
                TickMark<T> tick = tickMarkIterator.next();
                final TickMark<T> tm = tick;
                if (shouldAnimate()) {
                    FadeTransition ft = new FadeTransition(Duration.millis(250),tick.textNode);
                    ft.setToValue(0);
                    ft.setOnFinished(actionEvent -> {
                        getChildren().remove(tm.textNode);
                    });
                    ft.play();
                } else {
                    getChildren().remove(tm.textNode);
                }
                // we have to remove the tick mark immediately so we don't draw tick line for it or grid lines and fills
                tickMarkIterator.remove();
            }

            // add new tick marks for new values
            for(T newValue: newTickValues) {
                final TickMark<T> tick = new TickMark<T>();
                tick.setValue(newValue);
                tick.textNode.setText(getTickMarkLabel(newValue));
                tick.textNode.setFont(getTickLabelFont());
                tick.textNode.setFill(getTickLabelFill());
                tick.setTextVisible(isTickLabelsVisible());
                if (shouldAnimate()) tick.textNode.setOpacity(0);
                getChildren().add(tick.textNode);
                tickMarks.add(tick);
                if (shouldAnimate()) {
                    FadeTransition ft = new FadeTransition(Duration.millis(750),tick.textNode);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.play();
                }
            }

            // call tick marks updated to inform subclasses that we have updated tick marks
            tickMarksUpdated();
            // mark all done
            oldLength = length;
            rangeValid = true;
        }

        if (lengthDiffers || rangeInvalid || measureInvalid || tickLabelsVisibleInvalid) {
            measureInvalid = false;
            tickLabelsVisibleInvalid = false;
            // RT-12272 : tick labels overlapping
            // first check if all visible labels fit, if not, retain every nth label
            labelsToSkip.clear();
            int numLabelsToSkip = 0;
            double totalLabelsSize = 0;
            double maxLabelSize = 0;
            for (TickMark<T> m : tickMarks) {
                m.setPosition(getDisplayPosition(m.getValue()));
                if (m.isTextVisible()) {
                    double tickSize = measureTickMarkSize(m.getValue(), side);
                    totalLabelsSize += tickSize;
                    maxLabelSize = Math.round(Math.max(maxLabelSize, tickSize));
                }
            }
            if (maxLabelSize > 0 && length < totalLabelsSize) {
                numLabelsToSkip = ((int)(tickMarks.size() * maxLabelSize / length)) + 1;
            }

            if (numLabelsToSkip > 0) {
                int tickIndex = 0;
                for (TickMark<T> m : tickMarks) {
                    if (m.isTextVisible()) {
                        m.setTextVisible((tickIndex++ % numLabelsToSkip) == 0);
                    }
                }
            }

            // now check if labels for bounds overlap nearby labels, this can happen due to JDK-8097501
            // use tickLabelGap to prevent sticking
            if (tickMarks.size() > 2) {
                TickMark<T> m1 = tickMarks.get(0);
                TickMark<T> m2 = tickMarks.get(1);
                if (isTickLabelsOverlap(side, m1, m2, getTickLabelGap())) {
                    m2.setTextVisible(false);
                }
                m1 = tickMarks.get(tickMarks.size()-2);
                m2 = tickMarks.get(tickMarks.size()-1);
                if (isTickLabelsOverlap(side, m1, m2, getTickLabelGap())) {
                    m1.setTextVisible(false);
                }
            }
            updateTickMarks(side, length);
        }
    }

    private void updateTickMarks(Side side, double length) {
        // clear tick mark path elements as we will recreate
        tickMarkPath.getElements().clear();
        // do layout of axis label, tick mark lines and text
        final double width = getWidth();
        final double height = getHeight();
        final double tickMarkLength = (isTickMarkVisible() && getTickLength() > 0) ? getTickLength() : 0;
        final double effectiveLabelRotation = getEffectiveTickLabelRotation();
        if (Side.LEFT.equals(side)) {
            // offset path to make strokes snap to pixel
            tickMarkPath.setLayoutX(-0.5);
            tickMarkPath.setLayoutY(0.5);
            if (getLabel() != null) {
                axisLabel.getTransforms().setAll(new Translate(0, height), new Rotate(-90, 0, 0));
                axisLabel.setLayoutX(0);
                axisLabel.setLayoutY(0);
                //noinspection SuspiciousNameCombination
                axisLabel.resize(height, Math.ceil(axisLabel.prefHeight(width)));
            }
            for (TickMark<T> tick : tickMarks) {
                positionTextNode(tick.textNode, width - getTickLabelGap() - tickMarkLength,
                        tick.getPosition(), effectiveLabelRotation, side);
                updateTickMark(tick, length,
                        width - tickMarkLength, tick.getPosition(),
                        width, tick.getPosition());
            }
        } else if (Side.RIGHT.equals(side)) {
            // offset path to make strokes snap to pixel
            tickMarkPath.setLayoutX(0.5);
            tickMarkPath.setLayoutY(0.5);
            if (getLabel() != null) {
                final double axisLabelWidth = Math.ceil(axisLabel.prefHeight(width));
                axisLabel.getTransforms().setAll(new Translate(0, height), new Rotate(-90, 0, 0));
                axisLabel.setLayoutX(width-axisLabelWidth);
                axisLabel.setLayoutY(0);
                //noinspection SuspiciousNameCombination
                axisLabel.resize(height, axisLabelWidth);
            }
            for (TickMark<T> tick : tickMarks) {
                positionTextNode(tick.textNode, getTickLabelGap() + tickMarkLength,
                        tick.getPosition(), effectiveLabelRotation, side);
                updateTickMark(tick, length,
                        0, tick.getPosition(),
                        tickMarkLength, tick.getPosition());
            }
        } else if (Side.TOP.equals(side)) {
            // offset path to make strokes snap to pixel
            tickMarkPath.setLayoutX(0.5);
            tickMarkPath.setLayoutY(-0.5);
            if (getLabel() != null) {
                axisLabel.getTransforms().clear();
                axisLabel.setLayoutX(0);
                axisLabel.setLayoutY(0);
                axisLabel.resize(width, Math.ceil(axisLabel.prefHeight(width)));
            }
            for (TickMark<T> tick : tickMarks) {
                positionTextNode(tick.textNode, tick.getPosition(), height - tickMarkLength - getTickLabelGap(),
                        effectiveLabelRotation, side);
                updateTickMark(tick, length,
                        tick.getPosition(), height,
                        tick.getPosition(), height - tickMarkLength);
            }
        } else {
            // BOTTOM
            // offset path to make strokes snap to pixel
            tickMarkPath.setLayoutX(0.5);
            tickMarkPath.setLayoutY(0.5);
            if (getLabel() != null) {
                axisLabel.getTransforms().clear();
                final double labelHeight = Math.ceil(axisLabel.prefHeight(width));
                axisLabel.setLayoutX(0);
                axisLabel.setLayoutY(height - labelHeight);
                axisLabel.resize(width, labelHeight);
            }
            for (TickMark<T> tick : tickMarks) {
                positionTextNode(tick.textNode, tick.getPosition(), tickMarkLength + getTickLabelGap(),
                        effectiveLabelRotation, side);
                updateTickMark(tick, length,
                        tick.getPosition(), 0,
                        tick.getPosition(), tickMarkLength);
            }
        }
    }

    /**
     * Checks if two consecutive tick mark labels overlaps.
     * @param side side of the Axis
     * @param m1 first tick mark
     * @param m2 second tick mark
     * @param gap minimum space between labels
     * @return true if labels overlap
     */
    private boolean isTickLabelsOverlap(Side side, TickMark<T> m1, TickMark<T> m2, double gap) {
        if (!m1.isTextVisible() || !m2.isTextVisible()) return false;
        double m1Size = measureTickMarkSize(m1.getValue(), side);
        double m2Size = measureTickMarkSize(m2.getValue(), side);
        double m1Start = m1.getPosition() - m1Size / 2;
        double m1End = m1.getPosition() + m1Size / 2;
        double m2Start = m2.getPosition() - m2Size / 2;
        double m2End = m2.getPosition() + m2Size / 2;
        return side.isVertical() ? (m1Start - m2End) <= gap : (m2Start - m1End) <= gap;
    }

    /**
     * Positions a text node to one side of the given point, it X height is vertically centered on point if LEFT or
     * RIGHT and its centered horizontally if TOP ot BOTTOM.
     *
     * @param node The text node to position
     * @param posX The x position, to place text next to
     * @param posY The y position, to place text next to
     * @param angle The text rotation
     * @param side The side to place text next to position x,y at
     */
    private void positionTextNode(Text node, double posX, double posY, double angle, Side side) {
        node.setLayoutX(0);
        node.setLayoutY(0);
        node.setRotate(angle);
        final Bounds bounds = node.getBoundsInParent();
        if (Side.LEFT.equals(side)) {
            node.setLayoutX(posX-bounds.getWidth()-bounds.getMinX());
            node.setLayoutY(posY - (bounds.getHeight() / 2d) - bounds.getMinY());
        } else if (Side.RIGHT.equals(side)) {
            node.setLayoutX(posX-bounds.getMinX());
            node.setLayoutY(posY-(bounds.getHeight()/2d)-bounds.getMinY());
        } else if (Side.TOP.equals(side)) {
            node.setLayoutX(posX-(bounds.getWidth()/2d)-bounds.getMinX());
            node.setLayoutY(posY-bounds.getHeight()-bounds.getMinY());
        } else {
            node.setLayoutX(posX-(bounds.getWidth()/2d)-bounds.getMinX());
            node.setLayoutY(posY-bounds.getMinY());
        }
    }

    /**
     * Updates visibility of the text node and adds the tick mark to the path
     */
    private void updateTickMark(TickMark<T> tick, double length,
            double startX, double startY, double endX, double endY)
    {
        // check if position is inside bounds
        if (tick.getPosition() >= 0 && tick.getPosition() <= Math.ceil(length)) {
            tick.textNode.setVisible(tick.isTextVisible());
            // add tick mark line
            tickMarkPath.getElements().addAll(
                    new MoveTo(startX, startY),
                    new LineTo(endX, endY)
            );
        } else {
            tick.textNode.setVisible(false);
        }
    }
    /**
     * Get the string label name for a tick mark with the given value
     *
     * @param value The value to format into a tick label string
     * @return A formatted string for the given value
     */
    protected abstract String getTickMarkLabel(T value);

    /**
     * Measure the size of the label for given tick mark value. This uses the font that is set for the tick marks
     *
     *
     * @param labelText     tick mark label text
     * @param rotation  The text rotation
     * @return size of tick mark label for given value
     */
    protected final Dimension2D measureTickMarkLabelSize(String labelText, double rotation) {
        measure.setRotate(rotation);
        measure.setText(labelText);
        Bounds bounds = measure.getBoundsInParent();
        return new Dimension2D(bounds.getWidth(), bounds.getHeight());
    }

    /**
     * Measure the size of the label for given tick mark value. This uses the font that is set for the tick marks
     *
     * @param value     tick mark value
     * @param rotation  The text rotation
     * @return size of tick mark label for given value
     */
    protected final Dimension2D measureTickMarkSize(T value, double rotation) {
        return measureTickMarkLabelSize(getTickMarkLabel(value), rotation);
    }

    /**
     * Measure the size of the label for given tick mark value. This uses the font that is set for the tick marks
     *
     * @param value tick mark value
     * @param range range to use during calculations
     * @return size of tick mark label for given value
     */
    protected Dimension2D measureTickMarkSize(T value, Object range) {
        return measureTickMarkSize(value, getEffectiveTickLabelRotation());
    }

    /**
     * Measure the size of the label for given tick mark value. This uses the font that is set for the tick marks
     *
     * @param value tick mark value
     * @param side side of this Axis
     * @return size of tick mark label for given value
     * @see #measureTickMarkSize(Object, Object)
     */
    private double measureTickMarkSize(T value, Side side) {
        Dimension2D size = measureTickMarkSize(value, getEffectiveTickLabelRotation());
        return side.isVertical() ? size.getHeight() : size.getWidth();
    }

    final double getEffectiveTickLabelRotation() {
        return !isAutoRanging() || Double.isNaN(effectiveTickLabelRotation) ? getTickLabelRotation() : effectiveTickLabelRotation;
    }

    /**
     *
     * @param rotation NaN for using the tickLabelRotationProperty()
     */
    final void setEffectiveTickLabelRotation(double rotation) {
        effectiveTickLabelRotation = rotation;
    }

    // -------------- TICKMARK INNER CLASS -----------------------------------------------------------------------------

    /**
     * TickMark represents the label text, its associated properties for each tick
     * along the Axis.
     * @since JavaFX 2.0
     */
    public static final class TickMark<T> {
        /**
         * The display text for tick mark
         */
        private StringProperty label = new StringPropertyBase() {
            @Override protected void invalidated() {
                textNode.setText(getValue());
            }

            @Override
            public Object getBean() {
                return TickMark.this;
            }

            @Override
            public String getName() {
                return "label";
            }
        };
        public final String getLabel() { return label.get(); }
        public final void setLabel(String value) { label.set(value); }
        public final StringExpression labelProperty() { return label; }

        /**
         * The value for this tick mark in data units
         */
        private ObjectProperty<T> value = new SimpleObjectProperty<T>(this, "value");
        public final T getValue() { return value.get(); }
        public final void setValue(T v) { value.set(v); }
        public final ObjectExpression<T> valueProperty() { return value; }

        /**
         * The display position along the axis from axis origin in display units
         */
        private DoubleProperty position = new SimpleDoubleProperty(this, "position");
        public final double getPosition() { return position.get(); }
        public final void setPosition(double value) { position.set(value); }
        public final DoubleExpression positionProperty() { return position; }

        Text textNode = new Text();

        /** true if tick mark labels should be displayed */
        private BooleanProperty textVisible = new BooleanPropertyBase(true) {
            @Override protected void invalidated() {
                if(!get()) {
                    textNode.setVisible(false);
                }
            }

            @Override
            public Object getBean() {
                return TickMark.this;
            }

            @Override
            public String getName() {
                return "textVisible";
            }
        };

        /**
         * Indicates whether this tick mark label text is displayed or not.
         * @return true if tick mark label text is visible and false otherwise
         */
        public final boolean isTextVisible() { return textVisible.get(); }

        /**
         * Specifies whether this tick mark label text is displayed or not.
         * @param value true if tick mark label text is visible and false otherwise
         */
        public final void setTextVisible(boolean value) { textVisible.set(value); }

        /**
         * Creates and initializes an instance of TickMark.
         */
        public TickMark() {
        }

        /**
         * Returns a string representation of this {@code TickMark} object.
         * @return a string representation of this {@code TickMark} object.
         */
        @Override public String toString() {
            return value.get().toString();
        }
    }

    // -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------

    private static class StyleableProperties {
        private static final CssMetaData<Axis<?>,Side> SIDE =
            new CssMetaData<Axis<?>,Side>("-fx-side",
                new EnumConverter<Side>(Side.class)) {

            @Override
            public boolean isSettable(Axis<?> n) {
                return n.side == null || !n.side.isBound();
            }

            @SuppressWarnings("unchecked") // sideProperty() is StyleableProperty<Side>
            @Override
            public StyleableProperty<Side> getStyleableProperty(Axis<?> n) {
                return (StyleableProperty<Side>)n.sideProperty();
            }
        };

        private static final CssMetaData<Axis<?>,Number> TICK_LENGTH =
            new CssMetaData<Axis<?>,Number>("-fx-tick-length",
                SizeConverter.getInstance(), 8.0) {

            @Override
            public boolean isSettable(Axis<?> n) {
                return n.tickLength == null || !n.tickLength.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(Axis<?> n) {
                return (StyleableProperty<Number>)(WritableValue<Number>)n.tickLengthProperty();
            }
        };

        private static final CssMetaData<Axis<?>,Font> TICK_LABEL_FONT =
            new FontCssMetaData<Axis<?>>("-fx-tick-label-font",
                Font.font("system", 8.0)) {

            @Override
            public boolean isSettable(Axis<?> n) {
                return n.tickLabelFont == null || !n.tickLabelFont.isBound();
            }

            @SuppressWarnings("unchecked") // tickLabelFontProperty() is StyleableProperty<Font>
            @Override
            public StyleableProperty<Font> getStyleableProperty(Axis<?> n) {
                return (StyleableProperty<Font>)n.tickLabelFontProperty();
            }
        };

        private static final CssMetaData<Axis<?>,Paint> TICK_LABEL_FILL =
            new CssMetaData<Axis<?>,Paint>("-fx-tick-label-fill",
                PaintConverter.getInstance(), Color.BLACK) {

            @Override
            public boolean isSettable(Axis<?> n) {
                return n.tickLabelFill == null | !n.tickLabelFill.isBound();
            }

            @SuppressWarnings("unchecked") // tickLabelFillProperty() is StyleableProperty<Paint>
            @Override
            public StyleableProperty<Paint> getStyleableProperty(Axis<?> n) {
                return (StyleableProperty<Paint>)n.tickLabelFillProperty();
            }
        };

        private static final CssMetaData<Axis<?>,Number> TICK_LABEL_TICK_GAP =
            new CssMetaData<Axis<?>,Number>("-fx-tick-label-gap",
                SizeConverter.getInstance(), 3.0) {

            @Override
            public boolean isSettable(Axis<?> n) {
                return n.tickLabelGap == null || !n.tickLabelGap.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(Axis<?> n) {
                return (StyleableProperty<Number>)(WritableValue<Number>)n.tickLabelGapProperty();
            }
        };

        private static final CssMetaData<Axis<?>,Boolean> TICK_MARK_VISIBLE =
            new CssMetaData<Axis<?>,Boolean>("-fx-tick-mark-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(Axis<?> n) {
                return n.tickMarkVisible == null || !n.tickMarkVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(Axis<?> n) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)n.tickMarkVisibleProperty();
            }
        };

        private static final CssMetaData<Axis<?>,Boolean> TICK_LABELS_VISIBLE =
            new CssMetaData<Axis<?>,Boolean>("-fx-tick-labels-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(Axis<?> n) {
                return n.tickLabelsVisible == null || !n.tickLabelsVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(Axis<?> n) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)n.tickLabelsVisibleProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
        final List<CssMetaData<? extends Styleable, ?>> styleables =
            new ArrayList<CssMetaData<? extends Styleable, ?>>(Region.getClassCssMetaData());
            styleables.add(SIDE);
            styleables.add(TICK_LENGTH);
            styleables.add(TICK_LABEL_FONT);
            styleables.add(TICK_LABEL_FILL);
            styleables.add(TICK_LABEL_TICK_GAP);
            styleables.add(TICK_MARK_VISIBLE);
            styleables.add(TICK_LABELS_VISIBLE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    /** pseudo-class indicating this is a vertical Top side Axis. */
    private static final PseudoClass TOP_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("top");
    /** pseudo-class indicating this is a vertical Bottom side Axis. */
    private static final PseudoClass BOTTOM_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("bottom");
    /** pseudo-class indicating this is a vertical Left side Axis. */
    private static final PseudoClass LEFT_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("left");
    /** pseudo-class indicating this is a vertical Right side Axis. */
    private static final PseudoClass RIGHT_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("right");

}
