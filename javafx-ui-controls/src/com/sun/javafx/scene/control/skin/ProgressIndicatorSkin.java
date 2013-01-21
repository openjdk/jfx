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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import javafx.css.CssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.behavior.ProgressIndicatorBehavior;
import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.geometry.Insets;
import javafx.css.StyleableProperty;

public class ProgressIndicatorSkin extends BehaviorSkinBase<ProgressIndicator, ProgressIndicatorBehavior<ProgressIndicator>> {

    /***************************************************************************
     *                                                                         *
     * UI Subcomponents                                                        *
     *                                                                         *
     **************************************************************************/

    private static final String DONE = ControlResources.getString("ProgressIndicator.doneString");

    /** doneText is just used to know the size of done as that is the biggest text we need to allow for */
    private static final Text doneText = new Text(DONE);
    static {
        doneText.getStyleClass().add("text");
    }

    private IndeterminateSpinner spinner;
    private DeterminateIndicator determinateIndicator;
    private boolean timelineNulled = false;

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ProgressIndicatorSkin(ProgressIndicator control) {
        super(control, new ProgressIndicatorBehavior<ProgressIndicator>(control));

        InvalidationListener indeterminateListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                initialize();
            }
        };
        control.indeterminateProperty().addListener(indeterminateListener);

        InvalidationListener visibilityListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                if (getSkinnable().isIndeterminate() && timelineNulled && spinner == null) {
                    timelineNulled = false;
                    spinner = new IndeterminateSpinner(getSkinnable(), ProgressIndicatorSkin.this, spinEnabled.get(), progressColor.get());
                    getChildren().add(spinner);
                }
                
                if (spinner != null) {
                    if (getSkinnable().impl_isTreeVisible() && getSkinnable().getScene() != null) {
                        spinner.indeterminateTimeline.play();
                    }
                    else {
                        spinner.indeterminateTimeline.pause();
                        getChildren().remove(spinner);
                        spinner = null;
                        timelineNulled = true;
                    }
                }
            }
        };
        control.visibleProperty().addListener(visibilityListener);
        control.parentProperty().addListener(visibilityListener);

        InvalidationListener sceneListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                if (spinner != null) {
                    if (getSkinnable().getScene() == null) {
                        spinner.indeterminateTimeline.pause();
                        getChildren().remove(spinner);
                        spinner = null;
                        timelineNulled = true;
                    }
                }
                else {
                    if (getSkinnable().getScene() != null && getSkinnable().isIndeterminate()) {
                        timelineNulled = false;
                        spinner = new IndeterminateSpinner(getSkinnable(), ProgressIndicatorSkin.this, spinEnabled.get(), progressColor.get());
                        getChildren().add(spinner);
                        if (getSkinnable().impl_isTreeVisible()) {
                            spinner.indeterminateTimeline.play();
                        }
                        getSkinnable().requestLayout();
                    }
                }
            }
        };
        control.sceneProperty().addListener(sceneListener);

        initialize();
        getSkinnable().requestLayout();
    }

    private void initialize() {
        ProgressIndicator control = getSkinnable();
        boolean isIndeterminate = control.isIndeterminate();
        if (isIndeterminate) {
            // clean up determinateIndicator
            determinateIndicator = null;
            // create spinner
            spinner = new IndeterminateSpinner(control, this, spinEnabled.get(), progressColor.get());
            getChildren().clear();
            getChildren().add(spinner);
            if (getSkinnable().impl_isTreeVisible()) {
                spinner.indeterminateTimeline.play();
            }
        } else {
            // clean up after spinner
            if (spinner != null) {
                spinner.indeterminateTimeline.stop();
                spinner = null;
            }
            // create determinateIndicator
            determinateIndicator = new com.sun.javafx.scene.control.skin.ProgressIndicatorSkin.DeterminateIndicator(control, this, progressColor.get());
            getChildren().clear();
            getChildren().add(determinateIndicator);
        }
    }
    
    @Override public void dispose() {
        super.dispose();
        if (spinner != null) {
            spinner.indeterminateTimeline.stop();
            spinner = null;
        }
    }

    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        if (spinner != null && getSkinnable().isIndeterminate()) { 
            spinner.layoutChildren();
            spinner.resizeRelocate(0, 0, w, h);
        } else if (determinateIndicator != null) { 
            determinateIndicator.layoutChildren();
            determinateIndicator.resizeRelocate(0, 0, w, h);
        }
    }

    /***************************************************************************
     *                                                                         *
     * DeterminateIndicator                                                    *
     *                                                                         *
     **************************************************************************/

    static class DeterminateIndicator extends Region {
        private double textGap = 2.0F;

        // only update progress text on whole percentages
        private int intProgress;

        // only update pie arc to nearest degree
        private int degProgress;
        private ProgressIndicator control;
        private ProgressIndicatorSkin skin;
        private Text text;
        private StackPane indicator;
        private StackPane progress;
        private StackPane tick;
        private Arc arcShape;

        public DeterminateIndicator(ProgressIndicator control, ProgressIndicatorSkin s, Paint fillOverride) {
            this.control = control;
            this.skin = s;
            
            getStyleClass().add("determinate-indicator");

            intProgress = (int) Math.round(control.getProgress() * 100.0) ;
            degProgress = (int) (360 * control.getProgress());

            InvalidationListener progressListener = new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    updateProgress();
                }
            };
            control.progressProperty().addListener(progressListener);

            getChildren().clear();

            text = new Text((control.getProgress() >= 1) ? (DONE) : ("" + intProgress + "%"));
            text.setTextOrigin(VPos.TOP);
            text.getStyleClass().setAll("text", "percentage");

            // The circular background for the progress pie piece
            indicator = new StackPane();
            indicator.getStyleClass().setAll("indicator");

            // The shape for our progress pie piece
            arcShape = new Arc();
            arcShape.setType(ArcType.ROUND);
            arcShape.setStartAngle(90.0F);

            // Our progress pie piece
            progress = new StackPane();
            progress.getStyleClass().setAll("progress");
            progress.setScaleShape(false);
            progress.setCenterShape(false);
            progress.setShape(arcShape);
            progress.getChildren().clear();
            setFillOverride(fillOverride);

            // The check mark that's drawn at 100%
            tick = new StackPane();
            tick.getStyleClass().setAll("tick");

            getChildren().setAll(indicator, progress, text, tick);
            updateProgress();
        }

        private void setFillOverride(Paint fillOverride) {
            if (fillOverride instanceof Color) {
                Color c = (Color)fillOverride;
                progress.setStyle("-fx-background-color: rgba("+((int)(255*c.getRed()))+","+((int)(255*c.getGreen()))+","+((int)(255*c.getBlue()))+","+c.getOpacity()+");");
            } else {
                progress.setStyle(null);
            }
        }

        @Override public boolean usesMirroring() {
            // This is used instead of setting NodeOrientation,
            // allowing the Text node to inherit the current
            // orientation.
            return false;
        }

        private void updateProgress() {
            intProgress = (int) Math.round(control.getProgress() * 100.0) ;
            text.setText((control.getProgress() >= 1) ? (DONE) : ("" + intProgress + "%"));

            degProgress = (int) (360 * control.getProgress());
            arcShape.setLength(-degProgress);
            requestLayout();
        }

        @Override protected void layoutChildren() {
            // Position and size the circular background
            double doneTextHeight = doneText.getLayoutBounds().getHeight();
            final Insets controlInsets = control.getInsets();
            
            /*
            ** use the min of width, or height, keep it a circle
            */
            double areaW = (control.getWidth() - (controlInsets.getLeft() + controlInsets.getRight()));
            double areaH = (control.getHeight() - (controlInsets.getTop() + controlInsets.getBottom()));

            double radiusW = areaW / 2;
            double radiusH = (areaH-(textGap+doneTextHeight)) / 2;
            double radius = Math.min(radiusW, radiusH);

            indicator.setShape(new Circle(radius));
            indicator.resize(2 * radius, 2 * radius);

            /*
            ** we need to work out the available space between the padding,
            ** and centre the indicator inside it
            */
            indicator.setLayoutX(controlInsets.getLeft()+(radiusW - radius));
            indicator.setLayoutY(controlInsets.getTop()+(radiusH - radius));


            arcShape.setRadiusX(((indicator.getWidth() - indicator.getInsets().getLeft() - indicator.getInsets().getRight()) / 2));
            arcShape.setRadiusY(arcShape.getRadiusX());


            progress.setLayoutX(indicator.getLayoutX() + radius);
            progress.setLayoutY(indicator.getLayoutY() + radius);
            progress.resize(2 * arcShape.getRadiusX(), 2 * arcShape.getRadiusY());

            tick.setLayoutX(indicator.getLayoutX() + (indicator.getWidth() / 2) - (tick.getWidth() / 2));
            tick.setLayoutY(indicator.getLayoutY() + (indicator.getHeight() / 2) - (tick.getHeight() / 2));
            tick.setVisible(control.getProgress() >= 1);

            /*
            ** if the % text can't fit anywhere in the bounds then don't display it
            */
            double textWidth = com.sun.javafx.scene.control.skin.Utils.computeTextWidth(text.getFont(), text.getText(), 0.0);
            double textHeight = com.sun.javafx.scene.control.skin.Utils.computeTextHeight(text.getFont(), text.getText(), 0.0);
            if (control.getWidth() >= textWidth && control.getHeight() >= textHeight) {
                if (!text.isVisible()) {
                    text.setVisible(true);
                }
                text.setLayoutY(indicator.getLayoutY()+indicator.getHeight() + textGap);
                /*
                ** try to centre the text at the indicators radius.
                ** but if it can't then use the padding
                */
                if (textWidth > (radiusW*2)) {
                    text.setLayoutX(controlInsets.getLeft()+(radiusW - radius));
                }
                else {
                    text.setLayoutX(controlInsets.getLeft()+((radiusW*2 - textWidth)/2));
                }
            }
            else {
                if (text.isVisible()) {
                    text.setVisible(false);
                }
            }
        }

        @Override protected double computePrefWidth(double height) {
            final double indW = indicator.getInsets().getLeft() + indicator.getInsets().getRight() + progress.getInsets().getLeft() + progress.getInsets().getRight();
            return getInsets().getLeft() + Math.max(indW, doneText.getLayoutBounds().getWidth()) + getInsets().getRight();
        }

        @Override protected double computePrefHeight(double width) {
            double indH = indicator.getInsets().getTop() + indicator.getInsets().getBottom() + progress.getInsets().getTop() + progress.getInsets().getBottom();
            return getInsets().getTop() + indH + textGap + doneText.getLayoutBounds().getHeight() + getInsets().getBottom();
        }

        @Override protected double computeMaxWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMaxHeight(double width) {
            return computePrefHeight(width);
        }
    }

    /***************************************************************************
     *                                                                         *
     * IndeterminateSpinner                                                    *
     *                                                                         *
     **************************************************************************/

    static class IndeterminateSpinner extends Region {
        private ProgressIndicator control;
        private ProgressIndicatorSkin skin;
        private IndicatorPaths pathsG;
        private Timeline indeterminateTimeline;
        private double angle = 0.0F;
        private final List<Double> opacities = new ArrayList<Double>();
        private boolean spinEnabled = false;
        private Paint fillOverride = null;

        public IndeterminateSpinner(ProgressIndicator control, ProgressIndicatorSkin s, boolean spinEnabled, Paint fillOverride) {
            this.control = control;
            this.skin = s;
            this.spinEnabled = spinEnabled;
            this.fillOverride = fillOverride;

            setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            getStyleClass().setAll("spinner");

            pathsG = new IndicatorPaths(this);
            getChildren().add(pathsG);

            indeterminateTimeline = new Timeline();
            indeterminateTimeline.setCycleCount(Timeline.INDEFINITE);
            rebuildTimeline();

            rebuild();
        }

        public void setFillOverride(Paint fillOverride) {
            this.fillOverride = fillOverride;
            rebuild();
        }

        public void setSpinEnabled(boolean spinEnabled) {
            this.spinEnabled = spinEnabled;
            rebuildTimeline();
        }

        private void rebuildTimeline() {
            final ObservableList<KeyFrame> keyFrames = FXCollections.<KeyFrame>observableArrayList();
            if(spinEnabled) {
                keyFrames.add(new KeyFrame(Duration.millis(0), new KeyValue(pathsG.rotateProperty(), 360)));
                keyFrames.add(new KeyFrame(Duration.millis(3900), new KeyValue(pathsG.rotateProperty(), 0)));
            }
            for (int i = 100; i <= 3900; i += 100) {
                keyFrames.add(
                        new KeyFrame(
                                Duration.millis(i), new EventHandler<ActionEvent>() {
                            @Override public void handle(ActionEvent event) {
                                shiftColors();
                            }
                        }));
            }
            indeterminateTimeline.getKeyFrames().setAll(keyFrames);
        }

        private void pauseIndicator(boolean pause) {
            if (indeterminateTimeline != null) {
                if (pause) {
                    indeterminateTimeline.pause();
                }
                else {
                    indeterminateTimeline.play();
                }
            }
        }

        private class IndicatorPaths extends Pane {
            IndeterminateSpinner piSkin;
            IndicatorPaths(IndeterminateSpinner pi) {
                super();
                piSkin = pi;
                InvalidationListener treeVisibilityListener = new InvalidationListener() {
                        @Override public void invalidated(Observable valueModel) {
                            if (piSkin.skin.getSkinnable().impl_isTreeVisible()) {
                                piSkin.pauseIndicator(false);
                            }
                            else {
                                piSkin.pauseIndicator(true);
                            }
                        }
                    };
                impl_treeVisibleProperty().addListener(treeVisibilityListener);
            }

            @Override protected double computePrefWidth(double height) {
                double w = 0;
                for(Node child: getChildren()) {
                    if (child instanceof Region) {
                        Region region = (Region)child;
                        if (region.getShape() != null) {
                            w = Math.max(w,region.getShape().getLayoutBounds().getMaxX());
                        } else {
                            w = Math.max(w,region.prefWidth(height));
                        }
                    }
                }
                return w;
            }

            @Override protected double computePrefHeight(double width) {
                double h = 0;
                for(Node child: getChildren()) {
                    if (child instanceof Region) {
                        Region region = (Region)child;
                        if (region.getShape() != null) {
                            h = Math.max(h,region.getShape().getLayoutBounds().getMaxY());
                        } else {
                            h = Math.max(h,region.prefHeight(width));
                        }
                    }
                }
                return h;
            }

            @Override protected void layoutChildren() {
                // calculate scale
                double scale = getWidth() / computePrefWidth(-1);
                for(Node child: getChildren()) {
                    if (child instanceof Region) {
                        Region region = (Region)child;
                        if (region.getShape() != null) {
                            region.resize(
                                region.getShape().getLayoutBounds().getMaxX(),
                                region.getShape().getLayoutBounds().getMaxY()
                            );
                            region.getTransforms().setAll(new Scale(scale,scale,0,0));
                        } else {
                            region.autosize();
                        }
                    }
                }
            }
        }

        @Override protected void layoutChildren() {
            Insets controlInsets = control.getInsets();
            final double w = control.getWidth() - controlInsets.getLeft() - controlInsets.getRight();
            final double h = control.getHeight() - controlInsets.getTop() - controlInsets.getBottom();
            final double prefW = pathsG.prefWidth(-1);
            final double prefH = pathsG.prefHeight(-1);
            double scaleX = w / prefW;
            double scale = scaleX;
            if ((scaleX * prefH) > h) {
                scale = h / prefH;
            }
            double indicatorW = prefW * scale;
            double indicatorH = prefH * scale;
            pathsG.resizeRelocate((w - indicatorW) / 2, (h - indicatorH) / 2, indicatorW, indicatorH);
        }

        private void rebuild() {
            // update indeterminate indicator
            final int segments = skin.indeterminateSegmentCount.get();
            opacities.clear();
            pathsG.getChildren().clear();
            final double step = 0.8/(segments-1);
            for (int i = 0; i < segments; i++) {
                Region region = new Region();
                region.setScaleShape(false);
                region.setCenterShape(false);
                region.getStyleClass().addAll("segment", "segment" + i);
                if (fillOverride instanceof Color) {
                    Color c = (Color)fillOverride;
                    region.setStyle("-fx-background-color: rgba("+((int)(255*c.getRed()))+","+((int)(255*c.getGreen()))+","+((int)(255*c.getBlue()))+","+c.getOpacity()+");");
                } else {
                    region.setStyle(null);
                }
                pathsG.getChildren().add(region);
                opacities.add(Math.min(1, 0.2 + (i * step)));
            }
        }

        private void shiftColors() {
            if (opacities.size() <= 0) return;
            final int segments = skin.indeterminateSegmentCount.get();
            Collections.rotate(opacities, -1);
            for (int i = 0; i < segments; i++) {
                pathsG.getChildren().get(i).setOpacity(opacities.get(i));
            }
        }
    }

    public Paint getProgressColor() {
        return progressColor.get();
    }

    /**
     * The colour of the progress segment.
     */
    private ObjectProperty<Paint> progressColor =
            new StyleableObjectProperty<Paint>(null) {

                @Override public void set(Paint newProgressColor) {
                    final Paint color = (newProgressColor instanceof Color)
                            ? newProgressColor
                            : null;
                    super.set(color);
                }

                @Override protected void invalidated() {
                    if (spinner!=null) spinner.setFillOverride(get());
                    if (determinateIndicator!=null) determinateIndicator.setFillOverride(get());
                }

                @Override public Object getBean() {
                    return ProgressIndicatorSkin.this;
                }

                @Override public String getName() {
                    return "progressColorProperty";
                }

                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.PROGRESS_COLOR;
                }
            };

    /**
     * The number of segments in the spinner.
     */
    private IntegerProperty indeterminateSegmentCount =
            new StyleableIntegerProperty(8) {

                @Override protected void invalidated() {
                    if (spinner!=null) spinner.rebuild();
                }

                @Override public Object getBean() {
                    return ProgressIndicatorSkin.this;
                }

                @Override public String getName() {
                    return "indeterminateSegmentCount";
                }

                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.PROGRESS_COLOR;
                }
            };

    /**
     * True if the progress indicator should rotate as well as animate opacity.
     */
    private final BooleanProperty spinEnabled = new StyleableBooleanProperty(false) {
        @Override protected void invalidated() {
            if (spinner!=null) spinner.setSpinEnabled(get());
        }

        @Override public CssMetaData getCssMetaData() {
            return StyleableProperties.LEGEND_VISIBLE;
        }

        @Override public Object getBean() {
            return ProgressIndicatorSkin.this;
        }

        @Override public String getName() {
            return "spinEnabled";
        }
    };

    // *********** Stylesheet Handling *****************************************

    /**
     * Super-lazy instantiation pattern from Bill Pugh.
     * @treatAsPrivate implementation detail
     */
    private static class StyleableProperties {
        private static final CssMetaData<ProgressIndicator,Paint> PROGRESS_COLOR =
            new CssMetaData<ProgressIndicator,Paint>("-fx-progress-color",
                PaintConverter.getInstance(), null) {

            @Override
            public boolean isSettable(ProgressIndicator n) {
                final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) n.getSkin();
                return skin.progressColor == null ||
                        !skin.progressColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(ProgressIndicator n) {
                final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) n.getSkin();
                return (StyleableProperty)skin.progressColor;
            }
        };
        private static final CssMetaData<ProgressIndicator,Number> INDETERMINATE_SEGMENT_COUNT =
            new CssMetaData<ProgressIndicator,Number>("-fx-indeterminate-segment-count",
                                                     SizeConverter.getInstance(), 8) {

            @Override public void set(ProgressIndicator node, Number value, StyleOrigin origin) {
                super.set(node, value.intValue(), origin);
            }

            @Override public boolean isSettable(ProgressIndicator n) {
                final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) n.getSkin();
                return skin.indeterminateSegmentCount == null ||
                        !skin.indeterminateSegmentCount.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(ProgressIndicator n) {
                final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) n.getSkin();
                return (StyleableProperty)skin.indeterminateSegmentCount;
            }
        };
        private static final CssMetaData<ProgressIndicator,Boolean> LEGEND_VISIBLE =
            new CssMetaData<ProgressIndicator,Boolean>("-fx-spin-enabled",
                                           BooleanConverter.getInstance(), Boolean.FALSE) {

                @Override public boolean isSettable(ProgressIndicator node) {
                    final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) node.getSkin();
                    return skin.spinEnabled == null || !skin.spinEnabled.isBound();
                }

                @Override public StyleableProperty<Boolean> getStyleableProperty(ProgressIndicator node) {
                    final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) node.getSkin();
                    return (StyleableProperty)skin.spinEnabled;
                }
            };

        public static final List<CssMetaData> STYLEABLES;
        static {
            final List<CssMetaData> styleables = 
                new ArrayList<CssMetaData>(SkinBase.getClassCssMetaData());
            Collections.addAll(styleables,
                               PROGRESS_COLOR,
                               INDETERMINATE_SEGMENT_COUNT,
                               LEGEND_VISIBLE
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
