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
import javafx.geometry.Side;
import com.sun.javafx.test.BuilderTestBase;
import com.sun.javafx.test.ValueComparator;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 */
@RunWith(Parameterized.class)
public class BackgroundPosition_builder_Test extends BuilderTestBase {
    @Parameterized.Parameters
    public static Collection data() {
        BuilderTestBase.Configuration cfg = new BuilderTestBase.Configuration(BackgroundPosition.class);
        cfg.addProperty("horizontalSide", Side.RIGHT);
        cfg.addProperty("horizontalPosition", 3.0);
        cfg.addProperty("horizontalAsPercentage", false);
        cfg.addProperty("verticalSide", Side.BOTTOM);
        cfg.addProperty("verticalPosition", 1.0);
        cfg.addProperty("verticalAsPercentage", true);

        BuilderTestBase.Configuration cfg2 = new BuilderTestBase.Configuration(BackgroundPosition.class);
        cfg2.addProperty("horizontalPosition", 3.0);
        cfg2.addProperty("horizontalAsPercentage", false);

        BuilderTestBase.Configuration cfg3 = new BuilderTestBase.Configuration(BackgroundPosition.class);
        cfg3.addProperty("verticalSide", null, new ValueComparator() {
            @Override public boolean equals(Object expected, Object actual) {
                return actual == Side.TOP;
            }
        });
        cfg3.addProperty("horizontalSide", null, new ValueComparator() {
            @Override public boolean equals(Object expected, Object actual) {
                return actual == Side.LEFT;
            }
        });

        return Arrays.asList(new Object[] {
            config(cfg),
            config(cfg2),
            config(cfg3)
        });
    }

    public BackgroundPosition_builder_Test(final Configuration configuration) {
        super(configuration);
    }
}
