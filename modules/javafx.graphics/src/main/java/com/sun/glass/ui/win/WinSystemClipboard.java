/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

class WinSystemClipboard extends SystemClipboard {

    private static native void initIDs();
    static {
        initIDs();
    }

    private long ptr = 0L; //native pointer

    protected WinSystemClipboard(String name) {
        super(name);
        create();
    }

    protected final long getPtr() {
        return ptr;
    }

    protected native boolean isOwner();

    protected native void create();
    protected native void dispose();

    /*
     * public mime types to system clipboard
     */
    protected native void push(Object[] keys, int supportedActions);

    /*
     * extract clipboard snap-shot
     */
    protected native boolean pop();

    static final byte[] terminator = new byte[] { 0, 0 };
    static final String defaultCharset = "UTF-16LE";
    static final String RTFCharset = "US-ASCII";

    // Called from native code
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

    protected final void pushToSystem(HashMap<String, Object> cacheData, int supportedActions) {
        Set<String> mimes = cacheData.keySet();
        Set<String> mimesForSystem = new HashSet<String>();
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

    private native byte[] popBytes(String mime, long index);
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
                    // RT-16199 - internal Windows data null terminated
                    return new String(data, 0, data.length - 2, defaultCharset);
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
                        // RT-16199 - internal Windows data null terminated
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

    private native String[] popMimesFromSystem();
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

    @Override protected final void close() {
        dispose();
        ptr = 0L;
    }

    @Override protected native void pushTargetActionToSystem(int actionDone);

    private native int popSupportedSourceActions();
    @Override protected int supportedSourceActionsFromSystem() {
        if (!pop()) {
            return ACTION_NONE;
        }
        return popSupportedSourceActions();
   }
}

