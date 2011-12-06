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

import static com.sun.javafx.Utils.clamp;
import static com.sun.javafx.scene.control.skin.Utils.boundedSize;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventDispatcher;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.scene.control.behavior.ScrollPaneBehavior;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.scene.traversal.TraverseListener;
import com.sun.javafx.Utils;

public class ScrollPaneSkin extends SkinBase<ScrollPane, ScrollPaneBehavior> implements TraverseListener {
    /***************************************************************************
     *                                                                         *
     * UI Subcomponents                                                        *
     *                                                                         *
     **************************************************************************/

    private static final double DEFAULT_PREF_SIZE = 100.0;

    private static final double DEFAULT_MIN_SIZE = 36.0;

    private static final double DEFAULT_SB_BREADTH = 12.0;

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

    Rectangle clipRect;

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ScrollPaneSkin(ScrollPane scrollpane) {
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
    }

    private final InvalidationListener nodeListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            if (nodeWidth != -1.0 && nodeHeight != -1.0) {
                // if new size causes scrollbar visibility to change, then need to relayout
                if (vsbvis != determineVerticalSBVisible() || hsbvis != determineHorizontalSBVisible()) {
                    requestLayout();
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
            /*
            ** For a height change then we want to reduce
            ** viewport vertical jumping as much as possible. 
            ** We set a new vsb value to try to keep the same
            ** content position at the top of the viewport
            */
            double oldHeight = oldBounds.getHeight();
            double newHeight = newBounds.getHeight();
            if (oldHeight != newHeight) {
                double oldPositionY = (snapPosition(getInsets().getTop() - posY / (vsb.getMax() - vsb.getMin()) * (oldHeight - contentHeight)));
                double newPositionY = (snapPosition(getInsets().getTop() - posY / (vsb.getMax() - vsb.getMin()) * (newHeight - contentHeight)));
                
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
                double oldPositionX = (snapPosition(getInsets().getLeft() - posX / (hsb.getMax() - hsb.getMin()) * (oldWidth - contentWidth)));
                double newPositionX = (snapPosition(getInsets().getLeft() - posX / (hsb.getMax() - hsb.getMin()) * (newWidth - contentWidth)));

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
        setManaged(false);

        ScrollPane control = getSkinnable();
        scrollNode = control.getContent();

        if (scrollNode != null) {
            scrollNode.layoutBoundsProperty().addListener(nodeListener);
            scrollNode.layoutBoundsProperty().addListener(boundsChangeListener);
        }

        viewRect = new StackPane() {
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
                    scrollNode.resize(nodeWidth,nodeHeight);
                    if (vsbvis != determineVerticalSBVisible() || hsbvis != determineHorizontalSBVisible()) {
                        ScrollPaneSkin.this.requestLayout();
                    }
                }
                if (scrollNode != null) {
                    scrollNode.relocate(0,0);
                }
            }
        };
        // prevent requestLayout requests from within scrollNode from percolating up
        viewRect.setManaged(false);

        clipRect = new Rectangle();
        viewRect.setClip(clipRect);

        hsb = new ScrollBar();

        vsb = new ScrollBar();
        vsb.setOrientation(Orientation.VERTICAL);

        corner = new StackPane();
        corner.getStyleClass().setAll("corner");

        viewRect.getChildren().clear();
        if (scrollNode != null) {
            viewRect.getChildren().add(scrollNode);
        }

        getChildren().clear();
        getChildren().addAll(viewRect, vsb, hsb, corner);

        /*
        ** listeners, and assorted housekeeping
        */
        InvalidationListener vsbListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                posY = Utils.clamp(getSkinnable().getVmin(), vsb.getValue(), getSkinnable().getVmax());
                updatePosY();
            }
        };
        vsb.valueProperty().addListener(vsbListener);

        InvalidationListener hsbListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                posX = Utils.clamp(getSkinnable().getHmin(), hsb.getValue(), getSkinnable().getHmax());
                updatePosX();
            }
        };
        hsb.valueProperty().addListener(hsbListener);

        viewRect.setOnMousePressed(new EventHandler<javafx.scene.input.MouseEvent>() {
           @Override public void handle(javafx.scene.input.MouseEvent e) {
               pressX = e.getX() + viewRect.getLayoutX();
               pressY = e.getY() + viewRect.getLayoutY();
               ohvalue = hsb.getValue();
               ovvalue = vsb.getValue();
           }
        });


        viewRect.setOnDragDetected(new EventHandler<javafx.scene.input.MouseEvent>() {
           @Override public void handle(javafx.scene.input.MouseEvent e) {
               if (getSkinnable().isPannable()) {
                 dragDetected = true;
                 if (saveCursor == null) {
                     saveCursor = getCursor();
                     if (saveCursor == null) {
                         saveCursor = Cursor.DEFAULT;
                     }
                     setCursor(Cursor.MOVE);
                     requestLayout();
                 }
               }
           }
        });

        viewRect.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                 if (dragDetected == true) {
                     if (saveCursor != null) {
                         setCursor(saveCursor);
                         saveCursor = null;
                         requestLayout();
                     }
                     dragDetected = false;
                 }
            }
        });
        viewRect.setOnMouseDragged(new EventHandler<javafx.scene.input.MouseEvent>() {
           @Override public void handle(javafx.scene.input.MouseEvent e) {
               if (getSkinnable().isPannable()) {
                   double deltaX = pressX - (e.getX() + viewRect.getLayoutX());
                   double deltaY = pressY - (e.getY() + viewRect.getLayoutY());
                   /*
                   ** we only drag if not all of the content is visible.
                   */
                   if (hsb.getVisibleAmount() > 0.0 && hsb.getVisibleAmount() < hsb.getMax()) {
                       if (Math.abs(deltaX) > PAN_THRESHOLD) {
                           double newHVal = (ohvalue + deltaX / (nodeWidth - viewRect.getWidth()) * (hsb.getMax() - hsb.getMin()));
                           if (newHVal > hsb.getMax()) {
                               newHVal = hsb.getMax();
                           }
                           else if (newHVal < hsb.getMin()) {
                               newHVal = hsb.getMin();
                           }
                           hsb.setValue(newHVal);
                       }
                   }
                   /*
                   ** we only drag if not all of the content is visible.
                   */
                   if (vsb.getVisibleAmount() > 0.0 && vsb.getVisibleAmount() < vsb.getMax()) {
                       if (Math.abs(deltaY) > PAN_THRESHOLD) {
                           double newVVal = (ovvalue + deltaY / (nodeHeight - viewRect.getHeight()) * (vsb.getMax() - vsb.getMin()));
                           if (newVVal > vsb.getMax()) {
                               newVVal = vsb.getMax();
                           }
                           else if (newVVal < vsb.getMin()) {
                               newVVal = vsb.getMin();
                           }
                           vsb.setValue(newVVal);
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
               if (event.getEventType() == ScrollEvent.SCROLL) {
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
               if (event.getEventType() == ScrollEvent.SCROLL) {
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
        setOnScroll(new EventHandler<javafx.scene.input.ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                /*
                ** if we're completely visible then do nothing....
                ** we only consume an event that we've used.
                */
                if (vsb.getVisibleAmount() < vsb.getMax()) {

                    if ((event.getDeltaY() > 0.0 && vsb.getValue() > vsb.getMin()) ||
                        (event.getDeltaY() < 0.0 && vsb.getValue() < vsb.getMax())) {
                          double vRange = getSkinnable().getVmax()-getSkinnable().getVmin();
                          double vPixelValue = vRange / getSkinnable().getHeight();
                          vsb.setValue(vsb.getValue()+(-event.getDeltaY())*vPixelValue);
                          event.consume();
                    }
                }

                if (hsb.getVisibleAmount() < hsb.getMax()) {
                    if ((event.getDeltaX() > 0.0 && hsb.getValue() > hsb.getMin()) ||
                        (event.getDeltaX() < 0.0 && hsb.getValue() < hsb.getMax())) {
                            double hRange = getSkinnable().getHmax()-getSkinnable().getHmin();
                            double hPixelValue = hRange / getSkinnable().getWidth();
                            hsb.setValue(hsb.getValue()+(-event.getDeltaX())*hPixelValue);
                            event.consume();
                    }
                }
            }
        });

        TraversalEngine traversalEngine = new TraversalEngine(this, false);
        traversalEngine.addTraverseListener(this);
        setImpl_traversalEngine(traversalEngine);

        // ScrollPanes do not block all MouseEvents by default, unlike most other UI Controls.
        consumeMouseEvents(false);
    }


    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if (p == "NODE") {
            if (scrollNode != getSkinnable().getContent()) {
                if (scrollNode != null) {
                    scrollNode.layoutBoundsProperty().removeListener(nodeListener);
                    scrollNode.layoutBoundsProperty().removeListener(boundsChangeListener);
                    viewRect.getChildren().remove(scrollNode);
                }
                scrollNode = getSkinnable().getContent();
                if (scrollNode != null) {
                    nodeWidth = Math.floor(scrollNode.getLayoutBounds().getWidth());
                    nodeHeight = Math.floor(scrollNode.getLayoutBounds().getHeight());
                    viewRect.getChildren().setAll(scrollNode);
                    scrollNode.layoutBoundsProperty().addListener(nodeListener);
                    scrollNode.layoutBoundsProperty().addListener(boundsChangeListener);
                }
            }
            requestLayout();
        } else if (p == "FIT_TO_WIDTH") {
            requestLayout();
            viewRect.requestLayout();
        } else if (p == "FIT_TO_HEIGHT") {
            requestLayout();
            viewRect.requestLayout();
        } else if (p == "HBAR_POLICY") {
            // change might affect pref size, so requestLayout on control
            getSkinnable().requestLayout();
            requestLayout();
        } else if (p == "VBAR_POLICY") {
            // change might affect pref size, so requestLayout on control
            getSkinnable().requestLayout();
            requestLayout();
        } else if (p == "HVALUE") {
            hsb.setValue(getSkinnable().getHvalue());
        } else if (p == "VVALUE") {
            vsb.setValue(getSkinnable().getVvalue());
        } else if (p == "PREF_VIEWPORT_WIDTH") {
            // change affects pref size, so requestLayout on control
            getSkinnable().requestLayout();
        } else if (p == "PREF_VIEWPORT_HEIGHT") {
            // change affects pref size, so requestLayout on control
            getSkinnable().requestLayout();
        }
    }

    /*
    ** auto-scroll so node is within (0,0),(contentWidth,contentHeight)
    */
    public void onTraverse(Node n, Bounds b) {
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
            requestLayout();
        }
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
        if (getSkinnable().getPrefViewportWidth() > 0) {
            double vsbWidth = getSkinnable().getVbarPolicy() == ScrollBarPolicy.ALWAYS? vsb.prefWidth(-1) : 0;
            return (getSkinnable().getPrefViewportWidth() + vsbWidth + getInsets().getLeft() + getInsets().getRight());
        }
        else {
            return DEFAULT_PREF_SIZE;
        }
    }

    @Override protected double computePrefHeight(double width) {
        if (getSkinnable().getPrefViewportHeight() > 0) {
            double hsbHeight = getSkinnable().getHbarPolicy() == ScrollBarPolicy.ALWAYS? hsb.prefHeight(-1) : 0;
            return (getSkinnable().getPrefViewportHeight() + hsbHeight + getInsets().getTop() + getInsets().getBottom());
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

    @Override protected void layoutChildren() {
        ScrollPane control = getSkinnable();

        vsb.setMin(control.getVmin());
        vsb.setMax(control.getVmax());

        //should only do this on css setup
        hsb.setMin(control.getHmin());
        hsb.setMax(control.getHmax());

        contentWidth = control.getWidth() - getInsets().getLeft() - getInsets().getRight();
        contentHeight = control.getHeight() - getInsets().getTop() - getInsets().getBottom();

        computeScrollNodeSize(contentWidth, contentHeight);
        computeScrollBarSize();
        vsbvis = determineVerticalSBVisible();
        hsbvis = determineHorizontalSBVisible();

        if (vsbvis) {
            contentWidth -= vsbWidth;
        }
        if (hsbvis) {
            contentHeight -= hsbHeight;
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
        double cx = getInsets().getLeft();
        double cy = getInsets().getTop();

        vsb.setVisible(vsbvis);
        if (vsbvis) {
            vsb.resizeRelocate(snapPosition(control.getWidth() - vsbWidth - getInsets().getRight()), cy, vsbWidth, contentHeight);           
        }
        updateVerticalSB();

        hsb.setVisible(hsbvis);
        if (hsbvis) {
            hsb.resizeRelocate(cx, snapPosition(control.getHeight() - (hsbHeight + getInsets().getBottom())), contentWidth, hsbHeight);
        }
        updateHorizontalSB();

        viewRect.resize(contentWidth, contentHeight);
        clipRect.setWidth(contentWidth);
        clipRect.setHeight(contentHeight);
        clipRect.relocate(getInsets().getLeft() - viewRect.getLayoutX(), getInsets().getTop() - viewRect.getLayoutY());

        if (vsbvis && hsbvis) {
            corner.setVisible(true);
            corner.resizeRelocate(vsb.getLayoutX(), hsb.getLayoutY(), vsbWidth, hsbHeight);
        } else {
            corner.setVisible(false);
        }
        control.setViewportBounds(new BoundingBox(viewRect.getLayoutX(), viewRect.getLayoutY(), contentWidth, contentHeight));
    }
    
    private void computeScrollNodeSize(double contentWidth, double contentHeight) {
        if (scrollNode != null) {
            if (scrollNode.isResizable()) {
                ScrollPane control = getSkinnable();
                Orientation bias = scrollNode.getContentBias();
                if (bias == null) {
                    nodeWidth = boundedSize(control.isFitToWidth()? contentWidth : scrollNode.prefWidth(-1),
                                            scrollNode.minWidth(-1),scrollNode.maxWidth(-1));
                    nodeHeight = boundedSize(control.isFitToHeight()? contentHeight : scrollNode.prefHeight(-1),
                                             scrollNode.minHeight(-1), scrollNode.maxHeight(-1));

                } else if (bias == Orientation.HORIZONTAL) {
                    nodeWidth = boundedSize(control.isFitToWidth()? contentWidth : scrollNode.prefWidth(-1),
                                            scrollNode.minWidth(-1),scrollNode.maxWidth(-1));
                    nodeHeight = boundedSize(control.isFitToHeight()? contentHeight : scrollNode.prefHeight(nodeWidth),
                                            scrollNode.minHeight(nodeWidth),scrollNode.maxHeight(nodeWidth));

                } else { // bias == VERTICAL
                    nodeHeight = boundedSize(control.isFitToHeight()? contentHeight : scrollNode.prefHeight(-1),
                                             scrollNode.minHeight(-1), scrollNode.maxHeight(-1));
                    nodeWidth = boundedSize(control.isFitToWidth()? contentWidth : scrollNode.prefWidth(nodeHeight),
                                             scrollNode.minWidth(nodeHeight),scrollNode.maxWidth(nodeHeight));
                }

            } else {
                nodeWidth = scrollNode.getLayoutBounds().getWidth();
                nodeHeight = scrollNode.getLayoutBounds().getHeight();
            }
        }
    }

    private boolean determineHorizontalSBVisible() {
        final double contentw = getSkinnable().getWidth() - getInsets().getLeft() - getInsets().getRight();
        return (getSkinnable().getHbarPolicy().equals(ScrollBarPolicy.NEVER)) ? false :
                   ((getSkinnable().getHbarPolicy().equals(ScrollBarPolicy.ALWAYS)) ? true :
                       ((getSkinnable().isFitToWidth() && scrollNode.isResizable()) ? false :
                           (nodeWidth > contentw)));
    }

    private boolean determineVerticalSBVisible() {
        final double contenth = getSkinnable().getHeight() - getInsets().getTop() - getInsets().getBottom();
        return (getSkinnable().getVbarPolicy().equals(ScrollBarPolicy.NEVER)) ? false :
                  ((getSkinnable().getVbarPolicy().equals(ScrollBarPolicy.ALWAYS)) ? true :
                      ((getSkinnable().isFitToHeight() && scrollNode.isResizable()) ? false :
                          (nodeHeight > contenth)));
    }

    private void computeScrollBarSize() {
        vsbWidth = vsb.prefWidth(-1);
        if (vsbWidth == 0) {
            //            println("*** WARNING ScrollPaneSkin: can't get scroll bar width, using {DEFAULT_SB_BREADTH}");
            vsbWidth = DEFAULT_SB_BREADTH;
        }
        hsbHeight = hsb.prefHeight(-1);
        if (hsbHeight == 0) {
            //            println("*** WARNING ScrollPaneSkin: can't get scroll bar height, using {DEFAULT_SB_BREADTH}");
            hsbHeight = DEFAULT_SB_BREADTH;
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
                viewRect.setLayoutX(getInsets().getLeft());
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
                viewRect.setLayoutY(getInsets().getTop());
            }
        }
    }

    private double updatePosX() {
        viewRect.setLayoutX(snapPosition(getInsets().getLeft() - posX / (hsb.getMax() - hsb.getMin()) * (nodeWidth - contentWidth)));
        getSkinnable().setHvalue(Utils.clamp(getSkinnable().getHmin(), posX, getSkinnable().getHmax()));
        return posX;
    }

    private double updatePosY() {
        viewRect.setLayoutY(snapPosition(getInsets().getTop() - posY / (vsb.getMax() - vsb.getMin()) * (nodeHeight - contentHeight)));
        getSkinnable().setVvalue(Utils.clamp(getSkinnable().getVmin(), posY, getSkinnable().getVmax()));
        return posY;
    }
}
