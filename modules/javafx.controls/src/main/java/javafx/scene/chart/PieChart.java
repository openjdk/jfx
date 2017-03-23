/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Side;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;
import com.sun.javafx.collections.NonIterableChange;

import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.CssMetaData;

import javafx.css.converter.BooleanConverter;
import javafx.css.converter.SizeConverter;

import javafx.css.Styleable;
import javafx.css.StyleableProperty;

/**
 * Displays a PieChart. The chart content is populated by pie slices based on
 * data set on the PieChart.
 * <p> The clockwise property is set to true by default, which means slices are
 * placed in the clockwise order. The labelsVisible property is used to either display
 * pie slice labels or not.
 *
 * @since JavaFX 2.0
 */
public class PieChart extends Chart {

    // -------------- PRIVATE FIELDS -----------------------------------------------------------------------------------
    private static final int MIN_PIE_RADIUS = 25;
    private static final double LABEL_TICK_GAP = 6;
    private static final double LABEL_BALL_RADIUS = 2;
    private BitSet colorBits = new BitSet(8);
    private double pieRadius;
    private Data begin = null;
    private final Path labelLinePath = new Path() {
        @Override public boolean usesMirroring() {
            return false;
        }
    };
    private List<LabelLayoutInfo> labelLayoutInfos = null;
    private Legend legend = new Legend();
    private Data dataItemBeingRemoved = null;
    private Timeline dataRemoveTimeline = null;
    private final ListChangeListener<Data> dataChangeListener = c -> {
        while (c.next()) {
            // RT-28090 Probably a sort happened, just reorder the pointers.
            if (c.wasPermutated()) {
                Data ptr = begin;
                for (int i = 0; i < getData().size(); i++) {
                    Data item = getData().get(i);
                    updateDataItemStyleClass(item, i);
                    if (i == 0) {
                        begin = item;
                        ptr = begin;
                        begin.next = null;
                    } else {
                        ptr.next = item;
                        item.next = null;
                        ptr = item;
                    }
                }
                updateLegend();
                requestChartLayout();
                return;
            }
            // recreate linked list & set chart on new data
            for (int i = c.getFrom(); i < c.getTo(); i++) {
                Data item = getData().get(i);
                item.setChart(PieChart.this);
                if (begin == null) {
                    begin = item;
                    begin.next = null;
                } else {
                    if (i == 0) {
                        item.next = begin;
                        begin = item;
                    } else {
                        Data ptr = begin;
                        for (int j = 0; j < i -1 ; j++) {
                            ptr = ptr.next;
                        }
                        item.next = ptr.next;
                        ptr.next = item;
                    }
                }
            }
            // call data added/removed methods
            for (Data item : c.getRemoved()) {
                dataItemRemoved(item);
            }
            for (int i = c.getFrom(); i < c.getTo(); i++) {
                Data item = getData().get(i);
                // assign default color to the added slice
                // TODO: check nearby colors
                item.defaultColorIndex = colorBits.nextClearBit(0);
                colorBits.set(item.defaultColorIndex);
                dataItemAdded(item, i);
            }
            if (c.wasRemoved() || c.wasAdded()) {
                for (int i = 0; i < getData().size(); i++) {
                    Data item = getData().get(i);
                    updateDataItemStyleClass(item, i);
                }
                updateLegend();
            }
        }
        // re-layout everything
        requestChartLayout();
    };

    // -------------- PUBLIC PROPERTIES ----------------------------------------

    /** PieCharts data */
    private ObjectProperty<ObservableList<Data>> data = new ObjectPropertyBase<ObservableList<Data>>() {
        private ObservableList<Data> old;
        @Override protected void invalidated() {
            final ObservableList<Data> current = getValue();
            // add remove listeners
            if(old != null) old.removeListener(dataChangeListener);
            if(current != null) current.addListener(dataChangeListener);
            // fire data change event if series are added or removed
            if(old != null || current != null) {
                final List<Data> removed = (old != null) ? old : Collections.<Data>emptyList();
                final int toIndex = (current != null) ? current.size() : 0;
                // let data listener know all old data have been removed and new data that has been added
                if (toIndex > 0 || !removed.isEmpty()) {
                    dataChangeListener.onChanged(new NonIterableChange<Data>(0, toIndex, current){
                        @Override public List<Data> getRemoved() { return removed; }
                        @Override public boolean wasPermutated() { return false; }
                        @Override protected int[] getPermutation() {
                            return new int[0];
                        }
                    });
                }
            } else if (old != null && old.size() > 0) {
                // let series listener know all old series have been removed
                dataChangeListener.onChanged(new NonIterableChange<Data>(0, 0, current){
                    @Override public List<Data> getRemoved() { return old; }
                    @Override public boolean wasPermutated() { return false; }
                    @Override protected int[] getPermutation() {
                        return new int[0];
                    }
                });
            }
            old = current;
        }

        public Object getBean() {
            return PieChart.this;
        }

        public String getName() {
            return "data";
        }
    };
    public final ObservableList<Data> getData() { return data.getValue(); }
    public final void setData(ObservableList<Data> value) { data.setValue(value); }
    public final ObjectProperty<ObservableList<Data>> dataProperty() { return data; }

    /** The angle to start the first pie slice at */
    private DoubleProperty startAngle = new StyleableDoubleProperty(0) {
        @Override public void invalidated() {
            get();
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return PieChart.this;
        }

        @Override
        public String getName() {
            return "startAngle";
        }

        public CssMetaData<PieChart,Number> getCssMetaData() {
            return StyleableProperties.START_ANGLE;
        }
    };
    public final double getStartAngle() { return startAngle.getValue(); }
    public final void setStartAngle(double value) { startAngle.setValue(value); }
    public final DoubleProperty startAngleProperty() { return startAngle; }

    /** When true we start placing slices clockwise from the startAngle */
    private BooleanProperty clockwise = new StyleableBooleanProperty(true) {
        @Override public void invalidated() {
            get();
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return PieChart.this;
        }

        @Override
        public String getName() {
            return "clockwise";
        }

        public CssMetaData<PieChart,Boolean> getCssMetaData() {
            return StyleableProperties.CLOCKWISE;
        }
    };
    public final void setClockwise(boolean value) { clockwise.setValue(value);}
    public final boolean isClockwise() { return clockwise.getValue(); }
    public final BooleanProperty clockwiseProperty() { return clockwise; }


    /** The length of the line from the outside of the pie to the slice labels. */
    private DoubleProperty labelLineLength = new StyleableDoubleProperty(20d) {
        @Override public void invalidated() {
            get();
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return PieChart.this;
        }

        @Override
        public String getName() {
            return "labelLineLength";
        }

        public CssMetaData<PieChart,Number> getCssMetaData() {
            return StyleableProperties.LABEL_LINE_LENGTH;
        }
    };
    public final double getLabelLineLength() { return labelLineLength.getValue(); }
    public final void setLabelLineLength(double value) { labelLineLength.setValue(value); }
    public final DoubleProperty labelLineLengthProperty() { return labelLineLength; }

    /** When true pie slice labels are drawn */
    private BooleanProperty labelsVisible = new StyleableBooleanProperty(true) {
        @Override public void invalidated() {
            get();
            requestChartLayout();
        }

        @Override
        public Object getBean() {
            return PieChart.this;
        }

        @Override
        public String getName() {
            return "labelsVisible";
        }

        public CssMetaData<PieChart,Boolean> getCssMetaData() {
            return StyleableProperties.LABELS_VISIBLE;
        }
    };
    public final void setLabelsVisible(boolean value) { labelsVisible.setValue(value);}

    /**
     * Indicates whether pie slice labels are drawn or not
     * @return true if pie slice labels are visible and false otherwise.
     */
    public final boolean getLabelsVisible() { return labelsVisible.getValue(); }
    public final BooleanProperty labelsVisibleProperty() { return labelsVisible; }

    // -------------- CONSTRUCTOR ----------------------------------------------

    /**
     * Construct a new empty PieChart.
     */
    public PieChart() {
        this(FXCollections.<Data>observableArrayList());
    }

    /**
     * Construct a new PieChart with the given data
     *
     * @param data The data to use, this is the actual list used so any changes to it will be reflected in the chart
     */
    public PieChart(ObservableList<PieChart.Data> data) {
        getChartChildren().add(labelLinePath);
        labelLinePath.getStyleClass().add("chart-pie-label-line");
        setLegend(legend);
        setData(data);
        // set chart content mirroring to be always false i.e. chartContent mirrorring is not done
        // when  node orientation is right-to-left for PieChart.
        useChartContentMirroring = false;
    }

    // -------------- METHODS --------------------------------------------------

    private void dataNameChanged(Data item) {
        item.textNode.setText(item.getName());
        requestChartLayout();
        updateLegend();
    }

    private void dataPieValueChanged(Data item) {
        if (shouldAnimate()) {
            animate(
                new KeyFrame(Duration.ZERO, new KeyValue(item.currentPieValueProperty(),
                        item.getCurrentPieValue())),
                new KeyFrame(Duration.millis(500),new KeyValue(item.currentPieValueProperty(),
                        item.getPieValue(), Interpolator.EASE_BOTH))
            );
        } else {
            item.setCurrentPieValue(item.getPieValue());
            requestChartLayout(); // RT-23091
        }
    }

    private Node createArcRegion(Data item) {
        Node arcRegion = item.getNode();
        // check if symbol has already been created
        if (arcRegion == null) {
            arcRegion = new Region();
            arcRegion.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            arcRegion.setPickOnBounds(false);
            item.setNode(arcRegion);
        }
        return arcRegion;
    }

    private Text createPieLabel(Data item) {
        Text text = item.textNode;
        text.setText(item.getName());
        return text;
    }

    private void updateDataItemStyleClass(final Data item, int index) {
        Node node = item.getNode();
        if (node != null) {
            // Note: not sure if we want to add or check, ie be more careful and efficient here
            node.getStyleClass().setAll("chart-pie", "data" + index,
                    "default-color" + item.defaultColorIndex % 8);
            if (item.getPieValue() < 0) {
                node.getStyleClass().add("negative");
            }
        }
    }

    private void dataItemAdded(final Data item, int index) {
        // create shape
        Node shape = createArcRegion(item);
        final Text text = createPieLabel(item);
        item.getChart().getChartChildren().add(shape);
        if (shouldAnimate()) {
            // if the same data item is being removed, first stop the remove animation,
            // remove the item and then start the add animation.
            if (dataRemoveTimeline != null && dataRemoveTimeline.getStatus().equals(Animation.Status.RUNNING)) {
                if (dataItemBeingRemoved == item) {
                    dataRemoveTimeline.stop();
                    dataRemoveTimeline = null;
                    getChartChildren().remove(item.textNode);
                    getChartChildren().remove(shape);
                    removeDataItemRef(item);
                }
            }
            animate(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(item.currentPieValueProperty(), item.getCurrentPieValue()),
                    new KeyValue(item.radiusMultiplierProperty(), item.getRadiusMultiplier())),
                new KeyFrame(Duration.millis(500),
                        actionEvent -> {
                            text.setOpacity(0);
                            // RT-23597 : item's chart might have been set to null if
                            // this item is added and removed before its add animation finishes.
                            if (item.getChart() == null) item.setChart(PieChart.this);
                            item.getChart().getChartChildren().add(text);
                            FadeTransition ft = new FadeTransition(Duration.millis(150),text);
                            ft.setToValue(1);
                            ft.play();
                        },
                    new KeyValue(item.currentPieValueProperty(), item.getPieValue(), Interpolator.EASE_BOTH),
                    new KeyValue(item.radiusMultiplierProperty(), 1, Interpolator.EASE_BOTH))
            );
        } else {
            getChartChildren().add(text);
            item.setRadiusMultiplier(1);
            item.setCurrentPieValue(item.getPieValue());
        }

        // we sort the text nodes to always be at the end of the children list, so they have a higher z-order
        // (Fix for RT-34564)
        for (int i = 0; i < getChartChildren().size(); i++) {
            Node n = getChartChildren().get(i);
            if (n instanceof Text) {
                n.toFront();
            }
        }
    }

    private void removeDataItemRef(Data item) {
        if (begin == item) {
            begin = item.next;
        } else {
            Data ptr = begin;
            while(ptr != null && ptr.next != item) {
                ptr = ptr.next;
            }
            if(ptr != null) ptr.next = item.next;
        }
    }

    private Timeline createDataRemoveTimeline(final Data item) {
        final Node shape = item.getNode();
        Timeline t = new Timeline();
        t.getKeyFrames().addAll(new KeyFrame(Duration.ZERO,
                    new KeyValue(item.currentPieValueProperty(), item.getCurrentPieValue()),
                    new KeyValue(item.radiusMultiplierProperty(), item.getRadiusMultiplier())),
                new KeyFrame(Duration.millis(500),
                        actionEvent -> {
                            // removing item
                            colorBits.clear(item.defaultColorIndex);
                            getChartChildren().remove(shape);
                            // fade out label
                            FadeTransition ft = new FadeTransition(Duration.millis(150),item.textNode);
                            ft.setFromValue(1);
                            ft.setToValue(0);
                            ft.setOnFinished(new EventHandler<ActionEvent>() {
                                 @Override public void handle(ActionEvent actionEvent) {
                                     getChartChildren().remove(item.textNode);
                                     // remove chart references from old data - RT-22553
                                     item.setChart(null);
                                     removeDataItemRef(item);
                                     item.textNode.setOpacity(1.0);
                                 }
                            });
                            ft.play();
                        },
                    new KeyValue(item.currentPieValueProperty(), 0, Interpolator.EASE_BOTH),
                    new KeyValue(item.radiusMultiplierProperty(), 0))
                );
        return t;
    }

    private void dataItemRemoved(final Data item) {
        final Node shape = item.getNode();
        if (shouldAnimate()) {
            dataRemoveTimeline = createDataRemoveTimeline(item);
            dataItemBeingRemoved = item;
            animate(dataRemoveTimeline);
        } else {
            colorBits.clear(item.defaultColorIndex);
            getChartChildren().remove(item.textNode);
            getChartChildren().remove(shape);
            // remove chart references from old data
            item.setChart(null);
            removeDataItemRef(item);
        }
    }

    /** {@inheritDoc} */
    @Override protected void layoutChartChildren(double top, double left, double contentWidth, double contentHeight) {
        double total = 0.0;
        for (Data item = begin; item != null; item = item.next) {
            total+= Math.abs(item.getCurrentPieValue());
        }
        double scale = (total != 0) ? 360 / total : 0;

         // calculate combined bounds of all labels & pie radius
        double[] labelsX = null;
        double[] labelsY = null;
        double[] labelAngles = null;
        double labelScale = 1;
        List<LabelLayoutInfo> fullPie = null;
        boolean shouldShowLabels = getLabelsVisible();
        if (shouldShowLabels) {
            double xPad = 0d;
            double yPad = 0d;

            labelsX = new double[getDataSize()];
            labelsY = new double[getDataSize()];
            labelAngles = new double[getDataSize()];
            fullPie = new ArrayList<>();
            int index = 0;
            double start = getStartAngle();
            for (Data item = begin; item != null; item = item.next) {
                // remove any scale on the text node
                item.textNode.getTransforms().clear();

                double size = (isClockwise()) ? (-scale * Math.abs(item.getCurrentPieValue())) : (scale * Math.abs(item.getCurrentPieValue()));
                labelAngles[index] = normalizeAngle(start + (size / 2));
                final double sproutX = calcX(labelAngles[index], getLabelLineLength(), 0);
                final double sproutY = calcY(labelAngles[index], getLabelLineLength(), 0);
                labelsX[index] = sproutX;
                labelsY[index] = sproutY;
                xPad = Math.max(xPad, 2 * (item.textNode.getLayoutBounds().getWidth() + LABEL_TICK_GAP + Math.abs(sproutX)));
                if (sproutY > 0) { // on bottom
                    yPad = Math.max(yPad, 2 * Math.abs(sproutY+item.textNode.getLayoutBounds().getMaxY()));
                } else { // on top
                    yPad = Math.max(yPad, 2 * Math.abs(sproutY + item.textNode.getLayoutBounds().getMinY()));
                }
                start+= size;
                index++;
            }
            pieRadius = Math.min(contentWidth - xPad, contentHeight - yPad) / 2;
            // check if this makes the pie too small
            if (pieRadius < MIN_PIE_RADIUS ) {
                // calculate scale for text to fit labels in
                final double roomX = contentWidth-MIN_PIE_RADIUS-MIN_PIE_RADIUS;
                final double roomY = contentHeight-MIN_PIE_RADIUS-MIN_PIE_RADIUS;
                labelScale = Math.min(
                        roomX/xPad,
                        roomY/yPad
                );
                // hide labels if pie radius is less than minimum
                if ((begin == null && labelScale < 0.7) || ((begin.textNode.getFont().getSize()*labelScale) < 9)) {
                    shouldShowLabels = false;
                    labelScale = 1;
                } else {
                    // set pieRadius to minimum
                    pieRadius = MIN_PIE_RADIUS;
                    // apply scale to all label positions
                    for(int i=0; i< labelsX.length; i++) {
                        labelsX[i] =  labelsX[i] * labelScale;
                        labelsY[i] =  labelsY[i] * labelScale;
                    }
                }
            }
        }

        if (!shouldShowLabels) {
            pieRadius = Math.min(contentWidth,contentHeight) / 2;
            labelLinePath.getElements().clear();
        }

        if (getChartChildren().size() > 0) {
            double centerX = contentWidth / 2 + left;
            double centerY = contentHeight / 2 + top;
            int index = 0;
            for (Data item = begin; item != null; item = item.next) {
                // layout labels for pie slice
                item.textNode.setVisible(shouldShowLabels);
                if (shouldShowLabels) {
                    double size = (isClockwise()) ? (-scale * Math.abs(item.getCurrentPieValue())) : (scale * Math.abs(item.getCurrentPieValue()));
                    final boolean isLeftSide = !(labelAngles[index] > -90 && labelAngles[index] < 90);

                    double sliceCenterEdgeX = calcX(labelAngles[index], pieRadius, centerX);
                    double sliceCenterEdgeY = calcY(labelAngles[index], pieRadius, centerY);
                    double xval = isLeftSide ?
                        (labelsX[index] + sliceCenterEdgeX - item.textNode.getLayoutBounds().getMaxX() - LABEL_TICK_GAP) :
                        (labelsX[index] + sliceCenterEdgeX - item.textNode.getLayoutBounds().getMinX() + LABEL_TICK_GAP);
                    double yval = labelsY[index] + sliceCenterEdgeY - (item.textNode.getLayoutBounds().getMinY()/2) -2;

                    // do the line (Path)for labels
                    double lineEndX = sliceCenterEdgeX +labelsX[index];
                    double lineEndY = sliceCenterEdgeY +labelsY[index];
                    LabelLayoutInfo info = new LabelLayoutInfo(sliceCenterEdgeX,
                            sliceCenterEdgeY,lineEndX, lineEndY, xval, yval, item.textNode, Math.abs(size));
                    fullPie.add(info);

                    // set label scales
                    if (labelScale < 1) {
                        item.textNode.getTransforms().add(
                            new Scale(
                                    labelScale, labelScale,
                                    isLeftSide ? item.textNode.getLayoutBounds().getWidth() : 0, 0
                            )
                        );
                    }
                }
                index++;
            }

            // update/draw pie slices
            double sAngle = getStartAngle();
            for (Data item = begin; item != null; item = item.next) {
                Node node = item.getNode();
                Arc arc = null;
                if (node != null) {
                    if (node instanceof Region) {
                        Region arcRegion = (Region)node;
                        if (arcRegion.getShape() == null) {
                            arc = new Arc();
                            arcRegion.setShape(arc);
                        } else {
                            arc = (Arc)arcRegion.getShape();
                        }
                        arcRegion.setScaleShape(false);
                        arcRegion.setCenterShape(false);
                        arcRegion.setCacheShape(false);
                    }
                }
                double size = (isClockwise()) ? (-scale * Math.abs(item.getCurrentPieValue())) : (scale * Math.abs(item.getCurrentPieValue()));
                // update slice arc size
                arc.setStartAngle(sAngle);
                arc.setLength(size);
                arc.setType(ArcType.ROUND);
                arc.setRadiusX(pieRadius * item.getRadiusMultiplier());
                arc.setRadiusY(pieRadius * item.getRadiusMultiplier());
                node.setLayoutX(centerX);
                node.setLayoutY(centerY);
                sAngle += size;
            }
            // finally draw the text and line
            if (fullPie != null) {
                // Check for collision and resolve by hiding the label of the smaller pie slice
                resolveCollision(fullPie);

                if (!fullPie.equals(labelLayoutInfos)) {
                    labelLinePath.getElements().clear();
                    for (LabelLayoutInfo info : fullPie) {
                        if (info.text.isVisible()) drawLabelLinePath(info);
                    }
                    labelLayoutInfos = fullPie;
                }
            }
        }
    }

    // We check for pie slice label collision and if collision is detected, we then
    // compare the size of the slices, and hide the label of the smaller slice.
    private void resolveCollision(List<LabelLayoutInfo> list) {
        int boxH = (begin != null) ? (int)begin.textNode.getLayoutBounds().getHeight() : 0;
        for (int i = 0; i < list.size(); i++ ) {
            for (int j = i+1; j < list.size(); j++ ) {
                LabelLayoutInfo box1 = list.get(i);
                LabelLayoutInfo box2 = list.get(j);
                if ((box1.text.isVisible() && box2.text.isVisible()) &&
                        (fuzzyGT(box2.textY, box1.textY) ? fuzzyLT((box2.textY - boxH - box1.textY), 2) :
                                fuzzyLT((box1.textY - boxH - box2.textY), 2)) &&
                        (fuzzyGT(box1.textX, box2.textX) ? fuzzyLT((box1.textX - box2.textX), box2.text.prefWidth(-1)) :
                                fuzzyLT((box2.textX - box1.textX), box1.text.prefWidth(-1)))) {
                    if (fuzzyLT(box1.size, box2.size)) {
                        box1.text.setVisible(false);
                    } else {
                        box2.text.setVisible(false);
                    }
                }
            }
        }
    }

    private int fuzzyCompare(double o1, double o2) {
       double fuzz = 0.00001;
       return (((Math.abs(o1 - o2)) < fuzz) ? 0 : ((o1 < o2) ? -1 : 1));
    }

    private boolean fuzzyGT(double o1, double o2) {
        return fuzzyCompare(o1, o2) == 1;
    }

    private boolean fuzzyLT(double o1, double o2) {
        return fuzzyCompare(o1, o2) == -1;
    }

    private void drawLabelLinePath(LabelLayoutInfo info) {
        info.text.setLayoutX(info.textX);
        info.text.setLayoutY(info.textY);
        labelLinePath.getElements().add(new MoveTo(info.startX, info.startY));
        labelLinePath.getElements().add(new LineTo(info.endX, info.endY));

        labelLinePath.getElements().add(new MoveTo(info.endX-LABEL_BALL_RADIUS,info.endY));
        labelLinePath.getElements().add(new ArcTo(LABEL_BALL_RADIUS, LABEL_BALL_RADIUS,
                    90, info.endX,info.endY-LABEL_BALL_RADIUS, false, true));
        labelLinePath.getElements().add(new ArcTo(LABEL_BALL_RADIUS, LABEL_BALL_RADIUS,
                    90, info.endX+LABEL_BALL_RADIUS,info.endY, false, true));
        labelLinePath.getElements().add(new ArcTo(LABEL_BALL_RADIUS, LABEL_BALL_RADIUS,
                    90, info.endX,info.endY+LABEL_BALL_RADIUS, false, true));
        labelLinePath.getElements().add(new ArcTo(LABEL_BALL_RADIUS, LABEL_BALL_RADIUS,
                    90, info.endX-LABEL_BALL_RADIUS,info.endY, false, true));
        labelLinePath.getElements().add(new ClosePath());
    }
    /**
     * This is called whenever a series is added or removed and the legend needs to be updated
     */
    private void updateLegend() {
        Node legendNode = getLegend();
        if (legendNode != null && legendNode != legend) return; // RT-23596 dont update when user has set legend.
        legend.setVertical(getLegendSide().equals(Side.LEFT) || getLegendSide().equals(Side.RIGHT));
        List<Legend.LegendItem> legendList = new ArrayList<>();
        if (getData() != null) {
            for (Data item : getData()) {
                LegendItem legenditem = new LegendItem(item.getName());
                legenditem.getSymbol().getStyleClass().addAll(item.getNode().getStyleClass());
                legenditem.getSymbol().getStyleClass().add("pie-legend-symbol");
                legendList.add(legenditem);
            }
        }
        legend.getItems().setAll(legendList);
        if (legendList.size() > 0) {
            if (legendNode == null) {
                setLegend(legend);
            }
        } else {
            setLegend(null);
        }
    }

    private int getDataSize() {
        int count = 0;
        for (Data d = begin; d != null; d = d.next) {
            count++;
        }
        return count;
    }

    private static double calcX(double angle, double radius, double centerX) {
        return (double)(centerX + radius * Math.cos(Math.toRadians(-angle)));
    }

    private static double calcY(double angle, double radius, double centerY) {
        return (double)(centerY + radius * Math.sin(Math.toRadians(-angle)));
    }

     /** Normalize any angle into -180 to 180 deg range */
    private static double normalizeAngle(double angle) {
        double a = angle % 360;
        if (a <= -180) a += 360;
        if (a > 180) a -= 360;
        return a;
    }

    // -------------- INNER CLASSES --------------------------------------------

    // Class holding label line layout info for collision detection and removal
    private final static class LabelLayoutInfo {
        double startX;
        double startY;
        double endX;
        double endY;
        double textX;
        double textY;
        Text text;
        double size;

        LabelLayoutInfo(double startX, double startY, double endX, double endY,
                double textX, double textY, Text text, double size) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.textX = textX;
            this.textY = textY;
            this.text = text;
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LabelLayoutInfo that = (LabelLayoutInfo) o;
            return Double.compare(that.startX, startX) == 0 &&
                    Double.compare(that.startY, startY) == 0 &&
                    Double.compare(that.endX, endX) == 0 &&
                    Double.compare(that.endY, endY) == 0 &&
                    Double.compare(that.textX, textX) == 0 &&
                    Double.compare(that.textY, textY) == 0 &&
                    Double.compare(that.size, size) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(startX, startY, endX, endY, textX, textY, size);
        }
    }

    /**
     * PieChart Data Item, represents one slice in the PieChart
     *
     * @since JavaFX 2.0
     */
    public final static class Data {

        private Text textNode = new Text();
        /**
         * Next pointer for the next data item : so we can do animation on data delete.
         */
        private Data next = null;

        /**
         * Default color index for this slice.
         */
        private int defaultColorIndex;

        // -------------- PUBLIC PROPERTIES ------------------------------------

        /**
         * The chart which this data belongs to.
         */
        private ReadOnlyObjectWrapper<PieChart> chart = new ReadOnlyObjectWrapper<PieChart>(this, "chart");

        public final PieChart getChart() {
            return chart.getValue();
        }

        private void setChart(PieChart value) {
            chart.setValue(value);
        }

        public final ReadOnlyObjectProperty<PieChart> chartProperty() {
            return chart.getReadOnlyProperty();
        }

        /**
         * The name of the pie slice
         */
        private StringProperty name = new StringPropertyBase() {
            @Override
            protected void invalidated() {
                if (getChart() != null) getChart().dataNameChanged(Data.this);
            }

            @Override
            public Object getBean() {
                return Data.this;
            }

            @Override
            public String getName() {
                return "name";
            }
        };

        public final void setName(java.lang.String value) {
            name.setValue(value);
        }

        public final java.lang.String getName() {
            return name.getValue();
        }

        public final StringProperty nameProperty() {
            return name;
        }

        /**
         * The value of the pie slice
         */
        private DoubleProperty pieValue = new DoublePropertyBase() {
            @Override
            protected void invalidated() {
                if (getChart() != null) getChart().dataPieValueChanged(Data.this);
            }

            @Override
            public Object getBean() {
                return Data.this;
            }

            @Override
            public String getName() {
                return "pieValue";
            }
        };

        public final double getPieValue() {
            return pieValue.getValue();
        }

        public final void setPieValue(double value) {
            pieValue.setValue(value);
        }

        public final DoubleProperty pieValueProperty() {
            return pieValue;
        }

        /**
         * The current pie value, used during animation. This will be the last data value, new data value or
         * anywhere in between
         */
        private DoubleProperty currentPieValue = new SimpleDoubleProperty(this, "currentPieValue");

        private double getCurrentPieValue() {
            return currentPieValue.getValue();
        }

        private void setCurrentPieValue(double value) {
            currentPieValue.setValue(value);
        }

        private DoubleProperty currentPieValueProperty() {
            return currentPieValue;
        }

        /**
         * Multiplier that is used to animate the radius of the pie slice
         */
        private DoubleProperty radiusMultiplier = new SimpleDoubleProperty(this, "radiusMultiplier");

        private double getRadiusMultiplier() {
            return radiusMultiplier.getValue();
        }

        private void setRadiusMultiplier(double value) {
            radiusMultiplier.setValue(value);
        }

        private DoubleProperty radiusMultiplierProperty() {
            return radiusMultiplier;
        }

        /**
         * Readonly access to the node that represents the pie slice. You can use this to add mouse event listeners etc.
         */
        private ReadOnlyObjectWrapper<Node> node = new ReadOnlyObjectWrapper<>(this, "node");

        /**
         * Returns the node that represents the pie slice. You can use this to
         * add mouse event listeners etc.
         * @return the node that represents the pie slice
         */
        public Node getNode() {
            return node.getValue();
        }

        private void setNode(Node value) {
            node.setValue(value);
        }

        public ReadOnlyObjectProperty<Node> nodeProperty() {
            return node.getReadOnlyProperty();
        }

        // -------------- CONSTRUCTOR -------------------------------------------------

        /**
         * Constructs a PieChart.Data object with the given name and value.
         *
         * @param name  name for Pie
         * @param value pie value
         */
        public Data(java.lang.String name, double value) {
            setName(name);
            setPieValue(value);
            textNode.getStyleClass().addAll("text", "chart-pie-label");
            textNode.setAccessibleRole(AccessibleRole.TEXT);
            textNode.setAccessibleRoleDescription("slice");
            textNode.focusTraversableProperty().bind(Platform.accessibilityActiveProperty());
            textNode.accessibleTextProperty().bind( new StringBinding() {
                {bind(nameProperty(), currentPieValueProperty());}
                @Override protected String computeValue() {
                    return getName() + " represents " + getCurrentPieValue() + " percent";
                }
            });
        }

        // -------------- PUBLIC METHODS ----------------------------------------------

        /**
         * Returns a string representation of this {@code Data} object.
         *
         * @return a string representation of this {@code Data} object.
         */
        @Override
        public java.lang.String toString() {
            return "Data[" + getName() + "," + getPieValue() + "]";
        }
    }

    // -------------- STYLESHEET HANDLING --------------------------------------

    /*
     * Super-lazy instantiation pattern from Bill Pugh.
     */
     private static class StyleableProperties {
         private static final CssMetaData<PieChart,Boolean> CLOCKWISE =
             new CssMetaData<PieChart,Boolean>("-fx-clockwise",
                 BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(PieChart node) {
                return node.clockwise == null || !node.clockwise.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(PieChart node) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)node.clockwiseProperty();
            }
        };

         private static final CssMetaData<PieChart,Boolean> LABELS_VISIBLE =
             new CssMetaData<PieChart,Boolean>("-fx-pie-label-visible",
                 BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(PieChart node) {
                return node.labelsVisible == null || !node.labelsVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(PieChart node) {
                return (StyleableProperty<Boolean>)(WritableValue<Boolean>)node.labelsVisibleProperty();
            }
        };

         private static final CssMetaData<PieChart,Number> LABEL_LINE_LENGTH =
             new CssMetaData<PieChart,Number>("-fx-label-line-length",
                 SizeConverter.getInstance(), 20d) {

            @Override
            public boolean isSettable(PieChart node) {
                return node.labelLineLength == null || !node.labelLineLength.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(PieChart node) {
                return (StyleableProperty<Number>)(WritableValue<Number>)node.labelLineLengthProperty();
            }
        };

         private static final CssMetaData<PieChart,Number> START_ANGLE =
             new CssMetaData<PieChart,Number>("-fx-start-angle",
                 SizeConverter.getInstance(), 0d) {

            @Override
            public boolean isSettable(PieChart node) {
                return node.startAngle == null || !node.startAngle.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(PieChart node) {
                return (StyleableProperty<Number>)(WritableValue<Number>)node.startAngleProperty();
            }
        };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Chart.getClassCssMetaData());
            styleables.add(CLOCKWISE);
            styleables.add(LABELS_VISIBLE);
            styleables.add(LABEL_LINE_LENGTH);
            styleables.add(START_ANGLE);
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
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

}


