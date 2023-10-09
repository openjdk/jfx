/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.tools;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

/**
 * Collects information about the system and generates the report.
 */
public class SystemInfo {
    private final DecimalFormat numberFormat = new DecimalFormat("#0.###");
    private final StringBuilder sb = new StringBuilder();

    private SystemInfo() {

    }

    public static String generateReport() {
        return new SystemInfo().collect();
    }

    private void nl() {
        sb.append('\n');
    }

    private void header(String text) {
        sb.append(text);
        sb.append('\n');
        for (int i = 0; i < text.length(); i++) {
            sb.append('=');
        }
        nl();
    }

    private void safe(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x20) {
                sb.append(String.format("\\u00%02X", (int)c));
            } else {
                sb.append(c);
            }
        }
    }

    private void f(double x) {
        sb.append(numberFormat.format(x));
    }

    private void dumpBounds(Rectangle2D r) {
        sb.append("{x=");
        f(r.getMinX());
        sb.append(", y=");
        f(r.getMinY());
        sb.append(", w=");
        f(r.getWidth());
        sb.append(", h=");
        f(r.getHeight());
        sb.append("}");
    }

    private void dumpScreen(Screen screen, boolean primary, int num) {
        sb.append("  ");
        if (primary) {
            sb.append("Primary ");
        }
        sb.append("Screen");
        if (!primary) {
            sb.append(" #");
            sb.append(num);
        }
        sb.append(":");
        nl();
        sb.append("    dpi=");
        f(screen.getDpi());
        nl();
        sb.append("    bounds=");
        dumpBounds(screen.getBounds());
        nl();
        sb.append("    visual bounds=");
        dumpBounds(screen.getVisualBounds());
        nl();
        sb.append("    output.scale.x=");
        f(screen.getOutputScaleX());
        nl();
        sb.append("    output.scale.y=");
        f(screen.getOutputScaleY());
        nl();
    }

    private String collect() {
        // system properties
        header("System Properties");
        {
            Properties p = System.getProperties();
            ArrayList<String> keys = new ArrayList<>(p.stringPropertyNames());
            Collections.sort(keys);
            for (String k: keys) {
                String v = System.getProperty(k);
                sb.append(k);
                sb.append("=");
                safe(v);
                sb.append('\n');
            }
        }

        // environment
        sb.append('\n');
        header("Environment");
        {
            Map<String, String> env = System.getenv();
            ArrayList<String> keys = new ArrayList<>(env.keySet());
            Collections.sort(keys);
            for (String k: keys) {
                String v = env.get(k);
                sb.append(k);
                sb.append("=");
                safe(v);
                sb.append('\n');
            }
        }

        // screens
        sb.append('\n');
        header("Screens");
        {
            int num = 1;
            Screen primary = Screen.getPrimary();
            dumpScreen(primary, true, num++);

            List<Screen> screens = Screen.getScreens();
            for (Screen s: screens) {
                if (!s.equals(primary)) {
                    dumpScreen(s, false, num++);
                }
            }
        }
        return sb.toString();
    }
}
