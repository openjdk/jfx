/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import com.sun.javafx.tk.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// PENDING_DOC_REVIEW
/**
 * Represents a combination of keys which are used in keyboard shortcuts.
 * A key combination consists of a main key and a set of modifier keys. The main
 * key can be specified by its key code - {@code KeyCodeCombination} or key
 * character - {@code KeyCharacterCombination}. A modifier key is {@code shift},
 * {@code control}, {@code alt}, {@code meta} or {@code shortcut} and can be
 * defined as {@code DOWN}, {@code UP} or {@code ANY}.
 * <p>
 * The {@code shortcut} modifier is used to represent the modifier key which is
 * used commonly in keyboard shortcuts on the host platform. This is for
 * example {@code control} on Windows and {@code meta} (command key) on Mac.
 * By using {@code shortcut} key modifier developers can create platform
 * independent shortcuts. So the "Shortcut+C" key combination is handled
 * internally as "Ctrl+C" on Windows and "Meta+C" on Mac.
 * @since JavaFX 2.0
 */
public abstract class KeyCombination {

    /** Modifier which specifies that the {@code shift} key must be down. */
    public static final Modifier SHIFT_DOWN =
            new Modifier(KeyCode.SHIFT, ModifierValue.DOWN);
    /**
     * Modifier which specifies that the {@code shift} key can be either up or
     * down.
     */
    public static final Modifier SHIFT_ANY =
            new Modifier(KeyCode.SHIFT, ModifierValue.ANY);
    /** Modifier which specifies that the {@code control} key must be down. */
    public static final Modifier CONTROL_DOWN =
            new Modifier(KeyCode.CONTROL, ModifierValue.DOWN);
    /**
     * Modifier which specifies that the {@code control} key can be either up or
     * down.
     */
    public static final Modifier CONTROL_ANY =
            new Modifier(KeyCode.CONTROL, ModifierValue.ANY);
    /** Modifier which specifies that the {@code alt} key must be down. */
    public static final Modifier ALT_DOWN =
            new Modifier(KeyCode.ALT, ModifierValue.DOWN);
    /**
     * Modifier which specifies that the {@code alt} key can be either up or
     * down.
     */
    public static final Modifier ALT_ANY =
            new Modifier(KeyCode.ALT, ModifierValue.ANY);
    /** Modifier which specifies that the {@code meta} key must be down. */
    public static final Modifier META_DOWN =
            new Modifier(KeyCode.META, ModifierValue.DOWN);
    /**
     * Modifier which specifies that the {@code meta} key can be either up or
     * down.
     */
    public static final Modifier META_ANY =
            new Modifier(KeyCode.META, ModifierValue.ANY);
    /** Modifier which specifies that the {@code shortcut} key must be down. */
    public static final Modifier SHORTCUT_DOWN =
            new Modifier(KeyCode.SHORTCUT, ModifierValue.DOWN);
    /**
     * Modifier which specifies that the {@code shortcut} key can be either up
     * or down.
     */
    public static final Modifier SHORTCUT_ANY =
            new Modifier(KeyCode.SHORTCUT, ModifierValue.ANY);

    private static final Modifier[] POSSIBLE_MODIFIERS = {
        SHIFT_DOWN, SHIFT_ANY,
        CONTROL_DOWN, CONTROL_ANY,
        ALT_DOWN, ALT_ANY,
        META_DOWN, META_ANY,
        SHORTCUT_DOWN, SHORTCUT_ANY
    };

    /**
     * A KeyCombination that will match with no events.
     */
    public static final KeyCombination NO_MATCH = new KeyCombination() {
        @Override
        public boolean match(KeyEvent e) {
            return false;
        }
    };

    /** The state of the {@code shift} key in this key combination. */
    private final ModifierValue shift;

    /**
     * The state of the {@code shift} key in this key combination.
     * @return The state of the {@code shift} key in this key combination
     */
    public final ModifierValue getShift() {
        return shift;
    }
    /** The state of the {@code control} key in this key combination. */
    private final ModifierValue control;

    /**
     * The state of the {@code control} key in this key combination.
     * @return The state of the {@code control} key in this key combination
     */
    public final ModifierValue getControl() {
        return control;
    }
    /** The state of the {@code alt} key in this key combination. */
    private final ModifierValue alt;

    /**
     * The state of the {@code alt} key in this key combination.
     * @return The state of the {@code alt} key in this key combination.
     */
    public final ModifierValue getAlt() {
        return alt;
    }
    /** The state of the {@code meta} key in this key combination. */
    private final ModifierValue meta;

    /**
     * The state of the {@code meta} key in this key combination.
     * @return The state of the {@code meta} key in this key combination
     */
    public final ModifierValue getMeta() {
        return meta;
    }

    /** The state of the {@code shortcut} key in this key combination. */
    private final ModifierValue shortcut;

    /**
     * The state of the {@code shortcut} key in this key combination.
     * @return The state of the {@code shortcut} key in this key combination
     */
    public final ModifierValue getShortcut() {
        return shortcut;
    }

    /**
     * Constructs a {@code KeyCombination} with an explicit specification
     * of all modifier keys. Each modifier key can be set to {@code DOWN},
     * {@code UP} or {@code ANY}.
     *
     * @param shift the value of the {@code shift} modifier key
     * @param control the value of the {@code control} modifier key
     * @param alt the value of the {@code alt} modifier key
     * @param meta the value of the {@code meta} modifier key
     * @param shortcut the value of the {@code shortcut} modifier key
     */
    protected KeyCombination(final ModifierValue shift,
                             final ModifierValue control,
                             final ModifierValue alt,
                             final ModifierValue meta,
                             final ModifierValue shortcut) {
        if ((shift == null)
                || (control == null)
                || (alt == null)
                || (meta == null)
                || (shortcut == null)) {
            throw new NullPointerException("Modifier value must not be null!");
        }

        this.shift = shift;
        this.control = control;
        this.alt = alt;
        this.meta = meta;
        this.shortcut = shortcut;
    }

    /**
     * Constructs a {@code KeyCombination} with the specified list of modifiers.
     * All modifier keys which are not explicitly listed are set to the
     * default {@code UP} value.
     * <p>
     * All possible modifiers which change the default modifier value are
     * defined as constants in the {@code KeyCombination} class.
     *
     * @param modifiers the list of modifier keys and their corresponding values
     */
    protected KeyCombination(final Modifier... modifiers) {
        this(getModifierValue(modifiers, KeyCode.SHIFT),
             getModifierValue(modifiers, KeyCode.CONTROL),
             getModifierValue(modifiers, KeyCode.ALT),
             getModifierValue(modifiers, KeyCode.META),
             getModifierValue(modifiers, KeyCode.SHORTCUT));
    }

    /**
     * Tests whether this key combination matches the combination in the given
     * {@code KeyEvent}.
     * <p>
     * The implementation of this method in the {@code KeyCombination} class
     * does only a partial test with the modifier keys. This method is
     * overridden in subclasses to include the main key in the test.
     *
     * @param event the key event
     * @return {@code true} if the key combinations match, {@code false}
     *      otherwise
     */
    public boolean match(final KeyEvent event) {
        final KeyCode shortcutKey =
                Toolkit.getToolkit().getPlatformShortcutKey();
        return test(KeyCode.SHIFT, shift, shortcutKey, shortcut,
                    event.isShiftDown())
                && test(KeyCode.CONTROL, control, shortcutKey, shortcut,
                        event.isControlDown())
                && test(KeyCode.ALT, alt, shortcutKey, shortcut,
                        event.isAltDown())
                && test(KeyCode.META, meta, shortcutKey, shortcut,
                        event.isMetaDown());
    }

    /**
     * Returns a string representation of this {@code KeyCombination}.
     * <p>
     * The string representation consists of sections separated by plus
     * characters. Each section specifies either a modifier key or the main key.
     * <p>
     * A modifier key section contains the {@code KeyCode} name of a modifier
     * key. It can be prefixed with the {@code Ignored} keyword. A non-prefixed
     * modifier key implies its {@code DOWN} value while the prefixed version
     * implies the {@code ANY} (ignored) value. If some modifier key is not
     * specified in the string at all, it means it has the default {@code UP}
     * value.
     * <p>
     * The format of the main key section of the key combination string depends
     * on the {@code KeyCombination} subclass. It is either the key code name
     * for {@code KeyCodeCombination} or the single quoted key character for
     * {@code KeyCharacterCombination}.
     * <p>
     * Examples of {@code KeyCombination} string representations:

<PRE>
"Ctrl+Alt+Q"
"Ignore Shift+Ctrl+A"
"Alt+'w'"
</PRE>

     * @return the string representation of this {@code KeyCombination}
     */
    public String getName() {
        StringBuilder sb = new StringBuilder();
        addModifiersIntoString(sb);

        return sb.toString();
    }

    /**
     * Returns a string representation of this {@code KeyCombination} that is
     * suitable for display in a user interface (for example, beside a menu item).
     *
     * @return A string representation of this {@code KeyCombination}, suitable
     *      for display in a user interface.
     * @since JavaFX 8u20
     */
    public String getDisplayText() {
        StringBuilder stringBuilder = new StringBuilder();
        if (com.sun.javafx.PlatformUtil.isMac()) {
            // Macs have a different convention for keyboard accelerators -
            // no pluses to separate modifiers, and special symbols for
            // each modifier (in a particular order), etc
            if (getControl() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("\u2303");
            }
            if (getAlt() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("\u2325");
            }
            if (getShift() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("\u21e7");
            }
            if (getMeta() == KeyCombination.ModifierValue.DOWN || getShortcut() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("\u2318");
            }
            // TODO refer to RT-14486 for remaining glyphs
        }
        else {
            if (getControl() == KeyCombination.ModifierValue.DOWN || getShortcut() == KeyCombination.ModifierValue.DOWN ) {
                stringBuilder.append("Ctrl+");
            }
            if (getAlt() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("Alt+");
            }
            if (getShift() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("Shift+");
            }
            if (getMeta() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("Meta+");
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Tests whether this {@code KeyCombination} equals to the specified object.
     *
     * @param obj the object to compare to
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof KeyCombination)) {
            return false;
        }

        final KeyCombination other = (KeyCombination) obj;
        return (shift == other.shift)
                && (control == other.control)
                && (alt == other.alt)
                && (meta == other.meta)
                && (shortcut == other.shortcut);
    }

    /**
     * Returns a hash code value for this {@code KeyCombination}.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int hash = 7;

        hash = 23 * hash + shift.hashCode();
        hash = 23 * hash + control.hashCode();
        hash = 23 * hash + alt.hashCode();
        hash = 23 * hash + meta.hashCode();
        hash = 23 * hash + shortcut.hashCode();

        return hash;
    }

    /**
     * Returns a string representation of this object. Implementation returns
     * the result of the {@code getName()} call.
     *
     * @return the string representation of this {@code KeyCombination}
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Constructs a new {@code KeyCombination} from the specified string. The
     * string should be in the same format as produced by the {@code getName}
     * method.
     * <p>
     * If the main key section string is quoted in single quotes the method
     * creates a new {@code KeyCharacterCombination} for the unquoted substring.
     * Otherwise it finds the key code which name corresponds to the main key
     * section string and creates a {@code KeyCodeCombination} for it. If this
     * can't be done, it falls back to the {@code KeyCharacterCombination}.
     *
     * @param value the string which represents the requested key combination
     * @return the constructed {@code KeyCombination}
     * @since JavaFX 2.1
     */
    public static KeyCombination valueOf(String value) {
        final List<Modifier> modifiers = new ArrayList<Modifier>(4);

        final String[] tokens = splitName(value);

        KeyCode keyCode = null;
        String keyCharacter = null;
        for (String token : tokens) {

            if ((token.length() > 2)
                    && (token.charAt(0) == '\'')
                    && (token.charAt(token.length() - 1) == '\'')) {
                if ((keyCode != null) || (keyCharacter != null)) {
                    throw new IllegalArgumentException(
                            "Cannot parse key binding " + value);
                }

                keyCharacter = token.substring(1, token.length() - 1)
                        .replace("\\'", "'");
                continue;
            }

            final String normalizedToken = normalizeToken(token);

            final Modifier modifier = getModifier(normalizedToken);
            if (modifier != null) {
                modifiers.add(modifier);
                continue;
            }

            if ((keyCode != null) || (keyCharacter != null)) {
                throw new IllegalArgumentException(
                        "Cannot parse key binding " + value);
            }

            keyCode = KeyCode.getKeyCode(normalizedToken);
            if (keyCode == null) {
                keyCharacter = token;
            }
        }

        if ((keyCode == null) && (keyCharacter == null)) {
            throw new IllegalArgumentException(
                    "Cannot parse key binding " + value);
        }

        final Modifier[] modifierArray =
                modifiers.toArray(new Modifier[modifiers.size()]);
        return (keyCode != null)
                    ? new KeyCodeCombination(keyCode, modifierArray)
                    : new KeyCharacterCombination(keyCharacter, modifierArray);
    }

    /**
     * Constructs a new {@code KeyCombination} from the specified string. This
     * method simply delegates to {@link #valueOf(String)}.
     *
     * @param name the string which represents the requested key combination
     * @return the constructed {@code KeyCombination}
     *
     * @see #valueOf(String)
     */
    public static KeyCombination keyCombination(String name) {
        return valueOf(name);
    }

    /**
     * This class represents a pair of modifier key and its value.
     * @since JavaFX 2.0
     */
    public static final class Modifier {
        private final KeyCode key;
        private final ModifierValue value;

        private Modifier(final KeyCode key,
                         final ModifierValue value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Gets the modifier key of this {@code Modifier}.
         *
         * @return the modifier key
         */
        public KeyCode getKey() {
            return key;
        }

        /**
         * Gets the modifier value of this {@code Modifier}.
         *
         * @return the modifier value
         */
        public ModifierValue getValue() {
            return value;
        }

        /**
         * Returns a string representation of the modifier.
         * @return a string representation of the modifier
         */
        @Override
        public String toString() {
            return ((value == ModifierValue.ANY) ? "Ignore " : "")
                       + key.getName();

        }
    }

    /**
     * {@code ModifierValue} specifies state of modifier keys.
     * @since JavaFX 2.0
     */
    public static enum ModifierValue {
        /** Constant which indicates that the modifier key must be down. */
        DOWN,
        /** Constant which indicates that the modifier key must be up. */
        UP,
        /**
         * Constant which indicates that the modifier key can be either up or
         * down.
         */
        ANY
    }

    private void addModifiersIntoString(final StringBuilder sb) {
        addModifierIntoString(sb, KeyCode.SHIFT, shift);
        addModifierIntoString(sb, KeyCode.CONTROL, control);
        addModifierIntoString(sb, KeyCode.ALT, alt);
        addModifierIntoString(sb, KeyCode.META, meta);
        addModifierIntoString(sb, KeyCode.SHORTCUT, shortcut);
    }

    private static void addModifierIntoString(
            final StringBuilder sb,
            final KeyCode modifierKey,
            final ModifierValue modifierValue) {

        if (modifierValue == ModifierValue.UP) {
            return;
        }

        if (sb.length() > 0) {
            sb.append("+");
        }

        if (modifierValue == ModifierValue.ANY) {
            sb.append("Ignore ");
        }

        sb.append(modifierKey.getName());
    }

    private static boolean test(final KeyCode testedModifierKey,
                                final ModifierValue testedModifierValue,
                                final KeyCode shortcutModifierKey,
                                final ModifierValue shortcutModifierValue,
                                final boolean isKeyDown) {
        final ModifierValue finalModifierValue =
                (testedModifierKey == shortcutModifierKey)
                        ? resolveModifierValue(testedModifierValue,
                                               shortcutModifierValue)
                        : testedModifierValue;

        return test(finalModifierValue, isKeyDown);

    }

    private static boolean test(final ModifierValue modifierValue,
                                final boolean isDown) {
        switch (modifierValue) {
            case DOWN:
                return isDown;

            case UP:
                return !isDown;

            case ANY:
            default:
                return true;
        }
    }

    private static ModifierValue resolveModifierValue(
            final ModifierValue firstValue,
            final ModifierValue secondValue) {
        if ((firstValue == ModifierValue.DOWN)
                || (secondValue == ModifierValue.DOWN)) {
            return ModifierValue.DOWN;
        }

        if ((firstValue == ModifierValue.ANY)
                || (secondValue == ModifierValue.ANY)) {
            return ModifierValue.ANY;
        }

        return ModifierValue.UP;
    }

    static Modifier getModifier(final String name) {
        for (final Modifier modifier: POSSIBLE_MODIFIERS) {
            if (modifier.toString().equals(name)) {
                return modifier;
            }
        }

        return null;
    }

    private static ModifierValue getModifierValue(
            final Modifier[] modifiers,
            final KeyCode modifierKey) {
        ModifierValue modifierValue = ModifierValue.UP;
        for (final Modifier modifier: modifiers) {
            if (modifier == null) {
                throw new NullPointerException("Modifier must not be null!");
            }

            if (modifier.getKey() == modifierKey) {
                if (modifierValue != ModifierValue.UP) {
                    throw new IllegalArgumentException(
                            (modifier.getValue() != modifierValue)
                                    ? "Conflicting modifiers specified!"
                                    : "Duplicate modifiers specified!");
                }

                modifierValue = modifier.getValue();
            }
        }

        return modifierValue;
    }

    private static String normalizeToken(final String token) {
        final String[] words = token.split("\\s+");
        final StringBuilder sb = new StringBuilder();

        for (final String word: words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }

            sb.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            sb.append(word.substring(1).toLowerCase(Locale.ROOT));
        }

        return sb.toString();
    }

    private static String[] splitName(String name) {
        List<String> tokens = new ArrayList<String>();
        char[] chars = name.trim().toCharArray();

        final int STATE_BASIC = 0;      // general text
        final int STATE_WHITESPACE = 1; // spaces found
        final int STATE_SEPARATOR = 2;  // plus found
        final int STATE_QUOTED = 3;     // quoted text

        int state = STATE_BASIC;
        int tokenStart = 0;
        int tokenEnd = -1;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch(state) {
                case STATE_BASIC:
                    switch(c) {
                        case ' ':
                        case '\t':
                        case '\n':
                        case '\f':
                        case '\r':
                        case '\u000B':
                            tokenEnd = i;
                            state = STATE_WHITESPACE;
                            break;
                        case '+':
                            tokenEnd = i;
                            state = STATE_SEPARATOR;
                            break;
                        case '\'':
                            if (i == 0 || chars[i - 1] != '\\') {
                                state = STATE_QUOTED;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case STATE_WHITESPACE:
                    switch(c) {
                        case ' ':
                        case '\t':
                        case '\n':
                        case '\f':
                        case '\r':
                        case '\u000B':
                            break;
                        case '+':
                            state = STATE_SEPARATOR;
                            break;
                        case '\'':
                            state = STATE_QUOTED;
                            tokenEnd = -1;
                            break;
                        default:
                            state = STATE_BASIC;
                            tokenEnd = -1;
                            break;
                    }
                    break;
                case STATE_SEPARATOR:
                    switch(c) {
                        case ' ':
                        case '\t':
                        case '\n':
                        case '\f':
                        case '\r':
                        case '\u000B':
                            break;
                        case '+':
                            throw new IllegalArgumentException(
                                    "Cannot parse key binding " + name);
                        default:
                            if (tokenEnd <= tokenStart) {
                                throw new IllegalArgumentException(
                                        "Cannot parse key binding " + name);
                            }
                            tokens.add(new String(chars,
                                    tokenStart, tokenEnd - tokenStart));
                            tokenStart = i;
                            tokenEnd = -1;
                            state = (c == '\'' ? STATE_QUOTED : STATE_BASIC);
                            break;
                    }
                    break;
                case STATE_QUOTED:
                    if (c == '\'' && chars[i - 1] != '\\') {
                        state = STATE_BASIC;
                    }
                    break;
            }
        }

        switch(state) {
            case STATE_BASIC:
            case STATE_WHITESPACE:
                tokens.add(new String(chars,
                        tokenStart, chars.length - tokenStart));
                break;
            case STATE_SEPARATOR:
            case STATE_QUOTED:
                throw new IllegalArgumentException(
                        "Cannot parse key binding " + name);
        }

        return tokens.toArray(new String[tokens.size()]);
    }
}
