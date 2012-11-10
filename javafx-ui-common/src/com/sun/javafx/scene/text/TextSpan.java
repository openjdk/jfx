/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.text;

import com.sun.javafx.geom.RectBounds;

public interface TextSpan {
    /**
     * The text for the span, can be empty but not null.
     */
    public String getText();

    /**
     * The font for the span, if null the span is handled as embedded object.
     */
    public Object getFont();
    
    /**
     * The bounds for embedded object, only used the font returns null.
     * The text for a embedded object should be a single char ("\uFFFC" is 
     * recommended).
     */
    public RectBounds getBounds();
}
