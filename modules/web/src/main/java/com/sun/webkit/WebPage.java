/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

import com.sun.glass.utils.NativeLibLoader;
import com.sun.webkit.event.WCFocusEvent;
import com.sun.webkit.event.WCInputMethodEvent;
import com.sun.webkit.event.WCKeyEvent;
import com.sun.webkit.event.WCMouseEvent;
import com.sun.webkit.event.WCMouseWheelEvent;
import com.sun.webkit.graphics.*;
import com.sun.webkit.network.CookieManager;
import static com.sun.webkit.network.URLs.newURL;
import java.net.CookieHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import netscape.javascript.JSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class provides two-side interaction between native webkit core and
 * number of clients representing different subsystems of the WebPane component
 * such as
 * <ul>
 * <li>webpage rendering({@link WebPageClient})
 * <li>creating/disposing web frames ({@link WebFrameClient})
 * <li>creating new windows, alert dialogues ... ({@link UIClient})
 * <li>handling menus {@link MenuClient}
 * <li>supporting policy checking {@link PolicyClient}
 * </ul>
 */

public final class WebPage {
    private final static Logger log = Logger.getLogger(WebPage.class.getName());
    private final static Logger paintLog = Logger.getLogger(WebPage.class.getName() + ".paint");

    private static final int MAX_FRAME_QUEUE_SIZE = 10;

    // Native WebPage* pointer
    private long pPage = 0;

    // A flag to distinguish whether the web page hasn't been created
    // yet or had been already disposed - in both cases pPage is 0
    private boolean isDisposed = false;

    private int width, height;

    private int fontSmoothingType;

    private final WCFrameView hostWindow;

    // List of created frames
    private final Set<Long> frames = new HashSet<Long>();

    // The access control context associated with this object
    private final AccessControlContext accessControlContext;

    // Maps load request identifiers to URLs
    private final Map<Integer, String> requestURLs =
            new HashMap<Integer, String>();

    // There may be several RESOURCE_STARTED events for a resource,
    // so this map is used to convert them to RESOURCE_REDIRECTED
    private final Set<Integer> requestStarted = new HashSet<Integer>();

    // PAGE_LOCK is used to synchronize the following operations b/w Event & Main threads:
    // - rendering of the page (Main thread)
    // - native calls & other manipulations on the page (Event & Main threads)
    // - timer invocations (Event thread)
    private static final ReentrantLock PAGE_LOCK = new ReentrantLock();

    // The queue of render frames awaiting rendering.
    // Access to this object is synchronized on its monitor.
    // Accessed on: Event thread and Main thread.
    private final Queue<RenderFrame> frameQueue = new LinkedList<RenderFrame>();

    // The current frame being generated.
    // Accessed on: Event thread only.
    private RenderFrame currentFrame = new RenderFrame();

    static {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            NativeLibLoader.loadLibrary("jfxwebkit");
            log.finer("jfxwebkit loaded");

            if (CookieHandler.getDefault() == null) {
                boolean setDefault = Boolean.valueOf(System.getProperty(
                        "com.sun.webkit.setDefaultCookieHandler",
                        "true"));
                if (setDefault) {
                    CookieHandler.setDefault(new CookieManager());
                }
            }
            return null;
        });
    }

    public WebPage(WebPageClient pageClient,
                   UIClient uiClient,
                   PolicyClient policyClient,
                   InspectorClient inspectorClient,
                   ThemeClient themeClient,
                   boolean editable)
    {
        Invoker.getInvoker().checkEventThread();

        this.pageClient = pageClient;
        this.uiClient = uiClient;
        this.policyClient = policyClient;
        this.inspectorClient = inspectorClient;
        if (themeClient != null) {
            this.renderTheme = themeClient.createRenderTheme();
            this.scrollbarTheme = themeClient.createScrollBarTheme();
        } else {
            this.renderTheme = null;
            this.scrollbarTheme = null;
        }

        accessControlContext = AccessController.getContext();

        hostWindow = new WCFrameView(this);
        pPage = twkCreatePage(editable);

        twkInit(pPage, false);

        if (pageClient != null && pageClient.isBackBufferSupported()) {
            backbuffer = pageClient.createBackBuffer();
            backbuffer.ref();
        }
    }

    long getPage() {
        return pPage;
    }

    // Called from the native code
    private WCWidget getHostWindow() {
        return hostWindow;
    }

    /**
     * Returns the access control context associated with this object.
     * May be called on any thread.
     * @return the access control context associated with this object
     */
    public AccessControlContext getAccessControlContext() {
        return accessControlContext;
    }

    static boolean lockPage() {
        return Invoker.getInvoker().lock(PAGE_LOCK);
    }

    static boolean unlockPage() {
        return Invoker.getInvoker().unlock(PAGE_LOCK);
    }

    // *************************************************************************
    // Backbuffer support
    // *************************************************************************

    private WCPageBackBuffer backbuffer;
    private List<WCRectangle> dirtyRects = new LinkedList<WCRectangle>();

    private void addDirtyRect(WCRectangle toPaint) {
        for (Iterator<WCRectangle> it = dirtyRects.iterator(); it.hasNext();) {
            WCRectangle rect = it.next();
            // if already covered
            if (rect.contains(toPaint)) {
                return;
            }
            // if covers an existing one
            if (toPaint.contains(rect)) {
                it.remove();
                continue;
            }
            WCRectangle u = rect.createUnion(toPaint);
            // if squre of union is less than summary of squares
            if (u.getIntWidth() * u.getIntHeight() <
                rect.getIntWidth() * rect.getIntHeight() +
                toPaint.getIntWidth() * toPaint.getIntHeight())
            {
                it.remove();
                toPaint = u; // replace both the rects with their union
                continue;
            }
        }
        dirtyRects.add(toPaint);
    }

    public boolean isDirty() {
        lockPage();
        try {
            return !dirtyRects.isEmpty();
        } finally {
            unlockPage();
        }
    }

    private void updateDirty(WCRectangle clip) {
        if (paintLog.isLoggable(Level.FINEST)) {
            paintLog.log(Level.FINEST, "Entering, "
                    + "dirtyRects: {0}, currentFrame: {1}",
                    new Object[] {dirtyRects, currentFrame});
        }

        if (isDisposed || width <= 0 || height <= 0) {
            // If there're any dirty rects left, they are invalid.
            // Clear the list so that the platform doesn't consider
            // the page dirty.
            dirtyRects.clear();
            return;
        }
        if (clip == null) {
            clip = new WCRectangle(0, 0, width, height);
        }
        List<WCRectangle> oldDirtyRects = dirtyRects;
        dirtyRects = new LinkedList<WCRectangle>();
        twkPrePaint(getPage());
        while (!oldDirtyRects.isEmpty()) {
            WCRectangle r = oldDirtyRects.remove(0).intersection(clip);
            if (r.getWidth() <= 0 || r.getHeight() <= 0) {
                continue;
            }
            paintLog.log(Level.FINEST, "Updating: {0}", r);
            WCRenderQueue rq = WCGraphicsManager.getGraphicsManager()
                    .createRenderQueue(r, true);
            twkUpdateContent(getPage(), rq, r.getIntX() - 1, r.getIntY() - 1,
                             r.getIntWidth() + 2, r.getIntHeight() + 2);
            currentFrame.addRenderQueue(rq);
        }
        {
            WCRenderQueue rq = WCGraphicsManager.getGraphicsManager()
                    .createRenderQueue(clip, false);
            twkPostPaint(getPage(), rq,
                         clip.getIntX(), clip.getIntY(),
                         clip.getIntWidth(), clip.getIntHeight());
            currentFrame.addRenderQueue(rq);
        }

        if (paintLog.isLoggable(Level.FINEST)) {
            paintLog.log(Level.FINEST, "Dirty rects processed, "
                    + "dirtyRects: {0}, currentFrame: {1}",
                    new Object[] {dirtyRects, currentFrame});
        }

        if (currentFrame.getRQList().size() > 0) {
            synchronized (frameQueue) {
                paintLog.log(Level.FINEST, "About to update frame queue, "
                        + "frameQueue: {0}", frameQueue);

                Iterator<RenderFrame> it = frameQueue.iterator();
                while (it.hasNext()) {
                    RenderFrame frame = it.next();
                    for (WCRenderQueue rq : currentFrame.getRQList()) {
                        WCRectangle rqRect = rq.getClip();
                        if (rq.isOpaque()
                                && rqRect.contains(frame.getEnclosingRect()))
                        {
                            paintLog.log(Level.FINEST, "Dropping: {0}", frame);
                            frame.drop();
                            it.remove();
                            break;
                        }
                    }
                }

                frameQueue.add(currentFrame);
                currentFrame = new RenderFrame();

                if (frameQueue.size() > MAX_FRAME_QUEUE_SIZE) {
                    paintLog.log(Level.FINEST, "Frame queue exceeded maximum "
                            + "size, clearing and requesting full repaint");
                    dropRenderFrames();
                    repaintAll();
                }

                paintLog.log(Level.FINEST, "Frame queue updated, "
                        + "frameQueue: {0}", frameQueue);
            }
        }

        if (paintLog.isLoggable(Level.FINEST)) {
            paintLog.log(Level.FINEST,
                    "Exiting, dirtyRects: {0}, currentFrame: {1}",
                    new Object[] {dirtyRects, currentFrame});
        }
    }

    private void scroll(int x, int y, int w, int h, int dx, int dy) {
        if (paintLog.isLoggable(Level.FINEST)) {
            paintLog.finest("rect=[" + x + ", " + y + " " + w + "x" + h +
                            "] delta=[" + dx + ", " + dy + "]");
        }
        if (Math.abs(dx) < w && Math.abs(dy) < h) {
            int cx = (dx >= 0) ? x : x - dx;
            int cy = (dy >= 0) ? y : y - dy;
            int cw = (dx == 0) ? w : w - Math.abs(dx);
            int ch = (dy == 0) ? h : h - Math.abs(dy);

            WCRenderQueue rq = WCGraphicsManager.getGraphicsManager()
                    .createRenderQueue(
                            new WCRectangle(0, 0, width, height), false);
            ByteBuffer buffer = ByteBuffer.allocate(32)
                    .order(ByteOrder.nativeOrder())
                    .putInt(GraphicsDecoder.COPYREGION)
                    .putInt(backbuffer.getID())
                    .putInt(cx).putInt(cy).putInt(cw).putInt(ch)
                    .putInt(dx).putInt(dy);
            buffer.flip();
            rq.addBuffer(buffer);
            currentFrame.addRenderQueue(rq);

            // Now we have to translate "old" dirty rects that fit to the frame's
            // content as the content is already scrolled at the moment by webkit.
            if (!dirtyRects.isEmpty()) {
                WCRectangle scrollRect = new WCRectangle(x, y, w, h);

                for (WCRectangle r: dirtyRects) {
                    if (scrollRect.contains(r)) {
                        if (paintLog.isLoggable(Level.FINEST)) {
                            paintLog.log(Level.FINEST, "translating old dirty rect by the delta: " + r);
                        }
                        r.translate(dx, dy);
                    }
                }
            }
        }

        // Add the dirty (not copied) rect
        addDirtyRect(new WCRectangle(dx >= 0 ? x : x + w + dx,
                                     dy >= 0 ? y : y + h + dy,
                                     dx == 0 ? w : Math.abs(dx),
                                     dy == 0 ? h : Math.abs(dy)));
    }

    // Instances of this class may not be accessed and modified concurrently
    // by multiple threads
    private static final class RenderFrame {
        private final List<WCRenderQueue> rqList =
                new LinkedList<WCRenderQueue>();
        private final WCRectangle enclosingRect = new WCRectangle();

        // Called on: Event thread only
        private void addRenderQueue(WCRenderQueue rq) {
            if (rq.isEmpty()) {
                return;
            }
            rqList.add(rq);
            WCRectangle rqRect = rq.getClip();
            if (enclosingRect.isEmpty()) {
                enclosingRect.setFrame(rqRect.getX(), rqRect.getY(),
                                       rqRect.getWidth(), rqRect.getHeight());
            } else if (rqRect.isEmpty()) {
                // do nothing
            } else {
                WCRectangle.union(enclosingRect, rqRect, enclosingRect);
            }
        }

        // Called on: Event thread and Main thread
        private List<WCRenderQueue> getRQList() {
            return rqList;
        }

        // Called on: Event thread only
        private WCRectangle getEnclosingRect() {
            return enclosingRect;
        }

        // Called on: Event thread only
        private void drop() {
            for (WCRenderQueue rq : rqList) {
                rq.dispose();
            }
            rqList.clear();
            enclosingRect.setFrame(0, 0, 0, 0);
        }

        @Override
        public String toString() {
            return "RenderFrame{"
                    + "rqList=" + rqList + ", "
                    + "enclosingRect=" + enclosingRect
                    + "}";
        }
    }

    // *************************************************************************
    // Callback API
    // *************************************************************************

    private final WebPageClient pageClient;
    private final UIClient uiClient;
    private final PolicyClient policyClient;
    private InputMethodClient imClient;
    private final List<LoadListenerClient> loadListenerClients =
        new LinkedList<LoadListenerClient>();
    private final InspectorClient inspectorClient;
    private final RenderTheme renderTheme;
    private final ScrollBarTheme scrollbarTheme;

    public WebPageClient getPageClient() {
        return pageClient;
    }

    public void setInputMethodClient(InputMethodClient imClient) {
        this.imClient = imClient;
    }

    public void setInputMethodState(boolean state) {
        if (imClient != null) {
            // A web page containing multiple clients is a single client from Java
            // Input Method Framework's viewpoint. We need to control activation and
            // deactivation for each text field/area here. Also, we need to control
            // enabling and disabling input methods here so that input method events
            // won't get delivered to wrong places (e.g., background).
            imClient.activateInputMethods(state);
        }
    }

    public void addLoadListenerClient(LoadListenerClient l) {
        if (!loadListenerClients.contains(l)) {
            loadListenerClients.add(l);
        }
    }

    private RenderTheme getRenderTheme() {
        return renderTheme;
    }

    private static RenderTheme fwkGetDefaultRenderTheme() {
        return ThemeClient.getDefaultRenderTheme();
    }

    private ScrollBarTheme getScrollBarTheme() {
        return scrollbarTheme;
    }

    // *************************************************************************
    // UI stuff API
    // *************************************************************************

    public void setBounds(int x, int y, int w, int h) {
        lockPage();
        try {
            log.log(Level.FINE, "setBounds: " + x + " " + y + " " + w + " " + h);
            if (isDisposed) {
                log.log(Level.FINE, "setBounds() request for a disposed web page.");
                return;
            }
            width = w;
            height = h;
            twkSetBounds(getPage(), 0, 0, w, h);
            // In response to the above call, WebKit will issue many
            // repaint requests, one of which will be meant to invalidate
            // the entire visible area. However, if the current scroll
            // offset is non-zero, that repaint request will contain
            // incorrect coordinates.
            // As of time of writing this, this problem exists in both
            // MiniBrowser and WinLauncher.
            // MiniBrowser is based on WebKit2, and WebKit2 workarounds
            // this problem by calling m_drawingArea->setNeedsDisplay()
            // for the entire visible area from within the WebKit2's
            // WebPage::setSize().
            // WinLauncher workarounds this problem by setting the main
            // window class style to CS_HREDRAW | CS_VREDRAW and calling
            // MoveWindow() with bRepaint = TRUE when resizing the web
            // view.
            // We workaround this problem by invalidating the entire
            // visible area here.
            repaintAll();

        } finally {
            unlockPage();
        }
    }

    public void setOpaque(long frameID, boolean isOpaque) {
        lockPage();
        try {
            log.log(Level.FINE, "setOpaque: " + isOpaque);
            if (isDisposed) {
                log.log(Level.FINE, "setOpaque() request for a disposed web page.");
                return;
            }
            if (!frames.contains(frameID)) {
                return;
            }
            twkSetTransparent(frameID, !isOpaque);

        } finally {
            unlockPage();
        }
    }

    public void setBackgroundColor(long frameID, int backgroundColor) {
        lockPage();
        try {
            log.log(Level.FINE, "setBackgroundColor: " + backgroundColor);
            if (isDisposed) {
                log.log(Level.FINE, "setBackgroundColor() request for a disposed web page.");
                return;
            }
            if (!frames.contains(frameID)) {
                return;
            }
            twkSetBackgroundColor(frameID, backgroundColor);

        } finally {
            unlockPage();
        }
    }

    public void setBackgroundColor(int backgroundColor) {
        lockPage();
        try {
            log.log(Level.FINE, "setBackgroundColor: " + backgroundColor +
                   " for all frames");
            if (isDisposed) {
                log.log(Level.FINE, "setBackgroundColor() request for a disposed web page.");
                return;
            }

            for (long frameID: frames) {
                twkSetBackgroundColor(frameID, backgroundColor);
            }

        } finally {
            unlockPage();
        }
    }

    /*
     * Executed on the Event Thread.
     */
    public void updateContent(WCRectangle toPaint) {
        lockPage();
        try {
            paintLog.log(Level.FINEST, "toPaint: {0}", toPaint);
            if (isDisposed) {
                paintLog.fine("updateContent() request for a disposed web page.");
                return;
            }
            updateDirty(toPaint);

        } finally {
            unlockPage();
        }
    }

    public boolean isRepaintPending() {
        lockPage();
        try {
            synchronized (frameQueue) {
                return !frameQueue.isEmpty();
            }
        } finally {
            unlockPage();
        }
    }

    /*
     * Executed on printing thread.
     */
    public void print(WCGraphicsContext gc,
            final int x, final int y, final int w, final int h)
    {
        lockPage();
        try {
            final WCRenderQueue rq = WCGraphicsManager.getGraphicsManager().
                    createRenderQueue(new WCRectangle(x, y, w, h), true);
            FutureTask<Void> f = new FutureTask<Void>(() -> {
                twkUpdateContent(getPage(), rq, x, y, w, h);
            }, null);
            Invoker.getInvoker().invokeOnEventThread(f);
            
            try {
                // block until job is complete
                f.get();
            } catch (ExecutionException ex) {
                throw new AssertionError(ex);
            } catch (InterruptedException ex) {
                // ignore; recovery is impossible
            }

            rq.decode(gc);
        } finally {
            unlockPage();
        }
    }

    /*
     * Executed on the Render Thread.
     */
    public void paint(WCGraphicsContext gc, int x, int y, int w, int h) {
        lockPage();
        try {
            if (pageClient != null && pageClient.isBackBufferSupported()) {
                if (!backbuffer.validate(width, height)) {
                    // We need to repaint the whole page on the next turn
                    Invoker.getInvoker().invokeOnEventThread(() -> {
                        repaintAll();
                    });
                    return;
                }
                WCGraphicsContext bgc = backbuffer.createGraphics();
                try {
                    paint2GC(bgc);
                } finally {
                    backbuffer.disposeGraphics(bgc);
                }
                backbuffer.flush(gc, x, y, w, h);
            } else {
                paint2GC(gc);
            }
        } finally {
            unlockPage();
        }
    }

    private void paint2GC(WCGraphicsContext gc) {
        paintLog.finest("Entering");
        gc.setFontSmoothingType(this.fontSmoothingType);

        List<RenderFrame> framesToRender;
        synchronized (frameQueue) {
            framesToRender = new ArrayList(frameQueue);
            frameQueue.clear();
        }

        paintLog.log(Level.FINEST, "Frames to render: {0}", framesToRender);

        for (RenderFrame frame : framesToRender) {
            paintLog.log(Level.FINEST, "Rendering: {0}", frame);
            for (WCRenderQueue rq : frame.getRQList()) {
                gc.saveState();
                if (rq.getClip() != null) {
                    gc.setClip(rq.getClip());
                }
                rq.decode(gc);
                gc.restoreState();
            }
        }
        paintLog.finest("Exiting");
    }

    /*
     * Executed on the Event Thread.
     */
    public void dropRenderFrames() {
        lockPage();
        try {
            currentFrame.drop();
            synchronized (frameQueue) {
                for (RenderFrame frame = frameQueue.poll(); frame != null; frame = frameQueue.poll()) {
                    frame.drop();
                }
            }
        } finally {
            unlockPage();
        }
    }

    public void dispatchFocusEvent(WCFocusEvent fe) {
        lockPage();
        try {
            log.log(Level.FINEST, "dispatchFocusEvent: " + fe);
            if (isDisposed) {
                log.log(Level.FINE, "Focus event for a disposed web page.");
                return;
            }
            twkProcessFocusEvent(getPage(), fe.getID(), fe.getDirection());

        } finally {
            unlockPage();
        }
    }

    public boolean dispatchKeyEvent(WCKeyEvent ke) {
        lockPage();
        try {
            log.log(Level.FINEST, "dispatchKeyEvent: " + ke);
            if (isDisposed) {
                log.log(Level.FINE, "Key event for a disposed web page.");
                return false;
            }
            if (WCKeyEvent.filterEvent(ke)) {
                log.log(Level.FINEST, "filtered");
                return false;
            }
            return twkProcessKeyEvent(getPage(), ke.getType(), ke.getText(),
                                      ke.getKeyIdentifier(),
                                      ke.getWindowsVirtualKeyCode(),
                                      ke.isShiftDown(), ke.isCtrlDown(),
                                      ke.isAltDown(), ke.isMetaDown());
        } finally {
            unlockPage();
        }
    }

    public boolean dispatchMouseEvent(WCMouseEvent me) {
        lockPage();
        try {
            log.log(Level.FINEST, "dispatchMouseEvent: " + me.getX() + "," + me.getY());
            if (isDisposed) {
                log.log(Level.FINE, "Mouse event for a disposed web page.");
                return false;
            }

            return !isDragConfirmed() //When Webkit informes FX about drag start, it waits
                                      //for system DnD loop and not intereasted in
                                      //intermediate mouse events that can change text selection.
                && twkProcessMouseEvent(getPage(), me.getID(),
                                        me.getButton(), me.getClickCount(),
                                        me.getX(), me.getY(), me.getScreenX(), me.getScreenY(),
                                        me.isShiftDown(), me.isControlDown(), me.isAltDown(), me.isMetaDown(), me.isPopupTrigger(),
                                        me.getWhen() / 1000.0f);
        } finally {
            unlockPage();
        }
    }

    public boolean dispatchMouseWheelEvent(WCMouseWheelEvent me) {
        lockPage();
        try {
            log.log(Level.FINEST, "dispatchMouseWheelEvent: " + me);
            if (isDisposed) {
                log.log(Level.FINE, "MouseWheel event for a disposed web page.");
                return false;
            }
            return twkProcessMouseWheelEvent(getPage(),
                                             me.getX(), me.getY(), me.getScreenX(), me.getScreenY(),
                                             me.getDeltaX(), me.getDeltaY(),
                                             me.isShiftDown(), me.isControlDown(), me.isAltDown(), me.isMetaDown(),
                                             me.getWhen() / 1000.0f);
        } finally {
            unlockPage();
        }
    }

    public boolean dispatchInputMethodEvent(WCInputMethodEvent ie) {
        lockPage();
        try {
            log.log(Level.FINEST, "dispatchInputMethodEvent: " + ie);
            if (isDisposed) {
                log.log(Level.FINE, "InputMethod event for a disposed web page.");
                return false;
            }
            switch (ie.getID()) {
                case WCInputMethodEvent.INPUT_METHOD_TEXT_CHANGED:
                    return twkProcessInputTextChange(getPage(),
                                                     ie.getComposed(), ie.getCommitted(),
                                                     ie.getAttributes(), ie.getCaretPosition());

                case WCInputMethodEvent.CARET_POSITION_CHANGED:
                    return twkProcessCaretPositionChange(getPage(),
                                                         ie.getCaretPosition());
            }
            return false;

        } finally {
            unlockPage();
        }
    }

    public final static int DND_DST_ENTER = 0;
    public final static int DND_DST_OVER = 1;
    public final static int DND_DST_CHANGE = 2;
    public final static int DND_DST_EXIT = 3;
    public final static int DND_DST_DROP = 4;

    public final static int DND_SRC_ENTER = 100;
    public final static int DND_SRC_OVER = 101;
    public final static int DND_SRC_CHANGE = 102;
    public final static int DND_SRC_EXIT = 103;
    public final static int DND_SRC_DROP = 104;

    public int dispatchDragOperation(
            int commandId,
            String[] mimeTypes, String[] values,
            int x, int y,
            int screenX, int screenY,
            int dndActionId)
    {
        lockPage();
        try {
            log.log(Level.FINEST, "dispatchDragOperation: " + x + "," + y
                    + " dndCommand:" + commandId
                    + " dndAction" + dndActionId);
            if (isDisposed) {
                log.log(Level.FINE, "DnD event for a disposed web page.");
                return 0;
            }
            return twkProcessDrag(getPage(),
                    commandId,
                    mimeTypes, values,
                    x, y,
                    screenX, screenY,
                    dndActionId);
        } finally {
            unlockPage();
        }
    }

    public void confirmStartDrag() {
        if (uiClient != null)
            uiClient.confirmStartDrag();
    }

    public boolean isDragConfirmed(){
        return (uiClient != null)
            ? uiClient.isDragConfirmed()
            : false;
    }

    // *************************************************************************
    // Input methods
    // *************************************************************************

    public int[] getClientTextLocation(int index) {
        lockPage();
        try {
            if (isDisposed) {
                log.log(Level.FINE, "getClientTextLocation() request for a disposed web page.");
                return new int[] { 0, 0, 0, 0 };
            }
            return twkGetTextLocation(getPage(), index);

        } finally {
            unlockPage();
        }
    }

    public int getClientLocationOffset(int x, int y) {
        lockPage();
        try {
            if (isDisposed) {
                log.log(Level.FINE, "getClientLocationOffset() request for a disposed web page.");
                return 0;
            }
            return twkGetInsertPositionOffset(getPage());

        } finally {
            unlockPage();
        }
    }

    public int getClientInsertPositionOffset() {
        lockPage();
        try {
            if (isDisposed) {
                log.log(Level.FINE, "getClientInsertPositionOffset() request for a disposed web page.");
                return 0;
            }
            return twkGetInsertPositionOffset(getPage());

        } finally {
            unlockPage();
        }
    }

    public int getClientCommittedTextLength() {
        lockPage();
        try {
            if (isDisposed) {
                log.log(Level.FINE, "getClientCommittedTextOffset() request for a disposed web page.");
                return 0;
            }
            return twkGetCommittedTextLength(getPage());

        } finally {
            unlockPage();
        }
    }

    public String getClientCommittedText() {
        lockPage();
        try {
            if (isDisposed) {
                log.log(Level.FINE, "getClientCommittedText() request for a disposed web page.");
                return "";
            }
            return twkGetCommittedText(getPage());

        } finally {
            unlockPage();
        }
    }

    public String getClientSelectedText() {
        lockPage();
        try {
            if (isDisposed) {
                log.log(Level.FINE, "getClientSelectedText() request for a disposed web page.");
                return "";
            }
            return twkGetSelectedText(getPage());

        } finally {
            unlockPage();
        }
    }

    // *************************************************************************
    // Browser API
    // *************************************************************************

    public void dispose() {
        lockPage();
        try {
            log.log(Level.FINER, "dispose");

            stop();
            dropRenderFrames();
            isDisposed = true;

            twkDestroyPage(pPage);
            pPage = 0;

            for (long frameID : frames) {
                log.log(Level.FINE, "Undestroyed frame view: " + frameID);
            }
            frames.clear();

            if (backbuffer != null) {
                backbuffer.deref();
                backbuffer = null;
            }
        } finally {
            unlockPage();
        }
    }

    public String getName(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Get Name: frame = " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "getName() request for a disposed web page.");
                return null;
            }
            if (!frames.contains(frameID)) {
                return null;
            }
            return twkGetName(frameID);

        } finally {
            unlockPage();
        }
    }

    public String getURL(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Get URL: frame = " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "getURL() request for a disposed web page.");
                return null;
            }
            if (!frames.contains(frameID)) {
                return null;
            }
            return twkGetURL(frameID);

        } finally {
            unlockPage();
        }
    }

    public String getEncoding() {
        lockPage();
        try {
            log.log(Level.FINE, "Get encoding");
            if (isDisposed) {
                log.log(Level.FINE, "getEncoding() request for a disposed web page.");
                return null;
            }
            return twkGetEncoding(getPage());

        } finally {
            unlockPage();
        }
    }

    public void setEncoding(String encoding) {
        lockPage();
        try {
            log.log(Level.FINE, "Set encoding: encoding = " + encoding);
            if (isDisposed) {
                log.log(Level.FINE, "setEncoding() request for a disposed web page.");
                return;
            }
            if (encoding != null && !encoding.isEmpty()) {
                twkSetEncoding(getPage(), encoding);
            }

        } finally {
            unlockPage();
        }
    }

    // DRT support
    public String getInnerText(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Get inner text: frame = " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "getInnerText() request for a disposed web page.");
                return null;
            }
            if (!frames.contains(frameID)) {
                return null;
            }
            return twkGetInnerText(frameID);

        } finally {
            unlockPage();
        }
    }

    // DRT support
    public String getRenderTree(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Get render tree: frame = " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "getRenderTree() request for a disposed web page.");
                return null;
            }
            if (!frames.contains(frameID)) {
                return null;
            }
            return twkGetRenderTree(frameID);

        } finally {
            unlockPage();
        }
    }

    // DRT support
    public int getUnloadEventListenersCount(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "frame: " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "request for a disposed web page.");
                return 0;
            }
            if (!frames.contains(frameID)) {
                return 0;
            }
            return twkGetUnloadEventListenersCount(frameID);

        } finally {
            unlockPage();
        }
    }

    public String getContentType(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Get content type: frame = " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "getContentType() request for a disposed web page.");
                return null;
            }
            if (!frames.contains(frameID)) {
                return null;
            }
            return twkGetContentType(frameID);

        } finally {
            unlockPage();
        }
    }

    public String getTitle(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Get title: frame = " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "getTitle() request for a disposed web page.");
                return null;
            }
            if (!frames.contains(frameID)) {
                return null;
            }
            return twkGetTitle(frameID);

        } finally {
            unlockPage();
        }
    }

    public WCImage getIcon(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Get icon: frame = " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "getIcon() request for a disposed web page.");
                return null;
            }
            if (!frames.contains(frameID)) {
                return null;
            }
            String iconURL = twkGetIconURL(frameID);
            // do we need any cache for icons here?
            if (iconURL != null && !iconURL.isEmpty()) {
                return WCGraphicsManager.getGraphicsManager().getIconImage(iconURL);
            }
            return null;

        } finally {
            unlockPage();
        }
    }

    public void open(final long frameID, final String url) {
        lockPage();
        try {
            log.log(Level.FINE, "Open URL: " + url);
            if (isDisposed) {
                log.log(Level.FINE, "open() request for a disposed web page.");
                return;
            }
            if (!frames.contains(frameID)) {
                return;
            }
            twkOpen(frameID, url);

        } finally {
            unlockPage();
        }
    }

    public void load(final long frameID, final String text, final String contentType) {
        lockPage();
        try {
            log.log(Level.FINE, "Load text: " + text);
            if (text == null) {
                return;
            }
            if (isDisposed) {
                log.log(Level.FINE, "load() request for a disposed web page.");
                return;
            }
            if (!frames.contains(frameID)) {
                return;
            }
            // TODO: handle contentType
            twkLoad(frameID, text, contentType);

        } finally {
            unlockPage();
        }
    }

    public void stop(final long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Stop loading: frame = " + frameID);

            String url;
            String contentType;
            if (isDisposed) {
                log.log(Level.FINE, "cancel() request for a disposed web page.");
                return;
            }
            if (!frames.contains(frameID)) {
                return;
            }
            url = twkGetURL(frameID);
            contentType = twkGetContentType(frameID);
            twkStop(frameID);
            // WebKit doesn't send any notifications about loading stopped,
            // so sending it here
            fireLoadEvent(frameID, LoadListenerClient.LOAD_STOPPED, url, contentType, 1.0, 0);

        } finally {
            unlockPage();
        }
    }

    // stops all loading synchronously
    public void stop() {
        lockPage();
        try {
            log.log(Level.FINE, "Stop loading sync");
            if (isDisposed) {
                log.log(Level.FINE, "stopAll() request for a disposed web page.");
                return;
            }
            twkStopAll(getPage());

        } finally {
            unlockPage();
        }
    }

    public void refresh(final long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Refresh: frame = " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "refresh() request for a disposed web page.");
                return;
            }
            if (!frames.contains(frameID)) {
                return;
            }
            twkRefresh(frameID);

        } finally {
            unlockPage();
        }
    }

    public BackForwardList createBackForwardList() {
        return new BackForwardList(this);
    }

    public boolean goBack() {
        lockPage();
        try {
            log.log(Level.FINE, "Go back");
            if (isDisposed) {
                log.log(Level.FINE, "goBack() request for a disposed web page.");
                return false;
            }
            return twkGoBackForward(getPage(), -1);

        } finally {
            unlockPage();
        }
    }

    public boolean goForward() {
        lockPage();
        try {
            log.log(Level.FINE, "Go forward");
            if (isDisposed) {
                log.log(Level.FINE, "goForward() request for a disposed web page.");
                return false;
            }
            return twkGoBackForward(getPage(), 1);

        } finally {
            unlockPage();
        }
    }

    public boolean copy() {
        lockPage();
        try {
            log.log(Level.FINE, "Copy");
            if (isDisposed) {
                log.log(Level.FINE, "copy() request for a disposed web page.");
                return false;
            }
            long frameID = getMainFrame();
            if (!frames.contains(frameID)) {
                return false;
            }
            return twkCopy(frameID);

        } finally {
            unlockPage();
        }
    }

    // Find in page
    public boolean find(String stringToFind, boolean forward, boolean wrap, boolean matchCase) {
        lockPage();
        try {
            log.log(Level.FINE, "Find in page: stringToFind = " + stringToFind + ", " +
                    (forward ? "forward" : "backward") + (wrap ? ", wrap" : "") + (matchCase ? ", matchCase" : ""));
            if (isDisposed) {
                log.log(Level.FINE, "find() request for a disposed web page.");
                return false;
            }
            return twkFindInPage(getPage(), stringToFind, forward, wrap, matchCase);

        } finally {
            unlockPage();
        }
    }

    // Find in frame
    public boolean find(long frameID,
        String stringToFind, boolean forward, boolean wrap, boolean matchCase)
    {
        lockPage();
        try {
            log.log(Level.FINE, "Find in frame: stringToFind = " + stringToFind + ", " +
                    (forward ? "forward" : "backward") + (wrap ? ", wrap" : "") + (matchCase ? ", matchCase" : ""));
            if (isDisposed) {
                log.log(Level.FINE, "find() request for a disposed web page.");
                return false;
            }
            if (!frames.contains(frameID)) {
                return false;
            }
            return twkFindInFrame(frameID, stringToFind, forward, wrap, matchCase);

        } finally {
            unlockPage();
        }
    }

    public float getZoomFactor(boolean textOnly) {
        lockPage();
        try {
            log.log(Level.FINE, "Get zoom factor, textOnly=" + textOnly);
            if (isDisposed) {
                log.log(Level.FINE, "getZoomFactor() request for a disposed web page.");
                return 1.0f;
            }
            long frameID = getMainFrame();
            if (!frames.contains(frameID)) {
                return 1.0f;
            }
            return twkGetZoomFactor(frameID, textOnly);
        } finally {
            unlockPage();
        }
    }

    public void setZoomFactor(float zoomFactor, boolean textOnly) {
        lockPage();
        try {
            log.fine(String.format("Set zoom factor %.2f, textOnly=%b", zoomFactor, textOnly));
            if (isDisposed) {
                log.log(Level.FINE, "setZoomFactor() request for a disposed web page.");
                return;
            }
            long frameID = getMainFrame();
            if ((frameID == 0) || !frames.contains(frameID)) {
                return;
            }
            twkSetZoomFactor(frameID, zoomFactor, textOnly);
        } finally {
            unlockPage();
        }
    }

    public void setFontSmoothingType(int fontSmoothingType) {
        this.fontSmoothingType = fontSmoothingType;
        repaintAll();
    }

    // DRT support
    public void reset(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Reset: frame = " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "reset() request for a disposed web page.");
                return;
            }
            if ((frameID == 0) || !frames.contains(frameID)) {
                return;
            }
            twkReset(frameID);

        } finally {
            unlockPage();
        }
    }

    public Object executeScript(long frameID, String script) throws JSException {
        lockPage();
        try {
            log.log(Level.FINE, "execute script: \"" + script + "\" in frame = " + frameID);
            if (isDisposed) {
                log.log(Level.FINE, "executeScript() request for a disposed web page.");
                return null;
            }
            if ((frameID == 0) || !frames.contains(frameID)) {
                return null;
            }
            return twkExecuteScript(frameID, script);

        } finally {
            unlockPage();
        }
    }

    public long getMainFrame() {
        lockPage();
        try {
            log.log(Level.FINER, "getMainFrame: page = " + pPage);
            if (isDisposed) {
                log.log(Level.FINE, "getMainFrame() request for a disposed web page.");
                return 0L;
            }
            long mainFrameID = twkGetMainFrame(getPage());
            log.log(Level.FINER, "Main frame = " + mainFrameID);
            frames.add(mainFrameID);
            return mainFrameID;
        } finally {
            unlockPage();
        }
    }

    public long getParentFrame(long childID) {
        lockPage();
        try {
            log.log(Level.FINE, "getParentFrame: child = " + childID);
            if (isDisposed) {
                log.log(Level.FINE, "getParentFrame() request for a disposed web page.");
                return 0L;
            }
            if (!frames.contains(childID)) {
                return 0L;
            }
            return twkGetParentFrame(childID);
        } finally {
            unlockPage();
        }
    }

    public List<Long> getChildFrames(long parentID) {
        lockPage();
        try {
            log.log(Level.FINE, "getChildFrames: parent = " + parentID);
            if (isDisposed) {
                log.log(Level.FINE, "getChildFrames() request for a disposed web page.");
                return null;
            }
            if (!frames.contains(parentID)) {
                return null;
            }
            long[] children = twkGetChildFrames(parentID);
            List<Long> childrenList = new LinkedList<Long>();
            for (long child : children) {
                childrenList.add(Long.valueOf(child));
            }
            return childrenList;
        } finally {
            unlockPage();
        }
    }

    public WCRectangle getVisibleRect(long frameID) {
        lockPage();
        try {
            if (!frames.contains(frameID)) {
                return null;
            }
            int[] arr = twkGetVisibleRect(frameID);
            if (arr != null) {
                return new WCRectangle(arr[0], arr[1], arr[2], arr[3]);
            }
            return null;
        } finally {
            unlockPage();
        }
    }

    public void scrollToPosition(long frameID, WCPoint p) {
        lockPage();
        try {
            if (!frames.contains(frameID)) {
                return;
            }
            twkScrollToPosition(frameID, p.getIntX(), p.getIntY());
        } finally {
            unlockPage();
        }
    }

    public WCSize getContentSize(long frameID) {
        lockPage();
        try {
            if (!frames.contains(frameID)) {
                return null;
            }
            int[] arr = twkGetContentSize(frameID);
            if (arr != null) {
                return new WCSize(arr[0], arr[1]);
            }
            return null;
        } finally {
            unlockPage();
        }
    }

    // ---- DOM ---- //

    public Document getDocument(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "getDocument");
            if (isDisposed) {
                log.log(Level.FINE, "getDocument() request for a disposed web page.");
                return null;
            }

            if (!frames.contains(frameID)) {
                return null;
            }
            return twkGetDocument(frameID);
        } finally {
            unlockPage();
        }
    }

    public Element getOwnerElement(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "getOwnerElement");
            if (isDisposed) {
                log.log(Level.FINE, "getOwnerElement() request for a disposed web page.");
                return null;
            }

            if (!frames.contains(frameID)) {
                return null;
            }
            return twkGetOwnerElement(frameID);
        } finally {
            unlockPage();
        }
    }

   // ---- EDITING SUPPORT ---- //

    public boolean executeCommand(String command, String value) {
        lockPage();
        try {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "command: [{0}], value: [{1}]",
                        new Object[] {command, value});
            }
            if (isDisposed) {
                log.log(Level.FINE, "Web page is already disposed");
                return false;
            }

            boolean result = twkExecuteCommand(getPage(), command, value);

            log.log(Level.FINE, "result: [{0}]", result);
            return result;
        } finally {
            unlockPage();
        }
    }

    public boolean queryCommandEnabled(String command) {
        lockPage();
        try {
            log.log(Level.FINE, "command: [{0}]", command);
            if (isDisposed) {
                log.log(Level.FINE, "Web page is already disposed");
                return false;
            }

            boolean result = twkQueryCommandEnabled(getPage(), command);

            log.log(Level.FINE, "result: [{0}]", result);
            return result;
        } finally {
            unlockPage();
        }
    }

    public boolean queryCommandState(String command) {
        lockPage();
        try {
            log.log(Level.FINE, "command: [{0}]", command);
            if (isDisposed) {
                log.log(Level.FINE, "Web page is already disposed");
                return false;
            }

            boolean result = twkQueryCommandState(getPage(), command);

            log.log(Level.FINE, "result: [{0}]", result);
            return result;
        } finally {
            unlockPage();
        }
    }

    public String queryCommandValue(String command) {
        lockPage();
        try {
            log.log(Level.FINE, "command: [{0}]", command);
            if (isDisposed) {
                log.log(Level.FINE, "Web page is already disposed");
                return null;
            }

            String result = twkQueryCommandValue(getPage(), command);

            log.log(Level.FINE, "result: [{0}]", result);
            return result;
        } finally {
            unlockPage();
        }
    }

    public boolean isEditable() {
        lockPage();
        try {
            log.log(Level.FINE, "isEditable");
            if (isDisposed) {
                log.log(Level.FINE, "isEditable() request for a disposed web page.");
                return false;
            }

            return twkIsEditable(getPage());
        } finally {
            unlockPage();
        }
    }

    public void setEditable(boolean editable) {
        lockPage();
        try {
            log.log(Level.FINE, "setEditable");
            if (isDisposed) {
                log.log(Level.FINE, "setEditable() request for a disposed web page.");
                return;
            }

            twkSetEditable(getPage(), editable);
        } finally {
            unlockPage();
        }
    }

    /**
     * @return HTML content of the frame,
     *         or null if frame document is absent or non-HTML.
     */
    public String getHtml(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "getHtml");
            if (isDisposed) {
                log.log(Level.FINE, "getHtml() request for a disposed web page.");
                return null;
            }
            if (!frames.contains(frameID)) {
                return null;
            }
            return twkGetHtml(frameID);
        } finally {
            unlockPage();
        }
    }

    // ---- PRINTING SUPPORT ---- //

    public int beginPrinting(float width, float height) {
        lockPage();
        try {
            if (isDisposed) {
                log.warning("beginPrinting() called for a disposed web page.");
                return 0;
            }
            return twkBeginPrinting(getPage(), width, height);
        } finally {
            unlockPage();
        }
    }

    public void endPrinting() {
        lockPage();
        try {
            if (isDisposed) {
                log.warning("endPrinting() called for a disposed web page.");
                return;
            }
            twkEndPrinting(getPage());
        } finally {
            unlockPage();
        }
    }

    public void print(final WCGraphicsContext gc, final int pageNumber, final float width) {
        lockPage();
        try {
            if (isDisposed) {
                log.warning("print() called for a disposed web page.");
                return;
            }
            final WCRenderQueue rq = WCGraphicsManager.getGraphicsManager().
                    createRenderQueue(null, true);
            final CountDownLatch l = new CountDownLatch(1);
            Invoker.getInvoker().invokeOnEventThread(() -> {
                try {
                    twkPrint(getPage(), rq, pageNumber, width);
                } finally {
                    l.countDown();
                }
            });

            try {
                l.await();
            } catch (InterruptedException e) {
                rq.dispose();
                return;
            }
            rq.decode(gc);
        } finally {
            unlockPage();
        }
    }

    public int getPageHeight() {
        return getFrameHeight(getMainFrame());
    }

    public int getFrameHeight(long frameID) {
        lockPage();
        try {
            log.log(Level.FINE, "Get page height");
            if (isDisposed) {
                log.log(Level.FINE, "getFrameHeight() request for a disposed web page.");
                return 0;
            }
            if (!frames.contains(frameID)) {
                return 0;
            }
            int height = twkGetFrameHeight(frameID);
            log.log(Level.FINE, "Height = " + height);
            return height;
        } finally {
            unlockPage();
        }
    }

    public float adjustFrameHeight(long frameID,
                                   float oldTop, float oldBottom, float bottomLimit)
    {
        lockPage();
        try {
            log.log(Level.FINE, "Adjust page height");
            if (isDisposed) {
                log.log(Level.FINE, "adjustFrameHeight() request for a disposed web page.");
                return 0;
            }
            if (!frames.contains(frameID)) {
                return 0;
            }
            return twkAdjustFrameHeight(frameID, oldTop, oldBottom, bottomLimit);
        } finally {
            unlockPage();
        }
    }

    // ---- SETTINGS ---- //

    /**
     * Returns the usePageCache settings field.
     * @return {@code true} if this object uses the page cache,
     *         {@code false} otherwise.
     */
    public boolean getUsePageCache() {
        lockPage();
        try {
            return twkGetUsePageCache(getPage());
        } finally {
            unlockPage();
        }
    }

    /**
     * Sets the usePageCache settings field.
     * @param usePageCache {@code true} to use the page cache,
     *        {@code false} to not use the page cache.
     */
    public void setUsePageCache(boolean usePageCache) {
        lockPage();
        try {
            twkSetUsePageCache(getPage(), usePageCache);
        } finally {
            unlockPage();
        }
    }

    public boolean getDeveloperExtrasEnabled() {
        lockPage();
        try {
            boolean result = twkGetDeveloperExtrasEnabled(getPage());
            log.log(Level.FINE,
                    "Getting developerExtrasEnabled, result: [{0}]",
                    result);
            return result;
        } finally {
            unlockPage();
        }
    }

    public void setDeveloperExtrasEnabled(boolean enabled) {
        lockPage();
        try {
            log.log(Level.FINE,
                    "Setting developerExtrasEnabled, value: [{0}]",
                    enabled);
            twkSetDeveloperExtrasEnabled(getPage(), enabled);
        } finally {
            unlockPage();
        }
    }

    public boolean isJavaScriptEnabled() {
        lockPage();
        try {
            return twkIsJavaScriptEnabled(getPage());
        } finally {
            unlockPage();
        }
    }

    public void setJavaScriptEnabled(boolean enable) {
        lockPage();
        try {
            twkSetJavaScriptEnabled(getPage(), enable);
        } finally {
            unlockPage();
        }
    }

    public boolean isContextMenuEnabled() {
        lockPage();
        try {
            return twkIsContextMenuEnabled(getPage());
        } finally {
            unlockPage();
        }
    }

    public void setContextMenuEnabled(boolean enable) {
        lockPage();
        try {
            twkSetContextMenuEnabled(getPage(), enable);
        } finally {
            unlockPage();
        }
    }

    public void setUserStyleSheetLocation(String url) {
        lockPage();
        try {
            twkSetUserStyleSheetLocation(getPage(), url);
        } finally {
            unlockPage();
        }
    }

    public String getUserAgent() {
        lockPage();
        try {
            return twkGetUserAgent(getPage());
        } finally {
            unlockPage();
        }
    }

    public void setUserAgent(String userAgent) {
        lockPage();
        try {
            twkSetUserAgent(getPage(), userAgent);
        } finally {
            unlockPage();
        }
    }

    public void setLocalStorageDatabasePath(String path) {
        lockPage();
        try {
            twkSetLocalStorageDatabasePath(getPage(), path);
        } finally {
            unlockPage();
        }
    }

    public void setLocalStorageEnabled(boolean enabled) {
        lockPage();
        try {
            twkSetLocalStorageEnabled(getPage(), enabled);
        } finally {
            unlockPage();
        }
    }

    // ---- INSPECTOR SUPPORT ---- //

    public void connectInspectorFrontend() {
        lockPage();
        try {
            log.log(Level.FINE, "Connecting inspector frontend");
            twkConnectInspectorFrontend(getPage());
        } finally {
            unlockPage();
        }
    }

    public void disconnectInspectorFrontend() {
        lockPage();
        try {
            log.log(Level.FINE, "Disconnecting inspector frontend");
            twkDisconnectInspectorFrontend(getPage());
        } finally {
            unlockPage();
        }
    }

    public void dispatchInspectorMessageFromFrontend(String message) {
        lockPage();
        try {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE,
                        "Dispatching inspector message from frontend, "
                        + "message: [{0}]",
                        message);
            }
            twkDispatchInspectorMessageFromFrontend(getPage(), message);
        } finally {
            unlockPage();
        }
    }

    // *************************************************************************
    // Native callbacks
    // *************************************************************************

    private void fwkFrameCreated(long frameID) {
        log.log(Level.FINE, "Frame created: frame = " + frameID);
        if (frames.contains(frameID)) {
            log.log(Level.FINE, "Error in fwkFrameCreated: frame is already in frames");
            return;
        }
        frames.add(frameID);
    }

    private void fwkFrameDestroyed(long frameID) {
        log.log(Level.FINE, "Frame destroyed: frame = " + frameID);
        if (!frames.contains(frameID)) {
            log.log(Level.FINE, "Error in fwkFrameDestroyed: frame is not found in frames");
            return;
        }
        frames.remove(frameID);
    }

    private void fwkRepaint(int x, int y, int w, int h) {
        lockPage();
        try {
            if (paintLog.isLoggable(Level.FINEST)) {
                paintLog.log(Level.FINEST, "x: {0}, y: {1}, w: {2}, h: {3}",
                        new Object[] {x, y, w, h});
            }
            addDirtyRect(new WCRectangle(x, y, w, h));
        } finally {
            unlockPage();
        }
    }

    private void fwkScroll(int x, int y, int w, int h, int deltaX, int deltaY) {
        if (paintLog.isLoggable(Level.FINEST)) {
            paintLog.finest("Scroll: " + x + " " + y + " " + w + " " + h + "  " + deltaX + " " + deltaY);
        }
        if (pageClient == null || !pageClient.isBackBufferSupported()) {
            paintLog.finest("blit scrolling is switched off");
            // TODO: check why we return void, not boolean (see ScrollView::m_canBlitOnScroll)
            return;
        }
        scroll(x, y, w, h, deltaX, deltaY);
    }

    private void fwkTransferFocus(boolean forward) {
        log.log(Level.FINER, "Transfer focus " + (forward ? "forward" : "backward"));

        if (pageClient != null) {
            pageClient.transferFocus(forward);
        }
    }

    private void fwkSetCursor(long id) {
        log.log(Level.FINER, "Set cursor: " + id);

        if (pageClient != null) {
            pageClient.setCursor(id);
        }
    }

    private void fwkSetFocus(boolean focus) {
        log.log(Level.FINER, "Set focus: " + (focus ? "true" : "false"));

        if (pageClient != null) {
            pageClient.setFocus(focus);
        }
    }

    private void fwkSetTooltip(String tooltip) {
        log.log(Level.FINER, "Set tooltip: " + tooltip);

        if (pageClient != null) {
            pageClient.setTooltip(tooltip);
        }
    }

    private void fwkPrint() {
        log.log(Level.FINER, "Print");

        if (uiClient != null) {
            uiClient.print();
        }
    }

    private void fwkSetRequestURL(long pFrame, int id, String url) {
        log.log(Level.FINER, "Set request URL: id = " + id + ", url = " + url);

        synchronized (requestURLs) {
            requestURLs.put(id, url);
        }
    }

    private void fwkRemoveRequestURL(long pFrame, int id) {
        log.log(Level.FINER, "Set request URL: id = " + id);

        synchronized (requestURLs) {
            requestURLs.remove(id);
            requestStarted.remove(id);
        }
    }

    private WebPage fwkCreateWindow(
            boolean menu, boolean status, boolean toolbar, boolean resizable) {
        log.log(Level.FINER, "Create window");

        if (uiClient != null) {
            return uiClient.createPage(menu, status, toolbar, resizable);
        }
        return null;
    }

    private void fwkShowWindow() {
        log.log(Level.FINER, "Show window");

        if (uiClient != null) {
            uiClient.showView();
        }
    }

    private void fwkCloseWindow() {
        log.log(Level.FINER, "Close window");

        if (permitCloseWindowAction()) {
            if (uiClient != null) {
                uiClient.closePage();
            }
        }
    }

    private WCRectangle fwkGetWindowBounds() {
        log.log(Level.FINE, "Get window bounds");

        if (uiClient != null) {
            WCRectangle bounds = uiClient.getViewBounds();
            if (bounds != null) {
                return bounds;
            }
        }
        return fwkGetPageBounds();
    }

    private void fwkSetWindowBounds(int x, int y, int w, int h) {
        log.log(Level.FINER, "Set window bounds: " + x + " " + y + " " + w + " " + h);

        if (uiClient != null) {
            uiClient.setViewBounds(new WCRectangle(x, y, w, h));
        }
    }

    private WCRectangle fwkGetPageBounds() {
        log.log(Level.FINER, "Get page bounds");
        return new WCRectangle(0, 0, width, height);
    }

    private void fwkSetScrollbarsVisible(boolean visible) {
        // TODO: handle this request internally
    }

    private void fwkSetStatusbarText(String text) {
        log.log(Level.FINER, "Set statusbar text: " + text);

        if (uiClient != null) {
            uiClient.setStatusbarText(text);
        }
    }

    private String[] fwkChooseFile(String initialFileName, boolean multiple) {
        log.log(Level.FINER, "Choose file, initial=" + initialFileName);

        return uiClient != null
                ? uiClient.chooseFile(initialFileName, multiple)
                : null;
    }

    private void fwkStartDrag(
          Object image,
          int imageOffsetX, int imageOffsetY,
          int eventPosX, int eventPosY,
          String[] mimeTypes,
          Object[] values)
    {
        log.log(Level.FINER, "Start drag: ");

        if (uiClient != null) {
            uiClient.startDrag(
                  WCImage.getImage(image),
                  imageOffsetX, imageOffsetY,
                  eventPosX, eventPosY,
                  mimeTypes,
                  values);
        }
    }

    private WCPoint fwkScreenToWindow(WCPoint ptScreen) {
        log.log(Level.FINER, "fwkScreenToWindow");

        if (pageClient != null) {
            return pageClient.screenToWindow(ptScreen);
        }
        return ptScreen;
    }

    private WCPoint fwkWindowToScreen(WCPoint ptWindow) {
        log.log(Level.FINER, "fwkWindowToScreen");

        if (pageClient != null) {
            return pageClient.windowToScreen(ptWindow);
        }
        return ptWindow;
    }


    private void fwkAlert(String text) {
        log.log(Level.FINE, "JavaScript alert(): text = " + text);

        if (uiClient != null) {
            uiClient.alert(text);
        }
    }

    private boolean fwkConfirm(String text) {
        log.log(Level.FINE, "JavaScript confirm(): text = " + text);

        if (uiClient != null) {
            return uiClient.confirm(text);
        }
        return false;
    }

    private String fwkPrompt(String text, String defaultValue) {
        log.log(Level.FINE, "JavaScript prompt(): text = " + text + ", default = " + defaultValue);

        if (uiClient != null) {
            return uiClient.prompt(text, defaultValue);
        }
        return null;
    }

    private void fwkAddMessageToConsole(String message, int lineNumber,
            String sourceId)
    {
        log.log(Level.FINE, "fwkAddMessageToConsole(): message = " + message
                + ", lineNumber = " + lineNumber + ", sourceId = " + sourceId);
        if (pageClient != null) {
            pageClient.addMessageToConsole(message, lineNumber, sourceId);
        }
    }

    private void fwkFireLoadEvent(long frameID, int state,
                                  String url, String contentType,
                                  double progress, int errorCode)
    {
        log.log(Level.FINER, "Load event: pFrame = " + frameID + ", state = " + state +
                ", url = " + url + ", contenttype=" + contentType +
                ", progress = " + progress + ", error = " + errorCode);

        fireLoadEvent(frameID, state, url, contentType, progress, errorCode);
    }

    private void fwkFireResourceLoadEvent(long frameID, int state,
                                          int id, String contentType,
                                          double progress, int errorCode)
    {
        log.log(Level.FINER, "Resource load event: pFrame = " + frameID + ", state = " + state +
                ", id = " + id + ", contenttype=" + contentType +
                ", progress = " + progress + ", error = " + errorCode);

        String url = requestURLs.get(id);
        if (url == null) {
            log.log(Level.FINE, "Error in fwkFireResourceLoadEvent: unknown request id " + id);
            return;
        }

        int eventState = state;
        // convert second and all subsequent STARTED into REDIRECTED
        if (state == LoadListenerClient.RESOURCE_STARTED) {
            if (requestStarted.contains(id)) {
                eventState = LoadListenerClient.RESOURCE_REDIRECTED;
            } else {
                requestStarted.add(id);
            }
        }

        fireResourceLoadEvent(frameID, eventState, url, contentType, progress, errorCode);
    }

    private boolean fwkPermitNavigateAction(long pFrame, String url) {
        log.log(Level.FINE, "Policy: permit NAVIGATE: pFrame = " + pFrame + ", url = " + url);

        if (policyClient != null) {
            return policyClient.permitNavigateAction(pFrame, str2url(url));
        }
        return true;
    }

    private boolean fwkPermitRedirectAction(long pFrame, String url) {
        log.log(Level.FINE, "Policy: permit REDIRECT: pFrame = " + pFrame + ", url = " + url);

        if (policyClient != null) {
            return policyClient.permitRedirectAction(pFrame, str2url(url));
        }
        return true;
    }

    private boolean fwkPermitAcceptResourceAction(long pFrame, String url) {
        log.log(Level.FINE, "Policy: permit ACCEPT_RESOURCE: pFrame + " + pFrame + ", url = " + url);

        if (policyClient != null) {
            return policyClient.permitAcceptResourceAction(pFrame, str2url(url));
        }
        return true;
    }

    private boolean fwkPermitSubmitDataAction(long pFrame, String url,
                                              String httpMethod, boolean isSubmit)
    {
        log.log(Level.FINE, "Policy: permit " + (isSubmit ? "" : "RE") + "SUBMIT_DATA: pFrame = " +
                pFrame + ", url = " + url + ", httpMethod = " + httpMethod);

        if (policyClient != null) {
            if (isSubmit) {
                return policyClient.permitSubmitDataAction(pFrame, str2url(url), httpMethod);
            } else {
                return policyClient.permitResubmitDataAction(pFrame, str2url(url), httpMethod);
            }
        }
        return true;
    }

    private boolean fwkPermitEnableScriptsAction(long pFrame, String url) {
        log.log(Level.FINE, "Policy: permit ENABLE_SCRIPTS: pFrame + " + pFrame + ", url = " + url);

        if (policyClient != null) {
            return policyClient.permitEnableScriptsAction(pFrame, str2url(url));
        }
        return true;
    }

    private boolean fwkPermitNewWindowAction(long pFrame, String url) {
        log.log(Level.FINE, "Policy: permit NEW_PAGE: pFrame = " + pFrame + ", url = " + url);

        if (policyClient != null) {
            return policyClient.permitNewPageAction(pFrame, str2url(url));
        }
        return true;
    }

    // Called from fwkCloseWindow, that's why no "fwk" prefix
    private boolean permitCloseWindowAction() {
        log.log(Level.FINE, "Policy: permit CLOSE_PAGE");

        if (policyClient != null) {
            // Unfortunately, webkit doesn't provide an information about what
            // web frame initiated close window request, so using main frame here
            return policyClient.permitClosePageAction(getMainFrame());
        }
        return true;
    }

    private void fwkRepaintAll() {
        log.log(Level.FINE, "Repainting the entire page");
        repaintAll();
    }

    private boolean fwkSendInspectorMessageToFrontend(String message) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE,
                    "Sending inspector message to frontend, message: [{0}]",
                    message);
        }
        boolean result = false;
        if (inspectorClient != null) {
            log.log(Level.FINE, "Invoking inspector client");
            result = inspectorClient.sendMessageToFrontend(message);
        }
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Result: [{0}]", result);
        }
        return result;
    }

    // ---- DumpRenderTree support ---- //

    public static int getWorkerThreadCount() {
        return twkWorkerThreadCount();
    }

    private static native int twkWorkerThreadCount();

    private void fwkDidClearWindowObject(long pContext, long pWindowObject) {
        if (pageClient != null) {
            pageClient.didClearWindowObject(pContext, pWindowObject);
        }
    }

    // *************************************************************************
    // Private methods
    // *************************************************************************

    private URL str2url(String url) {
        try {
            return newURL(url);
        } catch (MalformedURLException ex) {
            log.log(Level.FINE, "Exception while converting \"" + url + "\" to URL", ex);
        }
        return null;
    }

    private void fireLoadEvent(long frameID, int state, String url,
            String contentType, double progress, int errorCode)
    {
        for (LoadListenerClient l : loadListenerClients) {
            l.dispatchLoadEvent(frameID, state, url, contentType, progress, errorCode);
        }
    }

    private void fireResourceLoadEvent(long frameID, int state, String url,
            String contentType, double progress, int errorCode)
    {
        for (LoadListenerClient l : loadListenerClients) {
            l.dispatchResourceLoadEvent(frameID, state, url, contentType, progress, errorCode);
        }
    }

    private void repaintAll() {
        dirtyRects.clear();
        addDirtyRect(new WCRectangle(0, 0, width, height));
    }

    // *************************************************************************
    // Native methods
    // *************************************************************************

    private native long twkCreatePage(boolean editable);
    private native void twkInit(long pPage, boolean usePlugins);
    private native void twkDestroyPage(long pPage);

    private native long twkGetMainFrame(long pPage);
    private native long twkGetParentFrame(long pFrame);
    private native long[] twkGetChildFrames(long pFrame);

    private native String twkGetName(long pFrame);
    private native String twkGetURL(long pFrame);
    private native String twkGetInnerText(long pFrame);
    private native String twkGetRenderTree(long pFrame);
    private native String twkGetContentType(long pFrame);
    private native String twkGetTitle(long pFrame);
    private native String twkGetIconURL(long pFrame);
    private native static Document twkGetDocument(long pFrame);
    private native static Element twkGetOwnerElement(long pFrame);

    private native void twkOpen(long pFrame, String url);
    private native void twkLoad(long pFrame, String text, String contentType);
    private native void twkStop(long pFrame);
    private native void twkStopAll(long pPage); // sync
    private native void twkRefresh(long pFrame);

    private native boolean twkGoBackForward(long pPage, int distance);

    private native boolean twkCopy(long pFrame);
    private native boolean twkFindInPage(long pPage,
                                         String stringToFind, boolean forward,
                                         boolean wrap, boolean matchCase);
    private native boolean twkFindInFrame(long pFrame,
                                          String stringToFind, boolean forward,
                                          boolean wrap, boolean matchCase);

    private native float twkGetZoomFactor(long pFrame, boolean textOnly);
    private native void twkSetZoomFactor(long pFrame, float zoomFactor, boolean textOnly);

    private native Object twkExecuteScript(long pFrame, String script);

    private native void twkReset(long pFrame);

    private native int twkGetFrameHeight(long pFrame);
    private native int twkBeginPrinting(long pPage, float width, float height);
    private native void twkEndPrinting(long pPage);
    private native void twkPrint(long pPage, WCRenderQueue gc, int pageNumber, float width);
    private native float twkAdjustFrameHeight(long pFrame, float oldTop, float oldBottom, float bottomLimit);

    private native int[] twkGetVisibleRect(long pFrame);
    private native void twkScrollToPosition(long pFrame, int x, int y);
    private native int[] twkGetContentSize(long pFrame);
    private native void twkSetTransparent(long pFrame, boolean isTransparent);
    private native void twkSetBackgroundColor(long pFrame, int backgroundColor);

    private native void twkSetBounds(long pPage, int x, int y, int w, int h);
    private native void twkPrePaint(long pPage);
    private native void twkUpdateContent(long pPage, WCRenderQueue rq, int x, int y, int w, int h);
    private native void twkPostPaint(long pPage, WCRenderQueue rq,
                                     int x, int y, int w, int h);

    private native String twkGetEncoding(long pPage);
    private native void twkSetEncoding(long pPage, String encoding);

    private native void twkProcessFocusEvent(long pPage, int id, int direction);
    private native boolean twkProcessKeyEvent(long pPage, int type, String text,
                                              String keyIdentifier,
                                              int windowsVirtualKeyCode,
                                              boolean shift, boolean ctrl,
                                              boolean alt, boolean meta);
    private native boolean twkProcessMouseEvent(long pPage, int id,
                                                int button, int clickCount,
                                                int x, int y, int sx, int sy,
                                                boolean shift, boolean control, boolean alt, boolean meta,
                                                boolean popupTrigger, float when);
    private native boolean twkProcessMouseWheelEvent(long pPage,
                                                     int x, int y, int sx, int sy,
                                                     float dx, float dy,
                                                     boolean shift, boolean control, boolean alt, boolean meta,
                                                     float when);
    private native boolean twkProcessInputTextChange(long pPage, String committed, String composed,
                                                     int[] attributes, int caretPosition);
    private native boolean twkProcessCaretPositionChange(long pPage, int caretPosition);
    private native int[] twkGetTextLocation(long pPage, int charIndex);
    private native int twkGetInsertPositionOffset(long pPage);
    private native int twkGetCommittedTextLength(long pPage);
    private native String twkGetCommittedText(long pPage);
    private native String twkGetSelectedText(long pPage);

    private native int twkProcessDrag(long page,
            int commandId,
            String[] mimeTypes, String[] values,
            int x, int y,
            int screenX, int screenY,
            int dndActionId);

    private native boolean twkExecuteCommand(long page, String command,
                                             String value);
    private native boolean twkQueryCommandEnabled(long page, String command);
    private native boolean twkQueryCommandState(long page, String command);
    private native String twkQueryCommandValue(long page, String command);
    private native boolean twkIsEditable(long page);
    private native void twkSetEditable(long page, boolean editable);
    private native String twkGetHtml(long pFrame);

    private native boolean twkGetUsePageCache(long page);
    private native void twkSetUsePageCache(long page, boolean usePageCache);
    private native boolean twkGetDeveloperExtrasEnabled(long page);
    private native void twkSetDeveloperExtrasEnabled(long page,
                                                     boolean enabled);
    private native boolean twkIsJavaScriptEnabled(long page);
    private native void twkSetJavaScriptEnabled(long page, boolean enable);
    private native boolean twkIsContextMenuEnabled(long page);
    private native void twkSetContextMenuEnabled(long page, boolean enable);
    private native void twkSetUserStyleSheetLocation(long page, String url);
    private native String twkGetUserAgent(long page);
    private native void twkSetUserAgent(long page, String userAgent);
    private native void twkSetLocalStorageDatabasePath(long page, String path);
    private native void twkSetLocalStorageEnabled(long page, boolean enabled);

    private native int twkGetUnloadEventListenersCount(long pFrame);

    private native void twkConnectInspectorFrontend(long pPage);
    private native void twkDisconnectInspectorFrontend(long pPage);
    private native void twkDispatchInspectorMessageFromFrontend(long pPage,
                                                                String message);
}
