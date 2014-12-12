/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.swt;

import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

final class SWTWindow extends Window {
    Shell shell;
    static SWTWindow focusWindow;
    
    protected SWTWindow(Window owner, Screen screen, int styleMask) {
        super(owner, screen, styleMask);
    }
    protected SWTWindow(long parent) {
        super(parent);
    }
    
    @Override protected long _createWindow(long ownerPtr, long screenPtr, int mask) {
        //int bits = SWT.SHELL_TRIM | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE;
        int bits = SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE;
        if ((mask & Window.TITLED) != 0) {
            if ((mask & Window.CLOSABLE) != 0) bits |= SWT.CLOSE;
            if ((mask & Window.MINIMIZABLE) != 0) bits |= SWT.MIN;
            if ((mask & Window.MAXIMIZABLE) != 0) bits |= SWT.MAX;
//            if ((mask & Window.RESIZEABLE) != 0) bits |= SWT.RESIZE;
            bits |= SWT.RESIZE;
        } else {
            bits |= SWT.NO_TRIM | SWT.NO_FOCUS;
        }
        //if ((mask & Window.TRANSPARENT) != 0) bits |= SWT.NO_TRIM;
        if ((mask & Window.UTILITY) != 0) bits |= SWT.TOOL;
        if ((mask & Window.POPUP) != 0) bits |=SWT.TOOL;
        if ((mask & Window.RIGHT_TO_LEFT) != 0) bits |= SWT.RIGHT_TO_LEFT;
        Shell parent = (Shell) SWTApplication.findWidget(ownerPtr);
        if (parent != null) {
            shell = new Shell(parent, bits);
        } else {
            shell = new Shell(Display.getDefault(), bits);
        }
        if ((mask & Window.TRANSPARENT) != 0) {
            shell.setData("transparent", true);
        }
        int [] shellEvents = new int [] {
            SWT.Activate,
            SWT.Close,
            SWT.Deactivate,
            SWT.Iconify,
            SWT.Deiconify,
            SWT.Move,
            SWT.Resize,
            SWT.Dispose,
        };
        Listener shellListener = event -> handleShellEvent (event);
        for (int i=0; i<shellEvents.length; i++) {
            shell.addListener(shellEvents[i], shellListener);
        }
        shell.setData(this);
        
        return SWTApplication.getHandle(shell);
    }

    void handleShellEvent (Event event) {
        switch (event.type) {
            case SWT.Activate: {
                notifyFocus(WindowEvent.FOCUS_GAINED);
                break;
            }
            case SWT.Deactivate: {
                /*
                 * Feature in SWT.  On the Mac, SWT sends a deactivate and then
                 * activate to the parent shell when the user clicks with the left
                 * mouse button in a child shell with the style SWT.NO_FOCUS.
                 * The fix is to avoid FOCUS_LOST during grab.
                 * 
                 * NOTE: This is probably not the correct code because FX gets
                 * a FOCUS_GAINED for the same stage when focus has not changed.
                 */
                if (SWTWindow.focusWindow != null) break;
                notifyFocus(WindowEvent.FOCUS_LOST);
                break;
            }
            case SWT.Iconify: {
                Rectangle rect = shell.getBounds();
                notifyResize(WindowEvent.MINIMIZE, rect.width, rect.height);
                break;
            }
            //TODO - implement maximize notification
            case SWT.Deiconify: {
                Rectangle rect = shell.getBounds();
                notifyResize(WindowEvent.RESTORE, rect.width, rect.height);
                break;
            }
            case SWT.Move: {
                Rectangle rect = shell.getBounds();
                Rectangle trim = shell.computeTrim(0, 0, rect.width, rect.height);
                notifyMove(rect.x - trim.x, rect.y - trim.y);
                break;
            }
            case SWT.Resize: {
                Rectangle bounds = shell.getBounds();
                notifyResize(WindowEvent.RESIZE, bounds.width, bounds.height);
                Rectangle rect = shell.getClientArea();
                Control [] children = shell.getChildren();
                for (int i=0; i<children.length; i++) children[i].setBounds(rect);
                break;
            }
            case SWT.Close: {
                notifyClose();
                event.doit = false;
                break;
            }
            case SWT.Dispose: {
                Image oldImage = shell.getImage();
                if (oldImage != null) {
                    shell.setImage(null);
                    oldImage.dispose();
                }
                notifyDestroy();
                break;
            }
        }
    }
    
    @Override protected long _createChildWindow(long parent) {
        //TODO - implement child windows
        /* applet */
        return _createWindow(parent, 0, 0);
    }
    
    @Override protected boolean _close(long ptr) {
        shell.dispose();
        return true;
    }
    
    @Override protected boolean _setView(long ptr, View view) {
        //TODO - dispose the current view when set to null?
        if (view == null) return true;
        Canvas canvas = ((SWTView) view).canvas;
        boolean success = canvas.setParent(shell);
        if (success) {
            canvas.setBounds(canvas.getParent().getClientArea());
            if (shell.getData("transparent") != null) {//(mask & Window.TRANSPARENT) != 0) {
                SWTApplication.setTransparent(shell);
            }
        }
        return success;
    }
    
    @Override protected void _setBounds(long ptr, 
            final int x, final int y, 
            final boolean xSet, final boolean ySet, 
            final int w, final int h, 
            final int cw, final int ch, float xGravity, float yGravity) {
        
        //TODO - using syncExec because of applet
        shell.getDisplay().syncExec(() -> {
            Rectangle rect = shell.getBounds();
            if (xSet) rect.x = x;
            if (ySet) rect.y = y;
            boolean hSet = false, wSet = false;
            //TODO - check that this is the right way to process w, h, cw, ch
            if (w != -1) {
                wSet = true;
                rect.width = w;
            } else {
                if (cw != -1) {
                    wSet = true;
                    rect.width = cw;
                }
            }
            if (h != -1) {
                hSet = true;
                rect.height = h;
            } else {
                if (ch != -1) {
                    hSet = true;
                    rect.height = ch;
                }
            }
            if (wSet || hSet) {
                Rectangle bounds= shell.computeTrim(rect.x, rect.y, rect.width, rect.height);
                shell.setBounds(rect.x, rect.y, bounds.width, bounds.height);
            } else {
                shell.setLocation(rect.x, rect.y);
            }
        });
    }
    
    @Override protected boolean _setMenubar(long ptr, long menubarPtr) {
        return true;
    }

    @Override protected boolean _minimize(long ptr, boolean minimize) {
        shell.setMinimized(minimize);
        return shell.getMinimized();
    }
    
    @Override protected boolean _maximize(long ptr, boolean maximize, boolean wasMaximized) {
        //TODO - what should be done with the wasMaximized flag
        shell.setMaximized(maximize);
        return shell.getMaximized();
    }

    @Override protected boolean _setVisible(long ptr, final boolean visible) {
        //TODO - using syncExec because of applet
        shell.getDisplay().syncExec(() -> {
            if ((shell.getStyle() & SWT.NO_FOCUS) != 0) {
                shell.setVisible(visible);
            } else {
                if (visible) {
                    shell.open();
                    //TODO - explicitly setting focus should not be necessary
                    shell.setFocus();
                } else {
                    shell.setVisible(false);
                }
            }
        });
        return true;
    }
    
    @Override protected boolean _setResizable(long ptr, boolean resizable) {
        //TODO - implement resizable
        return true;
    }
    
    @Override protected boolean _requestFocus(long ptr, int event) {
        shell.setFocus();
        return true;
    }
    
    @Override protected void _setFocusable(long ptr, boolean isFocusable) {
        //TODO - implement focus
    }
    
    @Override protected boolean _setTitle(long ptr, String title) {
        shell.setText(title);
        return true;
    }
    
    @Override protected void _setLevel(long ptr, int level) {
        //TODO - implement window stacking
    }
    
    @Override protected void _setAlpha(long ptr, float alpha) {
        shell.setAlpha((int)(alpha * 255));
    }
    
    @Override protected boolean _setBackground(long ptr, float r, float g, float b) {
        //TODO - implement background color
        return true;
    }
    
    @Override protected void _setEnabled(long ptr, boolean enabled) {
        shell.setEnabled(enabled);
    }
    
    @Override protected boolean _setMinimumSize(long ptr, int width, int height) {
        Point pt = new Point(width, height);
        shell.setMinimumSize(pt);
        return pt.equals(shell.getMinimumSize());
    }
    
    @Override protected boolean _setMaximumSize(long ptr, int width, int height) {
        //TODO - implement maximum size
        return false;
    }
    
    //TODO - implement icon
    @Override protected void _setIcon(long ptr, Pixels pixels) {
        Image oldImage = shell.getImage();
        Image newImage = SWTApplication.createImage(pixels);
        shell.setImage(newImage);
        if (oldImage != null) oldImage.dispose();
    }
    
    @Override protected void _toFront(long ptr) {
        shell.moveAbove(null);
    }
    
    @Override protected void _toBack(long ptr) {
        shell.moveBelow(null);
    }
    
    @Override protected void _enterModal(long ptr) {
        //TODO - implement modal
    }
    
    @Override protected void _enterModalWithWindow(long dialog, long window) {
        //TODO - implement modal
    }
    
    @Override protected void _exitModal(long ptr) {
        //TODO - implement modal
    }
    
    @Override protected boolean _grabFocus(long ptr) {
        focusWindow = this;
        return true;
    }
    
    @Override protected void _ungrabFocus(long ptr) {
        focusWindow = null;
    }
    
    @Override
    protected void _setCursor(long ptr, Cursor cursor) {
        int id = SWT.CURSOR_ARROW;
        switch (cursor.getType()) {
            case Cursor.CURSOR_DEFAULT: {
                // When the default cursor is requested, clear the current cursor
                // rather than setting the arrow.  During drag and drop, setting
                // any cursor clears the drag indicators.
                shell.setCursor(null);
                return;
            }
            case Cursor.CURSOR_CROSSHAIR: id = SWT.CURSOR_CROSS; break;
            case Cursor.CURSOR_TEXT:      id = SWT.CURSOR_IBEAM; break;
            case Cursor.CURSOR_WAIT:      id = SWT.CURSOR_WAIT; break;
            case Cursor.CURSOR_RESIZE_SOUTHWEST: id = SWT.CURSOR_SIZESW; break;
            case Cursor.CURSOR_RESIZE_SOUTHEAST: id = SWT.CURSOR_SIZESE; break;
            case Cursor.CURSOR_RESIZE_NORTHWEST: id = SWT.CURSOR_SIZENW; break;
            case Cursor.CURSOR_RESIZE_NORTHEAST: id = SWT.CURSOR_SIZENE; break;
            case Cursor.CURSOR_RESIZE_UP:  id = SWT.CURSOR_SIZEN; break;
            case Cursor.CURSOR_RESIZE_DOWN:  id = SWT.CURSOR_SIZES; break;
            case Cursor.CURSOR_RESIZE_LEFT:  id = SWT.CURSOR_SIZEW; break;
            case Cursor.CURSOR_RESIZE_RIGHT:  id = SWT.CURSOR_SIZEE; break;
            case Cursor.CURSOR_OPEN_HAND:
            case Cursor.CURSOR_CLOSED_HAND:
            case Cursor.CURSOR_POINTING_HAND:      id = SWT.CURSOR_HAND; break;
            case Cursor.CURSOR_MOVE:      id = SWT.CURSOR_SIZEALL; break;
            case Cursor.CURSOR_DISAPPEAR:
                //TODO - implement disappear cursor
                break;
            case Cursor.CURSOR_RESIZE_LEFTRIGHT:  id = SWT.CURSOR_SIZEWE; break;
            case Cursor.CURSOR_RESIZE_UPDOWN:  id = SWT.CURSOR_SIZENS; break;
            case Cursor.CURSOR_NONE:
                //TODO - implement hidden cursor / no cursor
                break;
            case Cursor.CURSOR_CUSTOM: {
                org.eclipse.swt.graphics.Cursor swtCursor = ((SWTCursor)cursor).cursor;
                if (swtCursor != null) {
                    shell.setCursor(swtCursor);
                    return;
                }
                break;
            }
        }
        Display display = Display.getDefault();
        org.eclipse.swt.graphics.Cursor swtCursor = display.getSystemCursor(id);
        shell.setCursor(swtCursor);
    }

    //TODO - implement IME
    @Override
    native protected void _requestInput(long ptr, String text, int type, double width, double height,
                                                    double Mxx, double Mxy, double Mxz, double Mxt,
                                                    double Myx, double Myy, double Myz, double Myt,
                                                    double Mzx, double Mzy, double Mzz, double Mzt);
    
    @Override
    native protected void _releaseInput(long ptr);
    
    @Override protected int _getEmbeddedX(long ptr) {
        // TODO: implement for child windows
        return 0;
    }
    
    @Override protected int _getEmbeddedY(long ptr) {
        // TODO: implement for child windows
        return 0;
    }

}

