/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.input;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class USKeyboardTest {

    private UInput ui;

    @Before
    public void initDevice() {
        TestLog.reset();
        ui = new UInput();
    }

    @After
    public void destroyDevice() throws InterruptedException {
        ui.waitForQuiet();
        try {
            ui.processLine("DESTROY");
        } catch (RuntimeException e) { }
        ui.processLine("CLOSE");
        ui.dispose();
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
            TestLog.waitForLog("Key released: SHIFT");
        }
        TestLog.waitForLog("Key typed: %0$c", new Object[] { c });
    }

    @Test
    public void testShift() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addKeyListeners();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("KEYBIT KEY_A");
        ui.processLine("KEYBIT KEY_1");
        ui.processLine("KEYBIT KEY_2");
        ui.processLine("KEYBIT KEY_3");
        ui.processLine("KEYBIT KEY_4");
        ui.processLine("KEYBIT KEY_5");
        ui.processLine("KEYBIT KEY_6");
        ui.processLine("KEYBIT KEY_7");
        ui.processLine("KEYBIT KEY_8");
        ui.processLine("KEYBIT KEY_9");
        ui.processLine("KEYBIT KEY_0");
        ui.processLine("KEYBIT KEY_LEFTSHIFT");
        ui.processLine("PROPERTY ID_INPUT_KEYBOARD 1");
        ui.processLine("CREATE");

        checkShift("KEY_A", 'a', 'A');
        checkShift("KEY_1", '1', '!');
        checkShift("KEY_2", '2', '@');
        checkShift("KEY_3", '3', '#');
        checkShift("KEY_4", '4', '$');
        checkShift("KEY_5", '5', '%');
        checkShift("KEY_6", '6', '^');
        checkShift("KEY_7", '7', '&');
        checkShift("KEY_8", '8', '*');
        checkShift("KEY_9", '9', '(');
        checkShift("KEY_0", '0', ')');
    }

    @Test
    public void testCapsLock() throws Exception {
        TestApplication.showFullScreenScene();
        TestApplication.addKeyListeners();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("KEYBIT KEY_A");
        ui.processLine("KEYBIT KEY_1");
        ui.processLine("KEYBIT KEY_2");
        ui.processLine("KEYBIT KEY_3");
        ui.processLine("KEYBIT KEY_4");
        ui.processLine("KEYBIT KEY_5");
        ui.processLine("KEYBIT KEY_6");
        ui.processLine("KEYBIT KEY_7");
        ui.processLine("KEYBIT KEY_8");
        ui.processLine("KEYBIT KEY_9");
        ui.processLine("KEYBIT KEY_0");
        ui.processLine("KEYBIT KEY_LEFTSHIFT");
        ui.processLine("KEYBIT KEY_CAPSLOCK");
        ui.processLine("PROPERTY ID_INPUT_KEYBOARD 1");
        ui.processLine("CREATE");

        ui.processLine("EV_KEY KEY_CAPSLOCK 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_CAPSLOCK 0");
        ui.processLine("EV_SYN");

        checkShift("KEY_A", 'A', 'a');
        checkShift("KEY_1", '1', '!');
        checkShift("KEY_2", '2', '@');
        checkShift("KEY_3", '3', '#');
        checkShift("KEY_4", '4', '$');
        checkShift("KEY_5", '5', '%');
        checkShift("KEY_6", '6', '^');
        checkShift("KEY_7", '7', '&');
        checkShift("KEY_8", '8', '*');
        checkShift("KEY_9", '9', '(');
        checkShift("KEY_0", '0', ')');

        ui.processLine("EV_KEY KEY_CAPSLOCK 1");
        ui.processLine("EV_SYN");
        ui.processLine("EV_KEY KEY_CAPSLOCK 0");
        ui.processLine("EV_SYN");
        TestLog.waitForLog("Key released: CAPS");
    }

}
