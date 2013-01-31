/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import com.sun.javafx.Utils;

public enum KeyModifier {
    SHIFT,
    CTRL,
    ALT,
    CMD;
    
    /**
     * Returns CMD on Mac OS, and CTRL on all other operating systems
     */
    public static KeyModifier getShortcutKey() {
        return Utils.isMac() ? CMD : CTRL;
    }
}
