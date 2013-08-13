/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.pgstub;

import java.security.AccessControlContext;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.TKStageListener;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.providers.AccessibleStageProvider;

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
    public TKScene createTKScene(boolean depthBuffer, boolean antiAliasing, AccessControlContext acc) {
        return new StubScene();
    }

    @Override
    public void setScene(TKScene scene) {
        if (scene != null) {
            StubScene s = (StubScene) scene;
            s.stage = this;
            notificationSender.setScene(s);
            if (width != -1 && height != -1)
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

    public boolean visible;
    public float opacity;

    @Override
    public void setBounds(float x, float y, boolean xSet, boolean ySet,
                          float width, float height,
                          float contentWidth, float contentHeight,
                          float xGravity, float yGravity)
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
    }

    // Just a helper method
    public void setSize(float w, float h) {
        setBounds(0, 0, false, false, w, h, 0, 0, 0, 0);
    }

    // Just a helper method
    public void setLocation(float x, float y) {
        setBounds(x, y, true, true, 0, 0, 0, 0, 0, 0);
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

    @Override
    public boolean grabFocus() {
        return false;
    }

    @Override
    public void ungrabFocus() {
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

    /**
     *
     * Accessibility glue for native
     *
     */

    /**
     * Initialize Accessiblility
     *
     * @param ac    the Glass accessible root object.
     */
    @Override public void setAccessibilityInitIsComplete(Object ac) {
        // TODO: Add code later
    }

    /**
     * Create accessible Glass object corresponding to stage
     *
     * @param ac    the FX accessible root/stage node.
     *
     * @return the Glass AccessibleRoot object.
     */
    @Override
    public Object accessibleCreateStageProvider(AccessibleStageProvider ac) {
        // TODO: Add code later
        return null ;
    }

    /**
     * Create accessible native object corresponding to controls
     *
     * @param ac
     * returns native Object
     */
    @Override public Object accessibleCreateBasicProvider(AccessibleProvider ac) {
        // TODO: Add code later
        return null;
    }

    /**
     * Delete accessible native object corresponding to controls
     *
     * @param nativeAcc
     * returns native Object
     */
    @Override public void accessibleDestroyBasicProvider(Object nativeAcc) {
        // TODO: Add code later
    }

    /**
     * Fire accessible event
     *
     * @param eventID   identifies the event.
     */
    @Override public void accessibleFireEvent(Object nativeAcc, int eventID) {
        // TODO: Add code later
    }

    /** Fire accessible property change event
     *
     * @param propertyId    identifies the property
     * @param oldProperty   the old value of the property
     * @param newProperty   the new value of the property
     */
    @Override public void accessibleFirePropertyChange(Object nativeAcc, int propertyId, int oldProperty,
                                             int newProperty ) {
        // TODO: Add code later
    }

    @Override public void accessibleFirePropertyChange(Object nativeAcc, int propertyId, boolean oldProperty,
                                             boolean newProperty ) {
        // TODO: Add code later
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
            process(new Notification() {
                        @Override
                        public void execute(final TKStageListener listener) {
                            listener.changedLocation(x, y);
                        }
                    });
        }

        @Override
        public void changedSize(final float width, final float height) {
            process(new Notification() {
                        @Override
                        public void execute(final TKStageListener listener) {
                            listener.changedSize(width, height);
                            if (scene != null && width != -1 && height != -1) {
                                scene.getListener().changedSize(width, height);
                            }
                        }
                    });
        }

        @Override
        public void changedFocused(final boolean focused,
                                   final FocusCause cause) {
            process(new Notification() {
                        @Override
                        public void execute(final TKStageListener listener) {
                            listener.changedFocused(focused, cause);
                        }
                    });
        }

        @Override
        public void changedIconified(final boolean iconified) {
            process(new Notification() {
                        @Override
                        public void execute(final TKStageListener listener) {
                            listener.changedIconified(iconified);
                        }
                    });
        }

        @Override
        public void changedMaximized(final boolean maximized) {
            process(new Notification() {
                        @Override
                        public void execute(final TKStageListener listener) {
                            listener.changedMaximized(maximized);
                        }
                    });
        }


        @Override
        public void changedResizable(final boolean resizable) {
            process(new Notification() {
                        @Override
                        public void execute(final TKStageListener listener) {
                            listener.changedResizable(resizable);
                        }
                    });
        }

        @Override
        public void changedFullscreen(final boolean fs) {
            process(new Notification() {
                        @Override
                        public void execute(final TKStageListener listener) {
                            listener.changedFullscreen(fs);
                        }
                    });
        }

        @Override
        public void closing() {
            process(new Notification() {
                        @Override
                        public void execute(final TKStageListener listener) {
                            listener.closing();
                        }
                    });
        }

        @Override
        public void closed() {
            process(new Notification() {
                        @Override
                        public void execute(final TKStageListener listener) {
                            listener.closed();
                        }
                    });
        }

        @Override
        public void focusUngrab() {
            process(new Notification() {
                        @Override
                        public void execute(final TKStageListener listener) {
                            listener.focusUngrab();
                        }
                    });
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

    }

    public void setRTL(boolean b) {
    }
}
