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

package test.javafx.scene.text;

import java.util.concurrent.CountDownLatch;
import javafx.geometry.Bounds;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

/**
 * Tests TextFlow Node
 */
public class TextFlowNodeTest {
    @BeforeAll
    public static void initFX() {
        CountDownLatch startupLatch = new CountDownLatch(1);
        Util.startup(startupLatch, startupLatch::countDown);
    }

    @Test
    public void testUnderlineShape() {
        Text t1 = new Text("one ");
        t1.setFont(new Font("Monospaced Regular", 16));
        Text t2 = new Text("two.");
        TextFlow f = new TextFlow(t1, t2);

        // underline 0,0 must be empty
        PathElement[] p = f.underlineShape(0, 0);
        Assertions.assertNotNull(p);
        Assertions.assertEquals(0, p.length);

        // underline 1,0 .. 1,len must increase monotonically
        int len = t1.getText().length() + t2.getText().length();
        double w = 0.0;
        for (int i = 1; i < len; i++) {
            p = f.underlineShape(0, i);
            Assertions.assertNotNull(p);

            // width must increase
            Bounds b = new Path(p).getBoundsInLocal();
            Assertions.assertTrue(b.getWidth() > w);
            w = b.getWidth();

            // test height greater than zero
            Assertions.assertTrue(b.getHeight() > 0.0);
        }

        // 0,1000 same as 0,len
        Bounds b1 = new Path(f.underlineShape(0, len)).getBoundsInLocal();
        Bounds b2 = new Path(f.underlineShape(0, 1000)).getBoundsInLocal();
        Assertions.assertEquals(b1, b2);
        Assertions.assertTrue(b1.getHeight() > 0.0);
    }
}
