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

package com.sun.javafx.webkit;

import static com.sun.glass.ui.Clipboard.DRAG_IMAGE;
import static com.sun.glass.ui.Clipboard.DRAG_IMAGE_OFFSET;
import static com.sun.glass.ui.Clipboard.IE_URL_SHORTCUT_FILENAME;
import static javafx.scene.web.WebEvent.ALERT;
import static javafx.scene.web.WebEvent.RESIZED;
import static javafx.scene.web.WebEvent.STATUS_CHANGED;
import static javafx.scene.web.WebEvent.VISIBILITY_CHANGED;

import com.sun.javafx.tk.Toolkit;
import com.sun.webkit.UIClient;
import com.sun.webkit.WebPage;
import com.sun.webkit.graphics.WCImage;
import com.sun.webkit.graphics.WCRectangle;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javax.imageio.ImageIO;

public final class UIClientImpl implements UIClient {
    private final Accessor accessor;
    private FileChooser chooser;
    private static final Map<String, FileExtensionInfo> fileExtensionMap = new HashMap<>();

    private static class FileExtensionInfo {
        private String description;
        private List<String> extensions;
        static void add(String type, String description, String... extensions) {
            FileExtensionInfo info = new FileExtensionInfo();
            info.description = description;
            info.extensions = Arrays.asList(extensions);
            fileExtensionMap.put(type, info);
        }

        private ExtensionFilter getExtensionFilter(String type) {
            final String extensionType = "*." + type;
            String desc = this.description + " ";

            if (type.equals("*")) {
                desc += extensions.stream().collect(java.util.stream.Collectors.joining(", ", "(", ")"));
                return new ExtensionFilter(desc, this.extensions);
            } else if (extensions.contains(extensionType)) {
                desc += "(" + extensionType + ")";
                return new ExtensionFilter(desc, extensionType);
            }
            return null;
        }
    }

    static {
        FileExtensionInfo.add("video", "Video Files", "*.webm", "*.mp4", "*.ogg");
        FileExtensionInfo.add("audio", "Audio Files", "*.mp3", "*.aac", "*.wav");
        FileExtensionInfo.add("text", "Text Files", "*.txt", "*.csv", "*.text", "*.ttf", "*.sdf", "*.srt", "*.htm", "*.html");
        FileExtensionInfo.add("image", "Image Files", "*.png", "*.jpg", "*.gif", "*.bmp", "*.jpeg");
    }

    public UIClientImpl(Accessor accessor) {
        this.accessor = accessor;
    }

    private WebEngine getWebEngine() {
        return accessor.getEngine();
    }

    private AccessControlContext getAccessContext() {
        return accessor.getPage().getAccessControlContext();
    }

    @Override public WebPage createPage(
            boolean menu, boolean status, boolean toolbar, boolean resizable) {
        final WebEngine w = getWebEngine();
        if (w != null && w.getCreatePopupHandler() != null) {
            final PopupFeatures pf =
                    new PopupFeatures(menu, status, toolbar, resizable);
            WebEngine popup = AccessController.doPrivileged(
                    (PrivilegedAction<WebEngine>) () -> w.getCreatePopupHandler().call(pf), getAccessContext());
            return Accessor.getPageFor(popup);
        }
        return null;
    }

    private void dispatchWebEvent(final EventHandler handler, final WebEvent ev) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            handler.handle(ev);
            return null;
        }, getAccessContext());
    }

    private void notifyVisibilityChanged(boolean visible) {
        WebEngine w = getWebEngine();
        if (w != null && w.getOnVisibilityChanged() != null) {
            dispatchWebEvent(
                    w.getOnVisibilityChanged(),
                    new WebEvent<Boolean>(w, VISIBILITY_CHANGED, visible));
        }
    }

    @Override public void closePage() {
        notifyVisibilityChanged(false);
    }

    @Override public void showView() {
        notifyVisibilityChanged(true);
    }

    @Override public WCRectangle getViewBounds() {
        WebView view = accessor.getView();
        Window win = null;
        if (view != null &&
            view.getScene() != null &&
            (win = view.getScene().getWindow()) != null)
        {
            return new WCRectangle(
                    (float) win.getX(), (float) win.getY(),
                    (float) win.getWidth(), (float) win.getHeight());
        }
        return null;
    }

    @Override public void setViewBounds(WCRectangle r) {
        WebEngine w = getWebEngine();
        if (w != null && w.getOnResized() != null) {
            dispatchWebEvent(
                    w.getOnResized(),
                    new WebEvent<Rectangle2D>(w, RESIZED,
                        new Rectangle2D(r.getX(), r.getY(), r.getWidth(), r.getHeight())));
        }
    }

    @Override public void setStatusbarText(String text) {
        WebEngine w = getWebEngine();
        if (w != null && w.getOnStatusChanged() != null) {
            dispatchWebEvent(
                    w.getOnStatusChanged(),
                    new WebEvent<String>(w, STATUS_CHANGED, text));
        }
    }

    @Override public void alert(String text) {
        WebEngine w = getWebEngine();
        if (w != null && w.getOnAlert() != null) {
            dispatchWebEvent(
                    w.getOnAlert(),
                    new WebEvent<String>(w, ALERT, text));
        }
    }

    @Override public boolean confirm(final String text) {
        final WebEngine w = getWebEngine();
        if (w != null && w.getConfirmHandler() != null) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<Boolean>) () -> w.getConfirmHandler().call(text), getAccessContext());
        }
        return false;
    }

    @Override public String prompt(String text, String defaultValue) {
        final WebEngine w = getWebEngine();
        if (w != null && w.getPromptHandler() != null) {
            final PromptData data = new PromptData(text, defaultValue);
            return AccessController.doPrivileged(
                    (PrivilegedAction<String>) () -> w.getPromptHandler().call(data), getAccessContext());
        }
        return "";
    }

    @Override public boolean canRunBeforeUnloadConfirmPanel() {
        return false;
    }

    @Override public boolean runBeforeUnloadConfirmPanel(String message) {
        return false;
    }

    @Override public String[] chooseFile(String initialFileName, boolean multiple, String mimeFilters) {
        // get the toplevel window
        Window win = null;
        WebView view = accessor.getView();
        if (view != null && view.getScene() != null) {
            win = view.getScene().getWindow();
        }

        if (chooser == null) {
            chooser = new FileChooser();
        }

        // Remove old filters, add specific filters and finally add generic filter
        chooser.getExtensionFilters().clear();
        if (mimeFilters != null && !mimeFilters.isEmpty()) {
            addMimeFilters(chooser, mimeFilters);
        }
        chooser.getExtensionFilters().addAll(new ExtensionFilter("All Files", "*.*"));

        // set initial directory
        if (initialFileName != null) {
            File dir = new File(initialFileName);
            while (dir != null && !dir.isDirectory()) {
                dir = dir.getParentFile();
            }
            chooser.setInitialDirectory(dir);
        }

        if (multiple) {
            List<File> files = chooser.showOpenMultipleDialog(win);
            if (files != null) {
                int n = files.size();
                String[] result = new String[n];
                for (int i = 0; i < n; i++) {
                    result[i] = files.get(i).getAbsolutePath();
                }
                return result;
            }
            return null;
        } else {
            File f = chooser.showOpenDialog(win);
            return f != null
                    ? new String[] { f.getAbsolutePath() }
                    : null;
        }
    }

    private void addSpecificFilters(FileChooser chooser, String mimeString) {
        if (mimeString.contains("/")) {
            final String splittedMime[] = mimeString.split("/");
            final String mainType = splittedMime[0];
            final String subType = splittedMime[1];
            final FileExtensionInfo extensionValue = fileExtensionMap.get(mainType);

            if (extensionValue != null) {
                ExtensionFilter extFilter = extensionValue.getExtensionFilter(subType);
                if(extFilter != null) {
                    chooser.getExtensionFilters().addAll(extFilter);
                }
            }
        }
    }

    private void addMimeFilters(FileChooser chooser, String mimeFilters) {
        if (mimeFilters.contains(",")) {
            // Filter consists of multiple MIME types
            String types[] = mimeFilters.split(",");
            for (String mimeType : types) {
                addSpecificFilters(chooser, mimeType);
            }
        } else {
            // Filter consists of single MIME type
            addSpecificFilters(chooser, mimeFilters);
        }
    }

    @Override public void print() {
    }

    private ClipboardContent content;
    private static DataFormat getDataFormat(String mimeType) {
        synchronized (DataFormat.class) {
            DataFormat ret = DataFormat.lookupMimeType(mimeType);
            if (ret == null) {
                ret = new DataFormat(mimeType);
            }
            return ret;
        }
    }

    //copy from com.sun.glass.ui.Clipboard
    private final static DataFormat DF_DRAG_IMAGE = getDataFormat(DRAG_IMAGE);
    private final static DataFormat DF_DRAG_IMAGE_OFFSET = getDataFormat(DRAG_IMAGE_OFFSET);

    @Override public void startDrag(WCImage image,
        int imageOffsetX, int imageOffsetY,
        int eventPosX, int eventPosY,
        String[] mimeTypes, Object[] values, boolean isImageSource
    ){
        content = new ClipboardContent();
        for (int i = 0; i < mimeTypes.length; ++i) if (values[i] != null) {
            try {
                content.put(getDataFormat(mimeTypes[i]),
                    IE_URL_SHORTCUT_FILENAME.equals(mimeTypes[i])
                        ? (Object)ByteBuffer.wrap(((String)values[i]).getBytes("UTF-16LE"))
                        : (Object)values[i]);
            } catch (UnsupportedEncodingException ex) {
                //never happens
            }
        }
        if (image != null) {
            ByteBuffer dragImageOffset = ByteBuffer.allocate(8);
            dragImageOffset.rewind();
            dragImageOffset.putInt(imageOffsetX);
            dragImageOffset.putInt(imageOffsetY);
            content.put(DF_DRAG_IMAGE_OFFSET, dragImageOffset);

            int w = image.getWidth();
            int h = image.getHeight();
            ByteBuffer pixels = image.getPixelBuffer();

            ByteBuffer dragImage = ByteBuffer.allocate(8 + w*h*4);
            dragImage.putInt(w);
            dragImage.putInt(h);
            dragImage.put(pixels);
            content.put(DF_DRAG_IMAGE, dragImage);

            //The image is prepared synchronously, that is sad.
            //Image need to be created by target request only.
            //QuantumClipboard.putContent have to be rewritten in Glass manner
            //with postponed data requests (DelayedCallback data object).
            if (isImageSource) {
                Object platformImage = image.getWidth() > 0 && image.getHeight() > 0 ?
                        image.getPlatformImage() : null;
                String fileExtension = image.getFileExtension();
                if (platformImage != null) {
                    try {
                        File temp = File.createTempFile("jfx", "." + fileExtension);
                        temp.deleteOnExit();
                        ImageIO.write(
                            toBufferedImage(Toolkit.getImageAccessor().fromPlatformImage(
                                Toolkit.getToolkit().loadPlatformImage(
                                    platformImage
                                )
                            )),
                            fileExtension,
                            temp);
                        content.put(DataFormat.FILES, Arrays.asList(temp));
                    } catch (IOException | SecurityException e) {
                        //That is ok. It was just an attempt.
                        //e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override public void confirmStartDrag() {
        WebView view = accessor.getView();
        if (view != null && content != null) {
            //TODO: implement native support for Drag Source actions.
            Dragboard db = view.startDragAndDrop(TransferMode.ANY);
            db.setContent(content);
        }
        content = null;
    }

    @Override public boolean isDragConfirmed() {
        return accessor.getView() != null && content != null;
    }

    private static int
            getBestBufferedImageType(PixelFormat<?> fxFormat, BufferedImage bimg,
                                     boolean isOpaque)
    {
        if (bimg != null) {
            int bimgType = bimg.getType();
            if (bimgType == BufferedImage.TYPE_INT_ARGB ||
                bimgType == BufferedImage.TYPE_INT_ARGB_PRE ||
                (isOpaque &&
                     (bimgType == BufferedImage.TYPE_INT_BGR ||
                      bimgType == BufferedImage.TYPE_INT_RGB)))
            {
                // We will allow the caller to give us a BufferedImage
                // that has an alpha channel, but we might not otherwise
                // construct one ourselves.
                // We will also allow them to choose their own premultiply
                // type which may not match the image.
                // If left to our own devices we might choose a more specific
                // format as indicated by the choices below.
                return bimgType;
            }
        }
        switch (fxFormat.getType()) {
            default:
            case BYTE_BGRA_PRE:
            case INT_ARGB_PRE:
                return BufferedImage.TYPE_INT_ARGB_PRE;
            case BYTE_BGRA:
            case INT_ARGB:
                return BufferedImage.TYPE_INT_ARGB;
            case BYTE_RGB:
                return BufferedImage.TYPE_INT_RGB;
            case BYTE_INDEXED:
                return (fxFormat.isPremultiplied()
                        ? BufferedImage.TYPE_INT_ARGB_PRE
                        : BufferedImage.TYPE_INT_ARGB);
        }
    }

    private static WritablePixelFormat<IntBuffer>
        getAssociatedPixelFormat(BufferedImage bimg)
    {
        switch (bimg.getType()) {
            // We lie here for xRGB, but we vetted that the src data was opaque
            // so we can ignore the alpha.  We use ArgbPre instead of Argb
            // just to get a loop that does not have divides in it if the
            // PixelReader happens to not know the data is opaque.
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return PixelFormat.getIntArgbPreInstance();
            case BufferedImage.TYPE_INT_ARGB:
                return PixelFormat.getIntArgbInstance();
            default:
                // Should not happen...
                throw new InternalError("Failed to validate BufferedImage type");
        }
    }

    private static boolean checkFXImageOpaque(PixelReader pr, int iw, int ih) {
        for (int x = 0; x < iw; x++) {
            for (int y = 0; y < ih; y++) {
                Color color = pr.getColor(x,y);
                if (color.getOpacity() != 1.0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BufferedImage fromFXImage(Image img, BufferedImage bimg) {
        PixelReader pr = img.getPixelReader();
        if (pr == null) {
            return null;
        }
        int iw = (int) img.getWidth();
        int ih = (int) img.getHeight();
        PixelFormat<?> fxFormat = pr.getPixelFormat();
        boolean srcPixelsAreOpaque = false;
        switch (fxFormat.getType()) {
            case INT_ARGB_PRE:
            case INT_ARGB:
            case BYTE_BGRA_PRE:
            case BYTE_BGRA:
                // Check fx image opacity only if
                // supplied BufferedImage is without alpha channel
                if (bimg != null &&
                        (bimg.getType() == BufferedImage.TYPE_INT_BGR ||
                         bimg.getType() == BufferedImage.TYPE_INT_RGB)) {
                    srcPixelsAreOpaque = checkFXImageOpaque(pr, iw, ih);
                }
                break;
            case BYTE_RGB:
                srcPixelsAreOpaque = true;
                break;
        }
        int prefBimgType = getBestBufferedImageType(pr.getPixelFormat(), bimg, srcPixelsAreOpaque);
        if (bimg != null) {
            int bw = bimg.getWidth();
            int bh = bimg.getHeight();
            if (bw < iw || bh < ih || bimg.getType() != prefBimgType) {
                bimg = null;
            } else if (iw < bw || ih < bh) {
                Graphics2D g2d = bimg.createGraphics();
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, bw, bh);
                g2d.dispose();
            }
        }
        if (bimg == null) {
            bimg = new BufferedImage(iw, ih, prefBimgType);
        }
        DataBufferInt db = (DataBufferInt)bimg.getRaster().getDataBuffer();
        int data[] = db.getData();
        int offset = bimg.getRaster().getDataBuffer().getOffset();
        int scan =  0;
        SampleModel sm = bimg.getRaster().getSampleModel();
        if (sm instanceof SinglePixelPackedSampleModel) {
            scan = ((SinglePixelPackedSampleModel)sm).getScanlineStride();
        }

        WritablePixelFormat<IntBuffer> pf = getAssociatedPixelFormat(bimg);
        pr.getPixels(0, 0, iw, ih, pf, data, offset, scan);
        return bimg;
    }

    // Method to implement the following via reflection:
    //     SwingFXUtils.fromFXImage(img, null)
    public static BufferedImage toBufferedImage(Image img) {
        try {
            return fromFXImage(img, null);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }

        // return null upon any exception
        return null;
    }

}
