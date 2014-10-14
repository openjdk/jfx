/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.theme;

import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.javafx.Utils;
import javafx.beans.Observable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollBar;

import com.sun.javafx.scene.control.skin.ScrollBarSkin;
import com.sun.webkit.graphics.Ref;
import com.sun.webkit.graphics.ScrollBarTheme;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.javafx.webkit.Accessor;
import com.sun.javafx.webkit.theme.RenderThemeImpl.Pool;
import com.sun.javafx.webkit.theme.RenderThemeImpl.ViewListener;
import com.sun.javafx.webkit.theme.RenderThemeImpl.Widget;
import com.sun.javafx.webkit.theme.RenderThemeImpl.WidgetType;

public final class ScrollBarThemeImpl extends ScrollBarTheme {

    private final static Logger log = Logger.getLogger(ScrollBarThemeImpl.class.getName());

    private WeakReference<ScrollBar> testSBRef = // used for hit testing
            new WeakReference<ScrollBar>(null);

    private boolean thicknessInitialized = false;

    private final Accessor accessor;

    private final Pool<ScrollBarWidget> pool;

    private final class ScrollBarWidget extends ScrollBar implements Widget {
        private ScrollBarWidget() {
            setOrientation(Orientation.VERTICAL);
            setMin(0);
            setManaged(false);
        }

        @Override public void impl_updatePeer() {
            super.impl_updatePeer();
            initializeThickness();
        }

        @Override public WidgetType getType() { return WidgetType.SCROLLBAR; }

        @Override protected void layoutChildren() {
            super.layoutChildren();
            initializeThickness();
        }
    }

    private static final class ScrollBarRef extends Ref {
        private final WeakReference<ScrollBarWidget> sbRef;

        private ScrollBarRef(ScrollBarWidget sb) {
            this.sbRef = new WeakReference<ScrollBarWidget>(sb);
        }

        private Control asControl() {
            return sbRef.get();
        }
    }

    /*
     * Note, the class should be instantiated no later than
     * the appropriate page is created to ensure 'testSB'
     * is added to the view before paiting starts.
     */
    public ScrollBarThemeImpl(final Accessor accessor) {
        this.accessor = accessor;
        pool = new Pool<ScrollBarWidget>(
                sb -> {
                    accessor.removeChild(sb);
                }, ScrollBarWidget.class);
        accessor.addViewListener(new ViewListener(pool, accessor) {
            @Override public void invalidated(Observable ov) {
                super.invalidated(ov);
                ScrollBar testSB = new ScrollBarWidget();
                // testSB should be added to the new WebView (if any)
                accessor.addChild(testSB);
                testSBRef = new WeakReference<ScrollBar>(testSB);
            }
        });

    }

    private static Orientation convertOrientation(int orientation) {
        return orientation == VERTICAL_SCROLLBAR ? Orientation.VERTICAL : Orientation.HORIZONTAL;
    }

    private void adjustScrollBar(ScrollBar sb, int w, int h, int orientation) {
        Orientation current = convertOrientation(orientation);
        if (current != sb.getOrientation()) {
            sb.setOrientation(current);
        }

        if (current == Orientation.VERTICAL) {
            w = ScrollBarTheme.getThickness();
        } else {
            h = ScrollBarTheme.getThickness();
        }

        if ((w != sb.getWidth()) || (h != sb.getHeight())) {
            sb.resize(w, h);
        }
    }

    private void adjustScrollBar(ScrollBar sb, int w, int h, int orientation,
                                 int value, int visibleSize, int totalSize)
    {
        adjustScrollBar(sb, w, h, orientation);
        boolean disable = totalSize <= visibleSize;
        sb.setDisable(disable);
        if (disable) {
            return;
        }
        if (value < 0) {
            value = 0;
        } else if(value > (totalSize - visibleSize)) {
            value = totalSize - visibleSize;
        }

        if (sb.getMax() != totalSize || sb.getVisibleAmount() != visibleSize) {
            sb.setValue(0); // reset 'value' to let 'max' & 'visibleAmount' be reinitialized
            sb.setMax(totalSize);
            sb.setVisibleAmount(visibleSize);
        }

        // For FX ScrollBar the following is true:
        //   [min <= value <= max] & [min <= visibleAmount <= max]
        // But webkit assumes that:
        //   [0 <= value <= totalSize - visibleAmount]
        // So, we calculate a factor from the following equation:
        //   (totalSize - visibleSize) * factor = totalSize
        if (totalSize > visibleSize) {
            float factor = ((float)totalSize) / (totalSize - visibleSize);
            if (sb.getValue() != value * factor) {
                sb.setValue(value * factor); // eventually set 'value'
            }
        }
    }

    @Override protected Ref createWidget(long id, int w, int h, int orientation,
                                         int value, int visibleSize,
                                         int totalSize)
    {
        ScrollBarWidget sb = pool.get(id);
        if (sb == null) {
            sb = new ScrollBarWidget();
            pool.put(id, sb);
            accessor.addChild(sb);
        }
        adjustScrollBar(sb, w, h, orientation, value, visibleSize, totalSize);
        
        return new ScrollBarRef(sb);
    }

    @Override public void paint(WCGraphicsContext g, Ref sbRef,
                                int x, int y, int pressedPart, int hoveredPart)
    {
        ScrollBar sb = (ScrollBar)((ScrollBarRef)sbRef).asControl();
        if (sb == null) {
            return;
        }
        
        if (log.isLoggable(Level.FINEST)) {
            log.log(Level.FINEST, "[{0}, {1} {2}x{3}], {4}",
                    new Object[] {x, y, sb.getWidth(), sb.getHeight(),
                    sb.getOrientation() == Orientation.VERTICAL ? "VERTICAL" : "HORIZONTAL"});
        }
        g.saveState();
        g.translate(x, y);
        Renderer.getRenderer().render(sb, g);
        g.restoreState();
    }

    @Override protected int hitTest(int w, int h, int orientation, int value,
                                    int visibleSize, int totalSize,
                                    int x, int y)
    {
        if (log.isLoggable(Level.FINEST)) {
            log.log(Level.FINEST, "[{0}, {1} {2}x{3}], {4}",
                    new Object[] {x, y, w, h, orientation == VERTICAL_SCROLLBAR ?
                    "VERTICAL" : "HORIZONTAL"});
        }

        ScrollBar testSB = testSBRef.get();
        if (testSB == null) {
            return NO_PART;
        }
        Node thumb = getThumb(testSB);
        Node track = getTrack(testSB);
        Node decButton = getDecButton(testSB);
        Node incButton = getIncButton(testSB);

        adjustScrollBar(testSB, w, h, orientation, value, visibleSize, totalSize);

        int trackX;
        int trackY;
        int incBtnX;
        int incBtnY;
        int thumbX;
        int thumbY;

        if (orientation == VERTICAL_SCROLLBAR) {
            trackX = incBtnX = thumbX = x;
            trackY = y - (int)decButton.getLayoutBounds().getHeight();
            thumbY = trackY - thumbPosition();
            incBtnY = trackY - (int)track.getLayoutBounds().getHeight();
        } else {
            trackY = incBtnY = thumbY = y;
            trackX = x - (int)decButton.getLayoutBounds().getWidth();
            thumbX = trackX - thumbPosition();
            incBtnX = trackX - (int)track.getLayoutBounds().getWidth();
        }

        if (thumb != null && thumb.isVisible() && thumb.contains(thumbX, thumbY)) {
            log.finer("thumb");
            return THUMB_PART;

        } else if (track != null && track.isVisible() && track.contains(trackX, trackY)) {

            if ((orientation == VERTICAL_SCROLLBAR && thumbPosition() >= trackY) ||
                (orientation == HORIZONTAL_SCROLLBAR && thumbPosition() >= trackX))
            {
                log.finer("back track");
                return BACK_TRACK_PART;

            } else if ((orientation == VERTICAL_SCROLLBAR && thumbPosition() < trackY) ||
                       (orientation == HORIZONTAL_SCROLLBAR && thumbPosition() < trackX))
            {
                log.finer("forward track");
                return FORWARD_TRACK_PART;
            }
        } else if (decButton != null && decButton.isVisible() && decButton.contains(x, y)) {
            log.finer("back button");
            return BACK_BUTTON_START_PART;

        } else if (incButton != null && incButton.isVisible() && incButton.contains(incBtnX, incBtnY)) {
            log.finer("forward button");
            return FORWARD_BUTTON_START_PART;
        }

        log.finer("no part");
        return NO_PART;
    }

    private int thumbPosition() {
        ScrollBar testSB = testSBRef.get();
        if (testSB == null) {
            return 0;
        }
        // position calculated after ScrollBarSkin.positionThumb()
        Node thumb = getThumb(testSB);
        if (thumb == null) {
            return 0;
        }
        double thumbLength = testSB.getOrientation() == Orientation.VERTICAL
                             ? thumb.getLayoutBounds().getHeight()
                             : thumb.getLayoutBounds().getWidth();

        Node track = getTrack(testSB);
        double trackLength = testSB.getOrientation() == Orientation.VERTICAL
                             ? track.getLayoutBounds().getHeight()
                             : track.getLayoutBounds().getWidth();

        double clampedValue = Utils.clamp(testSB.getMin(), testSB.getValue(), testSB.getMax());
        double range = testSB.getMax() - testSB.getMin();
        return (int) Math.round((range > 0)
                               ? ((trackLength - thumbLength) * (clampedValue - testSB.getMin()) / range)
                               : 0);
    }

    @Override protected int getThumbLength(int w, int h, int orientation,
                                           int value,
                                           int visibleSize, int totalSize)
    {        
        ScrollBar testSB = testSBRef.get();
        if (testSB == null) {
            return 0;
        }
        Node thumb = getThumb(testSB);
        if (thumb == null) {
            return 0;
        }
        adjustScrollBar(testSB, w, h, orientation, value, visibleSize, totalSize);
        
        double len = 0;
        if (orientation == VERTICAL_SCROLLBAR) {
            len = thumb.getLayoutBounds().getHeight();
        } else {
            len = thumb.getLayoutBounds().getWidth();
        }
        log.log(Level.FINEST, "thumb length: {0}", len);
        return (int)len;
    }

    @Override protected int getTrackPosition(int w, int h, int orientation) {
        ScrollBar testSB = testSBRef.get();
        if (testSB == null) {
            return 0;
        }
        Node decButton = getDecButton(testSB);
        if (decButton == null) {
            return 0;
        }
        adjustScrollBar(testSB, w, h, orientation);

        double pos = 0;
        if (orientation == VERTICAL_SCROLLBAR) {
            pos = decButton.getLayoutBounds().getHeight();
        } else {
            pos = decButton.getLayoutBounds().getWidth();
        }
        log.log(Level.FINEST, "track position: {0}", pos);
        return (int)pos;
    }

    @Override protected int getTrackLength(int w, int h, int orientation) {
        ScrollBar testSB = testSBRef.get();
        if (testSB == null) {
            return 0;
        }
        Node track = getTrack(testSB);
        if (track == null) {
            return 0;
        }
        adjustScrollBar(testSB, w, h, orientation);

        double len = 0;
        if (orientation == VERTICAL_SCROLLBAR) {
            len = track.getLayoutBounds().getHeight();
        } else {
            len = track.getLayoutBounds().getWidth();
        }
        log.log(Level.FINEST, "track length: {0}", len);
        return (int)len;
    }

    @Override protected int getThumbPosition(int w, int h, int orientation,
                                             int value,
                                             int visibleSize, int totalSize)
    {
        ScrollBar testSB = testSBRef.get();
        if (testSB == null) {
            return 0;
        }
        adjustScrollBar(testSB, w, h, orientation, value, visibleSize, totalSize);

        int pos = thumbPosition();
        log.log(Level.FINEST, "thumb position: {0}", pos);
        return pos;
    }

    private void initializeThickness() {
        if (!thicknessInitialized) {
            ScrollBar testSB = testSBRef.get();
            if (testSB == null) {
                return;
            }
            int thickness = (int) testSB.prefWidth(-1);
            if (thickness != 0 && ScrollBarTheme.getThickness() != thickness) {
                ScrollBarTheme.setThickness(thickness);
            }
            thicknessInitialized = true;
        }
    }

    private static Node getThumb(ScrollBar scrollBar) {
        return ((ScrollBarSkin)scrollBar.getSkin()).getThumb();
    }

    private static Node getTrack(ScrollBar scrollBar) {
        return ((ScrollBarSkin)scrollBar.getSkin()).getTrack();
    }

    private static Node getIncButton(ScrollBar scrollBar) {
        return ((ScrollBarSkin)scrollBar.getSkin()).getIncButton();
    }

    private static Node getDecButton(ScrollBar scrollBar) {
        return ((ScrollBarSkin)scrollBar.getSkin()).getDecButton();
    }
}
