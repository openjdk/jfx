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

package test.robot.javafx.scene.control.behavior;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.TextArea;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.text.Font;
import javafx.stage.Window;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import test.util.Util;

/**
 * Tests TextArea layout functionality.
 */
public class TextAreaLayoutRobotTest extends TextInputBehaviorRobotTest<TextArea> {
    private static final String TEXT = "| one two three four five six seven eight nine ten eleven twelve thirteen |";
    private static final double EPSILON = 0.0001;

    public TextAreaLayoutRobotTest() {
        super(new TextArea());
    }

    /**
     * Tests that the wrap text property is honored when changing font.
     * JDK-8314683
     */
    @Test
    public void testWrapWhenChangingFont() {
        waitForIdle();

        Util.runAndWait(() -> {
            Window w = control.getScene().getWindow();
            w.setWidth(200);
            w.setHeight(200);
            control.setWrapText(false);
            control.setText(TEXT);
        });

        waitForIdle();

        Util.runAndWait(() -> {
            int len = TEXT.length() - 1;
            double y0 = getCharPositionY(0);
            double y1 = getCharPositionY(len);
            Assertions.assertEquals(y0, y1, EPSILON);
        });

        waitForIdle();

        Util.runAndWait(() -> {
            control.setFont(Font.font("Dialog", 24));
        });

        waitForIdle();

        Util.runAndWait(() -> {
            int len = TEXT.length() - 1;
            double y0 = getCharPositionY(0);
            double y1 = getCharPositionY(len);

            Assertions.assertEquals(y0, y1, EPSILON);
        });
    }

    private double getCharPositionY(int ix) {
        TextAreaSkin skin = (TextAreaSkin)control.getSkin();
        Rectangle2D r = skin.getCharacterBounds(ix);
        return r.getMinY();
    }
}
