/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.mac;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.SystemClipboard;
import com.sun.glass.ui.Pixels;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Map;

class MacSystemClipboard extends SystemClipboard {

    static final String FILE_SCHEME = "file";
    static final private String BAD_URI_MSG = "bad URI in com.sun.glass.ui.mac.MacSystemClipboard for file: ";
    static final private String BAD_URL_MSG = "bad URL in com.sun.glass.ui.mac.MacSystemClipboard for file: ";
    
    // if true we'll synthesize a file list
    static final boolean SUPPORT_10_5_API = true;
    
    // if true we'll force the synthesized file list into 1st item as Plain text,
    // regardless of whether such attribute already exists or not
    static final boolean SUPPORT_10_5_API_FORCE = false;
    
    // http://javafx-jira.kenai.com/browse/RT-12187
    // Mac OS X 10.6 supports more than one nonhomogenous item, however, JFX currently does not
    static final boolean SUPPORT_10_6_API = false;
    
    long seed = 0;

    final MacPasteboard pasteboard;
    public MacSystemClipboard(String name) {
        super(name);
        if (name.equals(Clipboard.DND) == true) {
            this.pasteboard = new MacPasteboard(MacPasteboard.DragAndDrop);
        } else if (name.equals(Clipboard.SYSTEM) == true) {
            this.pasteboard = new MacPasteboard(MacPasteboard.General);
        } else {
            this.pasteboard = new MacPasteboard(name);
        }
    }
    
    @Override
    protected boolean isOwner() {
        return (this.seed == this.pasteboard.getSeed());
    }
    
    @Override
    protected int supportedSourceActionsFromSystem() {
        return this.pasteboard.getAllowedOperation();
    }
    
    @Override
    protected void pushTargetActionToSystem(int actionDone) {
        // TODO
    }
    
    @Override
    protected void pushToSystem(HashMap<String, Object> data, int supportedActions) {
        //System.out.println("MacSystemClipboard pushToSystem, supportedActions: "+supportedActions);
        HashMap<String,Object> itemFirst = null; // used to handle paste as one item if we can
        HashMap<String,Object> itemList[] = null; // special case: multiple items for handling urls 10.6 style
        
        java.util.Set keys = data.keySet();
        //System.out.println("keys:"+keys);
        //System.out.println("values:"+data.values());
        java.util.Iterator iterator = keys.iterator();
        while (iterator.hasNext() == true) {
            String mime = (String)iterator.next();
            Object object = data.get(mime);
            //System.out.println("    ["+mime+"] : ["+object+"]");
            if (object != null) {
                if (mime.equals(URI_TYPE) == true) {
                    // synthesize list of urls as seperate pasteboard items (Mac OS 10.6 style)
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
                    //System.out.println("        count:"+count);
                    if (count > 0) {
                        itemList = new HashMap[count];
                        count = 0;
                        for (int i=0; i<split.length; i++) {
                            String file = split[i];
                            if (file.startsWith("#") == false) {
                                // exclude comments: http://www.ietf.org/rfc/rfc2483.txt
                                //System.out.println("            utf:"+MacPasteboard.UtfFileUrl);
                                //System.out.println("            string:"+string);
                                URI uri = createUri(file, MacSystemClipboard.BAD_URI_MSG);
                                String utf = MacPasteboard.UtfUrl;
                                if (uri.getScheme() == null)
                                {
                                    utf = MacPasteboard.UtfFileUrl;
                                    uri = createUri(MacSystemClipboard.FILE_SCHEME, uri.getPath(), MacSystemClipboard.BAD_URI_MSG);
                                }
                                itemList[count] = new HashMap();
                                itemList[count].put(utf, uri.toASCIIString());
                                count++;
                            }
                        }
                    }
                } else if ((mime.equals(RAW_IMAGE_TYPE) == true) ||
                                (mime.equals(DRAG_IMAGE) == true)) {
                    Pixels pixels = null;
                    if (object instanceof Pixels) {
                        pixels = (Pixels) object;
                    } else if (object instanceof ByteBuffer) {
                        try {
                            ByteBuffer bb = (ByteBuffer) object;
                            bb.rewind();
                            pixels = Application.GetApplication().createPixels(bb.getInt(), bb.getInt(), bb.slice());
                        } catch (Exception ex) {
                            //Ignore all ill-sized arrays. Not a client problem.
                        }
                    } else if (object instanceof IntBuffer) {
                        try {
                            IntBuffer ib = (IntBuffer) object;
                            ib.rewind();
                            pixels = Application.GetApplication().createPixels(ib.get(), ib.get(), ib.slice());
                        } catch (Exception ex) {
                            //Ignore all ill-sized arrays. Not a client problem.
                        }
                    } else {
                        throw new RuntimeException(object.getClass().getName() + " cannot be converted to Pixels");
                    }
                    if (pixels != null) {
                        if (itemFirst == null) {
                            itemFirst = new HashMap();
                        }
                        itemFirst.put(FormatEncoder.mimeToUtf(mime), pixels);
                    }
                } else if ((mime.equals(TEXT_TYPE) == true) ||
                                (mime.equals(HTML_TYPE) == true) ||
                                    (mime.equals(RTF_TYPE) == true)) {
                    if (object instanceof String) {
                        String string = (String)object;
                        if (itemFirst == null) {
                            itemFirst = new HashMap();
                        }
                        itemFirst.put(FormatEncoder.mimeToUtf(mime), string);
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
                        // special case no explicit urls found - synthesize urls (Mac OS 10.6 style)
                        itemList = new HashMap[files.length];
                        for (int i=0; i<files.length; i++) {
                            String file = files[i];
                            URI uri = createUri(file, MacSystemClipboard.BAD_URI_MSG);
                            String utf = MacPasteboard.UtfUrl;
                            if (uri.getScheme() == null)
                            {
                                utf = MacPasteboard.UtfFileUrl;
                                uri = createUri(MacSystemClipboard.FILE_SCHEME, uri.getPath(), MacSystemClipboard.BAD_URI_MSG);
                            }
                            itemList[i] = new HashMap();
                            itemList[i].put(utf, uri.toASCIIString());
                        }
                    } else if (MacSystemClipboard.SUPPORT_10_5_API == true) {
                        // special case urls already exist - synthesize file list (Mac OS 10.5 API compatible)
                        if (itemFirst == null) {
                            itemFirst = new HashMap();
                        }
                        StringBuilder string = null;
                        for (int i=0; i<files.length; i++) {
                            String file = files[i];
                            URI uri = createUri(file, MacSystemClipboard.BAD_URI_MSG);
                            if (string == null) {
                                string = new StringBuilder();
                            }
                            string.append(uri.getPath());
                            if (i < (files.length-1)) {
                                string.append("\n");
                            }
                        }
                        if (string != null) {
                            if ((itemFirst.get(MacPasteboard.UtfString) == null) || (MacSystemClipboard.SUPPORT_10_5_API_FORCE == true)) {
                                itemFirst.remove(MacPasteboard.UtfString);
                                itemFirst.put(MacPasteboard.UtfString, string.toString());
                            }
                        }
                    }
                } else {
                    // http://javafx-jira.kenai.com/browse/RT-14592
                    // custom client mime type - pass through
                    if (itemFirst == null) {
                        itemFirst = new HashMap();
                    }
                    itemFirst.put(FormatEncoder.mimeToUtf(mime), serialize(object));
                }
            }
        }
        
//        System.out.println("    itemFirst:"+itemFirst);
//        if (itemFirst != null) {
//            iterator = itemFirst.keySet().iterator();
//            while (iterator.hasNext() == true) {
//                String utf = (String)iterator.next();
//                Object object = itemFirst.get(utf);
//                System.out.println("        "+utf+" : "+object);
//                itemList[0].put(utf, object);
//            }
//        }
            
//        System.out.println("    itemList:"+itemList);
//        if (itemList != null) {
//            for (int i=0; i<itemList.length; i++) {
//                System.out.println("        item["+i+"]");
//                HashMap item = itemList[i];
//                iterator = item.keySet().iterator();
//                while (iterator.hasNext() == true) {
//                    String utf = (String)iterator.next();
//                    Object object = item.get(utf);
//                    System.out.println("            "+utf+" : "+object);
//                    itemList[0].put(utf, object);
//                }
//            }
//        }
        
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
        
//        System.out.println("pushToSystem final items:"+itemList);
//        if (itemList != null) {
//            for (int i=0; i<itemList.length; i++) {
//                System.out.println("        item["+i+"]");
//                HashMap item = itemList[i];
//                iterator = item.keySet().iterator();
//                while (iterator.hasNext() == true) {
//                    String utf = (String)iterator.next();
//                    Object object = item.get(utf);
//                    System.out.println("            "+utf+" : "+object);
//                    itemList[0].put(utf, object);
//                }
//            }
//        }
        
        if (itemList != null) {
            this.seed = this.pasteboard.putItems(itemList, supportedActions);
        }
    }
    
    @Override
    protected Object popFromSystem(String mime) {
        //System.err.println("popFromSystem: ["+mime+"]");
        Object object = null;

        String[][] utfs = this.pasteboard.getUTFs();
        if (mime.equals(URI_TYPE) == true) {
            if (utfs != null) {
                java.util.ArrayList<String> list = new java.util.ArrayList<String>();
                for (int i=0; i<utfs.length; i++) {
                    String url = this.pasteboard.getItemStringForUTF(i, FormatEncoder.mimeToUtf(URI_TYPE));
                    //System.out.println("    url: "+url);
                    if (url != null) {
                        list.add(url);
                        if (SUPPORT_10_6_API == false) {
                            break;
                        }
                    }
                }
                if (list.size() > 0) {
                    if (SUPPORT_10_6_API == false) {
                        object = list.get(0);
                    } else {
                        object = list;
                    }
                }
            }
        } else if (mime.equals(RAW_IMAGE_TYPE) == true) {
            if (utfs != null) {
                java.util.ArrayList<Pixels> list = new java.util.ArrayList<Pixels>();
                for (int i=0; i<utfs.length; i++) {
                    Object data = this.pasteboard.getItemAsRawImage(i);
                    if (data != null) {
                        Pixels pixels = getPixelsForRawImage((byte[])data);
                        list.add(pixels);
                        if (SUPPORT_10_6_API == false) {
                            break;
                        }
                    }
                }
                if (list.size() > 0) {
                    if (SUPPORT_10_6_API == false) {
                        object = list.get(0);
                    } else {
                        object = list;
                    }
                }
            }
        } else if ((mime.equals(TEXT_TYPE) == true) ||
                        (mime.equals(HTML_TYPE) == true) ||
                            (mime.equals(RTF_TYPE) == true)) {
            if (utfs != null) {
                java.util.ArrayList<String> list = new java.util.ArrayList<String>();
                for (int i=0; i<utfs.length; i++) {
                    String item = this.pasteboard.getItemStringForUTF(i, FormatEncoder.mimeToUtf(mime));
                    if (item != null) {
                        list.add(item);
                        if (SUPPORT_10_6_API == false) {
                            break;
                        }
                    }
                }
                if (list.size() > 0) {
                    if (SUPPORT_10_6_API == false) {
                        object = list.get(0);
                    } else {
                        object = list;
                    }
                }
            }
        } else if (mime.equals(FILE_LIST_TYPE) == true) {
            // synthesize the list from individual URLs
            if (utfs != null) {
                java.util.ArrayList<String> list = new java.util.ArrayList<String>();
                for (int i=0; i<utfs.length; i++) {
                    String file = this.pasteboard.getItemStringForUTF(i, MacPasteboard.UtfFileUrl); // explicitly ask for urls
                    if (file != null) {
                        URL url = createUrl(file, MacSystemClipboard.BAD_URL_MSG);
                        list.add(url.getPath());
                        //System.out.println("    url: "+url.getPath());
                    }
                }
                if (list.size() > 0) {
                    object = new String[list.size()];
                    list.toArray((String[])object);
                }
            }
        } else {
            if (utfs != null) {
                java.util.ArrayList<ByteBuffer> list = new java.util.ArrayList<ByteBuffer>();
                for (int i=0; i<utfs.length; i++) {
                    byte data[] = this.pasteboard.getItemBytesForUTF(i, FormatEncoder.mimeToUtf(mime));
                    if (data != null) {
                        // http://javafx-jira.kenai.com/browse/RT-14592
                        // custom data - currently we wrap it up in ByteBuffer
                        ByteBuffer bb = ByteBuffer.wrap(data);
                        list.add(bb);
                        if (SUPPORT_10_6_API == false) {
                            break;
                        }
                    }
                }
                if (list.size() > 0) {
                    if (SUPPORT_10_6_API == false) {
                        object = list.get(0);
                    } else {
                        object = list;
                    }
                }
            }
        }
        
        //System.out.println("    object: "+object);
        return object;
    }
    
    @Override
    protected String[] mimesFromSystem() {
        String[][] all = this.pasteboard.getUTFs();

        java.util.ArrayList<String> mimes = new java.util.ArrayList<String>();

        //System.out.println("mimesFromSystem");
        //System.out.println("    utfs:");
        if (all != null) {
            for (int i=0; i<all.length; i++) {
                String[] utfs = all[i];
                if (utfs != null) {
                    for (int j=0; j<utfs.length; j++) {
                        String utf = utfs[j];
                        //System.out.println("        utfs["+i+"]:["+j+"]:"+utf);
                        String mime = FormatEncoder.utfToMime(utf);
                        //System.out.println("        mime:"+mime);
                        if ((mime != null) && (mimes.contains(mime) == false)) {
                            mimes.add(mime);
                        }
                    }
                }
            }
        }
        
        String[] strings = new String[mimes.size()];
        mimes.toArray(strings);
        
        //System.err.println("MacSystemClipboard mimesFromSystem:");
        //for (int i=0; i<strings.length; i++) {
        //    System.err.println("    mime["+i+"]:"+strings[i]);
        //}
        
        return strings;
    }
    
    @Override public String toString() {
        return "Mac OS X "+this.pasteboard.getName()+" Clipboard";
    }

    private static class FormatEncoder {
        private static final String DYNAMIC_UTI_PREFIX = "dyn.";

        private static final Map<String, String> utm = new HashMap<>();
        private static final Map<String, String> mtu = new HashMap<>();

        static {
            utm.put(MacPasteboard.UtfString, TEXT_TYPE);
            utm.put(MacPasteboard.UtfHtml, HTML_TYPE);
            utm.put(MacPasteboard.UtfRtf, RTF_TYPE);
            utm.put(MacPasteboard.UtfUrl, URI_TYPE);
            utm.put(MacPasteboard.UtfFileUrl, FILE_LIST_TYPE);
            utm.put(MacPasteboard.UtfTiff, RAW_IMAGE_TYPE);
            utm.put(MacPasteboard.UtfPng, RAW_IMAGE_TYPE);
            utm.put(MacPasteboard.UtfRawImageType, RAW_IMAGE_TYPE);
            utm.put(MacPasteboard.UtfDragImageType, DRAG_IMAGE);
            utm.put(MacPasteboard.UtfDragImageOffset, DRAG_IMAGE_OFFSET);

            mtu.put(TEXT_TYPE, MacPasteboard.UtfString);
            mtu.put(HTML_TYPE, MacPasteboard.UtfHtml);
            mtu.put(RTF_TYPE, MacPasteboard.UtfRtf);
            mtu.put(URI_TYPE, MacPasteboard.UtfUrl);
            mtu.put(FILE_LIST_TYPE, MacPasteboard.UtfFileUrl);
            mtu.put(RAW_IMAGE_TYPE, MacPasteboard.UtfRawImageType);
            mtu.put(DRAG_IMAGE, MacPasteboard.UtfDragImageType);
            mtu.put(DRAG_IMAGE_OFFSET, MacPasteboard.UtfDragImageOffset);
        }

        public static synchronized String mimeToUtf(String mime) {
            if (mtu.containsKey(mime)) {
                return mtu.get(mime);
            }
            String encodedUTI = _convertMIMEtoUTI(mime);
            mtu.put(mime, encodedUTI);
            utm.put(encodedUTI, mime);
            return encodedUTI;
        }

        public static synchronized String utfToMime(String uti) {
            if (utm.containsKey(uti)) {
                return utm.get(uti);
            }
            if (uti.startsWith(DYNAMIC_UTI_PREFIX)) {
                String decodedMIME = _convertUTItoMIME(uti);
                mtu.put(decodedMIME, uti);
                utm.put(uti, decodedMIME);
                return decodedMIME;
            }
            //Do not know who encoded it, pass-throw
            return uti;
        }

        private static native String _convertMIMEtoUTI(String mime);
        private static native String _convertUTItoMIME(String uti);
    }
    
    private URI createUri(String path, String message) {
        URI uri = null;
        try {
            uri = new URI(path);
        } catch (URISyntaxException ex) {
            System.err.println(message+path);
            Thread.dumpStack();
            //throw new RuntimeException(ex);
        }
        return uri;
    }
    
    private URI createUri(String scheme, String path, String message) {
        URI uri = null;
        try {
            uri = new URI(scheme, null, path, null);
        } catch (URISyntaxException ex) {
            System.err.println(message+path);
            Thread.dumpStack();
            //throw new RuntimeException(ex);
        }
        return uri;
    }
    
    private URL createUrl(String path, String message) {
        URL url = null;
        try {
            url = new URL(path);
        } catch (MalformedURLException ex) {
            System.err.println(message+path);
            Thread.dumpStack();
            //throw new RuntimeException(ex);
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
            throw new RuntimeException("can not handle "+object);
        }
    }
}
