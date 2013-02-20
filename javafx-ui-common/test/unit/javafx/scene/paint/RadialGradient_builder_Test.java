/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.paint;

import com.sun.javafx.test.BuilderTestBase;
import com.sun.javafx.test.DoubleComparator;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class RadialGradient_builder_Test extends BuilderTestBase {
    @Parameters
    public static Collection data() {
        BuilderTestBase.Configuration cfg = new BuilderTestBase.Configuration(RadialGradient.class);

        cfg.addProperty("centerX", 0.2);
        cfg.addProperty("centerY", 0.3);
        cfg.addProperty("focusAngle", 0.4);
        cfg.addProperty("focusDistance", 0.6);
        cfg.addProperty("radius", 0.8);
        cfg.addProperty("proportional", false);
        cfg.addProperty("cycleMethod", CycleMethod.NO_CYCLE);
        cfg.addProperty("stops",  (List<Stop>)Arrays.asList(new Stop(0.0, Color.RED), new Stop(1.0, Color.BEIGE)) );

        return Arrays.asList(new Object[] {
            config(cfg)
        });
    }

    public RadialGradient_builder_Test(final Configuration configuration) {
        super(configuration);
    }
}
