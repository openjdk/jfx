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

}
