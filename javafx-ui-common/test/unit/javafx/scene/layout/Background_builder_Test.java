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

import static javafx.scene.layout.BackgroundRepeat.*;

@RunWith(Parameterized.class)
public class Background_builder_Test extends BuilderTestBase {
    @Parameterized.Parameters
    public static Collection data() {
        final List<BackgroundFill> fills = new ArrayList<BackgroundFill>();
        fills.add(new BackgroundFill(Color.GREEN, new CornerRadii(3), new Insets(4)));
        fills.add(new BackgroundFill(Color.BLUE, new CornerRadii(6), new Insets(8)));

        final Image image1 = new Image("javafx/scene/layout/red.png");
        final Image image2 = new Image("javafx/scene/layout/blue.png");
        final List<BackgroundImage> images = new ArrayList<BackgroundImage>();
        images.add(new BackgroundImage(image1, SPACE, SPACE, null, null));
        images.add(new BackgroundImage(image2, ROUND, ROUND, null, null));

        BuilderTestBase.Configuration cfg = new BuilderTestBase.Configuration(Background.class);
        cfg.addProperty("fills", fills);
        cfg.addProperty("images", images);

        return Arrays.asList(
                new Object[] {
                        config(cfg),
                });
    }

    public Background_builder_Test(final Configuration configuration) {
        super(configuration);
    }
}
