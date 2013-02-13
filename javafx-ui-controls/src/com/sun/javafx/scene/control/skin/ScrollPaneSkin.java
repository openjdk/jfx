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

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.ScrollToEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.Utils;
import com.sun.javafx.scene.control.behavior.ScrollPaneBehavior;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.scene.traversal.TraverseListener;

import static com.sun.javafx.Utils.*;
import static com.sun.javafx.scene.control.skin.Utils.*;
import javafx.geometry.Insets;

public class ScrollPaneSkin extends BehaviorSkinBase<ScrollPane, ScrollPaneBehavior> implements TraverseListener {
    /***************************************************************************
     *                                                                         *
     * UI Subcomponents                                                        *
     *                                                                         *
     **************************************************************************/

    private static final double DEFAULT_PREF_SIZE = 100.0;

    private static final double DEFAULT_MIN_SIZE = 36.0;

    private static final double DEFAULT_SB_BREADTH = 12.0;
    private static final double DEFAULT_EMBEDDED_SB_BREADTH = 8.0;

    private static final double PAN_THRESHOLD = 0.5;

    // state from the control

    private Node scrollNode;

    private double nodeWidth;
    private double nodeHeight;

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
    protected ScrollBar hsb;
    protected ScrollBar vsb;

    double pressX;
    double pressY;
    double ohvalue;
    double ovvalue;
    private Cursor saveCursor =  null;
    private boolean dragDetected = false;
    private boolean touchDetected = false;
    private boolean mouseDown = false;

    Rectangle clipRect;

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ScrollPaneSkin(final ScrollPane scrollpane) {
        super(scrollpane, new ScrollPaneBehavior(scrollpane));
        initialize();
        // Register listeners
        registerChangeListener(scrollpane.contentProperty(), "NODE");
        registerChangeListener(scrollpane.fitToWidthProperty(), "FIT_TO_WIDTH");
        registerChangeListener(scrollpane.fitToHeightProperty(), "FIT_TO_HEIGHT");
        registerChangeListener(scrollpane.hbarPolicyProperty(), "HBAR_POLICY");
        registerChangeListener(scrollpane.vbarPolicyProperty(), "VBAR_POLICY");
        registerChangeListener(scrollpane.hvalueProperty(), "HVALUE");
        registerChangeListener(scrollpane.vvalueProperty(), "VVALUE");
        registerChangeListener(scrollpane.prefViewportWidthProperty(), "PREF_VIEWPORT_WIDTH");
        registerChangeListener(scrollpane.prefViewportHeightProperty(), "PREF_VIEWPORT_HEIGHT");
        
        scrollpane.addEventHandler(ScrollToEvent.scrollToNode(), new EventHandler<ScrollToEvent<Node>>() {
            @Override public void handle(ScrollToEvent<Node> event) {
                Node n = event.getScrollTarget();
                Bounds b = scrollpane.sceneToLocal(n.localToScene(n.getLayoutBounds()));
                scrollBoundsIntoView(b);
            }
        });
    }

    private final InvalidationListener nodeListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            if (nodeWidth != -1.0 && nodeHeight != -1.0) {
                // if new size causes scrollbar visibility to change, then need to relayout
                if (vsbvis != determineVerticalSBVisible() || hsbvis != determineHorizontalSBVisible()) {
                    getSkinnable().requestLayout();
                } else {
                    // otherwise just update scrollbars based on new scrollNode size
                    updateVerticalSB();
                    updateHorizontalSB();
                }
            }
        }
    };


    /*
    ** The content of the ScrollPane has just changed bounds, check scrollBar positions.
    */ 
   private final ChangeListener<Bounds> boundsChangeListener = new ChangeListener<Bounds>() {
        @Override public void changed(ObservableValue<? extends Bounds> observable, Bounds oldBounds, Bounds newBounds) {
            final Insets padding = getSkinnable().getInsets();
            
            /*
            ** For a height change then we want to reduce
            ** viewport vertical jumping as much as possible. 
            ** We set a new vsb value to try to keep the same
            ** content position at the top of the viewport
            */
            double oldHeight = oldBounds.getHeight();
            double newHeight = newBounds.getHeight();
            if (oldHeight != newHeight) {
                double oldPositionY = (snapPosition(padding.getTop() - posY / (vsb.getMax() - vsb.getMin()) * (oldHeight - contentHeight)));
                double newPositionY = (snapPosition(padding.getTop() - posY / (vsb.getMax() - vsb.getMin()) * (newHeight - contentHeight)));
                
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
            if (oldWidth != newWidth) {
                double oldPositionX = (snapPosition(padding.getLeft() - posX / (hsb.getMax() - hsb.getMin()) * (oldWidth - contentWidth)));
                double newPositionX = (snapPosition(padding.getLeft() - posX / (hsb.getMax() - hsb.getMin()) * (newWidth - contentWidth)));

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


    private void initialize() {
        // requestLayout calls below should not trigger requestLayout above ScrollPane
//        setManaged(false);

        ScrollPane control = getSkinnable();
        scrollNode = control.getContent();

        if (scrollNode != null) {
            scrollNode.layoutBoundsProperty().addListener(nodeListener);
            scrollNode.layoutBoundsProperty().addListener(boundsChangeListener);
        }

        viewRect = new StackPane() {

            @Override
            protected void layoutChildren() {
                viewContent.resize(getWidth(), getHeight());
            }

        };
        // prevent requestLayout requests from within scrollNode from percolating up
        viewRect.setManaged(false);
        viewRect.setCache(true);

        clipRect = new Rectangle();
        viewRect.setClip(clipRect);

        hsb = new ScrollBar();

        vsb = new ScrollBar();
        vsb.setOrientation(Orientation.VERTICAL);

        corner = new StackPane();
        corner.getStyleClass().setAll("corner");

        viewContent = new StackPane() {
            @Override public void requestLayout() {
                // if scrollNode requested layout, will want to recompute
                nodeWidth = -1;
                nodeHeight = -1;
                super.requestLayout(); // add as layout root for next layout pass
            }
            @Override protected void layoutChildren() {
                if (nodeWidth == -1 || nodeHeight == -1) {
                    computeScrollNodeSize(getWidth(),getHeight());
                }
                if (scrollNode != null && scrollNode.isResizable()) {
                    scrollNode.resize(snapSize(nodeWidth), snapSize(nodeHeight));
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

        /*
        ** listeners, and assorted housekeeping
        */
        InvalidationListener vsbListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                if (!PlatformUtil.isEmbedded()) {
                    posY = Utils.clamp(getSkinnable().getVmin(), vsb.getValue(), getSkinnable().getVmax());
                }
                else {
                    posY = vsb.getValue();
                }
                updatePosY();
            }
        };
        vsb.valueProperty().addListener(vsbListener);

        InvalidationListener hsbListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                if (!PlatformUtil.isEmbedded()) {
                    posX = Utils.clamp(getSkinnable().getHmin(), hsb.getValue(), getSkinnable().getHmax());
                }
                else {
                    posX = hsb.getValue();
                }
                updatePosX();
            }
        };
        hsb.valueProperty().addListener(hsbListener);

        viewRect.setOnMousePressed(new EventHandler<javafx.scene.input.MouseEvent>() {
           @Override public void handle(javafx.scene.input.MouseEvent e) {
               if (PlatformUtil.isEmbedded()) {
                   startSBReleasedAnimation();
               }
               mouseDown = true;
               pressX = e.getX();
               pressY = e.getY();
               ohvalue = hsb.getValue();
               ovvalue = vsb.getValue();
           }
        });


        viewRect.setOnDragDetected(new EventHandler<javafx.scene.input.MouseEvent>() {
           @Override public void handle(javafx.scene.input.MouseEvent e) {
                if (PlatformUtil.isEmbedded()) {
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
           }
        });

        viewRect.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                  if (PlatformUtil.isEmbedded()) {
                    startSBReleasedAnimation();
                 }
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
            }
        });
        viewRect.setOnMouseDragged(new EventHandler<javafx.scene.input.MouseEvent>() {
           @Override public void handle(javafx.scene.input.MouseEvent e) {
                if (PlatformUtil.isEmbedded()) {
                    startSBReleasedAnimation();
                }
               /*
               ** for mobile-touch we allow drag, even if not pannagle
               */
               if (getSkinnable().isPannable() || PlatformUtil.isEmbedded()) {
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
                           if (!PlatformUtil.isEmbedded()) {
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
                           if (!PlatformUtil.isEmbedded()) {
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
           }
        });


        /*
        ** don't allow the ScrollBar to handle the ScrollEvent,
        ** In a ScrollPane a vertical scroll should scroll on the vertical only,
        ** whereas in a horizontal ScrollBar it can scroll horizontally.
        */ 
        final EventDispatcher blockEventDispatcher = new EventDispatcher() {
           @Override public Event dispatchEvent(Event event, EventDispatchChain tail) {
               // block the event from being passed down to children
               return event;
           }
        };
        // block ScrollEvent from being passed down to scrollbar's skin
        final EventDispatcher oldHsbEventDispatcher = hsb.getEventDispatcher();
        hsb.setEventDispatcher(new EventDispatcher() {
           @Override public Event dispatchEvent(Event event, EventDispatchChain tail) {
               if (event.getEventType() == ScrollEvent.SCROLL &&
                       !((ScrollEvent)event).isDirect()) {
                   tail = tail.prepend(blockEventDispatcher);
                   tail = tail.prepend(oldHsbEventDispatcher);
                   return tail.dispatchEvent(event);
               }
               return oldHsbEventDispatcher.dispatchEvent(event, tail);
           }
        });
        // block ScrollEvent from being passed down to scrollbar's skin
        final EventDispatcher oldVsbEventDispatcher = vsb.getEventDispatcher();
        vsb.setEventDispatcher(new EventDispatcher() {
           @Override public Event dispatchEvent(Event event, EventDispatchChain tail) {
               if (event.getEventType() == ScrollEvent.SCROLL &&
                       !((ScrollEvent)event).isDirect()) {
                   tail = tail.prepend(blockEventDispatcher);
                   tail = tail.prepend(oldVsbEventDispatcher);
                   return tail.dispatchEvent(event);
               }
               return oldVsbEventDispatcher.dispatchEvent(event, tail);
           }
        });

        /*
        ** listen for ScrollEvents over the whole of the ScrollPane
        ** area, the above dispatcher having removed the ScrollBars
        ** scroll event handling.
        */
        getSkinnable().setOnScroll(new EventHandler<javafx.scene.input.ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                if (PlatformUtil.isEmbedded()) {
                    startSBReleasedAnimation();
                }
                /*
                ** if we're completely visible then do nothing....
                ** we only consume an event that we've used.
                */
                if (vsb.getVisibleAmount() < vsb.getMax()) {
                    double vRange = getSkinnable().getVmax()-getSkinnable().getVmin();
                    double vPixelValue;
                    if (nodeHeight > 0.0) {
                        vPixelValue = vRange / nodeHeight;
                    }
                    else {
                        vPixelValue = 0.0;
                    }
                    double newValue = vsb.getValue()+(-event.getDeltaY())*vPixelValue;
                    if (!PlatformUtil.isEmbedded()) {
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
                        if (!(((ScrollEvent)event).isInertia()) || (((ScrollEvent)event).isInertia()) && (contentsToViewTimeline == null || contentsToViewTimeline.getStatus() == Status.STOPPED)) {
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
                    double hPixelValue;
                    if (nodeWidth > 0.0) {
                        hPixelValue = hRange / nodeWidth;
                    }
                    else {
                        hPixelValue = 0.0;
                    }

                    double newValue = hsb.getValue()+(-event.getDeltaX())*hPixelValue;
                    if (!PlatformUtil.isEmbedded()) {
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
                        if (!(((ScrollEvent)event).isInertia()) || (((ScrollEvent)event).isInertia()) && (contentsToViewTimeline == null || contentsToViewTimeline.getStatus() == Status.STOPPED)) {
                            hsb.setValue(newValue);

                            if ((newValue > hsb.getMax() || newValue < hsb.getMin()) && (!mouseDown && !touchDetected)) {
                                startContentsToViewport();
                            }
                            event.consume();
                        }
                    }
                }
            }
        });

        /*
        ** there are certain animations that need to know if the touch is
        ** happening.....
        */
        getSkinnable().setOnTouchPressed(new EventHandler<TouchEvent>() {
            @Override public void handle(TouchEvent e) {
                touchDetected = true;
                startSBReleasedAnimation();
                e.consume();
            }
        });

        getSkinnable().setOnTouchReleased(new EventHandler<TouchEvent>() {
            @Override public void handle(TouchEvent e) {
                touchDetected = false;
                startSBReleasedAnimation();
                e.consume();
            }
        });

        TraversalEngine traversalEngine = new TraversalEngine(getSkinnable(), false);
        traversalEngine.addTraverseListener(this);
        getSkinnable().setImpl_traversalEngine(traversalEngine);

        // ScrollPanes do not block all MouseEvents by default, unlike most other UI Controls.
        consumeMouseEvents(false);
    }


    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if ("NODE".equals(p)) {
            if (scrollNode != getSkinnable().getContent()) {
                if (scrollNode != null) {
                    scrollNode.layoutBoundsProperty().removeListener(nodeListener);
                    scrollNode.layoutBoundsProperty().removeListener(boundsChangeListener);
                    viewContent.getChildren().remove(scrollNode);
                }
                scrollNode = getSkinnable().getContent();
                if (scrollNode != null) {
                    nodeWidth = snapSize(scrollNode.getLayoutBounds().getWidth());
                    nodeHeight = snapSize(scrollNode.getLayoutBounds().getHeight());
                    viewContent.getChildren().setAll(scrollNode);
                    scrollNode.layoutBoundsProperty().addListener(nodeListener);
                    scrollNode.layoutBoundsProperty().addListener(boundsChangeListener);
                }
            }
            getSkinnable().requestLayout();
        } else if ("FIT_TO_WIDTH".equals(p) || "FIT_TO_HEIGHT".equals(p)) {
            getSkinnable().requestLayout();
            viewRect.requestLayout();
        } else if ("HBAR_POLICY".equals(p) || "VBAR_POLICY".equals(p)) {
            // change might affect pref size, so requestLayout on control
            getSkinnable().requestLayout();
        } else if ("HVALUE".equals(p)) {
            hsb.setValue(getSkinnable().getHvalue());
        } else if ("VVALUE".equals(p)) {
            vsb.setValue(getSkinnable().getVvalue());
        } else if ("PREF_VIEWPORT_WIDTH".equals(p) || "PREF_VIEWPORT_HEIGHT".equals(p)) {
            // change affects pref size, so requestLayout on control
            getSkinnable().requestLayout();
        }
    }
    
    void scrollBoundsIntoView(Bounds b) {
        double dx = 0.0;
        double dy = 0.0;
        boolean needsLayout = false;
        if (b.getMaxX() > contentWidth) {
            dx = contentWidth - b.getMaxX();
        }
        if (b.getMinX() < 0) {
            dx = -b.getMinX();
        }
        if (b.getMaxY() > contentHeight) {
            dy = contentHeight - b.getMaxY();
        }
        if (b.getMinY() < 0) {
            dy = -b.getMinY();
        }
        // We want to move contentPanel's layoutX,Y by (dx,dy).
        // But to do this we have to set the scrollbars' values appropriately.

        double newHvalue = hsb.getValue();
        double newVvalue = vsb.getValue();
        if (dx != 0) {
            double sdx = -dx * (hsb.getMax() - hsb.getMin()) / (nodeWidth - viewRect.getWidth());
            if (sdx < 0) {
                sdx -= hsb.getUnitIncrement();
            } else {
                sdx += hsb.getUnitIncrement();
            }
            newHvalue = clamp(hsb.getMin(), hsb.getValue() + sdx, hsb.getMax());
            hsb.setValue(newHvalue);
            needsLayout = true;
        }
        if (dy != 0) {
            double sdy = -dy * (vsb.getMax() - vsb.getMin()) / (nodeHeight - viewRect.getHeight());
            if (sdy < 0) {
                sdy -= vsb.getUnitIncrement();
            } else {
                sdy += vsb.getUnitIncrement();
            }
            newVvalue = clamp(vsb.getMin(), vsb.getValue() + sdy, vsb.getMax());
            vsb.setValue(newVvalue);
            needsLayout = true;
        }

        if (needsLayout == true) {
            getSkinnable().requestLayout();
        }
    }

    /*
    ** auto-scroll so node is within (0,0),(contentWidth,contentHeight)
    */
    @Override public void onTraverse(Node n, Bounds b) {
        scrollBoundsIntoView(b);
    }

    public void hsbIncrement() {
        if (hsb != null) hsb.increment();
    }
    public void hsbDecrement() {
        if (hsb != null) hsb.decrement();
    }

    // TODO: add page increment and decrement
    public void hsbPageIncrement() {
        if (hsb != null) hsb.increment();
    }
    // TODO: add page increment and decrement
    public void hsbPageDecrement() {
        if (hsb != null) hsb.decrement();
    }

    public void vsbIncrement() {
        if (vsb != null) vsb.increment();
    }
    public void vsbDecrement() {
        if (vsb != null) vsb.decrement();
    }

    // TODO: add page increment and decrement
    public void vsbPageIncrement() {
        if (vsb != null) vsb.increment();
    }
    // TODO: add page increment and decrement
    public void vsbPageDecrement() {
        if (vsb != null) vsb.decrement();
    }

    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/

    @Override protected double computePrefWidth(double height) {
        final ScrollPane sp = getSkinnable();
        
        if (sp.getPrefViewportWidth() > 0) {
            double vsbWidth = sp.getVbarPolicy() == ScrollBarPolicy.ALWAYS? vsb.prefWidth(-1) : 0;
            final Insets padding = sp.getInsets();
            return (sp.getPrefViewportWidth() + vsbWidth + padding.getLeft() + padding.getRight());
        }
        else {
            return DEFAULT_PREF_SIZE;
        }
    }

    @Override protected double computePrefHeight(double width) {
        final ScrollPane sp = getSkinnable();
        
        if (sp.getPrefViewportHeight() > 0) {
            double hsbHeight = sp.getHbarPolicy() == ScrollBarPolicy.ALWAYS? hsb.prefHeight(-1) : 0;
            final Insets padding = sp.getInsets();
            return (sp.getPrefViewportHeight() + hsbHeight + padding.getTop() + padding.getBottom());
        }
        else {
            return DEFAULT_PREF_SIZE;
        }
    }

    @Override protected double computeMinWidth(double height) {
        double w = corner.minWidth(-1);
        return (w > 0) ? (3 * w) : (DEFAULT_MIN_SIZE);
    }

    @Override protected double computeMinHeight(double width) {
        double h = corner.minHeight(-1);
        return (h > 0) ? (3 * h) : (DEFAULT_MIN_SIZE);
    }

    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        final ScrollPane control = getSkinnable();
        final Insets insets = control.getInsets();
        final Insets padding = control.getPadding();

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
        double hsbWidth = contentWidth + padding.getLeft() + padding.getRight();
        double vsbHeight = contentHeight + padding.getTop() + padding.getBottom();

        computeScrollNodeSize(contentWidth, contentHeight);
        computeScrollBarSize();
        vsbvis = determineVerticalSBVisible();
        hsbvis = determineHorizontalSBVisible();

        if (vsbvis) {
            hsbWidth -= vsbWidth;
            if (!PlatformUtil.isEmbedded()) {
                contentWidth -= vsbWidth;
            }
        }
        if (hsbvis) {
            vsbHeight -= hsbHeight;
            if (!PlatformUtil.isEmbedded()) {
                contentHeight -= hsbHeight;
            }
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
                    computeScrollNodeSize(contentWidth, contentHeight);
                }
            } else if (vsbvis && !hsbvis) {
                computeScrollNodeSize(contentWidth, contentHeight);
                hsbvis = determineHorizontalSBVisible();
                if (hsbvis) {
                    // now both are visible
                    contentHeight -= hsbHeight;
                    computeScrollNodeSize(contentWidth, contentHeight);
                }
            }
        }

        // figure out the content area that is to be filled
        double cx = insets.getLeft()-padding.getLeft();
        double cy = insets.getTop()-padding.getTop();

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
            if (padding.getRight() < 1) {
                vsb.resizeRelocate(snapPosition(control.getWidth() - (vsbWidth + (insets.getRight()-padding.getRight()))), 
                                   snapPosition(cy), snapSize(vsbWidth), snapSize(vsbHeight));
            }
            else {
                vsb.resizeRelocate(snapPosition(control.getWidth() - ((vsbWidth+1) + (insets.getRight()-padding.getRight()))), 
                                   snapPosition(cy), snapSize(vsbWidth)+1, snapSize(vsbHeight));
            }
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
            if (padding.getBottom() < 1) {
                hsb.resizeRelocate(snapPosition(cx), snapPosition(control.getHeight() - (hsbHeight + (insets.getBottom()-padding.getBottom()))), 
                                                     snapSize(hsbWidth), snapSize(hsbHeight));
            }
            else {
                hsb.resizeRelocate(snapPosition(cx), snapPosition(control.getHeight() - ((hsbHeight+1) + (insets.getBottom()-padding.getBottom()))), 
                                                     snapSize(hsbWidth), snapSize(hsbHeight)+1);
            }
        }
        updateHorizontalSB();

        viewRect.resize(snapSize(contentWidth), snapSize(contentHeight));
        resetClip();

        if (vsbvis && hsbvis) {
            corner.setVisible(true);
            double cornerWidth = vsbWidth;
            double cornerHeight = hsbHeight;

            if (padding.getRight() >= 1) {
                cornerWidth++;
            }
            if (padding.getBottom() >= 1) {
                cornerHeight++;
            }
            corner.resizeRelocate(snapPosition(vsb.getLayoutX()), snapPosition(hsb.getLayoutY()), snapSize(cornerWidth), snapSize(cornerHeight));
        } else {
            corner.setVisible(false);
        }
        control.setViewportBounds(new BoundingBox(snapPosition(viewContent.getLayoutX()), snapPosition(viewContent.getLayoutY()), snapSize(contentWidth), snapSize(contentHeight)));
    }
    
    private void computeScrollNodeSize(double contentWidth, double contentHeight) {
        if (scrollNode != null) {
            if (scrollNode.isResizable()) {
                ScrollPane control = getSkinnable();
                Orientation bias = scrollNode.getContentBias();
                if (bias == null) {
                    nodeWidth = snapSize(boundedSize(control.isFitToWidth()? contentWidth : scrollNode.prefWidth(-1),
                                                         scrollNode.minWidth(-1),scrollNode.maxWidth(-1)));
                    nodeHeight = snapSize(boundedSize(control.isFitToHeight()? contentHeight : scrollNode.prefHeight(-1),
                                                          scrollNode.minHeight(-1), scrollNode.maxHeight(-1)));

                } else if (bias == Orientation.HORIZONTAL) {
                    nodeWidth = snapSize(boundedSize(control.isFitToWidth()? contentWidth : scrollNode.prefWidth(-1),
                                                         scrollNode.minWidth(-1),scrollNode.maxWidth(-1)));
                    nodeHeight = snapSize(boundedSize(control.isFitToHeight()? contentHeight : scrollNode.prefHeight(nodeWidth),
                                                          scrollNode.minHeight(nodeWidth),scrollNode.maxHeight(nodeWidth)));

                } else { // bias == VERTICAL
                    nodeHeight = snapSize(boundedSize(control.isFitToHeight()? contentHeight : scrollNode.prefHeight(-1),
                                                          scrollNode.minHeight(-1), scrollNode.maxHeight(-1)));
                    nodeWidth = snapSize(boundedSize(control.isFitToWidth()? contentWidth : scrollNode.prefWidth(nodeHeight),
                                                         scrollNode.minWidth(nodeHeight),scrollNode.maxWidth(nodeHeight)));
                }

            } else {
                nodeWidth = snapSize(scrollNode.getLayoutBounds().getWidth());
                nodeHeight = snapSize(scrollNode.getLayoutBounds().getHeight());
            }
        }
    }

    private boolean isReverseNodeOrientation() {
        return (scrollNode != null &&
                getSkinnable().getEffectiveNodeOrientation() !=
                            scrollNode.getEffectiveNodeOrientation());
    }

    private boolean determineHorizontalSBVisible() {
        final ScrollPane sp = getSkinnable();
        final Insets insets = sp.getInsets();
        final double contentw = sp.getWidth() - insets.getLeft() - insets.getRight();
        if (PlatformUtil.isEmbedded()) {
            return (tempVisibility && (nodeWidth > contentw));
        }
        else {
            return (getSkinnable().getHbarPolicy().equals(ScrollBarPolicy.NEVER)) ? false :
                ((getSkinnable().getHbarPolicy().equals(ScrollBarPolicy.ALWAYS)) ? true :
                 ((getSkinnable().isFitToWidth() && scrollNode != null ? scrollNode.isResizable() : false) ?
                  (nodeWidth > contentw && scrollNode.minWidth(-1) > contentw) : (nodeWidth > contentw)));
        }
    }

    private boolean determineVerticalSBVisible() {
        final ScrollPane sp = getSkinnable();
        final Insets insets = sp.getInsets();
        final double contenth = sp.getHeight() - insets.getTop() - insets.getBottom();
        if (PlatformUtil.isEmbedded()) {
            return (tempVisibility && (nodeHeight > contenth));
        }
        else {
            return (getSkinnable().getVbarPolicy().equals(ScrollBarPolicy.NEVER)) ? false :
                ((getSkinnable().getVbarPolicy().equals(ScrollBarPolicy.ALWAYS)) ? true :
                 ((getSkinnable().isFitToHeight() && scrollNode != null ? scrollNode.isResizable() : false) ?
                  (nodeHeight > contenth && scrollNode.minHeight(-1) > contenth) : (nodeHeight > contenth)));
        }
    }

    private void computeScrollBarSize() {
        vsbWidth = snapSize(vsb.prefWidth(-1));
        if (vsbWidth == 0) {
            //            println("*** WARNING ScrollPaneSkin: can't get scroll bar width, using {DEFAULT_SB_BREADTH}");
            if (PlatformUtil.isEmbedded()) {
                vsbWidth = DEFAULT_EMBEDDED_SB_BREADTH;
            }
            else {
                vsbWidth = DEFAULT_SB_BREADTH;
            }
        }
        hsbHeight = snapSize(hsb.prefHeight(-1));
        if (hsbHeight == 0) {
            //            println("*** WARNING ScrollPaneSkin: can't get scroll bar height, using {DEFAULT_SB_BREADTH}");
            if (PlatformUtil.isEmbedded()) {
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
                viewContent.setLayoutX(getSkinnable().getInsets().getLeft());
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
                viewContent.setLayoutY(getSkinnable().getInsets().getTop());
            }
        }
    }

    private double updatePosX() {
        final ScrollPane sp = getSkinnable();
        double x = isReverseNodeOrientation() ? (hsb.getMax() - (posX - hsb.getMin())) : posX;
        viewContent.setLayoutX(snapPosition(sp.getInsets().getLeft() - x / (hsb.getMax() - hsb.getMin()) * (nodeWidth - contentWidth)));
        sp.setHvalue(Utils.clamp(sp.getHmin(), posX, sp.getHmax()));
        return posX;
    }

    private double updatePosY() {
        final ScrollPane sp = getSkinnable();
        viewContent.setLayoutY(snapPosition(sp.getInsets().getTop() - posY / (vsb.getMax() - vsb.getMin()) * (nodeHeight - contentHeight)));
        sp.setVvalue(Utils.clamp(sp.getVmin(), posY, sp.getVmax()));
        return posY;
    }

    private void resetClip() {
        clipRect.setWidth(snapSize(contentWidth));
        clipRect.setHeight(snapSize(contentHeight));
        final ScrollPane sp = getSkinnable();
        Insets insets = sp.getInsets();
        clipRect.relocate(snapPosition(insets.getLeft()), snapPosition(insets.getTop()));
    }

    Timeline sbTouchTimeline;
    KeyFrame sbTouchKF1;
    KeyFrame sbTouchKF2;
    Timeline contentsToViewTimeline;
    KeyFrame contentsToViewKF1;
    KeyFrame contentsToViewKF2;
    KeyFrame contentsToViewKF3;

    private boolean tempVisibility;


    protected void startSBReleasedAnimation() {
        if (sbTouchTimeline == null) {
            /*
            ** timeline to leave the scrollbars visible for a short
            ** while after a scroll/drag
            */
            sbTouchTimeline = new Timeline();
            sbTouchKF1 = new KeyFrame(Duration.millis(0), new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    tempVisibility = true;
                }
            });

            sbTouchKF2 = new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    tempVisibility = false;
                    getSkinnable().requestLayout();
                }
            });
            sbTouchTimeline.getKeyFrames().addAll(sbTouchKF1, sbTouchKF2);
        }
        sbTouchTimeline.playFromStart();
    }



    protected void startContentsToViewport() {
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

        if (!PlatformUtil.isEmbedded()) {
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
        contentsToViewKF2 = new KeyFrame(Duration.millis(150), new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    getSkinnable().requestLayout();
                }
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


    private DoubleProperty contentPosX;
    private void setContentPosX(double value) { contentPosXProperty().set(value); }
    private double getContentPosX() { return contentPosX == null ? 0.0 : contentPosX.get(); }
    private DoubleProperty contentPosXProperty() {
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
    private void setContentPosY(double value) { contentPosYProperty().set(value); }
    private double getContentPosY() { return contentPosY == null ? 0.0 : contentPosY.get(); }
    private DoubleProperty contentPosYProperty() {
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
}
