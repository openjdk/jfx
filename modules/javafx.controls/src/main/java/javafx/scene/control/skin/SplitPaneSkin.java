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

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private ObservableList<Content> contentRegions;
    private ObservableList<ContentDivider> contentDividers;
    private ListenerHelper contentDividerListenerHelper;
    private boolean horizontal;
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
        final SplitPane s = getSkinnable();
        final double sw = s.getWidth();
        final double sh = s.getHeight();

        if ((horizontal ? sw == 0 : sh == 0) || contentRegions.isEmpty()) {
            return;
        }

        double dividerWidth = contentDividers.isEmpty() ? 0 : contentDividers.get(0).prefWidth(-1);

        if (contentDividers.size() > 0 && previousSize != -1 && previousSize != (horizontal ? sw  : sh)) {
            //This algorithm adds/subtracts a little to each panel on every resize
            List<Content> resizeList = new ArrayList<>();
            for (Content c: contentRegions) {
                if (c.isResizableWithParent()) {
                    resizeList.add(c);
                }
            }

            double delta = (horizontal ? s.getWidth() : s.getHeight()) - previousSize;
            boolean growing = delta > 0;

            delta = Math.abs(delta);

            if (delta != 0 && !resizeList.isEmpty()) {
                int portion = (int)(delta)/resizeList.size();
                int remainder = (int)delta%resizeList.size();
                int size = 0;
                if (portion == 0) {
                    portion = remainder;
                    size = remainder;
                    remainder = 0;
                } else {
                    size = portion * resizeList.size();
                }

                while (size > 0 && !resizeList.isEmpty()) {
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
                            double max = horizontal ? content.maxWidth(-1) : content.maxHeight(-1);
                            if ((area + portion) <= max) {
                                area += portion;
                            } else {
                                resizeList.remove(content);
                                continue;
                            }
                        } else {
                            double min = horizontal ? content.minWidth(-1) : content.minHeight(-1);
                            if ((area - portion) >= min) {
                                area -= portion;
                            } else {
                                resizeList.remove(content);
                                continue;
                            }
                        }
                        content.setArea(area);
                        size -= portion;
                        if (size == 0 && remainder != 0) {
                            portion = remainder;
                            size = remainder;
                            remainder = 0;
                        } else if (size == 0) {
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

            previousSize = horizontal ? sw : sh;
        } else {
            previousSize = horizontal ? sw : sh;
        }

        duringLayout = true;
        // If the window is less than the min size we want to resize proportionally
        double minSize = totalMinSize();
        if (minSize > (horizontal ? w : h)) {
            double percentage = 0;
            for (int i = 0; i < contentRegions.size(); i++) {
                Content c = contentRegions.get(i);
                double min = horizontal ? c.minWidth(-1) : c.minHeight(-1);
                percentage = min/minSize;
                if (horizontal) {
                    c.setArea(snapSpaceX(percentage * w));
                } else {
                    c.setArea(snapSpaceY(percentage * h));
                }
                c.setAvailable(0);
            }
            setupContentAndDividerForLayout();
            layoutDividersAndContent(w, h);
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
                        space = getAbsoluteDividerPos(divider);
                    } else {
                        double newPos = getAbsoluteDividerPos(previousDivider) + dividerWidth;
                        // Middle panels
                        if (getAbsoluteDividerPos(divider) <= getAbsoluteDividerPos(previousDivider)) {
                            // The current divider and the previous divider share the same position
                            // or the current divider position is less than the previous position.
                            // We will set the divider next to the previous divider.
                            setAndCheckAbsoluteDividerPos(divider, newPos);
                        }
                        space = getAbsoluteDividerPos(divider) - newPos;
                    }
                } else if (i == contentDividers.size()) {
                    // Last panel
                    space = (horizontal ? w : h) - (previousDivider != null ? getAbsoluteDividerPos(previousDivider) + dividerWidth : 0);
                }
                if (!resize || divider.posExplicit) {
                    contentRegions.get(i).setArea(space);
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

                double max = horizontal ? c.maxWidth(-1) : c.maxHeight(-1);
                double min = horizontal ? c.minWidth(-1) : c.minHeight(-1);

                if (c.getArea() >= max) {
                    // Add the space that needs to be distributed to the others
                    extraSpace += (c.getArea() - max);
                    c.setArea(max);
                }
                c.setAvailable(c.getArea() - min);
                if (c.getAvailable() < 0) {
                    spaceRequested += c.getAvailable();
                }
            }

            spaceRequested = Math.abs(spaceRequested);

            // Add the panels where we can take space from
            List<Content> availableList = new ArrayList<>();
            List<Content> storageList = new ArrayList<>();
            List<Content> spaceRequestor = new ArrayList<>();
            double available = 0;
            for (Content c: contentRegions) {
                if (c.getAvailable() >= 0) {
                    available += c.getAvailable();
                    availableList.add(c);
                }

                if (resize && !c.isResizableWithParent()) {
                    // We are making the SplitPane bigger and will need to
                    // distribute the extra space.
                    if (c.getArea() >= c.getResizableWithParentArea()) {
                        extraSpace += (c.getArea() - c.getResizableWithParentArea());
                    } else {
                        // We are making the SplitPane smaller and will need to
                        // distribute the space requested.
                        spaceRequested += (c.getResizableWithParentArea() - c.getArea());
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

            if (extraSpace > 0) {
                extraSpace = distributeTo(storageList, extraSpace);
                // After distributing add any panels that may still need space to the
                // spaceRequestor list.
                spaceRequested = 0;
                spaceRequestor.clear();
                available = 0;
                availableList.clear();
                for (Content c: contentRegions) {
                    if (c.getAvailable() < 0) {
                        spaceRequested += c.getAvailable();
                        spaceRequestor.add(c);
                    } else {
                        available += c.getAvailable();
                        availableList.add(c);
                    }
                }
                spaceRequested = Math.abs(spaceRequested);
            }

            if (available >= spaceRequested) {
                for (Content requestor: spaceRequestor) {
                    double min = horizontal ? requestor.minWidth(-1) : requestor.minHeight(-1);
                    requestor.setArea(min);
                    requestor.setAvailable(0);
                }
                // After setting all the space requestors to their min we have to
                // redistribute the space requested to any panel that still
                // has available space.
                if (spaceRequested > 0 && !spaceRequestor.isEmpty()) {
                    distributeFrom(spaceRequested, availableList);
                }

                // Only for resizing.  We should have all the panel areas
                // available computed.  We can total them up and see
                // how much space we have left or went over and redistribute.
                if (resize) {
                    double total = 0;
                    for (Content c: contentRegions) {
                        if (c.isResizableWithParent()) {
                            total += c.getArea();
                        } else {
                            total += c.getResizableWithParentArea();
                        }
                    }
                    total += (dividerWidth * contentDividers.size());
                    if (total < (horizontal ? w : h)) {
                        extraSpace += ((horizontal ? w : h) - total);
                        distributeTo(storageList, extraSpace);
                    } else {
                        spaceRequested += (total - (horizontal ? w : h));
                        distributeFrom(spaceRequested, storageList);
                    }
                }
            }

            setupContentAndDividerForLayout();

            // Check the bounds of every panel
            boolean passed = true;
            for (Content c: contentRegions) {
                double max = horizontal ? c.maxWidth(-1) : c.maxHeight(-1);
                double min = horizontal ? c.minWidth(-1) : c.minHeight(-1);
                if (c.getArea() < min || c.getArea() > max) {
                    passed = false;
                    break;
                }
            }
            if (passed) {
                break;
            }
        }

        layoutDividersAndContent(w, h);
        duringLayout = false;
        resize = false;
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double minWidth = 0;
        double maxMinWidth = 0;
        for (Content c: contentRegions) {
            minWidth += c.minWidth(-1);
            maxMinWidth = Math.max(maxMinWidth, c.minWidth(-1));
        }
        for (ContentDivider d: contentDividers) {
            minWidth += d.prefWidth(-1);
        }
        if (horizontal) {
            return minWidth + leftInset + rightInset;
        } else {
            return maxMinWidth + leftInset + rightInset;
        }
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double minHeight = 0;
        double maxMinHeight = 0;
        for (Content c: contentRegions) {
            minHeight += c.minHeight(-1);
            maxMinHeight = Math.max(maxMinHeight, c.minHeight(-1));
        }
        for (ContentDivider d: contentDividers) {
            minHeight += d.prefWidth(-1);
        }
        if (horizontal) {
            return maxMinHeight + topInset + bottomInset;
        } else {
            return minHeight + topInset + bottomInset;
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double prefWidth = 0;
        double prefMaxWidth = 0;
        for (Content c: contentRegions) {
            prefWidth += c.prefWidth(-1);
            prefMaxWidth = Math.max(prefMaxWidth, c.prefWidth(-1));
        }
        for (ContentDivider d: contentDividers) {
            prefWidth += d.prefWidth(-1);
        }
        if (horizontal) {
            return prefWidth + leftInset + rightInset;
        } else {
            return prefMaxWidth + leftInset + rightInset;
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double prefHeight = 0;
        double maxPrefHeight = 0;
        for (Content c: contentRegions) {
            prefHeight += c.prefHeight(-1);
            maxPrefHeight = Math.max(maxPrefHeight, c.prefHeight(-1));
        }
        for (ContentDivider d: contentDividers) {
            prefHeight += d.prefWidth(-1);
        }
        if (horizontal) {
            return maxPrefHeight + topInset + bottomInset;
        } else {
            return prefHeight + topInset + bottomInset;
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

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
        double dividerWidth = divider.prefWidth(-1);
        Content left = getLeft(divider);
        Content right = getRight(divider);
        double minLeft = left == null ? 0 : (horizontal) ? left.minWidth(-1) : left.minHeight(-1);
        double minRight = right == null ? 0 : (horizontal) ? right.minWidth(-1) : right.minHeight(-1);
        double maxLeft = left == null ? 0 :
            left.getContent() != null ? (horizontal) ? left.getContent().maxWidth(-1) : left.getContent().maxHeight(-1) : 0;
        double maxRight = right == null ? 0 :
            right.getContent() != null ? (horizontal) ? right.getContent().maxWidth(-1) : right.getContent().maxHeight(-1) : 0;

        double previousDividerPos = 0;
        double nextDividerPos = getSize();
        int index = contentDividers.indexOf(divider);

        if (index - 1 >= 0) {
            previousDividerPos = contentDividers.get(index - 1).getDividerPos();
            if (previousDividerPos == -1) {
                // Get the divider position if it hasn't been initialized.
                previousDividerPos = getAbsoluteDividerPos(contentDividers.get(index - 1));
            }
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
            double max = previousDividerPos == 0 ? maxLeft : previousDividerPos + dividerWidth + maxLeft;
            double min = nextDividerPos - minRight - dividerWidth;
            double stopPos = Math.min(max, min);
            if (newPos >= stopPos) {
                setAbsoluteDividerPos(divider, stopPos);
            } else {
                double rightMax = nextDividerPos - maxRight - dividerWidth;
                if (newPos <= rightMax) {
                    setAbsoluteDividerPos(divider, rightMax);
                } else {
                    setAbsoluteDividerPos(divider, newPos);
                }
            }
        } else {
            double max = nextDividerPos - maxRight - dividerWidth;
            double min = previousDividerPos == 0 ? minLeft : previousDividerPos + minLeft + dividerWidth;
            double stopPos = Math.max(max, min);
            if (newPos <= stopPos) {
                setAbsoluteDividerPos(divider, stopPos);
            } else {
                double leftMax = previousDividerPos + maxLeft + dividerWidth;
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
                divider.setPressPos(e.getSceneX());
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
            setAndCheckAbsoluteDividerPos(divider, Math.ceil(divider.getInitialPos() + delta));
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
            divider.setDividerPos(value);
            double size = getSize();
            if (size != 0) {
                // Adjust the position to the center of the
                // divider and convert its position to a percentage.
                double pos = value + divider.prefWidth(-1)/2;
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
            newPos -= divider.prefWidth(-1);
        } else {
            newPos -= divider.prefWidth(-1)/2;
        }
        return Math.round(newPos);
    }

    private double totalMinSize() {
        double dividerWidth = !contentDividers.isEmpty() ? contentDividers.size() * contentDividers.get(0).prefWidth(-1) : 0;
        double minSize = 0;
        for (Content c: contentRegions) {
            if (horizontal) {
                minSize += c.minWidth(-1);
            } else {
                minSize += c.minHeight(-1);
            }
        }
        return minSize + dividerWidth;
    }

    private double getSize() {
        final SplitPane s = getSkinnable();
        double size = totalMinSize();
        if (horizontal) {
            if (s.getWidth() > size) {
                size = s.getWidth() - snappedLeftInset() - snappedRightInset();
            }
        } else {
            if (s.getHeight() > size) {
                size = s.getHeight() - snappedTopInset() - snappedBottomInset();
            }
        }
        return size;
    }

    // Evenly distribute the size to the available list.
    // size is the amount to distribute.
    private double distributeTo(List<Content> available, double size) {
        if (available.isEmpty()) {
            return size;
        }

        size = horizontal ? snapSizeX(size) : snapSizeY(size);
        int portion = (int)(size)/available.size();
        int remainder;

        while (size > 0 && !available.isEmpty()) {
            Iterator<Content> i = available.iterator();
            while (i.hasNext()) {
                Content c = i.next();
                double max = Math.min((horizontal ? c.maxWidth(-1) : c.maxHeight(-1)), Double.MAX_VALUE);
                double min = horizontal ? c.minWidth(-1) : c.minHeight(-1);

                // We have too much space
                if (c.getArea() >= max) {
                    c.setAvailable(c.getArea() - min);
                    i.remove();
                    continue;
                }
                // Not enough space
                if (portion >= (max - c.getArea())) {
                    size -= (max - c.getArea());
                    c.setArea(max);
                    c.setAvailable(max - min);
                    i.remove();
                } else {
                    // Enough space
                    c.setArea(c.getArea() + portion);
                    c.setAvailable(c.getArea() - min);
                    size -= portion;
                }
                if ((int)size == 0) {
                    return size;
                }
            }
            if (available.isEmpty()) {
                // We reached the max size for everything just return
                return size;
            }
            portion = (int)(size)/available.size();
            remainder = (int)(size)%available.size();
            if (portion == 0 && remainder != 0) {
                portion = remainder;
                remainder = 0;
            }
        }
        return size;
    }

    // Evenly distribute the size from the available list.
    // size is the amount to distribute.
    private double distributeFrom(double size, List<Content> available) {
        if (available.isEmpty()) {
            return size;
        }

        size = horizontal ? snapSizeX(size) : snapSizeY(size);
        int portion = (int)(size)/available.size();
        int remainder;

        while (size > 0 && !available.isEmpty()) {
            Iterator<Content> i = available.iterator();
            while (i.hasNext()) {
                Content c = i.next();
                //not enough space taking available and setting min
                if (portion >= c.getAvailable()) {
                    c.setArea(c.getArea() - c.getAvailable()); // Min size
                    size -= c.getAvailable();
                    c.setAvailable(0);
                    i.remove();
                } else {
                    //enough space
                    c.setArea(c.getArea() - portion);
                    c.setAvailable(c.getAvailable() - portion);
                    size -= portion;
                }
                if ((int)size == 0) {
                    return size;
                }
            }
            if (available.isEmpty()) {
                // We reached the min size for everything just return
                return size;
            }
            portion = (int)(size)/available.size();
            remainder = (int)(size)%available.size();
            if (portion == 0 && remainder != 0) {
                portion = remainder;
                remainder = 0;
            }
        }
        return size;
    }

    private void setupContentAndDividerForLayout() {
        // Set all the value to prepare for layout
        double dividerWidth = contentDividers.isEmpty() ? 0 : contentDividers.get(0).prefWidth(-1);
        double startX = 0;
        double startY = 0;
        for (Content c: contentRegions) {
            if (resize && !c.isResizableWithParent()) {
                c.setArea(c.getResizableWithParentArea());
            }

            c.setX(startX);
            c.setY(startY);
            if (horizontal) {
                startX += (c.getArea() + dividerWidth);
            } else {
                startY += (c.getArea() + dividerWidth);
            }
        }

        startX = 0;
        startY = 0;
        // The dividers are already in the correct positions.  Disable
        // checking the divider positions.
        checkDividerPos = false;
        for (int i = 0; i < contentDividers.size(); i++) {
            ContentDivider d = contentDividers.get(i);
            if (horizontal) {
                startX += getLeft(d).getArea() + (i == 0 ? 0 : dividerWidth);
            } else {
                startY += getLeft(d).getArea() + (i == 0 ? 0 : dividerWidth);
            }
            d.setX(startX);
            d.setY(startY);
            setAbsoluteDividerPos(d, (horizontal ? d.getX() : d.getY()));
            d.posExplicit = false;
        }
        checkDividerPos = true;
    }

    private void layoutDividersAndContent(double width, double height) {
        final double paddingX = snappedLeftInset();
        final double paddingY = snappedTopInset();
        final double dividerWidth = contentDividers.isEmpty() ? 0 : contentDividers.get(0).prefWidth(-1);

        for (Content c: contentRegions) {
//            System.out.println("LAYOUT " + c.getId() + " PANELS X " + c.getX() + " Y " + c.getY() + " W " + (horizontal ? c.getArea() : width) + " H " + (horizontal ? height : c.getArea()));
            if (horizontal) {
                c.setClipSize(c.getArea(), height);
                layoutInArea(c, c.getX() + paddingX, c.getY() + paddingY, c.getArea(), height,
                    0/*baseline*/,HPos.CENTER, VPos.CENTER);
            } else {
                c.setClipSize(width, c.getArea());
                layoutInArea(c, c.getX() + paddingX, c.getY() + paddingY, width, c.getArea(),
                    0/*baseline*/,HPos.CENTER, VPos.CENTER);
            }
        }
        for (ContentDivider c: contentDividers) {
//            System.out.println("LAYOUT DIVIDERS X " + c.getX() + " Y " + c.getY() + " W " + (horizontal ? dividerWidth : width) + " H " + (horizontal ? height : dividerWidth));
            if (horizontal) {
                c.resize(dividerWidth, height);
                positionInArea(c, c.getX() + paddingX, c.getY() + paddingY, dividerWidth, height,
                    /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
            } else {
                c.resize(width, dividerWidth);
                positionInArea(c, c.getX() + paddingX, c.getY() + paddingY, width, dividerWidth,
                    /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
            }
        }
    }

    private double previousSize = -1;
    private int lastDividerUpdate = 0;
    private boolean resize = false;
    private boolean checkDividerPos = true;

    private void setAndCheckAbsoluteDividerPos(ContentDivider divider, double value) {
        double oldPos = divider.getDividerPos();
        setAbsoluteDividerPos(divider, value);
        checkDividerPosition(divider, value, oldPos);
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
                    return snappedLeftInset() + snappedRightInset();
                }

                @Override protected double computePrefHeight(double width) {
                    return snappedTopInset() + snappedBottomInset();
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
            return snappedLeftInset() + snappedRightInset();
        }

        @Override protected double computePrefHeight(double width) {
            return snappedTopInset() + snappedBottomInset();
        }

        @Override protected double computeMaxWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMaxHeight(double width) {
            return computePrefHeight(width);
        }

        @Override protected void layoutChildren() {
            double grabberWidth = grabber.prefWidth(-1);
            double grabberHeight = grabber.prefHeight(-1);
            double grabberX = (getWidth() - grabberWidth)/2;
            double grabberY = (getHeight() - grabberHeight)/2;
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

