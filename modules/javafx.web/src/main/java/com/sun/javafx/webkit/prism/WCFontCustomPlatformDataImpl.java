/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import com.sun.javafx.font.FontFactory;
import com.sun.javafx.font.PGFont;
import com.sun.prism.GraphicsPipeline;
import com.sun.webkit.graphics.WCFont;
import com.sun.webkit.graphics.WCFontCustomPlatformData;
import java.io.IOException;
import java.io.InputStream;

final class WCFontCustomPlatformDataImpl extends WCFontCustomPlatformData {

    private final PGFont font;


    WCFontCustomPlatformDataImpl(InputStream inputStream) throws IOException {
        FontFactory factory = GraphicsPipeline.getPipeline().getFontFactory();
        PGFont[] fa = factory.loadEmbeddedFont(null, inputStream,
                                               10, false, false);
        if (fa == null) {
            throw new IOException("Error loading font");
        }
        font = fa[0];
    }


    @Override
    protected WCFont createFont(int size, boolean bold, boolean italic) {
        FontFactory factory = GraphicsPipeline.getPipeline().getFontFactory();
        return new WCFontImpl(factory.deriveFont(font, bold, italic, size));
    }
}
