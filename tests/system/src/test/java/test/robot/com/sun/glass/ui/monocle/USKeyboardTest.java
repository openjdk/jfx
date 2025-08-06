/*
 * Copyright (c) 2014, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.com.sun.glass.ui.monocle;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.glass.ui.monocle.TestLogShim;


public class USKeyboardTest {

    private UInput ui;
    private Character [] bigChars;
    private Character [] smallChars;
    private Character [] digits;
    private Character [] digitsShift;
    private Character [] signs;
    private Character [] signsShift;
    private String [] signsKeyNames;

    @BeforeEach
    public void initDevice() {
        TestLogShim.reset();
        ui = new UInput();
    }

    @AfterEach
    public void destroyDevice() throws InterruptedException {
        ui.waitForQuiet();
        try {
            ui.processLine("DESTROY");
        } catch (RuntimeException e) { }
        ui.processLine("CLOSE");
        ui.dispose();
    }

    private void createUSKeyboard() {
        bigChars = new Character[26];
        for(int i = 65; i < 91; i++) {
            bigChars[i-65] = (char) i;
        }
        smallChars = new Character[26];
        for(int i = 97; i < 123; i++) {
            smallChars[i-97] = (char) i;
        }
        digits = new Character [] {'1','2','3','4','5','6','7','8','9','0'};
        digitsShift = new Character [] {'!','@','#','$','%','^','&','*','(',')'};

        signs =  new Character [] {'`','-','=','[',']',';','\'','\\',',','.','/'};
        signsShift = new Character [] {'~','_','+','{','}',':','"','|','<','>','?'};
        signsKeyNames = new String [] {"GRAVE", "MINUS","EQUAL","LEFTBRACE",
                                        "RIGHTBRACE","SEMICOLON","APOSTROPHE",
                                        "BACKSLASH","COMMA","DOT","SLASH"};
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("EVBIT EV_SYN");
        for(int i = 0; i < 26; i++) {
            ui.processLine("KEYBIT KEY_" + bigChars[i]);
        }
        for(int i = 0; i < 10; i++) {
            ui.processLine("KEYBIT KEY_" + digits[i]);
        }
        for(int i = 0; i < 11; i++) {
            ui.processLine("KEYBIT KEY_" + signsKeyNames[i]);
        }
        ui.processLine("KEYBIT 0x0033");
        ui.processLine("KEYBIT KEY_LEFTSHIFT");
        ui.processLine("KEYBIT KEY_CAPSLOCK");
        ui.processLine("PROPERTY ID_INPUT_KEYBOARD 1");
        ui.processLine("CREATE");
    }

    private void checkShift(String key, char unShifted, char shifted) throws Exception {
        checkKey(key, unShifted, false);
        checkKey(key, shifted, true);
    }

    private void checkKey(String key, char c, boolean shiftPressed) throws Exception {
        if (shiftPressed) {
            ui.processLine("EV_KEY KEY_LEFTSHIFT 1");
            ui.processLine("EV_SYN");
        }
        ui.processLine("EV_KEY " + key + " 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY " + key + " 0");
        ui.processLine("EV_SYN");
        if (shiftPressed) {
            ui.processLine("EV_KEY KEY_LEFTSHIFT 0");
            ui.processLine("EV_SYN");
            TestLogShim.waitForLog("Key released: SHIFT");
        }
        TestLogShim.waitForLog("Key typed: %1$c", new Object[] { c });
    }

    /**
     * The test is checking the lookup of each key on board,
     * at first with Shift button unpressed and then with Shift button pressed.
     */
    @Test
    public void testShift() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addKeyListeners();
        createUSKeyboard();

        for(int i = 0; i < 26; i++) {
            checkShift("KEY_"+ bigChars[i], smallChars[i], bigChars[i]);
        }
        for(int i = 0; i < 10; i++) {
            checkShift("KEY_"+ digits[i], digits[i], digitsShift[i]);
        }
        for(int i = 0; i < 11; i++) {
            checkShift("KEY_"+ signsKeyNames[i], signs[i], signsShift[i]);
        }
    }

    /**
     * The test is checking the lookup of each key on board,
     * when Caps Lock only is pressed and then with both Shift
     * and Caps Lock buttons are pressed.
     */
    @Test
    public void testCapsLock() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addKeyListeners();
        createUSKeyboard();

        ui.processLine("EV_KEY KEY_CAPSLOCK 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_CAPSLOCK 0");
        ui.processLine("EV_SYN");

        for(int i = 0; i < 26; i++) {
            checkShift("KEY_"+ bigChars[i], bigChars[i], smallChars[i]);
        }
        for(int i = 0; i < 10; i++) {
            checkShift("KEY_"+ digits[i], digits[i], digitsShift[i]);
        }
        for(int i = 0; i < 11; i++) {
            checkShift("KEY_"+ signsKeyNames[i], signs[i], signsShift[i]);
        }

        ui.processLine("EV_KEY KEY_CAPSLOCK 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_CAPSLOCK 0");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key released: CAPS");
    }

    /** Key presses and releases are allowed to overlap. JDK-8090306. */
    @Test
    public void testPressReleaseOrder() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addKeyListeners();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("KEYBIT KEY_1");
        ui.processLine("KEYBIT KEY_2");
        ui.processLine("KEYBIT KEY_3");
        ui.processLine("KEYBIT KEY_4");
        ui.processLine("KEYBIT KEY_CAPSLOCK");
        ui.processLine("PROPERTY ID_INPUT_KEYBOARD 1");
        ui.processLine("CREATE");

        ui.processLine("EV_KEY KEY_1 1");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key pressed: DIGIT1");
        TestLogShim.waitForLog("Key typed: 1");
        ui.processLine("EV_KEY KEY_2 1");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key pressed: DIGIT2");
        TestLogShim.waitForLog("Key typed: 2");
        ui.processLine("EV_KEY KEY_1 0");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key released: DIGIT1");
        ui.processLine("EV_KEY KEY_3 1");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key pressed: DIGIT3");
        TestLogShim.waitForLog("Key typed: 3");
        ui.processLine("EV_KEY KEY_2 0");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key released: DIGIT2");
        ui.processLine("EV_KEY KEY_4 1");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key pressed: DIGIT4");
        TestLogShim.waitForLog("Key typed: 4");
        ui.processLine("EV_KEY KEY_3 0");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key released: DIGIT3");
        ui.processLine("EV_KEY KEY_4 0");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key released: DIGIT3");
    }

    @Test
    public void testBackspace() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addKeyListeners();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("KEYBIT KEY_BACKSPACE");
        ui.processLine("KEYBIT KEY_LEFTSHIFT");
        ui.processLine("KEYBIT KEY_CAPSLOCK");
        ui.processLine("PROPERTY ID_INPUT_KEYBOARD 1");
        ui.processLine("CREATE");

        ui.processLine("EV_KEY KEY_BACKSPACE 1");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key pressed: BACK_SPACE");
        ui.processLine("EV_KEY KEY_BACKSPACE 0");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key released: BACK_SPACE");
        Assertions.assertEquals(0l, TestLogShim.countLogContaining("Key typed"));

        TestLogShim.reset();
        ui.processLine("EV_KEY KEY_LEFTSHIFT 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_BACKSPACE 1");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key pressed: BACK_SPACE");
        ui.processLine("EV_KEY KEY_BACKSPACE 0");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_LEFTSHIFT 0");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key released: BACK_SPACE");
        Assertions.assertEquals(0l, TestLogShim.countLogContaining("Key typed"));

        TestLogShim.reset();
        ui.processLine("EV_KEY KEY_CAPSLOCK 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_CAPSLOCK 0");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_BACKSPACE 1");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key pressed: BACK_SPACE");
        ui.processLine("EV_KEY KEY_BACKSPACE 0");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_CAPSLOCK 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_CAPSLOCK 0");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key released: BACK_SPACE");
        Assertions.assertEquals(0l, TestLogShim.countLogContaining("Key typed"));

        TestLogShim.reset();
        ui.processLine("EV_KEY KEY_CAPSLOCK 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_CAPSLOCK 0");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_LEFTSHIFT 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_BACKSPACE 1");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key pressed: BACK_SPACE");
        ui.processLine("EV_KEY KEY_BACKSPACE 0");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_LEFTSHIFT 0");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_CAPSLOCK 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_CAPSLOCK 0");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Key released: BACK_SPACE");
        Assertions.assertEquals(0l, TestLogShim.countLogContaining("Key typed"));
    }
}
