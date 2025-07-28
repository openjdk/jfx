/*
 * Copyright (c) 2025, Gluon. All rights reserved.
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
package com.sun.glass.ui.headless;

import com.sun.glass.events.MouseEvent;
import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicLong;
import javafx.scene.paint.Color;

public class HeadlessWindow extends Window {

    private static final AtomicLong ptrCount = new AtomicLong(0);
    private long ptr;
    private final HeadlessWindowManager windowManager;

    private int minWidth;
    private int minHeight;
    private int maxWidth = -1;
    private int maxHeight = -1;
    private int originalX, originalY, originalWidth, originalHeight;
    private boolean visible;

    private final ByteBuffer frameBuffer;
    private HeadlessView currentView;
    private HeadlessRobot robot;

    private final int stride = 1000;

    public HeadlessWindow(HeadlessWindowManager wm, Window owner, Screen screen, ByteBuffer frameBuffer, int styleMask) {
        super(owner, screen, styleMask);
        this.frameBuffer = frameBuffer;
        this.windowManager = wm;
    }

    @Override
    protected long _createWindow(long ownerPtr, long screenPtr, int mask) {
        this.ptr = ptrCount.incrementAndGet();
        return ptr;
    }

    @Override
    protected boolean _close(long ptr) {
        this.notifyDestroy();
        if (this.robot != null) {
            this.robot.windowRemoved(this);
        }
        return true;
    }

    @Override
    protected boolean _setView(long ptr, View view) {
        if (currentView != null) {
            currentView.notifyMouse(MouseEvent.EXIT, MouseEvent.BUTTON_NONE, 0, 0, 0, 0, 0, false, false);
        }
        this.currentView = (HeadlessView) view;
        if (currentView != null) {
            currentView.notifyMouse(MouseEvent.ENTER, MouseEvent.BUTTON_NONE, 0, 0, 0, 0, 0, false, false);
        }
        return true;
    }

    @Override
    protected void _updateViewSize(long ptr) {
        if (this.isVisible()) {
            currentView.notifyResize(width, height);
        }
    }

    @Override
    protected boolean _setMenubar(long ptr, long menubarPtr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected boolean _minimize(long ptr, boolean minimize) {
        notifyResize(minimize ? WindowEvent.MINIMIZE : WindowEvent.RESTORE, width, height);
        windowManager.repaintAll();
        return true;
    }

    @Override
    protected boolean _maximize(long ptr, boolean maximize, boolean wasMaximized) {
        int newX = 0;
        int newY = 0;
        int newWidth = 0;
        int newHeight = 0;
        if (maximize && !wasMaximized) {
            this.originalHeight = this.height;
            this.originalWidth = this.width;
            this.originalX = this.x;
            this.originalY = this.y;
            newWidth = screen.getWidth();
            newHeight = screen.getHeight();
            setState(State.MAXIMIZED);
        } else if (!maximize && wasMaximized) {
            newHeight = this.originalHeight;
            newWidth = this.originalWidth;
            newX = this.originalX;
            newY = this.originalY;
            setState(State.NORMAL);
        }
        notifyResizeAndMove(newX, newY, newWidth, newHeight);
        if (maximize) {
            notifyResize(WindowEvent.MAXIMIZE, newWidth, newHeight);
        }

        return maximize;
    }

    @Override
    protected void _setBounds(long ptr, int x, int y, boolean xSet, boolean ySet, int w, int h, int cw, int ch, float xGravity, float yGravity) {
        int newWidth = w > 0 ? w : cw > 0 ? cw : getWidth();
        int newHeight = h > 0 ? h : ch > 0 ? ch : getHeight();
        if (!xSet) {
            x = getX();
        }
        if (!ySet) {
            y = getY();
        }
        if (maxWidth >= 0) {
            newWidth = Math.min(newWidth, maxWidth);
        }
        if (maxHeight >= 0) {
            newHeight = Math.min(newHeight, maxHeight);
        }
        newWidth = Math.max(newWidth, minWidth);
        newHeight = Math.max(newHeight, minHeight);
        if (newWidth < getWidth()) {
            clearRect(getX() + newWidth, getWidth() - newWidth, getY(), getHeight());
        }
        if (newHeight < getHeight()) {
            clearRect(getX(), getWidth(), getY() + newHeight, getHeight() - newHeight);
        }
        notifyResizeAndMove(x, y, newWidth, newHeight);
    }

    @Override
    protected boolean _setVisible(long ptr, boolean v) {
        this.visible = v;
        return this.visible;
    }

    @Override
    protected boolean _setResizable(long ptr, boolean resizable) {
        return true;
    }

    @Override
    protected boolean _requestFocus(long ptr, int event) {
        this.notifyFocus(event);
        return this.isFocused();
    }

    @Override
    protected void _setFocusable(long ptr, boolean isFocusable) {
    }

    @Override
    protected boolean _grabFocus(long ptr) {
        return true;
    }

    @Override
    protected void _ungrabFocus(long ptr) {
    }

    @Override
    protected boolean _setTitle(long ptr, String title) {
        return true;
    }

    @Override
    protected void _setLevel(long ptr, int level) {
    }

    @Override
    protected void _setAlpha(long ptr, float alpha) {
    }

    @Override
    protected boolean _setBackground(long ptr, float r, float g, float b) {
        return true;
    }

    @Override
    protected void _setEnabled(long ptr, boolean enabled) {
    }

    @Override
    protected boolean _setMinimumSize(long ptr, int width, int height) {
        this.minWidth = width;
        this.minHeight = height;
        return true;
    }

    @Override
    protected boolean _setMaximumSize(long ptr, int width, int height) {
        this.maxWidth = width;
        this.maxHeight = height;
        return true;
    }

    @Override
    protected void _setIcon(long ptr, Pixels pixels) {
    }

    @Override
    protected void _setCursor(long ptr, Cursor cursor) {
    }

    @Override
    protected void _toFront(long ptr) {
    }

    @Override
    protected void _toBack(long ptr) {
    }

    @Override
    protected void _requestInput(long ptr, String text, int type, double width, double height,
            double Mxx, double Mxy, double Mxz, double Mxt,
            double Myx, double Myy, double Myz, double Myt,
            double Mzx, double Mzy, double Mzz, double Mzt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void _releaseInput(long ptr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void setFullscreen(boolean full) {
        int newX = 0;
        int newY = 0;
        int newWidth = 0;
        int newHeight = 0;
        if (full) {
            this.originalHeight = this.height;
            this.originalWidth = this.width;
            this.originalX = this.x;
            this.originalY = this.y;
            newX = 0;
            newY = 0;
            newWidth = screen.getWidth();
            newHeight = screen.getHeight();
        } else {
            newHeight = this.originalHeight;
            newWidth = this.originalWidth;
            newX = this.originalX;
            newY = this.originalY;
        }
        notifyResizeAndMove(newX, newY, newWidth, newHeight);
    }

    private void notifyResizeAndMove(int x, int y, int width, int height) {
        HeadlessView view = (HeadlessView) getView();
        notifyResize(WindowEvent.RESIZE, width, height);
        if (view != null) {
            view.notifyResize(width, height);
        }
        if (getX() != x || getY() != y) {
            notifyMove(x, y);
        }
    }

    public Color getColor(int lx, int ly) {
        int mx = lx;
        int my = ly;
        int idx = stride * my + mx;
        int rgba = frameBuffer.asIntBuffer().get(idx);
        int a = (rgba >> 24) & 0xFF;
        int r = (rgba >> 16) & 0xFF;
        int g = (rgba >> 8) & 0xFF;
        int b = rgba & 0xFF;

        Color color = Color.rgb(r, g, b, a/255.);
        return color;
    }

    public void getScreenCapture(int x, int y, int width, int height, int[] data, boolean scaleToFit) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int idx = i * width + j;
                int fidx = (y + i) * stride + x + j;
                int val = frameBuffer.asIntBuffer().get(fidx);
                data[idx] = val;
            }
        }
    }

    void blit(Pixels pixels) {
        int pW = pixels.getWidth();
        int pH = pixels.getHeight();
        int offsetX = this.getX();
        int offsetY = this.getY();

        IntBuffer intBuffer = (IntBuffer) pixels.getBuffer();

        for (int i = 0; i < pixels.getHeight(); i++) {
            int rowIdx = offsetY + i;
            for (int j = 0; j < pixels.getWidth(); j++) {
                int idx = rowIdx * stride + offsetX + j;
                int val = intBuffer.get(i * pixels.getWidth() + j);
                frameBuffer.asIntBuffer().put(idx, val);
            }
        }
    }

    void clearRect(int x0, int w0, int y0, int h0) {
        for (int i = 0; i < h0; i++) {
            int rowIdx = y0 + i;
            for (int j = 0; j < w0; j++) {
                int idx = rowIdx * stride + x0 + j;
                frameBuffer.asIntBuffer().put(idx, 0);
            }
        }
    }

    void setRobot(HeadlessRobot activeRobot) {
        this.robot = activeRobot;
    }
}
