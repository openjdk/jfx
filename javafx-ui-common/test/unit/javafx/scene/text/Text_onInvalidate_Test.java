/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.text;

import java.util.Arrays;
import java.util.Collection;

import javafx.geometry.VPos;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.test.OnInvalidateMethodsTestBase;

@RunWith(Parameterized.class)
public class Text_onInvalidate_Test extends OnInvalidateMethodsTestBase {

    public Text_onInvalidate_Test(Configuration config) {
        super(config);
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
              //rich text aware
              {new Configuration(Text.class, "text", "cool", new DirtyBits[] {DirtyBits.NODE_CONTENTS, DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY})},
              {new Configuration(Text.class, "x", 123.0, new DirtyBits[] {DirtyBits.NODE_GEOMETRY})},
              {new Configuration(Text.class, "y", 123.0, new DirtyBits[] {DirtyBits.NODE_GEOMETRY})},
              {new Configuration(Text.class, "font", new Font(10) , new DirtyBits[] {DirtyBits.TEXT_FONT, DirtyBits.NODE_CONTENTS, DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY})},
              {new Configuration(Text.class, "wrappingWidth", 5 , new DirtyBits[] {DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY})}, //note wrapping width sometimes can also cause DirtyBits.NODE_CONTENTS, 
              {new Configuration(Text.class, "underline", true , new DirtyBits[] {DirtyBits.TEXT_ATTRS})},
              {new Configuration(Text.class, "strikethrough", true , new DirtyBits[] {DirtyBits.TEXT_ATTRS})},
              {new Configuration(Text.class, "textAlignment", TextAlignment.RIGHT , new DirtyBits[] {DirtyBits.NODE_CONTENTS, DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY})},
              {new Configuration(Text.class, "textOrigin", VPos.BOTTOM , new DirtyBits[] {DirtyBits.NODE_BOUNDS , DirtyBits.NODE_GEOMETRY})},
              {new Configuration(Text.class, "boundsType", TextBoundsType.VISUAL , new DirtyBits[] {DirtyBits.NODE_BOUNDS , DirtyBits.NODE_GEOMETRY})}
        };
        return Arrays.asList(data);
    }
}
