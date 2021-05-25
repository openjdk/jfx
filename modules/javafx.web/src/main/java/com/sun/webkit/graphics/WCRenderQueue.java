/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.graphics;

import java.lang.annotation.Native;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.webkit.Invoker;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class WCRenderQueue extends Ref {
    private final static AtomicInteger idCountObj = new AtomicInteger(0);
    private final static PlatformLogger log =
            PlatformLogger.getLogger(WCRenderQueue.class.getName());
    @Native public final static int MAX_QUEUE_SIZE = 0x80000;

    private final LinkedList<BufferData> buffers = new LinkedList<BufferData>();
    private BufferData currentBuffer = new BufferData();
    private final WCRectangle clip;
    private int size = 0;
    private final boolean opaque;

    // Associated graphics context (currently used to draw to a buffered image).
    protected final WCGraphicsContext gc;

    protected WCRenderQueue(WCGraphicsContext gc) {
        this.clip = null;
        this.opaque = false;
        this.gc = gc;
    }

    protected WCRenderQueue(WCRectangle clip, boolean opaque) {
        this.clip = clip;
        this.opaque = opaque;
        this.gc = null;
    }

    public synchronized int getSize() {
        return size;
    }

    public synchronized void addBuffer(ByteBuffer buffer) {
        if (log.isLoggable(Level.FINE) && buffers.isEmpty()) {
            log.fine("'{'WCRenderQueue{0}[{1}]",
                    new Object[]{hashCode(), idCountObj.incrementAndGet()});
        }
        currentBuffer.setBuffer(buffer);
        buffers.addLast(currentBuffer);
        currentBuffer = new BufferData();
        size += buffer.capacity();
        if (size > MAX_QUEUE_SIZE && gc!=null) {
            // It is isolated queue over the canvas image [image-gc!=null].
            // We need to flush the changes periodically
            // by the same reason as in [WebPage.addLastRQ].
            flush();
        }
    }

    public synchronized boolean isEmpty() {
        return buffers.isEmpty();
    }

    public synchronized void decode(WCGraphicsContext gc) {
        if (gc == null || !gc.isValid()) {
            log.fine("WCRenderQueue::decode : GC is " + (gc == null ? "null" : " invalid"));
            return;
        }

        for (BufferData bdata : buffers) {
            try {
                GraphicsDecoder.decode(
                    WCGraphicsManager.getGraphicsManager(), gc, bdata);
            } catch (RuntimeException e) {
                e.printStackTrace(System.err);
            }
        }
        dispose();
    }

    public synchronized void decode() {
        if (gc == null || !gc.isValid()) {
            log.fine("WCRenderQueue::decode : GC is " + (gc == null ? "null" : " invalid"));
            return;
        }
        decode(gc);
        gc.flush();
    }

    public synchronized void decode(int fontSmoothingType) {
        if (gc == null || !gc.isValid()) {
            log.fine("WCRenderQueue::decode : GC is " + (gc == null ? "null" : " invalid"));
            return;
        }
        gc.setFontSmoothingType(fontSmoothingType);
        decode();
    }

    protected abstract void flush();

    private void fwkFlush() {
        flush();
    }

    private void fwkAddBuffer(ByteBuffer buffer) {
        addBuffer(buffer);
    }

    public WCRectangle getClip() {
        return clip;
    }

    public synchronized void dispose() {
        int n = buffers.size();
        if (n > 0) {
            int i = 0;
            final Object[] arr = new Object[n];
            for (BufferData bdata: buffers) {
                arr[i++] = bdata.getBuffer();
            }
            buffers.clear();
            Invoker.getInvoker().invokeOnEventThread(() -> {
                twkRelease(arr);
            });
            size = 0;
            if (log.isLoggable(Level.FINE)) {
                log.fine("'}'WCRenderQueue{0}[{1}]",
                        new Object[]{hashCode(), idCountObj.decrementAndGet()});
            }
        }
    }

    protected abstract void disposeGraphics();

    private void fwkDisposeGraphics() {
        disposeGraphics();
    }

    private native void twkRelease(Object[] bufs);

    /*is called from native*/
    private int refString(String str) {
        return currentBuffer.addString(str);
    }

    /*is called from native*/
    private int refIntArr(int[] arr) {
        return currentBuffer.addIntArray(arr);
    }

    /*is called from native*/
    private int refFloatArr(float[] arr) {
        return currentBuffer.addFloatArray(arr);
    }

    public boolean isOpaque() {
        return opaque;
    }

    @Override public synchronized String toString() {
        return "WCRenderQueue{"
                + "clip=" + clip + ", "
                + "size=" + size + ", "
                + "opaque=" + opaque
                + "}";
    }
}

final class BufferData {
    /* For passing data that does not fit into the queue */
    private final AtomicInteger idCount = new AtomicInteger(0);
    private final HashMap<Integer,String> strMap =
            new HashMap<Integer,String>();
    private final HashMap<Integer,int[]> intArrMap =
            new HashMap<Integer,int[]>();
    private final HashMap<Integer,float[]> floatArrMap =
            new HashMap<Integer,float[]>();

    private ByteBuffer buffer;

    private int createID() {
        return idCount.incrementAndGet();
    }

    int addIntArray(int[] a) {
        int id = createID();
        intArrMap.put(id, a);
        return id;
    }

    int[] getIntArray(int id) {
        return intArrMap.get(id);
    }

    int addFloatArray(float[] a) {
        int id = createID();
        floatArrMap.put(id, a);
        return id;
    }

    float[] getFloatArray(int id) {
        return floatArrMap.get(id);
    }

    int addString(String s) {
        int id = createID();
        strMap.put(id, s);
        return id;
    }

    String getString(int id) {
        return strMap.get(id);
    }

    ByteBuffer getBuffer() {
        return buffer;
    }

    void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
