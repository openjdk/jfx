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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventDispatcher;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Cell;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import javafx.util.Callback;

/**
 * Implementation of a virtualized container using a cell based mechanism.
 */
public class VirtualFlow extends Region {
    /**
     * There are two main complicating factors in the implementation of the
     * VirtualFlow, which are made even more complicated due to the performance
     * sensitive nature of this code. The first factor is the actual
     * virtualization mechanism, wired together with the PositionMapper.
     * The second complicating factor is the desire to do minimal layout
     * and minimal updates to CSS.
     *
     * Since the layout mechanism runs at most once per pulse, we want to hook
     * into this mechanism for minimal recomputation. Whenever a layout pass
     * is run we record the width/height that the virtual flow was last laid
     * out to. In subsequent passes, if the width/height has not changed then
     * we know we only have to rebuild the cells. If the width or height has
     * changed, then we can make appropriate decisions based on whether the
     * width / height has been reduced or expanded.
     *
     * In various places, if requestLayout is called it is generally just
     * used to indicate that some form of layout needs to happen (either the
     * entire thing has to be reconstructed, or just the cells need to be
     * reconstructed, generally).
     *
     * The accumCell is a special cell which is used in some computations
     * when an actual cell for that item isn't currently available. However,
     * the accumCell must be cleared whenever the cellFactory function is
     * changed because we need to use the cells that come from the new factory.
     *
     * In addition to storing the lastWidth and lastHeight, we also store the
     * number of cells that existed last time we performed a layout. In this
     * way if the number of cells change, we can request a layout and when it
     * occurs we can tell that the number of cells has changed and react
     * accordingly.
     *
     * Because the VirtualFlow can be laid out horizontally or vertically a
     * naming problem is present when trying to conceptualize and implement
     * the flow. In particular, the words "width" and "height" are not
     * precise when describing the unit of measure along the "virtualized"
     * axis and the "orthogonal" axis. For example, the height of a cell when
     * the flow is vertical is the magnitude along the "virtualized axis",
     * and the width is along the axis orthogonal to it.
     *
     * Since "height" and "width" are not reliable terms, we use the words
     * "length" and "breadth" to describe the magnitude of a cell along
     * the virtualized axis and orthogonal axis. For example, in a vertical
     * flow, the height=length and the width=breadth. In a horizontal axis,
     * the height=breadth and the width=length.
     *
     * These terms are somewhat arbitrary, but chosen so that when reading
     * most of the below code you can think in just one dimension, with
     * helper functions converting width/height in to length/breadth, while
     * also being different from width/height so as not to get confused with
     * the actual width/height of a cell.
     */
    /**
     * Indicates the primary direction of virtualization. If true, then the
     * primary direction of virtualization is vertical, meaning that cells will
     * stack vertically on top of each other. If false, then they will stack
     * horizontally next to each other.
     */
    private BooleanProperty vertical;
    public final void setVertical(boolean value) {
        verticalProperty().set(value);
    }

    public final boolean isVertical() {
        return vertical == null ? true : vertical.get();
    }

    public final BooleanProperty verticalProperty() {
        if (vertical == null) {
            vertical = new BooleanPropertyBase(true) {
                @Override protected void invalidated() {
                    addAllToPile();
                    pile.clear();
                    sheet.getChildren().clear();
                    cells.clear();
                    numCellsVisibleOnScreen = -1;
                    lastWidth = lastHeight = maxPrefBreadth = -1;
                    viewportBreadth = viewportLength = lastPosition = 0;
                    hbar.setValue(0);
                    vbar.setValue(0);
                    mapper.adjustPosition(0.0f);
                    setNeedsLayout(true);
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return VirtualFlow.this;
                }

                @Override
                public String getName() {
                    return "vertical";
                }
            };
        }
        return vertical;
    }

    /**
     * Indicates whether the VirtualFlow viewport is capable of being panned
     * by the user (either via the mouse or touch events).
     */
    private boolean pannable = true;
    public boolean isPannable() { return pannable; }
    public void setPannable(boolean value) { this.pannable = value; }

    /**
     * Indicates the number of cells that should be in the flow. The user of
     * the VirtualFlow must set this appropriately. When the cell count changes
     * the VirtualFlow responds by updating the visuals. If the items backing
     * the cells change, but the count has not changed, you must call the
     * reconfigureCells() function to update the visuals.
     */
    private int cellCount;
    public int getCellCount() { return cellCount; }
    public void setCellCount(int i) {
        int oldCount = getCellCount();
        this.cellCount = i;
        
        boolean countChanged = oldCount != cellCount;

        // ensure that the virtual scrollbar adjusts in size based on the current
        // cell count.
        if (countChanged) {
            VirtualScrollBar lengthBar = isVertical() ? vbar : hbar;
            lengthBar.setMax(i);
        }

        // I decided *not* to reset maxPrefBreadth here for the following
        // situation. Suppose I have 30 cells and then I add 10 more. Just
        // because I added 10 more doesn't mean the max pref should be
        // reset. Suppose the first 3 cells were extra long, and I was
        // scrolled down such that they weren't visible. If I were to reset
        // maxPrefBreadth when subsequent cells were added or removed, then the
        // scroll bars would erroneously reset as well. So I do not reset
        // the maxPrefBreadth here.

        // Fix for RT-12512, RT-14301 and RT-14864.
        // Without this, the VirtualFlow length-wise scrollbar would not change
        // as expected. This would leave items unable to be shown, as they
        // would exist outside of the visible area, even when the scrollbar
        // was at its maximum position.
        // FIXME this should be only executed on the pulse, so this will likely
        // lead to performance degradation until it is handled properly.
        if (countChanged) layoutChildren();

        // Fix for RT-13965: Without this line of code, the number of items in
        // the sheet would constantly grow, leaking memory for the life of the
        // application. This was especially apparent when the total number of
        // cells changes - regardless of whether it became bigger or smaller.
        sheet.getChildren().clear();

        if (countChanged) {
            Parent parent = getParent();
            if (parent != null) parent.requestLayout();
        }
        // TODO suppose I had 100 cells and I added 100 more. Further
        // suppose I was scrolled to the bottom when that happened. I
        // actually want to update the position of the mapper such that
        // the view remains "stable".
    }

    /**
     * The position of the VirtualFlow within its list of cells. This is a value
     * between 0 and 1.
     */
    private DoubleProperty position;

    public final void setPosition(double value) {
        positionProperty().set(value);
    }

    public final double getPosition() {
        return position == null ? 0.0 : position.get();
    }

    public final DoubleProperty positionProperty() {
        if (position == null) {
            position = new DoublePropertyBase() {
                /**
                 * Sets the position of the contents within the VirtualFlow and updates
                 * the ui accordingly.
                 */
                @Override protected void invalidated() {
                    if (get() != mapper.getPosition()) {
                        mapper.adjustPosition(get());
                        requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return VirtualFlow.this;
                }

                @Override
                public String getName() {
                    return "position";
                }
            };
        }
        return position;
    }

    /**
     * Callback which is invoked whenever the VirtualFlow needs a new
     * IndexedCell. The VirtualFlow attempts to reuse cells whenever possible
     * and only creates the minimal number of cells necessary.
     */
    private Callback<VirtualFlow, ? extends IndexedCell> createCell;
    public Callback<VirtualFlow, ? extends IndexedCell> getCreateCell() { return createCell; }
    protected void setCreateCell(Callback<VirtualFlow, ? extends IndexedCell> cc) {
        this.createCell = cc;

        if (createCell != null) {
            accumCell = null;
            setNeedsLayout(true);
            recreateCells();
            if (getParent() != null) getParent().requestLayout();
        }
    }

//    /**
//     * Called when the cell's visuals need to be reconfigured.
//     */
//    private Runnable1<Void, IndexedCell> configCell;
//    public Runnable1<Void, IndexedCell> getConfigCell() { return configCell; }
//    protected void setConfigCell(Runnable1<Void, IndexedCell> cc) {
//        this.configCell = cc;
//
//        if (configCell != null) {
//            // If this function changes dynamically then we need to
//            // reconfigure all of the cells.
//            maxPrefBreadth = -1;
//            requestLayout();
//            if (getParent() != null) getParent().requestLayout();
//        }
//    }

    /**
     * The mapper is used to map from the VirtualFlow's "position" to the actual
     * cells and so forth that are represented by that position.
     */
    private final PositionMapper mapper;

    /**
     * The number of cells on the first full page. This is recomputed whenever
     * the viewportLength changes, and is used for computing the visibleAmount
     * of the lengthBar.
     */
    private int numCellsVisibleOnScreen = -1;

//    /**
//     * Set to true during layoutChildren() and adjustPixels. This flag is used by the
//     * ClippedContainer class. If a requestLayout happens on the ClippedContainer and
//     * layingOut is false, then we assume one of the cells may have changed
//     * size. So we will then ask all the existing cells what their sizes are,
//     * and if the sum is different from the sum of their preferred sizes, then
//     * we know we need to adjust the sizes of all the cells.
//     */
//    private boolean layingOut = false;

    /**
     * The maximum preferred size in the non-virtual direction. For example,
     * if vertical, then this is the max pref width of all cells encountered.
     * <p>
     * In general, this is the largest preferred size in the non-virtual
     * direction that we have ever encountered. We don't reduce this size
     * unless instructed to do so, so as to reduce the amount of scroll bar
     * jitter. The access on this variable is package ONLY FOR TESTING.
     */
    double maxPrefBreadth;

    /**
     * The breadth of the viewport portion of the VirtualFlow as computed during
     * the layout pass. In a vertical flow this would be the same as the clip
     * view width. In a horizontal flow this is the clip view height.
     * The access on this variable is package ONLY FOR TESTING.
     */
    double viewportBreadth;

    /**
     * The length of the viewport portion of the VirtualFlow as computed
     * during the layout pass. In a vertical flow this would be the same as the
     * clip view height. In a horizontal flow this is the clip view width.
     * The access on this variable is package ONLY FOR TESTING.
     */
    double viewportLength;

    /**
     * The width of the VirtualFlow the last time it was laid out. We
     * use this information for several fast paths during the layout pass.
     */
    double lastWidth = -1;

    /**
     * The height of the VirtualFlow the last time it was laid out. We
     * use this information for several fast paths during the layout pass.
     */
    double lastHeight = -1;

    /**
     * The number of "virtual" cells in the flow the last time it was laid out.
     * For example, there may have been 1000 virtual cells, but only 20 actual
     * cells created and in use. In that case, lastCellCount would be 1000.
     */
    int lastCellCount = 0;

    /**
     * We remember the last value for vertical the last time we laid out the
     * flow. If vertical has changed, we will want to change the max & value
     * for the different scroll bars. Since we do all the scroll bar update
     * work in the layoutChildren function, we need to know what the old value for
     * vertical was.
     */
    boolean lastVertical;

    /**
     * The position last time we laid out. If none of the lastXXX vars have
     * changed respective to their values in layoutChildren, then we can just punt
     * out of the method (I hope...)
     */
    double lastPosition;

    /**
     * The breadth of the first visible cell last time we laid out.
     */
    double lastCellBreadth = -1;

    /**
     * The length of the first visible cell last time we laid out.
     */
    double lastCellLength = -1;

    /**
     * The list of cells representing those cells which actually make up the
     * current view. The cells are ordered such that the first cell in this
     * list is the first in the view, and the last cell is the last in the
     * view. When pixel scrolling, the list is simply shifted and items drop
     * off the beginning or the end, depending on the order of scrolling.
     * <p>
     * This is package private ONLY FOR TESTING
     */
    final ArrayLinkedList<IndexedCell> cells = new ArrayLinkedList<IndexedCell>();

    /**
     * A structure containing cells that can be reused later. These are cells
     * that at one time were needed to populate the view, but now are no longer
     * needed. We keep them here until they are needed again.
     * <p>
     * This is package private ONLY FOR TESTING
     */
    final ArrayLinkedList<IndexedCell> pile = new ArrayLinkedList<IndexedCell>();

    /**
     * A special cell used to accumulate bounds, such that we reduce object
     * churn. This cell must be recreated whenever the cell factory function
     * changes. This has package access ONLY for testing.
     */
    IndexedCell accumCell;

    /**
     * This group is used for holding the 'accumCell'. 'accumCell' must
     * be added to the skin for it to be styled. Otherwise, it doesn't
     * report the correct width/height leading to issues when scrolling
     * the flow
     */
    Group accumCellParent;

    /**
     * The group which holds the cells.
     */
    Group sheet;
    /**
     * The scroll bar used for scrolling horizontally. This has package access
     * ONLY for testing.
     */
    private VirtualScrollBar hbar = new VirtualScrollBar(this);

    final VirtualScrollBar getHbar() {
        return hbar;
    }
    /**
     * The scroll bar used to scrolling vertically. This has package access
     * ONLY for testing.
     */
    private VirtualScrollBar vbar = new VirtualScrollBar(this);

    final VirtualScrollBar getVbar() {
        return vbar;
    }

    /**
     * Control in which the cell's sheet is placed and forms the viewport. The
     * viewportBreadth and viewportLength are simply the dimensions of the
     * clipView. This has package access ONLY for testing.
     */
    ClippedContainer clipView;

    /**
     * When both the horizontal and vertical scroll bars are visible,
     * we have to 'fill in' the bottom right corner where the two scroll bars
     * meet. This is handled by this corner region. This has package access
     * ONLY for testing.
     */
    StackPane corner;

    // used for panning the virtual flow
    private double lastX;
    private double lastY;
    private boolean isPanning = false;

    public VirtualFlow() {
        mapper = new PositionMapper();
        mapper.setGetItemSize(new Callback<Integer, Double>() {
            @Override public Double call(Integer itemIndex) {
                return getCellLength(itemIndex);
            }
        });

        initContent();
        initBinds();
    }

    private void initContent() {
        // --- sheet
        sheet = new Group();
        sheet.setAutoSizeChildren(false);

        // --- clipView
        clipView = new ClippedContainer(this);
        clipView.setNode(sheet);
        getChildren().add(clipView);

        // --- accumCellParent
        accumCellParent = new Group();
        accumCellParent.setManaged(false);
        accumCellParent.setVisible(false);
        getChildren().add(accumCellParent);

        
        /*
        ** don't allow the ScrollBar to handle the ScrollEvent,
        ** In a VirtualFlow a vertical scroll should scroll on the vertical only,
        ** whereas in a horizontal ScrollBar it can scroll horizontally.
        */ 
        final EventDispatcher blockEventDispatcher = new EventDispatcher() {
           @Override public Event dispatchEvent(Event event, EventDispatchChain tail) {
               // block the event from being passed down to children
               return event;
           }
        };
        // block ScrollEvent from being passed down to scrollbar's skin
        final EventDispatcher oldHsbEventDispatcher = hbar.getEventDispatcher();
        hbar.setEventDispatcher(new EventDispatcher() {
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
        final EventDispatcher oldVsbEventDispatcher = vbar.getEventDispatcher();
        vbar.setEventDispatcher(new EventDispatcher() {
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
        ** listen for ScrollEvents over the whole of the VirtualFlow
        ** area, the above dispatcher having removed the ScrollBars
        ** scroll event handling.
        */
        setOnScroll(new EventHandler<javafx.scene.input.ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                /*
                ** calculate the delta in the direction of the flow.
                */ 
                double virtualDelta = 0.0;
                if (isVertical()) {
                    switch(event.getTextDeltaYUnits()) {
                        case PAGES:
                            virtualDelta = event.getTextDeltaY() * lastHeight;
                            break;
                        case LINES:
                            virtualDelta = event.getTextDeltaY() * lastCellLength;
                            break;
                        case NONE:
                            virtualDelta = event.getDeltaY();
                    }
                } else { // horizontal
                    switch(event.getTextDeltaXUnits()) {
                        case CHARACTERS:
                            // can we get character size here?
                            // for now, fall through to pixel values
                        case NONE:
                            double dx = event.getDeltaX();
                            double dy = event.getDeltaY();

                            virtualDelta = (Math.abs(dx) > Math.abs(dy) ? dx : dy);
                    }
                }

                if (virtualDelta != 0.0) { 
                    /*
                    ** only consume it if we use it
                    */
                    adjustPixels(-virtualDelta);
                    event.consume();
                }
                else {
                    /*
                    ** we didn't scroll in the Virtual plane, lets see
                    ** if we scrolled on the other plane.
                    */
                    ScrollBar nonVirtualBar = isVertical() ? hbar : vbar;
                    if (nonVirtualBar.isVisible()) {

                        double nonVirtualDelta = isVertical() ? event.getDeltaX() : event.getDeltaY();
                        double newValue = nonVirtualBar.getValue() - nonVirtualDelta;

                        if (newValue < nonVirtualBar.getMin()) {
                            nonVirtualBar.setValue(nonVirtualBar.getMin());
                        } else if (newValue > nonVirtualBar.getMax()) {
                            nonVirtualBar.setValue(nonVirtualBar.getMax());
                        } else {
                            nonVirtualBar.setValue(newValue);
                        }
                        event.consume();
                    }
                }
            }
        });


        addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if (isFocusTraversable()) {
                    requestFocus();
                }

                lastX = e.getX();
                lastY = e.getY();

                // determine whether the user has push down on the virtual flow,
                // or whether it is the scrollbar. This is done to prevent
                // mouse events being 'doubled up' when dragging the scrollbar
                // thumb - it has the side-effect of also starting the panning
                // code, leading to flicker
                isPanning = ! (vbar.getBoundsInParent().contains(e.getX(), e.getY())
                        || hbar.getBoundsInParent().contains(e.getX(), e.getY()));
            }
        });
        addEventFilter(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if (! isPanning || ! isPannable()) return;

                // With panning enabled, we support panning in both vertical
                // and horizontal directions, regardless of the fact that
                // VirtualFlow is virtual in only one direction.
                double xDelta = lastX - e.getX();
                double yDelta = lastY - e.getY();

                // figure out the distance that the mouse moved in the virtual
                // direction, and then perform the movement along that axis
                // virtualDelta will contain the amount we actually did move
                double virtualDelta = isVertical() ? yDelta : xDelta;
                double actual = adjustPixels(virtualDelta);
                if (actual != 0) {
                    // update last* here, as we know we've just adjusted the
                    // scrollbar. This means we don't get the situation where a
                    // user presses-and-drags a long way past the min or max
                    // values, only to change directions and see the scrollbar
                    // start moving immediately.
                    if (isVertical()) lastY = e.getY();
                    else lastX = e.getX();
                }

                // similarly, we do the same in the non-virtual direction
                double nonVirtualDelta = isVertical() ? xDelta : yDelta;
                ScrollBar nonVirtualBar = isVertical() ? hbar : vbar;
                if (nonVirtualBar.isVisible()) {
                    double newValue = nonVirtualBar.getValue() + nonVirtualDelta;
                    if (newValue < nonVirtualBar.getMin()) {
                        nonVirtualBar.setValue(nonVirtualBar.getMin());
                    } else if (newValue > nonVirtualBar.getMax()) {
                        nonVirtualBar.setValue(nonVirtualBar.getMax());
                    } else {
                        nonVirtualBar.setValue(newValue);

                        // same as the last* comment above
                        if (isVertical()) lastX = e.getX();
                        else lastY = e.getY();
                    }
                }
            }
        });

        /*
         * We place the scrollbars _above_ the rectangle, such that the drag
         * operations often used in conjunction with scrollbars aren't
         * misinterpreted as drag operations on the rectangle as well (which
         * would be the case if the scrollbars were underneath it as the
         * rectangle itself doesn't block the mouse.
         */
        // --- vbar
        vbar.setOrientation(Orientation.VERTICAL);
        vbar.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                event.consume();
            }
        });
        getChildren().add(vbar);

        // --- hbar
        hbar.setOrientation(Orientation.HORIZONTAL);
        hbar.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                event.consume();
            }
        });
        getChildren().add(hbar);

        // --- corner
        corner = new StackPane();
        corner.getStyleClass().setAll("corner");
        getChildren().add(corner);
    }

    private void initBinds() {
        // clipView binds
        InvalidationListener listenerX = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                updateHbar();
            }
        };
        verticalProperty().addListener(listenerX);
        hbar.valueProperty().addListener(listenerX);
        hbar.visibleProperty().addListener(listenerX);

//        ChangeListener listenerY = new ChangeListener() {
//            @Override public void handle(Bean bean, PropertyReference property) {
//                clipView.setClipY(isVertical() ? 0 : vbar.getValue());
//            }
//        };
//        addChangedListener(VERTICAL, listenerY);
//        vbar.addChangedListener(ScrollBar.VALUE, listenerY);

        /*
         * Watch the PositionMapper.position property, and when it changes
         * update the location position value within this VirtualFlow instance.
         * We don't expose the PositionMapper instance, but we do expose this
         * position property externally.
         */
        mapper.positionProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                setPosition(mapper.getPosition());
            }
        });

        super.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldHeight, Number newHeight) {
                // Fix for RT-8480, where the VirtualFlow does not show its content
                // after changing size to 0 and back.
                if (oldHeight.doubleValue() == 0 && newHeight.doubleValue() > 0) {
                    recreateCells();
                }
            }
        });
    }

    void updateHbar() {
        // Bring the clipView.clipX back to 0 if control is vertical or
        // the hbar isn't visible (fix for RT-11666)
        if (! isVisible() || getScene() == null) return;

        if (isVertical()) {
            if (hbar.isVisible()) {
                clipView.setClipX(hbar.getValue());
            } else {
                // all cells are now less than the width of the flow,
                // so we should shift the hbar/clip such that
                // everything is visible in the viewport.
                clipView.setClipX(0);
                hbar.setValue(0);
            }
        }
    }

    /***************************************************************************
     *                                                                         *
     *                          Layout Functionality                           *
     *                                                                         *
     **************************************************************************/

    /**
     * Overridden to implement somewhat more efficient support for layout. The
     * VirtualFlow can generally be considered as being unmanaged, in that
     * whenever the position changes, or other such things change, we need
     * to perform a layout but there is no reason to notify the parent. However
     * when things change which may impact the preferred size (such as
     * vertical, createCell, and configCell) then we need to notify the
     * parent.
     */
    @Override public void requestLayout() {
        if (getScene() != null && !isNeedsLayout()) {
            getScene().addToDirtyLayoutList(this);
            setNeedsLayout(true);
        }
    }

    @Override protected void layoutChildren() {
       if (needsRecreateCells) {
            maxPrefBreadth = -1;
            lastWidth = -1;
            lastHeight = -1;
            releaseCell(accumCell);
            accumCell = null;
            accumCellParent.getChildren().clear();
            addAllToPile();
            pile.clear();
            needsRecreateCells = false;
        } 
        
        if (needsReconfigureCells) {
            maxPrefBreadth = -1;
            lastWidth = -1;
            lastHeight = -1;
            needsReconfigureCells = false;
        }

        // if the width and/or height is 0, then there is no point doing
        // any of this work. In particular, this can happen during startup
        if (getWidth() <= 0 || getHeight() <= 0) {
            addAllToPile();
            hbar.setVisible(false);
            vbar.setVisible(false);
            corner.setVisible(false);
            return;
        }
        
        // we check if any of the cells in the cells list need layout. This is a
        // sign that they are perhaps animating their sizes. Without this check,
        // we may not perform a layout here, meaning that the cell will likely
        // 'jump' (in height normally) when the user drags the virtual thumb as
        // that is the first time the layout would occur otherwise.
        Cell cell;
        boolean cellNeedsLayout = false;
        for (int i = 0; i < cells.size(); i++) {
            cell = cells.get(i);
            cellNeedsLayout = cell.isNeedsLayout();
            if (cellNeedsLayout) break;
        }
        cell = null;
        
        IndexedCell firstCell = getFirstVisibleCell();

        // If no cells need layout, we check other criteria to see if this 
        // layout call is even necessary. If it is found that no layout is 
        // needed, we just punt.
        if (! cellNeedsLayout) {
            boolean cellSizeChanged = false;
            if (firstCell != null) {
                double breadth = getCellBreadth(firstCell);
                double length = getCellLength(firstCell);
                cellSizeChanged = (breadth != lastCellBreadth) || (length != lastCellLength);
                lastCellBreadth = breadth;
                lastCellLength = length;
            }

            if (getWidth() == lastWidth &&
                getHeight() == lastHeight &&
                getCellCount() == lastCellCount &&
                isVertical() == lastVertical &&
                getPosition() == lastPosition &&
                ! cellSizeChanged)
            {
                // TODO this happens to work around the problem tested by
                // testCellLayout_LayoutWithoutChangingThingsUsesCellsInSameOrderAsBefore
                // but isn't a proper solution. Really what we need to do is, when
                // laying out cells, we need to make sure that if a cell is pressed
                // AND we are doing a full rebuild then we need to make sure we
                // use that cell in the same physical location as before so that
                // it gets the mouse release event.
                return;
            }
        }

//        layingOut = true;

        /*
         * This function may get called under a variety of circumstances.
         * It will determine what has changed from the last time it was laid
         * out, and will then take one of several execution paths based on
         * what has changed so as to perform minimal layout work and also to
         * give the expected behavior. One or more of the following may have
         * happened:
         *
         *  1) width/height has changed
         *      - If the width and/or height has been reduced (but neither of
         *        them has been expanded), then we simply have to reposition and
         *        resize the scroll bars
         *      - If the width (in the vertical case) has expanded, then we
         *        need to resize the existing cells and reposition and resize
         *        the scroll bars
         *      - If the height (in the vertical case) has expanded, then we
         *        need to resize and reposition the scroll bars and add
         *        any trailing cells
         *
         *  2) cell count has changed
         *      - If the number of cells is bigger, or it is smaller but not
         *        so small as to move the position then we can just update the
         *        cells in place without performing layout and update the
         *        scroll bars.
         *      - If the number of cells has been reduced and it affects the
         *        position, then move the position and rebuild all the cells
         *        and update the scroll bars
         *
         *  3) size of the cell has changed
         *      - If the size changed in the virtual direction (ie: height
         *        in the case of vertical) then layout the cells, adding
         *        trailing cells as necessary and updating the scroll bars
         *      - If the size changed in the non virtual direction (ie: width
         *        in the case of vertical) then simply adjust the widths of
         *        the cells as appropriate and adjust the scroll bars
         *
         *  4) vertical changed, cells is empty, maxPrefBreadth == -1, etc
         *      - Full rebuild.
         *
         * Each of the conditions really resolves to several of a handful of
         * possible outcomes:
         *  a) reposition & rebuild scroll bars
         *  b) resize cells in non-virtual direction
         *  c) add trailing cells
         *  d) update cells
         *  e) resize cells in the virtual direction
         *  f) all of the above
         *
         * So this function first determines what outcomes need to occur, and
         * then will execute all the ones that really need to happen. Every code
         * path ends up touching the "reposition & rebuild scroll bars" outcome,
         * so that one will be executed every time.
         */
        boolean needTrailingCells = false;
        boolean rebuild = cellNeedsLayout ||
                isVertical() != lastVertical ||
                cells.isEmpty() ||
                maxPrefBreadth == -1 ||
                getPosition() != lastPosition ||
                getCellCount() != lastCellCount;

        if (! rebuild) {
            if ((isVertical() && getHeight() < lastHeight) || (! isVertical() && getWidth() < lastWidth)) {
                // resized in the non-virtual direction
                rebuild = true;
            } else if ((isVertical() && getHeight() > lastHeight) || (! isVertical() && getWidth() > lastWidth)) {
                // resized in the virtual direction
                needTrailingCells = true;
            }
        }

        // we update the viewport both here and after the following code.
        // Without this it is possible to run in to the following issues:
        // 1) The breadth bar may not show when a long item is added
        // 2) The breadth bar may disappear when another item is added.
        //    This is due to maxPrefBreadth being reset in reconfigureCells()
        // 3) The viewportLength being incorrectly set to not take into account
        //    the hbar protruding out and taking vertical space.
        updateViewport();

        // Get the index of the "current" cell
        int currentIndex = mapper.computeCurrentIndex();
        if (lastCellCount != getCellCount()) {
            // The cell count has changed. We want to keep the viewport
            // stable if possible. If position was 0 or 1, we want to keep
            // the position in the same place. If the new cell count is >=
            // the currentIndex, then we will adjust the position to be 1.
            // Otherwise, our goal is to leave the index of the cell at the
            // top consistent, with the same translation etc.
            if (getPosition() == 0 || getPosition() == 1) {
                // Update the item count
                mapper.setItemCount(getCellCount());
            } else if (currentIndex >= getCellCount()) {
                mapper.adjustPosition(1.0f);
                mapper.setItemCount(getCellCount());
            } else if (firstCell != null) {
                double firstCellOffset = getCellPosition(firstCell);
                int firstCellIndex = firstCell.getIndex();
                mapper.setItemCount(getCellCount());
                if (firstCell != null) {
                    mapper.adjustPositionToIndex(firstCellIndex);
                    double viewportTopToCellTop = -mapper.computeOffsetForCell(firstCellIndex);
                    mapper.adjustByPixelAmount(viewportTopToCellTop - firstCellOffset);
                }
            }

            // Update the current index
            currentIndex = mapper.computeCurrentIndex();
        }

        if (rebuild) {
            // Start by dumping all the cells into the pile
            addAllToPile();
            // The distance from the top of the viewport to the top of the
            // cell for the current index.
            double offset = -mapper.computeViewportOffset(mapper.getPosition());
            // Add all the leading and trailing cells (the call to add leading
            // cells will add the current cell as well -- that is, the one that
            // represents the current position on the mapper).
            addLeadingCells(currentIndex, offset);
            // Force filling of space with empty cells if necessary
            addTrailingCells(true);
        } else if (needTrailingCells) {
            addTrailingCells(true);
        }

        updateViewport();

        lastWidth = getWidth();
        lastHeight = getHeight();
        lastCellCount = getCellCount();
        lastVertical = isVertical();
        lastPosition = getPosition();
//        layingOut = false;
    }

    /**
     * Adds all the cells prior to and including the given currentIndex, until
     * no more can be added without falling off the flow. The startOffset
     * indicates the distance from the leading edge (top) of the viewport to
     * the leading edge (top) of the currentIndex.
     */
    private void addLeadingCells(int currentIndex, double startOffset) {
        // The offset will keep track of the distance from the top of the
        // viewport to the top of the current index. We will increment it
        // as we lay out leading cells.
        double offset = startOffset;
        // The index is the absolute index of the cell being laid out
        int index = currentIndex;

        // Offset should really be the bottom of the current index
        boolean first = true; // first time in, we just fudge the offset and let
                              // it be the top of the current index then redefine
                              // it as the bottom of the current index thereafter
        // while we have not yet laid out so many cells that they would fall
        // off the flow, we will continue to create and add cells. The
        // offset is our indication of whether we can lay out additional
        // cells. If the offset is ever < 0, except in the case of the very
        // first cell, then we must quit.
        IndexedCell cell = null;

        while (index >= 0 && (offset > 0 || first)) {
            cell = getAvailableCell(index);
            setCellIndex(cell, index);
            resizeCellSize(cell); // resize must be after config
            cells.addFirst(cell);

            // A little gross but better than alternatives because it reduces
            // the number of times we have to update a cell or compute its
            // size. The first time into this loop "offset" is actually the
            // top of the current index. On all subsequent visits, it is the
            // bottom of the current index.
            if (first) {
                first = false;
            } else {
                offset -= getCellLength(cell);
            }

            // Position the cell, and update the maxPrefBreadth variable as we go.
            positionCell(cell, offset);
            maxPrefBreadth = Math.max(maxPrefBreadth, getCellBreadth(cell));
            cell.setVisible(true);
            --index;
        }

        // There are times when after laying out the cells we discover that
        // the top of the first cell which represents index 0 is below the top
        // of the viewport. In these cases, we have to adjust the cells up
        // and reset the mapper position. This might happen when items got
        // removed at the top or when the viewport size increased.
        cell = cells.getFirst();
        int firstIndex = cell.getIndex();
        double firstCellPos = getCellPosition(cell);
        if (firstIndex == 0 && firstCellPos > 0) {
            mapper.adjustPosition(0.0f);
            offset = 0;
            for (int i = 0; i < cells.size(); i++) {
                cell = cells.get(i);
                positionCell(cell, offset);
                offset += getCellLength(cell);
            }
        }
    }

    /**
     * Adds all the trailing cells that come <em>after</em> the last index in
     * the cells ObservableList.
     */
    private boolean addTrailingCells(boolean fillEmptyCells) {
        // If cells is empty then addLeadingCells bailed for some reason and
        // we're hosed, so just punt
        if (cells.isEmpty()) return false;

        // While we have not yet laid out so many cells that they would fall
        // off the flow, so we will continue to create and add cells. When the
        // offset becomes greater than the width/height of the flow, then we
        // know we cannot add any more cells.
        IndexedCell startCell = cells.getLast();
        double offset = getCellPosition(startCell) + getCellLength(startCell);
        int index = startCell.getIndex() + 1;
        boolean filledWithNonEmpty = index <= getCellCount();

        while (offset < viewportLength) {
            if (index >= getCellCount()) {
                if (offset < viewportLength) filledWithNonEmpty = false;
                if (! fillEmptyCells) return filledWithNonEmpty;
            }
            IndexedCell cell = getAvailableCell(index);
            setCellIndex(cell, index);
            resizeCellSize(cell); // resize happens after config!
            cells.addLast(cell);

            // Position the cell and update the max pref
            positionCell(cell, offset);
            maxPrefBreadth = Math.max(maxPrefBreadth, getCellBreadth(cell));

            offset += getCellLength(cell);
            cell.setVisible(true);
            ++index;
        }

        // Discover whether the first cell coincides with index #0. If after
        // adding all the trailing cells we find that a) the first cell was
        // not index #0 and b) there are trailing cells, then we have a
        // problem. We need to shift all the cells down and add leading cells,
        // one at a time, until either the very last non-empty cells is aligned
        // with the bottom OR we have laid out cell index #0 at the first
        // position.
        IndexedCell firstCell = cells.getFirst();
        index = firstCell.getIndex();
        IndexedCell lastNonEmptyCell = getLastVisibleCell();
        double start = getCellPosition(firstCell);
        double end = getCellPosition(lastNonEmptyCell) + getCellLength(lastNonEmptyCell);
        if ((index != 0 || (index == 0 && start < 0)) && fillEmptyCells &&
                lastNonEmptyCell != null &&lastNonEmptyCell.getIndex() == getCellCount() - 1 && end < viewportLength) {

            double prospectiveEnd = end;
            double distance = viewportLength - end;
            while (prospectiveEnd < viewportLength && index != 0 && (-start) < distance) {
                index--;
                IndexedCell cell = getAvailableCell(index);
                setCellIndex(cell, index);
                resizeCellSize(cell); // resize must be after config
                cells.addFirst(cell);
                double cellLength = getCellLength(cell);
                start -= cellLength;
                prospectiveEnd += cellLength;
                positionCell(cell, start);
                maxPrefBreadth = Math.max(maxPrefBreadth, getCellBreadth(cell));
                cell.setVisible(true);
            }

            // The amount by which to translate the cells down
            firstCell = cells.getFirst();
            start = getCellPosition(firstCell);
            double delta = viewportLength - end;
            if (firstCell.getIndex() == 0 && delta > (-start)) {
                delta = (-start);
            }
            // Move things
            for (int i = 0; i < cells.size(); i++) {
                IndexedCell cell = cells.get(i);
                positionCell(cell, getCellPosition(cell) + delta);
            }

            // Check whether the first cell, subsequent to our adjustments, is
            // now index #0 and aligned with the top. If so, change the position
            // to be at 0 instead of 1.
            start = getCellPosition(firstCell);
            if (firstCell.getIndex() == 0 && start == 0) {
                mapper.adjustPosition(0);
            } else if (getPosition() != 1) {
                mapper.adjustPosition(1);
            }
        }

        return filledWithNonEmpty;
    }

    private void updateViewport() {
        // Initialize the viewportLength and viewportBreadth to match the
        // width/height of the flow
        double lastViewportLength = viewportLength;
        viewportLength = snapSize(isVertical() ? getHeight() : getWidth());
        viewportBreadth = snapSize(isVertical() ? getWidth() : getHeight());

        // Assign the hbar and vbar to the breadthBar and lengthBar so as
        // to make some subsequent calculations easier.
        VirtualScrollBar breadthBar = isVertical() ? hbar : vbar;
        VirtualScrollBar lengthBar = isVertical() ? vbar : hbar;
        double breadthBarLength = snapSize(isVertical() ? hbar.prefHeight(-1) : vbar.prefWidth(-1));
        double lengthBarBreadth = snapSize(isVertical() ? vbar.prefWidth(-1) : hbar.prefHeight(-1));

        // If there has been a switch between the virtualized bar, then we
        // will want to do some stuff TODO.
        breadthBar.setVirtual(false);
        lengthBar.setVirtual(true);

        // We need to determine whether the hbar and vbar are necessary. If the
        // flow has been scrolled in the virtual direction, then we know for
        // certain that the virtual scroll bar is required. If the
        // maxPrefBreadth is already greater than the viewport, then we know
        // we need the breadthBar as well. If neither of these two conditions
        // are met, then we need to grab the first page worth of cells and
        // compute the maxPrefBreadth and also determine if we have enough cells
        // such that it will require more than a single page.

        if (maxPrefBreadth == -1) {
            return;
        }

        // Perform a few computations used for understanding the effect of the
        // bars on the viewport dimensions. Here we tentatively decide whether
        // we need the breadth bar and the length bar.
        // The last condition here (viewportLength >= getHeight()) was added to
        // resolve the edge-case identified in RT-14350.
        boolean needLengthBar = getPosition() > 0 && (getCellCount() >= cells.size() || viewportLength >= getHeight());
        boolean needBreadthBar = maxPrefBreadth > viewportBreadth || (needLengthBar && maxPrefBreadth > (viewportBreadth - lengthBarBreadth));
//        System.out.println(maxPrefBreadth + " > " + viewportBreadth + " || " + "(" + needLengthBar + " && " + maxPrefBreadth + " > ( " + viewportBreadth + " - " + lengthBarBreadth + "))");

        // Start by optimistically deciding whether the length bar and
        // breadth bar are needed and adjust the viewport dimensions
        // accordingly. If during layout we find that one or the other of the
        // bars actually is needed, then we will perform a cleanup pass
        if (needBreadthBar) viewportLength -= breadthBarLength;
        if (needLengthBar) viewportBreadth -= lengthBarBreadth;

        // Update the viewport size based on our prospective viewportLength
        mapper.setViewportSize(viewportLength);

        breadthBar.setVisible(needBreadthBar);
        lengthBar.setVisible(needLengthBar);
//        System.out.println("breadthBar is visible: " + breadthBar.isVisible());
        updateScrollBarsAndViewport(lastViewportLength);
    }

    @Override protected void setWidth(double value) {
        super.setWidth(value);
        setNeedsLayout(true);
        requestLayout();
    }
    
    @Override protected void setHeight(double value) {
        super.setHeight(value);
        setNeedsLayout(true);
        requestLayout();
    }

    private void updateScrollBarsAndViewport(double lastViewportLength) {
        // Assign the hbar and vbar to the breadthBar and lengthBar so as
        // to make some subsequent calculations easier.
        VirtualScrollBar breadthBar = isVertical() ? hbar : vbar;
        VirtualScrollBar lengthBar = isVertical() ? vbar : hbar;
        double breadthBarLength = snapSize(isVertical() ? hbar.prefHeight(-1) : vbar.prefWidth(-1));
        double lengthBarBreadth = snapSize(isVertical() ? vbar.prefWidth(-1) : hbar.prefHeight(-1));
        
        // Now that we've laid out the cells, we may need to adjust the scroll
        // bars and update the viewport dimensions based on the bars
        // We have to do the following work twice because the first pass
        // through the loop may have made the breadth bar visible, which will
        // adjust the viewportLength, which may make the lengthBar need to
        // be visible as well.
        for (int i = 0; i < 2; i++) {
            if (! lengthBar.isVisible()) {
                // If cellCount is > than cells.size(), then we know we need the
                // length bar.
                if (getCellCount() > cells.size()) {
                    lengthBar.setVisible(true);
                } else if (getCellCount() == cells.size()) {
                    // We must check a corner case here where the cell count
                    // exactly matches the number of cells laid out. In this case,
                    // we need to check the last cell's layout position + length
                    // to determine if we need the length bar
                    IndexedCell lastCell = cells.getLast();
                    lengthBar.setVisible((getCellPosition(lastCell) + getCellLength(lastCell)) > viewportLength);
                }
                // If the bar is needed, adjust the viewportBreadth
                if (lengthBar.isVisible()) viewportBreadth -= lengthBarBreadth;
            }
            if (! breadthBar.isVisible()) {
                breadthBar.setVisible(maxPrefBreadth > viewportBreadth);
                if (breadthBar.isVisible()) viewportLength -= breadthBarLength;
            }
        }

        // Toggle visibility on the corner
        corner.setVisible(breadthBar.isVisible() && lengthBar.isVisible());

        numCellsVisibleOnScreen = 0;
        double sumCellLength = 0;
        double flowLength = (isVertical() ? getHeight() : getWidth()) -
            (breadthBar.isVisible() ? breadthBar.prefHeight(-1) : 0);
        for (int i = 0; i < cells.size(); i++) {
            IndexedCell cell = cells.get(i);
            if (cell != null && ! cell.isEmpty()) {
                sumCellLength += (isVertical() ? cell.getHeight() : cell.getWidth());
                if (sumCellLength > flowLength) {
                    break;
                }

                numCellsVisibleOnScreen++;
            }
        }

        // Now position and update the scroll bars
        if (breadthBar.isVisible()) {
            if (isVertical()) {
                hbar.resizeRelocate(0, viewportLength,
                        viewportBreadth, hbar.prefHeight(viewportBreadth));
            } else {
                vbar.resizeRelocate(viewportLength, 0,
                        vbar.prefWidth(viewportBreadth), viewportBreadth);

            }

            // There was a weird bug where the newMax would sometimes go < 0
            // when switching vertical and that would drive the min value to
            // something crazy negative.
            double newMax = Math.max(1, maxPrefBreadth - viewportBreadth);
            if (newMax != breadthBar.getMax()) {
                breadthBar.setMax(newMax);

                boolean maxed = breadthBar.getValue() != 0 && breadthBar.getMax() == breadthBar.getValue();
                if (maxed || breadthBar.getValue() > newMax) {
                    breadthBar.setValue(newMax);
                }

                breadthBar.setVisibleAmount((viewportBreadth / maxPrefBreadth) * newMax);
            }
        }

        if (lengthBar.isVisible()) {
            lengthBar.setMax(1);
            
            if (numCellsVisibleOnScreen == 0) {
                // special case to help resolve RT-17701
                lengthBar.setVisibleAmount(flowLength / sumCellLength);
            } else {
                lengthBar.setVisibleAmount(numCellsVisibleOnScreen / (float) getCellCount());
            }

            // Fix for RT-11873. If this isn't here, we can have a situation where
            // the scrollbar scrolls endlessly. This is possible when the cell
            // count grows as the user hits the maximal position on the scrollbar
            // (i.e. the list size dynamically grows as the user needs more).
            //
            // This code was commented out to resolve RT-14477 after testing
            // whether RT-11873 can be recreated. It could not, and therefore
            // for now this code will remained uncommented until it is deleted
            // following further testing.
//            if (lengthBar.getValue() == 1.0 && lastCellCount != getCellCount()) {
//                lengthBar.setValue(0.99);
//            }

            if (isVertical()) {
                double h = viewportLength;
                vbar.resizeRelocate(viewportBreadth, 0, vbar.prefWidth(h), h);
            } else {
                hbar.resizeRelocate(0, viewportBreadth, viewportLength, hbar.prefHeight(-1));
            }
        }

        if (corner.isVisible()) {
            corner.resize(vbar.getWidth(), hbar.getHeight());
            corner.relocate(hbar.getLayoutX() + hbar.getWidth(), vbar.getLayoutY() + vbar.getHeight());
        }

        clipView.resize(snapSize(isVertical() ? viewportBreadth : viewportLength),
                        snapSize(isVertical() ? viewportLength : viewportBreadth));

        // Update the viewport size based on our final viewportLength
        if (mapper.getViewportSize() != viewportLength) {
            mapper.setViewportSize(viewportLength);
        }

        // We may have adjusted the viewport length and breadth after the
        // layout due to scroll bars becoming visible. So we need to perform
        // a follow up pass and resize and shift all the cells to fit the
        // viewport. Note that the prospective viewport size is always >= the
        // final viewport size, so we don't have to worry about adding
        // cells during this cleanup phase.
        fitCells();

        // If the viewportLength becomes large enough that all cells fit
        // within the viewport, then we want to update the value to match.
        if (getPosition() != lengthBar.getValue()) {
            lengthBar.setValue(getPosition());
        }
    }

    /**
     * Adjusts the cells location and size if necessary. The breadths of all
     * cells will be adjusted to fit the viewportWidth or maxPrefBreadth, and
     * the layout position will be updated if necessary based on index and
     * offset.
     */
    private void fitCells() {
        double size = Math.max(maxPrefBreadth, viewportBreadth);
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            if (isVertical()) {
                cell.resize(size, cell.getHeight());
            } else {
                cell.resize(cell.getWidth(), size);
            }
        }
    }

    private void cull() {
        for (int i = cells.size() - 1; i >= 0; i--) {
            IndexedCell cell = cells.get(i);
            double cellSize = getCellLength(cell);
            double cellStart = getCellPosition(cell);
            double cellEnd = cellStart + cellSize;
            if (cellStart > viewportLength || cellEnd < 0) {
                addToPile(cells.remove(i));
            }
        }
    }

    /***************************************************************************
     *                                                                         *
     *                Helper functions for working with cells                  *
     *                                                                         *
     **************************************************************************/

    /**
     * Return a cell for the given index. This may be called for any cell,
     * including beyond the range defined by cellCount, in which case an
     * empty cell will be returned. The returned value should not be stored for
     * any reason.
     */
    IndexedCell getCell(int index) {
        // If there are cells, then we will attempt to get an existing cell
        if (! cells.isEmpty()) {
            // First check the cells that have already been created and are
            // in use. If this call returns a value, then we can use it
            IndexedCell cell = getVisibleCell(index);
            if (cell != null) return cell;
        }

        // check the pile
        for (int i = 0; i < pile.size(); i++) {
            IndexedCell cell = pile.get(i);
            if (cell != null && cell.getIndex() == index) {
                // Note that we don't remove from the pile: if we do it leads
                // to a severe performance decrease. This seems to be OK, as
                // getCell() is only used for cell measurement purposes.
                // pile.remove(i);
                return cell;
            }
        }

        // We need to use the accumCell and return that
        if (accumCell == null && getCreateCell() != null) {
            accumCell = getCreateCell().call(this);
            accumCellParent.getChildren().add(accumCell);
        }
        setCellIndex(accumCell, index);
        resizeCellSize(accumCell);
        return accumCell;
    }

    /**
     * After using the accum cell, it needs to be released!
     */
    private void releaseCell(IndexedCell cell) {
        if (accumCell != null && cell == accumCell) {
            accumCell.updateIndex(-1);
        }
    }

    /**
     * Compute and return the length of the cell for the given index. This is
     * called both internally when adjusting by pixels, and also at times
     * by PositionMapper (see the getItemSize callback). When called by
     * PositionMapper, it is possible that it will be called for some index
     * which is not associated with any cell, so we have to do a bit of work
     * to use a cell as a helper for computing cell size in some cases.
     */
    double getCellLength(int index) {
        IndexedCell cell = getCell(index);
        double length = getCellLength(cell);
        releaseCell(cell);
        return length;
    }

    /**
     */
    double getCellBreadth(int index) {
        IndexedCell cell = getCell(index);
        double b = getCellBreadth(cell);
        releaseCell(cell);
        return b;
    }

    /**
     * Gets the length of a specific cell
     */
    private double getCellLength(IndexedCell cell) {
        if (cell == null) return 0;

        return isVertical() ?
            cell.getLayoutBounds().getHeight()
            : cell.getLayoutBounds().getWidth();
    }

    private double getCellPrefLength(IndexedCell cell) {
        return isVertical() ?
            cell.prefHeight(-1)
            : cell.prefWidth(-1);
    }

    /**
     * Gets the breadth of a specific cell
     */
    private double getCellBreadth(Cell cell) {
        return isVertical() ?
            cell.prefWidth(-1)
            : cell.prefHeight(-1);
    }

    /**
     * Gets the layout position of the cell along the length axis
     */
    private double getCellPosition(IndexedCell cell) {
        if (cell == null) return 0;

        return isVertical() ?
            cell.getLayoutY()
            : cell.getLayoutX();
    }

    private void positionCell(IndexedCell cell, double position) {
        if (isVertical()) {
            cell.setLayoutX(0);
            cell.setLayoutY(snapSize(position));
        } else {
            cell.setLayoutX(snapSize(position));
            cell.setLayoutY(0);
        }
    }

    private void resizeCellSize(IndexedCell cell) {
        if (cell == null) return;
        if (isVertical())
            cell.resize(cell.getWidth(), cell.prefHeight(-1));
        else
            cell.resize(cell.prefWidth(-1), cell.getHeight());
    }

    private void setCellIndex(IndexedCell cell, int index) {
        if (cell == null) return;

        cell.updateIndex(index);
        if (cell.isNeedsLayout() && cell.getScene() != null) {
            cell.impl_processCSS(false);
        }
    }

    /***************************************************************************
     *                                                                         *
     *                 Helper functions for cell management                    *
     *                                                                         *
     **************************************************************************/

    /**
     * Get a cell which can be used in the layout. This function will reuse
     * cells from the pile where possible, and will create new cells when
     * necessary.
     */
    private IndexedCell getAvailableCell(int prefIndex) {
        IndexedCell cell = null;
        
        // Fix for RT-12822. We try to retrieve the cell from the pile rather
        // than just grab a random cell from the pile (or create another cell).
        for (int i = 0; i < pile.size(); i++) {
            IndexedCell _cell = pile.get(i);
            if (_cell != null && _cell.getIndex() == prefIndex) {
                pile.remove(i);
                cell = _cell;
            }
        }

        if (cell == null) {
            if (pile.size() > 0) {
                cell = pile.removeFirst();
            } else {
                cell = createCell.call(this);
            }
        }
        
        if (! sheet.getChildren().contains(cell)) {
            sheet.getChildren().add(cell);
        }
        
        return cell;
    }

    private void addAllToPile() {
        while (cells.size() > 0) {
            addToPile(cells.removeFirst());
        }
    }

    /**
     * Puts the given cell onto the pile. This is called whenever a cell has
     * fallen off the flow's start.
     */
    private void addToPile(IndexedCell cell) {
        cell.setVisible(false);
        pile.addLast(cell);
    }

    /**
     * Gets a cell for the given index if the cell has been created and laid out.
     * "Visible" is a bit of a misnomer, the cell might not be visible in the
     * viewport (it may be clipped), but does distinguish between cells that
     * have been created and are in use vs. those that are in the pile or
     * not created.
     */
    public IndexedCell getVisibleCell(int index) {
        if (cells.isEmpty()) return null;

        // check the last index
        IndexedCell lastCell = cells.getLast();
        int lastIndex = lastCell.getIndex();
        if (index == lastIndex) return lastCell;

        // check the first index
        IndexedCell firstCell = cells.getFirst();
        int firstIndex = firstCell.getIndex();
        if (index == firstIndex) return firstCell;

        // if index is > firstIndex and < lastIndex then we can get the index
        if (index > firstIndex && index < lastIndex) {
            return cells.get(index - firstIndex);
        }

        // there is no visible cell for the specified index
        return null;
    }

    /**
     * Locates and returns the last non-empty IndexedCell that is currently
     * partially or completely visible. This function may return null if there
     * are no cells, or if the viewport length is 0.
     */
    public IndexedCell getLastVisibleCell() {
        if (cells.isEmpty() || viewportLength <= 0) return null;

        IndexedCell cell;
        for (int i = cells.size() - 1; i >= 0; i--) {
            cell = cells.get(i);
            if (! cell.isEmpty()) {
                return cell;
            }
        }

        return null;
    }

    /**
     * Locates and returns the first non-empty IndexedCell that is partially or
     * completely visible. This really only ever returns null if there are no
     * cells or the viewport length is 0.
     */
    public IndexedCell getFirstVisibleCell() {
        if (cells.isEmpty() || viewportLength <= 0) return null;
        IndexedCell cell = cells.getFirst();
        return cell.isEmpty() ? null : cell;
    }

    public IndexedCell getLastVisibleCellWithinViewPort() {
        if (cells.isEmpty() || viewportLength <= 0) return null;

        IndexedCell cell;
        for (int i = cells.size() - 1; i >= 0; i--) {
            cell = cells.get(i);
            if (cell.isEmpty()) continue;

            if (cell.getLayoutY() < getHeight()) {
                return cell;
            }
        }

        return null;
    }

    public IndexedCell getFirstVisibleCellWithinViewPort() {
        if (cells.isEmpty() || viewportLength <= 0) return null;

        IndexedCell cell;
        for (int i = 0; i < cells.size(); i++) {
            cell = cells.get(i);
            if (cell.isEmpty()) continue;

            if (isVertical() && cell.getLayoutY() + cell.getHeight() > 0) {
                return cell;
            } else if (! isVertical() && cell.getLayoutX() + cell.getWidth() > 0) {
                return cell;
            }
        }

        return null;
    }

    /**
     * Adjust the position of cells so that the specified cell
     * will be positioned at the start of the viewport. The given cell must
     * already be "live". This is bad public API!
     */
    public void showAsFirst(IndexedCell firstCell) {
        if (firstCell != null) {
            adjustPixels(getCellPosition(firstCell));
        }
    }

    /**
     * Adjust the position of cells so that the specified cell
     * will be positioned at the end of the viewport. The given cell must
     * already be "live". This is bad public API!
     */
    public void showAsLast(IndexedCell lastCell) {
        if (lastCell != null) {
            adjustPixels(getCellPosition(lastCell) + getCellLength(lastCell) - viewportLength);
        }
    }

    /**
     * Adjusts the cells such that the selected cell will be fully visible in
     * the viewport (but only just).
     */
    public void show(IndexedCell cell) {
        if (cell != null) {
            double start = getCellPosition(cell);
            double length = getCellLength(cell);
            double end = start + length;
            if (start < 0) {
                adjustPixels(start);
            } else if (end > viewportLength) {
                adjustPixels(end - viewportLength);
            }
        }
    }

    public void show(int index) {
        IndexedCell cell = getVisibleCell(index);
        if (cell != null) {
            show(cell);
        } else {
            // See if the previous index is a visible cell
            IndexedCell prev = getVisibleCell(index - 1);
            if (prev != null) {
                // Need to add a new cell and then we can show it
//                layingOut = true;
                cell = getAvailableCell(index);
                setCellIndex(cell, index);
                resizeCellSize(cell); // resize must be after config
                cells.addLast(cell);
                positionCell(cell, getCellPosition(prev) + getCellLength(prev));
                maxPrefBreadth = Math.max(maxPrefBreadth, getCellBreadth(cell));
                cell.setVisible(true);
                show(cell);
//                layingOut = false;
                return;
            }
            // See if the next index is a visible cell
            IndexedCell next = getVisibleCell(index + 1);
            if (next != null) {
//                layingOut = true;
                cell = getAvailableCell(index);
                setCellIndex(cell, index);
                resizeCellSize(cell); // resize must be after config
                cells.addFirst(cell);
                positionCell(cell, getCellPosition(next) - getCellLength(cell));
                maxPrefBreadth = Math.max(maxPrefBreadth, getCellBreadth(cell));
                cell.setVisible(true);
                show(cell);
//                layingOut = false;
                return;
            }
        }
    }

    public void scrollTo(int index) {
        double offset = index * getCellLength(index);
        mapper.adjustByPixelChunk(offset);
        addAllToPile();
        requestLayout();        
    }
    
    /**
     * Given a delta value representing a number of pixels, this method attempts
     * to move the VirtualFlow in the given direction (positive is down/right,
     * negative is up/left) the given number of pixels. It returns the number of
     * pixels actually moved.
     */
    public double adjustPixels(double delta) {
        // Short cut this method for cases where nothing should be done
        if (delta == 0) return 0;

        if ((isVertical() && ! vbar.isVisible()) || (! isVertical() && ! hbar.isVisible())) return 0;
        if (mapper.getPosition() == 0.0f && delta < 0) return 0;
        if (mapper.getPosition() == 1.0f && delta > 0) return 0;

        double pos = mapper.getPosition();
        mapper.adjustByPixelAmount(delta);
        if (pos == mapper.getPosition()) {
            // The pos hasn't changed, there's nothing to do. This is likely
            // to occur when we hit either extremity
            return 0;
        }

//        layingOut = true;
        // Now move stuff around. Translating by pixels fundamentally means
        // moving the cells by the delta. However, after having
        // done that, we need to go through the cells and see which cells,
        // after adding in the translation factor, now fall off the viewport.
        // Also, we need to add cells as appropriate to the end (or beginning,
        // depending on the direction of travel).
        //
        // One simplifying assumption (that had better be true!) is that we
        // will only make it this far in the function if the virtual scroll
        // bar is visible. Otherwise, we never will pixel scroll. So as we go,
        // if we find that the maxPrefBreadth exceeds the viewportBreadth,
        // then we will be sure to show the breadthBar and update it
        // accordingly.
        if (cells.size() > 0) {
            for (int i = 0; i < cells.size(); i++) {
                IndexedCell cell = cells.get(i);
                positionCell(cell, getCellPosition(cell) - delta);
            }

            // Add any necessary leading cells
            IndexedCell firstCell = cells.getFirst();
            int firstIndex = firstCell.getIndex();
            double prevIndexSize = getCellLength(firstIndex - 1);
            addLeadingCells(firstIndex - 1, getCellPosition(firstCell) - prevIndexSize);

            // Starting at the tail of the list, loop adding cells until
            // all the space on the table is filled up. We want to make
            // sure that we DO NOT add empty trailing cells (since we are
            // in the full virtual case and so there are no trailing empty
            // cells).
            if (! addTrailingCells(false)) {
                // Reached the end, but not enough cells to fill up to
                // the end. So, remove the trailing empty space, and translate
                // the cells down
                IndexedCell lastCell = getLastVisibleCell();
                double lastCellSize = getCellLength(lastCell);
                double cellEnd = getCellPosition(lastCell) + lastCellSize;
                if (cellEnd < viewportLength) {
                    // Reposition the nodes
                    double emptySize = viewportLength - cellEnd;
                    for (int i = 0; i < cells.size(); i++) {
                        IndexedCell cell = cells.get(i);
                        positionCell(cell, getCellPosition(cell) + emptySize);
                    }
                    mapper.adjustPosition(1.0f);
                }
            }

            // Now throw away any cells that don't fit
            cull();
        }

        // Finally, update the scroll bars
        updateScrollBarsAndViewport(viewportLength);
        lastPosition = getPosition();
//        layingOut = false;

        // notify
        return delta; // TODO fake
    }

    private boolean needsReconfigureCells = false;
    private boolean needsRecreateCells = false;
    
    public void reconfigureCells() {
        needsReconfigureCells = true;
        requestLayout();
    }

    public void recreateCells() {
        needsRecreateCells = true;
        requestLayout();
    }

    private static final double GOLDEN_RATIO_MULTIPLIER = 0.618033987;

    private double getPrefBreadth(double oppDimension) {
        double max = getMaxCellWidth(10);

        // This primarily exists for the case where we do not want the breadth
        // to grow to ensure a golden ratio between width and height (for example,
        // when a ListView is used in a ComboBox - the width should not grow
        // just because items are being added to the ListView)
        if (oppDimension > -1) {
            double prefLength = getPrefLength();
            max = Math.max(max, prefLength * GOLDEN_RATIO_MULTIPLIER);
        }
        
        return max;
    }

    private double getPrefLength() {
        double sum = 0.0;
        int rows = Math.min(10, getCellCount());
        for (int i = 0; i < rows; i++) {
            sum += getCellLength(i);
        }
        return sum;
    }

    @Override protected double computePrefWidth(double height) {
        double w = isVertical() ? getPrefBreadth(height) : getPrefLength();
        return w + vbar.prefWidth(-1);
    }

    @Override protected double computePrefHeight(double width) {
        double h = isVertical() ? getPrefLength() : getPrefBreadth(width);
        return h + hbar.prefHeight(-1);
    }
    
    double getMaxCellWidth(int rowsToCount) {
        double max = 0.0;
        int rows = rowsToCount == -1 ? getCellCount() : Math.min(rowsToCount, getCellCount());
        for (int i = 0; i < rows; i++) {
            max = Math.max(max, getCellBreadth(i));
        }
        return max;
    }

    /**
     * A simple extension to Region that ensures that anything wanting to flow
     * outside of the bounds of the Region is clipped.
     */
    static class ClippedContainer extends Region {
        private VirtualFlow flow;

        /**
         * The Node which is embedded within this {@code ClipView}.
         */
        private Node node;
        public Node getNode() { return this.node; }
        public void setNode(Node n) {
            this.node = n;

            contentGroup.getChildren().clear();
            contentGroup.getChildren().add(node);
        }

        public void setClipX(double clipX) {
            contentGroup.setLayoutX(-clipX);
        }

        public void setClipY(double clipY) {
            contentGroup.setLayoutY(-clipY);
        }

        private final Group contentGroup;
        private final Rectangle clipRect;

        public ClippedContainer(final VirtualFlow flow) {
            if (flow == null) {
                throw new IllegalArgumentException("VirtualFlow can not be null");
            }

            this.flow = flow;
//            setManaged(false);

            // clipping
            clipRect = new Rectangle();
            clipRect.setSmooth(false);
            setClip(clipRect);
            // --- clipping

            // We create a contentGroup to store the content. This makes it easier
            // to translate its position (using clipX/clipY), which keeping
            // the actual clipRect located in the same location
            contentGroup = new Group();
            getChildren().add(contentGroup);

            super.widthProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    clipRect.setWidth(getWidth());
                }
            });
            super.heightProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    clipRect.setHeight(getHeight());
                }
            });
        }

//        /*
//         * TODO
//         * This code does have it's uses, but for the life of me I can't remember
//         * what they are. I'm leaving the code here for now until we get unit
//         * tests back up - at which point we can see how the tests go with this
//         * code missing, and in place.
//         */
//        @Override public void requestLayout() {
////            if (flow.layingOut) {
////                super.requestLayout();
////                return;
////            }
//
//            if (flow == null) {
//                return;
//            }
//            
//            ArrayLinkedList<IndexedCell> cells = flow.cells;
//            if (cells.size() > 0) {
////                flow.layingOut = true;
//
//                // we reset the maxPrefBreadth here so that if all cell widths
//                // have reduced, we won't keep the scrollbar unnecessarily.
//                flow.maxPrefBreadth = 0.0;
//
//                double offset = flow.getCellPosition(cells.getFirst());
//                for (int i = 0; i < cells.size(); i++) {
//                    IndexedCell cell = cells.get(i);
//                    flow.positionCell(cell, offset);
//                    cell.impl_processCSS(true);
//                    double prefLength = flow.getCellPrefLength(cell);
//                    offset += prefLength;
//                    flow.resizeCellSize(cell);
//                    flow.maxPrefBreadth = Math.max(flow.maxPrefBreadth, flow.getCellBreadth(cell));
//                }
//
//                // Add any necessary leading cells
//                IndexedCell firstCell = cells.getFirst();
//                int firstIndex = firstCell.getIndex();
//                double prevIndexSize = flow.getCellLength(firstIndex - 1);
//                flow.addLeadingCells(firstIndex - 1, flow.getCellPosition(firstCell) - prevIndexSize);
//
//                // Starting at the tail of the list, loop adding cells until
//                // all the space on the table is filled up. We want to make
//                // sure that we DO NOT add empty trailing cells (since we are
//                // in the full virtual case and so there are no trailing empty
//                // cells).
//                flow.addTrailingCells(true);
//
//                // Reached the end, but not enough cells to fill up to
//                // the end. So, remove the trailing empty space, and translate
//                // the cells down
//                IndexedCell lastCell = flow.getLastVisibleCell();
//                double lastCellSize = flow.getCellLength(lastCell);
//                double lastCellEnd = flow.getCellPosition(lastCell) + lastCellSize;
//                if (flow.getPosition() == 1.0f && lastCellEnd > flow.viewportLength && lastCell == cells.getLast()) {
//                    // Reposition the nodes
//                    double tooFar = flow.viewportLength - lastCellEnd;
//                    for (int i = 0; i < cells.size(); i++) {
//                        IndexedCell cell = cells.get(i);
//                        flow.positionCell(cell, flow.getCellPosition(cell) + tooFar);
//                    }
//                }
//
//                // Now throw away any cells that don't fit
//                flow.cull();
//
//                // Finally, update the scroll bars
//                if (flow.getPosition() == 0.0f && lastCell != cells.getLast()) {
//                    // hide the dang virtual scroll bar
//                    VirtualScrollBar lengthBar = flow.isVertical() ? flow.vbar : flow.hbar;
//                    if (lengthBar.isVisible()) {
//                        flow.viewportBreadth += flow.isVertical() ? flow.vbar.getWidth() : flow.hbar.getHeight();
//                        lengthBar.setVisible(false);
//                    }
//                }
//                flow.updateScrollBarsAndViewport(flow.viewportLength);
//                flow.lastPosition = flow.getPosition();
////                flow.layingOut = false;
//            }
//
//            super.requestLayout();
//        }
    }

    /**
     * A List-like implementation that is exceedingly efficient for the purposes
     * of the VirtualFlow. Typically there is not much variance in the number of
     * cells -- it is always some reasonably consistent number. Yet for efficiency
     * in code, we like to use a linked list implementation so as to append to
     * start or append to end. However, at times when we need to iterate, LinkedList
     * is expensive computationally as well as requiring the construction of
     * temporary iterators.
     * <p>
     * This linked list like implementation is done using an array. It begins by
     * putting the first item in the center of the allocated array, and then grows
     * outward (either towards the first or last of the array depending on whether
     * we are inserting at the head or tail). It maintains an index to the start
     * and end of the array, so that it can efficiently expose iteration.
     * <p>
     * This class is package private solely for the sake of testing.
     */
    static class ArrayLinkedList<T> {
        /**
         * The array list backing this class. We default the size of the array
         * list to be fairly large so as not to require resizing during normal
         * use, and since that many ArrayLinkedLists won't be created it isn't
         * very painful to do so.
         */
        private final ArrayList<T> array;

        private int firstIndex = -1;
        private int lastIndex = -1;

        public ArrayLinkedList() {
            array = new ArrayList<T>(50);

            for (int i = 0; i < 50; i++) {
                array.add(null);
            }
        }

        public T getFirst() {
            return firstIndex == -1 ? null : array.get(firstIndex);
        }

        public T getLast() {
            return lastIndex == -1 ? null : array.get(lastIndex);
        }

        public void addFirst(T cell) {
            // if firstIndex == -1 then that means this is the first item in the
            // list and we need to initialize firstIndex and lastIndex
            if (firstIndex == -1) {
                firstIndex = lastIndex = array.size() / 2;
                array.set(firstIndex, cell);
            } else if (firstIndex == 0) {
                // we're already at the head of the array, so insert at position
                // 0 and then increment the lastIndex to compensate
                array.add(0, cell);
                lastIndex++;
            } else {
                // we're not yet at the head of the array, so insert at the
                // firstIndex - 1 position and decrement first position
                array.set(--firstIndex, cell);
            }
        }

        public void addLast(T cell) {
            // if lastIndex == -1 then that means this is the first item in the
            // list and we need to initialize the firstIndex and lastIndex
            if (firstIndex == -1) {
                firstIndex = lastIndex = array.size() / 2;
                array.set(lastIndex, cell);
            } else if (lastIndex == array.size() - 1) {
                // we're at the end of the array so need to "add" so as to force
                // the array to be expanded in size
                array.add(++lastIndex, cell);
            } else {
                array.set(++lastIndex, cell);
            }
        }

        public int size() {
            return firstIndex == -1 ? 0 : lastIndex - firstIndex + 1;
        }

        public boolean isEmpty() {
            return firstIndex == -1;
        }

        public T get(int index) {
            if (index > lastIndex - firstIndex || index < 0) {
                throw new java.lang.ArrayIndexOutOfBoundsException();
            }

            return array.get(firstIndex + index);
        }

        public void clear() {
            for (int i = 0; i < array.size(); i++) {
                array.set(i, null);
            }

            firstIndex = lastIndex = -1;
        }

        public T removeFirst() {
            if (isEmpty()) return null;
            return remove(0);
        }

        public T removeLast() {
            if (isEmpty()) return null;
            return remove(lastIndex - firstIndex);
        }

        public T remove(int index) {
            if (index > lastIndex - firstIndex || index < 0) {
                throw new java.lang.ArrayIndexOutOfBoundsException();
            }

            // if the index == 0, then we're removing the first
            // item and can simply set it to null in the array and increment
            // the firstIndex unless there is only one item, in which case
            // we have to also set first & last index to -1.
            if (index == 0) {
                T cell = array.get(firstIndex);
                array.set(firstIndex, null);
                if (firstIndex == lastIndex) {
                    firstIndex = lastIndex = -1;
                } else {
                    firstIndex++;
                }
                return cell;
            } else if (index == lastIndex - firstIndex) {
                // if the index == lastIndex - firstIndex, then we're removing the
                // last item and can simply set it to null in the array and
                // decrement the lastIndex
                T cell = array.get(lastIndex);
                array.set(lastIndex--, null);
                return cell;
            } else {
                // if the index is somewhere in between, then we have to remove the
                // item and decrement the lastIndex
                T cell = array.get(firstIndex + index);
                array.set(firstIndex + index, null);
                for (int i = (firstIndex + index + 1); i <= lastIndex; i++) {
                    array.set(i - 1, array.get(i));
                }
                array.set(lastIndex--, null);
                return cell;
            }
        }
    }
}
