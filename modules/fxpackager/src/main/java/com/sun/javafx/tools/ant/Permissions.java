/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.ant;

import com.oracle.tools.packager.Log;
import org.apache.tools.ant.types.DataType;

/**
 * Definition of security permissions needed by application.
 * By default it is assumed that application may run in sandbox.
 * Requesting elevated permissions assumes that application jar
 * files are signed.
 *
 * @ant.type name="Permissions" category="javafx"
 */
public class Permissions extends DataType {
    @Deprecated final boolean embed = false;
    private boolean elevated = true;

    /**
     * If set to false then application can run in sandbox.
     *
     * @ant.not-required Default is true.
     */
    public void setElevated(boolean v) {
        elevated = v;
    }
    
    public boolean getElevated() {
        return get().elevated;
    }

    /**
     * If true then certificate used to sign jar files will be cached
     * in the JNLP file. This allows to ask user to accept elevating
     * permissions earlier and improves startup time.
     * <p>
     * This has no effect if application is run in the sandbox.
     *
     * @ant.not-required By default is false.
     */
    public void setCacheCertificates(boolean v) {
        if (v) {
            Log.info("JavaFX Ant Tasks no longer support caching certificates in JNLP.  Setting ignored.");
        }
    }
    
    private Permissions get() {
        if (isReference()) {
            return (Permissions) getRefid().getReferencedObject();
        }
        return this;
    }
}
