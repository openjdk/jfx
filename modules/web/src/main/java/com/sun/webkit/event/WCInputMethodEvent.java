/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.event;

import java.util.Arrays;

public final class WCInputMethodEvent {
    // event id
    public static final int INPUT_METHOD_TEXT_CHANGED = 0;
    public static final int CARET_POSITION_CHANGED = 1;

    private final int id;
    private final String composed;
    private final String committed;
    private final int[] attributes;
    private final int caretPosition;

    public WCInputMethodEvent(String composed, String committed,
                              int[] attributes, int caretPosition) {
        this.id = INPUT_METHOD_TEXT_CHANGED;
        this.composed = composed;
        this.committed = committed;
        this.attributes = Arrays.copyOf(attributes, attributes.length);
        this.caretPosition = caretPosition;
    }

    public WCInputMethodEvent(int caretPosition) {
        this.id = CARET_POSITION_CHANGED;
        this.composed = null;
        this.committed = null;
        this.attributes = null;
        this.caretPosition = caretPosition;
    }

    public int getID() {
        return id;
    }

    public String getComposed() {
        return composed;
    }

    public String getCommitted() {
        return committed;
    }

    public int[] getAttributes() {
        return Arrays.copyOf(attributes, attributes.length);
    }

    public int getCaretPosition() {
        return caretPosition;
    }
}
