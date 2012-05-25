/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URL;

/**
 * Encapsulate information about the source and nature of errors encountered
 * while parsing CSS or applying styles to Nodes. 
 */
public class CssError {
 
    /** The error message from the CSS code */
    public final String getMessage() {
        return message;
    }
        
    public CssError(String message) {
        this.message = message;
    }
    
    protected final String message;

    @Override
    public String toString() {
        return "CSS Error: " + message;
    }

    /** Encapsulate errors arising from parsing of stylesheet files */
    public final static class StylesheetParsingError extends CssError { 
        
        public StylesheetParsingError(URL url, String message) {
            super(message);
            this.url = url;
        }
        
        public URL getURL() {
            return url;
        }
        
        private final URL url;

        @Override
        public String toString() {
            final String path = url != null ? url.toExternalForm() : "?";
            // TBD: i18n
            return "CSS Error parsing " + path + ": " + message;
        }
        
    }
    
    /** Encapsulate errors arising from parsing of Node's style property */
    public final static class InlineStyleParsingError extends CssError { 
        
        public InlineStyleParsingError(Styleable styleable, String message) {
            super(message);
            this.styleable = styleable;
        }
        
        public Styleable getStyleable() {
            return styleable;
        }
        
        private final Styleable styleable;
        
        @Override
        public String toString() {
            final String inlineStyle = styleable.getStyle();
            final String source = styleable.toString();
            // TBD: i18n
            return "CSS Error parsing in-line style \'" + inlineStyle + 
                    "\' from " + source + ": " + message;
        }
    } 
    
    /** 
     * Encapsulate errors arising from parsing when the style is not 
     * an in-line style nor is the style from a stylesheet. Primarily to
     * support unit testing.
     */
    public final static class StringParsingError extends CssError { 
        
        public StringParsingError(String style, String message) {
            super(message);
            this.style = style;
        }
        
        public String getStyle() {
            return style;
        }
        
        private final String style;
        
        @Override
        public String toString() {
            // TBD: i18n
            return "CSS Error parsing \'" + style + ": " + message;
        }
    } 
    
    /** Encapsulates errors arising from applying a style to a Node. */
    public final static class PropertySetError extends CssError { 
        
        public PropertySetError(StyleableProperty styleableProperty, 
                Styleable styleable, String message) {
            super(message);
            this.styleableProperty = styleableProperty;
            this.styleable = styleable;
        }
        
        public Styleable getStyleable() {
            return styleable;
        }
        
        public StyleableProperty getProperty() {
            return styleableProperty;
        }
        
        private final StyleableProperty styleableProperty;
        private final Styleable styleable;
        
    }
}     

