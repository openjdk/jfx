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

package javafx.incubator.scene.control.behavior;

import java.util.EnumSet;
import java.util.Objects;
import javafx.event.EventType;
import javafx.incubator.scene.control.util.Util;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Key binding provides a way to map key event to a hash table key for easy matching.
 * Also it allows for encoding platform-specific keys without resorting to nested and/or
 * multiple key maps.
 *
 * @since 22
 */
public class KeyBinding implements EventCriteria<KeyEvent> {
    /**
     * Condition used to build input key mappings.
     * <p>
     * The KCondition values are used as keys in a hash table, so when the platform sends a key event with multiple
     * modifiers, some modifiers are dropped in order to make the final key binding to function lookup unambiguous.
     * <p>
     * The mapping is as follows:
     * <pre>
     * KCondition    Mac         Windows/Linux
     * ALT           OPTION      ALT
     * COMMAND       COMMAND     (ignored)
     * CTRL          CTRL        CTRL
     * META          COMMAND     META
     * OPTION        OPTION      (ignored)
     * SHIFT         SHIFT       SHIFT
     * SHORTCUT      COMMAND     CTRL
     * WINDOWS       (ignored)   META
     * </pre>
     */
    private enum KCondition {
        // modifier keys
        /** ALT modifier, mapped to OPTION on Mac, ALT on Windows/Linux */
        ALT,
        /** COMMAND modifier, mapped to COMMAND on Mac only */
        COMMAND,
        /** CTRL modifier */
        CTRL,
        /** META modifier, mapped to COMMAND on Mac, META on Windows/Linux */
        META,
        /** OPTION modifier, mapped to OPTION on Mac only */
        OPTION,
        /** SHIFT modifier */
        SHIFT,
        /** SHORTCUT modifier, mapped to COMMAND on Mac, CTRL on Windows/Linux */
        SHORTCUT,
        /** Windows key modifier (⊞), mapped to WINDOWS on Windows only */
        WINDOWS,

        // event types
        /** a key press event */
        KEY_PRESS,
        /** a key release event */
        KEY_RELEASE,
        /** a key typed event */
        KEY_TYPED,
    }

    private final Object key; // KeyCode or String
    private final EnumSet<KCondition> modifiers;

    private KeyBinding(Object key, EnumSet<KCondition> modifiers) {
        this.key = key;
        this.modifiers = modifiers;
    }

    /**
     * Utility method creates a KeyBinding corresponding to a key press.
     *
     * @param code key code
     * @return KeyBinding
     */
    public static KeyBinding of(KeyCode code) {
        return create(code, KCondition.KEY_PRESS);
    }

    /**
     * Utility method creates a KeyBinding corresponding to a command-code key press.
     *
     * @param code key code
     * @return KeyBinding
     */
    public static KeyBinding command(KeyCode code) {
        return create(code, KCondition.KEY_PRESS, KCondition.COMMAND);
    }

    /**
     * Utility method creates a KeyBinding corresponding to a alt-code key press.
     *
     * @param code key code
     * @return KeyBinding
     */
    public static KeyBinding alt(KeyCode code) {
        return create(code, KCondition.KEY_PRESS, KCondition.ALT);
    }

    /**
     * Utility method creates a KeyBinding corresponding to a ctrl-code key press.
     *
     * @param code key code
     * @return KeyBinding
     */
    public static KeyBinding ctrl(KeyCode code) {
        return create(code, KCondition.KEY_PRESS, KCondition.CTRL);
    }

    /**
     * Utility method creates a KeyBinding corresponding to a shift-code key press.
     *
     * @param code key code
     * @return KeyBinding
     */
    public static KeyBinding shift(KeyCode code) {
        return create(code, KCondition.KEY_PRESS, KCondition.SHIFT);
    }

    /**
     * Utility method creates a KeyBinding corresponding to a shortcut-code key press.
     *
     * @param code key code
     * @return KeyBinding
     */
    public static KeyBinding shortcut(KeyCode code) {
        return create(code, KCondition.KEY_PRESS, KCondition.SHORTCUT);
    }

    private static KeyBinding create(Object key, KCondition... mods) {
        return builder().init(key, mods).build();
    }

    /**
     * Determines whether this key binding if for the key press event.
     * @return true if this key binding if for the key press event
     */
    public boolean isKeyPress() {
        return modifiers.contains(KCondition.KEY_PRESS);
    }

    /**
     * Determines whether this key binding if for the key release event.
     * @return true if this key binding if for the key release event
     */
    public boolean isKeyRelease() {
        return modifiers.contains(KCondition.KEY_RELEASE);
    }

    /**
     * Determines whether this key binding if for the key typed event.
     * @return true if this key binding if for the key typed event
     */
    public boolean isKeyTyped() {
        return modifiers.contains(KCondition.KEY_TYPED);
    }

    /**
     * Determines whether {@code shortcut} key is down in this key binding.
     * @return true if {@code shortcut} key is down in this key binding
     */
    public boolean isShortcut() {
        if (Util.isMac()) {
            return modifiers.contains(KCondition.COMMAND);
        }
        return modifiers.contains(KCondition.CTRL);
    }

    /**
     * Determines whether {@code alt} key is down in this key binding.
     * @return true if {@code alt} key is down in this key binding
     */
    public boolean isAlt() {
        return modifiers.contains(KCondition.ALT);
    }

    /**
     * Determines whether {@code control} key is down in this key binding.
     * @return true if {@code control} key is down in this key binding
     */
    public boolean isControl() {
        return modifiers.contains(KCondition.CTRL);
    }

    /**
     * Determines whether {@code control} key is down in this key binding.
     * Applies to macOS platform only.
     * @return true if {@code control} key is down in this key binding
     */
    public boolean isCommand() {
        return modifiers.contains(KCondition.COMMAND);
    }

    /**
     * Determines whether {@code meta} key is down in this key binding.
     * @return true if {@code meta} key is down in this key binding
     */
    public boolean isMeta() {
        return modifiers.contains(KCondition.META);
    }

    /**
     * Determines whether {@code option} key is down in this key binding.
     * Applies to macOS only.
     * @return true if {@code option} key is down in this key binding
     */
    public boolean isOption() {
        return modifiers.contains(KCondition.OPTION);
    }

    /**
     * Determines whether {@code shift} key is down in this key binding.
     * @return true if {@code shift} key is down in this key binding
     */
    public boolean isShift() {
        return modifiers.contains(KCondition.SHIFT);
    }

    /**
     * Returns a {@link KeyCode} or null if the key binding is not for a key code.
     * @return key code
     */
    public KeyCode getKeyCode() {
        if (key instanceof KeyCode c) {
            return c;
        }
        return null;
    }

    /**
     * Creates a {@link Builder}.
     * @return the Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int hashCode() {
        int h = KeyBinding.class.hashCode();
        h = 31 * h + key.hashCode();
        h = 31 * h + modifiers.hashCode();
        return h;
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof KeyBinding k) {
            return
                Objects.equals(key, k.key) &&
                modifiers.equals(k.modifiers);
        }
        return false;
    }

    /**
     * Creates a Builder with a key pressed event.
     * @param c key code
     * @return Builder instance
     */
    public static Builder with(KeyCode c) {
        return builder().with(c);
    }

    /**
     * Creates a Builder with a key released event.
     * @param c key code
     * @return Builder instance
     */
    public static Builder withRelease(KeyCode c) {
        return builder().withRelease(c);
    }

    /**
     * Creates a Builder with a key pressed event.
     * @param c character pressed
     * @return Builder instance
     */
    public static Builder with(String c) {
        return builder().with(c);
    }

    /**
     * Creates a KeyBinding from a KeyEvent.  This call drops multiple key modifiers, performing
     * translation when necessary.  May return null if the event does not correspond to a valid KeyBinding.
     * @param ev key event
     * @return the key binding
     */
    public static KeyBinding from(KeyEvent ev) {
        Object key;
        EnumSet<KCondition> m = EnumSet.noneOf(KCondition.class);
        EventType<KeyEvent> t = ev.getEventType();
        if(t == KeyEvent.KEY_PRESSED) {
            m.add(KCondition.KEY_PRESS);
            key = ev.getCode();
        } else if(t == KeyEvent.KEY_RELEASED) {
            m.add(KCondition.KEY_RELEASE);
            key = ev.getCode();
        } else if(t == KeyEvent.KEY_TYPED) {
            m.add(KCondition.KEY_TYPED);
            key = ev.getCharacter();
        } else {
            return null;
        }

        boolean alt = ev.isAltDown();
        boolean ctrl = ev.isControlDown();
        boolean meta = ev.isMetaDown();
        boolean shortcut = ev.isShortcutDown();
        boolean option = false;
        boolean command = false;

        boolean mac = Util.isMac();
        boolean win = Util.isWindows();

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

        KeyBinding keyBinding = new KeyBinding(key, m);
        //System.err.println("kb=" + keyBinding + " ev=" + toString(ev)); // FIX
        return keyBinding;
    }

    // FIX remove, debug
//    private static String toString(KeyEvent ev) {
//        StringBuilder sb = new StringBuilder("KeyEvent{");
//        sb.append("type=").append(ev.getEventType());
//        sb.append(", char=").append(ev.getCharacter());
//
//        String ch = ev.getCharacter();
//        int sz = ch.length();
//        if (sz > 0) {
//            sb.append("(");
//            for (int i = 0; i < ch.length(); i++) {
//                sb.append(String.format("%02X", (int)ch.charAt(i)));
//            }
//            sb.append(")");
//        }
//
//        sb.append(", code=").append(ev.getCode());
//
//        if (ev.isShiftDown()) {
//            sb.append(", shift");
//        }
//        if (ev.isControlDown()) {
//            sb.append(", control");
//        }
//        if (ev.isAltDown()) {
//            sb.append(", alt");
//        }
//        if (ev.isMetaDown()) {
//            sb.append(", meta");
//        }
//        if (ev.isShortcutDown()) {
//            sb.append(", shortcut");
//        }
//
//        return sb.append("}").toString();
//    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("KeyBinding{key=");
        sb.append(key);
        sb.append(", modifiers=");
        sb.append(modifiers);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Returns the event type for this key binding.
     * @return KeyEvent
     */
    @Override
    public EventType<KeyEvent> getEventType() {
        if (isKeyPress()) {
            return KeyEvent.KEY_PRESSED;
        } else if (isKeyRelease()) {
            return KeyEvent.KEY_RELEASED;
        } else {
            return KeyEvent.KEY_TYPED;
        }
    }

    @Override
    public boolean isEventAcceptable(KeyEvent ev) {
        return KeyBinding.from(ev).equals(this);
    }

    /** Key bindings builder */
    public static class Builder {
        private Object key; // KeyCode or String
        private final EnumSet<KCondition> m = EnumSet.noneOf(KCondition.class);

        /** Constructs a Builder */
        public Builder() {
        }

        /**
         * Creates a Builder with key pressed event.
         * @param c key code
         * @return the Builder instance
         */
        public Builder with(KeyCode c) {
            if (key != null) {
                throw new IllegalArgumentException("only one KeyCode or character can be set");
            }
            key = c;
            return this;
        }

        /**
         * Creates a Builder with key released event.
         * @param c key code
         * @return the Builder instance
         */
        public Builder withRelease(KeyCode c) {
            if (key != null) {
                throw new IllegalArgumentException("only one KeyCode or character can be set");
            }
            key = c;
            m.add(KCondition.KEY_RELEASE);
            return this;
        }

        /**
         * Creates a Builder with a key pressed event.
         * @param c key character
         * @return the Builder instance
         */
        public Builder with(String c) {
            if (key != null) {
                throw new IllegalArgumentException("only one KeyCode or character can be set");
            }
            key = c;
            return this;
        }

        /**
         * Sets {@code alt} key down condition.
         * @return this Builder
         */
        public Builder alt() {
            m.add(KCondition.ALT);
            return this;
        }

        /**
         * Sets {@code alt} key down condition.
         * @param on condition
         * @return this Builder
         */
        public Builder alt(boolean on) {
            if (on) {
                m.add(KCondition.ALT);
            }
            return this;
        }

        /**
         * Sets {@code command} key down condition.
         * @return this Builder
         */
        public Builder command() {
            m.add(KCondition.COMMAND);
            return this;
        }

        /**
         * Sets {@code command} key down condition.
         * @param on condition
         * @return this Builder
         */
        public Builder command(boolean on) {
            if (on) {
                m.add(KCondition.COMMAND);
            }
            return this;
        }

        /**
         * Sets {@code control} key down condition.
         * @return this Builder
         */
        public Builder control() {
            m.add(KCondition.CTRL);
            return this;
        }

        /**
         * Sets {@code control} key down condition.
         * @param on condition
         * @return this Builder
         */
        public Builder control(boolean on) {
            if (on) {
                m.add(KCondition.CTRL);
            }
            return this;
        }

        /**
         * Sets {@code meta} key down condition.
         * @return this Builder
         */
        public Builder meta() {
            m.add(KCondition.META);
            return this;
        }

        /**
         * Sets {@code meta} key down condition.
         * @param on condition
         * @return this Builder
         */
        public Builder meta(boolean on) {
            if (on) {
                m.add(KCondition.META);
            }
            return this;
        }

        /**
         * Sets {@code option} key down condition.
         * @return this Builder
         */
        public Builder option() {
            m.add(KCondition.OPTION);
            return this;
        }

        /**
         * Sets {@code option} key down condition.
         * @param on condition
         * @return this Builder
         */
        public Builder option(boolean on) {
            if (on) {
                m.add(KCondition.OPTION);
            }
            return this;
        }

        /**
         * Sets {@code shift} key down condition.
         * @return this Builder
         */
        public Builder shift() {
            m.add(KCondition.SHIFT);
            return this;
        }

        /**
         * Sets {@code shift} key down condition.
         * @param on condition
         * @return this Builder
         */
        public Builder shift(boolean on) {
            if (on) {
                m.add(KCondition.SHIFT);
            }
            return this;
        }

        /**
         * Sets {@code shortcut} key down condition.
         * @return this Builder
         */
        public Builder shortcut() {
            m.add(KCondition.SHORTCUT);
            return this;
        }

        /**
         * Sets {@code shortcut} key down condition.
         * @param on condition
         * @return this Builder
         */
        public Builder shortcut(boolean on) {
            if (on) {
                m.add(KCondition.SHORTCUT);
            }
            return this;
        }

        private Builder init(Object key, KCondition... mods) {
            this.key = key;
            for (KCondition c : mods) {
                m.add(c);
            }
            return this;
        }

        private void replace(KCondition c, KCondition replaceWith) {
            if (m.contains(c)) {
                m.remove(c);
                m.add(replaceWith);
            }
        }

        /**
         * Creates a new {@link KeyBinding} instance.
         * @return a new key binding instance.
         */
        public KeyBinding build() {
            boolean mac = Util.isMac();
            boolean win = Util.isWindows();
            boolean linux = Util.isLinux();

            if (mac) {
                replace(KCondition.ALT, KCondition.OPTION);
                replace(KCondition.META, KCondition.COMMAND);
                replace(KCondition.SHORTCUT, KCondition.COMMAND);
            } else if (win) {
                replace(KCondition.SHORTCUT, KCondition.CTRL);
            } else if (linux) {
                replace(KCondition.SHORTCUT, KCondition.CTRL);
            }

            if (!mac) {
                if (m.contains(KCondition.COMMAND)) {
                    return null;
                } else if (m.contains(KCondition.OPTION)) {
                    return null;
                }

                replace(KCondition.WINDOWS, KCondition.META);
            }

            boolean pressed = m.contains(KCondition.KEY_PRESS);
            boolean released = m.contains(KCondition.KEY_RELEASE);
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

            // FIX just remove the previous one!
            // validate event type
            if (ct > 1) {
                throw new IllegalArgumentException("more than one key event type is specified");
            }

            if (t == null) {
                t = KCondition.KEY_PRESS;
            }
            m.add(t);

            // TODO validate: shortcut and !(other shortcut modifier)
            return new KeyBinding(key, m);
        }
    }
}
