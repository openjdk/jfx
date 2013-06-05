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

package com.sun.javafx.css;

import com.sun.javafx.collections.TrackableObservableList;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.css.StyleOrigin;

/**
 * A stylesheet which can apply properties to a tree of objects.  A stylesheet 
 * is a collection of zero or more {@link Rule Rules}, each of which is applied 
 * to each object in the tree.  Typically the selector will examine the object to
 * determine whether or not it is applicable, and if so it will apply certain 
 * property values to the object.
 * <p>
 * Stylesheets can be parsed from CSS documents or created programmatically.  
 * Once created, stylesheets can be freely modified, but the modifications do 
 * not affect styled objects until a subsequent {@link #applyTo} or 
 * {@link #reapply}.
 *
 */
public class Stylesheet {

    private final URL url;
    /** The URL from which the stylesheet was loaded.
     * @return The URL from which the stylesheet was loaded, or null if 
     *         the stylesheet was created from an inline style. 
     */
    public URL getUrl() {
        return url;
    }

    /**
     * True if this style came from user stylesheet, we need to know this so 
     * that we can make user important styles have higher priority than
     * author styles
     */
    private StyleOrigin origin = StyleOrigin.AUTHOR;
    public StyleOrigin getOrigin() {
        return origin;
    }
    public void setOrigin(StyleOrigin origin) {
        this.origin = origin;
    }

    /** All the rules contained in the stylesheet in the order they are in the file */
    private final ObservableList<Rule> rules = new TrackableObservableList<Rule>() {

        @Override
        protected void onChanged(Change<Rule> c) {
            c.reset();
            while (c.next()) {
                if (c.wasAdded()) {
                    for(Rule rule : c.getAddedSubList()) {
                        rule.setStylesheet(Stylesheet.this);
                    }
                } else if (c.wasRemoved()) {
                    for (Rule rule : c.getRemoved()) {
                        if (rule.getStylesheet() == Stylesheet.this) rule.setStylesheet(null);
                    }
                }
            }
        }
    };

    /** List of all font faces */
    private final List<FontFace> fontFaces = new ArrayList<FontFace>();

    /**
     * Constructs a stylesheet with the base URI defaulting to the root
     * path of the application.
     */
    public Stylesheet() {

//        ClassLoader cl = Thread.currentThread().getContextClassLoader();
//        this.url = (cl != null) ? cl.getResource("") : null;
        //
        // RT-17344
        // The above code is unreliable. The getResource call is intended
        // to return the root path of the Application instance, but it sometimes
        // returns null. Here, we'll set url to null and then when a url is 
        // resolved, the url path can be used in the getResource call. For 
        // example, if the css is -fx-image: url("images/duke.png"), we can
        // do cl.getResouce("images/duke.png") in URLConverter
        //

        this(null);
    }

    /**
     * Constructs a Stylesheet using the given URL as the base URI. The
     * parameter may not be null.
     */
    public Stylesheet(URL url) {

        this.url = url;
        
    }

    public List<Rule> getRules() {
        return rules;
    }

    public List<FontFace> getFontFaces() {
        return fontFaces;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Stylesheet) {
            Stylesheet other = (Stylesheet)obj;
            
            if (this.url == null && other.url == null) {
                return true;
            } else if (this.url == null || other.url == null) {
                return false;
            } else {
                // convert to Strings, as URL.equals is slow. See here:
                // http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html
                String thisUrlString = this.url.toExternalForm();
                String otherUrlString = other.url.toExternalForm();
                return thisUrlString.equals(otherUrlString);
            }
        }
        return false;
    }
    
    @Override public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this.url != null ? this.url.hashCode() : 0);
        return hash;
    }

    /** Returns a string representation of this object. */
    public @Override String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("/* ");
        if (url != null) sbuf.append(url);
        if (rules.isEmpty()) {
            sbuf.append(" */");
        } else {
            sbuf.append(" */\n");
            for(int r=0; r<rules.size(); r++) {
                sbuf.append(rules.get(r));
                sbuf.append('\n');
            }
        }
        return sbuf.toString();
    }

    public void writeBinary(final DataOutputStream os, final StringStore stringStore)
        throws IOException 
    {
        int index = stringStore.addString(origin.name());
        os.writeShort(index);
        os.writeShort(rules.size());
        for (Rule r : rules) r.writeBinary(os,stringStore);
    }
    
    // protected for unit testing 
    /** @treatAsPrivate public to allow unit testing */
    public void readBinary(DataInputStream is, String[] strings)
        throws IOException 
    {
        final int index = is.readShort();
        this.setOrigin(StyleOrigin.valueOf(strings[index]));
        final int nRules = is.readShort();
        List<Rule> persistedRules = new ArrayList<Rule>(nRules);
        for (int n=0; n<nRules; n++) {
            persistedRules.add(Rule.readBinary(is,strings));
        }
        this.rules.addAll(persistedRules);
        
    }

    /** Load a binary stylesheet file from a input stream */
    public static Stylesheet loadBinary(URL url) {

        if (url == null) return null;

        Stylesheet stylesheet = null;
        InputStream inputStream = null;
        BufferedInputStream bufferedInputStream = null;
        DataInputStream dataInputStream = null;
        try {
            inputStream = url.openStream();
            // current bss file is 33k so this leaves a little scope at 40k
            bufferedInputStream = new BufferedInputStream(inputStream, 40 * 1024);

            dataInputStream = new DataInputStream(bufferedInputStream);
            // read file version
            final int version = dataInputStream.readShort();
            if (version != 2)
                throw new IOException(url.toString() + " wrong file version. got "
                        + version + " expected 2");
            // read strings
            final String[] strings = StringStore.readBinary(dataInputStream);
            // read binary data
            stylesheet = new Stylesheet(url);
            stylesheet.readBinary(dataInputStream,strings);

        } catch (FileNotFoundException fnfe) {
            // This comes from url.openStream() and is expected. 
            // It just means that the .bss file doesn't exist.
        } catch (IOException ignored) {
            // TODO: User logger here
            System.err.println(ignored);
            ignored.printStackTrace(System.err);
        } finally {
            try {
                if (dataInputStream != null) {
                    dataInputStream.close();
                } else if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                } else if (inputStream != null) {
                    inputStream.close();
                }
            } catch(IOException ioe) {
            }
        }

        // return stylesheet
        return stylesheet;
    }
}
