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

package javafx.scene.effect;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.PropertiesTestBase;

@RunWith(Parameterized.class)
public final class Lighting_properties_Test extends PropertiesTestBase {
    @Parameters
    public static Collection data() {
        final Lighting testLighting = new Lighting();

        return Arrays.asList(new Object[] {
            config(testLighting, "light", 
                   new Light.Distant(),
                   new Light.Point()),
            config(testLighting, "bumpInput", null, new BoxBlur()),
            config(testLighting, "contentInput", null, new BoxBlur()),
            config(testLighting, "diffuseConstant", 1.0, 1.5),
            config(testLighting, "specularConstant", 0.3, 0.6),
            config(testLighting, "specularExponent", 20.0, 30.0),
            config(testLighting, "surfaceScale", 1.5, 0.5)
        });
    }

    public Lighting_properties_Test(final Configuration configuration) {
        super(configuration);
    }
}
