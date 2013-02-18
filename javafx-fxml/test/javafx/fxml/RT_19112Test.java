/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

public class RT_19112Test {
    @Test
    public void testStaticProperty() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_19112.fxml"));
        fxmlLoader.load();

        Widget widget1 = (Widget)fxmlLoader.getNamespace().get("widget1");
        assertEquals(Widget.getAlignment(widget1), Alignment.LEFT);

        Widget widget2 = (Widget)fxmlLoader.getNamespace().get("widget2");
        assertEquals(Widget.getAlignment(widget2), Alignment.RIGHT);
    }
}
