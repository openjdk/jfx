/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.webkit.UIClient;
import com.sun.webkit.WebPage;
import com.sun.webkit.graphics.WCImage;
import com.sun.webkit.graphics.WCRectangle;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
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
    // for testing purposes only
    private static String[] chooseFiles = null;

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

    @SuppressWarnings("removal")
    private AccessControlContext getAccessContext() {
        return accessor.getPage().getAccessControlContext();
    }

    @Override public WebPage createPage(
            boolean menu, boolean status, boolean toolbar, boolean resizable) {
        final WebEngine w = getWebEngine();
        if (w != null && w.getCreatePopupHandler() != null) {
            final PopupFeatures pf =
                    new PopupFeatures(menu, status, toolbar, resizable);
            @SuppressWarnings("removal")
            WebEngine popup = AccessController.doPrivileged(
                    (PrivilegedAction<WebEngine>) () -> w.getCreatePopupHandler().call(pf), getAccessContext());
            return Accessor.getPageFor(popup);
        }
        return null;
    }

    @SuppressWarnings("removal")
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

    @SuppressWarnings("removal")
    @Override public boolean confirm(final String text) {
        final WebEngine w = getWebEngine();
        if (w != null && w.getConfirmHandler() != null) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<Boolean>) () -> w.getConfirmHandler().call(text), getAccessContext());
        }
        return false;
    }

    @SuppressWarnings("removal")
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

    // for testing purposes only
    static void test_setChooseFiles(String[] files) {
        chooseFiles = files;
    }

    @Override public String[] chooseFile(String initialFileName, boolean multiple, String mimeFilters) {
        if (chooseFiles != null) {
            return chooseFiles;
        }
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
        if (image != null && !image.isNull()) {
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
                String fileExtension = image.getFileExtension();
                try {
                    File temp = File.createTempFile("jfx", "." + fileExtension);
                    temp.deleteOnExit();
                    ImageIO.write(
                        image.toBufferedImage(),
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

}
