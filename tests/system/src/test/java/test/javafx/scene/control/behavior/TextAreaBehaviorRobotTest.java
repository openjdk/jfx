/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.behavior;

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.END;
import static javafx.scene.input.KeyCode.HOME;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.UP;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

/**
 * Tests TextArea behavior with Robot using public APIs.
 */
public class TextAreaBehaviorRobotTest extends TextInputBehaviorRobotTest<TextArea> {

    public TextAreaBehaviorRobotTest() {
        super(new TextArea());
    }

    @Test
    public void testNavigation() throws Exception {
        execute(
//            exe(() -> {
//               control.addEventFilter(KeyEvent.ANY, (ev) -> {
//                   System.err.println(ev);
//               });
//            }),
            "0123456789", checkSelection(10),
            LEFT, LEFT, checkSelection(8),
            RIGHT, checkSelection(9),
            HOME, checkSelection(0),
            END, checkSelection(10),
            // home
            shortcut(HOME), checkSelection(0),
            // end
            shortcut(END), checkSelection(10)
        );
    }
}
