package com.sun.glass.ui.headless;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.CommonDialogs;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Size;
import com.sun.glass.ui.Timer;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class HeadlessApplication extends Application {

    private final NestedRunnableProcessor processor = new NestedRunnableProcessor();
    private final HeadlessWindowManager windowManager = new HeadlessWindowManager();
    private Screen[] screens;
    private HeadlessRobot activeRobot = null;
    ByteBuffer frameBuffer;

    private static final int MULTICLICK_MAX_X = 20;
    private static final int MULTICLICK_MAX_Y = 20;
    private static final long MULTICLICK_TIME = 500;

    @Override
    protected void runLoop(Runnable launchable) {
        processor.invokeLater(launchable);
        Thread eventThread = new Thread(processor);
        setEventThread(eventThread);
        eventThread.start();
    }

    @Override
    protected void _invokeAndWait(Runnable runnable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void _invokeLater(Runnable runnable) {
        processor.invokeLater(runnable);
    }

    @Override
    protected Object _enterNestedEventLoop() {
        return processor.newRunLoop();
    }

    @Override
    protected void _leaveNestedEventLoop(Object retValue) {
        processor.leaveCurrentLoop(retValue);
    }

    @Override
    protected int _isKeyLocked(int keyCode) {
        return KeyEvent.KEY_LOCK_OFF;
    }

    @Override
    public Window createWindow(Window owner, Screen screen, int styleMask) {
        HeadlessWindow window = new HeadlessWindow(windowManager, owner, screen, frameBuffer, styleMask);
        if (this.activeRobot != null) {
            activeRobot.windowAdded(window);
            window.setRobot(this.activeRobot);
        }
        return window;
    }

    @Override
    public View createView() {
        return new HeadlessView();
    }

    @Override
    public Cursor createCursor(int type) {
        return new HeadlessCursor(type);
    }

    @Override
    public Cursor createCursor(int x, int y, Pixels pixels) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void staticCursor_setVisible(boolean visible) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Size staticCursor_getBestSize(int width, int height) {
        return new Size(16, 16);
    }

    @Override
    public Pixels createPixels(int width, int height, ByteBuffer data) {
        return new HeadlessPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, ByteBuffer data, float scalex, float scaley) {
        return new HeadlessPixels(width, height, data, scalex, scaley);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data) {
        return new HeadlessPixels(width, height, data);
    }

    @Override
    public Pixels createPixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        return new HeadlessPixels(width, height, data, scalex, scaley);
    }

    @Override
    protected int staticPixels_getNativeFormat() {
        return Pixels.Format.BYTE_BGRA_PRE;
    }

    @Override
    public GlassRobot createRobot() {
        this.activeRobot = new HeadlessRobot(this);
        return this.activeRobot;
    }

    @Override
    protected double staticScreen_getVideoRefreshPeriod() {
        return 0.;
    }

    @Override
    protected Screen[] staticScreen_getScreens() {
        final int screenWidth = 1000;
        final int screenHeight = 1000;
        if (this.screens == null) {
            float scaleX = 1.f;
            float scaleY = 1.f;
            Screen screen = new Screen(0, 32, 0, 0, screenWidth, screenHeight, 0, 0, screenWidth, screenHeight, 0, 0, screenWidth, screenHeight, 100, 100, 1f, 1f, scaleX, scaleY);
            this.screens = new Screen[1];
            this.screens[0] = screen;
            this.frameBuffer = ByteBuffer.allocate(screen.getWidth() * screen.getHeight() * 4);
        }
        return this.screens;
    }

    @Override
    public Timer createTimer(Runnable runnable) {
        return new HeadlessTimer(runnable);
    }

    @Override
    protected int staticTimer_getMinPeriod() {
        return 0;
    }

    @Override
    protected int staticTimer_getMaxPeriod() {
        return 1_000_000;
    }

    @Override
    protected CommonDialogs.FileChooserResult staticCommonDialogs_showFileChooser(Window owner, String folder, String filename, String title, int type,
            boolean multipleMode, CommonDialogs.ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected File staticCommonDialogs_showFolderChooser(Window owner, String folder, String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected long staticView_getMultiClickTime() {
        return MULTICLICK_TIME;
    }

    @Override
    protected int staticView_getMultiClickMaxX() {
        return MULTICLICK_MAX_X;
    }

    @Override
    protected int staticView_getMultiClickMaxY() {
        return MULTICLICK_MAX_Y;
    }

    @Override
    protected boolean _supportsTransparentWindows() {
        return false;
    }

    @Override
    protected boolean _supportsUnifiedWindows() {
        return false;
    }

    @Override
    protected boolean _supportsExtendedWindows() {
        return false;
    }

    @Override
    protected int _getKeyCodeForChar(char c, int hint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
