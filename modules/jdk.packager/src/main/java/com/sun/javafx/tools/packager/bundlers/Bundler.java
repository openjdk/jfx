/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.packager.bundlers;

/**
 * @deprecated use {@link ToolProvider} to locate the {@code "javapackager"} tool instead.
 */
@Deprecated(since="10", forRemoval=true)
public final class Bundler {

    private Bundler() {}

    /**
     * Located here for backwards compatibility
     */
    @Deprecated
    public enum BundleType {
        NONE,
        @Deprecated
        ALL,      // Generates all bundlers
        JNLP,     // Generates JNLP
        NATIVE,   // Generates both app image and all installers
        IMAGE,    // Generates app image only
        INSTALLER // Generates installers
    }

    @Deprecated
    public static final class Bundle {
        public BundleType type = BundleType.NONE;
        public String format = null;
    }

    @Deprecated
    static public Bundle stringToBundle(String value) {
        Bundle result = new Bundle();

        if (!value.isEmpty()) {
            if ("false".equals(value) || "none".equals(value)) {
                result.type = BundleType.NONE;
            } else if ("all".equals(value) || "true".equals(value)) {
                result.type = BundleType.ALL;
            } else if ("jnlp".equals(value)) {
                result.type = BundleType.JNLP;
            } else if ("image".equals(value)) {
                result.type = BundleType.IMAGE;
            } else if ("native".equals(value)) {
                result.type = BundleType.NATIVE;
            } else if ("installer".equals(value)) {
                result.type = BundleType.INSTALLER;
            } else {
                //assume it is request to build only specific format (like exe or msi)
                result.type = BundleType.INSTALLER;
                result.format = (value != null) ? value.toLowerCase() : null;
            }
        }

        return result;
    }
}
