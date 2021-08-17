/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.image.Image;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;
import javafx.util.Pair;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.Pixels;
import com.sun.javafx.tk.ImageLoader;
import com.sun.javafx.tk.PermissionHelper;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.image.PixelReader;
import java.io.ObjectStreamClass;
import javafx.scene.image.WritablePixelFormat;

/**
 * The implementation of TKClipboard, which is used both for clipboards
 * and dragboards.
 */
final class QuantumClipboard implements TKClipboard {

    /**
     * Handle to the Glass peer.
     */
    private ClipboardAssistance systemAssistant;

    /**
     * Security access context for image loading
     *      com.sun.javafx.tk.quantum.QuantumClipboard
     *      javafx.scene.input.Clipboard
     *          ... user code ...
     */
    @SuppressWarnings("removal")
    private AccessControlContext accessContext = null;

    /**
     * Distinguishes between clipboard and dragboard. This is needed
     * because dragboard's flush() starts DnD operation so it mustn't be
     * called too early.
     */
    private boolean isCaching;

    /**
     * Cache of the data used for dragboard between setting them and flushing
     * them to system dragboard.
     */
    private List<Pair<DataFormat, Object>> dataCache;

    /**
     * Cache of the transfer modes used for dragboard between setting them and
     * flushing them to system dragboard.
     */
    private Set<TransferMode> transferModesCache;

    /**
     * An image which is displayed during the drag operation. Set by the user.
     */
    private Image dragImage = null;

    /**
     * An offset of the image which is displayed during the drag operation.
     * Set by the user.
     */
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private static ClipboardAssistance currentDragboard;

    /**
     * Disallow direct creation of QuantumClipboard
     */
    private QuantumClipboard() {
    }

    @Override public void setSecurityContext(@SuppressWarnings("removal") AccessControlContext acc) {
        if (accessContext != null) {
            throw new RuntimeException("Clipboard security context has been already set!");
        }
        accessContext = acc;
    }

    @SuppressWarnings("removal")
    private AccessControlContext getAccessControlContext() {
        if (accessContext == null) {
            throw new RuntimeException("Clipboard security context has not been set!");
        }
        return accessContext;
    }

    /**
     * Gets an instance of QuantumClipboard for the given assistant. This may be
     * a new instance after each call.
     * @param assistant
     * @return
     */
    public static QuantumClipboard getClipboardInstance(ClipboardAssistance assistant) {
        QuantumClipboard c = new QuantumClipboard();
        c.systemAssistant = assistant;
        c.isCaching = false;
        return c;
    }

    static ClipboardAssistance getCurrentDragboard() {
        return currentDragboard;
    }

    static void releaseCurrentDragboard() {
        // RT-34510: assert currentDragboard != null;
        currentDragboard = null;
    }

    /**
     * Gets an instance of QuantumClipboard for the given assistant for usage
     * as dragboard during drag and drop. It doesn't flush the data implicitly
     * and caches them until flush() is called. This may be
     * a new instance after each call.
     * @param assistant
     * @return
     */
    public static QuantumClipboard getDragboardInstance(ClipboardAssistance assistant, boolean isDragSource) {
        QuantumClipboard c = new QuantumClipboard();
        c.systemAssistant = assistant;
        c.isCaching = true;
        if (isDragSource) {
            currentDragboard = assistant;
        }
        return c;
    }

    public static int transferModesToClipboardActions(final Set<TransferMode> tms) {
        int actions = Clipboard.ACTION_NONE;
        for (TransferMode t : tms) {
            switch (t) {
                case COPY:
                    actions |= Clipboard.ACTION_COPY;
                    break;
                case MOVE:
                    actions |= Clipboard.ACTION_MOVE;
                    break;
                case LINK:
                    actions |= Clipboard.ACTION_REFERENCE;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "unsupported TransferMode " + tms);
            }
        }
        return actions;
    }

    public void setSupportedTransferMode(Set<TransferMode> tm) {
        if (isCaching) {
            transferModesCache = tm;
        }
        final int actions = transferModesToClipboardActions(tm);
        systemAssistant.setSupportedActions(actions);
    }

    public static Set<TransferMode> clipboardActionsToTransferModes(final int actions) {
        final Set<TransferMode> tms = EnumSet.noneOf(TransferMode.class);

        if ((actions & Clipboard.ACTION_COPY) != 0) {
            tms.add(TransferMode.COPY);
        }
        if ((actions & Clipboard.ACTION_MOVE) != 0) {
            tms.add(TransferMode.MOVE);
        }
        if ((actions & Clipboard.ACTION_REFERENCE) != 0) {
            tms.add(TransferMode.LINK);
        }

        return tms;
    }

    @Override public Set<TransferMode> getTransferModes() {
        if (transferModesCache != null) {
            return EnumSet.copyOf(transferModesCache);
        }

        ClipboardAssistance assistant = (currentDragboard != null) ? currentDragboard : systemAssistant;
        final Set<TransferMode> tms = clipboardActionsToTransferModes(assistant.getSupportedSourceActions());

        return tms;
    }

    @Override public void setDragView(Image image) {
        dragImage = image;
    }

    @Override public void setDragViewOffsetX(double offsetX) {
        dragOffsetX = offsetX;
    }

    @Override public void setDragViewOffsetY(double offsetY) {
        dragOffsetY = offsetY;
    }

    @Override public Image getDragView() {
        return dragImage;
    }

    @Override public double getDragViewOffsetX() {
        return dragOffsetX;
    }

    @Override public double getDragViewOffsetY() {
        return dragOffsetY;
    }

    public void close() {
        systemAssistant.close();
    }

    public void flush() {
        if (isCaching) {
            putContentToPeer(dataCache.toArray(new Pair[0]));
        }

        clearCache();
        clearDragView();
        systemAssistant.flush();
    }

    @Override public Object getContent(DataFormat dataFormat) {
        if (dataCache != null) {
            for (Pair<DataFormat, Object> pair : dataCache) {
                if (pair.getKey() == dataFormat) {
                    return pair.getValue();
                }
            }
            return null;
        }

        ClipboardAssistance assistant =
                (currentDragboard != null) ? currentDragboard : systemAssistant;

        if (dataFormat == DataFormat.IMAGE) {
            return readImage();
        } else if (dataFormat == DataFormat.URL) {
            return assistant.getData(Clipboard.URI_TYPE);
        } else if (dataFormat == DataFormat.FILES) {
            Object data = assistant.getData(Clipboard.FILE_LIST_TYPE);
            if (data == null) return Collections.emptyList();
            String[] paths = (String[]) data;
            List<File> list = new ArrayList<File>(paths.length);
            for (int i=0; i<paths.length; i++) {
                list.add(new File(paths[i]));
            }
            return list;
        }

        for (String mimeType : dataFormat.getIdentifiers()) {
            Object data = assistant.getData(mimeType);
            if (data instanceof ByteBuffer) {
                try {
                    ByteBuffer bb = (ByteBuffer) data;
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            bb.array());
                    ObjectInput in = new ObjectInputStream(bis) {
                        @Override protected Class<?> resolveClass(
                                ObjectStreamClass desc)
                                throws IOException, ClassNotFoundException {
                            return Class.forName(desc.getName(), false,
                                    Thread.currentThread().getContextClassLoader());
                        }
                    };
                    data = in.readObject();
                } catch (IOException e) {
                    // ignore, just return the ByteBuffer if we cannot parse it
                } catch (ClassNotFoundException e) {
                    // ignore, just return the ByteBuffer if we cannot parse it
                }
            }
            if (data != null) return data;
        }
        return null;
    }

    private static Image convertObjectToImage(Object obj) {
        if (obj instanceof Image) {
            return (Image) obj;
        } else {
            final Pixels pixels;
            if (obj instanceof ByteBuffer) {
                ByteBuffer bb = (ByteBuffer)obj;
                try {
                    bb.rewind();
                    int width = bb.getInt();
                    int height = bb.getInt();
                    pixels = Application.GetApplication().createPixels(
                        width, height, bb.slice());
                } catch (Exception e) {
                    //ignore incorrect sized arrays
                    //not a client problem
                    return null;
                }
            } else if (obj instanceof Pixels) {
                pixels = (Pixels)obj;
            } else {
                return null;
            }
            com.sun.prism.Image platformImage = PixelUtils.pixelsToImage(
                pixels);
            ImageLoader il =  Toolkit.getToolkit().loadPlatformImage(
                platformImage);
            return Toolkit.getImageAccessor().fromPlatformImage(il);
        }
    }

    private Image readImage() {
        ClipboardAssistance assistant =
                (currentDragboard != null) ? currentDragboard : systemAssistant;

        Object rawData = assistant.getData(Clipboard.RAW_IMAGE_TYPE);
        if (rawData == null) {
            Object htmlData = assistant.getData(Clipboard.HTML_TYPE);
            if (htmlData != null) {
                String url = parseIMG(htmlData);
                if (url != null) {
                    try {
                        @SuppressWarnings("removal")
                        SecurityManager sm = System.getSecurityManager();
                        if (sm != null) {
                            @SuppressWarnings("removal")
                            AccessControlContext context = getAccessControlContext();
                            URL u = new URL(url);
                            String protocol = u.getProtocol();
                            if (protocol.equalsIgnoreCase("jar")) {
                                String file = u.getFile();
                                u = new URL(file);
                                protocol = u.getProtocol();
                            }
                            if (protocol.equalsIgnoreCase("file")) {
                                FilePermission fp = new FilePermission(u.getFile(), "read");
                                sm.checkPermission(fp, context);
                            } else if (protocol.equalsIgnoreCase("ftp") ||
                                       protocol.equalsIgnoreCase("http") ||
                                       protocol.equalsIgnoreCase("https")) {
                                int port = u.getPort();
                                String hoststr = (port == -1 ? u.getHost() : u.getHost() + ":" + port);
                                SocketPermission sp = new SocketPermission(hoststr, "connect");
                                sm.checkPermission(sp, context);
                            } else {
                                PermissionHelper.checkClipboardPermission(context);
                            }
                        }
                        return (new Image(url));
                    } catch (MalformedURLException mue) {
                        return null;
                    } catch (SecurityException se) {
                        return null;
                    }
                }
            }
            return null;
        }
        return convertObjectToImage(rawData);
    }

    private static final Pattern findTagIMG =
            Pattern.compile("IMG\\s+SRC=\\\"([^\\\"]+)\\\"",
                            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private String parseIMG(Object data) {
        if (data == null) {
            return null;
        }
        if ((data instanceof String) == false) {
            return null;
        }
        String str = (String)data;
        Matcher matcher = findTagIMG.matcher(str);
        if (matcher.find()) {
             return (matcher.group(1));
        } else {
            return null;
        }
    }

    private boolean placeImage(final Image image) {
        if (image == null) {
            return false;
        }

        String url = image.getUrl();
        if (url == null || PixelUtils.supportedFormatType(url)) {
            com.sun.prism.Image prismImage =
                    (com.sun.prism.Image) Toolkit.getImageAccessor().getPlatformImage(image);
            Pixels pixels = PixelUtils.imageToPixels(prismImage);
            if (pixels != null) {
                systemAssistant.setData(Clipboard.RAW_IMAGE_TYPE, pixels);
                return true;
            } else {
                return false;
            }
        } else {
            systemAssistant.setData(Clipboard.URI_TYPE, url);
            return true;
        }
    }

    @Override public Set<DataFormat> getContentTypes() {
        Set<DataFormat> set = new HashSet<DataFormat>();

        if (dataCache != null) {
            for (Pair<DataFormat, Object> pair : dataCache) {
                set.add(pair.getKey());
            }
            return set;
        }

        ClipboardAssistance assistant =
                (currentDragboard != null) ? currentDragboard : systemAssistant;

        String[] types = assistant.getMimeTypes();
        if (types == null) {
            return set;
        }
        for (String t: types) {
            if (t.equalsIgnoreCase(Clipboard.RAW_IMAGE_TYPE)) {
                set.add(DataFormat.IMAGE);
            } else if (t.equalsIgnoreCase(Clipboard.URI_TYPE)) {
                set.add(DataFormat.URL);
            } else if (t.equalsIgnoreCase(Clipboard.FILE_LIST_TYPE)) {
                set.add(DataFormat.FILES);
            } else if (t.equalsIgnoreCase(Clipboard.HTML_TYPE)) {
                set.add(DataFormat.HTML);
                // RT-16812 - IE puts images on the clipboard in a HTML IMG url
                try {
                    //HTML header could be improperly formatted and we can get an exception here
                    if (parseIMG(assistant.getData(Clipboard.HTML_TYPE)) != null) {
                        set.add(DataFormat.IMAGE);
                    }
                } catch (Exception ex) {
                    //do nothing - it was just an attempt
                }
            } else {
                DataFormat dataFormat = DataFormat.lookupMimeType(t);
                if (dataFormat == null) {
                    //The user is interested in any format.
                    dataFormat = new DataFormat(t);
                }
                set.add(dataFormat);
            }
        }
        return set;
    }

    @Override public boolean hasContent(DataFormat dataFormat) {
        if (dataCache != null) {
            for (Pair<DataFormat, Object> pair : dataCache) {
                if (pair.getKey() == dataFormat) {
                    return true;
                }
            }
            return false;
        }

        ClipboardAssistance assistant =
                (currentDragboard != null) ? currentDragboard : systemAssistant;

        String[] stypes = assistant.getMimeTypes();
        if (stypes == null) {
            return false;
        }
        for (String t: stypes) {
            if (dataFormat == DataFormat.IMAGE &&
                    t.equalsIgnoreCase(Clipboard.RAW_IMAGE_TYPE)) {
                return true;
            } else if (dataFormat == DataFormat.URL &&
                       t.equalsIgnoreCase(Clipboard.URI_TYPE)) {
                return true;
            } else if (dataFormat == DataFormat.IMAGE &&
                       t.equalsIgnoreCase(Clipboard.HTML_TYPE) &&
                       parseIMG(assistant.getData(Clipboard.HTML_TYPE)) != null) {
                return true;
            } else if (dataFormat == DataFormat.FILES &&
                    t.equalsIgnoreCase(Clipboard.FILE_LIST_TYPE)) {
                return true;
            }

            DataFormat found = DataFormat.lookupMimeType(t);
            if (found != null && found.equals(dataFormat)) {
                return true;
            }
        }
        return false;
    }

    private static ByteBuffer prepareImage(Image image) {
        PixelReader pr = image.getPixelReader();

        int w = (int) image.getWidth();
        int h = (int) image.getHeight();

        byte[] pixels = new byte[w * h * 4];
        pr.getPixels(0, 0, w, h, WritablePixelFormat.getByteBgraInstance(), pixels, 0, w*4);

        ByteBuffer dragImageBuffer = ByteBuffer.allocate(8 + w * h * 4);
        dragImageBuffer.putInt(w);
        dragImageBuffer.putInt(h);
        dragImageBuffer.put(pixels);

        return dragImageBuffer;
    }

    private static ByteBuffer prepareOffset(double offsetX, double offsetY) {
        ByteBuffer dragImageOffset = ByteBuffer.allocate(8);

        dragImageOffset.rewind();
        dragImageOffset.putInt((int) offsetX);
        dragImageOffset.putInt((int) offsetY);

        return dragImageOffset;
    }

    private boolean putContentToPeer(Pair<DataFormat, Object>... content) {
        systemAssistant.emptyCache();

        boolean dataSet = false;

        // For each pair, we need to extract the DataFormat and data associated with
        // that pair. We then will send the data down to Glass, having done
        // some work for well known types (such as Image and File) in order to
        // adapt from FX to Glass requirements. For everything else, we just
        // pass down the mime type and data, for each mime type supported by
        // the DataFormat type.
        for (Pair<DataFormat, Object> pair : content) {
            final DataFormat dataFormat = pair.getKey();
            Object data = pair.getValue();

            // Images are handled specially. On Windows, the image type supported
            // on the clipboard is a DIB (device independent bitmap), while on Mac
            // it is a TIFF. Other native apps expect this entry on the clipboard
            // for image copy/paste. However other Java apps or FX apps (for example)
            // might expect the JPG bits directly, rather than the DIB / TIFF bits.
            // So what we do is, any IMAGE type DataFormat that comes in will be stored
            // in DIB / TIFF, while specific bits will also be stored (in the future).
            if (dataFormat == DataFormat.IMAGE) {
                dataSet = placeImage(convertObjectToImage(data));
            } else if (dataFormat == DataFormat.URL) {
                // TODO Weird, but this is how Glass wants it...
                systemAssistant.setData(Clipboard.URI_TYPE, data);
                dataSet = true;
            } else if (dataFormat == DataFormat.RTF) {
                systemAssistant.setData(Clipboard.RTF_TYPE, data);
                dataSet = true;
            } else if (dataFormat == DataFormat.FILES) {
                // Have to convert from List<File> to String[]
                List<File> list = (List<File>)data;
                if (list.size() != 0) {
                    String[] paths = new String[list.size()];
                    int i = 0;
                    for (File f : list) {
                        paths[i++] = f.getAbsolutePath();
                    }
                    systemAssistant.setData(Clipboard.FILE_LIST_TYPE, paths);
                    dataSet = true;
                }
            } else {
                if (data instanceof Serializable) {
                    if ((dataFormat != DataFormat.PLAIN_TEXT && dataFormat != DataFormat.HTML) ||
                        !(data instanceof String))
                    {
                        try {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ObjectOutput out = new ObjectOutputStream(bos);
                            out.writeObject(data);
                            out.close();
                            data = ByteBuffer.wrap(bos.toByteArray());
                        } catch (IOException e) {
                            throw new IllegalArgumentException("Could not serialize the data", e);
                        }
                    }
                } else if (data instanceof InputStream) {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    try (InputStream is = (InputStream)data) {
                        // TODO: performance
                        int i = is.read();
                        while (i != -1) {
                            bout.write(i);
                            i = is.read();
                        }
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Could not serialize the data", e);
                    }
                    data = ByteBuffer.wrap(bout.toByteArray());
                } else if (!(data instanceof ByteBuffer)) {
                    throw new IllegalArgumentException("Only serializable "
                            + "objects or ByteBuffer can be used as data "
                            + "with data format " + dataFormat);
                }
                for (String mimeType : dataFormat.getIdentifiers()) {
                    systemAssistant.setData(mimeType, data);
                    dataSet = true;
                }
            }
        }

        // add drag image and offsets to the peer
        if (dragImage != null) {
            ByteBuffer imageBuffer = prepareImage(dragImage);
            ByteBuffer offsetBuffer = prepareOffset(dragOffsetX, dragOffsetY);
            systemAssistant.setData(Clipboard.DRAG_IMAGE, imageBuffer);
            systemAssistant.setData(Clipboard.DRAG_IMAGE_OFFSET, offsetBuffer);
        }

        return dataSet;
    }


    @Override public boolean putContent(Pair<DataFormat, Object>... content) {

        for (Pair<DataFormat, Object> pair : content) {
            final DataFormat format = pair.getKey();
            final Object     data   = pair.getValue();

            if (format == null) {
                throw new NullPointerException("Clipboard.putContent: null data format");
            }
            if (data == null) {
                throw new NullPointerException("Clipboard.putContent: null data");
            }
        }

        boolean dataSet = false;

        if (isCaching) {
            if (dataCache == null) {
                dataCache = new ArrayList<Pair<DataFormat, Object>>(content.length);
            }
            for (Pair<DataFormat, Object> pair : content) {
                dataCache.add(pair);
                dataSet = true;
            }
        } else {
            dataSet = putContentToPeer(content);
            systemAssistant.flush();
        }

        return dataSet;
    }

    private void clearCache() {
        dataCache = null;
        transferModesCache = null;
    }

    private void clearDragView() {
        dragImage = null;
        dragOffsetX = dragOffsetY = 0;
    }
}
