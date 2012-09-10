/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import java.util.Arrays;
import java.util.Collection;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;
import com.sun.javafx.test.BuilderTestBase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 */
@RunWith(Parameterized.class)
public class BorderStroke_builder_Test extends BuilderTestBase {
    @Parameterized.Parameters
    public static Collection data() {
        BuilderTestBase.Configuration cfg = new BuilderTestBase.Configuration(BorderStroke.class);
        cfg.addProperty("topStroke", Color.PURPLE);
        cfg.addProperty("rightStroke", Color.YELLOW);
        cfg.addProperty("bottomStroke", Color.VIOLET);
        cfg.addProperty("leftStroke", Color.PALEGREEN);
        cfg.addProperty("topStyle", new BorderStrokeStyle(StrokeType.OUTSIDE, null, null, 5, 3, null));
        cfg.addProperty("rightStyle", new BorderStrokeStyle(StrokeType.INSIDE, null, null, 6, 3, null));
        cfg.addProperty("bottomStyle", new BorderStrokeStyle(StrokeType.CENTERED, null, null, 7, 3, null));
        cfg.addProperty("leftStyle", new BorderStrokeStyle(StrokeType.OUTSIDE, null, null, 8, 3, null));
        cfg.addProperty("radii", new CornerRadii(4));
        cfg.addProperty("widths", new BorderWidths(3));
        cfg.addProperty("insets", new Insets(-5));

        return Arrays.asList(
                new Object[] {
                        config(cfg),
                });
    }

    public BorderStroke_builder_Test(final Configuration configuration) {
        super(configuration);
    }
}
