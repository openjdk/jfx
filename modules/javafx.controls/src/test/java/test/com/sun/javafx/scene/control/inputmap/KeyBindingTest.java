/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.inputmap;

import com.sun.javafx.scene.control.inputmap.KeyBinding;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeyBindingTest {

    @Test public void getSpecificity() {
        final KeyCode code = KeyCode.ENTER;

        // Expected answer:
        // 1 pt for matching key code
        // 1 pt for matching key event type
        // 1 pt for matching no alt
        // 1 pt for matching no meta
        // 1 pt for matching shift or control
        // 0 pt for the other optional value of control/shift
        //
        // Total = 5.
        //
        int expect = 5;

        KeyBinding uut = new KeyBinding(code).shift().ctrl(KeyBinding.OptionalBoolean.ANY);

        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, null,
                null, code, true, false, false, false);

        assertEquals(expect, uut.getSpecificity(event)); // Gets 6 (fx 2.2, fx 8)

        uut = new KeyBinding(code).shift(KeyBinding.OptionalBoolean.ANY).ctrl();

        event = new KeyEvent(KeyEvent.KEY_PRESSED, null,
                null, code, false, true, false, false);

        assertEquals(expect, uut.getSpecificity(event)); // Gets 2 (fx 2.2, fx 8)
    }
}
