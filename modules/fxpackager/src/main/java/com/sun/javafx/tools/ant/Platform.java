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

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;

/**
 * Defines application platform requirements.
 *
 * Examples:
 * <pre>
 *    &lt;fx:platform javafx="2.0" j2se="7.0"/&gt;
 * </pre>
 * Application need JavaFX Runtime version 2.0 or later and JRE version 7.0 or later.
 *
 * Examples:
 * <pre>
 *    &lt;fx:platform javafx="2.0"&gt;
 *       &lt;jvmarg value="-Xmx400m"/&gt;
 *       &lt;jvmarg value="-verbose:jni"/&gt;
 *       &lt;property name="purpose" value="sample"/&gt;
 *    &lt;/fx:platform&gt;
 * </pre>
 * Application need JavaFX Runtime version 2.0 and need to be run in JVM launched
 * with "-Xmx400 -verbose:jni -Dpurpose=sample".
 *
 * @ant.type name="platform" category="javafx"
 */

public class Platform extends DataType {
    /**
     * Optional element (could be used multiple times).
     *
     * JVM argument to be set in the JVM where application is executed.
     *
     * @ant.not-required
     */
    public static class Jvmarg {
        String value;

        /**
         * Value of JVM argument.
         *
         * @ant.required
         */
        public void setValue(String v) {
            value = v;
        }
    }

    /**
     * Optional element (could be used multiple times).
     *
     * Java property to be set in the JVM where application is executed.
     *
     * @ant.not-required
     */
    public static class Property {
        String value;
        String name;

        /**
         * Value of property to be set.
         *
         * @ant.required
         */
        public void setValue(String v) {
            value = v;
        }

        /**
         * Name of property to be set.
         *
         * @ant.required
         */
        public void setName(String v) {
            name = v;
        }
    }

    String javaRoot = null; //used for self-contained apps
    String j2se = null;
    String javafx = null;
    List<Property> properties = new LinkedList<Property>();
    List<Jvmarg> jvmargs = new LinkedList<Jvmarg>();
    List<Property> jvmUserArgs = new LinkedList<Property>();

    /**
     * Minimum version of JRE required by application.
     *
     * @ant.not-required Default is any JRE supporting JavaFX.
     */
    public void setJ2se(String v) {
        j2se = v;
    }

    /**
     * Minimum version of JavaFX required by application.
     *
     * @ant.not-required Default is 2.0.
     */
    public void setJavafx(String v) {
        javafx = v;
    }

    public final static String USE_SYSTEM_JRE = "";

    public void setBasedir(String v) {
        if (v.trim().isEmpty()) {
            //special case: request to use system runtime
            javaRoot = USE_SYSTEM_JRE;
        } else {
            File f = new File(v);
            if (!f.exists()) {
                throw new BuildException(
                        "Specified JDK location do not exist: " + v);
            }
            javaRoot = f.getAbsolutePath();
        }
    }

    public Property createProperty() {
        Property t = new Property();
        properties.add(t);
        return t;
    }

    public Jvmarg createJvmarg() {
        Jvmarg t = new Jvmarg();
        jvmargs.add(t);
        return t;
    }

    public Property createJVMUserArg() {
        Property t = new Property();
        jvmUserArgs.add(t);
        return t;
    }

    //real object could be available by link
    public Platform get() {
        if (isReference()) {
            return (Platform) getRefid().getReferencedObject();
        }
        return this;
    }
}
