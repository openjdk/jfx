/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.input.ClipboardHelper;
import java.io.File;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.scene.image.Image;
import javafx.util.Pair;

import com.sun.javafx.tk.PermissionHelper;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.Toolkit;

/**
 * Represents an operating system clipboard, on which data may be placed during, for
 * example, cut, copy, and paste operations.
 * <p>
 * To access the general system clipboard, use the following code:
 * <pre>{@code
 *     Clipboard clipboard = Clipboard.getSystemClipboard();
 * }</pre>
 * <p>
 * There is only ever one instance of the system clipboard in the application, so it is
 * perfectly acceptable to stash a reference to it somewhere handy if you so choose.
 * <p>
 * The Clipboard operates on the concept of having a single conceptual item on the
 * clipboard at any one time -- though it may be placed on the clipboard in different
 * formats. For example, the user might select text in an HTML editor and press the
 * ctrl+c or cmd+c to copy it. In this case, the same text might be available on the
 * clipboard both as HTML and as plain text. There are two copies of the data on the
 * clipboard, but they both represent the same data.
 * <p>
 * Content is specified on the Clipboard by using the {@link #setContent}
 * method. First, construct a ClipboardContent object, then invoke setContent. Every time
 * setContent is called, any previous data on the clipboard is cleared and replaced with
 * this new content.
 * <pre><code>
 *     final Clipboard clipboard = Clipboard.getSystemClipboard();
 *     final ClipboardContent content = new ClipboardContent();
 *     content.putString("Some text");
 *     {@literal content.putHtml("<b>Some</b> text")};
 *     clipboard.setContent(content);
 * </code></pre>
 * <p>
 * The {@link ClipboardContent} class is simply a map with convenience methods for dealing
 * with common data types added to a clipboard.
 * <p>
 * Because multiple representations of the same data may exist on the clipboard, and because
 * different applications have different capabilities for handling different content types,
 * it is important to place as many data representations on the clipboard as is practical to
 * facilitate external applications. Note that sometimes the operating system might be
 * helpful in some cases and add multiple types for you. For example, the Mac might set the
 * plain text string for you when you specify the RTF type. How and under what circumstances
 * this occurs is outside the realm of this specification, consult your OS documentation.
 * <p>
 * When reading data off the clipboard, it is important to look for the richest
 * supported type first. For example, if I have a text document which supports embedding of
 * images and media formats, when pasting content from the clipboard I should first check to
 * see if the content can be represented as media or as an image. If not, then I might check
 * for RTF or HTML or whatever rich text format is supported by my document type. If not,
 * then I might just take a String.
 * <p>
 * Or for example, if I have a plain text document, then I would simple get a String
 * representation and use that, if available. I can check to see if the clipboard "hasHtml"
 * or "hasString".
 * <pre>{@code
 *     if (clipboard.hasString()) { ... }
 * }</pre>
 * <p>
 * In addition to the common or built in types, you may put any arbitrary data onto the
 * clipboard (assuming it is serializable).
 * <p>
 * Content types are defined by the DataFormat objects.
 * The DataFormat class defines an immutable object, and there are a number of static final
 * fields for common DataFormat types. Of course application specific DataFormat types can also be
 * declared and used. The following two methods are equivalent (and the second call
 * will override the first!)
 * <pre>{@code
 *     ClipboardContent content = new ClipboardContent();
 *     content.putString("some text");
 *     content.put(DataFormat.PLAIN_TEXT, "other text");
 * }</pre>
 * <p>
 * On embedded platforms that do not have their own windowing system, the
 * Clipboard returned from Clipboard.getSystemClipboard() might not be
 * accessible from outside the JavaFX application.
 *</p>
 * <p>
 * If a security manager is present, the application must have the
 * {@link javafx.util.FXPermission} "accessClipboard" in order for the
 * Clipboard returned from Clipboard.getSystemClipboard() to be
 * accessible from outside the JavaFX application. For compatibility with
 * previous versions of the JDK the equivalent {@code AWTPermission}
 * "accessClipboard" will also allow the FX clipboard to be accessible from
 * outside the JavaFX application.
 * </p>
 * <p>
 * If the application lacks permission or if the platform doesn't support
 * a shared clipboard, the clipboard
 * returned by Clipboard.getSystemClipboard() can be used for exchange of data
 * between different parts of one JavaFX application but cannot be used to
 * exchange data between multiple applications.
 * </p>
 *
 * @since JavaFX 2.0
 */
public class Clipboard {

    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        ClipboardHelper.setClipboardAccessor(new ClipboardHelper.ClipboardAccessor() {

            @Override
            public boolean contentPut(Clipboard clipboard) {
                return clipboard.contentPut();
            }
        });
    }

    /**
     * Whether user has put something on this clipboard. Needed for DnD.
     */
    private boolean contentPut = false;

    // future:
    /*
     * JavaFX supports the concept of multiple independently named clipboards. There is a
     * predefined clipboard which represents the main system clipboard, but it is possible
     * to create custom clipboards if you so desire. Some platforms, such as Mac OS X,
     * define a number of different named clipboards. You can access these from JavaFX by
     * simply creating a Clipboard with the correct name. Typically there is no need to do
     * so in your applications since the UI Controls will use the correct System clipboards,
     * if applicable.
     * <p>
     *
     * Sometimes you may want to put a reference to a data representation on the clipboard
     *
     * rather than the data itself. For example, the user may have selected a large block of
     * text, and wants to copy this to the clipboard. Instead of having to actually produce
     * multiple copies of this text, a reference can be placed on the clipboard instead. When
     * the developer subsequently attempts to read the value off the clipboard, this reference
     * is resolved. Or suppose that I want to put a Node on the clipboard, such that the
     * representation of that Node on the clipboard is as an image.
     * <pre>{@code
     *     final Node node = ...;
     *     ClipboardReference ref = new ClipboardReference() {
     *         @Override public InputStream get() {
     *             // convert the node to an image
     *             // return an input stream to the image
     *             ...
     *         }
     *     };
     *     clipboard.putReference(DataFormat.IMAGE_TIFF, ref);
     * }</code>
     * <p>
     * At the appropriate time, a client reading off the clipboard will ask for the data and
     * the system will invoke the provided callback to stream the image data over to the client.
     */

    private final AccessControlContext acc = AccessController.getContext();

    /**
     * Gets the current system clipboard, through which data can be stored and
     * retrieved. There is ever only one system clipboard for a JavaFX application.
     * @return The single system clipboard, used for cut / copy / paste operations
     */
    public static Clipboard getSystemClipboard() {
        try {
            PermissionHelper.checkClipboardPermission();
            return getSystemClipboardImpl();
        } catch (final SecurityException e) {
            return getLocalClipboardImpl();
        }
    }

    TKClipboard peer;

    // Only allow Dragboard to extend from this
    Clipboard(TKClipboard peer) {
        Toolkit.getToolkit().checkFxUserThread();
        if (peer == null) {
            throw new NullPointerException();
        }
        peer.setSecurityContext(acc);
        this.peer = peer;
    }

    /**
     * Clears the clipboard of any and all content. Any subsequent call to
     * {@link #getContentTypes} before putting more content on the clipboard
     * will result in an empty set being returned.
     */
    public final void clear() {
        setContent(null);
    }

    /**
     * Gets the set of DataFormat types on this Clipboard instance which have
     * associated data registered on the clipboard. This set will always
     * be non-null and immutable. If the Clipboard is subsequently modifed,
     * this returned set is not updated.
     *
     * @return A non-null immutable set of content types.
     */
    public final Set<DataFormat> getContentTypes() {
        return peer.getContentTypes();
    }

    /**
     * Puts content onto the clipboard. This call will always result in
     * clearing all previous content from the clipboard, and replacing
     * it with whatever content is specified in the supplied
     * ClipboardContent map.
     *
     * @param content The content to put on the clipboard. If null, the
     *        clipboard is simply cleared and no new content added.
     * @return True if successful, false if the content fails to be added.
     * @throws NullPointerException if null data reference is passed for any
     *                              format
     */
//    public abstract boolean setContent(DataFormat uti, Object content);
    public final boolean setContent(Map<DataFormat, Object> content) {
        Toolkit.getToolkit().checkFxUserThread();
        if (content == null) {
            contentPut = false;
            peer.putContent(new Pair[0]);
            return true;
        } else {
            Pair<DataFormat, Object>[] data = new Pair[content.size()];
            int index = 0;
            for (Map.Entry<DataFormat, Object> entry : content.entrySet()) {
                data[index++] = new Pair<DataFormat, Object>(entry.getKey(), entry.getValue());
            }
            contentPut = peer.putContent(data);
            return contentPut;
        }
    }

    /**
     * Returns the content stored in this clipboard of the given type, or null
     * if there is no content with this type.
     * @param dataFormat the format type
     * @return The content associated with this type, or null if there is none
     */
    public final Object getContent(DataFormat dataFormat) {
        Toolkit.getToolkit().checkFxUserThread();
        return getContentImpl(dataFormat);
    }

    /**
     * Getting content overridable by internal subclasses.
     */
    Object getContentImpl(DataFormat dataFormat) {
        return peer.getContent(dataFormat);
    }

    /**
     * Tests whether there is any content on this clipboard of the given DataFormat type.
     * @param dataFormat the format type
     * @return true if there is content on this clipboard for this type
     */
    public final boolean hasContent(DataFormat dataFormat) {
        Toolkit.getToolkit().checkFxUserThread();
        return peer.hasContent(dataFormat);
    }

    /**
     * Gets whether a plain text String (DataFormat.PLAIN_TEXT) has been registered
     * on this Clipboard.
     * @return true if <code>hasContent(DataFormat.PLAIN_TEXT)</code> returns true, false otherwise
     */
    public final boolean hasString() {
        return hasContent(DataFormat.PLAIN_TEXT);
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
        return (String) getContent(DataFormat.PLAIN_TEXT);
    }

    /**
     * Gets whether a url String (DataFormat.URL) has been registered
     * on this Clipboard.
     * @return true if hasContent(DataFormat.URL) returns true, false otherwise
     */
    public final boolean hasUrl() {
        return hasContent(DataFormat.URL);
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
        return (String) getContent(DataFormat.URL);
    }

    /**
     * Gets whether an HTML text String (DataFormat.HTML) has been registered
     * on this Clipboard.
     * @return true if <code>hasContent(DataFormat.HTML)</code> returns true, false otherwise
     */
    public final boolean hasHtml() {
        return hasContent(DataFormat.HTML);
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
        return (String) getContent(DataFormat.HTML);
    }

    /**
     * Gets whether an RTF String (DataFormat.RTF) has been registered
     * on this Clipboard.
     * @return true if hasContent(DataFormat.RTF) returns true, false otherwise
     */
    public final boolean hasRtf() {
        return hasContent(DataFormat.RTF);
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
        return (String) getContent(DataFormat.RTF);
    }

    /**
     * Gets whether an Image (DataFormat.IMAGE) has been registered
     * on this Clipboard.
     * @return true if hasContent(DataFormat.IMAGE) returns true, false otherwise
     */
    public final boolean hasImage() {
        return hasContent(DataFormat.IMAGE);
    };

    /**
     * Gets the Image from the clipboard which had previously
     * been registered. This is equivalent to invoking
     * <code>getContent(DataFormat.IMAGE)</code>. If no such entry exists,
     * null is returned.
     * @return The Image on the clipboard associated with DataFormat.IMAGE,
     * or null if there is not one.
     */
    public final Image getImage() {
        return (Image) getContent(DataFormat.IMAGE);
    }

    /**
     * Gets whether an List of Files (DataFormat.FILES) has been registered
     * on this Clipboard.
     * @return true if hasContent(DataFormat.FILES) returns true, false otherwise
     */
    public final boolean hasFiles() {
        return hasContent(DataFormat.FILES);
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
        return (List<File>) getContent(DataFormat.FILES);
    }

    boolean contentPut() {
        return contentPut;
    }

    private static Clipboard systemClipboard;

    private static synchronized Clipboard getSystemClipboardImpl() {
        if (systemClipboard == null) {
            systemClipboard =
                    new Clipboard(Toolkit.getToolkit().getSystemClipboard());
        }
        return systemClipboard;
    }

    private static Clipboard localClipboard;

    private static synchronized Clipboard getLocalClipboardImpl() {
        if (localClipboard == null) {
            localClipboard =
                    new Clipboard(Toolkit.getToolkit().createLocalClipboard());
        }
        return localClipboard;
    }
}
