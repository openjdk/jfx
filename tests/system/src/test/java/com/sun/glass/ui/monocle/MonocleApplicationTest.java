package com.sun.glass.ui.monocle;/*
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

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.Application;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class MonocleApplicationTest {

    /** Tests a US keyboard */
    private static int[][] TEST_CASES = {
            { 'a', KeyEvent.VK_A },
            { 'b', KeyEvent.VK_B },
            { 'c', KeyEvent.VK_C },
            { 'd', KeyEvent.VK_D },
            { 'e', KeyEvent.VK_E },
            { 'f', KeyEvent.VK_F },
            { 'g', KeyEvent.VK_G },
            { 'h', KeyEvent.VK_H },
            { 'i', KeyEvent.VK_I },
            { 'j', KeyEvent.VK_J },
            { 'k', KeyEvent.VK_K },
            { 'l', KeyEvent.VK_L },
            { 'm', KeyEvent.VK_M },
            { 'n', KeyEvent.VK_N },
            { 'o', KeyEvent.VK_O },
            { 'p', KeyEvent.VK_P },
            { 'q', KeyEvent.VK_Q },
            { 'r', KeyEvent.VK_R },
            { 's', KeyEvent.VK_S },
            { 't', KeyEvent.VK_T },
            { 'u', KeyEvent.VK_U },
            { 'v', KeyEvent.VK_V },
            { 'w', KeyEvent.VK_W },
            { 'x', KeyEvent.VK_X },
            { 'y', KeyEvent.VK_Y },
            { 'z', KeyEvent.VK_Z },
            { 'A', KeyEvent.VK_A },
            { 'B', KeyEvent.VK_B },
            { 'C', KeyEvent.VK_C },
            { 'D', KeyEvent.VK_D },
            { 'E', KeyEvent.VK_E },
            { 'F', KeyEvent.VK_F },
            { 'G', KeyEvent.VK_G },
            { 'H', KeyEvent.VK_H },
            { 'I', KeyEvent.VK_I },
            { 'J', KeyEvent.VK_J },
            { 'K', KeyEvent.VK_K },
            { 'L', KeyEvent.VK_L },
            { 'M', KeyEvent.VK_M },
            { 'N', KeyEvent.VK_N },
            { 'O', KeyEvent.VK_O },
            { 'P', KeyEvent.VK_P },
            { 'Q', KeyEvent.VK_Q },
            { 'R', KeyEvent.VK_R },
            { 'S', KeyEvent.VK_S },
            { 'T', KeyEvent.VK_T },
            { 'U', KeyEvent.VK_U },
            { 'V', KeyEvent.VK_V },
            { 'W', KeyEvent.VK_W },
            { 'X', KeyEvent.VK_X },
            { 'Y', KeyEvent.VK_Y },
            { 'Z', KeyEvent.VK_Z },
            { '`', KeyEvent.VK_BACK_QUOTE },
            { '~', KeyEvent.VK_BACK_QUOTE },
            { '1', KeyEvent.VK_1 },
            { '!', KeyEvent.VK_1 },
            { '@', KeyEvent.VK_2 },
            { '2', KeyEvent.VK_2 },
            { '#', KeyEvent.VK_3 },
            { '3', KeyEvent.VK_3 },
            { '$', KeyEvent.VK_4 },
            { '4', KeyEvent.VK_4 },
            { '%', KeyEvent.VK_5 },
            { '5', KeyEvent.VK_5 },
            { '^', KeyEvent.VK_6 },
            { '6', KeyEvent.VK_6 },
            { '&', KeyEvent.VK_7 },
            { '7', KeyEvent.VK_7 },
            { '*', KeyEvent.VK_8 },
            { '8', KeyEvent.VK_8 },
            { '(', KeyEvent.VK_9 },
            { '9', KeyEvent.VK_9 },
            { ')', KeyEvent.VK_0 },
            { '0', KeyEvent.VK_0 },
            { '_', KeyEvent.VK_MINUS },
            { '-', KeyEvent.VK_MINUS },
            { '+', KeyEvent.VK_EQUALS },
            { '=', KeyEvent.VK_EQUALS },
            { '{', KeyEvent.VK_BRACELEFT },
            { '[', KeyEvent.VK_BRACELEFT },
            { '}', KeyEvent.VK_BRACERIGHT },
            { ']', KeyEvent.VK_BRACERIGHT },
            { '|', KeyEvent.VK_BACK_SLASH },
            { '\\', KeyEvent.VK_BACK_SLASH },
            { ':', KeyEvent.VK_SEMICOLON },
            { ';', KeyEvent.VK_SEMICOLON },
            { '\"', KeyEvent.VK_QUOTE },
            { '\'', KeyEvent.VK_QUOTE },
            { '<', KeyEvent.VK_COMMA },
            { ',', KeyEvent.VK_COMMA },
            { '>', KeyEvent.VK_PERIOD },
            { '.', KeyEvent.VK_PERIOD },
            { '?', KeyEvent.VK_SLASH },
            { '/', KeyEvent.VK_SLASH },
    };

    @Test
    public void testCharToKeyCode() throws Exception {
        TestApplication.getStage();
        Assume.assumeTrue(TestApplication.isMonocle());
        TestRunnable.invokeAndWait(() -> {
            MonocleApplication app = (MonocleApplication)
                    Application.GetApplication();
            for (int i = 0; i < TEST_CASES.length; i++) {
                char ch = (char) TEST_CASES[i][0];
                int expectedCode = TEST_CASES[i][1];
                int code = app._getKeyCodeForChar(ch);
                Assert.assertEquals("Code for character "
                                    + ((int) ch) + " ('" + ch + "')",
                                    expectedCode, code);
            }
        });
    }
}
