/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.javafx.scene.control.skin.Utils.boundedSize;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.ParentHelper;
import com.sun.javafx.scene.control.ListenerHelper;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.ScrollPaneBehavior;
import com.sun.javafx.scene.traversal.ParentTraversalEngine;
import com.sun.javafx.util.Utils;

/**
 * Default skin implementation for the {@link ScrollPane} control.
 *
 * @see ScrollPane
 * @since 9
 */
public class ScrollPaneSkin extends SkinBase<ScrollPane> {
    /* *************************************************************************
     *                                                                         *
     * Static fields                                                           *
     *                                                                         *
     **************************************************************************/

    private static final double DEFAULT_PREF_SIZE = 100.0;

    private static final double DEFAULT_MIN_SIZE = 36.0;

    private static final double DEFAULT_SB_BREADTH = 12.0;
    private static final double DEFAULT_EMBEDDED_SB_BREADTH = 8.0;

    private static final double PAN_THRESHOLD = 0.5;



    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    // state from the control

    private Node scrollNode;
    private final BehaviorBase<ScrollPane> behavior;

    private double nodeWidth;
    private double nodeHeight;
    private boolean nodeSizeInvalid = true;

    private double posX;
    private double posY;

    // working state

    private boolean hsbvis;
    private boolean vsbvis;
    private double hsbHeight;
    private double vsbWidth;

    // substructure

    private StackPane viewRect;
    private StackPane viewContent;
    private double contentWidth;
    private double contentHeight;
    private StackPane corner;
    ScrollBar hsb;
    ScrollBar vsb;

    double pressX;
    double pressY;
    double ohvalue;
    double ovvalue;
    private Cursor saveCursor =  null;
    private boolean dragDetected = false;
    private boolean touchDetected = false;
    private boolean mouseDown = false;

    Rectangle clipRect;

    Timeline sbTouchTimeline;
    KeyFrame sbTouchKF1;
    KeyFrame sbTouchKF2;
    Timeline contentsToViewTimeline;
    KeyFrame contentsToViewKF1;
    KeyFrame contentsToViewKF2;
    KeyFrame contentsToViewKF3;

    private boolean tempVisibility;



    /* *************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    private final InvalidationListener nodeListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            if (!nodeSizeInvalid) {
                final Bounds scrollNodeBounds = scrollNode.getLayoutBounds();
                final double scrollNodeWidth = scrollNodeBounds.getWidth();
                final double scrollNodeHeight = scrollNodeBounds.getHeight();

                /*
                ** if the new size causes scrollbar visibility to change, then need to relayout
                ** we also need to correct the thumb size when the scrollnode's size changes
                */
                if (vsbvis != determineVerticalSBVisible() || hsbvis != determineHorizontalSBVisible() ||
                        (scrollNodeWidth != 0.0  && nodeWidth != scrollNodeWidth) ||
                        (scrollNodeHeight != 0.0 && nodeHeight != scrollNodeHeight)) {
                    getSkinnable().requestLayout();
                } else {
                    /**
                     * we just need to update scrollbars based on new scrollNode size,
                     * but we don't do this while dragging, there's no need,
                     * and it jumps, as dragging updates the scrollbar too.
                     */
                    if (!dragDetected) {
                        updateVerticalSB();
                        updateHorizontalSB();
                    }
                }
            }
        }
    };

    private final WeakInvalidationListener weakNodeListener = new WeakInvalidationListener(nodeListener);

    /*
    ** The content of the ScrollPane has just changed bounds, check scrollBar positions.
    */
    private final ChangeListener<Bounds> boundsChangeListener = new ChangeListener<>() {
        @Override public void changed(ObservableValue<? extends Bounds> observable, Bounds oldBounds, Bounds newBounds) {

            /*
            ** For a height change then we want to reduce
            ** viewport vertical jumping as much as possible.
            ** We set a new vsb value to try to keep the same
            ** content position at the top of the viewport
            */
            double oldHeight = oldBounds.getHeight();
            double newHeight = newBounds.getHeight();
            if (oldHeight > 0 && oldHeight != newHeight) {
                double oldPositionY = (snapPositionY(snappedTopInset() - posY / (vsb.getMax() - vsb.getMin()) * (oldHeight - contentHeight)));
                double newPositionY = (snapPositionY(snappedTopInset() - posY / (vsb.getMax() - vsb.getMin()) * (newHeight - contentHeight)));

                double newValueY = (oldPositionY/newPositionY)*vsb.getValue();
                if (newValueY < 0.0) {
                    vsb.setValue(0.0);
                }
                else if (newValueY < 1.0) {
                    vsb.setValue(newValueY);
                }
                else if (newValueY > 1.0) {
                    vsb.setValue(1.0);
                }
            }

            /*
            ** For a width change then we want to reduce
            ** viewport horizontal jumping as much as possible.
            ** We set a new hsb value to try to keep the same
            ** content position to the left of the viewport
            */
            double oldWidth = oldBounds.getWidth();
            double newWidth = newBounds.getWidth();
            if (oldWidth > 0 && oldWidth != newWidth) {
                double oldPositionX = (snapPositionX(snappedLeftInset() - posX / (hsb.getMax() - hsb.getMin()) * (oldWidth - contentWidth)));
                double newPositionX = (snapPositionX(snappedLeftInset() - posX / (hsb.getMax() - hsb.getMin()) * (newWidth - contentWidth)));

                double newValueX = (oldPositionX/newPositionX)*hsb.getValue();
                if (newValueX < 0.0) {
                    hsb.setValue(0.0);
                }
                else if (newValueX < 1.0) {
                    hsb.setValue(newValueX);
                }
                else if (newValueX > 1.0) {
                    hsb.setValue(1.0);
                }
            }
        }
    };

    private final WeakChangeListener<Bounds> weakBoundsChangeListener = new WeakChangeListener(boundsChangeListener);

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ScrollPaneSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ScrollPaneSkin(final ScrollPane control) {
        super(control);

        // install default input map for the ScrollPane control
        behavior = new ScrollPaneBehavior(control);

        initialize();

        // Register listeners
        ListenerHelper lh = ListenerHelper.get(this);

        lh.addChangeListener(control.contentProperty(), (ev) -> {
            if (scrollNode != getSkinnable().getContent()) {
                if (scrollNode != null) {
                    scrollNode.layoutBoundsProperty().removeListener(weakNodeListener);
                    scrollNode.layoutBoundsProperty().removeListener(weakBoundsChangeListener);
                    viewContent.getChildren().remove(scrollNode);
                }
                scrollNode = getSkinnable().getContent();
                if (scrollNode != null) {
                    nodeWidth = snapSizeX(scrollNode.getLayoutBounds().getWidth());
                    nodeHeight = snapSizeY(scrollNode.getLayoutBounds().getHeight());
                    viewContent.getChildren().setAll(scrollNode);
                    scrollNode.layoutBoundsProperty().addListener(weakNodeListener);
                    scrollNode.layoutBoundsProperty().addListener(weakBoundsChangeListener);
                }
            }
            getSkinnable().requestLayout();
        });

        lh.addChangeListener(
            () -> {
                getSkinnable().requestLayout();
                viewRect.requestLayout();
            },
            control.fitToWidthProperty(),
            control.fitToHeightProperty()
        );

        lh.addChangeListener(control.hvalueProperty(), e -> hsb.setValue(getSkinnable().getHvalue()));
        lh.addChangeListener(control.hmaxProperty(), e -> hsb.setMax(getSkinnable().getHmax()));
        lh.addChangeListener(control.hminProperty(), e -> hsb.setMin(getSkinnable().getHmin()));
        lh.addChangeListener(control.vvalueProperty(), e -> vsb.setValue(getSkinnable().getVvalue()));
        lh.addChangeListener(control.vmaxProperty(), e -> vsb.setMax(getSkinnable().getVmax()));
        lh.addChangeListener(control.vminProperty(), e -> vsb.setMin(getSkinnable().getVmin()));

        lh.addChangeListener(
            () -> {
                // change affects pref size, so requestLayout on control
                getSkinnable().requestLayout();
            },
            control.hbarPolicyProperty(),
            control.vbarPolicyProperty(),
            control.prefViewportWidthProperty(),
            control.prefViewportHeightProperty(),
            control.minViewportWidthProperty(),
            control.minViewportHeightProperty()
        );
    }



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    private DoubleProperty contentPosX;
    private final void setContentPosX(double value) { contentPosXProperty().set(value); }
    private final double getContentPosX() { return contentPosX == null ? 0.0 : contentPosX.get(); }
    private final DoubleProperty contentPosXProperty() {
        if (contentPosX == null) {
            contentPosX = new DoublePropertyBase() {
                @Override protected void invalidated() {
                    hsb.setValue(getContentPosX());
                    getSkinnable().requestLayout();
                }

                @Override
                public Object getBean() {
                    return ScrollPaneSkin.this;
                }

                @Override
                public String getName() {
                    return "contentPosX";
                }
            };
        }
        return contentPosX;
    }

    private DoubleProperty contentPosY;
    private final void setContentPosY(double value) { contentPosYProperty().set(value); }
    private final double getContentPosY() { return contentPosY == null ? 0.0 : contentPosY.get(); }
    private final DoubleProperty contentPosYProperty() {
        if (contentPosY == null) {
            contentPosY = new DoublePropertyBase() {
                @Override protected void invalidated() {
                    vsb.setValue(getContentPosY());
                    getSkinnable().requestLayout();
                }

                @Override
                public Object getBean() {
                    return ScrollPaneSkin.this;
                }

                @Override
                public String getName() {
                    return "contentPosY";
                }
            };
        }
        return contentPosY;
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        if (behavior != null) {
            behavior.dispose();
        }

        super.dispose();
    }

    /**
     * Returns the horizontal {@link ScrollBar} used in this ScrollPaneSkin
     * instance.
     * @return the horizontal ScrollBar used in this ScrollPaneSkin instance
     */
    public final ScrollBar getHorizontalScrollBar() {
        return hsb;
    }

    /**
     * Returns the vertical {@link ScrollBar} used in this ScrollPaneSkin
     * instance.
     * @return the vertical ScrollBar used in this ScrollPaneSkin instance
     */
    public final ScrollBar getVerticalScrollBar() {
        return vsb;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        final ScrollPane sp = getSkinnable();

        double vsbWidth = computeVsbSizeHint(sp);
        double minWidth = vsbWidth + snappedLeftInset() + snappedRightInset();

        if (sp.getPrefViewportWidth() > 0) {
            return (sp.getPrefViewportWidth() + minWidth);
        }
        else if (sp.getContent() != null) {
            return (sp.getContent().prefWidth(height) + minWidth);
        }
        else {
            return Math.max(minWidth, DEFAULT_PREF_SIZE);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        final ScrollPane sp = getSkinnable();

        double hsbHeight = computeHsbSizeHint(sp);
        double minHeight = hsbHeight + snappedTopInset() + snappedBottomInset();

        if (sp.getPrefViewportHeight() > 0) {
            return (sp.getPrefViewportHeight() + minHeight);
        }
        else if (sp.getContent() != null) {
            return (sp.getContent().prefHeight(width) + minHeight);
        }
        else {
            return Math.max(minHeight, DEFAULT_PREF_SIZE);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        final ScrollPane sp = getSkinnable();

        double vsbWidth = computeVsbSizeHint(sp);
        double minWidth = vsbWidth + snappedLeftInset() + snappedRightInset();

        if (sp.getMinViewportWidth() > 0) {
            return (sp.getMinViewportWidth() + minWidth);
        } else {
            double w = corner.minWidth(-1);
            return (w > 0) ? (3 * w) : (DEFAULT_MIN_SIZE);
        }

    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        final ScrollPane sp = getSkinnable();

        double hsbHeight = computeHsbSizeHint(sp);
        double minHeight = hsbHeight + snappedTopInset() + snappedBottomInset();

        if (sp.getMinViewportHeight() > 0) {
            return (sp.getMinViewportHeight() + minHeight);
        } else {
            double h = corner.minHeight(-1);
            return (h > 0) ? (3 * h) : (DEFAULT_MIN_SIZE);
        }
    }

    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        final ScrollPane control = getSkinnable();
        final Insets padding = control.getPadding();
        final double rightPadding = snapSizeX(padding.getRight());
        final double leftPadding = snapSizeX(padding.getLeft());
        final double topPadding = snapSizeY(padding.getTop());
        final double bottomPadding = snapSizeY(padding.getBottom());

        vsb.setMin(control.getVmin());
        vsb.setMax(control.getVmax());

        //should only do this on css setup
        hsb.setMin(control.getHmin());
        hsb.setMax(control.getHmax());

        contentWidth = w;
        contentHeight = h;

        /*
        ** we want the scrollbars to go right to the border
        */
        double hsbWidth = 0;
        double vsbHeight = 0;

        computeScrollNodeSize(contentWidth, contentHeight);
        computeScrollBarSize();

        for (int i = 0; i < 2; ++i) {
            vsbvis = determineVerticalSBVisible();
            hsbvis = determineHorizontalSBVisible();

            if (vsbvis && !Properties.IS_TOUCH_SUPPORTED) {
                contentWidth = w - vsbWidth;
            }
            hsbWidth = w + leftPadding + rightPadding - (vsbvis ? vsbWidth : 0);
            if (hsbvis && !Properties.IS_TOUCH_SUPPORTED) {
                contentHeight = h - hsbHeight;
            }
            vsbHeight = h + topPadding + bottomPadding - (hsbvis ? hsbHeight : 0);
        }


        if (scrollNode != null && scrollNode.isResizable()) {
            // maybe adjust size now that scrollbars may take up space
            if (vsbvis && hsbvis) {
                // adjust just once to accommodate
                computeScrollNodeSize(contentWidth, contentHeight);

            } else if (hsbvis && !vsbvis) {
                computeScrollNodeSize(contentWidth, contentHeight);
                vsbvis = determineVerticalSBVisible();
                if (vsbvis) {
                    // now both are visible
                    contentWidth -= vsbWidth;
                    hsbWidth -= vsbWidth;
                    computeScrollNodeSize(contentWidth, contentHeight);
                }
            } else if (vsbvis && !hsbvis) {
                computeScrollNodeSize(contentWidth, contentHeight);
                hsbvis = determineHorizontalSBVisible();
                if (hsbvis) {
                    // now both are visible
                    contentHeight -= hsbHeight;
                    vsbHeight -= hsbHeight;
                    computeScrollNodeSize(contentWidth, contentHeight);
                }
            }
        }

        // figure out the content area that is to be filled
        double cx = snappedLeftInset() - leftPadding;
        double cy = snappedTopInset() - topPadding;

        vsb.setVisible(vsbvis);
        if (vsbvis) {
            /*
            ** round up position of ScrollBar, round down it's size.
            **
            ** Positioning the ScrollBar
            **  The Padding should go between the content and the edge,
            **  otherwise changes in padding move the ScrollBar, and could
            **  in extreme cases size the ScrollBar to become unusable.
            **  The -1, +1 plus one bit :
            **   If padding in => 1 then we allow one pixel to appear as the
            **   outside border of the Scrollbar, and the rest on the inside.
            **   If padding is < 1 then we just stick to the edge.
            */
            vsb.resizeRelocate(snappedLeftInset() + w - vsbWidth + (rightPadding < 1 ? 0 : rightPadding - 1) ,
                    cy, vsbWidth, vsbHeight);
        }
        updateVerticalSB();

        hsb.setVisible(hsbvis);
        if (hsbvis) {
            /*
            ** round up position of ScrollBar, round down it's size.
            **
            ** Positioning the ScrollBar
            **  The Padding should go between the content and the edge,
            **  otherwise changes in padding move the ScrollBar, and could
            **  in extreme cases size the ScrollBar to become unusable.
            **  The -1, +1 plus one bit :
            **   If padding in => 1 then we allow one pixel to appear as the
            **   outside border of the Scrollbar, and the rest on the inside.
            **   If padding is < 1 then we just stick to the edge.
            */
            hsb.resizeRelocate(cx, snappedTopInset() + h - hsbHeight + (bottomPadding < 1 ? 0 : bottomPadding - 1),
                    hsbWidth, hsbHeight);
        }
        updateHorizontalSB();

        viewRect.resizeRelocate(snappedLeftInset(), snappedTopInset(), snapSizeX(contentWidth), snapSizeY(contentHeight));
        resetClip();

        if (vsbvis && hsbvis) {
            corner.setVisible(true);
            double cornerWidth = vsbWidth;
            double cornerHeight = hsbHeight;
            corner.resizeRelocate(snapPositionX(vsb.getLayoutX()), snapPositionY(hsb.getLayoutY()), snapSizeX(cornerWidth), snapSizeY(cornerHeight));
        } else {
            corner.setVisible(false);
        }
        control.setViewportBounds(new BoundingBox(snapPositionX(viewContent.getLayoutX()), snapPositionY(viewContent.getLayoutY()), snapSizeX(contentWidth), snapSizeY(contentHeight)));
    }

    /** {@inheritDoc} */
    @Override protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case VERTICAL_SCROLLBAR: return vsb;
            case HORIZONTAL_SCROLLBAR: return hsb;
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void initialize() {
        // requestLayout calls below should not trigger requestLayout above ScrollPane
//        setManaged(false);

        ScrollPane control = getSkinnable();
        scrollNode = control.getContent();

        ParentTraversalEngine traversalEngine = new ParentTraversalEngine(getSkinnable());
        traversalEngine.addTraverseListener((node, bounds) -> {
            // auto-scroll so node is within (0,0),(contentWidth,contentHeight)
            scrollBoundsIntoView(bounds);
        });
        ParentHelper.setTraversalEngine(getSkinnable(), traversalEngine);

        if (scrollNode != null) {
            scrollNode.layoutBoundsProperty().addListener(weakNodeListener);
            scrollNode.layoutBoundsProperty().addListener(weakBoundsChangeListener);
        }

        viewRect = new StackPane() {
            @Override protected void layoutChildren() {
                viewContent.resize(getWidth(), getHeight());
            }
        };
        // prevent requestLayout requests from within scrollNode from percolating up
        viewRect.setManaged(false);
        viewRect.setCache(true);
        viewRect.getStyleClass().add("viewport");

        clipRect = new Rectangle();
        viewRect.setClip(clipRect);

        hsb = new ScrollBar();

        vsb = new ScrollBar();
        vsb.setOrientation(Orientation.VERTICAL);

        EventHandler<MouseEvent> barHandler = ev -> {
            if (getSkinnable().isFocusTraversable()) {
                getSkinnable().requestFocus();
            }
        };

        ListenerHelper lh = ListenerHelper.get(this);

        lh.addEventFilter(hsb, MouseEvent.MOUSE_PRESSED, barHandler);
        lh.addEventFilter(vsb, MouseEvent.MOUSE_PRESSED, barHandler);

        corner = new StackPane();
        corner.getStyleClass().setAll("corner");

        viewContent = new StackPane() {
            @Override public void requestLayout() {
                // if scrollNode requested layout, will want to recompute
                nodeSizeInvalid = true;

                super.requestLayout(); // add as layout root for next layout pass

                // Need to layout the ScrollPane as well in case scrollbars
                // appeared or disappeared.
                ScrollPaneSkin.this.getSkinnable().requestLayout();
            }
            @Override protected void layoutChildren() {
                if (nodeSizeInvalid) {
                    computeScrollNodeSize(getWidth(),getHeight());
                }
                if (scrollNode != null && scrollNode.isResizable()) {
                    scrollNode.resize(snapSizeX(nodeWidth), snapSizeY(nodeHeight));
                    if (vsbvis != determineVerticalSBVisible() || hsbvis != determineHorizontalSBVisible()) {
                        getSkinnable().requestLayout();
                    }
                }
                if (scrollNode != null) {
                    scrollNode.relocate(0,0);
                }
            }
        };
        viewRect.getChildren().add(viewContent);

        if (scrollNode != null) {
            viewContent.getChildren().add(scrollNode);
            viewRect.nodeOrientationProperty().bind(scrollNode.nodeOrientationProperty());
        }

        getChildren().clear();
        getChildren().addAll(viewRect, vsb, hsb, corner);

        // listeners, and assorted housekeeping

        lh.addInvalidationListener(vsb.valueProperty(), (valueModel) -> {
            if (!Properties.IS_TOUCH_SUPPORTED) {
                posY = Utils.clamp(getSkinnable().getVmin(), vsb.getValue(), getSkinnable().getVmax());
            }
            else {
                posY = vsb.getValue();
            }
            updatePosY();
        });

        lh.addInvalidationListener(hsb.valueProperty(), (valueModel) -> {
            if (!Properties.IS_TOUCH_SUPPORTED) {
                posX = Utils.clamp(getSkinnable().getHmin(), hsb.getValue(), getSkinnable().getHmax());
            }
            else {
                posX = hsb.getValue();
            }
            updatePosX();
        });

        viewRect.setOnMousePressed(e -> {
            mouseDown = true;
            if (Properties.IS_TOUCH_SUPPORTED) {
                startSBReleasedAnimation();
            }
            pressX = e.getX();
            pressY = e.getY();
            ohvalue = hsb.getValue();
            ovvalue = vsb.getValue();
        });

        viewRect.setOnDragDetected(e -> {
             if (Properties.IS_TOUCH_SUPPORTED) {
                 startSBReleasedAnimation();
             }
            if (getSkinnable().isPannable()) {
              dragDetected = true;
              if (saveCursor == null) {
                  saveCursor = getSkinnable().getCursor();
                  if (saveCursor == null) {
                      saveCursor = Cursor.DEFAULT;
                  }
                  getSkinnable().setCursor(Cursor.MOVE);
                  getSkinnable().requestLayout();
              }
            }
        });

        viewRect.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
             mouseDown = false;
             if (dragDetected == true) {
                 if (saveCursor != null) {
                     getSkinnable().setCursor(saveCursor);
                     saveCursor = null;
                     getSkinnable().requestLayout();
                 }
                 dragDetected = false;
             }

             /*
             ** if the contents need repositioning, and there's is no
             ** touch event in progress, then start the repositioning.
             */
             if ((posY > getSkinnable().getVmax() || posY < getSkinnable().getVmin() ||
                 posX > getSkinnable().getHmax() || posX < getSkinnable().getHmin()) && !touchDetected) {
                 startContentsToViewport();
             }
        });

        viewRect.setOnMouseDragged(e -> {
             if (Properties.IS_TOUCH_SUPPORTED) {
                 startSBReleasedAnimation();
             }
            /*
            ** for mobile-touch we allow drag, even if not pannagle
            */
            if (getSkinnable().isPannable() || Properties.IS_TOUCH_SUPPORTED) {
                double deltaX = pressX - e.getX();
                double deltaY = pressY - e.getY();
                /*
                ** we only drag if not all of the content is visible.
                */
                if (hsb.getVisibleAmount() > 0.0 && hsb.getVisibleAmount() < hsb.getMax()) {
                    if (Math.abs(deltaX) > PAN_THRESHOLD) {
                        if (isReverseNodeOrientation()) {
                            deltaX = -deltaX;
                        }
                        double newHVal = (ohvalue + deltaX / (nodeWidth - viewRect.getWidth()) * (hsb.getMax() - hsb.getMin()));
                        if (!Properties.IS_TOUCH_SUPPORTED) {
                            if (newHVal > hsb.getMax()) {
                                newHVal = hsb.getMax();
                            }
                            else if (newHVal < hsb.getMin()) {
                                newHVal = hsb.getMin();
                            }
                            hsb.setValue(newHVal);
                        }
                        else {
                            hsb.setValue(newHVal);
                        }
                    }
                }
                /*
                ** we only drag if not all of the content is visible.
                */
                if (vsb.getVisibleAmount() > 0.0 && vsb.getVisibleAmount() < vsb.getMax()) {
                    if (Math.abs(deltaY) > PAN_THRESHOLD) {
                        double newVVal = (ovvalue + deltaY / (nodeHeight - viewRect.getHeight()) * (vsb.getMax() - vsb.getMin()));
                        if (!Properties.IS_TOUCH_SUPPORTED) {
                            if (newVVal > vsb.getMax()) {
                                newVVal = vsb.getMax();
                            }
                            else if (newVVal < vsb.getMin()) {
                                newVVal = vsb.getMin();
                            }
                            vsb.setValue(newVVal);
                        }
                        else {
                            vsb.setValue(newVVal);
                        }
                    }
                }
            }
            /*
            ** we need to consume drag events, as we don't want
            ** the scrollpane itself to be dragged on every mouse click
            */
            e.consume();
        });

        /*
        ** don't allow the ScrollBar to handle the ScrollEvent,
        ** In a ScrollPane a vertical scroll should scroll on the vertical only,
        ** whereas in a horizontal ScrollBar it can scroll horizontally.
        */
        // block the event from being passed down to children
        final EventDispatcher blockEventDispatcher = (event, tail) -> event;
        // block ScrollEvent from being passed down to scrollbar's skin
        final EventDispatcher oldHsbEventDispatcher = hsb.getEventDispatcher();
        hsb.setEventDispatcher((event, tail) -> {
            if (event.getEventType() == ScrollEvent.SCROLL &&
                    !((ScrollEvent)event).isDirect()) {
                tail = tail.prepend(blockEventDispatcher);
                tail = tail.prepend(oldHsbEventDispatcher);
                return tail.dispatchEvent(event);
            }
            return oldHsbEventDispatcher.dispatchEvent(event, tail);
        });
        // block ScrollEvent from being passed down to scrollbar's skin
        final EventDispatcher oldVsbEventDispatcher = vsb.getEventDispatcher();
        vsb.setEventDispatcher((event, tail) -> {
            if (event.getEventType() == ScrollEvent.SCROLL &&
                    !((ScrollEvent)event).isDirect()) {
                tail = tail.prepend(blockEventDispatcher);
                tail = tail.prepend(oldVsbEventDispatcher);
                return tail.dispatchEvent(event);
            }
            return oldVsbEventDispatcher.dispatchEvent(event, tail);
        });

        /*
         * listen for ScrollEvents over the whole of the ScrollPane
         * area, the above dispatcher having removed the ScrollBars
         * scroll event handling.
         *
         * Note that we use viewRect here, rather than setting the eventHandler
         * on the ScrollPane itself. This is for RT-31582, and effectively
         * allows for us to prioritise handling (and consuming) the event
         * internally, before it is made available to users listening to events
         * on the control. This is consistent with the VirtualFlow-based controls.
         */
        viewRect.addEventHandler(ScrollEvent.SCROLL, event -> {
            if (Properties.IS_TOUCH_SUPPORTED) {
                startSBReleasedAnimation();
            }
            /*
            ** if we're completely visible then do nothing....
            ** we only consume an event that we've used.
            */
            if (vsb.getVisibleAmount() < vsb.getMax()) {
                double vRange = getSkinnable().getVmax()-getSkinnable().getVmin();
                double hDelta = nodeHeight - contentHeight;
                double vPixelValue = hDelta > 0.0 ? vRange / hDelta : 0.0;
                double newValue = vsb.getValue()+(-event.getDeltaY())*vPixelValue;
                if (!Properties.IS_TOUCH_SUPPORTED) {
                    if ((event.getDeltaY() > 0.0 && vsb.getValue() > vsb.getMin()) ||
                        (event.getDeltaY() < 0.0 && vsb.getValue() < vsb.getMax())) {
                        vsb.setValue(newValue);
                        event.consume();
                    }
                }
                else {
                    /*
                    ** if there is a repositioning in progress then we only
                    ** set the value for 'real' events
                    */
                    if (!(event.isInertia()) || (event.isInertia()) && (contentsToViewTimeline == null || contentsToViewTimeline.getStatus() == Status.STOPPED)) {
                        vsb.setValue(newValue);
                        if ((newValue > vsb.getMax() || newValue < vsb.getMin()) && (!mouseDown && !touchDetected)) {
                            startContentsToViewport();
                        }
                        event.consume();
                    }
                }
            }

            if (hsb.getVisibleAmount() < hsb.getMax()) {
                double hRange = getSkinnable().getHmax()-getSkinnable().getHmin();
                double wDelta = nodeWidth - contentWidth;
                double hPixelValue = wDelta > 0.0 ? hRange / wDelta : 0.0;
                double newValue = hsb.getValue()+(-event.getDeltaX())*hPixelValue;
                if (!Properties.IS_TOUCH_SUPPORTED) {
                    if ((event.getDeltaX() > 0.0 && hsb.getValue() > hsb.getMin()) ||
                        (event.getDeltaX() < 0.0 && hsb.getValue() < hsb.getMax())) {
                        hsb.setValue(newValue);
                        event.consume();
                    }
                }
                else {
                    /*
                    ** if there is a repositioning in progress then we only
                    ** set the value for 'real' events
                    */
                    if (!(event.isInertia()) || (event.isInertia()) && (contentsToViewTimeline == null || contentsToViewTimeline.getStatus() == Status.STOPPED)) {
                        hsb.setValue(newValue);

                        if ((newValue > hsb.getMax() || newValue < hsb.getMin()) && (!mouseDown && !touchDetected)) {
                            startContentsToViewport();
                        }
                        event.consume();
                    }
                }
            }
        });

        /*
        ** there are certain animations that need to know if the touch is
        ** happening.....
        */
        lh.addEventHandler(getSkinnable(), TouchEvent.TOUCH_PRESSED, e -> {
            touchDetected = true;
            startSBReleasedAnimation();
            e.consume();
        });

        lh.addEventHandler(getSkinnable(), TouchEvent.TOUCH_RELEASED, e -> {
            touchDetected = false;
            e.consume();
        });

        // ScrollPanes do not block all MouseEvents by default, unlike most other UI Controls.
        consumeMouseEvents(false);

        // update skin initial state to match control (see RT-35554)
        hsb.setValue(control.getHvalue());
        vsb.setValue(control.getVvalue());
    }

    void scrollBoundsIntoView(Bounds b) {
        double dx = 0.0;
        double dy = 0.0;
        if (b.getMaxX() > contentWidth) {
            dx = b.getMinX() - snappedLeftInset();
        }
        if (b.getMinX() < snappedLeftInset()) {
            dx = b.getMaxX() - contentWidth - snappedLeftInset();
        }
        if (b.getMaxY() > snappedTopInset() + contentHeight) {
            dy = b.getMinY() - snappedTopInset();
        }
        if (b.getMinY() < snappedTopInset()) {
            dy = b.getMaxY() - contentHeight - snappedTopInset();
        }
        // We want to move contentPanel's layoutX,Y by (dx,dy).
        // But to do this we have to set the scrollbars' values appropriately.

        if (dx != 0) {
            double wd = nodeWidth - contentWidth;
            double sdx = wd > 0.0 ? dx * (hsb.getMax() - hsb.getMin()) / wd : 0;
            // Adjust back for some amount so that the Node border is not too close to view border
            sdx += -1 * Math.signum(sdx) * hsb.getUnitIncrement() / 5; // This accounts to 2% of view width
            hsb.setValue(hsb.getValue() + sdx);
            getSkinnable().requestLayout();
        }
        if (dy != 0) {
            double hd = nodeHeight - contentHeight;
            double sdy = hd > 0.0 ? dy * (vsb.getMax() - vsb.getMin()) / hd : 0.0;
            // Adjust back for some amount so that the Node border is not too close to view border
            sdy += -1 * Math.signum(sdy) * vsb.getUnitIncrement() / 5; // This accounts to 2% of view height
            vsb.setValue(vsb.getValue() + sdy);
            getSkinnable().requestLayout();
        }

    }

    /**
     * Computes the size that should be reserved for horizontal scrollbar in size hints (min/pref height)
     */
    private double computeHsbSizeHint(ScrollPane sp) {
        return ((sp.getHbarPolicy() == ScrollBarPolicy.ALWAYS) ||
                (sp.getHbarPolicy() == ScrollBarPolicy.AS_NEEDED && (sp.getPrefViewportHeight() > 0 || sp.getMinViewportHeight() > 0)))
                ? hsb.prefHeight(ScrollBar.USE_COMPUTED_SIZE)
                : 0;
    }

    /**
     * Computes the size that should be reserved for vertical scrollbar in size hints (min/pref width)
     */
    private double computeVsbSizeHint(ScrollPane sp) {
        return ((sp.getVbarPolicy() == ScrollBarPolicy.ALWAYS) ||
                (sp.getVbarPolicy() == ScrollBarPolicy.AS_NEEDED && (sp.getPrefViewportWidth() > 0
                        || sp.getMinViewportWidth() > 0)))
                ? vsb.prefWidth(ScrollBar.USE_COMPUTED_SIZE)
                : 0;
    }

    private void computeScrollNodeSize(double contentWidth, double contentHeight) {
        if (scrollNode != null) {
            if (scrollNode.isResizable()) {
                ScrollPane control = getSkinnable();
                Orientation bias = scrollNode.getContentBias();
                if (bias == null) {
                    nodeWidth = snapSizeX(boundedSize(control.isFitToWidth()? contentWidth : scrollNode.prefWidth(-1),
                                                         scrollNode.minWidth(-1),scrollNode.maxWidth(-1)));
                    nodeHeight = snapSizeY(boundedSize(control.isFitToHeight()? contentHeight : scrollNode.prefHeight(-1),
                                                          scrollNode.minHeight(-1), scrollNode.maxHeight(-1)));

                } else if (bias == Orientation.HORIZONTAL) {
                    nodeWidth = snapSizeX(boundedSize(control.isFitToWidth()? contentWidth : scrollNode.prefWidth(-1),
                                                         scrollNode.minWidth(-1),scrollNode.maxWidth(-1)));
                    nodeHeight = snapSizeY(boundedSize(control.isFitToHeight()? contentHeight : scrollNode.prefHeight(nodeWidth),
                                                          scrollNode.minHeight(nodeWidth),scrollNode.maxHeight(nodeWidth)));

                } else { // bias == VERTICAL
                    nodeHeight = snapSizeY(boundedSize(control.isFitToHeight()? contentHeight : scrollNode.prefHeight(-1),
                                                          scrollNode.minHeight(-1), scrollNode.maxHeight(-1)));
                    nodeWidth = snapSizeX(boundedSize(control.isFitToWidth()? contentWidth : scrollNode.prefWidth(nodeHeight),
                                                         scrollNode.minWidth(nodeHeight),scrollNode.maxWidth(nodeHeight)));
                }

            } else {
                nodeWidth = snapSizeX(scrollNode.getLayoutBounds().getWidth());
                nodeHeight = snapSizeY(scrollNode.getLayoutBounds().getHeight());
            }
            nodeSizeInvalid = false;
        }
    }

    private boolean isReverseNodeOrientation() {
        return (scrollNode != null &&
                getSkinnable().getEffectiveNodeOrientation() !=
                            scrollNode.getEffectiveNodeOrientation());
    }

    private boolean determineHorizontalSBVisible() {
        final ScrollPane sp = getSkinnable();

        if (Properties.IS_TOUCH_SUPPORTED) {
            return (tempVisibility && (nodeWidth > contentWidth));
        }
        else {
            // RT-17395: ScrollBarPolicy might be null. If so, treat it as "AS_NEEDED", which is the default
            ScrollBarPolicy hbarPolicy = sp.getHbarPolicy();
            return (ScrollBarPolicy.NEVER == hbarPolicy) ? false :
                   ((ScrollBarPolicy.ALWAYS == hbarPolicy) ? true :
                   ((sp.isFitToWidth() && scrollNode != null ? scrollNode.isResizable() : false) ?
                   (nodeWidth > contentWidth && scrollNode.minWidth(-1) > contentWidth) : (nodeWidth > contentWidth)));
        }
    }

    private boolean determineVerticalSBVisible() {
        final ScrollPane sp = getSkinnable();

        if (Properties.IS_TOUCH_SUPPORTED) {
            return (tempVisibility && (nodeHeight > contentHeight));
        }
        else {
            // RT-17395: ScrollBarPolicy might be null. If so, treat it as "AS_NEEDED", which is the default
            ScrollBarPolicy vbarPolicy = sp.getVbarPolicy();
            return (ScrollBarPolicy.NEVER == vbarPolicy) ? false :
                   ((ScrollBarPolicy.ALWAYS == vbarPolicy) ? true :
                   ((sp.isFitToHeight() && scrollNode != null ? scrollNode.isResizable() : false) ?
                   (nodeHeight > contentHeight && scrollNode.minHeight(-1) > contentHeight) : (nodeHeight > contentHeight)));
        }
    }

    private void computeScrollBarSize() {
        vsbWidth = snapSizeX(vsb.prefWidth(-1));
        if (vsbWidth == 0) {
            //            println("*** WARNING ScrollPaneSkin: can't get scroll bar width, using {DEFAULT_SB_BREADTH}");
            if (Properties.IS_TOUCH_SUPPORTED) {
                vsbWidth = DEFAULT_EMBEDDED_SB_BREADTH;
            }
            else {
                vsbWidth = DEFAULT_SB_BREADTH;
            }
        }
        hsbHeight = snapSizeY(hsb.prefHeight(-1));
        if (hsbHeight == 0) {
            //            println("*** WARNING ScrollPaneSkin: can't get scroll bar height, using {DEFAULT_SB_BREADTH}");
            if (Properties.IS_TOUCH_SUPPORTED) {
                hsbHeight = DEFAULT_EMBEDDED_SB_BREADTH;
            }
            else {
                hsbHeight = DEFAULT_SB_BREADTH;
            }
        }
    }

    private void updateHorizontalSB() {
        double contentRatio = nodeWidth * (hsb.getMax() - hsb.getMin());
        if (contentRatio > 0.0) {
            hsb.setVisibleAmount(contentWidth / contentRatio);
            hsb.setBlockIncrement(0.9 * hsb.getVisibleAmount());
            hsb.setUnitIncrement(0.1 * hsb.getVisibleAmount());
        }
        else {
            hsb.setVisibleAmount(0.0);
            hsb.setBlockIncrement(0.0);
            hsb.setUnitIncrement(0.0);
        }

        if (hsb.isVisible()) {
            updatePosX();
        } else {
            if (nodeWidth > contentWidth) {
                updatePosX();
            } else {
                viewContent.setLayoutX(0);
            }
        }
    }

    private void updateVerticalSB() {
        double contentRatio = nodeHeight * (vsb.getMax() - vsb.getMin());
        if (contentRatio > 0.0) {
            vsb.setVisibleAmount(contentHeight / contentRatio);
            vsb.setBlockIncrement(0.9 * vsb.getVisibleAmount());
            vsb.setUnitIncrement(0.1 * vsb.getVisibleAmount());
        }
        else {
            vsb.setVisibleAmount(0.0);
            vsb.setBlockIncrement(0.0);
            vsb.setUnitIncrement(0.0);
        }

        if (vsb.isVisible()) {
            updatePosY();
        } else {
            if (nodeHeight > contentHeight) {
                updatePosY();
            } else {
                viewContent.setLayoutY(0);
            }
        }
    }

    private double updatePosX() {
        final ScrollPane sp = getSkinnable();
        double x = isReverseNodeOrientation() ? (hsb.getMax() - (posX - hsb.getMin())) : posX;
        double hsbRange = hsb.getMax() - hsb.getMin();
        double minX = hsbRange > 0 ? -x / hsbRange * (nodeWidth - contentWidth) : 0;
        if (!Properties.IS_TOUCH_SUPPORTED) {
            minX = Math.min(minX, 0);
        }
        viewContent.setLayoutX(snapPositionX(minX));
        if (!sp.hvalueProperty().isBound()) sp.setHvalue(Utils.clamp(sp.getHmin(), posX, sp.getHmax()));
        return posX;
    }

    private double updatePosY() {
        final ScrollPane sp = getSkinnable();
        double vsbRange = vsb.getMax() - vsb.getMin();
        double minY = vsbRange > 0 ? -posY / vsbRange * (nodeHeight - contentHeight) : 0;
        if (!Properties.IS_TOUCH_SUPPORTED) {
            minY = Math.min(minY, 0);
        }
        viewContent.setLayoutY(snapPositionY(minY));
        if (!sp.vvalueProperty().isBound()) sp.setVvalue(Utils.clamp(sp.getVmin(), posY, sp.getVmax()));
        return posY;
    }

    private void resetClip() {
        clipRect.setWidth(snapSizeX(contentWidth));
        clipRect.setHeight(snapSizeY(contentHeight));
    }

    private void startSBReleasedAnimation() {
        if (sbTouchTimeline == null) {
            /*
            ** timeline to leave the scrollbars visible for a short
            ** while after a scroll/drag
            */
            sbTouchTimeline = new Timeline();
            sbTouchKF1 = new KeyFrame(Duration.millis(0), event -> {
                tempVisibility = true;
                if ((touchDetected == true || mouseDown == true) && NodeHelper.isTreeShowing(getSkinnable())) {
                    sbTouchTimeline.playFromStart();
                }
            });

            sbTouchKF2 = new KeyFrame(Duration.millis(1000), event -> {
                tempVisibility = false;
                getSkinnable().requestLayout();
            });
            sbTouchTimeline.getKeyFrames().addAll(sbTouchKF1, sbTouchKF2);
        }
        sbTouchTimeline.playFromStart();
    }

    private void startContentsToViewport() {
        double newPosX = posX;
        double newPosY = posY;

        setContentPosX(posX);
        setContentPosY(posY);

        if (posY > getSkinnable().getVmax()) {
            newPosY = getSkinnable().getVmax();
        }
        else if (posY < getSkinnable().getVmin()) {
            newPosY = getSkinnable().getVmin();
        }


        if (posX > getSkinnable().getHmax()) {
            newPosX = getSkinnable().getHmax();
        }
        else if (posX < getSkinnable().getHmin()) {
            newPosX = getSkinnable().getHmin();
        }

        if (!Properties.IS_TOUCH_SUPPORTED) {
            startSBReleasedAnimation();
        }

        /*
        ** timeline to return the contents of the scrollpane to the viewport
        */
        if (contentsToViewTimeline != null) {
            contentsToViewTimeline.stop();
        }
        contentsToViewTimeline = new Timeline();
        /*
        ** short pause before animation starts
        */
        contentsToViewKF1 = new KeyFrame(Duration.millis(50));
        /*
        ** reposition
        */
        contentsToViewKF2 = new KeyFrame(Duration.millis(150), event -> {
            getSkinnable().requestLayout();
        },
                new KeyValue(contentPosX, newPosX),
                new KeyValue(contentPosY, newPosY)
        );
        /*
        ** block out 'aftershocks', but real events will
        ** still reactivate
        */
        contentsToViewKF3 = new KeyFrame(Duration.millis(1500));
        contentsToViewTimeline.getKeyFrames().addAll(contentsToViewKF1, contentsToViewKF2, contentsToViewKF3);
        contentsToViewTimeline.playFromStart();
    }
}
