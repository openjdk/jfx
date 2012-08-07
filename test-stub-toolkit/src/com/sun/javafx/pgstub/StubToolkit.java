/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
/*
 * StubToolkit.java
 */

package com.sun.javafx.pgstub;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.geometry.Dimension2D;
import javafx.scene.image.Image;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.geom.ParallelCameraImpl;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PerspectiveCameraImpl;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.menu.MenuBase;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.runtime.async.AsyncOperation;
import com.sun.javafx.runtime.async.AsyncOperationListener;
import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.sg.PGArc;
import com.sun.javafx.sg.PGCanvas;
import com.sun.javafx.sg.PGCircle;
import com.sun.javafx.sg.PGCubicCurve;
import com.sun.javafx.sg.PGEllipse;
import com.sun.javafx.sg.PGGroup;
import com.sun.javafx.sg.PGImageView;
import com.sun.javafx.sg.PGLine;
import com.sun.javafx.sg.PGMediaView;
import com.sun.javafx.sg.PGPath;
import com.sun.javafx.sg.PGPolygon;
import com.sun.javafx.sg.PGPolyline;
import com.sun.javafx.sg.PGQuadCurve;
import com.sun.javafx.sg.PGRectangle;
import com.sun.javafx.sg.PGRegion;
import com.sun.javafx.sg.PGSVGPath;
import com.sun.javafx.sg.PGShape.StrokeLineCap;
import com.sun.javafx.sg.PGShape.StrokeLineJoin;
import com.sun.javafx.sg.PGShape.StrokeType;
import com.sun.javafx.sg.PGText;
import com.sun.javafx.tk.FileChooserType;
import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.ImageLoader;
import com.sun.javafx.tk.PlatformImage;
import com.sun.javafx.tk.ScreenConfigurationAccessor;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.TKDragGestureListener;
import com.sun.javafx.tk.TKDragSourceListener;
import com.sun.javafx.tk.TKDropTargetListener;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKScreenConfigurationListener;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.TKSystemMenu;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.BasicStroke;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractMasterTimer;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import javafx.scene.shape.FillRule;
import javafx.util.Pair;

/**
 * A Toolkit implementation for use with Testing.
 *
 * @author Richard
 */
public class StubToolkit extends Toolkit {

    private Map<Object, Object> contextMap = new HashMap<Object, Object>();

    private StubMasterTimer masterTimer = new StubMasterTimer();

    private PerformanceTracker performanceTracker = new StubPerformanceTracker();

    private final StubImageLoaderFactory imageLoaderFactory =
            new StubImageLoaderFactory();

    private CursorSizeConverter cursorSizeConverter =
            CursorSizeConverter.NO_CURSOR_SUPPORT;

    private int maximumCursorColors = 2;

    private TKScreenConfigurationListener screenConfigurationListener;
    
    private static final ScreenConfiguration[] DEFAULT_SCREEN_CONFIG = {
                new ScreenConfiguration(0, 0, 1920, 1200, 0, 0, 1920, 1172, 96)
            };
            
    private ScreenConfiguration[] screenConfigurations = DEFAULT_SCREEN_CONFIG;

    static {
        try {
            // ugly hack to initialize "runLater" method in Platform.java
            PlatformImpl.startup(new Runnable() { public void run() {}});
        } catch (Exception ex) {}

        // allow tests to access PG scenegraph
        // so that they can run with assertion enabled
        javafx.scene.Scene.impl_setAllowPGAccess(true);
    }
    private boolean pulseRequested;

    /*
     * overrides of Toolkit's abstract functions
     */

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public TKStage createTKStage(StageStyle stageStyle) {
        return new StubStage();
    }

    public TKStage createTKStage(StageStyle stageStyle, boolean primary,
            Modality modality, TKStage owner) {

        return new StubStage();
    }

    @Override
    public TKStage createTKPopupStage(StageStyle stageStyle, Object owner) {
        return new StubPopupStage();
    }

    @Override
    public TKStage createTKEmbeddedStage(HostInterface host) {
        return new StubStage();
    }

    @Override
    public TKSystemMenu getSystemMenu() {
        return new StubSystemMenu();
    }

    @Override
    public void startup(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void checkFxUserThread() {
        // Do nothing
    }

    @Override
    public void defer(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public Map<Object, Object> getContextMap() {
        return contextMap;
    }

    @Override public int getRefreshRate() {
        return -1;
    }

    private DelayedRunnable animationRunnable;

    @Override
    public void setAnimationRunnable(DelayedRunnable animationRunnable) {
        this.animationRunnable = animationRunnable;
    }

    @Override
    public PerformanceTracker getPerformanceTracker() {
        return performanceTracker;
    }

    @Override public PerformanceTracker createPerformanceTracker() {
        return new StubPerformanceTracker();
    }

    @Override
    protected Object createColorPaint(Color paint) {
        StubColor c = new StubColor(paint.getRed(),
                    paint.getGreen(),
                    paint.getBlue(),
                    paint.getOpacity());
        return c;
    }

    @Override
    protected Object createLinearGradientPaint(LinearGradient paint) {
        return new StubPaint();
    }

    @Override
    protected Object createRadialGradientPaint(RadialGradient paint) {
        return new StubPaint();
    }

    @Override
    protected Object createImagePatternPaint(ImagePattern paint) {
        return new StubPaint();
    }

    static BasicStroke tmpStroke = new BasicStroke();
    void initStroke(StrokeType pgtype, double strokewidth,
                    StrokeLineCap pgcap,
                    StrokeLineJoin pgjoin, float miterLimit)
    {
        int type;
        if (pgtype == StrokeType.CENTERED) {
            type = BasicStroke.TYPE_CENTERED;
        } else if (pgtype == StrokeType.INSIDE) {
            type = BasicStroke.TYPE_INNER;
        } else {
            type = BasicStroke.TYPE_OUTER;
        }

        int cap;
        if (pgcap == StrokeLineCap.BUTT) {
            cap = BasicStroke.CAP_BUTT;
        } else if (pgcap == StrokeLineCap.SQUARE) {
            cap = BasicStroke.CAP_SQUARE;
        } else {
            cap = BasicStroke.CAP_ROUND;
        }

        int join;
        if (pgjoin == StrokeLineJoin.BEVEL) {
            join = BasicStroke.JOIN_BEVEL;
        } else if (pgjoin == StrokeLineJoin.MITER) {
            join = BasicStroke.JOIN_MITER;
        } else {
            join = BasicStroke.JOIN_ROUND;
        }

        tmpStroke.set(type, (float) strokewidth, cap, join, miterLimit);
    }

    @Override
    public void accumulateStrokeBounds(Shape shape, float bbox[],
                                       StrokeType pgtype,
                                       double strokewidth,
                                       StrokeLineCap pgcap,
                                       StrokeLineJoin pgjoin,
                                       float miterLimit,
                                       BaseTransform tx)
    {

        initStroke(pgtype, strokewidth, pgcap, pgjoin, miterLimit);
        // TODO: The accumulation could be done directly without creating a Shape
        Shape.accumulate(bbox, tmpStroke.createStrokedShape(shape), tx);
    }

    @Override
    public boolean strokeContains(Shape shape, double x, double y,
                                  StrokeType pgtype,
                                  double strokewidth,
                                  StrokeLineCap pgcap,
                                  StrokeLineJoin pgjoin,
                                  float miterLimit)
    {
        initStroke(pgtype, strokewidth, pgcap, pgjoin, miterLimit);
        // TODO: The contains testing could be done directly without creating a Shape
        return tmpStroke.createStrokedShape(shape).contains((float) x, (float) y);
    }

    @Override
    public Shape createStrokedShape(Shape shape,
                                    StrokeType pgtype,
                                    double strokewidth,
                                    StrokeLineCap pgcap,
                                    StrokeLineJoin pgjoin,
                                    float miterLimit,
                                    float[] dashArray,
                                    float dashOffset) {
        initStroke(pgtype, strokewidth, pgcap, pgjoin, miterLimit);
        return tmpStroke.createStrokedShape(shape);
    }

    public CursorSizeConverter getCursorSizeConverter() {
        return cursorSizeConverter;
    }

    public void setCursorSizeConverter(
            CursorSizeConverter cursorSizeConverter) {
        this.cursorSizeConverter = cursorSizeConverter;
    }

    @Override
    public Dimension2D getBestCursorSize(int preferredWidth, int preferredHeight) {
        return cursorSizeConverter.getBestCursorSize(preferredWidth,
                                                     preferredHeight);
    }

    @Override
    public int getMaximumCursorColors() {
        return maximumCursorColors;
    }

    public void setMaximumCursorColors(int maximumCursorColors) {
        this.maximumCursorColors = maximumCursorColors;
    }

    @Override
    public AbstractMasterTimer getMasterTimer() {
        return masterTimer;
    }

    @Override
    public FontLoader getFontLoader() {
        return new StubFontLoader();
    }

    @Override public PGArc createPGArc() {
        return new StubArc();
    }

    @Override public PGCircle createPGCircle() {
        return new StubCircle();
    }

    @Override public PGCubicCurve createPGCubicCurve() {
        return new StubCubicCurve();
    }

    @Override public PGEllipse createPGEllipse() {
        return new StubEllipse();
    }

    @Override public PGLine createPGLine() {
        return new StubLine();
    }

    @Override public PGPath createPGPath() {
        return new StubPath();
    }

    @Override public PGPolygon createPGPolygon() {
        return new StubPolygon();
    }

    @Override public PGPolyline createPGPolyline() {
        return new StubPolyline();
    }

    @Override public PGQuadCurve createPGQuadCurve() {
        return new StubQuadCurve();
    }

    @Override public PGRectangle createPGRectangle() {
        return new StubRectangle();
    }

    @Override public PGImageView createPGImageView() {
        return new StubImageView();
    }

    @Override public PGMediaView createPGMediaView() {
        return new StubMediaView();
    }

    @Override public PGGroup createPGGroup() {
        return new StubGroup();
    }

    @Override public PGText createPGText() {
        return new StubText();
    }

    /*
     * additional testing functions
     */
    public void fireTestPulse() {
        firePulse();
    }

   public boolean isPulseRequested() {
        return pulseRequested;
    }

    public void clearPulseRequested() {
        pulseRequested = false;
    }

    // do nothing -- bringing in FrameJob and MasterTimer also bring in
    // Settings and crap which isn't setup for the testing stuff because
    // we don't run through a RuntimeProvider or do normal startup
    // public @Override public void triggerNextPulse():Void { }
    @Override public void requestNextPulse() {
        pulseRequested = true;
    }

    private TKClipboard clipboard = new TKClipboard() {
        private Map<DataFormat, Object> map = new HashMap<DataFormat, Object>();
        @Override public Set<DataFormat> getContentTypes() {
            return map.keySet();
        }

        @Override public boolean putContent(Pair<DataFormat, Object>... content) {
            boolean good;
            for (Pair<DataFormat,Object> pair : content) {
                good = map.put(pair.getKey(), pair.getValue()) == pair.getValue();
                if (!good) return false;
            }
            return true;
        }

        @Override public Object getContent(DataFormat dataFormat) {
            return map.get(dataFormat);
        }

        @Override public boolean hasContent(DataFormat dataFormat) {
            return map.containsKey(dataFormat);
        }

        @Override public Set<TransferMode> getTransferModes() {
            Set<TransferMode> modes = new HashSet<TransferMode>();
            modes.add(TransferMode.COPY);
            return modes;
        }
    
        @Override public void initSecurityContext() {
        }
    };


    @Override
    public TKClipboard getSystemClipboard() {
        return clipboard;
    }

    @Override public TKClipboard getNamedClipboard(String name) {
        return null;
    }

    public static Dragboard createDragboard() {
        StubToolkit tk = (StubToolkit)Toolkit.getToolkit();
        if (tk.dndDelegate != null) {
            return tk.dndDelegate.createDragboard();
        }
        return null;
    }

    @Override
    public void enableDrop(TKScene s, TKDropTargetListener l) {
        if (dndDelegate != null) {
            dndDelegate.enableDrop(l);
        }
    }

    private ScreenConfigurationAccessor accessor = new ScreenConfigurationAccessor() {
        @Override
        public int getMinX(Object obj) {
            return ((ScreenConfiguration) obj).getMinX();
        }

        @Override
        public int getMinY(Object obj) {
            return ((ScreenConfiguration) obj).getMinY();
        }

        @Override
        public int getWidth(Object obj) {
            return ((ScreenConfiguration) obj).getWidth();
        }

        @Override
        public int getHeight(Object obj) {
            return ((ScreenConfiguration) obj).getHeight();
        }

        @Override
        public int getVisualMinX(Object obj) {
            return ((ScreenConfiguration) obj).getVisualMinX();
        }

        @Override
        public int getVisualMinY(Object obj) {
            return ((ScreenConfiguration) obj).getVisualMinY();
        }

        @Override
        public int getVisualWidth(Object obj) {
            return ((ScreenConfiguration) obj).getVisualWidth();
        }

        @Override
        public int getVisualHeight(Object obj) {
            return ((ScreenConfiguration) obj).getVisualHeight();
        }

        @Override
        public float getDPI(Object obj) {
            return ((ScreenConfiguration) obj).getDPI();
        }
    };

    @Override
    public ScreenConfigurationAccessor setScreenConfigurationListener(
            TKScreenConfigurationListener listener) {
        screenConfigurationListener = listener;
        return accessor;
    }

    @Override
    public ScreenConfiguration getPrimaryScreen() {
        return screenConfigurations[0];
    }

    public void setScreens(ScreenConfiguration... screenConfigurations) {
        this.screenConfigurations = screenConfigurations.clone();
        if (screenConfigurationListener != null) {
            screenConfigurationListener.screenConfigurationChanged();
        }
    }

    public void resetScreens() {
        setScreens(DEFAULT_SCREEN_CONFIG);
    }

    @Override
    public List<ScreenConfiguration> getScreens() {
        return Arrays.asList(screenConfigurations);
    }

    @Override public void registerDragGestureListener(TKScene s, Set<TransferMode> tm, TKDragGestureListener l) {
        if (dndDelegate != null) {
            dndDelegate.registerListener(l);
        }
    }

    @Override public void startDrag(TKScene scene, Set<TransferMode> tm, TKDragSourceListener l, Dragboard dragboard) {
        if (dndDelegate != null) {
            dndDelegate.startDrag(scene, tm, l, dragboard);
        }
    }

    @Override
    public ImageLoader loadImage(String url, int width, int height,
            boolean preserveRatio, boolean smooth) {
        return imageLoaderFactory.createImageLoader(url, width, height,
                                                    preserveRatio, smooth);
    }

    @Override
    public ImageLoader loadImage(InputStream stream, int width, int height,
            boolean preserveRatio, boolean smooth) {
        return imageLoaderFactory.createImageLoader(stream, width, height,
                                                    preserveRatio, smooth);
    }

    @Override
    public AsyncOperation loadImageAsync(
            AsyncOperationListener listener, String url, int width, int height,
            boolean preserveRatio, boolean smooth) {
        return imageLoaderFactory.createAsyncImageLoader(
                listener, url, width, height, preserveRatio, smooth);
    }

    @Override
    public ImageLoader loadPlatformImage(Object platformImage) {
        return imageLoaderFactory.createImageLoader(platformImage,
                                                    0, 0, false, false);
    }

    @Override
    public PlatformImage createPlatformImage(int w, int h) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void waitFor(Task t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getKeyCodeForChar(String character) {
        if (charToKeyCodeMap != null) {
            final KeyCode keyCode = charToKeyCodeMap.get(character);
            if (keyCode != null) {
                return keyCode.impl_getCode();
            }
        }

        return 0;
    }

    @Override
    public PathElement[] convertShapeToFXPath(Object shape) {
        // Had to be mocked up for TextField tests (for the caret!)
        // Since the "shape" could be anything, I'm just returning
        // something here, doesn't matter what.
        return new PathElement[0];
    }

    @Override
    public HitInfo convertHitInfoToFX(Object hit) {
        return (HitInfo) hit;
    }

    @Override
    public Filterable toFilterable(Image img) {
        return StubFilterable.create((StubPlatformImage)img.impl_getPlatformImage());
    }

    @Override
    public FilterContext getFilterContext(Object config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isForwardTraversalKey(KeyEvent e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBackwardTraversalKey(KeyEvent e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PGSVGPath createPGSVGPath() {
        return new StubSVGPath();
    }

    @Override
    public PGRegion createPGRegion() {
        return new StubRegion();
    }

    @Override
    public Object createSVGPathObject(SVGPath svgpath) {
        int windingRule = (svgpath.getFillRule() == FillRule.NON_ZERO) ?
                           Path2D.WIND_NON_ZERO : Path2D.WIND_EVEN_ODD;

        return new StubSVGPath.SVGPathImpl(svgpath.getContent(), windingRule);
    }

    @Override
    public Path2D createSVGPath2D(SVGPath svgpath) {
        int windingRule = (svgpath.getFillRule() == FillRule.NON_ZERO) ?
                           Path2D.WIND_NON_ZERO : Path2D.WIND_EVEN_ODD;

        return new StubSVGPath.SVGPathImpl(svgpath.getContent(), windingRule);
    }

    @Override
    public boolean imageContains(Object image, float x, float y) {
        return ((StubPlatformImage) image).getImageInfo()
                                          .contains((int) x, (int) y);
    }

    public void setCurrentTime(long millis) {
        masterTimer.setCurrentTime(millis);
    }

    public void handleAnimation() {
        if (animationRunnable != null) {
            try {
                animationRunnable.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public StubImageLoaderFactory getImageLoaderFactory() {
        return imageLoaderFactory;
    }

    public void setAnimationTime(final long millis) {
        setCurrentTime(millis);
        handleAnimation();
        fireTestPulse();
    }

    @Override
    public PerspectiveCameraImpl createPerspectiveCamera() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ParallelCameraImpl createParallelCamera() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void installInputMethodRequests(TKScene scene, InputMethodRequests requests) {
        // just do nothing here.
    }

    private Map<String, KeyCode> charToKeyCodeMap;

    public void setCharToKeyCodeMap(Map<String, KeyCode> charToKeyCodeMap) {
        this.charToKeyCodeMap = charToKeyCodeMap;
    }

    @Override
    public Object renderToImage(ImageRenderingContext context) {
        throw new UnsupportedOperationException();
    }

    public Class externalFormatClass = StubToolkit.class;

    @Override
    public boolean isExternalFormatSupported(Class type) {
        return externalFormatClass == type;
    }

    public Object toExternalImagePassword = "toExternalImage";
    @Override
    public Object toExternalImage(Object o, Object o1) {
        return new Object [] { o, o1, toExternalImagePassword };
    }

    @Override public Object enterNestedEventLoop(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override public void exitNestedEventLoop(Object key, Object rval) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private KeyCode platformShortcutKey = KeyCode.SHORTCUT;

    public void setPlatformShortcutKey(final KeyCode platformShortcutKey) {
        this.platformShortcutKey = platformShortcutKey;
    }

    public KeyCode getPlatformShortcutKey() {
        return platformShortcutKey;
    }

    private DndDelegate dndDelegate;
    public void setDndDelegate(DndDelegate dndDelegate) {
        this.dndDelegate = dndDelegate;
    }

    @Override
    public PGCanvas createPGCanvas() {
        return new StubCanvas();
    }

    public interface DndDelegate {
        void startDrag(TKScene scene, Set<TransferMode> tm,
                TKDragSourceListener l, Dragboard dragboard);

        Dragboard createDragboard();

        DragEvent convertDragEventToFx(Object event, Dragboard dragboard);

        void registerListener(TKDragGestureListener l);

        void enableDrop(TKDropTargetListener l);
    }

    @Override
    public List<File> showFileChooser(TKStage ownerWindow,
                                      String title,
                                      File initialDirectory,
                                      FileChooserType fileChooserType,
                                      List<ExtensionFilter> extensionFilters) {
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
        return 500L;
    }

    @Override
    public int getMultiClickMaxX() {
        return 5;
    }

    @Override
    public int getMultiClickMaxY() {
        return 5;
    }

    public static final class ScreenConfiguration {
        private final int minX;
        private final int minY;
        private final int width;
        private final int height;
        private final int visualMinX;
        private final int visualMinY;
        private final int visualWidth;
        private final int visualHeight;
        private final float dpi;

        public ScreenConfiguration(final int minX, final int minY,
                                   final int width, final int height,
                                   final int visualMinX,
                                   final int visualMinY,
                                   final int visualWidth,
                                   final int visualHeight,
                                   final float dpi) {
            this.minX = minX;
            this.minY = minY;
            this.width = width;
            this.height = height;
            this.visualMinX = visualMinX;
            this.visualMinY = visualMinY;
            this.visualWidth = visualWidth;
            this.visualHeight = visualHeight;
            this.dpi = dpi;
        }

        public int getMinX() {
            return minX;
        }

        public int getMinY() {
            return minY;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getVisualMinX() {
            return visualMinX;
        }

        public int getVisualMinY() {
            return visualMinY;
        }

        public int getVisualWidth() {
            return visualWidth;
        }

        public int getVisualHeight() {
            return visualHeight;
        }

        public float getDPI() {
            return dpi;
        }
    }
    
    public static class StubSystemMenu implements TKSystemMenu {

        @Override
        public boolean isSupported() {
            return false;
        }

        @Override
        public void setMenus(List<MenuBase> menus) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
