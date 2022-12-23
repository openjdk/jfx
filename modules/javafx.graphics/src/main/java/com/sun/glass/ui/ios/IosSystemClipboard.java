/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.ios;

import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.SystemClipboard;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * System (copy/paste) UIPasteboard wrapper class.
 */
class IosSystemClipboard extends SystemClipboard {

    static final String FILE_SCHEME = "file";
    static final private String BAD_URI_MSG = "bad URI in com.sun.glass.ui.ios.IosSystemClipboard for file: ";
    static final private String BAD_URL_MSG = "bad URL in com.sun.glass.ui.ios.IosSystemClipboard for file: ";

    long seed = 0;

    final IosPasteboard pasteboard;

    public IosSystemClipboard(String name) {
        super(name);
        if (name.equals(Clipboard.SYSTEM) == true) {
            this.pasteboard = new IosPasteboard(IosPasteboard.General);
        } else {
            this.pasteboard = new IosPasteboard(name);
        }
    }

    @Override
    protected boolean isOwner() {
        return (this.seed == this.pasteboard.getSeed());
    }

    @Override
    protected void pushToSystem(HashMap<String, Object> data, int supportedActions) {
        HashMap<String,Object> itemFirst = null; // used to handle paste as one item if we can
        HashMap<String,Object> itemList[] = null; // special case: multiple items for handling urls 10.6 style

        java.util.Set keys = data.keySet();
        java.util.Iterator iterator = keys.iterator();

        while (iterator.hasNext() == true) {
            String mime = (String)iterator.next();
            Object object = data.get(mime);

            if (object != null) {
                if (mime.equals(URI_TYPE) == true) {
                    // synthesize list of urls as seperate pasteboard items
                    String list = (String)object;
                    String split[] = list.split("\n");
                    int count = 0;
                    for (int i=0; i<split.length; i++) {
                        String string = split[i];
                        if (string.startsWith("#") == false) {
                            // exclude comments: http://www.ietf.org/rfc/rfc2483.txt
                            count++;
                        }
                    }

                    if (count > 0) {
                        itemList = new HashMap[count];
                        count = 0;
                        for (int i=0; i<split.length; i++) {
                            String file = split[i];
                            if (file.startsWith("#") == false) {
                                // exclude comments: http://www.ietf.org/rfc/rfc2483.txt
                                URI uri = createUri(file, IosSystemClipboard.BAD_URI_MSG);
                                String utf = IosPasteboard.UtfUrl;
                                if (uri.getScheme() == null)
                                {
                                    utf = IosPasteboard.UtfFileUrl;
                                    uri = createUri(IosSystemClipboard.FILE_SCHEME, uri.getPath(), IosSystemClipboard.BAD_URI_MSG);
                                }
                                itemList[count] = new HashMap();
                                itemList[count].put(utf, uri.toASCIIString());
                                count++;
                            }
                        }
                    }
                } else if (mime.equals(RAW_IMAGE_TYPE) == true) {
                    Pixels pixels = (Pixels)object;
                    if (itemFirst == null) {
                        itemFirst = new HashMap();
                    }
                    itemFirst.put(mimeToUtf(mime), pixels);
                } else if ((mime.equals(TEXT_TYPE) == true) ||
                        (mime.equals(HTML_TYPE) == true) ||
                        (mime.equals(RTF_TYPE) == true)) {
                    if (object instanceof String) {
                        String string = (String)object;
                        if (itemFirst == null) {
                            itemFirst = new HashMap();
                        }
                        itemFirst.put(mimeToUtf(mime), string);
                    } else {
                        // http://javafx-jira.kenai.com/browse/RT-14593
                        // temporary code, DelayedCallback trips over this
                        // by reusing (incorrectly) text mime type
                        System.err.println("DelayedCallback not implemented yet: RT-14593");
                        Thread.dumpStack();
                    }
                } else if (mime.equals(FILE_LIST_TYPE)) {
                    // handle FILE_LIST_TYPE last to know whether to handle it as urls (10.6) or file list (10.5)
                    // depending on whether urls have been already explicitly set or not
                    String files[] = (String[])object;
                    if (data.get(URI_TYPE) == null) {
                        // special case no explicit urls found - synthesize urls (Ios OS 10.6 style)
                        itemList = new HashMap[files.length];
                        for (int i=0; i<files.length; i++) {
                            String file = files[i];
                            URI uri = createUri(file, IosSystemClipboard.BAD_URI_MSG);
                            String utf = IosPasteboard.UtfUrl;
                            if (uri.getScheme() == null)
                            {
                                utf = IosPasteboard.UtfFileUrl;
                                uri = createUri(IosSystemClipboard.FILE_SCHEME, uri.getPath(), IosSystemClipboard.BAD_URI_MSG);
                            }
                            itemList[i] = new HashMap();
                            itemList[i].put(utf, uri.toASCIIString());
                        }
                    }
                } else {
                    // custom client mime type - pass through (RT-14592)
                    if (itemFirst == null) {
                        itemFirst = new HashMap();
                    }
                    itemFirst.put(mimeToUtf(mime), serialize(object));
                }
            }
        }

        if (itemFirst != null) {
            if (itemList == null) {
                itemList = new HashMap[1];
                itemList[0] = itemFirst;
            } else {
                HashMap temp = itemList[0];
                itemList[0] = itemFirst;
                iterator = temp.keySet().iterator();
                while (iterator.hasNext() == true) {
                    String utf = (String)iterator.next();
                    Object object = temp.get(utf);
                    itemList[0].put(utf, object);
                }
            }
        }

        if (itemList != null) {
            this.seed = this.pasteboard.putItems(itemList, supportedActions);
        }
    }

    @Override
    protected void pushTargetActionToSystem(int actionDone) {

    }

    @Override
    protected Object popFromSystem(String mimeType) {
        Object object = null;

        String[][] utfs = this.pasteboard.getUTFs();
        if (mimeType.equals(URI_TYPE) == true) {
            if (utfs != null) {
                java.util.ArrayList<String> list = new java.util.ArrayList<>();
                for (int i=0; i<utfs.length; i++) {
                    String url = this.pasteboard.getItemStringForUTF(i, mimeToUtf(URI_TYPE));

                    if (url != null) {
                        list.add(url);
                        break;
                    }
                }
                if (list.size() > 0) {
                    object = list.get(0);
                }
            }
        } else if (mimeType.equals(RAW_IMAGE_TYPE) == true) {
            if (utfs != null) {
                java.util.ArrayList<Pixels> list = new java.util.ArrayList<>();
                for (int i=0; i<utfs.length; i++) {
                    Object data = this.pasteboard.getItemAsRawImage(i);
                    if (data != null) {
                        Pixels pixels = getPixelsForRawImage((byte[])data);
                        list.add(pixels);
                        break;
                    }
                }
                if (list.size() > 0) {
                    object = list.get(0);
                }
            }
        } else if ((mimeType.equals(TEXT_TYPE) == true) ||
                        (mimeType.equals(HTML_TYPE) == true) ||
                            (mimeType.equals(RTF_TYPE) == true)) {
            if (utfs != null) {
                java.util.ArrayList<String> list = new java.util.ArrayList<>();
                for (int i=0; i<utfs.length; i++) {
                    String item = this.pasteboard.getItemStringForUTF(i, mimeToUtf(mimeType));
                    if (item != null) {
                        list.add(item);
                        break;
                    }
                }
                if (list.size() > 0) {
                    object = list.get(0);
                }
            }
        } else if (mimeType.equals(FILE_LIST_TYPE) == true) {
            // synthesize the list from individual URLs
            if (utfs != null) {
                java.util.ArrayList<String> list = new java.util.ArrayList<>();
                for (int i=0; i<utfs.length; i++) {
                    String file = this.pasteboard.getItemStringForUTF(i, IosPasteboard.UtfFileUrl); // explicitly ask for urls
                    if (file != null) {
                        URL url = createUrl(file, IosSystemClipboard.BAD_URL_MSG);
                        list.add(url.getPath());
                    }
                }
                if (list.size() > 0) {
                    object = new String[list.size()];
                    list.toArray((String[])object);
                }
            }
        } else {
            if (utfs != null) {
                java.util.ArrayList<ByteBuffer> list = new java.util.ArrayList<>();
                for (int i=0; i<utfs.length; i++) {
                    byte data[] = this.pasteboard.getItemBytesForUTF(i, mimeToUtf(mimeType));
                    if (data != null) {
                        // http://javafx-jira.kenai.com/browse/RT-14592
                        // custom data - currently we wrap it up in ByteBuffer
                        ByteBuffer bb = ByteBuffer.wrap(data);
                        list.add(bb);
                        break;
                    }
                }
                if (list.size() > 0) {
                    object = list.get(0);
                }
            }
        }
        return object;
    }

    @Override
    protected int supportedSourceActionsFromSystem() {
        return this.pasteboard.getAllowedOperation();
    }


    @Override
    protected String[] mimesFromSystem() {
        String[][] all = this.pasteboard.getUTFs();

        java.util.ArrayList<String> mimes = new java.util.ArrayList<>();

        if (all != null) {
            for (int i=0; i<all.length; i++) {
                String[] utfs = all[i];
                if (utfs != null) {
                    for (int j=0; j<utfs.length; j++) {
                        String utf = utfs[j];
                        String mime = utfToMime(utf);
                        if ((mime != null) && (mimes.contains(mime) == false)) {
                            mimes.add(mime);
                        }
                    }
                }
            }
        }

        String[] strings = new String[mimes.size()];
        mimes.toArray(strings);

        return strings;
    }

    static private HashMap utm = null;
    private synchronized String utfToMime(String utf) {
        if (IosSystemClipboard.utm == null) {
            IosSystemClipboard.utm = new HashMap(6);
            IosSystemClipboard.utm.put(IosPasteboard.UtfString, TEXT_TYPE);
            IosSystemClipboard.utm.put(IosPasteboard.UtfHtml, HTML_TYPE);
            IosSystemClipboard.utm.put(IosPasteboard.UtfRtf, RTF_TYPE);
            IosSystemClipboard.utm.put(IosPasteboard.UtfUrl, URI_TYPE);
            IosSystemClipboard.utm.put(IosPasteboard.UtfFileUrl, FILE_LIST_TYPE);
            IosSystemClipboard.utm.put(IosPasteboard.UtfTiff, RAW_IMAGE_TYPE);
            IosSystemClipboard.utm.put(IosPasteboard.UtfPng, RAW_IMAGE_TYPE);
        }
        if (IosSystemClipboard.utm.containsKey(utf) == true) {
            return (String)IosSystemClipboard.utm.get(utf);
        } else {
            return utf; //utf.replace('.', '/');
        }
    }

    static private HashMap mtu = null;
    private synchronized String mimeToUtf(String mime) {
        if (IosSystemClipboard.mtu == null) {
            IosSystemClipboard.mtu = new HashMap(4);
            IosSystemClipboard.mtu.put(TEXT_TYPE, IosPasteboard.UtfString);
            IosSystemClipboard.mtu.put(HTML_TYPE, IosPasteboard.UtfHtml);
            IosSystemClipboard.mtu.put(RTF_TYPE, IosPasteboard.UtfRtf);
            IosSystemClipboard.mtu.put(URI_TYPE, IosPasteboard.UtfUrl);
            IosSystemClipboard.mtu.put(FILE_LIST_TYPE, IosPasteboard.UtfFileUrl);
            // pass RAW_IMAGE_TYPE through - do NOT substitute with UtfTiff or UtfPng!
        }
        if (IosSystemClipboard.mtu.containsKey(mime) == true) {
            return (String)IosSystemClipboard.mtu.get(mime);
        } else {
            return mime; //mime.replace('/', '.');
        }
    }

    private URI createUri(String path, String message) {
        URI uri = null;
        try {
            uri = new URI(path);
        } catch (URISyntaxException ex) {
            System.err.println(message+path);
        }
        return uri;
    }

    private URI createUri(String scheme, String path, String message) {
        URI uri = null;
        try {
            uri = new URI(scheme, null, path, null);
        } catch (URISyntaxException ex) {
            System.err.println(message+path);
        }
        return uri;
    }

    private URL createUrl(String path, String message) {
        URL url = null;
        try {
            url = new URL(path);
        } catch (MalformedURLException ex) {
            System.err.println(message+path);
        }
        return url;
    }

    private byte[] serialize(Object object) {
        if (object instanceof String) {
            String string = (String)object;
            return string.getBytes();
        } else if (object instanceof ByteBuffer) {
            ByteBuffer buffer = (ByteBuffer)object;
            return buffer.array();
        } else {
            throw new RuntimeException("can not handle " + object);
        }
    }
}
