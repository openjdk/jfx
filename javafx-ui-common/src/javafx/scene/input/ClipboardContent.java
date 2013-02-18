/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javafx.scene.image.Image;

/**
 * Data container for {@link Clipboard} data. It can hold multiple data in 
 * several data formats.
 */
public class ClipboardContent extends HashMap<DataFormat, Object> {
    /**
     * Gets whether a plain text String (DataFormat.PLAIN_TEXT) has been registered
     * on this Clipboard.
     * @return true if <code>hasContent(DataFormat.PLAIN_TEXT)</code> returns true, false otherwise
     */
    public final boolean hasString() {
        return containsKey(DataFormat.PLAIN_TEXT);
    }

    /**
     * Puts a plain text String onto the Clipboard. This is equivalent to
     * invoking <code>setContent(DataFormat.PLAIN_TEXT, s)</code>. Setting this value
     * to null effectively clears it from the clipboard.
     * @param s The string to place. This may be null.
     * @return True if the string was successfully placed on the clipboard.
     * @throws NullPointerException if null reference is passed
     */
    public final boolean putString(String s) {
        if (s == null) {
            throw new NullPointerException("Null string put on ClipboardContent");
        }
        return put(DataFormat.PLAIN_TEXT, s) == s;
    }

    /**
     * Gets the plain text String from the clipboard which had previously
     * been registered. This is equivalent to invoking
     * <code>getContent(DataFormat.PLAIN_TEXT)</code>. If no such entry exists,
     * null is returned.
     * @return The String on the clipboard associated with DataFormat.PLAIN_TEXT,
     * or null if there is not one.
     */
    public final String getString() {
        return (String) get(DataFormat.PLAIN_TEXT);
    }

    /**
     * Gets whether a url String (DataFormat.URL) has been registered
     * on this Clipboard.
     * @return true if hasContent(DataFormat.URL) returns true, false otherwise
     */
    public final boolean hasUrl() {
        return containsKey(DataFormat.URL);
    }

    /**
     * Puts a URL String onto the Clipboard. This is equivalent to
     * invoking <code>setContent(DataFormat.URL, s)</code>. Setting this value
     * to null effectively clears it from the clipboard.
     * @param url The string to place. This may be null.
     * @return True if the string was successfully placed on the clipboard.
     * @throws NullPointerException if null reference is passed
     */
    public final boolean putUrl(String url) {
        if (url == null) {
            throw new NullPointerException("Null URL put on ClipboardContent");
        }
        return put(DataFormat.URL, url) == url;
    }

    /**
     * Gets the URL String from the clipboard which had previously
     * been registered. This is equivalent to invoking
     * <code>getContent(DataFormat.URL)</code>. If no such entry exists,
     * null is returned.
     * @return The String on the clipboard associated with DataFormat.URL,
     * or null if there is not one.
     */
    public final String getUrl() {
        return (String) get(DataFormat.URL);
    }

    /**
     * Gets whether an HTML text String (DataFormat.HTML) has been registered
     * on this Clipboard.
     * @return true if <code>hasContent(DataFormat.HTML)</code> returns true, false otherwise
     */
    public final boolean hasHtml() {
        return containsKey(DataFormat.HTML);
    }

    /**
     * Puts an HTML text String onto the Clipboard. This is equivalent to
     * invoking <code>setContent(DataFormat.HTML, s)</code>. Setting this value
     * to null effectively clears it from the clipboard.
     * @param s The string to place. This may be null.
     * @return True if the string was successfully placed on the clipboard.
     * @throws NullPointerException if null reference is passed
     */
    public final boolean putHtml(String s) {
        if (s == null) {
            throw new NullPointerException("Null HTML put on ClipboardContent");
        }
        return put(DataFormat.HTML, s) == s;
    }

    /**
     * Gets the HTML text String from the clipboard which had previously
     * been registered. This is equivalent to invoking
     * <code>getContent(DataFormat.HTML)</code>. If no such entry exists,
     * null is returned.
     * @return The String on the clipboard associated with DataFormat.HTML,
     * or null if there is not one.
     */
    public final String getHtml() {
        return (String) get(DataFormat.HTML);
    }

    /**
     * Gets whether an RTF String (DataFormat.RTF) has been registered
     * on this Clipboard.
     * @return true if hasContent(DataFormat.RTF) returns true, false otherwise
     */
    public final boolean hasRtf() {
        return containsKey(DataFormat.RTF);
    }

    /**
     * Puts an RTF text String onto the Clipboard. This is equivalent to
     * invoking <code>setContent(DataFormat.RTF, s)</code>. Setting this value
     * to null effectively clears it from the clipboard.
     * @param rtf The string to place. This may be null.
     * @return True if the string was successfully placed on the clipboard.
     * @throws NullPointerException if null reference is passed
     */
    public final boolean putRtf(String rtf) {
        if (rtf == null) {
            throw new NullPointerException("Null RTF put on ClipboardContent");
        }
        return put(DataFormat.RTF, rtf) == rtf;
    }

    /**
     * Gets the RTF text String from the clipboard which had previously
     * been registered. This is equivalent to invoking
     * <code>getContent(DataFormat.RTF)</code>. If no such entry exists,
     * null is returned.
     * @return The String on the clipboard associated with DataFormat.RTF,
     * or null if there is not one.
     */
    public final String getRtf() {
        return (String) get(DataFormat.RTF);
    }

    /**
     * Gets whether an Image (DataFormat.IMAGE) has been registered
     * on this Clipboard.
     * @return true if hasContent(DataFormat.IMAGE) returns true, false otherwise
     */
    public final boolean hasImage() {
        return containsKey(DataFormat.IMAGE);
    };

    /**
     * Puts an Image onto the Clipboard. This is equivalent to
     * invoking <code>setContent(DataFormat.IMAGE, image)</code>. Setting this value
     * to null effectively clears it from the clipboard. When an image is placed
     * on the clipboard in this manner, an operating system dependent image
     * is loaded onto the clipboard (such as TIFF on mac or DIB on Windows).
     *
     * @param i The image to place. This may be null.
     * @return True if the image was successfully placed on the clipboard.
     * @throws NullPointerException if null reference is passed
     */
    public final boolean putImage(Image i) {
        if (i == null) {
            throw new NullPointerException("Null image put on ClipboardContent");
        }
        return put(DataFormat.IMAGE, i) == i;
    }

    /**
     * Gets the Image from the clipboard which had previously
     * been registered. This is equivalent to invoking
     * <code>getContent(DataFormat.IMAGE)</code>. If no such entry exists,
     * null is returned.
     * @return The Image on the clipboard associated with DataFormat.IMAGE,
     * or null if there is not one.
     */
    public final Image getImage() {
        return (Image) get(DataFormat.IMAGE);
    }

    /**
     * Gets whether an List of Files (DataFormat.FILES) has been registered
     * on this Clipboard.
     * @return true if hasContent(DataFormat.FILES) returns true, false otherwise
     */
    public final boolean hasFiles() {
        return containsKey(DataFormat.FILES);
    }

    /**
     * Puts an List of Files onto the Clipboard. This is equivalent to
     * invoking <code>setContent(DataFormat.FILES, files)</code>. Setting this value
     * to null effectively clears it from the clipboard.
     * @param files The files to place. This may be null.
     * @return True if the files were successfully placed on the clipboard.
     * @throws NullPointerException if null reference is passed
     */
    public final boolean putFiles(List<File> files) {
        if (files == null) {
            throw new NullPointerException("Null reference to files put "
                    + "on ClipboardContent");
        }
        return put(DataFormat.FILES, files) == files;
    }

    /**
     * Puts an List of Files onto the Clipboard, based on the file path. This is
     * simply a convenience method which constructs a List of Files and invokes
     * the {@link #putFiles} method.
     * @param filePaths The files to place. This may be null.
     * @return True if the files were successfully placed on the clipboard.
     * @throws NullPointerException if null reference is passed
     */
    public final boolean putFilesByPath(List<String> filePaths) {
        /* No need to throw NPE manually here, the code throws it anyway */
        final List<File> files = new ArrayList<File>(filePaths.size());
        for (String path : filePaths) {
            files.add(new File(path));
        }
        return putFiles(files);
    }

    /**
     * Gets the List of Files from the clipboard which had previously
     * been registered. This is equivalent to invoking
     * <code>getContent(DataFormat.FILES)</code>. If no such entry exists,
     * null is returned.
     * @return The List of Files on the clipboard associated with DataFormat.FILES,
     * or null if there is not one.
     */
    public final List<File> getFiles() {
        return (List<File>) get(DataFormat.FILES);
    }
}
