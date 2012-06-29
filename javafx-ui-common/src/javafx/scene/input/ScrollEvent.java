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

import com.sun.javafx.event.EventTypeUtil;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Scroll event indicates that user performed scrolling by mouse wheel,
 * track pad, touch screen or other similar device.
 * <p>
 * When the scrolling is produced by a touch gesture (such as dragging a finger
 * over a touch screen), it is surrounded by the {@code SCROLL_STARTED} and 
 * {@code SCROLL_FINISHED} events. Changing number of involved touch points during
 * the scrolling is considered a new gesture, so the pair of
 * {@code SCROLL_FINISHED} and {@code SCROLL_STARTED} notifications is delivered
 * each time the {@code touchCount} changes. When the scrolling is caused by a mouse
 * wheel rotation, only a one-time {@code SCROLL} event is delivered, without
 * the started/finished surroundings. If scrolling inertia is active on the
 * given platform, some {@code SCROLL} events with {@code isInertia()} returning
 * {@code true} can come after {@code SCROLL_FINISHED}.
 * <p>
 * The event is delivered to the top-most
 * node picked on the gesture coordinates in time of the gesture start - the
 * whole gesture is delivered to the same node even if the coordinates change
 * during the gesture. For mouse wheel rotation the event is delivered to the
 * top-most node picked on mouse cursor location. The delivery is independent
 * of current focus owner.
 * <p>
 * The event provides two different types of scrolling values: pixel-based and 
 * character/line-based. The basic {@code deltaX} and {@code deltaY} values 
 * give reasonable results when used as number of pixels
 * to scroll (The {@code totalDeltaX} and {@code totalDeltaY} contain the 
 * cumulative values for the whole gesture, zeros for mouse wheel).
 * For scrolling text (or other line-based content as tables) the
 * {@code textDelta} values should be used if they are available. The 
 * {@code textDeltaXUnits} and {@code textDeltaYUnits} determine how to 
 * interpret the {@code textDeltaX} and {@code textDeltaY} values. If the 
 * units are set to {@code NONE}, the text-based values are not available
 * (not provided by the underlying platform) and the pixel-based values
 * need to be used.
 * <p>
 * As all gestures, scrolling can be direct (performed directly at
 * the concrete coordinates as on touch screen - the center point among all
 * the touches is usually used as the gesture coordinates) or indirect (performed
 * indirectly as on track pad or with mouse - the mouse cursor location
 * is usually used as the gesture coordinates).
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
public class ScrollEvent extends GestureEvent {

    /**
     * Common supertype for all scroll event types.
     */
    public static final EventType<ScrollEvent> ANY =
            EventTypeUtil.registerInternalEventType(GestureEvent.ANY, "ANY_SCROLL");

    /**
     * This event occurs when user performs a scrolling action such as
     * rotating mouse wheel or dragging a finger over touch screen.
     */
    public static final EventType<ScrollEvent> SCROLL =
            EventTypeUtil.registerInternalEventType(ScrollEvent.ANY, "SCROLL");

    /**
     * This event occurs when a scrolling gesture is detected. It doesn't
     * occur for mouse wheel scrolling.
     */
    public static final EventType<ScrollEvent> SCROLL_STARTED =
            EventTypeUtil.registerInternalEventType(ScrollEvent.ANY, "SCROLL_STARTED");

    /**
     * This event occurs when a scrolling gesture ends. It doesn't
     * occur for mouse wheel scrolling.
     */
    public static final EventType<ScrollEvent> SCROLL_FINISHED =
            EventTypeUtil.registerInternalEventType(ScrollEvent.ANY, "SCROLL_FINISHED");

    
    private ScrollEvent(final EventType<? extends ScrollEvent> eventType) {
        super(eventType);
    }

    private ScrollEvent(Object source, EventTarget target,
            final EventType<? extends ScrollEvent> eventType) {
        super(source, target, eventType);
    }

    private ScrollEvent(final EventType<? extends ScrollEvent> eventType,
            double deltaX, double deltaY,
            double gestureDeltaX, double gestureDeltaY,
            HorizontalTextScrollUnits textDeltaXUnits, double textDeltaX,
            VerticalTextScrollUnits textDeltaYUnits, double textDeltaY,
            int touchCount,
            double x, double y,
            double screenX, double screenY,
            boolean shiftDown,
            boolean controlDown,
            boolean altDown,
            boolean metaDown,
            boolean direct,
            boolean inertia) {

        super(eventType, x, y, screenX, screenY,
                shiftDown, controlDown, altDown, metaDown, direct, inertia);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.totalDeltaX = gestureDeltaX;
        this.totalDeltaY = gestureDeltaY;
        this.textDeltaXUnits = textDeltaXUnits;
        this.textDeltaX = textDeltaX;
        this.textDeltaYUnits = textDeltaYUnits;
        this.textDeltaY = textDeltaY;
        this.touchCount = touchCount;
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
    
    private double totalDeltaX;

    /**
     * Gets the cumulative horizontal scroll amount for the whole gesture.
     * This value should be interpreted as a number of pixels to scroll
     * relatively to the state at the beginning of the gesture.
     * Contains zeros for mouse wheel scrolling.
     * <p>
     * The sign of the value is reversed compared to the coordinate system
     * (when you scroll right, the content actually needs to go left). So the
     * returned value can be simply added to the content's {@code X}
     * coordinate.
     *
     * @return Number of pixels scrolled horizontally during the gesture
     */
    public double getTotalDeltaX() {
        return totalDeltaX;
    }

    private double totalDeltaY;

    /**
     * Gets the cumulative vertical scroll amount for the whole gesture.
     * This value should be interpreted as a number of pixels to scroll
     * relatively to the state at the beginning of the gesture.
     * Contains zeros for mouse wheel scrolling.
     * <p>
     * The sign of the value is reversed compared to the coordinate system
     * (when you scroll down, the content actually needs to go up). So the
     * returned value can be simply added to the content's {@code Y}
     * coordinate.
     *
     * @return Number of pixels to scrolled vertically during the gesture
     */
    public double getTotalDeltaY() {
        return totalDeltaY;
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

    private int touchCount;

    /**
     * Gets number of touch points that caused this event. For non-touch source
     * devices as mouse wheel and for inertia events after gesture finish
     * it returns zero.
     * @return Number of touch points that caused this event
     */
    public int getTouchCount() {
        return touchCount;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static ScrollEvent impl_scrollEvent(EventType<ScrollEvent> eventType,
            double _scrollX, double _scrollY,
            double _totalScrollX, double _totalScrollY,
            HorizontalTextScrollUnits _scrollTextXUnits, double _scrollTextX,
            VerticalTextScrollUnits _scrollTextYUnits, double _scrollTextY,
            int _touchPoints,
            double _x, double _y,
            double _screenX, double _screenY,
            boolean _shiftDown,
            boolean _controlDown,
            boolean _altDown,
            boolean _metaDown,
            boolean _direct,
            boolean _inertia
          )
    {
        return new ScrollEvent(eventType, _scrollX, _scrollY,
                _totalScrollX, _totalScrollY,
                _scrollTextXUnits, _scrollTextX,
                _scrollTextYUnits, _scrollTextY,
                _touchPoints,
                _x, _y, _screenX, _screenY,
                _shiftDown, _controlDown, _altDown, _metaDown, 
                _direct, _inertia);
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
        sb.append(", totalDeltaX = ").append(getTotalDeltaX())
                .append(", totalDeltaY = ").append(getTotalDeltaY());
        sb.append(", textDeltaXUnits = ").append(getTextDeltaXUnits())
                .append(", textDeltaX = ").append(getTextDeltaX());
        sb.append(", textDeltaYUnits = ").append(getTextDeltaYUnits())
                .append(", textDeltaY = ").append(getTextDeltaY());
        sb.append(", touchCount = ").append(getTouchCount());
        sb.append(", x = ").append(getX()).append(", y = ").append(getY());
        sb.append(isDirect() ? ", direct" : ", indirect");

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
