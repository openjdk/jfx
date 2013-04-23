/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;

class GLGPUInfo {

    final String vendor;    // Information extracted from the GL_VENDOR string
    final String model;     // Information extracted from the GL_RENDERER string

    // Currently, we have no need for version field. Minimum version check is
    // done elsewhere.
    // String version;      // Information extracted from the GL_VERSION string

    GLGPUInfo(String vendor, String model) {
        this.vendor = vendor;
        this.model = model;
    }

    /**
     * Check this GPU information against an entry stored in the whiteList and
     * blackList of ES2Qualifier
     *
     * @param gi entry stored in the whiteList or blackList of ES2Qualifier
     * @return true if sub-string matches otherwise false
     */
    boolean matches(GLGPUInfo gi) {

        // Note: this.vendor and this.model can't be null hence no need to do
        // null check. This check is done in the GLFactory.isQualified() method.

        boolean result = true;
        if (gi.vendor != null) {
            result = vendor.startsWith(gi.vendor);
        }
        if (gi.model != null) {
            result = model.contains(gi.model);
        }
        return result;
    }

    @Override public String toString() {
        return "GLGPUInfo [vendor = " + vendor + ", model = " + model + "]";
    }
}
