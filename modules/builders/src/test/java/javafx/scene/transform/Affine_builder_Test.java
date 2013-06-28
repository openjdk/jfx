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

package javafx.scene.transform;

import com.sun.javafx.test.BuilderTestBase;
import java.util.Arrays;
import java.util.Collection;
import javafx.event.EventHandler;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class Affine_builder_Test extends BuilderTestBase {
    @Parameters
    public static Collection data() {
        BuilderTestBase.Configuration cfg = new BuilderTestBase.Configuration(Affine.class);

        cfg.addProperty("mxx", 1.0);
        cfg.addProperty("mxy", 2.0);
        cfg.addProperty("mxz", 3.0);
        cfg.addProperty("myx", 4.0);
        cfg.addProperty("myy", 5.0);
        cfg.addProperty("myz", 6.0);
        cfg.addProperty("mzx", 7.0);
        cfg.addProperty("mzy", 8.0);
        cfg.addProperty("mzz", 9.0);
        cfg.addProperty("tx", 10.0);
        cfg.addProperty("ty", 11.0);
        cfg.addProperty("tz", 12.0);

        return Arrays.asList(new Object[] {
            config(cfg)
        });
    }

    public Affine_builder_Test(final Configuration configuration) {
        super(configuration);
    }
}
