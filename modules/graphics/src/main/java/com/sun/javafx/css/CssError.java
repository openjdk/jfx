/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.Styleable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import javafx.css.CssMetaData;
import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Encapsulate information about the source and nature of errors encountered
 * while parsing CSS or applying styles to Nodes. 
 */
public class CssError {
 
    // RT-20643 - hold a ref so CssError doesn't leak Scene.
    private static Reference<Scene> SCENE_REF;
            
    /** 
     * Set the static scene variable. This scene will be set on all CssErrors
     * generated after the call is made. The argument may be null. Null should
     * be passed when the code exits a method from which CssErrors may be 
     * created. This is intended internal use and should not be called from
     * outside the css code.
     */
    public static void setCurrentScene(Scene scene) {
        
        // Treat as a no-op if noone cares about CssErrors
        if (StyleManager.getErrors() == null) return;
        
        if (scene != null) {
            // don't make new ref for same scene
            final Scene oldScene = SCENE_REF != null ? SCENE_REF.get() : null;
            if (oldScene != scene) {
                SCENE_REF = new WeakReference<Scene>(scene);
            }
        } else {
            SCENE_REF = null;
        }
    }
    
    /** @return The error message from the CSS code. */
    public final String getMessage() {
        return message;
    }

    public CssError(String message) {
        this.message = message;
        // RT-20643        
        this.sceneRef = SCENE_REF;
    }
    
    /** 
     * @return The Scene in which this error occurred, if known, or null.
     */
    public Scene getScene() {
        return sceneRef != null ? sceneRef.get() : null;
    }
    
    // RT-20643 - track the scene that this error belongs to       
    // Note that CssError has the potential to leak Scene so the Scene
    // variable is held as a Reference.
    private final Reference<Scene> sceneRef;
    protected final String message;

    @Override
    public String toString() {
        return "CSS Error: " + message;
    }

    /** Encapsulate errors arising from parsing of stylesheet files */
    public final static class StylesheetParsingError extends CssError { 
        
        public StylesheetParsingError(String url, String message) {
            super(message);
            this.url = url;
        }
        
        public String getURL() {
            return url;
        }
        
        private final String url;

        @Override
        public String toString() {
            final String path = url != null ? url : "?";
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
        
        public PropertySetError(CssMetaData styleableProperty, 
                Styleable styleable, String message) {
            super(message);
            this.styleableProperty = styleableProperty;
            this.styleable = styleable;
        }
        
        public Styleable getStyleable() {
            return styleable;
        }
        
        public CssMetaData getProperty() {
            return styleableProperty;
        }
        
        private final CssMetaData styleableProperty;
        private final Styleable styleable;
        
    }
}     

