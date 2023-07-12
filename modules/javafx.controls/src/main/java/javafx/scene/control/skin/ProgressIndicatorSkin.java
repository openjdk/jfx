/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.scene.TreeShowingProperty;
import com.sun.javafx.scene.control.skin.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Control;
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
import javafx.scene.text.TextBoundsType;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import javafx.css.CssMetaData;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;
import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.css.Styleable;

/**
 * Default skin implementation for the {@link ProgressIndicator} control.
 *
 * @see ProgressIndicator
 * @since 9
 */
public class ProgressIndicatorSkin extends SkinBase<ProgressIndicator> {

    /* *************************************************************************
     *                                                                         *
     * Static fields                                                           *
     *                                                                         *
     **************************************************************************/



    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    // JDK-8149818: This constant should not be static, because the
    // Locale may change between instances.

    /** DONE string is just used to know the size of Done as that is the biggest text we need to allow for */
    private final String DONE = ControlResources.getString("ProgressIndicator.doneString");

    final Duration CLIPPED_DELAY = new Duration(300);
    final Duration UNCLIPPED_DELAY = new Duration(0);

    private IndeterminateSpinner spinner;
    private DeterminateIndicator determinateIndicator;
    private ProgressIndicator control;
    private TreeShowingProperty treeShowingProperty;

    Animation indeterminateTransition;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ProgressIndicatorSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ProgressIndicatorSkin(ProgressIndicator control) {
        super(control);

        this.control = control;
        this.treeShowingProperty = new TreeShowingProperty(control);

        // register listeners
        registerChangeListener(control.indeterminateProperty(), e -> initialize());
        registerChangeListener(control.progressProperty(), e -> updateProgress());
        registerChangeListener(control.sceneProperty(), e->updateAnimation());
        registerChangeListener(treeShowingProperty, e -> updateAnimation());

        initialize();
        updateAnimation();
    }



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The colour of the progress segment.
     */
    private ObjectProperty<Paint> progressColor = new StyleableObjectProperty<Paint>(null) {
        @Override protected void invalidated() {
            final Paint value = get();
            if (value != null && !(value instanceof Color)) {
                if (isBound()) {
                    unbind();
                }
                set(null);
                throw new IllegalArgumentException("Only Color objects are supported");
            }
            if (spinner!=null) spinner.setFillOverride(value);
            if (determinateIndicator!=null) determinateIndicator.setFillOverride(value);
        }

        @Override public Object getBean() {
            return ProgressIndicatorSkin.this;
        }

        @Override public String getName() {
            return "progressColorProperty";
        }

        @Override public CssMetaData<ProgressIndicator,Paint> getCssMetaData() {
            return PROGRESS_COLOR;
        }
    };

    Paint getProgressColor() {
        return progressColor.get();
    }

    /**
     * The number of segments in the spinner.
     */
    private IntegerProperty indeterminateSegmentCount = new StyleableIntegerProperty(8) {
        @Override protected void invalidated() {
            if (spinner!=null) spinner.rebuild();
        }

        @Override public Object getBean() {
            return ProgressIndicatorSkin.this;
        }

        @Override public String getName() {
            return "indeterminateSegmentCount";
        }

        @Override public CssMetaData<ProgressIndicator,Number> getCssMetaData() {
            return INDETERMINATE_SEGMENT_COUNT;
        }
    };

    /**
     * True if the progress indicator should rotate as well as animate opacity.
     */
    private final BooleanProperty spinEnabled = new StyleableBooleanProperty(false) {
        @Override protected void invalidated() {
            if (spinner!=null) spinner.setSpinEnabled(get());
        }

        @Override public CssMetaData<ProgressIndicator,Boolean> getCssMetaData() {
            return SPIN_ENABLED;
        }

        @Override public Object getBean() {
            return ProgressIndicatorSkin.this;
        }

        @Override public String getName() {
            return "spinEnabled";
        }
    };



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        treeShowingProperty.dispose();

        if (indeterminateTransition != null) {
            indeterminateTransition.stop();
            indeterminateTransition = null;
        }

        if (spinner != null) {
            spinner = null;
        }

        control = null;
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        if (spinner != null && control.isIndeterminate()) {
            spinner.layoutChildren();
            spinner.resizeRelocate(0, 0, w, h);
        } else if (determinateIndicator != null) {
            determinateIndicator.layoutChildren();
            determinateIndicator.resizeRelocate(0, 0, w, h);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double minWidth = 0.0;

        if (spinner != null && control.isIndeterminate()) {
            minWidth = spinner.minWidth(-1);
        } else if (determinateIndicator != null) {
            minWidth = determinateIndicator.minWidth(-1);
        }
        return minWidth;
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double minHeight = 0.0;

        if (spinner != null && control.isIndeterminate()) {
            minHeight = spinner.minHeight(-1);
        } else if (determinateIndicator != null) {
            minHeight = determinateIndicator.minHeight(-1);
        }
        return minHeight;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double prefWidth = 0.0;

        if (spinner != null && control.isIndeterminate()) {
            prefWidth = spinner.prefWidth(height);
        } else if (determinateIndicator != null) {
            prefWidth = determinateIndicator.prefWidth(height);
        }
        return prefWidth;
    }

   /** {@inheritDoc} */
   @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double prefHeight = 0.0;

        if (spinner != null && control.isIndeterminate()) {
            prefHeight = spinner.prefHeight(width);
        } else if (determinateIndicator != null) {
            prefHeight = determinateIndicator.prefHeight(width);
        }
        return prefHeight;
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
    }


    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    void initialize() {
        boolean isIndeterminate = control.isIndeterminate();
        if (isIndeterminate) {
            // clean up the old determinateIndicator
            if (determinateIndicator != null) {
                determinateIndicator.unregisterListener();
            }
            determinateIndicator = null;

            // create spinner
            spinner = new IndeterminateSpinner(spinEnabled.get(), progressColor.get());
            getChildren().setAll(spinner);
            if (NodeHelper.isTreeShowing(control)) {
                if (indeterminateTransition != null) {
                    indeterminateTransition.play();
                }
            }
        } else {
            // clean up after spinner
            if (spinner != null) {
                if (indeterminateTransition != null) {
                    indeterminateTransition.stop();
                }
                spinner = null;
            }

            // create determinateIndicator
            determinateIndicator = new DeterminateIndicator(control, this, progressColor.get());
            getChildren().setAll(determinateIndicator);
        }
    }

    void updateProgress() {
        if (determinateIndicator != null) {
            determinateIndicator.updateProgress(control.getProgress());
        }
    }

    void createIndeterminateTimeline() {
        if (spinner != null) {
            spinner.rebuildTimeline();
        }
    }

    void pauseTimeline(boolean pause) {
        if (getSkinnable().isIndeterminate()) {
            if (indeterminateTransition == null) {
                createIndeterminateTimeline();
            }
            if (pause) {
                indeterminateTransition.pause();
            } else {
                indeterminateTransition.play();
            }
        }
    }

    void updateAnimation() {
        ProgressIndicator control = getSkinnable();
        final boolean isTreeShowing = NodeHelper.isTreeShowing(control) &&
                                      control.getScene() != null;
        if (indeterminateTransition != null) {
            pauseTimeline(!isTreeShowing);
        } else if (isTreeShowing) {
            createIndeterminateTimeline();
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final CssMetaData<ProgressIndicator,Paint> PROGRESS_COLOR =
            new CssMetaData<>("-fx-progress-color",
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
                    return (StyleableProperty<Paint>)(WritableValue<Paint>)skin.progressColor;
                }
            };
    private static final CssMetaData<ProgressIndicator,Number> INDETERMINATE_SEGMENT_COUNT =
            new CssMetaData<>("-fx-indeterminate-segment-count",
                    SizeConverter.getInstance(), 8) {

                @Override public boolean isSettable(ProgressIndicator n) {
                    final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) n.getSkin();
                    return skin.indeterminateSegmentCount == null ||
                            !skin.indeterminateSegmentCount.isBound();
                }

                @Override public StyleableProperty<Number> getStyleableProperty(ProgressIndicator n) {
                    final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) n.getSkin();
                    return (StyleableProperty<Number>)skin.indeterminateSegmentCount;
                }
            };
    private static final CssMetaData<ProgressIndicator,Boolean> SPIN_ENABLED =
            new CssMetaData<>("-fx-spin-enabled", BooleanConverter.getInstance(), Boolean.FALSE) {

                @Override public boolean isSettable(ProgressIndicator node) {
                    final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) node.getSkin();
                    return skin.spinEnabled == null || !skin.spinEnabled.isBound();
                }

                @Override public StyleableProperty<Boolean> getStyleableProperty(ProgressIndicator node) {
                    final ProgressIndicatorSkin skin = (ProgressIndicatorSkin) node.getSkin();
                    return (StyleableProperty<Boolean>)skin.spinEnabled;
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
    static {
        final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(SkinBase.getClassCssMetaData());
        styleables.add(PROGRESS_COLOR);
        styleables.add(INDETERMINATE_SEGMENT_COUNT);
        styleables.add(SPIN_ENABLED);
        STYLEABLES = Collections.unmodifiableList(styleables);
    }

    /**
     * Returns the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @return the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }



    /* *************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/

    private final class DeterminateIndicator extends Region {
        private double textGap = 2.0F;

        // only update progress text on whole percentages
        private int intProgress;

        // only update pie arc to nearest degree
        private int degProgress;
        private Text text;
        private StackPane indicator;
        private StackPane progress;
        private StackPane tick;
        private Arc arcShape;
        private Circle indicatorCircle;
        private double doneTextWidth;
        private double doneTextHeight;

        public DeterminateIndicator(ProgressIndicator control, ProgressIndicatorSkin s, Paint fillOverride) {

            getStyleClass().add("determinate-indicator");

            intProgress = (int) Math.round(control.getProgress() * 100.0) ;
            degProgress = (int) (360 * control.getProgress());

            getChildren().clear();

            text = new Text((control.getProgress() >= 1) ? (DONE) : ("" + intProgress + "%"));
            text.setTextOrigin(VPos.TOP);
            text.getStyleClass().setAll("text", "percentage");

            registerChangeListener(text.fontProperty(), o -> {
                doneTextWidth = Utils.computeTextWidth(text.getFont(), DONE, 0);
                doneTextHeight = Utils.computeTextHeight(text.getFont(), DONE, 0, TextBoundsType.LOGICAL_VERTICAL_CENTER);
            });

            // The circular background for the progress pie piece
            indicator = new StackPane();
            indicator.setScaleShape(false);
            indicator.setCenterShape(false);
            indicator.getStyleClass().setAll("indicator");
            indicatorCircle = new Circle();
            indicator.setShape(indicatorCircle);

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
            updateProgress(control.getProgress());
        }

        private void unregisterListener() {
            unregisterChangeListeners(text.fontProperty());
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

        private void updateProgress(double progress) {
            intProgress = (int) Math.round(progress * 100.0) ;
            text.setText((progress >= 1) ? (DONE) : ("" + intProgress + "%"));

            degProgress = (int) (360 * progress);
            arcShape.setLength(-degProgress);
            requestLayout();
        }

        @Override protected void layoutChildren() {
            // Position and size the circular background
            final double left = control.snappedLeftInset();
            final double right = control.snappedRightInset();
            final double top = control.snappedTopInset();
            final double bottom = control.snappedBottomInset();

            /*
            ** use the min of width, or height, keep it a circle
            */
            final double areaW = control.getWidth() - left - right;
            final double areaH = control.getHeight() - top - bottom - textGap - doneTextHeight;
            final double radiusW = areaW / 2;
            final double radiusH = areaH / 2;
            final double radius = Math.floor(Math.min(radiusW, radiusH));
            final double centerX = snapPositionX(left + radiusW);
            final double centerY = snapPositionY(top + radius);

            // find radius that fits inside radius - insetsPadding
            final double iLeft = indicator.snappedLeftInset();
            final double iRight = indicator.snappedRightInset();
            final double iTop = indicator.snappedTopInset();
            final double iBottom = indicator.snappedBottomInset();
            final double progressRadius = snapSizeX(Math.min(
                    Math.min(radius - iLeft, radius - iRight),
                    Math.min(radius - iTop, radius - iBottom)));

            indicatorCircle.setRadius(radius);
            indicator.setLayoutX(centerX);
            indicator.setLayoutY(centerY);

            arcShape.setRadiusX(progressRadius);
            arcShape.setRadiusY(progressRadius);
            progress.setLayoutX(centerX);
            progress.setLayoutY(centerY);

            // find radius that fits inside progressRadius - progressInsets
            final double pLeft = progress.snappedLeftInset();
            final double pRight = progress.snappedRightInset();
            final double pTop = progress.snappedTopInset();
            final double pBottom = progress.snappedBottomInset();
            final double indicatorRadius = snapSizeX(Math.min(
                    Math.min(progressRadius - pLeft, progressRadius - pRight),
                    Math.min(progressRadius - pTop, progressRadius - pBottom)));

            // find size of spare box that fits inside indicator radius
            double squareBoxHalfWidth = Math.ceil(Math.sqrt((indicatorRadius * indicatorRadius) / 2));

            tick.setLayoutX(centerX - squareBoxHalfWidth);
            tick.setLayoutY(centerY - squareBoxHalfWidth);
            tick.resize(squareBoxHalfWidth + squareBoxHalfWidth, squareBoxHalfWidth + squareBoxHalfWidth);
            tick.setVisible(control.getProgress() >= 1);

            // if the % text can't fit anywhere in the bounds then don't display it
            double textWidth = text.getLayoutBounds().getWidth();
            double textHeight = text.getLayoutBounds().getHeight();
            if (control.getWidth() >= textWidth && control.getHeight() >= textHeight) {
                if (!text.isVisible()) text.setVisible(true);
                text.setLayoutY(snapPositionY(centerY + radius + textGap));
                text.setLayoutX(snapPositionX(centerX - (textWidth/2)));
            } else {
                if (text.isVisible()) text.setVisible(false);
            }
        }

        @Override protected double computePrefWidth(double height) {
            final double left = control.snappedLeftInset();
            final double right = control.snappedRightInset();
            final double iLeft = indicator.snappedLeftInset();
            final double iRight = indicator.snappedRightInset();
            final double iTop = indicator.snappedTopInset();
            final double iBottom = indicator.snappedBottomInset();
            final double indicatorMax = snapSizeX(Math.max(Math.max(iLeft, iRight), Math.max(iTop, iBottom)));
            final double pLeft = progress.snappedLeftInset();
            final double pRight = progress.snappedRightInset();
            final double pTop = progress.snappedTopInset();
            final double pBottom = progress.snappedBottomInset();
            final double progressMax = snapSizeX(Math.max(Math.max(pLeft, pRight), Math.max(pTop, pBottom)));
            final double tLeft = tick.snappedLeftInset();
            final double tRight = tick.snappedRightInset();
            final double indicatorWidth = indicatorMax + progressMax + tLeft + tRight + progressMax + indicatorMax;
            return left + Math.max(indicatorWidth, doneTextWidth) + right;
        }

        @Override protected double computePrefHeight(double width) {
            final double top = control.snappedTopInset();
            final double bottom = control.snappedBottomInset();
            final double iLeft = indicator.snappedLeftInset();
            final double iRight = indicator.snappedRightInset();
            final double iTop = indicator.snappedTopInset();
            final double iBottom = indicator.snappedBottomInset();
            final double indicatorMax = snapSizeY(Math.max(Math.max(iLeft, iRight), Math.max(iTop, iBottom)));
            final double pLeft = progress.snappedLeftInset();
            final double pRight = progress.snappedRightInset();
            final double pTop = progress.snappedTopInset();
            final double pBottom = progress.snappedBottomInset();
            final double progressMax = snapSizeY(Math.max(Math.max(pLeft, pRight), Math.max(pTop, pBottom)));
            final double tTop = tick.snappedTopInset();
            final double tBottom = tick.snappedBottomInset();
            final double indicatorHeight = indicatorMax + progressMax + tTop + tBottom + progressMax + indicatorMax;
            return top + indicatorHeight + textGap + doneTextHeight + bottom;
        }

        @Override protected double computeMaxWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMaxHeight(double width) {
            return computePrefHeight(width);
        }
    }


    private final class IndeterminateSpinner extends Region {
        private IndicatorPaths pathsG;
        private final List<Double> opacities = new ArrayList<>();
        private boolean spinEnabled = false;
        private Paint fillOverride = null;

        private IndeterminateSpinner(boolean spinEnabled, Paint fillOverride) {
            this.spinEnabled = spinEnabled;
            this.fillOverride = fillOverride;

            setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            getStyleClass().setAll("spinner");

            pathsG = new IndicatorPaths();
            getChildren().add(pathsG);
            rebuild();

            rebuildTimeline();

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
            if (spinEnabled) {
                if (indeterminateTransition == null) {
                    indeterminateTransition = new Timeline();
                    indeterminateTransition.setCycleCount(Timeline.INDEFINITE);
                    indeterminateTransition.setDelay(UNCLIPPED_DELAY);
                } else {
                    indeterminateTransition.stop();
                    ((Timeline)indeterminateTransition).getKeyFrames().clear();
                }
                final ObservableList<KeyFrame> keyFrames = FXCollections.<KeyFrame>observableArrayList();

                keyFrames.add(new KeyFrame(Duration.millis(1), new KeyValue(pathsG.rotateProperty(), 360)));
                keyFrames.add(new KeyFrame(Duration.millis(3900), new KeyValue(pathsG.rotateProperty(), 0)));

                for (int i = 100; i <= 3900; i += 100) {
                    keyFrames.add(new KeyFrame(Duration.millis(i), event -> shiftColors()));
                }

                ((Timeline)indeterminateTransition).getKeyFrames().setAll(keyFrames);

                if (NodeHelper.isTreeShowing(control)) {
                    indeterminateTransition.playFromStart();
                } else {
                    indeterminateTransition.jumpTo(Duration.ZERO);
                }
            } else {
                if (indeterminateTransition != null) {
                    indeterminateTransition.stop();
                    ((Timeline)indeterminateTransition).getKeyFrames().clear();
                    indeterminateTransition = null;
                }
            }
        }

        private class IndicatorPaths extends Pane {
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
            final double w = control.getWidth() - control.snappedLeftInset() - control.snappedRightInset();
            final double h = control.getHeight() - control.snappedTopInset() - control.snappedBottomInset();
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
            final int segments = indeterminateSegmentCount.get();
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
                opacities.add(Math.max(0.1, (1.0 - (step*i))));
            }
        }

        private void shiftColors() {
            if (opacities.size() <= 0) return;
            final int segments = indeterminateSegmentCount.get();
            Collections.rotate(opacities, -1);
            for (int i = 0; i < segments; i++) {
                pathsG.getChildren().get(i).setOpacity(opacities.get(i));
            }
        }
    }
}
