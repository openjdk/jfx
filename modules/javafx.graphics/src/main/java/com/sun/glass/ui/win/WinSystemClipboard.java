/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.win;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.SystemClipboard;
import java.io.UnsupportedEncodingException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Windows system clipboard peer on the {@code gwin_clipboard_*} ABI of {@code glass_win_api.h}.
 * Every former {@code native} of this class has a {@code gwin_*} twin - the
 * {@code OleSetClipboard} / {@code OleGetClipboard} / {@code IDataObject} bodies of
 * {@code GlassClipboard.cpp} stay in C, because {@code ClipboardData} is a COM vtable that Windows and
 * other processes call through the OLE marshaller - except {@code isOwner}, whose whole JNI body was
 * {@code ole32!OleIsCurrentClipboard} behind a NULL test and which {@link WinGlassNative} binds
 * directly, and {@code initIDs}, which cached the three method ids and one field id the callback
 * table below replaces.
 * <p>
 * <b>The handle.</b> {@link #ptr} is the {@code IDataObject*} the C holds for this peer: the
 * library's own {@code ClipboardData} after a push, a foreign object after a pop, {@code NULL} for
 * none. The push exports return a <em>status</em>, not the handle: the C publishes the new object
 * through the {@code set_data_object} slot ({@link #dispatchSetDataObject}) at the instant the JNI
 * wrote this field - before {@code pushCommit}, {@code OleSetClipboard} and {@code DoDragDrop} - so
 * that a {@code content_changed} nested inside {@code OleSetClipboard} (Windows sends
 * {@code WM_DRAWCLIPBOARD} synchronously from inside it) or a self-drag's {@code drag_enter} inside
 * {@code DoDragDrop} already sees it. A handle returned from the push would re-store an object this
 * peer may have closed in between. {@link #pop} stores the return of {@code gwin_clipboard_pop}
 * unconditionally, {@code NULL} on failure, exactly as the JNI did.
 * <p>
 * <b>The registry.</b> The C never holds a Java reference; every slot carries the {@code int64_t}
 * id this peer registered itself under in its constructor, before {@link #create} could name it.
 * The JNI's {@code ClipboardData} held a global ref on the peer for its own lifetime, and a
 * {@code ClipboardData} can outlive {@link #close}: when another application holds the clipboard
 * open, the {@code OleFlushClipboard} retries of {@code gwin_clipboard_dispose} give up and the
 * object stays on the OLE clipboard, and the next cross-process paste still reaches
 * {@code fos_serialize} for this id. The entry is therefore strong and is removed only once the
 * peer is closed <em>and</em> every {@code ClipboardData} bound to it has fired
 * {@code data_object_disposed} ({@link #liveObjects}, counted up when {@code set_data_object}
 * publishes a non-NULL object and down when {@code ~ClipboardData} runs) - from whichever of
 * {@link #close} and {@link #dispatchDataObjectDisposed} gets there last.
 * <p>
 * <b>Threads and re-entrancy.</b> Everything here runs on the JavaFX application thread, which is
 * the Glass toolkit STA; every downcall can pump messages and every slot can arrive nested inside a
 * downcall or with no Java frame below it at all (a paste from another application). No method
 * takes a lock the C could re-enter through.
 */
class WinSystemClipboard extends SystemClipboard {

    /**
     * Every open peer by the id it registered under: strong, so that a {@code fos_serialize} for a
     * {@code ClipboardData} that outlived {@link #close} still finds its data (see the class
     * comment). Read by the seven {@code dispatch*} statics from inside upcall stubs on the
     * application thread; written in the constructor, {@link #close} and
     * {@link #dispatchDataObjectDisposed}.
     */
    private static final Map<Long, WinSystemClipboard> CLIPBOARDS = new ConcurrentHashMap<>();

    /** Ids count up from 1; 0 is what the C carries for "no peer" and is never handed out. */
    private static final AtomicLong NEXT_CLIPBOARD_ID = new AtomicLong(1L);

    /** Deliberately no initializer: it is assigned in the constructor after {@code super()} ran. */
    private long nativeId;

    /** The {@code IDataObject*} the C holds for this peer; see the class comment for who writes it. */
    private MemorySegment ptr = MemorySegment.NULL;

    /** {@code ClipboardData} objects bound to {@link #nativeId} that the C has not destroyed yet. */
    private int liveObjects;

    /** Set by {@link #close}; with {@link #liveObjects} it decides when the registry entry goes. */
    private boolean closed;

    protected WinSystemClipboard(String name) {
        super(name);
        nativeId = register(this);
        create();
    }

    /** Registers {@code clipboard} under a fresh id and returns it. */
    private static long register(WinSystemClipboard clipboard) {
        long id = NEXT_CLIPBOARD_ID.getAndIncrement();
        CLIPBOARDS.put(id, clipboard);
        return id;
    }

    /** Drops the entry for {@code id}, whatever its state; for tests that never close a peer. */
    static void unregister(long id) {
        CLIPBOARDS.remove(id);
    }

    static int clipboardRegistrySize() {
        return CLIPBOARDS.size();
    }

    /** The peer registered under {@code id}, or {@code null} for 0 and for an id that is gone. */
    static WinSystemClipboard clipboardFor(long id) {
        return CLIPBOARDS.get(id);
    }

    /** The id every clipboard slot carries for this peer. */
    final long nativeId() {
        return nativeId;
    }

    protected final MemorySegment getPtr() {
        return ptr;
    }

    /**
     * The {@code dnd_set_data_object} write: the object being dragged <em>in</em> from elsewhere,
     * which the C AddRef'd for the drag clipboard. Not a {@code ClipboardData} of this peer, so it
     * is not counted in {@link #liveObjects}; only {@link WinDnDClipboard} calls this.
     */
    final void setPtr(MemorySegment dataObject) {
        ptr = dataObject;
    }

    final int liveObjects() {
        return liveObjects;
    }

    final boolean isClosed() {
        return closed;
    }

    /**
     * {@code ole32!OleIsCurrentClipboard(ptr) == S_OK}, with the JNI body's {@code ptr == NULL ->
     * false} short-circuit first ({@code GlassClipboard.cpp}, the former {@code isOwner} export).
     */
    @Override
    protected boolean isOwner() {
        return WinGlassNative.oleIsCurrentClipboard(ptr);
    }

    /**
     * {@code gwin_clipboard_register_viewer}: joins the clipboard viewer chain for
     * {@link #contentChanged} and, if a previous peer was registered, disposes it first through the
     * {@code dispose_peer} slot. The status is ignored because the JNI was {@code void}: without a
     * toolkit it did nothing, and so does this.
     */
    protected void create() {
        int ignoredStatus = WinGlassNative.clipboardRegisterViewer(nativeId);
    }

    /**
     * {@code gwin_clipboard_dispose}: leaves the viewer chain, flushes the delayed data if this
     * object is the current OLE clipboard (so a paste still works after the application quit), then
     * releases it. Pumps messages; re-entrant. The library does not null the handle, {@link #close}
     * does.
     */
    protected void dispose() {
        WinGlassNative.clipboardDispose(ptr);
    }

    /*
     * public mime types to system clipboard: gwin_clipboard_push. The status (GWIN_ERR_OLE when
     * pushCommit or OleSetClipboard failed) is ignored to stay neutral - the JNI swallowed the
     * HRESULT - and the handle is NOT taken from the call: set_data_object stored it already, even
     * on the failure path, so the peer owns the object either way and the next push or dispose
     * releases it, as it always did.
     */
    protected void push(Object[] keys, int supportedActions) {
        int ignoredStatus = WinGlassNative.clipboardPush(ptr, nativeId, keys, supportedActions);
    }

    /*
     * extract clipboard snap-shot: gwin_clipboard_pop releases the old object whether or not
     * OleGetClipboard succeeds and answers the new one, or NULL - stored unconditionally, as the
     * JNI's setPtr was.
     */
    protected boolean pop() {
        ptr = WinGlassNative.clipboardPop(ptr);
        return !MemorySegment.NULL.equals(ptr);
    }

    static final byte[] terminator = new byte[] { 0, 0 };
    static final String defaultCharset = "UTF-16LE";
    static final String RTFCharset = "US-ASCII";

    // Called from native code, through dispatchFosSerialize
    private byte[] fosSerialize(String mime, long index) {
        Object data = getLocalData(mime);
        if (data instanceof ByteBuffer) {
            byte[] b = ((ByteBuffer)data).array();
            if (HTML_TYPE.equals(mime)) {
                b = WinHTMLCodec.encode(b);
            }
            return b;
        } else if (data instanceof String) {
            String st = ((String) data).replaceAll("(\r\n|\r|\n)", "\r\n");
            if (HTML_TYPE.equals(mime)) {
                try {
                    // NOTE: Transfer of HTML data on Windows uses UTF-8 encoding!
                    byte[] bytes = st.getBytes(WinHTMLCodec.defaultCharset);
                    ByteBuffer ba = ByteBuffer.allocate(bytes.length + 1);
                    ba.put(bytes);
                    ba.put((byte)0);

                    return WinHTMLCodec.encode(ba.array());
                } catch (UnsupportedEncodingException ex) {
                    // never happen
                    return null;
                }
            } else if (RTF_TYPE.equals(mime)) {
                try {
                    // NOTE: Transfer of RTF data on Windows uses US-ASCII encoding!
                    byte[] bytes = st.getBytes(RTFCharset);
                    ByteBuffer ba = ByteBuffer.allocate(bytes.length + 1);
                    ba.put(bytes);
                    ba.put((byte)0);
                    return ba.array();
                } catch (UnsupportedEncodingException ex) {
                    // could happen on user error
                    return null;
                }
            } else {
                ByteBuffer ba = ByteBuffer.allocate((st.length() + 1) * 2);
                try {
                    ba.put(st.getBytes(defaultCharset));
                } catch (UnsupportedEncodingException ex) {
                    //never happen
                }
                ba.put(terminator);
                return ba.array();
            }
        } else if (FILE_LIST_TYPE.equals(mime)) {
            String[] ast = ((String[]) data);
            if (ast != null && ast.length > 0) {
                int size = 0;
                for (String st : ast) {
                    size += (st.length() + 1) * 2;
                }
                size += 2;
                try {
                    ByteBuffer ba = ByteBuffer.allocate(size);
                    for (String st : ast) {
                        ba.put(st.getBytes(defaultCharset));
                        ba.put(terminator);
                    }
                    ba.put(terminator);
                    return ba.array();
                } catch (UnsupportedEncodingException ex) {
                    //never happen
                }
            }
        } else if (RAW_IMAGE_TYPE.equals(mime)) {
            Pixels pxls = (Pixels)data;
            if (pxls != null) {
                ByteBuffer ba = ByteBuffer.allocate(
                        pxls.getWidth() * pxls.getHeight() * 4 + 8);
                ba.putInt(pxls.getWidth());
                ba.putInt(pxls.getHeight());
                ba.put(pxls.asByteBuffer());
                return ba.array();
            }
        }
        //TODO: customizes for OS specific cases
        return null;
    }

    /*
     * The dispatch half of the seven GwinClipboardCallbacks slots: registry lookup, then the
     * instance method. The marshalling and the exception barrier are WinGlassNative's; an id the
     * registry does not know is a stale peer and answers the slot default silently. All on the
     * application thread, possibly with no Java frame below, possibly nested in a downcall.
     */

    /**
     * {@code fos_serialize}: the delayed render of one format for whoever pastes. {@code null} when
     * the peer is gone (the C answers {@code E_POINTER}, as it did for a null {@code byte[]}).
     */
    static byte[] dispatchFosSerialize(long clipboardId, String mime, long index) {
        WinSystemClipboard clipboard = CLIPBOARDS.get(clipboardId);
        return clipboard == null ? null : clipboard.fosSerialize(mime, index);
    }

    /** {@code action_performed} and {@code drag_action_performed}: {@code Clipboard.actionPerformed(int)}. */
    static void dispatchActionPerformed(long clipboardId, int action) {
        WinSystemClipboard clipboard = CLIPBOARDS.get(clipboardId);
        if (clipboard != null) {
            clipboard.actionPerformed(action);
        }
    }

    /** {@code content_changed}: {@code Clipboard.contentChanged()} from {@code WM_DRAWCLIPBOARD}. */
    static void dispatchContentChanged(long clipboardId) {
        WinSystemClipboard clipboard = CLIPBOARDS.get(clipboardId);
        if (clipboard != null) {
            clipboard.contentChanged();
        }
    }

    /**
     * {@code dispose_peer}: the previously registered peer, disposed from inside the next
     * {@code gwin_clipboard_register_viewer} (a second {@link #create}) or from the toolkit window's
     * {@code WM_DESTROY} - the flush that keeps a paste working after the application quit. The
     * JNI ran the dispose export on the old peer and left its field alone, so a later
     * {@link #close} released a dead object; the handle is nulled here instead, which the header
     * allows and which is the one divergence on this path.
     */
    static void dispatchDisposePeer(long clipboardId) {
        WinSystemClipboard clipboard = CLIPBOARDS.get(clipboardId);
        if (clipboard != null) {
            clipboard.dispose();
            clipboard.ptr = MemorySegment.NULL;
        }
    }

    /**
     * {@code set_data_object}: the JNI's {@code setPtr}, fired by both push exports right after the
     * {@code ClipboardData} is constructed and before anything can fail or upcall. A non-NULL
     * object is one more the C will destroy later.
     */
    static void dispatchSetDataObject(long clipboardId, MemorySegment dataObject) {
        WinSystemClipboard clipboard = CLIPBOARDS.get(clipboardId);
        if (clipboard != null) {
            clipboard.ptr = dataObject;
            if (!MemorySegment.NULL.equals(dataObject)) {
                clipboard.liveObjects++;
            }
        }
    }

    /**
     * {@code data_object_disposed}: {@code ~ClipboardData}, where the JNI's {@code DeleteGlobalRef}
     * was - possibly long after {@link #close}. Touches the registry and nothing else.
     */
    static void dispatchDataObjectDisposed(long clipboardId) {
        WinSystemClipboard clipboard = CLIPBOARDS.get(clipboardId);
        if (clipboard != null) {
            if (clipboard.liveObjects > 0) {
                clipboard.liveObjects--;
            }
            clipboard.removeIfDead();
        }
    }

    private void removeIfDead() {
        if (closed && liveObjects == 0) {
            CLIPBOARDS.remove(nativeId);
        }
    }

    private static final class MimeTypeParser {
        protected static final String externalBodyMime = "message/external-body";
        protected String mime;
        protected boolean bInMemoryFile;
        protected int index;

        public MimeTypeParser() {
            parse("");
        }

        public MimeTypeParser(String mimeFull) {
            parse(mimeFull);
        }

        public void parse(String mimeFull) {
            mime = mimeFull;
            bInMemoryFile = false;
            index = -1;
            //we are limited here by [message/external-body] mime type with clipboard acess-type,
            //because NetBeans has a clipboard format that includes [;]
            if (mimeFull.startsWith(externalBodyMime)) {
                String mimeParts[] = mimeFull.split(";");
                String accessType = "";
                int indexValue = -1;
                //RFC 1521 extension for [message/external-body] mime
                for (int i = 1; i < mimeParts.length; ++i) {
                    String params[] = mimeParts[i].split("=");
                    if (params.length == 2) {
                        if( params[0].trim().equalsIgnoreCase("index") ) {
                            //that is OK to have the runtime-exception here
                            //we already have a chance to have an exception in WinHTMLCodec
                            indexValue = Integer.parseInt(params[1].trim());
                        } else if( params[0].trim().equalsIgnoreCase("access-type") ) {
                            accessType = params[1].trim();
                        }
                    }
                    if (indexValue != -1 && !accessType.isEmpty()) {
                        //Better to stop here to avoid problem with "index=100.url" filename
                        //it is not a security problem - we can request any index without
                        //buffer overflow or null pointer exception
                        break;
                    }
                }
                //we are responsible only for FX synthetic access type!
                if (accessType.equalsIgnoreCase("clipboard")) {
                    bInMemoryFile = true;
                    mime = mimeParts[0];
                    index = indexValue;
                }
            }
        }

        public String getMime() {
            return mime;
        }

        public int getIndex() {
            return index;
        }

        public boolean isInMemoryFile() {
            return bInMemoryFile;
        }
    }

    @Override
    protected final void pushToSystem(HashMap<String, Object> cacheData, int supportedActions) {
        Set<String> mimes = cacheData.keySet();
        Set<String> mimesForSystem = new HashSet<>();
        MimeTypeParser parser = new MimeTypeParser();
        for (String mime : mimes) {
            parser.parse(mime);
            if ( !parser.isInMemoryFile() ) {
                //[message/external-body] mime with [access-type=clipboard]
                //could not be exported to the system due to synthetic nature (Win-API subst),
                //but it could be used for applcation-wide communication
                mimesForSystem.add(mime);
            }
        }
        push(mimesForSystem.toArray(), supportedActions);
    }

    /**
     * {@code gwin_clipboard_pop_bytes}: {@code null} for no data, an empty medium and an OLE failure
     * alike, as the JNI's null {@code byte[]} was - {@link #popFromSystem} relies on the conflation
     * for its {@code ;locale} and file-list fallbacks.
     */
    private byte[] popBytes(String mime, long index) {
        return WinGlassNative.clipboardPopBytes(ptr, mime, index);
    }

    @Override
    protected final Object popFromSystem(String mimeFull) {
        //we have to syncronize with system ones per
        //a popFromSystem function call, because
        //mime type data could be a collection of
        //sub-mimes likes "ms-stuff/XXXX"
        if ( !pop() ) {
            return null;
        }

        MimeTypeParser parser = new MimeTypeParser(mimeFull);
        String mime = parser.getMime();
        byte[] data = popBytes(mime, parser.getIndex());
        if (data != null) {
            if (TEXT_TYPE.equals(mime) || URI_TYPE.equals(mime)) {
                try {
                    return new String(data, 0, data.length, defaultCharset);
                } catch (UnsupportedEncodingException ex) {
                    //never happen
                }
            } else if (HTML_TYPE.equals(mime)) {
                try {
                    data = WinHTMLCodec.decode(data);
                    return new String(data, 0, data.length, WinHTMLCodec.defaultCharset);
                } catch (UnsupportedEncodingException ex) {
                    //never happen
                }
            } else if (RTF_TYPE.equals(mime)) {
                try {
                    return new String(data, 0, data.length, RTFCharset);
                } catch (UnsupportedEncodingException ex) {
                    //can happen for bad system data
                }
            } else if (FILE_LIST_TYPE.equals(mime)) {
                try {
                    String st = new String(data, 0, data.length, defaultCharset);
                    return st.split("\0");
                } catch (UnsupportedEncodingException ex) {
                    //never happen
                }
            } else if (RAW_IMAGE_TYPE.equals(mime)) {
                ByteBuffer size = ByteBuffer.wrap(data, 0, 8);
                return Application.GetApplication().createPixels(size.getInt(), size.getInt(),  ByteBuffer.wrap(data, 8, data.length - 8) );
            } else {
                return ByteBuffer.wrap(data);
            }
        } else {
            //alternative extraction if any
            if (URI_TYPE.equals(mime) || TEXT_TYPE.equals(mime)) {
                //try 8bit version
                data = popBytes(mime + ";locale", parser.getIndex());
                if (data != null) {
                    try {
                        // JDK-8118474 - internal Windows data null terminated
                        // Here we can request the "ms-stuff/locale" mime data
                        // from GlassClipbord for codepage detection, but
                        // for the most of cases [UTF-8] is ok.
                        return new String(data, 0, data.length - 1, "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        //could happen, but not a problem
                    }
                }
            }
            if (URI_TYPE.equals(mime)) {
                //we are here if [text/uri-list;locale] mime is absent or
                //URL could not be decoded from the [data] as String
                String[] ret = (String[])popFromSystem(FILE_LIST_TYPE);
                if (ret != null) {
                    StringBuilder out = new StringBuilder();
                    //"text/uri-list" spec: http://www.ietf.org/rfc/rfc2483.txt
                    for (int i = 0; i < ret.length; i++) {
                        String fileName = ret[i];
                        fileName = fileName.replace("\\", "/");
                        //fileName = fileName.replace(" ", "%20");
                        if (out.length() > 0) {
                            out.append("\r\n");
                        }
                        out.append("file:/").append(fileName);
                    }
                    return out.toString();
                }
            }
        }
        return null;
    }

    /**
     * {@code gwin_clipboard_pop_mimes}: {@code null} when there is no data object or the set came
     * out empty, else the mimes in an order that was never specified on either side.
     */
    private String[] popMimesFromSystem() {
        return WinGlassNative.clipboardPopMimes(ptr);
    }

    @Override
    protected final String[] mimesFromSystem() {
        //we have to syncronize with system
        //if we heed to do it. DnD clipboard need not.
        if (!pop()) {
            return null;
        }
        return popMimesFromSystem();
    }

    @Override public String toString() {
        return "Windows System Clipboard";
    }

    /**
     * Disposes, nulls the handle (the library cannot), and drops the registry entry - now, or when
     * the last {@code ClipboardData} bound to this peer is destroyed (see the class comment).
     */
    @Override protected final void close() {
        dispose();
        ptr = MemorySegment.NULL;
        closed = true;
        removeIfDead();
    }

    /** {@code gwin_clipboard_push_target_action}: silent on failure and on a NULL handle, like the JNI. */
    @Override protected void pushTargetActionToSystem(int actionDone) {
        WinGlassNative.clipboardPushTargetAction(ptr, actionDone);
    }

    private int popSupportedSourceActions() {
        return WinGlassNative.clipboardPopSupportedActions(ptr);
    }

    @Override protected int supportedSourceActionsFromSystem() {
        if (!pop()) {
            return ACTION_NONE;
        }
        return popSupportedSourceActions();
   }
}
