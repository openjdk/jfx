/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.pgstub;

import java.security.AccessControlContext;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.TKStageListener;

/**
 * @author Richard Bair
 */
public class StubStage implements TKStage {

    private NotificationSender notificationSender = new NotificationSender();

    @Override
    public void setTKStageListener(TKStageListener listener) {
        notificationSender.setListener(listener);
    }

    @Override
    public TKScene createTKScene(boolean depthBuffer, boolean msaa, @SuppressWarnings("removal") AccessControlContext acc) {
        return new StubScene();
    }

    @Override
    public void setScene(TKScene scene) {
        if (scene != null) {
            StubScene s = (StubScene) scene;
            s.stage = this;
            notificationSender.setScene(s);
            if (visible && width != -1 && height != -1)
                s.getListener().changedSize(width, height);
        }
    }

    public int numTimesSetSizeAndLocation;

    // Platform place/resize the window with some
    // "default" value. Pretending that the values
    // below are those platform defaults.
    public float x = 16;
    public float y = 12;
    public float width = 256;
    public float height = 192;
    public float renderScaleX = 1.0f;
    public float renderScaleY = 1.0f;

    public boolean visible;
    public float opacity;

    @Override
    public void setBounds(float x, float y, boolean xSet, boolean ySet,
                          float width, float height,
                          float contentWidth, float contentHeight,
                          float xGravity, float yGravity,
                          float renderScaleX, float renderScaleY)
    {
        numTimesSetSizeAndLocation++;

        boolean locationChanged = false;

        if (xSet && (this.x != x)) {
            this.x = x;
            locationChanged = true;
        }

        if (ySet && (this.y != y)) {
            this.y = y;
            locationChanged = true;
        }

        if (locationChanged) {
            notificationSender.changedLocation(x, y);
        }

        boolean sizeChanged = false;

        if (width > 0) {
            if (this.width != width) {
                this.width = width;
                sizeChanged = true;
            }
        } else if (contentWidth > 0) {
            if (this.width != contentWidth) {
                this.width = contentWidth;
                sizeChanged = true;
            }
        }

        if (height > 0) {
            if (this.height != height) {
                this.height = height;
                sizeChanged = true;
            }
        } else if (contentHeight > 0) {
            if (this.height != contentHeight) {
                this.height = contentHeight;
                sizeChanged = true;
            }
        }

        if (sizeChanged) {
            notificationSender.changedSize(width, height);
        }
        if (renderScaleX > 0.0) this.renderScaleX = renderScaleX;
        if (renderScaleY > 0.0) this.renderScaleY = renderScaleY;
    }

    @Override
    public float getPlatformScaleX() {
        return 1.0f;
    }

    @Override
    public float getPlatformScaleY() {
        return 1.0f;
    }

    @Override
    public float getOutputScaleX() {
        return 1.0f;
    }

    @Override
    public float getOutputScaleY() {
        return 1.0f;
    }

    // Just a helper method
    public void setSize(float w, float h) {
        setBounds(0, 0, false, false, w, h, 0, 0, 0, 0, 0, 0);
    }

    // Just a helper method
    public void setLocation(float x, float y) {
        setBounds(x, y, true, true, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public void setIcons(List icons) {
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;

        if (!visible) {
            notificationSender.changedFocused(false, FocusCause.DEACTIVATED);
        }

        notificationSender.changedLocation(x, y);
        notificationSender.changedSize(width, height);
    }

    @Override
    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    @Override
    public void setIconified(boolean iconified) {
        notificationSender.changedIconified(iconified);
    }

    @Override
    public void setMaximized(boolean maximized) {
        notificationSender.changedMaximized(maximized);
    }

    @Override
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        notificationSender.changedAlwaysOnTop(alwaysOnTop);
    }

    @Override
    public void setResizable(boolean resizable) {
        notificationSender.changedResizable(resizable);
    }

    @Override
    public void setImportant(boolean important) {
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
        notificationSender.changedFullscreen(fullScreen);
    }

    @Override
    public void requestFocus() {
        notificationSender.changedFocused(true, FocusCause.ACTIVATED);
    }

    @Override
    public void requestFocus(FocusCause cause) {
        notificationSender.changedFocused(true, cause);
    }

    @Override
    public void toBack() {
    }

    @Override
    public void toFront() {
    }

    @Override
    public void close() {
    }

    private boolean focusGrabbed;

    @Override
    public boolean grabFocus() {
        focusGrabbed = true;
        return true;
    }

    @Override
    public void ungrabFocus() {
        focusGrabbed = false;
    }

    public boolean isFocusGrabbed() {
        return focusGrabbed;
    }

    @Override
    public void setMinimumSize(int minWidth, int minHeight) {
    }

    @Override
    public void setMaximumSize(int maxWidth, int maxHeight) {
    }

    public void holdNotifications() {
        notificationSender.holdNotifications();
    }

    public void releaseNotifications() {
        notificationSender.releaseNotifications();
    }

    public void releaseSingleNotification() {
        notificationSender.releaseSingleNotification();
    }

    protected final TKStageListener getNotificationSender() {
        return notificationSender;
    }

    @Override
    public void requestInput(String text, int type, double width, double height,
                                double Mxx, double Mxy, double Mxz, double Mxt,
                                double Myx, double Myy, double Myz, double Myt,
                                double Mzx, double Mzy, double Mzz, double Mzt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void releaseInput() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private interface Notification {
        void execute(TKStageListener listener);
    }

    private static final class NotificationSender implements TKStageListener {
        private final Queue<Notification> queue =
                new LinkedList<Notification>();

        private boolean hold;
        private TKStageListener listener;
        private StubScene scene;

        public void setListener(final TKStageListener listener) {
            this.listener = listener;
        }

        public void setScene(final StubScene scene) {
            this.scene = scene;
        }

        public void holdNotifications() {
            hold = true;
        }

        public void releaseNotifications() {
            hold = false;
            flush();
        }

        private void releaseSingleNotification() {
            queue.poll().execute(listener);
        }

        @Override
        public void changedLocation(final float x, final float y) {
            process(listener1 -> listener1.changedLocation(x, y));
        }

        @Override
        public void changedSize(final float width, final float height) {
            process(listener1 -> {
                listener1.changedSize(width, height);
                if (scene != null && width != -1 && height != -1) {
                    scene.getListener().changedSize(width, height);
                }
            });
        }

        @Override
        public void changedScale(float xScale, float yScale) {
            process(listener1 -> listener1.changedScale(xScale, yScale));
        }

        @Override
        public void changedFocused(final boolean focused,
                                   final FocusCause cause) {
            process(listener1 -> listener1.changedFocused(focused, cause));
        }

        @Override
        public void changedIconified(final boolean iconified) {
            process(listener1 -> listener1.changedIconified(iconified));
        }

        @Override
        public void changedMaximized(final boolean maximized) {
            process(listener1 -> listener1.changedMaximized(maximized));
        }

        @Override
        public void changedAlwaysOnTop(boolean alwaysOnTop) {
            process(listener1 -> listener1.changedAlwaysOnTop(alwaysOnTop));
        }


        @Override
        public void changedResizable(final boolean resizable) {
            process(listener1 -> listener1.changedResizable(resizable));
        }

        @Override
        public void changedFullscreen(final boolean fs) {
            process(listener1 -> listener1.changedFullscreen(fs));
        }

        @Override
        public void closing() {
            process(listener1 -> listener1.closing());
        }

        @Override
        public void closed() {
            process(listener1 -> listener1.closed());
        }

        @Override
        public void focusUngrab() {
            process(listener1 -> listener1.focusUngrab());
        }

        private void process(final Notification notification) {
            if (hold) {
                queue.offer(notification);
                return;
            }

            if (listener != null) {
                notification.execute(listener);
            }
        }

        private void flush() {
            if (listener == null) {
                queue.clear();
                return;
            }

            Notification nextNotification = queue.poll();
            while (nextNotification != null) {
                nextNotification.execute(listener);
                nextNotification = queue.poll();
            }
        }

        /**
        * Initialize accessibility
        */
        public void initAccessibleTKStageListener() {
            // TODO: Add code later
        }

        @Override
        public void changedScreen(Object from, Object to) {
            // TODO: Add code later
        }

    }

    @Override
    public void setRTL(boolean b) {
    }

    @Override
    public void setEnabled(boolean b) {
    }

    @Override
    public long getRawHandle() {
        return 0L;
    }
}
