package com.sun.glass.ui;

import com.sun.glass.events.MouseEvent;
import javafx.scene.Node;
import javafx.stage.WindowRegion;
import javafx.stage.WindowRegionClassifier;

import java.util.HashMap;
import java.util.Map;

public class MoveResizeHelper {

    private static final Map<WindowRegion, Cursor> RESIZE_CURSORS = new HashMap<>();

    static {
        Application application = Application.GetApplication();
        RESIZE_CURSORS.put(WindowRegion.TOP_LEFT, application.createCursor(Cursor.CURSOR_RESIZE_NORTHWEST));
        RESIZE_CURSORS.put(WindowRegion.TOP_RIGHT, application.createCursor(Cursor.CURSOR_RESIZE_NORTHEAST));
        RESIZE_CURSORS.put(WindowRegion.BOTTOM_LEFT, application.createCursor(Cursor.CURSOR_RESIZE_SOUTHWEST));
        RESIZE_CURSORS.put(WindowRegion.BOTTOM_RIGHT, application.createCursor(Cursor.CURSOR_RESIZE_SOUTHEAST));
        RESIZE_CURSORS.put(WindowRegion.LEFT, application.createCursor(Cursor.CURSOR_RESIZE_LEFTRIGHT));
        RESIZE_CURSORS.put(WindowRegion.RIGHT, application.createCursor(Cursor.CURSOR_RESIZE_LEFTRIGHT));
        RESIZE_CURSORS.put(WindowRegion.TOP, application.createCursor(Cursor.CURSOR_RESIZE_UPDOWN));
        RESIZE_CURSORS.put(WindowRegion.BOTTOM, application.createCursor(Cursor.CURSOR_RESIZE_UPDOWN));
    }

    private final View view;
    private final Window window;
    private final WindowRegionClassifier regionClassifier;
    private int mouseDownX, mouseDownY;
    private int mouseDownWindowX, mouseDownWindowY;
    private int mouseDownWindowWidth, mouseDownWindowHeight;
    private WindowRegion currentWindowRegion;
    private Cursor lastCursor;

    public MoveResizeHelper(View view, Window window) {
        this.view = view;
        this.window = window;
        this.regionClassifier = window.regionClassifier;
    }

    public final boolean handleMouseEvent(int type, int button, int x, int y, int xAbs, int yAbs) {
        int wx = (int)(x / window.getPlatformScaleX());
        int wy = (int)(y / window.getPlatformScaleY());

        if (type != MouseEvent.DRAG) {
            var eventHandler = view.getEventHandler();
            Node pickedNode = eventHandler != null ? eventHandler.pickNode(wx, wy) : null;
            currentWindowRegion = regionClassifier.classify(wx, wy, pickedNode);
            updateCursor(currentWindowRegion);

            if (currentWindowRegion == WindowRegion.CLIENT) {
                return false;
            }
        }

        switch (type) {
            case MouseEvent.DRAG:
                handleMouseDrag(button, xAbs, yAbs, currentWindowRegion);
                break;
            case MouseEvent.DOWN:
                handleMouseDown(xAbs, yAbs);
                break;
        }

        return false;
    }

    protected boolean shouldStartMoveDrag(int button, WindowRegion region) {
        return button == MouseEvent.BUTTON_LEFT && region == WindowRegion.TITLE;
    }

    protected boolean shouldStartResizeDrag(int button, WindowRegion region) {
        return button == MouseEvent.BUTTON_LEFT && RESIZE_CURSORS.get(region) != null;
    }

    private void handleMouseDrag(int button, int xAbs, int yAbs, WindowRegion region) {
        if (shouldStartMoveDrag(button, region)) {
            handleMoveWindow(xAbs, yAbs);
        } else if (shouldStartResizeDrag(button, region)) {
            handleResizeWindow(xAbs, yAbs, region);
        }
    }

    private void handleMouseDown(int xAbs, int yAbs) {
        mouseDownX = xAbs;
        mouseDownY = yAbs;
        mouseDownWindowX = window.getX();
        mouseDownWindowY = window.getY();
        mouseDownWindowWidth = window.getWidth();
        mouseDownWindowHeight = window.getHeight();
    }

    private void handleMoveWindow(int xAbs, int yAbs) {
        window.setPosition(mouseDownWindowX + xAbs - mouseDownX, mouseDownWindowY + yAbs - mouseDownY);
    }

    private void handleResizeWindow(int xAbs, int yAbs, WindowRegion region) {
        int dx = xAbs - mouseDownX;
        int dy = yAbs - mouseDownY;

        switch (region) {
            case LEFT:
                adjustWindowPosition(dx, 0);
                adjustWindowSize(-dx, 0);
                break;
            case RIGHT:
                adjustWindowSize(dx, 0);
                break;
            case TOP:
                adjustWindowPosition(0, dy);
                adjustWindowSize(0, -dy);
                break;
            case BOTTOM:
                adjustWindowSize(0, dy);
                break;
            case TOP_LEFT:
                adjustWindowPosition(dx, dy);
                adjustWindowSize(-dx, -dy);
                break;
            case TOP_RIGHT:
                adjustWindowPosition(0, dy);
                adjustWindowSize(dx, -dy);
                break;
            case BOTTOM_LEFT:
                adjustWindowPosition(dx, 0);
                adjustWindowSize(-dx, dy);
                break;
            case BOTTOM_RIGHT:
                adjustWindowSize(dx, dy);
                break;
        }
    }

    private void adjustWindowPosition(int dx, int dy) {
        int unclampedWidth = mouseDownWindowWidth - dx;
        int unclampedHeight = mouseDownWindowHeight - dy;
        int clampedWidth = dx != 0 ? clampWidth(unclampedWidth) : unclampedWidth;
        int clampedHeight = dy != 0 ? clampHeight(unclampedHeight) : unclampedHeight;
        int cx = unclampedWidth - clampedWidth;
        int cy = unclampedHeight - clampedHeight;
        window.setPosition(mouseDownWindowX + dx + cx, mouseDownWindowY + dy + cy);
    }

    private void adjustWindowSize(int dx, int dy) {
        int width = dx != 0 ? clampWidth(mouseDownWindowWidth + dx) : mouseDownWindowWidth;
        int height = dy != 0 ? clampHeight(mouseDownWindowHeight + dy) : mouseDownWindowHeight;
        window.setSize(width, height);
    }

    private int clampWidth(int width) {
        return Math.max(window.getMinimumWidth(), Math.min(window.getMaximumWidth(), width));
    }

    private int clampHeight(int height) {
        return Math.max(window.getMinimumHeight(), Math.min(window.getMaximumHeight(), height));
    }

    private void updateCursor(WindowRegion region) {
        Cursor newCursor = RESIZE_CURSORS.get(region);

        if (lastCursor == null && newCursor != null) {
            lastCursor = window.getCursor();
        } else if (lastCursor != null && newCursor == null) {
            window.setCursor(lastCursor);
            lastCursor = null;
        }

        if (newCursor != null) {
            window.setCursor(newCursor);
        }
    }

}
