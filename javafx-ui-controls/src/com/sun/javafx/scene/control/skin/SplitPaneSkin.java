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

import java.util.ListIterator;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.scene.control.behavior.BehaviorBase;

public class SplitPaneSkin extends SkinBase<SplitPane, BehaviorBase<SplitPane>>  {

    private ObservableList<Content> contentRegions;
    private ObservableList<ContentDivider> contentDividers;
    private boolean horizontal;
    private boolean updateDividerPos = false;
    private double previousSize = -1;

    public SplitPaneSkin(final SplitPane splitPane) {
        super(splitPane, new BehaviorBase<SplitPane>(splitPane));
        setManaged(false);
        horizontal = getSkinnable().getOrientation() == Orientation.HORIZONTAL;

        contentRegions = FXCollections.<Content>observableArrayList();
        contentDividers = FXCollections.<ContentDivider>observableArrayList();

        int index = 0;
        for (Node n: getSkinnable().getItems()) {
            addContent(index++, n);
        }
        initializeContentListener();

        for (SplitPane.Divider d: getSkinnable().getDividers()) {
            addDivider(d);
        }

        registerChangeListener(splitPane.orientationProperty(), "ORIENTATION");
        registerChangeListener(splitPane.widthProperty(), "WIDTH");
        registerChangeListener(splitPane.heightProperty(), "HEIGHT");
    }

    private void addContent(int index, Node n) {
        Content c = new Content(n);
        contentRegions.add(index, c);
        getChildren().add(index, c);
    }

    private void removeContent(Node n) {
        for (Content c: contentRegions) {
            if (c.getContent().equals(n)) {
                getChildren().remove(c);
                contentRegions.remove(c);
                break;
            }
        }
    }

    private void initializeContentListener() {
        getSkinnable().getItems().addListener(new ListChangeListener<Node>() {
            @Override public void onChanged(Change<? extends Node> c) {
                while (c.next()) {
                    for (Node n : c.getRemoved()) {
                        removeContent(n);
                    }

                    int index = c.getFrom();
                    for (Node n : c.getAddedSubList()) {
                        addContent(index++, n);
                    }
                }
                // TODO there may be a more efficient way than rebuilding all the dividers
                // everytime the list changes.
                removeAllDividers();
                for (SplitPane.Divider d: getSkinnable().getDividers()) {
                    addDivider(d);
                }
            }
        });
    }

    // This listener is to be removed from 'removed' dividers and added to 'added' dividers
    private final ChangeListener posPropertyListener = new ChangeListener() {
        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue) {
            updateDividerPos = true;
            requestLayout();
        }
    };

    private void addDivider(SplitPane.Divider d) {
        ContentDivider c = new ContentDivider(d);
        c.setInitialPos(d.getPosition());
        c.setDividerPos(d.getPosition());
        d.positionProperty().addListener(posPropertyListener);
        // TODO Maybe call updatePosition here.
        initializeDivderEventHandlers(c);
        contentDividers.add(c);
        getChildren().add(c);
    }

    private void removeAllDividers() {
        ListIterator<ContentDivider> dividers = contentDividers.listIterator();
        while (dividers.hasNext()) {
            ContentDivider c = dividers.next();
            getChildren().remove(c);
            c.getDivider().positionProperty().removeListener(posPropertyListener);
            dividers.remove();
        }
    }

    private void initializeDivderEventHandlers(final ContentDivider divider) {
        // TODO: do we need to consume all mouse events?
        // they only bubble to the skin which consumes them by default
        divider.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                event.consume();
            }
        });

        divider.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                if (horizontal) {
                    divider.setInitialPos(divider.getDividerPos());
                    divider.setPressPos(e.getSceneX());
                } else {
                    divider.setInitialPos(divider.getDividerPos());
                    divider.setPressPos(e.getSceneY());
                }
                e.consume();
            }
        });

        divider.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                double dividerWidth = divider.prefWidth(-1);
                double halfDividerWidth = dividerWidth/2;
                Content left = getLeft(divider);
                Content right = getRight(divider);
                double minLeft = left == null ? 0 : (horizontal) ? left.minWidth(-1) : left.minHeight(-1);
                double minRight = right == null ? 0 : (horizontal) ? right.minWidth(-1) : right.minHeight(-1);
                double maxLeft = left == null ? 0 :
                    left.getContent() != null ? (horizontal) ? left.getContent().maxWidth(-1) : left.getContent().maxHeight(-1) : 0;
                double maxRight = right == null ? 0 :
                    right.getContent() != null ? (horizontal) ? right.getContent().maxWidth(-1) : right.getContent().maxHeight(-1) : 0;

                double delta;
                double w = 0;
                if (horizontal) {
                    delta = e.getSceneX() - divider.getPressPos();
                     w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
                } else {
                    delta = e.getSceneY() - divider.getPressPos();
                    w = getHeight() - (getInsets().getTop() + getInsets().getBottom());
                }

                // newPos is the center of the divider;
                double newPos = Math.ceil(divider.getInitialPos() + delta);

                double previousDividerPos = 0;
                double nextDividerPos = getSize();
                int index = contentDividers.indexOf(divider);

                if (index - 1 >= 0) {
                    previousDividerPos = contentDividers.get(index - 1).getDividerPos();
                }
                if (index + 1 < contentDividers.size()) {
                    nextDividerPos = contentDividers.get(index + 1).getDividerPos();
                }
                if (delta > 0) {
                    double max = previousDividerPos == 0 ? maxLeft + halfDividerWidth : previousDividerPos + maxLeft + dividerWidth;
                    double min = nextDividerPos - minRight - dividerWidth;
                    if (nextDividerPos > w) {
                        nextDividerPos = w;
                        min = nextDividerPos - dividerWidth;
                    }

                    if (nextDividerPos == w) {
                        min += halfDividerWidth;
                    }

                    double stopPos = Math.min(max, min);
                    if (newPos >= stopPos) {
                        setDividerPos(divider, stopPos);
                    } else {
                        setDividerPos(divider, newPos);
                    }
                } else {
                    double max = nextDividerPos - maxRight - dividerWidth;
                    double min = previousDividerPos == 0 ? minLeft + halfDividerWidth : previousDividerPos + minLeft + dividerWidth;

                    if (nextDividerPos == w) {
                        max += halfDividerWidth;
                    }

                    double stopPos = Math.max(max, min);
                    if (newPos <= stopPos) {
                        setDividerPos(divider, stopPos);
                    } else {
                        setDividerPos(divider, newPos);
                    }
                }
                e.consume();
            }
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

    private ContentDivider getContentDivider(SplitPane.Divider d) {
        for (ContentDivider c: contentDividers) {
            if (c.getDivider().equals(d)) {
                return c;
            }
        }
        return null;
    }

    @Override  protected void handleControlPropertyChanged(String property) {
        super.handleControlPropertyChanged(property);
        if (property == "ORIENTATION") {
            this.horizontal = getSkinnable().getOrientation() == Orientation.HORIZONTAL;
            for (ContentDivider c: contentDividers) {
                c.setGrabberStyle(horizontal);
            }
            getSkinnable().requestLayout();
        } else if (property == "WIDTH") {
            updateDividerPos = true;
        } else if (property == "HEIGHT") {
            updateDividerPos = true;
        }
    }

    private void setDividerPos(ContentDivider divider, double value) {
        if (getWidth() > 0 && getHeight() > 0) {
            SplitPane.Divider paneDivider = divider.getDivider();
            divider.setDividerPos(value);
            double size = getSize();
            if (size != 0) {
                paneDivider.setPosition(divider.getDividerPos() / size);
            } else {
                paneDivider.setPosition(0);
            }
        }
        requestLayout();
    }

    // Updates the divider with the SplitPane.Divider's position
    private void updateDividerPos(ContentDivider divider) {
        if (updateDividerPos) {
            if (getWidth() > 0 && getHeight() > 0) {
                double newPos = getSize() * divider.getDivider().getPosition();
                newPos -= divider.prefWidth(-1)/2;
                divider.setDividerPos(Math.round(newPos));
            }
            updateDividerPos = false;
        }
    }

    private int indexOfMaxContent() {
        double maxSize = 0;
        Content content = null;
        // We are only using displayWidth here because in layoutChildren()
        // the vertical orientation uses the width as its height.
        for (Content c: contentRegions) {
            if (c.fitsInArea() && c.getDisplayWidth() > maxSize) {
                maxSize = c.getDisplayWidth();
                content = c;
            }
        }
        return contentRegions.indexOf(content);
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

    private double totalMaxSize() {
        double dividerWidth = !contentDividers.isEmpty() ? contentDividers.size() * contentDividers.get(0).prefWidth(-1) : 0;
        double maxSize = 0;
        for (Content c: contentRegions) {
            maxSize += c.getContent() != null ? (horizontal) ? c.getContent().maxWidth(-1) : c.getContent().maxHeight(-1) : 0;
        }
        return maxSize + dividerWidth;
    }

    private double getSize() {
        double size = totalMinSize();
        if (horizontal) {
            if (getWidth() > size) {
                size = getWidth() - getInsets().getLeft() - getInsets().getRight();
            }
        } else {
            if (getHeight() > size) {
                size = getHeight() - getInsets().getTop() - getInsets().getBottom();
            }
        }
        return size;
    }

    private void layoutDividersAndContent() {
        double w = 0;
        double h = 0;
        double paddingX = getInsets().getLeft();
        double paddingY = getInsets().getTop();
        double dividerWidth = contentDividers.isEmpty() ? 0 : contentDividers.get(0).prefWidth(-1);

        if (this.horizontal) {
            w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
            h = getHeight() - (getInsets().getTop() + getInsets().getBottom());
        } else {
            w = getHeight() - (getInsets().getTop() + getInsets().getBottom());
            h = getWidth() - (getInsets().getLeft() + getInsets().getRight());
        }

        for (Content c: contentRegions) {
            if (horizontal) {
                layoutInArea(c, c.getX() + paddingX, c.getY() + paddingY, c.getDisplayWidth(), c.getDisplayHeight(),
                    0/*baseline*/,HPos.CENTER, VPos.CENTER);
            } else {
                layoutInArea(c, c.getX() + paddingX, c.getY() + paddingY, c.getDisplayHeight(), c.getDisplayWidth(),
                    0/*baseline*/,HPos.CENTER, VPos.CENTER);
            }
        }

        for (ContentDivider c: contentDividers) {
            if (horizontal) {
                c.resize(dividerWidth, h);
                positionInArea(c, c.getX() + paddingX, c.getY() + paddingY, dividerWidth, h,
                    /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
            } else {
                c.resize(h, dividerWidth);
                positionInArea(c, c.getX() + paddingX, c.getY() + paddingY, h, dividerWidth,
                    /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
            }
        }
    }

    private void resizeSplitPane(double w, double h) {
        boolean grow = previousSize < (horizontal ? getWidth() : getHeight());
        redistribute(grow, w, h);        
        layoutDividersAndContent();
    }

    private void redistribute(boolean redistribute, double w, double h) {        
        double dividerWidth = contentDividers.isEmpty() ? 0 : contentDividers.get(0).prefWidth(-1);
        double halfDividerWidth = dividerWidth / 2.0f;
        double maxSize = totalMaxSize();
        double startX = horizontal ? contentDividers.get(contentDividers.size() - 1).getDividerPos() - halfDividerWidth : 0;
        double startY = horizontal ? 0 : contentDividers.get(contentDividers.size() - 1).getDividerPos() - halfDividerWidth;
        double dividerPos = 0;
        double previousDividerPos = 0;
        double nextDividerPos = w;
        double pos = 0;

        Content left = null;
        ContentDivider divider = null;
        Content right = null;

        for (int i = contentDividers.size() - 1; i >= 0; i--) {
            divider = contentDividers.get(i);
            right = getRight(divider);
            left = getLeft(divider);

            nextDividerPos = (i + 1 >= contentDividers.size()) ? w : contentDividers.get(i + 1).getDividerPos() - halfDividerWidth;
            dividerPos = contentDividers.get(i).getDividerPos() - halfDividerWidth;
            previousDividerPos = (i - 1 < 0) ? 0 : contentDividers.get(i - 1).getDividerPos() - halfDividerWidth;

            double availableLeftWidth = dividerPos - (previousDividerPos == 0 ? 0 : (previousDividerPos + dividerWidth));
            double availableRightWidth = nextDividerPos - (dividerPos + dividerWidth);

            // do bounds checking to ensure min/max widths aren't being exceeded
            double minLeftWidth  = left == null ? 0 : (horizontal) ? left.minWidth(-1) : left.minHeight(-1);
            double prefLeftWidth = left == null ? 0 : (horizontal) ? left.prefWidth(-1) : left.prefHeight(-1);
            double maxLeftWidth  = left == null ? 0 :
                left.getContent() != null ? (horizontal) ? left.getContent().maxWidth(-1) : left.getContent().maxHeight(-1) : 0;

            double minRightWidth  = right == null ? 0 : (horizontal) ? right.minWidth(-1) : right.minHeight(-1);
            double prefRightWidth = right == null ? 0 : (horizontal) ? right.prefWidth(-1) : right.prefHeight(-1);
            double maxRightWidth  = right == null ? 0 :
                right.getContent() != null ? (horizontal) ? right.getContent().maxWidth(-1) : right.getContent().maxHeight(-1) : 0;

            // These properties are what the actual width will be set to
            double leftNodeWidth;
            double rightNodeWidth;

            // sort out right node
            if (availableRightWidth <= minRightWidth) {
                rightNodeWidth = minRightWidth;
            } else if (availableRightWidth >= maxRightWidth) {
                rightNodeWidth = maxRightWidth;
            } else {
                if (right.isManaged()) {
                    rightNodeWidth = availableRightWidth;
                } else {
                    rightNodeWidth = Math.min(prefRightWidth, availableRightWidth);
                }
            }

            // sort out left node
            if (availableLeftWidth <= minLeftWidth) {
                leftNodeWidth = minLeftWidth;
            } else if (availableLeftWidth >= maxLeftWidth) {
                leftNodeWidth = maxLeftWidth;
            } else {
                if (left.isManaged()) {
                    leftNodeWidth = availableLeftWidth;
                } else {
                    leftNodeWidth = Math.min(prefLeftWidth, availableLeftWidth);
                }
            }

            // Can the content fit in the area ?
            double rightArea = availableRightWidth;
            double leftArea = availableLeftWidth;

            // Setup all the values for layout
            if (horizontal) {
                if (rightArea >= maxRightWidth) {
                    right.setX(startX + dividerWidth + (rightArea - maxRightWidth)/2);
                    right.setY(startY);
                    right.setDisplayWidth(maxRightWidth);
                    right.setDisplayHeight(h);
                    if (redistribute) {
                        pos = nextDividerPos - maxRightWidth - dividerWidth;
                        dividerPos = pos;
                        leftArea = pos - (previousDividerPos == 0 ? 0 : (previousDividerPos + dividerWidth));

                        if (dividerPos + maxRightWidth + dividerWidth >= maxSize) {
                            pos = maxSize - maxRightWidth - dividerWidth;
                        }
                        rightArea = nextDividerPos - (pos + dividerWidth);
                        right.setX(pos + dividerWidth + (rightArea - maxRightWidth)/2);
                    } else {
                        pos = dividerPos;
                    }
                } else if (rightArea > minRightWidth) {
                    right.setX(startX + dividerWidth);
                    right.setY(startY);
                    right.setDisplayWidth(rightArea);
                    right.setDisplayHeight(h);
                    pos = dividerPos;
                } else {
                    startX = nextDividerPos - minRightWidth;
                    right.setX(startX);
                    right.setY(startY);
                    right.setDisplayWidth(minRightWidth);
                    right.setDisplayHeight(h);
                    pos = startX - dividerWidth;
                    dividerPos = pos;
                    leftArea = pos - (previousDividerPos == 0 ? 0 : (previousDividerPos + dividerWidth));                    
                    if (leftArea <= minLeftWidth) {
                        leftArea = minLeftWidth;
                        previousDividerPos = previousDividerPos <= 0 ? 0 : dividerPos - minLeftWidth - dividerWidth;
                    }
                }
                divider.setX(pos);
                divider.setY(startY);

                if (leftArea >= maxLeftWidth) {
                    startX = previousDividerPos;
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(maxLeftWidth);
                    left.setDisplayHeight(h);
                } else if (leftArea > minLeftWidth) {
                    startX = previousDividerPos;
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(leftArea);
                    left.setDisplayHeight(h);
                } else {
                    startX = previousDividerPos;
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(minLeftWidth);
                    left.setDisplayHeight(h);
                }
            } else {
                // VERTICAL ORIENTATION
                if (rightArea >= maxRightWidth) {
                    right.setX(startX);
                    right.setY(startY + dividerWidth + (rightArea - maxRightWidth)/2);
                    right.setDisplayWidth(maxRightWidth);
                    right.setDisplayHeight(h);
                    if (redistribute) {
                        pos = nextDividerPos - maxRightWidth - dividerWidth;
                        dividerPos = pos;
                        leftArea = pos - (previousDividerPos == 0 ? 0 : (previousDividerPos + dividerWidth));

                        if (dividerPos + maxRightWidth + dividerWidth >= maxSize) {
                            pos = maxSize - maxRightWidth - dividerWidth;
                        }
                        rightArea = nextDividerPos - (pos + dividerWidth);
                        right.setY(pos + dividerWidth + (rightArea - maxRightWidth)/2);
                    } else {
                        pos = dividerPos;
                    }
                } else if (rightArea > minRightWidth) {
                    right.setX(startX);
                    right.setY(startY + dividerWidth);
                    right.setDisplayWidth(rightArea);
                    right.setDisplayHeight(h);
                    pos = dividerPos;
                } else {
                    startY = nextDividerPos - minRightWidth;
                    right.setX(startX);
                    right.setY(startY);
                    right.setDisplayWidth(minRightWidth);
                    right.setDisplayHeight(h);
                    pos = startY - dividerWidth;
                    dividerPos = pos;
                    leftArea = pos - (previousDividerPos == 0 ? 0 : (previousDividerPos + dividerWidth));
                    if (leftArea <= minLeftWidth) {
                        leftArea = minLeftWidth;
                        previousDividerPos = previousDividerPos <= 0 ? 0 : dividerPos - minLeftWidth - dividerWidth;
                    }
                }
                divider.setX(startX);
                divider.setY(pos);

                if (leftArea >= maxLeftWidth) {
                    startY = previousDividerPos;
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(maxLeftWidth);
                    left.setDisplayHeight(h);
                } else if (leftArea > minLeftWidth) {
                    startY = previousDividerPos;
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(leftArea);
                    left.setDisplayHeight(h);
                } else {
                    startY = previousDividerPos;
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(minLeftWidth);
                    left.setDisplayHeight(h);
                }
            }
            setDividerPos(divider, pos + halfDividerWidth);
        }
    }

    @Override protected void layoutChildren() {        
        if (!getSkinnable().isVisible()) {
            return;
        }

        double w = 0;
        double h = 0;

        if (this.horizontal) {
            w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
            h = getHeight() - (getInsets().getTop() + getInsets().getBottom());
        } else {
            w = getHeight() - (getInsets().getTop() + getInsets().getBottom());
            h = getWidth() - (getInsets().getLeft() + getInsets().getRight());
        }

        if (contentDividers.size() > 0 && previousSize != -1 && previousSize != (horizontal ? getWidth() : getHeight())) {
            resizeSplitPane(w, h);
            previousSize = horizontal ? getWidth() : getHeight();
            return;
        }
        previousSize = horizontal ? getWidth() : getHeight();

        double startX = 0;
        double startY = 0;
        double dividerPos = 0;
        double previousDividerPos = 0;
        double nextDividerPos = w;
        double dividerWidth = contentDividers.isEmpty() ? 0 : contentDividers.get(0).prefWidth(-1);
        double halfDividerWidth = dividerWidth / 2.0f;
        double pos = 0;

        Content left = null;
        ContentDivider divider = null;
        Content right = null;
        ContentDivider nextDivider = null;

        for (int i = 0; i < contentRegions.size(); i++) {
            if (i == contentRegions.size() - 1) {
                // We only have one content region in the SplitPane.
                if (i == 0) {                    
                    contentRegions.get(0).setX(startX);
                    contentRegions.get(0).setY(startY);
                    contentRegions.get(0).setDisplayWidth(w);
                    contentRegions.get(0).setDisplayHeight(h);
                }
                break;
            }

            nextDividerPos = w;
            nextDivider = null;

            left = contentRegions.get(i);

            // TODO need to get rid the boolean updateDividerPos.
            if (i < contentDividers.size()) {
                updateDividerPos = true;
                if (divider != null) {
                    previousDividerPos = divider.getDividerPos();
                }
                divider = contentDividers.get(i);
            }
            if (i + 1 < contentRegions.size()) {
                right = contentRegions.get(i + 1);
            }

            updateDividerPos(divider);
            dividerPos = divider.getDividerPos();

            if (i + 1 < contentDividers.size()) {
                nextDivider = contentDividers.get(i + 1);
                updateDividerPos = true;
                updateDividerPos(nextDivider);
            }

            if (nextDivider != null && nextDivider.getDividerPos() > dividerPos) {
                nextDividerPos = nextDivider.getDividerPos();
            }

            // this is the space available to the left and right nodes.
            // it would be ideal if both left and right nodes would happily resize
            // to this value, but we need to check...
            double availableLeftWidth =
                (dividerPos + halfDividerWidth) - (previousDividerPos == 0 ? 0 : (previousDividerPos + dividerWidth - halfDividerWidth));
            double availableRightWidth = nextDividerPos - dividerPos - halfDividerWidth;

            // do bounds checking to ensure min/max widths aren't being exceeded
            double minLeftWidth  = left == null ? 0 : (horizontal) ? left.minWidth(-1) : left.minHeight(-1);
            double prefLeftWidth = left == null ? 0 : (horizontal) ? left.prefWidth(-1) : left.prefHeight(-1);
            double maxLeftWidth  = left == null ? 0 :
                left.getContent() != null ? (horizontal) ? left.getContent().maxWidth(-1) : left.getContent().maxHeight(-1) : 0;

            double minRightWidth  = right == null ? 0 : (horizontal) ? right.minWidth(-1) : right.minHeight(-1);
            double prefRightWidth = right == null ? 0 : (horizontal) ? right.prefWidth(-1) : right.prefHeight(-1);
            double maxRightWidth  = right == null ? 0 :
                right.getContent() != null ? (horizontal) ? right.getContent().maxWidth(-1) : right.getContent().maxHeight(-1) : 0;

            // These properties are what the actual width will be set to
            double leftNodeWidth;
            double rightNodeWidth;
            boolean divRecomputed = false;

            // sort out left node
            if (availableLeftWidth <= (minLeftWidth + halfDividerWidth)) {
                dividerPos = (horizontal ? startX : startY) + minLeftWidth;
                leftNodeWidth = minLeftWidth;
                availableRightWidth = nextDividerPos - dividerPos - halfDividerWidth;
                divRecomputed = true;
            } else if (availableLeftWidth >= maxLeftWidth) {
                leftNodeWidth = maxLeftWidth;
                availableRightWidth = nextDividerPos - dividerPos - halfDividerWidth;
            } else {
                // if the node isn't managed, we shrink, but don't grow, the node
                availableLeftWidth -= halfDividerWidth;
                if (left.isManaged()) {
                    leftNodeWidth = availableLeftWidth;
                } else {
                    leftNodeWidth = Math.min(prefLeftWidth, availableLeftWidth);
                }
            }

            // sort out right node
            if (availableRightWidth <= (minRightWidth + halfDividerWidth)) {
                rightNodeWidth = minRightWidth;

                // Without it the right node will overflow the side of the SplitPane
                if (!divRecomputed) {
                    double rw = nextDividerPos - (dividerPos + dividerWidth);
                    if (minRightWidth > rw) {
                        dividerPos = nextDividerPos - rw - dividerWidth;
                    } else {
                        dividerPos = nextDividerPos - minRightWidth - dividerWidth;
                    }
                }

                if (left.isManaged()) {
                    // without this a managed node will overflow into the right
                    // region, but we also have to be careful that if there is a max
                    // width set on the left node that we don't grow it past that.
                    leftNodeWidth = Math.min(dividerPos, maxLeftWidth);
                }
            } else if (availableRightWidth >= maxRightWidth) {
                rightNodeWidth = maxRightWidth;
            } else {
                availableRightWidth -= halfDividerWidth;
                // if the node isn't managed, we shrink, but don't grow, the node
                if (right.isManaged()) {
                    rightNodeWidth = availableRightWidth;
                } else {
                    rightNodeWidth = Math.min(prefRightWidth, availableRightWidth);
                }
            }

            // Can the content fit in the area ?
            double leftArea = dividerPos - (previousDividerPos == 0 ? 0 : previousDividerPos + dividerWidth - halfDividerWidth);
            double rightArea = nextDividerPos - (dividerPos + dividerWidth);

            // Setup all the values for layout
            if (horizontal) {
                if (leftArea >= maxLeftWidth) {
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(maxLeftWidth);
                    left.setDisplayHeight(h);
                    pos = startX + maxLeftWidth;
                    dividerPos = pos;
                    rightArea = nextDividerPos - (pos + dividerWidth);
                } else if (leftArea > minLeftWidth) {
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(leftArea);
                    left.setDisplayHeight(h);
                    pos = dividerPos;
                } else {
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(minLeftWidth);
                    left.setDisplayHeight(h);
                    pos = startX + minLeftWidth;
                }

                divider.setX(pos);
                divider.setY(startY);

                if (rightArea >= maxRightWidth) {
                    startX = dividerPos + dividerWidth;
                    right.setX(startX + (rightArea - maxRightWidth)/2);
                    right.setY(startY);
                    right.setDisplayWidth(maxRightWidth);
                    right.setDisplayHeight(h);
                } else if (rightArea > minRightWidth) {
                    startX = dividerPos + dividerWidth;
                    right.setX(startX);
                    right.setY(startY);
                    right.setDisplayWidth(rightArea);
                    right.setDisplayHeight(h);
                } else {
                    // If the contents minimum size is too big to fit
                    // in the right area we want to mark it so it is not included
                    // using when handle the overflow.
                    right.setFitsInArea(true);
                    if (minRightWidth > rightArea) {                        
                        right.setFitsInArea(false);
                    }
                    startX = dividerPos + dividerWidth;
                    right.setX(startX);
                    right.setY(startY);
                    right.setDisplayWidth(minRightWidth);
                    right.setDisplayHeight(h);
                }
            } else {
                // VERTICAL ORIENTATION
                if (leftArea >= maxLeftWidth) {
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(maxLeftWidth);
                    left.setDisplayHeight(h);
                    pos = startY + maxLeftWidth;
                    dividerPos = pos;
                    rightArea = nextDividerPos - (pos + dividerWidth);
                } else if (leftArea > minLeftWidth) {
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(leftArea);
                    left.setDisplayHeight(h);
                    pos = dividerPos;
                } else {
                    left.setX(startX);
                    left.setY(startY);
                    left.setDisplayWidth(minLeftWidth);
                    left.setDisplayHeight(h);
                    pos = startY + minLeftWidth;
                }

                divider.setX(startX);
                divider.setY(pos);

                if (rightArea >= maxRightWidth) {
                    startY = dividerPos + dividerWidth;
                    right.setX(startX);
                    right.setY(startY + (rightArea - maxRightWidth)/2);
                    right.setDisplayWidth(maxRightWidth);
                    right.setDisplayHeight(h);
                } else if (rightArea > minRightWidth) {
                    startY = dividerPos + dividerWidth;
                    right.setX(startX);
                    right.setY(startY);
                    right.setDisplayWidth(rightArea);
                    right.setDisplayHeight(h);
                } else {
                    // If the contents minimum size is too big to fit
                    // in the right area we want to mark it so it is not included
                    // using when handle the overflow.
                    right.setFitsInArea(true);
                    if (minRightWidth > rightArea) {
                        right.setFitsInArea(false);
                    }
                    startY = dividerPos + dividerWidth;
                    right.setX(startX);
                    right.setY(startY);
                    right.setDisplayWidth(minRightWidth);
                    right.setDisplayHeight(h);
                }
            }
            setDividerPos(divider, pos + halfDividerWidth);
        }

        // TODO need to remove the overflow counter;
        int overflowCounter = 0;

        // If we overflowed we may need several passes to fix the overflow.
        double overflow = contentRegions.size() > 1 ?
            (((contentDividers.get(contentDividers.size() - 1).getDividerPos() - halfDividerWidth) +
                dividerWidth + contentRegions.get(contentRegions.size() - 1).getDisplayWidth()) - w) : 0;
        
        if (overflow < 0) {
            // RT-18805 try and redistribute the dividers if there
            // is space left over.
            redistribute(true, w, h);
        }
        
        // TODO Maybe we should adjust for priority.
        while (overflow > 0 && overflowCounter < 50) {
            int index = indexOfMaxContent();
            if (index == -1) {
                break;
            }
            ListIterator<Content> contentList = contentRegions.listIterator(index);
            Content c = contentList.next();
            double min = horizontal ? c.minWidth(-1): c.minHeight(-1);

            if (c.getDisplayWidth() - overflow > min) {
                c.setDisplayWidth(c.getDisplayWidth() - overflow);
            } else {
                overflow -= (min - (c.getDisplayWidth() - overflow));
                c.setDisplayWidth(min);
            }
            while (contentList.hasNext()) {
                c = contentList.next();
                if (horizontal) {
                    c.setX(c.getX() - overflow);
                } else {
                    c.setY(c.getY() - overflow);
                }
            }

            ListIterator<ContentDivider> dividerList = contentDividers.listIterator(index);
            while (dividerList.hasNext()) {
                ContentDivider div = dividerList.next();
                if (horizontal) {
                    div.setX(div.getX() - overflow);
                } else {
                    div.setY(div.getY() - overflow);
                }
                setDividerPos(div, div.getDividerPos() - overflow);
            }

            overflow = ((contentDividers.get(contentDividers.size() - 1).getDividerPos() - halfDividerWidth) +
                      dividerWidth + contentRegions.get(contentRegions.size() - 1).getDisplayWidth()) - w;
            overflowCounter++;
        }
        layoutDividersAndContent();
    }

    @Override protected double computeMinWidth(double height) {
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
            return minWidth + getInsets().getLeft() + getInsets().getRight();
        } else {
            return maxMinWidth + getInsets().getLeft() + getInsets().getRight();
        }
    }

    @Override protected double computeMinHeight(double width) {
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
            return maxMinHeight + getInsets().getTop() + getInsets().getBottom();
        } else {
            return minHeight + getInsets().getTop() + getInsets().getBottom();
        }
    }

    @Override protected double computePrefWidth(double height) {
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
            return prefWidth + getInsets().getLeft() + getInsets().getRight();
        } else {
            return prefMaxWidth + getInsets().getLeft() + getInsets().getRight();
        }
    }

    @Override protected double computePrefHeight(double width) {
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
            return maxPrefHeight + getInsets().getTop() + getInsets().getBottom();
        } else {
            return prefHeight + getInsets().getTop() + getInsets().getBottom();
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
                    return getInsets().getLeft() + getInsets().getRight();
                }

                @Override protected double computePrefHeight(double width) {
                    return getInsets().getTop() + getInsets().getBottom();
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
            return getInsets().getLeft() + getInsets().getRight();
        }

        @Override protected double computePrefHeight(double width) {
            return getInsets().getTop() + getInsets().getBottom();
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

    class Content extends StackPane {
        private Node content;
        private Rectangle clipRect;
        private double x;
        private double y;
        private double displayWidth;
        private double displayHeight;
        private boolean fitsInArea;

        public Content(Node n) {
            this.clipRect = new Rectangle();
            setClip(clipRect);
            this.content = n;
            if (n != null) {
                getChildren().add(n);
            }
            this.x = 0;
            this.y = 0;
            this.displayWidth = 0;
            this.displayHeight = 0;
            fitsInArea = true;
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

        public double getDisplayHeight() {
            return displayHeight;
        }

        public void setDisplayHeight(double displayHeight) {
            this.displayHeight = displayHeight;
        }

        public double getDisplayWidth() {
            return displayWidth;
        }

        public void setDisplayWidth(double displayWidth) {
            this.displayWidth = displayWidth;
        }

        public boolean fitsInArea() {
            return fitsInArea;
        }

        public void setFitsInArea(boolean fitsInArea) {
            this.fitsInArea = fitsInArea;
        }

        @Override protected void setWidth(double value) {
            super.setWidth(value);
            clipRect.setWidth(value);
        }

        @Override protected void setHeight(double value) {
            super.setHeight(value);
            clipRect.setHeight(value);
        }
    }
}
