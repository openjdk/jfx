/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.fxml;

import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.*;

public class RT_18680 {
    @Test
    public void testEscapeSequences() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_18680.fxml"));
        fxmlLoader.load();

        Widget widget1 = (Widget)fxmlLoader.getNamespace().get("widget1");
        assertEquals(widget1.getName(), fxmlLoader.getNamespace().get("abc"));

        Widget widget2 = (Widget)fxmlLoader.getNamespace().get("widget2");
        assertEquals(widget2.getName(), "$abc");

        Widget widget3 = (Widget)fxmlLoader.getNamespace().get("widget3");
        assertEquals(widget3.getName(), "$abc");

        Widget widget4 = (Widget)fxmlLoader.getNamespace().get("widget4");
        assertEquals(widget4.getName(), "\\abc");
    }
}
