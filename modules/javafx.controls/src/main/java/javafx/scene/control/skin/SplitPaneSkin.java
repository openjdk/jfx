/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.scene.control.ListenerHelper;

/**
 * Default skin implementation for the {@link SplitPane} control.
 *
 * @see SplitPane
 * @since 9
 */
public class SplitPaneSkin extends SkinBase<SplitPane> {

    private static final double EPSILON = 1e-10;

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private ObservableList<Content> contentRegions;
    private ObservableList<ContentDivider> contentDividers;
    private ListenerHelper contentDividerListenerHelper;
    private boolean horizontal;
    private double contentWidth = -1;
    private double contentHeight = -1;

    /**
     * Flag which is used to determine whether we need to request layout when a divider position changed or not.
     * E.g. We don't want to request layout when we are changing the divider position in
     * {@link #layoutChildren(double, double, double, double)} since we are currently doing the layout.
     * See also: JDK-8277122
     */
    private boolean duringLayout;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new SplitPaneSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public SplitPaneSkin(final SplitPane control) {
        super(control);

        horizontal = getSkinnable().getOrientation() == Orientation.HORIZONTAL;

        contentRegions = FXCollections.<Content>observableArrayList();
        contentDividers = FXCollections.<ContentDivider>observableArrayList();

        int index = 0;
        for (Node n: getSkinnable().getItems()) {
            addContent(index++, n);
        }
        initializeContentListener();

        addDividers();

        ListenerHelper lh = ListenerHelper.get(this);
        lh.addChangeListener(control.orientationProperty(), (v) -> {
            this.horizontal = getSkinnable().getOrientation() == Orientation.HORIZONTAL;
            this.previousSize = -1;
            this.contentWidth = -1;
            this.contentHeight = -1;
            for (ContentDivider c: contentDividers) {
                c.setGrabberStyle(horizontal);
            }
            getSkinnable().requestLayout();
        });
        lh.addChangeListener(control.widthProperty(), (v) -> getSkinnable().requestLayout());
        lh.addChangeListener(control.heightProperty(), (v) -> getSkinnable().requestLayout());
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    @Override
    public void dispose() {
        removeAllDividers();

        super.dispose();
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        contentWidth = w;
        contentHeight = h;
        final double mainSize = horizontal ? w : h;

        if (mainSize == 0 || contentRegions.isEmpty()) {
            return;
        }

        if (!contentDividers.isEmpty() && previousSize != -1 && previousSize != mainSize) {
            //This algorithm adds/subtracts a little to each panel on every resize
            List<Content> resizeList = new ArrayList<>();
            for (Content c: contentRegions) {
                if (c.isResizableWithParent()) {
                    resizeList.add(c);
                }
            }

            double delta = snapSpaceOnAxis(mainSize - previousSize);
            boolean growing = delta > 0;

            delta = snapSpaceOnAxis(Math.abs(delta));

            if (isPositive(delta) && !resizeList.isEmpty()) {
                double portion = snapPortionOnAxis(delta / resizeList.size());
                double size = snapSpaceOnAxis(portion * resizeList.size());
                double remainder = snapSpaceOnAxis(delta - size);
                if (!isPositive(portion)) {
                    portion = remainder;
                    size = remainder;
                    remainder = 0;
                }

                while (isPositive(size) && !resizeList.isEmpty()) {
                    if (growing) {
                        lastDividerUpdate++;
                    } else {
                        lastDividerUpdate--;
                        if (lastDividerUpdate < 0) {
                            lastDividerUpdate = contentRegions.size() - 1;
                        }
                    }
                    int id = lastDividerUpdate%contentRegions.size();
                    Content content = contentRegions.get(id);
                    if (content.isResizableWithParent() && resizeList.contains(content)) {
                        double area = content.getArea();
                        if (growing) {
                            double max = getMaxSize(content);
                            if ((area + portion) <= max) {
                                area = snapSpaceOnAxis(area + portion);
                            } else {
                                resizeList.remove(content);
                                continue;
                            }
                        } else {
                            double min = getMinSize(content);
                            if ((area - portion) >= min) {
                                area = snapSpaceOnAxis(area - portion);
                            } else {
                                resizeList.remove(content);
                                continue;
                            }
                        }
                        setArea(content, area);
                        size = snapSpaceOnAxis(size - portion);
                        if (!isPositive(size) && isPositive(remainder)) {
                            portion = remainder;
                            size = remainder;
                            remainder = 0;
                        } else if (!isPositive(size)) {
                            break;
                        }
                    }
                }

                // If we are resizing the window save the current area into
                // resizableWithParentArea.  We use this value during layout.
                {
                    for (Content c: contentRegions) {
                        c.setResizableWithParentArea(c.getArea());
                        c.setAvailable(0);
                    }
                }
                resize = true;
            }

            previousSize = mainSize;
        } else {
            previousSize = mainSize;
        }

        duringLayout = true;
        // If the window is less than the min size we want to resize proportionally
        double minSize = totalMinSize();
        if (minSize > mainSize) {
            layoutBelowMinimumSize(mainSize);
            setupContentAndDividerForLayout();
            layoutDividersAndContent(x, y, w, h, true);
            resize = false;
            duringLayout = false;
            return;
        }

        for(int trys = 0; trys < 10; trys++) {
            // Compute the area in between each divider.
            ContentDivider previousDivider = null;
            ContentDivider divider = null;
            for (int i = 0; i < contentRegions.size(); i++) {
                double space = 0;
                if (i < contentDividers.size()) {
                    divider = contentDividers.get(i);
                    if (divider.posExplicit) {
                        checkDividerPosition(divider, posToDividerPos(divider, divider.d.getPosition()),
                                divider.getDividerPos());
                    }
                    if (i == 0) {
                        // First panel
                        space = snapSpaceOnAxis(getAbsoluteDividerPos(divider));
                    } else {
                        double newPos = snapPositionOnAxis(
                            getAbsoluteDividerPos(previousDivider) + getDividerSize(previousDivider));
                        // Middle panels
                        if (getAbsoluteDividerPos(divider) <= getAbsoluteDividerPos(previousDivider)) {
                            // The current divider and the previous divider share the same position
                            // or the current divider position is less than the previous position.
                            // We will set the divider next to the previous divider.
                            setAndCheckAbsoluteDividerPos(divider, newPos);
                        }
                        space = snapSpaceOnAxis(getAbsoluteDividerPos(divider) - newPos);
                    }
                } else if (i == contentDividers.size()) {
                    // Last panel
                    double end = previousDivider != null
                            ? snapPositionOnAxis(getAbsoluteDividerPos(previousDivider)
                                    + getDividerSize(previousDivider)) : 0;
                    space = snapSpaceOnAxis(mainSize - end);
                }
                if (!resize || divider.posExplicit) {
                    setArea(contentRegions.get(i), space);
                }
                previousDivider = divider;
            }

            // Compute the amount of space we have available.
            // Available is amount of space we can take from a panel before we reach its min.
            // If available is negative we don't have enough space and we will
            // proportionally take the space from the other availables.  If we have extra space
            // we will porportionally give it to the others
            double spaceRequested = 0;
            double extraSpace = 0;
            for (Content c: contentRegions) {
                if (c == null) continue;

                double max = getMaxSize(c);
                double min = getMinSize(c);

                if (c.getArea() >= max) {
                    // Add the space that needs to be distributed to the others
                    extraSpace = snapSpaceOnAxis(extraSpace + c.getArea() - max);
                    setArea(c, max);
                }
                c.setAvailable(snapSpaceOnAxis(c.getArea() - min));
                if (c.getAvailable() < 0) {
                    spaceRequested = snapSpaceOnAxis(spaceRequested + c.getAvailable());
                }
            }

            spaceRequested = snapSpaceOnAxis(Math.abs(spaceRequested));

            // Add the panels where we can take space from
            List<Content> availableList = new ArrayList<>();
            List<Content> storageList = new ArrayList<>();
            List<Content> spaceRequestor = new ArrayList<>();
            double available = 0;
            for (Content c: contentRegions) {
                if (c.getAvailable() >= 0) {
                    available = snapSpaceOnAxis(available + c.getAvailable());
                    availableList.add(c);
                }

                if (resize && !c.isResizableWithParent()) {
                    // We are making the SplitPane bigger and will need to
                    // distribute the extra space.
                    if (c.getArea() >= c.getResizableWithParentArea()) {
                        extraSpace = snapSpaceOnAxis(extraSpace + c.getArea() - c.getResizableWithParentArea());
                    } else {
                        // We are making the SplitPane smaller and will need to
                        // distribute the space requested.
                        spaceRequested = snapSpaceOnAxis(spaceRequested + c.getResizableWithParentArea() - c.getArea());
                    }
                    c.setAvailable(0);
                }
                // Add the panels where we can add space to;
                if (resize) {
                    if (c.isResizableWithParent()) {
                        storageList.add(c);
                    }
                } else {
                    storageList.add(c);
                }
                // List of panels that need space.
                if (c.getAvailable() < 0) {
                    spaceRequestor.add(c);
                }
            }

            if (isPositive(extraSpace)) {
                extraSpace = distributeTo(storageList, extraSpace);
                // After distributing add any panels that may still need space to the
                // spaceRequestor list.
                spaceRequested = 0;
                spaceRequestor.clear();
                available = 0;
                availableList.clear();
                for (Content c: contentRegions) {
                    if (c.getAvailable() < 0) {
                        spaceRequested = snapSpaceOnAxis(spaceRequested + c.getAvailable());
                        spaceRequestor.add(c);
                    } else {
                        available = snapSpaceOnAxis(available + c.getAvailable());
                        availableList.add(c);
                    }
                }
                spaceRequested = snapSpaceOnAxis(Math.abs(spaceRequested));
            }

            if (available >= spaceRequested) {
                for (Content requestor: spaceRequestor) {
                    double min = getMinSize(requestor);
                    setArea(requestor, min);
                    requestor.setAvailable(0);
                }
                // After setting all the space requestors to their min we have to
                // redistribute the space requested to any panel that still
                // has available space.
                if (isPositive(spaceRequested) && !spaceRequestor.isEmpty()) {
                    distributeFrom(spaceRequested, availableList);
                }

                // Only for resizing.  We should have all the panel areas
                // available computed.  We can total them up and see
                // how much space we have left or went over and redistribute.
                if (resize) {
                    double total = 0;
                    for (Content c: contentRegions) {
                        if (c.isResizableWithParent()) {
                            total = snapSpaceOnAxis(total + c.getArea());
                        } else {
                            total = snapSpaceOnAxis(total + c.getResizableWithParentArea());
                        }
                    }
                    total = snapSpaceOnAxis(total + getTotalDividerSize());
                    if (total < mainSize) {
                        extraSpace = snapSpaceOnAxis(extraSpace + mainSize - total);
                        distributeTo(storageList, extraSpace);
                    } else {
                        spaceRequested = snapSpaceOnAxis(spaceRequested + total - mainSize);
                        distributeFrom(spaceRequested, storageList);
                    }
                }
            }

            setupContentAndDividerForLayout();

            // Check the bounds of every panel
            boolean passed = true;
            for (Content c: contentRegions) {
                double max = getMaxSize(c);
                double min = getMinSize(c);
                if (c.getArea() < min || c.getArea() > max) {
                    passed = false;
                    break;
                }
            }
            if (passed) {
                break;
            }
        }

        layoutDividersAndContent(x, y, w, h, false);
        duringLayout = false;
        resize = false;
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double minWidth = 0;
        double maxMinWidth = 0;
        double contentHeight = height == -1 ? -1 : snapSpaceY(Math.max(0, height - topInset - bottomInset));
        for (Content c : contentRegions) {
            double dependentHeight = horizontal ? getSnappedDependentSize(c, contentHeight) : -1;
            double childMinWidth = snapSizeX(c.minWidth(dependentHeight));
            minWidth = snapSpaceX(minWidth + childMinWidth);
            maxMinWidth = Math.max(maxMinWidth, childMinWidth);
        }
        if (horizontal) {
            for (ContentDivider d : contentDividers) {
                minWidth = snapSpaceX(minWidth + getDividerSize(d));
            }
            return snapSpaceX(minWidth + leftInset + rightInset);
        } else {
            return snapSpaceX(maxMinWidth + leftInset + rightInset);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double minHeight = 0;
        double maxMinHeight = 0;
        double contentWidth = width == -1 ? -1 : snapSpaceX(Math.max(0, width - leftInset - rightInset));
        for (Content c : contentRegions) {
            double dependentWidth = horizontal ? -1 : getSnappedDependentSize(c, contentWidth);
            double childMinHeight = snapSizeY(c.minHeight(dependentWidth));
            minHeight = snapSpaceY(minHeight + childMinHeight);
            maxMinHeight = Math.max(maxMinHeight, childMinHeight);
        }
        if (horizontal) {
            return snapSpaceY(maxMinHeight + topInset + bottomInset);
        } else {
            for (ContentDivider d : contentDividers) {
                minHeight = snapSpaceY(minHeight + getDividerSize(d));
            }
            return snapSpaceY(minHeight + topInset + bottomInset);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double prefWidth = 0;
        double prefMaxWidth = 0;
        double contentHeight = height == -1 ? -1 : snapSpaceY(Math.max(0, height - topInset - bottomInset));
        for (Content c : contentRegions) {
            double dependentHeight = horizontal ? getSnappedDependentSize(c, contentHeight) : -1;
            double childPrefWidth = snapSizeX(c.prefWidth(dependentHeight));
            prefWidth = snapSpaceX(prefWidth + childPrefWidth);
            prefMaxWidth = Math.max(prefMaxWidth, childPrefWidth);
        }
        if (horizontal) {
            for (ContentDivider d : contentDividers) {
                prefWidth = snapSpaceX(prefWidth + getDividerSize(d));
            }
            return snapSpaceX(prefWidth + leftInset + rightInset);
        } else {
            return snapSpaceX(prefMaxWidth + leftInset + rightInset);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double prefHeight = 0;
        double maxPrefHeight = 0;
        double contentWidth = width == -1 ? -1 : snapSpaceX(Math.max(0, width - leftInset - rightInset));
        for (Content c : contentRegions) {
            double dependentWidth = horizontal ? -1 : getSnappedDependentSize(c, contentWidth);
            double childPrefHeight = snapSizeY(c.prefHeight(dependentWidth));
            prefHeight = snapSpaceY(prefHeight + childPrefHeight);
            maxPrefHeight = Math.max(maxPrefHeight, childPrefHeight);
        }
        if (horizontal) {
            return snapSpaceY(maxPrefHeight + topInset + bottomInset);
        } else {
            for (ContentDivider d : contentDividers) {
                prefHeight = snapSpaceY(prefHeight + getDividerSize(d));
            }
            return snapSpaceY(prefHeight + topInset + bottomInset);
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private double snapSizeOnAxis(double value) {
        return horizontal ? snapSizeX(value) : snapSizeY(value);
    }

    private double snapSpaceOnAxis(double value) {
        return horizontal ? snapSpaceX(value) : snapSpaceY(value);
    }

    private double snapPositionOnAxis(double value) {
        return horizontal ? snapPositionX(value) : snapPositionY(value);
    }

    private double snapPortionOnAxis(double value) {
        if (value > 0) {
            return -snapSizeOnAxis(-value);
        } else if (value < 0) {
            return snapSizeOnAxis(value);
        }
        return value;
    }

    private static boolean isPositive(double value) {
        return value > EPSILON;
    }

    private void setArea(Content content, double value) {
        content.setArea(snapSpaceOnAxis(value));
    }

    private double getMinSize(Content content) {
        double dependentSize = getSnappedDependentSize(content, horizontal ? contentHeight : contentWidth);
        double min = horizontal ? content.minWidth(dependentSize) : content.minHeight(dependentSize);
        return snapSizeOnAxis(min);
    }

    private double getMaxSize(Content content) {
        double dependentSize = getSnappedDependentSize(content, horizontal ? contentHeight : contentWidth);
        double max = horizontal ? content.maxWidth(dependentSize) : content.maxHeight(dependentSize);
        return max == Double.MAX_VALUE ? max : snapSizeOnAxis(max);
    }

    private double getSnappedDependentSize(Content content, double crossSize) {
        if (crossSize < 0) {
            return -1;
        }

        Orientation bias = content.getContentBias();
        if (horizontal && bias == Orientation.VERTICAL) {
            return snapSizeY(boundedSize(content.minHeight(-1), crossSize, content.maxHeight(-1)));
        } else if (!horizontal && bias == Orientation.HORIZONTAL) {
            return snapSizeX(boundedSize(content.minWidth(-1), crossSize, content.maxWidth(-1)));
        }

        return -1;
    }

    private static double boundedSize(double min, double value, double max) {
        return Math.min(Math.max(value, min), Math.max(min, max));
    }

    private double getDividerSize(ContentDivider divider) {
        return snapSizeOnAxis(divider.prefDividerSize(horizontal));
    }

    private double getTotalDividerSize() {
        double size = 0;

        for (ContentDivider divider : contentDividers) {
            size = snapSpaceOnAxis(size + getDividerSize(divider));
        }

        return size;
    }

    private void layoutBelowMinimumSize(double mainSize) {
        double totalDividerSize = getTotalDividerSize();
        double contentSize = snapSpaceOnAxis(Math.max(0, mainSize - totalDividerSize));
        double totalContentMinSize = 0;
        List<Double> minimumSizes = new ArrayList<>(contentRegions.size());

        for (Content content : contentRegions) {
            double minimumSize = getMinSize(content);
            minimumSizes.add(minimumSize);
            totalContentMinSize = snapSpaceOnAxis(totalContentMinSize + minimumSize);
        }

        double allocated = 0;
        double cumulativeMinSize = 0;

        for (int i = 0; i < contentRegions.size(); i++) {
            Content content = contentRegions.get(i);
            double boundary;

            if (i == contentRegions.size() - 1) {
                boundary = contentSize;
            } else if (isPositive(totalContentMinSize)) {
                cumulativeMinSize = snapSpaceOnAxis(cumulativeMinSize + minimumSizes.get(i));
                boundary = snapSpaceOnAxis(contentSize * cumulativeMinSize / totalContentMinSize);
            } else {
                boundary = snapSpaceOnAxis(contentSize * (i + 1) / contentRegions.size());
            }

            setArea(content, boundary - allocated);
            content.setAvailable(0);
            allocated = boundary;
        }
    }

    private void addContent(int index, Node n) {
        Content c = new Content(n);
        contentRegions.add(index, c);
        getChildren().add(index, c);
    }

    private void removeContent(Node n) {
        for (Content c: contentRegions) {
            if (c.getContent().equals(n)) {
                c.dispose();
                getChildren().remove(c);
                contentRegions.remove(c);
                break;
            }
        }
    }

    private void initializeContentListener() {
        ListenerHelper.get(this).addListChangeListener(getSkinnable().getItems(), (ListChangeListener<Node>) c -> {
            while (c.next()) {
                if (c.wasPermutated() || c.wasUpdated()) {
                    /**
                     * the contents were either moved, or updated.
                     * rebuild the contents to re-sync
                     */
                    getChildren().clear();
                    contentRegions.clear();
                    int index = 0;
                    for (Node n : c.getList()) {
                        addContent(index++, n);
                    }

                } else {
                    for (Node n : c.getRemoved()) {
                        removeContent(n);
                    }

                    int index = c.getFrom();
                    for (Node n : c.getAddedSubList()) {
                        addContent(index++, n);
                    }
                }
            }

            removeAllDividers();
            addDividers();
        });
    }

    private void checkDividerPosition(ContentDivider divider, double newPos, double oldPos) {
        newPos = snapPositionOnAxis(newPos);
        Content left = getLeft(divider);
        Content right = getRight(divider);
        double dividerSize = getDividerSize(divider);
        double minLeft = left == null ? 0 : getMinSize(left);
        double minRight = right == null ? 0 : getMinSize(right);
        double maxLeft = left == null ? 0 : getMaxSize(left);
        double maxRight = right == null ? 0 : getMaxSize(right);

        double previousDividerPos = 0;
        double previousDividerEnd = 0;
        double nextDividerPos = getSize();
        int index = contentDividers.indexOf(divider);

        if (index - 1 >= 0) {
            ContentDivider previousDivider = contentDividers.get(index - 1);
            previousDividerPos = previousDivider.getDividerPos();
            if (previousDividerPos == -1) {
                // Get the divider position if it hasn't been initialized.
                previousDividerPos = getAbsoluteDividerPos(previousDivider);
            }
            previousDividerEnd = snapPositionOnAxis(previousDividerPos + getDividerSize(previousDivider));
        }
        if (index + 1 < contentDividers.size()) {
            nextDividerPos = contentDividers.get(index + 1).getDividerPos();
            if (nextDividerPos == -1) {
                // Get the divider position if it hasn't been initialized.
                nextDividerPos = getAbsoluteDividerPos(contentDividers.get(index + 1));
            }
        }

        // Set the divider into the correct position by looking at the max and min content sizes.
        checkDividerPos = false;
        if (newPos > oldPos) {
            double max = snapPositionOnAxis(previousDividerEnd + maxLeft);
            double min = snapPositionOnAxis(nextDividerPos - minRight - dividerSize);
            double stopPos = snapPositionOnAxis(Math.min(max, min));
            if (newPos >= stopPos) {
                setAbsoluteDividerPos(divider, stopPos);
            } else {
                double rightMax = snapPositionOnAxis(nextDividerPos - maxRight - dividerSize);
                if (newPos <= rightMax) {
                    setAbsoluteDividerPos(divider, rightMax);
                } else {
                    setAbsoluteDividerPos(divider, newPos);
                }
            }
        } else {
            double max = snapPositionOnAxis(nextDividerPos - maxRight - dividerSize);
            double min = snapPositionOnAxis(previousDividerEnd + minLeft);
            double stopPos = snapPositionOnAxis(Math.max(max, min));
            if (newPos <= stopPos) {
                setAbsoluteDividerPos(divider, stopPos);
            } else {
                double leftMax = snapPositionOnAxis(previousDividerEnd + maxLeft);
                if (newPos >= leftMax) {
                    setAbsoluteDividerPos(divider, leftMax);
                } else {
                    setAbsoluteDividerPos(divider, newPos);
                }
            }
        }
        checkDividerPos = true;
    }

    private void addDividers() {
        contentDividerListenerHelper = new ListenerHelper();

        for (SplitPane.Divider d : getSkinnable().getDividers()) {
            ContentDivider c = new ContentDivider(d);
            c.setInitialPos(d.getPosition());
            c.setDividerPos(-1);

            ChangeListener<Number> li = new PosPropertyListener(c);
            contentDividerListenerHelper.addChangeListener(d.positionProperty(), li);

            initializeDividerEventHandlers(c);

            contentDividers.add(c);
            getChildren().add(c);
        }
    }

    private void removeAllDividers() {
        ListIterator<ContentDivider> dividers = contentDividers.listIterator();
        while (dividers.hasNext()) {
            ContentDivider c = dividers.next();
            getChildren().remove(c);
            dividers.remove();
        }

        lastDividerUpdate = 0;

        if (contentDividerListenerHelper != null) {
            contentDividerListenerHelper.disconnect();
            contentDividerListenerHelper = null;
        }
    }

    private void initializeDividerEventHandlers(final ContentDivider divider) {
        // TODO: do we need to consume all mouse events?
        // they only bubble to the skin which consumes them by default
        divider.addEventHandler(MouseEvent.ANY, event -> {
            event.consume();
        });

        divider.setOnMousePressed(e -> {
            if (horizontal) {
                divider.setInitialPos(divider.getDividerPos());
                divider.setPressPos(getSkinnable().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT
                        ? getSkinnable().getWidth() - e.getSceneX() : e.getSceneX());
            } else {
                divider.setInitialPos(divider.getDividerPos());
                divider.setPressPos(e.getSceneY());
            }
            e.consume();
        });

        divider.setOnMouseDragged(e -> {
            double delta = 0;
            if (horizontal) {
                delta = getSkinnable().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT
                        ? getSkinnable().getWidth() - e.getSceneX() : e.getSceneX();
            } else {
                delta = e.getSceneY();
            }
            delta -= divider.getPressPos();
            setAndCheckAbsoluteDividerPos(divider, snapPositionOnAxis(divider.getInitialPos() + delta));
            e.consume();
        });
    }

    private Content getLeft(ContentDivider d) {
        int index = contentDividers.indexOf(d);
        if (index != -1) {
            return contentRegions.get(index);
        }
        return null;
    }

    private Content getRight(ContentDivider d) {
        int index = contentDividers.indexOf(d);
        if (index != -1) {
            return contentRegions.get(index + 1);
        }
        return null;
    }

    // Value is the left edge of the divider
    private void setAbsoluteDividerPos(ContentDivider divider, double value) {
        if (getSkinnable().getWidth() > 0 && getSkinnable().getHeight() > 0 && divider != null) {
            SplitPane.Divider paneDivider = divider.getDivider();
            value = snapPositionOnAxis(value);
            divider.setDividerPos(value);
            double size = getSize();
            if (size != 0) {
                // Adjust the position to the center of the
                // divider and convert its position to a percentage.
                double pos = value + getDividerSize(divider) / 2;
                paneDivider.setPosition(pos / size);
            } else {
                paneDivider.setPosition(0);
            }
        }
    }

    // Updates the divider with the SplitPane.Divider's position
    // The value updated to SplitPane.Divider will be the center of the divider.
    // The returned position will be the left edge of the divider
    private double getAbsoluteDividerPos(ContentDivider divider) {
        if (getSkinnable().getWidth() > 0 && getSkinnable().getHeight() > 0 && divider != null) {
            SplitPane.Divider paneDivider = divider.getDivider();
            double newPos = posToDividerPos(divider, paneDivider.getPosition());
            divider.setDividerPos(newPos);
            return newPos;
        }
        return 0;
    }

    // Returns the left edge of the divider at pos
    // Pos is the percentage location from SplitPane.Divider.
    private double posToDividerPos(ContentDivider divider, double pos) {
        double newPos = getSize() * pos;
        if (pos == 1) {
            newPos -= getDividerSize(divider);
        } else {
            newPos -= getDividerSize(divider) / 2;
        }
        return snapPositionOnAxis(newPos);
    }

    private double totalMinSize() {
        double minSize = getTotalDividerSize();

        for (Content c : contentRegions) {
            minSize = snapSpaceOnAxis(minSize + getMinSize(c));
        }

        return minSize;
    }

    private double getSize() {
        final SplitPane s = getSkinnable();
        if (horizontal) {
            return Math.max(0, snapSpaceX(snapSizeX(s.getWidth()) - snappedLeftInset() - snappedRightInset()));
        } else {
            return Math.max(0, snapSpaceY(snapSizeY(s.getHeight()) - snappedTopInset() - snappedBottomInset()));
        }
    }

    // Evenly distribute the size to the available list.
    // size is the amount to distribute.
    private double distributeTo(List<Content> available, double size) {
        if (available.isEmpty()) {
            return size;
        }

        size = snapSpaceOnAxis(size);
        double portion = snapPortionOnAxis(size / available.size());
        if (!isPositive(portion) && isPositive(size)) {
            portion = size;
        }

        while (isPositive(size) && !available.isEmpty()) {
            Iterator<Content> i = available.iterator();
            while (i.hasNext()) {
                Content c = i.next();
                double max = getMaxSize(c);
                double min = getMinSize(c);
                double capacity = snapSpaceOnAxis(max - c.getArea());

                // We have too much space
                if (!isPositive(capacity)) {
                    c.setAvailable(snapSpaceOnAxis(c.getArea() - min));
                    i.remove();
                    continue;
                }
                // Not enough space
                if (portion >= capacity) {
                    size = snapSpaceOnAxis(size - capacity);
                    setArea(c, max);
                    c.setAvailable(snapSpaceOnAxis(max - min));
                    i.remove();
                } else {
                    // Enough space
                    setArea(c, c.getArea() + portion);
                    c.setAvailable(snapSpaceOnAxis(c.getArea() - min));
                    size = snapSpaceOnAxis(size - portion);
                }
                if (!isPositive(size)) {
                    return 0;
                }
            }
            if (available.isEmpty()) {
                // We reached the max size for everything just return
                return size;
            }
            portion = snapPortionOnAxis(size / available.size());
            if (!isPositive(portion)) {
                portion = size;
            }
        }
        return isPositive(size) ? size : 0;
    }

    // Evenly distribute the size from the available list.
    // size is the amount to distribute.
    private double distributeFrom(double size, List<Content> available) {
        if (available.isEmpty()) {
            return size;
        }

        size = snapSpaceOnAxis(size);
        double portion = snapPortionOnAxis(size / available.size());
        if (!isPositive(portion) && isPositive(size)) {
            portion = size;
        }

        while (isPositive(size) && !available.isEmpty()) {
            Iterator<Content> i = available.iterator();
            while (i.hasNext()) {
                Content c = i.next();
                double capacity = snapSpaceOnAxis(c.getAvailable());
                if (!isPositive(capacity)) {
                    c.setAvailable(0);
                    i.remove();
                    continue;
                }
                //not enough space taking available and setting min
                if (portion >= capacity) {
                    setArea(c, c.getArea() - capacity); // Min size
                    size = snapSpaceOnAxis(size - capacity);
                    c.setAvailable(0);
                    i.remove();
                } else {
                    //enough space
                    setArea(c, c.getArea() - portion);
                    c.setAvailable(snapSpaceOnAxis(c.getAvailable() - portion));
                    size = snapSpaceOnAxis(size - portion);
                }
                if (!isPositive(size)) {
                    return 0;
                }
            }
            if (available.isEmpty()) {
                // We reached the min size for everything just return
                return size;
            }
            portion = snapPortionOnAxis(size / available.size());
            if (!isPositive(portion)) {
                portion = size;
            }
        }
        return isPositive(size) ? size : 0;
    }

    private void setupContentAndDividerForLayout() {
        // Set all the value to prepare for layout
        double position = 0;

        // The dividers are already in the correct positions. Disable checking
        // while synchronizing their public position properties.
        checkDividerPos = false;

        for (int i = 0; i < contentRegions.size(); i++) {
            Content c = contentRegions.get(i);
            if (resize && !c.isResizableWithParent()) {
                setArea(c, c.getResizableWithParentArea());
            }

            if (horizontal) {
                c.setX(snapPositionOnAxis(position));
                c.setY(0);
            } else {
                c.setX(0);
                c.setY(snapPositionOnAxis(position));
            }

            position = snapPositionOnAxis(position + c.getArea());

            if (i < contentDividers.size()) {
                ContentDivider d = contentDividers.get(i);
                if (horizontal) {
                    d.setX(position);
                    d.setY(0);
                } else {
                    d.setX(0);
                    d.setY(position);
                }

                setAbsoluteDividerPos(d, position);
                d.posExplicit = false;
                position = snapPositionOnAxis(position + getDividerSize(d));
            }
        }
        checkDividerPos = true;
    }

    private void layoutDividersAndContent(double contentX, double contentY,
                                          double width, double height,
                                          boolean belowMinimumSize) {
        for (Content c : contentRegions) {
            double areaX = snapPositionX(c.getX() + contentX);
            double areaY = snapPositionY(c.getY() + contentY);
            double areaWidth = horizontal ? snapSpaceX(c.getArea()) : snapSpaceX(width);
            double areaHeight = horizontal ? snapSpaceY(height) : snapSpaceY(c.getArea());
            c.setClipSize(areaWidth, areaHeight);
            layoutInArea(c, areaX, areaY, areaWidth, areaHeight,
                    0/*baseline*/, HPos.CENTER, VPos.CENTER);
            if (belowMinimumSize) {
                // The proportional layout deliberately allocates less than the
                // content minimum, so restore its main-axis allocation.
                if (horizontal) {
                    c.resize(areaWidth, c.getHeight());
                } else {
                    c.resize(c.getWidth(), areaHeight);
                }
                positionInArea(c, areaX, areaY, areaWidth, areaHeight,
                        0/*baseline*/, HPos.CENTER, VPos.CENTER);
            }
        }

        for (ContentDivider c : contentDividers) {
            double areaX = snapPositionX(c.getX() + contentX);
            double areaY = snapPositionY(c.getY() + contentY);
            double areaWidth = horizontal ? getDividerSize(c) : snapSpaceX(width);
            double areaHeight = horizontal ? snapSpaceY(height) : getDividerSize(c);
            c.resize(areaWidth, areaHeight);
            positionInArea(c, areaX, areaY, areaWidth, areaHeight,
                /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
        }
    }

    private double previousSize = -1;
    private int lastDividerUpdate = 0;
    private boolean resize = false;
    private boolean checkDividerPos = true;

    private void setAndCheckAbsoluteDividerPos(ContentDivider divider, double value) {
        double oldPos = divider.getDividerPos();
        setAbsoluteDividerPos(divider, value);
        checkDividerPosition(divider, divider.getDividerPos(), oldPos);
    }



    /* *************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/

    // This listener is to be removed from 'removed' dividers and added to 'added' dividers
    class PosPropertyListener implements ChangeListener<Number> {
        ContentDivider divider;

        public PosPropertyListener(ContentDivider divider) {
            this.divider = divider;
        }

        @Override public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (checkDividerPos) {
                // When checking is enforced, we know that the position was set explicitly
                divider.posExplicit = true;
            }
            if (!duringLayout) {
                getSkinnable().requestLayout();
            }
        }
    }


    class ContentDivider extends StackPane {
        private double initialPos;
        private double dividerPos;
        private double pressPos;
        private SplitPane.Divider d;
        private StackPane grabber;
        private double x;
        private double y;
        private boolean posExplicit;

        public ContentDivider(SplitPane.Divider d) {
            getStyleClass().setAll("split-pane-divider");

            this.d = d;
            this.initialPos = 0;
            this.dividerPos = 0;
            this.pressPos = 0;

            grabber = new StackPane() {
                @Override protected double computeMinWidth(double height) {
                    return 0;
                }

                @Override protected double computeMinHeight(double width) {
                    return 0;
                }

                @Override protected double computePrefWidth(double height) {
                    return snapSpaceX(snappedLeftInset() + snappedRightInset());
                }

                @Override protected double computePrefHeight(double width) {
                    return snapSpaceY(snappedTopInset() + snappedBottomInset());
                }

                @Override protected double computeMaxWidth(double height) {
                    return computePrefWidth(-1);
                }

                @Override protected double computeMaxHeight(double width) {
                    return computePrefHeight(-1);
                }
            };
            setGrabberStyle(horizontal);
            getChildren().add(grabber);

            // TODO register a listener for SplitPane.Divider position
        }

        public SplitPane.Divider getDivider() {
            return this.d;
        }

        public final void setGrabberStyle(boolean horizontal) {
            grabber.getStyleClass().clear();
            grabber.getStyleClass().setAll("vertical-grabber");
            setCursor(Cursor.V_RESIZE);
            if (horizontal) {
                grabber.getStyleClass().setAll("horizontal-grabber");
                setCursor(Cursor.H_RESIZE);
            }
        }

        private double prefDividerSize(boolean horizontal) {
            Insets insets = getInsets();

            if (horizontal) {
                return snapSpaceX(snapSpaceX(insets.getLeft()) + snapSpaceX(insets.getRight()));
            } else {
                return snapSpaceY(snapSpaceY(insets.getLeft()) + snapSpaceY(insets.getRight()));
            }
        }

        public double getInitialPos() {
            return initialPos;
        }

        public void setInitialPos(double initialPos) {
            this.initialPos = initialPos;
        }

        public double getDividerPos() {
            return dividerPos;
        }

        public void setDividerPos(double dividerPos) {
            this.dividerPos = dividerPos;
        }

        public double getPressPos() {
            return pressPos;
        }

        public void setPressPos(double pressPos) {
            this.pressPos = pressPos;
        }

        // TODO remove x and y and replace with dividerpos.
        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        @Override protected double computeMinWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }

        @Override protected double computePrefWidth(double height) {
            return snapSpaceX(snappedLeftInset() + snappedRightInset());
        }

        @Override protected double computePrefHeight(double width) {
            return snapSpaceY(snappedTopInset() + snappedBottomInset());
        }

        @Override protected double computeMaxWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMaxHeight(double width) {
            return computePrefHeight(width);
        }

        @Override protected void layoutChildren() {
            double grabberWidth = snapSizeX(grabber.prefWidth(-1));
            double grabberHeight = snapSizeY(grabber.prefHeight(-1));
            double grabberX = snapPositionX((getWidth() - grabberWidth) / 2);
            double grabberY = snapPositionY((getHeight() - grabberHeight) / 2);
            grabber.resize(grabberWidth, grabberHeight);
            positionInArea(grabber, grabberX, grabberY, grabberWidth, grabberHeight,
                    /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
        }
    }

    static class Content extends StackPane {
        private Node content;
        private Rectangle clipRect;
        private double x;
        private double y;
        private double area;
        private double resizableWithParentArea;
        private double available;

        public Content(Node n) {
            this.clipRect = new Rectangle();
            setClip(clipRect);
            this.content = n;
            if (n != null) {
                getChildren().add(n);
            }
            this.x = 0;
            this.y = 0;
        }

        public Node getContent() {
            return content;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        // This is the area of the panel.  This will be used as the
        // width/height during layout.
        public double getArea() {
            return area;
        }

        public void setArea(double area) {
            this.area = area;
        }

        // This is the minimum available area for other panels to use
        // if they need more space.
        public double getAvailable() {
            return available;
        }

        public void setAvailable(double available) {
            this.available = available;
        }

        public boolean isResizableWithParent() {
            return SplitPane.isResizableWithParent(content);
        }

        public double getResizableWithParentArea() {
            return resizableWithParentArea;
        }

        // This is used to save the current area during resizing when
        // isResizeableWithParent equals false.
        public void setResizableWithParentArea(double resizableWithParentArea) {
            if (!isResizableWithParent()) {
                this.resizableWithParentArea = resizableWithParentArea;
            } else {
                this.resizableWithParentArea = 0;
            }
        }

        protected void setClipSize(double w, double h) {
            clipRect.setWidth(w);
            clipRect.setHeight(h);
        }

        private void dispose() {
            getChildren().remove(content);
        }

        @Override protected double computeMaxWidth(double height) {
            return snapSizeX(content.maxWidth(height));
        }

        @Override protected double computeMaxHeight(double width) {
            return snapSizeY(content.maxHeight(width));
        }
    }
}
