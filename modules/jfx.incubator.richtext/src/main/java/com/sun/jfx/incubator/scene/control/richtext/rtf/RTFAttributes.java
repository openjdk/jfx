/*
 * Copyright (c) 1997, 2024, Oracle and/or its affiliates. All rights reserved.
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
// adapted from package javax.swing.text.rtf;
package com.sun.jfx.incubator.scene.control.richtext.rtf;

import java.util.HashMap;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

class RTFAttributes {
    private static final RTFAttribute[] attributes = {
        new BooleanAttribute(RTFAttribute.D_CHARACTER, StyleAttributeMap.ITALIC, "i"),
        new BooleanAttribute(RTFAttribute.D_CHARACTER, StyleAttributeMap.BOLD, "b"),
        new BooleanAttribute(RTFAttribute.D_CHARACTER, StyleAttributeMap.UNDERLINE, "ul"),
        new BooleanAttribute(RTFAttribute.D_CHARACTER, StyleAttributeMap.STRIKE_THROUGH, "strike")
    };

    public static HashMap<String, RTFAttribute> attributesByKeyword() {
        HashMap<String, RTFAttribute> d = new HashMap<String, RTFAttribute>(attributes.length);
        for (RTFAttribute attribute : attributes) {
            d.put(attribute.rtfName(), attribute);
        }
        return d;
    }

    /**
     * Defines a boolean attribute.
     */
    static class BooleanAttribute extends RTFAttribute {
        private final boolean rtfDefault;
        private final boolean defaultValue;

        public BooleanAttribute(int domain, StyleAttribute s, String rtfName, boolean ds, boolean dr) {
            super(domain, s, rtfName);
            defaultValue = ds;
            rtfDefault = dr;
        }

        public BooleanAttribute(int d, StyleAttribute s, String r) {
            super(d, s, r);

            defaultValue = false;
            rtfDefault = false;
        }

        @Override
        public boolean set(AttrSet target) {
            /* TODO: There's some ambiguity about whether this should
               *set* or *toggle* the attribute. */
            target.addAttribute(attribute, Boolean.TRUE);

            return true; /* true indicates we were successful */
        }

        @Override
        public boolean set(AttrSet target, int parameter) {
            /* See above note in the case that parameter==1 */
            Boolean value = Boolean.valueOf(parameter != 0);
            target.addAttribute(attribute, value);
            return true; /* true indicates we were successful */
        }

        @Override
        public boolean setDefault(AttrSet target) {
            if (defaultValue != rtfDefault || (target.getAttribute(attribute) != null)) {
                target.addAttribute(getStyleAttribute(), Boolean.valueOf(rtfDefault));
            }
            return true;
        }
    }

    /**
     * Defines an object attribute.
     */
    static class AssertiveAttribute extends RTFAttribute {
        private final Object value;

        public AssertiveAttribute(int d, StyleAttribute s, String r) {
            super(d, s, r);
            value = Boolean.valueOf(true);
        }

        public AssertiveAttribute(int d, StyleAttribute s, String r, Object v) {
            super(d, s, r);
            value = v;
        }

        public AssertiveAttribute(int d, StyleAttribute s, String r, int v) {
            super(d, s, r);
            value = Integer.valueOf(v);
        }

        @Override
        public boolean set(AttrSet target) {
            if (value == null) {
                target.removeAttribute(attribute);
            } else {
                target.addAttribute(attribute, value);
            }
            return true;
        }

        @Override
        public boolean set(AttrSet target, int parameter) {
            return false;
        }

        @Override
        public boolean setDefault(AttrSet target) {
            target.removeAttribute(attribute);
            return true;
        }
    }

    /**
     * Defines a numeric attribute.
     */
    static class NumericAttribute extends RTFAttribute {
        private final int rtfDefault;
        private final Number defaultValue;
        private final float scale;

        protected NumericAttribute(int d, StyleAttribute s, String r) {
            super(d, s, r);
            rtfDefault = 0;
            defaultValue = null;
            scale = 1f;
        }

        public NumericAttribute(int d, StyleAttribute s, String r, int ds, int dr) {
            this(d, s, r, Integer.valueOf(ds), dr, 1f);
        }

        public NumericAttribute(int d, StyleAttribute s, String r, Number ds, int dr, float sc) {
            super(d, s, r);
            defaultValue = ds;
            rtfDefault = dr;
            scale = sc;
        }

        @Override
        public boolean set(AttrSet target) {
            return false;
        }

        @Override
        public boolean set(AttrSet target, int parameter) {
            Number v;
            if (scale == 1f) {
                v = Integer.valueOf(parameter);
            } else {
                v = Float.valueOf(parameter / scale);
            }
            target.addAttribute(attribute, v);
            return true;
        }

        @Override
        public boolean setDefault(AttrSet target) {
            Number old = (Number)target.getAttribute(attribute);
            if (old == null) {
                old = defaultValue;
            }
            if (old != null && ((scale == 1f && old.intValue() == rtfDefault)
                || (Math.round(old.floatValue() * scale) == rtfDefault))) {
                return true;
            }
            set(target, rtfDefault);
            return true;
        }
    }
}
