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

package com.sun.javafx.tk;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public abstract class FontLoader {
    public abstract void loadFont(Font font);
    public abstract List<String> getFamilies();
    public abstract List<String> getFontNames();
    public abstract List<String> getFontNames(String family);
    public abstract Font font(String family, FontWeight weight,
                              FontPosture posture, float size);
    /* The following function was there for fxdloader *only*.
     * Its now left here only until we remove the Swing pipeline.
     */
    public abstract Font font(Object inStream, float size);
    public abstract FontMetrics getFontMetrics(Font font);
    public abstract float computeStringWidth(String string, Font font);
    public abstract float getSystemFontSize();

    /** Default implementation temporarily uses font() above which
     * is obsoleted but left here until we remove the Swing pipeline.
     */
    public Font loadFont(InputStream in, double size) {
        return font(in,  (float)size);
    }

    /** Default implementation uses font() which is implemented for
     * all pipelines.
     */
    public Font loadFont(String path, double size) {
        InputStream in = null;
        Font font = null;
        try {
            in = new FileInputStream(path);
            font = font(in, (float)size);
        } catch (Exception e) {
        } finally {
            if (in != null) {
              try {
                in.close();
              }
              catch (Exception e) {
              }
            }
        }
        return font;
    }
}
