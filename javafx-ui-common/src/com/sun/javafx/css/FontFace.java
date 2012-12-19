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
package com.sun.javafx.css;

import java.util.List;
import java.util.Map;

/**
 * A FontFace is a @font-face definition from CSS file
 */
final public class FontFace {
    public static enum FontFaceSrcType {URL,LOCAL, REFERENCE};

    private final Map<String,String> descriptors;
    private final List<FontFaceSrc> sources;

    public FontFace(Map<String, String> descriptors, List<FontFaceSrc> sources) {
        this.descriptors = descriptors;
        this.sources = sources;
    }

    public Map<String, String> getDescriptors() {
        return descriptors;
    }

    public List<FontFaceSrc> getSources() {
        return sources;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("@font-face { ");
        for(Map.Entry<String,String> desc: descriptors.entrySet()) {
            sb.append(desc.getKey());
            sb.append(" : ");
            sb.append(desc.getValue());
            sb.append("; ");
        }
        sb.append("src : ");
        for(FontFaceSrc src: sources) {
            sb.append(src.getType());
            sb.append(" \"");
            sb.append(src.getSrc());
            sb.append("\", ");
        }
        sb.append("; ");
        sb.append(" }");
        return sb.toString();
    }

    public static class FontFaceSrc {
        private final FontFaceSrcType type;
        private final String src;
        private final String format;

        public FontFaceSrc(FontFaceSrcType type, String src, String format) {
            this.type = type;
            this.src = src;
            this.format = format;
        }

        public FontFaceSrc(FontFaceSrcType type, String src) {
            this.type = type;
            this.src = src;
            this.format = null;
        }

        public FontFaceSrcType getType() {
            return type;
        }

        public String getSrc() {
            return src;
        }

        public String getFormat() {
            return format;
        }
    }
}
