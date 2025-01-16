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

package jfx.incubator.scene.control.input;

import java.util.EnumSet;
import java.util.Objects;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import com.sun.javafx.PlatformUtil;

/**
 * This immutable class represents a combination of keys which are used in key mappings.
 * A key combination consists of a main key and a set of modifier keys.
 * The main key can be specified by its {@link KeyCode key code}
 * or key character, the latter must match values returned by {@link KeyEvent#getCharacter()}.
 * A modifier key is {@code shift}, {@code control}, {@code alt}, {@code meta} or {@code shortcut}.
 * <p>
 * This class also provides a set of convenience methods for refering to keys found on macOS platform.
 *
 * @since 24
 */
public class KeyBinding
//implements EventCriteria<KeyEvent>
{
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
        /** a key pressed event */
        KEY_PRESSED,
        /** a key released event */
        KEY_RELEASED,
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
     * Creates a {@code KeyBinding} which corresponds to the key press with the specified {@code KeyCode}.
     *
     * @param code the key code
     * @return the KeyBinding
     */
    public static KeyBinding of(KeyCode code) {
        return create(code, KCondition.KEY_PRESSED);
    }

    /**
     * This utility method creates a {@code KeyBinding} which corresponds to the key press
     * with the specified {@code KeyCode} and the macOS {@code ⌘ command} key modifier.
     * <p>
     * This method returns {@code null} on non-macOS platforms.
     *
     * @param code the key code
     * @return the KeyBinding, or null
     */
    public static KeyBinding command(KeyCode code) {
        return create(code, KCondition.KEY_PRESSED, KCondition.COMMAND);
    }

    /**
     * Creates a KeyBinding which corresponds to the key press with the specified {@code KeyCode}
     * and the {@code alt} key modifier ({@code option} on macOS).
     * <p>
     * This method is equivalent to {@link #option(KeyCode)} on macOS.
     *
     * @param code the key code
     * @return the KeyBinding
     */
    public static KeyBinding alt(KeyCode code) {
        return create(code, KCondition.KEY_PRESSED, KCondition.ALT);
    }

    /**
     * Creates a KeyBinding which corresponds to the key press with the specified {@code KeyCode}
     * and the {@code ctrl} key modifier ({@code control} on macOS).
     *
     * @param code the key code
     * @return the KeyBinding
     */
    public static KeyBinding control(KeyCode code) {
        return create(code, KCondition.KEY_PRESSED, KCondition.CTRL);
    }

    /**
     * Creates a KeyBinding which corresponds to the key press with the specified {@code KeyCode}
     * and the {@code shift} + {@code ctrl} key modifier ({@code control} on macOS).
     *
     * @param code the key code
     * @return the KeyBinding
     */
    public static KeyBinding controlShift(KeyCode code) {
        return create(code, KCondition.KEY_PRESSED, KCondition.CTRL, KCondition.SHIFT);
    }

    /**
     * Creates a KeyBinding which corresponds to the key press with the specified {@code KeyCode}
     * and the {@code option} key modifier on macOS.
     * <p>
     * This method returns {@code null} on non-macOS platforms.
     * On macOS, it is equivalent to calling {@link #alt(KeyCode)}.
     *
     * @param code the key code
     * @return the KeyBinding, or null
     */
    public static KeyBinding option(KeyCode code) {
        return create(code, KCondition.KEY_PRESSED, KCondition.OPTION);
    }

    /**
     * Creates a KeyBinding which corresponds to the key press with the specified {@code KeyCode}
     * and the {@code shift} key modifier.
     *
     * @param code the key code
     * @return the KeyBinding
     */
    public static KeyBinding shift(KeyCode code) {
        return create(code, KCondition.KEY_PRESSED, KCondition.SHIFT);
    }

    /**
     * Creates a KeyBinding which corresponds to the key press with the specified {@code KeyCode}
     * and the shortcut key modifier ({@code ⌘ command} on macOS, {@code ctrl} elsewhere).
     *
     * @param code the key code
     * @return the KeyBinding
     */
    public static KeyBinding shortcut(KeyCode code) {
        return create(code, KCondition.KEY_PRESSED, KCondition.SHORTCUT);
    }

    /**
     * Creates a KeyBinding which corresponds to the key press with the specified {@code KeyCode}
     * and the {@code shift} + {@code option} key modifiers on macOS.
     * <p>
     * This method returns {@code null} on non-macOS platforms.
     *
     * @param code the key code
     * @return the KeyBinding, or null
     */
    public static KeyBinding shiftOption(KeyCode code) {
        return create(code, KCondition.KEY_PRESSED, KCondition.SHIFT, KCondition.OPTION);
    }

    /**
     * Creates a KeyBinding which corresponds to the key press with the specified {@code KeyCode}
     * and the {@code shift} + shortcut key modifier ({@code ⌘ command} on macOS, {@code ctrl} elsewhere).
     *
     * @param code the key code
     * @return the KeyBinding, or null
     */
    public static KeyBinding shiftShortcut(KeyCode code) {
        return create(code, KCondition.KEY_PRESSED, KCondition.SHIFT, KCondition.SHORTCUT);
    }

    /**
     * Creates a new instance of {@code KeyBinding} with the new {@code KeyCode}
     * and the same set of the modifiers.
     *
     * @param newCode the key code
     * @return the KeyBinding
     */
    public KeyBinding withNewKeyCode(KeyCode newCode) {
        return new KeyBinding(newCode, modifiers);
    }

    private static KeyBinding create(Object key, KCondition... mods) {
        return new Builder(key).init(mods).build();
    }

    /**
     * Determines whether this key binding if for the key pressed event.
     * @return true if this key binding if for the key press event
     */
    public boolean isKeyPressed() {
        return modifiers.contains(KCondition.KEY_PRESSED);
    }

    /**
     * Determines whether this key binding if for the key released event.
     * @return true if this key binding if for the key release event
     */
    public boolean isKeyReleased() {
        return modifiers.contains(KCondition.KEY_RELEASED);
    }

    /**
     * Determines whether this key binding if for the key typed event.
     * @return true if this key binding if for the key typed event
     */
    public boolean isKeyTyped() {
        return modifiers.contains(KCondition.KEY_TYPED);
    }

    /**
     * Returns the {@link KeyCode}, or null if the key binding is not for a key code.
     *
     * @return key code, or null
     */
    public KeyCode getKeyCode() {
        if (key instanceof KeyCode c) {
            return c;
        }
        return null;
    }

    /**
     * Creates a {@link Builder} with the specified {@code KeyCode}.
     * @param code the key code
     * @return the Builder instance
     */
    public static Builder builder(KeyCode code) {
        return new Builder(code);
    }

    /**
     * Creates a {@link Builder} with the specified character.  The string must correspond to the
     * value returned by {@link KeyEvent#getCharacter()}.
     * @param character the character
     * @return the Builder instance
     */
    public static Builder builder(String character) {
        return new Builder(character);
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
     * Creates a KeyBinding from a KeyEvent, or a null if the event does not correspond to a valid KeyBinding.
     * @param ev the key event
     * @return the key binding, or null
     */
    static KeyBinding from(KeyEvent ev) {
        Object key;
        EnumSet<KCondition> m = EnumSet.noneOf(KCondition.class);
        EventType<KeyEvent> t = ev.getEventType();
        if(t == KeyEvent.KEY_PRESSED) {
            m.add(KCondition.KEY_PRESSED);
            key = ev.getCode();
        } else if(t == KeyEvent.KEY_RELEASED) {
            m.add(KCondition.KEY_RELEASED);
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

    @Override
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
//    @Override
//    public EventType<KeyEvent> getEventType() {
//        if (isKeyPressed()) {
//            return KeyEvent.KEY_PRESSED;
//        } else if (isKeyReleased()) {
//            return KeyEvent.KEY_RELEASED;
//        } else {
//            return KeyEvent.KEY_TYPED;
//        }
//    }
//
//    @Override
//    public boolean isEventAcceptable(KeyEvent ev) {
//        return equals(KeyBinding.from(ev));
//    }

    /**
     * A builder for {@code KeyBinding} objects.
     * <p>
     * By default, its {@code build} method creates a key {@code KEY_PRESSED} binding.  This can be changed
     * by calling either {@link #keyReleased()} or {@link #keyTyped()}.
     * <p>
     * The builder pattern can be used when convenience methods such as
     * {@link KeyBinding#control(KeyCode)} or
     * {@link KeyBinding#shiftShortcut(KeyCode)}
     * are not sufficient.
     * <p>
     * Example:
     * {@code
     * KeyBinding.builder(KeyCode.TAB).control().option().shift().build()
     * }
     */
    public static class Builder {
        private final Object key; // KeyCode or String
        private final EnumSet<KCondition> m = EnumSet.noneOf(KCondition.class);

        /** Constructs a Builder */
        Builder(Object key) {
            this.key = key;
        }

        /**
         * Sets the KEY_RELEASED condition, clearing KEY_PRESSED and KEY_TYPED.
         * @return the Builder instance
         */
        public Builder keyReleased() {
            m.remove(KCondition.KEY_PRESSED);
            m.remove(KCondition.KEY_TYPED);
            m.add(KCondition.KEY_RELEASED);
            return this;
        }

        /**
         * Sets the KEY_TYPED condition, clearing KEY_PRESSED and KEY_RELEASED.
         * @return the Builder instance
         */
        public Builder keyTyped() {
            m.remove(KCondition.KEY_PRESSED);
            m.add(KCondition.KEY_TYPED);
            m.remove(KCondition.KEY_RELEASED);
            return this;
        }

        /**
         * Sets the {@code alt} key down condition (the {@code option} key on macOS).
         * @return this Builder
         */
        public Builder alt() {
            m.add(KCondition.ALT);
            return this;
        }

        /**
         * Sets the {@code command} key down condition on macOS.
         * <p>
         * Setting this condition on non-macOS platforms will result in the
         * {@code build} method returning {@code null}.
         *
         * @return this Builder
         */
        public Builder command() {
            m.add(KCondition.COMMAND);
            return this;
        }

        /**
         * Sets the {@code control} key down condition.
         * @return this Builder
         */
        public Builder control() {
            m.add(KCondition.CTRL);
            return this;
        }

        /**
         * Sets the {@code meta} key down condition.
         * @return this Builder
         */
        public Builder meta() {
            m.add(KCondition.META);
            return this;
        }

        /**
         * Sets the {@code option} key down condition on macOS.
         * <p>
         * Setting this condition on non-macOS platforms will result in the
         * {@code build} method returning {@code null}.
         *
         * @return this Builder
         */
        public Builder option() {
            m.add(KCondition.OPTION);
            return this;
        }

        /**
         * Sets the {@code shift} key down condition.
         * @return this Builder
         */
        public Builder shift() {
            m.add(KCondition.SHIFT);
            return this;
        }

        /**
         * Sets the {@code shortcut} key down condition.
         * @return this Builder
         */
        public Builder shortcut() {
            m.add(KCondition.SHORTCUT);
            return this;
        }

        private Builder init(KCondition... mods) {
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
         * Creates a new {@link KeyBinding} instance from the current settings.
         *
         * @return a new key binding instance.
         */
        public KeyBinding build() {
            boolean mac = PlatformUtil.isMac();
            boolean win = PlatformUtil.isWindows();
            boolean linux = PlatformUtil.isLinux();

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
                if (m.contains(KCondition.COMMAND) || m.contains(KCondition.OPTION)) {
                    return null;
                }

                replace(KCondition.WINDOWS, KCondition.META);
            }

            boolean pressed = m.contains(KCondition.KEY_PRESSED);
            boolean released = m.contains(KCondition.KEY_RELEASED);
            boolean typed = m.contains(KCondition.KEY_TYPED);

            int ct = 0;
            KCondition t = null;
            if (pressed) {
                ct++;
                t = KCondition.KEY_PRESSED;
            }
            if (released) {
                ct++;
                t = KCondition.KEY_RELEASED;
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
                t = KCondition.KEY_PRESSED;
            }
            m.add(t);

            // TODO validate: shortcut and !(other shortcut modifier)
            return new KeyBinding(key, m);
        }
    }
}
