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

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.scene.Scene;
import javafx.stage.Stage;

/*
 * This application tests key event handling in JavaFX. Each test uses a Robot
 * to send platform key events and then verifies that the correct JavaFX
 * KeyEvents are generated.
 *
 * To provide thorough coverage a test has to be targeted at a specific layout.
 * Currently there are tests for U.S. (QWERTY), French (AZERTY), German (QWERTZ)
 * and Spanish (QWERTY) on Mac, Windows, and Linux. Since there's no way for
 * JavaFX to force the layout or verify which layout is currently active it is
 * up to the tester to configure the correct layout before running the test.
 *
 * Each language-specific test must be run against the default layout for that
 * language. For example, the German test is designed to work with the layout
 * labeled "German" on a Mac, not "German - Standard" or any other variant.
 *
 * There is also a generic test for Latin layouts which verifies that KeyCodes A
 * through Z are reachable and generate the letters 'a' through 'z'. An even
 * more generic test is available for non-Latin, non-IME layouts which verifies
 * that KeyCodes A through Z generate characters.
 *
 * None of these tests cover the top-row function keys or the Caps Lock key.
 * They also do not cover dead keys or keys which generate accented characters
 * (the latter don't have KeyCodes so the Robot cannot access them).
 *
 * These tests always check that the given KeyCode generates the expected
 * character (if any). They can optionally check that KeyCharacterCombinations
 * match for characters on that key. This option is disabled by default since
 * KeyCharacterCombinations don't work reliably on most platforms (for now).
 *
 * Mac users will need to grant permission for the Terminal application to use
 * accessibility features. Add Terminal to the list of applications in
 * System Settings > Privacy & Security > Accessibility.
 */

public class KeyboardTest extends Application {

    public static void main(String[] args) {
        Application.launch(KeyboardTest.class, args);
    }

    private static final String os = System.getProperty("os.name");
    private static final boolean onMac = os.startsWith("Mac");
    private static final boolean onLinux = os.startsWith("Linux");
    private static final boolean onWindows = os.startsWith("Windows");

    /**
     * Data for testing one key including the code and expected character.
     */
    static private class KeyData {
        /*
         * If character is null it means we don't expect a TYPED event. If
         * character is "wild" it means we'll accept anything (used for testing
         * non-Latin layouts).
         */
        public final KeyCode   code;
        public final String    character;

        /*
         * Optional characters on this key accessed using modifiers like Shift,
         * Option, or AltGr.
         */
        public String          comboChar;
        public String          comboChar2;

        /*
         * We also test a handful of KeyCodes which should not generate any
         * events, like UNDEFINED. For these we set the absent flag.
         */
        public boolean         absent;

        public KeyData(KeyCode cd, String ch, String combo1, String combo2) {
            code = cd;
            character = ch;
            comboChar = combo1;
            comboChar2 = combo2;
            absent = false;
        }
    }

    /**
     * List of keys to test for one layout
     */
    @SuppressWarnings("serial")
    static private class KeyList extends ArrayList<KeyData> {}

    static private class KeyListBuilder {

        private final KeyList list = new KeyList();
        public final KeyList getList() {
            return list;
        }

        private static final String DOUBLE_QUOTE = "\"";
        private static final String QUOTE        = "\'";
        private static final String BACK_SLASH   = "\\";
        private static final String A_GRAVE      = "\u00E0";
        private static final String E_GRAVE      = "\u00E8";
        private static final String E_ACUTE      = "\u00E9";
        private static final String SECTION      = "\u00A7";
        private static final String C_CEDILLA    = "\u00E7";
        private static final String DEGREE_SIGN  = "\u00B0";
        private static final String POUND_SIGN   = "\u00A3";
        private static final String MIDDLE_DOT   = "\u00B7";
        private static final String INV_EXCLAMATION_MARK = "\u00A1";
        private static final String INV_QUESTION_MARK    = "\u00BF";

        /* Add a key with unshifted and shifted characters */
        private void add(KeyCode cd, String base, String shifted) {
            list.add(new KeyData(cd, base, shifted, null));
        }

        /* Add a key with an unshifted character */
        private void add(KeyCode cd, String base) {
            list.add(new KeyData(cd, base, null, null));
        }

        /* Add a key that does not generate a TYPED event */
        private void add(KeyCode cd) {
            list.add(new KeyData(cd, null, null, null));
        }

        /* Add a key with unshifted, shifted, and AltGr characters */
        private void add(KeyCode cd, String base, String shifted, String altGr) {
            list.add(new KeyData(cd, base, shifted, altGr));
        }

        /* Add a key that should not generate any events */
        public void addAbsent(KeyCode cd) {
            KeyData missing = new KeyData(cd, null, null, null);
            missing.absent = true;
            list.add(missing);
        }

        /*
         * Add keys A through Z assuming they generate 'a' through 'z' unshifted
         * and 'A' to 'Z' shifted
         */
        private void addLetters() {
            for (Character c = 'A'; c <= 'Z'; ++c) {
                String s = String.valueOf(c);
                KeyCode code = KeyCode.valueOf(s);
                add(code, s.toLowerCase(Locale.ENGLISH), s);
            }
        }

        private void addDigits() {
            add(KeyCode.DIGIT0, "0");
            add(KeyCode.DIGIT1, "1");
            add(KeyCode.DIGIT2, "2");
            add(KeyCode.DIGIT3, "3");
            add(KeyCode.DIGIT4, "4");
            add(KeyCode.DIGIT5, "5");
            add(KeyCode.DIGIT6, "6");
            add(KeyCode.DIGIT7, "7");
            add(KeyCode.DIGIT8, "8");
            add(KeyCode.DIGIT9, "9");
        }

        private void addKeypad() {
            add(KeyCode.NUMPAD0,  "0");
            add(KeyCode.NUMPAD1,  "1");
            add(KeyCode.NUMPAD2,  "2");
            add(KeyCode.NUMPAD3,  "3");
            add(KeyCode.NUMPAD4,  "4");
            add(KeyCode.NUMPAD5,  "5");
            add(KeyCode.NUMPAD6,  "6");
            add(KeyCode.NUMPAD7,  "7");
            add(KeyCode.NUMPAD8,  "8");
            add(KeyCode.NUMPAD9,  "9");
            add(KeyCode.ADD,      "+");
            add(KeyCode.SUBTRACT, "-");
            add(KeyCode.MULTIPLY, "*");
            add(KeyCode.DIVIDE,   "/");
            if (onMac) {
                add(KeyCode.CLEAR, "");
            }
            /*
             * We do not add DECIMAL since the character it generates varies by
             * platform and language. It will be added later.
             */
        }

        private void addNavigation() {
            add(KeyCode.HOME);
            add(KeyCode.END);
            add(KeyCode.PAGE_UP);
            add(KeyCode.PAGE_DOWN);
            add(KeyCode.UP);
            add(KeyCode.DOWN);
            add(KeyCode.LEFT);
            add(KeyCode.RIGHT);
        }

        private void addMiscellaneous() {
            add(KeyCode.SHIFT);
            add(KeyCode.ALT);
            add(KeyCode.CONTROL);

            add(KeyCode.SPACE, " ");
            add(KeyCode.TAB,   "\t");

            /*
             * ENTER is assigned to both Return and Enter which generate
             * different characters. Platforms should always target Return.
             */
            add(KeyCode.ENTER, "\r");

            if (onMac) {
                add(KeyCode.COMMAND);
                add(KeyCode.BACK_SPACE, "");
                add(KeyCode.DELETE,     "");
                add(KeyCode.ESCAPE,     "");
            } else {
                add(KeyCode.BACK_SPACE, "\u0008");
                add(KeyCode.DELETE,     "\u007F");
                add(KeyCode.ESCAPE,     "\u001B");
                add(KeyCode.INSERT);

                // Sent twice to toggle off and back on
                add(KeyCode.NUM_LOCK);
                add(KeyCode.NUM_LOCK);
            }

            /*
             * We do not test CAPS. Every platform has special case code for
             * CAPS that generates multiple PRESSED and RELEASED events in
             * succession.
             */

            /*
             * Clearly this KeyCode should not generate any events.
             */
            addAbsent(KeyCode.UNDEFINED);
        }

        /*
         * Add all of the keys common to all layouts
         */
        private void addCommon() {
            addKeypad();
            addNavigation();
            addMiscellaneous();
        }

        /*
         * The U.S. English QWERTY layout. Same on all platforms.
         */
        public static KeyList usEnglishKeys() {
            KeyListBuilder builder = new KeyListBuilder();
            builder.addCommon();
            builder.addLetters();

            builder.add(KeyCode.DIGIT0, "0", ")");
            builder.add(KeyCode.DIGIT1, "1", "!");
            builder.add(KeyCode.DIGIT2, "2", "@");
            builder.add(KeyCode.DIGIT3, "3", "#");
            builder.add(KeyCode.DIGIT4, "4", "$");
            builder.add(KeyCode.DIGIT5, "5", "%");
            builder.add(KeyCode.DIGIT6, "6", "^");
            builder.add(KeyCode.DIGIT7, "7", "&");
            builder.add(KeyCode.DIGIT8, "8", "*");
            builder.add(KeyCode.DIGIT9, "9", "(");

            builder.add(KeyCode.BACK_QUOTE,    "`",  "~");
            builder.add(KeyCode.MINUS,         "-",  "_");
            builder.add(KeyCode.EQUALS,        "=",  "+");
            builder.add(KeyCode.OPEN_BRACKET,  "[",  "{");
            builder.add(KeyCode.CLOSE_BRACKET, "]",  "}");
            builder.add(KeyCode.BACK_SLASH,    BACK_SLASH, "|");
            builder.add(KeyCode.SEMICOLON,     ";",  ":");
            builder.add(KeyCode.QUOTE,         QUOTE, DOUBLE_QUOTE);
            builder.add(KeyCode.COMMA,         ",",  "<");
            builder.add(KeyCode.PERIOD,        ".",  ">");
            builder.add(KeyCode.SLASH,         "/",  "?");

            builder.add(KeyCode.DECIMAL,       ".");

            builder.addAbsent(KeyCode.PLUS);

            return builder.getList();
        }

        /* The French AZERTY layout */
        public static KeyList frenchKeys() {
            KeyListBuilder builder = new KeyListBuilder();
            builder.addCommon();
            builder.addLetters();

            /* Include one combination that involves AltGr. */
            final String altGrFive = "[";

            /*
             * On a French layout the unshifted top-row keys (which generate
             * digits in most other languages) generate punctuation or accented
             * characters. Linux uses these characters to generate KeyCodes; Mac
             * and Windows still encode these keys as digits.
             */
            if (onLinux) {
                builder.add(KeyCode.AMPERSAND,        "&",          "1");
                builder.add(KeyCode.QUOTEDBL,         DOUBLE_QUOTE, "3");
                builder.add(KeyCode.QUOTE,            QUOTE,        "4");
                builder.add(KeyCode.LEFT_PARENTHESIS, "(",          "5", altGrFive);
                builder.add(KeyCode.MINUS,            "-",          "6");
                builder.add(KeyCode.UNDERSCORE,       "_",          "8");
            } else {
                builder.add(KeyCode.DIGIT0, A_GRAVE,      "0");
                builder.add(KeyCode.DIGIT1, "&",          "1");
                builder.add(KeyCode.DIGIT2, E_ACUTE,      "2");
                builder.add(KeyCode.DIGIT3, DOUBLE_QUOTE, "3");
                builder.add(KeyCode.DIGIT4, QUOTE,        "4");
                /* Five, six, and eight require some tweaking, below */
                builder.add(KeyCode.DIGIT7, E_GRAVE,      "7");
                builder.add(KeyCode.DIGIT9, C_CEDILLA,    "9");

                if (onMac) {
                    builder.add(KeyCode.DIGIT5, "(",      "5");
                    builder.add(KeyCode.DIGIT6, SECTION,  "6");
                    builder.add(KeyCode.DIGIT8, "!",      "8");
                } else {
                    builder.add(KeyCode.DIGIT5, "(",      "5", altGrFive);
                    builder.add(KeyCode.DIGIT6, "-",      "6");
                    builder.add(KeyCode.DIGIT8, "_",      "8");
                }
            }

            builder.add(KeyCode.LESS,              "<", ">");
            builder.add(KeyCode.RIGHT_PARENTHESIS, ")", DEGREE_SIGN);
            builder.add(KeyCode.COMMA,             ",", "?");
            builder.add(KeyCode.SEMICOLON,         ";", ".");
            builder.add(KeyCode.COLON,             ":", "/");
            builder.add(KeyCode.EQUALS,            "=", "+");

            if (onMac) {
                builder.add(KeyCode.DOLLAR,        "$", "*");
                builder.add(KeyCode.MINUS,         "-", "_");
                builder.add(KeyCode.DECIMAL,       ",");
            } else {
                builder.add(KeyCode.DOLLAR,           "$", POUND_SIGN);
                builder.add(KeyCode.EXCLAMATION_MARK, "!", SECTION);
                builder.add(KeyCode.DECIMAL,          ".");
            }

            builder.addAbsent(KeyCode.PLUS);

            return builder.getList();
        }

        /* The German QWERTZ layout */
        public static KeyList germanKeys() {
            KeyListBuilder builder = new KeyListBuilder();
            builder.addCommon();
            builder.addLetters();

            /* Include one combination that involves Option/AltGr */
            final String altGrSeven = (onMac ? "|" : "{");
            final String decimalCharacter = (onLinux ? "." : ",");

            builder.add(KeyCode.DIGIT0, "0", "=");
            builder.add(KeyCode.DIGIT1, "1", "!");
            builder.add(KeyCode.DIGIT2, "2", DOUBLE_QUOTE);
            builder.add(KeyCode.DIGIT3, "3", SECTION);
            builder.add(KeyCode.DIGIT4, "4", "$");
            builder.add(KeyCode.DIGIT5, "5", "%");
            builder.add(KeyCode.DIGIT6, "6", "&");

            if (onMac) {
                builder.add(KeyCode.DIGIT7, "7", "/");
            }
            else {
                builder.add(KeyCode.DIGIT7, "7", "/", "{");
            }

            builder.add(KeyCode.DIGIT8, "8", "(");
            builder.add(KeyCode.DIGIT9, "9", ")");

            builder.add(KeyCode.LESS,        "<", ">");
            builder.add(KeyCode.PLUS,        "+", "*");
            builder.add(KeyCode.NUMBER_SIGN, "#", QUOTE);
            builder.add(KeyCode.COMMA,       ",", ";");
            builder.add(KeyCode.PERIOD,      ".", ":");
            builder.add(KeyCode.MINUS,       "-", "_");

            builder.add(KeyCode.DECIMAL,     decimalCharacter);

            builder.addAbsent(KeyCode.COLON);

            return builder.getList();
        }

        /* Spanish QWERTY */
        public static KeyList spanishKeys() {
            KeyListBuilder builder = new KeyListBuilder();
            builder.addCommon();
            builder.addLetters();

            final String decimalCharacter = (onMac ? "," : ".");

            builder.add(KeyCode.DIGIT0, "0", "=");
            builder.add(KeyCode.DIGIT1, "1", "!");
            builder.add(KeyCode.DIGIT2, "2", DOUBLE_QUOTE);
            builder.add(KeyCode.DIGIT3, "3", MIDDLE_DOT);
            builder.add(KeyCode.DIGIT4, "4", "$");
            builder.add(KeyCode.DIGIT5, "5", "%");
            builder.add(KeyCode.DIGIT6, "6", "&");
            builder.add(KeyCode.DIGIT7, "7", "/");
            builder.add(KeyCode.DIGIT8, "8", "(");
            builder.add(KeyCode.DIGIT9, "9", ")");

            builder.add(KeyCode.QUOTE,        QUOTE, "?");
            builder.add(KeyCode.INVERTED_EXCLAMATION_MARK, INV_EXCLAMATION_MARK, INV_QUESTION_MARK);
            builder.add(KeyCode.PLUS,         "+", "*");
            builder.add(KeyCode.LESS,         "<", ">");
            builder.add(KeyCode.COMMA,        ",", ";");
            builder.add(KeyCode.PERIOD,       ".", ":");
            builder.add(KeyCode.MINUS,        "-", "_");

            builder.add(KeyCode.DECIMAL,      decimalCharacter);

            builder.addAbsent(KeyCode.EQUALS);

            return builder.getList();
        }

        /*
         * A generic Latin layout. No digits since layouts derived from French
         * won't generate digit characters and may not even be encoded as digits
         * on Linux.
         */
        public static KeyList latinKeys() {
            KeyListBuilder builder = new KeyListBuilder();
            builder.addCommon();
            builder.addLetters();
            return builder.getList();
        }

        /*
         * For non-Latin layouts that do not use an IME (Greek, Cyrillic) we
         * should be able to access the letter KeyCodes though we have no idea
         * what characters they generate.
         */
        public static KeyList nonLatinKeys() {
            KeyListBuilder builder = new KeyListBuilder();
            builder.addCommon();
            builder.addDigits();
            for (Character c = 'A'; c <= 'Z'; ++c) {
                String s = String.valueOf(c);
                KeyCode code = KeyCode.valueOf(s);
                builder.add(code, "wild");
            }
            return builder.getList();
        }
    }

    private enum Layout {
        US_ENGLISH("U.S. English", KeyListBuilder.usEnglishKeys()),
        FRENCH("French", KeyListBuilder.frenchKeys()),
        GERMAN("German", KeyListBuilder.germanKeys()),
        SPANISH("Spanish", KeyListBuilder.spanishKeys()),
        LATIN("Latin", KeyListBuilder.latinKeys()),
        NON_LATIN("non-Latin", KeyListBuilder.nonLatinKeys());

        private final String label;
        private final KeyList keys;

        private Layout(String l, KeyList k) {
            this.label = l;
            this.keys = k;
        }

        @Override
        public String toString() {
            return label;
        }

        public KeyList getKeys() {
            return keys;
        }
    }

    /*
     * KeyCharacterCombinations should really work on the numeric keypad but
     * currently don't on Windows and Linux. The tests can exclude combinations
     * entirely, exclude just the numeric keypad, or cover both the main
     * keyboard and the keypad.
     */
    private enum CombinationScope {
        NONE("without combinations"),
        NO_KEYPAD("without keypad combinations"),
        ALL("with all combinations");

        private final String label;

        private CombinationScope(String l) {
            this.label = l;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private interface Logging {
        public void clear();
        public void addLine(String s);
    }

    /*
     * The class that walks through the key list sending Robot events and
     * verifies the expected KeyEvents come back.
     */
    private class TestRunner {

        /*
         * Configured during initialization
         */
        private final Layout layout;
        private final CombinationScope combinationScope;
        private final Node focusNode;
        private final Logging log;
        private final KeyList keys;

        /*
         * Our progress
         */
        private int currentIndex = -1;
        private int numSent = 0;
        private int numFailed = 0;

        /* The character in the last TYPED event */
        private String characterReceived = null;

        private final Robot robot = new Robot();
        private Timer timer = null;

        private final EventHandler<KeyEvent> pressedHandler = this::pressedEvent;
        private final EventHandler<KeyEvent> releasedHandler = this::releasedEvent;
        private final EventHandler<KeyEvent> typedHandler = this::typedEvent;

        private Runnable runAtEnd = null;

        public TestRunner(Layout layout, CombinationScope scope,
                          Node focusNode, Logging log) {
            this.layout = layout;
            this.combinationScope = scope;
            this.focusNode = focusNode;
            this.log = log;
            this.keys = layout.getKeys();
        }

        private String toPrintable(String s) {
            if (s == null) {
                return "null";
            } else if (!s.isEmpty()) {
                char c = s.charAt(0);
                int codePoint = s.codePointAt(0);
                if (Character.isISOControl(c) || Character.isWhitespace(c)) {
                    return String.format("U+%04X", codePoint);
                } else {
                    return s;
                }
            }
            return "empty";
        }

        private void fail(String s) {
            numFailed += 1;
            log.addLine("Failed: " + s);
        }

        private void start(Runnable atEnd) {
            runAtEnd = atEnd;

            log.clear();

            Optional<Boolean> capsLockOn = Platform.isKeyLocked(KeyCode.CAPS);
            Optional<Boolean> numLockOn = Platform.isKeyLocked(KeyCode.NUM_LOCK);
            boolean proceed = true;
            if (capsLockOn.isPresent() && capsLockOn.get() == Boolean.TRUE) {
                log.addLine("Disable Caps Lock before running test.");
                proceed = false;
            }
            if (numLockOn.isPresent() && numLockOn.get() == Boolean.FALSE) {
                log.addLine("Enable Num Lock before running test.");
                proceed = false;
            }
            if (!proceed) {
                if (runAtEnd != null) {
                    runAtEnd.run();
                }
                return;
            }

            String osName = "unknown";
            if (onWindows) {
                osName = "Win";
            } else if (onMac) {
                osName = "Mac";
            } else if (onLinux) {
                osName = "Linux";
            }

            log.addLine("[" + osName + "] Testing " + keys.size() + " keys on "
                    + layout + " " + combinationScope);

            focusNode.addEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
            focusNode.addEventFilter(KeyEvent.KEY_RELEASED, releasedHandler);
            focusNode.addEventFilter(KeyEvent.KEY_TYPED, typedHandler);
            focusNode.requestFocus();

            currentIndex = -1;
            advance();
        }

        private void advance() {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            currentIndex += 1;
            if (currentIndex >= keys.size()) {
                cleanup();
            } else {
                characterReceived = null;
                numSent += 1;
                KeyData data = keys.get(currentIndex);
                Platform.runLater(() -> sendCode(data.code));
            }
        }

        private void sendCode(KeyCode code) {
            /*
            * This timer is cleared when the RELEASED event calls advance().
            */
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> keyTimedOut());
                }
            };
            timer = new Timer();
            timer.schedule(task, 100);
            robot.keyPress(code);
            robot.keyRelease(code);
        }

        /*
        * No RELEASED event arrived. Acceptable if the key was supposed to be
        * absent.
        */
        private void keyTimedOut() {
            KeyData key = keys.get(currentIndex);
            if (!key.absent) {
                fail("code " + key.code.getName() + " did not produce any events");
            }
            advance();
        }

        private static boolean isOnKeypad(KeyCode code) {
            switch (code) {
                case DIVIDE, MULTIPLY, SUBTRACT, ADD, DECIMAL:
                case NUMPAD0, NUMPAD1, NUMPAD2, NUMPAD3, NUMPAD4:
                case NUMPAD5, NUMPAD6, NUMPAD7, NUMPAD8, NUMPAD9:
                    return true;
            }
            return false;
        }

        private void checkCombination(KeyEvent event, String comboString) {
            if (combinationScope == CombinationScope.NONE) {
                return;
            }
            if (comboString == null || comboString.isEmpty() || comboString.equals("wild")) {
                return;
            }
            if (isOnKeypad(event.getCode()) && (combinationScope != CombinationScope.ALL)) {
                return;
            }

            KeyCharacterCombination combo = new KeyCharacterCombination(comboString);
            if (!combo.match(event)) {
                fail("code " + event.getCode().getName() + " did not match combination "
                    + toPrintable(combo.getCharacter()));
            }
        }

        private void pressedEvent(KeyEvent e) {
            KeyData key = keys.get(currentIndex);
            KeyCode got = e.getCode();
            KeyCode expected = key.code;
            String preamble = "code " + key.code.getName() + " ";

            if (key.absent) {
                fail(preamble + "produced an unexpected PRESSED event");
            } else if (expected != got) {
                fail(preamble + "was sent but code " + got.getName() + " was received");
            } else {
                checkCombination(e, key.character);
                checkCombination(e, key.comboChar);
                checkCombination(e, key.comboChar2);
            }
            e.consume();
        }

        private void typedEvent(KeyEvent e) {
            KeyData key = keys.get(currentIndex);
            String preamble = "code " + key.code.getName() + " ";
            characterReceived = e.getCharacter();

            if (key.character == null) {
                String printable = toPrintable(characterReceived);
                fail(preamble + "produced an unexpected TYPED event (" + printable + ")");
            } else if (key.character.equals("wild")) {
                if (characterReceived == null || characterReceived.isEmpty()) {
                    fail(preamble + "produced a TYPED event with no character");
                }
            } else if (!key.character.equals(characterReceived)) {
                fail(preamble + "generated " + toPrintable(characterReceived)
                    + " instead of " + toPrintable(key.character));
            }
            e.consume();
        }

        private void releasedEvent(KeyEvent e) {
            KeyData key = keys.get(currentIndex);
            String preamble = "code " + key.code.getName() + " ";

            if (key.absent) {
                fail(preamble + "produced an unexpected RELEASED event");
            } else if ((key.character != null) && (characterReceived == null)) {
                fail(preamble + "did not produce a TYPED event");
            }
            e.consume();
            advance();
        }

        /*
         * Called after the list of keys is exhausted.
         */
        private void cleanup() {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }

            focusNode.removeEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
            focusNode.removeEventFilter(KeyEvent.KEY_RELEASED, releasedHandler);
            focusNode.removeEventFilter(KeyEvent.KEY_TYPED, typedHandler);

            log.addLine("Tested " + numSent + " keys with "
                    + (numFailed == 1 ? "1 failure" : numFailed + " failures"));

            if (runAtEnd != null) {
                runAtEnd.run();
            }
        }
    }

    private class TextLogging implements Logging {
        private final TextArea textArea;
        public TextLogging(TextArea ta) {
            textArea = ta;
        }

        @Override
        public void clear() {
            textArea.setText("");
        }

        @Override
        public void addLine(String s) {
            textArea.appendText(s + "\n");
        }
    }

    @Override
    public void start(Stage stage) {

        final TextArea logArea = new TextArea();
        logArea.setEditable(false);
        Logging logger = new TextLogging(logArea);

        ChoiceBox<Layout> layoutChoice = new ChoiceBox<>();
        layoutChoice.getItems().setAll(Layout.values());
        layoutChoice.setValue(Layout.US_ENGLISH);

        ChoiceBox<CombinationScope> combinationChoice = new ChoiceBox<>();
        combinationChoice.getItems().setAll(CombinationScope.values());
        combinationChoice.setValue(CombinationScope.NONE);

        Button testButton = new Button("Run test");
        testButton.setOnAction(b -> {
            testButton.setDisable(true);
            Layout layout = layoutChoice.getValue();
            CombinationScope comboScope = combinationChoice.getValue();
            TestRunner testRunner = new TestRunner(layout, comboScope, logArea, logger);
            testRunner.start(() -> {
                testButton.setDisable(false);
                testButton.requestFocus();
            });
        });

        HBox testControls = new HBox();
        testControls.setSpacing(5);
        testControls.getChildren().addAll(testButton, layoutChoice, combinationChoice);

        VBox root = new VBox();
        root.setPadding(new Insets(5));
        root.setSpacing(5);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        root.getChildren().addAll(testControls, logArea);

        Scene scene = new Scene(root, 640, 640);
        stage.setScene(scene);
        stage.setTitle("Keyboard Test");
        stage.show();

        Platform.runLater(testButton::requestFocus);
    }
}
