/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.lens;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.List;
import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.MouseEvent;
import com.sun.glass.events.ViewEvent;
import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.EventLoop;
import com.sun.glass.ui.Menu;
import com.sun.glass.ui.MenuBar;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Robot;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.Timer;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import sun.util.logging.PlatformLogger;

final class LensApplication extends Application {

    /** Bit to indicate that a device has touch support */
    private static final int DEVICE_TOUCH = 0;
    /** Bit to indicate that a device has multitouch support */
    private static final int DEVICE_MULTITOUCH = 1;
    /** Bit to indicate that a device has relative motion pointer support */
    private static final int DEVICE_POINTER = 2;
    /** Bit to indicate that a device has arrow keys and a select key */
    private static final int DEVICE_5WAY = 3;
    /** Bit to indicate that a device has a full PC keyboard */
    private static final int DEVICE_PC_KEYBOARD = 4;
    /** Largest bit used in device capability bitmasks */
    private static final int DEVICE_MAX = 4;
    /** A running count of the numbers of devices with each device capability */
    private int[] deviceFlags = new int[DEVICE_MAX + 1];

    Menu windowMenu;
    Menu editMenu;
    Menu fileMenu;

    private static final Object invokeAndWaitLock = new Object();
    private static Runnable waitingFor;

    private static int activeEventLoopThreads = 0;
    private static final Object activeEventLoopLock = new Object();

    private static boolean doComposite = true;

    static private boolean isInitialized = false;

    // setup for JNI
    private static native void _initIDs();

    // initialize any native display connection/state
    private static native boolean _initialize();

    private static native void _notifyRenderingEnd();

    private EventLoop dndEventLoop;

    private static void initLibrary() {
        final String lensProperty = "glass.lens";
        final String platform = AccessController.doPrivileged(
        new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(lensProperty, "");
            }
        });

        doComposite = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.getBoolean("doNativeComposite");
            }
        });

        if (isInitialized) {
            //make sure we make this only once
            return;
        }

        LensLogger.getLogger().info("LensApplication initialization");

        if (platform.equals("")) {
            LensLogger.getLogger().severe(
                "System property " + lensProperty + " not defined");
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Application.loadNativeLibrary("glass-lens-" + platform);
                return null;
            }
        });
        _initIDs();
        isInitialized = true;
        LensLogger.getLogger().info("LensApplication initialization done");
    }

    static {
        LensApplication.initLibrary();
    }

    @Override
    protected void staticView_notifyRenderingEnd() {
        //do nothing
    }

    //cache the singleton object for native layer usage
    native void registerApplication();

    //package protected to be used only by LensPlatformFactory
    LensApplication() {
        super();
        LensLogger.getLogger().fine("LensApplication ctor called, registering in"
                                    + " native layer");
        registerApplication();
    }

    private static abstract class Event {
        abstract void dispatch();
    }

    private static class RunnableEvent extends Event {
        private boolean wait;
        private Runnable runnable;

        RunnableEvent(boolean wait, Runnable runnable) {
            this.wait = wait;
            this.runnable = runnable;
        }

        @Override
        void dispatch() {
            runnable.run();
            if (wait) {
                synchronized (invokeAndWaitLock) {
                    waitingFor = null;
                    invokeAndWaitLock.notify();
                }
            }
        }

        @Override
        public String toString() {
            return "RunnableEvent[runnable=" + runnable + ",wait=" + wait + "]";
        }
    }

    @Override
    public boolean hasWindowManager() {
        return false;
    }

    /**
     * This class is used to handle key events
     *
     */
    private static class LensKeyEvent extends Event {

        //the view object to notify
        private LensView view;

        //type of the event (pressed, released...)
        // as defined in KeyEvent.java
        private int type;

        //key code
        private int keyCode;

        //bit mask of modifiers (shift, ctrl...)
        private int modifiers;

        // A buffer holding the char key sequence, can be 0 length
        private char[] chars;

        LensKeyEvent(LensView view, int type, int keyCode, int modifiers,
                     char[] chars) {
            this.view = view;
            this.type = type;
            this.keyCode = keyCode;
            this.modifiers = modifiers;
            this.chars = chars;
        }

        @Override
        void dispatch() {
            view._notifyKey(type, keyCode, chars, modifiers);
        }

        @Override
        public String toString() {
            return "LensKeyEvent[view=" + view
                   + ",type=" + type
                   + ",keyCode=" + keyCode
                   + ",modifiers=" + modifiers
                   + ",chars=" + String.valueOf(chars)
                   + "]";
        }
    }

    /**
     * This class is used to handle window related events
     *
     */
    private static class LensWindowEvent extends Event {

        static enum EType {
            CLOSE, DESTROY, EXPOSE, FOCUS, MOVE, RESIZE, UNGRAB
        };

        private EType type;

        // The window object to notify
        private LensWindow window;
        //The event that invoked this notification, as described
        // in WindowEvent class
        private int windowEvent;
        //Window parameters for update
        private int x;
        private int y;
        private int width;
        private int height;

        /**
         * Generic constructor, used when no parameters need to be
         * updated
         *
         * @param type LensApplication event
         * @param window the window to notify
         * @param windowEvent one of the events described in WindowEvent
         *                    class
         */
        LensWindowEvent(EType type, LensWindow window, int windowEvent) {
            this.type = type;
            this.window         = window;
            this.windowEvent    = windowEvent;
        }

        /**
         * Use this constructor when window parameters have been changed
         *
         *
         * @param type LensApplication event
         * @param window the window to notify
         * @param windowEvent one of the events described in WindowEvent
         *                    class
         * @param x
         * @param y
         * @param width
         * @param height
         */
        LensWindowEvent(EType type, LensWindow window, int windowEvent,
                        int x, int y,
                        int width, int height) {
            this.type = type;
            this.window         = window;
            this.windowEvent    = windowEvent;
            this.x              = x;
            this.y              = y;
            this.width          = width;
            this.height         = height;
        }

        @Override
        void dispatch() {
            switch (type) {
                case FOCUS:
                    window._notifyFocus(windowEvent);
                    break;
                case MOVE:
                    window._notifyMove(x, y);
                    break;
                case RESIZE:
                    window._notifyResize(windowEvent, width, height);
                    break;
                case UNGRAB:
                    window._notifyFocusUngrab();
                    break;
                case DESTROY:
                    window._notifyDestroy();
                    break;
                case CLOSE:
                    window._notifyClose();
                    break;
                case EXPOSE:
                    window._notifyExpose(x, y, width, height);
                    break;
                default:
                    LensLogger.getLogger().severe(
                        "Unrecognized window event type");
            }
        }

        @Override
        public String toString() {
            return super.toString() + "[window=" + window
                   + ",type=" + type
                   + ",windowEvent=" + windowEvent
                   + ",x=" + x
                   + ",y=" + y
                   + ",width=" + width
                   + ",height=" + height
                   + "]";
        }

    }
    private static class LensMouseEvent extends Event {

        private LensView target;
        private int action;
        private int x, y, absx, absy;
        private int button;
        private int modifiers;
        private boolean isPopupTrigger;
        private boolean isSynthesized;

        LensMouseEvent(LensView target,
                       int action,
                       int x, int y,
                       int absx, int absy,
                       int button,
                       int modifiers,
                       boolean isPopupTrigger,
                       boolean isSynthesized) {
            this.target = target;
            this.action = action;
            this.x = x;
            this.y = y;
            this.absx = absx;
            this.absy = absy;
            this.button = button;
            this.modifiers = modifiers;
            this.isPopupTrigger = isPopupTrigger;
            this.isSynthesized = isSynthesized;
        }

        @Override
        void dispatch() {
            target._notifyMouse(
                action, button,
                x, y,
                absx, absy,
                modifiers,
                isPopupTrigger, isSynthesized);
        }

        @Override
        public String toString() {
            return "LensMouseEvent[target=" + target
                   + ",action=" + action
                   + ",x=" + x
                   + ",y=" + y
                   + ",absx=" + absx
                   + ",absy=" + absy
                   + ",button=" + button
                   + ",modifiers=" + modifiers
                   + ",isPopupTrigger=" + isPopupTrigger
                   + ",isSynthesized=" + isSynthesized
                   + "]";
        }
    }

    private static class LensScrollEvent extends Event {
        private LensView target;
        private int x, y;
        private int absx, absy;
        private double deltaX, deltaY;
        private int modifiers;
        private int lines;
        private int chars;
        private int defaultLines;
        private int defaultChars;
        private double xMultiplier, yMultiplier;

        LensScrollEvent(LensView target,
                        int x, int y, int absx, int absy,
                        double deltaX, double deltaY, int modifiers, int lines, int chars,
                        int defaultLines, int defaultChars,
                        double xMultiplier, double yMultiplier) {

            this.target = target;
            this.x = x;
            this.y = y;
            this.absx = absx;
            this.absy = absy;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.modifiers = modifiers;
            this.lines = lines;
            this.chars = chars;
            this.defaultLines = defaultLines;
            this.defaultChars = defaultChars;
            this.xMultiplier = xMultiplier;
            this.yMultiplier = yMultiplier;
        }

        @Override
        void dispatch() {
            target._notifyScroll(
                x, y,
                absx, absy,
                deltaX, deltaY,
                modifiers,
                lines, chars,
                defaultLines, defaultChars,
                xMultiplier, yMultiplier);
        }

        @Override
        public String toString() {
            return "LensScrollEvent[target=" + target
                   + ",x=" + x
                   + ",y=" + y
                   + ",absx=" + absx
                   + ",absy=" + absy
                   + ",deltaX=" + deltaX
                   + ",deltaY=" + deltaY
                   + ",modifiers=" + modifiers
                   + ",lines=" + lines
                   + ",chars=" + chars
                   + ",defaultLines=" + defaultLines
                   + ",defaultChars=" + defaultChars
                   + ",xMultiplier=" + xMultiplier
                   + ",yMultiplier=" + yMultiplier
                   + "]";
        }
    }

    private static class LensTouchEvent extends Event {

        private LensView view;
        private int state;
        private long id;
        private int x;
        private int y;
        private int absX;
        private int absY;

        LensTouchEvent(LensView view, int state, long id,
                       int x, int y, int absX, int absY) {
            this.view = view;
            this.state = state;
            this.id = id;
            this.x = x;
            this.y = y;
            this.absX = absX;
            this.absY = absY;
        }

        @Override
        void dispatch() {
            LensTouchInputSupport.postTouchEvent(view, state, id, x, y, absX, absY);

        }

        @Override
        public String toString() {
            return "LensTouchEvent[view=" + view
                   + ",state=" + state
                   + ",id=" + id
                   + ",x=" + x
                   + ",y=" + y
                   + ",absX=" + absX
                   + ",absY=" + absY
                   + "]";
        }
    }


    private static class LensViewEvent extends Event {
        private LensView target;
        private int x, y, width, height;
        private int viewEventType;

        LensViewEvent(LensView view, int viewEventType,
                      int x, int y, int width, int height) {
            this.target = view;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.viewEventType = viewEventType;
        }

        @Override
        void dispatch() {
            target._notifyViewEvent(viewEventType);
        }

        @Override
        public String toString() {
            return "LensViewEvent[target=" + target
                   + ", x=" + x
                   + ", y=" + y
                   + ", width=" + width
                   + ", height=" + height
                   + ", event type code " + viewEventType
                   + ", event type name "
                   + ViewEvent.getTypeString(viewEventType);
        }
    }

    private class LensDragEvent extends Event {
        int x, y, absx, absy;
        DragActions action;
        LensView view;

        //This variable are used to overcome an enum limitation (you can't
        //use switch clause on custom values enum)
        final int ENTER = DragActions.ENTER.getValue();
        final int LEAVE = DragActions.LEAVE.getValue();
        final int OVER = DragActions.OVER.getValue();
        final int DROP = DragActions.DROP.getValue();

        LensDragEvent(LensView view, int x, int y, int absx, int absy, DragActions action) {
            this.absx = absx;
            this.absy = absy;
            this.x = x;
            this.y = y;
            this.action = action;
            this.view = view;
        }
        @Override
        void dispatch() {
            if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                LensLogger.getLogger().finest("processing drag  " + action);
            }
            switch (action) {
                case ENTER:
                    view._notifyDragEnter(x, y, absx, absy, Clipboard.ACTION_COPY_OR_MOVE);
                    break;
                case LEAVE:
                    view._notifyDragLeave();
                    break;
                case OVER:
                    view._notifyDragOver(x, y, absx, absy, Clipboard.ACTION_COPY_OR_MOVE);
                    break;
                case DROP:
                    view._notifyDragDrop(x, y, absx, absy, Clipboard.ACTION_COPY_OR_MOVE);
                    leaveDndEventLoop(null);
                    break;
                default:
                    return;
            }
        }
    }


    private class LensMenuEvent extends Event {
        LensView view;
        int x;
        int y;
        int xAbs;
        int yAbs;
        boolean isKeyboardTrigger;

        LensMenuEvent(LensView view, int x, int y, int xAbs,
                      int yAbs, boolean isKeyboardTrigger) {
            this.view = view;
            this.x = x;
            this.y = y;
            this.xAbs = xAbs;
            this.yAbs = yAbs;
            this.isKeyboardTrigger = isKeyboardTrigger;
        }

        @Override
        void dispatch() {
            view._notifyMenu(x, y, xAbs, yAbs, isKeyboardTrigger);
        }

        @Override
        public String toString() {
            return "LensMenuEvent[view=" + view
                   + ", x=" + x
                   + ", y=" + y
                   + ", absx=" + xAbs
                   + ", absy=" + yAbs
                   + ", isKeyboardTrigger=" + isKeyboardTrigger + "]";
        }
    }

    private class LensDeviceEvent extends Event {
        private int flags;
        private boolean attach;
        LensDeviceEvent(int flags, boolean attach) {
            this.flags = flags;
            this.attach = attach;
        }
        @Override
        void dispatch() {
            for (int i = 0; i <= DEVICE_MAX; i++) {
                if ((flags & (1 << i)) != 0) {
                    if (attach) {
                        deviceFlags[i] ++;
                    } else {
                        deviceFlags[i] --;
                    }
                }
            }
        }
    }

    private final LinkedList<Event> eventList = new LinkedList<Event>();

    private void postEvent(Event e) {
        if (Thread.currentThread() == getEventThread()) {
            try {
                e.dispatch();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            synchronized (eventList) {
                eventList.addLast(e);
                eventList.notify();
            }
        }
    }

    private static class RunLoopControl {
        boolean active; // thread should continue to process events.
        Object release; // object to return with on leave nested
    }

    // our stack of nested run loops - note using LinkedList because we
    // are already using that class for events.
    LinkedList<RunLoopControl> activeRunLoops = new LinkedList<RunLoopControl>();

    @Override
    protected Object _enterNestedEventLoop() {
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("_enterNestedEventLoop");
        }

        // we are being called on the current active event thread
        // via dispatch, so it is stalled until we return.

        // start our nested loop, which will block until that exits
        Object ret = _runLoop();

        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("Resuming event loop");
        }

        // and return the value that was passed into leaveNested
        return ret;
    }


    @Override
    protected void _leaveNestedEventLoop(Object retValue) {
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("_leaveNestedEventLoop");
        }

        // we are being called from dispatch of the current running
        // event thread. We want to cause this thread to exit, and
        // restart the nested on.

        RunLoopControl current = activeRunLoops.pop();
        assert current != null;

        // let the current run loop die when we return to dispatch.
        current.active = false;
        // and give it the ret object so it will return it to the
        // blocked nesting call.
        current.release = retValue;

        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("_leaveNestedEventLoop");
        }

        // when we return from this dispatched event, we will exit
        // because we are no longer active, and then the nested
        // call can return the release value we just provided.

    }

    private Object _runLoop() {
        final RunLoopControl control = new RunLoopControl();

        //push this new instance on the stack
        activeRunLoops.push(control);

        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("Starting event loop");
        }

        control.active = true;
        while (control.active) {
            Event event;
            synchronized (eventList) {
                if (eventList.isEmpty()) {
                    try {
                        eventList.wait();
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                if (eventList.isEmpty()) {
                    continue;
                }
                event = eventList.removeFirst();
            }

            if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                LensLogger.getLogger().fine("Processing " + event);
            }

            try {
                event.dispatch();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("Leaving event loop");
        }

        return control.release;
    }

    private static  void registerEventLoop() {
        synchronized (activeEventLoopLock) {
            activeEventLoopThreads ++;
            LensLogger.getLogger().info(
                "activeEventLoopThreads := " + activeEventLoopThreads);
            activeEventLoopLock.notifyAll();
        }
    }

    private static  void unregisterEventLoop() {
        synchronized (activeEventLoopLock) {
            activeEventLoopThreads --;
            LensLogger.getLogger().info(
                "activeEventLoopThreads := " + activeEventLoopThreads);
            activeEventLoopLock.notifyAll();
        }
    }

    private static  void waitEventLoopsToFinish() {
        synchronized (activeEventLoopLock) {
            try {
                LensLogger.getLogger().info("Waiting for all event loops to finish");
                while (activeEventLoopThreads > 0) {
                    LensLogger.getLogger().info(
                        "activeEventLoopThreads = " + activeEventLoopThreads);
                    activeEventLoopLock.wait();
                }
            } catch (InterruptedException e) {
                LensLogger.getLogger().severe("interrupted");
            }
        }
    }

    @Override
    protected void runLoop(Runnable launchable) {

        if (!_initialize()) {
            LensLogger.getLogger().severe("Display failed initialization");
            throw new RuntimeException("Display failed initialization");
        }

        _invokeLater(launchable);
        Thread toolkitThread = new Thread(
        new Runnable() {
            @Override
            public void run() {
                _runLoop();
            }
        }, "Lens Event Thread");
        setEventThread(toolkitThread);
        toolkitThread.start();
    }

    private static int nativeThreadCounter = 0;

    // Call into the native impl to start up any native device input queues
    // needed. Native will upcall to createNativeEventThread to start any threads.
    private native void startNativeEventLoop(final LensApplication lensApp,
                                             long nativeEventHandler,
                                             long nativeWindow);

    // Note: this Native event thread is designed to listen for events
    // from native sources. For example, it may poll some native input devices.
    // It is *not* the same as the FX event thred.
    private static void createNativeEventThread(final long nativeEventHandler,
                                                final long data) {

        final LensApplication lensApplication =
            (LensApplication)Application.GetApplication();

        Thread eventThread = new Thread(new Runnable() {
            @Override
            public void run() {
                registerEventLoop();
                lensApplication.startNativeEventLoop(lensApplication,
                                                     nativeEventHandler, data);

                //when the native function return
                //event loop has exited
                unregisterEventLoop();
            }
        }, ("Lens Native Event Thread " + (nativeThreadCounter++)));

        LensLogger.getLogger().info("Starting native event thread");

        eventThread.setDaemon(true);
        eventThread.start();
    }

    Object enterDnDEventLoop() {
        dndEventLoop = createEventLoop();
        return dndEventLoop.enter();
    }

    void leaveDndEventLoop(Object value) {
        dndEventLoop.leave(value);
    }

    native void shutdown();

    @Override
    protected void finishTerminating() {
        LensLogger.getLogger().info("Finishing terminating");

        shutdown();
        synchronized (eventList) {
            eventList.clear();
            while (!activeRunLoops.isEmpty()) {
                RunLoopControl control = activeRunLoops.pop();
                control.active = false;
            }
            eventList.notify();
        }

        super.finishTerminating();
    }

    @Override
    protected boolean _supportsTransparentWindows() {
        return true;
    }

    @Override protected boolean _supportsUnifiedWindows() {
        return false;
    }

    //*******************************************************************
    // Runloop/Event queue support additions

    //Window events

    /**
     * handles the following window events WindowEvent.MINIMIZE /
     * MAXIMIZE /RESTORE / RESIZE
     * See Window.java::notifyResize() for more information
     *
     * @param window the window object which this event belongs to
     * @param  eventType WindowEvent.MINIMIZE / MAXIMIZE /RESTORE /
     *                   RESIZE
     * @param width new width
     * @param height new height
     */
    protected void notifyWindowResize(LensWindow window,
                                      int eventType,
                                      int width, int height) {
        if (LensLogger.isLogging(PlatformLogger.INFO)) {
            LensLogger.getLogger().info(
                "Resize " + window + " to " + width + "x" + height);
        }
        if (window != null) {
            postEvent(new LensWindowEvent(LensWindowEvent.EType.RESIZE,
                                          window,
                                          eventType, 0, 0,
                                          width, height));
        }
    }

    /**
     * Notify JFX that the window have been moved and provide the
     * new coordinates.
     *
     * This notification can be either response for JFX request to
     * change the window location, or user have changed it. The
     * later case will happen when window a manager is used.
     *
     * @param window The window which this event belongs to
     * @param x new X coordinate of the window
     * @param y new Y Coordinate of the window
     */
    protected void notifyWindowMove(LensWindow window, int x, int y) {
        if (LensLogger.isLogging(PlatformLogger.INFO)) {
            LensLogger.getLogger().info(
                "Move " + window + " to " + x + "," + y);
        }
        postEvent(new LensWindowEvent(LensWindowEvent.EType.MOVE,
                                      window,
                                      WindowEvent.MOVE,
                                      x, y,
                                      0, 0));
    }

    /**
     * This notification informs JFX on window events that doesn't
     * requires additional information for handling the
     * notification. For example window have gained/lost focus.
     *
     * @param window The window which this event belongs to
     * @param windowEvent the event type as defined in WindowEvent
     *                    class.
     */
    protected void notifyWindowEvent(LensWindow window, int windowEvent) {

        String eventName;
        LensWindowEvent.EType etype = null;
        switch (windowEvent) {
            case WindowEvent.FOCUS_GAINED:
                eventName = "Focus Gained";
                etype = LensWindowEvent.EType.FOCUS;
                break;
            case WindowEvent.FOCUS_LOST:
                eventName = "Focus Lost";
                etype = LensWindowEvent.EType.FOCUS;
                break;
            case WindowEvent.DESTROY:
                eventName = "Window Destroy";
                etype = LensWindowEvent.EType.DESTROY;
                break;
            case WindowEvent.CLOSE:
                eventName = "window Close";
                etype = LensWindowEvent.EType.CLOSE;
                break;
            case WindowEvent.FOCUS_UNGRAB:
                etype = LensWindowEvent.EType.UNGRAB;
                eventName = "Ungrab";
                break;
            default:
                eventName = "Unknown event code=" + windowEvent + " Ignoring";
        }

        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine(
                "notifyWindowEvent eventType = " + eventName);
        }

        if (etype != null) {
            postEvent(new LensWindowEvent(etype, window, windowEvent));
        }
    }

    protected void windowExpose(LensWindow window, int x, int y, int width,
                                int height) {
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine(
                "Expose " + window + " "
                + x + "," + y + "+" + width + "x" + height);
        }
        postEvent(new LensWindowEvent(LensWindowEvent.EType.EXPOSE,
                                      window, WindowEvent.RESIZE,
                                      x, y, width, height));
    }


    //Input events


    /**
     * Notify key event from native layer
     *
     * @param window  the window which the view is related to
     * @param type event type (KeyEvent.PRESS ...)
     * @param keyCode key code for the event (KeyEvent.VK_*)
     * @param modifiers bit mask of key modifiers
     *                  (KeyEvent.MODIFIER_*)
     * @param chars char sequence buffer. can be 0 length, must not
     *              be null
     */
    private void notifyKeyEvent(LensView view, int type , int keyCode,
                                int modifiers, char[] chars) {
        try {
            if (LensLogger.isLogging(PlatformLogger.FINER)) {
                LensLogger.getLogger().finer("Key event on " + view);
            }
            postEvent(new LensKeyEvent(view, type, keyCode,
                                       modifiers , chars));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Notify mouse event from native layer
     *
     * @param window the window which the view is related to
     * @param eventType one of MouseEvent constants
     * @param x location of event inside the view
     * @param y location of event inside the view
     * @param absx location of event on the screen
     * @param absy location of event on the screen
     * @param button currently pressed button, required only in applicable events
     *               such as MouseEvent.DOWN
     * @param modifiers mask of currently pressed special keys and mouse
     *                  buttons. always required
     * @param isPopupTrigger true when event is context menu hint (usually right
     *                       button release)
     * @param isSynthesized used when event is logical such MouseEvent.CLICK
     */

    void notifyMouseEvent(LensView view, int eventType,
                          int x, int y, int absx, int absy,
                          int button, int modifiers,
                          boolean isPopupTrigger, boolean isSynthesized) {

        try {
            if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                LensLogger.getLogger().finest("Mouse event on " + view);
            }

            //continue process events only if not already consumed
            if (!handleDragEvents(view, eventType, x, y, absx, absy, button, modifiers)) {
                postEvent(new LensMouseEvent(view, eventType,
                                             x, y, absx, absy,
                                             button, modifiers,
                                             isPopupTrigger,
                                             isSynthesized));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ////// Drag And Drop Support

    //used to mark the button which triggered the drag - when its release
    //the drag ends
    private int cachedButtonPressed = MouseEvent.BUTTON_NONE;
    //The View that the drag is currently hoover above
    private LensView dragView = null;
    //mask of operations done on current drag event
    //mask values are ORed from DragActions enum
    private int dragActionsPreformed =  DragActions.NONE.getValue();

    //Mark if drag processing have started by upper levels,
    //value changes when notifyDragStart is called
    private boolean dragStarted = false;


    //Possible actioins for drag events
    private enum DragActions {
        NONE(0,     "NONE"),
        ENTER(1 << 1, "ENTER"),
        LEAVE(1 << 2, "LEAVE"),
        OVER(1 << 3, "OVER"),
        DROP(1 << 4, "DROP");
        public int value;
        private String name;
        DragActions(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return name;
        }

    };

    /**
     * This funtion should be only called from LensDnDClipboard after it has
     * been initialized.
     * After this method have called it meand that we are inside nested event
     * loop, which is resposible to handle all drag events, all other mouse
     * events are discarded until Drag Drop is detected.
     */
    void notifyDragStart() {
        dragStarted = true;
    }
    /**
     * Transforms mouse events into drag events when drag detected. When this
     * functions returns true, it means that the event is been consumed by this
     * method and no further processing is required
     *
     * @param view the view which owns the event
     * @param eventType type of event, one of MouseEvent constants
     * @param x location of event inside the view
     * @param y location of event inside the view
     * @param absx location of event on the screen
     * @param absy location of event on the screen
     * @param button currently pressed button, required only in applicable
     *               events such as MouseEvent.DOWN
     * @param modifiers mask of currently pressed special keys and mouse
     *                  buttons. always required
     * @return boolean true if event has been consumed and no further processing
     *         required
     */
    private boolean handleDragEvents(LensView view, int eventType,
                                     int x, int y, int absx, int absy,
                                     int button, int modifiers) {
        boolean eventConsumed = false;


        if (eventType == MouseEvent.DOWN && cachedButtonPressed == MouseEvent.BUTTON_NONE) {
            //save the button that might have strated the drag event
            cachedButtonPressed = button;
            if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                LensLogger.getLogger().finest("Caching mouse button - " + button);
            }
        } else if (eventType == MouseEvent.UP && button == cachedButtonPressed) {
            //reset cached button on mouse up
            cachedButtonPressed = MouseEvent.BUTTON_NONE;

            if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                LensLogger.getLogger().finest("reset mouse button cache " + button);
            }
            if (dragStarted) {
                //drag button has been released while drag is active = drop
                if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                    LensLogger.getLogger().finest("notifying drag DROP");
                }
                postEvent(new LensDragEvent(view, x, y, absx, absy, DragActions.DROP));
                //reset internal state machine
                dragActionsPreformed = DragActions.NONE.getValue();
                dragView = null;
                dragStarted = false;
            }
        } else if (eventType == MouseEvent.MOVE &&
                   cachedButtonPressed != MouseEvent.BUTTON_NONE &&
                   ((modifiers & KeyEvent.MODIFIER_BUTTON_PRIMARY) == KeyEvent.MODIFIER_BUTTON_PRIMARY ||
                    (modifiers & KeyEvent.MODIFIER_BUTTON_MIDDLE) == KeyEvent.MODIFIER_BUTTON_MIDDLE ||
                    (modifiers & KeyEvent.MODIFIER_BUTTON_SECONDARY) == KeyEvent.MODIFIER_BUTTON_SECONDARY)) {
            //move + mouse button pressed = drag

            if (dragStarted) {
                //consume all event after drag have started
                eventConsumed = true;

                //drag has been initiated - handle drag drop/over/enter/leave events
                if (dragView == view &&
                        dragActionsPreformed == DragActions.NONE.getValue()) {
                    //first notification
                    postEvent(new LensDragEvent(view, x, y, absx, absy, DragActions.ENTER));
                    dragActionsPreformed |= DragActions.ENTER.getValue();
                    if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                        LensLogger.getLogger().finest("Notifying DragEnter");
                    }
                } else if (dragView == view &&
                           (dragActionsPreformed & DragActions.ENTER.getValue()) == DragActions.ENTER.getValue()) {
                    //view was notified that drag has entered to it
                    //now we need to send DragOver notification
                    postEvent(new LensDragEvent(view, x, y, absx, absy, DragActions.OVER));
                    dragActionsPreformed |= DragActions.OVER.getValue();
                    if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                        LensLogger.getLogger().finest("Notifying DragOver");
                    }
                } else if (dragView != view) {
                    //drag was moved to another view, leave the old one and
                    //reset the actions flags and dragView,
                    //also notify the new view for dragEnter

                    if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                        LensLogger.getLogger().finest("Notifying DragLeave old view");
                    }
                    postEvent(new LensDragEvent(dragView, x, y, absx, absy, DragActions.LEAVE));

                    if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                        LensLogger.getLogger().finest("Notifying DragEnter new view");
                    }
                    postEvent(new LensDragEvent(view, x, y, absx, absy, DragActions.ENTER));

                    dragActionsPreformed = DragActions.ENTER.getValue();
                    dragView = view;
                }

            } else {
                eventType = MouseEvent.DRAG;
                if (dragView == null) {
                    //cache the view that the drag started on
                    dragView = view;
                }
                if (LensLogger.isLogging(PlatformLogger.FINEST)) {
                    LensLogger.getLogger().finest("Drag detected - sending DRAG event");
                }
                postEvent(new LensMouseEvent(view, eventType,
                                             x, y, absx, absy,
                                             button,
                                             modifiers,
                                             false /*isPopupTrigger*/,
                                             false /*isSynthesized*/));
                eventConsumed = true;

            }
        }

        return eventConsumed;
    }


    /**
     * Notify scroll event from native layer
     *
     * @param window the window which the view is related to
     * @param x
     * @param y
     * @param absx
     * @param absy
     * @param deltaX
     * @param deltaY
     * @param modifiers
     * @param lines
     * @param chars
     * @param defaultLines
     * @param defaultChars
     * @param xMultiplier
     * @param yMultiplier
     * @param modifiers
     */
    private void notifyScrollEvent(LensView view,
                                   int x, int y, int absx, int absy,
                                   double deltaX, double deltaY, int modifiers,
                                   int lines, int chars, int defaultLines,
                                   int defaultChars, double xMultiplier,
                                   double yMultiplier) {

        try {
            if (LensLogger.isLogging(PlatformLogger.FINE)) {
                LensLogger.getLogger().fine("Scroll event on " + view);
            }

            postEvent(new LensScrollEvent(view, x, y, absx, absy,
                                          deltaX, deltaY, modifiers,
                                          lines, chars, defaultLines,
                                          defaultChars, xMultiplier,
                                          yMultiplier));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
    * Notify touch event from native layer
    *
    * @param window the window which the view is related to
    * @param state the finger state (e.g. TouchEvent.TOUCH_PRESSED)
    * @param id the id of the finger slot
    * @param x
    * @param y
    * @param absX
    * @param absY
    */
    private void notifyTouchEvent(LensView view, int state, long id,
                                  int x, int y, int absX, int absY) {
        try {

            if (LensLogger.isLogging(PlatformLogger.FINE)) {
                LensLogger.getLogger().fine("Touch event "
                                            + state + " at "
                                            + x + "," + y
                                            + " on " + view);
            }

            postEvent(new LensTouchEvent(view, state, id,
                                         x, y, absX, absY));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * Notify view event from native
     * View events are one of the events listed in ViewEvent.java
     *
     * @param view the event occured in
     * @param viewEventType the type of event as listed in
     *                      ViewEvent.java
     */
    private void notifyViewEvent(LensView view, int viewEventType,
                                 int x, int y, int width, int height) {
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine(
                "Notify event type "
                + ViewEvent.getTypeString(viewEventType)
                + " on " + view);
        }

        postEvent(new LensViewEvent(view, viewEventType,
                                    x, y, width, height));
    }

    private void notifyMenuEvent(LensView view, int x, int y, int xAbs,
                                 int yAbs, boolean isKeyboardTrigger) {
        if (LensLogger.isLogging(PlatformLogger.FINER)) {
            LensLogger.getLogger().finer(
                "Notify menu event " +
                "x=" + x + ", y=" + y + ", xAbs=" + xAbs + ", yAbs=" + yAbs +
                ", isKeyboardTrigger " + isKeyboardTrigger +
                ", on " + view);
        }
        if (view != null) {
            postEvent(new LensMenuEvent(view, x, y, xAbs, yAbs, isKeyboardTrigger));
        } else {
            if (LensLogger.isLogging(PlatformLogger.FINER)) {
                LensLogger.getLogger().finer("view is null, skipping event");
            }
        }
    }

    /**
     * Notify device event from native
     * A device event is sent when an input device is attached or detached.
     *
     * @param flags the device type flags (a bitmask containing values up to 2^DEVICE_MAX)
     * @param attach true is the device was attached, false if it was detached.
     */
    private void notifyDeviceEvent(int flags, boolean attach) {
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine(
                "Notify device event attach=" + attach
                + ", flags=0x" + Integer.toHexString(flags));
        }

        postEvent(new LensDeviceEvent(flags, attach));
    }

    //*******************************************************************

    public void installWindowMenu(MenuBar menubar) {
        // not sure what menubars look like on an embedded device yet.
        this.windowMenu = createMenu("Window");
    }

    public Menu getWindowMenu() {
        return this.windowMenu;
    }

    @Override
    public void installDefaultMenus(MenuBar menubar) {
    }

    // FACTORY METHODS
    @Override
    public Window createWindow(Window owner, Screen screen, int styleMask) {
        return new LensWindow(owner, screen, styleMask);
    }

    @Override
    public Window createWindow(long parent) {
        return new LensWindow(parent);
    }

    @Override
    public View createView() {
        return new LensView();
    }

    @Override
    public Cursor createCursor(int type) {
        return new LensCursor(type);
    }

    @Override
    public Cursor createCursor(int x, int y, Pixels pixels) {
        return new LensCursor(x, y, pixels);
    }

    @Override
    protected void staticCursor_setVisible(boolean visible) {
        LensCursor.setVisible_impl(visible);
    }

    @Override
    protected Size staticCursor_getBestSize(int width, int height) {
        return LensCursor.getBestSize_impl(width, height);
    }

    @Override
    public Pixels createPixels(int width, int height, ByteBuffer data) {
        return new LensPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data) {
        return new LensPixels(width, height, data);
    }

    @Override
    protected int staticPixels_getNativeFormat() {
        return LensPixels.getNativeFormat_impl();
    }

    @Override
    public Robot createRobot() {
        return new LensRobot();
    }

    @Override
    protected Screen staticScreen_getDeepestScreen() {
        return LensScreen.getDeepestScreen_impl();
    }

    @Override
    protected Screen staticScreen_getMainScreen() {
        return LensScreen.getMainScreen_impl();
    }

    @Override
    protected Screen staticScreen_getScreenForLocation(int x, int y) {
        return LensScreen.getScreenForLocation_impl(x, y);
    }

    @Override
    protected Screen staticScreen_getScreenForPtr(long screenPtr) {
        return LensScreen.getScreenForPtr_impl(screenPtr);
    }

    @Override
    protected List<Screen> staticScreen_getScreens() {
        return LensScreen.getScreens_impl();
    }

    @Override protected double staticScreen_getVideoRefreshPeriod() {
        return 0.0;     // indicate millisecond resolution
    }

    @Override
    public Timer createTimer(Runnable runnable) {
        return new LensTimer(runnable);
    }

    @Override
    protected int staticTimer_getMinPeriod() {
        return LensTimer.getMinPeriod_impl();
    }

    @Override
    protected int staticTimer_getMaxPeriod() {
        return LensTimer.getMaxPeriod_impl();
    }

    @Override protected FileChooserResult
    staticCommonDialogs_showFileChooser(Window owner, String folder,
                                        String filename,
                                        String title, int type,
                                        boolean multipleMode,
                                        ExtensionFilter[] extensionFilters) {
        //TODO: support FileChooserResult
        return new FileChooserResult(LensCommonDialogs.showFileChooser_impl(folder, title, type,
                                                      multipleMode,
                                                      extensionFilters), null);
    }

    @Override
    protected File staticCommonDialogs_showFolderChooser(Window owner,
            String folder,
            String title) {
        return LensCommonDialogs.showFolderChooser_impl();
    }

    @Override protected long staticView_getMultiClickTime() {
        return LensView._getMultiClickTime();
    }

    @Override protected int staticView_getMultiClickMaxX() {
        return LensView._getMultiClickMaxX();
    }

    @Override protected int staticView_getMultiClickMaxY() {
        return LensView._getMultiClickMaxY();
    }

    @Override
    protected void _invokeAndWait(Runnable runnable) {
        if (LensLogger.isLogging(PlatformLogger.FINEST)) {
            LensLogger.getLogger().fine("invokeAndWait " + runnable);
        }
        synchronized (invokeAndWaitLock) {
            waitingFor = runnable;
        }
        synchronized (eventList) {
            eventList.addLast(new RunnableEvent(true, runnable));
            eventList.notify();
        }
        synchronized (invokeAndWaitLock) {
            while (waitingFor == runnable) {
                try {
                    invokeAndWaitLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    @Override
    protected void _invokeLater(Runnable runnable) {
        if (LensLogger.isLogging(PlatformLogger.FINEST)) {
            LensLogger.getLogger().fine("invokeLater " + runnable);
        }
        synchronized (eventList) {
            eventList.addLast(new RunnableEvent(false, runnable));
            eventList.notify();
        }
    }

    public boolean hasTwoLevelFocus() {
        return deviceFlags[DEVICE_PC_KEYBOARD] == 0 && deviceFlags[DEVICE_5WAY] > 0;
    }

    public boolean hasVirtualKeyboard() {
        return deviceFlags[DEVICE_PC_KEYBOARD] == 0 && deviceFlags[DEVICE_TOUCH] > 0;
    }

    public boolean hasTouch() {
        return deviceFlags[DEVICE_TOUCH] > 0;
    }

    public boolean hasMultiTouch() {
        return deviceFlags[DEVICE_MULTITOUCH] > 0;
    }

    public boolean hasPointer() {
        return deviceFlags[DEVICE_POINTER] > 0;
    }

}
