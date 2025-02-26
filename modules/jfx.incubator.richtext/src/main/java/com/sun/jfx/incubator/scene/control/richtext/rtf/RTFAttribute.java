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

import jfx.incubator.scene.control.richtext.model.StyleAttribute;

/**
 * This abstract class defines a 1-1 mapping between
 * an RTF keyword and a StyleAttribute attribute.
 */
abstract class  RTFAttribute {
    public static final int D_CHARACTER = 0;
    public static final int D_PARAGRAPH = 1;
    public static final int D_SECTION = 2;
    public static final int D_DOCUMENT = 3;
    public static final int D_META = 4;

    public abstract boolean set(AttrSet target);

    public abstract boolean set(AttrSet target, int parameter);

    public abstract boolean setDefault(AttrSet target);

    protected final int domain;
    protected final StyleAttribute attribute;
    protected final String rtfName;

    protected RTFAttribute(int domain, StyleAttribute attribute, String rtfName) {
        this.domain = domain;
        this.attribute = attribute;
        this.rtfName = rtfName;
    }

    public int domain() {
        return domain;
    }

    public StyleAttribute getStyleAttribute() {
        return attribute;
    }

    public String rtfName() {
        return rtfName;
    }
}
