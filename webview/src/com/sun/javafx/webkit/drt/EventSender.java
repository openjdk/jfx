/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.drt;

import com.sun.webkit.WebPage;
import com.sun.javafx.webkit.KeyCodeMap;
import com.sun.webkit.event.WCKeyEvent;
import com.sun.webkit.event.WCMouseEvent;
import com.sun.webkit.event.WCMouseWheelEvent;
import com.sun.webkit.event.WCTouchEvent;
import com.sun.webkit.event.WCTouchPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javafx.scene.input.KeyCode;

/**
 * Event sender for DRT tests.
 */
final class EventSender {

    private static final int ALT = 1;
    private static final int CTRL = 2;
    private static final int META = 4;
    private static final int SHIFT = 8;
    private static final int PRESSED = 16;

    private static final float ZOOM = 1.2f;
    private static final float SCROLL = 40f;

    private static final Map<Object, KeyCode> MAP = new HashMap<Object, KeyCode>();

    static {
        MAP.put("\r", KeyCode.ENTER);
        MAP.put("pageUp", KeyCode.PAGE_UP);
        MAP.put("pageDown", KeyCode.PAGE_DOWN);
        MAP.put("leftArrow", KeyCode.LEFT);
        MAP.put("upArrow", KeyCode.UP);
        MAP.put("rightArrow", KeyCode.RIGHT);
        MAP.put("downArrow", KeyCode.DOWN);
        MAP.put("printScreen", KeyCode.PRINTSCREEN);
        MAP.put("menu", KeyCode.CONTEXT_MENU);
        for (KeyCode code : KeyCode.values()) {
            MAP.put(code.impl_getCode(), code);
            MAP.put(code.getName().toLowerCase(), code);
            MAP.put(code.getName(), code);
        }
    }

    /**
     * The web page to send events to.
     */
    private final WebPage webPage;

    /**
     * The current state of the drag mode.
     */
    private boolean dragMode = true;

    /**
     * The current X position of the mouse.
     */
    private int mousePositionX;

    /**
     * The current Y position of the mouse.
     */
    private int mousePositionY;

    /**
     * The current state of mouse buttons.
     */
    private boolean mousePressed;

    /**
     * The time offset for events.
     */
    private long timeOffset;

    /**
     * The current modifiers for touch events.
     */
    private int modifiers;

    /**
     * The current points for touch events.
     */
    private final List<WCTouchPoint> points = new ArrayList<WCTouchPoint>();

    /**
     * Creates a new {@code EventSender}.
     */
    EventSender(WebPage webPage) {
        this.webPage = webPage;
    }

    /**
     * Implements the {@code keyDown}
     * method of the DRT event sender object.
     */
    private void keyDown(String key, int modifiers) {
        String keyChar = null;
        KeyCode code = MAP.get(key);
        if (1 == key.length()) {
            if (code == null) {
                code = MAP.get(Integer.valueOf(Character.toUpperCase(
                        key.charAt(0))));
            }
            keyChar = key;
        }
        if (code == null) {
            System.err.println("unexpected key = " + key);
        }
        else {
            KeyCodeMap.Entry keyCodeEntry = KeyCodeMap.lookup(code);
            String keyIdentifier = keyCodeEntry.getKeyIdentifier();
            int windowsVirtualKeyCode = keyCodeEntry.getWindowsVirtualKeyCode();
            dispatchKeyEvent(WCKeyEvent.KEY_PRESSED, null, keyIdentifier,
                    windowsVirtualKeyCode, modifiers);
            dispatchKeyEvent(WCKeyEvent.KEY_TYPED, keyChar, null,
                    0, modifiers);
            dispatchKeyEvent(WCKeyEvent.KEY_RELEASED, null, keyIdentifier,
                    windowsVirtualKeyCode, modifiers);
        }
    }

    /**
     * Implements the {@code mouseUp} and {@code mouseDown}
     * methods of the DRT event sender object.
     */
    private void mouseUpDown(int button, int modifiers) {
        mousePressed = isSet(modifiers, PRESSED);
        dispatchMouseEvent(mousePressed
                ? WCMouseEvent.MOUSE_PRESSED
                : WCMouseEvent.MOUSE_RELEASED, button, 1, modifiers);
    }

    /**
     * Implements the {@code mouseMoveTo}
     * method of the DRT event sender object.
     */
    private void mouseMoveTo(int x, int y) {
        mousePositionX = x;
        mousePositionY = y;
        dispatchMouseEvent(mousePressed
                ? WCMouseEvent.MOUSE_DRAGGED
                : WCMouseEvent.MOUSE_MOVED, 0, 0, 0);
    }

    /**
     * Implements the {@code mouseScrollBy} and {@code continuousMouseScrollBy}
     * methods of the DRT event sender object.
     */
    private void mouseScroll(float x, float y, boolean continuous) {
        if (continuous) {
            x /= SCROLL;
            y /= SCROLL;
        }
        webPage.dispatchMouseWheelEvent(new WCMouseWheelEvent(
                mousePositionX, mousePositionY,
                mousePositionX, mousePositionY,
                getEventTime(),
                false,
                false,
                false,
                false,
                x, y
        ));
    }

    /**
     * Implements the {@code leapForward}
     * method of the DRT event sender object.
     */
    private void leapForward(int timeOffset) {
        this.timeOffset += timeOffset;
    }

    /**
     * Implements the {@code contextClick}
     * method of the DRT event sender object.
     */
    private void contextClick() {
        dispatchMouseEvent(WCMouseEvent.MOUSE_PRESSED, WCMouseEvent.BUTTON2, 1, 0);
        dispatchMouseEvent(WCMouseEvent.MOUSE_RELEASED, WCMouseEvent.BUTTON2, 1, 0);
    }

    /**
     * Implements the {@code scheduleAsynchronousClick}
     * method of the DRT event sender object.
     */
    private void scheduleAsynchronousClick() {
        dispatchMouseEvent(WCMouseEvent.MOUSE_PRESSED, WCMouseEvent.BUTTON1, 1, 0);
        dispatchMouseEvent(WCMouseEvent.MOUSE_RELEASED, WCMouseEvent.BUTTON1, 1, 0);
    }

    /**
     * Implements the {@code touchStart}
     * method of the DRT event sender object.
     */
    private void touchStart() {
        dispatchTouchEvent(WCTouchEvent.TOUCH_START);
    }

    /**
     * Implements the {@code touchCancel}
     * method of the DRT event sender object.
     */
    private void touchCancel() {
        dispatchTouchEvent(WCTouchEvent.TOUCH_CANCEL);
    }

    /**
     * Implements the {@code touchMove}
     * method of the DRT event sender object.
     */
    private void touchMove() {
        dispatchTouchEvent(WCTouchEvent.TOUCH_MOVE);
    }

    /**
     * Implements the {@code touchEnd}
     * method of the DRT event sender object.
     */
    private void touchEnd() {
        dispatchTouchEvent(WCTouchEvent.TOUCH_END);
    }

    /**
     * Implements the {@code addTouchPoint}
     * method of the DRT event sender object.
     */
    private void addTouchPoint(int x, int y) {
        int id = 0;
        for (WCTouchPoint point : points) {
            if (id == point.getID())
                id++;
        }
        points.add(new WCTouchPoint(id, WCTouchPoint.STATE_PRESSED, x, y, x, y));

    }

    /**
     * Implements the {@code updateTouchPoint}
     * method of the DRT event sender object.
     */
    private void updateTouchPoint(int i, int x, int y) {
        points.get(i).update(x, y);
    }

    /**
     * Implements the {@code cancelTouchPoint}
     * method of the DRT event sender object.
     */
    private void cancelTouchPoint(int i) {
        points.get(i).update(WCTouchPoint.STATE_CANCELLED);
    }

    /**
     * Implements the {@code releaseTouchPoint}
     * method of the DRT event sender object.
     */
    private void releaseTouchPoint(int i) {
        points.get(i).update(WCTouchPoint.STATE_RELEASED);
    }

    /**
     * Implements the {@code clearTouchPoints}
     * method of the DRT event sender object.
     */
    private void clearTouchPoints() {
        points.clear();
    }

    /**
     * Implements the {@code setTouchModifier}
     * method of the DRT event sender object.
     */
    private void setTouchModifier(int modifier, boolean set) {
        modifiers = set ? (modifiers | modifier) : (modifiers & ~modifier);
    }

    /**
     * Implements the {@code scalePageBy}
     * method of the DRT event sender object.
     */
    private void scalePageBy(float scale, int x, int y) {
        throw new UnsupportedOperationException("scalePageBy(" + scale + "); x=" + x + "; y=" + y);
    }

    /**
     * Implements the {@code textZoomIn}, {@code textZoomOut}
     * {@code zoomPageInt}, and {@code zoomPageOut}
     * methods of the DRT event sender object.
     */
    private void zoom(boolean in, boolean textOnly) {
        float factor = webPage.getZoomFactor(textOnly);
        webPage.setZoomFactor(in ? (factor * ZOOM) : (factor / ZOOM), textOnly);
    }

    /**
     * Implements the {@code beginDragWithFiles}
     * method of the DRT event sender object.
     */
    private void beginDragWithFiles(String[] names) {
        StringBuilder sb = new StringBuilder("beginDragWithFiles");
        for (String name : names) {
            sb.append(", ").append(name);
        }
        throw new UnsupportedOperationException(sb.append('.').toString());
    }

    /**
     * Returns the {@code dragMode}
     * variable of the DRT event sender object.
     */
    private boolean getDragMode() {
        return dragMode;
    }

    /**
     * Sets the {@code dragMode}
     * variable of the DRT event sender object.
     */
    private void setDragMode(boolean mode) {
        dragMode = mode;
    }

    private long getEventTime() {
        return timeOffset + System.currentTimeMillis();
    }

    private void dispatchKeyEvent(int type, String text, String keyIdentifier,
                                  int windowsVirtualKeyCode, int modifiers)
    {
        webPage.dispatchKeyEvent(new WCKeyEvent(
                type, text, keyIdentifier, windowsVirtualKeyCode,
                isSet(modifiers, SHIFT),
                isSet(modifiers, CTRL),
                isSet(modifiers, ALT),
                isSet(modifiers, META)
        ));
    }

    private void dispatchMouseEvent(int type, int button, int clicks, int modifiers) {
        webPage.dispatchMouseEvent(new WCMouseEvent(
                type, button, clicks,
                mousePositionX, mousePositionY,
                mousePositionX, mousePositionY,
                getEventTime(),
                isSet(modifiers, SHIFT),
                isSet(modifiers, CTRL),
                isSet(modifiers, ALT),
                isSet(modifiers, META),
                false
        ));
    }

    private void dispatchTouchEvent(int type) {
        webPage.dispatchTouchEvent(new WCTouchEvent(
                type, points,
                getEventTime(),
                isSet(modifiers, SHIFT),
                isSet(modifiers, CTRL),
                isSet(modifiers, ALT),
                isSet(modifiers, META)
        ));
        Iterator<WCTouchPoint> it = points.iterator();
        while (it.hasNext()) {
            WCTouchPoint point = it.next();
            if (WCTouchPoint.STATE_RELEASED == point.getState()) {
                it.remove();
            }
            else {
                point.update(WCTouchPoint.STATE_STATIONARY);
            }
        }
    }

    private static boolean isSet(int modifiers, int modifier) {
        return modifier == (modifier & modifiers);
    }
}
