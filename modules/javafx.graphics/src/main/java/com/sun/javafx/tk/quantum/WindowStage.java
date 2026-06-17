/*
 * Copyright (c) 2008, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.iio.common.PushbroomScaler;
import com.sun.javafx.iio.common.ScalerFactory;
import com.sun.javafx.stage.HeaderButtonMetrics;
import com.sun.javafx.stage.StagePeerListener;
import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.TKStageListener;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class WindowStage extends GlassStage {

    protected Window platformWindow;

    protected javafx.stage.Stage fxStage;

    private StageStyle style;
    private GlassStage owner = null;
    private Modality modality = Modality.NONE;

    private OverlayWarning warning = null;
    private boolean rtl = false;
    private boolean transparent = false;
    private boolean darkFrame = false;
    private boolean isPrimaryStage = false;
    private boolean isPopupStage = false;
    private boolean isInFullScreen = false;
    private boolean isAlwaysOnTop = false;

    // An active window is visible && enabled && focusable.
    // The list is maintained in the z-order, so that the last element
    // represents the topmost window (or more accurately, the last
    // focused window, which we assume is very close to the last topmost one).
    private static List<WindowStage> activeWindows = new LinkedList<>();

    private static Map<Window, WindowStage> platformWindows = new HashMap<>();

    private static final Locale LOCALE = Locale.getDefault();

    private static final ResourceBundle RESOURCES =
        ResourceBundle.getBundle(WindowStage.class.getPackage().getName() +
                                 ".QuantumMessagesBundle", LOCALE);

    public WindowStage(javafx.stage.Window peerWindow, final StageStyle stageStyle, Modality modality,
                       TKStage owner, boolean darkFrame) {
        this.style = stageStyle;
        this.owner = (GlassStage)owner;
        this.modality = modality;
        this.darkFrame = darkFrame;

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

            Window ownerWindow = null;
            if (owner instanceof WindowStage) {
                ownerWindow = ((WindowStage)owner).platformWindow;
            }
            boolean resizable = fxStage != null && fxStage.isResizable();
            boolean focusable = true;
            int windowMask = rtl ? Window.RIGHT_TO_LEFT : 0;
            if (isPopupStage) { // TODO: make it a stage style?
                windowMask |= Window.POPUP;
                if (style == StageStyle.TRANSPARENT) {
                    windowMask |= Window.TRANSPARENT;
                }
                focusable = false;
                resizable = false;
            } else {
                // Downgrade conditional stage styles if not supported
                if (style == StageStyle.UNIFIED && !app.supportsUnifiedWindows()) {
                    style = StageStyle.DECORATED;
                } else if (style == StageStyle.EXTENDED && !app.supportsExtendedWindows()) {
                    style = StageStyle.DECORATED;
                }

                switch (style) {
                    case UNIFIED:
                        windowMask |= Window.UNIFIED;
                        // fall through
                    case DECORATED:
                        windowMask |= Window.TITLED | Window.CLOSABLE | Window.MINIMIZABLE | Window.MAXIMIZABLE;
                        break;
                    case EXTENDED:
                        windowMask |= Window.EXTENDED | Window.CLOSABLE | Window.MINIMIZABLE | Window.MAXIMIZABLE;
                        break;
                    case UTILITY:
                        windowMask |=  Window.TITLED | Window.UTILITY | Window.CLOSABLE;
                        break;
                    default:
                        windowMask |= (transparent ? Window.TRANSPARENT : Window.UNTITLED) | Window.CLOSABLE;
                        break;
                }

                if (ownerWindow != null || modality != Modality.NONE) {
                    windowMask &= ~(Window.MINIMIZABLE | Window.MAXIMIZABLE);
                }
            }

            if (modality != Modality.NONE) {
                windowMask |= Window.MODAL;
            }

            if (darkFrame) {
                windowMask |= Window.DARK_FRAME;
            }

            platformWindow = app.createWindow(ownerWindow, Screen.getMainScreen(), windowMask);
            platformWindow.setResizable(resizable);
            platformWindow.setFocusable(focusable);

            if (platformWindow.isExtendedWindow()) {
                platformWindow.headerButtonOverlayProperty().subscribe(overlay -> {
                    ViewScene scene = getViewScene();
                    if (scene != null) {
                        scene.setOverlay(isInFullScreen ? null : overlay);
                    }
                });

                platformWindow.headerButtonMetricsProperty().subscribe(this::notifyHeaderButtonMetricsChanged);
            }

            if (fxStage != null && fxStage.getScene() != null) {
                javafx.scene.paint.Paint paint = fxStage.getScene().getFill();
                if (paint instanceof javafx.scene.paint.Color) {
                    javafx.scene.paint.Color color = (javafx.scene.paint.Color) paint;
                    platformWindow.setBackground((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
                } else if (paint instanceof javafx.scene.paint.LinearGradient) {
                    javafx.scene.paint.LinearGradient lgradient = (javafx.scene.paint.LinearGradient) paint;
                    computeAndSetBackground(lgradient.getStops());
                } else if (paint instanceof javafx.scene.paint.RadialGradient) {
                    javafx.scene.paint.RadialGradient rgradient = (javafx.scene.paint.RadialGradient) paint;
                    computeAndSetBackground(rgradient.getStops());
                }
            }

        }
        platformWindows.put(platformWindow, this);
    }

    private void computeAndSetBackground(List<javafx.scene.paint.Stop> stops) {
        if (stops.size() == 1) {
            javafx.scene.paint.Color color = stops.get(0).getColor();
            platformWindow.setBackground((float) color.getRed(),
                    (float) color.getGreen(), (float) color.getBlue());
        } else if (stops.size() > 1) {
            // A simple attempt to find a reasonable average color that is
            // within the stops arrange.
            javafx.scene.paint.Color color = stops.get(0).getColor();
            javafx.scene.paint.Color color2 = stops.get(stops.size() - 1).getColor();
            platformWindow.setBackground((float) ((color.getRed() + color2.getRed()) / 2.0),
                    (float) ((color.getGreen() + color2.getGreen()) / 2.0),
                    (float) ((color.getBlue() + color2.getBlue()) / 2.0));
        }
    }

    private void notifyHeaderButtonMetricsChanged() {
        if (stageListener instanceof StagePeerListener listener && platformWindow != null) {
            var metrics = platformWindow.headerButtonMetricsProperty().get();
            listener.changedHeaderButtonMetrics(
                new HeaderButtonMetrics(metrics.leftInset(), metrics.rightInset(), metrics.minHeight()));
        }
    }

    public final Window getPlatformWindow() {
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

    @Override
    public void setTKStageListener(TKStageListener listener) {
        super.setTKStageListener(listener);
        notifyHeaderButtonMetricsChanged();
    }

    @Override public TKScene createTKScene(boolean depthBuffer, boolean msaa) {
        ViewScene scene = new ViewScene(fxStage != null ? fxStage.getScene() : null, depthBuffer, msaa);

        // The window-provided overlay is not visible in full-screen mode.
        if (!isInFullScreen) {
            scene.setOverlay(platformWindow.headerButtonOverlayProperty().get());
        }

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
        // JDK-8126842, JDK-8124937
        // We don't support scene changes in full-screen mode.
        exitFullScreen();
        super.setScene(scene);
        if (scene != null) {
            GlassScene newScene = getViewScene();
            View view = newScene.getPlatformView();
            QuantumToolkit.runWithRenderLock(() -> {
                platformWindow.setView(view);
                if (oldScene != null) oldScene.updateSceneState();
                newScene.updateSceneState();
                return null;
            });
        } else {
            QuantumToolkit.runWithRenderLock(() -> {
                // platformWindow can be null here, if this window is owned,
                // and its owner is being closed.
                if (platformWindow != null) {
                    platformWindow.setView(null);
                }
                if (oldScene != null) {
                    oldScene.updateSceneState();
                }
                return null;
            });
        }
        if (oldScene != null) {
            disposeScenePainter((ViewScene) oldScene);
        }
    }

    private static void disposeScenePainter(ViewScene oldScene) {
        ViewPainter painter = oldScene.getPainter();
        QuantumRenderer.getInstance().disposePresentable(painter.presentable);   // latched on RT
    }

    @Override public void setBounds(float x, float y, boolean xSet, boolean ySet,
                                    float w, float h, float cw, float ch,
                                    float xGravity, float yGravity,
                                    float renderScaleX, float renderScaleY)
    {
        if (renderScaleX > 0.0 || renderScaleY > 0.0) {
            // We set the render scale first since the call to setBounds()
            // below can induce a recursive update on the scales if it moves
            // the window to a new screen and we will then end up being called
            // back with a new scale.  We do not want to set these old scale
            // values after that recursion happens.
            if (renderScaleX > 0.0) {
                platformWindow.setRenderScaleX(renderScaleX);
            }
            if (renderScaleY > 0.0) {
                platformWindow.setRenderScaleY(renderScaleY);
            }
            ViewScene vscene = getViewScene();
            if (vscene != null) {
                vscene.updateSceneState();
                vscene.entireSceneNeedsRepaint();
            }
        }
        if (xSet || ySet || w > 0 || h > 0 || cw > 0 || ch > 0) {
            platformWindow.setBounds(x, y, xSet, ySet, w, h, cw, ch, xGravity, yGravity);
        }
    }

    @Override
    public float getPlatformScaleX() {
        return platformWindow.getPlatformScaleX();
    }

    @Override
    public float getPlatformScaleY() {
        return platformWindow.getPlatformScaleY();
    }

    @Override
    public float getOutputScaleX() {
        return platformWindow.getOutputScaleX();
    }

    @Override
    public float getOutputScaleY() {
        return platformWindow.getOutputScaleY();
    }

    @Override public void setMinimumSize(int minWidth, int minHeight) {
        minWidth  = (int) Math.ceil(minWidth  * getPlatformScaleX());
        minHeight = (int) Math.ceil(minHeight * getPlatformScaleY());
        platformWindow.setMinimumSize(minWidth, minHeight);
    }

    @Override public void setMaximumSize(int maxWidth, int maxHeight) {
        maxWidth  = (int) Math.ceil(maxWidth  * getPlatformScaleX());
        maxHeight = (int) Math.ceil(maxHeight * getPlatformScaleY());
        platformWindow.setMaximumSize(maxWidth, maxHeight);
    }

    static Image findBestImage(java.util.List icons, int width, int height) {
        Image image = null;
        double bestSimilarity = 3; //Impossibly high value
        for (Object icon : icons) {
            //Iterate imageList looking for best matching image.
            //'Similarity' measure is defined as good scale factor and small insets.
            //best possible similarity is 0 (no scale, no insets).
            //It's found by experimentation that good-looking results are achieved
            //with scale factors x1, x3/4, x2/3, xN, x1/N.
            //Check to make sure the image/image format is correct.
            Image im = (Image)icon;
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
                double scaleFactor = Math.min((double)width / (double)iw,
                                              (double)height / (double)ih);
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
                    adjw = (int)Math.round(iw / scaleDivider);
                    adjh = (int)Math.round(ih / scaleDivider);
                    scaleMeasure = 1.0 - 1.0 / scaleDivider;
                }
                double similarity = ((double)width - (double)adjw) / width +
                    ((double)height - (double)adjh) / height + //Large padding is bad
                    scaleMeasure; //Large rescale is bad
                if (similarity < bestSimilarity) {
                    bestSimilarity = similarity;
                    image = im;
                }
                if (similarity == 0) break;
            }
        }
        return image;
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

        Image image = findBestImage(icons, SMALL_ICON_WIDTH, SMALL_ICON_HEIGHT);
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
            }
            // Note: This method is required to workaround a glass issue
            // mentioned in JDK-8112637
            // If the hiding stage is unfocusable (i.e. it's a PopupStage),
            // then we don't do this to avoid stealing the focus.
            // JDK-8210973: APPLICATION_MODAL window can have owner.
            if (!isPopupStage && owner != null && owner instanceof WindowStage) {
                WindowStage ownerStage = (WindowStage)owner;
                ownerStage.requestToFront();
            }
        }
        QuantumToolkit.runWithRenderLock(() -> {
            // platformWindow can be null here, if this window is owned,
            // and its owner is being closed.
            if (platformWindow != null) {
                platformWindow.setVisible(visible);
            }
            super.setVisible(visible);
            return null;
        });
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
        }

        applyFullScreen();
    }

    @Override boolean isVisible() {
        return platformWindow.isVisible();
    }

    @Override public void setOpacity(float opacity) {
        platformWindow.setAlpha(opacity);
        GlassScene gs = getScene();
        if (gs != null) {
            gs.entireSceneNeedsRepaint();
        }
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
        if (isAlwaysOnTop == alwaysOnTop) {
            return;
        }

        if (alwaysOnTop) {
            platformWindow.setLevel(Level.FLOATING);
        } else {
            platformWindow.setLevel(Level.NORMAL);
        }
        isAlwaysOnTop = alwaysOnTop;
    }

    @Override public void setResizable(boolean resizable) {
        platformWindow.setResizable(resizable);
        // note: for child windows this is ignored and we fail silently
    }

    // Safely exit full screen
    void exitFullScreen() {
        setFullScreen(false);
    }

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
                v.enterFullscreen(false, false, false);
                if (warning != null && warning.inWarningTransition()) {
                    warning.setView(getViewScene());
                } else {
                    boolean showWarning = true;

                    KeyCombination key = null;
                    String exitMessage = null;

                    if (fxStage != null) {
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
            } else {
                if (warning != null) {
                    warning.cancel();
                }

                setWarning(null);
                v.exitFullscreen(false);
            }
        } else if (!isVisible() && warning != null) {
            // if the window is closed - re-open with fresh warning
            warning.cancel();
            setWarning(null);
        }
    }

    void setWarning(OverlayWarning newWarning) {
        this.warning = newWarning;
        if (newWarning != null) {
            getViewScene().setOverlay(newWarning);
        } else if (!isInFullScreen) {
            getViewScene().setOverlay(platformWindow.headerButtonOverlayProperty().get());
        }
    }

    @Override public void setFullScreen(boolean fullScreen) {
        if (isInFullScreen == fullScreen) {
            return;
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
        if (stageListener != null) {
            stageListener.changedFullscreen(fs);
        }
    }

    @Override public void toBack() {
        platformWindow.toBack();
    }

    @Override public void toFront() {
        platformWindow.requestFocus(); // JDK-8128222
        platformWindow.toFront();
    }

    private boolean isClosePostponed = false;
    private Window deadWindow = null;

    @Override
    public void postponeClose() {
        isClosePostponed = true;
    }

    @Override
    public void closePostponed() {
        if (deadWindow != null) {
            deadWindow.close();
            deadWindow = null;
        }
    }

    @Override public void close() {
        super.close();
        QuantumToolkit.runWithRenderLock(() -> {
            // prevents closing a closed platform window
            if (platformWindow != null) {
                platformWindows.remove(platformWindow);
                if (isClosePostponed) {
                    deadWindow = platformWindow;
                } else {
                    platformWindow.close();
                }
                platformWindow = null;
            }
            ViewScene oldScene = getViewScene();
            if (oldScene != null) {
                oldScene.updateSceneState();
                disposeScenePainter(oldScene);
            }
            return null;
        });
    }

    // setPlatformWindowClosed is only set upon receiving platform window has
    // closed notification. This state is necessary to prevent the platform
    // window from being closed more than once.
    void setPlatformWindowClosed() {
        if (platformWindow != null) {
            platformWindows.remove(platformWindow);
            platformWindow = null;
        }
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
        if (platformWindow != null) {
            platformWindow.setEnabled(enabled);
        }
        if (enabled) {
            // Check if window is really enabled - to handle nested case
            if (platformWindow != null && platformWindow.isEnabled()
                    && modality == Modality.APPLICATION_MODAL) {
                requestToFront();
            }
        } else {
            removeActiveWindow(this);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if ((owner != null) && (owner instanceof WindowStage)) {
            ((WindowStage) owner).setEnabled(enabled);
        }
        /*
         * JDK-8128168 - exit if stage is closed from under us as
         *            any further access to the Glass layer
         *            will throw an exception
         */
        if (enabled && (platformWindow == null || platformWindow.isClosed())) {
            return;
        }
        setPlatformEnabled(enabled);
    }

    @Override
    public long getRawHandle() {
       return platformWindow.getRawHandle();
    }

    // Note: This method is required to workaround a glass issue mentioned in JDK-8112637
    protected void requestToFront() {
        if (platformWindow != null) {
            platformWindow.toFront();
            platformWindow.requestFocus();
        }
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

    @Override
    public void setPrefHeaderButtonHeight(double height) {
        if (platformWindow != null) {
            platformWindow.setPrefHeaderButtonHeight(height);
        }
    }

    @Override
    public void setDarkFrame(boolean value) {
        darkFrame = value;

        if (platformWindow != null) {
            platformWindow.setDarkFrame(value);
        }
    }
}
