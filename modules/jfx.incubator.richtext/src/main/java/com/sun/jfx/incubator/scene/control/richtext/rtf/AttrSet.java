/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.richtext.rtf;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Attribute Container
 */
public class AttrSet {
    private final HashMap<Object, Object> attrs = new HashMap<>();
    private AttrSet parent;

    public AttrSet(AttrSet a) {
    }

    public AttrSet() {
    }

    public Object getAttribute(Object attr) {
        Object v = attrs.get(attr);
        if (v == null) {
            if (parent != null) {
                v = parent.getAttribute(attr);
            }
        }
        return v;
    }

    public Set<Object> getAttributeNames() {
        return attrs.keySet();
    }

    public void addAttribute(Object attr, Object value) {
        attrs.put(attr, value);
    }

    public void addAttributes(AttrSet a) {
        attrs.putAll(a.attrs);
    }

    public void removeAttribute(Object attr) {
        attrs.remove(attr);
    }

    /**
     * Sets the resolving parent.  This is the set
     * of attributes to resolve through if an attribute
     * isn't defined locally.
     *
     * @param parent the parent
     */
    public void setResolveParent(AttrSet parent) {
        this.parent = parent;
    }

    public StyleAttributeMap getStyleAttributeMap() {
        return StyleAttributeMap.builder().
            setBold(getBoolean(StyleAttributeMap.BOLD)).
            setFontFamily(getString(StyleAttributeMap.FONT_FAMILY)).
            setItalic(getBoolean(StyleAttributeMap.ITALIC)).
            setTextColor(getColor(StyleAttributeMap.TEXT_COLOR)).
            setUnderline(getBoolean(StyleAttributeMap.UNDERLINE)).
            build();
    }

    private boolean getBoolean(Object attr) {
        return Boolean.TRUE.equals(attrs.get(attr));
    }

    private String getString(Object attr) {
        Object v = attrs.get(attr);
        if (v instanceof String s) {
            return s;
        }
        return null;
    }

    private Color getColor(Object attr) {
        Object v = attrs.get(attr);
        if (v instanceof Color c) {
            return c;
        }
        return null;
    }

    public void setItalic(boolean on) {
        attrs.put(StyleAttributeMap.ITALIC, on);
    }

    public void setBold(boolean on) {
        attrs.put(StyleAttributeMap.BOLD, on);
    }

    public void setUnderline(boolean on) {
        attrs.put(StyleAttributeMap.UNDERLINE, on);
    }

    public void setForeground(Color c) {
        attrs.put(StyleAttributeMap.TEXT_COLOR, c);
    }

    public void setLeftIndent(double d) {
        // TODO
    }

    public void setRightIndent(double d) {
        // TODO
    }

    public void setFirstLineIndent(double d) {
        // TODO
    }

    public void setFontFamily(String fontFamily) {
        attrs.put(StyleAttributeMap.FONT_FAMILY, fontFamily);
    }

    public void setBackground(Color bg) {
        // TODO
    }

    public void setAlignment(TextAlignment left) {
        // TODO
    }

    /**
     * An internal AttrSet holder.  Original name: MockAttributeSet.
     */
    public static class Holder extends AttrSet {
        public HashMap<Object, Object> backing;

        public boolean isEmpty() {
            return backing.isEmpty();
        }

        public int getAttributeCount() {
            return backing.size();
        }

        public boolean isDefined(Object name) {
            return (backing.get(name)) != null;
        }

        public boolean isEqual(AttrSet attr) {
            throw new InternalError();
        }

        public AttrSet copyAttributes() {
            throw new InternalError();
        }

        @Override
        public Object getAttribute(Object name) {
            return backing.get(name);
        }

        public void addAttribute(StyleAttribute name, Object value) {
            backing.put(name, value);
        }

        @Override
        public void addAttributes(AttrSet attr) {
            for (Object k : attr.getAttributeNames()) {
                Object v = attr.getAttribute(k);
                backing.put(k, v);
            }
        }

        @Override
        public void removeAttribute(Object name) {
            backing.remove(name);
        }

        public void removeAttributes(AttrSet attr) {
            throw new InternalError();
        }

        public void removeAttributes(Enumeration<?> en) {
            throw new InternalError();
        }

        @Override
        public void setResolveParent(AttrSet pp) {
            throw new InternalError();
        }

        @Override
        public Set<Object> getAttributeNames() {
            return backing.keySet();
        }

        public boolean containsAttribute(Object name, Object value) {
            throw new InternalError();
        }

        public boolean containsAttributes(AttrSet attr) {
            throw new InternalError();
        }

        public AttrSet getResolveParent() {
            throw new InternalError();
        }
    }
}
