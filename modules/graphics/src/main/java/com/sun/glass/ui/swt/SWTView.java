/*
 * Copyright (c) 2012, 2013, Oracle  and/or its affiliates. All rights reserved.
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

import java.nio.IntBuffer;
import java.util.Map;

import com.sun.glass.events.*;
import com.sun.glass.ui.*;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.graphics.*;
//import org.eclipse.swt.internal.cocoa.OS;

final class SWTView extends View {
    Canvas canvas;
    DropTarget dropTarget;

    static Shell hiddenShell;
    
    public SWTView() {
        super();
    }

    //TODO - implement IME
    @Override protected void _enableInputMethodEvents(long ptr, boolean enable) { }

    @Override protected long _create(Map caps) {
        if (hiddenShell == null) {
            hiddenShell = new Shell(Display.getDefault(), SWT.SHELL_TRIM);
            Display.getDefault().disposeExec(new Runnable () {
                public void run () {
                    hiddenShell.dispose();
                    hiddenShell = null;
                }
            });
        }
        int bits = SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE | SWT.LEFT_TO_RIGHT;// | SWT.NO_FOCUS;
        if (SWT.getPlatform().equals("cocoa")) {
            canvas = SWTApplication.createGLCanvas(hiddenShell, bits, caps);

//            NSView view = canvas.view;
//            long setWantsLayer_SEL = OS.sel_registerName("setWantsLayer:");
//            OS.objc_msgSend(view.id, setWantsLayer_SEL, true);
        } else {
            canvas = new Canvas(hiddenShell, bits);
        }
        canvas.setData(this);
        Listener keyListener = new Listener () {
            public void handleEvent(Event event) {
                sendKeyEvent(event);
            }
        };
        int [] keyEvents = new int[] {
            SWT.KeyDown,
            SWT.KeyUp,
        };
        for (int i=0; i<keyEvents.length; i++) {
            canvas.addListener (keyEvents[i], keyListener);
        }
        Listener mouseListener = new Listener () {
            public void handleEvent(Event event) {
                sendMouseEvent(event);
            }
        };
        int [] mouseEvents = new int[] {
            SWT.MouseDown,
            SWT.MouseUp,
            SWT.MouseMove,
            SWT.MouseEnter,
            SWT.MouseExit,
            SWT.MouseHorizontalWheel,
            SWT.MouseVerticalWheel,
            SWT.MenuDetect,
        };
        for (int i=0; i<mouseEvents.length; i++) {
            canvas.addListener (mouseEvents[i], mouseListener);
        }
        canvas.addListener(SWT.Paint, new Listener() {
            public void handleEvent(Event event) {
                notifyRepaint(event.x, event.y, event.width, event.height);
            }
        });
        canvas.addListener(SWT.Resize, new Listener () {
            public void handleEvent(Event event) {
                org.eclipse.swt.graphics.Rectangle rect = canvas.getClientArea();
                notifyResize(rect.width, rect.height);
            }
        });
        
        //TODO - refactor to drop target creation in a better place
        dropTarget = SWTClipboard.createDropTarget(canvas);
        
        return SWTApplication.getHandle(canvas);
    }

    long layerID = 0;
    @Override public int getNativeRemoteLayerId(String serverName) {
        if (layerID != 0) return (int)layerID;
        // used when run inside plugin
/*
//        long JRSRemoteLayer_class = OS.objc_getClass("JRSRenderServer");

        Class<?> OS = Class.forName("org.eclipse.swt.internal.cocoa.OS");
        Method objc_getClass = OS.getDeclaredMethod("objc_getClass", String.class);
        long JRSRemoteLayer_class = (Long)objc_getClass.invoke(OS, "JRSRenderServer");
        
        
        //RemoteLayerStartServer
//        long startRenderServer_SEL = OS.sel_registerName("startRenderServer");
//        long result = OS.objc_msgSend(JRSRemoteLayer_class, startRenderServer_SEL);
//        System.out.println("sent startRenderServer="+result);
        
        //RemoteLayerGetServerPort
        System.out.println("connecting to " + serverName);
        NSString str = (NSString) new NSString().alloc();
        str = NSString.stringWith(serverName);
        System.out.println("class="+JRSRemoteLayer_class);
        long recieveRenderServer_SEL = OS.sel_registerName("recieveRenderServer:");
        System.out.println("SEL="+recieveRenderServer_SEL);
        System.out.println("sending msg getPort");
        long port = OS.objc_msgSend(JRSRemoteLayer_class, recieveRenderServer_SEL, str.id);
        System.out.println("port="+port);
        
        //RemoteLayerGetRemoteFromLocal
//        long localLayer = getNativeLayer();
        NSView view = canvas.view;
//
//        long setWantsLayer_SEL = OS.sel_registerName("setWantsLayer:");
//        OS.objc_msgSend(view.id, setWantsLayer_SEL, true);
        
        long layer_SEL = OS.sel_registerName("layer");
        long localLayer = OS.objc_msgSend(view.id, layer_SEL);
        System.out.println("localLayer="+localLayer);
        long createRemoteLayer_SEL = OS.sel_registerName("createRemoteLayerBoundTo:");
        System.out.println("sending msg createRemoteLayerBoundTo");
        long remoteLayer = OS.objc_msgSend(localLayer, createRemoteLayer_SEL, port);
        System.out.println("remoter layer =" + remoteLayer);
        
        //RemoteLayerGetIdForRemote
        long layerID_SEL = OS.sel_registerName("layerID");
        System.out.println("sending msg layerID_SEL");
        layerID = OS.objc_msgSend(remoteLayer, layerID_SEL);
        System.out.println("returning layerID="+layerID);
        
        String s = Display.getAppVersion();
        if (s==null) s = "";
        s+= " create remote layer " + layerID;
        Display.setAppVersion(s);
        */

        return (int)layerID;
    }

    @Override protected long _getNativeView(long ptr) {
        return ptr;
    }

    @Override protected  int _getX(long ptr) {
        //TODO - implement offset in parent
        return 0;
    }
    @Override protected int _getY(long ptr) {
        //TODO - implement offset in parent
        return 0;
    }

    @Override protected boolean _close(long ptr) {
        //TODO - implement destroy of a view
        return false;
    }

    @Override protected void _scheduleRepaint(long ptr) {
        canvas.redraw();
    }

    @Override protected  void _begin(final long ptr) {
        SWTApplication.lockFocus(canvas);
    }
    
    @Override protected void _end(final long ptr) {
        SWTApplication.unlockFocus(canvas);
    }

    @Override protected boolean _enterFullscreen(long ptr, boolean animate, boolean keepRatio, boolean hideCursor) {
        canvas.getShell().setFullScreen(true);
        if (canvas.getShell().getFullScreen()) {
            notifyView(ViewEvent.FULLSCREEN_ENTER);
            return true;
        }
        return false;
    };
    @Override protected void _exitFullscreen(long ptr, boolean animate) {
        canvas.getShell().setFullScreen(false);
        if (!canvas.getShell().getFullScreen()) {
            notifyView(ViewEvent.FULLSCREEN_EXIT);
        }
    };

    @Override  protected void _setParent(long ptr, long parentPtr) {
        //TODO - implement set parent (is this necessary?)
        //throw new RuntimeException("SWTView._setParent not implemented.");
    }
    
    @Override protected void _uploadPixels(long ptr, Pixels pixels) {
        //TODO - optimize pixel uploading
        int width = pixels.getWidth(), height = pixels.getHeight();
        int [] bytes = ((IntBuffer)pixels.getPixels()).array();
        PaletteData palette = new PaletteData(0x00ff0000, 0x0000ff00, 0x000000ff);
        
        //long t0 = System.currentTimeMillis();
        ImageData imageData = new ImageData(width, height, 32, palette);
        //long t1 = System.currentTimeMillis();
        //System.out.println("new ImageData: " + (t1-t0));
        
        imageData.setPixels(0, 0, width * height, bytes, 0);
        //long t2 = System.currentTimeMillis();
        //System.out.println("setPixels: " + (t2-t1));
        
        Image image = new Image(canvas.getDisplay(), imageData);
        //long t3 = System.currentTimeMillis();
        //System.out.println("new Image: " + (t3-t2));
        
        GC gc = new GC (canvas);
        //long t4 = System.currentTimeMillis();
        //System.out.println("new GC: " + (t4-t3));
        
        gc.drawImage(image, 0, 0);
        //long t5 = System.currentTimeMillis();
        //System.out.println("drawImage: " + (t5-t4));
        
        image.dispose();
        //long t6 = System.currentTimeMillis();
        //System.out.println("image.dispose(): " + (t6-t5));
        
        gc.dispose();
        //long t7 = System.currentTimeMillis();
        //System.out.println("gc.dispose(): " + (t7-t6));
    }

    void sendKeyEvent (Event event) {
        //TODO - should the notifyXXX be called instead?
        EventHandler eventHandler = getEventHandler();
        if (eventHandler == null) return;
        long time = System.nanoTime();
        int keyCode = SWTApplication.getKeyCode(event);
        int modifiers = SWTApplication.getModifiers(event);
        int action = event.type == SWT.KeyDown ? KeyEvent.PRESS : KeyEvent.RELEASE;
        char[] chars = new char[] { event.character };
        eventHandler.handleKeyEvent(this, time, action, keyCode, chars, modifiers);
        if (event.character != '\0' && event.type == SWT.KeyDown) {
            eventHandler.handleKeyEvent(this, time, KeyEvent.TYPED, keyCode, chars, modifiers);
        }
    }

    void sendMouseEvent (Event event) {
        //TODO - should the notifyXXX be called instead?
        EventHandler eventHandler = getEventHandler();
        if (eventHandler == null) return;
        long time = System.nanoTime();
        int type = 0;
        switch (event.type) {
            case SWT.MouseDown:
                type = MouseEvent.DOWN;
                if ((canvas.getShell().getStyle() & SWT.NO_FOCUS) != 0) {
                    canvas.forceFocus();
                }
                break;
            case SWT.MouseUp:
                type = MouseEvent.UP;
                break;
            case SWT.MouseMove:
                if ((event.stateMask & SWT.BUTTON_MASK) != 0) {
                    type = MouseEvent.DRAG;
                } else {
                    type = MouseEvent.MOVE;
                }
                break;
            case SWT.MouseEnter:
                type = MouseEvent.ENTER;
                break;
            case SWT.MouseExit:
                type = MouseEvent.EXIT;
                break;
            case SWT.MouseHorizontalWheel:
                type = MouseEvent.WHEEL;
                break;
            case SWT.MouseVerticalWheel:
                type = MouseEvent.WHEEL;
                break;
            case SWT.MenuDetect:
                break;
        }
        int button = SWTApplication.getButton(event);
        int modifiers = SWTApplication.getModifiers(event);
        switch (event.type) {
            case SWT.MouseHorizontalWheel:
                //TODO - horizontal mouse wheel not implemented
                break;
            case SWT.MouseVerticalWheel: {
                //TODO - mouse wheel not implemented, these values are hard coded
                Point point = canvas.toDisplay(event.x, event.y);
                eventHandler.handleScrollEvent(this, time, event.x, event.y, point.x, point.y, 0, event.count, modifiers, 1, 1, 1, 1, 1, 1);
                break;
            }
            case SWT.MenuDetect: {
                //TODO - compute trigger, don't hard code
                boolean isKeyboardTrigger = false;
                Point point = canvas.toControl(event.x, event.y);
                eventHandler.handleMenuEvent(this, point.x, point.y, event.x, event.y, isKeyboardTrigger);
                break;
            }
            default: {
                //TODO - compute trigger, don't hard code
                boolean isPopupTrigger = false;
                Point point = canvas.toDisplay(event.x, event.y);
                eventHandler.handleMouseEvent(this, time, type, button, event.x, event.y, point.x, point.y, modifiers, isPopupTrigger, false);
            }
        }
    }
    
    //TODO - fix visibility
    public void notifyDragStart(int button, int x, int y, int xAbs, int yAbs) {
        super. notifyDragStart(button, x, y, xAbs, yAbs);
    }
    
    //TODO - fix visibility
    public void notifyDragEnd(int performedAction) {
        super.notifyDragEnd(performedAction) ;
    }

    //TODO - fix visibility
    public int notifyDragEnter(int x, int y, int xAbs, int yAbs, int recommendedDropAction) {
        return super.notifyDragEnter(x, y, xAbs, yAbs, recommendedDropAction);
    }

    //TODO - fix visibility
    public int notifyDragOver(int x, int y, int xAbs, int yAbs, int recommendedDropAction) {
        return super.notifyDragOver(x, y, xAbs, yAbs, recommendedDropAction);
    }

    //TODO - fix visibility
    public void notifyDragLeave() {
        super.notifyDragLeave();
    }

    //TODO - fix visibility
    public int notifyDragDrop(int x, int y, int xAbs, int yAbs, int recommendedDropAction) {
        return super.notifyDragDrop(x, y, xAbs, yAbs, recommendedDropAction);
    }
}

