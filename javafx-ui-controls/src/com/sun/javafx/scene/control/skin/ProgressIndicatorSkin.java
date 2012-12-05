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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.Utils;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.scene.control.behavior.ProgressIndicatorBehavior;
import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.scene.control.SkinBase;

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


    private static final ObservableList<String> paths;
    static {
        paths = FXCollections.<String>observableArrayList();
        paths.addAll("M 18.19152 4.2642355 L 15.734064 5.984965 L 15.734064 5.984965 C  16.55803 7.1617074 17.0 8.563462 17.0 10.0 L 20.0 10.0 C  20.0 7.9478035 19.368612 5.9452963 18.19152 4.2642355 Z", "M 11.736482 0.15192246 L 11.215537 3.1063457 L 11.215537 3.1063457 C  12.63025 3.3557978 13.933962 4.034467 14.949747 5.0502524 L 10.0 10.0 L 17.071068 2.9289322 C  15.619946 1.4778103 13.757501 0.5082826 11.736482 0.15192246 Z", "M 10.0 0.0 C  7.9478035 0.0 5.9452963 0.6313881 4.2642355 1.8084795 L 5.984965 4.265936 L 5.984965 4.265936 C  7.1617074 3.4419718 8.563462 3.0 10.0 3.0 L 10.0 0.0 Z", "M 2.9289322 2.9289322 C  1.4778103 4.380054 0.5082826 6.2424994 0.15192246 8.263518 L 3.1063457 8.784463 L 3.1063457 8.784463 C  3.3557978 7.3697495 4.034467 6.0660377 5.0502524 5.0502524 L 5.0502524 5.0502524 L 2.9289322 2.9289322 Z", "M 0.0 10.0 C  0.0 12.0521965 0.6313881 14.054704 1.8084795 15.7357645 L 10.0 10.0 L 4.265936 14.015035 C  3.4419718 12.838292 3.0 11.436538 3.0 10.0 Z", "M 10.0 10.0 L 8.784463 16.893654 C  7.3697495 16.644201 6.0660377 15.965533 5.050253 14.949747 L 5.0502524 14.949747 L 2.9289322 17.071068 C  4.380054 18.52219 6.2424994 19.491718 8.263518 19.848078 L 10.0 10.0 Z", "M 10.0 10.0 L 14.015035 15.734064 C  12.838292 16.55803 11.436538 17.0 10.0 17.0 L 10.0 20.0 C  12.0521965 20.0 14.054704 19.368612 15.7357645 18.19152 L 10.0 10.0 Z", "M 10.0 10.0 L 16.893654 11.215537 C  16.644201 12.63025 15.965533 13.933962 14.949747 14.949747 L 17.071068 17.071068 C  18.52219 15.619946 19.491718 13.757501 19.848078 11.736482 L 10.0 10.0 Z");
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
                    spinner = new IndeterminateSpinner(getSkinnable(), ProgressIndicatorSkin.this);
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
                        spinner = new IndeterminateSpinner(getSkinnable(), ProgressIndicatorSkin.this);
                        getChildren().add(spinner);
                        if (getSkinnable().impl_isTreeVisible()) {
                            spinner.indeterminateTimeline.play();
                        }
                        requestLayout();
                    }
                }
            }
        };
        control.sceneProperty().addListener(sceneListener);

        initialize();
        requestLayout();
    }

    private void initialize() {
        ProgressIndicator control = getSkinnable();
        boolean isIndeterminate = control.isIndeterminate();
        if (isIndeterminate) {
            // clean up determinateIndicator
            determinateIndicator = null;
            // create spinner
            spinner = new IndeterminateSpinner(control, this);
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
            determinateIndicator = new com.sun.javafx.scene.control.skin.ProgressIndicatorSkin.DeterminateIndicator(control, this);
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
        private Font font;
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
        Arc arcShape;
        Arc arcProgress;

        public DeterminateIndicator(ProgressIndicator control, ProgressIndicatorSkin s) {
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

            arcProgress = new Arc();
            arcProgress.setType(ArcType.ROUND);
            arcProgress.setStartAngle(90.0F);
            arcProgress.setFill(skin.getProgressColor());

            // Our progress pie piece
            progress = new StackPane() {
                @Override protected void layoutChildren() {
                    arcProgress.setFill(skin.getProgressColor());
                }
            };

            progress.getStyleClass().setAll("progress");
            progress.setScaleShape(false);
            progress.setCenterShape(false);
            progress.setShape(arcShape);
            progress.getChildren().clear();
            progress.getChildren().addAll(arcProgress);

            // The check mark that's drawn at 100%
            tick = new StackPane();
            tick.getStyleClass().setAll("tick");

            getChildren().setAll(indicator, progress, text, tick);
            updateProgress();
        }

        @Override public boolean isAutomaticallyMirrored() {
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
            arcProgress.setLength(-degProgress);
            requestLayout();
        }

        @Override protected void layoutChildren() {
            // Position and size the circular background
            double doneTextHeight = doneText.getLayoutBounds().getHeight();
            /*
            ** use the min of width, or height, keep it a circle
            */
            double areaW = (control.getWidth() - (skin.getInsets().getLeft() + skin.getInsets().getRight()));
            double areaH = (control.getHeight() - (skin.getInsets().getTop() + skin.getInsets().getBottom()));

            double radiusW = areaW / 2;
            double radiusH = (areaH-(textGap+doneTextHeight)) / 2;
            double radius = Math.min(radiusW, radiusH);

            indicator.setShape(new Circle(radius));
            indicator.resize(2 * radius, 2 * radius);

            /*
            ** we need to work out the available space between the padding,
            ** and centre the indicator inside it
            */
            indicator.setLayoutX(skin.getInsets().getLeft()+(radiusW - radius));
            indicator.setLayoutY(skin.getInsets().getTop()+(radiusH - radius));


            arcShape.setRadiusX(((indicator.getWidth() - indicator.getInsets().getLeft() - indicator.getInsets().getRight()) / 2));
            arcShape.setRadiusY(arcShape.getRadiusX());
            arcProgress.setRadiusX(arcShape.getRadiusX()-1);
            arcProgress.setRadiusY(arcShape.getRadiusY()-1);


            progress.setLayoutX(indicator.getLayoutX() + radius);
            progress.setLayoutY(indicator.getLayoutY() + radius);
            progress.resize(2 * arcShape.getRadiusX(), 2 * arcShape.getRadiusY());

            tick.setLayoutX(indicator.getLayoutX() + (indicator.getWidth() / 2) - (tick.getWidth() / 2));
            tick.setLayoutY(indicator.getLayoutY() + (indicator.getHeight() / 2) - (tick.getHeight() / 2));
            tick.setVisible(control.getProgress() >= 1);

            /*
            ** if the % text can't fit anywhere in the bounds then don't display it
            */
            double textWidth = com.sun.javafx.scene.control.skin.Utils.computeTextWidth(font, text.getText(), 0.0);
            double textHeight = com.sun.javafx.scene.control.skin.Utils.computeTextHeight(font, text.getText(), 0.0);
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
                    text.setLayoutX(skin.getInsets().getLeft()+(radiusW - radius));
                }
                else {
                    text.setLayoutX(skin.getInsets().getLeft()+((radiusW*2 - textWidth)/2));
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
        protected ProgressIndicatorSkin skin;
        private IndicatorPaths pathsG;
        Scale scaleTransform;
        Rotate rotateTransform;

        public IndeterminateSpinner(ProgressIndicator control, ProgressIndicatorSkin s) {
            this.control = control;
            this.skin = s;

            setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            getStyleClass().setAll("spinner");

            skin.segmentColors = FXCollections.<Color>observableArrayList();
            skin.svgpaths = FXCollections.<SVGPath>observableArrayList();
            skin.setColors(skin.getProgressColor());

            pathsG = new IndicatorPaths(this);

            scaleTransform = new Scale();

            rotateTransform = new Rotate();
            rotateTransform.setAngle(angle);

            pathsG.getChildren().clear();
            pathsG.getChildren().addAll(skin.svgpaths);

            indeterminateTimeline = new Timeline();
            indeterminateTimeline.setCycleCount(Timeline.INDEFINITE);

            ObservableList<KeyFrame> keyframes = FXCollections.<KeyFrame>observableArrayList();
            for (int i = 100; i <= 3900; i += 100) {
                keyframes.add(
                      new KeyFrame(Duration.millis(i), new EventHandler<ActionEvent>() {
                              @Override public void handle(ActionEvent event) {
                                  skin.shiftColors();
                              }
                      }));
            }
            indeterminateTimeline.getKeyFrames().clear();
            indeterminateTimeline.getKeyFrames().addAll(keyframes);

            getChildren().clear();
            getChildren().addAll(pathsG);
            requestLayout();
        }

        void pauseIndicator(boolean pause) {
            if (indeterminateTimeline != null) {
                if (pause) {
                    indeterminateTimeline.pause();
                }
                else {
                    indeterminateTimeline.play();
                }
            }
        }


    class IndicatorPaths extends Group {
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
    }


        @Override protected void layoutChildren() {
            double radiusW = (control.getWidth() - (skin.getInsets().getLeft() + skin.getInsets().getRight())) / 2;
            double radiusH = (control.getHeight() - (skin.getInsets().getTop() + skin.getInsets().getBottom())) / 2;
            double radius = Math.min(radiusW, radiusH);

            scaleTransform.setX(radius/10);
            scaleTransform.setY(radius/10);

            rotateTransform.setPivotX(radius);
            rotateTransform.setPivotY(radius);

            pathsG.getTransforms().clear();
            pathsG.getTransforms().addAll(scaleTransform, rotateTransform);

            double diameter = radius*2;
            pathsG.resize(diameter, diameter);

            pathsG.setLayoutX(skin.getInsets().getLeft()+(radiusW - radius));
            pathsG.setLayoutY(skin.getInsets().getTop()+(radiusH - radius));
        }

        private Timeline indeterminateTimeline;
        private double angle = 0.0F;


        @Override protected double computePrefWidth(double height) {
            return getInsets().getLeft() + doneText.getLayoutBounds().getHeight() + getInsets().getRight();
        }

        @Override protected double computePrefHeight(double width) {
            /*
            ** use the same as the width, to keep it square
            */
            return getInsets().getTop() + doneText.getLayoutBounds().getHeight() + getInsets().getBottom();
        }

        @Override protected double computeMaxWidth(double height) {
            return computePrefWidth(-1);
        }

        @Override protected double computeMaxHeight(double width) {
            return computePrefHeight(-1);
        }
    }

    private ObservableList<Color> segmentColors;
    private ObservableList<SVGPath> svgpaths;

    private void setColors(Paint seedColor) {
        if (segmentColors != null) {
            segmentColors.clear();
            if (seedColor instanceof Color) {
                final Color c = (Color) seedColor;
                for (int i = 0; i <= 7; i++) segmentColors.add(Utils.deriveColor(c, -0.2F + 1.2F / 7.0F * i));
            } else {
                // as it a Paint we can not derive colors so just use as it
                for (int i = 0; i <= 7; i++) segmentColors.add((Color)seedColor);
            }

            for (int i = 0; i <= 7; i++) {
                SVGPath svgpath = new SVGPath();
                svgpath.setContent(paths.get(i));
                svgpath.setFill(segmentColors.get(i));
                svgpaths.add(svgpath);
            }
        }
    }

    private void shiftColors() {
        FXCollections.rotate(segmentColors, -1);
        for (int i = 0; i <= 7; i++) {
            svgpaths.get(i).setFill(segmentColors.get(i));
        }
    }

    public Paint getProgressColor() {
        return progressColor.get();
    }

    /**
     * The colour of the progress segment
     */
    private ObjectProperty<Paint> progressColor =            
            new StyleableObjectProperty<Paint>(Color.DODGERBLUE) {

        @Override public void set(Paint newProgressColor) {
            final Paint color = (newProgressColor instanceof Color)
                    ? newProgressColor 
                    : Color.DODGERBLUE;
            super.set(color);
        }
        
        @Override
        protected void invalidated() {
            setColors((Color)progressColor.get());
        }

        @Override
        public Object getBean() {
            return ProgressIndicatorSkin.this;
        }

        @Override
        public String getName() {
            return "progressColorProperty";
        }

        @Override
        public StyleableProperty getStyleableProperty() {
            return StyleableProperties.PROGRESS_COLOR;
        }
    };
        

    // *********** Stylesheet Handling *****************************************
    
    /**
     * Super-lazy instantiation pattern from Bill Pugh.
     * @treatAsPrivate implementation detail
     */
    private static class StyleableProperties {
        private static final StyleableProperty<ProgressIndicator,Paint> PROGRESS_COLOR =
            new StyleableProperty<ProgressIndicator,Paint>("-fx-progress-color",
                PaintConverter.getInstance(), Color.DODGERBLUE) {

            @Override
            public boolean isSettable(ProgressIndicator n) {
                final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) n.getSkin();
                return skin.progressColor == null || 
                        !skin.progressColor.isBound();
            }

            @Override
            public WritableValue<Paint> getWritableValue(ProgressIndicator n) {
                final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) n.getSkin();
                return skin.progressColor;
            }
        };

        public static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables = 
                new ArrayList<StyleableProperty>(SkinBase.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                               PROGRESS_COLOR
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return StyleableProperties.STYLEABLES;
    };

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }

}
