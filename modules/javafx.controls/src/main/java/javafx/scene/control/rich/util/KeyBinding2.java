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

package javafx.scene.control.rich.util;

import java.util.EnumSet;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import com.sun.javafx.PlatformUtil;

/**
 * Key binding provides a way to map key event to a hash table key for easy matching.
 * Also it allows for encoding platform-specific keys without resorting to nested and/or
 * multiple key maps.
 */
// TODO convert to class
@Deprecated
public record KeyBinding2(KeyCode code, EnumSet<KCondition> modifiers) {
    /** 
     * Creates a KeyBinding from a KeyEvent.  This call drops multiple key modifiers, performing
     * translation when necessary.  May return null if the event does not correspond to a valid KeyBinding.
     */
    public static KeyBinding2 from(KeyEvent ev) {
        EnumSet<KCondition> m = EnumSet.noneOf(KCondition.class);
        EventType<KeyEvent> t = ev.getEventType();
        if(t == KeyEvent.KEY_PRESSED) {
            m.add(KCondition.KEY_PRESS);
        } else if(t == KeyEvent.KEY_RELEASED) {
            m.add(KCondition.KEY_RELEASE);
        } else if(t == KeyEvent.KEY_TYPED) {
            m.add(KCondition.KEY_TYPED);
        } else {
            // FIX what is it?
            return null;
        }
        
        boolean alt = ev.isAltDown();
        boolean ctrl = ev.isControlDown();
        boolean meta = ev.isMetaDown();
        boolean shortcut = ev.isShortcutDown();
        boolean option = false;
        boolean command = false;
        
        boolean mac = PlatformUtil.isMac();
        boolean win = PlatformUtil.isWindows();

        // drop multiple modifiers, translating when necessary 

        if (mac) {
            if (alt) {
                alt = false;
                option = true;
            }
            if (shortcut) {
                meta = false;
                command = true;
            }
        } else {
            if (ctrl) {
                shortcut = false;
            }
        }

        if (alt) {
            m.add(KCondition.ALT);
        }

        if (command) {
            m.add(KCondition.COMMAND);
        }

        if (ctrl) {
            m.add(KCondition.CTRL);
        }

        if (meta) {
            m.add(KCondition.META);
        }

        if (option) {
            m.add(KCondition.OPTION);
        }

        if (ev.isShiftDown()) {
            m.add(KCondition.SHIFT);
        }

        KeyCode code = ev.getCode();
        KeyBinding2 keyBinding = new KeyBinding2(code, m);
        //System.err.println("kb=" + keyBinding + " ev=" + toString(ev)); // FIX
        return keyBinding;
    }

    // FIX remove, debug
    private static String toString(KeyEvent ev) {
        StringBuilder sb = new StringBuilder("KeyEvent{");
        sb.append("type=").append(ev.getEventType());
        sb.append(", char=").append(ev.getCharacter());

        String ch = ev.getCharacter();
        int sz = ch.length();
        if (sz > 0) {
            sb.append("(");
            for (int i = 0; i < ch.length(); i++) {
                sb.append(String.format("%02X", (int)ch.charAt(i)));
            }
            sb.append(")");
        }

        sb.append(", code=").append(ev.getCode());

        if (ev.isShiftDown()) {
            sb.append(", shift");
        }
        if (ev.isControlDown()) {
            sb.append(", control");
        }
        if (ev.isAltDown()) {
            sb.append(", alt");
        }
        if (ev.isMetaDown()) {
            sb.append(", meta");
        }
        if (ev.isShortcutDown()) {
            sb.append(", shortcut");
        }

        return sb.append("}").toString();
    }
    
    private static void replace(EnumSet<KCondition> m, KCondition c, KCondition replaceWith) {
        if (m.contains(c)) {
            m.remove(c);
            m.add(replaceWith);
        }
    }

    /**
     * Creates a key binding.
     * Might return null if the specified modifiers specify a different platform.
     */
    public static KeyBinding2 of(KeyCode code, KCondition... modifiers) {
        EnumSet<KCondition> m = EnumSet.noneOf(KCondition.class);
        for (KCondition modifier : modifiers) {
            m.add(modifier);
        }

        // mac-windows for now.  we might rethink the logic later if necessary.
        boolean mac = PlatformUtil.isMac();
        boolean win = PlatformUtil.isWindows();

        if (mac) {
            if (m.contains(KCondition.NOT_FOR_MAC)) {
                return null;
            } else if (m.contains(KCondition.FOR_WIN)) {
                return null;
            } else if (m.contains(KCondition.WINDOWS)) {
                return null;
            }

            replace(m, KCondition.ALT, KCondition.OPTION);
            replace(m, KCondition.META, KCondition.COMMAND);
            replace(m, KCondition.SHORTCUT, KCondition.COMMAND);
        } else if (PlatformUtil.isWindows()) {
            if (m.contains(KCondition.NOT_FOR_WIN)) {
                return null;
            } else if (m.contains(KCondition.FOR_MAC)) {
                return null;
            }

            replace(m, KCondition.SHORTCUT, KCondition.CTRL);
        }

        if (!mac) {
            if (m.contains(KCondition.COMMAND)) {
                return null;
            } else if (m.contains(KCondition.OPTION)) {
                return null;
            }

            replace(m, KCondition.WINDOWS, KCondition.META);
        }

        // remove platform 
        m.remove(KCondition.FOR_MAC);
        m.remove(KCondition.NOT_FOR_MAC);
        m.remove(KCondition.FOR_WIN);
        m.remove(KCondition.NOT_FOR_WIN);

        boolean pressed = m.contains(KCondition.KEY_PRESS);
        boolean released = m.contains(KCondition.KEY_PRESS);
        boolean typed = m.contains(KCondition.KEY_TYPED);

        int ct = 0;
        KCondition t = null;
        if (pressed) {
            ct++;
            t = KCondition.KEY_PRESS;
        }
        if (released) {
            ct++;
            t = KCondition.KEY_RELEASE;
        }
        if (typed) {
            ct++;
            t = KCondition.KEY_TYPED;
        }

        // validate event type
        if (ct > 1) {
            throw new IllegalArgumentException("more than one key event type is specified");
        }

        if (t == null) {
            t = KCondition.KEY_PRESS;
        }
        m.add(t);

        // TODO validate: shortcut and !(other shortcut modifier)
        return new KeyBinding2(code, m);
    }
}
