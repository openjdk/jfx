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

package com.sun.javafx.scene.control.rich.rtf;

import java.util.Enumeration;
import java.util.HashMap;
import javafx.scene.control.rich.model.StyleAttrs;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * Attribute Container
 */
public class MutableAttributeSet {
    private final HashMap<Object,Object> attributes = new HashMap<>();

    public MutableAttributeSet(MutableAttributeSet a) {
    }
    
    public MutableAttributeSet() {
    }

    public Object getAttribute(Object attr) {
        return null;
    }

    public Enumeration<Object> getAttributeNames() {
        return null;
    }
    
    public void addAttribute(Object attr, Object value) {
    }

    public void addAttributes(MutableAttributeSet a) {
    }

    public void removeAttribute(Object swingName) {
    }
    
    /**
     * Sets the resolving parent.  This is the set
     * of attributes to resolve through if an attribute
     * isn't defined locally.
     *
     * @param parent the parent
     */
    public void setResolveParent(MutableAttributeSet parent) {
    }

    public StyleAttrs getStyleAttrs() {
        return null;
    }

    public void setItalic(boolean b) {
    }

    public void setBold(boolean b) {
    }

    public void setUnderline(boolean b) {
    }

    public void setForeground(Color defaultColor) {
    }

    public void setLeftIndent(double d) {
    }

    public void setRightIndent(double d) {
    }

    public void setFirstLineIndent(double d) {
    }

    public void setFontFamily(String fontFamily) {
    }

    public void setBackground(Color bg) {
    }

    public void setAlignment(TextAlignment left) {
    }
}
