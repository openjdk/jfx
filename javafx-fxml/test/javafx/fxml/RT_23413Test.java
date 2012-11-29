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

import javafx.fxml.FXMLLoader;

import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.*;

public class RT_23413Test {
    private static URL LOCATION = RT_23413.class.getResource("rt_23413.fxml");
    private static final int COUNT = 500;

    @Test
    public void testTemplate() throws Exception {
        assertTrue(testTemplate(true) < testTemplate(false));
    }

    private long testTemplate(boolean template) throws Exception {
        FXMLLoader loader = new FXMLLoader(LOCATION);
        loader.setTemplate(template);

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            if (!template) {
                loader.setRoot(null);
            }

            loader.load();
        }

        long t1 = System.currentTimeMillis();

        long duration = t1 - t0;
        long average = duration / COUNT;

        System.out.printf("template:%b duration:%dms average:%d\n", template, duration, average);

        return average;
    }
}
