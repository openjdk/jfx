/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.text;

import java.util.Arrays;
import java.util.Collection;

import javafx.geometry.VPos;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import test.com.sun.javafx.test.CssMethodsTestBase;

@RunWith(Parameterized.class)
public class Text_cssMethods_Test extends CssMethodsTestBase {
    private static final Text TEST_TEXT = new Text();

    @Parameters
    public static Collection data() {
        return Arrays.asList(new Object[] {
            config(TEST_TEXT, "font", Font.getDefault(),
                   "-fx-font", Font.font("Verdana", FontWeight.BOLD, 22)),
            config(TEST_TEXT, "underline", false, "-fx-underline", true),
            config(TEST_TEXT, "strikethrough", false,
                   "-fx-strikethrough", true),
            config(TEST_TEXT, "textAlignment", TextAlignment.LEFT,
                   "-fx-text-alignment", TextAlignment.CENTER),
            config(TEST_TEXT, "textOrigin", VPos.BASELINE,
                   "-fx-text-origin", VPos.BOTTOM),
            config(TEST_TEXT, "translateX", 0.0, "-fx-translate-x", 10.0),
            config(TEST_TEXT, "fontSmoothingType", FontSmoothingType.LCD,
                "-fx-font-smoothing-type", FontSmoothingType.GRAY),
            config(TEST_TEXT, "tabSize", 8, "-fx-tab-size", 4)
        });
    }

    public Text_cssMethods_Test(final Configuration configuration) {
        super(configuration);
    }
}
