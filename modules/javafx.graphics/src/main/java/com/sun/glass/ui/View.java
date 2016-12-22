/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import com.sun.glass.events.MouseEvent;
import com.sun.glass.events.ViewEvent;

import java.lang.annotation.Native;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

public abstract class View {

    @Native public final static int GESTURE_NO_VALUE = Integer.MAX_VALUE;
    @Native public final static double GESTURE_NO_DOUBLE_VALUE = Double.NaN;

    @Native public final static byte IME_ATTR_INPUT                 = 0x00;
    @Native public final static byte IME_ATTR_TARGET_CONVERTED      = 0x01;
    @Native public final static byte IME_ATTR_CONVERTED             = 0x02;
    @Native public final static byte IME_ATTR_TARGET_NOTCONVERTED   = 0x03;
    @Native public final static byte IME_ATTR_INPUT_ERROR           = 0x04;

    final static boolean accessible = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
        String force = System.getProperty("glass.accessible.force");
        if (force != null) return Boolean.parseBoolean(force);

        /* By default accessibility is enabled for Mac 10.9 or greater and Windows 7 or greater. */
        try {
            String platform = Platform.determinePlatform();
            String major = System.getProperty("os.version").replaceFirst("(\\d+)\\.\\d+.*", "$1");
            String minor = System.getProperty("os.version").replaceFirst("\\d+\\.(\\d+).*", "$1");
            int v = Integer.parseInt(major) * 100 + Integer.parseInt(minor);
            return (platform.equals(Platform.MAC) && v >= 1009) ||
                   (platform.equals(Platform.WINDOWS) && v >= 601);
        } catch (Exception e) {
            return false;
        }
    });

    public static class EventHandler {
        public void handleViewEvent(View view, long time, int type) {
        }
        public void handleKeyEvent(View view, long time, int action,
                int keyCode, char[] keyChars, int modifiers) {
        }
        public void handleMenuEvent(View view, int x, int y, int xAbs,
                int yAbs, boolean isKeyboardTrigger) {
        }
        public void handleMouseEvent(View view, long time, int type, int button,
                                     int x, int y, int xAbs, int yAbs,
                                     int modifiers, boolean isPopupTrigger, boolean isSynthesized)
        {
        }

        /**
         * A Scroll event handler.
         *
         * The lines argument:
         * &gt; 0 - a number of lines to scroll per each 1.0 of deltaY scroll amount
         * == 0 - the scroll amount is in pixel units
         * &lt; 0 - the scrolling should be performed by pages. Each 1.0 of scroll amount
         * corresponds to exactly one page of scrollable content.
         *
         * Similarly, the chars argument specifies the number of characters
         * to scroll per 1.0 of the deltaX scrolling amount.
         * If the parameter is zero, the deltaX represents the number of
         * pixels to scroll.
         *
         * The defaultLines and defaultChars arguments contain the system-default
         * values of lines and chars. This can be used by the app to compute
         * the ratio of current settings and default settings and adjust the
         * pixel values accordingly.
         *
         * Multiplers are used when an app receives a non-zero unit values (i.e.
         * either the lines or chars are not zeroes), but wants instead get delta
         * values in pixels. In this case the app needs to multiply the deltas
         * on the provided multiplier parameter.
         */
        public void handleScrollEvent(View view, long time,
                int x, int y, int xAbs, int yAbs,
                double deltaX, double deltaY, int modifiers, int lines, int chars,
                int defaultLines, int defaultChars,
                double xMultiplier, double yMultiplier)
        {
        }

        public void handleInputMethodEvent(long time, String text,
                int[] clauseBoundary,
                int[] attrBoundary, byte[] attrValue,
                int commitCount, int cursorPos) {
        }

        public double[] getInputMethodCandidatePos(int offset) {
            return null;
        }

        public void handleDragStart(View view, int button, int x, int y, int xAbs, int yAbs,
                ClipboardAssistance dropSourceAssistant) {
        }

        public void handleDragEnd(View view, int performedAction) {
        }

        public int handleDragEnter(View view, int x, int y, int xAbs, int yAbs,
                int recommendedDropAction, ClipboardAssistance dropTargetAssistant) {
            return recommendedDropAction;
        }

        public int handleDragOver(View view, int x, int y, int xAbs, int yAbs,
                int recommendedDropAction, ClipboardAssistance dropTargetAssistant) {
            return recommendedDropAction;
        }

        public void handleDragLeave(View view, ClipboardAssistance dropTargetAssistant) {
        }

        public int handleDragDrop(View view, int x, int y, int xAbs, int yAbs,
                int recommendedDropAction, ClipboardAssistance dropTargetAssistant) {
            return Clipboard.ACTION_NONE;
        }

        /**
         * Touch event handler. Called when touch event occures.
         * Always followed with one ore more #handleNextTouchEvent() calls
         * and a single #handleEndTouchEvent() call.
         *
         * @param isDirect if event reported by direct or indirect touch device;
         *        touch screen is an example of direct touch device and
         *        touch pad is an example of indirect one
         * @param touchEventCount indicates number of #handleNextTouchEvent() calls
         *        that will follow this method call.
         */
        public void handleBeginTouchEvent(View view, long time, int modifiers,
                                          boolean isDirect, int touchEventCount) {
        }

        /**
         * Touch event handler. Called for every touch point in some touch event.
         *
         * If the touch event has been emitted with direct touch device
         * (touch screen) then x and y arguments designate touch point position
         * relative to the top-left corner of the view and xAbs and yAbs
         * arguments designate position relative to the top-left corner of the
         * screen. Both positions are measured in pixels.
         *
         * If the touch event has been emitted with indirect touch device
         * (touch pad) then x and y arguments designate normalized touch point
         * position. It is measured between (0,0) and (10000,10000), where (0,0)
         * is the top-left and (10000,10000) is the bottom-right position on
         * the indirect touch input device (touch pad). xAbs and yAbs
         * arguments are equal values of x and y arguments respectively.
         *
         * @see #handleBeginTouchEvent(com.sun.glass.ui.View, long, int, boolean, int)
         *
         * @param type touch event type. One of constants declared in
         *        #com.sun.glass.events.TouchEvent class.
         * @param touchId touch point identifier;
         *        every touch point has its own unique identifier;
         *        the identifier remains the same across multiple calls of
         *        #handleNextTouchEvent method for the same touch point until
         *        it is not released.
         * @param x the X coordinate of the touch point;
         * @param y the Y coordinate of the touch point;
         * @param xAbs absolute X coordinate of the touch point;
         * @param yAbs absolute Y coordinate of the touch point;
         */
        public void handleNextTouchEvent(View view, long time, int type,
                                         long touchId, int x, int y, int xAbs,
                                         int yAbs) {
        }

        /**
         * Touch event handler. Called to notify that all #handleNextTouchEvent
         * methods corresponding to some touch event have been called already.
         *
         * @see #handleBeginTouchEvent(com.sun.glass.ui.View, long, int, boolean, int)
         */
        public void handleEndTouchEvent(View view, long time) {
        }

        /**
         * Scroll gesture handler.
         *
         * If underlying system supports coordinates for gestures then x and y
         * arguments designate gesture position relative to the top-left
         * corner of the view and xAbs and yAbs designate gesture position
         * relative to the top-left corner of the screen. For gestures emitted
         * from direct touch input device (touch screen) positions are measured
         * in pixels. For gestures emitted from indirect touch input device
         * (touch pad) positions are normalized. For details of normalized
         * touch input position see #handleBeginTouchEvent method.
         *
         * If underlying system doesn't support coordinates for gestures then
         * x and y arguments designate mouse position relative to the top-left
         * corner of the view and xAbs and yAbs designate mouse position
         * relative to the top-left corner of the screen. Positions are measured
         * in pixels.
         *
         * If gesture handler is called to notify end of gesture, i.e. value of
         * type argument is equal to
         * com.sun.glass.events.GestureEvent.GESTURE_FINISHED constant then
         * x, y, xAbs and yAbs arguments may be set to View.GESTURE_NO_VALUE
         * constant indicating no data is available. This is implementation
         * specific behavior.
         *
         * Values of dx and dy arguments are always 0.0 if type argument
         * is set to com.sun.glass.events.GestureEvent.GESTURE_FINISHED
         * constant.
         *
         * For description of isDirect argument see #handleBeginTouchEvent
         * method.
         *
         * @param type gesture state. One of constants declared in
         *        #com.sun.glass.events.GestureEvent class.
         * @param isInertia if gesture is caused by inertia.
         * @param touchCount number of touch points at
         *        the moment of gesture execution; it is always set to
         *        View.GESTURE_NO_VALUE constant if value of type argument is
         *        set to com.sun.glass.events.GestureEvent.GESTURE_FINISHED
         *        constant
         * @param x the X coordinate of the gesture;
         * @param y the Y coordinate of the gesture;
         * @param xAbs absolute X coordinate of the gesture;
         * @param yAbs absolute Y coordinate of the gesture;
         * @param dx horizontal scroll delta. Positive if scrolling from
         *        left to right, non-positive otherwise
         * @param dy vertical scroll delta. Positive if scrolling from
         *        up to down, non-positive otherwise
         * @param totaldx total horizontal scroll calculated from all
         *        sequential scroll gestures, i.e. sum of all 'dx' values from
         *        previous sequential calls to this method
         * @param totaldy total vertical scroll calculated from all
         *        sequential scroll gestures, i.e. sum of all 'dy' values from
         *        previous sequential calls to this method
         * @param multiplierX the X multiplier
         * @param multiplierY the Y multiplier
         *
         * Multiplers are used when an app receives a non-zero unit values (i.e.
         * either the lines or chars are not zeroes), but wants instead get delta
         * values in pixels. In this case the app needs to multiply the deltas
         * on the provided multiplier parameter.
         */
        public void handleScrollGestureEvent(View view, long time, int type,
                                             int modifiers, boolean isDirect,
                                             boolean isInertia, int touchCount,
                                             int x, int y, int xAbs, int yAbs,
                                             double dx, double dy,
                                             double totaldx, double totaldy,
                                             double multiplierX, double multiplierY) {
        }

        /**
         * Zoom gesture handler.
         *
         * For description of isDirect argument see #handleBeginTouchEvent
         * method.
         *
         * For description of isInertia argument see #handleScrollGestureEvent
         * method.
         *
         * For description of type, x,y, xAbs and yAbs arguments
         * see #handleBeginTouchEvent method.
         *
         * If underlying system doesn't support measurement of expansion value
         * in zoom gestures then expansion and totalexpansion arguments are
         * always set to View.GESTURE_NO_DOUBLE_VALUE.
         *
         * If type argument is set to
         * com.sun.glass.events.GestureEvent.GESTURE_FINISHED constant value of
         * scale argument is always set to View.GESTURE_NO_DOUBLE_VALUE constant
         * and expansion argument is always 0.0.
         *
         * @param scale current zoom delta; the value is multiplicative
         *        and not additive.
         * @param expansion current expansion delta. Measured in pixels on
         *        direct touch input devices and normalized values on indirect
         *        touch input devices. See #handleBeginTouchEvent for
         *        description of units of indirect touch input devices.
         * @param totalscale total zoom calculated from all
         *        sequential zoom gestures, i.e. sum of all 'scale' values from
         *        previous sequential calls to this method
         * @param totalexpansion total expansion calculated from all
         *        sequential zoom gestures, i.e. sum of all 'expansion' values
         *        from previous sequential calls of this method
         */
        public void handleZoomGestureEvent(View view, long time, int type,
                                           int modifiers, boolean isDirect,
                                           boolean isInertia, int x, int y,
                                           int xAbs, int yAbs, double scale,
                                           double expansion, double totalscale,
                                           double totalexpansion) {
        }

        /**
         * Rotation gesture handler.
         *
         * For description of isDirect argument see #handleBeginTouchEvent
         * method.
         *
         * For description of isInertia argument see #handleScrollGestureEvent
         * method.
         *
         * For description of type, x,y, xAbs and yAbs arguments
         * see #handleBeginTouchEvent method.
         *
         * @param dangle current angle delta in degrees. Positive for clockwise
         *        rotation
         * @param totalangle total angle calculated from all
         *        sequential rotation gestures, i.e. sum of all 'dangle' values
         *        from previous sequential calls of this method
         */
        public void handleRotateGestureEvent(View view, long time, int type,
                                             int modifiers, boolean isDirect,
                                             boolean isInertia, int x, int y,
                                             int xAbs, int yAbs, double dangle,
                                             double totalangle) {
        }

        /**
         * Swipe gesture handler.
         *
         * For description of isDirect argument see #handleBeginTouchEvent
         * method.
         *
         * For description of isInertia and touchCount arguments
         * see #handleScrollGestureEvent method.
         *
         * For description of type, x,y, xAbs and yAbs arguments
         * see #handleBeginTouchEvent method.
         *
         * @param dir gesture direction.
         *        One of constants defined in com.sun.glass.events.SwipeGesture
         *        class.
         */
        public void handleSwipeGestureEvent(View view, long time, int type,
                                            int modifiers, boolean isDirect,
                                            boolean isInertia, int touchCount,
                                            int dir, int x, int y, int xAbs,
                                            int yAbs) {
        }

        public Accessible getSceneAccessible() {
            return null;
        }
    }

    public static long getMultiClickTime() {
        Application.checkEventThread();
        return Application.GetApplication().staticView_getMultiClickTime();
    }

    public static int getMultiClickMaxX() {
        Application.checkEventThread();
        return Application.GetApplication().staticView_getMultiClickMaxX();
    }

    public static int getMultiClickMaxY() {
        Application.checkEventThread();
        return Application.GetApplication().staticView_getMultiClickMaxY();
    }

    protected abstract void _enableInputMethodEvents(long ptr, boolean enable);
    protected void _finishInputMethodComposition(long ptr) {
        // Action needed only on Windows.
    }

    /*
        Read by the checkNotClosed method which could be called from lock/unlock on render thread
     */
    private volatile long ptr; // Native handle (NSView*, or internal structure pointer)
    private Window window; // parent window
    private EventHandler eventHandler;

    private int width = -1;     // not set
    private int height = -1;    // not set

    private boolean isValid = false; // true between ViewEvent.Add & ViewEvent.REMOVE
    private boolean isVisible = false;
    private boolean inFullscreen = false;

    static final public class Capability {
        // we need these for native code
        @Native static final public int k3dKeyValue                     = 0;
        @Native static final public int kSyncKeyValue                   = 1;
        @Native static final public int k3dProjectionKeyValue           = 2;
        @Native static final public int k3dProjectionAngleKeyValue      = 3;
        @Native static final public int k3dDepthKeyValue                = 4;
        @Native static final public int kHiDPIAwareKeyValue             = 5;

        static final public Object k3dKey                       = Integer.valueOf(k3dKeyValue); // value must be Boolean
        static final public Object kSyncKey                     = Integer.valueOf(kSyncKeyValue); // value must be Boolean
        static final public Object k3dProjectionKey             = Integer.valueOf(k3dProjectionKeyValue); // value must be Boolean
        static final public Object k3dProjectionAngleKey        = Integer.valueOf(k3dProjectionAngleKeyValue); // value must be Float
        static final public Object k3dDepthKey                  = Integer.valueOf(k3dDepthKeyValue); // value must be Integer(depth), where depth = 0, 4, 8, 16, 32etc
        static final public Object kHiDPIAwareKey               = Integer.valueOf(kHiDPIAwareKeyValue); // value must be Boolean; default = false (i.e. NOT HiDPI-aware)
    }


    protected abstract long _create(Map capabilities);
    protected View() {
        Application.checkEventThread();
        this.ptr = _create(Application.GetApplication().getDeviceDetails());
        if (this.ptr == 0L) {
            throw new RuntimeException("could not create platform view");
        }
    }

    private void checkNotClosed() {
        if (this.ptr == 0L) {
            throw new IllegalStateException("The view has already been closed");
        }
    }

    public boolean isClosed() {
        Application.checkEventThread();
        return this.ptr == 0L;
    }

    protected abstract long _getNativeView(long ptr);
    /**
     * On Windows ptr is a pointer to a native structure.
     * However, for external clients of the API, a HWND has to be returned.
     * Hence the native method.
     */
    public long getNativeView() {
        Application.checkEventThread();
        checkNotClosed();
        return _getNativeView(this.ptr);
    }

    /** Only used on Mac when run inside a plugin */
    public int getNativeRemoteLayerId(String serverName) {
        Application.checkEventThread();
        throw new RuntimeException("This operation is not supported on this platform");
    }

    public Window getWindow() {
        Application.checkEventThread();
        return this.window;
    }

    protected abstract int _getX(long ptr);
    /** X coordinate relative to the host (window or applet). */
    public int getX() {
        Application.checkEventThread();
        checkNotClosed();
        return _getX(this.ptr);
    }

    protected abstract int _getY(long ptr);
    /** Y coordinate relative to the host (window or applet). */
    public int getY() {
        Application.checkEventThread();
        checkNotClosed();
        return _getY(this.ptr);
    }

    public int getWidth() {
        Application.checkEventThread();
        return this.width;
    }

    public int getHeight() {
        Application.checkEventThread();
        return this.height;
    }

    protected abstract void _setParent(long ptr, long parentPtr);
    // Window calls the method from Window.setView()
    // package private
    void setWindow(Window window) {
        Application.checkEventThread();
        checkNotClosed();
        this.window = window;
        _setParent(this.ptr, window == null ? 0L : window.getNativeHandle());
        this.isValid = this.ptr != 0 && window != null;
    }

    // package private
    void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    protected abstract boolean _close(long ptr);
    public void close() {
        Application.checkEventThread();
        if (this.ptr == 0) {
            return;
        }
        if (isInFullscreen()) {
            _exitFullscreen(this.ptr, false);
        }
        Window host = getWindow();
        if (host != null) {
            host.setView(null); // will call this.setWindow(null)
        }
        this.isValid = false;
        _close(this.ptr);
        this.ptr = 0;
    }

    public EventHandler getEventHandler() {
        Application.checkEventThread();
        return this.eventHandler;
    }

    public void setEventHandler(EventHandler eventHandler) {
        Application.checkEventThread();
        this.eventHandler = eventHandler;
    }

    //-------- EVENTS --------//

    private void handleViewEvent(long time, int type) {
        if (this.eventHandler != null) {
            this.eventHandler.handleViewEvent(this, time, type);
        }
    }

    private void handleKeyEvent(long time, int action,
            int keyCode, char[] keyChars, int modifiers) {
        if (this.eventHandler != null) {
            this.eventHandler.handleKeyEvent(this, time, action, keyCode, keyChars, modifiers);
        }
    }

    private void handleMouseEvent(long time, int type, int button, int x, int y,
                                  int xAbs, int yAbs,
                                  int modifiers, boolean isPopupTrigger,
                                  boolean isSynthesized) {
        if (eventHandler != null) {
            eventHandler.handleMouseEvent(this, time, type, button, x, y, xAbs,
                                          yAbs, modifiers,
                                          isPopupTrigger, isSynthesized);
        }
    }

    private void handleMenuEvent(int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger) {
        if (this.eventHandler != null) {
            this.eventHandler.handleMenuEvent(this, x, y, xAbs, yAbs, isKeyboardTrigger);
        }
    }

    public void handleBeginTouchEvent(View view, long time, int modifiers,
                                      boolean isDirect, int touchEventCount) {
        if (eventHandler != null) {
            eventHandler.handleBeginTouchEvent(view, time, modifiers, isDirect,
                    touchEventCount);
        }
    }

    public void handleNextTouchEvent(View view, long time, int type,
                                     long touchId, int x, int y, int xAbs,
                                     int yAbs) {
        if (eventHandler != null) {
            eventHandler.handleNextTouchEvent(view, time, type, touchId, x, y, xAbs, yAbs);
        }
    }

    public void handleEndTouchEvent(View view, long time) {
        if (eventHandler != null) {
            eventHandler.handleEndTouchEvent(view, time);
        }
    }

    public void handleScrollGestureEvent(View view, long time, int type,
                                         int modifiers, boolean isDirect,
                                         boolean isInertia, int touchCount,
                                         int x, int y, int xAbs, int yAbs,
                                         double dx, double dy, double totaldx,
                                         double totaldy, double multiplierX,
                                         double multiplierY) {
        if (eventHandler != null) {
            eventHandler.handleScrollGestureEvent(view, time, type, modifiers, isDirect,
                    isInertia, touchCount, x, y, xAbs, yAbs,
                    dx, dy, totaldx, totaldy, multiplierX, multiplierY);
        }
    }

    public void handleZoomGestureEvent(View view, long time, int type,
                                       int modifiers, boolean isDirect,
                                       boolean isInertia, int originx,
                                       int originy, int originxAbs,
                                       int originyAbs, double scale,
                                       double expansion, double totalscale,
                                       double totalexpansion) {
        if (eventHandler != null) {
            eventHandler.handleZoomGestureEvent(view, time, type, modifiers, isDirect,
                                     isInertia, originx, originy, originxAbs,
                                     originyAbs, scale, expansion, totalscale,
                                     totalexpansion);
        }
    }

    public void handleRotateGestureEvent(View view, long time, int type,
                                         int modifiers, boolean isDirect,
                                         boolean isInertia, int originx,
                                         int originy, int originxAbs,
                                         int originyAbs, double dangle,
                                         double totalangle) {
        if (eventHandler != null) {
            eventHandler.handleRotateGestureEvent(view, time, type, modifiers, isDirect,
                    isInertia, originx, originy, originxAbs,
                    originyAbs, dangle, totalangle);
        }
    }

    public void handleSwipeGestureEvent(View view, long time, int type,
                                        int modifiers, boolean isDirect,
                                        boolean isInertia, int touchCount,
                                        int dir, int originx, int originy,
                                        int originxAbs, int originyAbs) {
        if (eventHandler != null) {
            eventHandler.handleSwipeGestureEvent(view, time, type, modifiers, isDirect,
                    isInertia, touchCount, dir, originx,
                    originy, originxAbs, originyAbs);
        }
    }

    private void handleInputMethodEvent(long time, String text, int[] clauseBoundary,
                int[] attrBoundary, byte[] attrValue,
                int commitCount, int cursorPos) {
        if (this.eventHandler != null) {
            this.eventHandler.handleInputMethodEvent(time, text, clauseBoundary,
                attrBoundary, attrValue,
                commitCount, cursorPos);
        }
    }

    public void enableInputMethodEvents(boolean enable) {
        Application.checkEventThread();
        checkNotClosed();
        _enableInputMethodEvents(this.ptr, enable);
    }

    public void finishInputMethodComposition() {
        Application.checkEventThread();
        checkNotClosed();
        _finishInputMethodComposition(this.ptr);
    }

    private double[] getInputMethodCandidatePos(int offset) {
        if (this.eventHandler != null) {
            return this.eventHandler.getInputMethodCandidatePos(offset);
        }
        return null;
    }

    private void handleDragStart(int button, int x, int y, int xAbs, int yAbs,
            ClipboardAssistance dropSourceAssistant) {
        if (this.eventHandler != null) {
            this.eventHandler.handleDragStart(this, button, x, y, xAbs, yAbs, dropSourceAssistant);
        }
    }

    private void handleDragEnd(int performedAction) {
        if (this.eventHandler != null) {
            this.eventHandler.handleDragEnd(this, performedAction);
        }
    }

    private int handleDragEnter(int x, int y, int xAbs, int yAbs,
            int recommendedDropAction, ClipboardAssistance dropTargetAssistant) {
        if (this.eventHandler != null) {
            return this.eventHandler.handleDragEnter(this, x, y, xAbs, yAbs, recommendedDropAction, dropTargetAssistant);
        } else {
            return recommendedDropAction;
        }
    }

    private int handleDragOver(int x, int y, int xAbs, int yAbs,
            int recommendedDropAction, ClipboardAssistance dropTargetAssistant) {
        if (this.eventHandler != null) {
            return this.eventHandler.handleDragOver(this, x, y, xAbs, yAbs, recommendedDropAction, dropTargetAssistant);
        } else {
            return recommendedDropAction;
        }
    }

    private void handleDragLeave(ClipboardAssistance dropTargetAssistant) {
        if (this.eventHandler != null) {
            this.eventHandler.handleDragLeave(this, dropTargetAssistant);
        }
    }

    private int handleDragDrop(int x, int y, int xAbs, int yAbs,
            int recommendedDropAction, ClipboardAssistance dropTargetAssistant) {
        if (this.eventHandler != null) {
            return this.eventHandler.handleDragDrop(this, x, y, xAbs, yAbs, recommendedDropAction, dropTargetAssistant);
        } else {
            return Clipboard.ACTION_NONE;
        }
    }

    //-------- DRAWING --------//
    protected abstract void _scheduleRepaint(long ptr);
    /** marks native surface dirty, so the system itself will create repaint event
     * */
    public void scheduleRepaint() {
        Application.checkEventThread();
        checkNotClosed();
        _scheduleRepaint(this.ptr);
    }

    protected abstract void _begin(long ptr);
    /** prepares to painting by locking native surface
     *
     * Called on the render thread
     */
    public void lock() {
        checkNotClosed();
        _begin(this.ptr);
    }

    protected abstract void _end(long ptr);
    /** ends painting by unlocking native surface and flushing
     * flushes surface (if flush == true) or discard it (flush == false)
     *
     * Called on the render thread
     */
    public void unlock() {
        checkNotClosed();
        _end(this.ptr);
    }

    protected abstract int _getNativeFrameBuffer(long ptr);

    /**
     * Called on the renderer thread and must be between lock and unlock
     */
    public int getNativeFrameBuffer() {
        return _getNativeFrameBuffer(this.ptr);
    }


    protected abstract void _uploadPixels(long ptr, Pixels pixels);
    /**
     * This method dumps the pixels on to the view.
     *
     * NOTE: On MS Windows calling this method is REQUIRED for
     * transparent windows in order to update them.
     */
    public void uploadPixels(Pixels pixels) {
        Application.checkEventThread();
        checkNotClosed();
        lock();
        try {
            _uploadPixels(this.ptr, pixels);
        } finally {
            unlock();
        }
    }


    //-------- FULLSCREEN --------//

    protected abstract boolean _enterFullscreen(long ptr, boolean animate, boolean keepRatio, boolean hideCursor);
    public boolean enterFullscreen(boolean animate, boolean keepRatio, boolean hideCursor) {
        Application.checkEventThread();
        checkNotClosed();
        return _enterFullscreen(this.ptr, animate, keepRatio, hideCursor);
    }

    protected abstract void _exitFullscreen(long ptr, boolean animate);
    public void exitFullscreen(boolean animate) {
        Application.checkEventThread();
        checkNotClosed();
        _exitFullscreen(this.ptr, animate);
    }

    public boolean isInFullscreen() {
        Application.checkEventThread();
        return this.inFullscreen;
    }

    public boolean toggleFullscreen(boolean animate, boolean keepRatio, boolean hideCursor) {
        Application.checkEventThread();
        checkNotClosed();
        if (!this.inFullscreen) {
            enterFullscreen(animate, keepRatio, hideCursor);
        } else {
            exitFullscreen(animate);
        }

        _scheduleRepaint(this.ptr);

        return this.inFullscreen;
    }

    public void updateLocation() {
        notifyView(ViewEvent.MOVE);
    }


    //-------- DELEGATE NOTIFICATIONS --------//

    protected void notifyView(int type) {
        //System.err.println("    notifyView: "+ViewEvent.getTypeString(type)+" on thread"+Thread.currentThread());
        if (type == ViewEvent.REPAINT) {
            if (isValid) {
                handleViewEvent(System.nanoTime(), type);
            }
        }
        else
        {
            boolean synthesizeMOVE = false;

            switch (type) {
                case ViewEvent.REMOVE:
                    isValid = false;
                    synthesizeMOVE = true;
                    break;
                case ViewEvent.ADD:
                    isValid = true;
                    synthesizeMOVE = true;
                    break;
                case ViewEvent.FULLSCREEN_ENTER:
                    this.inFullscreen = true;
                    synthesizeMOVE = true;
                    if (getWindow() != null) {
                        getWindow().notifyFullscreen(true);
                    }
                    break;
                case ViewEvent.FULLSCREEN_EXIT:
                    this.inFullscreen = false;
                    synthesizeMOVE = true;
                    if (getWindow() != null) {
                        getWindow().notifyFullscreen(false);
                    }
                    break;
                case ViewEvent.MOVE:
                case ViewEvent.RESIZE:
                    break;
                default:
                    System.err.println("Unknown view event type: " + type);
                    return;
            }

            handleViewEvent(System.nanoTime(), type);

            if (synthesizeMOVE) {
                // Generate MOVE event to update current insets. Native code may
                // send additional MOVE events when it detects insets change.
                handleViewEvent(System.nanoTime(), ViewEvent.MOVE);
            }
        }
    }

    protected void notifyResize(int width, int height) {
        if (this.width == width && this.height == height) {
            return;
        }

        this.width = width;
        this.height = height;
        handleViewEvent(System.nanoTime(), ViewEvent.RESIZE);
    }

    /*
     * x, y, width, heigth define the "dirty" rect
     */
    protected void notifyRepaint(int x, int y, int width, int height) {
        notifyView(ViewEvent.REPAINT);
    }


    // ------------ MENU EVENT HANDLING -----------------
//    protected void notifyMenu(int type, int button, int x, int y, int xAbs, int yAbs, int keyCode, char[] keyChars, int modifiers) {
        protected void notifyMenu(int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger) {
        handleMenuEvent(x, y, xAbs, yAbs, isKeyboardTrigger);
    }

    // ------------ MOUSE EVENTS HANDLING -----------------

    // Synchronized on the Main thread of the underlying native system
    private static WeakReference<View> lastClickedView = null;
    private static int lastClickedButton;
    private static long lastClickedTime;
    private static int lastClickedX, lastClickedY;
    private static int clickCount;
    private static boolean dragProcessed = false;

    protected void notifyMouse(int type, int button, int x, int y, int xAbs,
                               int yAbs, int modifiers, boolean isPopupTrigger,
                               boolean isSynthesized) {
        // gznote: optimize - only call for undecorated Windows!
        if (this.window != null) {
            // handled by window (programmatical move/resize)
            if (this.window.handleMouseEvent(type, button, x, y, xAbs, yAbs)) {
                // The evnet has been processed by Glass
                return;
            }
        }

        long now = System.nanoTime();
        if (type == MouseEvent.DOWN) {
            View lastClickedView = View.lastClickedView == null ? null : View.lastClickedView.get();

            if (lastClickedView == this &&
                    lastClickedButton == button &&
                    (now - lastClickedTime) <= 1000000L*getMultiClickTime() &&
                    Math.abs(x - lastClickedX) <= getMultiClickMaxX() &&
                    Math.abs(y - lastClickedY) <= getMultiClickMaxY())
            {
                clickCount++;
            } else {
                clickCount = 1;

                View.lastClickedView = new WeakReference<View>(this);
                lastClickedButton = button;
                lastClickedX = x;
                lastClickedY = y;
            }

            lastClickedTime = now;
        }

        handleMouseEvent(now, type, button, x, y, xAbs, yAbs,
                         modifiers, isPopupTrigger, isSynthesized);

        if (type == MouseEvent.DRAG) {
            // Send the handleDragStart() only once per a drag gesture
            if (!dragProcessed) {
                notifyDragStart(button, x, y, xAbs, yAbs);
                dragProcessed = true;
            }
        } else {
            dragProcessed = false;
        }
    }

    // ------------- END OF MOUSE EVENTS -----------------

    protected void notifyScroll(int x, int y, int xAbs, int yAbs,
            double deltaX, double deltaY, int modifiers, int lines, int chars,
            int defaultLines, int defaultChars,
            double xMultiplier, double yMultiplier)
    {
        if (this.eventHandler != null) {
            this.eventHandler.handleScrollEvent(this, System.nanoTime(),
                    x, y, xAbs, yAbs, deltaX, deltaY, modifiers, lines, chars,
                    defaultLines, defaultChars, xMultiplier, yMultiplier);
        }
    }

    protected void notifyKey(int type, int keyCode, char[] keyChars, int modifiers) {
        handleKeyEvent(System.nanoTime(), type, keyCode, keyChars, modifiers);
    }

    protected void notifyInputMethod(String text, int[] clauseBoundary,
        int[] attrBoundary, byte[] attrValue,
        int committedTextLength, int caretPos, int visiblePos) {
        handleInputMethodEvent(System.nanoTime(), text, clauseBoundary,
                attrBoundary, attrValue, committedTextLength, caretPos);
    }

    protected double[] notifyInputMethodCandidatePosRequest(int offset) {
        double[] ret = getInputMethodCandidatePos(offset);
        if (ret == null) {
            ret = new double[2];
            ret[0] = 0.0;
            ret[1] = 0.0;
        }
        return ret;
    }

    private ClipboardAssistance dropSourceAssistant;
    protected void notifyDragStart(int button, int x, int y, int xAbs, int yAbs) {
        dropSourceAssistant = new ClipboardAssistance(Clipboard.DND) {
            @Override public void actionPerformed(int performedAction) {
                // on Windows called from DnD modal loop
                // on Mac the View is the drag delegate and calls notifyDragEnd directly
                notifyDragEnd(performedAction);
            }
        };
        //DnD loop is inside dropSourceAssistant.flush()
        handleDragStart(button, x, y, xAbs, yAbs, dropSourceAssistant);
        //utilize dropSourceAssistant if DnD was not started.
        if (dropSourceAssistant != null) {
            dropSourceAssistant.close();
            dropSourceAssistant = null;
        }
    }

    protected void notifyDragEnd(int performedAction) {
        handleDragEnd(performedAction);
        if (dropSourceAssistant != null) {
            dropSourceAssistant.close();
            dropSourceAssistant = null;
        }
    }

    ClipboardAssistance  dropTargetAssistant;
    // callback for native code
    protected int notifyDragEnter(int x, int y, int xAbs, int yAbs, int recommendedDropAction) {
        dropTargetAssistant = new ClipboardAssistance(Clipboard.DND) {
            @Override public void flush() {
                throw new UnsupportedOperationException("Flush is forbidden from target!");
            }
        };
        return handleDragEnter(x, y, xAbs, yAbs, recommendedDropAction, dropTargetAssistant);
    }

    // callback for native code
    protected int notifyDragOver(int x, int y, int xAbs, int yAbs, int recommendedDropAction) {
        return handleDragOver(x, y, xAbs, yAbs, recommendedDropAction, dropTargetAssistant);
    }

    // callback for native code
    protected void notifyDragLeave() {
        handleDragLeave(dropTargetAssistant);
        dropTargetAssistant.close();
    }

    // callback for native code
    // gznote: should be renamed to notifyDragDrop/notifyDragPerformed to be consistent
    protected int notifyDragDrop(int x, int y, int xAbs, int yAbs, int recommendedDropAction) {
        int performedAction = handleDragDrop(x, y, xAbs, yAbs, recommendedDropAction, dropTargetAssistant);
        dropTargetAssistant.close();
        return performedAction;
    }

    public void notifyBeginTouchEvent(int modifiers, boolean isDirect,
                                      int touchEventCount) {
        handleBeginTouchEvent(this, System.nanoTime(), modifiers, isDirect,
                              touchEventCount);
    }

    public void notifyNextTouchEvent(int type, long touchId, int x, int y,
                                     int xAbs, int yAbs) {
        handleNextTouchEvent(this, System.nanoTime(), type, touchId, x, y, xAbs,
                             yAbs);
    }

    public void notifyEndTouchEvent() {
        handleEndTouchEvent(this, System.nanoTime());
    }

    public void notifyScrollGestureEvent(int type, int modifiers,
                                         boolean isDirect, boolean isInertia,
                                         int touchCount, int x, int y, int xAbs,
                                         int yAbs, double dx, double dy,
                                         double totaldx, double totaldy,
                                         double multiplierX, double multiplierY) {
        handleScrollGestureEvent(this, System.nanoTime(), type, modifiers,
                                 isDirect, isInertia, touchCount, x, y, xAbs,
                                 yAbs, dx, dy, totaldx, totaldy, multiplierX, multiplierY);
    }

    public void notifyZoomGestureEvent(int type, int modifiers, boolean isDirect,
                                       boolean isInertia, int originx,
                                       int originy, int originxAbs,
                                       int originyAbs, double scale,
                                       double expansion, double totalscale,
                                       double totalexpansion) {
        handleZoomGestureEvent(this, System.nanoTime(), type, modifiers,
                               isDirect, isInertia, originx, originy, originxAbs,
                               originyAbs, scale, expansion, totalscale,
                               totalexpansion);
    }

    public void notifyRotateGestureEvent(int type, int modifiers,
                                         boolean isDirect, boolean isInertia,
                                         int originx, int originy,
                                         int originxAbs, int originyAbs,
                                         double dangle, double totalangle) {
        handleRotateGestureEvent(this, System.nanoTime(), type, modifiers,
                                 isDirect, isInertia, originx, originy,
                                 originxAbs, originyAbs, dangle, totalangle);
    }

    public void notifySwipeGestureEvent(int type, int modifiers,
                                        boolean isDirect, boolean isInertia,
                                        int touchCount, int dir, int originx,
                                        int originy, int originxAbs,
                                        int originyAbs) {
        handleSwipeGestureEvent(this, System.nanoTime(), type, modifiers,
                                isDirect, isInertia, touchCount, dir, originx,
                                originy, originxAbs, originyAbs);
    }

    /**
     * Returns the accessible object for the view.
     * This method is called by JNI code when the
     * platform requested the accessible peer for the view.
     * On Windows it happens on WM_GETOBJECT.
     * On Mac it happens on NSView#accessibilityAttributeNames.
     */
    long getAccessible() {
        Application.checkEventThread();
        checkNotClosed();
        if (accessible) {
            Accessible acc = eventHandler.getSceneAccessible();
            if (acc != null) {
                acc.setView(this);
                return acc.getNativeAccessible();
            }
        }
        return 0L;
    }
}
