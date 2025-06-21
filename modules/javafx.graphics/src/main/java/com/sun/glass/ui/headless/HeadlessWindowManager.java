package com.sun.glass.ui.headless;

import com.sun.glass.ui.Window;
import java.util.List;

public class HeadlessWindowManager {

    void repaintAll() {
        List<Window> windows = Window.getWindows().stream()
                .filter(win -> win.getView() != null)
                .filter(win -> !win.isClosed())
                .filter(win -> win.isVisible()).toList();
        for (Window win : windows) {
            if (win.isVisible() && (!win.isMinimized())) {
                HeadlessWindow hw = (HeadlessWindow) win;
                HeadlessView hv = (HeadlessView) hw.getView();
                hv.notifyRepaint(hw.getX(), hw.getY(), hw.getWidth(), hw.getHeight());
            }
        }
    }

    private HeadlessWindow getFocusedWindow() {
        List<Window> windows = Window.getWindows().stream()
                .filter(win -> win.getView()!= null)
                .filter(win -> !win.isClosed())
                .filter(win -> win.isFocused()).toList();
        if (windows.isEmpty()) return null;
        if (windows.size() == 1) return (HeadlessWindow)windows.get(0);
        return (HeadlessWindow)windows.get(windows.size() -1);
    }
}
