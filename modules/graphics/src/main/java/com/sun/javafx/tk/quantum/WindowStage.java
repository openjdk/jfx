/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.security.AccessControlContext;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.*;
import com.sun.glass.ui.Window.Level;
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
import java.util.Locale;
import java.util.ResourceBundle;

class WindowStage extends GlassStage {

    protected Window platformWindow;

    protected javafx.stage.Stage fxStage;

    private StageStyle style;
    private GlassStage owner = null;
    private Modality modality = Modality.NONE;

    private OverlayWarning warning = null;
    private boolean rtl = false;
    private boolean transparent = false;
    private boolean isPrimaryStage = false;
    private boolean isAppletStage = false; // true if this is an embedded applet window
    private boolean isPopupStage = false;
    private boolean isInFullScreen = false;

    // A flag to indicate whether a call was generated from
    // an input event handler.
    private boolean inEventHandler = false;

    // An active window is visible && enabled && focusable.
    // The list is maintained in the z-order, so that the last element
    // represents the topmost window (or more accurately, the last
    // focused window, which we assume is very close to the last topmost one).
    private static List<WindowStage> activeWindows = new LinkedList<>();

    private static Map<Window, WindowStage> platformWindows = new HashMap<>();

    private static GlassAppletWindow appletWindow = null;
    static void setAppletWindow(GlassAppletWindow aw) {
        appletWindow = aw;
    }
    static GlassAppletWindow getAppletWindow() {
        return appletWindow;
    }

    private static final Locale LOCALE = Locale.getDefault();
    
    private static final ResourceBundle RESOURCES =
        ResourceBundle.getBundle(WindowStage.class.getPackage().getName() +
                                 ".QuantumMessagesBundle", LOCALE);


    public WindowStage(javafx.stage.Window peerWindow, final StageStyle stageStyle, Modality modality, TKStage owner) {
        this.style = stageStyle;
        this.owner = (GlassStage)owner;
        this.modality = modality;

        if (peerWindow instanceof javafx.stage.Stage) {
            fxStage = (Stage)peerWindow;
        } else {
            fxStage = null;
        }

        transparent = stageStyle == StageStyle.TRANSPARENT;
        if (owner == null) {
            if (this.modality == Modality.WINDOW_MODAL) {
                this.modality = Modality.NONE;
            }
        }
    }

    final void setIsPrimary() {
        isPrimaryStage = true;
        if (appletWindow != null) {
            // this is an embedded applet stage
            isAppletStage = true;
        }
    }

    final void setIsPopup() {
        isPopupStage = true;
    }

    // Called by QuantumToolkit, so we can override initPlatformWindow in subclasses
    public final WindowStage init(GlassSystemMenu sysmenu) {
        initPlatformWindow();
        platformWindow.setEventHandler(new GlassWindowEventHandler(this));
        if (sysmenu.isSupported()) {
            sysmenu.createMenuBar();
            platformWindow.setMenuBar(sysmenu.getMenuBar());
        }
        return this;
    }

    private void initPlatformWindow() {
        if (platformWindow == null) {
            Application app = Application.GetApplication();
            if (isPrimaryStage && (null != appletWindow)) {
                platformWindow = app.createWindow(appletWindow.getGlassWindow().getNativeWindow());
            } else {
                Window ownerWindow = null;
                if (owner instanceof WindowStage) {
                    ownerWindow = ((WindowStage)owner).platformWindow;
                }
                boolean resizable = false;
                boolean focusable = true;
                int windowMask = rtl ? Window.RIGHT_TO_LEFT : 0;
                if (isPopupStage) { // TODO: make it a stage style?
                    windowMask |= Window.POPUP;
                    if (style == StageStyle.TRANSPARENT) {
                        windowMask |= Window.TRANSPARENT;
                    }
                    focusable = false;
                } else {
                    switch (style) {
                        case UNIFIED:
                            if (app.supportsUnifiedWindows()) {
                                windowMask |= Window.UNIFIED;
                            }
                            // fall through
                        case DECORATED:
                            windowMask |=
                                    Window.TITLED | Window.CLOSABLE | Window.MINIMIZABLE | Window.MAXIMIZABLE;
                            resizable = true;
                            break;
                        case UTILITY:
                            windowMask |=  Window.TITLED | Window.UTILITY | Window.CLOSABLE;
                            break;
                        default:
                            windowMask |=
                                    (transparent ? Window.TRANSPARENT : Window.UNTITLED) | Window.CLOSABLE;
                            break;
                    }
                }
                platformWindow =
                        app.createWindow(ownerWindow, Screen.getMainScreen(), windowMask);
                platformWindow.setResizable(resizable);
                platformWindow.setFocusable(focusable);
            }
        }
        platformWindows.put(platformWindow, this);
    }

    final Window getPlatformWindow() {
        return platformWindow;
    }

    static WindowStage findWindowStage(Window platformWindow) {
        return platformWindows.get(platformWindow);
    }

    protected GlassStage getOwner() {
        return owner;
    }
    
    protected ViewScene getViewScene() {
        return (ViewScene)getScene();
    }
    
    StageStyle getStyle() {
        return style;
    }

    @Override public TKScene createTKScene(boolean depthBuffer, boolean antiAliasing, AccessControlContext acc) {
        ViewScene scene = new ViewScene(depthBuffer, antiAliasing);
        scene.setSecurityContext(acc);
        return scene;
    }
    
    /**
     * Set the scene to be displayed in this stage
     *
     * @param scene The peer of the scene to be displayed
     */
    @Override public void setScene(TKScene scene) {
        GlassScene oldScene = getScene();
        if (oldScene == scene) {
            // Nothing to do
            return;
        }
        // RT-21465, RT-28490
        // We don't support scene changes in full-screen mode.
        exitFullScreen();
        super.setScene(scene);
        if (scene != null) {
            GlassScene newScene = getViewScene();
            View view = newScene.getPlatformView();
            ViewPainter.renderLock.lock();
            try {
                platformWindow.setView(view);
                if (oldScene != null) oldScene.updateSceneState();
                newScene.updateSceneState();
            } finally {
                ViewPainter.renderLock.unlock();
            }
            requestFocus();
        } else {
            ViewPainter.renderLock.lock();
            try {
                // platformWindow can be null here, if this window is owned,
                // and its owner is being closed.
                if (platformWindow != null) {
                    platformWindow.setView(null);
                }
                if (oldScene != null) {
                    oldScene.updateSceneState();
                }
            } finally {
                ViewPainter.renderLock.unlock();
            }
        }
        if (oldScene != null) {
            ViewPainter painter = ((ViewScene)oldScene).getPainter();
            QuantumRenderer.getInstance().disposePresentable(painter.presentable);   // latched on RT
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
        platformWindow.setMinimumSize(minWidth, minHeight);
    }

    @Override public void setMaximumSize(int maxWidth, int maxHeight) {
        platformWindow.setMaximumSize(maxWidth, maxHeight);
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
            platformWindow.setIcon(null);
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

        PushbroomScaler scaler = ScalerFactory.createScaler(image.getWidth(), image.getHeight(),
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
            scaler.putSourceScanline(bytes, 0);
        }

        buf.rewind();

        final Image img = image.iconify(scaler.getDestination(), SMALL_ICON_WIDTH, SMALL_ICON_HEIGHT);
        platformWindow.setIcon(PixelUtils.imageToPixels(img));
    }

    @Override public void setTitle(String title) {
        platformWindow.setTitle(title);
    }

    @Override public void setVisible(final boolean visible) {
        // Before setting visible to false on the native window, we unblock
        // other windows.
        if (!visible) {
            removeActiveWindow(this);
            if (modality == Modality.WINDOW_MODAL) {
                if (owner != null && owner instanceof WindowStage) {
                    ((WindowStage) owner).setEnabled(true);
                }
            } else if (modality == Modality.APPLICATION_MODAL) {
                windowsSetEnabled(true);
            } else {
                // Note: This method is required to workaround a glass issue
                // mentioned in RT-12607
                if (owner != null && owner instanceof WindowStage) {
                    WindowStage ownerStage = (WindowStage)owner;
                    ownerStage.requestToFront();
                }
            }
        }
        try {
            ViewPainter.renderLock.lock();
            // platformWindow can be null here, if this window is owned,
            // and its owner is being closed.
            if (platformWindow != null) {
                platformWindow.setVisible(visible);
            }
            super.setVisible(visible);
        } finally {
            ViewPainter.renderLock.unlock();
        }
        // After setting visible to true on the native window, we block
        // other windows.
        if (visible) {
            if (modality == Modality.WINDOW_MODAL) {
                if (owner != null && owner instanceof WindowStage) {
                    ((WindowStage) owner).setEnabled(false);
                }
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

    @Override
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        if (alwaysOnTop) {
            if (hasPermission(alwaysOnTopPermission)) {
                platformWindow.setLevel(Level.FLOATING);
            }
        } else {
            platformWindow.setLevel(Level.NORMAL);
        }
        
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
    
    boolean isApplet() {
        return isPrimaryStage && null != appletWindow;
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
    private static final Permission alwaysOnTopPermission = new AllPermission();

    private boolean fullScreenFromUserEvent = false;

    private KeyCombination savedFullScreenExitKey = null;

    public final KeyCombination getSavedFullScreenExitKey() {
        return savedFullScreenExitKey;
    }

    private void applyFullScreen() {
        if (platformWindow == null) {
            // applyFullScreen() can be called from setVisible(false), while the
            // platformWindow has already been destroyed.
            return;
        }
        View v = platformWindow.getView();
        if (isVisible() && v != null && v.isInFullscreen() != isInFullScreen) {
            if (isInFullScreen) {
                // Check whether app is full screen trusted or flag is set
                // indicating that the fullscreen request came from an input
                // event handler.
                // If not notify the stageListener to reset fullscreen to false.
                final boolean isTrusted = isTrustedFullScreen();
                if (!isTrusted && !fullScreenFromUserEvent) {
                    exitFullScreen();
                } else {
                    v.enterFullscreen(false, false, false);
                    if (warning != null && warning.inWarningTransition()) {
                        warning.setView(getViewScene());
                    } else {
                        boolean showWarning = true;

                        KeyCombination key = null;
                        String exitMessage = null;

                        if (isTrusted && (fxStage != null)) {
                            // copy the user set definitions for later use.
                            key = fxStage.getFullScreenExitKeyCombination();

                            exitMessage = fxStage.getFullScreenExitHint();
                        }

                        savedFullScreenExitKey =
                                key == null
                                ? defaultFullScreenExitKeycombo
                                : key;

                        if (
                            // the hint is ""
                            "".equals(exitMessage) ||
                            // if the key is NO_MATCH
                            (savedFullScreenExitKey.equals(KeyCombination.NO_MATCH))
                                ) {
                            showWarning = false;
                        }

                        // the hint is not set, use the key for the message
                        if (showWarning && exitMessage == null) {
                            if (key == null) {
                                exitMessage = RESOURCES.getString("OverlayWarningESC");
                            } else {
                                String f = RESOURCES.getString("OverlayWarningKey");
                                exitMessage = f.format(f, savedFullScreenExitKey.toString());
                            }
                        }

                        if (showWarning && warning == null) {
                            setWarning(new OverlayWarning(getViewScene()));
                        }

                        if (showWarning && warning != null) {
                            warning.warn(exitMessage);
                        }
                    }
                }
            } else {
                if (warning != null) {
                    warning.cancel();
                    setWarning(null);
                }
                v.exitFullscreen(false);
            }
            // Reset flag once we are done process fullscreen
            fullScreenFromUserEvent = false;
        } else if (!isVisible() && warning != null) {
            // if the window is closed - re-open with fresh warning
            warning.cancel();
            setWarning(null);
        }
    }

    void setWarning(OverlayWarning newWarning) {
        this.warning = newWarning;
        getViewScene().synchroniseOverlayWarning();
    }

    OverlayWarning getWarning() {
        return warning;
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

        GlassStage fsWindow = activeFSWindow.get();
        if (fullScreen && (fsWindow != null)) {
            fsWindow.setFullScreen(false);
        }
        isInFullScreen = fullScreen;
        applyFullScreen();
        if (fullScreen) {
            activeFSWindow.set(this);
        }
    }

    void fullscreenChanged(final boolean fs) {
        if (!fs) {
            if (activeFSWindow.compareAndSet(this, null)) {
                isInFullScreen = false;
            }
        } else {
            isInFullScreen = true;
            activeFSWindow.set(this);
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
        ViewPainter.renderLock.lock();
        try {
            // prevents closing a closed platform window
            if (platformWindow != null) {
                platformWindows.remove(platformWindow);
                platformWindow.close();
                platformWindow = null;
            }
            GlassScene oldScene = getViewScene();
            if (oldScene != null) {
                oldScene.updateSceneState();
            }
        } finally {
            ViewPainter.renderLock.unlock();
        }
    }

    // setPlatformWindowClosed is only set upon receiving platform window has
    // closed notification. This state is necessary to prevent the platform
    // window from being closed more than once.
    void setPlatformWindowClosed() {
        platformWindow = null;
    }

    static void addActiveWindow(WindowStage window) {
        activeWindows.remove(window);
        activeWindows.add(window);
    }

    static void removeActiveWindow(WindowStage window) {
        activeWindows.remove(window);
    }

    final void handleFocusDisabled() {
        if (activeWindows.isEmpty()) {
            return;
        }
        WindowStage window = activeWindows.get(activeWindows.size() - 1);
        window.setIconified(false);
        window.requestToFront();
        window.requestFocus();
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

    @Override
    protected void setPlatformEnabled(boolean enabled) {
        super.setPlatformEnabled(enabled);
        platformWindow.setEnabled(enabled);
        if (enabled) {
            requestToFront();
        } else {
            removeActiveWindow(this);
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
            if (isAppletStage && null != appletWindow) {
                appletWindow.assertStageOrder();
            }
        }
    }

    // Note: This method is required to workaround a glass issue mentioned in RT-12607
    protected void requestToFront() {
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
     * Initialize Accessibility
     *
     * @param ac    the Glass accessible root object.
     */
    @Override public void setAccessibilityInitIsComplete(Object ac) {
        if (ac instanceof AccessibleRoot) {
            platformWindow.setAccessibilityInitIsComplete((AccessibleRoot)ac);
        } else {
            platformWindow.setAccessibilityInitIsComplete(null);
        }
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
        return AccessibleRoot.createAccessible(ac, platformWindow);
    }

    /**
     * Create Glass accessible object corresponding to controls
     * 
     * @param ac    the FX accessible node
     * 
     * @return the Glass accessible Object
     */
    @Override public Object accessibleCreateBasicProvider(AccessibleProvider ac) {
        return AccessibleBaseProvider.createProvider(ac);
    }

    /**
     * Delete Glass accessible object corresponding to controls
     *
     * @param glassAcc the Glass accessible
     */
    @Override public void accessibleDestroyBasicProvider(Object glassAcc) {
        if (glassAcc instanceof AccessibleBaseProvider) {
            ((AccessibleBaseProvider)glassAcc).destroyAccessible();
        }
    }

    /**
     * Fire accessible event
     *
     * @param glassAcc  the Glass accessible
     * @param eventID   identifies the event.
     */
    @Override public void accessibleFireEvent(Object glassAcc, int eventID) {
        if (glassAcc instanceof AccessibleBaseProvider) {
            ((AccessibleBaseProvider)glassAcc).fireEvent(eventID);
        }
    }
    
    /**
     * Fire accessible property change event
     * 
     * @param glassAcc      the Glass accessible 
     * @param propertyId    identifies the property
     * @param oldProperty   the old value of the property
     * @param newProperty   the new value of the property
     */
    @Override public void accessibleFirePropertyChange( Object glassAcc, int propertyId,
                                                        int oldProperty, int newProperty ) {
        if (glassAcc instanceof AccessibleBaseProvider) {
            ((AccessibleBaseProvider)glassAcc).
                firePropertyChange(propertyId, oldProperty, newProperty);
        }
    }
    
    /**
     * Fire accessible property change event
     * 
     * @param glassAcc      the Glass accessible
     * @param propertyId    identifies the property
     * @param oldProperty   the old value of the property
     * @param newProperty   the new value of the property
     */
    @Override public void accessibleFirePropertyChange( Object glassAcc, int propertyId,
                                                        boolean oldProperty,
                                                        boolean newProperty ) {
        if (glassAcc instanceof AccessibleBaseProvider) {
            ((AccessibleBaseProvider)glassAcc).
                firePropertyChange(propertyId, oldProperty, newProperty);
        }
    }
}
