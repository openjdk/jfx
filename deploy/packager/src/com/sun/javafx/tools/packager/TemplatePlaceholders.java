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

package com.sun.javafx.tools.packager;

public enum TemplatePlaceholders {

    //location of dtjava.js
    SCRIPT_URL("DT.SCRIPT.URL"),
    //script element to include dtjava.js
    SCRIPT_CODE("DT.SCRIPT.CODE"),
    //code to embed applet into given placeholder
    //need to be wrapped with function()
    EMBED_CODE_DYNAMIC("DT.EMBED.CODE.DYNAMIC"),
    //code needed to embed applet fron the onload hook (except inclusion of dtjava.js)
    EMBED_CODE_ONLOAD("DT.EMBED.CODE.ONLOAD"),
    //code need to launch application
    //need to be wrapped with function()
    LAUNCH_CODE("DT.LAUNCH.CODE"),

    EMBED_STATIC_HEADER("DT.EMBED.STATIC.HEADER"),
    EMBED_STATIC_FOOTER("DT.EMBED.STATIC.FOOTER"),
    EMBED_STATIC_CODE("DT.EMBED.STATIC.CODE");

    private String placeholder;

    private TemplatePlaceholders(String ph) {
        placeholder = ph;
    }
    public String getPlaceholder() {
        return placeholder;
    }

    public static TemplatePlaceholders fromString(String text) {
        if (text != null) {
            for (TemplatePlaceholders b : TemplatePlaceholders.values()) {
                if (text.equalsIgnoreCase(b.placeholder)) {
                    return b;
                }
            }
        }
        return null;
    }
}
