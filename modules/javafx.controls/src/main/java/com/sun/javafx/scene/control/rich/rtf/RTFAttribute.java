/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package com.sun.javafx.scene.control.rich.rtf;

import javafx.scene.control.rich.model.StyleAttribute;

/**
 * This interface describes a class which defines a 1-1 mapping between
 * an RTF keyword and a SwingText attribute.
 */
public abstract class  RTFAttribute {
    public static final int D_CHARACTER = 0;
    public static final int D_PARAGRAPH = 1;
    public static final int D_SECTION = 2;
    public static final int D_DOCUMENT = 3;
    public static final int D_META = 4;
    
    public abstract boolean set(AttrSet target);

    public abstract boolean set(AttrSet target, int parameter);

    public abstract boolean setDefault(AttrSet target);
    
    protected final int domain;
    protected final StyleAttribute swingName;
    protected final String rtfName;

    protected RTFAttribute(int domain, StyleAttribute swingName, String rtfName) {
        this.domain = domain;
        this.swingName = swingName;
        this.rtfName = rtfName;
    }

    public int domain() {
        return domain;
    }

    public StyleAttribute swingName() {
        return swingName;
    }

    public String rtfName() {
        return rtfName;
    }
}
