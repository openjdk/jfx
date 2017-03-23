/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.NamedArg;
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
 * <pre><code>
    {@literal node.setOnScroll(new EventHandler<ScrollEvent>()} {
        {@literal @Override} public void handle(ScrollEvent event) {
            node.setTranslateX(node.getTranslateX() + event.getDeltaX());
            node.setTranslateY(node.getTranslateY() + event.getDeltaY());
        }
    });
</code></pre>
 * <p>
 * A scroll event handler on text-based component behaving
 * according to system settings on all platforms should contain following logic:
 * <pre>{@code
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
 }</pre>
 *
 * @since JavaFX 2.0
 */
public final class ScrollEvent extends GestureEvent {

    private static final long serialVersionUID = 20121107L;

    /**
     * Common supertype for all scroll event types.
     */
    public static final EventType<ScrollEvent> ANY =
            new EventType<ScrollEvent> (GestureEvent.ANY, "ANY_SCROLL");

    /**
     * This event occurs when user performs a scrolling action such as
     * rotating mouse wheel or dragging a finger over touch screen.
     */
    public static final EventType<ScrollEvent> SCROLL =
            new EventType<ScrollEvent> (ScrollEvent.ANY, "SCROLL");

    /**
     * This event occurs when a scrolling gesture is detected. It doesn't
     * occur for mouse wheel scrolling.
     * @since JavaFX 2.2
     */
    public static final EventType<ScrollEvent> SCROLL_STARTED =
            new EventType<ScrollEvent> (ScrollEvent.ANY, "SCROLL_STARTED");

    /**
     * This event occurs when a scrolling gesture ends. It doesn't
     * occur for mouse wheel scrolling.
     * @since JavaFX 2.2
     */
    public static final EventType<ScrollEvent> SCROLL_FINISHED =
            new EventType<ScrollEvent> (ScrollEvent.ANY, "SCROLL_FINISHED");


    private ScrollEvent(Object source, EventTarget target,
                       final EventType<ScrollEvent> eventType,
                       double x, double y,
                       double screenX, double screenY,
                       boolean shiftDown,
                       boolean controlDown,
                       boolean altDown,
                       boolean metaDown,
                       boolean direct,
                       boolean inertia,
                       double deltaX, double deltaY,
                       double totalDeltaX, double totalDeltaY,
                       double multiplierX, double multiplierY,
                       HorizontalTextScrollUnits textDeltaXUnits, double textDeltaX,
                       VerticalTextScrollUnits textDeltaYUnits, double textDeltaY,
                       int touchCount, PickResult pickResult) {

        super(source, target, eventType, x, y, screenX, screenY,
                shiftDown, controlDown, altDown, metaDown, direct, inertia,
                pickResult);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.totalDeltaX = totalDeltaX;
        this.totalDeltaY = totalDeltaY;
        this.textDeltaXUnits = textDeltaXUnits;
        this.textDeltaX = textDeltaX;
        this.textDeltaYUnits = textDeltaYUnits;
        this.textDeltaY = textDeltaY;
        this.touchCount = touchCount;
        this.multiplierX = multiplierX;
        this.multiplierY = multiplierY;
    }

    /**
     * Constructs new ScrollEvent event.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @param direct true if the event was caused by direct input device. See {@link #isDirect() }
     * @param inertia if represents inertia of an already finished gesture.
     * @param deltaX horizontal scroll amount
     * @param deltaY vertical scroll amount
     * @param totalDeltaX cumulative horizontal scroll amount
     * @param totalDeltaY cumulative vertical scroll amount
     * @param textDeltaXUnits units for horizontal text-based scroll amount
     * @param textDeltaX horizontal text-based scroll amount
     * @param textDeltaYUnits units for vertical text-based scroll amount
     * @param textDeltaY vertical text-based scroll amount
     * @param touchCount number of touch points
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates and the target
     * @since JavaFX 8.0
     */
    public ScrollEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target,
                       final @NamedArg("eventType") EventType<ScrollEvent> eventType,
                       @NamedArg("x") double x, @NamedArg("y") double y,
                       @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
                       @NamedArg("shiftDown") boolean shiftDown,
                       @NamedArg("controlDown") boolean controlDown,
                       @NamedArg("altDown") boolean altDown,
                       @NamedArg("metaDown") boolean metaDown,
                       @NamedArg("direct") boolean direct,
                       @NamedArg("inertia") boolean inertia,
                       @NamedArg("deltaX") double deltaX, @NamedArg("deltaY") double deltaY,
                       @NamedArg("totalDeltaX") double totalDeltaX, @NamedArg("totalDeltaY") double totalDeltaY,
                       @NamedArg("textDeltaXUnits") HorizontalTextScrollUnits textDeltaXUnits, @NamedArg("textDeltaX") double textDeltaX,
                       @NamedArg("textDeltaYUnits") VerticalTextScrollUnits textDeltaYUnits, @NamedArg("textDeltaY") double textDeltaY,
                       @NamedArg("touchCount") int touchCount, @NamedArg("pickResult") PickResult pickResult) {
        this(source, target, eventType, x, y, screenX, screenY, shiftDown, controlDown,
                altDown, metaDown, direct, inertia, deltaX, deltaY, totalDeltaX,
                totalDeltaY, 1.0, 1.0, textDeltaXUnits, textDeltaX, textDeltaYUnits, textDeltaY,
                touchCount, pickResult);
    }

    /**
     * Constructs new ScrollEvent event with null source and target
     * @param eventType The type of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @param direct true if the event was caused by direct input device. See {@link #isDirect() }
     * @param inertia if represents inertia of an already finished gesture.
     * @param deltaX horizontal scroll amount
     * @param deltaY vertical scroll amount
     * @param totalDeltaX cumulative horizontal scroll amount
     * @param totalDeltaY cumulative vertical scroll amount
     * @param textDeltaXUnits units for horizontal text-based scroll amount
     * @param textDeltaX horizontal text-based scroll amount
     * @param textDeltaYUnits units for vertical text-based scroll amount
     * @param textDeltaY vertical text-based scroll amount
     * @param touchCount number of touch points
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @since JavaFX 8.0
     */
    public ScrollEvent(final @NamedArg("eventType") EventType<ScrollEvent> eventType,
            @NamedArg("x") double x, @NamedArg("y") double y,
            @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
            @NamedArg("shiftDown") boolean shiftDown,
            @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown,
            @NamedArg("metaDown") boolean metaDown,
            @NamedArg("direct") boolean direct,
            @NamedArg("inertia") boolean inertia,
            @NamedArg("deltaX") double deltaX, @NamedArg("deltaY") double deltaY,
            @NamedArg("totalDeltaX") double totalDeltaX, @NamedArg("totalDeltaY") double totalDeltaY,
            @NamedArg("textDeltaXUnits") HorizontalTextScrollUnits textDeltaXUnits, @NamedArg("textDeltaX") double textDeltaX,
            @NamedArg("textDeltaYUnits") VerticalTextScrollUnits textDeltaYUnits, @NamedArg("textDeltaY") double textDeltaY,
            @NamedArg("touchCount") int touchCount,
            @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, shiftDown, controlDown,
                altDown, metaDown, direct, inertia, deltaX, deltaY, totalDeltaX,
                totalDeltaY, 1.0, 1.0, textDeltaXUnits, textDeltaX, textDeltaYUnits, textDeltaY,
                touchCount, pickResult);
    }

    /**
     * Constructs new ScrollEvent event with null source and target
     * @param eventType The type of the event.
     * @param x The x with respect to the scene.
     * @param y The y with respect to the scene.
     * @param screenX The x coordinate relative to screen.
     * @param screenY The y coordinate relative to screen.
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @param direct true if the event was caused by direct input device. See {@link #isDirect() }
     * @param inertia if represents inertia of an already finished gesture.
     * @param deltaX horizontal scroll amount
     * @param deltaY vertical scroll amount
     * @param totalDeltaX cumulative horizontal scroll amount
     * @param totalDeltaY cumulative vertical scroll amount
     * @param multiplierX an X multiplier used to convert wheel rotations to pixels
     * @param multiplierY an Y multiplier used to convert wheel rotations to pixels
     * @param textDeltaXUnits units for horizontal text-based scroll amount
     * @param textDeltaX horizontal text-based scroll amount
     * @param textDeltaYUnits units for vertical text-based scroll amount
     * @param textDeltaY vertical text-based scroll amount
     * @param touchCount number of touch points
     * @param pickResult pick result. Can be null, in this case a 2D pick result
     *                   without any further values is constructed
     *                   based on the scene coordinates
     * @since JavaFX 8.0
     */
    public ScrollEvent(final @NamedArg("eventType") EventType<ScrollEvent> eventType,
                       @NamedArg("x") double x, @NamedArg("y") double y,
                       @NamedArg("screenX") double screenX, @NamedArg("screenY") double screenY,
                       @NamedArg("shiftDown") boolean shiftDown,
                       @NamedArg("controlDown") boolean controlDown,
                       @NamedArg("altDown") boolean altDown,
                       @NamedArg("metaDown") boolean metaDown,
                       @NamedArg("direct") boolean direct,
                       @NamedArg("inertia") boolean inertia,
                       @NamedArg("deltaX") double deltaX, @NamedArg("deltaY") double deltaY,
                       @NamedArg("totalDeltaX") double totalDeltaX, @NamedArg("totalDeltaY") double totalDeltaY,
                       @NamedArg("multiplierX") double multiplierX, @NamedArg("multiplierY") double multiplierY,
                       @NamedArg("textDeltaXUnits") HorizontalTextScrollUnits textDeltaXUnits, @NamedArg("textDeltaX") double textDeltaX,
                       @NamedArg("textDeltaYUnits") VerticalTextScrollUnits textDeltaYUnits, @NamedArg("textDeltaY") double textDeltaY,
                       @NamedArg("touchCount") int touchCount,
                       @NamedArg("pickResult") PickResult pickResult) {
        this(null, null, eventType, x, y, screenX, screenY, shiftDown, controlDown,
                altDown, metaDown, direct, inertia, deltaX, deltaY, totalDeltaX,
                totalDeltaY, multiplierX, multiplierY, textDeltaXUnits, textDeltaX,
                textDeltaYUnits, textDeltaY, touchCount, pickResult);
    }

    private final double deltaX;

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
     * @return Number of pixels to scroll horizontally
     */
    public double getDeltaX() {
        return deltaX;
    }

    private final double deltaY;

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
     * @return Number of pixels to scroll vertically
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
     * @since JavaFX 2.2
     */
    public double getTotalDeltaX() {
        return totalDeltaX;
    }

    private final double totalDeltaY;

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
     * @since JavaFX 2.2
     */
    public double getTotalDeltaY() {
        return totalDeltaY;
    }

    private final HorizontalTextScrollUnits textDeltaXUnits;

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

    private final VerticalTextScrollUnits textDeltaYUnits;

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

    private final double textDeltaX;

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

    private final double textDeltaY;

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

    private final int touchCount;

    /**
     * Gets number of touch points that caused this event. For non-touch source
     * devices as mouse wheel and for inertia events after gesture finish
     * it returns zero.
     * @return Number of touch points that caused this event
     * @since JavaFX 2.2
     */
    public int getTouchCount() {
        return touchCount;
    }

    private final double multiplierX;

    /**
     * Gets the multiplier used to convert mouse wheel rotation units to pixels
     * @return the x multiplier
     * @since JavaFX 8.0
     */
    public double getMultiplierX() {
        return multiplierX;
    }

    private final double multiplierY;

    /**
     * Gets the multiplier used to convert mouse wheel rotation units to pixels
     * @return the y multiplier
     * @since JavaFX 8.0
     */
    public double getMultiplierY() {
        return multiplierY;
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
        sb.append(", x = ").append(getX()).append(", y = ").append(getY())
                .append(", z = ").append(getZ());
        sb.append(isDirect() ? ", direct" : ", indirect");

        if (isInertia()) {
            sb.append(", inertia");
        }

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
        sb.append(", pickResult = ").append(getPickResult());

        return sb.append("]").toString();
    }

    @Override
    public ScrollEvent copyFor(Object newSource, EventTarget newTarget) {
        return (ScrollEvent) super.copyFor(newSource, newTarget);
    }

    /**
     * Creates a copy of the given event with the given fields substituted.
     * @param newSource the new source of the copied event
     * @param newTarget the new target of the copied event
     * @param type the new eventType
     * @return the event copy with the fields substituted
     * @since JavaFX 8.0
     */
    public ScrollEvent copyFor(Object newSource, EventTarget newTarget, EventType<ScrollEvent> type) {
        ScrollEvent e = copyFor(newSource, newTarget);
        e.eventType = type;
        return e;
    }

    @Override
    public EventType<ScrollEvent> getEventType() {
        return (EventType<ScrollEvent>) super.getEventType();
    }

    /**
     * Horizontal text-based scrolling units.
     * @since JavaFX 2.0
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
     * @since JavaFX 2.0
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
