/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk;

import javafx.geometry.Dimension2D;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.GlassRobot;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.runtime.async.AsyncOperation;
import com.sun.javafx.runtime.async.AsyncOperationListener;
import com.sun.javafx.scene.text.TextLayoutFactory;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractPrimaryTimer;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import java.util.Optional;

/**
 * A stubbed out Toolkit that provides no useful implementation. This is used
 * by the build to run the CSS to binary converter. The parser uses
 * PlatformLogger which requires a Toolkit.
 *
 */
final public class DummyToolkit extends Toolkit {

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean canStartNestedEventLoop() {
        return false;
    }

    @Override
    public Object enterNestedEventLoop(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void exitNestedEventLoop(Object key, Object rval) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void exitAllNestedEventLoops() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TKStage createTKStage(Window peerWindow, StageStyle stageStyle, boolean primary,
                                 Modality modality, TKStage owner, boolean rtl, boolean darkFrame) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TKStage createTKPopupStage(Window peerWindow, StageStyle popupStyle, TKStage owner) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TKStage createTKEmbeddedStage(HostInterface host) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TKSystemMenu getSystemMenu() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ImageLoader loadImage(String url, double width, double height, boolean preserveRatio, boolean smooth) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ImageLoader loadImage(InputStream stream, double width, double height, boolean preserveRatio, boolean smooth) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AsyncOperation loadImageAsync(AsyncOperationListener<ImageLoader> listener, String url, double width, double height, boolean preserveRatio, boolean smooth) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AsyncOperation loadImageAsync(AsyncOperationListener<ImageLoader> listener, InputStream stream, double width, double height, boolean preserveRatio, boolean smooth) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ImageLoader loadPlatformImage(Object platformImage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PlatformImage createPlatformImage(int w, int h) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startup(Runnable runnable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void defer(Runnable runnable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Future addRenderJob(RenderJob rj) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<Object, Object> getContextMap() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getRefreshRate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAnimationRunnable(DelayedRunnable animationRunnable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PerformanceTracker getPerformanceTracker() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override public PerformanceTracker createPerformanceTracker() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void waitFor(Task t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object createColorPaint(Color paint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object createLinearGradientPaint(LinearGradient paint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object createRadialGradientPaint(RadialGradient paint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object createImagePatternPaint(ImagePattern paint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void accumulateStrokeBounds(Shape shape, float[] bbox, StrokeType type, double strokewidth, StrokeLineCap cap, StrokeLineJoin join, float miterLimit, BaseTransform tx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean strokeContains(Shape shape, double x, double y, StrokeType type, double strokewidth, StrokeLineCap cap, StrokeLineJoin join, float miterLimit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Shape createStrokedShape(Shape shape, StrokeType pgtype, double strokewidth, StrokeLineCap pgcap, StrokeLineJoin pgjoin, float miterLimit, float[] dashArray, float dashOffset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getKeyCodeForChar(String character, int hint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Dimension2D getBestCursorSize(int preferredWidth, int preferredHeight) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMaximumCursorColors() {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public PathElement[] convertShapeToFXPath(Object shape) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Filterable toFilterable(Image img) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FilterContext getFilterContext(Object config) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isForwardTraversalKey(KeyEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isBackwardTraversalKey(KeyEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isNestedLoopRunning() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AbstractPrimaryTimer getPrimaryTimer() {
        return null;
    }

    @Override
    public FontLoader getFontLoader() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TextLayoutFactory getTextLayoutFactory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object createSVGPathObject(SVGPath svgpath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Path2D createSVGPath2D(SVGPath svgpath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean imageContains(Object image, float x, float y) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public com.sun.javafx.tk.TKClipboard getSystemClipboard() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TKClipboard getNamedClipboard(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ScreenConfigurationAccessor setScreenConfigurationListener(TKScreenConfigurationListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getPrimaryScreen() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<?> getScreens() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ScreenConfigurationAccessor getScreenConfigurationAccessor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void registerDragGestureListener(TKScene s, Set<TransferMode> tms, TKDragGestureListener l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startDrag(TKScene scene, Set<TransferMode> tms, TKDragSourceListener l, Dragboard dragboard) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void enableDrop(TKScene s, TKDropTargetListener l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void installInputMethodRequests(TKScene scene, InputMethodRequests requests) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object renderToImage(ImageRenderingContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public KeyCode getPlatformShortcutKey() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Optional<Boolean> isKeyLocked(KeyCode keyCode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileChooserResult showFileChooser(TKStage ownerWindow,
                                      String title,
                                      File initialDirectory,
                                      String initialFileName,
                                      FileChooserType fileChooserType,
                                      List<ExtensionFilter> extensionFilters,
                                      ExtensionFilter selectedFilter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public File showDirectoryChooser(TKStage ownerWindow,
                                     String title,
                                     File initialDirectory) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getMultiClickTime() {
        return 0L;
    }

    @Override
    public int getMultiClickMaxX() {
        return 0;
    }

    @Override
    public int getMultiClickMaxY() {
        return 0;
    }

    @Override
    public void requestNextPulse() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GlassRobot createRobot() {
        throw new UnsupportedOperationException("not implemented");
    }
}
