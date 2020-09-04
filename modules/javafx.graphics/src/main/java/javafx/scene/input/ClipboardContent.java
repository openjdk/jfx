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
 * @since JavaFX 2.0
 */
public class ClipboardContent extends HashMap<DataFormat, Object> {

    /**
     * Creates a {@code ClipboardContent}.
     */
    public ClipboardContent() {
    }

    /**
     * Gets whether a plain text String ({@code DataFormat.PLAIN_TEXT})
     * has been put to this {@code ClipboardContent}.
     * @return true if {@code containsKey(DataFormat.PLAIN_TEXT)}
     * returns true, false otherwise
     */
    public final boolean hasString() {
        return containsKey(DataFormat.PLAIN_TEXT);
    }

    /**
     * Puts a plain text String into the {@code ClipboardContent}. This is
     * equivalent to invoking {@code put(DataFormat.PLAIN_TEXT, s)}.
     * Setting this value to null effectively clears it
     * from the {@code ClipboardContent}.
     * @param s The string to place. This may be null.
     * @return always true (the string is always successfully put)
     */
    public final boolean putString(String s) {
        if (s == null) {
            remove(DataFormat.PLAIN_TEXT);
        } else {
            put(DataFormat.PLAIN_TEXT, s);
        }
        return true;
    }

    /**
     * Gets the plain text String from the {@code ClipboardContent}
     * which had previously been put. This is equivalent to invoking
     * {@code get(DataFormat.PLAIN_TEXT)}. If no such entry exists,
     * null is returned.
     * @return The String in the {@code ClipboardContent} associated
     * with {@code DataFormat.PLAIN_TEXT}, or null if there is not one.
     */
    public final String getString() {
        return (String) get(DataFormat.PLAIN_TEXT);
    }

    /**
     * Gets whether a URL String ({@code DataFormat.URL})
     * has been put to this {@code ClipboardContent}.
     * @return true if {@code containsKey(DataFormat.URL)}
     * returns true, false otherwise
     */
    public final boolean hasUrl() {
        return containsKey(DataFormat.URL);
    }

    /**
     * Puts a URL String into the {@code ClipboardContent}. This is
     * equivalent to invoking {@code put(DataFormat.URL, url)}.
     * Setting this value to null effectively clears it
     * from the {@code ClipboardContent}.
     * @param url The string to place. This may be null.
     * @return always true (the URL is always successfully put)
     */
    public final boolean putUrl(String url) {
        if (url == null) {
            remove(DataFormat.URL);
        } else {
            put(DataFormat.URL, url);
        }
        return true;
    }

    /**
     * Gets the URL String from the {@code ClipboardContent}
     * which had previously been put. This is equivalent to invoking
     * {@code get(DataFormat.URL)}. If no such entry exists,
     * null is returned.
     * @return The String in the {@code ClipboardContent} associated
     * with {@code DataFormat.URL}, or null if there is not one.
     */
    public final String getUrl() {
        return (String) get(DataFormat.URL);
    }

    /**
     * Gets whether an HTML String ({@code DataFormat.HTML})
     * has been put to this {@code ClipboardContent}.
     * @return true if {@code containsKey(DataFormat.HTML)}
     * returns true, false otherwise
     */
    public final boolean hasHtml() {
        return containsKey(DataFormat.HTML);
    }

    /**
     * Puts an HTML String into the {@code ClipboardContent}. This is
     * equivalent to invoking {@code put(DataFormat.HTML, html)}.
     * Setting this value to null effectively clears it
     * from the {@code ClipboardContent}.
     * @param html The string to place. This may be null.
     * @return always true (the HTML is always successfully put)
     */
    public final boolean putHtml(String html) {
        if (html == null) {
            remove(DataFormat.HTML);
        } else {
            put(DataFormat.HTML, html);
        }
        return true;
    }

    /**
     * Gets the HTML String from the {@code ClipboardContent}
     * which had previously been put. This is equivalent to invoking
     * {@code get(DataFormat.HTML)}. If no such entry exists,
     * null is returned.
     * @return The String in the {@code ClipboardContent} associated
     * with {@code DataFormat.HTML}, or null if there is not one.
     */
    public final String getHtml() {
        return (String) get(DataFormat.HTML);
    }

    /**
     * Gets whether a RTF String ({@code DataFormat.RTF})
     * has been put to this {@code ClipboardContent}.
     * @return true if {@code containsKey(DataFormat.RTF)}
     * returns true, false otherwise
     */
    public final boolean hasRtf() {
        return containsKey(DataFormat.RTF);
    }

    /**
     * Puts a RTF String into the {@code ClipboardContent}. This is
     * equivalent to invoking {@code put(DataFormat.RTF, rtf)}.
     * Setting this value to null effectively clears it
     * from the {@code ClipboardContent}.
     * @param rtf The string to place. This may be null.
     * @return always true (the RTF is always successfully put)
     */
    public final boolean putRtf(String rtf) {
        if (rtf == null) {
            remove(DataFormat.RTF);
        } else {
            put(DataFormat.RTF, rtf);
        }
        return true;
    }

    /**
     * Gets the RTF String from the {@code ClipboardContent}
     * which had previously been put. This is equivalent to invoking
     * {@code get(DataFormat.RTF)}. If no such entry exists,
     * null is returned.
     * @return The String in the {@code ClipboardContent} associated
     * with {@code DataFormat.RTF}, or null if there is not one.
     */
    public final String getRtf() {
        return (String) get(DataFormat.RTF);
    }

    /**
     * Gets whether an Image ({@code DataFormat.IMAGE})
     * has been put to this {@code ClipboardContent}.
     * @return true if {@code containsKey(DataFormat.IMAGE)}
     * returns true, false otherwise
     */
    public final boolean hasImage() {
        return containsKey(DataFormat.IMAGE);
    };

    /**
     * Puts an Image into the {@code ClipboardContent}. This is
     * equivalent to invoking {@code put(DataFormat.IMAGE, i)}.
     * Setting this value to null effectively clears it
     * from the {@code ClipboardContent}. When an image is placed
     * on the clipboard in this manner, an operating system dependent image
     * is loaded onto the clipboard (such as TIFF on Mac or DIB on Windows).
     *
     * @param i The image to place. This may be null.
     * @return always true (the image is always successfully put)
     */
    public final boolean putImage(Image i) {
        if (i == null) {
            remove(DataFormat.IMAGE);
        } else {
            put(DataFormat.IMAGE, i);
        }
        return true;
    }

    /**
     * Gets the Image from the {@code ClipboardContent}
     * which had previously been put. This is equivalent to invoking
     * {@code get(DataFormat.IMAGE)}. If no such entry exists,
     * null is returned.
     * @return The Image in the {@code ClipboardContent} associated
     * with {@code DataFormat.IMAGE}, or null if there is not one.
     */
    public final Image getImage() {
        return (Image) get(DataFormat.IMAGE);
    }

    /**
     * Gets whether a List of Files ({@code DataFormat.FILES})
     * has been put to this {@code ClipboardContent}.
     * @return true if {@code containsKey(DataFormat.FILES)}
     * returns true, false otherwise
     */
    public final boolean hasFiles() {
        return containsKey(DataFormat.FILES);
    }

    /**
     * Puts a List of Files into the {@code ClipboardContent}. This is
     * equivalent to invoking {@code put(DataFormat.FILES, files)}.
     * Setting this value to null effectively clears it
     * from the {@code ClipboardContent}.
     *
     * @param files The files to place. This may be null.
     * @return always true (the files are always successfully put)
     */
    public final boolean putFiles(List<File> files) {
        if (files == null) {
            remove(DataFormat.FILES);
        } else {
            put(DataFormat.FILES, files);
        }
        return true;
    }

    /**
     * Puts a List of Files into the {@code ClipboardContent}, based
     * on the file path. This is simply a convenience method which constructs
     * a List of Files and invokes the {@link #putFiles} method.
     *
     * @param filePaths The files to place. This may be null.
     * @return always true (the files are always successfully put)
     */
    public final boolean putFilesByPath(List<String> filePaths) {
        final List<File> files = new ArrayList<File>(filePaths.size());
        for (String path : filePaths) {
            files.add(new File(path));
        }
        return putFiles(files);
    }

    /**
     * Gets the List of Files from the {@code ClipboardContent}
     * which had previously been put. This is equivalent to invoking
     * {@code get(DataFormat.FILES)}. If no such entry exists,
     * null is returned.
     * @return The List of Files in the {@code ClipboardContent} associated
     * with {@code DataFormat.FILES}, or null if there is not one.
     */
    public final List<File> getFiles() {
        return (List<File>) get(DataFormat.FILES);
    }
}
