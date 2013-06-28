/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import com.sun.javafx.test.BuilderTestBase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static javafx.scene.layout.BorderRepeat.*;

@RunWith(Parameterized.class)
public class Border_builder_Test extends BuilderTestBase {
    @Parameterized.Parameters
    public static Collection data() {
        final List<BorderStroke> strokes = new ArrayList<BorderStroke>();
        strokes.add(new BorderStroke(Color.GREEN, BorderStrokeStyle.DASHED, new CornerRadii(3), new BorderWidths(4)));
        strokes.add(new BorderStroke(Color.BLUE, BorderStrokeStyle.DOTTED, new CornerRadii(6), new BorderWidths(8)));

        final Image image1 = new Image("javafx/scene/layout/red.png");
        final Image image2 = new Image("javafx/scene/layout/blue.png");
        final List<BorderImage> images = new ArrayList<BorderImage>();
        images.add(new BorderImage(image1, new BorderWidths(6), new Insets(2), BorderWidths.EMPTY, false, SPACE, ROUND));
        images.add(new BorderImage(image2, new BorderWidths(3), new Insets(4), BorderWidths.EMPTY, false, REPEAT, REPEAT));

        BuilderTestBase.Configuration cfg = new BuilderTestBase.Configuration(Border.class);
        cfg.addProperty("strokes", strokes);
        cfg.addProperty("images", images);

        return Arrays.asList(
                new Object[] {
                        config(cfg),
                });
    }

    public Border_builder_Test(final Configuration configuration) {
        super(configuration);
    }
}
