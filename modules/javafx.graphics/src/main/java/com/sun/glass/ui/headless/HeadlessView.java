package com.sun.glass.ui.headless;

import com.sun.glass.events.ViewEvent;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import java.util.Map;

public class HeadlessView extends View {

    private Map capabilities;
    private int x = 0;
    private int y = 0;
    private long parentPtr = 0;
    private Pixels pixels;

    private boolean imeEnabled;

    @Override
    protected void _enableInputMethodEvents(long ptr, boolean enable) {
        this.imeEnabled = enable;
    }

    @Override
    protected long _create(Map capabilities) {
        this.capabilities = capabilities;
        return 1;
    }

    @Override
    protected long _getNativeView(long ptr) {
        return ptr;
    }

    @Override
    protected int _getX(long ptr) {
        return x;
    }

    @Override
    protected int _getY(long ptr) {
        return y;
    }

    @Override
    protected void _setParent(long ptr, long parentPtr) {
        this.parentPtr = parentPtr;
    }

    @Override
    protected boolean _close(long ptr) {
        return true;
    }

    @Override
    protected void _scheduleRepaint(long ptr) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void _begin(long ptr) {
    }

    @Override
    protected void _end(long ptr) {
    }

    @Override
    protected int _getNativeFrameBuffer(long ptr) {
        return 0;
    }

    @Override
    protected void _uploadPixels(long ptr, Pixels pixels) {
        HeadlessWindow window = (HeadlessWindow)this.getWindow();
        this.pixels = pixels;
        window.blit(pixels);
    }

    @Override
    protected boolean _enterFullscreen(long ptr, boolean animate, boolean keepRatio, boolean hideCursor) {
        HeadlessWindow window = (HeadlessWindow)this.getWindow();
        window.setFullscreen(true);
        notifyView(ViewEvent.FULLSCREEN_ENTER);
        return true;
    }

    @Override
    protected void _exitFullscreen(long ptr, boolean animate) {
        HeadlessWindow window = (HeadlessWindow)this.getWindow();
        if (window != null) {
            window.setFullscreen(false);
        }
        notifyView(ViewEvent.FULLSCREEN_EXIT);
    }

    @Override
    protected void notifyResize(int width, int height) {
        super.notifyResize(width, height);
    }

    @Override
    protected void notifyMouse(int type, int button, int x, int y, int xAbs, int yAbs, int modifiers, boolean isPopupTrigger, boolean isSynthesized) {
        super.notifyMouse(type, button, x, y, xAbs, yAbs, modifiers, isPopupTrigger, isSynthesized);
    }

    @Override
    protected void notifyMenu(int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger) {
        super.notifyMenu(x, y, xAbs, yAbs, isKeyboardTrigger);
    }

    @Override
    protected void notifyScroll(int x, int y, int xAbs, int yAbs, double deltaX, double deltaY, int modifiers, int lines, int chars, int defaultLines, int defaultChars, double xMultiplier, double yMultiplier) {
        super.notifyScroll(x, y, xAbs, yAbs, deltaX, deltaY, modifiers, lines, chars, defaultLines, defaultChars, xMultiplier, yMultiplier);
    }

    @Override
    protected void notifyKey(int type, int keyCode, char[] keyChars, int modifiers) {
        super.notifyKey(type, keyCode, keyChars, modifiers);
    }

    @Override
    protected void notifyRepaint(int x, int y, int width, int height) {
        super.notifyRepaint(x, y, width, height);
    }

}
