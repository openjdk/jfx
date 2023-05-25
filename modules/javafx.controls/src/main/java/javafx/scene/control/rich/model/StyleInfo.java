/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.rich.model;

import javafx.scene.control.rich.RichTextArea;
import javafx.scene.control.rich.StyleResolver;

/**
 * Style information of a text segment can be represented either by a combination of
 * direct style (e.g. "-fx-fill:red;") and CSS stylesheet names, or a set of {@link StyleAttribute}s.
 * 
 * Objects of this class are immutable.
 */
public abstract class StyleInfo {
    /** returns true if styles are represented by the attributes */
    public boolean hasAttributes() { return false; }
    
    /** returns attributes, or null if {@link #hasAttributes()} is false */
    public StyleAttrs getAttributes() { return null; }
    
    /** returns a direct style string which can be null */
    public String getDirectStyle() { return null; }
    
    /** returns an array of CSS style names, can be null */
    public String[] getCss() { return null; }

    public static final StyleInfo NONE = StyleInfo.of(null, null);

    private StyleInfo() {
    }
    
    public StyleAttrs getStyleAttrs(StyleResolver resolver) {
        if (hasAttributes()) {
            return getAttributes();
        } else if (resolver == null) {
            return null;
        }
    
        // convert styles to attributes
        String sty = getDirectStyle();
        String[] css = getCss();
        if ((sty == null) && (css == null)) {
            return null;
        }
        return resolver.convert(sty, css);
    }
    
    public static StyleInfo of(String direct, String[] css) {
        return new StyleInfo() {
            @Override
            public String getDirectStyle() {
                return direct;
            }
            
            @Override
            public String[] getCss() {
                return css;
            }
        };
    }
    
    public static StyleInfo of(StyleAttrs a) {
        return new StyleInfo() {
            @Override
            public boolean hasAttributes() {
                return true;
            }

            public StyleAttrs getAttributes() {
                // TODO only if immutable
                return a;
            }
        };
    }
}
