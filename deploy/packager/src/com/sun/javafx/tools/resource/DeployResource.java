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

package com.sun.javafx.tools.resource;

import java.io.File;

public class DeployResource extends PackagerResource {
    private final Type type;
    private final String mode;
    private final String os;
    private final String arch;

    public static enum Type {
        UNKNOWN, jnlp, jar, nativelib, icon, license, data
    };

    public static final String TYPE_LICENSE = "license";
    public static final String TYPE_DATA    = "data";

    public DeployResource(final File baseDir, final String path) {
        this(baseDir, path, "eager");
    }

    public DeployResource(final File baseDir, final File file) {
        this(baseDir, file, "eager");
    }

    public DeployResource(final File baseDir, final String path,
                          final String mode) {
        super(baseDir, path);
        this.mode = mode;
        this.type = null;
        this.os = null;
        this.arch = null;
    }

    public DeployResource(final File baseDir, final File file,
                          final String mode) {
        this(baseDir, file, mode, null, null, null);
    }

    public DeployResource(final File baseDir, final File file,
                          final String mode, final String type,
                          final String os, final String arch) {
        super(baseDir, file);
        if (type == null) {
           if (file.getName().endsWith(".jar")) {
               this.type = Type.jar;
           } else if (file.getName().endsWith(".jnlp")) {
               this.type = Type.jnlp;
           } else {
            this.type = Type.UNKNOWN;
           }
        } else {
              this.type = Type.valueOf(type);
        }
        this.os = os;
        this.arch = arch;
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public Type getType() {
        return type;
    }

    public String getOs() {
        return os;
    }

    public String getArch() {
        return arch;
    }
}
