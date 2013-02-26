/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicReference;

import javafx.stage.Modality;
import javafx.stage.StageStyle;

import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.*;
import com.sun.glass.ui.accessible.AccessibleBaseProvider;
import com.sun.glass.ui.accessible.AccessibleRoot;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.iio.common.PushbroomScaler;
import com.sun.javafx.iio.common.ScalerFactory;
import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKStage;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.providers.AccessibleStageProvider;

class WindowStage extends GlassStage {

    PushbroomScaler scaler;
    StageStyle style;
    Window platformWindow;
    MenuBar menubar;
    String title;

    int minWidth = 0;
    int minHeight = 0;
    int maxWidth = Integer.MAX_VALUE;
    int maxHeight = Integer.MAX_VALUE;

    private OverlayWarning warning = null;
    private boolean rtl = false;
    private boolean transparent = false;
    private boolean isPrimaryStage = false;
    private boolean isAppletStage = false; // true if this is an embedded applet window
    private boolean isInFullScreen = false;
    // Owner of this window
    private TKStage owner = null;
    // Owner's window
    private Window ownerWindow = null;
    private Modality modality = Modality.NONE;

    // A flag to indicate whether a call was generated from
    // an input event handler.
    private boolean inEventHandler = false;

    private static final QuantumRenderer renderer  = QuantumRenderer.getInstance();
    
    private static final AtomicReference<WindowStage> activeFSWindowReference =
                            new AtomicReference<WindowStage>();

    private static GlassAppletWindow appletWindow = null;
    static void setAppletWindow(GlassAppletWindow aw) {
        appletWindow = aw;
    }
    static GlassAppletWindow getAppletWindow() {
        return appletWindow;
    }

    public WindowStage(final boolean verbose) {
        this(verbose, StageStyle.DECORATED, false, Modality.NONE, null);
    }

    public WindowStage(final boolean verbose, final StageStyle stageStyle) {
        this(verbose, stageStyle, false, Modality.NONE, null);
    }

    // Called by QuantumToolkit, so we can override initPlatformWindow in subclasses
    public final WindowStage init(GlassSystemMenu sysmenu) {
        initPlatformWindow();
        platformWindow.setEventHandler(new GlassWindowEventHandler(this));
        platformWindow.setMinimumSize(minWidth, minHeight);
        platformWindow.setMaximumSize(maxWidth, maxHeight);
        if (sysmenu.isSupported()) {
            sysmenu.createMenuBar();
            platformWindow.setMenuBar(sysmenu.getMenuBar());
        }
        return this;
    }

    public WindowStage(final boolean verbose, final StageStyle stageStyle,
            final boolean isPrimary, Modality modality, TKStage owner) {
        super(verbose);

        transparent = stageStyle == StageStyle.TRANSPARENT;

        this.style = stageStyle;
        this.isPrimaryStage = isPrimary;
        if (null != appletWindow && isPrimary) {
            // this is an embedded applet stage
            isAppletStage = true;
        }
        if (owner == null) {
            if (modality == Modality.WINDOW_MODAL) {
                modality = Modality.NONE;
            }
        } else {
            if (owner instanceof WindowStage) {
                ownerWindow = ((WindowStage) owner).platformWindow;
            } else {
                // We don't expect this case to happen.
                System.err.println("Error: Unsupported type of owner " + owner);
            }
        }
        this.owner = owner;
        this.modality = modality;
    }

    protected void initPlatformWindow() {
        int windowMask = rtl ? Window.RIGHT_TO_LEFT : 0;

        Application app = Application.GetApplication();
        if (isPrimaryStage && (null != appletWindow)) {
            platformWindow = app.createWindow(appletWindow.getGlassWindow().getNativeWindow());
        } else if (style == StageStyle.DECORATED || style == StageStyle.UNIFIED) {
            windowMask |= Window.TITLED | Window.CLOSABLE | Window.MINIMIZABLE |
                    Window.MAXIMIZABLE;
            if (style == StageStyle.UNIFIED && app.supportsUnifiedWindows()) {
                windowMask |= Window.UNIFIED;
            }
            platformWindow = 
                    app.createWindow(ownerWindow, Screen.getMainScreen(), windowMask);
            platformWindow.setResizable(true);
        } else if (style == StageStyle.UTILITY) {
            windowMask |=  Window.TITLED | Window.UTILITY | Window.CLOSABLE;
            platformWindow = 
                    app.createWindow(ownerWindow, Screen.getMainScreen(), windowMask);
        } else {
            windowMask |= (transparent ? Window.TRANSPARENT : Window.UNTITLED) |
                    Window.CLOSABLE;
            platformWindow = 
                    app.createWindow(ownerWindow, Screen.getMainScreen(), windowMask);
        }
    }

    protected Window getPlatformWindow() {
        return platformWindow;
    }

    protected TKStage getOwner() {
        return owner;
    }
    
    protected ViewScene getViewScene() {
        return (ViewScene)scene;
    }
    
    StageStyle getStyle() {
        return style;
    }

    @Override public TKScene createTKScene(boolean depthBuffer) {
        return new ViewScene(verbose, depthBuffer);
    }
    
    /**
     * Set the scene to be displayed in this stage
     *
     * @param scene The peer of the scene to be displayed
     */
    @Override public void setScene(TKScene scene) {
        GlassScene oldScene = this.scene;
        super.setScene(scene);
        if (this.scene != null) {
            GlassScene newScene = getViewScene();
            View view = newScene.getPlatformView();
            AbstractPainter.renderLock.lock();
            try {
                platformWindow.setView(view);
                if (oldScene != null) oldScene.updateViewState();
                newScene.updateViewState();
            } finally {
                AbstractPainter.renderLock.unlock();
            }
            requestFocus();
            applyFullScreen();
        } else {
            AbstractPainter.renderLock.lock();
            try {
                platformWindow.setView(null);
                if (oldScene != null) oldScene.updateViewState();
            } finally {
                AbstractPainter.renderLock.unlock();
            }
        }
        if (oldScene != null) {
            PrismPen  pen       = ((ViewScene)oldScene).getPen();
            ViewPainter painter = pen.getPainter();
            
            renderer.disposePresentable(painter.presentable);   // latched on RT
        }
    }
    
    @Override public void setBounds(float x, float y, boolean xSet, boolean ySet,
                                    float w, float h, float cw, float ch,
                                    float xGravity, float yGravity)
    {
        if (isAppletStage) {
            xSet = ySet = false;
        }
        platformWindow.setBounds((int)x, (int)y, xSet, ySet, 
                                 (int)w, (int)h, (int)cw, (int)ch, 
                                 xGravity, yGravity);
    }

    @Override public void setMinimumSize(int minWidth, int minHeight) {
        if (this.minWidth != minWidth || this.minHeight != minHeight) {
            this.minWidth = minWidth;
            this.minHeight = minHeight;
            if (platformWindow != null) {
                platformWindow.setMinimumSize(minWidth, minHeight);
            }
        }
    }

    @Override public void setMaximumSize(int maxWidth, int maxHeight) {
        if (this.maxWidth != maxWidth || this.maxHeight != maxHeight) {
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            if (platformWindow != null) {
                platformWindow.setMaximumSize(maxWidth, maxHeight);
            }
        }
    }

    @Override public void setIcons(java.util.List icons) {

        int SMALL_ICON_HEIGHT = 32;
        int SMALL_ICON_WIDTH = 32;
        if (PlatformUtil.isMac()) { //Mac Sized Icons
            SMALL_ICON_HEIGHT = 128;
            SMALL_ICON_WIDTH = 128;
        } else if (PlatformUtil.isWindows()) { //Windows Sized Icons
            SMALL_ICON_HEIGHT = 32;
            SMALL_ICON_WIDTH = 32;
        } else if (PlatformUtil.isLinux()) { //Linux icons
            SMALL_ICON_HEIGHT = 128;
            SMALL_ICON_WIDTH = 128;
        }

        if (icons == null || icons.size() < 1) { //no icons passed in
            return;
        }

        int width = platformWindow.getWidth();
        int height = platformWindow.getHeight();

        Image image = null;
        double bestSimilarity = 3; //Impossibly high value
        for (int i = 0; i < icons.size(); i++) {
            //Iterate imageList looking for best matching image.
            //'Similarity' measure is defined as good scale factor and small insets.
            //best possible similarity is 0 (no scale, no insets).
            //It's found by experimentation that good-looking results are achieved
            //with scale factors x1, x3/4, x2/3, xN, x1/N.
            //Check to make sure the image/image format is correct.
            Image im = (Image)icons.get(i);
            if (im == null || !(im.getPixelFormat() == PixelFormat.BYTE_RGB ||
                im.getPixelFormat() == PixelFormat.BYTE_BGRA_PRE ||
                im.getPixelFormat() == PixelFormat.BYTE_GRAY))
            {
                continue;
            }

            int iw = im.getWidth();
            int ih = im.getHeight();

            if (iw > 0 && ih > 0) {
                //Calc scale factor
                double scaleFactor = Math.min((double)SMALL_ICON_WIDTH / (double)iw,
                                              (double)SMALL_ICON_HEIGHT / (double)ih);
                //Calculate scaled image dimensions
                //adjusting scale factor to nearest "good" value
                int adjw;
                int adjh;
                double scaleMeasure = 1; //0 - best (no) scale, 1 - impossibly bad
                if (scaleFactor >= 2) {
                    //Need to enlarge image more than twice
                    //Round down scale factor to multiply by integer value
                    scaleFactor = Math.floor(scaleFactor);
                    adjw = iw * (int)scaleFactor;
                    adjh = ih * (int)scaleFactor;
                    scaleMeasure = 1.0 - 0.5 / scaleFactor;
                } else if (scaleFactor >= 1) {
                    //Don't scale
                    scaleFactor = 1.0;
                    adjw = iw;
                    adjh = ih;
                    scaleMeasure = 0;
                } else if (scaleFactor >= 0.75) {
                    //Multiply by 3/4
                    scaleFactor = 0.75;
                    adjw = iw * 3 / 4;
                    adjh = ih * 3 / 4;
                    scaleMeasure = 0.3;
                } else if (scaleFactor >= 0.6666) {
                    //Multiply by 2/3
                    scaleFactor = 0.6666;
                    adjw = iw * 2 / 3;
                    adjh = ih * 2 / 3;
                    scaleMeasure = 0.33;
                } else {
                    //Multiply size by 1/scaleDivider
                    //where scaleDivider is minimum possible integer
                    //larger than 1/scaleFactor
                    double scaleDivider = Math.ceil(1.0 / scaleFactor);
                    scaleFactor = 1.0 / scaleDivider;
                    adjw = (int)Math.round((double)iw / scaleDivider);
                    adjh = (int)Math.round((double)ih / scaleDivider);
                    scaleMeasure = 1.0 - 1.0 / scaleDivider;
                }
                double similarity = ((double)width - (double)adjw) / (double)width +
                    ((double)height - (double)adjh) / (double)height + //Large padding is bad
                    scaleMeasure; //Large rescale is bad
                if (similarity < bestSimilarity) {
                    image = im;
                }
                if (similarity == 0) break;
            }
        }

        if (image == null) {
            //No images were found, possibly all are broken
            return;
        }

        scaler = ScalerFactory.createScaler(image.getWidth(), image.getHeight(),
                                            image.getBytesPerPixelUnit(),
                                            SMALL_ICON_WIDTH, SMALL_ICON_HEIGHT, true);

        //shrink the image and convert the format to INT_ARGB_PRE
        ByteBuffer buf = (ByteBuffer) image.getPixelBuffer();
        byte bytes[] = new byte[buf.limit()];

        int iheight = image.getHeight();

        //Iterate through each scanline of the image
        //and pass it one at a time to the scaling object
        for (int z = 0; z < iheight; z++) {
            buf.position(z*image.getScanlineStride());
            buf.get(bytes, 0, image.getScanlineStride());
            if (this.scaler != null) {
                scaler.putSourceScanline(bytes, 0);
            }
        }

        buf.rewind();

        final Image img = image.iconify(scaler.getDestination(), SMALL_ICON_WIDTH, SMALL_ICON_HEIGHT);
        platformWindow.setIcon(PixelUtils.imageToPixels(img));
    }

    @Override public void setTitle(String title) {
        if (platformWindow == null) {
            this.title = title;
        } else {
            platformWindow.setTitle(title);
        }
    }

    @Override public void setVisible(final boolean visible) {
        // Before setting visible to false on the native window, we unblock
        // other windows.
        if (!visible) {
            if (modality == Modality.WINDOW_MODAL) {
                assert (owner != null);
                ((WindowStage) owner).setEnabled(true);
            } else if (modality == Modality.APPLICATION_MODAL) {
                windowsSetEnabled(true);
            } else {
                // Note: This method is required to workaround a glass issue
                // mentioned in RT-12607
                if (owner != null) {
                    WindowStage ownerStage = (WindowStage)owner;
                    ownerStage.requestToFront();
                }
            }
        }
        try {
            AbstractPainter.renderLock.lock();
            platformWindow.setVisible(visible);
            super.setVisible(visible);
        } finally {
            AbstractPainter.renderLock.unlock();
        }
        // After setting visible to true on the native window, we block
        // other windows.
        if (visible) {
            if (modality == Modality.WINDOW_MODAL) {
                assert (owner != null);
                ((WindowStage) owner).setEnabled(false);
            } else if (modality == Modality.APPLICATION_MODAL) {
                windowsSetEnabled(false);
            }
            if (isAppletStage && null != appletWindow) {
                appletWindow.assertStageOrder();
            }
        }
        
        applyFullScreen();
    }
    
    @Override boolean isVisible() {
        return platformWindow.isVisible();
    }
    
    @Override public void setOpacity(float opacity) {
        platformWindow.setAlpha(opacity);
    }

    public boolean needsUpdateWindow() {
        return transparent && (Application.GetApplication().shouldUpdateWindow());
    }

    @Override public void setIconified(boolean iconified) {
        if (platformWindow.isMinimized() == iconified) {
            return;
        }
        platformWindow.minimize(iconified);
    }

    @Override public void setMaximized(boolean maximized) {
        if (platformWindow.isMaximized() == maximized) {
            return;
        }
        platformWindow.maximize(maximized);
    }

    @Override public void setResizable(boolean resizable) {
        platformWindow.setResizable(resizable);
        // note: for child windows this is ignored and we fail silently
    }

    // Return true if this stage is trusted for full screen - doesn't have a
    // security manager, or a permission check doesn't result in a security
    // exeception.
    boolean isTrustedFullScreen() {
        return hasPermission(fullScreenPermission);
    }

    // Safely exit full screen
    void exitFullScreen() {
        setFullScreen(false);
    }

    private boolean hasPermission(Permission perm) {
        try {
            if (System.getSecurityManager() != null) {
                getAccessControlContext().checkPermission(perm);
            }
            return true;
        } catch (AccessControlException ae) {
            return false;
        }
    }

    // We may need finer-grained permissions in the future, but for
    // now AllPermission is good enough to do the job we need, such
    // as fullscreen support for signed/unsigned application.
    private static final Permission fullScreenPermission = new AllPermission();

    private boolean fullScreenFromUserEvent = false;

    private void applyFullScreen() {
        View v = platformWindow.getView();
        if (isVisible() && v != null && v.isInFullscreen() != isInFullScreen) {
            if (isInFullScreen) {

                // Check whether app is full screen trusted or flag is set
                // indicating that the fullscreen request came from an input
                // event handler.
                // If not notify the stageListener to reset fullscreen to false.
                if (!isTrustedFullScreen() && !fullScreenFromUserEvent) {
                    exitFullScreen();
                } else {
                    v.enterFullscreen(false, false, false);
                    if (warning != null && warning.inWarningTransition()) {
                        warning.setView(getViewScene());
                    } else if (warning == null) {
                        warning = new OverlayWarning(getViewScene());
                        warning.warn();
                    }
                }
            } else {
                if (warning != null) {
                    warning.cancel();
                    warning = null;
                }
                v.exitFullscreen(false);
            }
            // Reset flag once we are done process fullscreen
            fullScreenFromUserEvent = false;
        } else if (!isVisible() && warning != null) {
            // if the window is closed - re-open with fresh warning
            warning.cancel();
            warning = null;
        }
    }

    @Override public void setFullScreen(boolean fullScreen) {
        if (isInFullScreen == fullScreen) {
            return;
        }

       // Set a flag indicating whether this method was called from
        // an input event handler.
        if (isInEventHandler()) {
            fullScreenFromUserEvent = true;
        }

        if (fullScreen && (activeFSWindowReference.get() != null)) {
            activeFSWindowReference.get().setFullScreen(false);
        }
        isInFullScreen = fullScreen;
        applyFullScreen();
        if (fullScreen) {
            activeFSWindowReference.set(this);
        }
    }

    void fullscreenChanged(final boolean fs) {
        if (!fs) {
            activeFSWindowReference.compareAndSet(this, null);
        }
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                if (stageListener != null) {
                    stageListener.changedFullscreen(fs);
                }
                return null;
            }
        }, getAccessControlContext());
    }

    @Override public void toBack() {
        platformWindow.toBack();
        if (isAppletStage && null != appletWindow) {
            appletWindow.assertStageOrder();
        }
    }

    @Override public void toFront() {
        platformWindow.requestFocus(); // RT-17836
        platformWindow.toFront();
        if (isAppletStage && null != appletWindow) {
            appletWindow.assertStageOrder();
        }
    }
    
    @Override public void close() {
        super.close();
        // prevents closing a closed platform window
        if (!platformWindowClosed) {
            AbstractPainter.renderLock.lock();
            try {
                GlassScene oldScene = getViewScene();
                platformWindow.close();
                if (oldScene != null) oldScene.updateViewState();
            } finally {
                AbstractPainter.renderLock.unlock();
            }
        }
    }

    private boolean platformWindowClosed = false;
    // setPlatformWindowClosed is only set upon receiving platform window has
    // closed notification. This state is necessary to prevent the platform
    // window from being closed more than once.
    @Override protected void setPlatformWindowClosed() {
        platformWindowClosed = true;
    }

    // True for unowned stage
    @Override public boolean isTopLevel() {
        return owner == null;
    }

    @Override public boolean grabFocus() {
        return platformWindow.grabFocus();
    }

    @Override public void ungrabFocus() {
        platformWindow.ungrabFocus();
    }

    @Override public void requestFocus() {
        platformWindow.requestFocus();
    }
    
    @Override public void requestFocus(FocusCause cause) {
        switch (cause) {
            case TRAVERSED_FORWARD:
                platformWindow.requestFocus(WindowEvent.FOCUS_GAINED_FORWARD);
                break;
            case TRAVERSED_BACKWARD:
                platformWindow.requestFocus(WindowEvent.FOCUS_GAINED_BACKWARD);
                break;
            case ACTIVATED:
                platformWindow.requestFocus(WindowEvent.FOCUS_GAINED);
                break;
            case DEACTIVATED:
                platformWindow.requestFocus(WindowEvent.FOCUS_LOST);
                break;
        }
    }

    @Override protected void setPlatformEnabled(boolean enabled) {
        if (!platformWindowClosed) {
            platformWindow.setEnabled(enabled);
        }
        if (!enabled) {
            GlassStage.removeActiveWindow(this);
        }
    }

    void setEnabled(boolean enabled) {
        if ((owner != null) && (owner instanceof WindowStage)) {
            ((WindowStage) owner).setEnabled(enabled);
        }
        /*
         * RT-17588 - exit if stage is closed from under us as 
         *            any further access to the Glass layer 
         *            will throw an exception
         */
        if (enabled && platformWindow.isClosed()) {
            return;
        }
        setPlatformEnabled(enabled);
        if (enabled) {
            this.requestToFront();
            if (isAppletStage && null != appletWindow) {
                appletWindow.assertStageOrder();
            }
        }
    }

    void windowsSetEnabled(boolean enabled) {
        // TODO: Need to solve RT-12605:
        // If Window #1 pops up an APPLICATION modal dialog #2 it should block
        // Window #1, but will also block Window #3, #4, etc., unless those
        // windows are descendants of #2.
        for (GlassStage window : windows) {
            if (window != this) {
                window.setPlatformEnabled(enabled);
                if (enabled) {
                    window.requestToFront();
                }
            }
        }
    }

    // Note: This method is required to workaround a glass issue mentioned 
    // in RT-12607
    @Override protected void requestToFront() {
        platformWindow.toFront();
        platformWindow.requestFocus();
    }

    public void setInEventHandler(boolean inEventHandler) {
        this.inEventHandler = inEventHandler;
    }

    public boolean isInEventHandler() {
        return inEventHandler;
    }

    @Override
    public void requestInput(String text, int type, double width, double height, 
                        double Mxx, double Mxy, double Mxz, double Mxt,
                        double Myx, double Myy, double Myz, double Myt, 
                        double Mzx, double Mzy, double Mzz, double Mzt) {
        platformWindow.requestInput(text, type, width, height, 
                                    Mxx, Mxy, Mxz, Mxt, 
                                    Myx, Myy, Myz, Myt, 
                                    Mzx, Mzy, Mzz, Mzt);
    }

    @Override
    public void releaseInput() {
        platformWindow.releaseInput();
    }

    @Override public void setRTL(boolean b) {
        rtl = b;
    }

    /**
     * 
     * Accessibility glue for native
     * 
     */
    /**
     * Initialize Accessiblility
     */
    @Override public void accessibleInitIsComplete(Object ac) {
        if( ac instanceof AccessibleRoot)
            platformWindow.accessibilityInitIsComplete((AccessibleRoot)ac);
        else
            platformWindow.accessibilityInitIsComplete(null);
    } 

    /**
     * Create accessible native object corresponding to stage
     * 
     * @param ac 
     * returns native Object
     */
    @Override public Object accessibleCreateStageProvider(AccessibleStageProvider ac, long ptr) {
        return AccessibleRoot.createAccessible(ac,ptr) ;
    }

    /**
     * Create accessible native object corresponding to controls
     * 
     * @param ac 
     * returns native Object
     */
    @Override public Object accessibleCreateBasicProvider(AccessibleProvider ac) {
        return AccessibleBaseProvider.createProvider(ac);
    }

    /**
     * Delete accessible native object corresponding to controls
     * 
     * @param nativeAcc
     * returns native Object
     */
    @Override public void accessibleDestroyBasicProvider(Object nativeAcc) {
        if( nativeAcc instanceof AccessibleBaseProvider)
            ((AccessibleBaseProvider)nativeAcc).destroyAccessible();
    }

    /**
     * Fire accessible event
     * 
     * @param eventID   identifies the event.
     */
    @Override public void accessibleFireEvent(Object nativeAcc, int eventID) {
        if( nativeAcc instanceof AccessibleBaseProvider)
           ((AccessibleBaseProvider)nativeAcc).fireEvent(eventID); 
    }
    
    /** Fire accessible property change event
     * 
     * @param propertyId    identifies the property
     * @param oldProperty   the old value of the property
     * @param newProperty   the new value of the property
     */
    @Override public void accessibleFirePropertyChange(Object nativeAcc, int propertyId, int oldProperty,
                                             int newProperty ) {
        if( nativeAcc instanceof AccessibleBaseProvider)
           ((AccessibleBaseProvider)nativeAcc).firePropertyChange(propertyId, oldProperty, newProperty); 
    }
    
    @Override public void accessibleFirePropertyChange(Object nativeAcc, int propertyId, boolean oldProperty,
                                             boolean newProperty ) {
        if( nativeAcc instanceof AccessibleBaseProvider)
           ((AccessibleBaseProvider)nativeAcc).firePropertyChange(propertyId, oldProperty, newProperty); 
    }    
}
