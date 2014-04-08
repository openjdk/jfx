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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sun.glass.events.*;
import com.sun.glass.ui.*;
import com.sun.glass.ui.CommonDialogs.*;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.opengl.*;

//TODO - implement browser plugin
//TODO - fix crash on some machines
//TODO - implement keyboard (IME, NLS, Windows key ...)
//TODO - implement screens (depths, associated callbacks)
//TODO - implement accessibility
//TODO - implement touch
//TODO - implement retina
//
//TODO - implement focus grabs
//TODO - implement robot wheel and getX/Y without thread check
//TODO - implement clipboard and drag and drop images (get image)
//TODO - implement clipboard and drag and drop multiple data transfer
//TODO - implement missing cursors (glass has custom cursors for resize etc.)
//TODO - cursor hide/show
//TODO - implement file dialog multiple filters for a single description

public final class SWTApplication extends Application {
    Object loopReturn;
    static final String IS_EVENTTHREAD_KEY = "javafx.embed.isEventThread";
    
    //TODO - Prism on Mac uses exactly two GL contexts and does not destroy them
    //TODO - use a context per top level window to better match the platform
    static long context = 0, shareContext = 0;

    void runSWTEventLoop(Runnable launchable) {
        Display display = Display.getDefault();
        setEventThread(display.getThread());
        display.asyncExec(launchable);
        while (!display.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
    }

    long getLauncherClass(final Runnable launchable, final long launcherSel) {
        try {
            Class<?> OS = Class.forName("org.eclipse.swt.internal.cocoa.OS");
            //TODO - callback free'd when we exit() to the operating system
            Callback callback = new Callback(new Object() {
                long launcherProc(long /*int*/ id, long /*int*/ sel) {
                    //System.out.println("[launcherProc]");
                    if (sel == launcherSel) {
                        runSWTEventLoop(launchable);
                    }
                    return 0;
                }
            }, "launcherProc", 2);
            long proc2 = callback.getAddress();
            Method objc_getClass = OS.getDeclaredMethod("objc_getClass", String.class);
            long NSObject_class = (Long)objc_getClass.invoke(OS, "NSObject");
            Method objc_allocateClassPair = OS.getDeclaredMethod("objc_allocateClassPair", Long.TYPE, String.class, Long.TYPE);
            long launcherClass = (long) objc_allocateClassPair.invoke(OS, NSObject_class, "Proc", 0);
            Method class_addMethod = OS.getDeclaredMethod("class_addMethod", Long.TYPE, Long.TYPE, Long.TYPE, String.class);
            class_addMethod.invoke(OS, launcherClass, launcherSel, proc2, "@:");
            Method objc_registerClassPair = OS.getDeclaredMethod("objc_registerClassPair", Long.TYPE);
            objc_registerClassPair.invoke(OS, launcherClass);
            //System.out.println("[class registered="+launcherClass+"]");
            return launcherClass;
        } catch (Exception e) {
            return 0;
        }
//        if (launcherClass == 0) {
//            Callback callback = new Callback(this, "launcherProc", 2);
//            long proc2 = callback.getAddress();
//            launcherClass = OS.objc_allocateClassPair(OS.objc_getClass("NSObject"), "Proc", 0);
//            launcherSel = OS.sel_registerName("launcherSel");
//            OS.class_addMethod(launcherClass, launcherSel, proc2, "@:");
//            OS.objc_registerClassPair(launcherClass);
//            System.out.println("[class registered="+launcherClass+"]");
//        }
    }
    
    void runCocoaLoop(Runnable launchable) {
        try {
            Class<?> OS = Class.forName("org.eclipse.swt.internal.cocoa.OS");
            Method objc_msgSend_bool = OS.getDeclaredMethod("objc_msgSend_bool", Long.TYPE, Long.TYPE);
            long class_NSThread = OS.getDeclaredField("class_NSThread").getLong(OS);
            long sel_isMainThread = OS.getDeclaredField("sel_isMainThread").getLong(OS);
            boolean isMainThread = (Boolean)objc_msgSend_bool.invoke(OS, class_NSThread, sel_isMainThread);
            if (isMainThread) {
                runSWTEventLoop(launchable);
            } else {
                //System.out.println("[wrong thread]");
                Method sel_registerName = OS.getDeclaredMethod("sel_registerName", String.class);
                final long launcherSel = (long )sel_registerName.invoke(OS, "launcherSel");
                long launcherClass = getLauncherClass(launchable, launcherSel);
                long sel_alloc = OS.getDeclaredField("sel_alloc").getLong(OS);
                long sel_init = OS.getDeclaredField("sel_init").getLong(OS);
                long sel_performSelectorOnMainThread_withObject_waitUntilDone_ = OS.getDeclaredField("sel_performSelectorOnMainThread_withObject_waitUntilDone_").getLong(OS);
                long sel_release = OS.getDeclaredField("sel_release").getLong(OS);
                Method objc_msgSendLL = OS.getDeclaredMethod("objc_msgSend", Long.TYPE, Long.TYPE);
                Method objc_msgSendLLLLZ = OS.getDeclaredMethod("objc_msgSend", Long.TYPE, Long.TYPE, Long.TYPE, Long.TYPE, Boolean.TYPE);
                long id = (Long)objc_msgSendLL.invoke(OS, launcherClass, sel_alloc);
                id = (Long)objc_msgSendLL.invoke(OS, id, sel_init);
                objc_msgSendLLLLZ.invoke(OS, id, sel_performSelectorOnMainThread_withObject_waitUntilDone_, launcherSel, 0, false);
                objc_msgSendLL.invoke(OS, id, sel_release);
                //System.out.println("[message sent]");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (NSThread.isMainThread()) {
//            runSWTEventLoop();
//        } else {
//            System.out.println("[wrong thread]");
//            long cls = getLauncherClass();
//            long id = OS.objc_msgSend(cls, OS.sel_alloc);
//            NSObject obj = new NSObject(id);
//            obj.init();
//            obj.performSelectorOnMainThread(launcherSel, null, false);
//            obj.release();
//            System.out.println("[message sent]");
//        }
    }
    
    @Override
    protected void runLoop(final Runnable launchable) {
        if ("true".equals(System.getProperty(IS_EVENTTHREAD_KEY, "false"))) {
            Display display = Display.getDefault();
            setEventThread(display.getThread());
            launchable.run();
            return;
        }
        if (SWT.getPlatform().equals("cocoa")) {
            runCocoaLoop(launchable);
        } else {
            // the current thread can't block as the caller is waiting on it
            new Thread(() -> runSWTEventLoop(launchable)).start();
        }
    }

    @Override
    protected void finishTerminating() {
        if ("true".equals(System.getProperty(IS_EVENTTHREAD_KEY, "false"))) {
            return;
        }
        Display.getDefault().dispose();
    }

    @Override
    public Window createWindow(Window owner, Screen screen, int styleMask) {
        return new SWTWindow(owner, screen, styleMask);
    }

    final static long BROWSER_PARENT_ID = -1L;
    @Override
    public Window createWindow(long parent) {
        /* called by the applet code */
        SWTWindow window = new SWTWindow(parent);
        if (parent == BROWSER_PARENT_ID) {
            // Special case: a Mac embedded window, which is a parent to other child Windows.
            // Needs implicit view, with a layer that will be provided to the plugin
            window.setView(createView());
        }
        return window;
    }

    @Override
    public View createView() {
        return new SWTView();
    }
    
    @Override
    public Cursor createCursor(int type) {
        return new SWTCursor(type);
    }

    @Override
    public Cursor createCursor(int x, int y, Pixels pixels) {
        return new SWTCursor(x, y, pixels);
    }

    @Override
    protected void staticCursor_setVisible(boolean visible) {
        //TODO - cursor hide/show not implemented
    }

    @Override
    protected Size staticCursor_getBestSize(int width, int height) {
        Point [] sizes = Display.getDefault().getCursorSizes();
        return sizes.length > 0 ? new Size(sizes[0].x, sizes[0].y) : null;
    }

    @Override
    public Pixels createPixels(int width, int height, ByteBuffer data) {
        return new SWTPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data) {
        return new SWTPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data, float scale) {
        return new SWTPixels(width, height, data, scale);
    }

    @Override
    protected int staticPixels_getNativeFormat() {
        return Pixels.Format.BYTE_BGRA_PRE;
    }

    @Override
    public Robot createRobot() {
        return new SWTRobot();
    }

    @Override protected double staticScreen_getVideoRefreshPeriod() {
        //TODO - vsync not implemented
        return 0;
    }

    //TODO - get rid of reflection
    //TODO - implement multiple screens
    //TODO - implement resolution changed
    @Override protected Screen[] staticScreen_getScreens() {
        Display display = Display.getDefault();
        final Screen[] screens = new Screen[1];
        try {
            Constructor<Screen> screenConstructor = Screen.class.getDeclaredConstructor(
                    long.class, int.class, int.class, int.class,
                    int.class, int.class, int.class, int.class,
                    int.class, int.class, int.class, int.class,
                    float.class);
            screenConstructor.setAccessible(true);

            Monitor monitor = display.getPrimaryMonitor();
            Rectangle bounds = monitor.getBounds();
            Rectangle client = monitor.getClientArea();
            int depth = display.getDepth();
            Point dpi = display.getDPI();
            screens[0] = screenConstructor.newInstance(
                    1L,
                    depth,
                    bounds.x, bounds.y,
                    bounds.width, bounds.height,
                    client.x, client.y,
                    client.width, client.height,
                    dpi.x, dpi.y,
                    1.0f);

            return screens;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to construct a Screen", e);
        }
    }

    @Override
    public Timer createTimer(Runnable runnable) {
        return new SWTTimer(runnable);
    }

    @Override
    protected int staticTimer_getMinPeriod() {
        return 0;
    }

    @Override
    protected int staticTimer_getMaxPeriod() {
        return 100000;
    }

    @Override
    protected FileChooserResult staticCommonDialogs_showFileChooser(Window owner, String folder,
                                    String filename, String title, int type, boolean multipleMode,
                                    ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
        int bits = SWT.APPLICATION_MODAL;
        if (multipleMode) bits |= SWT.MULTI;
        switch (type) {
            case Type.OPEN: bits |= SWT.OPEN; break;
            case Type.SAVE: bits |= SWT.SAVE; break;
        }
        Shell parent = ((SWTWindow)owner).shell;
        FileDialog dialog = new FileDialog(parent, bits);
        dialog.setText(title);
        String [] filters = new String [extensionFilters.length];
        String [] extensions = new String [extensionFilters.length];
        for (int i=0; i<extensionFilters.length; i++) {
            filters [i] = extensionFilters[i].getDescription();
            List<String> list = extensionFilters[i].getExtensions();
            if (list.size() > 0) extensions[i] = list.get(0);
        }
        dialog.setFilterNames(filters);
        dialog.setFilterExtensions(extensions);
        dialog.setFilterPath(folder);
        dialog.setFilterIndex(defaultFilterIndex);
        dialog.setFileName(filename);
        if (dialog.open() == null) return new FileChooserResult();
        String path = dialog.getFilterPath();
        String [] names = dialog.getFileNames();
        String [] result = new String [names.length];
        for (int i=0; i<names.length; i++) {
            result [i] = path + names [i];
        }
        List<File> l = new java.util.ArrayList<File>();
        for (String s : result) {
            l.add(new File(s));
        }
        //TODO: support FileChooserResult
        return new FileChooserResult(l, null);
    }

    @Override
    protected File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title) {
        int bits = SWT.APPLICATION_MODAL;
        Shell parent = ((SWTWindow)owner).shell;
        DirectoryDialog dialog = new DirectoryDialog(parent, bits);
        dialog.setText(title);
        String result = dialog.open();
        return result == null ? null : new File(result);
    }
    
    @Override protected Object _enterNestedEventLoop() {
        loopReturn = null;
        while (loopReturn == null) {
            if (!Display.getDefault().readAndDispatch()) {
                Display.getDefault().sleep();
            }
        }
        try {
            return loopReturn;
        } finally {
            loopReturn = null;
        }
    }
    @Override protected void _leaveNestedEventLoop(Object retValue) {
        loopReturn = retValue;
    }

    @Override protected long staticView_getMultiClickTime() {
        return Display.getDefault().getDoubleClickTime();
    }

    @Override protected int staticView_getMultiClickMaxX() {
        return 4;
    }

    @Override protected int staticView_getMultiClickMaxY() {
        return 4;
    }

    @Override protected void _invokeAndWait(Runnable runnable) {
        Display.getDefault().syncExec(runnable);
    }

    @Override protected void _invokeLater(Runnable runnable) {
        Display.getDefault().asyncExec(runnable);
    }

    @Override
    protected boolean _supportsSystemMenu() {
        return SWT.getPlatform().equals("cocoa");
    }
    
    @Override
    protected boolean _supportsTransparentWindows() {
        return SWT.getPlatform().equals("cocoa");
    }

    @java.lang.Override protected boolean _supportsUnifiedWindows() {
        return false;
    }

    static final int [] [] KeyTable = {
        {KeyEvent.VK_UNDEFINED,     SWT.NULL},
        
        // SWT only
        {'\n' /*KeyEvent.VK_?????*/,         SWT.CR},
        
        // Misc
        {'\n' /*KeyEvent.VK_ENTER*/,         SWT.LF},
        {'\b' /*KeyEvent.VK_BACKSPACE*/,     SWT.BS},
        {'\t' /*KeyEvent.VK_TAB*/,           SWT.TAB},
//      {KeyEvent.VK_CANCEL         SWT.???},
//      {KeyEvent.VK_CLEAR          SWT.???},
//      {KeyEvent.VK_PAUSE          SWT.???},
        {KeyEvent.VK_ESCAPE,        SWT.ESC},
        {KeyEvent.VK_SPACE,         0x20},
        {KeyEvent.VK_DELETE,        SWT.DEL},
//      {KeyEvent.VK_PRINTSCREEN    SWT.???;
        {KeyEvent.VK_INSERT,        SWT.INSERT},
        {KeyEvent.VK_HELP,          SWT.HELP},
        
        // Modifiers
        {KeyEvent.VK_SHIFT,         SWT.SHIFT},
        {KeyEvent.VK_CONTROL,       SWT.CONTROL},
        {KeyEvent.VK_ALT,           SWT.ALT},
        {KeyEvent.VK_WINDOWS,       SWT.COMMAND},
 //     {KeyEvent.VK_CONTEXT_MENU,  SWT.???},
        {KeyEvent.VK_CAPS_LOCK,     SWT.CAPS_LOCK},
        {KeyEvent.VK_NUM_LOCK,      SWT.NUM_LOCK},
        {KeyEvent.VK_SCROLL_LOCK,   SWT.SCROLL_LOCK},
        
        // Navigation keys
        {KeyEvent.VK_PAGE_UP,       SWT.PAGE_UP},
        {KeyEvent.VK_PAGE_DOWN,     SWT.PAGE_DOWN},
        {KeyEvent.VK_END,           SWT.END},
        {KeyEvent.VK_HOME,          SWT.HOME},
        {KeyEvent.VK_LEFT,          SWT.ARROW_LEFT},
        {KeyEvent.VK_UP,            SWT.ARROW_UP},
        {KeyEvent.VK_RIGHT,         SWT.ARROW_RIGHT},
        {KeyEvent.VK_DOWN,          SWT.ARROW_DOWN},
    
        // Misc 2
        //TODO - suspect this only works for English keyboard
        {KeyEvent.VK_COMMA,                 ','}, // ','
        {KeyEvent.VK_MINUS,                 '-'}, // '-'
        {KeyEvent.VK_PERIOD,                '.'}, // '.'
        {KeyEvent.VK_SLASH,                 '/'}, // '/'
        {KeyEvent.VK_SEMICOLON,             ';'}, // ';'
        {KeyEvent.VK_EQUALS,                '='}, // '='
        {KeyEvent.VK_OPEN_BRACKET,          '['}, // '['
        {KeyEvent.VK_BACK_SLASH,            '\\'}, // '\'
        {KeyEvent.VK_CLOSE_BRACKET,         ']'}, // ']'

        // Numeric key pad keys
        {KeyEvent.VK_MULTIPLY,     SWT.KEYPAD_MULTIPLY}, // '*'
        {KeyEvent.VK_ADD,          SWT.KEYPAD_ADD}, // '+'
//        {KeyEvent.VK_SEPARATOR,    SWT.???},
        {KeyEvent.VK_SUBTRACT,     SWT.KEYPAD_SUBTRACT},
        {KeyEvent.VK_DECIMAL,      SWT.KEYPAD_DECIMAL},
        {KeyEvent.VK_DIVIDE,       SWT.KEYPAD_DIVIDE},
//        {KeyEvent.VK_????,         SWT.KEYPAD_EQUAL},
//        {KeyEvent.VK_????,         SWT.KEYPAD_CR},

        {KeyEvent.VK_AMPERSAND,             '@'},
        {KeyEvent.VK_ASTERISK,              '*'},

        {KeyEvent.VK_DOUBLE_QUOTE,          '"'}, // '"'
        {KeyEvent.VK_LESS,                  '<'}, // '<'
        {KeyEvent.VK_GREATER,               '>'}, // '>'
        {KeyEvent.VK_BRACELEFT,             '{'}, // '{'
        {KeyEvent.VK_BRACERIGHT,            '}'}, // '}'
        {KeyEvent.VK_BACK_QUOTE,            '`'}, // '`'
        {KeyEvent.VK_QUOTE,                 '\''}, // '''
        {KeyEvent.VK_AT,                    '@'}, // '@'
        {KeyEvent.VK_COLON,                 ':'}, // ':'
        {KeyEvent.VK_CIRCUMFLEX,            '^'}, // '^'
        {KeyEvent.VK_DOLLAR,                '$'}, // '$'
//      {KeyEvent.VK_EURO_SIGN,             0x0204},
        {KeyEvent.VK_EXCLAMATION,           '!'}, // '!'
//      {KeyEvent.VK_INV_EXCLAMATION,       0x0206},
        {KeyEvent.VK_LEFT_PARENTHESIS,      '('}, // '('
        {KeyEvent.VK_NUMBER_SIGN,           '#'}, // '#'
        {KeyEvent.VK_PLUS,                  '+'}, // '+'
        {KeyEvent.VK_RIGHT_PARENTHESIS,      ')'}, // ')'
        {KeyEvent.VK_UNDERSCORE,             '_'}, // '_'

        // Numeric keys
        //TODO - suspect this only works for English keyboard
        {KeyEvent.VK_0, '0'}, // '0'
        {KeyEvent.VK_1, '1'}, // '1'
        {KeyEvent.VK_2, '2'}, // '2'
        {KeyEvent.VK_3, '3'}, // '3'
        {KeyEvent.VK_4, '4'}, // '4'
        {KeyEvent.VK_5, '5'}, // '5'
        {KeyEvent.VK_6, '6'}, // '6'
        {KeyEvent.VK_7, '7'}, // '7'
        {KeyEvent.VK_8, '8'}, // '8'
        {KeyEvent.VK_9, '9'}, // '9'

        // Alpha keys
        //TODO - suspect this only works for English keyboard
        {KeyEvent.VK_A, 'a'}, // 'A'
        {KeyEvent.VK_B, 'b'}, // 'B'
        {KeyEvent.VK_C, 'c'}, // 'C'
        {KeyEvent.VK_D, 'd'}, // 'D'
        {KeyEvent.VK_E, 'e'}, // 'E'
        {KeyEvent.VK_F, 'f'}, // 'F'
        {KeyEvent.VK_G, 'g'}, // 'G'
        {KeyEvent.VK_H, 'h'}, // 'H'
        {KeyEvent.VK_I, 'i'}, // 'I'
        {KeyEvent.VK_J, 'j'}, // 'J'
        {KeyEvent.VK_K, 'k'}, // 'K'
        {KeyEvent.VK_L, 'l'}, // 'L'
        {KeyEvent.VK_M, 'm'}, // 'M'
        {KeyEvent.VK_N, 'n'}, // 'N'
        {KeyEvent.VK_O, 'o'}, // 'O'
        {KeyEvent.VK_P, 'p'}, // 'P'
        {KeyEvent.VK_Q, 'q'}, // 'Q'
        {KeyEvent.VK_R, 'r'}, // 'R'
        {KeyEvent.VK_S, 's'}, // 'S'
        {KeyEvent.VK_T, 't'}, // 'T'
        {KeyEvent.VK_U, 'u'}, // 'U'
        {KeyEvent.VK_V, 'v'}, // 'V'
        {KeyEvent.VK_W, 'w'}, // 'W'
        {KeyEvent.VK_X, 'x'}, // 'X'
        {KeyEvent.VK_Y, 'y'}, // 'Y'
        {KeyEvent.VK_Z, 'z'}, // 'Z'

        // Numpad keys
        {KeyEvent.VK_NUMPAD0,   SWT.KEYPAD_0},
        {KeyEvent.VK_NUMPAD1,   SWT.KEYPAD_1},
        {KeyEvent.VK_NUMPAD2,   SWT.KEYPAD_2},
        {KeyEvent.VK_NUMPAD3,   SWT.KEYPAD_3},
        {KeyEvent.VK_NUMPAD4,   SWT.KEYPAD_4},
        {KeyEvent.VK_NUMPAD5,   SWT.KEYPAD_5},
        {KeyEvent.VK_NUMPAD6,   SWT.KEYPAD_6},
        {KeyEvent.VK_NUMPAD7,   SWT.KEYPAD_7},
        {KeyEvent.VK_NUMPAD8,   SWT.KEYPAD_8},
        {KeyEvent.VK_NUMPAD9,   SWT.KEYPAD_9},
        
        // Function keys
        {KeyEvent.VK_F1,    SWT.F1},
        {KeyEvent.VK_F2,    SWT.F2},
        {KeyEvent.VK_F3,    SWT.F3},
        {KeyEvent.VK_F4,    SWT.F4},
        {KeyEvent.VK_F5,    SWT.F5},
        {KeyEvent.VK_F6,    SWT.F6},
        {KeyEvent.VK_F7,    SWT.F7},
        {KeyEvent.VK_F8,    SWT.F8},
        {KeyEvent.VK_F9,    SWT.F9},
        {KeyEvent.VK_F10,   SWT.F10},
        {KeyEvent.VK_F11,   SWT.F11},
        {KeyEvent.VK_F12,   SWT.F12},

    //TODO - map these to FX keys
//    /* Numeric Keypad Keys */
//    {KeyEvent.VK_MULTIPLY,    SWT.KEYPAD_MULTIPLY},
//    {KeyEvent.VK_ADD,         SWT.KEYPAD_ADD},
//    {KeyEvent.VK_RETURN,      SWT.KEYPAD_CR},
//    {KeyEvent.VK_SUBTRACT,    SWT.KEYPAD_SUBTRACT},
//    {KeyEvent.VK_DECIMAL,     SWT.KEYPAD_DECIMAL},
//    {KeyEvent.VK_DIVIDE,      SWT.KEYPAD_DIVIDE},
////  {KeyEvent.VK_????,        SWT.KEYPAD_EQUAL},
    };
    
    static int getKeyCode(Event event) {
        int keyCode = event.keyCode;
        for (int i=0; i<KeyTable.length; i++) {
            if (KeyTable [i] [1] == keyCode) return KeyTable [i] [0];
        }
        return 0;
    }
    
    static int getSWTKeyCode(int keyCode) {
        for (int i=0; i<KeyTable.length; i++) {
            if (KeyTable [i] [0] == keyCode) return KeyTable [i] [1];
        }
        return 0;
    }
    
    static int getModifiers(Event event) {
        int stateMask = event.stateMask;
        if (event.type == SWT.KeyDown) {
            if (event.keyCode == SWT.SHIFT) stateMask |= SWT.SHIFT;
            if (event.keyCode == SWT.CONTROL) stateMask |= SWT.CONTROL;
            if (event.keyCode == SWT.ALT) stateMask |= SWT.ALT;
            if (event.keyCode == SWT.COMMAND) stateMask |= SWT.COMMAND;
        }
        if (event.type == SWT.KeyUp) {
            if (event.keyCode == SWT.SHIFT) stateMask &= ~SWT.SHIFT;
            if (event.keyCode == SWT.CONTROL) stateMask &= ~SWT.CONTROL;
            if (event.keyCode == SWT.ALT) stateMask &= ~SWT.ALT;
            if (event.keyCode == SWT.COMMAND) stateMask &= ~SWT.COMMAND;
        }
        int modifiers = 0;
        if ((stateMask & SWT.SHIFT) != 0) {
            modifiers |= KeyEvent.MODIFIER_SHIFT;
        }
        if ((stateMask & SWT.CTRL) != 0) {
            modifiers |= KeyEvent.MODIFIER_CONTROL;
        }
        if ((stateMask & SWT.ALT) != 0) {
            modifiers |= KeyEvent.MODIFIER_ALT;
        }
        //TODO - can't get Windows key from SWT
        if ((stateMask & SWT.COMMAND) != 0) {
            modifiers |= KeyEvent.MODIFIER_COMMAND;
        }
        if (event.type == SWT.MouseDown) {
            if (event.button == 1) stateMask |= SWT.BUTTON1;
            if (event.button == 2) stateMask |= SWT.BUTTON2;
            if (event.button == 3) stateMask |= SWT.BUTTON3;
            if (event.button == 4) stateMask |= SWT.BUTTON4;
            if (event.button == 5) stateMask |= SWT.BUTTON5;
        }
        if (event.type == SWT.MouseUp) {
            if (event.button == 1) stateMask &= ~SWT.BUTTON1;
            if (event.button == 2) stateMask &= ~SWT.BUTTON2;
            if (event.button == 3) stateMask &= ~SWT.BUTTON3;
            if (event.button == 4) stateMask &= ~SWT.BUTTON4;
            if (event.button == 5) stateMask &= ~SWT.BUTTON5;
        }
        if ((stateMask & SWT.BUTTON1) != 0) {
            modifiers |= KeyEvent.MODIFIER_BUTTON_PRIMARY;
        }
        if ((stateMask & SWT.BUTTON2) != 0) {
            modifiers |= KeyEvent.MODIFIER_BUTTON_MIDDLE;
        }
        if ((stateMask & SWT.BUTTON3) != 0) {
            modifiers |= KeyEvent.MODIFIER_BUTTON_SECONDARY;
        }
        return modifiers;
    }
    
    static int getButton (Event event) {
        int button = MouseEvent.BUTTON_NONE;
        if (event.button == 1 || (event.stateMask & SWT.BUTTON1) != 0) {
            button = MouseEvent.BUTTON_LEFT;
        }
        if (event.button == 2 || (event.stateMask & SWT.BUTTON2) != 0) {
            button = MouseEvent.BUTTON_OTHER;
        }
        if (event.button == 3 || (event.stateMask & SWT.BUTTON3) != 0) {
            button = MouseEvent.BUTTON_RIGHT;
        }
        return button;
    }
    
    /*
     * Use reflection so that the code that accesses SWT internals can be
     * shared on all platforms.  Since there are not too many platform
     * differences at this time, this is too bad at this point.  A better
     * solution is to add portable API to SWT that provides the internal
     * handles for widgets in a data object.
     */
    
    static Widget findWidget(long handle) {
        Method method;
        try {
            method = Display.class.getDeclaredMethod("findWidget", new Class []{long.class});
            method.setAccessible(true);
            return (Widget) method.invoke(Display.getDefault(), (long)handle);
        } catch (Exception e) {
            try {
                method = Display.class.getDeclaredMethod("findWidget", new Class []{int.class});
                method.setAccessible(true);
                return (Widget) method.invoke(Display.getDefault(), (int)handle);
            } catch (Exception e2) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static long getHandle (Shell shell) {
        if (SWT.getPlatform().equals("win32")) return getHandleW32(shell);
        if (SWT.getPlatform().equals("cocoa")) return getHandleCocoa(shell);
        if (SWT.getPlatform().equals("gtk")) return getHandleGTK(shell);
        return 0;
    }

    static long getHandle (Control control) {
        if (SWT.getPlatform().equals("win32")) return getHandleW32(control);
        if (SWT.getPlatform().equals("cocoa")) return getHandleCocoa(control);
        if (SWT.getPlatform().equals("gtk")) return getHandleGTK(control);
        return 0;
    }
    
    static long getHandle(Class clazz, Object object, String name) {
        Field field;
        try {
            field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            try {
                return field.getLong(object);
            } catch (Exception e) {
                try {
                    return field.getInt(object);
                } catch (Exception e2) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    static void setHandle(Class clazz, Object object, String name, long value) {
        Field field;
        try {
            field = clazz.getField(name);
            field.setAccessible(true);
            try {
                field.setLong(object, value);
            } catch (Exception e) {
                try {
                    field.setInt(object, (int)value);
                } catch (Exception e2) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static long getHandleW32(Control control) {
        return getHandle(Control.class, control, "handle");
    }
    
    static long getHandleGTK(Control control) {
        return getHandle(Widget.class, control, "handle");
    }
    

    static long getHandleGTK(Shell control) {
        return getHandle(control.getClass(), control, "shellHandle");
    }

    static long getHandleCocoa(Control control) {
        try {
            Field field = control.getClass().getField("view");
            field.setAccessible(true);
            Object view = field.get(control);
            Class clazz = Class.forName("org.eclipse.swt.internal.cocoa.id");
            return getHandle(clazz, view, "id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static long getHandleCocoa(Shell control) {
        try {
            Field field = control.getClass().getDeclaredField("window");
            field.setAccessible(true);
            Object view = field.get(control);
            Class clazz = Class.forName("org.eclipse.swt.internal.cocoa.id");
            return getHandle(clazz, view, "id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static void invokeLock(Control control, String name) {
        try {
            Field field = control.getClass().getField("view");
            field.setAccessible(true);
            Object view = field.get(control);
            Method method = view.getClass().getMethod(name);
            method.invoke(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static void lockFocus (Control control) {
        if (SWT.getPlatform().equals("cocoa")) {
            //invokeLock(control, "lockFocus");
            setView(control, new_NSOpenGLContext(context));
        }
    }
    
    static void unlockFocus (Control control) {
        //if (SWT.getPlatform().equals("cocoa")) invokeLock(control, "unlockFocus");
    }
    
    static Object new_NSOpenGLContext (long context) {
        try {
            Class clazz = Class.forName("org.eclipse.swt.internal.cocoa.NSOpenGLContext");
            Object object = clazz.newInstance();
            setHandle(clazz, object, "id", context);
            return object;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static void setView(Control control, Object context) {
        try {
            Field field = control.getClass().getField("view");
            field.setAccessible(true);
            Object view = field.get(control);
            Class clazz = Class.forName("org.eclipse.swt.internal.cocoa.NSView");
            Method method = context.getClass().getMethod("setView", new Class[] {clazz});
            method.invoke(context, view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //TODO - temporary code, get rid of reflection
    static void setTransparent(Shell shell) {
        if (SWT.getPlatform().equals("cocoa")) {
            try {
                Class clazz = Class.forName("org.eclipse.swt.internal.cocoa.NSOpenGLContext");
                Object object = new_NSOpenGLContext(context);
                Method method = clazz.getMethod("setValues", new Class[] {int [].class, int.class});
                method.invoke(object, new int[]{0}, 236 /*OS.NSOpenGLCPSurfaceOpacity*/);
                Field field = shell.getClass().getField("view");
                Object view = field.get(shell);
                /*Class*/ clazz = Class.forName("org.eclipse.swt.internal.cocoa.NSView");
                /*Method*/ method = clazz.getMethod("window", new Class[] {});
                Object window = method.invoke(view);
                /*Class*/ clazz = Class.forName("org.eclipse.swt.internal.cocoa.NSWindow");
                /*Method*/ method = clazz.getMethod("graphicsContext", new Class[] {});
                Object context = method.invoke(window);
                /*Class*/ clazz = Class.forName("org.eclipse.swt.internal.cocoa.NSGraphicsContext");
                /*Method*/ method = clazz.getMethod("saveGraphicsState", new Class[] {});
                method.invoke(context);
                /*Method*/ method = clazz.getMethod("setCurrentContext", new Class[] {clazz});
                method.invoke(null, context);
                /*Class*/ clazz = Class.forName("org.eclipse.swt.internal.cocoa.NSGraphicsContext");
                /*Method*/ method = clazz.getMethod("setCompositingOperation", new Class[] {long.class});
                method.invoke(context, /*OS.NSCompositeClear*/ 0);
                /*Class*/ clazz = Class.forName("org.eclipse.swt.internal.cocoa.NSRect");
                Object rect = clazz.newInstance();
                /*Field*/ field = clazz.getField("width");
                field.setInt(rect, 1024);
                /*Field*/ field = clazz.getField("height");
                field.setInt(rect, 1024);
                /*Class*/ clazz = Class.forName("org.eclipse.swt.internal.cocoa.NSBezierPath");
                /*Method*/ method = clazz.getMethod("fillRect", new Class[] {rect.getClass()});
                method.invoke(null, rect);
                /*Class*/ clazz = Class.forName("org.eclipse.swt.internal.cocoa.NSGraphicsContext");
                /*Method*/ method = clazz.getMethod("restoreGraphicsState", new Class[] {});
                method.invoke(context);
            } catch (Exception e) {
                //e.printStackTrace();
            }
            /*
            ((NSOpenGLContext)new_NSOpenGLContext(context)).setValues(new int[]{0}, 236); //OS.NSOpenGLCPSurfaceOpacity);
            NSWindow window = shell.view.window();
            NSGraphicsContext context = window.graphicsContext();
            context.saveGraphicsState();
            NSGraphicsContext.setCurrentContext(context);
            context.setCompositingOperation(OS.NSCompositeClear);
            NSRect frame = new NSRect();
            frame.width = 1024;
            frame.height = 1024;
            NSBezierPath.fillRect(frame);
            context.restoreGraphicsState();
            */
        }
    }

    static Canvas createGLCanvas(Shell shell, int bits, Map caps) {
        GLData data = new GLData();
        //long context = 0, shareContext = 0;
        if (caps != null) {
            Long shareContextPtr = (Long) caps.get("shareContextPtr");
            if (shareContextPtr != null) {
                shareContext = shareContextPtr.longValue();
                //data.shareContext2 = shareContextPtr.longValue();
                //setHandle(GLData.class, data, "shareContext2", shareContextPtr.longValue());
            }
            Long contextPtr = (Long) caps.get("contextPtr");
            if (contextPtr != null) {
                context = contextPtr.longValue();
                //data.context = contextPtr.longValue();
                //setHandle(GLData.class, data, "context", contextPtr.longValue());
            }
        }
        data.doubleBuffer = true;
        GLCanvas canvas = new GLCanvas(shell, bits, data);
        //System.out.println(context + " " + shareContext);
        if (context != 0) {
            //TODO - the original GL context created by the canvas is leaked 
            //TODO - the Prism context is disposed when canvas is disposed
            final String GLCONTEXT_KEY = "org.eclipse.swt.internal.cocoa.glcontext";
            canvas.setData(GLCONTEXT_KEY, new_NSOpenGLContext(context));
        }
        return canvas;
    }
    
    static Image createImage(Pixels pixels) {
        if (pixels == null) return null;
        ImageData data = createImageData(pixels);
        return new Image(Display.getDefault(), data);
    }

    static ImageData createImageData(Pixels pixels) {
        if (pixels == null) return null;
        int width = pixels.getWidth();
        int height = pixels.getHeight();
        int bpr = width * 4;
        int dataSize = bpr * height;
        byte[] buffer = new byte[dataSize];
        byte[] alphaData = new byte[width * height];
        if (pixels.getBytesPerComponent() == 1) {
            // ByteBgraPre
            ByteBuffer pixbuf = (ByteBuffer) pixels.getPixels();
            for (int y = 0, offset = 0, alphaOffset = 0; y < height; y++) {
                for (int x = 0; x < width; x++, offset += 4) {
                    byte b = pixbuf.get();
                    byte g = pixbuf.get();
                    byte r = pixbuf.get();
                    byte a = pixbuf.get();
                    // non premultiplied ?
                    alphaData[alphaOffset++] = a;
                    buffer[offset] = b;
                    buffer[offset + 1] = g;
                    buffer[offset + 2] = r;
                    buffer[offset + 3] = 0;// alpha
                }
            }
        } else if (pixels.getBytesPerComponent() == 4) {
            // IntArgbPre
            IntBuffer pixbuf = (IntBuffer) pixels.getPixels();
            for (int y = 0, offset = 0, alphaOffset = 0; y < height; y++) {
                for (int x = 0; x < width; x++, offset += 4) {
                    int pixel = pixbuf.get();
                    byte b = (byte) (pixel & 0xFF);
                    byte g = (byte) ((pixel >> 8) & 0xFF);
                    byte r = (byte) ((pixel >> 16) & 0xFF);
                    byte a = (byte) ((pixel >> 24) & 0xFF);
                    // non premultiplied ?
                    alphaData[alphaOffset++] = a;
                    buffer[offset] = b;
                    buffer[offset + 1] = g;
                    buffer[offset + 2] = r;
                    buffer[offset + 3] = 0;// alpha
                }
            }
        } else {
            throw new IllegalArgumentException("unhandled pixel buffer");
        }
        PaletteData palette = new PaletteData(0xFF00, 0xFF0000, 0xFF000000);
        ImageData imageData = new ImageData(width, height, 32, palette, 4, buffer);
        imageData.alphaData = alphaData;
        return imageData;
    }

    //TODO - implement conversion from ImageData to Pixels
    static Pixels createPixels(ImageData data) {
        if (data == null) return null;
//        ImageData imageData = (ImageData) data;
//        int width = imageData.width, height = imageData.height;
//        int [] pixels = new int [width * height];
//        imageData.getPixels(0, 0, width * height, pixels, 0);
//        IntBuffer buffer = IntBuffer.wrap(pixels);
//        return new SWTPixels(width, height, buffer);
        return null;
    }
}
