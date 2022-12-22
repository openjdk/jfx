/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.javafx.util.WeakReferenceQueue;
import javafx.beans.NamedArg;

/**
 * Data format identifier used as means
 * of identifying the data stored on a clipboard/dragboard.
 * @since JavaFX 2.0
 */
public class DataFormat {

    /**
     * A static cache of all DataFormats created and currently in use. This is needed
     * by the underlying implementation, such that, given a mime type, we can determine
     * the associated DataFormat. The OS level is going to supply us with a mime type
     * (or other string based key), and we need to be able to map this back to the FX
     * DataFormat.
     */
    private static final WeakReferenceQueue<DataFormat> DATA_FORMAT_LIST = new WeakReferenceQueue<>();

    /**
     * Represents a plain text string.
     */
    public static final DataFormat PLAIN_TEXT = new DataFormat("text/plain");

    /**
     * Represents an HTML formatted string.
     */
    public static final DataFormat HTML = new DataFormat("text/html");

    /**
     * Represents an RTF formatted string
     */
    public static final DataFormat RTF = new DataFormat("text/rtf");

    /**
     * Represents a URL, encoded as a String
     */
    public static final DataFormat URL = new DataFormat("text/uri-list");

    /**
     * A special platform specific image type, such as is commonly used
     * on the clipboard and interoperates widely with other applications.
     * For example, on Mac this might ultimately be a TIFF while on
     * Windows it might be a DIB (device independent bitmap).
     */
    public static final DataFormat IMAGE = new DataFormat("application/x-java-rawimage");

    /**
     * Represents a List of Files.
     */
    public static final DataFormat FILES = new DataFormat("application/x-java-file-list", "java.file-list");

    /**
     * Data format used internally, represents dragged image. Making this
     * a private field prevents user from creating this DataFormat and breaking
     * our drag view implementation.
     */
    private static final DataFormat DRAG_IMAGE = new DataFormat("application/x-java-drag-image");

    /**
     * Data format used internally, represents offset in the dragged image.
     * Making this a private field prevents user from creating this DataFormat
     * and breaking our drag view implementation.
     */
    private static final DataFormat DRAG_IMAGE_OFFSET = new DataFormat("application/x-java-drag-image-offset");

    /**
     * A set of identifiers, typically mime types, for this DataFormat.
     * In most cases this will be a single String.
     */
    private final Set<String> identifier;

    /**
     * Create a new DataFormat, specifying the set of ids that are associated with
     * this data format. Typically the ids are one or more mime types. For each
     * id, any data associated with this DataFormat will be registered on the
     * clipboard. For example, suppose I had the following:
     * <pre>{@code
     *     DataFormat fmt = new DataFormat("text/foo", "text/bar");
     *     Clipboard clipboard = Clipboard.getSystemClipboard();
     *     ClipboardContent content = new ClipboardContent();
     *     content.put(fmt, "Hello");
     *     clipboard.setContent(content);
     * }</pre>
     *
     * With the above code, if I were to look on the clipboard, I'd find the String "Hello"
     * listed both for "text/foo" and "text/bar" on the clipboard.
     *
     * <p>
     * Note that the ids may be subject to platform restrictions in some cases.
     * For instance, Swing requires a mime type so if an {@code id} is not
     * of the "type/subtype" format it won't be possible
     * to drag data of this type from/to {@link javafx.embed.swing.JFXPanel}.
     * </p>
     * @param ids The set of ids used to represent this DataFormat on the clipboard.
     * @throws IllegalArgumentException if one of the given mime types is already
     *         assigned to another DataFormat.
     */
    public DataFormat(@NamedArg("ids") String... ids) {
        DATA_FORMAT_LIST.cleanup();
        if (ids != null) {
            for (String id : ids) {
                if (lookupMimeType(id) != null) {
                    throw new IllegalArgumentException("DataFormat '" + id +
                            "' already exists.");
                }
            }
            this.identifier = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ids)));
        } else {
            this.identifier = Collections.<String>emptySet();
        }

        // Add to the statis data format list.
        DATA_FORMAT_LIST.add(this);
    }

    /**
     * Gets the unmodifiable set of identifiers for this DataFormat.
     * @return an unmodifiable set that is never null.
     */
    public final Set<String> getIdentifiers() {
        return identifier;
    }

    /**
     * Returns a string representation of this {@code DataFormat} object.
     * @return a string representation of this {@code DataFormat} object.
     */
    @Override public String toString() {
        if (identifier.isEmpty()) {
            return "[]";
        } else if (identifier.size() == 1) {
            StringBuilder sb = new StringBuilder("[");
            sb.append(identifier.iterator().next());
            return (sb.append("]").toString());
        } else {
            StringBuilder b = new StringBuilder("[");
            Iterator<String> itr = identifier.iterator();
            while (itr.hasNext()) {
                b = b.append(itr.next());
                if (itr.hasNext()) {
                    b = b.append(", ");
                }
            }
            b = b.append("]");
            return b.toString();
        }
    }

    /**
     * Returns a hash code for this {@code DataFormat} object.
     * @return a hash code for this {@code DataFormat} object.
     */
    @Override public int hashCode() {
        int hash = 7;

        for (String id : identifier) {
            hash = 31 * hash + id.hashCode();
        }

        return hash;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is equal to the {@code obj} argument; {@code false} otherwise.
     */
    @Override public boolean equals(Object obj) {
        if (obj == null || ! (obj instanceof DataFormat)) {
            return false;
        }

        DataFormat otherDataFormat = (DataFormat) obj;

        if (identifier.equals(otherDataFormat.identifier)) {
            return true;
        }

        return false;
    }

    /**
     * Looks for the DataFormat which has been previously created with
     * the given mime type as one of its ids.
     * @param mimeType If null or the empty string, then null is returned.
     * @return The matching DataFormat
     */
    public static DataFormat lookupMimeType(String mimeType) {
        if (mimeType == null || mimeType.length() == 0) {
            return null;
        }

        Iterator itr = DATA_FORMAT_LIST.iterator();
        while (itr.hasNext()) {
            DataFormat dataFormat = (DataFormat) itr.next();
            if (dataFormat.getIdentifiers().contains(mimeType)) {
                return dataFormat;
            }
        }
        return null;
    }
}
