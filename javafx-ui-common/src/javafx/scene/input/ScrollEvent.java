/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.input;

import com.sun.javafx.scene.input.InputEventUtils;
import com.sun.javafx.tk.Toolkit;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;

/**
 * Scroll event indicates that user performed scrolling by mouse wheel, touchpad
 * or other similar device. It is always delivered to the node under cursor
 * regardless of current focus owner (similarly to mouse events).
 * <p>
 * The event provides two different types of scrolling values: pixel-based and 
 * character/line-based. The basic {@code deltaX} and {@code deltaY} values 
 * give reasonable results when used as number of pixels
 * to scroll. For scrolling text (or other line-based content as tables) the 
 * {@code textDelta} values should be used if they are available. The 
 * {@code textDeltaXUnits} and {@code textDeltaYUnits} determine how to 
 * interpret the {@code textDeltaX} and {@code textDeltaY} values. If the 
 * units are set to {@code NONE}, the text-based values are not available
 * (not provided by the underlying platform) and the pixel-based values
 * need to be used.
 * <p>
 * For example, scrolling a graphical node can be achieved by following code:
 * <code><pre>
    node.setOnScroll(new EventHandler<ScrollEvent>() {
        &#64;Override public void handle(ScrollEvent event) {
            node.setTranslateX(node.getTranslateX() + event.getDeltaX());
            node.setTranslateY(node.getTranslateY() + event.getDeltaY());
        }
    });
</pre></code>
 * <p>
 * A scroll event handler on text-based component behaving
 * according to system settings on all platforms should contain following logic:
 * <code><pre>
    switch(event.getTextDeltaYUnits()) {
        case LINES:
            // scroll about event.getTextDeltaY() lines 
            break;
        case PAGES:
            // scroll about event.getTextDeltaY() pages
            break;
        case NONE:
            // scroll about event.getDeltaY() pixels
            break;
    }
 </pre></code>
 */
public class ScrollEvent extends InputEvent {

    /**
     * This event occurs when user performs a scrolling action such as
     * rotating mouse wheel.
     */
    public static final EventType<ScrollEvent> SCROLL =
            new EventType<ScrollEvent>(InputEvent.ANY, "SCROLL");

    /**
     * Common supertype for all scroll event types.
     */
    public static final EventType<ScrollEvent> ANY = SCROLL;
    
    private ScrollEvent(final EventType<? extends ScrollEvent> eventType) {
        super(eventType);
    }

    private ScrollEvent(Object source, EventTarget target,
            final EventType<? extends ScrollEvent> eventType) {
        super(source, target, eventType);
    }
    
    /**
     * Fills the given event by this event's coordinates recomputed to the given
     * source object.
     * @param newEvent Event whose coordinates are to be filled
     * @param newSource Source object to compute coordinates for
     */
    private void recomputeCoordinatesToSource(ScrollEvent newEvent, Object newSource) {

        final Point2D newCoordinates = InputEventUtils.recomputeCoordinates(
                new Point2D(x, y), source, newSource);

        newEvent.x = newCoordinates.getX();
        newEvent.y = newCoordinates.getY();
        newEvent.sceneX = getSceneX();
        newEvent.sceneY = getSceneY();
    }
    
    /**
     * @InheritDoc
     */
    @Override
    public Event copyFor(Object newSource, EventTarget newTarget) {
        ScrollEvent e = (ScrollEvent) super.copyFor(newSource, newTarget);
        recomputeCoordinatesToSource(e, newSource);
        return e;
    }

    private double deltaX;

    /**
     * Gets the horizontal scroll amount. This value should be interpreted
     * as a number of pixels to scroll. When scrolling a text-based content,
     * the {@code textDeltaX} and {@code textDeltaXUnits} values should be 
     * considered first.
     * <p>
     * The sign of the value is reversed compared to the coordinate system
     * (when you scroll right, the content actually needs to go left). So the 
     * returned value can be simply added to the content's {@code X}
     * coordinate.
     * 
     * @return Number of pixels to scroll horizontally.
     */
    public double getDeltaX() {
        return deltaX;
    }

    private double deltaY;

    /**
     * Gets the vertical scroll amount. This value should be interpreted
     * as a number of pixels to scroll. When scrolling a line-based content,
     * the {@code textDeltaY} and {@code textDeltaYUnits} values should be 
     * considered first.
     * <p>
     * The sign of the value is reversed compared to the coordinate system
     * (when you scroll down, the content actually needs to go up). So the 
     * returned value can be simply added to the content's {@code Y}
     * coordinate.
     * 
     * @return Number of pixels to scroll vertically.
     */
    public double getDeltaY() {
        return deltaY;
    }
    
    private HorizontalTextScrollUnits textDeltaXUnits;

    /**
     * Gets the horizontal scrolling units for text-based scrolling.
     * The returned value indicates how to interpret the {@code getTextDeltaX()}
     * value. If the returned value is {@code NONE}, the text-based
     * scrolling value is not available and the pixel-based 
     * {@code getDeltaX()} value needs to be used.
     * 
     * @return the horizontal scrolling units for text-based scrolling
     */
    public HorizontalTextScrollUnits getTextDeltaXUnits() {
        return textDeltaXUnits;
    }

    private VerticalTextScrollUnits textDeltaYUnits;

    /**
     * Gets the vertical scrolling units for text-based scrolling.
     * The returned value indicates how to interpret the {@code getTextDeltaY()}
     * value. If the returned value is {@code NONE}, the text-based
     * scrolling value is not available and the pixel-based 
     * {@code getDeltaY()} value needs to be used.
     * 
     * @return the vertical scrolling units for text-based scrolling
     */
    public VerticalTextScrollUnits getTextDeltaYUnits() {
        return textDeltaYUnits;
    }
    
    private double textDeltaX;

    /**
     * Gets the horizontal text-based scroll amount. This value should be 
     * interpreted according to the {@code getTextDeltaXUnits()} value. 
     * 
     * @return Number of units to scroll horizontally, zero if the text-based 
     * horizontal scrolling data is not available {@code getTextDeltaXUnits()}
     * returns {@code NONE}
     */
    public double getTextDeltaX() {
        return textDeltaX;
    }
    
    private double textDeltaY;

    /**
     * Gets the vertical text-based scroll amount. This value should be 
     * interpreted according to the {@code getTextDeltaYUnits()} value. 
     * 
     * @return Number of units to scroll vertically, zero if the text-based 
     * vertical scrolling data is not available {@code getTextDeltaYUnits()}
     * returns {@code NONE}
     */
    public double getTextDeltaY() {
        return textDeltaY;
    }
    
    private double x;

    /**
     * Gets the horizontal position of the event relative to the
     * origin of the ScrollEvent's source.
     *
     * @return the horizontal position of the event relative to the
     * origin of the ScrollEvent's source.
     */
    public final double getX() {
        return x;
    }

    private double y;

    /**
     * Gets the vertical position of the event relative to the
     * origin of the ScrollEvent's source.
     * 
     * @return the vertical position of the event relative to the
     * origin of the ScrollEvent's source.
     */
    public final double getY() {
        return y;
    }

    private double screenX;

    /**
     * Gets the absolute horizontal position of the event.
     * @return the absolute horizontal position of the event
     */
    public final double getScreenX() {
        return screenX;
    }

    private double screenY;

    /**
     * Gets the absolute vertical position of the event.
     * @return the absolute vertical position of the event
     */
    public final double getScreenY() {
        return screenY;
    }

    private double sceneX;

    /**
     * Gets the horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the ScrollEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the ScrollEvent's node.
     * 
     * @return the horizontal position of the event relative to the
     * origin of the {@code Scene} that contains the ScrollEvent's source
     */
    public final double getSceneX() {
        return sceneX;
    }

    private double sceneY;

    /**
     * Gets the vertical position of the event relative to the
     * origin of the {@code Scene} that contains the ScrollEvent's source.
     * If the node is not in a {@code Scene}, then the value is relative to
     * the boundsInParent of the root-most parent of the ScrollEvent's node.
     * 
     * @return the vertical position of the event relative to the
     * origin of the {@code Scene} that contains the ScrollEvent's source
     */
    public final double getSceneY() {
        return sceneY;
    }
    
    private boolean shiftDown;

    /**
     * Indicates whether or not the Shift modifier is down on this event.
     * @return true if the Shift modifier is down on this event
     */
    public final boolean isShiftDown() {
        return shiftDown;
    }

    private boolean controlDown;

    /**
     * Indicates whether or not the Control modifier is down on this event.
     * @return true if the Control modifier is down on this event
     */
    public final boolean isControlDown() {
        return controlDown;
    }

    private boolean altDown;

    /**
     * Indicates whether or not the Alt modifier is down on this event.
     * @return true if the Alt modifier is down on this event
     */
    public final boolean isAltDown() {
        return altDown;
    }

    private boolean metaDown;

    /**
     * Indicates whether or not the Meta modifier is down on this event.
     * @return true if the Meta modifier is down on this event
     */
    public final boolean isMetaDown() {
        return metaDown;
    }

    /**
     * Indicates whether or not the host platform common shortcut modifier is
     * down on this event. This common shortcut modifier is a modifier key which
     * is used commonly in shortcuts on the host platform. It is for example
     * {@code control} on Windows and {@code meta} (command key) on Mac.
     *
     * @return {@code true} if the shortcut modifier is down, {@code false}
     *      otherwise
     */
    public final boolean isShortcutDown() {
        switch (Toolkit.getToolkit().getPlatformShortcutKey()) {
            case SHIFT:
                return shiftDown;

            case CONTROL:
                return controlDown;

            case ALT:
                return altDown;

            case META:
                return metaDown;

            default:
                return false;
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static ScrollEvent impl_scrollEvent(
            double _scrollX, double _scrollY,
            HorizontalTextScrollUnits _scrollTextXUnits, double _scrollTextX,
            VerticalTextScrollUnits _scrollTextYUnits, double _scrollTextY,
            double _x, double _y,
            double _screenX, double _screenY,
            boolean _shiftDown,
            boolean _controlDown,
            boolean _altDown,
            boolean _metaDown
          )
    {
        ScrollEvent e = new ScrollEvent(SCROLL);
        e.deltaX = _scrollX;
        e.deltaY = _scrollY;
        e.textDeltaXUnits = _scrollTextXUnits;
        e.textDeltaX = _scrollTextX;
        e.textDeltaYUnits = _scrollTextYUnits;
        e.textDeltaY = _scrollTextY;
        e.x = _x;
        e.y = _y;
        e.screenX = _screenX;
        e.screenY = _screenY;
        e.sceneX = _x;
        e.sceneY = _y;
        e.shiftDown = _shiftDown;
        e.controlDown = _controlDown;
        e.altDown = _altDown;
        e.metaDown = _metaDown;
        return e;
    }
    
    /**
     * Returns a string representation of this {@code ScrollEvent} object.
     * @return a string representation of this {@code ScrollEvent} object.
     */ 
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("ScrollEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", deltaX = ").append(getDeltaX())
                .append(", deltaY = ").append(getDeltaY());
        sb.append(", textDeltaXUnits = ").append(getTextDeltaXUnits())
                .append(", textDeltaX = ").append(getTextDeltaX());
        sb.append(", textDeltaYUnits = ").append(getTextDeltaYUnits())
                .append(", textDeltaY = ").append(getTextDeltaY());
        sb.append(", x = ").append(getX()).append(", y = ").append(getY());

        if (isShiftDown()) {
            sb.append(", shiftDown");
        }
        if (isControlDown()) {
            sb.append(", controlDown");
        }
        if (isAltDown()) {
            sb.append(", altDown");
        }
        if (isMetaDown()) {
            sb.append(", metaDown");
        }
        if (isShortcutDown()) {
            sb.append(", shortcutDown");
        }

        return sb.append("]").toString();
    }
    
    /**
     * Horizontal text-based scrolling units.
     */
    public static enum HorizontalTextScrollUnits {
        /**
         * The horizontal text-based scrolling data is not available (not
         * provided by the underlying platform).
         */
        NONE,
        
        /**
         * The horizontal text-based scrolling amount is a number of characters
         * to scroll.
         */
        CHARACTERS
    }

    /**
     * Vertical text-based scrolling units.
     */
    public static enum VerticalTextScrollUnits {
        /**
         * The vertical text-based scrolling data is not available (not
         * provided by the underlying platform).
         */
        NONE,

        /**
         * The vertical text-based scrolling amount is a number of lines
         * to scroll.
         */
        LINES,

        /**
         * The vertical text-based scrolling amount is a number of pages
         * to scroll.
         */
        PAGES
    }
}
