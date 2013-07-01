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

import java.util.Arrays;
import java.util.Collection;
import com.sun.javafx.test.BuilderTestBase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 */
@RunWith(Parameterized.class)
public class BackgroundSize_builder_Test extends BuilderTestBase {
    @Parameterized.Parameters
    public static Collection data() {
        BuilderTestBase.Configuration cfg = new BuilderTestBase.Configuration(BackgroundSize.class);
        cfg.addProperty("width", 1.0);
        cfg.addProperty("height", 2.0);
        cfg.addProperty("widthAsPercentage", true);
        cfg.addProperty("heightAsPercentage", false);
        cfg.addProperty("contain", false);
        cfg.addProperty("cover", false);

        BuilderTestBase.Configuration cfg2 = new BuilderTestBase.Configuration(BackgroundSize.class);
        cfg2.addProperty("widthAsPercentage", false);
        cfg2.addProperty("heightAsPercentage", true);
        cfg2.addProperty("cover", true);

        return Arrays.asList(new Object[] {
            config(cfg),
            config(cfg2)
        });
    }

    public BackgroundSize_builder_Test(final Configuration configuration) {
        super(configuration);
    }
}
