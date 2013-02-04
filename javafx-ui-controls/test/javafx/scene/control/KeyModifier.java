/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import com.sun.javafx.Utils;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.input.KeyCode;

public enum KeyModifier {
    SHIFT,
    CTRL,
    ALT,
    META;
    
    public static KeyModifier getShortcutKey() {
        // The StubToolkit doesn't know what the platform shortcut key is, so 
        // we have to tell it here (and lets not be cute about optimising this
        // code as we need the platform shortcut key to be known elsewhere in the
        // code base for keyboard navigation tests to work accurately).
        if (Toolkit.getToolkit() instanceof StubToolkit) {
            ((StubToolkit)Toolkit.getToolkit()).setPlatformShortcutKey(Utils.isMac() ? KeyCode.META : KeyCode.CONTROL);
        } 
        
        switch (Toolkit.getToolkit().getPlatformShortcutKey()) {
            case SHIFT:
                return SHIFT;

            case CONTROL:
                return CTRL;

            case ALT:
                return ALT;

            case META:
                return META;

            default:
                return null;
        }
    }
}
