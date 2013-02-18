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

package com.sun.javafx.tools.ant;

import org.apache.tools.ant.BuildException;

/**
 * Extension of standard ant FileSet type that provide means to
 * specify optional meta information on selected set of files.
 * This includes:
 *   <ul>
 *     <li> type or resource (JNLP, jar, etc)
 *     <li> OS and Architecture for which this resource is applicable
 *     <li> hint on when this resource is needed
 *          (helps to optimize loading order)
 *    </ul>
 *
 * Examples:
 * <pre>
 *        &lt;fx:fileset dir="dist" includes="app.jar"/>
 * </pre>
 * Defines set consisting of single jar file (type will be detected based on extension)
 * that is applicable to all OS/arch combinations and needed for application startup.
 * <p>
 * <pre>
 *        &lt;fx:fileset dir="dist" neededFor="preloader" os="windows">
 *            &lt;include name="*.jar"/>
 *        &lt;/fx:fileset>
 * </pre>
 * All the jars in the "dist" folder for Windows platfrom only. These jars needed
 * to be available to launch preloader.
 *
 * @ant.type name="fileset" category="javafx"
 */
public class FileSet extends org.apache.tools.ant.types.FileSet {
    //TODO: add support for locale & platform (see JNLP spec)

    //autoguess based on extension
    public final static int TYPE_AUTO = 0;
    public final static int TYPE_JAR = 1;
    public final static int TYPE_NATIVELIB = 2;
    public final static int TYPE_ICON = 3;
    public final static int TYPE_JNLP = 4;

    //these 2 types are only applicable to native bundles
    public final static int TYPE_DATA = 5;
    public final static int TYPE_LICENSE = 6;

    private int type = TYPE_AUTO;
    private String mode = "eager";
    private String os = null;
    private String arch = null;

    private FileSet get() {
        if (isReference()) {
            return (FileSet) getRefid().getReferencedObject();
        }
        return this;
    }
    //better use enum but then it need to be refactored ...
    final String[] types = {null, "jar", "nativelib", "icon", "jnlp", "data", "license"};

    public String getTypeAsString() {
        return types[get().type];
    }

    public String getOs() {
        return get().os;
    }

    public String getMode() {
        return get().mode;
    }

    public String getArch() {
        return get().arch;
    }

    /**
     * Type of the resources in the set. Supported types are "auto" for autodetect,
     * "jar", "jnlp", "native" for jar containing native libraries and "icon".
     *
     * @ant.not-required Default is to guess based on extension.
     */
    public void setType(String v) {
        if ("jar".equals(v)) {
            type = TYPE_JAR;
        } else if ("native".equals(v)) {
            type = TYPE_NATIVELIB;
        } else if ("icon".equals(v)) {
            type = TYPE_ICON;
        } else if ("jnlp".equals(v)) {
            type = TYPE_JNLP;
        } else if ("auto".equals(v)) {
            type = TYPE_AUTO;
        } else if ("data".equals(v)) {
            type = TYPE_DATA;
        } else if ("license".equals(v)) {
            type = TYPE_LICENSE;
        } else {
            throw new BuildException("Unsupported resource type [" + v + "].");
        }
    }

    /**
     * Defines when resources are needed (impacts loading priority).
     * Supported levels are:
     * <ul>
     *    <li> <em>preloader</em> - resources are needed to launch preloader
     *         (first thing to be executed)
     *    <li> <em>startup</em> - resources are needed for application startup.
     *    <li> <em>runtime</em> - resources are not required before application
     *       starts but may be needed at runtime.
     * </ul>
     *
     * @ant.not-required Default is "startup".
     */
    public void setRequiredFor(String v) {
        if ("preloader".equals(v)) {
            mode = "progress";
        } else if ("startup".equals(v)) {
            mode = "eager";
        } else if ("runtime".equals(v)) {
            mode = "lazy";
        } else {
            throw new BuildException("Unknown requiredFor value [" + v + "]");
        }
    }

    /**
     * Specifies the operating systems for which these resources should be considered.
     *
     * @ant.not-required Default is any.
     */
    public void setOs(String v) {
        os = v;
    }

    /**
     * Specifies the architecture for which these resources should be considered.
     *
     * @ant.not-required Default is any.
     */
    public void setArch(String v) {
        arch = v;
    }
}
